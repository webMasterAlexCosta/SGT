package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Regressões derivadas das regras originais, verificadas pela API, serviços e MySQL.
 * Cada comando tem sua própria transação, inclusive os comandos recusados.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasRegrasOriginaisHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";
    private static final String EVIDENCIA = "SEI-REGRAS-ORIGINAIS-PESSOAS";
    private static final String AUTORIZACAO = "ATO-REGRAS-ORIGINAIS-PESSOAS";

    @Test
    void cancelarAlocacaoProgramadaExigeConsentimentoParaExcedenteRestaurado() throws Exception {
        Preparado f = preparar(true);
        LocalDate mudanca = RelogioSistema.hoje().plusDays(10);
        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1))
                .path("id").asLong();
        long alocacaoProgramada = mudar(f, vinculo, mudanca, f.contrato().composicao2()).path("id").asLong();
        cadastrar(f, 1, f.contrato().composicao1(), mudanca);
        JsonNode trilhaAntes = eventosAlocacao(f, vinculo);
        Map<String, Object> req = acao();

        enviar(f.c().localA, V + "/" + vinculo + "/alocacoes/" + alocacaoProgramada + "/cancelar-programacao",
                req, 409, "CMP009 ALO003 cancelar programação não pode restaurar ocupação excedente sem consentimento específico");

        assertThat(posicao(f, vinculo, mudanca).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao2());
        assertThat(eventosAlocacao(f, vinculo)).isEqualTo(trilhaAntes);
        emTransacao(() -> {
            assertThat(em.find(AlocacaoVinculo.class, alocacaoProgramada).getEstadoNegocial())
                    .isEqualTo(EstadoNegocialAlocacao.REGISTRADA);
            return null;
        });

        req.put("confirmarInclusaoExcedente", true);
        enviar(f.c().localA, V + "/" + vinculo + "/alocacoes/" + alocacaoProgramada + "/cancelar-programacao",
                req, 200, "CMP008 CMP010 restauração excedente é permitida quando explicitamente autorizada e documentada");
        assertThat(posicao(f, vinculo, mudanca).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao1());
        emTransacao(() -> {
            List<EventoAlocacaoVinculo> excedentes = em.createQuery(
                            "select e from EventoAlocacaoVinculo e where e.vinculo.id=:v and e.inclusaoExcedente=true",
                            EventoAlocacaoVinculo.class)
                    .setParameter("v", vinculo).getResultList();
            assertThat(excedentes).hasSize(1);
            EventoAlocacaoVinculo evento = excedentes.get(0);
            assertThat(evento.getEfetivoPrevistoExcedente()).isEqualTo(1);
            assertThat(evento.getOcupacaoAposInclusaoExcedente()).isEqualTo(2L);
            assertThat(evento.getEvidencia()).isEqualTo(EVIDENCIA);
            assertThat(evento.getAutorizacao()).isEqualTo(AUTORIZACAO);
            return null;
        });
        exigirEventosPreservados(trilhaAntes, eventosAlocacao(f, vinculo));
    }

    @Test
    void anteciparInicioExigeConsentimentoParaExcedenteNoPeriodoAcrescido() throws Exception {
        Preparado f = preparar(false);
        LocalDate inicioAnterior = RelogioSistema.hoje().plusDays(10);
        long ocupante = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1))
                .path("id").asLong();
        enviar(f.c().localA, V + "/" + ocupante + "/desligar", desligamento(f, inicioAnterior.minusDays(1)),
                200, "Preparação: o ocupante anterior termina antes do início original do segundo vínculo");
        long vinculo = cadastrar(f, 1, f.contrato().composicao1(), inicioAnterior).path("id").asLong();
        JsonNode trilhaAntes = eventosVinculo(f, vinculo);
        JsonNode alocacoesAntes = eventosAlocacao(f, vinculo);
        Map<String, Object> req = acao();
        req.put("novoInicio", RelogioSistema.hoje().plusDays(5));
        JsonNode previa = previaInicio(f, vinculo, req);

        enviar(f.c().localA, V + "/" + vinculo + "/retificar-inicio", req, 409,
                "VIN014 CMP009 antecipação revalida capacidade de toda a faixa acrescentada e exige consentimento de excedente");

        assertThat(previa.path("impactos").toString().toUpperCase()).contains("EXCEDENTE");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataInicio())).isEqualTo(inicioAnterior);
        assertThat(eventosVinculo(f, vinculo)).isEqualTo(trilhaAntes);
        assertThat(eventosAlocacao(f, vinculo)).isEqualTo(alocacoesAntes);

        req.put("confirmarInclusaoExcedente", true);
        previaInicio(f, vinculo, req);
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-inicio", req, 200,
                "VIN014 CMP008 antecipação excedente documentada continua permitida, sem teto técnico artificial");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataInicio()))
                .isEqualTo(RelogioSistema.hoje().plusDays(5));
        List<JsonNode> retificacoes = eventosDoTipo(eventosVinculo(f, vinculo), "tipo", "RETIFICACAO_INICIO");
        assertThat(retificacoes).hasSize(1);
        assertThat(retificacoes.get(0).path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(retificacoes.get(0).path("efetivoPrevistoExcedente").asInt()).isEqualTo(1);
        assertThat(retificacoes.get(0).path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2);
        assertThat(retificacoes.get(0).path("evidencia").asText()).isEqualTo(EVIDENCIA);
        assertThat(retificacoes.get(0).path("autorizacao").asText()).isEqualTo(AUTORIZACAO);
        exigirEventosPreservados(trilhaAntes, eventosVinculo(f, vinculo));
    }

    @Test
    void desligamentoFuturoNaoCancelaSucessoraProgramadaSemTratamentoExplicito() throws Exception {
        Preparado f = preparar(true);
        LocalDate inicioSucessora = RelogioSistema.hoje().plusDays(20);
        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1))
                .path("id").asLong();
        long sucessora = mudar(f, vinculo, inicioSucessora, f.contrato().composicao2()).path("id").asLong();
        JsonNode trilhaVinculo = eventosVinculo(f, vinculo);
        JsonNode trilhaAlocacao = eventosAlocacao(f, vinculo);
        Map<String, Object> req = desligamento(f, RelogioSistema.hoje().plusDays(10));

        enviar(f.c().localA, V + "/" + vinculo + "/desligar", req, 409,
                "TMP009 ALO003 §5.26 programação futura órfã deve ser tratada antes de concluir o desligamento que a inviabiliza");

        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataFim())).isNull();
        assertThat(posicao(f, vinculo, inicioSucessora).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao2());
        assertThat(eventosVinculo(f, vinculo)).isEqualTo(trilhaVinculo);
        assertThat(eventosAlocacao(f, vinculo)).isEqualTo(trilhaAlocacao);

        enviar(f.c().localA, V + "/" + vinculo + "/alocacoes/" + sucessora + "/cancelar-programacao", acao(), 200,
                "ALO003 tratamento explícito cancela a programação futura e preserva seu fato de origem");
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", req, 200,
                "DES006 o desligamento futuro pode ser concluído depois do tratamento explícito da sucessora");
        enviar(f.c().localA, V + "/" + vinculo + "/cancelar-desligamento-programado", acao(), 200,
                "DES006 cancelamento de desligamento reabre cobertura sem desfazer cancelamento explícito de alocação");
        assertThat(posicao(f, vinculo, inicioSucessora).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao1());
        emTransacao(() -> {
            assertThat(em.find(AlocacaoVinculo.class, sucessora).getEstadoNegocial())
                    .isEqualTo(EstadoNegocialAlocacao.CANCELADA);
            return null;
        });
        exigirEventosPreservados(trilhaVinculo, eventosVinculo(f, vinculo));
        exigirEventosPreservados(trilhaAlocacao, eventosAlocacao(f, vinculo));
    }

    @Test
    void retificacaoDeRetificacaoDoInicioRecuperaAlocacaoHistoricaOriginal() throws Exception {
        Preparado f = preparar(true);
        LocalDate inicioOriginal = RelogioSistema.hoje().minusDays(20);
        LocalDate inicioSucessora = RelogioSistema.hoje().minusDays(10);
        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), inicioOriginal).path("id").asLong();
        mudar(f, vinculo, inicioSucessora, f.contrato().composicao2());
        JsonNode trilhaVinculo = eventosVinculo(f, vinculo);
        JsonNode trilhaAlocacao = eventosAlocacao(f, vinculo);

        retificarInicio(f, vinculo, RelogioSistema.hoje().minusDays(5));
        assertThat(posicao(f, vinculo, inicioOriginal.plusDays(5)).path("alocacaoId").isNull())
                .as("Não há alocação efetiva antes do início retificado do vínculo").isTrue();
        retificarInicio(f, vinculo, inicioOriginal);

        assertThat(posicao(f, vinculo, inicioOriginal.plusDays(5)).path("composicaoBaseId").asLong())
                .as("VIN007 VIN014 a segunda retificação recupera a combinação original do trecho restaurado")
                .isEqualTo(f.contrato().composicao1());
        assertThat(posicao(f, vinculo, inicioSucessora.plusDays(2)).path("composicaoBaseId").asLong())
                .as("ALO003 o evento posterior válido mantém a combinação e a data efetiva originais")
                .isEqualTo(f.contrato().composicao2());
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataInicio())).isEqualTo(inicioOriginal);
        assertThat(eventosDoTipo(eventosVinculo(f, vinculo), "tipo", "RETIFICACAO_INICIO")).hasSize(2);
        exigirEventosPreservados(trilhaVinculo, eventosVinculo(f, vinculo));
        exigirEventosPreservados(trilhaAlocacao, eventosAlocacao(f, vinculo));
    }

    @Test
    void retificacaoDeDesligamentoRecuperaSucessoraHistoricaAtingidaPeloLimite() throws Exception {
        Preparado f = preparar(true);
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.motivoId(), f.c().gestaoA.getId(), f.c().localA,
                    "Desligamento individual documentado para restauração da sucessora", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    RelogioSistema.hoje().minusDays(15), RelogioSistema.hoje().minusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-REGRAS-RESTAURACAO");
            return null;
        });

        LocalDate inicioOriginal = RelogioSistema.hoje().minusDays(20);
        LocalDate inicioSucessora = RelogioSistema.hoje().minusDays(10);
        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), inicioOriginal).path("id").asLong();
        mudar(f, vinculo, inicioSucessora, f.contrato().composicao2());
        JsonNode trilhaAlocacao = eventosAlocacao(f, vinculo);
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", desligamento(f, RelogioSistema.hoje().minusDays(15)),
                200, "DES003 desligamento retroativo legítimo continua permitido, com os efeitos históricos auditados");
        JsonNode trilhaVinculo = eventosVinculo(f, vinculo);
        assertThat(posicao(f, vinculo, RelogioSistema.hoje().minusDays(5)).path("situacaoTemporal").asText())
                .isEqualTo("SEM_ALOCACAO");

        Map<String, Object> req = desligamento(f, RelogioSistema.hoje().minusDays(1));
        confirmar(req, enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "DES005 a prévia considera a linha temporal a restaurar na nova data final"));
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", req, 200,
                "DES005 retificação do fim restaura os fatos posteriores que voltaram a ser válidos");

        assertThat(posicao(f, vinculo, RelogioSistema.hoje().minusDays(5)).path("composicaoBaseId").asLong())
                .as("DES005 ALO003 a ampliação não substitui silenciosamente a sucessora histórica pela combinação inicial")
                .isEqualTo(f.contrato().composicao2());
        assertThat(posicao(f, vinculo, inicioOriginal.plusDays(2)).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao1());
        assertThat(posicao(f, vinculo, RelogioSistema.hoje()).path("situacaoTemporal").asText())
                .isEqualTo("SEM_ALOCACAO");
        assertThat(eventosDoTipo(eventosVinculo(f, vinculo), "tipo", "RETIFICACAO_DESLIGAMENTO")).hasSize(1);
        exigirEventosPreservados(trilhaVinculo, eventosVinculo(f, vinculo));
        exigirEventosPreservados(trilhaAlocacao, eventosAlocacao(f, vinculo));
    }

    @Test
    void ampliarDesligamentoRevalidaExcedenteDaSucessoraHistoricaRestaurada() throws Exception {
        Preparado f = preparar(true);
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.motivoId(), f.c().gestaoA.getId(), f.c().localA,
                    "Desligamento individual documentado para reavaliar excedente da sucessora", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    RelogioSistema.hoje().minusDays(15), RelogioSistema.hoje().minusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-REGRAS-EXCEDENTE");
            return null;
        });

        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().minusDays(20))
                .path("id").asLong();
        mudar(f, vinculo, RelogioSistema.hoje().minusDays(10), f.contrato().composicao2());
        LocalDate fimAnterior = RelogioSistema.hoje().minusDays(15);
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", desligamento(f, fimAnterior), 200,
                "DES003 preparação: término retroativo anterior à sucessora histórica");
        cadastrar(f, 1, f.contrato().composicao2(), RelogioSistema.hoje().minusDays(5));
        Map<String, Object> req = desligamento(f, RelogioSistema.hoje().minusDays(1));
        JsonNode previa = enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "DES005 CMP009 a prévia considera cada combinação da faixa histórica restaurada");
        confirmar(req, previa);

        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", req, 409,
                "CMP009 a ampliação não pode avaliar somente a combinação inicial e ignorar o excedente da sucessora restaurada");
        assertThat(previa.path("impactos").toString().toUpperCase()).contains("EXCEDENTE");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataFim())).isEqualTo(fimAnterior);

        req.put("confirmarInclusaoExcedente", true);
        confirmar(req, enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "Prévia atualizada com a decisão explícita sobre o excedente da sucessora"));
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", req, 200,
                "CMP008 DES005 a sucessora é restaurada com consentimento e auditoria do excedente real");
        JsonNode evento = eventosDoTipo(eventosVinculo(f, vinculo), "tipo", "RETIFICACAO_DESLIGAMENTO").get(0);
        assertThat(evento.path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(evento.path("composicaoExcedenteId").asLong()).isEqualTo(f.contrato().composicao2());
        assertThat(evento.path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2);
        assertThat(posicao(f, vinculo, RelogioSistema.hoje().minusDays(5)).path("composicaoBaseId").asLong())
                .isEqualTo(f.contrato().composicao2());
    }

    @Test
    void restauracaoComExcedenteEmDuasCombinacoesExibeJustificativaEmCadaFaixa() throws Exception {
        Preparado f = preparar(true);
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.motivoId(), f.c().gestaoA.getId(), f.c().localA,
                    "Desligamento individual documentado nas duas combinações restauradas", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    RelogioSistema.hoje().minusDays(15), RelogioSistema.hoje().minusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-REGRAS-DUAS-COMBINACOES");
            return null;
        });

        long vinculo = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().minusDays(20))
                .path("id").asLong();
        mudar(f, vinculo, RelogioSistema.hoje().minusDays(10), f.contrato().composicao2());
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", desligamento(f, RelogioSistema.hoje().minusDays(15)),
                200, "Preparação: corte retroativo deixa livres as duas combinações em períodos sucessivos");
        long ocupanteA = cadastrar(f, 1, f.contrato().composicao1(), RelogioSistema.hoje().minusDays(14))
                .path("id").asLong();
        enviar(f.c().localA, V + "/" + ocupanteA + "/desligar", desligamento(f, RelogioSistema.hoje().minusDays(11)),
                200, "Preparação: outro vínculo ocupa A somente no primeiro trecho restaurado");
        cadastrar(f, 2, f.contrato().composicao2(), RelogioSistema.hoje().minusDays(5));
        Map<String, Object> req = desligamento(f, RelogioSistema.hoje().minusDays(1));
        req.put("confirmarInclusaoExcedente", true);
        confirmar(req, enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "CMP009 o gestor recebe a prévia e autoriza explicitamente a restauração excedente"));
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", req, 200,
                "CMP008 restauração excedente em várias combinações não sofre teto técnico artificial");

        conferirJustificativaDaCombinacao(f, f.contrato().composicao1(), RelogioSistema.hoje().minusDays(12));
        conferirJustificativaDaCombinacao(f, f.contrato().composicao2(), RelogioSistema.hoje().minusDays(4));
    }

    @Test
    void alocacaoRetroativaUsaAssociacaoHistoricaDaComposicaoPublicada() throws Exception {
        Preparado f = prepararContratoHistorico();
        JsonNode composicao = consultar(f.c().localA, base(f.contrato().id()) + "/composicoes",
                "Preparação: composição publicada preserva o contexto de Lotação e Turno").get(0);
        long lotacaoId = composicao.path("lotacaoId").asLong();
        ObjectNode cadastroAtual = (ObjectNode) consultar(f.c().localA, "/api/lotacoes/" + lotacaoId,
                "Consulta do cadastro mestre da Lotação histórica");
        cadastroAtual.putArray("turnos");
        executar(f.c().localA, put("/api/lotacoes/" + lotacaoId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastroAtual)), 200,
                "CAT004 a edição atual do cadastro não altera a composição contratual anteriormente publicada");

        LocalDate dataFato = RelogioSistema.hoje().minusDays(20);
        JsonNode criado = cadastrar(f, 0, f.contrato().composicao1(), dataFato);

        JsonNode posicao = posicao(f, criado.path("id").asLong(), dataFato);
        assertThat(posicao.path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao1());
        assertThat(posicao.path("lotacaoId").asLong()).isEqualTo(lotacaoId);
        assertThat(posicao.path("jornadaId").asLong()).isEqualTo(f.c().jornadaA.getId());
        assertThat(consultar(f.c().localA, "/api/lotacoes/" + lotacaoId,
                "O registro retroativo usa a composição histórica sem reescrever a configuração atual").path("turnos").size())
                .isZero();
    }

    private Preparado preparar(boolean duasComposicoes) {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                NovoContrato contrato = publicado(c, duasComposicoes ? 2 : 1, duasComposicoes ? Integer.valueOf(1) : null, false);
                return new Preparado(c, contrato, criarMotivo(c, contrato.id()));
            } catch (Exception e) {
                throw new IllegalStateException("Falha na preparação HTTP das regras originais de pessoas", e);
            }
        });
    }

    private Preparado prepararContratoHistorico() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                // A fixture declarará fonte explícita da disponibilidade do catálogo no período histórico.
                Lotacao lotacao = new Lotacao("Lotação histórica " + UUID.randomUUID().toString().substring(0, 8));
                lotacao.setGestaoLocal(c.gestaoA);
                lotacao.setMunicipio(c.lotacaoA.getMunicipio());
                lotacao.setTurnos(new ArrayList<>(List.of(c.jornadaA)));
                em.persist(lotacao);
                em.flush();
                LocalDate inicio = RelogioSistema.hoje().minusDays(30);
                LocalDate fim = RelogioSistema.hoje().minusDays(1);
                // Fonte sintética positiva, independente da flag mestre atual.
                var origemLotacao = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
                origemLotacao.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO);
                origemLotacao.setCatalogoId(lotacao.getId()); origemLotacao.setDataEfetiva(inicio); origemLotacao.setAtivo(true);
                origemLotacao.setMotivo("Fonte sintética explícita da Lotação desde o início da composição histórica");
                origemLotacao.setResponsavelId(c.localA.getId()); origemLotacao.setResponsavelUsername(c.localA.getUsername());
                em.persist(origemLotacao); em.flush();
                long contratoId = enviar(c.localA, "/api/contratos",
                        rascunho(c, "HIST-" + UUID.randomUUID().toString().substring(0, 8), 1, inicio, fim),
                        201, "Preparação: contrato com vigência histórica real").path("id").asLong();
                long composicaoId = enviar(c.localA, base(contratoId) + "/composicoes",
                        mapa("lotacaoId", lotacao.getId(), "jornadaId", c.jornadaA.getId(),
                                "cargoId", c.cargoA.getId(), "efetivoPrevisto", 1),
                        201, "CMP001 associação Lotação–Turno era válida na composição original").path("id").asLong();
                Map<String, Object> publicar = mapa("dataPublicacao", RelogioSistema.hoje(),
                        "referenciaSei", EVIDENCIA, "justificativa", "Registro documental da versão contratual histórica");
                JsonNode previa = enviar(c.localA, base(contratoId) + "/previas/publicar", publicar, 200,
                        "CON014 publicação retroativa tem prévia e confirmação próprias");
                publicar.put("tokenPrevia", previa.path("tokenPrevia").asText());
                publicar.put("confirmarImpacto", true);
                enviar(c.localA, base(contratoId) + "/publicar", publicar, 200,
                        "HIS006 publicação fotografa a composição Lotação–Turno–Cargo original");
                return new Preparado(c, new NovoContrato(contratoId, composicaoId, null, inicio, fim),
                        criarMotivo(c, contratoId));
            } catch (Exception e) {
                throw new IllegalStateException("Falha na preparação HTTP do contrato histórico", e);
            }
        });
    }

    private long criarMotivo(Cenario c, long contratoId) {
        MotivoDesligamento motivo = new MotivoDesligamento();
        motivo.setGestaoLocal(c.gestaoA);
        motivo.setDescricao("Desligamento individual regras originais " + contratoId);
        motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        em.persist(motivo);
        em.flush();
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId());
        origem.setDataEfetiva(em.find(com.example.siggtrat.entidade.Contrato.class, contratoId).getDataInicio());
        origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa individual desde o início do contrato deste cenário");
        em.persist(origem); em.flush();
        return motivo.getId();
    }

    private JsonNode cadastrar(Preparado f, int ordinal, long composicaoId, LocalDate inicio) throws Exception {
        int seed = Math.toIntExact(700000000L + f.contrato().id() * 10L + ordinal);
        return enviar(f.c().localA, V, mapa("pessoa", mapa("nome", "Pessoa regras originais " + seed, "cpf", cpf(seed)),
                        "contratoId", f.contrato().id(), "composicaoId", composicaoId, "dataInicio", inicio,
                        "confirmarInclusaoExcedente", false),
                201, "VIN001 ALO001 cadastro integrado utiliza a combinação publicada válida na data do fato");
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Correção documentada conforme os fatos originais",
                "evidencia", EVIDENCIA, "autorizacao", AUTORIZACAO, "confirmarInclusaoExcedente", false);
    }

    private Map<String, Object> desligamento(Preparado f, LocalDate fim) {
        Map<String, Object> req = acao();
        req.put("dataFim", fim);
        req.put("motivoDesligamentoId", f.motivoId());
        return req;
    }

    private JsonNode mudar(Preparado f, long vinculo, LocalDate data, long composicaoId) throws Exception {
        Map<String, Object> req = acao();
        req.put("dataEfetiva", data);
        req.put("composicaoId", composicaoId);
        confirmar(req, enviar(f.c().localA, V + "/" + vinculo + "/previas/alocacoes", req, 200,
                "ALO003 AUD004 prévia da mudança explícita de alocação"));
        return enviar(f.c().localA, V + "/" + vinculo + "/alocacoes", req, 200,
                "ALO002 nova combinação cria faixa no mesmo vínculo e mantém a faixa anterior");
    }

    private JsonNode previaInicio(Preparado f, long vinculo, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-inicio", req, 200,
                "VIN014 prévia vinculada ao conteúdo e às dependências da retificação do início");
        confirmar(req, previa);
        return previa;
    }

    private void retificarInicio(Preparado f, long vinculo, LocalDate inicio) throws Exception {
        Map<String, Object> req = acao();
        req.put("novoInicio", inicio);
        previaInicio(f, vinculo, req);
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-inicio", req, 200,
                "VIN014 retificação auditada do início preserva a cadeia e revalida a alocação");
    }

    private void confirmar(Map<String, Object> req, JsonNode previa) {
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("token").asText());
    }

    private JsonNode posicao(Preparado f, long vinculo, LocalDate data) throws Exception {
        return consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + data,
                "ALO007 consulta a alocação efetiva do dia, sem inferir pela projeção atual");
    }

    private JsonNode eventosVinculo(Preparado f, long vinculo) throws Exception {
        return consultar(f.c().localA, V + "/" + vinculo + "/eventos", "VIN007 trilha imutável do vínculo");
    }

    private JsonNode eventosAlocacao(Preparado f, long vinculo) throws Exception {
        return consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/eventos", "ALO003 trilha imutável da alocação");
    }

    private List<JsonNode> eventosDoTipo(JsonNode eventos, String campo, String tipo) {
        List<JsonNode> encontrados = new ArrayList<>();
        eventos.forEach(evento -> {
            if (tipo.equals(evento.path(campo).asText())) encontrados.add(evento);
        });
        return encontrados;
    }

    private void conferirJustificativaDaCombinacao(Preparado f, long composicao, LocalDate data) throws Exception {
        JsonNode excedentes = consultar(f.c().localA, base(f.contrato().id()) + "/composicoes/excedentes?data=" + data,
                "CMP011 cada composição excedente exibe a decisão que autorizou a ocupação daquele trecho");
        List<JsonNode> linhas = new ArrayList<>();
        excedentes.forEach(linha -> { if (linha.path("composicaoId").asLong() == composicao) linhas.add(linha); });
        assertThat(linhas).hasSize(1);
        JsonNode linha = linhas.get(0);
        assertThat(linha.path("quantidadeExcedente").asLong()).isEqualTo(1);
        List<JsonNode> justificativas = new ArrayList<>();
        linha.path("justificativas").forEach(j -> {
            if (EVIDENCIA.equals(j.path("evidencia").asText()) && AUTORIZACAO.equals(j.path("autorizacao").asText()))
                justificativas.add(j);
        });
        assertThat(justificativas).as("A composição %s deve expor a autorização aplicável em %s", composicao, data).isNotEmpty();
    }

    private void exigirEventosPreservados(JsonNode antes, JsonNode depois) {
        List<JsonNode> atuais = new ArrayList<>();
        depois.forEach(atuais::add);
        antes.forEach(evento -> assertThat(atuais).as("A versão original do evento %s permanece intacta", evento.path("id"))
                .contains(evento));
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoId) {}
}
