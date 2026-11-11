package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** AUD-06/FRE-015/PER-010: o escopo corrigido precede filtro, total e paginação. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud06FrequenciaEscopoPaginaHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"dia", "mensal", "intervalo"})
    void consultaUsaResponsabilidadeRetroativaSemReescreverFotografia(String rota) throws Exception {
        Preparado f = preparar();
        JsonNode destino = consultarPagina(f.c().localB, f, rota, 0, 2, false);
        JsonNode origem = consultarPagina(f.c().localA, f, rota, 0, 2, false);
        int totalDestino = rota.equals("dia") ? 1 : 5;
        int totalOrigem = rota.equals("dia") ? 0 : 2;
        assertEquals(totalDestino, destino.path("totalElements").asInt(), destino.toString());
        assertEquals(Math.min(totalDestino, 2), destino.path("content").size());
        assertEquals(totalOrigem, origem.path("totalElements").asInt(), origem.toString());
        assertEquals(f.ids().get(rota.equals("dia") ? 3 : 2).longValue(),
                destino.path("content").get(0).path("id").asLong());
        assertEquals(f.c().gestaoA.getId(), emTransacao(() ->
                em.find(Frequencia.class, f.ids().get(3)).getGestaoLocalSnapshotId()),
                "A consulta não altera a fotografia nem a assinatura da frequência fechada");
    }

    @Test
    void paginaIntermediariaETotalContemSomenteLinhasAutorizadas() throws Exception {
        Preparado f = preparar();
        JsonNode pagina = consultarPagina(f.c().localB, f, "intervalo", 1, 2, false);
        assertEquals(5, pagina.path("totalElements").asInt(), pagina.toString());
        assertEquals(3, pagina.path("totalPages").asInt());
        assertEquals(2, pagina.path("content").size());
        assertEquals(f.ids().get(4).longValue(), pagina.path("content").get(0).path("id").asLong());
        assertEquals(f.ids().get(5).longValue(), pagina.path("content").get(1).path("id").asLong());
        JsonNode ultima = consultarPagina(f.c().localB, f, "intervalo", 2, 2, true);
        assertEquals(1, ultima.path("content").size());
        assertEquals(f.ids().get(6).longValue(), ultima.path("content").get(0).path("id").asLong());
        assertTrue(ultima.path("last").asBoolean());
    }

    @Test
    void filtrosOrganizacionaisSaoAplicadosAResponsabilidadeCorrigida() throws Exception {
        Preparado f = preparar();
        var filtro = consulta(f, "intervalo", 0, 10, false)
                .param("gestaoLocalId", f.c().gestaoB.getId().toString())
                .param("lotacaoId", f.c().lotacaoB.getId().toString())
                .param("jornadaId", f.c().jornadaB.getId().toString())
                .param("superintendenciaId", f.c().seB.getId().toString());
        JsonNode nacional = dados(executar(f.c().nacional, filtro, 200,
                "Filtros e contagem nacionais respeitam a verdade temporal corrigida"));
        assertEquals(5, nacional.path("totalElements").asInt(), nacional.toString());
        JsonNode estadual = consultarPagina(f.c().estadualB, f, "intervalo", 0, 10, false);
        assertEquals(5, estadual.path("totalElements").asInt());
        JsonNode operacional = consultarPagina(f.c().operacionalB, f, "intervalo", 0, 10, false);
        assertEquals(5, operacional.path("totalElements").asInt());
        executar(f.c().presidente, consulta(f, "intervalo", 0, 10, false), 403,
                "O perfil sem consulta de frequência continua sem acesso");
    }

    @ParameterizedTest
    @ValueSource(strings = {"eventos", "evidencias-integridade"})
    void consultaPorIdNaoConcedeAcessoAoEscopoAnteriorDaFotografia(String rota) throws Exception {
        Preparado f = preparar();
        String path = "/api/frequencias/" + f.ids().get(3) + "/" + rota;
        executar(f.c().localA, get(path), 404,
                "A Gestão sem responsabilidade na data não recebe a prova pessoal");
        executar(f.c().localB, get(path), 200,
                "A Gestão responsável na data consulta a prova original preservada");
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        List<Long> ids = new ArrayList<>();
        for (int dia = 0; dia < 7; dia++) {
            var data = DATA.plusDays(dia);
            // FRE-001/FRE-015: a preparação respeita a Jornada histórica.
            // AuditoriaHttpSuporte.jornada define trabalho de segunda a sexta.
            // Manter sete registros inclui as duas folgas na prova de escopo/paginação.
            boolean diaTrabalho = data.getDayOfWeek().getValue() <= 5;
            var requisicao = mapa(
                    "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data,
                    "status", diaTrabalho ? "PRESENCA" : "DESCANSO",
                    "observacao", "Prova sintética restrita " + dia,
                    "justificativaRetificacao", "Registro histórico documentado para auditoria de escopo",
                    "chaveIdempotencia", "aud06-" + UUID.randomUUID());
            if (diaTrabalho) {
                requisicao.put("horasTrabalhadas", "04:00");
                requisicao.put("horarioSaida", "12:00");
            }
            ids.add(enviar(c.localA, "/api/frequencias", requisicao, 200,
                    "Registro compatível com a Jornada antes de conhecer o ato retroativo").path("id").asLong());
        }
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", DATA.plusDays(2), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Documento comprovou responsabilidade de B desde o terceiro dia",
                "referenciaSei", "SEI-AUD06-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200,
                "Transferência retroativa preserva frequências fechadas e muda responsabilidade da data");
        return new Preparado(c, List.copyOf(ids));
    }

    private JsonNode consultarPagina(UsuarioAplicacao usuario, Preparado f, String rota,
                                     int pagina, int tamanho, boolean semContagem) throws Exception {
        return dados(executar(usuario, consulta(f, rota, pagina, tamanho, semContagem), 200,
                "Autorização temporal é aplicada antes de selecionar e contar a página " + pagina));
    }

    private AbstractMockHttpServletRequestBuilder<?> consulta(Preparado f, String rota, int pagina, int tamanho, boolean semContagem) {
        var req = switch (rota) {
            case "dia" -> get("/api/frequencias").param("data", DATA.plusDays(3).toString());
            case "mensal" -> get("/api/frequencias/mensal").param("mes", "6").param("ano", "2026");
            default -> get("/api/frequencias/intervalo").param("inicio", DATA.toString()).param("fim", DATA.plusDays(6).toString());
        };
        return req.param("contratoId", f.c().contratoA.getId().toString()).param("page", String.valueOf(pagina))
                .param("size", String.valueOf(tamanho)).param("skipCount", String.valueOf(semContagem));
    }

    private record Preparado(Cenario c, List<Long> ids) {}
}
