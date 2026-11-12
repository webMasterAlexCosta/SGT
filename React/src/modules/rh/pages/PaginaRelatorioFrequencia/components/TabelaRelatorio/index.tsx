type FrequencyReportRow = ReturnType<typeof import('../../../../../../services/rh/ServicoDominioRelatorioFrequencia').formatarLinhaRelatorio>;
import React from "react";
import { CalendarDays, Clock3, Users, ChevronLeft, ChevronRight } from "lucide-react";
import { formatarDecimal } from "../../utils";
import estilos from "./index.module.css";
import { agruparContextosPorColaborador } from "../../../../../../services/rh/paginacaoRelatorios.ts";

const classePorSigla: Readonly<Record<string, string | undefined>> = {
  P: estilos.presenca,
  F: estilos.falta,
  PP: estilos.parcial,
  AT: estilos.atestado,
  D: estilos.descanso,
  FER: estilos.feriado,
  DP: estilos.dsrPerdido,
  RT: estilos.rt,
  SUSP: estilos.suspensao,
  ADV: estilos.advertencia,
  FR: estilos.folgaRemunerada,
};

const CabecalhoResumo = ({ children, titulo, tom = "neutro", inicioGrupo = false }: { children: React.ReactNode; titulo: (string); tom?: (string); inicioGrupo?: boolean }) => (
  <th
    className={`${estilos.cabResumo} ${estilos[tom]} ${inicioGrupo ? estilos.inicioGrupo : ""}`}
    title={titulo}
  >
    {children}
  </th>
);

const CabecalhoTabela = ({ diasDoMes }: { diasDoMes: string[] }) => (
  <thead>
    <tr>
      <th className={estilos.cabNome}>Colaborador</th>
      <th className={estilos.cabLotacao}>Lotação</th>
      <th className={estilos.cabTurno}>Turno</th>
      {diasDoMes.map((dia) => (
        <th key={dia} className={estilos.cabDia}>
          <span>{dia.slice(8, 10)}</span>
          <small>/{dia.slice(5, 7)}</small>
        </th>
      ))}
      <CabecalhoResumo titulo="Presenças integrais" tom="verde" inicioGrupo>P</CabecalhoResumo>
      <CabecalhoResumo titulo="Presenças parciais" tom="laranja">PP</CabecalhoResumo>
      <CabecalhoResumo titulo="Faltas injustificadas" tom="vermelho">F</CabecalhoResumo>
      <CabecalhoResumo titulo="Atestados médicos" tom="azul">AT</CabecalhoResumo>
      <CabecalhoResumo titulo="Descansos / Folgas / DSR" tom="cinza">D</CabecalhoResumo>
      <CabecalhoResumo titulo="Repousos trabalhados" tom="teal">RT</CabecalhoResumo>
      <CabecalhoResumo titulo="Tempo físico efetivamente trabalhado" tom="amarelo" inicioGrupo>H.REAL</CabecalhoResumo>
      <CabecalhoResumo titulo="Horas noturnas reduzidas" tom="azul">H.NOT</CabecalhoResumo>
      <CabecalhoResumo titulo="Total pagável apurado" tom="roxo">H.PAG</CabecalhoResumo>
      <CabecalhoResumo titulo="Total pagável em formato decimal" tom="cinza">DEC</CabecalhoResumo>
    </tr>
  </thead>
);

const LinhaContexto = ({ empregado, indice, diasDoMes }: { empregado: FrequencyReportRow; indice: number; diasDoMes: string[] }) => (
  <tr key={empregado.id || indice} className={indice % 2 === 0 ? estilos.par : estilos.impar}>
    <td className={estilos.nome}>
      <strong title={empregado.nome ?? undefined}>{empregado.nome || "Nome não informado no relatório"}</strong>
      <span title={empregado.cargo}>{empregado.cargo || "Função não informada"}</span>
      <span title={empregado.contrato}>{empregado.contrato || "Contrato não informado"}</span>
    </td>
    <td className={estilos.lotacao} title={empregado.lotacao}>
      {empregado.lotacao || "—"}
    </td>
    <td className={estilos.turno} title={empregado.turno}>
      {empregado.turno || "—"}
    </td>
    {diasDoMes.map((dia) => {
      const registro = empregado.diario?.[dia] || {};
      return (
        <td key={dia} className={estilos.dia} title={registro.status || "Sem registro"}>
          <span className={`${estilos.emblema} ${classePorSigla[String(registro.sigla)] || estilos.vazio}`}>
            {registro.sigla || "—"}
          </span>
        </td>
      );
    })}
    <td className={`${estilos.resumo} ${estilos.resumoVerde} ${estilos.inicioGrupo}`}>{empregado.totais?.presencas ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoLaranja}`}>{empregado.totais?.parciais ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoVermelho}`}>{empregado.totais?.faltas ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoAzul}`}>{empregado.totais?.atestados ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoCinza}`}>{empregado.totais?.descansos ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoTeal}`}>{empregado.totais?.rt ?? 0}</td>
    <td className={`${estilos.resumo} ${estilos.resumoAmarelo} ${estilos.inicioGrupo}`}>{empregado.totais?.horasFisicas || "00:00"}</td>
    <td className={`${estilos.resumo} ${estilos.resumoAzul}`}>{empregado.totais?.horasNoturnas || "00:00"}</td>
    <td className={`${estilos.resumo} ${estilos.resumoRoxo}`}>{empregado.totais?.horasPagaveis || "00:00"}</td>
    <td className={`${estilos.resumo} ${estilos.resumoCinza}`}>{formatarDecimal(empregado.totais?.totalPagavelDecimal)}h</td>
  </tr>
);

const LinhaColaborador = ({ grupo, indice, diasDoMes }: { grupo: { id: string | number | null; linhas: FrequencyReportRow[] }; indice: (number); diasDoMes: string[] }) => {
  if (grupo.linhas.length === 1) return <LinhaContexto empregado={grupo.linhas[0]} indice={indice} diasDoMes={diasDoMes} />;
  return (
    <tr className={indice % 2 === 0 ? estilos.par : estilos.impar}>
      <td colSpan={diasDoMes.length + 13} className={estilos.contextosColaborador}>
        <details>
          <summary><strong>{grupo.linhas[0].nome || "Nome não informado no relatório"}</strong> · {grupo.linhas.length} contextos históricos</summary>
          <div className={estilos.rolagem}>
            <table className={estilos.tabela} aria-label={grupo.linhas[0].nome
              ? `Contextos históricos de ${grupo.linhas[0].nome}`
              : "Contextos históricos — nome não informado no relatório"}>
              <CabecalhoTabela diasDoMes={diasDoMes} />
              <tbody>{grupo.linhas.map((empregado, contexto) => <LinhaContexto key={empregado.id || contexto} empregado={empregado} indice={contexto} diasDoMes={diasDoMes} />)}</tbody>
            </table>
          </div>
        </details>
      </td>
    </tr>
  );
};

export const TabelaRelatorio = ({
  dados,
  diasDoMes,
  totalColaboradores = dados.length,
  pagina = 0,
  totalPaginas = 0,
  primeira = true,
  ultima = true,
  carregando = false,
  aoMudarPagina,
  tamanho = 5,
  aoMudarTamanho,
}: { dados: FrequencyReportRow[]; diasDoMes: (string[]); totalColaboradores?: (number); pagina?: (number); totalPaginas?: (number); primeira?: (boolean); ultima?: (boolean); carregando?: (boolean); aoMudarPagina: (pagina: number) => void; tamanho?: (number); aoMudarTamanho: (tamanho: number) => void }) => {
  const grupos = agruparContextosPorColaborador(dados);
  return (
  <section className={estilos.cartao}>
    <div className={estilos.info}>
      <div className={estilos.metricasTabela}>
        <span className={estilos.chipMetrica}>
          <Users size={14} />
          <strong>{Number(totalColaboradores).toLocaleString("pt-BR")}</strong> colaborador(es) encontrados
        </span>
        <span className={estilos.chipMetrica}>
          <CalendarDays size={14} />
          <strong>{diasDoMes.length}</strong> dias apurados
        </span>
      </div>
      <div className={estilos.notaHoras}>
        <Clock3 size={14} />
        <span><strong>H.REAL</strong>: tempo físico · <strong>H.NOT</strong> e <strong>H.PAG</strong>: incluem redução noturna</span>
      </div>
    </div>

    <div className={estilos.rolagem}>
      <table className={estilos.tabela} aria-label="Relatório de frequência por colaborador">
        <CabecalhoTabela diasDoMes={diasDoMes} />
        <tbody>
          {grupos.map((grupo, indice) => <LinhaColaborador key={grupo.id} grupo={grupo} indice={indice} diasDoMes={diasDoMes} />)}
        </tbody>
      </table>
    </div>

    {Number(totalColaboradores) > 0 && (
      <div className={estilos.paginacao}>
        <div className={estilos.infoPaginaGrupo}>
          <div className={estilos.infoPagina}>
            Página <strong>{pagina + 1}</strong> de <strong>{Math.max(1, totalPaginas)}</strong>
          </div>
          <label className={estilos.tamanhoPagina}>
            Colaboradores por página
            <select value={tamanho} disabled={carregando} onChange={(evento) => aoMudarTamanho?.(Number(evento.target.value))}>
              <option value="5">5</option>
              <option value="25">25</option>
              <option value="100">100</option>
              <option value="200">200</option>
              <option value="500">500</option>
            </select>
          </label>
        </div>
        <div className={estilos.botoesPaginacao}>
          <button
            type="button"
            className={estilos.btnPaginacao}
            disabled={primeira || carregando}
            onClick={() => aoMudarPagina?.(Math.max(0, pagina - 1))}
          >
            <ChevronLeft size={14} /> Anterior
          </button>
          <button
            type="button"
            className={estilos.btnPaginacao}
            disabled={ultima || carregando}
            onClick={() => aoMudarPagina?.(pagina + 1)}
          >
            Próxima <ChevronRight size={14} />
          </button>
        </div>
      </div>
    )}
  </section>
);
};
