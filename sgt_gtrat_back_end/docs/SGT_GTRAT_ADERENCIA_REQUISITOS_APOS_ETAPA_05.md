# SGT/GTRAT — Revisão de Aderência ao Levantamento V8 após a Etapa 05

> **FOTOGRAFIA HISTÓRICA:** esta matriz registra a situação ao fechamento original da Etapa 05. As correções posteriores e a prontidão atual estão documentadas em `SGT_GTRAT_CORRECAO_PRE_ETAPA_06_SPRING_BOOT3_JAVA17_REV3.md`.

**Data:** 08/09/2026  
**Fonte revisada:** `LEVANTAMENTO_REQUISITOS.md` — versão 8.0, 198 requisitos canônicos  
**Código revisado:** backend acumulado das Etapas 01 a 05  
**Objetivo:** verificar, requisito por requisito, o que está efetivamente implementado, o que está parcial e o que ainda precisa ser executado nas Etapas 06–12.

## 1. Conclusão executiva

**Não, o backend inteiro ainda não implementa todos os requisitos do V8.** Isso é esperado pelo plano de 12 etapas: a Etapa 05 fecha o núcleo de Contrato + Composição + Motor Temporal + Aditamento + Redução + Remanejamento, mas ainda não conclui Pessoa/Vínculo/Alocação, documentos, frequência, permissões completas, relatórios, auditoria consolidada e testes finais.

A revisão foi deliberadamente conservadora: funcionalidade legada não é considerada aderente apenas por existir. Ela só recebe `IMPLEMENTADO` quando está compatível com a semântica do V8; quando depende de integração futura, recebe `PARCIAL` ou `PENDENTE`.

### Resultado geral

| Situação | Quantidade | Percentual |
|---|---:|---:|
| **IMPLEMENTADO** | 47 | 23,7% |
| **PARCIAL** | 50 | 25,3% |
| **PENDENTE** | 101 | 51,0% |
| **TOTAL** | **198** | **100%** |

> Estes percentuais são uma fotografia de aderência funcional, não uma estimativa linear de esforço. Um único requisito temporal pode ter custo maior que vários CRUDs.


## 1.1 Lacunas estruturais confirmadas no código atual

A revisão não marcou os blocos futuros como pendentes apenas por causa do cronograma; há incompatibilidades concretas que ainda precisam ser removidas:

- `Empregado` ainda possui diretamente `matricula`, `dataAdmissao`, `Jornada`, `Lotacao`, `Cargo` e lista de `Contrato`. Isso conflita com a separação V8 Pessoa → Vínculo → Alocação e será atacado nas Etapas 06–07.
- a frequência legada ainda contém regra `FREQUENCIA_PRAZO_EDICAO_HORAS = 8` e o `.env` contém `FREQUENCIA_MAX_RETROACTIVE_USER_DAYS=30`; portanto ela **não pode ser considerada aderente** à janela V8 de 7 dias até a Etapa 09.
- a Central de Pendências consolidada ainda não está implementada;
- documentos/anexos e classificação Pessoal/Restrito ainda não estão integrados;
- relatórios legados ainda não foram migrados para consumir integralmente a verdade temporal/versionada.

## 2. Situação por domínio

| Domínio | Total | Implementado | Parcial | Pendente |
|---|---:|---:|---:|---:|
| GOV — Governança e integridade | 2 | 1 | 1 | 0 |
| TMP — Temporalidade e reconstrução histórica | 17 | 4 | 10 | 3 |
| CON — Contrato e ciclo de vida | 25 | 13 | 8 | 4 |
| EMP — Empresa contratada | 3 | 2 | 1 | 0 |
| CAT — Cadastros auxiliares e catálogos | 9 | 0 | 4 | 5 |
| CMP — Composição contratual | 11 | 4 | 7 | 0 |
| RED — Redução e efetivo contratual | 8 | 6 | 2 | 0 |
| ADI — Aditamentos | 10 | 9 | 1 | 0 |
| REM — Remanejamentos | 7 | 6 | 1 | 0 |
| DOC — Documentos e SEI | 10 | 0 | 4 | 6 |
| COL — Pessoa / Colaborador | 12 | 0 | 0 | 12 |
| MAT — Matrícula | 5 | 0 | 0 | 5 |
| VIN — Vínculo contratual | 15 | 0 | 0 | 15 |
| ALO — Alocação | 8 | 0 | 0 | 8 |
| DES — Desligamento | 6 | 0 | 0 | 6 |
| FRE — Frequência | 19 | 0 | 0 | 19 |
| REL — Relatórios | 6 | 0 | 0 | 6 |
| AUD — Auditoria / pendências / integridade | 7 | 0 | 6 | 1 |
| PER — Perfis e permissões | 12 | 1 | 2 | 9 |
| HIS — Histórico e rastreabilidade | 6 | 1 | 3 | 2 |

## 3. Resultado específico da Etapa 05

Os domínios diretamente atacados nesta etapa ficaram assim:

- **RED:** 6/8 implementados, 2 parciais, 0 pendentes.
- **ADI:** 9/10 implementados, 1 parciais, 0 pendentes.
- **REM:** 6/7 implementados, 1 parciais, 0 pendentes.

Os itens ainda parciais nesses domínios não representam omissão acidental da Etapa 05; dependem de módulos posteriores:

- `RED-003`: o backend identifica impacto e não move pessoas, mas o tratamento completo exige Vínculo/Alocação e Central de Pendências.
- `RED-008`: fluxo excepcional já exige fundamento, SEI, confirmação e perfil técnico específico; a governança completa de concessão/auditoria da permissão será fechada em Segurança/Auditoria.
- `ADI-008`: fatos posteriores são preservados/reaplicados, porém incompatibilidades ainda precisam ser materializadas na Central de Pendências e refletidas em frequência/relatórios.
- `REM-005`: referência SEI está disponível; anexos opcionais serão integrados no módulo documental.

## 4. Correções realizadas durante esta revisão antes de fechar a Etapa 05

A revisão integral do V8 encontrou pontos que foram corrigidos no código antes do checkpoint:

1. **Revalidação real da prévia de impacto.** A confirmação não confia mais apenas em `confirmarImpacto=true`. A prévia gera um `tokenPrevia` SHA-256 a partir do estado relevante; a gravação recalcula o token sob transação e rejeita a confirmação se quantidade, ocupação, versão do contrato ou eventos considerados tiverem mudado.
2. **Serialização de operações críticas por contrato.** A confirmação de Aditamento/Remanejamento utiliza `PESSIMISTIC_WRITE` para impedir duas confirmações simultâneas sobre a mesma fotografia. O lock é apenas técnico e nunca define precedência negocial.
3. **Proteção contra repetição/reenvio.** Além do token, identificação + versão possui constraint única e o motor temporal impede cadeia inválida.
4. **Fotografia original de composição corrigida.** Na publicação, cada composição original precisa cobrir integralmente a vigência original do contrato; períodos posteriores são produzidos por fatos versionados, não por edição escondida da fotografia-base.
5. **Vigência de Aditamento validada.** A data efetiva precisa estar contida na vigência resultante do próprio fato.
6. **Remanejamento limitado à vigência reconstruída.** O fato só pode produzir efeito dentro da vigência temporal válida do contrato.
7. **Conflito cruzado Aditamento × Remanejamento.** Alterações sobre a mesma composição e data exigem precedência explícita; `MANTEM_COEXISTE` é rejeitado quando existe colisão real.

## 5. Matriz completa dos 198 requisitos

| ID | Requisito V8 | Situação após Etapa 05 | Etapa/fechamento | Avaliação |
|---|---|---|---|---|
| **GOV-003** | Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável. | **PARCIAL** | Etapas 04–05 + 06–12 | Garantido para fatos contratuais já migrados; demais domínios ainda serão versionados. |
| **GOV-004** | Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação. | **IMPLEMENTADO** | Etapas 02–03 | Rascunho não publicado pode ser descartado; publicado não volta a CRUD livre. |
| **TMP-001** | Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos. | **PENDENTE** | Etapas 06–07 | Modelo legado de Empregado ainda não foi substituído integralmente por Pessoa + Vínculo + Alocação. |
| **TMP-002** | Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo. | **PENDENTE** | Etapas 06–07 | Modelo legado de Empregado ainda não foi substituído integralmente por Pessoa + Vínculo + Alocação. |
| **TMP-003** | Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-004** | O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-005** | A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-006** | Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-007** | O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia. | **PENDENTE** | Etapas 06–07 | Modelo legado de Empregado ainda não foi substituído integralmente por Pessoa + Vínculo + Alocação. |
| **TMP-008** | Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-009** | Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente. | **PARCIAL** | Etapa 04 + Etapa 11 | Fatos posteriores são preservados; Central de Pendências ainda não existe. |
| **TMP-010** | Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada. | **IMPLEMENTADO** | Etapas 02–05 | Motor temporal contratual usa precedência explícita e deriva situação temporal. |
| **TMP-011** | Eventos da mesma data que não colidem podem coexistir. | **IMPLEMENTADO** | Etapas 02–05 | Motor temporal contratual usa precedência explícita e deriva situação temporal. |
| **TMP-012** | Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-013** | Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo. | **PARCIAL** | Etapa 05 + Etapas 06–07 | Data efetiva está correta para Aditamento/Remanejamento; vínculo/alocação/desligamento ainda não migrados. |
| **TMP-014** | Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-015** | Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência. | **PARCIAL** | Etapas 04–05 + integrações futuras | Coberto no domínio Contrato/Aditamento/Remanejamento; ainda falta propagar para Pessoa/Vínculo/Alocação/Frequência/Relatórios. |
| **TMP-016** | Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão. | **IMPLEMENTADO** | Etapas 02–05 | Motor temporal contratual usa precedência explícita e deriva situação temporal. |
| **TMP-017** | `FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito. | **IMPLEMENTADO** | Etapas 02–05 | Motor temporal contratual usa precedência explícita e deriva situação temporal. |
| **CON-001** | Número + ano identificam unicamente o contrato em todo o sistema. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-002** | Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-003** | Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável. | **PENDENTE** | Etapa 10/11 | Correção excepcional pós-publicação de número/ano com permissão e histórico pesquisável ainda não concluída. |
| **CON-004** | Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-005** | Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-006** | Data de início e fim de vigência são obrigatórias para contrato publicado. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-007** | Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-008** | Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-009** | O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-010** | Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-011** | Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-012** | Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-013** | Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período. | **PENDENTE** | Etapa 07/10 | Transferência excepcional de Gestão Local ainda não implementada. |
| **CON-014** | Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-015** | Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-016** | Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-017** | Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-018** | Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-019** | Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-020** | Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa. | **PENDENTE** | Etapas 04/11 | Retificação/cancelamento específico de uma anulação registrada incorretamente ainda não exposto como fluxo completo. |
| **CON-021** | Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-022** | A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato. | **PARCIAL** | Etapas 02–05 + integrações futuras | Núcleo contratual existe, mas impactos/pendências/vínculos/frequência ainda precisam integração. |
| **CON-023** | Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado. | **PENDENTE** | Etapa 07/10 | Transferência excepcional de Gestão Local ainda não implementada. |
| **CON-024** | Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **CON-025** | Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador. | **IMPLEMENTADO** | Etapas 02–05 | Implementado no núcleo contratual temporal atual. |
| **EMP-001** | Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada. | **IMPLEMENTADO** | Etapas 02 e 05 | Snapshot da empresa no contrato e alteração temporal por aditamento. |
| **EMP-002** | Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos. | **IMPLEMENTADO** | Etapas 02 e 05 | Snapshot da empresa no contrato e alteração temporal por aditamento. |
| **EMP-003** | Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real. | **PARCIAL** | Etapa 05 + Etapa 11 | Troca real de empresa está temporal; correção material de CNPJ ainda precisa fluxo excepcional dedicado. |
| **CAT-001** | Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre. | **PARCIAL** | Etapas 02–03 + Etapa 07/11 | Cadastros existem e preservam inativos, mas histórico nominal/auditoria temporal ainda não está completo. |
| **CAT-002** | Cargo é cadastro próprio administrado pela Gestão Local. | **PARCIAL** | Etapas 02–03 + Etapa 07/11 | Cadastros existem e preservam inativos, mas histórico nominal/auditoria temporal ainda não está completo. |
| **CAT-003** | Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos. | **PARCIAL** | Etapas 02–03 + Etapa 07/11 | Cadastros existem e preservam inativos, mas histórico nominal/auditoria temporal ainda não está completo. |
| **CAT-004** | Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual. | **PENDENTE** | Etapas 06–07/11 | Depende de histórico temporal dos catálogos, programações e motivo de desligamento. |
| **CAT-005** | Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação. | **PENDENTE** | Etapas 06–07/11 | Depende de histórico temporal dos catálogos, programações e motivo de desligamento. |
| **CAT-006** | Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria. | **PENDENTE** | Etapas 06–07/11 | Depende de histórico temporal dos catálogos, programações e motivo de desligamento. |
| **CAT-007** | Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva. | **PENDENTE** | Etapas 06–07/11 | Depende de histórico temporal dos catálogos, programações e motivo de desligamento. |
| **CAT-008** | Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita. | **PARCIAL** | Etapa 03 + Etapa 07 | Catálogo inativo é bloqueado em novas composições; uso retroativo pela validade histórica ainda não está completo. |
| **CAT-009** | Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas. | **PENDENTE** | Etapas 06–07/11 | Depende de histórico temporal dos catálogos, programações e motivo de desligamento. |
| **CMP-001** | Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático. | **IMPLEMENTADO** | Etapas 03–05 | Composição explícita, sem duplicidade, soma consistente e retorno por versão temporal. |
| **CMP-002** | Não pode haver combinação duplicada no mesmo recorte temporal. | **IMPLEMENTADO** | Etapas 03–05 | Composição explícita, sem duplicidade, soma consistente e retorno por versão temporal. |
| **CMP-003** | Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato. | **IMPLEMENTADO** | Etapas 03–05 | Composição explícita, sem duplicidade, soma consistente e retorno por versão temporal. |
| **CMP-004** | Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-005** | Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível. | **IMPLEMENTADO** | Etapas 03–05 | Composição explícita, sem duplicidade, soma consistente e retorno por versão temporal. |
| **CMP-006** | Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-007** | Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-008** | Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-009** | Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-010** | Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **CMP-011** | Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente. | **PARCIAL** | Etapas 03–05 + Etapas 06–07/11 | Projeção/impacto/excedente existem parcialmente; vínculo temporal, programações, desde-quando, justificativas e pendências ainda faltam. |
| **RED-001** | Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-002** | Limite de 25% é verificado sobre o total do contrato, não individualmente por composição. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-003** | Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados. | **PARCIAL** | Etapa 05 + Etapas 06–07/11 | Pode zerar composição respeitando o piso e exige confirmação; tratamento completo dos vínculos/pendências vem depois. |
| **RED-004** | Aumento contratual não possui limite de 25%. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-005** | Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-006** | O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-007** | Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro. | **IMPLEMENTADO** | Etapa 05 | Regra matemática e bloqueio normal implementados no backend. |
| **RED-008** | Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada. | **PARCIAL** | Etapa 05 + Etapas 10–11 | Fundamento, SEI, permissão especial, prévia/token e auditoria do fato existem; governança da concessão e auditoria reforçada global ainda faltam. |
| **ADI-001** | Contrato pode possuir vários aditamentos sem apagar registros anteriores. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-002** | Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-003** | Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-004** | Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-005** | Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-006** | Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-007** | Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-008** | Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados. | **PARCIAL** | Etapa 05 + Etapa 11 | Reaplicação/preservação existe; geração consolidada de pendência e impactos em frequência/relatórios ainda falta. |
| **ADI-009** | Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **ADI-010** | Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios. | **IMPLEMENTADO** | Etapa 05 | Implementado como fato temporal versionado e integrado ao motor de precedência. |
| **REM-001** | Remanejamento é fato próprio, identificado, datado, justificado e auditado. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **REM-002** | Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **REM-003** | Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **REM-004** | Remanejamento pode ser retificado/versionado, preservando original e cadeia completa. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **REM-005** | Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade. | **PARCIAL** | Etapa 05 + Etapa 08 | Referência SEI existe; anexo opcional entra no módulo documental. |
| **REM-006** | Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **REM-007** | Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito. | **IMPLEMENTADO** | Etapa 05 | Remanejamento temporal, soma zero, versionamento e cancelamento futuro implementados. |
| **DOC-001** | Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos. | **PARCIAL** | Etapas 03–05 + Etapa 08 | SEI é suportado e não usado como identificador único; anexos e gestão documental completa ainda faltam. |
| **DOC-002** | Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI. | **PARCIAL** | Etapas 03–05 + Etapa 08 | SEI é suportado e não usado como identificador único; anexos e gestão documental completa ainda faltam. |
| **DOC-003** | Remanejamento pode registrar SEI e mantém anexo opcional. | **PARCIAL** | Etapas 03–05 + Etapa 08 | SEI é suportado e não usado como identificador único; anexos e gestão documental completa ainda faltam. |
| **DOC-004** | Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato. | **PARCIAL** | Etapas 03–05 + Etapa 08 | SEI é suportado e não usado como identificador único; anexos e gestão documental completa ainda faltam. |
| **DOC-005** | Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **DOC-006** | Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **DOC-007** | Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **DOC-008** | Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **DOC-009** | Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **DOC-010** | Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado. | **PENDENTE** | Etapa 08/10/11 | Fluxo documental versionado, classificação e controle de acesso ainda não implementados. |
| **COL-001** | Nome e CPF são obrigatórios. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-002** | RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-003** | Endereço é mantido e opcional. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-004** | Dados bancários foram removidos do cadastro deste fluxo. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-005** | CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-006** | CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-007** | Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-009** | Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-010** | Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-011** | Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-012** | Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **COL-013** | Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **MAT-001** | Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **MAT-002** | Matrícula é opcional no vínculo e única dentro do contrato. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **MAT-003** | Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **MAT-004** | Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **MAT-005** | Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-001** | Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-002** | Antiga data de admissão é substituída por início efetivo do vínculo. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-003** | Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-004** | Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-005** | Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-006** | Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-007** | Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-008** | Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-009** | Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-010** | Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-011** | Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-012** | Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-013** | Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-014** | Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **VIN-015** | Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-001** | Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-002** | Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-003** | Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-004** | Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-005** | Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-006** | Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-007** | Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **ALO-008** | Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida. | **PENDENTE** | Etapa 07 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-001** | Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-002** | Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-003** | Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-004** | Desligamento possui motivo de catálogo, responsável e data/hora de registro. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-005** | Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **DES-006** | Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia. | **PENDENTE** | Etapa 06 | O modelo legado de Empregado não é considerado aderência V8; será substituído/refatorado na etapa indicada. |
| **FRE-001** | Frequência resolve vínculo e alocação válidos na data negocial. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-002** | Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-003** | Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-004** | Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-005** | Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-006** | Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-007** | Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-008** | Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-009** | Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-010** | Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-011** | Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-012** | Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-013** | No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-014** | Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-015** | A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-016** | Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-017** | Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-018** | Permissão de edição combina autorização atual do usuário com o escopo histórico do registro. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **FRE-019** | Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria. | **PENDENTE** | Etapa 09 | Há frequência legada, mas ela ainda não foi revalidada/migrada para Vínculo + Alocação histórica e regras V8; não é marcada como aderente. |
| **REL-001** | Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **REL-002** | Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **REL-003** | Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **REL-004** | Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **REL-005** | Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **REL-006** | Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica. | **PENDENTE** | Etapa 11 | Relatórios legados existem, porém ainda não usam integralmente a nova verdade temporal/versionada; serão reimplementados/integrados. |
| **AUD-001** | Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável. | **PARCIAL** | Etapas 04–05 + Etapa 11 | Coberto nos fatos contratuais migrados; falta consolidar auditoria em todos os demais domínios. |
| **AUD-002** | Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva. | **PARCIAL** | Etapas 04–05 + Etapa 11 | Coberto nos fatos contratuais migrados; falta consolidar auditoria em todos os demais domínios. |
| **AUD-003** | Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento. | **PARCIAL** | Etapas 04–05 + Etapa 11 | Coberto nos fatos contratuais migrados; falta consolidar auditoria em todos os demais domínios. |
| **AUD-004** | Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação. | **PARCIAL** | Etapa 05 + Etapas 06–11 | Etapa 05 adicionou token de prévia, revalidação transacional, lock e constraints; proteção deve ser propagada às demais operações críticas. |
| **AUD-005** | Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede. | **PARCIAL** | Etapa 05 + Etapas 06–11 | Etapa 05 adicionou token de prévia, revalidação transacional, lock e constraints; proteção deve ser propagada às demais operações críticas. |
| **AUD-006** | Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário. | **PENDENTE** | Etapa 11 | Central consolidada de pendências ainda não implementada. |
| **AUD-007** | Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal. | **PARCIAL** | Etapas 04–05 + Etapa 11 | Coberto nos fatos contratuais migrados; falta consolidar auditoria em todos os demais domínios. |
| **PER-001** | Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão. | **IMPLEMENTADO** | Etapa 05 | Aditamento e remanejamento exigem Gestor Local no escopo do contrato. |
| **PER-002** | Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial. | **PARCIAL** | Etapas 02–05 + Etapa 10 | Leitura/escrita contratual já restringe operações principais; matriz completa será revisada na etapa de permissões. |
| **PER-003** | Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial. | **PENDENTE** | Etapa 10 | Perfis nacionais, minimização de dados e documentos exigem revisão completa. |
| **PER-004** | Presidente atua somente em consulta nacional neste módulo. | **PARCIAL** | Etapas 02–05 + Etapa 10 | Leitura/escrita contratual já restringe operações principais; matriz completa será revisada na etapa de permissões. |
| **PER-005** | Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte. | **PENDENTE** | Etapa 10 | Perfis nacionais, minimização de dados e documentos exigem revisão completa. |
| **PER-006** | Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria. | **PENDENTE** | Etapa 10 | Perfis nacionais, minimização de dados e documentos exigem revisão completa. |
| **PER-007** | Correção excepcional de CPF é autorizada pelo Gestor Local. | **PENDENTE** | Etapa 06/10 | Correção excepcional de CPF ainda não implementada. |
| **PER-008** | Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado. | **PENDENTE** | Etapa 10/11 | Correção excepcional número/ano ainda não implementada. |
| **PER-009** | Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos. | **PENDENTE** | Etapa 10 | Perfis nacionais, minimização de dados e documentos exigem revisão completa. |
| **PER-010** | Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro. | **PENDENTE** | Etapa 09/10 | Permissão histórica da frequência depende da nova Alocação/Vínculo. |
| **PER-011** | Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica. | **PENDENTE** | Etapa 10 | Perfis nacionais, minimização de dados e documentos exigem revisão completa. |
| **PER-012** | Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial. | **PENDENTE** | Etapa 07/10 | Transferência excepcional de Gestão ainda não implementada. |
| **HIS-001** | Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos. | **PARCIAL** | Etapas 04–05 + Etapas 06–11 | Histórico contratual e consulta bitemporal existem; fotos de catálogos/pessoa/vínculo e demais domínios ainda faltam. |
| **HIS-002** | Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês. | **PENDENTE** | Etapas 06–07/11 | Depende de Vínculo e Alocação temporais. |
| **HIS-003** | Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação. | **PENDENTE** | Etapas 06–07/11 | Depende de Vínculo e Alocação temporais. |
| **HIS-004** | Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo. | **PARCIAL** | Etapas 04–05 + Etapas 06–11 | Histórico contratual e consulta bitemporal existem; fotos de catálogos/pessoa/vínculo e demais domínios ainda faltam. |
| **HIS-005** | História é a fonte canônica; projeções atuais são conveniência técnica. | **PARCIAL** | Etapas 04–05 + Etapas 06–11 | Histórico contratual e consulta bitemporal existem; fotos de catálogos/pessoa/vínculo e demais domínios ainda faltam. |
| **HIS-006** | A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores. | **IMPLEMENTADO** | Etapas 03–05 | Publicação preserva fotografia original e composição-base; eventos posteriores são deltas. |

## 6. O que falta por bloco

### Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento

Fechar principalmente `COL-*`, `MAT-*`, `VIN-*`, `DES-*` e partes de `TMP`, `CON`, `CMP`, `AUD` e `PER`. O ponto estrutural é retirar matrícula/contrato/lotação/turno/cargo do conceito de estado fixo de `Empregado` e introduzir Pessoa permanente + Vínculo temporal.

### Etapa 07 — Alocação temporal e transferência excepcional

Fechar `ALO-*`, seleção encadeada por composição, uso temporal dos catálogos, mudanças internas sem desligamento e transferência excepcional de Gestão Local.

### Etapa 08 — Documentos e SEI

Fechar `DOC-*`: anexos, versionamento de documentos, classificação Negocial/Pessoal-Restrito e preservação física/auditável.

### Etapa 09 — Frequência

Revalidar a frequência legada integralmente contra `FRE-001..019`, especialmente janela de 7 dias em `America/Sao_Paulo`, vínculo/alocação históricos, retificação formal e três dimensões de validade.

### Etapa 10 — Perfis e permissões

Fechar matriz `PER-*`, separação de Administrador técnico de responsável negocial, Gestor Nacional, minimização de dados e permissões excepcionais.

### Etapa 11 — Relatórios, auditoria e Central de Pendências

Fechar `REL-*`, `AUD-*`, pendências, consolidações, versões de relatórios e impactos retroativos.

### Etapa 12 — Testes finais e homologação técnica

Executar a matriz de aceite V8, integração MySQL, concorrência, carga, regressão, segurança, ArchUnit e `mvn clean test/package` no ambiente com acesso às dependências.

## 7. Limitação de validação do ambiente atual

O código foi validado por preflight Java 17, compilações direcionadas sem dependências externas e smoke tests puros das regras da Etapa 05. O Maven Wrapper continua impossibilitado de baixar o Maven porque o ambiente não resolve `repo.maven.apache.org`; por isso este documento **não declara** a suíte Maven completa como aprovada.

## 8. Decisão após a revisão

A **Etapa 05 pode ser fechada no escopo planejado**, com os itens transversais explicitamente rastreados para as próximas etapas. O próximo passo seguro é a **Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento**; não é recomendável iniciar React antes do fechamento das 12 etapas Java.
