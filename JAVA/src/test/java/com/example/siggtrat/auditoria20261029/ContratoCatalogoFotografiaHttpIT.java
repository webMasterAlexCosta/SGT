package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ContratoCatalogoFotografiaHttpIT extends ContratosHttpBase {
    @Test
    void renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var cadastro = mapa("descricao", "Tipo B no documento", "ativo", true, "matriculaVinculoObrigatoria", false,
                "gestaoLocalId", c.gestaoA.getId());
        long tipoId = enviar(c.localA, "/api/tipos-contratacao", cadastro, 201, "CAT-004 tipo de destino explícito").path("id").asLong();
        var req = aditamento("TROCA-TIPO-FOTOGRAFIA", RelogioSistema.hoje()); req.put("novoTipoContratacaoId", tipoId);
        var adi = registrarAditamento(c, n.id(), req);
        assertThat(adi.path("novoTipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento");
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "HIS-004 fotografia do tipo no momento do instrumento").path("versaoContratual").path("tipoContratacaoDescricao").asText())
                .isEqualTo("Tipo B no documento");
        Instant corte = Instant.now();
        cadastro.put("descricao", "Tipo C após atualização do catálogo");
        executar(c.localA, put("/api/tipos-contratacao/" + tipoId).contentType(MediaType.APPLICATION_JSON).content(json(cadastro)),
                200, "CAT-004 edição permitida do nome atual do catálogo");
        em.flush(); em.clear();
        var conhecidoAntes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje()
                + "&conhecidoAte=" + corte, "HIS-004 corte anterior à renomeação preserva a fotografia publicada");
        var conhecidoHoje = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CAT-004 fotografia publicada permanece após catálogo ser renomeado");
        var instrumentos = consultar(c.localA, base(n.id()) + "/aditamentos", "HIS-004 instrumento guarda nome presente no ato");
        assertAll(
                () -> assertThat(conhecidoAntes.path("versaoContratual").path("tipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento"),
                () -> assertThat(conhecidoHoje.path("versaoContratual").path("tipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento"),
                () -> assertThat(instrumentos.get(0).path("novoTipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento")
        );
    }
}
