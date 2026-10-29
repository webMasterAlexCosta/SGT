# SGT/GTRAT — Entrega final da Etapa 11

**Data-base:** 10/09/2026

## Situação

**Etapa 11 implementada e aprovada pela suíte automatizada, com homologação nativa MySQL preparada para a Etapa 12.**

## Escopo entregue

- Central de Pendências com ciclo `ABERTA → EM_TRATAMENTO → RESOLVIDA`;
- geração idempotente por retificação de frequência e fim natural de contrato;
- tratamento restrito ao Gestor Local responsável e resolução condicionada à origem;
- aviso e contagem `sujeito a revisão` em tela, PDF e XLSX;
- emissão administrativa formal, imutável, idempotente e versionada;
- emissão original, retificada, vigente e superada;
- snapshot histórico GZIP com SHA-256;
- visão efetiva atual, consulta conhecida até um instante e comparação histórica;
- invalidação seletiva após retificação de frequência ou vínculo;
- auditoria, isolamento organizacional e minimização de dados preservados.

## Requisitos promovidos

`VIN-008`, `VIN-014`, `FRE-011`, `FRE-012`, `REL-001` a `REL-006` e `AUD-006`.

Matriz global após a entrega:

| Situação | Quantidade |
|---|---:|
| Implementado | 144 |
| Parcial | 44 |
| Pendente | 10 |
| **Total** | **198** |

## Qualidade

- 563 fontes principais e 120 fontes de teste compiladas com Java 17;
- 383 testes executados;
- 0 falhas, 0 erros e 0 ignorados;
- 8 testes novos específicos da Etapa 11;
- artefato Maven gerável sem baixar dependências;
- pacote final com manifesto SHA-256 e sem `target`, banco, segredos ou binários auxiliares.

## Ressalva explícita

O executor bloqueia a abertura de sockets Unix/TCP (`Operation not permitted`). Embora o MySQL 8.4.11 tenha sido inicializado em diretório novo, a integração nativa específica da Etapa 11 não pôde ser executada. O teste `Etapa11PersistenceIT` está pronto e sua execução é o primeiro gate da Etapa 12. A presente entrega não declara homologação MySQL que não ocorreu.

## Decisão para continuidade

O backend está apto a seguir para a Etapa 12. O React deve começar somente depois da homologação final, revisão dos 198 requisitos e congelamento dos contratos HTTP.
