# Agrupamentos 198 — candidata04

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

Corte: 2026-11-04T03:18:33.728846+00:00. JAR 6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d; selo ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79.

**Homologação parcial.** A exportação é documental e não executa produto. Os valores completos estão nas tabelas CSV e nos JSONs de rastreabilidade. Nenhum campo vazio autoriza aprovação. 33 critérios documentais, Chrome 0/24, ALO/PER, FE02 e erros do consumidor automático permanecem qualificados.

## GOV-003

Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável.

Status: **PARCIAL**. Critérios: GOV-003:001, GOV-003:002, GOV-003:003.

## GOV-004

Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação.

Status: **PARCIAL**. Critérios: GOV-004:001, GOV-004:002, GOV-004:003, GOV-004:004.

## TMP-001

Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos.

Status: **PARCIAL**. Critérios: TMP-001:001, TMP-001:002, TMP-001:003.

## TMP-002

Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo.

Status: **PARCIAL**. Critérios: TMP-002:001, TMP-002:002.

## TMP-003

Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável.

Status: **PARCIAL**. Critérios: TMP-003:001, TMP-003:002, TMP-003:003, TMP-003:004, TMP-003:005.

## TMP-004

O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica.

Status: **PARCIAL**. Critérios: TMP-004:001, TMP-004:002, TMP-004:003, TMP-004:004, TMP-004:005.

## TMP-005

A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato.

Status: **PARCIAL**. Critérios: TMP-005:001, TMP-005:002.

## TMP-006

Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida.

Status: **PARCIAL**. Critérios: TMP-006:001, TMP-006:002.

## TMP-007

O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia.

Status: **PARCIAL**. Critérios: TMP-007:001, TMP-007:002.

## TMP-008

Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente.

Status: **PARCIAL**. Critérios: TMP-008:001, TMP-008:002, TMP-008:003, TMP-008:004.

## TMP-009

Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente.

Status: **PARCIAL**. Critérios: TMP-009:001, TMP-009:002, TMP-009:003.

## TMP-010

Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada.

Status: **PARCIAL**. Critérios: TMP-010:001, TMP-010:002, TMP-010:003.

## TMP-011

Eventos da mesma data que não colidem podem coexistir.

Status: **PARCIAL**. Critérios: TMP-011:001.

## TMP-012

Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos.

Status: **PARCIAL**. Critérios: TMP-012:001, TMP-012:002.

## TMP-013

Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo.

Status: **PARCIAL**. Critérios: TMP-013:001, TMP-013:002.

## TMP-014

Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito.

Status: **PARCIAL**. Critérios: TMP-014:001, TMP-014:002.

## TMP-015

Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência.

Status: **PARCIAL**. Critérios: TMP-015:001, TMP-015:002.

## TMP-016

Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão.

Status: **PARCIAL**. Critérios: TMP-016:001, TMP-016:002, TMP-016:003.

## TMP-017

`FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito.

Status: **PARCIAL**. Critérios: TMP-017:001, TMP-017:002, TMP-017:003.

## CON-001

Número + ano identificam unicamente o contrato em todo o sistema.

Status: **PARCIAL**. Critérios: CON-001:001, CON-001:002, CON-001:003, CON-001:004.

## CON-002

Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade.

Status: **PARCIAL**. Critérios: CON-002:001, CON-002:002, CON-002:003.

## CON-003

Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável.

Status: **PARCIAL**. Critérios: CON-003:001, CON-003:002, CON-003:003, CON-003:004, CON-003:005.

## CON-004

Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização.

Status: **PARCIAL**. Critérios: CON-004:001, CON-004:002, CON-004:003.

## CON-005

Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico.

Status: **PARCIAL**. Critérios: CON-005:001, CON-005:002, CON-005:003.

## CON-006

Data de início e fim de vigência são obrigatórias para contrato publicado.

Status: **PARCIAL**. Critérios: CON-006:001, CON-006:002, CON-006:003, CON-006:004.

## CON-007

Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências.

Status: **PARCIAL**. Critérios: CON-007:001, CON-007:002, CON-007:003, CON-007:004.

## CON-008

Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto.

Status: **PARCIAL**. Critérios: CON-008:001, CON-008:002, CON-008:003, CON-008:004.

## CON-009

O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões.

Status: **PARCIAL**. Critérios: CON-009:001, CON-009:002, CON-009:003, CON-009:004.

## CON-010

Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável.

Status: **PARCIAL**. Critérios: CON-010:001, CON-010:002, CON-010:003, CON-010:004.

## CON-011

Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita.

Status: **PARCIAL**. Critérios: CON-011:001, CON-011:002, CON-011:003, CON-011:004.

## CON-012

Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa.

Status: **PARCIAL**. Critérios: CON-012:001, CON-012:002, CON-012:003, CON-012:004.

## CON-013

Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período.

Status: **PARCIAL**. Critérios: CON-013:001, CON-013:002, CON-013:003, CON-013:004, CON-013:005.

## CON-014

Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos.

Status: **PARCIAL**. Critérios: CON-014:001, CON-014:002, CON-014:003, CON-014:004.

## CON-015

Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora.

Status: **PARCIAL**. Critérios: CON-015:001, CON-015:002, CON-015:003, CON-015:004.

## CON-016

Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI.

Status: **PARCIAL**. Critérios: CON-016:001, CON-016:002, CON-016:003, CON-016:004, CON-016:005, CON-016:006.

## CON-017

Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido.

Status: **PARCIAL**. Critérios: CON-017:001, CON-017:002, CON-017:003, CON-017:004.

## CON-018

Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente.

Status: **PARCIAL**. Critérios: CON-018:001, CON-018:002, CON-018:003, CON-018:004.

## CON-019

Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização.

Status: **PARCIAL**. Critérios: CON-019:001, CON-019:002, CON-019:003.

## CON-020

Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa.

Status: **PARCIAL**. Critérios: CON-020:001, CON-020:002, CON-020:003, CON-020:004.

## CON-021

Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO.

Status: **PARCIAL**. Critérios: CON-021:001, CON-021:002, CON-021:003, CON-021:004, CON-021:005.

## CON-022

A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato.

Status: **PARCIAL**. Critérios: CON-022:001, CON-022:002, CON-022:003, CON-022:004.

## CON-023

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: CON-023:001, CON-023:002, CON-023:003, CON-023:004, CON-023:005, CON-023:006, CON-023:007, CON-023:008, CON-023:009.

## CON-024

Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação.

Status: **PARCIAL**. Critérios: CON-024:001, CON-024:002, CON-024:003, CON-024:004.

## CON-025

Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador.

Status: **PARCIAL**. Critérios: CON-025:001, CON-025:002, CON-025:003.

## EMP-001

Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada.

Status: **PARCIAL**. Critérios: EMP-001:001, EMP-001:002, EMP-001:003.

## EMP-002

Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos.

Status: **PARCIAL**. Critérios: EMP-002:001, EMP-002:002, EMP-002:003, EMP-002:004.

## EMP-003

Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real.

Status: **PARCIAL**. Critérios: EMP-003:001, EMP-003:002, EMP-003:003, EMP-003:004, EMP-003:005.

## CAT-001

Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre.

Status: **PARCIAL**. Critérios: CAT-001:001, CAT-001:002.

## CAT-002

Cargo é cadastro próprio administrado pela Gestão Local.

Status: **PARCIAL**. Critérios: CAT-002:001, CAT-002:002.

## CAT-003

Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos.

Status: **PARCIAL**. Critérios: CAT-003:001, CAT-003:002.

## CAT-004

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: CAT-004:001, CAT-004:002, CAT-004:003, CAT-004:004.

## CAT-005

Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação.

Status: **PARCIAL**. Critérios: CAT-005:001, CAT-005:002.

## CAT-006

Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria.

Status: **PARCIAL**. Critérios: CAT-006:001, CAT-006:002.

## CAT-007

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: CAT-007:001, CAT-007:002, CAT-007:003, CAT-007:004, CAT-007:005, CAT-007:006, CAT-007:007, CAT-007:008.

## CAT-008

Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita.

Status: **PARCIAL**. Critérios: CAT-008:001, CAT-008:002.

## CAT-009

Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas.

Status: **PARCIAL**. Critérios: CAT-009:001, CAT-009:002.

## CMP-001

Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático.

Status: **PARCIAL**. Critérios: CMP-001:001, CMP-001:002, CMP-001:003.

## CMP-002

Não pode haver combinação duplicada no mesmo recorte temporal.

Status: **PARCIAL**. Critérios: CMP-002:001, CMP-002:002, CMP-002:003, CMP-002:004.

## CMP-003

Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato.

Status: **PARCIAL**. Critérios: CMP-003:001, CMP-003:002, CMP-003:003, CMP-003:004, CMP-003:005.

## CMP-004

Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente.

Status: **PARCIAL**. Critérios: CMP-004:001, CMP-004:002, CMP-004:003.

## CMP-005

Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível.

Status: **PARCIAL**. Critérios: CMP-005:001, CMP-005:002, CMP-005:003, CMP-005:004.

## CMP-006

Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida.

Status: **PARCIAL**. Critérios: CMP-006:001, CMP-006:002, CMP-006:003, CMP-006:004.

## CMP-007

Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história.

Status: **PARCIAL**. Critérios: CMP-007:001, CMP-007:002, CMP-007:003, CMP-007:004.

## CMP-008

Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo.

Status: **PARCIAL**. Critérios: CMP-008:001, CMP-008:002, CMP-008:003.

## CMP-009

Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria.

Status: **PARCIAL**. Critérios: CMP-009:001, CMP-009:002, CMP-009:003, CMP-009:004.

## CMP-010

Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente.

Status: **PARCIAL**. Critérios: CMP-010:001, CMP-010:002, CMP-010:003.

## CMP-011

Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente.

Status: **PARCIAL**. Critérios: CMP-011:001, CMP-011:002, CMP-011:003, CMP-011:004.

## RED-001

Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas.

Status: **PARCIAL**. Critérios: RED-001:001, RED-001:002, RED-001:003.

## RED-002

Limite de 25% é verificado sobre o total do contrato, não individualmente por composição.

Status: **PARCIAL**. Critérios: RED-002:001, RED-002:002, RED-002:003.

## RED-003

Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados.

Status: **PARCIAL**. Critérios: RED-003:001, RED-003:002, RED-003:003.

## RED-004

Aumento contratual não possui limite de 25%.

Status: **PARCIAL**. Critérios: RED-004:001, RED-004:002, RED-004:003.

## RED-005

Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada.

Status: **PARCIAL**. Critérios: RED-005:001, RED-005:002, RED-005:003.

## RED-006

O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo.

Status: **PARCIAL**. Critérios: RED-006:001, RED-006:002, RED-006:003, RED-006:004, RED-006:005.

## RED-007

Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro.

Status: **PARCIAL**. Critérios: RED-007:001, RED-007:002, RED-007:003, RED-007:004.

## RED-008

Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada.

Status: **PARCIAL**. Critérios: RED-008:001, RED-008:002, RED-008:003, RED-008:004, RED-008:005.

## ADI-001

Contrato pode possuir vários aditamentos sem apagar registros anteriores.

Status: **PARCIAL**. Critérios: ADI-001:001, ADI-001:002, ADI-001:003.

## ADI-002

Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória.

Status: **PARCIAL**. Critérios: ADI-002:001, ADI-002:002, ADI-002:003, ADI-002:004.

## ADI-003

Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante.

Status: **PARCIAL**. Critérios: ADI-003:001, ADI-003:002.

## ADI-004

Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto.

Status: **PARCIAL**. Critérios: ADI-004:001, ADI-004:002, ADI-004:003, ADI-004:004.

## ADI-005

Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica.

Status: **PARCIAL**. Critérios: ADI-005:001, ADI-005:002, ADI-005:003, ADI-005:004.

## ADI-006

Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data.

Status: **PARCIAL**. Critérios: ADI-006:001, ADI-006:002, ADI-006:003, ADI-006:004.

## ADI-007

Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada.

Status: **PARCIAL**. Critérios: ADI-007:001, ADI-007:002, ADI-007:003, ADI-007:004.

## ADI-008

Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados.

Status: **PARCIAL**. Critérios: ADI-008:001, ADI-008:002, ADI-008:003, ADI-008:004.

## ADI-009

Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação.

Status: **PARCIAL**. Critérios: ADI-009:001, ADI-009:002, ADI-009:003, ADI-009:004.

## ADI-010

Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios.

Status: **PARCIAL**. Critérios: ADI-010:001, ADI-010:002, ADI-010:003.

## REM-001

Remanejamento é fato próprio, identificado, datado, justificado e auditado.

Status: **PARCIAL**. Critérios: REM-001:001, REM-001:002, REM-001:003.

## REM-002

Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero.

Status: **PARCIAL**. Critérios: REM-002:001, REM-002:002, REM-002:003, REM-002:004.

## REM-003

Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento.

Status: **PARCIAL**. Critérios: REM-003:001, REM-003:002, REM-003:003, REM-003:004.

## REM-004

Remanejamento pode ser retificado/versionado, preservando original e cadeia completa.

Status: **PARCIAL**. Critérios: REM-004:001, REM-004:002, REM-004:003, REM-004:004.

## REM-005

Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade.

Status: **PARCIAL**. Critérios: REM-005:001, REM-005:002, REM-005:003.

## REM-006

Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação.

Status: **PARCIAL**. Critérios: REM-006:001, REM-006:002, REM-006:003.

## REM-007

Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito.

Status: **PARCIAL**. Critérios: REM-007:001, REM-007:002, REM-007:003, REM-007:004, REM-007:005.

## DOC-001

Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos.

Status: **PARCIAL**. Critérios: DOC-001:001, DOC-001:002, DOC-001:003.

## DOC-002

Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI.

Status: **PARCIAL**. Critérios: DOC-002:001, DOC-002:002, DOC-002:003.

## DOC-003

Remanejamento pode registrar SEI e mantém anexo opcional.

Status: **PARCIAL**. Critérios: DOC-003:001, DOC-003:002, DOC-003:003.

## DOC-004

Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato.

Status: **PARCIAL**. Critérios: DOC-004:001, DOC-004:002.

## DOC-005

Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior.

Status: **PARCIAL**. Critérios: DOC-005:001, DOC-005:002, DOC-005:003, DOC-005:004.

## DOC-006

Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI.

Status: **PARCIAL**. Critérios: DOC-006:001, DOC-006:002, DOC-006:003.

## DOC-007

Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável.

Status: **PARCIAL**. Critérios: DOC-007:001, DOC-007:002, DOC-007:003.

## DOC-008

Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo.

Status: **PARCIAL**. Critérios: DOC-008:001, DOC-008:002, DOC-008:003.

## DOC-009

Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito.

Status: **PARCIAL**. Critérios: DOC-009:001, DOC-009:002, DOC-009:003, DOC-009:004.

## DOC-010

Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado.

Status: **PARCIAL**. Critérios: DOC-010:001, DOC-010:002, DOC-010:003, DOC-010:004, DOC-010:005.

## COL-001

Nome e CPF são obrigatórios.

Status: **PARCIAL**. Critérios: COL-001:001, COL-001:002.

## COL-002

RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais.

Status: **PARCIAL**. Critérios: COL-002:001, COL-002:002.

## COL-003

Endereço é mantido e opcional.

Status: **PARCIAL**. Critérios: COL-003:001, COL-003:002.

## COL-004

Dados bancários foram removidos do cadastro deste fluxo.

Status: **PARCIAL**. Critérios: COL-004:001.

## COL-005

CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface.

Status: **PARCIAL**. Critérios: COL-005:001, COL-005:002, COL-005:003.

## COL-006

CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF.

Status: **PARCIAL**. Critérios: COL-006:001, COL-006:002, COL-006:003, COL-006:004.

## COL-007

Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente.

Status: **PARCIAL**. Critérios: COL-007:001, COL-007:002.

## COL-009

Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa.

Status: **PARCIAL**. Critérios: COL-009:001, COL-009:002.

## COL-010

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: COL-010:001, COL-010:002, COL-010:003, COL-010:004.

## COL-011

Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita.

Status: **PARCIAL**. Critérios: COL-011:001, COL-011:002.

## COL-012

Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época.

Status: **PARCIAL**. Critérios: COL-012:001, COL-012:002.

## COL-013

Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa.

Status: **PARCIAL**. Critérios: COL-013:001, COL-013:002.

## MAT-001

Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa.

Status: **PARCIAL**. Critérios: MAT-001:001, MAT-001:002.

## MAT-002

Matrícula é opcional no vínculo e única dentro do contrato.

Status: **PARCIAL**. Critérios: MAT-002:001, MAT-002:002.

## MAT-003

Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada.

Status: **PARCIAL**. Critérios: MAT-003:001, MAT-003:002, MAT-003:003.

## MAT-004

Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula.

Status: **PARCIAL**. Critérios: MAT-004:001, MAT-004:002.

## MAT-005

Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida.

Status: **PARCIAL**. Critérios: MAT-005:001, MAT-005:002, MAT-005:003.

## VIN-001

Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional.

Status: **PARCIAL**. Critérios: VIN-001:001, VIN-001:002.

## VIN-002

Antiga data de admissão é substituída por início efetivo do vínculo.

Status: **PARCIAL**. Critérios: VIN-002:001, VIN-002:002.

## VIN-003

Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes.

Status: **PARCIAL**. Critérios: VIN-003:001, VIN-003:002.

## VIN-004

Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo.

Status: **PARCIAL**. Critérios: VIN-004:001, VIN-004:002.

## VIN-005

Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva.

Status: **PARCIAL**. Critérios: VIN-005:001, VIN-005:002.

## VIN-006

Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido.

Status: **PARCIAL**. Critérios: VIN-006:001, VIN-006:002.

## VIN-007

Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente.

Status: **PARCIAL**. Critérios: VIN-007:001, VIN-007:002.

## VIN-008

Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa.

Status: **PARCIAL**. Critérios: VIN-008:001, VIN-008:002.

## VIN-009

Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior.

Status: **PARCIAL**. Critérios: VIN-009:001, VIN-009:002.

## VIN-010

Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local.

Status: **PARCIAL**. Critérios: VIN-010:001, VIN-010:002.

## VIN-011

Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência.

Status: **PARCIAL**. Critérios: VIN-011:001, VIN-011:002.

## VIN-012

Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita.

Status: **PARCIAL**. Critérios: VIN-012:001, VIN-012:002.

## VIN-013

Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo.

Status: **PARCIAL**. Critérios: VIN-013:001, VIN-013:002.

## VIN-014

Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores.

Status: **PARCIAL**. Critérios: VIN-014:001, VIN-014:002, VIN-014:003, VIN-014:004.

## VIN-015

Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria.

Status: **PARCIAL**. Critérios: VIN-015:001, VIN-015:002.

## ALO-001

Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo.

Status: **PARCIAL**. Critérios: ALO-001:001, ALO-001:002.

## ALO-002

Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação.

Status: **PARCIAL**. Critérios: ALO-002:001, ALO-002:002.

## ALO-003

Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades.

Status: **PARCIAL**. Critérios: ALO-003:001, ALO-003:002.

## ALO-004

Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior.

Status: **PARCIAL**. Critérios: ALO-004:001, ALO-004:002.

## ALO-005

Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar.

Status: **PARCIAL**. Critérios: ALO-005:001, ALO-005:002.

## ALO-006

Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data.

Status: **PARCIAL**. Critérios: ALO-006:001, ALO-006:002.

## ALO-007

Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida.

Status: **PARCIAL**. Critérios: ALO-007:001, ALO-007:002, ALO-007:003.

## ALO-008

Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida.

Status: **PARCIAL**. Critérios: ALO-008:001, ALO-008:002.

## DES-001

Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato.

Status: **PARCIAL**. Critérios: DES-001:001.

## DES-002

Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte.

Status: **PARCIAL**. Critérios: DES-002:001, DES-002:002.

## DES-003

Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado.

Status: **PARCIAL**. Critérios: DES-003:001, DES-003:002.

## DES-004

Desligamento possui motivo de catálogo, responsável e data/hora de registro.

Status: **PARCIAL**. Critérios: DES-004:001, DES-004:002.

## DES-005

Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição.

Status: **PARCIAL**. Critérios: DES-005:001, DES-005:002, DES-005:003.

## DES-006

Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia.

Status: **PARCIAL**. Critérios: DES-006:001, DES-006:002.

## FRE-001

Frequência resolve vínculo e alocação válidos na data negocial.

Status: **PARCIAL**. Critérios: FRE-001:001, FRE-001:002, FRE-001:003, FRE-001:004, FRE-001:005.

## FRE-002

Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo.

Status: **PARCIAL**. Critérios: FRE-002:001, FRE-002:002.

## FRE-003

Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00.

Status: **PARCIAL**. Critérios: FRE-003:001, FRE-003:002.

## FRE-004

Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora.

Status: **PARCIAL**. Critérios: FRE-004:001, FRE-004:002.

## FRE-005

Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente.

Status: **PARCIAL**. Critérios: FRE-005:001, FRE-005:002.

## FRE-006

Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa.

Status: **PARCIAL**. Critérios: FRE-006:001, FRE-006:002.

## FRE-007

Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria.

Status: **PARCIAL**. Critérios: FRE-007:001.

## FRE-008

Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis.

Status: **PARCIAL**. Critérios: FRE-008:001, FRE-008:002, FRE-008:003, FRE-008:004.

## FRE-009

Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada.

Status: **PARCIAL**. Critérios: FRE-009:001, FRE-009:002, FRE-009:003.

## FRE-010

Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores.

Status: **PARCIAL**. Critérios: FRE-010:001, FRE-010:002.

## FRE-011

Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências.

Status: **PARCIAL**. Critérios: FRE-011:001, FRE-011:002, FRE-011:003, FRE-011:004, FRE-011:005.

## FRE-012

Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada.

Status: **PARCIAL**. Critérios: FRE-012:001, FRE-012:002.

## FRE-013

No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação.

Status: **PARCIAL**. Critérios: FRE-013:001, FRE-013:002.

## FRE-014

Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional.

Status: **PARCIAL**. Critérios: FRE-014:001, FRE-014:002.

## FRE-015

A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador.

Status: **PARCIAL**. Critérios: FRE-015:001, FRE-015:002, FRE-015:003, FRE-015:004, FRE-015:005.

## FRE-016

Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original.

Status: **PARCIAL**. Critérios: FRE-016:001, FRE-016:002.

## FRE-017

Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial.

Status: **PARCIAL**. Critérios: FRE-017:001, FRE-017:002, FRE-017:003, FRE-017:004, FRE-017:005.

## FRE-018

Permissão de edição combina autorização atual do usuário com o escopo histórico do registro.

Status: **PARCIAL**. Critérios: FRE-018:001, FRE-018:002, FRE-018:003, FRE-018:004, FRE-018:005.

## FRE-019

Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria.

Status: **PARCIAL**. Critérios: FRE-019:001, FRE-019:002, FRE-019:003, FRE-019:004.

## REL-001

Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores.

Status: **PARCIAL**. Critérios: REL-001:001, REL-001:002, REL-001:003, REL-001:004, REL-001:005, REL-001:006, REL-001:007, REL-001:008.

## REL-002

Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração.

Status: **PARCIAL**. Critérios: REL-002:001, REL-002:002.

## REL-003

Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro.

Status: **PARCIAL**. Critérios: REL-003:001, REL-003:002.

## REL-004

Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão.

Status: **PARCIAL**. Critérios: REL-004:001, REL-004:002, REL-004:003.

## REL-005

Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada.

Status: **PARCIAL**. Critérios: REL-005:001, REL-005:002, REL-005:003.

## REL-006

Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica.

Status: **PARCIAL**. Critérios: REL-006:001, REL-006:002, REL-006:003.

## AUD-001

Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável.

Status: **PARCIAL**. Critérios: AUD-001:001, AUD-001:002, AUD-001:003, AUD-001:004, AUD-001:005.

## AUD-002

Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva.

Status: **PARCIAL**. Critérios: AUD-002:001, AUD-002:002, AUD-002:003, AUD-002:004.

## AUD-003

Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento.

Status: **PARCIAL**. Critérios: AUD-003:001, AUD-003:002, AUD-003:003, AUD-003:004.

## AUD-004

Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação.

Status: **PARCIAL**. Critérios: AUD-004:001, AUD-004:002, AUD-004:003, AUD-004:004.

## AUD-005

Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede.

Status: **PARCIAL**. Critérios: AUD-005:001, AUD-005:002, AUD-005:003, AUD-005:004, AUD-005:005.

## AUD-006

Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário.

Status: **PARCIAL**. Critérios: AUD-006:001, AUD-006:002, AUD-006:003, AUD-006:004, AUD-006:005, AUD-006:006.

## AUD-007

Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal.

Status: **PARCIAL**. Critérios: AUD-007:001, AUD-007:002, AUD-007:003, AUD-007:004.

## PER-001

Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão.

Status: **PARCIAL**. Critérios: PER-001:001, PER-001:002, PER-001:003, PER-001:004, PER-001:005.

## PER-002

Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial.

Status: **PARCIAL**. Critérios: PER-002:001, PER-002:002, PER-002:003, PER-002:004.

## PER-003

Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial.

Status: **PARCIAL**. Critérios: PER-003:001, PER-003:002, PER-003:003.

## PER-004

Presidente atua somente em consulta nacional neste módulo.

Status: **PARCIAL**. Critérios: PER-004:001, PER-004:002, PER-004:003, PER-004:004.

## PER-005

Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte.

Status: **PARCIAL**. Critérios: PER-005:001, PER-005:002, PER-005:003, PER-005:004.

## PER-006

Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria.

Status: **PARCIAL**. Critérios: PER-006:001, PER-006:002, PER-006:003, PER-006:004.

## PER-007

Correção excepcional de CPF é autorizada pelo Gestor Local.

Status: **PARCIAL**. Critérios: PER-007:001, PER-007:002, PER-007:003.

## PER-008

Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado.

Status: **PARCIAL**. Critérios: PER-008:001, PER-008:002, PER-008:003.

## PER-009

Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos.

Status: **PARCIAL**. Critérios: PER-009:001, PER-009:002, PER-009:003, PER-009:004.

## PER-010

Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro.

Status: **PARCIAL**. Critérios: PER-010:001, PER-010:002, PER-010:003, PER-010:004, PER-010:005.

## PER-011

Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica.

Status: **PARCIAL**. Critérios: PER-011:001, PER-011:002, PER-011:003, PER-011:004.

## PER-012

Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial.

Status: **PARCIAL**. Critérios: PER-012:001, PER-012:002, PER-012:003.

## HIS-001

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: HIS-001:001, HIS-001:002, HIS-001:003, HIS-001:004.

## HIS-002

Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês.

Status: **PARCIAL**. Critérios: HIS-002:001, HIS-002:002.

## HIS-003

Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação.

Status: **PARCIAL**. Critérios: HIS-003:001, HIS-003:002.

## HIS-004

Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo.

Status: **PARCIAL**. Critérios: HIS-004:001, HIS-004:002.

## HIS-005

História é a fonte canônica; projeções atuais são conveniência técnica.

Status: **PARCIAL**. Critérios: HIS-005:001, HIS-005:002, HIS-005:003.

## HIS-006

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

Status: **BLOQUEADO POR DOCUMENTAÇÃO**. Critérios: HIS-006:001, HIS-006:002, HIS-006:003, HIS-006:004.

