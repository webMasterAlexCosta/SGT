package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Segurança transversal não equivale a homologação funcional de cada rota. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SegurancaTodasRotasNovaAuditoriaHttpIT extends AuditoriaHttpSuporte {
    @Autowired PropriedadesTokenJwt tokenProperties;
    private static final Set<String> PUBLICAS = Set.of("GET /config.json", "GET /api/autenticacao/csrf",
            "GET /api/autenticacao/superintendencias", "GET /api/autenticacao/gestoes-locais",
            "GET /api/autenticacao/recuperar-senha/validar", "POST /api/autenticacao/autenticar",
            "POST /api/autenticacao/cadastrar", "POST /api/autenticacao/recuperar-senha/solicitar",
            "POST /api/autenticacao/recuperar-senha/confirmar", "POST /api/autenticacao/encerrar-sessao");

    @ParameterizedTest
    @ValueSource(strings = {"JWT_EXPIRADO", "TOKEN_REVOGADO", "ASSINATURA_INVALIDA"})
    void credencialInvalidaNuncaAlcancaControladoresPrivados(String condicao) throws Exception {
        Cenario c = criarCenarioPersistido();
        String dispositivo = "nova-auditoria-" + c.localA.getId();
        String token = jwt.gerarToken(c.localA.getId());
        if (condicao.equals("JWT_EXPIRADO")) {
            token = Jwts.builder().subject(String.valueOf(c.localA.getId())).id(UUID.randomUUID().toString())
                    .issuedAt(new Date(System.currentTimeMillis() - 120000))
                    .expiration(new Date(System.currentTimeMillis() - 60000))
                    .signWith(Keys.hmacShaKeyFor(tokenProperties.secret().getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512).compact();
        }
        String original = token;
        Long sessao = emTransacao(() -> {
            var agora = RelogioSistema.agora();
            TokenAtivo s = TokenAtivo.deTokenBruto(original, c.localA.getUsername(), agora, agora.plusHours(2));
            s.setLastSeenAt(agora); s.setDeviceHash(UtilitarioHashSeguranca.sha256Hexadecimal(dispositivo));
            em.persist(s); em.flush(); return s.getId();
        });
        if (condicao.equals("TOKEN_REVOGADO")) {
            emTransacao(() -> { em.remove(em.find(TokenAtivo.class, sessao)); return null; });
            assertThat(emTransacao(() -> em.find(TokenAtivo.class, sessao))).isNull();
        } else if (condicao.equals("ASSINATURA_INVALIDA")) {
            int inicioAssinatura = token.lastIndexOf('.') + 1;
            token = token.substring(0, inicioAssinatura) + (token.charAt(inicioAssinatura) == 'A' ? 'B' : 'A')
                    + token.substring(inicioAssinatura + 1);
        }
        for (String rota : privadas()) {
            String[] partes = rota.split(" ", 2);
            String caminho = partes[1].replaceAll("\\{[^}]+}", "1");
            var resultado = executar(null, request(HttpMethod.valueOf(partes[0]), caminho)
                    .cookie(new Cookie("autenticacao", token), new Cookie("id_dispositivo", dispositivo),
                            new Cookie("XSRF-TOKEN", "csrf-nova-auditoria"))
                    .header("X-XSRF-TOKEN", "csrf-nova-auditoria")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"), 401,
                    condicao + " bloqueia antes do handler: " + rota);
            var erro = dados(resultado);
            assertThat(erro.path("status").asInt()).isEqualTo(401);
            assertThat(erro.path("action").asText()).isEqualTo("LOGOUT");
            assertThat(erro.path("path").asText()).isEqualTo(caminho);
            assertThat(resultado.getResponse().getHeader("Cache-Control")).contains("no-store");
            assertThat(resultado.getHandler()).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CSRF_AUSENTE", "CSRF_INCORRETO"})
    void csrfInvalidoImpedeMutacoesAntesDoHandler(String condicao) throws Exception {
        Cenario c = criarCenarioPersistido();
        String token = jwt.gerarToken(c.localA.getId()), dispositivo = "csrf-auditoria-" + c.localA.getId();
        emTransacao(() -> {
            var agora = RelogioSistema.agora();
            TokenAtivo s = TokenAtivo.deTokenBruto(token, c.localA.getUsername(), agora, agora.plusHours(2));
            s.setLastSeenAt(agora); s.setDeviceHash(UtilitarioHashSeguranca.sha256Hexadecimal(dispositivo));
            em.persist(s); return null;
        });
        List<String> rotasPrivadas = privadas();
        Set<String> mutacoesEsperadas = new TreeSet<>();
        for (String rota : rotasPrivadas) {
            if (Set.of("POST", "PUT", "PATCH", "DELETE").contains(rota.split(" ", 2)[0])) {
                mutacoesEsperadas.add(rota);
            }
        }
        Set<String> exercitadas = new TreeSet<>();
        for (String rota : rotasPrivadas) {
            String[] partes = rota.split(" ", 2);
            if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(partes[0])) continue;
            String caminho = partes[1].replaceAll("\\{[^}]+}", "1");
            var req = request(HttpMethod.valueOf(partes[0]), caminho)
                    .cookie(new Cookie("autenticacao", token), new Cookie("id_dispositivo", dispositivo),
                            new Cookie("XSRF-TOKEN", "csrf-correto"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}");
            if (condicao.equals("CSRF_INCORRETO")) req.header("X-XSRF-TOKEN", "csrf-incorreto");
            var resultado = executar(null, req, 403, condicao + " bloqueia mutação antes do handler: " + rota);
            assertThat(dados(resultado).path("status").asInt()).isEqualTo(403);
            assertThat(resultado.getHandler()).isNull();
            exercitadas.add(rota);
        }
        assertThat(exercitadas).as("Todas as mutações privadas registradas foram recusadas por CSRF antes do handler")
                .containsExactlyInAnyOrderElementsOf(mutacoesEsperadas);
    }

    private List<String> privadas() {
        return InventarioRotasPrivadas.coletarEConferir(mappings, PUBLICAS);
    }
}
