package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** ALO008: ensaio alternativo de barreira e retomada VÁLIDA, proposto sem execução.
 * Não fecha a contraprova original de composição inválida selecionável na retomada.
 * O contrato, composições e vínculo sob teste são preparados por API. Os dados
 * de apoio de criarCenarioPersistido são fixtures sintéticas declaradas da base.
 * Nenhuma frequência futura, escrita JPA, SQL manual ou mock do domínio. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AlocacaoRetomadaInvariantesApiHttpIT extends ContratosHttpBase {
    private record F(Cenario c, long contrato, long composicaoA, long composicaoB,
                     long pessoa, long vinculo, long faixa, LocalDate hoje, LocalDate fim,
                     LocalDate inativacao, LocalDate reducao, LocalDate retomada) { }

    @Test
    void reservaDurantePausaRecusaZeramentoAceitaReducaoCompativelERetomaMesmaFaixa() throws Exception {
        F f = preparar();
        assertHabilitacao(f, f.hoje, "ATIVO");
        assertPares(f, f.reducao, 2, 6);
        var vinculoOriginal = consultar(f.c.localA, v(f), "ALO68 vínculo original por ID");
        var faixasOriginais = consultar(f.c.localA, v(f) + "/alocacoes", "ALO68 única faixa original");
        assertThat(faixasOriginais).hasSize(1);
        assertIdentidadeEIntervalosPorPk(f);

        var inativar = mapa("dataEfetiva", f.inativacao, "motivo", "Pausa futura inteiramente sintética do ensaio ALO68");
        confirmarCiclo(f, "inativar", inativar);
        assertHabilitacao(f, f.hoje, "ATIVO");
        assertHabilitacao(f, f.inativacao, "INATIVO");
        assertHabilitacao(f, f.reducao, "INATIVO");
        assertThat(consultar(f.c.localA, v(f), "Inativação programada preserva o mesmo vínculo")).isEqualTo(vinculoOriginal);
        assertThat(consultar(f.c.localA, v(f) + "/alocacoes", "Inativação preserva a mesma faixa inteira")).isEqualTo(faixasOriginais);

        var antesDaRecusa = acervo(f);
        var composicoesAntes = AssercoesComposicoesHttp.capturar(() -> consultar(f.c.localA,
                base(f.contrato) + "/composicoes?data=" + f.reducao, "ALO68 composições antes da tentativa de zeramento"));
        Map<String,Object> zerar = aditamento("ALO68-REDUCAO-" + f.contrato, f.reducao);
        zerar.put("justificativa", "Redução inteiramente sintética ALO68");
        zerar.put("referenciaSei", "SEI-SINTETICO-ALO68-REDUCAO");
        zerar.put("novoEfetivoTotal", 6);
        zerar.put("alteracoesComposicao", List.of(mapa("composicaoId", f.composicaoA, "quantidadeResultante", 0)));
        confirmarPreviaAdi(f, zerar);
        JsonNode recusa = enviar(f.c.localA, base(f.contrato) + "/aditamentos", zerar, 409,
                "Barreira legítima: retirar as duas vagas de A deixaria a faixa registrada sem capacidade durante a pausa");
        assertThat(recusa.path("message").asText()).containsIgnoringCase("ocupação");
        assertThat(acervo(f)).as("Recusa não grava instrumento, evento, versão, fonte ou alteração de vínculo/faixa").isEqualTo(antesDaRecusa);
        var composicoesDepois = AssercoesComposicoesHttp.capturar(() -> consultar(f.c.localA,
                base(f.contrato) + "/composicoes?data=" + f.reducao, "ALO68 nova consulta de composições após recusa"));
        AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(composicoesAntes, composicoesDepois);
        assertPares(f, f.reducao, 2, 6);
        assertIdentidadeEIntervalosPorPk(f);

        // Mesma data, ator, contrato, composição e espécie de ato; só a redução
        // pretendida passa de duas para uma vaga. Não se troca a guarda por mock.
        Map<String,Object> reduzirUma = new LinkedHashMap<>(zerar);
        reduzirUma.remove("tokenPrevia");
        reduzirUma.put("novoEfetivoTotal", 7);
        reduzirUma.put("alteracoesComposicao", List.of(mapa("composicaoId", f.composicaoA, "quantidadeResultante", 1)));
        confirmarPreviaAdi(f, reduzirUma);
        JsonNode adi = enviar(f.c.localA, base(f.contrato) + "/aditamentos", reduzirUma, 201,
                "Controle positivo: redução compatível de A2 para A1 é permitida durante pausa programada");
        assertThat(adi.path("id").asLong()).isPositive();
        assertPares(f, f.reducao.minusDays(1), 2, 6);
        assertPares(f, f.reducao, 1, 6);
        assertHabilitacao(f, f.hoje, "ATIVO");
        assertHabilitacao(f, f.inativacao, "INATIVO");
        assertHabilitacao(f, f.reducao, "INATIVO");
        assertThat(consultar(f.c.localA, v(f), "ADI não retifica vínculo nem sua data final")).isEqualTo(vinculoOriginal);
        assertThat(consultar(f.c.localA, v(f) + "/alocacoes", "ADI compatível não cria ou corta a faixa")).isEqualTo(faixasOriginais);

        var trilhaVinculo = consultar(f.c.localA, v(f) + "/eventos", "Fonte do vínculo antes da retomada válida");
        var trilhaAlocacao = consultar(f.c.localA, v(f) + "/alocacoes/eventos", "Fonte da faixa antes da retomada válida");
        var reativar = mapa("dataEfetiva", f.retomada, "motivo", "Retomada futura sintética com A1 suficiente para a mesma Pessoa");
        confirmarCiclo(f, "reativar", reativar);
        assertHabilitacao(f, f.hoje, "ATIVO");
        assertHabilitacao(f, f.inativacao, "INATIVO");
        assertHabilitacao(f, f.reducao, "INATIVO");
        assertHabilitacao(f, f.retomada.minusDays(1), "INATIVO");
        assertHabilitacao(f, f.retomada, "ATIVO");
        assertPares(f, f.retomada, 1, 6);
        assertThat(consultar(f.c.localA, v(f), "Retomada não cadastra nem retifica outro vínculo")).isEqualTo(vinculoOriginal);
        assertThat(consultar(f.c.localA, v(f) + "/alocacoes", "Retomada mantém exatamente a mesma faixa e composição")).isEqualTo(faixasOriginais);
        assertThat(consultar(f.c.localA, v(f) + "/eventos", "Retomada administrativa não inventa evento de vínculo")).isEqualTo(trilhaVinculo);
        assertThat(consultar(f.c.localA, v(f) + "/alocacoes/eventos", "Retomada administrativa não inventa evento de faixa")).isEqualTo(trilhaAlocacao);
        for (LocalDate data : List.of(f.inativacao, f.reducao, f.retomada)) {
            JsonNode p = consultar(f.c.localA, v(f) + "/alocacoes/posicao?data=" + data,
                    "Mesma identidade de alocação preservada no corte " + data);
            assertThat(p.path("alocacaoId").asLong()).isEqualTo(f.faixa);
            assertThat(p.path("vinculoId").asLong()).isEqualTo(f.vinculo);
            assertThat(p.path("composicaoBaseId").asLong()).isEqualTo(f.composicaoA);
            // conhecidoAte pertence à consulta. Não é comparado como fato persistido.
        }
        assertIdentidadeEIntervalosPorPk(f);
        JsonNode listaVinculos = consultar(f.c.localA, "/api/vinculos-contratuais?pessoaId=" + f.pessoa,
                "ALO68 apenas o vínculo original dessa Pessoa sintética");
        assertThat(listaVinculos.path("content")).hasSize(1);
        assertThat(listaVinculos.path("content").get(0).path("id").asLong()).isEqualTo(f.vinculo);
    }

    private F preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate hoje = RelogioSistema.hoje(), fim = hoje.plusDays(30);
        LocalDate i = hoje.plusDays(2), d = hoje.plusDays(4), r = hoje.plusDays(6);
        long cargoB = enviar(c.localA, "/api/cargos", mapa("nome", "Cargo B sintético ALO68 " + UUID.randomUUID(),
                "cbo", "0000-00", "ativo", true, "gestaoLocalId", c.gestaoA.getId()), 200,
                "Catálogo B criado por API no mesmo escopo, com origem de disponibilidade atual").path("id").asLong();
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c, "ALO68-" + UUID.randomUUID().toString().substring(0, 8),
                8, hoje, fim), 201, "Rascunho sintético: total8 e vigência cobre todo o ensaio").path("id").asLong();
        long a = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 2), 201,
                "Composição A com duas vagas, criada por API").path("id").asLong();
        long b = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, cargoB, 6), 201,
                "Composição B com seis vagas, criada por API").path("id").asLong();
        enviar(c.localA, base(contrato) + "/publicar", mapa("dataPublicacao", hoje,
                "referenciaSei", "SEI-SINTETICO-ALO68-ORIGINAL", "justificativa", "Publicação sintética de A2 e B6"), 200,
                "Publicação legítima: composições2+6 e total8");
        JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa sintética ALO68",
                "cpf", cpf(Math.toIntExact(843000000L + contrato))), "contratoId", contrato, "composicaoId", a,
                "dataInicio", hoje, "dataFim", fim), 201, "Único vínculo sintético em A cobrindo I, D e R");
        long v = vinculo.path("id").asLong(), pessoa = vinculo.path("pessoaId").asLong();
        JsonNode faixas = consultar(c.localA, "/api/vinculos-contratuais/" + v + "/alocacoes", "Origem API da única faixa");
        assertThat(faixas).hasSize(1);
        assertThat(faixas.get(0).path("composicaoBaseId").asLong()).isEqualTo(a);
        return new F(c, contrato, a, b, pessoa, v, faixas.get(0).path("id").asLong(), hoje, fim, i, d, r);
    }

    private void confirmarCiclo(F f, String operacao, Map<String,Object> req) throws Exception {
        JsonNode previa = enviar(f.c.localA, base(f.contrato) + "/previas/" + operacao, req, 200, "Prévia API de " + operacao);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(f.c.localA, base(f.contrato) + "/" + operacao, req, 200, "Confirmação API de " + operacao);
    }
    private void confirmarPreviaAdi(F f, Map<String,Object> req) throws Exception {
        JsonNode previa = enviar(f.c.localA, base(f.contrato) + "/aditamentos/previa", req, 200, "Prévia real da redução em D");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        assertHabilitacao(f, f.hoje, "ATIVO");
    }
    private void assertHabilitacao(F f, LocalDate data, String habilitacao) throws Exception {
        JsonNode p = consultar(f.c.localA, base(f.contrato) + "/posicao-temporal?data=" + data, "ALO68 posição administrativa em " + data);
        assertThat(p.path("contratoId").asLong()).isEqualTo(f.contrato);
        assertThat(p.path("habilitacaoAdministrativa").asText()).isEqualTo(habilitacao);
        assertThat(p.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
    }
    private void assertPares(F f, LocalDate data, int a, int b) throws Exception {
        JsonNode comps = consultar(f.c.localA, base(f.contrato) + "/composicoes?data=" + data, "ALO68 todos os pares por ID em " + data);
        assertThat(comps).hasSize(2);
        assertThat(java.util.stream.StreamSupport.stream(comps.spliterator(), false).collect(java.util.stream.Collectors.toMap(
                item -> item.path("id").asLong(), item -> item.path("efetivoPrevisto").asInt())))
                .isEqualTo(Map.of(f.composicaoA, a, f.composicaoB, b));
    }
    private Map<String,JsonNode> acervo(F f) throws Exception {
        Map<String,JsonNode> m = new LinkedHashMap<>();
        for (String recurso : List.of("", "/eventos", "/eventos/precedencias", "/versoes", "/aditamentos", "/documentos"))
            m.put("contrato" + recurso, consultar(f.c.localA, base(f.contrato) + recurso, "ALO68 acervo confirmado " + recurso));
        for (String recurso : List.of("", "/eventos", "/alocacoes", "/alocacoes/eventos"))
            m.put("vinculo" + recurso, consultar(f.c.localA, v(f) + recurso, "ALO68 acervo do vínculo " + recurso));
        return m;
    }
    private void assertIdentidadeEIntervalosPorPk(F f) {
        emTransacao(() -> {
            em.clear();
            Pessoa pessoa = em.find(Pessoa.class, f.pessoa);
            VinculoContratual v = em.find(VinculoContratual.class, f.vinculo);
            AlocacaoVinculo a = em.find(AlocacaoVinculo.class, f.faixa);
            ComposicaoContratual comp = em.find(ComposicaoContratual.class, f.composicaoA);
            assertThat(pessoa).isNotNull(); assertThat(v.getPessoa().getId()).isEqualTo(pessoa.getId());
            assertThat(v.getContrato().getId()).isEqualTo(f.contrato);
            assertThat(a.getVinculo().getId()).isEqualTo(f.vinculo);
            assertThat(a.getComposicao().getId()).isEqualTo(comp.getId());
            assertThat(comp.getContrato().getId()).isEqualTo(f.contrato);
            assertThat(v.getDataInicio()).isEqualTo(f.hoje); assertThat(v.getDataFim()).isEqualTo(f.fim);
            assertThat(a.getDataInicio()).isAfterOrEqualTo(v.getDataInicio());
            assertThat(a.getDataFim()).isNotNull().isBeforeOrEqualTo(v.getDataFim());
            assertThat(a.getDataInicio()).isBeforeOrEqualTo(f.inativacao);
            assertThat(a.getDataFim()).isAfterOrEqualTo(f.retomada);
            return null;
        });
    }
    private String v(F f) { return "/api/vinculos-contratuais/" + f.vinculo; }
}
