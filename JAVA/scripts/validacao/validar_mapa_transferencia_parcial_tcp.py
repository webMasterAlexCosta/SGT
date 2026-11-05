#!/usr/bin/env python3
"""CON-023/CAT-004: grade -> alocação intermediária -> município, no JAR por TCP.

Requer PreparacaoMapaParcialTcpIT opt-in e o recibo PROCESSO.json produzido por
gerenciar_jar_tcp.py. Não inicia processos, não lê MySQL, não usa MockMvc e não
gera fonte a partir do mestre atual. Fonte SEI é suficiente; PDF não é exigido.
O estado privado contém tokens de prévia; os recibos públicos usam Prova/Cliente
com mascaramento. A fase reinicio exige o mesmo JAR/banco/Redis e novo processo.
"""
import argparse
import copy
import datetime as dt
import json
import pathlib
import re
import traceback
import uuid

from validar_jar_tcp import Cliente, Prova, cpf_novo, sha
from validar_provas_historicas_tcp import agora, canonico, privado, query


def instante(valor):
    return dt.datetime.fromisoformat(valor.replace("Z", "+00:00"))


def campos_snapshot(texto):
    """Lê o formato V2 auditável, respeitando ;, = e barras escapadas pelo servidor."""
    partes, atual, escapado = [], [], False
    for caractere in texto:
        if escapado:
            atual.extend(("\\", caractere))
            escapado = False
        elif caractere == "\\":
            escapado = True
        elif caractere == ";":
            partes.append("".join(atual))
            atual = []
        else:
            atual.append(caractere)
    if escapado:
        raise ValueError("Snapshot auditável termina com escape incompleto")
    partes.append("".join(atual))
    campos = {}
    for parte in partes:
        if not parte:
            continue
        chave, valor = parte.split("=", 1)
        if chave in campos:
            raise ValueError("Campo duplicado no snapshot auditável")
        campos[chave] = None if valor == r"\N" else re.sub(
            r"\\(.)", lambda m: {"r": "\r", "n": "\n"}.get(m[1], m[1]), valor)
    if campos.get("snapshotVersao") != "2":
        raise ValueError("O roteiro exige o formato de snapshot auditável V2")
    return campos


class AuditoriaMapaParcial:
    def __init__(self, args, fixture, prova):
        self.a, self.f, self.p = args, fixture, prova
        self.s = json.loads(fixture["fonte_preexistente_json"])
        self.cid = self.s["contrato_id"]
        self.base = f"/api/contratos/{self.cid}"
        self.rota = self.base + "/provas-historicas"
        self.p.check("Fixture e fonte declaram cenário exclusivamente sintético",
                     fixture.get("somente_dados_sinteticos") is True
                     and self.s.get("somente_dados_sinteticos") is True
                     and fixture["cenario_id"] == self.s["cenario_id"])
        fonte_bytes = pathlib.Path(fixture["arquivo_fonte_preexistente"]).read_bytes()
        self.p.check("Fonte anterior à lacuna conserva seus bytes e SHA-256",
                     fonte_bytes == fixture["fonte_preexistente_json"].encode("utf-8")
                     and sha(fonte_bytes) == fixture["fonte_preexistente_sha256"])
        self.p.check("Origem por tríade e fonte municipal divergem do mestre corrente",
                     self.s["mapa_documentado"]["composicaoOrigemId"] is None
                     and self.s["municipio"] != self.s["municipio_mestre_divergente"])
        processo = self.processo()
        self.estado = dict(fixture_sha256=sha(pathlib.Path(args.fixture).read_bytes()),
                           cenario_id=fixture["cenario_id"], fonte_sha256=sha(fonte_bytes),
                           processo_inicial=processo, etapa="inicio")
        if args.fase == "reinicio":
            self.estado = json.loads(pathlib.Path(args.estado).read_text())
            self.p.check("Fase anterior terminou e usa exatamente a mesma fixture/fonte",
                         self.estado.get("etapa") == "inicial_concluida"
                         and self.estado["fixture_sha256"] == sha(pathlib.Path(args.fixture).read_bytes())
                         and self.estado["fonte_sha256"] == sha(fonte_bytes)
                         and self.estado["cenario_id"] == fixture["cenario_id"])
            anterior = self.estado["processo_inicial"]
            chaves = ("jar_sha256", "database", "redis_host", "redis_port", "redis_database", "base_http")
            self.p.check("Reinício comprova mesmos bytes do JAR, banco, Redis e endereço",
                         all(processo[k] == anterior[k] for k in chaves))
            self.p.check("Supervisor registra parada anterior e uma nova inicialização pronta",
                         processo["inicio_utc"] != anterior["inicio_utc"]
                         and any(e.get("acao") == "encerrado" and e.get("pid") == anterior["pid"]
                                 and instante(e["utc"]) > instante(anterior["inicio_utc"])
                                 and instante(e["utc"]) < instante(processo["inicio_utc"])
                                 for e in processo["eventos"]))
        self.clientes = {}
        for perfil in ("local", "nacional", "admin"):
            c = Cliente(args.base, perfil, prova, fixture["senha"])
            c.login(fixture["usuarios"][perfil])
            self.clientes[perfil] = c

    def processo(self):
        bruto = json.loads(pathlib.Path(self.a.processo_jar).read_text())
        digest = sha(pathlib.Path(bruto["jar"]).read_bytes())
        self.p.check("Supervisor vincula este endpoint ao JAR e à base sintética da fixture",
                     bruto["base_http"] == self.a.base.rstrip("/")
                     and bruto["database"] == self.f["banco_sintetico"]
                     and bruto["sha256"] == digest)
        eventos = bruto.get("eventos", [])
        inicios = [e for e in eventos if e.get("acao") == "iniciado" and e.get("pid") == bruto["pid"]]
        self.p.check("Há recibo de inicialização do processo JAR externo", bool(inicios))
        inicio = inicios[-1]
        self.p.check("A inicialização atual está pronta e não foi encerrada",
                     any(e.get("acao") == "pronto" and e.get("pid") == bruto["pid"]
                         and instante(e["utc"]) >= instante(inicio["utc"]) for e in eventos)
                     and not any(e.get("acao") == "encerrado" and e.get("pid") == bruto["pid"]
                                 and instante(e["utc"]) >= instante(inicio["utc"]) for e in eventos))
        return {"jar_sha256": digest, "database": bruto["database"], "pid": bruto["pid"],
                "base_http": bruto["base_http"], "redis_host": bruto["redis_host"],
                "redis_port": bruto["redis_port"], "redis_database": bruto["redis_database"],
                "inicio_utc": inicio["utc"], "eventos": eventos}

    def call(self, method, path, status=200, body=None, perfil="local", **kwargs):
        return self.clientes[perfil].call(method, path, status, body=body, **kwargs)[0]

    def guardar(self, **valores):
        self.estado.update(valores)
        privado(self.a.estado, self.estado)

    def versao_contrato(self):
        return self.call("GET", self.base)["version"]

    def requisicao(self, propriedade, versao, anterior=None):
        item = dict(mapeamentoId=self.s["mapeamento_id"], municipioComprovado=False, jornadaComprovada=False)
        if propriedade == "grade":
            item.update(jornadaSemanal=self.s["jornada_semanal"], jornadaComprovada=True)
        elif propriedade == "municipio":
            item.update(lotacaoMunicipio=self.s["municipio"], municipioComprovado=True)
        else:
            raise ValueError("Propriedade desconhecida")
        return dict(eventoOrigemId=self.s["evento_transferencia_id"], versaoAnteriorId=versao,
                    provaAnteriorId=anterior, dataDocumento=self.s["data_transferencia"],
                    referenciaSei=self.s["referencia_sei_" + propriedade],
                    motivo="Fonte sintética preexistente comprova somente " + propriedade + " do mapa",
                    autorizacaoAdministrativa="ATO-SINTETICO-TCP-MAPA",
                    versaoContratoEsperada=self.versao_contrato(), chaveIdempotencia=uuid.uuid4().hex,
                    configuracaoCompletaComprovada=False, camposComprovados=[],
                    fotografia=copy.deepcopy(self.s["fotografia_transferencia"]), composicoes=[item])

    def incorporar(self, nome, req):
        previa = self.call("POST", self.rota + "/previa", body=req)
        self.p.check("Prévia vinculante devolve token para " + nome, bool(previa.get("tokenPrevia")))
        req.update(tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True)
        self.guardar(**{"requisicao_" + nome: req})
        recibo = self.call("POST", self.rota, 201, body=req)
        self.guardar(**{"prova_" + nome: recibo})
        self.p.check("Fonte parcial preserva identidade do evento e hash: " + nome,
                     recibo["eventoOrigemId"] == self.s["evento_transferencia_id"]
                     and recibo["configuracaoCompletaComprovada"] is False
                     and bool(re.fullmatch(r"[0-9a-f]{64}", recibo["hashConteudo"])))
        return recibo

    def relatorio(self):
        return self.call("GET", query("/api/relatorios/frequencia-consolidada",
                         inicio=self.s["dia_frequencia"], fim=self.s["dia_sem_frequencia"], contratoIds=self.cid))

    def dia(self, relatorio, data):
        linhas = [r for r in relatorio["registros"] if r["contratoId"] == self.cid
                  and r["lotacaoId"] == self.f["lotacao_destino_id"]
                  and r["jornadaId"] == self.f["jornada_destino_id"]
                  and r["cargoId"] == self.f["cargo_destino_id"]]
        self.p.check("Relatório identifica exatamente o vínculo sintético do destino", len(linhas) == 1)
        return linhas[0]["diario"][data]

    def frequencia(self):
        pagina = self.call("GET", query("/api/frequencias/intervalo", inicio=self.s["dia_frequencia"],
                          fim=self.s["dia_frequencia"], contratoId=self.cid, size=20))
        registros = [r for r in pagina["content"] if r["id"] == self.estado["frequencia_id"]]
        self.p.check("Uma frequência persistida pertence ao vínculo intermediário", len(registros) == 1
                     and registros[0]["vinculoContratualId"] == self.estado["vinculo_id"])
        return registros[0]

    def pendencias(self):
        pagina = self.call("GET", query("/api/pendencias", contratoIds=self.cid,
                          inicio=self.s["dia_frequencia"], fim=self.s["dia_sem_frequencia"], tamanho=100))
        self.p.check("Consulta cobre todas as pendências deste contrato isolado", pagina["ultima"] is True)
        return [p for p in pagina["registros"] if p["frequenciaId"] == self.estado["frequencia_id"]
                and p["estado"] != "RESOLVIDA"]

    def documentos(self):
        docs = self.call("GET", self.base + "/documentos")
        self.p.check("Publicação e transferência preservam as fontes SEI permitidas",
                     {d["tipoFato"] for d in docs} == {"CONTRATO_ORIGINAL", "TRANSFERENCIA_GESTAO"}
                     and all(d["estado"] == "PUBLICADO" and bool(d["referenciaSei"]) for d in docs))
        detalhes = []
        for doc in docs:
            rota = self.base + f'/documentos/{doc["id"]}'
            eventos = self.call("GET", rota + "/eventos")
            integridade = self.call("GET", rota + "/evidencias-integridade")
            self.p.check("Cadeia documental íntegra: " + str(doc["id"]), integridade["cadeiaIntegra"] is True
                         and bool(integridade["eventos"])
                         and all(e["hashConfere"] and e["encadeamentoConfere"] for e in integridade["eventos"]))
            detalhes.append(dict(documento=doc, eventos=eventos, integridade=integridade))
        return detalhes

    def criar(self):
        self.guardar(etapa="captura_original")
        mestre = self.call("GET", f'/api/lotacoes/{self.f["lotacao_destino_id"]}')
        self.p.check("Mestre consultado por HTTP contém município divergente da fonte histórica",
                     mestre["municipio"] == self.s["municipio_mestre_divergente"])
        versoes = self.call("GET", self.base + "/versoes")
        transferencias = self.call("GET", self.base + "/transferencias-gestao")
        self.p.check("Mapa publicado conserva a origem por tríade sem composição física",
                     len(transferencias) == 1 and len(transferencias[0]["mapeamentos"]) == 1
                     and transferencias[0]["mapeamentos"][0]["id"] == self.s["mapeamento_id"]
                     and transferencias[0]["mapeamentos"][0]["composicaoOrigemId"] is None)
        por_id = {v["id"]: v for v in versoes}
        self.p.check("Fotografias da publicação e transferência coincidem com a fonte preservada",
                     por_id[self.s["versao_publicacao_id"]] == self.s["versao_original_publicacao"]
                     and por_id[self.s["versao_transferencia_id"]] == self.s["versao_original_transferencia"])
        self.guardar(originais=dict(versoes=versoes, transferencias=transferencias,
                     eventos=self.call("GET", self.base + "/eventos"), documentos=self.documentos()))
        self.p.check("Nenhuma prova foi preparada fora do JAR", self.call("GET", self.rota) == [])
        self.call("POST", "/api/permissoes-excepcionais", 201, perfil="admin", body=dict(
            tipo="INCORPORACAO_PROVA_HISTORICA", usuarioBeneficiarioId=self.f["usuarios_ids"]["local"],
            contratoId=self.cid, finalidade="Conferência TCP das propriedades parciais do mapa",
            justificativa="Fontes sintéticas anteriores à perda simulada, separadas por propriedade",
            autorizacaoAdministrativa="ATO-SINTETICO-TCP-MAPA",
            validoAte=(dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=12)).isoformat()))
        grade_req = self.requisicao("grade", self.s["versao_transferencia_id"])
        self.call("POST", self.rota + "/previa", 400, body=dict(grade_req, referenciaSei=None),
                  cenario="Justificativa sem referência/documento não comprova o passado")
        self.call("POST", self.rota + "/previa", 403, body=grade_req, perfil="nacional",
                  cenario="Perfil nacional consultivo não incorpora prova negocial")
        self.guardar(k0=agora())
        grade = self.incorporar("grade", grade_req)
        self.p.check("Primeira fonte comprova somente grade", grade["numeroRevisao"] == 1
                     and grade["provaAnteriorId"] is None)
        vinculo = self.call("POST", "/api/vinculos-contratuais", 201, body=dict(
            contratoId=self.cid, composicaoId=self.s["composicao_base_id"],
            lotacaoId=self.f["lotacao_destino_id"], jornadaId=self.f["jornada_destino_id"],
            cargoId=self.f["cargo_destino_id"], dataInicio=self.s["dia_frequencia"],
            pessoa=dict(nome="Pessoa sintética TCP entre provas do mapa", cpf=cpf_novo())))
        vid = vinculo["id"]
        alocacoes = self.call("GET", f"/api/vinculos-contratuais/{vid}/alocacoes")
        self.p.check("A alocação intermediária usa a composição canônica e a tríade do destino",
                     len(alocacoes) == 1 and alocacoes[0]["composicaoBaseId"] == self.s["composicao_base_id"]
                     and alocacoes[0]["lotacaoId"] == self.f["lotacao_destino_id"]
                     and alocacoes[0]["jornadaId"] == self.f["jornada_destino_id"]
                     and alocacoes[0]["cargoId"] == self.f["cargo_destino_id"])
        self.guardar(vinculo_id=vid, pessoa_id=vinculo["pessoaId"], alocacoes_originais=alocacoes,
                     eventos_vinculo=self.call("GET", f"/api/vinculos-contratuais/{vid}/eventos"),
                     eventos_alocacao=self.call("GET", f"/api/vinculos-contratuais/{vid}/alocacoes/eventos"))
        freq = self.call("POST", "/api/frequencias", body=dict(vinculoContratualId=vid,
                         dataReferencia=self.s["dia_frequencia"], status="PRESENCA",
                         justificativaRetificacao="Lançamento gerencial sintético após o prazo normal com a grade V1",
                         chaveIdempotencia="tcp-mapa-frequencia-" + uuid.uuid4().hex))
        self.guardar(frequencia_id=freq["id"])
        original = self.frequencia()
        eventos_freq = self.call("GET", f'/api/frequencias/{freq["id"]}/eventos')
        self.p.check("Frequência já nasce fechada e válida segundo a janela normal do sistema",
                     original["estadoNegocial"] == "FECHADA" and original["validadeOperacional"] == "VALIDA"
                     and len(eventos_freq) == 1)
        snapshot = campos_snapshot(eventos_freq[0]["snapshotNovo"])
        self.p.check("Fotografia auditável usa grade comprovada e conserva município desconhecido",
                     snapshot["lotacaoMunicipio"] is None and snapshot["minutosFisicos"] == "480"
                     and snapshot["jornadaEntrada1"] == "08:00" and snapshot["jornadaSaida1"] == "12:00"
                     and snapshot["jornadaEntrada2"] == "13:00" and snapshot["jornadaSaida2"] == "17:00"
                     and snapshot["autorOriginalId"] == str(self.f["usuarios_ids"]["local"]))
        relatorio = self.relatorio()
        dia_sem = self.dia(relatorio, self.s["dia_sem_frequencia"])
        dia_original = self.dia(relatorio, self.s["dia_frequencia"])
        self.p.check("Antes da prova municipal, a frequência válida integra o consolidado com 480 minutos",
                     dia_original["lancado"] is True and dia_original["status"] == "PRESENCA"
                     and dia_original["minutosFisicos"] == 480
                     and dia_original["minutosPagaveis"] == 480
                     and relatorio["totais"]["diasLancados"] == 1
                     and relatorio["totais"]["minutosFisicos"] == 480)
        self.p.check("Grade V1 prevê 480 minutos sem fabricar município/feriado",
                     dia_sem["lancado"] is False and dia_sem["status"] == "SEM_LANCAMENTO"
                     and dia_sem["minutosPrevistos"] == 480)
        self.p.check("Antes da fonte municipal não há pendência nem aviso de revisão",
                     self.pendencias() == [] and relatorio["revisao"]["sujeitoARevisao"] is False)
        emissao = self.call("POST", "/api/relatorios/administrativos/emissoes", 201, body=dict(
            tipo="FREQUENCIA_CONSOLIDADA", formato="VISUALIZACAO", inicio=self.s["dia_frequencia"],
            fim=self.s["dia_sem_frequencia"], contratoIds=[self.cid], chaveIdempotencia=uuid.uuid4().hex))
        self.p.check("Emissão intermediária documenta a visão anterior à fonte municipal",
                     emissao["metadados"]["estado"] == "VIGENTE"
                     and self.dia(emissao["conteudo"], self.s["dia_sem_frequencia"])["minutosPrevistos"] == 480)
        self.guardar(frequencia_original=original, eventos_frequencia_originais=eventos_freq,
                     snapshot_frequencia_original=snapshot, emissao_original=emissao, k_entre=agora())
        municipio_req = self.requisicao("municipio", grade["versaoGeradaId"], grade["id"])
        self.call("POST", self.rota + "/previa", 409, body=dict(municipio_req, provaAnteriorId=None),
                  cenario="Segunda propriedade exige relação explícita com a fonte anterior")
        municipio = self.incorporar("municipio", municipio_req)
        self.p.check("Município tardio é revisão explícita que não repete a grade",
                     municipio["numeroRevisao"] == 2 and municipio["provaAnteriorId"] == grade["id"]
                     and municipio["versaoAnteriorId"] == grade["versaoGeradaId"]
                     and "jornadaSemanal" not in municipio_req["composicoes"][0]
                     and municipio_req["composicoes"][0]["jornadaComprovada"] is False)
        self.verificar()
        self.guardar(etapa="inicial_concluida", versao_contrato_apos=self.versao_contrato())

    def verificar(self):
        e, grade, municipio = self.estado, self.estado["prova_grade"], self.estado["prova_municipio"]
        self.p.check("Ordem de conhecimento é fonte anterior, K0, grade, alocação/frequência, município",
                     instante(self.s["capturada_antes_da_lacuna_em"]) < instante(e["k0"])
                     < instante(grade["registradoEm"]) < instante(e["k_entre"]) < instante(municipio["registradoEm"]))
        cortes = [(e["k0"], []), (grade["registradoEm"], [grade]),
                  (e["k_entre"], [grade]), (municipio["registradoEm"], [grade, municipio])]
        for k, esperado in cortes:
            provas = self.call("GET", query(self.rota, conhecidoAte=k))
            self.p.check("Corte K conserva exatamente as fontes já conhecidas: " + k, provas == esperado)
            posicao = self.call("GET", query(self.base + "/posicao-temporal", data=self.s["dia_frequencia"], conhecidoAte=k))
            # Esta rota devolve uma projeção composta, com id=null; não promete
            # expor a linha física da versão. Os IDs das fontes vêm da cadeia.
            foto = posicao["versaoContratual"]
            self.p.check("Posição temporal preserva evento, identidade e Gestão efetiva naquele K: " + k,
                         foto["eventoOrigemId"] == self.s["evento_transferencia_id"]
                         and foto["gestaoLocalId"] == self.f["gestao_destino_id"]
                         and foto["numeroContrato"] == self.s["fotografia_transferencia"]["numeroContrato"])
        self.p.check("Cadeia atual contém somente as duas fontes", self.call("GET", self.rota) == [grade, municipio])
        versoes = self.call("GET", self.base + "/versoes")
        por_id = {v["id"]: v for v in versoes}
        self.p.check("Publicação original e versão original da transferência permanecem integrais",
                     len(versoes) == len(e["originais"]["versoes"]) + 2
                     and all(por_id.get(v["id"]) == v for v in e["originais"]["versoes"]))
        self.p.check("Cada prova referencia sua versão física criada somente no instante de conhecimento",
                     all(por_id[p["versaoGeradaId"]]["eventoOrigemId"] == self.s["evento_transferencia_id"]
                         and instante(por_id[p["versaoGeradaId"]]["registradoEm"]) == instante(p["registradoEm"])
                         for p in (grade, municipio)))
        self.p.check("Provas não reescrevem mapas nem eventos negociais originais",
                     self.call("GET", self.base + "/transferencias-gestao") == e["originais"]["transferencias"]
                     and self.call("GET", self.base + "/eventos") == e["originais"]["eventos"])
        self.p.check("Documentos SEI, autoria e cadeias originais permanecem idênticos",
                     self.documentos() == e["originais"]["documentos"])
        self.p.check("Prova histórica não altera o município corrente do catálogo",
                     self.call("GET", f'/api/lotacoes/{self.f["lotacao_destino_id"]}')["municipio"]
                     == self.s["municipio_mestre_divergente"])
        vid = e["vinculo_id"]
        self.p.check("DTOs e eventos originais da alocação/vínculo permanecem idênticos",
                     self.call("GET", f"/api/vinculos-contratuais/{vid}/alocacoes") == e["alocacoes_originais"]
                     and self.call("GET", f"/api/vinculos-contratuais/{vid}/alocacoes/eventos") == e["eventos_alocacao"]
                     and self.call("GET", f"/api/vinculos-contratuais/{vid}/eventos") == e["eventos_vinculo"])
        freq = self.frequencia()
        self.p.check("Município tardio atinge frequência fechada sem reabrir ou recalcular o original",
                     freq["estadoNegocial"] == "FECHADA" and freq["validadeOperacional"] == "SUJEITA_REVISAO"
                     and freq["estadoAuditoria"] == "SUJEITA_REVISAO" and freq["bloqueadaEdicaoOperacional"] is True)
        eventos = self.call("GET", f'/api/frequencias/{e["frequencia_id"]}/eventos')
        originais = e["eventos_frequencia_originais"]
        self.p.check("A revisão acrescenta exatamente um evento e conserva o primeiro integralmente",
                     eventos[:len(originais)] == originais and len(eventos) == len(originais) + 1
                     and eventos[-1]["tipoEvento"] == "IMPACTO_TEMPORAL")
        depois = campos_snapshot(eventos[-1]["snapshotNovo"])
        mutaveis = {"validade", "auditoria", "bloqueada", "versaoNegocial", "motivoRevisao"}
        antes = e["snapshot_frequencia_original"]
        self.p.check("Cálculo, município original, horários, autoria e identidades fotografados não são reescritos",
                     {k: v for k, v in depois.items() if k not in mutaveis}
                     == {k: v for k, v in antes.items() if k not in mutaveis})
        integridade = self.call("GET", f'/api/frequencias/{e["frequencia_id"]}/evidencias-integridade')
        self.p.check("Cadeia integral de frequência permanece verificável", integridade["cadeiaIntegra"] is True
                     and integridade["estado"] == "INTEGRA" and len(integridade["eventos"]) == len(eventos))
        pendencias = self.pendencias()
        self.p.check("Há uma única pendência da frequência e responsabilidade da Gestão destino",
                     len(pendencias) == 1 and pendencias[0]["contratoId"] == self.cid
                     and pendencias[0]["vinculoId"] == vid
                     and pendencias[0]["gestaoLocalId"] == self.f["gestao_destino_id"])
        relatorio = self.relatorio()
        dia_sem = self.dia(relatorio, self.s["dia_sem_frequencia"])
        dia_original = self.dia(relatorio, self.s["dia_frequencia"])
        self.p.check("Município comprovado chega ao calendário do dia sem lançamento",
                     dia_sem["status"] == "FERIADO" and dia_sem["minutosPrevistos"] == 0
                     and dia_sem["lancado"] is False)
        self.p.check("Relatório sinaliza exatamente a pendência da frequência sujeita a revisão",
                     relatorio["revisao"]["sujeitoARevisao"] is True
                     and relatorio["revisao"]["quantidadePendencias"] == 1)
        # LEVANTAMENTO 5.22/5.23, FRE-009/011/019 e REL-001/004 distinguem
        # memória histórica e cálculo vigente. O consolidado inclui só VALIDA;
        # a fotografia de 480 minutos continua exigida abaixo e na cadeia acima.
        self.p.check("Consolidado vigente exclui a frequência pendente e aplica o calendário comprovado",
                     dia_original["lancado"] is False and dia_original["status"] == "FERIADO"
                     and dia_original["minutosPrevistos"] == 0
                     and dia_original["minutosFisicos"] == 0 and dia_original["minutosPagaveis"] == 0
                     and relatorio["totais"]["diasLancados"] == 0
                     and relatorio["totais"]["minutosPrevistos"] == 0
                     and relatorio["totais"]["minutosFisicos"] == 0
                     and relatorio["totais"]["minutosPagaveis"] == 0,
                     esperado={"lancado": False, "status": "FERIADO", "minutosFisicos": 0},
                     observado={k: dia_original[k] for k in ("lancado", "status", "minutosFisicos")})
        self.p.check("A exclusão do consolidado não apaga os 480 minutos da memória histórica",
                     antes["minutosFisicos"] == depois["minutosFisicos"] == "480")
        emissao = self.call("GET", f'/api/relatorios/administrativos/emissoes/{e["emissao_original"]["metadados"]["id"]}')
        original = e["emissao_original"]
        self.p.check("Emissão antiga é superada conservando conteúdo, hash e corte de conhecimento",
                     emissao["metadados"]["estado"] == "SUPERADA"
                     and emissao["metadados"]["hashConteudo"] == original["metadados"]["hashConteudo"]
                     and emissao["metadados"]["fonteConhecidaAte"] == original["metadados"]["fonteConhecidaAte"]
                     and canonico(emissao["conteudo"]) == canonico(original["conteudo"]))

    def reinicio(self):
        antes = self.versao_contrato()
        self.p.check("Versão contratual persiste após encerramento do primeiro processo",
                     antes == self.estado["versao_contrato_apos"])
        self.verificar()
        for nome in ("grade", "municipio"):
            recibo = self.call("POST", self.rota, 201, body=self.estado["requisicao_" + nome],
                              cenario="Reenvio persistido após reinício: " + nome)
            self.p.check("Reenvio não duplica nem modifica a fonte " + nome,
                         recibo == self.estado["prova_" + nome])
        self.p.check("Reenvios não consomem nova versão do contrato", self.versao_contrato() == antes)
        self.verificar()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for opcao in ("base", "fixture", "estado", "saida", "processo-jar"):
        parser.add_argument("--" + opcao, required=True)
    parser.add_argument("--fase", choices=("criar", "reinicio"), required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"http://(?:127\.0\.0\.1|localhost):[0-9]+/?", args.base):
        raise SystemExit("O roteiro aceita somente o JAR HTTP local explicitamente isolado")
    saida, estado = pathlib.Path(args.saida).resolve(), pathlib.Path(args.estado).resolve()
    if estado.is_relative_to(saida) or pathlib.Path(args.fixture).resolve().is_relative_to(saida):
        raise SystemExit("Estado e fixture privados devem ficar fora da pasta de recibos públicos")
    if (saida / (args.fase + ".json")).exists():
        raise SystemExit("Recibo desta fase já existe; preserve a execução anterior")
    if args.fase == "criar" and estado.exists():
        raise SystemExit("Estado já existe: preserve inclusive execuções parciais; não recrie sobre elas")
    if args.fase == "reinicio" and not estado.is_file():
        raise SystemExit("A fase reinicio exige o estado privado da fase criar concluída")
    fixture = json.loads(pathlib.Path(args.fixture).read_text())
    prova = Prova(args.saida, args.fase, str(pathlib.Path(__file__).resolve()))
    try:
        auditoria = AuditoriaMapaParcial(args, fixture, prova)
        if args.fase == "criar":
            auditoria.criar()
        else:
            auditoria.reinicio()
        prova.concluir()
        print(json.dumps(dict(fase=args.fase, sucesso=True, requisicoes=len(prova.requests),
                              assercoes=len(prova.assertions)), ensure_ascii=False))
    except Exception:
        prova.salvar(erro=traceback.format_exc())
        raise


if __name__ == "__main__":
    main()
