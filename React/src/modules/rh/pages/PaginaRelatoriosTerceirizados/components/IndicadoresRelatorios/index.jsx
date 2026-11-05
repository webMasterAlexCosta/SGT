import React from "react";
import { BriefcaseBusiness, CircleCheckBig, FileWarning, Scale, Target, UserPlus, UsersRound, Users } from "lucide-react";
import estilos from "./index.module.css";

const RESULTADO = [
  { chave: "empregados", rotulo: "Empregados no resultado", Icone: UsersRound, classe: "azul" },
  { chave: "ativos", rotulo: "Ativos no resultado", Icone: CircleCheckBig, classe: "verde" },
  { chave: "semContratoVigente", rotulo: "Sem contrato vigente", Icone: FileWarning, classe: "vermelho" },
  { chave: "admitidosNoMes", rotulo: "Admitidos no mês", Icone: UserPlus, classe: "roxo" },
];

const CONTRATOS = [
  { chave: "contratosVigentes", rotulo: "Contratos vigentes no escopo", Icone: BriefcaseBusiness, classe: "amarelo" },
  { chave: "efetivoPrevisto", rotulo: "Efetivo previsto vigente", Icone: Target, classe: "azul" },
  { chave: "efetivoVinculado", rotulo: "Efetivo vinculado vigente", Icone: Users, classe: "verde" },
  { chave: "saldoContratual", rotulo: "Saldo (+ falta / − excesso)", Icone: Scale, classe: "vermelho" },
];

const formatar = (valor) => Number(valor || 0).toLocaleString("pt-BR");

const Grade = ({ titulo, descricao, itens, indicadores }) => (
  <section className={estilos.blocoIndicadores} aria-label={titulo}>
    <header><strong>{titulo}</strong><span>{descricao}</span></header>
    <div className={estilos.gradeIndicadores}>
      {itens.map(({ chave, rotulo, Icone, classe }) => (
        <article key={chave} className={`${estilos.cartaoIndicador} ${estilos[classe]}`}>
          <div className={estilos.icone}><Icone size={19} /></div>
          <div><strong>{formatar(indicadores?.[chave])}</strong><span>{rotulo}</span></div>
        </article>
      ))}
    </div>
  </section>
);

export const IndicadoresRelatorios = ({ indicadores }) => (
  <div className={estilos.conjuntoIndicadores}>
    <Grade
      titulo="Resultado filtrado"
      descricao="Totais calculados no MySQL sobre todos os colaboradores que atendem aos filtros."
      itens={RESULTADO}
      indicadores={indicadores}
    />
    <Grade
      titulo="Dimensionamento contratual"
      descricao="Considera contratos vigentes do escopo (ou o contrato selecionado); filtros de colaborador não alteram a capacidade prevista."
      itens={CONTRATOS}
      indicadores={indicadores}
    />
  </div>
);
