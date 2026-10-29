package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import jakarta.validation.constraints.Email;
import java.util.List;

public record AtualizacaoUsuarioAplicacaoDTO(
        @Email(message = "Informe um e-mail válido.") String email,
        Boolean active,
        String matricula,
        PerfilAcesso perfil,
        String password,
        Long lotacaoId,
        Long jornadaId,
        List<VinculoGestaoDTO> vinculosGestao,
        Long superintendenciaId,
        Long gestaoLocalId
) {
    public record VinculoGestaoDTO(Long lotacaoId, Long jornadaId) {}
}
