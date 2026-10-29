# SGT-GTRAT — Evidências de build e testes da Etapa 08

Data da execução: **09/09/2026**

## 1. Ambiente validado

| Item | Resultado |
|---|---|
| Java | OpenJDK 17.0.20 |
| Compilação Maven | `release 17` |
| Maven | 3.9.16 |
| Spring Boot | 3.5.16 |
| Fontes principais | 505 arquivos Java |
| Fontes de teste | 111 arquivos Java |
| Docker | Indisponível neste ambiente |

O manifesto do JAR confirmou:

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
Tests run: 342, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

O relatório JaCoCo foi gerado durante o ciclo de testes. Não existe meta global de cobertura declarada para esta etapa; o critério de saída combina regressão integral, cenários DOC específicos e checkpoints estáticos.

## 3. Testes específicos da Etapa 08

| Classe | Testes | Escopo principal |
|---|---:|---|
| `DocumentoComprobatorioServicoEtapa08Test` | 15 | Fonte obrigatória, SEI compartilhado, retificação, anexo posterior, invalidação, descarte, classificação, autorização, auditoria, MIME, assinatura binária, checksum e visibilidade de rascunho |
| **Total específico** | **15** | **DOC-001–DOC-010 e segurança de armazenamento** |

Entre os cenários aprovados estão:

- mesmo SEI em Contratos distintos;
- publicação por anexo preparado sem SEI;
- bloqueio de aditamento sem fonte;
- retificação de SEI com preservação da anterior;
- anexo posterior sem substituição silenciosa do SEI;
- arquivo publicado invalidado sem remoção física;
- descarte apenas de rascunho exclusivo sem SEI;
- Operacional negado em documento restrito;
- Gestor Local auditado em acesso necessário;
- Presidência negada por padrão e liberada somente por permissão explícita;
- MIME incompatível e assinatura binária falsa bloqueados;
- adulteração física detectada no download;
- rascunho oculto de perfil não responsável.

## 4. Checkpoints reproduzíveis

### T01–T28 — regressão anterior

Os quatro reprodutores temporais e de catálogo passaram integralmente.

### T29–T50 — Etapa 06

```bash
python3 scripts/validar-checkpoint-etapa06.py
```

Resultado: **T29–T50 aprovados**.

### T51–T70 — Etapa 07

```bash
python3 scripts/validar-checkpoint-etapa07.py
```

Resultado: **T51–T70 aprovados**.

### T71–T90 — Etapa 08

```bash
python3 scripts/validar-checkpoint-etapa08.py
```

Resultado: **T71–T90 aprovados**, cobrindo plataforma, versão documental, fontes, SEI não único, substituição, invalidação, descarte, classificação, permissão, auditoria, upload seguro, checksum, idempotência, integração negocial e compatibilidade Java 17.

## 5. Empacotamento

Comando equivalente:

```bash
./mvnw -B -ntp -DskipTests package
```

Resultado: JAR Spring Boot executável gerado com sucesso. O ZIP de fontes exclui `target`, `.git`, `.env`, caches, arquivos enviados por usuários e diretórios temporários.

## 6. Limitação e validação externa obrigatória

O Docker não está instalado nesta máquina. Antes da homologação, executar:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco MySQL descartável e vazio. Validar especialmente:

- criação das quatro tabelas documentais, índices e chaves estrangeiras;
- restrições de grupo lógico + versão e chave de idempotência;
- relacionamento do mesmo SEI com vários Contratos;
- rollback de arquivo e metadados em falha transacional;
- concorrência de duas substituições do mesmo documento;
- cadeia de eventos após múltiplas retificações;
- concessão, validade e revogação de permissão restrita;
- auditoria de consulta e download em persistência real;
- isolamento de arquivo órfão sem tocar arquivo referenciado;
- tamanho máximo e volume total configurados em ambiente real.

## 7. Parecer

Build, unidade, regressão, JAR e checkpoints estão verdes. O avanço para a Etapa 09 é tecnicamente autorizado, condicionado à integração MySQL/Testcontainers antes de homologação ou implantação compartilhada.
