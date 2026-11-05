package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Pessoa;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** RI07: nome historico vem de prova do periodo, inclusive na representacao exportada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatorioNomeLegadoDesconhecidoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo"})
    void relatorioEPlanilhaNaoSubstituemNomeHistoricoAusentePeloCadastroAtual(String tipo) throws Exception {
        conferirNomeHistorico(tipo, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo"})
    void relatorioEPlanilhaPreservamNomeComprovadoNaFrequenciaAposCorrecaoAtual(String tipo) throws Exception {
        conferirNomeHistorico(tipo, true);
    }

    private void conferirNomeHistorico(String tipo, boolean fotografiaComprovada) throws Exception {
        Cenario c = criarCenarioPersistido();
        String foto = fotografiaComprovada ? c.pessoaA.getNome() : null;
        if (fotografiaComprovada) declararProvaNominalSintetica(c, tipo, foto);
        long frequenciaId = fotografiaComprovada
                ? enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                        "dataReferencia", DATA, "status", "FALTA",
                        "justificativaRetificacao", "Falta documentada com fotografia nominal comprovada",
                        "chaveIdempotencia", "nome-relatorio-" + UUID.randomUUID()),
                        200, "Frequencia publicada fotografa o nome conhecido naquele registro").path("id").asLong()
                : criarFrequenciaLegadaSemNome(c);
        emTransacao(() -> {
            assertEquals(foto, em.find(Frequencia.class, frequenciaId).getEmpregadoSnapshotNome());
            assertEquals(0L, em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult(),
                    "Legado tem identidade e alocacao comprovadas, mas nenhum evento com fotografia nominal do vinculo");
            return null;
        });
        String atual = "NOME_ATUAL_RELATORIO_" + UUID.randomUUID();
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", atual, "motivo", "Correcao legal do cadastro atual",
                "evidencia", "SEI-NOME-RELATORIO", "autorizacao", "Despacho documentado da Gestao Local",
                "impactosEncontrados", "Preservar fotos existentes e ausencia de prova nominal historica"),
                200, "Correcao de identidade nao autoriza preencher nome historico desconhecido");
        emTransacao(() -> {
            assertEquals(atual, em.find(Pessoa.class, c.pessoaA.getId()).getNome());
            assertEquals(atual, em.find(Empregado.class, c.empregadoA.getId()).getNome());
            return null;
        });

        JsonNode resposta = dados(executar(c.localA,
                parametros(get("/api/relatorios/" + tipo), tipo, c), 200,
                "Relatorio historico usa a prova nominal disponivel, sem completar pelo cadastro atual"));
        JsonNode registros = tipo.equals("frequencia-consolidada") ? resposta.path("registros") : resposta;
        assertEquals(1, registros.size(), "A fonte continua consultavel no seu escopo comprovado");
        JsonNode registro = registros.get(0);
        assertEquals(c.empregadoA.getId().longValue(), registro.path("empregadoId").asLong());
        JsonNode faltas = tipo.equals("frequencia-consolidada") ? registro.path("totais").path("faltas") : registro.path("faltas");
        assertEquals(1, faltas.asInt(), "A falta real precisa compor o relatorio, sem resposta vazia para ocultar a falta de foto");

        MvcResult download = executar(c.localA,
                parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c).param("formato", "XLSX"),
                200, "Exportacao do mesmo relatorio trata nome historico desconhecido com seguranca");
        List<String> nomesNaPlanilha = nomesDaLinhaXlsx(download, c.cargoA.getNome(), atual);
        assertEquals(1, nomesNaPlanilha.size(), "A planilha deve conter a linha de dados real do colaborador");
        assertAll(
                () -> assertEquals(foto, registro.path("nome").isNull() ? null : registro.path("nome").asText(), registro.toString()),
                () -> assertEquals(foto == null ? "-" : foto, nomesNaPlanilha.get(0)),
                () -> assertFalse(resposta.toString().contains(atual), "Cadastro atual nao substitui fotografia do periodo"),
                () -> emTransacao(() -> {
                    assertEquals(foto, em.find(Frequencia.class, frequenciaId).getEmpregadoSnapshotNome(),
                            "Consulta e exportacao nao fabricam nem alteram a fotografia persistida");
                    return null;
                }));
    }

    /** Fonte opt-in dos positivos; não acrescenta evento original ao vínculo legado. */
    private void declararProvaNominalSintetica(Cenario c, String tipo, String nome) throws Exception {
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        // Absenteísmo nomeia uma linha mensal: todos os dias apurados precisam da
        // mesma prova. A hipótese negativa continua sem qualquer fonte nominal.
        java.time.LocalDate inicio = tipo.equals("absenteismo") ? DATA.withDayOfMonth(1) : DATA;
        java.time.LocalDate fim = tipo.equals("absenteismo")
                ? DATA.withDayOfMonth(DATA.lengthOfMonth()) : DATA;
        long pessoaVersao = consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                "Versão da Pessoa antes da fonte nominal sintética").path("version").asLong();
        long vinculoVersao = consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(),
                "Versão do Vínculo antes da fonte nominal sintética").path("version").asLong();
        java.util.Map<String,Object> req = mapa("vinculoId", c.vinculoA.getId(),
                "dataInicioEfetiva", inicio, "dataFimEfetiva", fim, "nomeComprovado", nome,
                "referenciaSei", "SEI-SINTETICO-RELATORIO-NOME-" + UUID.randomUUID(),
                "motivo", "Fonte sintética explícita do nome no intervalo deste controle positivo de relatório",
                "autorizacao", "ATO-HTTP", "versaoPessoaEsperada", pessoaVersao,
                "versaoVinculoEsperada", vinculoVersao, "chaveIdempotencia", UUID.randomUUID().toString());
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome";
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200,
                "Prévia da fonte nominal sintética somente do controle positivo");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        JsonNode prova = enviar(c.localA, rota, req, 201,
                "Prova independente permite fotografar nome passado sem fabricar evento de admissão");
        emTransacao(() -> {
            var fonte = em.find(com.example.siggtrat.entidade.ProvaNomePessoaHistorico.class,
                    prova.path("id").asLong());
            assertNotNull(fonte);
            assertEquals(c.pessoaA.getId(), fonte.getPessoa().getId());
            assertEquals(c.vinculoA.getId(), fonte.getVinculo().getId());
            assertEquals(nome, fonte.getNomeComprovado());
            assertEquals(inicio, fonte.getDataInicioEfetiva());
            assertEquals(fim, fonte.getDataFimEfetiva());
            assertNotNull(fonte.getRegistradoEm());
            assertEquals(0L, em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult(),
                    "Fonte independente não reescreve a origem legada nem o oráculo de zero eventos");
            return null;
        });
    }

    private long criarFrequenciaLegadaSemNome(Cenario c) {
        return emTransacao(() -> {
            Frequencia f = frequencia(c, false, DATA, "FALTA");
            f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            f.setEmpregadoSnapshotNome(null);
            f.setJornadaSnapshotDescricao(c.jornadaA.getDescricao()); f.setJornadaSnapshotDiaTrabalho(true);
            f.setJornadaSnapshotEntrada1("08:00"); f.setJornadaSnapshotSaida1("12:00");
            f.setJornadaSnapshotEntrada2("13:00"); f.setJornadaSnapshotSaida2("17:00");
            f.setLotacaoSnapshotNome(c.lotacaoA.getNome()); f.setLotacaoSnapshotMunicipio(c.lotacaoA.getMunicipio());
            f.setCargoSnapshotNome(c.cargoA.getNome()); f.setContratoSnapshotNumero(c.contratoA.getNumeroContrato());
            f.setMinutosFisicos(0); f.setMinutosNoturnosReais(0); f.setMinutosAdicionais(0); f.setMinutosPagaveis(0);
            em.flush(); return f.getId();
        });
    }

    private MockHttpServletRequestBuilder parametros(MockHttpServletRequestBuilder r, String tipo, Cenario c) {
        if (tipo.equals("frequencia-consolidada")) {
            return r.param("inicio", DATA.toString()).param("fim", DATA.toString())
                    .param("contratoIds", c.contratoA.getId().toString());
        }
        return r.param("mes", String.valueOf(DATA.getMonthValue())).param("ano", String.valueOf(DATA.getYear()))
                .param("contratoId", c.contratoA.getId().toString());
    }

    private List<String> nomesDaLinhaXlsx(MvcResult download, String cargo, String nomeAtual) throws Exception {
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", download.getResponse().getContentType());
        assertTrue(download.getResponse().getHeader("Content-Disposition").contains(".xlsx"));
        List<String> nomes = new ArrayList<>();
        boolean workbook = false;
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(download.getResponse().getContentAsByteArray()))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (entry.getName().equals("xl/workbook.xml")) workbook = true;
                if (!entry.getName().startsWith("xl/worksheets/") || !entry.getName().endsWith(".xml")) continue;
                byte[] xml = zip.readAllBytes();
                assertFalse(new String(xml, StandardCharsets.UTF_8).contains(nomeAtual),
                        "Formatacao XLSX nao deve reintroduzir o nome atual");
                var documento = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
                var linhas = documento.getElementsByTagNameNS("*", "row");
                for (int i = 0; i < linhas.getLength(); i++) {
                    var celulas = ((Element) linhas.item(i)).getElementsByTagNameNS("*", "c");
                    boolean contemCargo = false;
                    String nome = null;
                    for (int j = 0; j < celulas.getLength(); j++) {
                        Element celula = (Element) celulas.item(j);
                        if (cargo.equals(celula.getTextContent())) contemCargo = true;
                        if (celula.getAttribute("r").matches("A\\d+")) nome = celula.getTextContent();
                    }
                    if (contemCargo) nomes.add(nome);
                }
            }
        }
        assertTrue(workbook, "Download contem um XLSX valido");
        return nomes;
    }
}
