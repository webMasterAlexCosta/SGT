package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PER-004/006/011: consulta institucional da Central, sem comandos nem exposição de narrativas pessoais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreauditoriaPendenciaNarrativaHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"GESTOR_ESTADUAL", "GESTOR_NACIONAL", "PRESIDENTE"})
    void observacaoPessoalDeTratamentoNaoAmpliaExposicaoNaCentral(String perfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        String rg = "RG-PENDENCIA-" + c.pessoaA.getId();
        String email = "pendencia-" + c.pessoaA.getId() + "@example.test";
        ObjectNode body = (ObjectNode) consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                "Responsável consulta Pessoa antes da conferência documental");
        body.put("rg", rg); body.put("email", email);
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)), 200,
                "Dados pessoais são gravados no cadastro real antes de aparecerem na narrativa");
        long frequencia = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA, "status", "FALTA",
                "justificativaRetificacao", "Conferência documental do lançamento antigo",
                "chaveIdempotencia", "preauditoria-pendencia-" + UUID.randomUUID()), 200,
                "Registro real de frequência será atingido por fato retroativo").path("id").asLong();
        Map<String, Object> inativacao = mapa("dataEfetiva", DATA.minusDays(1),
                "dataAto", RelogioSistema.hoje(), "motivo", "Bloqueio retroativo documentado",
                "referenciaSei", "SEI-PREAUDITORIA-PENDENCIA");
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/inativar", inativacao,
                200, "Prévia real identifica impacto na frequência");
        inativacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        inativacao.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", inativacao, 200,
                "Fato temporal real gera pendência, sem inserir tarefa artificial");
        long pendencia = emTransacao(() -> em.createQuery(
                        "select p.id from PendenciaAdministrativa p where p.frequenciaId=:id", Long.class)
                .setParameter("id", frequencia).getSingleResult());
        String observacao = "Conferência de identidade realizada com RG " + rg + " e e-mail " + email;
        enviar(c.localA, "/api/pendencias/" + pendencia + "/iniciar-tratamento",
                mapa("observacao", observacao), 200,
                "Gestor registra observação real de tratamento contendo dados pessoais do cadastro");
        JsonNode local = listar(c.localA, c);
        assertThat(local.toString()).contains(rg, email);
        UsuarioAplicacao leitor = switch (perfil) {
            case "GESTOR_NACIONAL" -> c.nacional;
            case "PRESIDENTE" -> c.presidente;
            default -> c.estadualA;
        };
        emTransacao(() -> {
            UsuarioAplicacao fonte = em.find(UsuarioAplicacao.class, leitor.getId());
            assertThat(fonte.getPerfil()).isEqualTo(PerfilAcesso.valueOf(perfil));
            assertThat(fonte.getActive()).isTrue();
            assertThat(fonte.getForceChangePassword()).isFalse();
            assertThat(fonte.getLockoutEnd()).isNull();
            return null;
        });
        FotografiaTratamento antes = fotografiaTratamento(pendencia);
        assertThat(antes.iniciadaEm()).isNotNull();
        assertThat(antes.resolvidaEm()).isNull();
        JsonNode pessoaLimitada = consultar(leitor, "/api/pessoas/" + c.pessoaA.getId(),
                "Controle: dados pessoais permanecem ocultos na rota de cadastro");
        assertThat(pessoaLimitada.toString()).doesNotContain(rg, email);
        JsonNode limitado = listar(leitor, c);
        assertThat(limitado.path("registros")).isNotEmpty();
        assertThat(limitado.toString())
                .as("Central não pode revelar pela observação os mesmos dados pessoais minimizados no cadastro")
                .doesNotContain(rg, email);
        JsonNode dto = null;
        for (JsonNode item : limitado.path("registros")) if (item.path("id").asLong() == pendencia) dto = item;
        assertThat(dto).as("A pendência continua visível no escopo permitido").isNotNull();
        assertThat(dto.path("motivo").isNull() || dto.path("motivo").isMissingNode())
                .as("Narrativa livre de motivo segue o mesmo nível de minimização").isTrue();
        assertThat(dto.path("observacaoTratamento").isNull() || dto.path("observacaoTratamento").isMissingNode())
                .as("Observação livre não pode contornar a minimização").isTrue();
        JsonNode resumo = dados(executar(leitor, get("/api/pendencias/resumo")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()), 200,
                "PER-004: a consulta institucional também alcança o resumo no mesmo escopo"));
        assertThat(resumo.path("abertas").asLong(-1)).isZero();
        assertThat(resumo.path("emTratamento").asLong(-1)).isEqualTo(1L);
        assertThat(resumo.path("resolvidas").asLong(-1)).isZero();
        assertThat(resumo.path("retificacoesFrequencia").asLong(-1)).isEqualTo(1L);
        assertThat(resumo.path("finsNaturaisVigencia").asLong(-1)).isZero();
        assertThat(resumo.toString()).doesNotContain(rg, email);
        assertThat(fotografiaTratamento(pendencia)).isEqualTo(antes);
        for (String comando : new String[] {"iniciar-tratamento", "confirmar-resolucao"}) {
            enviar(leitor, "/api/pendencias/" + pendencia + "/" + comando,
                    mapa("observacao", "Tentativa institucional não deve substituir a observação local"),
                    403, "PER-004: permitir consulta não autoriza " + comando + " ao perfil " + perfil);
            assertThat(fotografiaTratamento(pendencia))
                    .as("Recusa do comando conserva estado, autoria, datas, versão e narrativa na transação seguinte")
                    .isEqualTo(antes);
        }
        emTransacao(() -> {
            var fonte = em.find(PendenciaAdministrativa.class, pendencia);
            assertEquals(observacao, fonte.getObservacaoTratamento(),
                    "Minimizar a resposta não apaga a narrativa original de tratamento");
            assertThat(fonte.getMotivo()).isNotBlank();
            assertThat(fonte.getEstado().name()).isEqualTo("EM_TRATAMENTO");
            assertEquals(c.localA.getId(), fonte.getIniciadaPorId());
            return null;
        });
    }

    private record FotografiaTratamento(String chaveOrigem, String estado, String motivo,
                                        String observacao, Instant iniciadaEm, Long iniciadaPorId,
                                        String iniciadaPor, Instant resolvidaEm, Long resolvidaPorId,
                                        String resolvidaPor, long version) { }

    private FotografiaTratamento fotografiaTratamento(long pendenciaId) {
        return emTransacao(() -> {
            PendenciaAdministrativa fonte = em.find(PendenciaAdministrativa.class, pendenciaId);
            return new FotografiaTratamento(fonte.getChaveOrigem(), fonte.getEstado().name(),
                    fonte.getMotivo(), fonte.getObservacaoTratamento(), fonte.getIniciadaEm(),
                    fonte.getIniciadaPorId(), fonte.getIniciadaPor(), fonte.getResolvidaEm(),
                    fonte.getResolvidaPorId(), fonte.getResolvidaPor(), fonte.getVersion());
        });
    }

    private JsonNode listar(UsuarioAplicacao usuario, Cenario c) throws Exception {
        return dados(executar(usuario, get("/api/pendencias").param("inicio", DATA.toString())
                        .param("fim", DATA.toString()).param("contratoIds", c.contratoA.getId().toString()),
                200, "Consulta da Central no escopo autorizado de " + usuario.getPerfil()));
    }
}
