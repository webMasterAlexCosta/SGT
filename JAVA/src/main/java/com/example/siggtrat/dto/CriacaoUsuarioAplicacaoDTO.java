package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CriacaoUsuarioAplicacaoDTO(
        @NotBlank(message = "Informe o usuário.") String username,
        @Email(message = "Informe um e-mail válido.") @NotBlank(message = "Informe o e-mail.") String email,
        @NotBlank(message = "Informe a matrícula.")
        @Pattern(regexp = "(?:\\D*\\d){8}\\D*", message = "A matrícula deve possuir 8 dígitos.")
        String matricula,
        @NotBlank(message = "Informe a senha inicial.") String password,
        @NotNull(message = "Informe o perfil.") PerfilAcesso perfil,
        Long superintendenciaId,
        Long gestaoLocalId,
        Long lotacaoId,
        Long jornadaId,
        List<AtualizacaoUsuarioAplicacaoDTO.VinculoGestaoDTO> vinculosGestao,
        Boolean active
) {}
