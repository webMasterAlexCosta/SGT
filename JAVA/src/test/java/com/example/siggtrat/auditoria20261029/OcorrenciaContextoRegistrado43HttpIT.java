package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fontes sintéticas explícitas e comandos HTTP existentes em main42, sem referência ao serviço proposto. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaContextoRegistrado43HttpIT extends Ocorrencia43HttpBase {
    @Autowired GestaoContratoTemporalServico gestaoFonte43;

    @Test
    void recontratacaoDaMesmaPessoaNaoHerdaFonteAncoradaNoVinculoAnterior() throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43), ontem = hoje.minusDays(1);
        Fonte43 fonte = fonteAnteriorComDiaHoje(c, "ATESTADO"); List<String> fotos = fotosFonte43(fonte.id());
        long motivoId = emTransacao(() -> {
            MotivoDesligamento motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            motivo.setDescricao("Desligamento sintético F43 " + UUID.randomUUID());
            motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
            em.persist(motivo); em.flush();
            EventoDisponibilidadeCatalogo disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            disponibilidade.setCatalogoId(motivo.getId()); disponibilidade.setDataEfetiva(INICIO); disponibilidade.setAtivo(true);
            disponibilidade.setMotivo("Fonte sintética do motivo distinta do contexto contratual"); em.persist(disponibilidade); em.flush();
            FonteMotivoSintetica.declarar(em, gestaoFonte43, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                    "Término individual antes da nova contratação", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    ontem, ontem, Instant.now(), "SEI-SINTETICO-F43-RECONTRATACAO");
            return motivo.getId();
        });
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/desligar", mapa(
                "dataFim", ontem, "motivoDesligamentoId", motivoId, "justificativa", "Fim individual sintético",
                "evidencia", "SEI-F43-FIM", "autorizacao", "ATO-F43-FIM"), 200, "Encerra vínculo anterior inclusive ontem");
        JsonNode novo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoaId", c.pessoaA.getId(), "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                "dataInicio", hoje, "matricula", "F43-" + UUID.randomUUID().toString().substring(0, 12)),
                201, "Reaproveita Pessoa explícita em vínculo consecutivo distinto");
        long vinculoNovo = novo.path("id").asLong(); assertNotEquals(c.vinculoA.getId().longValue(), vinculoNovo);
        assertEquals(c.pessoaA.getId().longValue(), novo.path("pessoaId").asLong());
        assertEquals(0L, quantidade43(vinculoNovo, hoje));
        JsonNode criada = gravar43(c.operacionalA, presenca43(vinculoNovo, hoje, "f43-recontratado-" + UUID.randomUUID()), 200);
        long id = criada.path("id").asLong();
        assertEquals("PRESENCA", criada.path("status").asText(), "Mesmo cadastro de Pessoa não transfere contexto do vínculo antigo");
        assertFalse(criada.path("observacao").asText().contains("Ocorrência ID: " + fonte.id()));
        fisico48043(id); assertEquals(fotos, fotosFonte43(fonte.id()));
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(c.empregadoA.getId(), f.getEmpregado().getId()); assertEquals(c.pessoaA.getId(), f.getPessoa().getId());
            assertEquals(vinculoNovo, f.getVinculoContratual().getId().longValue());
            assertNull(em.find(Ocorrencia.class, fonte.id()).getCanceladoEm()); return null;
        });
    }

    @Test
    void retificacaoCriaNovaAlocacaoERealinhaFonteSemExigirIgualdadeComIdAtual() throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43);
        Fonte43 fonte = fonteAnteriorComDiaHoje(c, "ATESTADO"); List<String> fotos = fotosFonte43(fonte.id());
        long destinoId = emTransacao(() -> {
            Jornada jornada = em.find(Jornada.class, c.jornadaA.getId());
            Lotacao destino = new Lotacao("Destino F43 " + UUID.randomUUID());
            destino.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            destino.setMunicipio(c.lotacaoA.getMunicipio()); destino.getTurnos().add(jornada); em.persist(destino); em.flush();
            EventoDisponibilidadeCatalogo disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.LOTACAO); disponibilidade.setCatalogoId(destino.getId());
            disponibilidade.setDataEfetiva(INICIO); disponibilidade.setAtivo(true);
            disponibilidade.setMotivo("Fonte sintética explícita da combinação de destino desde INICIO");
            disponibilidade.setResponsavelId(c.localA.getId()); disponibilidade.setResponsavelUsername(c.localA.getUsername());
            em.persist(disponibilidade);
            ComposicaoContratual composicao = composicao(em.find(Contrato.class, c.contratoA.getId()), destino,
                    jornada, em.find(Cargo.class, c.cargoA.getId()), 5); em.flush(); return composicao.getId();
        });
        var req = mapa("dataEfetiva", INICIO, "composicaoId", destinoId,
                "justificativa", "Retificação explícita da combinação preservando a fonte anterior",
                "evidencia", "SEI-F43-RETIFICACAO", "autorizacao", "ATO-F43-RETIFICACAO");
        String caminho = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        JsonNode previa = enviar(c.localA, caminho + "/previas/alocacoes", req, 200, "Prévia real de retificação da Alocação");
        assertEquals(0, previa.path("incompatibilidades").size());
        req.put("tokenPrevia", previa.path("token").asText()); req.put("confirmarImpacto", true);
        JsonNode alocacao = enviar(c.localA, caminho + "/alocacoes/" + c.alocacaoA.getId() + "/retificar", req,
                200, "Retificação substitui faixa e preserva fotografia antiga");
        long novaId = alocacao.path("id").asLong(); assertNotEquals(c.alocacaoA.getId().longValue(), novaId);
        var corpo = presenca43(c.vinculoA.getId(), hoje, "f43-retificada-" + UUID.randomUUID());
        corpo.put("justificativaRetificacao", "Primeiro lançamento válido após retificação da Alocação");
        JsonNode criada = gravar43(c.localA, corpo, 200); long id = criada.path("id").asLong();
        assertEquals("ATESTADO", criada.path("status").asText(), "Fonte registrada realinhada pela decisão temporal legítima");
        fisico48043(id); assertEquals(fotos, fotosFonte43(fonte.id()));
        emTransacao(() -> {
            AlocacaoVinculo origem = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
            assertEquals(c.lotacaoA.getId(), origem.getLotacao().getId()); assertEquals(c.jornadaA.getId(), origem.getJornada().getId());
            Frequencia f = em.find(Frequencia.class, id); assertEquals(novaId, f.getAlocacaoVinculo().getId().longValue());
            assertEquals(alocacao.path("lotacaoId").asLong(), f.getLotacaoSnapshotId().longValue());
            assertNull(em.find(Ocorrencia.class, fonte.id()).getCanceladoEm()); return null;
        });
    }

    @ParameterizedTest(name = "transferência com contexto do dia preservado={0}")
    @ValueSource(booleans = {true, false})
    void transferenciaExigeContextoRegistradoDoDiaEAceitaRealinhamentoDocumentado(boolean contextoPreservado) throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43);
        Fonte43 fonte = fonteAnteriorComDiaHoje(c, "ATESTADO");
        if (!contextoPreservado) {
            // Contraprova sintética: acervo sem contexto do dia. Não é remoção de fonte real nem backfill.
            int removidos = emTransacao(() -> em.createQuery("delete from ContextoOcorrencia c where c.ocorrencia.id=:o and c.dataReferencia=:d")
                    .setParameter("o", fonte.id()).setParameter("d", hoje).executeUpdate());
            assertEquals(1, removidos);
        }
        List<String> fotos = fotosFonte43(fonte.id()); conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", hoje, "dataAto", hoje, "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência sintética F43",
                "referenciaSei", "SEI-F43-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Mapa explícito transfere responsabilidade e combinação operacional");
        var corpo = presenca43(c.vinculoA.getId(), hoje, "f43-transferida-" + UUID.randomUUID());
        gravar43(c.operacionalA, corpo, 403);
        JsonNode criada = gravar43(c.operacionalB, corpo, 200); long id = criada.path("id").asLong();
        assertEquals(contextoPreservado ? "ATESTADO" : "PRESENCA", criada.path("status").asText(),
                "Mapa realinha fonte explícita; Pessoa/CPF/projeção atual não suprem contexto ausente");
        assertEquals(contextoPreservado, criada.path("observacao").asText().contains("Ocorrência ID: " + fonte.id()));
        assertEquals("ORIGINAL", criada.path("estadoAuditoria").asText()); fisico48043(id);
        assertEquals(fotos, fotosFonte43(fonte.id()));
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(c.gestaoB.getId(), f.getGestaoLocalSnapshotId());
            assertEquals(c.lotacaoB.getId(), f.getLotacaoSnapshotId()); assertEquals(c.jornadaB.getId(), f.getJornadaSnapshotId());
            assertEquals(c.operacionalB.getId(), f.getAutorOriginalId()); assertNull(em.find(Ocorrencia.class, fonte.id()).getCanceladoEm());
            return null;
        });
    }
}
