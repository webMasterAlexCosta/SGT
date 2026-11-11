# SGT — comece por aqui

> **Em desenvolvimento. Não concluído e não homologado para produção.** Não há autorização de implantação.

O SGT é um único projeto com backend Java/Spring em `JAVA/` e frontend React/TypeScript em `React/`. Requisitos documentados, testes aprovados em campanhas específicas e um JAR gerado não significam produto concluído.

1. [Estado atual e pendências](DOCUMENTACAO/ESTADO_ATUAL.md).
2. [Mapa da documentação](DOCUMENTACAO/README.md): negócio, contratos, técnica e evidências.
3. [Regras e requisitos](DOCUMENTACAO/01_RESUMO_REQUISITOS_E_REGRAS_DE_NEGOCIO.md), com corte histórico e suplemento da derivada atual identificados.
4. [Toolchain e comandos](DOCUMENTACAO/CODEX/TOOLCHAIN.md); [.codex/validation.toml](.codex/validation.toml) é a matriz de comandos.
5. [Instruções para o agente](AGENTS.md).

| Área | Para que serve |
|---|---|
| JAVA/src e React/src | Código atual; não foi alterado nesta organização. |
| DOCUMENTACAO | Entrada principal de requisitos, escopo, estado e documentação técnica. |
| JAVA/docs | Contratos e investigações especializados; subpastas HISTORICO_* são acervo de baselines. |
| React/documentacao | Notas específicas do frontend; preparações antigas são históricas. |
| JAVA/target | Saída gerada de Maven/testes; não integra a fonte versionada. |
| .homologacao | Acervo local privado; não publicar nem usar como configuração operacional. |

A cópia de segurança anterior à limpeza e o commit correspondente estão descritos no [índice de arquivo](DOCUMENTACAO/ARQUIVO/README.md). O histórico Git foi preservado; não houve push.
