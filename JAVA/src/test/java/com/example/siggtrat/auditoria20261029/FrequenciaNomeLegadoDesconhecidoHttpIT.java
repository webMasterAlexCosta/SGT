package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** RI07/HIS004: nome ausente em prova legada nao recebe o nome atual como passado conhecido. */
class FrequenciaNomeLegadoDesconhecidoHttpIT extends ContratosHttpBase {
    @Autowired AuditoriaFrequenciaServico auditoria;

    @ParameterizedTest
    @ValueSource(strings = {"frequencia", "excecoes", "adicionais"})
    void consultaDeFonteLegadaComNomeAusenteNaoUsaNomeAtualDaPessoa(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criarFonteLegada(c, recurso);
        String atual = corrigirNome(c);
        JsonNode consultada = consultarFonte(c, recurso, id);
        assertTrue(nome(consultada, recurso).isNull(),
                "Identidade e escopo comprovados permitem consulta, mas nao demonstram o nome historico ausente: " + consultada);
        assertFalse(consultada.toString().contains(atual));
        em.flush(); em.clear();
        assertNull(nomePersistido(recurso, id), "GET nao preenche a fotografia original ausente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"frequencia", "excecoes", "adicionais"})
    void retificacaoDeOutroAtributoConservaNomeDesconhecidoNaRaizENasVersoes(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criarFonteLegada(c, recurso);
        String atual = corrigirNome(c);
        JsonNode resposta;
        if (recurso.equals("frequencia")) {
            resposta = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                    "dataReferencia", DATA, "status", "PRESENCA", "justificativaRetificacao", "Documento retificou somente a presenca",
                    "chaveIdempotencia", "nome-desconhecido-" + UUID.randomUUID()), 200,
                    "Retificacao do status nao autoriza preencher nome original ausente");
        } else {
            Map<String,Object> pedido = recurso.equals("excecoes")
                    ? mapa("observacao", "Observacao retificada por documento posterior") : mapa("minutos", 90);
            pedido.put("justificativa", "Documento retificou atributo sem recuperar o nome historico");
            resposta = dados(executar(c.localA, put(rota(recurso) + "/{id}", id).contentType(APPLICATION_JSON).content(json(pedido)),
                    200, "Retificacao deve incorporar o legado sem inventar o nome ausente"));
        }
        em.flush(); em.clear();
        JsonNode eventos = consultar(c.localA, rota(recurso) + "/" + id + "/eventos",
                "Historia conserva a ausencia de nome na prova original e na retificacao de outro atributo");
        assertEquals(2, eventos.size());
        assertAll(
                () -> assertTrue(nome(resposta, recurso).isNull(), resposta.toString()),
                () -> assertNull(nomePersistido(recurso, id)),
                () -> assertFalse(eventos.toString().contains(atual), eventos.toString()));
        if (recurso.equals("frequencia")) {
            assertTrue(eventos.get(0).path("snapshotNovo").asText().contains("empregadoNome=\\N;"));
            assertTrue(eventos.get(1).path("snapshotAnterior").asText().contains("empregadoNome=\\N;"));
            assertTrue(eventos.get(1).path("snapshotNovo").asText().contains("empregadoNome=\\N;"));
            assertEquals("PRESENCA", em.find(Frequencia.class, id).getStatus());
        } else {
            assertEquals("INCORPORACAO_LEGADO", eventos.get(0).path("tipoOperacao").asText());
            assertEquals("RETIFICACAO", eventos.get(1).path("tipoOperacao").asText());
            assertTrue(eventos.get(0).path("fotografia").path("empregadoNome").isNull());
            assertTrue(eventos.get(1).path("fotografia").path("empregadoNome").isNull());
            assertTrue(eventos.get(0).path("usuarioId").isNull(), "Incorporacao nao inventa autoria original");
            assertEquals(eventos.get(0).path("hashVersao"), eventos.get(1).path("hashAnterior"));
        }
        JsonNode consultada = consultarFonte(c, recurso, id);
        assertTrue(nome(consultada, recurso).isNull());
    }

    private long criarFonteLegada(Cenario c, String recurso) {
        long id;
        if (recurso.equals("frequencia")) {
            Frequencia f = frequencia(c, false, DATA, "FALTA");
            f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            f.setJornadaSnapshotDescricao(c.jornadaA.getDescricao());
            f.setJornadaSnapshotDiaTrabalho(true);
            f.setJornadaSnapshotEntrada1("08:00"); f.setJornadaSnapshotSaida1("12:00");
            f.setJornadaSnapshotEntrada2("13:00"); f.setJornadaSnapshotSaida2("17:00");
            f.setLotacaoSnapshotNome(c.lotacaoA.getNome()); f.setLotacaoSnapshotMunicipio(c.lotacaoA.getMunicipio());
            f.setCargoSnapshotNome(c.cargoA.getNome()); f.setContratoSnapshotNumero(c.contratoA.getNumeroContrato());
            f.setMinutosFisicos(0); f.setMinutosNoturnosReais(0); f.setMinutosAdicionais(0); f.setMinutosPagaveis(0);
            f.setEmpregadoSnapshotNome(null);
            em.flush();
            auditoria.registrar(f, TipoEventoFrequencia.REGISTRO, null,
                    "Prova legada sem registro do nome original", null, null, null);
            id=f.getId();
        } else if (recurso.equals("excecoes")) {
            FrequenciaExcecao e = new FrequenciaExcecao();
            e.setEmpregado(c.empregadoA); e.setDataInicio(DATA); e.setDataFim(DATA);
            e.setTipo("FOLGA_REMUNERADA"); e.setStatus("APROVADO"); e.setObservacao("Observacao legada original");
            e.setEmpregadoNomeFotografia(null); em.persist(e); id=e.getId();
        } else {
            FrequenciaAdicional a = new FrequenciaAdicional();
            a.setEmpregado(c.empregadoA); a.setDataReferencia(DATA); a.setTipo("RT");
            a.setStatus("CALCULADO"); a.setMinutos(60); a.setQuantidade(BigDecimal.ONE);
            a.setJustificativa("Justificativa legada original"); a.setEmpregadoNomeFotografia(null);
            em.persist(a); id=a.getId();
        }
        em.flush(); em.clear();
        assertNull(nomePersistido(recurso,id));
        assertEquals(c.pessoaA.getId(), em.find(Empregado.class, c.empregadoA.getId()).getPessoaCanonica().getId());
        return id;
    }

    private String corrigirNome(Cenario c) throws Exception {
        String atual = "NOME_ATUAL_SEM_PROVA_HISTORICA_" + UUID.randomUUID();
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", atual, "motivo", "Correcao legal documentada do cadastro atual",
                "evidencia", "SEI-NOME-ATUAL", "autorizacao", "Despacho da Gestao Local",
                "impactosEncontrados", "Identidade atual; fontes antigas sem nome comprovado permanecem desconhecidas"),
                200, "Correcao real N1 para N2 nao prova o nome da fonte legada");
        em.flush(); em.clear();
        assertEquals(atual, em.find(Pessoa.class, c.pessoaA.getId()).getNome());
        assertEquals(atual, em.find(Empregado.class, c.empregadoA.getId()).getNome());
        return atual;
    }

    private JsonNode consultarFonte(Cenario c, String recurso, long id) throws Exception {
        JsonNode pagina = dados(executar(c.localA, get(rota(recurso)).param("data", DATA.toString())
                        .param("contratoId", c.contratoA.getId().toString()), 200,
                "Consulta autorizada de fonte com identidade e escopo historico comprovados"));
        assertEquals(1, pagina.path("content").size());
        JsonNode fonte = pagina.path("content").get(0);
        assertEquals(id, fonte.path("id").asLong());
        return fonte;
    }

    private JsonNode nome(JsonNode fonte, String recurso) {
        return recurso.equals("frequencia") ? fonte.path("empregado").path("nome") : fonte.path("empregadoNome");
    }

    private String nomePersistido(String recurso, long id) {
        return switch (recurso) {
            case "frequencia" -> em.find(Frequencia.class,id).getEmpregadoSnapshotNome();
            case "excecoes" -> em.find(FrequenciaExcecao.class,id).getEmpregadoNomeFotografia();
            default -> em.find(FrequenciaAdicional.class,id).getEmpregadoNomeFotografia();
        };
    }

    private String rota(String recurso) {
        return recurso.equals("frequencia") ? "/api/frequencias" : "/api/frequencias-" + recurso;
    }
}
