package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.example.siggtrat.entidade.VersaoComplementoFrequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Revisor transversal C4, hipótese C4-TR-H05. Não executada pelo autor.
 * A fixture com fonte é sintética explicitamente declarada; não é acervo histórico.
 * A fixture sem fonte conserva a identidade canônica e o contexto comprovado,
 * mas não possui origem nominal. Não se apaga qualquer dado para fabricar ausência. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4ComplementoNomeHistoricoRedIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"adicionais", "excecoes"})
    void complementoRetroativoSemOrigemNominalNaoCertificaNomeDoCadastroAtual(String recurso) throws Exception {
        assertThat(DATA).isBefore(com.example.siggtrat.utilitario.RelogioSistema.hoje());
        Cenario comFonte = criarCenarioPersistido();
        declararFonteNominalSintetica(comFonte, "C4-COMPLEMENTO-NOME-CONTROLE");
        String rota = "/api/frequencias-" + recurso;
        JsonNode controle = enviar(comFonte.localA, rota, pedido(comFonte, recurso), 201,
                "Controle positivo: fonte nominal original declarada cobre o dia retroativo");
        Estado controlePersistido = estado(recurso, controle.path("id").asLong());
        assertThat(controle.path("empregadoNome").asText()).isEqualTo(comFonte.pessoaA.getNome());
        assertThat(controlePersistido.nomeRaiz()).isEqualTo(comFonte.pessoaA.getNome());

        Cenario semFonte = criarCenarioPersistido();
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", semFonte.vinculoA.getId()).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(p) from ProvaNomePessoaHistorico p where p.pessoa.id=:p", Long.class)
                    .setParameter("p", semFonte.pessoaA.getId()).getSingleResult()).isZero();
            return null;
        });
        assertThat(semFonte.pessoaA.getNome()).isNotBlank();
        JsonNode criada = enviar(semFonte.localA, rota, pedido(semFonte, recurso), 201,
                "Nome corrente não é prova nominal de fato retroativo; campo desconhecido continua desconhecido");
        long id = criada.path("id").asLong();
        Estado persistida = estado(recurso, id);
        JsonNode eventos = consultar(semFonte.localA, rota + "/" + id + "/eventos",
                "Cadeia publicada não pode certificar o nome retroativo não documentado");

        Path evidencia = Path.of("target/auditoria-http/candidato4-complemento-nome-" + recurso + ".json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "hipotese", "C4-TR-H05", "recurso", recurso, "data", DATA,
                "controleComFonte", controle, "controlePersistido", controlePersistido,
                "ausenciaNominalVerificadaEmTransacaoNova", true,
                "cadastroAtualTemNome", true, "respostaSemFonte", criada,
                "persistenciaSemFonte", persistida, "historicoSemFonte", eventos)));

        assertAll("Contexto e identidade válidos não transformam nome atual em fotografia do passado",
                () -> assertThat(criada.path("empregadoNome").asText(null)).isNull(),
                () -> assertThat(persistida.nomeRaiz()).isNull(),
                () -> assertThat(persistida.nomeNaVersao()).isNull(),
                () -> assertThat(eventos.size()).isEqualTo(1),
                () -> assertThat(eventos.path(0).path("fotografia").path("empregadoNome").asText(null)).isNull());
    }

    private Map<String,Object> pedido(Cenario c, String recurso) {
        return recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                        "tipo", "FOLGA_REMUNERADA", "justificativa", "Fonte de exceção para conferência nominal histórica")
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT",
                        "minutos", 30, "quantidade", 1, "justificativa", "Fonte adicional para conferência nominal histórica");
    }

    private Estado estado(String recurso, long id) {
        return emTransacao(() -> {
            String tipo = recurso.equals("excecoes") ? "EXCECAO" : "ADICIONAL";
            String nome = recurso.equals("excecoes")
                    ? em.find(FrequenciaExcecao.class, id).getEmpregadoNomeFotografia()
                    : em.find(FrequenciaAdicional.class, id).getEmpregadoNomeFotografia();
            VersaoComplementoFrequencia versao = em.createQuery(
                    "select v from VersaoComplementoFrequencia v where v.tipoComplemento=:tipo and v.complementoId=:id",
                    VersaoComplementoFrequencia.class).setParameter("tipo", tipo).setParameter("id", id).getSingleResult();
            try {
                return new Estado(nome, mapper.readTree(versao.getFotografiaJson()).path("empregadoNome").asText(null),
                        versao.getHashVersao(), versao.getNumeroVersao());
            } catch (Exception ex) { throw new IllegalStateException("Fotografia persistida ilegível", ex); }
        });
    }
    private record Estado(String nomeRaiz, String nomeNaVersao, String hashVersao, Integer versao) { }
}
