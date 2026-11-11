package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dto.ConferenciaDocumentalAtoDTO;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.MaterialHashEventos;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Suplemento64: fixture e referências SINTÉTICAS. Não é upload, queda de processo ou prova de documento autêntico.
 * O único método interceptado chama a implementação real antes da falha sintética.
 * Proposta somente GREEN: depende de backend64 e TransferenciaConferencia64HttpBase do00RED. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaConferenciaRollback64HttpIT extends TransferenciaConferencia64HttpBase {
    @MockitoSpyBean
    MotorTemporalContratoServico motor;

    @Test
    void falhaDepoisDosInsertsReverteUsoEventoConferenciaMapaEFonteSemPerderOriginais() throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Teste sem transação ambiente");
        var f = preparar64(true);
        LocalDate efeito = f.n().inicio().plusDays(2);
        var pedido = intencao64(f, efeito);
        pedido.put("autorizacaoAdministrativa", "ATO-SINTETICO-64-ROLLBACK");
        pedido.put("referenciaSei", "SEI-SINTETICO-64-ROLLBACK");
        declarar64(pedido);
        JsonNode antes = relerRequerNova(() -> estado(f));
        var previa = enviar(f.c().localA, base(f.n().id()) + "/transferencias-gestao/previa", pedido, 200,
                "64ROLLBACK prévia real de intenção/grant/mapa sintéticos válidos");
        pedido.put("tokenPrevia", previa.path("tokenPrevia").asText());
        pedido.put("confirmarImpacto", true);
        assertEquals(antes, relerRequerNova(() -> estado(f)), "Prévia não grava ato nem consome concessão");
        String mesmoPayload = json(pedido);
        String mesmoToken = (String) pedido.get("tokenPrevia");
        var falhar = new AtomicBoolean(true);
        var falhaLancada = new AtomicBoolean(false);
        var pontosCompletos = new AtomicInteger();
        var revertido = new AtomicReference<Inseridos>();
        var confirmado = new AtomicReference<Inseridos>();

        doAnswer(invocacao -> {
            Contrato contrato = invocacao.getArgument(0);
            EventoCicloVidaContrato evento = invocacao.getArgument(1);
            Object fotografia = invocacao.callRealMethod();
            if (!Objects.equals(contrato.getId(), f.n().id())
                    || evento.getTipoEvento() != TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL)
                return fotografia;
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Observação dentro da transação do POST");
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            em.flush();
            em.clear();
            var inseridos = conferirInsertsNaTransacao(f, evento.getId(), ((VersaoContratoNegocial) fotografia).getId(), pedido);
            pontosCompletos.incrementAndGet();
            if (falhar.get()) {
                revertido.set(inseridos);
                falhaLancada.set(true);
                throw new FalhaSinteticaDepoisDosInserts64();
            }
            confirmado.set(inseridos);
            return fotografia;
        }).when(AopTestUtils.<MotorTemporalContratoServico>getUltimateTargetObject(motor))
                .registrarFotografiaProjetada(any(Contrato.class), any(EventoCicloVidaContrato.class),
                        any(LocalDate.class), any(VersaoContratoNegocialDTO.class));

        try {
            enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", pedido, 500,
                    "64ROLLBACK falha sintética só após INSERTs reais de uso/evento/conferência/transferência/mapa/fonte/fotografia");
        } finally {
            falhar.set(false);
        }
        assertTrue(falhaLancada.get(), "HTTP500 sozinho não prova que o ponto após os INSERTs foi atingido");
        assertEquals(1, pontosCompletos.get());
        Inseridos falhou = Objects.requireNonNull(revertido.get());
        JsonNode depoisDoRollback = relerRequerNova(() -> {
            exigirAusenciaDeTodasAsPKs(falhou);
            return estado(f);
        });
        assertEquals(antes, depoisDoRollback, "REQUIRES_NEW: mesmos originais/IDs/valores e nenhuma persistência parcial");
        assertEquals(mesmoPayload, json(pedido), "O retry positivo não altera nenhum campo da intenção");
        assertEquals(mesmoToken, pedido.get("tokenPrevia"), "O mesmo token permanece no controle positivo");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", pedido, 200,
                "64ROLLBACK desabilita somente a exceção; mesma prévia/payload/ator/concessão e guardas reais");
        assertEquals(2, pontosCompletos.get(), "Spy, método real, flush/clear e asserções continuam ativos no positivo");
        Inseridos passou = Objects.requireNonNull(confirmado.get());
        assertEquals(falhou.conferencia().hashIntencao(), passou.conferencia().hashIntencao(),
                "Mesma intenção; IDs técnicos/instante/uso novos não viram alcance reutilizável");
        JsonNode depoisDoControle = relerRequerNova(() -> {
            exigirAusenciaDeTodasAsPKs(falhou);
            var releitura = conferirInsertsNaTransacao(f, passou.eventoId(), passou.fotografiaId(), pedido);
            assertEquals(passou, releitura, "REQUIRES_NEW confirma os valores gravados no positivo por suas PKs");
            var atual = estado(f);
            exigirOriginaisPreservados(antes, atual);
            assertEquals(antes.path("ids").path("eventos").size() + 1, atual.path("ids").path("eventos").size());
            assertEquals(antes.path("ids").path("fotografias").size() + 1, atual.path("ids").path("fotografias").size());
            assertEquals(1, atual.path("ids").path("transferencias").size());
            assertEquals(1, atual.path("ids").path("mapas").size());
            assertEquals(1, atual.path("ids").path("usos").size());
            assertEquals(antes.path("ids").path("fontes").size() + 1, atual.path("ids").path("fontes").size());
            assertEquals(antes.path("ids").path("eventosFonte").size() + 1, atual.path("ids").path("eventosFonte").size());
            return atual;
        });
        // Evidência sintética de execução futura, sem senha/cookie/token nem conteúdo externo.
        Path evidencia = Path.of("target/auditoria-http/64-rollback-conferencia-ato.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "fixture", "INTEGRALMENTE_SINTETICA", "testAmbientTransaction", "NOT_SUPPORTED",
                "observation", "mesma transação do POST após callRealMethod + flush/clear, antes da exceção",
                "failure", "FalhaSinteticaDepoisDosInserts64", "samePayloadAndToken", true,
                "postFailureStatus", 500, "postControlStatus", 200,
                "uncommittedThenRolledBack", falhou, "committedControl", passou,
                "baseline", antes, "afterRollback", depoisDoRollback, "afterControl", depoisDoControle)) + "\n");
    }

    private Inseridos conferirInsertsNaTransacao(Fixture f, long eventoId, long fotografiaId, Map<String, Object> pedido) {
        var evento = em.find(EventoCicloVidaContrato.class, eventoId);
        assertNotNull(evento, "PK do evento já inserida no contexto SQL da operação");
        var conferencia = evento.getConferenciaDocumental(); assertNotNull(conferencia);
        assertEquals(VERSAO, conferencia.getVersaoDeclaracao());
        assertEquals(ConferenciaDocumentalTransferencia.DECLARACAO, conferencia.getDeclaracao());
        assertEquals(pedido.get("autorizacaoAdministrativa"), conferencia.getAutorizacaoAdministrativaAto());
        assertNotNull(conferencia.getIntencaoCanonica());
        assertEquals(MaterialHashEventos.sha256(conferencia.getVersaoIntencao(),
                MaterialHashEventos.texto(conferencia.getIntencaoCanonica())), conferencia.getHashIntencao());
        JsonNode intencao;
        try { intencao = mapper.readTree(conferencia.getIntencaoCanonica()); }
        catch (com.fasterxml.jackson.core.JsonProcessingException invalido) {
            throw new AssertionError("Intenção persistida deve ser JSON válido e legível", invalido);
        }
        assertEquals("TRANSFERIR_GESTAO", intencao.path("operacao").asText());
        assertEquals(f.n().id(), intencao.path("contratoId").asLong());
        assertEquals(evento.getDataEfetiva().toString(), intencao.path("dataEfetiva").asText());
        assertEquals(f.c().gestaoA.getId().longValue(), intencao.path("gestaoOrigemId").asLong());
        assertEquals(f.c().gestaoB.getId().longValue(), intencao.path("gestaoDestinoId").asLong());
        assertEquals(f.c().tipoB.getId().longValue(), intencao.path("tipoDestinoId").asLong());
        assertEquals(pedido.get("referenciaSei"), intencao.path("referenciaSei").asText());
        assertEquals(pedido.get("autorizacaoAdministrativa"), intencao.path("autorizacaoAdministrativaAto").asText());
        assertEquals(VERSAO, intencao.path("versaoConferenciaDocumental").asText());
        assertEquals(1, intencao.path("mapeamentos").size());
        assertEquals(f.n().composicao1(), intencao.path("mapeamentos").get(0).path("composicaoOrigemId").asLong());
        var uso = em.find(UsoPermissaoExcepcional.class, conferencia.getUsoPermissaoExcepcional().getId());
        assertNotNull(uso, "PK exata de uso, não o último uso por data");
        assertEquals(uso.getId(), conferencia.getUsoPermissaoExcepcional().getId());
        assertEquals(f.n().id(), uso.getRecursoId().longValue()); assertEquals("Contrato", uso.getTipoRecurso());
        assertEquals(evento.getUsuarioResponsavelId(), uso.getUsuarioId());
        assertEquals(f.c().localA.getId(), uso.getUsuarioId());
        assertEquals(evento.getUsuarioResponsavelUsername(), uso.getUsuarioUsername());
        assertEquals(pedido.get("motivo"), uso.getFinalidade());
        assertEquals(com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL, uso.getTipo());
        var concessao = em.find(PermissaoExcepcional.class, uso.getPermissao().getId()); assertNotNull(concessao);
        assertEquals(f.c().localA.getId(), concessao.getUsuarioBeneficiario().getId());
        assertEquals("ATO-HTTP", concessao.getAutorizacaoAdministrativa(), "Capacidade não foi substituída pelo ATO específico");
        assertTrue(concessao.getValidoAte().isAfter(uso.getUsadoEm()));
        var transferencia = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c and t.evento.id=:e", TransferenciaGestaoContrato.class)
                .setParameter("c", f.n().id()).setParameter("e", eventoId).getSingleResult();
        assertNotNull(em.find(TransferenciaGestaoContrato.class, transferencia.getId()));
        assertEquals(eventoId, transferencia.getEvento().getId().longValue());
        assertEquals(pedido.get("autorizacaoAdministrativa"), transferencia.getAutorizacaoAdministrativa());
        assertEquals(f.c().gestaoA.getId(), transferencia.getGestaoOrigem().getId());
        assertEquals(f.c().gestaoB.getId(), transferencia.getGestaoDestino().getId());
        var mapas = em.createQuery("select m.id from MapeamentoTransferenciaGestao m where m.transferencia.id=:t order by m.id", Long.class)
                .setParameter("t", transferencia.getId()).getResultList();
        assertEquals(1, mapas.size());
        var mapa = em.find(MapeamentoTransferenciaGestao.class, mapas.get(0));
        assertEquals(transferencia.getId(), mapa.getTransferencia().getId());
        assertEquals(f.n().composicao1(), mapa.getComposicaoOrigem().getId().longValue());
        assertEquals(f.c().lotacaoA.getId(), mapa.getLotacaoOrigem().getId());
        assertEquals(f.c().lotacaoB.getId(), mapa.getLotacaoDestino().getId());
        var fontes = em.createQuery("select d.id from DocumentoComprobatorio d where d.contrato.id=:c and d.tipoFato=:tipo and d.fatoId=:t order by d.id", Long.class)
                .setParameter("c", f.n().id()).setParameter("tipo", TipoFatoDocumental.TRANSFERENCIA_GESTAO)
                .setParameter("t", transferencia.getId()).getResultList();
        assertEquals(1, fontes.size(), "Fonte SEI sintética automática do próprio POST");
        var fonte = em.find(DocumentoComprobatorio.class, fontes.get(0));
        assertEquals(transferencia.getId(), fonte.getFatoId()); assertEquals(pedido.get("referenciaSei"), fonte.getReferenciaSei());
        assertEquals("PUBLICADO", fonte.getEstado().name());
        assertNull(fonte.getNomeArmazenado(), "Referência sem upload"); assertNull(fonte.getSha256Arquivo());
        var eventosFonte = em.createQuery("select e.id from EventoDocumentoComprobatorio e where e.documento.id in :docs order by e.id", Long.class)
                .setParameter("docs", fontes).getResultList();
        assertEquals(1, eventosFonte.size());
        var eventoFonte = em.find(EventoDocumentoComprobatorio.class, eventosFonte.get(0));
        assertEquals(fonte.getId(), eventoFonte.getDocumento().getId());
        assertEquals(fonte.getUltimoHashEventoAuditoria(), eventoFonte.getHashEvento());
        var fotografia = em.find(VersaoContratoNegocial.class, fotografiaId); assertNotNull(fotografia);
        assertEquals(eventoId, fotografia.getEventoOrigem().getId().longValue());
        assertEquals(f.n().id(), fotografia.getContrato().getId().longValue());
        return new Inseridos(uso.getId(), eventoId, transferencia.getId(), mapas, fontes, eventosFonte,
                fotografiaId, MotorTemporalContratoServico.paraConferenciaDocumentalDTO(evento));
    }

    private void exigirAusenciaDeTodasAsPKs(Inseridos i) {
        assertNull(em.find(UsoPermissaoExcepcional.class, i.usoId()));
        assertNull(em.find(EventoCicloVidaContrato.class, i.eventoId()));
        assertNull(em.find(TransferenciaGestaoContrato.class, i.transferenciaId()));
        assertNull(em.find(VersaoContratoNegocial.class, i.fotografiaId()));
        for (long id : i.mapas()) assertNull(em.find(MapeamentoTransferenciaGestao.class, id));
        for (long id : i.fontes()) assertNull(em.find(DocumentoComprobatorio.class, id));
        for (long id : i.eventosFonte()) assertNull(em.find(EventoDocumentoComprobatorio.class, id));
        assertEquals(0L, em.createQuery("select count(e) from EventoCicloVidaContrato e where e.conferenciaDocumental.usoPermissaoExcepcional.id=:u", Long.class)
                .setParameter("u", i.usoId()).getSingleResult().longValue(), "Embeddable não tem PK própria: nenhum evento retém sua FK de uso");
    }

    private <T> T relerRequerNova(Supplier<T> leitura) {
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        return tx.execute(status -> { em.clear(); return leitura.get(); });
    }

    private JsonNode estado(Fixture f) {
        long id = f.n().id();
        var c = em.find(Contrato.class, id);
        var contrato = mapa("id", c.getId(), "numero", c.getNumeroContrato(), "ano", c.getAnoContrato(),
                "inicio", c.getDataInicio(), "fim", c.getDataFim(), "gestao", c.getGestaoLocal().getId(),
                "tipo", c.getTipoContratacao().getId(), "estado", c.getEstadoNegocial(), "ativo", c.getAtivo(),
                "efetivoPrevisto", c.getEfetivoPrevisto(), "version", c.getVersion());
        var ids = mapa("eventos", ids("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id", id),
                "fotografias", ids("select v.id from VersaoContratoNegocial v where v.contrato.id=:c order by v.id", id),
                "transferencias", ids("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:c order by t.id", id),
                "mapas", ids("select m.id from MapeamentoTransferenciaGestao m where m.transferencia.contrato.id=:c order by m.id", id),
                "usos", ids("select u.id from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato' order by u.id", id),
                "fontes", ids("select d.id from DocumentoComprobatorio d where d.contrato.id=:c order by d.id", id),
                "eventosFonte", ids("select e.id from EventoDocumentoComprobatorio e where e.documento.contrato.id=:c order by e.id", id));
        var eventos = em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id", EventoCicloVidaContrato.class)
                .setParameter("c", id).getResultList().stream().map(motor::paraEventoDTO).toList();
        return mapper.valueToTree(mapa("contrato", contrato, "ids", ids,
                "eventos", eventos,
                "fotografias", linhas("select v.id,v.eventoOrigem.id,v.numeroVersao,v.dataEfetiva,v.registradoEm,v.gestaoLocalId,v.tipoContratacaoId,v.dataInicio,v.dataFim,v.efetivoPrevisto from VersaoContratoNegocial v where v.contrato.id=:c order by v.id", id),
                "composicoes", linhas("select x.id,x.lotacao.id,x.jornada.id,x.cargo.id,x.efetivoPrevisto,x.dataInicioVigencia,x.dataFimVigencia from ComposicaoContratual x where x.contrato.id=:c order by x.id", id),
                "fontes", linhas("select d.id,d.fatoId,d.tipoFato,d.fatoChave,d.estado,d.referenciaSei,d.grupoLogicoId,d.versaoDocumento,d.nomeArmazenado,d.sha256Arquivo,d.quantidadeEventosAuditoria,d.ultimoHashEventoAuditoria,d.registradoEm,d.version from DocumentoComprobatorio d where d.contrato.id=:c order by d.id", id),
                "eventosFonte", linhas("select e.id,e.documento.id,e.tipoEvento,e.hashEvento,e.hashAnterior,e.registradoEm,e.referenciaSeiNova from EventoDocumentoComprobatorio e where e.documento.contrato.id=:c order by e.id", id)));
    }
    private List<Long> ids(String jpql, long contratoId) {
        return em.createQuery(jpql, Long.class).setParameter("c", contratoId).getResultList();
    }
    private List<List<Object>> linhas(String jpql, long contratoId) {
        return em.createQuery(jpql, Object[].class).setParameter("c", contratoId).getResultList().stream().map(Arrays::asList).toList();
    }
    private void exigirOriginaisPreservados(JsonNode antes, JsonNode depois) {
        assertEquals(antes.path("contrato"), depois.path("contrato"), "Transferência futura não muda cadastro corrente");
        assertEquals(antes.path("composicoes"), depois.path("composicoes"));
        for (String grupo : List.of("eventos", "fotografias", "fontes", "eventosFonte"))
            for (JsonNode original : antes.path(grupo)) {
                long id = original.isArray() ? original.get(0).asLong() : original.path("id").asLong();
                JsonNode atual = java.util.stream.StreamSupport.stream(depois.path(grupo).spliterator(), false)
                        .filter(item -> (item.isArray() ? item.get(0).asLong() : item.path("id").asLong()) == id)
                        .findFirst().orElseThrow();
                assertEquals(original, atual, "Origem preservada por PK: " + grupo + "/" + id);
            }
    }
    private record Inseridos(long usoId, long eventoId, long transferenciaId, List<Long> mapas,
            List<Long> fontes, List<Long> eventosFonte, long fotografiaId, ConferenciaDocumentalAtoDTO conferencia) { }
    private static final class FalhaSinteticaDepoisDosInserts64 extends RuntimeException { }
}
