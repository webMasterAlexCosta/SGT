package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AditamentoContratual;
import com.example.siggtrat.entidade.RemanejamentoContratual;
import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * C4-CON-H02: identidade do instante de conhecimento do novo instrumento.
 * Fonte de produção: manifesto C4 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Autoria: revisão independente do núcleo contratual, somente proposta, sem execução.
 *
 * ADI/REM: recibo HTTP deve coincidir com JPA em nova transação e GET posterior.
 * Transferência: o POST devolve ContratoDTO e NÃO divulga registradoEm do instrumento;
 * portanto captura-se explicitamente o novo fato ainda gerenciado na transação
 * legítima da chamada HTTP, sem apresentar esse valor como recibo HTTP público.
 *
 * Usa fontes sintéticas declaradas por ContratosHttpBase e concessão administrativa
 * pelo endpoint existente. Não prova autenticidade de fontes documentais legadas.
 * Reenvio simples de ADI/REM deve ser recusado (409), pois não é retificação explícita.
 * Nenhum relógio, repositório, autorização ou serviço é substituído por mock.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4PrecisaoRecibosInstrumentosRedIT extends ContratosHttpBase {

    @Test
    void reciboAditamentoConservaInstanteDepoisDoCommitEReenvioRecusado() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, null);
        String rota = base(n.id()) + "/aditamentos";
        Map<String, Object> req = aditamento("PRECISAO-ADI-" + UUID.randomUUID(), n.inicio().plusDays(1));
        req.put("novaDescricao", "Fonte sintética para verificar a precisão do novo aditamento");
        req.put("referenciaSei", "SEI-SINTETICO-PRECISAO-ADI");
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200, "Controle: prévia legítima de ADI");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        JsonNode recibo = enviar(c.localA, rota, req, 201, "Controle: novo ADI confirmado por HTTP");
        long id = recibo.path("id").asLong();
        verificar(c, n.id(), "aditamento", "AditamentoContratual", id,
                Instant.parse(recibo.path("registradoEm").asText()), "recibo HTTP da mutação",
                () -> em.find(AditamentoContratual.class, id).getRegistradoEm(), rota, rota, req, 409);
    }

    @Test
    void reciboRemanejamentoConservaInstanteDepoisDoCommitEReenvioRecusado() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, 5);
        String rota = base(n.id()) + "/remanejamentos";
        Map<String, Object> req = remanejamento(n, "PRECISAO-REM-" + UUID.randomUUID(), n.inicio().plusDays(1), 1);
        req.put("justificativa", "Fonte sintética para verificar a precisão do novo remanejamento");
        req.put("referenciaSei", "SEI-SINTETICO-PRECISAO-REM");
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200, "Controle: prévia soma zero legítima");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        JsonNode recibo = enviar(c.localA, rota, req, 201, "Controle: novo REM confirmado por HTTP");
        long id = recibo.path("id").asLong();
        verificar(c, n.id(), "remanejamento", "RemanejamentoContratual", id,
                Instant.parse(recibo.path("registradoEm").asText()), "recibo HTTP da mutação",
                () -> em.find(RemanejamentoContratual.class, id).getRegistradoEm(), rota, rota, req, 409);
    }

    @Test
    void novoFatoTransferenciaConservaInstanteDepoisDoCommitEReenvioRecusado() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, null);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        String rota = base(n.id()) + "/transferir-gestao";
        Map<String, Object> req = mapa("dataEfetiva", n.inicio().plusDays(1),
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Fonte sintética para verificar a precisão da nova transferência",
                "referenciaSei", "SEI-SINTETICO-PRECISAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId())));
        FatoEmMemoria fato = emTransacao(() -> {
            try {
                enviar(c.localA, rota, req, 200, "Controle: transferência com fonte e concessão explícitas");
                var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:id",
                                TransferenciaGestaoContrato.class).setParameter("id", n.id()).getSingleResult();
                // A entidade permanece no contexto que criou o fato; não há refresh aqui.
                return new FatoEmMemoria(t.getId(), t.getRegistradoEm());
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        verificar(c, n.id(), "transferencia", "TransferenciaGestaoContrato", fato.id(), fato.registradoEm(),
                "novo fato ainda gerenciado na transação da chamada HTTP; POST não expõe este campo",
                () -> em.find(TransferenciaGestaoContrato.class, fato.id()).getRegistradoEm(),
                rota, base(n.id()) + "/transferencias-gestao", req, 400, 409);
    }

    private NovoContrato publicar(Cenario c, Integer primeira) {
        return emTransacao(() -> {
            try { return publicado(c, 10, primeira, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private void verificar(Cenario c, long contratoId, String nome, String entidade, long id,
                           Instant emitido, String naturezaCaptura, Supplier<Instant> leitor,
                           String rotaMutacao, String rotaConsulta, Map<String, Object> req,
                           Integer... statusReenvioEsperados) throws Exception {
        // Não existe transação de teste externa: cada chamada abre e encerra outro contexto JPA.
        Instant persistido = emTransacao(leitor);
        JsonNode antes = instrumento(c, rotaConsulta, id);
        long quantidadeAntes = quantidade(entidade, contratoId);
        int reenvio = mvc.perform(autenticar(post(rotaMutacao).contentType(MediaType.APPLICATION_JSON)
                .content(json(req)), c.localA)).andReturn().getResponse().getStatus();
        Instant aposReenvio = emTransacao(leitor);
        JsonNode depois = instrumento(c, rotaConsulta, id);
        long quantidadeDepois = quantidade(entidade, contratoId);
        Path evidencia = Path.of("target/auditoria-http/candidato4-precisao-instrumento-" + nome + ".json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, json(mapa("contratoId", contratoId, "instrumentoId", id,
                "naturezaCaptura", naturezaCaptura, "emitido", emitido, "persistidoNovaTransacao", persistido,
                "consultaAntesReenvio", antes, "statusReenvio", reenvio, "persistidoAposReenvio", aposReenvio,
                "consultaAposReenvio", depois, "quantidadeAntes", quantidadeAntes, "quantidadeDepois", quantidadeDepois)));
        assertAll(
                () -> assertThat(emitido).as("Novo fato já possui precisão persistível antes do commit")
                        .isEqualTo(emitido.truncatedTo(ChronoUnit.MICROS)),
                () -> assertThat(emitido).as("Instante emitido não muda ao reler o fato persistido").isEqualTo(persistido),
                () -> assertThat(Instant.parse(antes.path("registradoEm").asText())).isEqualTo(persistido),
                () -> assertThat(reenvio).as("Reenvio sem retificação explícita não cria outro fato").isIn(statusReenvioEsperados),
                () -> assertThat(quantidadeAntes).isEqualTo(1L),
                () -> assertThat(quantidadeDepois).isEqualTo(quantidadeAntes),
                () -> assertThat(aposReenvio).isEqualTo(persistido),
                () -> assertThat(Instant.parse(depois.path("registradoEm").asText())).isEqualTo(persistido));
    }

    private JsonNode instrumento(Cenario c, String rota, long id) throws Exception {
        JsonNode lista = consultar(c.nacional, rota, "Leitura posterior em requisição e contexto independentes");
        for (JsonNode item : lista) if (item.path("id").asLong() == id) return item;
        throw new AssertionError("Instrumento " + id + " ausente na consulta " + rota);
    }

    private long quantidade(String entidade, long contratoId) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contratoId).getSingleResult());
    }

    private record FatoEmMemoria(long id, Instant registradoEm) {}
}
