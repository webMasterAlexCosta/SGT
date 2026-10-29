import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("exportações grandes persistem em temporário do servidor e usam lotes de 5000", async () => {
  const [exportacao, armazenamento, frequencia, terceirizados, writer] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/ArmazenamentoTemporarioRelatorioServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioFrequenciaConsolidadoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioTerceirizadosServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/utilitario/XlsxStreamingWriter.java"),
  ]);

  assert.match(frequencia, /TAMANHO_LOTE_EXPORTACAO\s*=\s*5000/);
  assert.match(terceirizados, /TAMANHO_LOTE_EXPORTACAO\s*=\s*5000/);
  assert.match(exportacao, /gerarArquivoConcluido/);
  assert.match(exportacao, /persistirLoteXlsx/);
  assert.match(exportacao, /persistirLotePdf/);
  assert.match(armazenamento, /gerarArquivoConcluido/);
  assert.match(armazenamento, /transferirEExcluir/);
  assert.match(armazenamento, /Files\.createTempFile/);
  assert.match(armazenamento, /Files\.deleteIfExists/);
  assert.match(armazenamento, /@Scheduled/);
  assert.match(armazenamento, /java\.io\.tmpdir/);
  assert.match(writer, /public void flush\(\) throws IOException/);
});
