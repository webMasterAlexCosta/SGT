# SGT/GTRAT — Evidências de testes da Etapa 12

**Data de fechamento:** 10/09/2026  
**Ambiente:** Java 17.0.20, Maven 3.9.16, Spring Boot 3.5.16 e MySQL Community Server 8.4.11.

## Portões executados

| Portão | Resultado |
|---|---:|
| Fontes Java compiladas | 564 |
| Arquivos de teste compilados | 111 |
| Testes unitários/arquiteturais | 396 aprovados; 0 falhas; 0 erros; 0 ignorados |
| Testes de integração MySQL | 22 aprovados; 0 falhas; 0 erros; 0 ignorados |
| Teste de carga MySQL | 1 aprovado |
| Arranque completo do JAR | aplicação iniciada; `/config.json` respondeu HTTP 200 |
| Reconstrução a partir do ZIP | checksum interno aprovado; nomenclatura aprovada; 396/396 testes aprovados |
| Validação de nomenclatura | 564 fontes e 68 entidades com tabela explícita; aprovada |
| Banco da integração | instância e diretório de dados novos; schema `create-drop` |

## Integração MySQL 8.4.11

A suíte nativa validou disponibilidade, criação e remoção do schema, consultas de repositório, rollback, bloqueio otimista, inserção concorrente, constraints de idempotência e versão, além de persistência de relatório binário.

Durante essa execução foi detectado que o tipo implícito da coluna de snapshot não comportava 32 KiB. A entidade `EmissaoRelatorioAdministrativo` foi corrigida para `LONGBLOB`; a suíte completa foi repetida e os 22 testes passaram.

Em uma terceira instância descartável, o JAR repacotado foi iniciado com todos os serviços, segurança e jobs, usando schema novo. `AplicacaoBackendSigGtrat` iniciou em 45,524 s e `GET /config.json` respondeu HTTP 200.

## Carga nativa

| Medida | Resultado |
|---|---:|
| Usuários virtuais | 24 |
| Duração | 15 s |
| Operações totais | 450.821 |
| Leituras | 360.762 |
| Escritas | 90.059 |
| Falhas | 0 (0,0000%) |
| Vazão média | 30.047,08 operações/s |
| Latência P50 | 0 ms |
| Latência P95 | 2 ms |
| Latência P99 | 6 ms |
| Incrementos persistidos | 90.059 de 90.059 esperados |

Esse resultado caracteriza o cenário controlado executado na máquina de validação; não é uma promessa universal de capacidade de produção.

## Verificação do pacote distribuível

O ZIP foi testado com `unzip -t`, extraído em diretório novo e validado sem `target`, `.env` ou uploads. O manifesto interno passou em `sha256sum -c`; em seguida, a cópia extraída compilou as 564 fontes e repetiu os 396 testes com sucesso usando apenas o repositório Maven fornecido para a validação.

## Cobertura JaCoCo real

| Métrica | Coberto | Não coberto | Percentual |
|---|---:|---:|---:|
| Instruções | 34.026 | 88.612 | 27,75% |
| Ramos | 2.378 | 10.585 | 18,34% |
| Linhas | 6.584 | 14.888 | 30,66% |
| Métodos | 1.972 | 3.313 | 37,31% |
| Classes | 278 | 347 | 44,48% |

Os 198/198 da matriz significam aderência funcional auditada. Não devem ser confundidos com cobertura estrutural de 100%.

## Segurança direcionada

`SegurancaAdversarialEtapa12Test` adiciona três verificações: origem CORS hostil não autorizada, método TRACE sem cabeçalhos CORS e presença das decisões de segurança para TRACE negado, sessão stateless, HSTS, CSP, proteção de frame, Referrer-Policy e Permissions-Policy.

## 29 cenários de aceite de borda

| # | Cenário | Evidência principal | Resultado |
|---:|---|---|---|
| 1 | 100 → 140 → 75 | `RegraEfetivoContratualTest` e `RegraEfetivoContratual` | Aprovado |
| 2 | piso de 101 é 76 | `RegraEfetivoContratualTest` | Aprovado |
| 3 | redução cancelada deixa de consumir limite | motor temporal + projeção de aditamentos vigentes | Aprovado |
| 4 | redução excepcional acima de 25% | `PermissaoExcepcionalServicoEtapa10Test` + Aditamento | Aprovado |
| 5 | documento antigo conhecido depois | `MotorTemporalContratoEtapa04Test` | Aprovado |
| 6 | conflito no mesmo dia sem precedência | motor temporal e validações de Aditamento/Remanejamento | Aprovado |
| 7 | inativação retroativa atinge frequência | `FrequenciaEtapa09Test` + revalidação temporal | Aprovado |
| 8 | reativação sem criar nova alocação | Contrato/Vínculo/Alocação temporal | Aprovado |
| 9 | frequência fecha no 8º dia | `FrequenciaEtapa09Test` | Aprovado |
| 10 | correção do Gestor Local no 3º dia | `FrequenciaEtapa09Test` | Aprovado |
| 11 | jornada 22:00–06:00 | `CalculoFrequenciaServicoNoturnoTest` | Aprovado |
| 12 | correção excepcional de CPF | `PessoaServicoEtapa06Test` + permissões/minimização | Aprovado |
| 13 | transferência excepcional A → B | testes de transferência/composição da etapa07 | Aprovado |
| 14 | troca real de empresa/CNPJ | Aditamento e reconstrução de `VersaoContratoNegocial` | Aprovado |
| 15 | CNPJ corrigido como erro material | fluxo excepcional auditado de Contrato/Aditamento | Aprovado |
| 16 | catálogo inativo em fato retroativo | `RegraDisponibilidadeCatalogoTest` | Aprovado |
| 17 | catálogo inativo em evento futuro | `RegraDisponibilidadeCatalogoTest` | Aprovado |
| 18 | anulação com alcance desde a origem | ContratoServico + motor temporal | Aprovado |
| 19 | anulação com alcance parcial | data efetiva do evento anulatório | Aprovado |
| 20 | contrato futuro cancelado | `ContratoServicoEtapa02Test` e temporalidade | Aprovado |
| 21 | remanejamento futuro cancelado | RemanejamentoContratualServico + motor temporal | Aprovado |
| 22 | matrícula obrigatória | `VinculoContratualServicoEtapa06Test` | Aprovado |
| 23 | acesso a documento restrito | `DocumentoComprobatorioServicoEtapa08Test` | Aprovado |
| 24 | prévia muda de 20 para 23 impactos | `AssinaturaPreviaNegocialTest` + revalidação na confirmação | Aprovado |
| 25 | perda atual da permissão da Lotação A | acesso atual + escopo histórico em frequência | Aprovado |
| 26 | retificação muda consolidação emitida | testes de relatório/invalidação da etapa11 | Aprovado |
| 27 | clique repetido em aditamento | identificação/versionamento único + constraint MySQL | Aprovado |
| 28 | FUTURO passa a VIGENTE por data | `ContratoTemporalidadeEtapa02Test` | Aprovado |
| 29 | vigência ultrapassada passa a ENCERRADO | `ContratoTemporalidadeEtapa02Test` | Aprovado |

## Comandos reproduzíveis

```bash
mvn clean test
node scripts/validar-nomenclatura.mjs
```

Para integração, use um MySQL 8.4 vazio e execute o Failsafe com `-DskipITs=false`, informando `INTEGRATION_MYSQL_URL`, `INTEGRATION_MYSQL_USER` e `INTEGRATION_MYSQL_PASSWORD`. A categoria de carga é separada para não ser disparada acidentalmente junto da suíte comum.
