# API de Pessoa, Vínculo, Alocação e cadastros auxiliares

Este contrato complementa `CONTRATO_API_REACT_POS_AUDITORIA.md` e descreve as mudanças auditadas em outubro de 2026. Os caminhos e DTOs são os expostos pelos controladores do projeto. O inventário vigente da aplicação e as substituições de rotas estão em `conclusao-198/MATRIZ_ENDPOINTS_CANONICOS.json` e `conclusao-198/INTEGRACAO_REACT_CONTRATOS.md`. As matrizes de auditorias anteriores documentam apenas seus respectivos checkpoints.

## Fontes canônicas e projeções de leitura

`Pessoa` identifica uma pessoa permanente por CPF válido e único globalmente. CPF é persistido com onze dígitos, sem máscara. Nome e CPF só mudam pelo procedimento excepcional auditado. RG, CTPS, NIT/PIS, nascimento, e-mail e endereço são opcionais. Matrícula pertence ao Vínculo Contratual; conta bancária não integra esse cadastro.

`VinculoContratual` registra a relação temporal da Pessoa com um Contrato, incluindo início, último dia, matrícula e causa de término. Não existe sobreposição da mesma Pessoa, inclusive entre contratos e Gestões. O término em um dia permite outro vínculo no dia seguinte. As matrículas utilizadas continuam reservadas naquele contrato após alteração, desligamento ou cancelamento de programação.

`AlocacaoVinculo` registra intervalos diários de Lotação + Jornada/Turno + Cargo. Uma mudança de alocação mantém Pessoa e Vínculo. Toda data operacional do vínculo possui uma única alocação válida. Fotografias de nomes, município e jornada semanal preservam o conteúdo histórico mesmo após mudanças no cadastro atual.

Fotografias históricas ausentes ou vazias são expostas como `null`; os nomes atuais do cadastro não completam esse desconhecimento. Isso vale para o nome do histórico contratual do Empregado, os rótulos de Alocação na listagem e na posição temporal e a descrição/categoria de motivo já aplicado. Uma retificação de limites ou matrícula conserva essa ausência no novo evento, junto dos identificadores conhecidos. Uma restauração por mapa documentado também preserva os rótulos ausentes e a grade/município comprovados. Novas alocações efetivas hoje ou no futuro capturam sua configuração própria no registro. Em uma nova alocação retroativa, nomes, município e grade semanal vêm da composição reconstruída para a data efetiva; selecionar os IDs não documenta os valores atuais para o passado. Recuperar um valor de época nunca registrado depende de prova documental específica.

`Empregado` permanece como projeção operacional de leitura. A escrita por seu antigo CRUD foi removida. A frequência nova recebe `vinculoContratualId` e resolve Pessoa e Alocação no servidor. O cliente não precisa cadastrar outro Empregado para a mesma Pessoa.

Se a projeção de um vínculo já encerrado tiver sido inativada pelo reprocessamento operacional, uma recontratação que já esteja vigente reutiliza o mesmo `Pessoa.id` e `Empregado.id` e recompõe o contexto operacional do novo vínculo. Consulta, filtro ativo, grade e frequência passam a refletir essa relação, respeitando a habilitação administrativa temporal do contrato. A contratação programada não antecipa essa recomposição; os vínculos, faixas e lançamentos anteriores permanecem históricos.

Uma transferência de Gestão já efetiva sincroniza a projeção que representa o contrato afetado com o vínculo e a alocação válidos hoje. Gestão, Lotação, Turno, Cargo e referência de contrato continuam coerentes para gravações posteriores. Se o vínculo já terminou e não há outro vigente, a projeção fica inativa e perde a referência operacional ao contrato transferido, conservando seu contexto histórico. Uma programação futura não antecipa essa mudança nem substitui a projeção de outro contrato ainda vigente.

| Intenção do cliente | Operação vigente |
|---|---|
| Cadastrar Pessoa, Vínculo e Alocação inicial | `POST /api/vinculos-contratuais`, na mesma transação |
| Alterar dados opcionais da Pessoa | `PUT /api/pessoas/{id}/dados-opcionais` |
| Corrigir nome ou CPF | `POST /api/pessoas/{id}/correcoes-identidade`, com motivo, evidência, autorização e impactos |
| Alterar contexto, início ou término | Comandos temporais de Vínculo/Alocação, com prévia quando exigida |
| Consultar projeção operacional | GETs de `/api/empregados`, sem POST/PUT/DELETE |
| Consultar histórico contratual da projeção | `GET /api/empregados/{id}/historico-contratos`, com fotografia do período |

As rotas de Banco, Agência, Conta, Tipo de Conta, Telefone e Endereço isolado foram removidas dos controladores. Endereço opcional pertence ao cadastro de Pessoa; não possui CRUD paralelo. Status de Empregado continua como catálogo operacional. Campos desconhecidos de JSON não constituem comandos de identidade, vínculo, alocação ou cadastro bancário e não são persistidos nesses domínios.

A consulta de Empregado monta o DTO dentro de uma transação de leitura, inclusive com `open-in-view=false`. A atualização opcional bloqueia a Pessoa antes de registrar sua alteração. Um campo omitido conserva o valor confirmado por outro escritor enquanto a requisição aguardava o lock. As alterações geram histórico assinado com antes/depois, autoria, data de registro, data efetiva e escopo canônico; a auditoria técnica é enfileirada somente depois do commit. Falhas de validação revertem Pessoa, Endereço e histórico juntos.

## Sessão e formato de resposta

As requisições usam sessão JWT em cookie e vínculo de dispositivo. POST/PUT/DELETE exigem CSRF válido. Nas operações marcadas com `@ExigirSenha`, a sessão comum exige confirmação pelo cabeçalho `X-User-Password`; uma sessão com 2FA ativo e validado pode substituir essa confirmação, conforme o interceptor de autenticação. O inventário registra quais operações possuem essa exigência. Confirmação ausente retorna 428 (`PASSWORD_CONFIRMATION_REQUIRED`); confirmação incorreta retorna 403, preservando a sessão.

Respostas de sucesso com corpo são envolvidas em `RespostaApi`: `timestamp`, `status`, `error` nulo, `message`, `requestId`, `meta`, `data` e `path`. Coleções paginadas possuem `data.content` e metadados de paginação. Respostas 204 não possuem corpo. O cliente deve usar os campos, sem depender do texto de `message`.

Erro de preenchimento ou referência inválida retorna 400. Perfil não autorizado retorna 403. Um recurso individual fora do escopo normalmente retorna 404. Conflito negocial, prévia desatualizada, sobreposição ou matrícula reservada retorna 409. As listas já vêm filtradas no banco antes da contagem e paginação; não devem ser filtradas somente no navegador.

## Consulta e escopo

| Perfil | Consulta canônica de Pessoa/Vínculo/Alocação | Escrita |
|---|---|---|
| Gestor Local ativo | Gestão sob sua responsabilidade; cadastro pessoal completo quando necessário ao fluxo | Operações negociais da própria Gestão |
| Gestor Estadual | Gestões do seu Estado; dados pessoais minimizados | Sem escrita negocial comum |
| Gestor Nacional | Consulta nacional minimizada | Sem escrita negocial comum |
| Presidente | Consulta nacional sem identificação pessoal desnecessária: nome, CPF e matrícula suprimidos | Sem escrita negocial |
| Administrador | Consulta técnica minimizada; dados restritos exigem concessão específica | Não assume o papel negocial do Gestor Local |
| Operacional | Somente as combinações autorizadas de Lotação + Turno, avaliadas nas faixas canônicas | Frequência autorizada; sem alteração cadastral/negocial |

O recorte operacional é aplicado à coleção de Pessoas, coleção de Vínculos, detalhes, alocações, eventos, posições e opções de alocação. A posição usa a data efetiva solicitada. O endereço atual do Empregado, seu contrato atual ou outra projeção não concedem acesso a uma faixa histórica. Consulta restrita integral exige escopo, finalidade e, para leitores de auditoria/suporte, concessão específica. Operacional não recebe o cadastro integral. A minimização também abrange a matrícula nas posições de vínculos aninhadas e os documentos nos campos de evidência/autorização dos eventos; o conteúdo original permanece preservado para o Gestor Local responsável.

Textos livres podem conter dados pessoais além dos campos documentais. Por isso, `motivo` e `impactosEncontrados` das correções de Pessoa e `justificativa` dos eventos de Vínculo/Alocação são nulos nas consultas minimizadas, assim como evidência e autorização. O Gestor Local responsável conserva a leitura integral. Tipo, categoria, identificadores, datas, responsáveis e hashes continuam visíveis no escopo autorizado; a minimização não modifica a fonte persistida.

A mesma regra vale para `justificativa`, `evidencia` e `autorizacao` aninhadas na consulta de excedentes por composição. Quantitativos, combinação, datas, identificadores e origem do evento permanecem disponíveis no escopo do leitor.

Os GETs administrativos do legado Empregado e Histórico não são abertos ao Operacional ou à Presidência. O Operacional utiliza `/api/empregados/filtrados-frequencia` quando a interface anterior ainda é necessária, com Lotação e Turno obrigatórios e escopados.

## Pessoa: seis operações

| Método e caminho | Entrada | Saída em `data` |
|---|---|---|
| `GET /api/pessoas` | `busca`, `contratoId`, `ativoEm`, `page`, `size` | Página de `PessoaResumoDTO` |
| `GET /api/pessoas/{id}` | ID no escopo | `PessoaDTO` minimizado conforme perfil |
| `GET /api/pessoas/{id}/dados-restritos` | `finalidade` obrigatória; autorização adicional quando aplicável | `PessoaDTO` integral e acesso registrado |
| `PUT /api/pessoas/{id}/dados-opcionais` | Opcionais e indicadores explícitos de limpeza | `PessoaDTO` |
| `POST /api/pessoas/{id}/correcoes-identidade` | Tipo `CPF` ou `NOME`, novo valor, motivo, evidência, autorização e impactos encontrados | `PessoaDTO` atualizado, mesma identidade |
| `GET /api/pessoas/{id}/correcoes-identidade` | ID no escopo | Lista com valor anterior/novo, autoria, registro, motivo e evidências autorizadas |

Na atualização opcional, omitir um campo ou enviá-lo nulo mantém seu valor atual. Para apagar, envie `limparRg`, `limparCtpsSerie`, `limparNitPisPasep`, `limparDataNascimento`, `limparEmail` ou `limparEndereco` como `true`. Um endereço informado exige logradouro. O endereço é normalizado conforme o domínio de Endereço.

Não existe POST isolado de Pessoa. A criação usa o cadastro integrado de Vínculo. Uma correção de CPF não funde pessoas: CPF pertencente a outra Pessoa retorna 409. A projeção que possui associação canônica com a Pessoa acompanha o CPF corrigido; o identificador incorreto anterior fica disponível para seu titular real sem reassociar a projeção de outra Pessoa.

Se o CPF de um novo cadastro coincidir com um Empregado legado sem associação canônica comprovada, o cadastro integrado retorna 409 e reverte toda a tentativa. A coincidência de CPF não autoriza atribuir a esse cadastro a identidade ou a história de outra Pessoa. A identificação/duplicidade deve receber tratamento documental explícito; recontratações cuja associação canônica já está estabelecida continuam reutilizando a mesma Pessoa e projeção.

## Vínculo e filtros de período

`GET /api/vinculos-contratuais` aceita `contratoId`, `pessoaId`, `estado`, `inicioDe`, `inicioAte`, `fimDe`, `fimAte`, `ativoEm`, `busca`, `page` e `size`.

- `inicioDe`/`inicioAte` limitam o **início efetivo** do vínculo, de forma inclusiva.
- `fimDe`/`fimAte` limitam o **último dia efetivo** do vínculo. Vínculos abertos não aparecem nesse filtro.
- `ativoEm` consulta cobertura na data informada.
- Uma mudança interna de alocação não é novo início nem desligamento.
- Intervalos invertidos retornam 400. A ordenação é fixa para paginação estável.

`POST /api/vinculos-contratuais` aceita `pessoaId` existente **ou** `pessoa` nova, nunca ambos. Os demais campos são `contratoId`, `dataInicio`, `dataFim` opcional, `matricula` opcional conforme contrato/tipo e a composição ou combinação de Lotação + Jornada + Cargo. A operação retorna 201. Pessoa, Vínculo, Alocação inicial, reserva de matrícula, projeção e evento são confirmados na mesma transação. Falha posterior à inserção da Pessoa reverte toda a operação.

A obrigatoriedade de matrícula usa a política contratual efetiva na data informada, fotografada na publicação e alterável por aditamento documentado. Editar o Tipo de Contratação atual não reescreve a política já publicada. Retificar o início também revalida a matrícula conhecida para o novo início; as reservas históricas permanecem vinculadas aos seus vínculos de origem.

Se o acervo legado não comprova a política histórica, omitir a matrícula retorna 409 até que a condição seja tratada. Informar uma matrícula válida e não reservada continua permitido; não se usa o Tipo mestre atual para inventar uma política anterior.

O contrato precisa estar publicado e administrativamente ativo na data efetiva de início. Uma inativação programada já conhecida bloqueia inclusão dentro de seu intervalo mesmo que a projeção atual ainda esteja ativa. Capacidade zero bloqueia inclusão. A composição precisa permanecer válida durante toda a faixa: um zeramento futuro já registrado impede criar um vínculo aberto que o atravesse, enquanto um vínculo que termine antes do zeramento permanece admissível. A mesma validação protege alterações, retificações e cancelamentos de alocação. Excedente exige `confirmarInclusaoExcedente`, `justificativaExcedente`, `evidenciaExcedente` e `autorizacaoExcedente`; a evidência fica completa no evento desde a primeira inserção.

A listagem de opções em `/api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao?data=AAAA-MM-DD` retorna composição, IDs e nomes da combinação, efetivo previsto, ocupação, ocupação após inclusão, excedente e necessidade de confirmação específica. Aceita filtros opcionais de Lotação e Jornada.

## Prévia e confirmação

Retificação de início, retificação de desligamento e alteração/retificação de alocação possuem prévia. O cliente envia o payload proposto e recebe `token`, alvo, datas e impactos/incompatibilidades. Na confirmação, reenvia os mesmos dados com `confirmarImpacto=true` e `tokenPrevia`.

A prévia de alocação considera a faixa inteira proposta. Se uma composição for zerada em uma data futura dentro dessa faixa, a resposta aponta a incompatibilidade; a confirmação rejeita a operação sem alterar as faixas existentes.

A retificação do início aceita `novoInicio`, `justificativa`, `evidencia`, `autorizacao`, `confirmarImpacto`, `tokenPrevia` e `confirmarInclusaoExcedente`. Quando a antecipação acrescenta dias com ocupação acima do previsto, a prévia informa o excedente e a gravação exige `confirmarInclusaoExcedente=true`. Omitir esse indicador equivale a não consentir. A capacidade de todas as combinações restauradas integra a assinatura da prévia, tanto na antecipação do início como na ampliação do término.

Quando mais de uma faixa restaurada produz excedente, cada faixa recebe a fotografia da autorização aplicável, registrada a partir do primeiro dia de excedente do próprio trecho. A consulta `/api/contratos/{contratoId}/composicoes/excedentes?data=AAAA-MM-DD` apresenta essas justificativas por combinação e data, inclusive quando a data consultada antecede o novo término do vínculo.

| Operação | Prévia | Confirmação |
|---|---|---|
| Início | `POST /{id}/previas/retificacao-inicio` | `POST /{id}/retificar-inicio` |
| Desligamento | `POST /{id}/previas/retificacao-desligamento` | `POST /{id}/retificar-desligamento` |
| Alocação | `POST /{id}/previas/alocacoes` | `POST /{id}/alocacoes` ou `POST /{id}/alocacoes/{alocacaoId}/retificar` |

Os caminhos da tabela têm prefixo `/api/vinculos-contratuais`. A assinatura inclui payload negocial e dependências relevantes: versões/estado do contrato e composição, vínculo, faixas/eventos de alocação, frequências afetadas, pendências e emissões. Na retificação de desligamento, a prévia também valida o motivo no escopo e na data efetiva; a versão desse motivo integra a assinatura. A confirmação revalida o estado sob os locks da operação, com isolamento `READ_COMMITTED`. Alteração do estado ou do payload devolve 409 e exige nova prévia. Os indicadores de consentimento são enviados na confirmação; não substituem justificativa, evidência e autorização.

A assinatura percorre todas as dependências em páginas escalares de até 200 registros, ordenadas por ID. Um digest incremental e a contagem resumem cada categoria, mantendo o resultado compacto inclusive com muitos vínculos, frequências ou emissões. A versão `dependencias-v2` invalida naturalmente os tokens emitidos pelo formato anterior; basta obter uma nova prévia.

Se a retificação retirar cobertura de uma frequência já registrada, o conteúdo original é preservado. O sistema registra o impacto, marca revisão/invalidade operacional, bloqueia edição operacional e gera pendência de tratamento. A frequência não é apagada nem transferida silenciosamente para outro vínculo/alocação.

## Datas, cancelamentos e fotografias

Início e nova alocação usam a data como primeiro dia da nova situação. Desligamento usa a data como último dia do vínculo; a inatividade começa no seguinte. Mudança de alocação nesse último dia exige `confirmarPrecedenciaUltimoDia=true`: a nova alocação vale durante o último dia e o desligamento produz efeito depois dele.

Essa precedência também é exigida quando a retificação restaura uma sucessora histórica, ou aplica um mapeamento de transferência já documentado, exatamente no novo último dia.

Programação de vínculo pode ser cancelada antes de começar por `/{id}/cancelar-programacao`. Alocação futura usa `/{id}/alocacoes/{alocacaoId}/cancelar-programacao`. Desligamento futuro usa `/{id}/cancelar-desligamento-programado`. Os três preservam a cadeia original e exigem justificativa, evidência e autorização. Cancelamento de programação de vínculo não é desligamento de um vínculo efetivamente ocorrido.

Depois do cancelamento da programação, `/{id}/desligar` retorna 409 sem modificar fim, motivo ou cadeia. A mesma Pessoa pode receber uma nova programação por cadastro integrado, com matrícula não reservada; correções históricas continuam nos comandos explícitos de retificação.

Cancelar uma alocação futura restaura a faixa anterior somente depois de validar a combinação e a ocupação de todo o período restaurado. Se surgir excedente, a ação exige `confirmarInclusaoExcedente=true`, além dos documentos obrigatórios, e registra essa decisão na trilha. Sem consentimento, retorna 409 e mantém a programação e suas faixas intactas.

Um novo início ou término que inviabilize completamente uma mudança de alocação ainda futura retorna 409 até que essa programação seja cancelada ou retificada explicitamente. As prévias apontam os IDs afetados. O desligamento retroativo legítimo continua disponível: seu corte de cobertura não apaga as decisões históricas de alocação. Uma retificação posterior recompõe as combinações e datas originais que voltam a ser válidas, sem desfazer cancelamentos explícitos de alocação.

Quando a extensão do término alcança uma transferência que ocorreu depois do fim anteriormente registrado, o plano reaplica o mapeamento documental válido daquela transferência. A prévia identifica o destino e revalida as capacidades de todas as faixas. A confirmação cria a sucessora necessária, com referência à transferência, documentos da retificação, autor e instante de registro atuais. Nomes, município e grade semanal do destino vêm da fotografia do mapeamento. Mudanças posteriores de catálogo não substituem esse contexto documentado.

Sem mapeamento comprovado para a origem, ou se um mapa legado não contém a fotografia da grade necessária à reconstrução, a operação retorna 409 para tratamento documental explícito. Não se preenchem fotografias antigas com os cadastros atuais. Município nulo permanece nulo quando a grade fotografada comprova a captura do contexto; a ausência dessa prova impede criar automaticamente uma sucessora histórica que nunca chegou a existir. O bloqueio preserva o acervo existente e não representa reconstrução de um passado desconhecido.

Transferências já efetivas e atos imediatos de inativação/reativação mantêm a projeção operacional coerente com a posição canônica de hoje. A seleção considera o vínculo efetivamente vigente; uma programação futura não substitui o vínculo atual, e um vínculo já encerrado não é reaberto. Na inativação administrativa, os identificadores, limites e eventos de Vínculo/Alocação permanecem intactos. Atualizações opcionais pela rota canônica de Pessoa continuam disponíveis, e a reativação dentro da vigência reutiliza a mesma linha.

A combinação Lotação + Turno + Cargo é resolvida pela composição contratual publicada válida no recorte. Remover hoje um Turno da lista mestre de uma Lotação não invalida essa combinação histórica. Uma combinação nova continua sujeita à validação dos catálogos e da associação no fluxo contratual próprio.

`/{id}/alterar-matricula` registra nova matrícula na data efetiva, sem liberar a anterior. `/{id}/desligar` registra causa de término e autoria. `/contratos/{contratoId}/tratar-fim-natural` aplica o fim do contrato aos vínculos ainda abertos, com motivo próprio e sem ultrapassar a vigência.

A validação de matrícula considera as fronteiras de política já documentadas dentro de todo o período proposto. Cadastro sem matrícula que atravesse uma obrigação futura retorna 409, assim como antecipar o início, ampliar o término ou cancelar um desligamento programado quando a faixa restaurada alcançaria essa obrigação. Uma política opcional na nova data inicial não dispensa uma obrigação intermediária já registrada. Um término anterior à obrigação e uma matrícula válida informada continuam permitidos. Cancelar um aditamento de dispensa também retorna 409 se sua retirada deixar um vínculo vigente ou programado sem matrícula exigida; o ato permanece válido até o tratamento explícito da incompatibilidade.

`/{id}/eventos`, `/{id}/alocacoes`, `/{id}/alocacoes/eventos`, `/{id}/alocacoes/posicao` e `/pessoas/{pessoaId}/posicao` permitem reconstrução e consulta histórica. Alocações aceitam `mes=AAAA-MM`; posições exigem `data` e aceitam `conhecidoAte` em formato Instant. Um corte de conhecimento anterior ao registro não recebe um vínculo descoberto posteriormente.

Na posição da Pessoa, um vínculo atingido por anulação contratual apresenta `situacaoTemporal=ANULADO` a partir da data de alcance reconstruída, respeitando também `conhecidoAte`. Datas anteriores conservam sua situação própria. Essa informação é derivada da posição temporal do contrato e preserva o identificador, os eventos e o estado registrado do vínculo.

Eventos de vínculo incluem `motivoDesligamentoIdFotografia`, `motivoDesligamentoDescricaoFotografia` e `motivoDesligamentoCategoriaFotografia`. A descrição aplicada também é preservada no vínculo. Renomear um motivo não muda um desligamento anterior. Motivos hoje inativos podem ser utilizados em fatos retroativos quando a disponibilidade histórica comprovar validade naquela data.

O cabeçalho de `PosicaoPessoaDTO` contém `nomeAtual` e `cpfAtual`, conforme os nomes dos campos. A identidade fotografada no momento de cada fato é consultada nos eventos, por `pessoaNomeFotografia` e `pessoaCpfFotografia`; a correção cadastral não altera essas fotografias.

## Cadastros e histórico administrativo

Cargo, Tipo de Contratação, Motivo de Desligamento, Lotação e Jornada são administrados no escopo da Gestão Local. O ID é mantido ao inativar/reativar. Cargo, Lotação e Jornada não podem ser inativados enquanto composições ou alocações ainda cobrem dias futuros sem tratamento. Cancelar uma programação isolada não elimina os demais usos futuros existentes.

Jornada mantém os sete dias por identidade diária e atualiza horários sem recriar as linhas com a mesma chave de dia da semana. Regras adicionais pertencem à Jornada e respeitam o mesmo escopo. As fotografias já aplicadas a alocações/frequências não são reescritas ao editar os horários atuais.

A publicação nova conserva a grade semanal e o município na composição. Combinação nova documentada em aditamento conserva sua configuração própria; ADI apenas quantitativo e remanejamento mantêm os nomes e a configuração da combinação existente, inclusive campos desconhecidos. Os rótulos fotografados no registro de um item quantitativo são contexto desse registro e não autorizam uma renomeação retroativa da combinação. A projeção temporal transporta esses valores sem alterar os mestres ou as fotografias JPA. Mapas de transferência aplicáveis prevalecem com sua fotografia documental.

As novas colunas históricas são opcionais para o acervo anterior: leitura, atualização de outro campo e reinicialização não as preenchem com os cadastros atuais. Município originalmente nulo continua nulo mesmo quando a grade está comprovada. Sem grade histórica suficiente, a frequência e o relatório respondem 409 para tratamento documental; desconhecimento não equivale a descanso ou previsão de zero horas.

`/api/historicos-empregados`, `/{id}`, `/empregado/{id}` e `/empregado/{id}/integridade` usam o escopo fotografado em cada período. Alterar a projeção atual do Empregado não muda o escopo de um evento antigo. A verificação de integridade confere a cadeia HMAC e sua âncora final, incluindo quantidade e último registro.

Nos três GETs que retornam registros, `descricao` é texto livre e pode conter valores pessoais antigos ou novos, inclusive RG e e-mail inseridos pelo próprio narrador de alterações. O Gestor Local autorizado recebe a narrativa integral. Gestor Estadual, Gestor Nacional e Administrador recebem `descricao=null`, preservando `id`, referência do Empregado, `dataEvento`, usuário responsável, `hashIntegridade` e `version`. Essa minimização ocorre somente no DTO: texto original e cadeia assinada permanecem intactos. O cliente não deve interpretar a ausência do texto como ausência do evento.

## Evidência de verificação

Os testes completos desta área estão no pacote `com.example.siggtrat.auditoria20261029`: `PessoasLacunasHttpIT`, `PessoasVinculosContratoHttpIT`, `PessoasAtomicidadeHttpIT`, `HistoricosEmpregadosContratoHttpIT`, `CatalogosContratoHttpIT`, `CatalogosTemporalHttpIT`, `PessoasEscopoOperacionalHttpIT`, `PessoasValidacoesAdicionaisHttpIT`, `PessoasMinimizacaoEventosHttpIT`, `PessoasHistoricoMinimizacaoHttpIT`, `PessoasComposicaoFuturaHttpIT` e `PessoasAssinaturaImpactoVolumeHttpIT`. Eles passam pela aplicação, segurança, DTOs, serviços e MySQL. `PessoasAtomicidadeHttpIT`, `PessoasComposicaoFuturaHttpIT` e `PessoasHistoricoMinimizacaoHttpIT` executam sem transação externa de teste e verificam o estado após commits ou rollback. Os cenários concorrentes observam a espera efetiva no `performance_schema` do MySQL antes de liberar a etapa dependente da transação bloqueadora.

Os números e resultados das execuções atuais são publicados nos relatórios e XMLs de `conclusao-198`. Resultados históricos de rodadas anteriores não servem como comprovação da versão atual. A matriz final associa cada critério aos métodos efetivamente executados, aos arquivos de produção e aos hashes da rodada correspondente.
