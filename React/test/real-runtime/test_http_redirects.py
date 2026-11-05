"""Real urllib pipeline with controlled responses; never HTTP, DB or application processes.

Removing the clients' redirect refusal must fail these tests: no redirect may
change an API mutation, forward private headers, or produce a successful gate.
Only the network transport is replaced; clients, cookies and handlers are real.
"""
import contextlib
from email.message import Message
from http import HTTPStatus
import io
import json
import os
from pathlib import Path
import runpy
import shutil
import socket
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import urllib.request
import urllib.response


SOURCE = Path(__file__).parent
ORIGIN = "http://127.0.0.1:18081"
DESTINATIONS = (
    "http://example.invalid/collected?private=SYNTHETIC_ONLY",
    "http://127.0.0.1:18082/collected",
    "https://127.0.0.1:18081/collected",
    "/collected",
)


class ControlledTransport(urllib.request.BaseHandler):
    handler_order = 100

    def __init__(self, owner):
        self.owner = owner

    def http_open(self, request):
        owner = self.owner
        owner.requests.append(request)
        headers = Message()
        headers["Content-Type"] = "application/json"
        status = 200
        if owner.redirect_status and request.full_url == ORIGIN + owner.redirect_path:
            status = owner.redirect_status
            headers["Location"] = owner.redirect_location
            payload = {"redirect": True}
        elif request.full_url == ORIGIN + "/api/autenticacao/csrf":
            payload = {"headerName": "X-Test-Csrf", "token": "token-sintetico"}
            headers["Set-Cookie"] = "SGT_SESSION=cookie-sintetico; Path=/; HttpOnly"
        elif request.full_url == ORIGIN + "/api/autenticacao/autenticar":
            payload = {"forceChangePassword": True}
        elif request.full_url == ORIGIN + "/api/autenticacao/alterar-senha-inicial":
            payload = {}
        elif request.full_url == ORIGIN + "/api/autenticacao/sessao":
            payload = {"id": 17, "authenticated": True}
        elif request.full_url == ORIGIN + "/api/documentos/17/download":
            payload = None
            headers.replace_header("Content-Type", "application/pdf")
        elif request.full_url == ORIGIN + "/api/fornecedores":
            status = owner.normal_status
            payload = {"id": 81}
        else:
            # A redirected request would be a false successful endpoint.
            payload = owner.collected_payload
        body = b"%PDF-1.4\nSYNTHETIC\x00\xff" if payload is None else json.dumps(
            {"status": "success" if status < 300 else "error", "data": payload}).encode()
        response = urllib.response.addinfourl(io.BytesIO(body), headers, request.full_url, status)
        response.msg = HTTPStatus(status).phrase
        owner.responses.append(response)
        return response

    https_open = http_open


class RedirectTransportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="sgt-redirect-offline-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.runtime = self.root / "runtime"
        self.private = self.runtime / "private"
        self.private.mkdir(parents=True)
        (self.root / "evidence/infra").mkdir(parents=True)
        for name in ("api_client.py", "http-smoke.py", "configuracao_http.py"):
            shutil.copyfile(SOURCE / name, self.runtime / name)
        (self.private / "secrets.json").write_text(json.dumps({
            "admin_password": "senha-nova-sintetica", "bootstrap_password": "senha-inicial-sintetica"}))
        self.requests = []
        self.responses = []
        self.redirect_status = None
        self.redirect_path = "/api/fornecedores"
        self.redirect_location = DESTINATIONS[0]
        self.collected_payload = {"id": 81, "forceChangePassword": False}
        self.normal_status = 200

    @contextlib.contextmanager
    def configured(self):
        build_opener = urllib.request.build_opener

        def with_controlled_transport(*handlers):
            return build_opener(*handlers, ControlledTransport(self))

        def prohibited(*args, **kwargs):
            raise AssertionError("Offline test prohibits network, subprocesses and waits")

        with patch.dict(os.environ, {"SGT_BACKEND_URL": ORIGIN}, clear=True), \
                patch.object(sys, "path", [str(self.runtime), *sys.path]), \
                patch("urllib.request.build_opener", side_effect=with_controlled_transport), \
                patch.object(socket, "socket", side_effect=prohibited), \
                patch.object(socket, "create_connection", side_effect=prohibited), \
                patch.object(subprocess, "Popen", side_effect=prohibited), \
                patch.object(subprocess, "run", side_effect=prohibited), \
                patch("time.sleep", side_effect=prohibited), \
                contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            yield

    def load_client(self):
        namespace = runpy.run_path(str(self.runtime / "api_client.py"), run_name="redirect_test_client")
        client = namespace["Client"]({"username": "sgt.synthetic", "password": "senha-sintetica"})
        return namespace, client

    def test_normal_mutation_preserves_json_csrf_confirmation_cookie_and_receipt(self):
        self.normal_status = 201
        with self.configured():
            namespace, client = self.load_client()
            result = client.request("POST", "/api/fornecedores", {"razaoSocial": "SINTETICO"}, expected=201)
        self.assertEqual(result, {"id": 81})
        self.assertEqual(len(self.requests), 2)
        request = self.requests[-1]
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(json.loads(request.data), {"razaoSocial": "SINTETICO"})
        self.assertEqual(request.get_header("X-user-password"), "senha-sintetica")
        self.assertEqual(request.get_header("X-test-csrf"), "token-sintetico")
        self.assertIn("SGT_SESSION=cookie-sintetico", request.get_header("Cookie"))
        self.assertEqual(namespace["EVENTS"][-1]["status"], 201)
        self.assertEqual(client.cookie_path.stat().st_mode & 0o777, 0o600)

    def test_explicit_business_refusal_remains_observable(self):
        self.normal_status = 403
        with self.configured():
            _, client = self.load_client()
            self.assertEqual(client.request("GET", "/api/fornecedores", expected=403), {"id": 81})
            with self.assertRaisesRegex(RuntimeError, "HTTP 403"):
                client.request("GET", "/api/fornecedores", expected=200)

    def test_normal_download_preserves_binary_bytes(self):
        with self.configured():
            _, client = self.load_client()
            with client.opener.open(ORIGIN + "/api/documentos/17/download") as response:
                self.assertEqual(response.status, 200)
                self.assertEqual(response.headers["Content-Type"], "application/pdf")
                self.assertEqual(response.read(), b"%PDF-1.4\nSYNTHETIC\x00\xff")
        self.assertEqual(len(self.requests), 1)

    def test_normal_smoke_keeps_login_password_change_and_session(self):
        with self.configured():
            runpy.run_path(str(self.runtime / "http-smoke.py"), run_name="__main__")
        self.assertEqual([request.get_method() for request in self.requests], ["GET", "POST", "GET", "POST", "GET"])
        self.assertEqual(json.loads(self.requests[1].data)["password"], "senha-inicial-sintetica")
        self.assertEqual(json.loads(self.requests[3].data)["newPassword"], "senha-nova-sintetica")
        report = json.loads((self.root / "evidence/infra/http-readiness.json").read_text())
        self.assertTrue(report["passed"])
        self.assertTrue(report["mandatory_initial_password_change_observed"])
        self.assertEqual(report["authenticated_session_http"], 200)


def client_redirect_case(method, status):
    def test(self):
        for destination in DESTINATIONS:
            with self.subTest(destination=destination):
                self.requests.clear()
                self.responses.clear()
                self.redirect_status, self.redirect_location = status, destination
                with self.configured():
                    _, client = self.load_client()
                    with self.assertRaisesRegex(RuntimeError, "HTTP " + str(status)) as refused:
                        client.request(method, "/api/fornecedores", {} if method == "POST" else None)
                self.assertNotIn("private=SYNTHETIC_ONLY", str(refused.exception))
                self.assertNotIn("senha-sintetica", str(refused.exception))
                self.assertEqual(len(self.requests), 2 if method == "POST" else 1)
                self.assertEqual(self.requests[-1].full_url, ORIGIN + "/api/fornecedores")
                self.assertEqual(self.requests[-1].get_method(), method)
                self.assertTrue(self.responses[-1].closed)
    return test


def smoke_redirect_case(stage, status):
    def test(self):
        for destination in DESTINATIONS:
            with self.subTest(destination=destination):
                self.requests.clear()
                self.redirect_status, self.redirect_location = status, destination
                self.redirect_path = "/api/autenticacao/" + ("csrf" if stage == "csrf" else "autenticar")
                self.collected_payload = ({"headerName": "X-Test-Csrf", "token": "token-sintetico"}
                                          if stage == "csrf" else {"forceChangePassword": False})
                with self.configured(), self.assertRaises(SystemExit) as refused:
                    runpy.run_path(str(self.runtime / "http-smoke.py"), run_name="__main__")
                self.assertEqual(refused.exception.code, 1)
                self.assertEqual(len(self.requests), 1 if stage == "csrf" else 2)
                self.assertEqual(self.requests[-1].full_url, ORIGIN + self.redirect_path)
                self.assertFalse((self.root / "evidence/infra/http-readiness.json").exists())
                self.assertFalse((self.private / "http-credentials.json").exists())
    return test


for code in (301, 302, 303, 307, 308):
    for verb in ("GET", "POST"):
        setattr(RedirectTransportTest, "test_client_rejects_" + verb.lower() + "_" + str(code),
                client_redirect_case(verb, code))
    for phase in ("csrf", "login"):
        setattr(RedirectTransportTest, "test_smoke_rejects_" + phase + "_" + str(code),
                smoke_redirect_case(phase, code))


if __name__ == "__main__":
    unittest.main(verbosity=2)
