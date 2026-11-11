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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/** CON023/ALO003/ALO007/HIS005: transferências corrigidas reconstroem faixas sem apagar decisões anteriores. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoTransferenciaAlocacaoHttpIT extends ContratosHttpBase {
    @Test
    void catalogoDaOrigemSemUsoFuturoPodeSerInativadoAposTransferenciaEfetiva() throws Exception {
        Preparado f = preparar(false); transferir(f);
        executar(f.c().localA, delete("/api/cargos/" + f.c().cargoA.getId()), 204,
                "CAT005 composição histórica de origem não é programação operacional futura após transferência");
        assertThat(posicao(f, f.dataTransferencia().minusDays(1), null).path("cargoId").asLong())
                .isEqualTo(f.c().cargoA.getId());
        assertThat(posicao(f, RelogioSistema.hoje().plusDays(1), null).path("cargoId").asLong())
                .isEqualTo(f.c().cargoB.getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean futura) throws Exception {
        Preparado f = preparar(futura);
        JsonNode original = transferir(f);
        long eventoOriginal = original.path("eventoId").asLong();
        if (eventoOriginal == 0) eventoOriginal = eventoTransferencia(f);
        LocalDate posterior = f.dataTransferencia().plusDays(3);
        long alocacaoPosterior = alterarAlocacao(f, posterior);
        Instant conhecimentoAnterior = Instant.now();
        UsuarioAplicacao executor = futura ? f.c().localA : f.c().localB;
        if (!futura) concederDestino(f);
        Map<String, Object> req = transferencia(f, f.dataTransferencia().plusDays(1));
        req.put("eventoPrecedenteId", eventoOriginal); req.put("relacaoPrecedencia", "RETIFICA");
        req.put("motivo", "Correção documental da data de transferência preservando decisão de alocação posterior");
        if (!futura) {
            var previa = enviar(executor, base(f.n().id()) + "/transferencias-gestao/previa", req, 200,
                    "AUD004 prévia da retificação retroativa com alocação posterior independente");
            req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        }
        enviar(executor, base(f.n().id()) + "/transferir-gestao", req, 200,
                "ALO003 CON023 retificação reconstitui a origem e reaplica somente a transferência vigente");

        assertThat(posicao(f, f.dataTransferencia(), null).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        JsonNode destino = posicao(f, f.dataTransferencia().plusDays(1), null);
        assertThat(destino.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
        assertThat(destino.path("cargoId").asLong()).isEqualTo(f.c().cargoB.getId());
        JsonNode mantida = posicao(f, posterior, null);
        assertThat(mantida.path("alocacaoId").asLong()).isEqualTo(alocacaoPosterior);
        assertThat(mantida.path("cargoId").asLong()).isEqualTo(f.cargoDestino2());
        assertThat(posicao(f, f.dataTransferencia(), conhecimentoAnterior).path("lotacaoId").asLong())
                .isEqualTo(f.c().lotacaoB.getId());
        assertThat(contar(f, "VinculoContratual")).isEqualTo(1L);
        JsonNode trilha = consultar(f.c().nacional, "/api/vinculos-contratuais/" + f.vinculoId() + "/alocacoes/eventos",
                "HIS005 correção acrescenta fatos e preserva os anteriores");
        assertThat(trilha.toString()).contains("TRANSFERENCIA_GESTAO", "REPROCESSAMENTO_TRANSFERENCIA");
        emTransacao(() -> {
            assertThat(em.find(AlocacaoVinculo.class, alocacaoPosterior).getCargo().getId()).isEqualTo(f.cargoDestino2());
            assertThat(em.find(VinculoContratual.class, f.vinculoId()).getPessoa().getId()).isEqualTo(f.pessoaId());
            return null;
        });
    }

    @Test
    void cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico() throws Exception {
        Preparado f = preparar(true);
        transferir(f); long transferenciaId = transferenciaId(f);
        Instant conhecimentoAnterior = Instant.now();
        long faixasAntes = contarFaixas(f);
        enviar(f.c().localA, cancelarRota(f, transferenciaId), cancelamento(f), 200,
                "ALO007 cancelamento futuro recompõe a faixa da Gestão de origem");
        assertThat(posicao(f, f.dataTransferencia(), null).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(posicao(f, f.n().fim(), null).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(posicao(f, f.dataTransferencia(), conhecimentoAnterior).path("lotacaoId").asLong())
                .isEqualTo(f.c().lotacaoB.getId());
        assertThat(contarFaixas(f)).isGreaterThanOrEqualTo(faixasAntes);
        assertThat(contar(f, "VinculoContratual")).isEqualTo(1L);
        emTransacao(() -> {
            assertThat(em.find(TransferenciaGestaoContrato.class, transferenciaId).getEvento().getSituacaoFato().name())
                    .isEqualTo("CANCELADO"); return null;
        });
    }

    @Test
    void cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral() throws Exception {
        Preparado f = preparar(true);
        transferir(f); long transferenciaId = transferenciaId(f);
        long alocacaoPosterior = alterarAlocacao(f, f.dataTransferencia().plusDays(3));
        long eventosContrato = contar(f, "EventoCicloVidaContrato");
        long eventosAlocacao = contarEventosAlocacao(f);
        enviar(f.c().localA, cancelarRota(f, transferenciaId), cancelamento(f), 409,
                "ALO003 decisão independente da Gestão B precisa de tratamento antes de cancelar sua transferência");
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(eventosContrato);
        assertThat(contarEventosAlocacao(f)).isEqualTo(eventosAlocacao);
        assertThat(posicao(f, f.dataTransferencia().plusDays(3), null).path("alocacaoId").asLong())
                .isEqualTo(alocacaoPosterior);
        emTransacao(() -> {
            assertThat(em.find(TransferenciaGestaoContrato.class, transferenciaId).getEvento().getSituacaoFato().name())
                    .isEqualTo("VIGENTE"); return null;
        });
    }

    private Preparado preparar(boolean futura) throws Exception {
        Cenario c = criarCenarioPersistido();
        // Este Cargo pertence somente ao contrato desta prova; o cenário de apoio
        // não deve introduzir outra programação futura sobre a origem testada.
        c.cargoA = emTransacao(() -> {
            Cargo cargo = new Cargo("Origem reprojeção " + c.gestaoA.getId(), "1");
            cargo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(cargo.getId());
            origem.setDataEfetiva(RelogioSistema.hoje().minusDays(30)); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita: origem válida desde o início contratual"); em.persist(origem);
            return cargo;
        });
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, 5, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long cargoDestino2 = emTransacao(() -> {
            Cargo cargo = new Cargo("Destino independente " + n.id(), "2");
            cargo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoB.getId()));
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(cargo.getId());
            origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita: cargo válido desde o início do contrato"); em.persist(origem);
            return cargo.getId();
        });
        JsonNode v = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa reprojeção " + n.id(), "cpf", cpf(640000000 + Math.toIntExact(n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", n.inicio()),
                201, "VIN001 início canônico anterior à transferência");
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        return new Preparado(c, n, v.path("id").asLong(), v.path("pessoaId").asLong(), cargoDestino2,
                RelogioSistema.hoje().plusDays(futura ? 5 : -10));
    }

    private void concederDestino(Preparado f) throws Exception {
        enviar(f.c().admin, "/api/permissoes-excepcionais", mapa("tipo", "TRANSFERENCIA_GESTAO_LOCAL",
                "usuarioBeneficiarioId", f.c().localB.getId(), "contratoId", f.n().id(),
                "finalidade", "Retificação documentada da transferência", "justificativa", "Ato documental conferido",
                "autorizacaoAdministrativa", "ATO-HTTP", "validoAte", Instant.now().plusSeconds(7200)), 201,
                "Gestor responsável atual recebe autorização específica para retificar");
    }

    private JsonNode transferir(Preparado f) throws Exception {
        return enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", transferencia(f, f.dataTransferencia()),
                200, "CON023 mapa explícito preserva as duas combinações e suas alocações");
    }

    private Map<String, Object> transferencia(Preparado f, LocalDate data) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", f.c().tipoB.getId(), "motivo", "Transferência documentada com duas combinações",
                "referenciaSei", "SEI-TRANSFERENCIA-ALOCACAO", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                                "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId()),
                        mapa("composicaoOrigemId", f.n().composicao2(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                                "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.cargoDestino2())));
    }

    private long alterarAlocacao(Preparado f, LocalDate data) throws Exception {
        UsuarioAplicacao executor = f.dataTransferencia().isAfter(RelogioSistema.hoje()) ? f.c().localA : f.c().localB;
        Map<String, Object> req = mapa("dataEfetiva", data, "composicaoId", f.n().composicao2(),
                "justificativa", "Decisão independente posterior da alocação", "evidencia", "SEI-ALOCACAO-POSTERIOR",
                "autorizacao", "ATO-ALOCACAO-POSTERIOR");
        JsonNode previa = enviar(executor, "/api/vinculos-contratuais/" + f.vinculoId() + "/previas/alocacoes",
                req, 200, "ALO003 prévia da mudança independente posterior");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        return enviar(executor, "/api/vinculos-contratuais/" + f.vinculoId() + "/alocacoes", req, 200,
                "ALO002 decisão posterior troca Cargo sem trocar Pessoa/Vínculo").path("id").asLong();
    }

    private JsonNode posicao(Preparado f, LocalDate data, Instant conhecido) throws Exception {
        return consultar(f.c().nacional, "/api/vinculos-contratuais/" + f.vinculoId() + "/alocacoes/posicao?data=" + data
                        + (conhecido == null ? "" : "&conhecidoAte=" + conhecido),
                "HIS005 posição temporal de Alocação nos cortes efetivo/conhecimento");
    }
    private Map<String, Object> cancelamento(Preparado f) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", f.dataTransferencia(), "dataAto", RelogioSistema.hoje(),
                "motivo", "Reorganização futura não ocorrerá", "referenciaSei", "SEI-CANCELA-TRANSFERENCIA",
                "autorizacaoAdministrativa", "ATO-HTTP");
    }
    private String cancelarRota(Preparado f, long id) { return base(f.n().id()) + "/transferencias-gestao/" + id + "/cancelar"; }
    private long transferenciaId(Preparado f) { return emTransacao(() -> em.createQuery("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:id", Long.class)
            .setParameter("id", f.n().id()).getSingleResult()); }
    private long eventoTransferencia(Preparado f) { return emTransacao(() -> em.find(TransferenciaGestaoContrato.class, transferenciaId(f)).getEvento().getId()); }
    private long contar(Preparado f, String entidade) { return emTransacao(() -> em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:id", Long.class)
            .setParameter("id", f.n().id()).getSingleResult()); }
    private long contarFaixas(Preparado f) { return emTransacao(() -> em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:id", Long.class)
            .setParameter("id", f.vinculoId()).getSingleResult()); }
    private long contarEventosAlocacao(Preparado f) { return emTransacao(() -> em.createQuery("select count(a) from EventoAlocacaoVinculo a where a.vinculo.id=:id", Long.class)
            .setParameter("id", f.vinculoId()).getSingleResult()); }
    private record Preparado(Cenario c, NovoContrato n, long vinculoId, long pessoaId,
                             long cargoDestino2, LocalDate dataTransferencia) {}
}
