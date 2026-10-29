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
public class GestaoLocalServico {
    private final GestaoLocalRepositorio repositorio;
    private final SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;

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
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) return List.of(paraDTO(user.getGestaoLocal()));
        return List.of();
    }

    @Transactional
    public GestaoLocalDTO criar(GestaoLocalDTO dto) {
        GestaoLocal entity = new GestaoLocal();
        mapear(dto, entity, null);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public GestaoLocalDTO atualizar(Long id, GestaoLocalDTO dto) {
        GestaoLocal entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
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

    private GestaoLocalDTO paraDTO(GestaoLocal entity) {
        SuperintendenciaEstadual se = entity.getSuperintendencia();
        return new GestaoLocalDTO(entity.getId(), entity.getNome(), entity.getCodigo(), entity.getAtivo(),
                se == null ? null : se.getId(), se == null ? null : se.getNome(), se == null ? null : se.getUf());
    }
}
