# SGT/GTRAT — Auditoria final da evolução do backend

**Data:** 08/09/2026  
**Comparação:** Java original antes da Etapa 01 × backend corrigido da Etapa 05  
**Escopo:** aderência ao Levantamento V8, evolução técnica, regressões pré-Etapa 06 e condição para continuidade.

## Conclusão executiva

O sistema evoluiu de forma real e verificável. A última versão não é apenas uma reorganização documental: ela removeu o fluxo legado de reconciliação automática, migrou para Spring Boot 3.5.16/Java 17 e acrescentou o núcleo temporal de Contrato, Composição, Aditamento, Redução e Remanejamento.

O núcleo corrigido das Etapas 01–05 foi validado nesta auditoria. As regressões direcionadas T01–T28 passaram e a suíte Maven passou com **292 testes, 0 falhas, 0 erros e 0 ignorados**.

Isso não significa que os 198 requisitos do V8 estejam concluídos. Pessoa/Vínculo/Alocação, frequência temporal completa, documentos, permissões completas, relatórios temporais, Central de Pendências e integração MySQL ainda pertencem às etapas seguintes.

## Comparação objetiva

| Aspecto | Original antes da Etapa 01 | Última versão auditada |
|---|---|---|
| Spring Boot | 4.1.0 | 3.5.16 |
| Java de compilação | 21 | 17 (`release 17`) |
| Build | Maven | Maven 3.9.16 |
| Temporalidade contratual | núcleo anterior | motor temporal com versões, retificações, precedência e data de conhecimento |
| Aditamento | inexistente no núcleo original | entidade, itens, prévia, token, lock e versionamento |
| Remanejamento | inexistente no núcleo original | entidade, itens, soma zero, prévia, lock e versionamento |
| Redução contratual | sem a regra final validada | piso de 75% sobre o efetivo original, com exceção formal |
| Reconciliação automática de legado | presente | removida do fluxo de inicialização |
| Catálogos | estrutura anterior | Cargo/Lotação/Turno com inativação e validações de Gestão Local |
| Evidência de teste | validação incompleta | 292 testes Maven + T01–T28 direcionados |

### O que foi removido

Foram retirados da última versão os arquivos responsáveis pela reconciliação automática de vínculo legado no startup:

- `configuracao/InicializadorContratoUnicoEmpregado.java`;
- `servico/EmpregadoContratoUnicoServico.java`;
- `scripts/migrar_para_gtrat.sql`.

Essa remoção é coerente com a decisão de banco de desenvolvimento descartável e evita que a inicialização crie, encerre ou reescreva fatos negociais sem uma operação explícita.

## Correções pré-Etapa 06 verificadas

| Bloqueio | Verificação | Resultado |
|---|---|---|
| Cadeia de retificação/substituição | T01–T03 | Passou |
| Remanejamento depois de retroatividade | T04 | Passou; total reconstruído preservado |
| Conflito Aditamento × Remanejamento | T05–T08 | Passou nas duas ordens |
| Piso de redução | T09–T12 | Passou |
| Soma zero do remanejamento e hash da prévia | T13–T15 | Passou |
| Contrato futuro cancelado | T16 | Passou; não reaparece como vigente |
| Vigência após encerramento antecipado | T17–T18 | Passou |
| Publicação integral das composições | T19–T21 | Passou |
| Motivo de desligamento | T22–T24 | Passou |
| Cargo, Lotação e Turno | T25–T28 | Passou |

O checkpoint estático também passou em **42 verificações**, incluindo Boot 3, Java 17, Testcontainers 1.x, regra ArchUnit, ausência de `List.getFirst()/getLast()` e remoção do legado de startup.

## Situação dos requisitos V8

A matriz de 198 requisitos recebida continua sendo a referência de rastreabilidade das etapas. A fotografia documental após a Etapa 05 registra:

| Situação registrada na matriz | Quantidade |
|---|---:|
| Implementado | 47 |
| Parcial | 50 |
| Pendente | 101 |
| Total | 198 |

Esses números não devem ser lidos como percentual de homologação. As correções R01–R09 e os testes desta auditoria comprovam o gate do núcleo contratual, mas não reclassificam automaticamente todos os requisitos dependentes de Pessoa, Vínculo, Alocação, Frequência e Relatórios.

### Ainda não concluído

- **Pessoa/Colaborador:** o modelo ainda possui `Empregado` como estrutura legada; a identidade permanente da Pessoa ainda não foi separada integralmente.
- **Vínculo/Matrícula/Desligamento:** ainda não há o novo ciclo temporal completo previsto para a Etapa 06.
- **Alocação:** Lotação + Turno + Cargo ainda não foi fechada como entidade temporal do Vínculo.
- **Frequência:** permanecem regras legadas de janela e retroatividade; a migração para Vínculo/Alocação históricos é da Etapa 09.
- **Documentos/SEI:** anexos, classificação Pessoal/Restrito e versionamento documental ainda não foram integrados.
- **Permissões:** a matriz excepcional e a minimização de dados ainda precisam do fechamento da Etapa 10.
- **Relatórios/Auditoria:** relatórios legados ainda não consomem integralmente a nova verdade temporal; a Central de Pendências é da Etapa 11.
- **MySQL/Testcontainers:** não foi possível executar a integração porque este ambiente não possui Docker nem `/var/run/docker.sock`.

## Decisão

O backend está **apto para iniciar a Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento**, sem iniciar ainda a implementação React.

O sistema **não está pronto para homologação final**. A liberação da Etapa 06 não substitui a integração MySQL, a implementação das etapas 07–12 nem o aceite funcional completo do V8.

