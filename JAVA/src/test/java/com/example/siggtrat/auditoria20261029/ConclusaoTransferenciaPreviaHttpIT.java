package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Segundo lote RED: intenção completa, dependências novas e sucessão de Gestões. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoTransferenciaPreviaHttpIT extends ContratosHttpBase {
    @Test
    void segundaTransferenciaResolveIdDaComposicaoNoContextoTemporalDaOrigem() throws Exception {
        var f = preparar(true);
        LocalDate primeira = f.n().inicio().plusDays(2);
        transferir(f, primeira);
        LocalDate segunda = primeira.plusDays(4);
        var req = transferencia(f, segunda);
        req.put("gestaoLocalDestinoId", f.c().gestaoA.getId());
        req.put("tipoContratacaoDestinoId", f.c().tipoA.getId());
        req.put("mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(),
                "lotacaoDestinoId", f.c().lotacaoA.getId(), "jornadaDestinoId", f.c().jornadaA.getId(),
                "cargoDestinoId", f.c().cargoA.getId())));
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200,
                "CON023 o identificador canônico da composição resolve o recorte B vigente, mesmo que sua fotografia original seja A");
        assertThat(foto(f, primeira).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        assertThat(foto(f, segunda).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoA.getId());
        assertThat(foto(f, segunda).path("tipoContratacaoId").asLong()).isEqualTo(f.c().tipoA.getId());
        var comps = consultar(f.c().nacional, base(f.n().id()) + "/composicoes?data=" + segunda,
                "CON023 duas transferências conservam quantidade e identidade contratual");
        assertThat(comps.get(0).path("id").asLong()).isEqualTo(f.n().composicao1());
        assertThat(comps.get(0).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(comps.get(0).path("efetivoPrevisto").asInt()).isEqualTo(5);
        assertThat(contar(f, "TransferenciaGestaoContrato")).isEqualTo(2L);
        emTransacao(() -> {
            var cadeia = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:id order by t.dataEfetiva",
                    TransferenciaGestaoContrato.class).setParameter("id", f.n().id()).getResultList();
            assertThat(cadeia.get(1).getGestaoOrigem().getId()).isEqualTo(f.c().gestaoB.getId());
            assertThat(cadeia.get(1).getMapeamentos().get(0).getLotacaoOrigem().getId()).isEqualTo(f.c().lotacaoB.getId());
            return null;
        });
    }

    @Test
    void retificacaoRetroativaSemPreviaNaoPublicaNovaVersao() throws Exception {
        var f = preparar(false); LocalDate original = RelogioSistema.hoje().minusDays(10);
        transferir(f, original); concederDestino(f);
        var req = retificacao(f, original.plusDays(1));
        long eventos = contar(f, "EventoCicloVidaContrato");
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", req, 409,
                "AUD004 retificação retroativa exige impacto calculado e confirmação explícita");
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(eventos);
        assertThat(contar(f, "TransferenciaGestaoContrato")).isEqualTo(1L);
        assertThat(foto(f, original).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
    }

    @Test
    void novaDependenciaDepoisDaPreviaExigeNovaConfirmacaoDaRetificacao() throws Exception {
        var f = preparar(false); LocalDate original = RelogioSistema.hoje().minusDays(10);
        transferir(f, original); concederDestino(f);
        var req = retificacao(f, original.plusDays(1));
        long eventos = contar(f, "EventoCicloVidaContrato");
        var primeira = previa(f, req);
        assertThat(primeira.path("tokenPrevia").asText()).matches("[0-9a-f]{64}");
        assertThat(primeira.path("quantidadeVinculosAfetados").asInt()).isZero();
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(eventos);
        enviar(f.c().localB, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Dependência após prévia " + f.n().id(), "cpf", cpf(670000000 + Math.toIntExact(f.n().id()))),
                "contratoId", f.n().id(), "composicaoId", f.n().composicao1(), "dataInicio", original.plusDays(2)),
                201, "AUD004 dependência real inserida depois da prévia da transferência");
        req.put("tokenPrevia", primeira.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", req, 409,
                "AUD004 confirmação relê dependências sob o bloqueio do contrato");
        assertThat(contar(f, "TransferenciaGestaoContrato")).isEqualTo(1L);
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(eventos);
        var atualizada = previa(f, req);
        assertThat(atualizada.path("tokenPrevia").asText()).isNotEqualTo(primeira.path("tokenPrevia").asText());
        assertThat(atualizada.path("quantidadeVinculosAfetados").asInt()).isEqualTo(1);
        req.put("tokenPrevia", atualizada.path("tokenPrevia").asText());
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", req, 200,
                "AUD004 nova prévia permite confirmar o contexto conhecido integralmente");
        assertThat(contar(f, "VinculoContratual")).isEqualTo(1L);
        assertThat(contar(f, "TransferenciaGestaoContrato")).isEqualTo(2L);
        assertThat(foto(f, original).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoA.getId());
        assertThat(foto(f, original.plusDays(1)).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
    }

    @Test
    void tokenDaPreviaVinculaTipoDeContratacaoDoDestino() throws Exception {
        var f = preparar(false); LocalDate original = RelogioSistema.hoje().minusDays(10);
        transferir(f, original); concederDestino(f);
        long outroTipo = emTransacao(() -> {
            TipoContratacao tipo = new TipoContratacao(); tipo.setGestaoLocal(em.getReference(GestaoLocal.class, f.c().gestaoB.getId()));
            tipo.setDescricao("Tipo alternativo documentado " + f.n().id()); tipo.setAtivo(true);
            tipo.setMatriculaVinculoObrigatoria(false); em.persist(tipo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.TIPO_CONTRATACAO); origem.setCatalogoId(tipo.getId());
            origem.setDataEfetiva(f.n().inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita de disponibilidade desde o início contratual"); em.persist(origem);
            return tipo.getId();
        });
        var req = retificacao(f, original.plusDays(1)); var previa = previa(f, req);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        req.put("tipoContratacaoDestinoId", outroTipo);
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", req, 409,
                "AUD004 token não autoriza Tipo diferente do que foi apresentado na prévia");
        assertThat(contar(f, "TransferenciaGestaoContrato")).isEqualTo(1L);
        var atualizada = previa(f, req); req.put("tokenPrevia", atualizada.path("tokenPrevia").asText());
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", req, 200,
                "AUD004 nova intenção explicitamente confirmada pode produzir sua versão");
        assertThat(foto(f, original.plusDays(1)).path("tipoContratacaoId").asLong()).isEqualTo(outroTipo);
    }

    @Test
    void cancelamentoDeTransferenciaMantemRestricaoDePerfilEAtoEspecifico() throws Exception {
        var f = preparar(true); LocalDate efeito = f.n().inicio().plusDays(2); transferir(f, efeito);
        long transferencia = transferenciaId(f);
        var req = mapa("dataEfetiva", efeito, "dataAto", RelogioSistema.hoje(), "motivo", "Cancelamento documentado",
                "referenciaSei", "SEI-CANCELAMENTO-CONTROLE", "autorizacaoAdministrativa", "ATO-HTTP");
        String rota = base(f.n().id()) + "/transferencias-gestao/" + transferencia + "/cancelar";
        for (var usuario : List.of(f.c().estadualA, f.c().nacional, f.c().admin, f.c().operacionalA))
            enviar(usuario, rota, req, 403, "PER012 cancelamento excepcional não concede escrita a outro perfil");
        enviar(f.c().localB, rota, req, 404, "PER012 Gestão externa não cancela transferência alheia");
        req.put("autorizacaoAdministrativa", "OUTRO-ATO");
        enviar(f.c().localA, rota, req, 409, "PER012 autorização específica não pode ser substituída por texto livre");
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(2L);
        req.put("autorizacaoAdministrativa", "ATO-HTTP");
        enviar(f.c().localA, rota, req, 200, "PER012 gestor responsável com autorização específica pode cancelar");
        assertThat(contar(f, "EventoCicloVidaContrato")).isEqualTo(3L);
    }

    private Preparado preparar(boolean futuro) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, futuro); }
            catch (Exception e) { throw new IllegalStateException(e); } });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL"); return new Preparado(c, n);
    }
    private void transferir(Preparado f, LocalDate data) throws Exception {
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", transferencia(f, data), 200,
                "CON023 transferência inicial comprovada antes da correção");
    }
    private Map<String, Object> transferencia(Preparado f, LocalDate data) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", f.c().tipoB.getId(), "motivo", "Transferência documentada da responsabilidade",
                "referenciaSei", "SEI-PREVIA-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                        "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId())));
    }
    private Map<String, Object> retificacao(Preparado f, LocalDate data) {
        var req = transferencia(f, data);
        long evento = emTransacao(() -> em.find(TransferenciaGestaoContrato.class, transferenciaId(f)).getEvento().getId());
        req.put("eventoPrecedenteId", evento); req.put("relacaoPrecedencia", "RETIFICA");
        req.put("motivo", "Retificação retroativa da data e do contexto documentado"); return req;
    }
    private void concederDestino(Preparado f) throws Exception {
        enviar(f.c().admin, "/api/permissoes-excepcionais", mapa("tipo", "TRANSFERENCIA_GESTAO_LOCAL",
                "usuarioBeneficiarioId", f.c().localB.getId(), "contratoId", f.n().id(), "finalidade", "Retificação documentada",
                "justificativa", "Ato documental conferido", "autorizacaoAdministrativa", "ATO-HTTP",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "PER012 autorização própria do gestor atual");
    }
    private JsonNode previa(Preparado f, Map<String, Object> req) throws Exception {
        return enviar(f.c().localB, base(f.n().id()) + "/transferencias-gestao/previa", req, 200,
                "AUD004 prévia concretiza intenção e dependências antes de registrar a retificação");
    }
    private JsonNode foto(Preparado f, LocalDate data) throws Exception {
        return consultar(f.c().nacional, base(f.n().id()) + "/posicao-temporal?data=" + data,
                "HIS001 contexto contratual reconstruído").path("versaoContratual");
    }
    private long transferenciaId(Preparado f) { return emTransacao(() -> em.createQuery(
            "select t.id from TransferenciaGestaoContrato t where t.contrato.id=:id", Long.class)
            .setParameter("id", f.n().id()).getSingleResult()); }
    private long contar(Preparado f, String tipo) { return emTransacao(() -> em.createQuery(
            "select count(x) from " + tipo + " x where x.contrato.id=:id", Long.class)
            .setParameter("id", f.n().id()).getSingleResult()); }
    private record Preparado(Cenario c, NovoContrato n) {}
}
