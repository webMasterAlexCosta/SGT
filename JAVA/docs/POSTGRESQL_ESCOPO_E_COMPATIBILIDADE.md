# PostgreSQL: escopo comprovado e limites de compatibilidade

O aceite focal desta campanha refere-se a uma **base PostgreSQL nova**. Ele não comprova a migração de uma base existente, nem autoriza reutilizar automaticamente dados ou estados produzidos por versões anteriores do SGT.

## Evidências disponíveis

- A origem MySQL configurada foi autenticada e consultada: versão 8.0.45 e zero tabelas no schema configurado. O recibo é `31_INVENTARIO_FONTE_LEGADO/CONFIGURED_MYSQL_IDENTITY_EXCLUSIVE_DEFAULTS.json`. Isso descreve somente a origem efetivamente consultada; não comprova a ausência de dados em outros acervos.
- A homologação atual usa PostgreSQL real 18.6 em instância isolada. Na base ITv2 inicialmente vazia, Hibernate/JPA criou 72 tabelas. Os 21 testes selecionados de Fornecedor/Frequência passaram nesse recorte. Recibos: `42_REMOCAO_CODIGO_OBSOLETO/FRESH_JPA_TEST_SUMMARY.json` e `FRESH_JPA_SCHEMA_METADATA.json`.
- A base appv2 estava vazia e não havia sido iniciada no recibo de 06/11/2026 às 10:52:58 UTC. A execução posterior do JAR precisa de evidência própria; esse recibo não a antecipa.

## O que não foi homologado

Não foi comprovada transferência de um acervo não vazio, conversão de schema antigo ou compatibilidade integral com estados persistidos pelas versões anteriores. Não se atribui prova de migração a testes com dados sintéticos novos, à alteração da configuração de conexão ou à criação de tabelas pela JPA.

O modelo atual retirou o mapeamento legado da coluna `fornecedor.monitoramento_status`. Um schema antigo que ainda exija essa coluna no INSERT pode ser incompatível; o aceite da base nova não ensaia esse upgrade. Na fase 43, também foi delimitado que correções manuais persistidas pelo código anterior podem conservar marcadores antigos de ocorrência. Reutilizar esses estados exige avaliação própria; não se presume que a correção dos novos comandos os converteu.

As bases anteriores, os eventos, os hashes e os recibos permanecem preservados. Não foram apagados, recalculados ou promovidos a fatos da base nova para obter passagem dos testes. A dispensa de manter código legado não dispensa preservar dados e evidências.

## Esquema e operação

Hibernate/JPA continua sendo o único gestor do schema de domínio. Não há autorização para DDL manual, `schema.sql`, Flyway ou Liquibase. O perfil de produção usa validação do schema; isso não constitui um processo de transferência ou conversão de dados.

Uma eventual importação de dados antigos precisa identificar origem, destino, versão do artefato e representação dos fatos, além de demonstrar sua preservação e releitura. Até existir essa prova, a troca de banco está validada apenas no escopo funcional documentado da baseline PostgreSQL nova. Não há homologação integral de migração nem deploy de produção nesta declaração.
