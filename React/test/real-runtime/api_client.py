"""Small real-HTTP client for synthetic fixtures; all session material stays private."""
import http.cookiejar
import json
from pathlib import Path
import urllib.error
import urllib.request
from configuracao_http import opener_api, origem_backend

ROOT = Path(__file__).resolve().parent
PRIVATE = (ROOT / "private").resolve()
BASE = origem_backend()
EVENTS = []


def save_private(path, data):
    path = Path(path)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    path.chmod(0o600)


class Client:
    def __init__(self, account, cookie_path=None):
        self.account = account
        self.cookie_path = Path(cookie_path or PRIVATE / ("cookies-" + account["username"] + ".txt"))
        self.cookies = http.cookiejar.LWPCookieJar(str(self.cookie_path))
        if self.cookie_path.exists():
            self.cookies.load(ignore_discard=True, ignore_expires=True)
        self.opener = opener_api(self.cookies)

    def request(self, method, path, payload=None, expected=None, confirmation=True):
        headers = {"User-Agent": "SGT-Synthetic-Fixture/1.0", "Accept": "application/json"}
        if method not in {"GET", "HEAD"}:
            csrf = self.request("GET", "/api/autenticacao/csrf", expected=200)
            headers[csrf["headerName"]] = csrf["token"]
            if confirmation:
                headers["X-User-Password"] = self.account["password"]
        raw = None
        if payload is not None:
            headers["Content-Type"] = "application/json"
            raw = json.dumps(payload).encode()
        req = urllib.request.Request(BASE + path, data=raw, method=method, headers=headers)
        try:
            response = self.opener.open(req, timeout=30)
        except urllib.error.HTTPError as exc:
            response = exc
        body = response.read()
        try:
            value = json.loads(body) if body else None
        except ValueError:
            value = {"non_json_body_bytes": len(body)}
        EVENTS.append({"actor": self.account["username"], "method": method, "path": path, "status": response.status})
        save_private(PRIVATE / "fixture-last-response.json", {"actor": self.account["username"], "method": method, "path": path, "status": response.status, "body": value})
        self.cookies.save(ignore_discard=True, ignore_expires=True)
        self.cookie_path.chmod(0o600)
        if expected is not None and response.status not in (expected if isinstance(expected, (list, tuple)) else [expected]):
            raise RuntimeError(f"{method} {path}: HTTP {response.status}, expected {expected}; response is private")
        if isinstance(value, dict) and "data" in value and "status" in value:
            return value["data"]
        return value

    def login(self):
        password = self.account["password"] if self.account.get("initialized") else self.account.get("temporary_password", self.account["password"])
        result = self.request("POST", "/api/autenticacao/autenticar", {"matricula": self.account["matricula"], "password": password}, expected=200, confirmation=False)
        if result.get("forceChangePassword"):
            self.request("POST", "/api/autenticacao/alterar-senha-inicial", {"newPassword": self.account["password"]}, expected=200, confirmation=False)
        self.account["initialized"] = True
        return self.request("GET", "/api/autenticacao/sessao", expected=200)
