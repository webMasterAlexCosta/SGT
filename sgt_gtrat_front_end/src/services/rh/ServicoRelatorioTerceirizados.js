import { CONFIG } from '../../core/config';
import { requisitar } from '../../core/http';

/**
 * A interface nunca materializa a base inteira. A leitura visual é paginada em
 * blocos de até 5.000 registros; exportações completas são geradas em fluxo no
 * backend e baixadas diretamente pelo navegador.
 */
export const RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO = 5000;

const consolidado = async (params = {}, options = {}) => requisitar({
  method: 'GET',
  url: '/relatorios/terceirizados/consolidado',
  params,
  retries: 1,
  silentError: true,
  ...options,
});

const obterUrlExportacao = (params = {}, formato = 'XLSX') => {
  const query = new URLSearchParams();
  Object.entries({ ...params, formato: String(formato || 'XLSX').toUpperCase() })
    .forEach(([chave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '' && valor !== false) {
        query.append(chave, String(valor));
      }
    });
  const base = String(CONFIG.API_URL || '').replace(/\/$/, '');
  return `${base}/relatorios/terceirizados/exportacao?${query.toString()}`;
};

export const ServicoRelatorioTerceirizados = {
  consolidado,
  obterUrlExportacao,
};
