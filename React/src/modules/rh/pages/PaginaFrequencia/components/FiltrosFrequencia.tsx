import type { useLogicaFrequencia } from '../useLogicaFrequencia';
type FrequencyModel = ReturnType<typeof useLogicaFrequencia>;
import React, { useState, useEffect, useRef, memo } from "react";
import { Filter, Search, ChevronDown, X } from "lucide-react";
import estilos from "../index.module.css";

export const FiltrosFrequencia = memo(
  ({
    eGestorOperacional,
    eGestorGestaoLocal,
    escoposDoUsuario = [],
    listaDeContratos = [],
    listaDeLotacoesDisponiveis = [],
    listaDeTurnosDisponiveis = [],
    contratosSelecionados = [], 
    aoMudarContratos,      
    lotacaoSelecionada = "",
    aoMudarLotacao,
    turnoSelecionado = "",
    aoMudarTurno,
    termoDeBuscaGlobal = "",
    aoMudarTermoDeBusca,
    aoLimparTodosOsFiltros,
    escopoBloqueado = false,
    justificativaRetificacao = "",
    aoMudarJustificativaRetificacao,
  }: { eGestorOperacional: FrequencyModel['auth']['eGestorOperacional']; eGestorGestaoLocal: FrequencyModel['auth']['eGestorGestaoLocal']; escoposDoUsuario?: FrequencyModel['filtros']['escoposLiberadosDoUsuario']; listaDeContratos?: FrequencyModel['filtros']['contratosBaixados']; listaDeLotacoesDisponiveis?: FrequencyModel['filtros']['listaLotacoesVisiveisAoUsuario']; listaDeTurnosDisponiveis?: FrequencyModel['filtros']['listaTurnosVisiveisAoUsuario']; contratosSelecionados?: FrequencyModel['filtros']['contratosSelecionadosPeloUsuario']; aoMudarContratos: FrequencyModel['acoes']['setContratosSelecionadosPeloUsuario']; lotacaoSelecionada?: FrequencyModel['filtros']['lotacaoSelecionadaPeloUsuario']; aoMudarLotacao: FrequencyModel['acoes']['setLotacaoSelecionadaPeloUsuario']; turnoSelecionado?: FrequencyModel['filtros']['turnoSelecionadoPeloUsuario']; aoMudarTurno: FrequencyModel['acoes']['setTurnoSelecionadoPeloUsuario']; termoDeBuscaGlobal?: FrequencyModel['filtros']['termoDeBuscaDigitado']; aoMudarTermoDeBusca: FrequencyModel['acoes']['setTermoDeBuscaDigitado']; aoLimparTodosOsFiltros: FrequencyModel['acoes']['aoRequisitarLimpezaDosFiltros']; escopoBloqueado?: boolean; justificativaRetificacao?: FrequencyModel['filtros']['justificativaRetificacao']; aoMudarJustificativaRetificacao: FrequencyModel['acoes']['setJustificativaRetificacao'] }) => {
    const [buscaEmTempoReal, setBuscaEmTempoReal] = useState(termoDeBuscaGlobal);
    const [dropdownContratoAberto, setDropdownContratoAberto] = useState(false);
    const dropdownRef = useRef<HTMLElementTagNameMap['div'] | null>(null);

    useEffect(() => {
      const aoClicarFora = (event: MouseEvent) => {
        if (dropdownRef.current && event.target instanceof Node && !dropdownRef.current.contains(event.target)) {
          setDropdownContratoAberto(false);
        }
      };
      document.addEventListener("mousedown", aoClicarFora);
      return () => document.removeEventListener("mousedown", aoClicarFora);
    }, []);

    useEffect(() => {
      const temporizadorDeBusca = setTimeout(() => {
        if (buscaEmTempoReal !== termoDeBuscaGlobal) {
          aoMudarTermoDeBusca(buscaEmTempoReal);
        }
      }, 500);
      return () => clearTimeout(temporizadorDeBusca);
    }, [buscaEmTempoReal, termoDeBuscaGlobal, aoMudarTermoDeBusca]);

    useEffect(() => {
      setBuscaEmTempoReal(termoDeBuscaGlobal);
    }, [termoDeBuscaGlobal]);

    useEffect(() => {
      if (escopoBloqueado) setDropdownContratoAberto(false);
    }, [escopoBloqueado]);

    const lidarComAlternanciaDeContrato = (idContratoStr: (string)) => {
      if (escopoBloqueado) return;
      aoMudarContratos((antigosContratos) => {
        const estadoGarantido = Array.isArray(antigosContratos) ? antigosContratos : [];
        if (estadoGarantido.includes(idContratoStr)) {
          return estadoGarantido.filter(id => id !== idContratoStr);
        }
        return [...estadoGarantido, idContratoStr];
      });
    };

    const todosSelecionados = contratosSelecionados.length === 0 || contratosSelecionados.length === listaDeContratos.length;
    
    let rotuloContrato: string | null = "-- Todos os Contratos --";
    if (!todosSelecionados) {
        if (contratosSelecionados.length === 1) {
            const c = listaDeContratos.find(c => String(c.id) === contratosSelecionados[0]);
            rotuloContrato = c ? (c.numeroContrato ? `${c.numeroContrato} - ${c.descricao || c.descricao}` : (c.descricao || c.descricao)) : "1 selecionado";
        } else {
            rotuloContrato = `${contratosSelecionados.length} contratos selecionados`;
        }
    }

    const limparBuscaInterna = () => {
      setBuscaEmTempoReal("");
      aoMudarTermoDeBusca("");
    };

    return (
      <>
        <div className={estilos.cartaoFiltros} data-tour="freq-filters">
          <div className={estilos.cabecalhoFiltros}>
            <div style={{ width: "100%" }}>
              <h3 className={estilos.tituloFiltro}>
                <Filter size={16} color="#3b82f6" />{" "}
                {eGestorGestaoLocal ? "Filtros da Gestão Local" : eGestorOperacional ? "Filtragem Operacional" : "Filtros e Busca"}
              </h3>
              <div className={estilos.gradeFiltros}>
                
                {/* DROPDOWN CUSTOMIZADO COM ESTILOS CENTRALIZADOS */}
                <div className={`${estilos.colunaFiltro3} ${estilos.dropdownContratosContainer}`} ref={dropdownRef}>
                  <div 
                    className={`${estilos.entradaSelecao} ${estilos.dropdownContratosTrigger} ${escopoBloqueado ? estilos.controleEscopoBloqueado : ""}`} 
                    role="button"
                    tabIndex={escopoBloqueado ? -1 : 0}
                    aria-label="Selecionar contratos"
                    aria-haspopup="listbox"
                    aria-expanded={dropdownContratoAberto}
                    aria-disabled={escopoBloqueado}
                    onClick={() => !escopoBloqueado && setDropdownContratoAberto((aberto) => !aberto)}
                    onKeyDown={(evento) => {
                      if (escopoBloqueado) return;
                      if (evento.key === "Enter" || evento.key === " ") {
                        evento.preventDefault();
                        setDropdownContratoAberto((aberto) => !aberto);
                      }
                      if (evento.key === "Escape") setDropdownContratoAberto(false);
                    }}
                  >
                    <span className={estilos.dropdownContratosTexto}>{rotuloContrato}</span>
                    <ChevronDown size={16} style={{ color: '#64748b' }} />
                  </div>
                  
                  {dropdownContratoAberto && (
                    <div className={estilos.dropdownContratosMenu}>
                      <label className={estilos.dropdownContratosItemHeader}>
                        <input 
                          type="checkbox" 
                          checked={todosSelecionados}
                          onChange={() => !escopoBloqueado && aoMudarContratos([])}
                          disabled={escopoBloqueado}
                          className={estilos.dropdownContratosCheckbox}
                        />
                        <span className={estilos.dropdownContratosLabelHeader}>Marcar/Desmarcar Todos</span>
                      </label>
                      {(listaDeContratos || []).map(contrato => {
                         const strId = String(contrato.id);
                         const selecionado = contratosSelecionados.includes(strId);
                         return (
                           <label key={contrato.id} className={estilos.dropdownContratosItem}>
                             <input 
                               type="checkbox" 
                               checked={selecionado}
                               onChange={() => lidarComAlternanciaDeContrato(strId)}
                               disabled={escopoBloqueado}
                               className={estilos.dropdownContratosCheckbox}
                             />
                             <span className={`${estilos.dropdownContratosLabelItem} ${selecionado ? estilos.dropdownContratosLabelItemAtivo : ""}`}>
                                {contrato.numeroContrato ? `${contrato.numeroContrato} - ` : ""}
                                {contrato.descricao || "Contrato " + contrato.id}
                             </span>
                           </label>
                         );
                      })}
                    </div>
                  )}
                </div>

                {(eGestorOperacional || escoposDoUsuario.length > 0) && (
                  <>
                    <div className={estilos.colunaFiltro3}>
                      <select
                        aria-label="Lotação"
                        value={lotacaoSelecionada}
                        disabled={escopoBloqueado}
                        onChange={(e) => {
                          aoMudarLotacao(e.target.value);
                          aoMudarTurno("");
                        }}
                        className={estilos.entradaSelecao}
                      >
                        <option value="">-- Selecione a Lotação --</option>
                        {listaDeLotacoesDisponiveis.map((lotacao) => (
                          <option key={lotacao.id} value={(lotacao.id) ?? undefined}>
                            {lotacao.nome || lotacao.nome}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className={estilos.colunaFiltro3}>
                      <select
                        aria-label="Turno"
                        value={turnoSelecionado}
                        onChange={(e) => aoMudarTurno(e.target.value)}
                        disabled={escopoBloqueado || !lotacaoSelecionada || listaDeTurnosDisponiveis.length === 0}
                        className={estilos.entradaSelecao}
                      >
                        <option value="">
                          {lotacaoSelecionada
                            ? listaDeTurnosDisponiveis.length > 0
                              ? "-- Selecione o Turno --"
                              : "Sem Turnos"
                            : "-- Aguardando Lotação --"}
                        </option>
                        {listaDeTurnosDisponiveis.map((turno) => (
                          <option key={turno.id} value={(turno.id) ?? undefined}>
                            {turno.descricao || turno.descricao || turno.id}
                          </option>
                        ))}
                      </select>
                    </div>
                  </>
                )}

                <div className={eGestorOperacional || escoposDoUsuario.length > 0 ? estilos.colunaFiltro3 : estilos.colunaFiltro9}>
                   <div className={estilos.envoltorioEntradaBusca}>
                    <Search className={estilos.iconeBusca} size={16} />
                    <input
                      type="text"
                      aria-label="Buscar colaborador por nome ou CPF"
                      placeholder="Buscar por Nome ou CPF..."
                      value={buscaEmTempoReal}
                      onChange={(e) => setBuscaEmTempoReal(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") aoMudarTermoDeBusca(buscaEmTempoReal);
                      }}
                      className={estilos.entradaBusca}
                    />
                    {/* BOTÃO DE LIMPAR (X) VISÍVEL APENAS QUANDO HÁ TEXTO */}
                    {buscaEmTempoReal && (
                      <button 
                        type="button" 
                        onClick={limparBuscaInterna}
                        className={estilos.botaoLimparBuscaInterno}
                        title="Limpar busca"
                      >
                        <X size={16} />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          {eGestorGestaoLocal && (
            <div className={estilos.blocoJustificativaRetificacao}>
              <label htmlFor="justificativa-retificacao-frequencia">Justificativa da retificação</label>
              <textarea
                id="justificativa-retificacao-frequencia"
                value={justificativaRetificacao}
                onChange={(evento) => aoMudarJustificativaRetificacao?.(evento.target.value)}
                disabled={escopoBloqueado}
                maxLength={500}
                placeholder="Obrigatória para qualquer gravação realizada pelo Gestor Local (mínimo 5 caracteres)."
              />
              <span>{String(justificativaRetificacao || "").trim().length}/500 · o backend exige ao menos 5 caracteres</span>
            </div>
          )}

          {(contratosSelecionados.length > 0 || lotacaoSelecionada || turnoSelecionado || termoDeBuscaGlobal) && (
            <div className={estilos.rodapeFiltros}>
              <span className={estilos.textoRodapeFiltros}>
                Exibindo resultados filtrados na base de dados.
              </span>
              <button
                type="button"
                disabled={escopoBloqueado}
                onClick={() => {
                  setBuscaEmTempoReal("");
                  aoLimparTodosOsFiltros();
                }}
                className={estilos.botaoLimparFiltros}
              >
                Limpar tudo
              </button>
            </div>
          )}
        </div>

        {!eGestorOperacional && escoposDoUsuario.length === 0 && (
          <div className={estilos.caixaAviso}>
            Seu usuário não possui nenhum vínculo de gestão cadastrado. Solicite à sua Gestão Local a definição da lotação e do turno.
          </div>
        )}

        {(!lotacaoSelecionada || !turnoSelecionado) && (
          <div className={estilos.caixaInfo}>
            <Filter size={18} />
            <span>Selecione a <b>Lotação</b> e o <b>Turno</b> acima para carregar o quadro completo de frequência.</span>
          </div>
        )}
      </>
    );
  }
);

FiltrosFrequencia.displayName = "FiltrosFrequencia";
