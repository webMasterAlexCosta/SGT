import React from "react";
import { Building2, ChevronDown, Edit, MapPinned } from "lucide-react";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao";
import estilos from "./index.module.css";

const REGIOES = [
  { nome: "Norte", ufs: ["AC", "AP", "AM", "PA", "RO", "RR", "TO"] },
  { nome: "Nordeste", ufs: ["AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE"] },
  { nome: "Centro-Oeste", ufs: ["DF", "GO", "MT", "MS"] },
  { nome: "Sudeste", ufs: ["ES", "MG", "RJ", "SP"] },
  { nome: "Sul", ufs: ["PR", "RS", "SC"] },
];

const ordenarPorUf = (itens = []) => [...itens].sort((a, b) =>
  String(a?.uf || "").localeCompare(String(b?.uf || ""), "pt-BR"),
);

const ordenarLocais = (itens = []) => [...itens].sort((a, b) =>
  String(a?.nome || "").localeCompare(String(b?.nome || ""), "pt-BR", { sensitivity: "base" }),
);

export const GradeGestoresEstaduais = ({
  itens,
  gestoesLocaisPorSuperintendencia,
  presidente,
  carregando,
  aoEditar,
}) => {
  const totalEstaduaisAtivos = itens.filter((item) => item?.ativo !== false).length;
  const todosLocais = Array.from(gestoesLocaisPorSuperintendencia.values()).flat();
  const totalLocaisAtivos = todosLocais.filter((item) => item?.ativo !== false).length;

  const porRegiao = REGIOES.map((regiao) => {
    const gestores = ordenarPorUf(
      itens.filter((item) => regiao.ufs.includes(String(item?.uf || "").toUpperCase())),
    );
    const totalLocais = gestores.reduce(
      (total, item) => total + (gestoesLocaisPorSuperintendencia.get(String(item.id)) || []).length,
      0,
    );

    return { ...regiao, gestores, totalLocais };
  }).filter((regiao) => regiao.gestores.length > 0);

  return (
    <section className={estilos.blocoEstrutura} aria-label="Estrutura organizacional por região">
      <div className={estilos.resumoEstrutura}>
        <div><MapPinned size={18} /><span>Gestores estaduais</span><strong>{itens.length}</strong></div>
        <div><span className={estilos.pontoAtivo} /><span>Estaduais ativos</span><strong>{totalEstaduaisAtivos}</strong></div>
        <div><Building2 size={18} /><span>Gestões Locais</span><strong>{todosLocais.length}</strong></div>
        <div><span className={estilos.pontoAtivo} /><span>Gestões Locais ativas</span><strong>{totalLocaisAtivos}</strong></div>
      </div>

      <div className={estilos.regioes}>
        {porRegiao.map((regiao) => (
          <section key={regiao.nome} className={estilos.regiao} aria-label={`Região ${regiao.nome}`}>
            <header className={estilos.cabecalhoRegiao}>
              <div>
                <strong>{regiao.nome}</strong>
                <span>{regiao.gestores.length} UF(s)</span>
              </div>
              <span className={estilos.totalRegiao}>
                <Building2 size={15} /> {regiao.totalLocais} Gestão Local(s)
              </span>
            </header>

            <div className={estilos.grade}>
              {regiao.gestores.map((item) => {
                const locais = ordenarLocais(gestoesLocaisPorSuperintendencia.get(String(item.id)) || []);
                const quantidade = locais.length;

                return (
                  <article key={item.id} className={estilos.cartao}>
                    <header className={estilos.topoCartao}>
                      <div className={estilos.uf}>{item.uf}</div>
                      <div className={estilos.dados}>
                        <small>Gestor estadual</small>
                        <strong title={formatarGestorEstadual(item, item.uf)}>
                          {formatarGestorEstadual(item, item.uf)}
                        </strong>
                        <span className={item.ativo === false ? estilos.statusInativo : estilos.statusAtivo}>
                          {item.ativo === false ? "Inativo" : "Ativo"}
                        </span>
                      </div>
                      {aoEditar && <button type="button" onClick={() => aoEditar(item)} title="Editar gestor estadual" aria-label={`Editar ${item.uf}`}>
                        <Edit size={16} />
                      </button>}
                    </header>

                    <details className={estilos.locais}>
                      <summary>
                        <span><Building2 size={15} /> {quantidade} Gestão Local(s)</span>
                        <ChevronDown size={16} className={estilos.chevron} />
                      </summary>
                      <div className={estilos.listaLocais}>
                        {locais.map((local) => (
                          <div key={local.id} className={estilos.local}>
                            <span className={estilos.codigoLocal}>{local.codigo || "—"}</span>
                            <div>
                              <strong>{formatarGestaoLocal(local)}</strong>
                              <small className={local.ativo === false ? estilos.textoInativo : ""}>
                                {local.ativo === false ? "Inativo" : "Ativo"}
                              </small>
                            </div>
                          </div>
                        ))}
                        {!locais.length && (
                          <p className={estilos.semLocais}>
                            {presidente
                              ? "Nenhuma Gestão Local cadastrada neste gestor estadual."
                              : "Nenhuma Gestão Local vinculada."}
                          </p>
                        )}
                      </div>
                    </details>
                  </article>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {!carregando && !itens.length && <div className={estilos.vazio}>Nenhum gestor estadual cadastrado.</div>}
    </section>
  );
};
