const CHAVES_SENSIVEIS = /(?:password|senha|authorization|cookie|token|secret|x-user-password|cpf|matricula)/i;
const MARCADORES_TECNICOS = /(?:\bException\b|\bjava\.|\borg\.springframework\.|\bat\s+[\w.$]+\([^)]*:\d+\))/i;

export const redigirTextoSensivel = (texto) => String(texto ?? '')
  .replace(/\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b/g, '***.***.***-**')
  .replace(/(Bearer\s+)[A-Za-z0-9._~+\/-]+=*/gi, '$1[REDACTED]')
  .replace(/((?:password|senha|token|secret)\s*[=:]\s*)[^\s,;]+/gi, '$1[REDACTED]');

export const mensagemEhTecnica = (mensagem) => MARCADORES_TECNICOS.test(String(mensagem || ''));

export const sanitizarEstrutura = (valor, profundidade = 0) => {
  if (profundidade > 5) return '[TRUNCATED]';
  if (valor == null || typeof valor === 'number' || typeof valor === 'boolean') return valor;
  if (typeof valor === 'string') return redigirTextoSensivel(valor);
  if (Array.isArray(valor)) return valor.map((item) => sanitizarEstrutura(item, profundidade + 1));
  if (typeof valor === 'object') {
    const saida = {};
    Object.entries(valor).forEach(([chave, item]) => {
      if (/^(?:stack|config|request|response|headers)$/i.test(chave)) return;
      if (/^(?:message|mensagem|detail|title)$/i.test(chave) && typeof item === 'string' && mensagemEhTecnica(item)) {
        saida[chave] = '[TECHNICAL_MESSAGE_REDACTED]';
        return;
      }
      saida[chave] = CHAVES_SENSIVEIS.test(chave) ? '[REDACTED]' : sanitizarEstrutura(item, profundidade + 1);
    });
    return saida;
  }
  return String(valor);
};
