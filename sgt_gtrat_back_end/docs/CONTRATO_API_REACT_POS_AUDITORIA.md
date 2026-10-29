# Contrato de integração React — pós-auditoria

## Sessão, 2FA e CSRF

O cliente deve usar cookies (`credentials: "include"`). Antes de qualquer POST/PUT/PATCH/DELETE, execute `GET /api/autenticacao/csrf`. Guarde o valor `token` da resposta e envie-o no cabeçalho indicado por `headerName` (normalmente `X-XSRF-TOKEN`). O cookie `XSRF-TOKEN` possui caminho `/` e é legível pela SPA.

Login:

```json
{
  "matricula": "12345678",
  "password": "senha",
  "totp": "123456"
}
```

Quando o usuário possui 2FA ativo e o código não foi enviado, a API responde **202** com `requiresTwoFactor=true` e `canSkipTwoFactor=false`. Código incorreto não cria sessão; código correto conclui o login. Não existe `skipTwoFactor` no contrato novo.

## Pessoa

`GET /api/pessoas` retorna um `Page<PessoaResumoDTO>` e aceita:

- `busca`, `contratoId`, `ativoEm`;
- `page` e `size`;
- a ordenação efetiva é fixa no servidor para paginação estável.

Detalhe: `GET /api/pessoas/{id}`. Não existe POST isolado de Pessoa. Para criar uma pessoa operacional, use a operação integrada de Vínculo.

## Vínculo e inclusão excedente

`GET /api/vinculos-contratuais` retorna `Page<VinculoContratualDTO>` e aceita `contratoId`, `pessoaId`, `estado`, `inicioDe`, `inicioAte`, `ativoEm`, `busca`, `page` e `size`.

Criação integrada: `POST /api/vinculos-contratuais`.

```json
{
  "pessoaId": null,
  "pessoa": { "nome": "Pessoa", "cpf": "52998224725" },
  "contratoId": 10,
  "dataInicio": "2026-10-01",
  "dataFim": null,
  "matricula": "M-001",
  "composicaoId": 20,
  "confirmarInclusaoExcedente": false,
  "justificativaExcedente": null,
  "evidenciaExcedente": null,
  "autorizacaoExcedente": null
}
```

Antes de criar, consulte `GET /api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao?data=AAAA-MM-DD`. Cada opção informa `efetivoPrevisto`, `ocupacaoAtual`, `ocupacaoAposInclusao`, `excedenteAposInclusao` e `exigeJustificativaExcedente`.

Se a inclusão exceder a capacidade, somente o Gestor Local pode confirmar, e os quatro campos de excedente devem ser preenchidos. Capacidade zero nunca admite inclusão. O backend refaz o cálculo sob lock; a prévia do navegador não é fonte de verdade.

Monitor: `GET /api/contratos/{contratoId}/composicoes/excedentes?data=AAAA-MM-DD`.

## Frequência canônica

POST `/api/frequencias` e POST `/api/frequencias/lote` exigem `vinculoContratualId`. `pessoaId`, `alocacaoVinculoId` e os snapshots organizacionais são resolvidos pelo backend na data da frequência. O React não cadastra nem escolhe um `Empregado` paralelo.

```json
{
  "vinculoContratualId": 123,
  "dataReferencia": "2026-09-10",
  "status": "PRESENCA",
  "entrada1": "08:00",
  "saida1": "12:00",
  "entrada2": "13:00",
  "saida2": "17:00"
}
```

O lote não aceita mistura de identidade canônica com a projeção operacional interna.

## Precedência no último dia

Em alteração/retificação de Alocação cuja `dataEfetiva` seja igual ao último dia do Vínculo, envie `confirmarPrecedenciaUltimoDia=true`. A regra é: a Alocação vale durante o último dia; o desligamento produz efeito após esse dia.

## Correção de anulação

`POST /api/contratos/{id}/corrigir-anulacao` exige o ID do evento de anulação, a mesma data efetiva, relação `CANCELA`, motivo e referência documental:

```json
{
  "dataEfetiva": "2026-09-01",
  "dataAto": "2026-09-10",
  "motivo": "Anulação lançada indevidamente",
  "referenciaSei": "SEI-0001",
  "eventoPrecedenteId": 456,
  "relacaoPrecedencia": "CANCELA",
  "ordemDocumental": 2
}
```

A correção não apaga a anulação, não reabre vínculos e mantém o contrato administrativamente inativo. A retomada exige reativação explícita.

## Autorização da Presidência

Presidência possui leitura nacional dos relatórios e painéis autorizados. Emissão formal e demais escritas continuam proibidas para esse perfil.
