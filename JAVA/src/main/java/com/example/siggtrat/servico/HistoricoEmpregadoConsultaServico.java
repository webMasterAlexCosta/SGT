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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;

@Service
public class HistoricoEmpregadoConsultaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(HistoricoEmpregadoConsultaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final HistoricoEmpregadoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final HistoricoIntegridadeServico integridadeServico;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final AutorizacaoTemporalOcorrenciaServico autorizacaoOcorrencia;

    public HistoricoEmpregadoConsultaServico(HistoricoEmpregadoRepositorio repositorio,
                                          EmpregadoRepositorio empregadoRepositorio,
                                          AcessoOrganizacionalServico acessoOrganizacionalServico,
                                          HistoricoIntegridadeServico integridadeServico,
                                          MinimizadorDadosPessoaisServico minimizador,
                                          AutorizacaoTemporalOcorrenciaServico autorizacaoOcorrencia) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.integridadeServico = integridadeServico;
        this.minimizador = minimizador;
        this.autorizacaoOcorrencia = autorizacaoOcorrencia;
    }

    @Transactional(readOnly = true)
    public Page<HistoricoEmpregadoDTO> listarTodos(Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        return listarNoEscopo(user, null, pageable);
    }

    @Transactional(readOnly = true)
    public List<HistoricoEmpregadoDTO> buscarPorEmpregado(Long id, Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirEmpregadoExistente(id);
        return listarNoEscopo(user, id, pageable).getContent();
    }

    private Page<HistoricoEmpregadoDTO> listarNoEscopo(UsuarioAplicacao user, Long empregadoId, Pageable pageable) {
        exigirPerfilDeConsulta(user);
        Pageable limitado = UtilitarioPaginacao.limitar(pageable, Sort.by("dataEvento").descending().and(Sort.by("id").descending()));
        Sort ordenacao = limitado.getSort().getOrderFor("id") == null ? limitado.getSort().and(Sort.by("id")) : limitado.getSort();
        List<HistoricoEmpregadoDTO> selecionados = new ArrayList<>();
        long total = 0; int lote = 0;
        Page<HistoricoEmpregado> candidatos;
        // A fotografia antiga não pode pré-filtrar um fato cuja responsabilidade foi retificada.
        // Filtrar antes da paginação pública também impede vazamento no totalElements.
        do {
            Pageable recorte = PageRequest.of(lote++, 200, ordenacao);
            candidatos = empregadoId == null ? repositorio.findAll(recorte)
                    : repositorio.findByEmpregado_IdOrderByDataEventoDesc(empregadoId, recorte);
            for (HistoricoEmpregado h : candidatos) {
                if (!podeConsultar(user, h)) continue;
                if (total >= limitado.getOffset() && selecionados.size() < limitado.getPageSize()) selecionados.add(paraDTO(h, user));
                total++;
            }
        } while (candidatos.hasNext());
        return new PageImpl<>(selecionados, limitado, total);
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
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        HistoricoEmpregado historico = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Histórico não encontrado."));
        exigirEscopoDoRegistro(user, historico);
        return paraDTO(historico, user);
    }

    private void exigirEmpregadoExistente(Long empregadoId) {
        if (!empregadoRepositorio.existsById(empregadoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado.");
        }
    }

    private void exigirAlgumPeriodoNoEscopo(UsuarioAplicacao user, Long empregadoId) {
        exigirPerfilDeConsulta(user);
        if (acessoOrganizacionalServico.ehGlobal(user)) return;
        int lote = 0; Page<HistoricoEmpregado> candidatos;
        do {
            candidatos = repositorio.findByEmpregado_IdOrderByDataEventoDesc(empregadoId, PageRequest.of(lote++, 200));
            if (candidatos.stream().anyMatch(h -> podeConsultar(user, h))) return;
        } while (candidatos.hasNext());
        throw acessoNegado();
    }

    private void exigirEscopoDoRegistro(UsuarioAplicacao user, HistoricoEmpregado historico) {
        exigirPerfilDeConsulta(user);
        if (!podeConsultar(user, historico)) throw acessoNegado();
    }

    private void exigirPerfilDeConsulta(UsuarioAplicacao user) {
        if (user == null || !Boolean.TRUE.equals(user.getActive())
                || !(acessoOrganizacionalServico.ehGlobal(user)
                || acessoOrganizacionalServico.ehGestorEstadual(user) && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null
                || acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null)) throw acessoNegado();
    }

    private boolean podeConsultar(UsuarioAplicacao user, HistoricoEmpregado historico) {
        if (historico.getOcorrencia() != null && historico.getVersaoAssinatura() != null && historico.getVersaoAssinatura() >= 3)
            return autorizacaoOcorrencia.podeConsultar(user, autorizacaoOcorrencia.resolver(historico.getOcorrencia()));
        // Sem vínculo explícito autenticado, a prova genérica/legada conserva seu próprio escopo.
        if (acessoOrganizacionalServico.ehGlobal(user)) return true;
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null
                && java.util.Objects.equals(historico.getSuperintendenciaSnapshotId(),
                        acessoOrganizacionalServico.superintendenciaEfetiva(user).getId())) return true;
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null
                && java.util.Objects.equals(historico.getGestaoLocalSnapshotId(), user.getGestaoLocal().getId())) return true;
        return false;
    }

    private ResponseStatusException acessoNegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seu perfil não possui acesso a este período do histórico de empregados.");
    }

    private HistoricoEmpregadoDTO paraDTO(HistoricoEmpregado h, UsuarioAplicacao leitor) {
        EmpregadoReferenciaDTO empregado = h.getEmpregado() == null ? null : new EmpregadoReferenciaDTO(h.getEmpregado().getId());
        UsuarioReferenciaDTO usuario = h.getUsuario() == null ? null : new UsuarioReferenciaDTO(h.getUsuario().getId(), h.getUsuario().getUsername());
        // A narrativa é texto livre e pode conter valores pessoais anteriores/novos.
        // Minimizar somente a resposta mantém o original assinado e a cadeia verificável.
        String descricao = minimizador.restrito(h.getDescricao(), minimizador.nivelPadrao(leitor));
        return new HistoricoEmpregadoDTO(h.getId(), empregado, h.getDataEvento(), descricao, usuario, h.getHashIntegridade(),
                h.getVersion(), h.getDataEfetiva(), h.getVersaoAssinatura(), h.getOcorrencia() == null ? null : h.getOcorrencia().getId());
    }
}
