package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RED complementar: origem legada sem evento e dependência programada da política de matrícula. */
class ContratosLegadoPoliticaComplementarHttpIT extends ContratosHttpBase {
    @Test
    void primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem() throws Exception {
        var c = criarCenario(); String descricaoOriginal = c.contratoA.getDescricao();
        var req = aditamento("ADI-PRIMEIRO-LEGADO", RelogioSistema.hoje());
        req.put("novaDescricao", "Descrição posterior documentada");
        registrarAditamento(c, c.contratoA.getId(), req);

        var antes = consultar(c.localA, base(c.contratoA.getId()) + "/posicao-temporal?data=" + RelogioSistema.hoje().minusDays(1),
                "CON008/HIS001 publicação legada continua sendo a base original");
        assertThat(antes.path("versaoContratual").path("descricao").asText()).isEqualTo(descricaoOriginal);
    }

    @Test
    void aditamentoNaoPodeTornarMatriculaObrigatoriaComVinculoProgramadoSemMatricula() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Programação ainda sem matrícula",
                        "cpf", cpf(850000000 + c.pessoaA.getId().intValue())), "contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", RelogioSistema.hoje().plusDays(10)), 201,
                "MAT005 programação válida enquanto matrícula é opcional");
        var req = aditamento("ADI-EXIGENCIA-FUTURA", RelogioSistema.hoje().plusDays(5));
        req.put("novaDescricao", "Exigência documentada posterior"); req.put("novaMatriculaVinculoObrigatoria", true);
        previaAditamento(c, n.id(), req);

        enviar(c.localA, base(n.id()) + "/aditamentos", req, 409,
                "MAT005 programação sem matrícula deve ser tratada antes da nova exigência anterior à ativação");
    }
}
