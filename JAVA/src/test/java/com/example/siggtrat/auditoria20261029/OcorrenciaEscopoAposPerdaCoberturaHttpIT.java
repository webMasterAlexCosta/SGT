package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DIF-02/PER-010: a perda de cobertura não desfaz a responsabilidade já corrigida. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaEscopoAposPerdaCoberturaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;
    @Autowired HistoricoIntegridadeServico integridade;

    @Test
    void anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa() throws Exception {
        Cenario c = criarCenarioPersistido();
        // Este cenário conhece a admissão original; a fixture JPA genérica não
        // fornece fonte nominal e deve continuar servindo aos controles de ausência.
        String nomeDocumentado = c.pessoaA.getNome();
        declararFonteNominalSintetica(c, "OCORRENCIA-ANULACAO-ESCOPO");
        emTransacao(() -> {
            em.flush(); em.clear();
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(vinculo.getDataInicio()).isEqualTo(INICIO);
            assertThat(vinculo.getDataFim()).isNull();
            assertThat(DATA).isAfter(INICIO).isBefore(com.example.siggtrat.utilitario.RelogioSistema.hoje());
            var fontes = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v "
                            + "and e.dataEfetiva<=:dia order by e.registradoEm,e.id", EventoVinculoContratual.class)
                    .setParameter("v", vinculo.getId()).setParameter("dia", DATA.minusDays(1)).getResultList();
            assertThat(fontes).hasSize(1);
            EventoVinculoContratual fonte = fontes.get(0);
            assertThat(fonte.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
            assertThat(fonte.getDataEfetiva()).isEqualTo(INICIO);
            assertThat(fonte.getRegistradoEm()).isEqualTo(INICIO.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            assertThat(fonte.getPessoaNomeFotografia()).isEqualTo(nomeDocumentado);
            assertThat(fonte.getPessoaCpfFotografia()).isEqualTo(vinculo.getPessoa().getCpf());
            assertThat(fonte.getInicioNovo()).isEqualTo(vinculo.getDataInicio());
            assertThat(fonte.getGestaoLocalIdFotografia()).isEqualTo(c.gestaoA.getId());
            assertThat(fonte.getResponsavelId()).isEqualTo(c.localA.getId());
            assertThat(fonte.getEvidencia()).isEqualTo("SEI-FIXTURE-OCORRENCIA-ANULACAO-ESCOPO");
            assertThat(fonte.getAutorizacao()).isEqualTo("ATO-FIXTURE-OCORRENCIA-ANULACAO-ESCOPO");
            assertThat(fonte.getHashEvento()).isNotBlank();
            return null;
        });
        byte[] prova = "Documento sintético anterior à transferência conhecida".getBytes(StandardCharsets.UTF_8);
        JsonNode ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova.txt", "text/plain", prova))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                .param("classificacoesAnexos", "NEGOCIAL")
                .param("chaveIdempotencia", "prova-anulacao-" + UUID.randomUUID()), 201,
                "Fonte original gravada em transação real no contexto A então conhecido"));
        long id = ocorrencia.path("id").asLong(), anexo = ocorrencia.path("anexos").get(0).path("id").asLong();
        String hash = emTransacao(() -> em.find(AnexoOcorrencia.class, anexo).getSha256());
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA.minusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Responsabilidade retroativa documentada", "referenciaSei", "SEI-ESCOPO-FONTE",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", c.composicaoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência altera responsabilidade efetiva sem alterar o documento original");
        executar(c.localA, get("/api/ocorrencias/{id}", id), 403,
                "Controle anterior à anulação: A já não possui o dia");
        executar(c.localB, get("/api/ocorrencias/{id}", id), 200,
                "Controle anterior à anulação: B possui o dia");
        emTransacao(() -> {
            MotivoDesligamento motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoB.getId()));
            motivo.setDescricao("Anulação preserva prova da responsabilidade efetiva");
            motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO); em.persist(motivo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa de anulação anterior à ocorrência auditada");
            em.persist(origem); em.flush();
            // Fonte contextual de B apenas em DATA, após a transferência em DATA−1.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(),
                    c.gestaoB.getId(), c.localB,
                    "Anulação preserva prova da responsabilidade efetiva",
                    CategoriaTerminoVinculo.ANULACAO_CONTRATO, DATA, DATA,
                    com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                    "SEI-SINTETICO-OCORRENCIA-ANULACAO-ESCOPO-B");
            return null;
        });
        Map<String,Object> anulacao = mapa("dataEfetiva", DATA, "dataAto", RelogioSistema.hoje(),
                "motivo", "Retirada de cobertura sem desfazer transferência", "referenciaSei", "SEI-PERDA-COBERTURA");
        JsonNode previa = enviar(c.localB, base(c.contratoA.getId()) + "/previas/anular", anulacao, 200,
                "Prévia real considera os fatos já documentados");
        anulacao.put("tokenPrevia", previa.path("tokenPrevia").asText()); anulacao.put("confirmarImpacto", true);
        enviar(c.localB, base(c.contratoA.getId()) + "/anular", anulacao, 200,
                "Anulação confirmada preserva ocorrência e anexo");

        executar(c.localA, get("/api/ocorrencias/{id}", id), 403,
                "Perda de validade operacional não restaura escopo A superado");
        executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Download também não recua para a fotografia organizacional superada");
        executar(c.localB, get("/api/ocorrencias/{id}", id), 200,
                "Gestão B responsável continua consultando a prova após a anulação");
        executar(c.estadualA, get("/api/ocorrencias/{id}", id), 403,
                "Auditoria estadual da origem também perde o dia efetivamente transferido");
        JsonNode estadual = dados(executar(c.estadualB, get("/api/ocorrencias/{id}", id), 200,
                "Auditoria estadual do destino consulta o fato comprovado com minimização"));
        assertTrue(estadual.path("ocorrencia").path("observacao").isNull());
        assertArrayEquals(prova, executar(c.localB, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Documento original permanece íntegro e autorizado pelo contexto efetivo").getResponse().getContentAsByteArray());
        assertEquals(0, dados(executar(c.localA, get("/api/ocorrencias")
                .param("contratoId", c.contratoA.getId().toString()).param("data", DATA.toString()), 200,
                "Paginação da origem não contabiliza prova fora do escopo")).path("totalElements").asInt());
        assertEquals(1, dados(executar(c.localB, get("/api/ocorrencias")
                .param("contratoId", c.contratoA.getId().toString()).param("data", DATA.toString()), 200,
                "Paginação do destino conserva a prova histórica")).path("totalElements").asInt());
        emTransacao(() -> {
            Ocorrencia persistida = em.find(Ocorrencia.class, id);
            assertEquals(c.gestaoA.getId(), persistida.getContextos().get(0).getGestaoLocalId(),
                    "Foto original não é reescrita para resolver autorização");
            assertNull(persistida.getCanceladoEm()); assertEquals(hash, em.find(AnexoOcorrencia.class, anexo).getSha256());
            return null;
        });
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());
    }
}
