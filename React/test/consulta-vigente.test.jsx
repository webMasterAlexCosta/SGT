import { act, renderHook } from '@testing-library/react';
import { expect, it } from 'vitest';
import { useConsultaVigente } from '../src/core/hooks/useConsultaVigente.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
it('invalida a operação anterior inclusive quando transporte ignora abort',()=>{
  const {result}=renderHook(()=>useConsultaVigente());
  const a=result.current.iniciar();const b=result.current.iniciar();
  expect(a.signal.aborted).toBe(true);expect(a.atual()).toBe(false);expect(b.atual()).toBe(true);
});
it('uma geração de sessão nova descarta respostas pendentes',()=>{
  const {result}=renderHook(()=>useConsultaVigente());const a=result.current.iniciar();
  act(()=>invalidarGeracaoSessao());expect(a.atual()).toBe(false);
  const b=result.current.iniciar();expect(b.atual()).toBe(true);
});
it('troca de filtro e desmontagem invalidam tokens antigos',()=>{
  const {result,rerender,unmount}=renderHook(({filtro})=>useConsultaVigente(filtro),{initialProps:{filtro:'A'}});
  const a=result.current.iniciar();rerender({filtro:'B'});expect(a.atual()).toBe(false);
  const b=result.current.iniciar();unmount();expect(b.atual()).toBe(false);
});
