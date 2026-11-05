package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class CriteriosCOcorrenciasHttpIT extends CriteriosCHttpBase {
    private UsuarioAplicacao leitor(Cenario c, String perfil) {
        return switch (perfil) {
            case "ADMINISTRADOR" -> c.admin;
            case "GESTOR_NACIONAL" -> c.nacional;
            case "PRESIDENTE" -> c.presidente;
            case "GESTOR_ESTADUAL" -> c.estadualA;
            default -> throw new IllegalArgumentException(perfil);
        };
    }

    private JsonNode criar(PessoaPreparada x, String marcador, String classe) throws Exception {
        return dados(executar(x.c().localA, ocorrencia(x, x.fato(), 1, "c-" + UUID.randomUUID(),
                marcador, "restrito-c.txt", bytes(marcador), classe), 201,
                "Controle positivo: ocorrência válida criada pelo Gestor Local"));
    }

    private long concederLeitura(PessoaPreparada x, UsuarioAplicacao u, String finalidade) throws Exception {
        String tipo = u.getPerfil().name().equals("ADMINISTRADOR")
                ? "INTERVENCAO_NEGOCIAL_TECNICA" : "ACESSO_DADOS_PESSOAIS_RESTRITOS";
        JsonNode p = enviar(x.c().admin, "/api/permissoes-excepcionais", mapa("tipo", tipo,
                "usuarioBeneficiarioId", u.getId(), "contratoId", x.contrato().id(),
                "finalidade", finalidade, "justificativa", "Ensaio sintético C de necessidade específica",
                "autorizacaoAdministrativa", "ATO-SINTETICO-C-SEM-VALOR-ADMINISTRATIVO",
                "validoAte", Instant.now().plusSeconds(1800)), 201, "Concessão pessoal válida e específica");
        assertTrue(p.path("ativa").isBoolean() && p.path("ativa").asBoolean()); return p.path("id").asLong();
    }

    private JsonNode usos(PessoaPreparada x, long concessao) throws Exception {
        return consultar(x.c().admin, "/api/permissoes-excepcionais/" + concessao + "/usos", "Trilha da mesma concessão");
    }

    @ParameterizedTest(name = "{0}: finalidade ausente/branca com concessão vigente")
    @ValueSource(strings = {"GESTOR_NACIONAL", "PRESIDENTE", "ADMINISTRADOR"})
    void finalidadeAusenteOuBrancaNaoBaixaMesmaConcessaoPositiva(String perfil) throws Exception {
        PessoaPreparada x = pessoaPreparada(); UsuarioAplicacao u = leitor(x.c(), perfil);
        String finalidade = "Conferência documental C " + x.vinculo(), marca = "PRIVADO-C-" + UUID.randomUUID();
        JsonNode o = criar(x, marca, "PESSOAL_RESTRITO"); long anexo = o.path("anexos").get(0).path("id").asLong();
        String rota = "/api/ocorrencias/anexos/" + anexo;
        executar(u, get(rota).param("finalidade", finalidade), 403, "Controle sem concessão ainda não lê bytes pessoais");
        long grant = concederLeitura(x, u, finalidade);
        assertEquals(0, usos(x, grant).size());
        assertArrayEquals(bytes(marca), executar(u, get(rota).param("finalidade", finalidade), 200,
                "Controle positivo: mesmo ator, arquivo e concessão com finalidade").getResponse().getContentAsByteArray());
        JsonNode usosAntes = usos(x, grant); assertEquals(1, usosAntes.size());
        JsonNode estado = estadoOcorrencias(x.empregado()); var files = arquivos();
        for (String valor : new String[]{null, "", "   "}) {
            var req = get(rota); if (valor != null) req.param("finalidade", valor);
            var r = executar(u, req, 403, "DOC-010/PER-005/PER-011: variar somente finalidade para ausente/branca");
            assertFalse(Arrays.equals(bytes(marca), r.getResponse().getContentAsByteArray()));
            assertEquals(usosAntes, usos(x, grant), "Recusa não registra um uso autorizado inexistente");
            semEfeitoOcorrencia(x, estado, files);
        }
        assertArrayEquals(bytes(marca), executar(u, get(rota).param("finalidade", finalidade), 200,
                "Controle positivo posterior mantém a mesma concessão vigente").getResponse().getContentAsByteArray());
        JsonNode usosDepois = usos(x, grant); assertEquals(2, usosDepois.size());
        for (JsonNode uso : usosDepois) {
            assertEquals(u.getId().longValue(), uso.path("usuarioId").asLong());
            assertEquals("AnexoOcorrencia", uso.path("tipoRecurso").asText());
            assertEquals(anexo, uso.path("recursoId").asLong()); assertEquals(finalidade, uso.path("finalidade").asText());
        }
        semEfeitoOcorrencia(x, estado, files);
        registrar("finalidade-" + perfil + "-" + x.vinculo(), mapa("perfil", perfil, "concessao", grant,
                "anexo", anexo, "recusas", List.of("ausente:403", "vazia:403", "branca:403"),
                "controles", List.of(200, 200), "usos", usosDepois, "shaBytes", sha(bytes(marca))));
    }

    @ParameterizedTest(name = "{0}: GET não habilita criação/cancelamento")
    @ValueSource(strings = {"GESTOR_ESTADUAL", "PRESIDENTE"})
    void consultaInstitucionalNaoHabilitaCriacaoNemCancelamento(String perfil) throws Exception {
        PessoaPreparada x = pessoaPreparada(); UsuarioAplicacao u = leitor(x.c(), perfil);
        JsonNode existente = criar(x, "OCORRENCIA-CONTROLE-" + x.vinculo(), "NEGOCIAL");
        long id = existente.path("id").asLong(); String key = "c-negacao-" + UUID.randomUUID();
        LocalDate outraData = x.fato().minusDays(2); String marca = "NOVA-INTENCAO-VALIDA-" + x.vinculo();
        JsonNode estado = estadoOcorrencias(x.empregado()); var files = arquivos();
        executar(u, ocorrencia(x, outraData, 1, key, marca, "nova-prova.txt", bytes(marca), "NEGOCIAL"), 403,
                "PER-002/PER-004 criação válida negada antes de qualquer GET institucional");
        semEfeitoOcorrencia(x, estado, files);
        JsonNode consultada = dados(executar(u, get("/api/ocorrencias/{id}", id), 200, "GET institucional permitido"));
        assertEquals(id, consultada.path("ocorrencia").path("id").asLong());
        executar(u, ocorrencia(x, outraData, 1, key, marca, "nova-prova.txt", bytes(marca), "NEGOCIAL"), 403,
                "PER-002/PER-004 GET não habilitou a mesma intenção de criação");
        semEfeitoOcorrencia(x, estado, files);
        executar(u, delete("/api/ocorrencias/{id}", id), 403,
                "PER-002/PER-004 GET não habilitou cancelamento");
        semEfeitoOcorrencia(x, estado, files);
        JsonNode aceito = dados(executar(x.c().localA, ocorrencia(x, outraData, 1, key, marca,
                "nova-prova.txt", bytes(marca), "NEGOCIAL"), 201,
                "Controle: mesma intenção e chave são válidas para o Gestor Local"));
        assertNotEquals(id, aceito.path("id").asLong());
        executar(x.c().localA, delete("/api/ocorrencias/{id}", id), 204,
                "Controle: cancelamento existente é válido ao Gestor Local");
        registrar("sem-escrita-" + perfil + "-" + x.vinculo(), mapa("perfil", perfil,
                "ocorrenciaExistente", id, "estadoDuranteRecusas", estado, "arquivosDuranteRecusas", files,
                "novaOcorrenciaControle", aceito.path("id"), "estadoFinal", estadoOcorrencias(x.empregado())));
    }

    @Test
    void listaEstadualIncluiIdETotalMasNaoNarrativaSeiOuAnexoPessoal() throws Exception {
        PessoaPreparada x = pessoaPreparada(); String marca = "NARRATIVA-PRIVADA-" + UUID.randomUUID();
        JsonNode o = criar(x, marca, "PESSOAL_RESTRITO"); long id = o.path("id").asLong();
        JsonNode estado = estadoOcorrencias(x.empregado()); var files = arquivos();
        JsonNode controle = dados(executar(x.c().localA, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado())).param("size", "5"), 200,
                "Controle de lista completa com narrativa realmente persistida"));
        assertEquals(1, controle.path("totalElements").asLong()); assertTrue(controle.toString().contains(marca));
        assertTrue(controle.toString().contains("SEI-PRIVADO-C-" + x.vinculo()));
        JsonNode lista = dados(executar(x.c().estadualA, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado())).param("size", "5"), 200,
                "PER-006:003/004 estadual recebe a ocorrência minimizada com ID e total reais"));
        assertEquals(1, lista.path("totalElements").asLong()); assertEquals(1, lista.path("content").size());
        JsonNode item = lista.path("content").get(0); assertEquals(id, item.path("id").asLong());
        for (String segredo : List.of(marca, "SEI-PRIVADO-C-" + x.vinculo(), "restrito-c.txt"))
            assertFalse(lista.toString().contains(segredo), "Marcador privado vazou na lista: " + segredo);
        assertTrue(item.path("observacao").isNull()); assertTrue(item.path("numeroSei").isNull());
        assertTrue(item.path("anexos").isEmpty());
        semEfeitoOcorrencia(x, estado, files);
        JsonNode reconsulta = dados(executar(x.c().localA, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado())).param("size", "5"), 200,
                "Minimização não remove os dados da fonte"));
        assertEquals(controle, reconsulta);
        registrar("lista-estadual-" + x.vinculo(), mapa("ocorrencia", id, "listaEstadual", lista,
                "shaFonteAntesEDepois", sha(json(estado).getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    @Test
    void fronteiraDeGestaoEmDoisDiasRecusaCriacaoECancelamentoParciais() throws Exception {
        PessoaPreparada x = pessoaPreparada(); LocalDate fronteira = x.fato().minusDays(3);
        LocalDate primeiroDia = fronteira.minusDays(1); String marca = "DOIS-DIAS-" + x.vinculo();
        JsonNode o = dados(executar(x.c().localA, ocorrencia(x, primeiroDia, 2, "c-multi-" + UUID.randomUUID(),
                marca, "multidia.txt", bytes(marca), "NEGOCIAL"), 201,
                "Controle: antes da transferência a Gestão A cobre ambos os dias"));
        long id = o.path("id").asLong(); long anexo = o.path("anexos").get(0).path("id").asLong();
        assertEquals(2, estadoOcorrencias(x.empregado()).path("contextos").size());
        conceder(x.c(), x.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(x.c().localA, base(x.contrato().id()) + "/transferir-gestao", mapa(
                "dataEfetiva", fronteira, "dataAto", com.example.siggtrat.utilitario.RelogioSistema.hoje(),
                "gestaoLocalDestinoId", x.c().gestaoB.getId(), "tipoContratacaoDestinoId", x.c().tipoB.getId(),
                "motivo", "Fronteira documental sintética no segundo dia da ocorrência", "referenciaSei", "SEI-C-MULTIDIA",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", x.contrato().composicao1(), "lotacaoDestinoId", x.c().lotacaoB.getId(),
                        "jornadaDestinoId", x.c().jornadaB.getId(), "cargoDestinoId", x.c().cargoB.getId()))),
                200, "Transferência real e retroativa define A no primeiro dia e B no segundo");
        JsonNode estado = estadoOcorrencias(x.empregado()); var files = arquivos();
        for (UsuarioAplicacao u : List.of(x.c().localA, x.c().localB)) {
            executar(u, ocorrencia(x, primeiroDia, 2, "c-neg-multi-" + u.getId(), "Nova intenção multi-dia",
                    "multidia-novo.txt", bytes("NOVA INTENCAO"), "NEGOCIAL"), 403,
                    "PER-001:004 criação indivisível requer todos os dias da Gestão");
            semEfeitoOcorrencia(x, estado, files);
            executar(u, delete("/api/ocorrencias/{id}", id), 403,
                    "PER-001:004 cancelamento indivisível não aceita autorização só de um dia");
            semEfeitoOcorrencia(x, estado, files);
        }
        assertArrayEquals(bytes(marca), executar(x.c().nacional, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Prova negocial preservada e consultável pelo perfil nacional").getResponse().getContentAsByteArray());
        // Both owners have legal single-day controls; no invented multi-Gestão local role is granted.
        JsonNode a = dados(executar(x.c().localA, ocorrencia(x, primeiroDia.minusDays(1), 1,
                "c-dia-a-" + UUID.randomUUID(), "Dia somente A", "a.txt", bytes("A"), "NEGOCIAL"), 201,
                "Controle positivo de criação dentro da Gestão A efetiva"));
        JsonNode b = dados(executar(x.c().localB, ocorrencia(x, fronteira.plusDays(1), 1,
                "c-dia-b-" + UUID.randomUUID(), "Dia somente B", "b.txt", bytes("B"), "NEGOCIAL"), 201,
                "Controle positivo de criação dentro da Gestão B efetiva"));
        executar(x.c().localA, delete("/api/ocorrencias/{id}", a.path("id").asLong()), 204, "Controle cancelamento local A");
        executar(x.c().localB, delete("/api/ocorrencias/{id}", b.path("id").asLong()), 204, "Controle cancelamento local B");
        registrar("multidia-gestao-" + x.vinculo(), mapa("ocorrenciaIndivisivel", id,
                "fronteira", fronteira, "estadoDuranteRecusas", estado, "shaArquivoPreservado", sha(bytes(marca)),
                "controlesDiaA", a.path("id"), "controlesDiaB", b.path("id")));
    }

    @Test
    void reenvioSemChaveNormalizaBordasEIdentificaArquivos() throws Exception {
        PessoaPreparada x = pessoaPreparada(); String texto = "Intenção documental C " + x.vinculo();
        byte[] conteudo = bytes(texto); String nome = "mesma-fonte.txt";
        JsonNode primeiro = dados(executar(x.c().localA, ocorrencia(x, x.fato(), 1, null,
                "  " + texto + "  ", nome, conteudo, "NEGOCIAL"), 201, "AUD-005 controle sem chave explícita"));
        JsonNode estado = estadoOcorrencias(x.empregado()); var files = arquivos();
        JsonNode segundo = dados(executar(x.c().localA, ocorrencia(x, x.fato(), 1, null,
                texto, nome, conteudo, "NEGOCIAL"), 201,
                "AUD-005 mesmo digest: normalização apenas das bordas, não whitespace interno inventado"));
        assertEquals(primeiro, segundo); semEfeitoOcorrencia(x, estado, files);
        String key = "c-explicita-" + UUID.randomUUID(); LocalDate outroDia = x.fato().minusDays(2);
        JsonNode explicita = dados(executar(x.c().localA, ocorrencia(x, outroDia, 1, key,
                texto, nome, conteudo, "NEGOCIAL"), 201, "Controle de identidade de arquivo com chave explícita"));
        JsonNode antesVariantes = estadoOcorrencias(x.empregado()); var filesVariantes = arquivos();
        executar(x.c().localA, ocorrencia(x, outroDia, 1, key, texto, nome, bytes("BYTES DIFERENTES"), "NEGOCIAL"),
                409, "AUD-005 mesma chave com bytes divergentes");
        semEfeitoOcorrencia(x, antesVariantes, filesVariantes);
        executar(x.c().localA, ocorrencia(x, outroDia, 1, key, texto, "outro-nome.txt", conteudo, "NEGOCIAL"),
                409, "AUD-005 mesma chave com identidade nominal do arquivo divergente");
        semEfeitoOcorrencia(x, antesVariantes, filesVariantes);
        executar(x.c().localA, ocorrencia(x, outroDia, 1, key, texto, nome, conteudo, "PESSOAL_RESTRITO"),
                409, "AUD-005 mesma chave com classificação documental divergente");
        semEfeitoOcorrencia(x, antesVariantes, filesVariantes);
        JsonNode repetir = dados(executar(x.c().localA, ocorrencia(x, outroDia, 1, key, texto, nome, conteudo, "NEGOCIAL"),
                201, "Controle positivo final da intenção original"));
        assertEquals(explicita, repetir); semEfeitoOcorrencia(x, antesVariantes, filesVariantes);
        registrar("ocorrencia-idempotencia-" + x.vinculo(), mapa("semChaveId", primeiro.path("id"),
                "comChaveId", explicita.path("id"), "variantesRecusadas", List.of("bytes", "nome", "classificacao"),
                "estadoConfirmado", antesVariantes));
    }
}
