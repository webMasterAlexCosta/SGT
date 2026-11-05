package com.example.siggtrat.dto;
import java.time.*;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
public record RegistroProvaMotivoHistoricoRequisicao(
 Long contratoId, String descricaoComprovada, CategoriaTerminoVinculo categoriaComprovada, Long versaoMotivoEsperada, Long versaoContratoEsperada, LocalDate dataInicioEfetiva, LocalDate dataFimEfetiva,
 String referenciaSei, Long documentoComprobatorioId, String finalidadeAcessoDocumento,
 String motivo, String autorizacao, Long provaAnteriorId, String chaveIdempotencia,
 Boolean confirmarImpacto, String tokenPrevia) {}
