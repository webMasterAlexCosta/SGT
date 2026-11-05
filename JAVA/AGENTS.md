# AGENTS.md — candidata de remoção Governança SGT

O pedido atual autoriza Java e React nesta cópia isolada. Remover Governança de Terceiros, processos exclusivos e risco manual/contínuo; preservar núcleo operacional, Fornecedor compartilhado, GOV-003, documentos, histórico e auditoria.
- Hibernate/JPA é o único gestor de schema: ddl-auto=update, spring.sql.init.mode=never. Sem DDL manual, Flyway/Liquibase ou purga.
- MySQL Server8.0.45 real. Connector/J 9.7.0 é driver e permanece.
- Preservar regras/perfis/escopos/senha/CSRF/locks/transações/fotografias. Não reinterpretar os sete dias.
- Nenhum deploy, push, alteração de produção ou concessão de privilégios sem autorização específica.
- Persistência, integração e concorrência precisam de MySQL real; mocks apenas em testes isolados.
- Uma suíte pesada por vez, considerando cgroup efetivo e orçamento agregado.
- Não empacotar .env, credenciais de ambiente, tokens/cookies ou diretórios privados.
- Usar checkpoints, recibos de comandos, mesmos bytes de JAR/build, revisão independente e evidência antes de conclusão.
Esta é candidata pendente de integração/409/navegador/reinício. Matriz e relatórios históricos não aprovam os bytes atuais. A conta root não pode contornar bloqueios. As orientações herdadas de Java congelado e198 requisitos sem mudança de escopo foram substituídas pelo pedido atual.

