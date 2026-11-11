# SGT — mapa da documentação

Leia primeiro o [estado atual](ESTADO_ATUAL.md). O projeto não está concluído nem homologado para produção.

| Pergunta | Fonte |
|---|---|
| O que o produto deve fazer? | [Resumo e contrato da derivada atual](01_RESUMO_REQUISITOS_E_REGRAS_DE_NEGOCIO.md), [catálogo](02_CATALOGO_DOS_198_REQUISITOS.md), [índice ROP](03_INDICE_64_ROP.md). |
| O que foi removido e o que deve permanecer? | [Mapa e suplemento PostgreSQL](MATRIZES_COMPLETAS/MAPA_REMOVIDOS_PRESERVADOS.md). |
| Como funciona a janela de frequência? | [Regra vigente: N configurável, padrão30](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md), [oráculos temporais/autorização](MATRIZES_COMPLETAS/REGRAS_TEMPORAIS_E_AUTORIZACAO.md). |
| Como preparar e validar? | [Toolchain atual](CODEX/TOOLCHAIN.md), [matriz de comandos](../.codex/validation.toml), [AGENTS](../AGENTS.md). |
| O que foi comprovado? | [Limites e pendências](ESTADO_ATUAL.md), recibos em CODEX/ e relatórios de AUDITORIA_E_PENDENCIAS/. Cada resultado vale para seus bytes. |
| Onde estão os gerados anteriores? | [Arquivo e recuperação](ARQUIVO/README.md). |

Hierarquia de leitura: decisões humanas comprovadas e escopo vigente; requisitos aplicáveis e suplemento PostgreSQL; contratos; código/testes como evidência a investigar; relatórios antigos como histórico. Não misturar namespaces de IDs nem tratar a contagem 198 como conclusão. Referências MySQL, navegador bloqueado e preparações Node antigas descrevem campanhas anteriores; o ambiente atual é definido na toolchain, sem alegar que as jornadas foram reexecutadas.

A [campanha de09/11/2026](CODEX/CAMPANHA_20261109/PLANO.md) executa Java antes de React, com Chrome/Chromium desktop, sem Sonar e teto agregado15GB. A decisão humana de frequência substitui sete dias fixos por N configurável/padrão30 nos catálogos históricos, preservados como fonte de rastreabilidade; não altera as outras permissões nem os sete dias da grade semanal.
