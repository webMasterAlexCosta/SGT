package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** §5.21 / CON018/024: ato conhecido hoje pode declarar alcance em outra data. */
class ContratoAnulacaoFuturaHttpIT extends ContratosHttpBase {
    @Test
    void alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false); motivo(c);
        LocalDate alcance = RelogioSistema.hoje().plusDays(5);
        var adi = aditamento("ADI-PRESERVADO-ANULACAO", alcance.plusDays(1));
        adi.put("novaDescricao", "Fato posterior preservado para auditoria"); registrarAditamento(c, n.id(), adi);
        anular(c, n, alcance);

        var detalhe = consultar(c.localA, base(n.id()), "CON024 estado corrente respeita alcance futuro documentado");
        assertThat(detalhe.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(detalhe.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        var antes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + alcance.minusDays(1), "CON024 validade antes do alcance");
        var depois = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + alcance, "CON024 invalidade desde o alcance");
        assertThat(antes.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(depois.path("estadoNegocial").asText()).isEqualTo("ANULADO");
        var aditamentos = consultar(c.localA, base(n.id()) + "/aditamentos", "GOV003 anulação não apaga ato posterior");
        assertThat(aditamentos).hasSize(1);
        assertThat(aditamentos.get(0).path("identificacao").asText()).isEqualTo("ADI-PRESERVADO-ANULACAO");
    }

    @Test
    void anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false); motivo(c);
        LocalDate alcance = RelogioSistema.hoje().plusDays(5); anular(c, n, alcance);
        enviar(c.localA, "/api/vinculos-contratuais", cadastro(c, n, 860000000, RelogioSistema.hoje(), alcance.minusDays(1)), 201,
                "CON018/024 período ainda válido não pode ser bloqueado pela data de digitação do ato");
        enviar(c.localA, "/api/vinculos-contratuais", cadastro(c, n, 870000000, alcance, null), 409,
                "CON018 nenhum novo vínculo operacional pode iniciar no alcance da anulação");
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(alcance);
            assertThat(consultar(c.localA, base(n.id()), "CON024 sem rotina técnica estado passa a ANULADO na data")
                    .path("estadoNegocial").asText()).isEqualTo("ANULADO");
        }
    }

    @Test
    void correcaoDeAnulacaoFuturaMantemEfeitoAdministrativoNaDataDeclarada() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false); motivo(c);
        LocalDate alcance = RelogioSistema.hoje().plusDays(5); anular(c, n, alcance);
        long evento = 0;
        for (var e : consultar(c.localA, base(n.id()) + "/eventos", "CON020 anulação original preservada"))
            if (e.path("tipoEvento").asText().equals("ANULACAO")) evento = e.path("id").asLong();
        acao(c, n.id(), "corrigir-anulacao", mapa("dataEfetiva", alcance, "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção documental antes do início do alcance", "referenciaSei", "SEI-CORRECAO-ALCANCE-FUTURO",
                "eventoPrecedenteId", evento, "relacaoPrecedencia", "CANCELA"));

        var hoje = consultar(c.localA, base(n.id()), "CON020/024 correção não antecipa bloqueio futuro");
        assertThat(hoje.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(hoje.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        var noAlcance = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + alcance,
                "CON020 depois do alcance corrigido permanece necessária reativação explícita");
        assertThat(noAlcance.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(noAlcance.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
    }

    @Test
    void posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false); motivo(c);
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", cadastro(c, n, 880000000, n.inicio(), null), 201,
                "VIN001 vínculo válido antes do ato anulatório");
        LocalDate alcance = RelogioSistema.hoje().plusDays(5); anular(c, n, alcance);
        String posicao = "/api/vinculos-contratuais/pessoas/" + vinculo.path("pessoaId").asLong() + "/posicao?data=";
        var antes = consultar(c.localA, posicao + alcance.minusDays(1), "CON018 faixa anterior permanece válida").path("vinculos").get(0);
        var depois = consultar(c.localA, posicao + alcance, "CON018 posição marca fato atingido pela anulação").path("vinculos").get(0);
        assertThat(antes.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(depois.path("situacaoTemporal").asText()).isEqualTo("ANULADO");
        assertThat(depois.path("vinculoId").asLong()).isEqualTo(vinculo.path("id").asLong());
    }

    private void anular(Cenario c, NovoContrato n, LocalDate alcance) throws Exception {
        acao(c, n.id(), "anular", mapa("dataEfetiva", alcance, "dataAto", RelogioSistema.hoje(),
                "motivo", "Ato declara invalidade somente a partir da data expressa", "referenciaSei", "SEI-ALCANCE-FUTURO"));
    }

    private JsonNode acao(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "Prévia do alcance documental " + operacao);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(c.localA, base(id) + "/" + operacao, req, 200, "Ato documental " + operacao);
    }

    private Map<String, Object> cadastro(Cenario c, NovoContrato n, int seed, LocalDate inicio, LocalDate fim) {
        return mapa("pessoa", mapa("nome", "Pessoa do alcance " + seed, "cpf", cpf(seed + c.pessoaA.getId().intValue())),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", inicio, "dataFim", fim);
    }

    private void motivo(Cenario c) {
        var m = new MotivoDesligamento(); m.setGestaoLocal(c.gestaoA); m.setDescricao("Anulação de alcance documentado");
        m.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO); em.persist(m); em.flush();
    }
}
