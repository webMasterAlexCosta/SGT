# Acesso específico necessário para concluir a validação
Data documental: 01/11/2026, America/Sao_Paulo.

## Estado
A instância nova foi inicializada exclusivamente em privado/mysql8046/data, sem reutilizar dados 8.4. O processo e SELECT VERSION() informam 8.0.46. A rede está desabilitada; somente o socket privado permite a leitura inicial de metadados. Nenhum catálogo da aplicação, usuário ou GRANT foi criado. Não foi feita conexão da aplicação com root. A configuração TCP planejada é 127.0.0.1:33076, a ativar somente para o ambiente de teste autorizado.

O item 11 do pedido determina autorização específica antes de conceder privilégios. A autorização de implementação e inicialização não substitui essa decisão. As negativas anteriores para sgt_runtime@127.0.0.1 e sgt_baseline_crud_20261101 permanecem respeitadas; estas contas/bases não serão usadas.

## Proposta concreta de menor alcance
| Item | Destino | Operação/justificativa |
|---|---|---|
| Conta nova | sgt_validacao_8046@127.0.0.1 | Conta restrita à conexão local da instância sintética nova; senha aleatória privada. |
| Catálogos novos | sgt8046_baseline, sgt8046_candidata, sgt8046_upgrade, sgt8046_http | Comparação isolada; regressão candidata; preservação baseline→candidata; JAR/navegador/reinício. Nenhum dado real. |
| Privilégios apenas nesses quatro catálogos | SELECT, INSERT, UPDATE, DELETE | Exercitar fluxos e fixtures sintéticos e validar transações/rollback. DELETE já é usado por testes existentes, nunca para limpeza de dados do usuário. |
| Privilégios apenas nesses quatro catálogos | CREATE, ALTER, INDEX, REFERENCES | Permitir exclusivamente que Hibernate/update crie e atualize tabelas, índices e FKs. Nenhum script de tabelas. |
| Leitura de observabilidade | SELECT em performance_schema.data_lock_waits, performance_schema.data_locks e performance_schema.threads | As barreiras dos testes existentes observam locks reais. As fontes foram inventariadas em evidencias/priviliegios_observacao_referencias.json. |
| Observação de transações | PROCESS, somente nesta instância descartável | Necessário para information_schema.innodb_trx usado em AuditoriaFinalCorrecaoCpfControlesHttpIT. É privilégio de alcance da instância, não granular por catálogo; não dá execução de comandos em produção. |

Não se solicita ALL PRIVILEGES, GRANT OPTION, SUPER, FILE, criação de usuários pela conta de testes, DROP de catálogo/tabelas ou acesso a outra instância. A criação inicial dos quatro catálogos vazios e da conta faz parte da operação administrativa a autorizar; todas as tabelas da aplicação continuam exclusivamente sob Hibernate.

## O que fica bloqueado enquanto não autorizado
Inicialização do schema pela aplicação, integrações MySQL/Redis, reprodução causal do 409, testes HTTP do JAR, preservação dos dados legados, navegador completo com backend e comparação de reinício. A leitura de VERSION() e funções sem tabela não equivale a esses testes.

Nenhum pedido de GRANT foi enviado nesta rodada e nenhuma rejeição automática nova de GRANT ocorreu. A pausa resulta da instrução expressa do usuário. As preparações de fonte, testes unitários, builds e revisão documental continuam independentemente.

