// Caminho: java/com/example/siggtrat/dto/RelatorioHorasDTO.java
package com.example.siggtrat.dto;

public record RelatorioHorasDTO(
        Long empregadoId,
        String nome,
        String cargo,
        String lotacao,
        String turno,
        int totalPresencas,
        int totalPresencasParciais,
        String totalHorasTrabalhadas, // Formato "HH:mm"
        Double totalHorasNoturnas
) {}