# SGT-GTRAT — Evidências de build e testes da Etapa 06

Data da execução: **09/09/2026**

## 1. Ambiente confirmado

| Item | Resultado |
|---|---|
| Java de compilação | release 17 |
| Spring Boot | 3.5.16 |
| Maven | 3.9.16 |
| Código de produção | 456 arquivos Java |
| Código de teste | 106 arquivos Java |
| Relatórios Surefire | 76 classes/suítes |
| Banco esperado | MySQL em schema novo por versão |

O manifesto do JAR executável confirmou:

```text
Build-Jdk-Spec: 17
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.siggtrat.AplicacaoBackendSigGtrat
Spring-Boot-Version: 3.5.16
```

SHA-256 do JAR gerado durante a validação:

```text
42925d365786bca96d24c6fe4c74b771c5ce995216f12c7d4e0ec861e6e41d63
```

O diretório `target` e o JAR não são incluídos no ZIP de fontes; o hash acima identifica o resultado reproduzido neste ambiente.

## 2. Suíte JUnit completa

Comando equivalente executado, usando o repositório Maven fornecido e modo offline:

```bash
mvn -B -ntp -o -Dmaven.repo.local=/caminho/repository clean test
```

Resultado:

```text
Tests run: 315, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

A suíte contém 292 verificações herdadas e 23 testes específicos da Etapa 06:

| Classe | Testes | Resultado |
|---|---:|---|
| `PessoaVinculoEtapa06Test` | 6 | Aprovado |
| `MotorTemporalVinculoEtapa06Test` | 4 | Aprovado |
| `PessoaServicoEtapa06Test` | 3 | Aprovado |
| `VinculoContratualServicoEtapa06Test` | 3 | Aprovado |
| `VinculoContratualCicloVidaEtapa06Test` | 7 | Aprovado |
| **Total específico** | **23** | **Aprovado** |

Durante um teste herdado de persistência de erro, a suíte registra propositalmente uma stack trace de exceção simulada. Ela é parte do cenário testado e não representa falha: o resumo final permaneceu com zero falhas e zero erros.

## 3. Checkpoints T01–T50

Foram reproduzidos os scripts:

```bash
python3 scripts/reproduzir-regressao-temporal-pre-etapa06.py
python3 scripts/reproduzir-regressao-complementar-pre-etapa06.py
python3 scripts/reproduzir-catalogo-motivo-desligamento-pre-etapa06.py
python3 scripts/reproduzir-catalogos-base-pre-etapa06.py
python3 scripts/validar-checkpoint-pre-etapa06.py
python3 scripts/validar-checkpoint-etapa06.py
```

Resultados:

- T01–T16: aprovados;
- T17–T21: aprovados;
- T22–T24: aprovados;
- T25–T28: aprovados;
- T29–T50: aprovados;
- checklist estrutural pré-Etapa 06: aprovado integralmente.

O script T25–T28 aceita ambientes que disponibilizam apenas `java`, usando `com.sun.tools.javac.Main` quando o executável `javac` não está no `PATH`. A compilação continua obrigatoriamente em `--release 17`.

## 4. Build empacotável

Comando:

```bash
mvn -B -ntp -o -Dmaven.repo.local=/caminho/repository -DskipTests package
```

Resultado: `BUILD SUCCESS`, com JAR Spring Boot executável.

## 5. Segurança do pacote

Antes da compactação foram verificados:

- ausência de `.env` real;
- ausência de chaves privadas e certificados `.p12`, `.pfx`, `.jks` ou `.pem`;
- ausência de padrões comuns de token/chave no código-fonte;
- presença apenas de placeholders em `.env.example`;
- exclusão de `target`, caches, metadados de IDE e artefatos temporários do ZIP.

## 6. Limitação da validação

O comando abaixo **não foi executado**, porque o ambiente não possuía o executável Docker:

```bash
mvn -B -ntp -Pintegration verify
```

Consequentemente, esta entrega não deve ser chamada de homologada em MySQL. Antes da homologação, é obrigatório executar o perfil `integration` com Docker/Testcontainers, em banco descartável, e guardar os relatórios Failsafe.

## 7. Critério de avanço

O checkpoint de unidade, regressão e build está verde. O avanço para a Etapa 07 é tecnicamente autorizado, condicionado à execução paralela da integração MySQL antes de qualquer homologação ou implantação compartilhada.
