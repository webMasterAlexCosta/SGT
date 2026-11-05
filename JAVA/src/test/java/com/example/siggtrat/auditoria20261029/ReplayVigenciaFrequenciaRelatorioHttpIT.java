package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** TMP-008/014, FRE-001 e REL-001 após commits reais de fontes recebidas fora de ordem. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReplayVigenciaFrequenciaRelatorioHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void doisAditamentosDeFimNaMesmaDataExigemPrecedenciaAntesDaConfirmacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = confirmar(() -> publicado(c, 5, null, false));
        LocalDate efeito = RelogioSistema.hoje().plusDays(5);
        var primeiro = aditamento("FIM-A-" + n.id(), efeito);
        primeiro.put("novaDataFim", n.fim().plusDays(10));
        var a = confirmar(() -> registrarAditamento(c, n.id(), primeiro));
        var segundo = aditamento("FIM-B-" + n.id(), efeito);
        segundo.put("novaDataFim", n.fim().plusDays(20));
        confirmar(() -> previaAditamento(c, n.id(), segundo));
        enviar(c.localA, base(n.id()) + "/aditamentos", segundo, 409,
                "Dois deltas de vigência no mesmo dia não recebem precedência pela ordem dos POSTs");
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "Recusa não cria instrumento parcial")).hasSize(1);

        segundo.put("eventoPrecedenteId", a.path("eventoId").asLong());
        segundo.put("relacaoPrecedencia", "SUCEDE");
        confirmar(() -> registrarAditamento(c, n.id(), segundo));
        var p = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + efeito,
                "Relação SUCEDE explícita ordena os dois deltas publicados");
        assertThat(p.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(20).toString());
        assertThat(p.path("versaoContratual").path("dataFim")).isEqualTo(p.path("vigenciaFim"));
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "Ambos os instrumentos permanecem auditáveis")).hasSize(2);
    }

    @Test
    void vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate hoje = RelogioSistema.hoje();
        NovoContrato n = confirmar(() -> publicarPeriodo(c, hoje.minusDays(30), hoje.minusDays(8)));
        // A prorrogação tardia preserva eventual fim natural anterior. O cenário
        // declara a causa e sua disponibilidade histórica exigidas por VIN-010;
        // a disponibilidade não é inferida da flag atual do catálogo.
        emTransacao(() -> {
            var motivo = new com.example.siggtrat.entidade.MotivoDesligamento();
            motivo.setGestaoLocal(c.gestaoA); motivo.setAtivo(true);
            motivo.setDescricao("Fim natural documentado para o replay " + n.id());
            motivo.setCategoria(com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
            em.persist(motivo); em.flush();
            var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa natural desde o início do contrato de replay");
            em.persist(origem); em.flush();
            // Fonte contextual opt-in deste replay: atividade não comprova categoria no fim antigo.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, n.id(), motivo.getId(), c.gestaoA.getId(), c.localA,
                    "Fim natural documentado para o replay",
                    com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                    n.fim(), n.fim(), com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                    "SEI-SINTETICO-C6-REPLAY-FIM-NATURAL-ANTIGO");
            assertThat(motivo.getId()).isPositive();
            return null;
        });

        var descricao = aditamento("DESCRICAO-REPLAY-" + n.id(), hoje.minusDays(12));
        descricao.put("novaDescricao", "Descrição posterior sem alteração de vigência");
        confirmar(() -> registrarAditamento(c, n.id(), descricao));
        Instant antesDaProrrogacao = Instant.now();
        var prorrogacao = aditamento("PRORROGACAO-REPLAY-" + n.id(), hoje.minusDays(16));
        prorrogacao.put("novaDataFim", hoje.plusDays(30));
        confirmar(() -> registrarAditamento(c, n.id(), prorrogacao));

        LocalDate diaVinculo = n.fim().plusDays(1), diaFrequencia = diaVinculo.plusDays(1);
        var historica = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + diaFrequencia
                        + "&conhecidoAte=" + antesDaProrrogacao,
                "Corte anterior ao commit não conhece a extensão recebida posteriormente");
        assertThat(historica.path("vigenciaFim").asText()).isEqualTo(n.fim().toString());
        assertThat(historica.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
        JsonNode fotografiasAntes = consultar(c.localA, base(n.id()) + "/versoes", "Fotografias persistidas antes das consultas");

        var atual = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + diaFrequencia,
                "Vigência e DTO interno devem concordar depois do replay");
        assertThat(atual.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(atual.path("vigenciaFim").asText()).isEqualTo(hoje.plusDays(30).toString());
        assertThat(atual.path("versaoContratual").path("dataFim")).isEqualTo(atual.path("vigenciaFim"));

        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", diaVinculo,
                "pessoa", mapa("nome", "Pessoa no período estendido", "cpf", cpf(Math.toIntExact(940000000L + n.id())))),
                201, "Novo vínculo real cabe na extensão conhecida sem reabrir vínculo encerrado");
        String status = diaFrequencia.getDayOfWeek().getValue() >= 6 ? "DESCANSO" : "FALTA";
        var frequencia = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo.path("id").asLong(), "dataReferencia", diaFrequencia, "status", status,
                "justificativaRetificacao", "Fonte documental do período prorrogado",
                "chaveIdempotencia", "freq-replay-" + UUID.randomUUID()),
                200, "Resolvedor de frequência reconhece a mesma cobertura do vínculo e do contrato");
        assertThat(frequencia.path("id").asLong()).isPositive();

        var relatorio = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                        .param("inicio", diaFrequencia.toString()).param("fim", diaFrequencia.toString())
                        .param("contratoIds", String.valueOf(n.id())),
                200, "População oficial inclui o vínculo e o lançamento no período prorrogado"));
        assertThat(relatorio.path("registros").size()).isEqualTo(1);
        var dia = relatorio.path("registros").get(0).path("diario").path(diaFrequencia.toString());
        assertThat(dia.path("lancado").asBoolean()).isTrue();
        assertThat(dia.path("status").asText()).isEqualTo(status);
        assertThat(consultar(c.localA, base(n.id()) + "/versoes", "Consulta e consumo não alteram fotografias históricas"))
                .isEqualTo(fotografiasAntes);
    }

    private NovoContrato publicarPeriodo(Cenario c, LocalDate inicio, LocalDate fim) throws Exception {
        var draft = enviar(c.localA, "/api/contratos", rascunho(c,
                "REPLAY-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim), 201,
                "Origem documental de contrato com término anterior ao período adicional");
        long id = draft.path("id").asLong();
        long composicao = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "Composição original integral").path("id").asLong();
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-REPLAY-ORIGEM",
                "justificativa", "Fonte original do contrato para replay", "confirmarImpacto", true);
        var previa = enviar(c.localA, base(id) + "/previas/publicar", publicacao, 200, "Prévia da publicação retroativa");
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localA, base(id) + "/publicar", publicacao, 200, "Publicação preserva origem antes dos instrumentos");
        return new NovoContrato(id, composicao, null, inicio, fim);
    }

    private <T> T confirmar(Operacao<T> operacao) {
        return emTransacao(() -> {
            try { return operacao.executar(); }
            catch (Exception e) { throw new IllegalStateException("Falha na preparação documentada do replay", e); }
        });
    }

    @FunctionalInterface private interface Operacao<T> { T executar() throws Exception; }
}
