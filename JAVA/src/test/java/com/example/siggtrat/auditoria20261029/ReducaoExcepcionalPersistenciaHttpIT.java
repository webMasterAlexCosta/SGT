package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.AditamentoContratual;
import com.example.siggtrat.entidade.PermissaoExcepcional;
import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * RED-008:004. Mesmo fluxo400/403/409/201 do contrato existente, mas cada
 * requisição termina sua transação. Aditamento, concessão e uso são relidos
 * depois do commit. O uso referencia a concessão e o Contrato; não existe
 * vínculo direto Uso -> Aditamento no modelo, nem consumo por mera consulta.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReducaoExcepcionalPersistenciaHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    private static final String FUNDAMENTO = "Ato jurídico específico documentado para redução sintética";
    private static final String FINALIDADE = "Auditoria de persistência da redução excepcional";
    private static final String JUSTIFICATIVA_CONCESSAO = "Ato documental sintético conferido";
    private static final String AUTORIZACAO = "ATO-SINTETICO-REDUCAO";

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void excecaoAutorizadaPersisteInstrumentoEUmUsoComAtorTipoEscopoEFinalidadeCorretos() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().plusDays(10);
        long contrato = dados(postar(c.localA, "/api/contratos", rascunho(c,
                "RED-PERSISTE-" + UUID.randomUUID().toString().substring(0, 8), 100, inicio, inicio.plusDays(60)), 201, "rascunho")).path("id").asLong();
        long comp = dados(postar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 100),
                201, "composicao100")).path("id").asLong();
        postar(c.localA, base(contrato) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-REDUCAO-ORIGINAL", "justificativa", "Base integral publicada"), 200, "publicar");
        LocalDate efeito = inicio.plusDays(1);
        var req = aditamento("REDUCAO-EXCECAO-PERSISTIDA", efeito);
        req.put("novoEfetivoTotal", 60);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", comp, "quantidadeResultante", 60)));
        req.put("reducaoExcepcional", true);
        negativa(c, contrato, req, 400, "sem-fundamento");
        req.put("fundamentoExcecao", FUNDAMENTO);
        negativa(c, contrato, req, 403, "sem-concessao");
        JsonNode concedida = dados(postar(c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "REDUCAO_CONTRATUAL_EXCEPCIONAL", "usuarioBeneficiarioId", c.localA.getId(),
                "contratoId", contrato, "finalidade", FINALIDADE, "justificativa", JUSTIFICATIVA_CONCESSAO,
                "autorizacaoAdministrativa", AUTORIZACAO, "validoAte", Instant.now().plusSeconds(7200)), 201, "concessao"));
        long permissaoId = concedida.path("id").asLong();
        assertThat(permissaoId).isPositive();
        req.put("confirmarImpacto", false);
        negativa(c, contrato, req, 409, "sem-confirmacao");
        req.put("confirmarImpacto", true);
        prever(c, contrato, req);
        var antes = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        assertThat(antes.tabelas().get("permissao_excepcional_uso").linhas()).isEmpty();
        Instant inicioComando = Instant.now().minusSeconds(1);
        MvcResult resultado = http(c.localA, base(contrato) + "/aditamentos", req);
        Instant fimComando = Instant.now().plusSeconds(1);
        long instrumentoId = dados(resultado).path("id").asLong();
        // Sem transação envolvente: andReturn já atravessou o advice e o commit.
        var depois = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        LeituraPersistida leitura = lerPersistidos(permissaoId, instrumentoId);
        MvcResult consulta = mvc.perform(autenticar(get(base(contrato) + "/aditamentos"), c.localA)).andReturn();
        LeituraPersistida segundaLeitura = lerPersistidos(permissaoId, instrumentoId);
        var depoisDasLeituras = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        SnapshotQuantitativos.registrar(mapper, "reducao-persistencia", "autorizada", mapa("contratoId", contrato,
                "composicaoId", comp, "permissaoId", permissaoId, "instrumentoId", instrumentoId,
                "resposta", SnapshotQuantitativos.http(mapper, resultado), "consulta", SnapshotQuantitativos.http(mapper, consulta),
                "antes", antes, "depoisCommit", depois, "depoisDasLeituras", depoisDasLeituras,
                "leituraPersistida", leitura, "segundaLeituraPersistida", segundaLeitura,
                "modeloDoUso", "Concessão + tipo/ator + tipoRecurso=Contrato/idRecurso; finalidade do uso é o fundamento da exceção. Sem FK ao instrumento."));

        assertAll("A exceção documentada precisa sobreviver ao commit com um consumo auditável",
                () -> assertThat(resultado.getResponse().getStatus()).isEqualTo(201),
                () -> assertThat(dados(resultado).path("reducaoExcepcional").asBoolean()).isTrue(),
                () -> assertThat(dados(resultado).path("fundamentoExcecao").asText()).isEqualTo(FUNDAMENTO),
                () -> assertThat(dados(resultado).path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId()),
                () -> assertThat(consulta.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(dados(consulta)).hasSize(1),
                () -> assertThat(leitura.instrumento()).isNotNull(),
                () -> conferirInstrumento(leitura.instrumento(), c, contrato, instrumentoId, efeito, inicioComando, fimComando),
                () -> conferirConcessao(leitura.concessao(), c, contrato, permissaoId),
                () -> assertThat(leitura.usos()).hasSize(1),
                () -> conferirUso(leitura.usos().get(0), c, contrato, permissaoId, inicioComando, fimComando),
                () -> assertThat(depois.tabelas().get("aditamento_contratual").linhas()).extracting(SnapshotQuantitativos.Linha::id)
                        .containsExactly(instrumentoId),
                () -> assertThat(depois.tabelas().get("aditamento_composicao").linhas()).hasSize(1),
                () -> assertThat(depois.tabelas().get("permissao_excepcional_uso").linhas()).hasSize(1),
                () -> assertThat(depois.tabelas().get("permissao_excepcional"))
                        .as("Concessão permanece vigente; consumo gera uma trilha separada").isEqualTo(antes.tabelas().get("permissao_excepcional")),
                () -> assertThat(segundaLeitura).isEqualTo(leitura),
                () -> assertThat(depoisDasLeituras).as("Consulta não registra novo uso nem altera o instrumento").isEqualTo(depois));
    }

    private void negativa(Cenario c, long contrato, Map<String, Object> req, int esperado, String caso) throws Exception {
        prever(c, contrato, req);
        var antes = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        MvcResult resposta = http(c.localA, base(contrato) + "/aditamentos", req);
        var depois = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        SnapshotQuantitativos.registrar(mapper, "reducao-persistencia", caso, mapa("contratoId", contrato,
                "resposta", SnapshotQuantitativos.http(mapper, resposta), "antes", antes, "depois", depois));
        assertAll(() -> assertThat(resposta.getResponse().getStatus()).isEqualTo(esperado),
                () -> assertThat(depois).as("Tentativa não autorizada não grava instrumento, versão ou consumo").isEqualTo(antes));
    }

    private LeituraPersistida lerPersistidos(long permissaoId, long instrumentoId) {
        return emTransacao(() -> {
            AditamentoContratual a = instrumentoId > 0 ? em.find(AditamentoContratual.class, instrumentoId) : null;
            PermissaoExcepcional p = em.find(PermissaoExcepcional.class, permissaoId);
            Instrumento i = a == null ? null : new Instrumento(a.getId(), a.getContrato().getId(), a.getEvento().getId(),
                    a.getDataDocumento(), a.getDataEfetiva(), a.getNovoEfetivoTotal(), a.getReducaoExcepcional(), a.getImpactoConfirmado(),
                    hash(a.getFundamentoExcecao()), hash(a.getJustificativa()), hash(a.getReferenciaSei()), a.getUsuarioResponsavelId(),
                    hash(a.getUsuarioResponsavelUsername()), a.getRegistradoEm(), a.getVersaoInstrumento());
            Concessao concessao = p == null ? null : new Concessao(p.getId(), p.getTipo(), p.getUsuarioBeneficiario().getId(),
                    p.getContrato() == null ? null : p.getContrato().getId(), p.getGestaoLocal() == null ? null : p.getGestaoLocal().getId(),
                    hash(p.getFinalidade()), hash(p.getJustificativa()), hash(p.getAutorizacaoAdministrativa()), p.getConcedidoPorId(),
                    p.getAtivo(), p.getConcedidoEm(), p.getValidoAte());
            List<Uso> usos = em.createQuery("select u from UsoPermissaoExcepcional u where u.permissao.id=:id order by u.id", UsoPermissaoExcepcional.class)
                    .setParameter("id", permissaoId).getResultList().stream().map(u -> new Uso(u.getId(), u.getPermissao().getId(),
                            u.getTipo(), u.getUsuarioId(), hash(u.getUsuarioUsername()), u.getTipoRecurso(), u.getRecursoId(),
                            hash(u.getFinalidade()), u.getUsadoEm())).toList();
            return new LeituraPersistida(i, concessao, usos);
        });
    }

    private void conferirInstrumento(Instrumento i, Cenario c, long contrato, long id, LocalDate efeito, Instant inicio, Instant fim) {
        assertThat(i).isNotNull();
        assertAll(() -> assertThat(i.id()).isEqualTo(id), () -> assertThat(i.contratoId()).isEqualTo(contrato),
                () -> assertThat(i.eventoId()).isPositive(), () -> assertThat(i.dataDocumento()).isEqualTo(efeito),
                () -> assertThat(i.dataEfetiva()).isEqualTo(efeito), () -> assertThat(i.novoEfetivoTotal()).isEqualTo(60),
                () -> assertThat(i.excepcional()).isTrue(), () -> assertThat(i.impactoConfirmado()).isTrue(),
                () -> assertThat(i.fundamentoSha()).isEqualTo(hash(FUNDAMENTO)),
                () -> assertThat(i.justificativaSha()).isEqualTo(hash("Alteração documentada de auditoria")),
                () -> assertThat(i.seiSha()).isEqualTo(hash("SEI-ADI")),
                () -> assertThat(i.responsavelId()).isEqualTo(c.localA.getId()),
                () -> assertThat(i.responsavelSha()).isEqualTo(hash(c.localA.getUsername())),
                () -> assertThat(i.registradoEm()).isBetween(inicio, fim), () -> assertThat(i.versaoInstrumento()).isEqualTo(1));
    }

    private void conferirConcessao(Concessao p, Cenario c, long contrato, long id) {
        assertThat(p).isNotNull();
        assertAll(() -> assertThat(p.id()).isEqualTo(id), () -> assertThat(p.tipo()).isEqualTo(TipoPermissaoExcepcional.REDUCAO_CONTRATUAL_EXCEPCIONAL),
                () -> assertThat(p.beneficiarioId()).isEqualTo(c.localA.getId()), () -> assertThat(p.contratoId()).isEqualTo(contrato),
                () -> assertThat(p.gestaoLocalId()).as("Concessão contratual usa o contrato como escopo").isNull(),
                () -> assertThat(p.finalidadeSha()).isEqualTo(hash(FINALIDADE)),
                () -> assertThat(p.justificativaSha()).isEqualTo(hash(JUSTIFICATIVA_CONCESSAO)),
                () -> assertThat(p.autorizacaoSha()).isEqualTo(hash(AUTORIZACAO)),
                () -> assertThat(p.concedidoPorId()).isEqualTo(c.admin.getId()), () -> assertThat(p.ativo()).isTrue(),
                () -> assertThat(p.validoAte()).isAfter(p.concedidoEm()));
    }

    private void conferirUso(Uso u, Cenario c, long contrato, long permissao, Instant inicio, Instant fim) {
        assertAll(() -> assertThat(u.id()).isPositive(), () -> assertThat(u.permissaoId()).isEqualTo(permissao),
                () -> assertThat(u.tipo()).isEqualTo(TipoPermissaoExcepcional.REDUCAO_CONTRATUAL_EXCEPCIONAL),
                () -> assertThat(u.usuarioId()).isEqualTo(c.localA.getId()),
                () -> assertThat(u.usuarioSha()).isEqualTo(hash(c.localA.getUsername())),
                () -> assertThat(u.tipoRecurso()).isEqualTo("Contrato"), () -> assertThat(u.recursoId()).isEqualTo(contrato),
                () -> assertThat(u.finalidadeSha()).as("O modelo grava o fundamento como finalidade do uso").isEqualTo(hash(FUNDAMENTO)),
                () -> assertThat(u.usadoEm()).isBetween(inicio, fim));
    }

    private void prever(Cenario c, long contrato, Map<String, Object> req) throws Exception {
        var resultado = postar(c.localA, base(contrato) + "/aditamentos/previa", req, 200, "previa");
        req.put("tokenPrevia", dados(resultado).path("tokenPrevia").asText());
    }

    private MvcResult http(UsuarioAplicacao usuario, String path, Object body) throws Exception {
        return mvc.perform(autenticar(post(path).contentType(APPLICATION_JSON).content(json(body)), usuario)).andReturn();
    }

    private MvcResult postar(UsuarioAplicacao usuario, String path, Object body, int esperado, String etapa) throws Exception {
        var resultado = http(usuario, path, body);
        SnapshotQuantitativos.registrar(mapper, "reducao-persistencia", "setup-" + etapa, SnapshotQuantitativos.http(mapper, resultado));
        assertThat(resultado.getResponse().getStatus()).as("Pré-condição HTTP: %s", etapa).isEqualTo(esperado);
        return resultado;
    }

    private String hash(String valor) { return valor == null ? null : SnapshotQuantitativos.hash(valor); }
    private record LeituraPersistida(Instrumento instrumento, Concessao concessao, List<Uso> usos) { }
    private record Instrumento(long id, long contratoId, long eventoId, LocalDate dataDocumento, LocalDate dataEfetiva,
                                Integer novoEfetivoTotal, Boolean excepcional, Boolean impactoConfirmado, String fundamentoSha,
                                String justificativaSha, String seiSha, Long responsavelId, String responsavelSha,
                                Instant registradoEm, Integer versaoInstrumento) { }
    private record Concessao(long id, TipoPermissaoExcepcional tipo, long beneficiarioId, Long contratoId, Long gestaoLocalId,
                            String finalidadeSha, String justificativaSha, String autorizacaoSha, Long concedidoPorId,
                            Boolean ativo, Instant concedidoEm, Instant validoAte) { }
    private record Uso(long id, long permissaoId, TipoPermissaoExcepcional tipo, Long usuarioId, String usuarioSha,
                      String tipoRecurso, Long recursoId, String finalidadeSha, Instant usadoEm) { }
}
