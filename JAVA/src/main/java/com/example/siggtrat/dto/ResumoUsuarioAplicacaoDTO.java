package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import java.util.List;

public record ResumoUsuarioAplicacaoDTO(
        Long id,
        String username,
        String email,
        String matricula,
        PerfilAcesso perfil,
        String autoridade,
        Boolean active,
        Boolean forceChangePassword,
        Boolean locked,
        String lockoutEnd,
        Referencia lotacao,
        Referencia jornada,
        List<VinculoGestaoReferencia> vinculosGestao,
        OrganizacaoReferencia superintendencia,
        OrganizacaoReferencia gestaoLocal,
        Boolean retroactiveUnlocked,
        String retroactiveUnlockExpireAt,
        String fotoUrl,
        String createdAt
) {
    public record Referencia(Long id, String label) {}
    public record OrganizacaoReferencia(Long id, String label, String codigo, String uf) {}
    public record VinculoGestaoReferencia(Long id, Referencia lotacao, Referencia jornada) {}
}
