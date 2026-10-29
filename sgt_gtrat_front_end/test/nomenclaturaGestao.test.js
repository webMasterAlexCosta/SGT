import test from "node:test";
import assert from "node:assert/strict";
import {
  formatarGestorEstadual,
  formatarGestaoLocal,
} from "../src/core/formatters/nomenclaturaGestao.js";
import { AUTORIDADES, PERFIS, rotuloPerfil } from "../src/core/security/roles.js";

test("converte a nomenclatura institucional para os gestores", () => {
  assert.equal(formatarGestorEstadual("SE/RJ"), "Gestor estadual/RJ");
  assert.equal(formatarGestorEstadual({ nome: "SE/SP", uf: "SP" }), "Gestor estadual/SP");
  assert.equal(formatarGestaoLocal("GTRAT Niterói"), "GTRAT Niterói");
});

test("usa os perfis e autoridades canônicos do Java e mantém rótulos de interface", () => {
  assert.equal(AUTORIDADES.PRESIDENTE, "PERFIL_PRESIDENTE");
  assert.equal(AUTORIDADES.GESTOR_NACIONAL, "PERFIL_GESTOR_NACIONAL");
  assert.equal(AUTORIDADES.GESTOR_ESTADUAL, "PERFIL_GESTOR_ESTADUAL");
  assert.equal(AUTORIDADES.GESTOR_LOCAL, "PERFIL_GESTOR_LOCAL");
  assert.equal(rotuloPerfil({ perfil: PERFIS.PRESIDENTE, autoridade: AUTORIDADES.PRESIDENTE }), "Presidente");
  assert.equal(rotuloPerfil({ perfil: PERFIS.GESTOR_NACIONAL, autoridade: AUTORIDADES.GESTOR_NACIONAL }), "Gestor nacional");
  assert.equal(
    rotuloPerfil({ perfil: PERFIS.GESTOR_ESTADUAL, autoridade: AUTORIDADES.GESTOR_ESTADUAL, superintendenciaNome: "SE/RJ" }),
    "Gestor estadual/RJ",
  );
  assert.equal(rotuloPerfil({ perfil: PERFIS.GESTOR_LOCAL, autoridade: AUTORIDADES.GESTOR_LOCAL }), "Gestor local");
  assert.equal(
    rotuloPerfil({ perfil: PERFIS.GESTOR_LOCAL, autoridade: AUTORIDADES.GESTOR_LOCAL, gestaoLocal: { nome: "GEDIS/RJ" } }),
    "GEDIS/RJ",
  );
});
