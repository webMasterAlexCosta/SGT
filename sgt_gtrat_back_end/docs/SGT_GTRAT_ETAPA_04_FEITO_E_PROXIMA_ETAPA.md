# SGT/GTRAT — Etapa 04 concluída e próxima etapa

**Data:** 08/09/2026  
**Etapa:** 04 — Motor Temporal, Histórico, Versionamento e Precedência Negocial  
**Base:** SGT_GTRAT_BACKEND_JAVA_ETAPA_03.zip  
**Próxima etapa:** 05 — Aditamentos, redução contratual e remanejamentos

---

# 1. Objetivo da Etapa 04

Criar a infraestrutura temporal comum do backend para que fatos negociais não sejam resolvidos pela posição atual das entidades nem pela ordem técnica em que foram gravados.

A partir desta etapa, o núcleo contratual passa a distinguir explicitamente:

- **data efetiva** — quando o fato produz efeito negocial;
- **data/hora de registro** — quando o sistema tomou conhecimento;
- **identidade lógica do fato** — independente do ID técnico do banco;
- **versão negocial**;
- **chave semântica de conflito**;
- **ordem documental**, quando existir;
- **situação corrente da versão**;
- **relações explícitas de precedência**.

O motor criado nesta etapa será utilizado diretamente por Aditamento, Remanejamento, redução contratual, retificações e cancelamentos nas etapas seguintes.

---

# 2. Implementações realizadas

## 2.1. Evolução de `EventoCicloVidaContrato`

A linha de eventos criada nas Etapas 02/03 foi reaproveitada como base do motor temporal. Não foi criado um segundo histórico paralelo.

Cada evento agora possui também:

- `fatoLogicoId`;
- `chaveNegocial`;
- `versaoNegocial`;
- `ordemDocumental`;
- `situacaoFato`.

### Identidade lógica

`fatoLogicoId` identifica uma cadeia de versões sem utilizar o ID do banco como identidade negocial.

Em uma retificação/substituição:

- o fato anterior permanece gravado;
- o novo fato mantém a mesma identidade lógica;
- a versão é incrementada.

### Compatibilidade com eventos antigos

As novas colunas foram mantidas compatíveis com bancos que já possuam eventos anteriores.

Eventos legados sem os novos metadados recebem valores de leitura seguros, enquanto novos eventos são gravados integralmente com os metadados temporais.

Não foi criada dependência de `NOT NULL` que exigisse apagar eventos históricos para migração.

---

# 3. Relações explícitas de precedência

Foi criado o enum:

`TipoRelacaoPrecedenciaNegocial`

com:

- `SUCEDE`;
- `RETIFICA`;
- `CANCELA`;
- `SUBSTITUI`;
- `MANTEM_COEXISTE`.

Também foi criada a entidade:

`RelacaoPrecedenciaEventoContrato`

A relação registra:

- contrato;
- evento fonte;
- evento alvo;
- tipo da relação;
- data/hora de registro;
- usuário responsável;
- motivo.

## Semântica adotada

Exemplos:

`Evento B SUCEDE Evento A`

significa que B vem negocialmente depois de A.

`Evento B RETIFICA Evento A`

significa que A permanece auditável, mas deixa de ser a verdade vigente quando a retificação passa a ser conhecida.

`Evento B CANCELA Evento A`

preserva A, mas retira sua validade conforme a cadeia negocial.

`Evento B SUBSTITUI Evento A`

preserva A como versão histórica e torna B a versão aplicável.

---

# 4. Bloqueio de conflito sem precedência

Foi implementada detecção por:

`Contrato + chaveNegocial + dataEfetiva`

Chaves atuais:

- `ESTADO_NEGOCIAL`;
- `HABILITACAO_ADMINISTRATIVA`;
- `VIGENCIA`;
- `IDENTIFICACAO`;
- `GESTAO_LOCAL`.

Quando um novo evento colide semanticamente com outro fato válido na mesma data:

- o ID do banco não é usado para decidir;
- `registradoEm` não é usado para decidir;
- a ordem do `INSERT` não é usada para decidir;
- a ordem em que o usuário clicou não é usada para decidir.

A operação retorna conflito HTTP `409` se a relação não for informada.

O erro informa os eventos conflitantes que precisam ser relacionados.

---

# 5. Novos campos no comando das operações contratuais

`AcaoCicloVidaContratoDTO` passa a aceitar, opcionalmente:

- `eventoPrecedenteId`;
- `relacaoPrecedencia`;
- `ordemDocumental`.

Os campos antigos foram preservados:

- `dataEfetiva`;
- `dataAto`;
- `motivo`;
- `referenciaSei`.

Foi mantido construtor compatível com os testes e chamadas das Etapas 02 e 03.

Quando não existe conflito, precedência não precisa ser inventada artificialmente.

Quando existe conflito real na mesma data/chave, `eventoPrecedenteId` e `relacaoPrecedencia` precisam ser informados juntos.

---

# 6. Ordenação sem dependência técnica

Foi criado o serviço:

`MotorTemporalContratoServico`

Ele é responsável por reconstruir a linha temporal.

Eventos são primeiramente agrupados pela **data efetiva**.

Dentro da mesma data, quando a ordem altera o resultado, são utilizadas relações negociais explícitas.

Foi implementada ordenação topológica para relações `SUCEDE`.

Um ciclo de precedência é tratado como inconsistência e bloqueado.

Para fatos independentes que não disputam a mesma chave, qualquer ordenação interna utilizada apenas para apresentação não é utilizada como regra negocial.

---

# 7. Retificação, cancelamento e substituição sem exclusão

O motor calcula fatos válidos a partir das relações conhecidas.

Relações:

- `RETIFICA`;
- `CANCELA`;
- `SUBSTITUI`;

retiram o alvo da verdade vigente sem excluir sua linha histórica.

A coluna `situacaoFato` funciona como projeção/conveniência atual:

- `VIGENTE`;
- `RETIFICADO`;
- `CANCELADO`;
- `SUBSTITUIDO`.

Entretanto, **consultas históricas não confiam somente nessa projeção**.

A validade é reconstruída novamente usando:

- fatos existentes;
- relações existentes;
- instante `conhecidoAte`.

Isso evita que uma retificação registrada hoje altere artificialmente a resposta para a pergunta:

> O que o sistema conhecia antes dessa retificação ser registrada?

---

# 8. Retificação de retificação

O motor foi preparado para a cadeia de correções.

As relações invalidantes são avaliadas considerando se o próprio evento fonte ainda permanece válido.

Exemplo conceitual:

1. Fato A;
2. Fato B cancela A;
3. Fato C retifica/invalida B.

A existência de C impede que B continue sendo utilizado automaticamente como fonte válida para invalidar A.

Essa regra é importante para permitir cadeias longas de versões sem limite artificial.

---

# 9. Versão negocial do Contrato

Foi criada a entidade:

`VersaoContratoNegocial`

Ela representa uma fotografia imutável dos dados contratuais de uma versão.

Campos preservados na fotografia:

- número;
- ano;
- descrição;
- Tipo de Contratação — ID e descrição fotografada;
- empresa/fornecedor — ID;
- CNPJ;
- razão social;
- nome fantasia;
- vigência inicial;
- vigência final;
- efetivo previsto;
- Gestão Local — ID e nome fotografado;
- data efetiva da versão;
- data/hora de registro;
- evento de origem;
- número da versão.

## Publicação

A publicação agora cria:

`Versão Negocial 1`

Essa fotografia será a base para os Aditamentos da Etapa 05.

Cada alteração contratual futura poderá produzir uma nova fotografia sem reescrever a publicação original.

---

# 10. Consulta bitemporal

Foi implementado:

`GET /api/contratos/{id}/posicao-temporal`

Parâmetros:

- `data` — obrigatório;
- `conhecidoAte` — opcional.

## 10.1. Sem `conhecidoAte`

Exemplo:

```text
GET /api/contratos/10/posicao-temporal?data=2026-06-15
```

Retorna a **verdade efetiva atual** sobre 15/06/2026, considerando todas as retificações válidas conhecidas atualmente.

## 10.2. Com `conhecidoAte`

Exemplo:

```text
GET /api/contratos/10/posicao-temporal?data=2026-06-15&conhecidoAte=2026-07-01T14:00:00Z
```

Retorna a posição de 15/06/2026 utilizando somente fatos e relações que já estavam registrados até o instante informado.

Essa separação permite responder às duas perguntas exigidas pelo levantamento:

1. qual é hoje a verdade válida sobre uma data passada;
2. o que o sistema conhecia naquela época.

---

# 11. Conteúdo da posição temporal nesta etapa

`PosicaoTemporalContratoDTO` retorna:

- contrato;
- data de referência;
- instante limite de conhecimento;
- estado negocial;
- situação temporal;
- habilitação administrativa;
- vigência inicial;
- vigência final;
- versão contratual aplicável;
- eventos aplicados;
- relações de precedência consideradas.

Nesta Etapa 04 a reconstrução cobre os fatos já existentes no backend:

- publicação;
- inativação administrativa;
- reativação administrativa;
- encerramento antecipado;
- cancelamento de contrato FUTURO;
- anulação.

Aditamentos, Remanejamentos e alterações de composição serão conectados ao mesmo motor na Etapa 05.

---

# 12. Novas consultas auxiliares

## Relações

```text
GET /api/contratos/{id}/eventos/precedencias
```

Retorna as relações explícitas existentes entre eventos.

## Versões negociais

```text
GET /api/contratos/{id}/versoes
```

Retorna as fotografias negociais do Contrato.

---

# 13. Retroatividade

Um evento com data efetiva antiga não é colocado no fim da linha histórica apenas porque foi digitado hoje.

O motor:

1. utiliza a data efetiva para posicionar o fato;
2. preserva fatos posteriores;
3. reaplica os fatos posteriores válidos;
4. considera relações de retificação/cancelamento/substituição;
5. não apaga automaticamente eventos posteriores.

## Pendências por incompatibilidade

A infraestrutura desta etapa identifica a linha de eventos que deverá ser reaplicada.

A criação de pendência ocorre quando uma **regra de domínio** determinar que um evento posterior se tornou incompatível.

Nesta Etapa 04 ainda não existe Aditamento/Remanejamento para produzir esse tipo de incompatibilidade contratual concreta.

Por isso, não foi criada uma pendência fictícia apenas para satisfazer estrutura.

Na Etapa 05, ao conectar Aditamento e Remanejamento ao motor, a análise de impacto utilizará esta linha temporal para:

- localizar eventos posteriores;
- reaplicar os válidos;
- identificar incompatibilidades reais;
- impedir conclusão quando necessário;
- criar pendência para o escopo efetivamente afetado.

Conflitos de precedência da mesma data são bloqueados antes da conclusão e, portanto, não geram fato inconsistente para depois ser corrigido.

---

# 14. Correção da projeção administrativa em evento retroativo

Foi corrigida uma consequência importante das Etapas anteriores.

Antes, uma inativação retroativa poderia simplesmente executar:

`ativo = false`

mesmo quando uma reativação posterior já estivesse válida.

Agora, após registrar uma inativação/reativação retroativa, a projeção atual é reconstruída novamente pela linha temporal.

Exemplo:

1. inativação efetiva em 01/03;
2. reativação efetiva em 20/03;
3. em setembro é cadastrada retroativamente uma inativação de 10/03.

A nova ocorrência não pode deixar o contrato atualmente inativo se a reativação de 20/03 continuar válida.

O motor reaplica os fatos posteriores e a projeção atual permanece coerente.

---

# 15. Arquivos novos

## Entidades

- `TipoRelacaoPrecedenciaNegocial.java`
- `SituacaoFatoNegocial.java`
- `RelacaoPrecedenciaEventoContrato.java`
- `VersaoContratoNegocial.java`

## Repositórios

- `RelacaoPrecedenciaEventoContratoRepositorio.java`
- `VersaoContratoNegocialRepositorio.java`

## DTOs

- `RelacaoPrecedenciaEventoContratoDTO.java`
- `VersaoContratoNegocialDTO.java`
- `PosicaoTemporalContratoDTO.java`

## Serviços

- `MotorTemporalContratoServico.java`

## Testes

- `MotorTemporalContratoEtapa04Test.java`

---

# 16. Arquivos principais alterados

- `EventoCicloVidaContrato.java`
- `EventoCicloVidaContratoRepositorio.java`
- `EventoCicloVidaContratoDTO.java`
- `AcaoCicloVidaContratoDTO.java`
- `ContratoServico.java`
- `ContratoControlador.java`

---

# 17. Testes adicionados

Foram adicionados cenários automatizados para verificar:

1. conflito na mesma data/chave sem precedência é bloqueado;
2. `SUCEDE` explícito resolve conflito sem depender da ordem de gravação;
3. `RETIFICA` preserva a identidade lógica e incrementa a versão;
4. fato retificado continua preservado, mas deixa de ser vigente;
5. consulta `conhecidoAte` diferencia o que o sistema conhecia antes e depois do registro do fato.

---

# 18. Validações executadas nesta etapa

## Preflight

Executado:

```text
scripts/validar-backend.sh
```

Resultado:

```text
Preflight OK: Java 17.0.11, Wrapper e scripts POSIX consistentes.
```

## Compilação direcionada do núcleo temporal

Foram compilados com `javac --release 17`, usando stubs das dependências externas já utilizadas nas etapas anteriores:

- novas entidades;
- novos enums;
- novos DTOs;
- novos repositórios;
- `MotorTemporalContratoServico`.

Resultado:

```text
CORE_JAVAC_OK
```

## Compilação direcionada de integração

Também foram compilados contra as assinaturas das Etapas 02/03:

- `ContratoServico`;
- `ContratoControlador`;
- integração com `MotorTemporalContratoServico`.

Resultado:

```text
INTEGRATION_JAVAC_OK
```

## Smoke test executável do motor

Além da compilação, foi executado um teste isolado em memória do núcleo temporal, cobrindo:

- bloqueio de conflito da mesma data/chave sem precedência;
- gravação permitida quando `SUCEDE` é informado explicitamente;
- persistência da relação de precedência.

Resultado:

```text
SMOKE_ETAPA04_OK
```

---

# 19. Maven Wrapper

Foi tentado novamente:

```text
./mvnw -q -DskipTests compile
```

Resultado do ambiente atual:

```text
Baixando Apache Maven 3.9.16...
curl: (6) Could not resolve host: repo.maven.apache.org
```

Portanto:

- o problema continua sendo resolução/acesso externo ao Maven Central;
- não é erro Java identificado pelo compilador direcionado;
- `mvn test` completo **não é declarado como executado/aprovado**;
- o projeto continua Maven, sem migração desnecessária para Gradle.

---

# 20. Requisitos temporais cobertos pela Etapa 04

## TMP-003

Coberto pela estrutura de fato: data efetiva, registro, usuário e motivo.

## TMP-004

Motor de reconstrução por data efetiva implementado.

## TMP-005

`registradoEm` é separado da data efetiva e participa do corte `conhecidoAte`.

## TMP-006

Nenhuma rotina de inicialização foi criada para inventar fatos. A posição é reconstruída a partir do histórico.

## TMP-008

Inserção retroativa é ordenada pela data efetiva e fatos posteriores não são apagados.

## TMP-009

Base de reaplicação está pronta. Incompatibilidades concretas serão criadas pelas regras de domínio da Etapa 05 em diante, sem exclusão automática.

## TMP-010

Conflitos na mesma data/chave são bloqueados sem precedência explícita.

## TMP-011

Fatos independentes podem coexistir.

## TMP-012

Um documento/fato antigo registrado posteriormente é reposicionado pela data efetiva e não substitui silenciosamente os posteriores.

## TMP-016

Foram implementadas as relações:

- SUCEDE;
- RETIFICA;
- CANCELA;
- SUBSTITUI;
- MANTEM_COEXISTE.

## TMP-017

A situação temporal continua derivada da linha válida e da data; não foi criado job para fabricar mudança de estado.

---

# 21. Pontos deliberadamente não antecipados

A Etapa 04 não implementa ainda:

- entidade de Aditamento;
- itens quantitativos do Aditamento;
- Remanejamento;
- cálculo acumulado de redução de 25%;
- exceção jurídica acima de 25%;
- modificação temporal de composição após publicação;
- mudança temporal de empresa/CNPJ por aditamento;
- alteração temporal de Tipo de Contratação;
- prorrogação por aditamento;
- Central de Pendências completa.

Esses itens pertencem à Etapa 05 ou etapas posteriores e agora poderão utilizar o motor temporal sem duplicar lógica.

---

# 22. Próxima etapa — Etapa 05

## Aditamentos, redução contratual e remanejamentos

**Esforço:** Muito Alto

A Etapa 05 utilizará diretamente:

- `MotorTemporalContratoServico`;
- `VersaoContratoNegocial`;
- `RelacaoPrecedenciaEventoContrato`;
- `fatoLogicoId`;
- `versaoNegocial`;
- consulta bitemporal.

---

# 23. Plano da Etapa 05

## 23.1. Aditamento

Criar entidade própria com:

- identificação/número;
- data do documento;
- data da assinatura;
- data efetiva;
- justificativa;
- responsável;
- data/hora de registro;
- SEI/anexo conforme disponibilidade documental atual;
- evento temporal de origem.

Permitir alteração documentada de:

- efetivo total;
- distribuição do efetivo;
- vigência;
- Lotação;
- Turno;
- Cargo;
- composição;
- descrição;
- Tipo de Contratação;
- empresa/CNPJ;
- razão social;
- nome fantasia.

Aditamento não será utilizado para substituir operações próprias de:

- publicação;
- inativação;
- reativação;
- encerramento;
- cancelamento;
- anulação.

## 23.2. Aditamento quantitativo

Registrar por composição:

- quantidade anterior;
- variação;
- quantidade resultante.

## 23.3. Redução de 25%

Implementar:

- base fixa = efetivo originalmente publicado;
- piso = 75% da base original;
- cálculo cumulativo somente de reduções válidas;
- aumento posterior não modifica a base;
- cancelamento/retificação deixa de consumir limite quando deixa de ser válido;
- arredondamento do piso para cima.

Cenários obrigatórios:

```text
100 -> 140 -> 75 = permitido
101 -> piso 76
```

## 23.4. Exceção acima de 25%

Exigir:

- fundamento jurídico/negocial;
- fonte comprobatória;
- permissão técnica excepcional;
- Gestor Local autorizado;
- justificativa;
- prévia de impacto;
- auditoria reforçada.

## 23.5. Remanejamento

Criar fato próprio com:

- identificação;
- data efetiva;
- distribuição anterior;
- distribuição resultante;
- justificativa;
- responsável;
- registro;
- SEI opcional;
- anexo opcional quando a infraestrutura documental estiver disponível.

Regras:

- total do contrato preservado;
- soma das variações = zero;
- apenas composições já existentes;
- nova composição exige Aditamento;
- pode ser retificado;
- pode ser programado;
- programação futura pode ser cancelada sem exclusão física.

## 23.6. Retroatividade

Ao cadastrar Aditamento/Remanejamento antigo:

1. inserir na data efetiva real;
2. gerar nova versão negocial;
3. reaplicar eventos posteriores;
4. identificar incompatibilidades concretas;
5. impedir movimentação automática de pessoas;
6. gerar pendência quando o evento posterior precisar de tratamento.

## 23.7. Prévia de impacto

Preparar a base de prévia para operações que:

- reduzam efetivo;
- zerem composição;
- removam composição;
- alterem vigência;
- tornem programação futura incompatível.

A prévia deverá ser revalidada no momento da confirmação nas etapas que introduzirem os vínculos temporais completos.

---

# 24. Entrega da Etapa 05

Ao concluir a próxima etapa serão gerados:

- `SGT_GTRAT_BACKEND_JAVA_ETAPA_05.zip`
- `SGT_GTRAT_ETAPA_05_FEITO_E_PROXIMA_ETAPA.md`

A Etapa 06 será iniciada somente sobre o ZIP fechado da Etapa 05.

---

# 25. Status

**ETAPA 04: CONCLUÍDA**  
**MOTOR TEMPORAL: IMPLANTADO**  
**VERSIONAMENTO CONTRATUAL: BASE IMPLANTADA**  
**PRECEDÊNCIA NEGOCIAL: IMPLANTADA**  
**CONSULTA BITEMPORAL: IMPLANTADA**  
**PRÓXIMA: ETAPA 05 — ADITAMENTOS, REDUÇÃO E REMANEJAMENTOS**
