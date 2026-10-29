package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.FrequenciaAdicionalDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.FrequenciaAdicionalRepositorio;
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
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@Transactional
public class FrequenciaAdicionalServico {

    private static final long MAX_RANGE_DAYS = 366;

    private final FrequenciaAdicionalRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;

    public FrequenciaAdicionalServico(FrequenciaAdicionalRepositorio repositorio,
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
    public Page<FrequenciaAdicionalDTO> listar(LocalDate data,
                                             LocalDate inicio,
                                             LocalDate fim,
                                             String tipo,
                                             Long lotacaoId,
                                             Long jornadaId,
                                             Long contratoId,
                                             Pageable pageable) {
        boolean consultaDia = data != null;
        boolean consultaRange = inicio != null || fim != null;
        if (!consultaDia && !consultaRange) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe data ou o período completo (início e fim).");
        }
        if (consultaDia && consultaRange) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use data ou período, não ambos.");
        }
        if (consultaRange) {
            if (inicio == null || fim == null || inicio.isAfter(fim)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
            }
            if (ChronoUnit.DAYS.between(inicio, fim) > MAX_RANGE_DAYS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O período máximo para adicionais é de " + MAX_RANGE_DAYS + " dias.");
            }
        } else {
            inicio = data;
            fim = data;
        }

        UsuarioAplicacao user = usuarioAtual();
        if (acessoOrganizacionalServico.ehGlobal(user) && lotacaoId == null && jornadaId == null && contratoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos lotação, turno ou contrato para consultar adicionais.");
        }

        LocalDate dataInicio = inicio;
        LocalDate dataFim = fim;
        Specification<FrequenciaAdicional> spec = (root, query, cb) ->
                cb.between(root.get("dataReferencia"), dataInicio, dataFim);
        if (tipo != null && !tipo.isBlank()) {
            String tipoNormalizado = tipo.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("tipo")), tipoNormalizado));
        }
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
                Sort.by("dataReferencia").ascending().and(Sort.by("empregado.nome").ascending()));
        return repositorio.findAll(spec, limitado).map(this::paraDTO);
    }

    public FrequenciaAdicionalDTO criar(FrequenciaAdicionalDTO dto) {
        Empregado empregado = validarEObterEmpregado(dto.empregadoId());
        if (dto.dataReferencia() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        if (dto.tipo() == null || dto.tipo().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo obrigatório.");

        FrequenciaAdicional obj = new FrequenciaAdicional();
        obj.setEmpregado(empregado);
        obj.setDataReferencia(dto.dataReferencia());
        obj.setTipo(dto.tipo().trim().toUpperCase(Locale.ROOT));
        obj.setMinutos(dto.minutos());
        obj.setQuantidade(dto.quantidade());
        obj.setStatus(dto.status() == null || dto.status().isBlank() ? ConstantesDominio.STATUS_CALCULADO : dto.status());
        obj.setJustificativa(dto.justificativa());
        return paraDTO(repositorio.save(obj));
    }

    public FrequenciaAdicionalDTO atualizar(Long id, FrequenciaAdicionalDTO dto) {
        FrequenciaAdicional existing = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado."));
        validarEObterEmpregado(existing.getEmpregado().getId());
        if (dto.empregadoId() != null && !dto.empregadoId().equals(existing.getEmpregado().getId())) {
            existing.setEmpregado(validarEObterEmpregado(dto.empregadoId()));
        }
        if (dto.dataReferencia() != null) existing.setDataReferencia(dto.dataReferencia());
        if (dto.tipo() != null && !dto.tipo().isBlank()) existing.setTipo(dto.tipo().trim().toUpperCase(Locale.ROOT));
        if (dto.minutos() != null) existing.setMinutos(dto.minutos());
        if (dto.quantidade() != null) existing.setQuantidade(dto.quantidade());
        if (dto.status() != null) existing.setStatus(dto.status());
        if (dto.justificativa() != null) existing.setJustificativa(dto.justificativa());
        return paraDTO(repositorio.save(existing));
    }

    public void excluir(Long id) {
        FrequenciaAdicional existing = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado."));
        validarEObterEmpregado(existing.getEmpregado().getId());
        repositorio.delete(existing);
    }

    private Empregado validarEObterEmpregado(Long empregadoId) {
        if (empregadoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do empregado obrigatório.");
        Empregado empregado = empregadoRepositorio.findById(empregadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado."));
        UsuarioAplicacao user = usuarioAtual();
        if (!acessoOrganizacionalServico.ehGestaoLocal(user)
                || !userAccessScopeServico.podeGerenciar(user, empregado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para gerir este colaborador.");
        }
        return empregado;
    }

    private Specification<FrequenciaAdicional> porEscopo(UsuarioAplicacao user) {
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

    private FrequenciaAdicionalDTO paraDTO(FrequenciaAdicional entity) {
        return new FrequenciaAdicionalDTO(
                entity.getId(), entity.getEmpregado().getId(), entity.getEmpregado().getNome(),
                entity.getDataReferencia(), entity.getTipo(), entity.getMinutos(), entity.getQuantidade(),
                entity.getStatus(), entity.getJustificativa(),
                entity.getFrequencia() != null ? entity.getFrequencia().getId() : null
        );
    }
}
