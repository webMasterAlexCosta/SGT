package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proposta de reprodução das adjacências do TRACE-VIN014-REL006-001; sem aprovação presumida. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TerminoVinculoEmissaoMaterializadaHttpIT extends ContratosHttpBase {
    @Autowired private GestaoContratoTemporalServico gestaoTemporal;

    @ParameterizedTest
    @ValueSource(strings = {"DESLIGAR", "RETIFICAR_DESLIGAMENTO"})
    void reducaoDoFimSuperaEmissaoDoDiaRetiradoEPreservaUltimoDiaValido(String comando) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate diaRetirado = DATA;
        LocalDate novoFim = DATA.minusDays(1);
        LocalDate fimAnterior = DATA.plusDays(3);
        long motivoId = prepararCausaEFimSinteticos(c, comando, novoFim, fimAnterior);
        declararFonteNominalSintetica(c, "REL006-TERMINO-MATERIALIZADO-" + comando);
        long vinculoId = c.vinculoA.getId();
        assertEquals(0L, quantidadeEventosAlocacao(vinculoId),
                "O cenário exercita o primeiro ajuste de Alocação que ainda não tem evento de origem");
        assertEquals(0L, emTransacao(() -> em.createQuery(
                        "select count(f) from Frequencia f where f.vinculoContratual.id=:id", Long.class)
                .setParameter("id", vinculoId).getSingleResult()));

        JsonNode original = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtro(c, diaRetirado), 201,
                "REL-006 emissão preexistente da faixa materializada que será retirada pelo término");
        JsonNode controle = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtro(c, novoFim), 201,
                "DES-002 o último dia continua incluído e deve conservar sua emissão vigente");
        assertEquals(1, original.path("conteudo").path("registros").size());
        assertEquals(480, original.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        assertEquals(1, controle.path("conteudo").path("registros").size());
        assertEquals(480, controle.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        long emissaoId = original.path("metadados").path("id").asLong();
        long controleId = controle.path("metadados").path("id").asLong();
        Foto anterior = fotografia(emissaoId);
        Foto controleAnterior = fotografia(controleId);
        assertEquals("VIGENTE", anterior.estado());
        assertEquals("VIGENTE", controleAnterior.estado());

        Map<String, Object> pedido = mapa("dataFim", novoFim, "motivoDesligamentoId", motivoId,
                "justificativa", "Documento sintético define o último dia inclusive do vínculo",
                "evidencia", "SEI-SINTETICO-TERMINO-EMISSAO-" + comando,
                "autorizacao", "Conferência pelo Gestor Local responsável");
        String rota = "/api/vinculos-contratuais/" + vinculoId;
        if (comando.equals("RETIFICAR_DESLIGAMENTO")) {
            JsonNode previa = enviar(c.localA, rota + "/previas/retificacao-desligamento", pedido,
                    200, "DES-005 prévia do término materializado antes da retificação HTTP");
            assertEquals(fimAnterior.toString(), previa.path("valorAtual").asText());
            assertEquals(64, previa.path("token").asText().length());
            pedido.put("tokenPrevia", previa.path("token").asText());
            pedido.put("confirmarImpacto", true);
        }
        JsonNode resposta = enviar(c.localA, rota + (comando.equals("DESLIGAR")
                        ? "/desligar" : "/retificar-desligamento"), pedido, 200,
                "DES-002/DES-005 comando real retira cobertura somente depois do último dia válido");
        assertEquals(novoFim.toString(), resposta.path("dataFim").asText());
        assertEquals(novoFim, emTransacao(() -> {
            em.clear();
            return em.find(VinculoContratual.class, vinculoId).getDataFim();
        }));
        assertTrue(quantidadeEventosAlocacao(vinculoId) > 0);

        Foto depois = fotografia(emissaoId);
        Foto controleDepois = fotografia(controleId);
        assertAll("REL-006 deve superar apenas o recorte retirado após commit de " + comando,
                () -> assertEquals("SUPERADA", depois.estado()),
                () -> assertEquals("RETIFICACAO_FONTE", depois.causa()),
                () -> assertEquals(anterior.hash(), depois.hash()),
                () -> assertArrayEquals(anterior.gzip(), depois.gzip()),
                () -> assertEquals("VIGENTE", controleDepois.estado()),
                () -> assertNull(controleDepois.causa()),
                () -> assertEquals(controleAnterior.hash(), controleDepois.hash()),
                () -> assertArrayEquals(controleAnterior.gzip(), controleDepois.gzip()));
    }

    private long prepararCausaEFimSinteticos(Cenario c, String comando,
                                            LocalDate novoFim, LocalDate fimAnterior) {
        return emTransacao(() -> {
            MotivoDesligamento motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            motivo.setDescricao("Término sintético do vínculo " + c.vinculoA.getId());
            motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
            motivo.setAtivo(true);
            motivo.setCriadoPorId(c.localA.getId());
            motivo.setCriadoPorUsername(c.localA.getUsername());
            em.persist(motivo);
            em.flush();
            EventoDisponibilidadeCatalogo disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            disponibilidade.setCatalogoId(motivo.getId());
            disponibilidade.setDataEfetiva(INICIO);
            disponibilidade.setAtivo(true);
            disponibilidade.setMotivo("Fonte sintética explícita de disponibilidade para o intervalo deste teste");
            em.persist(disponibilidade);
            FonteMotivoSintetica.declarar(em, gestaoTemporal, c.contratoA.getId(), motivo.getId(),
                    c.gestaoA.getId(), c.localA, motivo.getDescricao(),
                    CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL, novoFim, fimAnterior,
                    Instant.now(), "SEI-SINTETICO-CAUSA-TERMINO-EMISSAO-" + comando);
            if (comando.equals("RETIFICAR_DESLIGAMENTO")) {
                // Estado sintético recebido sem evento de Alocação: não se chama
                // o comando de desligamento para não fabricar uma origem completa.
                VinculoContratual v = em.find(VinculoContratual.class, c.vinculoA.getId());
                v.setDataFim(fimAnterior);
                v.setMotivoDesligamento(motivo);
                v.setMotivoDesligamentoDescricaoFotografia(motivo.getDescricao());
                v.setMotivoDesligamentoCategoriaFotografia(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                v.setDesligamentoRegistradoEm(Instant.now());
                v.setDesligamentoRegistradoPorId(c.localA.getId());
                v.setDesligamentoRegistradoPorUsername(c.localA.getUsername());
                em.find(AlocacaoVinculo.class, c.alocacaoA.getId()).setDataFim(fimAnterior);
            }
            em.flush();
            return motivo.getId();
        });
    }

    private Map<String, Object> filtro(Cenario c, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", data, "fim", data, "contratoIds", List.of(c.contratoA.getId()),
                "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "chaveIdempotencia", "termino-materializado-" + UUID.randomUUID());
    }

    private long quantidadeEventosAlocacao(long vinculoId) {
        return emTransacao(() -> em.createQuery(
                        "select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:id", Long.class)
                .setParameter("id", vinculoId).getSingleResult());
    }

    private Foto fotografia(long id) {
        return emTransacao(() -> {
            em.clear();
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            assertNotNull(e);
            return new Foto(e.getEstado().name(), e.getCausaSuperacao() == null ? null
                    : e.getCausaSuperacao().name(), e.getHashConteudo(), e.getConteudoSnapshotGzip().clone());
        });
    }

    private record Foto(String estado, String causa, String hash, byte[] gzip) { }
}
