# Matriz transversal de 76 posições — candidata04

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

Corte documental C04: 2026-11-04T08:22:33.455770+00:00. JAR 6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d; selo ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79. Corte C03 preservado no histórico.

**Homologação parcial.** A exportação é documental e não executa produto. Os valores completos estão nas tabelas CSV e nos JSONs de rastreabilidade. Nenhum campo vazio autoriza aprovação. 33 critérios documentais, Chrome 0/24, ALO/PER, FE02 e erros do consumidor automático permanecem qualificados.

68 critérios individualizados e oito posições PERF de um intervalo com parágrafo comum. Não foram inventados oito predicados de desempenho independentes. Namespaces preservam a seção para evitar confusão entre códigos iguais.

O adendo C04 acrescenta a continuação HTTP/SQL/reinício03 e o XLSX variado03, ambos revisados independentemente. A limitação antiga de séries constantes fica como histórico, superada apenas no novo recorte DTO/OOXML; UI e PDF React continuam não comprovados. O mapa de controllers é estático, com handlers e fontes, e não altera classificações funcionais. Veja [o adendo e seus limites](RASTREABILIDADE_ADENDO_FE02_C04/ADENDO_C04_FE02_E_CONTROLADORES.md).

## 8.1::INF-001

**Procedimento/contexto literal:** Listar, extrair e hashear os quatro pacotes H02 e seis H03.

**Aceite literal:** Quatro SHA-256 H02 idênticos à tabela; H03 segregado; ZIPs inteiros e sem path traversal.

**Status:** COMPROVADO NO PROCEDIMENTO DE IDENTIDADE E SEGREGAÇÃO.

**Limite:** Releitura dos quatro H02 com SHA-256/CRC, nomes seguros e ausência de duplicados/links; seis H03 segregados e identificados. A extração original está documentada, mas não foi repetida nesta releitura. Nenhuma aprovação funcional de H03 é inferida..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/0).

## 8.1::INF-002

**Procedimento/contexto literal:** Inventariar dependências reais da Biblioteca antes de downloads.

**Aceite literal:** Inventário com versão, formato, tamanho, hash, incompatibilidades e licença/proveniência quando identificáveis.

**Status:** COMPROVADO NO ESCOPO LITERAL DO INVENTÁRIO DISPONÍVEL.

**Limite:** Oito pacotes fornecidos têm versão, formato, tamanho, SHA e proveniência inventariados. O complemento registra 530 arquivos nomeados de licenças/avisos identificáveis, com hashes e limites por pacote. Chrome contém apenas aviso Widevine identificável; não se atribui esse aviso a todo Chrome. Redis não trouxe texto próprio identificável e libaio é arquivo isolado. Não se exige nem se afirma SCA transitivo, licença uniforme ou parecer jurídico. O inventário inicial precede os downloads; o complemento de avisos tem data posterior explicitada..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/1).

## 8.1::INF-003

**Procedimento/contexto literal:** Resolver e iniciar `mysqld` do pacote 8.0.45 em runtime compatível.

**Aceite literal:** Processo, PID, porta, datadir isolado, logs e binário exato 8.0.45; zero fallback.

**Status:** COMPROVADO NO PROCEDIMENTO DE PROCESSO E IDENTIDADE.

**Limite:** Binário MySQL 8.0.45 fornecido, comando do processo próprio, PID/namespace, porta, datadir isolado, logs e conexão foram vinculados. A leitura de /proc/PID/exe foi negada; os demais vínculos são explícitos. A igualdade de UUID não significa continuidade ininterrupta do processo e nenhum fallback foi usado..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/2).

## 8.1::INF-004

**Procedimento/contexto literal:** Conectar via cliente e JDBC da aplicação/Testcontainers.

**Aceite literal:** `VERSION()`, UUID, `CURRENT_USER()`, `DatabaseMetaData` = MySQL Server 8.0.45 na instância correta.

**Status:** PARCIAL.

**Limite:** DataSourceprodutivo confirma produto/version45 e registraUUID; comparaçãoUUIDesperado eGRANTS pertencemàinstrumentação externa, nãoaoBeanPostProcessor..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/3).

## 8.1::INF-005

**Procedimento/contexto literal:** Forçar tentativa de versão diferente no teste guardião (somente ambiente descartável).

**Aceite literal:** Teste falha fechado; não altera oráculos de domínio. Não executar suite funcional de homologação em versão proibida.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/4).

## 8.1::INF-006

**Procedimento/contexto literal:** Iniciar Redis verdadeiro; executar ping, gravar/ler chave de teste e revogar token sintético.

**Aceite literal:** Comando confirmado, conexão isolada, sem fake ou limpeza de base compartilhada.

**Status:** COMPROVADO NAS OPERAÇÕES OBSERVADAS, COM LIMITES DE CAUSALIDADE.

**Limite:** Redis verdadeiro confirmou AUTH/PING e SET/GET/DEL de chave própria no DB de teste. A revogação administrativa foi observada por HTTP e pela fotografia de sessões SQL, com credencial antiga recusada. Não houve inspeção direta da chave de revogação no Redis nem prova de causalidade exclusiva do Redis para cada 401. Nenhum FLUSH de base compartilhada é atribuído a essas operações..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/5).

## 8.1::INF-007

**Procedimento/contexto literal:** Validar Chrome fornecido e CDP Playwright com página local e captura.

**Aceite literal:** Executável real, versão, hash, screenshot DOM e console, não só importação da dependência.

**Status:** BLOQUEADO POR AMBIENTE.

**Limite:** Chrome real abortaAF_UNIX/ProcessSingleton/SIGABRT antesCDP/DOM;0/24..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/6).

## 8.1::INF-008

**Procedimento/contexto literal:** Compilar Java e React a partir das fontes extraídas.

**Aceite literal:** Maven/Node/lockfile/versões registradas; nenhuma dependência baixada silenciosamente em versão diferente.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/7).

## 8.1::INF-009

**Procedimento/contexto literal:** Carregar o bundle frontend dentro do JAR candidato.

**Aceite literal:** Manifesto e bytes dos assets servidos em HTTP correspondem ao build React declarado.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/8).

## 8.1::INF-010

**Procedimento/contexto literal:** Reiniciar servidor em mesma porta/banco/configuração e provar `ACCEPTING_TRAFFIC` e rota autenticada.

**Aceite literal:** Mesmo hash JAR, mesmo UUID MySQL, HTTP real e dados preservados.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/9).

## 8.1::INF-011

**Procedimento/contexto literal:** Registrar cotas de memória/CPU e medir consumo das suítes.

**Aceite literal:** Ausência de OOM/OOMKill ou diagnóstico separado; paralelismo pesado controlado.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/10).

## 8.1::INF-012

**Procedimento/contexto literal:** Inventariar e corrigir guards literais de 8.0.46 em testes/containers.

**Aceite literal:** Diff limitado e motivado, grep final de referências ativas, suíte 8.0.45 executável.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/11).

## 8.4::SEC-001

**Procedimento/contexto literal:** Sem autenticação solicita criação/retificação/arquivo protegido.

**Aceite literal:** 401/recusa contratual; **zero** mutação, evento ou arquivo publicado.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/12).

## 8.4::SEC-002

**Procedimento/contexto literal:** Usuário autenticado sem escopo territorial tenta ação de Gestor.

**Aceite literal:** 403; nenhuma alteração temporal ou auditoria de ato autorizado.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/13).

## 8.4::SEC-003

**Procedimento/contexto literal:** Operacional tenta alterar frequência fora de lotação/turno/data.

**Aceite literal:** 403/409 conforme contrato; autoridade verificada no servidor **pelo dia do fato**.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Os três casos não constituem uma única contraprova HTTP TCP de Operacional fora de lotação/turno/data após transferência. A terceira prova positiva usa Gestor. As integrações citadas herdam transação de teste; a observação não deve ser descrita como leitura após commit independente. A UI continua sem jornada Chrome..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/14).

## 8.4::SEC-004

**Procedimento/contexto literal:** Revogar permissão **após prévia e antes de confirmar**.

**Aceite literal:** Confirmação recusada com fonte/versão preservadas; UI não mostra sucesso.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** O oráculo aceita 401/403/404/409; não exige causa e mensagem únicas. Preserva a fotografia de campos/contagens selecionados, não todas as colunas. A alteração de conta/perfil não equivale a toda revogação possível de uma concessão específica. Feedback de sucesso da UI e confirmação TCP real permanecem sem prova neste vínculo..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/15).

## 8.4::SEC-005

**Procedimento/contexto literal:** Omitir/inutilizar token CSRF em POST/PUT/DELETE.

**Aceite literal:** Rejeição protegida; cookies/sessão não substituem CSRF.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/16).

## 8.4::SEC-006

**Procedimento/contexto literal:** Duas confirmações do mesmo token/idempotency key.

**Aceite literal:** Um único ato efetivo; replay canônico ou conflito contratualmente definido.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/17).

## 8.4::SEC-007

**Procedimento/contexto literal:** Dois usuários disputam mesmo contrato/versão.

**Aceite literal:** Um vencedor legítimo; perdedor 409, nenhuma perda silenciosa de atualização.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/18).

## 8.4::SEC-008

**Procedimento/contexto literal:** Token expirado durante download/operador revogado.

**Aceite literal:** Acesso negado mesmo com URL antiga; sem vazamento do arquivo.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/19).

## 8.4::SEC-009

**Procedimento/contexto literal:** Troca sessão A→B com resposta retardada.

**Aceite literal:** Nenhum dado A renderizado, nenhuma nova mutação de A; monitorar backend 403 se houver ataque direto.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Duas abas reais do Chrome com cookie HTTP compartilhado e resposta retardada não foram executadas. O gate exige monitorar recusa backend403 quando houver ataque direto; essa contraprova HTTP específica não é demonstrada por esses casos React nem pela Native vinculada. Ausência de mutação já em trânsito no backend não pode ser inferida da guarda da continuação local..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/20).

## 8.4::SEC-010

**Procedimento/contexto literal:** 2FA habilitar/desabilitar/erro/tentativas repetidas.

**Aceite literal:** Estabilidade de polling, sessão coerente e segredos omitidos de logs.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** As respostas e o segredo TOTP são sintéticos, na fronteira requisitar/adapter; não há habilitação/desabilitação TOTP TCP real demonstrada neste adendo. O assert de consulta estável não é observação de polling prolongado em Chrome. Omissão de segredos reais em logs operacionais durante todos esses atos requer contraprova específica; varredura do pacote público é outra camada e não substitui essa prova..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/21).

## 8.4::SEC-011

**Procedimento/contexto literal:** Logout concorrente com idle timeout/revogação.

**Aceite literal:** Sem exceção de exclusão otimista; token revogado e outras sessões não afetadas indevidamente.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Ausência de exceção optimistic-lock em colisão real de logout/idle/revogação não é demonstrada pelos casos React nem pelo logout sequencial da Native. Demais sessões intactas em concorrência real exigem cookies/atores reais e observação no backend; B do adapter não é sessão persistida. Chrome real permanece bloqueado antes de DOM/CDP. Este acréscimo cobre repositório/JPA, não HTTP, Redis, JWT real ou a corrida completa de idle/logout em navegador. O adendo React39 e os dois logouts TCP sequenciais continuam com seus próprios limites; nenhum deles é elevado a corrida HTTP por este caso..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/22).

## 8.4::SEC-012

**Procedimento/contexto literal:** Caminho/ID de documento de outra Gestão/usuário.

**Aceite literal:** Sem acesso indevido, incluindo path traversal e URL previsível.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A recusa de um ID conhecido cobre esse acesso previsível específico; não demonstra toda enumeração de URLs. Não foi localizado, nos corpos vinculados, vetor explícito de path traversal. Não se atribui proteção universal a caminho/URL nem jornada de navegador..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/23).

## 8.4::SEC-013

**Procedimento/contexto literal:** URLs antigas de governança com cache `200/HIT`.

**Aceite literal:** 404 autenticado e sem reexposição; Fornecedor operacional segue ativo.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/24).

## 8.4::SEC-014

**Procedimento/contexto literal:** Entradas HTML, strings de busca, anexos e parâmetros temporais malformados.

**Aceite literal:** Validação e escape corretos, nenhum XSS/injeção e sem mutação; registrar caso reproduzível sem tentar explorar terceiros.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Não há vetor HTML/XSS, string de busca injetada ou contraprova de escape no navegador nos métodos selecionados. Não se infere ausência geral de injeção. O teste de parâmetros da frequência não fotografa todas as mutações possíveis; cada assertiva vale para o campo/contador que efetivamente lê..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/25).

## 8.5::TMP-001

**Procedimento/contexto literal:** frequência em D, antes de `D+7 00:00` por 1 nanossegundo ou a menor precisão efetivamente suportada: permitida somente se vínculo/alocação/autorização/ausência de futuro também válidos.

**Aceite literal:** frequência em D, antes de `D+7 00:00` por 1 nanossegundo ou a menor precisão efetivamente suportada: permitida somente se vínculo/alocação/autorização/ausência de futuro também válidos.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A massa demonstra os pré-requisitos válidos desse cenário; não percorre todas as combinações de vínculo/alocação/autorização. ZonedDateTime.now é controlado no teste, com chamadas reais restantes. Não é observação HTTP TCP/Chrome no instante físico..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/26).

## 8.5::TMP-002

**Procedimento/contexto literal:** exatamente `D+7 00:00` (`America/Sao_Paulo`): encerrada, **limite exclusivo**.

**Aceite literal:** exatamente `D+7 00:00` (`America/Sao_Paulo`): encerrada, **limite exclusivo**.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A precisão de nanossegundo é do relógio Java controlado; não foi medido um relógio físico nem uma fronteira de agendamento. ZonedDateTime.now é controlado no teste, com chamadas reais restantes. Não é observação HTTP TCP/Chrome no instante físico..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/27).

## 8.5::TMP-003

**Procedimento/contexto literal:** depois de D+7: recusada na via operacional; verificar UI, API e persistência, não apenas contador.

**Aceite literal:** depois de D+7: recusada na via operacional; verificar UI, API e persistência, não apenas contador.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** O gate exige UI, API e persistência; somente MockMvc e persistência real foram exercidos aqui. A UI atual permanece bloqueada. ZonedDateTime.now é controlado no teste, com chamadas reais restantes. Não é observação HTTP TCP/Chrome no instante físico..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/28).

## 8.5::TMP-004

**Procedimento/contexto literal:** D+8: iniciar fechamento quando programado e verificar idempotência.

**Aceite literal:** D+8: iniciar fechamento quando programado e verificar idempotência.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** O caso integrado chama o serviço diretamente no início do oitavo dia (D+7); não é execução do scheduler no D+8 literal nem no horário físico programado. Os dois timeouts reais 1205/40001 do consumidor automático no controle chunk500 permanecem em evidência. Não houve massa vencida legítima naquele ensaio que comprovasse sua recuperação sob contenção..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/29).

## 8.5::TMP-005

**Procedimento/contexto literal:** presença futura recusada pelo Gestor e Operacional, independentemente da UI.

**Aceite literal:** presença futura recusada pelo Gestor e Operacional, independentemente da UI.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** São requisições MockMvc autenticadas de forma sintética; não há conexão TCP nem interação de UI neste vínculo. O relógio real do ensaio determina a data de amanhã..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/30).

## 8.5::TMP-006

**Procedimento/contexto literal:** turno noturno começando em D e encerrando em D+1: apuração pertence a D.

**Aceite literal:** turno noturno começando em D e encerrando em D+1: apuração pertence a D.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Preparação de catálogos é sintética e a transação de teste é herdada. Não comprova commit independente, troca de data na UI ou navegador..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/31).

## 8.5::TMP-007

**Procedimento/contexto literal:** cargo/lotação/turno muda em D+1: fato iniciado em D conserva os valores efetivos de D.

**Aceite literal:** cargo/lotação/turno muda em D+1: fato iniciado em D conserva os valores efetivos de D.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/32).

## 8.5::TMP-008

**Procedimento/contexto literal:** contrato publicado com retificação posterior: data de efeito e data de conhecimento geram fotografias diferentes.

**Aceite literal:** contrato publicado com retificação posterior: data de efeito e data de conhecimento geram fotografias diferentes.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/33).

## 8.5::TMP-009

**Procedimento/contexto literal:** catálogos inativos hoje, mas válidos em D: uso histórico positivo sem reativação artificial.

**Aceite literal:** catálogos inativos hoje, mas válidos em D: uso histórico positivo sem reativação artificial.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/34).

## 8.5::TMP-010

**Procedimento/contexto literal:** aquisição tardia de fonte: histórico anterior deve indicar desconhecimento, não afirmar falso fato.

**Aceite literal:** aquisição tardia de fonte: histórico anterior deve indicar desconhecimento, não afirmar falso fato.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/35).

## 8.5::TMP-011

**Procedimento/contexto literal:** mudança excepcional de Gestão: escopo sobre fato pretérito segue autoridade **da data do fato**; testar CON-023 mecanismo técnico.

**Aceite literal:** mudança excepcional de Gestão: escopo sobre fato pretérito segue autoridade **da data do fato**; testar CON-023 mecanismo técnico.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/36).

## 8.5::TMP-012

**Procedimento/contexto literal:** calendário 2024/2023 e 29/02: nenhum comparativo inexistente; valores discriminantes.

**Aceite literal:** calendário 2024/2023 e 29/02: nenhum comparativo inexistente; valores discriminantes.

**Status:** PARCIAL.

**Limite:** DTO/SQL/reinício e XLSX com dez séries atuais discriminantes comprovados no recorte fechado03. Contexto do exportador construído fora da UI; a função produtiva, o writer OOXML e os DTOs reais não foram simulados. Sem jornada Chrome, renderização de gráficos, download por navegador ou PDF React nesta prova. watch:null foi solicitado, mas sua aplicação não foi comprovada. Excedente contratual permanece zero e não é coluna diária B–L. Tentativas anteriores interrompidas/falhas conservam seus resultados; não há aprovação integral do módulo ou ROP..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/37).

## 8.5::TMP-013

**Procedimento/contexto literal:** cliente `Pacific/Kiritimati` ou UTC+14 vs data administrativa São Paulo: API decide dia corretamente, evitando arredondamento UTC impróprio.

**Aceite literal:** cliente `Pacific/Kiritimati` ou UTC+14 vs data administrativa São Paulo: API decide dia corretamente, evitando arredondamento UTC impróprio.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** O corpo não configura cliente Pacific/Kiritimati/UTC+14 e não envia requisição API com esse fuso. A prova é adjacente de política, não execução do predicado completo do gate..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/38).

## 8.5::TMP-014

**Procedimento/contexto literal:** se alegar teste de atravessamento físico de meia-noite, usar processo isolado e prova de antes/no/depois; caso contrário registrar NÃO EXECUTADO, mesmo que TMP-001..003 passem com Clock de teste.

**Aceite literal:** se alegar teste de atravessamento físico de meia-noite, usar processo isolado e prova de antes/no/depois; caso contrário registrar NÃO EXECUTADO, mesmo que TMP-001..003 passem com Clock de teste.

**Status:** NÃO EXECUTADO.

**Limite:** Nenhum atravessamento físico de meia-noite;Clocktest não substitui este gate..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/39).

## 8.6::CON-001

**Procedimento/contexto literal:** empresa duplicada em dois POSTs simultâneos, CNPJ único; diferenciar falha de unicidade de erro HTTP e auditoria.

**Aceite literal:** empresa duplicada em dois POSTs simultâneos, CNPJ único; diferenciar falha de unicidade de erro HTTP e auditoria.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A barreira usa fixture HQL gerada por Hibernate e rollback; não são dois POSTs concorrentes de criação da mesma empresa. Não distingue, em dois POSTs simultâneos, o conflito final de unicidade, o erro HTTP correspondente e sua auditoria. Esse predicado permanece aberto..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/40).

## 8.6::CON-002

**Procedimento/contexto literal:** cadastro persistido com resposta perdida, recuperação pelo catálogo, nenhuma segunda empresa e só associação retomada.

**Aceite literal:** cadastro persistido com resposta perdida, recuperação pelo catálogo, nenhuma segunda empresa e só associação retomada.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A persistência e a perda de resposta são simuladas no adapter; não há commit SQL seguido de perda TCP real nessa evidência. Native de contrato/PROR/relatórios e preservação de Fornecedor não reproduz por si só a falha de resposta deste gate. Executar cenário HTTP/Chrome autenticado com fonte comprometida, catálogo real e associação retomada, preservando o controle de unicidade e as fotografias..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/41).

## 8.6::CON-003

**Procedimento/contexto literal:** publicação do contrato vs associação de empresa em mesmo rascunho; vitória legítima e conflito com versão sem mescla indevida.

**Aceite literal:** publicação do contrato vs associação de empresa em mesmo rascunho; vitória legítima e conflito com versão sem mescla indevida.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/42).

## 8.6::CON-004

**Procedimento/contexto literal:** dois aditamentos/previsões simultâneos e prévia expirada: sem fechamento de faixa incoerente.

**Aceite literal:** dois aditamentos/previsões simultâneos e prévia expirada: sem fechamento de faixa incoerente.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/43).

## 8.6::CON-005

**Procedimento/contexto literal:** dois movimentos de alocação com data efetiva concorrente: sem sobreposição, sem duplicar faixa atual.

**Aceite literal:** dois movimentos de alocação com data efetiva concorrente: sem sobreposição, sem duplicar faixa atual.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Não são dois movimentos de alocação na mesma data efetiva, nem há assertiva específica de uma única faixa atual de alocação. O acesso é por serviços com contexto de autenticação sintético; não por HTTP. A barreira de partida não é prova de espera física no lock..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/44).

## 8.6::CON-006

**Procedimento/contexto literal:** duas admissões/vínculos com reserva global da Pessoa: regra de unicidade ou concorrência do domínio aplicada atomicamente.

**Aceite literal:** duas admissões/vínculos com reserva global da Pessoa: regra de unicidade ou concorrência do domínio aplicada atomicamente.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/45).

## 8.6::CON-007

**Procedimento/contexto literal:** dois lançamentos de frequência com mesma chave: idempotência; com chaves diferentes: conflito específico, não duplicar o mesmo fato.

**Aceite literal:** dois lançamentos de frequência com mesma chave: idempotência; com chaves diferentes: conflito específico, não duplicar o mesmo fato.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** A segunda prova usa a mesma chave com outra intenção; não substitui o predicado de duas chaves diferentes concorrentes. A largada usa latch de threads; não se afirma espera física de lock ou captura de ordem TCP..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/46).

## 8.6::CON-008

**Procedimento/contexto literal:** lote de frequência com um elemento inválido/futuro: rollback conforme contrato; não deixar metade comprometida.

**Aceite literal:** lote de frequência com um elemento inválido/futuro: rollback conforme contrato; não deixar metade comprometida.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/47).

## 8.6::CON-009

**Procedimento/contexto literal:** ocorrência com anexo, rollback após falha de arquivo: metadados e arquivo final coerentes, sem órfãos.

**Aceite literal:** ocorrência com anexo, rollback após falha de arquivo: metadados e arquivo final coerentes, sem órfãos.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** O rollback é imposto explicitamente; não foi provocado por falha de I/O do arquivo. O oráculo não inventaria globalmente .staging, todos os metadados e todos os contextos. O nome do método não supre esses limites; não há comprovação de ausência universal de órfãos..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/48).

## 8.6::CON-010

**Procedimento/contexto literal:** duas retificações de documento/SEI: versão técnica, replay, conflitos e bytes de arquivo corretos.

**Aceite literal:** duas retificações de documento/SEI: versão técnica, replay, conflitos e bytes de arquivo corretos.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/49).

## 8.6::CON-011

**Procedimento/contexto literal:** revogação de perfil e sessão durante espera de lock/confirmar: revalidar autorização no momento exigido, zero mutação após revogação.

**Aceite literal:** revogação de perfil e sessão durante espera de lock/confirmar: revalidar autorização no momento exigido, zero mutação após revogação.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Os oráculos aceitam 401/403/404/409. Não são snapshots integrais de todas as colunas. Revogação direta do JWT/sessão durante o lock não é exercida nesses métodos; conta/perfil são as causas discriminadas. Sem navegação ou transporte TCP..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/50).

## 8.6::CON-012

**Procedimento/contexto literal:** fechamento em dois executores após restart: não concluir duas vezes, prefixo preservado, contadores auditáveis, id de job/tentativa estável.

**Aceite literal:** fechamento em dois executores após restart: não concluir duas vezes, prefixo preservado, contadores auditáveis, id de job/tentativa estável.

**Status:** PARCIAL.

**Limite:** UmaJVM própria de cada vez, executor observado1; reinício sequencial e preservação comprovados. Não foram executados dois executores ativos/cluster simultâneo. Variante500:umworker manual,mas consumidorautomático scheduling-1 teve2timeouts1205; sem segundoexecutor inferido nem aprovação de concorrência distribuída..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/51).

## 8.8::ARQ-001

**Procedimento/contexto literal:** download PDF/XLSX real, headers, magic bytes, extensão, MIME, hash, nome e conteúdo esperado.

**Aceite literal:** download PDF/XLSX real, headers, magic bytes, extensão, MIME, hash, nome e conteúdo esperado.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/52).

## 8.8::ARQ-002

**Procedimento/contexto literal:** arquivo PDF 41 registros em duas páginas; não declarar 85 se não gerou 85; contar páginas e linhas conforme fonte.

**Aceite literal:** arquivo PDF 41 registros em duas páginas; não declarar 85 se não gerou 85; contar páginas e linhas conforme fonte.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/53).

## 8.8::ARQ-003

**Procedimento/contexto literal:** Excel 29/02 com valores discriminantes e fórmulas/valores carregados, comparar com DTO de mesma requisição.

**Aceite literal:** Excel 29/02 com valores discriminantes e fórmulas/valores carregados, comparar com DTO de mesma requisição.

**Status:** PARCIAL_DTO_XLSX_PRODUTIVO; COLUNAS CONSTANTES/PDF/UI PENDENTES.

**Limite:** DTO/SQL/reinício e XLSX com dez séries atuais discriminantes comprovados no recorte fechado03. Contexto do exportador construído fora da UI; a função produtiva, o writer OOXML e os DTOs reais não foram simulados. Sem jornada Chrome, renderização de gráficos, download por navegador ou PDF React nesta prova. watch:null foi solicitado, mas sua aplicação não foi comprovada. Excedente contratual permanece zero e não é coluna diária B–L. Tentativas anteriores interrompidas/falhas conservam seus resultados; não há aprovação integral do módulo ou ROP..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/54).

## 8.8::ARQ-004

**Procedimento/contexto literal:** arquivo da pessoa A não pode ser servido sob sessão B nem após revogação de permissão.

**Aceite literal:** arquivo da pessoa A não pode ser servido sob sessão B nem após revogação de permissão.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/55).

## 8.8::ARQ-005

**Procedimento/contexto literal:** documentos SEI com upload/download, retificação com anexo não vazio e checksum idêntico.

**Aceite literal:** documentos SEI com upload/download, retificação com anexo não vazio e checksum idêntico.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/56).

## 8.8::ARQ-006

**Procedimento/contexto literal:** PDF de relatório longo: colunas não cortadas, fonte legível, cabeçalhos, páginas, quebras; alcance PDF/UA declarado separadamente.

**Aceite literal:** PDF de relatório longo: colunas não cortadas, fonte legível, cabeçalhos, páginas, quebras; alcance PDF/UA declarado separadamente.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/57).

## 8.8::ARQ-007

**Procedimento/contexto literal:** cancelamento de exportação/erro intermediário não marca job como concluído nem disponibiliza arquivo inválido.

**Aceite literal:** cancelamento de exportação/erro intermediário não marca job como concluído nem disponibiliza arquivo inválido.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Não há cancelamento público de exportação nem a mesma falha de I/O no fluxo integrado. A alteração de autorização é fixture JPA explícita. A comparação integrada enumera caminhos no diretório observado; não prova ausência global de todos os resíduos nem comportamento visual..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/58).

## 8.8::ARQ-008

**Procedimento/contexto literal:** respostas fora de ordem não trocam relatório/período ao baixar.

**Aceite literal:** respostas fora de ordem não trocam relatório/período ao baixar.

**Status:** PARCIAL_COM_VINCULO_ESPECIFICO_POR_CAMADA.

**Limite:** Não houve resposta real fora de ordem seguida de download no navegador; os arquivos reais da Native e writer36 são caminhos concluídos normalmente. O caso de respostaB tardia verifica contexto e botão habilitado; não realiza por si um download físico após essa liberação. PDF41 e XLSX reais confirmam conteúdo normal e reinício em outros gates; não substituem a corrida desta aceitação. Chrome bloqueado; não aprovar ARQ-008 integralmente pelo conjunto de unidades..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/59).

## 8.9::RST-001

**Procedimento/contexto literal:** preservar baseline sem modificá-la; derivar banco sintético **8.0.45** com entidades Hibernate.

**Aceite literal:** preservar baseline sem modificá-la; derivar banco sintético **8.0.45** com entidades Hibernate.

**Status:** PARCIAL.

**Limite:** 72tabelas produzidas porHibernateH02na45,223linhas preservadas no upgrade; tabelasfinanceiras/riscoausentes não recebem prova material retroativa..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/60).

## 8.9::RST-002

**Procedimento/contexto literal:** inicialização nova cria/atualiza apenas via JPA; todas as FKs/restrições relevantes verificadas por leitura `information_schema`.

**Aceite literal:** inicialização nova cria/atualiza apenas via JPA; todas as FKs/restrições relevantes verificadas por leitura `information_schema`.

**Status:** PARCIAL.

**Limite:** 72tabelas produzidas porHibernateH02na45,223linhas preservadas no upgrade; tabelasfinanceiras/riscoausentes não recebem prova material retroativa..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/61).

## 8.9::RST-003

**Procedimento/contexto literal:** boot repetido `ddl-auto=update` sem dano, sem recriar dados/fatos ou apagar arquivo.

**Aceite literal:** boot repetido `ddl-auto=update` sem dano, sem recriar dados/fatos ou apagar arquivo.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/62).

## 8.9::RST-004

**Procedimento/contexto literal:** parada graciosa; snapshots de tabelas, linhas, índices, anexos, jobs e SHA antes/depois, mesmo JAR/env/UUID MySQL.

**Aceite literal:** parada graciosa; snapshots de tabelas, linhas, índices, anexos, jobs e SHA antes/depois, mesmo JAR/env/UUID MySQL.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/63).

## 8.9::RST-005

**Procedimento/contexto literal:** falha controlada de JVM própria com commit parcial legítimo; recuperação automática e preservação prefixo já persistido.

**Aceite literal:** falha controlada de JVM própria com commit parcial legítimo; recuperação automática e preservação prefixo já persistido.

**Status:** PARCIAL.

**Limite:** SIGKILL da aplicação própria com4linhas confirmadas→200mesmojob/tentativa,lease300/semPOST. Não quedaMySQL nemcluster..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/64).

## 8.9::RST-006

**Procedimento/contexto literal:** Redis cache legado contém payload antiga rota removida; autenticação 404 e sem DEL/FLUSH indevido.

**Aceite literal:** Redis cache legado contém payload antiga rota removida; autenticação 404 e sem DEL/FLUSH indevido.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/65).

## 8.9::RST-007

**Procedimento/contexto literal:** upgrade funcional de esquema legado **produzido pelo Hibernate da versão de origem na mesma instância 8.0.45**, preservando dados, sem `schema.sql` ou scripts DDL.

**Aceite literal:** upgrade funcional de esquema legado **produzido pelo Hibernate da versão de origem na mesma instância 8.0.45**, preservando dados, sem `schema.sql` ou scripts DDL.

**Status:** PARCIAL.

**Limite:** 72tabelas produzidas porHibernateH02na45,223linhas preservadas no upgrade; tabelasfinanceiras/riscoausentes não recebem prova material retroativa..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/66).

## 8.9::RST-008

**Procedimento/contexto literal:** exportações, login e documentos funcionam após reinício; versões exibidas e hashes conferidos.

**Aceite literal:** exportações, login e documentos funcionam após reinício; versões exibidas e hashes conferidos.

**Status:** PARCIAL.

**Limite:** Somente aspectos e camadas vinculados; ler literal integral, limitações e índices. Sem aprovação por título/contagem..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/67).

## 8.10::PERF-001

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/68).

## 8.10::PERF-002

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/69).

## 8.10::PERF-003

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/70).

## 8.10::PERF-004

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/71).

## 8.10::PERF-005

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/72).

## 8.10::PERF-006

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/73).

## 8.10::PERF-007

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/74).

## 8.10::PERF-008

**Procedimento/contexto literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Aceite literal:** O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

**Status:** NÃO EXECUTADO NO CORPUS NACIONAL EQUIVALENTE.

**Limite:** 8posições definidas por um parágrafo comum, sem8predicados inventados. CargaJPA,corpusHTTP41/200 e controles200 variantes1/500 não são corpus nacional equivalente. Tempos149,162/361,557s instrumentados;2timeouts reais do consumidor automático no500 preservados..

**Vínculos:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_76_GATES_TRANSVERSAIS_NOVA_DADOS.json#/gates/75).

