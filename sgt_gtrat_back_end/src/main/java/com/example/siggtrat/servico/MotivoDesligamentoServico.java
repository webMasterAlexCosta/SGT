package com.example.siggtrat.servico;

import com.example.siggtrat.dto.MotivoDesligamentoDTO;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.MotivoDesligamentoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MotivoDesligamentoServico {

    private final MotivoDesligamentoRepositorio repositorio;
    private final AcessoOrganizacionalServico acesso;

    public MotivoDesligamentoServico(
            MotivoDesligamentoRepositorio repositorio,
            AcessoOrganizacionalServico acesso
    ) {
        this.repositorio = repositorio;
        this.acesso = acesso;
    }

    public List<MotivoDesligamentoDTO> listar() {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirConsultaContrato(usuario);
        List<MotivoDesligamento> itens;
        if (acesso.ehGlobal(usuario) || acesso.ehPresidente(usuario)) {
            itens = repositorio.findAllByOrderByGestaoLocalNomeAscDescricaoAsc();
        } else if (acesso.ehGestorEstadual(usuario) && acesso.superintendenciaEfetiva(usuario) != null) {
            itens = repositorio.findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(
                    acesso.superintendenciaEfetiva(usuario).getId());
        } else if (usuario.getGestaoLocal() != null) {
            itens = repositorio.findByGestaoLocalIdOrderByDescricaoAsc(usuario.getGestaoLocal().getId());
        } else {
            itens = List.of();
        }
        return itens.stream().map(this::paraDTO).toList();
    }

    public MotivoDesligamentoDTO buscarPorId(Long id) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        MotivoDesligamento entidade = obter(id);
        exigirConsultaNoEscopo(usuario, entidade.getGestaoLocal());
        return paraDTO(entidade);
    }

    @Transactional
    public MotivoDesligamentoDTO criar(MotivoDesligamentoDTO dto) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirGestorLocal(usuario);
        validar(dto);
        GestaoLocal gestao = usuario.getGestaoLocal();
        String descricao = normalizar(dto.descricao());
        if (repositorio.findByGestaoLocalIdAndDescricaoIgnoreCase(gestao.getId(), descricao).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um Motivo de Desligamento com esta descrição na Gestão Local.");
        }
        MotivoDesligamento entidade = new MotivoDesligamento();
        entidade.setGestaoLocal(gestao);
        entidade.setDescricao(descricao);
        entidade.setCategoria(dto.categoria());
        entidade.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        entidade.setCriadoPorId(usuario.getId());
        entidade.setCriadoPorUsername(usuario.getUsername());
        return paraDTO(repositorio.save(entidade));
    }

    @Transactional
    public MotivoDesligamentoDTO atualizar(Long id, MotivoDesligamentoDTO dto) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirGestorLocal(usuario);
        validar(dto);
        MotivoDesligamento entidade = obter(id);
        if (entidade.getGestaoLocal() == null || usuario.getGestaoLocal() == null
                || !Objects.equals(entidade.getGestaoLocal().getId(), usuario.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Motivo de Desligamento não encontrado na sua Gestão Local.");
        }
        String descricao = normalizar(dto.descricao());
        repositorio.findByGestaoLocalIdAndDescricaoIgnoreCase(usuario.getGestaoLocal().getId(), descricao)
                .filter(outro -> !Objects.equals(outro.getId(), id))
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Já existe um Motivo de Desligamento com esta descrição na Gestão Local.");
                });
        entidade.setDescricao(descricao);
        entidade.setCategoria(dto.categoria());
        if (dto.ativo() != null) entidade.setAtivo(dto.ativo());
        return paraDTO(repositorio.save(entidade));
    }

    private void validar(MotivoDesligamentoDTO dto) {
        if (dto == null || dto.descricao() == null || dto.descricao().isBlank() || dto.categoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Descrição e categoria do Motivo de Desligamento são obrigatórias.");
        }
    }

    private MotivoDesligamento obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Motivo de Desligamento não encontrado."));
    }

    private void exigirGestorLocal(UsuarioAplicacao usuario) {
        if (!acesso.ehGestaoLocal(usuario) || usuario.getGestaoLocal() == null
                || !Boolean.TRUE.equals(usuario.getGestaoLocal().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local ativo administra Motivos de Desligamento.");
        }
    }

    private void exigirConsultaNoEscopo(UsuarioAplicacao usuario, GestaoLocal gestaoLocal) {
        if (acesso.ehGlobal(usuario) || acesso.ehPresidente(usuario)) return;
        if (!acesso.podeAcessarGestaoLocal(usuario, gestaoLocal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Motivo de Desligamento não encontrado no seu escopo.");
        }
    }

    private static String normalizar(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }

    private MotivoDesligamentoDTO paraDTO(MotivoDesligamento entidade) {
        GestaoLocal gestao = entidade.getGestaoLocal();
        SuperintendenciaEstadual se = gestao != null ? gestao.getSuperintendencia() : null;
        return new MotivoDesligamentoDTO(
                entidade.getId(), entidade.getDescricao(), entidade.getCategoria(), entidade.getAtivo(),
                gestao != null ? gestao.getId() : null,
                gestao != null ? gestao.getNome() : null,
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                se != null ? se.getUf() : null,
                entidade.getCriadoEm(), entidade.getCriadoPorId(), entidade.getCriadoPorUsername(),
                entidade.getAtualizadoEm(), entidade.getVersion()
        );
    }
}
