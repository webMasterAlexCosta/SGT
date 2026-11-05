import React, { useId } from 'react';
import estilos from './historico.module.css';

export const CampoHistorico = ({ rotulo, valor, aoMudar, tipo = 'text', opcoes, ajuda, amplo = false, ...props }) => {
  const id = useId();
  const comum = { id, value: valor ?? '', onChange: (evento) => aoMudar?.(evento.target.value), 'aria-describedby': ajuda ? `${id}-ajuda` : undefined, ...props };
  return <div className={`${estilos.campo} ${amplo ? estilos.campoAmplo : ''}`}>
    <label htmlFor={id}>{rotulo}</label>
    {opcoes ? <select {...comum}>{opcoes.map(([chave, nome]) => <option value={chave} key={chave}>{nome}</option>)}</select>
      : tipo === 'textarea' ? <textarea {...comum} /> : <input type={tipo} {...comum} />}
    {ajuda && <small id={`${id}-ajuda`}>{ajuda}</small>}
  </div>;
};
