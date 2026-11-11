# Homologação funcional do laboratório

**APROVADA no alcance funcional integrado examinado, em Chrome/Chromium desktop.** Os pareceres independentes backend26 e frontend12 não encontraram P0–P3 ou obrigação aplicável remanescente nesse recorte. A prioridade foi atender negócio, integridade, confiabilidade e permissões. Ajuste fino de latência continua adiado por decisão humana.

## Candidato e aprovação

- JAR08: `JAVA/target/sig-gtrat-backend-0.0.4-operacional-20261101.jar`, SHA-256 `f9d0a761f139094ce1db585a1cc818843482f003c247c6ba03b66ebf3af9f4c6`.
- Java24 e frontend11 liberaram os bytes produtivos; backend26 e frontend12 encerraram os predicados integrados examinados. Nenhuma alteração produtiva posterior foi necessária para as três demonstrações finais.
- Backend26: [parecer](EVIDENCIAS_VERSIONADAS/originais/review-integrated64-backend26/parecer.md), SHA-256 `938b0ef073211ebf4e88711edfbd24fe6971461adbd98ca750d57e6af21717e7`.
- Frontend12: [parecer](EVIDENCIAS_VERSIONADAS/originais/review-integrated64-frontend12/parecer.md), SHA-256 `0b45efcb4d141febab4cd2ea2d0a3324eefdaeffcee76b9f982f49c13311dda8`. Conferiu 2.730 fontes e21 entradas intactas; freeze e prova do gpt-6.1-sol/xhigh real estão no mesmo diretório.
- As revisões ocorreram em chats locais separados e visíveis no Desktop, somente leitura. Não foram substituídas por subagentes.

## Evidência decisiva

PostgreSQL18.6 e Valkey9.0.7 foram usados em laboratório nativo isolado autorizado, com destino, role limitada e ownership conferidos. Schema por Hibernate/JPA. Autenticação, cookies, CSRF e recusas foram validados no backend real. Foram demonstradas concorrência, rollback após interrupção, retomada de jobs por dois executores sem duplicação, reinício com persistência e arquivos íntegros, responsabilidade histórica, concessão de ocorrência somente para leitura, documentos/versionamento, relatórios e jornadas Chrome.

As três demonstrações finais acrescentaram 83 verificações no mesmo JAR: falha parcial real13, documentos/cargos46 e plantão histórico24. A última combina UI22–06/N30, mudança de alocação em D+1 e recusa autenticada da outra Gestão. Preparação e tentativas falhas foram preservadas e excluídas do aceite. As demais provas conservam a camada, o hash e o alcance dos artefatos originais; não se afirmam todos os testes executados no JAR08.

O usuário confirmou em outro computador físico, Chrome/Windows, login, recarga de `/historico`, acesso a `/frequencia` e saída. O acesso do próprio servidor não foi usado como comprovação externa.

Os lotes pesados foram sequenciais, com checkpoint durável antes de cada um e carga reduzida. Teto agregado15GB, proteção preventiva11GB. Sem Sonar. TypeScript strict/build e regressões pertinentes passaram; o projeto não possui script lint e nenhuma aprovação de lint foi inventada. Contagens detalhadas permanecem nos recibos, sem somar repetições como testes únicos.

## Interpretação e limites

O [ledger](EVIDENCIAS_POR_ACEITE.json) preserva 820 IDs:635 critérios,76 gates,64 ROPs,21 módulos e24 jornadas. Rastreabilidade, classes PASS, métodos candidatos e SUPPORTED não equivalem a aprovação coletiva de cada item. Os dez aceites expressamente nomeados pelos pareceres25/26 foram materializados com camada e limite; os demais estados individuais permanecem preservados.

A aprovação cobre o laboratório sintético e os predicados reconciliados. Não homologa produção, migração de acervo, escala nacional/SLA não definido, autenticidade institucional de fixtures ou PDF/UA integral. Esses fatos não foram alegados como executados. Nenhum deploy foi realizado.

O [manifesto de evidências versionadas](EVIDENCIAS_VERSIONADAS/MANIFESTO.json) diferencia cópias byte a byte de metadados seguros e resumos derivados dos recibos. Git protege código, testes, documentação e evidências selecionadas. Banco, uploads, credenciais, ambientes privados e sessões exigem backup próprio e não integram a publicação do código.
