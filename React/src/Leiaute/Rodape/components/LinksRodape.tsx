import React from "react";
import { EnvolucroDica } from "../../../core/ui/EnvolucroDica";
import estilos from "../index.module.css";

const ItemLink = ({ dica, rotulo, destino, aoClicar }: { dica?: string; rotulo: string; destino?: string; aoClicar?: () => void }) => (
  <li>
    {!aoClicar && (!destino || destino === "#") ? (
      <span className={estilos.linkIndisponivel}><span>{rotulo}</span><small>Indisponível</small></span>
    ) : (
    <EnvolucroDica
      tooltip={dica}
      position="top"
      wrapperClass={estilos.envoltorioDicaRodape}
    >
      {aoClicar ? (
        <button onClick={aoClicar} className={estilos.botaoLink}>
          {rotulo}
        </button>
      ) : (
        <a href={destino}>{rotulo}</a>
      )}
    </EnvolucroDica>
    )}
  </li>
);

export const LinksRodape = ({ aoAbrirCreditos }: { aoAbrirCreditos?: () => void }) => (
  <div className={estilos.colunas}>
    <div className={estilos.coluna}>
      <h4>Fale Conosco</h4>
      <ul>
        <ItemLink
          dica="Abrir chamado para a equipe de suporte"
          rotulo="Suporte Gestão Local"
        />
        <ItemLink
          dica="Documentação e passo a passo do sistema"
          rotulo="Manual do sistema"
        />
      </ul>
    </div>
    <div className={estilos.coluna}>
      <h4>Sobre o Sistema</h4>
      <ul>
        <ItemLink
          dica="Baixar documentação legal (PDF)"
          rotulo="Subsidio Jurídico PDF"
        />
        <ItemLink
          dica="Conheça a equipe e a tecnologia"
          rotulo="Créditos"
          aoClicar={aoAbrirCreditos}
        />
      </ul>
    </div>
    <div className={`${estilos.coluna} ${estilos.colunaDireita}`}>
      <h4>Informações</h4>
      <ul>
        <ItemLink
          dica="Ler as regras de utilização"
          rotulo="Política de uso interno"
        />
        <ItemLink
          dica="Diretrizes de proteção de dados (LGPD)"
          rotulo="Segurança da informação"
        />
      </ul>
    </div>
  </div>
);
