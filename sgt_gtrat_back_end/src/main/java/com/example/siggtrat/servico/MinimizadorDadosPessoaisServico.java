package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import org.springframework.stereotype.Service;

/** Política única de exposição de dados pessoais aplicada a todos os DTOs. */
@Service
public class MinimizadorDadosPessoaisServico {

    public NivelExposicaoDadosPessoais nivelPadrao(UsuarioAplicacao usuario) {
        PerfilAcesso perfil = usuario == null ? null : usuario.getPerfil();
        if (perfil == PerfilAcesso.GESTOR_LOCAL) return NivelExposicaoDadosPessoais.COMPLETO;
        if (perfil == PerfilAcesso.OPERACIONAL) return NivelExposicaoDadosPessoais.OPERACIONAL_MINIMIZADO;
        if (perfil == PerfilAcesso.PRESIDENTE) return NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS;
        return NivelExposicaoDadosPessoais.AUDITORIA_MINIMIZADA;
    }

    public boolean completo(NivelExposicaoDadosPessoais nivel) {
        return nivel == NivelExposicaoDadosPessoais.COMPLETO;
    }

    public String nome(String valor, NivelExposicaoDadosPessoais nivel) {
        return nivel == NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS ? null : valor;
    }

    public String cpf(String valor, NivelExposicaoDadosPessoais nivel) {
        if (completo(nivel) || valor == null || valor.isBlank()) return valor;
        if (nivel == NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS) return null;
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() < 2) return "***";
        return "***.***.***-" + digitos.substring(digitos.length() - 2);
    }

    public String matricula(String valor, NivelExposicaoDadosPessoais nivel) {
        if (completo(nivel) || nivel == NivelExposicaoDadosPessoais.OPERACIONAL_MINIMIZADO
                || valor == null || valor.isBlank()) return valor;
        if (nivel == NivelExposicaoDadosPessoais.INSTITUCIONAL_SEM_DADOS_PESSOAIS) return null;
        String limpo = valor.replaceAll("\\s", "");
        int preservar = Math.min(2, limpo.length());
        return "*".repeat(Math.max(0, limpo.length() - preservar))
                + limpo.substring(limpo.length() - preservar);
    }

    public <T> T restrito(T valor, NivelExposicaoDadosPessoais nivel) {
        return completo(nivel) ? valor : null;
    }
}
