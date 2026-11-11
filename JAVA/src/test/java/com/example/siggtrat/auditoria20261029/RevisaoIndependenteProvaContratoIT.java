package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ConsultaProvaHistoricaContratoServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Propostas independentes de reprodução, ainda NÃO executadas pelo revisor.
 * Fonte alvo: SOURCE_MANIFEST.json SHA-256
 * 7d443bb0049979083ed022b0d5585c2318a35f794ac20c1f2bad9c4ba0e87d7f.
 * As perdas de dados são DML apenas sobre fixtures sintéticas descartáveis.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoIndependenteProvaContratoIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ConsultaProvaHistoricaContratoServico consultaProvas;

    @Test
    void provaParcialDeDescricaoNaoCertificaTipoDestinoAusente() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data = RelogioSistema.hoje().minusDays(2);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência sintética da revisão independente",
                "referenciaSei", "SEI-REVISAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "preparar transferência canônica");

        var req = emTransacao(() -> {
            var transferencia = em.createQuery(
                    "select t from TransferenciaGestaoContrato t where t.contrato.id=:c", TransferenciaGestaoContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            var versao = em.createQuery(
                    "select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e", VersaoContratoNegocial.class)
                    .setParameter("e", transferencia.getEvento().getId()).getSingleResult();
            // A fotografia é preservada; o mapeamento específico do Tipo se perdeu.
            Map<String, Object> foto = fotografia(versao);
            var fonte = mapa("eventoOrigemId", transferencia.getEvento().getId(),
                    "versaoAnteriorId", versao.getId(), "dataDocumento", data,
                    "referenciaSei", "SEI-SOMENTE-DESCRICAO", "motivo", "Fonte comprova somente a descrição",
                    "autorizacaoAdministrativa", "ATO-CONFERENCIA", "versaoContratoEsperada", transferencia.getContrato().getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                    "camposComprovados", List.of("descricao"), "fotografia", foto, "composicoes", List.of());
            em.createQuery("update TransferenciaGestaoContrato t set t.tipoContratacaoDestino=null, t.tipoDestinoDescricaoFotografia=null where t.id=:id")
                    .setParameter("id", transferencia.getId()).executeUpdate();
            return fonte;
        });
        enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "usuarioBeneficiarioId", c.localB.getId(), "contratoId", n.id(),
                "tipo", "INCORPORACAO_PROVA_HISTORICA", "finalidade", "Conferir a descrição documentada",
                "justificativa", "Cenário independente com perda sintética do Tipo", "autorizacaoAdministrativa", "ATO-REVISAO",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "autorizar responsável atual");

        String posicao = base(n.id()) + "/posicao-temporal?data=" + data;
        assertThat(consultar(c.localB, posicao, "controle: Tipo não comprovado antes da prova parcial")
                .path("versaoContratual").path("tipoContratacaoId").isNull()).isTrue();
        Instant conhecidoAntes = Instant.now();
        var previa = enviar(c.localB, rota(n) + "/previa", req, 200, "a descrição permanece igual à fonte original");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localB, rota(n), req, 201, "incorporar somente o campo expressamente comprovado");

        var depois = consultar(c.localB, posicao, "CON-023: prova da descrição não prova o Tipo de destino")
                .path("versaoContratual");
        assertThat(depois.path("tipoContratacaoId").isNull())
                .as("a lacuna do Tipo deve permanecer depois de prova parcial de outro campo").isTrue();
        assertThat(depois.path("tipoContratacaoComprovado").asBoolean()).isFalse();
        assertThat(consultar(c.localB, posicao + "&conhecidoAte=" + conhecidoAntes, "corte anterior preservado")
                .path("versaoContratual").path("tipoContratacaoId").isNull()).isTrue();
    }

    @Test
    void fonteDeclaradaIntegralSemEmpresaObrigatoriaDeveSerRecusada() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        long versaoId = ((Number) req.get("versaoAnteriorId")).longValue();
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.cnpjEmpresa=null, v.razaoSocialEmpresa=null, v.nomeFantasiaEmpresa=null where v.id=:id")
                    .setParameter("id", versaoId).executeUpdate();
            return null;
        });
        @SuppressWarnings("unchecked") var foto = (Map<String, Object>) req.get("fotografia");
        foto.put("cnpjEmpresa", null);
        foto.put("razaoSocialEmpresa", null);
        foto.put("nomeFantasiaEmpresa", null);
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");

        enviar(c.localA, rota(n) + "/previa", req, 400,
                "EMP-001/HIS-006: CNPJ, razão social e nome fantasia ausentes impedem declarar fonte integral");
        assertThat(emTransacao(() -> em.createQuery(
                "select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                .setParameter("c", n.id()).getSingleResult())).isZero();
    }

    @Test
    void provaDoNumeroOriginalPropagaAoMotorDepoisDeAditamentoSemDeltaDeNumero() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        @SuppressWarnings("unchecked") var foto = (Map<String, Object>) req.get("fotografia");
        String numeroOriginal = String.valueOf(foto.get("numeroContrato"));
        LocalDate dataPosterior = n.inicio().plusDays(5);
        var adi = aditamento("ADI-INDEPENDENTE-SEM-ALTERAR-NUMERO", dataPosterior);
        adi.put("novaDescricao", "Descrição própria do aditamento posterior");
        emTransacao(() -> {
            try { return registrarAditamento(c, n.id(), adi); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.numeroContrato=null where v.contrato.id=:c")
                    .setParameter("c", n.id()).executeUpdate();
            return null;
        });
        req.put("versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, n.id()).getVersion()));
        req.put("configuracaoCompletaComprovada", false);
        req.put("camposComprovados", List.of("numeroContrato"));
        req.put("composicoes", List.of());
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        confirmar(c.localA, n, req);

        Instant corte = Instant.now();
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dataPosterior + "&conhecidoAte=" + corte,
                "controle: consulta contratual recompõe o número original comprovado")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(numeroOriginal);
        assertThat(emTransacao(() -> motor.reconstruirPosicao(em.find(Contrato.class, n.id()), dataPosterior, corte)
                .versaoContratual().numeroContrato()))
                .as("o Motor consumido por frequência e revalidação deve preservar a mesma identificação no mesmo D/K")
                .isEqualTo(numeroOriginal);
    }

    @Test
    void provaSomenteDeMatriculaNaoConverteDiaReduzidoDesconhecidoEmAusenciaComprovada() throws Exception {
        var c = criarCenarioPersistido();
        var n = novoComDiaReduzido(c);
        var req = fonteOriginal(n);
        long versaoId = ((Number) req.get("versaoAnteriorId")).longValue();
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.matriculaVinculoObrigatoriaEfetiva=null, v.diaSemanaReduzido=null, v.horasDiaReduzido=null where v.id=:v")
                    .setParameter("v", versaoId).executeUpdate();
            return null;
        });
        req.put("configuracaoCompletaComprovada", false);
        req.put("camposComprovados", List.of("matriculaVinculoObrigatoriaEfetiva"));
        req.put("composicoes", List.of());
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        confirmar(c.localA, n, req);

        emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            assertThat(contrato.getDiaSemanaReduzido())
                    .as("a fonte só de matrícula não autoriza limpar a projeção operacional do dia reduzido")
                    .isEqualTo(DayOfWeek.WEDNESDAY);
            assertThat(contrato.getHorasDiaReduzido()).isEqualTo(4);
            assertThat(em.find(VersaoContratoNegocial.class, versaoId).getDiaSemanaReduzido()).isNull();
            return null;
        });
    }

    @Test
    void fragmentosDeMunicipioEGradeEmRevisoesExplicitasFormamConfiguracaoComprovada() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        @SuppressWarnings("unchecked") var original = (Map<String, Object>) ((List<?>) req.get("composicoes")).get(0);
        String grade = String.valueOf(original.get("jornadaSemanal"));
        emTransacao(() -> {
            em.createQuery("update ComposicaoContratual c set c.lotacaoMunicipioFotografia=null, c.jornadaSemanalFotografia=null where c.id=:id")
                    .setParameter("id", n.composicao1()).executeUpdate();
            return null;
        });
        req.put("configuracaoCompletaComprovada", false);
        req.put("camposComprovados", List.of());
        req.put("composicoes", List.of(mapa("composicaoId", n.composicao1(), "municipioComprovado", true,
                "lotacaoMunicipio", "Município documental independente", "jornadaComprovada", false)));
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        var primeira = confirmar(c.localA, n, req);

        req.put("chaveIdempotencia", UUID.randomUUID().toString());
        req.put("versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, n.id()).getVersion()));
        req.put("provaAnteriorId", primeira.path("id").asLong());
        req.put("versaoAnteriorId", primeira.path("versaoGeradaId").asLong());
        req.put("referenciaSei", "SEI-FRAGMENTO-SEMANA-INDEPENDENTE");
        req.put("composicoes", List.of(mapa("composicaoId", n.composicao1(), "municipioComprovado", false,
                "jornadaSemanal", grade, "jornadaComprovada", true)));
        confirmar(c.localA, n, req);

        emTransacao(() -> {
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            Instant corte = Instant.now();
            var configuracao = consultaProvas.configuracao(comp, corte);
            assertThat(configuracao.lotacaoMunicipio()).isEqualTo("Município documental independente");
            assertThat(configuracao.jornadaSemanal()).isEqualTo(grade);
            assertThat(consultaProvas.composicaoPossuiProva(comp, corte))
                    .as("as duas dimensões documentadas na mesma cadeia explícita devem habilitar o resolvedor de frequência")
                    .isTrue();
            return null;
        });
    }

    private com.fasterxml.jackson.databind.JsonNode confirmar(UsuarioAplicacao usuario, NovoContrato n,
                                                                Map<String, Object> req) throws Exception {
        var previa = enviar(usuario, rota(n) + "/previa", req, 200, "prévia independente da fonte autorizada");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        return enviar(usuario, rota(n), req, 201, "confirmar a mesma intenção documental");
    }

    private NovoContrato novoComDiaReduzido(Cenario c) {
        return emTransacao(() -> {
            try {
                LocalDate inicio = RelogioSistema.hoje().minusDays(30), fim = RelogioSistema.hoje().plusDays(120);
                var cadastro = rascunho(c, "REVISOR-" + UUID.randomUUID().toString().substring(0, 8), 10, inicio, fim);
                cadastro.put("diaSemanaReduzido", DayOfWeek.WEDNESDAY);
                cadastro.put("horasDiaReduzido", 4);
                long id = enviar(c.localA, "/api/contratos", cadastro, 201, "rascunho com redução expressa de jornada")
                        .path("id").asLong();
                long comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 10),
                        201, "composição original").path("id").asLong();
                var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-REDUCAO-ORIGINAL",
                        "justificativa", "Publicação sintética com dia reduzido explícito");
                var previa = enviar(c.localA, base(id) + "/previas/publicar", publicacao, 200, "prévia de publicação retroativa");
                publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
                publicacao.put("confirmarImpacto", true);
                enviar(c.localA, base(id) + "/publicar", publicacao, 200, "publicar configuração original expressa");
                em.flush();
                return new NovoContrato(id, comp, null, inicio, fim);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private NovoContrato novo(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private String rota(NovoContrato n) { return base(n.id()) + "/provas-historicas"; }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografia(VersaoContratoNegocial versao) {
        Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(versao), LinkedHashMap.class);
        for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
            foto.remove(campo);
        return foto;
    }

    private Map<String, Object> fonteOriginal(NovoContrato n) {
        return emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var versao = motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow();
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            return mapa("eventoOrigemId", versao.getEventoOrigem().getId(), "versaoAnteriorId", versao.getId(),
                    "dataDocumento", RelogioSistema.hoje().minusDays(40), "referenciaSei", "SEI-FONTE-INDEPENDENTE",
                    "motivo", "Fonte histórica sintética para revisão independente",
                    "autorizacaoAdministrativa", "ATO-CONFERENCIA", "versaoContratoEsperada", contrato.getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", fotografia(versao), "composicoes", List.of(item));
        });
    }
}
