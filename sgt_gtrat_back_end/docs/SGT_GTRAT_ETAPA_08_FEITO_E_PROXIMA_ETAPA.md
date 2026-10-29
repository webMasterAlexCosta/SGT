# SGT-GTRAT — Etapa 08 concluída e decisão para a Etapa 09

Data: **09/09/2026**  
Checkpoint: **Documentos, anexos e SEI**

## 1. Decisão do checkpoint

A Etapa 08 está concluída no backend e apta a avançar para a Etapa 09. A plataforma permanece fixada em **Spring Boot 3.5.16 e Java 17**; não houve retorno ao Spring Boot 4 nem adoção de Java superior ao 17.

Os requisitos `DOC-001` a `DOC-010` estão implementados. O checkpoint foi aprovado por 342 testes automatizados e pelos validadores T01–T90. A homologação continua condicionada ao perfil MySQL/Testcontainers em máquina com Docker.

## 2. O que foi implementado

### 2.1 Agregado documental canônico

`DocumentoComprobatorio` representa uma versão de fonte ligada a um Contrato e a um fato de negócio. O agregado registra:

- tipo e identificador/chave do fato;
- grupo lógico e número da versão;
- versão anterior;
- estado `RASCUNHO`, `PUBLICADO`, `SUBSTITUIDO` ou `INVALIDO`;
- classificação `NEGOCIAL` ou `PESSOAL_RESTRITO`;
- referência SEI exata e normalizada;
- nome original e nome físico aleatório;
- MIME, tamanho e SHA-256 do arquivo;
- data efetiva, instante de registro e fotografias do responsável;
- publicação e chave de idempotência;
- controle otimista de concorrência.

A fonte derivada pode ser `ANEXO`, `SEI` ou `ANEXO_E_SEI`. A referência SEI não é única, permitindo que o mesmo processo sustente múltiplos Contratos.

### 2.2 Versionamento sem apagamento histórico

Uma correção de SEI cria nova versão e marca a anterior como substituída. O mesmo ocorre na troca de arquivo ou metadados. Quando um Contrato foi publicado apenas com SEI e recebe o arquivo depois, o sistema registra o evento específico `ANEXO_ACRESCENTADO`, preservando o SEI e a versão anterior.

Documento publicado incorreto não é apagado. O fluxo comum permite:

- substituição versionada;
- retificação de SEI;
- invalidação auditada.

Exclusão física somente é permitida para rascunho nunca publicado, com arquivo exclusivo e sem referência SEI de valor externo. Rascunhos também ficam visíveis somente ao Gestor Local responsável.

### 2.3 Integração com fatos negociais

O repositório documental foi integrado a:

- publicação do Contrato original;
- aditamento;
- remanejamento;
- transferência excepcional de Gestão Local;
- encerramento antecipado;
- anulação.

Contrato e aditamento validam fonte obrigatória. Remanejamento aceita fonte opcional. Uma referência SEI já enviada junto ao comando é materializada no repositório canônico sem duplicidade acidental. O descarte de Contrato em rascunho também respeita o valor auditável dos documentos preparados.

### 2.4 Segurança dos arquivos

O upload valida:

- limite individual configurado;
- extensão permitida;
- coerência entre extensão e MIME;
- assinatura binária do formato;
- nome original higienizado;
- nome físico aleatório;
- caminho normalizado dentro do diretório autorizado;
- checksum SHA-256.

O download recalcula o checksum e é bloqueado quando o conteúdo foi adulterado. A verificação de órfãos considera tanto os anexos antigos de ocorrência quanto os documentos canônicos desta etapa.

### 2.5 Conteúdo pessoal/restrito

O acesso a `PESSOAL_RESTRITO` exige finalidade declarada. O comportamento é:

| Perfil | Regra documental |
|---|---|
| Gestor Local responsável | Acesso por necessidade dentro do próprio Contrato, sempre auditado |
| Gestor Estadual/Nacional/Presidente | Sem acesso restrito padrão; depende de permissão específica vigente |
| Administrador técnico | Sem acesso restrito automático; depende de autorização excepcional explícita |
| Operacional | Acesso documental negado neste módulo |

A concessão e a revogação de permissão registram finalidade, justificativa, autorização administrativa, validade e responsáveis. Consulta de metadados, histórico e download restritos geram registro imutável com usuário, perfis, finalidade, ação, horário e permissão utilizada.

### 2.6 Auditoria e integridade

`EventoDocumentoComprobatorio` preserva antes/depois, motivo, data efetiva, tempo de conhecimento, responsável, SEI e checksum anterior/novo. Os eventos de um grupo lógico formam cadeia SHA-256 verificável.

As operações aceitam chave de idempotência. A materialização automática de SEI usa chave derivada por SHA-256 com tamanho compatível com o banco.

## 3. Endpoints acrescentados

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/contratos/{contratoId}/documentos` | Listar documentos visíveis |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}` | Consultar metadados |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/eventos` | Consultar cadeia histórica |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/arquivo` | Baixar anexo com verificação de checksum |
| POST | `/api/contratos/{contratoId}/documentos` | Preparar/publicar anexo, SEI ou ambos |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/retificar-sei` | Criar versão corrigida da referência |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/substituir` | Criar nova versão documental |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/invalidar` | Invalidar versão publicada |
| DELETE | `/api/contratos/{contratoId}/documentos/{documentoId}` | Descartar somente rascunho exclusivo sem SEI |
| GET/POST | `/api/contratos/{contratoId}/documentos/{documentoId}/permissoes` | Consultar ou conceder acesso restrito |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/permissoes/{permissaoId}/revogar` | Revogar permissão |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/acessos` | Consultar auditoria de acessos |

O contrato detalhado está em `SGT_GTRAT_API_DOCUMENTOS_ETAPA_08.md`.

## 4. Resultado dos testes

- 342 testes JUnit executados;
- 342 aprovados, 0 falhas, 0 erros e 0 ignorados;
- 15 testes específicos da Etapa 08;
- T01–T28 de regressão temporal e catálogos aprovados;
- T29–T50 da Etapa 06 aprovados;
- T51–T70 da Etapa 07 aprovados;
- T71–T90 da Etapa 08 aprovados;
- 505 fontes Java principais e 111 arquivos Java de teste compilados com `release 17`;
- JAR Spring Boot executável produzido com sucesso.

## 5. Situação de aderência

No recorte documental:

- **10 de 10 requisitos DOC implementados**;
- `AUD-001`, `AUD-002`, `AUD-003`, `AUD-005` e `AUD-007` reforçados no domínio documental;
- `PER-009` e `PER-011` reforçados, permanecendo a revisão transversal e a minimização completa para a Etapa 10.

Não são antecipadas como concluídas as regras de frequência da Etapa 09, a revisão global de perfis da Etapa 10 nem relatórios e Central de Pendências da Etapa 11.

## 6. Limitação de ambiente

O comando `docker` não está disponível neste ambiente. Portanto, o perfil `integration` com MySQL/Testcontainers não foi executado. Essa validação é obrigatória antes da homologação e deve usar banco descartável, vazio e exclusivo desta versão.

## 7. Próxima decisão

Iniciar a **Etapa 09 — Frequência integrada ao novo modelo temporal**, fazendo cada frequência resolver Contrato, Vínculo, Alocação e escopo válidos na data de início da jornada.

Não iniciar React. O ZIP desta entrega é um checkpoint da Etapa 08; o ZIP Java C previsto no plano será fechado após a Etapa 09.
