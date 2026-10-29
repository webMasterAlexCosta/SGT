# SGT/GTRAT — Validação de build e testes

**Data:** 08/09/2026  
**Base:** Spring Boot 3.5.16 + Java 17 + Maven 3.9.16

## Ambiente utilizado

- Java runtime: 17.0.20;
- compilador configurado no Maven: `release 17`;
- Maven: 3.9.16;
- dependências: repositório Maven offline fornecido junto aos arquivos da revisão;
- Docker: indisponível neste ambiente.

## Comandos e resultados

### Suíte padrão

```bash
mvn -o -B -ntp clean test
```

Resultado:

```text
419 fontes de produção compiladas
101 fontes de teste compiladas
292 testes executados
0 falhas
0 erros
0 ignorados
BUILD SUCCESS
```

### Empacotamento

```bash
mvn -o -B -ntp -DskipTests clean package
```

Resultado: `BUILD SUCCESS`.

O JAR Spring Boot foi gerado, validado com `unzip -t` e contém:

- `Main-Class: org.springframework.boot.loader.launch.JarLauncher`;
- `Start-Class: com.example.siggtrat.AplicacaoBackendSigGtrat`;
- `Spring-Boot-Version: 3.5.16`;
- bytecode de produção e testes com major version **61**, correspondente a Java 17.

### Validações direcionadas

```bash
python3 scripts/validar-checkpoint-pre-etapa06.py
python3 scripts/reproduzir-regressao-temporal-pre-etapa06.py
python3 scripts/reproduzir-regressao-complementar-pre-etapa06.py
python3 scripts/reproduzir-catalogo-motivo-desligamento-pre-etapa06.py
python3 scripts/reproduzir-catalogos-base-pre-etapa06.py
```

Resultado consolidado: checkpoint estático aprovado e cenários T01–T28 aprovados.

## Ajuste de infraestrutura de testes realizado nesta entrega

O Mockito Inline precisava anexar o Byte Buddy dinamicamente. Em ambientes restritos, isso fazia o comando Maven falhar antes de executar os testes. O `pom.xml` passou a:

- declarar `byte-buddy-agent` como dependência de teste;
- iniciar o agente explicitamente no Surefire;
- preservar o agente JaCoCo por meio de avaliação tardia de `argLine`.

Esse ajuste não altera o código funcional do sistema; torna a execução da suíte reproduzível em Java 17.

## Limitação ainda aberta

Os testes de integração `*IT` dependem de MySQL via Testcontainers. Eles não foram certificados nesta sessão porque não há executável Docker nem socket Docker disponível.

Executar no ambiente com Docker:

```bash
mvn -Pintegration verify
```

Esse é o próximo gate técnico de persistência. A ausência de Docker não anulou os resultados dos testes unitários, arquiteturais, de serviço e das regressões temporais já executadas.

