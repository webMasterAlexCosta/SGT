import { observarFalha } from '../../../../core/http/falhaObservada';
import React, { useId, useLayoutEffect, useRef, useState } from 'react';
import { Paperclip } from 'lucide-react';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.tsx';
import { eGestaoLocal, eUsuarioOperacional } from '../../../../core/security/roles.ts';
import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia.ts';
import { useConsultaVigente } from '../../../../core/hooks/useConsultaVigente';
import { obterGeracaoSessao } from '../../../../core/security/geracaoSessao';
import estilos from './index.module.css';

export const AnexoOcorrencia = ({ anexo, usuario }: { anexo: (import("../../../../contracts/java").OcorrenciaResumoDTOAnexoResumo); usuario: (import("../../../../contracts/auth").AuthenticatedUser | null) }) => {
    const campoFinalidadeId = useId();
    const [aberto, setAberto] = useState(false);
    const [finalidade, setFinalidade] = useState('');
    const [baixando, setBaixando] = useState(false);
    const [erro, setErro] = useState<string | null>(null);
    const baixandoRef = useRef(false);
    const urls = useRef(new Map<string, ReturnType<typeof setTimeout>>());
    const contexto = JSON.stringify([usuario?.id, usuario?.perfil, anexo.id, obterGeracaoSessao()]);
    const consulta = useConsultaVigente(contexto);
    useLayoutEffect(() => {
        setAberto(false); setFinalidade(''); setBaixando(false); setErro(null); baixandoRef.current = false;
        return () => {
            consulta.invalidar();
            urls.current.forEach((timer, url) => { clearTimeout(timer); URL.revokeObjectURL(url); });
            urls.current.clear();
        };
    }, [contexto, consulta]);
    const restrito = anexo.classificacaoAcesso === 'PESSOAL_RESTRITO';
    const bloqueado = restrito && eUsuarioOperacional(usuario);
    const exigeFinalidade = restrito && !eGestaoLocal(usuario);

    const baixar = async (evento: React.FormEvent<HTMLFormElement>) => {
        evento.preventDefault();
        if (baixandoRef.current || bloqueado || (exigeFinalidade && !finalidade.trim())) return;
        const operacao = consulta.iniciar();
        baixandoRef.current = true;
        setBaixando(true);
        setErro(null);
        try {
            const arquivo = await ServicoOcorrencia.baixarAnexo(anexo.id, finalidade, { signal: operacao.signal, silentError: true });
            if (!operacao.atual()) return;
            if (!arquivo.blob) throw new Error('O servidor não enviou o anexo solicitado.');
            const url = URL.createObjectURL(arquivo.blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = arquivo.nomeArquivo || anexo.nomeOriginal || 'anexo';
            document.body.appendChild(link);
            link.click();
            link.remove();
            const timer = setTimeout(() => { URL.revokeObjectURL(url); urls.current.delete(url); }, 1000);
            urls.current.set(url, timer);
            setAberto(false);
            setFinalidade('');
        } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

            if (operacao.atual() && !falha.canceled) setErro(falha.message || 'Não foi possível acessar o anexo.');
        } finally {
            if (operacao.atual()) { setBaixando(false); baixandoRef.current = false; }
        }
    };
    const fechar = () => { consulta.invalidar(); baixandoRef.current = false; setBaixando(false); setAberto(false); };

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
        <JanelaModal isOpen={aberto} onClose={fechar} title="Acessar anexo">
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
                    <Botao variant="ghost" type="button" onClick={fechar}>{baixando ? 'Cancelar download' : 'Cancelar'}</Botao>
                    <button type="submit" className={estilos.botaoSubmeterAzul} disabled={baixando || (exigeFinalidade && !finalidade.trim())}>{baixando ? 'Baixando...' : 'Baixar anexo'}</button>
                </div>
            </form>
        </JanelaModal>
    </>;
};
