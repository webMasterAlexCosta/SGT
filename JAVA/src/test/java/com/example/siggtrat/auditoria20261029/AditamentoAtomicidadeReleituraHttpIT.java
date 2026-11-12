package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Files;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** ADI004: fixture integralmente sintética. Proposta sem execução.
 * Falha em dependência real após gravar fotografia, itens e fontes no próprio POST.
 * Não há transação externa. Arquivos preexistentes participam; o POST de aditamento
 * publica fontes preparadas, mas não recebe nem cria upload novo. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AditamentoAtomicidadeReleituraHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo armazenamento;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.example.siggtrat.servico.MotorTemporalContratoServico motor;

    @Test
    void falhaAposPostDeAditamentoDesfazItensFontesEPreservaAcervoEArquivosConfirmados() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 100, 25, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "ADI004 origem confirmada").get(0).path("id").asLong();
        byte[] antigo = pdfSintetico("Arquivo preexistente sintético preservado");
        JsonNode documentoAntigo = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", "Fonte sintética preexistente ADI004",
                        "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", n.inicio(),
                        "chaveIdempotencia", "adi004-antigo-" + UUID.randomUUID())))
                .file(new MockMultipartFile("arquivo", "preexistente.pdf", "application/pdf", antigo)), 201,
                "Controle positivo: arquivo original confirmado antes da operação revertida"));
        long docAntigoId = documentoAntigo.path("id").asLong();
        byte[] novo = pdfSintetico("Arquivo preparado exclusivamente sintético para o aditamento");
        var preparada = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoChave", "ADI004-REVERTER",
                        "titulo", "Fonte sintética preparada antes do POST", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", n.inicio().plusDays(2), "chaveIdempotencia", "adi004-novo-" + UUID.randomUUID())))
                .file(new MockMultipartFile("arquivo", "preparado.pdf", "application/pdf", novo)), 201,
                "Upload sintético confirmado antes do POST: não faz parte da falha da operação de aditamento"));
        long preparadaId = preparada.path("id").asLong();
        assertThat(preparada.path("estado").asText()).isEqualTo("RASCUNHO");
        var antes = estadoContrato(c, n);
        var arquivosAntes = arquivos();
        var itensAntes = itens(n.id());
        var idsNovos = new java.util.concurrent.atomic.AtomicReference<Map<String,List<Long>>>();
        var docsAntes = emTransacao(() -> em.createQuery("select d.id from DocumentoComprobatorio d where d.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getResultList());
        var pedido = aditamento("ADI004-REVERTER", n.inicio().plusDays(2));
        pedido.put("novoEfetivoTotal", 110);
        pedido.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 35)));
        emTransacao(() -> { try { previaAditamento(c, n.id(), pedido); return null; }
            catch (Exception e) { throw new IllegalStateException(e); } });
        var habilitada = new java.util.concurrent.atomic.AtomicBoolean(true);
        var disparos = new java.util.concurrent.atomic.AtomicInteger();
        // Resolva o alvo AOP antes de abrir o contexto de stubbing do Mockito.
        // A inspeção do proxy nesse contexto pode consumir a resposta no logger.
        var motorAlvo = org.springframework.test.util.AopTestUtils
                .<com.example.siggtrat.servico.MotorTemporalContratoServico>getUltimateTargetObject(motor);
        assertThat(org.mockito.Mockito.mockingDetails(motorAlvo).isSpy())
                .as("A falha é injetada no spy real do motor, após resolver o proxy").isTrue();
        org.mockito.Mockito.doAnswer(invocacao -> {
            Contrato contrato = invocacao.getArgument(0);
            EventoCicloVidaContrato evento = invocacao.getArgument(1);
            Object fotografia = invocacao.callRealMethod();
            if (habilitada.get() && contrato.getId().equals(n.id())
                    && evento.getTipoEvento() == TipoEventoCicloVidaContrato.ADITAMENTO) {
                em.flush(); em.clear();
                long eventoId = evento.getId();
                long atoId = em.createQuery("select a.id from AditamentoContratual a where a.contrato.id=:id and a.evento.id=:evento", Long.class)
                        .setParameter("id", n.id()).setParameter("evento", eventoId).getSingleResult();
                assertThat(em.find(AditamentoContratual.class, atoId)).isNotNull();
                assertThat(em.find(EventoCicloVidaContrato.class, eventoId)).isNotNull();
                var itensCriados = em.createQuery("select i.id from AditamentoComposicao i where i.aditamento.id=:id", Long.class)
                        .setParameter("id", atoId).getResultList();
                var docsCriados = em.createQuery("select d.id from DocumentoComprobatorio d where d.contrato.id=:id and d.fatoId=:fato", Long.class)
                        .setParameter("id", n.id()).setParameter("fato", atoId).getResultList();
                assertThat(itensCriados).hasSize(1);
                assertThat(docsCriados).contains(preparadaId);
                assertThat(em.find(DocumentoComprobatorio.class, preparadaId).getEstado().name()).isEqualTo("PUBLICADO");
                var novosDocs = new ArrayList<>(docsCriados); novosDocs.removeAll(docsAntes);
                assertThat(novosDocs).hasSize(1);
                idsNovos.set(Map.of("aditamento", List.of(atoId), "evento", List.of(eventoId), "itens", itensCriados,
                        "documentos", novosDocs, "fotografia", List.of(((VersaoContratoNegocial) fotografia).getId())));
                disparos.incrementAndGet();
                throw new FalhaSinteticaPersistencia();
            }
            return fotografia;
        }).when(motorAlvo)
                .registrarFotografiaProjetada(org.mockito.ArgumentMatchers.any(Contrato.class),
                        org.mockito.ArgumentMatchers.any(EventoCicloVidaContrato.class), org.mockito.ArgumentMatchers.any(java.time.LocalDate.class),
                        org.mockito.ArgumentMatchers.any(com.example.siggtrat.dto.VersaoContratoNegocialDTO.class));
        try {
            enviar(c.localA, base(n.id()) + "/aditamentos", pedido, 500,
                    "Falha sintética após persistência real de item/fonte/fotografia dentro do próprio POST");
        } finally { habilitada.set(false); }
        assertThat(disparos.get()).as("O ponto posterior às escritas foi efetivamente atingido pela PK da fixture").isEqualTo(1);

        assertThat(idsNovos.get()).isNotNull();
        // NOT_SUPPORTED externo: cada leitura abaixo abre transação própria após o rollback.
        emTransacao(() -> {
            em.clear();
            for (long id : idsNovos.get().get("aditamento")) assertThat(em.find(AditamentoContratual.class, id)).isNull();
            for (long id : idsNovos.get().get("evento")) assertThat(em.find(EventoCicloVidaContrato.class, id)).isNull();
            for (long id : idsNovos.get().get("itens")) assertThat(em.find(AditamentoComposicao.class, id)).isNull();
            for (long id : idsNovos.get().get("documentos")) assertThat(em.find(DocumentoComprobatorio.class, id)).isNull();
            for (long id : idsNovos.get().get("fotografia")) assertThat(em.find(VersaoContratoNegocial.class, id)).isNull();
            return null;
        });
        assertThat(estadoContrato(c, n)).isEqualTo(antes);
        assertThat(itens(n.id())).isEqualTo(itensAntes);
        assertThat(arquivos()).as("Mesmos caminhos e bytes confirmados, sem staging ou arquivo novo órfão").isEqualTo(arquivosAntes);
        assertThat(executar(c.localA, get(base(n.id()) + "/documentos/" + docAntigoId + "/arquivo"), 200,
                "Arquivo sintético preexistente continua íntegro após falha do POST").getResponse().getContentAsByteArray()).isEqualTo(antigo);
        assertThat(emTransacao(() -> em.find(DocumentoComprobatorio.class, preparadaId).getEstado().name())).isEqualTo("RASCUNHO");
        var confirmada = enviar(c.localA, base(n.id()) + "/aditamentos", pedido, 201,
                "Controle positivo: mesma intenção e token confirmam depois de desabilitar somente a falha sintética");
        assertThat(disparos.get()).isEqualTo(1);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "Um único instrumento após repetição válida")).hasSize(1);
        assertThat(emTransacao(() -> em.find(DocumentoComprobatorio.class, preparadaId).getFatoId())).isEqualTo(confirmada.path("id").asLong());
        assertThat(quantidades(c, n, n.inicio().plusDays(2))).isEqualTo(Map.of(n.composicao1(), 35, n.composicao2(), 75));
        assertThat(arquivos()).as("O POST só publica a associação dos arquivos confirmados, sem upload novo").isEqualTo(arquivosAntes);
    }

    private Map<String,JsonNode> estadoContrato(Cenario c, NovoContrato n) throws Exception {
        var estado = new LinkedHashMap<String,JsonNode>();
        for (String recurso : List.of("", "/aditamentos", "/eventos", "/eventos/precedencias", "/versoes", "/documentos",
                "/composicoes?data=" + n.inicio().plusDays(2))) {
            java.time.Instant inicioConsulta = java.time.Instant.now();
            JsonNode observado = consultar(c.localA, base(n.id()) + recurso,
                    "ADI004 releitura completa do acervo sintético " + recurso);
            if (recurso.startsWith("/composicoes?")) {
                for (JsonNode composicao : observado) {
                    assertThat(composicao.path("conhecidoAte").isTextual()).isTrue();
                    java.time.Instant corte = java.time.Instant.parse(composicao.path("conhecidoAte").asText());
                    assertThat(corte).isBetween(inicioConsulta.minusSeconds(1), java.time.Instant.now().plusSeconds(1));
                    // Metadado desta consulta muda a cada GET. Não remover registradoEm das fontes ou outros fatos.
                    ((com.fasterxml.jackson.databind.node.ObjectNode) composicao).remove("conhecidoAte");
                }
            }
            estado.put(recurso, observado);
        }
        var docs = estado.get("/documentos");
        for (JsonNode doc : docs) {
            long id = doc.path("id").asLong();
            estado.put("eventos-documento-" + id, consultar(c.localA, base(n.id()) + "/documentos/" + id + "/eventos", "ADI004 cadeia documental preexistente"));
        }
        return estado;
    }

    private List<String> itens(long contratoId) {
        return emTransacao(() -> em.createQuery("select i.id,i.aditamento.id,i.composicaoBase.id,i.quantidadeAnterior,i.variacao,i.quantidadeResultante from AditamentoComposicao i where i.aditamento.contrato.id=:id order by i.id", Object[].class)
                .setParameter("id", contratoId).getResultList().stream().map(Arrays::toString).toList());
    }

    private Map<String,String> arquivos() {
        var raiz = armazenamento.uploadDir();
        if (!Files.exists(raiz)) return Map.of();
        try (var caminhos = Files.walk(raiz)) {
            var estado = new TreeMap<String,String>();
            for (var p : caminhos.filter(Files::isRegularFile).toList())
                estado.put(raiz.relativize(p).toString(), HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));
            return Map.copyOf(estado);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private Map<Long,Integer> quantidades(Cenario c, NovoContrato n, java.time.LocalDate data) throws Exception {
        var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data, "ADI004 todos os IDs e quantidades após controle positivo");
        assertThat(composicoes).hasSize(2);
        return java.util.stream.StreamSupport.stream(composicoes.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(i -> i.path("id").asLong(), i -> i.path("efetivoPrevisto").asInt()));
    }
    private byte[] pdfSintetico(String texto) throws Exception {
        var saida = new java.io.ByteArrayOutputStream();
        var documento = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(documento, saida);
        documento.open(); documento.add(new com.lowagie.text.Paragraph(texto)); documento.close();
        return saida.toByteArray();
    }
    private static final class FalhaSinteticaPersistencia extends RuntimeException { }
}
