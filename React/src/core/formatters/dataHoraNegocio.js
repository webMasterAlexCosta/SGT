// LocalDateTime do Java é expresso no fuso definido em ConstantesDominio.
// Um valor com Z/offset já identifica um instante e conserva esse significado.
const FUSO_NEGOCIO = 'America/Sao_Paulo';
const formatador = new Intl.DateTimeFormat('en-CA', {
  timeZone: FUSO_NEGOCIO,
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
});

const partesNoFuso = (instante) => Object.fromEntries(
  formatador.formatToParts(new Date(instante))
    .filter(({ type }) => type !== 'literal').map(({ type, value }) => [type, value]),
);

const numeroCivil = ({ year, month, day, hour = 0, minute = 0, second = 0 }, milissegundo = 0) => {
  const data = new Date(0);
  data.setUTCFullYear(Number(year), Number(month) - 1, Number(day));
  data.setUTCHours(Number(hour), Number(minute), Number(second), milissegundo);
  return data.getTime();
};

export const obterDataNoFusoNegocio = (instante = Date.now()) => {
  const numero = new Date(instante).getTime();
  if (!Number.isFinite(numero)) return null;
  const { year, month, day } = partesNoFuso(numero);
  return `${year}-${month}-${day}`;
};

export const interpretarDataHoraNoFusoNegocio = (valor) => {
  if (valor instanceof Date) return valor.getTime();
  if (typeof valor !== 'string') return NaN;
  const texto = valor.trim();
  if (/(?:Z|[+-]\d{2}:\d{2})$/i.test(texto)) return Date.parse(texto);
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(texto);
  if (!partes) return NaN;
  const [, year, month, day, hour, minute, second = '00', fracao = ''] = partes;
  const milissegundo = Number(fracao.padEnd(3, '0').slice(0, 3));
  const civil = numeroCivil({ year, month, day, hour, minute, second }, milissegundo);
  const conferida = new Date(civil).toISOString().slice(0, 19);
  if (conferida !== `${year}-${month}-${day}T${hour}:${minute}:${second}`) return NaN;

  // Resolve o deslocamento com a base de fusos do navegador, inclusive datas
  // históricas com horário de verão. Não fixa o deslocamento em -03:00.
  let instante = civil;
  for (let tentativa = 0; tentativa < 4; tentativa += 1) {
    const diferenca = civil - numeroCivil(partesNoFuso(instante), milissegundo);
    if (diferenca === 0) return instante;
    instante += diferenca;
  }
  // Horário civil inexistente não recebe um instante inventado pelo cliente.
  return NaN;
};
