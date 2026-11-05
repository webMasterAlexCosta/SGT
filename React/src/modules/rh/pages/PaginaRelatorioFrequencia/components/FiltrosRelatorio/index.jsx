import React, { useEffect, useRef, useState } from "react";
import { AlertTriangle, Briefcase, Calendar, ChevronDown, Clock, Filter, MapPin, Search } from "lucide-react";
import estilos from "./index.module.css";

const LIMITE_DIAS = 366;

export const FiltrosRelatorio = ({
  dataInicio,
  setDataInicio,
  dataFim,
  setDataFim,
  contratos,
  contratosSelecionados,
  setContratosSelecionados,
  lotacoesDisponiveis,
  lotacaoSelecionada,
  setLotacaoSelecionada,
  turnosDisponiveis,
  turnoSelecionado,
  setTurnoSelecionado,
  aoGerarRelatorio,
  carregando,
}) => {
  const [dropdownContratoAberto, setDropdownContratoAberto] = useState(false);
  const [erroData, setErroData] = useState(null);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const fecharAoClicarFora = (evento) => {
      if (dropdownRef.current && !dropdownRef.current.contains(evento.target)) {
        setDropdownContratoAberto(false);
      }
    };
    document.addEventListener("mousedown", fecharAoClicarFora);
    return () => document.removeEventListener("mousedown", fecharAoClicarFora);
  }, []);

  useEffect(() => {
    if (!dataInicio || !dataFim) {
      setErroData("Informe o período com datas válidas.");
      return;
    }
    const inicio = new Date(`${dataInicio}T12:00:00Z`);
    const fim = new Date(`${dataFim}T12:00:00Z`);
    if (inicio > fim) {
      setErroData("A data inicial não pode ser superior à data final.");
      return;
    }
    const quantidadeDias = Math.ceil((fim - inicio) / 86400000) + 1;
    if (quantidadeDias > LIMITE_DIAS) {
      setErroData(`Período de ${quantidadeDias} dias excede o limite permitido de ${LIMITE_DIAS} dias.`);
      return;
    }
    setErroData(null);
  }, [dataInicio, dataFim]);

  const listaContratos = contratos || [];
  const todosSelecionados = contratosSelecionados.length === 0 || contratosSelecionados.length === listaContratos.length;

  const alternarContrato = (id) =>
    setContratosSelecionados(
      contratosSelecionados.includes(id)
        ? contratosSelecionados.filter((item) => item !== id)
        : [...contratosSelecionados, id]
    );

  const contratoUnico = listaContratos.find((c) => String(c.id) === contratosSelecionados[0]);
  const rotuloContrato = todosSelecionados
    ? "Todos os contratos autorizados"
    : contratosSelecionados.length === 1
      ? `${contratoUnico?.numero ? `${contratoUnico.numero} · ` : ""}${contratoUnico?.nome || contratoUnico?.descricao || "1 selecionado"}`
      : `${contratosSelecionados.length} contratos selecionados`;

  return (
    <section className={estilos.cartao}>
      <div className={estilos.cabecalhoFiltros}>
        <div className={estilos.tituloSessao}>
          <Filter size={16} />
          <h2>Parâmetros de Pesquisa</h2>
        </div>
        <span className={estilos.dica}>Filtros organizacionais opcionais</span>
      </div>

      <div className={estilos.grade}>
        <label className={estilos.campo}>
          <span className={estilos.rotulo}><Calendar size={13} /> Início</span>
          <input
            type="date"
            className={estilos.controle}
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
          />
        </label>

        <label className={estilos.campo}>
          <span className={estilos.rotulo}><Calendar size={13} /> Fim</span>
          <input
            type="date"
            className={estilos.controle}
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
          />
        </label>

        <div className={estilos.campo} ref={dropdownRef}>
          <span className={estilos.rotulo}><Briefcase size={13} /> Contrato / Vínculo</span>
          <button
            type="button"
            className={`${estilos.controle} ${estilos.seletorMultiplo}`}
            onClick={() => setDropdownContratoAberto((prev) => !prev)}
            aria-expanded={dropdownContratoAberto}
          >
            <span className={estilos.textoSeletor}>{rotuloContrato}</span>
            <ChevronDown size={15} className={`${estilos.seta} ${dropdownContratoAberto ? estilos.setaAberta : ""}`} />
          </button>

          {dropdownContratoAberto && (
            <div className={estilos.menuContratos}>
              <label className={estilos.opcaoTodos}>
                <input
                  type="checkbox"
                  checked={todosSelecionados}
                  onChange={() => setContratosSelecionados([])}
                />
                <strong>Todos os contratos autorizados</strong>
              </label>
              <div className={estilos.divisorDropdown} />
              <div className={estilos.listaOpcoesContratos}>
                {listaContratos.map((contrato) => {
                  const id = String(contrato.id);
                  return (
                    <label key={contrato.id} className={estilos.opcaoContrato}>
                      <input
                        type="checkbox"
                        checked={contratosSelecionados.includes(id)}
                        onChange={() => alternarContrato(id)}
                      />
                      <span>{contrato.numero ? `${contrato.numero} · ` : ""}{contrato.nome || contrato.descricao}</span>
                    </label>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        <label className={estilos.campo}>
          <span className={estilos.rotulo}><MapPin size={13} /> Lotação</span>
          <div className={estilos.selectComIcone}>
            <select
              className={estilos.controle}
              value={lotacaoSelecionada}
              onChange={(e) => setLotacaoSelecionada(e.target.value)}
            >
              <option value="">{lotacoesDisponiveis.length ? "Todas as lotações" : "Sem lotações"}</option>
              {lotacoesDisponiveis.map((l) => (
                <option key={l.id} value={l.id}>{l.nome || l.descricao}</option>
              ))}
            </select>
            <ChevronDown size={15} className={estilos.iconeSelect} />
          </div>
        </label>

        <label className={estilos.campo}>
          <span className={estilos.rotulo}><Clock size={13} /> Turno</span>
          <div className={estilos.selectComIcone}>
            <select
              className={estilos.controle}
              value={turnoSelecionado}
              onChange={(e) => setTurnoSelecionado(e.target.value)}
              disabled={!turnosDisponiveis.length}
            >
              <option value="">{turnosDisponiveis.length ? "Todos os turnos" : "Sem turnos"}</option>
              {turnosDisponiveis.map((t) => (
                <option key={t.id} value={t.id}>{t.descricao || t.nome || t.id}</option>
              ))}
            </select>
            <ChevronDown size={15} className={estilos.iconeSelect} />
          </div>
        </label>
      </div>

      {erroData && (
        <div className={estilos.erro}>
          <AlertTriangle size={16} />
          <span>{erroData}</span>
        </div>
      )}

      <div className={estilos.rodape}>
        <div className={estilos.resumoInfo}>
          Selecione o período de apuração e clique em <strong>Gerar relatório</strong>.
        </div>
        <button
          type="button"
          className={estilos.botaoBuscar}
          onClick={aoGerarRelatorio}
          disabled={carregando || Boolean(erroData)}
        >
          {carregando ? <span className={estilos.girador} /> : <Search size={16} />}
          <span>{carregando ? "Processando..." : "Gerar Relatório"}</span>
        </button>
      </div>
    </section>
  );
};
