package com.example.siggtrat.servico;

import com.example.siggtrat.dto.RegraAdicionalDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.RegraAdicional;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.RegraAdicionalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

@Service
public class RegraAdicionalServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RegraAdicionalServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RegraAdicionalRepositorio repositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;

    public RegraAdicionalServico(RegraAdicionalRepositorio repositorio, JornadaRepositorio jornadaRepositorio,
                                  AcessoOrganizacionalServico acessoOrganizacionalServico) {
        this.repositorio = repositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
    }

    @Transactional(readOnly = true)
    public List<RegraAdicionalDTO> listar(Long jornadaId) {
        Jornada jornada = obterJornadaVisivel(jornadaId);
        return repositorio.findByJornada_IdOrderByOrdemAsc(jornada.getId()).stream().map(this::paraDTO).toList();
    }

    @Transactional
    public RegraAdicionalDTO criar(Long jornadaId, RegraAdicionalDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        Jornada jornada = obterJornadaGerenciavel(jornadaId, usuario);
        RegraAdicional regra = new RegraAdicional();
        aplicarDados(dto, regra);
        regra.setJornada(jornada);
        validar(regra);
        RegraAdicional salva = repositorio.save(regra);
        repositorio.flush();
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        return paraDTO(salva);
    }

    @Transactional
    public RegraAdicionalDTO atualizar(Long id, RegraAdicionalDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        RegraAdicional existente = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra adicional não encontrada."));
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        Jornada jornada = obterJornadaGerenciavel(existente.getJornada().getId());
        aplicarDados(dto, existente);
        existente.setJornada(jornada);
        validar(existente);
        return paraDTO(repositorio.save(existente));
    }

    @Transactional
    public void excluir(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        RegraAdicional existente = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra adicional não encontrada."));
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        obterJornadaGerenciavel(existente.getJornada().getId());
        repositorio.delete(existente);
    }

    private void aplicarDados(RegraAdicionalDTO dto, RegraAdicional r) {
        if (dto == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Regra inválida.");
        r.setTipo(dto.tipo()); r.setInicio(dto.inicio()); r.setFim(dto.fim()); r.setPercentual(dto.percentual());
        r.setAtivo(dto.ativo()); r.setOrdem(dto.ordem());
    }

    private void validar(RegraAdicional regra) {
        if (regra.getTipo() == null || regra.getTipo().isBlank()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Tipo da regra é obrigatório.");
        validarHorario("Início", regra.getInicio()); validarHorario("Fim", regra.getFim());
        boolean ini = regra.getInicio() != null && !regra.getInicio().isBlank();
        boolean fim = regra.getFim() != null && !regra.getFim().isBlank();
        if (ini ^ fim) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe início e fim da janela, ou deixe ambos em branco.");
        if (regra.getPercentual() != null && regra.getPercentual().signum() < 0) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Percentual não pode ser negativo.");
        if (regra.getAtivo() == null) regra.setAtivo(true);
    }

    private void validarHorario(String rotulo, String horario) {
        if (horario == null || horario.isBlank()) return;
        if (horario.length() != 5) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio(rotulo + " deve estar no formato HH:mm");
        try { LocalTime.parse(horario); } catch (Exception e) { throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio(rotulo + " inválido (use HH:mm)"); }
    }

    private Jornada obterJornadaVisivel(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        Jornada jornada = jornadaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado."));
        if (acessoOrganizacionalServico.ehGlobal(usuario) || acessoOrganizacionalServico.podeAcessarJornada(usuario, jornada)) return jornada;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado no seu escopo.");
    }

    private Jornada obterJornadaGerenciavel(Long id) {
        return obterJornadaGerenciavel(id, acessoOrganizacionalServico.usuarioAtual());
    }

    private Jornada obterJornadaGerenciavel(Long id, UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(usuario))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente a Gestão Local pode gerir regras de seus turnos.");
        Jornada jornada = jornadaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado."));
        if (jornada.getGestaoLocal() != null && acessoOrganizacionalServico.podeAcessarGestaoLocal(usuario, jornada.getGestaoLocal())) return jornada;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno não encontrado no seu escopo.");
    }

    private RegraAdicionalDTO paraDTO(RegraAdicional r) {
        return new RegraAdicionalDTO(r.getId(), r.getTipo(), r.getInicio(), r.getFim(), r.getPercentual(), r.getAtivo(), r.getOrdem());
    }
}
