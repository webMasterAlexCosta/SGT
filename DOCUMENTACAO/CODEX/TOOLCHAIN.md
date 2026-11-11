# SGT — toolchain e comandos atuais

Escolha humana de 06/11/2026: Node **26.13.0**, npm **12.2.2**, JDK/bytecode **25** e Spring Boot **4.1.2**. `.codex/validation.toml` na raiz do projeto é a matriz de comandos das skills pessoais; não é uma configuração nativa adicional do Codex.

## Desenvolvimento e versões

- Use os runtimes escolhidos somente neste projeto; `.node-version`, `.nvmrc`, `packageManager`, `engines` e `.java-version` registram a escolha sem impor um gerenciador ou trocar instalações globais.
- Confirme `node --version`, `npm --version`, `java -version`, `javac -version` e `bash ./mvnw --version` no módulo Java. O wrapper permanece Maven3.9.16. Java25 é runtime e release de compilação; sem preview.
- Frontend: npm ci --ignore-scripts --no-audit --no-fund. Preserve package-lock v3; `.npmrc` fixa lockfile-version=3 e engine-strict=true. React/Vite/Router/TypeScript e dependências fora do escopo permanecem nas versões dos manifests/lockfiles. Não executar npm update/audit fix ou migrar Router por inferência.
- HMR Vite e API Spring real separados; preserve os proxies e processos existentes. Build/typecheck/testes são checkpoints, não passos obrigatórios por edição.
- O controle de filas e orçamento segue AGENTS e a matriz; não executar suítes pesadas paralelas. A inexistência de um comando não é aprovação desse check.

## Preservação e integração

PostgreSQL18.6 e os recursos privados existentes permanecem intactos. Integração exige base nova/isolada, parâmetros explícitos, role e ownership conferidos; Redis real isolado nos cenários que já o exigem. Schema é exclusivo de Hibernate/JPA conforme AGENTS, sem DDL manual/importação de acervo. `.env` não é distribuído e VITE_* nunca recebe segredo.

Os scripts/README de campanhas antigas e seus números/hashes permanecem históricos. Considere os recibos atuais para declarar compilação, testes, integração, navegador ou aceite. Fonte/assets/JAR devem permanecer com os mesmos bytes entre build e checks. O build não autoriza produção, deploy ou migração de dados.

## Fontes primárias

- https://nodejs.org/en/blog/release/v26.13.0
- https://github.com/npm/cli/blob/v12.2.2/package.json
- https://github.com/spring-projects/spring-boot/blob/v4.1.2/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/system-requirements.adoc
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html

## Particularidades SGT

Migração Boot3.5.16→4.1.2/release17→25. O BOM passa a Framework7.0.10, Security7.1.2, Hibernate7.4.13.Final e Tomcat11.0.27/Servlet6.1. São mudanças transitivas necessárias ao parent escolhido, preservando regras/locks/transações/contratos.

Jackson2.21.7 permanece para JSON/JJWT/cache via spring-boot-jackson2 e spring.jackson2.*, com conversores HTTP preferindo jackson2; Jackson3.1.7 continua gerenciado pelo BOM. Spring Retry2.0.13 preserva a versão do BOM anterior com pin explícito. Testcontainers1.21.4 e demais pins compatíveis permanecem; isso não autoriza executá-lo automaticamente.

React19.2.5/Vite8.2.4/Router7.18.3/TypeScript5.9.3 permanecem. npm run test:preparacao e os testes Vitest são gates separados. Não existe script lint nesta baseline. npm run sync:java:check confere sem copiar; sync:java copia e exige conferência/checkpoint do destino. O package Java inclui testes unitários e não comprova os ITs.

Vitest4.1.12 recebe --no-experimental-webstorage em test.execArgv, nos workers. O runner não herda esse flag automaticamente do processo principal. Isso permite que jsdom forneça localStorage/sessionStorage como na baseline anterior, preservando aplicação, fixtures e todas as asserções. Referência: https://github.com/vitest-dev/vitest/blob/v4.1.12/docs/config/execargv.md

A integração Redis real identificou NoClassDefFoundError para io.netty.channel.MultiThreadIoEventLoopGroup com o pin legado Netty4.1.137.Final. Foi removido somente esse override incompatível: Netty4.2.19.Final e Lettuce7.5.2.RELEASE vêm do BOM4.1.2. Isso é uma exigência de compatibilidade da migração; as demais versões externas compatíveis continuam preservadas.

A fixture de CacheRotasRetiradasRedisHttpIT usa putIfAbsent para garantir publicação imediata antes do HTTP, conforme o contrato Cache7.0.10. O put simples pode ser diferido; a instrumentação observou essa janela sem transação ativa. Todas as assertivas de bytes,404,401,403 e preservação da entrada continuam iguais; nenhum código de cache/negócio da aplicação foi alterado. Referência: https://docs.spring.io/spring-framework/docs/7.0.10/javadoc-api/org/springframework/cache/Cache.html

Recibo da validação desta migração: [MIGRACAO_VERIFICADA.json](MIGRACAO_VERIFICADA.json). Os resultados e hashes vinculam somente os bytes descritos; não representam aceite de produção.

## Revisão final da configuração

Os três rótulos Java/compilação/Boot dos créditos ativos e o teste existente foram alinhados à toolchain. Novo JAR/asset e smoke estão no recibo complementar; o recibo inicial identifica os bytes anteriores. Consulte [REVISAO_FINAL_VERIFICADA.json](REVISAO_FINAL_VERIFICADA.json).


## Contratos técnicos preservados na organização de 07/11/2026

JDK/release 25, Boot 4.1.2, Node 26.13.0/npm 12.2.2 exatos; React 19.2.5/Vite 8.2.4/Router 7.18.3/TypeScript 5.9.3, lockfile v3. PostgreSQL Server 18.6 real/pgJDBC 42.7.14; Redis real isolado nos cenários que o utilizam.

Esses valores registram o estado dos manifests/lockfiles/BOM e das decisões já documentadas. Esta organização não migra dependências nem altera código ou configurações da aplicação. Instruções operacionais, isolamento, schema, recursos e limites de execução estão no AGENTS da raiz e na matriz de comandos. Evidência de compilação não equivale a homologação funcional ou de produção.

Instalação em React/: npm ci --ignore-scripts --no-audit --no-fund; desenvolvimento por npm run dev com proxy existente para a API real. O BOM controla Spring/ORM/Servlet e os pins externos compatíveis permanecem; não migrar contratos para Jackson3 por inferência.
