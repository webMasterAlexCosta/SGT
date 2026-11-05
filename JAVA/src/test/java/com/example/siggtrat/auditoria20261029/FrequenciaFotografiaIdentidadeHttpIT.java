package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.pessoa.TipoCorrecaoPessoa;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.CorrecaoPessoa;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** COL-010/012, TMP-004 e HIS-004/005: nome atual e fotografia da jornada são dados distintos. */
class FrequenciaFotografiaIdentidadeHttpIT extends AuditoriaHttpSuporte {
    @Test
    void correcaoDeIdentidadeNaoReescreveConsultaNemRetificacaoDaFrequencia() throws Exception {
        Cenario c = criarCenario();
        String nomeAnterior = "NOME_ANTERIOR_" + UUID.randomUUID();
        String nomeAtual = "NOME_ATUAL_" + UUID.randomUUID();
        c.pessoaA.setNome(nomeAnterior); c.empregadoA.setNome(nomeAnterior);
        declararFonteNominalSintetica(c, "preservacao-frequencia-" + UUID.randomUUID());
        JsonNode criada = registrar(c, DATA, "FALTA");
        long id = criada.path("id").asLong();
        assertEquals(nomeAnterior, criada.path("empregado").path("nome").asText());
        corrigirNome(c, nomeAtual);
        em.flush(); em.clear();
        assertEquals(nomeAtual, em.find(Pessoa.class, c.pessoaA.getId()).getNome());

        for (String rota : List.of("?data=" + DATA, "/mensal?mes=6&ano=2026",
                "/intervalo?inicio=" + DATA + "&fim=" + DATA)) {
            JsonNode pagina = dados(executar(c.localA, get("/api/frequencias" + rota)
                            .param("contratoId", c.contratoA.getId().toString()),
                    200, "A consulta da frequência conserva a fotografia registrada " + rota));
            assertEquals(1, pagina.path("totalElements").asInt());
            assertEquals(nomeAnterior, pagina.path("content").get(0).path("empregado").path("nome").asText(),
                    "A correção do cadastro atual não pode renomear um lançamento histórico.");
        }

        JsonNode retificada = registrar(c, DATA, "PRESENCA");
        assertEquals(id, retificada.path("id").asLong());
        assertEquals(nomeAnterior, retificada.path("empregado").path("nome").asText(),
                "A retificação de status não é autorização para substituir a identidade fotografada.");
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "Retificação mantém as duas fotografias e o autor original"));
        assertEquals(2, eventos.size());
        assertTrue(eventos.get(0).path("snapshotNovo").asText().contains("empregadoNome=" + nomeAnterior));
        assertTrue(eventos.get(1).path("snapshotAnterior").asText().contains("empregadoNome=" + nomeAnterior));
        assertTrue(eventos.get(1).path("snapshotNovo").asText().contains("empregadoNome=" + nomeAnterior));
        assertFalse(eventos.toString().contains(nomeAtual));
        em.flush(); em.clear();
        Frequencia persistida = em.find(Frequencia.class, id);
        assertEquals(nomeAnterior, persistida.getEmpregadoSnapshotNome());
        assertEquals(c.localA.getId(), persistida.getAutorOriginalId());
        assertEquals(nomeAtual, em.find(Pessoa.class, c.pessoaA.getId()).getNome());
    }

    @ParameterizedTest(name = "referenciaHa{0}Dias")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void retificacaoDeStatusPreservaFotografiaAnteriorSemCongelarNovosDias(int diasAtras) throws Exception {
        Cenario c = criarCenario();
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate referencia = hoje.minusDays(diasAtras);
        LocalDate diaNovo = referencia.getDayOfWeek().getValue() <= 5 ? referencia
                : referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        assertTrue(diaNovo.getDayOfWeek().getValue() <= 5);
        assertFalse(diaNovo.isAfter(referencia));
        assertFalse(referencia.isAfter(hoje));
        assertTrue(diaNovo.isAfter(DATA), "Os dois lançamentos pertencem a dias distintos dentro da fixture.");
        String nomeAnterior = c.pessoaA.getNome();
        String nomeAtual = "Nome legal posterior " + UUID.randomUUID();
        declararFonteNominalSintetica(c, "novos-dias-frequencia-" + UUID.randomUUID());
        comprovarDiaTrabalhadoSemFrequencia(c, DATA);
        JsonNode original = registrar(c, DATA, "FALTA");
        corrigirNome(c, nomeAtual);
        em.flush(); em.clear();
        assertEquals(nomeAtual, em.find(Pessoa.class, c.pessoaA.getId()).getNome());
        List<CorrecaoPessoa> correcoes = em.createQuery(
                        "select x from CorrecaoPessoa x where x.pessoa.id=:p order by x.id", CorrecaoPessoa.class)
                .setParameter("p", c.pessoaA.getId()).getResultList();
        assertEquals(1, correcoes.size(), "A correção real precede a captura da nova frequência.");
        CorrecaoPessoa correcao = correcoes.get(0);
        assertEquals(TipoCorrecaoPessoa.NOME, correcao.getTipoCorrecao());
        assertEquals(nomeAnterior, correcao.getValorAnterior());
        assertEquals(nomeAtual, correcao.getValorNovo());
        assertEquals(c.localA.getId(), correcao.getResponsavelId());
        Instant correcaoRegistradaEm = correcao.getRegistradoEm();
        assertNotNull(correcaoRegistradaEm);
        JsonNode historica = registrar(c, DATA, "PRESENCA");
        assertEquals(original.path("id"), historica.path("id"));
        assertEquals(nomeAnterior, historica.path("empregado").path("nome").asText(),
                "A retificação de status conserva a fotografia já registrada no fato original.");
        // TMP-004/005 e COL-010: inserir depois da correção não prova sua vigência
        // passada. Fonte opt-in conforme CONFLITOS_ORACULOS_E_FIXTURES.md.
        String nomeEsperado = diaNovo.isBefore(hoje) ? nomeAnterior : nomeAtual;
        comprovarDiaTrabalhadoSemFrequencia(c, diaNovo);
        JsonNode atual = registrar(c, diaNovo, "FALTA");
        assertNotEquals(original.path("id"), atual.path("id"));
        assertEquals(nomeEsperado, atual.path("empregado").path("nome").asText(),
                "O nome segue a fonte do dia efetivo; conhecimento da correção não retroage sua validade.");
        em.flush(); em.clear();
        Frequencia persistidaAnterior = em.find(Frequencia.class, historica.path("id").asLong());
        Frequencia persistidaNova = em.find(Frequencia.class, atual.path("id").asLong());
        assertEquals(nomeAnterior, persistidaAnterior.getEmpregadoSnapshotNome());
        assertEquals(nomeEsperado, persistidaNova.getEmpregadoSnapshotNome());
        assertEquals(DATA, persistidaAnterior.getDataReferencia());
        assertEquals(diaNovo, persistidaNova.getDataReferencia());
        assertEquals("PRESENCA", persistidaAnterior.getStatus());
        assertEquals("FALTA", persistidaNova.getStatus());
        assertEquals(Boolean.TRUE, persistidaAnterior.getJornadaSnapshotDiaTrabalho());
        assertEquals(Boolean.TRUE, persistidaNova.getJornadaSnapshotDiaTrabalho());
        assertFalse(persistidaAnterior.getRegistradoEm().isAfter(correcaoRegistradaEm));
        assertFalse(persistidaNova.getRegistradoEm().isBefore(correcaoRegistradaEm),
                "O novo fato é registrado com conhecimento da correção nominal já persistida.");
        assertEquals(2L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    private void comprovarDiaTrabalhadoSemFrequencia(Cenario c, LocalDate dia) {
        em.flush(); em.clear();
        VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
        AlocacaoVinculo alocacao = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
        assertNotNull(vinculo); assertNotNull(alocacao);
        assertEquals(c.contratoA.getId(), vinculo.getContrato().getId());
        assertEquals(vinculo.getId(), alocacao.getVinculo().getId());
        assertFalse(vinculo.getDataInicio().isAfter(dia));
        if (vinculo.getDataFim() != null) assertFalse(vinculo.getDataFim().isBefore(dia));
        assertFalse(alocacao.getDataInicio().isAfter(dia));
        if (alocacao.getDataFim() != null) assertFalse(alocacao.getDataFim().isBefore(dia));
        var semana = FotografiaJornada.dias(alocacao.getJornadaSemanalFotografia());
        assertEquals(7, semana.size(), "A fonte é a semana fotografada da Alocação, não o mestre atual.");
        var dias = semana.stream().filter(d -> d.diaSemana() == dia.getDayOfWeek()).toList();
        assertEquals(1, dias.size());
        assertTrue(dias.get(0).diaTrabalho());
        assertEquals(480, dias.get(0).minutosPrevistos());
        Long gestaoId = alocacao.getLotacao().getGestaoLocal().getId();
        Long seId = alocacao.getLotacao().getGestaoLocal().getSuperintendencia().getId();
        List<CalendarioEvento> descansos = em.createQuery("select e from CalendarioEvento e where e.data=:dia "
                        + "and e.cancelado=false and (e.ativo is null or e.ativo=true) "
                        + "and upper(e.tipo) in ('FERIADO','DIA_DESCANSO')", CalendarioEvento.class)
                .setParameter("dia", dia).getResultList();
        // Mais forte que a restrição de município/Lotação: não existe descanso
        // global nem na SE/Gestão própria deste cenário. Nenhuma fonte é removida.
        assertTrue(descansos.stream().noneMatch(e -> e.getGestaoLocal() == null && e.getSuperintendencia() == null
                || e.getGestaoLocal() != null && Objects.equals(gestaoId, e.getGestaoLocal().getId())
                || e.getSuperintendencia() != null && Objects.equals(seId, e.getSuperintendencia().getId())),
                "Pré-condição: nenhuma fonte de descanso alcança o cenário no dia escolhido.");
        assertEquals(0L, em.createQuery("select count(e) from FrequenciaExcecao e where e.empregado.id=:e "
                        + "and e.dataInicio<=:dia and e.dataFim>=:dia", Long.class)
                .setParameter("e", c.empregadoA.getId()).setParameter("dia", dia).getSingleResult());
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v "
                        + "and f.dataReferencia=:dia", Long.class)
                .setParameter("v", c.vinculoA.getId()).setParameter("dia", dia).getSingleResult(),
                "A data deve receber um registro novo, nunca retificar uma fotografia já existente.");
    }

    private void corrigirNome(Cenario c, String nome) throws Exception {
        JsonNode atual = dados(executar(c.localA, post("/api/pessoas/{id}/correcoes-identidade", c.pessoaA.getId())
                .contentType(APPLICATION_JSON).content(json(Map.of(
                        "tipo", "NOME", "novoValor", nome, "motivo", "Alteração legal de nome documentada",
                        "evidencia", "Documento SEI de identidade", "autorizacao", "Despacho da Gestão Local",
                        "impactosEncontrados", "Cadastro atual e projeção; fotografias históricas preservadas"))),
                200, "Correção excepcional do cadastro atual da Pessoa"));
        assertEquals(nome, atual.path("nome").asText());
    }

    private JsonNode registrar(Cenario c, LocalDate data, String status) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(Map.of("vinculoContratualId", c.vinculoA.getId(),
                        "dataReferencia", data.toString(), "status", status,
                        "justificativaRetificacao", "Conferência documental da jornada histórica",
                        "chaveIdempotencia", "fotografia-frequencia-" + UUID.randomUUID()))),
                200, "Gravação formal da frequência na data de início da jornada"));
    }
}
