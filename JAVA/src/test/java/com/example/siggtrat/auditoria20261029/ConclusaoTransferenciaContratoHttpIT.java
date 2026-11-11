package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** CON-023 / TMP-008/012/014/016: fatos persistidos e relidos em transações independentes. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoTransferenciaContratoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSource;
    @Test
    void retificarTransferenciaProgramadaReconstroiOrigemEReaplicaAditamentoPosterior() throws Exception {
        var f = preparar();
        LocalDate original = f.n().inicio().plusDays(2);
        transferir(f, original);
        var anterior = transferencia(f.n().id());
        var adi = aditamento("ADI-POSTERIOR-A-RETIFICACAO", original.plusDays(5));
        adi.put("novaDescricao", "Descrição posterior independente preservada");
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        Instant conhecidoAntes = Instant.now();

        var corrigir = requisicao(f, original.plusDays(1));
        corrigir.put("eventoPrecedenteId", anterior.evento());
        corrigir.put("relacaoPrecedencia", "RETIFICA");
        corrigir.put("motivo", "Documento corrigiu a data de responsabilidade para o dia seguinte");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", corrigir, 200,
                "CON023 retificação usa origem anterior ao ato substituído, sem tratar B como origem de si mesma");

        assertThat(foto(f, original).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoA.getId());
        assertThat(foto(f, original.plusDays(1)).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        var posterior = foto(f, original.plusDays(5));
        assertThat(posterior.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        assertThat(posterior.path("tipoContratacaoId").asLong()).isEqualTo(f.c().tipoB.getId());
        assertThat(posterior.path("descricao").asText()).isEqualTo("Descrição posterior independente preservada");
        assertThat(consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + original
                + "&conhecidoAte=" + conhecidoAntes, "TMP005 conhecimento anterior mantém o ato então válido")
                .path("versaoContratual").path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        assertThat(contar(f.n().id(), "TransferenciaGestaoContrato")).isEqualTo(2L);
        assertThat(contar(f.n().id(), "AditamentoContratual")).isEqualTo(1L);
        assertThat(emTransacao(() -> em.find(TransferenciaGestaoContrato.class, anterior.id())
                .getEvento().getSituacaoFato().name())).isEqualTo("RETIFICADO");
    }

    @Test
    void cancelarTransferenciaFuturaPreservaOriginalEPosteriorSemEfeitoDeResponsabilidade() throws Exception {
        var f = preparar();
        LocalDate efeito = f.n().inicio().plusDays(2);
        transferir(f, efeito);
        var original = transferencia(f.n().id());
        var adi = aditamento("ADI-INDEPENDENTE-DA-TRANSFERENCIA", efeito.plusDays(3));
        adi.put("novaDescricao", "Ato posterior que continua válido");
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });

        enviar(f.c().localA, cancelamento(f, original), cancelar(efeito), 200,
                "CON023/GOV003 cancelamento explícito retira o efeito e conserva o ato original");

        var posicao = foto(f, efeito.plusDays(3));
        assertThat(posicao.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoA.getId());
        assertThat(posicao.path("tipoContratacaoId").asLong()).isEqualTo(f.c().tipoA.getId());
        assertThat(posicao.path("descricao").asText()).isEqualTo("Ato posterior que continua válido");
        var composicoes = consultar(f.c().nacional, base(f.n().id()) + "/composicoes?data=" + efeito.plusDays(3),
                "CON023 cancelamento restitui o contexto anterior sem apagar o mapa");
        assertThat(composicoes.get(0).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(contar(f.n().id(), "TransferenciaGestaoContrato")).isEqualTo(1L);
        assertThat(emTransacao(() -> em.find(TransferenciaGestaoContrato.class, original.id())
                .getEvento().getSituacaoFato().name())).isEqualTo("CANCELADO");
        assertThat(contar(f.n().id(), "AditamentoContratual")).isEqualTo(1L);
    }

    @Test
    void cancelarTransferenciaComTipoPosteriorIncompativelFazRollbackIntegral() throws Exception {
        var f = preparar();
        LocalDate efeito = f.n().inicio().plusDays(2);
        transferir(f, efeito);
        var original = transferencia(f.n().id());
        var adi = aditamento("ADI-TIPO-DESTINO-DEPENDENTE", efeito.plusDays(3));
        adi.put("novoTipoContratacaoId", f.c().tipoB.getId());
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        long eventosAntes = contar(f.n().id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(f.n().id(), "VersaoContratoNegocial");

        enviar(f.c().localA, cancelamento(f, original), cancelar(efeito), 409,
                "CON023/TMP009 posterior com Tipo de B impede cancelar responsabilidade B sem tratamento");

        assertThat(contar(f.n().id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(f.n().id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(emTransacao(() -> em.find(TransferenciaGestaoContrato.class, original.id())
                .getEvento().getSituacaoFato().name())).isEqualTo("VIGENTE");
        assertThat(foto(f, efeito.plusDays(3)).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
    }

    @Test
    void transferenciaPodeUsarVigenciaProrrogadaPorAditamentoFuturo() throws Exception {
        var f = preparar();
        var adi = aditamento("ADI-PRORROGA-ANTES-DE-TRANSFERIR", f.n().inicio().plusDays(1));
        adi.put("novaDataFim", f.n().fim().plusDays(30));
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        LocalDate efeito = f.n().fim().plusDays(2);
        transferir(f, efeito);
        assertThat(foto(f, efeito).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        assertThat(foto(f, efeito).path("dataFim").asText()).isEqualTo(f.n().fim().plusDays(30).toString());
    }

    @Test
    void transferenciaForaDaVigenciaEncurtadaPorAditamentoFuturoNaoPersiste() throws Exception {
        var f = preparar();
        var adi = aditamento("ADI-ENCURTA-ANTES-DE-TRANSFERIR", f.n().inicio().plusDays(1));
        adi.put("novaDataFim", f.n().inicio().plusDays(10));
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        long eventosAntes = contar(f.n().id(), "EventoCicloVidaContrato");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f, f.n().inicio().plusDays(11)), 400,
                "TMP014 transferência respeita a vigência reconstruída na data, não o fim materializado de hoje");
        assertThat(contar(f.n().id(), "TransferenciaGestaoContrato")).isZero();
        assertThat(contar(f.n().id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
    }

    @Test
    void transferenciaRecusaComposicaoPosteriorAindaReferenteAGestaoOrigem() throws Exception {
        var f = preparar();
        LocalDate efeito = f.n().inicio().plusDays(2);
        long cargoPosterior = emTransacao(() -> {
            var cargo = new com.example.siggtrat.entidade.Cargo(); cargo.setGestaoLocal(f.c().gestaoA);
            cargo.setNome("Cargo posterior explícito " + java.util.UUID.randomUUID()); cargo.setAtivo(true);
            em.persist(cargo); em.flush(); return cargo.getId();
        });
        var adi = aditamento("ADI-RECORTE-A-POSTERIOR", efeito.plusDays(3));
        adi.put("novoEfetivoTotal", 15);
        adi.put("alteracoesComposicao", List.of(mapa("lotacaoId", f.c().lotacaoA.getId(),
                "jornadaId", f.c().jornadaA.getId(), "cargoId", cargoPosterior, "quantidadeResultante", 5)));
        emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        long eventosAntes = contar(f.n().id(), "EventoCicloVidaContrato");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f, efeito), 409,
                "CON023 mapa da data de transferência não autoriza manter nova composição posterior de A sob Gestão B");
        assertThat(contar(f.n().id(), "TransferenciaGestaoContrato")).isZero();
        assertThat(contar(f.n().id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(foto(f, efeito.plusDays(3)).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoA.getId());
        assertThat(contar(f.n().id(), "AditamentoContratual")).isEqualTo(1L);
    }

    @Test
    void encerramentoAntecipadoUsaVigenciaReconstruidaProrrogadaPorAtoFuturo() throws Exception {
        var f = preparar();
        emTransacao(() -> {
            var motivo = new com.example.siggtrat.entidade.MotivoDesligamento();
            motivo.setGestaoLocal(em.getReference(com.example.siggtrat.entidade.GestaoLocal.class, f.c().gestaoA.getId()));
            motivo.setDescricao("Rescisão documentada específica " + f.n().id());
            motivo.setCategoria(com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
            motivo.setCriadoPorId(f.c().localA.getId()); motivo.setCriadoPorUsername(f.c().localA.getUsername());
            em.persist(motivo); return null;
        });
        var adi = aditamento("ADI-PRORROGA-ANTES-DA-RESCISAO", f.n().inicio().plusDays(1));
        adi.put("novaDataFim", f.n().fim().plusDays(30));
        JsonNode adiRegistrado=emTransacao(() -> { try { return registrarAditamento(f.c(), f.n().id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        long adiId=adiRegistrado.path("id").asLong(),eventoAdi=adiRegistrado.path("eventoId").asLong();
        JsonNode instrumentoAntes=consultar(f.c().nacional,base(f.n().id())+"/aditamentos","CON016 ADI exato antes da rescisão").get(0).deepCopy();
        assertThat(instrumentoAntes.path("id").asLong()).isEqualTo(adiId);
        assertThat(instrumentoAntes.path("eventoId").asLong()).isEqualTo(eventoAdi);
        assertThat(instrumentoAntes.path("identificacao").asText()).isEqualTo("ADI-PRORROGA-ANTES-DA-RESCISAO");
        assertThat(instrumentoAntes.path("novaDataFim").asText()).isEqualTo(f.n().fim().plusDays(30).toString());
        var prorrogado=consultar(f.c().nacional,base(f.n().id())+"/posicao-temporal?data="+f.n().fim().plusDays(15),
                "CON016 ADI futuro cobre a data que depois será objeto de rescisão");
        assertThat(prorrogado.path("contratoId").asLong()).isEqualTo(f.n().id());
        assertThat(prorrogado.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(prorrogado.path("vigenciaFim").asText()).isEqualTo(f.n().fim().plusDays(30).toString());
        assertThat(prorrogado.path("versaoContratual").path("dataFim").asText()).isEqualTo(f.n().fim().plusDays(30).toString());
        LocalDate efeito = f.n().fim().plusDays(15);
        var req = mapa("dataEfetiva", efeito, "dataAto", RelogioSistema.hoje(), "motivo", "Rescisão no período prorrogado",
                "referenciaSei", "SEI-RESCISAO-PRORROGADA");
        var previa = enviar(f.c().localA, base(f.n().id()) + "/previas/encerrar-antecipadamente", req, 200,
                "CON016 prévia de encerramento considera os fatos contratuais programados");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(f.c().localA, base(f.n().id()) + "/encerrar-antecipadamente", req, 200,
                "CON016/TMP014 término reduz a vigência reconstruída, ainda que posterior ao fim materializado atual");
        var encerrado = consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + efeito.plusDays(1),
                "CON016 rescisão torna dia seguinte temporalmente encerrado");
        assertThat(encerrado.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
        assertThat(encerrado.path("vigenciaFim").asText()).isEqualTo(efeito.toString());
        assertThat(contar(f.n().id(), "AditamentoContratual")).isEqualTo(1L);
        JsonNode instrumentosDepois=consultar(f.c().nacional,base(f.n().id())+"/aditamentos","CON016 rescisão não remove nem reescreve ADI");
        assertThat(instrumentosDepois).hasSize(1);
        assertThat(instrumentosDepois.get(0)).isEqualTo(instrumentoAntes);
        emTransacao(() -> {
            var persistido=em.find(com.example.siggtrat.entidade.AditamentoContratual.class,adiId);
            assertThat(persistido.getContrato().getId()).isEqualTo(f.n().id());
            assertThat(persistido.getEvento().getId()).isEqualTo(eventoAdi);
            assertThat(persistido.getIdentificacao()).isEqualTo("ADI-PRORROGA-ANTES-DA-RESCISAO");
            assertThat(persistido.getNovaDataFim()).isEqualTo(f.n().fim().plusDays(30));return null;
        });
    }

    @Test
    void duasTransferenciasConflitantesConcorrentesNaoPublicamDuasCadeias() throws Exception {
        var f = preparar();
        LocalDate data = f.n().inicio().plusDays(2);
        var eventosAntes = consultar(f.c().nacional, base(f.n().id()) + "/eventos", "CON023 origem publicada antes da disputa");
        assertThat(eventosAntes).hasSize(1);
        long eventoOrigemId = eventosAntes.get(0).path("id").asLong();
        String corpo = json(requisicao(f, data));
        var primeira = autenticar(post(base(f.n().id()) + "/transferir-gestao")
                .contentType(MediaType.APPLICATION_JSON).content(corpo), f.c().localA);
        var segunda = autenticar(post(base(f.n().id()) + "/transferir-gestao")
                .contentType(MediaType.APPLICATION_JSON).content(corpo), f.c().localA);
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(3);
        var liberarContrato = new CountDownLatch(1);
        var bloqueadorPronto = new java.util.concurrent.CompletableFuture<Long>();
        var atorA = new Ator("transferencia-A"); var atorB = new Ator("transferencia-B");
        try {
            var bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(com.example.siggtrat.entidade.Contrato.class, f.n().id(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                bloqueadorPronto.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(25, TimeUnit.SECONDS)) throw new AssertionError("CON023 fixture não liberou o Contrato");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return null;
            }));
            long raiz = bloqueadorPronto.get(10, TimeUnit.SECONDS);
            var a = pool.submit(() -> atorA.executar(() -> { inicio.await(); return mvc.perform(primeira).andReturn().getResponse().getStatus(); }));
            var b = pool.submit(() -> atorB.executar(() -> { inicio.await(); return mvc.perform(segunda).andReturn().getResponse().getStatus(); }));
            inicio.countDown();
            var esperas = EsperaJdbcPostgres.aguardarAmbos(new JdbcTemplate(dataSource), raiz,
                    atorA, sql -> sql.selecaoChave("contrato", "id_contrato", f.n().id()),
                    atorB, sql -> sql.selecaoChave("contrato", "id_contrato", f.n().id()),
                    "CON023 transferência/Contrato " + f.n().id(), a, b);
            assertThat(esperas).isNotEmpty();
            assertThat(a.isDone()).isFalse(); assertThat(b.isDone()).isFalse();
            liberarContrato.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            var resultados = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(resultados.stream().filter(status -> status == 200).count()).isEqualTo(1L);
            assertThat(resultados).allMatch(status -> status == 200 || status == 400 || status == 409);
        } finally {
            inicio.countDown(); liberarContrato.countDown(); pool.shutdownNow();
        }
        assertThat(contar(f.n().id(), "TransferenciaGestaoContrato")).isEqualTo(1L);
        assertThat(contar(f.n().id(), "EventoCicloVidaContrato")).isEqualTo(2L);
        var transferida = transferencia(f.n().id());
        var eventosDepois = consultar(f.c().nacional, base(f.n().id()) + "/eventos", "CON023 cadeia por IDs e relações, sem fonte parcial");
        var porId = java.util.stream.StreamSupport.stream(eventosDepois.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(e -> e.path("id").asLong(), e -> e));
        assertThat(porId.keySet()).containsExactlyInAnyOrder(eventoOrigemId, transferida.evento());
        assertThat(porId.get(eventoOrigemId)).isEqualTo(eventosAntes.get(0));
        emTransacao(() -> {
            var t = em.find(TransferenciaGestaoContrato.class, transferida.id());
            assertThat(t.getContrato().getId()).isEqualTo(f.n().id());
            assertThat(t.getEvento().getId()).isEqualTo(transferida.evento());
            assertThat(t.getEvento().getContrato().getId()).isEqualTo(f.n().id());
            assertThat(t.getEvento().getTipoEvento().name()).isEqualTo("TRANSFERENCIA_GESTAO_LOCAL");
            var mapas = em.createQuery("select m from MapeamentoTransferenciaGestao m where m.transferencia.id=:id",
                    com.example.siggtrat.entidade.MapeamentoTransferenciaGestao.class).setParameter("id", t.getId()).getResultList();
            assertThat(mapas).hasSize(1);
            assertThat(mapas.get(0).getComposicaoOrigem().getId()).isEqualTo(f.n().composicao1());
            assertThat(mapas.get(0).getTransferencia().getEvento().getId()).isEqualTo(transferida.evento());
            assertThat(mapas.get(0).getLotacaoDestino().getId()).isEqualTo(f.c().lotacaoB.getId());
            assertThat(mapas.get(0).getJornadaDestino().getId()).isEqualTo(f.c().jornadaB.getId());
            assertThat(mapas.get(0).getCargoDestino().getId()).isEqualTo(f.c().cargoB.getId());
            return null;
        });
        assertThat(foto(f, data).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
    }

    private Preparado preparar() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 10, null, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        return new Preparado(c, n);
    }

    private void transferir(Preparado f, LocalDate data) throws Exception {
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", requisicao(f, data), 200,
                "CON023 transferência válida com Tipo, contexto e autorização específicos");
    }

    private Map<String, Object> requisicao(Preparado f, LocalDate data) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", f.c().tipoB.getId(), "motivo", "Reorganização administrativa documentada",
                "referenciaSei", "SEI-CONCLUSAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(),
                        "lotacaoDestinoId", f.c().lotacaoB.getId(), "jornadaDestinoId", f.c().jornadaB.getId(),
                        "cargoDestinoId", f.c().cargoB.getId())));
    }

    private Map<String, Object> cancelar(LocalDate data) {
        return mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "motivo", "Ato futuro revogado expressamente",
                "referenciaSei", "SEI-CANCELAMENTO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP");
    }

    private String cancelamento(Preparado f, Transferencia original) {
        return base(f.n().id()) + "/transferencias-gestao/" + original.id() + "/cancelar";
    }

    private JsonNode foto(Preparado f, LocalDate data) throws Exception {
        return consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + data,
                "HIS001 posição contratual completa no corte efetivo").path("versaoContratual");
    }

    private Transferencia transferencia(long contratoId) {
        return emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:id", TransferenciaGestaoContrato.class)
                    .setParameter("id", contratoId).getSingleResult();
            return new Transferencia(t.getId(), t.getEvento().getId());
        });
    }

    private long contar(long contratoId, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contratoId).getSingleResult());
    }

    private record Preparado(Cenario c, NovoContrato n) {}
    private record Transferencia(long id, long evento) {}
}
