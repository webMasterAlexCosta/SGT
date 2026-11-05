package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Contrato operacional do cadastro extraído. Cadeia MVC da aplicação via MockMvc, com filtros,
 * serviço e MySQL; as mutações são confirmadas e relidas em novas transações.
 * Não usa a entidade/DTO/serviço de governança para preparar nem consultar dados.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FornecedorOperacionalHttpIT extends ContratosHttpBase {
    private static final String F = "/api/fornecedores";

    @Test
    void cadastroPersisteIdentidadeEAtualizacaoSemExporRisco() throws Exception {
        var c = criarCenarioPersistido();
        String cnpj = cnpjSintetico();
        var criado = enviar(c.localA, F, mapa("razaoSocial", "  Aaaa   Prestadora operacional  ",
                "cnpj", cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "."
                        + cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12),
                "email", " cadastro@example.test "), 200,
                "Empresa contratada criada pela rota operacional com CNPJ normalizado");
        campos(criado, "id", "razaoSocial", "cnpj", "email", "ativo");
        long id = criado.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(criado.path("cnpj").asText()).isEqualTo(cnpj);
        assertThat(criado.path("razaoSocial").asText()).isEqualTo("Aaaa Prestadora operacional");
        assertThat(criado.path("ativo").asBoolean()).isTrue();
        EstadoFornecedor antes = estadoFornecedor(id);
        assertThat(antes.cnpj()).isEqualTo(cnpj);
        assertThat(antes.email()).isEqualTo("cadastro@example.test");
        assertThat(antes.ativo()).isTrue();

        var atualizado = dados(executar(c.localA, put(F + "/{id}", id).contentType(APPLICATION_JSON)
                        .content(json(mapa("razaoSocial", "Aaaa Prestadora atualizada", "cnpj", cnpj,
                                "email", "novo@example.test", "ativo", false))), 200,
                "Atualização cadastral conserva ID e é confirmada no MySQL"));
        campos(atualizado, "id", "razaoSocial", "cnpj", "email", "ativo");
        EstadoFornecedor depois = estadoFornecedor(id);
        assertThat(depois.id()).isEqualTo(antes.id());
        assertThat(depois.versao()).isGreaterThan(antes.versao());
        assertThat(depois.razao()).isEqualTo("Aaaa Prestadora atualizada");
        assertThat(depois.cnpj()).isEqualTo(cnpj);
        assertThat(depois.ativo()).isFalse();
        long ultimo = enviar(c.localA, F, mapa("razaoSocial", "Zzzz Prestadora operacional",
                "cnpj", cnpjSintetico()), 200, "Segunda empresa para verificar ordenação do catálogo").path("id").asLong();
        JsonNode lista = consultar(c.localA, F, "Catálogo operacional permanece compartilhado e ordenado");
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : lista) {
            campos(item, "id", "razaoSocial", "cnpj", "email", "ativo");
            ids.add(item.path("id").asLong());
        }
        assertThat(ids).contains(id, ultimo);
        assertThat(ids.indexOf(id)).isLessThan(ids.indexOf(ultimo));
    }

    @Test
    void vinculoPersisteNoRascunhoERecusasConservamContratoPublicado() throws Exception {
        var c = criarCenarioPersistido();
        String cnpj = cnpjSintetico();
        long fornecedor = enviar(c.localA, F, mapa("razaoSocial", "Empresa de vínculo operacional",
                "cnpj", cnpj), 200, "Cadastro canônico para vínculo de rascunho").path("id").asLong();
        var inicio = RelogioSistema.hoje().plusDays(10);
        var req = rascunho(c, "FORN-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(50));
        req.put("cnpjEmpresa", cnpj);
        long rascunho = enviar(c.localA, "/api/contratos", req, 201,
                "Rascunho operacional próprio com identidade coerente").path("id").asLong();
        var vinculado = dados(executar(c.localA,
                put("/api/contratos/{id}/fornecedor/{fid}", rascunho, fornecedor), 200,
                "Empresa pode ser selecionada antes da publicação"));
        campos(vinculado, "contratoId", "fornecedor");
        campos(vinculado.path("fornecedor"), "id", "razaoSocial", "cnpj", "email", "ativo");
        assertThat(vinculado.path("contratoId").asLong()).isEqualTo(rascunho);
        assertThat(estadoContrato(rascunho).path("fornecedorId").asLong()).isEqualTo(fornecedor);

        JsonNode publicadoAntes = estadoContrato(c.contratoA.getId());
        assertThat(publicadoAntes.path("fotografias")).isNotEmpty();
        executar(c.localA, put("/api/contratos/{id}/fornecedor/{fid}", c.contratoA.getId(), fornecedor),
                409, "Contrato publicado exige aditamento, conservando GOV-003");
        assertThat(estadoContrato(c.contratoA.getId())).isEqualTo(publicadoAntes);
        JsonNode rascunhoAntes = estadoContrato(rascunho);
        executar(c.localB, put("/api/contratos/{id}/fornecedor/{fid}", rascunho, fornecedor),
                404, "Outra Gestão não pode alterar o vínculo do rascunho");
        assertThat(estadoContrato(rascunho)).isEqualTo(rascunhoAntes);
    }

    @Test
    void edicaoCadastralNaoReescreveFotografiaDeEmpresaPublicada() throws Exception {
        var c = criarCenarioPersistido();
        String cnpj = cnpjSintetico();
        long fornecedor = enviar(c.localA, F, mapa("razaoSocial", "Empresa na publicação",
                "cnpj", cnpj), 200, "Identidade da empresa publicada").path("id").asLong();
        var inicio = RelogioSistema.hoje().plusDays(10);
        Map<String, Object> req = rascunho(c, "FOTO-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(50));
        req.put("fornecedorId", fornecedor);
        req.put("cnpjEmpresa", cnpj);
        req.put("razaoSocialEmpresa", "Empresa na publicação");
        req.put("nomeFantasiaEmpresa", "Empresa publicada");
        long contrato = enviar(c.localA, "/api/contratos", req, 201,
                "Contrato com fotografia empresarial explícita").path("id").asLong();
        enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "Composição original antes da publicação");
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-FORNECEDOR",
                "justificativa", "Publicação sintética explícita para prova de preservação");
        var previa = enviar(c.localA, base(contrato) + "/previas/publicar", publicacao, 200,
                "Prévia da publicação documentada");
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        enviar(c.localA, base(contrato) + "/publicar", publicacao, 200,
                "Publicação produz fato e fotografia persistidos");
        JsonNode antes = estadoContrato(contrato);
        assertThat(antes.path("fotografias")).isNotEmpty();
        assertThat(antes.path("fornecedorId").asLong()).isEqualTo(fornecedor);

        executar(c.localA, put(F + "/{id}", fornecedor).contentType(APPLICATION_JSON).content(json(
                mapa("razaoSocial", "Empresa cadastrada depois", "cnpj", cnpj,
                        "email", "posterior@example.test", "ativo", false))), 200,
                "Cadastro atual pode mudar sem reescrever fato publicado");
        assertThat(estadoFornecedor(fornecedor).razao()).isEqualTo("Empresa cadastrada depois");
        assertThat(estadoContrato(contrato)).as("Identidade, versão, dados, eventos e fotos publicados")
                .isEqualTo(antes);
        var posicao = consultar(c.localA, base(contrato) + "/posicao-temporal?data=" + inicio,
                "EMP-002: leitura temporal conserva razão social documentada");
        assertThat(posicao.path("versaoContratual").path("fornecedorId").asLong()).isEqualTo(fornecedor);
        assertThat(posicao.path("versaoContratual").path("razaoSocialEmpresa").asText())
                .isEqualTo("Empresa na publicação");
    }

    @Test
    void cnpjInvalidoRetorna400SemPersistirFornecedor() throws Exception {
        var c = criarCenarioPersistido();
        long antes = quantidadeFornecedores();
        enviar(c.localA, F, mapa("razaoSocial", "Fornecedor inválido", "cnpj", "123"),
                400, "CNPJ inválido continua sendo erro de cliente");
        assertThat(quantidadeFornecedores()).isEqualTo(antes);
    }

    @Test
    void somenteGestorLocalPodeUsarCadastroEVinculacao() throws Exception {
        var c = criarCenarioPersistido();
        List<String[]> rotas = List.of(new String[]{"GET", F}, new String[]{"POST", F},
                new String[]{"PUT", F + "/1"},
                new String[]{"PUT", "/api/contratos/" + c.contratoA.getId() + "/fornecedor/1"});
        for (var usuario : List.of(c.operacionalA, c.estadualA, c.nacional, c.presidente, c.admin)) {
            for (var rota : rotas) executar(usuario,
                    request(HttpMethod.valueOf(rota[0]), rota[1]).contentType(APPLICATION_JSON).content("{}"),
                    403, "Cadastro operacional não amplia poder de " + usuario.getPerfil());
        }
        executar(null, get(F), 401, "Cadastro exige autenticação");
        executar(c.localA, get(F), 200, "Controle positivo: Gestão Local mantém seu catálogo");
    }

    private EstadoFornecedor estadoFornecedor(long id) {
        return emTransacao(() -> {
            em.clear();
            Fornecedor f = em.find(Fornecedor.class, id);
            assertThat(f).isNotNull();
            return new EstadoFornecedor(f.getId(), f.getVersion(), f.getRazaoSocial(), f.getCnpj(),
                    f.getEmail(), f.getAtivo());
        });
    }

    private JsonNode estadoContrato(long id) {
        return emTransacao(() -> {
            em.clear();
            Contrato contrato = em.find(Contrato.class, id);
            var fotografias = em.createQuery("select v.id,v.numeroVersao,v.fornecedorId,v.cnpjEmpresa,"
                            + "v.razaoSocialEmpresa,v.nomeFantasiaEmpresa,v.dataEfetiva from VersaoContratoNegocial v "
                            + "where v.contrato.id=:id order by v.id", Object[].class)
                    .setParameter("id", id).getResultList();
            var eventos = em.createQuery("select e.id from EventoCicloVidaContrato e "
                            + "where e.contrato.id=:id order by e.id", Long.class)
                    .setParameter("id", id).getResultList();
            return mapper.valueToTree(mapa("id", contrato.getId(), "versao", contrato.getVersion(),
                    "estado", contrato.getEstadoNegocial().name(), "fornecedorId",
                    contrato.getFornecedor() == null ? null : contrato.getFornecedor().getId(),
                    "cnpj", contrato.getCnpjEmpresa(), "razao", contrato.getRazaoSocialEmpresa(),
                    "fantasia", contrato.getNomeFantasiaEmpresa(), "fotografias", fotografias, "eventos", eventos));
        });
    }

    private long quantidadeFornecedores() {
        return emTransacao(() -> em.createQuery("select count(f) from Fornecedor f", Long.class).getSingleResult());
    }

    private static String cnpjSintetico() {
        return String.format(Locale.ROOT, "%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
    }

    private static void campos(JsonNode node, String... esperados) {
        List<String> campos = new ArrayList<>();
        node.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder(esperados);
    }

    private record EstadoFornecedor(long id, long versao, String razao, String cnpj, String email, Boolean ativo) {}
}
