import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import manifesto from '../package.json';
import { ModalCreditosColaboracao } from '../src/core/ui/ModalCreditosColaboracao.jsx';

afterEach(cleanup);

const abrir = () => render(<ModalCreditosColaboracao isOpen onClose={() => {}} />);

describe('créditos correspondem à candidata distribuída', () => {
  it('exibe as versões declaradas das dependências reais do frontend', () => {
    abrir();
    const dependencias = { ...manifesto.dependencies, ...manifesto.devDependencies };
    for (const [nome, pacote] of [
      ['React', 'react'], ['React DOM', 'react-dom'], ['React Router DOM', 'react-router-dom'],
      ['Axios', 'axios'], ['React Toastify', 'react-toastify'], ['Recharts', 'recharts'],
      ['Lucide React', 'lucide-react'], ['Vite', 'vite'], ['@vitejs/plugin-react', '@vitejs/plugin-react'],
      ['@vitejs/plugin-basic-ssl', '@vitejs/plugin-basic-ssl'], ['@vladmandic/face-api', '@vladmandic/face-api'],
      ['jsPDF', 'jspdf'], ['jsPDF AutoTable', 'jspdf-autotable'],
    ]) {
      expect.soft(screen.queryByText(`${nome} ${dependencias[pacote]}`, { exact: true }), pacote).toBeInTheDocument();
    }
  });

  it('informa o intervalo de Node e o gerenciador declarados, sem bibliotecas ausentes', () => {
    abrir();
    expect.soft(screen.queryByText(`Node.js ${manifesto.engines.node}`, { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText(manifesto.packageManager.replace('@', ' '), { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText(/^React Icons /)).not.toBeInTheDocument();
    expect.soft(screen.queryByText(/^Apache HttpClient /)).not.toBeInTheDocument();
  });

  it('identifica Java 17 e Spring Boot 3.5.16 da candidata operacional', () => {
    abrir();
    expect.soft(screen.queryByText('Java 17', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText('Compilação Java 17', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText('Spring Boot 3.5.16', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText(/Java 25/)).not.toBeInTheDocument();
    expect.soft(screen.queryByText('Spring Boot 4.1.0', { exact: true })).not.toBeInTheDocument();
  });

  it('mostra a versão do frontend, sem usar o ano como número de versão', () => {
    abrir();
    expect(screen.getByText(`Gestão de Terceiros • Versão ${manifesto.version}`, { exact: true })).toBeInTheDocument();
  });

  it('distingue detecção para fotografia e dependências instaladas sem uso ativo', () => {
    abrir();
    expect.soft(screen.queryByText('Detecção facial para enquadramento de fotografia', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText('Dependência instalada; avisos atuais usam componente próprio', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText('Dependência instalada; HTTPS atual usa PFX configurado', { exact: true })).toBeInTheDocument();
    expect.soft(screen.queryByText('Reconhecimento / validação facial', { exact: true })).not.toBeInTheDocument();
  });
});
