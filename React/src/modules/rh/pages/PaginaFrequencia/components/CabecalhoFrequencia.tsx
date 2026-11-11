import type { useLogicaFrequencia } from '../useLogicaFrequencia';
type FrequencyModel = ReturnType<typeof useLogicaFrequencia>;
import React, { memo, useEffect, useState } from "react";
import {
  Building2,
  Calendar,
  CalendarX,
  ChevronLeft,
  ChevronRight,
  Lightbulb,
  LoaderCircle,
  MapPin,
  ShieldCheck,
  Unlock,
  UserCog,
  UsersRound,
} from "lucide-react";
import { Botao, EnvolucroDica } from "../../../../../core/ui/EnvolucroDica.tsx";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../core/formatters/nomenclaturaGestao.ts";
import { interpretarDataHoraNoFusoNegocio } from "../../../../../core/formatters/dataHoraNegocio.ts";
import estilos from "../index.module.css";

export const CabecalhoFrequencia = memo(({
  eAdministradorGlobal,
  eGestorGestaoLocal,
  eGestorOperacional,
  usuarioLogado,
  dataReferencia,
  aoAlterarDataReferencia,
  statusDaData,
  estaSalvando,
  progressoEnvio,
  edicaoBloqueada,
  estaCarregando,
  listaDeFrequencia,
  aoMudarDataPorBotoes,
  aoIniciarSalvamentoGlobal,
  aoAbrirTourGuiado,
}: { eAdministradorGlobal: FrequencyModel['auth']['isAdmin']; eGestorGestaoLocal: FrequencyModel['auth']['eGestorGestaoLocal']; eGestorOperacional: FrequencyModel['auth']['eGestorOperacional']; usuarioLogado: FrequencyModel['auth']['user']; dataReferencia: FrequencyModel['estado']['dataReferenciaAtiva']; aoAlterarDataReferencia: FrequencyModel['acoes']['setDataReferenciaAtiva']; statusDaData: FrequencyModel['estado']['statusDaDataValidada']; estaSalvando: FrequencyModel['estado']['indicadorSalvamento']; progressoEnvio: FrequencyModel['estado']['progressoEnvio']; edicaoBloqueada: boolean; estaCarregando: FrequencyModel['estado']['indicadorCarregamento']; listaDeFrequencia: FrequencyModel['estado']['listaDaFrequencia']; aoMudarDataPorBotoes: FrequencyModel['acoes']['aoAcionarMudarDataPeloCabecalho']; aoIniciarSalvamentoGlobal: FrequencyModel['acoes']['invocarGatilhoDeDespachoAoServidorGlobal']; aoAbrirTourGuiado: () => void }) => {
  const [tempoRestanteRetroativo, setTempoRestanteRetroativo] = useState("");

  useEffect(() => {
    if (usuarioLogado?.isRetroactiveUnlocked && usuarioLogado?.retroactiveExpireAt && !eGestorOperacional) {
      const atualizar = () => {
        const diferenca = interpretarDataHoraNoFusoNegocio(usuarioLogado.retroactiveExpireAt) - Date.now();
        if (!Number.isFinite(diferenca)) return setTempoRestanteRetroativo("");
        if (diferenca <= 0) return setTempoRestanteRetroativo("Expirado");
        const horas = Math.floor(diferenca / 3600000);
        const minutos = Math.floor((diferenca % 3600000) / 60000);
        const segundos = Math.floor((diferenca % 60000) / 1000);
        setTempoRestanteRetroativo(`${String(horas).padStart(2, "0")}:${String(minutos).padStart(2, "0")}:${String(segundos).padStart(2, "0")}`);
      };
      atualizar();
      const intervalo = setInterval(atualizar, 1000);
      return () => clearInterval(intervalo);
    }
    setTempoRestanteRetroativo("");
    return undefined;
  }, [usuarioLogado, eGestorOperacional]);

  const nomeGestaoLocalBruto = usuarioLogado?.gestaoLocalNome || usuarioLogado?.gestaoLocal?.nome;
  const nomeGestaoLocal = nomeGestaoLocalBruto
    ? formatarGestaoLocal(usuarioLogado?.gestaoLocal || nomeGestaoLocalBruto)
    : "";
  const nomeSuperintendenciaBruto = usuarioLogado?.superintendenciaNome || usuarioLogado?.superintendencia?.nome;
  const nomeSuperintendencia = nomeSuperintendenciaBruto || usuarioLogado?.uf
    ? formatarGestorEstadual(usuarioLogado?.superintendencia || nomeSuperintendenciaBruto, usuarioLogado?.uf)
    : "";
  const nomeLotacao = usuarioLogado?.lotacaoNome || usuarioLogado?.lotacao;
  const total = Number(progressoEnvio?.total || 0);
  const processados = Number(progressoEnvio?.processados || 0);
  const percentual = total > 0 ? Math.min(100, Math.floor((processados * 100) / total)) : 0;
  const rotuloSalvar = estaSalvando
    ? total > 0 ? `ENVIANDO ${percentual}%` : "ENVIANDO..."
    : "SALVAR ALTERAÇÕES";

  return (
    <>
      <div className={estilos.cartaoCabecalho} data-tour="freq-header">
        <div className={estilos.cabecalhoEsquerda}>
          <div className={estilos.envoltorioTitulo}>
            <h1 className={estilos.titulo}>
              Controle de Frequência
              {eAdministradorGlobal && (
                <span className={estilos.emblemaAdmin}><UserCog size={12} /> ADMINISTRAÇÃO TÉCNICA</span>
              )}
              {eGestorGestaoLocal && (
                <span className={estilos.emblemaAdmin}><UsersRound size={12} /> GESTÃO LOCAL</span>
              )}
              {usuarioLogado?.isRetroactiveUnlocked && !eGestorOperacional && (
                <span className={estilos.emblemaRetroativo}>
                  <Unlock size={12} /> RETROATIVO LIBERADO{tempoRestanteRetroativo ? ` (${tempoRestanteRetroativo})` : ""}
                </span>
              )}
            </h1>
            <p className={estilos.subtitulo}>
              Frequência diária canônica por vínculo contratual; janelas e bloqueios são validados pelo servidor.
            </p>
            <div className={estilos.identificacaoEscopoFrequencia}>
              <span><Building2 size={13} /> {nomeGestaoLocal || "Administração técnica"}{nomeSuperintendencia ? ` · ${nomeSuperintendencia}` : ""}</span>
              {nomeLotacao && <span><MapPin size={13} /> {nomeLotacao}</span>}
            </div>
          </div>
          <button type="button" onClick={aoAbrirTourGuiado} className={estilos.botaoDicas}>
            <Lightbulb size={14} className={estilos.iconeDica} /> Dicas da tela
          </button>
        </div>

        <div className={estilos.acoesCabecalho}>
          <div className={estilos.grupoNavegacaoData}>
            <EnvolucroDica tooltip="Ir para o dia anterior" position="bottom" wrapperClass={estilos.envoltorioDicaData}>
              <button type="button" aria-label="Ir para o dia anterior" disabled={estaSalvando} onClick={() => aoMudarDataPorBotoes(-1)} className={`${estilos.botaoNavegacaoData} ${estilos.botaoNavEsquerda}`}>
                <ChevronLeft size={18} />
              </button>
            </EnvolucroDica>
            <EnvolucroDica tooltip="Selecione uma data específica" position="bottom" wrapperClass={estilos.envoltorioDicaDataFlex}>
              <div className={estilos.envoltorioEntradaData}>
                <div className={estilos.caixaIconeData}><Calendar size={16} className={!statusDaData?.valid ? estilos.iconeInvalido : estilos.iconeValido} /></div>
                <input
                  type="date"
                  aria-label="Data de referência da frequência"
                  value={dataReferencia || ""}
                  onChange={(evento) => aoAlterarDataReferencia(evento.target.value)}
                  disabled={estaSalvando}
                  className={`${estilos.entradaData} ${!statusDaData?.valid ? estilos.entradaDataInvalida : ""}`}
                />
              </div>
            </EnvolucroDica>
            <EnvolucroDica tooltip="Ir para o próximo dia" position="bottom" wrapperClass={estilos.envoltorioDicaData}>
              <button type="button" aria-label="Ir para o próximo dia" disabled={estaSalvando} onClick={() => aoMudarDataPorBotoes(1)} className={`${estilos.botaoNavegacaoData} ${estilos.botaoNavDireita}`}>
                <ChevronRight size={18} />
              </button>
            </EnvolucroDica>
          </div>

          <Botao
            onClick={aoIniciarSalvamentoGlobal}
            variant="primary"
            className={!statusDaData?.valid ? estilos.botaoSalvarInvalido : ""}
            disabled={edicaoBloqueada || estaSalvando || estaCarregando || !Array.isArray(listaDeFrequencia) || listaDeFrequencia.length === 0 || !statusDaData?.valid}
            tooltip={!statusDaData?.valid ? "Data futura não permitida" : "Enviar somente alterações explícitas pelo contrato canônico de frequência"}
            tooltipPosition="bottom"
          >
            <div data-tour="freq-save" className={estilos.conteudoBotaoSalvar}>
              {estaSalvando ? <LoaderCircle size={18} className={estilos.iconeGirando} /> : <ShieldCheck size={18} className={estilos.iconeBotaoSalvar} />}
              {rotuloSalvar}
            </div>
          </Botao>
        </div>
      </div>

      {estaSalvando && progressoEnvio && (
        <div className={estilos.faixaFechamento} role="status" aria-live="polite">
          <div className={estilos.resumoFechamento}>
            <span className={estilos.iconeFechamento}><LoaderCircle size={18} className={estilos.iconeGirando} /></span>
            <div className={estilos.textoFechamento}>
              <strong>Envio canônico em andamento</strong>
              <span>
                {processados.toLocaleString("pt-BR")} de {total.toLocaleString("pt-BR")} registro(s)
                {progressoEnvio.totalLotes > 1 ? ` · lote ${progressoEnvio.loteAtual}/${progressoEnvio.totalLotes}` : ""}
                {" · sem retry automático"}
              </span>
            </div>
          </div>
          <div className={estilos.barraFechamento} aria-label={`Progresso ${percentual}%`}><span style={{ width: `${percentual}%` }} /></div>
          <span className={estilos.percentualFechamento}>{percentual}%</span>
        </div>
      )}

      {!statusDaData?.valid && (
        <div className={estilos.faixaErro}>
          <div className={estilos.conteudoFaixaErro}>
            <CalendarX size={24} className={estilos.iconeFaixaErro} />
            <div>
              <h3 className={estilos.tituloFaixaErro}>Data indisponível</h3>
              <p className={estilos.textoFaixaErro}>Data futura não permitida. Regras de retroatividade são validadas pelo backend.</p>
            </div>
          </div>
          <div className={estilos.emblemaFaixaErro}>SOMENTE LEITURA</div>
        </div>
      )}
    </>
  );
});
CabecalhoFrequencia.displayName = "CabecalhoFrequencia";
