#!/usr/bin/env python3
"""Exercise the candidate JAR over real HTTP; keep credentials and cookies private."""
import http.cookiejar
import json
from pathlib import Path
import sys
import time
import urllib.error
import urllib.request
from configuracao_http import opener_api, origem_backend

ROOT = Path(__file__).resolve().parent
PRIVATE = (ROOT / "private").resolve()
BASE = origem_backend()
VALUES = json.loads((PRIVATE / "secrets.json").read_text())
JAR = http.cookiejar.LWPCookieJar(str(PRIVATE / "http-cookies.txt"))
OPENER = opener_api(JAR)
EVENTS = []
PRIVATE_RESULTS = []


def request(method, path, payload=None, csrf=None):
    headers = {"User-Agent": "SGT-Isolated-HTTP-Smoke/1.0", "Accept": "application/json"}
    if csrf:
        headers[csrf["headerName"]] = csrf["token"]
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        response = OPENER.open(req, timeout=15)
    except urllib.error.HTTPError as exc:
        response = exc
    body = response.read()
    try:
        value = json.loads(body)
    except ValueError:
        value = {"non_json_response": True}
    EVENTS.append({"method": method, "path": path, "status": response.status, "body_bytes": len(body)})
    PRIVATE_RESULTS.append({"path": path, "status": response.status, "body": value})
    # The backend wraps successful DTOs in RespostaApi.data.
    payload = value.get("data") if isinstance(value, dict) else None
    return response.status, payload if isinstance(payload, dict) else value


def require(status, expected, label):
    if status != expected:
        raise RuntimeError(f"{label}: expected HTTP {expected}, observed {status}; private response retained")


def csrf():
    status, value = request("GET", "/api/autenticacao/csrf")
    require(status, 200, "CSRF initialization")
    if not isinstance(value.get("token"), str) or not value.get("headerName"):
        raise RuntimeError("CSRF response did not contain its expected contract fields")
    return value


def run():
    token = None
    for _ in range(60):
        try:
            token = csrf()
            break
        except (urllib.error.URLError, TimeoutError, ConnectionError):
            time.sleep(1)
    if token is None:
        raise RuntimeError("HTTP readiness timed out")
    changed = PRIVATE / "admin-password-changed"
    initial_password = VALUES["admin_password"] if changed.exists() else VALUES["bootstrap_password"]
    status, login = request("POST", "/api/autenticacao/autenticar", {"matricula": "90000001", "password": initial_password}, token)
    require(status, 200, "Synthetic administrator login")
    forced = bool(login.get("forceChangePassword"))
    if forced:
        status, _ = request("POST", "/api/autenticacao/alterar-senha-inicial", {"newPassword": VALUES["admin_password"]}, csrf())
        require(status, 200, "Mandatory initial password change")
        changed.touch(mode=0o600)
    status, session = request("GET", "/api/autenticacao/sessao")
    require(status, 200, "Authenticated session")
    account = {"base_url": BASE, "matricula": "90000001", "username": "adm", "password": VALUES["admin_password"], "password_changed_via_http": changed.exists()}
    path = PRIVATE / "http-credentials.json"
    path.write_text(json.dumps(account, indent=2) + "\n")
    path.chmod(0o600)
    result = {"passed": True, "base_url": BASE, "checks": EVENTS, "mandatory_initial_password_change_observed": forced, "authenticated_session_http": status, "csrf_tokens_and_auth_cookies_excluded": True, "completed_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
    (ROOT.parent / "evidence/infra/http-readiness.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


try:
    run()
except Exception as exc:
    print(f"HTTP smoke failed: {type(exc).__name__}: {exc}", file=sys.stderr)
    sys.exit(1)
finally:
    path = PRIVATE / "http-smoke-private.json"
    path.write_text(json.dumps(PRIVATE_RESULTS, indent=2) + "\n")
    path.chmod(0o600)
    JAR.save(ignore_discard=True, ignore_expires=True)
    Path(JAR.filename).chmod(0o600)
