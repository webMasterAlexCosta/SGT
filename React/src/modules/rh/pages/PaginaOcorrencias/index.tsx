import { observarFalha } from '../../../../core/http/falhaObservada';
import { mensagemFalha } from "../../../../core/http/mensagemFalha";
import React, { useState, useEffect, useMemo, useRef } from 'react';
import { 
    Search, Plus, Trash2, User, 
    Clock, AlertTriangle, FileText, XCircle, Star, Ban,
    Calendar, X, Upload, Briefcase, File
} from 'lucide-react';

import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia.ts';
import { Botao, JanelaModal, CampoEntrada } from '../../../../core/ui/EnvolucroDica.tsx';
import { notificar } from '../../../../core/events.ts';
import { useConfirmacaoSenha } from '../../../../core/security/useConfirmacaoSenha.tsx';
import { useConsultaVigente } from '../../../../core/hooks/useConsultaVigente.ts';
import { obterGeracaoSessao } from '../../../../core/security/geracaoSessao.ts';
import { obterDataNoFusoNegocio } from '../../../../core/formatters/dataHoraNegocio.ts';
import { CONFIG } from '../../../../core/config.ts'; 
import estilos from './index.module.css';
import { useAutenticacao } from '../../../auth/context.tsx';
import { ePresidente, eUsuarioOperacional, podeGerenciarOcorrencias } from '../../../../core/security/roles.ts';
import { AnexoOcorrencia } from './AnexoOcorrencia.tsx';
import { ConsultaOcorrenciaEspecifica } from './ConsultaOcorrenciaEspecifica.tsx';

export const PaginaOcorrencias = () => {
    const { user } = useAutenticacao();
    if (ePresidente(user)) return (
        <div className={estilos.containerGeral}>
            <header className={estilos.cabecalhoPagina}>
                <div className={estilos.blocoTituloPagina}>
                    <span className={estilos.iconeCabecalhoPagina}><FileText size={23} /></span>
                    <div><h1>Ocorrências</h1><p>Consulte uma ocorrência por ID conforme a autorização recebida.</p></div>
                </div>
            </header>
            <ConsultaOcorrenciaEspecifica usuario={user} />
        </div>
    );
    return <GestaoOcorrencias />;
};

const GestaoOcorrencias = () => {
    const { user } = useAutenticacao();
    const podeEditar = podeGerenciarOcorrencias(user);
    const consultaOperacional = eUsuarioOperacional(user);
    const [termoBusca, setTermoBusca] = useState('');
    const [empregados, setEmpregados] = useState<(import("../../../../contracts/java").EmpregadoDTO[] | import("../../../../contracts/java").EmpregadoFrequenciaDTO[])>([]);
    const [empregadoSelecionado, setEmpregadoSelecionado] = useState<(import("../../../../contracts/java").EmpregadoDTO | import("../../../../contracts/java").EmpregadoFrequenciaDTO) | null>(null);
    const [carregandoEmpregados, setCarregandoEmpregados] = useState(false);
    const [erroBusca, setErroBusca] = useState('');
    const [buscaConcluida, setBuscaConcluida] = useState(false);
    const [ocorrencias, setOcorrencias] = useState<(import("../../../../contracts/java").OcorrenciaResumoDTO[])>([]);
    const [erroHistorico, setErroHistorico] = useState('');
    const [estadoHistorico, setEstadoHistorico] = useState('inicial');
    const [modalAberto, setModalAberto] = useState(false);
    
    const referenciaEntradaArquivo = useRef<HTMLElementTagNameMap['input'] | null>(null);
    
    const obterDataHojeNegocial = () => obterDataNoFusoNegocio() || '';

    const [dataConsulta, setDataConsulta] = useState(obterDataHojeNegocial);
    const [escopoSelecionado, setEscopoSelecionado] = useState('');
    const escoposOperacionais = useMemo(() => {
        const unicos = new Map<string, { chave: string; lotacaoId: number; turnoId: number; rotulo: string }>();
        for (const vinculo of Array.isArray(user?.vinculosGestao) ? user.vinculosGestao : []) {
            const lotacaoId = Number(vinculo.lotacaoId ?? vinculo.lotacao?.id);
            const turnoId = Number(vinculo.jornadaId ?? vinculo.jornada?.id);
            if (!Number.isSafeInteger(lotacaoId) || lotacaoId <= 0 || !Number.isSafeInteger(turnoId) || turnoId <= 0) continue;
            const chave = `${lotacaoId}:${turnoId}`;
            unicos.set(chave, {
                chave, lotacaoId, turnoId,
                rotulo: `${vinculo.lotacaoNome || vinculo.lotacao?.label || vinculo.lotacao?.nome || `Lotação ${lotacaoId}`} — ${vinculo.jornadaNome || vinculo.jornada?.label || vinculo.jornada?.descricao || `Turno ${turnoId}`}`,
            });
        }
        return Array.from(unicos.values());
    }, [user?.vinculosGestao]);
    const chaveEscopo = escopoSelecionado || (escoposOperacionais.length === 1 ? escoposOperacionais[0].chave : '');
    const escopoAtivo = escoposOperacionais.find((escopo) => escopo.chave === chaveEscopo);
    const contextoBuscaDisponivel = !consultaOperacional || Boolean(dataConsulta && escopoAtivo);
    const mensagemErro = (erro: unknown, fallback: string) => mensagemFalha(erro, fallback);
    const geracaoSessao = obterGeracaoSessao();
    const contextoConsulta = JSON.stringify([consultaOperacional, dataConsulta, chaveEscopo, user?.id, user?.perfil, user?.autoridade, geracaoSessao]);
    const contextoColaborador = JSON.stringify([contextoConsulta, empregadoSelecionado?.id]);
    const consultasBusca = useConsultaVigente(JSON.stringify([contextoColaborador, termoBusca]));
    const consultasHistorico = useConsultaVigente(contextoColaborador);
    const operacoesOcorrencia = useConsultaVigente(contextoColaborador);

    useEffect(() => {
        setEmpregadoSelecionado(null);
        setEmpregados([]);
        setOcorrencias([]);
        setErroBusca('');
        setBuscaConcluida(false);
        setModalAberto(false);
    }, [contextoConsulta]);

    const formularioInicial = { 
        motivo: 'ATRASO', 
        observacao: '', 
        dataOcorrencia: obterDataHojeNegocial(),
        dias: '',
        tipoRo: '',
        numeroSei: '',
        arquivos: [] as File[] 
    };
    const [dadosFormulario, setDadosFormulario] = useState(formularioInicial);

    const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

    const obterConfiguracaoTipo = (tipo: (string | null) | (string)) => {
        const chave = (tipo ?? '').toString().trim().toUpperCase();
        const configuracoes: Readonly<Record<string, { cor: string; fundo: string; borda: string; anel: string; icone: React.ReactNode; rotulo: string } | undefined>> = {
            'ATRASO': { cor: estilos.tipoAmbarTexto, fundo: estilos.tipoAmbarFundo, borda: estilos.tipoAmbarBorda, anel: estilos.tipoAmbarAnel, icone: <Clock size={18}/>, rotulo: 'Atraso' },
            'FALTA': { cor: estilos.tipoVermelhoTexto, fundo: estilos.tipoVermelhoFundo, borda: estilos.tipoVermelhoBorda, anel: estilos.tipoVermelhoAnel, icone: <XCircle size={18}/>, rotulo: 'Falta' },
            'ADVERTENCIA': { cor: estilos.tipoLaranjaTexto, fundo: estilos.tipoLaranjaFundo, borda: estilos.tipoLaranjaBorda, anel: estilos.tipoLaranjaAnel, icone: <AlertTriangle size={18}/>, rotulo: 'Advertência' },
            'SUSPENSAO': { cor: estilos.tipoRosaTexto, fundo: estilos.tipoRosaFundo, borda: estilos.tipoRosaBorda, anel: estilos.tipoRosaAnel, icone: <Ban size={18}/>, rotulo: 'Suspensão' },
            'ELOGIO': { cor: estilos.tipoEsmeraldaTexto, fundo: estilos.tipoEsmeraldaFundo, borda: estilos.tipoEsmeraldaBorda, anel: estilos.tipoEsmeraldaAnel, icone: <Star size={18}/>, rotulo: 'Elogio' },
            'ATESTADO': { cor: estilos.tipoAzulTexto, fundo: estilos.tipoAzulFundo, borda: estilos.tipoAzulBorda, anel: estilos.tipoAzulAnel, icone: <FileText size={18}/>, rotulo: 'Atestado' },
        };
        return configuracoes[chave] || { cor: estilos.tipoCinzaTexto, fundo: estilos.tipoCinzaFundo, borda: estilos.tipoCinzaBorda, anel: estilos.tipoCinzaAnel, icone: <FileText size={18}/> , rotulo: chave || 'Evento' };
    };

    const obterIniciais = (nome: (string | null)) => nome ? nome.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() : '??';

    const estatisticas = useMemo(() => {
        if (!ocorrencias.length) return null;
        return {
            total: ocorrencias.length,
            suspensoes: ocorrencias.filter(o => o.motivo === 'SUSPENSAO').length,
            faltas: ocorrencias.filter(o => o.motivo === 'FALTA').length,
            atestados: ocorrencias.filter(o => o.motivo === 'ATESTADO').length,
            ultimo: ocorrencias[0]?.dataOcorrencia
        };
    }, [ocorrencias]);

    useEffect(() => {
        setEmpregados([]);
        setErroBusca('');
        setBuscaConcluida(false);
        setCarregandoEmpregados(false);
        if (!termoBusca || termoBusca.length < 2 || !contextoBuscaDisponivel) return;
        if (consultaOperacional && !escopoAtivo) return;

        if (empregadoSelecionado && termoBusca === empregadoSelecionado.nome) {
            return;
        }

        const consulta = consultasBusca.iniciar();
        const temporizador = setTimeout(() => {
            if (!consulta.atual()) return;
            setCarregandoEmpregados(true);
            ServicoOcorrencia.buscarEmpregados(termoBusca,
                { signal: consulta.signal, skipLoading: true, silentError: true },
                consultaOperacional && escopoAtivo ? { dataRef: dataConsulta, lotacaoId: escopoAtivo.lotacaoId, turnoId: escopoAtivo.turnoId } : undefined)
               .then(res => {
                   if (!consulta.atual()) return;
                   setEmpregados(res);
                   setBuscaConcluida(true);
               })
               .catch((errOriginal: unknown) => {
      const err = observarFalha(errOriginal);

                   if (!consulta.atual() || err?.canceled) return;
                   setErroBusca(mensagemErro(err, 'Não foi possível consultar colaboradores neste contexto.'));
               })
               .finally(() => {
                   if (consulta.atual()) {
                       setCarregandoEmpregados(false);
                   }
               });
        }, CONFIG.SEARCH_DEBOUNCE_MS); 
        return () => {
            consultasBusca.invalidar();
            clearTimeout(temporizador);
        };
    }, [consultasBusca, contextoConsulta, termoBusca, empregadoSelecionado, contextoBuscaDisponivel, consultaOperacional, dataConsulta, escopoAtivo?.lotacaoId, escopoAtivo?.turnoId]);

    useEffect(() => {
        const consulta = consultasHistorico.iniciar();
        setOcorrencias([]);
        setErroHistorico('');
        setEstadoHistorico('inicial');
        if (empregadoSelecionado) {
            setEstadoHistorico('carregando');
            ServicoOcorrencia.listarPorEmpregado(empregadoSelecionado.id, { signal: consulta.signal, skipLoading: true, silentError: true })
               .then(res => {
                   if (!consulta.atual()) return;
                   setOcorrencias(res);
                   setEstadoHistorico('sucesso');
               })
               .catch((errOriginal: unknown) => {
      const err = observarFalha(errOriginal);

                   if (!consulta.atual() || err?.canceled) return;
                   setErroHistorico(mensagemErro(err, 'Não foi possível consultar o histórico deste colaborador.'));
                   setEstadoHistorico('erro');
               });
        }
        return consultasHistorico.invalidar;
    }, [consultasHistorico, contextoConsulta, empregadoSelecionado]);

    const lidarComSalvar = async (evento: React.FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        if (!empregadoSelecionado || !podeEditar) return;

        const validacao = ServicoOcorrencia.validar(dadosFormulario);
        if (!validacao.valido) {
            notificar('error', validacao.mensagem);
            return;
        }

        const dadosEnvio = ServicoOcorrencia.prepararCarga(
            empregadoSelecionado.id,
            dadosFormulario,
        );
        const msgConfirmacao = ServicoOcorrencia.obterMensagemConfirmacao(dadosFormulario);
        const operacao = operacoesOcorrencia.iniciar();

        await confirmarComSenha({
            titulo: 'Confirmar ocorrência',
            mensagem: msgConfirmacao,
            acao: async (senhaConfirmacao) => {
                if (!operacao.atual()) return false;
                let consultaRecarga: ReturnType<typeof consultasHistorico.iniciar> | undefined;
                try {
                    await ServicoOcorrencia.criar(dadosEnvio, { senhaConfirmacao, silentError: true });
                    if (!operacao.atual()) return false;
                    notificar('success', 'Ocorrência registrada. Os efeitos de frequência foram processados pelo Java.');
                    
                    setModalAberto(false);
                    setDadosFormulario({ ...formularioInicial, dataOcorrencia: obterDataHojeNegocial() });
                    if (referenciaEntradaArquivo.current) referenciaEntradaArquivo.current.value = "";
                    
                    consultaRecarga = consultasHistorico.iniciar();
                    const res = await ServicoOcorrencia.listarPorEmpregado(empregadoSelecionado.id, { signal: consultaRecarga.signal, silentError: true });
                    if (!operacao.atual() || !consultaRecarga.atual()) return false;
                    setOcorrencias(res);
                    setErroHistorico('');
                    setEstadoHistorico('sucesso');
                } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

                    if (!operacao.atual() || (consultaRecarga && !consultaRecarga.atual())) return false;
                    console.error("Erro ao salvar:", erroOriginal);
                    notificar('error', erro?.message || 'Não foi possível registrar a ocorrência.');
                    throw erroOriginal; 
                }
            }
        });
    };

    const lidarComExcluir = async (id: (number | null), motivo: (string | null)) => {
        if (!podeEditar || !empregadoSelecionado) return;
        const mensagemExclusao = motivo === 'FALTA' || motivo === 'ATESTADO' || motivo === 'SUSPENSAO' 
            ? 'A ocorrência será excluída e o Java reconciliará novamente a frequência afetada.'
            : 'A ocorrência será excluída. O Java decidirá os efeitos associados ao registro.';

        const operacao = operacoesOcorrencia.iniciar();
        await confirmarComSenha({
            titulo: 'Excluir Ocorrência',
            mensagem: mensagemExclusao,
            acao: async (senhaConfirmacao) => {
                if (!operacao.atual()) return false;
                try {
                    await ServicoOcorrencia.excluir(id, { senhaConfirmacao, silentError: true });
                    if (!operacao.atual()) return false;
                    setOcorrencias(estadoAnterior => estadoAnterior.filter(o => o.id !== id));
                    notificar('success', 'Ocorrência removida e frequência reconciliada pelo Java.');
                } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

                    if (!operacao.atual()) return false;
                    notificar('error', erro?.message || 'Não foi possível remover o registro.');
                    throw erroOriginal; 
                }
            }
        });
    };

    const limparSelecao = () => {
        setEmpregadoSelecionado(null);
        setTermoBusca('');
        setEmpregados([]);
    };

    const fecharFormulario = () => {
        operacoesOcorrencia.invalidar();
        setModalAberto(false);
    };

    return (
        <div className={estilos.containerGeral}>
            <ModalConfirmacao />

            <header className={estilos.cabecalhoPagina}>
                <div className={estilos.blocoTituloPagina}>
                    <span className={estilos.iconeCabecalhoPagina}>
                        <FileText size={23} />
                    </span>
                    <div>
                        <span className={estilos.sobretituloPagina}>Gestão funcional</span>
                        <h1>Ocorrências</h1>
                        <p>Consulte o histórico e registre eventos funcionais por colaborador.</p>
                    </div>
                </div>
                <div className={estilos.resumoCabecalho}>
                    <span>{empregadoSelecionado ? 'Colaborador selecionado' : 'Consulta funcional'}</span>
                    <strong>{empregadoSelecionado?.nome || 'Localize um colaborador'}</strong>
                </div>
            </header>

            <ConsultaOcorrenciaEspecifica usuario={user} />

            <div className={estilos.gradePrincipal}>
            <div className={estilos.colunaLateral}>
                <div className={estilos.cartaoBusca}>
                    <h2 className={estilos.tituloBusca}>
                        <Search size={20} className={estilos.iconeAzul}/> Localizar
                    </h2>
                    {consultaOperacional && (
                        <div className={estilos.filtrosOperacionais}>
                            <label htmlFor="dataConsultaOcorrencias">Data de referência da busca</label>
                            <input id="dataConsultaOcorrencias" type="date" value={dataConsulta} onChange={evento => setDataConsulta(evento.target.value)} />
                            <label htmlFor="escopoConsultaOcorrencias">Lotação e turno</label>
                            <select id="escopoConsultaOcorrencias" value={chaveEscopo} onChange={evento => setEscopoSelecionado(evento.target.value)}>
                                <option value="">Selecione o contexto</option>
                                {escoposOperacionais.map(escopo => <option key={escopo.chave} value={escopo.chave}>{escopo.rotulo}</option>)}
                            </select>
                            <p>A busca considera os colaboradores da lotação e do turno na data selecionada. O histórico permanece sujeito ao seu acesso atual.</p>
                            {escoposOperacionais.length === 0 && <p role="status">A consulta exige uma lotação e turno disponíveis no seu perfil.</p>}
                        </div>
                    )}
                    <div className={estilos.envoltorioRelativo}>
                        <div className={estilos.envoltorioEntradaBusca}>
                            <input 
                                type="text" 
                                placeholder="Nome ou CPF..." 
                                aria-label="Nome ou CPF do colaborador"
                                disabled={!contextoBuscaDisponivel}
                                className={estilos.entradaBusca}
                                value={termoBusca}
                                onChange={(evento) => {
                                    if(empregadoSelecionado) setEmpregadoSelecionado(null);
                                    setTermoBusca(evento.target.value);
                                }}
                            />
                            {termoBusca && (
                                <button onClick={limparSelecao} className={estilos.botaoLimparBusca}>
                                    <X size={18} />
                                </button>
                            )}
                        </div>
                        
                        {!empregadoSelecionado && empregados.length > 0 && (
                            <div className={estilos.listaSugestoes}>
                                {empregados.map(emp => (
                                    <button 
                                        key={emp.id} 
                                        className={estilos.itemSugestao}
                                        onClick={() => {
                                            setEmpregadoSelecionado(emp);
                                            setTermoBusca(emp.nome || "");
                                            setEmpregados([]);
                                        }}
                                    >
                                        <div className={estilos.avatarIniciais}>
                                            {obterIniciais(emp.nome)}
                                        </div>
                                        <div>
                                            <div className={estilos.nomeSugestao}>{emp.nome}</div>
                                            <div className={estilos.detalhesSugestao}>
                                                <span className={estilos.cargoSugestao}>{emp.cargoNome || 'Sem Cargo'}</span>
                                                {emp.cpf && <span className={estilos.cpfSugestao}>{emp.cpf}</span>}
                                            </div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}
                        {carregandoEmpregados && <div className={estilos.indicadorCarregando}></div>}
                        {erroBusca && <p role="alert" className={estilos.mensagemErro}>{erroBusca}</p>}
                        {!empregadoSelecionado && buscaConcluida && empregados.length === 0 && <p role="status" className={estilos.textoSemRegistros}>Nenhum colaborador encontrado neste contexto.</p>}
                    </div>
                </div>

                {empregadoSelecionado && (
                    <div className={estilos.cartaoPerfil}>
                        <div className={estilos.fundoDecorativoPerfil}></div>
                        <div className={estilos.conteudoPerfil}>
                            <div className={estilos.cabecalhoPerfil}>
                                <div className={estilos.avatarGrande}>
                                    {obterIniciais(empregadoSelecionado.nome)}
                                </div>
                                <span className={estilos.matriculaPerfil}>
                                    {empregadoSelecionado.matricula || 'N/A'}
                                </span>
                            </div>
                            <h3 className={estilos.nomePerfil}>{empregadoSelecionado.nome}</h3>
                            <div className={estilos.cargoPerfil}>
                                <Briefcase size={14} />
                                <span>{empregadoSelecionado.cargoNome || 'Cargo não definido'}</span>
                            </div>
                            {podeEditar && <button 
                                onClick={() => { 
                                    setDadosFormulario({ ...formularioInicial, dataOcorrencia: obterDataHojeNegocial() });
                                    setModalAberto(true); 
                                }} 
                                className={estilos.botaoAcaoPrincipal}
                            >
                                <Plus size={18} className={estilos.iconeAcaoPrincipal}/> Registrar Ocorrência
                            </button>}
                        </div>
                    </div>
                )}
            </div>

            <div className={estilos.areaConteudo}>
                {!empregadoSelecionado ? (
                    <div className={estilos.estadoVazioNaoSelecionado}>
                        <span className={estilos.iconeVazioContainer}>
                            <User size={30} className={estilos.iconeEstadoVazio}/>
                        </span>
                        <h3>Selecione um colaborador</h3>
                        <p>Use a busca ao lado para visualizar ocorrências, documentos e o histórico funcional.</p>
                    </div>
                ) : (
                    <div className={estilos.containerHistorico}>
                        {estatisticas && (
                            <div className={estilos.gradeEstatisticas}>
                                <div className={estilos.cartaoEstatisticaPadrao}>
                                    <span className={estilos.rotuloEstatistica}>Total</span>
                                    <div className={estilos.valorEstatistica}>{estatisticas.total}</div>
                                </div>
                                <div className={estilos.cartaoEstatisticaAzul}>
                                    <span className={estilos.rotuloEstatisticaAzul}>Atestados</span>
                                    <div className={estilos.valorEstatisticaAzul}>{estatisticas.atestados}</div>
                                </div>
                                <div className={estilos.cartaoEstatisticaRosa}>
                                    <span className={estilos.rotuloEstatisticaRosa}>Suspensões</span>
                                    <div className={estilos.valorEstatisticaRosa}>{estatisticas.suspensoes}</div>
                                </div>
                                <div className={estilos.cartaoEstatisticaPadrao}>
                                    <span className={estilos.rotuloEstatistica}>Última</span>
                                    <div className={estilos.valorDataPequeno}>
                                        {estatisticas.ultimo ? new Date(estatisticas.ultimo + 'T12:00:00').toLocaleDateString('pt-BR', {day:'2-digit', month:'short'}) : '-'}
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className={estilos.cartaoHistorico}>
                            <div className={estilos.cabecalhoHistorico}>
                                <h3 className={estilos.tituloHistorico}>
                                    <FileText className={estilos.iconeAzul} size={20}/> Histórico funcional
                                </h3>
                                {estadoHistorico === 'sucesso' && <span className={estilos.contadorHistorico}>{ocorrencias.length} registro(s)</span>}
                            </div>

                            {estadoHistorico === 'carregando' && <p role="status" className={estilos.textoSemRegistros}>Consultando histórico...</p>}
                            {erroHistorico && <p role="alert" className={estilos.mensagemErro}>{erroHistorico}</p>}
                            {estadoHistorico === 'sucesso' && (ocorrencias.length === 0 ? (
                                <p className={estilos.textoSemRegistros}>Nenhum registro encontrado.</p>
                            ) : (
                                <div className={estilos.envoltorioLinhaTempo}>
                                    <div className={estilos.linhaConectora}></div>
                                    {ocorrencias.map((ocorrencia) => {
                                        const config = obterConfiguracaoTipo(ocorrencia.motivo);
                                        const totalDias = ocorrencia.quantidadeDias || ocorrencia.diasSuspensao || ocorrencia.diasAfastamento;
                                        const dataExibicao = new Date(ocorrencia.dataOcorrencia + 'T12:00:00');

                                        return (
                                            <div key={ocorrencia.id} className={`${estilos.itemLinhaTempo} ${estilos.itemLinhaTempoGrupoHover}`}>
                                                <div className={`${estilos.iconeStatusGeral} ${config.fundo} ${config.cor}`}>
                                                    {config.icone}
                                                </div>

                                                <div className={estilos.caixaOcorrencia}>
                                                    {podeEditar && <button onClick={() => lidarComExcluir(ocorrencia.id, ocorrencia.motivo)} className={estilos.botaoLixeira}>
                                                        <Trash2 size={18}/>
                                                    </button>}

                                                    <div className={estilos.cabecalhoCaixaOcorrencia}>
                                                        <span className={`${estilos.emblemaOcorrencia} ${config.fundo} ${config.cor} ${config.borda}`}>
                                                            {config.rotulo}
                                                        </span>
                                                        {totalDias != null && totalDias > 0 && (
                                                            <span className={estilos.emblemaQtdDias}>
                                                                <Clock size={12}/> {totalDias} Dias
                                                            </span>
                                                        )}
                                                        <div className={estilos.dataOcorrenciaTexto}>
                                                            <Calendar size={14}/>
                                                            {dataExibicao.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })}
                                                        </div>
                                                    </div>

                                                    <p className={`${estilos.textoObservacao} ${!ocorrencia.observacao ? estilos.textoSemObservacao : ''}`}>
                                                        {ocorrencia.observacao || "Sem observações."}
                                                    </p>

                                                    {ocorrencia.anexos && ocorrencia.anexos.length > 0 && (
                                                        <div className={estilos.envoltorioAnexos}>
                                                            {ocorrencia.anexos.map(anexo => (
                                                                <AnexoOcorrencia key={anexo.id} anexo={anexo} usuario={user} />
                                                            ))}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
            </div>

            {podeEditar && <JanelaModal isOpen={modalAberto} onClose={fecharFormulario} title="Nova Ocorrência">
                <form onSubmit={lidarComSalvar} className={estilos.formularioPadrao}>
                    <div className={estilos.gradeFormulario}>
                        <div className={estilos.colunaDupla}>
                            <label className={estilos.rotuloCampoForm}>Selecione o Tipo</label>
                            <div className={estilos.gradeBotoesTipo}>
                                {['ATRASO', 'FALTA', 'ATESTADO', 'ADVERTENCIA', 'SUSPENSAO', 'ELOGIO'].map(tipoItem => {
                                    const estaAtivo = dadosFormulario.motivo === tipoItem;
                                    const config = obterConfiguracaoTipo(tipoItem);
                                    return (
                                        <button
                                            key={tipoItem}
                                            type="button"
                                            onClick={() => setDadosFormulario(estadoAnterior => ({ 
                                                ...estadoAnterior, 
                                                motivo: tipoItem
                                            }))}
                                            className={`${estilos.botaoTipoPadrao} ${estaAtivo ? `${config.fundo} ${config.cor} ${config.borda} ${config.anel} ${estilos.botaoTipoAnimado}` : estilos.botaoTipoInativo}`}
                                        >
                                            {config.icone}
                                            {config.rotulo}
                                        </button>
                                    )
                                })}
                            </div>
                            <label className={estilos.rotuloCampoForm} htmlFor="motivoOcorrenciaLivre">Motivo informado ao Java</label>
                            <input
                                id="motivoOcorrenciaLivre"
                                className={estilos.entradaDias}
                                value={dadosFormulario.motivo}
                                onChange={evento => setDadosFormulario({...dadosFormulario, motivo: evento.target.value.toUpperCase()})}
                                placeholder="Ex.: LICENCA_MEDICA"
                                maxLength={80}
                                required
                            />
                        </div>

                        <div className={estilos.colunaUnicaResponsiva}>
                            <CampoEntrada 
                                label="Data do Ocorrido" 
                                type="date" 
                                value={dadosFormulario.dataOcorrencia} 
                                onChange={evento => setDadosFormulario({...dadosFormulario, dataOcorrencia: evento.target.value})} 
                                required 
                            />
                        </div>

                        <div className={estilos.colunaUnicaResponsiva}>
                            <CampoEntrada
                                label="Tipo de RO (opcional)"
                                value={dadosFormulario.tipoRo}
                                onChange={evento => setDadosFormulario({...dadosFormulario, tipoRo: evento.target.value})}
                                placeholder="Tipo do registro operacional"
                            />
                        </div>
                        <div className={estilos.colunaUnicaResponsiva}>
                            <CampoEntrada
                                label="Número SEI (opcional)"
                                value={dadosFormulario.numeroSei}
                                onChange={evento => setDadosFormulario({...dadosFormulario, numeroSei: evento.target.value})}
                                placeholder="Referência administrativa"
                            />
                        </div>

                        {['ATESTADO', 'SUSPENSAO', 'FALTA'].includes(String(dadosFormulario.motivo || '').toUpperCase()) && (
                            <div className={`${estilos.colunaUnicaResponsiva} ${estilos.animacaoSurgir}`}>
                                <label className={estilos.rotuloCampoForm}>
                                    {dadosFormulario.motivo === 'ATESTADO' ? 'Qtd Dias (Abono)' : 
                                     dadosFormulario.motivo === 'SUSPENSAO' ? 'Qtd Dias (Suspensão)' : 'Qtd Dias (Falta)'}
                                </label>
                                <input
                                    type="number"
                                    min="1"
                                    className={estilos.entradaDias}
                                    placeholder="1"
                                    value={dadosFormulario.dias}
                                    onChange={evento => setDadosFormulario({...dadosFormulario, dias: evento.target.value})}
                                />
                            </div>
                        )}

                        <div className={`${estilos.colunaDupla} ${estilos.animacaoSurgir}`}>
                            <label className={estilos.rotuloAzul}>
                                <Upload size={16}/> Anexos da ocorrência
                            </label>
                            <div className={estilos.areaUploadArquivos} onClick={() => referenciaEntradaArquivo.current?.click()}>
                                <input
                                    type="file"
                                    multiple
                                    accept="application/pdf,image/jpeg,image/png,image/webp,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    ref={referenciaEntradaArquivo}
                                    className={estilos.inputArquivoOculto}
                                    onChange={evento => setDadosFormulario({...dadosFormulario, arquivos: Array.from(evento.target.files || [])})}
                                />
                                {dadosFormulario.arquivos && dadosFormulario.arquivos.length > 0 ? (
                                    <div className={estilos.listaArquivosSelecionados}>
                                        {dadosFormulario.arquivos.map((arquivo, indice) => (
                                            <div key={`${arquivo.name}-${arquivo.size}-${arquivo.lastModified}-${indice}`} className={estilos.itemArquivoSelecionado}>
                                                <File size={14}/> {arquivo.name}
                                            </div>
                                        ))}
                                        <span className={estilos.textoAlterarArquivo}>Clique para alterar a seleção</span>
                                    </div>
                                ) : (
                                    <div className={estilos.textoPadraoUpload}>
                                        Anexe PDF, imagens, TXT, DOC ou DOCX quando necessário.
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className={estilos.colunaDupla}>
                            <label className={estilos.rotuloCampoForm}>Detalhamento</label>
                            <textarea 
                                className={estilos.entradaAreaTexto}
                                placeholder="Descreva observações..."
                                value={dadosFormulario.observacao}
                                onChange={evento => setDadosFormulario({...dadosFormulario, observacao: evento.target.value})}
                            />
                        </div>
                    </div>

                    <div className={estilos.rodapeBotoesFormulario}>
                        <Botao variant="ghost" type="button" onClick={fecharFormulario}>Cancelar</Botao>
                        <button type="submit" className={estilos.botaoSubmeterAzul}>Salvar Registro</button>
                    </div>
                </form>
            </JanelaModal>}
        </div>
    );
};
