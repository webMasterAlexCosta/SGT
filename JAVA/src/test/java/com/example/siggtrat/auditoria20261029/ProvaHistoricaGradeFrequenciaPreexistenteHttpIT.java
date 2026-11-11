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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Hipótese independente transversal: FRE-009/REL-001, revisão de grade com IDs constantes.
 * Não compilado nem executado pelo revisor; somente proposta fora da árvore de trabalho.
 * A Alocação da fixture conserva lacuna real e a Frequência nasce após a prova V1.
 * A prova V2 corrige explicitamente a mesma origem; não é alteração do mestre corrente.
 * PostgreSQL e serviços reais. O DML de perda é exclusivo da fixture sintética.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaGradeFrequenciaPreexistenteHttpIT extends ContratosHttpBase {
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
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualToIgnoringCase("PostgreSQL");
        }
    }

    @Test
    void revisaoDaGradeQueOriginouFrequenciaFechadaIdentificaImpactoSemReescreverProvaAnterior() throws Exception {
        Origem o = origemComLacuna(false);
        LocalDate dia = o.data().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(dia).isBefore(RelogioSistema.hoje().minusDays(7));
        long vinculo = cadastrar(o, dia, 7);
        assertThat(fotoAlocacao(vinculo).grade()).isNull();
        assertThat(fotoAlocacao(vinculo).municipio()).isNull();
        concederFonte(o);
        long documentoV1 = publicarFonte(o);
        JsonNode provaV1 = incorporar(o, fonte(o, documentoV1));
        Instant k1 = Instant.parse(provaV1.path("registradoEm").asText());
        assertThat(projetar(o, dia, k1).grade()).isEqualTo(GRADE_8H);
        JsonNode lancada = enviar(o.c().localA, "/api/frequencias", mapa("vinculoContratualId", vinculo,
                "dataReferencia", dia, "status", "PRESENCA", "justificativaRetificacao", "Conferência com a prova V1 de oito horas",
                "chaveIdempotencia", UUID.randomUUID().toString()), 200,
                "Controle: a frequência fechada nasce da prova V1, sem grade própria completa da Alocação");
        long frequenciaId = lancada.path("id").asLong();
        FotoFrequencia antes = fotoFrequencia(frequenciaId);
        assertThat(antes.estado()).isEqualTo("FECHADA");
        assertThat(antes.validade()).isEqualTo("VALIDA");
        assertThat(antes.minutos()).isEqualTo(480);
        assertThat(antes.saida2()).isEqualTo("17:00");
        assertThat(fotoAlocacao(vinculo).grade()).isNull();
        JsonNode eventosAntes = consultar(o.c().localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Cadeia original e fotografia V1 antes da correção documental");

        String grade6h = GRADE_8H.replace("17:00", "15:00");
        byte[] pdfV2 = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
                + "% FONTE SINTETICA V2: corrige explicitamente a grade original para seis horas\n% "
                + grade6h + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
        long documentoV2 = dados(executar(o.c().localA, multipart(base(o.n().id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoId", o.aditamento(), "fatoChave", o.identificacao(),
                        "titulo", "Fonte sintética retificadora da grade do mesmo ADI", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", o.data(), "chaveIdempotencia", UUID.randomUUID().toString())))
                .file(new MockMultipartFile("arquivo", "fonte-sintetica-v2.pdf", "application/pdf", pdfV2)), 201,
                "Nova fonte própria da revisão V2, sem reaproveitar o conteúdo documental de oito horas")).path("id").asLong();
        Map<String,Object> reqV2 = fonte(o, documentoV2);
        reqV2.put("provaAnteriorId", provaV1.path("id").asLong());
        reqV2.put("versaoAnteriorId", provaV1.path("versaoGeradaId").asLong());
        reqV2.put("motivo", "Fonte V2 corrige a grade original no mesmo período e item ADI");
        Map<String,Object> itemV2 = itemFonte(o);
        itemV2.put("jornadaSemanal", grade6h);
        reqV2.put("composicoes", List.of(itemV2));
        JsonNode provaV2 = incorporar(o, reqV2);
        Instant k2 = Instant.parse(provaV2.path("registradoEm").asText());
        assertThat(projetar(o, dia, k1).grade()).isEqualTo(GRADE_8H);
        assertThat(projetar(o, dia, k2).grade()).isEqualTo(grade6h);
        assertThat(provaV2.path("provaAnteriorId").asLong()).isEqualTo(provaV1.path("id").asLong());
        assertThat(fotoAlocacao(vinculo).grade()).isNull();

        FotoFrequencia depois = fotoFrequencia(frequenciaId);
        long pendencias = emTransacao(() -> em.createQuery("select count(p) from PendenciaAdministrativa p "
                        + "where p.frequenciaId=:f and p.estado<>:resolvida", Long.class)
                .setParameter("f", frequenciaId)
                .setParameter("resolvida", com.example.siggtrat.dominio.pendencia.EstadoPendencia.RESOLVIDA).getSingleResult());
        JsonNode eventosDepois = consultar(o.c().localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Original permanece íntegro; impacto acrescenta uma versão de revisão");
        JsonNode relatorio = dados(executar(o.c().localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", dia.toString()).param("fim", dia.toString()).param("contratoIds", Long.toString(o.n().id())),
                200, "Relatório não pode tratar a frequência de oito horas como definitiva após correção comprovada para seis"));
        assertAll("FRE-009: revisão da fonte que sustentou o cálculo identifica o registro fechado atingido",
                () -> assertThat(depois.validade()).isEqualTo("SUJEITA_REVISAO"),
                () -> assertThat(depois.auditoria()).isEqualTo("SUJEITA_REVISAO"),
                () -> assertThat(pendencias).isEqualTo(1L),
                () -> assertThat(relatorio.path("revisao").path("sujeitoARevisao").asBoolean()).isTrue(),
                () -> assertThat(depois.minutos()).isEqualTo(antes.minutos()),
                () -> assertThat(depois.saida2()).isEqualTo(antes.saida2()),
                () -> assertThat(depois.autorOriginal()).isEqualTo(antes.autorOriginal()),
                () -> assertThat(depois.alocacao()).isEqualTo(antes.alocacao()),
                () -> assertThat(depois.jornada()).isEqualTo(antes.jornada()),
                () -> assertThat(eventosDepois.get(0)).isEqualTo(eventosAntes.get(0)));
    }

    private FotoFrequencia fotoFrequencia(long id) {
        return emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class,id);
            return new FotoFrequencia(f.getEstadoNegocial().name(), f.getValidadeOperacional().name(),
                    f.getEstadoAuditoria().name(), f.getMinutosFisicos(), f.getJornadaSnapshotSaida2(),
                    f.getAutorOriginalId(), f.getAlocacaoVinculo().getId(), f.getJornadaSnapshotId());
        });
    }
    private record FotoFrequencia(String estado, String validade, String auditoria, Integer minutos,
                                  String saida2, Long autorOriginal, Long alocacao, Long jornada) { }

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
