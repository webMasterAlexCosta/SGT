import test from "node:test";
import assert from "node:assert/strict";
import { extrairDigitosMatricula, formatarMatricula, matriculaEstaCompleta } from "../src/core/formatters/matricula.js";

test("formata matrícula canônica", () => {
  assert.equal(formatarMatricula("89614745"), "8.961.474-5");
  assert.equal(formatarMatricula("8.961.474-5"), "8.961.474-5");
});

test("normaliza e limita matrícula a oito dígitos", () => {
  assert.equal(extrairDigitosMatricula("8.961.474-5abc99"), "89614745");
  assert.equal(matriculaEstaCompleta("8.961.474-5"), true);
  assert.equal(matriculaEstaCompleta("8961474"), false);
});
