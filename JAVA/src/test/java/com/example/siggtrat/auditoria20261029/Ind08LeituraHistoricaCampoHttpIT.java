package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.sgtfixture.ind08.ProvaHistoricaContratoFalhaFixture;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * IND-08: contrato aditivo de leitura por campo, baseado em fontes sintéticas
 * explícitas. A fixture simula perda documental por DML; não altera schema nem
 * usa cadastros atuais como substitutos da história. Cada request lê em sua
 * transação real. MockMvc prova Spring/segurança/JPA/PostgreSQL, não transporte TCP.
 *
 * Falhas que estes testes devem detectar: deduzir prova da verdade booleana,
 * ampliar máscara parcial, vazar K futuro, confundir restrição com ausência,
 * perder a origem em projeção ou certificar rótulo a partir de seu catálogo.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(Ind08LeituraHistoricaCampoHttpIT.MapeamentoDaFalha.class)
class Ind08LeituraHistoricaCampoHttpIT extends ContratosHttpBase {
    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {
            com.example.siggtrat.AplicacaoBackendSigGtrat.class,
            ProvaHistoricaContratoFalhaFixture.class
    })
    static class MapeamentoDaFalha { }

    private static final String GRADE = "MONDAY|true|08:00|12:00|13:00|17:00;"
            + "TUESDAY|true|08:00|12:00|13:00|17:00;WEDNESDAY|true|08:00|12:00|13:00|17:00;"
            + "THURSDAY|true|08:00|12:00|13:00|17:00;FRIDAY|true|08:00|12:00|13:00|17:00;"
            + "SATURDAY|false||||;SUNDAY|false||||";
    private static final String SEI_RESTRITO = "SEI-SINTETICO-IND08-NAO-EXPOR";
    private static final String MOTIVO_RESTRITO = "Narrativa sintética restrita da conferência IND08";
    private static final List<String> METADADOS_RESTRITOS = List.of("referenciaSei", "motivo",
            "autorizacaoAdministrativa", "documentoComprobatorioId", "hashFonteArquivo");

    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motor;

    private record Origem(Cenario c, NovoContrato n, long versao, long evento,
                          Map<String, Object> fotografia) { }

    @BeforeEach
    void exigirPersistenciaRealSemDdlManual() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void publicacaoDistingueZeroFalsoEAusenciaSemInventarIdParaPosicaoProjetada() throws Exception {
        Origem o = origem(true);
        JsonNode posicao = posicao(o, o.n().inicio(), null);
        JsonNode versao = posicao.path("versaoContratual");
        JsonNode acervo = versaoPorId(versoes(o, null), o.versao());
        JsonNode zero = itemPorId(composicoes(o, o.n().inicio(), null), o.n().composicao2());

        assertAll("Publicação tem fonte persistida; a posição continua sendo projeção",
                () -> assertThat(versao.path("id").isNull()).isTrue(),
                () -> estado(versao, "matriculaTipoContratacaoObrigatoria", "VALOR_COMPROVADO"),
                () -> assertThat(versao.path("matriculaTipoContratacaoObrigatoria").booleanValue()).isFalse(),
                () -> estado(versao, "matriculaVinculoObrigatoria", "AUSENCIA_COMPROVADA"),
                () -> estado(versao, "diaSemanaReduzido", "AUSENCIA_COMPROVADA"),
                () -> origemContem(versao, "numeroContrato", "versaoId", o.versao()),
                () -> estado(acervo, "matriculaTipoContratacaoObrigatoria", "VALOR_COMPROVADO"),
                () -> assertThat(zero.path("efetivoPrevisto").intValue()).isZero(),
                () -> estado(zero, "efetivoPrevisto", "VALOR_COMPROVADO"),
                () -> assertThat(zero.path("permiteNovaAlocacao").booleanValue()).isFalse(),
                () -> estado(zero, "permiteNovaAlocacao", "VALOR_COMPROVADO"),
                () -> origemContem(zero, "efetivoPrevisto", "composicaoId", o.n().composicao2()),
                () -> estado(posicao, "vigenciaInicio", "VALOR_COMPROVADO"));
    }

    @Test
    void mascaraParcialHerdaSomenteProvaRealPreservandoKExatoEVersoesAnteriores() throws Exception {
        Origem o = origem(false);
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.descricao=null, v.tipoContratacaoDescricao=null, "
                    + "v.matriculaVinculoObrigatoria=null, v.matriculaTipoContratacaoObrigatoria=null, "
                    + "v.matriculaVinculoObrigatoriaEfetiva=null, v.diaSemanaReduzido=null, v.horasDiaReduzido=null where v.id=:id")
                    .setParameter("id", o.versao()).executeUpdate();
            return null;
        });
        conceder(o.c(), o.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode p1 = incorporar(o, fonte(o, null, List.of("descricao"),
                mapa("descricao", "Descrição documental da primeira prova"), List.of()));
        Instant k1 = instante(p1);
        JsonNode p2 = incorporar(o, fonte(o, p1,
                List.of("matriculaVinculoObrigatoria", "matriculaTipoContratacaoObrigatoria", "matriculaVinculoObrigatoriaEfetiva"),
                mapa("matriculaVinculoObrigatoria", false, "matriculaTipoContratacaoObrigatoria", false,
                        "matriculaVinculoObrigatoriaEfetiva", false), List.of()));
        Instant k2 = instante(p2);
        JsonNode p3 = incorporar(o, fonte(o, p2, List.of("diaSemanaReduzido", "horasDiaReduzido"),
                mapa("diaSemanaReduzido", null, "horasDiaReduzido", null), List.of()));
        Instant k3 = instante(p3);
        Path fixture=Path.of("target/evolucao-fixtures/ind08-historico-parcial.json");
        Files.createDirectories(fixture.getParent());
        Files.writeString(fixture,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "origem", "Fixture sintética IND08: lacunas de acervo simuladas no teste; provas incorporadas pela API canônica",
                "contratoId",o.n().id(),"matriculaGestor",o.c().localA.getMatricula(),"dataReferencia",o.n().inicio(),
                "conhecidoAntes",k1.minusNanos(1),"conhecidoAposDescricao",k1,
                "conhecidoAposMatricula",k2,"conhecidoAposAusencia",k3,
                "provaDescricaoId",p1.path("id").longValue(),"provaMatriculaId",p2.path("id").longValue(),
                "provaAusenciaId",p3.path("id").longValue())));

        JsonNode anterior = posicao(o, o.n().inicio(), k1.minusNanos(1)).path("versaoContratual");
        JsonNode emK1 = posicao(o, o.n().inicio(), k1).path("versaoContratual");
        JsonNode emK2 = posicao(o, o.n().inicio(), k2).path("versaoContratual");
        JsonNode emK3 = posicao(o, o.n().inicio(), k3).path("versaoContratual");
        JsonNode fotoP2 = p2.path("fotografia");
        JsonNode acervoOriginal = versaoPorId(versoes(o, k3), o.versao());

        assertAll("Uma máscara de matrícula não prova ausência de dia reduzido nem rótulo do Tipo",
                () -> estado(anterior, "descricao", "NAO_COMPROVADO"),
                () -> estado(emK1, "descricao", "VALOR_COMPROVADO"),
                () -> origemContem(emK1, "descricao", "provaId", p1.path("id").longValue()),
                () -> estado(emK1, "matriculaTipoContratacaoObrigatoria", "NAO_COMPROVADO"),
                () -> estado(emK2, "matriculaTipoContratacaoObrigatoria", "VALOR_COMPROVADO"),
                () -> assertThat(emK2.path("matriculaTipoContratacaoObrigatoria").booleanValue()).isFalse(),
                () -> estado(emK2, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(emK2, "tipoContratacaoDescricao", "NAO_COMPROVADO"),
                () -> estado(emK3, "diaSemanaReduzido", "AUSENCIA_COMPROVADA"),
                () -> origemContem(emK3, "diaSemanaReduzido", "provaId", p3.path("id").longValue()),
                () -> origemContem(emK3, "descricao", "provaId", p1.path("id").longValue()),
                () -> estado(fotoP2, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> origemContem(fotoP2, "descricao", "provaId", p1.path("id").longValue()),
                () -> estado(acervoOriginal, "descricao", "NAO_COMPROVADO"),
                () -> assertThat(p2.path("camposComprovados").toString()).doesNotContain("diaSemanaReduzido", "descricao"));
        assertThat(versoes(o, k1.minusNanos(1))).hasSize(1);
        assertThat(versoes(o, k1)).hasSize(2);
        assertThat(provas(o, k2)).hasSize(2);
        emTransacao(() -> {
            em.clear();
            assertThat(em.find(VersaoContratoNegocial.class, o.versao()).getDescricao()).isNull();
            assertThat(em.find(ProvaHistoricaContrato.class, p1.path("id").longValue()).getRegistradoEm()).isEqualTo(k1);
            assertThat(em.find(ProvaHistoricaContrato.class, p3.path("id").longValue()).getProvaAnterior().getId())
                    .isEqualTo(p2.path("id").longValue());
            return null;
        });
    }

    @Test
    void provaDeGradeNaoProvaMunicipioERotuloNaoVemDoMestreAtual() throws Exception {
        Origem o = origem(false);
        emTransacao(() -> {
            em.createQuery("update ComposicaoContratual c set c.lotacaoNomeFotografia='', c.jornadaDescricaoFotografia='', "
                    + "c.cargoNomeFotografia='', c.lotacaoMunicipioFotografia=null, c.jornadaSemanalFotografia=null where c.id=:id")
                    .setParameter("id", o.n().composicao1()).executeUpdate();
            em.createQuery("update Lotacao l set l.nome='Mestre atual não é fotografia', l.municipio='Município atual indevido' where l.id=:id")
                    .setParameter("id", o.c().lotacaoA.getId()).executeUpdate();
            return null;
        });
        conceder(o.c(), o.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode p1 = incorporar(o, fonte(o, null, List.of(), Map.of(), List.of(mapa(
                "composicaoId", o.n().composicao1(), "lotacaoNome", "Lotação comprovada pela fonte original",
                "jornadaSemanal", GRADE, "jornadaComprovada", true, "municipioComprovado", false))));
        Instant k1 = instante(p1);
        JsonNode p2 = incorporar(o, fonte(o, p1, List.of(), Map.of(), List.of(mapa(
                "composicaoId", o.n().composicao1(), "lotacaoMunicipio", null,
                "municipioComprovado", true, "jornadaComprovada", false))));
        Instant k2 = instante(p2);
        JsonNode antes = itemPorId(composicoes(o, o.n().inicio(), k1.minusNanos(1)), o.n().composicao1());
        JsonNode entre = itemPorId(composicoes(o, o.n().inicio(), k1), o.n().composicao1());
        JsonNode depois = itemPorId(composicoes(o, o.n().inicio(), k2), o.n().composicao1());
        JsonNode fragmento = p2.path("composicoes").path(0);

        assertAll("Máscaras municipais/semanais e os rótulos permanecem independentes",
                () -> estado(antes, "lotacaoNome", "NAO_COMPROVADO"),
                () -> estado(antes, "jornadaSemanal", "NAO_COMPROVADO"),
                () -> estado(entre, "jornadaSemanal", "VALOR_COMPROVADO"),
                () -> assertThat(entre.path("jornadaSemanal").asText()).isEqualTo(GRADE),
                () -> estado(entre, "lotacaoMunicipio", "NAO_COMPROVADO"),
                () -> assertThat(entre.path("lotacaoMunicipio").isNull()).isTrue(),
                () -> assertThat(depois.path("lotacaoNome").asText()).isEqualTo("Lotação comprovada pela fonte original"),
                () -> estado(depois, "cargoNome", "NAO_COMPROVADO"),
                () -> estado(depois, "jornadaDescricao", "NAO_COMPROVADO"),
                () -> estado(depois, "lotacaoMunicipio", "AUSENCIA_COMPROVADA"),
                () -> origemContem(depois, "lotacaoMunicipio", "provaId", p2.path("id").longValue()),
                () -> origemContem(depois, "jornadaSemanal", "provaId", p1.path("id").longValue()),
                () -> estado(fragmento, "lotacaoMunicipio", "AUSENCIA_COMPROVADA"),
                () -> estado(fragmento, "jornadaSemanal", "NAO_COMPROVADO"),
                () -> assertThat(depois.path("conhecidoAte").asText()).isEqualTo(k2.toString()));
        emTransacao(() -> {
            em.clear();
            var original = em.find(ComposicaoContratual.class, o.n().composicao1());
            assertThat(original.getLotacaoNomeFotografia()).isEmpty();
            assertThat(original.getJornadaSemanalFotografia()).isNull();
            assertThat(original.getLotacaoMunicipioFotografia()).isNull();
            return null;
        });
    }

    @Test
    void deltasEmDConservamOrigemDeCampoEComposicaoNovaNaoGanhaIdFisico() throws Exception {
        Origem o = origem(false);
        long cargo = emTransacao(() -> {
            Cargo novo = new Cargo("Cargo documental novo IND08", "1");
            novo.setGestaoLocal(em.find(GestaoLocal.class, o.c().gestaoA.getId()));
            em.persist(novo); em.flush();
            var disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.CARGO);
            disponibilidade.setCatalogoId(novo.getId()); disponibilidade.setDataEfetiva(o.n().inicio());
            disponibilidade.setAtivo(true); disponibilidade.setMotivo("Disponibilidade sintética explícita IND08");
            em.persist(disponibilidade); em.flush();
            return novo.getId();
        });
        LocalDate dataAditamento = o.n().inicio().plusDays(3);
        Map<String, Object> req = aditamento("IND08-ADI-" + o.n().id(), dataAditamento);
        req.put("novaDescricao", "Descrição que só vale a partir do ADI");
        req.put("novoEfetivoTotal", 12);
        req.put("alteracoesComposicao", List.of(mapa("lotacaoId", o.c().lotacaoA.getId(),
                "jornadaId", o.c().jornadaA.getId(), "cargoId", cargo, "quantidadeResultante", 2)));
        JsonNode adi = registrar(o, req);
        long item = emTransacao(() -> em.createQuery("select i.id from AditamentoComposicao i where i.aditamento.id=:id", Long.class)
                .setParameter("id", adi.path("id").longValue()).getSingleResult());
        Instant k = Instant.now();
        JsonNode anterior = posicao(o, dataAditamento.minusDays(1), k).path("versaoContratual");
        JsonNode atual = posicao(o, dataAditamento, k).path("versaoContratual");
        JsonNode nova = porCampo(composicoes(o, dataAditamento, k), "cargoId", cargo);

        assertAll("D aplica somente deltas pertinentes, sem usar o ID de uma versão como ID projetado",
                () -> assertThat(anterior.path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP"),
                () -> origemContem(anterior, "descricao", "versaoId", o.versao()),
                () -> assertThat(atual.path("descricao").asText()).isEqualTo("Descrição que só vale a partir do ADI"),
                () -> origemContem(atual, "descricao", "eventoId", adi.path("eventoId").longValue()),
                () -> origemContem(atual, "numeroContrato", "versaoId", o.versao()),
                () -> assertThat(atual.path("id").isNull()).isTrue(),
                () -> assertThat(nova.path("id").isNull()).isTrue(),
                () -> assertThat(nova.path("efetivoPrevisto").intValue()).isEqualTo(2),
                () -> estado(nova, "cargoNome", "VALOR_COMPROVADO"),
                () -> origemContem(nova, "cargoNome", "aditamentoComposicaoId", item));
    }

    @Test
    void fonteRestritaNaoVazaPorMetadataENaoViraDesconhecimentoFactual() throws Exception {
        Origem o = origem(false);
        conceder(o.c(), o.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode recibo = incorporar(o, fonte(o, null, List.of("descricao"), Map.of(), List.of()));
        for (UsuarioAplicacao leitor : List.of(o.c().localA, o.c().nacional, o.c().presidente)) {
            JsonNode prova = consultar(leitor, base(o.n().id()) + "/provas-historicas", "Fonte mantém sua proteção documental").path(0);
            for (String campo : METADADOS_RESTRITOS) {
                JsonNode metadata = prova.path("provaCampos").path(campo);
                assertThat(prova.has(campo)).as("Escalar restrito " + campo).isFalse();
                assertThat(metadata.path("visibilidade").asText()).isEqualTo("RESTRITA");
                assertThat(metadata.path("estado").isNull()).as("Restrição não é conclusão factual").isTrue();
                assertThat(metadata.path("origens")).isEmpty();
            }
            assertThat(prova.toString()).doesNotContain(SEI_RESTRITO, MOTIVO_RESTRITO, "ATO-IND08-RESTRITO");
            assertThat(prova.path("id").longValue()).isEqualTo(recibo.path("id").longValue());
        }
        executar(o.c().localB, get(base(o.n().id()) + "/provas-historicas"), 404, "Outra Gestão não enumera provas");
        executar(o.c().localB, get(base(o.n().id()) + "/posicao-temporal").param("data", o.n().inicio().toString()),
                404, "Metadata não amplia o escopo contratual");
    }

    @Test
    void leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha() throws Exception {
        Origem o = origem(false);
        conceder(o.c(), o.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode prova = incorporar(o, fonte(o, null, List.of("descricao"), Map.of(), List.of()));
        long provaId = prova.path("id").longValue();
        assertThat(emTransacao(() -> em.find(ProvaHistoricaContrato.class, provaId).getNumeroRevisao()))
                .as("A fonte canônica criou a primeira revisão válida antes da simulação")
                .isEqualTo(1);
        assertThatThrownBy(() -> emTransacao(() -> {
            // Apenas interpretar: se a proteção regredir, esta contraprova não
            // executa DML nem deixa dano artificial antes do finally abaixo.
            em.createQuery("update ProvaHistoricaContrato p set p.numeroRevisao=3 where p.id=:id");
            return null;
        })).as("A entidade de produção continua recusando atualização HQL de fonte imutável")
                .isInstanceOf(org.hibernate.query.sqm.InterpretationException.class)
                .hasMessageContaining("immutable entity");
        boolean mutacaoConfirmada = false;
        try {
            emTransacao(() -> {
                assertThat(em.createQuery("update ProvaHistoricaContratoFalhaFixture p set p.numeroRevisao=3 "
                                + "where p.id=:id and p.contratoId=:contrato and p.eventoOrigemId=:evento "
                                + "and p.numeroRevisao=1 and p.provaAnteriorId is null")
                        .setParameter("id", provaId).setParameter("contrato", o.n().id())
                        .setParameter("evento", o.evento()).executeUpdate()).isEqualTo(1);
                return null;
            });
            mutacaoConfirmada = true;
            executar(o.c().localA, get(base(o.n().id()) + "/provas-historicas"), 409,
                    "Trilha incompleta não pode fornecer proveniência de campo");
            executar(o.c().localA, get(base(o.n().id()) + "/versoes"), 409,
                    "Acervo não certifica fotografia gerada de cadeia divergente");
            executar(o.c().nacional, get("/api/contratos"), 409,
                    "A listagem nacional também recusa a cadeia enquanto está inválida");
        } finally {
            if (mutacaoConfirmada) {
                // Desfaz somente o dano artificial deste controle negativo. Não apaga
                // a prova ou seus fatos e não altera a política de imutabilidade.
                emTransacao(() -> {
                    assertThat(em.createQuery("update ProvaHistoricaContratoFalhaFixture p set p.numeroRevisao=1 "
                                    + "where p.id=:id and p.contratoId=:contrato and p.eventoOrigemId=:evento "
                                    + "and p.numeroRevisao=3 and p.provaAnteriorId is null")
                            .setParameter("id", provaId).setParameter("contrato", o.n().id())
                            .setParameter("evento", o.evento()).executeUpdate())
                            .as("Restaurar exclusivamente a revisão sintética alterada pelo próprio teste")
                            .isEqualTo(1);
                    return null;
                });
            }
        }
        assertThat(emTransacao(() -> em.find(ProvaHistoricaContrato.class, provaId).getNumeroRevisao()))
                .as("Nova transação lê a revisão original preservada")
                .isEqualTo(1);
        executar(o.c().nacional, get("/api/contratos"), 200,
                "A listagem nacional volta a ler a prova válida após desfazer a simulação");
    }

    /** Regressão de isolamento: usa o controle negativo real e outra requisição nacional. */
    @Test
    void controleNegativoDeCadeiaNaoContaminaListagemNacionalPosterior() throws Exception {
        Cenario leitor = criarCenarioPersistido();
        executar(leitor.nacional, get("/api/contratos"), 200,
                "Controle inicial: o ambiente já contém somente cadeias legíveis");
        List<Map<String, Object>> antes = diagnosticarProvasPersistidas();
        try {
            leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha();
        } finally {
            List<Map<String, Object>> depois = diagnosticarProvasPersistidas();
            java.util.Set<Object> idsAntes = antes.stream().map(p -> p.get("provaId"))
                    .collect(java.util.stream.Collectors.toSet());
            List<Map<String, Object>> novas = depois.stream()
                    .filter(p -> !idsAntes.contains(p.get("provaId"))).toList();
            Path arquivo = Path.of("target/auditoria-http/ind08-isolamento-cadeia.json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "observadoEm", Instant.now(), "testeCriador",
                    "Ind08LeituraHistoricaCampoHttpIT.leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha",
                    "provasAntes", antes, "provasDepois", depois, "provasCriadasPeloControle", novas,
                    "primeiraRecusaHabilitacao", diagnosticarPrimeiraRecusaNacional())));
        }
        executar(leitor.nacional, get("/api/contratos"), 200,
                "Regressão: o controle negativo não deixa corrupção sintética para outra requisição");
    }

    private List<Map<String, Object>> diagnosticarProvasPersistidas() {
        return emTransacao(() -> em.createQuery(
                        "select p from ProvaHistoricaContrato p order by p.contrato.id, p.eventoOrigem.id, p.numeroRevisao, p.id",
                        ProvaHistoricaContrato.class).getResultList().stream().map(p -> mapa(
                        "provaId", p.getId(), "contratoId", p.getContrato().getId(),
                        "eventoOrigemId", p.getEventoOrigem().getId(), "numeroRevisao", p.getNumeroRevisao(),
                        "provaAnteriorId", p.getProvaAnterior() == null ? null : p.getProvaAnterior().getId(),
                        "registradoEm", p.getRegistradoEm())).toList());
    }

    private Map<String, Object> diagnosticarPrimeiraRecusaNacional() {
        Instant conhecido = Instant.now();
        List<Long> ids = emTransacao(() -> em.createQuery(
                "select c.id from Contrato c order by c.numeroContrato asc", Long.class).getResultList());
        for (Long id : ids) {
            try {
                emTransacao(() -> motor.habilitacaoAdministrativaEm(
                        em.find(Contrato.class, id), RelogioSistema.hoje(), conhecido));
            } catch (org.springframework.web.server.ResponseStatusException erro) {
                return mapa("contratoId", id, "status", erro.getStatusCode().value(),
                        "mensagem", erro.getReason(), "conhecidoAte", conhecido,
                        "stackAplicacao", java.util.Arrays.stream(erro.getStackTrace())
                                .filter(s -> s.getClassName().startsWith("com.example.siggtrat."))
                                .map(StackTraceElement::toString).toList());
            }
        }
        return null;
    }

    private Origem origem(boolean comZero) {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, comZero ? 10 : null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        return emTransacao(() -> {
            var v = motor.fotografiaOriginalConhecida(em.find(Contrato.class, n.id()), Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
            for (String metadado : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(metadado);
            return new Origem(c, n, v.getId(), v.getEventoOrigem().getId(), foto);
        });
    }

    private Map<String, Object> fonte(Origem o, JsonNode anterior, List<String> campos,
                                      Map<String, Object> valores, List<Map<String, Object>> itens) {
        Map<String, Object> foto = new LinkedHashMap<>(o.fotografia()); foto.putAll(valores);
        return mapa("eventoOrigemId", o.evento(), "versaoAnteriorId", anterior == null ? o.versao() : anterior.path("versaoGeradaId").longValue(),
                "provaAnteriorId", anterior == null ? null : anterior.path("id").longValue(),
                "dataDocumento", o.n().inicio(), "referenciaSei", SEI_RESTRITO,
                "motivo", MOTIVO_RESTRITO, "autorizacaoAdministrativa", "ATO-IND08-RESTRITO",
                "versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, o.n().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", campos, "fotografia", foto, "composicoes", itens);
    }

    private JsonNode incorporar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/provas-historicas";
        JsonNode previa = enviar(o.c().localA, rota + "/previa", req, 200, "Prévia documental IND08");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(o.c().localA, rota, req, 201, "Fonte sintética com máscara explícita incorporada");
    }

    private JsonNode registrar(Origem o, Map<String, Object> req) throws Exception {
        String rota = base(o.n().id()) + "/aditamentos";
        JsonNode previa = enviar(o.c().localA, rota + "/previa", req, 200, "Prévia canônica do ADI IND08");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(o.c().localA, rota, req, 201, "ADI aplica somente seus próprios deltas");
    }

    private JsonNode posicao(Origem o, LocalDate d, Instant k) throws Exception {
        var req = get(base(o.n().id()) + "/posicao-temporal").param("data", d.toString());
        if (k != null) req.param("conhecidoAte", k.toString());
        return dados(executar(o.c().localA, req, 200, "Posição no mesmo D/K da evidência por campo"));
    }

    private JsonNode composicoes(Origem o, LocalDate d, Instant k) throws Exception {
        var req = get(base(o.n().id()) + "/composicoes").param("data", d.toString());
        if (k != null) req.param("conhecidoAte", k.toString());
        return dados(executar(o.c().localA, req, 200, "Composição respeita o recorte explícito"));
    }

    private JsonNode versoes(Origem o, Instant k) throws Exception {
        var req = get(base(o.n().id()) + "/versoes");
        if (k != null) req.param("conhecidoAte", k.toString());
        return dados(executar(o.c().localA, req, 200, "Acervo conserva cada fotografia e seu conhecimento"));
    }

    private JsonNode provas(Origem o, Instant k) throws Exception {
        return dados(executar(o.c().localA, get(base(o.n().id()) + "/provas-historicas")
                .param("conhecidoAte", k.toString()), 200, "Cadeia documental limitada ao corte K"));
    }

    private Instant instante(JsonNode prova) { return Instant.parse(prova.path("registradoEm").asText()); }
    private JsonNode versaoPorId(JsonNode lista, long id) { return porCampo(lista, "id", id); }
    private JsonNode itemPorId(JsonNode lista, long id) { return porCampo(lista, "id", id); }
    private JsonNode porCampo(JsonNode lista, String campo, long id) {
        for (JsonNode item : lista) if (item.path(campo).isNumber() && item.path(campo).longValue() == id) return item;
        throw new AssertionError("Registro canônico ausente no resultado: " + campo + "=" + id);
    }

    private void estado(JsonNode objeto, String campo, String esperado) {
        JsonNode prova = objeto.path("provaCampos").path(campo);
        assertThat(prova.path("visibilidade").asText()).as("Visibilidade de " + campo).isEqualTo("DISPONIVEL");
        assertThat(prova.path("estado").asText()).as("Comprovação independente de " + campo).isEqualTo(esperado);
        if ("NAO_COMPROVADO".equals(esperado)) assertThat(prova.path("origens")).isEmpty();
        else assertThat(prova.path("origens")).isNotEmpty();
    }

    private void origemContem(JsonNode objeto, String campo, String referencia, long id) {
        boolean encontrada = false;
        for (JsonNode origem : objeto.path("provaCampos").path(campo).path("origens")) {
            if (origem.path(referencia).isNumber() && origem.path(referencia).longValue() == id) encontrada = true;
        }
        assertThat(encontrada).as("Origem real de " + campo + ": " + referencia + "=" + id).isTrue();
    }
}
