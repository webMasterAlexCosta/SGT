package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** RI07: faixa histórica da grade usa seus rótulos; o nome pessoal corrente continua corrente. */
class GradeFrequenciaRotulosHistoricosHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void gradeNaDataPassadaConservaRotulosFotografadosOuDesconhecidos(boolean possuiFoto) throws Exception {
        Cenario c = criarCenario();
        String lotacaoOriginal = c.lotacaoA.getNome();
        String jornadaOriginal = c.jornadaA.getDescricao();
        String cargoOriginal = c.cargoA.getNome();
        if (!possuiFoto) {
            em.flush();
            assertEquals(1, em.createQuery("update AlocacaoVinculo set lotacaoNomeFotografia=null, "
                            + "jornadaDescricaoFotografia=null, cargoNomeFotografia=null where id=:id")
                    .setParameter("id", c.alocacaoA.getId()).executeUpdate());
        }
        em.clear();
        String sufixo = UUID.randomUUID().toString();
        String lotacaoAtual = "LOTACAO_ATUAL_GRADE_" + sufixo;
        String jornadaAtual = "JORNADA_ATUAL_GRADE_" + sufixo;
        String cargoAtual = "CARGO_ATUAL_GRADE_" + sufixo;
        em.find(Lotacao.class, c.lotacaoA.getId()).setNome(lotacaoAtual);
        em.find(Jornada.class, c.jornadaA.getId()).setDescricao(jornadaAtual);
        em.find(Cargo.class, c.cargoA.getId()).setNome(cargoAtual);
        em.flush(); em.clear();
        String nomePessoalAtual = "PESSOA_ATUAL_DA_GRADE_" + sufixo;
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", nomePessoalAtual, "motivo", "Correcao do cadastro pessoal corrente",
                "evidencia", "SEI-GRADE-NOME", "autorizacao", "Despacho da Gestao Local",
                "impactosEncontrados", "Nome pessoal corrente separado dos rotulos historicos da alocacao"),
                200, "Grade operacional pode exibir nome pessoal atual sem alterar a fotografia do recorte");
        JsonNode pagina = consultar(c.localA, "/api/empregados/filtrados-frequencia?dataRef=" + DATA
                        + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                        + "&turnoId=" + c.jornadaA.getId(),
                "Faixa canonica historica conserva seus rotulos e seus identificadores");
        assertEquals(1, pagina.path("content").size());
        JsonNode linha = pagina.path("content").get(0);
        assertEquals(c.empregadoA.getId().longValue(), linha.path("id").asLong());
        assertEquals(c.lotacaoA.getId().longValue(), linha.path("lotacaoId").asLong());
        assertEquals(c.jornadaA.getId().longValue(), linha.path("turnoId").asLong());
        assertEquals(nomePessoalAtual, linha.path("nome").asText(), "A coluna pessoal corrente nao e fotografia da frequencia");
        em.flush(); em.clear();
        AlocacaoVinculo faixa = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
        assertNotNull(faixa.getJornadaSemanalFotografia());
        assertEquals(c.cargoA.getId(), faixa.getCargo().getId());
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:id", Long.class)
                .setParameter("id", c.vinculoA.getId()).getSingleResult(), "Leitura da grade nao fabrica uma fonte de frequencia");
        assertAll(
                () -> assertEquals(possuiFoto ? lotacaoOriginal : null, texto(linha, "lotacaoNome")),
                () -> assertEquals(possuiFoto ? jornadaOriginal : null, texto(linha, "turnoDescricao")),
                () -> assertEquals(possuiFoto ? cargoOriginal : null, texto(linha, "cargoNome")),
                () -> assertEquals(possuiFoto ? lotacaoOriginal : null, faixa.getLotacaoNomeFotografia()),
                () -> assertEquals(possuiFoto ? jornadaOriginal : null, faixa.getJornadaDescricaoFotografia()),
                () -> assertEquals(possuiFoto ? cargoOriginal : null, faixa.getCargoNomeFotografia()),
                () -> assertFalse(linha.toString().contains(lotacaoAtual)),
                () -> assertFalse(linha.toString().contains(jornadaAtual)),
                () -> assertFalse(linha.toString().contains(cargoAtual)));
    }

    private String texto(JsonNode linha, String campo) {
        assertTrue(linha.has(campo));
        return linha.path(campo).isNull() ? null : linha.path(campo).asText();
    }
}
