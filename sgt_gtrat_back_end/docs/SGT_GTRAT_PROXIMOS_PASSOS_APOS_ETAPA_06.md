# SGT-GTRAT — Próximos passos após a Etapa 06

Data-base: **09/09/2026**

## 1. Ação obrigatória antes da homologação

Executar a integração MySQL em ambiente com Docker:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco descartável e vazio, exclusivo desta versão. Não importar nem conciliar automaticamente a base de versões anteriores. Validar especialmente:

- criação das novas tabelas e índices;
- unicidade global de CPF;
- unicidade histórica contrato + matrícula;
- transação Pessoa + Vínculo + reserva + Alocação + Evento;
- rollback integral em conflito;
- consultas bitemporais;
- concorrência na reserva de matrícula e na criação de vínculos.

## 2. Etapa 07 — Alocação temporal

Objetivo: substituir a posição fixa do colaborador por períodos completos de `Lotação + Turno + Cargo` dentro do Vínculo.

Implementar:

1. transformar `AlocacaoVinculo` em agregado temporal versionado;
2. mudança de alocação sem criar novo vínculo ou desligamento;
3. retificação retroativa preservando fatos posteriores compatíveis;
4. no máximo uma alocação efetiva por pessoa/dia;
5. exatamente uma alocação em todo dia operacional do vínculo;
6. detecção de lacunas, sobreposições e colisão com desligamento na mesma data;
7. consulta mensal contendo todas as mudanças do período;
8. seleção encadeada Contrato → Lotação → Turno → Cargo baseada na composição temporal;
9. preservação da alocação durante inativação administrativa;
10. transferência excepcional de Gestão Local com origem, destino, data efetiva, motivo, autorização, auditoria e mapeamento obrigatório para composição válida.

Critérios de saída:

- testes unitários de borda para intervalos inclusivos;
- testes de integração MySQL para constraints e concorrência;
- frequência legada ainda compatível até a Etapa 09;
- nenhuma alteração silenciosa de vínculo ao mover a alocação.

## 3. Etapa 08 — Documentos e SEI

Implementar fontes comprobatórias:

- anexo;
- referência SEI;
- anexo + SEI.

Regras mínimas:

- contrato publicado e aditamento exigem fonte;
- remanejamento pode possuir fonte;
- arquivo publicado não é apagado;
- documento incorreto é invalidado ou substituído por nova versão;
- referência SEI incorreta é retificada sem apagar a anterior;
- um processo SEI pode relacionar vários contratos;
- documentos são classificados como `NEGOCIAL` ou `PESSOAL_RESTRITO`;
- todo acesso a documento pessoal/restrito é autorizado e auditado.

Critério de saída: cadeia documental reproduzível para cada fato negocial, sem exclusão física do histórico publicado.

## 4. Etapa 09 — Frequência integrada ao novo modelo

Migrar a frequência para resolver, na data do lançamento:

- contrato válido;
- vínculo válido;
- alocação válida;
- escopo histórico da Gestão Local.

Implementar:

- janela operacional de 7 dias corridos em `America/Sao_Paulo`;
- fechamento no início do 8º dia;
- correção do Gestor Local dentro ou fora da janela como retificação;
- bloqueio de nova edição operacional após retificação do Gestor;
- jornada noturna pertencente à data de início;
- tratamento de fatos retroativos com preservação do original, marcação de impacto e retificação quando aplicável;
- conclusão da repercussão de VIN-014 sobre frequência.

Ao concluir as Etapas 07–09, gerar o **ZIP Java C**, contendo Alocação, transferência de Gestão, documentos, SEI, frequência e testes de integração.

## 5. Etapa 10 — Perfis, permissões e minimização

Revisar integralmente:

- Administrador;
- Gestor Nacional;
- Gestor Estadual;
- Gestor Local;
- Presidente;
- Usuário Operacional.

O Gestor Local permanece como principal autor negocial. Administrador é perfil técnico e não deve virar responsável negocial automaticamente. Nacional, Estadual e Presidente são predominantemente consulta/auditoria, conforme escopo.

Criar permissões específicas para correções excepcionais, redução excepcional, transferência de Gestão e acesso pessoal restrito. Revisar todos os DTOs para minimização de dados por perfil, incluindo o contrato de apresentação do CPF para o futuro frontend.

## 6. Etapa 11 — Relatórios, indicadores, auditoria e pendências

Migrar relatórios e indicadores para a verdade temporal:

- versão vigente/retificada para relatório oficial;
- versões antigas sempre auditáveis;
- emissão com período, usuário, versão, pendências e natureza original/retificada;
- indicadores de previsto, presente, ausente, pendente e excedente;
- filtros por contrato, Lotação, Turno, Cargo, data e período;
- auditoria mostrando fato, data efetiva, conhecimento, autor, anterior, novo e precedência.

Criar Central de Pendências com `ABERTA`, `EM_TRATAMENTO` e `RESOLVIDA`, contexto/origem e bloqueio restrito ao escopo realmente afetado. Nesta etapa deve ser concluída a pendência automática do VIN-008 para vínculos abertos ao fim natural.

## 7. Etapa 12 — Fechamento do Java

Executar e ampliar:

- testes unitários JUnit/Mockito;
- testes JPA/MySQL/Testcontainers;
- transações e constraints;
- endpoints e segurança;
- ArchUnit;
- concorrência e idempotência;
- revalidação de prévias;
- carga de frequência, relatórios, indicadores e reconstrução histórica;
- todos os 20 cenários obrigatórios de borda do plano.

Também devem ser encerrados todos os requisitos marcados como parciais e emitida uma matriz global requisito → evidência.

Ao concluir, gerar o **ZIP Java Final**, com backend completo, configuração reproduzível, documentação e evidências. Somente depois congelar os contratos de API e iniciar a integração React.

## 8. Sequência de entregas

| Ordem | Entrega | Conteúdo |
|---:|---|---|
| 1 | Validação imediata | MySQL/Testcontainers da Etapa 06 |
| 2 | Etapa 07 | Alocação temporal e transferência de Gestão |
| 3 | Etapa 08 | Documentos e SEI |
| 4 | Etapa 09 | Frequência temporal |
| 5 | ZIP Java C | Checkpoint das Etapas 07–09 |
| 6 | Etapa 10 | Perfis, permissões e minimização |
| 7 | Etapa 11 | Relatórios, auditoria e pendências |
| 8 | Etapa 12 | Testes, carga e fechamento |
| 9 | ZIP Java Final | Backend Java completo |
| 10 | React | Implementação sobre APIs congeladas |

## 9. Regras que não devem ser revertidas

- manter Java 17 e Spring Boot 3;
- usar banco novo por versão durante esta evolução;
- não fundir Pessoas por CPF automaticamente;
- não reutilizar matrícula histórica no mesmo contrato;
- não reabrir vínculo automaticamente após prorrogação tardia;
- não transformar inativação administrativa em desligamento;
- não apagar fatos, documentos ou fotografias históricas;
- não remover `Empregado` até a migração dos consumidores nas Etapas 07 e 09 estar coberta por testes.
