import React, { useEffect, useMemo, useState } from "react";
import { JanelaModal, Botao } from "../../../../../core/ui/EnvolucroDica.jsx";
import { ServicoEmpregado } from "../../../../../services/rh/ServicoEmpregado.js";
import {
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  FileText,
  MapPin,
  RefreshCw,
  Users,
} from "lucide-react";
import { normalizarHistoricoContrato } from "../../../../../services/rh/ServicoDominioEmpregado.js";
import estilos from "./ModalHistoricoContrato.module.css";

const formatarDataHora = (valor) => {
  if (!valor) return "-";
  const texto = String(valor);
  const possuiFuso = /Z$|[+-]\d{2}:?\d{2}$/.test(texto);
  const data = new Date(possuiFuso ? texto : `${texto}Z`);
  if (Number.isNaN(data.getTime())) return texto;

  return data.toLocaleString("pt-BR", {
    timeZone: "America/Sao_Paulo",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const formatarData = (valor) => {
  if (!valor) return "-";
  const data = new Date(`${String(valor).slice(0, 10)}T12:00:00`);
  if (Number.isNaN(data.getTime())) return String(valor);
  return data.toLocaleDateString("pt-BR");
};

const formatarDiaSemana = (valor) => {
  const nomes = {
    MONDAY: "Segunda-feira",
    TUESDAY: "Terça-feira",
    WEDNESDAY: "Quarta-feira",
    THURSDAY: "Quinta-feira",
    FRIDAY: "Sexta-feira",
    SATURDAY: "Sábado",
    SUNDAY: "Domingo",
  };
  return nomes[String(valor || "").toUpperCase()] || "-";
};

const CampoDetalhe = ({ rotulo, valor, Icone }) => (
  <div className={estilos.campoDetalhe}>
    <span className={estilos.rotuloDetalhe}>
      {Icone && <Icone size={13} aria-hidden="true" />}
      {rotulo}
    </span>
    <strong className={estilos.valorDetalhe} title={String(valor || "-")}>
      {valor ?? "-"}
    </strong>
  </div>
);

export const ModalHistoricoContrato = ({ isOpen, onClose, empregado }) => {
  const [historico, setHistorico] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [revisao, setRevisao] = useState(0);

  useEffect(() => {
    if (!isOpen || !empregado?.id) {
      setHistorico([]);
      setErro("");
      return undefined;
    }

    const controlador = new AbortController();
    setCarregando(true);
    setErro("");

    ServicoEmpregado.buscarHistoricoContrato(empregado.id, {
      signal: controlador.signal,
    })
      .then((resposta) => {
        if (controlador.signal.aborted) return;
        setHistorico(normalizarHistoricoContrato(resposta));
      })
      .catch((falha) => {
        if (controlador.signal.aborted || falha?.name === "CanceledError") return;
        console.error("Falha ao carregar histórico contratual:", falha);
        setErro(falha?.message || "Não foi possível carregar o histórico contratual.");
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCarregando(false);
      });

    return () => controlador.abort();
  }, [isOpen, empregado?.id, revisao]);

  const resumo = useMemo(() => {
    const atual = historico.find((item) => item.ativo);
    return {
      total: historico.length,
      atual,
      encerrados: historico.filter((item) => !item.ativo).length,
    };
  }, [historico]);

  return (
    <JanelaModal
      isOpen={isOpen}
      onClose={onClose}
      title={`Histórico de contratos — ${empregado?.nome || "Colaborador"}`}
      maxWidth="max-w-4xl"
    >
      <div className={estilos.conteinerGeral}>
        <div className={estilos.resumoHistorico}>
          <div>
            <span>Vínculos registrados</span>
            <strong>{resumo.total}</strong>
          </div>
          <div>
            <span>Contrato atual</span>
            <strong>{resumo.atual?.numeroContrato || "Sem vínculo"}</strong>
          </div>
          <div>
            <span>Encerrados</span>
            <strong>{resumo.encerrados}</strong>
          </div>
        </div>

        {carregando ? (
          <div className={estilos.mensagemCarregando} role="status">
            <RefreshCw size={18} className={estilos.iconeGirando} />
            Carregando histórico...
          </div>
        ) : erro ? (
          <div className={estilos.mensagemErro} role="alert">
            <p>{erro}</p>
            <button type="button" onClick={() => setRevisao((valor) => valor + 1)}>
              Tentar novamente
            </button>
          </div>
        ) : historico.length === 0 ? (
          <div className={estilos.mensagemVazia}>
            Nenhum histórico de vínculo encontrado.
          </div>
        ) : (
          <div className={estilos.linhaTempo}>
            {historico.map((item) => (
              <article key={item.id} className={estilos.itemLinhaTempo}>
                <div
                  className={`${estilos.marcadorLinhaTempo} ${
                    item.ativo ? estilos.marcadorAtivo : estilos.marcadorInativo
                  }`}
                  aria-hidden="true"
                />

                <div
                  className={`${estilos.cartaoContrato} ${
                    item.ativo ? estilos.cartaoAtivo : estilos.cartaoInativo
                  }`}
                >
                  <header className={estilos.cabecalhoCartao}>
                    <div className={estilos.identificacaoContrato}>
                      <span className={estilos.iconeContrato} aria-hidden="true">
                        <FileText size={18} />
                      </span>
                      <div>
                        <span className={estilos.numeroContrato}>{item.numeroContrato}</span>
                        <h4>{item.nomeContrato}</h4>
                      </div>
                    </div>
                    <div className={estilos.emblemasContrato}>
                      <span className={item.contratoAtivo ? estilos.emblemaContratoAtivo : estilos.emblemaContratoInativo}>
                        {item.contratoAtivo ? "Contrato ativo" : "Contrato inativo"}
                      </span>
                      {item.ativo && <span className={estilos.emblemaAtual}>VÍNCULO ATUAL</span>}
                    </div>
                  </header>

                  <div className={estilos.blocoDescricao}>
                    <span>Descrição</span>
                    <p>{item.descricaoContrato}</p>
                  </div>

                  <div className={estilos.gradeDetalhes}>
                    <CampoDetalhe rotulo="Tipo" valor={item.tipoContrato} Icone={FileText} />
                    <CampoDetalhe rotulo="Gestão Local" valor={item.gestaoLocalNome} Icone={Building2} />
                    <CampoDetalhe
                      rotulo="Gestor estadual"
                      valor={`${item.superintendenciaNome}${item.uf !== "-" ? ` — ${item.uf}` : ""}`}
                      Icone={MapPin}
                    />
                    <CampoDetalhe
                      rotulo="Efetivo previsto"
                      valor={item.efetivoPrevisto ?? "-"}
                      Icone={Users}
                    />
                    <CampoDetalhe
                      rotulo="Efetivo atual"
                      valor={item.efetivoAtual ?? "-"}
                      Icone={Users}
                    />
                    <CampoDetalhe
                      rotulo="Saldo de efetivo"
                      valor={item.saldoEfetivo ?? "-"}
                      Icone={Users}
                    />
                    <CampoDetalhe
                      rotulo="Ocupação"
                      valor={item.percentualOcupacao == null ? "-" : `${item.percentualOcupacao}%`}
                      Icone={Users}
                    />
                    <CampoDetalhe
                      rotulo="Início do contrato"
                      valor={formatarData(item.contratoDataInicio)}
                      Icone={Calendar}
                    />
                    <CampoDetalhe
                      rotulo="Fim do contrato"
                      valor={item.contratoDataFim ? formatarData(item.contratoDataFim) : "Sem término"}
                      Icone={Calendar}
                    />
                    <CampoDetalhe
                      rotulo="Dia reduzido"
                      valor={formatarDiaSemana(item.diaSemanaReduzido)}
                      Icone={Clock}
                    />
                    <CampoDetalhe
                      rotulo="Horas do dia reduzido"
                      valor={item.horasDiaReduzido ?? "-"}
                      Icone={Clock}
                    />
                  </div>

                  <div className={estilos.periodoVinculo}>
                    <CampoDetalhe
                      rotulo="Início do vínculo"
                      valor={formatarDataHora(item.dataInicio)}
                      Icone={Calendar}
                    />
                    <CampoDetalhe
                      rotulo="Fim do vínculo"
                      valor={item.dataFim ? formatarDataHora(item.dataFim) : "Vigente"}
                      Icone={item.dataFim ? Clock : CheckCircle2}
                    />
                  </div>

                  <div className={estilos.observacaoTexto}>
                    <span>Observação do histórico</span>
                    <p>{item.observacao}</p>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}

        <div className={estilos.rodapeBotoes}>
          <Botao variant="secondary" onClick={onClose}>Fechar</Botao>
        </div>
      </div>
    </JanelaModal>
  );
};
