# SGT-GTRAT — Etapa 06 concluída e decisão para a Etapa 07

Data: **09/09/2026**  
Entrega do plano: **ZIP Java B — Passos 4 a 6**

## 1. Decisão do checkpoint

A Etapa 06 foi implementada sobre a base auditada da Etapa 05 e está apta a avançar para a Etapa 07, com uma ressalva operacional: os testes de integração MySQL/Testcontainers precisam ser executados em máquina com Docker antes de homologação.

A plataforma permanece em **Spring Boot 3.5.16 e Java 17**. Não foi realizada atualização para Spring Boot 4 ou Java superior.

## 2. O que foi implementado

### 2.1 Pessoa permanente

Foi criada a entidade `Pessoa`, separada de contrato, matrícula e alocação. Ela contém:

- nome e CPF obrigatórios;
- RG, CTPS/série, NIT/PIS/PASEP, nascimento, e-mail e endereço opcionais;
- CPF normalizado para 11 dígitos, validado e único globalmente;
- ausência de dados bancários e matrícula no cadastro permanente;
- atualização parcial em que campo omitido mantém o valor e limpeza exige indicador explícito.

Nome e CPF não possuem edição comum. Correções excepcionais criam `CorrecaoPessoa` imutável com valor anterior, valor novo, motivo, evidência, autorização, impactos, responsável e instante de registro. CPF já pertencente a outra pessoa bloqueia a operação e não provoca fusão automática.

### 2.2 Vínculo Contratual

Foi criada a entidade `VinculoContratual`, com:

- Pessoa;
- Contrato;
- início efetivo;
- fim efetivo opcional e inclusivo;
- matrícula do vínculo;
- estado negocial separado da situação temporal;
- motivo e dados administrativos do desligamento.

O cadastro integrado cria, na mesma transação, a Pessoa quando necessária, o Vínculo, a reserva de matrícula, a alocação inicial e o primeiro evento histórico.

As regras implementadas incluem:

- bloqueio global de sobreposição da mesma pessoa, inclusive entre contratos e Gestões Locais diferentes;
- permissão de troca direta em dias consecutivos;
- vínculo futuro derivado como `PROGRAMADO` e sem operação antes do início;
- cancelamento de programação futura sem converter o fato em desligamento;
- retificação de início com prévia de impacto assinada e revalidação na confirmação;
- fim do vínculo interpretado como último dia de existência.

### 2.3 Matrícula

A matrícula foi movida para o Vínculo e possui normalização própria. `ReservaMatriculaContrato` mantém a combinação contrato + matrícula reservada para sempre, inclusive após alteração, desligamento ou cancelamento da programação.

A obrigatoriedade pode ser definida pelo Tipo de Contratação e sobrescrita no Contrato. Quando obrigatória, o vínculo não é criado sem matrícula válida.

### 2.4 Desligamento

Foram implementados:

- desligamento retroativo, atual ou futuro;
- motivo do catálogo da Gestão Local;
- responsável e data/hora de registro separados da data efetiva;
- retificação com prévia de impacto e verificação integral de sobreposição;
- cancelamento de desligamento ainda programado;
- fechamento por fim natural do contrato;
- encerramento conjunto por encerramento antecipado ou anulação contratual;
- preservação de desligamentos anteriores mais curtos.

### 2.5 História bitemporal e integridade

`EventoVinculoContratual` é imutável e registra:

- data efetiva e instante de conhecimento;
- encadeamento com o evento precedente;
- valores anteriores e novos;
- fotografia do nome e CPF da pessoa;
- fotografia do contrato e da Gestão Local;
- responsável, justificativa, evidência e autorização;
- hash SHA-256 do evento e hash anterior.

`MotorTemporalVinculoServico` reconstrói a posição por data de referência e por `conhecidoAte`. Alteração de matrícula somente produz efeito a partir da data efetiva correspondente.

### 2.6 Integração com o Contrato

O ciclo de vida contratual agora aciona o domínio canônico de Vínculos:

- encerramento antecipado fecha vínculos abertos na mesma data;
- cancelamento de contrato futuro cancela as programações vinculadas;
- anulação trata os vínculos conforme o alcance temporal;
- inativação administrativa não encerra vínculo nem alocação;
- prorrogação registrada após o fim antigo primeiro consolida o fim anterior, evitando reabertura automática; eventual reabertura exige retificação explícita.

As contagens de ocupação de Contrato, Composição, Aditamento e Remanejamento usam Vínculo/Alocação quando o contrato já possui registros canônicos. Contratos ainda não migrados continuam usando `Empregado` como fallback transitório.

## 3. Endpoints acrescentados

### Pessoa

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/pessoas` | Listar pessoas no escopo |
| GET | `/api/pessoas/{id}` | Consultar pessoa |
| POST | `/api/pessoas` | Cadastrar pessoa |
| PUT | `/api/pessoas/{id}/dados-opcionais` | Atualizar ou limpar campos opcionais |
| POST | `/api/pessoas/{id}/correcoes-identidade` | Corrigir excepcionalmente nome ou CPF |
| GET | `/api/pessoas/{id}/correcoes-identidade` | Consultar trilha de correções |

### Vínculo

| Método | Endpoint | Finalidade |
|---|---|---|
| GET/POST | `/api/vinculos-contratuais` | Listar ou cadastrar vínculo integrado |
| GET | `/api/vinculos-contratuais/{id}` | Consultar vínculo |
| GET | `/api/vinculos-contratuais/{id}/eventos` | Consultar cadeia histórica |
| GET | `/api/vinculos-contratuais/pessoas/{pessoaId}/posicao` | Reconstrução bitemporal |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-inicio` | Prévia de retificação de início |
| POST | `/api/vinculos-contratuais/{id}/retificar-inicio` | Confirmar retificação de início |
| POST | `/api/vinculos-contratuais/{id}/alterar-matricula` | Alterar matrícula com nova reserva histórica |
| POST | `/api/vinculos-contratuais/{id}/cancelar-programacao` | Cancelar vínculo futuro |
| POST | `/api/vinculos-contratuais/{id}/desligar` | Registrar desligamento |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-desligamento` | Prévia de retificação do fim |
| POST | `/api/vinculos-contratuais/{id}/retificar-desligamento` | Confirmar retificação do fim |
| POST | `/api/vinculos-contratuais/{id}/cancelar-desligamento-programado` | Cancelar fim ainda futuro |
| POST | `/api/vinculos-contratuais/contratos/{contratoId}/tratar-fim-natural` | Tratar vínculos abertos após fim natural |

## 4. Resultado dos testes

- 315 testes JUnit executados;
- 315 aprovados, 0 falhas, 0 erros e 0 ignorados;
- 23 testes específicos da Etapa 06;
- T01–T28 de regressão pré-Etapa 06 aprovados;
- T29–T50 do novo checkpoint aprovados;
- JAR executável produzido com Java 17 e Spring Boot 3.5.16.

Detalhes e comandos estão em `SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_06.md`.

## 5. Situação de aderência

Dos 38 requisitos diretamente associados a Pessoa, Matrícula, Vínculo e Desligamento:

- **36 implementados no backend da Etapa 06**;
- **2 parcialmente implementados**: VIN-008 e VIN-014.

VIN-008 aguarda a Central de Pendências da Etapa 11. VIN-014 já possui prévia, assinatura e revalidação de sobreposição, mas a repercussão efetiva em frequência e relatórios depende das Etapas 09 e 11.

## 6. Próxima decisão

Iniciar a **Etapa 07 — Alocação temporal**. A implementação inicial desta entrega existe apenas para garantir atomicidade no primeiro vínculo; a Etapa 07 deve torná-la uma linha temporal completa, sem lacunas ou sobreposições, com transferência excepcional de Gestão Local.

Não iniciar React ainda. O próximo checkpoint de entrega é o **ZIP Java C**, após as Etapas 07, 08 e 09.
