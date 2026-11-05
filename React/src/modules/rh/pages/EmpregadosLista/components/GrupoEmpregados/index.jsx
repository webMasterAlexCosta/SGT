import React, { memo, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Briefcase,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  Edit,
  Eye,
  EyeOff,
  FileText,
  History,
  MapPin,
  Users,
} from "lucide-react";
import { EnvolucroDica } from "../../../../../../core/ui/EnvolucroDica.jsx";
import { MODOS_VISUALIZACAO } from "../../visualizacao.js";
import estilos from "./index.module.css";

const obterIniciais = (nome) =>
  nome
    ? nome
        .split(" ")
        .filter(Boolean)
        .slice(0, 2)
        .map((parte) => parte[0])
        .join("")
        .toUpperCase()
    : "??";

const obterCorStatus = (status) => {
  const statusNormalizado = String(status || "").toUpperCase();
  if (statusNormalizado === "ATIVO") return estilos.statusAtivo;
  if (statusNormalizado.includes("FERIAS") || statusNormalizado.includes("FÉRIAS")) {
    return estilos.statusFerias;
  }
  if (statusNormalizado.includes("AFASTADO")) return estilos.statusAfastado;
  return estilos.statusInativo;
};

const mascararCpf = (cpf) => {
  if (!cpf) return "***.***.***-**";
  const numeros = cpf.replace(/\D/g, "");
  return numeros.length === 11
    ? `***.${numeros.substring(3, 6)}.${numeros.substring(6, 9)}-**`
    : "***.***.***-**";
};

const obterDadosColaborador = (colaborador) => ({
  lotacao:
    colaborador?.lotacao?.nome ||
    colaborador?.lotacao?.descricao ||
    colaborador?.lotacaoNome ||
    "-",
  jornada:
    colaborador?.jornada?.descricao ||
    colaborador?.jornada?.nome ||
    colaborador?.turno?.descricao ||
    colaborador?.turno?.nome ||
    colaborador?.jornadaDescricao ||
    "-",
  cargo:
    colaborador?.cargo?.nome ||
    colaborador?.cargo?.descricao ||
    colaborador?.cargoNome ||
    "-",
  status:
    colaborador?.status?.descricao ||
    colaborador?.statusDescricao ||
    "INDEFINIDO",
});

const IdentidadeColaborador = memo(({ colaborador, compacta = false, mostrarMatricula = true }) => (
  <div className={`${estilos.celulaUsuario} ${compacta ? estilos.celulaUsuarioCompacta : ""}`}>
    <div className={estilos.avatarUsuario} aria-hidden="true">
      {obterIniciais(colaborador.nome)}
    </div>
    <div className={estilos.textosUsuario}>
      <p className={estilos.nomeUsuario} title={colaborador.nome}>
        {colaborador.nome}
      </p>
      {mostrarMatricula && (
        <p className={estilos.matriculaUsuario}>
          {colaborador.matricula || "S/ Matrícula"}
        </p>
      )}
    </div>
  </div>
));
IdentidadeColaborador.displayName = "IdentidadeColaborador";

const CpfColaborador = memo(({ colaborador, cpfRevelado, acoes }) => (
  <div className={estilos.envoltorioCpf}>
    <span className={estilos.emblemaCpf}>
      {cpfRevelado ? colaborador.cpf : mascararCpf(colaborador.cpf)}
    </span>
    <EnvolucroDica
      tooltip={cpfRevelado ? "Ocultar CPF" : "Exibir CPF"}
      position="top"
      wrapperClass={estilos.flexEmLinha}
    >
      <button
        type="button"
        onClick={() => acoes.alternarVisualizacaoCpf(colaborador.id)}
        className={`${estilos.botaoOlho} ${
          cpfRevelado ? estilos.botaoOlhoRevelado : estilos.botaoOlhoOculto
        }`}
        aria-label={cpfRevelado ? `Ocultar CPF de ${colaborador.nome}` : `Exibir CPF de ${colaborador.nome}`}
      >
        {cpfRevelado ? (
          <EyeOff size={14} strokeWidth={2.5} aria-hidden="true" />
        ) : (
          <Eye size={14} strokeWidth={2.5} aria-hidden="true" />
        )}
      </button>
    </EnvolucroDica>
  </div>
));
CpfColaborador.displayName = "CpfColaborador";

const StatusColaborador = memo(({ status }) => (
  <span className={`${estilos.emblemaStatus} ${obterCorStatus(status)}`}>
    {status}
  </span>
));
StatusColaborador.displayName = "StatusColaborador";

const AcoesColaborador = memo(
  ({ colaborador, modaisAcoes, eAdministrador, podeEditar, posicaoTooltip = "top" }) => (
    <div className={estilos.envoltorioAcoes}>
      {podeEditar && (
        <EnvolucroDica
          tooltip="Editar registro"
          position={posicaoTooltip}
          wrapperClass={estilos.blocoEmLinha}
        >
          <button
            type="button"
            onClick={() => modaisAcoes.abrirEdicao(colaborador.id)}
            className={`${estilos.botaoAcao} ${estilos.botaoEditar}`}
            aria-label={`Editar ${colaborador.nome}`}
          >
            <Edit size={16} aria-hidden="true" />
          </button>
        </EnvolucroDica>
      )}
      <EnvolucroDica
        tooltip="Histórico contratual"
        position={posicaoTooltip}
        wrapperClass={estilos.blocoEmLinha}
      >
        <button
          type="button"
          onClick={() => modaisAcoes.abrirHistoricoContrato(colaborador)}
          className={`${estilos.botaoAcao} ${estilos.botaoHistorico}`}
          aria-label={`Abrir histórico contratual de ${colaborador.nome}`}
        >
          <History size={16} aria-hidden="true" />
        </button>
      </EnvolucroDica>
      {eAdministrador && (
        <EnvolucroDica
          tooltip="Logs de auditoria"
          position={posicaoTooltip}
          wrapperClass={estilos.blocoEmLinha}
        >
          <button
            type="button"
            onClick={() => modaisAcoes.abrirHistoricoAuditoria(colaborador)}
            className={`${estilos.botaoAcao} ${estilos.botaoAuditoria}`}
            aria-label={`Abrir auditoria de ${colaborador.nome}`}
          >
            <Eye size={16} aria-hidden="true" />
          </button>
        </EnvolucroDica>
      )}
    </div>
  ),
);
AcoesColaborador.displayName = "AcoesColaborador";

const IconeTexto = ({ Icone, texto }) => (
  <div className={estilos.envoltorioIconeTexto}>
    <Icone size={14} className={estilos.iconeTextoIcone} aria-hidden="true" />
    <span className={estilos.iconeTextoSpan} title={texto}>
      {texto}
    </span>
  </div>
);

const LinhaTabelaColaborador = memo(
  ({ colaborador, cpfRevelado, acoes, modaisAcoes, eAdministrador, podeEditar }) => {
    const dados = obterDadosColaborador(colaborador);

    return (
      <tr className={estilos.linhaTabela}>
        <td className={`${estilos.celulaTabela} ${estilos.celulaTabelaPrimeira}`}>
          <IdentidadeColaborador colaborador={colaborador} />
        </td>
        <td className={estilos.celulaTabela}>
          <span className={estilos.valorMatriculaTabela}>{colaborador.matricula || "-"}</span>
        </td>
        <td className={estilos.celulaTabela}>
          <CpfColaborador
            colaborador={colaborador}
            cpfRevelado={cpfRevelado}
            acoes={acoes}
          />
        </td>
        <td className={estilos.celulaTabela}>
          <IconeTexto Icone={MapPin} texto={dados.lotacao} />
        </td>
        <td className={estilos.celulaTabela}>
          <IconeTexto Icone={Clock} texto={dados.jornada} />
        </td>
        <td className={estilos.celulaTabela}>
          <IconeTexto Icone={Briefcase} texto={dados.cargo} />
        </td>
        <td className={estilos.celulaTabela}>
          <StatusColaborador status={dados.status} />
        </td>
        <td className={`${estilos.celulaTabela} ${estilos.celulaTabelaUltima}`}>
          <AcoesColaborador
            colaborador={colaborador}
            modaisAcoes={modaisAcoes}
            eAdministrador={eAdministrador}
            podeEditar={podeEditar}
            posicaoTooltip="left"
          />
        </td>
      </tr>
    );
  },
);
LinhaTabelaColaborador.displayName = "LinhaTabelaColaborador";

const ItemListaColaborador = memo(
  ({ colaborador, cpfRevelado, acoes, modaisAcoes, eAdministrador, podeEditar }) => {
    const dados = obterDadosColaborador(colaborador);

    return (
      <article className={estilos.itemLista} role="listitem">
        <IdentidadeColaborador colaborador={colaborador} compacta />

        <div className={estilos.metadadosLista}>
          <IconeTexto Icone={MapPin} texto={dados.lotacao} />
          <IconeTexto Icone={Clock} texto={dados.jornada} />
          <IconeTexto Icone={Briefcase} texto={dados.cargo} />
        </div>

        <div className={estilos.lateralLista}>
          <CpfColaborador
            colaborador={colaborador}
            cpfRevelado={cpfRevelado}
            acoes={acoes}
          />
          <StatusColaborador status={dados.status} />
          <AcoesColaborador
            colaborador={colaborador}
            modaisAcoes={modaisAcoes}
            eAdministrador={eAdministrador}
            podeEditar={podeEditar}
          />
        </div>
      </article>
    );
  },
);
ItemListaColaborador.displayName = "ItemListaColaborador";

const DetalheGrade = ({ Icone, rotulo, valor }) => (
  <div className={estilos.detalheGrade}>
    <Icone size={15} className={estilos.iconeDetalheGrade} aria-hidden="true" />
    <div className={estilos.textoDetalheGrade}>
      <dt className={estilos.rotuloDetalheGrade}>{rotulo}</dt>
      <dd className={estilos.valorDetalheGrade} title={valor}>
        {valor}
      </dd>
    </div>
  </div>
);

const CartaoGradeColaborador = memo(
  ({ colaborador, cpfRevelado, acoes, modaisAcoes, eAdministrador, podeEditar }) => {
    const dados = obterDadosColaborador(colaborador);

    return (
      <article className={estilos.cartaoColaborador} role="listitem">
        <header className={estilos.topoCartaoColaborador}>
          <IdentidadeColaborador colaborador={colaborador} compacta mostrarMatricula={false} />
          <StatusColaborador status={dados.status} />
        </header>

        <dl className={estilos.detalhesGrade}>
          <div className={estilos.campoGradeSimples}>
            <dt>Matrícula</dt>
            <dd>{colaborador.matricula || "-"}</dd>
          </div>
          <div className={estilos.campoGradeCpf}>
            <dt>CPF</dt>
            <dd>
              <CpfColaborador
                colaborador={colaborador}
                cpfRevelado={cpfRevelado}
                acoes={acoes}
              />
            </dd>
          </div>
          <DetalheGrade Icone={MapPin} rotulo="Lotação" valor={dados.lotacao} />
          <DetalheGrade Icone={Clock} rotulo="Turno" valor={dados.jornada} />
          <DetalheGrade Icone={Briefcase} rotulo="Cargo" valor={dados.cargo} />
        </dl>

        <footer className={estilos.rodapeCartaoColaborador}>
          <AcoesColaborador
            colaborador={colaborador}
            modaisAcoes={modaisAcoes}
            eAdministrador={eAdministrador}
            podeEditar={podeEditar}
          />
        </footer>
      </article>
    );
  },
);
CartaoGradeColaborador.displayName = "CartaoGradeColaborador";

const obterLoteRenderizacao = (modo) =>
  modo === MODOS_VISUALIZACAO.GRADE ? 120 : 250;

const ConteudoGrupo = ({
  grupo,
  modoVisualizacao,
  cpfsRevelados,
  acoes,
  modaisAcoes,
  eAdministrador,
  podeEditar,
}) => {
  const lote = obterLoteRenderizacao(modoVisualizacao);
  const [limiteRenderizacao, definirLimiteRenderizacao] = useState(lote);

  useEffect(() => {
    definirLimiteRenderizacao(lote);
  }, [grupo.id, grupo.lista, modoVisualizacao, lote]);


  const listaVisivel = useMemo(
    () => grupo.lista.slice(0, limiteRenderizacao),
    [grupo.lista, limiteRenderizacao],
  );
  const restante = Math.max(0, grupo.lista.length - listaVisivel.length);

  const propriedadesComuns = (colaborador) => ({
    colaborador,
    cpfRevelado: Boolean(cpfsRevelados[colaborador.id]),
    acoes,
    modaisAcoes,
    eAdministrador,
    podeEditar,
  });

  let conteudo;
  if (modoVisualizacao === MODOS_VISUALIZACAO.GRADE) {
    conteudo = (
      <div className={estilos.gradeColaboradores} role="list">
        {listaVisivel.map((colaborador) => (
          <CartaoGradeColaborador
            key={colaborador.id}
            {...propriedadesComuns(colaborador)}
          />
        ))}
      </div>
    );
  } else if (modoVisualizacao === MODOS_VISUALIZACAO.LISTA) {
    conteudo = (
      <div className={estilos.listaCompacta} role="list">
        {listaVisivel.map((colaborador) => (
          <ItemListaColaborador
            key={colaborador.id}
            {...propriedadesComuns(colaborador)}
          />
        ))}
      </div>
    );
  } else {
    conteudo = (
      <div className={estilos.recipienteTabela}>
        <table className={estilos.tabela}>
          <thead className={estilos.cabecalhoTabela}>
            <tr>
              <th className={`${estilos.celulaCabecalho} ${estilos.celulaCabecalhoPrimeira}`} scope="col">
                Colaborador
              </th>
              <th className={estilos.celulaCabecalho} scope="col">Matrícula</th>
              <th className={estilos.celulaCabecalho} scope="col">CPF</th>
              <th className={estilos.celulaCabecalho} scope="col">Lotação</th>
              <th className={estilos.celulaCabecalho} scope="col">Turno</th>
              <th className={estilos.celulaCabecalho} scope="col">Cargo</th>
              <th className={estilos.celulaCabecalho} scope="col">Status</th>
              <th className={`${estilos.celulaCabecalho} ${estilos.celulaCabecalhoUltima}`} scope="col">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {listaVisivel.map((colaborador) => (
              <LinhaTabelaColaborador
                key={colaborador.id}
                {...propriedadesComuns(colaborador)}
              />
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <>
      {conteudo}
      {restante > 0 && (
        <div className={estilos.rodapeRenderizacaoProgressiva}>
          <span>
            Exibindo {listaVisivel.length} de {grupo.lista.length} colaboradores
          </span>
          <button
            type="button"
            className={estilos.botaoRenderizarMais}
            onClick={() =>
              definirLimiteRenderizacao((atual) =>
                Math.min(grupo.lista.length, atual + lote),
              )
            }
          >
            Mostrar mais {Math.min(lote, restante)}
          </button>
        </div>
      )}
    </>
  );
};

export const GrupoEmpregados = memo(
  ({
    grupo = {},
    estaExpandido,
    alternarGrupo = () => {},
    cpfsRevelados = {},
    acoes = {},
    modaisAcoes = {},
    eAdministrador,
    podeEditar = true,
    modoVisualizacao,
    ocultarCabecalho = false,
  }) => {
    const listaGrupo = Array.isArray(grupo?.lista) ? grupo.lista : [];
    const estatisticasGrupo = {
      total: Number.isFinite(Number(grupo?.stats?.total))
        ? Number(grupo.stats.total)
        : listaGrupo.length,
      ativos: Number.isFinite(Number(grupo?.stats?.ativos))
        ? Number(grupo.stats.ativos)
        : 0,
    };
    const grupoSeguro = {
      ...grupo,
      id: grupo?.id ?? "grupo-sem-id",
      titulo: grupo?.titulo || "Grupo",
      descricao: grupo?.descricao || "",
      isWarning: Boolean(grupo?.isWarning),
      lista: listaGrupo,
      stats: estatisticasGrupo,
    };

    const sufixoId = String(grupoSeguro.id).replace(/[^a-zA-Z0-9_-]/g, "-");
    const idCabecalho = `cabecalho-grupo-${sufixoId}`;
    const idConteudo = `conteudo-grupo-${sufixoId}`;

    return (
      <section
        className={`${estilos.cartaoGrupo} ${
          grupoSeguro.isWarning ? estilos.cartaoGrupoAviso : ""
        }`}
        aria-labelledby={ocultarCabecalho ? undefined : idCabecalho}
      >
        {!ocultarCabecalho && (
        <button
          type="button"
          id={idCabecalho}
          onClick={() => alternarGrupo(grupoSeguro.id)}
          className={`${estilos.cabecalhoGrupo} ${
            grupoSeguro.isWarning ? estilos.cabecalhoGrupoAviso : ""
          } ${estaExpandido ? estilos.cabecalhoGrupoComBorda : ""}`}
          aria-expanded={Boolean(estaExpandido)}
          aria-controls={idConteudo}
        >
          <span className={estilos.cabecalhoGrupoEsquerda}>
            <span className={estilos.botaoAlternarGrupo} aria-hidden="true">
              {estaExpandido ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
            </span>
            <span
              className={`${estilos.envoltorioIconeGrupo} ${
                grupoSeguro.isWarning ? estilos.iconeGrupoAviso : estilos.iconeGrupoNormal
              }`}
              aria-hidden="true"
            >
              {grupoSeguro.isWarning ? <AlertCircle size={20} /> : <FileText size={20} />}
            </span>
            <span className={estilos.textosGrupo}>
              <span
                className={`${estilos.tituloGrupo} ${
                  grupoSeguro.isWarning ? estilos.tituloGrupoAviso : ""
                }`}
              >
                {grupoSeguro.titulo}
              </span>
              {grupoSeguro.descricao && (
                <span className={estilos.descricaoGrupo}>{grupoSeguro.descricao}</span>
              )}
            </span>
          </span>

          <span className={estilos.envoltorioEstatisticasGrupo}>
            <span className={estilos.emblemaEstatisticasGrupo}>
              <span className={estilos.estatisticaTotal}>
                <Users size={14} aria-hidden="true" /> {grupoSeguro.stats.total}
              </span>
              <span className={estilos.divisorEstatistica} aria-hidden="true" />
              <span className={estilos.estatisticaAtivo}>
                <CheckCircle2 size={14} aria-hidden="true" /> {grupoSeguro.stats.ativos} Ativos
              </span>
            </span>
          </span>
        </button>
        )}

        {(ocultarCabecalho || estaExpandido) && (
          <div
            id={idConteudo}
            role="region"
            aria-labelledby={ocultarCabecalho ? undefined : idCabecalho}
          >
            <ConteudoGrupo
              grupo={grupoSeguro}
              modoVisualizacao={modoVisualizacao}
              cpfsRevelados={cpfsRevelados}
              acoes={acoes}
              modaisAcoes={modaisAcoes}
              eAdministrador={eAdministrador}
              podeEditar={podeEditar}
            />
          </div>
        )}
      </section>
    );
  },
);

GrupoEmpregados.displayName = "GrupoEmpregados";
