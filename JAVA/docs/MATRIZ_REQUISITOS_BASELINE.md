# Matriz de requisitos — baseline

**Versão física e provas atuais; resultados históricos não conferem aprovação.**

198 IDs;635 critérios. Contagem: A=0, B=149, C=49, D=0, E=0.

| ID | Antes | Depois | Critérios técnicos demonstrados na versão atual | Enunciado original |
|---|---|---|---:|---|
| GOV-003 | B | — | Não se aplica à baseline | Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável. |
| GOV-004 | B | — | Não se aplica à baseline | Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação. |
| TMP-001 | B | — | Não se aplica à baseline | Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos. |
| TMP-002 | B | — | Não se aplica à baseline | Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo. |
| TMP-003 | B | — | Não se aplica à baseline | Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável. |
| TMP-004 | C | — | Não se aplica à baseline | O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica. |
| TMP-005 | C | — | Não se aplica à baseline | A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato. |
| TMP-006 | B | — | Não se aplica à baseline | Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida. |
| TMP-007 | B | — | Não se aplica à baseline | O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia. |
| TMP-008 | C | — | Não se aplica à baseline | Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente. |
| TMP-009 | C | — | Não se aplica à baseline | Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente. |
| TMP-010 | B | — | Não se aplica à baseline | Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada. |
| TMP-011 | B | — | Não se aplica à baseline | Eventos da mesma data que não colidem podem coexistir. |
| TMP-012 | C | — | Não se aplica à baseline | Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos. |
| TMP-013 | B | — | Não se aplica à baseline | Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo. |
| TMP-014 | C | — | Não se aplica à baseline | Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito. |
| TMP-015 | B | — | Não se aplica à baseline | Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência. |
| TMP-016 | C | — | Não se aplica à baseline | Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão. |
| TMP-017 | C | — | Não se aplica à baseline | `FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito. |
| CON-001 | C | — | Não se aplica à baseline | Número + ano identificam unicamente o contrato em todo o sistema. |
| CON-002 | B | — | Não se aplica à baseline | Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade. |
| CON-003 | C | — | Não se aplica à baseline | Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável. |
| CON-004 | B | — | Não se aplica à baseline | Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização. |
| CON-005 | B | — | Não se aplica à baseline | Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico. |
| CON-006 | B | — | Não se aplica à baseline | Data de início e fim de vigência são obrigatórias para contrato publicado. |
| CON-007 | B | — | Não se aplica à baseline | Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências. |
| CON-008 | C | — | Não se aplica à baseline | Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto. |
| CON-009 | B | — | Não se aplica à baseline | O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões. |
| CON-010 | B | — | Não se aplica à baseline | Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável. |
| CON-011 | B | — | Não se aplica à baseline | Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita. |
| CON-012 | B | — | Não se aplica à baseline | Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa. |
| CON-013 | B | — | Não se aplica à baseline | Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período. |
| CON-014 | B | — | Não se aplica à baseline | Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos. |
| CON-015 | B | — | Não se aplica à baseline | Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora. |
| CON-016 | B | — | Não se aplica à baseline | Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI. |
| CON-017 | B | — | Não se aplica à baseline | Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido. |
| CON-018 | B | — | Não se aplica à baseline | Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente. |
| CON-019 | C | — | Não se aplica à baseline | Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização. |
| CON-020 | B | — | Não se aplica à baseline | Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa. |
| CON-021 | B | — | Não se aplica à baseline | Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO. |
| CON-022 | B | — | Não se aplica à baseline | A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato. |
| CON-023 | C | — | Não se aplica à baseline | Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado. |
| CON-024 | B | — | Não se aplica à baseline | Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação. |
| CON-025 | C | — | Não se aplica à baseline | Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador. |
| EMP-001 | B | — | Não se aplica à baseline | Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada. |
| EMP-002 | B | — | Não se aplica à baseline | Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos. |
| EMP-003 | B | — | Não se aplica à baseline | Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real. |
| CAT-001 | B | — | Não se aplica à baseline | Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre. |
| CAT-002 | B | — | Não se aplica à baseline | Cargo é cadastro próprio administrado pela Gestão Local. |
| CAT-003 | B | — | Não se aplica à baseline | Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos. |
| CAT-004 | C | — | Não se aplica à baseline | Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual. |
| CAT-005 | C | — | Não se aplica à baseline | Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação. |
| CAT-006 | B | — | Não se aplica à baseline | Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria. |
| CAT-007 | C | — | Não se aplica à baseline | Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva. |
| CAT-008 | C | — | Não se aplica à baseline | Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita. |
| CAT-009 | B | — | Não se aplica à baseline | Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas. |
| CMP-001 | B | — | Não se aplica à baseline | Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático. |
| CMP-002 | B | — | Não se aplica à baseline | Não pode haver combinação duplicada no mesmo recorte temporal. |
| CMP-003 | B | — | Não se aplica à baseline | Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato. |
| CMP-004 | B | — | Não se aplica à baseline | Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente. |
| CMP-005 | B | — | Não se aplica à baseline | Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível. |
| CMP-006 | B | — | Não se aplica à baseline | Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida. |
| CMP-007 | B | — | Não se aplica à baseline | Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história. |
| CMP-008 | B | — | Não se aplica à baseline | Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo. |
| CMP-009 | B | — | Não se aplica à baseline | Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria. |
| CMP-010 | B | — | Não se aplica à baseline | Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente. |
| CMP-011 | B | — | Não se aplica à baseline | Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente. |
| RED-001 | B | — | Não se aplica à baseline | Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas. |
| RED-002 | B | — | Não se aplica à baseline | Limite de 25% é verificado sobre o total do contrato, não individualmente por composição. |
| RED-003 | B | — | Não se aplica à baseline | Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados. |
| RED-004 | B | — | Não se aplica à baseline | Aumento contratual não possui limite de 25%. |
| RED-005 | B | — | Não se aplica à baseline | Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada. |
| RED-006 | B | — | Não se aplica à baseline | O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo. |
| RED-007 | B | — | Não se aplica à baseline | Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro. |
| RED-008 | B | — | Não se aplica à baseline | Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada. |
| ADI-001 | B | — | Não se aplica à baseline | Contrato pode possuir vários aditamentos sem apagar registros anteriores. |
| ADI-002 | B | — | Não se aplica à baseline | Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória. |
| ADI-003 | B | — | Não se aplica à baseline | Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante. |
| ADI-004 | B | — | Não se aplica à baseline | Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto. |
| ADI-005 | B | — | Não se aplica à baseline | Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica. |
| ADI-006 | B | — | Não se aplica à baseline | Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data. |
| ADI-007 | B | — | Não se aplica à baseline | Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada. |
| ADI-008 | B | — | Não se aplica à baseline | Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados. |
| ADI-009 | B | — | Não se aplica à baseline | Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação. |
| ADI-010 | B | — | Não se aplica à baseline | Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios. |
| REM-001 | B | — | Não se aplica à baseline | Remanejamento é fato próprio, identificado, datado, justificado e auditado. |
| REM-002 | B | — | Não se aplica à baseline | Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero. |
| REM-003 | B | — | Não se aplica à baseline | Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento. |
| REM-004 | B | — | Não se aplica à baseline | Remanejamento pode ser retificado/versionado, preservando original e cadeia completa. |
| REM-005 | B | — | Não se aplica à baseline | Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade. |
| REM-006 | B | — | Não se aplica à baseline | Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação. |
| REM-007 | C | — | Não se aplica à baseline | Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito. |
| DOC-001 | B | — | Não se aplica à baseline | Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos. |
| DOC-002 | B | — | Não se aplica à baseline | Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI. |
| DOC-003 | B | — | Não se aplica à baseline | Remanejamento pode registrar SEI e mantém anexo opcional. |
| DOC-004 | B | — | Não se aplica à baseline | Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato. |
| DOC-005 | B | — | Não se aplica à baseline | Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior. |
| DOC-006 | B | — | Não se aplica à baseline | Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI. |
| DOC-007 | B | — | Não se aplica à baseline | Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável. |
| DOC-008 | B | — | Não se aplica à baseline | Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo. |
| DOC-009 | B | — | Não se aplica à baseline | Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito. |
| DOC-010 | C | — | Não se aplica à baseline | Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado. |
| COL-001 | B | — | Não se aplica à baseline | Nome e CPF são obrigatórios. |
| COL-002 | B | — | Não se aplica à baseline | RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais. |
| COL-003 | B | — | Não se aplica à baseline | Endereço é mantido e opcional. |
| COL-004 | B | — | Não se aplica à baseline | Dados bancários foram removidos do cadastro deste fluxo. |
| COL-005 | B | — | Não se aplica à baseline | CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface. |
| COL-006 | B | — | Não se aplica à baseline | CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF. |
| COL-007 | B | — | Não se aplica à baseline | Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente. |
| COL-009 | B | — | Não se aplica à baseline | Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa. |
| COL-010 | C | — | Não se aplica à baseline | Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época. |
| COL-011 | B | — | Não se aplica à baseline | Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita. |
| COL-012 | B | — | Não se aplica à baseline | Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época. |
| COL-013 | B | — | Não se aplica à baseline | Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa. |
| MAT-001 | B | — | Não se aplica à baseline | Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa. |
| MAT-002 | B | — | Não se aplica à baseline | Matrícula é opcional no vínculo e única dentro do contrato. |
| MAT-003 | B | — | Não se aplica à baseline | Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada. |
| MAT-004 | B | — | Não se aplica à baseline | Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula. |
| MAT-005 | B | — | Não se aplica à baseline | Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida. |
| VIN-001 | B | — | Não se aplica à baseline | Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional. |
| VIN-002 | B | — | Não se aplica à baseline | Antiga data de admissão é substituída por início efetivo do vínculo. |
| VIN-003 | B | — | Não se aplica à baseline | Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes. |
| VIN-004 | B | — | Não se aplica à baseline | Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo. |
| VIN-005 | C | — | Não se aplica à baseline | Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva. |
| VIN-006 | B | — | Não se aplica à baseline | Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido. |
| VIN-007 | B | — | Não se aplica à baseline | Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente. |
| VIN-008 | C | — | Não se aplica à baseline | Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa. |
| VIN-009 | C | — | Não se aplica à baseline | Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior. |
| VIN-010 | C | — | Não se aplica à baseline | Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local. |
| VIN-011 | B | — | Não se aplica à baseline | Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência. |
| VIN-012 | B | — | Não se aplica à baseline | Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita. |
| VIN-013 | B | — | Não se aplica à baseline | Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo. |
| VIN-014 | C | — | Não se aplica à baseline | Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores. |
| VIN-015 | C | — | Não se aplica à baseline | Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria. |
| ALO-001 | B | — | Não se aplica à baseline | Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo. |
| ALO-002 | B | — | Não se aplica à baseline | Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação. |
| ALO-003 | B | — | Não se aplica à baseline | Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades. |
| ALO-004 | B | — | Não se aplica à baseline | Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior. |
| ALO-005 | B | — | Não se aplica à baseline | Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar. |
| ALO-006 | B | — | Não se aplica à baseline | Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data. |
| ALO-007 | C | — | Não se aplica à baseline | Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida. |
| ALO-008 | B | — | Não se aplica à baseline | Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida. |
| DES-001 | B | — | Não se aplica à baseline | Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato. |
| DES-002 | B | — | Não se aplica à baseline | Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte. |
| DES-003 | B | — | Não se aplica à baseline | Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado. |
| DES-004 | C | — | Não se aplica à baseline | Desligamento possui motivo de catálogo, responsável e data/hora de registro. |
| DES-005 | C | — | Não se aplica à baseline | Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição. |
| DES-006 | B | — | Não se aplica à baseline | Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia. |
| FRE-001 | C | — | Não se aplica à baseline | Frequência resolve vínculo e alocação válidos na data negocial. |
| FRE-002 | B | — | Não se aplica à baseline | Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo. |
| FRE-003 | B | — | Não se aplica à baseline | Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00. |
| FRE-004 | B | — | Não se aplica à baseline | Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora. |
| FRE-005 | B | — | Não se aplica à baseline | Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente. |
| FRE-006 | B | — | Não se aplica à baseline | Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa. |
| FRE-007 | B | — | Não se aplica à baseline | Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria. |
| FRE-008 | C | — | Não se aplica à baseline | Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis. |
| FRE-009 | B | — | Não se aplica à baseline | Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada. |
| FRE-010 | B | — | Não se aplica à baseline | Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores. |
| FRE-011 | B | — | Não se aplica à baseline | Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências. |
| FRE-012 | C | — | Não se aplica à baseline | Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada. |
| FRE-013 | B | — | Não se aplica à baseline | No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação. |
| FRE-014 | B | — | Não se aplica à baseline | Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional. |
| FRE-015 | C | — | Não se aplica à baseline | A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador. |
| FRE-016 | B | — | Não se aplica à baseline | Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original. |
| FRE-017 | B | — | Não se aplica à baseline | Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial. |
| FRE-018 | C | — | Não se aplica à baseline | Permissão de edição combina autorização atual do usuário com o escopo histórico do registro. |
| FRE-019 | B | — | Não se aplica à baseline | Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria. |
| REL-001 | C | — | Não se aplica à baseline | Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores. |
| REL-002 | B | — | Não se aplica à baseline | Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração. |
| REL-003 | C | — | Não se aplica à baseline | Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro. |
| REL-004 | B | — | Não se aplica à baseline | Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão. |
| REL-005 | C | — | Não se aplica à baseline | Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada. |
| REL-006 | B | — | Não se aplica à baseline | Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica. |
| AUD-001 | B | — | Não se aplica à baseline | Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável. |
| AUD-002 | B | — | Não se aplica à baseline | Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva. |
| AUD-003 | C | — | Não se aplica à baseline | Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento. |
| AUD-004 | C | — | Não se aplica à baseline | Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação. |
| AUD-005 | B | — | Não se aplica à baseline | Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede. |
| AUD-006 | C | — | Não se aplica à baseline | Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário. |
| AUD-007 | B | — | Não se aplica à baseline | Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal. |
| PER-001 | B | — | Não se aplica à baseline | Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão. |
| PER-002 | B | — | Não se aplica à baseline | Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial. |
| PER-003 | B | — | Não se aplica à baseline | Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial. |
| PER-004 | C | — | Não se aplica à baseline | Presidente atua somente em consulta nacional neste módulo. |
| PER-005 | B | — | Não se aplica à baseline | Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte. |
| PER-006 | C | — | Não se aplica à baseline | Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria. |
| PER-007 | B | — | Não se aplica à baseline | Correção excepcional de CPF é autorizada pelo Gestor Local. |
| PER-008 | B | — | Não se aplica à baseline | Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado. |
| PER-009 | B | — | Não se aplica à baseline | Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos. |
| PER-010 | C | — | Não se aplica à baseline | Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro. |
| PER-011 | C | — | Não se aplica à baseline | Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica. |
| PER-012 | B | — | Não se aplica à baseline | Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial. |
| HIS-001 | C | — | Não se aplica à baseline | Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos. |
| HIS-002 | B | — | Não se aplica à baseline | Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês. |
| HIS-003 | B | — | Não se aplica à baseline | Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação. |
| HIS-004 | C | — | Não se aplica à baseline | Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo. |
| HIS-005 | C | — | Não se aplica à baseline | História é a fonte canônica; projeções atuais são conveniência técnica. |
| HIS-006 | C | — | Não se aplica à baseline | A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores. |

## Critérios, oráculos e evidências individuais

O JSON correspondente preserva integralmente o texto, origem, análise individual, componentes, todos os métodos/invocações, XMLs, hashes e pendências. O detalhe a seguir permite navegar sem depender de nomes de testes como prova.

### GOV-003 — B

Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**GOV-003:checkpoint:001** DELETE publicado=409 e contrato permanece

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**GOV-003:checkpoint:002** Retificar mantém versões original e nova

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**GOV-003:checkpoint:003** Cancelar preserva autor/motivo/data e retira só efeito

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

### GOV-004 — B

Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**GOV-004:checkpoint:001** Rascunho vazio aceita descarte 204

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**GOV-004:checkpoint:002** GET após descarte=404

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**GOV-004:checkpoint:003** Fato/vínculo/histórico impede descarte

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#rascunhoComDependenciaAuditavelNaoPodeSerDescartado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#rascunhoComDependenciaAuditavelNaoPodeSerDescartado(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#rascunhoComDependenciaAuditavelNaoPodeSerDescartado(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#rascunhoComDependenciaAuditavelNaoPodeSerDescartado(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**GOV-004:checkpoint:004** Falha intermediária não deixa descarte parcial

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#falhaNaTransacaoDeDescarteRestauraContratoEComposicaoEmNovaLeitura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#falhaNaTransacaoDeDescarteRestauraContratoEComposicaoEmNovaLeitura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### TMP-001 — B

Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-001:checkpoint:001** Pessoa, Vínculo e Alocação são raízes relacionadas distintas; cadastro integrado preserva a identidade canônica.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

**TMP-001:checkpoint:002** Frequência resolve Contrato/Vínculo/Alocação da data e devolve seus identificadores separadamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes`; localização: `METODO_LOCALIZADO`.

**TMP-001:checkpoint:003** Empregado legado sem associação/fotografia não adota outra Pessoa apenas por igualdade do CPF; fotografia própria existente conserva a identidade documentada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.

### TMP-002 — B

Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-002:checkpoint:001** Recontratação em dias consecutivos conserva Pessoa e cria novo Vínculo sem sobreposição.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.

**TMP-002:checkpoint:002** Mudança de Lotação/Turno/Cargo retifica faixas da Alocação, preservando o mesmo Vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.

### TMP-003 — B

Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-003:checkpoint:001** Fatos separam data efetiva, instante de conhecimento, autor e motivo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

**TMP-003:checkpoint:002** Alterações de calendário preservam fotografia integral, sequência, autoria e conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

**TMP-003:checkpoint:003** Fontes manuais mantêm criação, retificação de retificação e cancelamento com autoria e motivo próprios.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.

**TMP-003:checkpoint:004** Motivo aceito de adicional é persistido integralmente na raiz e na versão; excesso é recusado antes da gravação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaAdicionalJustificativaLimiteHttpIT#criacaoValidaLimiteSemTruncarMotivoNemGravarParcialmente`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaAdicionalJustificativaLimiteHttpIT#retificacaoValidaLimiteEPreservaOriginalQuandoMotivoExcedeMaximo`; localização: `METODO_LOCALIZADO`.

**TMP-003:checkpoint:005** Normalização do motivo de um novo comando é coerente na validação, raiz e versão, sem reescrever fatos anteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaAdicionalJustificativaLimiteHttpIT#espacosExternosNaoFazemMotivoValidoDivergirEntreRaizEVersao`; localização: `METODO_LOCALIZADO`.

### TMP-004 — C

O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-004:checkpoint:001** Posição histórica contratual e de alocação usa fotografias dos fatos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.
- `com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test.xml`; XML SHA-256 `e426c819cb32f7d2885b3afbe619c1a283ce11a001108743c03cc630f3fccfb3`.

**TMP-004:checkpoint:002** Gravar ou retificar frequência sobre alocação legada não converte rótulos atuais em fotografia de contexto histórico.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaFotografiasAusentesHttpIT#gravacaoNaoCompletaRotulosLegadosComMestreEPreservaProvaDaPropriaFrequencia`; localização: `METODO_LOCALIZADO`.

**TMP-004:checkpoint:003** Leitura e retificação de frequência mantêm a identidade já fotografada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaFotografiaIdentidadeHttpIT#correcaoDeIdentidadeNaoReescreveConsultaNemRetificacaoDaFrequencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFotografiaIdentidadeHttpIT#retificacaoDeStatusPreservaFotografiaAnteriorSemCongelarNovosDias`; localização: `METODO_LOCALIZADO`.

**TMP-004:checkpoint:004** A posição em corte anterior deriva o estado do evento válido naquele corte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

**TMP-004:checkpoint:005** Retificação/cancelamento de fonte manual não apaga o fato que reconstrói o passado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

### TMP-005 — C

A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-005:checkpoint:001** Contrato, Vínculo e Alocação respeitam o conhecimento disponível até o instante solicitado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotoresTemporaisConhecimentoRegressaoTest#cancelamentoConhecidoDepoisNaoApagaAlocacaoDaVisaoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test.xml`; XML SHA-256 `c54055d82029280a0c79e902e2d584cae19e5b59fe08d00316a1d3816332a44a`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.
- `com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest#cancelamentoConhecidoDepoisNaoApagaAlocacaoDaVisaoAnterior`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest.xml`; XML SHA-256 `57285416951f436d3557f1fd14285ba6886142330403fecb51f64586d7260a5b`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

**TMP-005:checkpoint:002** Calendário e complemento separam data do fato e instante de registro em consultas de conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

### TMP-006 — B

Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-006:checkpoint:001** Reexecutar inicializadores não cria ou altera fatos negociais existentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `TemporalidadeInicializacaoHttpIT#reexecutarInicializadoresPreservaCustomizacaoEFatosNegociaisJaRegistrados`; localização: `METODO_LOCALIZADO`.

**TMP-006:checkpoint:002** Chegada da data de fato programado altera a posição consultada sem gravar outro fato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita`; localização: `METODO_LOCALIZADO`.

### TMP-007 — B

O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-007:checkpoint:001** Alocação possui granularidade diária, inclusive quando retificada na própria data.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.

**TMP-007:checkpoint:002** Jornada22–06 usa a alocação da data de início mesmo após mudança no dia seguinte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT.xml`; XML SHA-256 `295b506f1da7c3076444dce1232a4c561bdcb98bd8291a7fcca5cc2246e525e3`.

### TMP-008 — C

Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-008:checkpoint:001** Documento retroativo recompõe o contrato e reaplica documento posterior válido.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-008:checkpoint:002** Mudança retroativa e retificações sucessivas preservam as faixas originais e sucessoras válidas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasRegrasOriginaisHttpIT#retificacaoDeRetificacaoDoInicioRecuperaAlocacaoHistoricaOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasRegrasOriginaisHttpIT#retificacaoDeDesligamentoRecuperaSucessoraHistoricaAtingidaPeloLimite`; localização: `METODO_LOCALIZADO`.

**TMP-008:checkpoint:003** Extensão através de transferência documentada recompõe destino sem recontratar e valida sua capacidade.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasRetificacaoAposTransferenciaHttpIT#ampliarTerminoAlemDaTransferenciaUsaMapeamentoDocumentadoSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasRetificacaoAposTransferenciaHttpIT#extensaoMapeadaRevalidaExcedenteDoDestinoAntesDeCriarSucessora`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiaTransferenciaRestauradaHttpIT#sucessoraRestauradaUsaMunicipioEGradeDocumentadosNaTransferencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiaTransferenciaRestauradaHttpIT#mapaLegadoSemFotografiaBloqueiaReconstrucaoSemInventarGradeDoCadastroAtual`; localização: `METODO_LOCALIZADO`.

**TMP-008:checkpoint:004** Restauração de faixa e cancelamento de dispensa preservam a obrigação posterior conhecida ou recusam o conflito sem gravação parcial.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasPoliticaMatriculaFuturaHttpIT#ampliarOuCancelarTerminoRevalidaObrigacaoProgramadaNaFaixaRestaurada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasPoliticaMatriculaFuturaHttpIT#cancelarDispensaProgramadaRecusaVinculoFuturoQuePerderiaPoliticaOpcional`; localização: `METODO_LOCALIZADO`.

### TMP-009 — C

Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-009:checkpoint:001** Replay incompatível preserva documento posterior e gera pendência identificável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-009:checkpoint:002** Retirada retroativa de cobertura mantém a frequência e encaminha revisão.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.

**TMP-009:checkpoint:003** Programação humana futura incompatível não é cancelada como ajuste técnico.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasRegrasOriginaisHttpIT#desligamentoFuturoNaoCancelaSucessoraProgramadaSemTratamentoExplicito`; localização: `METODO_LOCALIZADO`.

### TMP-010 — B

Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-010:checkpoint:001** Colisão na mesma data é bloqueada até existir ordem negocial completa.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosPrecedenciaBifurcadaHttpIT#mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosPrecedenciaBifurcadaHttpIT#sequenciaFormalCompletaDesambiguaSucessoresDoMesmoAto`; localização: `METODO_LOCALIZADO`.

**TMP-010:checkpoint:002** SUCEDE explícito define a posição sem usar ordem técnica de gravação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalContratoEtapa04Test#sucedeExplicitoResolveConflitoSemUsarOrdemDeGravacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#sucedeExplicitoResolveConflitoSemUsarOrdemDeGravacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test.xml`; XML SHA-256 `c54055d82029280a0c79e902e2d584cae19e5b59fe08d00316a1d3816332a44a`.

**TMP-010:checkpoint:003** Restaurar sucessora no novo último dia exige precedência entre os fatos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT.xml`; XML SHA-256 `ff0136f4ae799a21501b940d7c763e548a03441ca9e50de5fdbf03ea0bb92f6e`.

### TMP-011 — B

Eventos da mesma data que não colidem podem coexistir.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-011:checkpoint:001** Recortes distintos na mesma data podem coexistir; recortes colidentes continuam exigindo precedência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### TMP-012 — C

Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-012:checkpoint:001** Documento antigo recebido depois entra pela data efetiva e reaplica fatos válidos posteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-012:checkpoint:002** Se o documento antigo torna um posterior incompatível, o posterior é preservado e o conflito fica explícito.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### TMP-013 — B

Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-013:checkpoint:001** Desligamento usa a data como último dia; novo vínculo no dia seguinte não sobrepõe.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#trocaDiretaEmDiasConsecutivosNaoSobrepoePeriodosInclusivos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#trocaDiretaEmDiasConsecutivosNaoSobrepoePeriodosInclusivos`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

**TMP-013:checkpoint:002** Mudança de alocação aplica nova situação desde a data indicada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.

### TMP-014 — C

Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-014:checkpoint:001** ADI fora da vigência só produz efeito quando o próprio ato amplia validamente a cobertura.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-014:checkpoint:002** Prorrogação conhecida e programada permite cadastro dentro da extensão mantendo término humano.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

### TMP-015 — B

Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**TMP-015:checkpoint:001** Prorrogação não pode criar lacuna interna na vigência do mesmo contrato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#prorrogacaoNaoPodeCriarLacunaInternaNaVigencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoNaoPodeCriarLacunaInternaNaVigencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-015:checkpoint:002** Extensão contínua não apaga término humano nem recontratação já programada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

### TMP-016 — C

Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-016:checkpoint:001** Relação de precedência pode ser transitiva através de ato que altera outro campo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosPrecedenciaBifurcadaHttpIT#precedenciaTransitivaPodePassarPorInstrumentoQueAlteraOutroCampo`; localização: `METODO_LOCALIZADO`.

**TMP-016:checkpoint:002** Retificar predecessor conserva a sequência de sucessor válido e o original consultável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosPrecedenciaBifurcadaHttpIT#retificarAntecessorPreservaSequenciaDoSucessorValidoNaMesmaData`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.

**TMP-016:checkpoint:003** Restauração coincidente com término não pode usar data de registro como precedência implícita.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT.xml`; XML SHA-256 `ff0136f4ae799a21501b940d7c763e548a03441ca9e50de5fdbf03ea0bb92f6e`.

### TMP-017 — C

`FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**TMP-017:checkpoint:001** FUTURO/VIGENTE/ENCERRADO são posições derivadas da data e não novos estados negociados.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.

**TMP-017:checkpoint:002** Programação válida durante inatividade não reativa a operação silenciosamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**TMP-017:checkpoint:003** Anulação explícita afeta vínculo desde seu alcance, preservando antes da data/corte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

### CON-001 — C

Número + ano identificam unicamente o contrato em todo o sistema.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-001:checkpoint:001** Duplicar número/ano na mesma Gestão=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-001:checkpoint:002** Duplicar entre Gestões=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-001:checkpoint:003** Número igual em ano distinto pode existir

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#mesmoNumeroEmAnosDiferentesIdentificaDoisContratosPersistidos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#mesmoNumeroEmAnosDiferentesIdentificaDoisContratosPersistidos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-001:checkpoint:004** Restrição JPA uk_contrato_numero_ano presente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#identificacaoTemRestricaoCompostaNumeroEAno`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#identificacaoTemRestricaoCompostaNumeroEAno`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.

### CON-002 — B

Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-002:checkpoint:001** PUT em rascunho altera número/ano

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-002:checkpoint:002** Número/ano duplicado bloqueia sem persistir

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#identificadorDuplicadoEmPutDeRascunhoNaoAlteraNenhumContrato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#identificadorDuplicadoEmPutDeRascunhoNaoAlteraNenhumContrato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**CON-002:checkpoint:003** PUT publicado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-003 — C

Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-003:checkpoint:001** PUT comum publicado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-003:checkpoint:002** Sem concessão correção=403

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-003:checkpoint:003** Correção autorizada preserva antes/depois e versões

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.etapa10.CorrecaoIdentificadorContratoServicoEtapa10Test#corrigeVersionaMantemHistoricoEAuditaComAutorizacaoConcedida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.etapa10.CorrecaoIdentificadorContratoServicoEtapa10Test#corrigeVersionaMantemHistoricoEAuditaComAutorizacaoConcedida`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa10.CorrecaoIdentificadorContratoServicoEtapa10Test.xml`; XML SHA-256 `0c1ab9168ef0cdcccf0e8a8df7946d3a43936687b5ba7a760386acd3a8503dfd`.

**CON-003:checkpoint:004** Pesquisa pelo identificador anterior encontra contrato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-003:checkpoint:005** Reenvio exato não cria nova correção

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-004 — B

Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-004:checkpoint:001** Descartar rascunho e recriar mesmo número/ano=201

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-004:checkpoint:002** Novo ID difere do descartado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-004:checkpoint:003** Publicado permanece impedido de excluir

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-005 — B

Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-005:checkpoint:001** Detalhe tem campos obrigatórios e eixos distintos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.

**CON-005:checkpoint:002** Rotas composição/instrumentos/pendências/eventos expõem fatos do mesmo contrato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico`; localização: `METODO_LOCALIZADO`.

**CON-005:checkpoint:003** Consulta datada preserva empresa/tipo/vigência da data

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.

### CON-006 — B

Data de início e fim de vigência são obrigatórias para contrato publicado.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-006:checkpoint:001** Rascunho sem uma data=201

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-006:checkpoint:002** Publicar sem início ou fim=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-006:checkpoint:003** Fim anterior ao início rejeitado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-006:checkpoint:004** Publicação válida mantém datas efetivas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-007 — B

Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-007:checkpoint:001** Contrato pode ser VIGENTE+INATIVO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-007:checkpoint:002** Passagem do tempo muda FUTURO/VIGENTE/ENCERRADO sem evento novo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.

**CON-007:checkpoint:003** Anulado/cancelado dependem de evento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#rascunhoCanceladoEAnuladoDependemDeEstadoNegocialExplicito`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#rascunhoCanceladoEAnuladoDependemDeEstadoNegocialExplicito`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-007:checkpoint:004** Detalhe e posição temporal devem concordar na mesma data

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoInativaPermaneceInativaNaPosicaoTemporal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoInativaPermaneceInativaNaPosicaoTemporal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

### CON-008 — C

Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-008:checkpoint:001** Empresa/tipo/vigência/fonte incompletos bloqueiam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-008:checkpoint:002** Soma divergente e duplicidade bloqueiam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT.xml`; XML SHA-256 `fa92a48bc547fe9388094f4c0ee52517a761589d446165640cb25228f5b46e10`.

**CON-008:checkpoint:003** Publicação cria evento/fotografia/base original

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT.xml`; XML SHA-256 `b841692b96e8fc7276a796bcd56329ec4f163174289f5343b45ea5200e4eb2e1`.

**CON-008:checkpoint:004** Todos valores publicados devem estar reconstruíveis sem mestre atual

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.

### CON-009 — B

O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-009:checkpoint:001** Gestor Local escreve só própria Gestão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT.xml`; XML SHA-256 `934a535783f1bfa5521f3a96f054dc3f4312dec641ff4e8eeaf9d66543dd054e`.

**CON-009:checkpoint:002** Gestor Estadual lê estado e não escreve

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-009:checkpoint:003** Gestões externas recebem 404

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT.xml`; XML SHA-256 `934a535783f1bfa5521f3a96f054dc3f4312dec641ff4e8eeaf9d66543dd054e`.

**CON-009:checkpoint:004** Transferência preserva titular anterior por período

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-010 — B

Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-010:checkpoint:001** Criar intervalo INATIVO sem mudar vigência/vínculo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-010:checkpoint:002** Futuro não bloqueia antes da data

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#inativacaoFuturaRegistraFatoSemMudarProjecaoAtualAntesDaData`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#inativacaoFuturaRegistraFatoSemMudarProjecaoAtualAntesDaData`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

**CON-010:checkpoint:003** Reativação preserva intervalo e vínculo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-010:checkpoint:004** Prévia desatualizada bloqueia confirmação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### CON-011 — B

Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-011:checkpoint:001** ADI programado altera descrição durante bloqueio

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-011:checkpoint:002** REM programado altera distribuição

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-011:checkpoint:003** Habilitação continua INATIVO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-011:checkpoint:004** Vínculo na data bloqueada=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CON-012 — B

Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-012:checkpoint:001** Extensão válida torna situação VIGENTE

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-012:checkpoint:002** Habilitação segue INATIVO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-012:checkpoint:003** Vínculo encerrado mantém término

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.

**CON-012:checkpoint:004** Retomada depende de comando explícito

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.

### CON-013 — B

Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-013:checkpoint:001** PUT comum publicado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-013:checkpoint:002** Transferência exige autorização

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT.xml`; XML SHA-256 `255599bdb231f15edffc29729bb0ee1925bab20151f5571517bdb5b320ee8914`.

**CON-013:checkpoint:003** Antes da data Gestão origem

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-013:checkpoint:004** Depois da data Gestão destino

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaUsaTipoMapeadoNoDestinoEPreservaTipoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-013:checkpoint:005** Nenhum cadastro histórico original é reescrito

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#historicoDaTransferenciaNaoReescreveNomesAposRenomearCatalogosEGestoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.

### CON-014 — B

Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-014:checkpoint:001** Sem prévia retroativa=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaExigePreviaMesmoComFonteEComposicaoCompletas`; localização: `METODO_LOCALIZADO`.

**CON-014:checkpoint:002** Prévia+confirmação publica

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaTemPreviaEAssinaDadosDePreparacao`; localização: `METODO_LOCALIZADO`.

**CON-014:checkpoint:003** Mudança da preparação invalida token

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaTemPreviaEAssinaDadosDePreparacao`; localização: `METODO_LOCALIZADO`.

**CON-014:checkpoint:004** Evento mantém início real e registro distinto

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaTemPreviaEAssinaDadosDePreparacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### CON-015 — B

Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-015:checkpoint:001** Sem data do ato/fonte rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-015:checkpoint:002** Anular preserva contrato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasHttpIT#anulacaoExigePreviaAntesDeRetirarValidadeDosFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasHttpIT#anulacaoExigePreviaAntesDeRetirarValidadeDosFatosRelacionados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasHttpIT.xml`; XML SHA-256 `a05bc124a3c9c40a8b6499992b7887d63a0f9efe8b6068fb9ad88087a0581b17`.

**CON-015:checkpoint:003** Evento guarda motivo/autor/data/fonte

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-015:checkpoint:004** Posição no alcance ANULADO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

### CON-016 — B

Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-016:checkpoint:001** Data final efetiva reduzida

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-016:checkpoint:002** Fonte obrigatória

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-016:checkpoint:003** Vínculo aberto termina no mesmo dia com ENCERRAMENTO_ANTECIPADO_CONTRATO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-016:checkpoint:004** Dia seguinte contrato ENCERRADO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-016:checkpoint:005** Nenhuma realocação automática

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#encerramentoSemCausaCadastradaFazRollbackEDepoisEncerraSemRealocacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#encerramentoSemCausaCadastradaFazRollbackEDepoisEncerraSemRealocacao`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-016:checkpoint:006** Rescisao compara termo com vigencia reconstruida por atos futuros.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#encerramentoAntecipadoUsaVigenciaReconstruidaProrrogadaPorAtoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#encerramentoAntecipadoUsaVigenciaReconstruidaProrrogadaPorAtoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.

### CON-017 — B

Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-017:checkpoint:001** Instrumento futuro persiste sem efeito antecipado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**CON-017:checkpoint:002** Na data correta aparece em posição

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**CON-017:checkpoint:003** Fora de vigência bloqueia salvo extensão válida no próprio ADI

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-017:checkpoint:004** Cancelamento anterior ao efeito preserva história

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.

### CON-018 — B

Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-018:checkpoint:001** Vínculo e alocação continuam consultáveis

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

**CON-018:checkpoint:002** Contrato da pessoa não é substituído

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-018:checkpoint:003** Frequência afetada mantém prova mas deixa cálculo válido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.FrequenciaAnulacaoContratoPreservacaoHttpIT#anulacaoPreservaFrequenciaFechadaEProvaOriginalMasRetiraCalculoEGeraPendencia`; localização: `METODO_LOCALIZADO`.

**CON-018:checkpoint:004** Anulação possui origem auditável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CON-019 — C

Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-019:checkpoint:001** DELETE anulado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-019:checkpoint:002** Criar mesmo número/ano=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-019:checkpoint:003** Consulta histórico preserva o anulado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### CON-020 — B

Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-020:checkpoint:001** Correção conserva anulação original

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#correcaoCancelaAnulacaoSemApagarHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.
- `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#correcaoCancelaAnulacaoSemApagarHistorico`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test.xml`; XML SHA-256 `c54055d82029280a0c79e902e2d584cae19e5b59fe08d00316a1d3816332a44a`.

**CON-020:checkpoint:002** Relação CANCELA aparece na história

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#correcaoCancelaAnulacaoSemApagarHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#correcaoCancelaAnulacaoSemApagarHistorico`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test.xml`; XML SHA-256 `c54055d82029280a0c79e902e2d584cae19e5b59fe08d00316a1d3816332a44a`.

**CON-020:checkpoint:003** Detalhe e posição temporal permanecem INATIVO até reativação explícita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#correcaoDeAnulacaoFuturaMantemEfeitoAdministrativoNaDataDeclarada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#correcaoDeAnulacaoFuturaMantemEfeitoAdministrativoNaDataDeclarada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest.xml`; XML SHA-256 `ff2fca812c4ea4df89f25ba90884b2c094df32845bb1254fadfc7a6ac96c0d45`.

**CON-020:checkpoint:004** Novo vínculo não opera antes da reativação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

### CON-021 — B

Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-021:checkpoint:001** Contrato já iniciado não admite cancelar-futuro

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-021:checkpoint:002** Cancelamento de futuro retorna CANCELADO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.

**CON-021:checkpoint:003** Vínculos futuros não operam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-021:checkpoint:004** Origem e motivo permanecem

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.

**CON-021:checkpoint:005** Contrato FUTURO no registro não admite alcance de cancelamento após início, deixando intervalo operacional; ato documentado antes do início permanece permitido.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoCancelamentoFuturoHttpIT#contratoAindaFuturoNaoAceitaCancelamentoComAlcanceDepoisDoInicio`; localização: `METODO_LOCALIZADO`.

### CON-022 — B

A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-022:checkpoint:001** VIGENTE e INATIVO coexistem

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.
- `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test.xml`; XML SHA-256 `d88077e033f0558cfa69dd1b61579ee04571e69f87e7a2b8130fd7a8ec612f8a`.

**CON-022:checkpoint:002** Pendência não troca estado negocial

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#pendenciaDeReplayNaoTrocaEstadoNegocialNemHabilitacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#pendenciaDeReplayNaoTrocaEstadoNegocialNemHabilitacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-022:checkpoint:003** Consulta atual e temporal concordam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

**CON-022:checkpoint:004** Somente evento próprio altera habilitação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#operacaoAdministrativaNaoSubstituiCorrecaoExplicitaDeAnulacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#operacaoAdministrativaNaoSubstituiCorrecaoExplicitaDeAnulacao(TipoEventoCicloVidaContrato)[1]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest.xml`; XML SHA-256 `ff2fca812c4ea4df89f25ba90884b2c094df32845bb1254fadfc7a6ac96c0d45`.
- `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#operacaoAdministrativaNaoSubstituiCorrecaoExplicitaDeAnulacao(TipoEventoCicloVidaContrato)[2]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest.xml`; XML SHA-256 `ff2fca812c4ea4df89f25ba90884b2c094df32845bb1254fadfc7a6ac96c0d45`.

### CON-023 — C

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-023:checkpoint:001** Sem autorização ou mapa completo bloqueia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.etapa07.TransferenciaGestaoContratoServicoEtapa07Test#bloqueiaTransferenciaSemMapeamentoDoContextoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT.xml`; XML SHA-256 `255599bdb231f15edffc29729bb0ee1925bab20151f5571517bdb5b320ee8914`.
- `com.example.siggtrat.etapa07.TransferenciaGestaoContratoServicoEtapa07Test#bloqueiaTransferenciaSemMapeamentoDoContextoPosterior`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.TransferenciaGestaoContratoServicoEtapa07Test.xml`; XML SHA-256 `322072f77543d72daffdfe07a24668a3c6a017bf366eca4cb655c6dbb77e7ad6`.

**CON-023:checkpoint:002** Tipo de contratação posterior deve ser válido na Gestão destino

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaExigeMapeamentoExplicitoDoTipoLocalDeDestino`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaRejeitaTipoQuePertenceAGestaoDeOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaUsaTipoMapeadoNoDestinoEPreservaTipoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaSemTipoDestinoNaoDeclaraTipoDaGestaoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoQueDependeDoTipoLegadoExigeRegularizacaoExplicitaDoDestino`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#aditamentoPosteriorDeTipoDaOrigemExigeTratamentoAntesDaTransferencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaSemTipoDestinoNaoDeclaraTipoDaGestaoAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoQueDependeDoTipoLegadoExigeRegularizacaoExplicitaDoDestino`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.

**CON-023:checkpoint:003** Composições e alocações mapeadas sem apagar anteriores

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasRetificacaoAposTransferenciaHttpIT#ampliarTerminoAlemDaTransferenciaUsaMapeamentoDocumentadoSemRecontratar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CON-023:checkpoint:004** Histórico de origem não muda após renomear cadastros

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#historicoDaTransferenciaNaoReescreveNomesAposRenomearCatalogosEGestoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT#novoMapaConservaProvaOuAusenciaDaOrigemECapturaDestinoDoAto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.contrato.ComposicaoFotografiaLegadaTest#callbackDeNovoMapaPreservaOrigemDesconhecidaECapturaDestinoDocumentado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.
- `com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT#novoMapaConservaProvaOuAusenciaDaOrigemECapturaDestinoDoAto(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT.xml`; XML SHA-256 `a5f7da6d8d77517a5486b668e8ca5b2130bf8a37649155305c8e4c383fde5bd2`.
- `com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT#novoMapaConservaProvaOuAusenciaDaOrigemECapturaDestinoDoAto(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT.xml`; XML SHA-256 `a5f7da6d8d77517a5486b668e8ca5b2130bf8a37649155305c8e4c383fde5bd2`.
- `com.example.siggtrat.contrato.ComposicaoFotografiaLegadaTest#callbackDeNovoMapaPreservaOrigemDesconhecidaECapturaDestinoDocumentado`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ComposicaoFotografiaLegadaTest.xml`; XML SHA-256 `680e2ba706d220da5a35ded7e5a9bfeaba4b0851bb618305df77354535a16f02`.

**CON-023:checkpoint:005** A política de matrícula resultante da transferência deve ser compatível com vínculos vigentes e programados atingidos; respeita override contratual false comprovado e matrícula válida já informada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#transferenciaParaTipoObrigatorioRecusaVinculoSemMatriculaSemMudarFatos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#overrideContratualOpcionalPermiteTransferirParaTipoObrigatorio`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#matriculaInformadaPermiteTransferenciaComHerancaObrigatoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#regularizarMatriculaAposRecusaPermiteMesmaTransferenciaSemConsumirConcessaoDuasVezes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#aditamentoPosteriorDispensandoMatriculaAntesDoInicioProgramadoEhRespeitadoNaTransferencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#restauracaoPosteriorDaHerancaObrigatoriaImpedeTransferenciaMesmoComOverrideOpcionalInicial`; localização: `METODO_LOCALIZADO`.

**CON-023:checkpoint:006** Retificacao preserva transferencia anterior e reaplica ato posterior valido no corte efetivo e de conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#retificarTransferenciaProgramadaReconstroiOrigemEReaplicaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#retificarTransferenciaProgramadaReconstroiOrigemEReaplicaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.

**CON-023:checkpoint:007** Cancelamento futuro e explicito, preserva original e posteriores validos; posterior incompativel implica rollback.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaFuturaPreservaOriginalEPosteriorSemEfeitoDeResponsabilidade`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaComTipoPosteriorIncompativelFazRollbackIntegral`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaFuturaPreservaOriginalEPosteriorSemEfeitoDeResponsabilidade`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaComTipoPosteriorIncompativelFazRollbackIntegral`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.

**CON-023:checkpoint:008** Transferencia usa vigencia reconstruida, incluindo prorrogacao e encurtamento programados.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaPodeUsarVigenciaProrrogadaPorAditamentoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaForaDaVigenciaEncurtadaPorAditamentoFuturoNaoPersiste`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaPodeUsarVigenciaProrrogadaPorAditamentoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaForaDaVigenciaEncurtadaPorAditamentoFuturoNaoPersiste`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.

**CON-023:checkpoint:009** Contexto posterior inclui todas composicoes e tentativas concorrentes nao publicam fatos conflitantes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaRecusaComposicaoPosteriorAindaReferenteAGestaoOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#duasTransferenciasConflitantesConcorrentesNaoPublicamDuasCadeias`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaRecusaComposicaoPosteriorAindaReferenteAGestaoOrigem`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#duasTransferenciasConflitantesConcorrentesNaoPublicamDuasCadeias`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT.xml`; XML SHA-256 `07e9790b7c640fbd7d4014e4dc8fef3b8457a720d101306470d953ce6d74743b`.

### CON-024 — B

Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CON-024:checkpoint:001** Ato posterior com alcance anterior mantém ambas as datas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CON-024:checkpoint:002** Dia anterior ao alcance não fica anulado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

**CON-024:checkpoint:003** Dia do alcance fica ANULADO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.
- `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT.xml`; XML SHA-256 `982f862f699d9553a45e6ac16f77b9bd1055ebb8ee85308af76724ddb3cbf4f4`.

**CON-024:checkpoint:004** Registro possui Instant e autor

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CON-025 — C

Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CON-025:checkpoint:001** Criar identidade do cancelado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**CON-025:checkpoint:002** Excluir cancelado=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CON-025:checkpoint:003** História e motivo permanecem consultáveis

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

### EMP-001 — B

Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**EMP-001:checkpoint:001** Omitir cada campo empresarial permite rascunho mas bloqueia publicação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**EMP-001:checkpoint:002** CNPJ inválido em tamanho rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**EMP-001:checkpoint:003** Fotografia completa persiste

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### EMP-002 — B

Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**EMP-002:checkpoint:001** Antes do evento mantém empresa original

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**EMP-002:checkpoint:002** Na data aplica novos nomes

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**EMP-002:checkpoint:003** Outro contrato não muda

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**EMP-002:checkpoint:004** Fluxo de governança não troca fornecedor publicado livremente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.servico.GovernancaTerceirosContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.servico.GovernancaTerceirosContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.servico.GovernancaTerceirosContratoTest.xml`; XML SHA-256 `8d91ef49a0a45d868de19847793dd94b396cbeb39cc13fefb920d0452e61bb1f`.

### EMP-003 — B

Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**EMP-003:checkpoint:001** Troca real mantém empresa anterior no passado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#trocaDeFornecedorNaoPodeConservarSnapshotsDaEmpresaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#trocaDeFornecedorNaoPodeConservarSnapshotsDaEmpresaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT.xml`; XML SHA-256 `2dcfae194e06c4cdde77103e65e91dea03bf2894f3a62e0d1d9790ef96ee2d59`.

**EMP-003:checkpoint:002** Correção material produz CORRECAO_CNPJ

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#correcaoMaterialCnpjTemFatoProprioPreservaCorteDeConhecimentoEIdempotencia`; localização: `METODO_LOCALIZADO`.

**EMP-003:checkpoint:003** Corte conhecidoAte preserva informação anterior

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#correcaoMaterialCnpjTemFatoProprioPreservaCorteDeConhecimentoEIdempotencia`; localização: `METODO_LOCALIZADO`.

**EMP-003:checkpoint:004** Reenvio exato é idempotente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#correcaoMaterialCnpjTemFatoProprioPreservaCorteDeConhecimentoEIdempotencia`; localização: `METODO_LOCALIZADO`.

**EMP-003:checkpoint:005** Fornecedor escolhido deve corresponder ao CNPJ informado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#fornecedorInformadoDeveConferirComCnpjExplicitoDoInstrumento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#fornecedorInformadoDeveConferirComCnpjExplicitoDoInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT.xml`; XML SHA-256 `2dcfae194e06c4cdde77103e65e91dea03bf2894f3a62e0d1d9790ef96ee2d59`.

### CAT-001 — B

Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CAT-001:checkpoint:001** Tipo é entidade prévia e selecionada por ID, sem aceitar texto livre como substituto.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.

**CAT-001:checkpoint:002** Somente Gestor Local ativo administra Tipos da sua Gestão; ID de outra Gestão é recusado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.ContratoServicoEtapa02Test.xml`; XML SHA-256 `737d5363638211a42b000ccbe8d222cd7ec6d26b45ef022c4b73a03f2176cdaf`.

### CAT-002 — B

Cargo é cadastro próprio administrado pela Gestão Local.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CAT-002:checkpoint:001** Cargo possui identidade/cadastro próprio com validação de nome.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CargoServicoTest#criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.servico.CargoServicoTest#criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.servico.CargoServicoTest.xml`; XML SHA-256 `91b30ce6b403084e88f520716251e8e8e1c4b51e7ec7336ec907d49d25fc51fd`.

**CAT-002:checkpoint:002** Escrita e unicidade nominal são delimitadas pela Gestão Local proprietária.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CargoServicoTest#criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.servico.CargoServicoTest#criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.servico.CargoServicoTest.xml`; XML SHA-256 `91b30ce6b403084e88f520716251e8e8e1c4b51e7ec7336ec907d49d25fc51fd`.

### CAT-003 — B

Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CAT-003:checkpoint:001** Inativar preserva a entidade e sua identidade; leitura histórica continua disponível.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.

**CAT-003:checkpoint:002** Listagens/detalhes autorizados não excluem um catálogo somente por estar inativo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.

### CAT-004 — C

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CAT-004:checkpoint:001** Novos fatos de alocação/vínculo capturam rótulos e configurações da origem efetivamente escolhida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiaTransferenciaRestauradaHttpIT#sucessoraRestauradaUsaMunicipioEGradeDocumentadosNaTransferencia`; localização: `METODO_LOCALIZADO`.

**CAT-004:checkpoint:002** Renomear Gestão/Lotação/Turno/Cargo/Tipo posteriormente não modifica as fotografias dos fatos anteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `InstrumentosFotografiaCatalogoHttpIT#historicoDoRemanejamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.

**CAT-004:checkpoint:003** Uma fotografia antiga nula/vazia continua desconhecida; nenhum nome/configuração é preenchido pelo mestre atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasFotografiasHistoricasAusentesHttpIT#listaDeAlocacoesNaoCompletaRotulosAusentesPeloCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiasHistoricasAusentesHttpIT#motorSemEventosNaoInfereRotulosHistoricosDoMestre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiaTransferenciaRestauradaHttpIT#mapaLegadoSemFotografiaBloqueiaReconstrucaoSemInventarGradeDoCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.

**CAT-004:checkpoint:004** Documentos/relatórios/exportações consomem fotografias por fato; a prova transversal depende também dos lotes contratos e frequência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `InstrumentosFotografiaCatalogoHttpIT#historicoDoAditamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.

### CAT-005 — C

Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CAT-005:checkpoint:001** Inativação de Cargo/Lotação/Turno é recusada enquanto composições, alocações, instrumentos ou transferências válidas cobrem programação futura.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransferenciaLacunasHttpIT#transferenciaProgramadaSemPessoasImpedeInativarCatalogoDoDestino`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#catalogoDaOrigemSemUsoFuturoPodeSerInativadoAposTransferenciaEfetiva`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#catalogoDaOrigemSemUsoFuturoPodeSerInativadoAposTransferenciaEfetiva`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

**CAT-005:checkpoint:002** O tratamento da programação ocorre por comando de cancelamento/retificação preservando o fato original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

### CAT-006 — B

Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CAT-006:checkpoint:001** Reativação explícita preserva o ID do catálogo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**CAT-006:checkpoint:002** O ato grava estado anterior/novo, data efetiva, registro e responsável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT.xml`; XML SHA-256 `6366c8ed27c2232032769472c902bd150fdb4249c4fbf81ce8a039728385aba9`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### CAT-007 — C

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CAT-007:checkpoint:001** Fato retroativo aceita item hoje inativo quando a trilha comprova atividade na data efetiva.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCatalogosEmpresaHttpIT#composicaoOriginalRetroativaUsaCatalogosValidosNoPeriodo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCatalogosEmpresaHttpIT#contratoRetroativoUsaTipoValidoNaDataMesmoInativoHoje`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#composicaoOriginalRetroativaUsaCatalogosValidosNoPeriodo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT.xml`; XML SHA-256 `2dcfae194e06c4cdde77103e65e91dea03bf2894f3a62e0d1d9790ef96ee2d59`.
- `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#contratoRetroativoUsaTipoValidoNaDataMesmoInativoHoje`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT.xml`; XML SHA-256 `2dcfae194e06c4cdde77103e65e91dea03bf2894f3a62e0d1d9790ef96ee2d59`.

**CAT-007:checkpoint:002** Corte de conhecimento exclui transições registradas depois do instante consultado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**CAT-007:checkpoint:003** Antes da primeira origem comprovada, ou sem fonte de origem, disponibilidade é desconhecida e o uso negocial dependente é recusado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#estadoAtualOuAnteriorSemMarcoDeOrigemNaoProvaDisponibilidadeRetroativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosLegadoSemProvaHttpIT#disponibilidadeSemEventoPermaneceDesconhecidaAtePrimeiroFatoDocumentado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosLegadoSemProvaHttpIT#aditamentoAntesDoPrimeiroFatoSemEstadoAnteriorRetornaConflitoDeConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransferenciaLacunasHttpIT#primeiraInativacaoSemFonteNaoInventaDisponibilidadeAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#estadoAtualOuAnteriorSemMarcoDeOrigemNaoProvaDisponibilidadeRetroativa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#disponibilidadeSemEventoPermaneceDesconhecidaAtePrimeiroFatoDocumentado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoAntesDoPrimeiroFatoSemEstadoAnteriorRetornaConflitoDeConhecimento(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoAntesDoPrimeiroFatoSemEstadoAnteriorRetornaConflitoDeConhecimento(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT.xml`; XML SHA-256 `9c9b6d4baac31b4f7091fe8be5cc42ce2229761930a27c2c34d7f46e981dac6e`.

**CAT-007:checkpoint:004** Primeira transição concorrente produz uma única origem e rejeita estado anterior obsoleto sem gravação parcial.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#primeiraTransicaoConcorrenteTemUmaOrigemERejeitaEstadoAnteriorObsoleto`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#primeiraTransicaoConcorrenteTemUmaOrigemERejeitaEstadoAnteriorObsoleto`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**CAT-007:checkpoint:005** Disponibilidade retroativa é documentada por comando canônico escopado com motivo, fonte, autorização e versão esperada, sem certificar período anterior à fonte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#fonteRetroativaDocumentadaPermiteVinculoPassadoComCargoHojeInativo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#primeiraTransicaoAtualEReativacaoConservamIdEHistoriaSemInventarOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#somenteGestorLocalDaPropriaGestaoPodeRegistrarOuPrepararTransicao`; localização: `METODO_LOCALIZADO`.

**CAT-007:checkpoint:006** A inserção retroativa respeita a ordem efetiva e conserva reativação posterior na projeção atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#transicaoRetroativaPreservaReativacaoEfetivaPosteriorMesmoRegistradaAntes`; localização: `METODO_LOCALIZADO`.

**CAT-007:checkpoint:007** Confirmação de ato retroativo exige prévia vigente de versão, trilha e dependências; reenvio da mesma intenção não duplica fato ou versão.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#reenvioDaMesmaIntencaoRetornaMesmoEventoSemExigirVersaoJaConsumida`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#previaObsoletaPorNovoVinculoExigeNovaAnaliseMesmoSemMudarVersaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#requisicaoIncompletaOuNaoConfirmadaNaoProduzFatos`; localização: `METODO_LOCALIZADO`.

**CAT-007:checkpoint:008** Ato incompatível com fatos posteriores ou com programação futura é recusado atomicamente, preservando as fontes existentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#atoRetroativoNaoPodeInvalidarEscolhaPosteriorNemApagarSuaFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTransicoesOperacionaisHttpIT#inativacaoAtualPelaNovaRotaRespeitaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.

### CAT-008 — C

Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**CAT-008:checkpoint:001** Um item atualmente inativo não autoriza programação futura.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PublicacaoCatalogoInativadoHttpIT#publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.PublicacaoCatalogoInativadoHttpIT#publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PublicacaoCatalogoInativadoHttpIT.xml`; XML SHA-256 `52bd51625f0d3c502414ce2822fce9c540bbd02c400c60e65dd9825a4526dbc0`.

**CAT-008:checkpoint:002** Reativação explícita cria nova prova sem apagar o intervalo de inatividade.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PublicacaoCatalogoInativadoHttpIT#publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.
- `com.example.siggtrat.auditoria20261029.PublicacaoCatalogoInativadoHttpIT#publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PublicacaoCatalogoInativadoHttpIT.xml`; XML SHA-256 `52bd51625f0d3c502414ce2822fce9c540bbd02c400c60e65dd9825a4526dbc0`.

### CAT-009 — B

Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CAT-009:checkpoint:001** Catálogo classifica desligamento individual, fim de vigência, encerramento antecipado, anulação e outras causas separadamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.

**CAT-009:checkpoint:002** Cada término usa causa da categoria e Gestão responsável na data do fato; descrição/categoria aplicadas ficam fotografadas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.
- `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT.xml`; XML SHA-256 `dc9d0e7c0b07e30718f82b7be586cae81e5f177b10f228c01a0c36ea82be916a`.

### CMP-001 — B

Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-001:checkpoint:001** Uma entrada cria exatamente uma composição

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CMP-001:checkpoint:002** IDs obrigatórios validados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CMP-001:checkpoint:003** Composição inexistente não é opção de vínculo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#composicaoInexistenteRecusaVinculoSemGravarPessoaReservaOuHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#composicaoInexistenteRecusaVinculoSemGravarPessoaReservaOuHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT.xml`; XML SHA-256 `0bcffdc75af27e0579dc558bd352c836813c3bb2e64795b636c7c74286e46a6c`.

### CMP-002 — B

Não pode haver combinação duplicada no mesmo recorte temporal.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-002:checkpoint:001** Criar duplicata sobreposta=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CMP-002:checkpoint:002** Lista mantém uma entrada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CMP-002:checkpoint:003** Itens repetidos ADI/REM rejeitados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**CMP-002:checkpoint:004** Transações concorrentes serializam pelo contrato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT.xml`; XML SHA-256 `fa92a48bc547fe9388094f4c0ee52517a761589d446165640cb25228f5b46e10`.

### CMP-003 — B

Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-003:checkpoint:001** Publicação soma diferente=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**CMP-003:checkpoint:002** Novo total incompatível com deltas=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#totalDoAditamentoDeveConferirComDeltasSemMutacaoParcial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#totalDoAditamentoDeveConferirComDeltasSemMutacaoParcial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CMP-003:checkpoint:003** Retroativo+posterior mantém total igual à soma

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-003:checkpoint:004** Remanejamento total constante

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.RegraEfetivoContratualTest.xml`; XML SHA-256 `a673f7040ce9bf75228105b7bda830e10340e863f99c94d1a88e6e990aa1e699`.

**CMP-003:checkpoint:005** Somas reais devem coincidir com total sem overflow.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#publicacaoRecusaSomaQueSoCoincideComTotalPorOverflow`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#aditamentoRecusaTotalResultanteQueSofreriaOverflow`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#publicacaoRecusaSomaQueSoCoincideComTotalPorOverflow`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT.xml`; XML SHA-256 `5ea4ee51dc8270c8ad67d0b997e50c4adb4bce649c61080ab95a87a050689861`.
- `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#aditamentoRecusaTotalResultanteQueSofreriaOverflow`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT.xml`; XML SHA-256 `5ea4ee51dc8270c8ad67d0b997e50c4adb4bce649c61080ab95a87a050689861`.

### CMP-004 — B

Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-004:checkpoint:001** Zero retorna permiteNovaAlocacao=false

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-004:checkpoint:002** Inclusão mesmo confirmada como excedente=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-004:checkpoint:003** Histórico da composição permanece

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CMP-005 — B

Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-005:checkpoint:001** Antes do retorno capacidade zero

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-005:checkpoint:002** Na data nova capacidade positiva

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-005:checkpoint:003** Nova alocação permitida somente a partir do retorno

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-005:checkpoint:004** Instrumentos preservados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### CMP-006 — B

Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-006:checkpoint:001** Prévia aponta programação impactada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ImpactoCapacidadeLimitesHttpIT#previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**CMP-006:checkpoint:002** Confirmação=409 se dependência continua

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#remanejamentoNaoZeraComposicaoComVinculoProgramadoDepoisDoEfeito`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**CMP-006:checkpoint:003** Contrato/instrumentos/pessoas não mudam no bloqueio

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**CMP-006:checkpoint:004** Após tratamento explícito pode confirmar

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

### CMP-007 — B

Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-007:checkpoint:001** Impactos incluem ocupação e dependências futuras

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ImpactoCapacidadeLimitesHttpIT#previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**CMP-007:checkpoint:002** Prévia assinada muda quando dependência muda

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**CMP-007:checkpoint:003** Ocupação incompatível bloqueia mesmo confirmarImpacto=true

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT.xml`; XML SHA-256 `46ded17ec6fe922b143e10200c06d5072c21788cb6aace33e0b04f9267ecb398`.
- `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT.xml`; XML SHA-256 `46ded17ec6fe922b143e10200c06d5072c21788cb6aace33e0b04f9267ecb398`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**CMP-007:checkpoint:004** Pessoa não muda automaticamente de combinação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

### CMP-008 — B

Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-008:checkpoint:001** Sem confirmação inclusão excedente rejeita com aviso

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.

**CMP-008:checkpoint:002** Com confirmação/justificativa Gestor autoriza

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.

**CMP-008:checkpoint:003** Acima de 100% persiste

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.

### CMP-009 — B

Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-009:checkpoint:001** Perfil sem gestão não inclui

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#mutacoesDePessoaVinculoEAlocacaoExigemGestorLocalAtivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.

**CMP-009:checkpoint:002** Justificativa obrigatória em excesso

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#consentimentoDeExcedenteSemJustificativaRecusaSemGravar`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#consentimentoDeExcedenteSemJustificativaRecusaSemGravar`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT.xml`; XML SHA-256 `0bcffdc75af27e0579dc558bd352c836813c3bb2e64795b636c7c74286e46a6c`.

**CMP-009:checkpoint:003** Registro vincula autor e prova

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#excedenteAutorizadoPersisteAutoriaDocumentosDataECapacidadeNoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#excedenteAutorizadoPersisteAutoriaDocumentosDataECapacidadeNoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT.xml`; XML SHA-256 `0bcffdc75af27e0579dc558bd352c836813c3bb2e64795b636c7c74286e46a6c`.

**CMP-009:checkpoint:004** Consulta excedente recupera justificativa

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#restauracaoComExcedenteEmDuasCombinacoesExibeJustificativaEmCadaFaixa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CMP-010 — B

Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-010:checkpoint:001** Inclusões justificadas continuam possíveis acima da capacidade

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.

**CMP-010:checkpoint:002** Contagem mostra diferença real

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-010:checkpoint:003** Zero continua vedado por regra diferente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### CMP-011 — B

Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**CMP-011:checkpoint:001** Excedente 2-1 informa 1

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-011:checkpoint:002** desdeQuando coincide com início do episódio

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-011:checkpoint:003** Justificativa presente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#restauracaoComExcedenteEmDuasCombinacoesExibeJustificativaEmCadaFaixa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**CMP-011:checkpoint:004** Aumento a 2 zera excedente sem apagar histórico

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aumentoAbsorveExcedenteSemReescreverEpisodioAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aumentoAbsorveExcedenteSemReescreverEpisodioAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

### RED-001 — B

Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-001:checkpoint:001** 100→90→75 permitido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**RED-001:checkpoint:002** Abaixo de75 sem exceção bloqueado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**RED-001:checkpoint:003** Replay posterior que viola piso gera conflito sem aplicar redução

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.

### RED-002 — B

Limite de 25% é verificado sobre o total do contrato, não individualmente por composição.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-002:checkpoint:001** Reduzir composição25→0 em total101 para76 permitido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**RED-002:checkpoint:002** Total ainda deve respeitar piso

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**RED-002:checkpoint:003** Ocupação deve ser tratada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

### RED-003 — B

Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-003:checkpoint:001** Zero sem dependentes e piso respeitado é permitido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**RED-003:checkpoint:002** Zero com futuro dependente bloqueia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#remanejamentoNaoZeraComposicaoComVinculoProgramadoDepoisDoEfeito`; localização: `METODO_LOCALIZADO`.

**RED-003:checkpoint:003** Não mover pessoas automaticamente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

### RED-004 — B

Aumento contratual não possui limite de 25%.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-004:checkpoint:001** 100→140 aceito

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**RED-004:checkpoint:002** Aumento superior a25 não exige exceção de redução

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**RED-004:checkpoint:003** Soma da composição acompanha aumento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### RED-005 — B

Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-005:checkpoint:001** 74 para base100=409 comum

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**RED-005:checkpoint:002** Flag sozinha sem fundamento rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**RED-005:checkpoint:003** Exceção completa passa com Gestão responsável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### RED-006 — B

O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-006:checkpoint:001** 100→140→75 permitido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**RED-006:checkpoint:002** Base permanece100

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.RegraEfetivoContratualTest.xml`; XML SHA-256 `a673f7040ce9bf75228105b7bda830e10340e863f99c94d1a88e6e990aa1e699`.

**RED-006:checkpoint:003** Retificar/cancelar remove efeito antigo do cálculo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**RED-006:checkpoint:004** Replay revalida piso após retirada de aumento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.

**RED-006:checkpoint:005** Piso original continua valido em toda faixa numerica admitida pelo modelo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#reducaoDeEfetivoGrandeContinuaSujeitaAoPisoOriginal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.ConclusaoEfetivoContratualTest.xml`; XML SHA-256 `bec4c8073a04d942d37b3614cefa68babde42f69f5ea5b41733f8f5234ecbe05`.
- `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#reducaoDeEfetivoGrandeContinuaSujeitaAoPisoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT.xml`; XML SHA-256 `5ea4ee51dc8270c8ad67d0b997e50c4adb4bce649c61080ab95a87a050689861`.

### RED-007 — B

Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-007:checkpoint:001** Base101 exige76

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato101ArredondaPisoPara76`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato101ArredondaPisoPara76`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.RegraEfetivoContratualTest.xml`; XML SHA-256 `a673f7040ce9bf75228105b7bda830e10340e863f99c94d1a88e6e990aa1e699`.

**RED-007:checkpoint:002** Base100 exige75

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.RegraEfetivoContratualTest.xml`; XML SHA-256 `a673f7040ce9bf75228105b7bda830e10340e863f99c94d1a88e6e990aa1e699`.

**RED-007:checkpoint:003** Resultado75 com base101 bloqueia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**RED-007:checkpoint:004** Arredondamento conserva precisao no maior inteiro admitido.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.ConclusaoEfetivoContratualTest.xml`; XML SHA-256 `bec4c8073a04d942d37b3614cefa68babde42f69f5ea5b41733f8f5234ecbe05`.

### RED-008 — B

Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**RED-008:checkpoint:001** Falta fundamento=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**RED-008:checkpoint:002** Falta concessão=403

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**RED-008:checkpoint:003** Sem confirmação=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**RED-008:checkpoint:004** Exceção documentada autorizada persiste

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**RED-008:checkpoint:005** Flag de exceção em redução normal não autoriza violação futura por replay

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.

### ADI-001 — B

Contrato pode possuir vários aditamentos sem apagar registros anteriores.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-001:checkpoint:001** Dois instrumentos aparecem na lista

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.

**ADI-001:checkpoint:002** Reenvio não duplica

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade`; localização: `METODO_LOCALIZADO`.

**ADI-001:checkpoint:003** Retificação versão2 mantém versão1

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### ADI-002 — B

Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-002:checkpoint:001** Campos obrigatórios ausentes rejeitam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**ADI-002:checkpoint:002** Sem fonte=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade`; localização: `METODO_LOCALIZADO`.

**ADI-002:checkpoint:003** Autor/registro são servidor

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**ADI-002:checkpoint:004** Assinatura opcional preservada quando informada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### ADI-003 — B

Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-003:checkpoint:001** Aumento100→140 registra anterior100,variação40,resultante140

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**ADI-003:checkpoint:002** Histórico mantém esses valores após novos fatos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

### ADI-004 — B

Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-004:checkpoint:001** Aumento e redução válidos aceitos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**ADI-004:checkpoint:002** Abaixo piso comum=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada`; localização: `METODO_LOCALIZADO`.

**ADI-004:checkpoint:003** Redução ocupada/futuro órfão=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**ADI-004:checkpoint:004** Falha deixa toda transação intacta

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.
- `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT.xml`; XML SHA-256 `46ded17ec6fe922b143e10200c06d5072c21788cb6aace33e0b04f9267ecb398`.

### ADI-005 — B

Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-005:checkpoint:001** Fato antigo digitado depois produz efeito no dia certo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoRetroativoRespeitaAssociacaoPublicadaAposRemocaoNoCatalogoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoRetroativoRespeitaAssociacaoPublicadaAposRemocaoNoCatalogoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

**ADI-005:checkpoint:002** Posterior válido continua aplicado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-005:checkpoint:003** Nova frequência após prévia invalida token

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#novaFrequenciaInvalidaTokenDeAditamentoRetroativo`; localização: `METODO_LOCALIZADO`.

**ADI-005:checkpoint:004** Versões anteriores preservadas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT.xml`; XML SHA-256 `b841692b96e8fc7276a796bcd56329ec4f163174289f5343b45ea5200e4eb2e1`.

### ADI-006 — B

Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-006:checkpoint:001** Cancelar futuro remove efeito

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.

**ADI-006:checkpoint:002** Cancelar efetivo rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**ADI-006:checkpoint:003** Original e cancelamento auditáveis

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**ADI-006:checkpoint:004** Dependente futuro incompatível bloqueia cancelamento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarAditamentoQueReativouComposicaoComVinculoFuturoEhBloqueadoSemMutacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarAditamentoQueReativouComposicaoComVinculoFuturoEhBloqueadoSemMutacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT.xml`; XML SHA-256 `fc9164b5d56ebebfa51d19de0574f5310c253895261f6121e6a84c49c42cee22`.

### ADI-007 — B

Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-007:checkpoint:001** Versão2 com RETIFICA preserva versão1

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**ADI-007:checkpoint:002** Retificação subsequente conserva cadeia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**ADI-007:checkpoint:003** Capacidade omitida na retificação é revalidada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT.xml`; XML SHA-256 `46ded17ec6fe922b143e10200c06d5072c21788cb6aace33e0b04f9267ecb398`.

**ADI-007:checkpoint:004** Resultado temporal usa versão vigente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### ADI-008 — B

Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-008:checkpoint:001** Instrumento posterior continua na lista

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-008:checkpoint:002** Incompatibilidade gera pendência ligada ao evento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-008:checkpoint:003** Não aplica delta incompatível parcialmente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.
- `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT.xml`; XML SHA-256 `04b5b0c772ceb5466706f6cad83379082045c6ef30b2950066822894e2a9f292`.

**ADI-008:checkpoint:004** Com dependente operacional impede operação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

### ADI-009 — B

Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-009:checkpoint:001** Sem precedência colisão=409

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-009:checkpoint:002** SUCEDE válido decide valor

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-009:checkpoint:003** Sucessores bifurcados sem relação entre si bloqueiam

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles`; localização: `METODO_LOCALIZADO`.

**ADI-009:checkpoint:004** Recortes distintos coexistem

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### ADI-010 — B

Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ADI-010:checkpoint:001** Campos estado/ativo no JSON não mudam estado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**ADI-010:checkpoint:002** RETIFICA/SUBSTITUI/CANCELA apontando evento de ciclo é recusado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[1]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[2]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[3]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[4]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[5]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[6]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

**ADI-010:checkpoint:003** SUCEDE misto válido pode ordenar sem invalidar estado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#precedenciaDeOrdemEntreTiposDiferentesContinuaPermitida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#precedenciaDeOrdemEntreTiposDiferentesContinuaPermitida`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

### REM-001 — B

Remanejamento é fato próprio, identificado, datado, justificado e auditado.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-001:checkpoint:001** Registro contém identificação/data/justificativa/autor

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-001:checkpoint:002** Itens guardam anterior/variação/resultante

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**REM-001:checkpoint:003** História usa tipo REMANEJAMENTO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### REM-002 — B

Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-002:checkpoint:001** -5/+5 preserva100

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.RegraEfetivoContratualTest.xml`; XML SHA-256 `a673f7040ce9bf75228105b7bda830e10340e863f99c94d1a88e6e990aa1e699`.

**REM-002:checkpoint:002** -5/+4 rejeita400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero`; localização: `METODO_LOCALIZADO`.

**REM-002:checkpoint:003** Replay inválido não aplica metade da redistribuição

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT.xml`; XML SHA-256 `946c23d546e112a09e9f23d80a2ea91cd67c6069b96c31d2be921acf7c5744e8`.

**REM-002:checkpoint:004** Soma zero deve ser matematica, nao resultado do descarte de bits altos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesQueSomamDoisElevadoA32NaoSaoTratadasComoRedistribuicaoNeutra`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesGrandesRealmenteCompensadasPreservamTotal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#remanejamentoRecusaVariacoesCujaSomaRealNaoEhZero`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesQueSomamDoisElevadoA32NaoSaoTratadasComoRedistribuicaoNeutra`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.ConclusaoEfetivoContratualTest.xml`; XML SHA-256 `bec4c8073a04d942d37b3614cefa68babde42f69f5ea5b41733f8f5234ecbe05`.
- `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesGrandesRealmenteCompensadasPreservamTotal`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.regra.ConclusaoEfetivoContratualTest.xml`; XML SHA-256 `bec4c8073a04d942d37b3614cefa68babde42f69f5ea5b41733f8f5234ecbe05`.
- `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#remanejamentoRecusaVariacoesCujaSomaRealNaoEhZero`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT.xml`; XML SHA-256 `5ea4ee51dc8270c8ad67d0b997e50c4adb4bce649c61080ab95a87a050689861`.

### REM-003 — B

Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-003:checkpoint:001** Composição inexistente=400

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero`; localização: `METODO_LOCALIZADO`.

**REM-003:checkpoint:002** Vigência fora do contrato rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**REM-003:checkpoint:003** Chave repetida rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-003:checkpoint:004** Nenhuma nova composição criada pelo REM

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

### REM-004 — B

Remanejamento pode ser retificado/versionado, preservando original e cadeia completa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-004:checkpoint:001** Versão2 mantém original RETIFICADO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-004:checkpoint:002** Repetir retificação conserva cadeia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-004:checkpoint:003** Trocar destino não pode retirar capacidade de futuro ocupado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT.xml`; XML SHA-256 `46ded17ec6fe922b143e10200c06d5072c21788cb6aace33e0b04f9267ecb398`.

**REM-004:checkpoint:004** Não invalidar fato de outro domínio

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[1]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[2]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[3]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[4]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[5]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(TipoEventoCicloVidaContrato, TipoRelacaoPrecedenciaNegocial)[6]`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

### REM-005 — B

Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-005:checkpoint:001** REM sem anexo e sem SEI aceita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-005:checkpoint:002** Fonte informada permanece vinculada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.DocumentosInstrumentosPersistenciaHttpIT#remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-005:checkpoint:003** Ausência de fonte não causa erro

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.

### REM-006 — B

Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REM-006:checkpoint:001** Composição histórica redistribuída

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**REM-006:checkpoint:002** Habilitação segue INATIVO

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**REM-006:checkpoint:003** Nova operação no bloqueio rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

### REM-007 — C

Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**REM-007:checkpoint:001** Original futuro cancelável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.

**REM-007:checkpoint:002** Efetivo já iniciado rejeita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT.xml`; XML SHA-256 `380afbd022a2ea43ea45438332ea62bc14655a54fd999547b965cbf0d4482ed4`.

**REM-007:checkpoint:003** Posição futura não aplica cancelado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

**REM-007:checkpoint:004** Com futuro dependente incompatível cancelação bloqueia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT.xml`; XML SHA-256 `fc9164b5d56ebebfa51d19de0574f5310c253895261f6121e6a84c49c42cee22`.

**REM-007:checkpoint:005** Histórico conserva original+cancelamento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.
- `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT.xml`; XML SHA-256 `7249505cef5e842cbc29b59b46b6bebd92b0f05ea336ad5aad90fde63a580334`.

### DOC-001 — B

Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-001:checkpoint:001** Publicação exige SEI, anexo ou ambos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `DocumentosContratuaisHttpIT#documentoRascunhoExclusivoPodeSerExcluidoEPublicacaoAceitaSomenteAnexo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**DOC-001:checkpoint:002** Rejeitar fonte vazia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**DOC-001:checkpoint:003** Anexo suficiente sem SEI e SEI suficiente sem arquivo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRascunhoExclusivoPodeSerExcluidoEPublicacaoAceitaSomenteAnexo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

### DOC-002 — B

Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-002:checkpoint:001** Aditamento deve ter fonte documental válida

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `DocumentosInstrumentosPersistenciaHttpIT#aditamentoSemSeiPublicaAnexoPreparadoEVinculaFonteAoInstrumentoCorreto`; localização: `METODO_LOCALIZADO`.

**DOC-002:checkpoint:002** Fonte acompanha fato e identidade da versão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosInstrumentosPersistenciaHttpIT#aditamentoSemSeiPublicaAnexoPreparadoEVinculaFonteAoInstrumentoCorreto`; localização: `METODO_LOCALIZADO`.

**DOC-002:checkpoint:003** Sem fonte recusar gravação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade`; localização: `METODO_LOCALIZADO`.

### DOC-003 — B

Remanejamento pode registrar SEI e mantém anexo opcional.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-003:checkpoint:001** Remanejamento aceita ausência de arquivo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `DocumentosInstrumentosPersistenciaHttpIT#remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo`; localização: `METODO_LOCALIZADO`.

**DOC-003:checkpoint:002** SEI opcional permanece vinculado ao fato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosInstrumentosPersistenciaHttpIT#remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo`; localização: `METODO_LOCALIZADO`.

**DOC-003:checkpoint:003** Cadeia de versões preservada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo`; localização: `METODO_LOCALIZADO`.

### DOC-004 — B

Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-004:checkpoint:001** SEI não tem unicidade de contrato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**DOC-004:checkpoint:002** Dois contratos com mesma referência são publicados com identidades próprias

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

### DOC-005 — B

Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-005:checkpoint:001** Nova referência SEI cria versão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-005:checkpoint:002** Referência anterior permanece consultável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-005:checkpoint:003** Motivo e autor são registrados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-005:checkpoint:004** Reenvio com intenção divergente não muda a cadeia

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentoComprobatorioServicoEtapa08Test#idempotenciaDaRetificacaoComparaDocumentoAlvo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test#idempotenciaDaRetificacaoComparaDocumentoAlvo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test.xml`; XML SHA-256 `50325a7aad3a9913b52ecbfea402db612b190cfa20a46c3139cda33dc6bb02bf`.

### DOC-006 — B

Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-006:checkpoint:001** Anexo pode chegar após publicação com SEI

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-006:checkpoint:002** A nova versão preserva referência SEI e vínculo anterior

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-006:checkpoint:003** Download confere checksum do arquivo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentoComprobatorioServicoEtapa08Test#downloadComArquivoAdulteradoEhBloqueadoPorChecksum`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test#downloadComArquivoAdulteradoEhBloqueadoPorChecksum`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test.xml`; XML SHA-256 `50325a7aad3a9913b52ecbfea402db612b190cfa20a46c3139cda33dc6bb02bf`.

### DOC-007 — B

Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-007:checkpoint:001** Documento de fato publicado não é excluído fisicamente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.

**DOC-007:checkpoint:002** Invalidação/substituição preserva metadados e bytes

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaProvaPreservadaHttpIT#retiradaDaOcorrenciaPreservaFatoEArquivoJaReferenciadosPelaCadeiaEEmissao`; localização: `METODO_LOCALIZADO`.

**DOC-007:checkpoint:003** Cancelar ocorrência preserva fonte e permite auditoria autorizada

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaProvaPreservadaHttpIT#retiradaDaOcorrenciaPreservaFatoEArquivoJaReferenciadosPelaCadeiaEEmissao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento`; localização: `METODO_LOCALIZADO`.

### DOC-008 — B

Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-008:checkpoint:001** Rascunho nunca publicado admite descarte transacional

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT.xml`; XML SHA-256 `09a2d8447b57cc1275a843108f6770efa64e9f39278b41f2341e8922b56a553b`.
- `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT.xml`; XML SHA-256 `09a2d8447b57cc1275a843108f6770efa64e9f39278b41f2341e8922b56a553b`.

**DOC-008:checkpoint:002** Preservar dependência que possui valor documental externo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosRascunhoCommitRollbackHttpIT#referenciaExternaNoRascunhoImpedeDescarteEPreservaMetadadosEArquivo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#referenciaExternaNoRascunhoImpedeDescarteEPreservaMetadadosEArquivo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT.xml`; XML SHA-256 `09a2d8447b57cc1275a843108f6770efa64e9f39278b41f2341e8922b56a553b`.

**DOC-008:checkpoint:003** Exclusão física só após commit

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT.xml`; XML SHA-256 `09a2d8447b57cc1275a843108f6770efa64e9f39278b41f2341e8922b56a553b`.
- `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT.xml`; XML SHA-256 `09a2d8447b57cc1275a843108f6770efa64e9f39278b41f2341e8922b56a553b`.

### DOC-009 — B

Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DOC-009:checkpoint:001** Cada anexo possui classificação própria

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#uploadValidaClassificacaoFormatoESigiloPorEscopo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#classificacaoInvalidaOuQuantidadeIncompativelNaoPublicaArquivoNemFato`; localização: `METODO_LOCALIZADO`.

**DOC-009:checkpoint:002** NEGOCIAL e PESSOAL_RESTRITO têm políticas diferentes

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento`; localização: `METODO_LOCALIZADO`.

**DOC-009:checkpoint:003** Metadados também respeitam classificação

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**DOC-009:checkpoint:004** Classificação explícita por anexo; legado sem classificação tratado como pessoal/restrito; hash SHA-256 dos novos bytes; metadados pessoais omitidos de consultas minimizadas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#classificacaoInvalidaOuQuantidadeIncompativelNaoPublicaArquivoNemFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#anexoLegadoSemClasseOuHashMantemRestricaoENaoRecebeCertificacaoRetroativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento`; localização: `METODO_LOCALIZADO`.

### DOC-010 — C

Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**DOC-010:checkpoint:001** Conteúdo restrito exige necessidade e perfil/concessão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado`; localização: `METODO_LOCALIZADO`.

**DOC-010:checkpoint:002** Operacional não lê documento pessoal

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**DOC-010:checkpoint:003** Acesso permitido é auditado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado`; localização: `METODO_LOCALIZADO`.

**DOC-010:checkpoint:004** Revogação/expiração aplica-se a cada leitura

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**DOC-010:checkpoint:005** Download revalida todos os dias do contexto do fato, perfil, concessão vigente e finalidade; registra uso auditável. Revogação não é contornada por cache. Operacional não recebe documento pessoal; Presidente somente mediante permissão explícita.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado`; localização: `METODO_LOCALIZADO`.

### COL-001 — B

Nome e CPF são obrigatórios.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-001:checkpoint:001** Cadastro integrado sem nome/CPF é recusado sem persistência parcial.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

**COL-001:checkpoint:002** Nome não vazio é normalizado e preservado na identidade canônica.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

### COL-002 — B

RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-002:checkpoint:001** RG/CTPS/NIT-PIS/data de nascimento/e-mail podem ser omitidos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.

**COL-002:checkpoint:002** Quando informados, os opcionais são persistidos no cadastro de Pessoa e expostos conforme perfil.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.

### COL-003 — B

Endereço é mantido e opcional.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-003:checkpoint:001** Ausência de endereço é aceita.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.

**COL-003:checkpoint:002** Endereço informado é mantido no cadastro; remoção exige ação explícita.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.

### COL-004 — B

Dados bancários foram removidos do cadastro deste fluxo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-004:checkpoint:001** Cadastro canônico de Pessoa e seus contratos de entrada/saída não incluem dados bancários.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

### COL-005 — B

CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-005:checkpoint:001** CPF valida tamanho e dígitos verificadores e rejeita sequências repetidas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

**COL-005:checkpoint:002** CPF é normalizado sem pontuação antes da persistência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

**COL-005:checkpoint:003** Unicidade global é garantida inclusive em cadastros concorrentes no MySQL.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

### COL-006 — B

CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-006:checkpoint:001** Atualização comum de opcionais não permite alterar CPF.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-006:checkpoint:002** Correção excepcional exige Gestor Local autorizado no escopo, motivo, evidência, autorização e impactos declarados.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-006:checkpoint:003** Novo CPF é validado; ato preserva anterior/novo/responsável/data de registro sem fundir identidade.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-006:checkpoint:004** CPF anterior deixa de ser identificador ativo, podendo ser usado por titular distinto após validação sem apropriar a história da Pessoa original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### COL-007 — B

Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-007:checkpoint:001** Correção para CPF ativo de outra Pessoa é recusada atomicamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.

**COL-007:checkpoint:002** Projeção legada coincidente por CPF não é adotada sem associação canônica explícita.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.

### COL-009 — B

Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-009:checkpoint:001** Nome não pode ser alterado pelo fluxo comum de opcionais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-009:checkpoint:002** Correção excepcional auditada mantém o mesmo Pessoa.id e fotografa anterior/novo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### COL-010 — C

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**COL-010:checkpoint:001** Cadastro atual e projeção operacional mostram o nome corrigido.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-010:checkpoint:002** Novos fatos preservam o nome fotografado e a correção atual não muda eventos/documentos anteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiasHistoricasAusentesHttpIT#historicoContratualNaoCompletaNomeAusenteAposCorrecaoDaPessoa`; localização: `METODO_LOCALIZADO`.

**COL-010:checkpoint:003** Consulta temporal separa nomeAtual de nomeFotografia e aplica os cortes efetivo e de conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-010:checkpoint:004** Registro antigo sem fotografia nominal permanece desconhecido; não se alega recuperação de dado ausente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasFotografiasHistoricasAusentesHttpIT#historicoContratualNaoCompletaNomeAusenteAposCorrecaoDaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### COL-011 — B

Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-011:checkpoint:001** Recontratação com pessoaId preserva campos opcionais omitidos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-011:checkpoint:002** Limpeza usa indicadores explícitos, mantendo os demais opcionais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaServicoEtapa06Test.xml`; XML SHA-256 `ca582ef62902caa4ce834089675b52a257ce6e63ccb0c781d1a7e78998da085c`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### COL-012 — B

Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-012:checkpoint:001** Dados atuais pertencem à Pessoa canônica; histórico usa dados fotografados no fato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-012:checkpoint:002** Ausência de fotografia é distinta de valor atual e nunca é completada implicitamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### COL-013 — B

Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**COL-013:checkpoint:001** Uma Pessoa é identificada globalmente por CPF atual e ID estável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**COL-013:checkpoint:002** Recontratação reutiliza essa Pessoa e cria outro Vínculo, sem duplicar identidade ou projeção.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### MAT-001 — B

Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**MAT-001:checkpoint:001** Matrícula reside no Vínculo e nas reservas por Contrato, não no cadastro permanente da Pessoa.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalVinculoEtapa06Test#alteracaoDeMatriculaSoValeAPartirDaDataEfetiva`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#alteracaoDeMatriculaSoValeAPartirDaDataEfetiva`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

**MAT-001:checkpoint:002** Consulta temporal aplica cada alteração de matrícula na data efetiva.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalVinculoEtapa06Test#alteracaoDeMatriculaSoValeAPartirDaDataEfetiva`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#alteracaoDeMatriculaSoValeAPartirDaDataEfetiva`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

### MAT-002 — B

Matrícula é opcional no vínculo e única dentro do contrato.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**MAT-002:checkpoint:001** Matrícula é opcional na ausência de política obrigatória.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#matriculaEhNormalizadaNoDominioDoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#matriculaEhNormalizadaNoDominioDoVinculo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

**MAT-002:checkpoint:002** Valor normalizado não pode ser atribuído a outro vínculo do mesmo Contrato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#matriculaEhNormalizadaNoDominioDoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#matriculaEhNormalizadaNoDominioDoVinculo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

### MAT-003 — B

Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**MAT-003:checkpoint:001** Desligamento mantém a reserva histórica.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.

**MAT-003:checkpoint:002** Alteração mantém também a reserva anterior.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.

**MAT-003:checkpoint:003** Cancelamento de programação não libera a matrícula.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.

### MAT-004 — B

Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**MAT-004:checkpoint:001** Novo vínculo da mesma Pessoa não pode reutilizar matrícula já reservada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.

**MAT-004:checkpoint:002** Matrícula diferente permite recontratação válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.

### MAT-005 — B

Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**MAT-005:checkpoint:001** Política padrão é opcional; contrato pode fixar regra ou herdar a do Tipo fotografado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

**MAT-005:checkpoint:002** Inclusão/ativação e toda faixa atingida por política obrigatória exigem matrícula válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

**MAT-005:checkpoint:003** Mudança futura/transferência revalida vínculos existentes e programados nas fronteiras pertinentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

### VIN-001 — B

Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-001:checkpoint:001** Pessoa nova, primeiro Vínculo e Alocação inicial são gravados na mesma transação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

**VIN-001:checkpoint:002** Falha posterior reverte todos os componentes e permite nova tentativa íntegra.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.

### VIN-002 — B

Antiga data de admissão é substituída por início efetivo do vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-002:checkpoint:001** O fato de início usa dataInicio efetiva do Vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.

**VIN-002:checkpoint:002** Pesquisa de início não usa mudança de alocação nem dataAdmissao da projeção como novo vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita`; localização: `METODO_LOCALIZADO`.

### VIN-003 — B

Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-003:checkpoint:001** Sobreposição é verificada globalmente por Pessoa, inclusive contratos/Gestões distintos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**VIN-003:checkpoint:002** Locks e revalidação impedem sobreposição em cadastro/retificações concorrentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

### VIN-004 — B

Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-004:checkpoint:001** Fim inclusivo em D permite vínculo seguinte em D+1.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

**VIN-004:checkpoint:002** Fim em D não permite outro vínculo iniciar em D.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

### VIN-005 — C

Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-005:checkpoint:001** Vínculo com início futuro retorna PROGRAMADO antes do início.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

**VIN-005:checkpoint:002** Programação não torna o colaborador operacionalmente ativo antes da data efetiva.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

### VIN-006 — B

Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-006:checkpoint:001** Programação pode ser cancelada antes do início.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualServicoEtapa06Test#cancelamentoDeProgramacaoNaoViraDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualServicoEtapa06Test#cancelamentoDeProgramacaoNaoViraDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualServicoEtapa06Test.xml`; XML SHA-256 `fd00b91d48aaafeea44ffb668482ad66db32204d2413c3ad1deb7eaf82101dce`.

**VIN-006:checkpoint:002** Cancelamento não grava desligamento individual de relação nunca efetivada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualServicoEtapa06Test#cancelamentoDeProgramacaoNaoViraDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualServicoEtapa06Test#cancelamentoDeProgramacaoNaoViraDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualServicoEtapa06Test.xml`; XML SHA-256 `fd00b91d48aaafeea44ffb668482ad66db32204d2413c3ad1deb7eaf82101dce`.

### VIN-007 — B

Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-007:checkpoint:001** Cancelamento/retificação preservam evento original e relacionamento de precedência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

**VIN-007:checkpoint:002** Corte de conhecimento anterior recupera a programação então conhecida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.

### VIN-008 — C

Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-008:checkpoint:001** Vínculo aberto perde operação no dia seguinte ao fim natural do Contrato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PendenciaAdministrativaServicoEtapa11Test#fimNaturalCriaPendenciaAdministrativaPorVinculoAberto`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.
- `com.example.siggtrat.etapa11.PendenciaAdministrativaServicoEtapa11Test#fimNaturalCriaPendenciaAdministrativaPorVinculoAberto`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.PendenciaAdministrativaServicoEtapa11Test.xml`; XML SHA-256 `d036cc89af9091d86ff662b15e639868df06b1c230fa9241d65a24c3bd92df09`.

**VIN-008:checkpoint:002** Pendência administrativa identifica o vínculo ainda aberto sem encerrá-lo por rotina técnica.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PendenciaAdministrativaServicoEtapa11Test#fimNaturalCriaPendenciaAdministrativaPorVinculoAberto`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test.xml`; XML SHA-256 `0027d29baf379b6db6d785a37dfcb5fffe752e9e0214e14a583ced7cb77a5013`.
- `com.example.siggtrat.etapa11.PendenciaAdministrativaServicoEtapa11Test#fimNaturalCriaPendenciaAdministrativaPorVinculoAberto`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.PendenciaAdministrativaServicoEtapa11Test.xml`; XML SHA-256 `d036cc89af9091d86ff662b15e639868df06b1c230fa9241d65a24c3bd92df09`.

### VIN-009 — C

Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-009:checkpoint:001** Tratamento da pendência usa o fim efetivo do Contrato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.

**VIN-009:checkpoint:002** Desligamento real anterior é preservado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.

### VIN-010 — C

Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-010:checkpoint:001** Fim natural usa categoria FIM_VIGENCIA_CONTRATO do catálogo da Gestão competente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[2]`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.

**VIN-010:checkpoint:002** Gestor Local administra o motivo, e o ato preserva descrição/categoria aplicadas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[2]`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.

### VIN-011 — B

Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-011:checkpoint:001** Data efetiva de encerramento natural não ultrapassa fim da vigência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

**VIN-011:checkpoint:002** Registro administrativo posterior conserva Instant próprio e responsável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

### VIN-012 — B

Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-012:checkpoint:001** Prorrogação posterior não reabre vínculo já encerrado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.

**VIN-012:checkpoint:002** Reabertura/retificação requer comando explícito com impacto e revalidações.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.

### VIN-013 — B

Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**VIN-013:checkpoint:001** Inativação administrativa não altera término do Vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**VIN-013:checkpoint:002** Reativação dentro da mesma vigência retoma somente vínculos temporalmente válidos, sem criar outro vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### VIN-014 — C

Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-014:checkpoint:001** Retificação de início exige justificativa/evidência/autorização e prévia confirmada válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**VIN-014:checkpoint:002** Revalida sobreposição global, matrícula e alocação inclusive eventos posteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**VIN-014:checkpoint:003** Frequências e relatórios atingidos são reavaliados, com pendências e invalidação quando cabíveis.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**VIN-014:checkpoint:004** Evento original e versões posteriores são preservados e reconstruíveis por conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT.xml`; XML SHA-256 `5f95aea354e68a684bede6558d2a5c046ecd808338d4175550847197355371a4`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

### VIN-015 — C

Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**VIN-015:checkpoint:001** Encerramento antecipado encerra vínculos abertos na mesma data efetiva.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**VIN-015:checkpoint:002** Término individual anterior é preservado; causa específica é fotografada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### ALO-001 — B

Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-001:checkpoint:001** Cada Alocação relaciona Vínculo, Lotação, Turno e Cargo em faixa temporal.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

**ALO-001:checkpoint:002** Somente combinação presente na composição válida no recorte é selecionável.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

### ALO-002 — B

Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-002:checkpoint:001** Mudança de Lotação/Turno/Cargo cria faixa sucessora e encerra anterior em D-1.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

**ALO-002:checkpoint:002** A Pessoa e o Vínculo permanecem os mesmos; não é novo desligamento/recontratação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

### ALO-003 — B

Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-003:checkpoint:001** Mudança retroativa altera apenas o trecho alcançado e preserva eventos posteriores válidos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

**ALO-003:checkpoint:002** Incompatibilidades posteriores são apresentadas e impedem confirmação sem tratamento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

### ALO-004 — B

Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-004:checkpoint:001** Há no máximo uma faixa REGISTRADA por Pessoa/dia.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

**ALO-004:checkpoint:002** Nova informação no mesmo início substitui/retifica a faixa anterior preservada, sem sobreposição.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.

### ALO-005 — B

Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-005:checkpoint:001** Mudança e desligamento no mesmo dia exigem precedência explícita do último dia.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT.xml`; XML SHA-256 `8edd5de85c3c9da5aaf5c7266ef8c818fbf83c39bff83919d36b332a2c1a6731`.
- `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT.xml`; XML SHA-256 `8edd5de85c3c9da5aaf5c7266ef8c818fbf83c39bff83919d36b332a2c1a6731`.

**ALO-005:checkpoint:002** A regra vale nas duas ordens de lançamento e também nas retificações.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT.xml`; XML SHA-256 `8edd5de85c3c9da5aaf5c7266ef8c818fbf83c39bff83919d36b332a2c1a6731`.
- `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT.xml`; XML SHA-256 `8edd5de85c3c9da5aaf5c7266ef8c818fbf83c39bff83919d36b332a2c1a6731`.

### ALO-006 — B

Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-006:checkpoint:001** Jornada que atravessa meia-noite pertence à data de início.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**ALO-006:checkpoint:002** A composição/alocação usada é a válida nessa data, sem deslocar o fato ao dia seguinte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

### ALO-007 — C

Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**ALO-007:checkpoint:001** Toda faixa operacional de Vínculo fica coberta sem lacunas por exatamente uma Alocação válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoAbertoNaoPodeAtravessarZeramentoFuturoRegistradoAntesDoCadastro`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#faixaSemCoberturaNaDataEfetivaEhBloqueada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#faixaSemCoberturaNaDataEfetivaEhBloqueada`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

**ALO-007:checkpoint:002** Mudanças de início/fim, prorrogação e programação posterior revalidam cobertura/composição nas fronteiras.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoAbertoNaoPodeAtravessarZeramentoFuturoRegistradoAntesDoCadastro`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#faixaSemCoberturaNaDataEfetivaEhBloqueada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#faixaSemCoberturaNaDataEfetivaEhBloqueada`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.
- `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT.xml`; XML SHA-256 `9956e3d19280f7641e77830cc542a11924962f5ac57daf9d233054e2a3391dc4`.

**ALO-007:checkpoint:003** Reprojeção após retificar/cancelar transferência preserva decisões independentes válidas e rejeita incompatíveis atomicamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

### ALO-008 — B

Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**ALO-008:checkpoint:001** Inativação administrativa conserva a faixa de Alocação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**ALO-008:checkpoint:002** Retomada exige faixa ainda válida, não sucedida e composição válida; não cria nova faixa só por reativar.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test.xml`; XML SHA-256 `09cb2ed4c1282542dbfa04bc511c7d3bc735081b973d091808428f9c984fcf5d`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### DES-001 — B

Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DES-001:checkpoint:001** Término pertence ao Vínculo específico, sem alterar término do Contrato como substituto.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.

### DES-002 — B

Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DES-002:checkpoint:001** Data de desligamento é inclusiva para o Vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

**DES-002:checkpoint:002** Inatividade começa em D+1; último dia não é excluído da cobertura.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test.xml`; XML SHA-256 `b826652c92366978992edcc0d4e7e47ee034d738b6c622daac2fd3935b16c5cc`.

### DES-003 — B

Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DES-003:checkpoint:001** Desligamento retroativo usa a data real do fato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

**DES-003:checkpoint:002** Registro posterior é auditado e não reescreve fatos existentes silenciosamente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test.xml`; XML SHA-256 `37e3d09be9cb5743e2a93a4438f29bbfb00d4343f142430a09d0078c5ca62529`.

### DES-004 — C

Desligamento possui motivo de catálogo, responsável e data/hora de registro.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**DES-004:checkpoint:001** Desligamento exige causa válida de catálogo e Gestão responsável na data.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[2]`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.

**DES-004:checkpoint:002** Responsável e registro são preservados; renomeação posterior não altera a causa aplicada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.
- `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean)[2]`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT.xml`; XML SHA-256 `268aa743f538e971a462e7a0eac4d0ef425609b04b18f0b596356f9e306635e3`.

### DES-005 — C

Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**DES-005:checkpoint:001** Retificação mantém original e novo evento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasDesligamentoExcedenteFuturoHttpIT#retificarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**DES-005:checkpoint:002** Revalida toda a linha temporal quanto a sobreposição, alocações posteriores e capacidade/excedente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasDesligamentoExcedenteFuturoHttpIT#retificarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**DES-005:checkpoint:003** Confirmação usa prévia ainda válida; mudança concorrente rejeita o comando.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasDesligamentoExcedenteFuturoHttpIT#retificarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT.xml`; XML SHA-256 `f2c1179d20532b80beff1856aebd9dd094e20532d509bb2fb763f8d875706ccc`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

### DES-006 — B

Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**DES-006:checkpoint:001** Desligamento futuro pode ser cancelado ou retificado por comando próprio.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoFuturoPodeSerCanceladoSemApagarOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasDesligamentoExcedenteFuturoHttpIT#cancelarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

**DES-006:checkpoint:002** Cadeia original é preservada e período restaurado é revalidado antes de confirmar.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoFuturoPodeSerCanceladoSemApagarOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasDesligamentoExcedenteFuturoHttpIT#cancelarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest.xml`; XML SHA-256 `52303c9b99b58f232f45098c169cbd11585d5d43ed668bbe94a119116c995bf9`.
- `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa`: **ERROR**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT.xml`; XML SHA-256 `f4bcfaf94ef11943840fc4f84f80034b0660e75262d726596ea28222bcfcbbbb`.

### FRE-001 — C

Frequência resolve vínculo e alocação válidos na data negocial.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**FRE-001:checkpoint:001** Lançamento resolve vínculo/alocação da data e expõe seus IDs e estados independentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaEtapa09Test#resolvedorBloqueiaContratoHistoricamenteInativo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#resolvedorBloqueiaContratoHistoricamenteInativo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-001:checkpoint:002** Transferência/ADI e jornada noturna respeitam Gestão e alocação efetivas do início da jornada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaGestaoTransferidaAditamentoHttpIT#frequenciaDepoisDoAditamentoUsaGestaoDoFatoSemRestaurarFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaGestaoTransferidaAditamentoHttpIT#frequenciaDepoisDoAditamentoUsaGestaoDoFatoSemRestaurarFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaGestaoTransferidaAditamentoHttpIT.xml`; XML SHA-256 `a61cb1765ce9eedc1a1ca71345505a0b67017c59a3d6b13b50d5fbd5acf63b3c`.
- `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT.xml`; XML SHA-256 `295b506f1da7c3076444dce1232a4c561bdcb98bd8291a7fcca5cc2246e525e3`.

**FRE-001:checkpoint:003** Complementos novos exigem cobertura e autorização em todos os dias do período.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#cadastroExigeVinculoEAlocacaoValidosNaDataDoComplemento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#excecaoDeVariosDiasExigeAutorizacaoEmTodosOsDiasAtingidos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeCadastrarComplementoMesmoDepoisDaTransferencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoPodeCadastrarComplementoEmDiaDaGestaoAnterior`; localização: `METODO_LOCALIZADO`.

**FRE-001:checkpoint:004** Fechamento de contrato transferido resolve o contrato pelo dia solicitado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.

**FRE-001:checkpoint:005** CPF isolado de raiz legada não comprova contexto de criação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.

### FRE-002 — B

Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-002:checkpoint:001** A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-002:checkpoint:002** A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

### FRE-003 — B

Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-003:checkpoint:001** A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-003:checkpoint:002** A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

### FRE-004 — B

Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-004:checkpoint:001** Edição operacional dentro da janela avança versão e mantém antes/depois, autor e registro.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT.xml`; XML SHA-256 `295b506f1da7c3076444dce1232a4c561bdcb98bd8291a7fcca5cc2246e525e3`.

**FRE-004:checkpoint:002** Cadeia de auditoria da frequência verifica hashes e detecta conteúdo incompatível.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#auditoriaGeraHashEValidaCadeia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#auditoriaGeraHashEValidaCadeia`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

### FRE-005 — B

Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-005:checkpoint:001** Após a janela, Operacional é recusado e Gestor corrige por ato versionado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.

**FRE-005:checkpoint:002** Retificação de memória manual preserva a memória anterior no evento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#retificacaoManualDeNoiteParaDiaPreservaAMemoriaAnteriorNaCadeia`; localização: `METODO_LOCALIZADO`.

### FRE-006 — B

Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-006:checkpoint:001** Gestor pode retificar antes do7º dia, com versão formal e bloqueio apenas daquele registro.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado`; localização: `METODO_LOCALIZADO`.

**FRE-006:checkpoint:002** Retificação gerencial sem justificativa é rejeitada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#parametrosInvalidosNaoSaoAceitosComoConsultasOuLancamentosValidos`; localização: `METODO_LOCALIZADO`.

### FRE-007 — B

Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-007:checkpoint:001** Gestor pode retificar jornada antiga sem prazo máximo rígido e com justificativa.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.

### FRE-008 — C

Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**FRE-008:checkpoint:001** Retificação altera o resultado oficial atual sem reescrever emissão anterior.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**FRE-008:checkpoint:002** Memória manual de horas é aplicada de forma consistente em consulta, consolidação, emissão e arquivo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(String, String, String, int, int, int)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT.xml`; XML SHA-256 `0ce60b57e1c213ca57e0fa743fa03f4cc93f341d9dd45d77b3a8501ab25ef3f6`.
- `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(String, String, String, int, int, int)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT.xml`; XML SHA-256 `0ce60b57e1c213ca57e0fa743fa03f4cc93f341d9dd45d77b3a8501ab25ef3f6`.
- `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(String, String, String, int, int, int)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT.xml`; XML SHA-256 `0ce60b57e1c213ca57e0fa743fa03f4cc93f341d9dd45d77b3a8501ab25ef3f6`.
- `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(String, String, String, int, int, int)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT.xml`; XML SHA-256 `0ce60b57e1c213ca57e0fa743fa03f4cc93f341d9dd45d77b3a8501ab25ef3f6`.
- `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(String, String, String, int, int, int)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT.xml`; XML SHA-256 `0ce60b57e1c213ca57e0fa743fa03f4cc93f341d9dd45d77b3a8501ab25ef3f6`.

**FRE-008:checkpoint:003** Reconciliação automática preserva fontes manuais e recalcula apenas RT derivado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#adicionalDerivadoContinuaReconciliavelComProvaNaFrequenciaDeOrigem`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#adicionalDerivadoContinuaReconciliavelComProvaNaFrequenciaDeOrigem`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**FRE-008:checkpoint:004** Mudança de calendário invalida resultados dependentes, inclusive dias SEM_LANCAMENTO.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

### FRE-009 — B

Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-009:checkpoint:001** Impacto retroativo em vínculo/alocação identifica frequência atingida e mantém seu conteúdo original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-009:checkpoint:002** Retirar/mover fonte que sustenta FOLGA fechada exige revisão sem alterar silenciosamente a jornada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado`; localização: `METODO_LOCALIZADO`.

**FRE-009:checkpoint:003** Concorrência entre inativação e gravação não deixa frequência válida sob contexto obsoleto.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaConcorrenciaContratoHttpIT#inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo`; localização: `METODO_LOCALIZADO`.

### FRE-010 — B

Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-010:checkpoint:001** Frequência após desligamento retroativo é preservada e retirada da população operacional válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.

**FRE-010:checkpoint:002** Consolidado sem faixa operacional mantém aviso, sem misturar a frequência inválida aos indicadores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada`; localização: `METODO_LOCALIZADO`.

### FRE-011 — B

Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-011:checkpoint:001** Relatório com retificação pendente expõe aviso e contagem no recorte correto.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**FRE-011:checkpoint:002** Exportação XLSX mostra o aviso no próprio documento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test.xml`; XML SHA-256 `1c3d955a7de3bf13549fa6fe8347500b44efd2923236ce1266e014ba2cd3d686`.

**FRE-011:checkpoint:003** Presidência e comparação institucional recebem aviso real limitado ao recorte publicado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.

**FRE-011:checkpoint:004** Retirada de fonte de FOLGA gera aviso e pendência apenas para o dia afetado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado`; localização: `METODO_LOCALIZADO`.

**FRE-011:checkpoint:005** Pendência e aviso acompanham o responsável efetivo após transferência retroativa, inclusive quando a revisão já estava aberta.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria`; localização: `METODO_LOCALIZADO`.

### FRE-012 — C

Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**FRE-012:checkpoint:001** Após corrigir, verdade atual e nova emissão usam o resultado retificado preservando a original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**FRE-012:checkpoint:002** Pendência só é resolvida depois da correção da origem.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

### FRE-013 — B

No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-013:checkpoint:001** No início do8º dia, a política de edição já considera frequência fechada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-013:checkpoint:002** Cache não conserva ABERTA depois da fronteira e rotina técnica grava fechamento auditado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CacheEscopoTemporalHttpIT#cacheInternoNaoConservaEstadoAbertoDepoisDoInicioDoOitavoDia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaEtapa09Test#fechamentoAutomaticoPersisteEstadoEEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#fechamentoAutomaticoPersisteEstadoEEvento`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

### FRE-014 — B

Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-014:checkpoint:001** Retificação gerencial bloqueia nova edição operacional daquele registro.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado`; localização: `METODO_LOCALIZADO`.

**FRE-014:checkpoint:002** Nova retificação gerencial acrescenta versão mesmo se uma anterior já existe.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.

### FRE-015 — C

A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**FRE-015:checkpoint:001** Filtros de intervalo limitam cada dia à Gestão efetiva da alocação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#intervaloMistoNaoEntregaJulhoAGestaoDeOrigem`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#gestaoDeDestinoRecebeSomenteDiasPosterioresATransferencia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#intervaloMistoNaoEntregaJulhoAGestaoDeOrigem`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#gestaoDeDestinoRecebeSomenteDiasPosterioresATransferencia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

**FRE-015:checkpoint:002** Alteração da projeção atual de contrato não apaga horas do período histórico.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaRelatoriosLacunasHttpIT#horasTrabalhadasContinuamVisiveisQuandoProjecaoAtualMudouDeContrato`; localização: `METODO_LOCALIZADO`.

**FRE-015:checkpoint:003** Complementos e filtros usam a responsabilidade histórica; origem continua podendo manter o fato.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#consultaEFiltrosPreservamGestaoHistoricaDepoisDeTransferencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeManterComplementoDepoisDaTransferencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoCapturaNemExcluiComplementoDeOutraGestao`; localização: `METODO_LOCALIZADO`.

**FRE-015:checkpoint:004** Perda de cobertura não restaura uma Gestão superada nem apaga acesso histórico comprovado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#perdaDeCoberturaOperacionalNaoApagaContextoComprovadoParaConsulta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT#anulacaoNaoRestauraGestaoDaFotografiaSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.

**FRE-015:checkpoint:005** Fechamento por contrato explícito usa Gestão do dia, não a atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.

### FRE-016 — B

Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-016:checkpoint:001** Mudança retroativa de alocação não reassocia a frequência nem transfere edição ao novo Operacional.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-016:checkpoint:002** Correção autorizada preserva autor original e antes/depois auditáveis.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT.xml`; XML SHA-256 `295b506f1da7c3076444dce1232a4c561bdcb98bd8291a7fcca5cc2246e525e3`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

### FRE-017 — B

Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-017:checkpoint:001** Retirada de cobertura conserva a frequência como prova e marca validade operacional inválida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#impactoRetroativoSemCoberturaPreservaRegistroEMarcaInvalido`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#impactoRetroativoSemCoberturaPreservaRegistroEMarcaInvalido`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-017:checkpoint:002** Recuperar cobertura não regulariza silenciosamente a frequência pendente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaEtapa09Test#reativacaoNaoRestauraValidadeSilenciosamente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#reativacaoNaoRestauraValidadeSilenciosamente`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.frequencia.FrequenciaEtapa09Test.xml`; XML SHA-256 `c22f0e7b923272704af23dc740c999fb00e88999a6f51c022c0f3ecdd036c866`.

**FRE-017:checkpoint:003** Transações concorrentes não confirmam cobertura inválida como válida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaConcorrenciaContratoHttpIT#inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo`; localização: `METODO_LOCALIZADO`.

**FRE-017:checkpoint:004** Anulação contratual desde a jornada preserva frequência fechada e emissão original, retirando a frequência do cálculo vigente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaAnulacaoContratoPreservacaoHttpIT#anulacaoPreservaFrequenciaFechadaEProvaOriginalMasRetiraCalculoEGeraPendencia`; localização: `METODO_LOCALIZADO`.

**FRE-017:checkpoint:005** Inativação documentada retira do cálculo somente os dias abrangidos pelo alcance, mantendo as fontes históricas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

### FRE-018 — C

Permissão de edição combina autorização atual do usuário com o escopo histórico do registro.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**FRE-018:checkpoint:001** Edição exige perfil atual autorizado e escopo histórico próprio.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaPermissaoHistoricaHttpIT#concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT#concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT.xml`; XML SHA-256 `657e453e47dcfdf847f56f63119bf2bbf9a3a6afb062a77ddd7f02379cc2d033`.
- `com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT#concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT.xml`; XML SHA-256 `657e453e47dcfdf847f56f63119bf2bbf9a3a6afb062a77ddd7f02379cc2d033`.
- `com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT#concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT.xml`; XML SHA-256 `657e453e47dcfdf847f56f63119bf2bbf9a3a6afb062a77ddd7f02379cc2d033`.

**FRE-018:checkpoint:002** Revogação atual de permissão excepcional vale em consulta e eventos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaConsultaMinimizacaoHttpIT#permissaoExcepcionalEscopadaRegistraUsoERevogacaoValeEmConsultaEEventos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaConsultaMinimizacaoHttpIT#permissaoExcepcionalEscopadaRegistraUsoERevogacaoValeEmConsultaEEventos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaConsultaMinimizacaoHttpIT.xml`; XML SHA-256 `2f81dfbf3455e9a053d60a75bff9caab3fc9f20a26545b58fa93ec2903b1f49a`.

**FRE-018:checkpoint:003** Complementos autorizam origem antes de destino e todos os dias de uma exceção.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoCapturaNemExcluiComplementoDeOutraGestao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#excecaoDeVariosDiasExigeAutorizacaoEmTodosOsDiasAtingidos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeManterComplementoDepoisDaTransferencia`; localização: `METODO_LOCALIZADO`.

**FRE-018:checkpoint:004** Sem identidade comprovada, o CPF atual de outra Pessoa não concede consulta nem escrita; prova própria existente mantém acesso legítimo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#fonteLegadaSemProvaNaoMudaDeGestaoPorCoincidenciaAtualDeCpf`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fonteLegadaSemProvaNaoMudaDeGestaoPorCoincidenciaAtualDeCpf(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fonteLegadaSemProvaNaoMudaDeGestaoPorCoincidenciaAtualDeCpf(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT.xml`; XML SHA-256 `d71852095f05e8a9dd179dc6e5d09a8e6002606304c861d17613ce7221e214e9`.

**FRE-018:checkpoint:005** Comando de fechamento usa autorização presente sobre Gestão histórica.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.
- `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT.xml`; XML SHA-256 `0f143cfb3860d10df71fd16df8a1abf9e0a928f93e98d578f5afe9ee7abab2f0`.

### FRE-019 — B

Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**FRE-019:checkpoint:001** Visibilidade, estado negocial, validade e auditoria são campos distintos.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes`; localização: `METODO_LOCALIZADO`.

**FRE-019:checkpoint:002** Registro inválido permanece auditável sem integrar totais operacionais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa`; localização: `METODO_LOCALIZADO`.

**FRE-019:checkpoint:003** Fonte com cobertura retirada continua acessível pelo escopo comprovado, realinhado às transferências válidas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `FrequenciaComplementosEscopoTemporalHttpIT#perdaDeCoberturaOperacionalNaoApagaContextoComprovadoParaConsulta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT#anulacaoNaoRestauraGestaoDaFotografiaSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.

**FRE-019:checkpoint:004** Preservar leitura de fatos passados não libera o bloqueio operacional corrente nem fabrica sua data inicial.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

### REL-001 — C

Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**REL-001:checkpoint:001** Relatório oficial atual reflete a última retificação e conserva a emissão anterior.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**REL-001:checkpoint:002** Comparação institucional de emissão local conserva o recorte publicado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.

**REL-001:checkpoint:003** Mudança de calendário ou fonte de FOLGA sinaliza a verdade atual dependente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

**REL-001:checkpoint:004** Consolidado e absenteísmo usam nome historicamente comprovado, com ausência explícita quando não há fotografia.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioNomeLegadoDesconhecidoHttpIT#relatorioEPlanilhaNaoSubstituemNomeHistoricoAusentePeloCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioNomeLegadoDesconhecidoHttpIT#relatorioEPlanilhaPreservamNomeComprovadoNaFrequenciaAposCorrecaoAtual`; localização: `METODO_LOCALIZADO`.

**REL-001:checkpoint:005** Rótulos de Lotação, Turno e Cargo do relatório derivam de fotografia comprovada da Alocação ou Frequência.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioRotulosLegadosHttpIT#relatorioPreservaRotulosComprovadosEExplicitaAusenciaLegada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatorioRotulosLegadosHttpIT#relatorioPreservaRotulosComprovadosEExplicitaAusenciaLegada(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioRotulosLegadosHttpIT.xml`; XML SHA-256 `bf2ed32a2807c850f26733a3b7693fda008143061582dc85ee58920a7d65ad91`.
- `com.example.siggtrat.auditoria20261029.RelatorioRotulosLegadosHttpIT#relatorioPreservaRotulosComprovadosEExplicitaAusenciaLegada(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioRotulosLegadosHttpIT.xml`; XML SHA-256 `bf2ed32a2807c850f26733a3b7693fda008143061582dc85ee58920a7d65ad91`.

**REL-001:checkpoint:006** Flag corrente sem data de inativação não exclui apuração histórica comprovada, com ou sem versões contratuais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#contratoInativoHojeContinuaNoRelatorioHistoricoSemFiltroDeContrato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataPreservaFaixaHistoricaMesmoSemVersoes`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#contratoInativoHojeContinuaNoRelatorioHistoricoSemFiltroDeContrato`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataPreservaFaixaHistoricaMesmoSemVersoes`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

**REL-001:checkpoint:007** Apuração continua respeitando inatividade comprovada por evento temporal ou fotografia administrativa inicial.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#fotografiaAdministrativaInativaPermaneceBloqueadaMesmoComFlagAtualAtiva`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#fotografiaAdministrativaInativaPermaneceBloqueadaMesmoComFlagAtualAtiva`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

**REL-001:checkpoint:008** Intervalo misto é dividido na fronteira de consulta corrente sem registrar um ato de inativação presumido.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT.xml`; XML SHA-256 `ea953bb0aaea582cd2de2cd8455afbf27c2372ce220cd5dbb822af5dd6b61926`.

### REL-002 — B

Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REL-002:checkpoint:001** Nova emissão após invalidação tem versão maior e natureza RETIFICADA.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioAdministrativoServicoEtapa11Test#novaEmissaoAposInvalidacaoDaFonteEhRetificadaEPreservaAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test#novaEmissaoAposInvalidacaoDaFonteEhRetificadaEPreservaAnterior`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test.xml`; XML SHA-256 `d12eb64f577e0c4846cd9ee34f56b54b076939e86c04b73a125688b33af31196`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**REL-002:checkpoint:002** Fontes temporais sem lançamento e calendário também provocam emissão retificada quando alteram resultado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT.xml`; XML SHA-256 `0d2bb2a8dd009903d1ee6fe2e1799d1581ce1bc045e7e9e3fdee6f3f8bee9f9a`.
- `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT.xml`; XML SHA-256 `0d2bb2a8dd009903d1ee6fe2e1799d1581ce1bc045e7e9e3fdee6f3f8bee9f9a`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

### REL-003 — C

Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**REL-003:checkpoint:001** Consulta de emissão e posições distingue conhecimento anterior da verdade atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotoresTemporaisConhecimentoRegressaoTest#reducaoPosteriorDoContratoNaoEncerraVinculoNoConhecimentoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.
- `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test.xml`; XML SHA-256 `c54055d82029280a0c79e902e2d584cae19e5b59fe08d00316a1d3816332a44a`.
- `com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest#reducaoPosteriorDoContratoNaoEncerraVinculoNoConhecimentoAnterior`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest.xml`; XML SHA-256 `57285416951f436d3557f1fd14285ba6886142330403fecb51f64586d7260a5b`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

**REL-003:checkpoint:002** Fontes de calendário/complementos mantêm cadeia consultável no instante de conhecimento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

### REL-004 — B

Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REL-004:checkpoint:001** Relatório pendente indica revisão e quantidade, inclusive quando não há linhas operacionais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**REL-004:checkpoint:002** Aviso acompanha o arquivo exportado e o recorte institucional autorizado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test.xml`; XML SHA-256 `1c3d955a7de3bf13549fa6fe8347500b44efd2923236ce1266e014ba2cd3d686`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.

**REL-004:checkpoint:003** O aviso do relatório segue o escopo responsável pela revisão, sem alterar a fotografia original da fonte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria`; localização: `METODO_LOCALIZADO`.

### REL-005 — C

Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**REL-005:checkpoint:001** Emissão registra período, geração, usuário/perfil, versão, natureza, pendências e hash.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioAdministrativoServicoEtapa11Test#emissaoOriginalPreservaSnapshotHashAutorPeriodoEPendencias`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test#emissaoOriginalPreservaSnapshotHashAutorPeriodoEPendencias`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test.xml`; XML SHA-256 `d12eb64f577e0c4846cd9ee34f56b54b076939e86c04b73a125688b33af31196`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**REL-005:checkpoint:002** Reenvio de mesma intenção preserva emissão e intenção diferente na mesma chave é conflito.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissoesRejeitamPerfilEscritorIndevidoIntencaoDivergenteEFiltrosInvalidos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissoesRejeitamPerfilEscritorIndevidoIntencaoDivergenteEFiltrosInvalidos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**REL-005:checkpoint:003** Concorrência idempotente só retorna após transação válida de recuperação.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioAdministrativoConcorrenciaTest#repeticaoConcorrenteSoRetornaDepoisDeNovaTransacaoValida`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.etapa11.RelatorioAdministrativoConcorrenciaTest#repeticaoConcorrenteSoRetornaDepoisDeNovaTransacaoValida`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa11.RelatorioAdministrativoConcorrenciaTest.xml`; XML SHA-256 `3ae7248913cec15ac4d6247d4ad5fc64234c8c6f7feae6d560c56a091177f4b4`.

### REL-006 — B

Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**REL-006:checkpoint:001** Alteração temporal invalida exatamente intervalos/escopos atingidos e preserva conteúdo anterior.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `InvalidacaoEmissaoTemporalTest#retificacaoAtingeAlocacaoAnteriorENovaEPreservaOutroRecorteEConteudo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.servico.InvalidacaoEmissaoTemporalTest#retificacaoAtingeAlocacaoAnteriorENovaEPreservaOutroRecorteEConteudo`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.servico.InvalidacaoEmissaoTemporalTest.xml`; XML SHA-256 `7828412080fb99399b08cf6afc243b4dda92ef236de8f83c43cb19f7ef38f9ad`.
- `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT.xml`; XML SHA-256 `0d2bb2a8dd009903d1ee6fe2e1799d1581ce1bc045e7e9e3fdee6f3f8bee9f9a`.
- `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT.xml`; XML SHA-256 `0d2bb2a8dd009903d1ee6fe2e1799d1581ce1bc045e7e9e3fdee6f3f8bee9f9a`.

**REL-006:checkpoint:002** Cálculo afetado por calendário e retirada de fonte de FOLGA supera emissão vigente sem apagar prova.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.

**REL-006:checkpoint:003** Comparação da consolidação vigente mantém o escopo da emissão original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.

### AUD-001 — B

Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**AUD-001:checkpoint:001** Registrar autor, data de registro, data efetiva, motivo e antes/depois

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.

**AUD-001:checkpoint:002** Escopo auditado corresponde ao fato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricoOcorrenciaContextoAtualHttpIT#historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal`; localização: `METODO_LOCALIZADO`.

**AUD-001:checkpoint:003** Cadeia protege os metadados de autorização

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#assinaturaDetectaAlteracaoDoEscopoQueControlaAConsultaHistorica`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricoAssinaturaLegadaMistaHttpIT#cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.HistoricoAssinaturaLegadaMistaHttpIT#cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.HistoricoAssinaturaLegadaMistaHttpIT.xml`; XML SHA-256 `bec3384f0cad909dc6d71e768736ea47cd30dee75ada319066fb6141f984b26a`.

**AUD-001:checkpoint:004** Ocorrências registram autor/instante/contexto efetivo; histórico novo v2 autentica Gestão/estado/data; histórico de ocorrência v3 também autentica o vínculo explícito à fonte. Calendário e fontes manuais preservam versões com autoria, registro, conteúdo e cadeia de hashes. Assinaturas legadas não são reescritas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricoAssinaturaLegadaMistaHttpIT#cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.HistoricoAssinaturaLegadaMistaHttpIT#cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.HistoricoAssinaturaLegadaMistaHttpIT.xml`; XML SHA-256 `bec3384f0cad909dc6d71e768736ea47cd30dee75ada319066fb6141f984b26a`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-001:checkpoint:005** Atualização dos dados opcionais da Pessoa pelo fluxo canônico registra valores anteriores/novos, autor, instante, data efetiva e motivo de operação; rollback não confirma os dados nem publica ação bem-sucedida.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ConclusaoApiCanonicaHttpIT#atualizacaoCanonicaRegistraAntesDepoisAutorDataEfetivaESomenteAposCommit`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoApiCanonicaHttpIT#atualizacaoCanonicaRegistraAntesDepoisAutorDataEfetivaESomenteAposCommit`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoApiCanonicaHttpIT.xml`; XML SHA-256 `1e87f401faba5049929fa3e836be48e6867b6b1a9ccf0881b28f6a7a95e64cf9`.

### AUD-002 — B

Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**AUD-002:checkpoint:001** Retificação preserva original e nova versão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.

**AUD-002:checkpoint:002** Motivo/autor/data efetiva/registro são explícitos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.
- `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String, String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT.xml`; XML SHA-256 `289347a6ba45c7142166fe6d1455f6dd13f3790d7183e1b1ca1c8ae666d637dc`.

**AUD-002:checkpoint:003** Alteração de fonte de frequência não apaga sua versão original

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-002:checkpoint:004** Retificações de calendário/exceção/adicional acrescentam fotografias imutáveis; cancelamentos lógicos preservam raiz e versões. Reconciliar frequência não remove fonte manual publicada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

### AUD-003 — C

Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**AUD-003:checkpoint:001** Data efetiva e registro são distintas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-003:checkpoint:002** Consulta conhecidoAte respeita conhecimento disponível

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-003:checkpoint:003** Auditoria mantém as versões superadas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-003:checkpoint:004** Situação de evento contratual em conhecidoAte é calculada pelas relações conhecidas no corte. Cadeias de calendário e complementos distinguem data do fato e instante registrado, com consulta histórica por conhecidoAte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

### AUD-004 — C

Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**AUD-004:checkpoint:001** Prévia assina estado/impactos completos

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PreviaCicloVidaLimitesHttpIT#previaLimitaDetalhesMasContaTudoEAssinaDependenciaForaDaAmostra`; localização: `METODO_LOCALIZADO`.

**AUD-004:checkpoint:002** Confirmação relê sob lock

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

**AUD-004:checkpoint:003** Dependência fora da amostra invalida token

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PreviaCicloVidaLimitesHttpIT#previaLimitaDetalhesMasContaTudoEAssinaDependenciaForaDaAmostra`; localização: `METODO_LOCALIZADO`.

**AUD-004:checkpoint:004** Nova confirmação é exigida

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT.xml`; XML SHA-256 `e9470312381c4cec21038ee0b8edee521537d6000656fb87ce17c46c4cfba60d`.

### AUD-005 — B

Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**AUD-005:checkpoint:001** Reenvio de mesma intenção não cria fato duplicado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#reenviosRealmenteConcorrentesConfirmamUmaUnicaFonteNoMysql`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioSemContratoConcorrenciaHttpIT#cadastrosConcorrentesSemContratoNaoPublicamDuasFontesIguais`; localização: `METODO_LOCALIZADO`.

**AUD-005:checkpoint:002** Mesma chave com intenção diferente conflita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade`; localização: `METODO_LOCALIZADO`.

**AUD-005:checkpoint:003** Autorização é revalidada antes de devolver resposta idempotente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.

**AUD-005:checkpoint:004** Ocorrência identifica intenção normalizada e arquivos sob bloqueio de contexto, chave explícita ou digest determinístico. Mesmo reenvio retorna mesmo fato; intenção divergente409; duas transações concorrentes confirmam uma única fonte. PUT idêntico de calendário/complemento não acrescenta versão.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#reenviosRealmenteConcorrentesConfirmamUmaUnicaFonteNoMysql`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#rollbackDaCriacaoReverteFonteContextosArquivoEHistoricoAssinado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `IdempotenciaFontesFrequenciaHttpIT#reenvioDePutIdenticoNaoDuplicaVersaoMasNovaAlteracaoPermaneceDocumentada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioSemContratoConcorrenciaHttpIT#cadastrosConcorrentesSemContratoNaoPublicamDuasFontesIguais`; localização: `METODO_LOCALIZADO`.

**AUD-005:checkpoint:005** Reenvio operacional de frequência (sequencial ou simultâneo, com/sem chave) preserva versão e cadeia; mesma chave com intenção diferente retorna409; textos com separadores têm fronteiras inequívocas no hash.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#reenvioOperacionalPreservaVersaoCadeiaEAutorComOuSemChave`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#mesmaChaveOperacionalComIntencaoDiferenteConflitaSemMutacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#reenviosOperacionaisConcorrentesConfirmamUmUnicoFato`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#novaAlteracaoSemChaveContinuaVersionadaEAuditavel`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#camposTextuaisComSeparadorNaoColidemNaIntencaoGerencial`; localização: `METODO_LOCALIZADO`.

### AUD-006 — C

Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**AUD-006:checkpoint:001** Central identifica origem, motivo, datas, responsável e estado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**AUD-006:checkpoint:002** Pendência bloqueia só recorte atingido

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT.xml`; XML SHA-256 `03747d3e28747dc0f4d325359bc2030226dd7c0b87944751da44eaab561cc336`.

**AUD-006:checkpoint:003** Responsabilidade usa Gestão efetiva, inclusive após transferência

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT.xml`; XML SHA-256 `bb051c8cbbbe7da46a6fd4d7c574c61e3556ea550eea07215be9a250ffeddd4c`.

**AUD-006:checkpoint:004** Pendência de fim natural/conflito usa Gestão efetiva no dia do efeito. Aviso de relatório respeita o recorte congelado da emissão, inclusive consulta institucional nacional.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT.xml`; XML SHA-256 `bb051c8cbbbe7da46a6fd4d7c574c61e3556ea550eea07215be9a250ffeddd4c`.
- `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT.xml`; XML SHA-256 `89a73558b23a76b17e0504dbd8bf5346b38418ec9ba84c9cd06dd8070993e760`.

**AUD-006:checkpoint:005** Pendência de frequência acompanha a responsabilidade efetiva por Gestão/Lotação/Turno após transferência retroativa, inclusive revisão já aberta, sem reescrever fotografia, autoria ou versão original da fonte. Relatório do responsável avisa revisão e deixa o aviso após correção e resolução comprovadas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte`; localização: `METODO_LOCALIZADO`.

**AUD-006:checkpoint:006** Todas as pendências ainda ativas da mesma frequência, inclusive versões anteriores em tratamento, acompanham a responsabilidade efetiva após transferência; IDs, chaves, motivo, surgimento, autoria de início e fotografia da fonte permanecem. Correção humana resolve as revisões da origem e retira o aviso.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria`; localização: `METODO_LOCALIZADO`.

### AUD-007 — B

Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**AUD-007:checkpoint:001** Retificação de retificação sem teto artificial

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-007:checkpoint:002** Cadeia completa permanece consultável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.

**AUD-007:checkpoint:003** Versão vigente e datas são inequívocas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest.xml`; XML SHA-256 `47b1f7110483974a919bc9f78c7614bb1c5fb9e1b5255f57273df081a7cfb395`.

**AUD-007:checkpoint:004** Retificação de retificação preserva versões sucessivas também nas fontes calendário/exceção/adicional; conhecidoAte seleciona apenas conhecimento existente no corte. Correção de uma anulação não invalida outra decisão independente.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT.xml`; XML SHA-256 `ae929e86752b2dc202f7ad6099554f22161b8865e515dde743468273d8d60706`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT.xml`; XML SHA-256 `b0531f57e15fd91d67f731468de7afd90a62d24d63471034956823ee2945f7a8`.
- `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest.xml`; XML SHA-256 `ff2fca812c4ea4df89f25ba90884b2c094df32845bb1254fadfc7a6ac96c0d45`.

### PER-001 — B

Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-001:checkpoint:001** Gestor Local escreve somente no próprio escopo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-001:checkpoint:002** Rotas canônicas e legadas aplicam a mesma fronteira

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-001:checkpoint:003** Ocorrência respeita Gestão efetiva da data do fato

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.

**PER-001:checkpoint:004** Criação e cancelamento exigem Gestor Local na Gestão efetiva de todos os dias do fato; gravação e histórico não dependem dos campos atuais materializados.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo`; localização: `METODO_LOCALIZADO`.

**PER-001:checkpoint:005** Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT.xml`; XML SHA-256 `ccff48798e1e0c526410acb824ea6a7ef04b79d3177fb6f587b6bc3e3ba24ea7`.

### PER-002 — B

Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-002:checkpoint:001** Estadual consulta somente o próprio estado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-002:checkpoint:002** Não executa atos negociais

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-002:checkpoint:003** Limite territorial segue o fato histórico consultado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.

**PER-002:checkpoint:004** Consulta de ocorrência usa estado da Gestão reconstruída para a data efetiva; não libera escrita ao Gestor Estadual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### PER-003 — B

Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-003:checkpoint:001** Gestor Nacional consulta/audita nacionalmente

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-003:checkpoint:002** Escrita negocial continua exclusiva ao Gestor Local

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-003:checkpoint:003** Concessão pessoal não concede escrita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

### PER-004 — C

Presidente atua somente em consulta nacional neste módulo.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**PER-004:checkpoint:001** Presidente consulta nacional sem escrita

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-004:checkpoint:002** Contratos/pessoas/vínculos institucionais minimizados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-004:checkpoint:003** Documento negocial permitido no escopo nacional

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.

**PER-004:checkpoint:004** Presidência consulta documentos negociais e DTOs institucionais minimizados das ocorrências em âmbito nacional; GET não libera comandos de criação/cancelamento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.

### PER-005 — B

Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-005:checkpoint:001** Administração concede permissões técnicas

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-005:checkpoint:002** Não ganha escrita negocial comum

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

**PER-005:checkpoint:003** Acesso funcional pessoal exige concessão e uso auditado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `AuditoriaDadosRestritosNovaAuditoriaHttpIT#auditoriaTecnicaNaoContornaMinimizacaoDosDadosPessoais`; localização: `METODO_LOCALIZADO`.

**PER-005:checkpoint:004** Administrador técnico permanece sem escrita negocial; documento pessoal exige intervenção excepcional vigente, finalidade e trilha de uso.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado`; localização: `METODO_LOCALIZADO`.

### PER-006 — C

Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**PER-006:checkpoint:001** Estadual recebe somente dados necessários

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-006:checkpoint:002** CPF mascarado e opcionais ausentes

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-006:checkpoint:003** Narrativas livres de ocorrência não escapam na lista

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**PER-006:checkpoint:004** Lista/detalhe de ocorrência e narrativas livres de Pessoa, vínculo, alocação e excedente passam pela política central de minimização; fontes persistidas não são alteradas.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#motivoEImpactosDaCorrecaoPessoalSaoMinimizadosSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeVinculoEMinimizadaSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeAlocacaoEMinimizadaSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT.xml`; XML SHA-256 `07e704d2a23f983dfd528ee32a13571581568b6ad018974c037f44446e543b3f`.

### PER-007 — B

Correção excepcional de CPF é autorizada pelo Gestor Local.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-007:checkpoint:001** Correção CPF é ato do Gestor Local

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-007:checkpoint:002** Novo CPF validado e único

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.

**PER-007:checkpoint:003** Motivo/evidência/autorização/valores anterior e novo preservados

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior`; localização: `METODO_LOCALIZADO`.

### PER-008 — B

Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-008:checkpoint:001** Correção de identificador exige concessão específica ativa

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosPermissoesTransferenciaGestaoHttpIT#gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-008:checkpoint:002** Administração concede, Gestor Local executa

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosPermissoesTransferenciaGestaoHttpIT#gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria`; localização: `METODO_LOCALIZADO`.

**PER-008:checkpoint:003** Uso auditado e alias antigo reservado/pesquisável

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosPermissoesTransferenciaGestaoHttpIT#gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria`; localização: `METODO_LOCALIZADO`.

### PER-009 — B

Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-009:checkpoint:001** Perfis de auditoria consultam documentos negociais no seu escopo

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.

**PER-009:checkpoint:002** Conteúdo pessoal não é liberado pelo simples acesso ao evento

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**PER-009:checkpoint:003** Documento restrito exige concessão

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.

**PER-009:checkpoint:004** Documento negocial de ocorrência tem consulta autorizada pelo contexto efetivo; Presidente recebe conteúdo negocial nacional com dados pessoais minimizados no DTO. Classificação pessoal continua exigindo concessão.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

### PER-010 — C

Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**PER-010:checkpoint:001** Autorização atual sobre contexto histórico do registro

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#consultaEListaUsamLotacaoETurnoEfetivosMesmoComProjecaoDefasada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricoOcorrenciaContextoAtualHttpIT#historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.

**PER-010:checkpoint:002** Mudança de Lotação/Turno não transfere acesso a fatos de outra data

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#fatoAnteriorConservaEscopoAntigoMesmoDepoisDeAtualizarAProjecao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo`; localização: `METODO_LOCALIZADO`.

**PER-010:checkpoint:003** Revogação atual remove acesso

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel`; localização: `METODO_LOCALIZADO`.

**PER-010:checkpoint:004** DIF-02: uma política temporal compartilhada resolve Lotação/Turno/Gestão do fato, cobre detalhe, lista, filtros antes de paginação, total, download, criação, cancelamento e concessão. Contextos de ocorrência são fotografados; correções retroativas revalidam a linha temporal; ausência de prova bloqueia acesso.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#consultaEListaUsamLotacaoETurnoEfetivosMesmoComProjecaoDefasada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#fatoAnteriorConservaEscopoAntigoMesmoDepoisDeAtualizarAProjecao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricoOcorrenciaContextoAtualHttpIT#historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal`; localização: `METODO_LOCALIZADO`.

**PER-010:checkpoint:005** Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT.xml`; XML SHA-256 `ccff48798e1e0c526410acb824ea6a7ef04b79d3177fb6f587b6bc3e3ba24ea7`.

### PER-011 — C

Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**PER-011:checkpoint:001** Nacional e Presidente recebem conteúdo minimizado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[3]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[4]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[5]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String)[6]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-011:checkpoint:002** Conteúdo adicional requer concessão específica

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-011:checkpoint:003** A concessão é revalidada e usada com auditoria

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT.xml`; XML SHA-256 `de3045d6b7a466d280cc4cf2bb24b7e4fb569903b1e9259c98f398a3d23b682b`.

**PER-011:checkpoint:004** Nacional e Presidente recebem narrativas minimizadas e documentos pessoais somente com finalidade e concessão explícita vigente; a Presidência não recebe nome na consulta institucional padrão. Mesma política aplicada às narrativas de identidade/vínculo/alocação/excedentes.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#motivoEImpactosDaCorrecaoPessoalSaoMinimizadosSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeVinculoEMinimizadaSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeAlocacaoEMinimizadaSemAlterarFonte`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT.xml`; XML SHA-256 `07e704d2a23f983dfd528ee32a13571581568b6ad018974c037f44446e543b3f`.

### PER-012 — B

Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**PER-012:checkpoint:001** Transferência de Gestão exige autorização administrativa específica

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosPermissoesTransferenciaGestaoHttpIT#concessaoNegocialDepoisDaTransferenciaExigeResponsavelEfetivo`; localização: `METODO_LOCALIZADO`.

**PER-012:checkpoint:002** Ator é Gestor Local autorizado

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `DocumentosPermissoesTransferenciaGestaoHttpIT#concessaoNegocialDepoisDaTransferenciaExigeResponsavelEfetivo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia`; localização: `METODO_LOCALIZADO`.

**PER-012:checkpoint:003** Estadual não passa a executar negócio

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT.xml`; XML SHA-256 `c6dc0e9e0f3ba6d1f4885465fdd59c824470bcd632d02d4af65c1cdd82b4b96c`.

### HIS-001 — C

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**HIS-001:checkpoint:001** Posição contratual apresenta empresa, tipo, composição, efetivo e origem de eventos da data.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.
- `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT.xml`; XML SHA-256 `b3ecdebfeb4adbac8a7b401095ca4f0e43b57456d5757c5c0524ef00a73f2389`.

**HIS-001:checkpoint:002** Novas publicações/aditamentos fotografam política de matrícula e redução de jornada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

**HIS-001:checkpoint:003** Consulta legada sem configuração original explicita desconhecimento e não adota o mestre atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.

**HIS-001:checkpoint:004** Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.

### HIS-002 — B

Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**HIS-002:checkpoint:001** História retorna todos os vínculos autorizados da Pessoa e todas as faixas de Alocação no mês.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#duasAlocacoesDoMesmoColaboradorNoMesNaoDuplicamPopulacaoDoResumo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**HIS-002:checkpoint:002** Nome histórico usa fotografia do fato, separado do cadastro atual.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `FrequenciaHistoricoIndicadoresAuditoriaHttpIT#duasAlocacoesDoMesmoColaboradorNoMesNaoDuplicamPopulacaoDoResumo`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### HIS-003 — B

Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação.

Implementação localizada; baseline atual não comprovou integralmente todos os critérios. Aprovação de versões anteriores não foi importada.

**HIS-003:checkpoint:001** Filtros por início/fim selecionam datas de Vínculo.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto`; localização: `METODO_LOCALIZADO`.

**HIS-003:checkpoint:002** Mudança interna de Alocação não aparece como novo vínculo ou desligamento.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto`; localização: `METODO_LOCALIZADO`.

### HIS-004 — C

Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**HIS-004:checkpoint:001** Alterar cadastro atual de Pessoa/Cargo/Lotação/Turno/Tipo/Gestão não modifica fotografias anteriores.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricosEmpregadosContratoHttpIT#historicoContratualLegadoNaoMudaQuandoContratoEGestaoSaoRenomeados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.
- `com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test.xml`; XML SHA-256 `e426c819cb32f7d2885b3afbe619c1a283ce11a001108743c03cc630f3fccfb3`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

**HIS-004:checkpoint:002** Fotografias ausentes não são completadas pelos mestres atuais.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `HistoricosEmpregadosContratoHttpIT#historicoContratualLegadoNaoMudaQuandoContratoEGestaoSaoRenomeados`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT.xml`; XML SHA-256 `22d40ffd5b209310823b6b177648d848653d9e01c917ac8990a2f2a5ab0a1dff`.
- `com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica`: **PASSOU**; `evidencias/baseline/unitarios_02/surefire-reports/TEST-com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test.xml`; XML SHA-256 `e426c819cb32f7d2885b3afbe619c1a283ce11a001108743c03cc630f3fccfb3`.
- `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT.xml`; XML SHA-256 `be51d66112aeeb492b6402cb0ec4be62bfab00807a855be8c05c57b26d3c9c75`.

### HIS-005 — C

História é a fonte canônica; projeções atuais são conveniência técnica.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**HIS-005:checkpoint:001** Motores históricos usam a cadeia de eventos conhecida no corte.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `VinculoConhecimentoSemOrigemHttpIT#primeiroEventoConhecidoProvaEstadoAnteriorSemHerdarLimiteModificadoDepois`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `VinculoConhecimentoSemOrigemHttpIT#corteAnteriorAPrimeiraFonteNaoCertificaProjecaoAtualComoEstadoHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corteAnteriorAoCadastroNaoRevelaVinculoDaProjecaoAtual`; localização: `METODO_LOCALIZADO`.

**HIS-005:checkpoint:002** Projeção Empregado não sobrepõe Vínculo/Alocação canônicos nem ressuscita programação cancelada.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `RelatorioFrequenciaCanonicaIT#alocacaoCanonicaPrevaleceSobreDimensaoLegadaSemDuplicarPessoa`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `RelatorioFrequenciaCanonicaIT#vinculoCanonicoCanceladoNaoRessurgePeloHistoricoLegado`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `PessoasLacunasHttpIT#corteAnteriorAoCadastroNaoRevelaVinculoDaProjecaoAtual`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT#alocacaoCanonicaPrevaleceSobreDimensaoLegadaSemDuplicarPessoa`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT.xml`; XML SHA-256 `7f71930dd6cec291b14f1b1e41932cf9b891717d2ba87e043ebbf0ff7e5a0d09`.
- `com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT#vinculoCanonicoCanceladoNaoRessurgePeloHistoricoLegado`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT.xml`; XML SHA-256 `7f71930dd6cec291b14f1b1e41932cf9b891717d2ba87e043ebbf0ff7e5a0d09`.

**HIS-005:checkpoint:003** Eventos novos de reprocessamento preservam a visão anterior ao seu registro.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.
- `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT.xml`; XML SHA-256 `4c99134bcc80482854e3b788e53d6e9ab4b36e25b47c88e54daf0cd3e4f3331d`.

### HIS-006 — C

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

Defeito presente na implementação recebida, identificado por reprodução e comparação das fontes; ver histórico RED e registro de defeitos.

**HIS-006:checkpoint:001** Publicação fotografa o cargo efetivamente selecionado no rascunho e a base quantitativa original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ComposicaoFotografiaRascunhoHttpIT#editarCargoNoRascunhoPublicaFotografiaDoCargoEfetivamenteEscolhido`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ComposicaoFotografiaRascunhoHttpIT#editarCargoNoRascunhoPublicaFotografiaDoCargoEfetivamenteEscolhido`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ComposicaoFotografiaRascunhoHttpIT.xml`; XML SHA-256 `51ef7ebc22e6bdb8aa96da18e2d8bc4f1bb34ec8d4a5a6e4a6b03bea33128f6a`.

**HIS-006:checkpoint:002** Novas fotografias preservam todas as configurações operacionais contratuais e ADI conserva a original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.
- `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT.xml`; XML SHA-256 `dc51a8ef1e5166df3ef93ec402fb44bc58fec32b100727e2f4ea12e43c3f7918`.

**HIS-006:checkpoint:003** Legado incompleto não recebe configuração atual retroativamente como se fosse a publicação original.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.
- `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla`: **FAILURE**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT.xml`; XML SHA-256 `4541136ffff551c9dc189cc421170749bd7268f00108cf68fc4d19986c72b4b9`.

**HIS-006:checkpoint:004** Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado.

Mapeamento recebido e execução parcial. A presença do método ou seu resultado isolado não comprova todo o critério.

- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual`; localização: `METODO_LOCALIZADO`.
- Referência recebida: `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData`; localização: `METODO_LOCALIZADO`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual(boolean)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual(boolean)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData(String)[1]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.
- `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData(String)[2]`: **PASSOU**; `evidencias/baseline/integracao_parcial_01/failsafe-reports/TEST-com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT.xml`; XML SHA-256 `721d26c69df743a0b097f95bb09248dd7a0991acb22bbdf6fb70448d02b2b213`.

