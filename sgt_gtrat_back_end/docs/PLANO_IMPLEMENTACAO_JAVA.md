# SGT/GTRAT — Plano de Implementação do Backend Java

**Data:** 08/09/2026  
**Base funcional:** SGT_GTRAT_HISTORICO_COMPLETO_LEVANTAMENTO_REQUISITOS_V8_FINAL  
**Base técnica inicial:** sgt_gtrat_back_end.zip  
**Estratégia:** concluir e estabilizar integralmente o backend Java antes de iniciar a evolução do React.

---

# 1. Objetivo deste plano

Este documento organiza a implementação do backend Java do SGT/GTRAT em etapas controladas, evitando alterações grandes e simultâneas que possam gerar regressões difíceis de localizar.

O objetivo principal é transformar o backend atual em uma implementação aderente ao levantamento funcional V8, preservando tudo o que já funciona e substituindo, de forma progressiva, as partes do domínio que ainda refletem regras antigas.

O frontend React não faz parte desta etapa.

A implementação Java deve ser concluída, testada e estabilizada antes da retomada do React.

---

# 2. Diretriz principal

O backend atual não será descartado nem refeito integralmente do zero.

A estratégia será:

1. preservar infraestrutura existente que esteja correta;
2. corrigir build, configuração e estrutura quando necessário;
3. remodelar o núcleo de Contrato, Pessoa, Vínculo, Alocação e Temporalidade;
4. adaptar progressivamente frequência, documentos, permissões, relatórios e auditoria;
5. manter testes automatizados durante toda a evolução;
6. gerar checkpoints intermediários em ZIP;
7. somente ao final produzir o backend Java consolidado para servir de contrato estável ao React.

---

# 3. Premissas funcionais obrigatórias

A implementação deverá respeitar especialmente os seguintes princípios do levantamento V8:

- Contrato é a base da estrutura operacional.
- Pessoa/Colaborador, Vínculo Contratual e Alocação são conceitos distintos.
- Data efetiva e data/hora de registro são conceitos diferentes.
- Fato negocial publicado não é apagado fisicamente.
- Retificação cria nova versão e preserva a anterior.
- Retroatividade deve preservar a linha temporal e apresentar impactos.
- Nenhuma operação crítica deve mover, desligar, corrigir ou realocar pessoas silenciosamente.
- O histórico é a fonte canônica.
- A projeção atual é apenas uma conveniência técnica.
- Reinicialização da aplicação não pode criar ou alterar fatos negociais.
- A granularidade de Contrato, Vínculo e Alocação é diária.
- Eventos conflitantes na mesma data exigem precedência negocial explícita.
- Estados FUTURO, VIGENTE e ENCERRADO devem ser derivados da linha temporal.
- CANCELADO e ANULADO dependem de fatos negociais explícitos.

---

# 4. Estratégia de execução

A implementação será dividida em **12 passos principais**, agrupados em **4 checkpoints de entrega**.

## Checkpoint A

Passos:

- Passo 1
- Passo 2
- Passo 3

Entrega esperada:

- projeto compilando;
- estrutura técnica estabilizada;
- novo núcleo de Contrato;
- cadastros auxiliares;
- composição contratual;
- testes correspondentes;
- ZIP intermediário A.

## Checkpoint B

Passos:

- Passo 4
- Passo 5
- Passo 6

Entrega esperada:

- motor temporal;
- histórico/versionamento;
- aditamentos;
- remanejamentos;
- redução contratual;
- Pessoa;
- Vínculo;
- Matrícula;
- Desligamento;
- ZIP intermediário B.

## Checkpoint C

Passos:

- Passo 7
- Passo 8
- Passo 9

Entrega esperada:

- Alocação temporal;
- transferência de Gestão;
- documentos e SEI;
- frequência integrada ao novo modelo temporal;
- ZIP intermediário C.

## Checkpoint D

Passos:

- Passo 10
- Passo 11
- Passo 12

Entrega esperada:

- segurança e permissões definitivas;
- relatórios e indicadores;
- auditoria;
- pendências;
- testes completos;
- revisão de aderência;
- ZIP Java final.

---

# 5. Passo 1 — Diagnóstico e estabilização da base Java

**Esforço estimado:** Médio

## Objetivo

Criar uma base técnica confiável antes das alterações profundas de domínio.

## Atividades

- analisar integralmente a estrutura atual;
- verificar dependências Maven;
- validar versão Java;
- corrigir problemas de Maven Wrapper;
- revisar application.properties/application.yml;
- revisar `.env`;
- validar conexão MySQL;
- revisar Hibernate/JPA;
- revisar HikariCP;
- revisar Redis;
- revisar configuração de testes;
- identificar classes legadas que poderão ser reaproveitadas;
- identificar classes que deverão ser substituídas;
- mapear controllers, services, repositories e DTOs ligados aos requisitos novos;
- validar estrutura Controller → DTO → Service → Repository;
- executar build e testes existentes;
- registrar baseline dos testes.

## Resultado esperado

Backend antigo compilando de maneira reproduzível e com diagnóstico documentado.

## Critério de conclusão

- `mvn clean test` executável;
- `mvn clean package` executável;
- problemas estruturais identificados;
- nenhuma alteração funcional profunda ainda misturada com esta etapa.

---

# 6. Passo 2 — Remodelagem do domínio Contrato

**Esforço estimado:** Muito Alto

## Objetivo

Substituir o modelo simplificado de contrato pelo modelo definido no levantamento V8.

## Implementar

### Identificação

- número;
- ano;
- unicidade número + ano;
- histórico de correção excepcional;
- reserva de número/ano após publicação;
- liberação apenas para rascunho nunca publicado.

### Dados contratuais

- descrição;
- Tipo de Contratação;
- empresa terceirizada;
- CNPJ;
- razão social;
- nome fantasia;
- vigência;
- efetivo total originalmente publicado;
- efetivo vigente;
- Gestão Local responsável.

### Ciclo de vida

Separar:

- RASCUNHO;
- FUTURO;
- VIGENTE;
- ENCERRADO;
- CANCELADO;
- ANULADO.

Separar também a habilitação administrativa:

- ativo;
- inativo.

### Operações

- criação de rascunho;
- publicação;
- descarte de rascunho;
- inativação;
- reativação;
- encerramento;
- cancelamento de contrato futuro;
- anulação;
- correção excepcional;
- transferência excepcional de Gestão Local.

## Resultado esperado

Contrato deixa de ser um cadastro baseado apenas no estado atual e passa a possuir ciclo de vida coerente com o requisito.

---

# 7. Passo 3 — Composição Contratual e cadastros auxiliares

**Esforço estimado:** Alto

## Objetivo

Implementar a composição explícita do efetivo previsto.

## Composição

Cada composição deve representar:

`Lotação + Turno + Cargo + Efetivo previsto`

## Regras

- não gerar produto cartesiano;
- impedir combinação duplicada no mesmo período;
- soma das composições igual ao efetivo total vigente;
- quantidade zero bloqueia nova alocação;
- retorno de zero para positivo cria nova versão temporal;
- redução de composição ocupada exige prévia de impacto;
- pessoas não são realocadas automaticamente.

## Cadastros auxiliares

Revisar/implementar:

- Tipo de Contratação;
- Lotação;
- Turno;
- Cargo;
- Motivo de Desligamento.

## Temporalidade dos cadastros

- cadastro inativo continua disponível no histórico;
- pode ser usado retroativamente quando era válido na data;
- não pode ser usado em evento futuro sem reativação;
- reativação deve ser auditada.

## Excedente

Implementar:

- detecção;
- alerta;
- justificativa;
- auditoria;
- consulta do excedente;
- data de início do excedente;
- quantidade prevista;
- quantidade vinculada;
- quantidade excedente.

---

# 8. Passo 4 — Motor temporal, histórico e versionamento

**Esforço estimado:** Muito Alto

## Objetivo

Construir o núcleo mais importante do sistema.

O sistema deverá conseguir reconstruir a verdade negocial em qualquer data.

## Implementar conceitos

- data efetiva;
- data/hora de registro;
- usuário responsável;
- motivo;
- versão;
- versão anterior;
- versão vigente;
- evento substituído;
- evento cancelado;
- evento retificado.

## Precedência negocial

Implementar relações explícitas:

- SUCEDE;
- RETIFICA;
- CANCELA;
- SUBSTITUI;
- MANTÉM/COEXISTE.

## Regras

- ID do banco não define precedência;
- timestamp não define precedência;
- ordem de gravação não define precedência;
- conflito na mesma data sem precedência deve ser bloqueado;
- fato antigo inserido posteriormente deve ocupar sua posição histórica correta;
- eventos posteriores válidos devem ser reaplicados;
- incompatibilidades devem gerar pendências;
- fatos posteriores nunca devem ser apagados automaticamente.

## Resultado esperado

Serviços capazes de responder:

- estado do contrato em uma data;
- composição vigente em uma data;
- empresa vigente em uma data;
- vigência vigente em uma data;
- eventos válidos até uma data;
- visão conhecida pelo sistema em determinado momento.

---

# 9. Passo 5 — Aditamentos, reduções e remanejamentos

**Esforço estimado:** Muito Alto

## Aditamentos

Implementar:

- múltiplos aditamentos;
- identificação;
- data do documento;
- data da assinatura;
- data efetiva;
- justificativa;
- responsável;
- data/hora de registro;
- documento/SEI;
- versão;
- retificação;
- cancelamento de programação futura.

## Alterações possíveis

- efetivo;
- composição;
- vigência;
- Lotação;
- Turno;
- Cargo;
- descrição;
- Tipo de Contratação;
- empresa;
- CNPJ;
- razão social;
- nome fantasia.

## Redução contratual

Implementar regra de 25%.

### Base

Efetivo originalmente publicado.

### Piso

75% do efetivo original.

### Regras

- aumentos posteriores não alteram a base;
- limite cumulativo;
- redução cancelada/retificada deixa de consumir limite vigente;
- arredondamento para cima quando fracionário;
- composição individual pode cair mais de 25%;
- regra incide sobre o total do contrato.

## Exceção acima de 25%

Exigir:

- fundamento jurídico/negocial;
- documento comprobatório;
- permissão excepcional;
- Gestor Local autorizado;
- justificativa;
- prévia de impacto;
- auditoria reforçada.

## Remanejamento

Implementar:

- redistribuição entre composições existentes;
- soma das variações = zero;
- total do contrato preservado;
- retificação;
- programação futura;
- cancelamento futuro;
- histórico integral.

---

# 10. Passo 6 — Pessoa, Vínculo, Matrícula e Desligamento

**Esforço estimado:** Muito Alto

## Pessoa

Separar Pessoa do vínculo contratual.

### Campos principais

- nome;
- CPF;
- RG;
- CTPS;
- NIT/PIS;
- nascimento;
- e-mail;
- endereço.

## CPF

- único globalmente;
- validado;
- armazenado sem pontuação;
- não editável no fluxo comum;
- correção excepcional auditada;
- sem fusão automática de pessoas.

## Vínculo

Criar entidade própria para:

`Pessoa + Contrato + período`

## Regras

- impedir sobreposição global de vínculos;
- permitir vínculo futuro;
- cancelar vínculo futuro programado;
- permitir retificação;
- preservar cadeia histórica;
- revalidar impactos.

## Matrícula

Mover matrícula para o Vínculo.

### Regras

- opcional por padrão;
- configurável como obrigatória;
- única no contrato;
- não reutilizável após vínculo histórico;
- recontratação cria nova matrícula.

## Desligamento

Implementar:

- data efetiva;
- motivo;
- responsável;
- data/hora de registro;
- retroativo;
- futuro;
- retificação;
- cancelamento de programação.

A data de desligamento será o último dia do vínculo.

---

# 11. Passo 7 — Alocação temporal

**Esforço estimado:** Muito Alto

## Objetivo

Retirar Lotação, Turno e Cargo do conceito de posição fixa do colaborador.

## Alocação

Cada período representa:

`Lotação + Turno + Cargo`

## Regras

- mudança cria nova alocação;
- não cria novo vínculo;
- não gera desligamento;
- pode ser retroativa;
- eventos posteriores válidos permanecem;
- no máximo uma alocação efetiva por pessoa/dia;
- todo dia de vínculo operacional deve possuir exatamente uma alocação;
- consulta mensal apresenta todas as mudanças do período.

## Seleção encadeada

Contrato → Lotação → Turno → Cargo.

Apenas combinações válidas podem ser utilizadas.

## Inativação administrativa

Inativar contrato:

- não encerra vínculo;
- não encerra alocação;
- apenas bloqueia operação.

Ao reativar, a alocação anterior pode voltar a operar quando ainda temporalmente válida.

## Transferência excepcional de Gestão Local

Implementar:

- Gestão anterior;
- Gestão nova;
- data efetiva;
- motivo;
- autorização;
- auditoria;
- contexto histórico;
- mapeamento para cadastros da nova Gestão;
- bloqueio se não houver composição válida posterior.

---

# 12. Passo 8 — Documentos e SEI

**Esforço estimado:** Alto

## Implementar fontes comprobatórias

- anexo;
- SEI;
- ambos.

## Regras

Contrato publicado exige fonte comprobatória.

Aditamento exige fonte comprobatória.

Remanejamento pode possuir fonte.

## Histórico

- arquivo publicado não é apagado;
- arquivo incorreto é invalidado/substituído;
- referência SEI incorreta é versionada;
- SEI anterior permanece histórico;
- mesmo processo SEI pode estar relacionado a vários contratos.

## Classificação

Criar:

- NEGOCIAL;
- PESSOAL_RESTRITO.

## Auditoria

Acesso a documento PESSOAL_RESTRITO deve ser auditado.

---

# 13. Passo 9 — Frequência integrada ao novo modelo

**Esforço estimado:** Muito Alto

## Objetivo

Fazer a frequência utilizar Vínculo e Alocação válidos na data.

## Regra temporal

A frequência deve resolver:

- contrato válido;
- vínculo válido;
- alocação válida;
- escopo histórico.

## Janela do Operacional

7 dias corridos.

A própria data da frequência é o dia 1.

Fuso:

`America/Sao_Paulo`

No início do 8º dia:

`FECHADA`

## Gestor Local

- pode corrigir dentro dos 7 dias;
- pode corrigir depois dos 7 dias;
- toda correção do Gestor é retificação;
- registro corrigido pelo Gestor deixa de ser editável pelo Operacional.

## Jornada noturna

A frequência pertence à data de início da jornada.

Exemplo:

22:00 de 10/09 → 06:00 de 11/09

Pertence a 10/09.

## Retroatividade

Quando fato retroativo atingir frequência:

- preservar original;
- marcar impacto;
- retirar da visão operacional quando necessário;
- manter auditoria;
- gerar retificação quando aplicável.

---

# 14. Passo 10 — Perfis, permissões e escopos

**Esforço estimado:** Alto

## Revisar os perfis

- Administrador;
- Gestor Nacional;
- Gestor Estadual;
- Gestor Local;
- Presidente;
- Usuário Operacional.

## Gestor Local

Principal perfil de escrita negocial.

## Gestor Estadual

Consulta/auditoria estadual.

Sem escrita negocial comum.

## Gestor Nacional

Consulta/auditoria nacional.

Sem escrita negocial comum.

## Presidente

Consulta nacional.

Sem escrita negocial.

## Administrador

Administração técnica.

Não deve ser tratado automaticamente como responsável negocial.

## Operacional

Atua principalmente na frequência.

## Permissões excepcionais

Implementar permissões específicas para operações como:

- correção de número/ano;
- redução excepcional;
- transferência de Gestão;
- acesso restrito quando aplicável.

## Minimização de dados

Revisar retorno de DTOs de acordo com o perfil.

---

# 15. Passo 11 — Relatórios, indicadores, auditoria e pendências

**Esforço estimado:** Muito Alto

## Relatórios

Adaptar relatórios para utilizar a verdade temporal.

### Relatório oficial atual

Usar versão vigente/retificada.

### Versões antigas

Continuam auditáveis.

### Emissão

Registrar:

- período;
- geração;
- usuário;
- versão;
- pendências;
- original ou retificada.

## Indicadores

Preparar backend para fornecer:

- previsto;
- presentes;
- ausentes;
- pendentes;
- excedente;
- consolidações.

Considerar filtros por:

- contrato;
- Lotação;
- Turno;
- Cargo;
- data;
- período.

## Auditoria

Permitir identificar:

- fato;
- data efetiva;
- data de registro;
- autor;
- valor anterior;
- valor novo;
- versões;
- relações de precedência.

## Central de pendências

Estados:

- ABERTA;
- EM_TRATAMENTO;
- RESOLVIDA.

Cada pendência deve possuir contexto e origem.

Pendência deve bloquear somente o escopo realmente afetado.

---

# 16. Passo 12 — Testes, carga, aderência e fechamento do Java

**Esforço estimado:** Muito Alto

## Testes unitários

JUnit + Mockito.

Cobrir:

- regras temporais;
- contratos;
- composições;
- aditamentos;
- remanejamentos;
- redução;
- vínculo;
- alocação;
- desligamento;
- frequência;
- permissões.

## Testes de integração

Cobrir:

- JPA;
- MySQL;
- transações;
- constraints;
- concorrência;
- endpoints;
- segurança.

## Testes arquiteturais

ArchUnit para verificar:

- Controller → Service → Repository;
- DTOs;
- ausência de acesso indevido a Repository por Controller;
- separação de responsabilidades.

## Testes de concorrência

Garantir que:

- requisição duplicada não gere fato negocial duplicado;
- dois usuários não criem versões conflitantes sem validação;
- prévia de impacto seja revalidada na confirmação.

## Testes de carga

Validar especialmente:

- frequência em grande volume;
- relatórios;
- indicadores;
- consultas históricas;
- pesquisa de colaboradores;
- reconstrução temporal.

## Cenários obrigatórios de borda

Incluir, no mínimo:

1. contrato 100 → 140 → 75;
2. contrato original 101 com piso 76;
3. cancelamento de redução;
4. redução excepcional acima de 25%;
5. documento antigo cadastrado posteriormente;
6. conflito de dois eventos na mesma data;
7. inativação retroativa atingindo frequência;
8. reativação sem recriação artificial de alocação;
9. frequência fechando no 8º dia;
10. correção do Gestor durante janela do Operacional;
11. jornada noturna;
12. correção de CPF;
13. transferência excepcional de Gestão;
14. troca real de empresa;
15. correção material de CNPJ;
16. uso retroativo de catálogo atualmente inativo;
17. bloqueio de catálogo inativo em evento futuro;
18. anulação desde a origem;
19. anulação com alcance parcial;
20. contrato futuro cancelado.

---

# 17. Checkpoints e entregas

## ZIP Java A

Após passos 1 a 3.

Deve conter:

- projeto compilável;
- contrato remodelado;
- composição;
- cadastros;
- testes do bloco;
- relatório do que foi implementado.

## ZIP Java B

Após passos 4 a 6.

Deve conter:

- temporalidade;
- versionamento;
- aditamentos;
- remanejamentos;
- Pessoa;
- Vínculo;
- Matrícula;
- Desligamento;
- testes.

## ZIP Java C

Após passos 7 a 9.

Deve conter:

- Alocação;
- transferência de Gestão;
- documentos;
- SEI;
- frequência;
- testes de integração.

## ZIP Java Final

Após passos 10 a 12.

Deve conter:

- backend completo;
- segurança;
- permissões;
- relatórios;
- auditoria;
- pendências;
- testes completos;
- documentação final;
- build reproduzível;
- configuração de ambiente;
- pronto para integração com React.

---

# 18. Estimativa qualitativa de esforço

| Bloco | Esforço |
|---|---|
| Passo 1 | Médio |
| Passo 2 | Muito Alto |
| Passo 3 | Alto |
| Passo 4 | Muito Alto |
| Passo 5 | Muito Alto |
| Passo 6 | Muito Alto |
| Passo 7 | Muito Alto |
| Passo 8 | Alto |
| Passo 9 | Muito Alto |
| Passo 10 | Alto |
| Passo 11 | Muito Alto |
| Passo 12 | Muito Alto |

## Esforço geral do backend

**9/10**

O esforço principal não está nos CRUDs.

Os componentes de maior complexidade são:

1. temporalidade;
2. reconstrução histórica;
3. retroatividade;
4. versionamento;
5. precedência;
6. análise de impacto;
7. consistência entre Contrato, Vínculo, Alocação e Frequência;
8. relatórios baseados na verdade histórica.

---

# 19. Ordem obrigatória de implementação

A ordem recomendada é:

```text
PASSO 1
   ↓
PASSO 2
   ↓
PASSO 3
   ↓
ZIP JAVA A
   ↓
PASSO 4
   ↓
PASSO 5
   ↓
PASSO 6
   ↓
ZIP JAVA B
   ↓
PASSO 7
   ↓
PASSO 8
   ↓
PASSO 9
   ↓
ZIP JAVA C
   ↓
PASSO 10
   ↓
PASSO 11
   ↓
PASSO 12
   ↓
ZIP JAVA FINAL
   ↓
CONGELAMENTO DO CONTRATO DA API
   ↓
INÍCIO DO REACT
```

---

# 20. Estratégia para o React

O React não deve ser desenvolvido simultaneamente com as grandes mudanças do domínio Java.

Ao final do Java:

1. congelar contratos dos endpoints;
2. documentar DTOs;
3. documentar filtros;
4. documentar paginação;
5. documentar regras de permissão;
6. documentar mensagens de erro;
7. documentar endpoints de histórico;
8. documentar endpoints dos indicadores;
9. documentar endpoints de relatórios;
10. somente então adaptar o React antigo.

Essa estratégia reduz o risco de novas regressões no frontend.

---

# 21. Princípio de preservação do backend existente

Durante a implementação:

- não remover funcionalidade existente sem verificar dependências;
- não renomear massivamente classes sem necessidade;
- não realizar refatorações cosméticas junto com mudanças negociais;
- não modificar Java e React simultaneamente;
- manter testes existentes quando ainda representarem regras válidas;
- substituir testes que representem regras superadas;
- documentar regressões encontradas;
- manter checkpoints funcionando.

---

# 22. Definição de pronto do backend Java

O backend Java somente será considerado concluído quando:

- compilar sem erro;
- testes unitários passarem;
- testes de integração passarem;
- regras V8 estiverem rastreadas;
- histórico puder ser reconstruído;
- retroatividade estiver coberta;
- precedência estiver implementada;
- operações críticas estiverem auditadas;
- contratos, vínculos e alocações estiverem temporalmente corretos;
- frequência utilizar a posição histórica correta;
- relatórios utilizarem a verdade vigente;
- permissões estiverem coerentes;
- não houver exclusão física indevida de fatos publicados;
- operações duplicadas estiverem protegidas;
- consultas de grande volume forem aceitáveis;
- API estiver estável para consumo pelo React.

---

# 23. Decisão de execução

Para reduzir risco e consumo de recursos, a implementação será feita em etapas.

A próxima atividade recomendada é:

**PASSO 1 — Diagnóstico e estabilização completa do backend Java atual.**

Somente depois do Passo 1 serão iniciadas alterações profundas nas entidades e regras negociais.

---

# 24. Resumo executivo

Quantidade de passos Java:

**12**

Quantidade de checkpoints:

**4**

Estratégia:

**Java primeiro → estabilização → contrato de API → React depois**

Prioridade máxima:

**preservação histórica e temporal**

Maior risco técnico:

**tentar adaptar o modelo antigo sem separar corretamente Pessoa, Vínculo e Alocação**

Maior proteção contra regressões:

**testes por etapa + ZIP intermediário + não alterar Java e React simultaneamente**

Status:

**PLANO PRONTO PARA INÍCIO DA IMPLEMENTAÇÃO**
