package com.example.siggtrat.dto;
import java.time.LocalDate;
public record FornecedorGovernancaDTO(Long id, String razaoSocial, String cnpj, String email, Boolean ativo, Integer riscoScore, String riscoNivel, LocalDate proximaAvaliacao, String monitoramentoStatus) {}
