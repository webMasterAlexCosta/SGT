package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** VIN012: propostas não executadas. Todos os dados e fontes são sintéticos.
 * Contrato/composição/vínculo/fim/prorrogação/política são preparados por suas APIs.
 * JPA apenas declara o catálogo/fonte de Motivo de teste e lê fatos por PK.
 * Não há DML para alterar limites publicados ou fabricar a invalidez do alvo. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReaberturaFimNaturalRevalidacoesHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;
    private static final String V = "/api/vinculos-contratuais";
    private record F(Cenario c, NovoContrato n, long motivo, long vinculo, long pessoa, long alocacao, List<Long> faixas,
                     LocalDate fimAntigo, LocalDate fimNovo) { }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reaberturaDeFimNaturalRevalidaCapacidadeDaFaixaRestauradaAntesDeGravar(boolean comSucessoraHistorica) throws Exception {
        F f = preparar(false, comSucessoraHistorica);
        long composicaoVigente = f.n.composicao2() == null ? f.n.composicao1() : f.n.composicao2();
        JsonNode posterior = enviar(f.c.localA, V, mapa("pessoa", mapa("nome", "Ocupante posterior inteiramente sintético",
                        "cpf", cpf(Math.toIntExact(832000000L + f.n.id()))), "contratoId", f.n.id(),
                        "composicaoId", composicaoVigente, "dataInicio", f.fimAntigo.plusDays(1)), 201,
                "VIN012 controle: outra Pessoa ocupa legitimamente a vaga após o fim natural já preservado");
        long posteriorId = posterior.path("id").asLong();
        assertThat(posterior.path("pessoaId").asLong()).isNotEqualTo(f.pessoa);
        var antes = estado(f, f.vinculo);
        var antesPosterior = estado(f, posteriorId);
        Map<String,Object> req = pedido(f);
        JsonNode previa = confirmarPrevia(f, req);
        assertThat(previa.path("vinculosConflitantes")).isEmpty();
        assertThat(previa.path("impactos").toString()).contains("ocupação excedente", f.fimAntigo.plusDays(1).toString());
        JsonNode recusa = enviar(f.c.localA, V + "/" + f.vinculo + "/retificar-desligamento", req, 409,
                "VIN012 comando explícito/impacto não dispensam consentimento específico de capacidade restaurada");
        assertThat(recusa.path("message").asText()).containsIgnoringCase("excedente");
        assertThat(estado(f, f.vinculo)).as("Nenhuma alteração parcial no vínculo/faixas/trilhas após recusa").isEqualTo(antes);
        assertThat(estado(f, posteriorId)).isEqualTo(antesPosterior);
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, f.vinculo).getDataFim()).isEqualTo(f.fimAntigo);
            assertThat(em.find(AlocacaoVinculo.class, f.alocacao).getDataFim()).isEqualTo(f.fimAntigo);
            return null;
        });

        req.put("confirmarInclusaoExcedente", true);
        confirmarPrevia(f, req);
        JsonNode corrigido = enviar(f.c.localA, V + "/" + f.vinculo + "/retificar-desligamento", req, 200,
                "VIN012 controle positivo: mesmas datas/Pessoa/contrato com consentimento expresso de excedente");
        assertThat(corrigido.path("id").asLong()).isEqualTo(f.vinculo);
        assertThat(corrigido.path("pessoaId").asLong()).isEqualTo(f.pessoa);
        conferirLimites(f);
        JsonNode eventos = consultar(f.c.localA, V + "/" + f.vinculo + "/eventos", "VIN012 decisão explícita conserva fim natural e encadeia correção");
        assertEventosAnterioresPreservados(antes.get("/eventos"), eventos);
        var correcoes = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> "RETIFICACAO_DESLIGAMENTO".equals(e.path("tipo").asText())).toList();
        assertThat(correcoes).hasSize(1);
        assertThat(correcoes.get(0).path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(correcoes.get(0).path("composicaoExcedenteId").asLong()).isEqualTo(composicaoVigente);
        assertThat(correcoes.get(0).path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2L);
        assertThat(correcoes.get(0).path("evidencia").asText()).isEqualTo("SEI-SINTETICO-VIN012-REABERTURA");
        assertThat(estado(f, posteriorId)).as("Autorizar a ampliação de A não altera o vínculo de B").isEqualTo(antesPosterior);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reaberturaDeFimNaturalRevalidaMatriculaNaFronteiraRestaurada(boolean matriculaInformada) throws Exception {
        F f = preparar(matriculaInformada, false);
        LocalDate fronteira = f.fimAntigo.plusDays(2);
        Map<String,Object> obrigacao = aditamento("VIN012-EXIGENCIA-RESTAURADA-" + f.n.id(), fronteira);
        obrigacao.put("novaMatriculaVinculoObrigatoria", true);
        emTransacao(() -> {
            try { registrarAditamento(f.c, f.n.id(), obrigacao); return null; }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        var antes = estado(f, f.vinculo);
        Map<String,Object> req = pedido(f);
        confirmarPrevia(f, req);
        JsonNode politicaAntes = consultar(f.c.localA, base(f.n.id()) + "/posicao-temporal?data=" + fronteira.minusDays(1),
                "VIN012 controle: exigência não é retroagida para a véspera").path("versaoContratual");
        JsonNode politicaDepois = consultar(f.c.localA, base(f.n.id()) + "/posicao-temporal?data=" + fronteira,
                "VIN012 exigência começa inclusive na fronteira da faixa a restaurar").path("versaoContratual");
        assertThat(politicaAntes.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(politicaAntes.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        assertThat(politicaDepois.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(politicaDepois.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        if (!matriculaInformada) {
            JsonNode recusa = enviar(f.c.localA, V + "/" + f.vinculo + "/retificar-desligamento", req, 409,
                    "VIN012 matrícula ausente bloqueia reabertura que atravessa obrigação já publicada");
            assertThat(recusa.path("message").asText()).containsIgnoringCase("matrícula");
            assertThat(estado(f, f.vinculo)).isEqualTo(antes);
        } else {
            JsonNode corrigido = enviar(f.c.localA, V + "/" + f.vinculo + "/retificar-desligamento", req, 200,
                    "VIN012 controle: matrícula presente permite restaurar o mesmo vínculo sobre a mesma fronteira");
            assertThat(corrigido.path("id").asLong()).isEqualTo(f.vinculo);
            assertThat(corrigido.path("pessoaId").asLong()).isEqualTo(f.pessoa);
            conferirLimites(f);
            assertEventosAnterioresPreservados(antes.get("/eventos"), consultar(f.c.localA,
                    V + "/" + f.vinculo + "/eventos", "VIN012 criação e fim natural não são reescritos pela reabertura"));
        }
    }

    private F preparar(boolean matricula, boolean comSucessoraHistorica) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().minusDays(30), fimAntigo = RelogioSistema.hoje().minusDays(5);
        LocalDate fimNovo = RelogioSistema.hoje().plusDays(10);
        NovoContrato n = emTransacao(() -> {
            try {
                long id = enviar(c.localA, "/api/contratos", rascunho(c, "VIN012-API-" + UUID.randomUUID().toString().substring(0, 8),
                        comSucessoraHistorica ? 2 : 1, inicio, fimAntigo), 201, "Contrato sintético com vigência original expirada, criado pela API").path("id").asLong();
                long comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 1), 201,
                        "Composição original de uma vaga, criada pela API").path("id").asLong();
                Long comp2 = null;
                if (comSucessoraHistorica) {
                    Cargo segundo = new Cargo("Cargo sucessor sintético VIN012 " + id, "2");
                    segundo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId())); em.persist(segundo); em.flush();
                    EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                    origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(segundo.getId()); origem.setDataEfetiva(inicio);
                    origem.setAtivo(true); origem.setMotivo("Disponibilidade sintética do cargo da sucessora desde o início"); em.persist(origem); em.flush();
                    comp2 = enviar(c.localA, base(id) + "/composicoes", composicao(c, segundo.getId(), 1), 201,
                            "Segunda composição original válida criada pela API").path("id").asLong();
                }
                var req = mapa("dataPublicacao", inicio, "referenciaSei", "SEI-SINTETICO-VIN012-ORIGINAL", "confirmarImpacto", true);
                req.put("tokenPrevia", enviar(c.localA, base(id) + "/previas/publicar", req, 200,
                        "Prévia legítima da publicação retroativa").path("tokenPrevia").asText());
                enviar(c.localA, base(id) + "/publicar", req, 200, "Publicação original confirmada antes do vínculo");
                return new NovoContrato(id, comp, comp2, inicio, fimAntigo);
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        long natural = emTransacao(() -> {
            MotivoDesligamento m = new MotivoDesligamento(); m.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            m.setDescricao("Fim natural sintético VIN012 " + n.id()); m.setCategoria(CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
            em.persist(m); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
            origem.setDataEfetiva(inicio); origem.setAtivo(true); origem.setMotivo("Disponibilidade sintética declarada do motivo desde o início");
            em.persist(origem);
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, n.id(), m.getId(), c.gestaoA.getId(), c.localA,
                    "Fim natural sintético do contrato API", CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                    fimAntigo, fimNovo, Instant.now(), "SEI-SINTETICO-VIN012-MOTIVO");
            return m.getId();
        });
        Map<String,Object> cadastro = mapa("pessoa", mapa("nome", "Pessoa reabertura natural inteiramente sintética",
                "cpf", cpf(Math.toIntExact(831000000L + n.id()))), "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", inicio);
        if (matricula) cadastro.put("matricula", "VIN012-" + n.id());
        JsonNode criado = enviar(c.localA, V, cadastro, 201, "Admissão sintética legítima no contrato original");
        long vinculo = criado.path("id").asLong(), pessoa = criado.path("pessoaId").asLong();
        if (comSucessoraHistorica) {
            Map<String,Object> mudanca = mapa("dataEfetiva", fimAntigo.minusDays(2), "composicaoId", n.composicao2(),
                    "justificativa", "Sucessora histórica sintética decidida antes do fim natural", "evidencia", "SEI-SINTETICO-VIN012-SUCESSORA",
                    "autorizacao", "ATO-SINTETICO-VIN012-SUCESSORA", "confirmarImpacto", true);
            var previa = enviar(c.localA, V + "/" + vinculo + "/previas/alocacoes", mudanca, 200,
                    "Prévia legítima da mudança de Alocação anterior ao fim natural");
            mudanca.put("tokenPrevia", previa.path("token").asText());
            enviar(c.localA, V + "/" + vinculo + "/alocacoes", mudanca, 200,
                    "Sucessora criada por comando API legítimo, sem edição JPA da faixa");
        }
        List<Long> faixas = emTransacao(() -> em.createQuery("select a.id from AlocacaoVinculo a where a.vinculo.id=:id order by a.dataInicio,a.id", Long.class)
                .setParameter("id", vinculo).getResultList());
        assertThat(faixas).hasSize(comSucessoraHistorica ? 2 : 1);
        long alocacao = faixas.get(faixas.size() - 1);
        enviar(c.localA, V + "/contratos/" + n.id() + "/tratar-fim-natural", null, 200,
                "Término natural efetivado pela API, sem mudar diretamente limites no banco");
        F f = new F(c, n, natural, vinculo, pessoa, alocacao, faixas, fimAntigo, fimNovo);
        var encerrado = estado(f, vinculo);
        assertThat(encerrado.get("").path("dataFim").asText()).isEqualTo(fimAntigo.toString());
        Map<String,Object> prorrogacao = aditamento("VIN012-PRORROGACAO-TARDIA-" + n.id(), fimAntigo.minusDays(1));
        prorrogacao.put("novaDataFim", fimNovo);
        emTransacao(() -> {
            try { registrarAditamento(c, n.id(), prorrogacao); return null; }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        assertThat(estado(f, vinculo)).as("Prorrogação tardia não reabre limites/trilhas do vínculo automaticamente").isEqualTo(encerrado);
        return f;
    }

    private Map<String,Object> pedido(F f) {
        return mapa("dataFim", f.fimNovo, "motivoDesligamentoId", f.motivo,
                "justificativa", "Reabertura explícita inteiramente sintética após prorrogação tardia",
                "evidencia", "SEI-SINTETICO-VIN012-REABERTURA", "autorizacao", "ATO-SINTETICO-VIN012-REABERTURA",
                "confirmarImpacto", true, "confirmarInclusaoExcedente", false);
    }
    private JsonNode confirmarPrevia(F f, Map<String,Object> req) throws Exception {
        JsonNode previa = enviar(f.c.localA, V + "/" + f.vinculo + "/previas/retificacao-desligamento", req, 200,
                "VIN012 prévia da própria reabertura natural");
        req.put("tokenPrevia", previa.path("token").asText());
        return previa;
    }
    private Map<String,JsonNode> estado(F f, long vinculo) throws Exception {
        Map<String,JsonNode> resultado = new LinkedHashMap<>();
        for (String recurso : List.of("", "/eventos", "/alocacoes", "/alocacoes/eventos"))
            resultado.put(recurso, consultar(f.c.localA, V + "/" + vinculo + recurso,
                    "VIN012 estado negocial por ID em leitura posterior " + recurso));
        return resultado;
    }
    private void conferirLimites(F f) {
        emTransacao(() -> {
            em.clear();
            VinculoContratual v = em.find(VinculoContratual.class, f.vinculo);
            assertThat(v.getPessoa().getId()).isEqualTo(f.pessoa);
            assertThat(v.getDataFim()).isEqualTo(f.fimNovo);
            assertThat(em.find(AlocacaoVinculo.class, f.alocacao).getDataFim()).isEqualTo(f.fimNovo);
            assertThat(em.createQuery("select a.id from AlocacaoVinculo a where a.vinculo.id=:id", Long.class)
                    .setParameter("id", f.vinculo).getResultList()).containsExactlyInAnyOrderElementsOf(f.faixas);
            if (f.n.composicao2() != null) {
                assertThat(em.find(AlocacaoVinculo.class, f.faixas.get(0)).getDataFim()).isEqualTo(f.fimAntigo.minusDays(3));
                assertThat(em.find(AlocacaoVinculo.class, f.alocacao).getComposicao().getId()).isEqualTo(f.n.composicao2());
            }
            return null;
        });
    }
    private void assertEventosAnterioresPreservados(JsonNode antes, JsonNode depois) {
        var porId = java.util.stream.StreamSupport.stream(depois.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(e -> e.path("id").asLong(), e -> e));
        for (var e : antes) assertThat(porId.get(e.path("id").asLong())).isEqualTo(e);
    }
}
