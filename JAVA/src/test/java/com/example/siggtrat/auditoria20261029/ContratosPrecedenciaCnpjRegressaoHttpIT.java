package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TMP-010/016: conflito por campo e sequência completa entre famílias de fatos. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContratosPrecedenciaCnpjRegressaoHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "descrição e correção CNPJ coexistem sem colisão; correção primeiro={0}")
    @ValueSource(booleans = {false, true})
    void descricaoSemCnpjPodeCoexistirComCorrecaoMaterialNoMesmoDia(boolean correcaoPrimeiro) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var dia = RelogioSistema.hoje().plusDays(2);
        var adi = aditamento("DESCRICAO-SEM-CONFLITO-CNPJ", dia);
        adi.put("novaDescricao", "Descrição independente do CNPJ");
        if (correcaoPrimeiro) enviar(c.localA, base(n.id()) + "/correcoes-cnpj",
                correcao(dia, "11222333000181"), 201, "Primeiro fato de CNPJ documentado");
        registrarEmTransacao(c, n.id(), adi);
        if (!correcaoPrimeiro) enviar(c.localA, base(n.id()) + "/correcoes-cnpj",
                correcao(dia, "11222333000181"), 201, "Campo CNPJ independe de descrição no mesmo dia");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Ambos os campos independentes permanecem comprovados").path("versaoContratual");
        assertThat(posicao.path("descricao").asText()).isEqualTo("Descrição independente do CNPJ");
        assertThat(posicao.path("cnpjEmpresa").asText()).isEqualTo("11222333000181");
        assertThat(consultar(c.localA, base(n.id()) + "/eventos/precedencias", "Campos independentes dispensam relação"))
                .isEmpty();
    }

    @Test
    void cadeiaMistaDeCnpjExigeOrdemCompletaEPreservaTodosOsFatos() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var dia = RelogioSistema.hoje().plusDays(2);
        var a = enviar(c.localA, base(n.id()) + "/correcoes-cnpj", correcao(dia, "11222333000181"),
                201, "Primeira correção material documentada");
        var adi = aditamento("CNPJ-INTERMEDIARIO-DA-CADEIA", dia);
        adi.put("novoCnpjEmpresa", "98765432000198");
        adi.put("novaRazaoSocialEmpresa", "Sucessora intermediária documentada");
        adi.put("novoNomeFantasiaEmpresa", "Intermediária");
        adi.put("eventoPrecedenteId", a.path("eventoId").asLong());
        adi.put("relacaoPrecedencia", "SUCEDE");
        var b = registrarEmTransacao(c, n.id(), adi);
        var terceira = correcao(dia, "12345678000190");
        terceira.put("eventoPrecedenteId", a.path("eventoId").asLong());
        terceira.put("relacaoPrecedencia", "SUCEDE");
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        enviar(c.localA, base(n.id()) + "/correcoes-cnpj", terceira, 409,
                "A→B e A→C não ordenam os CNPJs conflitantes de B e C");
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Recusa conserva o último CNPJ ordenado").path("versaoContratual").path("cnpjEmpresa").asText())
                .isEqualTo("98765432000198");
        terceira.put("eventoPrecedenteId", b.path("eventoId").asLong());
        var sucessora = enviar(c.localA, base(n.id()) + "/correcoes-cnpj", terceira, 201,
                "A→B→C estabelece precedência completa mesmo entre chaves técnicas distintas");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Todos os fatos permanecem auditáveis e o CNPJ de C prevalece");
        assertThat(posicao.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(posicao.path("eventosAplicados").findValues("id")).extracting(JsonNode::asLong)
                .contains(a.path("eventoId").asLong(), b.path("eventoId").asLong(), sucessora.path("eventoId").asLong());
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes + 1);
    }

    private Map<String, Object> correcao(LocalDate dia, String cnpj) {
        return mapa("cnpjCorrigido", cnpj, "dataEfetiva", dia, "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção material documentada e auditada", "referenciaSei", "SEI-CNPJ-REGRESSAO",
                "chaveIdempotencia", UUID.randomUUID().toString());
    }

    private JsonNode registrarEmTransacao(Cenario c, long contrato, Map<String, Object> requisicao) {
        return emTransacao(() -> {
            try { return registrarAditamento(c, contrato, requisicao); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private long contar(long contrato, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contrato).getSingleResult());
    }
}
