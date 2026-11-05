# SGT/GTRAT Java — correção C25-H04-8045

Esta cópia deriva da baseline **C25-H02** para a campanha de 2026-11-03, com **MySQL Server exatamente 8.0.45**. O identificador Maven recebido continua `0.0.4-operacional-20261101`; a identidade da derivada é dada pelos hashes de fontes, JAR e assets em `SELO_FINAL_BYTES.json`, acompanhado da entrega. Os recibos e a matriz de aceite da campanha identificam o alcance efetivamente homologado e os bloqueios restantes. Build ou teste unitário isolado não representa homologação integral.

As correções Java desta derivada abrangem a versão canônica retornada pelas mutações documentais e a seleção de empregados do fechamento, que passa a interromper a avaliação da página quando o bloco está preenchido, conservando os filtros temporais e a verificação de todos os vínculos da mesma Pessoa. A guarda opcional `--app.homologacao.mysql8045=true` verifica a DataSource real antes da inicialização do schema. O React integrado possui correções de sessão entre abas e intenção/prévia de prorrogação, descritas nos dossiês da entrega.

**Proveniência:** os relatos da distribuição de origem preservados abaixo, inclusive suas contagens de testes e pendências, são históricos. Eles não aprovam os bytes C25-H04-8045 nem substituem a repetição F12 da campanha atual.

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

A retirada do mapeamento não executa purga de tabelas de processos, risco ou financeiro; a preservação física no upgrade ainda precisa ser conferida. Há uma compatibilidade estrutural documentada em Fornecedor: monitoramento_status era NOT NULL sem default SQL. Um campo privado sem acessores e updatable=false preenche o mesmo literal legado no INSERT, sem cálculo, job, consulta API ou atualização de risco. A prova material baseline→candidata aguarda o teste de upgrade.

## Execução e recursos

Java 17, Maven 3.9.16, Spring Boot 3.5.16, Connector/J 9.7.0 e servidor MySQL EXATAMENTE 8.0.45. Node 22.16.0/npm10.9.2; versões de dependências preservadas. Redis real nos cenários que o utilizam.
Uma suíte pesada por vez: Maven 512 MiB; fork 768 MiB/metaspace 256 MiB; Node 768 MiB; um worker; pool de testes8 conexões.

Para execução local, copie .env.example para .env e configure credenciais autorizadas. Não distribua .env nem execute-o como shell. Testes unitários de configuração leem o exemplo público.

```bash
mvn -B -ntp -T1 test
mvn -B -ntp -T1 clean package
```

Integração exige catálogos e conta especificamente autorizados, INTEGRATION_MYSQL_URL/USER/PASSWORD e Redis configurados fora do código:
```bash
mvn -B -ntp -T1 -Pintegration verify
```
Não contornar a autorização por container ou pela conta root. package verde não comprova verify com integração.
Os dois suportes Java recusam URL/usuário/senha ausentes ou vazios, contas internas/`root` e credenciais embutidas na URL antes de abrir uma conexão. Propriedades `integration.mysql.url/user/password` têm precedência explícita sobre o ambiente; um valor vazio não seleciona outra identidade. `INTEGRATION_MYSQL_MODE=testcontainers` é uma alternativa opt-in, sem configuração nativa parcial, e usa conta de schema; nenhuma falta de configuração inicia Docker automaticamente.

Os scripts de validação exigem `MYSQL_USER`/`MYSQL_PWD` explícitos e não recorrem a conta administrativa. O cliente 8.0 usa `--no-defaults` e um login-path vazio no ambiente do subprocesso. As configurações locais e senhas sintéticas ficam fora dos relatórios/pacotes.

Regressão da resolução de configuração, sem conexão externa:

```bash
mvn -B -ntp -T1 -Dtest=ConfiguracaoMysqlIntegracaoTest test
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s scripts/validacao -p test_configuracao_mysql.py -v
```

JAR: target/sig-gtrat-backend-0.0.4-operacional-20261101.jar. Seus recursos estáticos devem coincidir com o dist do React do mesmo manifesto.

## Rastreabilidade e pendências

docs/LEIA_PRIMEIRO_CANDIDATA.md e as matrizes herdadas identificam a distribuição de origem. docs/HISTORICO_BASELINE_0_0_3 conserva os documentos anteriormente substituídos. Os relatórios, recibos, matrizes e checkpoints externos da entrega C25-H04-8045 identificam os comandos e resultados efetivos desta correção, sem reutilizar resultados históricos como aceite atual.

Registro histórico recebido sobre o409: logs comprovavam IND-08 negativo antes do CRUD nacional, com reprodução causal ainda pendente naquele corte. A classificação atual deve ser consultada nas novas matrizes da campanha; esse registro não constitui um resultado de execução C25-H04-8045. As validações históricas permanecem exigidas.


## Verificação histórica da distribuição de origem

Java: **500/500** unitários e `clean package` com saída 0. React: **469/469**, preparação separada **1/1** e build de produção com saída 0. Os **98 arquivos** do React estão incorporados no JAR e conferidos por SHA-256. As integrações, `-Pintegration verify`, o diagnóstico causal do 409, o navegador e o reinício continuam pendentes; consultar os relatórios da candidata.
