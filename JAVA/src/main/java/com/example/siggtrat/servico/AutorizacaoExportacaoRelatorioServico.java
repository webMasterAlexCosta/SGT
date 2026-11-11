package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Fotografia de autorização persistida; não depende das autoridades antigas de um JWT nem de cache. */
@Service
public class AutorizacaoExportacaoRelatorioServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AutorizacaoExportacaoRelatorioServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final UsuarioAplicacaoRepositorio usuarios;
    private final AcessoOrganizacionalServico acesso;
    private final MinimizadorDadosPessoaisServico minimizador;

    public AutorizacaoExportacaoRelatorioServico(UsuarioAplicacaoRepositorio usuarios,
                                                 AcessoOrganizacionalServico acesso,
                                                 MinimizadorDadosPessoaisServico minimizador) {
        this.usuarios = usuarios;
        this.acesso = acesso;
        this.minimizador = minimizador;
    }

    // A consulta e o streaming podem ocorrer em threads/transações diferentes.
    // Uma transação nova impede que o contexto JPA ou o snapshot REPEATABLE_READ
    // da apuração ocultem uma revogação já confirmada em outra conexão.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Fotografia capturar(String username) {
        UsuarioAplicacao usuario = usuarios.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário da exportação não está disponível."));
        if (!Boolean.TRUE.equals(usuario.getActive()) || !acesso.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A autorização atual não permite acesso à exportação.");
        }
        Map<String, Long> revisoes = new TreeMap<>();
        revisoes.put("usuario:" + usuario.getId(), usuario.getVersion());
        Unidade estadual = unidade(acesso.superintendenciaEfetiva(usuario), revisoes);
        Gestao gestao = gestao(usuario.getGestaoLocal(), revisoes);
        Par principal = par(usuario.getLotacao(), usuario.getJornada(), revisoes);
        List<Par> adicionais = usuario.getVinculosGestao().stream().map(vinculo -> {
            revisoes.put("vinculo:" + vinculo.getId(), vinculo.getVersion());
            return par(vinculo.getLotacao(), vinculo.getJornada(), revisoes);
        }).distinct().sorted(Comparator.comparing(Par::toString)).toList();
        Contexto contexto = new Contexto(usuario.getId(), usuario.getUsername(), usuario.getPerfil(),
                minimizador.nivelPadrao(usuario), estadual, gestao, principal, adicionais,
                acesso.podeConsultarContratos(usuario), acesso.podeConsultarEmpregados(usuario),
                acesso.podeConsultarFrequencia(usuario), acesso.podeUsarFrequenciaDiaria(usuario),
                acesso.podeAcessarRelatoriosGestao(usuario));
        return new Fotografia(contexto, Map.copyOf(revisoes));
    }

    private Unidade unidade(SuperintendenciaEstadual se, Map<String, Long> revisoes) {
        if (se == null) return null;
        revisoes.put("superintendencia:" + se.getId(), se.getVersion());
        return new Unidade(se.getId(), Boolean.TRUE.equals(se.getAtivo()));
    }

    private Gestao gestao(GestaoLocal gestao, Map<String, Long> revisoes) {
        if (gestao == null) return null;
        revisoes.put("gestao:" + gestao.getId(), gestao.getVersion());
        return new Gestao(gestao.getId(), Boolean.TRUE.equals(gestao.getAtivo()),
                unidade(gestao.getSuperintendencia(), revisoes));
    }

    private Par par(Lotacao lotacao, Jornada jornada, Map<String, Long> revisoes) {
        if (lotacao != null) revisoes.put("lotacao:" + lotacao.getId(), lotacao.getVersion());
        if (jornada != null) revisoes.put("jornada:" + jornada.getId(), jornada.getVersion());
        return new Par(lotacao == null ? null : lotacao.getId(),
                lotacao != null && Boolean.TRUE.equals(lotacao.getAtivo()),
                gestao(lotacao == null ? null : lotacao.getGestaoLocal(), revisoes),
                jornada == null ? null : jornada.getId(),
                jornada != null && Boolean.TRUE.equals(jornada.getAtivo()),
                gestao(jornada == null ? null : jornada.getGestaoLocal(), revisoes));
    }

    public record Unidade(Long id, boolean ativa) { }
    public record Gestao(Long id, boolean ativa, Unidade superintendencia) { }
    public record Par(Long lotacaoId, boolean lotacaoAtiva, Gestao gestaoLotacao,
                      Long jornadaId, boolean jornadaAtiva, Gestao gestaoJornada) { }
    public record Contexto(Long usuarioId, String username, PerfilAcesso perfil, NivelExposicaoDadosPessoais nivel,
                           Unidade superintendencia, Gestao gestao, Par principal, List<Par> adicionais,
                           boolean consultarContratos, boolean consultarEmpregados, boolean consultarFrequencia,
                           boolean operarFrequencia, boolean acessarRelatorios) { }
    public record Fotografia(Contexto contexto, Map<String, Long> revisoes) { }
}
