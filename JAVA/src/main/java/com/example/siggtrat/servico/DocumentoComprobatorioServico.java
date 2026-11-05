package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.*;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.documento.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

/**
 * O banco guarda metadados/versionamento e o armazenamento físico usa nomes
 * aleatórios, checksum e compensação de rollback.
 */
@Service
@Transactional(readOnly = true)
public class DocumentoComprobatorioServico {
    private static final Set<String> EXTENSOES = Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx");
    private static final Map<String, String> MIME_POR_EXTENSAO = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );

    private record ArquivoPreparado(Path caminho, String nomeOriginal, String nomeArmazenado,
                                    String tipoMime, long tamanho, String sha256) {}

    private final DocumentoComprobatorioRepositorio documentoRepositorio;
    private final EventoDocumentoComprobatorioRepositorio eventoRepositorio;
    private final PermissaoDocumentoRestritoRepositorio permissaoRepositorio;
    private final AcessoDocumentoRestritoRepositorio acessoDocumentoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final UsuarioAplicacaoRepositorio usuarioRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final Path armazenamento;
    private final Path temporarios;
    private final long tamanhoMaximo;
    private VinculoFatoDocumentalServico vinculoFatoDocumental;
    private GestaoContratoTemporalServico gestaoContratoTemporal;
    private PermissaoExcepcionalServico permissoesExcepcionais;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void configurarPermissoesDeConferencia(PermissaoExcepcionalServico servico) {
        this.permissoesExcepcionais=servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarVinculoFatoDocumental(VinculoFatoDocumentalServico servico) {
        this.vinculoFatoDocumental = servico;
    }

    public DocumentoComprobatorioServico(
            DocumentoComprobatorioRepositorio documentoRepositorio,
            EventoDocumentoComprobatorioRepositorio eventoRepositorio,
            PermissaoDocumentoRestritoRepositorio permissaoRepositorio,
            AcessoDocumentoRestritoRepositorio acessoDocumentoRepositorio,
            ContratoRepositorio contratoRepositorio,
            UsuarioAplicacaoRepositorio usuarioRepositorio,
            AcessoOrganizacionalServico acesso,
            PropriedadesArmazenamentoArquivo armazenamentoProperties,
            PropriedadesEnvioArquivo envioProperties) {
        this.documentoRepositorio = documentoRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.permissaoRepositorio = permissaoRepositorio;
        this.acessoDocumentoRepositorio = acessoDocumentoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.acesso = acesso;
        this.armazenamento = armazenamentoProperties.uploadDir().toAbsolutePath().normalize();
        this.temporarios = armazenamento.resolve(".staging").normalize();
        this.tamanhoMaximo = envioProperties.maxFileSizeBytes();
    }

    @Transactional
    public List<DocumentoComprobatorioDTO> listar(Long contratoId, String finalidade) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Contrato contrato = obterContratoParaLeitura(contratoId);
        acesso.revalidarAposEspera(usuario);
        acesso.exigirAcessoContrato(usuario, contrato);
        if (acesso.ehOperacional(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário Operacional não acessa documentos deste módulo.");
        }
        List<DocumentoComprobatorioDTO> resultado = new ArrayList<>();
        for (DocumentoComprobatorio documento : documentoRepositorio.findByContratoIdOrderByRegistradoEmAscIdAsc(contratoId)) {
            if (documento.getEstado() == EstadoDocumentoComprobatorio.RASCUNHO
                    && !gestorResponsavel(usuario, documento.getContrato())) {
                continue;
            }
            if (documento.getClassificacaoAcesso() == ClassificacaoAcessoDocumento.PESSOAL_RESTRITO) {
                if (finalidade == null || finalidade.isBlank()) continue;
                try {
                    autorizarEAuditar(documento, usuario, finalidade, TipoAcessoDocumento.CONSULTA_METADADOS);
                } catch (ResponseStatusException ex) {
                    if (ex.getStatusCode() == HttpStatus.FORBIDDEN) continue;
                    throw ex;
                }
            }
            resultado.add(paraDTO(documento));
        }
        return List.copyOf(resultado);
    }

    @Transactional
    public DocumentoComprobatorioDTO buscar(Long contratoId, Long documentoId, String finalidade) {
        obterContratoParaLeitura(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        autorizarEAuditar(documento, usuario, finalidade, TipoAcessoDocumento.CONSULTA_METADADOS);
        return paraDTO(documento);
    }

    /** Leitura interna limitada à incorporação de prova; não amplia os GETs documentais. */
    @Transactional
    public DocumentoComprobatorioDTO conferirFonteHistorica(Long contratoId,Long documentoId,String finalidade) {
        Contrato contrato=obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio documento=obterNoContrato(documentoId,contratoId);
        UsuarioAplicacao usuario=acesso.usuarioAtual();
        if(acesso.podeAcessarContrato(usuario,contrato)) {
            autorizarEAuditar(documento,usuario,finalidade,TipoAcessoDocumento.CONSULTA_METADADOS);
        } else {
            if(permissoesExcepcionais==null||gestaoContratoTemporal==null
                    ||gestaoContratoTemporal.gestaoAtual(contrato)!=null||!acesso.ehGestaoLocal(usuario)
                    ||usuario.getGestaoLocal()==null||!Boolean.TRUE.equals(usuario.getGestaoLocal().getAtivo()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Fonte não encontrada no escopo de conferência.");
            var concessao=permissoesExcepcionais.exigirERegistrarUso(usuario,
                    com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA,
                    contrato,usuario.getGestaoLocal(),"Conferir fonte de origem contratual não demonstrada",
                    "DocumentoComprobatorio",documentoId);
            if(concessao.getGestaoLocal()==null
                    ||!Objects.equals(concessao.getGestaoLocal().getId(),usuario.getGestaoLocal().getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A conferência exige designação administrativa explícita.");
            // A designação de conferência não equivale a custódia histórica nem
            // concede acesso pessoal: fonte restrita ainda exige concessão documental.
            autorizarConteudoEAuditar(documento,usuario,finalidade,TipoAcessoDocumento.CONSULTA_METADADOS,true);
        }
        if(documento.getEstado()!=EstadoDocumentoComprobatorio.PUBLICADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A fonte deve estar publicada e válida.");
        if(documento.possuiArquivo())conferirArquivo(documento);
        return paraDTO(documento);
    }

    @Transactional
    public List<EventoDocumentoComprobatorioDTO> listarEventos(Long contratoId, Long documentoId, String finalidade) {
        obterContratoParaLeitura(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        autorizarEAuditar(documento, usuario, finalidade, TipoAcessoDocumento.CONSULTA_HISTORICO);
        List<EventoDocumentoComprobatorio> eventos = eventoRepositorio
                .findByGrupoLogicoIdOrderByRegistradoEmAscIdAsc(documento.getGrupoLogicoId());
        if (!cadeiaIntegra(eventos)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A cadeia de integridade do documento não confere.");
        }
        return eventos.stream().map(this::paraDTO).toList();
    }

    @Transactional
    public EvidenciasIntegridadeDocumentoDTO consultarEvidenciasIntegridade(
            Long contratoId, Long documentoId, String finalidade) {
        obterContratoParaLeitura(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        autorizarEAuditar(documento, usuario, finalidade, TipoAcessoDocumento.CONSULTA_HISTORICO);
        List<EventoDocumentoComprobatorio> eventos = eventoRepositorio
                .findByGrupoLogicoIdOrderByRegistradoEmAscIdAsc(documento.getGrupoLogicoId());
        List<EvidenciasIntegridadeDocumentoDTO.Evento> evidencias = new ArrayList<>();
        String anterior = null;
        boolean integra = !eventos.isEmpty();
        for (EventoDocumentoComprobatorio evento : eventos) {
            String calculado = calcularHashEvento(evento);
            boolean hashConfere = Objects.equals(evento.getHashEvento(), calculado);
            boolean encadeamentoConfere = Objects.equals(anterior, evento.getHashAnterior());
            integra &= hashConfere && encadeamentoConfere;
            evidencias.add(new EvidenciasIntegridadeDocumentoDTO.Evento(paraDTO(evento),
                    evento.getHashEvento(), calculado, hashConfere, encadeamentoConfere));
            anterior = evento.getHashEvento();
        }
        integra &= terminaisConferem(documento.getGrupoLogicoId(), eventos);
        return new EvidenciasIntegridadeDocumentoDTO(documento.getId(), documento.getGrupoLogicoId(),
                integra ? "INTEGRA" : "INCONSISTENTE_OU_NAO_VERIFICAVEL", integra, evidencias);
    }

    @Transactional
    public BaixaDocumentoComprobatorioDTO baixar(Long contratoId, Long documentoId, String finalidade) {
        obterContratoParaLeitura(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        autorizarEAuditar(documento, usuario, finalidade, TipoAcessoDocumento.DOWNLOAD);
        Path caminho=conferirArquivo(documento);
        return new BaixaDocumentoComprobatorioDTO(caminho, documento.getNomeOriginalArquivo(),
                documento.getTipoMime(), Optional.ofNullable(documento.getTamanhoBytes()).orElse(0L));
    }

    private Path conferirArquivo(DocumentoComprobatorio documento) {
        if (!documento.possuiArquivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta fonte não possui arquivo anexado.");
        }
        Path caminho = resolverArquivo(documento.getNomeArmazenado());
        if (!Files.isRegularFile(caminho)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O arquivo físico não foi localizado; a inconsistência foi preservada para auditoria.");
        }
        String hashAtual = hashArquivo(caminho);
        if (!hashAtual.equalsIgnoreCase(documento.getSha256Arquivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O checksum do arquivo não confere; download bloqueado por integridade.");
        }
        return caminho;
    }

    @Transactional
    public DocumentoComprobatorioDTO criar(Long contratoId, CadastroDocumentoComprobatorioRequisicao req,
                                            MultipartFile arquivo) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Contrato contrato = obterContratoParaAtualizacao(contratoId);
        exigirEscritaDocumental(usuario, contrato);
        validarCadastro(req, arquivo);
        if (vinculoFatoDocumental != null) vinculoFatoDocumental.validar(contrato, req);
        String assinaturaArquivo = assinaturaArquivo(arquivo);
        String hashIntencao = hashIntencao("CADASTRO", contratoId, usuario.getId(), req.tipoFato(),
                req.fatoId(), DocumentoComprobatorio.normalizarChave(chaveFato(req.tipoFato(), req.fatoChave(), req.fatoId())),
                req.titulo(), req.descricao(), req.referenciaSei(), req.classificacaoAcesso(), req.dataEfetiva(), assinaturaArquivo);
        // A mesma intenção tem identidade também quando o cliente omite a chave.
        // O lock do Contrato já foi adquirido, e o hash inclui ator, escopo e bytes.
        String chaveCadastro = vazio(req.chaveIdempotencia()) ? "AUTO-CAD:" + hashIntencao : req.chaveIdempotencia();
        DocumentoComprobatorio idempotente = localizarIdempotente(chaveCadastro, contratoId, hashIntencao,
                d -> cadastroLegadoCoincide(d, req, usuario, assinaturaArquivo));
        if (idempotente != null) return paraDTO(idempotente);

        ArquivoPreparado preparado = prepararArquivo(arquivo);
        DocumentoComprobatorio documento = novoDocumento(contrato, req, preparado, usuario);
        documento.setChaveIdempotencia(chaveCadastro.trim());
        documento.setHashIntencao(hashIntencao);
        boolean fatoJaPublicado = req.fatoId() != null
                && contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO;
        documento.setEstado(fatoJaPublicado ? EstadoDocumentoComprobatorio.PUBLICADO
                : EstadoDocumentoComprobatorio.RASCUNHO);
        if (fatoJaPublicado) marcarPublicacao(documento, usuario);
        DocumentoComprobatorio salvo = salvarComCompensacao(documento, preparado);
        registrarEvento(salvo, fatoJaPublicado ? TipoEventoDocumento.PUBLICACAO : TipoEventoDocumento.CRIACAO_RASCUNHO,
                usuario, fatoJaPublicado ? "Fonte acrescentada a fato já publicado" : "Fonte preparada para publicação",
                null, salvo.getEstado(), null, salvo.getReferenciaSei(), null, salvo.getSha256Arquivo());
        documentoRepositorio.flush();
        return paraDTO(salvo);
    }

    @Transactional
    public DocumentoComprobatorioDTO retificarSei(Long contratoId, Long documentoId,
                                                   RetificacaoSeiDocumentoRequisicao req) {
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio anterior = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirEscritaDocumental(usuario, anterior.getContrato());
        if (req == null || vazio(req.novaReferenciaSei()) || vazio(req.motivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nova referência SEI e motivo da retificação são obrigatórios.");
        }
        String hashIntencao = hashIntencao("RETIFICACAO_SEI", contratoId, documentoId, usuario.getId(),
                req.novaReferenciaSei(), req.motivo());
        DocumentoComprobatorio idempotente = localizarIdempotente(req.chaveIdempotencia(), contratoId, hashIntencao,
                d -> versaoLegadaCoincide(d, anterior, usuario, req.motivo(), TipoEventoDocumento.RETIFICACAO_SEI)
                        && iguais(d.getReferenciaSei(), req.novaReferenciaSei()));
        if (idempotente != null) return paraDTO(idempotente);
        if (DocumentoComprobatorio.normalizarReferencia(req.novaReferenciaSei())
                .equals(DocumentoComprobatorio.normalizarReferencia(anterior.getReferenciaSei()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A nova referência SEI é igual à versão atual.");
        }
        exigirVersionavel(anterior);
        DocumentoComprobatorio nova = copiarVersao(anterior, usuario, req.chaveIdempotencia());
        nova.setHashIntencao(hashIntencao);
        nova.setReferenciaSei(req.novaReferenciaSei());
        anterior.setEstado(EstadoDocumentoComprobatorio.SUBSTITUIDO);
        documentoRepositorio.save(anterior);
        nova = documentoRepositorio.save(nova);
        registrarEvento(nova, TipoEventoDocumento.RETIFICACAO_SEI, usuario, req.motivo(),
                EstadoDocumentoComprobatorio.PUBLICADO, nova.getEstado(), anterior.getReferenciaSei(),
                nova.getReferenciaSei(), anterior.getSha256Arquivo(), nova.getSha256Arquivo());
        documentoRepositorio.flush();
        return paraDTO(nova);
    }

    @Transactional
    public DocumentoComprobatorioDTO substituir(Long contratoId, Long documentoId,
                                                 SubstituicaoDocumentoComprobatorioRequisicao req,
                                                 MultipartFile arquivo) {
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio anterior = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirEscritaDocumental(usuario, anterior.getContrato());
        if (req == null || vazio(req.motivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo da substituição é obrigatório.");
        }
        String assinaturaArquivo = assinaturaArquivo(arquivo);
        String hashIntencao = hashIntencao("SUBSTITUICAO", contratoId, documentoId, usuario.getId(),
                req.titulo(), req.descricao(), req.referenciaSei(), req.motivo(), assinaturaArquivo);
        DocumentoComprobatorio idempotente = localizarIdempotente(req.chaveIdempotencia(), contratoId, hashIntencao,
                d -> substituicaoLegadaCoincide(d, anterior, req, usuario, assinaturaArquivo));
        if (idempotente != null) return paraDTO(idempotente);
        exigirVersionavel(anterior);
        ArquivoPreparado preparado = prepararArquivo(arquivo);
        String referencia = vazio(req.referenciaSei()) ? anterior.getReferenciaSei() : req.referenciaSei();
        if (preparado == null && vazio(referencia)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A nova versão deve possuir anexo, referência SEI ou ambos.");
        }
        DocumentoComprobatorio nova = copiarVersao(anterior, usuario, req.chaveIdempotencia());
        nova.setHashIntencao(hashIntencao);
        nova.setTitulo(vazio(req.titulo()) ? anterior.getTitulo() : req.titulo());
        nova.setDescricao(req.descricao() == null ? anterior.getDescricao() : req.descricao());
        nova.setReferenciaSei(referencia);
        if (preparado != null) aplicarArquivo(nova, preparado);
        anterior.setEstado(EstadoDocumentoComprobatorio.SUBSTITUIDO);
        documentoRepositorio.save(anterior);
        nova = salvarComCompensacao(nova, preparado);
        registrarEvento(nova, preparado != null && anterior.possuiSei() && !anterior.possuiArquivo()
                        ? TipoEventoDocumento.ANEXO_ACRESCENTADO : TipoEventoDocumento.SUBSTITUICAO,
                usuario, req.motivo(), EstadoDocumentoComprobatorio.PUBLICADO, nova.getEstado(),
                anterior.getReferenciaSei(), nova.getReferenciaSei(), anterior.getSha256Arquivo(), nova.getSha256Arquivo());
        documentoRepositorio.flush();
        return paraDTO(nova);
    }

    @Transactional
    public DocumentoComprobatorioDTO invalidar(Long contratoId, Long documentoId,
                                                AcaoDocumentoComprobatorioRequisicao req) {
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirEscritaDocumental(usuario, documento.getContrato());
        if (req == null || vazio(req.motivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo da invalidação é obrigatório.");
        }
        if (documento.getEstado() != EstadoDocumentoComprobatorio.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente uma versão publicada pode ser invalidada; rascunho exclusivo deve ser descartado.");
        }
        documento.setEstado(EstadoDocumentoComprobatorio.INVALIDO);
        documentoRepositorio.save(documento);
        registrarEvento(documento, TipoEventoDocumento.INVALIDACAO, usuario, req.motivo(),
                EstadoDocumentoComprobatorio.PUBLICADO, EstadoDocumentoComprobatorio.INVALIDO,
                documento.getReferenciaSei(), documento.getReferenciaSei(), documento.getSha256Arquivo(), documento.getSha256Arquivo());
        // O evento atualiza a âncora auditável e incrementa @Version no flush.
        // Materializar o DTO antes dele expõe uma versão já obsoleta no commit.
        documentoRepositorio.flush();
        return paraDTO(documento);
    }

    @Transactional
    public void descartarRascunho(Long contratoId, Long documentoId) {
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirEscritaDocumental(usuario, documento.getContrato());
        if (documento.getEstado() != EstadoDocumentoComprobatorio.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento publicado nunca é excluído fisicamente.");
        }
        if (documento.possuiSei()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rascunho com referência SEI possui valor auditável externo e não pode ser apagado.");
        }
        descartar(documento);
    }

    @Transactional
    public PermissaoDocumentoRestritoDTO concederAcesso(Long contratoId, Long documentoId,
                                                         ConcessaoAcessoDocumentoRequisicao req) {
        // Permissões também acrescentam fatos à cadeia documental. Serializar
        // antes da primeira leitura impede dois eventos com o mesmo antecessor.
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao responsavel = acesso.usuarioAtual();
        exigirAdministrador(responsavel);
        if (documento.getClassificacaoAcesso() != ClassificacaoAcessoDocumento.PESSOAL_RESTRITO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permissão específica aplica-se apenas a documento Pessoal/Restrito.");
        }
        if (req == null || req.usuarioId() == null || vazio(req.finalidade()) || vazio(req.justificativa())
                || vazio(req.autorizacaoAdministrativa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Beneficiário, finalidade, justificativa e autorização administrativa são obrigatórios.");
        }
        if (req.validoAte() != null && !req.validoAte().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A validade da permissão deve estar no futuro.");
        }
        UsuarioAplicacao beneficiario = usuarioRepositorio.findById(req.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário beneficiário não encontrado."));
        // Antecipar a espera da FK do beneficiário antes de revalidar o executor.
        // O lock de leitura mantém esse alvo estável até o INSERT da permissão.
        if (entityManager != null)
            entityManager.refresh(beneficiario, jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        acesso.revalidarAposEspera(responsavel);
        exigirAdministrador(responsavel);
        if (acesso.ehOperacional(beneficiario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário Operacional não recebe acesso a documento Pessoal/Restrito neste módulo.");
        }
        PermissaoDocumentoRestrito permissao = new PermissaoDocumentoRestrito();
        permissao.setDocumentoReferencia(documento);
        permissao.setGrupoLogicoId(documento.getGrupoLogicoId());
        permissao.setUsuarioBeneficiario(beneficiario);
        permissao.setFinalidade(req.finalidade());
        permissao.setJustificativa(req.justificativa());
        permissao.setAutorizacaoAdministrativa(req.autorizacaoAdministrativa());
        permissao.setValidoAte(req.validoAte());
        permissao.setConcedidoPorId(responsavel.getId());
        permissao.setConcedidoPorUsername(responsavel.getUsername());
        permissao = permissaoRepositorio.save(permissao);
        registrarEvento(documento, TipoEventoDocumento.CONCESSAO_ACESSO, responsavel,
                "Permissão " + permissao.getId() + " concedida para " + beneficiario.getUsername(),
                documento.getEstado(), documento.getEstado(), documento.getReferenciaSei(), documento.getReferenciaSei(),
                documento.getSha256Arquivo(), documento.getSha256Arquivo());
        return paraDTO(permissao);
    }

    @Transactional
    public PermissaoDocumentoRestritoDTO revogarAcesso(Long contratoId, Long documentoId, Long permissaoId,
                                                        AcaoDocumentoComprobatorioRequisicao req) {
        obterContratoParaAtualizacao(contratoId);
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao responsavel = acesso.usuarioAtual();
        exigirAdministrador(responsavel);
        if (req == null || vazio(req.motivo()) || vazio(req.autorizacaoAdministrativa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Motivo e autorização administrativa da revogação são obrigatórios.");
        }
        PermissaoDocumentoRestrito permissao = permissaoRepositorio.findById(permissaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada."));
        // Esperar pelo alvo antes de mutá-lo ou acrescentar evento à cadeia.
        if (entityManager != null)
            entityManager.refresh(permissao, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        acesso.revalidarAposEspera(responsavel);
        exigirAdministrador(responsavel);
        if (!Objects.equals(permissao.getGrupoLogicoId(), documento.getGrupoLogicoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não pertence ao documento informado.");
        }
        if (!Boolean.TRUE.equals(permissao.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permissão já está revogada.");
        }
        permissao.setAtivo(false);
        permissao.setRevogadoEm(Instant.now());
        permissao.setRevogadoPorId(responsavel.getId());
        permissao.setRevogadoPorUsername(responsavel.getUsername());
        permissao.setMotivoRevogacao(req.motivo());
        permissao = permissaoRepositorio.save(permissao);
        registrarEvento(documento, TipoEventoDocumento.REVOGACAO_ACESSO, responsavel,
                "Permissão " + permissao.getId() + " revogada: " + req.motivo(),
                documento.getEstado(), documento.getEstado(), documento.getReferenciaSei(), documento.getReferenciaSei(),
                documento.getSha256Arquivo(), documento.getSha256Arquivo());
        return paraDTO(permissao);
    }

    public List<PermissaoDocumentoRestritoDTO> listarPermissoes(Long contratoId, Long documentoId) {
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirAdministradorOuGestorResponsavel(usuario, documento.getContrato());
        return permissaoRepositorio.findByGrupoLogicoIdOrderByConcedidoEmAscIdAsc(documento.getGrupoLogicoId())
                .stream().map(this::paraDTO).toList();
    }

    public List<AcessoDocumentoRestritoDTO> listarAcessos(Long contratoId, Long documentoId) {
        DocumentoComprobatorio documento = obterNoContrato(documentoId, contratoId);
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirAdministradorOuGestorResponsavel(usuario, documento.getContrato());
        return acessoDocumentoRepositorio.findByDocumentoGrupoLogicoIdOrderByAcessadoEmAscIdAsc(documento.getGrupoLogicoId())
                .stream().map(this::paraDTO).toList();
    }

    /** Valida SEI informado no comando ou documento previamente preparado. */
    public void validarFonteObrigatoria(Contrato contrato, TipoFatoDocumental tipoFato,
                                        String fatoChave, String referenciaSei) {
        if (!vazio(referenciaSei)) return;
        String chave = chaveFato(tipoFato, fatoChave, null);
        boolean existe = documentoRepositorio.existeFonte(contrato.getId(), tipoFato,
                DocumentoComprobatorio.normalizarChave(chave),
                List.of(EstadoDocumentoComprobatorio.RASCUNHO));
        if (!existe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O fato exige fonte comprobatória por anexo, referência SEI ou ambos.");
        }
    }

    /** Publica rascunhos preparados e materializa a referência SEI do comando. */
    @Transactional
    public void publicarFontesDoFato(Contrato contrato, TipoFatoDocumental tipoFato, String fatoChave,
                                     Long fatoId, String referenciaSei, LocalDate dataEfetiva,
                                     UsuarioAplicacao usuario) {
        String chave = chaveFato(tipoFato, fatoChave, fatoId);
        String chaveNormalizada = DocumentoComprobatorio.normalizarChave(chave);
        List<DocumentoComprobatorio> rascunhos = documentoRepositorio
                .findByContratoIdAndTipoFatoAndFatoChaveNormalizadaAndEstadoOrderByRegistradoEmAscIdAsc(
                        contrato.getId(), tipoFato, chaveNormalizada, EstadoDocumentoComprobatorio.RASCUNHO);
        boolean seiJaRepresentado = false;
        String seiNormalizado = DocumentoComprobatorio.normalizarReferencia(referenciaSei);
        for (DocumentoComprobatorio documento : rascunhos) {
            documento.setFatoId(fatoId);
            documento.setEstado(EstadoDocumentoComprobatorio.PUBLICADO);
            marcarPublicacao(documento, usuario);
            documentoRepositorio.save(documento);
            registrarEvento(documento, TipoEventoDocumento.PUBLICACAO, usuario, "Fonte vinculada ao fato publicado",
                    EstadoDocumentoComprobatorio.RASCUNHO, EstadoDocumentoComprobatorio.PUBLICADO,
                    documento.getReferenciaSei(), documento.getReferenciaSei(), documento.getSha256Arquivo(), documento.getSha256Arquivo());
            seiJaRepresentado |= seiNormalizado != null && seiNormalizado.equals(documento.getReferenciaSeiNormalizada());
        }
        if (!vazio(referenciaSei) && !seiJaRepresentado) {
            String materialIdempotencia = tipoFato + "|" + contrato.getId() + "|" + fatoId + "|" + seiNormalizado;
            String idempotencia = "AUTO:" + UtilitarioHashSeguranca.sha256Hexadecimal(materialIdempotencia);
            if (documentoRepositorio.findByChaveIdempotencia(idempotencia).isEmpty()) {
                CadastroDocumentoComprobatorioRequisicao req = new CadastroDocumentoComprobatorioRequisicao(
                        tipoFato, fatoId, chave, "Fonte SEI — " + tipoFato.name(),
                        "Referência SEI registrada junto ao fato negocial", referenciaSei,
                        ClassificacaoAcessoDocumento.NEGOCIAL, dataEfetiva, idempotencia);
                DocumentoComprobatorio documento = novoDocumento(contrato, req, null, usuario);
                documento.setEstado(EstadoDocumentoComprobatorio.PUBLICADO);
                marcarPublicacao(documento, usuario);
                documento = documentoRepositorio.save(documento);
                registrarEvento(documento, TipoEventoDocumento.PUBLICACAO, usuario, "Referência SEI do fato publicado",
                        null, EstadoDocumentoComprobatorio.PUBLICADO, null, referenciaSei, null, null);
            }
        }
    }

    /** Permite descarte do Contrato somente quando seus documentos são rascunhos locais sem valor SEI externo. */
    @Transactional
    public void descartarRascunhosExclusivos(Contrato contrato, UsuarioAplicacao usuario) {
        List<DocumentoComprobatorio> documentos = documentoRepositorio
                .findByContratoIdOrderByRegistradoEmAscIdAsc(contrato.getId());
        if (documentos.stream().anyMatch(d -> d.getEstado() != EstadoDocumentoComprobatorio.RASCUNHO || d.possuiSei())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O rascunho possui documento publicado ou referência SEI com valor auditável e não pode ser descartado.");
        }
        for (DocumentoComprobatorio documento : documentos) descartar(documento);
    }

    public boolean cadeiaHashIntegra(String grupoLogicoId) {
        List<EventoDocumentoComprobatorio> eventos = eventoRepositorio
                .findByGrupoLogicoIdOrderByRegistradoEmAscIdAsc(grupoLogicoId);
        return cadeiaIntegra(eventos) && terminaisConferem(grupoLogicoId, eventos);
    }

    private DocumentoComprobatorio novoDocumento(Contrato contrato, CadastroDocumentoComprobatorioRequisicao req,
                                                  ArquivoPreparado preparado, UsuarioAplicacao usuario) {
        DocumentoComprobatorio documento = new DocumentoComprobatorio();
        documento.setQuantidadeEventosAuditoria(0L);
        documento.setContrato(contrato);
        documento.setTipoFato(req.tipoFato());
        documento.setFatoId(req.fatoId());
        documento.setFatoChave(chaveFato(req.tipoFato(), req.fatoChave(), req.fatoId()));
        documento.setGrupoLogicoId(UUID.randomUUID().toString());
        documento.setTitulo(req.titulo());
        documento.setDescricao(req.descricao());
        documento.setReferenciaSei(req.referenciaSei());
        documento.setClassificacaoAcesso(req.classificacaoAcesso());
        documento.setDataEfetiva(req.dataEfetiva());
        documento.setChaveIdempotencia(limpar(req.chaveIdempotencia()));
        documento.setRegistradoPorId(usuario == null ? null : usuario.getId());
        documento.setRegistradoPorUsername(usuario == null ? ConstantesDominio.USUARIO_SISTEMA : usuario.getUsername());
        aplicarArquivo(documento, preparado);
        return documento;
    }

    private DocumentoComprobatorio copiarVersao(DocumentoComprobatorio anterior, UsuarioAplicacao usuario,
                                                 String chaveIdempotencia) {
        DocumentoComprobatorio nova = new DocumentoComprobatorio();
        nova.setQuantidadeEventosAuditoria(0L);
        nova.setContrato(anterior.getContrato());
        nova.setTipoFato(anterior.getTipoFato());
        nova.setFatoId(anterior.getFatoId());
        nova.setFatoChave(anterior.getFatoChave());
        nova.setGrupoLogicoId(anterior.getGrupoLogicoId());
        nova.setVersaoDocumento(Optional.ofNullable(anterior.getVersaoDocumento()).orElse(1) + 1);
        nova.setDocumentoAnterior(anterior);
        nova.setEstado(anterior.getEstado() == EstadoDocumentoComprobatorio.RASCUNHO
                ? EstadoDocumentoComprobatorio.RASCUNHO : EstadoDocumentoComprobatorio.PUBLICADO);
        nova.setClassificacaoAcesso(anterior.getClassificacaoAcesso());
        nova.setTitulo(anterior.getTitulo());
        nova.setDescricao(anterior.getDescricao());
        nova.setReferenciaSei(anterior.getReferenciaSei());
        nova.setNomeOriginalArquivo(anterior.getNomeOriginalArquivo());
        nova.setNomeArmazenado(anterior.getNomeArmazenado());
        nova.setTipoMime(anterior.getTipoMime());
        nova.setTamanhoBytes(anterior.getTamanhoBytes());
        nova.setSha256Arquivo(anterior.getSha256Arquivo());
        nova.setDataEfetiva(anterior.getDataEfetiva());
        nova.setChaveIdempotencia(limpar(chaveIdempotencia));
        nova.setRegistradoPorId(usuario == null ? null : usuario.getId());
        nova.setRegistradoPorUsername(usuario == null ? ConstantesDominio.USUARIO_SISTEMA : usuario.getUsername());
        if (nova.getEstado() == EstadoDocumentoComprobatorio.PUBLICADO) marcarPublicacao(nova, usuario);
        return nova;
    }

    private void validarCadastro(CadastroDocumentoComprobatorioRequisicao req, MultipartFile arquivo) {
        if (req == null || req.tipoFato() == null || req.classificacaoAcesso() == null || vazio(req.titulo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de fato, classificação e título do documento são obrigatórios.");
        }
        if ((arquivo == null || arquivo.isEmpty()) && vazio(req.referenciaSei())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe anexo, referência SEI ou ambos.");
        }
        if (req.tipoFato() != TipoFatoDocumental.CONTRATO_ORIGINAL && req.fatoId() == null && vazio(req.fatoChave())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Documento preparado para fato futuro exige fatoChave.");
        }
        if (!vazio(req.chaveIdempotencia()) && req.chaveIdempotencia().trim().length() > 96) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chave de idempotência excede 96 caracteres.");
        }
    }

    private ArquivoPreparado prepararArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;
        if (arquivo.getSize() <= 0 || arquivo.getSize() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo excede o limite individual configurado.");
        }
        String nomeOriginal = higienizarNome(arquivo.getOriginalFilename());
        String extensao = extensao(nomeOriginal);
        if (!EXTENSOES.contains(extensao)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Extensão de arquivo não permitida.");
        }
        String mimeCanonico = MIME_POR_EXTENSAO.get(extensao);
        String informado = limpar(arquivo.getContentType());
        boolean aliasJpeg = ("jpg".equals(extensao) || "jpeg".equals(extensao))
                && "image/jpg".equalsIgnoreCase(informado);
        if (informado != null && !"application/octet-stream".equalsIgnoreCase(informado)
                && !mimeCanonico.equalsIgnoreCase(informado)
                && !aliasJpeg) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo MIME não corresponde à extensão do arquivo.");
        }
        try {
            Files.createDirectories(armazenamento);
            Files.createDirectories(temporarios);
            String nomeArmazenado = UUID.randomUUID() + "." + extensao;
            Path temporario = temporarios.resolve(UUID.randomUUID() + ".upload").normalize();
            Path definitivo = resolverArquivo(nomeArmazenado);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(arquivo.getInputStream(), digest)) {
                Files.copy(in, temporario, StandardCopyOption.REPLACE_EXISTING);
            }
            validarAssinatura(temporario, extensao);
            try {
                Files.move(temporario, definitivo, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporario, definitivo, StandardCopyOption.REPLACE_EXISTING);
            }
            registrarExclusaoEmRollback(definitivo);
            return new ArquivoPreparado(definitivo, nomeOriginal, nomeArmazenado, mimeCanonico,
                    Files.size(definitivo), HexFormat.of().formatHex(digest.digest()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar o documento com segurança.", ex);
        }
    }

    private void validarAssinatura(Path arquivo, String extensao) throws IOException {
        byte[] inicio;
        try (InputStream in = Files.newInputStream(arquivo)) { inicio = in.readNBytes(8); }
        boolean valido = switch (extensao) {
            case "pdf" -> comeca(inicio, 0x25, 0x50, 0x44, 0x46, 0x2D);
            case "png" -> comeca(inicio, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg", "jpeg" -> comeca(inicio, 0xFF, 0xD8, 0xFF);
            case "doc", "xls" -> comeca(inicio, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "docx", "xlsx" -> comeca(inicio, 0x50, 0x4B, 0x03, 0x04)
                    || comeca(inicio, 0x50, 0x4B, 0x05, 0x06) || comeca(inicio, 0x50, 0x4B, 0x07, 0x08);
            default -> false;
        };
        if (!valido) {
            Files.deleteIfExists(arquivo);
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "A assinatura binária não corresponde ao formato informado.");
        }
    }

    private boolean comeca(byte[] valor, int... esperado) {
        if (valor.length < esperado.length) return false;
        for (int i = 0; i < esperado.length; i++) if ((valor[i] & 0xFF) != esperado[i]) return false;
        return true;
    }

    private DocumentoComprobatorio salvarComCompensacao(DocumentoComprobatorio documento, ArquivoPreparado preparado) {
        try {
            return documentoRepositorio.save(documento);
        } catch (RuntimeException ex) {
            if (preparado != null) excluirSilenciosamente(preparado.caminho());
            throw ex;
        }
    }

    private void aplicarArquivo(DocumentoComprobatorio documento, ArquivoPreparado preparado) {
        if (preparado == null) return;
        documento.setNomeOriginalArquivo(preparado.nomeOriginal());
        documento.setNomeArmazenado(preparado.nomeArmazenado());
        documento.setTipoMime(preparado.tipoMime());
        documento.setTamanhoBytes(preparado.tamanho());
        documento.setSha256Arquivo(preparado.sha256());
    }

    private void autorizarEAuditar(DocumentoComprobatorio documento, UsuarioAplicacao usuario,
                                    String finalidade, TipoAcessoDocumento tipo) {
        acesso.exigirAcessoContrato(usuario, documento.getContrato());
        autorizarConteudoEAuditar(documento,usuario,finalidade,tipo,false);
    }

    private void autorizarConteudoEAuditar(DocumentoComprobatorio documento,UsuarioAplicacao usuario,
            String finalidade,TipoAcessoDocumento tipo,boolean conferenciaDesignada) {
        acesso.revalidarAposEspera(usuario);
        if (acesso.ehOperacional(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário Operacional não acessa documentos deste módulo.");
        }
        if (documento.getEstado() == EstadoDocumentoComprobatorio.RASCUNHO
                && !gestorResponsavel(usuario, documento.getContrato())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Rascunho documental é visível somente ao Gestor Local responsável.");
        }
        if (documento.getClassificacaoAcesso() == ClassificacaoAcessoDocumento.NEGOCIAL) return;
        if (vazio(finalidade)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Acesso a documento Pessoal/Restrito exige finalidade declarada.");
        }
        PermissaoDocumentoRestrito permissao = null;
        if (conferenciaDesignada || !gestorResponsavel(usuario, documento.getContrato())) {
            permissao = permissaoRepositorio.buscarAtivas(documento.getGrupoLogicoId(), usuario.getId(), Instant.now())
                    .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Documento Pessoal/Restrito exige permissão específica vigente."));
            if (entityManager != null) entityManager.refresh(permissao, jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            if (!Boolean.TRUE.equals(permissao.getAtivo()) || permissao.getRevogadoEm() != null
                    || permissao.getValidoAte() != null && !permissao.getValidoAte().isAfter(Instant.now())
                    || !Objects.equals(permissao.getUsuarioBeneficiario().getId(), usuario.getId())
                    || !Objects.equals(permissao.getGrupoLogicoId(), documento.getGrupoLogicoId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A concessão documental foi revogada, expirou ou deixou de corresponder ao acesso.");
            }
        }
        AcessoDocumentoRestrito registro = new AcessoDocumentoRestrito();
        registro.setDocumento(documento);
        registro.setTipoAcesso(tipo);
        registro.setFinalidade(finalidade);
        registro.setUsuarioId(usuario.getId());
        registro.setUsuarioUsername(usuario.getUsername());
        registro.setPerfisFotografia(usuario.getPerfil() == null
                ? null : usuario.getPerfil().autoridade());
        registro.setPermissaoId(permissao == null ? null : permissao.getId());
        acessoDocumentoRepositorio.save(registro);
    }

    private void exigirEscritaDocumental(UsuarioAplicacao usuario, Contrato contrato) {
        acesso.revalidarAposEspera(usuario);
        acesso.exigirAcessoContrato(usuario, contrato);
        if (!gestorResponsavel(usuario, contrato)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local responsável pode registrar ou versionar documentos negociais.");
        }
    }

    private void exigirAdministrador(UsuarioAplicacao usuario) {
        if (!acesso.ehAdministrador(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Concessão excepcional de acesso restrito exige administração técnica.");
        }
    }

    private void exigirAdministradorOuGestorResponsavel(UsuarioAplicacao usuario, Contrato contrato) {
        if (acesso.ehAdministrador(usuario)) return;
        if (gestorResponsavel(usuario, contrato)) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não consulta a auditoria de acesso restrito.");
    }

    private boolean gestorResponsavel(UsuarioAplicacao usuario, Contrato contrato) {
        GestaoLocal gestao = gestaoContratoTemporal == null ? contrato.getGestaoLocal()
                : gestaoContratoTemporal.gestaoAtual(contrato);
        return acesso.ehGestaoLocal(usuario) && usuario.getGestaoLocal() != null && gestao != null
                && Objects.equals(usuario.getGestaoLocal().getId(), gestao.getId());
    }

    private void registrarEvento(DocumentoComprobatorio documento, TipoEventoDocumento tipo, UsuarioAplicacao usuario,
                                  String motivo, EstadoDocumentoComprobatorio estadoAnterior,
                                  EstadoDocumentoComprobatorio estadoNovo, String seiAnterior, String seiNovo,
                                  String shaAnterior, String shaNovo) {
        EventoDocumentoComprobatorio anterior = eventoRepositorio
                .findTopByGrupoLogicoIdOrderByRegistradoEmDescIdDesc(documento.getGrupoLogicoId()).orElse(null);
        // MySQL persiste TIMESTAMP/DATETIME com precisão de microssegundos.
        // A assinatura deve utilizar exatamente o instante que será recuperado.
        Instant agora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String hashAnterior = anterior == null ? null : anterior.getHashEvento();
        String material = String.join("|", documento.getGrupoLogicoId(), String.valueOf(documento.getId()), tipo.name(),
                String.valueOf(documento.getDataEfetiva()), agora.toString(), valor(usuario == null ? null : usuario.getId()),
                valor(usuario == null ? ConstantesDominio.USUARIO_SISTEMA : usuario.getUsername()), valor(motivo),
                valor(estadoAnterior), valor(estadoNovo), valor(seiAnterior), valor(seiNovo), valor(shaAnterior), valor(shaNovo),
                valor(hashAnterior));
        EventoDocumentoComprobatorio evento = new EventoDocumentoComprobatorio();
        evento.setDocumento(documento);
        evento.setGrupoLogicoId(documento.getGrupoLogicoId());
        evento.setTipoEvento(tipo);
        evento.setDataEfetiva(documento.getDataEfetiva());
        evento.setRegistradoEm(agora);
        evento.setResponsavelId(usuario == null ? null : usuario.getId());
        evento.setResponsavelUsername(usuario == null ? ConstantesDominio.USUARIO_SISTEMA : usuario.getUsername());
        evento.setMotivo(motivo);
        evento.setEstadoAnterior(estadoAnterior);
        evento.setEstadoNovo(estadoNovo);
        evento.setReferenciaSeiAnterior(seiAnterior);
        evento.setReferenciaSeiNova(seiNovo);
        evento.setSha256Anterior(shaAnterior);
        evento.setSha256Novo(shaNovo);
        evento.setHashAnterior(hashAnterior);
        evento.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(material));
        eventoRepositorio.save(evento);
        // A âncora nasce somente nas fábricas de versões novas. Um acervo legado
        // não ganha certificação por adotar os eventos que sobreviveram.
        if (documento.getQuantidadeEventosAuditoria() != null) {
            documento.setQuantidadeEventosAuditoria(documento.getQuantidadeEventosAuditoria() + 1);
            documento.setUltimoHashEventoAuditoria(evento.getHashEvento());
            // Todos os chamadores já persistiram/carregaram esta versão na mesma
            // transação; o dirty checking grava a âncora junto do evento.
        }
    }

    private boolean cadeiaIntegra(List<EventoDocumentoComprobatorio> eventos) {
        if (eventos.isEmpty()) return false;
        String anterior = null;
        for (EventoDocumentoComprobatorio evento : eventos) {
            if (!Objects.equals(anterior, evento.getHashAnterior())) return false;
            if (!Objects.equals(evento.getHashEvento(), calcularHashEvento(evento))) return false;
            anterior = evento.getHashEvento();
        }
        // A listagem preserva acesso ao acervo legado cujos elos conferem.
        // Certificar completude exige também terminaisConferem, usado nas evidências.
        return true;
    }

    /** Completude é confrontada com todas as raízes persistidas, inclusive sem evento remanescente. */
    private boolean terminaisConferem(String grupo, List<EventoDocumentoComprobatorio> eventos) {
        List<DocumentoComprobatorio> versoes = documentoRepositorio.findByGrupoLogicoIdOrderByVersaoDocumentoAsc(grupo);
        if (versoes.isEmpty()) return false;
        Map<Long, Long> quantidades = new HashMap<>();
        Map<Long, String> terminais = new HashMap<>();
        for (EventoDocumentoComprobatorio evento : eventos) {
            if (evento.getDocumento() == null || !Objects.equals(grupo, evento.getGrupoLogicoId())) return false;
            Long id = evento.getDocumento().getId();
            quantidades.merge(id, 1L, Long::sum);
            terminais.put(id, evento.getHashEvento());
        }
        Long anteriorId = null;
        int numero = 1;
        for (DocumentoComprobatorio versao : versoes) {
            boolean ultima = Objects.equals(versao.getId(), versoes.get(versoes.size() - 1).getId());
            // Uma raiz substituída atesta a existência de sucessora. A perda
            // conjunta da última versão e de seus eventos não certifica o prefixo.
            if (ultima ? versao.getEstado() == EstadoDocumentoComprobatorio.SUBSTITUIDO
                    : versao.getEstado() != EstadoDocumentoComprobatorio.SUBSTITUIDO) return false;
            if (!Objects.equals(versao.getVersaoDocumento(), numero++)
                    || !Objects.equals(anteriorId, versao.getDocumentoAnterior() == null
                            ? null : versao.getDocumentoAnterior().getId())
                    || versao.getQuantidadeEventosAuditoria() == null
                    || versao.getQuantidadeEventosAuditoria() < 1
                    || !Objects.equals(versao.getQuantidadeEventosAuditoria(), quantidades.remove(versao.getId()))
                    || versao.getUltimoHashEventoAuditoria() == null
                    || !Objects.equals(versao.getUltimoHashEventoAuditoria(), terminais.get(versao.getId()))) return false;
            anteriorId = versao.getId();
        }
        return quantidades.isEmpty();
    }

    private Contrato obterContratoParaLeitura(Long contratoId) {
        if (entityManager == null) return obterContrato(contratoId);
        // Mesma ordem da concessão, revogação e versionamento: Contrato antes
        // de Documento/Permissão. Mantém autorização e consumo na mesma ordem serial.
        Contrato contrato = entityManager.find(Contrato.class, contratoId,
                jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        if (contrato == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado.");
        return contrato;
    }

    private String calcularHashEvento(EventoDocumentoComprobatorio evento) {
        String material = String.join("|", evento.getGrupoLogicoId(), String.valueOf(evento.getDocumento().getId()),
                evento.getTipoEvento().name(), String.valueOf(evento.getDataEfetiva()), evento.getRegistradoEm().toString(),
                valor(evento.getResponsavelId()), valor(evento.getResponsavelUsername()), valor(evento.getMotivo()),
                valor(evento.getEstadoAnterior()), valor(evento.getEstadoNovo()), valor(evento.getReferenciaSeiAnterior()),
                valor(evento.getReferenciaSeiNova()), valor(evento.getSha256Anterior()), valor(evento.getSha256Novo()),
                valor(evento.getHashAnterior()));
        return UtilitarioHashSeguranca.sha256Hexadecimal(material);
    }

    private DocumentoComprobatorio localizarIdempotente(String chave, Long contratoId, String hashIntencao,
                                                        java.util.function.Predicate<DocumentoComprobatorio> legado) {
        if (vazio(chave)) return null;
        if (chave.trim().length() > 96) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chave de idempotência excede 96 caracteres.");
        }
        return documentoRepositorio.findByChaveIdempotencia(chave.trim()).map(documento -> {
            if (!Objects.equals(documento.getContrato().getId(), contratoId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chave de idempotência já foi usada em outro contexto.");
            }
            boolean coincide = documento.getHashIntencao() == null
                    ? legado.test(documento) : Objects.equals(documento.getHashIntencao(), hashIntencao);
            if (!coincide) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A chave de idempotência já foi usada para outra intenção documental.");
            }
            return documento;
        }).orElse(null);
    }

    private boolean cadastroLegadoCoincide(DocumentoComprobatorio d, CadastroDocumentoComprobatorioRequisicao r,
                                            UsuarioAplicacao usuario, String arquivo) {
        return d.getDocumentoAnterior() == null && Objects.equals(d.getRegistradoPorId(), usuario.getId())
                && d.getTipoFato() == r.tipoFato()
                && (r.fatoId() == null || Objects.equals(d.getFatoId(), r.fatoId()))
                && Objects.equals(DocumentoComprobatorio.normalizarChave(d.getFatoChave()),
                                  DocumentoComprobatorio.normalizarChave(chaveFato(r.tipoFato(), r.fatoChave(), r.fatoId())))
                && iguais(d.getTitulo(), r.titulo()) && iguais(d.getDescricao(), r.descricao())
                && iguais(d.getReferenciaSei(), r.referenciaSei()) && d.getClassificacaoAcesso() == r.classificacaoAcesso()
                && Objects.equals(d.getDataEfetiva(), r.dataEfetiva()) && Objects.equals(assinaturaArquivo(d), arquivo);
    }

    private boolean versaoLegadaCoincide(DocumentoComprobatorio d, DocumentoComprobatorio anterior,
                                          UsuarioAplicacao usuario, String motivo, TipoEventoDocumento... tipos) {
        if (d.getDocumentoAnterior() == null || !Objects.equals(d.getDocumentoAnterior().getId(), anterior.getId())
                || !Objects.equals(d.getRegistradoPorId(), usuario.getId())) return false;
        return eventoRepositorio.findFirstByDocumentoIdOrderByRegistradoEmAscIdAsc(d.getId())
                .filter(e -> Arrays.asList(tipos).contains(e.getTipoEvento()) && iguais(e.getMotivo(), motivo))
                .isPresent();
    }

    private boolean substituicaoLegadaCoincide(DocumentoComprobatorio d, DocumentoComprobatorio anterior,
                                               SubstituicaoDocumentoComprobatorioRequisicao r,
                                               UsuarioAplicacao usuario, String arquivo) {
        return versaoLegadaCoincide(d, anterior, usuario, r.motivo(),
                TipoEventoDocumento.SUBSTITUICAO, TipoEventoDocumento.ANEXO_ACRESCENTADO)
                && iguais(d.getTitulo(), vazio(r.titulo()) ? anterior.getTitulo() : r.titulo())
                && iguais(d.getDescricao(), r.descricao() == null ? anterior.getDescricao() : r.descricao())
                && iguais(d.getReferenciaSei(), vazio(r.referenciaSei()) ? anterior.getReferenciaSei() : r.referenciaSei())
                && Objects.equals(assinaturaArquivo(d), arquivo == null ? assinaturaArquivo(anterior) : arquivo);
    }

    private String assinaturaArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;
        if (arquivo.getSize() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite individual configurado.");
        }
        try (InputStream in = arquivo.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            long tamanho = 0;
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                tamanho += lidos;
                if (tamanho > tamanhoMaximo) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Arquivo excede o limite individual configurado.");
                digest.update(buffer, 0, lidos);
            }
            String nome = higienizarNome(arquivo.getOriginalFilename());
            return hashIntencao(nome, MIME_POR_EXTENSAO.get(extensao(nome)), tamanho,
                    HexFormat.of().formatHex(digest.digest()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", e);
        }
    }

    private String assinaturaArquivo(DocumentoComprobatorio d) {
        return d.possuiArquivo() ? hashIntencao(d.getNomeOriginalArquivo(), d.getTipoMime(),
                d.getTamanhoBytes(), d.getSha256Arquivo()) : null;
    }

    private static String hashIntencao(Object... campos) {
        StringBuilder material = new StringBuilder();
        for (Object campo : campos) {
            String valor = campo == null ? null : normalizarIntencao(String.valueOf(campo));
            material.append(valor == null ? -1 : valor.length()).append(':');
            if (valor != null) material.append(valor);
        }
        return UtilitarioHashSeguranca.sha256Hexadecimal(material.toString());
    }

    private static boolean iguais(String a, String b) {
        return Objects.equals(normalizarIntencao(a), normalizarIntencao(b));
    }

    private static String normalizarIntencao(String valor) {
        return vazio(valor) ? null : valor.trim().replaceAll("\\s+", " ");
    }

    private void exigirVersionavel(DocumentoComprobatorio documento) {
        if (documento.getEstado() != EstadoDocumentoComprobatorio.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente a versão publicada vigente pode ser retificada ou substituída.");
        }
    }

    private void marcarPublicacao(DocumentoComprobatorio documento, UsuarioAplicacao usuario) {
        documento.setPublicadoEm(Instant.now());
        documento.setPublicadoPorId(usuario == null ? null : usuario.getId());
        documento.setPublicadoPorUsername(usuario == null ? ConstantesDominio.USUARIO_SISTEMA : usuario.getUsername());
    }

    private void descartar(DocumentoComprobatorio documento) {
        Path arquivo = documento.possuiArquivo() ? resolverArquivo(documento.getNomeArmazenado()) : null;
        boolean exclusivo = arquivo != null && documentoRepositorio.countByNomeArmazenado(documento.getNomeArmazenado()) <= 1;
        eventoRepositorio.deleteByDocumentoId(documento.getId());
        documentoRepositorio.delete(documento);
        if (exclusivo) registrarExclusaoAposCommit(arquivo);
    }

    private void registrarExclusaoEmRollback(Path arquivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) excluirSilenciosamente(arquivo);
            }
        });
    }

    private void registrarExclusaoAposCommit(Path arquivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            excluirSilenciosamente(arquivo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { excluirSilenciosamente(arquivo); }
        });
    }

    private void excluirSilenciosamente(Path arquivo) {
        try { Files.deleteIfExists(arquivo); } catch (IOException ignored) { }
    }

    private Path resolverArquivo(String nome) {
        if (nome == null || nome.isBlank() || !Path.of(nome).getFileName().toString().equals(nome)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome físico de documento inválido.");
        }
        Path caminho = armazenamento.resolve(nome).normalize();
        if (!caminho.startsWith(armazenamento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Caminho físico de documento inválido.");
        }
        return caminho;
    }

    private String hashArquivo(Path caminho) {
        try (InputStream in = Files.newInputStream(caminho)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            in.transferTo(new OutputStream() { @Override public void write(int b) { digest.update((byte) b); }
                @Override public void write(byte[] b, int off, int len) { digest.update(b, off, len); }});
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível verificar o checksum do documento.", ex);
        }
    }

    private String higienizarNome(String nome) {
        String valor = nome == null ? "arquivo" : nome.replace('\\', '/');
        valor = valor.substring(valor.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_").trim();
        if (valor.isBlank() || valor.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
        return valor;
    }

    private String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private String chaveFato(TipoFatoDocumental tipo, String chave, Long fatoId) {
        if (!vazio(chave)) return chave.trim();
        if (tipo == TipoFatoDocumental.CONTRATO_ORIGINAL) return "ORIGINAL";
        if (fatoId != null) return "ID:" + fatoId;
        return tipo.name();
    }

    private Contrato obterContrato(Long id) {
        return contratoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterContratoParaAtualizacao(Long id) {
        return contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private DocumentoComprobatorio obterNoContrato(Long documentoId, Long contratoId) {
        DocumentoComprobatorio documento = documentoRepositorio.findById(documentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        if (!Objects.equals(documento.getContrato().getId(), contratoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não pertence ao Contrato informado.");
        }
        return documento;
    }

    private DocumentoComprobatorioDTO paraDTO(DocumentoComprobatorio d) {
        return new DocumentoComprobatorioDTO(d.getId(), d.getContrato().getId(), d.getTipoFato(), d.getFatoId(),
                d.getFatoChave(), d.getGrupoLogicoId(), d.getVersaoDocumento(),
                d.getDocumentoAnterior() == null ? null : d.getDocumentoAnterior().getId(), d.getEstado(),
                d.getClassificacaoAcesso(), d.getTipoFonte(), d.getTitulo(), d.getDescricao(), d.getReferenciaSei(),
                d.getNomeOriginalArquivo(), d.getTipoMime(), d.getTamanhoBytes(), d.getSha256Arquivo(), d.getDataEfetiva(),
                d.getRegistradoEm(), d.getRegistradoPorId(), d.getRegistradoPorUsername(), d.getPublicadoEm(),
                d.getPublicadoPorId(), d.getPublicadoPorUsername(),
                d.getEstado() == EstadoDocumentoComprobatorio.PUBLICADO || d.getEstado() == EstadoDocumentoComprobatorio.RASCUNHO,
                d.possuiArquivo(), d.getVersion());
    }

    private EventoDocumentoComprobatorioDTO paraDTO(EventoDocumentoComprobatorio e) {
        return new EventoDocumentoComprobatorioDTO(e.getId(), e.getDocumento().getId(), e.getGrupoLogicoId(), e.getTipoEvento(),
                e.getDataEfetiva(), e.getRegistradoEm(), e.getResponsavelId(), e.getResponsavelUsername(), e.getMotivo(),
                e.getEstadoAnterior(), e.getEstadoNovo(), e.getReferenciaSeiAnterior(), e.getReferenciaSeiNova(),
                e.getSha256Anterior(), e.getSha256Novo(), e.getHashAnterior(), e.getHashEvento());
    }

    private PermissaoDocumentoRestritoDTO paraDTO(PermissaoDocumentoRestrito p) {
        return new PermissaoDocumentoRestritoDTO(p.getId(), p.getDocumentoReferencia().getId(), p.getGrupoLogicoId(),
                p.getUsuarioBeneficiario().getId(), p.getUsuarioBeneficiario().getUsername(), p.getFinalidade(),
                p.getJustificativa(), p.getAutorizacaoAdministrativa(), p.getConcedidoEm(), p.getConcedidoPorId(),
                p.getConcedidoPorUsername(), p.getValidoAte(), p.getAtivo(), p.getRevogadoEm(), p.getRevogadoPorId(),
                p.getRevogadoPorUsername(), p.getMotivoRevogacao(), p.getVersion());
    }

    private AcessoDocumentoRestritoDTO paraDTO(AcessoDocumentoRestrito a) {
        return new AcessoDocumentoRestritoDTO(a.getId(), a.getDocumento().getId(), a.getTipoAcesso(), a.getFinalidade(),
                a.getUsuarioId(), a.getUsuarioUsername(), a.getPerfisFotografia(), a.getPermissaoId(), a.getAcessadoEm());
    }

    private static boolean vazio(String valor) { return valor == null || valor.isBlank(); }
    private static String limpar(String valor) { return vazio(valor) ? null : valor.trim(); }
    private static String valor(Object valor) { return valor == null ? "" : String.valueOf(valor); }
}
