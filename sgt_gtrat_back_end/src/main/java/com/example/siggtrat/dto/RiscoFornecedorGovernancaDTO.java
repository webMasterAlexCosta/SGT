package com.example.siggtrat.dto;
import java.time.LocalDate;
public record RiscoFornecedorGovernancaDTO(Long id, Long fornecedorId, String fornecedor, LocalDate avaliadoEm, LocalDate proximaAvaliacao, Integer scoreTotal, String nivel, String evidencias, String avaliadoPor) {}
