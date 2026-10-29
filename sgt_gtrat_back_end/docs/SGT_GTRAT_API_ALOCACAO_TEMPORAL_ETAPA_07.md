# SGT-GTRAT — API de Alocação temporal da Etapa 07

Data-base: **09/09/2026**  
Base das rotas: `/api`

## 1. Consultas de Alocação

| Método | Rota | Parâmetros |
|---|---|---|
| GET | `/vinculos-contratuais/{id}/alocacoes` | `mes=YYYY-MM` opcional |
| GET | `/vinculos-contratuais/{id}/alocacoes/eventos` | Sem parâmetros adicionais |
| GET | `/vinculos-contratuais/{id}/alocacoes/posicao` | `data=YYYY-MM-DD`; `conhecidoAte` opcional em ISO-8601 |
| GET | `/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao` | `data`; `lotacaoId` e `jornadaId` opcionais |

A listagem mensal usa sobreposição de períodos: uma faixa iniciada antes do mês ou terminada depois dele aparece se possuir pelo menos um dia efetivo dentro do mês.

`conhecidoAte` permite responder como a informação era conhecida naquele instante. Sem esse parâmetro, a API retorna a verdade efetiva reconstruída com todos os eventos conhecidos atualmente.

## 2. Fluxo de alteração

### 2.1 Gerar prévia

`POST /vinculos-contratuais/{id}/previas/alocacoes`

```json
{
  "dataEfetiva": "2026-10-01",
  "composicaoId": 120,
  "lotacaoId": 31,
  "jornadaId": 44,
  "cargoId": 58,
  "justificativa": "Movimentação interna autorizada",
  "evidencia": "SEI-00000.000000/2026-00",
  "autorizacao": "ATO-2026-015",
  "confirmarImpacto": false,
  "tokenPrevia": null
}
```

A combinação pode ser informada por `composicaoId`. Os identificadores de Lotação, Jornada e Cargo permitem validar o recorte selecionado e suportam combinações temporais de aditamento sem composição-base persistida.

Resposta resumida:

```json
{
  "token": "assinatura-da-previa",
  "vinculoId": 77,
  "alocacaoAfetadaId": 901,
  "dataEfetiva": "2026-10-01",
  "fimFaixaAtual": "2026-12-31",
  "impactos": ["faixa temporal de Lotação + Turno + Cargo"],
  "incompatibilidades": []
}
```

### 2.2 Confirmar alteração

`POST /vinculos-contratuais/{id}/alocacoes`

Repetir a mesma requisição, agora com:

```json
{
  "confirmarImpacto": true,
  "tokenPrevia": "assinatura-da-previa"
}
```

Todos os demais campos devem permanecer iguais aos usados na prévia. O token é revalidado na confirmação; mudanças no contexto invalidam a operação.

### 2.3 Retificar faixa na mesma data

`POST /vinculos-contratuais/{id}/alocacoes/{alocacaoId}/retificar`

Usa o mesmo corpo da alteração. A data efetiva deve ser o início da faixa retificada. A faixa anterior passa a `SUBSTITUIDA`, a correção recebe novo identificador e ambos os fatos permanecem na auditoria.

### 2.4 Cancelar programação futura

`POST /vinculos-contratuais/{id}/alocacoes/{alocacaoId}/cancelar-programacao`

```json
{
  "justificativa": "Movimentação futura não ocorrerá",
  "evidencia": "SEI-00000.000000/2026-00",
  "autorizacao": "ATO-2026-016",
  "confirmarImpacto": true,
  "tokenPrevia": null
}
```

Somente faixa ainda futura pode ser cancelada por essa rota. A predecessora é restaurada sem criar lacuna.

## 3. Opções encadeadas

A rota de opções retorna apenas combinações com efetivo previsto positivo na composição temporal da data. Os filtros podem ser aplicados progressivamente:

1. informar apenas `contratoId` e `data` para listar Lotação;
2. acrescentar `lotacaoId` para restringir Turnos;
3. acrescentar `jornadaId` para restringir Cargos.

Cada item contém `efetivoPrevisto`, `ocupacaoAtual` e `disponivelParaNovaAlocacao`.

## 4. Transferência excepcional de Gestão

### 4.1 Executar

`POST /contratos/{id}/transferir-gestao`

```json
{
  "dataEfetiva": "2026-11-01",
  "dataAto": "2026-10-20",
  "gestaoLocalDestinoId": 9,
  "motivo": "Reorganização administrativa",
  "referenciaSei": "SEI-00000.000000/2026-00",
  "autorizacaoAdministrativa": "AUT-TRANSF-2026-003",
  "mapeamentos": [
    {
      "composicaoOrigemId": 120,
      "lotacaoOrigemId": 31,
      "jornadaOrigemId": 44,
      "cargoOrigemId": 58,
      "lotacaoDestinoId": 131,
      "jornadaDestinoId": 144,
      "cargoDestinoId": 158
    }
  ],
  "eventoPrecedenteId": null,
  "relacaoPrecedencia": null,
  "ordemDocumental": null
}
```

A operação é atômica. Se qualquer composição positiva ou Alocação posterior não possuir mapa válido, nada é transferido.

### 4.2 Consultar

`GET /contratos/{id}/transferencias-gestao`

A resposta expõe origem, destino, data efetiva, motivo, SEI, autorização, responsável, instante de registro e os mapeamentos completos com fotografias dos catálogos.

## 5. Regras de autorização e conflito

- operações de escrita exigem senha confirmada pelo mecanismo `@ExigirSenha`;
- alteração de Alocação exige permissão de gestão de empregado e acesso ao Contrato;
- transferência exige Gestor Local autorizado da Gestão de origem;
- catálogo inativo bloqueia nova Alocação atual ou futura;
- composição com efetivo zero é indisponível;
- lacuna, sobreposição, combinação ausente ou token divergente retorna conflito;
- inexistência de entidade retorna não encontrado;
- data fora do Vínculo/Contrato retorna requisição inválida ou conflito conforme o caso.

## 6. Observação de compatibilidade

Os endpoints usam tipos disponíveis em Java 17 e pertencem à aplicação Spring Boot 3.5.16. A frequência ainda será adaptada para consumir a consulta temporal na Etapa 09.

