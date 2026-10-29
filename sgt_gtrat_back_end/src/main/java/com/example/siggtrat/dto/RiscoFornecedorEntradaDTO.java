package com.example.siggtrat.dto;
import java.time.LocalDate;
public record RiscoFornecedorEntradaDTO(Long fornecedorId, Integer scoreTrabalhista, Integer scoreFinanceiro, Integer scoreCompliance, Integer scoreSeguranca, LocalDate proximaAvaliacao, String evidencias) {}
