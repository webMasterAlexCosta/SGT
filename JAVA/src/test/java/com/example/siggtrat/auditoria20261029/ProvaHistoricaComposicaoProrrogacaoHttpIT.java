package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Proposta independente; o revisor NÃO compilou nem executou este caso.
 * Oráculo: LEVANTAMENTO_REQUISITOS.md, §5.4/CON-008, TMP-004/TMP-015,
 * HIS-001/HIS-004/HIS-006. A prorrogação tempestiva do mesmo contrato mantém
 * continuidade; a fonte da composição original continua pertinente ao mesmo
 * recorte, sem estender a alocação além da vigência contratual reconstruída.
 *
 * Variantes: (false) fonte comprova só a grade, mantendo município original já
 * fotografado; (true) fonte também declara esse mesmo município explicitamente.
 * A primeira verifica conhecimento por propriedade; a segunda isola o guard
 * temporal após o fim original. Nenhum oráculo é relaxado entre variantes.
 * PDF e perda de colunas são fixtures sintéticas explícitas.
 * Não cria DDL. MockMvc não substitui o gate externo TCP/JAR/Redis.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaComposicaoProrrogacaoHttpIT extends ContratosHttpBase {
    private static final String GRADE = "MONDAY|true|08:00|12:00|13:00|17:00;"
            + "TUESDAY|true|08:00|12:00|13:00|17:00;WEDNESDAY|true|08:00|12:00|13:00|17:00;"
            + "THURSDAY|true|08:00|12:00|13:00|17:00;FRIDAY|true|08:00|12:00|13:00|17:00;"
            + "SATURDAY|false||||;SUNDAY|false||||";
    private static byte[] fonteSintetica(String municipioComprovado) {
        return ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
                + "% FONTE SINTETICA: grade original, preservada na continuidade do mesmo recorte.\n"
                + "% " + GRADE + "\n"
                + (municipioComprovado == null ? ""
                        : "% MUNICIPIO ORIGINAL EXPRESSAMENTE COMPROVADO: " + municipioComprovado + "\n")
                + "%%EOF").getBytes(StandardCharsets.UTF_8);
    }

    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicaoTemporal;
    @Autowired ResolvedorContextoTemporalFrequenciaServico resolvedor;

    @BeforeEach
    void exigirPersistenciaPermitida() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            var meta = conexao.getMetaData();
            assertThat(meta.getDatabaseProductName()).isEqualToIgnoringCase("MySQL");
            assertThat(meta.getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @ParameterizedTest(name = "município explicitamente comprovado na fonte = {0}")
    @ValueSource(booleans = {false, true})
    void provaDaComposicaoOriginalAlcancaAlocacaoContinuadaNaVigenciaProrrogada(boolean comprovarMunicipio) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", RelogioSistema.hoje().minusDays(1),
                "pessoa", mapa("nome", "Pessoa sintética com continuidade na prorrogação",
                        "cpf", cpf(Math.toIntExact(730000000L + n.id())))), 201,
                "Vínculo sem desligamento humano, alocado até o limite original").path("id").asLong();
        LocalDate fimProrrogado = n.fim().plusDays(30);
        LocalDate diaContinuado = n.fim().plusDays(1);
        var adi = aditamento("ADI-CONTINUIDADE-" + n.id(), RelogioSistema.hoje());
        adi.put("novaDataFim", fimProrrogado);
        String rotaAdi = base(n.id()) + "/aditamentos";
        adi.put("tokenPrevia", enviar(c.localA, rotaAdi + "/previa", adi, 200,
                "Prévia da prorrogação tempestiva").path("tokenPrevia").asText());
        enviar(c.localA, rotaAdi, adi, 201, "Prorrogação mantém continuidade do vínculo e da alocação");

        Map<String, Object> req = emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var original = motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow();
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var a = alocacao(vinculo);
            assertThat(comp.getDataFimVigencia()).isEqualTo(n.fim());
            assertThat(a.getDataFim()).isEqualTo(fimProrrogado);
            assertThat(a.getLotacaoMunicipioFotografia()).isNotBlank();
            assertThat(comp.getJornadaSemanalFotografia()).isEqualTo(GRADE);
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(GRADE);
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(original), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
            // Simulação explícita de perda antiga, sem modificar limites temporais.
            assertThat(em.createQuery("update ComposicaoContratual x set x.jornadaSemanalFotografia=null where x.id=:id")
                    .setParameter("id", comp.getId()).executeUpdate()).isEqualTo(1);
            assertThat(em.createQuery("update AlocacaoVinculo x set x.jornadaSemanalFotografia=null where x.id=:id")
                    .setParameter("id", a.getId()).executeUpdate()).isEqualTo(1);
            // Mestre atual divergente impede que o teste passe por recaptura.
            for (var jornadaDia : em.createQuery("select d from JornadaDiaSemana d where d.jornada.id=:id", JornadaDiaSemana.class)
                    .setParameter("id", c.jornadaA.getId()).getResultList()) {
                if (Boolean.TRUE.equals(jornadaDia.getDiaTrabalho())) jornadaDia.setSaida2("16:00");
            }
            var item = mapa("composicaoId", comp.getId(), "jornadaComprovada", true,
                    "jornadaSemanal", GRADE, "municipioComprovado", comprovarMunicipio);
            if (comprovarMunicipio) item.put("lotacaoMunicipio", comp.getLotacaoMunicipioFotografia());
            return mapa("eventoOrigemId", original.getEventoOrigem().getId(), "versaoAnteriorId", original.getId(),
                    "dataDocumento", n.inicio(), "motivo", "Conferência sintética da grade original após perda parcial",
                    "autorizacaoAdministrativa", "ATO-HTTP", "chaveIdempotencia", UUID.randomUUID().toString(),
                    "configuracaoCompletaComprovada", false, "camposComprovados", List.of(), "fotografia", foto,
                    "composicoes", List.of(item));
        });
        long versaoOriginalId = ((Number) req.get("versaoAnteriorId")).longValue();
        String originais = originais(n, vinculo, versaoOriginalId);
        @SuppressWarnings("unchecked")
        Map<String, Object> itemFonte = (Map<String, Object>) ((List<?>) req.get("composicoes")).get(0);
        byte[] pdf = fonteSintetica(comprovarMunicipio ? (String) itemFonte.get("lotacaoMunicipio") : null);
        JsonNode documento = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", req.get("eventoOrigemId"), "fatoChave", "ORIGINAL",
                        "titulo", "Fonte sintética original da continuidade", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", n.inicio(), "chaveIdempotencia", UUID.randomUUID().toString())))
                .file(new MockMultipartFile("arquivo", "fonte-sintetica-continuidade.pdf", "application/pdf", pdf)),
                201, "Anexo sintético do contrato original"));
        assertThat(documento.path("estado").asText()).isEqualTo("PUBLICADO");
        long documentoId = documento.path("id").asLong();
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        req.put("documentoComprobatorioId", documentoId);
        req.put("finalidadeAcessoDocumento", "Conferir grade sintética original");
        req.put("versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, n.id()).getVersion()));
        Instant k0 = Instant.now();
        assertThat(emTransacao(() -> resolvedor.resolverConfiguracao(alocacao(vinculo), diaContinuado, k0).jornadaSemanal())).isNull();
        String rota = base(n.id()) + "/provas-historicas";
        req.put("tokenPrevia", enviar(c.localA, rota + "/previa", req, 200, "Conferir fonte exata da composição-base").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        JsonNode prova = enviar(c.localA, rota, req, 201, "Conhecimento novo sem reescrever originais");
        Instant k1 = Instant.parse(prova.path("registradoEm").asText());

        // Controles positivos separam disponibilidade de fonte e continuidade do guard suspeito.
        emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var posicao = motor.reconstruirPosicao(contrato, diaContinuado, k1);
            assertThat(posicao.situacaoTemporal().name()).isEqualTo("VIGENTE");
            assertThat(posicao.vigenciaFim()).isEqualTo(fimProrrogado);
            var projetada = composicaoTemporal.projetarEntidades(contrato, diaContinuado, k1).stream()
                    .filter(p -> Long.valueOf(n.composicao1()).equals(p.composicaoBaseId())).findFirst().orElseThrow();
            assertThat(projetada.dataFim()).isEqualTo(n.fim());
            assertThat(projetada.jornadaSemanal()).isEqualTo(GRADE);
            assertThat(resolvedor.resolverConfiguracao(alocacao(vinculo), diaContinuado, k0).jornadaSemanal()).isNull();
            assertThat(resolvedor.resolverPorVinculo(vinculo, diaContinuado)).isNotNull();
            return null;
        });
        assertThat(originais(n, vinculo, versaoOriginalId)).isEqualTo(originais);
        assertThat(executar(c.localA, get(base(n.id()) + "/documentos/" + documentoId + "/arquivo")
                .param("finalidade", "Conferir fonte sintética preservada"), 200, "Fonte imutável")
                .getResponse().getContentAsByteArray()).isEqualTo(pdf);
        String gradeNoFimOriginal = emTransacao(() ->
                resolvedor.resolverConfiguracao(alocacao(vinculo), n.fim(), k1).jornadaSemanal());
        String gradeNaProrrogacao = emTransacao(() ->
                resolvedor.resolverConfiguracao(alocacao(vinculo), diaContinuado, k1).jornadaSemanal());
        // Os dois resultados são verificados em ambas as variantes; todos os controles
        // de cobertura, corte K, originais e bytes da fonte ocorreram antes deste bloco.
        assertAll(
                () -> assertThat(gradeNoFimOriginal)
                        .as("grade documental complementa lacuna antes do fim original; município já era conhecido")
                        .isEqualTo(GRADE),
                () -> assertThat(gradeNaProrrogacao)
                        .as("mesmo contrato, mesmo recorte e alocação legitimamente prorrogada")
                        .isEqualTo(GRADE));
    }

    private AlocacaoVinculo alocacao(long vinculo) {
        return em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                .setParameter("v", vinculo).getSingleResult();
    }

    private String originais(NovoContrato n, long vinculo, long versao) {
        return emTransacao(() -> {
            var a = alocacao(vinculo);
            var c = em.find(ComposicaoContratual.class, n.composicao1());
            return mapper.valueToTree(mapa("versao", motor.paraVersaoDTO(em.find(VersaoContratoNegocial.class, versao)),
                    "composicaoId", c.getId(), "composicaoInicio", c.getDataInicioVigencia(), "composicaoFim", c.getDataFimVigencia(),
                    "composicaoGrade", c.getJornadaSemanalFotografia(), "composicaoMunicipio", c.getLotacaoMunicipioFotografia(),
                    "alocacaoId", a.getId(), "inicio", a.getDataInicio(), "fim", a.getDataFim(),
                    "grade", a.getJornadaSemanalFotografia(), "municipio", a.getLotacaoMunicipioFotografia())).toString();
        });
    }
}
