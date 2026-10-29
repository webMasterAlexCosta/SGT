import React from "react";
import { Botao, CampoEntrada, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const ModalAgencia = ({ estado, dados, bancos, setDados, aoFechar, aoSalvar }) => (
  <JanelaModal isOpen={estado.aberto} onClose={aoFechar} title={estado.item ? "Editar agência" : "Nova agência"} maxWidth="max-w-md">
    <form onSubmit={aoSalvar} className={estilos.formulario}>
      <CampoEntrada label="Número da agência" value={dados.numero} onChange={(e) => setDados((atual) => ({ ...atual, numero: e.target.value }))} required />
      <label className={estilos.campo}><span>Banco</span><select value={dados.bancoId} onChange={(e) => setDados((atual) => ({ ...atual, bancoId: e.target.value }))} required><option value="">Selecione um banco...</option>{bancos.map((banco) => <option key={banco.id} value={banco.id}>{banco.codigo} - {banco.nome}</option>)}</select></label>
      <div className={estilos.rodape}><Botao variant="secondary" type="button" onClick={aoFechar}>Cancelar</Botao><Botao type="submit">Salvar</Botao></div>
    </form>
  </JanelaModal>
);
