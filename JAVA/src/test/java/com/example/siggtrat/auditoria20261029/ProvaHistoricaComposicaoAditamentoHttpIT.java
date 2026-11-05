package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CAT-004/HIS-001/HIS-006: fonte posterior da combinação criada em ADI.
 * O documento e a perda são sintéticos e exclusivos da fixture MySQL.
 * As ações são HTTP reais da aplicação; releituras usam novas transações.
 * Não depende da nova classe de fragmento para compilar e demonstrar o RED.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaComposicaoAditamentoHttpIT extends ContratosHttpBase {
    private static final String GRADE_8H = "MONDAY|true|08:00|12:00|13:00|17:00;"
            + "TUESDAY|true|08:00|12:00|13:00|17:00;"
            + "WEDNESDAY|true|08:00|12:00|13:00|17:00;"
            + "THURSDAY|true|08:00|12:00|13:00|17:00;"
            + "FRIDAY|true|08:00|12:00|13:00|17:00;"
            + "SATURDAY|false||||;SUNDAY|false||||";
    private static final byte[] PDF = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
            + "% FONTE SINTETICA EXPLICITA DO TESTE: grade semanal original de oito horas\n"
            + "% " + GRADE_8H + "\n%%EOF").getBytes(StandardCharsets.UTF_8);

    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicaoTemporal;
    @Autowired DataSource dataSource;

    @BeforeEach
    void exigirMySqlReal() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualToIgnoringCase("MySQL");
        }
    }

    @Test
    void fontePosteriorDoItemAdiPreservaOriginalConhecimentoAutoriaEIdempotencia() throws Exception {
        Origem o = origemComLacuna(true);
        long documento = publicarFonte(o);
        long permissao = concederFonte(o);
        Map<String, Object> req = fonte(o, documento);
        Estado antes = estado(o);
        Instant conhecimentoAnterior = Instant.now();
        FotoProjetada lacuna = projetar(o, o.data(), conhecimentoAnterior);
        assertThat(lacuna).isEqualTo(new FotoProjetada(null, null, null, null, null, 2));
        assertThat(consultar(o.c().localA, rota(o) + "?conhecidoAte=" + conhecimentoAnterior,
                "Sem prova no corte anterior à incorporação")).isEmpty();

        JsonNode recibo = incorporar(o, req);
        long provaId = recibo.path("id").asLong();
        Instant conhecimentoPosterior = Instant.parse(recibo.path("registradoEm").asText());
        assertThat(conhecimentoPosterior).isAfter(conhecimentoAnterior);
        assertThat(recibo.path("configuracaoCompletaComprovada").asBoolean()).isFalse();
        assertThat(recibo.path("numeroRevisao").asInt()).isEqualTo(1);
        assertThat(projetar(o, o.data(), conhecimentoAnterior)).isEqualTo(lacuna);
        conferirConfiguracao(o, projetar(o, o.data(), conhecimentoPosterior), 2);
        assertThat(consultar(o.c().localA, rota(o) + "?conhecidoAte=" + conhecimentoAnterior,
                "A fonte tardia não retroage o conhecimento")).isEmpty();
        assertThat(consultar(o.c().localA, rota(o) + "?conhecidoAte=" + conhecimentoPosterior,
                "Consulta documental inclui a prova no instante de incorporação")).hasSize(1);
        JsonNode composicoes = consultar(o.c().localA, base(o.n().id()) + "/composicoes?data=" + o.data(),
                "Consulta HTTP resolve o rótulo da exata combinação documentada");
        JsonNode nova = null;
        for (JsonNode comp : composicoes) if (comp.path("cargoId").asLong() == o.cargo()) nova = comp;
        assertThat(nova).isNotNull();
        assertThat(nova.path("cargoNome").asText()).isEqualTo(o.configuracao().cargo());
        assertThat(nova.path("efetivoPrevisto").asInt()).isEqualTo(2);

        String shaFonte = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PDF));
        emTransacao(() -> {
            ProvaHistoricaContrato p = em.find(ProvaHistoricaContrato.class, provaId);
            DocumentoComprobatorio d = em.find(DocumentoComprobatorio.class, documento);
            assertThat(p.getEventoOrigem().getId()).isEqualTo(o.evento());
            assertThat(p.getVersaoAnterior().getId()).isEqualTo(o.versao());
            assertThat(p.getVersaoGerada().getId()).isNotEqualTo(o.versao());
            assertThat(p.getAutorId()).isEqualTo(o.c().localA.getId());
            assertThat(p.getPermissao().getId()).isEqualTo(permissao);
            assertThat(p.getDocumentoComprobatorio().getId()).isEqualTo(documento);
            assertThat(p.getHashFonteArquivo()).isEqualTo(shaFonte);
            assertThat(p.getVersaoFonteDocumento()).isEqualTo(d.getVersion());
            assertThat(p.getDataDocumento()).isEqualTo(o.data());
            assertThat(p.getCamposComprovados()).isEmpty();
            assertThat(p.getHashConteudo()).matches("[0-9a-f]{64}");
            assertThat(fotografiaNegocial(p.getVersaoGerada())).isEqualTo(o.fotografia());
            return null;
        });
        Estado depois = estado(o);
        conferirUmaIncorporacao(antes, depois);
        assertThat(depois.original()).isEqualTo(antes.original());
        assertThat(fotoItem(o.item())).isEqualTo(o.original());

        JsonNode repetida = enviar(o.c().localA, rota(o), req, 201,
                "Reenvio da mesma intenção retorna a prova sem novo uso ou versão");
        assertThat(repetida.path("id").asLong()).isEqualTo(provaId);
        assertThat(estado(o)).isEqualTo(depois);
        var outraIntencao = new LinkedHashMap<>(req);
        outraIntencao.put("motivo", "Outra intenção não pode reutilizar a chave confirmada");
        enviar(o.c().localA, rota(o), outraIntencao, 409,
                "A chave documental não pode identificar intenções diferentes");
        assertThat(estado(o)).isEqualTo(depois);
    }

    @Test
    void provaDaOrigemAdiPropagaPorAdiQuantitativoERemSemReescreverInstrumentosOuAlocacao() throws Exception {
        Origem o = origemComLacuna(false);
        LocalDate dataQuantitativo = o.data().plusDays(2);
        var quantitativo = aditamento("ADI-DELTA-POSTERIOR-" + o.n().id(), dataQuantitativo);
        quantitativo.put("novoEfetivoTotal", 8);
        quantitativo.put("alteracoesComposicao", List.of(mapa("lotacaoId", o.c().lotacaoA.getId(),
                "jornadaId", o.c().jornadaA.getId(), "cargoId", o.cargo(), "quantidadeResultante", 3)));
        long adiPosterior = registrar(o.c(), o.n().id(), "/aditamentos", quantitativo);
        LocalDate dataRem = dataQuantitativo.plusDays(2);
        var rem = mapa("identificacao", "REM-HERDA-ORIGEM-" + o.n().id(), "dataEfetiva", dataRem,
                "justificativa", "Delta de um posto entre combinações já existentes", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", o.n().composicao1(), "variacao", -1),
                        mapa("lotacaoId", o.c().lotacaoA.getId(), "jornadaId", o.c().jornadaA.getId(),
                                "cargoId", o.cargo(), "variacao", 1)));
        long remPosterior = registrar(o.c(), o.n().id(), "/remanejamentos", rem);
        LocalDate dia = dataRem.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(dia).isBefore(RelogioSistema.hoje());
        long vinculoAntigo = cadastrar(o, dia, 1);
        assertThat(fotoAlocacao(vinculoAntigo)).isEqualTo(new Configuracao(o.configuracao().lotacao(),
                o.configuracao().jornada(), o.configuracao().cargo(), null, null));
        executar(o.c().localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", dia.toString()).param("fim", dia.toString())
                .param("contratoIds", Long.toString(o.n().id())), 409,
                "A ausência original impede apuração; a grade atual de seis horas não é fonte");
        String atosAntes = fotografiasDosAtos(o, adiPosterior, remPosterior);
        Estado estadoAntes = estado(o);
        long documento = publicarFonte(o);
        concederFonte(o);
        Instant conhecimentoAnterior = Instant.now();
        Map<String, Object> req = fonte(o, documento);
        JsonNode recibo = incorporar(o, req);
        Instant conhecimentoPosterior = Instant.parse(recibo.path("registradoEm").asText());

        for (LocalDate data : List.of(o.data(), dataQuantitativo, dataRem)) {
            int quantidade = data.equals(o.data()) ? 2 : data.equals(dataQuantitativo) ? 3 : 4;
            assertThat(projetar(o, data, conhecimentoAnterior).grade()).isNull();
            conferirConfiguracao(o, projetar(o, data, conhecimentoPosterior), quantidade);
        }
        assertThat(fotografiasDosAtos(o, adiPosterior, remPosterior)).isEqualTo(atosAntes);
        assertThat(estado(o).original()).isEqualTo(estadoAntes.original());
        assertThat(fotoItem(o.item())).isEqualTo(o.original());
        JsonNode relatorio = dados(executar(o.c().localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", dia.toString()).param("fim", dia.toString())
                .param("contratoIds", Long.toString(o.n().id())), 200,
                "Relatório resolve a origem ADI comprovada sem reescrever alocação anterior"));
        assertThat(relatorio.path("registros")).hasSize(1);
        assertThat(relatorio.path("registros").get(0).path("diario").path(dia.toString())
                .path("minutosPrevistos").asInt()).isEqualTo(480);
        JsonNode frequencia = enviar(o.c().localA, "/api/frequencias", mapa("vinculoContratualId", vinculoAntigo,
                "dataReferencia", dia, "status", "PRESENCA", "justificativaRetificacao", "Fonte ADI incorporada posteriormente",
                "chaveIdempotencia", UUID.randomUUID().toString()), 200,
                "Apuração usa a grade de oito horas comprovada para a origem ADI exata");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, frequencia.path("id").asLong());
            assertThat(f.getMinutosFisicos()).isEqualTo(480);
            assertThat(f.getLotacaoSnapshotMunicipio()).isEqualTo(o.configuracao().municipio());
            assertThat(f.getJornadaSnapshotSaida2()).isEqualTo("17:00");
            return null;
        });
        assertThat(fotoAlocacao(vinculoAntigo).grade()).isNull();
        assertThat(fotoAlocacao(vinculoAntigo).municipio()).isNull();
        long vinculoNovo = cadastrar(o, dia, 2);
        assertThat(fotoAlocacao(vinculoNovo)).isEqualTo(o.configuracao());
        assertThat(fotografiasDosAtos(o, adiPosterior, remPosterior)).isEqualTo(atosAntes);
        assertThat(fotoItem(o.item())).isEqualTo(o.original());
    }

    @Test
    void incorporacaoAdiRecusaFonteConcessaoAlvoOuCamposInadequadosEExigeRevisaoExplicita() throws Exception {
        Origem o = origemComLacuna(false);
        long documento = publicarFonte(o);
        Map<String, Object> req = fonte(o, documento);
        Estado semConcessao = estado(o);
        enviar(o.c().localA, rota(o) + "/previa", req, 403, "Fonte não dispensa concessão específica");
        enviar(o.c().localA, rota(o), req, 403, "Confirmação sem concessão não incorpora nada");
        assertThat(estado(o)).isEqualTo(semConcessao);
        concederFonte(o);

        var quantitativo = aditamento("ADI-ALVO-DIFERENTE-" + o.n().id(), o.data().plusDays(1));
        quantitativo.put("novoEfetivoTotal", 8);
        quantitativo.put("alteracoesComposicao", List.of(mapa("lotacaoId", o.c().lotacaoA.getId(),
                "jornadaId", o.c().jornadaA.getId(), "cargoId", o.cargo(), "quantidadeResultante", 3)));
        long outroAdi = registrar(o.c(), o.n().id(), "/aditamentos", quantitativo);
        long outroItem = emTransacao(() -> em.createQuery("select i.id from AditamentoComposicao i where i.aditamento.id=:a", Long.class)
                .setParameter("a", outroAdi).getSingleResult());
        req = fonte(o, documento);
        Estado antesRecusas = estado(o);
        var semFonte = new LinkedHashMap<>(req);
        semFonte.remove("documentoComprobatorioId");
        semFonte.remove("referenciaSei");
        enviar(o.c().localA, rota(o) + "/previa", semFonte, 400, "A lacuna permanece quando não há fonte");
        enviar(o.c().localA, rota(o), semFonte, 400, "Recusa de fonte também é atômica na confirmação");
        var alvoErrado = new LinkedHashMap<>(req);
        var itemErrado = itemFonte(o);
        itemErrado.put("aditamentoComposicaoId", outroItem);
        alvoErrado.put("composicoes", List.of(itemErrado));
        enviar(o.c().localA, rota(o) + "/previa", alvoErrado, 404,
                "Item do ADI posterior não pode ser declarado como item da origem anterior");
        var ambiguo = new LinkedHashMap<>(req);
        var doisAlvos = itemFonte(o);
        doisAlvos.put("composicaoId", o.n().composicao1());
        ambiguo.put("composicoes", List.of(doisAlvos));
        enviar(o.c().localA, rota(o) + "/previa", ambiguo, 400, "Um fragmento não pode ter duas origens canônicas");
        var escalar = new LinkedHashMap<>(req);
        escalar.put("camposComprovados", List.of("descricao"));
        enviar(o.c().localA, rota(o) + "/previa", escalar, 400,
                "A extensão de configuração ADI não certifica alterações contratuais escalares");
        assertThat(estado(o)).isEqualTo(antesRecusas);

        // Primeiro conhecimento prova apenas a grade; o município ainda não é declarado.
        var gradeSomente = itemFonte(o);
        gradeSomente.remove("lotacaoMunicipio");
        gradeSomente.put("municipioComprovado", false);
        req.put("composicoes", List.of(gradeSomente));
        Instant k0 = Instant.now();
        JsonNode primeira = incorporar(o, req);
        Instant k1 = Instant.parse(primeira.path("registradoEm").asText());
        assertThat(projetar(o, o.data(), k0).grade()).isNull();
        assertThat(projetar(o, o.data(), k1).grade()).isEqualTo(GRADE_8H);
        assertThat(projetar(o, o.data(), k1).municipio()).isNull();

        var revisao = fonte(o, documento);
        revisao.put("versaoAnteriorId", primeira.path("versaoGeradaId").asLong());
        revisao.put("composicoes", List.of(mapa("aditamentoComposicaoId", o.item(),
                "lotacaoMunicipio", o.configuracao().municipio(), "municipioComprovado", true,
                "jornadaComprovada", false)));
        Estado antesRevisao = estado(o);
        enviar(o.c().localA, rota(o) + "/previa", revisao, 409,
                "Fragmento posterior exige o predecessor explícito da cadeia documental");
        assertThat(estado(o)).isEqualTo(antesRevisao);
        revisao.put("provaAnteriorId", primeira.path("id").asLong());
        JsonNode segunda = incorporar(o, revisao);
        Instant k2 = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(segunda.path("numeroRevisao").asInt()).isEqualTo(2);
        assertThat(segunda.path("provaAnteriorId").asLong()).isEqualTo(primeira.path("id").asLong());
        assertThat(projetar(o, o.data(), k0).grade()).isNull();
        assertThat(projetar(o, o.data(), k1).municipio()).isNull();
        conferirConfiguracao(o, projetar(o, o.data(), k2), 2);
        assertThat(consultar(o.c().localA, rota(o) + "?conhecidoAte=" + k1,
                "Corte intermediário conserva somente a primeira prova")).hasSize(1);
        assertThat(consultar(o.c().localA, rota(o) + "?conhecidoAte=" + k2,
                "Corte posterior conserva as duas revisões")).hasSize(2);
        conferirUmaIncorporacao(antesRevisao, estado(o));
        assertThat(fotoItem(o.item())).isEqualTo(o.original());
    }

    private Origem origemComLacuna(boolean apagarRotulos) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long cargo = emTransacao(() -> {
            Cargo novo = new Cargo("Cargo original da fonte ADI " + n.id(), "1");
            novo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            em.persist(novo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(novo.getId());
            origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da disponibilidade do Cargo do ADI");
            em.persist(origem); em.flush();
            return novo.getId();
        });
        LocalDate data = n.inicio().plusDays(2);
        String identificacao = "ADI-ORIGEM-FONTE-" + n.id();
        var req = aditamento(identificacao, data);
        req.put("novoEfetivoTotal", 7);
        req.put("alteracoesComposicao", List.of(mapa("lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "cargoId", cargo, "quantidadeResultante", 2)));
        long aditamento = registrar(c, n.id(), "/aditamentos", req);
        Origem o = emTransacao(() -> {
            AditamentoComposicao item = em.createQuery("select i from AditamentoComposicao i where i.aditamento.id=:a", AditamentoComposicao.class)
                    .setParameter("a", aditamento).getSingleResult();
            assertThat(item.getComposicaoBase()).isNull();
            assertThat(item.getQuantidadeAnterior()).isZero();
            assertThat(item.getJornadaSemanalFotografia()).isEqualTo(GRADE_8H);
            long evento = item.getAditamento().getEvento().getId();
            VersaoContratoNegocial versao = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e", VersaoContratoNegocial.class)
                    .setParameter("e", evento).getSingleResult();
            Configuracao fonte = configuracao(item);
            // DML somente da fixture: simula colunas ausentes do acervo, nunca é correção produtiva.
            String perda = "update AditamentoComposicao i set i.jornadaSemanalFotografia=null, i.lotacaoMunicipioFotografia=null";
            if (apagarRotulos) perda += ", i.lotacaoNomeFotografia='', i.jornadaDescricaoFotografia='', i.cargoNomeFotografia=''";
            assertThat(em.createQuery(perda + " where i.id=:i").setParameter("i", item.getId()).executeUpdate()).isEqualTo(1);
            return new Origem(c, n, aditamento, item.getId(), evento, versao.getId(), cargo, data,
                    identificacao, fotografiaNegocial(versao), fonte, null);
        });
        Origem comOriginal = new Origem(c, n, aditamento, o.item(), o.evento(), o.versao(), cargo, data,
                identificacao, o.fotografia(), o.configuracao(), fotoItem(o.item()));
        alterarMestres(comOriginal);
        return comOriginal;
    }

    private long registrar(Cenario c, long contrato, String sufixo, Map<String, Object> req) throws Exception {
        String rota = base(contrato) + sufixo;
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200, "Prévia canônica do instrumento sintético");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, rota, req, 201, "Instrumento confirmado preserva seu contexto documental").path("id").asLong();
    }

    private void alterarMestres(Origem o) throws Exception {
        String rotaJornada = "/api/jornadas/" + o.c().jornadaA.getId();
        ObjectNode jornada = (ObjectNode) consultar(o.c().localA, rotaJornada, "DTO integral para mudança atual da grade").deepCopy();
        jornada.put("descricao", "Jornada atual sem valor de prova para o ADI " + o.n().id());
        for (JsonNode dia : jornada.path("dias")) if (dia.path("diaTrabalho").asBoolean()) ((ObjectNode) dia).put("saida2", "15:00");
        executar(o.c().localA, put(rotaJornada).contentType(MediaType.APPLICATION_JSON).content(json(jornada)), 200,
                "Mestre passa a seis horas após o fato histórico");
        String rotaLotacao = "/api/lotacoes/" + o.c().lotacaoA.getId();
        ObjectNode lotacao = (ObjectNode) consultar(o.c().localA, rotaLotacao, "DTO integral para mudança administrativa atual").deepCopy();
        lotacao.put("nome", "Lotação atual diferente da fonte " + o.n().id());
        lotacao.put("municipio", "MUNICIPIO ATUAL DIFERENTE " + o.n().id());
        executar(o.c().localA, put(rotaLotacao).contentType(MediaType.APPLICATION_JSON).content(json(lotacao)), 200,
                "Mestre atual não documenta município ou rótulo da origem ADI");
        emTransacao(() -> {
            em.find(Cargo.class, o.cargo()).setNome("Cargo atual diferente da fonte " + o.n().id());
            return null;
        });
    }

    private long publicarFonte(Origem o) throws Exception {
        JsonNode documento = dados(executar(o.c().localA, multipart(base(o.n().id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoId", o.aditamento(), "fatoChave", o.identificacao(),
                        "titulo", "Fonte sintética posterior da configuração do ADI", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", o.data(), "chaveIdempotencia", UUID.randomUUID().toString())))
                .file(new MockMultipartFile("arquivo", "fonte-sintetica-adi.pdf", "application/pdf", PDF)), 201,
                "Documento sintético é publicado pelo fluxo canônico e vinculado ao ADI exato"));
        assertThat(documento.path("estado").asText()).isEqualTo("PUBLICADO");
        assertThat(documento.path("fatoId").asLong()).isEqualTo(o.aditamento());
        return documento.path("id").asLong();
    }

    private long concederFonte(Origem o) throws Exception {
        return enviar(o.c().admin, "/api/permissoes-excepcionais", mapa("tipo", "INCORPORACAO_PROVA_HISTORICA",
                "usuarioBeneficiarioId", o.c().localA.getId(), "contratoId", o.n().id(),
                "finalidade", "Conferência da fonte sintética da composição ADI", "justificativa", "Fonte sintética explícita recebida",
                "autorizacaoAdministrativa", "ATO-HTTP", "validoAte", Instant.now().plusSeconds(7200)), 201,
                "Concessão específica do contrato e responsável atual").path("id").asLong();
    }

    private Map<String, Object> fonte(Origem o, long documento) {
        long versaoContrato = emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion());
        return mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", o.versao(), "dataDocumento", o.data(),
                "documentoComprobatorioId", documento, "finalidadeAcessoDocumento", "Conferir fonte do item ADI identificado",
                "motivo", "Documento sintético posterior preenche lacuna somente do item da origem ADI",
                "autorizacaoAdministrativa", "ATO-HTTP", "versaoContratoEsperada", versaoContrato,
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()),
                "composicoes", List.of(itemFonte(o)));
    }

    private Map<String, Object> itemFonte(Origem o) {
        return mapa("aditamentoComposicaoId", o.item(), "lotacaoNome", o.configuracao().lotacao(),
                "jornadaDescricao", o.configuracao().jornada(), "cargoNome", o.configuracao().cargo(),
                "lotacaoMunicipio", o.configuracao().municipio(), "municipioComprovado", true,
                "jornadaSemanal", GRADE_8H, "jornadaComprovada", true);
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(o.c().localA, rota(o) + "/previa", req, 200,
                "Fonte autêntica no cenário, autorizada e vinculada ao item ADI, recebe prévia");
        assertThat(previa.path("tokenPrevia").asText()).isNotBlank();
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localA, rota(o), req, 201,
                "Prova posterior cria conhecimento novo sem transformar lacuna em mestre atual");
    }

    private String rota(Origem o) { return base(o.n().id()) + "/provas-historicas"; }

    private FotoProjetada projetar(Origem o, LocalDate data, Instant conhecido) {
        return emTransacao(() -> {
            var p = composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), data, conhecido).stream()
                    .filter(i -> i.cargo() != null && i.cargo().getId().equals(o.cargo())).findFirst().orElseThrow();
            return new FotoProjetada(p.lotacaoNome(), p.jornadaDescricao(), p.cargoNome(),
                    p.lotacaoMunicipio(), p.jornadaSemanal(), p.efetivoPrevisto());
        });
    }

    private void conferirConfiguracao(Origem o, FotoProjetada foto, int quantidade) {
        assertThat(foto).isEqualTo(new FotoProjetada(o.configuracao().lotacao(), o.configuracao().jornada(),
                o.configuracao().cargo(), o.configuracao().municipio(), GRADE_8H, quantidade));
    }

    private long cadastrar(Origem o, LocalDate data, int ordem) throws Exception {
        return enviar(o.c().localA, "/api/vinculos-contratuais", mapa("contratoId", o.n().id(),
                "lotacaoId", o.c().lotacaoA.getId(), "jornadaId", o.c().jornadaA.getId(), "cargoId", o.cargo(),
                "dataInicio", data, "pessoa", mapa("nome", "Pessoa sintética da configuração ADI " + ordem,
                        "cpf", cpf(Math.toIntExact(720000000L + o.n().id() * 10 + ordem)))), 201,
                "Alocação retroativa preserva o conhecimento documental da combinação na data").path("id").asLong();
    }

    private Configuracao fotoAlocacao(long vinculo) {
        return emTransacao(() -> {
            var a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).getSingleResult();
            return new Configuracao(a.getLotacaoNomeFotografia(), a.getJornadaDescricaoFotografia(),
                    a.getCargoNomeFotografia(), a.getLotacaoMunicipioFotografia(), a.getJornadaSemanalFotografia());
        });
    }

    private Configuracao configuracao(AditamentoComposicao i) {
        return new Configuracao(i.getLotacaoNomeFotografia(), i.getJornadaDescricaoFotografia(), i.getCargoNomeFotografia(),
                i.getLotacaoMunicipioFotografia(), i.getJornadaSemanalFotografia());
    }

    private FotoItem fotoItem(long id) {
        return emTransacao(() -> {
            var i = em.find(AditamentoComposicao.class, id);
            return new FotoItem(i.getId(), i.getAditamento().getId(), i.getComposicaoBase() == null ? null : i.getComposicaoBase().getId(),
                    configuracao(i), i.getQuantidadeAnterior(), i.getVariacao(), i.getQuantidadeResultante());
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografiaNegocial(VersaoContratoNegocial v) {
        Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
        return foto;
    }

    private String fotografiasDosAtos(Origem o, long adiPosterior, long remPosterior) {
        return emTransacao(() -> {
            var adi = em.createQuery("select i from AditamentoComposicao i where i.aditamento.id in :ids order by i.id", AditamentoComposicao.class)
                    .setParameter("ids", List.of(o.aditamento(), adiPosterior)).getResultList().stream()
                    .map(i -> List.of(i.getId(), i.getQuantidadeAnterior(), i.getVariacao(), i.getQuantidadeResultante(), configuracao(i))).toList();
            var rem = em.createQuery("select i from RemanejamentoComposicao i where i.remanejamento.id=:r order by i.id", RemanejamentoComposicao.class)
                    .setParameter("r", remPosterior).getResultList().stream()
                    .map(i -> List.of(i.getId(), i.getQuantidadeAnterior(), i.getVariacao(), i.getQuantidadeResultante(),
                            new Configuracao(i.getLotacaoNomeFotografia(), i.getJornadaDescricaoFotografia(), i.getCargoNomeFotografia(),
                                    i.getLotacaoMunicipioFotografia(), i.getJornadaSemanalFotografia()))).toList();
            return mapper.valueToTree(mapa("adi", adi, "rem", rem)).toString();
        });
    }

    private Estado estado(Origem o) {
        return emTransacao(() -> {
            long provas = em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                    .setParameter("c", o.n().id()).getSingleResult();
            long versoes = em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                    .setParameter("c", o.n().id()).getSingleResult();
            long eventos = em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                    .setParameter("c", o.n().id()).getSingleResult();
            long usos = em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.permissao.contrato.id=:c and u.tipo=:t", Long.class)
                    .setParameter("c", o.n().id()).setParameter("t", TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA).getSingleResult();
            String original = mapper.valueToTree(motor.paraVersaoDTO(em.find(VersaoContratoNegocial.class, o.versao()))).toString();
            Contrato contrato = em.find(Contrato.class, o.n().id());
            return new Estado(provas, versoes, eventos, usos, contrato.getVersion(), contrato.getEfetivoPrevisto(), original);
        });
    }

    private void conferirUmaIncorporacao(Estado antes, Estado depois) {
        assertThat(depois.provas()).isEqualTo(antes.provas() + 1);
        assertThat(depois.versoes()).isEqualTo(antes.versoes() + 1);
        assertThat(depois.usos()).isEqualTo(antes.usos() + 1);
        assertThat(depois.eventos()).isEqualTo(antes.eventos());
        assertThat(depois.efetivo()).isEqualTo(antes.efetivo());
        assertThat(depois.original()).isEqualTo(antes.original());
    }

    private record Configuracao(String lotacao, String jornada, String cargo, String municipio, String grade) {}
    private record FotoProjetada(String lotacao, String jornada, String cargo, String municipio, String grade, int quantidade) {}
    private record FotoItem(Long id, Long aditamento, Long base, Configuracao configuracao, Integer anterior, Integer variacao, Integer resultante) {}
    private record Estado(long provas, long versoes, long eventos, long usos, long versaoContrato, Integer efetivo, String original) {}
    private record Origem(Cenario c, NovoContrato n, long aditamento, long item, long evento, long versao, long cargo,
                          LocalDate data, String identificacao, Map<String, Object> fotografia, Configuracao configuracao, FotoItem original) {}
}
