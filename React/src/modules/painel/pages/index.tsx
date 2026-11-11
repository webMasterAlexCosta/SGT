import React, { useState, useEffect } from "react";
import {
  AlertTriangle,
  BarChart3,
  BriefcaseBusiness,
  CalendarClock,
  Gauge,
  LayoutDashboard,
  ShieldCheck,
} from "lucide-react";
import { usePainel } from "./index.ts";
import { ServicoSistema } from "../../../services/system/ServicoSistema.ts";
import estilos from "./index.module.css";
import { IndicadorEfetivo } from "../components/IndicadorEfetivo/index.tsx";
import { exportarPainelGerencial, exportarPainelPdf } from "../exportacao.ts";
import {
  AlertasDecisao,
  CabecalhoGovernanca,
  CoberturaFrequencia,
  EvolucaoOperacional,
  IntegridadeDados,
  MatrizUnidades,
  NotasMetodologicas,
  ResumoExecutivo,
  SaudeContratual,
} from "../components/GestaoExecutiva/index.tsx";
import {
  AnaliseCadastral,
  AtalhosGerenciais,
  AuditoriaRecente,
  ChecklistGovernanca,
  ComparativoPeriodo,
  ComparativoUnidades,
  DistribuicaoStatus,
  FunilFrequencia,
  HorizonteContratual,
  ImpactoAbsenteismo,
  LeituraGerencialPeriodo,
  PadraoSemanal,
  PressaoOperacional,
  PrioridadesPorUnidade,
  RankingLotacaoTurno,
  QuadroPessoal,
  SituacaoDetalhadaQuadro,
} from "../components/FerramentasGestao/index.tsx";

const obterNomeUsuario = (user: (import("../../../contracts/auth").AuthenticatedUser | null)) =>
  user?.nomeCompleto ||
  user?.nome ||
  user?.displayName ||
  user?.username ||
  "Usuário";

const obterLocalizacaoUsuario = (user: import("../../../contracts/auth").AuthFields | null) => {
  const lotacao =
    user?.lotacaoNome ||
    (user?.lotacao && typeof user.lotacao === 'object' ? user.lotacao.label || user.lotacao.nome : null) ||
    (typeof user?.lotacao === "string" ? user.lotacao : null);
  const unidade =
    user?.gestaoLocal?.label ||
    user?.gestaoLocal?.nome ||
    user?.gestaoLocalNome ||
    user?.superintendencia?.label ||
    user?.superintendencia?.nome ||
    user?.superintendenciaNome ||
    null;
  const uf = user?.uf || user?.superintendencia?.uf || null;

  const localBase = lotacao || unidade;
  if (localBase && uf && !String(localBase).toUpperCase().includes(String(uf).toUpperCase())) {
    return `${localBase} - ${uf}`;
  }
  if (localBase) return localBase;
  if (uf) return `UF ${uf}`;
  return "Localização não disponível";
};

const DataHoraLocalUsuario = ({ user }: { user: import("../../../contracts/auth").AuthenticatedUser | null }) => {
  const [dataHora, setDataHora] = useState(new Date());
  const localizacaoUsuario = obterLocalizacaoUsuario(user);
  const [local, setLocal] = useState(localizacaoUsuario);

  useEffect(() => {
    const timer = setInterval(() => setDataHora(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    let ativo = true;
    const controlador = new AbortController();
    setLocal(localizacaoUsuario);

    if (!("geolocation" in navigator)) return () => { ativo = false; };

    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          const { cidade, estado } = await ServicoSistema.buscarLocalizacaoPorCoordenadas(
            coords,
            { signal: controlador.signal },
          );

          if (ativo && cidade) {
            setLocal(`${cidade}${estado ? ` - ${estado}` : ""}`);
          }
        } catch {
          if (ativo) setLocal(localizacaoUsuario);
        }
      },
      () => {
        if (ativo) setLocal(localizacaoUsuario);
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    );

    return () => {
      ativo = false;
      controlador.abort();
    };
  }, [localizacaoUsuario]);

  return (
    <>
      {dataHora.toLocaleDateString("pt-BR")} às {dataHora.toLocaleTimeString("pt-BR")} | {local}
    </>
  );
};

const numero = (valor: (number)) => new Intl.NumberFormat("pt-BR").format(Number(valor || 0));
const dataBr = (valor: string | null | undefined) => valor
  ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${valor}T12:00:00`))
  : "—";

const TituloSecao = ({ icone: Icone, titulo, descricao, marcador }: { icone: import("lucide-react").LucideIcon; titulo: string; descricao: string; marcador: string }) => (
  <div className={estilos.tituloSecao}>
    <span><Icone size={19} aria-hidden="true" /></span>
    <div>
      <div className={estilos.linhaTituloSecao}>
        <h2>{titulo}</h2>
        {marcador ? <small>{marcador}</small> : null}
      </div>
      <p>{descricao}</p>
    </div>
  </div>
);

const RadarExecutivo = ({ dados, aoAbrirSecao }: { dados: (import("../../../contracts/java").PainelGestaoDTO); aoAbrirSecao: (React.Dispatch<React.SetStateAction<string>>) }) => {
  const itens = [
    {
      rotulo: "Prioridades",
      valor: Number(dados?.alertas?.length || 0),
      detalhe: "alertas para decisão",
      secao: "operacao",
      classe: estilos.radarCritico,
      icone: AlertTriangle,
    },
    {
      rotulo: "Pendências na referência",
      valor: Number(dados?.frequencia?.pendentes || 0),
      detalhe: `sem lançamento em ${dataBr(dados?.frequencia?.data || dados?.referencia)}`,
      secao: "operacao",
      classe: estilos.radarAtencao,
      icone: CalendarClock,
    },
    {
      rotulo: "Contratos",
      valor: Number(dados?.contratos?.vencendoEm30Dias || 0),
      detalhe: "vencendo a partir da referência",
      secao: "contratos",
      classe: estilos.radarInfo,
      icone: BriefcaseBusiness,
    },
    {
      rotulo: "Integridade",
      valor: Number(dados?.integridade?.totalInconsistencias || 0),
      detalhe: "inconsistências identificadas",
      secao: "governanca",
      classe: Number(dados?.integridade?.totalInconsistencias || 0) > 0 ? estilos.radarCritico : estilos.radarOk,
      icone: ShieldCheck,
    },
  ];

  return (
    <section className={estilos.radarExecutivo} aria-label="Atenção executiva">
      <div className={estilos.radarCabecalho}>
        <div>
          <span>Leitura rápida</span>
        </div>
        <p>Os números abaixo usam os mesmos dados consolidados do painel e não fazem novas consultas.</p>
      </div>
      <div className={estilos.radarGrade}>
        {itens.map(({ rotulo, valor, detalhe, secao, classe, icone: Icone }) => (
          <button key={rotulo} type="button" onClick={() => aoAbrirSecao(secao)} className={`${estilos.radarItem} ${classe}`}>
            <span className={estilos.radarIcone}><Icone size={18} aria-hidden="true" /></span>
            <div><small>{rotulo}</small><strong>{numero(valor)}</strong><p>{detalhe}</p></div>
          </button>
        ))}
      </div>
    </section>
  );
};

const OPCOES_VISAO = [
  { id: "resumo", rotulo: "Resumo" },
  { id: "contratos", rotulo: "Contratos" },
  { id: "operacao", rotulo: "Operação" },
  { id: "governanca", rotulo: "Governança" },
  { id: "tudo", rotulo: "Tudo" },
];

const NavegacaoPainel = ({ visao, aoMudar }: { visao: string; aoMudar: React.Dispatch<React.SetStateAction<string>> }) => (
  <nav className={estilos.navegacaoPainel} aria-label="Modo de visualização do painel">
    <span><LayoutDashboard size={16} aria-hidden="true" /> Modo foco</span>
    {OPCOES_VISAO.map((opcao) => (
      <button
        key={opcao.id}
        type="button"
        className={visao === opcao.id ? estilos.navegacaoAtiva : ""}
        aria-pressed={visao === opcao.id}
        onClick={() => aoMudar(opcao.id)}
      >
        {opcao.rotulo}
      </button>
    ))}
  </nav>
);

const Espaco = ({ tamanho = "simples", children }: { tamanho?: (string); children: React.ReactNode }) => (
  <div className={`${estilos.slot} ${estilos[`slot_${tamanho}`] || ""}`}>{children}</div>
);

export const Painel = ({ onNavigate }: { onNavigate: (caminho: string) => void }) => {
  const {
    dados,
    atividades,
    rankingLotacoes,
    rankingCarregando,
    referencia,
    periodoDias,
    hoje,
    carregando,
    erro,
    processamento,
    perfil,
    user,
    acoes,
  } = usePainel(onNavigate);
  const [visaoPainel, setVisaoPainel] = useState("resumo");
  const [indicadorEfetivoRelatorio, setIndicadorEfetivoRelatorio] = useState<import("../../../contracts/painel").StaffingReport | null>(null);
  useEffect(() => setIndicadorEfetivoRelatorio(null), [referencia]);
  const exibir = (secao: (string)) => visaoPainel === "tudo" || visaoPainel === secao;
  const inicioPeriodo = dados?.periodo?.inicio;
  const fimPeriodo = dados?.periodo?.fim || dados?.referencia || referencia;
  const diasPeriodo = Number(dados?.periodo?.dias || periodoDias || 0);
  const rotuloJanela = inicioPeriodo && fimPeriodo
    ? `${dataBr(inicioPeriodo)} a ${dataBr(fimPeriodo)} (${diasPeriodo} dias)`
    : `${diasPeriodo} dias até ${dataBr(fimPeriodo)}`;
  const dataReferenciaOperacional = dados?.frequencia?.data || dados?.referencia || referencia;

  return (
    <main className={estilos.pagina}>
      <CabecalhoGovernanca
        dados={(dados) ?? undefined}
        referencia={referencia}
        hoje={hoje}
        periodoDias={periodoDias}
        carregando={carregando}
        aoMudarReferencia={acoes.setReferencia}
        aoMudarPeriodo={acoes.setPeriodoDias}
        aoRecarregar={acoes.recarregar}
        aoExportarXlsx={() => exportarPainelGerencial(dados, periodoDias, indicadorEfetivoRelatorio)}
        aoExportarPdf={() => exportarPainelPdf(dados, periodoDias, indicadorEfetivoRelatorio)}
      />

      {erro && <div className={estilos.erro}><AlertTriangle size={18} />{erro}</div>}
      {(carregando || processamento?.status === "PROCESSANDO" || processamento?.status === "PENDENTE") && (
        <section className={estilos.processamento} aria-live="polite">
          <div>
            <strong>{processamento?.desatualizado ? "Atualizando painel em segundo plano" : "Consolidando painel gerencial"}</strong>
            <span>{processamento?.mensagem || "O servidor está agregando os indicadores sem carregar colaboradores no navegador."}</span>
          </div>
          <div className={estilos.barraProcessamento} aria-label={`Progresso ${processamento?.progresso || 0}%`}>
            <span style={{ width: `${Math.max(3, Math.min(100, Number(processamento?.progresso || 0)))}%` }} />
          </div>
          <b>{Number(processamento?.progresso || 0)}%</b>
        </section>
      )}
      {carregando && !dados && <div className={estilos.carregando}>A página permanece disponível enquanto o MySQL consolida os dados.</div>}

      {dados && (
        <>
          <div className={estilos.avisoEscopo}>
            Seja bem-vindo, {obterNomeUsuario(user)}. <DataHoraLocalUsuario user={user} />.
          </div>

          <NavegacaoPainel visao={visaoPainel} aoMudar={setVisaoPainel} />

          {exibir("resumo") && (
            <section id="painel-resumo" className={estilos.blocoVisao}>
              <IndicadorEfetivo referencia={referencia} aoAtualizarRelatorio={setIndicadorEfetivoRelatorio} />
              <ResumoExecutivo dados={dados} indicadorEfetivo={(indicadorEfetivoRelatorio) ?? undefined} />
              <RadarExecutivo dados={dados} aoAbrirSecao={setVisaoPainel} />
              <section className={`${estilos.gradeBento} ${estilos.gradeAcoes}`}>
                <Espaco tamanho="triplo"><AtalhosGerenciais perfil={perfil} aoNavegar={acoes.navegar} /></Espaco>
              </section>
            </section>
          )}

          {exibir("contratos") && (
            <section id="painel-contratos" className={estilos.secaoPainel}>
              <TituloSecao
                icone={BriefcaseBusiness}
                titulo="Gestão contratual e dimensionamento"
                descricao="Efetivo previsto, ocupação, vencimentos e riscos que exigem atuação gerencial."
                marcador="Capacidade"
              />
              <div className={estilos.gradeBento}>
                <Espaco tamanho="duplo"><SaudeContratual contratos={(dados.contratos) ?? undefined} efetivo={(dados.efetivo) ?? undefined} podeAbrir={perfil.podeVisualizarContratos} aoNavegar={acoes.navegar} /></Espaco>
                <Espaco><PrioridadesPorUnidade unidades={(dados.unidades) ?? undefined} nivel={(dados.escopo?.nivel) ?? undefined} /></Espaco>
                <Espaco><QuadroPessoal quadro={(dados.quadro) ?? undefined} /></Espaco>
                <Espaco><HorizonteContratual horizonte={(dados.horizonteContratual) ?? undefined} /></Espaco>
                <Espaco tamanho="triplo"><AnaliseCadastral analise={(dados.analiseCadastro) ?? undefined} admissoesMensais={(dados.admissoesMensais) ?? undefined} periodoDias={periodoDias} /></Espaco>
                <Espaco><SituacaoDetalhadaQuadro dados={(dados.situacaoQuadro) ?? undefined} /></Espaco>
              </div>
            </section>
          )}

          {exibir("operacao") && (
            <section id="painel-operacao" className={estilos.secaoPainel}>
              <TituloSecao
                icone={BarChart3}
                titulo={`Análise histórica da operação — ${diasPeriodo} dias`}
                descricao={`Janela real de ${rotuloJanela}. Absenteísmo, comparação, distribuição, padrão semanal, evolução e ranking usam todos os lançamentos desse intervalo.`}
                marcador="Histórico"
              />
              <div className={estilos.gradeBento}>
                <Espaco tamanho="triplo">
                  <LeituraGerencialPeriodo
                    periodo={(dados.periodo) ?? undefined}
                    comparativo={(dados.comparativoPeriodo) ?? undefined}
                  />
                </Espaco>
                <Espaco><ImpactoAbsenteismo periodo={(dados.periodo) ?? undefined} /></Espaco>
                <Espaco><PressaoOperacional pressao={(dados.pressaoOperacional) ?? undefined} /></Espaco>
                <Espaco><PadraoSemanal dados={(dados.padraoSemanal) ?? undefined} /></Espaco>
                <Espaco tamanho="triplo"><ComparativoPeriodo periodo={(dados.periodo) ?? undefined} comparativo={(dados.comparativoPeriodo) ?? undefined} /></Espaco>
                <Espaco tamanho="triplo"><EvolucaoOperacional evolucao={(dados.evolucao) ?? undefined} periodoDias={diasPeriodo} /></Espaco>
                <Espaco tamanho="duplo"><DistribuicaoStatus dados={(dados.distribuicaoStatus) ?? undefined} periodo={(dados.periodo) ?? undefined} /></Espaco>
                <Espaco><AlertasDecisao alertas={(dados.alertas) ?? undefined} aoNavegar={acoes.navegar} /></Espaco>
                {!perfil.presidente && (
                  <Espaco tamanho="triplo">
                    <RankingLotacaoTurno
                      dados={rankingLotacoes}
                      periodoDias={diasPeriodo}
                      carregando={rankingCarregando}
                      aoCarregar={acoes.carregarRanking}
                    />
                  </Espaco>
                )}
              </div>

              <TituloSecao
                icone={CalendarClock}
                titulo={`Fotografia da data de referência — ${dataBr(dataReferenciaOperacional)}`}
                descricao="Os cartões abaixo são deliberadamente de um único dia: mostram quem estava esperado na escala, quem recebeu lançamento e o que ficou pendente exatamente na referência. Eles não representam os 7, 15, 30, 60 ou 90 dias."
                marcador="1 dia"
              />
              <div className={estilos.gradeBento}>
                <Espaco tamanho="duplo"><CoberturaFrequencia frequencia={(dados.frequencia) ?? undefined} podeOperar={perfil.podeOperarFrequencia} aoNavegar={acoes.navegar} /></Espaco>
                <Espaco><FunilFrequencia frequencia={(dados.frequencia) ?? undefined} /></Espaco>
              </div>
            </section>
          )}

          {exibir("governanca") && (
            <section id="painel-governanca" className={estilos.secaoPainel}>
              <TituloSecao
                icone={ShieldCheck}
                titulo="Governança, integridade e comparação"
                descricao="Sinais de qualidade dos dados, comparação entre unidades e rastreabilidade das alterações."
                marcador="Controle"
              />
              <div className={estilos.gradeBento}>
                <Espaco tamanho="duplo"><IntegridadeDados integridade={(dados.integridade) ?? undefined} /></Espaco>
                <Espaco><ChecklistGovernanca dados={dados} /></Espaco>
                <Espaco tamanho="triplo"><ComparativoUnidades unidades={(dados.unidades) ?? undefined} nivel={(dados.escopo?.nivel) ?? undefined} /></Espaco>
                <Espaco tamanho="triplo"><MatrizUnidades unidades={(dados.unidades) ?? undefined} nivel={(dados.escopo?.nivel) ?? undefined} /></Espaco>
                {!perfil.presidente && (
                  <Espaco tamanho="triplo">
                    <AuditoriaRecente
                      atividades={atividades}
                      podeAbrirLogs={perfil.podeAcessarAuditoria}
                      aoNavegar={acoes.navegar}
                    />
                  </Espaco>
                )}
              </div>
              <NotasMetodologicas notas={(dados.notasMetodologicas) ?? undefined} geradoEm={(dados.geradoEm) ?? undefined} />
            </section>
          )}
        </>
      )}
    </main>
  );
};