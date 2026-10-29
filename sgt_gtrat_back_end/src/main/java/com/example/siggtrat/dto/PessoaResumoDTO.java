package com.example.siggtrat.dto;

import java.time.Instant;

/** Projeção leve para listagens paginadas de Pessoa. */
public record PessoaResumoDTO(Long id, String nome, String cpf,
                              Instant criadoEm, Instant atualizadoEm, long version) {}
