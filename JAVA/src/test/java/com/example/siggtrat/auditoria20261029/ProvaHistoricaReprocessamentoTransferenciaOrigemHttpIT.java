package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.MotorTemporalAlocacaoServico;
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
 * Reprodução permanente de CON-023:006 com cadeia real de transferências.
 * T1 A->B mantém duas combinações; T2 B->A converge por tríades e tem grade-only.
 * Retificar T1 reaplica T2 e cria uma faixa cujo evento deve preservar a origem
 * real já disponível em FaixaPlanejada.anterior, sem inferir mapas legados.
 *
 * A única simulação de perda é DML em grade/município dos mapas de T2. Nenhuma
 * origem ou evento da alocação é fabricado/alterado na fixture. Demais atos são
 * realizados por HTTP MockMvc com serviços/autorização e MySQL reais.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaReprocessamentoTransferenciaOrigemHttpIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motorContrato;
    @Autowired MotorTemporalAlocacaoServico motorAlocacao;
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
    void retificarTransferenciaAnteriorPreservaOrigemDaFaixaReprocessadaDeMapaParcial() throws Exception {
        Origem o = preparar();
        String mapasOriginais = fotografiasMapas(o);
        String publicacaoOriginal = fotografiaVersao(o.publicacao());
        String versaoOriginalT2 = fotografiaVersao(o.versaoT2());
        JsonNode primeira = incorporar(o, requisicao(o, o.versaoT2(), null, List.of(
                mapa("mapeamentoId", o.mapa1(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false),
                mapa("mapeamentoId", o.mapa2(), "jornadaSemanal", o.grade(),
                        "jornadaComprovada", true, "municipioComprovado", false))));

        long vinculo = enviar(o.c().localA, "/api/vinculos-contratuais", mapa(
                "contratoId", o.n().id(), "composicaoId", o.n().composicao2(),
                "dataInicio", o.n().inicio().plusDays(1), "pessoa", mapa(
                        "nome", "Pessoa sintética da segunda origem em cadeia retificada",
                        "cpf", cpf(Math.toIntExact(780000000L + o.n().id())))), 201,
                "Inclusão anterior a T1 reaplica T1 e T2 pela origem real da pessoa").path("id").asLong();
        long alocacaoAntiga = alocacaoEm(vinculo, o.dia(), Instant.now());
        String fotoAntiga = fotografiaAlocacao(alocacaoAntiga);
        List<Long> eventosAntigos = idsEventos(vinculo);
        String fotosEventosAntigos = fotografiasEventos(eventosAntigos);
        assertThat(configuracao(alocacaoAntiga, o.dia(), Instant.now()).municipioComprovado()).isFalse();
        Instant antesDaRetificacao = Instant.now();
        List<Long> decisoesOriginais = decisoesIndependentes(vinculo, antesDaRetificacao);
        assertThat(decisoesOriginais).hasSize(1);

        // T1 muda só de data. T2 continua válido, convergente e com grade-only.
        var retificacao = transferenciaT1(o.c(), o.n(), o.cargoB2(), o.dataT1().plusDays(1));
        retificacao.put("eventoPrecedenteId", o.eventoT1());
        retificacao.put("relacaoPrecedencia", "RETIFICA");
        retificacao.put("motivo", "Retificar data de T1 mantendo T2 e sua fonte documental parcial");
        transferir(o.c().localA, o.n().id(), retificacao);

        long alocacaoNova = alocacaoEm(vinculo, o.dia(), Instant.now());
        assertThat(alocacaoNova).isNotEqualTo(alocacaoAntiga);
        assertThat(alocacaoEm(vinculo, o.dia(), antesDaRetificacao)).isEqualTo(alocacaoAntiga);
        long alocacaoOrigemReal = alocacaoEm(vinculo, o.dataT2().minusDays(1), Instant.now());
        String fotoOrigemReal = fotografiaRelacaoOrigem(alocacaoOrigemReal);
        String fotoNova = fotografiaAlocacao(alocacaoNova);
        OrigemEvento novoEvento = emTransacao(() -> {
            var e = em.createQuery("select e from EventoAlocacaoVinculo e where e.vinculo.id=:v "
                    + "and e.alocacaoNova.id=:a and e.transferenciaGestaoId=:t and e.tipoEvento=:tipo",
                    EventoAlocacaoVinculo.class).setParameter("v", vinculo).setParameter("a", alocacaoNova)
                    .setParameter("t", o.transferenciaT2()).setParameter("tipo", TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA)
                    .getSingleResult();
            assertThat(e.getRegistradoEm()).isAfter(antesDaRetificacao);
            assertThat(e.getLotacaoNovaId()).isEqualTo(o.c().lotacaoA.getId());
            assertThat(e.getJornadaNovaId()).isEqualTo(o.c().jornadaA.getId());
            assertThat(e.getCargoNovoId()).isEqualTo(o.c().cargoA.getId());
            return new OrigemEvento(e.getId(), e.getAlocacaoAnteriorId(), e.getLotacaoAnteriorId(),
                    e.getJornadaAnteriorId(), e.getCargoAnteriorId(), e.getRegistradoEm(),
                    mapper.valueToTree(mapa("id", e.getAlocacaoAnteriorId(), "composicao", e.getComposicaoAnteriorId(),
                            "lotacaoId", e.getLotacaoAnteriorId(), "lotacaoNome", e.getLotacaoAnteriorNome(),
                            "jornadaId", e.getJornadaAnteriorId(), "jornadaDescricao", e.getJornadaAnteriorDescricao(),
                            "cargoId", e.getCargoAnteriorId(), "cargoNome", e.getCargoAnteriorNome(),
                            "inicio", e.getInicioAnterior(), "fim", e.getFimAnterior(), "estado", e.getEstadoAnterior())).toString());
        });
        String fotoEventoNovo = fotografiasEventos(List.of(novoEvento.id()));
        Instant entreReprocessamentoEMunicipio = Instant.now();
        assertThat(configuracao(alocacaoNova, o.dia(), entreReprocessamentoEMunicipio).jornadaSemanal()).isEqualTo(o.grade());
        assertThat(configuracao(alocacaoNova, o.dia(), entreReprocessamentoEMunicipio).municipioComprovado()).isFalse();

        // Só o mapa de retorno da segunda origem recebe município. O agregado
        // continua desconhecido; a nova faixa reprocessada tem fonte individual.
        JsonNode segunda = incorporar(o, requisicao(o, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), List.of(mapa("mapeamentoId", o.mapa2(),
                        "lotacaoMunicipio", o.municipio(), "municipioComprovado", true,
                        "jornadaComprovada", false))));
        Instant kMunicipio = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(kMunicipio).isAfter(entreReprocessamentoEMunicipio);

        // Estado/fim das projeções antigas pode ser encerrado por novos eventos;
        // fotografias negociais e eventos antigos, porém, permanecem imutáveis.
        assertThat(fotografiaAlocacao(alocacaoAntiga)).isEqualTo(fotoAntiga);
        assertThat(fotografiaAlocacao(alocacaoNova)).isEqualTo(fotoNova);
        assertThat(fotografiasEventos(eventosAntigos)).isEqualTo(fotosEventosAntigos);
        assertThat(fotografiasEventos(List.of(novoEvento.id()))).isEqualTo(fotoEventoNovo);
        assertThat(fotografiasMapas(o)).isEqualTo(mapasOriginais);
        assertThat(fotografiaVersao(o.publicacao())).isEqualTo(publicacaoOriginal);
        assertThat(fotografiaVersao(o.versaoT2())).isEqualTo(versaoOriginalT2);
        assertThat(configuracao(alocacaoNova, o.dia(), entreReprocessamentoEMunicipio).municipioComprovado()).isFalse();
        assertThat(configuracao(alocacaoAntiga, o.dia(), antesDaRetificacao).municipioComprovado()).isFalse();
        var depois = configuracao(alocacaoNova, o.dia(), kMunicipio);
        var agregado = emTransacao(() -> composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), o.dia(), kMunicipio)
                .stream().filter(p -> p.lotacao().getId().equals(o.c().lotacaoA.getId())
                        && p.jornada().getId().equals(o.c().jornadaA.getId()) && p.cargo().getId().equals(o.c().cargoA.getId()))
                .findFirst().orElseThrow());

        // T3 torna a faixa T2 reprocessada a "anterior" de uma transferência
        // normal. Sem corrigir o discriminador do motor, ela seria importada
        // como decisão humana independente, em vez de continuar derivada.
        Instant antesDaTerceira = Instant.now();
        transferir(o.c().localA, o.n().id(), mapa("dataEfetiva", RelogioSistema.hoje().minusDays(1),
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", o.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", o.c().tipoB.getId(), "motivo", "T3 posterior testa proveniência derivada após reprocessamento",
                "referenciaSei", "SEI-T3-POSTERIOR", "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(
                        mapa("lotacaoOrigemId", o.c().lotacaoA.getId(), "jornadaOrigemId", o.c().jornadaA.getId(),
                                "cargoOrigemId", o.c().cargoA.getId(), "lotacaoDestinoId", o.c().lotacaoB.getId(),
                                "jornadaDestinoId", o.c().jornadaB.getId(), "cargoDestinoId", o.c().cargoB.getId()))));
        List<Long> decisoesDepoisDaTerceira = decisoesIndependentes(vinculo, Instant.now());
        assertThat(alocacaoEm(vinculo, RelogioSistema.hoje(), antesDaTerceira)).isEqualTo(alocacaoNova);
        assertThat(alocacaoEm(vinculo, RelogioSistema.hoje(), Instant.now())).isNotEqualTo(alocacaoNova);
        assertThat(alocacaoEm(vinculo, o.dia(), antesDaRetificacao)).isEqualTo(alocacaoAntiga);
        assertThat(decisoesIndependentes(vinculo, antesDaRetificacao)).isEqualTo(decisoesOriginais);
        assertThat(fotografiasEventos(List.of(novoEvento.id()))).isEqualTo(fotoEventoNovo);
        assertAll("Novo evento deve conservar a origem real já disponível no reprocessamento",
                () -> assertThat(novoEvento.alocacaoAnterior()).isEqualTo(alocacaoOrigemReal),
                () -> assertThat(novoEvento.fotografiaAnterior()).isEqualTo(fotoOrigemReal),
                () -> assertThat(novoEvento.lotacaoAnterior()).isEqualTo(o.c().lotacaoB.getId()),
                () -> assertThat(novoEvento.jornadaAnterior()).isEqualTo(o.c().jornadaB.getId()),
                () -> assertThat(novoEvento.cargoAnterior()).isEqualTo(o.cargoB2()),
                () -> assertThat(agregado.municipioComprovado()).isFalse(),
                () -> assertThat(agregado.lotacaoMunicipio()).isNull(),
                () -> assertThat(depois.municipioComprovado()).isTrue(),
                () -> assertThat(depois.lotacaoMunicipio()).isEqualTo(o.municipio()),
                () -> assertThat(depois.jornadaSemanal()).isEqualTo(o.grade()),
                () -> assertThat(decisoesDepoisDaTerceira).isEqualTo(decisoesOriginais));
    }

    private Origem preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 6, 3, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long cargoB2 = emTransacao(() -> {
            Cargo cargo = new Cargo("Destino B secundário reprocessamento " + n.id(), "2");
            cargo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoB.getId()));
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(cargo.getId());
            origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética de disponibilidade do cargo B2 desde a vigência"); em.persist(origem);
            em.find(Lotacao.class, c.lotacaoA.getId()).setMunicipio("MUNICIPIO FONTE T2 " + n.id());
            return cargo.getId();
        });
        LocalDate dataT1 = RelogioSistema.hoje().minusDays(20);
        LocalDate dataT2 = RelogioSistema.hoje().minusDays(10);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        transferir(c.localA, n.id(), transferenciaT1(c, n, cargoB2, dataT1));
        long eventoT1 = emTransacao(() -> em.createQuery("select t.evento.id from TransferenciaGestaoContrato t where t.contrato.id=:c",
                Long.class).setParameter("c", n.id()).getSingleResult());
        concederPara(c, n.id(), c.localB, "TRANSFERENCIA_GESTAO_LOCAL", "ATO-HTTP");
        transferir(c.localB, n.id(), mapa("dataEfetiva", dataT2, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoA.getId(), "tipoContratacaoDestinoId", c.tipoA.getId(),
                "motivo", "Retorno convergente documentado por tríades", "referenciaSei", "SEI-T2-CONVERGENTE",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(
                        mapaRetorno(c, c.cargoB.getId()), mapaRetorno(c, cargoB2))));
        Origem o = emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c and t.dataEfetiva=:d",
                    TransferenciaGestaoContrato.class).setParameter("c", n.id()).setParameter("d", dataT2).getSingleResult();
            var m1 = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(c.cargoB.getId())).findFirst().orElseThrow();
            var m2 = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(cargoB2)).findFirst().orElseThrow();
            assertThat(m1.getComposicaoOrigem()).isNull(); assertThat(m2.getComposicaoOrigem()).isNull();
            String grade = m2.getJornadaDestinoSemanalFotografia();
            String municipio = m2.getLotacaoDestinoMunicipioFotografia();
            assertThat(grade).isNotBlank().isEqualTo(m1.getJornadaDestinoSemanalFotografia());
            assertThat(municipio).isNotBlank();
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e",
                    VersaoContratoNegocial.class).setParameter("e", t.getEvento().getId()).getSingleResult();
            var p = motorContrato.fotografiaOriginalConhecida(t.getContrato(), Instant.now()).orElseThrow();
            var foto = fotografiaNegocial(v);
            // Somente perda de propriedades da fixture; nunca de origem/evento.
            assertThat(em.createQuery("update MapeamentoTransferenciaGestao m set m.jornadaDestinoSemanalFotografia=null, "
                    + "m.lotacaoDestinoMunicipioFotografia=null where m.transferencia.id=:t")
                    .setParameter("t", t.getId()).executeUpdate()).isEqualTo(2);
            em.find(Lotacao.class, c.lotacaoA.getId()).setMunicipio("MESTRE ATUAL NAO COMPROVA T2 " + n.id());
            return new Origem(c, n, cargoB2, eventoT1, t.getId(), t.getEvento().getId(), m1.getId(), m2.getId(),
                    v.getId(), p.getId(), dataT1, dataT2, RelogioSistema.hoje().minusDays(5), grade, municipio, foto);
        });
        concederPara(c, n.id(), c.localA, "INCORPORACAO_PROVA_HISTORICA", "ATO-FONTE-T2");
        return o;
    }

    private Map<String, Object> transferenciaT1(Cenario c, NovoContrato n, long cargoB2, LocalDate data) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "T1 preserva duas combinações de origem",
                "referenciaSei", "SEI-T1-RETIFICAVEL", "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(
                        mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()),
                        mapa("composicaoOrigemId", n.composicao2(), "lotacaoDestinoId", c.lotacaoB.getId(),
                                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", cargoB2)));
    }

    private Map<String, Object> mapaRetorno(Cenario c, long cargoOrigem) {
        return mapa("lotacaoOrigemId", c.lotacaoB.getId(), "jornadaOrigemId", c.jornadaB.getId(), "cargoOrigemId", cargoOrigem,
                "lotacaoDestinoId", c.lotacaoA.getId(), "jornadaDestinoId", c.jornadaA.getId(), "cargoDestinoId", c.cargoA.getId());
    }

    private void transferir(UsuarioAplicacao usuario, long contrato, Map<String, Object> req) throws Exception {
        var previa = enviar(usuario, base(contrato) + "/transferencias-gestao/previa", req, 200, "Prévia do ato/retificação com posteriores");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(usuario, base(contrato) + "/transferir-gestao", req, 200, "Ato autorizado conserva a cadeia posterior válida");
    }

    private void concederPara(Cenario c, long contrato, UsuarioAplicacao usuario, String tipo, String ato) throws Exception {
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", tipo, "usuarioBeneficiarioId", usuario.getId(),
                "contratoId", contrato, "finalidade", "Reprodução sintética da cadeia temporal",
                "justificativa", "Fonte e retificação específicas da fixture", "autorizacaoAdministrativa", ato,
                "validoAte", Instant.now().plusSeconds(7200)), 201, "Concessão ao responsável atual");
    }

    private Map<String, Object> requisicao(Origem o, long versao, Long anterior, List<Map<String, Object>> itens) {
        return mapa("eventoOrigemId", o.eventoT2(), "versaoAnteriorId", versao, "provaAnteriorId", anterior,
                "dataDocumento", o.dataT2(), "referenciaSei", "SEI-SINTETICO-FONTE-T2",
                "motivo", "Incorporar somente propriedade comprovada do mapa de retorno",
                "autorizacaoAdministrativa", "ATO-FONTE-T2", "versaoContratoEsperada",
                emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()), "composicoes", itens);
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(o.c().localA, rota + "/previa", req, 200, "Prévia de fonte parcial de T2").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localA, rota, req, 201, "Fonte parcial preserva cadeia e K");
    }

    private long alocacaoEm(long vinculo, LocalDate data, Instant k) {
        return emTransacao(() -> motorAlocacao.reconstruir(em.find(VinculoContratual.class, vinculo), data, k).alocacaoId());
    }

    private List<Long> decisoesIndependentes(long vinculo, Instant k) {
        return emTransacao(() -> motorAlocacao.reconstruirDecisoesIndependentes(
                em.find(VinculoContratual.class, vinculo), k).stream().map(MotorTemporalAlocacaoServico.FaixaProjetada::id).toList());
    }

    private String fotografiaRelacaoOrigem(long id) {
        return emTransacao(() -> {
            var a = em.find(AlocacaoVinculo.class, id);
            return mapper.valueToTree(mapa("id", a.getId(), "composicao", a.getComposicao() == null ? null : a.getComposicao().getId(),
                    "lotacaoId", a.getLotacao().getId(), "lotacaoNome", a.getLotacaoNomeFotografia(),
                    "jornadaId", a.getJornada().getId(), "jornadaDescricao", a.getJornadaDescricaoFotografia(),
                    "cargoId", a.getCargo().getId(), "cargoNome", a.getCargoNomeFotografia(),
                    "inicio", a.getDataInicio(), "fim", a.getDataFim(), "estado", a.getEstadoNegocial())).toString();
        });
    }

    private com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico.Configuracao configuracao(long id, LocalDate data, Instant k) {
        return emTransacao(() -> resolvedor.resolverConfiguracao(em.find(AlocacaoVinculo.class, id), data, k));
    }

    private String fotografiaAlocacao(long id) {
        return emTransacao(() -> {
            var a = em.find(AlocacaoVinculo.class, id);
            return mapper.valueToTree(mapa("id", a.getId(), "vinculo", a.getVinculo().getId(),
                    "composicao", a.getComposicao() == null ? null : a.getComposicao().getId(),
                    "lotacao", a.getLotacaoNomeFotografia(), "jornada", a.getJornadaDescricaoFotografia(),
                    "cargo", a.getCargoNomeFotografia(), "municipio", a.getLotacaoMunicipioFotografia(),
                    "grade", a.getJornadaSemanalFotografia(), "ausencia", a.getMunicipioAusenciaComprovada(),
                    "criado", a.getCriadoEm())).toString();
        });
    }

    private List<Long> idsEventos(long vinculo) {
        return emTransacao(() -> em.createQuery("select e.id from EventoAlocacaoVinculo e where e.vinculo.id=:v order by e.id", Long.class)
                .setParameter("v", vinculo).getResultList());
    }

    private String fotografiasEventos(List<Long> ids) {
        return emTransacao(() -> mapper.valueToTree(ids.stream().map(id -> {
            var e = em.find(EventoAlocacaoVinculo.class, id);
            return mapa("id", e.getId(), "tipo", e.getTipoEvento(), "registrado", e.getRegistradoEm(),
                    "transferencia", e.getTransferenciaGestaoId(), "alocacao", e.getAlocacaoNova().getId(),
                    "anterior", e.getAlocacaoAnteriorId(), "lotacaoAnterior", e.getLotacaoAnteriorId(),
                    "jornadaAnterior", e.getJornadaAnteriorId(), "cargoAnterior", e.getCargoAnteriorId(),
                    "hashAnterior", e.getHashAnterior(), "hashEvento", e.getHashEvento());
        }).toList()).toString());
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
        return emTransacao(() -> mapper.valueToTree(motorContrato.paraVersaoDTO(em.find(VersaoContratoNegocial.class, id))).toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografiaNegocial(VersaoContratoNegocial v) {
        Map<String, Object> foto = mapper.convertValue(motorContrato.paraVersaoDTO(v), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
        return foto;
    }

    private record Origem(Cenario c, NovoContrato n, long cargoB2, long eventoT1, long transferenciaT2,
                          long eventoT2, long mapa1, long mapa2, long versaoT2, long publicacao,
                          LocalDate dataT1, LocalDate dataT2, LocalDate dia, String grade,
                          String municipio, Map<String, Object> fotografia) {}
    private record OrigemEvento(long id, Long alocacaoAnterior, Long lotacaoAnterior,
                                Long jornadaAnterior, Long cargoAnterior, Instant registradoEm, String fotografiaAnterior) {}
}
