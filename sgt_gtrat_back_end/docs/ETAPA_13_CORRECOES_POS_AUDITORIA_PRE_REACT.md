# Etapa 13 — correções pós-auditoria e contrato pré-React

Data da validação: 11/09/2026.

## Resultado

O backend permanece em **Java 17** e **Spring Boot 3.5.16**. Os 17 itens P0 do backlog da auditoria foram tratados e o projeto atende ao **Gate A para início do desenvolvimento funcional do React**. Esta conclusão não equivale à liberação de homologação ou produção: os P1 do Gate B continuam registrados como trabalho posterior.

Comando final executado:

```bash
mvn clean package
```

Resultado: **402 testes, 0 falhas, 0 erros, 0 ignorados e BUILD SUCCESS**. O artefato executável é `target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar`.

## Resposta aos P0 da auditoria

| Item | Situação | Correção aplicada |
|---|---|---|
| P0-01 | Concluído | Frequência pública usa `vinculoContratualId`; Pessoa/Vínculo/Alocação são resolvidos temporalmente e `Empregado` virou projeção interna automática. |
| P0-02 | Concluído | Inclusão calcula capacidade na data efetiva e, quando excedente, exige Gestor Local, confirmação, justificativa, evidência e autorização auditadas. |
| P0-03 | Concluído | Monitor informa excesso, `desdeQuandoExcedente` e justificativas relacionadas, aceitando a data de consulta. |
| P0-04 | Concluído | Aditamento/remanejamento abaixo da ocupação efetiva são bloqueados; confirmação não contorna a regra. |
| P0-05 | Concluído | Novo evento `CORRECAO_ANULACAO` e endpoint preservam a anulação original e a cancelam por precedência explícita. |
| P0-06 | Concluído | Ativação, inativação e reativação de Cargo, Lotação, Jornada e Tipo de Contratação são registradas temporalmente e consultadas na data do fato. |
| P0-07 | Concluído | Vínculo, Alocação, Aditamento e Remanejamento recusam escrita quando o contrato está administrativamente inativo. |
| P0-08 | Concluído | Transferência futura registra fotografia projetada e só altera a projeção corrente na data efetiva. |
| P0-09 | Concluído | Conflito em replay preserva a última verdade válida, produz diagnóstico e materializa pendência administrativa. |
| P0-10 | Concluído | Pessoa possui busca, filtros, paginação, ordenação fixa e projeção leve no banco. |
| P0-11 | Concluído | Vínculo aplica escopo organizacional, filtros e paginação na consulta JPA, sem `findAll()` em memória. |
| P0-12 | Concluído | Mutações de Pessoa, Vínculo, Pendência e Relatório invalidam o cache de negócio. |
| P0-13 | Concluído | Se 2FA estiver ativo, TOTP é obrigatório; o contrato `skipTwoFactor` foi removido. |
| P0-14 | Concluído | SPA obtém `XSRF-TOKEN`, devolve `X-XSRF-TOKEN` e requisições mutáveis sem token recebem 403. |
| P0-15 | Concluído | POST isolado de Pessoa foi removido; a criação operacional é integrada a Pessoa + Vínculo + Alocação. |
| P0-16 | Concluído | Presidência consulta relatórios nacionais, mas não recebe permissão de escrita/emissão formal. |
| P0-17 | Concluído | Alteração de Alocação no último dia exige confirmação explícita e registra a regra de precedência na trilha. |

Os cinco requisitos antes classificados como não atendidos — CON-020, CAT-007, CMP-006, CMP-009 e CMP-011 — possuem agora implementação correspondente.

## Pontos técnicos relevantes

- Locks pessimistas no Contrato e na Pessoa serializam a criação canônica concorrente.
- Contadores parciais de login não são apagados prematuramente pela limpeza agendada.
- Filtros JWT e rate-limit não são registrados duas vezes pelo contêiner Servlet.
- `Forwarded`/`X-Forwarded-*` são tratados por `server.forward-headers-strategy=framework`.
- A dependência direta redundante do Apache HttpClient foi removida.
- Nenhum `.env` real ou credencial foi incluído; somente `.env.example` com placeholders.

## Validação e limite desta etapa

O MySQL 8.4.11 anexado foi inicializado em base vazia durante a validação, mas o ambiente isolado impediu o servidor de abrir seu socket Unix (`Operation not permitted`). Por isso, esta entrega registra a integração MySQL como item do Gate B, sem alegar que ela foi executada aqui. O modelo Hibernate, as consultas, os contratos HTTP e o JAR foram compilados/testados pela suíte completa.

## Itens conscientemente fora do Gate A

Antes de homologação séria, concluir o Gate B da auditoria: rate-limit compartilhado entre instâncias, idempotência uniforme de comandos, migrations Flyway/baseline, perfil de produção fail-closed, proxy HTTPS/cookie Secure em infraestrutura real, concorrência real em MySQL e carga controlada.
