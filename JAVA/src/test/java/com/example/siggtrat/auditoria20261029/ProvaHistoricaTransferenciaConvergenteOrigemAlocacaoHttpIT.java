package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Controle permanente das origens individuais em mapas convergentes.
 * Controle da correção por proveniência transitória: agregado e alocação
 * derivada têm fontes diferentes quando dois mapas convergem.
 *
 * Dados/SEI sintéticos; DML simula perda de duas propriedades de mapas somente
 * na fixture. Nenhuma coluna de origem/evento/alocação é fabricada ou alterada.
 * PostgreSQL real 8.0.45; Hibernate update; nenhum esquema manual.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaTransferenciaConvergenteOrigemAlocacaoHttpIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicaoTemporal;
    @Autowired ConsultaProvaHistoricaContratoServico consultaProvas;
    @Autowired ResolvedorContextoTemporalFrequenciaServico resolvedor;

    @BeforeEach
    void exigirPersistenciaPermitida() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            var meta = conexao.getMetaData();
            assertThat(meta.getDatabaseProductName()).isEqualToIgnoringCase("PostgreSQL");
            assertThat(meta.getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void alocacaoDerivadaUsaSeuMapaMesmoQuandoAgregadoAindaNaoComprovaMunicipio() throws Exception {
        Origem o = preparar();
        String mapasOriginais = fotografiasMapas(o);
        String versaoOriginal = fotografiaVersao(o.versao());
        String publicacaoOriginal = fotografiaVersao(o.publicacao());
        Instant k0 = Instant.now();

        JsonNode primeira = incorporar(o, requisicao(o, o.versao(), null, List.of(
                mapa("mapeamentoId", o.mapa1(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false),
                mapa("mapeamentoId", o.mapa2(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false))));
        Instant k1 = Instant.parse(primeira.path("registradoEm").asText());
        assertThat(k1).isAfter(k0);
        assertThat(agregado(o, k0).jornadaSemanal()).isNull();
        assertThat(agregado(o, k1).jornadaSemanal()).isEqualTo(o.grade());
        assertThat(agregado(o, k1).municipioComprovado()).isFalse();

        // A inclusão é registrada entre fontes, mas tem início EFETIVO antes
        // da transferência e segue M2. O próprio serviço produz evento e faixa.
        long derivado = enviar(o.c().localB, "/api/vinculos-contratuais", mapa(
                "contratoId", o.n().id(), "composicaoId", o.n().composicao2(),
                "lotacaoId", o.c().lotacaoA.getId(), "jornadaId", o.c().jornadaA.getId(),
                "cargoId", o.cargo2(), "dataInicio", o.n().inicio().plusDays(1),
                "pessoa", mapa("nome", "Pessoa sintética derivada do mapa dois",
                        "cpf", cpf(Math.toIntExact(770000000L + 2 * o.n().id())))), 201,
                "A origem anterior da pessoa identifica exatamente M2").path("id").asLong();
        long alocacaoDerivada = alocacaoDestinoId(o, derivado);

        // Outra inclusão nasce diretamente no agregado; não tem origem M1/M2
        // demonstrada apenas por compartilhar os mesmos catálogos de destino.
        long direto = enviar(o.c().localB, "/api/vinculos-contratuais", mapa(
                "contratoId", o.n().id(), "lotacaoId", o.c().lotacaoB.getId(),
                "jornadaId", o.c().jornadaB.getId(), "cargoId", o.c().cargoB.getId(),
                "dataInicio", o.dia(), "pessoa", mapa("nome", "Pessoa sintética criada no agregado",
                        "cpf", cpf(Math.toIntExact(770000001L + 2 * o.n().id())))), 201,
                "Grade concordante permite inclusão no agregado sem inventar município").path("id").asLong();
        long alocacaoDireta = alocacaoDestinoId(o, direto);
        String fotoDerivada = fotografiaAlocacao(alocacaoDerivada);
        String fotoDireta = fotografiaAlocacao(alocacaoDireta);
        String eventoOrigem = fotografiaEventoOrigem(derivado, alocacaoDerivada);

        emTransacao(() -> {
            var a = em.find(AlocacaoVinculo.class, alocacaoDerivada);
            assertThat(a.getComposicao()).as("M2 foi informado por tríade; nenhuma origem física foi inventada").isNull();
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(o.grade());
            assertThat(a.getLotacaoMunicipioFotografia()).isNull();
            assertThat(a.getMunicipioAusenciaComprovada()).isFalse();
            var e = eventoOrigem(derivado, alocacaoDerivada);
            assertThat(e.getTransferenciaGestaoId()).isEqualTo(o.transferencia());
            assertThat(e.getAlocacaoAnteriorId()).isNotNull().isNotEqualTo(alocacaoDerivada);
            assertThat(e.getComposicaoAnteriorId()).isEqualTo(o.n().composicao2());
            assertThat(e.getLotacaoAnteriorId()).isEqualTo(o.c().lotacaoA.getId());
            assertThat(e.getJornadaAnteriorId()).isEqualTo(o.c().jornadaA.getId());
            assertThat(e.getCargoAnteriorId()).isEqualTo(o.cargo2());
            assertThat(e.getLotacaoNovaId()).isEqualTo(o.c().lotacaoB.getId());
            assertThat(e.getJornadaNovaId()).isEqualTo(o.c().jornadaB.getId());
            assertThat(e.getCargoNovoId()).isEqualTo(o.c().cargoB.getId());
            return null;
        });
        Instant kEntre = Instant.now();
        assertThat(configuracaoAlocacao(alocacaoDerivada, o.dia(), kEntre).municipioComprovado()).isFalse();
        assertThat(configuracaoAlocacao(alocacaoDireta, o.dia(), kEntre).municipioComprovado()).isFalse();

        // Só M2 recebe prova municipal. M1 continua desconhecido, logo o
        // agregado continua desconhecido, mas a pessoa derivada possui M2 exato.
        JsonNode segunda = incorporar(o, requisicao(o, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), List.of(mapa("mapeamentoId", o.mapa2(),
                        "lotacaoMunicipio", o.municipio(), "municipioComprovado", true,
                        "jornadaComprovada", false))));
        Instant k2 = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(k2).isAfter(kEntre);
        assertThat(segunda.path("provaAnteriorId").asLong()).isEqualTo(primeira.path("id").asLong());

        // Fotografias e evidência de proveniência não são reescritas.
        assertThat(fotografiasMapas(o)).isEqualTo(mapasOriginais);
        assertThat(fotografiaVersao(o.versao())).isEqualTo(versaoOriginal);
        assertThat(fotografiaVersao(o.publicacao())).isEqualTo(publicacaoOriginal);
        assertThat(fotografiaAlocacao(alocacaoDerivada)).isEqualTo(fotoDerivada);
        assertThat(fotografiaAlocacao(alocacaoDireta)).isEqualTo(fotoDireta);
        assertThat(fotografiaEventoOrigem(derivado, alocacaoDerivada)).isEqualTo(eventoOrigem);
        assertThat(configuracaoAlocacao(alocacaoDerivada, o.dia(), kEntre).lotacaoMunicipio()).isNull();
        assertThat(configuracaoAlocacao(alocacaoDerivada, o.dia(), kEntre).municipioComprovado()).isFalse();

        var agregadoDepois = agregado(o, k2);
        var derivadaDepois = configuracaoAlocacao(alocacaoDerivada, o.dia(), k2);
        var diretaDepois = configuracaoAlocacao(alocacaoDireta, o.dia(), k2);
        assertAll("Prova restrita a M2 não se perde no agregado nem se espalha a outra origem",
                () -> assertThat(configuracaoMapa(o.mapa1(), k2).municipioComprovado()).isFalse(),
                () -> assertThat(configuracaoMapa(o.mapa2(), k2).municipioComprovado()).isTrue(),
                () -> assertThat(agregadoDepois.municipioComprovado()).isFalse(),
                () -> assertThat(agregadoDepois.lotacaoMunicipio()).isNull(),
                () -> assertThat(agregadoDepois.jornadaSemanal()).isEqualTo(o.grade()),
                () -> assertThat(derivadaDepois.municipioComprovado()).isTrue(),
                () -> assertThat(derivadaDepois.lotacaoMunicipio()).isEqualTo(o.municipio()),
                () -> assertThat(derivadaDepois.jornadaSemanal()).isEqualTo(o.grade()),
                () -> assertThat(diretaDepois.municipioComprovado()).isFalse(),
                () -> assertThat(diretaDepois.lotacaoMunicipio()).isNull(),
                () -> assertThat(diretaDepois.jornadaSemanal()).isEqualTo(o.grade()));
    }

    private Origem preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 6, 3, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long cargo2 = emTransacao(() -> em.find(ComposicaoContratual.class, n.composicao2()).getCargo().getId());
        emTransacao(() -> {
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio("MUNICIPIO DOCUMENTADO M2 " + n.id());
            return null;
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data = RelogioSistema.hoje().minusDays(20);
        LocalDate dia = RelogioSistema.hoje().minusDays(7);
        var req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Convergência sintética de dois recortes independentes", "referenciaSei", "SEI-MAPAS-CONVERGENTES",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(
                        mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()),
                        mapa("lotacaoOrigemId", c.lotacaoA.getId(), "jornadaOrigemId", c.jornadaA.getId(),
                                "cargoOrigemId", cargo2, "lotacaoDestinoId", c.lotacaoB.getId(),
                                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        var previa = enviar(c.localA, base(n.id()) + "/transferencias-gestao/previa", req, 200,
                "Prévia de mapas convergentes com origens distintas");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "Convergência explícita autorizada");
        Origem o = emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c",
                    TransferenciaGestaoContrato.class).setParameter("c", n.id()).getSingleResult();
            assertThat(t.getMapeamentos()).hasSize(2);
            var m1 = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(c.cargoA.getId())).findFirst().orElseThrow();
            var m2 = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(cargo2)).findFirst().orElseThrow();
            assertThat(m2.getComposicaoOrigem()).isNull();
            assertThat(m1.getJornadaDestinoSemanalFotografia()).isNotBlank().isEqualTo(m2.getJornadaDestinoSemanalFotografia());
            String grade = m2.getJornadaDestinoSemanalFotografia();
            String municipio = m2.getLotacaoDestinoMunicipioFotografia();
            assertThat(municipio).isNotBlank();
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e",
                    VersaoContratoNegocial.class).setParameter("e", t.getEvento().getId()).getSingleResult();
            var p = motor.fotografiaOriginalConhecida(t.getContrato(), Instant.now()).orElseThrow();
            var foto = fotografiaNegocial(v);
            assertThat(em.createQuery("update MapeamentoTransferenciaGestao m set "
                    + "m.jornadaDestinoSemanalFotografia=null, m.lotacaoDestinoMunicipioFotografia=null "
                    + "where m.transferencia.id=:t").setParameter("t", t.getId()).executeUpdate()).isEqualTo(2);
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio("MUNICIPIO ATUAL NAO COMPROVA M2 " + n.id());
            return new Origem(c, n, cargo2, t.getId(), m1.getId(), m2.getId(), t.getEvento().getId(),
                    v.getId(), p.getId(), data, dia, grade, municipio, foto);
        });
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "INCORPORACAO_PROVA_HISTORICA",
                "usuarioBeneficiarioId", c.localB.getId(), "contratoId", n.id(),
                "finalidade", "Conferência sintética das fontes por mapa",
                "justificativa", "Distinguir prova individual e recorte agregado",
                "autorizacaoAdministrativa", "ATO-CONVERGENCIA",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "Concessão documental ao responsável atual");
        return o;
    }

    private Map<String, Object> requisicao(Origem o, long versao, Long anterior, List<Map<String, Object>> itens) {
        return mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", versao, "provaAnteriorId", anterior,
                "dataDocumento", o.data(), "referenciaSei", "SEI-SINTETICO-FONTE-POR-MAPA",
                "motivo", "Provar somente as propriedades e mapas declarados",
                "autorizacaoAdministrativa", "ATO-CONVERGENCIA", "versaoContratoEsperada",
                emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()), "composicoes", itens);
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(o.c().localB, rota + "/previa", req, 200, "Prévia de fonte restrita ao mapa").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localB, rota, req, 201, "Fonte parcial preserva origem e conhecimento");
    }

    private long alocacaoDestinoId(Origem o, long vinculo) {
        return emTransacao(() -> em.createQuery("select a.id from AlocacaoVinculo a where a.vinculo.id=:v and a.lotacao.id=:l",
                Long.class).setParameter("v", vinculo).setParameter("l", o.c().lotacaoB.getId()).getSingleResult());
    }

    private EventoAlocacaoVinculo eventoOrigem(long vinculo, long alocacao) {
        return em.createQuery("select e from EventoAlocacaoVinculo e where e.vinculo.id=:v and e.alocacaoNova.id=:a "
                + "and e.tipoEvento=:tipo", EventoAlocacaoVinculo.class).setParameter("v", vinculo)
                .setParameter("a", alocacao).setParameter("tipo", TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO).getSingleResult();
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoMapa(long mapaId, Instant k) {
        return emTransacao(() -> consultaProvas.configuracao(em.find(MapeamentoTransferenciaGestao.class, mapaId), k));
    }

    private ComposicaoContratualTemporalServico.ComposicaoProjetada agregado(Origem o, Instant k) {
        return emTransacao(() -> composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), o.dia(), k)
                .stream().filter(p -> p.lotacao().getId().equals(o.c().lotacaoB.getId())
                        && p.jornada().getId().equals(o.c().jornadaB.getId()) && p.cargo().getId().equals(o.c().cargoB.getId()))
                .findFirst().orElseThrow());
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoAlocacao(long id, LocalDate dia, Instant k) {
        return emTransacao(() -> resolvedor.resolverConfiguracao(em.find(AlocacaoVinculo.class, id), dia, k));
    }

    private String fotografiaAlocacao(long id) {
        return emTransacao(() -> {
            var a = em.find(AlocacaoVinculo.class, id);
            return mapper.valueToTree(mapa("id", a.getId(), "vinculo", a.getVinculo().getId(),
                    "composicao", a.getComposicao() == null ? null : a.getComposicao().getId(),
                    "lotacao", a.getLotacaoNomeFotografia(), "jornada", a.getJornadaDescricaoFotografia(),
                    "cargo", a.getCargoNomeFotografia(), "municipio", a.getLotacaoMunicipioFotografia(),
                    "grade", a.getJornadaSemanalFotografia(), "ausencia", a.getMunicipioAusenciaComprovada(),
                    "inicio", a.getDataInicio(), "fim", a.getDataFim(), "criado", a.getCriadoEm())).toString();
        });
    }

    private String fotografiaEventoOrigem(long vinculo, long alocacao) {
        return emTransacao(() -> {
            var e = eventoOrigem(vinculo, alocacao);
            return mapper.valueToTree(mapa("id", e.getId(), "registrado", e.getRegistradoEm(), "data", e.getDataEfetiva(),
                    "transferencia", e.getTransferenciaGestaoId(), "alocacao", e.getAlocacaoNova().getId(),
                    "anterior", e.getAlocacaoAnteriorId(), "cargoAnterior", e.getCargoAnteriorId(),
                    "cargoNovo", e.getCargoNovoId(), "hashAnterior", e.getHashAnterior(), "hashEvento", e.getHashEvento())).toString();
        });
    }

    private String fotografiasMapas(Origem o) {
        return emTransacao(() -> mapper.valueToTree(List.of(o.mapa1(), o.mapa2()).stream().map(id -> {
            var m = em.find(MapeamentoTransferenciaGestao.class, id);
            return mapa("id", m.getId(), "transferencia", m.getTransferencia().getId(),
                    "composicao", m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                    "cargoOrigem", m.getCargoOrigem().getId(), "cargoDestino", m.getCargoDestino().getId(),
                    "lotacao", m.getLotacaoDestinoNomeFotografia(), "jornada", m.getJornadaDestinoDescricaoFotografia(),
                    "cargo", m.getCargoDestinoNomeFotografia(), "municipio", m.getLotacaoDestinoMunicipioFotografia(),
                    "grade", m.getJornadaDestinoSemanalFotografia());
        }).toList()).toString());
    }

    private String fotografiaVersao(long id) {
        return emTransacao(() -> mapper.valueToTree(motor.paraVersaoDTO(em.find(VersaoContratoNegocial.class, id))).toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografiaNegocial(VersaoContratoNegocial v) {
        Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
        return foto;
    }

    private record Origem(Cenario c, NovoContrato n, long cargo2, long transferencia, long mapa1, long mapa2,
                          long evento, long versao, long publicacao, LocalDate data, LocalDate dia,
                          String grade, String municipio, Map<String, Object> fotografia) {}
}
