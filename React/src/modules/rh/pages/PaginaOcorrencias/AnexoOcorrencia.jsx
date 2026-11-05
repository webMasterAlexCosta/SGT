import React, { useId, useState } from 'react';
import { Paperclip } from 'lucide-react';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.jsx';
import { eGestaoLocal, eUsuarioOperacional } from '../../../../core/security/roles.js';
import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia.js';
import estilos from './index.module.css';

export const AnexoOcorrencia = ({ anexo, usuario }) => {
    const campoFinalidadeId = useId();
    const [aberto, setAberto] = useState(false);
    const [finalidade, setFinalidade] = useState('');
    const [baixando, setBaixando] = useState(false);
    const [erro, setErro] = useState(null);
    const restrito = anexo.classificacaoAcesso === 'PESSOAL_RESTRITO';
    const bloqueado = restrito && eUsuarioOperacional(usuario);
    const exigeFinalidade = restrito && !eGestaoLocal(usuario);

    const baixar = async (evento) => {
        evento.preventDefault();
        if (baixando || bloqueado || (exigeFinalidade && !finalidade.trim())) return;
        setBaixando(true);
        setErro(null);
        try {
            const arquivo = await ServicoOcorrencia.baixarAnexo(anexo.id, finalidade);
            const url = URL.createObjectURL(arquivo.blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = arquivo.nomeArquivo || anexo.nomeOriginal;
            document.body.appendChild(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
            setAberto(false);
            setFinalidade('');
        } catch (falha) {
            setErro(falha?.message || 'Não foi possível acessar o anexo.');
        } finally {
            setBaixando(false);
        }
    };

    return <>
        <button
            type="button"
            className={estilos.linkAnexo}
            aria-label={`Acessar anexo: ${anexo.nomeOriginal}`}
            title={bloqueado ? 'O perfil Operacional não tem acesso a anexos pessoais restritos.' : undefined}
            disabled={bloqueado}
            onClick={() => { setErro(null); setAberto(true); }}
        >
            <Paperclip size={14} aria-hidden="true" /> {anexo.nomeOriginal}
        </button>
        {bloqueado && <span className={estilos.avisoAcessoAnexo}>Anexo pessoal restrito: acesso indisponível ao perfil Operacional.</span>}
        <JanelaModal isOpen={aberto} onClose={() => { if (!baixando) setAberto(false); }} title="Acessar anexo">
            <form onSubmit={baixar} className={estilos.formularioPadrao}>
                <strong>{anexo.nomeOriginal}</strong>
                {exigeFinalidade && <p>Informe a finalidade do acesso. O acesso a este documento também exige uma concessão específica vigente.</p>}
                <div>
                    <label className={estilos.rotuloCampoForm} htmlFor={campoFinalidadeId}>Finalidade do acesso{exigeFinalidade ? ' (obrigatória)' : ' (opcional)'}</label>
                    <textarea
                        id={campoFinalidadeId}
                        className={estilos.entradaAreaTexto}
                        value={finalidade}
                        onChange={(evento) => setFinalidade(evento.target.value)}
                        required={exigeFinalidade}
                        disabled={baixando}
                    />
                </div>
                {erro && <p className={estilos.erroAcessoAnexo} role="alert">{erro}</p>}
                <div className={estilos.rodapeBotoesFormulario}>
                    <Botao variant="ghost" type="button" disabled={baixando} onClick={() => setAberto(false)}>Cancelar</Botao>
                    <button type="submit" className={estilos.botaoSubmeterAzul} disabled={baixando || (exigeFinalidade && !finalidade.trim())}>{baixando ? 'Baixando...' : 'Baixar anexo'}</button>
                </div>
            </form>
        </JanelaModal>
    </>;
};
