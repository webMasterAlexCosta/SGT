# SGT-GTRAT — API de perfis e permissões da Etapa 10

**Base:** `/api`  
**Autenticação:** JWT existente  
**Conteúdo:** `application/json`, exceto documentos já definidos na Etapa 08

## 1. Contrato canônico de perfil

Valores de `perfil`:

```text
ADMINISTRADOR
GESTOR_NACIONAL
GESTOR_ESTADUAL
GESTOR_LOCAL
PRESIDENTE
OPERACIONAL
```

Authorities derivadas:

```text
PERFIL_ADMINISTRADOR
PERFIL_GESTOR_NACIONAL
PERFIL_GESTOR_ESTADUAL
PERFIL_GESTOR_LOCAL
PERFIL_PRESIDENTE
PERFIL_OPERACIONAL
```

Respostas de autenticação, perfil e usuário expõem `perfil` e `autoridade`. Requisições de criação/atualização de usuário recebem `perfil`. Os campos antigos `role` e `roles` não são aceitos nem retornados.

Exemplo:

```json
{
  "perfil": "GESTOR_ESTADUAL",
  "autoridade": "PERFIL_GESTOR_ESTADUAL"
}
```

## 2. Permissões excepcionais

### Listar

```http
GET /api/permissoes-excepcionais
```

Administrador visualiza todas. Demais perfis autorizados visualizam apenas concessões próprias.

### Conceder

```http
POST /api/permissoes-excepcionais
```

Exclusivo do Administrador e protegido por confirmação de senha (`@ExigirSenha`).

```json
{
  "tipo": "CORRECAO_IDENTIFICADOR_CONTRATO",
  "usuarioBeneficiarioId": 42,
  "contratoId": 1001,
  "gestaoLocalId": null,
  "finalidade": "Corrigir identificação conforme processo formal",
  "justificativa": "Divergência comprovada no instrumento publicado",
  "autorizacaoAdministrativa": "SEI-2026/123456",
  "validoAte": "2026-09-10T18:00:00Z"
}
```

Regras:

- correção de identificador, redução e transferência exigem Gestor Local e contrato específico de sua Gestão;
- intervenção técnica exige Administrador beneficiário e contrato específico;
- dados pessoais restritos exigem Gestor Estadual, Gestor Nacional ou Presidente e contrato/Gestão específica;
- a validade deve estar no futuro;
- não pode haver duplicidade ativa do mesmo tipo, beneficiário e escopo.

### Revogar

```http
POST /api/permissoes-excepcionais/{id}/revogar
```

```json
{
  "motivo": "Processo administrativo encerrado"
}
```

Exclusivo do Administrador e protegido por confirmação de senha.

### Consultar usos

```http
GET /api/permissoes-excepcionais/{id}/usos
```

Exclusivo do Administrador. Cada item contém permissão, tipo, usuário, recurso, finalidade e instante.

## 3. Correção de número/ano do contrato

### Executar

```http
POST /api/contratos/{contratoId}/correcoes-identificador
```

```json
{
  "novoNumero": "CTR-000123",
  "novoAno": 2026,
  "dataEfetiva": "2026-09-09",
  "dataAto": "2026-09-09",
  "motivo": "Correção de erro material no identificador",
  "referenciaSei": "SEI-2026/123456",
  "autorizacaoAdministrativa": "SEI-2026/123456",
  "chaveIdempotencia": "corr-id-1001-20260909-01",
  "eventoPrecedenteId": null,
  "relacaoPrecedencia": null,
  "ordemDocumental": null
}
```

Pré-condições:

- usuário atual é Gestor Local do contrato;
- contrato está publicado;
- documento comprobatório publicado referencia `CORRECAO_IDENTIFICADOR` e o `referenciaSei` informado;
- existe permissão `CORRECAO_IDENTIFICADOR_CONTRATO` vigente para o usuário/contrato;
- `autorizacaoAdministrativa` coincide com a concessão;
- novo número/ano difere do atual;
- chave de idempotência não foi usada em outra operação.

Retorno: `201 Created` com identificador da correção, contrato, evento, permissão, antes/depois, datas, fonte, autorização e responsável.

### Histórico do contrato

```http
GET /api/contratos/{contratoId}/correcoes-identificador
```

### Pesquisa por identificador antigo ou novo

```http
GET /api/contratos/historico-identificador?numero=CTR-000123&ano=2026
```

Os resultados são filtrados pelo escopo do usuário atual.

## 4. Dados pessoais restritos

```http
GET /api/pessoas/{pessoaId}/dados-restritos?finalidade=Auditoria%20do%20processo%20SEI-2026-123456
```

- Gestor Local recebe a visão completa no próprio escopo;
- Operacional é negado;
- Gestor Estadual, Gestor Nacional e Presidente precisam de `ACESSO_DADOS_PESSOAIS_RESTRITOS`;
- Administrador precisa de `INTERVENCAO_NEGOCIAL_TECNICA`;
- todo uso excepcional exige finalidade não vazia e gera trilha de auditoria.

Nas consultas comuns, o consumidor deve aceitar os campos nulos/mascarados definidos pela política do perfil e nunca tentar reconstruir valores protegidos.

## 5. Impacto obrigatório no React futuro

1. Substituir toda leitura/envio de `role` ou `roles` por `perfil`.
2. Usar `autoridade` apenas na integração com guards; não manter lista própria divergente.
3. Ocultar ações negociais para qualquer perfil diferente de `GESTOR_LOCAL`.
4. Criar telas administrativas para concessão, revogação e usos.
5. Exigir `finalidade` ao solicitar dado pessoal restrito.
6. Exibir nível de exposição e máscaras sem tentar reconstruir valores.
7. Enviar chave de idempotência estável na correção de número/ano.
8. Tratar `403`, `409` e expiração de permissão como estados de domínio, não como erro genérico.

## 6. Compatibilidade

Esta é uma alteração deliberadamente incompatível com o contrato de perfis anterior. Como o projeto usa banco novo durante o desenvolvimento, não existe endpoint, coluna ou adaptador de compatibilidade para `roles`.
