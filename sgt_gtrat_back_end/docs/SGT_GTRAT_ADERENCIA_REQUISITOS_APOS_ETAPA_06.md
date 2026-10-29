# SGT-GTRAT — Aderência aos requisitos após a Etapa 06

Data: **09/09/2026**  
Escopo avaliado: **COL, MAT, VIN e DES** do levantamento consolidado.

## 1. Resumo

| Grupo | Total | Implementado | Parcial | Pendente |
|---|---:|---:|---:|---:|
| Pessoa/Colaborador | 12 | 12 | 0 | 0 |
| Matrícula | 5 | 5 | 0 | 0 |
| Vínculo Contratual | 15 | 13 | 2 | 0 |
| Desligamento | 6 | 6 | 0 | 0 |
| **Total** | **38** | **36** | **2** | **0** |

“Implementado” nesta matriz significa implementado no backend Java do checkpoint correspondente. A interface React ainda não foi iniciada; por isso, apresentação visual, inclusive máscara de CPF, será verificada no ciclo do frontend.

## 2. Pessoa / Colaborador

| ID | Situação | Evidência no checkpoint |
|---|---|---|
| COL-001 | Implementado | `Pessoa` exige nome e CPF; `PessoaServico` valida o cadastro. |
| COL-002 | Implementado | RG, CTPS/série, NIT/PIS/PASEP, nascimento e e-mail são anuláveis/opcionais. |
| COL-003 | Implementado | Relação opcional com `Endereco`, com criação, atualização e limpeza explícita. |
| COL-004 | Implementado | `Pessoa` não possui Conta, Banco, Agência ou dados de pagamento. |
| COL-005 | Implementado no backend | CPF possui validação dos dígitos, restrição global, normalização sem pontuação e armazenamento em 11 dígitos. A máscara fica deliberadamente no futuro React. |
| COL-006 | Implementado | Não existe atualização comum de CPF. Correção excepcional exige Gestor Local, motivo, evidência, autorização e impactos; preserva anterior, novo, autor e instante. |
| COL-007 | Implementado | CPF já vinculado bloqueia correção com conflito; não há fusão automática. |
| COL-009 | Implementado | Nome só muda pelo fluxo excepcional auditado, mantendo a mesma Pessoa. |
| COL-010 | Implementado | DTO atual lê `Pessoa.nome`; cada `EventoVinculoContratual` guarda fotografia imutável do nome da época. |
| COL-011 | Implementado | Atualização opcional interpreta `null` como manter; limpeza exige `limpar...=true`. |
| COL-012 | Implementado | Cadastro atual e fotografias históricas são estruturas separadas. |
| COL-013 | Implementado | CPF identifica a Pessoa; recontratação usa `pessoaId` e cria novo Vínculo. |

## 3. Matrícula

| ID | Situação | Evidência no checkpoint |
|---|---|---|
| MAT-001 | Implementado | Campo matrícula está em `VinculoContratual`; não existe em `Pessoa`. |
| MAT-002 | Implementado | Matrícula é opcional por padrão e a reserva possui unicidade contrato + matrícula. |
| MAT-003 | Implementado | `ReservaMatriculaContrato` é histórica, sem fluxo de exclusão, e não é removida ao alterar/cancelar/desligar. |
| MAT-004 | Implementado | Validação consulta a reserva histórica antes de qualquer novo vínculo ou alteração, inclusive para a mesma Pessoa. |
| MAT-005 | Implementado | `TipoContratacao.matriculaVinculoObrigatoria` e override no Contrato; criação bloqueada quando a regra efetiva exige matrícula. |

## 4. Vínculo Contratual

| ID | Situação | Evidência no checkpoint |
|---|---|---|
| VIN-001 | Implementado | `cadastrar` está transacionado e cria Pessoa, Vínculo, reserva, alocação inicial e evento. |
| VIN-002 | Implementado | O modelo canônico usa `dataInicio` do Vínculo, não `dataAdmissao` fixa da Pessoa. |
| VIN-003 | Implementado | Verificação por Pessoa considera todos os contratos e Gestões; intervalos inclusivos sobrepostos são bloqueados. |
| VIN-004 | Implementado | A regra inclusiva permite o próximo vínculo em `fimAnterior + 1 dia`. |
| VIN-005 | Implementado | Situação `PROGRAMADO` é derivada antes do início; consultas operacionais exigem início alcançado. |
| VIN-006 | Implementado | `cancelarProgramacao` só aceita vínculo futuro e grava cancelamento, sem motivo/data de desligamento. |
| VIN-007 | Implementado | Eventos apontam para o precedente com relação ORIGEM/RETIFICA/CANCELA/SUBSTITUI e hashes encadeados. |
| VIN-008 | **Parcial** | O vínculo aberto fica operacionalmente encerrado após o fim do contrato e existe tratamento manual. A geração automática da pendência aguarda a Central de Pendências da Etapa 11. |
| VIN-009 | Implementado | Tratamento de fim natural aplica a data final do contrato somente aos vínculos sem desligamento anterior. |
| VIN-010 | Implementado | Fim natural exige exatamente um motivo ativo da categoria `FIM_VIGENCIA_CONTRATO` na Gestão Local. |
| VIN-011 | Implementado | Evento separa `dataEfetiva` de `registradoEm`; validação impede fim posterior à vigência. |
| VIN-012 | Implementado | Aditamento tardio chama proteção que consolida o fim antigo; reabertura depende de retificação explícita do Vínculo. |
| VIN-013 | Implementado | Inativação/reativação administrativa não altera Vínculo nem Alocação; operação consulta a habilitação atual do Contrato. |
| VIN-014 | **Parcial** | Retificação de início é versionada, auditada, possui prévia assinada e revalida sobreposição/alocação/matrícula/eventos. Reprocessamento real de frequência e relatórios depende das Etapas 09 e 11. |
| VIN-015 | Implementado | Encerramento antecipado fecha vínculos abertos na mesma data e preserva desligamentos anteriores, usando categoria própria. |

## 5. Desligamento

| ID | Situação | Evidência no checkpoint |
|---|---|---|
| DES-001 | Implementado | Desligamento está no Vínculo, não é campo fixo do Contrato. |
| DES-002 | Implementado | `dataFim` é inclusiva; a situação muda para encerrado somente no dia seguinte. |
| DES-003 | Implementado | Data passada é aceita; evento preserva separadamente o instante posterior de registro. |
| DES-004 | Implementado | Motivo de catálogo, responsável e data/hora ficam registrados no Vínculo e no Evento. |
| DES-005 | Implementado | Retificação preserva fim anterior/novo, evento precedente e valida toda a linha de vínculos da Pessoa. |
| DES-006 | Implementado | Desligamento futuro pode ser retificado ou cancelado, com cadeia de eventos preservada. |

## 6. Itens que continuam fora deste checkpoint

Não devem ser confundidos com defeitos da Etapa 06:

- linha temporal completa de Alocação: Etapa 07;
- fontes documentais/anexos/SEI e classificação restrita: Etapa 08;
- frequência resolvida por Vínculo + Alocação histórica: Etapa 09;
- revisão final de perfis, minimização e permissões excepcionais: Etapa 10;
- Central de Pendências, relatórios temporais e indicadores: Etapa 11;
- integração MySQL, concorrência, carga e fechamento global: Etapa 12;
- interface React: após o fechamento do Java.

## 7. Conclusão

O núcleo da Etapa 06 está implementado sem declarar o backend completo. O avanço correto é iniciar a Etapa 07 e manter VIN-008 e VIN-014 rastreados até as etapas que possuem os módulos consumidores necessários.
