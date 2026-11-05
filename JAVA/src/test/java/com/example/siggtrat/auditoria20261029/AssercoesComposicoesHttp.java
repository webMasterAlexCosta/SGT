package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Separa o instante da consulta de seus fatos sem reutilizar um corte antigo.
 * Preserva igualdade exata de TODOS os demais campos, inclusive novas provas,
 * origens, estados e valores aninhados. Não normaliza datas ou dados de negócio.
 */
final class AssercoesComposicoesHttp {
    private AssercoesComposicoesHttp() { }

    @FunctionalInterface
    interface Consulta { JsonNode executar() throws Exception; }

    record Leitura(JsonNode dados, Instant inicioConsulta, Instant fimConsulta) {
        Leitura { dados = dados.deepCopy(); }
    }

    static Leitura capturar(Consulta consulta) throws Exception {
        Instant inicio = Instant.now();
        JsonNode dados = consulta.executar();
        Instant fim = Instant.now();
        return new Leitura(dados, inicio, fim);
    }

    static void exigirPreservacaoComConsultaPosterior(Leitura antes, Leitura depois) {
        Instant conhecidoAntes = validarInstanteDaPropriaConsulta(antes);
        Instant conhecidoDepois = validarInstanteDaPropriaConsulta(depois);
        assertThat(depois.inicioConsulta()).as("Segunda leitura foi feita depois da primeira")
                .isAfterOrEqualTo(antes.fimConsulta());
        assertThat(conhecidoDepois).as("Sem conhecidoAte explícito, o corte deve avançar com a nova consulta")
                .isAfter(conhecidoAntes);
        assertThat(depois.dados().size()).as("Nenhuma composição pode ser adicionada ou removida pela recusa")
                .isEqualTo(antes.dados().size());
        for (int indice = 0; indice < antes.dados().size(); indice++) {
            ObjectNode fatoAntes = ((ObjectNode) antes.dados().get(indice)).deepCopy();
            ObjectNode fatoDepois = ((ObjectNode) depois.dados().get(indice)).deepCopy();
            // Única separação permitida: metadado da consulta, já validado acima.
            // Não remover recursivamente campos homônimos de provas históricas.
            fatoAntes.remove("conhecidoAte");
            fatoDepois.remove("conhecidoAte");
            assertThat(fatoDepois).as("Composição %s conserva todos os campos de negócio e de prova", indice)
                    .isEqualTo(fatoAntes);
        }
    }

    private static Instant validarInstanteDaPropriaConsulta(Leitura leitura) {
        assertThat(leitura.dados().isArray()).as("A API devolve a coleção real de composições").isTrue();
        assertThat(leitura.dados()).as("Os cenários possuem composições publicadas explícitas").isNotEmpty();
        assertThat(leitura.fimConsulta()).isAfterOrEqualTo(leitura.inicioConsulta());
        Instant primeiro = null;
        for (JsonNode item : leitura.dados()) {
            assertThat(item.isObject()).isTrue();
            assertThat(item.path("conhecidoAte").isTextual())
                    .as("Cada composição informa o instante de conhecimento da consulta").isTrue();
            Instant conhecido = Instant.parse(item.path("conhecidoAte").asText());
            assertThat(conhecido).as("O corte deve pertencer à requisição atual, sem reutilizar o corte anterior")
                    .isBetween(leitura.inicioConsulta(), leitura.fimConsulta());
            if (primeiro == null) primeiro = conhecido;
            else assertThat(conhecido).as("Uma coleção usa o mesmo corte de conhecimento").isEqualTo(primeiro);
        }
        return primeiro;
    }
}
