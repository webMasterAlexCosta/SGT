import type { TotaisAnalitico } from '../../../../contracts/painel-analitico';
export const numero = (valor: number) => new Intl.NumberFormat('pt-BR').format(valor);
export const percentual = (valor: number | null) => valor === null ? 'Sem base' : `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(valor)}%`;
export const dataCivil = (valor: string | null) => valor ? valor.split('-').reverse().join('/') : '—';
export const intervalo = (inicio: string | null, fim: string | null) => `${dataCivil(inicio)} a ${dataCivil(fim)}`;
export const nomesMetricas: Record<keyof TotaisAnalitico, string> = { registros: 'Registros', presencas: 'Presenças', ausencias: 'Ausências' };
