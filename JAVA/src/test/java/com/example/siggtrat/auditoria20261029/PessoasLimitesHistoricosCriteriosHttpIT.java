package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasLimitesHistoricosCriteriosHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";

    @Test
    void fimNaturalPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior() throws Exception {
        Preparado f = preparar(false);
        LocalDate hoje = RelogioSistema.hoje();
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.individual(), f.c().gestaoA.getId(), f.c().localA,
                    "Causa individual documentada anterior ao fim natural", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    hoje.minusDays(10), hoje.minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-INDIVIDUAL-NATURAL");
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.natural(), f.c().gestaoA.getId(), f.c().localA,
                    "Fim natural documentado da vigência publicada", CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                    f.contrato().fim(), f.contrato().fim(), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-FIM-NATURAL");
            return null;
        });

        long anterior = cadastrar(f, 0, hoje.minusDays(20));
        long aberto = cadastrar(f, 1, hoje.minusDays(20));
        terminar(f, anterior, hoje.minusDays(10));
        Linha antes = linha(anterior);

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.contrato().fim().plusDays(1));
            JsonNode tratados = enviar(f.c().localA, V + "/contratos/" + f.contrato().id() + "/tratar-fim-natural",
                    mapa(), 200, "VIN009 o tratamento posterior encerra somente o vínculo ainda aberto");
            assertThat(tratados).hasSize(2);
        }
        assertThat(linha(anterior)).as("Desligamento individual, motivo, faixas e hashes permanecem intactos")
                .isEqualTo(antes);
        conferirFim(aberto, f.contrato().fim(), f.natural(), CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        JsonNode eventos = consultar(f.c().localA, V + "/" + aberto + "/eventos", "VIN010 causa própria do fim natural");
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("FIM_NATURAL_CONTRATO");
        assertThat(eventos.get(1).path("dataEfetiva").asText()).isEqualTo(f.contrato().fim().toString());
    }

    @Test
    void rescisaoPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior() throws Exception {
        Preparado f = preparar(false);
        LocalDate hoje = RelogioSistema.hoje();
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.individual(), f.c().gestaoA.getId(), f.c().localA,
                    "Causa individual documentada anterior à rescisão", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    hoje.minusDays(10), hoje.minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-INDIVIDUAL-RESCISAO");
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.rescisao(), f.c().gestaoA.getId(), f.c().localA,
                    "Rescisão contratual documentada sem mudança da causa individual", CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO,
                    hoje.minusDays(5), hoje.minusDays(5), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-RESCISAO");
            return null;
        });

        long anterior = cadastrar(f, 0, hoje.minusDays(20));
        long aberto = cadastrar(f, 1, hoje.minusDays(20));
        terminar(f, anterior, hoje.minusDays(10));
        Linha antes = linha(anterior);
        LocalDate dataRescisao = hoje.minusDays(5);
        Map<String, Object> req = mapa("dataEfetiva", dataRescisao, "dataAto", hoje,
                "motivo", "Rescisão documentada sem reescrever desligamento individual", "referenciaSei", "SEI-RESCISAO-CRITERIO");
        JsonNode previa = enviar(f.c().localA, base(f.contrato().id()) + "/previas/encerrar-antecipadamente", req, 200,
                "VIN015 prévia da rescisão distingue vínculo aberto e desligamento anterior");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        JsonNode contrato = enviar(f.c().localA, base(f.contrato().id()) + "/encerrar-antecipadamente", req, 200,
                "VIN015 rescisão aplica data própria apenas aos vínculos atingidos");
        assertThat(contrato.path("dataFim").asText()).isEqualTo(dataRescisao.toString());
        assertThat(linha(anterior)).as("Término individual anterior não ganha nova causa nem evento contratual")
                .isEqualTo(antes);
        conferirFim(aberto, dataRescisao, f.rescisao(), CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
        JsonNode eventos = consultar(f.c().localA, V + "/" + aberto + "/eventos", "VIN015 rescisão auditada do vínculo aberto");
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("ENCERRAMENTO_ANTECIPADO_CONTRATO");
        assertThat(eventos.get(1).path("dataEfetiva").asText()).isEqualTo(dataRescisao.toString());
    }

    @Test
    void adiarInicioAlemDeFaixaFuturaExigeTratarProgramacaoAntesDaRetificacao() throws Exception {
        Preparado f = preparar(true);
        LocalDate hoje = RelogioSistema.hoje();
        long vinculo = cadastrar(f, 0, hoje.minusDays(20));
        JsonNode b = alocar(f, vinculo, hoje.plusDays(5), f.contrato().composicao2(), null);
        alocar(f, vinculo, hoje.plusDays(10), f.contrato().composicao1(), null);
        Linha antes = linha(vinculo);
        Map<String, Object> req = acao(); req.put("novoInicio", hoje.plusDays(12));
        JsonNode previa = enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-inicio", req, 200,
                "VIN014 prévia aponta a faixa futura que perderia completamente a cobertura");
        assertThat(previa.path("impactos").toString()).contains(String.valueOf(b.path("id").asLong()));
        confirmar(req, previa);
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-inicio", req, 409,
                "VIN014 adiar início não cancela silenciosamente decisão futura independente");
        assertThat(linha(vinculo)).as("Negação conserva início, faixas e toda a trilha em outra transação")
                .isEqualTo(antes);
        assertThat(consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + hoje.plusDays(6),
                "Programação B permanece no dia que perderia cobertura").path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao2());
    }

    @Test
    void duasRetificacoesDeAlocacaoNoMesmoDiaPreservamTresVersoesEUmaFaixaEfetiva() throws Exception {
        Preparado f = preparar(true);
        LocalDate inicio = RelogioSistema.hoje().minusDays(20), dia = inicio.plusDays(10);
        long vinculo = cadastrar(f, 0, inicio);
        long primeira = alocar(f, vinculo, dia, f.contrato().composicao2(), null).path("id").asLong();
        long segunda = alocar(f, vinculo, dia, f.contrato().composicao1(), primeira).path("id").asLong();
        long terceira = alocar(f, vinculo, dia, f.contrato().composicao2(), segunda).path("id").asLong();
        assertThat(List.of(primeira, segunda, terceira)).doesNotHaveDuplicates();
        emTransacao(() -> {
            assertThat(em.find(AlocacaoVinculo.class, primeira).getEstadoNegocial()).isEqualTo(EstadoNegocialAlocacao.SUBSTITUIDA);
            assertThat(em.find(AlocacaoVinculo.class, segunda).getEstadoNegocial()).isEqualTo(EstadoNegocialAlocacao.SUBSTITUIDA);
            assertThat(em.find(AlocacaoVinculo.class, terceira).getEstadoNegocial()).isEqualTo(EstadoNegocialAlocacao.REGISTRADA);
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:v and a.dataInicio=:d", Long.class)
                    .setParameter("v", vinculo).setParameter("d", dia).getSingleResult()).isEqualTo(3);
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:v and a.estadoNegocial=:estado "
                            + "and a.dataInicio<=:d and (a.dataFim is null or a.dataFim>=:d)", Long.class)
                    .setParameter("v", vinculo).setParameter("estado", EstadoNegocialAlocacao.REGISTRADA)
                    .setParameter("d", dia).getSingleResult()).isEqualTo(1);
            assertThat(em.find(VinculoContratual.class, vinculo).getDataInicio()).isEqualTo(inicio);
            assertThat(em.find(VinculoContratual.class, vinculo).getDataFim()).isNull();
            return null;
        });
        JsonNode faixas = consultar(f.c().localA, V + "/" + vinculo + "/alocacoes", "ALO004 todas as versões continuam consultáveis");
        List<Long> ids = new ArrayList<>(); faixas.forEach(a -> ids.add(a.path("id").asLong()));
        assertThat(ids).contains(primeira, segunda, terceira);
        JsonNode eventos = consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/eventos", "ALO004 retificação de retificação encadeada");
        List<JsonNode> retificacoes = new ArrayList<>();
        eventos.forEach(e -> { if ("RETIFICACAO".equals(e.path("tipoEvento").asText())) retificacoes.add(e); });
        assertThat(retificacoes).hasSize(2);
        assertThat(retificacoes.get(0).path("alocacaoAnteriorId").asLong()).isEqualTo(primeira);
        assertThat(retificacoes.get(0).path("alocacaoNovaId").asLong()).isEqualTo(segunda);
        assertThat(retificacoes.get(1).path("alocacaoAnteriorId").asLong()).isEqualTo(segunda);
        assertThat(retificacoes.get(1).path("alocacaoNovaId").asLong()).isEqualTo(terceira);
        // Cada retificação registra também o ajuste explícito do estado da faixa
        // substituída. Esse fato intermediário integra a cadeia e não deve sumir.
        long precedenteDaSegunda = retificacoes.get(1).path("eventoPrecedenteId").asLong();
        List<JsonNode> intermediarios = new ArrayList<>();
        eventos.forEach(e -> { if (e.path("id").asLong() == precedenteDaSegunda) intermediarios.add(e); });
        assertThat(intermediarios).hasSize(1);
        JsonNode ajuste = intermediarios.get(0);
        assertThat(ajuste.path("tipoEvento").asText()).isEqualTo("AJUSTE_LIMITE_VINCULO");
        assertThat(ajuste.path("eventoPrecedenteId").asLong()).isEqualTo(retificacoes.get(0).path("id").asLong());
        assertThat(ajuste.path("hashAnterior").asText()).isEqualTo(retificacoes.get(0).path("hashEvento").asText());
        assertThat(retificacoes.get(1).path("hashAnterior").asText()).isEqualTo(ajuste.path("hashEvento").asText());
        assertThat(retificacoes.get(0).path("hashEvento").asText()).hasSize(64);
        assertThat(retificacoes.get(1).path("hashEvento").asText()).hasSize(64);
        assertThat(consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + dia,
                "ALO004 só a última versão governa o dia retificado").path("alocacaoId").asLong()).isEqualTo(terceira);
    }

    @Test
    void sucessoraCriadaDoMapaNoNovoUltimoDiaExigePrecedenciaAntesDePersistir() throws Exception {
        Preparado f = preparar(false);
        LocalDate hoje = RelogioSistema.hoje(), fimAnterior = hoje.minusDays(10), transferencia = hoje.minusDays(5);
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.individual(), f.c().gestaoA.getId(), f.c().localA,
                    "Término individual documentado antes do mapa", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    fimAnterior, fimAnterior, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-MAPA-ORIGEM");
            return null;
        });

        long vinculo = cadastrar(f, 0, hoje.minusDays(20));
        terminar(f, vinculo, fimAnterior);
        conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", transferencia, "dataAto", hoje, "gestaoLocalDestinoId", f.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", f.c().tipoB.getId(), "motivo", "Transferência documentada posterior ao fim",
                "referenciaSei", "SEI-MAPA-ULTIMO-DIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(),
                        "lotacaoDestinoId", f.c().lotacaoB.getId(), "jornadaDestinoId", f.c().jornadaB.getId(),
                        "cargoDestinoId", f.c().cargoB.getId()))), 200,
                "ALO005 mapa foi documentado sem criar faixa no vínculo já encerrado");
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.individualB(), f.c().gestaoB.getId(), f.c().localB,
                    "Término individual documentado no destino no dia da transferência", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    transferencia, transferencia, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-LIMITES-MAPA-DESTINO");
            return null;
        });
        Linha antes = linha(vinculo);
        Map<String, Object> req = acao(); req.put("dataFim", transferencia); req.put("motivoDesligamentoId", f.individualB());
        JsonNode previa = enviar(f.c().localB, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "ALO005 prévia considera a sucessora ainda inexistente que nasce do mapeamento");
        assertThat(previa.path("impactos").toString().toLowerCase()).contains("precedência");
        confirmar(req, previa);
        enviar(f.c().localB, V + "/" + vinculo + "/retificar-desligamento", req, 409,
                "ALO005 mapeamento documental não substitui decisão de precedência no último dia");
        assertThat(linha(vinculo)).as("Sem precedência não há nova faixa, evento ou alteração de término")
                .isEqualTo(antes);
        req.put("confirmarPrecedenciaUltimoDia", true);
        confirmar(req, enviar(f.c().localB, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "Nova prévia contém a decisão inequívoca sobre a alocação do último dia"));
        enviar(f.c().localB, V + "/" + vinculo + "/retificar-desligamento", req, 200,
                "ALO005 sucessora documentada vale no último dia; desligamento depois dele");
        JsonNode posicao = consultar(f.c().localB, V + "/" + vinculo + "/alocacoes/posicao?data=" + transferencia,
                "DestinoB governa exatamente o novo último dia");
        assertThat(posicao.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
        assertThat(consultar(f.c().localB, V + "/" + vinculo + "/alocacoes/posicao?data=" + transferencia.plusDays(1),
                "A faixa de um dia não continua depois do desligamento").path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        emTransacao(() -> {
            List<AlocacaoVinculo> destino = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v and a.lotacao.id=:l", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).setParameter("l", f.c().lotacaoB.getId()).getResultList();
            assertThat(destino).hasSize(1);
            assertThat(destino.get(0).getDataInicio()).isEqualTo(transferencia);
            assertThat(destino.get(0).getDataFim()).isEqualTo(transferencia);
            assertThat(em.find(VinculoContratual.class, vinculo).getDataFim()).isEqualTo(transferencia);
            return null;
        });
    }

    private Preparado preparar(boolean duasComposicoes) {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario(); NovoContrato n = publicado(c, 6, duasComposicoes ? 3 : null, false);
                return new Preparado(c, n, motivo(c.gestaoA, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL, n.inicio()),
                        motivo(c.gestaoA, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO, n.inicio()),
                        motivo(c.gestaoA, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO, n.inicio()),
                        motivo(c.gestaoB, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL, n.inicio()));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private long motivo(GestaoLocal gestao, CategoriaTerminoVinculo categoria, LocalDate origemComprovada) {
        MotivoDesligamento m = new MotivoDesligamento(); m.setGestaoLocal(gestao); m.setCategoria(categoria);
        m.setDescricao("Critério histórico " + categoria + " " + gestao.getId()); em.persist(m);
        // O cenário comprova a disponibilidade desde seu início contratual;
        // a flag atual de um motivo criado hoje não prova o período anterior.
        EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
        origem.setDataEfetiva(origemComprovada); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita do motivo desde o início do cenário contratual");
        em.persist(origem); return m.getId();
    }

    private long cadastrar(Preparado f, int ordinal, LocalDate inicio) throws Exception {
        int seed = Math.toIntExact(747000000L + f.contrato().id() * 10L + ordinal);
        return enviar(f.c().localA, V, mapa("pessoa", mapa("nome", "Pessoa critérios históricos " + seed, "cpf", cpf(seed)),
                "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(), "dataInicio", inicio), 201,
                "Cadastro integrado estabelece a linha canônica original").path("id").asLong();
    }

    private void terminar(Preparado f, long vinculo, LocalDate fim) throws Exception {
        Map<String, Object> req = acao(); req.put("dataFim", fim); req.put("motivoDesligamentoId", f.individual());
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", req, 200, "Desligamento individual anterior documentado");
    }

    private JsonNode alocar(Preparado f, long vinculo, LocalDate data, Long composicao, Long retificar) throws Exception {
        Map<String, Object> req = acao(); req.put("dataEfetiva", data); req.put("composicaoId", composicao);
        confirmar(req, enviar(f.c().localA, V + "/" + vinculo + "/previas/alocacoes", req, 200,
                "Prévia de decisão temporal de alocação"));
        return enviar(f.c().localA, V + "/" + vinculo + "/alocacoes" + (retificar == null ? "" : "/" + retificar + "/retificar"),
                req, 200, "Alocação é confirmada com sua própria prévia");
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Conferência documental do limite e contexto efetivos",
                "evidencia", "SEI-LIMITES-CRITERIOS", "autorizacao", "ATO-LIMITES-CRITERIOS");
    }

    private void confirmar(Map<String, Object> req, JsonNode previa) {
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
    }

    private void conferirFim(long vinculo, LocalDate fim, long motivo, CategoriaTerminoVinculo categoria) {
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, vinculo);
            assertThat(v.getDataFim()).isEqualTo(fim);
            assertThat(v.getMotivoDesligamento().getId()).isEqualTo(motivo);
            assertThat(v.getMotivoDesligamento().getCategoria()).isEqualTo(categoria);
            return null;
        });
    }

    private Linha linha(long vinculo) {
        return emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, vinculo);
            return new Linha(v.getId() + "|" + v.getDataInicio() + "|" + v.getDataFim() + "|" + v.getEstadoNegocial()
                    + "|" + (v.getMotivoDesligamento() == null ? null : v.getMotivoDesligamento().getId()),
                    linhas("select a.id,a.dataInicio,a.dataFim,a.estadoNegocial,a.composicao.id from AlocacaoVinculo a where a.vinculo.id=:v order by a.id", vinculo),
                    linhas("select e.id,e.tipoEvento,e.hashEvento from EventoVinculoContratual e where e.vinculo.id=:v order by e.id", vinculo),
                    linhas("select e.id,e.tipoEvento,e.hashEvento from EventoAlocacaoVinculo e where e.vinculo.id=:v order by e.id", vinculo));
        });
    }

    private List<String> linhas(String jpql, long vinculo) {
        return em.createQuery(jpql, Object[].class).setParameter("v", vinculo).getResultList().stream().map(Arrays::toString).toList();
    }

    private record Preparado(Cenario c, NovoContrato contrato, long individual, long natural, long rescisao, long individualB) {}
    private record Linha(String vinculo, List<String> alocacoes, List<String> eventosVinculo, List<String> eventosAlocacao) {}
}
