"""Gates de identidade/persistência; arquivos reais, serviços externos controlados.

Não inicia Java, banco ou HTTP. A comparação JSON, a prontidão por log e o recibo
usam os helpers reais. Somente captura MySQL e ciclo de processos são substituídos.
"""
from contextlib import ExitStack, redirect_stdout
import copy
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import manage
import snapshot


spec = importlib.util.spec_from_file_location("restart_gate_under_test", Path(__file__).with_name("restart-check.py"))
restart = importlib.util.module_from_spec(spec)
spec.loader.exec_module(restart)


def photograph(jar="a" * 64):
    return {
        "label": "before", "captured_utc": "2026-11-02T05:00:00Z",
        "schema": "sgt8045_http", "mysql_version": "8.0.45", "jar_sha256": jar,
        "mysql_identity": {
            "version": "8.0.45", "server_uuid": "11111111-2222-3333-4444-555555555555",
            "schema": "sgt8045_http", "port": 33076,
            "current_user": "sgt_validacao_8045@127.0.0.1",
        },
        "read_only_transaction": True, "consistent_snapshot": True,
        "application_ddl": False, "raw_row_values_exported": False,
        "schema_metadata_sha256": "b" * 64,
        "schema_metadata_sections_sha256": {"tables": "c" * 64},
        "tables": {"contrato": {"row_count": 1, "data_sha256": "d" * 64, "column_count": 2}},
        "aggregate_data_sha256": "e" * 64, "table_count": 1,
    }


def change(photo, kind):
    if kind == "jar":
        photo["jar_sha256"] = "f" * 64
    elif kind == "schema":
        photo["schema"] = photo["mysql_identity"]["schema"] = "sgt8045_upgrade"
    elif kind == "version":
        photo["mysql_version"] = photo["mysql_identity"]["version"] = "8.4.11"
    elif kind == "uuid":
        photo["mysql_identity"]["server_uuid"] = "99999999-2222-3333-4444-555555555555"
    elif kind == "user":
        photo["mysql_identity"]["current_user"] = "outra_conta@127.0.0.1"
    elif kind == "port":
        photo["mysql_identity"]["port"] = 33077
    elif kind == "missing_identity":
        photo.pop("mysql_identity")
    elif kind == "metadata":
        photo["schema_metadata_sha256"] = "f" * 64
    elif kind == "data":
        photo["tables"]["contrato"]["data_sha256"] = "f" * 64
        photo["aggregate_data_sha256"] = "f" * 64
    elif kind == "added_table":
        # Mesmo agregado declarado não pode ocultar a tabela observada a mais.
        photo["tables"]["empregado"] = {"row_count": 0, "data_sha256": "f" * 64, "column_count": 1}
    elif kind == "removed_table":
        photo["tables"].pop("contrato")
    elif kind == "changed_table":
        # A comparação deve conferir os mapas, além do agregado declarado.
        photo["tables"]["contrato"]["row_count"] = 2
    else:
        raise ValueError(kind)


class SnapshotComparisonTest(unittest.TestCase):
    def compare(self, kind=None):
        with tempfile.TemporaryDirectory(prefix="sgt-snapshot-gate-") as tmp:
            first, second = photograph(), photograph()
            second["label"] = "after"
            second["captured_utc"] = "2026-11-02T05:01:00Z"
            if kind:
                change(second, kind)
            before, after = Path(tmp) / "before.json", Path(tmp) / "after.json"
            before.write_text(json.dumps(first)); after.write_text(json.dumps(second))
            old_bytes = (before.read_bytes(), after.read_bytes())
            result = snapshot.compare(before, after)
            self.assertEqual((before.read_bytes(), after.read_bytes()), old_bytes)
            return result

    def rejected(self, kind):
        result = self.compare(kind)
        self.assertEqual(result.get("status"), "FAILED", result)
        return result

    def test_identical_data_preserves_existing_oracles_without_rewriting_files(self):
        result = self.compare()
        self.assertTrue(result["same_jar"])
        self.assertTrue(result["same_schema_metadata"])
        self.assertTrue(result["same_all_table_data"])
        self.assertEqual(result["changed_tables"], {})
        self.assertEqual(result["added_tables"], [])
        self.assertEqual(result["removed_tables"], [])
        self.assertEqual(result["excluded_tables"], [])

    def test_different_catalog_is_not_the_same_database(self):
        self.assertFalse(self.rejected("schema")["same_database_identity"])

    def test_different_server_version_is_rejected(self):
        self.assertFalse(self.rejected("version")["same_database_identity"])

    def test_different_server_uuid_is_rejected(self):
        self.assertFalse(self.rejected("uuid")["same_database_identity"])

    def test_different_connection_user_is_rejected(self):
        self.assertFalse(self.rejected("user")["same_database_identity"])

    def test_different_server_port_is_rejected(self):
        self.assertFalse(self.rejected("port")["same_database_identity"])

    def test_legacy_snapshot_without_identity_is_not_certified(self):
        self.assertFalse(self.rejected("missing_identity")["same_database_identity"])

    def test_added_table_is_explicit_even_if_aggregate_was_not_updated(self):
        self.assertEqual(self.rejected("added_table")["added_tables"], ["empregado"])

    def test_removed_table_is_explicit_even_if_aggregate_was_not_updated(self):
        self.assertEqual(self.rejected("removed_table")["removed_tables"], ["contrato"])

    def test_changed_table_is_explicit_even_if_aggregate_was_not_updated(self):
        self.assertIn("contrato", self.rejected("changed_table")["changed_tables"])

    def test_compare_cli_returns_nonzero_and_keeps_false_flags(self):
        with tempfile.TemporaryDirectory(prefix="sgt-snapshot-cli-") as tmp:
            first, second = photograph(), photograph()
            change(second, "metadata")
            paths = [Path(tmp) / "before.json", Path(tmp) / "after.json"]
            for path, value in zip(paths, [first, second]):
                path.write_text(json.dumps(value))
            output = io.StringIO()
            with patch("sys.argv", ["snapshot.py", "compare", *map(str, paths)]), redirect_stdout(output):
                with self.assertRaises(SystemExit) as result:
                    snapshot.main()
            self.assertNotEqual(result.exception.code, 0)
            receipt = json.loads(output.getvalue())
            self.assertEqual(receipt["status"], "FAILED")
            self.assertFalse(receipt["same_schema_metadata"])


class SnapshotCaptureTest(unittest.TestCase):
    def capture(self, server_uuid="11111111-2222-3333-4444-555555555555"):
        with tempfile.TemporaryDirectory(prefix="sgt-snapshot-capture-") as tmp:
            root = Path(tmp); jar = root / "candidate.jar"; jar.write_bytes(b"synthetic jar bytes")
            queries = []

            def mysql(query):
                queries.append(query)
                if query.startswith("SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION"):
                    return "contrato\tid\t1\ncontrato\tnumero\t2"
                if query == "SELECT VERSION();":
                    return "8.0.45"
                identity = "@@IDENTITY\n8.0.45\t" + server_uuid + "\tsgt8045_http\t33076\tsgt_validacao_8045@127.0.0.1\n"
                body = "@@META:tables\ncontrato\tInnoDB\n@@TABLE:contrato\n" + "a" * 64
                return (identity if "@@IDENTITY" in query else "") + body

            with patch.multiple(manage, EVIDENCE=root, JAR=jar, SCHEMA="sgt8045_http"), \
                    patch.object(manage, "require_configuration", return_value={}), \
                    patch.object(manage, "mysql", side_effect=mysql):
                path, result = snapshot.capture("synthetic")
            self.assertEqual(json.loads(path.read_text()), result)
            return result, queries

    def test_identity_is_recorded_from_the_same_read_only_snapshot_connection(self):
        result, queries = self.capture()
        self.assertEqual(result.get("mysql_identity"), photograph()["mysql_identity"])
        query = next(q for q in queries if "START TRANSACTION" in q)
        self.assertIn("@@IDENTITY", query)
        self.assertLess(query.index("START TRANSACTION"), query.index("@@IDENTITY"))
        self.assertLess(query.index("@@IDENTITY"), query.index("COMMIT"))
        self.assertTrue(result["read_only_transaction"])
        self.assertTrue(result["consistent_snapshot"])
        self.assertFalse(result["application_ddl"])
        self.assertFalse(result["raw_row_values_exported"])

    def test_unobserved_uuid_does_not_create_a_certified_snapshot(self):
        with self.assertRaisesRegex(RuntimeError, "ident|UUID|uuid"):
            self.capture(server_uuid="not-a-server-uuid")


class RestartGateTest(unittest.TestCase):
    def execute(self, difference=None, capture_error=None, dies_during_capture=False, foreign_both=False, existing_receipt=False):
        with tempfile.TemporaryDirectory(prefix="sgt-restart-gate-") as tmp, ExitStack() as stack:
            root = Path(tmp); private = root / "private"; evidence = root / "evidence"
            (private / "app").mkdir(parents=True); evidence.mkdir()
            jar = root / "candidate.jar"; jar.write_bytes(b"reviewed synthetic jar")
            expected = hashlib.sha256(jar.read_bytes()).hexdigest()
            (private / "java.pid").write_text("101")
            app_log = private / "app/java.log"; app_log.write_text("old process log\n")
            first = photograph("a" * 64 if foreign_both else expected)
            second = copy.deepcopy(first); second["label"] = "after"
            second["captured_utc"] = "2026-11-02T05:01:00Z"
            if difference:
                change(second, difference)
            paths = [evidence / "before.json", evidence / "after.json"]
            for path, value in zip(paths, [first, second]):
                path.write_text(json.dumps(value))
            if existing_receipt:
                (evidence / "case-restart-comparison.json").write_text(
                    '{"status":"PASSED","prior_evidence":"preserve original receipt"}\n')
            stop_requests = []

            class Supervisor:
                returncode = None
                terminated = False

                def __init__(self, *_args, **_kwargs):
                    (private / "java.pid").write_text("202")
                    with app_log.open("a") as log:
                        log.write("2026-11-02 DEBUG 202 --- [main] availability : ReadinessState changed to ACCEPTING_TRAFFIC\n")

                def poll(self):
                    return self.returncode

                def terminate(self):
                    self.terminated = True
                    self.returncode = -15

                def wait(self, timeout=None):
                    if self.returncode is None:
                        self.returncode = 0
                    return self.returncode

            supervisor = None

            def launch(*args, **kwargs):
                nonlocal supervisor
                supervisor = Supervisor(*args, **kwargs)
                return supervisor

            def capture(label):
                after = label.endswith("-after")
                if capture_error == ("after" if after else "before"):
                    raise RuntimeError("synthetic capture unavailable")
                if after and dies_during_capture:
                    supervisor.returncode = 7
                return (paths[1], second) if after else (paths[0], first)

            stack.enter_context(patch.multiple(manage, PRIVATE=private, EVIDENCE=evidence, JAR=jar))
            stack.enter_context(patch.object(manage, "require_configuration", return_value={}))
            stack.enter_context(patch.object(manage, "stop", side_effect=lambda what: stop_requests.append(what)))
            stack.enter_context(patch.object(restart, "alive", side_effect=lambda pid: pid == 202 and supervisor is not None and supervisor.poll() is None))
            stack.enter_context(patch.object(restart.subprocess, "Popen", side_effect=launch))
            stack.enter_context(patch.object(restart.signal, "signal", return_value=None))
            stack.enter_context(patch.object(snapshot, "capture", side_effect=capture))
            stack.enter_context(patch("socket.socket.connect", side_effect=AssertionError("HTTP/network is forbidden in this isolated gate test")))
            stack.enter_context(patch("sys.argv", ["restart-check.py", "--expected-jar-sha256", expected, "--label", "case"]))
            output = io.StringIO(); code = 0
            try:
                with redirect_stdout(output):
                    restart.main()
            except SystemExit as exc:
                code = exc.code
            except RuntimeError:
                code = 1
            receipt_path = evidence / "case-restart-comparison.json"
            receipt = json.loads(receipt_path.read_text()) if receipt_path.exists() else None
            return code, receipt, output.getvalue(), supervisor, stop_requests

    def rejected(self, **kwargs):
        code, receipt, output, supervisor, _stop_requests = self.execute(**kwargs)
        self.assertNotEqual(code, 0, "Divergência não pode completar o gate normalmente")
        self.assertIsNotNone(receipt, "A falha precisa conservar recibo conclusivo")
        self.assertEqual(receipt.get("status"), "FAILED", receipt)
        self.assertEqual(receipt["http_requests_performed_by_helper"], 0)
        self.assertNotIn("Restart validation recorded; keeping", output)
        if supervisor is not None:
            self.assertIsNotNone(supervisor.poll(), "Supervisor recusado deve ser encerrado")
        return receipt

    def test_identical_restart_is_explicitly_passed(self):
        code, receipt, _output, _supervisor, _stop_requests = self.execute()
        self.assertEqual(code, 0)
        self.assertEqual(receipt.get("status"), "PASSED")
        self.assertTrue(receipt["same_jar"])
        self.assertTrue(receipt["same_schema_metadata"])
        self.assertTrue(receipt["same_all_table_data"])
        self.assertEqual(receipt["http_requests_performed_by_helper"], 0)

    def test_jar_difference_fails_before_http(self):
        self.assertFalse(self.rejected(difference="jar")["same_jar"])

    def test_schema_metadata_difference_fails_before_http(self):
        self.assertFalse(self.rejected(difference="metadata")["same_schema_metadata"])

    def test_data_difference_fails_before_http(self):
        self.assertFalse(self.rejected(difference="data")["same_all_table_data"])

    def test_other_database_fails_before_http(self):
        self.assertFalse(self.rejected(difference="schema")["same_database_identity"])

    def test_added_table_fails_even_with_equal_declared_aggregate(self):
        self.assertEqual(self.rejected(difference="added_table")["added_tables"], ["empregado"])

    def test_removed_table_fails_even_with_equal_declared_aggregate(self):
        self.assertEqual(self.rejected(difference="removed_table")["removed_tables"], ["contrato"])

    def test_changed_table_fails_even_with_equal_declared_aggregate(self):
        self.assertIn("contrato", self.rejected(difference="changed_table")["changed_tables"])

    def test_both_snapshots_must_match_reviewed_jar(self):
        self.rejected(foreign_both=True)

    def test_capture_failure_before_restart_is_recorded(self):
        self.rejected(capture_error="before")

    def test_capture_failure_after_restart_is_recorded(self):
        self.rejected(capture_error="after")

    def test_jvm_death_during_capture_prevents_certification(self):
        self.rejected(dies_during_capture=True)

    def test_preserved_receipt_is_not_overwritten_or_used_to_stop_the_runtime(self):
        code, receipt, _output, supervisor, stop_requests = self.execute(existing_receipt=True)
        self.assertNotEqual(code, 0)
        self.assertEqual(receipt, {"status": "PASSED", "prior_evidence": "preserve original receipt"})
        self.assertEqual(stop_requests, [])
        self.assertIsNone(supervisor)


if __name__ == "__main__":
    unittest.main()
