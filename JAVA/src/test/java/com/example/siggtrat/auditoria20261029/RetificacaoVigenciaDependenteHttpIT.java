package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RED proposto pela revisão independente; ainda não executado nesta etapa. */
class RetificacaoVigenciaDependenteHttpIT extends ContratosHttpBase {
    @Test
    void retificarProrrogacaoParaSomenteDescricaoNaoPodeRetirarVigenciaDeVinculoProgramado() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        var efeito = RelogioSistema.hoje().plusDays(5);
        var req = aditamento("PRORROGACAO-A-RETIFICAR", efeito);
        req.put("novaDataFim", n.fim().plusDays(30));
        var instrumento = registrarAditamento(c, n.id(), req);
        var inicioPessoa = n.fim().plusDays(1);
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", inicioPessoa,
                "pessoa", mapa("nome", "Pessoa dependente da extensão retificada", "cpf", "52998224725")), 201,
                "TMP-014 vínculo programado com cobertura válida pela prorrogação");
        em.flush();
        em.clear();
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "ADI-007 fatos antes da retificação incompatível");

        var retificar = aditamento("PRORROGACAO-A-RETIFICAR", efeito);
        retificar.put("novaDescricao", "Nova versão sem alteração de vigência");
        retificar.put("eventoPrecedenteId", instrumento.path("eventoId").asLong());
        retificar.put("relacaoPrecedencia", "RETIFICA");
        var previa = previaAditamento(c, n.id(), retificar);
        assertThat(previa.path("alertas").toString()).contains("cobertura", String.valueOf(vinculo.path("id").asLong()));
        enviar(c.localA, base(n.id()) + "/aditamentos/" + instrumento.path("id").asLong() + "/retificar", retificar,
                409, "TMP-014/ADI-008 retificação não pode tornar vínculo futuro órfão ao retirar prorrogação");
        em.flush();
        em.clear();

        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "ADI-007 recusa preserva cadeia original")).isEqualTo(eventosAntes);
        var instrumentos = consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-007 recusa não cria versão parcial");
        assertThat(instrumentos).hasSize(1);
        assertThat(instrumentos.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + inicioPessoa,
                "TMP-014 programação conserva cobertura contratual no dia de início");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo.path("id").asLong(),
                "CMP-006 vínculo futuro não é cancelado ou deslocado silenciosamente")
                .path("estadoNegocial").asText()).isEqualTo("REGISTRADO");

        retificar.put("novaDataFim", n.fim().plusDays(30));
        previaAditamento(c, n.id(), retificar);
        enviar(c.localA, base(n.id()) + "/aditamentos/" + instrumento.path("id").asLong() + "/retificar", retificar,
                200, "ADI-007 retificação que preserva a cobertura dependente continua permitida");
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-007 original e retificação permanecem auditáveis"))
                .hasSize(2);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + inicioPessoa,
                "TMP-014 retificação válida conserva cobertura no início programado")
                .path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
    }
}
