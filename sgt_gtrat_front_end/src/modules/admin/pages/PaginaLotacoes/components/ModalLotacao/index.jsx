import React from "react";
import { Building2, Check, Clock3, Info, MapPin } from "lucide-react";
import { Botao, CampoEntrada, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import {
  formatarGestorEstadual,
  formatarGestaoLocal,
} from "../../../../../../core/formatters/nomenclaturaGestao.js";
import { resumirHorariosDoTurno } from "../../resumoHorarios.js";
import estilos from "./index.module.css";

export const ModalLotacao = ({
  aberto,
  aoFechar,
  dados,
  aoMudarDados,
  aoSalvar,
  podeEscrever = true,
  acessoGlobal,
  gestoesLocais,
  turnos,
  aoAlternarTurno,
}) => {
  const turnosMarcados = new Set((dados.turnos || []).map((turno) => String(turno.id)));

  return (
    <JanelaModal
      isOpen={aberto}
      onClose={aoFechar}
      title={dados.id ? "Editar lotação" : "Nova lotação"}
      maxWidth="max-w-2xl"
    >
      <form onSubmit={aoSalvar} className={estilos.formulario}>
        <div className={estilos.apresentacao}>
          <span className={estilos.iconeApresentacao}><MapPin size={21} /></span>
          <div>
            <strong>{dados.nome?.trim() || "Unidade operacional"}</strong>
            <p>Defina o responsável, a localização e os turnos que poderão ser usados nesta lotação.</p>
          </div>
        </div>

        {acessoGlobal && (
          <div className={estilos.grupoCampo}>
            <label htmlFor="gestaoLocal-lotacao"><Building2 size={14} /> Gestão Local responsável</label>
            <select
              id="gestaoLocal-lotacao"
              value={dados.gestaoLocalId}
              onChange={(evento) => aoMudarDados("gestaoLocalId", evento.target.value)}
              required
            >
              <option value="">Selecione a Gestão Local...</option>
              {gestoesLocais.map((gestaoLocal) => (
                <option key={gestaoLocal.id} value={gestaoLocal.id}>
                  {formatarGestaoLocal(gestaoLocal)} · {formatarGestorEstadual(
                    gestaoLocal.superintendencia || gestaoLocal.superintendenciaNome,
                    gestaoLocal.uf || gestaoLocal.superintendencia?.uf,
                  )}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className={estilos.gradeCampos}>
          <CampoEntrada
            label="Nome da lotação (unidade)"
            value={dados.nome}
            onChange={(evento) => aoMudarDados("nome", evento.target.value)}
            placeholder="Ex.: CTE BENFICA"
            required
          />
          <CampoEntrada
            label="Município"
            value={dados.municipio}
            onChange={(evento) => aoMudarDados("municipio", evento.target.value)}
            placeholder="Ex.: Rio de Janeiro"
            required
          />
        </div>

        <section className={estilos.secaoTurnos}>
          <div className={estilos.cabecalhoTurnos}>
            <div>
              <span><Clock3 size={15} /> Turnos disponíveis</span>
              <p>São exibidos apenas os turnos da Gestão Local selecionada.</p>
            </div>
            <strong>{turnosMarcados.size} selecionado(s)</strong>
          </div>

          {turnos.length === 0 ? (
            <div className={estilos.semTurnos}>
              <Info size={18} />
              <span>
                {dados.gestaoLocalId
                  ? "Nenhum turno foi cadastrado para esta Gestão Local."
                  : "Selecione a Gestão Local para carregar os turnos."}
              </span>
            </div>
          ) : (
            <div className={estilos.listaSelecao}>
              {turnos.map((turno) => {
                const marcado = turnosMarcados.has(String(turno.id));
                return (
                  <label
                    key={turno.id}
                    className={`${estilos.itemTurno} ${marcado ? estilos.itemMarcado : ""}`}
                  >
                    <input
                      type="checkbox"
                      checked={marcado}
                      onChange={() => aoAlternarTurno(turno.id)}
                    />
                    <span className={estilos.checkVisual}>{marcado && <Check size={13} />}</span>
                    <span className={estilos.infoTurno}>
                      <strong>{turno.descricao}</strong>
                      <small>{resumirHorariosDoTurno(turno)}</small>
                    </span>
                  </label>
                );
              })}
            </div>
          )}
        </section>

        {dados.id && (
          <label className={estilos.statusAtivo}>
            <input type="checkbox" checked={dados.ativo !== false} onChange={(evento) => aoMudarDados("ativo", evento.target.checked)} disabled={!podeEscrever} />
            <span>Lotação ativa para novas operações</span>
            <small>A inativação preserva o histórico e pode ser bloqueada pelo Java quando houver programação futura.</small>
          </label>
        )}

        <div className={estilos.dica}>
          <Info size={14} /> A vinculação limita os turnos disponíveis aos empregados desta unidade.
        </div>

        <footer className={estilos.rodape}>
          <Botao variant="secondary" type="button" onClick={aoFechar}>Cancelar</Botao>
          <Botao type="submit">{dados.id ? "Salvar alterações" : "Cadastrar lotação"}</Botao>
        </footer>
      </form>
    </JanelaModal>
  );
};
