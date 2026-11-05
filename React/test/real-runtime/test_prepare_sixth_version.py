"""Configuration regressions only: no HTTP, SQL, browser or real session is used."""
import contextlib
import io
import json
import os
from pathlib import Path
import runpy
import sys
import tempfile
import types
import unittest
from unittest.mock import patch


SCRIPT = Path(__file__).with_name("prepare_sixth_version.py")


class PrepareSixthVersionConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="sgt-sixth-config-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.runtime = self.root / "runtime"
        self.private = self.runtime / "private"
        self.private.mkdir(parents=True)
        (self.root / "evidence/infra").mkdir(parents=True)
        self.fixture = {"synthetic": True, "ids": {"contrato_rascunho": 37},
                        "objects": {"contrato_rascunho": {"id": 37}},
                        "accounts": {"gestor_local": {"username": "synthetic.local"}}}
        self.business = {"contratoEndpoint": "/api/contratos/37/provas-historicas",
                         "contratoReplay": {"eventoOrigemId": 101, "tokenPrevia": "old", "confirmarImpacto": True}}
        self.calls = []
        self.logins = []
        self.before = 5

    def run_driver(self):
        (self.private / "e2e-fixture.json").write_text(json.dumps(self.fixture))
        (self.private / "e2e-business.json").write_text(json.dumps(self.business))
        owner = self

        class Client:
            def __init__(self, account):
                self.account = account

            def login(self):
                owner.logins.append(self.account["username"])

            def request(self, method, path, payload=None, expected=None):
                owner.calls.append({"method": method, "path": path, "status": expected, "payload": payload})
                if path.endswith("/versoes"):
                    return list(range(owner.before if len(owner.calls) == 1 else 6))
                if path.endswith("/provas-historicas") and method == "GET":
                    return [{"id": 201, "versaoGeradaId": 301, "numeroRevisao": 2, "eventoOrigemId": 101}]
                if path.endswith("/previa"):
                    return {"tokenPrevia": "synthetic-preview"}
                if method == "POST":
                    return {"id": 202, "versaoGeradaId": 302}
                return {"version": 11}

        api = types.ModuleType("api_client")
        api.Client = Client
        api.EVENTS = self.calls
        api.save_private = lambda *_: None
        with patch.dict(os.environ, {"SGT_TEST_RUNTIME": str(self.runtime)}), \
                patch.dict(sys.modules, {"api_client": api}), \
                patch.object(sys, "path", list(sys.path)), contextlib.redirect_stdout(io.StringIO()):
            runpy.run_path(str(SCRIPT), run_name="__main__")

    def test_uses_observed_contract_and_keeps_five_to_six_boundary(self):
        self.run_driver()
        self.assertEqual([call["path"] for call in self.calls], [
            "/api/contratos/37/versoes", "/api/contratos/37/provas-historicas",
            "/api/contratos/37", "/api/contratos/37/provas-historicas/previa",
            "/api/contratos/37/provas-historicas", "/api/contratos/37/versoes"])
        command = self.calls[-2]["payload"]
        self.assertEqual(command["provaAnteriorId"], 201)
        self.assertEqual(command["versaoAnteriorId"], 301)
        self.assertEqual(command["versaoContratoEsperada"], 11)
        self.assertTrue(command["confirmarImpacto"])
        self.assertEqual(command["tokenPrevia"], "synthetic-preview")
        report = json.loads((self.root / "evidence/infra/pos-auditoria-sixth-version-fixture.json").read_text())
        self.assertEqual((report["before_versions"], report["after_versions"]), (5, 6))

    def test_mismatched_receipt_stops_before_login_or_requests(self):
        self.business["contratoEndpoint"] = "/api/contratos/38/provas-historicas"
        with self.assertRaisesRegex(ValueError, "contrato"):
            self.run_driver()
        self.assertEqual(self.logins, [])
        self.assertEqual(self.calls, [])

    def test_missing_observed_contract_stops_before_login_or_requests(self):
        del self.fixture["ids"]["contrato_rascunho"]
        with self.assertRaisesRegex(ValueError, "contrato"):
            self.run_driver()
        self.assertEqual(self.logins, [])
        self.assertEqual(self.calls, [])

    def test_previous_browser_checkpoint_is_never_overwritten(self):
        state = self.private / "e2e-gestor_local-browser-state.json"
        old = self.private / "e2e-gestor_local-browser-state-before-sixth-correcao.json"
        state.write_text("CURRENT SYNTHETIC STATE")
        old.write_text("EARLIER CHECKPOINT")
        self.run_driver()
        self.assertEqual(old.read_text(), "EARLIER CHECKPOINT")
        self.assertFalse(state.exists())
        saved = [p for p in self.private.glob("e2e-gestor_local-browser-state-before-sixth-*.json") if p != old]
        self.assertEqual(len(saved), 1)
        self.assertEqual(saved[0].read_text(), "CURRENT SYNTHETIC STATE")

    def test_wrong_boundary_still_stops_before_any_mutation(self):
        self.before = 4
        with self.assertRaisesRegex(RuntimeError, "five"):
            self.run_driver()
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.calls[0]["method"], "GET")


if __name__ == "__main__":
    unittest.main()
