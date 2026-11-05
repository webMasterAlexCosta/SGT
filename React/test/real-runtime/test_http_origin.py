"""Unit regressions for HTTP origins; urllib openers are controlled, never connected."""
import contextlib
import io
import json
import os
from pathlib import Path
import runpy
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch


SOURCE = Path(__file__).parent


class ControlledResponse:
    def __init__(self, status, payload):
        self.status = status
        self.payload = payload

    def read(self):
        return json.dumps({"status": self.status, "data": self.payload}).encode()


class HttpOriginConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="sgt-http-origin-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.runtime = self.root / "runtime"
        self.private = self.runtime / "private"
        self.private.mkdir(parents=True)
        (self.root / "evidence/infra").mkdir(parents=True)
        for name in ["api_client.py", "http-smoke.py", "configuracao_http.py"]:
            if (SOURCE / name).exists():
                shutil.copyfile(SOURCE / name, self.runtime / name)
        (self.private / "secrets.json").write_text(json.dumps({
            "admin_password": "senha-ficticia-nova", "bootstrap_password": "senha-ficticia-inicial"}))
        self.requests = []
        self.login_status = 200
        self.other_status = 201
        owner = self

        class ControlledOpener:
            def open(self, request, timeout):
                owner.requests.append(request)
                if request.full_url.endswith("/autenticacao/csrf"):
                    return ControlledResponse(200, {"headerName": "X-Test-Csrf", "token": "token-ficticio"})
                if request.full_url.endswith("/autenticacao/autenticar"):
                    return ControlledResponse(owner.login_status, {"forceChangePassword": True})
                if request.full_url.endswith("/autenticacao/alterar-senha-inicial"):
                    return ControlledResponse(200, {})
                if request.full_url.endswith("/autenticacao/sessao"):
                    return ControlledResponse(200, {"id": 17})
                return ControlledResponse(owner.other_status, {"id": 81})

        self.opener = ControlledOpener()

    @contextlib.contextmanager
    def configured(self, origin):
        env = {} if origin is None else {"SGT_BACKEND_URL": origin}
        with patch.dict(os.environ, env, clear=True), \
                patch.object(sys, "path", [str(self.runtime), *sys.path]), \
                patch("urllib.request.build_opener", return_value=self.opener), \
                contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            yield

    def load_client(self):
        namespace = runpy.run_path(str(self.runtime / "api_client.py"), run_name="sgt_configuration_test")
        return namespace, namespace["Client"]({"username": "sgt.synthetic", "password": "senha-ficticia"})

    def test_client_uses_explicit_nondefault_origin_and_preserves_csrf_confirmation(self):
        with self.configured("http://127.0.0.1:18081/"):
            namespace, client = self.load_client()
            result = client.request("POST", "/api/fornecedores", {"razaoSocial": "EMPRESA SINTETICA"}, expected=201)
        self.assertEqual(namespace["BASE"], "http://127.0.0.1:18081")
        self.assertEqual(result, {"id": 81})
        self.assertEqual([r.full_url for r in self.requests], [
            "http://127.0.0.1:18081/api/autenticacao/csrf", "http://127.0.0.1:18081/api/fornecedores"])
        headers = {k.lower(): v for k, v in self.requests[-1].header_items()}
        self.assertEqual(headers["x-test-csrf"], "token-ficticio")
        self.assertEqual(headers["x-user-password"], "senha-ficticia")
        self.assertEqual(client.cookie_path.parent, self.private)
        self.assertEqual(client.cookie_path.stat().st_mode & 0o777, 0o600)

    def test_client_supports_explicit_https_without_redirecting_to_local_default(self):
        with self.configured("https://sgt-synthetic.example.test:18443"):
            namespace, client = self.load_client()
            client.request("GET", "/api/fornecedores", expected=201)
        self.assertEqual(namespace["BASE"], "https://sgt-synthetic.example.test:18443")
        self.assertEqual(self.requests[0].full_url, namespace["BASE"] + "/api/fornecedores")

    def test_smoke_uses_explicit_nondefault_origin_and_keeps_login_change_session_sequence(self):
        with self.configured("http://127.0.0.1:18081/"):
            runpy.run_path(str(self.runtime / "http-smoke.py"), run_name="__main__")
        self.assertEqual([r.full_url for r in self.requests], [
            "http://127.0.0.1:18081/api/autenticacao/csrf",
            "http://127.0.0.1:18081/api/autenticacao/autenticar",
            "http://127.0.0.1:18081/api/autenticacao/csrf",
            "http://127.0.0.1:18081/api/autenticacao/alterar-senha-inicial",
            "http://127.0.0.1:18081/api/autenticacao/sessao"])
        self.assertEqual(json.loads(self.requests[1].data)["password"], "senha-ficticia-inicial")
        self.assertEqual(json.loads(self.requests[3].data)["newPassword"], "senha-ficticia-nova")
        for index in [1, 3]:
            self.assertEqual(dict((k.lower(), v) for k, v in self.requests[index].header_items())["x-test-csrf"], "token-ficticio")
        credentials = self.private / "http-credentials.json"
        self.assertEqual(json.loads(credentials.read_text())["base_url"], "http://127.0.0.1:18081")
        self.assertEqual(credentials.stat().st_mode & 0o777, 0o600)
        self.assertTrue((self.private / "admin-password-changed").exists())
        report = json.loads((self.root / "evidence/infra/http-readiness.json").read_text())
        self.assertEqual(report["base_url"], "http://127.0.0.1:18081")
        self.assertTrue(report["passed"])
        self.assertTrue(report["mandatory_initial_password_change_observed"])

    def test_client_unexpected_status_remains_failure(self):
        self.other_status = 409
        with self.configured("http://127.0.0.1:18081"):
            _, client = self.load_client()
            with self.assertRaisesRegex(RuntimeError, "HTTP 409"):
                client.request("GET", "/api/fornecedores", expected=200)

    def test_smoke_invalid_login_remains_failure(self):
        self.login_status = 403
        with self.configured("http://127.0.0.1:18081"), self.assertRaises(SystemExit) as stopped:
            runpy.run_path(str(self.runtime / "http-smoke.py"), run_name="__main__")
        self.assertEqual(stopped.exception.code, 1)
        self.assertFalse((self.root / "evidence/infra/http-readiness.json").exists())


def rejects_origin(origin, script):
    def test(self):
        before = sorted(p.name for p in self.private.iterdir())
        with self.configured(origin), self.assertRaisesRegex(ValueError, "SGT_BACKEND_URL"):
            runpy.run_path(str(self.runtime / script), run_name="__main__")
        self.assertEqual(self.requests, [])
        self.assertEqual(sorted(p.name for p in self.private.iterdir()), before)
    return test


for label, origin in [
    ("missing", None), ("empty", ""), ("scheme", "ftp://127.0.0.1:18081"),
    ("credentials", "http://synthetic:secret@127.0.0.1:18081"),
    ("path", "http://127.0.0.1:18081/api"), ("query", "http://127.0.0.1:18081/?x=1"),
    ("fragment", "http://127.0.0.1:18081/#x"), ("host", "http:///api"),
    ("port", "http://127.0.0.1:65536"), ("whitespace", " http://127.0.0.1:18081"),
]:
    for script in ["api_client.py", "http-smoke.py"]:
        setattr(HttpOriginConfigurationTest, "test_rejects_" + label + "_" + script.replace(".py", "").replace("-", "_"),
                rejects_origin(origin, script))


if __name__ == "__main__":
    unittest.main()
