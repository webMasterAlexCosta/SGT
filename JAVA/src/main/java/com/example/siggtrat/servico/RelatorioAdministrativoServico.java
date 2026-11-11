package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Emissões administrativas imutáveis. O conteúdo conhecido no instante da
 * emissão é comprimido e preservado; somente seu estado vigente/superado muda.
 */
@Service
public class RelatorioAdministrativoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RelatorioAdministrativoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private final EmissaoRelatorioAdministrativoRepositorio repositorio;
    private final RelatorioFrequenciaConsolidadoServico relatorioFrequencia;
    private final PendenciaAdministrativaServico pendencias;
    private final AcessoOrganizacionalServico acesso;
    private final RegistroAuditoriaServico auditoria;
    private final ObjectMapper objectMapper;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final InvalidacaoCacheServico invalidacaoCache;

    public RelatorioAdministrativoServico(
            EmissaoRelatorioAdministrativoRepositorio repositorio,
            RelatorioFrequenciaConsolidadoServico relatorioFrequencia,
            PendenciaAdministrativaServico pendencias,
            AcessoOrganizacionalServico acesso,
            RegistroAuditoriaServico auditoria,
            ObjectMapper objectMapper,
            MinimizadorDadosPessoaisServico minimizador,
            InvalidacaoCacheServico invalidacaoCache
    ) {
        this.repositorio = repositorio;
        this.relatorioFrequencia = relatorioFrequencia;
        this.pendencias = pendencias;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
        this.minimizador = minimizador;
        this.invalidacaoCache = invalidacaoCache;
    }

    @Transactional
    public RelatorioAdministrativoDTO previsualizar(EmissaoRelatorioRequisicao requisicao) {
        UsuarioAplicacao usuario = usuarioLeitor();
        FiltroNormalizado filtro = normalizar(requisicao, false);
        ConteudoGerado conteudo = gerarConteudo(filtro, usuario);
        Instant agora = Instant.now();
        MetadadosEmissaoRelatorioDTO meta = new MetadadosEmissaoRelatorioDTO(
                null, filtro.tipo(), filtro.formato(), 0, NaturezaEmissaoRelatorio.ORIGINAL,
                EstadoEmissaoRelatorio.VIGENTE, "PREVIA", filtro.inicio(), filtro.fim(), agora,
                usuario.getId(), usuario.getUsername(), usuario.getPerfil().name(),
                conteudo.aviso().sujeitoARevisao(), conteudo.aviso().quantidadePendencias(),
                conteudo.aviso().mensagem(), conteudo.hash(), conteudo.bytes().length,
                null, null, null, null, agora);
        return new RelatorioAdministrativoDTO(meta, conteudo.json());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @org.springframework.retry.annotation.Retryable(
            retryFor = {DataIntegrityViolationException.class,
                    org.springframework.dao.PessimisticLockingFailureException.class,
                    org.springframework.orm.ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 25, multiplier = 2, maxDelay = 100))
    public RelatorioAdministrativoDTO emitir(EmissaoRelatorioRequisicao requisicao) {
        UsuarioAplicacao usuario = usuarioLeitor();
        FiltroNormalizado filtro = normalizar(requisicao, true);
        EscopoSerie escopo = escopo(usuario);
        String filtrosJson = json(filtro.paraPersistencia());
        String chaveSerie = sha256(filtro.tipo() + "|" + filtro.formato() + "|" + filtrosJson + "|" + escopo.chave());
        String hashIntencao = sha256(chaveSerie + "|" + filtro.chaveIdempotencia());

        Optional<EmissaoRelatorioAdministrativo> repetida = repositorio
                .findByUsuarioIdAndChaveIdempotencia(usuario.getId(), filtro.chaveIdempotencia());
        if (repetida.isPresent()) {
            // O reenvio também devolve dados pessoais. Neste ramo não haverá
            // aquisição posterior de raiz negocial, mantendo a ordem dos locks.
            acesso.revalidarAposEspera(usuario);
            usuarioLeitor();
            EmissaoRelatorioAdministrativo existente = repetida.get();
            if (!Objects.equals(existente.getHashIntencao(), hashIntencao)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A chave de idempotência já foi usada para outra emissão.");
            }
            exigirAcesso(usuario, existente);
            return completo(existente, usuario);
        }

        // Mesma ordem das alterações de fonte: Contratos antes das emissões.
        // READ_COMMITTED impede que as consultas anteriores ao lock fixem uma
        // visão antiga quando foi necessário esperar por outra transação.
        List<Long> contratosBloqueados = relatorioFrequencia.bloquearFontesParaEmissao(filtro.contratoIds());
        acesso.revalidarAposEspera(usuario);
        usuarioLeitor();
        Instant fonteConhecidaAte = Instant.now();
        ConteudoGerado conteudo = gerarConteudo(filtro, usuario, contratosBloqueados);
        EmissaoRelatorioAdministrativo anterior = repositorio
                .findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(chaveSerie).orElse(null);
        boolean fonteRetificada = anterior != null
                && anterior.getEstado() == EstadoEmissaoRelatorio.SUPERADA
                && anterior.getCausaSuperacao() == CausaSuperacaoRelatorio.RETIFICACAO_FONTE;
        NaturezaEmissaoRelatorio natureza = anterior == null
                ? NaturezaEmissaoRelatorio.ORIGINAL
                : fonteRetificada ? NaturezaEmissaoRelatorio.RETIFICADA : anterior.getNatureza();

        if (anterior != null && anterior.getEstado() == EstadoEmissaoRelatorio.VIGENTE) {
            superar(anterior, CausaSuperacaoRelatorio.NOVA_EMISSAO,
                    "Nova emissão administrativa da mesma série.");
        }

        Instant agora = Instant.now();
        EmissaoRelatorioAdministrativo emissao = new EmissaoRelatorioAdministrativo();
        emissao.setChaveSerie(chaveSerie);
        emissao.setNumeroVersao(anterior == null ? 1 : anterior.getNumeroVersao() + 1);
        emissao.setTipo(filtro.tipo());
        emissao.setFormato(filtro.formato());
        emissao.setNatureza(natureza);
        emissao.setEstado(EstadoEmissaoRelatorio.VIGENTE);
        emissao.setInicio(filtro.inicio());
        emissao.setFim(filtro.fim());
        emissao.setGeradoEm(agora);
        emissao.setFonteConhecidaAte(fonteConhecidaAte);
        emissao.setUsuarioId(usuario.getId());
        emissao.setUsuarioUsername(usuario.getUsername());
        emissao.setPerfilUsuario(usuario.getPerfil().name());
        emissao.setSujeitoRevisao(conteudo.aviso().sujeitoARevisao());
        emissao.setQuantidadePendencias(conteudo.aviso().quantidadePendencias());
        emissao.setAvisoRevisao(conteudo.aviso().mensagem());
        emissao.setConteudoSnapshotGzip(gzip(conteudo.bytes()));
        emissao.setHashConteudo(conteudo.hash());
        emissao.setTamanhoConteudoBytes(conteudo.bytes().length);
        emissao.setFiltrosJson(filtrosJson);
        emissao.setContratoIdsCsv(csv(filtro.contratoIds()));
        emissao.setLotacaoId(filtro.lotacaoId());
        emissao.setJornadaId(filtro.jornadaId());
        emissao.setGestaoLocalEscopoId(escopo.gestaoLocalId());
        emissao.setSuperintendenciaEscopoId(escopo.superintendenciaId());
        emissao.setEmissaoAnteriorId(anterior == null ? null : anterior.getId());
        emissao.setChaveIdempotencia(filtro.chaveIdempotencia());
        emissao.setHashIntencao(hashIntencao);

        // Violação concorrente deve sair desta transação. O interceptor de retry
        // executa o método novamente após rollback, com uma nova unidade de trabalho.
        emissao = repositorio.saveAndFlush(emissao);
        auditoria.registrarAcaoComoUsuario("RELATORIO_EMITIDO", "EmissaoRelatorioAdministrativo",
                emissao.getId(), emissao.getTipo() + " v" + emissao.getNumeroVersao(),
                RegistroAuditoriaServico.detalhes("natureza", emissao.getNatureza(), "estado", emissao.getEstado(),
                        "inicio", emissao.getInicio(), "fim", emissao.getFim(),
                        "sujeitoRevisao", emissao.isSujeitoRevisao(),
                        "quantidadePendencias", emissao.getQuantidadePendencias(),
                        "hashConteudo", emissao.getHashConteudo()), usuario);
        // Cada tentativa de emissão possui sua própria transação. A revisão
        // participa dela; o controller não envolve o retry numa transação já
        // comprometida por uma violação de unicidade concorrente.
        invalidacaoCache.invalidarCachesDeNegocioAposCommit();
        return completo(emissao, usuario);
    }

    @Transactional(readOnly = true)
    public RelatorioAdministrativoDTO obter(Long id) {
        UsuarioAplicacao usuario = usuarioLeitor();
        EmissaoRelatorioAdministrativo emissao = repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Emissão de relatório não encontrada."));
        exigirAcesso(usuario, emissao);
        return completo(emissao, usuario);
    }

    @Transactional(readOnly = true)
    public RelatorioAdministrativoDTO conhecidaAte(EmissaoRelatorioRequisicao requisicao, Instant conhecidoAte) {
        if (conhecidoAte == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Instante de conhecimento é obrigatório.");
        UsuarioAplicacao usuario = usuarioLeitor();
        FiltroNormalizado filtro = normalizar(requisicao, false);
        String chave = sha256(filtro.tipo() + "|" + filtro.formato() + "|"
                + json(filtro.paraPersistencia()) + "|" + escopo(usuario).chave());
        EmissaoRelatorioAdministrativo emissao = repositorio
                .findTopByChaveSerieAndGeradoEmLessThanEqualOrderByGeradoEmDescIdDesc(chave, conhecidoAte)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Não existe emissão conhecida até o instante informado para este recorte."));
        return new RelatorioAdministrativoDTO(paraMetadados(emissao, usuario, conhecidoAte),
                minimizarConteudo(conteudo(emissao), emissao.getTipo(), usuario));
    }

    @Transactional
    public ComparacaoEmissaoRelatorioDTO compararComVigente(Long id) {
        UsuarioAplicacao usuario = usuarioLeitor();
        EmissaoRelatorioAdministrativo emissao = repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Emissão de relatório não encontrada."));
        exigirAcesso(usuario, emissao);
        FiltroNormalizado filtro = filtroDaEmissao(emissao);
        ConteudoGerado vigente = gerarConteudo(filtro, usuario, null,
                emissao.getGestaoLocalEscopoId(), emissao.getSuperintendenciaEscopoId());
        JsonNode historico = minimizarConteudo(conteudo(emissao), emissao.getTipo(), usuario);
        return new ComparacaoEmissaoRelatorioDTO(paraMetadados(emissao, usuario), vigente.aviso(),
                emissao.getHashConteudo(), vigente.hash(),
                !conteudosEquivalentes(historico, vigente.json()), historico, vigente.json());
    }

    private boolean conteudosEquivalentes(JsonNode historico, JsonNode vigente) {
        // readTree do snapshot usa IntNode para IDs pequenos; valueToTree de um
        // DTO preserva LongNode. Essa diferença interna não altera o número JSON.
        // A comparação percorre as árvores existentes, sem duplicar o relatório
        // nem alterar seus bytes/hash, e continua detectando valores diferentes.
        return historico.equals((a, b) -> {
            if (a.isNumber() && b.isNumber()) return a.decimalValue().compareTo(b.decimalValue());
            return a.equals(b) ? 0 : 1;
        }, vigente);
    }

    @Transactional(readOnly = true)
    public Page<MetadadosEmissaoRelatorioDTO> listar(TipoRelatorioAdministrativo tipo,
                                                      EstadoEmissaoRelatorio estado,
                                                      LocalDate inicio, LocalDate fim,
                                                      Integer pagina, Integer tamanho) {
        UsuarioAplicacao usuario = usuarioLeitor();
        Pageable pageable = PageRequest.of(Math.max(0, pagina == null ? 0 : pagina),
                Math.min(TAMANHO_MAXIMO_PAGINA, Math.max(1, tamanho == null ? 50 : tamanho)),
                Sort.by(Sort.Direction.DESC, "geradoEm").and(Sort.by(Sort.Direction.DESC, "id")));
        Specification<EmissaoRelatorioAdministrativo> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> itens = new ArrayList<>();
            if (acesso.ehGestorEstadual(usuario)) {
                SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(usuario);
                itens.add(cb.equal(root.get("superintendenciaEscopoId"), se == null ? -1L : se.getId()));
            } else if (acesso.ehGestaoLocal(usuario)) {
                itens.add(cb.equal(root.get("gestaoLocalEscopoId"), usuario.getGestaoLocal().getId()));
            }
            if (tipo != null) itens.add(cb.equal(root.get("tipo"), tipo));
            if (estado != null) itens.add(cb.equal(root.get("estado"), estado));
            if (inicio != null) itens.add(cb.greaterThanOrEqualTo(root.get("fim"), inicio));
            if (fim != null) itens.add(cb.lessThanOrEqualTo(root.get("inicio"), fim));
            return cb.and(itens.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repositorio.findAll(spec, pageable).map(emissao -> paraMetadados(emissao, usuario));
    }

    private ConteudoGerado gerarConteudo(FiltroNormalizado filtro, UsuarioAplicacao usuario) {
        return gerarConteudo(filtro, usuario, null);
    }

    private ConteudoGerado gerarConteudo(FiltroNormalizado filtro, UsuarioAplicacao usuario,
                                         List<Long> contratosBloqueados) {
        return gerarConteudo(filtro, usuario, contratosBloqueados, null, null);
    }

    private ConteudoGerado gerarConteudo(FiltroNormalizado filtro, UsuarioAplicacao usuario,
            List<Long> contratosBloqueados, Long gestaoLocalId, Long superintendenciaId) {
        boolean recortePublicado = gestaoLocalId != null || superintendenciaId != null;
        AvisoRevisaoRelatorioDTO aviso = recortePublicado
                ? pendencias.avisoRelatorioNoEscopo(filtro.inicio(), filtro.fim(), filtro.contratoIds(),
                        filtro.lotacaoId(), filtro.jornadaId(), gestaoLocalId, superintendenciaId)
                : pendencias.avisoRelatorio(filtro.inicio(), filtro.fim(), filtro.contratoIds(),
                        filtro.lotacaoId(), filtro.jornadaId());
        List<Long> contratos = contratosBloqueados == null ? filtro.contratoIds() : contratosBloqueados;
        Object valor;
        if (contratosBloqueados != null && contratosBloqueados.isEmpty()) {
            // Uma lista vazia na API normal significa todos os contratos. Aqui
            // significa que a descoberta travada não encontrou fonte alguma.
            valor = filtro.tipo() == TipoRelatorioAdministrativo.ABSENTEISMO ? List.of()
                    : new RelatorioFrequenciaConsolidadoDTO(true, CalculoFrequenciaServico.CALCULO_VERSAO,
                    filtro.inicio(), filtro.fim(), List.of(), List.of(),
                    new RelatorioFrequenciaConsolidadoDTO.Totais(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), aviso);
        } else {
            valor = switch (filtro.tipo()) {
                case FREQUENCIA_CONSOLIDADA -> recortePublicado
                        ? relatorioFrequencia.gerarNoEscopo(filtro.inicio(), filtro.fim(), contratos,
                                filtro.lotacaoId(), filtro.jornadaId(), gestaoLocalId, superintendenciaId)
                        : relatorioFrequencia.gerar(filtro.inicio(), filtro.fim(), contratos,
                                filtro.lotacaoId(), filtro.jornadaId());
                case ABSENTEISMO -> recortePublicado
                        ? relatorioFrequencia.gerarAbsenteismoNoEscopo(filtro.inicio(), filtro.fim(), contratos,
                                filtro.lotacaoId(), filtro.jornadaId(), gestaoLocalId, superintendenciaId)
                        : relatorioFrequencia.gerarAbsenteismo(filtro.inicio(), filtro.fim(), contratos,
                                filtro.lotacaoId(), filtro.jornadaId());
            };
        }
        JsonNode arvore = minimizarConteudo(objectMapper.valueToTree(valor), filtro.tipo(), usuario);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(arvore);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao serializar o relatório administrativo.", e);
        }
        return new ConteudoGerado(arvore, bytes, sha256(bytes), aviso);
    }

    private RelatorioAdministrativoDTO completo(EmissaoRelatorioAdministrativo emissao, UsuarioAplicacao usuario) {
        return new RelatorioAdministrativoDTO(paraMetadados(emissao, usuario),
                minimizarConteudo(conteudo(emissao), emissao.getTipo(), usuario));
    }

    /**
     * A árvore é criada nesta leitura; somente a representação autorizada muda.
     * O blob comprimido, o hash e o tamanho da emissão continuam intocados.
     */
    private JsonNode minimizarConteudo(JsonNode arvore, TipoRelatorioAdministrativo tipo, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        if (minimizador.completo(nivel)) return arvore;
        JsonNode registros = tipo == TipoRelatorioAdministrativo.ABSENTEISMO ? arvore : arvore.path("registros");
        if (!registros.isArray()) return arvore;
        for (JsonNode registro : registros) {
            if (!(registro instanceof ObjectNode linha)) continue;
            if (linha.has("nome")) linha.put("nome", minimizador.nome(linha.path("nome").asText(null), nivel));
            if (linha.has("cpf")) linha.put("cpf", minimizador.cpf(linha.path("cpf").asText(null), nivel));
            if (linha.has("matricula")) linha.put("matricula", minimizador.matricula(linha.path("matricula").asText(null), nivel));
        }
        if (nivel == NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS) {
            // Ordenar por identidade técnica evita depender do nome suprimido e
            // torna comparáveis a prova local e a apuração institucional atual.
            List<JsonNode> ordenados = new ArrayList<>();
            registros.forEach(ordenados::add);
            ordenados.sort(Comparator.comparingLong((JsonNode r) -> r.path("empregadoId").asLong())
                    .thenComparing(r -> r.path("chaveAlocacao").asText(""))
                    .thenComparing(r -> r.path("lotacao").asText(""))
                    .thenComparing(r -> r.path("cargo").asText(""))
                    .thenComparing(r -> r.path("turno").asText("")));
            ((ArrayNode) registros).removeAll().addAll(ordenados);
        }
        return arvore;
    }

    private JsonNode conteudo(EmissaoRelatorioAdministrativo emissao) {
        try {
            return objectMapper.readTree(gunzip(emissao.getConteudoSnapshotGzip()));
        } catch (IOException e) {
            throw new IllegalStateException("Snapshot da emissão está corrompido.", e);
        }
    }

    private MetadadosEmissaoRelatorioDTO paraMetadados(EmissaoRelatorioAdministrativo e, UsuarioAplicacao usuario) {
        return paraMetadados(e, usuario, null);
    }

    private MetadadosEmissaoRelatorioDTO paraMetadados(EmissaoRelatorioAdministrativo e, UsuarioAplicacao usuario,
                                                      Instant conhecidoAte) {
        // A emissão preservada pode ter sido superada depois do corte pedido.
        // Somente sua representação histórica muda; a entidade atual e o blob
        // continuam intocados, inclusive nas consultas dentro de uma transação.
        boolean superacaoPosterior = conhecidoAte != null && e.getSuperadaEm() != null
                && e.getSuperadaEm().isAfter(conhecidoAte);
        EstadoEmissaoRelatorio estado = superacaoPosterior ? EstadoEmissaoRelatorio.VIGENTE : e.getEstado();
        String situacao = estado == EstadoEmissaoRelatorio.SUPERADA
                ? "SUPERADA" : e.getNatureza().name();
        return new MetadadosEmissaoRelatorioDTO(e.getId(), e.getTipo(), e.getFormato(), e.getNumeroVersao(),
                e.getNatureza(), estado, situacao, e.getInicio(), e.getFim(), e.getGeradoEm(),
                e.getUsuarioId(), e.getUsuarioUsername(), e.getPerfilUsuario(), e.isSujeitoRevisao(),
                e.getQuantidadePendencias(), e.getAvisoRevisao(), e.getHashConteudo(),
                e.getTamanhoConteudoBytes(), e.getEmissaoAnteriorId(), superacaoPosterior ? null : e.getSuperadaEm(),
                superacaoPosterior ? null : e.getCausaSuperacao(),
                // Justificativa livre pode conter dados pessoais/restritos. A
                // política do leitor vale também para metadados, preservando a
                // causa tipada, a prova persistida, o blob e o hash originais.
                superacaoPosterior ? null : minimizador.restrito(e.getMotivoSuperacao(),
                        minimizador.nivelPadrao(usuario)), e.getFonteConhecidaAte());
    }

    private UsuarioAplicacao usuarioLeitor() {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        if (!acesso.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu perfil não possui acesso aos relatórios administrativos.");
        }
        return usuario;
    }

    private void exigirAcesso(UsuarioAplicacao usuario, EmissaoRelatorioAdministrativo emissao) {
        if (acesso.ehAdministrador(usuario) || acesso.ehGestorNacional(usuario)
                || acesso.ehPresidente(usuario)) return;
        if (acesso.ehGestorEstadual(usuario)) {
            SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(usuario);
            if (se != null && Objects.equals(se.getId(), emissao.getSuperintendenciaEscopoId())) return;
        } else if (acesso.ehGestaoLocal(usuario) && usuario.getGestaoLocal() != null
                && Objects.equals(usuario.getGestaoLocal().getId(), emissao.getGestaoLocalEscopoId())) return;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emissão de relatório não encontrada no seu escopo.");
    }

    private FiltroNormalizado normalizar(EmissaoRelatorioRequisicao r, boolean exigirIdempotencia) {
        if (r == null || r.tipo() == null || r.formato() == null || r.inicio() == null || r.fim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo, formato, início e fim são obrigatórios.");
        }
        if (r.inicio().isAfter(r.fim()) || ChronoUnit.DAYS.between(r.inicio(), r.fim()) > 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Período inválido; a emissão aceita no máximo 367 dias inclusivos.");
        }
        String chave = normalizarTexto(r.chaveIdempotencia());
        if (exigirIdempotencia && chave == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chave de idempotência é obrigatória para uma emissão formal.");
        }
        List<Long> contratos = r.contratoIds() == null ? List.of()
                : r.contratoIds().stream().filter(Objects::nonNull).distinct().sorted().toList();
        return new FiltroNormalizado(r.tipo(), r.formato(), r.inicio(), r.fim(), contratos,
                r.lotacaoId(), r.jornadaId(), chave);
    }

    private FiltroNormalizado filtroDaEmissao(EmissaoRelatorioAdministrativo e) {
        return new FiltroNormalizado(e.getTipo(), e.getFormato(), e.getInicio(), e.getFim(),
                idsCsv(e.getContratoIdsCsv()), e.getLotacaoId(), e.getJornadaId(), null);
    }

    private EscopoSerie escopo(UsuarioAplicacao usuario) {
        if (acesso.ehGestaoLocal(usuario)) {
            Long id = usuario.getGestaoLocal() == null ? null : usuario.getGestaoLocal().getId();
            Long se = usuario.getGestaoLocal() == null || usuario.getGestaoLocal().getSuperintendencia() == null
                    ? null : usuario.getGestaoLocal().getSuperintendencia().getId();
            return new EscopoSerie("GL:" + id, id, se);
        }
        if (acesso.ehGestorEstadual(usuario)) {
            SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(usuario);
            Long id = se == null ? null : se.getId();
            return new EscopoSerie("SE:" + id, null, id);
        }
        return new EscopoSerie("NACIONAL", null, null);
    }

    private void superar(EmissaoRelatorioAdministrativo e, CausaSuperacaoRelatorio causa, String motivo) {
        if (e.getEstado() == EstadoEmissaoRelatorio.SUPERADA) return;
        e.setEstado(EstadoEmissaoRelatorio.SUPERADA);
        e.setSuperadaEm(Instant.now());
        e.setCausaSuperacao(causa);
        e.setMotivoSuperacao(motivo);
        repositorio.save(e);
    }

    private String json(Object valor) {
        try { return objectMapper.writeValueAsString(valor); }
        catch (IOException e) { throw new IllegalStateException("Falha ao serializar filtros do relatório.", e); }
    }

    private static byte[] gzip(byte[] dados) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(saida)) {
            gzip.write(dados);
            gzip.finish();
            return saida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao comprimir snapshot do relatório.", e);
        }
    }

    private static byte[] gunzip(byte[] dados) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(dados));
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            gzip.transferTo(saida);
            return saida.toByteArray();
        }
    }

    private static String sha256(String valor) { return sha256(valor.getBytes(StandardCharsets.UTF_8)); }
    private static String sha256(byte[] valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor));
        } catch (Exception e) { throw new IllegalStateException("SHA-256 indisponível.", e); }
    }
    private static String normalizarTexto(String valor) {
        if (valor == null) return null;
        String n = valor.trim();
        return n.isBlank() ? null : n;
    }
    private static String csv(List<Long> ids) {
        return ids == null || ids.isEmpty() ? null : String.join(",", ids.stream().map(String::valueOf).toList());
    }
    private static List<Long> idsCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
    }

    private record ConteudoGerado(JsonNode json, byte[] bytes, String hash, AvisoRevisaoRelatorioDTO aviso) { }
    private record EscopoSerie(String chave, Long gestaoLocalId, Long superintendenciaId) { }
    private record FiltroPersistido(TipoRelatorioAdministrativo tipo, FormatoEmissaoRelatorio formato,
                                    LocalDate inicio, LocalDate fim, List<Long> contratoIds,
                                    Long lotacaoId, Long jornadaId) { }
    private record FiltroNormalizado(TipoRelatorioAdministrativo tipo, FormatoEmissaoRelatorio formato,
                                     LocalDate inicio, LocalDate fim, List<Long> contratoIds,
                                     Long lotacaoId, Long jornadaId, String chaveIdempotencia) {
        private FiltroPersistido paraPersistencia() {
            return new FiltroPersistido(tipo, formato, inicio, fim, contratoIds, lotacaoId, jornadaId);
        }
    }
}
