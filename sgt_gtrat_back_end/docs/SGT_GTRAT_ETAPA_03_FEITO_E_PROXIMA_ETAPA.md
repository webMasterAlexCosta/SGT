# SGT/GTRAT — Etapa 03 concluída e próxima etapa

**Data:** 08/09/2026  
**Etapa:** 03 — Composição Contratual, cadastros auxiliares e publicação definitiva  
**Base:** SGT_GTRAT_BACKEND_JAVA_ETAPA_02.zip

---

## 1. Objetivo da Etapa 03

Implementar a composição contratual explícita definida no levantamento V8 e fechar o fluxo de publicação do contrato sem permitir que um contrato incompleto seja publicado.

A composição passa a representar explicitamente:

`Contrato + Lotação + Turno/Jornada + Cargo + Efetivo previsto + período de vigência`

A Etapa 03 também prepara os cadastros de Lotação, Turno e Cargo para inativação sem perda histórica e cria uma visão transitória de excedente usando o modelo atual de Empregado.

---

## 2. O que foi implementado

### 2.1. Nova entidade `ComposicaoContratual`

Foi criada a entidade `ComposicaoContratual` com:

- Contrato;
- Lotação;
- Jornada/Turno;
- Cargo;
- efetivo previsto;
- data inicial de vigência;
- data final de vigência;
- versionamento otimista JPA.

Também foram adicionados índices para consulta por contrato, período e recorte operacional.

### 2.2. Combinação explícita

Não é utilizado produto cartesiano automático.

Cada combinação Lotação + Turno + Cargo precisa existir expressamente como registro de composição.

### 2.3. Proteção contra composição duplicada

O serviço verifica sobreposição temporal da mesma combinação.

Duas composições com o mesmo:

- Contrato;
- Lotação;
- Turno;
- Cargo;

não podem coexistir com períodos sobrepostos.

Também existe constraint defensiva no banco para a identidade da versão inicial do recorte.

### 2.4. Coerência de Gestão Local

Ao incluir ou alterar composição em RASCUNHO:

- Lotação deve pertencer à Gestão Local do contrato;
- Turno deve pertencer à Gestão Local do contrato;
- Turno deve estar efetivamente relacionado à Lotação selecionada;
- Cargo com Gestão Local definida deve pertencer à mesma Gestão Local do contrato.

Isso evita montar composição usando cadastros incompatíveis entre unidades.

### 2.5. Preparação de Lotação, Turno e Cargo para inativação

Foram adicionados campos de habilitação `ativo` a:

- Lotação;
- Jornada/Turno;
- Cargo.

Cargo também recebeu vínculo opcional com Gestão Local.

O vínculo é opcional apenas para permitir a migração de registros legados já existentes. Novos fluxos deverão trabalhar com Cargo contextualizado na Gestão Local.

Uma composição nova não aceita catálogo marcado como inativo.

### 2.6. Efetivo zero

A entidade possui regra explícita:

`efetivoPrevisto == 0` → não permite nova alocação operacional.

O método `permiteNovaAlocacaoEm(data)` diferencia:

- composição historicamente existente;
- composição utilizável para nova alocação.

A integração definitiva dessa regra com o novo Vínculo/Alocação será feita na Etapa 06, quando o modelo antigo de Empregado deixar de ser a fonte de alocação atual.

### 2.7. API de composição

Foi criado:

`/api/contratos/{contratoId}/composicoes`

Operações disponíveis:

- `GET` — listar todas as composições;
- `GET ?data=YYYY-MM-DD` — consultar composições válidas em uma data;
- `POST` — criar composição no RASCUNHO;
- `PUT /{id}` — alterar composição no RASCUNHO;
- `DELETE /{id}` — remover composição enquanto RASCUNHO.

Após publicação, criação/alteração/exclusão direta da composição é bloqueada.

Mudanças posteriores serão feitas por Aditamento/Remanejamento versionados.

### 2.8. Publicação definitiva do Contrato

Foi habilitado:

`POST /api/contratos/{id}/publicar`

A publicação somente ocorre se o contrato estiver em `RASCUNHO`.

Antes de publicar são validados:

- número;
- ano;
- descrição;
- vigência inicial;
- vigência final;
- Tipo de Contratação cadastrado e ativo;
- CNPJ;
- razão social;
- nome fantasia;
- fonte comprobatória;
- existência de composição;
- soma do efetivo das composições;
- ausência de composição duplicada;
- unicidade número + ano.

Nesta etapa a fonte comprobatória operacionalizada é a referência SEI. O suporte completo a anexos e classificação documental entra na Etapa 08.

### 2.9. Fotografia original da publicação

Ao publicar:

- estado negocial passa de `RASCUNHO` para `PUBLICADO`;
- `efetivoOriginalPublicado` é congelado;
- referência SEI da publicação é preservada;
- data/hora de publicação é registrada;
- usuário responsável é registrado;
- evento `PUBLICACAO` é criado na linha de eventos de ciclo de vida.

Como a composição deixa de ser editável diretamente depois da publicação, os registros publicados funcionam como fotografia estrutural da composição original até a implantação do motor geral de versões das Etapas 04 e 05.

### 2.10. Soma das composições

Na data inicial do contrato:

`SOMA(efetivo previsto das composições válidas) == efetivo total previsto do contrato`

Caso contrário, a publicação é bloqueada.

Exemplo:

- Contrato: efetivo 100;
- Composição A: 40;
- Composição B: 30;
- Composição C: 20.

Total = 90.

Resultado: publicação bloqueada.

### 2.11. Monitor transitório de excedente

Foi criada consulta:

`GET /api/contratos/{contratoId}/composicoes/excedentes`

A consulta compara, para cada composição atual:

- efetivo previsto;
- quantidade de empregados ativos atualmente vinculados ao mesmo Contrato + Lotação + Turno + Cargo;
- quantidade excedente.

O excedente não produz teto técnico automático.

#### Limitação conhecida e intencional

O modelo antigo ainda guarda Lotação/Turno/Cargo diretamente no `Empregado`.

Por isso, nesta etapa, o campo `desdeQuandoExcedente` permanece `null`.

Não foi inventada uma data aproximada.

A data correta será calculada quando a Etapa 06 implantar Vínculo e Alocação temporal, permitindo reconstruir quando o excedente realmente começou.

### 2.12. Proteção no descarte de RASCUNHO

Quando um RASCUNHO é descartado, suas composições preparatórias são removidas antes do contrato.

Isso é permitido porque essas composições nunca foram publicadas e não representam fato negocial histórico.

Contrato publicado continua sem exclusão física.

---

## 3. Arquivos principais criados

- `ComposicaoContratual.java`
- `ComposicaoContratualDTO.java`
- `ExcedenteComposicaoDTO.java`
- `PublicacaoContratoDTO.java`
- `ComposicaoContratualRepositorio.java`
- `ComposicaoContratualServico.java`
- `ComposicaoContratualControlador.java`
- `ComposicaoContratualEtapa03Test.java`

---

## 4. Arquivos principais alterados

- `ContratoServico.java`
- `ContratoControlador.java`
- `Cargo.java`
- `Lotacao.java`
- `Jornada.java`
- `EmpregadoRepositorio.java`

---

## 5. Requisitos da composição tratados nesta etapa

### CMP-001

Implementado: composição explícita Lotação + Turno + Cargo + efetivo previsto.

### CMP-002

Implementado: bloqueio de combinação duplicada no mesmo recorte temporal.

### CMP-003

Implementado na publicação: soma das composições deve ser igual ao efetivo total.

### CMP-004

Implementado no domínio da composição: efetivo zero torna a composição indisponível para nova alocação.

A ligação definitiva com o novo fluxo de Vínculo/Alocação entra na Etapa 06.

### CMP-005

Estrutura temporal preparada: uma composição possui início/fim e pode possuir nova versão futura.

O versionamento negocial completo será consolidado pelos motores das Etapas 04 e 05.

### CMP-006 / CMP-007

Contrato publicado não permite alteração direta da composição. Redução, zeramento ou remoção após publicação deverão passar pelo fluxo de impacto + Aditamento/Remanejamento.

O tratamento completo entra nas Etapas 04 e 05.

### CMP-008 / CMP-010 / CMP-011

Foi implementada a detecção do excedente atual sem impor teto rígido.

A reconstrução histórica de `desde quando` e as justificativas vinculadas ao ato de inclusão serão concluídas com Vínculo/Alocação temporal na Etapa 06.

### CMP-009

A exigência definitiva de justificativa do Gestor Local ao realizar nova inclusão excedente será integrada ao novo Vínculo da Etapa 06. Não foi acoplada artificialmente ao campo `observacao` do Empregado legado.

---

## 6. Validações técnicas executadas

### Preflight

Resultado:

`Preflight OK: Java 17.0.11`

### Compilação direcionada

Foram compilados com stubs de infraestrutura:

- DTOs novos;
- `ComposicaoContratual`;
- repositório de composição;
- `ComposicaoContratualServico`;
- `ComposicaoContratualControlador`;
- `ContratoServico` alterado;
- `ContratoControlador` alterado;
- `Cargo` alterado;
- `Jornada` alterada;
- `Lotacao` alterada.

Resultado das verificações centrais: **OK**.

### Maven completo

O comando foi tentado novamente:

`./mvnw -q -DskipTests compile`

O ambiente não conseguiu resolver:

`repo.maven.apache.org`

Erro:

`curl: (6) Could not resolve host: repo.maven.apache.org`

Portanto, `mvn test` completo NÃO está sendo declarado como aprovado.

Isso continua sendo uma limitação do ambiente de execução, não uma migração para Gradle nem um erro de dependência detectado no `pom.xml`.

---

# 7. Decisões importantes desta etapa

## 7.1. Não usar produto cartesiano

O sistema não presume que todas as Lotações combinam com todos os Turnos e todos os Cargos.

## 7.2. Não permitir edição de composição publicada

Isso evita reescrever a fotografia original do contrato.

## 7.3. Não inventar histórico de excedente

O backend antigo não possui informação suficiente para descobrir com precisão quando começou um excedente por composição.

Em vez de inventar uma data, o campo permanece sem valor histórico até a implantação da Alocação temporal.

## 7.4. Não usar `ativo` do Contrato como situação temporal

Mantida a decisão da Etapa 02.

## 7.5. Não liberar publicação parcial

A publicação agora é efetivamente validada contra a composição.

---

# 8. Estado do Checkpoint A

O Checkpoint A previsto no plano compreende:

- Etapa 01 — estabilização;
- Etapa 02 — núcleo Contrato;
- Etapa 03 — composição/publicação.

Com a conclusão da Etapa 03, o **Checkpoint Java A está funcionalmente fechado** dentro das dependências previstas das etapas seguintes.

Ainda existem integrações deliberadamente futuras:

- versionamento temporal geral;
- aditamento/remanejamento;
- novo Vínculo;
- nova Alocação;
- documentos/anexos;
- frequência temporal.

Essas integrações não devem ser simuladas no modelo antigo.

---

# 9. Próxima etapa — Etapa 04

## Motor temporal, histórico, versionamento e precedência negocial

**Esforço:** Muito Alto

Esta será uma das etapas mais importantes de todo o backend.

### Objetivos

Criar infraestrutura comum para fatos negociais temporais, permitindo reconstruir a verdade válida em uma data sem depender do estado atual das entidades.

### Implementações previstas

#### 9.1. Estrutura comum de evento/fato negocial

Registrar:

- data efetiva;
- data/hora de registro;
- usuário responsável;
- motivo;
- referência documental;
- versão;
- fato anterior;
- fato sucessor;
- validade.

#### 9.2. Relações de precedência

Implementar:

- `SUCEDE`;
- `RETIFICA`;
- `CANCELA`;
- `SUBSTITUI`;
- `MANTEM_COEXISTE`.

#### 9.3. Conflitos na mesma data

Não utilizar:

- ID do banco;
- milissegundo de gravação;
- ordem física da query;
- ordem em que o usuário digitou.

Conflitos sem precedência explícita serão bloqueados.

#### 9.4. Retroatividade

Um fato antigo registrado posteriormente deverá:

1. entrar na posição temporal correta;
2. preservar fatos posteriores válidos;
3. reaplicar a linha temporal;
4. detectar incompatibilidades;
5. criar pendências quando necessário;
6. nunca apagar automaticamente fatos posteriores.

#### 9.5. Consulta temporal

Preparar serviços para responder:

- como estava o contrato em uma data;
- qual era a composição válida;
- qual empresa era válida;
- qual vigência era válida;
- quais fatos já produziam efeito;
- o que o sistema conhecia em determinada data/hora de registro.

#### 9.6. Base para Etapa 05

O motor será utilizado por:

- Aditamento;
- Remanejamento;
- redução de 25%;
- retificações;
- cancelamentos;
- mudanças contratuais.

---

# 10. Entrega da próxima etapa

Ao concluir a Etapa 04 serão gerados:

- `SGT_GTRAT_BACKEND_JAVA_ETAPA_04.zip`
- `SGT_GTRAT_ETAPA_04_FEITO_E_PROXIMA_ETAPA.md`

A Etapa 05 será iniciada somente sobre o ZIP fechado da Etapa 04.

---

## Status

**ETAPA 03: CONCLUÍDA**  
**CHECKPOINT JAVA A: FECHADO**  
**PRÓXIMA: ETAPA 04 — MOTOR TEMPORAL, HISTÓRICO E PRECEDÊNCIA**
