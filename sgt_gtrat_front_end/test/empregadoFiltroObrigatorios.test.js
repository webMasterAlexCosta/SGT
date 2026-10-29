import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const caminhoFormulario = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FormularioEmpregado/index.jsx",
  import.meta.url,
);
const caminhoSecoes = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FormularioEmpregado/components/SecoesFormulario.jsx",
  import.meta.url,
);

test("cadastro oferece filtro de obrigatórios sem retirar as abas existentes", async () => {
  const [formulario, secoes] = await Promise.all([
    readFile(caminhoFormulario, "utf8"),
    readFile(caminhoSecoes, "utf8"),
  ]);

  assert.match(formulario, /id:\s*"essenciais",\s*label:\s*"Campos Obrigatórios"/);
  for (const aba of ["pessoais", "trabalhistas", "contatos", "beneficios", "bancarias", "contratos"]) {
    assert.match(formulario, new RegExp(`id:\\s*"${aba}"`), `A aba ${aba} deve ser preservada.`);
  }

  const secaoEssencial = secoes.match(
    /export const SecaoCamposEssenciais[\s\S]*?SecaoCamposEssenciais\.displayName/,
  )?.[0];
  assert.ok(secaoEssencial, "A seção de campos obrigatórios deve existir.");

  for (const campo of ["nome", "cpf", "dataAdmissao", "lotacaoId", "cargoId", "jornadaId"]) {
    assert.match(secaoEssencial, new RegExp(`name="${campo}"`), `O campo obrigatório ${campo} deve aparecer no filtro.`);
  }
  assert.doesNotMatch(secaoEssencial, /name="matricula"/, "A matrícula opcional não deve aparecer no filtro de obrigatórios.");
  assert.match(
    formulario,
    /case "essenciais"[\s\S]*?<SecaoMatrizDeContratos/,
    "O vínculo contratual deve estar disponível no filtro porque o backend exige contrato para colaborador ativo.",
  );
});
