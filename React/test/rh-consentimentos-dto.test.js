import { describe, expect, it } from 'vitest';
import { prepararAcaoAuditavel, prepararDesligamento, prepararRetificacaoInicio } from '../src/services/rh/ServicoDominioPessoaVinculo.js';
import { prepararAlteracaoAlocacao } from '../src/services/rh/ServicoDominioAlocacaoVinculo.js';

describe('Consentimentos explícitos dos DTOs Java de RH', () => {
  it.each([
    ['RetificacaoInicioVinculoRequisicao', prepararRetificacaoInicio],
    ['DesligamentoVinculoRequisicao', prepararDesligamento],
    ['AlteracaoAlocacaoVinculoRequisicao', prepararAlteracaoAlocacao],
  ])('%s transmite o aceite de excedente e mantém falso quando não informado', (_nome, preparar) => {
    expect(preparar({ confirmarInclusaoExcedente: true }).confirmarInclusaoExcedente).toBe(true);
    expect(preparar({}).confirmarInclusaoExcedente).toBe(false);
  });

  it('desligamento transmite a precedência do último dia sem estendê-la aos comandos genéricos', () => {
    expect(prepararDesligamento({ confirmarPrecedenciaUltimoDia: true }).confirmarPrecedenciaUltimoDia).toBe(true);
    expect(prepararDesligamento({}).confirmarPrecedenciaUltimoDia).toBe(false);
    expect(prepararAcaoAuditavel({ confirmarInclusaoExcedente: true })).not.toHaveProperty('confirmarInclusaoExcedente');
  });
});
