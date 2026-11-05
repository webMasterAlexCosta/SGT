# IND-08 — contrato aditivo de leitura histórica por campo

**Estado:** contrato definido para testes RED e implementação na cópia de evolução. Não é declaração de implementação nem de execução aprovada.

**Fontes normativas:** `trabalho/backend/docs/LEVANTAMENTO_REQUISITOS.md`, GOV-003, TMP-003/004/005/006/008/010/012/016, REL-003, AUD-001/002/003/007, HIS-001/004/005/006 e PER-006/009/011; `trabalho/backend/docs/INTEGRACAO_FRONTEND.md`, 3.1, 3.3 e 9. O pedido de evolução de 01/11/2026 autoriza esta derivação sem alterar os originais.

## 1. Forma comum

Cada objeto de leitura histórica mantém seus escalares e recebe um mapa `provaCampos`. A chave é o nome exato do escalar daquele objeto. O valor factual permanece no escalar: o mapa não o duplica.

```json
{
  "matriculaTipoContratacaoObrigatoria": false,
  "diaSemanaReduzido": null,
  "tipoContratacaoDescricao": null,
  "provaCampos": {
    "matriculaTipoContratacaoObrigatoria": {
      "estado": "VALOR_COMPROVADO",
      "visibilidade": "DISPONIVEL",
      "origens": [{
        "tipo": "PROVA_HISTORICA",
        "provaId": 73,
        "eventoId": 11,
        "versaoId": 19,
        "dataEfetiva": "2025-08-01",
        "registradoEm": "2026-11-01T01:30:00.123456Z"
      }]
    },
    "diaSemanaReduzido": {
      "estado": "AUSENCIA_COMPROVADA",
      "visibilidade": "DISPONIVEL",
      "origens": [{
        "tipo": "PROVA_HISTORICA",
        "provaId": 74,
        "eventoId": 11,
        "versaoId": 20,
        "dataEfetiva": "2025-08-01",
        "registradoEm": "2026-11-01T01:40:00.000001Z"
      }]
    },
    "tipoContratacaoDescricao": {
      "estado": "NAO_COMPROVADO",
      "visibilidade": "DISPONIVEL",
      "origens": []
    }
  }
}
```

Os IDs acima são ilustrativos. Respostas reais só usam IDs de registros existentes.

| Estado | Significado |
|---|---|
| `VALOR_COMPROVADO` | Existe fonte aplicável para o valor deste campo. Inclui `0` e `false`. |
| `AUSENCIA_COMPROVADA` | A fonte declara expressamente a ausência do valor opcional. Não equivale a uma lacuna. |
| `NAO_COMPROVADO` | As fontes conhecidas e aplicáveis não certificam o campo. |

`visibilidade` é um eixo independente: `DISPONIVEL` ou `RESTRITA`. Quando o próprio conteúdo/metadado é restrito, a resposta usa `estado: null`, `origens: []` e escalar nulo ou omitido. Restrição não autoriza concluir `NAO_COMPROVADO` nem `AUSENCIA_COMPROVADA`.

```json
{
  "provaCampos": {
    "referenciaSei": {
      "estado": null,
      "visibilidade": "RESTRITA",
      "origens": []
    }
  }
}
```

O frontend verifica visibilidade antes de apresentar o escalar. Uma resposta antiga sem `provaCampos` não permite deduzir ausência comprovada a partir de `null`. O fallback deve indicar que a resposta não trouxe comprovação explícita, sem consultar cadastros/cache de outro recorte ou leitor.

## 2. Proveniência

Uma entrada pode ter várias `origens` quando o valor resulta de múltiplas fontes aplicáveis. Não selecionar arbitrariamente o último mapa de uma composição convergente.

Tipos previstos:

- `FOTOGRAFIA_CONTRATUAL`: versão persistida original ou fotografia de ato próprio.
- `PROVA_HISTORICA`: revisão documental que declara o campo.
- `EVENTO_CONTRATUAL`: ato canônico que altera aquele campo/eixo.
- `COMPOSICAO_PUBLICADA`: composição original preservada.
- `ITEM_ADITAMENTO`: item que introduziu configuração ou alterou quantitativo.
- `MAPEAMENTO_TRANSFERENCIA`: mapa real da transferência que introduziu o contexto de destino.

Cada origem contém somente as referências pertinentes: `provaId`, `eventoId`, `versaoId`, `composicaoId`, `aditamentoComposicaoId`, `mapeamentoId`, `dataEfetiva` e `registradoEm`. Referências não pertinentes podem estar ausentes ou nulas. `tipo`, `dataEfetiva` e `registradoEm` não autorizam inventar um registro quando ele não existe; um instante não preservado permanece nulo.

Não são expostos SEI, título/identidade/hash de documento pessoal, conteúdo de anexo, motivo, autorização administrativa, identificação de ator ou PII pela proveniência. As rotas documentais continuam responsáveis por autorização, finalidade e auditoria próprias.

## 3. Rotas e posição do mapa

| Leitura | Forma aditiva |
|---|---|
| `GET /api/contratos/{id}/posicao-temporal?data=D&conhecidoAte=K` | `provaCampos` na raiz para eixos/vigência; `versaoContratual.provaCampos` para escalares da versão efetiva. `dataReferencia` e `conhecidoAte` existentes são os cortes aplicados. |
| `GET /api/contratos/{id}/versoes?conhecidoAte=K` | Cada item mantém os escalares da versão persistida e acrescenta `provaCampos`. K é opcional e limita o acervo conhecido. Não aplica a uma fotografia antiga provas incorporadas depois dela. |
| `GET /api/contratos/{id}/composicoes?data=D&conhecidoAte=K` | Cada item mantém os escalares existentes e recebe `lotacaoMunicipio`, `jornadaSemanal`, `dataReferencia`, `conhecidoAte` e `provaCampos`. O novo K é opcional; omiti-lo conserva a consulta ao conhecimento atual. |
| `GET /api/contratos/{id}/provas-historicas?conhecidoAte=K` | Cada prova mantém metadados existentes e acrescenta `camposComprovados`, `fotografia`, `composicoes` e `provaCampos` para os metadados sensíveis omitidos. |
| `POST /api/contratos/{id}/provas-historicas` | Mesmo recibo aditivo de prova. Requests, prévia e idempotência conservam seu contrato. |

`VersaoContratoNegocialDTO` continua sendo o DTO interno/de escrita. Wrappers/DTOs exclusivos de leitura preservam o JSON escalar de `/versoes` e da versão aninhada da posição. Metadados não participam do hash da intenção, não são copiados do request e não são persistidos como uma fotografia nova.

### 3.1. Campos da versão

O mapa cobre `dataEfetiva`, `numeroContrato`, `anoContrato`, `descricao`, `tipoContratacaoId`, `tipoContratacaoDescricao`, `fornecedorId`, `cnpjEmpresa`, `razaoSocialEmpresa`, `nomeFantasiaEmpresa`, `dataInicio`, `dataFim`, `efetivoPrevisto`, `gestaoLocalId`, `gestaoLocalNome`, `matriculaVinculoObrigatoria`, `matriculaTipoContratacaoObrigatoria`, `matriculaVinculoObrigatoriaEfetiva`, `diaSemanaReduzido`, `horasDiaReduzido` e `habilitacaoAdministrativa`.

A identidade técnica da projeção não recebe prova artificial: `id` permanece nulo quando não há versão persistida correspondente. `numeroVersao` de uma projeção não se transforma em ID ou em comprovação da fotografia integral. As referências em `origens` identificam apenas fontes reais que contribuíram para cada campo.

### 3.2. Campos da composição

O mapa cobre `lotacaoId`, `lotacaoNome`, `jornadaId`, `jornadaDescricao`, `cargoId`, `cargoNome`, `efetivoPrevisto`, `dataInicioVigencia`, `dataFimVigencia`, `permiteNovaAlocacao`, `lotacaoMunicipio` e `jornadaSemanal`.

Configuração nova introduzida por ADI pode continuar com `id: null`. Sua origem usa `aditamentoComposicaoId`. Uma transferência usa mapas reais, inclusive múltiplos mapas em convergência. Não se fabrica ID de composição pela coincidência dos catálogos.

### 3.3. Provas e fragmentos

`camposComprovados` é a máscara **daquela revisão**, sem expandi-la com campos herdados. `fotografia` contém a versão gerada persistida com `provaCampos` cumulativo somente até essa revisão. Assim um campo herdado continua apontando para a revisão/fotografia que o comprovou.

`composicoes` contém somente os fragmentos declarados naquela revisão:

```json
{
  "id": 74,
  "numeroRevisao": 2,
  "camposComprovados": [],
  "composicoes": [{
    "composicaoId": 8,
    "mapeamentoId": null,
    "aditamentoComposicaoId": null,
    "lotacaoNome": null,
    "jornadaDescricao": null,
    "cargoNome": null,
    "lotacaoMunicipio": null,
    "municipioComprovado": true,
    "jornadaSemanal": null,
    "jornadaComprovada": false,
    "provaCampos": {
      "lotacaoMunicipio": {
        "estado": "AUSENCIA_COMPROVADA",
        "visibilidade": "DISPONIVEL",
        "origens": [{
          "tipo": "PROVA_HISTORICA",
          "provaId": 74,
          "eventoId": 11,
          "versaoId": 20,
          "composicaoId": 8,
          "dataEfetiva": "2025-08-01",
          "registradoEm": "2026-11-01T01:40:00.000001Z"
        }]
      },
      "jornadaSemanal": {
        "estado": "NAO_COMPROVADO",
        "visibilidade": "DISPONIVEL",
        "origens": []
      }
    }
  }]
}
```

O fragmento municipal não comprova grade, rótulo ou valor de outro fragmento. A posição efetiva, diferentemente desse fragmento isolado, pode herdar a grade comprovada por revisão anterior, mantendo sua origem.

## 4. D, K, ausência e herança

- O replay aplica a cadeia válida em D e somente o conhecimento registrado até K. K exato é inclusivo, na precisão real em microssegundos; K um nanossegundo antes não conhece a nova prova.
- Provas parciais usam `camposComprovados`, `municipioComprovado` e `jornadaComprovada`. Não certificam a fotografia inteira por conter valores não nulos.
- Um escalar nulo só vira ausência comprovada quando a máscara/flag/fotografia originalmente íntegra para aquela propriedade demonstra a ausência. Nomes ausentes sem flag continuam desconhecidos.
- Políticas nulas podem representar herança quando a fotografia ou ato explícito a demonstram; desconhecimento não vira herança automática.
- Uma prova de matrícula não prova ausência de dia reduzido. Uma prova de grade não prova ausência municipal. Um ID conhecido não prova o rótulo histórico correspondente.
- Deltas quantitativos não renomeiam a configuração; configuração de item ADI só vale quando o item introduz aquela combinação.
- O fim de uma combinação introduzida por ADI herda a vigência pactuada no seu ponto de entrada, reconstruída da fotografia original e dos deltas conhecidos/aplicáveis. Sua origem pode ser a versão original ou o evento de prorrogação. A projeção atual do contrato não fornece esse valor. A fotografia de fim da composição-base permanece preservada; a vigência operacional posterior depende do contrato, inclusive em prorrogação válida.
- Um `0` técnico produzido por fallback não se torna comprovação; zero e falso documentados são valores preservados.
- Mapas convergentes só certificam um valor agregado quando as origens aplicáveis o certificam consistentemente. Divergência mantém desconhecimento, sem buscar o cadastro atual.
- Uma fotografia derivada de ADI, transferência ou correção é interpretada no D da própria fotografia e K de seu registro. Herda prova por campo; sua materialização não transforma prova parcial em integral. O valor armazenado é preservado, mas só recebe prova do replay se coincidir com o valor sustentado naquele recorte. Prova posterior não reclassifica a versão antiga.
- A regra de captura integral exige fotografia vinculada ao evento real `PUBLICACAO` (HIS-006). O fluxo canônico captura juntos matrícula e dia reduzido; somente nessa origem o marcador legado de matrícula efetiva pode comprovar ausência dos opcionais. Ordinal `numeroVersao=1` sem evento não autentica a origem. Um evento de publicação separado e aplicável pode comprovar o estado negocial, sem certificar os campos da fotografia sem vínculo.

## 5. Data de incorporação e privacidade

O instante de incorporação é `ProvaHistoricaContrato.registradoEm`. `dataEfetiva` responde quando o fato valeu e `dataDocumento` tem finalidade documental; nenhum substitui o instante de conhecimento.

O fuso negocial documentado é `America/Sao_Paulo` (`INTEGRACAO_FRONTEND.md:105`, FRE-002 e `RelogioSistema.ZONA`). A interface preserva datas civis e apresenta instantes nesse fuso explicitamente. Filtros por dia civil usam início do dia até início do seguinte, sem arredondar para `23:59:59`.

O mapa de metadados da prova declara `RESTRITA` para `referenciaSei`, `motivo`, `autorizacaoAdministrativa`, `documentoComprobatorioId` e `hashFonteArquivo`; os respectivos escalares continuam omitidos. A leitura da prova não passa a conceder acesso documental a qualquer perfil. O estado factual e as origens desses metadados são nulos/vazios, independentemente de existirem valores internos.

## 6. Verificação planejada

Testes permanentes exercitam API/Spring/autorização/JPA/MySQL reais com fontes sintéticas explícitas, comparando os estados e referências em publicação, versões, posição, prova e composição. Incluem revisão parcial, K exato/anterior, município ausente/desconhecido, zero/falso, herança, ausência de ID físico projetado, isolamento de escopo e minimização. A execução, RED/GREEN e prova de transporte TCP/reinício pertencem à coordenação serializada da raiz. Até seus resultados, o contrato permanece uma especificação testável, sem alegação de aprovação.

## 7. Consumidores React e separação de leituras

Todos os consumidores das rotas evoluídas devem interpretar o mapa antes de formatar ou reaproveitar os escalares. O ID técnico de uma versão ou composição existente continua servindo como referência; isso não comprova nome, número contratual, catálogo ou qualquer outro campo do registro.

| Consumidor | Dados abrangidos |
|---|---|
| `PaginaHistorico` / `ResultadosHistoricos` | Posição D/K, versão aninhada, versões persistidas do acervo/publicação, composições D/K e por D/conhecimento atual, recibos de provas, fotografias e fragmentos. |
| `ModalDetalhesContrato` via `ServicoContrato` | Eixos apresentados da posição temporal; número, ano e data efetiva de cada versão. O resumo do contrato atual tem DTO próprio e continua separado. |
| `PainelAlteracoesContrato` via `ServicoAlteracoesContrato` | Nomes, efetivo e vigências nos cartões de composição; rótulos das opções de aditamento e remanejamento e da confirmação de exclusão do rascunho. |
| `PainelExcecoesContrato` via `ServicoAlteracoesContrato` | Opções de composição de origem e preenchimento de IDs de catálogo na transferência. Escalar restrito, não comprovado ou ausente não é copiado automaticamente para o formulário. |

O leitor comum aplica os mesmos estados e a mesma visibilidade à renderização rica e ao texto dos elementos `<option>`. O fallback escalar continua restrito às respostas sem o mapa. Propriedade de negócio ausente de um mapa presente não recebe um valor por fallback.

### 7.1. Preparação de rascunho

`GET /composicoes` de contrato `RASCUNHO` retorna os escalares atuais de preparação no wrapper de leitura, com campos `NAO_COMPROVADO` e `DISPONIVEL`: não há prova publicada a reconstruir. O formulário de edição usa esses escalares no contexto explícito de preparação do rascunho, sem apresentá-los como fotografia histórica. O mapa não impede editar o rascunho. Os DTOs de POST/PUT permanecem inalterados; a preparação não fabrica metadados de comprovação.

### 7.2. Leituras com DTOs próprios

Relatórios administrativos, relatórios de terceirizados, emissões e exportadores de frequência/painel/auditoria não passam a receber `provaCampos` por este contrato. Também excedentes, impactos de prévia e catálogos atuais têm respostas próprias. Seus consumidores mantêm a semântica anterior e entram nas regressões; o frontend não converte escalares desses DTOs em prova histórica nem propaga mapas de outra consulta. Essa separação não dispensa os quatro consumidores da tabela acima de interpretar os mapas efetivamente recebidos.
