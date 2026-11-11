package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Regressão do RED novos_red_05: TMP004/005, CAT004 e HIS004.
 * Publicação nova deve preservar configuração exata; acervo legado sem fonte
 * conserva desconhecimento. IDs de catálogo não reconstroem horários perdidos.
 * Atos de hoje e futuros possuem fotografia própria.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT extends ContratosHttpBase {

    @Test
    void novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c);
        Configuracao original = configuracaoMestre(c);
        assertThat(minutosSegunda(original.grade())).isEqualTo(480);
        FotografiaComposicao publicada = fotografiaComposicao(n.composicao1());
        assertThat(publicada.configuracao()).isEqualTo(original);
        Configuracao atual = alterarSomenteGradeEMunicipio(c);
        assertThat(minutosSegunda(atual.grade())).isEqualTo(360);
        assertThat(atual.municipio()).isNotEqualTo(original.municipio());

        LocalDate data = n.inicio().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(data).isBefore(RelogioSistema.hoje());
        long vinculo = cadastrar(c, n, data, 688000000L);
        Configuracao fotografia = fotografia(vinculo);

        // A publicação nova comprova a configuração completa. O RED antigo,
        // arquivado, admitia null porque a fonte então não guardava esses campos.
        JsonNode relatorio = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", data.toString()).param("fim", data.toString())
                .param("contratoIds", Long.toString(n.id())), 200,
                "A previsão retroativa usa as oito horas diárias publicadas, preservadas na composição"));
        JsonNode frequencia = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo, "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Registro da presença; não altera a grade nem o município históricos",
                "chaveIdempotencia", "configuracao-retroativa-" + UUID.randomUUID()), 200,
                "Uma presença conserva os horários publicados, sem copiar o mestre posterior");
        Integer minutosPersistidos = emTransacao(() ->
                em.find(Frequencia.class, frequencia.path("id").asLong()).getMinutosFisicos());
        String municipioPersistido = emTransacao(() ->
                em.find(Frequencia.class, frequencia.path("id").asLong()).getLotacaoSnapshotMunicipio());
        assertAll(
                () -> assertThat(fotografia.grade()).isEqualTo(original.grade()),
                () -> assertThat(fotografia.municipio()).isEqualTo(original.municipio()),
                () -> assertThat(relatorio.path("registros").size()).isEqualTo(1),
                () -> assertThat(relatorio.path("registros").get(0).path("diario").path(data.toString())
                        .path("minutosPrevistos").asInt()).isEqualTo(480),
                () -> assertThat(minutosPersistidos).isEqualTo(480),
                () -> assertThat(municipioPersistido).isEqualTo(original.municipio()),
                () -> assertThat(fotografiaComposicao(n.composicao1()))
                        .as("Projeção e apuração não alteram a fotografia JPA nem sua versão")
                        .isEqualTo(publicada));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void novoAtoDeHojeOuFuturoContinuaCapturandoAConfiguracaoAtual(boolean futuro) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c);
        Configuracao atual = alterarSomenteGradeEMunicipio(c);
        long vinculo = cadastrar(c, n, RelogioSistema.hoje().plusDays(futuro ? 10 : 0), 689000000L);
        Configuracao fotografia = fotografia(vinculo);
        assertAll(
                () -> assertThat(minutosSegunda(atual.grade())).isEqualTo(360),
                () -> assertThat(fotografia.grade()).isEqualTo(atual.grade()),
                () -> assertThat(fotografia.municipio()).isEqualTo(atual.municipio()));
    }

    @Test
    void ausenciaLegitimaDaGradeNaoEPreenchidaPelaAlteracaoAtualDoMestre() throws Exception {
        Cenario c = criarCenarioPersistido();
        emTransacao(() -> {
            // DML de fixture simula somente acervo anterior à fotografia semanal.
            // Não altera schema, datas, IDs, eventos ou disponibilidade dos catálogos.
            assertThat(em.createQuery("update AlocacaoVinculo a set a.jornadaSemanalFotografia=null, "
                            + "a.lotacaoMunicipioFotografia=null where a.id=:id")
                    .setParameter("id", c.alocacaoA.getId()).executeUpdate()).isEqualTo(1);
            return null;
        });
        alterarSomenteGradeEMunicipio(c);
        Configuracao fotografia = fotografia(c.vinculoA.getId());
        assertThat(fotografia.grade()).isNull();
        assertThat(fotografia.municipio()).isNull();
        executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoIds", c.contratoA.getId().toString()), 409,
                "Fonte canônica legada sem grade não usa os horários atuais como origem");
        enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA, "status", "PRESENCA",
                "justificativaRetificacao", "Controle sem fonte da configuração histórica",
                "chaveIdempotencia", "configuracao-ausente-" + UUID.randomUUID()), 409,
                "Ausência histórica permanece explícita depois de editar o mestre");
        assertThat(fotografia(c.vinculoA.getId())).isEqualTo(fotografia);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate(boolean atualizarOutraColuna) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c);
        emTransacao(() -> {
            assertThat(em.createQuery("update ComposicaoContratual c set c.jornadaSemanalFotografia=null, "
                            + "c.lotacaoMunicipioFotografia=null where c.id=:id")
                    .setParameter("id", n.composicao1()).executeUpdate()).isEqualTo(1);
            return null;
        });
        alterarSomenteGradeEMunicipio(c);
        if (atualizarOutraColuna) emTransacao(() -> {
            // A atualização controlada da fixture força @PreUpdate de acervo
            // publicado, sem colocar a consulta abaixo fora do período válido.
            em.find(ComposicaoContratual.class, n.composicao1()).setDataFimVigencia(n.fim().minusDays(1));
            return null;
        });
        LocalDate data = n.inicio().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        long vinculo = cadastrar(c, n, data, 690000000L);
        Configuracao ausencia = new Configuracao(null, null);
        assertThat(fotografiaComposicao(n.composicao1()).configuracao()).isEqualTo(ausencia);
        assertThat(fotografia(vinculo)).isEqualTo(ausencia);
        executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", data.toString()).param("fim", data.toString())
                .param("contratoIds", Long.toString(n.id())), 409,
                "Nova alocação derivada de publicação legada sem grade não permite previsão inventada");
        enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo, "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Presença não recupera os horários ausentes da publicação legada",
                "chaveIdempotencia", "composicao-sem-grade-" + UUID.randomUUID()), 409,
                "Frequência exige regularização documental da configuração desconhecida");
        long frequencias = emTransacao(() -> em.createQuery(
                        "select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                .setParameter("v", vinculo).getSingleResult());
        assertThat(frequencias).isZero();
        assertThat(fotografia(vinculo)).isEqualTo(ausencia);
    }

    @Test
    void municipioOriginalLegitimamenteNuloNaoECompletadoPeloMestrePosterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = "/api/lotacoes/" + c.lotacaoA.getId();
        ObjectNode lote = (ObjectNode) consultar(c.localA, rota, "Cadastro anterior à publicação").deepCopy();
        lote.putNull("municipio");
        executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON).content(json(lote)), 200,
                "O município é campo opcional e está legitimamente ausente no original");
        NovoContrato n = publicar(c);
        Configuracao original = configuracaoMestre(c);
        assertThat(original.municipio()).isNull();
        assertThat(minutosSegunda(original.grade())).isEqualTo(480);
        alterarSomenteGradeEMunicipio(c);
        LocalDate data = n.inicio().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        long vinculo = cadastrar(c, n, data, 691000000L);
        assertThat(fotografia(vinculo)).isEqualTo(original);
        JsonNode f = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo, "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Grade comprovada permite frequência sem inventar município",
                "chaveIdempotencia", "municipio-original-nulo-" + UUID.randomUUID()), 200,
                "Ausência de município não elimina a grade documentada da publicação");
        emTransacao(() -> {
            Frequencia persistida = em.find(Frequencia.class, f.path("id").asLong());
            assertThat(persistida.getMinutosFisicos()).isEqualTo(480);
            assertThat(persistida.getLotacaoSnapshotMunicipio()).isNull();
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia(boolean mudarDepoisDaPrevia) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().minusDays(30);
        LocalDate fim = RelogioSistema.hoje().plusDays(120);
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c,
                "FOTO-GRADE-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim), 201,
                "Preparação ainda não congela a configuração original do contrato").path("id").asLong();
        long composicao = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "Rascunho começa com a Jornada de oito horas").path("id").asLong();
        FotografiaComposicao rascunho = fotografiaComposicao(composicao);
        assertThat(minutosSegunda(rascunho.configuracao().grade())).isEqualTo(480);
        Configuracao definitiva = mudarDepoisDaPrevia ? configuracaoMestre(c) : alterarSomenteGradeEMunicipio(c);
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-GRADE-DEFINITIVA",
                "justificativa", "Publicação da configuração final documentada");
        JsonNode previa = enviar(c.localA, base(contrato) + "/previas/publicar", publicacao, 200,
                "Prévia real antes de publicar o original retroativo");
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        if (mudarDepoisDaPrevia) {
            definitiva = alterarSomenteGradeEMunicipio(c);
            enviar(c.localA, base(contrato) + "/publicar", publicacao, 409,
                    "Token anterior não autoriza publicar outra grade e outro município");
            assertThat(consultar(c.localA, base(contrato), "Publicação rejeitada não modifica o estado")
                    .path("estadoNegocial").asText()).isEqualTo("RASCUNHO");
            assertThat(fotografiaComposicao(composicao)).isEqualTo(rascunho);
            assertThat(consultar(c.localA, base(contrato) + "/eventos", "Grade alterada não publica evento parcial")).isEmpty();
            assertThat(consultar(c.localA, base(contrato) + "/versoes", "Grade alterada não publica versão parcial")).isEmpty();
            JsonNode atualizada = enviar(c.localA, base(contrato) + "/previas/publicar", publicacao, 200,
                    "Nova prévia vincula a confirmação à configuração alterada");
            assertThat(atualizada.path("tokenPrevia").asText()).isNotEqualTo(previa.path("tokenPrevia").asText());
            publicacao.put("tokenPrevia", atualizada.path("tokenPrevia").asText());
        }
        assertThat(minutosSegunda(definitiva.grade())).isEqualTo(360);
        enviar(c.localA, base(contrato) + "/publicar", publicacao, 200,
                "Publicação fotografa a configuração final, não a escolha anterior do rascunho");
        FotografiaComposicao publicada = fotografiaComposicao(composicao);
        assertThat(publicada.configuracao()).isEqualTo(definitiva);

        String rotaJornada = "/api/jornadas/" + c.jornadaA.getId();
        ObjectNode turno = (ObjectNode) consultar(c.localA, rotaJornada, "Mestre posterior à publicação").deepCopy();
        for (JsonNode dia : turno.path("dias")) {
            if (dia.path("diaTrabalho").asBoolean()) ((ObjectNode) dia).put("saida2", "14:00");
        }
        executar(c.localA, put(rotaJornada).contentType(MediaType.APPLICATION_JSON).content(json(turno)), 200,
                "Nova mudança do mestre para cinco horas preserva a publicação de seis horas");
        assertThat(minutosSegunda(configuracaoMestre(c).grade())).isEqualTo(300);
        LocalDate data = inicio.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        long vinculo = cadastrar(c, new NovoContrato(contrato, composicao, null, inicio, fim), data, 692000000L);
        assertThat(fotografia(vinculo)).isEqualTo(definitiva);
        assertThat(fotografiaComposicao(composicao)).isEqualTo(publicada);
    }

    private FotografiaComposicao fotografiaComposicao(long id) {
        return emTransacao(() -> {
            ComposicaoContratual c = em.find(ComposicaoContratual.class, id);
            return new FotografiaComposicao(new Configuracao(c.getJornadaSemanalFotografia(),
                    c.getLotacaoMunicipioFotografia()), c.getVersion());
        });
    }

    private NovoContrato publicar(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private long cadastrar(Cenario c, NovoContrato n, LocalDate data, long semente) throws Exception {
        return enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa da revisão de configuração " + n.id(),
                        "cpf", cpf(Math.toIntExact(semente + n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", data),
                201, "Nova pessoa/vínculo/alocação informa apenas a composição e a data efetiva")
                .path("id").asLong();
    }

    private Configuracao alterarSomenteGradeEMunicipio(Cenario c) throws Exception {
        String rotaJornada = "/api/jornadas/" + c.jornadaA.getId();
        ObjectNode jornada = (ObjectNode) consultar(c.localA, rotaJornada,
                "Preserva os sete dias, regras adicionais, escopo e disponibilidade").deepCopy();
        for (JsonNode dia : jornada.path("dias")) {
            if (dia.path("diaTrabalho").asBoolean()) ((ObjectNode) dia).put("saida2", "15:00");
        }
        executar(c.localA, put(rotaJornada).contentType(MediaType.APPLICATION_JSON).content(json(jornada)),
                200, "Mudança administrativa atual de 8h para 6h, sem retificar configuração passada");
        String rotaLotacao = "/api/lotacoes/" + c.lotacaoA.getId();
        ObjectNode lotacao = (ObjectNode) consultar(c.localA, rotaLotacao,
                "Preserva nome, turnos, escopo e disponibilidade da Lotação").deepCopy();
        lotacao.put("municipio", "MUNICIPIO POSTERIOR " + c.lotacaoA.getId());
        executar(c.localA, put(rotaLotacao).contentType(MediaType.APPLICATION_JSON).content(json(lotacao)),
                200, "Mudança atual de município não é correção documental retroativa");
        return configuracaoMestre(c);
    }

    private Configuracao configuracaoMestre(Cenario c) {
        return emTransacao(() -> new Configuracao(
                FotografiaJornada.capturar(em.find(Jornada.class, c.jornadaA.getId())),
                em.find(Lotacao.class, c.lotacaoA.getId()).getMunicipio()));
    }

    private Configuracao fotografia(long vinculo) {
        return emTransacao(() -> {
            AlocacaoVinculo a = em.createQuery(
                            "select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).getSingleResult();
            return new Configuracao(a.getJornadaSemanalFotografia(), a.getLotacaoMunicipioFotografia());
        });
    }

    private int minutosSegunda(String grade) {
        return FotografiaJornada.dias(grade).stream().filter(d -> d.diaSemana() == DayOfWeek.MONDAY)
                .findFirst().orElseThrow().minutosPrevistos();
    }

    private record Configuracao(String grade, String municipio) {}
    private record FotografiaComposicao(Configuracao configuracao, long versao) {}
}
