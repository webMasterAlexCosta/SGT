#!/usr/bin/env python3
"""Consolida requisitos, registro Spring, chamadas HTTP e XML JUnit sem inferir sucesso de 401.

Uso: python3 scripts/gerar_matriz_auditoria.py --projeto . --execucao .
     --notas caminho/notas --saida docs/auditoria-integral --rotulo checkpoint
As notas mantêm a análise de cada critério; os resultados são sempre recalculados dos XML.
"""
import argparse
import collections
import csv
import datetime
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET


def ler_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def gravar_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def base_metodo(nome):
    return re.split(r"[\[(]", nome)[0]


def indice(nome):
    encontrado = re.search(r"\[(\d+)\]", nome)
    return encontrado.group(1) if encontrado else None


def referencias(regra):
    refs = list(regra.get("evidencia_nominal", []))
    for criterio in regra.get("criterios_http", []):
        refs.extend(criterio.get("metodos", []))
    for prova in regra.get("provas_por_criterio", []):
        if prova.get("arquivo") and prova.get("metodo"):
            refs.append(Path(prova["arquivo"]).stem + "#" + prova["metodo"])
    return sorted(set(ref.rsplit("/", 1)[-1].replace(".java#", "#") for ref in refs))


def carregar_execucao(raiz):
    arquivos = []
    for sub in ["surefire-reports", "failsafe-reports"]:
        arquivos.extend((raiz / "target" / sub).glob("TEST-*.xml"))
    # Se um teste aparece nos dois plugins, prevalece a execução mais recente.
    suites = {}
    for path in sorted(arquivos, key=lambda p: p.stat().st_mtime):
        suite = ET.parse(path).getroot()
        suites[suite.get("name")] = (path, suite)
    casos = []
    for _, (path, suite) in sorted(suites.items()):
        for teste in suite.findall("testcase"):
            falha = teste.find("failure")
            erro = teste.find("error")
            ignorado = teste.find("skipped")
            estado = "FALHOU" if falha is not None else "ERRO" if erro is not None else "IGNORADO" if ignorado is not None else "PASSOU"
            detalhe = falha if falha is not None else erro
            casos.append({"classe": teste.get("classname", suite.get("name")),
                          "metodo": teste.get("name"), "resultado": estado,
                          "xml": str(path.relative_to(raiz)),
                          "mensagem": (detalhe.get("message", "") if detalhe is not None else "")[:1200]})
    return casos


def localizar(ref, casos):
    if "#" not in ref:
        return []
    classe, metodo = ref.rsplit("#", 1)
    classe = classe.rsplit("/", 1)[-1].rsplit(".", 1)[-1]
    n = indice(metodo)
    return [c for c in casos if c["classe"].rsplit(".", 1)[-1] == classe
            and base_metodo(c["metodo"]) == base_metodo(metodo)
            and (not n or indice(c["metodo"]) == n)]


def prova_chamada(chamada, casos):
    ref = chamada["suite"] + "#" + chamada.get("test_method", "")
    n = indice(chamada.get("test_case", ""))
    if n:
        ref += "[" + n + "]"
    encontrados = localizar(ref, casos)
    return bool(encontrados) and all(c["resultado"] == "PASSOU" for c in encontrados)


def rota_regex(path):
    return "".join("[^/]+" if parte.startswith("{") else re.escape(parte)
                   for parte in re.split(r"(\{[^}]+\})", path))


def escapar(texto):
    return str(texto).replace("|", "\\|").replace("\n", " ")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for nome in ["projeto", "execucao", "notas", "saida"]:
        parser.add_argument("--" + nome, required=True, type=Path)
    parser.add_argument("--rotulo", required=True)
    parser.add_argument("--estrito", action="store_true")
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    levantamento = (args.projeto / "docs/LEVANTAMENTO_REQUISITOS.md").read_text(encoding="utf-8")
    canonicos = dict(re.findall(r"\|\s*\*\*([A-Z]{3}-\d{3})\*\*\s*\|\s*([^\n|]+)", levantamento))
    assert len(canonicos) == 198, f"Inventário canônico inesperado: {len(canonicos)}"
    notas_regras, notas_rotas, achados = {}, {}, []
    for nome in ["contratos", "pessoas", "frequencia", "seguranca_auditoria"]:
        nota = ler_json(args.notas / (nome + ".json"))
        for regra in nota.get("requisitos", nota.get("regras", [])):
            assert regra["id"] not in notas_regras, f"Requisito duplicado: {regra['id']}"
            notas_regras[regra["id"]] = {"frente": nome, **regra}
        for rota in nota["endpoints"]:
            chave = (rota["metodo"], rota["path"])
            assert chave not in notas_rotas, f"Endpoint duplicado nas notas: {chave}"
            notas_rotas[chave] = {"frente": nome, **rota}
        achados.extend({"frente": nome, **a} for a in nota.get("achados", []))
    assert set(canonicos) == set(notas_regras), {
        "sem_nota": sorted(set(canonicos) - set(notas_regras)),
        "nao_canonicos": sorted(set(notas_regras) - set(canonicos))}
    casos = carregar_execucao(args.execucao)
    requisitos = []
    for codigo, texto in sorted(canonicos.items()):
        nota = notas_regras[codigo]
        evidencias = []
        for ref in referencias(nota):
            encontrados = localizar(ref, casos)
            estado = "SEM_EXECUCAO" if not encontrados else "PASSOU" if all(c["resultado"] == "PASSOU" for c in encontrados) else "PENDENTE"
            evidencias.append({"teste": ref, "resultado": estado, "casos_junit": encontrados})
        estado = "PASSOU" if evidencias and all(e["resultado"] == "PASSOU" for e in evidencias) else "PENDENTE"
        requisitos.append({"id": codigo, "regra_canonica": texto.strip(), "frente": nota["frente"],
                           "resultado": estado, "evidencias": evidencias, "analise_por_criterio": nota})
    runtime = args.execucao / "target/auditoria-http"
    rotas = ler_json(runtime / "endpoints-runtime.json")
    chamadas = [json.loads(linha) for linha in (runtime / "requests.jsonl").read_text().splitlines() if linha.strip()]
    grupos = {(r["method"], r["path"]): [] for r in rotas}
    nao_resolvidas = []
    for chamada in chamadas:
        candidatas = [r for r in rotas if r["method"] == chamada["method"]
                      and re.fullmatch(rota_regex(r["path"]), chamada["path"])]
        if not candidatas:
            nao_resolvidas.append(chamada)
            continue
        # Um literal como /opcoes precede /{id}, inclusive quando a segurança recusa antes do handler.
        escolhida = min(candidatas, key=lambda r: r["path"].count("{"))
        grupos[(escolhida["method"], escolhida["path"])].append({**chamada, "teste_junit_aprovado": prova_chamada(chamada, casos)})
    endpoints = []
    for rota in rotas:
        chave = (rota["method"], rota["path"])
        nota, chamadas_rota = notas_rotas.get(chave), grupos[chave]
        validas = [c for c in chamadas_rota if c["status_ok"] and c["teste_junit_aprovado"]]
        aposentada = any(chave[1] == p or chave[1].startswith(p + "/") for p in ["/api/contas", "/api/enderecos", "/api/telefones"])
        estrutural = chave[0] == "DELETE" and chave[1] in ["/api/gestoes-locais/{id}", "/api/superintendencias/{id}"]
        status_nominal = 403 if aposentada else 409 if estrutural else None
        if status_nominal is not None:
            nominais = [c for c in validas if c["actual_status"] == status_nominal and c["perfil"] != "ANONIMO"]
        else:
            nominais = [c for c in validas if 200 <= c["actual_status"] < 300]
        semantica = "recusa intencional de rota aposentada" if aposentada else "recusa de exclusão estrutural" if estrutural else "caminho autorizado"
        estado = "PASSOU" if nota is not None and nominais else "PENDENTE"
        endpoints.append({"metodo": chave[0], "path": chave[1], "resultado": estado, "criterio_nominal": semantica,
                          "registro_spring": rota, "contrato_e_escopo": nota,
                          "requisicoes": len(chamadas_rota), "nominais_aprovadas": len(nominais),
                          "status_verificados": sorted({c["actual_status"] for c in validas}),
                          "perfis_verificados": sorted({c["perfil"] for c in validas}),
                          "metodos_verificados": sorted({c["suite"] + "#" + c.get("test_method", "") for c in validas
                                                          if c["suite"] != "AutenticacaoTodasRotasHttpIT"}),
                          "campos_entrada_observados": sorted({f for c in validas for f in c.get("request_fields", [])}),
                          "campos_resposta_observados": sorted({f for c in nominais for f in c.get("response_fields", [])}),
                          "cenarios_executados": chamadas_rota})
    resumo = {"rotulo": args.rotulo, "gerado_em_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "metodologia": "A aprovação exige referências nominais executadas e aprovadas. Status HTTP isolado não basta. O conteúdo e os efeitos são assertivos nos testes apontados. 401 apenas comprova a barreira anônima. Campos observados não substituem schemas e critérios das notas. Esta matriz descreve cenários de aceitação, não uma prova de ausência universal de defeitos.",
              "testes_junit": dict(collections.Counter(c["resultado"] for c in casos)),
              "requisitos": dict(collections.Counter(r["resultado"] for r in requisitos)),
              "endpoints": dict(collections.Counter(r["resultado"] for r in endpoints)),
              "endpoints_por_criterio": dict(collections.Counter(r["criterio_nominal"] for r in endpoints)),
              "requisicoes_http_registradas": len(chamadas),
              "requisicoes_por_status": dict(sorted(collections.Counter(c["actual_status"] for c in chamadas).items())),
              "requisicoes_por_perfil": dict(sorted(collections.Counter(c["perfil"] for c in chamadas).items())),
              "rotas_aposentadas": sum(r["criterio_nominal"] == "recusa intencional de rota aposentada" for r in endpoints),
              "requisicoes_sem_rota": len(nao_resolvidas),
              "notas_sem_rota_em_execucao": [" ".join(k) for k in sorted(set(notas_rotas) - set(grupos))]}
    gravar_json(args.saida / "RESUMO_EXECUCAO.json", resumo)
    gravar_json(args.saida / "MATRIZ_198_REQUISITOS.json", requisitos)
    gravar_json(args.saida / "MATRIZ_ENDPOINTS.json", endpoints)
    gravar_json(args.saida / "RESULTADOS_JUNIT.json", casos)
    gravar_json(args.saida / "ACHADOS_POR_FRENTE.json", achados)
    gravar_json(args.saida / "REQUISICOES_SEM_ROTA.json", nao_resolvidas)
    with (args.saida / "MATRIZ_198_REQUISITOS.csv").open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f); w.writerow(["requisito", "regra", "frente", "resultado", "testes"])
        w.writerows([r["id"], r["regra_canonica"], r["frente"], r["resultado"], "; ".join(e["teste"] for e in r["evidencias"])] for r in requisitos)
    linhas = ["# Matriz dos 198 requisitos", "", f"**Execução:** {args.rotulo}", "", resumo["metodologia"], "",
              "| ID | Regra definida | Resultado | Evidência nominal |", "|---|---|---|---|"]
    for r in requisitos:
        linhas.append("| " + " | ".join(escapar(x) for x in [r["id"], r["regra_canonica"], r["resultado"], "; ".join(e["teste"] for e in r["evidencias"]) or "Referência ainda pendente"]) + " |")
    (args.saida / "MATRIZ_198_REQUISITOS.md").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    linhas = ["# Matriz de endpoints, escopo e resposta esperada", "", f"**Execução:** {args.rotulo}. **Inventário Spring:** {len(rotas)} combinações de método e rota.", "",
              "Os detalhes completos de parâmetros, anotações de validação, campos dos DTOs, expectativas e cada chamada estão em MATRIZ_ENDPOINTS.json. As assertivas de conteúdo e efeitos estão nos métodos apontados; respostas anônimas não contam como sucesso funcional.", "",
              "| Método e rota | Escopo | Esperado | Resultado / status comprovados | Evidência nominal |", "|---|---|---|---|---|"]
    for r in endpoints:
        nota = r["contrato_e_escopo"] or {}
        esperado = nota.get("response_contract", "Contrato ainda pendente")
        if isinstance(esperado, dict):
            esperado = json.dumps(esperado, ensure_ascii=False)
        linhas.append("| " + " | ".join(escapar(x) for x in [r["metodo"] + " " + r["path"], nota.get("escopo", "Pendente"),
                       str(nota.get("success_status", "")) + " — " + str(esperado), r["resultado"] + " / " + ", ".join(map(str, r["status_verificados"])),
                       "; ".join(r["metodos_verificados"])]) + " |")
    (args.saida / "MATRIZ_ENDPOINTS.md").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    print(json.dumps(resumo, ensure_ascii=False, indent=2))
    if args.estrito and (resumo["requisitos"].get("PENDENTE", 0) or resumo["endpoints"].get("PENDENTE", 0)
                        or any(c["resultado"] != "PASSOU" for c in casos) or nao_resolvidas
                        or resumo["notas_sem_rota_em_execucao"]):
        raise SystemExit("Matriz contém pendências; fechamento estrito recusado.")


if __name__ == "__main__":
    main()
