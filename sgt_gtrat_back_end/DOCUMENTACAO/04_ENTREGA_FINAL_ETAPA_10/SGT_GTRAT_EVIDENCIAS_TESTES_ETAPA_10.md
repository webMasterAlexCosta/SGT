# SGT-GTRAT — Evidências de testes da Etapa 10

**Execução:** 09/09/2026  
**JDK:** OpenJDK 17.0.20  
**Maven:** 3.9.16  
**Spring Boot:** 3.5.16  
**Banco real:** MySQL Community Server 8.4.11

## 1. Regressão Java completa

Comando:

```bash
mvn -o -Dmaven.repo.local=<repositorio-fornecido> clean test
```

Resultado final:

```text
Tests run: 375, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

O total anterior era 359. A Etapa 10 adicionou 16 testes direcionados:

| Classe | Testes | Cobertura |
|---|---:|---|
| `PerfilEMinimizacaoEtapa10Test` | 7 | enum único, seis perfis, escopos, escrita local, presidente e minimização |
| `PermissaoExcepcionalServicoEtapa10Test` | 6 | concessão, negação, escopo, uso, divergência e revogação |
| `CorrecaoIdentificadorContratoServicoEtapa10Test` | 3 | bloqueio sem permissão, autorização divergente e correção versionada |
| **Total novo** | **16** | |

Também foram executados todos os testes de arquitetura, contratos HTTP, etapas anteriores, segurança, frequência, temporalidade, documentos e relatórios existentes.

## 2. Integração com MySQL 8.4.11 nativo

Comando equivalente:

```bash
INTEGRATION_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/sgt_gtrat_etapa10' \
INTEGRATION_MYSQL_USER=root \
INTEGRATION_MYSQL_PASSWORD='' \
mvn -Pintegration verify
```

Resultado:

```text
18 testes de integração
0 falhas
0 erros
0 ignorados
```

Distribuição:

| Classe | Testes |
|---|---:|
| `DatabaseAvailabilityIT` | 3 |
| `DatabaseSchemaContractIT` | 2 |
| `DirectDatabaseServiceSmokeIT` | 3 |
| `RepositoryQuerySmokeIT` | 2 |
| `TokenAtivoRepositorioIT` | 3 |
| `TransactionIntegrityIT` | 5 |
| **Total** | **18** |

Esses testes criaram o schema do zero, consultaram todas as entidades e repositórios, verificaram constraints, rollback, optimistic lock, concorrência de inserção, reuso do pool e SQL direto dos serviços.

## 3. Carga concorrente opt-in

Comando equivalente:

```bash
mvn -Pdb-load verify
```

Resultado medido:

| Métrica | Resultado | Limite |
|---|---:|---:|
| usuários virtuais | 24 | — |
| duração | 15 s | — |
| operações | 463.263 | > 0 |
| throughput | 30.855,56 ops/s | >= 10 ops/s |
| P50 | 0 ms | — |
| P95 | 2 ms | <= 2.000 ms |
| P99 | 6 ms | — |
| falhas | 0 | <= 0,5% |
| escritas | 93.001 | — |
| incrementos persistidos | 93.001 | igual às escritas |

Não houve erro nem lost update.

## 4. Inicialização integral do artefato

O JAR empacotado foi iniciado contra outra base MySQL 8.4 vazia, usando `.env` derivado do exemplo e `ddl-auto=update`.

Evidência:

```text
Started AplicacaoBackendSigGtrat in 18.149 seconds
HTTP /: 404
```

O `404` na raiz é esperado porque esta entrega não inclui o build React; a resposta HTTP confirma Tomcat, filtros, segurança, beans e schema ativos.

## 5. Verificações estáticas de limpeza

Foram pesquisadas as fontes de produção por `ROLE_*`, `roles`, `getRoles`, `setRoles` e `findByRoles`: nenhuma ocorrência do modelo de autorização antigo permaneceu. A única ocorrência semelhante a “compatibilidade” corresponde ao substantivo funcional `incompatibilidades` da prévia de alocação, sem relação com legado.

## 6. Observações

- Os avisos de `drop foreign key` emitidos por Hibernate antes da primeira criação em `create-drop` são tolerados pelo gerador e não representam falha de teste.
- A mensagem de duplicate key em `TokenAtivoRepositorioIT` é provocada deliberadamente para comprovar a constraint única.
- O caminho Testcontainers continua disponível; nesta validação foi usado o binário MySQL 8.4.11 fornecido, sem Docker.
