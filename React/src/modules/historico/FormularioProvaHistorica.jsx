import React, { useEffect, useRef, useState } from 'react';
import { FileCheck2, Plus, RefreshCw, ShieldCheck, Trash2 } from 'lucide-react';
import { useAutenticacao } from '../auth/context.jsx';
import { eGestaoLocal } from '../../core/security/roles.js';
import { serializarLocalDate } from '../../core/http/datasHttp.js';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../../core/security/geracaoSessao.js';
import { CAMPOS_FOTOGRAFIA, CATEGORIAS_MOTIVO, DIAS_SEMANA } from '../../services/historico/dominioHistorico.js';
import { ServicoHistorico } from '../../services/historico/ServicoHistorico.js';
import { CampoHistorico as Campo } from './CampoHistorico.jsx';
import { DetalhesDados } from './ResultadosHistoricos.jsx';
import { usePreviaHistorica } from './usePreviaHistorica.js';
import estilos from './historico.module.css';

const inicial = () => ({
  dataEfetiva: '', ativo: '', versaoEsperada: '', motivo: '', evidencia: '', autorizacao: '',
  dataDocumento: '', referenciaSei: '', documentoComprobatorioId: '', finalidadeAcessoDocumento: '',
  autorizacaoAdministrativa: '', versaoContratoEsperada: '', eventoOrigemId: '', versaoAnteriorId: '', provaAnteriorId: '',
  recuperarPublicacaoOriginal: false, configuracaoCompletaComprovada: false, camposComprovados: [], fotografia: {}, composicoes: [],
  nomeComprovado: '', eventoOrigemRetificadoId: '', versaoPessoaEsperada: '', versaoVinculoEsperada: '',
  dataInicioEfetiva: '', dataFimEfetiva: '', descricaoComprovada: '', categoriaComprovada: '', versaoMotivoEsperada: '', modalidadeNome: 'correcao',
});
const novaComposicao = () => ({ origem: 'composicaoId', composicaoId: '', mapeamentoId: '', aditamentoComposicaoId: '',
  lotacaoNome: '', jornadaDescricao: '', cargoNome: '', lotacaoMunicipio: '', municipioComprovado: '', municipioEstado: '', jornadaComprovada: '',
  semana: DIAS_SEMANA.map(([dia]) => ({ dia, trabalha: '', entrada1: '', saida1: '', entrada2: '', saida2: '' })),
});
const SIM_NAO = [['', 'Ausência documentada'], ['true', 'Sim'], ['false', 'Não']];
const ORIGENS = [['composicaoId', 'Composição original da publicação'], ['mapeamentoId', 'Mapeamento de transferência'], ['aditamentoComposicaoId', 'Item de composição do aditamento']];
const NOMES_ID_ORIGEM = { composicaoId: 'ID da composição original', mapeamentoId: 'ID do mapeamento de transferência', aditamentoComposicaoId: 'ID do item de aditamento' };

const Fotografia = ({ formulario, mudar }) => <details className={estilos.grupoDetalhes}>
  <summary>Fotografia documentada · {formulario.configuracaoCompletaComprovada ? 21 : formulario.camposComprovados.length} de 21 campos comprovados</summary>
  <p className={estilos.ajuda}>Marque somente os campos abrangidos pela fonte. Campo desmarcado permanece desconhecido. Campo marcado e vazio declara ausência documentada, quando admitida para essa propriedade. Os valores não são copiados do cadastro atual.</p>
  <div className={estilos.gridCampos}>{CAMPOS_FOTOGRAFIA.map((campo) => {
    const comprovado = formulario.configuracaoCompletaComprovada || formulario.camposComprovados.includes(campo.nome);
    const opcoes = campo.tipo === 'boolean' ? SIM_NAO : campo.tipo === 'dia' ? [['', 'Ausência documentada'], ...DIAS_SEMANA] : campo.tipo === 'habilitacao' ? [['', 'Ausência documentada'], ['ATIVO', 'Ativo'], ['INATIVO', 'Inativo']] : undefined;
    return <div className={estilos.fotografiaCampo} key={campo.nome}>
      <label className={estilos.checkbox}><input type="checkbox" checked={comprovado} onChange={(evento) => mudar((estado) => ({
        ...estado, configuracaoCompletaComprovada: false,
        camposComprovados: evento.target.checked ? [...new Set([...estado.camposComprovados, campo.nome])] : (estado.configuracaoCompletaComprovada ? CAMPOS_FOTOGRAFIA.map(({ nome }) => nome) : estado.camposComprovados).filter((nome) => nome !== campo.nome),
      }))} />Comprovado: {campo.rotulo}</label>
      <Campo rotulo={campo.rotulo} valor={formulario.fotografia[campo.nome]} aoMudar={(valor) => mudar((estado) => ({ ...estado, fotografia: { ...estado.fotografia, [campo.nome]: valor } }))}
        disabled={!comprovado} tipo={campo.tipo === 'id' || campo.tipo === 'integer' ? 'number' : campo.tipo === 'date' ? 'date' : 'text'}
        opcoes={opcoes} min={campo.tipo === 'id' ? 1 : campo.min} max={campo.max} step={campo.tipo === 'id' || campo.tipo === 'integer' ? 1 : undefined} maxLength={campo.limite} />
    </div>;
  })}</div>
</details>;

const Composicoes = ({ formulario, mudar }) => {
  const alterar = (indice, campos) => mudar((estado) => ({ ...estado, composicoes: estado.composicoes.map((item, i) => i === indice ? { ...item, ...campos } : item) }));
  return <details className={estilos.grupoDetalhes}>
    <summary>Configurações de origem · {formulario.composicoes.length} item(ns)</summary>
    <p className={estilos.ajuda}>Cada item identifica uma composição da publicação, um mapeamento de transferência ou um item originário de aditamento. Denominações, município e grade semanal são provas independentes. As quantidades preservadas pelo ato são conferidas pelo servidor.</p>
    <button type="button" className={estilos.adicionar} onClick={() => mudar((estado) => ({ ...estado, composicoes: [...estado.composicoes, novaComposicao()] }))}><Plus size={16} aria-hidden="true" /> Adicionar configuração comprovada</button>
    {formulario.composicoes.map((item, indice) => <section key={indice} className={estilos.configuracao} aria-label={`Configuração ${indice + 1}`}>
      <header><h4>Configuração {indice + 1}</h4><button type="button" className={estilos.secundario} onClick={() => mudar((estado) => ({ ...estado, composicoes: estado.composicoes.filter((_, i) => i !== indice) }))}><Trash2 size={15} aria-hidden="true" /> Remover item {indice + 1}</button></header>
      <div className={estilos.gridCampos}>
        <Campo rotulo={`Origem da configuração ${indice + 1}`} valor={item.origem} opcoes={ORIGENS} aoMudar={(origem) => alterar(indice, { origem, composicaoId: '', mapeamentoId: '', aditamentoComposicaoId: '' })} />
        <Campo rotulo={`${NOMES_ID_ORIGEM[item.origem]} · item ${indice + 1}`} tipo="number" min="1" step="1" valor={item[item.origem]} aoMudar={(valor) => alterar(indice, { [item.origem]: valor })} />
        <Campo rotulo={`Nome histórico da lotação · item ${indice + 1}`} valor={item.lotacaoNome} maxLength={150} aoMudar={(lotacaoNome) => alterar(indice, { lotacaoNome })} />
        <Campo rotulo={`Descrição histórica da jornada · item ${indice + 1}`} valor={item.jornadaDescricao} maxLength={150} aoMudar={(jornadaDescricao) => alterar(indice, { jornadaDescricao })} />
        <Campo rotulo={`Nome histórico do cargo · item ${indice + 1}`} valor={item.cargoNome} maxLength={150} aoMudar={(cargoNome) => alterar(indice, { cargoNome })} />
        <Campo rotulo={`Município na fonte · item ${indice + 1}`} valor={item.municipioEstado} opcoes={[
          ['', 'Desconhecido / não declarado'], ['false', 'Não comprovado'], ['true', 'Município comprovado'], ['ausente', 'Ausência de município comprovada'],
        ]} aoMudar={(municipioEstado) => alterar(indice, { municipioEstado, municipioComprovado: municipioEstado === 'ausente' ? 'true' : municipioEstado, lotacaoMunicipio: municipioEstado === 'true' ? item.lotacaoMunicipio : '' })} />
        <Campo rotulo={`Município documentado · item ${indice + 1}`} valor={item.lotacaoMunicipio} disabled={item.municipioEstado !== 'true'} maxLength={180} aoMudar={(lotacaoMunicipio) => alterar(indice, { lotacaoMunicipio })} />
        <Campo rotulo={`Grade semanal comprovada · item ${indice + 1}`} valor={item.jornadaComprovada} opcoes={[
          ['', 'Desconhecida / não declarada'], ['false', 'Não comprovada'], ['true', 'Comprovada pela fonte'],
        ]} aoMudar={(jornadaComprovada) => alterar(indice, { jornadaComprovada })} />
      </div>
      {item.jornadaComprovada === 'true' && <div className={`${estilos.rolagemTabela} ${estilos.grade}`}>
        <p className={estilos.ajuda}>Declare todos os sete dias. Dias não trabalhados ficam sem horários. A comprovação da grade independe do município.</p>
        <table aria-label={`Grade semanal da configuração ${indice + 1}`}><thead><tr><th scope="col">Dia</th><th scope="col">Há trabalho?</th><th scope="col">Entrada 1</th><th scope="col">Saída 1</th><th scope="col">Entrada 2</th><th scope="col">Saída 2</th></tr></thead>
          <tbody>{item.semana.map((dia, posicao) => {
            const nome = DIAS_SEMANA.find(([chave]) => chave === dia.dia)[1];
            const mudarDia = (campos) => alterar(indice, { semana: item.semana.map((registro, i) => i === posicao ? { ...registro, ...campos } : registro) });
            return <tr key={dia.dia}><th scope="row">{nome}</th><td><select aria-label={`Trabalho em ${nome} · item ${indice + 1}`} value={dia.trabalha} onChange={(evento) => mudarDia({ trabalha: evento.target.value, ...(evento.target.value !== 'true' ? { entrada1: '', saida1: '', entrada2: '', saida2: '' } : {}) })}><option value="">Declarar</option><option value="true">Sim</option><option value="false">Não</option></select></td>
              {['entrada1', 'saida1', 'entrada2', 'saida2'].map((campo, ordem) => <td key={campo}><input type="time" aria-label={`${['Entrada 1', 'Saída 1', 'Entrada 2', 'Saída 2'][ordem]} em ${nome} · item ${indice + 1}`} value={dia[campo]} disabled={dia.trabalha !== 'true'} onChange={(evento) => mudarDia({ [campo]: evento.target.value })} /></td>)}
            </tr>;
          })}</tbody>
        </table>
      </div>}
    </section>)}
  </details>;
};

export const FormularioProvaHistorica = ({ tipo, alvo, aoSucesso, aoOcupacao }) => {
  const geracaoSessao = obterGeracaoSessao();
  const permitido = eGestaoLocal(useAutenticacao()?.user);
  const [formulario, setFormulario] = useState(inicial);
  const [versoesAtuais, setVersoesAtuais] = useState(null);
  const [erroVersoes, setErroVersoes] = useState(null);
  const [carregandoVersoes, setCarregandoVersoes] = useState(false);
  const versoesRef = useRef(null);
  const assinaturaAlvo = JSON.stringify([tipo, alvo]);
  useEffect(() => { setFormulario(inicial()); }, [tipo]);
  useEffect(() => {
    setVersoesAtuais(null); setErroVersoes(null); setCarregandoVersoes(false);
    return () => { versoesRef.current?.abort(); versoesRef.current = null; };
  }, [assinaturaAlvo]);
  useEffect(() => {
    const sinal = obterSinalSessao(geracaoSessao);
    const encerrar = () => {
      versoesRef.current?.abort(); versoesRef.current = null;
      setFormulario(inicial());
      setVersoesAtuais(null); setErroVersoes(null); setCarregandoVersoes(false);
    };
    sinal.addEventListener('abort', encerrar, { once: true });
    if (sinal.aborted) encerrar();
    return () => sinal.removeEventListener('abort', encerrar);
  }, [geracaoSessao]);
  const dados = { ...formulario, ...(tipo === 'nome' ? { vinculoId: alvo.vinculoId } : {}), ...(tipo === 'motivo' ? { contratoId: alvo.contratoId } : {}) };
  const fluxo = usePreviaHistorica({ tipo, alvo, formulario: dados, permitido, aoSucesso });
  useEffect(() => { aoOcupacao?.(fluxo.processando === 'confirmacao'); return () => aoOcupacao?.(false); }, [fluxo.processando, aoOcupacao]);
  const mudar = (proximo) => {
    versoesRef.current?.abort(); versoesRef.current = null; setCarregandoVersoes(false);
    fluxo.invalidar(); setFormulario(proximo);
  };
  const carregarVersoes = async () => {
    if (!sessaoAtual(geracaoSessao) || !permitido || versoesRef.current || fluxo.processando || fluxo.previa || fluxo.recibo) return;
    const controlador = new AbortController();
    versoesRef.current = controlador; setCarregandoVersoes(true); setErroVersoes(null);
    try {
      const atuais = await ServicoHistorico.obterVersoesAtuais(tipo, alvo, { signal: controlador.signal });
      if (!sessaoAtual(geracaoSessao) || controlador.signal.aborted || versoesRef.current !== controlador) return;
      fluxo.invalidar();
      setFormulario((estado) => ({ ...estado, ...atuais }));
      setVersoesAtuais(atuais);
    } catch (falha) {
      if (sessaoAtual(geracaoSessao) && !controlador.signal.aborted && versoesRef.current === controlador && !falha?.canceled) setErroVersoes(falha?.message || 'Não foi possível obter as versões atuais.');
    } finally {
      if (versoesRef.current === controlador) { versoesRef.current = null; setCarregandoVersoes(false); }
    }
  };
  const campo = (nome, rotulo, props = {}) => <Campo key={nome} rotulo={rotulo} valor={formulario[nome]} aoMudar={(valor) => mudar((estado) => ({ ...estado, [nome]: valor }))} {...props} />;
  const hoje = serializarLocalDate(new Date());
  const numero = { tipo: 'number', min: 0, step: 1 };
  const identificador = { tipo: 'number', min: 1, step: 1 };
  const titulo = tipo === 'catalogo' ? 'Registrar disponibilidade documentada' : 'Incorporar uma fonte histórica';
  if (!permitido) return <p className={estilos.aviso}>A incorporação de fontes está disponível ao Gestor Local autorizado. Seu perfil mantém as consultas permitidas pelo servidor.</p>;

  return <form aria-label="Fonte histórica" className={estilos.formulario} noValidate onSubmit={(evento) => { evento.preventDefault(); fluxo.confirmar(); }}>
    <header><div className={estilos.etapa}>Fonte → prévia → confirmação</div><h2 className={estilos.cabecalhoCartao}>{titulo}</h2>
      <p className={estilos.ajuda}>{tipo === 'catalogo' ? 'Documente o estado e a data do ato. A prévia confere efeitos e preserva transições posteriores.' : 'Informe valores conferidos na fonte e o alcance do documento. O servidor exige custódia ativa e concessão vigente INCORPORACAO_PROVA_HISTORICA para o contrato.'}</p></header>
    <div className={estilos.contexto}><strong>{tipo === 'contrato' ? 'Contrato' : tipo === 'catalogo' ? 'Item do catálogo' : tipo === 'nome' ? 'Pessoa' : 'Motivo'} #{alvo.id}</strong>
      {tipo === 'nome' && <span>Vínculo #{alvo.vinculoId} · mesma identidade canônica</span>}
      {tipo === 'motivo' && <span>Contrato #{alvo.contratoId}</span>}{tipo === 'catalogo' && <span>{alvo.tipoCatalogo}</span>}</div>

    <section className={estilos.aviso} aria-label="Controle de concorrência atual">
      <header className={estilos.cabecalhoCartao}><h3>Controle de concorrência atual</h3><button type="button" className={estilos.secundario} disabled={carregandoVersoes || Boolean(fluxo.processando) || Boolean(fluxo.previa) || Boolean(fluxo.recibo)} onClick={carregarVersoes}>{carregandoVersoes ? 'Carregando versões…' : 'Carregar versões atuais'}</button></header>
      <p className={estilos.ajuda}>Esta leitura preenche apenas as versões esperadas dos cadastros atuais. A fonte histórica e seus valores permanecem os declarados no formulário. Após conflito, carregue as versões e gere uma nova prévia.</p>
      {versoesAtuais && <DetalhesDados dados={versoesAtuais} />}
      {erroVersoes && <div role="alert" className={estilos.erro}>{erroVersoes}</div>}
    </section>

    <fieldset disabled={fluxo.processando === 'confirmacao'}>
      <legend>{tipo === 'catalogo' ? 'Ato de disponibilidade' : 'Conteúdo e alcance da fonte'}</legend>
      <div className={estilos.gridCampos}>
        {tipo === 'catalogo' && <>
          {campo('dataEfetiva', 'Data efetiva da transição', { tipo: 'date', min: '1000-01-01', max: hoje, required: true })}
          {campo('ativo', 'Disponibilidade documentada', { opcoes: [['', 'Selecione o estado documentado'], ['true', 'Ativo'], ['false', 'Inativo']], required: true })}
          {campo('versaoEsperada', 'Versão esperada do catálogo', { ...numero, required: true })}
          {campo('evidencia', 'Evidência da transição', { tipo: 'textarea', maxLength: 1000, amplo: true, required: true })}
        </>}
        {tipo === 'contrato' && <>
          {campo('dataDocumento', 'Data do documento original', { tipo: 'date', max: hoje, required: true })}
          {campo('versaoContratoEsperada', 'Versão esperada do contrato', { ...numero, required: true, ajuda: 'Use a versão atual de concorrência do cadastro; ela difere do número da fotografia histórica.' })}
          {campo('eventoOrigemId', 'ID do evento de origem', { ...identificador, ajuda: 'Publicação, transferência ou aditamento que originou a configuração.' })}
          <label className={`${estilos.checkbox} ${estilos.campoAmplo}`}><input type="checkbox" checked={formulario.recuperarPublicacaoOriginal} onChange={(evento) => mudar((estado) => ({ ...estado, recuperarPublicacaoOriginal: evento.target.checked }))} />A fonte permite recuperar uma publicação original ausente.</label>
          {formulario.recuperarPublicacaoOriginal && <p className={`${estilos.aviso} ${estilos.campoAmplo}`}>A recuperação exige fonte integral de uma publicação ausente. Contrato nunca publicado segue o fluxo normal de publicação. O servidor confere a origem e a completude documental.</p>}
          <label className={`${estilos.checkbox} ${estilos.campoAmplo}`}><input type="checkbox" checked={formulario.configuracaoCompletaComprovada} onChange={(evento) => mudar((estado) => ({ ...estado, configuracaoCompletaComprovada: evento.target.checked, camposComprovados: evento.target.checked ? CAMPOS_FOTOGRAFIA.map(({ nome }) => nome) : estado.camposComprovados }))} />A fonte comprova a configuração integral, incluindo os campos e todas as composições originais.</label>
        </>}
        {tipo === 'nome' && <>
          {campo('modalidadeNome', 'Natureza da prova nominal', { opcoes: [['correcao', 'Correção documental de nome no período'], ['mudanca', 'Mudança legal posterior de nome']] })}
          {campo('nomeComprovado', 'Nome comprovado', { maxLength: 180, required: true })}
          {campo('versaoPessoaEsperada', 'Versão esperada da pessoa', { ...numero, required: true })}
          {campo('versaoVinculoEsperada', 'Versão esperada do vínculo', { ...numero, required: true })}
          <p className={`${estilos.aviso} ${estilos.campoAmplo}`}>{formulario.modalidadeNome === 'mudanca' ? 'Uma mudança legal posterior deve usar seu próprio intervalo comprovado e conservar a mesma Pessoa/Vínculo e os nomes dos períodos anteriores.' : 'Uma correção documental declara o intervalo correto e relaciona a prova anterior ou o evento nominal que está sendo retificado.'}</p>
        </>}
        {tipo === 'motivo' && <>
          {campo('descricaoComprovada', 'Descrição comprovada do motivo', { maxLength: 160, required: true })}
          {campo('categoriaComprovada', 'Categoria comprovada', { opcoes: [['', 'Selecione a categoria documentada'], ...CATEGORIAS_MOTIVO], required: true })}
          {campo('versaoMotivoEsperada', 'Versão esperada do motivo', { ...numero, required: true })}
          {campo('versaoContratoEsperada', 'Versão esperada do contrato', { ...numero, required: true })}
        </>}
        {(tipo === 'nome' || tipo === 'motivo') && <>
          {campo('dataInicioEfetiva', 'Início do intervalo comprovado', { tipo: 'date', min: '1000-01-01', required: true })}
          {campo('dataFimEfetiva', 'Fim do intervalo comprovado', { tipo: 'date', min: formulario.dataInicioEfetiva || '1000-01-01', ajuda: 'Intervalo inclusivo. Vazio indica intervalo sem fim declarado.' })}
        </>}
      </div>
      {tipo === 'contrato' && <><Fotografia formulario={formulario} mudar={mudar} /><Composicoes formulario={formulario} mudar={mudar} /></>}
      {tipo !== 'catalogo' && <details className={estilos.grupoDetalhes} open>
        <summary>Identificação da fonte e cadeia documental</summary>
        <div className={estilos.gridCampos}>
          {campo('referenciaSei', 'Referência SEI', { maxLength: tipo === 'contrato' ? 200 : 1000, ajuda: 'Informe a referência SEI ou um documento comprobatório publicado.' })}
          {campo('documentoComprobatorioId', 'ID do documento comprobatório', identificador)}
          {campo('finalidadeAcessoDocumento', 'Finalidade de acesso ao documento', { maxLength: 1000 })}
          {campo('provaAnteriorId', 'ID da prova anterior', { ...identificador, ajuda: 'Preencha quando esta fonte retifica uma prova vigente.' })}
          {tipo === 'contrato' && campo('versaoAnteriorId', 'ID da fotografia anterior', identificador)}
          {tipo === 'nome' && campo('eventoOrigemRetificadoId', 'ID do evento nominal retificado', identificador)}
        </div>
      </details>}
      <div className={estilos.gridCampos} style={{ marginTop: '1rem' }}>
        {campo('motivo', 'Motivo / justificativa', { tipo: 'textarea', amplo: true, maxLength: tipo === 'contrato' ? 1500 : tipo === 'catalogo' ? 500 : 1000, required: true })}
        {campo(tipo === 'contrato' ? 'autorizacaoAdministrativa' : 'autorizacao', 'Autorização', { amplo: true, maxLength: tipo === 'contrato' ? 180 : 1000, required: true })}
      </div>
    </fieldset>

    {fluxo.erro && <div role="alert" className={estilos.erro}>{fluxo.erro}</div>}
    {fluxo.previa && <section className={estilos.previa} aria-label="Prévia da fonte">
      <h3><ShieldCheck size={18} aria-hidden="true" /> Impactos informados pelo servidor</h3>
      {fluxo.previa.efeito && <p>{fluxo.previa.efeito}</p>}
      {Array.isArray(fluxo.previa.impactos) && <ul>{fluxo.previa.impactos.map((impacto, indice) => <li key={indice}>{impacto}</li>)}</ul>}
      <details className={estilos.grupoDetalhes}><summary>Metadados da prévia</summary><DetalhesDados dados={Object.fromEntries(Object.entries(fluxo.previa).filter(([chave]) => chave !== 'impactos' && chave !== 'efeito'))} /></details>
      <label className={estilos.checkbox}><input type="checkbox" checked={fluxo.confirmarImpacto} disabled={Boolean(fluxo.processando) || fluxo.incerto} onChange={(evento) => fluxo.setConfirmarImpacto(evento.target.checked)} />Li os impactos e confirmo esta intenção.</label>
    </section>}
    {fluxo.recibo && <section className={estilos.sucesso} role="status"><strong>Registrado com sucesso no servidor.</strong><p>O recibo abaixo identifica a intenção incorporada. Consulte novamente para obter os efeitos no recorte desejado.</p><DetalhesDados dados={fluxo.recibo} /></section>}
    <div className={estilos.acoes}>
      <button type="button" className={estilos.secundario} disabled={Boolean(fluxo.processando) || Boolean(fluxo.recibo) || fluxo.incerto} onClick={fluxo.gerarPrevia}><RefreshCw size={16} aria-hidden="true" />{fluxo.processando === 'previa' ? 'Analisando fonte…' : 'Gerar prévia'}</button>
      <button type="submit" className={estilos.primario} disabled={!fluxo.previa || !fluxo.confirmarImpacto || Boolean(fluxo.processando) || Boolean(fluxo.recibo) || fluxo.incerto}><FileCheck2 size={16} aria-hidden="true" />{fluxo.processando === 'confirmacao' ? 'Aguardando confirmação do servidor…' : tipo === 'catalogo' ? 'Registrar transição' : 'Confirmar incorporação'}</button>
      {fluxo.incerto && <button type="button" className={estilos.primario} disabled={Boolean(fluxo.processando)} onClick={() => fluxo.confirmar(true)}>Repetir a mesma intenção</button>}
    </div>
  </form>;
};
