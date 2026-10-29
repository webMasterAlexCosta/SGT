# SGT/GTRAT — entrega pós-auditoria pré-React

Use o diretório `PROJETO_BACKEND` como raiz Maven.

1. Confirme Java 17 com `java -version`.
2. Copie `PROJETO_BACKEND/.env.example` para `PROJETO_BACKEND/.env` e substitua todos os placeholders.
3. Inicie MySQL 8.4 e crie/autorize a base configurada em `DB_URL`.
4. Execute `./mvnw clean package` ou Maven 3.9.x equivalente.
5. Inicie com `java -jar target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar`.

Validação desta entrega: 402 testes, zero falhas/erros/ignorados, Java 17, Spring Boot 3.5.16 e JAR executável gerado.

Leia primeiro:

- `PROJETO_BACKEND/docs/ETAPA_13_CORRECOES_POS_AUDITORIA_PRE_REACT.md`;
- `PROJETO_BACKEND/docs/CONTRATO_API_REACT_POS_AUDITORIA.md`.

O `.env` real não integra o pacote. As credenciais temporárias usadas pelo usuário devem ser revogadas/rotacionadas como planejado.
