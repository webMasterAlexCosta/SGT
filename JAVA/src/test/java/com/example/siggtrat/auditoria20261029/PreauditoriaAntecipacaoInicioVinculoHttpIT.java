package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.entidade.ProvaNomePessoaHistorico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** VIN-014: antecipar início passado deve ser possível também para um vínculo criado pela API. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreauditoriaAntecipacaoInicioVinculoHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "[{index}] renomearAtual={0}, fonteNominalAntecipada={1}")
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void antecipaInicioPassadoComPreviaEFonteSemInventarMudancaNominal(
            boolean renomearDepoisDaCriacao, boolean comprovarNomeNoPeriodoAntecipado) throws Exception {
        Cenario c = criarCenarioPersistido();
        String nome = "Identidade documentada no cadastro de origem";
        String cpfNovo;
        do {
            cpfNovo = cpf(ThreadLocalRandom.current().nextInt(500_000_000, 999_999_999));
            String candidato = cpfNovo;
            if (emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", candidato).getSingleResult()) == 0L) break;
        } while (true);
        JsonNode criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", nome, "cpf", cpfNovo),
                "contratoId", c.contratoA.getId(), "dataInicio", DATA.plusDays(5),
                "composicaoId", c.composicaoA.getId()), 201,
                "VIN-001: Pessoa, Vínculo e Alocação cadastrados pela API com fotografia de origem");
        long id = criado.path("id").asLong();
        String rota = "/api/vinculos-contratuais/" + id;
        JsonNode eventosOriginais = consultar(c.localA, rota + "/eventos",
                "Prova de origem permanece íntegra antes da retificação");
        assertThat(eventosOriginais).hasSize(1);
        assertThat(eventosOriginais.get(0).path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(eventosOriginais.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(nome);
        String nomeAtual = "Nome legal novo posterior à fotografia inicial";
        if (renomearDepoisDaCriacao) {
            JsonNode pessoaAtual = enviar(c.localA,
                    "/api/pessoas/" + criado.path("pessoaId").asLong() + "/correcoes-identidade",
                    mapa("tipo", "NOME", "novoValor", nomeAtual, "motivo", "Mudança legal atual comprovada",
                            "evidencia", "SEI-PREAUDITORIA-MUDANCA-NOME", "autorizacao", "ATO-NOME",
                            "impactosEncontrados", "Preservar a fotografia histórica do vínculo " + id), 200,
                    "COL-009/COL-010: nome atual muda sem reescrever a fotografia da criação");
            assertThat(pessoaAtual.path("nome").asText()).isEqualTo(nomeAtual);
        }

        long pessoaId = criado.path("pessoaId").asLong();
        assertEquals(0L, quantidadeProvasNominais(pessoaId, id));
        if (comprovarNomeNoPeriodoAntecipado) {
            // Fonte opt-in somente de 10 a 14/06: o cadastro original começa em 15/06.
            declararProvaNominalDoPeriodoAntecipado(c, pessoaId, id, nome);
        }
        assertEquals(comprovarNomeNoPeriodoAntecipado ? 1L : 0L,
                quantidadeProvasNominais(pessoaId, id));
        assertEquals(eventosOriginais, consultar(c.localA, rota + "/eventos",
                "A prova independente não altera nem acrescenta evento à criação original"));

        Map<String, Object> req = mapa("novoInicio", DATA,
                "justificativa", "Documento original comprova início cinco dias antes do informado",
                "evidencia", "SEI-PREAUDITORIA-ADMISSAO-E-IDENTIDADE",
                "autorizacao", "Conferência documental pelo Gestor Local responsável");
        JsonNode previa = enviar(c.localA, rota + "/previas/retificacao-inicio", req, 200,
                "VIN-014/AUD-004: antecipação válida recebe prévia e revalidação de impactos");
        req.put("tokenPrevia", previa.path("token").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, rota + "/retificar-inicio", req, 200,
                "VIN-014: início passado pode ser antecipado dentro do contrato sem conflito de identidade");

        assertEquals(DATA, emTransacao(() -> em.find(VinculoContratual.class, id).getDataInicio()));
        JsonNode eventosDepois = consultar(c.localA, rota + "/eventos",
                "Retificação acrescenta nova versão e conserva a origem");
        assertThat(eventosDepois).hasSize(2);
        assertEquals(eventosOriginais.get(0), eventosDepois.get(0));
        assertThat(eventosDepois.get(1).path("dataEfetiva").asText()).isEqualTo(DATA.toString());
        // VIN-014 corrige o início; COL-010/HIS-004 mantêm o alcance da fonte nominal.
        // A foto da criação de 15/06 não prova, sozinha, o nome em 10/06.
        if (comprovarNomeNoPeriodoAntecipado) {
            assertThat(eventosDepois.get(1).path("pessoaNomeFotografia").asText()).isEqualTo(nome);
        } else {
            assertThat(eventosDepois.get(1).path("pessoaNomeFotografia").isNull()).isTrue();
            assertThat(eventosDepois.get(1).toString()).doesNotContain(nome);
        }
        assertEquals(comprovarNomeNoPeriodoAntecipado ? 1L : 0L,
                quantidadeProvasNominais(pessoaId, id));
        if (renomearDepoisDaCriacao)
            assertThat(eventosDepois.get(1).toString()).doesNotContain(nomeAtual);
        JsonNode posicao = consultar(c.localA, rota + "/alocacoes/posicao?data=" + DATA,
                "ALO-007: primeiro dia antecipado fica coberto por uma alocação válida");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
    }

    private long quantidadeProvasNominais(long pessoaId, long vinculoId) {
        return emTransacao(() -> em.createQuery(
                "select count(p) from ProvaNomePessoaHistorico p where p.pessoa.id=:p and p.vinculo.id=:v", Long.class)
                .setParameter("p", pessoaId).setParameter("v", vinculoId).getSingleResult());
    }

    /** Prova explícita de um intervalo anterior; não estende nem reescreve a CRIACAO. */
    private void declararProvaNominalDoPeriodoAntecipado(Cenario c, long pessoaId, long vinculoId,
                                                        String nome) throws Exception {
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        long pessoaVersao = consultar(c.localA, "/api/pessoas/" + pessoaId,
                "Versão da Pessoa depois da eventual mudança do nome atual").path("version").asLong();
        long vinculoVersao = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId,
                "Versão do Vínculo antes da prova nominal explícita").path("version").asLong();
        Map<String, Object> req = mapa("vinculoId", vinculoId,
                "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA.plusDays(4), "nomeComprovado", nome,
                "referenciaSei", "SEI-SINTETICO-PREAUDITORIA-NOME-" + UUID.randomUUID(),
                "motivo", "Fonte sintética explícita do nome entre 10 e 14/06, anterior à criação documentada em 15/06",
                "autorizacao", "ATO-HTTP", "versaoPessoaEsperada", pessoaVersao,
                "versaoVinculoEsperada", vinculoVersao, "chaveIdempotencia", UUID.randomUUID().toString());
        String rota = "/api/pessoas/" + pessoaId + "/provas-nome";
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200,
                "Prévia da prova nominal limitada ao intervalo antecipado deste controle positivo");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        JsonNode prova = enviar(c.localA, rota, req, 201,
                "Prova nominal explícita do período antecipado sem alterar a fotografia original");
        emTransacao(() -> {
            ProvaNomePessoaHistorico fonte = em.find(ProvaNomePessoaHistorico.class, prova.path("id").asLong());
            assertNotNull(fonte);
            assertEquals(pessoaId, fonte.getPessoa().getId().longValue());
            assertEquals(vinculoId, fonte.getVinculo().getId().longValue());
            assertEquals(c.contratoA.getId(), fonte.getContrato().getId());
            assertEquals(nome, fonte.getNomeComprovado());
            assertEquals(DATA, fonte.getDataInicioEfetiva());
            assertEquals(DATA.plusDays(4), fonte.getDataFimEfetiva());
            assertNotNull(fonte.getRegistradoEm());
            assertEquals(1L, em.createQuery(
                    "select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", vinculoId).getSingleResult().longValue());
            return null;
        });
    }
}
