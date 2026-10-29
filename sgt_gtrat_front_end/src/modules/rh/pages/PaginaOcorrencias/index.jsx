import React, { useState, useEffect, useMemo, useRef } from 'react';
import { 
    Search, Plus, Trash2, User, 
    Clock, AlertTriangle, FileText, XCircle, Star, Ban,
    Calendar, X, Upload, Paperclip, Briefcase, File
} from 'lucide-react';

import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia.js';
import { Botao, JanelaModal, CampoEntrada } from '../../../../core/ui/EnvolucroDica.jsx';
import { notificar } from '../../../../core/events.js';
import { useConfirmacaoSenha } from '../../../../core/security/useConfirmacaoSenha.jsx';
import { CONFIG } from '../../../../core/config.js'; 
import estilos from './index.module.css';
import { useAutenticacao } from '../../../auth/context.jsx';
import { podeGerenciarOcorrencias } from '../../../../core/security/roles.js';

export const PaginaOcorrencias = () => {
    const { user } = useAutenticacao();
    const podeEditar = podeGerenciarOcorrencias(user);
    const [termoBusca, setTermoBusca] = useState('');
    const [empregados, setEmpregados] = useState([]);
    const [empregadoSelecionado, setEmpregadoSelecionado] = useState(null);
    const [carregandoEmpregados, setCarregandoEmpregados] = useState(false);
    const [ocorrencias, setOcorrencias] = useState([]);
    const [modalAberto, setModalAberto] = useState(false);
    
    const referenciaEntradaArquivo = useRef(null);
    
    const obterDataHojeLocal = () => {
        const data = new Date();
        const ano = data.getFullYear();
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${ano}-${mes}-${dia}`;
    };

    const formularioInicial = { 
        motivo: 'ATRASO', 
        observacao: '', 
        dataOcorrencia: obterDataHojeLocal(), 
        dias: '',
        tipoRo: '',
        numeroSei: '',
        arquivos: [] 
    };
    const [dadosFormulario, setDadosFormulario] = useState(formularioInicial);

    const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

    const obterConfiguracaoTipo = (tipo) => {
        const chave = (tipo ?? '').toString().trim().toUpperCase();
        const configuracoes = {
            'ATRASO': { cor: estilos.tipoAmbarTexto, fundo: estilos.tipoAmbarFundo, borda: estilos.tipoAmbarBorda, anel: estilos.tipoAmbarAnel, icone: <Clock size={18}/>, rotulo: 'Atraso' },
            'FALTA': { cor: estilos.tipoVermelhoTexto, fundo: estilos.tipoVermelhoFundo, borda: estilos.tipoVermelhoBorda, anel: estilos.tipoVermelhoAnel, icone: <XCircle size={18}/>, rotulo: 'Falta' },
            'ADVERTENCIA': { cor: estilos.tipoLaranjaTexto, fundo: estilos.tipoLaranjaFundo, borda: estilos.tipoLaranjaBorda, anel: estilos.tipoLaranjaAnel, icone: <AlertTriangle size={18}/>, rotulo: 'Advertência' },
            'SUSPENSAO': { cor: estilos.tipoRosaTexto, fundo: estilos.tipoRosaFundo, borda: estilos.tipoRosaBorda, anel: estilos.tipoRosaAnel, icone: <Ban size={18}/>, rotulo: 'Suspensão' },
            'ELOGIO': { cor: estilos.tipoEsmeraldaTexto, fundo: estilos.tipoEsmeraldaFundo, borda: estilos.tipoEsmeraldaBorda, anel: estilos.tipoEsmeraldaAnel, icone: <Star size={18}/>, rotulo: 'Elogio' },
            'ATESTADO': { cor: estilos.tipoAzulTexto, fundo: estilos.tipoAzulFundo, borda: estilos.tipoAzulBorda, anel: estilos.tipoAzulAnel, icone: <FileText size={18}/>, rotulo: 'Atestado' },
        };
        return configuracoes[chave] || { cor: estilos.tipoCinzaTexto, fundo: estilos.tipoCinzaFundo, borda: estilos.tipoCinzaBorda, anel: estilos.tipoCinzaAnel, icone: <FileText size={18}/> , rotulo: chave || 'Evento' };
    };

    const obterIniciais = (nome) => nome ? nome.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() : '??';

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
        if (!termoBusca || termoBusca.length < 2) {
            setEmpregados([]); return;
        }
        
        // CORREÇÃO: Previne o re-fetch desnecessário quando o utilizador clica no nome da lista
        if (empregadoSelecionado && termoBusca === empregadoSelecionado.nome) {
            return;
        }

        const controlador = new AbortController();
        const temporizador = setTimeout(() => {
            setCarregandoEmpregados(true);
            ServicoOcorrencia.buscarEmpregados(termoBusca, { signal: controlador.signal, skipLoading: true })
               .then(res => setEmpregados(res))
               .catch(err => { if (!err?.canceled) console.error(err); })
               .finally(() => {
                   if (!controlador.signal.aborted) {
                       setCarregandoEmpregados(false);
                   }
               });
        }, CONFIG.SEARCH_DEBOUNCE_MS); 
        return () => {
            controlador.abort();
            clearTimeout(temporizador);
        };
    }, [termoBusca, empregadoSelecionado]);

    useEffect(() => {
        const controlador = new AbortController();
        if (empregadoSelecionado) {
            ServicoOcorrencia.listarPorEmpregado(empregadoSelecionado.id, { signal: controlador.signal, skipLoading: true })
               .then(res => setOcorrencias(res))
               .catch(err => { if (!err?.canceled) console.error(err); });
        } else {
            setOcorrencias([]);
        }
        return () => controlador.abort();
    }, [empregadoSelecionado]);

    const lidarComSalvar = async (evento) => {
        evento.preventDefault();
        if (!empregadoSelecionado) return;

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

        await confirmarComSenha({
            titulo: 'Confirmar ocorrência',
            mensagem: msgConfirmacao,
            acao: async (senhaConfirmacao) => {
                try {
                    await ServicoOcorrencia.criar(dadosEnvio, { senhaConfirmacao });
                    notificar('success', 'Ocorrência registrada. Os efeitos de frequência foram processados pelo Java.');
                    
                    setModalAberto(false);
                    setDadosFormulario({ ...formularioInicial, dataOcorrencia: obterDataHojeLocal() });
                    if (referenciaEntradaArquivo.current) referenciaEntradaArquivo.current.value = "";
                    
                    const res = await ServicoOcorrencia.listarPorEmpregado(empregadoSelecionado.id);
                    setOcorrencias(res);
                } catch (erro) {
                    console.error("Erro ao salvar:", erro);
                    notificar('error', erro?.message || 'Não foi possível registrar a ocorrência.');
                    throw erro; 
                }
            }
        });
    };

    const lidarComExcluir = async (id, motivo) => {
        const mensagemExclusao = motivo === 'FALTA' || motivo === 'ATESTADO' || motivo === 'SUSPENSAO' 
            ? 'A ocorrência será excluída e o Java reconciliará novamente a frequência afetada.'
            : 'A ocorrência será excluída. O Java decidirá os efeitos associados ao registro.';

        await confirmarComSenha({
            titulo: 'Excluir Ocorrência',
            mensagem: mensagemExclusao,
            acao: async (senhaConfirmacao) => {
                try {
                    await ServicoOcorrencia.excluir(id, { senhaConfirmacao });
                    setOcorrencias(estadoAnterior => estadoAnterior.filter(o => o.id !== id));
                    notificar('success', 'Ocorrência removida e frequência reconciliada pelo Java.');
                } catch (erro) {
                    notificar('error', erro?.message || 'Não foi possível remover o registro.');
                    throw erro; 
                }
            }
        });
    };

    const limparSelecao = () => {
        setEmpregadoSelecionado(null);
        setTermoBusca('');
        setEmpregados([]);
    };

    const obterUrlDownload = (anexoId) => {
        return ServicoOcorrencia.obterUrlAnexo(anexoId);
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

            <div className={estilos.gradePrincipal}>
            <div className={estilos.colunaLateral}>
                <div className={estilos.cartaoBusca}>
                    <h2 className={estilos.tituloBusca}>
                        <Search size={20} className={estilos.iconeAzul}/> Localizar
                    </h2>
                    <div className={estilos.envoltorioRelativo}>
                        <div className={estilos.envoltorioEntradaBusca}>
                            <input 
                                type="text" 
                                placeholder="Nome ou CPF..." 
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
                                            setTermoBusca(emp.nome);
                                            setEmpregados([]);
                                        }}
                                    >
                                        <div className={estilos.avatarIniciais}>
                                            {obterIniciais(emp.nome)}
                                        </div>
                                        <div>
                                            <div className={estilos.nomeSugestao}>{emp.nome}</div>
                                            <div className={estilos.detalhesSugestao}>
                                                <span className={estilos.cargoSugestao}>{emp.cargo?.nome || 'Sem Cargo'}</span>
                                                {emp.cpf && <span className={estilos.cpfSugestao}>{emp.cpf}</span>}
                                            </div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}
                        {carregandoEmpregados && <div className={estilos.indicadorCarregando}></div>}
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
                                <span>{empregadoSelecionado.cargo?.nome || 'Cargo não definido'}</span>
                            </div>
                            {podeEditar && <button 
                                onClick={() => { 
                                    setDadosFormulario({ ...formularioInicial, dataOcorrencia: obterDataHojeLocal() }); 
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
                                <span className={estilos.contadorHistorico}>{ocorrencias.length} registro(s)</span>
                            </div>

                            {ocorrencias.length === 0 ? (
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
                                                        {totalDias > 0 && (
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
                                                                <a 
                                                                    key={anexo.id}
                                                                    href={obterUrlDownload(anexo.id)} 
                                                                    target="_blank" 
                                                                    rel="noopener noreferrer" 
                                                                    className={estilos.linkAnexo}
                                                                >
                                                                    <Paperclip size={14}/> {anexo.nomeOriginal}
                                                                </a>
                                                            ))}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
            </div>

            {podeEditar && <JanelaModal isOpen={modalAberto} onClose={() => setModalAberto(false)} title="Nova Ocorrência">
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
                        <Botao variant="ghost" type="button" onClick={() => setModalAberto(false)}>Cancelar</Botao>
                        <button type="submit" className={estilos.botaoSubmeterAzul}>Salvar Registro</button>
                    </div>
                </form>
            </JanelaModal>}
        </div>
    );
};
