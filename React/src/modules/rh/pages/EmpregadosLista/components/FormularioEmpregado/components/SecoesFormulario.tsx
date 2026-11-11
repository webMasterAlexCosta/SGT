import type { useLogicaFormulario } from '../useLogicaFormulario';
type FormModel = ReturnType<typeof useLogicaFormulario>;
import React, { memo, useDeferredValue, useEffect, useMemo, useState } from "react";
import { User, Briefcase, MapPin, Phone, Wallet, Building2, FileText, Check, Pencil, Trash2, Plus, AlertTriangle, ClipboardCheck, Search } from "lucide-react";
import { CampoEntrada, Seletor } from "../../../../../../../core/ui/EnvolucroDica.tsx";
import { ComponenteCampoBloqueado, ComponenteCartaoDeSecao, ComponenteInterruptorLogico } from "./ComponentesBase";
import { formatarMoedaBrasileira } from "../utilitarios";
import estilos from "./SecoesFormulario.module.css";

export const SecaoInformacoesPessoais = memo(({ dadosDoFormulario, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={User} tituloDaSecao="Informações Pessoais do Colaborador">
    <div className={estilos.gradeColunas12}>
      <div className={estilos.colunaSpan8}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("nome")} aoAlternarTrava={() => aoAlternarTravas("nome")} rotuloDoCampo="Nome Completo"><CampoEntrada label="Nome Completo" name="nome" value={dadosDoFormulario.nome || ""} onChange={aoMudarValores} required disabled={!validarHabilitacao("nome")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("matricula")} aoAlternarTrava={() => aoAlternarTravas("matricula")} rotuloDoCampo="Matrícula de Registro (opcional)"><CampoEntrada label="Matrícula (opcional)" name="matricula" value={dadosDoFormulario.matricula || ""} onChange={aoMudarValores} maxLength={30} ajuda="Campo opcional, com limite de 30 caracteres." disabled={!validarHabilitacao("matricula")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cpf")} aoAlternarTrava={() => aoAlternarTravas("cpf")} rotuloDoCampo="Cadastro de Pessoa Física (CPF)"><CampoEntrada label="CPF" name="cpf" value={dadosDoFormulario.cpf || ""} onChange={aoMudarValores} required disabled={!validarHabilitacao("cpf")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("rg")} aoAlternarTrava={() => aoAlternarTravas("rg")} rotuloDoCampo="Registro Geral (RG)"><CampoEntrada label="RG" name="rg" value={dadosDoFormulario.rg || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("rg")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("ctpsSerie")} aoAlternarTrava={() => aoAlternarTravas("ctpsSerie")} rotuloDoCampo="Série da CTPS"><CampoEntrada label="CTPS (Série)" name="ctpsSerie" value={dadosDoFormulario.ctpsSerie || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("ctpsSerie")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("nitPisPasep")} aoAlternarTrava={() => aoAlternarTravas("nitPisPasep")} rotuloDoCampo="Número NIT/PIS/PASEP"><CampoEntrada label="NIT / PIS" name="nitPisPasep" value={dadosDoFormulario.nitPisPasep || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("nitPisPasep")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("dataNascimento")} aoAlternarTrava={() => aoAlternarTravas("dataNascimento")} rotuloDoCampo="Data de Nascimento"><CampoEntrada type="date" label="Nascimento" name="dataNascimento" value={dadosDoFormulario.dataNascimento || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("dataNascimento")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("formacaoEscolar")} aoAlternarTrava={() => aoAlternarTravas("formacaoEscolar")} rotuloDoCampo="Nível de Formação Escolar"><CampoEntrada label="Formação" name="formacaoEscolar" value={dadosDoFormulario.formacaoEscolar || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("formacaoEscolar")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan8}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("email")} aoAlternarTrava={() => aoAlternarTravas("email")} rotuloDoCampo="Correio Eletrônico (E-mail)"><CampoEntrada type="email" label="E-mail" name="email" value={dadosDoFormulario.email || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("email")} /></ComponenteCampoBloqueado></div>
    </div>
  </ComponenteCartaoDeSecao>
));

SecaoInformacoesPessoais.displayName = "SecaoInformacoesPessoais";

export const SecaoCamposEssenciais = memo(({ dadosDoFormulario, aoMudarDadosPessoais, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario, listasCarregadas, listaDeTurnosVisiveis, funcaoRotuloTurno }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarDadosPessoais: FormModel['acoes']['aoLidarComMudancaEntrada']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null; listasCarregadas: FormModel['estado']['listasAuxiliares']; listaDeTurnosVisiveis: FormModel['derivados']['turnosDisponiveisFiltrados']; funcaoRotuloTurno: FormModel['acoes']['exibirDescricaoTurnoComCargaHoraria'] }) => (
  <>
    <div className={estilos.avisoCamposEssenciais} role="note">
      <ClipboardCheck size={20} aria-hidden="true" />
      <div>
        <strong>Filtro de campos obrigatórios</strong>
        <span>
          Conforme solicitado em reunião este é o filtro
          </span>
      </div>
    </div>

    <ComponenteCartaoDeSecao iconeDecorativo={ClipboardCheck} tituloDaSecao="Campos obrigatórios do colaborador">
      <div className={estilos.gradeColunas12}>
        <div className={estilos.colunaSpan8}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("nome")} aoAlternarTrava={() => aoAlternarTravas("nome")} rotuloDoCampo="Nome Completo">
            <CampoEntrada label="Nome Completo" name="nome" value={dadosDoFormulario.nome || ""} onChange={aoMudarDadosPessoais} required disabled={!validarHabilitacao("nome")} />
          </ComponenteCampoBloqueado>
        </div>
        <div className={estilos.colunaSpan4}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cpf")} aoAlternarTrava={() => aoAlternarTravas("cpf")} rotuloDoCampo="Cadastro de Pessoa Física (CPF)">
            <CampoEntrada label="CPF" name="cpf" value={dadosDoFormulario.cpf || ""} onChange={aoMudarDadosPessoais} required inputMode="numeric" maxLength={14} placeholder="000.000.000-00" disabled={!validarHabilitacao("cpf")} />
          </ComponenteCampoBloqueado>
        </div>
        <div className={estilos.colunaSpan4}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("dataAdmissao")} aoAlternarTrava={() => aoAlternarTravas("dataAdmissao")} rotuloDoCampo="Data Oficial de Admissão">
            <CampoEntrada type="date" label="Admissão" name="dataAdmissao" value={dadosDoFormulario.dataAdmissao || ""} onChange={aoMudarValores} required disabled={!validarHabilitacao("dataAdmissao")} />
          </ComponenteCampoBloqueado>
        </div>
        <div className={estilos.colunaSpan4}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("lotacaoId")} aoAlternarTrava={() => aoAlternarTravas("lotacaoId")} rotuloDoCampo="Centro de Custos/Lotação">
            <Seletor label="Lotação Operacional" required name="lotacaoId" value={dadosDoFormulario.lotacaoId || ""} onChange={aoMudarValores} options={listasCarregadas.lotacoes} disabled={!validarHabilitacao("lotacaoId")} />
          </ComponenteCampoBloqueado>
        </div>
        <div className={estilos.colunaSpan4}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cargoId")} aoAlternarTrava={() => aoAlternarTravas("cargoId")} rotuloDoCampo="Cargo ou Função">
            <Seletor label="Cargo Ocupado" required name="cargoId" value={dadosDoFormulario.cargoId || ""} onChange={aoMudarValores} options={listasCarregadas.cargos} disabled={!validarHabilitacao("cargoId")} />
          </ComponenteCampoBloqueado>
        </div>
        <div className={estilos.colunaSpan12}>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("jornadaId")} aoAlternarTrava={() => aoAlternarTravas("jornadaId")} rotuloDoCampo="Jornada/Turno de Trabalho">
            <Seletor label="Turno de Trabalho" required name="jornadaId" value={dadosDoFormulario.jornadaId || ""} onChange={aoMudarValores} disabled={!dadosDoFormulario.lotacaoId || !validarHabilitacao("jornadaId")} options={(listaDeTurnosVisiveis.map(turnoBase => ({ id: turnoBase.id, nome: funcaoRotuloTurno(turnoBase) }))) ?? undefined} />
          </ComponenteCampoBloqueado>
        </div>
      </div>
    </ComponenteCartaoDeSecao>
  </>
));

SecaoCamposEssenciais.displayName = "SecaoCamposEssenciais";

export const SecaoInformacoesTrabalhistas = memo(({ dadosDoFormulario, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario, listasCarregadas, listaDeTurnosVisiveis, funcaoRotuloTurno, dadosFinanceirosContrato, previsaoSalarioCalculado }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null; listasCarregadas: FormModel['estado']['listasAuxiliares']; listaDeTurnosVisiveis: FormModel['derivados']['turnosDisponiveisFiltrados']; funcaoRotuloTurno: FormModel['acoes']['exibirDescricaoTurnoComCargaHoraria']; dadosFinanceirosContrato: FormModel['derivados']['resumoValorHoraContrato']; previsaoSalarioCalculado: number | null }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={Briefcase} tituloDaSecao="Vínculos e Posição na Empresa">
    <div className={estilos.gradeColunas4}>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("dataAdmissao")} aoAlternarTrava={() => aoAlternarTravas("dataAdmissao")} rotuloDoCampo="Data Oficial de Admissão"><CampoEntrada type="date" label="Admissão" name="dataAdmissao" value={dadosDoFormulario.dataAdmissao || ""} onChange={aoMudarValores} required disabled={!validarHabilitacao("dataAdmissao")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("lotacaoId")} aoAlternarTrava={() => aoAlternarTravas("lotacaoId")} rotuloDoCampo="Centro de Custos/Lotação"><Seletor label="Lotação Operacional" required name="lotacaoId" value={dadosDoFormulario.lotacaoId || ""} onChange={aoMudarValores} options={listasCarregadas.lotacoes} disabled={!validarHabilitacao("lotacaoId")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("jornadaId")} aoAlternarTrava={() => aoAlternarTravas("jornadaId")} rotuloDoCampo="Jornada/Turno de Trabalho"><Seletor label="Turno de Trabalho" required name="jornadaId" value={dadosDoFormulario.jornadaId || ""} onChange={aoMudarValores} disabled={!dadosDoFormulario.lotacaoId || !validarHabilitacao("jornadaId")} options={(listaDeTurnosVisiveis.map(turnoBase => ({ id: turnoBase.id, nome: funcaoRotuloTurno(turnoBase) }))) ?? undefined} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cargoId")} aoAlternarTrava={() => aoAlternarTravas("cargoId")} rotuloDoCampo="Cargo ou Função"><Seletor label="Cargo Ocupado" required name="cargoId" value={dadosDoFormulario.cargoId || ""} onChange={aoMudarValores} options={listasCarregadas.cargos} disabled={!validarHabilitacao("cargoId")} /></ComponenteCampoBloqueado>
    </div>
    {identificadorUsuario && (
      <div className={estilos.caixaResumo}>
        <div className={estilos.itemResumo}><span className={estilos.rotuloResumo}>Remuneração por Hora</span><div className={estilos.valorResumo}>{dadosFinanceirosContrato.valor ? formatarMoedaBrasileira(dadosFinanceirosContrato.valor) : <span className={estilos.valorResumoVazio}>-</span>}</div></div>
        <div className={estilos.itemResumoComBorda}><span className={estilos.rotuloResumo}>Estimativa de Salário Base</span><div className={estilos.valorResumoVerde}>{previsaoSalarioCalculado ? formatarMoedaBrasileira(previsaoSalarioCalculado) : <span className={estilos.valorResumoVazio}>-</span>}</div></div>
        <div className={estilos.itemResumoStatus}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("statusId")} aoAlternarTrava={() => aoAlternarTravas("statusId")} rotuloDoCampo="Situação Atual do Colaborador"><Seletor label="Status Atual no Sistema" name="statusId" value={dadosDoFormulario.statusId || ""} onChange={aoMudarValores} options={listasCarregadas.statuses} disabled={!validarHabilitacao("statusId")} /></ComponenteCampoBloqueado></div>
      </div>
    )}
  </ComponenteCartaoDeSecao>
));

SecaoInformacoesTrabalhistas.displayName = "SecaoInformacoesTrabalhistas";

export const SecaoTelefonesComunicacao = memo(({ dadosDoFormulario, situacaoDeEdicaoTelefones, acaoAtualizarTextoTelefone, acaoGerenciarListaTelefones, acaoAlternarBotaoEdicaoTelefone }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; situacaoDeEdicaoTelefones: boolean[]; acaoAtualizarTextoTelefone: FormModel['acoes']['aoMudarValorTelefone']; acaoGerenciarListaTelefones: FormModel['acoes']['gerenciarListaDeTelefones']; acaoAlternarBotaoEdicaoTelefone: FormModel['acoes']['alternarModoEdicaoDoTelefone'] }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={Phone} tituloDaSecao="Contatos Telefônicos">
    <div className={estilos.listaContatos}>
      {dadosDoFormulario.telefones.map((textoDoTelefone, indiceDoItem) => (
        <div key={indiceDoItem} className={estilos.linhaTelefone}>
          <div className={estilos.envoltorioEntradaTelefone}>
             <CampoEntrada placeholder="(XX) XXXXX-XXXX" value={textoDoTelefone || ""} onChange={eventoDeMudanca => acaoAtualizarTextoTelefone(indiceDoItem, eventoDeMudanca.target.value)} readOnly={!situacaoDeEdicaoTelefones[indiceDoItem]} />
          </div>
          <button type="button" onClick={() => acaoAlternarBotaoEdicaoTelefone(indiceDoItem)} className={`${estilos.botaoTelefone} ${situacaoDeEdicaoTelefones[indiceDoItem] ? estilos.botaoTelefoneEditando : estilos.botaoTelefoneEditar}`}>
             {situacaoDeEdicaoTelefones[indiceDoItem] ? <Check size={18} /> : <Pencil size={18} />}
          </button>
          <button type="button" onClick={() => acaoGerenciarListaTelefones("remover", indiceDoItem)} disabled={dadosDoFormulario.telefones.length === 1 && indiceDoItem === 0} className={`${estilos.botaoTelefone} ${estilos.botaoTelefoneRemover}`}>
             <Trash2 size={18} />
          </button>
        </div>
      ))}
      <div className={estilos.envoltorioBotaoAdicionarTelefone}>
         <button type="button" onClick={() => acaoGerenciarListaTelefones("adicionar")} className={estilos.botaoAdicionarTelefone}>
           <Plus size={18} /> Adicionar número alternativo
         </button>
      </div>
    </div>
  </ComponenteCartaoDeSecao>
));

SecaoTelefonesComunicacao.displayName = "SecaoTelefonesComunicacao";

export const SecaoBeneficiosGerais = memo(({ dadosDoFormulario, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={Wallet} tituloDaSecao="Informações de FGTS e Benefícios">
    <div className={estilos.listaBeneficios}>
      <div className={estilos.caixaBeneficio}>
        <div className={estilos.cabecalhoBeneficio}>
          <span className={estilos.tituloBeneficio}>Depósitos do Fundo de Garantia (FGTS)</span>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("optanteFgts")} aoAlternarTrava={() => aoAlternarTravas("optanteFgts")} rotuloDoCampo="Chave de Opção do FGTS"><ComponenteInterruptorLogico nomeAtributo="optanteFgts" estaMarcado={dadosDoFormulario.optanteFgts} aoAlternarMudanca={aoMudarValores} rotuloTexto="Colaborador é Optante" estaDesabilitado={!validarHabilitacao("optanteFgts")} /></ComponenteCampoBloqueado>
        </div>
        <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("dataOpcaoFgts")} aoAlternarTrava={() => aoAlternarTravas("dataOpcaoFgts")} rotuloDoCampo="Data Efetiva da Opção"><CampoEntrada type="date" label="Data Base de Opção" name="dataOpcaoFgts" value={dadosDoFormulario.dataOpcaoFgts || ""} onChange={aoMudarValores} disabled={!dadosDoFormulario.optanteFgts || !validarHabilitacao("dataOpcaoFgts")} /></ComponenteCampoBloqueado>
      </div>
      <div className={estilos.caixaBeneficio}>
        <div className={estilos.cabecalhoBeneficio}>
          <span className={estilos.tituloBeneficio}>Fornecimento de Vale Alimentação (VA)</span>
          <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("optanteVa")} aoAlternarTrava={() => aoAlternarTravas("optanteVa")} rotuloDoCampo="Chave de Recebimento de VA"><ComponenteInterruptorLogico nomeAtributo="optanteVa" estaMarcado={dadosDoFormulario.optanteVa} aoAlternarMudanca={aoMudarValores} rotuloTexto="Colaborador recebe o Vale" estaDesabilitado={!validarHabilitacao("optanteVa")} /></ComponenteCampoBloqueado>
        </div>
        <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("valorVa")} aoAlternarTrava={() => aoAlternarTravas("valorVa")} rotuloDoCampo="Montante Financeiro do VA"><CampoEntrada label="Valor Mensal do Benefício (R$)" name="valorVa" value={dadosDoFormulario.valorVa || ""} onChange={aoMudarValores} disabled={!dadosDoFormulario.optanteVa || !validarHabilitacao("valorVa")} /></ComponenteCampoBloqueado>
      </div>
    </div>
  </ComponenteCartaoDeSecao>
));

SecaoBeneficiosGerais.displayName = "SecaoBeneficiosGerais";

export const SecaoLogradouroEDependentes = memo(({ dadosDoFormulario, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={MapPin} tituloDaSecao="Detalhes Residenciais e Dependentes Legais">
    <div className={`${estilos.gradeColunas12} ${estilos.enderecoComBorda}`}>
      <div className={estilos.colunaSpan8}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("logradouro")} aoAlternarTrava={() => aoAlternarTravas("logradouro")} rotuloDoCampo="Nome do Logradouro"><CampoEntrada label="Logradouro" name="logradouro" value={dadosDoFormulario.logradouro || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("logradouro")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("numero")} aoAlternarTrava={() => aoAlternarTravas("numero")} rotuloDoCampo="Número Residencial"><CampoEntrada label="Número" name="numero" value={dadosDoFormulario.numero || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("numero")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cep")} aoAlternarTrava={() => aoAlternarTravas("cep")} rotuloDoCampo="Código Postal (CEP)"><CampoEntrada label="CEP" name="cep" value={dadosDoFormulario.cep || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("cep")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan8}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("bairro")} aoAlternarTrava={() => aoAlternarTravas("bairro")} rotuloDoCampo="Bairro Residencial"><CampoEntrada label="Bairro" name="bairro" value={dadosDoFormulario.bairro || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("bairro")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan8}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("cidade")} aoAlternarTrava={() => aoAlternarTravas("cidade")} rotuloDoCampo="Município ou Cidade"><CampoEntrada label="Cidade" name="cidade" value={dadosDoFormulario.cidade || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("cidade")} /></ComponenteCampoBloqueado></div>
      <div className={estilos.colunaSpan4}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("uf")} aoAlternarTrava={() => aoAlternarTravas("uf")} rotuloDoCampo="Estado (UF)"><CampoEntrada label="Unidade Federativa (UF)" name="uf" value={dadosDoFormulario.uf || ""} onChange={aoMudarValores} maxLength={2} disabled={!validarHabilitacao("uf")} /></ComponenteCampoBloqueado></div>
    </div>
    <div className={estilos.gradeColunas4Dependentes}>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("qtdDependenteIr")} aoAlternarTrava={() => aoAlternarTravas("qtdDependenteIr")} rotuloDoCampo="Dependentes Base IR"><CampoEntrada label="Total de Dependentes (IR)" type="number" name="qtdDependenteIr" value={dadosDoFormulario.qtdDependenteIr || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("qtdDependenteIr")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("qtdDependenteSalFamilia")} aoAlternarTrava={() => aoAlternarTravas("qtdDependenteSalFamilia")} rotuloDoCampo="Dependentes Base Salário Família"><CampoEntrada label="Dependentes (Sal. Família)" type="number" name="qtdDependenteSalFamilia" value={dadosDoFormulario.qtdDependenteSalFamilia || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("qtdDependenteSalFamilia")} /></ComponenteCampoBloqueado>
      <div className={estilos.colunaSpan2Lg}><ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("outrosBeneficios")} aoAlternarTrava={() => aoAlternarTravas("outrosBeneficios")} rotuloDoCampo="Anotações Extras de Benefícios"><CampoEntrada label="Outros Benefícios Extraordinários" name="outrosBeneficios" value={dadosDoFormulario.outrosBeneficios || ""} onChange={aoMudarValores} disabled={!validarHabilitacao("outrosBeneficios")} /></ComponenteCampoBloqueado></div>
    </div>
  </ComponenteCartaoDeSecao>
));

SecaoLogradouroEDependentes.displayName = "SecaoLogradouroEDependentes";

export const SecaoInformacoesBancarias = memo(({ dadosDoFormulario, aoMudarValores, aoAlternarTravas, validarHabilitacao, identificadorUsuario, listasCarregadas, filtragemBancariaExibida }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; aoMudarValores: FormModel['acoes']['aoLidarComMudancaEntrada']; aoAlternarTravas: FormModel['acoes']['aoAlternarBloqueioCampo']; validarHabilitacao: FormModel['acoes']['verificarCampoEstaHabilitado']; identificadorUsuario?: number | string | null; listasCarregadas: FormModel['estado']['listasAuxiliares']; filtragemBancariaExibida: FormModel['derivados']['agenciasBancariasFiltradas'] }) => (
  <ComponenteCartaoDeSecao iconeDecorativo={Building2} tituloDaSecao="Dados para Pagamento Bancário">
    <div className={estilos.gradeColunas4}>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("bancoId")} aoAlternarTrava={() => aoAlternarTravas("bancoId")} rotuloDoCampo="Instituição Bancária"><Seletor label="Banco Favorecido" name="bancoId" value={dadosDoFormulario.bancoId || ""} onChange={aoMudarValores} options={(Array.isArray(listasCarregadas?.bancos) ? listasCarregadas.bancos : []).map(itemBanco => ({ id: itemBanco.id, nome: `${itemBanco.codigo} - ${itemBanco.nome}` }))} disabled={!validarHabilitacao("bancoId")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("agenciaId")} aoAlternarTrava={() => aoAlternarTravas("agenciaId")} rotuloDoCampo="Código da Agência"><Seletor label="Agência de Vínculo" name="agenciaId" value={dadosDoFormulario.agenciaId || ""} onChange={aoMudarValores} options={(Array.isArray(filtragemBancariaExibida) ? filtragemBancariaExibida : []).map(itemAgencia => ({ id: itemAgencia.id, nome: itemAgencia.numero }))} disabled={!validarHabilitacao("agenciaId")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("tipoContaId")} aoAlternarTrava={() => aoAlternarTravas("tipoContaId")} rotuloDoCampo="Modalidade da Conta"><Seletor label="Classificação da Conta" name="tipoContaId" value={dadosDoFormulario.tipoContaId || ""} onChange={aoMudarValores} options={Array.isArray(listasCarregadas?.tiposConta) ? listasCarregadas.tiposConta : []} disabled={!validarHabilitacao("tipoContaId")} /></ComponenteCampoBloqueado>
      <ComponenteCampoBloqueado ehNovoRegistro={!identificadorUsuario} estaDesbloqueadoParaEdicao={validarHabilitacao("numeroConta")} aoAlternarTrava={() => aoAlternarTravas("numeroConta")} rotuloDoCampo="Numeração da Conta"><CampoEntrada label="Numeração Oficial da Conta" name="numeroConta" value={dadosDoFormulario.numeroConta || ""} onChange={aoMudarValores} placeholder="00000-0" disabled={!validarHabilitacao("numeroConta")} /></ComponenteCampoBloqueado>
    </div>
  </ComponenteCartaoDeSecao>
));

SecaoInformacoesBancarias.displayName = "SecaoInformacoesBancarias";

export const SecaoMatrizDeContratos = memo(({ dadosDoFormulario, acaoRealizarAlternanciaContrato, listasCarregadas, bloqueioInatividadeStatus }: { dadosDoFormulario: FormModel['estado']['dadosFormulario']; acaoRealizarAlternanciaContrato: FormModel['acoes']['aoAlternarSelecaoDoContrato']; listasCarregadas: FormModel['estado']['listasAuxiliares']; bloqueioInatividadeStatus: boolean }) => {
  const [termoContrato, definirTermoContrato] = useState("");
  const [limiteContratos, definirLimiteContratos] = useState(80);
  const termoDiferido = useDeferredValue(termoContrato);

  const contratosFiltrados = useMemo(() => {
    const termo = String(termoDiferido || "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLocaleLowerCase("pt-BR")
      .trim();
    const atualId = String(dadosDoFormulario.contratoId || "");
    const lista = (Array.isArray(listasCarregadas?.contratos) ? listasCarregadas.contratos : []).filter((contrato) => {
      if (!termo) return true;
      const indice = `${contrato.numeroContrato || ""} ${contrato.descricao || ""}`
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("pt-BR");
      return indice.includes(termo);
    });

    // O contrato atual aparece primeiro ao editar, mesmo em bases com milhares
    // de contratos e renderização progressiva.
    return [...lista].sort((a, b) => {
      const aAtual = String(a.id) === atualId;
      const bAtual = String(b.id) === atualId;
      if (aAtual !== bAtual) return aAtual ? -1 : 1;
      return String(a.numeroContrato || "").localeCompare(
        String(b.numeroContrato || ""),
        "pt-BR",
      );
    });
  }, [listasCarregadas?.contratos, dadosDoFormulario.contratoId, termoDiferido]);

  useEffect(() => {
    definirLimiteContratos(80);
  }, [termoDiferido, dadosDoFormulario.contratoId]);

  const contratosVisiveis = contratosFiltrados.slice(0, limiteContratos);

  return (
    <ComponenteCartaoDeSecao iconeDecorativo={FileText} tituloDaSecao="Contrato Atual do Colaborador">
      {!bloqueioInatividadeStatus && (
        <p className={estilos.ajudaContratoUnico}>Selecione somente um contrato atual. Alterações de lotação, cargo ou turno mantêm este vínculo; somente a troca do contrato encerra o vínculo anterior no histórico.</p>
      )}
      {bloqueioInatividadeStatus && (
        <div className={estilos.caixaAlerta}>
          <AlertTriangle className={estilos.iconeAlerta} size={20} />
          <div><p className={estilos.tituloAlerta}>Aviso Crítico do Sistema</p><p className={estilos.textoAlerta}>Colaboradores marcados como INATIVOS ficam impossibilitados de reter vínculos contratuais ativos.</p></div>
        </div>
      )}
      {(Array.isArray(listasCarregadas?.contratos) ? listasCarregadas.contratos : []).length === 0 ? (
        <div className={estilos.contratosVazios}><p>O sistema não identificou nenhum contrato vigente nesta base.</p></div>
      ) : (
        <>
          <label className={estilos.pesquisaContratoFormulario}>
            <Search size={16} aria-hidden="true" />
            <input
              type="search"
              value={termoContrato}
              onChange={(evento) => definirTermoContrato(evento.target.value)}
              placeholder="Pesquisar número ou descrição do contrato..."
              disabled={bloqueioInatividadeStatus}
            />
          </label>
          <div
            className={`${estilos.gradeContratos} ${bloqueioInatividadeStatus ? estilos.gradeContratosInativos : ""}`}
            role="radiogroup"
            aria-label="Contrato atual do colaborador"
          >
            {contratosVisiveis.map((itemContratoNoLoop) => {
              const esteElementoEstaSelecionado = String(dadosDoFormulario.contratoId) === String(itemContratoNoLoop.id);
              const identificadorContrato = itemContratoNoLoop?.id ?? "";
              const numeroContrato = itemContratoNoLoop?.numeroContrato || itemContratoNoLoop?.numero || (identificadorContrato ? `Contrato ${identificadorContrato}` : "Contrato");
              const descricaoContrato = itemContratoNoLoop?.descricao || itemContratoNoLoop?.nome || "Sem descrição informada";
              return (
                <button
                  type="button"
                  key={identificadorContrato || `${numeroContrato}-${descricaoContrato}`}
                  onClick={() => acaoRealizarAlternanciaContrato(String(identificadorContrato))}
                  className={`${estilos.cartaoContrato} ${esteElementoEstaSelecionado ? estilos.cartaoContratoSelecionado : ""}`}
                  role="radio"
                  aria-checked={esteElementoEstaSelecionado}
                  aria-label={`Selecionar ${numeroContrato}: ${descricaoContrato}`}
                  disabled={bloqueioInatividadeStatus || !identificadorContrato}
                >
                  <span className={`${estilos.verificacaoContrato} ${esteElementoEstaSelecionado ? estilos.verificacaoContratoSelecionado : ""}`} aria-hidden="true">
                    {esteElementoEstaSelecionado && <Check size={14} strokeWidth={3} />}
                  </span>
                  <span className={estilos.envoltorioTextoContrato}>
                    <span className={`${estilos.numeroContrato} ${esteElementoEstaSelecionado ? estilos.numeroContratoSelecionado : ""}`}>{numeroContrato}</span>
                    <span className={estilos.descricaoContrato} title={descricaoContrato}>{descricaoContrato}</span>
                  </span>
                </button>
              );
            })}
          </div>
          {contratosVisiveis.length === 0 && (
            <div className={estilos.contratosVazios}><p>Nenhum contrato corresponde à pesquisa.</p></div>
          )}
          {contratosFiltrados.length > contratosVisiveis.length && (
            <button
              type="button"
              className={estilos.botaoMaisContratosFormulario}
              onClick={() => definirLimiteContratos((limite) => limite + 80)}
            >
              Mostrar mais {Math.min(80, contratosFiltrados.length - contratosVisiveis.length)} contratos
            </button>
          )}
        </>
      )}
    </ComponenteCartaoDeSecao>
  );
});

SecaoMatrizDeContratos.displayName = "SecaoMatrizDeContratos";