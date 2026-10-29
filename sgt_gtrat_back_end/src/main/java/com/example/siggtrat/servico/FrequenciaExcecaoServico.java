package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.FrequenciaExcecaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.FrequenciaExcecaoRepositorio;
import com.example.siggtrat.especificacao.EspecificacaoEscopo;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional
public class FrequenciaExcecaoServico {

    private final FrequenciaExcecaoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;

    public FrequenciaExcecaoServico(FrequenciaExcecaoRepositorio repositorio,
                                    EmpregadoRepositorio empregadoRepositorio,
                                    UsuarioAplicacaoRepositorio appUserRepositorio,
                                    EscopoAcessoUsuarioServico userAccessScopeServico,
                                    AcessoOrganizacionalServico acessoOrganizacionalServico) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.appUserRepositorio = appUserRepositorio;
        this.userAccessScopeServico = userAccessScopeServico;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaExcecaoDTO> listarAtivasPorData(LocalDate data,
                                                       Long lotacaoId,
                                                       Long jornadaId,
                                                       Long contratoId,
                                                       Pageable pageable) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        UsuarioAplicacao user = usuarioAtual();
        if (acessoOrganizacionalServico.ehGlobal(user) && lotacaoId == null && jornadaId == null && contratoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos lotação, turno ou contrato para consultar exceções.");
        }

        Specification<FrequenciaExcecao> spec = (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get("dataInicio"), data),
                cb.greaterThanOrEqualTo(root.get("dataFim"), data),
                cb.notEqual(cb.upper(root.get("status")), ConstantesDominio.STATUS_CANCELADO)
        );
        if (lotacaoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empregado").get("lotacao").get("id"), lotacaoId));
        }
        if (jornadaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empregado").get("jornada").get("id"), jornadaId));
        }
        if (contratoId != null) {
            spec = spec.and((root, query, cb) -> {
                // EXISTS evita multiplicar a entidade raiz pela coleção de contratos.
                // Além de dispensar DISTINCT, preserva a ordenação
                // por empregado.nome usada pela frequência no MySQL.
                var contratoDoEmpregado = query.subquery(Long.class);
                var empregadoContrato = contratoDoEmpregado.from(Empregado.class);
                var contratos = empregadoContrato.join("contratos", jakarta.persistence.criteria.JoinType.INNER);
                contratoDoEmpregado.select(empregadoContrato.get("id"));
                contratoDoEmpregado.where(
                        cb.equal(empregadoContrato.get("id"), root.get("empregado").get("id")),
                        cb.equal(contratos.get("id"), contratoId));
                return cb.exists(contratoDoEmpregado);
            });
        }
        spec = spec.and(porEscopo(user));
        Pageable limitado = UtilitarioPaginacao.limitar(pageable,
                Sort.by("dataInicio").ascending().and(Sort.by("empregado.nome").ascending()));
        return repositorio.findAll(spec, limitado).map(this::paraDTO);
    }

    public FrequenciaExcecaoDTO criar(FrequenciaExcecaoDTO dto) {
        validarDto(dto);
        Empregado empregado = validarEBloquearEmpregado(dto.empregadoId());
        String tipo = dto.tipo().trim().toUpperCase(Locale.ROOT);
        validarSobreposicao(empregado.getId(), tipo, dto.dataInicio(), dto.dataFim(), null);

        FrequenciaExcecao obj = new FrequenciaExcecao();
        obj.setEmpregado(empregado);
        obj.setDataInicio(dto.dataInicio());
        obj.setDataFim(dto.dataFim());
        obj.setTipo(tipo);
        obj.setStatus(dto.status() == null || dto.status().isBlank()
                ? ConstantesDominio.STATUS_APROVADO : dto.status().trim().toUpperCase(Locale.ROOT));
        obj.setObservacao(dto.observacao());
        return paraDTO(repositorio.save(obj));
    }

    public FrequenciaExcecaoDTO atualizar(Long id, FrequenciaExcecaoDTO dto) {
        FrequenciaExcecao existing = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exceção não encontrada."));
        Long empregadoId = dto.empregadoId() != null ? dto.empregadoId() : existing.getEmpregado().getId();
        Empregado empregado = validarEBloquearEmpregado(empregadoId);
        LocalDate inicio = dto.dataInicio() != null ? dto.dataInicio() : existing.getDataInicio();
        LocalDate fim = dto.dataFim() != null ? dto.dataFim() : existing.getDataFim();
        String tipo = dto.tipo() != null && !dto.tipo().isBlank()
                ? dto.tipo().trim().toUpperCase(Locale.ROOT) : existing.getTipo();
        validarPeriodo(inicio, fim);
        validarSobreposicao(empregado.getId(), tipo, inicio, fim, existing.getId());

        existing.setEmpregado(empregado);
        existing.setDataInicio(inicio);
        existing.setDataFim(fim);
        existing.setTipo(tipo);
        if (dto.status() != null) existing.setStatus(dto.status().trim().toUpperCase(Locale.ROOT));
        if (dto.observacao() != null) existing.setObservacao(dto.observacao());
        return paraDTO(repositorio.save(existing));
    }

    public void excluir(Long id) {
        FrequenciaExcecao existing = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exceção não encontrada."));
        validarEBloquearEmpregado(existing.getEmpregado().getId());
        repositorio.delete(existing);
    }

    private void validarDto(FrequenciaExcecaoDTO dto) {
        if (dto == null || dto.empregadoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
        }
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        if (dto.tipo() == null || dto.tipo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo obrigatório.");
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período obrigatório.");
        }
        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data inicial não pode ser posterior à data final.");
        }
    }

    private void validarSobreposicao(Long empregadoId, String tipo, LocalDate inicio, LocalDate fim, Long ignoreId) {
        if (repositorio.existsOverlap(empregadoId, tipo, inicio, fim, ignoreId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma exceção deste tipo para o período informado.");
        }
    }

    private Empregado validarEBloquearEmpregado(Long empregadoId) {
        if (empregadoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
        // O lock torna atômica a validação de sobreposição para o mesmo empregado.
        Empregado empregado = empregadoRepositorio.buscarPorIdParaAtualizacao(empregadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado."));
        UsuarioAplicacao user = usuarioAtual();
        if (!acessoOrganizacionalServico.ehGestaoLocal(user)
                || !userAccessScopeServico.podeGerenciar(user, empregado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para gerir este colaborador.");
        }
        return empregado;
    }

    private Specification<FrequenciaExcecao> porEscopo(UsuarioAplicacao user) {
        if (acessoOrganizacionalServico.ehGlobal(user)) return (root, query, cb) -> cb.conjunction();
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
            Long id = user.getGestaoLocal().getId();
            return (root, query, cb) -> cb.equal(root.get("empregado").get("gestaoLocal").get("id"), id);
        }
        return EspecificacaoEscopo.filtrarPorEscopo(user, userAccessScopeServico);
    }

    private UsuarioAplicacao usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "";
        return appUserRepositorio.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado."));
    }

    private FrequenciaExcecaoDTO paraDTO(FrequenciaExcecao entity) {
        return new FrequenciaExcecaoDTO(
                entity.getId(), entity.getEmpregado().getId(), entity.getEmpregado().getNome(),
                entity.getDataInicio(), entity.getDataFim(), entity.getTipo(),
                entity.getStatus(), entity.getObservacao()
        );
    }
}
