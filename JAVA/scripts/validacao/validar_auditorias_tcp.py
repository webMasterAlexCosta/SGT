#!/usr/bin/env python3
"""Provas adicionais sobre o JAR externo, por HTTP/TCP e sem acesso ao código Java.

Requer a fixture JPA sintética de PreparacaoHttpTcpIT e o PDF produzido pelo
roteiro validar_jar_tcp.py. A fase reinicio exige o MESMO JAR e banco preservado.
Não contém DDL, não inicia banco/servidor e não substitui testes de integração.
"""
import argparse
import concurrent.futures
import copy
import datetime as dt
import io
import json
import pathlib
import threading
import time
import traceback
import urllib.parse
import uuid
import xml.etree.ElementTree as ET
import zipfile
from zoneinfo import ZoneInfo

from validar_jar_tcp import Cliente, Prova, cpf_novo, sha


def query(path, **params):
    return path + "?" + urllib.parse.urlencode(params)


class Auditoria:
    def __init__(self, args, fixture, prova):
        self.args, self.f, self.p = args, fixture, prova
        self.hoje = dt.datetime.now(ZoneInfo("America/Sao_Paulo")).date()
        self.clientes = {}
        for papel, matricula in fixture["usuarios"].items():
            cliente = Cliente(args.base, papel, prova, fixture["senha"])
            cliente.login(matricula)
            self.clientes[papel] = cliente
        self.estado = {}

    def call(self, papel, method, path, status=200, body=None, **kwargs):
        return self.clientes[papel].call(method, path, status, body=body, **kwargs)[0]

    def guardar(self, nome, valor):
        self.estado[nome] = valor
        pathlib.Path(self.args.estado).write_text(json.dumps(self.estado, ensure_ascii=False, indent=2))

    def aguardar(self, papel, rota, descricao, terminal, prazo=45):
        limite = time.monotonic() + prazo
        while True:
            atual = self.call(papel, "GET", rota)
            self.p.check(descricao + ": processamento sem falha",
                         atual.get("status") not in ("FALHA", "ERRO"), observado=atual)
            if terminal(atual):
                return atual
            if time.monotonic() >= limite:
                self.p.check(descricao + ": conclusão dentro do prazo", False, prazo, atual)
            time.sleep(0.15)

    def dia_util_recente(self, dias=1):
        dia = self.hoje - dt.timedelta(days=dias)
        while dia.weekday() >= 5:
            dia -= dt.timedelta(days=1)
        return dia

    def linhas_negociais_xlsx(self, arquivo, nome):
        self.p.check("Download é um ZIP XLSX válido", zipfile.is_zipfile(io.BytesIO(arquivo)))
        ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
        linhas = []
        with zipfile.ZipFile(io.BytesIO(arquivo)) as z:
            self.p.check("Todas as entradas ZIP conferem CRC", z.testzip() is None)
            self.p.check("Pacote contém workbook e tipos OOXML",
                         {"[Content_Types].xml", "xl/workbook.xml"}.issubset(z.namelist()))
            compartilhadas = []
            if "xl/sharedStrings.xml" in z.namelist():
                compartilhadas = ["".join(i.itertext()) for i in
                                   ET.fromstring(z.read("xl/sharedStrings.xml")).findall("s:si", ns)]
            for planilha in sorted(n for n in z.namelist()
                                   if n.startswith("xl/worksheets/sheet") and n.endswith(".xml")):
                for row in ET.fromstring(z.read(planilha)).findall(".//s:row", ns):
                    valores = []
                    for cell in row.findall("s:c", ns):
                        if cell.get("t") == "s":
                            valor = compartilhadas[int(cell.findtext("s:v", default="0", namespaces=ns))]
                        elif cell.get("t") == "inlineStr":
                            valor = "".join(cell.find("s:is", ns).itertext())
                        else:
                            valor = cell.findtext("s:v", default="", namespaces=ns)
                        valores.append([cell.get("r", "").rstrip("0123456789"), valor])
                    if any(nome in valor for _, valor in valores):
                        linhas.append(valores)
        self.p.check("XLSX contém a identidade fotografada da fonte sintética", bool(linhas), nome, linhas)
        return linhas

    def renovar_sessao_local(self, perfil, gestao_id):
        cliente = Cliente(self.args.base, "local/" + perfil, self.p, self.f["senha"])
        cliente.login(self.f["usuarios"]["local"])
        self.clientes["local"] = cliente
        atual = self.call("local", "GET", "/api/usuarios/meu-perfil")
        self.p.check("Nova sessão confirma perfil e escopo persistidos por HTTP",
                     atual["id"] == self.f["usuarios_ids"]["local"] and atual["perfil"] == perfil
                     and (atual.get("gestaoLocal") or {}).get("id") == gestao_id,
                     dict(perfil=perfil, gestao_id=gestao_id), atual)

    def exportacao_autorizacao(self, referencia=None):
        if referencia is None:
            c = self.contrato()
            req = self.vinculo_req(c)
            v = self.call("local", "POST", "/api/vinculos-contratuais", 201, req)
            dia = self.dia_util_recente()
            freq = self.frequencia(v["id"], dia, "FALTA")
            referencia = dict(contrato=c, vinculo=v["id"], frequencia=freq["id"],
                              data=str(dia), nome=req["pessoa"]["nome"], fases={})
        provas_fase = []
        restaurar = dict(perfil="GESTOR_LOCAL", gestaoLocalId=self.f["gestao_id"],
                         superintendenciaId=self.f["superintendencia_id"])
        for mudanca in ("GESTAO", "PERFIL_MINIMIZADO"):
            solicitacao = self.call("local", "POST", query(
                "/api/relatorios/frequencia-consolidada/exportacao/solicitacao",
                inicio=referencia["data"], fim=referencia["data"], contratoIds=referencia["contrato"]["id"],
                formato="XLSX", totalEstimado=1), 202)
            rota = "/api/relatorios/exportacoes/" + solicitacao["id"]
            pronta = self.aguardar("local", rota, "Exportação real pronta antes de alterar autorização",
                                   lambda d: d.get("status") == "CONCLUIDO" and d.get("podeBaixar"))
            self.p.check("Arquivo pronto declara tamanho positivo", pronta["tamanhoBytes"] > 0)
            alterado = (dict(perfil="GESTOR_LOCAL", gestaoLocalId=self.f["gestao_destino_id"],
                             superintendenciaId=self.f["superintendencia_destino_id"])
                        if mudanca == "GESTAO" else dict(perfil="PRESIDENTE"))
            rota_usuario = f'/api/usuarios/{self.f["usuarios_ids"]["local"]}'
            try:
                self.call("admin", "PUT", rota_usuario, body=alterado,
                          cenario="Admin altera autorização persistida do proprietário: " + mudanca)
                self.call("local", "GET", rota, 401,
                          cenario="Alteração administrativa revoga a sessão anterior do beneficiário")
                self.renovar_sessao_local(alterado["perfil"], alterado.get("gestaoLocalId"))
                self.call("local", "GET", rota, 403,
                          cenario="Proprietário autenticado com nova política não consulta metadados do XLSX antigo")
                _, recusado, headers = self.clientes["local"].call("GET", rota + "/arquivo", 403,
                    cenario="Nova política recusa os bytes do arquivo pronto: " + mudanca)
                self.p.check("Recusa contém erro e nenhum pacote XLSX ou disposição de download",
                             not zipfile.is_zipfile(io.BytesIO(recusado)) and not recusado.startswith(b"PK")
                             and "spreadsheetml" not in headers.get("Content-Type", "")
                             and "attachment" not in headers.get("Content-Disposition", ""))
            finally:
                # Restauração também usa a API; nenhum oráculo consulta JPA/SQL.
                self.call("admin", "PUT", rota_usuario, body=restaurar,
                          cenario="Restabelecer o contexto legítimo do usuário sintético")
                self.renovar_sessao_local("GESTOR_LOCAL", self.f["gestao_id"])
            conservada = self.call("local", "GET", rota)
            campos = ("id", "status", "nomeArquivo", "formato", "tamanhoBytes", "iniciadoEm", "atualizadoEm")
            self.p.check("Negativas não consomem nem substituem a tarefa ou o arquivo pronto",
                         all(conservada.get(k) == pronta.get(k) for k in campos)
                         and conservada.get("podeBaixar") is True, pronta, conservada)
            _, arquivo, headers = self.clientes["local"].call("GET", rota + "/arquivo",
                cenario="Contexto restaurado baixa a mesma tarefa anteriormente recusada")
            self.p.check("Download autorizado tem tamanho anunciado e tipo XLSX",
                         len(arquivo) == pronta["tamanhoBytes"]
                         and "spreadsheetml" in headers.get("Content-Type", ""))
            linhas = self.linhas_negociais_xlsx(arquivo, referencia["nome"])
            hash_linhas = sha(json.dumps(linhas, ensure_ascii=False, sort_keys=True).encode())
            if referencia.get("hash_linhas_negociais"):
                self.p.check("Nova geração preserva os dados negociais da fonte, inclusive após reinício",
                             hash_linhas == referencia["hash_linhas_negociais"],
                             referencia["hash_linhas_negociais"], hash_linhas)
            else:
                referencia["hash_linhas_negociais"] = hash_linhas
            caminho = self.p.pasta / f'exportacao-{self.p.fase}-{mudanca}-{solicitacao["id"]}.xlsx'
            caminho.write_bytes(arquivo)
            self.p.check("Artefato baixado conserva SHA-256 ao ser arquivado", sha(caminho.read_bytes()) == sha(arquivo))
            baixada = self.aguardar("local", rota, "Download autorizado finaliza a tarefa",
                                    lambda d: d.get("status") == "BAIXADO")
            self.p.check("Consumo ocorre somente depois da transferência autorizada", not baixada["podeBaixar"])
            self.call("local", "GET", rota + "/arquivo", 409,
                      cenario="Tarefa consumida não entrega um segundo arquivo")
            provas_fase.append(dict(mudanca=mudanca, tarefa=solicitacao["id"], tamanho=len(arquivo),
                                   sha256=sha(arquivo), arquivo=str(caminho), hash_linhas_negociais=hash_linhas))
            referencia["fases"][self.p.fase] = provas_fase
            self.guardar("exportacao_autorizacao", referencia)

    def contrato(self, capacidade=3, numero=None):
        inicio, fim = self.hoje - dt.timedelta(days=40), self.hoje + dt.timedelta(days=120)
        dados = dict(numeroContrato=numero or "AUDTCP-" + uuid.uuid4().hex[:10].upper(),
                     anoContrato=inicio.year, descricao="Contrato sintético da prova TCP ampliada",
                     gestaoLocalId=self.f["gestao_id"], tipoContratacaoId=self.f["tipo_id"],
                     cnpjEmpresa="12345678000190", razaoSocialEmpresa="Empresa sintética TCP",
                     nomeFantasiaEmpresa="TCP", dataInicio=str(inicio), dataFim=str(fim), efetivoPrevisto=capacidade)
        c = self.call("local", "POST", "/api/contratos", 201, dados)
        base = f'/api/contratos/{c["id"]}'
        comp = self.call("local", "POST", base + "/composicoes", 201,
                         dict(lotacaoId=self.f["lotacao_id"], jornadaId=self.f["jornada_id"],
                              cargoId=self.f["cargo_id"], efetivoPrevisto=capacidade))
        req = dict(dataPublicacao=str(self.hoje), referenciaSei="SEI-AUDTCP",
                   justificativa="Fonte documental sintética para publicação retroativa")
        previa = self.call("local", "POST", base + "/previas/publicar", body=req)
        self.call("local", "POST", base + "/publicar", body=dict(req, tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True))
        return dict(id=c["id"], composicao=comp["id"], numero=dados["numeroContrato"], ano=inicio.year,
                    inicio=str(inicio), fim=str(fim))

    def vinculo_req(self, c, inicio=None, fim=None):
        dados = dict(pessoa=dict(nome="Pessoa sintética TCP " + uuid.uuid4().hex[:10], cpf=cpf_novo()),
                     contratoId=c["id"], composicaoId=c["composicao"], dataInicio=str(inicio or c["inicio"]),
                     matricula="AUDTCP-" + uuid.uuid4().hex[:10])
        if fim is not None:
            dados["dataFim"] = str(fim)
        return dados

    def conceder(self, c, tipo):
        self.call("admin", "POST", "/api/permissoes-excepcionais", 201,
                  dict(tipo=tipo, usuarioBeneficiarioId=self.f["usuarios_ids"]["local"], contratoId=c["id"],
                       finalidade="Auditoria TCP", justificativa="Prova autorizada sobre base sintética",
                       autorizacaoAdministrativa="ATO-AUDTCP",
                       validoAte=(dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=2)).isoformat()))

    def incorporar_causa_sintetica(self, c, categoria):
        """Fonte contextual explícita; disponibilidade do cadastro não prova seu texto passado."""
        self.conceder(c, "INCORPORACAO_PROVA_HISTORICA")
        motivo_id = self.f["motivos"][categoria]
        motivo = self.call("local", "GET", f"/api/motivos-desligamento/{motivo_id}")
        contrato = self.call("local", "GET", f'/api/contratos/{c["id"]}')
        rota = f"/api/motivos-desligamento/{motivo_id}/provas-historicas"
        req = dict(contratoId=c["id"], descricaoComprovada="Causa sintética TCP " + categoria,
                   categoriaComprovada=categoria, dataInicioEfetiva=c["inicio"], dataFimEfetiva=c["fim"],
                   referenciaSei="SEI-SINTETICO-TCP-CAUSA-" + categoria,
                   motivo="Fonte sintética explícita do cenário de causa contratual retroativa",
                   autorizacao="ATO-SINTETICO-TCP-CAUSA", versaoMotivoEsperada=motivo["version"],
                   versaoContratoEsperada=contrato["version"], chaveIdempotencia=uuid.uuid4().hex)
        previa = self.call("local", "POST", rota + "/previa", body=req)
        prova = self.call("local", "POST", rota, 201,
                          dict(req, confirmarImpacto=True, tokenPrevia=previa["tokenPrevia"]))
        contexto = self.call("local", "GET", query(f"/api/motivos-desligamento/{motivo_id}/contexto-historico",
                                                   contratoId=c["id"], data=c["inicio"]))
        self.p.check("Causa retroativa tem fonte contextual contratual explícita antes de anular",
                     contexto["comprovado"] and contexto["categoria"] == categoria
                     and contexto["fonteId"] == prova["id"])
        return prova

    def transferir(self, c, data):
        return self.call("local", "POST", f'/api/contratos/{c["id"]}/transferir-gestao', body=dict(
            dataEfetiva=str(data), dataAto=str(self.hoje), gestaoLocalDestinoId=self.f["gestao_destino_id"],
            tipoContratacaoDestinoId=self.f["tipo_destino_id"], motivo="Responsabilidade documentada por ato TCP",
            referenciaSei="SEI-AUDTCP-TRANSFERENCIA", autorizacaoAdministrativa="ATO-AUDTCP",
            mapeamentos=[dict(composicaoOrigemId=c["composicao"], lotacaoDestinoId=self.f["lotacao_destino_id"],
                              jornadaDestinoId=self.f["jornada_destino_id"], cargoDestinoId=self.f["cargo_destino_id"])]))

    def posicao(self, vid, data):
        return self.call("nacional", "GET", query(f"/api/vinculos-contratuais/{vid}/alocacoes/posicao", data=data))

    def frequencia(self, vid, dia, status="PRESENCA", papel="local", chave=None):
        body = dict(vinculoContratualId=vid, dataReferencia=str(dia), status=status,
                    justificativaRetificacao="Conferência documental sintética TCP",
                    chaveIdempotencia=chave or "audtcp-" + uuid.uuid4().hex)
        if status == "PRESENCA":
            body.update(horasTrabalhadas="04:00", horarioSaida="12:00")
        return self.call(papel, "POST", "/api/frequencias", body=body)

    def futura_e_catalogo(self):
        c = self.contrato()
        data = self.hoje + dt.timedelta(days=7)
        self.conceder(c, "TRANSFERENCIA_GESTAO_LOCAL")
        self.transferir(c, data)
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201,
                      self.vinculo_req(c, self.hoje - dt.timedelta(days=2), self.hoje + dt.timedelta(days=25)))
        a, b = self.posicao(v["id"], data - dt.timedelta(days=1)), self.posicao(v["id"], data)
        self.p.check("Transferência futura separa as duas faixas do vínculo posterior ao ato",
                     a["lotacaoId"] == self.f["lotacao_id"] and b["lotacaoId"] == self.f["lotacao_destino_id"])
        self.call("outro", "GET", f'/api/vinculos-contratuais/{v["id"]}', 404,
                  cenario="Destino ainda futuro não acessa identidade operacional atual")
        catalogo = self.call("outro", "GET", f'/api/lotacoes/{self.f["lotacao_destino_id"]}')
        catalogo["nome"] = "Nome posterior ao mapa " + uuid.uuid4().hex[:8]
        self.call("outro", "PUT", f'/api/lotacoes/{self.f["lotacao_destino_id"]}', body=catalogo)
        depois = self.posicao(v["id"], data)
        self.p.check("Renomeação do catálogo não reescreve a fotografia do mapa",
                     depois["lotacaoNome"] == b["lotacaoNome"] and depois["lotacaoNome"] != catalogo["nome"])
        self.guardar("futura", dict(contrato=c, vinculo=v["id"], data=str(data), nome_fotografado=b["lotacaoNome"]))

    def capacidade_concorrente(self):
        c = self.contrato(1)
        barreira = threading.Barrier(2)
        reqs = [self.vinculo_req(c, self.hoje) for _ in range(2)]
        def inserir(req):
            barreira.wait(timeout=15)
            return self.call("local", "POST", "/api/vinculos-contratuais", (201, 409), req,
                             cenario="Duas requisições TCP disputam a única vaga sem consentimento de excedente")
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            resultado = list(executor.map(inserir, reqs))
        vencedores = [x for x in resultado if isinstance(x, dict) and x.get("id") and x.get("pessoaId")]
        self.p.check("Disputa concorrente confirma exatamente um vínculo", len(vencedores) == 1, 1, len(vencedores))
        pagina = self.call("local", "GET", query("/api/vinculos-contratuais", contratoId=c["id"]))
        self.p.check("Nova requisição após commits conta somente um vínculo", pagina["totalElements"] == 1)
        self.guardar("concorrencia", dict(contrato=c, vinculo=vencedores[0]["id"], cpfs=[x["pessoa"]["cpf"] for x in reqs]))

    def configuracao_retroativa(self):
        """Publica a fonte de 8h, altera o mestre e só então inclui o fato antigo."""
        jornada_rota = f'/api/jornadas/{self.f["jornada_id"]}'
        lotacao_rota = f'/api/lotacoes/{self.f["lotacao_id"]}'
        jornada_original = self.call("local", "GET", jornada_rota)
        lotacao_original = self.call("local", "GET", lotacao_rota)
        def grade(jornada):
            campos = ("diaTrabalho", "entrada1", "saida1", "entrada2", "saida2")
            return {d.get("diaSemana"): [d.get(k) for k in campos] for d in jornada.get("dias", [])}
        uteis = ("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        semana_8h = {d: [True, "08:00", "12:00", "13:00", "17:00"] for d in uteis}
        semana_8h.update({d: [False, None, None, None, None] for d in ("SATURDAY", "SUNDAY")})
        self.p.check("Pré-condição: fonte a publicar tem sete dias e oito horas nos dias úteis",
                     len(jornada_original.get("dias", [])) == 7 and grade(jornada_original) == semana_8h,
                     semana_8h, grade(jornada_original))
        c = self.contrato()
        jornada_nova = copy.deepcopy(jornada_original)
        for dia in jornada_nova["dias"]:
            if dia["diaTrabalho"]:
                dia["saida2"] = "15:00"
        lotacao_nova = dict(lotacao_original, municipio="MUNICIPIO POSTERIOR TCP " + uuid.uuid4().hex[:8])
        try:
            self.call("local", "PUT", jornada_rota, body=jornada_nova)
            self.call("local", "PUT", lotacao_rota, body=lotacao_nova)
            jornada_confirmada = self.call("local", "GET", jornada_rota)
            semana_6h = {d: [True, "08:00", "12:00", "13:00", "15:00"] for d in uteis}
            semana_6h.update({d: [False, None, None, None, None] for d in ("SATURDAY", "SUNDAY")})
            self.p.check("Pré-condição: nova leitura confirma seis horas no mestre antes do fato retroativo",
                         len(jornada_confirmada.get("dias", [])) == 7 and grade(jornada_confirmada) == semana_6h,
                         semana_6h, grade(jornada_confirmada))
            lotacao_confirmada = self.call("local", "GET", lotacao_rota)
            municipio_esperado = lotacao_nova["municipio"].upper()
            self.p.check("Pré-condição: nova leitura confirma o município corrente alterado",
                         lotacao_confirmada.get("municipio") == municipio_esperado
                         and lotacao_confirmada.get("municipio") != lotacao_original.get("municipio"),
                         municipio_esperado, lotacao_confirmada.get("municipio"))
            v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
            data = str(self.dia_util_recente())
            f = self.call("local", "POST", "/api/frequencias", body=dict(
                vinculoContratualId=v["id"], dataReferencia=data, status="PRESENCA",
                justificativaRetificacao="Presença sem substituição da grade histórica comprovada",
                chaveIdempotencia="configuracao-tcp-" + uuid.uuid4().hex))
            rota = query("/api/relatorios/frequencia-consolidada", inicio=data, fim=data, contratoIds=c["id"])
            relatorio = self.call("local", "GET", rota)
            dia = relatorio["registros"][0]["diario"][data]
            self.p.check("Alocação retroativa nova preserva 8h publicadas após mestre mudar para 6h",
                         dia["minutosPrevistos"] == 480 and dia["minutosFisicos"] == 480,
                         dict(previstos=480, fisicos=480), dia)
            self.guardar("configuracao_retroativa", dict(contrato=c, vinculo=v["id"], frequencia=f["id"],
                         data=data, rota_relatorio=rota, municipio_original=lotacao_original.get("municipio")))
        finally:
            # Nova versão de transporte quando a API do catálogo a oferece.
            for rota, original in ((jornada_rota, jornada_original), (lotacao_rota, lotacao_original)):
                atual = self.call("local", "GET", rota)
                restaurar = dict(original)
                if "version" in atual:
                    restaurar["version"] = atual["version"]
                self.call("local", "PUT", rota, body=restaurar,
                          cenario="Restaura o catálogo sintético sem reescrever fotografias aplicadas")

    def identidade(self):
        c = self.contrato()
        self.conceder(c, "CORRECAO_IDENTIFICADOR_CONTRATO")
        novo = "CORR-TCP-" + uuid.uuid4().hex[:10].upper()
        self.call("local", "POST", f'/api/contratos/{c["id"]}/correcoes-identificador', 201, dict(
            novoNumero=novo, novoAno=c["ano"], dataEfetiva=c["inicio"], dataAto=str(self.hoje),
            motivo="Correção documental da identidade", referenciaSei="SEI-AUDTCP-NUMERO",
            autorizacaoAdministrativa="ATO-AUDTCP", chaveIdempotencia="audtcp-identidade-" + uuid.uuid4().hex))
        self.call("local", "POST", "/api/contratos", 409,
                  dict(numeroContrato=c["numero"], anoContrato=c["ano"], gestaoLocalId=self.f["gestao_id"]),
                  cenario="Número histórico corrigido permanece reservado ao mesmo contrato")
        encontrados = self.call("local", "GET", query("/api/contratos/historico-identificador", numero=c["numero"], ano=c["ano"]))
        self.p.check("Busca histórica continua apontando para a identidade original",
                     len(encontrados) == 1 and encontrados[0]["contratoId"] == c["id"] and
                     encontrados[0]["numeroAnterior"] == c["numero"] and encontrados[0]["numeroNovo"] == novo)
        self.guardar("identidade", dict(contrato=c, novo_numero=novo))

    def piso_concorrente(self):
        c = self.contrato(100)
        rota = f'/api/contratos/{c["id"]}/aditamentos'
        reqs = []
        for i in (1, 2):
            req = dict(identificacao="PISO-TCP-" + uuid.uuid4().hex[:10], dataDocumento=str(self.hoje),
                       dataEfetiva=str(self.hoje + dt.timedelta(days=i)), justificativa="Redução sintética documentada até o piso",
                       referenciaSei="SEI-AUDTCP-PISO", novoEfetivoTotal=75, confirmarImpacto=True,
                       alteracoesComposicao=[dict(composicaoId=c["composicao"], quantidadeResultante=75)])
            previa = self.call("local", "POST", rota + "/previa", body=req)
            req["tokenPrevia"] = previa["tokenPrevia"]
            reqs.append(req)
        barreira = threading.Barrier(2)
        def confirmar(req):
            barreira.wait(timeout=15)
            return self.call("local", "POST", rota, (201, 409), req,
                             cenario="Prévia concorrente não confirma redução sobre estado quantitativo superado")
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            respostas = list(executor.map(confirmar, reqs))
        self.p.check("Somente uma redução concorrente confirma sua prévia",
                     sum(bool(x.get("id")) for x in respostas if isinstance(x, dict)) == 1)
        data = self.hoje + dt.timedelta(days=3)
        posicao = self.call("local", "GET", query(f'/api/contratos/{c["id"]}/posicao-temporal', data=data))
        self.p.check("Piso global preserva exatamente 75 sobre os 100 originais",
                     posicao["versaoContratual"]["efetivoPrevisto"] == 75)
        abaixo = dict(reqs[0], identificacao="ABAIXO-TCP-" + uuid.uuid4().hex[:10], dataEfetiva=str(data),
                      novoEfetivoTotal=74, alteracoesComposicao=[dict(composicaoId=c["composicao"], quantidadeResultante=74)])
        abaixo.pop("tokenPrevia")
        previa = self.call("local", "POST", rota + "/previa", body=abaixo)
        abaixo["tokenPrevia"] = previa["tokenPrevia"]
        self.call("local", "POST", rota, 409, abaixo, cenario="Redução não excepcional abaixo de 75% é rejeitada")
        instrumentos = self.call("local", "GET", rota)
        self.p.check("Conflito e redução abaixo do piso não publicam instrumentos parciais", len(instrumentos) == 1)
        self.guardar("piso", dict(contrato=c, data=str(data)))

    def causas_desligamento(self):
        c = self.contrato()
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
        eventos = self.call("local", "GET", f'/api/vinculos-contratuais/{v["id"]}/eventos')
        for categoria in ("ANULACAO_CONTRATO", "ENCERRAMENTO_ANTECIPADO_CONTRATO", "FIM_VIGENCIA_CONTRATO"):
            self.call("local", "POST", f'/api/vinculos-contratuais/{v["id"]}/desligar', 409,
                      dict(dataFim=str(self.hoje), motivoDesligamentoId=self.f["motivos"][categoria],
                           justificativa="Hipótese inválida sem a causa contratual correspondente",
                           evidencia="SEI-AUDTCP", autorizacao="ATO-AUDTCP"),
                      cenario="Causa " + categoria + " exige fato contratual compatível")
        atual = self.call("local", "GET", f'/api/vinculos-contratuais/{v["id"]}')
        depois = self.call("local", "GET", f'/api/vinculos-contratuais/{v["id"]}/eventos')
        self.p.check("Três recusas preservam término e trilha do vínculo", atual.get("dataFim") == v.get("dataFim") and eventos == depois)
        individual = self.call("local", "POST", f'/api/vinculos-contratuais/{v["id"]}/desligar', body=dict(
            dataFim=str(self.hoje), motivoDesligamentoId=self.f["motivos"]["DESLIGAMENTO_INDIVIDUAL"],
            justificativa="Desligamento individual comprovado", evidencia="SEI-AUDTCP-INDIVIDUAL", autorizacao="ATO-AUDTCP"))
        self.p.check("Controle positivo confirma causa individual legítima", individual["dataFim"] == str(self.hoje))
        self.guardar("desligamento", dict(contrato=c, vinculo=v["id"], fim=str(self.hoje)))

    def transferencia_retroativa(self):
        c = self.contrato()
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
        self.conceder(c, "TRANSFERENCIA_GESTAO_LOCAL")
        inicio = self.hoje - dt.timedelta(days=14)
        # A fixture trabalha de segunda a sexta. A prova é de sete registros e
        # seus escopos, incluindo os dois descansos documentados pela Jornada.
        dias = [inicio + dt.timedelta(days=i) for i in range(7)]
        ids = [self.frequencia(v["id"], dia, "DESCANSO" if dia.weekday() >= 5 else "PRESENCA")["id"]
               for dia in dias]
        rota = query("/api/frequencias/intervalo", inicio=inicio, fim=inicio + dt.timedelta(days=6), contratoId=c["id"], size=20)
        for _ in range(2):
            self.p.check("Cache anterior à transferência guarda sete linhas autorizadas na origem",
                         self.call("local", "GET", rota)["totalElements"] == 7)
            self.p.check("Cache anterior guarda resultado vazio do destino", self.call("outro", "GET", rota)["totalElements"] == 0)
        self.transferir(c, inicio + dt.timedelta(days=2))
        for _ in range(2):
            a, b = self.call("local", "GET", rota), self.call("outro", "GET", rota)
            self.p.check("Autorização retroativa antecede cache, contagem e página", a["totalElements"] == 2 and b["totalElements"] == 5)
            self.p.check("Prova original e escopo efetivo permanecem distinguíveis",
                         b["content"][0]["gestaoLocalId"] == self.f["gestao_id"] and
                         b["content"][0]["escopoHistoricoEfetivo"]["gestaoLocalId"] == self.f["gestao_destino_id"])
        pagina = self.call("outro", "GET", query("/api/frequencias/intervalo", inicio=inicio,
                         fim=inicio + dt.timedelta(days=6), contratoId=c["id"], page=1, size=2))
        self.p.check("Página intermediária contém os IDs certos entre cinco autorizados",
                     pagina["totalElements"] == 5 and [x["id"] for x in pagina["content"]] == ids[4:6])
        for recurso in ("eventos", "evidencias-integridade"):
            self.call("local", "GET", f"/api/frequencias/{ids[3]}/{recurso}", 404)
            self.call("outro", "GET", f"/api/frequencias/{ids[3]}/{recurso}")
        self.guardar("retroativa", dict(contrato=c, vinculo=v["id"], inicio=str(inicio), ids=ids, rota=rota))

    def painel(self):
        c = self.estado["futura"]["contrato"]
        rota = query("/api/painel/efetivo", referencia=self.hoje, periodo="DIARIO", contratoId=c["id"])
        for papel in ("nacional", "presidente"):
            d1, _, h1 = self.clientes[papel].call("GET", rota)
            d2, _, h2 = self.clientes[papel].call("GET", rota)
            self.p.check("Redis real MISS/HIT separado para " + papel,
                         h1.get("X-SGT-Cache") == "MISS" and h2.get("X-SGT-Cache") == "HIT" and d1 == d2)
            self.p.check("Capacidade de detalhamento respeitada para " + papel,
                         bool(d1.get("quadroDimensionado")) == (papel == "nacional"))
        calculo = self.call("nacional", "POST", query("/api/painel/calculos", referencia=self.hoje, periodoDias=7), (200, 202))
        self.call("presidente", "GET", f'/api/painel/calculos/{calculo["id"]}', 403,
                  cenario="Alcance nacional não concede capacidades nominais do cálculo de outro perfil")
        self.guardar("painel", dict(calculo=calculo["id"]))

    def retificacao_anulacao(self):
        c = self.contrato()
        self.incorporar_causa_sintetica(c, "ANULACAO_CONTRATO")
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
        dia = self.dia_util_recente()
        dia_fechado = self.dia_util_recente(8)
        # FRE-002/003/013: fechamento começa no oitavo dia. Anulação não
        # antecipa essa janela (§5.22); o segundo registro já deve estar fechado.
        self.p.check("Datas distinguem janela aberta e prazo negocial já encerrado",
                     1 <= (self.hoje - dia).days <= 3 and
                     (self.hoje - dia_fechado).days >= 8 and
                     c["inicio"] <= str(dia_fechado) < str(dia) <= c["fim"])
        freq = self.call("operacional", "POST", "/api/frequencias", body=dict(vinculoContratualId=v["id"], dataReferencia=str(dia), status="FALTA"))
        self.p.check("Frequência operacional recente nasce aberta e válida antes de qualquer anulação",
                     freq["estadoNegocial"] == "ABERTA" and freq["validadeOperacional"] == "VALIDA"
                     and freq["bloqueadaEdicaoOperacional"] is False)
        filtro = dict(tipo="FREQUENCIA_CONSOLIDADA", formato="VISUALIZACAO", inicio=str(dia), fim=str(dia),
                      contratoIds=[c["id"]], chaveIdempotencia="audtcp-emissao-" + uuid.uuid4().hex)
        emissao = self.call("local", "POST", "/api/relatorios/administrativos/emissoes", 201, filtro)
        self.call("operacional", "POST", "/api/frequencias", body=dict(vinculoContratualId=v["id"], dataReferencia=str(dia),
                  status="PRESENCA", entrada1="08:00", saida1="12:00", entrada2="13:00", saida2="17:00"))
        eid = emissao["metadados"]["id"]
        antiga = self.call("local", "GET", f"/api/relatorios/administrativos/emissoes/{eid}")
        comparacao = self.call("local", "GET", f"/api/relatorios/administrativos/emissoes/{eid}/comparacao-vigente")
        self.p.check("Correção operacional supera emissão preservando conteúdo e hash",
                     antiga["metadados"]["estado"] == "SUPERADA" and antiga["conteudo"] == emissao["conteudo"] and
                     antiga["metadados"]["hashConteudo"] == emissao["metadados"]["hashConteudo"] and comparacao["alterado"])
        # Registro tardio legítimo pelo Gestor: exige justificativa/chave e não
        # altera relógio, banco ou o estado do primeiro registro artificialmente.
        fechada = self.frequencia(v["id"], dia_fechado, "FALTA")
        fontes = ((freq["id"], dia, "ABERTA", "PRESENCA"),
                  (fechada["id"], dia_fechado, "FECHADA", "FALTA"))
        eventos_antes = {}
        fotografias_antes = {}
        campos_preservados = ("id", "dataReferencia", "status", "entrada1", "saida1", "entrada2", "saida2",
                             "horasTrabalhadas", "horarioSaida", "horasNoturnas", "observacao",
                             "pessoaId", "vinculoContratualId", "alocacaoVinculoId", "contratoId", "empregado")
        for fid, data, estado, status in fontes:
            pagina = self.call("local", "GET", query("/api/frequencias", data=data, contratoId=c["id"]))
            linhas = [x for x in pagina["content"] if x["id"] == fid]
            self.p.check("Pré-condição relida: um registro " + estado + " válido na data correspondente",
                         len(linhas) == 1 and linhas[0]["estadoNegocial"] == estado
                         and linhas[0]["validadeOperacional"] == "VALIDA" and linhas[0]["status"] == status)
            if estado == "ABERTA":
                self.p.check("Pré-condição da alteração operacional conserva as quatro batidas informadas",
                             [linhas[0].get(k) for k in ("entrada1", "saida1", "entrada2", "saida2")]
                             == ["08:00", "12:00", "13:00", "17:00"])
            fotografias_antes[fid] = {k: linhas[0].get(k) for k in campos_preservados}
            eventos_antes[fid] = self.call("local", "GET", f"/api/frequencias/{fid}/eventos")
            self.p.check("Fonte anterior à anulação tem cadeia de eventos", bool(eventos_antes[fid]))
        # O alcance documental inclui ambas as datas; não existe comando de
        # fechamento antecipado, proibido pela norma para o Gestor Local.
        ato = dict(dataEfetiva=str(dia_fechado), dataAto=str(self.hoje), motivo="Anulação documental sintética TCP", referenciaSei="SEI-AUDTCP-ANULACAO")
        previa = self.call("local", "POST", f'/api/contratos/{c["id"]}/previas/anular', body=ato)
        self.call("local", "POST", f'/api/contratos/{c["id"]}/anular', body=dict(ato, tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True))
        for fid, data, estado, _ in fontes:
            pagina = self.call("local", "GET", query("/api/frequencias", data=data, contratoId=c["id"]))
            linhas = [x for x in pagina["content"] if x["id"] == fid]
            self.p.check("Anulação preserva registro " + estado + " e distingue perda de cobertura do fechamento",
                         len(linhas) == 1 and linhas[0]["validadeOperacional"] == "INVALIDA_COBERTURA"
                         and linhas[0]["estadoNegocial"] == estado and linhas[0]["estadoAuditoria"] == "SUJEITA_REVISAO"
                         and linhas[0]["bloqueadaEdicaoOperacional"] is True
                         and {k: linhas[0].get(k) for k in campos_preservados} == fotografias_antes[fid])
            eventos = self.call("local", "GET", f"/api/frequencias/{fid}/eventos")
            self.p.check("Impacto acrescenta evento sem reescrever cadeia do registro " + estado,
                         len(eventos) > len(eventos_antes[fid])
                         and eventos[:len(eventos_antes[fid])] == eventos_antes[fid])
            vigente = dict(filtro, inicio=str(data), fim=str(data))
            atual = self.call("local", "POST", "/api/relatorios/administrativos/previa", body=vigente)
            self.p.check("Recálculo exclui registro " + estado + " sem cobertura e mantém pendência explícita",
                         not atual["conteudo"]["registros"] and atual["metadados"]["sujeitoARevisao"]
                         and atual["metadados"]["quantidadePendencias"] >= 1)
        self.guardar("anulacao", dict(contrato=c, vinculo=v["id"], frequencia=freq["id"], data=str(dia),
                                     frequencia_fechada=fechada["id"], data_fechada=str(dia_fechado),
                                     emissao=eid, hash_emissao=emissao["metadados"]["hashConteudo"]))

    def documento_revogado(self):
        c = self.estado["desligamento"]["contrato"]
        eventos = self.call("local", "GET", f'/api/contratos/{c["id"]}/eventos')
        publicacao = next(x for x in eventos if x["tipoEvento"] == "PUBLICACAO")
        pdf = pathlib.Path(self.args.pdf).read_bytes()
        meta = dict(tipoFato="CONTRATO_ORIGINAL", fatoId=publicacao["id"], fatoChave="ORIGINAL",
                    titulo="Prova documental pessoal TCP", referenciaSei="SEI-AUDTCP-DOC", classificacaoAcesso="PESSOAL_RESTRITO",
                    dataEfetiva=c["inicio"], chaveIdempotencia="audtcp-doc-" + uuid.uuid4().hex)
        doc, _, _ = self.clientes["local"].multipart(f'/api/contratos/{c["id"]}/documentos', meta, pdf, esperado=201)
        rota = f'/api/contratos/{c["id"]}/documentos/{doc["id"]}'
        self.call("nacional", "GET", rota + "/arquivo?finalidade=auditoria", 403)
        perm = self.call("admin", "POST", rota + "/permissoes", body=dict(usuarioId=self.f["usuarios_ids"]["nacional"],
            finalidade="auditoria", justificativa="Conferência temporária dos bytes TCP", autorizacaoAdministrativa="ATO-AUDTCP",
            validoAte=(dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=2)).isoformat()))
        _, raw, _ = self.clientes["nacional"].call("GET", rota + "/arquivo?finalidade=auditoria")
        self.p.check("Concessão entrega bytes exatos do PDF pessoal", raw == pdf and sha(raw) == doc["sha256Arquivo"])
        self.call("admin", "POST", rota + f'/permissoes/{perm["id"]}/revogar', body=dict(
            motivo="Conferência encerrada", autorizacaoAdministrativa="ATO-AUDTCP-REVOGACAO"))
        self.call("nacional", "GET", rota + "/arquivo?finalidade=auditoria", 403,
                  cenario="Mesma sessão perde download após revogação confirmada")
        self.guardar("documento", dict(rota=rota, hash=sha(pdf), id=doc["id"], contrato=c["id"]))

    @staticmethod
    def faltas(painel):
        return (painel.get("periodo") or {}).get("faltasNaoJustificadas", -1)

    def painel_atual_ou_superado(self, calculo, esperado):
        return (not calculo.get("dados") or self.faltas(calculo["dados"]) == esperado
                or calculo.get("desatualizado") is True)

    def painel_persistido_retificado(self):
        c = self.contrato()
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
        dia = self.dia_util_recente()
        consulta = query("/api/painel/gestao", referencia=dia, periodoDias=7)
        antes = self.faltas(self.call("local", "GET", consulta))
        self.p.check("Painel de controle expõe contagem verificável", isinstance(antes, int) and antes >= 0)
        freq = self.frequencia(v["id"], dia, "FALTA")
        req = query("/api/painel/calculos", referencia=dia, periodoDias=7)
        pedido = self.call("local", "POST", req + "&forcar=true", (200, 202))
        rota = f'/api/painel/calculos/{pedido["id"]}'
        pronto = self.aguardar("local", rota, "Painel persistido inclui a falta real",
                               lambda d: d.get("concluido") and d.get("dados") is not None)
        self.p.check("Controle assíncrono contém exatamente a falta acrescentada",
                     self.faltas(pronto["dados"]) == antes + 1, antes + 1, self.faltas(pronto["dados"]))
        self.frequencia(v["id"], dia, "PRESENCA")
        vigente = self.call("local", "GET", consulta)
        self.p.check("Retificação real retira a falta do cálculo vigente", self.faltas(vigente) == antes,
                     antes, self.faltas(vigente))
        antigo = self.call("local", "GET", rota)
        self.p.check("Polling não anuncia payload antigo como atual após retificação",
                     self.painel_atual_ou_superado(antigo, antes), observado=antigo)
        repetido = self.call("local", "POST", req, (200, 202))
        self.p.check("Solicitação repetida não reutiliza falta antiga como atual",
                     self.painel_atual_ou_superado(repetido, antes), observado=repetido)
        atualizado = self.aguardar("local", f'/api/painel/calculos/{repetido["id"]}',
                                  "Recálculo persistido após retificação",
                                  lambda d: d.get("concluido") and d.get("dados") is not None
                                  and not d.get("desatualizado"))
        self.p.check("Recálculo persistido coincide com a verdade vigente",
                     self.faltas(atualizado["dados"]) == antes, antes, self.faltas(atualizado["dados"]))
        self.guardar("painel_retificacao", dict(contrato=c, vinculo=v["id"], frequencia=freq["id"],
                     data=str(dia), calculo=repetido["id"], faltas=antes))

    def conferir_minimizacao_pendencia(self, referencia):
        rota = query("/api/pendencias", contratoIds=referencia["contrato"]["id"])
        local = self.call("local", "GET", rota)
        item = next(x for x in local["registros"] if x["id"] == referencia["pendencia"])
        self.p.check("Responsável local conserva narrativa, autoria e estado de tratamento",
                     item["observacaoTratamento"] == referencia["observacao"]
                     and item["estado"] == "EM_TRATAMENTO" and bool(item.get("iniciadaEm"))
                     and item["iniciadaPor"] == referencia["autor"])
        for papel in ("estadual", "nacional", "presidente"):
            pessoa = self.call(papel, "GET", f'/api/pessoas/{referencia["pessoa"]}')
            self.p.check("Cadastro minimiza RG/e-mail para " + papel,
                         all(v not in json.dumps(pessoa, ensure_ascii=False)
                             for v in (referencia["rg"], referencia["email"])))
            pagina = self.call(papel, "GET", rota)
            minimizado = next(x for x in pagina["registros"] if x["id"] == referencia["pendencia"])
            self.p.check("Central não contorna minimização do cadastro para " + papel,
                         minimizado.get("observacaoTratamento") is None and minimizado.get("motivo") is None
                         and all(v not in json.dumps(pagina, ensure_ascii=False)
                                 for v in (referencia["rg"], referencia["email"])))
        releitura = self.call("local", "GET", rota)
        relida = next(x for x in releitura["registros"] if x["id"] == referencia["pendencia"])
        self.p.check("Leitura minimizada preserva integralmente a resposta autorizada do responsável", relida == item)

    def pendencia_narrativa(self):
        c = self.contrato()
        self.incorporar_causa_sintetica(c, "ANULACAO_CONTRATO")
        v = self.call("local", "POST", "/api/vinculos-contratuais", 201, self.vinculo_req(c))
        pessoa_id = v["pessoaId"]
        rg, email = "RG-TCP-" + uuid.uuid4().hex[:12], "pendencia-" + uuid.uuid4().hex[:12] + "@example.test"
        pessoa = self.call("local", "GET", f"/api/pessoas/{pessoa_id}")
        self.call("local", "PUT", f"/api/pessoas/{pessoa_id}/dados-opcionais", body=dict(pessoa, rg=rg, email=email))
        dia = self.dia_util_recente()
        freq = self.frequencia(v["id"], dia, "FALTA")
        ato = dict(dataEfetiva=str(dia - dt.timedelta(days=1)), dataAto=str(self.hoje),
                   motivo="Revisão documental para prova de minimização TCP", referenciaSei="SEI-TCP-PENDENCIA")
        rota_contrato = f'/api/contratos/{c["id"]}'
        previa = self.call("local", "POST", rota_contrato + "/previas/inativar", body=ato)
        self.call("local", "POST", rota_contrato + "/inativar", body=dict(
            ato, tokenPrevia=previa["tokenPrevia"], confirmarImpacto=True))
        pendencias = self.call("local", "GET", query("/api/pendencias", contratoIds=c["id"]))
        pendencia = next(x for x in pendencias["registros"] if x.get("frequenciaId") == freq["id"])
        observacao = "Conferência de identidade com RG " + rg + " e e-mail " + email
        tratamento = self.call("local", "POST", f'/api/pendencias/{pendencia["id"]}/iniciar-tratamento',
                               body=dict(observacao=observacao))
        referencia = dict(contrato=c, vinculo=v["id"], pessoa=pessoa_id, frequencia=freq["id"],
                          pendencia=pendencia["id"], rg=rg, email=email, observacao=observacao,
                          autor=tratamento["iniciadaPor"])
        self.conferir_minimizacao_pendencia(referencia)
        self.guardar("pendencia_narrativa", referencia)

    def verificar_reinicio(self):
        e = self.estado
        configuracao = e["configuracao_retroativa"]
        relatorio_configuracao = self.call("local", "GET", configuracao["rota_relatorio"])
        dia_configuracao = relatorio_configuracao["registros"][0]["diario"][configuracao["data"]]
        self.p.check("Após reinício, grade publicada conserva previsão e presença de 480 minutos",
                     dia_configuracao["minutosPrevistos"] == 480 and dia_configuracao["minutosFisicos"] == 480)
        f = e["futura"]
        p = self.posicao(f["vinculo"], f["data"])
        self.p.check("Após reinício, transferência futura mantém destino e fotografia",
                     p["lotacaoId"] == self.f["lotacao_destino_id"] and p["lotacaoNome"] == f["nome_fotografado"])
        for papel, total in (("local", 2), ("outro", 5)):
            pagina = self.call(papel, "GET", e["retroativa"]["rota"])
            self.p.check("Após reinício, página autorizada de " + papel + " preserva contagem", pagina["totalElements"] == total)
        c = e["identidade"]["contrato"]
        atual = self.call("local", "GET", f'/api/contratos/{c["id"]}')
        self.p.check("Correção de identidade persistida", atual["numeroContrato"] == e["identidade"]["novo_numero"])
        self.call("local", "POST", "/api/contratos", 409, dict(numeroContrato=c["numero"], anoContrato=c["ano"], gestaoLocalId=self.f["gestao_id"]))
        pagina = self.call("local", "GET", query("/api/vinculos-contratuais", contratoId=e["concorrencia"]["contrato"]["id"]))
        self.p.check("Após reinício, corrida permanece com um único vencedor", pagina["totalElements"] == 1)
        piso = self.call("local", "GET", query(f'/api/contratos/{e["piso"]["contrato"]["id"]}/posicao-temporal', data=e["piso"]["data"]))
        self.p.check("Após reinício, piso de 75% permanece confirmado", piso["versaoContratual"]["efetivoPrevisto"] == 75)
        a = e["anulacao"]
        emissao = self.call("local", "GET", f'/api/relatorios/administrativos/emissoes/{a["emissao"]}')
        self.p.check("Após reinício, emissão superada conserva hash", emissao["metadados"]["hashConteudo"] == a["hash_emissao"] and emissao["metadados"]["estado"] == "SUPERADA")
        for fid, data, estado in ((a["frequencia"], a["data"], "ABERTA"),
                                  (a["frequencia_fechada"], a["data_fechada"], "FECHADA")):
            evidencia = self.call("local", "GET", f"/api/frequencias/{fid}/evidencias-integridade")
            self.p.check("Após reinício, cadeia da frequência anulada " + estado + " continua verificável", evidencia["cadeiaIntegra"])
            pagina = self.call("local", "GET", query("/api/frequencias", data=data, contratoId=a["contrato"]["id"]))
            linhas = [x for x in pagina["content"] if x["id"] == fid]
            # A janela é a mesma regra civil, inclusive se o reinício ocorrer
            # em outro dia. A prova não exige que ABERTA permaneça para sempre.
            esperado = "FECHADA" if self.hoje >= dt.date.fromisoformat(data) + dt.timedelta(days=7) else estado
            self.p.check("Após reinício, cobertura e estado temporal continuam separados para " + estado,
                         len(linhas) == 1 and linhas[0]["estadoNegocial"] == esperado
                         and linhas[0]["validadeOperacional"] == "INVALIDA_COBERTURA"
                         and linhas[0]["estadoAuditoria"] == "SUJEITA_REVISAO"
                         and linhas[0]["bloqueadaEdicaoOperacional"] is True)
        self.call("nacional", "GET", e["documento"]["rota"] + "/arquivo?finalidade=auditoria", 403)
        _, raw, _ = self.clientes["local"].call("GET", e["documento"]["rota"] + "/arquivo?finalidade=gestao")
        self.p.check("Após reinício, bytes do documento preservados para responsável autorizado", sha(raw) == e["documento"]["hash"])
        self.call("presidente", "GET", f'/api/painel/calculos/{e["painel"]["calculo"]}', 403)
        exportacao = e["exportacao_autorizacao"]
        for anterior in exportacao["fases"]["criar"]:
            # Tarefas/arquivos temporários não constituem fatos persistidos.
            # O contrato e a frequência devem sustentar nova geração idêntica
            # quanto aos dados negociais; timestamps do ZIP não são comparados.
            self.call("local", "GET", f'/api/relatorios/exportacoes/{anterior["tarefa"]}', 404,
                      cenario="Reinício não ressuscita tarefa temporária já consumida")
            self.p.check("Hash do artefato TCP da primeira fase permanece verificável",
                         sha(pathlib.Path(anterior["arquivo"]).read_bytes()) == anterior["sha256"])
        self.exportacao_autorizacao(exportacao)
        painel = e["painel_retificacao"]
        consulta = self.call("local", "GET", f'/api/painel/calculos/{painel["calculo"]}')
        self.p.check("Após reinício, payload persistido retificado está atual ou explicitamente superado",
                     self.painel_atual_ou_superado(consulta, painel["faltas"]))
        recalculo = self.call("local", "POST", query("/api/painel/calculos",
                              referencia=painel["data"], periodoDias=7), (200, 202))
        pronto = self.aguardar("local", f'/api/painel/calculos/{recalculo["id"]}',
                               "Painel recalculado após reinício", lambda d: d.get("concluido")
                               and d.get("dados") is not None and not d.get("desatualizado"))
        self.p.check("Após reinício, painel conserva contagem da fonte retificada",
                     self.faltas(pronto["dados"]) == painel["faltas"], painel["faltas"], self.faltas(pronto["dados"]))
        self.conferir_minimizacao_pendencia(e["pendencia_narrativa"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("base", "fixture", "estado", "saida"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--pdf")
    parser.add_argument("--fase", choices=("criar", "reinicio"), required=True)
    args = parser.parse_args()
    prova = Prova(args.saida, args.fase, "validar_auditorias_tcp.py")
    try:
        fixture = json.loads(pathlib.Path(args.fixture).read_text())
        auditoria = Auditoria(args, fixture, prova)
        if args.fase == "criar":
            if not args.pdf:
                raise ValueError("A fase criar requer --pdf com documento real sintético.")
            for executar in (auditoria.futura_e_catalogo, auditoria.capacidade_concorrente, auditoria.piso_concorrente, auditoria.identidade,
                             auditoria.causas_desligamento, auditoria.transferencia_retroativa, auditoria.painel,
                             auditoria.retificacao_anulacao, auditoria.documento_revogado,
                             auditoria.exportacao_autorizacao, auditoria.painel_persistido_retificado, auditoria.pendencia_narrativa,
                             auditoria.configuracao_retroativa):
                executar()
        else:
            auditoria.estado = json.loads(pathlib.Path(args.estado).read_text())
            auditoria.verificar_reinicio()
        prova.concluir()
        print(json.dumps({"sucesso": True, "fase": args.fase, "requisicoes": len(prova.requests), "assercoes": len(prova.assertions)}))
    except Exception:
        prova.salvar(traceback.format_exc())
        raise


if __name__ == "__main__":
    main()
