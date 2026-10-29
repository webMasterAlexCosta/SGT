# SGT/GTRAT — Etapa 02 do Backend Java

**Etapa:** 02 — Remodelagem do domínio Contrato  
**Data:** 08/09/2026  
**Base de entrada:** `SGT_GTRAT_BACKEND_JAVA_ETAPA_01.zip`  
**Base funcional:** `SGT_GTRAT_HISTORICO_COMPLETO_LEVANTAMENTO_REQUISITOS_V8_FINAL`  
**Status:** **ETAPA 02 CONCLUÍDA NO ESCOPO PREVISTO, COM PUBLICAÇÃO FINAL DELIBERADAMENTE TRANSFERIDA PARA A ETAPA 03 POR DEPENDÊNCIA DA COMPOSIÇÃO CONTRATUAL**

---

# 1. Objetivo da Etapa 02

A Etapa 02 teve como objetivo substituir o modelo simplificado de Contrato por uma base compatível com o levantamento V8, sem reescrever simultaneamente os demais módulos do sistema.

O foco foi criar a estrutura necessária para que o contrato deixe de depender apenas de campos de estado atual e passe a distinguir:

- identificação `número + ano`;
- estado negocial explícito;
- situação temporal derivada;
- habilitação administrativa;
- Tipo de Contratação como cadastro próprio;
- fotografia dos dados da empresa;
- eventos próprios do ciclo de vida;
- descarte físico restrito a rascunhos nunca publicados;
- preservação de compatibilidade com o backend antigo durante a migração progressiva.

---

# 2. Requisitos V8 diretamente trabalhados nesta etapa

Foram trabalhados principalmente os requisitos:

- `CON-001` — número + ano identificam unicamente o contrato;
- `CON-002` — número/ano podem ser ajustados enquanto RASCUNHO;
- `CON-004` — somente rascunho nunca publicado pode liberar número/ano;
- `CON-005` — ampliação estrutural dos dados do contrato;
- `CON-006` — estrutura preparada para vigência obrigatória na publicação;
- `CON-007` — separação entre situação temporal/negocial e habilitação administrativa;
- `CON-008` — RASCUNHO pode estar incompleto e publicação exige consistência;
- `CON-009` — contrato pertence a uma Gestão Local por vez;
- `CON-010` — inativação e reativação como eventos próprios;
- `CON-015` — estado ANULADO explícito;
- `CON-016` — encerramento antecipado distinto de inativação e fim natural;
- `CON-021` — CANCELADO para contrato FUTURO cancelado antes do início;
- `CON-022` — eixos independentes de ciclo de vida;
- `CON-024` — anulação registra data do ato e data efetiva separadamente;
- `CON-025` — contrato FUTURO publicado e CANCELADO não libera seu identificador;
- `EMP-001` — estrutura para fotografia de CNPJ, razão social e nome fantasia;
- `CAT-001` — Tipo de Contratação como cadastro, e não texto livre.

Alguns requisitos foram **estruturalmente preparados**, mas somente poderão ser concluídos em etapas posteriores por dependerem de composição, motor temporal, permissões excepcionais ou vínculos.

---

# 3. Alterações realizadas

## 3.1. Identificação do contrato por número + ano

A entidade `Contrato` passou a possuir:

- `numeroContrato`;
- `anoContrato`.

Foi criada restrição de unicidade composta:

`numero_contrato + ano_contrato`

O serviço valida duplicidade de número + ano em criação/edição de rascunho.

### Compatibilidade com registros antigos

O campo `anoContrato` não foi tornado `NOT NULL` no banco nesta etapa para evitar quebrar bases antigas antes da regularização/migração.

Para novos contratos, o ano passa a fazer parte do modelo canônico.

---

# 3.2. Estado negocial explícito

Foi criado o enum:

`EstadoNegocialContrato`

Valores implementados:

- `RASCUNHO`;
- `PUBLICADO`;
- `CANCELADO`;
- `ANULADO`.

A escolha de persistir `PUBLICADO` e derivar FUTURO/VIGENTE/ENCERRADO evita transformar passagem do tempo em novo fato negocial.

---

# 3.3. Situação temporal derivada

Foi criado:

`SituacaoTemporalContrato`

Valores:

- `RASCUNHO`;
- `FUTURO`;
- `VIGENTE`;
- `ENCERRADO`;
- `CANCELADO`;
- `ANULADO`.

A entidade `Contrato` implementa resolução temporal por data de referência.

Regras implementadas:

- antes do início → `FUTURO`;
- entre início e fim, inclusive → `VIGENTE`;
- após o fim → `ENCERRADO`;
- RASCUNHO, CANCELADO e ANULADO dependem do estado negocial explícito.

Não foi criado job que altere fatos negociais apenas porque o dia mudou.

---

# 3.4. Habilitação administrativa separada

Foi criado:

`HabilitacaoAdministrativaContrato`

Valores:

- `ATIVO`;
- `INATIVO`.

O campo legado `ativo` foi preservado temporariamente como **projeção administrativa**, e não mais como situação temporal do contrato.

Isso permite compatibilidade progressiva com módulos antigos que ainda consultam `getAtivo()` enquanto os serviços são migrados nas etapas correspondentes.

---

# 3.5. Contrato novo nasce como RASCUNHO

A operação:

`POST /api/contratos`

passou a criar RASCUNHO.

Um rascunho pode estar incompleto, conforme o V8.

A edição livre por:

`PUT /api/contratos/{id}`

é permitida somente enquanto o contrato estiver em RASCUNHO.

Depois de publicado, a edição livre é bloqueada e deverá ser substituída pelos eventos negociais adequados.

---

# 3.6. Descarte de rascunho

A exclusão física foi restringida.

`DELETE /api/contratos/{id}`

somente pode excluir contrato em RASCUNHO e sem fatos/vínculos associados.

São verificados:

- empregados relacionados;
- histórico de vínculos legado;
- eventos do ciclo de vida.

Contrato publicado, cancelado ou anulado não pode ser apagado fisicamente por esta operação.

---

# 3.7. Tipo de Contratação como cadastro próprio

Foi criada a entidade:

`TipoContratacao`

Com:

- descrição;
- ativo/inativo;
- Gestão Local;
- criação;
- usuário criador;
- atualização;
- controle otimista de versão.

Foram criados:

- `TipoContratacaoRepositorio`;
- `TipoContratacaoServico`;
- `TipoContratacaoControlador`;
- `TipoContratacaoDTO`.

Endpoints:

- `GET /api/tipos-contratacao`;
- `GET /api/tipos-contratacao/{id}`;
- `POST /api/tipos-contratacao`;
- `PUT /api/tipos-contratacao/{id}`.

A criação/alteração negocial desse cadastro foi limitada ao Gestor Local ativo.

---

# 3.8. Correção de isolamento entre Gestões Locais

Durante a revisão da etapa foi encontrada uma falha possível:

se o JSON de contrato omitisse `gestaoLocalId`, uma implementação incompleta poderia validar o Tipo de Contratação somente contra o ID recebido e não contra a Gestão efetivamente resolvida do usuário.

Isso foi corrigido.

Agora:

1. a Gestão Local efetiva do contrato é resolvida primeiro;
2. o Tipo de Contratação é carregado;
3. o serviço compara a Gestão do Tipo com a Gestão efetiva do Contrato;
4. Tipo de Contratação de outra Gestão é bloqueado mesmo quando `gestaoLocalId` não foi enviado no payload.

Foi adicionado teste específico para essa situação.

---

# 3.9. Snapshot dos dados da empresa

Foram acrescentados ao contrato:

- `cnpjEmpresa`;
- `razaoSocialEmpresa`;
- `nomeFantasiaEmpresa`;
- referência opcional ao `Fornecedor` legado.

A estratégia adotada foi manter a referência ao cadastro mestre, mas fotografar os valores negociais no contrato.

Isso evita que uma mudança posterior no fornecedor reescreva silenciosamente o passado do contrato.

Para registros legados sem snapshot, a leitura ainda pode usar dados do Fornecedor como compatibilidade temporária.

---

# 3.10. Snapshot do Tipo de Contratação

O campo textual legado `tipo` foi mantido como fotografia negocial.

O novo `TipoContratacao` é a referência canônica para novos contratos, mas a descrição exibida pelo contrato usa o snapshot registrado no próprio contrato.

Assim, renomear futuramente o cadastro mestre não altera silenciosamente a fotografia histórica do contrato.

---

# 3.11. Estrutura de eventos do ciclo de vida

Foi criada a entidade:

`EventoCicloVidaContrato`

Cada evento registra, conforme aplicável:

- contrato;
- tipo do evento;
- data efetiva;
- data do ato;
- data/hora de registro;
- ID do usuário;
- username;
- motivo;
- referência SEI;
- valor anterior;
- valor novo.

Foi criado o enum:

`TipoEventoCicloVidaContrato`

Com estrutura para:

- publicação;
- inativação administrativa;
- reativação administrativa;
- encerramento antecipado;
- cancelamento de contrato futuro;
- anulação;
- correção de identificador;
- transferência de Gestão Local.

A Etapa 04 ampliará essa linha com precedência, retificação, cancelamento e relações entre fatos.

---

# 3.12. Consulta dos eventos

Foi criado:

`GET /api/contratos/{id}/eventos`

para consulta da trilha de eventos do ciclo de vida do contrato dentro do escopo autorizado.

---

# 3.13. Inativação administrativa

Foi implementado:

`POST /api/contratos/{id}/inativar`

A operação:

- exige data efetiva;
- exige motivo;
- registra autor;
- registra evento;
- não altera a vigência;
- não transforma o contrato em ENCERRADO;
- não apaga vínculos;
- permite evento futuro.

Para evento futuro, a projeção atual não é alterada antes da data efetiva.

Foi criado teste específico garantindo esse comportamento.

---

# 3.14. Reativação administrativa

Foi implementado:

`POST /api/contratos/{id}/reativar`

A reativação:

- é novo evento;
- não apaga a inativação anterior;
- exige data efetiva;
- exige motivo;
- preserva histórico.

---

# 3.15. Linha temporal da habilitação administrativa

O `ContratoServico` passou a conseguir reconstruir a habilitação administrativa de um contrato por data usando os eventos de:

- inativação;
- reativação.

Eventos futuros não alteram a habilitação apresentada para hoje antes da data efetiva.

Esta resolução já é usada no DTO de Contrato.

Os módulos antigos que ainda consultam diretamente `contrato.getAtivo()` serão migrados progressivamente nas etapas de Vínculo, Frequência e Relatórios.

---

# 3.16. Cancelamento de contrato FUTURO

Foi implementado:

`POST /api/contratos/{id}/cancelar-futuro`

Somente contrato:

- publicado;
- ainda FUTURO;

pode utilizar essa operação.

O contrato passa para:

`CANCELADO`

O registro não é apagado e o número/ano continua ocupado.

---

# 3.17. Anulação

Foi implementado:

`POST /api/contratos/{id}/anular`

A operação exige nesta etapa:

- data efetiva de alcance;
- data do ato anulatório;
- motivo;
- referência SEI como fonte comprobatória.

São preservados separadamente:

- data efetiva;
- data do ato;
- data/hora de registro.

O contrato passa para:

`ANULADO`

sem exclusão física.

---

# 3.18. Encerramento antecipado

Foi implementado no serviço e exposto por:

`POST /api/contratos/{id}/encerrar-antecipadamente`

A operação:

- exige motivo;
- exige fonte comprobatória/referência SEI nesta etapa;
- exige nova data final menor que a data final atual;
- não confunde encerramento com inativação;
- registra evento antes da alteração da projeção atual.

## Proteção temporária

Como a nova entidade `VinculoContratual` somente será implantada na Etapa 06, o encerramento é atualmente bloqueado quando o contrato possui vínculos/empregados associados.

Essa decisão é intencional: é mais seguro bloquear do que encerrar pessoas silenciosamente antes da existência do mecanismo definitivo de análise de impacto e término de Vínculo.

---

# 3.19. Efetivo original publicado

Foi criado o campo:

`efetivoOriginalPublicado`

Ele será preenchido no ato de publicação quando a Etapa 03 implementar a composição contratual.

Esse campo será a base fixa da regra de redução de 25% na Etapa 05.

---

# 3.20. Metadados de publicação

Foram criados campos para:

- data/hora de publicação;
- usuário que publicou;
- username;
- referência SEI da publicação.

O preenchimento definitivo ocorrerá quando o endpoint de publicação for habilitado na Etapa 03.

---

# 3.21. Inicializador de dados

O seed existente foi adaptado para trabalhar com número + ano nos contratos de demonstração criados por ele.

Foi evitada uma refatoração ampla do seed nesta etapa para não misturar alteração negocial com mudanças não necessárias.

---

# 4. Arquivos principais adicionados

## Entidades

- `EstadoNegocialContrato.java`
- `SituacaoTemporalContrato.java`
- `HabilitacaoAdministrativaContrato.java`
- `TipoEventoCicloVidaContrato.java`
- `EventoCicloVidaContrato.java`
- `TipoContratacao.java`

## DTOs

- `AcaoCicloVidaContratoDTO.java`
- `EventoCicloVidaContratoDTO.java`
- `TipoContratacaoDTO.java`

## Repositórios

- `EventoCicloVidaContratoRepositorio.java`
- `TipoContratacaoRepositorio.java`

## Serviços

- `TipoContratacaoServico.java`

## Controllers

- `TipoContratacaoControlador.java`

## Testes

- `ContratoTemporalidadeEtapa02Test.java`
- `ContratoServicoEtapa02Test.java`

---

# 5. Arquivos principais alterados

- `Contrato.java`
- `ContratoDTO.java`
- `ContratoRepositorio.java`
- `ContratoServico.java`
- `ContratoControlador.java`
- `InicializadorDados.java`

---

# 6. Testes adicionados nesta etapa

Foram adicionados testes para validar, entre outros pontos:

- FUTURO/VIGENTE/ENCERRADO derivados pela data;
- RASCUNHO/CANCELADO/ANULADO dependentes de fato explícito;
- independência entre situação temporal e habilitação administrativa;
- restrição composta número + ano;
- novo contrato nascendo como RASCUNHO;
- bloqueio de edição livre de contrato publicado;
- cancelamento de contrato FUTURO;
- isolamento do Tipo de Contratação entre Gestões Locais;
- inativação futura sem antecipar a alteração da projeção atual.

---

# 7. Validações técnicas executadas

## 7.1. Preflight do projeto

Executado:

`./scripts/validar-backend.sh`

Resultado:

`Preflight OK: Java 17.0.11, Wrapper e scripts POSIX consistentes.`

## 7.2. Compilação técnica direcionada da Etapa 02

Foi executada compilação com `javac` das classes modificadas/adicionadas da Etapa 02 contra stubs equivalentes às dependências externas e às assinaturas do projeto.

Resultado:

`Targeted Etapa 02 compile OK`

Isso valida sintaxe, tipos e assinaturas do bloco alterado.

## 7.3. Maven completo

O `mvn test` completo não foi declarado como aprovado porque este ambiente não consegue resolver o Maven Central pelo Maven Wrapper.

O erro é de acesso/resolução externa do ambiente e não foi mascarado.

Na máquina de desenvolvimento/homologação, executar:

```bash
./mvnw clean test
./mvnw clean package
```

ou no Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

---

# 8. Decisões de compatibilidade

## 8.1. `ativo` não foi removido

Ele ainda é utilizado por módulos antigos.

Foi reinterpretado como projeção da habilitação administrativa.

A remoção imediata quebraria Frequência, Empregado e Relatórios antes das respectivas etapas de migração.

## 8.2. `tipo` textual não foi removido

Passou a funcionar como snapshot histórico, enquanto `TipoContratacao` é a referência canônica dos novos contratos.

## 8.3. `Fornecedor` não foi removido

Foi mantido como referência auxiliar/legada, mas CNPJ e razão social do contrato passam a possuir fotografia própria.

## 8.4. Contratos legados sem estado negocial

São tratados como `PUBLICADO` para compatibilidade.

Novos contratos criados pela API são explicitamente `RASCUNHO`.

---

# 9. O que deliberadamente NÃO foi concluído nesta etapa

Os itens abaixo não foram esquecidos. Eles foram adiados porque dependem de domínios posteriores.

## 9.1. Publicação definitiva

Não foi criado endpoint de publicação funcional ainda.

Motivo:

O V8 exige que a publicação valide:

- campos obrigatórios;
- empresa completa;
- Tipo de Contratação;
- fonte comprobatória;
- pelo menos uma composição;
- soma das composições igual ao efetivo total;
- ausência de combinação duplicada;
- fotografia integral da composição original.

A Composição Contratual será criada na Etapa 03.

Liberar publicação na Etapa 02 permitiria criar contrato formalmente publicado em desacordo com o requisito.

## 9.2. Correção excepcional de número/ano depois da publicação

A estrutura de evento `CORRECAO_IDENTIFICADOR` está criada.

A operação final depende de:

- permissão excepcional específica;
- registro da autorização;
- índice histórico pesquisável do identificador anterior.

A autorização excepcional será consolidada junto à camada de permissões e o versionamento histórico será concluído com o motor temporal.

## 9.3. Transferência excepcional de Gestão Local

A estrutura `TRANSFERENCIA_GESTAO_LOCAL` está preparada, mas a operação não foi habilitada.

Motivo:

O requisito exige, antes de concluir a transferência:

- composição válida na nova Gestão;
- mapeamento explícito de Lotação/Turno/Cargo quando necessário;
- preservação da Gestão anterior por período;
- autorização administrativa específica.

A Composição entra na Etapa 03 e a linha temporal completa entra na Etapa 04.

## 9.4. Retificação/cancelamento da anulação

Será tratada pelo motor de versionamento/precedência da Etapa 04.

## 9.5. Impacto de inativação retroativa em frequência

A Etapa 02 registra a linha temporal administrativa.

A geração de impactos/retificações de frequência será feita quando Frequência for integrada ao novo domínio, na Etapa 09.

## 9.6. Encerramento com vínculos existentes

Está bloqueado de forma segura enquanto a nova estrutura Pessoa → Vínculo → Alocação não existir.

Será liberado com prévia e tratamento correto após as Etapas 06 e 07.

---

# 10. Situação da Etapa 02

A Etapa 02 é considerada concluída porque entregou a infraestrutura estrutural do Contrato necessária para as próximas fases, sem criar atalhos que violem o V8.

A publicação final não é considerada pendência da Etapa 02: ela foi conscientemente deslocada para o início da Etapa 03 porque **publicação e composição são atomicamente dependentes pelo requisito funcional**.

---

# 11. Próxima etapa — Etapa 03

## Nome

**Composição Contratual + cadastros auxiliares + fechamento da Publicação**

## Objetivo

Implementar a composição explícita:

`Lotação + Turno + Cargo + Efetivo previsto`

sem gerar produto cartesiano.

---

# 12. Escopo planejado da Etapa 03

## 12.1. Composição Contratual

Criar entidade temporal própria para representar:

- Contrato;
- Lotação;
- Turno/Jornada;
- Cargo;
- efetivo previsto;
- início de validade;
- fim de validade;
- versão/histórico quando aplicável.

## 12.2. Validações

Implementar:

- combinação explícita;
- bloqueio de duplicidade no mesmo recorte temporal;
- soma das composições igual ao efetivo total;
- quantitativo zero indisponível para nova alocação;
- retorno de zero para positivo por nova versão temporal;
- validação de Gestão Local dos catálogos;
- bloqueio de catálogos incompatíveis.

## 12.3. Publicação do contrato

Depois que a composição existir, implementar:

`POST /api/contratos/{id}/publicar`

Validar atomicamente:

- número;
- ano;
- descrição;
- Tipo de Contratação;
- empresa;
- CNPJ;
- razão social;
- nome fantasia;
- início;
- fim;
- efetivo total;
- composição;
- fonte comprobatória;
- soma dos quantitativos;
- duplicidades.

Ao publicar:

- registrar evento `PUBLICACAO`;
- preencher `efetivoOriginalPublicado`;
- registrar usuário/data/hora;
- preservar snapshot integral;
- impedir retorno à edição livre;
- reservar definitivamente número + ano.

## 12.4. Catálogos auxiliares

Revisar e adaptar:

- Lotação;
- Turno/Jornada;
- Cargo;
- Tipo de Contratação.

Implementar regra de uso temporal de cadastro inativo:

- histórico continua consultável;
- retroativo pode usar catálogo que era válido naquela data;
- futuro não pode usar catálogo atualmente inativo;
- reativação preserva identidade e auditoria.

## 12.5. Excedente

Preparar cálculo/consulta por composição:

- previsto;
- quantidade vinculada;
- excedente;
- desde quando;
- justificativas.

A criação definitiva de novos Vínculos acima do previsto será integrada quando Vínculo for implementado.

---

# 13. Critério de conclusão da Etapa 03

A Etapa 03 estará concluída quando:

- composição explícita existir;
- nenhuma composição duplicada puder existir no mesmo período;
- soma das composições for validada;
- publicação puder ser executada com todas as validações do V8;
- snapshot original puder ser preservado;
- contrato publicado não puder ser editado como formulário livre;
- catálogos respeitarem escopo e uso histórico;
- testes unitários e de integração do bloco estiverem criados;
- ZIP da Etapa 03 e MD da Etapa 03 forem gerados.

---

# 14. Ordem de continuidade

```text
ETAPA 02 — CONCLUÍDA
        ↓
ETAPA 03 — COMPOSIÇÃO + PUBLICAÇÃO
        ↓
ZIP JAVA ETAPA 03
        ↓
ETAPA 04 — MOTOR TEMPORAL / VERSIONAMENTO / PRECEDÊNCIA
```

---

# 15. Resumo executivo

**Etapa 02:** concluída.  
**React:** não alterado.  
**Backend antigo:** preservado onde necessário para migração progressiva.  
**Novo núcleo contratual:** implantado.  
**Situação temporal:** derivada.  
**Habilitação administrativa:** separada.  
**Tipo de Contratação:** cadastro próprio.  
**Eventos de ciclo de vida:** implantados.  
**Rascunho:** implantado.  
**Cancelamento FUTURO:** implantado.  
**Anulação:** implantada.  
**Inativação/reativação:** implantadas como fatos.  
**Encerramento antecipado:** estrutura segura implantada.  
**Publicação:** será fechada na Etapa 03 junto da composição, conforme dependência obrigatória do V8.  
**Próxima etapa:** Composição Contratual + publicação definitiva.
