# SGT-GTRAT — Etapa 07 concluída e decisão para a Etapa 08

Data: **09/09/2026**  
Checkpoint: **Alocação temporal e transferência excepcional de Gestão Local**

## 1. Decisão do checkpoint

A Etapa 07 está concluída no backend e apta a avançar para a Etapa 08. A plataforma permanece fixada em **Spring Boot 3.5.16 e Java 17**; não houve retorno ao Spring Boot 4 nem adoção de versão superior do Java.

O checkpoint foi aprovado por 327 testes automatizados e pelos validadores T01–T70. A homologação continua condicionada à execução do perfil MySQL/Testcontainers em uma máquina com Docker.

## 2. O que foi implementado

### 2.1 Linha temporal de Alocação

`AlocacaoVinculo` deixou de representar apenas uma posição inicial e passou a ser uma faixa temporal versionada, com:

- Vínculo Contratual;
- Lotação, Turno/Jornada e Cargo;
- composição-base opcional para suportar recortes originados por aditamento;
- início e fim efetivos inclusivos;
- estados `REGISTRADA`, `SUBSTITUIDA` e `CANCELADA`;
- autor, instante de criação e controle otimista de versão.

Uma mudança fecha a faixa anterior no dia imediatamente anterior e cria a sucessora na data efetiva. Se a nova informação começa no mesmo dia da faixa corrente, ocorre retificação formal, sem duas Alocações efetivas para a mesma pessoa no mesmo dia.

### 2.2 Cobertura e consistência

O serviço valida que todo dia operacional do Vínculo possui exatamente uma Alocação. São bloqueados:

- lacunas;
- sobreposições;
- datas fora do Vínculo ou do Contrato;
- combinação ausente da composição temporal;
- combinação com efetivo previsto igual ou inferior a zero;
- catálogo inativo em nova Alocação atual ou futura;
- reativação administrativa cuja Alocação não corresponda mais a uma composição temporal válida.

O fim do Vínculo continua inclusivo. Mudanças no último dia ficam explícitas na prévia de impacto, preservando uma interpretação inequívoca para eventual desligamento na mesma data.

### 2.3 Prévia, retificação e cancelamento

Mudanças de Alocação exigem justificativa, evidência, autorização, confirmação de impacto e token de prévia assinado. A prévia informa:

- faixa afetada;
- data efetiva;
- impactos em histórico, frequência, relatórios e indicadores;
- incompatibilidades que impedem a confirmação.

Programações futuras podem ser canceladas sem apagar a faixa. A faixa predecessora é restaurada até o limite correto do Vínculo e toda a operação fica auditada.

### 2.4 História bitemporal

`EventoAlocacaoVinculo` registra fotografias anteriores e novas, data efetiva, instante de conhecimento, relação de precedência, responsável, justificativa, evidência, autorização e hashes encadeados.

`MotorTemporalAlocacaoServico` reconstrói a posição em uma data de negócio e, opcionalmente, limita a visão ao que já era conhecido em determinado instante. O ajuste da faixa sucedida também é registrado como fato explícito, evitando inferência silenciosa na reconstrução.

### 2.5 Consulta mensal e seleção encadeada

A consulta mensal retorna todas as faixas que se sobrepõem ao mês, inclusive mudanças internas. A API de opções usa a composição temporal do Contrato e suporta o encadeamento:

`Contrato → Lotação → Turno → Cargo`

Cada opção informa efetivo previsto e ocupação na data de referência.

### 2.6 Integração com o Vínculo

O serviço canônico de Vínculo agora:

- cria a primeira faixa por meio do serviço temporal de Alocação;
- sincroniza a linha quando o início ou o fim do Vínculo é retificado;
- limita a cobertura em desligamento ou fim contratual;
- restaura a faixa quando um desligamento programado é cancelado;
- preserva o fallback legado apenas para contratos ainda sem registros canônicos.

### 2.7 Transferência excepcional de Gestão Local

Foi criado um fluxo transacional que exige:

- Contrato publicado;
- Gestor Local da Gestão de origem;
- Gestão de destino ativa e vinculada a Superintendência ativa;
- data efetiva e data do ato;
- motivo, referência SEI e autorização administrativa;
- mapeamento explícito de cada recorte `Lotação + Turno + Cargo` da origem para uma combinação válida no destino;
- cobertura das composições positivas e das Alocações posteriores.

A transferência preserva origem, destino, ato, responsável e mapeamentos. Na data efetiva, atualiza o contexto temporal do Contrato e cria faixas sucessoras para as Alocações atingidas, sem novo Vínculo e sem desligamento.

## 3. Endpoints acrescentados

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/vinculos-contratuais/{id}/alocacoes` | Listar faixas, opcionalmente por mês |
| GET | `/api/vinculos-contratuais/{id}/alocacoes/eventos` | Consultar auditoria imutável |
| GET | `/api/vinculos-contratuais/{id}/alocacoes/posicao` | Reconstruir posição bitemporal |
| GET | `/api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao` | Obter seleção encadeada temporal |
| POST | `/api/vinculos-contratuais/{id}/previas/alocacoes` | Gerar prévia e token |
| POST | `/api/vinculos-contratuais/{id}/alocacoes` | Criar faixa sucessora |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/retificar` | Retificar formalmente uma faixa |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/cancelar-programacao` | Cancelar programação futura |
| GET | `/api/contratos/{id}/transferencias-gestao` | Consultar transferências e mapas |
| POST | `/api/contratos/{id}/transferir-gestao` | Executar transferência excepcional |

O contrato detalhado das requisições está em `SGT_GTRAT_API_ALOCACAO_TEMPORAL_ETAPA_07.md`.

## 4. Resultado dos testes

- 327 testes JUnit executados;
- 327 aprovados, 0 falhas, 0 erros e 0 ignorados;
- 12 testes específicos da Etapa 07;
- T01–T28 de regressão temporal e catálogos aprovados;
- T29–T50 da Etapa 06 aprovados;
- T51–T70 da Etapa 07 aprovados;
- 479 fontes Java principais e 110 arquivos Java de teste compilados com `release 17`;
- JAR Spring Boot executável produzido com sucesso.

## 5. Situação de aderência

No recorte diretamente afetado pela Etapa 07 foram avaliados 15 requisitos:

- **12 implementados**;
- **3 parcialmente implementados**: ALO-006, VIN-014 e PER-012.

ALO-006 já possui resolução temporal da Alocação por data, mas a Jornada noturna somente consumirá essa verdade após a migração de frequência na Etapa 09. VIN-014 já revalida a Alocação, restando frequência e relatórios. PER-012 exige Gestor Local da origem e referência de autorização, mas a permissão administrativa granular será fechada na Etapa 10.

VIN-008, herdado da Etapa 06, continua parcial até a Central de Pendências da Etapa 11.

## 6. Limitação de ambiente

O comando `docker version` não está disponível neste ambiente. Portanto, o perfil `integration` com MySQL/Testcontainers não foi executado. Essa validação é obrigatória antes da homologação e deve usar banco descartável, vazio e exclusivo desta versão.

## 7. Próxima decisão

Iniciar a **Etapa 08 — Documentos, anexos e SEI**, preservando a regra de não exclusão física do histórico publicado e classificando conteúdo como `NEGOCIAL` ou `PESSOAL_RESTRITO`.

Não iniciar React ainda. A Etapa 09 ainda precisa migrar a frequência para Vínculo e Alocação históricos; somente depois será emitido o ZIP Java C previsto no plano.

