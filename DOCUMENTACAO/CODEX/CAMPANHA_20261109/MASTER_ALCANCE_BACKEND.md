# Gates master — obrigação atual por etapa

Os 76 IDs de `master_gates` em [MATRIZ_ACEITE.json](MATRIZ_ACEITE.json) agora possuem obrigação atual individual, linha da fonte e camadas aplicáveis. A classificação resulta de pesquisa documental integrada pelo root; não concede aceite nem substitui revisão independente. Nenhum dos 76 IDs é gate Sonar. A decisão humana de não usar Sonar nesta missão não dispensa esses predicados.

| Camada | Significado |
|---|---|
| B | Obrigação técnica backend local, exigível antes da implementação principal React. |
| F | Parte frontend, Chrome/Chromium ou integração posterior; o resultado backend não a aprova. |
| H | Condição literal histórica, com proveniência preservada e adaptação explícita ao repositório atual. |
| C_DOCUMENTAL | Autenticidade de fatos/documentos reais, apenas quando esses fatos fizerem parte da alegação. |
| C_FISICA | Virada física do dia, que precisa de prova própria para ser alegada. |
| C_COMPATIBILIDADE | Upgrade de schema/estados anteriores, fora da alegação de base nova sem migração. |
| C_NACIONAL | Capacidade nacional, que depende de corpus equivalente e não é inferida dos controles locais. |

Camadas combinadas mantêm a obrigação B. Por exemplo: testar download servidor não aprova Blob/browser, e usar fonte sintética para validar checksum não certifica documento administrativo real.

No backend permanecem exigíveis identidade/isolamento/ownership, toolchain, autenticação/CSRF/permissões, autorização temporal, unicidade/idempotência/concorrência/rollback, documentos, cache real, jobs, reinício e os controles locais disponíveis. Os hashes literais H02/H03 e MySQL do mestre identificam a campanha anterior: não autorizam reconstruir aquela infraestrutura nem promover seus resultados a PostgreSQL atual.

Bundle final, Chrome/DOM/console, intenção e resposta obsoleta entre sessões, Blob/download cliente, UI, zoom/teclado e acesso LAN por outro computador pertencem à integração final. Isso permite cumprir a sequência Java → gate backend → React → integração, sem antecipar o aceite dessas partes.

Os oito PERF compartilham o aceite do mestre, linha 518; não foram inventadas oito subdivisões. Identificar corpus, ambiente e métricas efetivamente observadas, conservar 200 registros com chunks 1/500 e relatório de 41 registros, e manter nacional NÃO EXECUTADO sem corpus equivalente. Ausência de SLA nacional fechado não dispensa os controles locais. O PDF precisa ser real e legível; alcance PDF/UA é declarado separadamente, sem certificação PDF/A inferida.

Fontes: [mestre §§8.1, 8.4–8.10](../../MATRIZES_COMPLETAS/PROMPT_MESTRE_SGT_CORRECAO_E_HOMOLOGACAO_8045_20261103.md), [plano atual](PLANO.md), [janela N/padrão 30](../../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md), [escopo PostgreSQL/base nova](../../../JAVA/docs/POSTGRESQL_ESCOPO_E_COMPATIBILIDADE.md) e [delimitação do aceite](ESCOPO_HOMOLOGACAO.md). Cada ID conserva `verification`, `review` e `acceptance` separados da classificação de alcance.
