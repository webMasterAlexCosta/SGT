# Adaptação ao MySQL Server 8.0.46
Data documental: 01/11/2026, America/Sao_Paulo.
Estado: adaptação de código/configuração em execução; compatibilidade integral da aplicação PENDENTE de integração autorizada.

## Identidade e instância
Pacote: mysql-8.0.46-linux-glibc2.17-x86_64-minimal.tar.xz, 61.608.108 bytes, libfile_48f2f8921fa88191810e425d4b5a354a.
SHA-256 conferido: e2777d490fcaadc5a4be06db89b6935c78909bd9c3382af859ab284af0c950b0.
Linux x86_64/glibc2.39 executa o binário que informa MySQL 8.0.46. Diretório de dados completamente novo, sem downgrade.
evidencias/mysql8046_instancia_inicial.json registra SELECT VERSION()=8.0.46, UUID da instância, utf8mb4/utf8mb4_0900_ai_ci e apenas catálogos de sistema. Rede desabilitada (porta efetiva0), socket privado. Porta TCP planejada33076 ainda não usada pela aplicação.
O cliente mysql desse pacote precisava libncurses.so.5/libtinfo.so.5, ausentes no host. A consulta inicial foi feita por PyMySQL 1.1.2 isolado. Depois, os pacotes oficiais Ubuntu libncurses5/libtinfo5 6.3-2ubuntu0.3 foram obtidos e extraídos apenas em runtime/mysql-client-libs, sem instalar ou alterar o SO. Com LD_LIBRARY_PATH privado, o cliente também informa 8.0.46. Hashes, URLs e tamanhos: evidencias/mysql_cliente_dependencias.json. Dependências da aplicação continuam inalteradas.

## Driver
mysql.version=9.7.0 no pom é Connector/J e foi mantido. As notas oficiais de Connector/J 9.7.0 declaram suporte a servidores MySQL 8.0 e posteriores:
https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html
Isso é evidência documental de suporte; a conexão desta aplicação via Connector/J ainda deve ser demonstrada em MySql8046AmbienteHttpIT, HTTP e reinício.

## Alterações
MATRIZ_MYSQL.json relaciona cada arquivo, linhas anteriores e hashes.
- Containers dos dois suportes: mysql:8.0.46.
- Guards explícitos: versão exatamente 8.0.46, substituindo startsWith8.4; nenhuma asserção de integridade ou negócio foi removida.
- Script fotografar_persistencia_jar.py conserva validação de FK/strict/schema/dados e passa a exigir 8.0.46.
- Novo MySql8046AmbienteHttpIT: produto/versão via metadata E SELECT VERSION() na conexão da aplicação, catálogo, UUID, porta, driver, charset/collation, JSON, microssegundos, FKs e propriedades de schema.
- ddl-auto=update e spring.sql.init.mode=never mantidos nos perfis.
- Não há DDL manual de aplicação, Flyway/Liquibase, remoção automática de tabelas/colunas ou concessão de privilégios.
- Referências8.4 em relatórios históricos permanecem como histórico, sem representar alvo atual.

## Consultas sem mutação já executadas
evidencias/mysql8046_consultas_sem_mutacao.json registra JSON booleano true e número0, DATETIME(6) preservando123456 microssegundos, comparação de acento/caixa sob utf8mb4_0900_ai_ci, foreign_key_checks=1, innodb_strict_mode=1, REPEATABLE-READ e sessão+00:00.
Essas consultas não criam dados/tabelas nem substituem prova de locks, rollback, esquema Hibernate, timezone de negócio ou consultas da aplicação.

## Verificações de aplicação ainda necessárias
Inicialização/update do schema vazio e legado; constraints/índices/FKs; persistência temporal e ordenação real; queries JSON eventualmente usadas; escopo/autorização; locks e timeout/rollback; pools; mesma conexão8.0.46 no teste e JAR; reinício e inventário das estruturas históricas.
Consultar ACESSO_NECESSARIO_PARA_INTEGRACAO.md. Não declarar compatibilidade integral ou homologação a partir do binário, de metadata isolada ou de build.


## Estruturas legadas identificadas nas entidades de origem

| Estrutura/coluna | Situação na candidata | Evidência atual e limite |
|---|---|---|
| processo_terceiro, índice idx_processo_terceiro_status_prazo, vínculos id_empregado/id_contrato | Entidade e repositório removidos; nenhum fluxo novo depende da tabela. | Inspeção de fontes/JAR; fotografia física antes/depois ainda pendente. |
| avaliacao_risco_fornecedor, índice idx_risco_fornecedor_proxima e vínculo id_fornecedor | Entidade, repositório, serviços e rotas removidos. | Nomes antigos aparecem somente nos testes negativos/relatórios; preservação física ainda pendente. |
| fornecedor.risco_score, fornecedor.risco_nivel, fornecedor.proxima_avaliacao | Saíram do mapeamento ativo; nenhuma purga ou alteração manual. | Colunas explicitamente nomeadas no mapeamento anterior. Inventário do schema real ainda pendente. |
| fornecedor.monitoramento_status | Mapeamento privado inerte mantido para compatibilidade com NOT NULL antigo, sem getter/setter e sem UPDATE. | Necessidade estrutural identificada; prova real de INSERT em schema legado ainda pendente. |
| fornecedor.id_fornecedor, cnpj, razaoSocial, email, ativo, version e FK de contratos | Preservados no cadastro e nos consumidores temporais. | Hashes/contratos/testes unitários; integração de cadastro e upgrade ainda pendente. |

Nenhuma tabela ou coluna antiga foi declarada fisicamente apagada. Retirada de responsabilidade de aplicação e remoção física são decisões diferentes. Limpeza física continua fora desta autorização.

## Dependências nativas auxiliares

Os pacotes libncurses5/libtinfo5 vieram do repositório HTTPS oficial Ubuntu; os links exatos e SHA-256 constam no recibo. Foram somente extraídos no diretório isolado e carregados pelo cliente 8.0.46. Não se usou binário ou datadir 8.4.11, nem link ABI fictício para libncurses6.
