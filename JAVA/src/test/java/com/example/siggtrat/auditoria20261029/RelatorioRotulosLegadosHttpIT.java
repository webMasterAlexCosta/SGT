package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** RI07/CAT004/HIS004: rótulos históricos não são completados com nomes atuais do catálogo. */
class RelatorioRotulosLegadosHttpIT extends ContratosHttpBase {
    private enum FonteFoto { AUSENTE, ALOCACAO, FREQUENCIA }

    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo"})
    void relatorioPreservaRotulosComprovadosEExplicitaAusenciaLegada(String tipo) {
        assertAll(
                () -> conferir(tipo, FonteFoto.AUSENTE),
                () -> conferir(tipo, FonteFoto.ALOCACAO),
                () -> conferir(tipo, FonteFoto.FREQUENCIA));
    }

    private void conferir(String tipo, FonteFoto fonte) throws Exception {
        Cenario c = criarCenario();
        String lotacaoOriginal = c.lotacaoA.getNome();
        String jornadaOriginal = c.jornadaA.getDescricao();
        String cargoOriginal = c.cargoA.getNome();
        Frequencia f = frequencia(c, false, DATA, "FALTA");
        f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
        f.setEmpregadoSnapshotNome(c.pessoaA.getNome());
        f.setContratoSnapshotNumero(c.contratoA.getNumeroContrato());
        f.setJornadaSnapshotDiaTrabalho(true);
        f.setJornadaSnapshotEntrada1("08:00"); f.setJornadaSnapshotSaida1("12:00");
        f.setJornadaSnapshotEntrada2("13:00"); f.setJornadaSnapshotSaida2("17:00");
        f.setLotacaoSnapshotMunicipio(c.lotacaoA.getMunicipio());
        f.setMinutosFisicos(0); f.setMinutosNoturnosReais(0); f.setMinutosAdicionais(0); f.setMinutosPagaveis(0);
        f.setLotacaoSnapshotNome(fonte == FonteFoto.FREQUENCIA ? lotacaoOriginal : null);
        f.setJornadaSnapshotDescricao(fonte == FonteFoto.FREQUENCIA ? jornadaOriginal : null);
        f.setCargoSnapshotNome(fonte == FonteFoto.FREQUENCIA ? cargoOriginal : null);
        em.flush();
        long id = f.getId();
        if (fonte != FonteFoto.ALOCACAO) {
            // DML restrito à fixture simula acervo anterior às fotografias; IDs, município e grade permanecem.
            assertEquals(1, em.createQuery("update AlocacaoVinculo set lotacaoNomeFotografia=null, "
                            + "jornadaDescricaoFotografia=null, cargoNomeFotografia=null where id=:id")
                    .setParameter("id", c.alocacaoA.getId()).executeUpdate());
        }
        em.clear();
        String sufixo = UUID.randomUUID().toString();
        String lotacaoAtual = "LOTACAO_ATUAL_SEM_PROVA_" + sufixo;
        String jornadaAtual = "JORNADA_ATUAL_SEM_PROVA_" + sufixo;
        String cargoAtual = "CARGO_ATUAL_SEM_PROVA_" + sufixo;
        em.find(Lotacao.class, c.lotacaoA.getId()).setNome(lotacaoAtual);
        em.find(Jornada.class, c.jornadaA.getId()).setDescricao(jornadaAtual);
        em.find(Cargo.class, c.cargoA.getId()).setNome(cargoAtual);
        em.flush(); em.clear();
        AlocacaoVinculo alocacao = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
        assertEquals(c.lotacaoA.getId(), alocacao.getLotacao().getId());
        assertEquals(c.jornadaA.getId(), alocacao.getJornada().getId());
        assertEquals(c.cargoA.getId(), alocacao.getCargo().getId());
        assertNotNull(alocacao.getJornadaSemanalFotografia(), "A grade comprovada continua permitindo calcular a jornada");
        assertEquals(fonte == FonteFoto.ALOCACAO ? lotacaoOriginal : null, alocacao.getLotacaoNomeFotografia());
        assertEquals(0L, em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());

        AbstractMockHttpServletRequestBuilder<?> pedido = get("/api/relatorios/" + tipo);
        if (tipo.equals("frequencia-consolidada")) {
            pedido.param("inicio", DATA.toString()).param("fim", DATA.toString())
                    .param("contratoIds", c.contratoA.getId().toString());
        } else {
            pedido.param("mes", String.valueOf(DATA.getMonthValue())).param("ano", String.valueOf(DATA.getYear()))
                    .param("contratoId", c.contratoA.getId().toString());
        }
        JsonNode resposta = dados(executar(c.localA, pedido, 200,
                "Rotulos historicos com fonte " + fonte + " conservam prova e desconhecimento sem usar catalogo atual"));
        JsonNode registros = tipo.equals("frequencia-consolidada") ? resposta.path("registros") : resposta;
        assertEquals(1, registros.size(), "Ausencia de rotulos nao remove a pessoa com identidade e contexto comprovados");
        JsonNode linha = registros.get(0);
        assertEquals(c.empregadoA.getId().longValue(), linha.path("empregadoId").asLong());
        assertEquals(1, (tipo.equals("frequencia-consolidada") ? linha.path("totais") : linha).path("faltas").asInt());
        if (tipo.equals("frequencia-consolidada")) {
            assertEquals(c.lotacaoA.getId().longValue(), linha.path("lotacaoId").asLong());
            assertEquals(c.jornadaA.getId().longValue(), linha.path("jornadaId").asLong());
            assertEquals(c.cargoA.getId().longValue(), linha.path("cargoId").asLong());
        }
        em.flush(); em.clear();
        Frequencia relida = em.find(Frequencia.class, id);
        assertAll(
                () -> assertEquals(fonte == FonteFoto.AUSENTE ? null : lotacaoOriginal, texto(linha, "lotacao"), fonte.toString()),
                () -> assertEquals(fonte == FonteFoto.AUSENTE ? null : jornadaOriginal, texto(linha, "turno"), fonte.toString()),
                () -> assertEquals(fonte == FonteFoto.AUSENTE ? null : cargoOriginal, texto(linha, "cargo"), fonte.toString()),
                () -> assertFalse(resposta.toString().contains(lotacaoAtual)),
                () -> assertFalse(resposta.toString().contains(jornadaAtual)),
                () -> assertFalse(resposta.toString().contains(cargoAtual)),
                () -> assertEquals(fonte == FonteFoto.FREQUENCIA ? lotacaoOriginal : null, relida.getLotacaoSnapshotNome()),
                () -> assertEquals(fonte == FonteFoto.FREQUENCIA ? jornadaOriginal : null, relida.getJornadaSnapshotDescricao()),
                () -> assertEquals(fonte == FonteFoto.FREQUENCIA ? cargoOriginal : null, relida.getCargoSnapshotNome()));
    }

    private String texto(JsonNode linha, String campo) {
        assertTrue(linha.has(campo), "O campo permanece explícito, inclusive quando desconhecido: " + campo);
        return linha.path(campo).isNull() ? null : linha.path(campo).asText();
    }
}
