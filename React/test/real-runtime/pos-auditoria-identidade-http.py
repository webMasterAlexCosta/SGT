#!/usr/bin/env python3
"""Prova os contratos públicos de identidade e a limitação Operacional, sem DDL."""
from pathlib import Path
import hashlib
import json
import os
import sys
from urllib.parse import urlencode

runtime = Path(os.environ["SGT_TEST_RUNTIME"]).resolve()
sys.path.insert(0, str(runtime))
from api_client import Client, EVENTS, save_private
import manage

manage.require_configuration()
fixture_path = runtime / "private/e2e-fixture.json"
fixture = json.loads(fixture_path.read_text())
assert fixture.get("synthetic") is True
checks = []
result = {"synthetic": True, "application_sql_used": False, "checks": checks,
          "jar_sha256": hashlib.file_digest(manage.JAR.open("rb"), "sha256").hexdigest()}
def check(label, condition, **detail):
    checks.append({"check": label, "passed": bool(condition), **detail})
    assert condition, label
def rows(body):
    return body if isinstance(body, list) else body.get("content", [])
try:
    op = Client(fixture["accounts"]["audit_operacional"])
    op.login()
    params = {"dataRef": fixture["dates"]["today"], "lotacaoId": fixture["ids"]["lotacao"],
              "turnoId": fixture["ids"]["jornada"], "searchTerm": fixture["pending_vinculo_request"]["pessoa"]["cpf"],
              "page": 0, "size": 5}
    grid = op.request("GET", "/api/empregados/filtrados-frequencia?" + urlencode(params), expected=200)
    employees = rows(grid)
    check("Busca autorizada retorna uma identidade sintética", len(employees) == 1)
    employee = employees[0]
    eid = employee["id"]
    check("DTO real da grade não oferece Pessoa/Vínculo canônicos", not employee.get("pessoaId") and not employee.get("vinculoContratualId"), returned_keys=sorted(employee))
    reference = fixture["dates"]["today"]
    path = f"/api/empregados/{eid}/historico-contratos?data={reference}"
    denied = op.request("GET", path)
    check("Histórico que fornece associação canônica é vedado ao Operacional", EVENTS[-1]["status"] == 403, status=EVENTS[-1]["status"], code=denied.get("code"))
    session = op.request("GET", "/api/autenticacao/sessao", expected=200)
    check("403 genérico não encerra a sessão Java", session.get("authenticated") is True)
    rejected = op.request("POST", "/api/frequencias", {"empregado": {"id": eid}, "dataReferencia": reference, "status": "FOLGA"})
    check("POST público exige ID de Vínculo mesmo com Empregado informado", EVENTS[-1]["status"] == 400,
          status=EVENTS[-1]["status"], code=rejected.get("code"), message=rejected.get("message"))
    local = Client(fixture["accounts"]["gestor_local"])
    local.login()
    histories = rows(local.request("GET", path, expected=200))
    check("Histórico autorizado fornece Empregado/data autoritativos", bool(histories) and all(h.get("empregadoId") == eid and h.get("dataReferenciaIdentidade") == reference for h in histories))
    candidates = {h["vinculoCanonicoId"] for h in histories if h.get("vinculoCanonicoId")}
    compatible = []
    for vid in candidates:
        position = local.request("GET", f"/api/vinculos-contratuais/{vid}/alocacoes/posicao?data={reference}", expected=200)
        check("Posição responde o Vínculo/data consultados", position.get("vinculoId") == vid and position.get("dataReferencia") == reference)
        if position.get("situacaoTemporal") == "VIGENTE" and position.get("alocacaoId") and position.get("lotacaoId") == params["lotacaoId"] and position.get("jornadaId") == params["turnoId"]:
            compatible.append(vid)
    check("Uma única associação canônica é compatível com a linha", len(compatible) == 1, empregadoId=eid, vinculosCanonicos=compatible)
    result["canonical"] = {"empregadoId": eid, "vinculoContratualId": compatible[0], "dataReferencia": reference,
                           "lotacaoId": params["lotacaoId"], "jornadaId": params["turnoId"]}
    save_private(runtime / "private/identity-http-context.json", result["canonical"])
    result["passed"] = True
except Exception as exc:
    result["passed"] = False
    result["failure"] = str(exc)
    raise
finally:
    save_private(fixture_path, fixture)
    result["http"] = EVENTS
    (runtime.parent / "evidence/infra/pos-auditoria-identidade-http.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    # Este login substitui a sessão do navegador; preservar estado anterior sem
    # deixar o próximo cenário reutilizar um token já revogado pelo Java.
    old = runtime / "private/e2e-gestor_local-browser-state.json"
    if old.exists(): old.rename(old.with_name("e2e-gestor_local-before-identity-http.json"))
    print(json.dumps({"passed": result.get("passed"), "checks": len(checks)}))
