package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** A rota de monitoramento não pode reexpor os documentos minimizados na rota de eventos. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasExcedenteNarrativasHttpIT extends ContratosHttpBase {
    @Test
    void excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 2, 1, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate inicio = RelogioSistema.hoje().minusDays(3);
        String privadoVinculo = "RG-VINCULO-EXCEDENTE-" + f.n().id();
        String privadoAlocacao = "RG-ALOCACAO-EXCEDENTE-" + f.n().id();
        String evidencia = "documento-pessoal-" + f.n().id() + "@example.test";
        String autorizacao = "Autorização pessoal CPF-SINTETICO-" + f.n().id();
        enviar(f.c().localA, "/api/vinculos-contratuais", cadastro(f, 0, f.n().composicao1(), inicio), 201,
                "Preparação: primeira pessoa ocupa A sem excedente");
        Map<String, Object> excedente = cadastro(f, 1, f.n().composicao1(), inicio);
        excedente.put("confirmarInclusaoExcedente", true);
        excedente.put("justificativaExcedente", privadoVinculo);
        excedente.put("evidenciaExcedente", evidencia);
        excedente.put("autorizacaoExcedente", autorizacao);
        long segundo = enviar(f.c().localA, "/api/vinculos-contratuais", excedente, 201,
                "CMP009 criação de vínculo excedente mantém a fonte pessoal original").path("id").asLong();
        long terceiro = enviar(f.c().localA, "/api/vinculos-contratuais", cadastro(f, 2, f.n().composicao2(), inicio), 201,
                "Preparação: terceiro vínculo nasce em B sem excedente").path("id").asLong();
        Map<String, Object> mudanca = mapa("dataEfetiva", RelogioSistema.hoje().minusDays(1),
                "composicaoId", f.n().composicao1(), "justificativa", privadoAlocacao,
                "evidencia", evidencia, "autorizacao", autorizacao, "confirmarInclusaoExcedente", true);
        String raiz = "/api/vinculos-contratuais/" + terceiro;
        JsonNode previa = enviar(f.c().localA, raiz + "/previas/alocacoes", mudanca, 200,
                "Prévia real da inclusão excedente por alocação");
        mudanca.put("confirmarImpacto", true); mudanca.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localA, raiz + "/alocacoes", mudanca, 200,
                "CMP009 excedente por alocação também conserva sua fonte documentada");

        String rota = base(f.n().id()) + "/composicoes/excedentes?data=" + RelogioSistema.hoje();
        JsonNode original = linha(consultar(f.c().localA, rota, "Gestor responsável vê as justificativas completas"), f.n().composicao1());
        assertThat(original.path("quantidadeExcedente").asLong()).isEqualTo(2);
        assertThat(original.path("justificativas").toString()).contains(privadoVinculo, privadoAlocacao, evidencia, autorizacao);
        assertThat(original.path("justificativas").toString()).contains("VINCULO", "ALOCACAO");
        for (var leitor : List.of(f.c().localA, f.c().operacionalA, f.c().estadualA, f.c().nacional, f.c().admin, f.c().presidente)) {
            JsonNode atual = linha(consultar(leitor, rota, "PER006 PER011 monitoramento com minimização para " + leitor.getPerfil()),
                    f.n().composicao1());
            for (String campo : List.of("composicaoId", "efetivoPrevisto", "quantidadeVinculadaAtual", "quantidadeExcedente", "desdeQuandoExcedente"))
                assertThat(atual.path(campo)).isEqualTo(original.path(campo));
            assertThat(atual.path("justificativas")).hasSize(original.path("justificativas").size());
            if (leitor == f.c().localA) assertThat(atual).isEqualTo(original);
            else assertThat(atual.toString()).as("Consulta por composição não reexpõe narrativa pessoal para %s", leitor.getPerfil())
                    .doesNotContain(privadoVinculo, privadoAlocacao, evidencia, autorizacao);
            for (JsonNode justificativa : atual.path("justificativas")) {
                assertThat(justificativa.path("eventoId").asLong()).isPositive();
                assertThat(justificativa.path("vinculoId").asLong()).isPositive();
                assertThat(justificativa.path("origemEvento").asText()).isIn("VINCULO", "ALOCACAO");
            }
        }
        executar(f.c().localB, get(rota), 404, "Monitoramento não amplia o escopo para outra Gestão");
        emTransacao(() -> {
            java.util.Set<String> fontes = new java.util.HashSet<>();
            for (JsonNode j : original.path("justificativas")) {
                if ("VINCULO".equals(j.path("origemEvento").asText())) {
                    var e = em.find(EventoVinculoContratual.class, j.path("eventoId").asLong());
                    assertThat(e.getVinculo().getId()).isEqualTo(segundo);
                    assertThat(j.path("vinculoId").asLong()).isEqualTo(segundo);
                    assertThat(e.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
                    assertThat(fontes.add("VINCULO:INCLUSAO")).isTrue();
                    assertThat(e.getJustificativa()).contains(privadoVinculo);
                    assertThat(e.getEvidencia()).isEqualTo(evidencia);
                    assertThat(e.getAutorizacao()).isEqualTo(autorizacao);
                } else {
                    var e = em.find(EventoAlocacaoVinculo.class, j.path("eventoId").asLong());
                    assertThat(e.getVinculo().getId()).isEqualTo(j.path("vinculoId").asLong());
                    if (e.getVinculo().getId().equals(segundo)) {
                        // A inclusão integrada também documenta a alocação inicial
                        // com a mesma fonte do vínculo; não é a mudança do terceiro.
                        assertThat(e.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo.CRIACAO);
                        assertThat(e.getDataEfetiva()).isEqualTo(inicio);
                        assertThat(e.getJustificativa()).contains(privadoVinculo).doesNotContain(privadoAlocacao);
                        assertThat(fontes.add("ALOCACAO:INCLUSAO")).isTrue();
                    } else {
                        assertThat(e.getVinculo().getId()).isEqualTo(terceiro);
                        assertThat(e.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo.ALTERACAO);
                        assertThat(e.getDataEfetiva()).isEqualTo(RelogioSistema.hoje().minusDays(1));
                        assertThat(e.getJustificativa()).contains(privadoAlocacao).doesNotContain(privadoVinculo);
                        assertThat(fontes.add("ALOCACAO:MUDANCA")).isTrue();
                    }
                    assertThat(e.getEvidencia()).isEqualTo(evidencia);
                    assertThat(e.getAutorizacao()).isEqualTo(autorizacao);
                }
            }
            assertThat(fontes).containsExactlyInAnyOrder("VINCULO:INCLUSAO", "ALOCACAO:INCLUSAO", "ALOCACAO:MUDANCA");
            return null;
        });
    }

    private Map<String, Object> cadastro(Preparado f, int ordinal, long composicao, LocalDate inicio) {
        int seed = Math.toIntExact(720000000L + f.n().id() * 10L + ordinal);
        return mapa("pessoa", mapa("nome", "Pessoa excedente privado " + seed, "cpf", cpf(seed)),
                "contratoId", f.n().id(), "composicaoId", composicao, "dataInicio", inicio);
    }

    private JsonNode linha(JsonNode linhas, long composicao) {
        List<JsonNode> encontradas = new ArrayList<>();
        linhas.forEach(linha -> { if (linha.path("composicaoId").asLong() == composicao) encontradas.add(linha); });
        assertThat(encontradas).hasSize(1);
        return encontradas.get(0);
    }

    private record Preparado(Cenario c, NovoContrato n) {}
}
