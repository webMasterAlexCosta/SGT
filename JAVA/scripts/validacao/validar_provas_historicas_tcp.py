#!/usr/bin/env python3
"""Provas históricas reais via HTTP/TCP no mesmo JAR, antes/depois do reinício.

Não inicia processos nem usa Java/SQL como oráculo. Requer a preparação sintética
opt-in e compara fonte documental, DTOs, versões e trilhas relidas pela API.
O estado contém tokens de prévia: arquivo privado, sem publicação direta.
"""
import argparse
import copy
import datetime as dt
import json
import os
import pathlib
import re
import textwrap
import traceback
import urllib.parse
import uuid

from validar_jar_tcp import Cliente, Prova, sha


def query(path, **params):
    return path + "?" + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})


def agora():
    return dt.datetime.now(dt.timezone.utc).isoformat()


def canonico(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def privado(path, value):
    path = pathlib.Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    os.fchmod(fd, 0o600)
    with os.fdopen(fd, "w") as out:
        json.dump(value, out, ensure_ascii=False, indent=2)


def pdf_fonte_sintetica(fonte):
    """PDF autossuficiente: todos os valores da fonte, sem biblioteca externa."""
    texto = ["DOCUMENTO SINTETICO EXCLUSIVO DO TESTE ISOLADO SGT/GTRAT",
             "Nao representa documento de pessoa, contrato ou organizacao reais.",
             "Fonte declarada antes da simulacao da lacuna na fixture JPA.",
             "SHA-256 da fonte JSON canonica: " + sha(canonico(fonte)), ""]
    for linha in json.dumps(fonte, ensure_ascii=False, indent=2, sort_keys=True).splitlines():
        texto.extend(textwrap.wrap(linha, 100, replace_whitespace=False, drop_whitespace=False) or [""])
    paginas = [texto[i:i + 61] for i in range(0, len(texto), 61)]
    objetos = [b"<< /Type /Catalog /Pages 2 0 R >>", b"", b"<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"]
    kids = []
    for linhas in paginas:
        pagina_id, stream_id = len(objetos) + 1, len(objetos) + 2
        kids.append(f"{pagina_id} 0 R")
        comandos = [b"BT /F1 8 Tf 11 TL 36 806 Td"]
        for linha in linhas:
            segura = linha.encode("cp1252", "replace").replace(b"\\", b"\\\\").replace(b"(", b"\\(").replace(b")", b"\\)")
            comandos.append(b"(" + segura + b") Tj T*")
        comandos.append(b"ET")
        stream = b"\n".join(comandos)
        objetos.append(f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents {stream_id} 0 R >>".encode())
        objetos.append(f"<< /Length {len(stream)} >>\nstream\n".encode() + stream + b"\nendstream")
    objetos[1] = ("<< /Type /Pages /Kids [" + " ".join(kids) + f"] /Count {len(kids)} >>").encode()
    pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for numero, objeto in enumerate(objetos, 1):
        offsets.append(len(pdf)); pdf.extend(f"{numero} 0 obj\n".encode() + objeto + b"\nendobj\n")
    xref = len(pdf)
    pdf.extend(f"xref\n0 {len(objetos) + 1}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        pdf.extend(f"{offset:010} 00000 n \n".encode())
    pdf.extend(f"trailer\n<< /Size {len(objetos) + 1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode())
    return bytes(pdf)


class AuditoriaProvas:
    def __init__(self, args, fixture, prova):
        self.a, self.f, self.p = args, fixture, prova
        self.h = fixture["provas_historicas"]
        self.p.check("Fixture declara fontes exclusivamente sintéticas", self.h.get("somente_dados_sinteticos") is True)
        self.clientes = {}
        for perfil in ("local", "outro", "nacional", "admin"):
            c = Cliente(args.base, perfil, prova, fixture["senha"])
            c.login(fixture["usuarios"][perfil]); self.clientes[perfil] = c
        self.estado = {"fixture_sha256": sha(pathlib.Path(args.fixture).read_bytes()), "fontes_sinteticas": True}

    def call(self, method, path, status=200, body=None, perfil="local", **kwargs):
        return self.clientes[perfil].call(method, path, status, body=body, **kwargs)[0]

    def guardar(self, chave, valor):
        self.estado[chave] = valor
        privado(self.a.estado, self.estado)

    def conceder(self, contrato_id):
        return self.call("POST", "/api/permissoes-excepcionais", 201, perfil="admin", body=dict(
            tipo="INCORPORACAO_PROVA_HISTORICA", usuarioBeneficiarioId=self.f["usuarios_ids"]["local"],
            contratoId=contrato_id, finalidade="Incorporacao de fontes sinteticas no teste TCP",
            justificativa="Acervo sintetico controlado com documento-oraculo preexistente",
            autorizacaoAdministrativa="ATO-SINTETICO-TCP-PROVAS",
            validoAte=(dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=12)).isoformat()))

    def confirmar(self, rota, req):
        previa = self.call("POST", rota + "/previa", body=req)
        self.p.check("Prévia devolve token não vazio", isinstance(previa.get("tokenPrevia"), str) and bool(previa["tokenPrevia"]))
        req.update(tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True)
        return self.call("POST", rota, 201, req)

    def versoes_nome(self):
        return [self.call("GET", f'/api/pessoas/{self.h["pessoa_id"]}')["version"],
                self.call("GET", f'/api/vinculos-contratuais/{self.h["vinculo_id"]}')["version"]]

    def versoes_motivo(self):
        return [self.call("GET", f'/api/motivos-desligamento/{self.h["motivo_id"]}')["version"],
                self.call("GET", f'/api/contratos/{self.h["contrato_contexto_id"]}')["version"]]

    def nome(self, data=None, conhecido=None, perfil="local"):
        return self.call("GET", query(f'/api/pessoas/{self.h["pessoa_id"]}/nome-historico',
                         vinculoId=self.h["vinculo_id"], data=data or self.h["data_contexto"], conhecidoAte=conhecido), perfil=perfil)

    def motivo(self, data=None, conhecido=None, perfil="local"):
        return self.call("GET", query(f'/api/motivos-desligamento/{self.h["motivo_id"]}/contexto-historico',
                         contratoId=self.h["contrato_contexto_id"], data=data or self.h["data_contexto"], conhecidoAte=conhecido), perfil=perfil)

    def nome_req(self, nome, anterior=None):
        pv, vv = self.versoes_nome()
        return dict(vinculoId=self.h["vinculo_id"], nomeComprovado=nome,
                    dataInicioEfetiva=self.h["data_inicio_contexto"], dataFimEfetiva=self.h["data_fim_contexto"],
                    referenciaSei="SEI-SINTETICO-TCP-NOME", motivo="Fonte nominal sintetica com periodo explicitamente comprovado",
                    autorizacao="ATO-SINTETICO-TCP-PROVAS", versaoPessoaEsperada=pv, versaoVinculoEsperada=vv,
                    provaAnteriorId=anterior, chaveIdempotencia=uuid.uuid4().hex)

    def motivo_req(self, descricao, categoria, anterior=None):
        mv, cv = self.versoes_motivo()
        return dict(contratoId=self.h["contrato_contexto_id"], descricaoComprovada=descricao, categoriaComprovada=categoria,
                    dataInicioEfetiva=self.h["data_inicio_contexto"], dataFimEfetiva=self.h["data_fim_contexto"],
                    referenciaSei="SEI-SINTETICO-TCP-MOTIVO", motivo="Documento sintetico comprova texto e categoria no periodo",
                    autorizacao="ATO-SINTETICO-TCP-PROVAS", versaoMotivoEsperada=mv, versaoContratoEsperada=cv,
                    provaAnteriorId=anterior, chaveIdempotencia=uuid.uuid4().hex)

    def negativas_apos_concessao(self, rota, req):
        self.call("POST", rota + "/previa", 400,
                  dict(req, referenciaSei=None, documentoComprobatorioId=None), cenario="Justificativa sem fonte não prova o passado")
        self.call("POST", rota + "/previa", 404, req, perfil="outro", cenario="Outra Gestão não incorpora prova do contrato")
        self.call("POST", rota + "/previa", 403, req, perfil="nacional", cenario="Consulta nacional não concede escrita negocial")
        self.call("POST", rota + "/previa", 403, req, perfil="admin", cenario="Administrador técnico não incorpora prova negocial")
        self.call("POST", rota, 409, req, cenario="Confirmação sem prévia vinculante é recusada")
        self.call("POST", rota, 428, req, confirmar=False, cenario="Confirmação da senha é exigida no registro")
        self.call("POST", rota, 403, req, csrf=False, cenario="Registro sem CSRF é recusado")

    def contrato_criar(self):
        cid = self.h["contrato_prova_id"]
        base, rota = f"/api/contratos/{cid}", f"/api/contratos/{cid}/provas-historicas"
        req = copy.deepcopy(self.h["fonte_contrato"])
        originais = self.call("GET", base + "/versoes")
        origem = next(v for v in originais if v["id"] == req["versaoAnteriorId"])
        eventos = self.call("GET", base + "/eventos")
        self.p.check("A origem sintética preserva a lacuna e o evento de publicação", origem["descricao"] is None
                     and origem["tipoContratacaoDescricao"] is None and len(eventos) == 1)
        posicao = self.call("GET", query(base + "/posicao-temporal", data=self.h["data_contrato"]))
        self.p.check("Sem fonte, o mestre atual não preenche a descrição histórica", posicao["versaoContratual"]["descricao"] is None)
        self.call("POST", rota + "/previa", 403, req, cenario="Gestor precisa de concessão específica para incorporar o contrato")
        self.conceder(cid)
        self.negativas_apos_concessao(rota, req)
        conflito = copy.deepcopy(req); conflito["fotografia"]["numeroContrato"] = "NUMERO-CONFLITANTE-SINTETICO"
        self.call("POST", rota + "/previa", 409, conflito, cenario="Fonte complementar não sobrescreve identificador já comprovado")
        self.p.check("Negativas não gravaram prova contratual", self.call("GET", rota) == [])
        # O PDF contém TODOS os valores da fonte sintética; não é outro relatório
        # reaproveitado como se fosse o documento que comprova o contrato.
        pdf = pdf_fonte_sintetica(req)
        arquivo = self.p.pasta / "fonte-documental-sintetica-contrato.pdf"
        arquivo.write_bytes(pdf)
        docs = self.call("GET", base + "/documentos")
        self.p.check("Publicação sintética possui uma fonte original", len(docs) == 1 and docs[0]["estado"] == "PUBLICADO")
        doc = self.clientes["local"].multipart(base + f'/documentos/{docs[0]["id"]}/substituir',
                dict(motivo="Incorporar bytes da fonte sintetica preexistente", chaveIdempotencia=uuid.uuid4().hex),
                pdf, nome="fonte-documental-sintetica-contrato.pdf")[0]
        self.p.check("Fonte física preserva hash exato do documento enviado", doc["sha256Arquivo"] == sha(pdf))
        req.update(documentoComprobatorioId=doc["id"], referenciaSei=None,
                   versaoContratoEsperada=self.call("GET", base)["version"])
        conhecido_antes = agora()
        prova = self.confirmar(rota, req)
        versao_confirmada = self.call("GET", base)["version"]
        repetida = self.call("POST", rota, 201, req, cenario="Reenvio contratual retorna mesma prova")
        self.p.check("Idempotência contratual preserva ID, hash e versão", repetida == prova
                     and self.call("GET", base)["version"] == versao_confirmada)
        diferente = copy.deepcopy(req); diferente["motivo"] += "; outra intenção"
        self.call("POST", rota, 409, diferente, cenario="Mesma chave contratual rejeita outra intenção")
        self.p.check("Prova contratual tem hash verificável e origem preservada", bool(re.fullmatch(r"[0-9a-f]{64}", prova["hashConteudo"]))
                     and prova["versaoAnteriorId"] == origem["id"] and prova["eventoOrigemId"] == eventos[0]["id"])
        self.guardar("contrato", dict(id=cid, rota=rota, origem=origem, eventos=eventos, prova=prova,
                     conhecido_antes=conhecido_antes, requisicao=req, documento_id=doc["id"], documento_sha256=sha(pdf),
                     versao_apos=versao_confirmada))
        self.contrato_verificar()

    def contrato_verificar(self):
        e = self.estado["contrato"]; base, rota = f'/api/contratos/{e["id"]}', e["rota"]
        fonte = self.h["fonte_contrato"]["fotografia"]
        atual = self.call("GET", query(base + "/posicao-temporal", data=self.h["data_contrato"]))["versaoContratual"]
        self.p.check("Fonte tardia recupera descrição e tipo sem perder identidade canônica",
                     atual["descricao"] == fonte["descricao"] and atual["tipoContratacaoDescricao"] == fonte["tipoContratacaoDescricao"]
                     and atual["numeroContrato"] == fonte["numeroContrato"] and atual["gestaoLocalId"] == fonte["gestaoLocalId"])
        antiga = self.call("GET", query(base + "/posicao-temporal", data=self.h["data_contrato"], conhecidoAte=e["conhecido_antes"]))
        self.p.check("Corte anterior à fonte continua desconhecendo a descrição", antiga["versaoContratual"]["descricao"] is None)
        self.p.check("Antes da incorporação não existia prova consultável", self.call("GET", query(rota, conhecidoAte=e["conhecido_antes"])) == [])
        origens = self.call("GET", base + "/versoes")
        self.p.check("Fotografia original permanece byte a byte no DTO da origem", next(v for v in origens if v["id"] == e["origem"]["id"]) == e["origem"])
        self.p.check("Prova não cria nem reescreve evento de publicação", self.call("GET", base + "/eventos") == e["eventos"])
        self.p.check("A lista persistida contém uma prova contratual e seu hash", self.call("GET", rota) == [e["prova"]])
        self.call("GET", rota, 404, perfil="outro", cenario="Outra Gestão não lista as provas contratuais")
        _, raw, headers = self.clientes["local"].call("GET", base + f'/documentos/{e["documento_id"]}/arquivo')
        self.p.check("Bytes da fonte física persistem com hash e proteção de download", sha(raw) == e["documento_sha256"]
                     and headers.get("X-Content-Type-Options") == "nosniff" and "attachment" in headers.get("Content-Disposition", ""))
        integridade = self.call("GET", base + f'/documentos/{e["documento_id"]}/evidencias-integridade')
        self.p.check("Cadeia documental continua íntegra", integridade["cadeiaIntegra"] is True
                     and all(x["hashConfere"] and x["encadeamentoConfere"] for x in integridade["eventos"]))
        self.call("GET", base + f'/documentos/{e["documento_id"]}/arquivo', 404, perfil="outro")

    def nome_criar(self):
        rota = f'/api/pessoas/{self.h["pessoa_id"]}/provas-nome'
        antes = self.call("GET", f'/api/pessoas/{self.h["pessoa_id"]}')
        eventos = self.call("GET", f'/api/vinculos-contratuais/{self.h["vinculo_id"]}/eventos')
        ausente = self.nome()
        self.p.check("Nome mestre não é certificado histórico", ausente["nome"] is None and ausente["comprovado"] is False)
        req = self.nome_req(self.h["nome_comprovado"])
        self.call("POST", rota + "/previa", 403, req, cenario="Concessão de outro contrato não autoriza a prova nominal")
        self.conceder(self.h["contrato_contexto_id"])
        self.negativas_apos_concessao(rota, req)
        conhecido_antes = agora()
        original = self.confirmar(rota, req)
        versoes = self.versoes_nome()
        repetida = self.call("POST", rota, 201, req)
        self.p.check("Reenvio nominal não cria outra prova nem consome versões", repetida["id"] == original["id"]
                     and repetida["reenvio"] is True and self.versoes_nome() == versoes)
        self.call("POST", rota, 409, dict(req, nomeComprovado="Outra intenção sintética"))
        self.p.check("Conflito de chave nominal não consome versões", self.versoes_nome() == versoes)
        retificacao = self.nome_req(self.h["nome_retificado"])
        self.call("POST", rota + "/previa", 409, retificacao, cenario="Nome conflitante exige relação explícita com a prova anterior")
        retificacao["provaAnteriorId"] = original["id"]
        nova = self.confirmar(rota, retificacao)
        self.guardar("nome", dict(rota=rota, mestre_antes=antes, eventos=eventos, prova=original, retificacao=nova,
                     conhecido_antes=conhecido_antes, requisicao=req, versoes_apos=self.versoes_nome()))
        self.nome_verificar()

    def nome_verificar(self):
        e = self.estado["nome"]
        atual = self.nome(); antigo = self.nome(conhecido=e["prova"]["registradoEm"])
        self.p.check("Nome retificado vale somente após seu conhecimento", atual["nome"] == self.h["nome_retificado"]
                     and atual["fonteId"] == e["retificacao"]["id"] and antigo["nome"] == self.h["nome_comprovado"]
                     and antigo["fonteId"] == e["prova"]["id"])
        self.p.check("Antes de receber a fonte o nome continua desconhecido", self.nome(conhecido=e["conhecido_antes"])["nome"] is None)
        for data in (dt.date.fromisoformat(self.h["data_inicio_contexto"]) - dt.timedelta(days=1),
                     dt.date.fromisoformat(self.h["data_fim_contexto"]) + dt.timedelta(days=1)):
            self.p.check("Fonte nominal não extrapola seu intervalo efetivo " + str(data), self.nome(data=str(data))["nome"] is None)
        mestre = self.call("GET", f'/api/pessoas/{self.h["pessoa_id"]}')
        self.p.check("Prova nominal preserva cadastro e CPF", mestre["nome"] == e["mestre_antes"]["nome"] and mestre["cpf"] == e["mestre_antes"]["cpf"])
        self.p.check("Nenhuma fotografia ou evento de vínculo foi reescrito", self.call("GET", f'/api/vinculos-contratuais/{self.h["vinculo_id"]}/eventos') == e["eventos"])
        provas = self.call("GET", query(e["rota"], vinculoId=self.h["vinculo_id"]))
        self.p.check("Duas fontes nominais persistem com relação explícita e original intacto", provas == [e["prova"], e["retificacao"]]
                     and e["retificacao"]["provaAnteriorId"] == e["prova"]["id"])
        self.call("GET", query(e["rota"], vinculoId=self.h["vinculo_id"]), 404, perfil="outro")
        minimizadas = self.call("GET", query(e["rota"], vinculoId=self.h["vinculo_id"]), perfil="nacional")
        self.p.check("Consulta nacional preserva versões sem narrativa ou origem documental privada", len(minimizadas) == 2
                     and all(p.get(k) is None for p in minimizadas for k in ("referenciaSei", "motivo", "autorizacao", "documentoComprobatorioId", "responsavelUsername")))

    def motivo_criar(self):
        rota = f'/api/motivos-desligamento/{self.h["motivo_id"]}/provas-historicas'
        mestre = self.call("GET", f'/api/motivos-desligamento/{self.h["motivo_id"]}')
        ausente = self.motivo()
        self.p.check("Disponibilidade e cadastro atual não inventam descrição ou categoria antiga",
                     ausente["comprovado"] is False and ausente["descricao"] is None and ausente["categoria"] is None)
        req = self.motivo_req(self.h["descricao_motivo_comprovada"], "DESLIGAMENTO_INDIVIDUAL")
        self.negativas_apos_concessao(rota, req)
        self.call("POST", rota + "/previa", 400, dict(req, categoriaComprovada=None), cenario="Fonte sem categoria não herda a categoria atual")
        conhecido_antes = agora()
        original = self.confirmar(rota, req)
        versoes = self.versoes_motivo()
        repetida = self.call("POST", rota, 201, req)
        self.p.check("Reenvio da causa preserva prova e versões", repetida["id"] == original["id"] and repetida["reenvio"] is True
                     and self.versoes_motivo() == versoes)
        self.call("POST", rota, 409, dict(req, categoriaComprovada="OUTRO"), cenario="Outra categoria não reutiliza a chave de idempotência")
        self.p.check("Conflito da causa não altera versões", self.versoes_motivo() == versoes)
        nova_req = self.motivo_req(self.h["descricao_motivo_retificada"], "FIM_VIGENCIA_CONTRATO")
        self.call("POST", rota + "/previa", 409, nova_req, cenario="Causa conflitante exige retificação explícita")
        nova_req["provaAnteriorId"] = original["id"]
        nova = self.confirmar(rota, nova_req)
        self.guardar("motivo", dict(rota=rota, mestre_antes=mestre, prova=original, retificacao=nova,
                     conhecido_antes=conhecido_antes, requisicao=req, versoes_apos=self.versoes_motivo()))
        self.motivo_verificar()

    def motivo_verificar(self):
        e = self.estado["motivo"]
        atual = self.motivo(); antigo = self.motivo(conhecido=e["prova"]["registradoEm"])
        self.p.check("Retificação contextual conserva verdade por data de conhecimento",
                     atual["descricao"] == self.h["descricao_motivo_retificada"] and atual["categoria"] == "FIM_VIGENCIA_CONTRATO"
                     and atual["fonteId"] == e["retificacao"]["id"] and antigo["descricao"] == self.h["descricao_motivo_comprovada"]
                     and antigo["categoria"] == "DESLIGAMENTO_INDIVIDUAL" and antigo["fonteId"] == e["prova"]["id"])
        self.p.check("Corte anterior continua sem texto ou categoria histórica", self.motivo(conhecido=e["conhecido_antes"])["comprovado"] is False)
        for data in (dt.date.fromisoformat(self.h["data_inicio_contexto"]) - dt.timedelta(days=1),
                     dt.date.fromisoformat(self.h["data_fim_contexto"]) + dt.timedelta(days=1)):
            self.p.check("A causa não excede o período documental " + str(data), self.motivo(data=str(data))["comprovado"] is False)
        mestre = self.call("GET", f'/api/motivos-desligamento/{self.h["motivo_id"]}')
        campos = ("id", "descricao", "categoria", "ativo", "gestaoLocalId", "criadoEm")
        self.p.check("Fonte histórica não reescreve mestre nem disponibilidade atual", all(mestre.get(k) == e["mestre_antes"].get(k) for k in campos))
        provas = self.call("GET", query(e["rota"], contratoId=self.h["contrato_contexto_id"]))
        self.p.check("Duas provas de causa persistem sem apagar a original", provas == [e["prova"], e["retificacao"]]
                     and e["retificacao"]["provaAnteriorId"] == e["prova"]["id"])
        self.call("GET", query(e["rota"], contratoId=self.h["contrato_contexto_id"]), 404, perfil="outro")
        minimizadas = self.call("GET", query(e["rota"], contratoId=self.h["contrato_contexto_id"]), perfil="nacional")
        self.p.check("Metadados da causa minimizam autoria, justificativa e referência privada", len(minimizadas) == 2
                     and all(p.get(k) is None for p in minimizadas for k in ("referenciaSei", "motivo", "autorizacao", "documentoComprobatorioId", "responsavelUsername")))

    def reinicio(self):
        self.estado = json.loads(pathlib.Path(self.a.estado).read_text())
        self.p.check("Reinício usa a mesma fixture comprovada", self.estado["fixture_sha256"] == sha(pathlib.Path(self.a.fixture).read_bytes()))
        self.contrato_verificar(); self.nome_verificar(); self.motivo_verificar()
        # Idempotência é relida do MySQL após perder memória do processo. A
        # primeira prova, ainda que posteriormente retificada, não é duplicada.
        for tipo in ("contrato", "nome", "motivo"):
            e = self.estado[tipo]
            recibo = self.call("POST", e["rota"], 201, e["requisicao"], cenario="Reenvio persistido após reinício: " + tipo)
            self.p.check("Reenvio após reinício conserva a origem de " + tipo, recibo["id"] == e["prova"]["id"])
        self.p.check("Reenvio após reinício não consome versão contratual", self.call("GET", f'/api/contratos/{self.estado["contrato"]["id"]}')["version"] == self.estado["contrato"]["versao_apos"])
        self.p.check("Reenvio após reinício não consome versões nominais", self.versoes_nome() == self.estado["nome"]["versoes_apos"])
        self.p.check("Reenvio após reinício não consome versões da causa", self.versoes_motivo() == self.estado["motivo"]["versoes_apos"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ("base", "fixture", "estado", "saida"):
        parser.add_argument("--" + option, required=True)
    parser.add_argument("--fase", required=True, choices=("criar", "reinicio"))
    args = parser.parse_args()
    if not re.fullmatch(r"http://(?:127\.0\.0\.1|localhost):[0-9]+/?", args.base):
        raise SystemExit("Este roteiro aceita somente JAR HTTP local explicitamente isolado.")
    fixture = json.loads(pathlib.Path(args.fixture).read_text())
    prova = Prova(args.saida, args.fase, pathlib.Path(__file__).name)
    try:
        audit = AuditoriaProvas(args, fixture, prova)
        if args.fase == "criar":
            if pathlib.Path(args.estado).exists():
                raise RuntimeError("Estado já existe: preserve a primeira execução e use a fase reinicio; não repita criação sobre prova parcial.")
            audit.contrato_criar(); audit.nome_criar(); audit.motivo_criar()
        else:
            audit.reinicio()
        prova.concluir()
        print(json.dumps(dict(fase=args.fase, sucesso=True, requisicoes=len(prova.requests), assercoes=len(prova.assertions)), ensure_ascii=False))
    except Exception:
        prova.salvar(erro=traceback.format_exc())
        raise


if __name__ == "__main__":
    main()
