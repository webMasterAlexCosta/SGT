import React, { memo } from "react";
import { AlertTriangle, CheckCircle2, Search, Star } from "lucide-react";
import estilos from "./index.module.css";

const CARTOES = [
  {
    id: "total",
    rotulo: "Encontrados",
    valor: ({ totalGeral }) => Number(totalGeral || 0),
    Icone: Search,
    classe: estilos.cartaoPrimario,
  },
  {
    id: "ativos",
    rotulo: "Ativos na tela",
    valor: ({ estatisticas }) => Number(estatisticas?.ativos || 0),
    Icone: CheckCircle2,
    classe: estilos.cartaoSucesso,
  },
  {
    id: "sem-contrato",
    rotulo: "Sem contrato",
    valor: ({ estatisticas }) => Number(estatisticas?.semContrato || 0),
    Icone: AlertTriangle,
    classe: estilos.cartaoAlerta,
  },
  {
    id: "favoritos",
    rotulo: "Contratos salvos",
    valor: ({ quantidadeFavoritos }) => Number(quantidadeFavoritos || 0),
    Icone: Star,
    classe: estilos.cartaoFavorito,
  },
];

export const EstatisticasEmpregados = memo(
  ({ estatisticas = {}, totalGeral = 0, quantidadeFavoritos = 0 }) => {
    const totalSeguro = Number(totalGeral || 0);
    const totalEstatisticas = Number(estatisticas?.total || 0);
    if (totalEstatisticas === 0 && totalSeguro === 0) return null;

    return (
      <div className={estilos.gradeEstatisticas}>
        {CARTOES.map(({ id, rotulo, valor, Icone, classe }) => (
          <div key={id} className={`${estilos.cartaoEstatistica} ${classe}`}>
            <span className={estilos.iconeCartao} aria-hidden="true">
              <Icone size={18} />
            </span>
            <div>
              <strong className={estilos.valorCartao}>
                {valor({
                  estatisticas,
                  totalGeral: totalSeguro,
                  quantidadeFavoritos,
                }).toLocaleString("pt-BR")}
              </strong>
              <p className={estilos.rotuloCartao}>{rotulo}</p>
            </div>
          </div>
        ))}
      </div>
    );
  },
);

EstatisticasEmpregados.displayName = "EstatisticasEmpregados";
