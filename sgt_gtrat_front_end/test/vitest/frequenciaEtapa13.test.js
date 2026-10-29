import { describe, expect, it, vi } from "vitest";
import {
  TAMANHO_MAXIMO_LOTE_FREQUENCIA,
  dividirEmLotesFrequencia,
  executarLotesFrequenciaSequencial,
  frequenciaBloqueadaParaOperacional,
  montarPayloadFrequenciaCanonica,
  possuiIdentidadeCanonicaFrequencia,
} from "../../src/services/rh/ServicoDominioFrequencia.js";

const linhaBase = {
  empregadoId: 999,
  vinculoContratualId: 42,
  frequenciaId: 7,
  status: " presenca_parcial ",
  obs: "  saída autorizada  ",
  horasTrabalhadas: "4,5",
  horarioSaida: "12:30",
  horasNoturnas: 1.25,
};

describe("Etapa 13 - contrato canônico de frequência", () => {
  it("monta escrita exclusivamente por vinculoContratualId", () => {
    const payload = montarPayloadFrequenciaCanonica({ linha: linhaBase, dataReferencia: "2026-09-10" });
    expect(payload.vinculoContratualId).toBe(42);
    expect(payload.dataReferencia).toBe("2026-09-10");
    expect(payload.status).toBe("PRESENCA_PARCIAL");
    expect(payload.horasTrabalhadas).toBe("4.5");
    expect(payload).not.toHaveProperty("empregado");
    expect(payload).not.toHaveProperty("empregadoId");
  });

  it("recusa fallback para empregado.id quando o vínculo não existe", () => {
    expect(possuiIdentidadeCanonicaFrequencia({ empregadoId: 10 })).toBe(false);
    expect(() => montarPayloadFrequenciaCanonica({
      linha: { empregadoId: 10, status: "FALTA" },
      dataReferencia: "2026-09-10",
    })).toThrow(/vinculoContratualId/);
  });

  it("Gestor Local envia justificativa e chave idempotente exigidas pelo Java", () => {
    const payload = montarPayloadFrequenciaCanonica({
      linha: linhaBase,
      dataReferencia: "2026-09-10",
      gestorLocal: true,
      justificativaRetificacao: "Correção validada pela gestão local",
      chaveIdempotencia: "freq-12345678",
    });
    expect(payload.justificativaRetificacao).toBe("Correção validada pela gestão local");
    expect(payload.chaveIdempotencia).toBe("freq-12345678");
    expect(() => montarPayloadFrequenciaCanonica({
      linha: linhaBase,
      dataReferencia: "2026-09-10",
      gestorLocal: true,
      justificativaRetificacao: "abc",
      chaveIdempotencia: "freq-12345678",
    })).toThrow(/5 caracteres/);
  });

  it("preserva a data de início também para jornada noturna", () => {
    const payload = montarPayloadFrequenciaCanonica({
      linha: { ...linhaBase, entrada1: "22:00", saida1: "06:00" },
      dataReferencia: "2026-09-10",
    });
    expect(payload.dataReferencia).toBe("2026-09-10");
    expect(payload.entrada1).toBe("22:00");
    expect(payload.saida1).toBe("06:00");
  });

  it("respeita o limite de 1000 itens por lote", () => {
    const itens = Array.from({ length: 2501 }, (_, i) => ({ id: i + 1 }));
    const lotes = dividirEmLotesFrequencia(itens);
    expect(TAMANHO_MAXIMO_LOTE_FREQUENCIA).toBe(1000);
    expect(lotes.map((lote) => lote.length)).toEqual([1000, 1000, 501]);
  });

  it("expõe progresso parcial e não repete automaticamente lote que falhou", async () => {
    const payloads = Array.from({ length: 2001 }, (_, i) => ({ vinculoContratualId: i + 1 }));
    const enviarLote = vi.fn()
      .mockResolvedValueOnce(Array.from({ length: 1000 }, (_, i) => ({ id: i + 1 })))
      .mockRejectedValueOnce(new Error("falha controlada"));
    await expect(executarLotesFrequenciaSequencial({
      payloads,
      enviarIndividual: vi.fn(),
      enviarLote,
    })).rejects.toMatchObject({
      progressoFrequencia: {
        totalSalvo: 1000,
        lotesConcluidos: 1,
        loteFalhou: 2,
        totalLotes: 3,
      },
    });
    expect(enviarLote).toHaveBeenCalledTimes(2);
  });

  it("usa somente estados oficiais para pré-bloqueio operacional", () => {
    expect(frequenciaBloqueadaParaOperacional({ jaSalvo: true, estadoNegocial: "FECHADA" })).toBe(true);
    expect(frequenciaBloqueadaParaOperacional({ jaSalvo: true, estadoAuditoria: "SUJEITA_REVISAO" })).toBe(true);
    expect(frequenciaBloqueadaParaOperacional({ jaSalvo: true, validadeOperacional: "INVALIDA" })).toBe(true);
    expect(frequenciaBloqueadaParaOperacional({ jaSalvo: true, estadoNegocial: "ABERTA", validadeOperacional: "VALIDA", estadoAuditoria: "ORIGINAL" })).toBe(false);
  });
});
