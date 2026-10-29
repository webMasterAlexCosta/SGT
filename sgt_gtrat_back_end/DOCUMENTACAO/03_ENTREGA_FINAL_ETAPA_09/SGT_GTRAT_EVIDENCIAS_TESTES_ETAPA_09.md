# SGT-GTRAT — Evidências de build, regressão e MySQL da Etapa 09

**Data da execução:** 09/09/2026

## 1. Ambiente

| Item | Resultado |
|---|---|
| Spring Boot | 3.5.16 |
| `pom.xml` Java | 17 |
| Compilação Maven | `release 17` |
| Maven | 3.9.16 |
| JVM disponível na execução | OpenJDK 21.0.11 |
| MySQL real | Community Server 8.4.11 |
| Banco de integração | descartável, vazio e exclusivo `sgt_etapa09` |
| Fontes principais | 517 |
| Fontes de teste | 112 |

## 2. Suíte completa

Comando de regressão/integração usado no fechamento:

```bash
mvn -B -ntp -Pintegration verify
```

No ambiente desta sessão, Maven foi executado offline com o repositório fornecido e com as variáveis de conexão para o MySQL externo.

Resultados:

```text
Tests run: 359, Failures: 0, Errors: 0, Skipped: 0
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os 18 testes de integração cobrem disponibilidade/pool, descoberta e execução de queries dos repositórios, integridade transacional, concorrência/unique, `@Version`, contrato do schema Hibernate, `TokenAtivoRepositorio` e chamadas diretas de banco ainda existentes em serviços.

## 3. Testes específicos da Etapa 09

Classe: `FrequenciaEtapa09Test`

Resultado final:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Cenários cobertos incluem:

- primeiro/sétimo/oitavo dia;
- conversão por `ZoneId` de São Paulo sem offset fixo;
- resolução temporal de Contrato/Vínculo/Alocação/Gestão;
- bloqueio de Contrato historicamente inativo;
- perda retroativa de cobertura;
- reativação sem restauração silenciosa;
- mudança de Alocação sem reassociação automática;
- hash/cadeia da auditoria;
- idempotência por intenção;
- fechamento automático persistido;
- toda escrita do Gestor como retificação formal;
- ausência da regra legada de prazo por `updatedAt`;
- consumidores oficiais ignorando frequência não válida;
- hooks retroativos de Vínculo/Alocação/Contrato/Aditamento;
- endpoint de eventos;
- jornada noturna ligada à data de início.

## 4. Checkpoints

Resultado acumulado final:

- T01–T28 / pre-Etapa 06: aprovados;
- T29–T50: aprovados;
- T51–T70: aprovados;
- T71–T90: aprovados;
- T91–T110: aprovados.

## 5. JAR

JAR Spring Boot executável gerado durante `verify`.

Manifesto observado:

```text
Build-Jdk-Spec: 21
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.siggtrat.AplicacaoBackendSigGtrat
Spring-Boot-Version: 3.5.16
```

O `Build-Jdk-Spec: 21` reflete a JVM da máquina de execução, não o alvo de compilação. Os compiladores principal e de teste registraram explicitamente `release 17`.

## 6. Testcontainers

O caminho padrão do perfil de integração continua usando Testcontainers quando `INTEGRATION_MYSQL_URL` não é fornecida. Docker não está disponível neste ambiente, então essa variante não foi executada aqui.

Antes da homologação final, executar em máquina com Docker e JDK 17:

```bash
./mvnw -B -ntp -Pintegration verify
```

## 7. Parecer

A Etapa 09 possui evidência forte de regressão, schema e persistência MySQL real. A limitação remanescente é exclusivamente ambiental para provar a mesma execução em runtime JDK 17 e Testcontainers/Docker.
