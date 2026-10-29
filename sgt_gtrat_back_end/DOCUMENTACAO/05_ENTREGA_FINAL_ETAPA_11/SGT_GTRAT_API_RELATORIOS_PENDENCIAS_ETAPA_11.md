# SGT/GTRAT — API de relatórios e pendências da Etapa 11

**Data-base:** 10/09/2026  
**Prefixo:** `/api`

Todos os endpoints exigem autenticação. Administrador, Gestor Nacional, Gestor Estadual e Gestor Local podem consultar dentro do próprio escopo; apenas o Gestor Local responsável pode tratar pendências.

## 1. Central de Pendências

| Método | Caminho | Finalidade |
|---|---|---|
| `GET` | `/pendencias` | Lista paginada por estado, tipo, período e contratos |
| `GET` | `/pendencias/resumo` | Contadores do recorte acessível |
| `POST` | `/pendencias/{id}/iniciar-tratamento` | Move item aberto para tratamento |
| `POST` | `/pendencias/{id}/confirmar-resolucao` | Resolve somente após revalidar a origem |

Parâmetros de listagem: `estado`, `tipo`, `inicio`, `fim`, `contratoIds`, `pagina` e `tamanho`. Estados: `ABERTA`, `EM_TRATAMENTO`, `RESOLVIDA`. Tipos: `RETIFICACAO_FREQUENCIA`, `FIM_NATURAL_VIGENCIA`.

Corpo opcional das transições:

```json
{
  "observacao": "Tratamento conferido com a documentação do contrato."
}
```

O resumo informa `abertas`, `emTratamento`, `resolvidas`, `retificacoesFrequencia` e `finsNaturaisVigencia`; os dois últimos contadores consideram apenas pendências ativas.

## 2. Relatórios administrativos

| Método | Caminho | Finalidade |
|---|---|---|
| `POST` | `/relatorios/administrativos/previa` | Calcula a visão atual sem formalizar versão |
| `POST` | `/relatorios/administrativos/emissoes` | Cria emissão formal imutável e idempotente |
| `GET` | `/relatorios/administrativos/emissoes/{id}` | Recupera metadados e snapshot preservado |
| `POST` | `/relatorios/administrativos/emissoes/conhecimento?conhecidoAte=...` | Recupera a última emissão conhecida até um instante |
| `GET` | `/relatorios/administrativos/emissoes/{id}/comparacao-vigente` | Compara emissão histórica com cálculo atual |
| `GET` | `/relatorios/administrativos/emissoes` | Lista metadados por tipo, estado e período |

Tipos: `FREQUENCIA_CONSOLIDADA`, `ABSENTEISMO`. Formatos declarados: `VISUALIZACAO`, `XLSX`, `PDF`. Estados: `VIGENTE`, `SUPERADA`.

Exemplo de emissão formal:

```json
{
  "tipo": "FREQUENCIA_CONSOLIDADA",
  "formato": "XLSX",
  "inicio": "2026-08-01",
  "fim": "2026-08-31",
  "contratoIds": [10, 12],
  "lotacaoId": null,
  "jornadaId": null,
  "chaveIdempotencia": "frequencia-agosto-2026-v1"
}
```

A mesma chave enviada pelo mesmo usuário e com a mesma intenção retorna a emissão já criada. Reutilizar a chave com filtros diferentes resulta em conflito. O período máximo é de 367 dias inclusivos.

## 3. Conteúdo e metadados

Uma resposta formal contém:

```json
{
  "metadados": {
    "id": 81,
    "tipo": "FREQUENCIA_CONSOLIDADA",
    "formato": "XLSX",
    "versao": 2,
    "natureza": "RETIFICADA",
    "estado": "VIGENTE",
    "situacao": "RETIFICADA",
    "inicio": "2026-08-01",
    "fim": "2026-08-31",
    "sujeitoARevisao": false,
    "quantidadePendencias": 0,
    "hashConteudo": "sha256...",
    "emissaoAnteriorId": 57
  },
  "conteudo": {}
}
```

Também são retornados geração, autoria/perfil, aviso, tamanho do conteúdo, superação/causa/motivo e `fonteConhecidaAte`. O snapshot é a prova histórica; a comparação vigente pode retornar `alterado: true` sem modificar esse conteúdo.

## 4. Aviso de revisão nos relatórios existentes

Os DTOs de frequência consolidada e absenteísmo passam a incluir:

```json
{
  "revisao": {
    "sujeitoARevisao": true,
    "quantidadePendencias": 2,
    "mensagem": "SUJEITO A REVISÃO — 2 pendências de retificação."
  }
}
```

A mesma mensagem aparece de forma visível nas exportações XLSX e PDF. Para uma emissão administrativa definitiva, o consumidor deve primeiro formalizar a emissão com o mesmo recorte e depois solicitar a exportação correspondente.

## 5. Códigos relevantes

- `400`: filtros/período inválidos ou chave formal ausente;
- `403`: perfil sem acesso ou tentativa de tratamento por perfil não responsável;
- `404`: recurso inexistente ou fora do escopo;
- `409`: origem ainda não resolvida, pendência já resolvida ou conflito de idempotência.
