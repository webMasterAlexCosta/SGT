# Gatling - testes de carga de engenharia

Este diretório é propositalmente isolado do `pom.xml` principal. O objetivo é usar
uma engine especializada de performance sem poluir o classpath de testes do
Spring Boot.

## Executar

Com o backend já iniciado:

```bash
cd performance-tests
mvn gatling:test
```

O Gatling gera relatório HTML próprio em `target/gatling/` com gráficos e
estatísticas de latência, throughput, erros e percentis.

## Parâmetros

Exemplo com 100 usuários concorrentes por 2 minutos:

```bash
mvn gatling:test \
  -Dload.users=100 \
  -Dload.rampSeconds=15 \
  -Dload.durationSeconds=120 \
  -Dload.thinkTimeMs=500 \
  -Dload.maxP95Ms=2000 \
  -Dload.maxFailedPct=1
```

Endpoint autenticado:

```bash
mvn gatling:test \
  -Dload.endpoint=/api/painel \
  -Dload.token=SEU_JWT
```

Parâmetros disponíveis:

- `load.baseUrl` (padrão `http://localhost:8080`)
- `load.endpoint` (padrão `/api/auth/superintendencias`)
- `load.token` (JWT opcional)
- `load.users` (usuários concorrentes; padrão 25)
- `load.rampSeconds` (rampa; padrão 5s)
- `load.durationSeconds` (patamar; padrão 30s)
- `load.thinkTimeMs` (pausa por usuário; padrão 1000ms)
- `load.maxP95Ms` (SLO de P95; padrão 1500ms)
- `load.maxFailedPct` (máximo de falhas; padrão 1%)

> O cenário fornecido é GET e leitura. Para endpoints de escrita, crie uma
> simulação dedicada com massa de dados descartável/Testcontainers; não reutilize
> carga destrutiva contra a base de desenvolvimento.
