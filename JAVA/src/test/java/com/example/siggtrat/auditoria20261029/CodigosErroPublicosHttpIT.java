package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Rotas produtivas/segurança/JPA reais; nenhum controller de falha artificial. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CodigosErroPublicosHttpIT extends ContratosHttpBase {
    @Test void codigosAditivosPreservamRecusasDeDominioValidacaoEConflitoEspecifico() throws Exception {
        var c = criarCenarioPersistido();
        String identidade = String.format(Locale.ROOT, "%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
        long empresasAntes = empresas();
        var invalida = executar(c.localA, post("/api/fornecedores").contentType(APPLICATION_JSON)
                .content(json(mapa("cnpj", identidade, "email", "cadastro@example.test"))), 400,
                "Regra pública de razão social obrigatória mantém400");
        var erro400 = mapper.readTree(invalida.getResponse().getContentAsString());
        assertThat(erro400.path("code").asText()).isEqualTo("HTTP_400");
        assertThat(erro400.path("message").asText()).contains("razaoSocial");
        assertThat(empresas()).isEqualTo(empresasAntes);

        var req = mapa("razaoSocial", "Empresa sintética código estável", "cnpj", identidade,
                "email", "cadastro@example.test");
        var ausente = executar(c.localA, put("/api/fornecedores/" + Long.MAX_VALUE)
                .contentType(APPLICATION_JSON).content(json(req)), 404, "ID inexistente mantém404 e não grava");
        var erro404 = mapper.readTree(ausente.getResponse().getContentAsString());
        assertThat(erro404.path("code").asText()).isEqualTo("HTTP_404");
        assertThat(erro404.path("message").asText()).contains("não encontrado");
        assertThat(empresas()).isEqualTo(empresasAntes);

        long usuariosAntes = usuarios();
        var validacao = executar(c.admin, post("/api/usuarios").contentType(APPLICATION_JSON)
                .content(json(mapa("email", "sem-arroba"))), 400, "Validação Jakarta real mantém os campos úteis");
        var erroValidacao = mapper.readTree(validacao.getResponse().getContentAsString());
        assertThat(erroValidacao.path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(erroValidacao.path("fields").findValuesAsText("field")).contains("email");
        assertThat(erroValidacao.path("message").asText()).isNotBlank();
        assertThat(usuarios()).isEqualTo(usuariosAntes);

        var criado = enviar(c.localA, "/api/fornecedores", req, 200, "Controle positivo não recebe envelope de erro");
        assertThat(criado.path("id").asLong()).isPositive();
        var conflito = executar(c.localA, post("/api/fornecedores").contentType(APPLICATION_JSON)
                .content(json(req)), 409, "Código específico de unicidade não é substituído por código genérico");
        var erro409 = mapper.readTree(conflito.getResponse().getContentAsString());
        assertThat(erro409.path("code").asText()).isEqualTo("CNPJ_DUPLICADO");
        assertThat(empresas()).isEqualTo(empresasAntes + 1);
        SnapshotQuantitativos.registrar(mapper, "codigos-http", "contrato-publico",
                mapa("http400", SnapshotQuantitativos.http(mapper, invalida),
                        "http404", SnapshotQuantitativos.http(mapper, ausente),
                        "validacao", SnapshotQuantitativos.http(mapper, validacao),
                        "conflitoEspecifico", SnapshotQuantitativos.http(mapper, conflito),
                        "empresasAntes", empresasAntes, "empresasDepois", empresas(),
                        "usuariosAntes", usuariosAntes, "usuariosDepois", usuarios()));
    }

    private long empresas() { return emTransacao(() -> em.createQuery("select count(f) from Fornecedor f", Long.class).getSingleResult()); }
    private long usuarios() { return emTransacao(() -> em.createQuery("select count(u) from UsuarioAplicacao u", Long.class).getSingleResult()); }
}
