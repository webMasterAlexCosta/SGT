package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

abstract class ContratosHttpBase extends AuditoriaHttpSuporte {
    protected record NovoContrato(long id, long composicao1, Long composicao2, LocalDate inicio, LocalDate fim) {}

    protected Map<String, Object> mapa(Object... pares) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) r.put(String.valueOf(pares[i]), pares[i + 1]);
        return r;
    }

    protected JsonNode enviar(UsuarioAplicacao u, String path, Object body, int status, String regra) throws Exception {
        var result = executar(u, post(path).contentType(MediaType.APPLICATION_JSON).content(json(body)), status, regra);
        JsonNode resposta = dados(result);
        if ((status >= 200 && status < 300 || status == 409)
                && path.matches(".*/(previas/(publicar|inativar)|publicar|inativar|correcoes-cnpj|aditamentos(/previa)?|remanejamentos(/previa)?)$")) {
            var arquivo = java.nio.file.Path.of("target/auditoria-http/contratos-exemplos.jsonl");
            java.nio.file.Files.createDirectories(arquivo.getParent());
            java.nio.file.Files.writeString(arquivo, json(mapa("metodo", "POST", "path", path, "request", body,
                    "status", status, "response_data", resposta,
                    "response_envelope", mapper.readTree(result.getResponse().getContentAsString()))) + "\n",
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        }
        return resposta;
    }

    protected JsonNode consultar(UsuarioAplicacao u, String path, String regra) throws Exception {
        return dados(executar(u, get(path), 200, regra));
    }

    protected String base(long contratoId) { return "/api/contratos/" + contratoId; }

    protected Map<String, Object> rascunho(Cenario c, String numero, int total, LocalDate inicio, LocalDate fim) {
        return mapa("numeroContrato", numero, "anoContrato", inicio.getYear(), "descricao", "Contrato de auditoria HTTP",
                "gestaoLocalId", c.gestaoA.getId(), "tipoContratacaoId", c.tipoA.getId(),
                "cnpjEmpresa", "12345678000190", "razaoSocialEmpresa", "Empresa Original",
                "nomeFantasiaEmpresa", "Original", "dataInicio", inicio, "dataFim", fim, "efetivoPrevisto", total);
    }

    protected Map<String, Object> composicao(Cenario c, long cargoId, int quantidade) {
        return mapa("lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "cargoId", cargoId, "efetivoPrevisto", quantidade);
    }

    protected NovoContrato publicado(Cenario c, int total, Integer primeira, boolean futuro) throws Exception {
        LocalDate inicio = RelogioSistema.hoje().plusDays(futuro ? 10 : -30);
        LocalDate fim = RelogioSistema.hoje().plusDays(120);
        JsonNode draft = enviar(c.localA, "/api/contratos", rascunho(c, "HTTP-" + UUID.randomUUID().toString().substring(0, 8),
                total, inicio, fim), 201, "CON-008 criação de rascunho completo");
        long id = draft.path("id").asLong();
        long comp1 = enviar(c.localA, base(id) + "/composicoes",
                composicao(c, c.cargoA.getId(), primeira == null ? total : primeira), 201, "CMP-001 composição explícita").path("id").asLong();
        Long comp2 = null;
        if (primeira != null) {
            Cargo cargo = new Cargo();
            cargo.setGestaoLocal(c.gestaoA); cargo.setNome("Cargo HTTP " + UUID.randomUUID().toString().substring(0, 8)); cargo.setAtivo(true);
            em.persist(cargo); em.flush();
            // A origem é uma fonte sintética explícita deste cenário retroativo,
            // não uma conclusão extraída da disponibilidade atual do cargo.
            var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO);
            origem.setCatalogoId(cargo.getId()); origem.setDataEfetiva(inicio); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do Cargo secundário desde o início contratual");
            em.persist(origem); em.flush();
            comp2 = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargo.getId(), total - primeira),
                    201, "CMP-001 segunda combinação explícita").path("id").asLong();
        }
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-ORIGINAL", "justificativa", "Publicação integral documentada");
        if (inicio.isBefore(RelogioSistema.hoje())) {
            var previa = enviar(c.localA, base(id) + "/previas/publicar", publicacao, 200, "CON-014 prévia antes do original retroativo");
            publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText()); publicacao.put("confirmarImpacto", true);
        }
        enviar(c.localA, base(id) + "/publicar", publicacao, 200, "CON-008/DOC-001 publicação");
        em.flush();
        return new NovoContrato(id, comp1, comp2, inicio, fim);
    }

    protected Map<String, Object> aditamento(String identificacao, LocalDate data) {
        return mapa("identificacao", identificacao, "dataDocumento", data, "dataEfetiva", data,
                "justificativa", "Alteração documentada de auditoria", "referenciaSei", "SEI-ADI", "confirmarImpacto", true);
    }

    protected JsonNode previaAditamento(Cenario c, long id, Map<String, Object> req) throws Exception {
        em.flush();
        JsonNode p = enviar(c.localA, base(id) + "/aditamentos/previa", req, 200, "ADI-002/AUD-004 prévia por HTTP");
        req.put("tokenPrevia", p.path("tokenPrevia").asText());
        return p;
    }

    protected JsonNode registrarAditamento(Cenario c, long id, Map<String, Object> req) throws Exception {
        previaAditamento(c, id, req);
        JsonNode a = enviar(c.localA, base(id) + "/aditamentos", req, 201, "ADI-001/ADI-003 gravação auditada");
        em.flush();
        return a;
    }

    protected Map<String, Object> remanejamento(NovoContrato n, String identificacao, LocalDate data, int var) {
        return mapa("identificacao", identificacao, "dataEfetiva", data, "justificativa", "Redistribuição explícita",
                "confirmarImpacto", true, "itens", List.of(mapa("composicaoId", n.composicao1(), "variacao", -var),
                        mapa("composicaoId", n.composicao2(), "variacao", var)));
    }

    protected JsonNode previaRemanejamento(Cenario c, long id, Map<String, Object> req) throws Exception {
        em.flush();
        JsonNode p = enviar(c.localA, base(id) + "/remanejamentos/previa", req, 200, "REM-002 prévia soma zero");
        req.put("tokenPrevia", p.path("tokenPrevia").asText());
        return p;
    }

    protected MockMultipartFile metadados(Object body) throws Exception {
        return new MockMultipartFile("metadados", "metadados.json", MediaType.APPLICATION_JSON_VALUE,
                json(body).getBytes(StandardCharsets.UTF_8));
    }

    protected void conceder(Cenario c, long id, String tipo) throws Exception {
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", tipo,
                "usuarioBeneficiarioId", c.localA.getId(), "contratoId", id,
                "finalidade", "Auditoria HTTP", "justificativa", "Ato documental conferido",
                "autorizacaoAdministrativa", "ATO-HTTP", "validoAte", java.time.Instant.now().plusSeconds(7200)),
                201, "permissão técnica específica antes do ato negocial");
    }
}
