import React from "react";
import {
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  Clock3,
  Coffee,
  Copy,
  MoonStar,
  Edit3,
  Trash2,
} from "lucide-react";
import { JanelaModal, Botao, CampoEntrada } from "../../../../../../core/ui/EnvolucroDica";
import { CONFIG } from "../../../../../../core/config";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao.js";
import {
  analisarDiaDeTrabalho,
  formatarParaHoraDecimal,
  formatarParaHorasEMinutos,
  obterInformacoesDoDia,
} from "../../../../../../services/admin/ServicoDominioJornada.js";
import estilos from "./index.module.css";

export const ModalDeTurno = ({
  aberto,
  aoFechar,
  dadosDoFormulario = {},
  idEdicaoTurno,
  podeEscrever = true,
  analiseDoFormulario = {},
  aoMudarDadosGerais,
  aoMudarAtivo,
  acessoGlobal,
  listaDeGestoesLocais = [],
  aoMudarGestaoLocal,
  aoMudarConfiguracaoDoDia,
  aoReplicarHorariosAosOutrosDias,
  aoSalvarTurnoNoSistema,
  regrasAdicionais = [],
  carregandoRegras = false,
  formularioRegra = {},
  regraEmEdicao = null,
  aoMudarFormularioRegra,
  aoEditarRegra,
  aoSalvarRegra,
  aoExcluirRegra,
  aoCancelarEdicaoRegra,
}) => {
  if (!aberto) return null;

  const diasDoFormulario = Array.isArray(dadosDoFormulario?.dias)
    ? dadosDoFormulario.dias
    : [];
  const analiseSegura = {
    minutosSemanais: 0,
    quantidadeDiasTrabalho: 0,
    atravessaMeiaNoite: false,
    ...analiseDoFormulario,
  };

  // Verificação de limites globais para o status do cabeçalho
  const cargaSemanalHoras = Number(analiseSegura.minutosSemanais || 0) / 60;
  const excedeuMaximo = cargaSemanalHoras > CONFIG.JORNADA_REFERENCIA_HORAS;
  const abaixoMinimo = cargaSemanalHoras < CONFIG.JORNADA_LIMIAR_INFERIOR;
  const temErroDeLimite = excedeuMaximo || abaixoMinimo;

  let textoStatus = "Validado";
  if (excedeuMaximo) textoStatus = "Carga Alta";
  if (abaixoMinimo) textoStatus = "Carga Baixa";

  return (
    <JanelaModal
      isOpen={aberto}
      onClose={aoFechar}
      title={idEdicaoTurno ? "Editar Rotina de Trabalho" : "Nova Rotina de Trabalho"}
      maxWidth="max-w-4xl"
    >
      <div className={estilos.corpoModal}>
        
        {/* CABEÇALHO / RESUMO */}
        <div className={estilos.painelResumo}>
          <div className={estilos.resumoInfo}>
            <span className={estilos.tagResumo}>Configuração Semanal</span>
            <h2 className={estilos.tituloResumo}>{dadosDoFormulario.descricao?.trim() || "Nova Rotina"}</h2>
            <p className={estilos.textoResumo}>
              Ajuste os períodos diários e acompanhe a carga total calculada antes de salvar.
            </p>
          </div>
          <div className={estilos.gradeMetricas}>
            <div className={`${estilos.cartaoMetrica} ${temErroDeLimite ? estilos.metricaAlerta : ""}`}>
              <span className={estilos.rotuloMetrica}><CalendarClock size={12} className={estilos.iconeMetrica}/> Carga Semanal</span>
              <strong className={estilos.valorMetrica}>{formatarParaHorasEMinutos(analiseSegura.minutosSemanais)}</strong>
            </div>
            <div className={estilos.cartaoMetrica}>
              <span className={estilos.rotuloMetrica}><Clock3 size={12} className={estilos.iconeMetrica}/> Dias Ativos</span>
              <strong className={estilos.valorMetrica}>{analiseSegura.quantidadeDiasTrabalho}</strong>
            </div>
            <div className={`${estilos.cartaoMetrica} ${temErroDeLimite ? estilos.metricaAlerta : estilos.metricaSucesso}`}>
              <span className={estilos.rotuloMetrica}>Status</span>
              <strong className={estilos.valorMetrica}>{textoStatus}</strong>
            </div>
          </div>
        </div>

        {/* IDENTIFICAÇÃO ORGANIZACIONAL */}
        {acessoGlobal && (
          <div className={estilos.secaoGestaoLocal}>
            <label htmlFor="gestaoLocal-turno">Gestão Local responsável pelo turno</label>
            <select id="gestaoLocal-turno" value={dadosDoFormulario.gestaoLocalId || ""} onChange={(evento) => aoMudarGestaoLocal(evento.target.value)} required>
              <option value="">Selecione...</option>
              {(listaDeGestoesLocais || []).map((gestaoLocal) => (
                <option key={gestaoLocal.id} value={gestaoLocal.id}>{formatarGestaoLocal(gestaoLocal)} · {formatarGestorEstadual(gestaoLocal.superintendencia || gestaoLocal.superintendenciaNome, gestaoLocal.uf)}</option>
              ))}
            </select>
          </div>
        )}

        {/* NOME DO TURNO */}
        <div className={estilos.secaoNome}>
          <CampoEntrada
            label="Nome/Descrição do Turno"
            name="descricao"
            value={dadosDoFormulario.descricao}
            onChange={aoMudarDadosGerais}
            placeholder={`Ex.: Turno 1`}
            required
          />
          <p className={estilos.dicaCampo}>
            Use um nome claro para facilitar a identificação desta escala nas listagens de RH.
          </p>
        </div>

        {idEdicaoTurno && (
          <label className={estilos.statusAtivo}>
            <input
              type="checkbox"
              checked={dadosDoFormulario.ativo !== false}
              onChange={(evento) => aoMudarAtivo?.(evento.target.checked)}
              disabled={!podeEscrever}
            />
            <span>Turno ativo para novas operações</span>
            <small>Desativar preserva consultas históricas; o Java pode bloquear se houver programação futura.</small>
          </label>
        )}

        <div className={`${estilos.avisoNoturno} ${analiseSegura.atravessaMeiaNoite ? estilos.avisoNoturnoAtivo : ""}`}>
          <MoonStar size={18} />
          <span>
            Turnos noturnos são permitidos. Quando a saída for menor que a entrada
            (por exemplo, 22:00 às 06:00), o sistema considera a saída no dia seguinte.
          </span>
        </div>

        {/* TABELA DE DIAS */}
        <div className={estilos.secaoTabela}>
          <div className={estilos.cabecalhoTabela}>
            <div className={estilos.colDia}>Dia da Semana</div>
            <div className={estilos.colPeriodo}>1º Período</div>
            <div className={estilos.colPeriodo}>2º Período</div>
            <div className={estilos.colCarga}>Carga</div>
            <div className={estilos.colAcoes}>Ações</div>
          </div>
          
          <div className={estilos.corpoTabela}>
            {diasDoFormulario.map((diaAtual, indiceDoDia) => {
              const dadosDoDia = obterInformacoesDoDia(diaAtual.diaSemana);
              const analiseEspecificaDoDia = analisarDiaDeTrabalho(diaAtual);
              
              // Se a carga diária passar de 24h (ex: erro de digitação cruzando meia noite duplo), destacamos em vermelho
              const diaExcedeu24h = analiseEspecificaDoDia.minutos > (24 * 60);

              return (
                <div
                  key={diaAtual.diaSemana}
                  className={`${estilos.linhaTabela} ${!diaAtual.diaTrabalho ? estilos.linhaInativa : ""} ${diaExcedeu24h ? estilos.linhaComErro : ""}`}
                >
                  {/* Interruptor e Nome */}
                  <div className={estilos.colDia}>
                    <label className={estilos.toggleSwitch} aria-label={`Ativar ${dadosDoDia.nome}`}>
                      <input
                        type="checkbox"
                        checked={diaAtual.diaTrabalho}
                        onChange={(e) => aoMudarConfiguracaoDoDia(indiceDoDia, "diaTrabalho", e.target.checked)}
                      />
                      <span className={estilos.toggleSlider}></span>
                    </label>
                    <div className={estilos.infoDia}>
                      <strong>{dadosDoDia.nome}</strong>
                      <span>{diaAtual.diaTrabalho ? "Ativo" : "Folga"}</span>
                    </div>
                  </div>

                  {diaAtual.diaTrabalho ? (
                    <>
                      {/* 1º Período */}
                      <div className={estilos.colPeriodo}>
                        <div className={estilos.grupoHorario}>
                          <input
                            type="text"
                            placeholder="Início"
                            value={diaAtual.entrada1}
                            onChange={(e) => aoMudarConfiguracaoDoDia(indiceDoDia, "entrada1", e.target.value)}
                            maxLength={5}
                          />
                          <span className={estilos.separadorHorario}>às</span>
                          <input
                            type="text"
                            placeholder="Fim"
                            value={diaAtual.saida1}
                            onChange={(e) => aoMudarConfiguracaoDoDia(indiceDoDia, "saida1", e.target.value)}
                            maxLength={5}
                          />
                        </div>
                      </div>

                      {/* 2º Período */}
                      <div className={estilos.colPeriodo}>
                        <div className={estilos.grupoHorario}>
                          <input
                            type="text"
                            placeholder="Início"
                            value={diaAtual.entrada2}
                            onChange={(e) => aoMudarConfiguracaoDoDia(indiceDoDia, "entrada2", e.target.value)}
                            maxLength={5}
                          />
                          <span className={estilos.separadorHorario}>às</span>
                          <input
                            type="text"
                            placeholder="Fim"
                            value={diaAtual.saida2}
                            onChange={(e) => aoMudarConfiguracaoDoDia(indiceDoDia, "saida2", e.target.value)}
                            maxLength={5}
                          />
                        </div>
                      </div>

                      {/* Carga Calculada */}
                      <div className={estilos.colCarga}>
                        <span className={`${estilos.emblemaCarga} ${diaExcedeu24h ? estilos.cargaErro : estilos.cargaSucesso}`} title={diaExcedeu24h ? "A carga diária não pode ser superior a 24 horas" : ""}>
                          {diaExcedeu24h ? <AlertTriangle size={14} className={estilos.iconeCarga} /> : <CheckCircle2 size={14} className={estilos.iconeCarga} />}
                          {formatarParaHorasEMinutos(analiseEspecificaDoDia.minutos)}
                        </span>
                      </div>

                      {/* Ações (Replicar) */}
                      <div className={estilos.colAcoes}>
                        <button
                          type="button"
                          onClick={() => aoReplicarHorariosAosOutrosDias(indiceDoDia)}
                          className={estilos.botaoCopiar}
                          title="Replicar estes horários para os demais dias"
                        >
                          <Copy size={16} />
                        </button>
                      </div>
                    </>
                  ) : (
                    <div className={estilos.diaFolgaOcupador}>
                      <Coffee size={16} /> Sem jornada de trabalho definida
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {idEdicaoTurno && (
          <section className={estilos.secaoRegras} aria-labelledby="titulo-regras-adicionais">
            <div className={estilos.cabecalhoRegras}>
              <div>
                <h3 id="titulo-regras-adicionais">Regras adicionais</h3>
                <p>Janelas, percentuais e outras regras associadas especificamente a esta jornada.</p>
              </div>
              <span>{regrasAdicionais.length} regra(s)</span>
            </div>

            {carregandoRegras ? (
              <p className={estilos.estadoRegras}>Carregando regras adicionais...</p>
            ) : regrasAdicionais.length === 0 ? (
              <p className={estilos.estadoRegras}>Nenhuma regra adicional cadastrada.</p>
            ) : (
              <div className={estilos.listaRegras}>
                {regrasAdicionais.map((regra) => (
                  <div key={regra.id} className={estilos.itemRegra}>
                    <div>
                      <strong>{regra.tipo}</strong>
                      <span>
                        {regra.inicio && regra.fim ? `${regra.inicio}–${regra.fim}` : "Sem janela"}
                        {regra.percentual != null ? ` · ${regra.percentual}%` : ""}
                        {regra.ativo === false ? " · inativa" : ""}
                      </span>
                    </div>
                    {podeEscrever && (
                      <div className={estilos.acoesRegra}>
                        <button type="button" onClick={() => aoEditarRegra?.(regra)} aria-label={`Editar regra ${regra.tipo}`}><Edit3 size={15} /></button>
                        <button type="button" onClick={() => aoExcluirRegra?.(regra.id)} aria-label={`Excluir regra ${regra.tipo}`}><Trash2 size={15} /></button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {podeEscrever && (
              <div className={estilos.formularioRegra}>
                <label>Tipo<input value={formularioRegra.tipo || ""} onChange={(e) => aoMudarFormularioRegra?.("tipo", e.target.value)} placeholder="Ex.: ADICIONAL_NOTURNO" /></label>
                <label>Início<input type="time" value={formularioRegra.inicio || ""} onChange={(e) => aoMudarFormularioRegra?.("inicio", e.target.value)} /></label>
                <label>Fim<input type="time" value={formularioRegra.fim || ""} onChange={(e) => aoMudarFormularioRegra?.("fim", e.target.value)} /></label>
                <label>Percentual<input type="number" min="0" step="0.01" value={formularioRegra.percentual ?? ""} onChange={(e) => aoMudarFormularioRegra?.("percentual", e.target.value)} /></label>
                <label>Ordem<input type="number" min="0" value={formularioRegra.ordem ?? 0} onChange={(e) => aoMudarFormularioRegra?.("ordem", e.target.value)} /></label>
                <label className={estilos.regraAtiva}><input type="checkbox" checked={formularioRegra.ativo !== false} onChange={(e) => aoMudarFormularioRegra?.("ativo", e.target.checked)} /> Ativa</label>
                <div className={estilos.botoesRegra}>
                  {regraEmEdicao && <Botao variant="secondary" type="button" onClick={aoCancelarEdicaoRegra}>Cancelar edição</Botao>}
                  <Botao type="button" onClick={aoSalvarRegra}>{regraEmEdicao ? "Atualizar regra" : "Adicionar regra"}</Botao>
                </div>
              </div>
            )}
          </section>
        )}

        {/* RODAPÉ DO MODAL */}
        <div className={estilos.rodapeModal}>
          <div className={estilos.infoRodape}>
            <strong className={temErroDeLimite ? "text-red-500" : ""}>{formatarParaHorasEMinutos(analiseDoFormulario.minutosSemanais)}</strong>
            <span>calculadas na rotina</span>
          </div>
          <div className={estilos.botoesRodape}>
            <Botao variant="secondary" onClick={aoFechar}>Descartar</Botao>
            <Botao onClick={aoSalvarTurnoNoSistema}>
              {idEdicaoTurno ? "Atualizar Escala" : "Cadastrar Escala"}
            </Botao>
          </div>
        </div>
        
      </div>
    </JanelaModal>
  );
};
