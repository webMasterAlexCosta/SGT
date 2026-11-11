package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * C4-CON-H03. Autoria: revisor independente do núcleo contratual.
 * Fonte C4: 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Proposta para execução central, sem execução/compilação pelo autor.
 *
 * Usa publicação e duas correções legítimas por HTTP, fontes sintéticas explícitas
 * e concessão específica. Não altera fatos antigos, relógio ou campos via reflexão.
 * O recibo de correção expõe registradoEm; eventos, versões e relações aqui são
 * capturados como fatos ainda gerenciados na transação da chamada HTTP. NÃO se
 * atribui esses três campos ao ContratoDTO de publicação, que não os devolve.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4PrecisaoFatosTemporaisRedIT extends ContratosHttpBase {

    @Test
    void publicacaoConservaInstantesDoEventoEVersaoEmNovaTransacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        Captura captura = emTransacao(() -> {
            try {
                NovoContrato n = publicado(c, 10, null, true);
                EventoCicloVidaContrato e = em.createQuery(
                                "select e from EventoCicloVidaContrato e where e.contrato.id=:id", EventoCicloVidaContrato.class)
                        .setParameter("id", n.id()).getSingleResult();
                VersaoContratoNegocial v = em.createQuery(
                                "select v from VersaoContratoNegocial v where v.contrato.id=:id", VersaoContratoNegocial.class)
                        .setParameter("id", n.id()).getSingleResult();
                return new Captura(n, List.of(evento(e), versao(v)));
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        assertAll("Publicação: novos fatos mantêm o instante ao persistir",
                observar(c, captura, "publicacao"));
    }

    @Test
    void correcaoComSucessaoConservaReciboEventoVersaoERelacaoDepoisDoCommitEReenvio() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        conceder(c, n.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        String rota = base(n.id()) + "/correcoes-identificador";
        JsonNode primeira = enviar(c.localA, rota, correcao(n), 201,
                "Controle: primeira correção com fonte e concessão específicas");
        Map<String, Object> req = correcao(n);
        req.put("eventoPrecedenteId", primeira.path("eventoId").asLong());
        req.put("relacaoPrecedencia", "SUCEDE");
        Captura captura = emTransacao(() -> {
            try {
                JsonNode recibo = enviar(c.localA, rota, req, 201,
                        "Controle: segunda correção declara a sequência negocial na mesma data");
                long eventoId = recibo.path("eventoId").asLong();
                EventoCicloVidaContrato e = em.find(EventoCicloVidaContrato.class, eventoId);
                VersaoContratoNegocial v = em.createQuery(
                                "select v from VersaoContratoNegocial v where v.eventoOrigem.id=:id", VersaoContratoNegocial.class)
                        .setParameter("id", eventoId).getSingleResult();
                RelacaoPrecedenciaEventoContrato r = em.createQuery(
                                "select r from RelacaoPrecedenciaEventoContrato r where r.eventoFonte.id=:id",
                                RelacaoPrecedenciaEventoContrato.class)
                        .setParameter("id", eventoId).getSingleResult();
                return new Captura(n, List.of(new Marcador("CorrecaoIdentificadorContrato",
                        recibo.path("id").asLong(), Instant.parse(recibo.path("registradoEm").asText()),
                        "/correcoes-identificador", "recibo HTTP da correção"), evento(e), versao(v),
                        new Marcador("RelacaoPrecedenciaEventoContrato", r.getId(), r.getRegistradoEm(),
                                "/eventos/precedencias", "nova relação ainda gerenciada; não é campo do recibo")));
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        List<Executable> verificacoes = observar(c, captura, "correcao-com-sucessao");
        Marcador correcao = captura.marcadores().get(0);
        Instant persistido = instante(correcao);
        long antes = contar(n.id(), "CorrecaoIdentificadorContrato");
        JsonNode reenvio = enviar(c.localA, rota, req, 201, "Reenvio autorizado retorna o fato existente");
        long depois = contar(n.id(), "CorrecaoIdentificadorContrato");
        gravar("reenvio-correcao", mapa("id", correcao.id(), "emitido", correcao.instante(),
                "persistido", persistido, "reenvio", reenvio, "quantidadeAntes", antes, "quantidadeDepois", depois));
        verificacoes.add(() -> assertThat(reenvio.path("id").asLong()).isEqualTo(correcao.id()));
        verificacoes.add(() -> assertThat(Instant.parse(reenvio.path("registradoEm").asText()))
                .as("Reenvio conserva o instante originalmente emitido").isEqualTo(correcao.instante()));
        verificacoes.add(() -> assertThat(antes).isEqualTo(2L));
        verificacoes.add(() -> assertThat(depois).isEqualTo(antes));
        assertAll("Correção: recibo e fatos da mesma decisão mantêm seus instantes", verificacoes);
    }

    private Map<String, Object> correcao(NovoContrato n) {
        return mapa("novoNumero", "PRECISAO-" + UUID.randomUUID().toString().substring(0, 8),
                "novoAno", n.inicio().getYear(), "dataEfetiva", n.inicio().plusDays(1),
                "dataAto", RelogioSistema.hoje(),
                "motivo", "Fonte sintética explícita para verificar precisão da correção e de sua sequência",
                "referenciaSei", "SEI-SINTETICO-PRECISAO-CORRECAO", "autorizacaoAdministrativa", "ATO-HTTP",
                "chaveIdempotencia", UUID.randomUUID().toString());
    }

    private Marcador evento(EventoCicloVidaContrato e) {
        return new Marcador("EventoCicloVidaContrato", e.getId(), e.getRegistradoEm(), "/eventos",
                "novo evento ainda gerenciado; não é campo do ContratoDTO de publicação");
    }

    private Marcador versao(VersaoContratoNegocial v) {
        return new Marcador("VersaoContratoNegocial", v.getId(), v.getRegistradoEm(), "/versoes",
                "nova versão ainda gerenciada; não é campo do ContratoDTO de publicação");
    }

    private List<Executable> observar(Cenario c, Captura captura, String nome) throws Exception {
        List<Executable> verificacoes = new ArrayList<>();
        List<Map<String, Object>> registros = new ArrayList<>();
        for (Marcador m : captura.marcadores()) {
            Instant gravado = instante(m);
            JsonNode lista = consultar(c.nacional, base(captura.contrato().id()) + m.rota(),
                    "Consulta posterior ao commit do fato temporal");
            JsonNode encontrado = null;
            for (JsonNode item : lista) if (item.path("id").asLong() == m.id()) encontrado = item;
            if (encontrado == null) throw new AssertionError("Fato ausente: " + m.entidade() + "/" + m.id());
            Instant consultado = Instant.parse(encontrado.path("registradoEm").asText());
            registros.add(mapa("entidade", m.entidade(), "id", m.id(), "naturezaCaptura", m.natureza(),
                    "instanteEmMemoriaOuRecibo", m.instante(), "persistidoNovaTransacao", gravado,
                    "consultadoHttp", consultado));
            verificacoes.add(() -> assertThat(m.instante()).as(m.entidade() + ": precisão persistível do novo fato")
                    .isEqualTo(m.instante().truncatedTo(ChronoUnit.MICROS)));
            verificacoes.add(() -> assertThat(m.instante()).as(m.entidade() + ": instante não muda ao reler o PostgreSQL")
                    .isEqualTo(gravado));
            verificacoes.add(() -> assertThat(consultado).as(m.entidade() + ": GET coincide com persistência").isEqualTo(gravado));
        }
        gravar(nome, mapa("contratoId", captura.contrato().id(), "fatos", registros));
        return verificacoes;
    }

    private Instant instante(Marcador m) {
        return emTransacao(() -> em.createQuery("select e.registradoEm from " + m.entidade() + " e where e.id=:id", Instant.class)
                .setParameter("id", m.id()).getSingleResult());
    }

    private long contar(long contratoId, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contratoId).getSingleResult());
    }

    private void gravar(String nome, Object dados) throws Exception {
        Path destino = Path.of("target/auditoria-http/candidato4-precisao-fatos-" + nome + ".json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json(dados));
    }

    private record Marcador(String entidade, long id, Instant instante, String rota, String natureza) {}
    private record Captura(NovoContrato contrato, List<Marcador> marcadores) {}
}
