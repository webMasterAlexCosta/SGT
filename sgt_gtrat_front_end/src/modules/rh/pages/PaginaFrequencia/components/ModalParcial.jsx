import React, { memo, useState, useEffect, useCallback } from 'react';
import { AlertCircle, Zap } from 'lucide-react';
import { JanelaModal, CampoEntrada, Botao } from '../../../../../core/ui/EnvolucroDica.jsx';
import logoCorreios from '../../../../../assets/logo-footer.png';
import estilos from '../index.module.css';

export const ModalParcial = memo(({ dadosModalParcial, aoAlterarModalParcial, aoConfirmarParcial }) => {
  const [erros, setErros] = useState({});
  const [tocados, setTouched] = useState({});

  const calcularSaidaAutomatica = useCallback((entrada, horasTrabalhadasStr) => {
    if (!entrada || !horasTrabalhadasStr) return "";
    
    const [hEntrada, mEntrada] = entrada.split(':').map(Number);
    const minutosEntrada = (hEntrada * 60) + mEntrada;
    
    let minutosTrabalhados = 0;
    const horasStr = String(horasTrabalhadasStr).trim().replace(",", ".");
    
    if (horasStr.includes(':')) {
        const [h, m] = horasStr.split(':').map(Number);
        if (!isNaN(h) && !isNaN(m)) {
            minutosTrabalhados = (h * 60) + m;
        }
    } else {
        const val = parseFloat(horasStr);
        if (!isNaN(val)) minutosTrabalhados = Math.round(val * 60);
    }
    
    if (minutosTrabalhados <= 0) return "";

    const totalMinutos = (minutosEntrada + minutosTrabalhados) % 1440; 
    
    const hSaida = Math.floor(totalMinutos / 60);
    const mSaida = totalMinutos % 60;
    
    return `${String(hSaida).padStart(2, '0')}:${String(mSaida).padStart(2, '0')}`;
  }, []);

  useEffect(() => {
    if (!dadosModalParcial.open) {
      setErros({});
      setTouched({});
      return;
    }

    const novosErros = {};
    const horasStr = String(dadosModalParcial.horas || "").trim().replace(",", ".");
    let minutosTrab = 0;
    let horasValidas = true;

    if (horasStr.includes(':')) {
        const partes = horasStr.split(':');
        if (partes.length === 2) {
            const h = Number(partes[0]);
            const m = Number(partes[1]);
            if (isNaN(h) || isNaN(m) || m >= 60 || h < 0) horasValidas = false;
            else minutosTrab = (h * 60) + m;
        } else {
            horasValidas = false;
        }
    } else {
        const val = parseFloat(horasStr);
        if (isNaN(val) || val < 0) horasValidas = false;
        else minutosTrab = Math.round(val * 60);
    }

    if (minutosTrab > 0 && horasValidas) {
        const saidaCalculada = calcularSaidaAutomatica(dadosModalParcial.entradaOriginal, horasStr);
        if (saidaCalculada && !dadosModalParcial.saidaManual && dadosModalParcial.saida !== saidaCalculada) {
             aoAlterarModalParcial(prev => ({ ...prev, saida: saidaCalculada }));
        }
    }

    if (!horasStr) {
      novosErros.horas = "Informe o total trabalhado.";
    } else if (!horasValidas || minutosTrab <= 0) {
      novosErros.horas = "Valor inválido. Use HH:MM (ex: 07:20) ou decimal.";
    }

    // RELAXAMENTO DA VALIDAÇÃO DA SAÍDA: Apenas exige a saída caso exista uma entrada original (ou seja, não é dia de folga)
    if (!dadosModalParcial.saida && dadosModalParcial.entradaOriginal) {
      novosErros.saida = "Saída obrigatória para turnos regulares.";
    }

    if (!dadosModalParcial.obs || dadosModalParcial.obs.trim().length < 5) {
      novosErros.obs = "Justificativa muito curta. min 5 caracteres.";
    }

    setErros(novosErros);
  }, [dadosModalParcial.horas, dadosModalParcial.saida, dadosModalParcial.obs, dadosModalParcial.open, dadosModalParcial.entradaOriginal, dadosModalParcial.saidaManual, calcularSaidaAutomatica, aoAlterarModalParcial]);

  const lidarComMudancaSaida = (valor) => {
    aoAlterarModalParcial(prev => ({ ...prev, saida: valor, saidaManual: true }));
  };

  const lidarComConfirmacao = () => {
    setTouched({ horas: true, saida: true, obs: true });
    if (Object.keys(erros).length === 0) {
      aoConfirmarParcial();
    }
  };

  return (
    <JanelaModal 
      isOpen={dadosModalParcial.open} 
      onClose={() => aoAlterarModalParcial({ open: false, empregadoId: null, horas: '', saida: '', entradaOriginal: '', obs: '', noturnas: 0, saidaManual: false })} 
      title="Registro de Presença Parcial" 
      maxWidth="max-w-md"
    >
      <div className={estilos.corpoModalParcial}>
          <div className={estilos.bannerCorreios}>
            <div className={estilos.logoCaixa}>
              <img src={logoCorreios} alt="Correios" className={estilos.logoBanner} />
            </div>
            <div className={estilos.textosBanner}>
              <h3>Ajuste de Jornada</h3>
              <p>Entrada do Turno: <b>{dadosModalParcial.entradaOriginal || "Sem turno (Folga)"}</b></p>
            </div>
          </div>

          <div className={estilos.gradeInputParcial}>
              <div className={estilos.envoltorioInputParcial}>
                <CampoEntrada 
                  label="Horas Trabalhadas *" 
                  type="text" 
                  placeholder="Ex: 07:20 ou 7.5" 
                  value={dadosModalParcial.horas} 
                  onChange={e => aoAlterarModalParcial(prev => ({...prev, horas: e.target.value, saidaManual: false}))} 
                  onBlur={() => setTouched(p => ({...p, horas: true}))}
                  tooltip="Digite em formato de relógio HH:MM (ex: 07:20) ou em decimal (ex: 7.5)." 
                />
                {(tocados.horas || erros.horas) && erros.horas && (
                  <span className={estilos.mensagemErroParcial}><AlertCircle size={10}/> {erros.horas}</span>
                )}
              </div>

              <div className={estilos.envoltorioInputParcial}>
                <CampoEntrada 
                  label={dadosModalParcial.entradaOriginal ? "Horário de Saída *" : "Horário de Saída (Opcional)"} 
                  type="time" 
                  value={dadosModalParcial.saida} 
                  onChange={e => lidarComMudancaSaida(e.target.value)} 
                  onBlur={() => setTouched(p => ({...p, saida: true}))}
                  tooltip="Calculado automaticamente baseando-se na entrada original. Edite se o colaborador entrou com atraso." 
                />
                {(tocados.saida || erros.saida) && erros.saida && (
                  <span className={estilos.mensagemErroParcial}><AlertCircle size={10}/> {erros.saida}</span>
                )}
                {dadosModalParcial.horas && !erros.horas && !dadosModalParcial.saidaManual && dadosModalParcial.entradaOriginal && (
                    <span className={estilos.indicadorSugerido}>
                        <Zap size={10}/> Sugerido
                    </span>
                )}
              </div>
          </div>
          
          <div className={estilos.envoltorioInputParcial}>
            <CampoEntrada 
              label="Justificativa / Observação *" 
              value={dadosModalParcial.obs} 
              onChange={e => aoAlterarModalParcial(prev => ({...prev, obs: e.target.value}))} 
              onBlur={() => setTouched(p => ({...p, obs: true}))}
              placeholder="Ex: Saída antecipada para curso..." 
            />
            {(tocados.obs || erros.obs) && erros.obs && (
              <span className={estilos.mensagemErroParcial}><AlertCircle size={10}/> {erros.obs}</span>
            )}
          </div>
          
          <div className={estilos.rodapeModal}>
              <Botao variant="secondary" onClick={() => aoAlterarModalParcial({ open: false, saidaManual: false })}>Cancelar</Botao>
              <Botao onClick={lidarComConfirmacao} className={estilos.botaoConfirmarParcial}>Gravar Registro</Botao>
          </div>
      </div>
    </JanelaModal>
  );
});

ModalParcial.displayName = "ModalParcial";