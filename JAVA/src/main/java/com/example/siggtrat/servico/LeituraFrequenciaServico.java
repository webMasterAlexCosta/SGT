package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Nível de leitura após a autorização do escopo histórico da frequência. */
@Service
public class LeituraFrequenciaServico {
    private final MinimizadorDadosPessoaisServico minimizador;
    private final PermissaoExcepcionalServico permissoes;
    private final ContratoRepositorio contratos;
    private final GestaoLocalRepositorio gestoes;

    public LeituraFrequenciaServico(MinimizadorDadosPessoaisServico minimizador,
                                    PermissaoExcepcionalServico permissoes,
                                    ContratoRepositorio contratos,
                                    GestaoLocalRepositorio gestoes) {
        this.minimizador = minimizador;
        this.permissoes = permissoes;
        this.contratos = contratos;
        this.gestoes = gestoes;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NivelExposicaoDadosPessoais nivel(UsuarioAplicacao usuario, Long contratoId,
                                            Long gestaoHistoricaId, String finalidade,
                                            String tipoRecurso, Long recursoId) {
        NivelExposicaoDadosPessoais padrao = minimizador.nivelPadrao(usuario);
        // A observação do dia é necessária ao trabalho do Operacional já
        // autorizado sobre essa alocação. Isso não amplia o cadastro pessoal.
        if (minimizador.completo(padrao) || padrao == NivelExposicaoDadosPessoais.OPERACIONAL_MINIMIZADO) {
            return NivelExposicaoDadosPessoais.COMPLETO;
        }
        Contrato contrato = contratoId == null ? null : contratos.findById(contratoId).orElse(null);
        GestaoLocal gestao = gestaoHistoricaId == null ? null : gestoes.findById(gestaoHistoricaId).orElse(null);
        TipoPermissaoExcepcional tipo = usuario != null && usuario.getPerfil() == PerfilAcesso.ADMINISTRADOR
                ? TipoPermissaoExcepcional.INTERVENCAO_NEGOCIAL_TECNICA
                : TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS;
        if (!permissoes.possuiPermissaoAtivaNoEscopoHistorico(usuario, tipo, contrato, gestao)) return padrao;
        // Relê a concessão ao registrar o uso. Revogação/expiração não pode ser
        // contornada por uma página que permaneceu no cache de consultas.
        permissoes.exigirERegistrarUsoNoEscopoHistorico(usuario, tipo, contrato, gestao,
                finalidade, tipoRecurso, recursoId);
        return NivelExposicaoDadosPessoais.COMPLETO;
    }
}
