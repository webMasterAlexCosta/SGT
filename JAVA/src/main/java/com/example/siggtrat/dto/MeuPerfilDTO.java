package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import java.time.LocalDateTime;
import java.util.List;

public record MeuPerfilDTO(
        Long id,
        String username,
        String email,
        PerfilAcesso perfil,
        String autoridade,
        String matricula,
        String turno,
        Long jornadaId,
        ResumoUsuarioAplicacaoDTO.Referencia jornada,
        ResumoUsuarioAplicacaoDTO.Referencia lotacao,
        String lotacaoNome,
        Long lotacaoId,
        List<ResumoUsuarioAplicacaoDTO.VinculoGestaoReferencia> vinculosGestao,
        ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia superintendencia,
        ResumoUsuarioAplicacaoDTO.OrganizacaoReferencia gestaoLocal,
        boolean forceChangePassword,
        boolean isRetroactiveUnlocked,
        LocalDateTime retroactiveExpireAt,
        String fotoUrl,
        LocalDateTime createdAt,
        boolean twoFactorEnabled
) {}
