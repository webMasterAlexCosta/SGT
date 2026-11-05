package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.PermissaoExcepcional;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Consumidores de CT007: autorização atual de concessão, documento e correção de identificador. */
class DocumentosPermissoesTransferenciaGestaoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concessaoNegocialDepoisDaTransferenciaExigeResponsavelEfetivo(boolean beneficiarioOrigem) throws Exception {
        var f = preparar();
        var c = f.cenario();
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            var beneficiario = beneficiarioOrigem ? c.localA : c.localB;
            var gestao = beneficiarioOrigem ? c.gestaoA : c.gestaoB;
            var resposta = enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                    "tipo", "CORRECAO_IDENTIFICADOR_CONTRATO", "usuarioBeneficiarioId", beneficiario.getId(),
                    "contratoId", f.contrato().id(), "gestaoLocalId", gestao.getId(),
                    "finalidade", "Corrigir identificação do contrato sob responsabilidade atual",
                    "justificativa", "Erro material documentado após reorganização",
                    "autorizacaoAdministrativa", "ATO-CORRECAO-DESTINO", "validoAte", Instant.now().plusSeconds(3600)),
                    beneficiarioOrigem ? 400 : 201,
                    "PER-008/PER-012 concessão usa a gestão efetiva na data atual, sem materialização adicional");
            long quantidade = em.createQuery("""
                    select count(p) from PermissaoExcepcional p
                    where p.contrato.id=:contrato and p.usuarioBeneficiario.id=:usuario and p.tipo=:tipo
                    """, Long.class).setParameter("contrato", f.contrato().id())
                    .setParameter("usuario", beneficiario.getId())
                    .setParameter("tipo", TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO).getSingleResult();
            assertThat(quantidade).isEqualTo(beneficiarioOrigem ? 0L : 1L);
            if (!beneficiarioOrigem) {
                assertThat(resposta.path("usuarioBeneficiarioId").asLong()).isEqualTo(c.localB.getId());
                assertThat(resposta.path("contratoId").asLong()).isEqualTo(f.contrato().id());
            }
        }
    }

    @Test
    void gestorDestinoConsultaEVersionaDocumentoRestritoComFinalidadeEAuditoria() throws Exception {
        var f = preparar();
        var c = f.cenario();
        long id = f.contrato().id();
        long publicacao = consultar(c.localA, base(id) + "/eventos", "Publicação original da fonte")
                .get(0).path("id").asLong();
        var doc = dados(executar(c.localA, multipart(base(id) + "/documentos").file(metadados(mapa(
                "tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao, "fatoChave", "ORIGINAL",
                "titulo", "Fonte restrita sob responsabilidade contratual", "referenciaSei", "SEI-RESTRITO-TRANSFERIDO",
                "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", f.contrato().inicio()))),
                201, "DOC-009 preparação da fonte restrita antes do efeito da transferência"));
        String path = base(id) + "/documentos/" + doc.path("id").asLong();
        em.flush();
        em.clear();

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            var lido = consultar(c.localB, path + "?finalidade=operacao-atual",
                    "DOC-010 gestor efetivo do destino acessa restrito necessário com finalidade");
            assertThat(lido.path("classificacaoAcesso").asText()).isEqualTo("PESSOAL_RESTRITO");
            var acessos = consultar(c.localB, path + "/acessos", "DOC-010 gestor atual consulta auditoria da fonte");
            assertThat(acessos).hasSize(1);
            assertThat(acessos.get(0).path("usuarioId").asLong()).isEqualTo(c.localB.getId());
            assertThat(acessos.get(0).path("finalidade").asText()).isEqualTo("operacao-atual");
            assertThat(acessos.get(0).path("permissaoId").isNull()).isTrue();
            assertThat(consultar(c.localB, path + "/permissoes", "DOC-010 dispensa concessão para o gestor responsável"))
                    .isEmpty();
            var nova = enviar(c.localB, path + "/retificar-sei", mapa("novaReferenciaSei", "SEI-RESTRITO-CORRIGIDO",
                    "motivo", "Correção documental pela gestão atual", "chaveIdempotencia", "doc-destino-" + id),
                    200, "DOC-005 gestão atual pode versionar a fonte preservando o histórico");
            assertThat(nova.path("documentoAnteriorId").asLong()).isEqualTo(doc.path("id").asLong());
            assertThat(nova.path("classificacaoAcesso").asText()).isEqualTo("PESSOAL_RESTRITO");
            assertThat(consultar(c.localB, path + "?finalidade=conferir-historico", "DOC-005 fonte anterior não reescrita")
                    .path("referenciaSei").asText()).isEqualTo("SEI-RESTRITO-TRANSFERIDO");
            executar(c.localA, get(path).param("finalidade", "operacao-antiga"), 404,
                    "DOC-010 antiga responsabilidade não concede leitura corrente");
        }
    }

    @Test
    void gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria() throws Exception {
        var f = preparar();
        var c = f.cenario();
        long id = f.contrato().id();
        var anterior = consultar(c.nacional, base(id), "Identificação antes da correção excepcional");
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            // Arranjo JPA explícito: isola o consumidor da correção do defeito no endpoint
            // de concessão, exercitado acima por HTTP. Segurança, validação do uso,
            // comando, evento, fotografia e persistência continuam sendo reais.
            long permissaoId = prepararConcessaoExistenteParaDestino(f);
            var req = mapa("novoNumero", "CORR-DESTINO-" + id, "novoAno", f.dataEfetiva().getYear(),
                    "dataEfetiva", f.dataEfetiva(), "dataAto", f.dataEfetiva(),
                    "motivo", "Correção material sob responsabilidade efetiva do destino", "referenciaSei", "SEI-CORR-DESTINO",
                    "autorizacaoAdministrativa", "ATO-CORRECAO-DESTINO", "chaveIdempotencia", "corr-destino-" + id);
            var correcao = enviar(c.localB, base(id) + "/correcoes-identificador", req, 201,
                    "PER-008 gestor atual autorizado deve passar também pelo guard específico da correção");
            assertThat(correcao.path("permissaoId").asLong()).isEqualTo(permissaoId);
            assertThat(correcao.path("responsavelId").asLong()).isEqualTo(c.localB.getId());
            assertThat(correcao.path("numeroAnterior").asText()).isEqualTo(anterior.path("numeroContrato").asText());
            assertThat(correcao.path("numeroNovo").asText()).isEqualTo("CORR-DESTINO-" + id);
            assertThat(enviar(c.localB, base(id) + "/correcoes-identificador", req, 201,
                    "PER-008 repetição exata mantém concessão e evento originais").path("id").asLong())
                    .isEqualTo(correcao.path("id").asLong());
            var usos = consultar(c.admin, "/api/permissoes-excepcionais/" + permissaoId + "/usos",
                    "PER-008 uso auditado sem consumo duplicado na repetição");
            assertThat(usos).hasSize(1);
            assertThat(usos.get(0).path("usuarioId").asLong()).isEqualTo(c.localB.getId());
            assertThat(consultar(c.localB, "/api/contratos/historico-identificador?numero="
                    + anterior.path("numeroContrato").asText() + "&ano=" + anterior.path("anoContrato").asInt(),
                    "CON-003 identificador anterior continua pesquisável para o gestor atual")).hasSize(1);
            executar(c.localA, get(base(id) + "/correcoes-identificador"), 404,
                    "PER-008 histórico do contrato respeita responsabilidade atual");
        }
    }

    private long prepararConcessaoExistenteParaDestino(Preparado f) {
        var c = f.cenario();
        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setTipo(TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO);
        permissao.setContrato(em.getReference(Contrato.class, f.contrato().id()));
        permissao.setUsuarioBeneficiario(em.getReference(UsuarioAplicacao.class, c.localB.getId()));
        permissao.setFinalidade("Correção de identificador autorizada para a gestão efetiva");
        permissao.setJustificativa("Concessão documental existente, isolada do fluxo de cadastro da permissão");
        permissao.setAutorizacaoAdministrativa("ATO-CORRECAO-DESTINO");
        permissao.setConcedidoPorId(c.admin.getId());
        permissao.setConcedidoPorUsername(c.admin.getUsername());
        permissao.setValidoAte(Instant.now().plusSeconds(3600));
        em.persist(permissao);
        em.flush();
        long id = permissao.getId();
        em.clear();
        return id;
    }

    private Preparado preparar() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(
                "dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Reorganização programada com contexto mapeado", "referenciaSei", "SEI-TRANSFERENCIA-CONSUMIDORES",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "PER-012 transferência futura registrada por HTTP com autorização específica");
        em.flush();
        em.clear();
        return new Preparado(c, n, data);
    }

    private record Preparado(Cenario cenario, NovoContrato contrato, LocalDate dataEfetiva) {}
}
