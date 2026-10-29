# SGT-GTRAT — API de documentos, anexos e SEI da Etapa 08

Data: **09/09/2026**  
Base: `/api/contratos/{contratoId}/documentos`

## 1. Modelo funcional

Uma versão documental possui:

| Campo | Significado |
|---|---|
| `tipoFato` | `CONTRATO_ORIGINAL`, `ADITAMENTO`, `REMANEJAMENTO`, `TRANSFERENCIA_GESTAO`, `ENCERRAMENTO_ANTECIPADO`, `ANULACAO`, `VINCULO`, `ALOCACAO` ou `OUTRO` |
| `fatoId` / `fatoChave` | Identificação do fato publicado ou futuro |
| `grupoLogicoId` | Identidade estável entre versões |
| `versaoDocumento` | Sequência da versão |
| `estado` | `RASCUNHO`, `PUBLICADO`, `SUBSTITUIDO` ou `INVALIDO` |
| `classificacaoAcesso` | `NEGOCIAL` ou `PESSOAL_RESTRITO` |
| `tipoFonte` | Derivado como `ANEXO`, `SEI` ou `ANEXO_E_SEI` |
| `referenciaSei` | Referência textual, não única entre Contratos |
| `sha256Arquivo` | Integridade do conteúdo físico |
| `dataEfetiva` | Data em que a fonte sustenta o fato |
| `registradoEm` | Data/hora de conhecimento administrativo |

## 2. Criar fonte

`POST /api/contratos/{contratoId}/documentos`

Conteúdo: `multipart/form-data`.

- parte `metadados`: JSON obrigatório;
- parte `arquivo`: opcional quando existe `referenciaSei`.

Exemplo da parte `metadados`:

```json
{
  "tipoFato": "ADITAMENTO",
  "fatoId": null,
  "fatoChave": "ADIT-2026-04",
  "titulo": "Quarto termo aditivo",
  "descricao": "Fonte preparada antes da confirmação",
  "referenciaSei": "53180.000123/2026-10",
  "classificacaoAcesso": "NEGOCIAL",
  "dataEfetiva": "2026-10-01",
  "chaveIdempotencia": "cliente-9b39e2"
}
```

Informe arquivo, SEI ou ambos. Para fato futuro diferente do Contrato original, `fatoChave` é obrigatório. O Gestor Local deve pertencer à Gestão responsável pelo Contrato.

## 3. Consultar e baixar

| Método | Rota | Observação |
|---|---|---|
| GET | `/` | Lista versões visíveis; `finalidade` permite solicitar itens restritos autorizados |
| GET | `/{documentoId}` | Consulta metadados |
| GET | `/{documentoId}/eventos` | Retorna a cadeia histórica após validar seus hashes |
| GET | `/{documentoId}/arquivo` | Recalcula SHA-256 e devolve como anexo com `nosniff` |

Para `PESSOAL_RESTRITO`, enviar `?finalidade=...`. A finalidade não concede acesso por si só: o perfil e a permissão vigente também são validados. Todo acesso autorizado é registrado.

Rascunho fica visível somente ao Gestor Local responsável.

## 4. Retificar SEI

`POST /{documentoId}/retificar-sei`

```json
{
  "novaReferenciaSei": "53180.000124/2026-56",
  "motivo": "Correção de erro material na referência",
  "chaveIdempotencia": "ret-sei-9b39e2"
}
```

A versão publicada anterior passa a `SUBSTITUIDO`; a nova permanece `PUBLICADO`. O conteúdo anterior não é removido.

## 5. Substituir ou acrescentar anexo

`POST /{documentoId}/substituir`

Conteúdo: `multipart/form-data` com `metadados` e arquivo opcional.

```json
{
  "titulo": "Contrato assinado",
  "descricao": "Via integral recebida após a publicação",
  "referenciaSei": null,
  "motivo": "Acréscimo do arquivo ao registro inicialmente publicado por SEI",
  "chaveIdempotencia": "sub-doc-9b39e2"
}
```

Referência SEI nula mantém o valor anterior. Se a versão anterior continha somente SEI e a nova recebe arquivo, o evento é `ANEXO_ACRESCENTADO`.

## 6. Invalidar e descartar

### Invalidar versão publicada

`POST /{documentoId}/invalidar`

```json
{
  "motivo": "Arquivo associado ao fato incorreto",
  "autorizacaoAdministrativa": null
}
```

O estado passa a `INVALIDO`; o arquivo e a trilha permanecem.

### Descartar rascunho

`DELETE /{documentoId}`

Permitido somente se:

- o estado é `RASCUNHO`;
- nunca houve publicação;
- não há referência SEI;
- o arquivo físico é exclusivo.

## 7. Permissões restritas

### Conceder

`POST /{documentoId}/permissoes`

```json
{
  "usuarioId": 45,
  "finalidade": "Auditoria institucional do processo",
  "justificativa": "Necessidade formal registrada no chamado",
  "autorizacaoAdministrativa": "AUT-2026-0091",
  "validoAte": "2026-09-30T23:59:59Z"
}
```

Somente Administração técnica concede. O beneficiário Operacional é recusado.

### Revogar

`POST /{documentoId}/permissoes/{permissaoId}/revogar`

```json
{
  "motivo": "Finalidade concluída",
  "autorizacaoAdministrativa": "AUT-2026-0091-R"
}
```

### Auditar

| Método | Rota | Perfis |
|---|---|---|
| GET | `/{documentoId}/permissoes` | Administração ou Gestor Local responsável |
| GET | `/{documentoId}/acessos` | Administração ou Gestor Local responsável |

## 8. Respostas de erro relevantes

| HTTP | Situação típica |
|---:|---|
| 400 | Metadados, fonte, finalidade ou motivo ausente |
| 403 | Perfil, escopo ou permissão insuficiente |
| 404 | Contrato, documento, arquivo lógico ou permissão inexistente |
| 409 | Versão não editável, tentativa de apagar publicado, checksum divergente ou cadeia inválida |
| 413 | Arquivo acima do limite configurado |
| 415 | Extensão, MIME ou assinatura binária inválida |

Operações de escrita possuem confirmação de senha por `@ExigirSenha`. Envie chave de idempotência distinta por comando negocial e repita a mesma chave apenas ao reenviar exatamente a mesma intenção.
