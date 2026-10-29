import React, { useCallback, useMemo, useState } from "react";
import {
  UserRound,
  BriefcaseBusiness,
  Phone,
  Gift,
  Landmark,
  FileText,
  X,
  Save,
  Loader2,
  ClipboardCheck,
} from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica.jsx";
import { useLogicaFormulario } from "./useLogicaFormulario";
import {
  SecaoCamposEssenciais,
  SecaoInformacoesPessoais,
  SecaoInformacoesTrabalhistas,
  SecaoTelefonesComunicacao,
  SecaoBeneficiosGerais,
  SecaoLogradouroEDependentes,
  SecaoInformacoesBancarias,
  SecaoMatrizDeContratos,
} from "./components/SecoesFormulario";
import estilos from "./index.module.css";

const formatarCpf = (valor = "") => {
  const digitos = String(valor || "").replace(/\D/g, "").slice(0, 11);

  if (digitos.length <= 3) return digitos;
  if (digitos.length <= 6) {
    return `${digitos.slice(0, 3)}.${digitos.slice(3)}`;
  }
  if (digitos.length <= 9) {
    return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6)}`;
  }

  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
};

export const FormularioEmpregado = ({
  id,
  onSuccess: aoTerSucesso,
  onCancel: aoCancelar,
}) => {
  const { estado, derivados, acoes } = useLogicaFormulario({
    idUsuario: id,
    aoTerSucesso,
  });

  const [abaAtiva, setAbaAtiva] = useState("essenciais");

  const dadosPessoaisFormatados = useMemo(
    () => ({
      ...estado.dadosFormulario,
      cpf: formatarCpf(estado.dadosFormulario.cpf),
    }),
    [estado.dadosFormulario],
  );

  const aoMudarDadosPessoais = useCallback(
    (evento) => {
      if (evento.target.name !== "cpf") {
        acoes.aoLidarComMudancaEntrada(evento);
        return;
      }

      acoes.aoLidarComMudancaEntrada({
        target: {
          name: evento.target.name,
          value: formatarCpf(evento.target.value),
          type: evento.target.type,
          checked: evento.target.checked,
        },
      });
    },
    [acoes.aoLidarComMudancaEntrada],
  );

  /*
   * A aba de contratos deve existir tanto no cadastro quanto na edição.
   * Antes ela era incluída apenas quando `id` existia, impedindo que o
   * vínculo contratual fosse selecionado durante o novo cadastro.
   */
  const abas = useMemo(
    () => [
      { id: "essenciais", label: "Campos Obrigatórios", icon: ClipboardCheck },
      { id: "pessoais", label: "Dados Pessoais", icon: UserRound },
      {
        id: "trabalhistas",
        label: "Vínculo Trabalhista",
        icon: BriefcaseBusiness,
      },
      { id: "contatos", label: "Contatos e Endereço", icon: Phone },
      { id: "beneficios", label: "Benefícios", icon: Gift },
      { id: "bancarias", label: "Dados Bancários", icon: Landmark },
      {
        id: "contratos",
        label: "Vínculo do Contrato",
        icon: FileText,
      },
    ],
    [],
  );

  const renderizarConteudoAba = () => {
    switch (abaAtiva) {
      case "essenciais":
        return (
          <>
            <SecaoCamposEssenciais
              dadosDoFormulario={dadosPessoaisFormatados}
              aoMudarDadosPessoais={aoMudarDadosPessoais}
              aoMudarValores={acoes.aoLidarComMudancaEntrada}
              aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
              validarHabilitacao={acoes.verificarCampoEstaHabilitado}
              identificadorUsuario={id}
              listasCarregadas={estado.listasAuxiliares}
              listaDeTurnosVisiveis={derivados.turnosDisponiveisFiltrados}
              funcaoRotuloTurno={acoes.exibirDescricaoTurnoComCargaHoraria}
            />
            <SecaoMatrizDeContratos
              dadosDoFormulario={estado.dadosFormulario}
              acaoRealizarAlternanciaContrato={acoes.aoAlternarSelecaoDoContrato}
              listasCarregadas={estado.listasAuxiliares}
              bloqueioInatividadeStatus={derivados.statusAtualEstaInativo}
            />
          </>
        );

      case "pessoais":
        return (
          <SecaoInformacoesPessoais
            dadosDoFormulario={dadosPessoaisFormatados}
            aoMudarValores={aoMudarDadosPessoais}
            aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
            validarHabilitacao={acoes.verificarCampoEstaHabilitado}
            identificadorUsuario={id}
          />
        );

      case "trabalhistas":
        return (
          <SecaoInformacoesTrabalhistas
            dadosDoFormulario={estado.dadosFormulario}
            aoMudarValores={acoes.aoLidarComMudancaEntrada}
            aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
            validarHabilitacao={acoes.verificarCampoEstaHabilitado}
            identificadorUsuario={id}
            listasCarregadas={estado.listasAuxiliares}
            listaDeTurnosVisiveis={derivados.turnosDisponiveisFiltrados}
            funcaoRotuloTurno={acoes.exibirDescricaoTurnoComCargaHoraria}
            dadosFinanceirosContrato={derivados.resumoValorHoraContrato}
            previsaoSalarioCalculado={derivados.valorSalarioPrevisto}
          />
        );

      case "contatos":
        return (
          <>
            <SecaoTelefonesComunicacao
              dadosDoFormulario={estado.dadosFormulario}
              situacaoDeEdicaoTelefones={estado.listaTelefonesEditaveis}
              acaoAtualizarTextoTelefone={acoes.aoMudarValorTelefone}
              acaoGerenciarListaTelefones={acoes.gerenciarListaDeTelefones}
              acaoAlternarBotaoEdicaoTelefone={
                acoes.alternarModoEdicaoDoTelefone
              }
            />

            <SecaoLogradouroEDependentes
              dadosDoFormulario={estado.dadosFormulario}
              aoMudarValores={acoes.aoLidarComMudancaEntrada}
              aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
              validarHabilitacao={acoes.verificarCampoEstaHabilitado}
              identificadorUsuario={id}
            />
          </>
        );

      case "beneficios":
        return (
          <SecaoBeneficiosGerais
            dadosDoFormulario={estado.dadosFormulario}
            aoMudarValores={acoes.aoLidarComMudancaEntrada}
            aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
            validarHabilitacao={acoes.verificarCampoEstaHabilitado}
            identificadorUsuario={id}
          />
        );

      case "bancarias":
        return (
          <SecaoInformacoesBancarias
            dadosDoFormulario={estado.dadosFormulario}
            aoMudarValores={acoes.aoLidarComMudancaEntrada}
            aoAlternarTravas={acoes.aoAlternarBloqueioCampo}
            validarHabilitacao={acoes.verificarCampoEstaHabilitado}
            identificadorUsuario={id}
            listasCarregadas={estado.listasAuxiliares}
            filtragemBancariaExibida={derivados.agenciasBancariasFiltradas}
          />
        );

      case "contratos":
        return (
          <SecaoMatrizDeContratos
            dadosDoFormulario={estado.dadosFormulario}
            acaoRealizarAlternanciaContrato={
              acoes.aoAlternarSelecaoDoContrato
            }
            listasCarregadas={estado.listasAuxiliares}
            bloqueioInatividadeStatus={derivados.statusAtualEstaInativo}
          />
        );

      default:
        return null;
    }
  };

  return (
    <div className={estilos.containerGlobal}>
      <form
        onSubmit={acoes.aoSubmeterFormularioServidor}
        className={estilos.layoutPrincipal}
      >
        <nav className={estilos.sidebar}>
          <div className={estilos.sidebarHeader}>
            <h2 className={estilos.sidebarTitle}>
              {id ? "Edição de Registro" : "Novo Cadastro"}
            </h2>
          </div>

          <div className={estilos.sidebarNav}>
            {abas.map((aba) => {
              const Icone = aba.icon;

              return (
                <button
                  key={aba.id}
                  type="button"
                  className={`${estilos.tabLateral} ${
                    abaAtiva === aba.id ? estilos.tabAtiva : ""
                  }`}
                  onClick={() => setAbaAtiva(aba.id)}
                >
                  <div className={estilos.tabContent}>
                    <Icone size={18} className={estilos.tabIcone} />
                    <span>{aba.label}</span>
                  </div>

                  {abaAtiva === aba.id && (
                    <div className={estilos.indicadorAtivo} />
                  )}
                </button>
              );
            })}
          </div>
        </nav>

        <div className={estilos.areaPrincipal}>
          <div className={estilos.conteudoRolavel}>
            <div className={estilos.animacaoEntrada}>
              {renderizarConteudoAba()}
            </div>

            <div className={estilos.espacadorRodape} />
          </div>

          <div className={estilos.rodapeAcoes}>
            <Botao
              variant="secondary"
              type="button"
              onClick={aoCancelar}
              disabled={estado.indicadorCarregamento}
              className={estilos.btnDescartar}
            >
              <X size={16} />
              Cancelar
            </Botao>

            <Botao
              type="submit"
              disabled={estado.indicadorCarregamento}
              className={estilos.btnGravar}
            >
              {estado.indicadorCarregamento ? (
                <>
                  <Loader2 size={16} className={estilos.iconeGirando} />
                  Salvando...
                </>
              ) : (
                <>
                  <Save size={16} />
                  {id ? "Salvar Alterações" : "Concluir Cadastro"}
                </>
              )}
            </Botao>
          </div>
        </div>
      </form>
    </div>
  );
};
