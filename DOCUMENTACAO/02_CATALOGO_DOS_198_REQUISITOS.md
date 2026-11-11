# Levantamento dos 198 requisitos atualizados — enunciados consolidados

> **Corte histórico preservado.** Para implementação/aceite atuais, aplique o [suplemento da janela configurável/padrão30](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) e o [plano de09/11](CODEX/CAMPANHA_20261109/PLANO.md). Os estados históricos abaixo não são aprovação atual.

Fonte: `MATRIZ_198_GRUPOS_NOVA.md` (corte documental C03 de 04/11/2026). Este índice transcreve os enunciados por ID e respectivo status; a decomposição em 635 critérios, predicados e evidências permanece na matriz original, incluída neste ZIP. A classificação `PARCIAL` não equivale a reprovação de cada predicado técnico e não equivale à homologação final.

## Grupo GOV — 2 requisitos

**GOV-003** — Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável. **[Situação: PARCIAL]**

**GOV-004** — Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação. **[Situação: PARCIAL]**

## Grupo TMP — 17 requisitos

**TMP-001** — Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos. **[Situação: PARCIAL]**

**TMP-002** — Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo. **[Situação: PARCIAL]**

**TMP-003** — Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável. **[Situação: PARCIAL]**

**TMP-004** — O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica. **[Situação: PARCIAL]**

**TMP-005** — A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato. **[Situação: PARCIAL]**

**TMP-006** — Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida. **[Situação: PARCIAL]**

**TMP-007** — O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia. **[Situação: PARCIAL]**

**TMP-008** — Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente. **[Situação: PARCIAL]**

**TMP-009** — Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente. **[Situação: PARCIAL]**

**TMP-010** — Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada. **[Situação: PARCIAL]**

**TMP-011** — Eventos da mesma data que não colidem podem coexistir. **[Situação: PARCIAL]**

**TMP-012** — Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos. **[Situação: PARCIAL]**

**TMP-013** — Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo. **[Situação: PARCIAL]**

**TMP-014** — Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito. **[Situação: PARCIAL]**

**TMP-015** — Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência. **[Situação: PARCIAL]**

**TMP-016** — Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão. **[Situação: PARCIAL]**

**TMP-017** — `FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito. **[Situação: PARCIAL]**

## Grupo CON — 25 requisitos

**CON-001** — Número + ano identificam unicamente o contrato em todo o sistema. **[Situação: PARCIAL]**

**CON-002** — Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade. **[Situação: PARCIAL]**

**CON-003** — Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável. **[Situação: PARCIAL]**

**CON-004** — Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização. **[Situação: PARCIAL]**

**CON-005** — Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico. **[Situação: PARCIAL]**

**CON-006** — Data de início e fim de vigência são obrigatórias para contrato publicado. **[Situação: PARCIAL]**

**CON-007** — Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências. **[Situação: PARCIAL]**

**CON-008** — Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto. **[Situação: PARCIAL]**

**CON-009** — O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões. **[Situação: PARCIAL]**

**CON-010** — Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável. **[Situação: PARCIAL]**

**CON-011** — Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita. **[Situação: PARCIAL]**

**CON-012** — Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa. **[Situação: PARCIAL]**

**CON-013** — Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período. **[Situação: PARCIAL]**

**CON-014** — Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos. **[Situação: PARCIAL]**

**CON-015** — Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora. **[Situação: PARCIAL]**

**CON-016** — Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI. **[Situação: PARCIAL]**

**CON-017** — Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido. **[Situação: PARCIAL]**

**CON-018** — Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente. **[Situação: PARCIAL]**

**CON-019** — Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização. **[Situação: PARCIAL]**

**CON-020** — Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa. **[Situação: PARCIAL]**

**CON-021** — Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO. **[Situação: PARCIAL]**

**CON-022** — A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato. **[Situação: PARCIAL]**

**CON-023** — Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

**CON-024** — Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação. **[Situação: PARCIAL]**

**CON-025** — Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador. **[Situação: PARCIAL]**

## Grupo EMP — 3 requisitos

**EMP-001** — Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada. **[Situação: PARCIAL]**

**EMP-002** — Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos. **[Situação: PARCIAL]**

**EMP-003** — Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real. **[Situação: PARCIAL]**

## Grupo CAT — 9 requisitos

**CAT-001** — Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre. **[Situação: PARCIAL]**

**CAT-002** — Cargo é cadastro próprio administrado pela Gestão Local. **[Situação: PARCIAL]**

**CAT-003** — Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos. **[Situação: PARCIAL]**

**CAT-004** — Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

**CAT-005** — Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação. **[Situação: PARCIAL]**

**CAT-006** — Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria. **[Situação: PARCIAL]**

**CAT-007** — Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

**CAT-008** — Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita. **[Situação: PARCIAL]**

**CAT-009** — Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas. **[Situação: PARCIAL]**

## Grupo CMP — 11 requisitos

**CMP-001** — Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático. **[Situação: PARCIAL]**

**CMP-002** — Não pode haver combinação duplicada no mesmo recorte temporal. **[Situação: PARCIAL]**

**CMP-003** — Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato. **[Situação: PARCIAL]**

**CMP-004** — Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente. **[Situação: PARCIAL]**

**CMP-005** — Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível. **[Situação: PARCIAL]**

**CMP-006** — Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida. **[Situação: PARCIAL]**

**CMP-007** — Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história. **[Situação: PARCIAL]**

**CMP-008** — Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo. **[Situação: PARCIAL]**

**CMP-009** — Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria. **[Situação: PARCIAL]**

**CMP-010** — Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente. **[Situação: PARCIAL]**

**CMP-011** — Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente. **[Situação: PARCIAL]**

## Grupo RED — 8 requisitos

**RED-001** — Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas. **[Situação: PARCIAL]**

**RED-002** — Limite de 25% é verificado sobre o total do contrato, não individualmente por composição. **[Situação: PARCIAL]**

**RED-003** — Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados. **[Situação: PARCIAL]**

**RED-004** — Aumento contratual não possui limite de 25%. **[Situação: PARCIAL]**

**RED-005** — Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada. **[Situação: PARCIAL]**

**RED-006** — O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo. **[Situação: PARCIAL]**

**RED-007** — Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro. **[Situação: PARCIAL]**

**RED-008** — Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada. **[Situação: PARCIAL]**

## Grupo ADI — 10 requisitos

**ADI-001** — Contrato pode possuir vários aditamentos sem apagar registros anteriores. **[Situação: PARCIAL]**

**ADI-002** — Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória. **[Situação: PARCIAL]**

**ADI-003** — Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante. **[Situação: PARCIAL]**

**ADI-004** — Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto. **[Situação: PARCIAL]**

**ADI-005** — Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica. **[Situação: PARCIAL]**

**ADI-006** — Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data. **[Situação: PARCIAL]**

**ADI-007** — Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada. **[Situação: PARCIAL]**

**ADI-008** — Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados. **[Situação: PARCIAL]**

**ADI-009** — Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação. **[Situação: PARCIAL]**

**ADI-010** — Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios. **[Situação: PARCIAL]**

## Grupo REM — 7 requisitos

**REM-001** — Remanejamento é fato próprio, identificado, datado, justificado e auditado. **[Situação: PARCIAL]**

**REM-002** — Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero. **[Situação: PARCIAL]**

**REM-003** — Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento. **[Situação: PARCIAL]**

**REM-004** — Remanejamento pode ser retificado/versionado, preservando original e cadeia completa. **[Situação: PARCIAL]**

**REM-005** — Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade. **[Situação: PARCIAL]**

**REM-006** — Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação. **[Situação: PARCIAL]**

**REM-007** — Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito. **[Situação: PARCIAL]**

## Grupo DOC — 10 requisitos

**DOC-001** — Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos. **[Situação: PARCIAL]**

**DOC-002** — Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI. **[Situação: PARCIAL]**

**DOC-003** — Remanejamento pode registrar SEI e mantém anexo opcional. **[Situação: PARCIAL]**

**DOC-004** — Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato. **[Situação: PARCIAL]**

**DOC-005** — Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior. **[Situação: PARCIAL]**

**DOC-006** — Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI. **[Situação: PARCIAL]**

**DOC-007** — Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável. **[Situação: PARCIAL]**

**DOC-008** — Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo. **[Situação: PARCIAL]**

**DOC-009** — Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito. **[Situação: PARCIAL]**

**DOC-010** — Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado. **[Situação: PARCIAL]**

## Grupo COL — 12 requisitos

**COL-001** — Nome e CPF são obrigatórios. **[Situação: PARCIAL]**

**COL-002** — RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais. **[Situação: PARCIAL]**

**COL-003** — Endereço é mantido e opcional. **[Situação: PARCIAL]**

**COL-004** — Dados bancários foram removidos do cadastro deste fluxo. **[Situação: PARCIAL]**

**COL-005** — CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface. **[Situação: PARCIAL]**

**COL-006** — CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF. **[Situação: PARCIAL]**

**COL-007** — Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente. **[Situação: PARCIAL]**

**COL-009** — Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa. **[Situação: PARCIAL]**

**COL-010** — Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

**COL-011** — Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita. **[Situação: PARCIAL]**

**COL-012** — Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época. **[Situação: PARCIAL]**

**COL-013** — Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa. **[Situação: PARCIAL]**

## Grupo MAT — 5 requisitos

**MAT-001** — Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa. **[Situação: PARCIAL]**

**MAT-002** — Matrícula é opcional no vínculo e única dentro do contrato. **[Situação: PARCIAL]**

**MAT-003** — Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada. **[Situação: PARCIAL]**

**MAT-004** — Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula. **[Situação: PARCIAL]**

**MAT-005** — Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida. **[Situação: PARCIAL]**

## Grupo VIN — 15 requisitos

**VIN-001** — Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional. **[Situação: PARCIAL]**

**VIN-002** — Antiga data de admissão é substituída por início efetivo do vínculo. **[Situação: PARCIAL]**

**VIN-003** — Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes. **[Situação: PARCIAL]**

**VIN-004** — Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo. **[Situação: PARCIAL]**

**VIN-005** — Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva. **[Situação: PARCIAL]**

**VIN-006** — Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido. **[Situação: PARCIAL]**

**VIN-007** — Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente. **[Situação: PARCIAL]**

**VIN-008** — Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa. **[Situação: PARCIAL]**

**VIN-009** — Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior. **[Situação: PARCIAL]**

**VIN-010** — Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local. **[Situação: PARCIAL]**

**VIN-011** — Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência. **[Situação: PARCIAL]**

**VIN-012** — Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita. **[Situação: PARCIAL]**

**VIN-013** — Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo. **[Situação: PARCIAL]**

**VIN-014** — Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores. **[Situação: PARCIAL]**

**VIN-015** — Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria. **[Situação: PARCIAL]**

## Grupo ALO — 8 requisitos

**ALO-001** — Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo. **[Situação: PARCIAL]**

**ALO-002** — Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação. **[Situação: PARCIAL]**

**ALO-003** — Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades. **[Situação: PARCIAL]**

**ALO-004** — Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior. **[Situação: PARCIAL]**

**ALO-005** — Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar. **[Situação: PARCIAL]**

**ALO-006** — Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data. **[Situação: PARCIAL]**

**ALO-007** — Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida. **[Situação: PARCIAL]**

**ALO-008** — Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida. **[Situação: PARCIAL]**

## Grupo DES — 6 requisitos

**DES-001** — Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato. **[Situação: PARCIAL]**

**DES-002** — Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte. **[Situação: PARCIAL]**

**DES-003** — Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado. **[Situação: PARCIAL]**

**DES-004** — Desligamento possui motivo de catálogo, responsável e data/hora de registro. **[Situação: PARCIAL]**

**DES-005** — Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição. **[Situação: PARCIAL]**

**DES-006** — Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia. **[Situação: PARCIAL]**

## Grupo FRE — 19 requisitos

**FRE-001** — Frequência resolve vínculo e alocação válidos na data negocial. **[Situação: PARCIAL]**

**FRE-002** — Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo. **[Situação: PARCIAL]**

**FRE-003** — Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00. **[Situação: PARCIAL]**

**FRE-004** — Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora. **[Situação: PARCIAL]**

**FRE-005** — Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente. **[Situação: PARCIAL]**

**FRE-006** — Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa. **[Situação: PARCIAL]**

**FRE-007** — Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria. **[Situação: PARCIAL]**

**FRE-008** — Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis. **[Situação: PARCIAL]**

**FRE-009** — Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada. **[Situação: PARCIAL]**

**FRE-010** — Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores. **[Situação: PARCIAL]**

**FRE-011** — Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências. **[Situação: PARCIAL]**

**FRE-012** — Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada. **[Situação: PARCIAL]**

**FRE-013** — No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação. **[Situação: PARCIAL]**

**FRE-014** — Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional. **[Situação: PARCIAL]**

**FRE-015** — A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador. **[Situação: PARCIAL]**

**FRE-016** — Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original. **[Situação: PARCIAL]**

**FRE-017** — Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial. **[Situação: PARCIAL]**

**FRE-018** — Permissão de edição combina autorização atual do usuário com o escopo histórico do registro. **[Situação: PARCIAL]**

**FRE-019** — Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria. **[Situação: PARCIAL]**

## Grupo REL — 6 requisitos

**REL-001** — Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores. **[Situação: PARCIAL]**

**REL-002** — Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração. **[Situação: PARCIAL]**

**REL-003** — Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro. **[Situação: PARCIAL]**

**REL-004** — Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão. **[Situação: PARCIAL]**

**REL-005** — Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada. **[Situação: PARCIAL]**

**REL-006** — Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica. **[Situação: PARCIAL]**

## Grupo AUD — 7 requisitos

**AUD-001** — Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável. **[Situação: PARCIAL]**

**AUD-002** — Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva. **[Situação: PARCIAL]**

**AUD-003** — Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento. **[Situação: PARCIAL]**

**AUD-004** — Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação. **[Situação: PARCIAL]**

**AUD-005** — Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede. **[Situação: PARCIAL]**

**AUD-006** — Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário. **[Situação: PARCIAL]**

**AUD-007** — Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal. **[Situação: PARCIAL]**

## Grupo PER — 12 requisitos

**PER-001** — Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão. **[Situação: PARCIAL]**

**PER-002** — Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial. **[Situação: PARCIAL]**

**PER-003** — Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial. **[Situação: PARCIAL]**

**PER-004** — Presidente atua somente em consulta nacional neste módulo. **[Situação: PARCIAL]**

**PER-005** — Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte. **[Situação: PARCIAL]**

**PER-006** — Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria. **[Situação: PARCIAL]**

**PER-007** — Correção excepcional de CPF é autorizada pelo Gestor Local. **[Situação: PARCIAL]**

**PER-008** — Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado. **[Situação: PARCIAL]**

**PER-009** — Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos. **[Situação: PARCIAL]**

**PER-010** — Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro. **[Situação: PARCIAL]**

**PER-011** — Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica. **[Situação: PARCIAL]**

**PER-012** — Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial. **[Situação: PARCIAL]**

## Grupo HIS — 6 requisitos

**HIS-001** — Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

**HIS-002** — Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês. **[Situação: PARCIAL]**

**HIS-003** — Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação. **[Situação: PARCIAL]**

**HIS-004** — Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo. **[Situação: PARCIAL]**

**HIS-005** — História é a fonte canônica; projeções atuais são conveniência técnica. **[Situação: PARCIAL]**

**HIS-006** — A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores. **[Situação: BLOQUEADO POR DOCUMENTAÇÃO]**

