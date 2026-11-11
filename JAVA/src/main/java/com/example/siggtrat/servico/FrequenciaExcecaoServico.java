package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO;
import com.example.siggtrat.dto.FrequenciaExcecaoDTO;
import com.example.siggtrat.dto.VersaoComplementoFrequenciaDTO;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.FrequenciaExcecaoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FrequenciaExcecaoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(FrequenciaExcecaoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final FrequenciaExcecaoRepositorio repositorio;
    private final UsuarioAplicacaoRepositorio usuarios;
    private final AcessoOrganizacionalServico acesso;
    private final ComplementoFrequenciaTemporalServico temporal;
    private final EntityManager em;
    private final RevalidacaoFrequenciaTemporalServico revalidacao;

    public FrequenciaExcecaoServico(FrequenciaExcecaoRepositorio repositorio,
            UsuarioAplicacaoRepositorio usuarios, AcessoOrganizacionalServico acesso,
            ComplementoFrequenciaTemporalServico temporal, EntityManager em,
            RevalidacaoFrequenciaTemporalServico revalidacao) {
        this.repositorio = repositorio; this.usuarios = usuarios; this.acesso = acesso;
        this.temporal = temporal; this.em = em; this.revalidacao = revalidacao;
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaExcecaoDTO> listarAtivasPorData(LocalDate data, Long lotacaoId,
            Long jornadaId, Long contratoId, Pageable pageable) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        UsuarioAplicacao user = usuarioAtual();
        if (acesso.ehGlobal(user) && lotacaoId == null && jornadaId == null && contratoId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos lotação, turno ou contrato para consultar exceções.");
        Specification<FrequenciaExcecao> spec = (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get("dataInicio"), data),
                cb.greaterThanOrEqualTo(root.get("dataFim"), data),
                cb.notEqual(cb.upper(root.get("status")), ConstantesDominio.STATUS_CANCELADO));
        Pageable limitado = UtilitarioPaginacao.limitar(pageable,
                Sort.by("dataInicio").ascending().and(Sort.by("empregado.nome")));
        Sort ordem = limitado.getSort().getOrderFor("id") == null
                ? limitado.getSort().and(Sort.by("id")) : limitado.getSort();
        List<FrequenciaExcecaoDTO> selecionados = new ArrayList<>();
        long total = 0; int lote = 0;
        Page<FrequenciaExcecao> candidatos;
        // Autorização antes da paginação pública: os totais também pertencem ao escopo histórico.
        do {
            candidatos = repositorio.findAll(spec, PageRequest.of(lote++, 200, ordem));
            for (FrequenciaExcecao e : candidatos) {
                var contextos = contexto(e);
                if (!temporal.podeConsultar(user, contextos)
                        || !temporal.atendeFiltros(contextos, data, lotacaoId, jornadaId, contratoId)) continue;
                if (total >= limitado.getOffset() && selecionados.size() < limitado.getPageSize())
                    selecionados.add(paraConsulta(e, user));
                total++;
            }
            // Não limpa o contexto de transações chamadoras nem fontes alheias à página.
            for (FrequenciaExcecao e : candidatos) if (em.contains(e)) em.detach(e);
        } while (candidatos.hasNext());
        return new PageImpl<>(selecionados, limitado, total);
    }

    public FrequenciaExcecaoDTO criar(FrequenciaExcecaoDTO dto) {
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        validarDto(dto);
        JustificativaFonteFrequencia.validar(dto.justificativa(), false);
        Empregado empregado = temporal.bloquearEmpregados(dto.empregadoId()).get(dto.empregadoId());
        acesso.revalidarAposEspera(user);
        var contextos = temporal.validarCriacao(user, empregado, dto.dataInicio(), dto.dataFim());
        String tipo = normalizarTipo(dto.tipo());
        validarSobreposicao(empregado.getId(), tipo, dto.dataInicio(), dto.dataFim(), null);
        FrequenciaExcecao obj = new FrequenciaExcecao();
        obj.setEmpregado(empregado);
        obj.setEmpregadoNomeFotografia(temporal.capturarNomeComprovado(empregado, contextos));
        obj.setDataInicio(dto.dataInicio()); obj.setDataFim(dto.dataFim()); obj.setTipo(tipo);
        obj.setStatus(dto.status() == null || dto.status().isBlank()
                ? ConstantesDominio.STATUS_APROVADO : normalizarTipo(dto.status()));
        obj.setObservacao(dto.observacao());
        obj = repositorio.saveAndFlush(obj);
        FrequenciaExcecaoDTO foto = paraDTO(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.EXCECAO, obj.getId(), "CRIACAO",
                obj.getDataInicio(), foto, contextos, user, dto.justificativa());
        revalidacao.revalidarPorExcecao(null, foto, "Inclusão da fonte de exceção " + obj.getId(), user);
        return foto;
    }

    public FrequenciaExcecaoDTO atualizar(Long id, FrequenciaExcecaoDTO dto) {
        FrequenciaExcecao obj = obter(id);
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados da exceção.");
        Long origemId = obj.getEmpregado().getId();
        Long destinoId = dto.empregadoId() == null ? origemId : dto.empregadoId();
        var empregados = temporal.bloquearEmpregados(origemId, destinoId);
        bloquearFonte(obj, origemId);
        acesso.revalidarAposEspera(user);
        var contextosAnteriores = contexto(obj);
        temporal.exigirEscrita(user, contextosAnteriores);
        LocalDate inicio = dto.dataInicio() == null ? obj.getDataInicio() : dto.dataInicio();
        LocalDate fim = dto.dataFim() == null ? obj.getDataFim() : dto.dataFim();
        validarPeriodo(inicio, fim);
        Empregado destino = empregados.get(destinoId);
        boolean mesmoRecorte = origemId.equals(destinoId) && inicio.equals(obj.getDataInicio()) && fim.equals(obj.getDataFim());
        var contextos = mesmoRecorte ? contextosAnteriores : temporal.validarCriacao(user, destino, inicio, fim);
        String tipo = dto.tipo() == null || dto.tipo().isBlank() ? obj.getTipo() : normalizarTipo(dto.tipo());
        String status = dto.status() == null ? obj.getStatus() : normalizarTipo(dto.status());
        if (ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(obj.getStatus())
                && !ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(status))
            contextos = temporal.validarCriacao(user, destino, inicio, fim);
        validarSobreposicao(destinoId, tipo, inicio, fim, id);
        FrequenciaExcecaoDTO antes = paraDTO(obj);
        String nome = mesmoRecorte ? obj.getEmpregadoNomeFotografia()
                : temporal.capturarNomeComprovado(destino, contextos);
        FrequenciaExcecaoDTO depois = new FrequenciaExcecaoDTO(id, destinoId, nome, inicio, fim, tipo, status,
                dto.observacao() == null ? obj.getObservacao() : dto.observacao());
        if (temporal.mesmoConteudo(antes, depois)) return depois;
        String motivo = JustificativaFonteFrequencia.validar(dto.justificativa(), true);
        temporal.preservarLegado(ComplementoFrequenciaTemporalServico.EXCECAO, id, obj.getDataInicio(), antes, contextosAnteriores);
        obj.setEmpregado(destino);
        obj.setEmpregadoNomeFotografia(nome);
        obj.setDataInicio(inicio); obj.setDataFim(fim); obj.setTipo(tipo); obj.setStatus(status);
        if (dto.observacao() != null) obj.setObservacao(dto.observacao());
        repositorio.saveAndFlush(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.EXCECAO, id, "RETIFICACAO", inicio,
                depois, contextos, user, motivo);
        revalidacao.revalidarPorExcecao(antes, depois, "Retificação da fonte de exceção " + id + ": " + motivo, user);
        return depois;
    }

    public void excluir(Long id) { excluir(id, null); }

    public void excluir(Long id, String justificativa) {
        FrequenciaExcecao obj = obter(id);
        UsuarioAplicacao user = usuarioAtual(); temporal.exigirGestor(user);
        Long empregadoId = obj.getEmpregado().getId();
        temporal.bloquearEmpregados(empregadoId); bloquearFonte(obj, empregadoId);
        acesso.revalidarAposEspera(user);
        var contextos = contexto(obj); temporal.exigirEscrita(user, contextos);
        if (ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(obj.getStatus())) return;
        justificativa = JustificativaFonteFrequencia.validar(justificativa, true);
        FrequenciaExcecaoDTO antes = paraDTO(obj);
        temporal.preservarLegado(ComplementoFrequenciaTemporalServico.EXCECAO, id, obj.getDataInicio(), antes, contextos);
        obj.setStatus(ConstantesDominio.STATUS_CANCELADO);
        repositorio.saveAndFlush(obj);
        temporal.registrar(ComplementoFrequenciaTemporalServico.EXCECAO, id, "CANCELAMENTO", obj.getDataInicio(),
                paraDTO(obj), contextos, user, justificativa);
        revalidacao.revalidarPorExcecao(antes, paraDTO(obj), "Cancelamento da fonte de exceção " + id + ": " + justificativa, user);
    }

    @Transactional(readOnly = true)
    public List<VersaoComplementoFrequenciaDTO> listarEventos(Long id, Instant conhecidoAte) {
        FrequenciaExcecao obj = obter(id);
        return temporal.listarEventos(ComplementoFrequenciaTemporalServico.EXCECAO, id, conhecidoAte, usuarioAtual(), contexto(obj));
    }

    private List<ContextoComplementoFrequenciaDTO> contexto(FrequenciaExcecao obj) {
        return temporal.resolver(ComplementoFrequenciaTemporalServico.EXCECAO, obj.getId(), obj.getEmpregado(),
                obj.getDataInicio(), obj.getDataFim());
    }

    private void bloquearFonte(FrequenciaExcecao obj, Long empregadoEsperado) {
        em.refresh(obj, LockModeType.PESSIMISTIC_WRITE);
        if (!Objects.equals(empregadoEsperado, obj.getEmpregado().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A fonte foi alterada por outro comando; recarregue antes de retificar.");
    }

    private FrequenciaExcecao obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exceção não encontrada."));
    }
    private void validarDto(FrequenciaExcecaoDTO dto) {
        if (dto == null || dto.empregadoId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        if (dto.tipo() == null || dto.tipo().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo obrigatório.");
    }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período obrigatório.");
        if (inicio.isAfter(fim)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inicial não pode ser posterior à data final.");
    }
    private void validarSobreposicao(Long empregadoId, String tipo, LocalDate inicio, LocalDate fim, Long ignorarId) {
        if (repositorio.existsOverlap(empregadoId, tipo, inicio, fim, ignorarId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma exceção deste tipo para o período informado.");
    }
    private UsuarioAplicacao usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarios.findByUsername(auth == null ? "" : auth.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado."));
    }
    private String normalizarTipo(String texto) { return texto.trim().toUpperCase(Locale.ROOT); }
    private FrequenciaExcecaoDTO paraDTO(FrequenciaExcecao e) {
        return new FrequenciaExcecaoDTO(e.getId(), e.getEmpregado().getId(),
                e.getEmpregadoNomeFotografia(),
                e.getDataInicio(), e.getDataFim(), e.getTipo(), e.getStatus(), e.getObservacao());
    }
    private FrequenciaExcecaoDTO paraConsulta(FrequenciaExcecao e, UsuarioAplicacao user) {
        FrequenciaExcecaoDTO foto = paraDTO(e);
        return new FrequenciaExcecaoDTO(foto.id(), foto.empregadoId(), temporal.nomeParaConsulta(foto.empregadoNome(), user), foto.dataInicio(), foto.dataFim(),
                foto.tipo(), foto.status(), temporal.narrativaParaConsulta(foto.observacao(), user));
    }
}
