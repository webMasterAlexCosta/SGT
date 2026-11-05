package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * TMP-004/005/008/012: limite herdado de composição introduzida por ADI é histórico.
 * HTTP/Spring/JPA/MySQL reais. O único dado alterado diretamente no primeiro caso
 * é a projeção materializada atual, para demonstrar que ela não é fonte do replay.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Ind08VigenciaComposicaoDKHttpIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicoesTemporais;

    private record Origem(Cenario c, NovoContrato contrato, long versaoOriginal, long cargoNovo) { }
    private record NovaComposicao(Origem origem, LocalDate data, Instant conhecido, long evento) { }

    @Test
    void alteracaoDaProjecaoAtualNaoMudaValorOrigemOuLimiteDoReplayNoMesmoDK() throws Exception {
        Origem o = origem();
        NovaComposicao nova = introduzir(o, o.contrato().inicio().plusDays(3));
        JsonNode antes = composicao(nova, nova.conhecido());
        LocalDate fimAtualIndevido = o.contrato().fim().minusDays(45);
        emTransacao(() -> {
            Contrato atual = em.find(Contrato.class, o.contrato().id());
            atual.setDataFim(fimAtualIndevido);
            em.flush();
            return null;
        });

        JsonNode depois = composicao(nova, nova.conhecido());
        JsonNode posicao = posicao(o, nova.data(), nova.conhecido());
        LocalDate limiteConsumidoPeloCalculo = limiteInterno(nova, nova.conhecido());
        LocalDate fimOriginalPersistido = emTransacao(() ->
                em.find(VersaoContratoNegocial.class, o.versaoOriginal()).getDataFim());

        assertAll("O mesmo D/K não pode absorver o fim da projeção atual",
                () -> assertThat(antes.path("dataFimVigencia").asText()).isEqualTo(o.contrato().fim().toString()),
                () -> assertThat(depois.path("dataFimVigencia").asText()).isEqualTo(antes.path("dataFimVigencia").asText()),
                () -> assertThat(limiteConsumidoPeloCalculo).isEqualTo(o.contrato().fim()),
                () -> assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(o.contrato().fim().toString()),
                () -> assertThat(depois.path("id").isNull()).isTrue(),
                () -> assertThat(depois.path("efetivoPrevisto").asInt()).isEqualTo(2),
                () -> assertThat(fimOriginalPersistido).isEqualTo(o.contrato().fim()),
                () -> fonte(depois, "versaoId", o.versaoOriginal()));
    }

    @Test
    void prorrogacaoAnteriorRecebidaDepoisAlteraSomenteConhecimentoPosteriorEConservaFonteReal() throws Exception {
        Origem o = origem();
        NovaComposicao nova = introduzir(o, o.contrato().inicio().plusDays(3));
        Instant kAnterior = nova.conhecido();
        LocalDate fimProrrogado = o.contrato().fim().plusDays(30);
        Map<String,Object> req = aditamento("IND08-PRORROGA-ANTERIOR-" + o.contrato().id(),
                o.contrato().inicio().plusDays(2));
        req.put("novaDataFim", fimProrrogado);
        JsonNode prorrogacao = registrar(o, req);
        Instant kPosterior = Instant.now();

        JsonNode antigo = composicao(nova, kAnterior);
        JsonNode novo = composicao(nova, kPosterior);
        JsonNode posicaoAntes = posicao(o, nova.data(), kAnterior);
        JsonNode posicaoDepois = posicao(o, nova.data(), kPosterior);
        LocalDate limiteAntes = limiteInterno(nova, kAnterior);
        LocalDate limiteDepois = limiteInterno(nova, kPosterior);
        long eventoProrrogacao = prorrogacao.path("eventoId").asLong();

        assertAll("A prova de prorrogação segue sua data efetiva e o corte de conhecimento",
                () -> assertThat(antigo.path("dataFimVigencia").asText()).isEqualTo(o.contrato().fim().toString()),
                () -> assertThat(novo.path("dataFimVigencia").asText()).isEqualTo(fimProrrogado.toString()),
                () -> assertThat(limiteAntes).isEqualTo(o.contrato().fim()),
                () -> assertThat(limiteDepois).isEqualTo(fimProrrogado),
                () -> assertThat(posicaoAntes.path("vigenciaFim").asText()).isEqualTo(o.contrato().fim().toString()),
                () -> assertThat(posicaoDepois.path("vigenciaFim").asText()).isEqualTo(fimProrrogado.toString()),
                () -> fonte(antigo, "versaoId", o.versaoOriginal()),
                () -> fonte(novo, "eventoId", eventoProrrogacao));

        JsonNode antesDaCombinacao = lista(o, nova.data().minusDays(1), kPosterior);
        for (JsonNode item : antesDaCombinacao)
            assertThat(item.path("cargoId").asLong()).isNotEqualTo(o.cargoNovo());
        assertThat(emTransacao(() -> em.find(ComposicaoContratual.class, o.contrato().composicao1())
                .getDataFimVigencia())).isEqualTo(o.contrato().fim());
    }

    private Origem origem() {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        return emTransacao(() -> {
            long versao = motor.fotografiaOriginalConhecida(em.find(Contrato.class, n.id()), Instant.now())
                    .orElseThrow().getId();
            Cargo cargo = new Cargo("Cargo sintético vigência IND08 " + n.id(), "1");
            cargo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.CARGO);
            disponibilidade.setCatalogoId(cargo.getId()); disponibilidade.setDataEfetiva(n.inicio());
            disponibilidade.setAtivo(true); disponibilidade.setMotivo("Fonte sintética original explícita IND08");
            em.persist(disponibilidade); em.flush();
            return new Origem(c, n, versao, cargo.getId());
        });
    }

    private NovaComposicao introduzir(Origem o, LocalDate data) throws Exception {
        Map<String,Object> req = aditamento("IND08-NOVA-COMBINACAO-" + o.contrato().id(), data);
        req.put("novoEfetivoTotal", 12);
        req.put("alteracoesComposicao", List.of(mapa("lotacaoId", o.c().lotacaoA.getId(),
                "jornadaId", o.c().jornadaA.getId(), "cargoId", o.cargoNovo(), "quantidadeResultante", 2)));
        JsonNode adi = registrar(o, req);
        return new NovaComposicao(o, data, Instant.now(), adi.path("eventoId").asLong());
    }

    private JsonNode registrar(Origem o, Map<String,Object> req) throws Exception {
        String rota = base(o.contrato().id()) + "/aditamentos";
        JsonNode previa = enviar(o.c().localA, rota + "/previa", req, 200, "Prévia histórica por fonte canônica");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(o.c().localA, rota, req, 201, "Registro real conserva D e K distintos");
    }

    private JsonNode lista(Origem o, LocalDate data, Instant conhecido) throws Exception {
        return dados(executar(o.c().localA, get(base(o.contrato().id()) + "/composicoes")
                .param("data", data.toString()).param("conhecidoAte", conhecido.toString()), 200,
                "Composições no recorte fixo D/K"));
    }

    private JsonNode composicao(NovaComposicao nova, Instant conhecido) throws Exception {
        for (JsonNode item : lista(nova.origem(), nova.data(), conhecido))
            if (item.path("cargoId").asLong() == nova.origem().cargoNovo()) return item;
        throw new AssertionError("Combinação introduzida por ADI ausente no recorte");
    }

    private JsonNode posicao(Origem o, LocalDate data, Instant conhecido) throws Exception {
        return dados(executar(o.c().localA, get(base(o.contrato().id()) + "/posicao-temporal")
                .param("data", data.toString()).param("conhecidoAte", conhecido.toString()), 200,
                "Posição contratual usa as mesmas fontes históricas"));
    }

    private LocalDate limiteInterno(NovaComposicao nova, Instant conhecido) {
        return emTransacao(() -> composicoesTemporais.projetarEntidades(
                        em.find(Contrato.class, nova.origem().contrato().id()), nova.data(), conhecido).stream()
                .filter(c -> c.cargo().getId().equals(nova.origem().cargoNovo()))
                .findFirst().orElseThrow().dataFim());
    }

    private void fonte(JsonNode composicao, String chave, long id) {
        JsonNode campo = composicao.path("provaCampos").path("dataFimVigencia");
        assertThat(campo.path("estado").asText()).isEqualTo("VALOR_COMPROVADO");
        assertThat(campo.path("visibilidade").asText()).isEqualTo("DISPONIVEL");
        boolean encontrada = false;
        for (JsonNode origem : campo.path("origens"))
            if (origem.path(chave).isNumber() && origem.path(chave).asLong() == id) encontrada = true;
        assertThat(encontrada).as("Limite herdado requer referência histórica real: " + chave + "=" + id).isTrue();
    }
}
