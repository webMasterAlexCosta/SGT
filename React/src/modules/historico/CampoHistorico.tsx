import React, { useId } from 'react';
import estilos from './historico.module.css';

export const CampoHistorico = ({ rotulo, valor, aoMudar, tipo = 'text', opcoes, ajuda, amplo = false, ...props }: React.HTMLAttributes<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement> & Pick<React.InputHTMLAttributes<HTMLInputElement>, "name" | "required" | "disabled" | "min" | "max" | "step" | "maxLength" | "placeholder"> & { rotulo: string; valor?: string | number; aoMudar?: (valor: string) => void; tipo?: string; opcoes?: readonly (readonly (string | number)[])[]; ajuda?: string; amplo?: boolean }) => {
  const id = useId();
  const comum = { id, value: valor ?? '', onChange: (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => aoMudar?.(evento.target.value), 'aria-describedby': ajuda ? `${id}-ajuda` : undefined, ...props };
  return <div className={`${estilos.campo} ${amplo ? estilos.campoAmplo : ''}`}>
    <label htmlFor={id}>{rotulo}</label>
    {opcoes ? <select {...comum}>{opcoes.map(([chave, nome]) => <option value={chave} key={chave}>{nome}</option>)}</select>
      : tipo === 'textarea' ? <textarea {...comum} /> : <input type={tipo} {...comum} />}
    {ajuda && <small id={`${id}-ajuda`}>{ajuda}</small>}
  </div>;
};
