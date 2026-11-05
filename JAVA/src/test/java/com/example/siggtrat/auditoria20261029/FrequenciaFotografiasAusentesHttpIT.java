package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** RI07: ausência de fotografia não se torna dado comprovado por uma operação posterior. */
class FrequenciaFotografiasAusentesHttpIT extends ContratosHttpBase {
    @Autowired AuditoriaFrequenciaServico auditoria;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pendenciaDeRevisaoPreservaNomeConhecidoOuAusenteDaFrequencia(boolean nomeComprovado) throws Exception {
        Cenario c = criarCenario();
        String original = nomeComprovado ? c.pessoaA.getNome() : null;
        Frequencia f = fonte(c, original, false);
        long id = f.getId();
        em.flush();
        auditoria.registrar(f, TipoEventoFrequencia.REGISTRO, null,
                "Registro original com os dados nominais comprovados disponiveis", null, null, null);
        String atual = "NOME_ATUAL_PENDENCIA_" + UUID.randomUUID();
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", atual, "motivo", "Correcao legal posterior ao fato",
                "evidencia", "SEI-NOME-PENDENCIA", "autorizacao", "Despacho da Gestao Local",
                "impactosEncontrados", "Nao completar fotografias historicas ausentes"),
                200, "Correcao atual nao modifica a fonte historica da futura pendencia");
        var ato = mapa("dataEfetiva", DATA, "motivo", "Inativacao retroativa documentada sobre a jornada original");
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/inativar", ato,
                200, "Previa do impacto que originara revisao da frequencia");
        ato.put("tokenPrevia", previa.path("tokenPrevia").asText());
        ato.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", ato,
                200, "Ato retroativo cria pendencia a partir da fonte original");
        em.flush(); em.clear();
        Frequencia relida = em.find(Frequencia.class, id);
        assertEquals("SUJEITA_REVISAO", relida.getEstadoAuditoria().name());
        assertEquals("FECHADA", relida.getEstadoNegocial().name());
        assertEquals(original, relida.getEmpregadoSnapshotNome());
        PendenciaAdministrativa p = em.createQuery("select p from PendenciaAdministrativa p where p.frequenciaId=:id", PendenciaAdministrativa.class)
                .setParameter("id", id).getSingleResult();
        assertEquals(c.pessoaA.getId(), p.getPessoaId());
        assertEquals(c.vinculoA.getId(), p.getVinculoId());
        assertEquals(c.gestaoA.getId(), p.getGestaoLocalId());
        JsonNode pagina = consultar(c.localA, "/api/pendencias?tipo=RETIFICACAO_FREQUENCIA&inicio=" + DATA
                        + "&fim=" + DATA + "&contratoIds=" + c.contratoA.getId(),
                "Consulta da pendencia conserva o nome efetivamente comprovado pela frequencia de origem");
        assertEquals(1, pagina.path("registros").size());
        JsonNode consulta = pagina.path("registros").get(0);
        assertEquals(p.getId().longValue(), consulta.path("id").asLong());
        assertAll(
                () -> assertEquals(original, p.getPessoaFotografia()),
                () -> assertEquals(original, consulta.path("pessoa").isNull() ? null : consulta.path("pessoa").asText()),
                () -> assertFalse(consulta.toString().contains(atual)));
    }

    @ParameterizedTest
    @CsvSource({"CRIACAO,false", "RETIFICACAO,false", "RETIFICACAO,true"})
    void gravacaoNaoCompletaRotulosLegadosComMestreEPreservaProvaDaPropriaFrequencia(String operacao, boolean fotoFrequencia) throws Exception {
        Cenario c = criarCenario();
        String lotacaoOriginal = c.lotacaoA.getNome();
        String jornadaOriginal = c.jornadaA.getDescricao();
        String cargoOriginal = c.cargoA.getNome();
        Long frequenciaAnteriorId = null;
        JsonNode eventoOriginal = null;
        if (operacao.equals("RETIFICACAO")) {
            Frequencia f = fonte(c, c.pessoaA.getNome(), fotoFrequencia);
            em.flush();
            auditoria.registrar(f, TipoEventoFrequencia.REGISTRO, null,
                    "Frequencia original com fotografia disponivel sem completar lacunas", null, null, null);
            frequenciaAnteriorId = f.getId();
            eventoOriginal = consultar(c.localA, "/api/frequencias/" + f.getId() + "/eventos", "Evento original antes da retificacao").get(0);
        }
        em.flush();
        // Simula somente fotografias nominais ausentes de acervo legado. Identidade e grade permanecem comprovadas.
        assertEquals(1, em.createQuery("update AlocacaoVinculo set lotacaoNomeFotografia=null, "
                        + "jornadaDescricaoFotografia=null, cargoNomeFotografia=null where id=:id")
                .setParameter("id", c.alocacaoA.getId()).executeUpdate());
        em.clear();
        String sufixo = UUID.randomUUID().toString();
        String lotacaoAtual = "LOTACAO_ATUAL_GRAVACAO_" + sufixo;
        String jornadaAtual = "JORNADA_ATUAL_GRAVACAO_" + sufixo;
        String cargoAtual = "CARGO_ATUAL_GRAVACAO_" + sufixo;
        em.find(Lotacao.class, c.lotacaoA.getId()).setNome(lotacaoAtual);
        em.find(Jornada.class, c.jornadaA.getId()).setDescricao(jornadaAtual);
        em.find(Cargo.class, c.cargoA.getId()).setNome(cargoAtual);
        em.flush(); em.clear();
        assertNotNull(em.find(AlocacaoVinculo.class, c.alocacaoA.getId()).getJornadaSemanalFotografia());
        JsonNode resposta = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA, "status", "PRESENCA",
                "justificativaRetificacao", "Documento altera somente presenca, sem recuperar rotulos historicos",
                "chaveIdempotencia", "rotulos-gravacao-" + UUID.randomUUID()),
                200, "Gravacao usa o contexto historico comprovado sem fabricar nomes de catalogo");
        long id = resposta.path("id").asLong();
        if (frequenciaAnteriorId != null) assertEquals(frequenciaAnteriorId.longValue(), id);
        em.flush(); em.clear();
        Frequencia atualizada = em.find(Frequencia.class, id);
        assertEquals(c.vinculoA.getId(), atualizada.getVinculoContratual().getId());
        assertEquals(c.alocacaoA.getId(), atualizada.getAlocacaoVinculo().getId());
        assertEquals(c.lotacaoA.getId(), atualizada.getLotacaoSnapshotId());
        assertEquals(c.jornadaA.getId(), atualizada.getJornadaSnapshotId());
        assertEquals(c.cargoA.getId(), atualizada.getCargoSnapshotId());
        assertEquals(480, atualizada.getMinutosFisicos());
        JsonNode eventos = consultar(c.localA, "/api/frequencias/" + id + "/eventos", "Versoes nao inventam fotografia ausente");
        assertEquals(operacao.equals("CRIACAO") ? 1 : 2, eventos.size());
        if (eventoOriginal != null) assertEquals(eventoOriginal, eventos.get(0));
        String depois = eventos.get(eventos.size() - 1).path("snapshotNovo").asText();
        assertAll(
                () -> assertEquals(fotoFrequencia ? lotacaoOriginal : null, atualizada.getLotacaoSnapshotNome()),
                () -> assertEquals(fotoFrequencia ? jornadaOriginal : null, atualizada.getJornadaSnapshotDescricao()),
                () -> assertEquals(fotoFrequencia ? cargoOriginal : null, atualizada.getCargoSnapshotNome()),
                () -> assertFalse(depois.contains(lotacaoAtual)),
                () -> assertFalse(depois.contains(jornadaAtual)),
                () -> assertFalse(depois.contains(cargoAtual)));
        if (!fotoFrequencia) {
            assertTrue(depois.contains("lotacaoNome=\\N;"));
            assertTrue(depois.contains("jornadaDescricao=\\N;"));
            assertTrue(depois.contains("cargoNome=\\N;"));
        }
    }

    private Frequencia fonte(Cenario c, String nome, boolean rotulosComprovados) {
        Frequencia f = frequencia(c, false, DATA, "FALTA");
        f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
        f.setEmpregadoSnapshotNome(nome); f.setContratoSnapshotNumero(c.contratoA.getNumeroContrato());
        f.setJornadaSnapshotDiaTrabalho(true);
        f.setJornadaSnapshotEntrada1("08:00"); f.setJornadaSnapshotSaida1("12:00");
        f.setJornadaSnapshotEntrada2("13:00"); f.setJornadaSnapshotSaida2("17:00");
        f.setLotacaoSnapshotMunicipio(c.lotacaoA.getMunicipio());
        f.setMinutosFisicos(0); f.setMinutosNoturnosReais(0); f.setMinutosAdicionais(0); f.setMinutosPagaveis(0);
        f.setLotacaoSnapshotNome(rotulosComprovados ? c.lotacaoA.getNome() : null);
        f.setJornadaSnapshotDescricao(rotulosComprovados ? c.jornadaA.getDescricao() : null);
        f.setCargoSnapshotNome(rotulosComprovados ? c.cargoA.getNome() : null);
        return f;
    }
}
