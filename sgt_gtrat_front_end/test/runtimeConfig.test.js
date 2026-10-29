import test from "node:test";
import assert from "node:assert/strict";
import {
  combinarConfiguracaoEfetiva,
  validarRuntimeConfig,
} from "../src/core/runtimeConfig.js";

const runtimeValido = () => ({
  EM_MANUTENCAO: true,
  PREVISAO_RETORNO: "18:00",
  CLIENT_IP: "127.0.0.1",
  HORAS_DIA_TRABALHO: "8",
  HORA_INICIO_NOTURNO: "22:00",
  MINUTOS_HORA_NOTURNA: "52.5",
});

test("aceita e normaliza configuração JSON válida", () => {
  const config = validarRuntimeConfig(runtimeValido());
  assert.equal(config.EM_MANUTENCAO, true);
  assert.equal(config.HORAS_DIA_TRABALHO, 8);
  assert.equal(config.MINUTOS_HORA_NOTURNA, 52.5);
  assert.equal(Object.isFrozen(config), true);
});

test("rejeita configuração que não seja objeto", () => {
  assert.throws(() => validarRuntimeConfig("window.APP_CONFIG = {}"), TypeError);
});

test("rejeita horário noturno inválido", () => {
  assert.throws(
    () => validarRuntimeConfig({ ...runtimeValido(), HORA_INICIO_NOTURNO: "99:99" }),
    /HORA_INICIO_NOTURNO/,
  );
});

test("rejeita números ausentes, NaN, zero e fora do intervalo em vez de mascará-los", () => {
  assert.throws(
    () => validarRuntimeConfig({ ...runtimeValido(), HORAS_DIA_TRABALHO: 0 }),
    /HORAS_DIA_TRABALHO/,
  );
  assert.throws(
    () => validarRuntimeConfig({ ...runtimeValido(), MINUTOS_HORA_NOTURNA: 90 }),
    /MINUTOS_HORA_NOTURNA/,
  );
  assert.throws(
    () => validarRuntimeConfig({ ...runtimeValido(), HORAS_DIA_TRABALHO: "NaN" }),
    /HORAS_DIA_TRABALHO/,
  );
});

test("rejeita manutenção que não seja booleano real", () => {
  assert.throws(
    () => validarRuntimeConfig({ ...runtimeValido(), EM_MANUTENCAO: "false" }),
    /EM_MANUTENCAO/,
  );
});

test("/config.json prevalece nos campos públicos dinâmicos sem substituir infraestrutura", () => {
  const efetiva = combinarConfiguracaoEfetiva(
    { API_URL: "/api", EM_MANUTENCAO: false, PREVISAO_RETORNO: "local" },
    runtimeValido(),
  );
  assert.equal(efetiva.API_URL, "/api");
  assert.equal(efetiva.EM_MANUTENCAO, true);
  assert.equal(efetiva.PREVISAO_RETORNO, "18:00");
  assert.equal(efetiva.HORAS_DIA_TRABALHO, 8);
  assert.equal(Object.isFrozen(efetiva), true);
});
