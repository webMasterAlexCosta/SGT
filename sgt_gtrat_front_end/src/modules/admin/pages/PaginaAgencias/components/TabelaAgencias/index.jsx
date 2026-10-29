import React from "react";
import { Edit, Trash2 } from "lucide-react";
import { Cartao, Tabela } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const TabelaAgencias = ({ agencias, obterNomeBanco, aoEditar, aoExcluir, podeEscrever = false }) => (
  <Cartao>
    {!agencias.length ? <div className={estilos.vazio}>Nenhuma agência encontrada.</div> : (
      <Tabela headers={["Número", "Banco", ...(podeEscrever ? ["Ações"] : [])]}>
        {agencias.map((agencia) => (
          <tr key={agencia.id} className={estilos.linha}>
            <td>{agencia.numero}</td>
            <td className={estilos.banco}>{obterNomeBanco(agencia.bancoId)}</td>
            {podeEscrever && <td><div className={estilos.acoes}><button type="button" onClick={() => aoEditar(agencia)} className={estilos.editar} title="Editar"><Edit size={16} /></button><button type="button" onClick={() => aoExcluir(agencia.id)} className={estilos.excluir} title="Excluir"><Trash2 size={16} /></button></div></td>}
          </tr>
        ))}
      </Tabela>
    )}
  </Cartao>
);
