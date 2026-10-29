import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("ranking do painel tem cancelamento independente e sempre libera o estado de carregamento", async () => {
  const painel = await ler("src/modules/painel/pages/index.js");

  assert.match(painel, /const controladorRankingRef = useRef\(null\)/);
  assert.match(painel, /const rankingCarregandoRef = useRef\(false\)/);
  assert.match(painel, /\{ signal: controlador\.signal \}/);
  assert.match(painel, /controladorRankingRef\.current === controlador/);
  assert.match(painel, /rankingCarregandoRef\.current = false;\s*setRankingCarregando\(false\)/);
  assert.doesNotMatch(
    painel,
    /const controlador = controladorRef\.current;[\s\S]{0,500}buscarRankingLotacoes/,
    "ranking não pode reutilizar o AbortController do cálculo principal",
  );
});

test("toda troca de rota reposiciona o container principal no topo", async () => {
  const aplicacao = await ler("src/Aplicacao.jsx");

  assert.match(
    aplicacao,
    /referenciaPrincipal\.current\?\.scrollTo\(\{ top: 0, left: 0, behavior: "auto" \}\);\s*setMostrarBotaoTopo\(false\);\s*\}, \[localizacao\.pathname\]\)/,
  );
});

test("tour da frequência acompanha o ancestral rolável em vez de depender do scroll da janela", async () => {
  const tour = await ler("src/modules/rh/pages/PaginaFrequencia/components/TourGuiado.jsx");

  assert.match(tour, /elementoAlvo\.scrollIntoView\(/);
  assert.match(tour, /document\.addEventListener\("scroll", aoRolarOuRedimensionar, true\)/);
  assert.doesNotMatch(tour, /window\.scrollTo\(/);
  assert.doesNotMatch(tour, /retangulo\.top \+ window\.scrollY/);
});
