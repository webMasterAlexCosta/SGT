package com.example.siggtrat.servico;

import com.example.siggtrat.dto.GestaoLocalDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GestaoLocalServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(GestaoLocalServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final GestaoLocalRepositorio repositorio;
    private final SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public GestaoLocalServico(GestaoLocalRepositorio repositorio,
                        SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
                        AcessoOrganizacionalServico acessoOrganizacionalServico,
                        GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico) {
        this.repositorio = repositorio;
        this.superintendenciaRepositorio = superintendenciaRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.gestaoAcessoHierarquicoServico = gestaoAcessoHierarquicoServico;
    }

    @Transactional(readOnly = true)
    public List<GestaoLocalDTO> listar(Long superintendenciaId) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(user)) {
            List<GestaoLocal> result = superintendenciaId == null
                    ? repositorio.findAllByOrderByNomeAsc()
                    : repositorio.findBySuperintendenciaIdOrderByNomeAsc(superintendenciaId);
            return result.stream().map(this::paraDTO).toList();
        }
        if (acessoOrganizacionalServico.ehGestorEstadual(user) && user.getSuperintendencia() != null) {
            return repositorio.findBySuperintendenciaIdOrderByNomeAsc(user.getSuperintendencia().getId())
                    .stream().map(this::paraDTO).toList();
        }
        if ((acessoOrganizacionalServico.ehGestaoLocal(user) || acessoOrganizacionalServico.ehOperacional(user))
                && user.getGestaoLocal() != null) return List.of(paraDTO(user.getGestaoLocal()));
        return List.of();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public GestaoLocalDTO criar(GestaoLocalDTO dto) {
        UsuarioAplicacao executor = exigirAdministradorAtual();
        GestaoLocal entity = new GestaoLocal();
        mapear(dto, entity, null);
        GestaoLocal salva = repositorio.saveAndFlush(entity);
        // O INSERT pode esperar em FK ou unicidade. A autorização original
        // precisa continuar válida antes de confirmar qualquer nova Gestão.
        revalidarAdministrador(executor);
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public GestaoLocalDTO atualizar(Long id, GestaoLocalDTO dto) {
        UsuarioAplicacao executor = exigirAdministradorAtual();
        GestaoLocal entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
        if (entityManager != null) {
            // Ordem estrutural: SE, Gestão, executor. A atividade do pai não
            // pode mudar entre a validação e o commit de um filho ativo.
            entityManager.refresh(entity.getSuperintendencia(), jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            entityManager.refresh(entity, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        revalidarAdministrador(executor);
        boolean estavaAtivo = Boolean.TRUE.equals(entity.getAtivo());
        Long atual = entity.getSuperintendencia() == null ? null : entity.getSuperintendencia().getId();
        if (dto != null && dto.superintendenciaId() != null && atual != null && !atual.equals(dto.superintendenciaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A Gestão Estadual/UF de uma Gestão Local é estrutural e não pode ser alterada.");
        }
        mapear(dto, entity, id);
        GestaoLocal salvo = repositorio.save(entity);
        if (estavaAtivo && Boolean.FALSE.equals(salvo.getAtivo())) {
            gestaoAcessoHierarquicoServico.inativarGestaoLocal(salvo.getId());
        }
        repositorio.flush();
        return paraDTO(salvo);
    }

    @Transactional(readOnly = true)
    public void validarExclusao(Long id) {
        repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A Gestão Local é estrutural e não pode ser excluída. Inative-a quando necessário.");
    }

    private void mapear(GestaoLocalDTO dto, GestaoLocal entity, Long currentId) {
        if (dto == null || dto.nome() == null || dto.nome().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da Gestão Local é obrigatório.");
        if (dto.codigo() == null || dto.codigo().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código da Gestão Local é obrigatório.");
        if (dto.superintendenciaId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Estadual/UF da Gestão Local é obrigatória.");

        SuperintendenciaEstadual se = superintendenciaRepositorio.findById(dto.superintendenciaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestor estadual inválido."));
        if (entityManager != null)
            entityManager.refresh(se, jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        boolean gestaoLocalAtiva = dto.ativo() == null || Boolean.TRUE.equals(dto.ativo());
        if (gestaoLocalAtiva && !Boolean.TRUE.equals(se.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível ativar uma Gestão Local enquanto a Gestão Estadual estiver inativa.");
        }
        repositorio.findBySuperintendenciaIdAndCodigoIgnoreCase(se.getId(), dto.codigo().trim()).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe uma Gestão Local com este código na Gestão Estadual selecionada.");
        });
        entity.setNome(dto.nome());
        entity.setCodigo(dto.codigo());
        entity.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        entity.setSuperintendencia(se);
    }

    private UsuarioAplicacao exigirAdministradorAtual() {
        UsuarioAplicacao executor = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.ehAdministrador(executor))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Administrador pode alterar a estrutura organizacional.");
        return executor;
    }

    private void revalidarAdministrador(UsuarioAplicacao executor) {
        acessoOrganizacionalServico.revalidarAposEspera(executor);
        if (!acessoOrganizacionalServico.ehAdministrador(executor))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Administrador pode alterar a estrutura organizacional.");
    }

    private GestaoLocalDTO paraDTO(GestaoLocal entity) {
        SuperintendenciaEstadual se = entity.getSuperintendencia();
        return new GestaoLocalDTO(entity.getId(), entity.getNome(), entity.getCodigo(), entity.getAtivo(),
                se == null ? null : se.getId(), se == null ? null : se.getNome(), se == null ? null : se.getUf());
    }
}
