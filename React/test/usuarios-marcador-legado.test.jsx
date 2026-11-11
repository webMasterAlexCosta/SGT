import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { TabelaUsuarios } from '../src/modules/admin/pages/PaginaUsuarios/components/TabelaUsuarios/index.tsx';
afterEach(cleanup);
it.each([false,true])('marcador legado=%s não oferece nem promete liberação individual de frequência', marcado => {
  render(<TabelaUsuarios usuarios={[{id:2,username:'operador-sintetico',perfil:'OPERACIONAL',active:true,retroactiveUnlocked:marcado}]}
    gestorAtual={{id:1,perfil:'GESTOR_LOCAL'}} carregando={false} obterPerfil={u=>u.perfil} podeGerenciarUsuario={()=>true}
    descreverAbrangencia={()=>'Escopo sintético'} aoRedefinirSenha={vi.fn()} aoEditar={vi.fn()} aoAlterarAtivacao={vi.fn()} aoMudarPagina={vi.fn()} />);
  expect(screen.queryByRole('button',{name:/acesso retroativo/i})).not.toBeInTheDocument();
  expect(screen.queryByText(/^Retroativo(?: até|$)/)).not.toBeInTheDocument();
  if(marcado)expect(screen.getByText('Marcador legado')).toHaveAttribute('title',expect.stringMatching(/não altera.*janela/i));
  else expect(screen.queryByText('Marcador legado')).not.toBeInTheDocument();
  expect(screen.getByRole('button',{name:'Editar cadastro de operador-sintetico'})).toBeEnabled();
});
