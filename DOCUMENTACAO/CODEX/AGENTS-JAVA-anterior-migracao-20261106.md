# SGT — derivada PostgreSQL

- Escopo autorizado: Java/React nesta cópia isolada; remover Governança de Terceiros, processos exclusivos e risco manual/contínuo. Preservar núcleo operacional, Fornecedor compartilhado, GOV-003, documentos, histórico/auditoria, regras/perfis/escopos/senha/CSRF/locks/transações/fotografias e a interpretação dos sete dias.
- Ordem de 06/11/2026: PostgreSQL Server 18.6 real/pgJDBC 42.7.14 substituem MySQL histórico; manter Boot 3.5.16/release 17. Persistência/integração/concorrência exigem esse PostgreSQL real; mocks só em testes isolados.
- Só Hibernate/JPA gere schema: ddl-auto=update, spring.sql.init.mode=never; sem DDL manual, Flyway/Liquibase ou purga. Exceção específica prod: ddl-auto=validate e generate-ddl=false, sem garantia de detectar todo drift.
- Integração exige INTEGRATION_POSTGRES_URL/USER/PASSWORD explícitos e role limitada; sem fallback/containers automáticos ou Testcontainers. Considere cgroup efetivo/orçamento agregado; sequenciamento das suítes e revisão seguem o global.
- Sem deploy/push/alteração de produção/concessão de privilégios sem autorização específica. Não empacote .env, credenciais, tokens/cookies ou diretórios privados; root não contorna bloqueios.
- Use checkpoints/recibos, mesmos bytes de JAR/build e evidência antes de concluir. Candidata ainda pendente de integração/409/navegador/reinício; matriz/relatórios antigos não aprovam bytes atuais. O pedido atual substitui Java congelado e os 198 requisitos sem mudança de escopo herdados.
