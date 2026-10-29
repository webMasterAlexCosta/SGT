# SGT-GTRAT — Auditoria consolidada do original até a Etapa 08

**Data da auditoria:** 09/09/2026  
**Versão auditada:** backend ao final da Etapa 08  
**Plataforma obrigatória confirmada:** Spring Boot 3.5.16 e Java 17

## 1. Conclusão executiva

O sistema evoluiu de forma real, cumulativa e verificável desde o Java original até a Etapa 08. A versão atual preserva as entregas anteriores, compila e empacota corretamente com Spring Boot 3.5.16/Java 17, passa em **342 testes Maven** e em todos os checkpoints direcionados **T01–T90**.

**Decisão para a próxima fase: APTO PARA INICIAR A ETAPA 09, COM RESSALVAS PLANEJADAS.**

Isso não significa que os 198 requisitos finais estejam concluídos. Os requisitos previstos para as Etapas 09–12 permanecem abertos, principalmente frequência, revisão global de permissões, relatórios temporais, Central de Pendências e homologação final em MySQL. As dependências parciais já conhecidas — VIN-008, VIN-014, ALO-006 e PER-012 — continuam corretamente rastreadas para essas etapas.

Não foi encontrada regressão inesperada nas entregas das Etapas 01–08.

## 2. Escopo e método

A verificação considerou:

- o levantamento canônico com 198 requisitos;
- o plano de implementação das Etapas 01–12;
- a auditoria preservada que comparou diretamente o Java original com a Etapa 05;
- as matrizes de aderência das Etapas 05, 06, 07 e 08;
- comparação cumulativa dos fontes das Etapas 05, 06, 07 e 08;
- inspeção do código e das configurações da Etapa 08;
- regressão Maven completa na versão atual;
- execução dos checkpoints T01–T90;
- geração e inspeção do JAR executável.

### Limitação sobre o ZIP original

A cópia transitória do ZIP original enviada anteriormente não está mais disponível no diretório de trabalho desta sessão. Por isso, esta auditoria não repetiu uma segunda extração byte a byte do original. Para esse trecho da evolução foi usada a auditoria preservada `SGT_GTRAT_AUDITORIA_FINAL_EVOLUCAO_2026-09-08.md`, que registrou a comparação direta original × Etapa 05 e suas evidências.

As Etapas 05–08 foram novamente verificadas nos fontes disponíveis. Caso seja exigida uma nova comparação independente byte a byte desde o ZIP original, será necessário reenviar somente esse ZIP. Essa limitação documental não bloqueia a Etapa 09, pois os checkpoints T01–T28 das entregas antigas também foram reexecutados e passaram na versão da Etapa 08.

## 3. Continuidade tecnológica

| Verificação | Resultado |
|---|---|
| Java original | Spring Boot 4.1.0 / Java 21, conforme auditoria direta preservada |
| Etapa 05 | Migração confirmada para Spring Boot 3.5.16 / Java 17 |
| Etapa 08 — `pom.xml` | `spring-boot-starter-parent` 3.5.16 e `java.version` 17 |
| Compilação atual | `javac` com `release 17` |
| Manifesto do JAR | `Spring-Boot-Version: 3.5.16` e `Build-Jdk-Spec: 17` |
| Configurações ativas | Nenhuma referência ativa a Spring Boot 4 ou Java 21 |
| Empacotamento | JAR Spring Boot executável gerado com sucesso |

A decisão de permanecer em Spring Boot 3 e Java 17 está efetivamente aplicada, e não apenas documentada.

## 4. Evolução cumulativa verificada

| Comparação | Arquivos adicionados | Alterados | Removidos |
|---|---:|---:|---:|
| Etapa 05 → Etapa 06 | 50 | 14 | 0 |
| Etapa 06 → Etapa 07 | 34 | 9 | 0 |
| Etapa 07 → Etapa 08 | 34 | 8 | 0 |

O código principal Java cresceu de **419 arquivos na Etapa 05** para **505 arquivos na Etapa 08**. A suíte cresceu de **101 para 111 fontes de teste**. Não houve exclusão de arquivos entre as Etapas 05 e 08.

Principais incrementos confirmados:

- Etapa 06: Pessoa/Colaborador, Vínculo, reserva de matrícula, desligamento, eventos e cadeia de integridade;
- Etapa 07: Alocação temporal, transferência excepcional, mapeamentos e projeção contratual;
- Etapa 08: documentos versionados, SEI, anexos, classificação restrita, auditoria de acesso e armazenamento seguro.

## 5. Resultado por etapa

| Etapa | Recorte principal | Resultado da auditoria |
|---|---|---|
| 01–05 | Diagnóstico; Contrato; composição; temporalidade; aditamento; redução; remanejamento | Núcleo preservado; T01–T28 passaram novamente |
| 06 | COL, MAT, VIN e DES | 36 de 38 implementados; VIN-008 e VIN-014 parciais por dependências futuras |
| 07 | Alocação e transferência | 12 de 15 implementados; ALO-006, VIN-014 e PER-012 parciais por dependências futuras |
| 08 | DOC-001–DOC-010 | 10 de 10 implementados |

### Pendências parciais legítimas

| Requisito | Estado atual | Fechamento previsto |
|---|---|---|
| VIN-008 | O encerramento operacional existe; falta gerar automaticamente item na Central de Pendências | Etapa 11 |
| VIN-014 | Retificação e revalidação temporal existem; falta repercussão completa em frequência e relatórios | Etapas 09 e 11 |
| ALO-006 | O motor temporal identifica a alocação válida; frequência ainda não o consome integralmente | Etapa 09 |
| PER-012 | O fluxo excepcional valida origem e autorização; falta administração granular global | Etapa 10 |

Esses itens não representam regressão da Etapa 08. São integrações explicitamente reservadas aos módulos consumidores posteriores.

## 6. Matriz global dos 198 requisitos

A tabela abaixo é uma **consolidação formal conservadora**: parte da matriz global da Etapa 05 e sobrepõe apenas as reclassificações explicitamente comprovadas pelas matrizes das Etapas 06–08. Ela evita declarar como concluído qualquer requisito transversal sem uma revalidação formal específica.

| Grupo | Total | Implementado | Parcial | Pendente |
|---|---:|---:|---:|---:|
| GOV — governança | 2 | 1 | 1 | 0 |
| TMP — temporalidade | 17 | 4 | 10 | 3 |
| CON — contratos | 25 | 15 | 8 | 2 |
| EMP — empregado legado | 3 | 2 | 1 | 0 |
| CAT — catálogos | 9 | 0 | 4 | 5 |
| CMP — composição | 11 | 4 | 7 | 0 |
| RED — redução | 8 | 6 | 2 | 0 |
| ADI — aditamento | 10 | 9 | 1 | 0 |
| REM — remanejamento | 7 | 6 | 1 | 0 |
| DOC — documentos e SEI | 10 | 10 | 0 | 0 |
| COL — pessoa/colaborador | 12 | 12 | 0 | 0 |
| MAT — matrícula | 5 | 5 | 0 | 0 |
| VIN — vínculo | 15 | 13 | 2 | 0 |
| ALO — alocação | 8 | 7 | 1 | 0 |
| DES — desligamento | 6 | 6 | 0 | 0 |
| FRE — frequência | 19 | 0 | 0 | 19 |
| REL — relatórios | 6 | 0 | 0 | 6 |
| AUD — auditoria/pendências | 7 | 0 | 6 | 1 |
| PER — permissões | 12 | 1 | 3 | 8 |
| HIS — histórico | 6 | 3 | 3 | 0 |
| **Total** | **198** | **104** | **50** | **44** |

Essa contagem é um piso documental, não uma alegação de que somente 104 comportamentos existam no código. Há melhorias transversais já presentes — por exemplo, integração documental e reforços temporais — que não foram promovidas de status sem matriz formal própria. O número correto para decisão de avanço é o cumprimento do recorte de cada etapa e suas dependências declaradas.

## 7. Evidências de teste e construção

### Suíte Maven

| Métrica | Resultado |
|---|---:|
| Fontes Java principais compilados | 505 |
| Fontes de teste compilados | 111 |
| Testes executados | 342 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |
| Resultado | BUILD SUCCESS |

### Checkpoints direcionados

| Faixa | Escopo | Resultado |
|---|---|---|
| T01–T28 | Regressões temporais, aditamento, remanejamento e catálogos das Etapas 01–05 | 28/28 aprovados |
| T29–T50 | Pessoa, Vínculo, Matrícula, Desligamento e integridade da Etapa 06 | 22/22 aprovados |
| T51–T70 | Alocação temporal e transferência da Etapa 07 | 20/20 aprovados |
| T71–T90 | Documentos, SEI, acesso restrito e armazenamento da Etapa 08 | 20/20 aprovados |
| **Total** |  | **90/90 aprovados** |

O empacotamento adicional com testes já executados também terminou com sucesso e gerou o JAR executável da aplicação.

## 8. Lacunas confirmadas antes da Etapa 09

O módulo de frequência ainda contém regras legadas e, portanto, não deve ser considerado aderente ao levantamento final:

- configuração padrão de retroatividade do usuário ainda está em 30 dias;
- a entidade de frequência ainda calcula prazo de edição com a constante legada de 8 horas;
- FRE-001–FRE-019 permanecem no escopo da Etapa 09;
- ALO-006 ainda precisa ser consumido pelo cálculo de frequência;
- a parte de frequência de VIN-014 ainda precisa de reprocessamento temporal.

Essa constatação confirma que a próxima etapa correta é a **Etapa 09 — Frequência temporal**, sem antecipar conclusão global do sistema.

## 9. Itens posteriores que não podem ser declarados concluídos

| Etapa futura | Itens principais |
|---|---|
| 09 | FRE-001–FRE-019; consumo da Alocação temporal; repercussão de VIN-014 em frequência |
| 10 | Revisão e minimização global de PER-001–PER-012; administração granular de exceções |
| 11 | REL-001–REL-006; Central de Pendências; fechamento de VIN-008; repercussões de relatórios e auditoria |
| 12 | Testes finais de carga/concorrência; integração e homologação com MySQL; matriz final de aderência |

O ambiente atual não possui Docker disponível. Assim, a regressão Java e os checkpoints foram executados, mas a homologação com MySQL/Testcontainers não foi repetida nesta auditoria. Esse teste deve permanecer como critério obrigatório da Etapa 12 ou ser antecipado quando houver infraestrutura disponível.

## 10. Critérios de entrada aprovados para a Etapa 09

- Spring Boot 3.5.16 e Java 17 confirmados no build e no JAR;
- T01–T90 aprovados;
- 342 testes sem falhas ou erros;
- Etapas 06 e 07 preservadas com dependências explícitas;
- DOC-001–DOC-010 concluídos na Etapa 08;
- ausência de regressão estrutural entre Etapas 05–08;
- lacunas de frequência identificadas e alinhadas ao escopo da Etapa 09.

## 11. Decisão final

**A evolução até a Etapa 08 atende ao plano de implementação e está tecnicamente apta a prosseguir para a Etapa 09.**

O avanço deve manter os seguintes controles:

1. não alterar Spring Boot 3.5.16 nem Java 17;
2. preservar T01–T90 e os 342 testes atuais como regressão mínima;
3. implementar FRE-001–FRE-019 sem usar a entidade legada como prova de aderência;
4. fechar ALO-006 e a parcela de frequência de VIN-014;
5. atualizar a matriz global após a Etapa 09, sem promover requisitos de permissões, relatórios ou pendências antes de suas etapas;
6. manter banco vazio por versão e não reintroduzir reconciliação automática de legado.

