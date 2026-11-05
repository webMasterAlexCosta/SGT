import React, { memo } from "react";
import { BriefcaseBusiness, CheckCircle2, Plus, Star, Users } from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica.jsx";
import estilos from "./index.module.css";

const Resumo = ({ Icone, valor, rotulo, classe = "" }) => (
  <div className={`${estilos.cartaoResumo} ${classe}`} title={`${valor} ${rotulo}`}>
    <span className={estilos.iconeResumo} aria-hidden="true">
      <Icone size={17} />
    </span>
    <div className={estilos.textoResumo}>
      <strong>{Number(valor || 0).toLocaleString("pt-BR")}</strong>
      <span>{rotulo}</span>
    </div>
  </div>
);

export const CabecalhoEmpregados = memo(({
  totalElementos,
  quantidadeContratos,
  quantidadeAtivos,
  quantidadeFavoritos,
  abrirModalNovo,
  podeEditar = true,
}) => (
  <header className={estilos.cabecalhoPagina}>
    <div className={estilos.blocoTitulo}>
      <span className={estilos.sobretitulo}>Gestão de terceirizados</span>
      <h1 className={estilos.tituloPagina}>Cadastro de empregados</h1>
      <p className={estilos.descricaoPagina}>
        Consulte, filtre e atualize colaboradores e seus vínculos em uma única visão.
      </p>
    </div>

    <div className={estilos.areaDireita}>
      <div className={estilos.resumosCabecalho} aria-label="Resumo da tela">
        <Resumo Icone={BriefcaseBusiness} valor={quantidadeContratos} rotulo="contratos" />
        <Resumo Icone={Users} valor={totalElementos} rotulo="colaboradores" />
        <Resumo Icone={CheckCircle2} valor={quantidadeAtivos} rotulo="ativos na tela" classe={estilos.resumoAtivo} />
        <Resumo Icone={Star} valor={quantidadeFavoritos} rotulo="salvos" classe={estilos.resumoFavorito} />
      </div>

      {podeEditar && (
        <Botao onClick={abrirModalNovo} className={estilos.botaoNovoCorreios} tooltip="Cadastrar empregado">
          <Plus size={18} strokeWidth={3} />
          <span>Novo empregado</span>
        </Botao>
      )}
    </div>
  </header>
));

CabecalhoEmpregados.displayName = "CabecalhoEmpregados";
