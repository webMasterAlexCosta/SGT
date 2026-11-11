import type { ChartTooltipProps } from '../../../../../../contracts/painel';
import React from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { BarChart3, PieChart } from "lucide-react";
import estilos from "./index.module.css";

const TooltipGrafico = ({ active, payload, label }: ChartTooltipProps) => {
  if (!active || !payload?.length) return null;
  return <div className={estilos.tooltip}><strong>{label}</strong><span>{String(payload[0].value ?? "")} empregado(s)</span></div>;
};

const quantidade = (item: (import("../../../../../../contracts/java").RelatorioTerceirizadosDTODistribuicao) | (import("../../../../../../contracts/java").RelatorioTerceirizadosDTODistribuicaoContrato)) => Math.max(0, Number(item?.quantidade || 0));

export const GraficosRelatorios = ({ distribuicaoStatus, distribuicaoContratos }: { distribuicaoStatus: (import("../../../../../../contracts/java").RelatorioTerceirizadosDTODistribuicao[]); distribuicaoContratos: (import("../../../../../../contracts/java").RelatorioTerceirizadosDTODistribuicaoContrato[]) }) => {
  const maiorStatus = Math.max(1, ...distribuicaoStatus.map(quantidade));
  const maiorContrato = Math.max(1, ...distribuicaoContratos.map(quantidade));

  return (
    <section className={estilos.gradeGraficos}>
      <article className={estilos.cartaoGrafico}>
        <header><div><PieChart size={17} /><strong>Distribuição por status</strong></div><span>{distribuicaoStatus.length} situação(ões)</span></header>
        <div className={estilos.listaStatus}>
          {distribuicaoStatus.length ? distribuicaoStatus.map((item) => (
            <div key={item.nome} className={estilos.linhaStatus}>
              <div><span>{item.nome}</span><strong>{quantidade(item).toLocaleString("pt-BR")}</strong></div>
              <div className={estilos.trilho}><span style={{ width: `${Math.max(4, (quantidade(item) / maiorStatus) * 100)}%` }} /></div>
            </div>
          )) : <p className={estilos.semDados}>Sem dados para o filtro atual.</p>}
        </div>
      </article>

      <article className={estilos.cartaoGrafico}>
        <header><div><BarChart3 size={17} /><strong>Empregados por contrato</strong></div><span>8 maiores vínculos vigentes</span></header>

        <div className={estilos.listaContratosMobile} aria-label="Distribuição de empregados por contrato">
          {distribuicaoContratos.length ? distribuicaoContratos.map((item) => (
            <div key={`${item.id ?? "sem"}-${item.nome}`} className={estilos.linhaContratoMobile}>
              <div><span title={(item.nome) ?? undefined}>{item.nome}</span><strong>{quantidade(item).toLocaleString("pt-BR")}</strong></div>
              <div className={estilos.trilhoContrato}><span style={{ width: `${Math.max(4, (quantidade(item) / maiorContrato) * 100)}%` }} /></div>
            </div>
          )) : <p className={estilos.semDados}>Sem vínculos contratuais para apresentar.</p>}
        </div>

        <div className={estilos.areaGrafico}>
          {distribuicaoContratos.length ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={distribuicaoContratos} margin={{ top: 10, right: 8, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="nome" tick={{ fontSize: 10, fill: "#64748b" }} tickLine={false} axisLine={false} interval={0} tickFormatter={(valor: string | number) => String(valor).length > 12 ? `${String(valor).slice(0, 10)}…` : String(valor)} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: "#64748b" }} tickLine={false} axisLine={false} />
                <Tooltip content={<TooltipGrafico />} cursor={{ fill: "rgba(0,65,107,.05)" }} />
                <Bar dataKey="quantidade" fill="#00416B" radius={[5, 5, 0, 0]} maxBarSize={38} />
              </BarChart>
            </ResponsiveContainer>
          ) : <p className={estilos.semDados}>Sem vínculos contratuais para apresentar.</p>}
        </div>
      </article>
    </section>
  );
};
