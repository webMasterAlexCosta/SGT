package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.AutorizacaoTemporalOcorrenciaServico;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * PER-010:004 / H04. MockMvc com filtros reais e MySQL da DataSource da aplicação;
 * cada requisição confirma sua transação e os snapshots são relidos depois.
 * Não representa navegador, conexão TCP HTTP nem uma implantação do JAR.
 * A única retirada de fonte é a fixture legada sintética e explícita da parte B.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PermissaoOcorrenciaMultidiasSemOrigemH04HttpIT extends ContratosHttpBase {
    @Autowired GestaoContratoTemporalServico gestoes;
    @Autowired AutorizacaoTemporalOcorrenciaServico autorizacao;
    @Autowired HistoricoIntegridadeServico integridade;
    @Autowired PropriedadesArmazenamentoArquivo armazenamento;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.example.siggtrat.servico.MotorTemporalContratoServico motorContrato;
    @Autowired com.example.siggtrat.servico.MotorTemporalVinculoServico motorVinculo;
    @Autowired com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico contextoOperacional;

    private static final String FINALIDADE = "Conferência sintética PER010004 da prova de afastamento";
    private static final String NARRATIVA = "Narrativa pessoal SINTETICA PER010004 reservada";
    private static final byte[] DOCUMENTO =
            "Documento pessoal SINTETICO PER010004; bytes integrais e não vazios.\n".getBytes(StandardCharsets.UTF_8);
    private static final String SEM_CONTEXTO = "Sem permissão no contexto histórico comprovado da ocorrência.";

    @Test
    void concessoesSeparadasDeAEBDevemCobrirTodosOsDiasSemConsumoParcialNaRecusa() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "PER010004-MULTIDIAS");
        LocalDate d1 = DATA, d2 = DATA.plusDays(1);
        assertTrue(d2.isBefore(RelogioSistema.hoje()));
        JsonNode criada = criar(c, c.localA, d1, 2, "A-origem");
        long ocorrencia = criada.path("id").asLong();
        long anexo = criada.path("anexos").get(0).path("id").asLong();
        JsonNode fotoOriginal = fotografia(ocorrencia);
        assertEquals(2, fotoOriginal.path("contextos").size());
        for (JsonNode contexto : fotoOriginal.path("contextos"))
            assertEquals(c.gestaoA.getId().longValue(), contexto.path("gestao_local_id").asLong());
        verificarArquivo(anexo);
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());

        transferir(c, d2, "A-multidias");
        assertEquals(c.gestaoB.getId(), gestaoAtual(c));
        assertEquals(c.gestaoA.getId(), gestao(c, d1));
        assertEquals(c.gestaoB.getId(), gestao(c, d2));
        JsonNode contextosEfetivos = emTransacao(() -> mapper.valueToTree(
                autorizacao.resolver(em.find(Ocorrencia.class, ocorrencia))));
        assertEquals(2, contextosEfetivos.size());
        assertEquals(c.gestaoA.getId().longValue(), contextosEfetivos.get(0).path("gestaoId").asLong());
        assertEquals(c.gestaoB.getId().longValue(), contextosEfetivos.get(1).path("gestaoId").asLong());
        assertEquals(fotoOriginal, fotografia(ocorrencia), "A correção temporal não reescreve a fotografia original");
        registrar(c, "A-contextos", mapa("data1", d1, "data2", d2, "gestaoAtual", gestaoAtual(c),
                "contextosEfetivos", contextosEfetivos, "fotografiaOriginal", fotoOriginal));

        negarSemEfeito(c, c.localA, get("/api/ocorrencias/{id}", ocorrencia), 403, SEM_CONTEXTO, "A-local-A");
        negarSemEfeito(c, c.localB, get("/api/ocorrencias/{id}", ocorrencia), 403, SEM_CONTEXTO, "A-local-B");
        verificarPaginas(c, c.localA, d1, 0, ocorrencia, "A-lista-local-A");
        verificarPaginas(c, c.localB, d1, 0, ocorrencia, "A-lista-local-B");
        verificarPaginas(c, c.nacional, d1, 1, ocorrencia, "A-lista-nacional");
        JsonNode minimizado = dados(executar(c.nacional, get("/api/ocorrencias/{id}", ocorrencia), 200,
                "PER010004 A leitor global consulta somente representação minimizada"));
        assertTrue(minimizado.path("ocorrencia").path("observacao").isNull());
        assertTrue(minimizado.path("ocorrencia").path("anexos").isEmpty());
        assertFalse(minimizado.toString().contains(NARRATIVA));
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "finalidade e concessão específica vigente", "A-arquivo-sem-concessao");

        JsonNode pa = concessaoGestao(c, c.gestaoA.getId(), "A-concessao-A");
        long permissaoA = pa.path("id").asLong();
        assertTrue(pa.path("contratoId").isNull());
        assertEquals(c.gestaoA.getId().longValue(), pa.path("gestaoLocalId").asLong());
        assertEquals(0, usos(c, permissaoA).size());
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/{id}", ocorrencia).param("finalidade", FINALIDADE),
                403, "permissão excepcional específica", "A-detalhe-so-A");
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                403, "permissão excepcional específica", "A-arquivo-so-A");
        assertEquals(0, usos(c, permissaoA).size(), "Falha em B deve reverter o consumo intermediário de A");

        JsonNode pb = concessaoGestao(c, c.gestaoB.getId(), "A-concessao-B");
        long permissaoB = pb.path("id").asLong();
        assertTrue(pb.path("contratoId").isNull());
        assertEquals(c.gestaoB.getId().longValue(), pb.path("gestaoLocalId").asLong());
        Instant inicioUsos = Instant.now();
        JsonNode integral = dados(executar(c.nacional, get("/api/ocorrencias/{id}", ocorrencia)
                .param("finalidade", FINALIDADE), 200, "PER010004 A detalhe integral cobre A e B"));
        assertEquals(NARRATIVA, integral.path("ocorrencia").path("observacao").asText());
        assertEquals(1, integral.path("ocorrencia").path("anexos").size());
        verificarUsos(c, permissaoA, ocorrencia, anexo, 1, inicioUsos);
        verificarUsos(c, permissaoB, ocorrencia, anexo, 1, inicioUsos);
        verificarDownload(c.nacional, anexo, "A-download-A-B");
        JsonNode usosA = verificarUsos(c, permissaoA, ocorrencia, anexo, 2, inicioUsos);
        JsonNode usosB = verificarUsos(c, permissaoB, ocorrencia, anexo, 2, inicioUsos);
        registrar(c, "A-consumos-completos", mapa("concessaoA", pa, "concessaoB", pb,
                "usosA", usosA, "usosB", usosB, "arquivoSha256", sha(DOCUMENTO)));

        JsonNode revogada = enviar(c.admin, "/api/permissoes-excepcionais/" + permissaoA + "/revogar",
                mapa("motivo", "Fim da necessidade documental em A; controle temporal PER010004"), 200,
                "PER010004 A revoga apenas A mantendo a Gestão corrente B autorizada");
        assertFalse(revogada.path("ativa").asBoolean(true));
        assertEquals(Boolean.TRUE, emTransacao(() -> em.find(PermissaoExcepcional.class, permissaoB).getAtivo()));
        assertNull(emTransacao(() -> em.find(PermissaoExcepcional.class, permissaoB).getRevogadoEm()));
        assertTrue(emTransacao(() -> em.find(PermissaoExcepcional.class, permissaoB).getValidoAte()).isAfter(Instant.now()));
        assertEquals(c.gestaoB.getId(), gestaoAtual(c));
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                403, "permissão excepcional específica", "A-download-A-revogada");
        assertEquals(usosA, usos(c, permissaoA));
        assertEquals(usosB, usos(c, permissaoB));
        assertEquals(fotoOriginal, fotografia(ocorrencia));
        verificarArquivo(anexo);
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());
        registrar(c, "A-conclusao", mapa("camada", "MOCKMVC_JPA_MYSQL", "predicados", "EXECUTADOS",
                "fotografiaOriginalPreservada", fotografia(ocorrencia), "concessaoARevogada", revogada,
                "usosA", usos(c, permissaoA), "usosB", usos(c, permissaoB)));
    }

    @Test
    void concessaoGenericaNaoSubstituiOrigemHistoricaEmNenhumDosSeisConsumidores() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "PER010004-LEGADO-SEM-ORIGEM");
        LocalDate semOrigem = DATA, comprovado = DATA.plusDays(2);
        JsonNode alvo = criar(c, c.localA, semOrigem, 1, "B-origem-valida");
        long ocorrencia = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        JsonNode fotoOriginal = fotografia(ocorrencia);
        executar(c.localA, get("/api/ocorrencias/{id}", ocorrencia), 200,
                "PER010004 B controle positivo anterior à retirada sintética da fonte");
        assertArrayEquals(DOCUMENTO, executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "PER010004 B arquivo íntegro e acessível com contexto comprovado").getResponse().getContentAsByteArray());
        transferir(c, comprovado.minusDays(1), "B-transferencia-posterior");
        // A transferência prova Gestão, mas não substitui a origem da habilitação.
        // Dois atos legítimos fornecem uma fonte própria de ATIVO desde D2,
        // mantendo o dia anterior dependente da publicação que será retirada.
        cicloAdministrativo(c, "inativar", comprovado.minusDays(1));
        cicloAdministrativo(c, "reativar", comprovado);
        JsonNode controle = criar(c, c.localB, comprovado, 1, "B-controle-posterior");
        long ocorrenciaControle = controle.path("id").asLong();
        long anexoControle = controle.path("anexos").get(0).path("id").asLong();
        assertEquals(c.gestaoA.getId(), gestao(c, semOrigem));
        assertEquals(c.gestaoB.getId(), gestao(c, comprovado));
        JsonNode causaAntes = causaOperacional(c, semOrigem);
        assertEquals("VIGENTE", causaAntes.path("vinculoTemporal").path("situacaoTemporal").asText());
        assertEquals("VIGENTE", causaAntes.path("contratoTemporal").path("situacaoTemporal").asText());
        assertEquals("ATIVO", causaAntes.path("contratoTemporal").path("habilitacaoAdministrativa").asText());
        emTransacao(() -> {
            assertNotNull(contextoOperacional.resolver(em.find(Empregado.class, c.empregadoA.getId()), semOrigem));
            return null;
        });
        List<Long> fontesPreservadas = emTransacao(() -> em.createQuery(
                "select v.id from VersaoContratoNegocial v where v.contrato.id=:c and v.numeroVersao<>1 order by v.id", Long.class)
                .setParameter("c", c.contratoA.getId()).getResultList());
        assertFalse(fontesPreservadas.isEmpty());

        emTransacao(() -> {
            // Fixture SINTÉTICA de acervo incompleto. O ato posterior continua preservado.
            // HQL de entidade, SQL gerado pelo Hibernate; não é um fluxo administrativo.
            assertEquals(1, em.createQuery("delete from VersaoContratoNegocial v "
                            + "where v.contrato.id=:c and v.numeroVersao=1")
                    .setParameter("c", c.contratoA.getId()).executeUpdate());
            assertEquals(fontesPreservadas, em.createQuery("select v.id from VersaoContratoNegocial v where v.contrato.id=:c order by v.id", Long.class)
                    .setParameter("c", c.contratoA.getId()).getResultList(), "Somente a origem1 foi retirada; fontes posteriores permanecem idênticas");
            return null;
        });
        assertNull(gestao(c, semOrigem));
        assertEquals(c.gestaoB.getId(), gestao(c, comprovado));
        assertEquals(c.gestaoB.getId(), gestaoAtual(c), "A coluna corrente é a isca e permanece preenchida");
        assertEquals(Boolean.TRUE, emTransacao(() -> em.find(UsuarioAplicacao.class, c.localB.getId()).getActive()));
        assertNotNull(emTransacao(() -> em.find(VinculoContratual.class, c.vinculoA.getId())));
        assertTrue(emTransacao(() -> autorizacao.resolver(em.find(Ocorrencia.class, ocorrencia)).isEmpty()));
        JsonNode causaDepois = causaOperacional(c, semOrigem);
        assertEquals(causaAntes.path("vinculoMaterializado"), causaDepois.path("vinculoMaterializado"));
        assertEquals(causaAntes.path("eventosVinculo"), causaDepois.path("eventosVinculo"));
        assertEquals(causaAntes.path("contratoMaterializado"), causaDepois.path("contratoMaterializado"));
        assertEquals("REGISTRADO", causaDepois.path("vinculoTemporal").path("estadoNegocial").asText());
        assertEquals("VIGENTE", causaDepois.path("vinculoTemporal").path("situacaoTemporal").asText());
        assertEquals("PUBLICADO", causaDepois.path("contratoTemporal").path("estadoNegocial").asText());
        for (String campo : List.of("vigenciaInicio", "vigenciaFim", "situacaoTemporal", "habilitacaoAdministrativa", "versaoContratual"))
            assertTrue(causaDepois.path("contratoTemporal").path(campo).isNull(), "Origem ausente não pode inventar " + campo);
        assertFalse(causaDepois.path("origemConhecida").asBoolean());
        assertFalse(causaDepois.path("versaoAplicavel").asBoolean());
        var causaControle = causaOperacional(c, comprovado);
        assertEquals("VIGENTE", causaControle.path("contratoTemporal").path("situacaoTemporal").asText());
        assertEquals("ATIVO", causaControle.path("contratoTemporal").path("habilitacaoAdministrativa").asText());
        registrar(c, "B-causalidade-sem-origem", mapa("antes", causaAntes, "depois", causaDepois,
                "controleComAtoPosterior", causaControle, "ramoAtingido", "Resolvedor: nenhum vínculo com posição contratual operacional comprovada"));
        assertEquals(fotoOriginal, fotografia(ocorrencia), "Retira-se a origem negocial, não o documento ou suas fotos");
        verificarArquivo(anexo);
        registrar(c, "B-precondicoes", mapa("fixture", "SINTETICA_LEGADA_SEM_VERSAO_ORIGINAL",
                "diaSemOrigem", semOrigem, "gestaoDiaSemOrigem", gestao(c, semOrigem),
                "diaComprovado", comprovado, "gestaoDiaComprovado", gestao(c, comprovado),
                "gestaoAtual", gestaoAtual(c), "fotografiaAlvo", fotoOriginal));

        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/{id}", ocorrencia), 403, SEM_CONTEXTO,
                "B-detalhe-sem-concessao");
        verificarPaginas(c, c.nacional, semOrigem, 0, ocorrencia, "B-lista-sem-origem");
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                403, SEM_CONTEXTO, "B-arquivo-antes-concessao");
        JsonNode antesConcessao = snapshot(c);
        JsonNode concessao = enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                "contratoId", c.contratoA.getId(), "finalidade", FINALIDADE,
                "justificativa", "Concessão genérica não restaura origem documental ausente",
                "autorizacaoAdministrativa", "ATO-SINTETICO-PER010004-B", "validoAte", Instant.now().plusSeconds(1800)),
                201, "PER010004 B cadastro genérico é válido; não decide uma ocorrência ou data");
        long permissao = concessao.path("id").asLong();
        assertTrue(concessao.path("ativa").asBoolean());
        assertEquals(0, usos(c, permissao).size());
        assertSemEfeito(c, "B-concessao-nao-reconstitui-fato", antesConcessao);
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/{id}", ocorrencia).param("finalidade", FINALIDADE),
                403, SEM_CONTEXTO, "B-detalhe-concessao-vigente");
        negarSemEfeito(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                403, SEM_CONTEXTO, "B-arquivo-concessao-vigente");
        assertEquals(0, usos(c, permissao).size(), "Concessão não pode fabricar contexto nem consumo");

        // Mesmo leitor, finalidade e concessão: apenas o dia de prova íntegra libera a leitura.
        Instant inicioControle = Instant.now();
        JsonNode integralControle = dados(executar(c.nacional, get("/api/ocorrencias/{id}", ocorrenciaControle)
                .param("finalidade", FINALIDADE), 200, "PER010004 B controle positivo posterior com a mesma concessão"));
        assertEquals(NARRATIVA, integralControle.path("ocorrencia").path("observacao").asText());
        verificarDownload(c.nacional, anexoControle, "B-download-controle-com-origem");
        JsonNode usosControle = verificarUsos(c, permissao, ocorrenciaControle, anexoControle, 2, inicioControle);

        negarSemEfeito(c, c.localB, multipartCriacao(c, semOrigem, 1, "B-criacao-sem-origem"), 409,
                "Não existe Vínculo Contratual operacional válido na data da jornada.", "B-criar-sem-origem");
        negarSemEfeito(c, c.localB, delete("/api/ocorrencias/{id}", ocorrencia), 403, SEM_CONTEXTO,
                "B-cancelar-sem-origem");
        verificarPaginas(c, c.nacional, semOrigem, 0, ocorrencia, "B-lista-pos-concessao");
        assertEquals(usosControle, usos(c, permissao));
        assertEquals(fotoOriginal, fotografia(ocorrencia));
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, ocorrencia).getCanceladoEm()));
        verificarArquivo(anexo);
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());

        // A mesma autoridade atual consegue escrever/cancelar onde a fonte posterior existe.
        executar(c.localB, delete("/api/ocorrencias/{id}", ocorrenciaControle), 204,
                "PER010004 B controle positivo da autoridade e do cancelamento em dia comprovado");
        assertNotNull(emTransacao(() -> em.find(Ocorrencia.class, ocorrenciaControle).getCanceladoEm()));
        JsonNode novaComProva = criar(c, c.localB, comprovado, 1, "B-criacao-controle-com-origem");
        assertTrue(novaComProva.path("id").asLong() > 0);
        assertEquals(fotoOriginal, fotografia(ocorrencia));
        registrar(c, "B-conclusao", mapa("camada", "MOCKMVC_JPA_MYSQL", "seisConsumidores", List.of(
                        "detalhe403", "lista200vazia", "arquivo403semBytes", "criar409semEfeito",
                        "cancelar403semEfeito", "conceder201_e_uso403semEfeito"),
                "limiteArquitetural", "Concessão não recebe ocorrênciaId/data: 201 genérico não prova concessão contextual recusada",
                "concessao", concessao, "usosSomenteControleComOrigem", usosControle,
                "fotografiaOriginalPreservada", fotografia(ocorrencia), "arquivoSha256", sha(DOCUMENTO)));
    }

    private JsonNode criar(Cenario c, UsuarioAplicacao ator, LocalDate dia, int dias, String rotulo) throws Exception {
        JsonNode r = dados(executar(ator, multipartCriacao(c, dia, dias, rotulo), 201,
                "PER010004 " + rotulo + ": criação legítima e fonte nominal sintética explícita"));
        assertTrue(r.path("id").asLong() > 0);
        assertEquals(1, r.path("anexos").size());
        return r;
    }

    private MockHttpServletRequestBuilder multipartCriacao(Cenario c, LocalDate dia, int dias, String rotulo) {
        return multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova-PER010004.txt", "text/plain", DOCUMENTO))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", dia.toString()).param("dias", Integer.toString(dias))
                .param("observacao", NARRATIVA).param("classificacoesAnexos", "PESSOAL_RESTRITO")
                .param("chaveIdempotencia", "PER010004-" + rotulo + "-" + UUID.randomUUID());
    }

    private void transferir(Cenario c, LocalDate dia, String rotulo) throws Exception {
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", dia, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência retroativa legítima PER010004 " + rotulo,
                "referenciaSei", "SEI-SINTETICO-PER010004", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "PER010004 transferência A→B pela API e commit real");
    }

    private JsonNode concessaoGestao(Cenario c, long gestao, String rotulo) throws Exception {
        return enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS",
                "usuarioBeneficiarioId", c.nacional.getId(), "gestaoLocalId", gestao, "finalidade", FINALIDADE,
                "justificativa", "Conferência autorizada somente na Gestão informada",
                "autorizacaoAdministrativa", "ATO-SINTETICO-PER010004-" + rotulo,
                "validoAte", Instant.now().plusSeconds(1800)), 201, "PER010004 " + rotulo + " sem contratoId");
    }

    private void cicloAdministrativo(Cenario c, String operacao, LocalDate data) throws Exception {
        var req = mapa("dataEfetiva", data, "motivo", "Ato sintético explícito PER010004 para controle posterior " + operacao);
        var previa = enviar(c.localB, base(c.contratoA.getId()) + "/previas/" + operacao, req, 200,
                "Prévia real de habilitação posterior antes de retirar qualquer fonte");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localB, base(c.contratoA.getId()) + "/" + operacao, req, 200,
                "Ato administrativo legítimo, sem modificar campo de habilitação por fixture");
    }

    private JsonNode causaOperacional(Cenario c, LocalDate data) {
        var leitura = new org.springframework.transaction.support.TransactionTemplate(transactionManager); leitura.setReadOnly(true);
        return leitura.execute(status -> {
            em.clear(); var v = em.find(VinculoContratual.class, c.vinculoA.getId()); var contrato = v.getContrato();
            assertEquals(c.pessoaA.getId(), v.getPessoa().getId()); assertEquals(c.contratoA.getId(), contrato.getId());
            assertFalse(v.getDataInicio().isAfter(data)); assertTrue(v.getDataFim() == null || !v.getDataFim().isBefore(data));
            assertEquals(EstadoNegocialContrato.PUBLICADO, contrato.getEstadoNegocial());
            assertEquals(HabilitacaoAdministrativaContrato.ATIVO, contrato.getHabilitacaoAdministrativa());
            assertFalse(contrato.getDataInicio().isAfter(data)); assertFalse(contrato.getDataFim().isBefore(data));
            var eventos = em.createQuery("select e.id,e.tipoEvento,e.dataEfetiva,e.inicioNovo,e.fimNovo,e.hashAnterior,e.hashEvento "
                            + "from EventoVinculoContratual e where e.vinculo.id=:v order by e.id", Object[].class)
                    .setParameter("v", v.getId()).getResultList().stream().map(java.util.Arrays::toString).toList();
            return mapper.valueToTree(mapa("vinculoMaterializado", mapa("id", v.getId(), "pessoa", v.getPessoa().getId(),
                            "contrato", contrato.getId(), "inicio", v.getDataInicio(), "fim", v.getDataFim(), "estado", v.getEstadoNegocial()),
                    "eventosVinculo", eventos, "contratoMaterializado", mapa("id", contrato.getId(), "gestao", contrato.getGestaoLocal().getId(),
                            "inicio", contrato.getDataInicio(), "fim", contrato.getDataFim(), "estado", contrato.getEstadoNegocial(),
                            "habilitacao", contrato.getHabilitacaoAdministrativa()),
                    "vinculoTemporal", motorVinculo.reconstruir(v, data, Instant.now()),
                    "contratoTemporal", motorContrato.reconstruirPosicao(contrato, data, Instant.now()),
                    "origemConhecida", motorContrato.fotografiaOriginalConhecida(contrato, Instant.now()).isPresent(),
                    "versaoAplicavel", motorContrato.versaoAplicavel(contrato, data, Instant.now()).isPresent()));
        });
    }

    private JsonNode usos(Cenario c, long permissao) throws Exception {
        JsonNode r = consultar(c.admin, "/api/permissoes-excepcionais/" + permissao + "/usos",
                "PER010004 leitura dos usos efetivamente confirmados");
        assertTrue(r.isArray());
        return r;
    }

    private JsonNode verificarUsos(Cenario c, long permissao, long ocorrencia, long anexo, int quantidade, Instant inicio) throws Exception {
        JsonNode r = usos(c, permissao);
        assertEquals(quantidade, r.size(), "Um consumo por contexto contrato/Gestão em cada requisição");
        int detalhes = 0, arquivos = 0;
        for (JsonNode u : r) {
            assertTrue(u.path("id").asLong() > 0);
            assertEquals(permissao, u.path("permissaoId").asLong());
            assertEquals("ACESSO_DADOS_PESSOAIS_RESTRITOS", u.path("tipo").asText());
            assertEquals(c.nacional.getId().longValue(), u.path("usuarioId").asLong());
            assertEquals(c.nacional.getUsername(), u.path("usuario").asText());
            assertEquals(FINALIDADE, u.path("finalidade").asText());
            Instant quando = Instant.parse(u.path("usadoEm").asText());
            assertFalse(quando.isBefore(inicio.minusSeconds(1)));
            assertFalse(quando.isAfter(Instant.now().plusSeconds(1)));
            if (u.path("tipoRecurso").asText().equals("Ocorrencia")) {
                assertEquals(ocorrencia, u.path("recursoId").asLong()); detalhes++;
            } else {
                assertEquals("AnexoOcorrencia", u.path("tipoRecurso").asText());
                assertEquals(anexo, u.path("recursoId").asLong()); arquivos++;
            }
        }
        assertEquals(1, detalhes);
        assertEquals(quantidade - 1, arquivos);
        return r;
    }

    private void verificarDownload(UsuarioAplicacao ator, long anexo, String rotulo) throws Exception {
        MvcResult r = executar(ator, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE), 200,
                "PER010004 " + rotulo + ": bytes e hash da prova integral");
        assertNotNull(r.getResponse().getHeader("Content-Disposition"));
        assertArrayEquals(DOCUMENTO, r.getResponse().getContentAsByteArray());
        assertEquals(sha(DOCUMENTO), sha(r.getResponse().getContentAsByteArray()));
        verificarArquivo(anexo);
    }

    private void verificarPaginas(Cenario c, UsuarioAplicacao ator, LocalDate dia, int total, long ocorrencia, String rotulo) throws Exception {
        JsonNode antes = snapshot(c);
        List<JsonNode> paginas = new ArrayList<>();
        for (int pagina = 0; pagina < 2; pagina++) {
            JsonNode r = dados(executar(ator, get("/api/ocorrencias").param("contratoId", c.contratoA.getId().toString())
                    .param("data", dia.toString()).param("page", Integer.toString(pagina)).param("size", "1"),
                    200, "PER010004 " + rotulo + " página " + pagina));
            assertEquals(total, r.path("totalElements").asInt());
            assertEquals(total, r.path("totalPages").asInt());
            assertEquals(pagina == 0 ? total : 0, r.path("content").size());
            if (total == 1 && pagina == 0) {
                assertEquals(ocorrencia, r.path("content").get(0).path("id").asLong());
                assertTrue(r.path("content").get(0).path("anexos").isEmpty());
                assertFalse(r.toString().contains(NARRATIVA));
            }
            paginas.add(r);
        }
        registrar(c, rotulo + "-paginas", mapa("paginas", paginas));
        assertSemEfeito(c, rotulo, antes);
    }

    private void negarSemEfeito(Cenario c, UsuarioAplicacao ator, MockHttpServletRequestBuilder pedido,
                               int status, String causa, String rotulo) throws Exception {
        JsonNode antes = snapshot(c);
        MvcResult r = executar(ator, pedido, status, "PER010004 " + rotulo);
        String corpo = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(corpo.contains(causa), "Causa discriminante ausente: " + corpo);
        assertFalse(corpo.contains(NARRATIVA));
        assertFalse(corpo.contains(new String(DOCUMENTO, StandardCharsets.UTF_8)));
        assertNull(r.getResponse().getHeader("Content-Disposition"), "A recusa não publica o arquivo");
        registrar(c, rotulo + "-resposta", mapa("status", status, "causa", causa,
                "response", mapper.readTree(corpo), "responseSha256", sha(r.getResponse().getContentAsByteArray())));
        assertSemEfeito(c, rotulo, antes);
    }

    private void assertSemEfeito(Cenario c, String rotulo, JsonNode antes) throws Exception {
        JsonNode depois = snapshot(c);
        registrar(c, rotulo + "-efeitos", mapa("antes", antes, "depois", depois));
        assertEquals(antes, depois, "Recusa/consulta não muda agregado, frequências, eventos, usos ou armazenamento: " + rotulo);
    }

    private JsonNode snapshot(Cenario c) throws Exception {
        var leitura = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        Map<String, Object> r = leitura.execute(status -> {
            long empregado = c.empregadoA.getId();
            return mapa("ocorrencias", jdbc.queryForList("select * from ocorrencia where id_empregado=? order by id", empregado),
                    "contextos", jdbc.queryForList("select x.* from contexto_ocorrencia x join ocorrencia o on o.id=x.ocorrencia_id where o.id_empregado=? order by x.id", empregado),
                    "anexos", jdbc.queryForList("select a.* from anexo_ocorrencia a join ocorrencia o on o.id=a.id_ocorrencia where o.id_empregado=? order by a.id", empregado),
                    "frequencias", jdbc.queryForList("select * from frequencia where id_empregado=? order by id", empregado),
                    "eventosFrequencia", jdbc.queryForList("select e.* from evento_frequencia e join frequencia f on f.id=e.id_frequencia where f.id_empregado=? order by e.id_evento_frequencia", empregado),
                    "historico", jdbc.queryForList("select * from historico_empregado where id_empregado=? order by id_historico", empregado),
                    "usos", jdbc.queryForList("select * from permissao_excepcional_uso where id_usuario=? order by id_permissao_uso", c.nacional.getId()));
        });
        Map<String, String> arquivos = new TreeMap<>();
        Path raiz = armazenamento.uploadDir().toAbsolutePath().normalize();
        if (Files.exists(raiz)) try (var stream = Files.walk(raiz)) {
            for (Path p : stream.filter(Files::isRegularFile).sorted().toList())
                arquivos.put(raiz.relativize(p).toString(), sha(Files.readAllBytes(p)));
        }
        r.put("armazenamentoSha256", arquivos);
        return mapper.valueToTree(r);
    }

    private JsonNode fotografia(long ocorrencia) {
        var leitura = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        return leitura.execute(status -> mapper.valueToTree(mapa(
                "ocorrencia", jdbc.queryForList("select * from ocorrencia where id=?", ocorrencia),
                "contextos", jdbc.queryForList("select * from contexto_ocorrencia where ocorrencia_id=? order by id", ocorrencia),
                "anexos", jdbc.queryForList("select * from anexo_ocorrencia where id_ocorrencia=? order by id", ocorrencia),
                "historico", jdbc.queryForList("select * from historico_empregado where id_ocorrencia=? order by id_historico", ocorrencia))));
    }

    private void verificarArquivo(long id) throws Exception {
        String[] salvo = emTransacao(() -> {
            AnexoOcorrencia a = em.find(AnexoOcorrencia.class, id);
            return new String[]{a.getNomeSalvo(), a.getSha256()};
        });
        Path raiz = armazenamento.uploadDir().toAbsolutePath().normalize();
        Path arquivo = raiz.resolve(salvo[0]).normalize();
        assertTrue(arquivo.startsWith(raiz));
        assertTrue(Files.isRegularFile(arquivo));
        assertArrayEquals(DOCUMENTO, Files.readAllBytes(arquivo));
        assertEquals(sha(DOCUMENTO), salvo[1]);
    }

    private Long gestao(Cenario c, LocalDate dia) {
        return emTransacao(() -> {
            GestaoLocal g = gestoes.gestaoEm(em.find(Contrato.class, c.contratoA.getId()), dia, null);
            return g == null ? null : g.getId();
        });
    }

    private Long gestaoAtual(Cenario c) {
        return emTransacao(() -> em.find(Contrato.class, c.contratoA.getId()).getGestaoLocal().getId());
    }

    private String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void registrar(Cenario c, String fase, Map<String, Object> conteudo) throws Exception {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("criterio", "PER-010:004"); r.put("fase", fase); r.put("emUtc", Instant.now());
        r.put("camada", "MOCKMVC_COM_FILTROS_JPA_MYSQL_REAL");
        r.put("contratoId", c.contratoA.getId()); r.put("empregadoId", c.empregadoA.getId());
        r.putAll(conteudo);
        Path destino = Path.of("target/auditoria-http/PER010004-" + c.contratoA.getId() + "-" + fase + ".json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(r));
    }
}
