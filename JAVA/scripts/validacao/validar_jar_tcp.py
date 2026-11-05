#!/usr/bin/env python3
"""Validação HTTP/TCP do JAR, usando exclusivamente uma base isolada e fixtures sintéticas.

Uso: python3 validar_jar_tcp.py --base http://127.0.0.1:18086 --fixture fixture.json
     --estado estado.json --saida evidencias/http --fase criar|reinicio
Não inicia servidores, não executa DDL e não usa MockMvc. A fase reinicio deve ser
executada depois de encerrar e iniciar novamente o MESMO JAR com a MESMA base.
"""
import argparse
import datetime as dt
import hashlib
import http.cookiejar
import io
import json
import os
import pathlib
import subprocess
import traceback
import threading
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET
import zipfile
from zoneinfo import ZoneInfo


def sha(b):
    return hashlib.sha256(b).hexdigest()


def clean(value):
    if isinstance(value, dict):
        return {k: ("[MASCARADO]" if any(t in k.lower() for t in
                ("password", "senha", "secret", "token")) else clean(v)) for k, v in value.items()}
    if isinstance(value, list):
        return [clean(v) for v in value]
    return value


class Prova:
    def __init__(self, pasta, fase, roteiro):
        self.inicio_utc = dt.datetime.now(dt.timezone.utc).isoformat()
        self.fim_utc = None
        self.base_http = None
        self.pasta = pathlib.Path(pasta)
        self.pasta.mkdir(parents=True, exist_ok=True)
        self.fase, self.requests, self.assertions = fase, [], []
        utilitario = pathlib.Path(__file__).resolve()
        arquivo_roteiro = utilitario.parent / roteiro
        self.roteiro = arquivo_roteiro.name
        self.roteiro_sha256 = sha(arquivo_roteiro.read_bytes())
        self.utilitario = utilitario.name
        self.utilitario_sha256 = sha(utilitario.read_bytes())
        self.trava = threading.RLock()
        self.concluido = False

    def salvar(self, erro=None):
        resultado = {"fase": self.fase, "roteiro": self.roteiro,
                     "inicio_utc": self.inicio_utc, "fim_utc": self.fim_utc,
                     "base_http": self.base_http,
                     "roteiro_sha256": self.roteiro_sha256,
                     "utilitario": self.utilitario, "utilitario_sha256": self.utilitario_sha256,
                     "transporte": "HTTP/TCP real; urllib; processo JAR externo",
                     "instante_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
                     "requisicoes": self.requests, "assercoes": self.assertions,
                     "concluido": self.concluido,
                     "sucesso": self.concluido and erro is None and bool(self.assertions)
                                and all(x["passou"] for x in self.assertions), "erro": erro}
        with self.trava:
            # Keep the same assertions and result fields; commit each complete
            # receipt atomically so filesystem synchronization cannot expose a
            # partially rewritten JSON object.
            destino = self.pasta / (self.fase + ".json")
            temporario = destino.with_suffix(".complete.tmp")
            with temporario.open("w", encoding="utf-8") as arquivo:
                arquivo.write(json.dumps(resultado, ensure_ascii=False, indent=2))
                arquivo.flush()
                os.fsync(arquivo.fileno())
            temporario.replace(destino)

    def check(self, nome, condicao, esperado=None, observado=None):
        with self.trava:
            self.assertions.append({"cenario": nome, "passou": bool(condicao),
                                    "esperado": esperado, "observado": observado})
            self.salvar()
        if not condicao:
            raise AssertionError(f"{nome}: esperado={esperado!r}; observado={observado!r}")

    def concluir(self):
        self.fim_utc = dt.datetime.now(dt.timezone.utc).isoformat()
        self.concluido = True
        self.salvar()


class Cliente:
    def __init__(self, base, perfil, prova, senha):
        self.base, self.perfil, self.prova, self.senha = base.rstrip('/'), perfil, prova, senha
        with prova.trava:
            if prova.base_http not in (None, self.base):
                raise ValueError("A mesma prova não pode combinar endpoints HTTP diferentes.")
            prova.base_http = self.base
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def call(self, method, path, esperado=200, body=None, cenario=None, headers=None,
             csrf=True, confirmar=True, raw=None, content_type=None):
        h = {"Accept": "application/json", "User-Agent": "SGT-Auditoria-TCP-20261029"}
        if method not in ("GET", "HEAD", "OPTIONS"):
            if csrf and self.csrf:
                h["X-XSRF-TOKEN"] = self.csrf
            if confirmar:
                h["X-User-Password"] = self.senha
        if body is not None:
            raw = json.dumps(body, ensure_ascii=False).encode()
            h["Content-Type"] = "application/json"
        if content_type:
            h["Content-Type"] = content_type
        h.update(headers or {})
        req = urllib.request.Request(self.base + path, data=raw, headers=h, method=method)
        try:
            resp = self.opener.open(req, timeout=90)
        except urllib.error.HTTPError as exc:
            resp = exc
        payload = resp.read()
        try:
            parsed = json.loads(payload)
        except (ValueError, UnicodeDecodeError):
            parsed = None
        registro = {"metodo": method, "rota": path, "perfil": self.perfil, "cenario": cenario or path,
                    "esperado_status": esperado, "observado_status": resp.status,
                    "request": clean(body), "response": clean(parsed),
                    "bytes": len(payload), "sha256_resposta": sha(payload),
                    "headers": {k: v for k, v in resp.headers.items() if k.lower() in
                                ("content-type", "content-disposition", "cache-control", "x-content-type-options",
                                 "x-frame-options", "x-sgt-cache", "vary")}}
        self.prova.requests.append(registro)
        status_correto = resp.status in esperado if isinstance(esperado, (list, tuple)) else resp.status == esperado
        self.prova.check(cenario or f"{method} {path}", status_correto, esperado, resp.status)
        return parsed.get("data", parsed) if isinstance(parsed, dict) else parsed, payload, resp.headers

    def bootstrap(self):
        d, _, _ = self.call("GET", "/api/autenticacao/csrf", cenario="Bootstrap CSRF real")
        self.csrf = d["token"]
        self.prova.check("CSRF declara header correto", d["headerName"] == "X-XSRF-TOKEN")

    def login(self, matricula):
        self.bootstrap()
        d, _, h = self.call("POST", "/api/autenticacao/autenticar",
                             body={"matricula": str(matricula), "password": self.senha},
                             cenario="Login real " + self.perfil)
        self.prova.check("Login não devolve JWT/senha no JSON", "token" not in d and "password" not in d)
        set_cookies = h.get_all("Set-Cookie", [])
        auth = next(c for c in set_cookies if c.startswith("autenticacao="))
        self.prova.check("Cookie de autenticação HttpOnly e SameSite=Strict", "HttpOnly" in auth and "SameSite=Strict" in auth)
        self.bootstrap()
        s, _, _ = self.call("GET", "/api/autenticacao/sessao", cenario="Sessão persistida e vinculada ao dispositivo")
        self.prova.check("Sessão autenticada", s.get("authenticated") is True)

    def multipart(self, path, meta, arquivo, nome="contrato-tcp.pdf", esperado=200):
        boundary = "SGTAuditoria" + uuid.uuid4().hex
        parts = [f'--{boundary}\r\nContent-Disposition: form-data; name="metadados"; filename="metadados.json"\r\nContent-Type: application/json\r\n\r\n'.encode(),
                 json.dumps(meta, ensure_ascii=False).encode(), b'\r\n',
                 f'--{boundary}\r\nContent-Disposition: form-data; name="arquivo"; filename="{nome}"\r\nContent-Type: application/pdf\r\n\r\n'.encode(),
                 arquivo, f'\r\n--{boundary}--\r\n'.encode()]
        return self.call("POST", path, esperado, raw=b''.join(parts),
                         content_type="multipart/form-data; boundary=" + boundary,
                         cenario="Upload multipart real; hash/bytes conferidos na leitura")


def cpf_novo():
    base = str(int(uuid.uuid4().hex[:12], 16) % 900000000 + 100000000)
    for tamanho in (9, 10):
        resto = sum(int(base[i]) * (tamanho + 1 - i) for i in range(tamanho)) % 11
        base += str(0 if resto < 2 else 11 - resto)
    return base


def linhas_xlsx(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        root = ET.fromstring(z.read("xl/worksheets/sheet1.xml"))
    ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    return [["".join(c.itertext()) for c in row.findall("s:c", ns)] for row in root.findall(".//s:row", ns)]


def verificar_persistencia(local, nacional, outro, estado, prova):
    cid, fid, vid = estado["contrato_id"], estado["frequencia_id"], estado["vinculo_id"]
    contrato, _, _ = local.call("GET", f"/api/contratos/{cid}", cenario="Contrato publicado persiste após commit/reinício")
    prova.check("Estado publicado e identificação persistidos", contrato["estadoNegocial"] == "PUBLICADO"
                and contrato["numeroContrato"] == estado["numero_contrato"])
    vinculo, _, _ = local.call("GET", f"/api/vinculos-contratuais/{vid}")
    prova.check("Vínculo mantém pessoa e contrato", vinculo["pessoaId"] == estado["pessoa_id"] and vinculo["contratoId"] == cid)
    outro.call("GET", f"/api/contratos/{cid}", 404, cenario="Contrato fora do escopo territorial")
    outro.call("GET", f"/api/vinculos-contratuais/{vid}", 404, cenario="Vínculo fora do escopo territorial")
    q = urllib.parse.urlencode({"inicio": estado["data"], "fim": estado["data"], "contratoId": cid})
    pagina, _, _ = local.call("GET", "/api/frequencias/intervalo?" + q)
    itens = [f for f in pagina["content"] if f["id"] == fid]
    prova.check("Uma frequência com identidade e observação corretas", len(itens) == 1 and itens[0]["vinculoContratualId"] == vid
                and itens[0]["observacao"] == estado["observacao"])
    minimizada, _, _ = nacional.call("GET", "/api/frequencias/intervalo?" + q,
                                    cenario="Minimização de observação em consulta Nacional")
    itens_minimizados = [f for f in minimizada["content"] if f["id"] == fid]
    prova.check("Nacional recebe o registro com observação minimizada", len(itens_minimizados) == 1
                and itens_minimizados[0].get("observacao") is None)
    prova.check("Observação pessoal não exposta ao Nacional", estado["observacao"] not in json.dumps(minimizada, ensure_ascii=False))
    evidencia, _, _ = local.call("GET", f"/api/frequencias/{fid}/evidencias-integridade")
    prova.check("Hash integral persistido e verificável", evidencia["cadeiaIntegra"] is True and evidencia["estado"] == "INTEGRA")
    prova.check("Idempotência/rejeições não adicionam eventos", len(evidencia["eventos"]) == 1)
    hash_atual = evidencia["eventos"][0]["hashArmazenado"]
    prova.check("Hash anterior ao reinício é preservado", hash_atual == estado["hash_evento"], estado["hash_evento"], hash_atual)
    minimo, _, _ = nacional.call("GET", f"/api/frequencias/{fid}/eventos", cenario="Evento minimizado com cadeia original preservada")
    prova.check("Projeção declara minimização e omite narrativa", minimo[0].get("conteudoMinimizado") is True
                and estado["observacao"] not in json.dumps(minimo, ensure_ascii=False))
    emissao, _, _ = local.call("GET", f'/api/relatorios/administrativos/emissoes/{estado["emissao_id"]}')
    prova.check("Fotografia formal mantém hash e valores", emissao["metadados"]["hashConteudo"] == estado["hash_emissao"]
                and emissao["conteudo"]["registros"][0]["diario"][estado["data"]]["minutosFisicos"] == 240)
    _, raw, headers = local.call("GET", f'/api/contratos/{cid}/documentos/{estado["documento_id"]}/arquivo')
    prova.check("Arquivo documental íntegro após commit/reinício", sha(raw) == estado["hash_documento"])
    prova.check("Download preserva tipo, nosniff e attachment", headers.get_content_type() == "application/pdf"
                and headers.get("X-Content-Type-Options") == "nosniff"
                and "attachment" in headers.get("Content-Disposition", ""))


def criar(local, nacional, outro, admin, f, prova):
    hoje = dt.datetime.now(ZoneInfo("America/Sao_Paulo")).date()
    data = hoje - dt.timedelta(days=10)
    while data.weekday() >= 5:
        data -= dt.timedelta(days=1)
    inicio, fim = data - dt.timedelta(days=7), hoje + dt.timedelta(days=120)
    numero = "TCP-" + uuid.uuid4().hex[:12].upper()
    body = dict(numeroContrato=numero, anoContrato=inicio.year, descricao="Contrato criado no JAR final por HTTP/TCP",
                gestaoLocalId=f["gestao_id"], tipoContratacaoId=f["tipo_id"], cnpjEmpresa="12345678000190",
                razaoSocialEmpresa="Empresa Sintética TCP", nomeFantasiaEmpresa="TCP", dataInicio=str(inicio),
                dataFim=str(fim), efetivoPrevisto=2)
    minimo, _, _ = local.call("POST", "/api/contratos", 201, body={}, cenario="Payload mínimo inicia rascunho incompleto permitido")
    prova.check("Rascunho mínimo preserva estado de preparação", minimo["estadoNegocial"] == "RASCUNHO")
    local.call("DELETE", f'/api/contratos/{minimo["id"]}', 204, cenario="Rascunho mínimo sem fato negocial pode ser descartado")
    local.call("GET", f'/api/contratos/{minimo["id"]}', 404, cenario="Descarte confirmado por leitura posterior")
    local.call("POST", "/api/contratos", 400, body={"anoContrato": "ano-invalido"}, cenario="Tipo inválido de ano rejeitado")
    admin.call("POST", "/api/contratos", 403, body=body, cenario="Administrador técnico não escreve contrato negocial")
    contrato, _, _ = local.call("POST", "/api/contratos", 201, body=body)
    cid = contrato["id"]
    prova.check("Criação inicia em RASCUNHO", contrato["estadoNegocial"] == "RASCUNHO")
    base = f"/api/contratos/{cid}"
    comp = dict(lotacaoId=f["lotacao_id"], jornadaId=f["jornada_id"], cargoId=f["cargo_id"], efetivoPrevisto=2)
    composicao, _, _ = local.call("POST", base + "/composicoes", 201, body=comp)
    local.call("POST", base + "/composicoes", 409, body=comp, cenario="Composição duplicada rejeitada sem duplicação")
    publicacao = dict(dataPublicacao=str(hoje), referenciaSei="SEI-TCP-FINAL", justificativa="Publicação retroativa conferida via TCP")
    previa, _, _ = local.call("POST", base + "/previas/publicar", body=publicacao)
    publicacao.update(tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True)
    local.call("POST", base + "/publicar", body=publicacao)
    local.call("PUT", base, 409, body=body, cenario="Publicado não aceita edição comum")
    local.call("DELETE", base, 409, cenario="Publicado não pode ser apagado")
    pessoa = dict(nome="Pessoa TCP " + uuid.uuid4().hex[:8], cpf=cpf_novo(), email="pessoa.tcp@example.test")
    vinc = dict(pessoa=pessoa, contratoId=cid, dataInicio=str(inicio), matricula="TCP-" + uuid.uuid4().hex[:8],
                composicaoId=composicao["id"])
    v, _, _ = local.call("POST", "/api/vinculos-contratuais", 201, body=vinc,
                         cenario="Pessoa, vínculo e alocação criados atomicamente")
    vid, pid = v["id"], v["pessoaId"]
    alocacoes, _, _ = local.call("GET", f"/api/vinculos-contratuais/{vid}/alocacoes")
    prova.check("Alocação inicial persistida", len(alocacoes) == 1)
    observacao = "Observação pessoal sintética TCP " + uuid.uuid4().hex[:10]
    freq = dict(vinculoContratualId=vid, dataReferencia=str(data), status="PRESENCA", horasTrabalhadas="04:00",
                horarioSaida="12:00", observacao=observacao, justificativaRetificacao="Conferência documental por TCP",
                chaveIdempotencia="tcp-frequencia-" + uuid.uuid4().hex)
    frequencia, _, _ = local.call("POST", "/api/frequencias", body=freq)
    repetida, _, _ = local.call("POST", "/api/frequencias", body=freq, cenario="Reenvio idempotente da frequência")
    prova.check("Reenvio mantém ID e versão", frequencia["id"] == repetida["id"] and
                frequencia["versaoNegocial"] == repetida["versaoNegocial"])
    local.call("POST", "/api/frequencias", 409, body=dict(freq, status="FALTA"), cenario="Mesma chave com conteúdo diferente não altera frequência")
    lote_data = data + dt.timedelta(days=1)
    lote_ok = dict(freq, dataReferencia=str(lote_data), chaveIdempotencia="tcp-lote-" + uuid.uuid4().hex)
    lote_invalido = dict(freq, dataReferencia=str(hoje + dt.timedelta(days=1)),
                        chaveIdempotencia="tcp-lote-invalido-" + uuid.uuid4().hex)
    local.call("POST", "/api/frequencias/lote", 400, body=[lote_ok, lote_invalido],
               cenario="Segundo item futuro falha dentro do processamento e reverte o primeiro")
    pagina, _, _ = local.call("GET", "/api/frequencias?" + urllib.parse.urlencode({"data": str(lote_data), "contratoId": cid}))
    prova.check("Rollback do lote confirmado por nova requisição", pagina["totalElements"] == 0)
    q = urllib.parse.urlencode({"inicio": str(data), "fim": str(data), "contratoIds": cid})
    relatorio, _, _ = local.call("GET", "/api/relatorios/frequencia-consolidada?" + q)
    dia = relatorio["registros"][0]["diario"][str(data)]
    prova.check("Relatório usa 240 minutos manuais, preservando cálculo", dia["minutosFisicos"] == 240 and dia["minutosPagaveis"] == 240)
    local.call("GET", "/api/relatorios/frequencia-consolidada/exportacao?" + q + "&formato=CSV", 400,
               cenario="CSV rejeitado: o contrato de frequência prevê XLSX/PDF")
    for formato in ("XLSX", "PDF"):
        _, raw, h = local.call("GET", "/api/relatorios/frequencia-consolidada/exportacao?" + q + "&formato=" + formato)
        (prova.pasta / ("frequencia-tcp." + formato.lower())).write_bytes(raw)
        prova.check("Exportação " + formato + " não vazia", len(raw) > 100)
        if formato == "XLSX":
            rows = linhas_xlsx(raw)
            header = next(r for r in rows if "COLABORADOR" in r and "H.REAL" in r)
            row = next(r for r in rows if pessoa["nome"] in r)
            prova.check("XLSX publica H.REAL e H.PAG corretos", row[header.index("H.REAL")] == "04:00" and row[header.index("H.PAG")] == "04:00")
        else:
            prova.check("PDF possui assinatura binária", raw.startswith(b"%PDF-"))
            texto_pdf = prova.pasta / "frequencia-tcp-pdf.txt"
            subprocess.run(["pdftotext", "-layout", str(prova.pasta / "frequencia-tcp.pdf"), str(texto_pdf)], check=True)
            texto = " ".join(texto_pdf.read_text().split())
            prova.check("PDF contém identidade, H.REAL/H.PAG e cálculo manual", pessoa["nome"] in texto
                        and "H.REAL" in texto and "H.PAG" in texto and texto.count("04:00") >= 2)
    filtro = dict(tipo="FREQUENCIA_CONSOLIDADA", formato="VISUALIZACAO", inicio=str(data), fim=str(data),
                  contratoIds=[cid], chaveIdempotencia="tcp-emissao-" + uuid.uuid4().hex)
    emissao, _, _ = local.call("POST", "/api/relatorios/administrativos/emissoes", 201, body=filtro)
    mesma, _, _ = local.call("POST", "/api/relatorios/administrativos/emissoes", 201, body=filtro)
    prova.check("Emissão idempotente preserva ID e hash", emissao["metadados"]["id"] == mesma["metadados"]["id"] and
                emissao["metadados"]["hashConteudo"] == mesma["metadados"]["hashConteudo"])
    local.call("POST", "/api/relatorios/administrativos/emissoes", 409,
               body=dict(filtro, fim=str(data + dt.timedelta(days=1))), cenario="Idempotência de emissão rejeita outra intenção")
    docs, _, _ = local.call("GET", base + "/documentos")
    prova.check("Publicação gerou fonte documental", len(docs) == 1 and docs[0]["referenciaSei"] == "SEI-TCP-FINAL")
    pdf = (prova.pasta / "frequencia-tcp.pdf").read_bytes()
    doc, _, _ = local.multipart(base + f'/documentos/{docs[0]["id"]}/substituir',
                               {"motivo": "Anexo recebido e verificado por TCP", "chaveIdempotencia": "tcp-doc-" + uuid.uuid4().hex}, pdf)
    prova.check("Documento declara hash dos bytes enviados", doc["sha256Arquivo"] == sha(pdf))
    evidencia, _, _ = local.call("GET", f'/api/frequencias/{frequencia["id"]}/evidencias-integridade')
    estado = dict(contrato_id=cid, numero_contrato=numero, vinculo_id=vid, pessoa_id=pid,
                  frequencia_id=frequencia["id"], data=str(data), observacao=observacao,
                  hash_evento=evidencia["eventos"][0]["hashArmazenado"], emissao_id=emissao["metadados"]["id"],
                  hash_emissao=emissao["metadados"]["hashConteudo"], documento_id=doc["id"], hash_documento=sha(pdf))
    verificar_persistencia(local, nacional, outro, estado, prova)
    return estado


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--base", required=True); p.add_argument("--fixture", required=True)
    p.add_argument("--estado", required=True); p.add_argument("--saida", required=True)
    p.add_argument("--fase", choices=["criar", "reinicio"], required=True)
    args = p.parse_args(); fixture = json.loads(pathlib.Path(args.fixture).read_text())
    prova = Prova(args.saida, args.fase, "validar_jar_tcp.py")
    try:
        clientes = {n: Cliente(args.base, n, prova, fixture["senha"]) for n in ("local", "outro", "nacional", "admin", "anonimo")}
        clientes["anonimo"].call("GET", "/api/contratos", 401, cenario="Anônimo não acessa contrato")
        clientes["anonimo"].call("POST", "/api/autenticacao/autenticar", 403,
                                 body={"matricula": fixture["usuarios"]["local"], "password": fixture["senha"]},
                                 csrf=False, cenario="Login sem CSRF bloqueado")
        for n in ("local", "outro", "nacional", "admin"):
            clientes[n].login(fixture["usuarios"][n])
        if args.fase == "criar":
            estado = criar(clientes["local"], clientes["nacional"], clientes["outro"], clientes["admin"], fixture, prova)
            pathlib.Path(args.estado).write_text(json.dumps(estado, ensure_ascii=False, indent=2))
        else:
            verificar_persistencia(clientes["local"], clientes["nacional"], clientes["outro"],
                                    json.loads(pathlib.Path(args.estado).read_text()), prova)
        local = clientes["local"]
        cookie_antigo = "; ".join(c.name + "=" + c.value for c in local.cookies)
        local.call("POST", "/api/autenticacao/encerrar-sessao", cenario="Logout revoga sessão persistida")
        clientes["anonimo"].call("GET", "/api/autenticacao/sessao", 401,
                                 headers={"Cookie": cookie_antigo}, cenario="Replay do cookie revogado bloqueado")
        prova.concluir()
        print(json.dumps({"fase": args.fase, "requisicoes": len(prova.requests), "assercoes": len(prova.assertions), "sucesso": True}))
    except Exception as exc:
        prova.salvar(f"{type(exc).__name__}: {exc}")
        traceback.print_exc()
        raise


if __name__ == "__main__":
    main()
