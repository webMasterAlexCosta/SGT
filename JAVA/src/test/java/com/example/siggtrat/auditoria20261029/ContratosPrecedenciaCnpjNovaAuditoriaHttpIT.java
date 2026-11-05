package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** EMP-003 + TMP-010/016: o campo atingido determina conflito, não a família da rota. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContratosPrecedenciaCnpjNovaAuditoriaHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "segundo fato de CNPJ exige precedência; correção primeiro={0}")
    @ValueSource(booleans = {false, true})
    void cnpjEmAditamentoECorrecaoNoMesmoDiaNaoPodeSerDecididoPelaChaveTecnica(boolean correcaoPrimeiro)
            throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var dia = RelogioSistema.hoje().plusDays(2);
        var adi = aditamento("EMPRESA-SUCESSORA-SEM-PRECEDENCIA", dia);
        adi.put("novoCnpjEmpresa", "98765432000198");
        adi.put("novaRazaoSocialEmpresa", "Empresa sucessora documentada");
        adi.put("novoNomeFantasiaEmpresa", "Sucessora");
        var corrigir = mapa("cnpjCorrigido", "11222333000181", "dataEfetiva", dia,
                "dataAto", RelogioSistema.hoje(), "motivo", "Correção material do CNPJ documentada para a mesma data",
                "referenciaSei", "SEI-CNPJ-MESMO-DIA", "chaveIdempotencia", UUID.randomUUID().toString());

        if (correcaoPrimeiro) {
            enviar(c.localA, base(n.id()) + "/correcoes-cnpj", corrigir, 201,
                    "Primeiro fato de correção material é documentalmente válido");
        } else {
            emTransacao(() -> {
                try { return registrarAditamento(c, n.id(), adi); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
        }
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        JsonNode posicaoAntes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Somente o primeiro fato é conhecido na data").path("versaoContratual");

        if (correcaoPrimeiro) {
            // A prévia informa dados; o comando deve recusar a colisão sem a relação negocial.
            emTransacao(() -> {
                try { return previaAditamento(c, n.id(), adi); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
            enviar(c.localA, base(n.id()) + "/aditamentos", adi, 409,
                    "TMP-010: ADI e correção atingem CNPJ no mesmo dia e exigem precedência explícita");
        } else {
            enviar(c.localA, base(n.id()) + "/correcoes-cnpj", corrigir, 409,
                    "TMP-010: correção e ADI atingem CNPJ no mesmo dia e exigem precedência explícita");
        }

        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Recusa não substitui o CNPJ do primeiro fato").path("versaoContratual").path("cnpjEmpresa"))
                .isEqualTo(posicaoAntes.path("cnpjEmpresa"));
    }

    private long contar(long contrato, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contrato).getSingleResult());
    }

    @ParameterizedTest(name = "SUCEDE explícito ordena CNPJ; correção primeiro={0}")
    @ValueSource(booleans = {false, true})
    void sucessaoExplicitaEntreAditamentoECorrecaoMantemAmbosEAplicaSucessor(boolean correcaoPrimeiro)
            throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var dia = RelogioSistema.hoje().plusDays(2);
        var adi = aditamento("EMPRESA-SUCESSAO-EXPLICITA", dia);
        adi.put("novoCnpjEmpresa", "98765432000198");
        adi.put("novaRazaoSocialEmpresa", "Empresa sucessora documentada");
        adi.put("novoNomeFantasiaEmpresa", "Sucessora");
        var correcao = mapa("cnpjCorrigido", "11222333000181", "dataEfetiva", dia,
                "dataAto", RelogioSistema.hoje(), "motivo", "Correção material ordenada por ato expresso",
                "referenciaSei", "SEI-CNPJ-SUCEDE", "chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode primeiro;
        JsonNode segundo;
        if (correcaoPrimeiro) {
            primeiro = enviar(c.localA, base(n.id()) + "/correcoes-cnpj", correcao, 201, "Correção precedente");
            adi.put("eventoPrecedenteId", primeiro.path("eventoId").asLong());
            adi.put("relacaoPrecedencia", "SUCEDE");
            segundo = emTransacao(() -> {
                try { return registrarAditamento(c, n.id(), adi); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
        } else {
            primeiro = emTransacao(() -> {
                try { return registrarAditamento(c, n.id(), adi); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
            correcao.put("eventoPrecedenteId", primeiro.path("eventoId").asLong());
            correcao.put("relacaoPrecedencia", "SUCEDE");
            segundo = enviar(c.localA, base(n.id()) + "/correcoes-cnpj", correcao, 201, "Correção sucessora explícita");
        }
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "SUCEDE faz prevalecer exatamente o fato posterior documentado");
        assertThat(posicao.path("versaoContratual").path("cnpjEmpresa").asText())
                .isEqualTo(correcaoPrimeiro ? "98765432000198" : "11222333000181");
        assertThat(posicao.path("eventosAplicados").findValues("id")).extracting(JsonNode::asLong)
                .contains(primeiro.path("eventoId").asLong(), segundo.path("eventoId").asLong());
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(3);
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias", "Relação explícita preservada");
        assertThat(relacoes.size()).isEqualTo(1);
        assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("SUCEDE");
        assertThat(relacoes.get(0).path("eventoFonteId").asLong()).isEqualTo(segundo.path("eventoId").asLong());
        assertThat(relacoes.get(0).path("eventoAlvoId").asLong()).isEqualTo(primeiro.path("eventoId").asLong());
    }
}
