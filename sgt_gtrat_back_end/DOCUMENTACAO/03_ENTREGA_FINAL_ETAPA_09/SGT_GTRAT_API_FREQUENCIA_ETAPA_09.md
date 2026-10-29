# SGT-GTRAT — Contrato da API de Frequência após a Etapa 09

**Base:** `/api/frequencias`

## 1. Consultas

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/frequencias?data=YYYY-MM-DD` | Consulta diária paginada |
| GET | `/api/frequencias/mensal?mes=M&ano=A` | Consulta mensal paginada |
| GET | `/api/frequencias/intervalo?inicio=YYYY-MM-DD&fim=YYYY-MM-DD` | Consulta por período |
| GET | `/api/frequencias/{id}/eventos` | Cadeia auditável de eventos/versões |

As consultas existentes preservam filtros por Lotação, Jornada, Contrato, Gestão Local, Superintendência e status quando aplicáveis.

## 2. Escrita

| Método | Endpoint | Finalidade |
|---|---|---|
| POST | `/api/frequencias` | Criar/alterar uma frequência conforme perfil e janela |
| POST | `/api/frequencias/lote` | Criar/alterar lote conforme as mesmas regras negociais |

As rotas de escrita usam `@ExigirSenha`.

## 3. Campos temporais expostos no DTO

Além dos campos de frequência já existentes, a resposta passa a expor:

- `pessoaId`;
- `vinculoContratualId`;
- `alocacaoVinculoId`;
- `contratoId`;
- `lotacaoId`;
- `jornadaId`;
- `cargoId`;
- `gestaoLocalId`;
- `estadoNegocial`;
- `validadeOperacional`;
- `estadoAuditoria`;
- `bloqueadaEdicaoOperacional`;
- `versaoNegocial`;
- `prazoEdicao`.

## 4. Retificação pelo Gestor Local

Toda escrita do Gestor Local é uma retificação formal. O payload deve informar:

- `justificativaRetificacao`: mínimo de 5 caracteres;
- `chaveIdempotencia`: mínimo de 8 caracteres.

A mesma chave pode ser repetida somente para a mesma intenção. Reutilização da chave para conteúdo diferente gera conflito.

Após a retificação:

- `estadoAuditoria = RETIFICADA_GESTOR`;
- `bloqueadaEdicaoOperacional = true`;
- a versão negocial avança;
- a cadeia de eventos preserva a versão anterior.

## 5. Operacional

O Operacional:

- precisa possuir autorização atual sobre o escopo histórico da frequência;
- pode atuar somente até o fim do 7º dia corrido;
- não pode editar registro já alterado pelo Gestor;
- não pode editar registro `SUJEITA_REVISAO` ou fora da validade operacional.

## 6. Jornada noturna

A chave negocial continua sendo `dataReferencia`, correspondente à data de início da jornada. Uma jornada iniciada às 22:00 de um dia e terminada às 06:00 do dia seguinte permanece associada ao primeiro dia.

## 7. Eventos

`GET /api/frequencias/{id}/eventos` devolve versões ordenadas, contendo número da versão, tipo do evento, data da frequência, antes/depois, motivo, usuário, instante, hash anterior e hash atual. A leitura valida a cadeia de integridade antes de devolver os eventos.
