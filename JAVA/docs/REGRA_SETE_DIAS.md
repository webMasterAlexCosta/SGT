# Regra real dos sete dias da frequência

Data documental: **01/11/2026 — America/Sao_Paulo**, conforme referência local do cliente. Carimbos de execução histórica, quando citados, permanecem em UTC de **02/11/2026**. Escopo: leitura da especificação, Java e React da baseline. Nenhum teste ou navegador foi executado nesta investigação e nenhuma regra foi alterada.

Raízes consultadas:

- Java: `/workspace/scratch/eb5933b67e7b/sgt_remocao/baseline/backend`.
- React: `/workspace/scratch/eb5933b67e7b/sgt_remocao/baseline/react`.

## 1. Regra fechada e fonte de autoridade

**O marco é a data de início da jornada (`dataReferencia`), que conta como dia 1. A janela do Operacional compreende sete dias civis corridos, em `America/Sao_Paulo`, e termina no início de D+7, o oitavo dia. O limite final é exclusivo.**

Exemplo normativo: frequência de 01/01 permite edição normal até o fim de 07/01; a partir de 08/01 às 00:00, está `FECHADA`. Não se contam sete dias úteis, sete dias depois da gravação nem 168 horas depois do primeiro lançamento.

Fonte normativa interna principal: `backend/docs/LEVANTAMENTO_REQUISITOS.md`, versão 8.0 de 06/09/2026, seção 5.22 e requisitos FRE-001 a FRE-019, em especial:

| Requisito | Determinação |
|---|---|
| FRE-001 | Resolver vínculo e alocação válidos na data negocial. |
| FRE-002 | Sete dias corridos; data da frequência é dia 1; fuso São Paulo. |
| FRE-003 | Exemplo de 01/01 até 07/01 23:59:59; bloqueio em 08/01 00:00. |
| FRE-004 | Alteração operacional na janela mantém antes/depois, autor e instante. |
| FRE-005/006/007 | Gestor Local corrige dentro ou depois da janela, com retificação formal; não existe prazo máximo rígido para a retificação tardia. |
| FRE-013 | Fechamento negocial automático no oitavo dia; qualquer mudança posterior é retificação. |
| FRE-014 | Intervenção do Gestor gera versão e bloqueia nova edição daquele registro pelo Operacional. |
| FRE-015/016/018 | Escopo histórico da alocação combinado com autorização atual; mudança retroativa não transfere silenciosamente a autoria/edição operacional. |
| FRE-017/019 | Perda de cobertura preserva auditoria, separada de validade operacional e estado negocial. |

`backend/AGENTS.md` coloca esse levantamento e decisões posteriores expressamente comprovadas acima de código/testes e relatórios históricos. `backend/docs/INTEGRACAO_FRONTEND.md`, seção 7.2, reafirma explicitamente D+7 às 00:00 e a independência dos três estados. Nenhuma decisão posterior que mude a contagem foi localizada na investigação. Esta é a regra do projeto; não foi inferida de legislação externa.

## 2. Inclusividade e fuso implementados

`backend/src/main/java/com/example/siggtrat/servico/PoliticaTemporalFrequenciaServico.java` é a implementação central:

```java
dataFrequencia.plusDays(7).atStartOfDay(zona)
```

`fechada(data, agora)` converte `agora` pelo mesmo instante para o fuso negocial e retorna `!referencia.isBefore(instanteFechamento(data))`. Logo:

| Instante em São Paulo, para D = 01/01 | Estado temporal |
|---|---|
| 01/01, durante o primeiro dia | Janela aberta, sujeita às demais validações. |
| 07/01 23:59:59 | Janela aberta. |
| Frações de segundo de 07/01 anteriores à meia-noite | Ainda anteriores ao limite exclusivo. |
| 08/01 00:00:00 exato | Fechada. |
| Qualquer instante posterior | Fechada. |

A formulação robusta é `agora < início de D+7`; a referência normativa a 23:59:59 não significa descartar frações do último segundo. O cálculo é civil, com `ZoneId`, e não usa offset fixo. `ConstantesDominio.TIMEZONE_PADRAO` e `RelogioSistema.ZONA` são `America/Sao_Paulo`. O fuso do computador do usuário não altera a autorização Java.

`prazoLocal(data)` retorna o instante do fechamento convertido para `LocalDateTime`. Assim, `prazoEdicao` é **o limite exclusivo do oitavo dia**, não o último segundo autorizado. `FrequenciaDTO.prazoEdicao` não traz um offset próprio.

## 3. O que o prazo permite e o que continua bloqueado

| Situação | Regra de escrita observada |
|---|---|
| Operacional, dia atual ou passado, dentro da janela | Pode registrar/alterar se possuir autorização atual sobre o escopo histórico e houver cobertura válida. |
| Operacional, data com D+7 já alcançado | Não pode registrar nem alterar; o serviço responde 403. |
| Gestor Local, dentro da janela | Pode intervir com justificativa e idempotência; cria evento/versão formal e bloqueia o Operacional somente no registro atingido. Não fecha antecipadamente o estado negocial. |
| Gestor Local, depois da janela | Pode retificar sem prazo máximo rígido, preservando estado fechado, histórico, justificativa e autoria. |
| Registro com intervenção gerencial anterior | Não volta à edição operacional, mesmo se a janela ainda estiver aberta. |
| Registro sujeito a revisão ou com validade operacional diferente de `VALIDA` | Operacional é recusado; tratamento cabe ao Gestor, com validação do contexto. |
| Administrador, Estadual, Nacional ou outros perfis sem escrita negocial | Não ganham poder de retificar pela posição hierárquica; o backend recusa a escrita. |
| Data futura | Lançamento de frequência é recusado antes da regra de janela, inclusive para Gestor Local. |
| Vínculo, contrato ou alocação sem efeito válido na data | A janela aberta não autoriza lançamento; o resolvedor temporal recusa o contexto. |

Fontes Java: `FrequenciaServico.salvarOuAtualizarInterno()`, `validarEdicaoOperacional()`, `exigirAutorizacaoNoContexto()`, `exigirRetificacaoGerencial()` e `exigirMesmoContextoHistorico()`.

Detalhes da retificação gerencial observados no código: justificativa com ao menos cinco caracteres e chave de idempotência com oito a 120 caracteres. Esses controles não criam uma exceção para presença futura nem permitem usar vínculo inexistente na data. O serviço mantém `autorOriginalId`, grava evento com snapshots e, para nova intenção válida, atualiza `versaoNegocial` e invalida relatórios dependentes. Reenvios idempotentes não são novas alterações e têm controles específicos.

## 4. Marco da jornada, cobertura e fotografias

O marco da janela não é `createdAt`, `updatedAt`, `registradoEm` ou instante de retificação. A política recebe exclusivamente `dataReferencia`.

Para turno noturno, a frequência pertence à **data de início da jornada**. Um turno de 22:00 de 10/09 até 06:00 de 11/09 continua pertencendo a 10/09 e usa o vínculo/alocação desse dia. Alteração em 11/09 não move o registro para outra alocação nem reinicia a janela. Fonte: levantamento, seção 5.22; controle existente `FrequenciaTemporalBordasHttpIT.jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte()`.

`ResolvedorContextoTemporalFrequenciaServico.resolverPorVinculo()/resolverEntidades()` exige, na data da jornada:

- vínculo `VIGENTE`;
- contrato `PUBLICADO`, temporalmente `VIGENTE` e administrativamente `ATIVO`;
- alocação `VIGENTE` com identidade disponível e vínculo coerente;
- Gestão histórica definida e escopo histórico consistente.

A fotografia da alocação e suas fontes comprovadas sustentam a jornada. Ausência de prova não pode ser corrigida com o cadastro atual por conveniência. Retroatividade autorizada altera a verdade oficial de modo versionado, sem apagar prova original.

GOV-003 proíbe apagar fisicamente fato negocial publicado. FRE-009/010/017/019 exige preservar frequência atingida por perda retroativa de cobertura, sinalizar o problema e excluí-la da visão operacional válida. `RevalidacaoFrequenciaTemporalServico` registra impacto e snapshots; essa revisão não equivale a reabrir a janela ou autorizar presença sem vínculo. `estadoNegocial`, `validadeOperacional` e `estadoAuditoria` são eixos diferentes.

## 5. Fechamento negocial e processamento técnico

O prazo independe de lote, relatório ou exportação. `FrequenciaServico.paraDTO()` projeta `FECHADA` quando a política temporal já fechou; a escrita consulta a mesma política, portanto não depende de o job já ter persistido o estado.

`FechamentoAutomaticoFrequenciaNegocialServico.fecharVencidas()` persiste fechamento e evento auditável em lotes transacionais; o cron padrão é a cada minuto no fuso São Paulo. Isso não concede até um minuto extra ao Operacional. `FrequenciaServico.fecharNegocialmenteSeNecessario()` também aplica fechamento auditável quando cabível.

A retificação do Gestor dentro da janela mantém o registro `ABERTA` até o prazo natural e utiliza `bloqueadaEdicaoOperacional=true`. Converter esse bloqueio antecipado em `FECHADA` confundiria duas regras distintas.

## 6. Configurações que não são exceção à regra

`PropriedadesFrequencia` contém limites de intervalo de consulta e lote, não duração configurável da janela. A política central usa sete dias diretamente.

Existem campos e comandos de usuário chamados `retroactiveUnlockExpireAt`, `retroactiveUnlockHours` e `retroactiveUnlockDays`; `UsuarioAplicacaoServico` produz mensagem de liberação retroativa. Na busca das fontes principais, a autorização canônica de frequência **não lê esses campos**. Sua existência não comprova uma exceção a FRE-002/003/013 nem autoriza reabrir frequência para Operacional. Qualquer decisão sobre remoção/ajuste desse resíduo está fora deste diagnóstico.

Outras referências a “sete dias” podem significar os sete dias da grade semanal de um turno ou marcos de alerta contratual de 30/15/7 dias. Elas não definem a janela de edição de frequência.

## 7. React: comportamento lido e ponto a verificar

`react/src/services/rh/ServicoDominioFrequencia.js`, função `frequenciaBloqueadaParaOperacional()`, usa os estados retornados pelo backend e `bloqueadaEdicaoOperacional` para registros já salvos. `react/src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js`, função `obterRelatorioDaJanelaDeEdicao()`, declara o prazo como informativo; a escrita continua sujeita à validação do POST no servidor.

Há uma possível divergência **de apresentação**, ainda não reproduzida: `prazoEdicao` é `LocalDateTime` sem offset no DTO, e o contador usa `new Date(prazoServidor)`. Uma string sem offset pode ser interpretada no fuso local do navegador, deslocando o contador em computadores fora de São Paulo. Isso não altera a política Java. Antes de corrigir, verificar o JSON efetivo e o contador em navegadores com fusos diferentes. O achado foi comunicado à frente React; nenhuma mudança foi feita.

Para registros ainda não salvos, a função de bloqueio retorna `false`; isso também não comprova permissão operacional fora da janela, pois a requisição deve ser recusada no backend. A análise estática não substitui a prova do fluxo completo.

## 8. Controles existentes e validação pendente

| Controle localizado | O que o código do teste verifica |
|---|---|
| `FrequenciaEtapa09Test.janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia()` | Aberta no fim do sétimo dia, fechada à meia-noite do oitavo e prazo D+7. |
| `FrequenciaEtapa09Test.politicaConverteInstanteParaSaoPauloSemOffsetFixo()` | Conversão de instante UTC para a fronteira em São Paulo. |
| `FrequenciaContratosHttpIT.perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado()` | Escrita operacional na janela, intervenção gerencial, estado ainda aberto e recusa posterior ao Operacional. O método lido não inclui uma segunda frequência de controle, apesar do nome; não ampliar a conclusão. |
| `FrequenciaContratosHttpIT.operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve()` | Recusa da escrita fora da janela e por Gestão/perfil indevido. |
| `FrequenciaTemporalBordasHttpIT.alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal()` | Auditoria operacional, snapshots e autor original. |
| `RevisaoFechamentoNegocialMysqlHttpIT` | Fechamento persistido e evento auditável no MySQL. |
| `PreservacaoFrequenciaSemFinanceiroHttpIT.apiRecusaPresencaFuturaSemGravarFonteOuEvento()` | Futuro recusado para Gestor e Operacional, sem fonte/evento. |
| `PreservacaoFrequenciaSemFinanceiroHttpIT.loteComItemFuturoReverteTambemOPrimeiroItemValido()` | Atomicidade do lote com item futuro. |

Esses testes foram **lidos**, não executados por este investigador. Os relatórios anteriores afirmam aprovações nos recortes selecionados, mas a missão atual requer recibos vinculados à fonte e MySQL reais usados agora. A reprodução deve manter os controles de fronteira, presença futura, cobertura na data, retificação e auditoria; remover esses controles para simplificar a suíte não comprovaria preservação funcional.
