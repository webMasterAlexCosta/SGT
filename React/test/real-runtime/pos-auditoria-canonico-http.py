#!/usr/bin/env python3
"""Valida a alternativa canônica pública; nenhuma associação textual é feita."""
from pathlib import Path
import json
import os
import sys
from urllib.parse import urlencode

runtime = Path(os.environ["SGT_TEST_RUNTIME"]).resolve()
sys.path.insert(0, str(runtime))
from api_client import Client, EVENTS, save_private

fixture_path = runtime / "private/e2e-fixture.json"
fixture = json.loads(fixture_path.read_text())
business = json.loads((runtime / "private/e2e-business.json").read_text())
assert fixture["synthetic"] is True
checks, evidence = [], {"synthetic": True, "application_sql_used": False, "writes": False}
def check(label, value, **detail):
    checks.append({"check": label, "passed": bool(value), **detail})
    assert value, label
try:
    for role in ("audit_operacional", "gestor_local"):
        client = Client(fixture["accounts"][role])
        client.login()
        date = fixture["dates"]["today"]
        params = {"ativoEm": date, "contratoId": fixture["ids"]["contrato_rascunho"], "page": 0, "size": 5}
        page = client.request("GET", "/api/vinculos-contratuais?" + urlencode(params), expected=200)
        records = page.get("content", [])
        check(role + ": leitura canônica paginada autorizada", len(records) <= 5 and any(v.get("id") == business["vinculoId"] for v in records))
        chosen = next(v for v in records if v["id"] == business["vinculoId"])
        check(role + ": Vínculo referencia Pessoa criada pela fixture", chosen.get("pessoaId") == business["pessoaId"], vinculoId=chosen["id"], pessoaId=chosen["pessoaId"])
        position = client.request("GET", f"/api/vinculos-contratuais/{chosen['id']}/alocacoes/posicao?data={date}", expected=200)
        check(role + ": posição de alocação pertence ao ID/data consultados", position.get("vinculoId") == chosen["id"] and position.get("dataReferencia") == date)
        check(role + ": alocação vigente pertence à Lotação/Jornada selecionadas", position.get("situacaoTemporal") == "VIGENTE" and bool(position.get("alocacaoId")) and position.get("lotacaoId") == fixture["ids"]["lotacao"] and position.get("jornadaId") == fixture["ids"]["jornada"])
        person = client.request("GET", f"/api/vinculos-contratuais/pessoas/{chosen['pessoaId']}/posicao?data={date}", expected=200)
        evidence.setdefault("positions", {})[role] = {"vinculo": chosen, "alocacao": position, "pessoa": person}
    evidence["passed"] = True
except Exception as exc:
    evidence["passed"] = False
    evidence["failure"] = str(exc)
    raise
finally:
    evidence["checks"], evidence["http"] = checks, EVENTS
    save_private(fixture_path, fixture)
    (runtime.parent / "evidence/infra/pos-auditoria-canonico-http.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n")
    for role in ("audit_operacional", "gestor_local"):
        state = runtime / f"private/e2e-{role}-browser-state.json"
        if state.exists(): state.rename(state.with_name(f"e2e-{role}-before-canonical-http.json"))
    print(json.dumps({"passed": evidence.get("passed"), "checks": len(checks)}))
