package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Controle de regressão R01: CAT-004:001/003 e recorte de HIS-001:001.
 * Preparado sem execução; o RED deve ser vinculado ao hash efetivamente testado.
 *
 * Mapas, vínculos, fontes SEI e pessoas são sintéticos. A única lacuna simulada
 * por DML são os três rótulos dos mapas, como strings vazias (colunas NOT NULL).
 * Grade, município, origens, eventos, predecessor e fotografias individuais
 * não são fabricados nem corrigidos por DML. Fontes novas entram por HTTP.
 *
 * MockMvc percorre dispatcher/filtros/serviços reais, com PostgreSQL 18.6 e Hibernate.
 * A base suprime o agendador; esta classe não substitui persistência por mocks.
 * Consulta HTTP de composições tem data, mas não K: os cortes K são exercitados
 * no serviço temporal real, em transações novas, sem inventar parâmetro de API.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Execution(ExecutionMode.SAME_THREAD)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ProvaHistoricaTransferenciaConvergenteRotulosHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private Environment ambiente;
    @Autowired private ComposicaoContratualRepositorio composicoes;
    @Autowired private ComposicaoContratualTemporalServico composicaoTemporal;
    @Autowired private ConsultaProvaHistoricaContratoServico consultaProvas;
    @Autowired private MotorTemporalContratoServico motor;
    @Autowired private ResolvedorContextoTemporalFrequenciaServico resolvedor;

    @BeforeEach
    void exigirMysqlRealSemSchemaManual() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void mapaM2SemRotulosNaoEmprestaM1AoAgregadoNemAInclusaoDireta() throws Exception {
        exercitar(false);
    }

    @Test
    void fontesDivergentesDosTresRotulosNaoEscolhemUmaOrigemDoAgregado() throws Exception {
        exercitar(true);
    }

    private void exercitar(boolean divergentes) throws Exception {
        Origem o = preparar();
        Rotulos m1 = new Rotulos("Lotação documental M1 " + o.n().id(),
                "Jornada documental M1 " + o.n().id(), "Cargo documental M1 " + o.n().id());
        Rotulos m2 = divergentes
                ? new Rotulos("Lotação documental M2 " + o.n().id(),
                        "Jornada documental M2 " + o.n().id(), "Cargo documental M2 " + o.n().id())
                : m1;
        String mapasOriginais = fotografiaMapas(o);
        String versoesOriginais = fotografiaVersoes(o);
        Instant k0 = Instant.now();
        Recorte semProvas = agregado(o, k0);

        JsonNode primeira = incorporar(o, o.versao(), null, o.mapa1(), m1, "M1");
        Instant k1 = Instant.parse(primeira.path("registradoEm").asText());
        assertThat(k1).as("Pré-condição: a primeira fonte é posterior ao corte K0").isAfter(k0);
        String prova1Original = fotografiaProva(primeira.path("id").asLong());
        Recorte soM1 = agregado(o, k1);
        JsonNode httpSoM1 = agregadoHttp(o);

        // Ambas têm início EFETIVO anterior à transferência. O HTTP cria as
        // faixas e seus eventos de proveniência; M1/M2 não são atribuídos por DML.
        Inclusao derivada1 = incluir(o, o.ordem().get(0), 0);
        Inclusao derivada2 = incluir(o, o.ordem().get(1), 1);
        Instant kEntre = Instant.now();
        Config derivada1Antes = configuracaoAlocacao(derivada1.foto().id(), o.dia(), kEntre);
        Config derivada2Antes = configuracaoAlocacao(derivada2.foto().id(), o.dia(), kEntre);

        // Caso 1 nasce com M2 desconhecido. Caso 2 nasce somente após as duas
        // fontes divergentes. A requisição direta usa a tríade de DESTINO.
        Inclusao diretaAntes = divergentes ? null : incluir(o, null, 2);
        Instant kDiretaAntes = divergentes ? null : Instant.now();
        JsonNode httpDiretaAntes = divergentes ? null : agregadoHttp(o);

        JsonNode segunda = incorporar(o, primeira.path("versaoGeradaId").asLong(),
                primeira.path("id").asLong(), o.mapa2(), m2, "M2");
        Instant k2 = Instant.parse(segunda.path("registradoEm").asText());
        assertThat(k2).as("Pré-condição: M2 só é comprovado depois de K entre as fontes").isAfter(kEntre);
        if (kDiretaAntes != null) assertThat(k2).isAfter(kDiretaAntes);
        Inclusao direta = divergentes ? incluir(o, null, 2) : diretaAntes;
        Instant kDireta = divergentes ? Instant.now() : kDiretaAntes;
        JsonNode httpNoAtoDireto = divergentes ? agregadoHttp(o) : httpDiretaAntes;
        Recorte noAtoDireto = agregado(o, kDireta);
        Config diretaNoAto = configuracaoAlocacao(direta.foto().id(), o.dia(), kDireta);
        Config diretaDepois = configuracaoAlocacao(direta.foto().id(), o.dia(), Instant.now());
        Recorte depois = agregado(o, k2);
        JsonNode httpDepois = agregadoHttp(o);
        Config mapa1Depois = configuracaoMapa(o.mapa1(), k2);
        Config mapa2Depois = configuracaoMapa(o.mapa2(), k2);
        Config derivada1Depois = configuracaoAlocacao(derivada1.foto().id(), o.dia(), k2);
        Config derivada2Depois = configuracaoAlocacao(derivada2.foto().id(), o.dia(), k2);

        evidenciar(divergentes, mapa("ordemReplay", o.ordem(), "mapa1", o.mapa1(), "mapa2", o.mapa2(),
                "rotulosMestre", o.mestre(), "rotulosFonte1", m1, "rotulosFonte2", m2,
                "k0", k0, "k1", k1, "kEntre", kEntre, "k2", k2, "kInclusaoDireta", kDireta,
                "primeiraFonte", primeira, "segundaFonte", segunda, "semProvas", semProvas,
                "agregadoSoM1", soM1, "httpSoM1", httpSoM1, "agregadoNoAtoDireto", noAtoDireto,
                "httpNoAtoDireto", httpNoAtoDireto, "fotoDireta", direta.foto(), "diretaNoAto", diretaNoAto,
                "agregadoDepois", depois, "httpDepois", httpDepois, "diretaDepois", diretaDepois,
                "derivada1", derivada1, "derivada2", derivada2,
                "derivada1Depois", derivada1Depois, "derivada2Depois", derivada2Depois));

        // Todas as verificações do achado ficam no mesmo assertAll: um RED no
        // agregado não impede registrar também o rótulo incorreto na alocação.
        assertAll("R01: rótulos por campo, sem empréstimo entre mapas convergentes; divergentes=" + divergentes,
                () -> nulos("Agregado sem qualquer fonte em K0", semProvas.rotulos()),
                () -> nulos("K0 continua sem rótulos após as incorporações", agregado(o, k0).rotulos()),
                () -> nulos("Só M1 não comprova os três rótulos do agregado", soM1.rotulos()),
                () -> nulos("K1 não recebe a fonte posterior de M2", agregado(o, k1).rotulos()),
                () -> nulosHttp("Consulta HTTP quando M2 não tem fonte", httpSoM1),
                () -> nulos("Agregado no ato direto não escolhe M1, M2 ou mestre", noAtoDireto.rotulos()),
                () -> nulosHttp("Consulta HTTP no ato direto", httpNoAtoDireto),
                () -> nulos("Fotografia persistida da inclusão direta", direta.foto().rotulos()),
                () -> nulos("Resolvedor da inclusão direta no corte do ato", diretaNoAto.rotulos()),
                () -> diferentes("M1 não é o mestre atual", m1, o.mestre()),
                () -> diferentes("M2 declarado não é o mestre atual", m2, o.mestre()),
                () -> assertThat(mapa1Depois.rotulos()).isEqualTo(m1),
                () -> assertThat(mapa2Depois.rotulos()).isEqualTo(m2),
                () -> assertThat(configuracaoMapa(o.mapa1(), k1).rotulos()).isEqualTo(m1),
                () -> nulos("M2 permanece sem fonte em K1", configuracaoMapa(o.mapa2(), k1).rotulos()),
                () -> assertThat(derivada1.foto().rotulos()).isEqualTo(m1),
                () -> assertThat(derivada1Antes.rotulos()).isEqualTo(m1),
                () -> assertThat(derivada1Depois.rotulos()).isEqualTo(m1),
                () -> nulos("M2 individual não herda M1 antes da própria fonte", derivada2.foto().rotulos()),
                () -> nulos("Resolvedor individual M2 em K entre fontes", derivada2Antes.rotulos()),
                () -> nulos("Corte antigo de M2 não recebe a fonte posterior",
                        configuracaoAlocacao(derivada2.foto().id(), o.dia(), kEntre).rotulos()),
                () -> assertThat(derivada2Depois.rotulos()).as("Derivada de M2 usa a fonte exata de M2").isEqualTo(m2),
                () -> origemCanonica(o, derivada1, o.ordem().get(0)),
                () -> origemCanonica(o, derivada2, o.ordem().get(1)),
                () -> assertThat(derivada2.foto().composicao()).as("Mapa M2 por tríade não fabrica composição física").isNull(),
                () -> assertThat(direta.eventosTransferencia()).as("Inclusão direta não possui origem individual inventada").isEmpty(),
                () -> assertThat(depois.origens()).containsExactlyInAnyOrder(o.mapa1(), o.mapa2()),
                () -> assertThat(depois.efetivo()).isEqualTo(6),
                () -> assertThat(ordemReplay(o.n().id())).isEqualTo(o.ordem()),
                () -> assertThat(segunda.path("provaAnteriorId").asLong()).isEqualTo(primeira.path("id").asLong()),
                () -> assertThat(segunda.path("versaoAnteriorId").asLong()).isEqualTo(primeira.path("versaoGeradaId").asLong()),
                () -> assertThat(segunda.path("numeroRevisao").asInt()).isEqualTo(2),
                () -> assertThat(vigente(o, k1)).isEqualTo(primeira.path("id").asLong()),
                () -> assertThat(vigente(o, k2)).isEqualTo(segunda.path("id").asLong()),
                () -> assertThat(fotografiaProva(primeira.path("id").asLong())).isEqualTo(prova1Original),
                () -> assertThat(fotografiaMapas(o)).isEqualTo(mapasOriginais),
                () -> assertThat(fotografiaVersoes(o)).isEqualTo(versoesOriginais),
                () -> assertThat(fotografiaAlocacao(derivada1.foto().id())).isEqualTo(derivada1.foto()),
                () -> assertThat(fotografiaAlocacao(derivada2.foto().id())).isEqualTo(derivada2.foto()),
                () -> assertThat(fotografiaAlocacao(direta.foto().id())).isEqualTo(direta.foto()),
                () -> assertThat(eventosTransferencia(derivada1.vinculo())).isEqualTo(derivada1.eventosTransferencia()),
                () -> assertThat(eventosTransferencia(derivada2.vinculo())).isEqualTo(derivada2.eventosTransferencia()),
                () -> controlesIndependentes(o, depois, direta, derivada1Depois, derivada2Depois),
                () -> {
                    if (divergentes) {
                        diferentes("As duas fontes divergem nos três rótulos", m1, m2);
                        nulos("Duas fontes divergentes não comprovam rótulo único", depois.rotulos());
                        nulosHttp("HTTP após duas fontes divergentes", httpDepois);
                        nulos("Inclusão direta não escolhe uma fonte divergente", diretaDepois.rotulos());
                    } else {
                        assertThat(depois.rotulos()).as("Concordância posterior completa o agregado").isEqualTo(m1);
                        iguaisHttp("HTTP após concordância explícita", httpDepois, m1);
                        assertThat(diretaDepois.rotulos()).as("Fonte tardia completa a consulta, mantendo a foto nula").isEqualTo(m1);
                    }
                });
    }

    private Origem preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 6, 3, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        // Mesma consulta e ordenação que iniciam o LinkedHashMap do replay.
        // O primeiro recorte real passa a ser M1; não se presume ordem de IDs.
        List<OrigemIndividual> ordem = ordemReplay(n.id());
        assertThat(ordem).hasSize(2);
        assertThat(ordem.get(0).cargo()).isNotEqualTo(ordem.get(1).cargo());
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data = RelogioSistema.hoje().minusDays(20), dia = RelogioSistema.hoje().minusDays(7);
        var m1 = mapa("composicaoOrigemId", ordem.get(0).composicao(), "lotacaoDestinoId", c.lotacaoB.getId(),
                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId());
        var m2 = mapa("lotacaoOrigemId", ordem.get(1).lotacao(), "jornadaOrigemId", ordem.get(1).jornada(),
                "cargoOrigemId", ordem.get(1).cargo(), "lotacaoDestinoId", c.lotacaoB.getId(),
                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId());
        var req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Convergência sintética para prova dos rótulos de cada origem",
                "referenciaSei", "SEI-SINTETICO-R01-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(m1, m2));
        req.put("tokenPrevia", enviar(c.localA, base(n.id()) + "/transferencias-gestao/previa", req, 200,
                "Fixture: prévia de duas origens reais e um destino").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "Fixture: transferência canônica");
        Origem o = emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c",
                    TransferenciaGestaoContrato.class).setParameter("c", n.id()).getSingleResult();
            assertThat(t.getMapeamentos()).hasSize(2);
            var primeiro = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(ordem.get(0).cargo()))
                    .findFirst().orElseThrow();
            var segundo = t.getMapeamentos().stream().filter(m -> m.getCargoOrigem().getId().equals(ordem.get(1).cargo()))
                    .findFirst().orElseThrow();
            assertThat(primeiro.getComposicaoOrigem().getId()).isEqualTo(ordem.get(0).composicao());
            assertThat(segundo.getComposicaoOrigem()).isNull();
            String grade = primeiro.getJornadaDestinoSemanalFotografia();
            String municipio = primeiro.getLotacaoDestinoMunicipioFotografia();
            assertThat(grade).isNotBlank().isEqualTo(segundo.getJornadaDestinoSemanalFotografia());
            assertThat(municipio).isNotBlank().isEqualTo(segundo.getLotacaoDestinoMunicipioFotografia());
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e",
                    VersaoContratoNegocial.class).setParameter("e", t.getEvento().getId()).getSingleResult();
            var publicacao = motor.fotografiaOriginalConhecida(t.getContrato(), Instant.now()).orElseThrow();
            Map<String, Object> foto = fotografiaNegocial(v);
            Rotulos mestre = new Rotulos(primeiro.getLotacaoDestino().getNome(),
                    primeiro.getJornadaDestino().getDescricao(), primeiro.getCargoDestino().getNome());
            // Somente fixture legada: NOT NULL permite vazio, nunca alterar DDL.
            // Não tocar IDs, origem, grade, município, eventos ou suas fotografias.
            assertThat(em.createQuery("update MapeamentoTransferenciaGestao m set "
                            + "m.lotacaoDestinoNomeFotografia='', m.jornadaDestinoDescricaoFotografia='', "
                            + "m.cargoDestinoNomeFotografia='' where m.transferencia.id=:t")
                    .setParameter("t", t.getId()).executeUpdate()).isEqualTo(2);
            return new Origem(c, n, ordem, t.getId(), primeiro.getId(), segundo.getId(), t.getEvento().getId(),
                    v.getId(), publicacao.getId(), data, dia, grade, municipio, mestre, foto);
        });
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "INCORPORACAO_PROVA_HISTORICA",
                "usuarioBeneficiarioId", c.localB.getId(), "contratoId", n.id(),
                "finalidade", "Conferência de rótulos por mapa", "justificativa", "Fontes sintéticas independentes M1/M2",
                "autorizacaoAdministrativa", "ATO-SINTETICO-R01", "validoAte", Instant.now().plusSeconds(7200)),
                201, "Fixture: permissão documental escopada ao contrato");
        return o;
    }

    private List<OrigemIndividual> ordemReplay(long contrato) {
        return novaLeitura(() -> composicoes.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contrato)
                .stream().map(c -> new OrigemIndividual(c.getId(), c.getLotacao().getId(),
                        c.getJornada().getId(), c.getCargo().getId())).toList());
    }

    private JsonNode incorporar(Origem o, long versao, Long anterior, long mapaId, Rotulos r, String referencia) throws Exception {
        var req = mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", versao, "provaAnteriorId", anterior,
                "dataDocumento", o.data(), "referenciaSei", "SEI-SINTETICO-R01-ROTULOS-" + referencia,
                "motivo", "Fonte sintética declara apenas os três rótulos deste mapa",
                "autorizacaoAdministrativa", "ATO-SINTETICO-R01", "versaoContratoEsperada",
                novaLeitura(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", List.of(), "fotografia", new LinkedHashMap<>(o.fotografia()),
                "composicoes", List.of(mapa("mapeamentoId", mapaId, "lotacaoNome", r.lotacao(),
                        "jornadaDescricao", r.jornada(), "cargoNome", r.cargo(),
                        "municipioComprovado", false, "jornadaComprovada", false)));
        String rota = base(o.n().id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(o.c().localB, rota + "/previa", req, 200,
                "Fonte legítima sintética: prévia por mapa, sem reatribuir grade ou município").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(o.c().localB, rota, req, 201, "Fonte parcial encadeada, preservando a prova anterior");
    }

    private Inclusao incluir(Origem o, OrigemIndividual origem, int ordinal) throws Exception {
        boolean direta = origem == null;
        var req = mapa("contratoId", o.n().id(), "dataInicio", direta ? o.dia() : o.n().inicio().plusDays(1),
                "lotacaoId", direta ? o.c().lotacaoB.getId() : origem.lotacao(),
                "jornadaId", direta ? o.c().jornadaB.getId() : origem.jornada(),
                "cargoId", direta ? o.c().cargoB.getId() : origem.cargo(),
                "pessoa", mapa("nome", "Pessoa sintética R01 " + ordinal,
                        "cpf", cpf(Math.toIntExact(780000000L + 3 * o.n().id() + ordinal))));
        if (!direta) req.put("composicaoId", origem.composicao());
        long vinculo = enviar(o.c().localB, "/api/vinculos-contratuais", req, 201,
                direta ? "Inclusão direta no agregado com grade comprovada; rótulos podem permanecer desconhecidos"
                        : "Inclusão retroativa antes da transferência; o replay documenta a origem individual").path("id").asLong();
        long alocacao = novaLeitura(() -> em.createQuery("select a.id from AlocacaoVinculo a "
                        + "where a.vinculo.id=:v and a.lotacao.id=:l", Long.class)
                .setParameter("v", vinculo).setParameter("l", o.c().lotacaoB.getId()).getSingleResult());
        return new Inclusao(vinculo, fotografiaAlocacao(alocacao), eventosTransferencia(vinculo));
    }

    private Recorte agregado(Origem o, Instant k) {
        return novaLeitura(() -> {
            var itens = composicaoTemporal.projetarEntidades(em.find(Contrato.class, o.n().id()), o.dia(), k).stream()
                    .filter(p -> p.lotacao().getId().equals(o.c().lotacaoB.getId())
                            && p.jornada().getId().equals(o.c().jornadaB.getId()) && p.cargo().getId().equals(o.c().cargoB.getId())).toList();
            assertThat(itens).as("Fixture: exatamente um recorte convergente").hasSize(1);
            var p = itens.get(0);
            return new Recorte(new Rotulos(p.lotacaoNome(), p.jornadaDescricao(), p.cargoNome()),
                    p.efetivoPrevisto(), p.jornadaSemanal(), p.lotacaoMunicipio(), p.mapeamentosOrigemIds());
        });
    }

    private JsonNode agregadoHttp(Origem o) throws Exception {
        JsonNode resposta = consultar(o.c().localB, base(o.n().id()) + "/composicoes?data=" + o.dia(),
                "HIS001/CAT004: consulta HTTP do recorte de destino");
        List<JsonNode> itens = new ArrayList<>();
        for (JsonNode p : resposta) {
            if (p.path("lotacaoId").asLong() == o.c().lotacaoB.getId()
                    && p.path("jornadaId").asLong() == o.c().jornadaB.getId()
                    && p.path("cargoId").asLong() == o.c().cargoB.getId()) itens.add(p);
        }
        assertThat(itens).as("Fixture: uma composição de destino na resposta HTTP").hasSize(1);
        return itens.get(0);
    }

    private Config configuracaoMapa(long id, Instant k) {
        return novaLeitura(() -> config(consultaProvas.configuracao(em.find(MapeamentoTransferenciaGestao.class, id), k)));
    }

    private Config configuracaoAlocacao(long id, LocalDate dia, Instant k) {
        return novaLeitura(() -> config(resolvedor.resolverConfiguracao(em.find(AlocacaoVinculo.class, id), dia, k)));
    }

    private Config config(ConsultaProvaHistoricaContratoServico.Configuracao c) {
        return new Config(new Rotulos(c.lotacaoNome(), c.jornadaDescricao(), c.cargoNome()),
                c.jornadaSemanal(), c.lotacaoMunicipio());
    }

    private FotoAlocacao fotografiaAlocacao(long id) {
        return novaLeitura(() -> {
            var a = em.find(AlocacaoVinculo.class, id);
            return new FotoAlocacao(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                    new Rotulos(a.getLotacaoNomeFotografia(), a.getJornadaDescricaoFotografia(), a.getCargoNomeFotografia()),
                    a.getJornadaSemanalFotografia(), a.getLotacaoMunicipioFotografia(), a.getMunicipioAusenciaComprovada(),
                    a.getDataInicio(), a.getDataFim(), a.getCriadoEm(), a.getVersion());
        });
    }

    private List<EventoFoto> eventosTransferencia(long vinculo) {
        return novaLeitura(() -> em.createQuery("select e from EventoAlocacaoVinculo e "
                        + "where e.vinculo.id=:v and e.tipoEvento=:tipo order by e.id", EventoAlocacaoVinculo.class)
                .setParameter("v", vinculo).setParameter("tipo", TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO)
                .getResultList().stream().map(e -> new EventoFoto(e.getId(), e.getAlocacaoNova().getId(),
                        e.getTransferenciaGestaoId(), e.getAlocacaoAnteriorId(), e.getComposicaoAnteriorId(),
                        e.getLotacaoAnteriorId(), e.getJornadaAnteriorId(), e.getCargoAnteriorId(),
                        e.getDataEfetiva(), e.getRegistradoEm(), e.getHashAnterior(), e.getHashEvento())).toList());
    }

    private void origemCanonica(Origem o, Inclusao inclusao, OrigemIndividual esperada) {
        assertThat(inclusao.eventosTransferencia()).singleElement().satisfies(e -> assertAll("Origem gerada pelo replay",
                () -> assertThat(e.transferencia()).isEqualTo(o.transferencia()),
                () -> assertThat(e.alocacao()).isEqualTo(inclusao.foto().id()),
                () -> assertThat(e.anterior()).isNotNull().isNotEqualTo(e.alocacao()),
                () -> assertThat(e.composicaoAnterior()).isEqualTo(esperada.composicao()),
                () -> assertThat(e.lotacaoAnterior()).isEqualTo(esperada.lotacao()),
                () -> assertThat(e.jornadaAnterior()).isEqualTo(esperada.jornada()),
                () -> assertThat(e.cargoAnterior()).isEqualTo(esperada.cargo()),
                () -> assertThat(e.hashEvento()).hasSize(64)));
    }

    private void controlesIndependentes(Origem o, Recorte agregado, Inclusao direta, Config d1, Config d2) {
        assertAll("Grade concordante e município original independem dos rótulos",
                () -> assertThat(agregado.grade()).isEqualTo(o.grade()),
                () -> assertThat(direta.foto().grade()).isEqualTo(o.grade()),
                () -> assertThat(d1.grade()).isEqualTo(o.grade()),
                () -> assertThat(d2.grade()).isEqualTo(o.grade()),
                () -> assertThat(agregado.municipio()).isEqualTo(o.municipio()),
                () -> assertThat(direta.foto().municipio()).isEqualTo(o.municipio()));
    }

    private void nulos(String contexto, Rotulos r) {
        assertAll(contexto,
                () -> assertThat(r.lotacao()).as(contexto + ": Lotação").isNull(),
                () -> assertThat(r.jornada()).as(contexto + ": Jornada").isNull(),
                () -> assertThat(r.cargo()).as(contexto + ": Cargo").isNull());
    }

    private void nulosHttp(String contexto, JsonNode r) {
        assertAll(contexto,
                () -> assertThat(r.path("lotacaoNome").isNull()).as(contexto + ": Lotação").isTrue(),
                () -> assertThat(r.path("jornadaDescricao").isNull()).as(contexto + ": Jornada").isTrue(),
                () -> assertThat(r.path("cargoNome").isNull()).as(contexto + ": Cargo").isTrue());
        // IND-08: um agregado divergente/desconhecido não comprova ausência.
        for(String campo:List.of("lotacaoNome","jornadaDescricao","cargoNome")) {
            var prova=r.path("provaCampos").path(campo);
            assertThat(prova.path("estado").asText()).as(contexto+": evidência de "+campo).isEqualTo("NAO_COMPROVADO");
            assertThat(prova.path("visibilidade").asText()).isEqualTo("DISPONIVEL");
            assertThat(prova.path("origens")).isEmpty();
        }
    }

    private void iguaisHttp(String contexto, JsonNode r, Rotulos esperado) {
        assertAll(contexto,
                () -> assertThat(r.path("lotacaoNome").asText()).isEqualTo(esperado.lotacao()),
                () -> assertThat(r.path("jornadaDescricao").asText()).isEqualTo(esperado.jornada()),
                () -> assertThat(r.path("cargoNome").asText()).isEqualTo(esperado.cargo()));
        for(String campo:List.of("lotacaoNome","jornadaDescricao","cargoNome")) {
            var prova=r.path("provaCampos").path(campo);
            assertThat(prova.path("estado").asText()).isEqualTo("VALOR_COMPROVADO");
            assertThat(prova.path("origens")).as("Os dois mapas concordantes precisam conservar sua proveniência").hasSize(2);
            for(JsonNode origem:prova.path("origens"))assertThat(origem.path("mapeamentoId").asLong()).isPositive();
        }
    }

    private void diferentes(String contexto, Rotulos a, Rotulos b) {
        assertAll(contexto,
                () -> assertThat(a.lotacao()).isNotBlank().isNotEqualTo(b.lotacao()),
                () -> assertThat(a.jornada()).isNotBlank().isNotEqualTo(b.jornada()),
                () -> assertThat(a.cargo()).isNotBlank().isNotEqualTo(b.cargo()));
    }

    private String fotografiaMapas(Origem o) {
        return novaLeitura(() -> mapper.valueToTree(List.of(o.mapa1(), o.mapa2()).stream().map(id -> {
            var m = em.find(MapeamentoTransferenciaGestao.class, id);
            return mapa("id", m.getId(), "transferencia", m.getTransferencia().getId(),
                    "composicaoOrigem", m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                    "origem", List.of(m.getLotacaoOrigem().getId(), m.getJornadaOrigem().getId(), m.getCargoOrigem().getId()),
                    "destino", List.of(m.getLotacaoDestino().getId(), m.getJornadaDestino().getId(), m.getCargoDestino().getId()),
                    "lotacao", m.getLotacaoDestinoNomeFotografia(), "jornada", m.getJornadaDestinoDescricaoFotografia(),
                    "cargo", m.getCargoDestinoNomeFotografia(), "grade", m.getJornadaDestinoSemanalFotografia(),
                    "municipio", m.getLotacaoDestinoMunicipioFotografia());
        }).toList()).toString());
    }

    private String fotografiaVersoes(Origem o) {
        return novaLeitura(() -> mapper.valueToTree(List.of(o.publicacao(), o.versao()).stream()
                .map(id -> motor.paraVersaoDTO(em.find(VersaoContratoNegocial.class, id))).toList()).toString());
    }

    private String fotografiaProva(long id) {
        return novaLeitura(() -> {
            var p = em.find(ProvaHistoricaContrato.class, id);
            return mapper.valueToTree(mapa("id", p.getId(), "evento", p.getEventoOrigem().getId(),
                    "anterior", p.getProvaAnterior() == null ? null : p.getProvaAnterior().getId(),
                    "versaoAnterior", p.getVersaoAnterior().getId(), "versaoGerada", p.getVersaoGerada().getId(),
                    "numeroRevisao", p.getNumeroRevisao(), "registradoEm", p.getRegistradoEm(),
                    "hashConteudo", p.getHashConteudo())).toString();
        });
    }

    private long vigente(Origem o, Instant k) {
        return novaLeitura(() -> consultaProvas.vigente(o.evento(), k).orElseThrow().getId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografiaNegocial(VersaoContratoNegocial v) {
        Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
            foto.remove(campo);
        return foto;
    }

    private <T> T novaLeitura(Supplier<T> leitura) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setReadOnly(true);
        return tx.execute(status -> { em.clear(); return leitura.get(); });
    }

    private void evidenciar(boolean divergentes, Object dados) throws Exception {
        Path path = Path.of("target/auditoria-http/r01-rotulos-convergentes/"
                + (divergentes ? "fontes-divergentes" : "mapa-m2-desconhecido") + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json(mapa("achado", "R01", "transporte", "MockMvc; não TCP",
                "fontes", "Sintéticas, declaradas e incorporadas por HTTP", "observacoes", dados)));
    }

    private record Rotulos(String lotacao, String jornada, String cargo) {}
    private record Config(Rotulos rotulos, String grade, String municipio) {}
    private record Recorte(Rotulos rotulos, int efetivo, String grade, String municipio, Set<Long> origens) {}
    private record OrigemIndividual(long composicao, long lotacao, long jornada, long cargo) {}
    private record FotoAlocacao(long id, Long composicao, Rotulos rotulos, String grade, String municipio,
            Boolean ausenciaMunicipal, LocalDate inicio, LocalDate fim, Instant criadoEm, long versao) {}
    private record EventoFoto(Long id, Long alocacao, Long transferencia, Long anterior, Long composicaoAnterior,
            Long lotacaoAnterior, Long jornadaAnterior, Long cargoAnterior, LocalDate data,
            Instant registradoEm, String hashAnterior, String hashEvento) {}
    private record Inclusao(long vinculo, FotoAlocacao foto, List<EventoFoto> eventosTransferencia) {}
    private record Origem(Cenario c, NovoContrato n, List<OrigemIndividual> ordem, long transferencia, long mapa1,
            long mapa2, long evento, long versao, long publicacao, LocalDate data, LocalDate dia,
            String grade, String municipio, Rotulos mestre, Map<String, Object> fotografia) {}
}
