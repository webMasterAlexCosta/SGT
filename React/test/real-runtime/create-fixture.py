#!/usr/bin/env python3
"""Create labelled synthetic fixtures solely through the unmodified application's HTTP API."""
import datetime as dt
import json
import secrets
import sys
from zoneinfo import ZoneInfo
from api_client import BASE, ROOT, PRIVATE, Client, EVENTS, save_private

PATH = PRIVATE / "e2e-fixture.json"
FIXTURE = json.loads(PATH.read_text()) if PATH.exists() else {"base_url": BASE, "synthetic": True, "accounts": {}, "ids": {}, "objects": {}}
TODAY = dt.datetime.now(ZoneInfo("America/Sao_Paulo")).date()
FIXTURE.setdefault("dates", {"today": str(TODAY), "past": str(TODAY-dt.timedelta(days=30)), "future": str(TODAY+dt.timedelta(days=30)), "end": str(TODAY+dt.timedelta(days=400))})
FIXTURE.setdefault("reference", "SGT-E2E-SYNTHETIC-20261101 — referência exclusivamente sintética para validação, sem valor documental real")


def save():
    save_private(PATH, FIXTURE)


def create(client, key, path, payload, expected=(200, 201)):
    if key in FIXTURE["objects"]:
        return FIXTURE["objects"][key]
    result = client.request("POST", path, payload, expected=expected)
    FIXTURE["objects"][key] = result
    if isinstance(result, dict) and "id" in result:
        FIXTURE["ids"][key] = result["id"]
    save()
    return result


def account(client, key, username, matricula, perfil, **scope):
    if key in FIXTURE["accounts"]:
        return FIXTURE["accounts"][key]
    temporary = secrets.token_urlsafe(24) + "aA1!"
    final = secrets.token_urlsafe(24) + "bB2!"
    result = create(client, "user_" + key, "/api/usuarios", {"username": username, "email": username + "@correios.com.br", "matricula": matricula, "password": temporary, "perfil": perfil, "active": True, **scope})
    value = {"id": result["id"], "username": username, "matricula": matricula, "password": final, "temporary_password": temporary, "perfil": perfil, "initialized": False, **scope}
    FIXTURE["accounts"][key] = value
    save()
    return value


def run():
    if "fixture_admin" not in FIXTURE["accounts"]:
        root_account = json.loads((PRIVATE / "http-credentials.json").read_text())
        root_account["initialized"] = True
        root_client = Client(root_account, PRIVATE / "http-cookies.txt")
        account(root_client, "fixture_admin", "sgt.e2e.fixture.admin", "99000101", "ADMINISTRADOR")
    admin = Client(FIXTURE["accounts"]["fixture_admin"])
    admin.login()
    save()
    states = admin.request("GET", "/api/superintendencias", expected=200)
    state = next(x for x in states if x["uf"] == "SP")
    FIXTURE["ids"]["superintendencia"] = state["id"]
    for name, suffix in (("gestao_origem", "ORIGEM"), ("gestao_destino", "DESTINO")):
        create(admin, name, "/api/gestoes-locais", {"nome": "SGT E2E SINTETICA " + suffix, "codigo": "SGTE2E" + suffix, "ativo": True, "superintendenciaId": state["id"]})
    origin = FIXTURE["ids"]["gestao_origem"]
    destination = FIXTURE["ids"]["gestao_destino"]
    local_account = account(admin, "gestor_local", "sgt.e2e.gestor.origem", "99000102", "GESTOR_LOCAL", superintendenciaId=state["id"], gestaoLocalId=origin)
    destination_account = account(admin, "gestor_destino", "sgt.e2e.gestor.destino", "99000103", "GESTOR_LOCAL", superintendenciaId=state["id"], gestaoLocalId=destination)
    local = Client(local_account)
    local.login()
    dest = Client(destination_account)
    dest.login()
    save()
    for client, prefix, scope_id in ((local, "", origin), (dest, "destino_", destination)):
        create(client, prefix + "tipo", "/api/tipos-contratacao", {"descricao": "SGT E2E TIPO SINTETICO " + str(scope_id), "ativo": True, "matriculaVinculoObrigatoria": False, "gestaoLocalId": scope_id})
        create(client, prefix + "cargo", "/api/cargos", {"nome": "SGT E2E CARGO SINTETICO " + str(scope_id), "cbo": "411005", "descricao": "Fixture sintética de integração", "ativo": True, "gestaoLocalId": scope_id})
        days = [{"diaSemana": day, "diaTrabalho": i < 5, "entrada1": "08:00" if i < 5 else None, "saida1": "12:00" if i < 5 else None, "entrada2": "13:00" if i < 5 else None, "saida2": "17:00" if i < 5 else None} for i, day in enumerate(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"])]
        jornada = create(client, prefix + "jornada", "/api/jornadas", {"descricao": "SGT E2E JORNADA SINTETICA " + str(scope_id), "ativo": True, "gestaoLocalId": scope_id, "dias": days, "regrasAdicionais": []})
        create(client, prefix + "lotacao", "/api/lotacoes", {"nome": "SGT E2E LOTACAO SINTETICA " + str(scope_id), "municipio": "Sao Paulo", "ativo": True, "gestaoLocalId": scope_id, "turnos": [{"id": jornada["id"], "descricao": jornada["descricao"]}]})
    create(local, "motivo_desligamento", "/api/motivos-desligamento", {"descricao": "SGT E2E MOTIVO SINTETICO", "categoria": "DESLIGAMENTO_INDIVIDUAL", "ativo": True, "gestaoLocalId": origin})
    for key, number, start in (("contrato_rascunho", "SGTE2E001", FIXTURE["dates"]["today"]), ("contrato_futuro", "SGTE2E002", FIXTURE["dates"]["future"])):
        create(local, key, "/api/contratos", {"numeroContrato": number, "anoContrato": TODAY.year, "descricao": "CONTRATO SINTETICO SGT E2E — rascunho para validacao", "tipoContratacaoId": FIXTURE["ids"]["tipo"], "gestaoLocalId": origin, "cnpjEmpresa": "11444777000161", "razaoSocialEmpresa": "FORNECEDOR SINTETICO SGT E2E LTDA", "nomeFantasiaEmpresa": "SGT E2E SINTETICO", "dataInicio": start, "dataFim": FIXTURE["dates"]["end"], "efetivoPrevisto": 2, "matriculaVinculoObrigatoria": False})
        create(local, "composicao_" + key, "/api/contratos/" + str(FIXTURE["ids"][key]) + "/composicoes", {"lotacaoId": FIXTURE["ids"]["lotacao"], "jornadaId": FIXTURE["ids"]["jornada"], "cargoId": FIXTURE["ids"]["cargo"], "efetivoPrevisto": 2, "dataInicioVigencia": start, "dataFimVigencia": FIXTURE["dates"]["end"]})
    grant_expiry = (dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=7)).isoformat().replace("+00:00", "Z")
    create(admin, "permissao_prova_historica", "/api/permissoes-excepcionais", {"tipo": "INCORPORACAO_PROVA_HISTORICA", "usuarioBeneficiarioId": local_account["id"], "contratoId": FIXTURE["ids"]["contrato_rascunho"], "gestaoLocalId": origin, "finalidade": "Validacao sintética por HTTP/UI sem valor documental real", "justificativa": FIXTURE["reference"], "autorizacaoAdministrativa": "AUTORIZACAO SINTETICA SGT E2E — ambiente isolado", "validoAte": grant_expiry})
    cpf = [int(x) for x in "925481703"]
    for length in (9, 10):
        digit = (sum(cpf[i] * (length + 1 - i) for i in range(length)) * 10) % 11
        cpf.append(0 if digit == 10 else digit)
    FIXTURE["pending_vinculo_request"] = {"pessoa": {"nome": "PESSOA SINTETICA SGT E2E", "cpf": "".join(map(str, cpf)), "dataNascimento": "1988-06-15"}, "contratoId": FIXTURE["ids"]["contrato_rascunho"], "dataInicio": FIXTURE["dates"]["today"], "composicaoId": FIXTURE["ids"]["composicao_contrato_rascunho"]}
    FIXTURE["pessoa_vinculo_pending_reason"] = "A API exige contrato publicado para criar vínculo e pessoa na mesma transação. Publicação permanece reservada ao teste UI da coordenação."
    FIXTURE["ready"] = True
    save()
    print(json.dumps({"ready": True, "ids": FIXTURE["ids"], "accounts": list(FIXTURE["accounts"]), "contracts_published": False, "credentials_emitted": False}, indent=2))


try:
    run()
except Exception as exc:
    print(f"Fixture preparation failed: {type(exc).__name__}: {exc}", file=sys.stderr)
    sys.exit(1)
finally:
    save()
    receipt = ROOT.parent / "evidence/infra/fixture-http-summary.json"
    old = json.loads(receipt.read_text()) if receipt.exists() else {"calls": []}
    old["calls"].extend(EVENTS)
    old.update({"ready": FIXTURE.get("ready", False), "synthetic": True, "application_sql_used": False, "tokens_passwords_cookies_excluded": True, "ids": FIXTURE["ids"]})
    receipt.write_text(json.dumps(old, indent=2) + "\n")
