package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.CorrecaoPessoa;
import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Texto livre pode carregar dados pessoais fora dos campos tipados de documento. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasNarrativasMinimizacaoHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais/";

    @Test
    void motivoEImpactosDaCorrecaoPessoalSaoMinimizadosSemAlterarFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        String motivo = "Documento pessoal RG-CORRECAO-SINTETICO-" + c.pessoaA.getId();
        String impactos = "Contato reservado impacto-" + c.pessoaA.getId() + "@example.test";
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade";
        enviar(c.localA, rota, mapa("tipo", "NOME", "novoValor", "Nome corrigido da pessoa " + c.pessoaA.getId(),
                        "motivo", motivo, "impactosEncontrados", impactos,
                        "evidencia", "SEI-CORRECAO-NARRATIVA", "autorizacao", "ATO-CORRECAO-NARRATIVA"),
                200, "COL006 correção excepcional conserva a narrativa original completa");
        JsonNode original = consultar(c.localA, rota, "O Gestor Local responsável consulta o motivo e os impactos integrais");
        assertThat(original.toString()).contains(motivo, impactos);

        conferirPerfis(c, rota, original, motivo, impactos);

        emTransacao(() -> {
            CorrecaoPessoa fonte = em.find(CorrecaoPessoa.class, original.get(0).path("id").asLong());
            assertThat(fonte.getMotivo()).isEqualTo(motivo);
            assertThat(fonte.getImpactosEncontrados()).isEqualTo(impactos);
            return null;
        });
    }

    @Test
    void justificativaDoEventoDeVinculoEMinimizadaSemAlterarFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        // Este cenário conhece a admissão original; a fixture JPA genérica não
        // fornece fonte nominal e deve continuar servindo aos controles de ausência.
        String nomeDocumentado = c.pessoaA.getNome();
        declararFonteNominalSintetica(c, "PESSOAS-NARRATIVA-MATRICULA");
        emTransacao(() -> {
            em.flush(); em.clear();
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(vinculo.getDataInicio()).isEqualTo(INICIO);
            assertThat(vinculo.getDataFim()).isNull();
            assertThat(DATA).isAfter(INICIO).isBefore(com.example.siggtrat.utilitario.RelogioSistema.hoje());
            var fontes = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v "
                            + "and e.dataEfetiva<=:dia order by e.registradoEm,e.id", EventoVinculoContratual.class)
                    .setParameter("v", vinculo.getId()).setParameter("dia", DATA.minusDays(1)).getResultList();
            assertThat(fontes).hasSize(1);
            EventoVinculoContratual fonte = fontes.get(0);
            assertThat(fonte.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
            assertThat(fonte.getDataEfetiva()).isEqualTo(INICIO);
            assertThat(fonte.getRegistradoEm()).isEqualTo(INICIO.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            assertThat(fonte.getPessoaNomeFotografia()).isEqualTo(nomeDocumentado);
            assertThat(fonte.getPessoaCpfFotografia()).isEqualTo(vinculo.getPessoa().getCpf());
            assertThat(fonte.getInicioNovo()).isEqualTo(vinculo.getDataInicio());
            assertThat(fonte.getGestaoLocalIdFotografia()).isEqualTo(c.gestaoA.getId());
            assertThat(fonte.getResponsavelId()).isEqualTo(c.localA.getId());
            assertThat(fonte.getEvidencia()).isEqualTo("SEI-FIXTURE-PESSOAS-NARRATIVA-MATRICULA");
            assertThat(fonte.getAutorizacao()).isEqualTo("ATO-FIXTURE-PESSOAS-NARRATIVA-MATRICULA");
            assertThat(fonte.getHashEvento()).isNotBlank();
            return null;
        });
        String narrativa = "Ato contém RG-VINCULO-SINTETICO-" + c.vinculoA.getId() + "; reservado@example.test";
        enviar(c.localA, V + c.vinculoA.getId() + "/alterar-matricula",
                mapa("dataEfetiva", DATA, "novaMatricula", "NARR-" + c.vinculoA.getId(), "justificativa", narrativa,
                        "evidencia", "SEI-MATRICULA-NARRATIVA", "autorizacao", "ATO-MATRICULA-NARRATIVA"),
                200, "MAT001 alteração real de matrícula preserva justificativa de origem");
        String rota = V + c.vinculoA.getId() + "/eventos";
        JsonNode original = consultar(c.localA, rota, "Gestor responsável consulta a justificativa completa do vínculo");
        assertThat(original.toString()).contains(narrativa);

        conferirPerfis(c, rota, original, narrativa);

        emTransacao(() -> {
            for (JsonNode evento : original) {
                EventoVinculoContratual fonte = em.find(EventoVinculoContratual.class, evento.path("id").asLong());
                assertThat(fonte.getJustificativa()).isEqualTo(evento.path("justificativa").asText());
                assertThat(fonte.getHashEvento()).isEqualTo(evento.path("hashEvento").asText());
            }
            return null;
        });
    }

    @Test
    void justificativaDoEventoDeAlocacaoEMinimizadaSemAlterarFonte() throws Exception {
        Preparado preparado = emTransacao(() -> {
            Cenario c = criarCenario();
            Cargo cargo = new Cargo();
            cargo.setNome("Cargo de narrativa " + c.gestaoA.getId());
            cargo.setGestaoLocal(c.gestaoA);
            em.persist(cargo);
            var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO);
            origem.setCatalogoId(cargo.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do Cargo anterior ao evento de alocação com narrativa");
            em.persist(origem);
            long composicao = composicao(c.contratoA, c.lotacaoA, c.jornadaA, cargo, 5).getId();
            em.flush();
            return new Preparado(c, composicao);
        });
        Cenario c = preparado.c();
        String narrativa = "Ato contém RG-ALOCACAO-SINTETICO-" + c.vinculoA.getId() + "; pessoal@example.test";
        Map<String, Object> req = mapa("dataEfetiva", DATA, "composicaoId", preparado.composicao(),
                "justificativa", narrativa, "evidencia", "SEI-ALOCACAO-NARRATIVA", "autorizacao", "ATO-ALOCACAO-NARRATIVA");
        JsonNode previa = enviar(c.localA, V + c.vinculoA.getId() + "/previas/alocacoes", req, 200,
                "ALO003 mudança de alocação com narrativa livre tem prévia real");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("token").asText());
        enviar(c.localA, V + c.vinculoA.getId() + "/alocacoes", req, 200,
                "ALO002 registra nova faixa e evento com o conteúdo declarado");
        String rota = V + c.vinculoA.getId() + "/alocacoes/eventos";
        JsonNode original = consultar(c.localA, rota, "Gestor responsável consulta a justificativa completa da alocação");
        assertThat(original.toString()).contains(narrativa);

        conferirPerfis(c, rota, original, narrativa);

        emTransacao(() -> {
            for (JsonNode evento : original) {
                EventoAlocacaoVinculo fonte = em.find(EventoAlocacaoVinculo.class, evento.path("id").asLong());
                assertThat(fonte.getJustificativa()).isEqualTo(evento.path("justificativa").asText());
                assertThat(fonte.getHashEvento()).isEqualTo(evento.path("hashEvento").asText());
            }
            return null;
        });
    }

    private void conferirPerfis(Cenario c, String rota, JsonNode original, String... marcadores) throws Exception {
        Map<Long, JsonNode> porId = new LinkedHashMap<>();
        original.forEach(evento -> porId.put(evento.path("id").asLong(), evento));
        for (UsuarioAplicacao leitor : List.of(c.localA, c.operacionalA, c.estadualA, c.nacional, c.admin, c.presidente)) {
            JsonNode resposta = consultar(leitor, rota, "PER006 PER011 minimização de texto livre para " + leitor.getPerfil());
            assertThat(resposta).hasSize(original.size());
            if (leitor == c.localA) assertThat(resposta).isEqualTo(original);
            else assertThat(resposta.toString()).as("Dados pessoais não escapam pela narrativa para %s", leitor.getPerfil())
                    .doesNotContain(marcadores);
            for (JsonNode evento : resposta) {
                JsonNode fonte = porId.get(evento.path("id").asLong());
                assertThat(fonte).isNotNull();
                for (String campo : List.of("id", "tipo", "tipoEvento", "dataEfetiva", "registradoEm",
                        "responsavelId", "responsavelUsername", "hashEvento", "hashAnterior")) {
                    if (fonte.has(campo)) assertThat(evento.path(campo)).as("Metadado %s preservado", campo).isEqualTo(fonte.path(campo));
                }
            }
        }
        executar(c.localB, get(rota), 404, "Minimizar conteúdo não concede acesso à Gestão alheia");
        executar(c.operacionalB, get(rota), 404, "Minimizar conteúdo não concede acesso ao par Lotação–Turno alheio");
    }

    private record Preparado(Cenario c, long composicao) {}
}
