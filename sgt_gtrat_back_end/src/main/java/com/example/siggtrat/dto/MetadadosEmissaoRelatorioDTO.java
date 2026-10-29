package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.relatorio.*;

import java.time.Instant;
import java.time.LocalDate;

public record MetadadosEmissaoRelatorioDTO(
        Long id,
        TipoRelatorioAdministrativo tipo,
        FormatoEmissaoRelatorio formato,
        int versao,
        NaturezaEmissaoRelatorio natureza,
        EstadoEmissaoRelatorio estado,
        String situacao,
        LocalDate inicio,
        LocalDate fim,
        Instant geradoEm,
        Long usuarioId,
        String usuario,
        String perfil,
        boolean sujeitoARevisao,
        long quantidadePendencias,
        String aviso,
        String hashConteudo,
        long tamanhoConteudoBytes,
        Long emissaoAnteriorId,
        Instant superadaEm,
        CausaSuperacaoRelatorio causaSuperacao,
        String motivoSuperacao,
        Instant fonteConhecidaAte
) { }
