package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Reprodução permanente da incorporação parcial de fontes de transferência.
 * Integrado após baseline original conferida; dados exclusivamente sintéticos.
 *
 * Fontes e dados expressamente sintéticos. A perda simulada usa DML apenas nos
 * dados da fixture. O schema deve ser gerido exclusivamente pelo Hibernate.
 * MockMvc cobre controladores/autorização/serviços e PostgreSQL reais, não TCP/JAR.
 * A referência SEI é fonte permitida: o teste não impõe PDF obrigatório.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaTransferenciaRevisaoParcialHttpIT extends ContratosHttpBase {
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

    @ParameterizedTest(name = "origemPorId={0}, ausenciaMunicipal={1}, inclusaoAntesDaTransferencia={2}")
    @CsvSource({"true,false,false", "false,false,false", "true,true,false", "false,true,false",
            "true,false,true", "false,false,true", "true,true,true", "false,true,true"})
    void alocacaoEntreProvasDoMapaRespeitaOrigemConhecimentoEAusencia(
            boolean origemPorId, boolean ausenciaMunicipal, boolean inclusaoAntesDaTransferencia) throws Exception {
        Origem o = preparar(origemPorId, ausenciaMunicipal);
        String mapaOriginal = fotografiaMapa(o);
        String versaoOriginal = fotografiaVersao(o.versao());
        String publicacaoOriginal = fotografiaVersao(o.publicacao());
        Instant k0 = Instant.now();

        var primeiraReq = requisicao(o, o.versao(), null,
                mapa("mapeamentoId", o.mapa(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false));
        JsonNode primeira = incorporar(o, primeiraReq);
        Instant k1 = Instant.parse(primeira.path("registradoEm").asText());
        assertThat(k1).isAfter(k0);
        assertThat(configuracaoMapa(o, k0).jornadaSemanal()).isNull();
        assertThat(configuracaoMapa(o, k0).municipioComprovado()).isFalse();
        assertThat(configuracaoMapa(o, k1).jornadaSemanal()).isEqualTo(o.grade());
        assertThat(configuracaoMapa(o, k1).lotacaoMunicipio()).isNull();
        assertThat(configuracaoMapa(o, k1).municipioComprovado()).isFalse();

        long vinculo = enviar(o.c().localB, "/api/vinculos-contratuais", mapa(
                "contratoId", o.n().id(), "composicaoId", o.n().composicao1(),
                "lotacaoId", inclusaoAntesDaTransferencia ? o.c().lotacaoA.getId() : o.c().lotacaoB.getId(),
                "jornadaId", inclusaoAntesDaTransferencia ? o.c().jornadaA.getId() : o.c().jornadaB.getId(),
                "cargoId", inclusaoAntesDaTransferencia ? o.c().cargoA.getId() : o.c().cargoB.getId(),
                "dataInicio", inclusaoAntesDaTransferencia ? o.n().inicio().plusDays(1) : o.dia(),
                "pessoa", mapa("nome", "Pessoa sintética entre fontes do mapa",
                        "cpf", cpf(Math.toIntExact(750000000L + o.n().id())))), 201,
                "Grade comprovada viabiliza alocação, sem afirmar município ausente").path("id").asLong();
        String alocacaoOriginal = fotografiaAlocacao(vinculo);
        Instant kEntre = Instant.now();
        emTransacao(() -> {
            var a = alocacao(vinculo);
            if (inclusaoAntesDaTransferencia && !origemPorId) assertThat(a.getComposicao()).isNull();
            else assertThat(a.getComposicao().getId()).isEqualTo(o.n().composicao1());
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(o.grade());
            assertThat(a.getLotacaoMunicipioFotografia()).isNull();
            assertThat(a.getMunicipioAusenciaComprovada()).isFalse();
            return null;
        });
        assertThat(configuracaoAlocacao(vinculo, o.dia(), kEntre).municipioComprovado()).isFalse();

        JsonNode lancada = enviar(o.c().localB, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo, "dataReferencia", o.dia(), "status", "PRESENCA",
                "justificativaRetificacao", "Conferência sintética entre as provas parciais do mapa",
                "chaveIdempotencia", UUID.randomUUID().toString()), 200,
                "Frequência fechada usa a grade comprovada e preserva município desconhecido");
        long frequenciaId = lancada.path("id").asLong();
        String fotoFrequencia = fotografiaFrequencia(frequenciaId);
        JsonNode eventosFrequencia = consultar(o.c().localB, "/api/frequencias/" + frequenciaId + "/eventos",
                "Eventos originais antes da incorporação municipal");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, frequenciaId);
            assertThat(f.getEstadoNegocial().name()).isEqualTo("FECHADA");
            assertThat(f.getValidadeOperacional().name()).isEqualTo("VALIDA");
            assertThat(f.getLotacaoSnapshotMunicipio()).isNull();
            assertThat(f.getMinutosFisicos()).isEqualTo(480);
            return null;
        });

        var segundaReq = requisicao(o, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), mapa("mapeamentoId", o.mapa(),
                        "lotacaoMunicipio", o.municipio(), "municipioComprovado", true,
                        "jornadaComprovada", false));
        JsonNode segunda = incorporar(o, segundaReq);
        Instant k2 = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(k2).isAfter(kEntre);
        assertThat(segunda.path("numeroRevisao").asInt()).isEqualTo(2);
        assertThat(segunda.path("provaAnteriorId").asLong()).isEqualTo(primeira.path("id").asLong());

        // Compara dados recarregados em transações novas, não o cache de entidades.
        assertThat(fotografiaMapa(o)).isEqualTo(mapaOriginal);
        assertThat(fotografiaVersao(o.versao())).isEqualTo(versaoOriginal);
        assertThat(fotografiaVersao(o.publicacao())).isEqualTo(publicacaoOriginal);
        assertThat(fotografiaAlocacao(vinculo)).isEqualTo(alocacaoOriginal);
        assertThat(fotografiaFrequencia(frequenciaId)).isEqualTo(fotoFrequencia);
        assertThat(consultar(o.c().localB, "/api/frequencias/" + frequenciaId + "/eventos",
                "A incorporação preserva o evento original de frequência").get(0)).isEqualTo(eventosFrequencia.get(0));
        assertThat(configuracaoMapa(o, kEntre).lotacaoMunicipio()).isNull();
        assertThat(configuracaoMapa(o, kEntre).municipioComprovado()).isFalse();
        assertThat(configuracaoAlocacao(vinculo, o.dia(), kEntre).lotacaoMunicipio()).isNull();
        assertThat(configuracaoAlocacao(vinculo, o.dia(), kEntre).municipioComprovado()).isFalse();

        var projetada = configuracaoProjetada(o, k2);
        var depois = configuracaoAlocacao(vinculo, o.dia(), k2);
        // Esperado pelo domínio: ID e tríade identificam o mesmo recorte canônico.
        // No original inspecionado, a variante por tríade deve falhar aqui: mapa
        // tem composicaoOrigem=null, mas alocação/projeção têm a composição-base.
        assertAll("Prova municipal chega apenas em K2 e não altera a fotografia persistida",
                () -> assertThat(configuracaoMapa(o, k2).municipioComprovado()).isTrue(),
                () -> assertThat(projetada.municipioComprovado()).isTrue(),
                () -> assertThat(projetada.lotacaoMunicipio()).isEqualTo(o.municipio()),
                () -> assertThat(depois.municipioComprovado()).isTrue(),
                () -> assertThat(depois.lotacaoMunicipio()).isEqualTo(o.municipio()),
                () -> assertThat(depois.jornadaSemanal()).isEqualTo(o.grade()),
                () -> assertThat(emTransacao(() -> em.find(Frequencia.class, frequenciaId)
                        .getValidadeOperacional().name())).isEqualTo(ausenciaMunicipal ? "VALIDA" : "SUJEITA_REVISAO"),
                () -> assertThat(emTransacao(() -> em.createQuery(
                        "select count(p) from PendenciaAdministrativa p where p.frequenciaId=:f "
                                + "and p.estado<>:resolvida", Long.class)
                        .setParameter("f", frequenciaId)
                        .setParameter("resolvida", com.example.siggtrat.dominio.pendencia.EstadoPendencia.RESOLVIDA)
                        .getSingleResult())).isEqualTo(ausenciaMunicipal ? 0L : 1L));
    }

    @ParameterizedTest(name = "convergenciaComGradeComprovada={0}")
    @ValueSource(booleans = {true, false})
    void convergenciaDeMapasNaoEscolheArbitrariamenteAUltimaFotografia(boolean gradeComprovada) throws Exception {
        Origem o = preparar(true, false, true);
        JsonNode primeira = null;
        if (gradeComprovada) primeira = incorporar(o, requisicao(o, o.versao(), null,
                mapa("mapeamentoId", o.mapa(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false)));
        Instant antes = Instant.now();
        var agregado = configuracaoProjetada(o, antes);
        assertThat(agregado.efetivoPrevisto()).isEqualTo(5);
        assertThat(agregado.lotacaoMunicipio()).isNull();
        assertThat(agregado.municipioComprovado()).isFalse();
        if (gradeComprovada) assertThat(agregado.jornadaSemanal()).isEqualTo(o.grade());
        else assertThat(agregado.jornadaSemanal()).isNull();

        String estadoAntesDaInclusao = estadoPersistidoDaInclusao(o.n().id());
        JsonNode resposta = enviar(o.c().localB, "/api/vinculos-contratuais", mapa(
                "contratoId", o.n().id(), "composicaoId", o.n().composicao1(),
                "lotacaoId", o.c().lotacaoB.getId(), "jornadaId", o.c().jornadaB.getId(),
                "cargoId", o.c().cargoB.getId(), "dataInicio", o.dia(),
                "pessoa", mapa("nome", "Pessoa sintética em recorte agregado",
                        "cpf", cpf(Math.toIntExact(760000000L + o.n().id())))), gradeComprovada ? 201 : 409,
                "Uma origem convergente incompleta não autoriza escolher o último mapa nem inferir sua grade");
        if (!gradeComprovada) {
            assertThat(estadoPersistidoDaInclusao(o.n().id())).isEqualTo(estadoAntesDaInclusao);
            assertThat(emTransacao(() -> em.createQuery(
                    "select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                    .setParameter("c", o.n().id()).getSingleResult())).isZero();
            return;
        }
        long vinculo = resposta.path("id").asLong();
        String alocacaoOriginal = fotografiaAlocacao(vinculo);
        emTransacao(() -> {
            assertThat(alocacao(vinculo).getLotacaoMunicipioFotografia()).isNull();
            assertThat(alocacao(vinculo).getMunicipioAusenciaComprovada()).isFalse();
            return null;
        });
        Instant entre = Instant.now();
        JsonNode segunda = incorporar(o, requisicao(o, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), mapa("mapeamentoId", o.mapa(),
                        "lotacaoMunicipio", o.municipio(), "municipioComprovado", true,
                        "jornadaComprovada", false)));
        Instant depois = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(configuracaoAlocacao(vinculo, o.dia(), entre).municipioComprovado()).isFalse();
        assertThat(configuracaoAlocacao(vinculo, o.dia(), depois).municipioComprovado()).isTrue();
        assertThat(configuracaoAlocacao(vinculo, o.dia(), depois).lotacaoMunicipio()).isEqualTo(o.municipio());
        assertThat(fotografiaAlocacao(vinculo)).isEqualTo(alocacaoOriginal);
    }

    private String estadoPersistidoDaInclusao(long contrato) {
        return emTransacao(() -> mapper.valueToTree(mapa(
                "versaoContrato", em.find(Contrato.class, contrato).getVersion(),
                "pessoas", em.createQuery("select count(p) from Pessoa p", Long.class).getSingleResult(),
                "vinculos", em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                        .setParameter("c", contrato).getSingleResult(),
                "alocacoes", em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", contrato).getSingleResult(),
                "eventos", em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                        .setParameter("c", contrato).getSingleResult(),
                "versoes", em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                        .setParameter("c", contrato).getSingleResult())).toString());
    }

    private Origem preparar(boolean origemPorId, boolean ausenciaMunicipal) throws Exception {
        return preparar(origemPorId, ausenciaMunicipal, false);
    }

    private Origem preparar(boolean origemPorId, boolean ausenciaMunicipal, boolean convergente) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, convergente ? 2 : null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        emTransacao(() -> {
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio(
                    ausenciaMunicipal ? null : "MUNICIPIO DOCUMENTADO " + n.id());
            return null;
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data = RelogioSistema.hoje().minusDays(20);
        LocalDate dia = RelogioSistema.hoje().minusDays(7)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<String, Object> item = mapa("lotacaoDestinoId", c.lotacaoB.getId(),
                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId());
        if (origemPorId) item.put("composicaoOrigemId", n.composicao1());
        else {
            item.put("lotacaoOrigemId", c.lotacaoA.getId());
            item.put("jornadaOrigemId", c.jornadaA.getId());
            item.put("cargoOrigemId", c.cargoA.getId());
        }
        Map<String, Object> req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência sintética de recorte identificado explicitamente",
                "referenciaSei", "SEI-SINTETICO-MAPA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", convergente ? List.of(item, mapa("composicaoOrigemId", n.composicao2(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId())) : List.of(item));
        JsonNode previa = enviar(c.localA, base(n.id()) + "/transferencias-gestao/previa", req, 200,
                "Prévia deve aceitar origem por ID ou pela tríade canônica");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "Transferência sintética válida");
        Origem o = emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c",
                    TransferenciaGestaoContrato.class).setParameter("c", n.id()).getSingleResult();
            var m = t.getMapeamentos().get(0);
            if (convergente) {
                assertThat(t.getMapeamentos()).hasSize(2);
                assertThat(m.getComposicaoOrigem().getId()).isEqualTo(n.composicao1());
            }
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e",
                    VersaoContratoNegocial.class).setParameter("e", t.getEvento().getId()).getSingleResult();
            var p = motor.fotografiaOriginalConhecida(t.getContrato(), Instant.now()).orElseThrow();
            if (origemPorId) assertThat(m.getComposicaoOrigem().getId()).isEqualTo(n.composicao1());
            else assertThat(m.getComposicaoOrigem()).isNull();
            String grade = m.getJornadaDestinoSemanalFotografia();
            String municipio = m.getLotacaoDestinoMunicipioFotografia();
            assertThat(grade).isNotBlank();
            if (ausenciaMunicipal) assertThat(municipio).isNull();
            else assertThat(municipio).isNotBlank();
            Map<String, Object> foto = fotografiaNegocial(v);
            // Apenas a fixture simula a perda das duas propriedades do mapa.
            assertThat(em.createQuery("update MapeamentoTransferenciaGestao m set "
                    + "m.jornadaDestinoSemanalFotografia=null, m.lotacaoDestinoMunicipioFotografia=null "
                    + "where m.id=:m").setParameter("m", m.getId()).executeUpdate()).isEqualTo(1);
            return new Origem(c, n, m.getId(), t.getEvento().getId(), v.getId(), p.getId(),
                    data, dia, grade, municipio, foto);
        });
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "INCORPORACAO_PROVA_HISTORICA",
                "usuarioBeneficiarioId", c.localB.getId(), "contratoId", n.id(),
                "finalidade", "Conferência sintética das propriedades do mapa",
                "justificativa", "Fontes SEI sintéticas separadas para grade e município",
                "autorizacaoAdministrativa", "ATO-CONFERENCIA-MAPA",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "Concessão ao responsável atual");
        if (!ausenciaMunicipal) enviar(c.localB, "/api/calendario-eventos", mapa(
                "data", dia, "tipo", "FERIADO", "descricao", "Feriado municipal sintético do mapa",
                "abrangencia", "MUNICIPAL", "municipio", o.municipio(), "ativo", true), 201,
                "Fonte municipal não é aplicada enquanto o município do mapa está desconhecido");
        // O serviço valida o município do evento contra as lotações da Gestão.
        // A fonte nasce legitimamente antes da divergência deliberada do mestre.
        emTransacao(() -> {
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio("MUNICIPIO ATUAL SEM PROVA " + n.id());
            return null;
        });
        return o;
    }

    private String fotografiaFrequencia(long id) {
        return emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            return mapper.valueToTree(mapa("id", f.getId(), "vinculo", f.getVinculoContratual().getId(),
                    "alocacao", f.getAlocacaoVinculo().getId(), "autor", f.getAutorOriginalId(),
                    "municipio", f.getLotacaoSnapshotMunicipio(), "entrada1", f.getJornadaSnapshotEntrada1(),
                    "saida1", f.getJornadaSnapshotSaida1(), "entrada2", f.getJornadaSnapshotEntrada2(),
                    "saida2", f.getJornadaSnapshotSaida2(), "minutos", f.getMinutosFisicos(),
                    "data", f.getDataReferencia(), "estado", f.getEstadoNegocial())).toString();
        });
    }

    private Map<String, Object> requisicao(Origem o, long versao, Long anterior, Map<String, Object> item) {
        return mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", versao, "provaAnteriorId", anterior,
                "dataDocumento", o.data(), "referenciaSei", "SEI-SINTETICO-PROPRIEDADE-MAPA",
                "motivo", "Incorporar apenas a propriedade declarada na fonte sintética",
                "autorizacaoAdministrativa", "ATO-CONFERENCIA-MAPA", "versaoContratoEsperada",
                emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()),
                "composicoes", List.of(item));
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(o.c().localB, rota + "/previa", req, 200,
                "Prévia da propriedade documentada no mapa").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localB, rota, req, 201, "Incorporação parcial encadeada autorizada");
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoMapa(Origem o, Instant k) {
        return emTransacao(() -> consultaProvas.configuracao(em.find(MapeamentoTransferenciaGestao.class, o.mapa()), k));
    }

    private ComposicaoContratualTemporalServico.ComposicaoProjetada configuracaoProjetada(Origem o, Instant k) {
        return emTransacao(() -> composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), o.dia(), k)
                .stream().filter(p -> Long.valueOf(o.n().composicao1()).equals(p.composicaoBaseId())).findFirst().orElseThrow());
    }

    private ConsultaProvaHistoricaContratoServico.Configuracao configuracaoAlocacao(long vinculo, LocalDate dia, Instant k) {
        return emTransacao(() -> resolvedor.resolverConfiguracao(alocacao(vinculo), dia, k));
    }

    private AlocacaoVinculo alocacao(long vinculo) {
        return em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v "
                        + "order by a.dataInicio desc, a.id desc", AlocacaoVinculo.class)
                .setParameter("v", vinculo).setMaxResults(1).getSingleResult();
    }

    private String fotografiaAlocacao(long vinculo) {
        return emTransacao(() -> {
            var a = alocacao(vinculo);
            return mapper.valueToTree(mapa("id", a.getId(), "vinculo", a.getVinculo().getId(),
                    "composicao", a.getComposicao() == null ? null : a.getComposicao().getId(),
                    "lotacao", a.getLotacaoNomeFotografia(), "jornada", a.getJornadaDescricaoFotografia(),
                    "cargo", a.getCargoNomeFotografia(), "municipio", a.getLotacaoMunicipioFotografia(),
                    "grade", a.getJornadaSemanalFotografia(), "ausenciaMunicipal", a.getMunicipioAusenciaComprovada(),
                    "origemAdi", a.getAditamentoComposicaoFotografia() == null ? null : a.getAditamentoComposicaoFotografia().getId(),
                    "inicio", a.getDataInicio(), "fim", a.getDataFim(), "criadoEm", a.getCriadoEm())).toString();
        });
    }

    private String fotografiaMapa(Origem o) {
        return emTransacao(() -> {
            var m = em.find(MapeamentoTransferenciaGestao.class, o.mapa());
            return mapper.valueToTree(mapa("id", m.getId(), "transferencia", m.getTransferencia().getId(),
                    "composicao", m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                    "lotacaoOrigem", m.getLotacaoOrigem().getId(), "jornadaOrigem", m.getJornadaOrigem().getId(),
                    "cargoOrigem", m.getCargoOrigem().getId(), "lotacaoDestino", m.getLotacaoDestino().getId(),
                    "jornadaDestino", m.getJornadaDestino().getId(), "cargoDestino", m.getCargoDestino().getId(),
                    "lotacao", m.getLotacaoDestinoNomeFotografia(), "jornada", m.getJornadaDestinoDescricaoFotografia(),
                    "cargo", m.getCargoDestinoNomeFotografia(), "municipio", m.getLotacaoDestinoMunicipioFotografia(),
                    "grade", m.getJornadaDestinoSemanalFotografia())).toString();
        });
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

    private record Origem(Cenario c, NovoContrato n, long mapa, long evento, long versao, long publicacao,
                          LocalDate data, LocalDate dia, String grade, String municipio, Map<String, Object> fotografia) {}
}
