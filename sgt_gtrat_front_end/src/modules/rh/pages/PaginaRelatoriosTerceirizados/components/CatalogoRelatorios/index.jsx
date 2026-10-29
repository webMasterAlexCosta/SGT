import React from "react";
import { ArrowRight, CalendarRange, ClipboardCheck, FileClock, UserRoundX } from "lucide-react";
import estilos from "./index.module.css";

const RELATORIOS = [
  { titulo: "Frequência consolidada", descricao: "Presenças, faltas, atestados, registros parciais e horas trabalhadas por período.", caminho: "/relatorio-mensal", Icone: CalendarRange },
  { titulo: "Absenteísmo", descricao: "Taxa individual, faltas, atestados, DSR perdido e índice de disrupção.", caminho: "/relatorio-absenteismo", Icone: UserRoundX },
  { titulo: "Cadastro e contratos", descricao: "Quadro atual de empregados, situação cadastral, lotação, turno e vínculo contratual.", caminho: "/relatorios", Icone: ClipboardCheck, atual: true },
  { titulo: "Histórico de movimentações", descricao: "Consulte admissões, alterações cadastrais e mudanças de contrato pela auditoria.", caminho: "/logs", Icone: FileClock },
];

export const CatalogoRelatorios = ({ aoNavegar }) => (
  <section className={estilos.secaoCatalogo}>
    <header><strong>Catálogo de relatórios</strong><span>Acesse análises específicas do sistema</span></header>
    <div className={estilos.gradeCatalogo}>
      {RELATORIOS.map(({ titulo, descricao, caminho, Icone, atual }) => (
        <button key={titulo} type="button" className={`${estilos.cartaoRelatorio} ${atual ? estilos.cartaoAtual : ""}`} onClick={() => !atual && aoNavegar(caminho)} disabled={atual}>
          <span className={estilos.icone}><Icone size={19} /></span>
          <span className={estilos.textos}><strong>{titulo}</strong><small>{descricao}</small></span>
          <ArrowRight size={17} className={estilos.seta} />
        </button>
      ))}
    </div>
  </section>
);
