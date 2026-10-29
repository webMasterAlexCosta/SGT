import test from "node:test";
import assert from "node:assert/strict";
import { carregarTodasAsPaginas } from "../src/services/rh/nucleoPaginacao.js";

const pagina = (content, totalPages, totalElements = content.length) => ({ content, totalPages, totalElements });

test("retorna página única sem chamadas adicionais", async () => {
  const chamadas = [];
  const resultado = await carregarTodasAsPaginas({
    buscarPagina: async (...args) => { chamadas.push(args); return pagina([1, 2], 1, 2); },
    tamanhoPagina: 1000,
    maximoItens: 10000,
  });
  assert.deepEqual(resultado, [1, 2]);
  assert.equal(chamadas.length, 1);
  assert.deepEqual(chamadas[0], [0, 1000, false]);
});

test("mantém a ordem mesmo quando páginas paralelas terminam fora de ordem", async () => {
  const atrasos = [0, 40, 5, 20];
  const resultado = await carregarTodasAsPaginas({
    buscarPagina: async (numero) => {
      await new Promise((resolve) => setTimeout(resolve, atrasos[numero]));
      return pagina([numero], 4, 4);
    },
    tamanhoPagina: 1,
    maximoItens: 10,
    concorrencia: 4,
  });
  assert.deepEqual(resultado, [0, 1, 2, 3]);
});

test("limita a concorrência por grupo", async () => {
  let ativas = 0;
  let pico = 0;
  const resultado = await carregarTodasAsPaginas({
    buscarPagina: async (numero) => {
      ativas += 1;
      pico = Math.max(pico, ativas);
      await new Promise((resolve) => setTimeout(resolve, 5));
      ativas -= 1;
      return pagina([numero], 10, 10);
    },
    tamanhoPagina: 1,
    maximoItens: 20,
    concorrencia: 3,
  });
  assert.equal(resultado.length, 10);
  assert.ok(pico <= 3, `Pico observado: ${pico}`);
});

test("páginas posteriores recebem skipCount verdadeiro", async () => {
  const flags = [];
  await carregarTodasAsPaginas({
    buscarPagina: async (numero, _size, skipCount) => {
      flags.push([numero, skipCount]);
      return pagina([numero], 3, 3);
    },
    tamanhoPagina: 1,
    maximoItens: 3,
  });
  assert.deepEqual(flags.sort((a, b) => a[0] - b[0]), [[0, false], [1, true], [2, true]]);
});

test("rejeita total acima do limite antes de buscar outras páginas", async () => {
  let chamadas = 0;
  await assert.rejects(
    carregarTodasAsPaginas({
      buscarPagina: async () => { chamadas += 1; return pagina([1], 101, 100_001); },
      tamanhoPagina: 1000,
      maximoItens: 100_000,
    }),
    /ultrapassou o limite seguro/,
  );
  assert.equal(chamadas, 1);
});

test("propaga falha de uma página e não devolve lista parcial", async () => {
  await assert.rejects(
    carregarTodasAsPaginas({
      buscarPagina: async (numero) => {
        if (numero === 2) throw new Error("falha simulada");
        return pagina([numero], 4, 4);
      },
      tamanhoPagina: 1,
      maximoItens: 10,
    }),
    /falha simulada/,
  );
});

test("conteúdo vazio encerra a leitura mesmo com totalPages inconsistente", async () => {
  let chamadas = 0;
  const resultado = await carregarTodasAsPaginas({
    buscarPagina: async () => { chamadas += 1; return pagina([], 8, 0); },
    tamanhoPagina: 100,
    maximoItens: 1000,
  });
  assert.deepEqual(resultado, []);
  assert.equal(chamadas, 1);
});

test("usa o menor tamanho entre tamanhoPagina e maximoItens", async () => {
  let tamanho;
  await carregarTodasAsPaginas({
    buscarPagina: async (_pagina, size) => { tamanho = size; return pagina([], 1, 0); },
    tamanhoPagina: 1000,
    maximoItens: 125,
  });
  assert.equal(tamanho, 125);
});

test("valida parâmetros numéricos", async () => {
  const fn = async () => pagina([], 1, 0);
  await assert.rejects(carregarTodasAsPaginas({ buscarPagina: fn, tamanhoPagina: 0, maximoItens: 1 }), /tamanhoPagina/);
  await assert.rejects(carregarTodasAsPaginas({ buscarPagina: fn, tamanhoPagina: 1, maximoItens: 0 }), /maximoItens/);
  await assert.rejects(carregarTodasAsPaginas({ buscarPagina: fn, tamanhoPagina: 1, maximoItens: 1, concorrencia: 0 }), /concorrencia/);
});

test("valida a função de busca", async () => {
  await assert.rejects(carregarTodasAsPaginas({ buscarPagina: null, tamanhoPagina: 1, maximoItens: 1 }), /buscarPagina/);
});
