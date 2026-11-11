import React, { useEffect, useMemo, useState } from "react";
import {
  Building2,
  Crown,
  Info,
  MapPinned,
  ShieldCheck,
  UserRoundCog,
  X,
} from "lucide-react";
import { CONFIG } from "../../../../../core/config";
import { formatarMatricula } from "../../../../../core/formatters/matricula.ts";
import estilos from "./ModalTeste.module.css";

const UFS_BRASIL = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
  "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
  "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
];

const montarContaEstadual = (uf: (string), tipo: (string), siglaGestaoLocal = "") => {
  const eGestaoLocal = tipo === "GESTOR_LOCAL";
  const indiceUf = UFS_BRASIL.indexOf(uf) + 1;
  const usuario = eGestaoLocal
    ? `gestorLocal${siglaGestaoLocal.charAt(0)}${siglaGestaoLocal.slice(1).toLowerCase()}${uf}`
    : (uf === "RJ" ? "gestorEstadual" : `gestorEstadual${uf}`);
  const prefixoMatricula = { GTRAT: "92", GEDIS: "93", GETRA: "94" }[siglaGestaoLocal] || "95";

  return {
    // Sincronizado com os usuários e as matrículas do DataInitializer.java.
    usuario,
    matricula: `${eGestaoLocal ? prefixoMatricula : "91"}${String(indiceUf).padStart(6, "0")}`,
    perfil: eGestaoLocal ? `${siglaGestaoLocal}/${uf} · Gestor local` : `Gestor estadual/${uf}`,
    escopo: eGestaoLocal
      ? `Gestão dos acessos e da operação local em ${uf}`
      : `Gestão estadual de ${uf}`,
    senha: CONFIG.TEST_GESTOR_PASSWORD,
    Icone: eGestaoLocal ? UserRoundCog : Building2,
  };
};

const CONTAS_NACIONAIS = [
  {
    // Corrigido de "presidente" para "gestor" para refletir o BD
    usuario: "gestor",
    matricula: "90000002",
    perfil: "Gestor nacional",
    escopo: "Visão nacional consolidada por gestores estaduais",
    senha: CONFIG.TEST_GLOBAL_PASSWORD,
    Icone: Crown,
  },
  {
    usuario: "adm",
    matricula: "90000001",
    perfil: "Administrador",
    escopo: "Administração técnica completa do sistema",
    senha: CONFIG.TEST_GLOBAL_PASSWORD,
    Icone: ShieldCheck,
  },
];

export const ModalTeste = ({ aoFechar, aoPreencherUsuario }: { aoFechar: (() => void); aoPreencherUsuario: (dados: { matricula: string; senha: string }) => void }) => {
  const [ufSelecionada, setUfSelecionada] = useState("RJ");

  useEffect(() => {
    const overflowAnterior = document.body.style.overflow;
    const lidarComTecla = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") aoFechar();
    };

    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", lidarComTecla);

    return () => {
      document.body.style.overflow = overflowAnterior;
      document.removeEventListener("keydown", lidarComTecla);
    };
  }, [aoFechar]);

  const contasEstaduais = useMemo(
    () => [
      montarContaEstadual(ufSelecionada, "SE"),
      montarContaEstadual(ufSelecionada, "GESTOR_LOCAL", "GTRAT"),
      montarContaEstadual(ufSelecionada, "GESTOR_LOCAL", "GEDIS"),
      montarContaEstadual(ufSelecionada, "GESTOR_LOCAL", "GETRA"),
    ],
    [ufSelecionada],
  );

  const renderizarConta = ({ usuario, matricula, perfil, escopo, senha, Icone }: ReturnType<typeof montarContaEstadual>) => (
    <button
      key={usuario}
      type="button"
      onClick={() => aoPreencherUsuario({ matricula, senha })}
      className={estilos.cartaoUsuario}
    >
      <span className={estilos.iconeUsuario}><Icone size={19} /></span>
      <span className={estilos.dadosUsuario}>
        <strong>{usuario}</strong>
        <span>{perfil}</span>
        <span className={estilos.matriculaUsuario}>
          Matrícula {formatarMatricula(matricula)}
        </span>
        <small>{escopo}</small>
      </span>
      <span className={estilos.acaoUsuario}>Usar conta</span>
    </button>
  );

  return (
    <div
      className={estilos.sobreposicaoModal}
      role="presentation"
      onMouseDown={(evento) => evento.target === evento.currentTarget && aoFechar()}
    >
      <section
        className={estilos.cartaoModal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-acesso-teste"
      >
        <header className={estilos.cabecalhoModal}>
          <div className={estilos.identificacaoCabecalho}>
            <div className={estilos.iconeCabecalho}><Info size={22} /></div>
            <div>
              <span>Ambiente de testes / homologação</span>
              <h2 id="titulo-acesso-teste">Escolha um perfil de acesso</h2>
              <p>
                
                Gestor nacional, Administrador, Gestor estadual e Gestão Local.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={aoFechar}
            className={estilos.botaoFechar}
            aria-label="Fechar acessos de teste"
          >
            <X size={20} />
          </button>
        </header>

        <div className={estilos.corpoModal}>
          <section className={estilos.grupoPerfis}>
            <div className={estilos.tituloGrupo}>
              <strong>Gestão nacional</strong>
              <span>2 contas</span>
            </div>
            <div className={estilos.gradeUsuarios}>
              {CONTAS_NACIONAIS.map(renderizarConta)}
            </div>
          </section>

          <section className={estilos.grupoPerfis}>
            <div className={estilos.cabecalhoEstadual}>
              <div className={estilos.tituloGrupoSemMargem}>
                <strong>Gestão estadual</strong>
                <span>Gestor estadual e três Gestões Locais da UF selecionada</span>
              </div>
              <label className={estilos.seletorUf}>
                <MapPinned size={16} />
                <span>UF</span>
                <select
                  value={ufSelecionada}
                  onChange={(evento) => setUfSelecionada(evento.target.value)}
                  aria-label="Selecionar unidade federativa para acesso de teste"
                >
                  {UFS_BRASIL.map((uf) => (
                    <option key={uf} value={uf}>{uf}</option>
                  ))}
                </select>
              </label>
            </div>
            <div className={estilos.gradeUsuarios}>
              {contasEstaduais.map(renderizarConta)}
            </div>
          </section>
        </div>

        <footer className={estilos.rodapeModal}>
          <div className={estilos.avisoSenhas}>
            <ShieldCheck size={16} />
            <span>
              Qualquer falhar ou sugestões entre em contato com  anaclaudiacarneiro@correios.com.br ou alexc@correios.com.br
            </span>
          </div>
          <button type="button" onClick={aoFechar}>Fechar</button>
        </footer>
      </section>
    </div>
  );
};
