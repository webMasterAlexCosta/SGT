package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** FRE-001/015/018: complementos usam a data efetiva, inclusive depois da transferência. */
class FrequenciaComplementosEscopoTemporalHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void consultaEFiltrosPreservamGestaoHistoricaDepoisDeTransferencia(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso, DATA);
        transferir(c, DATA.plusDays(1));
        JsonNode origem = dados(executar(c.localA, get(rota(recurso)).param("data", DATA.toString())
                .param("lotacaoId", c.lotacaoA.getId().toString()).param("jornadaId", c.jornadaA.getId().toString())
                .param("contratoId", c.contratoA.getId().toString()), 200,
                "Consulta usa Lotação, Jornada, Contrato e Gestão válidos na data do complemento"));
        JsonNode destino = dados(executar(c.localB, get(rota(recurso)).param("data", DATA.toString()), 200,
                "Gestão posterior não assume o conteúdo histórico do complemento"));
        assertAll(
                () -> assertEquals(1, origem.path("content").size(), origem.toString()),
                () -> assertEquals(id, origem.path("content").path(0).path("id").asLong(), origem.toString()),
                () -> assertEquals(0, destino.path("content").size(), destino.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void gestaoHistoricaPodeCadastrarComplementoMesmoDepoisDaTransferencia(String recurso) throws Exception {
        Cenario c = criarCenario();
        transferir(c, DATA.plusDays(1));
        JsonNode criado = enviar(c.localA, rota(recurso), corpo(c, recurso, DATA), 201,
                "Autorização atual da Gestão A alcança o dia que ainda pertencia à Gestão A");
        assertEquals(c.empregadoA.getId().longValue(), criado.path("empregadoId").asLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void gestaoAtualNaoPodeCadastrarComplementoEmDiaDaGestaoAnterior(String recurso) throws Exception {
        Cenario c = criarCenario();
        transferir(c, DATA.plusDays(1));
        enviar(c.localB, rota(recurso), corpo(c, recurso, DATA), 403,
                "A projeção atual na Gestão B não concede escrita sobre o dia histórico da Gestão A");
    }

    @ParameterizedTest
    @CsvSource({"excecoes,PUT", "adicionais,PUT", "excecoes,DELETE", "adicionais,DELETE"})
    void gestaoAtualNaoCapturaNemExcluiComplementoDeOutraGestao(String recurso, String metodo) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso, DATA);
        transferir(c, DATA.plusDays(1));
        if (metodo.equals("DELETE")) {
            executar(c.localB, delete(rota(recurso) + "/{id}", id), 403,
                    "DELETE autoriza o contexto original, mesmo com a projeção atual na Gestão B");
        } else {
            // O destino pertence de fato a B. A origem A precisa ser autorizada antes da troca.
            Map<String,Object> mudanca = recurso.equals("excecoes")
                    ? mapa("dataInicio", DATA.plusDays(1), "dataFim", DATA.plusDays(1), "observacao", "Captura indevida")
                    : mapa("dataReferencia", DATA.plusDays(1), "minutos", 90);
            executar(c.localB, put(rota(recurso) + "/{id}", id).contentType(APPLICATION_JSON).content(json(mudanca)),
                    403, "PUT não transfere um fato da Gestão A para B por simples mudança da data");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void gestaoHistoricaPodeManterComplementoDepoisDaTransferencia(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso, DATA);
        transferir(c, DATA.plusDays(1));
        Map<String,Object> mudanca = recurso.equals("excecoes")
                ? mapa("observacao", "Conferência histórica autorizada") : mapa("minutos", 90);
        mudanca.put("justificativa", "Conferência documental corrigiu a fonte histórica da Gestão A");
        JsonNode alterado = dados(executar(c.localA, put(rota(recurso) + "/{id}", id)
                .contentType(APPLICATION_JSON).content(json(mudanca)), 200,
                "Gestão histórica continua podendo manter o fato sob sua autorização atual"));
        assertEquals(id, alterado.path("id").asLong());
        executar(c.localA, delete(rota(recurso) + "/{id}", id).param("justificativa", "Fonte histórica cancelada após conferência"), 204,
                "O comando de exclusão mantém o contrato HTTP e exige o escopo histórico");
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void cadastroExigeVinculoEAlocacaoValidosNaDataDoComplemento(String recurso) throws Exception {
        Cenario c = criarCenario();
        enviar(c.localA, rota(recurso), corpo(c, recurso, INICIO.minusDays(1)), 409,
                "Possuir um Empregado atual não autoriza lançamento anterior ao Vínculo e à Alocação");
    }

    @Test
    void excecaoDeVariosDiasExigeAutorizacaoEmTodosOsDiasAtingidos() throws Exception {
        Cenario c = criarCenario();
        transferir(c, DATA.plusDays(1));
        Map<String,Object> pedido = corpo(c, "excecoes", DATA);
        pedido.put("dataFim", DATA.plusDays(2));
        enviar(c.localB, rota("excecoes"), pedido, 403,
                "Exceção indivisível que atravessa A e B não é autorizada apenas pela Gestão do último dia");
        JsonNode propria = enviar(c.localB, rota("excecoes"), corpo(c, "excecoes", DATA.plusDays(1)), 201,
                "Controle: Gestão B pode registrar o período inteiramente posterior à transferência");
        assertEquals(DATA.plusDays(1).toString(), propria.path("dataInicio").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void perdaDeCoberturaOperacionalNaoApagaContextoComprovadoParaConsulta(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso, DATA);
        Map<String,Object> acao = mapa("dataEfetiva", DATA, "dataAto", RelogioSistema.hoje(),
                "motivo", "Inativação administrativa documentada do período", "referenciaSei", "SEI-COMPLEMENTO-INATIVO");
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/inativar", acao, 200,
                "Prévia real antes de retirar a cobertura operacional do complemento já registrado");
        acao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        acao.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", acao, 200,
                "Inativação retroativa é fato administrativo, sem apagar prova anterior");
        // Controle adicional: uma projeção posterior não substitui a prova histórica preservada.
        Empregado atual = em.find(Empregado.class, c.empregadoA.getId());
        atual.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoB.getId()));
        atual.setLotacao(em.getReference(Lotacao.class, c.lotacaoB.getId()));
        atual.setJornada(em.getReference(Jornada.class, c.jornadaB.getId()));
        atual.setCargo(em.getReference(Cargo.class, c.cargoB.getId()));
        atual.getContratos().clear();
        atual.getContratos().add(em.getReference(Contrato.class, c.contratoB.getId()));
        em.flush(); em.clear();
        JsonNode origem = dados(executar(c.localA, get(rota(recurso)).param("data", DATA.toString()), 200,
                "Inatividade operacional não impede consulta da prova no escopo histórico comprovado"));
        JsonNode destino = dados(executar(c.localB, get(rota(recurso)).param("data", DATA.toString()), 200,
                "A projeção atual também não captura a prova que perdeu cobertura operacional"));
        assertAll(
                () -> assertEquals(1, origem.path("content").size(), origem.toString()),
                () -> assertEquals(id, origem.path("content").path(0).path("id").asLong()),
                () -> assertEquals(0, destino.path("content").size(), destino.toString()));
    }

    private long criar(Cenario c, String recurso, LocalDate data) throws Exception {
        return enviar(c.localA, rota(recurso), corpo(c, recurso, data), 201,
                "Complemento original válido registrado sob a Gestão A").path("id").asLong();
    }

    private Map<String,Object> corpo(Cenario c, String recurso, LocalDate data) {
        return recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", data, "dataFim", data,
                       "tipo", "FOLGA_REMUNERADA", "observacao", "Compensação histórica documentada")
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", data, "tipo", "RT",
                       "minutos", 60, "quantidade", 1, "justificativa", "Serviço histórico documentado");
    }

    private String rota(String recurso) { return "/api/frequencias-" + recurso; }

    private void transferir(Cenario c, LocalDate data) throws Exception {
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência documental posterior ao dia do complemento", "referenciaSei", "SEI-COMPLEMENTO-TRANSFERIDO",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", c.composicaoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência real reconstrói o escopo A antes da data e B a partir dela");
        // A projeção é materializada explicitamente a partir da transferência já publicada.
        // ProjecaoOperacionalPessoaServico conserva a organização de Empregados existentes;
        // chamar aquele método não constitui a pré-condição de organização atual B.
        AlocacaoVinculo destino = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v "
                        + "and a.dataInicio=:d and a.estadoNegocial=:s", AlocacaoVinculo.class)
                .setParameter("v", c.vinculoA.getId()).setParameter("d", data)
                .setParameter("s", EstadoNegocialAlocacao.REGISTRADA).getSingleResult();
        Contrato contratoAtual = em.find(Contrato.class, c.contratoA.getId());
        assertEquals(c.gestaoB.getId(), contratoAtual.getGestaoLocal().getId());
        Empregado atual = em.find(Empregado.class, c.empregadoA.getId());
        atual.setGestaoLocal(contratoAtual.getGestaoLocal());
        atual.setLotacao(destino.getLotacao());
        atual.setJornada(destino.getJornada());
        atual.setCargo(destino.getCargo());
        atual.getContratos().clear(); atual.getContratos().add(contratoAtual);
        em.flush(); em.clear();
        assertEquals(c.gestaoB.getId(), em.find(Empregado.class, c.empregadoA.getId()).getGestaoLocal().getId(),
                "O caso requer a projeção atual em B, mantendo a linha temporal anterior em A");
    }
}
