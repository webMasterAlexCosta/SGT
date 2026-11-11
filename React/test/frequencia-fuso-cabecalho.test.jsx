import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { afterEach, expect, it, vi } from 'vitest';
import { CabecalhoFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/components/CabecalhoFrequencia.tsx';

afterEach(() => { cleanup(); vi.useRealTimers(); });

it.each([
  ['antes', '2026-11-02T02:59:59Z', '2026-11-02T00:00:00', '00:00:01'],
  ['no limite', '2026-11-02T03:00:00Z', '2026-11-02T00:00:00', 'Expirado'],
  ['depois', '2026-11-02T03:00:01Z', '2026-11-02T00:00:00', 'Expirado'],
  ['com Z', '2026-11-02T02:30:00Z', '2026-11-02T03:00:00Z', '00:30:00'],
  ['com offset', '2026-11-02T02:30:00Z', '2026-11-02T00:00:00-03:00', '00:30:00'],
])('representa a expiração recebida no perfil %s sem usar o fuso do dispositivo', (_caso, agora, prazo, esperado) => {
  vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] });
  vi.setSystemTime(new Date(agora));
  render(<CabecalhoFrequencia
    usuarioLogado={{ isRetroactiveUnlocked: true, retroactiveExpireAt: prazo }}
    eGestorOperacional={false} dataReferencia="2026-11-01"
    statusDaData={{ valid: true }} listaDeFrequencia={[]} />);
  expect(screen.getByText(new RegExp(`RETROATIVO LIBERADO.*${esperado}`))).toBeInTheDocument();
});
