# SGT/GTRAT — Etapa 05 concluída: Aditamentos, Redução e Remanejamentos

> **DOCUMENTO HISTÓRICO / SUPERADO:** a decisão de prontidão deste arquivo foi substituída por `SGT_GTRAT_CORRECAO_PRE_ETAPA_06_SPRING_BOOT3_JAVA17_REV3.md`. Use a Revisão 3 como checkpoint atual antes da Etapa 06.



**Data:** 08/09/2026  
**Base:** Backend Java Etapa 04 + Levantamento V8  
**Status:** CONCLUÍDA NO ESCOPO DA ETAPA 05

## 1. Objetivo

Implementar fatos negociais de Aditamento e Remanejamento sobre o motor temporal da Etapa 04, incluindo redução contratual normal/excepcional, retroatividade, versionamento, cancelamento de programação futura, precedência explícita e prévia de impacto.

## 2. O que foi implementado

### 2.1 Aditamento contratual

- entidade `AditamentoContratual` e itens `AditamentoComposicao`;
- identificação + versão do instrumento, com constraint única por contrato;
- data do documento, data de assinatura opcional, data efetiva, justificativa, usuário e data/hora de registro;
- referência SEI obrigatória nesta etapa como fonte comprobatória; anexos entram na Etapa 08;
- alterações de descrição, Tipo de Contratação, fornecedor/empresa, CNPJ, razão social, nome fantasia e vigência;
- alterações quantitativas por composição, com quantidade anterior, variação e quantidade resultante;
- aumento e redução;
- fato retroativo;
- fato programado para o futuro;
- retificação versionada sem exclusão física;
- cancelamento de aditamento futuro por relação `CANCELA`;
- preservação e reaplicação de fatos posteriores válidos;
- projeção escalar do contrato pela fotografia publicada + aditamentos válidos;
- fotografia versionada da posição projetada após cada alteração.

### 2.2 Regra de redução de 25%

- base fixa = `efetivoOriginalPublicado`;
- piso normal = `ceil(75% × efetivoOriginalPublicado)`;
- aumentos posteriores não alteram a base;
- aumento sem limite percentual artificial;
- redução abaixo do piso bloqueada no fluxo comum;
- composição individual pode reduzir mais de 25%, inclusive a zero, desde que o total respeite o piso e os impactos sejam confirmados;
- fluxo excepcional abaixo do piso exige fundamento jurídico/negocial, referência SEI, confirmação explícita e Gestor Local com `ROLE_REDUCAO_CONTRATUAL_EXCEPCIONAL`;
- cálculo isolado em `RegraEfetivoContratual`, sem dependência de Spring/JPA.

### 2.3 Remanejamento

- entidade `RemanejamentoContratual` e itens `RemanejamentoComposicao`;
- somente composições já existentes na data efetiva podem participar;
- pelo menos duas composições;
- cada variação deve ser diferente de zero;
- resultado de composição nunca pode ficar negativo;
- soma das variações deve ser exatamente zero;
- total do contrato antes/depois deve permanecer igual;
- retificação versionada;
- cancelamento de remanejamento futuro;
- referência SEI opcional;
- integração com o motor temporal e precedência explícita.

### 2.4 Projeção temporal das composições

- a tabela de composição publicada permanece fotografia-base;
- Aditamentos e Remanejamentos são aplicados como deltas na ordem temporal/negocial válida;
- fatos cancelados/retificados deixam de compor a visão vigente sem serem apagados;
- contratos publicados consultam composição projetada por data;
- composições de RASCUNHO continuam editáveis diretamente;
- após publicação, CRUD direto da fotografia original permanece bloqueado.

### 2.5 Precedência e conflitos

- Aditamento × Aditamento na mesma data/recorte exige precedência;
- Remanejamento × Remanejamento na mesma data/recorte exige precedência;
- Aditamento × Remanejamento na mesma composição/data também é detectado;
- `MANTEM_COEXISTE` não é aceito quando dois fatos realmente colidem;
- ID, timestamp e ordem de digitação não são usados como precedência negocial.

### 2.6 Prévia de impacto revalidada

- endpoints de prévia retornam `tokenPrevia`;
- token SHA-256 incorpora versão do contrato, comando, quantidades, ocupação e fatos temporais relevantes;
- confirmação reabre o contrato com lock pessimista, recalcula a prévia e compara a assinatura;
- alteração do impacto entre prévia e confirmação gera `409 Conflict` e exige nova prévia;
- `confirmarImpacto` não é mais suficiente sozinho.

### 2.7 Concorrência e duplicidade acidental

- `ContratoRepositorio.buscarPorIdParaAtualizacao` usa `PESSIMISTIC_WRITE` nas gravações críticas de Aditamento/Remanejamento;
- o lock serializa confirmações do mesmo contrato, mas não decide precedência negocial;
- constraints de identificação + versão protegem contra duplicidade persistente;
- token de prévia fica inválido depois de alteração concorrente ou repetição já aplicada;
- cadeia temporal continua validada pelo motor da Etapa 04.

### 2.8 Correção detectada durante a revisão integral

- a publicação passou a exigir que cada composição da fotografia original cubra integralmente a vigência originalmente publicada;
- isto impede um contrato publicado de possuir períodos internos sem composição coerente escondidos no CRUD;
- alterações posteriores de composição/vigência passam exclusivamente por fatos versionados;
- remanejamento é validado contra a vigência reconstruída;
- a data efetiva do aditamento deve estar contida na vigência resultante do próprio aditamento.

## 3. Arquivos principais adicionados

- `controlador/AditamentoContratualControlador.java`;
- `controlador/RemanejamentoContratualControlador.java`;
- `entidade/AditamentoContratual.java`;
- `entidade/AditamentoComposicao.java`;
- `entidade/RemanejamentoContratual.java`;
- `entidade/RemanejamentoComposicao.java`;
- `servico/AditamentoContratualServico.java`;
- `servico/RemanejamentoContratualServico.java`;
- `servico/ComposicaoContratualTemporalServico.java`;
- `regra/RegraEfetivoContratual.java`;
- `regra/AssinaturaPreviaNegocial.java`;
- DTOs de comando, prévia, impacto e resposta;
- `repositorio/AditamentoContratualRepositorio.java`;
- `repositorio/RemanejamentoContratualRepositorio.java`;

## 4. Arquivos principais alterados

- `ConstantesDominio.java`;
- `EventoCicloVidaContrato.java`;
- `TipoEventoCicloVidaContrato.java`;
- `ContratoRepositorio.java`;
- `ComposicaoContratualServico.java`;
- `ContratoServico.java`;
- `MotorTemporalContratoServico.java`;

## 5. Validações executadas

- Java disponível: **21.0.11**;
- `scripts/validar-backend.sh preflight`: **OK**;
- compilação `javac` dos DTOs/regras da Etapa 05: **OK**;
- compilação `javac` das quatro novas entidades com stubs das anotações/dependências: **OK**;
- compilação `javac` dos repositories alterados/novos com stubs das APIs Spring Data: **OK**;
- smoke test puro `RegraEfetivoContratual + AssinaturaPreviaNegocial`: **SMOKE_ETAPA05_OK**;
- cenários: 100→75; 101→76; 100→74 excepcional; remanejamento -10+4+6; rejeição -10+4+5; alteração de impacto invalida token.

### Limitação do Maven

O Maven Wrapper foi tentado novamente, porém o ambiente atual continua sem resolução DNS para `repo.maven.apache.org`. A falha ocorre antes da compilação/download das dependências. Portanto não há declaração falsa de `mvn test` completo aprovado neste ambiente.

## 6. Resultado da revisão integral do levantamento V8

Foram auditados **198 requisitos canônicos**. Situação após a Etapa 05:

- **47 implementados**;
- **50 parciais**;
- **101 pendentes**.

A matriz completa está no arquivo `SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_05.md` e também será incluída dentro do ZIP em `docs/`.

A Etapa 05 fecha seu escopo, mas **não significa que o V8 inteiro esteja pronto**. Pessoa/Vínculo/Alocação ainda usam estruturas legadas e não são considerados aderentes; frequência e relatórios também ainda precisam ser integrados à nova verdade temporal.

## 7. O que será feito na Etapa 06

### Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento

- separar identidade permanente da Pessoa do Vínculo Contratual;
- mover matrícula para o Vínculo;
- validar CPF global e preparar correções excepcionais auditadas;
- eliminar sobreposição global de vínculos;
- permitir vínculo futuro `PROGRAMADO` sem ativação antecipada;
- cancelar/retificar programação preservando cadeia;
- criar motivos de desligamento coerentes;
- desligamento como último dia do vínculo;
- desligamento retroativo/futuro/versionado;
- integrar fim natural, encerramento antecipado e anulação ao Vínculo;
- reavaliar ocupação/excedente usando Vínculo temporal, substituindo contagens baseadas no estado atual de `Empregado`;
- preparar a operação transacional Pessoa + Vínculo para a Alocação da Etapa 07.

### Requisitos prioritários da Etapa 06

`COL-*`, `MAT-*`, `VIN-*`, `DES-*`, além das partes ainda pendentes de `TMP`, `CON`, `CMP`, `AUD` e `PER` relacionadas a pessoas/vínculos.

## 8. Critério de saída da Etapa 06

Ao final, o backend deverá conseguir responder, para uma data, **qual Pessoa estava vinculada a qual Contrato, com qual matrícula e em qual estado temporal**, sem depender do campo de “contrato atual” do `Empregado`. A Alocação Lotação + Turno + Cargo será fechada na Etapa 07.

**Próxima etapa recomendada:** Etapa 06.
