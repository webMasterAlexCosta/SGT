package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
 * Proposta independente para reprodução centralizada; NÃO executada pelo revisor.
 * Oráculo: LEVANTAMENTO_REQUISITOS.md, princípios 3/5/8, §5.4, CAT-004,
 * TMP-004/TMP-005 e HIS-001/HIS-006: cada propriedade deve respeitar sua fonte e K.
 *
 * Fixture e PDF são expressamente sintéticos. DML da fixture simula perda de
 * colunas antigas; não recupera dados de produção e não introduz DDL manual.
 * MockMvc exercita controladores, autenticação, serviços e MySQL reais; a prova
 * de transporte TCP/JAR e de Redis real permanece gate separado da raiz.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaAditamentoRevisaoParcialHttpIT extends ContratosHttpBase {
    private static final String GRADE = "MONDAY|true|08:00|12:00|13:00|17:00;"
            + "TUESDAY|true|08:00|12:00|13:00|17:00;WEDNESDAY|true|08:00|12:00|13:00|17:00;"
            + "THURSDAY|true|08:00|12:00|13:00|17:00;FRIDAY|true|08:00|12:00|13:00|17:00;"
            + "SATURDAY|false||||;SUNDAY|false||||";
    private static final byte[] PDF = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
            + "% FONTE SINTETICA INDEPENDENTE: jornada de oito horas e municipio original.\n"
            + "% " + GRADE + "\n%%EOF").getBytes(StandardCharsets.UTF_8);

    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicaoTemporal;
    @Autowired ResolvedorContextoTemporalFrequenciaServico resolvedor;

    @BeforeEach
    void exigirPersistenciaPermitida() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            var meta = conexao.getMetaData();
            assertThat(meta.getDatabaseProductName()).isEqualToIgnoringCase("MySQL");
            assertThat(meta.getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void alocacaoEntreProvasNaoConverteMunicipioDesconhecidoEmAusenciaDefinitiva() throws Exception {
        Origem o = prepararOrigem();
        long documento = publicarFonte(o);
        conceder(o.c(), o.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        String itemOriginal = itemOriginal(o);
        String versaoOriginal = versaoOriginal(o);
        byte[] arquivoOriginal = baixar(o, documento);
        Instant k0 = Instant.now();

        var primeiraReq = requisicao(o, documento, o.versao(), null,
                mapa("aditamentoComposicaoId", o.item(), "jornadaSemanal", GRADE,
                        "jornadaComprovada", true, "municipioComprovado", false));
        JsonNode primeira = incorporar(o, primeiraReq);
        Instant k1 = Instant.parse(primeira.path("registradoEm").asText());
        assertThat(configuracaoProjetada(o, k0).jornadaSemanal()).isNull();
        assertThat(configuracaoProjetada(o, k1).jornadaSemanal()).isEqualTo(GRADE);
        assertThat(configuracaoProjetada(o, k1).lotacaoMunicipio()).isNull();

        // Esta posição entre revisões é o caso ausente nos três testes focais.
        long vinculo = criarVinculo(o);
        String alocacaoOriginal = fotografiaAlocacao(vinculo);
        Instant kEntre = Instant.now();
        var antes = configuracaoAlocacao(vinculo, o.dia(), kEntre);
        assertThat(antes.jornadaSemanal()).isEqualTo(GRADE);
        assertThat(antes.lotacaoMunicipio()).isNull();

        var segundaReq = requisicao(o, documento, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), mapa("aditamentoComposicaoId", o.item(),
                        "lotacaoMunicipio", o.municipio(), "municipioComprovado", true,
                        "jornadaComprovada", false));
        JsonNode segunda = incorporar(o, segundaReq);
        Instant k2 = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(k2).isAfter(kEntre);
        assertThat(segunda.path("numeroRevisao").asInt()).isEqualTo(2);
        assertThat(segunda.path("provaAnteriorId").asLong()).isEqualTo(primeira.path("id").asLong());
        assertThat(configuracaoProjetada(o, kEntre).lotacaoMunicipio()).isNull();
        assertThat(configuracaoProjetada(o, k2).lotacaoMunicipio()).isEqualTo(o.municipio());
        assertThat(configuracaoAlocacao(vinculo, o.dia(), kEntre).lotacaoMunicipio()).isNull();

        var depois = configuracaoAlocacao(vinculo, o.dia(), k2);
        JsonNode relatorio = consultar(o.c().localA,
                "/api/relatorios/frequencia-consolidada?inicio=" + o.dia() + "&fim=" + o.dia()
                        + "&contratoIds=" + o.n().id(), "A fonte tardia deve alcançar o feriado municipal histórico");
        assertThat(relatorio.path("registros")).hasSize(1);
        JsonNode dia = relatorio.path("registros").get(0).path("diario").path(o.dia().toString());

        // Primeiro verifica imutabilidade, antes dos oráculos esperados como RED.
        assertThat(itemOriginal(o)).isEqualTo(itemOriginal);
        assertThat(versaoOriginal(o)).isEqualTo(versaoOriginal);
        assertThat(fotografiaAlocacao(vinculo)).isEqualTo(alocacaoOriginal);
        assertThat(baixar(o, documento)).isEqualTo(arquivoOriginal).isEqualTo(PDF);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PDF));
        emTransacao(() -> {
            var p = em.find(ProvaHistoricaContrato.class, segunda.path("id").asLong());
            assertThat(p.getHashFonteArquivo()).isEqualTo(sha);
            assertThat(p.getAutorId()).isEqualTo(o.c().localA.getId());
            assertThat(p.getProvaAnterior().getId()).isEqualTo(primeira.path("id").asLong());
            return null;
        });
        assertAll("Município comprovado depois de alocação criada com prova apenas da grade",
                () -> assertThat(depois.lotacaoMunicipio()).isEqualTo(o.municipio()),
                () -> assertThat(depois.jornadaSemanal()).isEqualTo(GRADE),
                () -> assertThat(dia.path("status").asText()).isEqualTo("FERIADO"),
                () -> assertThat(dia.path("minutosPrevistos").asInt()).isZero());
    }

    private Origem prepararOrigem() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long cargo = emTransacao(() -> {
            Cargo novo = new Cargo("Cargo sintético independente ADI " + n.id(), "1");
            novo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            em.persist(novo); em.flush();
            var origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(novo.getId());
            origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da disponibilidade do Cargo deste teste");
            em.persist(origem); em.flush();
            return novo.getId();
        });
        LocalDate data = n.inicio().plusDays(2);
        LocalDate dia = data.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(dia).isBefore(RelogioSistema.hoje());
        String identificacao = "ADI-INDEPENDENTE-" + n.id();
        var req = aditamento(identificacao, data);
        req.put("novoEfetivoTotal", 7);
        req.put("alteracoesComposicao", List.of(mapa("lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "cargoId", cargo, "quantidadeResultante", 2)));
        String rota = base(n.id()) + "/aditamentos";
        req.put("tokenPrevia", enviar(c.localA, rota + "/previa", req, 200, "Prévia do ADI sintético").path("tokenPrevia").asText());
        long adi = enviar(c.localA, rota, req, 201, "ADI cria uma combinação nova documentada").path("id").asLong();
        Origem o = emTransacao(() -> {
            var i = em.createQuery("select i from AditamentoComposicao i where i.aditamento.id=:a", AditamentoComposicao.class)
                    .setParameter("a", adi).getSingleResult();
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e", VersaoContratoNegocial.class)
                    .setParameter("e", i.getAditamento().getEvento().getId()).getSingleResult();
            assertThat(i.getComposicaoBase()).isNull();
            assertThat(i.getJornadaSemanalFotografia()).isEqualTo(GRADE);
            String municipio = i.getLotacaoMunicipioFotografia();
            assertThat(municipio).isNotBlank();
            Map<String, Object> foto = fotografiaNegocial(v);
            // Perda simulada apenas na fixture, preservando nomes e quantidades.
            assertThat(em.createQuery("update AditamentoComposicao i set i.jornadaSemanalFotografia=null, "
                    + "i.lotacaoMunicipioFotografia=null where i.id=:i").setParameter("i", i.getId()).executeUpdate()).isEqualTo(1);
            return new Origem(c, n, adi, i.getId(), i.getAditamento().getEvento().getId(), v.getId(),
                    cargo, data, dia, identificacao, municipio, foto);
        });
        enviar(c.localA, "/api/calendario-eventos", mapa("data", dia, "tipo", "FERIADO",
                "descricao", "Feriado municipal sintético independente", "abrangencia", "MUNICIPAL",
                "municipio", o.municipio(), "ativo", true), 201, "Fonte de calendário municipal da fixture");
        ObjectNode lotacao = (ObjectNode) consultar(c.localA, "/api/lotacoes/" + c.lotacaoA.getId(), "Ler mestre antes da alteração").deepCopy();
        lotacao.put("municipio", "MUNICIPIO ATUAL SEM VALOR HISTORICO " + n.id());
        executar(c.localA, put("/api/lotacoes/" + c.lotacaoA.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(lotacao)), 200, "Mestre atual diverge da fonte histórica; não pode ser recapturado");
        return o;
    }

    private long publicarFonte(Origem o) throws Exception {
        JsonNode d = dados(executar(o.c().localA, multipart(base(o.n().id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoId", o.aditamento(),
                        "fatoChave", o.identificacao(), "titulo", "Fonte sintética independente de revisão parcial",
                        "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", o.data(),
                        "chaveIdempotencia", UUID.randomUUID().toString())))
                .file(new MockMultipartFile("arquivo", "fonte-independente-adi.pdf", "application/pdf", PDF)),
                201, "Anexo sintético preservado e vinculado ao ADI exato"));
        assertThat(d.path("estado").asText()).isEqualTo("PUBLICADO");
        return d.path("id").asLong();
    }

    private Map<String, Object> requisicao(Origem o, long documento, long versao, Long anterior, Map<String, Object> item) {
        return mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", versao, "provaAnteriorId", anterior,
                "dataDocumento", o.data(), "documentoComprobatorioId", documento,
                "finalidadeAcessoDocumento", "Conferência sintética independente da fonte ADI",
                "motivo", "Incorporar somente propriedades declaradas pela fonte sintética",
                "autorizacaoAdministrativa", "ATO-HTTP", "versaoContratoEsperada",
                emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()),
                "composicoes", List.of(item));
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(o.c().localA, rota + "/previa", req, 200, "Prévia da prova parcial").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localA, rota, req, 201, "Incorporação autorizada com predecessor explícito");
    }

    private long criarVinculo(Origem o) throws Exception {
        return enviar(o.c().localA, "/api/vinculos-contratuais", mapa("contratoId", o.n().id(),
                "lotacaoId", o.c().lotacaoA.getId(), "jornadaId", o.c().jornadaA.getId(), "cargoId", o.cargo(),
                "dataInicio", o.dia(), "pessoa", mapa("nome", "Pessoa sintética entre provas parciais",
                        "cpf", cpf(Math.toIntExact(710000000L + o.n().id())))), 201,
                "Alocação criada entre provas de propriedades diferentes").path("id").asLong();
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoProjetada(Origem o, Instant k) {
        return emTransacao(() -> {
            var p = composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), o.dia(), k).stream()
                    .filter(x -> x.cargo().getId().equals(o.cargo())).findFirst().orElseThrow();
            return new ConsultaProvaHistoricaContratoServico.Configuracao(p.lotacaoNome(), p.jornadaDescricao(),
                    p.cargoNome(), p.lotacaoMunicipio(), p.jornadaSemanal());
        });
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoAlocacao(long vinculo, LocalDate dia, Instant k) {
        return emTransacao(() -> resolvedor.resolverConfiguracao(alocacao(vinculo), dia, k));
    }

    private AlocacaoVinculo alocacao(long vinculo) {
        return em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                .setParameter("v", vinculo).getSingleResult();
    }

    private String fotografiaAlocacao(long vinculo) {
        return emTransacao(() -> {
            var a = alocacao(vinculo);
            return mapper.valueToTree(mapa("id", a.getId(), "vinculo", a.getVinculo().getId(),
                    "lotacao", a.getLotacaoNomeFotografia(), "jornada", a.getJornadaDescricaoFotografia(),
                    "cargo", a.getCargoNomeFotografia(), "municipio", a.getLotacaoMunicipioFotografia(),
                    "grade", a.getJornadaSemanalFotografia(), "inicio", a.getDataInicio(), "fim", a.getDataFim())).toString();
        });
    }

    private String itemOriginal(Origem o) {
        return emTransacao(() -> {
            var i = em.find(AditamentoComposicao.class, o.item());
            return mapper.valueToTree(mapa("id", i.getId(), "adi", i.getAditamento().getId(),
                    "anterior", i.getQuantidadeAnterior(), "variacao", i.getVariacao(), "resultante", i.getQuantidadeResultante(),
                    "lotacao", i.getLotacaoNomeFotografia(), "jornada", i.getJornadaDescricaoFotografia(),
                    "cargo", i.getCargoNomeFotografia(), "municipio", i.getLotacaoMunicipioFotografia(),
                    "grade", i.getJornadaSemanalFotografia())).toString();
        });
    }

    private String versaoOriginal(Origem o) {
        return emTransacao(() -> mapper.valueToTree(motor.paraVersaoDTO(em.find(VersaoContratoNegocial.class, o.versao()))).toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografiaNegocial(VersaoContratoNegocial v) {
        Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
        return foto;
    }

    private byte[] baixar(Origem o, long documento) throws Exception {
        return executar(o.c().localA, get(base(o.n().id()) + "/documentos/" + documento + "/arquivo")
                .param("finalidade", "Conferir imutabilidade da fonte sintética"), 200,
                "Download conserva os bytes exatos da fonte").getResponse().getContentAsByteArray();
    }

    private record Origem(Cenario c, NovoContrato n, long aditamento, long item, long evento, long versao,
                          long cargo, LocalDate data, LocalDate dia, String identificacao,
                          String municipio, Map<String, Object> fotografia) {}
}
