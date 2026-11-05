import React, { memo, Suspense, lazy } from "react";
import { JanelaModal } from "../../../../../core/ui/EnvolucroDica.jsx";
import estilos from "./ModaisAgrupados.module.css";

// Importação corrigida para o mesmo nível de pasta (./) e usando as exportações corretas
const FormularioColaborador = lazy(() => import("./FormularioEmpregado/index.jsx").then((m) => ({ default: m.FormularioEmpregado })));
const ModalHistoricoAuditoria = lazy(() => import("../ui/ModalHistoricoAuditoria.jsx").then((m) => ({ default: m.ModalHistoricoAuditoria })));
const ModalHistoricoContrato = lazy(() => import("../ui/ModalHistoricoContrato.jsx").then((m) => ({ default: m.ModalHistoricoContrato })));

export const ModaisAgrupados = memo(({ modais = {}, paginaAtual, carregarColaboradores = () => {} }) => {
  const auditoria = modais?.auditoria || { aberto: false, colaborador: null };
  const historico = modais?.historico || { aberto: false, colaborador: null };
  const edicao = modais?.edicao || { aberto: false, id: null };
  const definirAuditoria = typeof modais?.definirAuditoria === "function" ? modais.definirAuditoria : () => {};
  const definirHistorico = typeof modais?.definirHistorico === "function" ? modais.definirHistorico : () => {};
  const definirEdicao = typeof modais?.definirEdicao === "function" ? modais.definirEdicao : () => {};

  return (
  <Suspense fallback={null}>
    {auditoria.aberto && (
      <ModalHistoricoAuditoria 
        isOpen={auditoria.aberto} 
        onClose={() => definirAuditoria({ aberto: false, colaborador: null })} 
        empregado={auditoria.colaborador} 
      />
    )}
    
    {historico.aberto && (
      <ModalHistoricoContrato 
        isOpen={historico.aberto} 
        onClose={() => definirHistorico({ aberto: false, colaborador: null })} 
        empregado={historico.colaborador} 
      />
    )}
    
    {edicao.aberto && (
      <JanelaModal 
        isOpen={edicao.aberto} 
        onClose={() => definirEdicao({ aberto: false, id: null })} 
        title={edicao.id ? "Editar Colaborador" : "Novo Colaborador"} 
        maxWidth="max-w-4xl"
      >
        <div className={estilos.conteudoFormulario}>
          <FormularioColaborador
            id={edicao.id}
            onSuccess={() => { 
              definirEdicao({ aberto: false, id: null }); 
              carregarColaboradores(paginaAtual); 
            }}
            onCancel={() => definirEdicao({ aberto: false, id: null })}
          />
        </div>
      </JanelaModal>
    )}
  </Suspense>
  );
});

ModaisAgrupados.displayName = "ModaisAgrupados";
