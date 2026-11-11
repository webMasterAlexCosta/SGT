package com.example.siggtrat.etapa08;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesEnvioArquivo;
import com.example.siggtrat.dominio.documento.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.DocumentoComprobatorioServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentoComprobatorioServicoEtapa08Test {

    @TempDir
    Path diretorio;

    private DocumentoComprobatorioRepositorio documentos;
    private EventoDocumentoComprobatorioRepositorio eventos;
    private PermissaoDocumentoRestritoRepositorio permissoes;
    private AcessoDocumentoRestritoRepositorio acessosRegistrados;
    private ContratoRepositorio contratos;
    private UsuarioAplicacaoRepositorio usuarios;
    private AcessoOrganizacionalServico acesso;
    private DocumentoComprobatorioServico servico;
    private final Map<Long, DocumentoComprobatorio> bancoDocumentos = new LinkedHashMap<>();
    private final Map<String, DocumentoComprobatorio> bancoIdempotencia = new HashMap<>();
    private final Map<String, EventoDocumentoComprobatorio> ultimoEvento = new HashMap<>();
    private final List<EventoDocumentoComprobatorio> bancoEventos = new ArrayList<>();
    private final AtomicLong sequenciaDocumento = new AtomicLong(100);
    private final AtomicLong sequenciaEvento = new AtomicLong(500);
    private GestaoLocal gestao;
    private Contrato contrato;
    private UsuarioAplicacao usuarioAtual;

    @BeforeEach
    void preparar() {
        documentos = mock(DocumentoComprobatorioRepositorio.class);
        eventos = mock(EventoDocumentoComprobatorioRepositorio.class);
        permissoes = mock(PermissaoDocumentoRestritoRepositorio.class);
        acessosRegistrados = mock(AcessoDocumentoRestritoRepositorio.class);
        contratos = mock(ContratoRepositorio.class);
        usuarios = mock(UsuarioAplicacaoRepositorio.class);
        acesso = mock(AcessoOrganizacionalServico.class);

        gestao = new GestaoLocal();
        gestao.setId(11L);
        gestao.setNome("Gestão Recife");
        gestao.setCodigo("REC");
        contrato = contrato(21L, EstadoNegocialContrato.RASCUNHO);
        usuarioAtual = usuario(31L, "gestor.local", "PERFIL_GESTOR_LOCAL", gestao);

        when(contratos.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return Objects.equals(id, contrato.getId()) ? Optional.of(contrato) : Optional.empty();
        });
        when(contratos.buscarPorIdParaAtualizacao(anyLong())).thenAnswer(inv ->
                contratos.findById(inv.getArgument(0)));
        when(acesso.usuarioAtual()).thenAnswer(inv -> usuarioAtual);
        doNothing().when(acesso).exigirAcessoContrato(any(), any());
        when(acesso.ehGestaoLocal(any())).thenAnswer(inv -> possuiPapel(inv.getArgument(0), "PERFIL_GESTOR_LOCAL"));
        when(acesso.ehOperacional(any())).thenAnswer(inv -> possuiPapel(inv.getArgument(0), "PERFIL_OPERACIONAL"));
        when(acesso.ehAdministrador(any())).thenAnswer(inv -> possuiPapel(inv.getArgument(0), "PERFIL_ADMINISTRADOR"));

        when(documentos.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(bancoDocumentos.get(inv.getArgument(0))));
        when(documentos.findByChaveIdempotencia(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(bancoIdempotencia.get(inv.getArgument(0))));
        when(documentos.save(any())).thenAnswer(inv -> {
            DocumentoComprobatorio documento = inv.getArgument(0);
            if (documento.getId() == null) documento.setId(sequenciaDocumento.incrementAndGet());
            bancoDocumentos.put(documento.getId(), documento);
            if (documento.getChaveIdempotencia() != null) {
                bancoIdempotencia.put(documento.getChaveIdempotencia(), documento);
            }
            return documento;
        });
        doAnswer(inv -> {
            DocumentoComprobatorio documento = inv.getArgument(0);
            bancoDocumentos.remove(documento.getId());
            return null;
        }).when(documentos).delete(any());
        when(documentos.countByNomeArmazenado(anyString())).thenReturn(1L);
        when(documentos.findByContratoIdOrderByRegistradoEmAscIdAsc(anyLong())).thenAnswer(inv -> bancoDocumentos.values()
                .stream().filter(d -> Objects.equals(d.getContrato().getId(), inv.getArgument(0))).toList());

        when(eventos.findTopByGrupoLogicoIdOrderByRegistradoEmDescIdDesc(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(ultimoEvento.get(inv.getArgument(0))));
        when(eventos.findFirstByDocumentoIdOrderByRegistradoEmAscIdAsc(anyLong())).thenAnswer(inv ->
                bancoEventos.stream().filter(e -> Objects.equals(e.getDocumento().getId(), inv.getArgument(0))).findFirst());
        when(eventos.save(any())).thenAnswer(inv -> {
            EventoDocumentoComprobatorio evento = inv.getArgument(0);
            if (evento.getId() == null) evento.setId(sequenciaEvento.incrementAndGet());
            bancoEventos.add(evento);
            ultimoEvento.put(evento.getGrupoLogicoId(), evento);
            return evento;
        });
        when(permissoes.buscarAtivas(anyString(), anyLong(), any())).thenReturn(List.of());

        servico = new DocumentoComprobatorioServico(documentos, eventos, permissoes, acessosRegistrados,
                contratos, usuarios, acesso, new PropriedadesArmazenamentoArquivo(diretorio),
                new PropriedadesEnvioArquivo(5_000_000, 10_000_000, 3));
    }

    @Test
    void falhaAoFecharStreamDoUploadRemoveTemporarioSemPublicarDocumento() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "documento.pdf", "application/pdf",
                "%PDF-1.4\nconteudo".getBytes()) {
            private int leituras;

            @Override
            public InputStream getInputStream() throws IOException {
                InputStream entrada = super.getInputStream();
                // A primeira leitura calcula a intenção; a segunda prepara o armazenamento.
                if (++leituras == 1) return entrada;
                return new FilterInputStream(entrada) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        throw new IOException("Falha sintética no fechamento do upload");
                    }
                };
            }
        };

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicaoAnexo("io-upload"), arquivo));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, erro.getStatusCode());
        assertInstanceOf(IOException.class, erro.getCause());
        assertTrue(bancoDocumentos.isEmpty());
        assertTrue(bancoEventos.isEmpty());
        try (var arquivos = Files.walk(diretorio)) {
            assertEquals(0, arquivos.filter(Files::isRegularFile).count(),
                    "A falha de upload não pode deixar bytes sem documento, inclusive no staging");
        }
    }

    @Test
    void falhaDeExclusaoRegistraDiagnosticoSeguroSemPerderErroOriginal() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(DocumentoComprobatorioServico.class);
        var capturador = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        capturador.start();
        logger.addAppender(capturador);
        try {
            MockMultipartFile arquivo = new MockMultipartFile("arquivo", "documento.pdf", "application/pdf",
                    "%PDF-1.4\nconteudo".getBytes()) {
                private int leituras;

                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream entrada = super.getInputStream();
                    if (++leituras == 1) return entrada;
                    return new FilterInputStream(entrada) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            // Simula falha de FS na compensação, sem alterar permissões do host.
                            try (var staging = Files.list(diretorio.resolve(".staging"))) {
                                Path temporario = staging.findFirst().orElseThrow();
                                Files.delete(temporario);
                                Files.createDirectory(temporario);
                                Files.writeString(temporario.resolve("conteudo-privado"), "nao-expor");
                            }
                            throw new IOException("Falha original com detalhe privado nao-expor");
                        }
                    };
                }
            };
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> servico.criar(21L, requisicaoAnexo("io-cleanup"), arquivo));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, erro.getStatusCode());
            assertEquals("Falha original com detalhe privado nao-expor", erro.getCause().getMessage());
            assertTrue(bancoDocumentos.isEmpty());
            assertTrue(bancoEventos.isEmpty());
            assertEquals(1, capturador.list.size(), "Falha de compensação exige diagnóstico próprio");
            String diagnostico = capturador.list.getFirst().getFormattedMessage();
            assertTrue(diagnostico.contains("DirectoryNotEmptyException"));
            assertFalse(diagnostico.contains(diretorio.toString()));
            assertFalse(diagnostico.contains("nao-expor"));
        } finally {
            logger.detachAppender(capturador);
            capturador.stop();
        }
    }

    @Test
    void idempotenciaLegadaDoCadastroComparaCamposSemModificarHistorico() {
        var requisicao = requisicaoSei("SEI 100/2026", "legado-cadastro");
        var original = servico.criar(21L, requisicao, null);
        bancoDocumentos.get(original.id()).setHashIntencao(null);

        var repetida = servico.criar(21L, requisicao, null);

        assertEquals(original.id(), repetida.id());
        assertNull(bancoDocumentos.get(original.id()).getHashIntencao());
        assertEquals(1, bancoEventos.size());
        var erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicaoSei("SEI 200/2026", "legado-cadastro"), null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void idempotenciaLegadaDaRetificacaoUsaEventoOriginal() {
        var alvo = documentoPublicado("SEI 100/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        var requisicao = new RetificacaoSeiDocumentoRequisicao("SEI 200/2026", "Correção documental", "legado-ret");
        var original = servico.retificarSei(21L, alvo.getId(), requisicao);
        bancoDocumentos.get(original.id()).setHashIntencao(null);

        var repetida = servico.retificarSei(21L, alvo.getId(), requisicao);

        assertEquals(original.id(), repetida.id());
        assertEquals(1, bancoEventos.size());
        var erro = assertThrows(ResponseStatusException.class,
                () -> servico.retificarSei(21L, alvo.getId(),
                        new RetificacaoSeiDocumentoRequisicao("SEI 200/2026", "Outro motivo", "legado-ret")));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void idempotenciaDoCadastroRejeitaConteudoDiferenteNoMesmoContrato() {
        servico.criar(21L, requisicaoSei("SEI 100/2026", "mesma-chave"), null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicaoSei("SEI 200/2026", "mesma-chave"), null));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals(1, bancoDocumentos.size());
    }

    @Test
    void idempotenciaDoCadastroReutilizaMesmaIntencaoSemNovoDocumento() {
        var requisicao = requisicaoSei("SEI 100/2026", "mesma-chave");
        var original = servico.criar(21L, requisicao, null);

        var repetida = servico.criar(21L, requisicao, null);

        assertEquals(original.id(), repetida.id());
        assertEquals(1, bancoDocumentos.size());
        assertEquals(1, bancoEventos.size());
    }

    @Test
    void idempotenciaDoCadastroComparaConteudoBinario() {
        var requisicao = requisicaoAnexo("arquivo-idem");
        servico.criar(21L, requisicao, pdfValido("documento.pdf"));
        var diferente = new MockMultipartFile("arquivo", "documento.pdf", "application/pdf",
                "%PDF-1.4\nconteudo diferente".getBytes());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicao, diferente));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals(1, bancoDocumentos.size());
    }

    @Test
    void idempotenciaNaoConfundeCadastroComRetificacao() {
        servico.criar(21L, requisicaoSei("SEI 100/2026", "outra-operacao"), null);
        var alvo = documentoPublicado("SEI 300/2026", ClassificacaoAcessoDocumento.NEGOCIAL);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.retificarSei(21L, alvo.getId(),
                        new RetificacaoSeiDocumentoRequisicao("SEI 200/2026", "Correção", "outra-operacao")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals(EstadoDocumentoComprobatorio.PUBLICADO, alvo.getEstado());
    }

    @Test
    void idempotenciaDaRetificacaoComparaDocumentoAlvo() {
        var primeiro = documentoPublicado("SEI 100/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        var segundo = documentoPublicado("SEI 200/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        var requisicao = new RetificacaoSeiDocumentoRequisicao("SEI 300/2026", "Correção", "idem-retificar");
        servico.retificarSei(21L, primeiro.getId(), requisicao);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.retificarSei(21L, segundo.getId(), requisicao));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals(EstadoDocumentoComprobatorio.PUBLICADO, segundo.getEstado());
    }

    @Test
    void idempotenciaDaSubstituicaoComparaMotivo() {
        var alvo = documentoPublicado("SEI 100/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        servico.substituir(21L, alvo.getId(),
                new SubstituicaoDocumentoComprobatorioRequisicao("Novo título", null, "SEI 200/2026",
                        "Correção A", "idem-substituir"), null);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.substituir(21L, alvo.getId(),
                        new SubstituicaoDocumentoComprobatorioRequisicao("Novo título", null, "SEI 200/2026",
                                "Correção B", "idem-substituir"), null));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void doc004MesmoSeiPodeReferenciarContratosDiferentes() {
        CadastroDocumentoComprobatorioRequisicao requisicao = requisicaoSei("SEI 12345/2026", "idempotencia-1");
        DocumentoComprobatorioDTO primeiro = servico.criar(contrato.getId(), requisicao, null);

        Contrato segundoContrato = contrato(22L, EstadoNegocialContrato.RASCUNHO);
        when(contratos.findById(22L)).thenReturn(Optional.of(segundoContrato));
        CadastroDocumentoComprobatorioRequisicao segundaRequisicao = requisicaoSei("SEI 12345/2026", "idempotencia-2");
        DocumentoComprobatorioDTO segundo = servico.criar(22L, segundaRequisicao, null);

        assertEquals(primeiro.referenciaSei(), segundo.referenciaSei());
        assertNotEquals(primeiro.grupoLogicoId(), segundo.grupoLogicoId());
        verify(documentos, times(2)).save(any(DocumentoComprobatorio.class));
    }

    @Test
    void doc001FontePendentePermitePublicacaoSemSei() {
        when(documentos.existeFonte(eq(21L), eq(TipoFatoDocumental.CONTRATO_ORIGINAL),
                eq("ORIGINAL"), eq(List.of(EstadoDocumentoComprobatorio.RASCUNHO)))).thenReturn(true);

        assertDoesNotThrow(() -> servico.validarFonteObrigatoria(contrato,
                TipoFatoDocumental.CONTRATO_ORIGINAL, "ORIGINAL", null));
    }

    @Test
    void doc002AditamentoSemSeiNemAnexoEhBloqueado() {
        when(documentos.existeFonte(anyLong(), any(), anyString(), anyCollection())).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.validarFonteObrigatoria(contrato, TipoFatoDocumental.ADITAMENTO,
                        "ADIT-2026-01", " "));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void doc005RetificacaoSeiCriaNovaVersaoEPreservaAnterior() {
        DocumentoComprobatorio anterior = documentoPublicado("SEI 111/2026", ClassificacaoAcessoDocumento.NEGOCIAL);

        DocumentoComprobatorioDTO nova = servico.retificarSei(21L, anterior.getId(),
                new RetificacaoSeiDocumentoRequisicao("SEI 222/2026", "Correção do número", "ret-1"));

        assertEquals(EstadoDocumentoComprobatorio.SUBSTITUIDO, anterior.getEstado());
        assertEquals(2, nova.versaoDocumento());
        assertEquals(anterior.getId(), nova.documentoAnteriorId());
        assertEquals("SEI 222/2026", nova.referenciaSei());
        assertEquals(TipoEventoDocumento.RETIFICACAO_SEI, bancoEventos.get(bancoEventos.size() - 1).getTipoEvento());
    }

    @Test
    void doc006AnexoPosteriorPreservaSeiSemSubstituicaoSilenciosa() {
        DocumentoComprobatorio anterior = documentoPublicado("SEI 333/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        MockMultipartFile pdf = pdfValido("contrato.pdf");

        DocumentoComprobatorioDTO nova = servico.substituir(21L, anterior.getId(),
                new SubstituicaoDocumentoComprobatorioRequisicao(null, null, null,
                        "Anexo recebido posteriormente", "sub-1"), pdf);

        assertEquals(EstadoDocumentoComprobatorio.SUBSTITUIDO, anterior.getEstado());
        assertEquals("SEI 333/2026", nova.referenciaSei());
        assertTrue(nova.downloadDisponivel());
        assertEquals(TipoFonteComprobatoria.ANEXO_E_SEI, nova.tipoFonte());
        assertEquals(TipoEventoDocumento.ANEXO_ACRESCENTADO,
                bancoEventos.get(bancoEventos.size() - 1).getTipoEvento());
    }

    @Test
    void doc007ArquivoPublicadoIncorretoEhInvalidadoSemExclusaoFisica() throws Exception {
        DocumentoComprobatorio publicado = documentoPublicadoComArquivo();
        Path arquivo = diretorio.resolve(publicado.getNomeArmazenado());

        DocumentoComprobatorioDTO resultado = servico.invalidar(21L, publicado.getId(),
                new AcaoDocumentoComprobatorioRequisicao("Arquivo incorreto", null));

        assertEquals(EstadoDocumentoComprobatorio.INVALIDO, resultado.estado());
        assertTrue(Files.exists(arquivo));
        verify(documentos, never()).delete(publicado);
    }

    @Test
    void doc008RascunhoExclusivoSemSeiPodeSerRemovidoFisicamente() throws Exception {
        DocumentoComprobatorio rascunho = documentoPublicadoComArquivo();
        rascunho.setEstado(EstadoDocumentoComprobatorio.RASCUNHO);
        Path arquivo = diretorio.resolve(rascunho.getNomeArmazenado());

        servico.descartarRascunho(21L, rascunho.getId());

        assertFalse(Files.exists(arquivo));
        verify(eventos).deleteByDocumentoId(rascunho.getId());
        verify(documentos).delete(rascunho);
    }

    @Test
    void doc008RascunhoComSeiNaoPodeSerApagado() {
        DocumentoComprobatorio rascunho = documentoPublicado("SEI 444/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        rascunho.setEstado(EstadoDocumentoComprobatorio.RASCUNHO);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.descartarRascunho(21L, rascunho.getId()));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(documentos, never()).delete(rascunho);
    }

    @Test
    void doc010OperacionalNaoAcessaDocumentoRestrito() {
        DocumentoComprobatorio restrito = documentoPublicado("SEI 555/2026",
                ClassificacaoAcessoDocumento.PESSOAL_RESTRITO);
        usuarioAtual = usuario(32L, "operacional", "PERFIL_OPERACIONAL", gestao);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.buscar(21L, restrito.getId(), "Consulta do vínculo"));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(acessosRegistrados);
    }

    @Test
    void doc010GestorResponsavelAcessaRestritoComFinalidadeEAuditoria() {
        DocumentoComprobatorio restrito = documentoPublicado("SEI 666/2026",
                ClassificacaoAcessoDocumento.PESSOAL_RESTRITO);

        DocumentoComprobatorioDTO resultado = servico.buscar(21L, restrito.getId(), "Conferência contratual");

        assertEquals(restrito.getId(), resultado.id());
        ArgumentCaptor<AcessoDocumentoRestrito> captor = ArgumentCaptor.forClass(AcessoDocumentoRestrito.class);
        verify(acessosRegistrados).save(captor.capture());
        assertEquals("Conferência contratual", captor.getValue().getFinalidade());
        assertNull(captor.getValue().getPermissaoId());
    }

    @Test
    void doc010PresidenciaSomenteAcessaRestritoComPermissaoExplicitaEAuditoria() {
        DocumentoComprobatorio restrito = documentoPublicado("SEI 777/2026",
                ClassificacaoAcessoDocumento.PESSOAL_RESTRITO);
        usuarioAtual = usuario(33L, "presidencia", "PERFIL_PRESIDENTE", null);

        ResponseStatusException negado = assertThrows(ResponseStatusException.class,
                () -> servico.buscar(21L, restrito.getId(), "Análise institucional"));
        assertEquals(HttpStatus.FORBIDDEN, negado.getStatusCode());

        PermissaoDocumentoRestrito permissao = new PermissaoDocumentoRestrito();
        permissao.setId(88L);
        permissao.setGrupoLogicoId(restrito.getGrupoLogicoId());
        permissao.setDocumentoReferencia(restrito);
        permissao.setUsuarioBeneficiario(usuarioAtual);
        permissao.setFinalidade("Análise institucional");
        permissao.setJustificativa("Necessidade formal");
        permissao.setAutorizacaoAdministrativa("AUT-2026-9");
        when(permissoes.buscarAtivas(eq(restrito.getGrupoLogicoId()), eq(33L), any()))
                .thenReturn(List.of(permissao));

        assertDoesNotThrow(() -> servico.buscar(21L, restrito.getId(), "Análise institucional"));
        ArgumentCaptor<AcessoDocumentoRestrito> captor = ArgumentCaptor.forClass(AcessoDocumentoRestrito.class);
        verify(acessosRegistrados).save(captor.capture());
        assertEquals(88L, captor.getValue().getPermissaoId());
    }

    @Test
    void uploadComMimeIncompativelEhRecusado() {
        MockMultipartFile falsoPdf = new MockMultipartFile("arquivo", "falso.pdf", "image/jpg",
                "%PDF-1.4\nconteudo".getBytes());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicaoAnexo("mime-1"), falsoPdf));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, erro.getStatusCode());
    }

    @Test
    void uploadComAssinaturaBinariaInvalidaEhRecusado() {
        MockMultipartFile falsoPdf = new MockMultipartFile("arquivo", "falso.pdf", "application/pdf",
                "nao-e-pdf".getBytes());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(21L, requisicaoAnexo("assinatura-1"), falsoPdf));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, erro.getStatusCode());
    }

    @Test
    void downloadComArquivoAdulteradoEhBloqueadoPorChecksum() throws Exception {
        DocumentoComprobatorio publicado = documentoPublicadoComArquivo();
        Files.writeString(diretorio.resolve(publicado.getNomeArmazenado()), "conteúdo adulterado",
                StandardOpenOption.TRUNCATE_EXISTING);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.baixar(21L, publicado.getId(), null));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void rascunhoNaoEhExpostoAConsultaPresidencial() {
        DocumentoComprobatorio rascunho = documentoPublicado("SEI 888/2026", ClassificacaoAcessoDocumento.NEGOCIAL);
        rascunho.setEstado(EstadoDocumentoComprobatorio.RASCUNHO);
        usuarioAtual = usuario(33L, "presidencia", "PERFIL_PRESIDENTE", null);

        assertTrue(servico.listar(21L, null).isEmpty());
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.buscar(21L, rascunho.getId(), null));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    private DocumentoComprobatorio documentoPublicado(String sei, ClassificacaoAcessoDocumento classificacao) {
        DocumentoComprobatorio documento = new DocumentoComprobatorio();
        documento.setId(sequenciaDocumento.incrementAndGet());
        documento.setContrato(contrato);
        documento.setTipoFato(TipoFatoDocumental.CONTRATO_ORIGINAL);
        documento.setFatoId(contrato.getId());
        documento.setFatoChave("ORIGINAL");
        documento.setGrupoLogicoId(UUID.randomUUID().toString());
        documento.setTitulo("Contrato original");
        documento.setReferenciaSei(sei);
        documento.setClassificacaoAcesso(classificacao);
        documento.setEstado(EstadoDocumentoComprobatorio.PUBLICADO);
        documento.setPublicadoEm(Instant.now());
        documento.setPublicadoPorId(31L);
        documento.setPublicadoPorUsername("gestor.local");
        bancoDocumentos.put(documento.getId(), documento);
        return documento;
    }

    private DocumentoComprobatorio documentoPublicadoComArquivo() throws Exception {
        byte[] conteudo = "%PDF-1.4\nobjeto de teste".getBytes();
        DocumentoComprobatorio documento = documentoPublicado(null, ClassificacaoAcessoDocumento.NEGOCIAL);
        String nome = UUID.randomUUID() + ".pdf";
        Files.write(diretorio.resolve(nome), conteudo);
        documento.setNomeOriginalArquivo("contrato.pdf");
        documento.setNomeArmazenado(nome);
        documento.setTipoMime("application/pdf");
        documento.setTamanhoBytes((long) conteudo.length);
        documento.setSha256Arquivo(sha256(conteudo));
        return documento;
    }

    private CadastroDocumentoComprobatorioRequisicao requisicaoSei(String sei, String idempotencia) {
        return new CadastroDocumentoComprobatorioRequisicao(TipoFatoDocumental.CONTRATO_ORIGINAL, null,
                "ORIGINAL", "Contrato original", null, sei, ClassificacaoAcessoDocumento.NEGOCIAL,
                LocalDate.of(2026, 9, 9), idempotencia);
    }

    private CadastroDocumentoComprobatorioRequisicao requisicaoAnexo(String idempotencia) {
        return new CadastroDocumentoComprobatorioRequisicao(TipoFatoDocumental.CONTRATO_ORIGINAL, null,
                "ORIGINAL", "Contrato original", null, null, ClassificacaoAcessoDocumento.NEGOCIAL,
                LocalDate.of(2026, 9, 9), idempotencia);
    }

    private MockMultipartFile pdfValido(String nome) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", "%PDF-1.4\nconteudo".getBytes());
    }

    private Contrato contrato(Long id, EstadoNegocialContrato estado) {
        Contrato valor = new Contrato();
        valor.setId(id);
        valor.setGestaoLocal(gestao);
        valor.setEstadoNegocial(estado);
        return valor;
    }

    private UsuarioAplicacao usuario(Long id, String username, String roles, GestaoLocal gestaoLocal) {
        UsuarioAplicacao valor = new UsuarioAplicacao();
        valor.setId(id);
        valor.setUsername(username);
        valor.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((roles).replace("PERFIL_", "")));
        valor.setGestaoLocal(gestaoLocal);
        return valor;
    }

    private boolean possuiPapel(UsuarioAplicacao usuario, String papel) {
        return perfis(usuario).contains(papel);
    }

    private Set<String> perfis(UsuarioAplicacao usuario) {
        if (usuario == null || usuario.getPerfil() == null) return Set.of();
        return Set.of(usuario.getPerfil().autoridade());
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
