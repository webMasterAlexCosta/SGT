package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ResumoUsuarioAplicacaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoGestaoUsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import com.example.siggtrat.utilitario.RelogioSistema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mapeamento de entidade de usuário para contrato HTTP sem acoplar DTO a JPA. */
final class MapeadorUsuarioAplicacaoDTO {
    private MapeadorUsuarioAplicacaoDTO() {}

    static ResumoUsuarioAplicacaoDTO deEntidade(UsuarioAplicacao user) {
        if (user == null) return null;

        List<ResumoUsuarioAplicacaoDTO.VinculoGestaoReferencia> vinculos = new ArrayList<>();
        if (user.getVinculosGestao() != null) {
            for (VinculoGestaoUsuarioAplicacao vinculo : user.getVinculosGestao()) {
                if (vinculo == null) continue;
                vinculos.add(new ResumoUsuarioAplicacaoDTO.VinculoGestaoReferencia(
                        vinculo.getId(),
                        referencia(vinculo.getLotacao()),
                        referencia(vinculo.getJornada())
                ));
            }
        }

        ResumoUsuarioAplicacaoDTO.Referencia lotacaoPrincipal = referencia(user.getLotacao());
        ResumoUsuarioAplicacaoDTO.Referencia jornadaPrincipal = referencia(user.getJornada());
        if (!vinculos.isEmpty() && (lotacaoPrincipal == null || jornadaPrincipal == null)) {
            ResumoUsuarioAplicacaoDTO.VinculoGestaoReferencia primeiro = vinculos.get(0);
            if (lotacaoPrincipal == null) lotacaoPrincipal = primeiro.lotacao();
            if (jornadaPrincipal == null) jornadaPrincipal = primeiro.jornada();
        }

        LocalDateTime agora = RelogioSistema.agora();
        boolean retroactiveUnlocked = user.getRetroactiveUnlockExpireAt() != null
                && agora.isBefore(user.getRetroactiveUnlockExpireAt());
        boolean locked = user.getLockoutEnd() != null && agora.isBefore(user.getLockoutEnd());

        SuperintendenciaEstadual se = user.getSuperintendencia();
        if (se == null && user.getGestaoLocal() != null) se = user.getGestaoLocal().getSuperintendencia();

        return new ResumoUsuarioAplicacaoDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                UtilitarioMatricula.formatarParaExibicao(user.getMatricula()),
                user.getPerfil(),
                user.getPerfil() != null ? user.getPerfil().autoridade() : null,
                user.getActive(),
                Boolean.TRUE.equals(user.getForceChangePassword()),
                locked,
                user.getLockoutEnd() != null ? user.getLockoutEnd().toString() : null,
                lotacaoPrincipal,
                jornadaPrincipal,
                vinculos,
                referencia(se),
                referencia(user.getGestaoLocal()),
                retroactiveUnlocked,
                user.getRetroactiveUnlockExpireAt() != null ? user.getRetroactiveUnlockExpireAt().toString() : null,
                user.getFotoUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }

    private static ResumoUsuarioAplicacaoDTO.Referencia referencia(Lotacao lotacao) {
        return lotacao == null ? null : new ResumoUsuarioAplicacaoDTO.Referencia(lotacao.getId(), lotacao.getNome());
    }

    private static ResumoUsuarioAplicacaoDTO.Referencia referencia(Jornada jornada) {
        return jornada == null ? null : new ResumoUsuarioAplicacaoDTO.Referencia(jornada.getId(), jornada.getDescricao());
    }

    private static ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia referencia(SuperintendenciaEstadual se) {
        return se == null ? null : new ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia(se.getId(), se.getNome(), null, se.getUf());
    }

    private static ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia referencia(GestaoLocal gestaoLocal) {
        if (gestaoLocal == null) return null;
        String uf = gestaoLocal.getSuperintendencia() != null ? gestaoLocal.getSuperintendencia().getUf() : null;
        return new ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia(gestaoLocal.getId(), gestaoLocal.getNome(), gestaoLocal.getCodigo(), uf);
    }
}
