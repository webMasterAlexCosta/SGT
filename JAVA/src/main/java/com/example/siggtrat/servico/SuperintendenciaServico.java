package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SuperintendenciaServico {
    private final SuperintendenciaEstadualRepositorio repositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public SuperintendenciaServico(
            SuperintendenciaEstadualRepositorio repositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico
    ) {
        this.repositorio = repositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.gestaoAcessoHierarquicoServico = gestaoAcessoHierarquicoServico;
    }

    @Transactional(readOnly = true)
    public List<SuperintendenciaDTO> listar() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(user))
            return repositorio.findAllByOrderByUfAsc().stream().map(this::paraDTO).toList();
        Long id = user.getSuperintendencia() != null ? user.getSuperintendencia().getId()
                : user.getGestaoLocal() != null && user.getGestaoLocal().getSuperintendencia() != null
                    ? user.getGestaoLocal().getSuperintendencia().getId() : null;
        if (id == null) return List.of();
        return repositorio.findById(id).map(item -> List.of(paraDTO(item))).orElseGet(List::of);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SuperintendenciaDTO criar(SuperintendenciaDTO dto) {
        UsuarioAplicacao executor = exigirAdministradorAtual();
        validar(dto, null);
        SuperintendenciaEstadual entity = new SuperintendenciaEstadual();
        mapear(dto, entity);
        SuperintendenciaEstadual salva = repositorio.saveAndFlush(entity);
        // A unicidade pode ter feito o INSERT aguardar outra transação.
        // A mesma autoridade deve continuar válida antes do commit.
        revalidarAdministrador(executor);
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SuperintendenciaDTO atualizar(Long id, SuperintendenciaDTO dto) {
        UsuarioAplicacao executor = exigirAdministradorAtual();
        SuperintendenciaEstadual entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestor estadual não encontrado."));
        if (entityManager != null)
            entityManager.refresh(entity, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        revalidarAdministrador(executor);
        boolean estavaAtiva = Boolean.TRUE.equals(entity.getAtivo());
        validar(dto, id);
        String uf = dto.uf().trim().toUpperCase();
        if (!entity.getUf().equalsIgnoreCase(uf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A UF de uma SE é estrutural e não pode ser alterada.");
        }
        boolean solicitouAtivacao = dto.ativo() == null || Boolean.TRUE.equals(dto.ativo());
        if (!estavaAtiva && solicitouAtivacao) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Use a ação de reativação para gerar uma nova senha temporária do gestor estadual.");
        }
        mapear(dto, entity);
        SuperintendenciaEstadual salva = repositorio.save(entity);
        if (estavaAtiva && Boolean.FALSE.equals(salva.getAtivo())) {
            gestaoAcessoHierarquicoServico.inativarSuperintendencia(salva.getId());
        }
        repositorio.flush();
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AtivacaoUsuarioAplicacaoDTO ativar(Long id) {
        UsuarioAplicacao executor = exigirAdministradorAtual();
        SuperintendenciaEstadual entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestor estadual não encontrado."));
        if (entityManager != null)
            entityManager.refresh(entity, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        revalidarAdministrador(executor);
        if (Boolean.TRUE.equals(entity.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O gestor estadual já está ativo.");
        }
        AtivacaoUsuarioAplicacaoDTO resultado = gestaoAcessoHierarquicoServico.ativarGestorEstadual(id);
        repositorio.flush();
        return resultado;
    }

    @Transactional(readOnly = true)
    public void validarExclusao(Long id) {
        repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestor estadual não encontrado."));
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "As 27 SEs são estruturais e não podem ser excluídas. Inative a SE quando necessário.");
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

    private void validar(SuperintendenciaDTO dto, Long currentId) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da SE não informados.");
        String uf = dto.uf() == null ? "" : dto.uf().trim().toUpperCase();
        if (!uf.matches("[A-Z]{2}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UF deve conter exatamente duas letras.");
        if (!ConstantesDominio.UFS_BRASIL.contains(uf)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma UF válida do Brasil.");
        repositorio.findByUfIgnoreCase(uf).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma SE para esta UF.");
        });
    }

    private void mapear(SuperintendenciaDTO dto, SuperintendenciaEstadual entity) {
        entity.setUf(dto.uf());
        entity.setNome("SE/" + dto.uf().trim().toUpperCase());
        entity.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
    }

    private SuperintendenciaDTO paraDTO(SuperintendenciaEstadual e) {
        return new SuperintendenciaDTO(e.getId(), e.getNome(), e.getUf(), e.getAtivo());
    }
}
