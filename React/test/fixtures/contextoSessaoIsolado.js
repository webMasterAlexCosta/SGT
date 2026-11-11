import { invalidarGeracaoSessao } from '../../src/core/security/geracaoSessao.ts';
import { confirmarLoginExplicito, obterIntencaoSaida } from '../../src/core/security/saidaSessao.ts';

// Somente antes de cada caso independente. Não usar entre as ações de um caso:
// remount, logout, storage/focus e corridas devem preservar seus próprios sinais.
// Limpar Storage sem redefinir esta fronteira representaria um clear externo,
// e não um ambiente novo para o próximo teste.
export const iniciarContextoSessaoIsolado = () => {
  confirmarLoginExplicito(obterIntencaoSaida());
  document.cookie = 'sgt_revisao_compartilhada_sessao=; Max-Age=0; Path=/';
  invalidarGeracaoSessao();
};
