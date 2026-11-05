"""Explicit HTTP origin and transport for the synthetic runtime clients."""
import os
from urllib.parse import urlsplit, urlunsplit
import urllib.request


class RedirecionamentoHttpRecusado(RuntimeError):
    def __init__(self, status):
        self.status = status
        # Do not expose a Location header that may contain private URL data.
        super().__init__(f"HTTP {status}: redirecionamento inesperado recusado pelo cliente de validação.")


class _RejeitarRedirecionamento(urllib.request.HTTPRedirectHandler):
    def http_error_302(self, request, response, code, message, headers):
        response.close()
        raise RedirecionamentoHttpRecusado(code)

    http_error_301 = http_error_302
    http_error_303 = http_error_302
    http_error_307 = http_error_302
    http_error_308 = http_error_302


def opener_api(cookies):
    return urllib.request.build_opener(
        urllib.request.ProxyHandler({}),
        urllib.request.HTTPCookieProcessor(cookies),
        _RejeitarRedirecionamento(),
    )


def origem_backend(ambiente=None):
    values = os.environ if ambiente is None else ambiente
    raw = values.get("SGT_BACKEND_URL")
    if not isinstance(raw, str) or not raw:
        raise ValueError("Defina SGT_BACKEND_URL com a origem HTTP(S) do JAR desta execução.")
    message = "SGT_BACKEND_URL deve ser uma origem HTTP(S), sem credenciais, caminho, query ou fragmento."
    if any(character.isspace() or ord(character) < 32 or ord(character) == 127 for character in raw):
        raise ValueError(message)
    try:
        parsed = urlsplit(raw)
        port = parsed.port
        if (parsed.scheme not in {"http", "https"} or not parsed.hostname
                or parsed.username is not None or parsed.password is not None
                or parsed.path not in {"", "/"} or parsed.query or parsed.fragment
                or "?" in raw or "#" in raw or parsed.netloc.endswith(":")
                or (port is not None and not 1 <= port <= 65535)):
            raise ValueError(message)
    except ValueError:
        # Do not repeat a malformed URL that could contain embedded credentials.
        raise ValueError(message) from None
    return urlunsplit((parsed.scheme, parsed.netloc, "", "", ""))
