package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revisão independente: CON-023, CAT-004/007 e COL-010.
 * Oráculo: §§4.6, 5.1.2, 5.12, 6.5, 6.11, 6.20 e 8.7 da norma.
 * Cada alteração de acervo é DML de fixture, nunca uma recuperação de dados reais.
 * Não há DDL; leitura após confirmação/recusa ocorre em nova transação.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoHistoricaCatalogosIdentidadeHttpIT extends ContratosHttpBase {

    @ParameterizedTest(name = "ativoAtualSemOrigem={0}")
    @ValueSource(booleans = {true, false})
    void disponibilidadeSemOrigemNaoUsaFlagAtualEAceitaFonteRetroativaExplicita(boolean ativoAtual) throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate efeito = n.inicio().plusDays(2);
        emTransacao(() -> {
            removerTrilhaCargo(c.cargoA.getId());
            em.find(Cargo.class, c.cargoA.getId()).setAtivo(ativoAtual);
            return null;
        });
        var desconhecida = consultar(c.localA, disponibilidade(c.cargoA.getId(), efeito),
                "CAT007 flag atual não prova disponibilidade passada");
        assertThat(desconhecida.path("disponibilidadeComprovada").asBoolean()).isFalse();
        assertThat(desconhecida.path("ativo").isNull()).isTrue();
        assertThat(desconhecida.path("eventoFonteId").isNull()).isTrue();

        var req = aditamento("REVISOR-CAT-" + n.id(), efeito);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 6)));
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 409,
                "CAT007 uso retroativo sem prova deve ser recusado");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isZero();
            eventoDisponibilidade(c.cargoA.getId(), n.inicio(), true, null, Instant.now());
            eventoDisponibilidade(c.cargoA.getId(), RelogioSistema.hoje(), false, true, Instant.now());
            em.find(Cargo.class, c.cargoA.getId()).setAtivo(false);
            return null;
        });
        var comprovada = consultar(c.localA, disponibilidade(c.cargoA.getId(), efeito),
                "CAT007 fonte explícita prova atividade passada apesar da inatividade atual");
        assertThat(comprovada.path("disponibilidadeComprovada").asBoolean()).isTrue();
        assertThat(comprovada.path("ativo").asBoolean()).isTrue();
        emTransacao(() -> {
            try { registrarAditamento(c, n.id(), req); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
            return null;
        });
        emTransacao(() -> {
            assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isEqualTo(1L);
            assertThat(em.find(Cargo.class, c.cargoA.getId()).getAtivo()).isFalse();
            return null;
        });
    }

    @Test
    void corteDeConhecimentoDaDisponibilidadeExcluiOrigemRegistradaDepois() throws Exception {
        var c = criarCenarioPersistido();
        Instant corte = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.SECONDS);
        emTransacao(() -> {
            removerTrilhaCargo(c.cargoA.getId());
            eventoDisponibilidade(c.cargoA.getId(), INICIO, true, null, corte.plusSeconds(60));
            em.find(Cargo.class, c.cargoA.getId()).setAtivo(false);
            return null;
        });
        var antes = consultar(c.localA, disponibilidade(c.cargoA.getId(), DATA) + "&conhecidoAte=" + corte,
                "CAT007/TMP005 informação registrada depois não era conhecida");
        assertThat(antes.path("disponibilidadeComprovada").asBoolean()).isFalse();
        assertThat(antes.path("ativo").isNull()).isTrue();
        var depois = consultar(c.localA, disponibilidade(c.cargoA.getId(), DATA) + "&conhecidoAte=" + corte.plusSeconds(90),
                "CAT007 fonte conhecida permite afirmar disponibilidade");
        assertThat(depois.path("disponibilidadeComprovada").asBoolean()).isTrue();
        assertThat(depois.path("ativo").asBoolean()).isTrue();
    }

    @Test
    void transferenciaExigeAutorizacaoEMapaEPreservaContextosAposRenomeacao() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate efeito = RelogioSistema.hoje().minusDays(2);
        var req = transferencia(c, n, efeito);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 403,
                "CON023 autorização administrativa não é substituída pelo texto da requisição");
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var incompleta = mapa(); incompleta.putAll(req); incompleta.put("mapeamentos", List.of());
        enviar(c.localA, base(n.id()) + "/transferir-gestao", incompleta, 400,
                "CON023 contexto posterior precisa de mapa explícito completo");
        conferirAusenciaDeTransferencia(n.id(), c.gestaoA.getId());

        String origem = c.gestaoA.getNome(), destino = c.gestaoB.getNome();
        String cargoOrigem = c.cargoA.getNome(), cargoDestino = c.cargoB.getNome();
        String tipoOrigem = c.tipoA.getDescricao(), tipoDestino = c.tipoB.getDescricao();
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200,
                "CON023 transferência autorizada conserva os dois contextos");
        emTransacao(() -> {
            em.find(GestaoLocal.class, c.gestaoA.getId()).setNome("Origem renomeada " + n.id());
            em.find(GestaoLocal.class, c.gestaoB.getId()).setNome("Destino renomeado " + n.id());
            em.find(Cargo.class, c.cargoA.getId()).setNome("Cargo origem atual " + n.id());
            em.find(Cargo.class, c.cargoB.getId()).setNome("Cargo destino atual " + n.id());
            em.find(TipoContratacao.class, c.tipoA.getId()).setDescricao("Tipo origem atual " + n.id());
            em.find(TipoContratacao.class, c.tipoB.getId()).setDescricao("Tipo destino atual " + n.id());
            return null;
        });
        var t = consultar(c.nacional, base(n.id()) + "/transferencias-gestao", "CAT004 nomes da transferência ficam imutáveis").get(0);
        assertThat(t.path("gestaoOrigemNome").asText()).isEqualTo(origem);
        assertThat(t.path("gestaoDestinoNome").asText()).isEqualTo(destino);
        assertThat(t.path("tipoContratacaoDestinoDescricao").asText()).isEqualTo(tipoDestino);
        assertThat(t.path("mapeamentos").get(0).path("cargoOrigemNome").asText()).isEqualTo(cargoOrigem);
        assertThat(t.path("mapeamentos").get(0).path("cargoDestinoNome").asText()).isEqualTo(cargoDestino);
        var antes = consultar(c.nacional, posicao(n.id(), efeito.minusDays(1)), "CON023 contexto anterior preservado")
                .path("versaoContratual");
        var depois = consultar(c.nacional, posicao(n.id(), efeito), "CON023 novo contexto somente desde a transferência")
                .path("versaoContratual");
        assertThat(antes.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(antes.path("tipoContratacaoDescricao").asText()).isEqualTo(tipoOrigem);
        assertThat(depois.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
        assertThat(depois.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoB.getId());
        assertThat(depois.path("tipoContratacaoDescricao").asText()).isEqualTo(tipoDestino);
        assertThat(depois.path("tipoContratacaoComprovado").asBoolean()).isTrue();
    }

    @Test
    void transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate efeito = n.inicio().plusDays(10);
        String nomeDaEpoca = consultar(c.nacional, posicao(n.id(), efeito.minusDays(1)),
                "CAT004 fonte publicada anterior à transferência").path("versaoContratual").path("gestaoLocalNome").asText();
        emTransacao(() -> {
            em.find(GestaoLocal.class, c.gestaoA.getId()).setNome("Nome adotado hoje, depois do período " + n.id());
            return null;
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", transferencia(c, n, efeito), 200,
                "CON023 inserção retroativa mantém contexto histórico comprovado da origem");
        var t = consultar(c.nacional, base(n.id()) + "/transferencias-gestao", "CAT004 nome da origem na época do ato").get(0);
        assertThat(t.path("gestaoOrigemNome").asText()).as("O ato é retroativo; a fonte nominal da origem já existia")
                .isEqualTo(nomeDaEpoca);
    }

    @Test
    void atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate inicio = n.inicio().plusDays(1), recorte = inicio.plusDays(2);
        String antigo = "NOME ORIGINAL DO PERIODO " + n.id();
        String novo = "NOME LEGAL ADOTADO HOJE " + n.id();
        var v = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "dataInicio", inicio,
                        "matricula", "REV-ORIGINAL-" + n.id(), "composicaoId", n.composicao1(),
                        "pessoa", mapa("nome", antigo, "cpf", cpf(700000000 + (int) (n.id() % 100000000)))),
                201, "COL010 fonte nominal original criada pelo fluxo canônico");
        long vinculoId = v.path("id").asLong(), pessoaId = v.path("pessoaId").asLong();
        corrigirNome(c, pessoaId, novo);
        String rota = "/api/vinculos-contratuais/pessoas/" + pessoaId + "/posicao?data=" + recorte;
        var antes = consultar(c.localA, rota, "COL010 cadastro atual muda, fotografia anterior permanece");
        assertThat(antes.path("nomeAtual").asText()).isEqualTo(novo);
        assertThat(antes.path("nomeFotografia").asText()).isEqualTo(antigo);

        enviar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/alterar-matricula",
                mapa("dataEfetiva", inicio.plusDays(1), "novaMatricula", "REV-CORRIGIDA-" + n.id(),
                        "justificativa", "Ato tardio altera somente matrícula; nome não foi alterado naquela época",
                        "evidencia", "SEI-MATRICULA-DO-PERIODO", "autorizacao", "ATO-MATRICULA"), 200,
                "COL010 alteração retroativa de matrícula não é retificação nominal do passado");
        var depois = consultar(c.localA, rota, "COL010 consulta do mesmo período conserva a identidade nominal comprovada");
        assertThat(depois.path("nomeAtual").asText()).isEqualTo(novo);
        assertThat(depois.path("nomeFotografia").asText()).as("Evento de matrícula não redefine nome de período anterior")
                .isEqualTo(antigo);
        emTransacao(() -> {
            var criacao = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id "
                            + "and e.tipoEvento=com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO",
                            EventoVinculoContratual.class).setParameter("id", vinculoId).getSingleResult();
            assertThat(criacao.getPessoaNomeFotografia()).isEqualTo(antigo);
            assertThat(em.find(Pessoa.class, pessoaId).getNome()).isEqualTo(novo);
            return null;
        });
    }

    @Test
    void ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual() throws Exception {
        var c = criarCenarioPersistido();
        String novo = "NOME ATUAL SEM FONTE HISTORICA " + c.pessoaA.getId();
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult()).isZero();
            return null;
        });
        corrigirNome(c, c.pessoaA.getId(), novo);
        var posicao = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId() + "/posicao?data=" + DATA,
                "COL010 ausência de fonte nominal não autoriza uso retroativo do nome atual");
        assertThat(posicao.path("nomeAtual").asText()).isEqualTo(novo);
        assertThat(posicao.path("nomeFotografia").isNull()).isTrue();
        assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
        assertThat(posicao.path("eventoIdentidadeFonteId").isNull()).isTrue();
    }

    private void corrigirNome(Cenario c, long pessoaId, String nome) throws Exception {
        enviar(c.localA, "/api/pessoas/" + pessoaId + "/correcoes-identidade",
                mapa("tipo", "NOME", "novoValor", nome,
                        "motivo", "Mudança legal ocorrida hoje; não altera o nome usado em períodos anteriores",
                        "evidencia", "SEI-MUDANCA-LEGAL-HOJE", "autorizacao", "ATO-NOMINAL-HOJE",
                        "impactosEncontrados", "Atualização do cadastro atual; história anterior preservada"),
                200, "COL010 mudança legal atual preserva o nome efetivamente registrado no passado");
    }

    private void conferirAusenciaDeTransferencia(long id, long gestaoOrigem) {
        emTransacao(() -> {
            assertThat(em.createQuery("select count(t) from TransferenciaGestaoContrato t where t.contrato.id=:id", Long.class)
                    .setParameter("id", id).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                    .setParameter("id", id).getSingleResult()).isEqualTo(1L);
            assertThat(em.find(Contrato.class, id).getGestaoLocal().getId()).isEqualTo(gestaoOrigem);
            return null;
        });
    }

    private Map<String, Object> transferencia(Cenario c, NovoContrato n, LocalDate efeito) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", efeito, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Reorganização documentada do contexto operacional",
                "referenciaSei", "SEI-REVISOR-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
    }

    private void removerTrilhaCargo(long id) {
        em.createQuery("delete from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id")
                .setParameter("tipo", TipoCatalogoTemporal.CARGO).setParameter("id", id).executeUpdate();
    }

    private void eventoDisponibilidade(long id, LocalDate efeito, boolean ativo, Boolean anterior, Instant registrado) {
        var evento = new EventoDisponibilidadeCatalogo(); evento.setTipoCatalogo(TipoCatalogoTemporal.CARGO);
        evento.setCatalogoId(id); evento.setDataEfetiva(efeito); evento.setAtivo(ativo); evento.setAtivoAnterior(anterior);
        evento.setRegistradoEm(registrado); evento.setMotivo("Fonte sintética expressa do cenário independente");
        em.persist(evento);
    }

    private NovoContrato publicarPersistido(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private String disponibilidade(long cargo, LocalDate data) {
        return "/api/catalogos-temporais/CARGO/" + cargo + "/disponibilidade?data=" + data;
    }
    private String posicao(long id, LocalDate data) { return base(id) + "/posicao-temporal?data=" + data; }
}
