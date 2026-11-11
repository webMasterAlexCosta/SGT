package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** AUD-02: motivo de catálogo não cria o ato contratual que justifica o término. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud02DesligamentoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private record EstadoVinculo(LocalDate fim, Long motivoId, CategoriaTerminoVinculo categoria,
                                  Instant registradoEm, long versao, long eventos, LocalDate fimAlocacao) {}

    @ParameterizedTest
    @EnumSource(value = CategoriaTerminoVinculo.class, names = {
            "ANULACAO_CONTRATO", "ENCERRAMENTO_ANTECIPADO_CONTRATO", "FIM_VIGENCIA_CONTRATO"})
    void desligamentoIndividualRejeitaCausaContratualSemAtoCorrespondente(CategoriaTerminoVinculo categoria) throws Exception {
        Cenario c = criarCenarioPersistido();
        long motivo = motivo(c, categoria);
        fonteMotivo(c, motivo, categoria, DATA, DATA, "AUD02-CAUSA-SEM-ATO");
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/desligar", pedido(DATA, motivo), 409,
                        "AUD-02 causa contratual não pode ser criada por desligamento individual"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @ParameterizedTest
    @EnumSource(value = CategoriaTerminoVinculo.class, names = {
            "ANULACAO_CONTRATO", "ENCERRAMENTO_ANTECIPADO_CONTRATO", "FIM_VIGENCIA_CONTRATO"})
    void previaDeRetificacaoRejeitaCausaContratualSemAtoCorrespondente(CategoriaTerminoVinculo categoria) throws Exception {
        Cenario c = criarCenarioPersistido();
        long individual = motivo(c, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        long contratual = motivo(c, categoria);
        fonteMotivo(c, individual, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA.minusDays(2), DATA.minusDays(2), "AUD02-INDIVIDUAL-ANTES-DA-PREVIA");
        fonteMotivo(c, contratual, categoria, DATA, DATA, "AUD02-CAUSA-CONTRATUAL-SEM-ATO");
        enviar(c.localA, vinculo(c) + "/desligar", pedido(DATA.minusDays(2), individual), 200,
                "DES-004 desligamento individual documentado antes da tentativa de retificação");
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", pedido(DATA, contratual), 409,
                        "AUD-02 prévia não pode autorizar causa contratual sem ato"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @ParameterizedTest
    @EnumSource(value = CategoriaTerminoVinculo.class, names = {"DESLIGAMENTO_INDIVIDUAL", "OUTRO"})
    void desligamentoIndividualEOuOutraCausaCadastradaContinuamValidos(CategoriaTerminoVinculo categoria) throws Exception {
        Cenario c = criarCenarioPersistido();
        long idMotivo = motivo(c, categoria);
        fonteMotivo(c, idMotivo, categoria, DATA, DATA, "AUD02-CAUSA-INDIVIDUAL-VALIDA");
        EstadoVinculo antes = estado(c);
        assertThat(antes.eventos()).isEqualTo(1); // origem explícita, ainda sem término
        enviar(c.localA, vinculo(c) + "/desligar", pedido(DATA, idMotivo), 200,
                "CAT-009 motivos individuais disponíveis não dependem de ato contratual");
        EstadoVinculo gravado = estado(c);
        assertThat(gravado.fim()).isEqualTo(DATA); assertThat(gravado.motivoId()).isEqualTo(idMotivo);
        assertThat(gravado.categoria()).isEqualTo(categoria); assertThat(gravado.registradoEm()).isNotNull();
        assertThat(gravado.eventos()).isEqualTo(antes.eventos() + 1); assertThat(gravado.fimAlocacao()).isEqualTo(DATA);
        conferirPrimeiroMotivo(c, categoria);
    }

    @ParameterizedTest
    @EnumSource(value = CategoriaTerminoVinculo.class, names = {
            "ANULACAO_CONTRATO", "ENCERRAMENTO_ANTECIPADO_CONTRATO", "FIM_VIGENCIA_CONTRATO"})
    void causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada(CategoriaTerminoVinculo categoria) throws Exception {
        Cenario c = criarCenarioPersistido();
        long idMotivo = motivo(c, categoria);
        fonteMotivo(c, idMotivo, categoria, DATA, DATA, "AUD02-CAUSA-COM-ATO-E-DATA");
        if (categoria == CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO) {
            definirFimOriginalDaFixture(c, DATA);
            enviar(c.localA, "/api/vinculos-contratuais/contratos/" + c.contratoA.getId() + "/tratar-fim-natural",
                    null, 200, "VIN-009 fim natural é tratado na data final publicada");
        } else {
            efetivarCiclo(c, operacao(categoria), DATA);
        }
        EstadoVinculo original = estado(c);
        assertThat(original.fim()).isEqualTo(DATA); assertThat(original.categoria()).isEqualTo(categoria);
        retificar(c, pedido(DATA, idMotivo));
        EstadoVinculo corrigido = estado(c);
        assertThat(corrigido.fim()).isEqualTo(DATA); assertThat(corrigido.motivoId()).isEqualTo(idMotivo);
        assertThat(corrigido.categoria()).isEqualTo(categoria);
        assertThat(corrigido.eventos()).isEqualTo(original.eventos() + 1);
        conferirPrimeiroMotivo(c, categoria);
    }

    @ParameterizedTest
    @EnumSource(value = CategoriaTerminoVinculo.class, names = {"ANULACAO_CONTRATO", "ENCERRAMENTO_ANTECIPADO_CONTRATO"})
    void existenciaDoAtoNaoAutorizaUsarSuaCausaEmOutraData(CategoriaTerminoVinculo categoria) throws Exception {
        Cenario c = criarCenarioPersistido();
        long idMotivo = motivo(c, categoria);
        fonteMotivo(c, idMotivo, categoria, DATA.minusDays(1), DATA, "AUD02-ATO-NAO-COBRE-OUTRA-DATA");
        efetivarCiclo(c, operacao(categoria), DATA);
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", pedido(DATA.minusDays(1), idMotivo),
                        409, "CON-024/VIN-015 causa contratual respeita data efetiva, não mera existência de ato"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @Test
    void fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural() throws Exception {
        Cenario c = criarCenarioPersistido();
        long rescisao = motivo(c, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
        long natural = motivo(c, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        fonteMotivo(c, rescisao, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO,
                DATA, DATA, "AUD02-RESCISAO-DOCUMENTADA");
        fonteMotivo(c, natural, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                DATA, DATA, "AUD02-CATALOGO-NATURAL-SEM-FATO-NATURAL");
        efetivarCiclo(c, "encerrar-antecipadamente", DATA);
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", pedido(DATA, natural), 409,
                        "CAT-009 rescisão que mudou a data atual do contrato não equivale a fim natural"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @Test
    void anulacaoFuturaNaoJustificaCausaAnulatoriaAntesDoAlcance() throws Exception {
        Cenario c = criarCenarioPersistido();
        long anulacao = motivo(c, CategoriaTerminoVinculo.ANULACAO_CONTRATO);
        LocalDate alcance = com.example.siggtrat.utilitario.RelogioSistema.hoje().plusDays(10);
        efetivarCiclo(c, "anular", alcance);
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", pedido(alcance.minusDays(1), anulacao),
                        409, "CON-024 ato futuro não retroage sua causa à faixa válida anterior"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @Test
    void confirmacaoRevalidaAtoAnulatorioRetiradoDepoisDaPrevia() throws Exception {
        Cenario c = criarCenarioPersistido();
        long anulacao = motivo(c, CategoriaTerminoVinculo.ANULACAO_CONTRATO);
        fonteMotivo(c, anulacao, CategoriaTerminoVinculo.ANULACAO_CONTRATO,
                DATA, DATA, "AUD02-ATO-ANULATORIO-SERA-RETIRADO");
        efetivarCiclo(c, "anular", DATA);
        Map<String, Object> req = pedido(DATA, anulacao);
        confirmarPreviaDesligamento(c, req);
        long eventoAnulacao = emTransacao(() -> em.createQuery("select e.id from EventoCicloVidaContrato e "
                        + "where e.contrato.id=:c and e.tipoEvento=:tipo", Long.class)
                .setParameter("c", c.contratoA.getId()).setParameter("tipo", TipoEventoCicloVidaContrato.ANULACAO).getSingleResult());
        var correcao = mapa("dataEfetiva", DATA, "dataAto", DATA.plusDays(1),
                "motivo", "Ato anulatório registrado incorretamente", "referenciaSei", "SEI-AUD02-CORRECAO",
                "eventoPrecedenteId", eventoAnulacao, "relacaoPrecedencia", "CANCELA");
        confirmarCiclo(c, "corrigir-anulacao", correcao);
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/retificar-desligamento", req, 409,
                        "AUD-004/AUD-02 confirmação deve revalidar causa após correção do ato"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @Test
    void prorrogacaoTardiaPreservaFimNaturalComprovadoEAdmiteRetificacaoExplicitaDaNovaFaixa() throws Exception {
        Cenario c = criarCenarioPersistido();
        long natural = motivo(c, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        fonteMotivo(c, natural, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                DATA, DATA, "AUD02-FIM-NATURAL-ANTES-DA-PRORROGACAO");
        definirFimOriginalDaFixture(c, DATA);
        enviar(c.localA, "/api/vinculos-contratuais/contratos/" + c.contratoA.getId() + "/tratar-fim-natural", null,
                200, "VIN-012 término natural verdadeiro antes da prorrogação tardia");
        EstadoVinculo fimOriginal = estado(c);
        prorrogar(c, DATA.minusDays(1), FIM);
        assertThat(estado(c)).isEqualTo(fimOriginal);
        retificar(c, pedido(DATA, natural));
        assertThat(estado(c).fim()).isEqualTo(DATA);
        conferirPrimeiroMotivo(c, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        retificar(c, pedido(FIM, natural));
        assertThat(estado(c).fim()).isEqualTo(FIM);
        assertThat(estado(c).categoria()).isEqualTo(CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        conferirPrimeiroMotivo(c, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
    }

    @Test
    void desligamentoIndividualAnteriorARescisaoConservaSuaCausaEAdmiteRetificacaoIndividual() throws Exception {
        Cenario c = criarCenarioPersistido();
        long individual = motivo(c, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        long rescisao = motivo(c, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
        fonteMotivo(c, individual, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA.minusDays(10), DATA.minusDays(9), "AUD02-INDIVIDUAL-ANTERIOR-PRESERVADO");
        fonteMotivo(c, rescisao, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO,
                DATA, DATA, "AUD02-RESCISAO-POSTERIOR");
        enviar(c.localA, vinculo(c) + "/desligar", pedido(DATA.minusDays(10), individual), 200,
                "VIN-015 desligamento real anterior à rescisão");
        EstadoVinculo anterior = estado(c);
        efetivarCiclo(c, "encerrar-antecipadamente", DATA);
        assertThat(estado(c)).isEqualTo(anterior);
        retificar(c, pedido(DATA.minusDays(9), individual));
        assertThat(estado(c).fim()).isEqualTo(DATA.minusDays(9));
        assertThat(estado(c).categoria()).isEqualTo(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        conferirPrimeiroMotivo(c, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
    }

    @Test
    void prorrogacaoNaoConverteDesligamentoIndividualAnteriorEmFimNatural() throws Exception {
        Cenario c = criarCenarioPersistido();
        long individual = motivo(c, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        long natural = motivo(c, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        fonteMotivo(c, individual, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA.minusDays(5), DATA.minusDays(5), "AUD02-INDIVIDUAL-NAO-E-NATURAL");
        fonteMotivo(c, natural, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                DATA.minusDays(5), DATA, "AUD02-CATALOGO-NATURAL-NAO-CRIA-ATO");
        definirFimOriginalDaFixture(c, DATA);
        enviar(c.localA, vinculo(c) + "/desligar", pedido(DATA.minusDays(5), individual), 200,
                "Fixture AUD02: fato individual anterior ao fim natural antigo");
        prorrogar(c, DATA.minusDays(1), FIM);
        EstadoVinculo antes = estado(c);
        assertAll(
                () -> enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", pedido(DATA.minusDays(5), natural), 409,
                        "AUD-02 exceção de preservação histórica não inventa causa natural para término individual"),
                () -> assertThat(estado(c)).isEqualTo(antes));
    }

    @Test
    void reabrirFimNaturalAposProrrogacaoRecusaSobreposicaoComVinculoPosteriorDaMesmaPessoa() throws Exception {
        Cenario c=criarCenarioPersistido();
        long natural=motivo(c,CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        fonteMotivo(c,natural,CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,DATA,DATA,"B07-REABERTURA-CONFLITO");
        definirFimOriginalDaFixture(c,DATA);
        enviar(c.localA,"/api/vinculos-contratuais/contratos/"+c.contratoA.getId()+"/tratar-fim-natural",null,200,
                "VIN012 vínculo original encerra no fim natural verdadeiro");
        EstadoVinculo original=estado(c);
        prorrogar(c,DATA.minusDays(1),FIM);
        assertThat(estado(c)).isEqualTo(original);
        JsonNode posterior=enviar(c.localA,"/api/vinculos-contratuais",mapa("pessoaId",c.pessoaA.getId(),
                "contratoId",c.contratoA.getId(),"composicaoId",c.composicaoA.getId(),"dataInicio",DATA.plusDays(1)),201,
                "VIN012 a Pessoa original é reaproveitada em novo vínculo válido, sem sobreposição na origem");
        long posteriorId=posterior.path("id").asLong();
        assertThat(posterior.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId());
        assertThat(posteriorId).isNotEqualTo(c.vinculoA.getId());
        var req=pedido(FIM,natural);
        JsonNode previa=enviar(c.localA,vinculo(c)+"/previas/retificacao-desligamento",req,200,
                "VIN012 prévia mostra conflito da faixa que seria restaurada");
        assertThat(java.util.stream.StreamSupport.stream(previa.path("vinculosConflitantes").spliterator(),false)
                .map(JsonNode::asLong).toList()).containsExactly(posteriorId);
        req.put("confirmarImpacto",true);req.put("tokenPrevia",previa.path("token").asText());
        JsonNode trilhaAntes=consultar(c.localA,vinculo(c)+"/eventos","VIN012 fotografia da trilha antes da recusa");
        JsonNode erro=enviar(c.localA,vinculo(c)+"/retificar-desligamento",req,409,
                "VIN012 confirmação válida não autoriza reabrir sobre outro vínculo da mesma Pessoa");
        assertThat(erro.path("message").asText()).containsIgnoringCase("sobreposição");
        assertThat(estado(c)).as("Recusa preserva vínculo original, versão, motivo e faixa em transação nova").isEqualTo(original);
        assertThat(consultar(c.localA,vinculo(c)+"/eventos","VIN012 trilha após rollback")).isEqualTo(trilhaAntes);
        emTransacao(() -> {
            VinculoContratual p=em.find(VinculoContratual.class,posteriorId);
            assertThat(p.getPessoa().getId()).isEqualTo(c.pessoaA.getId());
            assertThat(p.getDataInicio()).isEqualTo(DATA.plusDays(1));assertThat(p.getDataFim()).isNull();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:p",Long.class)
                    .setParameter("p",c.pessoaA.getId()).getSingleResult()).isEqualTo(2);
            return null;
        });
    }

    private void fonteMotivo(Cenario c, long motivoId, CategoriaTerminoVinculo categoria,
                             LocalDate inicio, LocalDate fim, String identificacao) {
        emTransacao(() -> {
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivoId, c.gestaoA.getId(), c.localA,
                    "Motivo AUD02 " + categoria, categoria, inicio, fim, Instant.now(),
                    "SEI-SINTETICO-" + identificacao);
            return null;
        });
    }

    private String vinculo(Cenario c) { return "/api/vinculos-contratuais/" + c.vinculoA.getId(); }

    @Override
    protected Cenario criarCenarioPersistido() {
        Cenario c = super.criarCenarioPersistido();
        // A base cria apenas a projeção do vínculo por JPA. Estes cenários de
        // término pressupõem uma admissão documentada desde INICIO, portanto
        // sua fonte nominal é declarada aqui, antes de qualquer ato testado.
        // Fixtures de ausência histórica das outras classes permanecem vazias.
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", v.getId()).getSingleResult()).isZero();
            EventoVinculoContratual origem = new EventoVinculoContratual();
            origem.setVinculo(v); origem.setTipoEvento(TipoEventoVinculoContratual.CRIACAO);
            origem.setRelacaoPrecedencia(TipoRelacaoEventoVinculo.ORIGEM);
            origem.setDataEfetiva(INICIO); origem.setRegistradoEm(Instant.parse("2026-01-01T00:00:00Z"));
            origem.setInicioNovo(INICIO); origem.setMatriculaNova(v.getMatricula());
            origem.setPessoaNomeFotografia(v.getPessoa().getNome());
            origem.setPessoaCpfFotografia(v.getPessoa().getCpf());
            origem.setContratoNumeroFotografia(v.getContrato().getNumeroContrato());
            origem.setContratoAnoFotografia(v.getContrato().getAnoContrato());
            origem.setGestaoLocalIdFotografia(c.gestaoA.getId()); origem.setGestaoLocalNomeFotografia(c.gestaoA.getNome());
            origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername());
            origem.setJustificativa("Fonte sintética explícita da admissão e identidade no início do cenário AUD02");
            origem.setEvidencia("SEI-AUD02-ORIGEM"); origem.setAutorizacao("ATO-AUD02-ORIGEM");
            origem.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal("FIXTURE-AUD02-CRIACAO|"
                    + v.getId() + "|" + INICIO + "|" + v.getPessoa().getNome() + "|" + v.getPessoa().getCpf()));
            em.persist(origem); em.flush();
            return null;
        });
        return c;
    }

    private String operacao(CategoriaTerminoVinculo categoria) {
        return categoria == CategoriaTerminoVinculo.ANULACAO_CONTRATO ? "anular" : "encerrar-antecipadamente";
    }

    private Map<String, Object> pedido(LocalDate fim, long motivo) {
        return mapa("dataFim", fim, "motivoDesligamentoId", motivo,
                "justificativa", "Retificação ou desligamento baseado no documento original",
                "evidencia", "SEI-AUD02", "autorizacao", "ATO-AUD02");
    }

    private long motivo(Cenario c, CategoriaTerminoVinculo categoria) {
        return emTransacao(() -> {
            MotivoDesligamento m = new MotivoDesligamento();
            m.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            m.setDescricao("Motivo AUD02 " + categoria); m.setCategoria(categoria); em.persist(m); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
            origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do catálogo de motivos desde o início do cenário");
            origem.setRegistradoEm(Instant.parse("2025-01-01T00:00:00Z")); em.persist(origem);
            return m.getId();
        });
    }

    private void definirFimOriginalDaFixture(Cenario c, LocalDate fim) {
        // Preparação documental sintética completa: publicação e alocação concordam;
        // nenhuma inferência é feita a partir de uma alteração posterior do cadastro.
        emTransacao(() -> {
            em.find(Contrato.class, c.contratoA.getId()).setDataFim(fim);
            // A versão é imutável (updatable=false): seu setter apenas alterava
            // o objeto em memória e não preparava a prova no PostgreSQL. Este DML é
            // exclusivo da fixture sintética, antes de qualquer ato sob teste.
            assertThat(em.createQuery("update VersaoContratoNegocial v set v.dataFim=:fim where v.contrato.id=:c")
                    .setParameter("fim", fim).setParameter("c", c.contratoA.getId()).executeUpdate()).isEqualTo(1);
            em.find(ComposicaoContratual.class, c.composicaoA.getId()).setDataFimVigencia(fim);
            em.find(AlocacaoVinculo.class, c.alocacaoA.getId()).setDataFim(fim);
            return null;
        });
        emTransacao(() -> {
            assertThat(em.createQuery("select v.dataFim from VersaoContratoNegocial v where v.contrato.id=:c", LocalDate.class)
                    .setParameter("c", c.contratoA.getId()).getResultList()).containsExactly(fim);
            return null;
        });
    }

    private EstadoVinculo estado(Cenario c) {
        return emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, c.vinculoA.getId());
            long eventos = em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", v.getId()).getSingleResult();
            return new EstadoVinculo(v.getDataFim(), v.getMotivoDesligamento() == null ? null : v.getMotivoDesligamento().getId(),
                    v.getMotivoDesligamentoCategoriaFotografia(), v.getDesligamentoRegistradoEm(), v.getVersion(), eventos,
                    em.find(AlocacaoVinculo.class, c.alocacaoA.getId()).getDataFim());
        });
    }

    private void efetivarCiclo(Cenario c, String operacao, LocalDate data) throws Exception {
        confirmarCiclo(c, operacao, mapa("dataEfetiva", data, "dataAto", DATA,
                "motivo", "Ato contratual específico e comprovado", "referenciaSei", "SEI-AUD02-CONTRATO"));
    }

    private void confirmarCiclo(Cenario c, String operacao, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/" + operacao, req, 200,
                "Prévia do ato contratual AUD02");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/" + operacao, req, 200, "Ato contratual AUD02 commitado");
    }

    private void confirmarPreviaDesligamento(Cenario c, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, vinculo(c) + "/previas/retificacao-desligamento", req, 200,
                "DES-005 prévia legítima da retificação do término");
        req.put("tokenPrevia", previa.path("token").asText()); req.put("confirmarImpacto", true);
    }

    private void retificar(Cenario c, Map<String, Object> req) throws Exception {
        confirmarPreviaDesligamento(c, req);
        enviar(c.localA, vinculo(c) + "/retificar-desligamento", req, 200,
                "DES-005 retificação legítima preserva a cadeia causal");
    }

    private void prorrogar(Cenario c, LocalDate data, LocalDate fim) throws Exception {
        var req = aditamento("PRORROGACAO-AUD02", data); req.put("novaDataFim", fim);
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/aditamentos/previa", req, 200,
                "VIN-012 prévia de prorrogação tardia explícita");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localA, base(c.contratoA.getId()) + "/aditamentos", req, 201,
                "VIN-012 prorrogação altera vigência sem reabrir vínculo automaticamente");
    }

    private void conferirPrimeiroMotivo(Cenario c, CategoriaTerminoVinculo categoria) {
        emTransacao(() -> {
            var eventos = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id",
                            EventoVinculoContratual.class).setParameter("v", c.vinculoA.getId()).getResultList();
            assertThat(eventos).hasSizeGreaterThanOrEqualTo(2);
            assertThat(eventos.get(0).getTipoEvento()).isEqualTo(TipoEventoVinculoContratual.CRIACAO);
            assertThat(eventos.get(0).getPessoaNomeFotografia()).isEqualTo(c.pessoaA.getNome());
            assertThat(eventos.get(0).getPessoaCpfFotografia()).isEqualTo(c.pessoaA.getCpf());
            assertThat(eventos.get(0).getMotivoDesligamentoCategoriaFotografia()).isNull();
            assertThat(eventos.get(1).getMotivoDesligamentoCategoriaFotografia()).isEqualTo(categoria);
            return null;
        });
    }
}
