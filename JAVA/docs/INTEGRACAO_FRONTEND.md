# Integração do cliente React com o SGT/GTRAT

## 1. Escopo e estado deste guia

**Suplemento vigente de09/11/2026:** os recibos abaixo pertencem à candidata histórica. Na derivada atual, a janela usa N configurável/padrão30, conforme [regra vigente](REGRA_JANELA_FREQUENCIA.md); Governança de Terceiros permanece removida. Este guia não declara homologação da fonte atual.

Este documento descreve os fluxos canônicos das APIs atuais de Contrato, Pessoa, Vínculo, Alocação, frequência, relatórios, documentos e pendências. Os caminhos são relativos à raiz HTTP da aplicação. Os nomes de campos são os dos DTOs Java, inclusive quando diferem entre uma prévia e sua confirmação.

**Estado em 31/10/2026: guia conferido para a candidata07 congelada.** A fonte de compilação está identificada pelo manifesto SHA-256 `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`; o manifesto geral auditável é `89e4e5155b48941b15e08a344487c70f82826e649415fd4f5b9bc66d0cdea8e0`. O JAR validado tem SHA-256 `582dfe319873c94d58ad4b4ccea47311e98c9528ed8a24d18d01de4fad168946`. A regressão integral, a carga, as provas HTTP/TCP e o reinício foram concluídos e conciliados com essa fonte e esse artefato. O item 13.2 distingue o alcance de cada prova e seus limites.

O inventário do contexto Spring real contém **264 pares de método HTTP e rota, em 40 controladores**, publicados em `docs/MATRIZ_ENDPOINTS.json` e `docs/MATRIZ_ENDPOINTS.md`. Essa contagem não significa que todas as rotas foram exercitadas por TCP. A interface de versão dos cinco catálogos está comprovada na regressão integral atual: o GET canônico expõe `version`, e o PUT devolve a versão confirmada no cenário verificado. O item 6.3 descreve os oráculos; o cliente deve consumir a versão retornada, sem adivinhar nem incrementar o valor localmente.

As revisões técnicas foram encerradas sem defeito funcional confirmado aberto, mantendo **seis requisitos em E por bloqueio documental: CON-023, CAT-004, CAT-007, COL-010, HIS-001 e HIS-006**. Tratamento seguro de dados desconhecidos e testes com fontes sintéticas não fornecem a origem histórica autêntica que falta nesses requisitos. Este guia não declara aprovação integral dos 198 requisitos.

**O frontend não foi implementado nem homologado por esta revisão.** Seus 94 arquivos estáticos permanecem idênticos, byte a byte, ao original e ao checkpoint recebidos. Este documento orienta uma integração futura. A proposta anterior está preservada em `docs/HISTORICO/documentacao_intermediaria/INTEGRACAO_FRONTEND_candidata03_proposta.md`; a conferência das referências e provas atuais está registrada em `docs/HISTORICO/REVISAO_INTEGRACAO_FRONTEND_CANDIDATA07.json`.

A autoridade funcional é `docs/LEVANTAMENTO_REQUISITOS.md`. Referências a IDs abaixo identificam o enunciado literal dessa norma, e os caminhos Java identificam o contrato observado. Matrizes e documentos anteriores não substituem o DTO, a regra original ou a evidência vinculada ao hash da entrega.

## 2. Transporte, sessão e respostas

### 2.1. Sessão do navegador

Use `credentials: "include"` nas requisições. A autenticação usa cookies e vínculo de dispositivo; a aplicação cliente não precisa extrair ou armazenar um JWT em `localStorage`.

O início de sessão segue estas operações:

1. `GET /api/autenticacao/csrf`. A resposta em `data` contém `headerName`, `parameterName` e `token`.
2. `POST /api/autenticacao/autenticar`, com `matricula`, `password` e `totp` quando aplicável, enviando o token no cabeçalho informado pelo bootstrap, atualmente `X-XSRF-TOKEN`.
3. Após o login, consultar novamente `/api/autenticacao/csrf`: a autenticação invalida o cookie CSRF anterior.
4. Consultar `GET /api/autenticacao/sessao` e `GET /api/usuarios/meu-perfil` para montar a sessão e as capacidades da tela.

`GET /api/autenticacao/sessao?touch=true` e `POST /api/autenticacao/renovar-sessao` possuem finalidades próprias de manutenção de sessão. O cliente deve tratar `forceChangePassword` quando retornado. O encerramento explícito usa `POST /api/autenticacao/encerrar-sessao`.

Os comandos protegidos por `@ExigirSenha` exigem confirmação em sessão comum pelo cabeçalho `X-User-Password`. Uma sessão com 2FA habilitado e efetivamente validado pode satisfazer essa confirmação conforme o interceptor. A senha de confirmação não é campo do DTO negocial nem deve ser persistida junto ao formulário. A falta de confirmação retorna **428**; senha de confirmação inválida retorna **403**, sem significar necessariamente perda da sessão.

Para implantação em outra origem, a origem do React precisa estar autorizada na configuração CORS e os cookies precisam ser enviados. O servidor expõe `X-Request-Id` e `Content-Disposition` por CORS. Não dependa de ler outros cabeçalhos de diagnóstico em uma chamada entre origens sem conferir a configuração correspondente.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/AutenticacaoControlador.java`; `src/main/java/com/example/siggtrat/controlador/UsuarioAplicacaoControlador.java`; `src/main/java/com/example/siggtrat/servico/AutenticacaoServico.java`; `src/main/java/com/example/siggtrat/seguranca/InterceptadorExigirSenha.java`; `src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java`.

### 2.2. Envelope, listas e paginação

Uma resposta JSON de sucesso com corpo usa o envelope abaixo. O DTO específico está em `data`:

```json
{
  "timestamp": "2026-10-31T00:00:00Z",
  "status": 200,
  "error": null,
  "message": "OK",
  "requestId": "identificador-da-requisicao",
  "meta": null,
  "data": {},
  "path": "/api/contratos/123"
}
```

Datas e IDs do exemplo são ilustrativos. `message` não é uma enumeração de regra de negócio. Uma resposta **204** não precisa de leitura JSON. Downloads e fluxos binários não usam esse envelope.

Há mais de um formato de lista:

| API | Corpo dentro de `data` | Parâmetros usuais |
|---|---|---|
| Contratos, Pessoas, Vínculos, frequência e outros métodos que retornam `Page` | `content`, `totalElements`, `totalPages`, `number`, `size`, entre outros; o envelope também pode trazer `meta` | `page` e `size`, a partir da página zero |
| Pendências | `registros`, `totalElementos`, `totalPaginas`, `pagina`, `tamanho`, `primeira`, `ultima` | `pagina` e `tamanho` |
| Lista de emissões administrativas | `Page` dentro de `data` | `pagina` e `tamanho`, conforme seu controlador |
| Composições, instrumentos, documentos e eventos nos métodos que retornam `List` | Array diretamente em `data` | Filtros específicos; não presumir `content` |

A frequência recebe `contratoId` singular nos filtros. O consolidado de frequência e os relatórios administrativos recebem `contratoIds`. Mantenha essas diferenças no adaptador de cada recurso.

**Fontes:** `src/main/java/com/example/siggtrat/configuracao/RespostaApi.java`; `src/main/java/com/example/siggtrat/configuracao/TratadorRespostaSucesso.java`; `src/main/java/com/example/siggtrat/configuracao/MetadadosPagina.java`; `src/main/java/com/example/siggtrat/dto/PaginaPendenciasDTO.java`; `src/main/java/com/example/siggtrat/controlador/FrequenciaControlador.java`; `src/main/java/com/example/siggtrat/controlador/RelatorioControlador.java`; `src/main/java/com/example/siggtrat/controlador/RelatorioAdministrativoControlador.java`.

### 2.3. Tratamento de erros

Os erros não têm garantia de um `data` equivalente ao envelope de sucesso. O tratador de domínio usa `status`, `error`, `message` e, em alguns casos, `code` e `fields`. Filtros de segurança também podem produzir a resposta. Leia o status HTTP antes de interpretar o corpo e trate `message` como texto, nunca como HTML.

| Status | Significado relevante | Comportamento do cliente |
|---|---|---|
| **400** | Corpo, referência, data ou combinação inválida; validação de campos | Manter o formulário, apresentar `fields` quando existir e corrigir os dados. |
| **401** | Sessão ausente, inválida, expirada ou revogada | Atualizar o estado de sessão, remover dados de tela vinculados à autorização anterior e conduzir à autenticação. Não repetir a escrita automaticamente após login. |
| **403** | Perfil, escopo, finalidade ou concessão insuficientes; também pode ser CSRF ou confirmação de senha inválida | Distinguir `code` quando existir, reavaliar a sessão/capacidade e preservar a intenção para decisão humana. Não trocar a rota para tentar obter o mesmo conteúdo. |
| **404** | Recurso inexistente ou não visível no escopo atual | Informar indisponibilidade sem afirmar que o recurso foi apagado. Listas fora de escopo podem retornar vazias em vez de 404. |
| **409** | Conflito negocial, versão/impacto alterado, capacidade, precedência, identidade reservada, reutilização indevida de chave ou fonte histórica insuficiente | Reler recurso e histórico. Quando for prévia obsoleta, obter nova prévia e nova confirmação. Quando faltar prova ou houver dependência, tratar a causa; não usar repetição cega. |
| **428** | `PASSWORD_CONFIRMATION_REQUIRED` | Solicitar confirmação da operação e reenviar somente após a ação do usuário. |
| **410** | Arquivo temporário indisponível, quando indicado pelo serviço de exportação | Atualizar a tarefa; uma nova geração é uma nova solicitação explícita. |
| **413 / 415** | Tamanho ou formato de arquivo não aceito | Corrigir o arquivo de entrada; respeitar os limites do ambiente. |
| **429** | Restrição de tentativas ou limite aplicável | Apresentar a mensagem e a espera informada; não disparar novos comandos em laço. |
| **5xx ou perda de conexão** | Falha técnica ou resposta não recebida | Não concluir que houve commit nem que houve rollback. Consultar o resultado; nos fluxos com chave, reenviar a mesma intenção com a mesma chave conforme o item 8. |

Os códigos adicionais existentes incluem `DATA_CONFLICT`, `INVALID_REQUEST_BODY`, `FREQUENCIA_DUPLICADA`, `PASSWORD_CONFIRMATION_REQUIRED` e `INVALID_CONFIRMATION_PASSWORD`. Nem todo 409 possui `code`; não classifique regras exclusivamente pelo texto da mensagem.

**Fontes:** `src/main/java/com/example/siggtrat/configuracao/TratadorGlobalExcecoes.java`; `src/main/java/com/example/siggtrat/seguranca/FiltroAutenticacaoJwt.java`; `src/main/java/com/example/siggtrat/servico/RelatorioExportacaoAssincronaServico.java`.

## 3. Modelo temporal e apresentação de desconhecimento

### 3.1. Conceitos que devem permanecer separados

| Conceito | Identidade e finalidade |
|---|---|
| Pessoa | Identidade permanente; CPF globalmente único e dados opcionais. |
| Vínculo Contratual | Relação da Pessoa com o Contrato em um intervalo; matrícula pertence ao vínculo. |
| Alocação | Faixa diária de Lotação + Jornada/Turno + Cargo dentro do vínculo. |
| Empregado | Projeção operacional de leitura usada por telas e cálculos; não é um segundo cadastro independente da Pessoa. |
| Contrato | Base operacional, com composição, vigência, fatos publicados, habilitação administrativa e pendências. |

O início de vínculo/alocação e a data efetiva de um instrumento são o primeiro dia da nova situação. `dataFim` de desligamento é o último dia do vínculo. Uma mudança de alocação não deve ser apresentada como desligamento e recontratação. Jornada que cruza a meia-noite usa a data de início da jornada.

Preserve datas civis `YYYY-MM-DD` como datas civis nos formulários. Instantes como `registradoEm` e `conhecidoAte` têm outra finalidade e usam ISO-8601 com fuso/offset. A data corrente negocial do backend é calculada em `America/Sao_Paulo`; converter uma data civil para meia-noite UTC pode deslocar sua exibição no navegador.

**Base normativa literal:** TMP-005: “A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato.” TMP-007 define granularidade diária. TMP-013 define o primeiro/último dia. **Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, TMP-001 a TMP-007, TMP-013 e ALO-006; `src/main/java/com/example/siggtrat/utilitario/RelogioSistema.java`; `src/main/java/com/example/siggtrat/constante/ConstantesDominio.java`.

### 3.2. Estado do contrato tem eixos independentes

`ContratoDTO` e a posição temporal separam:

| Campo/eixo | Valores ou significado |
|---|---|
| `estadoNegocial` | `RASCUNHO`, `PUBLICADO`, `CANCELADO`, `ANULADO` |
| `situacaoTemporal` | `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO`, `ANULADO` na data consultada |
| `habilitacaoAdministrativa` | `ATIVO` ou `INATIVO` |
| Pendências | Tarefas e escopos de tratamento; não são um novo valor de `estadoNegocial` |

Um contrato pode estar temporalmente vigente e administrativamente inativo. Não determine a habilitação operacional apenas por comparação de `dataFim` com a data do navegador. Para a data solicitada use a posição reconstruída, incluindo `vigenciaInicio`, `vigenciaFim` e os eixos devolvidos pelo backend.

Vínculo usa `PROGRAMADO`, enquanto Alocação usa `PROGRAMADA`; Alocação também pode apresentar `SEM_ALOCACAO`. Use os enums do recurso correspondente.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, CON-007, CON-022 e TMP-017; `src/main/java/com/example/siggtrat/dto/ContratoDTO.java`; `src/main/java/com/example/siggtrat/dto/PosicaoTemporalContratoDTO.java`; `src/main/java/com/example/siggtrat/entidade/EstadoNegocialContrato.java`; `src/main/java/com/example/siggtrat/entidade/SituacaoTemporalContrato.java`; `src/main/java/com/example/siggtrat/entidade/HabilitacaoAdministrativaContrato.java`; `src/main/java/com/example/siggtrat/dominio/vinculo/SituacaoTemporalVinculo.java`; `src/main/java/com/example/siggtrat/dominio/alocacao/SituacaoTemporalAlocacao.java`.

### 3.3. Posições históricas e campos nulos

| Consulta canônica | Cortes |
|---|---|
| `GET /api/contratos/{id}/posicao-temporal` | `data` obrigatória; `conhecidoAte` opcional |
| `GET /api/contratos/{id}/eventos`, `/eventos/precedencias` | Trilhas correspondentes do contrato; conferir a assinatura do método para filtros |
| `GET /api/contratos/{id}/versoes` | Acervo de fotografias persistidas; `conhecidoAte` opcional limita versões conhecidas |
| `GET /api/contratos/{id}/composicoes?data=...` | Posição na data; `conhecidoAte` opcional limita o conhecimento no mesmo replay |
| `GET /api/vinculos-contratuais/pessoas/{pessoaId}/posicao` | `data` obrigatória; `conhecidoAte` opcional |
| `GET /api/vinculos-contratuais/{id}/alocacoes/posicao` | `data` obrigatória; `conhecidoAte` opcional |
| `GET /api/catalogos-temporais/{tipo}/{id}/disponibilidade` | `data` obrigatória; `conhecidoAte` opcional |

`PosicaoPessoaDTO` separa `nomeAtual`/`cpfAtual` de `nomeFotografia`, `eventoIdentidadeFonteId` e `fotografiaIdentidadeComprovada`. O nome do cadastro atual não deve preencher uma fotografia histórica ausente. Não há campo de CPF histórico adicional nessa resposta.

`DisponibilidadeCatalogoDTO` traz `Boolean ativo` e `disponibilidadeComprovada`. `ativo: null` não significa ativo nem inativo. `VersaoContratoNegocialDTO` possui políticas de matrícula anuláveis e `tipoContratacaoComprovado`: ausência de comprovação não vira `false` por coerção de JavaScript. Para políticas de negócio, mantenha a distinção entre valor explícito, herança quando definida pelo DTO e desconhecimento histórico.

Nomes históricos de Lotação/Turno/Cargo e identificadores de alocação podem ser nulos. IDs conhecidos não certificam rótulos, município ou grade de época. A leitura de Composição acrescenta `lotacaoMunicipio` e `jornadaSemanal`; os requests de composição e a posição de Alocação conservam seus próprios contratos. Não invente campos de request para completar a fotografia.

Na apresentação, use indicação como “Não comprovado para este período” quando houver flag/fonte que sustente essa interpretação. Um `null` por minimização de perfil deve ser tratado como conteúdo não disponibilizado ao leitor, conforme o item 9. Campo opcional vazio também é uma situação distinta. Não use uma única regra `valor || cadastroAtual.valor` para esses casos.

**Base normativa literal:** HIS-004: “Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo.” HIS-005: “História é a fonte canônica; projeções atuais são conveniência técnica.” **Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, TMP-004, CAT-004, HIS-004 e HIS-005; `src/main/java/com/example/siggtrat/dto/PosicaoPessoaDTO.java`; `src/main/java/com/example/siggtrat/dto/PosicaoAlocacaoTemporalDTO.java`; `src/main/java/com/example/siggtrat/dto/DisponibilidadeCatalogoDTO.java`; `src/main/java/com/example/siggtrat/dto/VersaoContratoNegocialDTO.java`; `src/main/java/com/example/siggtrat/controlador/ComposicaoContratualControlador.java`.

#### IND-08 — comprovação por campo

As leituras de posição contratual, versões, provas históricas e composições mantêm os escalares JSON e acrescentam `provaCampos` no objeto que contém os valores. Na posição, os dados contratuais usam `versaoContratual.provaCampos`; os eixos/vigência possuem também um mapa na raiz. Cada composição informa `dataReferencia` e `conhecidoAte` efetivamente usados.

```json
{
  "matriculaTipoContratacaoObrigatoria": false,
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
    }
  }
}
```

IDs do exemplo são ilustrativos. `VALOR_COMPROVADO` inclui zero e falso; `AUSENCIA_COMPROVADA` exige fonte explícita de ausência; `NAO_COMPROVADO` indica lacuna. `visibilidade` é independente: conteúdo `RESTRITA` possui estado factual nulo, origens vazias e escalar nulo/omitido. Verifique a visibilidade antes de apresentar o valor. Uma resposta antiga sem o mapa não autoriza inferir ausência comprovada de um escalar nulo.

`origens` pode ter vários registros, por exemplo mapas convergentes concordantes. Os tipos são `FOTOGRAFIA_CONTRATUAL`, `PROVA_HISTORICA`, `EVENTO_CONTRATUAL`, `COMPOSICAO_PUBLICADA`, `ITEM_ADITAMENTO` e `MAPEAMENTO_TRANSFERENCIA`; as referências pertinentes são `provaId`, `eventoId`, `versaoId`, `composicaoId`, `aditamentoComposicaoId` e `mapeamentoId`, além das datas preservadas. Não há ID físico criado para a projeção: `id: null` continua válido na versão/composição projetada.

A listagem e o recibo de `/api/contratos/{id}/provas-historicas` acrescentam `camposComprovados` **daquela revisão**, `fotografia` com evidência acumulada somente até ela e `composicoes` com seus fragmentos declarados. Cada fragmento possui uma origem canônica de composição, mapa ou item ADI, seus rótulos/município/semana, flags `municipioComprovado`/`jornadaComprovada` e `provaCampos`. O fragmento não herda propriedades de outra revisão; a posição efetiva pode herdá-las, conservando a origem correta.

O instante de incorporação continua sendo `registradoEm` da prova, em microssegundos, e não a data efetiva, a data do documento ou o horário da consulta. Preserve o ISO exato para K; a exibição negocial usa `America/Sao_Paulo`. A leitura de versões antigas não incorpora retrospectivamente prova posterior. Uma prova parcial de matrícula não certifica ausência de dia reduzido; uma prova de grade não certifica município; conhecer um ID não certifica seu nome histórico.

O fim herdado por uma nova combinação de ADI vem da fotografia original conhecida em K e dos deltas de vigência já aplicáveis na ordem do replay. A origem identifica a versão ou o evento que comprova esse limite; mudanças na projeção atual do contrato não alteram o mesmo recorte D/K. O fim fotografado de uma composição-base permanece preservado: ele não representa um encerramento independente da composição quando o mesmo contrato é prorrogado. A validade operacional segue a vigência contratual reconstruída.

Uma fotografia materializada de ADI, transferência ou correção conserva a prova dos campos no D e no instante de registro daquela fotografia. Materializar um `false` comprovado não certifica ausência de outro campo nulo. Uma prova incorporada depois pode mudar a posição em K posterior, mas não a interpretação da versão anterior do acervo. Valores legados que divergem das fontes daquele recorte permanecem nos escalares preservados, com metadata `NAO_COMPROVADO`.

A fotografia original íntegra tem vínculo persistido ao evento `PUBLICACAO`, conforme HIS-006 e o fluxo canônico de publicação. Nesse fluxo, `MotorTemporalContratoServico.registrarFotografia` captura conjuntamente políticas de matrícula e configuração do dia reduzido; o marcador legado de matrícula efetiva só permite interpretar ausências opcionais nessa fotografia original vinculada. Não se aplica a fotos derivadas nem a valores preenchidos isoladamente. `numeroVersao: 1` sem vínculo de origem não autentica os campos: os escalares continuam consultáveis, com comprovação desconhecida. Um evento explícito de publicação disponível no recorte ainda pode comprovar `estadoNegocial`, mesmo se a referência da fotografia se perdeu; a prova do estado não certifica automaticamente seus demais campos.

**Implementação:** DTOs exclusivos `VersaoContratoLeituraDTO`, `PosicaoTemporalContratoLeituraDTO`, `ComposicaoContratualLeituraDTO` e `FragmentoProvaHistoricaComposicaoDTO`; `ConsultaProvaHistoricaContratoServico`; replays em `AditamentoContratualServico` e `ComposicaoContratualTemporalServico`. O DTO de fotografia usado no request e no hash permanece inalterado. Relatórios, emissões e exportações conservam seus contratos específicos; não presumir que também receberam `provaCampos`.

## 4. Prévia, confirmação e precedência

### 4.1. Protocolo comum

A tela prepara uma intenção completa, consulta a prévia, apresenta os impactos e só confirma depois da decisão do usuário. A confirmação usa os mesmos dados relevantes e o token recebido. Editar datas, quantidades, destino, fonte, justificativa, precedência ou outras partes assinadas invalida a prévia local; obtenha outra prévia antes de confirmar.

O token não é uma reserva de capacidade, uma concessão de permissão, uma chave universal de idempotência ou autorização para ignorar conflitos. O backend relê o estado no commit. Se houver 409 por impacto alterado, a tela deve atualizar a prévia e exigir nova decisão. Não confirmar novamente em segundo plano usando um token recém-obtido sem mostrar os novos impactos.

As confirmações específicas são independentes. `confirmarImpacto` não substitui `confirmarInclusaoExcedente`, `confirmarPrecedenciaUltimoDia` ou a concessão excepcional exigida pelo ato.

**Base normativa literal:** AUD-004: “Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação.” **Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, seção “Revalidação da prévia de impacto”; `src/main/java/com/example/siggtrat/servico/PreviaCicloVidaContratoServico.java`; `src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java`; `src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java`.

### 4.2. Pares de endpoints e nomes de token

Todos os caminhos abaixo são POST; `{c}` identifica o contrato e `{v}` o vínculo.

| Fluxo | Prévia | Confirmação | Campo recebido → campo enviado |
|---|---|---|---|
| Publicação original | `/api/contratos/{c}/previas/publicar` | `/api/contratos/{c}/publicar` | `tokenPrevia` → `tokenPrevia` |
| Ciclo de vida | `/api/contratos/{c}/previas/{operacao}` | `/api/contratos/{c}/{operacao}` | `tokenPrevia` → `tokenPrevia` |
| Aditamento | `/api/contratos/{c}/aditamentos/previa` | `/api/contratos/{c}/aditamentos` | `tokenPrevia` → `tokenPrevia` |
| Remanejamento | `/api/contratos/{c}/remanejamentos/previa` | `/api/contratos/{c}/remanejamentos` | `tokenPrevia` → `tokenPrevia` |
| Transferência de Gestão | `/api/contratos/{c}/transferencias-gestao/previa` | `/api/contratos/{c}/transferir-gestao` | `tokenPrevia` → `tokenPrevia` |
| Retificação do início do vínculo | `/api/vinculos-contratuais/{v}/previas/retificacao-inicio` | `/api/vinculos-contratuais/{v}/retificar-inicio` | **`token` → `tokenPrevia`** |
| Retificação do desligamento | `/api/vinculos-contratuais/{v}/previas/retificacao-desligamento` | `/api/vinculos-contratuais/{v}/retificar-desligamento` | **`token` → `tokenPrevia`** |
| Alteração de Alocação | `/api/vinculos-contratuais/{v}/previas/alocacoes` | `/api/vinculos-contratuais/{v}/alocacoes` | **`token` → `tokenPrevia`** |
| Disponibilidade de catálogo | `/api/catalogos-temporais/{tipo}/{id}/transicoes/previa` | `/api/catalogos-temporais/{tipo}/{id}/transicoes` | `tokenPrevia` → `tokenPrevia`; `version` consultada → `versaoEsperada` |

Em ciclo de vida, as operações incluem `inativar`, `reativar`, `encerrar-antecipadamente`, `cancelar-futuro`, `anular` e `corrigir-anulacao`. A publicação tem seu DTO próprio. Transferência deve usar a prévia especializada, pois a intenção inclui mapa e destino; não substituir por uma prévia genérica sem esses dados.

Na implementação atual, a publicação exige confirmação de impacto quando o original é retroativo. A transferência inicial admite registro sem o token adicional; a retificação de transferência com alcance atual ou passado exige a confirmação especializada. A interface pode apresentar a prévia especializada também na preparação inicial, mantendo os mesmos dados na confirmação. As transições de catálogo são até hoje; o token é obrigatório no registro retroativo. A tabela não transforma toda operação atual/futura em uma exigência nova do servidor.

`PreviaCicloVidaContratoDTO` informa contagens totais e limites de detalhamento: `limiteDetalhes`, `vinculosTruncados`, `programacoesTruncadas` e `datasComFrequenciasTruncadas`. Não trate o tamanho de um array truncado como o total de pessoas ou datas atingidas. Apresente o total e avise que a lista é parcial.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/ContratoControlador.java`; `src/main/java/com/example/siggtrat/controlador/AditamentoContratualControlador.java`; `src/main/java/com/example/siggtrat/controlador/RemanejamentoContratualControlador.java`; `src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java`; `src/main/java/com/example/siggtrat/controlador/DisponibilidadeCatalogoControlador.java`; `src/main/java/com/example/siggtrat/dto/PreviaCicloVidaContratoDTO.java`; `src/main/java/com/example/siggtrat/dto/PreviaImpactoVinculoDTO.java`; `src/main/java/com/example/siggtrat/dto/PreviaImpactoAlocacaoDTO.java`; `src/main/java/com/example/siggtrat/servico/TransferenciaGestaoContratoServico.java`.

### 4.3. Retificação de instrumentos e precedência

Para retificar ADI/REM, consulte o instrumento vigente e seu `eventoId`. Gere a prévia na rota usual do instrumento, com sua identificação, `eventoPrecedenteId` apontando ao evento original e `relacaoPrecedencia: "RETIFICA"`. Use a mesma ordem documental efetiva: se a confirmação omitir `ordemDocumental`, o serviço reaproveita a ordem do original; a intenção da prévia precisa coincidir com essa normalização. Confirme em `/{aditamentoId}/retificar` ou `/{remanejamentoId}/retificar`. O backend preserva o original e cria a nova versão.

Os valores aceitos por `TipoRelacaoPrecedenciaNegocial` são `SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI` e `MANTEM_COEXISTE`. `eventoPrecedenteId` e `relacaoPrecedencia` devem ser informados em conjunto quando utilizados. A redação normativa “MANTÉM/COEXISTE” corresponde a `MANTEM_COEXISTE` no JSON. Ordem de ID, ordem de chegada e timestamp não substituem a precedência documental.

Um evento posterior incompatível pode gerar pendência; outros conflitos impedem a operação com 409. Após um comando confirmado, atualize a posição, a trilha e as pendências. Não remova da interface o evento anterior como se tivesse sido apagado.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, TMP-009 a TMP-016, ADI-007/008/009 e AUD-002; `src/main/java/com/example/siggtrat/entidade/TipoRelacaoPrecedenciaNegocial.java`; `src/main/java/com/example/siggtrat/dto/AditamentoContratualDTO.java`; `src/main/java/com/example/siggtrat/dto/RemanejamentoContratualDTO.java`; métodos `retificar` em `src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java` e `src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java`.

## 5. Contratos, composição e instrumentos

### 5.1. Rascunho e publicação

`POST /api/contratos` retorna 201 com `ContratoDTO`. Rascunho pode estar incompleto. `PUT /api/contratos/{id}` edita o rascunho; depois da publicação, alterações negociais seguem os comandos específicos. `DELETE /api/contratos/{id}` descarta somente rascunho elegível, retorna 204 e pode recusar dependências. Publicado não deve receber botão de exclusão física.

Na publicação são exigidos identificação (`numeroContrato`, `anoContrato` inteiro), descrição, Gestão/Tipo coerentes, vigência completa, empresa (`cnpjEmpresa`, `razaoSocialEmpresa`, `nomeFantasiaEmpresa`), efetivo e composição consistente. O original precisa de ao menos uma fonte comprobatória, anexo e/ou SEI.

As composições em `/api/contratos/{c}/composicoes` usam `lotacaoId`, `jornadaId`, `cargoId` e `efetivoPrevisto`. No original, as faixas devem cobrir a vigência contratual e sua soma deve coincidir com o total. A publicação captura os valores documentados nesse ato. Alterar o mestre depois não autoriza a tela a reescrever o que foi publicado.

Exemplo de corpo de publicação, com dados do formulário já conferidos:

```json
{
  "dataPublicacao": "2026-10-30",
  "referenciaSei": "REFERENCIA-DOCUMENTAL-DO-ATO",
  "justificativa": "Descrição do ato conferido pelo responsável"
}
```

Para o original retroativo, envie esse corpo à prévia; depois acrescente `tokenPrevia` recebido e `confirmarImpacto: true` ao comando de publicação. A data efetiva do original decorre de `dataInicio` do contrato; não substitua a data inicial real pela data de digitação.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, GOV-003/004, CON-008/014, CMP e DOC-001; `src/main/java/com/example/siggtrat/dto/ContratoDTO.java`; `src/main/java/com/example/siggtrat/dto/PublicacaoContratoDTO.java`; `src/main/java/com/example/siggtrat/dto/ComposicaoContratualDTO.java`; `src/main/java/com/example/siggtrat/servico/ContratoServico.java`, métodos `publicar`, `validarContratoParaPublicacao`, `validarComposicoesParaPublicacao`; `src/main/java/com/example/siggtrat/entidade/ComposicaoContratual.java`.

### 5.2. Aditamento e remanejamento

| Instrumento | Dados centrais do request | Resultado que a tela deve apresentar |
|---|---|---|
| ADI | `identificacao`, `dataDocumento`, `dataEfetiva`, `justificativa`, fonte; alterações não quantitativas pertinentes; `alteracoesComposicao[].quantidadeResultante` | Valores anterior/resultante e variação calculados pelo backend, efeito na vigência/políticas, piso, ocupação e alertas |
| REM | `identificacao`, `dataEfetiva`, `justificativa`, `itens[].variacao`, SEI quando aplicável | Duas ou mais composições existentes, soma das variações zero, total preservado e impactos |

Para uma composição existente, o item pode identificar `composicaoId`; DTOs também têm `lotacaoId`, `jornadaId` e `cargoId` para as combinações admitidas pelo serviço. ADI recebe **quantidade resultante**; REM recebe **variação**. Não troque essas semânticas no formulário. Não envie `variacao` como se fosse campo do item de ADI.

Em ADI, `novoEfetivoTotal`, quando informado, deve coincidir com a composição projetada. A prévia devolve `efetivoOriginalPublicado`, `pisoNormalReducao`, `excedeLimiteNormal25`, `exigeAutorizacaoExcepcional`, `possuiImpactoOcupacao`, `impactosComposicao` e `alertas`. Use o piso inteiro devolvido pelo servidor; não decida a aprovação por arredondamento percentual no navegador. Confirmação de impacto não autoriza reduzir abaixo da ocupação válida.

A exceção ao piso requer `reducaoExcepcional`, `fundamentoExcecao`, documentação e concessão técnica `REDUCAO_CONTRATUAL_EXCEPCIONAL` ao Gestor Local executor. A prévia pode informar a necessidade dessa exceção sem permitir sua confirmação pelo fluxo comum.

ADI também possui `novaMatriculaVinculoObrigatoria`, `restaurarPoliticaMatriculaDoTipo`, `novoDiaSemanaReduzido`, `novasHorasDiaReduzido` e `removerConfiguracaoDiaReduzido`. Utilize os indicadores explícitos de restauração/remoção; não envie zero, vazio ou `false` para representar indiscriminadamente “herdar” ou “desconhecido”. Exiba os minutos de frequência e previsão calculados pelo backend, sem derivá-los no React desses campos contratuais isolados.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, RED-001 a RED-008, ADI-001 a ADI-010 e REM-001 a REM-007; `src/main/java/com/example/siggtrat/dto/AditamentoContratualRequisicao.java`; `src/main/java/com/example/siggtrat/dto/AditamentoComposicaoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/RemanejamentoContratualRequisicao.java`; `src/main/java/com/example/siggtrat/dto/RemanejamentoComposicaoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/PreviaAditamentoContratualDTO.java`; `src/main/java/com/example/siggtrat/dto/PreviaRemanejamentoContratualDTO.java`.

### 5.3. Ciclo de vida, cancelamento e correções

`AcaoCicloVidaContratoDTO` utiliza `dataEfetiva`, `dataAto`, `motivo`, `referenciaSei`, campos de precedência, `tokenPrevia` e `confirmarImpacto`. A obrigatoriedade específica depende do ato: anulação e encerramento têm fonte documental e regras próprias.

| Intenção | Comando canônico | Observação funcional |
|---|---|---|
| Inativar / reativar | `POST /api/contratos/{c}/inativar` ou `/reativar` | Eixo administrativo; exige decisão explícita e prévia correspondente. |
| Encerrar antecipadamente | `POST /api/contratos/{c}/encerrar-antecipadamente` | Altera o fim efetivo e trata vínculos abertos conforme a causa. |
| Cancelar contrato ainda futuro | `POST /api/contratos/{c}/cancelar-futuro` | Prévia própria; `dataEfetiva` do cancelamento deve anteceder o início reconstruído. |
| Anular | `POST /api/contratos/{c}/anular` | Ato invalidatório, preservando história; não equivale ao cancelamento futuro. |
| Corrigir anulação indevida | `POST /api/contratos/{c}/corrigir-anulacao` | Referencia o evento de anulação, relação `CANCELA`, mesma data efetiva e fonte; preserva necessidade de reativação explícita. |
| Cancelar ADI futuro | `POST /api/contratos/{c}/aditamentos/{id}/cancelar` | Recebe ação com motivo e data do ato; não há exclusão física. O alcance é o do instrumento cancelado. |
| Cancelar REM futuro | `POST /api/contratos/{c}/remanejamentos/{id}/cancelar` | Preserva original, cancelamento e fatos posteriores; dependências podem impedir ou gerar tratamento conforme a regra canônica. |
| Corrigir número/ano | `POST /api/contratos/{c}/correcoes-identificador` | Exige permissão excepcional, motivo/fonte/datas e chave. Identidade anterior permanece reservada. |
| Corrigir erro material de CNPJ | `POST /api/contratos/{c}/correcoes-cnpj` | Usa `cnpjCorrigido` e evidência do erro material; troca real de empresa pertence ao ADI. |

Não associe uma ação genérica “Excluir” aos diferentes comandos de cancelamento, encerramento e anulação. Depois de uma confirmação, consulte novamente o estado retornado e a posição temporal. `DELETE` não substitui essas operações.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, CON-010 a CON-025, ADI-006, REM-007 e GOV-003; `src/main/java/com/example/siggtrat/controlador/ContratoControlador.java`; `src/main/java/com/example/siggtrat/controlador/CorrecaoIdentificadorContratoControlador.java`; `src/main/java/com/example/siggtrat/controlador/CorrecaoCnpjContratoControlador.java`; `src/main/java/com/example/siggtrat/dto/AcaoCicloVidaContratoDTO.java`; `src/main/java/com/example/siggtrat/dto/CorrecaoIdentificadorContratoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/CorrecaoCnpjContratoRequisicao.java`.

### 5.4. Transferência excepcional de Gestão

`TransferenciaGestaoContratoRequisicao` contém `dataEfetiva`, `dataAto`, `gestaoLocalDestinoId`, `tipoContratacaoDestinoId`, `motivo`, `referenciaSei`, `autorizacaoAdministrativa` e `mapeamentos`, além de precedência e confirmação quando aplicáveis. O mapa informa a origem e os IDs de Lotação/Turno/Cargo de destino; não basta alterar `gestaoLocalId` de um contrato publicado.

A concessão técnica é específica, vigente e revogável; o executor negocial continua sendo o Gestor Local autorizado. Uma transferência futura não antecipa acesso operacional do destino. Uma transferência retroativa pode mudar quem está autorizado a consultar uma frequência antiga sem reescrever seus campos de fotografia.

Retificação usa o mesmo comando de transferência, declarando o evento precedente e relação de retificação. A prévia especializada precisa conter a mesma intenção e mapa. O cancelamento futuro tem rota própria: `POST /api/contratos/{c}/transferencias-gestao/{transferenciaId}/cancelar`, com `CancelamentoTransferenciaGestaoRequisicao` (`dataEfetiva`, `dataAto`, `motivo`, `referenciaSei`, `autorizacaoAdministrativa`). Não envie relação `CANCELA` para criar uma nova transferência.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, CON-013/023, PER-012 e TMP-008; `src/main/java/com/example/siggtrat/dto/TransferenciaGestaoContratoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/MapeamentoTransferenciaGestaoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/CancelamentoTransferenciaGestaoRequisicao.java`; `src/main/java/com/example/siggtrat/servico/TransferenciaGestaoContratoServico.java`.

## 6. Seleções, catálogos e disponibilidade

### 6.1. Seleção na data do fato

Para cadastrar ou alterar Alocação, use:

`GET /api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao?data=YYYY-MM-DD`

Os filtros opcionais são `lotacaoId` e `jornadaId`. A resposta é uma lista de `OpcaoAlocacaoTemporalDTO`, com IDs/nomes, `efetivoPrevisto`, `ocupacaoAtual`, `ocupacaoAposInclusao`, `excedenteAposInclusao`, `exigeJustificativaExcedente` e `disponivelParaNovaAlocacao`. Mudar a data ou o contrato exige atualizar as opções. Um item que era selecionável em uma consulta não garante a confirmação após outro usuário ocupar a vaga.

`composicaoBaseId` pode ser nulo em uma combinação derivada de ADI. Nesse caso use os IDs de Lotação/Jornada/Cargo oferecidos pela opção nos campos previstos do request; não fabrique um ID de composição persistida.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java`; `src/main/java/com/example/siggtrat/dto/OpcaoAlocacaoTemporalDTO.java`; `src/main/java/com/example/siggtrat/dto/CadastroVinculoContratualRequisicao.java`; `src/main/java/com/example/siggtrat/dto/AlteracaoAlocacaoVinculoRequisicao.java`.

### 6.2. Cadastro atual e consulta temporal

As famílias atuais de cadastro incluem `/api/lotacoes`, `/api/jornadas`, `/api/cargos`, `/api/tipos-contratacao` e `/api/motivos-desligamento`. Jornada contém exatamente os sete dias, com `diaSemana` em `MONDAY` a `SUNDAY`, `diaTrabalho` e intervalos `entrada1`, `saida1`, `entrada2`, `saida2`. Dia de descanso não recebe horários. Os DTOs e formatos de lista variam por família; não use um adaptador genérico que presuma o mesmo status de criação ou a mesma paginação.

Para disponibilidade em uma data, use `/api/catalogos-temporais/{tipo}/{id}/disponibilidade`, com `tipo` entre `LOTACAO`, `JORNADA`, `CARGO`, `TIPO_CONTRATACAO`, `MOTIVO_DESLIGAMENTO`. `disponibilidadeComprovada: false` não autoriza preencher o passado com a flag atual. Catálogo atualmente inativo não deve ser selecionado para um fato futuro sem reativação explícita admissível.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, CAT-004 a CAT-008; `src/main/java/com/example/siggtrat/dominio/catalogo/TipoCatalogoTemporal.java`; `src/main/java/com/example/siggtrat/dto/JornadaDTO.java`; `src/main/java/com/example/siggtrat/dto/JornadaDiaSemanaDTO.java`; `src/main/java/com/example/siggtrat/dto/LotacaoDTO.java`; `src/main/java/com/example/siggtrat/controlador/DisponibilidadeCatalogoControlador.java`.

### 6.3. Transição documentada e versão do catálogo

`TransicaoDisponibilidadeCatalogoRequisicao` exige, inclusive na prévia: `dataEfetiva`, `ativo`, `motivo`, `evidencia`, `autorizacao`, `versaoEsperada` e `chaveIdempotencia`. Os limites atuais são 500 caracteres para motivo, 1.000 para evidência/autorização e 80 para chave. O comando documenta datas até hoje; não agenda transição futura. Para ato retroativo, confirme com o token e `confirmarImpacto: true`.

A prévia informa `versaoAtual`, `ativoAnterior`, `ativoAtualAposRegistro`, impactos e token. O recibo `TransicaoCatalogoDTO` contém `eventoId`, `registradoEm`, `versaoAtual`, `ativoAtualAposRegistro` e `reenvio`. Os valores do recibo pertencem à intenção confirmada; após atos posteriores, reenvio não transforma o recibo antigo em posição atual.

Os cinco DTOs canônicos — `LotacaoDTO`, `JornadaDTO`, `CargoDTO`, `TipoContratacaoDTO` e `MotivoDesligamentoDTO` — expõem `version`. Consulte o item pelo GET da sua família e use esse número em `versaoEsperada`. A consulta de disponibilidade tem outra finalidade e não substitui esse GET. A resposta do PUT de edição deve trazer a versão confirmada; o cliente pode consumir esse campo retornado, sem estimar o próximo número.

A versão é obrigatória já na prévia: `TransicaoDisponibilidadeCatalogoServico.preparar` compara `versaoEsperada` com a versão JPA antes de devolver `versaoAtual`. Valor ausente não é zero. Se a versão ficou obsoleta, trate 409, recarregue o cadastro e refaça a prévia. Isso continua necessário depois da correção dos DTOs.

**Evidência atual desta interface:** `CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT.edicaoExpoeVersaoConfirmadaEPermiteTransicaoSemAdivinhacao(TipoCatalogoTemporal)` passou nas cinco invocações de `regressao_integral_06`, vinculada ao manifesto de compilação deste guia: `LOTACAO`, `JORNADA`, `CARGO`, `TIPO_CONTRATACAO` e `MOTIVO_DESLIGAMENTO`. O teste cria e edita por HTTP MockMvc com MySQL real, relê a versão confirmada em nova transação, exige campo inteiro explícito no GET, verifica prévia e confirmação com versão válida, recusa a anterior sem mutação e compara PUT com GET. O XML arquivado tem SHA-256 `ef52c52af76052c76d17fd87fc2de68c240959a3763124027cf49d6544eb4a0f`, com cinco aprovações e nenhuma falha, erro ou caso ignorado. O ato desse teste é de hoje; essa prova direta de `CAT-007:checkpoint:005` não comprova sozinha todos os casos retroativos, origem histórica ausente, concorrência ou transporte TCP, nem remove o bloqueio documental de CAT-007.

**Fontes:** `src/main/java/com/example/siggtrat/dto/TransicaoDisponibilidadeCatalogoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/PreviaTransicaoCatalogoDTO.java`; `src/main/java/com/example/siggtrat/dto/TransicaoCatalogoDTO.java`; `src/main/java/com/example/siggtrat/dto/CargoDTO.java`; `src/main/java/com/example/siggtrat/dto/TipoContratacaoDTO.java`; `src/main/java/com/example/siggtrat/dto/MotivoDesligamentoDTO.java`; `src/main/java/com/example/siggtrat/dto/LotacaoDTO.java`; `src/main/java/com/example/siggtrat/dto/JornadaDTO.java`; `src/main/java/com/example/siggtrat/servico/TransicaoDisponibilidadeCatalogoServico.java`, métodos `validar` e `preparar`; `src/main/java/com/example/siggtrat/servico/JornadaServico.java`, `src/main/java/com/example/siggtrat/servico/LotacaoServico.java`, `src/main/java/com/example/siggtrat/servico/CargoServico.java` e `src/main/java/com/example/siggtrat/servico/TipoContratacaoServico.java`, métodos `atualizar` e `paraDTO`; `src/test/java/com/example/siggtrat/auditoria20261029/CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT.java`.

## 7. Pessoa, Vínculo, Alocação e frequência

### 7.1. Cadastro e alterações humanas

`POST /api/vinculos-contratuais` cria Pessoa nova ou reutiliza `pessoaId`, registra o Vínculo e sua Alocação inicial na mesma operação. `CadastroVinculoContratualRequisicao` usa `pessoaId` ou `pessoa`, `contratoId`, `dataInicio`, `dataFim` opcional, `matricula`, `composicaoId` ou o recorte de IDs admitido. Em excedente, os campos do cadastro são `confirmarInclusaoExcedente`, `justificativaExcedente`, `evidenciaExcedente` e `autorizacaoExcedente`.

`PUT /api/pessoas/{id}/dados-opcionais` recebe os opcionais e indicadores de limpeza: `rg/limparRg`, `ctpsSerie/limparCtpsSerie`, `nitPisPasep/limparNitPisPasep`, `dataNascimento/limparDataNascimento`, `email/limparEmail` e `endereco/limparEndereco`. Campo omitido mantém o valor atual; limpeza precisa do indicador. Envie o DTO da operação, evitando reenviar dados desnecessários de uma resposta completa.

Nome/CPF usam `POST /api/pessoas/{id}/correcoes-identidade`, com `tipo`, `novoValor`, `motivo`, `evidencia`, `autorizacao` e `impactosEncontrados`. Matrícula usa `POST /api/vinculos-contratuais/{id}/alterar-matricula` e pertence ao vínculo. Não ofereça fusão automática de Pessoas em resposta a conflito de CPF.

Para retificar início, o campo é `novoInicio`. Para desligamento, é `dataFim` junto de `motivoDesligamentoId`, justificativa, evidência e autorização. A prévia de retificação devolve `token`; a confirmação recebe `tokenPrevia`. Confirmação de excedente e precedência no último dia, quando exigidas, são decisões adicionais explícitas.

Mudança de Lotação/Turno/Cargo usa os comandos de Alocação. A retificação de uma faixa utiliza `POST /api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/retificar`; o cancelamento de uma faixa ainda programada usa `.../{alocacaoId}/cancelar-programacao`. Cancelar vínculo programado e cancelar desligamento programado são, respectivamente, `/{id}/cancelar-programacao` e `/{id}/cancelar-desligamento-programado`. A elegibilidade é decidida no servidor.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, COL, VIN, ALO e DES; `src/main/java/com/example/siggtrat/controlador/PessoaControlador.java`; `src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java`; `src/main/java/com/example/siggtrat/dto/AtualizacaoPessoaOpcionalRequisicao.java`; `src/main/java/com/example/siggtrat/dto/CorrecaoIdentidadePessoaRequisicao.java`; `src/main/java/com/example/siggtrat/dto/RetificacaoInicioVinculoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/DesligamentoVinculoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/AlteracaoAlocacaoVinculoRequisicao.java`.

### 7.2. Frequência e suas três classificações

O lançamento canônico usa `POST /api/frequencias` com `vinculoContratualId`, `dataReferencia`, `status` e os campos de horas pertinentes. O servidor resolve Pessoa e Alocação. Não cadastre outro Empregado nem envie uma fotografia montada com o catálogo atual para representar uma data antiga.

O Gestor Local deve informar `justificativaRetificacao` com ao menos cinco caracteres e `chaveIdempotencia` de 8 a 120 caracteres, inclusive no lançamento gerencial inicial. O Operacional segue sua janela e seu escopo; uma intervenção gerencial pode impedir retorno à edição operacional. `POST /api/frequencias/lote` recebe um array de DTOs; não apresente sucesso parcial por dedução do número de itens se a requisição inteira foi recusada.

| Campo | Valores atuais | Uso da tela |
|---|---|---|
| `estadoNegocial` | `ABERTA`, `FECHADA` | Janela normal de edição |
| `validadeOperacional` | `VALIDA`, `SUJEITA_REVISAO`, `INVALIDA_COBERTURA` | Participação na visão operacional oficial |
| `estadoAuditoria` | `ORIGINAL`, `ALTERADA_OPERACIONAL`, `RETIFICADA_GESTOR`, `SUJEITA_REVISAO` | Situação auditável da versão |

Observe também `bloqueadaEdicaoOperacional` e `versaoNegocial`. Uma frequência preservada para auditoria pode estar fechada e inválida para o cálculo corrente. Não apague essa linha nem a inclua em totais recalculados no React como se continuasse válida.

A janela civil conta a data de início do plantão como primeiro dia e fecha **no início de D+N**, N configurável/padrão30, em `America/Sao_Paulo`. Com N=30, um registro de01/01 fecha em31/01 às00:00; inclusão/retificação não reinicia o prazo. A tela usa `prazoEdicao` efetivo e os estados da API, sem recalcular sete dias nem conceder permissão. O campo é uma data/hora civil de São Paulo; o fuso local do navegador não redefine seu significado. Anulação ou perda de cobertura pode produzir `INVALIDA_COBERTURA` e bloqueio de edição sem transformar uma frequência `ABERTA` em `FECHADA`. A frequência já fechada preserva esse estado; os três eixos continuam independentes. Os recibos TCP da candidata histórica não aprovam a execução atual.

`gestaoLocalId`, `lotacaoId`, `jornadaId` e outros campos de fotografia podem continuar representando a prova original. `escopoHistoricoEfetivo` distingue o contexto autorizado após reconstrução. A autorização combina o perfil atual com o escopo histórico: não deduza direito de edição somente pela Gestão atual do contrato.

As consultas canônicas incluem `/api/frequencias?data=...`, `/intervalo?inicio=...&fim=...`, `/mensal?mes=...&ano=...`, `/{id}/eventos` e `/{id}/evidencias-integridade`. Use os valores de cálculo retornados pelo relatório; a indicação manual de horas, horários efetivos, noite, grade e exceções não deve ser reinterpretada por uma soma paralela no cliente.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, FRE-002/003/013 e PER-010; `src/main/java/com/example/siggtrat/dto/FrequenciaDTO.java`; `src/main/java/com/example/siggtrat/controlador/FrequenciaControlador.java`; `src/main/java/com/example/siggtrat/servico/FrequenciaServico.java`, métodos `exigirRetificacaoGerencial`, `validarEdicaoOperacional` e `exigirAutorizacaoNoContexto`; `src/main/java/com/example/siggtrat/servico/PoliticaTemporalFrequenciaServico.java`; `src/main/java/com/example/siggtrat/dominio/frequencia/EstadoNegocialFrequencia.java`; `src/main/java/com/example/siggtrat/dominio/frequencia/ValidadeOperacionalFrequencia.java`; `src/main/java/com/example/siggtrat/dominio/frequencia/EstadoAuditoriaFrequencia.java`; `scripts/validacao/validar_auditorias_tcp.py`, cenário de anulação com frequências aberta e fechada.

## 8. Idempotência, concorrência e respostas perdidas

Mantenha uma chave estável durante a mesma intenção nos fluxos que oferecem `chaveIdempotencia`. Um novo ato recebe outra chave. Reenvio legítimo preserva a mesma intenção, o mesmo responsável e a mesma chave; usar a chave para conteúdo diferente pode retornar 409. `X-Request-Id` serve à correlação de requisições e não é chave negocial de idempotência.

| Operação com campo próprio | Requisito de chave observado |
|---|---|
| Frequência | 8 a 120 caracteres quando fornecida; obrigatória para intervenção gerencial |
| Emissão administrativa | Obrigatória, até 120 caracteres |
| Correção de número/ano ou CNPJ | Obrigatória, até 80 caracteres |
| Transição temporal de catálogo | Obrigatória, até 80 caracteres; recibo com `reenvio` |
| Cadastro/substituição/retificação documental que possui esse campo | Respeitar o DTO da ação; cadastro limita a chave a 96 caracteres |

ADI, REM e outros comandos sem campo `chaveIdempotencia` no DTO não adquirem essa capacidade porque o cliente enviou uma propriedade extra. A configuração Jackson ignora propriedades desconhecidas. A identidade do instrumento, os eventos e as regras de não duplicação continuam sendo tratados pelo servidor.

Para evitar duplicidade acidental, bloqueie o botão enquanto a requisição está pendente e conserve a intenção enviada. Após timeout, consulte a trilha/resultado antes de permitir uma nova gravação equivalente. Em fluxo com chave, o reenvio da mesma intenção pode recuperar o resultado sem novo fato. Não troque a chave apenas porque a resposta demorou.

Um `version` em resposta não é um contrato universal de `If-Match`. Use `versaoEsperada` somente onde o request o define, e use os tokens próprios das prévias. Não estime a próxima versão. Em uma corrida, uma consulta prévia de vaga, identificador ou disponibilidade é informativa; o commit pode recusar a intenção com 409. Atualize os dados e permita revisão da decisão, sem esconder o conflito como sucesso.

**Base normativa literal:** AUD-005: “Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede.” **Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, seção “Prevenção de duplicidade acidental”; `src/main/java/com/example/siggtrat/dto/EmissaoRelatorioRequisicao.java`; `src/main/java/com/example/siggtrat/dto/CorrecaoIdentificadorContratoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/CorrecaoCnpjContratoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/TransicaoDisponibilidadeCatalogoRequisicao.java`; `src/main/java/com/example/siggtrat/servico/TransicaoDisponibilidadeCatalogoServico.java`; `src/main/java/com/example/siggtrat/configuracao/ConfiguracaoJackson.java`.

## 9. Perfis, concessões e minimização

| Perfil | Papel no módulo e apresentação padrão |
|---|---|
| `GESTOR_LOCAL` | Principal escritor negocial no próprio escopo; nível completo de dados quando autorizado ao recurso |
| `OPERACIONAL` | Frequência no contexto e janela autorizados; CPF minimizado, sem acesso geral a comandos contratuais ou documentos pessoais |
| `GESTOR_ESTADUAL` | Consulta/auditoria no estado; dados pessoais reduzidos ao nível permitido, sem escrita negocial local |
| `GESTOR_NACIONAL` | Consulta/auditoria nacional; dados pessoais minimizados, sem herdar escrita negocial |
| `PRESIDENTE` | Consulta institucional nacional, sem dados pessoais no nível padrão; não equiparar suas capacidades às do Gestor Nacional |
| `ADMINISTRADOR` | Administração técnica; não é automaticamente responsável negocial nem leitor irrestrito de conteúdo pessoal |

O frontend pode ocultar ações incompatíveis com o perfil, mas o backend continua decidindo escopo e permissão em cada chamada. Alcance nacional não dá todas as capacidades. Por exemplo, a emissão administrativa possui restrição expressa ao Presidente, embora a consulta de relatórios seja permitida conforme seu escopo.

A política padrão pode devolver CPF mascarado, matrícula reduzida, nome ausente e opcionais/narrativas nulos. A minimização também alcança observações de frequência, `motivo` e `observacaoTratamento` de pendência e `motivoSuperacao` de emissão. Não complete esses campos com cache de outra sessão, resposta de outro perfil ou narrativa obtida em uma rota adjacente. Limpe caches de autorização no logout e na mudança de perfil/escopo; distinga os recortes e o leitor na chave dos dados em memória.

O metadata de prova histórica não concede acesso à sua fonte documental. `referenciaSei`, `motivo`, `autorizacaoAdministrativa`, `documentoComprobatorioId` e `hashFonteArquivo` continuam omitidos nessa leitura; suas entradas em `provaCampos` indicam `visibilidade: RESTRITA`, `estado: null` e `origens: []`. Não tratar essa restrição como desconhecimento factual. Finalidade, permissão e auditoria documental continuam nas rotas próprias do item 10.

Permissões excepcionais têm tipo, beneficiário, escopo, finalidade, justificativa, autorização administrativa e validade. Os tipos atuais são `CORRECAO_IDENTIFICADOR_CONTRATO`, `REDUCAO_CONTRATUAL_EXCEPCIONAL`, `TRANSFERENCIA_GESTAO_LOCAL`, `INCORPORACAO_PROVA_HISTORICA`, `ACESSO_DADOS_PESSOAIS_RESTRITOS` e `INTERVENCAO_NEGOCIAL_TECNICA`.

`POST /api/permissoes-excepcionais` usa `ConcessaoPermissaoExcepcionalRequisicao` e retorna 201. Revogação usa `POST /api/permissoes-excepcionais/{id}/revogar`. A concessão não é um novo perfil; finalidade/escopo/validade continuam sendo reavaliados. As permissões específicas de documento têm rotas e DTOs próprios no item 10.

Modalidade contextual definida em 09/11/2026 (candidata em regressão na campanha atual): enviar `ocorrenciaId` com `tipo=ACESSO_DADOS_PESSOAIS_RESTRITOS`, omitindo `contratoId` e `gestaoLocalId`. Preservar os demais campos obrigatórios; a resposta identifica `ocorrenciaId` e não representa escopo pessoal genérico sobre o contrato. O Administrador atual concede para beneficiário ativo de consulta institucional; todos os dias precisam de contexto histórico comprovado e escopo autorizado. Só o detalhe/anexo da própria ocorrência pode consumir a concessão, com finalidade compatível e validade/revogação reavaliadas. Não usar esse alvo para liberar frequência, outra ocorrência, documentos contratuais ou ações de escrita. Concessões sem `ocorrenciaId` conservam seu contrato genérico separado. Consulte `DOCUMENTACAO/CODEX/CAMPANHA_20261109/CONCESSAO_LEITURA_OCORRENCIA.md` para checks e limites atuais; esta descrição não concede homologação.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, PER-001 a PER-012; `src/main/java/com/example/siggtrat/dominio/acesso/PerfilAcesso.java`; `src/main/java/com/example/siggtrat/servico/MinimizadorDadosPessoaisServico.java`; `src/main/java/com/example/siggtrat/controlador/PermissaoExcepcionalControlador.java`; `src/main/java/com/example/siggtrat/dto/ConcessaoPermissaoExcepcionalRequisicao.java`; `src/main/java/com/example/siggtrat/controlador/RelatorioAdministrativoControlador.java`; `src/main/java/com/example/siggtrat/servico/RelatorioAdministrativoServico.java`.

## 10. Documentos e downloads

### 10.1. Fonte comprobatória e versões

A família é `/api/contratos/{contratoId}/documentos`. Listar, consultar um documento, consultar `/{id}/eventos` e `/{id}/evidencias-integridade` são operações distintas de baixar `/{id}/arquivo`.

O cadastro é multipart: parte **`metadados`**, com JSON de `CadastroDocumentoComprobatorioRequisicao`, e parte **`arquivo`**, quando houver anexo. O JSON contém `tipoFato`, `fatoId`/`fatoChave` conforme o momento do ato, `titulo`, `descricao`, `referenciaSei`, `classificacaoAcesso`, `dataEfetiva` e `chaveIdempotencia`. Ao usar `FormData`, deixe o navegador construir o `Content-Type` e o boundary; não fixe `application/json` no request inteiro.

`classificacaoAcesso` é `NEGOCIAL` ou `PESSOAL_RESTRITO`. `tipoFonte` distingue `ANEXO`, `SEI` e `ANEXO_E_SEI`. Estados documentais incluem `RASCUNHO`, `PUBLICADO`, `SUBSTITUIDO` e `INVALIDO`; `grupoLogicoId`, `versaoDocumento`, `documentoAnteriorId` e `versaoAtual` permitem exibir a cadeia.

| Ação | Rota POST | Corpo |
|---|---|---|
| Corrigir SEI | `/{documentoId}/retificar-sei` | `RetificacaoSeiDocumentoRequisicao` |
| Acrescentar/substituir anexo versionado | `/{documentoId}/substituir` | Multipart `metadados`/`arquivo`; motivo obrigatório |
| Invalidar versão | `/{documentoId}/invalidar` | `AcaoDocumentoComprobatorioRequisicao` |
| Conceder acesso pessoal | `/{documentoId}/permissoes` | `usuarioId`, `finalidade`, `justificativa`, `autorizacaoAdministrativa`, `validoAte` |
| Revogar concessão | `/{documentoId}/permissoes/{permissaoId}/revogar` | `motivo`, `autorizacaoAdministrativa` |

O prefixo da tabela é `/api/contratos/{contratoId}/documentos`. Criar documento retorna 201; conceder/revogar permissão documental retornam 200. A versão publicada errada continua na trilha; substituir ou invalidar não significa apagar os bytes históricos. Exclusão de rascunho documental tem elegibilidade própria.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, DOC-001 a DOC-010; `src/main/java/com/example/siggtrat/controlador/DocumentoComprobatorioControlador.java`; `src/main/java/com/example/siggtrat/dto/CadastroDocumentoComprobatorioRequisicao.java`; `src/main/java/com/example/siggtrat/dto/DocumentoComprobatorioDTO.java`; `src/main/java/com/example/siggtrat/dto/RetificacaoSeiDocumentoRequisicao.java`; `src/main/java/com/example/siggtrat/dto/SubstituicaoDocumentoComprobatorioRequisicao.java`; `src/main/java/com/example/siggtrat/dto/ConcessaoAcessoDocumentoRequisicao.java`.

### 10.2. Download autorizado

`downloadDisponivel` orienta a tela, mas não congela a permissão para uma chamada futura. O GET do arquivo revalida o acesso; para documento pessoal, informe a finalidade adequada no parâmetro `finalidade`. Uma concessão pode ter sido revogada ou expirado desde a listagem. Trate 403/404/409 e os demais erros antes de converter o corpo em Blob.

Um download bem-sucedido é binário, com seu tipo MIME e `Content-Disposition`; não tente acessar `response.data.data` como em JSON. O nome do arquivo deve vir do cabeçalho/DTO e ser tratado como dado. Use `URL.createObjectURL` para o Blob e libere-o após o uso. Não salve uma resposta JSON de erro com extensão PDF/XLSX. O DTO documental oferece `sha256Arquivo` e `tamanhoBytes` para apresentação/conferência; esses campos não ampliam a autorização de leitura.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/DocumentoComprobatorioControlador.java`, método `baixar`; `src/main/java/com/example/siggtrat/servico/DocumentoComprobatorioServico.java`; `src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java`.

## 11. Relatórios, emissões e cálculos assíncronos

### 11.1. Consulta corrente e exportação

`GET /api/relatorios/frequencia-consolidada` usa `inicio`, `fim`, `contratoIds`, `lotacaoId` e `jornadaId`. A versão `/paginada` recebe `page` e `size`. A rota `/exportacao` recebe `formato=XLSX` ou `PDF`; CSV não é um formato desse contrato.

O cliente apresenta `minutosFisicos`, `minutosPagaveis`, previsão, adicional/noturno e os avisos conforme retornados. Não confunda tempo físico com total pagável nem remonte os dados históricos com o catálogo corrente. Absenteísmo possui consultas próprias em `/api/relatorios/absenteismo`, com `mes`, `ano` e filtros definidos no controlador.

Para exportação longa do consolidado, use:

1. `POST /api/relatorios/frequencia-consolidada/exportacao/solicitacao`, com parâmetros de consulta e `formato`; retorno 202.
2. `GET /api/relatorios/exportacoes/{uuid}` para acompanhar `status`, `etapa`, `mensagem`, `processados`, estimativas, `concluido`, `falhou` e `podeBaixar`.
3. Quando permitido, `GET /api/relatorios/exportacoes/{uuid}/arquivo`.

Estados atuais são `NA_FILA`, `PROCESSANDO`, `CONCLUIDO`, `BAIXANDO`, `BAIXADO` e `FALHA`. `concluido` isoladamente não significa que ainda há download disponível; use `podeBaixar`. O backend revalida perfil/escopo atual antes de entregar bytes. Um 403 de leitura/download não consome o arquivo pronto; restaurar um contexto legítimo é uma decisão administrativa, não uma ação automática do React.

Após transferência bem-sucedida, a tarefa fica `BAIXADO` e não entrega o mesmo arquivo novamente. Tarefas de exportação são temporárias; não use seu UUID como identidade de uma emissão administrativa persistida nem presuma que a tarefa sobreviverá ao reinício.

`segundosDecorridos` é calculado novamente a cada consulta, inclusive no estado `BAIXADO`; sua variação isolada não indica mudança de estado negocial. A estimativa `segundosRestantesEstimados` fica nula fora de `PROCESSANDO`. Preserve esses valores como indicadores de acompanhamento, sem usá-los como versão do recurso.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/RelatorioControlador.java`; `src/main/java/com/example/siggtrat/dto/ExportacaoRelatorioStatusDTO.java`; `src/main/java/com/example/siggtrat/servico/RelatorioExportacaoAssincronaServico.java`; `src/main/java/com/example/siggtrat/servico/AutorizacaoExportacaoRelatorioServico.java`.

### 11.2. Emissão formal e corte de conhecimento

As operações administrativas ficam em `/api/relatorios/administrativos`:

| Operação | Rota | Resposta |
|---|---|---|
| Pré-visualizar a verdade corrente | `POST /previa` | `RelatorioAdministrativoDTO`; `metadados.situacao = PREVIA`, sem emissão persistida |
| Emitir versão administrativa | `POST /emissoes` | 201 com `metadados` e `conteudo`; idempotência por chave |
| Consultar versão preservada | `GET /emissoes/{id}` | Conteúdo e metadados autorizados dessa emissão |
| Comparar com a visão corrente | `GET /emissoes/{id}/comparacao-vigente` | Conteúdo histórico/corrente e diferenças indicadas pelo DTO |
| Recuperar emissão conhecida no corte | `POST /emissoes/conhecimento?conhecidoAte=...` | Emissão existente até o instante solicitado; não cria retroativamente uma emissão inexistente |

`EmissaoRelatorioRequisicao` usa `tipo`, `formato`, `inicio`, `fim`, `contratoIds`, `lotacaoId`, `jornadaId` e `chaveIdempotencia`. Os tipos atuais são `FREQUENCIA_CONSOLIDADA` e `ABSENTEISMO`; formatos declarados são `VISUALIZACAO`, `XLSX` e `PDF`. O retorno desses métodos administrativos é JSON `metadados`/`conteudo`, inclusive com um formato declarado. O formato não transforma a rota de emissão em um endpoint binário; os endpoints de exportação acima têm contrato próprio.

`natureza` distingue `ORIGINAL` e `RETIFICADA`; `estado` distingue `VIGENTE` e `SUPERADA`. Preserve essas dimensões e mostre `sujeitoARevisao`, `quantidadePendencias`, `aviso`, `emissaoAnteriorId`, `superadaEm` e causa quando disponíveis. Uma emissão superada conserva o conteúdo/hash original; não substitua silenciosamente seu conteúdo pelo da comparação vigente.

O backend projeta os metadados no corte de conhecimento. Uma superação ocorrida depois do corte não deve ser reinserida pelo React com base em uma consulta atual. `motivoSuperacao` pode ser nulo por minimização, mesmo com causa tipada e data disponíveis. O hash documenta o conteúdo preservado pelo servidor; não presuma que serializar novamente uma projeção minimizada do JSON produzirá o mesmo hash.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, REL, AUD-003 e TMP-005; `src/main/java/com/example/siggtrat/controlador/RelatorioAdministrativoControlador.java`; `src/main/java/com/example/siggtrat/dto/EmissaoRelatorioRequisicao.java`; `src/main/java/com/example/siggtrat/dto/MetadadosEmissaoRelatorioDTO.java`; `src/main/java/com/example/siggtrat/dto/RelatorioAdministrativoDTO.java`; `src/main/java/com/example/siggtrat/servico/RelatorioAdministrativoServico.java`, métodos `conhecidaAte`, `compararComVigente` e `paraMetadados`.

### 11.3. Painel persistido

`GET /api/painel/efetivo` e `GET /api/painel/gestao` são consultas próprias. `POST /api/painel/calculos` aceita `referencia`, `periodoDias` e `forcar`; o cálculo é consultado por `GET /api/painel/calculos/{id}`.

`PainelCalculoDTO` contém `concluido`, `desatualizado`, `mensagem` e `dados`; `dados` pode vir nulo enquanto não houver resultado disponível. Não anuncie um resultado antigo como corrente quando `desatualizado` for verdadeiro. Mantenha a autorização do leitor no polling; alcance territorial igual não transforma o cálculo de outro perfil/usuário em conteúdo acessível. O cache é uma otimização do servidor e não substitui a revalidação de autorização.

**Fontes:** `src/main/java/com/example/siggtrat/controlador/PainelControlador.java`; `src/main/java/com/example/siggtrat/controlador/PainelCalculoControlador.java`; `src/main/java/com/example/siggtrat/dto/PainelCalculoDTO.java`.

## 12. Pendências e retomada operacional explícita

`GET /api/pendencias` aceita filtros de `estado`, `tipo`, `inicio`, `fim`, `contratoIds`, `pagina` e `tamanho`. `GET /api/pendencias/resumo` oferece o resumo correspondente. Use `PendenciaDTO.id` como identidade da tarefa; não deduplique por pessoa/vínculo/data ignorando o ID. Uma ocorrência posterior pode exigir outra tarefa, preservando a anterior resolvida.

O Presidente pode consultar a lista e o resumo, com a minimização e o escopo aplicáveis. Essa capacidade de consulta não permite iniciar tratamento nem confirmar resolução: os dois comandos POST permanecem vedados a esse perfil.

Cada item pode informar origem (`origemEntidade`, `origemId`, `operacaoOrigem`), escopo, pessoa/vínculo/frequência, data efetiva, estado, responsável e `acaoSugerida`. Os estados são `ABERTA`, `EM_TRATAMENTO` e `RESOLVIDA`. Narrativas podem ser minimizadas conforme o item 9.

Os comandos humanos são `POST /api/pendencias/{id}/iniciar-tratamento` e `POST /api/pendencias/{id}/confirmar-resolucao`, com `observacao` opcional de até 1.000 caracteres. Confirmar resolução fecha a tarefa elegível; não é um comando genérico para encerrar vínculo, apagar evento, alterar vigência ou reativar contrato. Se a causa continua presente ou a origem não pode ser comprovada, o backend pode recusar com 409. Uma tarefa de fim antigo comprovadamente superado pode ser tratada sem produzir desligamento artificial.

O GET de pendências pode sincronizar projeções/tarefas, mas não equivale à decisão de encerramento. O tratamento humano de fim natural usa `POST /api/vinculos-contratuais/contratos/{contratoId}/tratar-fim-natural`. A elegibilidade depende da vigência reconstruída e da causa; a tela não deve acionar a rotina ao abrir uma página nem determinar seu alcance apenas pelo `dataFim` materializado em outra resposta.

### Retomada após inativação, prorrogação ou correção

As regras literais que orientam a interface são:

> CON-012: “Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa.”

> VIN-012: “Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita.”

> CON-020: “Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa.”

Depois de uma prorrogação ou correção, releia a posição, a habilitação administrativa, vínculos, alocações e pendências. Uma retomada administrativa é o comando explícito de reativação. Se o vínculo ainda é temporalmente válido e não foi encerrado, a reativação dentro da vigência pode permitir sua operação sem criar outro vínculo; se já foi encerrado, a interface deve oferecer o tratamento permitido e sua prévia, sem reabrir automaticamente. O mesmo vale para a Alocação ainda válida e não sucedida.

Reinício do servidor, atualização de cache ou abertura de uma tela não são atos negociais de reativação ou recontratação.

**Fontes:** `docs/LEVANTAMENTO_REQUISITOS.md`, CON-011/012/020, VIN-008 a VIN-013, ALO-008, AUD-006 e TMP-006; `src/main/java/com/example/siggtrat/controlador/PendenciaAdministrativaControlador.java`; `src/main/java/com/example/siggtrat/dto/PendenciaDTO.java`; `src/main/java/com/example/siggtrat/dto/TratamentoPendenciaRequisicao.java`; `src/main/java/com/example/siggtrat/servico/PendenciaAdministrativaServico.java`, métodos `confirmarResolucao`, `validarOrigemResolvida`, `fimDaPendenciaSuperado` e `paraDto`; `src/main/java/com/example/siggtrat/servico/VinculoContratualServico.java`, método `tratarFimNatural`; `src/main/java/com/example/siggtrat/servico/ContratoServico.java`, métodos `reativarAdministrativamente` e `corrigirAnulacao`.

## 13. Implementação do cliente e limites de validação

### 13.1. Integração recomendada para o React

Mantenha adaptadores separados para resposta JSON envelopada, listas paginadas próprias e downloads. Preserve o corpo da intenção enquanto aguarda a confirmação; desabilite gravação duplicada sem impedir a leitura do impacto. A prévia deve ficar vinculada ao formulário, à data e ao contexto de autorização que a produziram. Alterações de contexto descartam a prévia local.

Use o estado retornado depois do comando e refaça as consultas afetadas. Não faça atualização otimista de vigência, habilitação, encerramento, quantidade ou concessão antes do resultado definitivo. Uma seleção antiga de catálogo/capacidade não certifica o commit. Estados históricos devem permanecer acessíveis na trilha, com rótulos e fonte próprios.

A implementação de leitura pode tratar 204 separadamente, testar o tipo de conteúdo e então extrair `data` do envelope de sucesso. O tratamento de erro precisa preservar `status`, `code`, `message` e `fields` disponíveis. Uma função genérica de repetição automática não deve reenviar todos os POSTs após 401, 409 ou perda de conexão.

As rotas de escrita recomendadas neste documento são os comandos canônicos apresentados. `Empregado` permanece leitura operacional; criação e alterações humanas usam Pessoa/Vínculo/Alocação. Não restaurar um CRUD paralelo para contornar as regras temporais.

### 13.2. O que cada evidência permite concluir

| Evidência | Alcance | Limite |
|---|---|---|
| Leitura de controladores/DTOs/serviços da candidata07 | Identifica rotas, campos, invariantes e pré-condições deste guia; todas as referências de código e roteiros foram conferidas contra os bytes congelados | Leitura estática, por si só, não demonstra HTTP externo nem commit concorrente real |
| `regressao_integral_06` | 1.607 casos aprovados em 342 classes: 493 unitários e 1.114 de integração, sem falhas, erros ou casos ignorados; oráculos associados ao MySQL real no recorte de integração | Os asserts específicos definem a cobertura. MockMvc não equivale a navegador nem ao JAR por TCP |
| `carga_final_01` | 494 casos aprovados: os mesmos 493 unitários e um cenário de carga; 153.613 operações, 30.729 incrementos esperados e persistidos, sem erros observados | Carga JDBC sintética; não prova todos os fluxos HTTP nem escala ilimitada. As duas rodadas somam 2.101 execuções e 1.608 invocações distintas, sem contar os unitários repetidos como cobertura nova |
| Inventário `RequestMappingHandlerMapping` do Spring real | 264 pares de método HTTP e rota, em 40 controladores; arquivo `endpoints-runtime.json` da integral06 e matriz atual | Inventaria a interface. A existência de uma rota ou de uma requisição associada não homologa todas as suas regras |
| `PreparacaoHttpTcpIT` | Prepara e relê a fixture sintética necessária ao JAR | A preparação não substitui a execução externa nem constitui fonte histórica autêntica |
| `validar_jar_tcp.py` e `validar_auditorias_tcp.py`, fases `criar` e `reinicio` | Quatro provas principais aprovadas: **457 requisições e 819 asserções**, com o mesmo JAR antes/depois do reinício e parada final comprovada | Cobertura dos cenários e perfis registrados; não cobre todos os caminhos das 264 rotas |
| Execuções TCP próprias dos três revisores | **95 requisições e 336 asserções** aprovadas: Contratos 28/56, Pessoas e Catálogos 32/119, Transversais 35/161 | Autenticação e consultas independentes sobre os cenários preparados; não devem ser apresentadas como nova execução de escrita negocial, concorrência ou rollback |
| Capturas SQL antes/depois do reinício e publicação revalidada | Preservação de 72 tabelas, 525 linhas, 138 FKs e seis CHECKs, sem órfãos; estrutura e digests vinculados aos artefatos privados | Estado sintético observado sem escritores durante as capturas; não certifica migração de dados reais nem inventaria triggers, rotinas, eventos ou views |

Os controles específicos de configuração/publicação, alocação retroativa, rótulos quantitativos e cancelamento/replay estão descritos pelos testes correspondentes e pelos registros independentes de oráculos. Nomes de testes e totais de casos não homologam todos os critérios. A execução TCP atual inclui a releitura da grade corrente antes do cenário histórico e comprova as pré-condições distintas de frequência aberta e fechada antes da anulação. A tentativa anterior reprovada está preservada em `evidencias/jar_tentativa01_reprovada` e não integra o aceite atual.

Os roteiros TCP revistos não exercitam cancelamento futuro de contrato/ADI/REM/transferência/alocação; anulação e revogação documental não substituem essa cobertura. Também não se deve atribuir ao cenário 480/480 uma prova de município histórico apenas porque o município corrente foi alterado.

O vínculo entre a fonte e o artefato foi conferido em `evidencias/jar/CLASSES_JAR_EXERCITADAS.json`: as 776 classes empacotadas são idênticas às classes da execução integral. A integral06 e a carga01 usam exatamente a mesma fonte compilada da candidata07. A candidata07 alterou apenas o roteiro TCP de auditorias para corrigir sua pré-condição; o JAR permaneceu igual. O gate `evidencias/jar/PROVA_HTTP_REINICIO.json`, SHA-256 `373e2c8a5a68283a9aff9f2a7004a2b8ab1ae1aaea5e3ad95b895d8e77f9db6a`, vincula as quatro provas, os processos, suas janelas, a parada final e as capturas. `evidencias/jar/PUBLICACAO_PERSISTENCIA.json` revalida as projeções distribuíveis contra as capturas privadas.

Os seis E listados no item 1 permanecem bloqueios documentais. A aprovação dos controles técnicos e dos gates de execução não supre nomes, configurações ou fatos de época sem origem comprovada. O guia e as evidências fornecem o contrato de integração e seus limites; não representam implementação, teste em navegador ou homologação do frontend React. Os caminhos de evidência citados neste item são relativos à raiz do pacote de auditoria; o registro documental em `docs/HISTORICO/REVISAO_INTEGRACAO_FRONTEND_CANDIDATA07.json` relaciona seus hashes.

A geração e a validação do pacote distribuível são registradas nos relatórios finais, com hashes próprios de seus utilitários. Revisões documentais e ajustes posteriores do empacotador têm registro separado; não se presume que pertençam aos 1.076 arquivos do manifesto auditável congelado da candidata07. Esse acompanhamento da distribuição não altera a identidade da fonte Java, do JAR ou das provas técnicas citadas aqui.

**Fontes de verificação:** `AGENTS.md`; `src/test/java/com/example/siggtrat/auditoria20261029/InstrumentosConfiguracaoHistoricaHttpIT.java`; `src/test/java/com/example/siggtrat/auditoria20261029/AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT.java`; `src/test/java/com/example/siggtrat/auditoria20261029/InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT.java`; `src/test/java/com/example/siggtrat/auditoria20261029/AuditoriaCancelamentoRemanejamentoReplayHttpIT.java`; `scripts/validacao/PreparacaoHttpTcpIT.java`; `scripts/validacao/validar_jar_tcp.py`; `scripts/validacao/validar_auditorias_tcp.py`.
