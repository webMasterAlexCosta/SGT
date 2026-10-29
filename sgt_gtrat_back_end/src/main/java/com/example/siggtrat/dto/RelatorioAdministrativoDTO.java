package com.example.siggtrat.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RelatorioAdministrativoDTO(
        MetadadosEmissaoRelatorioDTO metadados,
        JsonNode conteudo
) { }
