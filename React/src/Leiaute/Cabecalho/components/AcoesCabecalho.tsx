import React from 'react';
import { UserCircle, LogOut } from 'lucide-react';
import { CONFIG } from '../../../core/config';
import estilos from '../index.module.css';

export const AcoesCabecalho = ({ usuario, aoSair, aoNavegar }: { usuario: import("../../../contracts/auth").AuthenticatedUser | null; aoSair: () => void; aoNavegar: (id: string) => void }) => {
  const chaveArmazenamentoFoto = `${CONFIG.APP_NAME.toLowerCase()}_foto_url`;
  const fotoSalva = localStorage.getItem(chaveArmazenamentoFoto) || usuario?.fotoUrl;
  
  const urlBase = CONFIG.API_URL.replace(/\/api\/?$/, '');
  const urlFotoCompleta = fotoSalva ? `${urlBase}${fotoSalva}` : null;

  return (
    <>
      <button onClick={() => aoNavegar('/perfil')} className={estilos.botaoPerfil} type="button" title="Meu Perfil">
        {urlFotoCompleta ? (
          <img src={urlFotoCompleta} alt="Perfil do Usuário" width="38" height="38" className={estilos.imagemPerfil} loading="lazy" />
        ) : (
          <UserCircle size={24} />
        )}
      </button>

      <div className={estilos.divisor}></div>
      
      <button onClick={aoSair} className={estilos.botaoSair} type="button" title="Sair do Sistema">
        <LogOut size={20} strokeWidth={2.5} />
        <span className={estilos.textoSair}>Sair</span>
      </button>
    </>
  );
};