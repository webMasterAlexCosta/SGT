import type { ObservedFailure } from "../../../../core/http/falhaObservada";
import { observarFalha } from '../../../../core/http/falhaObservada';
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Save, Settings2 } from "lucide-react";
import { notificar } from "../../../../core/events";
import { EstruturaPagina } from "../../../../core/ui/EstruturaPagina.tsx";
import { Botao, Cartao } from "../../../../core/ui/EnvolucroDica.tsx";
import { ServicoConfiguracaoSistema } from "../../../../services/admin/ServicoConfiguracaoSistema.ts";
import estilos from "./index.module.css";

type ConfigurationDefinition = { chave: string; rotulo: string; descricao: string } & ({ tipo: "number"; min: number; max: number; step: number } | { tipo: "time"; min?: undefined; max?: undefined; step?: undefined });
const DEFINICOES: readonly ConfigurationDefinition[] = Object.freeze([
  {
    chave: "HORAS_DIA_TRABALHO",
    rotulo: "Horas do dia de trabalho",
    descricao: "Quantidade de horas que compõem 1 dia de trabalho padrão.",
    tipo: "number",
    min: 1,
    max: 24,
    step: 1,
  },
  {
    chave: "HORA_INICIO_NOTURNO",
    rotulo: "Início do adicional noturno",
    descricao: "Horário em que se inicia a contagem do adicional noturno.",
    tipo: "time",
  },
  {
    chave: "MINUTOS_HORA_NOTURNA",
    rotulo: "Minutos da hora noturna",
    descricao: "Minutos que equivalem a 1 hora contábil no período noturno.",
    tipo: "number",
    min: 0.1,
    max: 60,
    step: 0.1,
  },
]);

const CHAVES_EDITAVEIS = new Set(DEFINICOES.map((item) => item.chave));

const construirEstado = (lista: import("../../../../contracts/java").ConfiguracaoSistemaDTO[] = []) => {
  const mapa = new Map(
    (Array.isArray(lista) ? lista : [])
      .filter((item) => CHAVES_EDITAVEIS.has(item?.chave || ""))
      .map((item) => [item.chave, item]),
  );
  return Object.fromEntries(DEFINICOES.map((definicao) => [
    definicao.chave,
    String(mapa.get(definicao.chave)?.valor ?? ""),
  ]));
};

const horaValida = (valor: (string)) => {
  const correspondencia = /^(\d{2}):(\d{2})$/.exec(String(valor || ""));
  if (!correspondencia) return false;
  const hora = Number(correspondencia[1]);
  const minuto = Number(correspondencia[2]);
  return hora >= 0 && hora <= 23 && minuto >= 0 && minuto <= 59;
};

const validarValor = (definicao: ConfigurationDefinition, valor: (string)) => {
  if (definicao.tipo === "time") return horaValida(valor);
  const numero = Number(valor);
  return Number.isFinite(numero) && numero >= definicao.min && numero <= definicao.max;
};

export const PaginaConfiguracoesSistema = () => {
  const [valores, setValores] = useState(() => construirEstado());
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<ObservedFailure | null>(null);
  const [ultimaLeitura, setUltimaLeitura] = useState<(import("../../../../contracts/java").ConfiguracaoSistemaDTO[])>([]);

  const carregar = useCallback(async (options = {}) => {
    setCarregando(true);
    setErro(null);
    try {
      const resposta = await ServicoConfiguracaoSistema.buscarConfiguracoes(options);
      const lista = Array.isArray(resposta) ? resposta : [];
      setUltimaLeitura(lista);
      setValores(construirEstado(lista));
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      if (!falha?.canceled) setErro(falha);
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    const controlador = new AbortController();
    void carregar({ signal: controlador.signal });
    return () => controlador.abort();
  }, [carregar]);

  const chavesSomenteLeitura = useMemo(
    () => ultimaLeitura.filter((item) => item?.chave && !CHAVES_EDITAVEIS.has(item.chave)),
    [ultimaLeitura],
  );

  const salvar = async () => {
    const invalida = DEFINICOES.find((definicao) => !validarValor(definicao, valores[definicao.chave]));
    if (invalida) {
      notificar("error", `Revise o valor de “${invalida.rotulo}”.`);
      return;
    }

    const payload = DEFINICOES.map((definicao) => ({
      chave: definicao.chave,
      valor: String(valores[definicao.chave]).trim(),
      descricao: definicao.descricao,
    }));

    setSalvando(true);
    try {
      await ServicoConfiguracaoSistema.salvarConfiguracoes(payload);
      notificar("success", "Configurações administrativas atualizadas.");
      await carregar();
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      if (!falha?.canceled) notificar("error", falha?.message || "Não foi possível salvar as configurações.");
    } finally {
      setSalvando(false);
    }
  };

  return (
    <EstruturaPagina
      titulo="Configurações do sistema"
      subtitulo="Parâmetros administrativos persistidos no Java. A tela não mantém cópia autoritativa no navegador."
      carregando={carregando}
      erro={erro}
      aoTentarNovamente={carregar}
      acoes={(
        <Botao onClick={salvar} disabled={salvando || carregando}>
          <Save size={16} aria-hidden="true" /> {salvando ? "Salvando..." : "Salvar configurações"}
        </Botao>
      )}
    >
      <div className={estilos.grade}>
        {DEFINICOES.map((definicao) => (
          <Cartao key={definicao.chave} className={estilos.cartao}>
            <div className={estilos.cabecalhoCampo}>
              <span className={estilos.icone}><Settings2 size={18} aria-hidden="true" /></span>
              <div>
                <strong>{definicao.rotulo}</strong>
                <code>{definicao.chave}</code>
              </div>
            </div>
            <p>{definicao.descricao}</p>
            <label className={estilos.campo}>
              <span>Valor</span>
              <input
                type={definicao.tipo}
                min={definicao.min}
                max={definicao.max}
                step={definicao.step}
                value={valores[definicao.chave] ?? ""}
                onChange={(evento) => setValores((atual) => ({
                  ...atual,
                  [definicao.chave]: evento.target.value,
                }))}
                disabled={salvando}
                required
              />
            </label>
          </Cartao>
        ))}
      </div>

      {chavesSomenteLeitura.length > 0 && (
        <aside className={estilos.aviso} role="note">
          <strong>Outros parâmetros retornados pelo servidor</strong>
          <p>
            {chavesSomenteLeitura.length} {chavesSomenteLeitura.length === 1 ? "chave foi mantida" : "chaves foram mantidas"} fora do formulário,
            porque o serviço Java atual não autoriza sua gravação por este endpoint.
          </p>
        </aside>
      )}
    </EstruturaPagina>
  );
};
