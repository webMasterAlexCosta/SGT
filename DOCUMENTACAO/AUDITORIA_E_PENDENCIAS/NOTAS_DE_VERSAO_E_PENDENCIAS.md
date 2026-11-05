# Notas da candidata C25-H04 e pendências

## Identidade da entrega

Esta derivada parte da baseline C25-H02/build04, preservada com seu estado reprovado, e considera H03 como checkpoint ainda não homologado. O commit de produto é `19824eb0aa93d6a4d826b757852fee15a68adb92`. O JAR tem SHA256 `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d`; o selo F11 operacional rev2 tem SHA256 `ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79`.

As denominações históricas Java 0.0.4 e React 1.4.4 não identificam, sozinhas, os bytes desta entrega. Use os hashes, o commit, os manifestos e os IDs de execução. Os nomes da pasta e do documento mestre conservam `20261103`; as execuções que atravessaram a meia-noite têm seus horários reais de 4 de novembro em UTC.

## Correções implementadas

| Área | Alteração efetiva | Contratos preservados |
|---|---|---|
| Documentos comprobatórios | Flush depois de registrar o evento e antes de montar o DTO nas operações de criar, retificar, substituir e invalidar. | Versão documental de negócio, versão técnica, autorização, transação, arquivo, idempotência e locks. |
| Ambiente MySQL | Guarda opcional da DataSource antes de inicializar Hibernate, exigindo produto MySQL e versão exata 8.0.45. | Nenhuma criação manual de tabela ou dado; sem endpoint de diagnóstico exposto. Recusa nominal de contas administrativas, com limites de teste declarados. |
| Elegibilidade do fechamento | Interrupção da avaliação de candidatos depois de preencher o bloco solicitado, aplicada após o filtro. | Ordem, cursor, contagem total, regras temporais e avaliação dos vínculos da mesma Pessoa. |
| Cancelamento concorrente do fechamento | Detach da entidade do job recém-lida e ainda sem mudanças pendentes, para que o lock final obtenha a versão corrente. | `@Version`, bloqueio pessimista, isolamento, atomicidade de frequência/cursor/contador, heartbeat e cancelamento cooperativo. |
| Sessão React | Identidade compartilhada e opaca da sessão e verificação da geração nos caminhos de saída, atividade e resposta assíncrona. | Uma sessão antiga não pode iniciar mutação ou revogação na sessão seguinte; cenários positivos de renovação, mesmo usuário e indisponibilidade parcial de armazenamento continuam testados. |
| Prorrogação React | Entrada editável de `novaDataFim`, separada da data efetiva, invalidação da prévia obsoleta e tratamento dos estados de confirmação. | Demais aditamentos, validação do servidor, token da prévia, espera, cancelamento e preservação do formulário em erro. |

São **quatro arquivos produtivos Java e oito React**. A matriz de correções tem 112 linhas: 15 pares defeito/arquivo React, quatro alterações Java e 93 caminhos de assets derivados do build. Isso não representa 112 defeitos independentes. Alterações de testes, infraestrutura de validação, documentos e arquivos gerados têm sua justificativa no inventário de 271 caminhos comparados diretamente entre os ZIPs originais e finais.

Não foi inventada uma correção do renderizador PDF. FE-01 recebeu contraprovas e controles de sessão; a inspeção visual do PDF de 41 registros não confirmou a suspeita de cabeçalho encoberto. As renderizações e a contraprova por pixels foram preservadas.

## Regressão nova e alcance

`F12_JAVA_COMPLETO_04` executou **599 testes unitários e 1.400 de integração**, em **449 XMLs frescos**, sem falhas, erros ou skips. O build recompilado produziu exatamente o JAR selado. A reprodução independente de 14 unitários e 17 integrações repetiu um subconjunto desses testes; não se soma esse número como testes únicos novos.

`F12_REACT_COVERAGE_01` executou **653 invocações em 88 suítes**, sem falhas, erros ou skips. O multiconjunto preserva 649 identidades classe/nome e suas multiplicidades. Os 51 casos novos são subconjunto da suíte. O build de produção foi reproduzido e os 98 assets conferidos no ZIP, no JAR e por HTTP antes e depois do reinício.

A cobertura Java cumulativa reporta 90,30% das linhas e 65,23% dos ramos de seu universo. React reporta 73,58% e 57,19%, respectivamente. Os 269 JS/JSX de `src` se dividem em 163 reportados e 106 ausentes; os 232 registros totais do provider incluem ainda 69 itens de outra natureza. Não foi presumido percentual para arquivos ausentes nem cobertura de negócio a partir dessas métricas.

A campanha Native atual separa a criação de fatos na H02 das verificações da derivada: **3.250 asserções e 1.357 requisições no JAR 6ab**, além das 105/59 de preparação H02. Ela comprovou os casos HTTP declarados, os oito cenários documentais e sua revalidação após reinício, os 98 assets por fase, os arquivos sintéticos e a persistência no alcance observado. As fotografias de upgrade e reinício não comprovam preservação de tabelas legadas que não existiam na massa sintética.

O ciclo de chunk 1 concluiu o controle de **200/200**, cancelamento com prefixo positivo e reprocessamento, revogação real da sessão do criador, queda da JVM própria, recuperação automática e segunda inicialização sem novo pedido. O controle levou 149,162 s e a repetição chunk 500 terminou 200/200 em 361,557 s. A primeira tentativa de chunk 500 ficou interrompida e não recebe PASS; a repetição usou identidade e fixture distintas. O comparador novo conferiu 200 ordinais × 19 campos normalizados, com sete verificações aprovadas. Dois timeouts SQL1205/SQLState40001 do consumidor automático ocorreram durante a transação manual longa. A massa não continha frequências vencidas para demonstrar sua recuperação; o PASS manual não elimina esse limite.

## Pendências e bloqueios explícitos

### Navegador

O Google Chrome fornecido foi lançado novamente com Playwright enquanto o JAR final estava vivo. O processo terminou por `SIGABRT` após negação de AF_UNIX no `ProcessSingleton`, antes de CDP ou DOM. **Zero jornadas foram executadas; as 24 jornadas mínimas permanecem não executadas por bloqueio de ambiente.**

Isso mantém sem homologação visual a troca real de abas de RI03, o idle no navegador, a confirmação da prorrogação pela interface, os estados de erro e sucesso de ROP-064, FE-01, menus, foco, zoom, downloads e o dashboard dos jobs. Os testes unitários e HTTP não recebem crédito por esses predicados. O bloco exige um ambiente que permita iniciar o Google Chrome fornecido, preservando MySQL Server 8.0.45 e Redis.

### Documentos administrativos autênticos

| Grupo | Critérios dependentes de evidência autêntica |
|---|---:|
| CON-023 | 9 |
| CAT-004 | 4 |
| CAT-007 | 8 |
| COL-010 | 4 |
| HIS-001 | 4 |
| HIS-006 | 4 |
| **Total** | **33** |

Dados sintéticos, PDFs gerados e referências SEI de teste permitem exercitar software, mas não demonstram autenticidade administrativa, competência ou fatos reais. Esses critérios não foram aprovados por existência de uma fixture.

### Predicados e limites adicionais

- **ALO-008:002:** a bateria complementar manteve 49 invocações, com 48 PASS e uma falha de preparação. O aditamento esperado como 201 foi recusado com 409 porque reduziria composição abaixo da ocupação. A reativação pretendida não foi alcançada. A investigação mostrou por que as sequências examinadas de inativação/prorrogação não eliminam legitimamente a reserva temporal; não é prova universal de inalcançabilidade de todo estado inválido. Não foi fabricado estado por SQL/JPA.
- **PER-010:004, conceder:** os consumidores e controles efetivamente exercitados ficam discriminados na matriz. Uma concessão genérica 201, sem vínculo suficiente com ocorrência e período histórico, não aprova o predicado contextual de conceder.
- **FE-02:** a continuação HTTP/SQL/reinício03 e o writer React03 comprovaram dez séries diárias distintas e os dois XLSX reais, com sete abas, 15×12 posições diárias e igualdade de valores/tipos/formatos depois do reinício. B–K têm 15 valores distintos, L tem 14, e 29/02 não inventa comparativo. Foram 106 chamadas/328 asserções HTTP, 26.739 verificações do writer e 23.706 da revisão independente, mantidas como contagens separadas. O contexto foi construído fora da UI; gráficos, download Chrome e PDF React da massa discriminante continuam sem comprovação. A desativação do watcher não foi demonstrada. O ensaio anterior com séries constantes conserva seu recorte e resultado próprios.
- **Temporalidade e acessibilidade:** testes com relógio lógico não demonstram uma virada física de meia-noite; os cenários de navegador, tecnologia assistiva e conformidade PDF/UA não avaliados permanecem sem comprovação.
- **Escala e desempenho:** a carga JPA de 24 workers por 15 segundos e o corpus de 200 GETs autenticados são massas sintéticas delimitadas. Não há demonstração de capacidade nacional, SLA geral ou superioridade de uma variante por inferência.
- **Dependências:** as 88 bibliotecas embarcadas não foram atualizadas. O parecer de segurança distingue advisories aplicáveis à faixa de versão, pré-condições e ausência de uso encontrada na inspeção; não equivale a comprovação de ausência universal de vulnerabilidades.

## Ocorrências operacionais preservadas

A candidata anterior, JAR `2e9219…`, permanece reprovada no cancelamento concorrente que motivou o patch atual. A F12 Java03, falha por falta de privilégios em um catálogo, foi preservada sem converter erros de inicialização em defeitos de negócio nem ocultá-los. O reparo de privilégios foi mínimo e foi seguido da nova F12 Java04 integral.

A sessão de runtime perdida durante a primeira rodada chunk 500 não teve término normal observado. Seus arquivos foram copiados após observação dos processos ausentes e estabilidade dos inventários, e o MySQL realizou recuperação normal mantendo a UUID. Não houve recuperação forçada, remoção de redo ou reconstrução artificial de resultado. A causa permanece indeterminada; os contadores OOM/oom_kill observados eram zero. A repetição posterior teve nova identidade e resultado terminal próprio, sem converter a tentativa interrompida em aprovação.

A FE02_HTTP_VARIADO_01 ficou interrompida, com 164 chamadas/275 asserções capturadas, sem terminal ou aprovação. As qualificações01–03 falhas foram preservadas, e a qualificação04 comprovou o prefixo de 72 itens por leitura e comparação integral. A continuação02 falhou em uma expectativa instrumental de normalização após um POST201; o item persistido não foi reenviado. A extensão73/01 falhou antes da aplicação por uma captura do ACK no diretório protegido; a extensão73/02 qualificou o item adicional. A continuação03 criou somente os 49 vínculos restantes e registrou 29 lotes, alcançando 122 vínculos do corpus e 1.392 frequências. O catálogo total contém ainda dois auxiliares preservados. O writer03 consumiu os DTOs finais antes/depois do reinício. Os primeiros FAILs dos leitores de enum, representação temporal, corte histórico, assinaturas e metadados mantêm os dados originais, diffs e contraprovas; não foram ocultados nem convertidos em aprovações das tentativas anteriores.

A discrepância histórica mencionada ocorreu por volta de 00:49 UTC. A classe de mesmos bytes encontrada no encerramento atual teve uma geração posterior comprovada na preparação FE02, entre 02:47:18.824557 e 02:47:20.122227; isso não explica retroativamente a reintrodução das 00:49. A tentativa de limpeza03 falhou antes de qualquer remoção porque comparava um baseline de 724 classes a um inventário que também continha três recursos normais. Os três recursos foram comprovados no ZIP Java selado. A derivação04 manteve o inventário de todos os arquivos e a recusa de extras desconhecidos; arquivou e releu a única classe gerada antes de removê-la. Encerrou às 08:29:25 UTC, e uma chamada separada às 08:29:41 confirmou 724 classes + três recursos = 727 arquivos normais, zero fixture, 899 arquivos principais e JAR intactos. O recibo dessa releitura tem SHA256 `0ca3eea249581655e191cca4909916ac60a3e62c97b784751c078d1c8853f41f`. Não foi executado novo produto, Node, HTTP, SQL ou JVM para essa limpeza; ela comprova apenas o estado posterior. O último MySQL/Redis encerrou normalmente, com códigos zero, às 08:18:50 UTC; a limpeza não reiniciou serviços nem modificou os quatro produtos selados.

## Uso da entrega

Os quatro artefatos de produto são os ZIPs de fontes Java e React, o ZIP de build React e `sgt-backend-final.jar`. Para reproduzir a identidade literal, use esses ZIPs e o guia de build. O diff Git tem 269 caminhos; a comparação dos originais tem 271 por duas exceções de importação documentadas. Aplicar somente o patch a uma extração bruta da baseline não reproduz automaticamente essas duas exceções.

O JAR e os fontes corrigidos são entregues para análise e reprodução no alcance declarado. **Esta tarefa não autoriza implantação em produção.** A decisão formal e a lista final de impedimentos constam em `RELATORIO_HOMOLOGACAO_FINAL.md`; os manifestos e o recibo de publicação distinguem identidade dos arquivos de aprovação funcional.
