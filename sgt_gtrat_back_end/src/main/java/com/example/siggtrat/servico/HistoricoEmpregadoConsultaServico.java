package com.example.siggtrat.servico;

import com.example.siggtrat.dto.EmpregadoReferenciaDTO;
import com.example.siggtrat.dto.HistoricoEmpregadoDTO;
import com.example.siggtrat.dto.UsuarioReferenciaDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.HistoricoEmpregadoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HistoricoEmpregadoConsultaServico {
    private final HistoricoEmpregadoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final HistoricoIntegridadeServico integridadeServico;

    public HistoricoEmpregadoConsultaServico(HistoricoEmpregadoRepositorio repositorio,
                                          EmpregadoRepositorio empregadoRepositorio,
                                          AcessoOrganizacionalServico acessoOrganizacionalServico,
                                          HistoricoIntegridadeServico integridadeServico) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.integridadeServico = integridadeServico;
    }

    @Transactional(readOnly = true)
    public Page<HistoricoEmpregadoDTO> listarTodos(Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Page<HistoricoEmpregado> page;
        if (acessoOrganizacionalServico.ehGlobal(user)) page = repositorio.findAll(pageable);
        else if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            page = repositorio.findBySuperintendenciaSnapshotIdOrderByDataEventoDesc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(user).getId(), pageable);
        } else if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
            page = repositorio.findByGestaoLocalSnapshotIdOrderByDataEventoDesc(
                    user.getGestaoLocal().getId(), pageable);
        } else throw acessoNegado();
        return page.map(this::paraDTO);
    }

    @Transactional(readOnly = true)
    public List<HistoricoEmpregadoDTO> buscarPorEmpregado(Long id, Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirEmpregadoExistente(id);
        Page<HistoricoEmpregado> page;
        if (acessoOrganizacionalServico.ehGlobal(user)) {
            page = repositorio.findByEmpregado_IdOrderByDataEventoDesc(id, pageable);
        } else if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            page = repositorio.findByEmpregado_IdAndSuperintendenciaSnapshotIdOrderByDataEventoDesc(
                    id, acessoOrganizacionalServico.superintendenciaEfetiva(user).getId(), pageable);
        } else if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
            page = repositorio.findByEmpregado_IdAndGestaoLocalSnapshotIdOrderByDataEventoDesc(
                    id, user.getGestaoLocal().getId(), pageable);
        } else throw acessoNegado();
        return page.getContent().stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public HistoricoIntegridadeServico.ResultadoVerificacao verificarIntegridade(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirEmpregadoExistente(id);
        exigirAlgumPeriodoNoEscopo(user, id);
        return integridadeServico.verificarEmpregado(id);
    }

    @Transactional(readOnly = true)
    public HistoricoEmpregadoDTO buscarPorId(Long id) {
        HistoricoEmpregado historico = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Histórico não encontrado."));
        exigirEscopoDoRegistro(acessoOrganizacionalServico.usuarioAtual(), historico);
        return paraDTO(historico);
    }

    private void exigirEmpregadoExistente(Long empregadoId) {
        if (!empregadoRepositorio.existsById(empregadoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado.");
        }
    }

    private void exigirAlgumPeriodoNoEscopo(UsuarioAplicacao user, Long empregadoId) {
        if (acessoOrganizacionalServico.ehGlobal(user)) return;
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null
                && repositorio.existsByEmpregado_IdAndSuperintendenciaSnapshotId(
                        empregadoId, acessoOrganizacionalServico.superintendenciaEfetiva(user).getId())) return;
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null
                && repositorio.existsByEmpregado_IdAndGestaoLocalSnapshotId(
                        empregadoId, user.getGestaoLocal().getId())) return;
        throw acessoNegado();
    }

    private void exigirEscopoDoRegistro(UsuarioAplicacao user, HistoricoEmpregado historico) {
        if (acessoOrganizacionalServico.ehGlobal(user)) return;
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null
                && java.util.Objects.equals(historico.getSuperintendenciaSnapshotId(),
                        acessoOrganizacionalServico.superintendenciaEfetiva(user).getId())) return;
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null
                && java.util.Objects.equals(historico.getGestaoLocalSnapshotId(), user.getGestaoLocal().getId())) return;
        throw acessoNegado();
    }

    private ResponseStatusException acessoNegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seu perfil não possui acesso a este período do histórico de empregados.");
    }

    private HistoricoEmpregadoDTO paraDTO(HistoricoEmpregado h) {
        EmpregadoReferenciaDTO empregado = h.getEmpregado() == null ? null : new EmpregadoReferenciaDTO(h.getEmpregado().getId());
        UsuarioReferenciaDTO usuario = h.getUsuario() == null ? null : new UsuarioReferenciaDTO(h.getUsuario().getId(), h.getUsuario().getUsername());
        return new HistoricoEmpregadoDTO(h.getId(), empregado, h.getDataEvento(), h.getDescricao(), usuario, h.getHashIntegridade(), h.getVersion());
    }
}
