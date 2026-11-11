export interface WorkDay {
  diaSemana?: import("../../contracts/java").JornadaDiaSemanaDTO["diaSemana"];
  diaTrabalho?: boolean | null;
  entrada1?: string | null; saida1?: string | null; entrada2?: string | null; saida2?: string | null;
}
export interface ShiftDraft { descricao?: string; gestaoLocalId?: string | number; ativo?: boolean; dias?: WorkDay[] }
export interface AdditionalRuleDraft { tipo?: string; inicio?: string; fim?: string; percentual?: string | number; ordem?: string | number; ativo?: boolean }
export const DIAS_SEMANA: { id: NonNullable<import("../../contracts/java").JornadaDiaSemanaDTO["diaSemana"]>; nome: string; curto: string; fimSemana: boolean }[] = [
  { id: "SUNDAY", nome: "Domingo", curto: "DOM", fimSemana: true },
  { id: "MONDAY", nome: "Segunda-feira", curto: "SEG", fimSemana: false },
  { id: "TUESDAY", nome: "Terça-feira", curto: "TER", fimSemana: false },
  { id: "WEDNESDAY", nome: "Quarta-feira", curto: "QUA", fimSemana: false },
  { id: "THURSDAY", nome: "Quinta-feira", curto: "QUI", fimSemana: false },
  { id: "FRIDAY", nome: "Sexta-feira", curto: "SEX", fimSemana: false },
  { id: "SATURDAY", nome: "Sábado", curto: "SÁB", fimSemana: true },
];

export const ORDEM_FORMULARIO: NonNullable<import("../../contracts/java").JornadaDiaSemanaDTO["diaSemana"]>[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
export const ORDEM_LISTAGEM: NonNullable<import("../../contracts/java").JornadaDiaSemanaDTO["diaSemana"]>[] = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];

export const obterInformacoesDoDia = (diaSemana: string | null | undefined) =>
  DIAS_SEMANA.find((dia) => dia.id === diaSemana) || DIAS_SEMANA[0];

export const validarFormatoDeHora = (valor: string | null | undefined) => {
  if (!valor) return true;
  if (!/^\d{2}:\d{2}$/.test(valor)) return false;
  const [horas, minutos] = valor.split(":").map(Number);
  return horas >= 0 && horas <= 23 && minutos >= 0 && minutos <= 59;
};

export const formatarEntradaDeHora = (valor: string | boolean | null | undefined) => {
  if (!valor) return "";
  let valorLimpo = String(valor).replace(/\D/g, "");
  if (valorLimpo.length > 4) valorLimpo = valorLimpo.substring(0, 4);
  if (valorLimpo.length > 2)
    return `${valorLimpo.substring(0, 2)}:${valorLimpo.substring(2)}`;
  return valorLimpo;
};

export const converterHoraParaMinutos = (hora: string | null | undefined) => {
  if (!validarFormatoDeHora(hora) || !hora) return null;
  const [horas, minutos] = hora.split(":").map(Number);
  return horas * 60 + minutos;
};

export const calcularIntervaloEmMinutos = (entrada: string | null | undefined, saida: string | null | undefined) => {
  const inicio = converterHoraParaMinutos(entrada);
  const fim = converterHoraParaMinutos(saida);
  if (inicio === null || fim === null) return 0;
  if (inicio === fim) return 0;
  return fim > inicio ? fim - inicio : 1440 - inicio + fim;
};

export const formatarParaHoraDecimal = (totalMinutos: number | null | undefined) => {
  const valorDecimal = (totalMinutos || 0) / 60;
  return Number.isInteger(valorDecimal)
    ? valorDecimal.toString()
    : valorDecimal.toFixed(1);
};

export const formatarParaHorasEMinutos = (totalMinutos: number | null | undefined) => {
  const horas = Math.floor((totalMinutos || 0) / 60);
  const minutos = (totalMinutos || 0) % 60;
  if (!horas && !minutos) return "0h";
  if (!minutos) return `${horas}h`;
  return `${horas}h${String(minutos).padStart(2, "0")}min`;
};

export const verificarSePeriodoEstaIncompleto = (entrada: string | null | undefined, saida: string | null | undefined) =>
  Boolean((entrada && !saida) || (!entrada && saida));

export const analisarDiaDeTrabalho = (diaBase: WorkDay = {}) => {
  if (!diaBase.diaTrabalho) {
    return {
      ...diaBase,
      minutos: 0,
      periodos: [],
      incompleto: false,
      semHorario: false,
      atravessaMeiaNoite: false,
    };
  }
  const primeiroIncompleto = verificarSePeriodoEstaIncompleto(
    diaBase.entrada1,
    diaBase.saida1,
  );
  const segundoIncompleto = verificarSePeriodoEstaIncompleto(
    diaBase.entrada2,
    diaBase.saida2,
  );
  const primeiroCompleto = Boolean(diaBase.entrada1 && diaBase.saida1);
  const segundoCompleto = Boolean(diaBase.entrada2 && diaBase.saida2);

  const periodosCalculados = [
    primeiroCompleto
      ? {
          entrada: diaBase.entrada1,
          saida: diaBase.saida1,
          minutos: calcularIntervaloEmMinutos(diaBase.entrada1, diaBase.saida1),
        }
      : null,
    segundoCompleto
      ? {
          entrada: diaBase.entrada2,
          saida: diaBase.saida2,
          minutos: calcularIntervaloEmMinutos(diaBase.entrada2, diaBase.saida2),
        }
      : null,
  ].filter((periodo): periodo is NonNullable<typeof periodo> => Boolean(periodo));

  return {
    ...diaBase,
    minutos: periodosCalculados.reduce(
      (total, periodo) => total + periodo.minutos,
      0,
    ),
    periodos: periodosCalculados,
    incompleto: primeiroIncompleto || segundoIncompleto,
    semHorario: !primeiroCompleto && !segundoCompleto,
    atravessaMeiaNoite: periodosCalculados.some(
      (periodo) =>
        Number(converterHoraParaMinutos(periodo.saida)) <
        Number(converterHoraParaMinutos(periodo.entrada)),
    ),
  };
};

export const analisarEscalaCompleta = (turno: { dias?: WorkDay[] | null } = {}) => {
  const mapaDeDias = new Map(
    (turno.dias || []).map((dia) => [dia.diaSemana, dia]),
  );

  const diasAnalisados = ORDEM_FORMULARIO.map((idDia) => {
    const diaEncontrado = mapaDeDias.get(idDia);
    if (diaEncontrado)
      return analisarDiaDeTrabalho({
        ...diaEncontrado,
        diaTrabalho: diaEncontrado.diaTrabalho !== false,
      });
    return analisarDiaDeTrabalho({ diaSemana: idDia, diaTrabalho: false });
  });

  const diasDeTrabalho = diasAnalisados.filter((dia) => dia.diaTrabalho);
  const folgas = diasAnalisados.filter((dia) => !dia.diaTrabalho);
  const diasFimDeSemana = diasDeTrabalho.filter(
    (dia) => obterInformacoesDoDia(dia.diaSemana).fimSemana,
  );
  const diasComDoisPeriodos = diasDeTrabalho.filter(
    (dia) => dia.periodos.length > 1,
  );
  const totalMinutosSemanais = diasAnalisados.reduce(
    (total, dia) => total + dia.minutos,
    0,
  );

  return {
    dias: diasAnalisados,
    quantidadeDiasTrabalho: diasDeTrabalho.length,
    quantidadeFolgas: folgas.length,
    quantidadeFimDeSemana: diasFimDeSemana.length,
    quantidadeDoisPeriodos: diasComDoisPeriodos.length,
    minutosSemanais: totalMinutosSemanais,
    possuiFimDeSemana: diasFimDeSemana.length > 0,
    atravessaMeiaNoite: diasAnalisados.some((dia) => dia.atravessaMeiaNoite),
  };
};