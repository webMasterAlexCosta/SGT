const normalizarParteChave = (valor: unknown) =>
  String(valor ?? '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '_')
    .replace(/^_+|_+$/g, '');

export const criarChaveAtividadeSessao = (appName: (string), usuario: (import("../../contracts/auth").AuthenticatedUser | null)) => {
  const app = normalizarParteChave(appName) || 'app';
  const identificador =
    usuario?.id ??
    usuario?.matricula ??
    usuario?.username ??
    usuario?.user ??
    'sessao';

  return `${app}_session_last_activity_${normalizarParteChave(identificador) || 'sessao'}`;
};

export const calcularRestanteInatividade = ({
  ultimaAtividadeEm,
  agora = Date.now(),
  timeoutMs,
}: { ultimaAtividadeEm?: number | string | null; agora?: number; timeoutMs: number }) => {
  const limite = Number(timeoutMs);
  const ultima = Number(ultimaAtividadeEm);
  const instanteAtual = Number(agora);

  if (!Number.isFinite(limite) || limite <= 0) return 0;
  if (!Number.isFinite(ultima) || ultima <= 0) return limite;
  if (!Number.isFinite(instanteAtual)) return limite;

  const decorrido = Math.max(0, instanteAtual - ultima);
  return Math.max(0, limite - decorrido);
};

export const estaInativa = (opcoes: Parameters<typeof calcularRestanteInatividade>[0]) =>
  calcularRestanteInatividade(opcoes) <= 0;
