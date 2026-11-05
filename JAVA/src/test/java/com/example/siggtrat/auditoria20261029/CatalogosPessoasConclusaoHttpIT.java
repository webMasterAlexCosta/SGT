package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** CAT-007 e COL-010/012: provas novas; nenhuma leitura completa uma fonte histórica ausente. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogosPessoasConclusaoHttpIT extends ContratosHttpBase {
    @Autowired DisponibilidadeCatalogoTemporalServico disponibilidade;

    @Test
    void fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica() throws Exception {
        Cenario c = criarCenarioPersistido();
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                        .contentType(MediaType.APPLICATION_JSON).content(json(mapa("nome", "Troca não excepcional",
                                "cpf", cpf(620000000 + c.pessoaA.getId().intValue()), "email", "opcional@example.test"))),
                200, "COL006 COL009 fluxo comum não troca nome/CPF");
        JsonNode atual = consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(), "COL012 leitura canônica após atualização opcional");
        assertThat(atual.path("nome").asText()).isEqualTo(c.pessoaA.getNome());
        assertThat(atual.path("cpf").asText()).isEqualTo(c.pessoaA.getCpf());
        assertThat(atual.path("email").asText()).isEqualTo("opcional@example.test");
        assertThat(consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade",
                "Fluxo opcional não cria correção de identidade sem ato excepcional")).isEmpty();
    }

    @Test
    void recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo() throws Exception {
        Cenario c = criarCenarioPersistido();
        String cpf = cpf(630000000 + c.pessoaA.getId().intValue());
        JsonNode primeiro = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa recontratada " + c.pessoaA.getId(), "cpf", cpf,
                        "rg", "RG-MANTIDO", "email", "recontratacao@example.test",
                        "endereco", mapa("logradouro", "Rua original", "numero", "17")),
                "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                "dataInicio", DATA, "dataFim", DATA.plusDays(2), "matricula", "M-PRIMEIRO-" + c.pessoaA.getId()),
                201, "COL011 cadastro anterior com campos opcionais comprovados");
        long pessoaId = primeiro.path("pessoaId").asLong();
        JsonNode segundo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoaId", pessoaId,
                        "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                        "dataInicio", DATA.plusDays(3), "matricula", "M-SEGUNDO-" + c.pessoaA.getId()), 201,
                "COL013 VIN004 recontratação no dia consecutivo reutiliza Pessoa sem repetir opcionais");
        assertThat(segundo.path("pessoaId").asLong()).isEqualTo(pessoaId);
        assertThat(segundo.path("id").asLong()).isNotEqualTo(primeiro.path("id").asLong());
        JsonNode atual = consultar(c.localA, "/api/pessoas/" + pessoaId, "COL011 omissão não apaga dados existentes");
        assertThat(atual.path("rg").asText()).isEqualTo("RG-MANTIDO");
        assertThat(atual.path("email").asText()).isEqualTo("recontratacao@example.test");
        assertThat(atual.path("endereco").path("logradouro").asText()).isEqualTo("RUA ORIGINAL");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", cpf).getSingleResult()).isEqualTo(1);
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:id", Long.class)
                    .setParameter("id", pessoaId).getSingleResult()).isEqualTo(2); return null;
        });
    }

    @Test
    void posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento() throws Exception {
        Cenario c = criarCenarioPersistido();
        String nomeAnterior = "Nome registrado antes da correção " + c.pessoaA.getId();
        JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", nomeAnterior, "cpf", cpf(610000000 + c.pessoaA.getId().intValue())),
                "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                "dataInicio", DATA), 201, "COL010 fato novo registra identidade nominal própria");
        long pessoaId = vinculo.path("pessoaId").asLong();
        long vinculoId = vinculo.path("id").asLong();
        JsonNode evento = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/eventos",
                "COL010 fonte documental anterior à correção").get(0);
        Instant conhecidoAntes = Instant.now();
        String nomeAtual = "Nome legal posterior " + pessoaId;
        enviar(c.localA, "/api/pessoas/" + pessoaId + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", nomeAtual, "motivo", "Mudança legal comprovada",
                "evidencia", "SEI-NOME-CONCLUSAO", "autorizacao", "ATO-NOME-CONCLUSAO",
                "impactosEncontrados", "Cadastro atual e projeção operacional; fatos anteriores preservados"), 200,
                "COL009 correção excepcional da mesma Pessoa");
        JsonNode posicao = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + pessoaId
                        + "/posicao?data=" + DATA + "&conhecidoAte=" + conhecidoAntes,
                "COL010 COL012 identidade atual separada da fotografia histórica conhecida");
        assertThat(posicao.path("nomeAtual").asText()).isEqualTo(nomeAtual);
        assertThat(posicao.path("nomeFotografia").asText()).isEqualTo(nomeAnterior);
        assertThat(posicao.path("eventoIdentidadeFonteId").asLong()).isEqualTo(evento.path("id").asLong());
        assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isTrue();
        JsonNode fonteDepois = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/eventos",
                "COL010 correção não altera evento persistido").get(0);
        assertThat(fonteDepois.path("pessoaNomeFotografia").asText()).isEqualTo(nomeAnterior);
        assertThat(fonteDepois.path("hashEvento").asText()).isEqualTo(evento.path("hashEvento").asText());
    }

    @Test
    void posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode posicao = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId()
                        + "/posicao?data=" + DATA,
                "COL010 fonte nominal ausente é desconhecida, separada do cadastro atual");
        assertThat(posicao.path("nomeAtual").asText()).isEqualTo(c.pessoaA.getNome());
        assertThat(posicao.has("nomeFotografia")).isTrue();
        assertThat(posicao.path("nomeFotografia").isNull()).isTrue();
        assertThat(posicao.has("cpfFotografia")).isFalse();
        assertThat(posicao.path("eventoIdentidadeFonteId").isNull()).isTrue();
        assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
    }

    @Test
    void disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = emTransacao(() -> {
            Cargo cargo = new Cargo("Cargo bitemporal " + c.gestaoA.getId(), "1");
            cargo.setGestaoLocal(em.getReference(com.example.siggtrat.entidade.GestaoLocal.class, c.gestaoA.getId()));
            em.persist(cargo); em.flush();
            origem(cargo.getId(), INICIO, true, null, Instant.parse("2026-01-02T12:00:00Z"));
            origem(cargo.getId(), DATA, false, true, Instant.parse("2026-08-02T12:00:00Z"));
            origem(cargo.getId(), DATA.plusDays(10), true, false, Instant.parse("2026-09-02T12:00:00Z"));
            return cargo.getId();
        });
        String rota = "/api/catalogos-temporais/CARGO/" + id + "/disponibilidade?data=" + DATA.plusDays(1);
        JsonNode antesDoConhecimento = consultar(c.localA, rota + "&conhecidoAte=2026-07-01T12:00:00Z",
                "CAT007 ato retroativo ainda desconhecido não muda o estado consultado");
        assertThat(antesDoConhecimento.path("ativo").asBoolean()).isTrue();
        JsonNode depoisDoConhecimento = consultar(c.localA, rota + "&conhecidoAte=2026-08-03T12:00:00Z",
                "CAT007 mesma data efetiva passa a refletir inativação conhecida depois");
        assertThat(depoisDoConhecimento.path("ativo").asBoolean()).isFalse();
        assertThat(depoisDoConhecimento.path("disponibilidadeComprovada").asBoolean()).isTrue();
        JsonNode reativado = consultar(c.localA, "/api/catalogos-temporais/CARGO/" + id
                        + "/disponibilidade?data=" + DATA.plusDays(11) + "&conhecidoAte=2026-09-03T12:00:00Z",
                "CAT006 CAT007 identidade preservada após reativação");
        assertThat(reativado.path("ativo").asBoolean()).isTrue();
        executar(c.localB, get(rota), 404, "CAT007 consulta temporal de catálogo respeita a Gestão proprietária");
        JsonNode desconhecido = consultar(c.localA, "/api/catalogos-temporais/CARGO/" + id
                        + "/disponibilidade?data=2025-12-01&conhecidoAte=2026-09-03T12:00:00Z",
                "CAT007 antes da origem comprovada permanece desconhecido");
        assertThat(desconhecido.path("ativo").isNull()).isTrue();
        assertThat(desconhecido.path("disponibilidadeComprovada").asBoolean()).isFalse();
    }

    @Test
    void estadoAtualOuAnteriorSemMarcoDeOrigemNaoProvaDisponibilidadeRetroativa() {
        Cenario c = criarCenarioPersistido();
        Long id = emTransacao(() -> {
            Cargo cargo = new Cargo("Cargo sem fonte antiga " + c.gestaoA.getId(), "1");
            cargo.setGestaoLocal(em.getReference(com.example.siggtrat.entidade.GestaoLocal.class, c.gestaoA.getId()));
            em.persist(cargo); em.flush(); return cargo.getId();
        });
        assertThatThrownBy(() -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, DATA, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        disponibilidade.registrarTransicao(TipoCatalogoTemporal.CARGO, id, true, false,
                "Primeira transição sem marco documental da origem", c.localA);
        assertThatThrownBy(() -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, DATA, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void primeiraTransicaoConcorrenteTemUmaOrigemERejeitaEstadoAnteriorObsoleto() throws Exception {
        Cenario c = criarCenarioPersistido();
        Long id = emTransacao(() -> {
            MotivoDesligamento motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.getReference(com.example.siggtrat.entidade.GestaoLocal.class, c.gestaoA.getId()));
            motivo.setDescricao("Motivo concorrente " + c.gestaoA.getId());
            motivo.setCategoria(com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.OUTRO);
            em.persist(motivo); em.flush(); return motivo.getId();
        });
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> tarefa = () -> {
                prontas.countDown();
                assertThat(inicio.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    disponibilidade.registrarTransicaoComOrigem(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO,
                            id, true, false, INICIO, "Inativação concorrente comprovada", c.localA);
                    return 200;
                } catch (ResponseStatusException e) { return e.getStatusCode().value(); }
            };
            var a = executor.submit(tarefa); var b = executor.submit(tarefa);
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue(); inicio.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            emTransacao(() -> {
                var eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo "
                                + "and e.catalogoId=:id order by e.dataEfetiva, e.registradoEm, e.id", EventoDisponibilidadeCatalogo.class)
                        .setParameter("tipo", TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO).setParameter("id", id).getResultList();
                assertThat(eventos).hasSize(2);
                assertThat(eventos.get(0).getDataEfetiva()).isEqualTo(INICIO);
                assertThat(eventos.get(0).getAtivo()).isTrue();
                assertThat(eventos.get(1).getDataEfetiva()).isEqualTo(RelogioSistema.hoje());
                assertThat(eventos.get(1).getAtivoAnterior()).isTrue();
                assertThat(eventos.get(1).getAtivo()).isFalse(); return null;
            });
        } finally {
            inicio.countDown(); executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void origem(Long id, LocalDate data, boolean ativo, Boolean anterior, Instant registro) {
        EventoDisponibilidadeCatalogo e = new EventoDisponibilidadeCatalogo();
        e.setTipoCatalogo(TipoCatalogoTemporal.CARGO); e.setCatalogoId(id); e.setDataEfetiva(data);
        e.setAtivo(ativo); e.setAtivoAnterior(anterior); e.setMotivo("Fonte sintética explícita para teste dos dois cortes");
        e.setRegistradoEm(registro); em.persist(e);
    }
}
