interface LogoutOperation { intencao: string; geracao: number; promessa: Promise<boolean> | null }
interface RevokeOptions { intencao: string; geracao: number; executar: () => unknown | Promise<unknown> }
// Intenção local de saída, independente do cookie HttpOnly e da confirmação
// remota. Não contém identidade, credenciais nem tokens de autenticação.
const CHAVE_SAIDA = 'sgt_intencao_saida_sessao';
const CHAVE_LOGIN_EXPLICITO_LOCAL = 'sgt_exigir_login_explicito_nesta_aba';
const ARMAZENAMENTO_INDISPONIVEL = 'armazenamento-indisponivel';
let sequenciaSaida = 0;
let intencaoSemPersistencia: string | null = null;
let revogacaoEmAndamento: LogoutOperation | null = null;
let intencaoRevogada: string | null = null;
let loginExplicitoLocalNecessario = false;

// Observar outra sessão não é sair dela. Este bloqueio pertence somente à
// aba e não entra na coordenação da revogação nem no localStorage compartilhado.
export const exigirLoginExplicitoLocal = () => {
  loginExplicitoLocalNecessario = true;
  try { sessionStorage.setItem(CHAVE_LOGIN_EXPLICITO_LOCAL, '1'); } catch {}
};

export const restauracaoLocalBloqueada = () => {
  if (loginExplicitoLocalNecessario) return true;
  try { return sessionStorage.getItem(CHAVE_LOGIN_EXPLICITO_LOCAL) === '1'; }
  catch { return true; }
};

const usarArmazenamentos = (operacao: (storage: Storage) => void) => {
  let acessiveis = 0;
  for (const nome of ['localStorage', 'sessionStorage'] as const) {
    try {
      const armazenamento = globalThis[nome];
      if (!armazenamento) continue;
      operacao(armazenamento);
      acessiveis += 1;
    } catch {
      // Uma política do browser pode negar leitura, gravação ou remoção.
    }
  }
  return acessiveis;
};

export const obterIntencaoSaida = () => {
  let persistida: string | null = null;
  const leituras = usarArmazenamentos(armazenamento => {
    const valor = armazenamento.getItem(CHAVE_SAIDA);
    if (valor && !persistida) persistida = valor;
  });
  if (persistida) return persistida;
  if (intencaoSemPersistencia) return intencaoSemPersistencia;
  // Leitura pode funcionar mesmo com gravação negada (ex.: quota/política).
  // Sem poder persistir uma saída, ausência de marcador não autoriza uma
  // instância nova a restaurar silenciosamente o cookie. A verificação usa
  // só um valor temporário sem identidade, removido imediatamente.
  const chaveVerificacao = `${CHAVE_SAIDA}_suporte`;
  const utilizaveis = usarArmazenamentos(armazenamento => {
    armazenamento.setItem(chaveVerificacao, '1');
    if (armazenamento.getItem(chaveVerificacao) !== '1') throw new Error('Armazenamento indisponível.');
    armazenamento.removeItem(chaveVerificacao);
  });
  return leituras && utilizaveis ? null : ARMAZENAMENTO_INDISPONIVEL;
};

export const registrarIntencaoSaida = () => {
  const intencao = `${Date.now()}-${++sequenciaSaida}`;
  const gravacoes = usarArmazenamentos(armazenamento => armazenamento.setItem(CHAVE_SAIDA, intencao));
  intencaoSemPersistencia = gravacoes ? null : intencao;
  return intencao;
};

// Só chamar depois de autenticação, perfil e sessão validados. Um logout mais
// novo, inclusive observado no armazenamento, não pode ser limpo por esse login.
export const confirmarLoginExplicito = (intencaoEsperada: string | null) => {
  if (obterIntencaoSaida() !== intencaoEsperada) return false;
  usarArmazenamentos(armazenamento => armazenamento.removeItem(CHAVE_SAIDA));
  intencaoSemPersistencia = null;
  intencaoRevogada = null;
  loginExplicitoLocalNecessario = false;
  try { sessionStorage.removeItem(CHAVE_LOGIN_EXPLICITO_LOCAL); } catch {}
  return true;
};

export const obterRevogacaoEmAndamento = () => revogacaoEmAndamento;
export const revogacaoFoiConfirmada = (intencao: string | null) => Boolean(intencao) && intencaoRevogada === intencao;

// A promessa pertence à saída e sobrevive à desmontagem do provider. Nenhum
// login pode avançar a geração/enviar credenciais antes de ela se resolver.
export const iniciarRevogacaoCoordenada = ({ intencao, geracao, executar }: RevokeOptions) => {
  if (revogacaoEmAndamento) return revogacaoEmAndamento;
  const operacao: LogoutOperation = { intencao, geracao, promessa: null };
  revogacaoEmAndamento = operacao;
  let requisicao: unknown;
  try { requisicao = executar(); } catch { requisicao = false; }
  operacao.promessa = Promise.resolve(requisicao)
    .then(confirmada => {
      if (confirmada === true) intencaoRevogada = intencao;
      return confirmada === true;
    }, () => false)
    .finally(() => {
      if (revogacaoEmAndamento === operacao) revogacaoEmAndamento = null;
    });
  return operacao;
};
