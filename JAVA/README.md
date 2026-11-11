# SGT/GTRAT Java — correção C25-H04 → PostgreSQL18.6

> **Entrada de módulo. Projeto em desenvolvimento, sem homologação integral de produção.** Consulte o [estado atual](../DOCUMENTACAO/ESTADO_ATUAL.md) e o README da raiz. Trechos de preparação/entrega abaixo identificam suas campanhas e não concedem conclusão ou produção.

> Toolchain atualizada por pedido humano para Node26.13.0/npm12.2.2, Java/release25 e Boot4.1.2. Consulte `../DOCUMENTACAO/CODEX/TOOLCHAIN.md` e o recibo de migração correspondente. Os números, hashes e aprovações de campanhas anteriores abaixo permanecem históricos e não aprovam estes novos bytes.

Esta cópia deriva da baseline **C25-H04**, sob ordem humana de 2026-11-06, com **PostgreSQL Server exatamente18.6**. O identificador Maven recebido continua `0.0.4-operacional-20261101`; a identidade da derivada é dada pelos hashes de fontes, JAR e assets em `SELO_FINAL_BYTES.json`, acompanhado da entrega. Os recibos e a matriz de aceite da campanha identificam o alcance efetivamente homologado e os bloqueios restantes. Build ou teste unitário isolado não representa homologação integral.

As correções Java desta derivada abrangem a versão canônica retornada pelas mutações documentais e a seleção de empregados do fechamento, que passa a interromper a avaliação da página quando o bloco está preenchido, conservando os filtros temporais e a verificação de todos os vínculos da mesma Pessoa. A guarda obrigatória `VerificadorAmbientePostgres186` verifica a DataSource real antes da inicialização do schema. O React integrado possui correções de sessão entre abas e intenção/prévia de prorrogação, descritas nos dossiês da entrega.

**Proveniência:** os relatos da distribuição de origem preservados abaixo, inclusive suas contagens de testes e pendências, são históricos. Eles não aprovam os bytes C25-H04 → PostgreSQL18.6 nem substituem a repetição F12 da campanha atual.

Derivação dos bytes Java 0.0.3-operacional-20261101/React 1.4.3-operacional.20261101 da entrega “Remover Governança Financeira”. Par atual: Java 0.0.4-operacional-20261101 e React 1.4.4-operacional.20261101.

## Alterações e preservação

Foram retirados processos exclusivos de onboarding/offboarding, risco manual completo, monitoramento contínuo, jobs, resumos, DTOs, entidades/repositórios e APIs exclusivos da Governança. As funções financeiras previamente retiradas continuam ausentes.

Fornecedor e seu repositório permanecem operacionais, com identidade, CNPJ, IDs, relacionamentos e versionamento. Novos contratos:
- GET/POST /api/fornecedores
- PUT /api/fornecedores/{id}
- PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}

Mantêm Gestão Local, senha nas mutações, CSRF, escopo, locks e revalidação. Vínculo permanece limitado a rascunho. Não há alias da API antiga.
No React, Empresas contratadas fica em Cadastros e configurações, rota /empresas-contratadas.

Contratos, aditamentos, pessoas, vínculos, cargos, composição, alocações, frequência, ocorrências, atestados, documentos, relatórios, segurança e auditoria permanecem. GOV-003 protege fatos publicados; não foi retirado. Os 198 requisitos originais não descrevem risco/processos desse módulo adicional. As classificações192 A + 6 E históricas não certificam esta candidata.

A frequência mantém D como dia 1, fechamento D+7 às 00:00 em America/Sao_Paulo, retificação gerencial com justificativa/auditoria, vínculo efetivo na data e rejeição de presença futura.

## Schema e dados

Apenas Hibernate/JPA administra tabelas: ddl-auto=update e spring.sql.init.mode=never. Sem DDL manual de aplicação, schema.sql, Flyway, Liquibase ou purga.

O aceite atual refere-se à baseline PostgreSQL nova. A migração de acervos existentes e a compatibilidade com estados anteriores não estão comprovadas; os dados antigos permanecem preservados. Consulte [o escopo e os limites de compatibilidade](docs/POSTGRESQL_ESCOPO_E_COMPATIBILIDADE.md).

Na decisão posterior desta sessão, a fase 42 retirou do modelo atual de `Fornecedor` o campo privado `estadoMonitoramentoLegado`, que mapeava `monitoramento_status`, e métodos obsoletos de `EmpregadoRepositorio`. A compatibilidade com o `NOT NULL` antigo permanece documentada como contexto histórico em `docs/ADAPTACAO_MYSQL_8_0_46.md`; ela não é requisito do schema novo. Foram usadas bases PostgreSQL 18.6 novas, com schema criado exclusivamente por Hibernate/JPA. As bases anteriores e suas evidências foram preservadas; a retirada do mapeamento não executa purga nem autoriza apagar coluna ou dado legado.

### Suplemento da fase 42 — 06/11/2026

A validação deste recorte está nos recibos externos da campanha, em `42_REMOCAO_CODIGO_OBSOLETO`:

- `FORCED_BUILD_VALIDATION.json`: recompilação forçada de todas as fontes main/test; 6/6 verificações de arquitetura, sem falhas, erros, skips ou drift registrado. O campo obsoleto e os métodos removidos não aparecem nas classes verificadas.
- `FRESH_JPA_TEST_SUMMARY.json`: 21/21 integrações selecionadas em cinco classes de Fornecedor e Frequência, sem falhas, erros, skips ou drift registrado.
- `FRESH_JPA_SCHEMA_METADATA.json`, de 06/11/2026 às 10:52:58 UTC: PostgreSQL 18.6; ITv2 (`sgt_lab_it_v2_xwn5s07h`) com 72 tabelas públicas criadas por Hibernate/JPA e sem `fornecedor.monitoramento_status`. Nesse recibo, appv2 (`sgt_lab_app_v2_xwn5s07h`) ainda tinha zero tabelas e não havia sido iniciada. A consulta de metadados não executou DDL de domínio.

Esses resultados pertencem aos bytes e às bases identificados nos recibos. Não demonstram boot da appv2, upgrade de acervo legado, homologação global, Chrome ou um novo JAR selado. O prompt humano de 03/11 e os recibos/matrizes históricos permanecem preservados; seus resultados não foram convertidos em aceite atual.

**Revisão focal dos códigos e recibos:** `42_REMOCAO_CODIGO_OBSOLETO/PARECER_REVISOR.md` aceita o recorte da fase 42, sem achados acionáveis. O parecer preserva os limites: baseline PostgreSQL/JPA nova; compatibilidade com schema antigo, regressão completa atual, Chrome posterior à fase 39 e JAR final não homologados. A fase 43 e esta proposta documental não integram aquele recorte de revisão.

## Execução e recursos

Java/release25, Maven 3.9.16, Spring Boot 4.1.2, pgJDBC42.7.14 e servidor PostgreSQL EXATAMENTE18.6 (runtimeJDK25.0.4.1, bytecode release25). Node26.13.0/npm12.2.2; versões de dependências preservadas. Redis real nos cenários que o utilizam.
Uma suíte pesada por vez: Maven 512 MiB; fork 768 MiB/metaspace 256 MiB; Node 768 MiB; um worker; pool de testes8 conexões.

Para execução local, copie .env.example para .env e configure credenciais autorizadas. Não distribua .env nem execute-o como shell. Testes unitários de configuração leem o exemplo público.

```bash
mvn -B -ntp -T1 test
mvn -B -ntp -T1 clean package
```

Integração exige catálogos e conta especificamente autorizados, INTEGRATION_POSTGRES_URL/USER/PASSWORD e Redis configurados fora do código:
```bash
mvn -B -ntp -T1 -Pintegration verify
```
Não contornar a autorização por container ou pela conta root. package verde não comprova verify com integração.
Os dois suportes Java exigem configuração nativa explícita, recusam URL/usuário/senha ausentes ou vazios, contas administrativas e credenciais embutidas na URL antes de abrir conexão. `integration.postgres.url/user/password` têm precedência sobre o ambiente; vazio é erro. `INTEGRATION_POSTGRES_MODE=testcontainers` é recusado: o laboratório deve ser provisionado externamente, com role sem superuser/CREATEDB/CREATEROLE/replication/BYPASSRLS e sem portas públicas. Não há fallback.

O perfil `prod` usa `ddl-auto=validate`, `generate-ddl=false` e `sql.init.mode=never`, sem mutação de schema; Hibernate validate não detecta todo drift. Dev/homologação seguem `update`. Não executar scripts históricos MySQL desta pasta como provas PostgreSQL. Colação textual/acento/case e os casos complexos de locks estão discriminados no relatório da etapa; não receberam aceite implícito.

Regressão focal sem banco: `bash mvnw -Dtest=ConfiguracaoPostgresIntegracaoTest,GuardaPostgres186Test,PostgresPersistenceContractTest test`. Integrações devem ser executadas em lotes sequenciais de5ou10 classes, com `test-compile failsafe:integration-test failsafe:verify -Pintegration -Dit.test=<classes>`. Não passar `-DskipTests`, pois também desativa Failsafe.

JAR: target/sig-gtrat-backend-0.0.4-operacional-20261101.jar. Seus recursos estáticos devem coincidir com o dist do React do mesmo manifesto.

## Rastreabilidade e pendências

docs/LEIA_PRIMEIRO_CANDIDATA.md e as matrizes herdadas identificam a distribuição de origem. docs/HISTORICO_BASELINE_0_0_3 conserva os documentos anteriormente substituídos. Os relatórios, recibos, matrizes e checkpoints externos da entrega C25-H04 → PostgreSQL18.6 identificam os comandos e resultados efetivos desta correção, sem reutilizar resultados históricos como aceite atual.

Registro histórico recebido sobre o409: logs comprovavam IND-08 negativo antes do CRUD nacional, com reprodução causal ainda pendente naquele corte. A classificação atual deve ser consultada nas novas matrizes da campanha; esse registro não constitui um resultado de execução C25-H04 → PostgreSQL18.6. As validações históricas permanecem exigidas.


## Verificação histórica da distribuição de origem

Java: **500/500** unitários e `clean package` com saída 0. React: **469/469**, preparação separada **1/1** e build de produção com saída 0. Os **98 arquivos** do React estão incorporados no JAR e conferidos por SHA-256. As integrações, `-Pintegration verify`, o diagnóstico causal do 409, o navegador e o reinício continuam pendentes; consultar os relatórios da candidata.
