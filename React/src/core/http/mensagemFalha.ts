const objeto = (valor: unknown): Record<string, unknown> | null => valor !== null && typeof valor === 'object' ? valor as Record<string, unknown> : null;
export function mensagemFalha(falha: unknown, padrao: string): string {
  const erro = objeto(falha);
  const resposta = objeto(erro?.response);
  const dadosResposta = objeto(resposta?.data);
  const dados = objeto(erro?.data);
  for (const candidato of [dadosResposta?.message, dadosResposta?.mensagem, dados?.message, dados?.mensagem, erro?.message]) {
    if (typeof candidato === 'string' && candidato) return candidato;
  }
  return padrao;
}
