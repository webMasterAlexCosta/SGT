# SGT/GTRAT — Backend Java final

Backend encerrado na Etapa 12 com Java 17, Spring Boot 3.5.16 e MySQL 8.4.

## Validação rápida

```bash
mvn clean test
node scripts/validar-nomenclatura.mjs
```

## Execução

1. Crie um banco MySQL 8.4 vazio chamado `sgt_gtrat`.
2. Copie `.env.example` para `.env`.
3. Substitua credenciais, senhas iniciais, segredo JWT e segredo de auditoria.
4. Execute `mvn spring-boot:run`.

Não use Java 21 ou Spring Boot 4 neste projeto. Não existe migração de banco anterior; desenvolvimento e testes partem de banco novo. O contrato para o React está em `../DOCUMENTACAO/06_ENTREGA_FINAL_ETAPA_12/SGT_GTRAT_API_CONGELADA_BACKEND_JAVA.md`.
