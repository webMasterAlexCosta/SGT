package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.StatusEmpregado;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.siggtrat.servico.ProjecaoOperacionalPessoaServico;
import com.example.siggtrat.entidade.Contrato;

/** Controle combinado da hipótese expressa em LIMITES_E_PROXIMAS_VERIFICACOES.md. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasRecontratacaoAposInativacaoHttpIT extends ContratosHttpBase {
    @Autowired ProjecaoOperacionalPessoaServico projecao;
    @ParameterizedTest(name = "referenciaHa{0}Dias")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void recontratacaoDepoisDeInativarProjecaoEncerradaVoltaAConsultaGradeEFrequenciaSemDuplicarIdentidade(int diasAtras) throws Exception {
        Cenario c = emTransacao(() -> {
            Cenario preparado = criarCenario();
            if (em.createQuery("select count(s) from StatusEmpregado s where upper(s.descricao)='INATIVO'", Long.class)
                    .getSingleResult() == 0) em.persist(new StatusEmpregado("INATIVO"));
            return preparado;
        });
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate referencia = hoje.minusDays(diasAtras);
        // Cada invocação parte de um dos sete dias de referência. FALTA exige
        // dia trabalhado: terça no vínculo anterior e sexta no novo vínculo.
        // A grade permanece consultada em hoje, inclusive sábado/domingo.
        LocalDate diaFrequenciaNova = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        LocalDate inicioAnterior = diaFrequenciaNova.minusDays(10);
        LocalDate diaFrequenciaAnterior = diaFrequenciaNova.minusDays(3);
        LocalDate fimAnterior = diaFrequenciaNova.minusDays(2);
        LocalDate inicioNovo = diaFrequenciaNova.minusDays(1);
        assertThat(diaFrequenciaAnterior.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(diaFrequenciaNova.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(inicioAnterior).isBefore(diaFrequenciaAnterior);
        assertThat(diaFrequenciaAnterior).isBeforeOrEqualTo(fimAnterior);
        assertThat(fimAnterior).isBefore(inicioNovo);
        assertThat(inicioNovo).isBeforeOrEqualTo(diaFrequenciaNova);
        assertThat(diaFrequenciaNova).isBeforeOrEqualTo(referencia).isBeforeOrEqualTo(hoje);
        String cpf = cpf(Math.toIntExact(744000000L + c.contratoA.getId()));
        JsonNode primeiro = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa recontratada após inativação " + cpf, "cpf", cpf),
                        "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                        "dataInicio", inicioAnterior, "dataFim", fimAnterior), 201,
                "Preparação: vínculo encerrado antes de hoje com identidade criada por HTTP");
        long pessoaId = primeiro.path("pessoaId").asLong(), vinculoAnterior = primeiro.path("id").asLong();
        long empregadoId = emTransacao(() -> em.createQuery(
                "select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult());
        comprovarDiaTrabalhadoSemDescansoAplicavel(c, vinculoAnterior, empregadoId, diaFrequenciaAnterior);
        JsonNode frequenciaAnterior = registrarFrequencia(c, vinculoAnterior, diaFrequenciaAnterior);
        long frequenciaAnteriorId = frequenciaAnterior.path("id").asLong();
        List<String> historiaAnterior = historia(vinculoAnterior, frequenciaAnteriorId);

        // Reprocessamento operacional de vínculo já encerrado: não existe DELETE de Pessoa/Empregado.
        emTransacao(() -> { projecao.sincronizarAposAlteracaoContratual(em.find(Contrato.class, c.contratoA.getId())); return null; });
        assertThat(emTransacao(() -> em.find(Empregado.class, empregadoId).getStatus().getDescricao()))
                .isEqualToIgnoringCase("INATIVO");
        assertThat(historia(vinculoAnterior, frequenciaAnteriorId)).isEqualTo(historiaAnterior);

        JsonNode novo = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoaId", pessoaId, "contratoId", c.contratoA.getId(),
                        "composicaoId", c.composicaoA.getId(), "dataInicio", inicioNovo), 201,
                "COL013 novo vínculo após inativação reutiliza a Pessoa comprovada");
        long vinculoNovo = novo.path("id").asLong();
        assertThat(vinculoNovo).isNotEqualTo(vinculoAnterior);
        assertThat(novo.path("pessoaId").asLong()).isEqualTo(pessoaId);

        JsonNode detalhe = consultar(c.localA, "/api/empregados/" + empregadoId,
                "A consulta corrente usa a nova vigência da mesma identidade");
        assertThat(detalhe.path("id").asLong()).isEqualTo(empregadoId);
        assertThat(detalhe.path("statusDescricao").asText()).isEqualToIgnoringCase("ATIVO");
        assertThat(ids(consultar(c.localA, "/api/empregados?status=ATIVO&search=" + cpf,
                "Filtro ativo inclui a Pessoa recontratada após inativação"))).containsExactly(empregadoId);
        assertThat(ids(consultar(c.operacionalA, "/api/empregados/filtrados-frequencia?dataRef=" + hoje
                        + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                        + "&turnoId=" + c.jornadaA.getId() + "&searchTerm=" + cpf,
                "Grade operacional inclui o novo vínculo sem criar outra projeção"))).containsExactly(empregadoId);
        comprovarDiaTrabalhadoSemDescansoAplicavel(c, vinculoNovo, empregadoId, diaFrequenciaNova);
        JsonNode frequenciaNova = registrarFrequencia(c, vinculoNovo, diaFrequenciaNova);
        assertThat(frequenciaNova.path("empregado").path("id").asLong()).isEqualTo(empregadoId);

        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                    .setParameter("p", pessoaId).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select v.id from VinculoContratual v where v.pessoa.id=:p order by v.id", Long.class)
                    .setParameter("p", pessoaId).getResultList()).containsExactly(vinculoAnterior, vinculoNovo);
            Frequencia antiga = em.find(Frequencia.class, frequenciaAnteriorId);
            Frequencia atual = em.find(Frequencia.class, frequenciaNova.path("id").asLong());
            assertThat(antiga.getDataReferencia()).isEqualTo(diaFrequenciaAnterior);
            assertThat(atual.getDataReferencia()).isEqualTo(diaFrequenciaNova);
            assertThat(antiga.getStatus()).isEqualTo("FALTA");
            assertThat(atual.getStatus()).isEqualTo("FALTA");
            assertThat(antiga.getJornadaSnapshotDiaTrabalho()).isTrue();
            assertThat(atual.getJornadaSnapshotDiaTrabalho()).isTrue();
            assertThat(antiga.getPessoa().getId()).isEqualTo(pessoaId);
            assertThat(atual.getPessoa().getId()).isEqualTo(pessoaId);
            assertThat(antiga.getVinculoContratual().getId()).isEqualTo(vinculoAnterior);
            assertThat(atual.getVinculoContratual().getId()).isEqualTo(vinculoNovo);
            assertThat(antiga.getEmpregado().getId()).isEqualTo(empregadoId);
            assertThat(atual.getEmpregado().getId()).isEqualTo(empregadoId);
            return null;
        });
        assertThat(historia(vinculoAnterior, frequenciaAnteriorId))
                .as("Recontratação preserva faixas, eventos e fotografia da frequência anterior após commit")
                .isEqualTo(historiaAnterior);
    }

    private void comprovarDiaTrabalhadoSemDescansoAplicavel(Cenario c, long vinculoId,
                                                           long empregadoId, LocalDate dia) {
        emTransacao(() -> {
            em.flush(); em.clear();
            VinculoContratual vinculo = em.find(VinculoContratual.class, vinculoId);
            assertThat(vinculo).as("Pré-condição: vínculo HTTP já confirmado").isNotNull();
            assertThat(vinculo.getContrato().getId()).isEqualTo(c.contratoA.getId());
            assertThat(vinculo.getDataInicio()).isBeforeOrEqualTo(dia);
            if (vinculo.getDataFim() != null) assertThat(vinculo.getDataFim()).isAfterOrEqualTo(dia);
            var alocacoes = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v",
                            AlocacaoVinculo.class).setParameter("v", vinculoId).getResultList();
            assertThat(alocacoes).as("Pré-condição: uma alocação canônica para este vínculo").hasSize(1);
            AlocacaoVinculo alocacao = alocacoes.get(0);
            assertThat(alocacao.getDataInicio()).isBeforeOrEqualTo(dia);
            if (alocacao.getDataFim() != null) assertThat(alocacao.getDataFim()).isAfterOrEqualTo(dia);
            assertThat(alocacao.getComposicao().getId()).isEqualTo(c.composicaoA.getId());
            assertThat(alocacao.getJornada().getId()).isEqualTo(c.jornadaA.getId());
            var semana = FotografiaJornada.dias(alocacao.getJornadaSemanalFotografia());
            assertThat(semana).as("A prova vem da semana persistida da alocação, não do mestre atual").hasSize(7);
            assertThat(semana.stream().filter(d -> d.diaSemana() == dia.getDayOfWeek()).toList())
                    .singleElement().satisfies(d -> {
                        assertThat(d.diaTrabalho()).isTrue();
                        assertThat(d.minutosPrevistos()).isEqualTo(480);
                    });
            Long gestaoId = alocacao.getLotacao().getGestaoLocal().getId();
            Long seId = alocacao.getLotacao().getGestaoLocal().getSuperintendencia().getId();
            var descansos = em.createQuery("select e from CalendarioEvento e where e.data=:dia "
                            + "and e.cancelado=false and (e.ativo is null or e.ativo=true) "
                            + "and upper(e.tipo) in ('FERIADO','DIA_DESCANSO')", CalendarioEvento.class)
                    .setParameter("dia", dia).getResultList();
            // Pré-condição mais forte que o filtro municipal/lotação: não há
            // fonte de descanso global ou na SE/Gestão exclusiva do cenário.
            assertThat(descansos).as("Pré-condição: calendário persistido permite a FALTA deste cenário")
                    .noneMatch(e -> e.getGestaoLocal() == null && e.getSuperintendencia() == null
                            || e.getGestaoLocal() != null && Objects.equals(gestaoId, e.getGestaoLocal().getId())
                            || e.getSuperintendencia() != null && Objects.equals(seId, e.getSuperintendencia().getId()));
            assertThat(em.createQuery("select count(e) from FrequenciaExcecao e where e.empregado.id=:e "
                            + "and e.dataInicio<=:dia and e.dataFim>=:dia", Long.class)
                    .setParameter("e", empregadoId).setParameter("dia", dia).getSingleResult())
                    .as("Pré-condição: a identidade recém-criada não possui exceção de descanso no dia")
                    .isZero();
            assertThat(em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v "
                            + "and f.dataReferencia=:dia", Long.class)
                    .setParameter("v", vinculoId).setParameter("dia", dia).getSingleResult()).isZero();
            return null;
        });
    }

    private JsonNode registrarFrequencia(Cenario c, long vinculoId, LocalDate dia) throws Exception {
        return enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", vinculoId,
                "dataReferencia", dia, "status", "FALTA", "justificativaRetificacao", "Conferência documental da jornada no respectivo vínculo",
                "chaveIdempotencia", "recontratacao-" + UUID.randomUUID()),
                200, "Frequência por vínculo canônico no respectivo período");
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>(); pagina.path("content").forEach(x -> ids.add(x.path("id").asLong())); return ids;
    }

    private List<String> historia(long vinculoId, long frequenciaId) {
        return emTransacao(() -> {
            List<String> registros = new ArrayList<>();
            for (String jpql : List.of(
                    "select v.id,v.pessoa.id,v.dataInicio,v.dataFim,v.estadoNegocial from VinculoContratual v where v.id=:v",
                    "select a.id,a.dataInicio,a.dataFim,a.estadoNegocial from AlocacaoVinculo a where a.vinculo.id=:v order by a.id",
                    "select e.id,e.hashEvento from EventoVinculoContratual e where e.vinculo.id=:v order by e.id",
                    "select e.id,e.hashEvento from EventoAlocacaoVinculo e where e.vinculo.id=:v order by e.id"))
                em.createQuery(jpql, Object[].class).setParameter("v", vinculoId).getResultList()
                        .stream().map(Arrays::toString).forEach(registros::add);
            Frequencia f = em.find(Frequencia.class, frequenciaId);
            registros.add(f.getId() + "|" + f.getDataReferencia() + "|" + f.getPessoa().getId() + "|"
                    + f.getVinculoContratual().getId() + "|" + f.getAlocacaoVinculo().getId() + "|"
                    + f.getStatus() + "|" + f.getEmpregadoSnapshotNome() + "|" + f.getAutorOriginalId());
            return registros;
        });
    }
}
