# SGT-GTRAT — Evidências de build e testes da Etapa 07

Data da execução: **09/09/2026**

## 1. Ambiente validado

| Item | Resultado |
|---|---|
| Java | OpenJDK 17.0.20 |
| Compilação Maven | `release 17` |
| Maven | 3.9.16 |
| Spring Boot | 3.5.16 |
| Fontes principais | 479 arquivos Java |
| Fontes de teste | 110 arquivos Java |
| Docker | Indisponível neste ambiente |

O manifesto do artefato confirmou:

```text
Build-Jdk-Spec: 17
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.siggtrat.AplicacaoBackendSigGtrat
Spring-Boot-Version: 3.5.16
```

## 2. Suíte completa

Comando equivalente reproduzível:

```bash
./mvnw -B -ntp clean test
```

Resultado final:

```text
Tests run: 327, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

O relatório JaCoCo foi gerado durante o ciclo de testes. Não há meta de cobertura global declarada nesta etapa; o critério de saída é a regressão integral mais os cenários específicos e checkpoints.

## 3. Testes específicos da Etapa 07

| Classe | Testes | Escopo principal |
|---|---:|---|
| `AlocacaoVinculoServicoEtapa07Test` | 5 | Faixa sucessora, retificação na mesma data, cancelamento futuro, lacuna e revalidação na reativação |
| `MotorTemporalAlocacaoEtapa07Test` | 3 | Data efetiva, data de conhecimento, integridade e reconstrução exata da divisão de faixa |
| `ComposicaoTransferenciaGestaoEtapa07Test` | 2 | Aplicação temporal do mapeamento e bloqueio de composição sem mapa |
| `TransferenciaGestaoContratoServicoEtapa07Test` | 2 | Transferência transacional e rejeição de contexto inválido |
| **Total específico** | **12** | **Etapa 07** |

## 4. Checkpoints reproduzíveis

### T01–T28 — regressão anterior

Foram executados os quatro reprodutores temporais e de catálogo. Todos os cenários T01–T28 passaram, incluindo:

- cadeias de retificação e substituição;
- remanejamento e aditamento em conflito;
- piso original e soma de efetivo;
- integridade por hash;
- contrato cancelado sem reabertura;
- publicação com fotografia integral;
- governança de Motivo de Desligamento, Cargo, Lotação e Turno.

### T29–T50 — Etapa 06

`python3 scripts/validar-checkpoint-etapa06.py`

Resultado: **T29–T50 aprovados**.

### T51–T70 — Etapa 07

`python3 scripts/validar-checkpoint-etapa07.py`

Resultado: **T51–T70 aprovados**, cobrindo:

- versões Spring Boot 3 e Java 17;
- faixa temporal e controle de versão;
- fotografias e cadeia SHA-256;
- reconstrução bitemporal;
- fechamento da faixa anterior e retificação na mesma data;
- cobertura integral e unicidade diária;
- efetivo e catálogo válidos;
- consulta mensal e seleção encadeada;
- preservação em inativação e revalidação na reativação;
- sincronização com o Vínculo;
- transferência, autorização e mapeamentos;
- compatibilidade Java 17 e ausência de `.env` real.

## 5. Empacotamento

Comando equivalente:

```bash
./mvnw -B -ntp -DskipTests package
```

Resultado: JAR Spring Boot executável gerado com sucesso. O ZIP de fontes exclui `target`, `.git`, `.env`, caches e arquivos temporários.

## 6. Limitação e validação obrigatória externa

O Docker não está instalado nesta máquina, portanto o perfil MySQL/Testcontainers não foi executado. Antes da homologação:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco MySQL descartável e vazio. Validar especialmente:

- criação das tabelas e índices de Alocação, eventos, transferência e mapeamentos;
- integridade das chaves estrangeiras;
- rollback integral de transferência incompleta;
- concorrência em mudanças de faixa e ocupação;
- consultas mensais e bitemporais;
- bloqueio de lacunas e sobreposições em persistência real;
- reativação com composição válida e inválida.

## 7. Parecer

Build, unidade, regressão e checkpoints estão verdes. O avanço para a Etapa 08 é tecnicamente autorizado, condicionado à integração MySQL/Testcontainers antes de homologação ou implantação compartilhada.

