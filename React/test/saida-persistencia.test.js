import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
const novaInstancia = async () => { vi.resetModules(); return import('../src/core/security/saidaSessao.js'); };
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); });
afterEach(() => { vi.restoreAllMocks(); });
describe('Intenção de saída atravessa nova instância JavaScript', () => {
  it('nova instância mantém saída persistida e apenas confirmação de login explícito a limpa', async () => {
    const primeira = await novaInstancia(); const intencao = primeira.registrarIntencaoSaida();
    const segunda = await novaInstancia(); expect(segunda.obterIntencaoSaida()).toBe(intencao);
    expect(segunda.confirmarLoginExplicito(intencao)).toBe(true);
    const terceira = await novaInstancia(); expect(terceira.obterIntencaoSaida()).toBeNull();
  });
  it('leitura permitida com gravação negada não autoriza restaurar cookie numa instância nova', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Gravação indisponível', 'SecurityError'); });
    const primeira = await novaInstancia(); primeira.registrarIntencaoSaida();
    const segunda = await novaInstancia(); expect(segunda.obterIntencaoSaida()).not.toBeNull();
  });
  it('uma saída mais nova impede que a confirmação de login antiga limpe seu marcador', async () => {
    const primeira = await novaInstancia(); const antiga = primeira.registrarIntencaoSaida();
    const nova = primeira.registrarIntencaoSaida(); expect(primeira.confirmarLoginExplicito(antiga)).toBe(false);
    const segunda = await novaInstancia(); expect(segunda.obterIntencaoSaida()).toBe(nova);
  });
});
