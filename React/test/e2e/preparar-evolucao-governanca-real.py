#!/usr/bin/env python3
"""Prepara contrato, composições e recortes históricos sintéticos pela API real."""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import sys
from zoneinfo import ZoneInfo


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--label", required=True, help="Identificador novo para esta execução isolada")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,40}", args.label):
        raise ValueError("Label deve ter de 1 a 40 caracteres alfanuméricos, hífen ou sublinhado.")
    runtime = Path(os.environ["SGT_TEST_RUNTIME"]).resolve()
    private = runtime / "private"
    sys.path.insert(0, str(runtime))
    source = runtime / "api_client.py"
    spec = importlib.util.spec_from_file_location("sgt_evolucao_api_client", source)
    client_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(client_module)
    base = client_module.BASE
    fixture_path = private / "e2e-fixture.json"
    fixture = json.loads(fixture_path.read_text())
    if fixture.get("synthetic") is not True:
        raise ValueError("Fixture sintética privada obrigatória.")
    account = fixture["accounts"]["gestor_local"]
    historical_account = fixture["accounts"].get("ind08_historico")
    if not historical_account or not historical_account.get("password"):
        raise ValueError("A coordenação deve preparar accounts.ind08_historico na fixture privada.")
    historical_path = private / "ind08-historico-parcial.json"
    historical = json.loads(historical_path.read_text())
    if historical_account.get("matricula") != historical.get("matriculaGestor"):
        raise ValueError("O alias histórico não corresponde à fixture atual da suíte Java.")
    output = Path(os.environ.get("SGT_E2E_OUTPUT", runtime.parent / "evidencias/browser-evolucao"))
    output.mkdir(parents=True, exist_ok=True)
    state_path = private / f"evolucao-governanca-real-{args.label}.json"
    state = json.loads(state_path.read_text()) if state_path.exists() else {"synthetic": True, "label": args.label, "objects": {}}
    if state.get("browserStarted"):
        raise ValueError("Este label já iniciou consultas no navegador. Prepare outro label para repetir o cenário completo.")
    if state.get("label") != args.label or state.get("synthetic") is not True:
        raise ValueError("Estado privado incompatível com o label solicitado.")
    today = dt.datetime.now(ZoneInfo("America/Sao_Paulo")).date()
    if state.get("dates", {}).get("setupDay") not in {None, str(today)}:
        raise ValueError("O dia de preparação mudou; use outro label para uma fixture nova.")
    state["dates"] = {"setupDay": str(today), "contractStart": str(today + dt.timedelta(days=1)),
        "contractEnd": str(today + dt.timedelta(days=400))}
    state["historical"] = {key: historical[key] for key in ["contratoId", "dataReferencia", "conhecidoAntes",
        "conhecidoAposDescricao", "conhecidoAposMatricula", "conhecidoAposAusencia",
        "provaDescricaoId", "provaMatriculaId", "provaAusenciaId"]}
    state["historicalFixtureSha256"] = hashlib.sha256(historical_path.read_bytes()).hexdigest()
    state["historicalOrigin"] = historical.get("origem")
    state["baseUrl"] = base
    state["ready"] = False
    save = lambda: client_module.save_private(state_path, state)
    save()
    client = client_module.Client(account)
    report = {"label": args.label, "synthetic": True, "transport": "HTTP TCP to real JAR",
        "application_sql_used": False, "scope": "contract-and-history-only", "passed": False}

    def once(key, route, payload):
        if key not in state["objects"]:
            result = client.request("POST", route, payload, expected=(200, 201))
            if not isinstance(result, dict) or not isinstance(result.get("id"), int):
                raise ValueError("A API não devolveu a identidade persistida da fixture.")
            state["objects"][key] = {k: result[k] for k in ["id", "version", "numeroContrato"] if k in result}
            save()
        return state["objects"][key]["id"]

    try:
        client.login()
        client_module.save_private(fixture_path, fixture)
        ids = fixture["ids"]
        dates = state["dates"]
        zero_cargo = once("zeroCargo", "/api/cargos", {"nome": "SGT EVO ZERO " + args.label,
            "cbo": "411005", "descricao": "Composição sintética com zero documentado para E2E IND08",
            "ativo": True, "gestaoLocalId": ids["gestao_origem"]})
        contract = once("contract", "/api/contratos", {"numeroContrato": "SGTEVOUI-" + args.label,
            "anoContrato": today.year, "descricao": "Contrato sintético isolado do E2E de evolução",
            "gestaoLocalId": ids["gestao_origem"], "tipoContratacaoId": ids["tipo"],
            "cnpjEmpresa": "11444777000161", "razaoSocialEmpresa": "Fornecedor sintético E2E evolução",
            "nomeFantasiaEmpresa": "SGT EVOLUCAO E2E", "dataInicio": dates["contractStart"],
            "dataFim": dates["contractEnd"], "efetivoPrevisto": 2, "matriculaVinculoObrigatoria": False})
        route = f"/api/contratos/{contract}"
        for key, cargo, quantity in [("positiveComposition", ids["cargo"], 2), ("zeroComposition", zero_cargo, 0)]:
            once(key, route + "/composicoes", {"lotacaoId": ids["lotacao"], "jornadaId": ids["jornada"],
                "cargoId": cargo, "efetivoPrevisto": quantity, "dataInicioVigencia": dates["contractStart"],
                "dataFimVigencia": dates["contractEnd"]})
        once("publication", route + "/publicar", {"dataPublicacao": str(today),
            "referenciaSei": "SEI-SINTETICO-EVOUI-SEM-VALOR-DOCUMENTAL",
            "justificativa": "Publicação sintética integral para verificar a interface integrada"})
        state["ready"] = True
        save()
        report.update(passed=True, contractId=contract, zeroCompositionId=state["objects"]["zeroComposition"]["id"],
            dates=dates, historicalContractId=historical["contratoId"],
            historicalFixtureSha256=state["historicalFixtureSha256"], historicalOrigin=state["historicalOrigin"])
    except Exception as exc:
        report["failure"] = {"type": type(exc).__name__, "message": str(exc)}
        raise
    finally:
        save()
        report["http"] = [{k: item[k] for k in ["method", "path", "status"]} for item in client_module.EVENTS]
        report["finishedUtc"] = dt.datetime.now(dt.timezone.utc).isoformat()
        public_path = output / f"evolucao-governanca-real-{args.label}-setup.json"
        public_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        print(json.dumps({"passed": report["passed"], "label": args.label, "credentials_emitted": False,
            "report": str(public_path)}, ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"Preparação interrompida: {type(error).__name__}: {error}", file=sys.stderr)
        raise SystemExit(1)
