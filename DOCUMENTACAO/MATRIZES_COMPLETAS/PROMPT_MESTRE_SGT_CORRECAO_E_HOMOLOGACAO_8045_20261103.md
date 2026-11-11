# PROMPT MESTRE — CORREÇÃO EFETIVA, REGRESSÃO E HOMOLOGAÇÃO INDEPENDENTE INTEGRAL DO SGT

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

**Projeto:** SGT — Sistema de Gestão Contratual e Operacional de Trabalhadores Terceirizados  
**Baseline comprovada:** C25-H02, build04, reprovada para aceite integral  
**Continuação de trabalho:** checkpoint H03 avulso, NÃO homologado  
**Ambiente obrigatório de nova homologação:** **MySQL Server 8.0.45 EXATAMENTE**, Hibernate/JPA, Redis real, Java/Spring Boot, React e Google Chrome real/Playwright  
**Missão:** IMPLEMENTAR, EXECUTAR, TESTAR, CORRIGIR, HOMOLOGAR, REVISAR E ENTREGAR; não produzir apenas um plano, relatório ou outro prompt.  
**Data de referência do pedido:** 03/11/2026.

> ## REGRA ABSOLUTA E INEGOCIÁVEL
>
> Em **TODOS** os testes de banco, persistência, integração, concorrência, HTTP, E2E, upgrade, reinício, jobs e homologação da nova derivada, utilizar **somente MySQL Server 8.0.45 real**, comprovado por executável, conexão e JDBC. MySQL 8.0.46 e MySQL 8.4 aparecem em documentos antigos porque foram usados historicamente; isso **não** autoriza seu uso na nova campanha. Proibido trocar por 8.0.46/8.4, MariaDB, H2 ou banco simulado. Se a execução real 8.0.45 estiver impedida, declarar **BLOQUEADO POR AMBIENTE** para os gates afetados. Não alterar um teste para fingir que 8.0.45 foi utilizado.
>
> O schema/tabelas da aplicação são geridos **exclusivamente pelo Hibernate/JPA**, via `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`. **Não criar DDL manual, `schema.sql`, `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`, scripts de migração, Flyway ou Liquibase**. Modelar tipos, chaves, constraints, índices e relações por anotações nas entidades. Consultas SQL **somente leitura** podem ser utilizadas como contraprova de persistência e integridade.
>
> **Nada é HOMOLOGADO apenas por compilar, passar testes unitários ou ter HTTP 200.** Todo defeito corrigido exige RED documentado, GREEN reexecutado, persistência verificada, regressão transversal, caso real de navegador quando aplicável, e repetição no **JAR e nos assets React finais identificados por SHA-256**.

---

## 0. Identidade do trabalho, autoridade das fontes e limites

Aja como uma equipe de especialistas distintos nas seguintes frentes: engenharia Java 17/Spring Boot/Hibernate, React/Vite/JSX, domínio contratual-administrativo, qualidade de dados/temporalidade, segurança, banco de dados MySQL, Redis/jobs, QA/E2E Chrome/Playwright, acessibilidade, desempenho e auditoria independente. Se puder usar agentes separados, segregar implementação e revisão; registrar todos os agentes, modelos/versões quando expostos, decisões, comandos e logs. Se só houver uma execução, separar formalmente revisão adversarial de construção e **não alegar independência organizacional inexistente**.

A tarefa NÃO é redesenhar o SGT, expandir o escopo de negócio, trocar tecnologias, adotar banco alternativo, introduzir migração, remover funções intactas ou aprovar resultados históricos por conveniência. O SGT está **em desenvolvimento** e deve continuar centrado em **contratos e trabalhadores terceirizados**. Não implementar nesta campanha pagamento, fator gerador, faturamento, glosas, ajustes de faturamento, atestes financeiros ou execução de pagamentos: são trabalhos futuros e não constituem o módulo financeiro removido. O cadastro operacional **Fornecedor** é obrigatório e deve ser preservado; não confundir com funções exclusivas antigas de Governança de Terceiros/risco.

### Ordem de precedência para conflitos

1. Esta solicitação: MySQL **8.0.45 exato**, Hibernate/JPA como único gestor de DDL, escopo operacional preservado, correções testadas antes do aceite.
2. Contratos de negócio efetivamente expressos nas fontes Java mais recentes e documentação funcional corroborada; não criar regras ad hoc.
3. C25-H02 build04 como baseline **completa**; H03 como material de correção **não homologado**, sem promover patches automaticamente.
4. Matrizes de requisitos, contratos HTTP e evidências históricas; quando divergirem do código, documentar a divergência e resolver pela regra substantiva do domínio, sem afrouxar oráculos.
5. Relatórios anteriores como **pistas e baseline de regressão**, nunca como provas da nova candidata ou do MySQL 8.0.45.

Se houver contradição objetiva de regra que não possa ser decidida por fonte primária, bloquear **somente o ato contraditório**, registrar duas interpretações e ainda assim executar o restante. Não recorrer a perguntas repetidas por dados já presentes na Biblioteca.

## 1. Localização EXATA da baseline, material de continuidade e identidade binária

**C25-H02 final build04**, candidato completo mais novo encontrado, **reprovado** na última homologação integral. Os identificadores internos Java `0.0.4-operacional-20261101` e React `1.4.4-operacional.20261101` se repetem entre derivações; identificar SEMPRE pelo SHA-256.

**Diretório de fontes/JAR H02:**

`/Projetos/SGT/08_HOMOLOGACAO_INDEPENDENTE_C25_H01_CHROME_20261102/05_DERIVADA_SE_NECESSARIA/`

| Nome exato na Biblioteca | SHA-256 obrigatório |
|---|---|
| `SGT_JAVA_0.0.4_C25_H02_FONTES_DA_DERIVADA.zip` | `0e9e10dbede2b6fde0e4ea18da1b06c14a1b03709c46330a9dae5dc4ee960ff5` |
| `SGT_REACT_1.4.4_C25_H02_FONTES_DA_DERIVADA.zip` | `e019b00e5c836ff78d1c6c365cfe3c847b25e98f065c6c280ce4fc7b7dd81a0a` |
| `SGT_REACT_1.4.4_C25_H02_BUILD_PRODUCAO.zip` | `0df5f00ed8338e2d458242fd48112767cce2d7f965c209e64b44fea471ecef1d` |
| `sgt-backend-0.0.4-operacional-20261101-c25-h02.jar` | `2e90a6c7ad7949baf86a305178c391352c29562c6035bf04b7e0fd07e67c9ef1` |

**H03 CHECKPOINT, NÃO HOMOLOGADO:**

`/Projetos/SGT/08_HOMOLOGACAO_INDEPENDENTE_C25_H01_CHROME_20261102/05_DERIVADA_SE_NECESSARIA/H03_CHECKPOINT_NAO_HOMOLOGADO/`

Seis arquivos identificados: `apply_h03.py`, `src_core_security_geracaoSessao.js`, `src_modules_auth_context.jsx`, `src_PainelAlteracoesContrato_index.jsx`, `test_h03-fronteira-abas.test.js`, `test_h03-prorrogacao-campo.test.jsx`. O script de patch usa diretório temporário de uma sessão anterior: **NÃO executá-lo sem inspeção e sem adaptar os caminhos em cópia de trabalho**. Conferir origem, diffs, imports, testes e efeitos colaterais; integrar apenas após RED/GREEN.

**Advertência de baseline:** `/Projetos/SGT/01_VERSAO_ATUAL/` contém uma geração antiga (`0.0.2`/React `1.4.2`). **Não selecionar por nome**. H01 e C25 anteriores são referências históricas, não destino de alterações. Não modificar pacote anterior, não forjar um build04 como se fosse H03.

### Passos obrigatórios antes de alterar um byte de produção

1. Listar a Biblioteca nos caminhos acima e verificar que existem os quatro artefatos H02 e os seis H03; registrar `file_id`, caminho, tamanho, data e origem.
2. Copiar apenas para área derivada, extrair ZIP/JAR em diretórios limpos e seguros; verificar ZIP CRC, segurança dos nomes, ausência de caminhos de travessia, quantidade/identidade de entradas, pacote de assets e estrutura do código.
3. Calcular SHA-256 **novamente em bytes brutos** e confrontar com a tabela. Anotar qualquer discordância como falha de identidade, não ignorá-la.
4. Levantar Java `pom.xml`, plugins Surefire/Failsafe/Jacoco, `application*.properties/yml`, perfil de integração, entidades, controladores, DTOs, serviços, repositórios, interceptors, schedulers e interfaces React. Não presumir caminho de pacote diferente da fonte real.
5. Registrar os hashes dos **arquivos de produção usados no build**, do JAR, dos assets React incorporados e da configuração pública. Identificar processo/porta efetivos de cada execução.
6. Criar `BASELINE_CONGELADA_H02.md`, `CHECKPOINT_00_IDENTIDADE.*`, `INVENTARIO_COMPONENTES.json` e `MANIFESTO_SHA256_BASELINE.txt` com identificação explícita de H03 ainda não homologada.

## 2. Documentos canônicos — leitura e cruzamento obrigatório

**Campanha anterior e relatórios finais:**

`/Projetos/SGT/08_HOMOLOGACAO_INDEPENDENTE_C25_H01_CHROME_20261102/04_RESULTADO_E_MANIFESTOS/`

Ler integralmente, e não apenas os parágrafos de conclusão: `LEIA_PRIMEIRO_ENTREGA.md`, `RELATORIO_HOMOLOGACAO_INDEPENDENTE_BROWSER_REAL.md`, `LIMITES_E_PENDENCIAS.md`, `MATRIZ_GATES_E_EVIDENCIAS.md`/`.json`, `MATRIZ_198_635_COM_PROVAS.md`/`.json`, `MATRIZ_64_ROP_NAVEGADOR.md`/`.json`, `MAPA_MODULOS_REMOCAO_PRESERVACAO.md`/`.json`, `RELATORIO_E2E_CHROME.md`/`.json`, `RELATORIO_MYSQL_REDIS.md`, `RELATORIO_CONCORRENCIA.md`, `RELATORIO_SEGURANCA.md`, `RELATORIO_DESEMPENHO.md`, `RELATORIO_EXPORTACOES.md`, `RELATORIO_UPGRADE_REINICIO.md`, `PARECER_REVISAO_INDEPENDENTE.md`, inventário e hashes finais.

**Defeitos H02:**

`/Projetos/SGT/08_HOMOLOGACAO_INDEPENDENTE_C25_H01_CHROME_20261102/03_DEFEITOS_E_REVISAO/MATRIZ_DEFEITOS_RED_GREEN_FINAL.md`

**Mapa funcional de 21 módulos e regras:**

`/Projetos/SGT/08_HOMOLOGACAO_INDEPENDENTE_C25_H01_CHROME_20261102/01_INVENTARIO_FUNCIONAL/MAPA_MODULOS_E_REGRAS_NEGOCIO_C25_H01.md`

**Matriz operacional C25, com dados iniciais/ação/oráculo/endpoint dos 64 ROP:**

`/Projetos/SGT/06_CANDIDATA_CORRIGIDA_C25_0_0_4_1_4_4_MYSQL8046_20261102/C25_MATRIZ_REGRAS_TESTES_OPERACIONAIS.md`

**Outras fontes:** `C25_MATRIZ_REQUISITOS_FINAL.md`, `C25_MATRIZ_ENDPOINTS.md`, `C25_MATRIZ_REMOCAO_PRESERVACAO.md`, histórico de homologação em `07_HOMOLOGACAO_INDEPENDENTE_C25_20261102/`, checkpoint e diffs H02. Diferenciar regras contratuais aceitas de comentários auxiliares anteriores. A menção a 8.0.46 em **nomes históricos de pasta** não altera a exigência de 8.0.45 para a nova derivada.

### Método de leitura e rastreabilidade

Para cada requisito ou ROP, registrar: `ID original`, texto integral, origem exata, módulo, regra, código Java, endpoint, DTO, estado JPA, componente React, teste unitário, integração 8.0.45, HTTP/TCP, Chrome, persistência, documento autêntico, status anterior, status NOVO, explicação e `SHA-256` da prova. Não converter `test pass` em cumprimento integral do critério sem verificar **todos os predicados**. Critério que depende de documento autêntico não vira tecnicamente aprovado por ter sido simulado.

## 3. Dependências na Biblioteca: inventariar ANTES de instalar ou pedir download

Raiz: `/Projetos/SGT/dependencia/`; verificar `Proprias/` e `Copiadas_SGE/` recursivamente. Dependências já localizadas, sujeitas a verificação de conteúdo, integridade, ABI e versão:

| Categoria | Evidência de arquivo na Biblioteca | Verificação concreta |
|---|---|---|
| **MySQL 8.0.45** | `Proprias/mysql-community-server-core_8.0.45-1debian12_amd64 (1).deb` | SHA-256 `976cfca59a127d12f86f86aab7eac60e5aae9bfa5c974299565ee60873b364aa`; pacote `mysql-community-server-core`, versão `8.0.45-1debian12`, `amd64`. Não confundir `server-core` com instalação completa. |
| Bibliotecas MySQL | `Proprias/libaio.so.1`, `Copiadas_SGE/Outras/libaio.so.1` | Comprovar `ldd`, ABI, dependências restantes, permissões e inicialização real. |
| Redis + Playwright | `Proprias/SGT_COMPLEMENTO_RUNTIME_REDIS_PLAYWRIGHT_20261101.zip` | Inspecionar binários, versões, hashes e testes de conexão real. |
| Chrome | `Copiadas_SGE/Navegadores/chrome-linux64.zip`, `Proprias/SGT_REACT_COMPLETO_COM_CHROME_20261101T024306Z.zip` | Versão real do Google Chrome fornecido, `--version`, execução headless/headed, CDP e Playwright; não apenas asset HTTP. |
| Node/React | `Proprias/SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip`, `Copiadas_SGE/Node-e-React/node-v22.16.0-linux-x64.tar.xz`, `node_modules-linux-node22(SGE).tar.gz`, `npm_cache_SGE.tar.gz` | Conferir lockfile, Vite/Vitest/React instalados, não substituí-los por versões aleatórias. |
| Java/Maven | `Copiadas_SGE/Java-e-Spring/`, `Copiadas_SGE/Outras/apache-maven-3.9.16-bin.zip`, caches Maven | Usar JDK compatível `--release 17`, confirmar Maven e dependências efetivas, plugin de cobertura. |

O `.deb` fornecido é **Debian 12 AMD64**. Suas dependências de pacote incluem `libaio1`, `libc6`, `libgcc-s1`, `libmecab2`, `libnuma1`, `libssl3`, `libstdc++6`, `libtirpc3` e `zlib1g` em versões compatíveis. Resolver somente com componentes adequados e autorizados. **Não** tentar instalar `.deb` em Windows/Fedora como se fosse instalador nativo; pode usar execução isolada compatível, sem tocar no MySQL instalado no host. Não reutilizar datadir de servidor mais novo por downgrade físico. Se existir outro binário oficial **8.0.45 exatamente** previamente disponível, verificar proveniência, SHA e equivalência antes de utilizá-lo.

### Prova tripla de versão do MySQL — antes e durante a homologação

Registrar, sem credenciais ou segredos:

1. Binário/serviço real: `mysqld --version` ou equivalente do **processo em execução**, caminho resolvido, PID, imagem/container com digest quando houver.
2. Conexão real: `SELECT VERSION(), @@version, @@version_comment, @@server_uuid, @@port, CURRENT_USER()` **somente leitura** no servidor efetivamente utilizado pela aplicação. Conferir `8.0.45` e identidade única da instância de homologação.
3. Driver: `DatabaseMetaData.getDatabaseProductName()` e `getDatabaseProductVersion()` pela **mesma DataSource** usada em testes/HTTP/JAR, com versão `8.0.45` e produto MySQL.
4. Se usar Testcontainers, inspecionar imagem/tag/digest e confirmar o `SELECT VERSION()` por conexão **dentro do teste**; tag `mysql:8.0.45` por si só não encerra a prova.
5. Em cada `receipt.json` ou início de suíte, incluir `mysql_expected=8.0.45`, `mysql_observed`, `server_uuid`, driver/JAR/React hash, processo e porta. A guarda deve **falhar fechado** em qualquer versão diferente.
6. Verificar que HTTP/Chrome do aplicativo, Java IT e os containers se conectam às **instâncias esperadas**, não à porta ou MySQL padrão de uma sessão anterior. Se houver instâncias isoladas distintas para suites, cada uma precisa ser 8.0.45.

### Remoção da dependência de testes 8.0.46 sem enfraquecimento

Inventariar todas as referências literais de versão em fontes, testes, Testcontainers, scripts, Docker/Compose, properties/YAML, fixtures, arquivos de build e documentação **de execução atual**. Exemplos históricos a procurar nas fontes H02: `MySqlIntegrationTestSupport.java`, `AuditoriaHttpSuporte.java`, `MySql8046AmbienteHttpIT.java`, `TokenRevogacaoConcorrenciaH02IT.java`, `PreparacaoMapaParcialTcpIT.java`; confirmar cada caminho pela árvore real. Trocar **somente as exigências operacionais do banco** e os nomes descritivos dependentes da versão quando justificado; nunca editar assertions do domínio para forçar aprovação. Manter os relatórios antigos imutáveis, pois eles realmente usaram 8.0.46. Antes/depois: diff, linha, motivo, teste RED no 8.0.45, GREEN, guarda de recusa para 8.0.46 e comprovação JDBC. O sistema só pode depender de recursos e SQL válidos em 8.0.45; investigar qualquer incompatibilidade real, não ocultá-la.

## 4. Ambiente seguro, orçamento e semântica de testes

- **Java:** confirmar versão efetiva (baseline anterior compilada com Java 17), Maven, locale, encoding UTF-8 e fuso `America/Sao_Paulo`; preservar compatibilidade do JAR e da configuração aprovada. Não migrar Spring Boot, Hibernate, Vite ou Java por iniciativa própria.
- **MySQL 8.0.45 real:** instância/datadir/porta isolados, conta de aplicação com privilégios necessários e restritos, sem root da aplicação. Hibernate/JPA `ddl-auto=update`; `spring.sql.init.mode=never`; sem DDL manual. Nenhuma ação destrutiva sobre base real.
- **Redis real:** instância/namespace/bases isoladas por campanha; testar estado de token, cache e filas; não usar cache fake nem `FLUSHALL` em ambiente compartilhado.
- **Chrome:** usar o navegador da Biblioteca com Playwright/CDP; servir o mesmo bundle React que está dentro do JAR final. Registrar `browser.version()`, executável/hash, logs de console, requests, responses, erros e screenshots. Não equiparar jsdom a E2E nem GET 200 de HTML a renderização.
- **Recursos:** detectar `cgroup/memory.max`, cores/quota, memória livre/heap; a campanha anterior tinha cgroup de 8 GiB (independentemente da memória da estação). Definir orçamento atualizado, `-Xmx`, metaspace, limite de forks Surefire/Failsafe, `maxWorkers=1`/sem paralelismo para Vitest quando necessário, contextos Playwright limitados. Maven pesado, Node pesado, navegador pesado e MySQL devem ser coordenados, com logs de pico de memória. Nenhuma falha OOM pode ser reclassificada como comportamento aprovado.
- **Dados:** somente pessoas, empresas, contratos, CPF/CNPJ e documentos **sintéticos**, identificados como tal. Não publicar CPF real, senha, cookie, token ou QR/seed 2FA. Acesso a documentos autênticos, quando houver, requer autorização de leitura e tratamento de dados.
- **Tempo:** testes de fronteira podem usar `Clock` injetado para Java e ambiente isolado explicitamente controlado; **não** rebatizar relógio mockado como travessia real do sistema nem alterar o relógio do host compartilhado. Distinção precisa entre teste de limite lógico, teste E2E cliente sob fuso alternativo e processo/JVM realmente atravessando o instante.
- **Evidência de persistência:** gerar snapshots e consultas **somente SELECT** antes/depois, sob transação de leitura, sem corrigir dados por SQL manual. Conferir também reabertura após reinício sem alteração da configuração ou reinicialização indevida da base.

## 5. Escopo NEGATIVO: preservar remoção efetiva da Governança

As funções exclusivas de avaliação de risco, score/pesos/níveis, monitoramento contínuo de fornecedores, cadastro de processos exclusivos de onboarding/offboarding de terceiros, rotas/UI/jobs/DTOs/relatórios exclusivamente financeiros ou de risco foram removidas de propósito. Inspecionar dependências reversas no Java, React, permissões e Redis legado. **Não restaurar código, endpoints ou menus exclusivos**, nem implementar pagamentos/faturamento agora. URLs removidas devem permanecer 404 para usuário devidamente autenticado, inclusive quando houver cache Redis legado `200/HIT`. A funcionalidade de fornecedor operacional não deve retornar 404.

A existência de uma coluna física antiga como `monitoramento_status NOT NULL` ou campo privado de compatibilidade em `Fornecedor` pode ser necessária para preservar dados legados; não promover isso a função ativa e **não apagar/alterar tabela manualmente** para parecer que a remoção foi completa. Não remover `Fornecedor`, histórico, GOV-003, contratos, instrumentos, recontratação, desligamento normal, auditoria ou documentos. Exigir prova por inventário de classes/entidades/controladores/React e reexecução de rotas.

---

## 6. HOMOLOGAÇÃO FUNCIONAL POR MÓDULO: 21 cadernos de teste completos

Os cenários abaixo são **obrigações de investigação e teste**. Sua execução deve respeitar os endpoints, perfis e detalhes efetivos da implementação; se o nome de uma rota divergir, mapear para a rota real em `MATRIZ_ENDPOINTS_NOVA` em vez de criar endpoint fictício. Cada caderno deve conter provas positivas, negativas, fronteiras temporais, concorrência e persistência quando aplicável. Os roteiros ROP-001 a ROP-064 com **dados iniciais, ação, resultado esperado e endpoints históricos** aparecem completos no **Anexo A** deste prompt e devem ser estendidos, não ignorados.

### M01 — Empresas contratadas / Fornecedor — ROP-001, 002, 003, 049

**Objeto:** cadastro operacional legítimo de empresa terceirizada; identidade normalizada; associação contratual em rascunho; consulta histórica e inativação sem apagar fatos.

**Teste de ponta a ponta:** entrar como Gestor Local autorizado; cadastrar CNPJ sintético e razão social/e-mail válidos, conferir resposta e GET; atualizar e reconsultar; inativar e reativar preservando ID; criar contrato rascunho e associar o cadastro; publicar e tentar nova associação, esperando **409** sem alteração de fornecedor/versão/auditoria imprópria; conferir ligação original. Em contrato anteriormente publicado, editar apenas descrição de um **outro** rascunho e confirmar que `fornecedorId` não virou `null` por efeito colateral.

**Negativos/segurança:** CNPJ inválido/duplicado e e-mail inválido; perfil sem escopo; dois usuários tentando duplicar a empresa; espera de lock com revogação de autorização antes do commit; resposta de cadastro perdida após persistência e repetição da intenção (RI03). Conferir não duplicação, eventual 409, mensagem explícita e recuperação por seleção da empresa existente, reenviando **somente** associação legítima.

**Histórico:** alterar situação/razão atuais e comparar fotografia da publicação anterior. **Proibido** recriar campos de risco, score e onboarding financeiro. A URL operacional de Fornecedor deve permanecer ativa mesmo com URLs de governança removidas.

### M02 — Estrutura organizacional e Gestão Local — ROP-032, 037, 050

**Objeto:** Superintendência, Gestão Local, escopos territorial/administrativo e autoridade temporal efetiva.

**Casos:** criar/consultar gestores e recortes autorizados; trocar Gestão Local em data efetiva e provar acessos antes/na data/depois; consultar o mesmo contrato/ocorrência por usuários de Gestão antiga e nova, considerando **responsável no dia do fato**; revogar permissão entre geração de prévia e confirmação; testar usuário externo ao recorte (403, zero efeito). Transferência excepcional deve requerer ato, fundamento, documentação, versão e trilha; sem documentação autêntica, classificar o mecanismo técnico separadamente de CON-023 documental.

### M03 — Catálogos: tipo, cargo, lotação, turno, jornada, situação e motivo — ROP-005, 029, 030

**Casos:** registrar catálogos válidos por Gestão; normalizar códigos/nomes/municípios; impedir duplicidade e combinações não ofertadas; validar inativação, reativação e validade temporal com cadastro já usado; opção inativa não deve ser selecionável para fato novo, mas deve continuar visível na fotografia histórica; alternar filtros rapidamente e impedir retorno antigo de repor opções desatualizadas; testar 401/403/409 e erros de catálogo depois de sucesso parcial do carregamento principal.

**Prova:** snapshots com data de disponibilidade e conhecimento; zero/falso/null não podem perder semântica; CAT-004 e CAT-007 permanecem bloqueados quanto à autenticidade até apresentação de fontes reais.

### M04 — Contrato: rascunho, publicação, vigência, anulação, inativação, encerramento — ROP-004, 003, 050

**Casos:** número+ano únicos; criar rascunho com empresa, tipo, instrumento, datas, fonte e composição; editar descrição sem perder fornecedor; tentar publicar com informação faltante e comprovar recusa sem publicação parcial; publicar com **prévia real** e conferir fotografia original; tentar edição livre da publicação (recusa); executar atos permitidos de encerramento/inativação/anulação com motivo, data efetiva e trilha; testar autorização e versão concorrente antes de confirmar. Alterações em vigência e empresa devem produzir atos formais próprios, não `PUT` silencioso.

**Temporalidade:** consultar antes/na/depois da vigência e após atos retroativos, distinguindo data de efeito de data de conhecimento. Dois operadores publicando simultaneamente não podem criar duas publicações nem perder versões. Testar reinício mantendo contratos/documentos/auditoria.

### M05 — Composição contratual e quantitativos — ROP-004, 009, 050

**Regra cardinal:** composições explícitas por **Lotação + Turno + Cargo**; a soma das quantidades deve igualar o efetivo pactuado; não gerar produto cartesiano nem duplicidade vigente.

**Massa:** publicar contrato original com quantitativo 100 e distribuições reais; reduzir para 75 (**limite ordinário permitido**) e tentar 74 (**negado na via ordinária**) mesmo após aumento intermediário para 120; verificar que o piso permanece 75% de **100 originalmente publicados**, e não de 120. Uma redução excepcional abaixo do piso só pode ocorrer com fundamento, fonte e autorização específica de Gestor Local nas condições do contrato. Provar valores anteriores/novos, arredondamentos quando aplicáveis e ausência de atualização silenciosa dos trabalhadores.

**Excedente:** compor 2 postos, alocar 3 vínculos/presenças parciais e verificar visualização de **150%** onde o indicador representa excedente; texto/DTO devem conservar 150% mesmo quando a barra visual se limita a 100%. Excedente exige aviso, justificativa e auditoria, mas **não inventar teto proibitivo** se não existe na regra.

### M06 — Aditamentos e prorrogação comum — ROP-004, 050; defeito PROR01

**Fluxo completo:** contrato publicado com data final conhecida → abrir aditamento normal → preencher identificação, justificativa, data efetiva, data documental/assinatura, **`novaDataFim`** de prorrogação, fonte e componentes exigidos → obter prévia → alterar dado depois da prévia para testar invalidação → obter nova prévia → confirmar com token atual e usuário autorizado → GET do contrato e da versão histórica → reiniciar JAR e reconferir estado. O formulário deve mostrar controle acessível de nova data final e o payload deve preservar a diferença entre `dataEfetiva` e `novaDataFim`.

**Negativos:** data final anterior à permitida/incompatível com vigência, campos ausentes, datas em fuso civil distinto, confirmação repetida, dupla confirmação concorrente, autorização revogada entre prévia/confirmar, documento ausente quando obrigatório, versão 409 e conexão perdida após commit. O erro deve manter formulário preenchido, informar conflito e não enviar ato duplicado. **Não confundir prorrogação com mera troca de data de ciclo de vida.**

### M07 — Remanejamento de composições — ROP-009, 050

**Regra:** redistribuir quantidade entre composições **existentes**, com variações cuja soma seja zero, sem alterar o efetivo global. Testar remanejamento válido, origem insuficiente, destino inexistente, duplicação de faixa, data fora da vigência, sobreposição temporal, assinatura documental e autorização. Reconstruir a alocação anterior; não movimentar empregados automaticamente como efeito implícito do remanejamento; vigência e histórico devem refletir datas efetivas.

### M08 — Pessoas e Empregados — ROP-006, 007, 025, 029

**Regra:** Pessoa é identidade permanente; Empregado é projeção operacional; Vínculo é relação temporal separada. Testar CPF sintético válido, unicidade, nome atual, alteração histórica, homônimos, nome social quando previsto, dados inconsistentes e validações. Selecionar o vínculo por identificação inequívoca em frequência e ocorrências: IDs, matrícula e contrato, não somente nome. Alterar nome atual não altera a fotografia jurídica do fato pretérito. Testar permissões para consulta de dados pessoais e minimização do DTO. COL-010 documental exige fonte autorizada de nomes anteriores.

### M09 — Vínculos contratuais — ROP-006, 007, 008, 025–028, 031, 048

**Fluxos:** admitir Pessoa em contrato válido com matrícula e data; programar admissão futura e impedir atos prematuros; desligar, cancelar desligamento e recontratar preservando histórico; tentar matrícula duplicada e associação a contrato inativo; duas inclusões simultâneas de uma Pessoa sob regras de reserva global; consulta de vínculo fora da validade; recontratação deve criar vínculo distinto sem duplicar Pessoa. Testar fronteiras inclusivas/exclusivas definidas no contrato, autorização, cadastros inativos e idempotência de POST/PUT. Não perder ocorrências/atestado/frequência do vínculo desligado retroativamente.

### M10 — Alocações e movimentações temporais — ROP-009, 027–032

**Fluxos:** mover trabalhador entre Lotação, Turno e Cargo em data de efeito, fechar faixa anterior sem lacuna/sobreposição indevida; garantir composição existente e pertencente ao contrato; verificar pessoa/contrato/cargo corretos para cada data; cancelar movimento e reconstruir estado sem adulterar histórico. Testar turno noturno, mudanças do dia seguinte e permissões por Gestão vigentes no dia. Operação com alocação futura, cancelada ou estranha ao vínculo deve ser recusada. Excedente deve ser auditado, sem fabricação de proibição genérica.

### M11 — Calendário administrativo e jornada — ROP-005, 017, 018, 030

**Casos:** validar jornadas semanais, feriados, eventos, escalas, vigência e hora local; fevereiro bissexto 2024 com 29/02; ano comum de comparação 2023; turno noturno que começa em D e termina em D+1; determinar pertencimento do lançamento ao **início da jornada**. Mudança de turno/cargo/município no dia seguinte não pode reclassificar os fatos de D. Datas sem fonte de escala não recebem presença inventada; calendário gerado para relatório deve coincidir com fonte e período.

### M12 — Frequência: presença integral, parcial, ausência e limites — ROP-010–022, 025–028, 043–045

**Positivos:** batidas explícitas e presença sem batidas quando comprovada por jornada válida; presenças parciais e integrais; cálculo de minutos, jornada, apuração noturna, faltas/absenteísmo, totalização por trabalhador/contrato/Gestão e auditoria. Identificar inequivocamente Pessoa + Vínculo + Alocação + data do início da jornada. Separar **FALTA**, ausência de lançamento, ausência justificada e ausência de prova.

**Bordas:** data futura sempre recusada **pela API inclusive Gestor Local**; registro de D permitido apenas até instante estritamente anterior a `D+7 00:00 America/Sao_Paulo`; exatamente às `D+7 00:00` recusar e após também; em D+8, observar fechamento conforme fluxo. Testar relógio controlado e processo real em ensaios separados. No turno noturno, mudança de alocação na madrugada do dia seguinte não altera o fato originado em D.

**Correção gerencial:** Gestor Local corrige com fundamento, versão e auditoria; durante janela, o bloqueio operacional recai **sobre o registro corrigido**, não todos os outros; depois da janela somente atos excepcionais autorizados. Testar idempotência com/sem chave, requisições simultâneas, contrato inativado entre validação e commit, executor revogado durante lock e rollback de lote com item futuro/inválido. Zero escrita parcial sem semântica contratual prevista.

### M13 — Ajustes, exceções e fechamento assíncrono de frequência — ROP-019–021, 034–036, 043–045, 054

**Fluxo:** preparar massa de **200 vínculos elegíveis** e outra configuração com chunk produtivo **500**. Criar job único, acompanhar estados do mesmo ID, progresso, quantidade concluída, erros, latência, cancelamento e reprocessamento. Reproduzir a falha anterior (não terminou em 120s com chunk 1) e determinar se a causa é desempenho, espera do driver, elegibilidade repetida, lock, scheduler ou problema real de execução; não apenas aumentar timeout até ficar verde.

**Concorrência/recuperação:** cancelar após prefixo **N>0** realmente persistido, comparar registros antes/depois, reprocessar sem duplicar efeitos, introduzir falha controlada **da JVM própria** depois de prefixo confirmado, reiniciar exatamente os mesmos bytes/configuração e observar progressão automática do **mesmo job/tentativa** em UI, API e MySQL. Testar idempotência da segunda inicialização e evitar duplo executor. Demonstrar **200/200 e estado terminal**, ou classificar como falha/não execução e manter evidências parciais honestas. A prova H02 antiga de 47/200 (48 no corte) não encerra este item.

**Regras complementares:** D+8, estados de vínculo/contrato, documentos, cancelamento de operação, retorno de erro/retry, auditoria, progresso de jobs e reabertura após reinício. Dados confirmados não podem ser apagados por limpeza de teste.

### M14 — Ocorrências, atestados, justificativas e anexos — ROP-022–024, 031, 046

**Casos:** criar ocorrência em data com vínculo/alocação/Gestão válidos; anexar arquivo sintético, baixar e comparar SHA; atestado de vários dias com intervalo correto, inclusive atravessando gestão; ocorrência sobre presença já existente deve preservar presença/versões e aplicar somente efeito permitido; tentativa de fato no futuro ou por usuário sem escopo deve ser recusada; validar extensão/MIME/tamanho e acesso a anexos restritos. Conflito concorrente deve ser idempotente e fazer rollback **de fato e arquivo**, não deixar órfãos em `.staging`/repositório final. Revalidar autoridade temporal do usuário no dia do fato (DIF-02 histórico), não apenas Gestão/lotação atuais.

### M15 — Pendências administrativas e regularização — ROP-037, 054; AUD-006

**Casos:** abrir pendência por fato/recorte, listar somente escopo permitido, regularizar com fonte e histórico, provar bloqueio apenas no conjunto alcançado pela pendência; efetuar **escrita positiva fora do recorte atingido** com autorização válida (`AUD-006:002`), e testar replay conflitante após mudança temporal de Gestão (`AUD-006:004`). Cancelar pendência não deve apagar evidência pretérita nem liberar autorizações alheias. Não declarar testada a escrita positiva apenas porque GET funcionou.

### M16 — Histórico, provas temporais, nomes e catálogos — ROP-029–033, 038–040

**Regra:** separar **quando o fato produziu efeito** de **quando a administração tomou conhecimento**. Testar fotografia de publicação original, alteração posterior de CNPJ/empresa, nome, cargo/lotação/turno, validade de cadastro, transferências de Gestão e documentos de publicação; consultas por data de efeito e corte de conhecimento. `null`, valor não comprovado e ausência factual são coisas distintas; zero e `false` são valores legítimos. Uma fonte tardia não pode ser retroprojetada como conhecida antes de seu registro.

**Provas:** prévia/confirmar, idempotência, permissão atual versus histórico, reconstrução anterior à retificação, ordem dos atos e cadeia documental. Não retornar 200 fabricado para prova inconsistente. Testar com e sem fonte autêntica; mecanismo técnico pode passar enquanto HIS-001/HIS-006, CAT-004/CAT-007, COL-010/CON-023 continuam documentalmente bloqueados.

### M17 — Documentos, SEI, upload/download, versões — ROP-038, 039, 047; defeito DOC-DTO

**Fluxo:** anexar arquivo sintético com checksum, classificação de acesso e referência SEI quando contratualmente exigida; versionar/retificar; confirmar/documentar; reler via GET e replay; verificar conteúdo byte a byte. Em prova de primeira resposta, `version` técnica deve obedecer contrato explícito com GET/replay/JPA depois de commit (H02 retornava 0 e depois 1); não confundir `version` de trava técnica com versão negocial do documento.

**Negativos:** revogar permissão atual e tentar link de download antigo; sessão expirada durante download; arquivos inválidos; uploads interrompidos; dois retificadores simultâneos; replay 409; transação falha antes/depois de persistir anexo e limpeza de órfãos **somente na massa isolada**, sem tocar arquivos de usuários. Prever política de retenção e não apagar documentos históricos. FE-01: atravessar o ramo de `await import(...)` do PDF inline em carregamento frio e mudança de sessão, provando descartes antes/depois do import; não tomar exportação assíncrona como substituta.

### M18 — Painel, indicadores, efetivo e análises — ROP-057, 061, 064

**Casos:** efetivo contratado versus alocado, indicador 150%, taxa de presença/absenteísmo com fonte/denominador corretos, status nulos/zeros, filtros por período, Gestão e contrato; alternar rapidamente A→B, reter/liberar resposta de A e provar que gráfico/título/tabela/exportação continuam B; invalidar resultado quando retificação altera fonte. Filtros de indicadores historicamente corrigidos não podem regredir. Testar erro em uma subconsulta com outras concluídas e impedir dados antigos sob cabeçalho novo.

### M19 — Relatórios e exportações PDF/XLSX síncronas/assíncronas — ROP-040–042, 057, 061

**Casos:** download real em Chrome sob diferentes perfis/períodos/contratos; PDF de **41 registros em 2 páginas** como controle de paginação de relatório; outro caso com uma pessoa; tabela UI de 5 registros por página e sexto acessível; exportação de período com 25+1 quando API real utilizar essa paginação. Verificar magic bytes, headers, content-type, tamanho, autorizações, contagens, conteúdo textual, colunas, valores, paginação, identidade de arquivo e hash. PDF/XLSX gerado deve ter período e identidade da **resposta atual**, nunca arquivo de filtro anterior.

**FE-02:** calendário bissexto 2024 × 2023: 15 datas de fevereiro de 2024 contra 14 datas correspondentes de 2023 no recorte anterior; 29/02 **sem inventar valor comparativo**. Além de shape, usar valores **diferentes por dia** no DTO real e validar cada posição/índice na planilha e PDF; massa com todos zeros NÃO prova ausência de deslocamento. Importação inline FE-01 permanece caso separado. Inspecionar legibilidade em zoom padrão, quebra de página, repetição de cabeçalhos, truncamento e acessibilidade PDF/UA no alcance disponível; não declarar WCAG/PDF-UA integral sem auditoria.

### M20 — Usuários, perfis, autenticação, sessão, CSRF e 2FA — ROP-037, 039, 049, 064; RI03

**Matriz de perfis:** Gestor Local, Operacional, consulta e demais perfis reais encontrados; verificar endpoint por endpoint com usuário legítimo, sem login, com sessão expirada, escopo alheio, permissão revogada e CSRF ausente/incorreto; distinguir **401**, **403**, **409**, **404** de acordo com contrato. Não confiar no React como barreira de autorização.

**Sessão entre abas:** login A, iniciar fluxo assíncrono e reter resposta HTTP verdadeira; logout/login B em outra aba, confirmar cookies e `userId`, liberar A; **nenhum POST/PUT/DELETE da intenção A** depois de B; nenhum dado privado de A pode reaparecer; formulário parcial seguro e recuperação sem duplicidade. Testar variantes POST→PUT, PUT final já confirmado, perda de resposta após commit, resultado tardio de GET, múltiplos logouts/logins, abas em foco/desfoco, `storage` bloqueado, clear storage, evento focus, remount e corrida com 2FA. Backend deve permanecer protegendo a operação mesmo que UI tenha bug.

**2FA:** habilitar/desabilitar, recuperação de estado após novo login, recuperação de 401, rejeição de requisições antigas e limite razoável de polling; H01 realizou 710 GETs em 5s e H02 corrigiu o comportamento principal. Reexecutar HTTP real. Não expor segredo ou QR nas capturas. Sessão idle deve efetivamente encerrar UI e revogar token sem exceção de exclusão otimista; teste real de tempo e reconciliação de token.

### M21 — Auditoria, causalidade e governança de alterações — ROP-050, 055, 060, 062–064

**Casos:** para publicar, retificar, corrigir, transferir, vincular, remanejar, anular, baixar documento restrito quando auditável e realizar exceção, verificar autor, perfil, data/hora, data efetiva, motivação, fonte, versão anterior/atual e sequência de eventos. Falha 401/403/409, cancelamento de confirmação ou resposta antiga não deve produzir auditoria de ato inexistente. O log deve ser consistente após rollback, cancelamento, reprocessamento, upgrade e reinício. Revisão de escopo administrativa não libera escrita em outra Gestão sem autorização temporal. ROP-064 foi **FALHOU na H02**: testar expressamente que nenhuma recusa, confirmação cancelada ou resposta tardia aparente sucesso ao usuário ou em banco.

## 7. Dossiê de defeitos e pendências: reproduzir, corrigir, homologar INDIVIDUALMENTE

### 7.1. DEF-H02-RI03-POST — CRÍTICO: mutação da intenção A após sessão B

**RED comprovado na H02:** `H02_RI03_TAB_POST_03`. Um POST autenticado persistiu a empresa 16; o controlador reteve sua resposta **real**. Outra aba efetuou logout de A e login de B. Quando a resposta POST original foi liberada, a aba A iniciou um **PUT de associação** ao contrato 21, 29 ms após a confirmação do login B. O backend respondeu **403**; cinco SELECTs de reconciliação confirmaram empresa única, contrato sem fornecedor e sem auditoria de associação. **Não houve bypass de autorização demonstrado**, mas a UI enviou mutação proibida. O caso `H02_RI03_TAB_PUT_03` passou 34 checks em ramo distinto e não apaga o RED do POST.

**Hipótese a investigar, NÃO tomar como verdade a priori:** geração de sessão/`intencao.atual()` é local ao contexto da aba e não percebe o momento de troca A→B no cookie/estado compartilhado. H03 propõe marcador público/opaco em `localStorage`, escuta de `storage`/`focus` e avanço da geração; precisa de auditoria específica para corridas, abas abertas antes da instalação do listener, evento disparado só na outra aba, política de storage, erros de acesso, transições logout/login, remount e abertura de novas abas.

**Oráculo RED→GREEN obrigatório:**

1. Montar massa com rascunho real e empresa ausente, duas contas com escopos distintos, duas abas Playwright no mesmo contexto adequado e MySQL 8.0.45.
2. Disparar POST empresa na aba A e **reter a entrega da resposta verdadeira**, sem simular um sucesso falso de servidor.
3. Confirmar em MySQL que existe exatamente uma empresa, depois concluir logout A/login B na aba B. Esperar prova da sessão B **autenticada**, não apenas clique em login.
4. Entregar resposta antiga do POST, coletar rede e provar **ausência de PUT de associação iniciado após B**; não se satisfazer com 403. A aba A deve invalidar estado/intenções e não exibir dados privados de A como se fossem atuais.
5. Selecionar a empresa existente sob autorização válida e repetir **só a associação**, sem novo POST da empresa e sem duplicidade; GET e auditoria consistentes.
6. Repetir retenção do PUT depois de associação já concluída; preservar o resultado legitimamente persistido, sem nova associação/retry após logout.
7. Repetir: GET antigo retornando, resposta perdida após commit, duas mudanças rápidas A→B→C, aba em background, foco antes/depois, storage indisponível, renovação de 2FA, token revogado, logout e login com mesmo usuário mas nova sessão e contexto React remontado.
8. Testar que marcador de coordenação **não contém** username, CPF, sessão, JWT, CSRF, senha ou segredos; validar que sincronização entre abas não executa logout HTTP em cookie já pertencente à sessão B.
9. Conferir que erro/cancelamento mostra estado honesto, preserva dados úteis sem reautenticá-los automaticamente e não cria falso sucesso de associação.
10. Testes de componente, hook, integração e E2E Chrome contra JAR FINAL; comparar os logs/redes antes/depois e snapshot MySQL de cada variante.

**Resultado exigido:** `POST antigo chegou` pode ser fato verdadeiro; `PUT iniciado depois da troca` deve ser **zero**; `empresa duplicada` deve ser **zero**; `associação feita sem direito` deve ser **zero**. Uma 403 do servidor é proteção adicional, **não** aprovação do React.

### 7.2. DEF-H02-PROR01 — ALTO: prorrogação não executável pela UI

**RED comprovado:** `H02_UI_PROR01`: em contrato 18 publicado, o React exibiu data efetiva, documento e assinatura, mas **não** exibiu entrada `novaDataFim`; `formAditamento` e serviço já conheciam esse campo. Nenhum POST foi enviado.

**Exigir:**

- Analisar o JSX e todos os modos do formulário, não apenas o teste de payload do H03; criar teste DOM com `getByLabelText` e acessibilidade; renderizar controle editável de data final somente no contexto onde a regra exige, sem alterar outros tipos de aditamento.
- Cobrir validação local e de backend das datas, ausência de data e relações de vigência conforme contrato. `dataEfetiva` não pode sobrescrever `novaDataFim` nem sofrer deslocamento por conversão UTC/client TZ.
- Prévia real → confirmar com token da prévia correta; se formulário mudar depois da prévia, impedir confirmação com token obsoleto; testar usuário revogado entre os passos.
- E2E Chrome: criar um contrato novo sintético publicado, preencher a prorrogação, concluir confirmação, GET de datas/versão, `posicaoTemporal`/histórico antes e depois, documentos e auditoria. Reiniciar JAR, reconferir data e cadeia sem duplicações.
- Negativos: datas inválidas, conflito de versão 409, formulário parcialmente preenchido, cancelamento/ESC, duplo clique, segunda confirmação, falha de rede após commit, perfil não autorizado (403), tentativa antes de publicação e documento inadequado.
- Regredir redução de efetivo, remanejamento e outros aditamentos, para que o novo campo não quebre seus payloads.

### 7.3. DEF-H02-DOC-VERSION — ALTO: resposta inicial versus replay de retificação

**RED comprovado:** `H02_C_CONFORMANCE_02`: primeiro DTO de retificação retornou `version=0` e o replay `version=1`, com mesmo documento 7 e versão negocial 2. `H02_C_DOC_CANONICAL_03` comprovou GET pós-commit/JPA/replay coerentes com `version=1`, **sem sanar a primeira resposta**.

**Procedimento:** localizar rota/DTO/serviço/transação/versão de bloqueio otimista; caracterizar exatamente qual campo é `version` técnica e qual é versão documental de negócio; inspecionar se resposta é construída antes de persistir/flush, cache de entidade, listener Hibernate, mapeador ou transação sem releitura; **não presumir a causa**. Formular contrato correto de resposta inicial e replay conforme os usos reais e segurança; escrever teste de integração com MySQL 8.0.45 que compare primeira resposta + GET pós-commit + estado JPA + replay idempotente + GET final. Comparar document ID, versão negocial, hash do arquivo, referência SEI, categoria/acesso, data de conhecimento, auditoria, efeitos de 409 e consumidores Java/React. Incluir documento **com anexo não vazio** para comprovar integridade byte a byte e outro caso sem anexo para não pressupor arquivo existente. Não corrigir alterando apenas expectativa do teste; preservar locking/idempotência.

### 7.4. DEF-H02-CLOSURE — ALTO: fechamento e retomada não concluídos no teste original

**Histórico real:** `H02_CLOSURE_UI_01` criou/cancelou/reprocessou o job com 200 vínculos, mas não obteve `CONCLUIDO` no prazo declarado de 120 s com **chunk 1** (não padrão de produção); cancelamento ocorreu antes de haver prefixo persistido. Após interrupção forçada, 46 frequências foram preservadas. Duas observações de browser falharam por modal/selector. Uma terceira inicialização alcançou **47/200** em UI/banco e 48 no corte final: recuperação **parcial e delimitada**, não conclusão 200/200. Padrão produtivo informado: **chunk 500**.

**Investigar sem pressupor defeito do algoritmo:** distinguir repetição de consultas de elegibilidade em páginas de 200, locks e esperas, custos de reconstrução temporal, heartbeat, tempo de I/O, throughput, reinícios, GC, polling, job realmente ativo, timeout de driver e bloqueio por modal. Instrumentar por fase e obter perfis de tempo/memória/queries sem vazar dados. Se houver erro de desempenho/algoritmo, corrigir com teste de regressão mantendo transação/idempotência; não aumentar timeout cegamente nem tirar as assertions de conclusão.

**Aceite:** executar um ciclo real até **200/200 + status terminal** em MySQL 8.0.45, sob condições de carga e chunk declaradas; medir duração e causa. Repetir cancelamento após N>0 e preservar prefixo; reprocessamento sem duplicações; forçar morte **somente da JVM de teste**, reiniciar mesmos bytes/configuração e observar progresso automático sem POST adicional. Comparar cada linha confirmada, arquivo, evento e trilha antes/depois de reinício; verificar ausência de dois executores ativos sobre mesmo job e comportamento com sessão administrativa revogada. Realizar contraste chunk1/chunk500 sem extrapolação indevida para carga nacional.

### 7.5. DEF-ROP064 — falsa aparência de sucesso: reprovação anterior

`ROP-064` permaneceu FALHOU na H02. Construir suíte que teste: HTTP 401/403/409, confirmação cancelada, rejeição de validação, resposta antiga liberada após nova seleção ou sessão, toast atrasado, resposta positiva de **etapa parcial** (cadastro empresa) antes da etapa final recusada (associação contrato), erros em subconsulta e dados de filtro anterior. Nenhum caso pode emitir mensagem “salvo/publicado/associado/exportado” em contraste com persistência real. A cada caso comparar mensagem UI, rede, versão JPA e auditoria; silêncio não basta — deve existir feedback adequado e a interface não deve perder campos.

### 7.6. Quatro contraprovas SEMÂNTICAS, historicamente NÃO concluídas

Estas **não são automaticamente defeitos de código**. Exigir o predicado, preparação legítima e resultado observado:

- **`ALO-008:002`:** reativação em composição inválida que seja **alcançável legitimamente**, recusada antes da retomada, sem gravação. A campanha anterior tentou zerar composição ocupada e foi recusada por 409 no preparo; isso NÃO testa a reativação. Não fabricar estado inconsistente por SQL/JPA só para acertar o alvo. Criar sequência válida pela API contratual que permita examinar o ramo ou declarar precisamente por que é inalcançável.
- **`PER-010:004`:** autorização/concessão dentro do contexto histórico de ocorrência de múltiplos dias e contraprova de ausência de prova **em cada consumidor**: detalhe, lista, arquivo, criar, cancelar e conceder. Separar recusa atual de prova histórica legítima.
- **`AUD-006:002`:** após pendência bloqueante em um recorte, realizar **escrita positiva fora do recorte**, com mesma autorização válida, e provar commit + auditoria correta. GET ou leitura fora do recorte não substitui escrita.
- **`AUD-006:004`:** construir conflito de replay em período de Gestão **diferente da atual**, e provar que a autoridade no dia do efeito prevalece. Não substituir por ensaio simples de fim natural.

### 7.7. Autenticidade documental: seis grupos / 33 critérios bloqueados

| Requisito | Qtde | Evidência externa/autêntica indispensável |
|---|---:|---|
| `CON-023` | 9 | Ato real de transferência excepcional de Gestão Local, competência, fundamento, origem/destino, efeito e fonte verificável. |
| `CAT-004` | 4 | Fotografias/versionamento administrativo autenticável de catálogos nas datas avaliadas, autoria, origem e momento de conhecimento. |
| `CAT-007` | 8 | Registro oficial de início/fim de disponibilidade, suspensão e reativação de cadastros com efeitos no período. |
| `COL-010` | 4 | Fonte autorizada de nome atual e histórico da pessoa, identidade e sucessão temporal. |
| `HIS-001` | 4 | Dossiê real de contrato com atos, datas, vigências, fontes, retificações e cortes de conhecimento. |
| `HIS-006` | 4 | Publicação original e anexos para reconstrução integral de composição/empresa/efetivo e campos juridicamente relevantes. |
| **TOTAL** | **33** | Prova administrativa real autorizada; **dados sintéticos não substituem**. |

Testar tecnicamente motores de reconstrução e recusa quando a prova está ausente. Ao final, se não houver acervo autêntico, marcar cada predicado documental como **BLOQUEADO POR DOCUMENTAÇÃO**, indicando documento requisitado, responsável/fonte, método de confrontação e dados necessários minimizados. Jamais declarar que os 33 itens foram resolvidos por screenshot, hash, PDF sintético ou resposta 200.

### 7.8. Outras lacunas históricas que exigem ensaio explícito

1. **FE-01 PDF inline:** não atingiu o ramo de importação dinâmica; executar Chrome em carregamento frio, travar/liberar `await import(...)`, trocar sessão e verificar nenhum download/dado antigo reaproveitado; separar do protocolo PDF assíncrono que passou no recorte.
2. **FE-02 calendário comparativo:** forma 15/14 e 29/02 sem comparativo tem prova parcial; falta teste discriminante **com DTO real e valores diferentes por dia** em cada coluna de relatório.
3. **Virada D+7 de meia-noite:** testes Java e timezone UTC+14 não equivalem ao processo/JVM nativo atravessando 00:00 local; executar experimento isolado temporalmente controlado ou manter NÃO EXECUTADO, sem alterar host compartilhado.
4. **Escala nacional:** o histórico de >30s não foi reproduzido com massa/hardware/cache/concorrência equivalentes. Definir corpus e limites antes de benchmark; sem equivalência, relatório só pode reivindicar desempenho da massa efetiva.
5. **Acessibilidade:** testes de foco/ESC/teclado passaram em recortes, mas falta WCAG integral, leitor de tela e PDF/UA; listar quais critérios e tecnologias foram efetivamente avaliados.
6. **Lint/typecheck:** não estavam configurados; registrar como NÃO CONFIGURADO, sem chamar Vite build de typecheck. Se adicionar gates, fazê-lo sem mudança cosmética massiva e aferir resultados.
7. **Spring MVC/dependências:** dois avisos de segurança apareceram na análise antiga de 88 bibliotecas. Reavaliar aplicabilidade e versões com fontes e ambiente disponíveis; não introduzir atualização ampla e não confundir alerta com exploração.
8. **JPA/schema:** H01 teve suite falha com 32 FKs ausentes e `errno 184`; controle em schema novo passou, sem causa fechada. H02 passou 1.384 IT na campanha anterior em 8.0.46. No novo 8.0.45 testar tabela, relacionamentos, constraints, criação e atualização automática por Hibernate, inclusive schema novo e versão legada sintética, sem DDL manual.
9. **Artefatos intermediários:** H02 build02 foi rejeitado por UI vazia e 145 assets; build03 invalidado ao surgir 47 assets após congelamento. Exigir assets completos, fonte congelada e JAR final com bundle realmente executado. Não confundir contagem histórica de 98 assets com obrigação de nunca mudar após alteração legítima.
10. **Integridade de testes:** logs/paths de console foram substituídos em execução antiga; evidências novas exigem hashes, descritores e XMLs frescos reconciliados. Testes interrompidos ou setup de driver falho não podem virar PASS.

## 8. Matriz executável de homologação: camadas, suítes e evidências

Toda suíte deverá utilizar o mesmo padrão de relato: **ID**, precondições, perfil, massa sintética, relação com requisito/ROP, endpoint/arquivo, ação, asserção **positiva e negativa**, expectativa de banco, resultados, versão MySQL observada, SHA-256 de JAR/assets e prova localizável. Cada caso de teste tem status próprio; não contar uma suíte como executada se apenas seu `beforeAll` rodou. Scripts da campanha anterior são ponto de partida, não prova nem substituto da nova execução.

### 8.1. Fase ZERO — 12 provas de infraestrutura e identidade (INF-001..INF-012)

| ID | Procedimento de teste | Critério verificável |
|---|---|---|
| INF-001 | Listar, extrair e hashear os quatro pacotes H02 e seis H03. | Quatro SHA-256 H02 idênticos à tabela; H03 segregado; ZIPs inteiros e sem path traversal. |
| INF-002 | Inventariar dependências reais da Biblioteca antes de downloads. | Inventário com versão, formato, tamanho, hash, incompatibilidades e licença/proveniência quando identificáveis. |
| INF-003 | Resolver e iniciar `mysqld` do pacote 8.0.45 em runtime compatível. | Processo, PID, porta, datadir isolado, logs e binário exato 8.0.45; zero fallback. |
| INF-004 | Conectar via cliente e JDBC da aplicação/Testcontainers. | `VERSION()`, UUID, `CURRENT_USER()`, `DatabaseMetaData` = MySQL Server 8.0.45 na instância correta. |
| INF-005 | Forçar tentativa de versão diferente no teste guardião (somente ambiente descartável). | Teste falha fechado; não altera oráculos de domínio. Não executar suite funcional de homologação em versão proibida. |
| INF-006 | Iniciar Redis verdadeiro; executar ping, gravar/ler chave de teste e revogar token sintético. | Comando confirmado, conexão isolada, sem fake ou limpeza de base compartilhada. |
| INF-007 | Validar Chrome fornecido e CDP Playwright com página local e captura. | Executável real, versão, hash, screenshot DOM e console, não só importação da dependência. |
| INF-008 | Compilar Java e React a partir das fontes extraídas. | Maven/Node/lockfile/versões registradas; nenhuma dependência baixada silenciosamente em versão diferente. |
| INF-009 | Carregar o bundle frontend dentro do JAR candidato. | Manifesto e bytes dos assets servidos em HTTP correspondem ao build React declarado. |
| INF-010 | Reiniciar servidor em mesma porta/banco/configuração e provar `ACCEPTING_TRAFFIC` e rota autenticada. | Mesmo hash JAR, mesmo UUID MySQL, HTTP real e dados preservados. |
| INF-011 | Registrar cotas de memória/CPU e medir consumo das suítes. | Ausência de OOM/OOMKill ou diagnóstico separado; paralelismo pesado controlado. |
| INF-012 | Inventariar e corrigir guards literais de 8.0.46 em testes/containers. | Diff limitado e motivado, grep final de referências ativas, suíte 8.0.45 executável. |

### 8.2. Suítes Java e Hibernate

**JAVA-U:** rodar todos os testes unitários com registros de classes, XML Surefire, horários, falhas, ignorados e contagem única. O histórico H02 teve **576 unitários**; a nova quantidade pode crescer em razão das correções. Não declarar 576 como resultado novo sem executá-los.

**JAVA-I:** rodar todos os testes de integração/Failsafe com MySQL **8.0.45**, incluindo as novas regressões, e auditar XMLs frescos, logs dos forks, classes efetivamente executadas, conexão JDBC e zero testes ignorados silenciosamente. A H02 antiga teve **1.384 integrações**, mas isso era no MySQL 8.0.46: **não transportável**.

**JAVA-SCHEMA:** em banco 8.0.45 **novo e isolado**, permitir Hibernate criar/atualizar os objetos por suas entidades; conferir metadados de `information_schema` **por leitura**, relacionamentos, índices únicos, nullable, collation e FKs. Se houver erro automático de DDL Hibernate, corrigir a **modelagem JPA**/dependências, nunca emitir CREATE/ALTER TABLE manual. Repetir inicialização sem alterações destrutivas. Em outro cenário **sintético legado criado previamente pelo próprio Hibernate**, executar upgrade de fontes/versões suportadas sobre **o mesmo MySQL 8.0.45** e comparar linhas/anexos antes/depois; a impossibilidade de montar baseline compatível deve ser declarada, não fingida.

**JAVA-CONC:** barreiras sincronizadas reais em testes com DB, múltiplas transações/threads e contas/escopos diferentes, testando optimistic locking, unique constraint, retry idempotente, revogação durante espera de lock, 409 no commit e atomicidade de arquivo+metadados.

**JAVA-HIST:** `Clock` fixo, duas datas de negócio distintas, conhecimento tardio, transferência de Gestão e versões de documento; comparar snapshot histórico vs estado atual para todas as famílias temporais. Não usar valores de relógio não registrados.

**JAVA-API:** testes de contrato que verificam serialização DTO, códigos 200/201/204/400/401/403/404/409 segundo implementação real, sem exposição de entidades, propriedades sensíveis, versionamento e headers de download. Cada endpoint alterado requer teste positivo, negativo e autorizado/revogado.

**JAVA-RES:** testes de fechamento e scheduler reais com MySQL+Redis. Não confundir TaskScheduler substituído ou transação de teste revertida com job nativo que confirma commit externo.

**JAVA-COVER:** cobertura JaCoCo e ramos críticos, relatório por pacote e linhas/branches efetivos. Evitar escolher apenas testes fáceis para elevar percentagem. Nenhuma meta percentual artificial substitui prova de comportamento.

### 8.3. Suítes React, build e acessibilidade

**REACT-U:** executar todos os Vitest existentes, depois acrescentar testes de componentes de prorrogação, troca de aba, arquivos, histórico, filtros, paginação, `ESC`, prevenção de duplo envio e 401/403/409. Histórico H02 teve **602 testes em 84 arquivos**; a quantidade nova é derivada de XML/JUnit original da execução, não estimativa. Usar `maxWorkers=1`/sequencial se o limite de memória indicar necessidade.

**REACT-ASYNC:** testar corrida com duas promises controladas e **respostas HTTP reais nos E2E**; alterar filtros A/B/A, logout/login, atualizar token, desmontar componentes, cancelar requests; verificar que nenhuma resposta anterior restaura UI, sessão, tarifas ou documentos de outra intenção. Testes só com promises sintéticas são unitários, **não** E2E.

**REACT-BUILD:** usar lockfile, gerar build produção, validar importações e assets no ZIP/JAR, configuração pública, rotas base, MIME/CSP e inicialização sem white screen. Qualquer asset adicional precisa estar presente e correspondido; não exigir exatamente 98 se houve mudança legítima, mas justificar diff.

**REACT-LINT/TYPE:** inventariar configuração real; se inexistente, indicar NÃO CONFIGURADO. Se viável, adicionar checks dedicados sem mudanças indiscriminadas que desviem do objetivo. Corrigir erros introduzidos pelas alterações.

**A11Y:** navegação só teclado, sequência de foco, labels, aria-live para mensagens, ESC para diálogos, retorno de foco, menu fechado não tabulável, estado de carregamento e botões de confirmação. Documentar tecnologias e critérios WCAG realmente avaliados; leitores de tela/PDF-UA não examinados ficam explicitamente não executados.

### 8.4. Matriz de segurança HTTP real (SEC-001..SEC-014)

| ID | Contexto adversarial | Oráculo além do status HTTP |
|---|---|---|
| SEC-001 | Sem autenticação solicita criação/retificação/arquivo protegido. | 401/recusa contratual; **zero** mutação, evento ou arquivo publicado. |
| SEC-002 | Usuário autenticado sem escopo territorial tenta ação de Gestor. | 403; nenhuma alteração temporal ou auditoria de ato autorizado. |
| SEC-003 | Operacional tenta alterar frequência fora de lotação/turno/data. | 403/409 conforme contrato; autoridade verificada no servidor **pelo dia do fato**. |
| SEC-004 | Revogar permissão **após prévia e antes de confirmar**. | Confirmação recusada com fonte/versão preservadas; UI não mostra sucesso. |
| SEC-005 | Omitir/inutilizar token CSRF em POST/PUT/DELETE. | Rejeição protegida; cookies/sessão não substituem CSRF. |
| SEC-006 | Duas confirmações do mesmo token/idempotency key. | Um único ato efetivo; replay canônico ou conflito contratualmente definido. |
| SEC-007 | Dois usuários disputam mesmo contrato/versão. | Um vencedor legítimo; perdedor 409, nenhuma perda silenciosa de atualização. |
| SEC-008 | Token expirado durante download/operador revogado. | Acesso negado mesmo com URL antiga; sem vazamento do arquivo. |
| SEC-009 | Troca sessão A→B com resposta retardada. | Nenhum dado A renderizado, nenhuma nova mutação de A; monitorar backend 403 se houver ataque direto. |
| SEC-010 | 2FA habilitar/desabilitar/erro/tentativas repetidas. | Estabilidade de polling, sessão coerente e segredos omitidos de logs. |
| SEC-011 | Logout concorrente com idle timeout/revogação. | Sem exceção de exclusão otimista; token revogado e outras sessões não afetadas indevidamente. |
| SEC-012 | Caminho/ID de documento de outra Gestão/usuário. | Sem acesso indevido, incluindo path traversal e URL previsível. |
| SEC-013 | URLs antigas de governança com cache `200/HIT`. | 404 autenticado e sem reexposição; Fornecedor operacional segue ativo. |
| SEC-014 | Entradas HTML, strings de busca, anexos e parâmetros temporais malformados. | Validação e escape corretos, nenhum XSS/injeção e sem mutação; registrar caso reproduzível sem tentar explorar terceiros. |

### 8.5. Matriz temporal e civil (TMP-001..TMP-014)

- **TMP-001:** frequência em D, antes de `D+7 00:00` por 1 nanossegundo ou a menor precisão efetivamente suportada: permitida somente se vínculo/alocação/autorização/ausência de futuro também válidos.
- **TMP-002:** exatamente `D+7 00:00` (`America/Sao_Paulo`): encerrada, **limite exclusivo**.
- **TMP-003:** depois de D+7: recusada na via operacional; verificar UI, API e persistência, não apenas contador.
- **TMP-004:** D+8: iniciar fechamento quando programado e verificar idempotência.
- **TMP-005:** presença futura recusada pelo Gestor e Operacional, independentemente da UI.
- **TMP-006:** turno noturno começando em D e encerrando em D+1: apuração pertence a D.
- **TMP-007:** cargo/lotação/turno muda em D+1: fato iniciado em D conserva os valores efetivos de D.
- **TMP-008:** contrato publicado com retificação posterior: data de efeito e data de conhecimento geram fotografias diferentes.
- **TMP-009:** catálogos inativos hoje, mas válidos em D: uso histórico positivo sem reativação artificial.
- **TMP-010:** aquisição tardia de fonte: histórico anterior deve indicar desconhecimento, não afirmar falso fato.
- **TMP-011:** mudança excepcional de Gestão: escopo sobre fato pretérito segue autoridade **da data do fato**; testar CON-023 mecanismo técnico.
- **TMP-012:** calendário 2024/2023 e 29/02: nenhum comparativo inexistente; valores discriminantes.
- **TMP-013:** cliente `Pacific/Kiritimati` ou UTC+14 vs data administrativa São Paulo: API decide dia corretamente, evitando arredondamento UTC impróprio.
- **TMP-014:** se alegar teste de atravessamento físico de meia-noite, usar processo isolado e prova de antes/no/depois; caso contrário registrar NÃO EXECUTADO, mesmo que TMP-001..003 passem com Clock de teste.

### 8.6. Matriz de concorrência/atomicidade (CON-001..CON-012)

1. **CON-001:** empresa duplicada em dois POSTs simultâneos, CNPJ único; diferenciar falha de unicidade de erro HTTP e auditoria.
2. **CON-002:** cadastro persistido com resposta perdida, recuperação pelo catálogo, nenhuma segunda empresa e só associação retomada.
3. **CON-003:** publicação do contrato vs associação de empresa em mesmo rascunho; vitória legítima e conflito com versão sem mescla indevida.
4. **CON-004:** dois aditamentos/previsões simultâneos e prévia expirada: sem fechamento de faixa incoerente.
5. **CON-005:** dois movimentos de alocação com data efetiva concorrente: sem sobreposição, sem duplicar faixa atual.
6. **CON-006:** duas admissões/vínculos com reserva global da Pessoa: regra de unicidade ou concorrência do domínio aplicada atomicamente.
7. **CON-007:** dois lançamentos de frequência com mesma chave: idempotência; com chaves diferentes: conflito específico, não duplicar o mesmo fato.
8. **CON-008:** lote de frequência com um elemento inválido/futuro: rollback conforme contrato; não deixar metade comprometida.
9. **CON-009:** ocorrência com anexo, rollback após falha de arquivo: metadados e arquivo final coerentes, sem órfãos.
10. **CON-010:** duas retificações de documento/SEI: versão técnica, replay, conflitos e bytes de arquivo corretos.
11. **CON-011:** revogação de perfil e sessão durante espera de lock/confirmar: revalidar autorização no momento exigido, zero mutação após revogação.
12. **CON-012:** fechamento em dois executores após restart: não concluir duas vezes, prefixo preservado, contadores auditáveis, id de job/tentativa estável.

Para cada uma: executar simultaneidade comprovada por barreiras/tempo/log, não dois requests sequenciais, e verificar estado final por **SELECT somente leitura** em MySQL 8.0.45.

### 8.7. E2E Chrome real: jornadas completas mínimas

Executar os **20 drivers originais** identificados no acervo anterior e todos os auxiliares que forem de fato aplicáveis; havia 24 arquivos `.cjs` no pacote, mas apenas 20 drivers de jornada, três auxiliares e um teste Node. Não inventar um 21º driver nem contar auxiliares como E2E aprovados. Corrigir setup/seletores de testes sem remover assertions. Cada jornada relevante deve ter prova UI (DOM/captura), HTTP, JAR e banco:

- **E2E-01 (Fornecedor):** criar/editar/inativar/reativar; paginação cinco+sexto; CNPJ normalizado; tabela/DTO consistente.
- **E2E-02 (Contrato):** rascunho, associação, composição, publicação, documento, histórico e bloqueio pós-publicação.
- **E2E-03 (RI03):** POST resposta retida, troca de aba e GET/PUT pós-sessão; branch positivo de recuperação; banco reconciliado.
- **E2E-04 (Prorrogação):** preencher `novaDataFim`, prévia, confirmação, GET e histórico; ausência de campo = FAIL.
- **E2E-05 (Aditamento/remanejamento):** reduzir 25% na base original, rejeitar 26%, exceção quando autorizada, remanejamento soma zero.
- **E2E-06 (Pessoa):** CPF sintético, homônimos, vínculo, matrícula, alocação e identificação única.
- **E2E-07 (Admissão):** futura/programada, desligamento, cancelamento, recontratação e fotografia anterior.
- **E2E-08 (Frequência):** integral, parcial, FALTA e sem registro; tela/DTO/indicador consistentes.
- **E2E-09 (Futuro):** presença futura recusada tanto pela UI quanto via API real com Gestor Local.
- **E2E-10 (Tempo):** D+7/turno noturno e autorização histórica sem confundir Clock lógico com relógio nativo.
- **E2E-11 (Atestados):** ocorrência com anexo real, vigência multi-dia, download e versão histórica.
- **E2E-12 (Pendência):** recorte bloqueado, escrita legítima fora do recorte, conflito temporal de Gestão.
- **E2E-13 (Histórico):** fonte tardia, nome anterior, CNPJ/empresa anterior, versão de documento, emissão historicamente coerente.
- **E2E-14 (Painel):** 150%, período A→B com respostas fora de ordem, loading e exportação atual.
- **E2E-15 (PDF):** 41 registros/2 páginas, arquivo íntegro, download com sessão correta e primeira página/última página conferidas.
- **E2E-16 (XLSX):** 2024/2023 bissexto com valores discriminantes, 29/02 sem comparativo artificial.
- **E2E-17 (Segurança):** autenticação, perfis, 2FA, logout, idle real, cookies/CSRF, duas abas e download revogado.
- **E2E-18 (Menus/UX):** ESC, foco, erros 409 preservando formulário, botões durante confirmação, paginação, múltiplos modais.
- **E2E-19 (Jobs):** fechamento 200/200 + cancelamento prefixo>0/reprocessamento; dashboard de progresso real.
- **E2E-20 (Reinício):** mesmo JAR, MySQL e Redis após parada, contrato/frequência/documento/job e audit trail preservados.
- **E2E-21 (Remoção):** rotas antigas 404 em cache `200/HIT`, UI financeira/risco ausente, fornecedor e fluxos operacionais intactos.
- **E2E-22 (Documento versionado):** primeira resposta de retificação vs replay/GET/versão JPA e anexo íntegro.
- **E2E-23 (PDF inline FE-01):** lazy import, sessão trocada, nenhum download antigo nem vazamento.
- **E2E-24 (Recusas ROP-064):** 401/403/409, confirmação cancelada, falha parcial e resposta atrasada não simulam sucesso.

Os identificadores E2E-01..24 acima são **novos roteiros de aceite**, não afirmação de que existiam 24 jornadas aprovadas. Registrar cobertura por cenário e por camada. Capturar screenshots em resolução desktop **1440×900** e, onde aplicável, zoom 150%, fuso São Paulo e caso alternativo UTC+14; testar visual real, não só HTTP 200 de asset. Se o acesso ao browser não estiver disponível, classificar E2E como **NÃO EXECUTADO** e não homologar integralmente.

### 8.8. Exportação, arquivos e integridade de resultados

- **ARQ-001:** download PDF/XLSX real, headers, magic bytes, extensão, MIME, hash, nome e conteúdo esperado.
- **ARQ-002:** arquivo PDF 41 registros em duas páginas; não declarar 85 se não gerou 85; contar páginas e linhas conforme fonte.
- **ARQ-003:** Excel 29/02 com valores discriminantes e fórmulas/valores carregados, comparar com DTO de mesma requisição.
- **ARQ-004:** arquivo da pessoa A não pode ser servido sob sessão B nem após revogação de permissão.
- **ARQ-005:** documentos SEI com upload/download, retificação com anexo não vazio e checksum idêntico.
- **ARQ-006:** PDF de relatório longo: colunas não cortadas, fonte legível, cabeçalhos, páginas, quebras; alcance PDF/UA declarado separadamente.
- **ARQ-007:** cancelamento de exportação/erro intermediário não marca job como concluído nem disponibiliza arquivo inválido.
- **ARQ-008:** respostas fora de ordem não trocam relatório/período ao baixar.

### 8.9. Upgrade, persistência e reinício (RST-001..RST-008)

- RST-001: preservar baseline sem modificá-la; derivar banco sintético **8.0.45** com entidades Hibernate.
- RST-002: inicialização nova cria/atualiza apenas via JPA; todas as FKs/restrições relevantes verificadas por leitura `information_schema`.
- RST-003: boot repetido `ddl-auto=update` sem dano, sem recriar dados/fatos ou apagar arquivo.
- RST-004: parada graciosa; snapshots de tabelas, linhas, índices, anexos, jobs e SHA antes/depois, mesmo JAR/env/UUID MySQL.
- RST-005: falha controlada de JVM própria com commit parcial legítimo; recuperação automática e preservação prefixo já persistido.
- RST-006: Redis cache legado contém payload antiga rota removida; autenticação 404 e sem DEL/FLUSH indevido.
- RST-007: upgrade funcional de esquema legado **produzido pelo Hibernate da versão de origem na mesma instância 8.0.45**, preservando dados, sem `schema.sql` ou scripts DDL.
- RST-008: exportações, login e documentos funcionam após reinício; versões exibidas e hashes conferidos.

### 8.10. Desempenho e operação nacional (PERF-001..PERF-008)

O SGT já teve reclamação histórica de consulta nacional acima de 30 s; a campanha antiga usou massa pequena e não reproduziu condições equivalentes. Não inventar SLA nem declarar resolvido sem reprodução. Caracterizar **corpus** (empresas, contratos, pessoas, alocações, eventos, anos), **hardware**, **MySQL/Redis/Java**, concorrência, paginação, cache frio/quente, rede e queries, e medir **p50, p95, p99**, erro, throughput, payload, filas, memória, GC, locks, índices e contadores. Testar listagem nacional, painéis, busca paginada, histórico, frequência, fechamento com 200/chunk1 e 200/chunk500, exportação de 41 registros e volume maior quando houver massa comparável. Tratar performance ruim por causa demonstrada (profiling), não por bypass da regra temporal. Se faltar massa equivalente, declarar **testado apenas no corpus X**, gate nacional **NÃO EXECUTADO**.

## 9. Execução por fases com gates, checkpoints e regras de reexecução

**Não encerrar após análise, compilação, uma correção ou testes superficiais.** Trabalhar de forma incremental, preservando resultados e salvando na Biblioteca a cada etapa caso tenha acesso de escrita. Em cada marco, produzir evidência; quando gate não puder avançar, registrar impedimento e executar outros independentes. Os arquivos de saída devem ser gerados **nesta nova campanha**, nunca renomeando relatórios históricos.

| Fase | Trabalho real e decisões | Gate de saída e artefatos |
|---|---|---|
| **F00 — Baseline** | Recuperar quatro H02, seis H03, documentos, manifestos; extrair limpo e verificar hashes; inventariar arquitetura e dependências. | `CHECKPOINT_00_BASELINE.zip`, `IDENTIDADE_H02.json`, `INVENTARIO_ATIVOS.csv`, status H02 REPROVADO. |
| **F01 — Dependências/ambiente 8.0.45** | Resolver servidor MySQL 8.0.45 exato do acervo, Redis, Java/Node/Maven e Chrome. Executar INF-001..012; separar perfis/portas e segredos. | `CHECKPOINT_01_RUNTIME_MYSQL8045.zip`, `COMPROVACAO_MYSQL_8_0_45.md`, logs de processo/JDBC/Chrome. Se não iniciar, bloqueio explícito, sem fallback. |
| **F02 — Contratos e matrizes** | Inventariar endpoints reais, 21 módulos, 198 requisitos, 635 critérios, 64 ROP e 4+33 lacunas, inclusive remoções de Governança. | `CHECKPOINT_02_CONTRATOS.zip`, matrizes HTTP/REQ/ROP por predicado, inventário de regras/dados. |
| **F03 — RED isolado** | Reproduzir RI03-POST, PROR01, DOC-DTO, fechamento, ROP-064, sem alterar oráculos; registrar causas observadas e contraste H03. | `CHECKPOINT_03_RED.zip`, `MATRIZ_DEFEITOS_RED.csv`, logs/rede/SELECT/screenshot de cada caso, resultados FALHOU ou bloqueio real. |
| **F04 — Correções mínimas estruturais** | Alterar Java/React/testes de maneira controlada, sem refatoração geral ou regras novas; preservar credenciais/segredos fora dos pacotes. | `CHECKPOINT_04_IMPLEMENTACAO.zip`, diff por defeito, testes novos e justificativa do conjunto de arquivos alterados. |
| **F05 — GREEN por defeito** | Para cada alteração: unitário → integração 8.0.45 → HTTP no JAR → Chrome se UI → SELECT/JPA → segurança/regressão focal. | `CHECKPOINT_05_GREEN.zip`; tabela por defeito com prova RED, GREEN, estado banco e hash do candidato. |
| **F06 — Regra a regra** | Executar os cadernos M01..M21 e ROP-001..064; resolver contraprovas, FE-01/FE-02 e lacunas tecnicamente resolvíveis. | `CHECKPOINT_06_REGRAS.zip`, matriz 198/635, matriz ROP64 e componentes pendentes por predicado. |
| **F07 — Regressão total** | Maven clean verify UT+IT, React completo, build produção, lint/typecheck onde configurados, coverage, HTTP/TCP, acesso/permissões e emissão de arquivos. | `CHECKPOINT_07_REGRESSAO.zip`, XMLs frescos, logs, hashes, contagens reconcileadas. Falha = não promover. |
| **F08 — E2E real** | Reiniciar JAR correto, navegador Google Chrome fornecido, login/2FA/autorização reais, fluxos completos, 1440×900, HAR/trace sanitizados. | `CHECKPOINT_08_CHROME.zip`, `RELATORIO_E2E_CHROME_NOVA.md`, provas com ações→rede→MySQL. |
| **F09 — Concorrência/jobs/reinício** | CON-001..012, fechamento 200/200, cancelamento com prefixo não vazio, H03 sessão cross-tab, reinício e integridade, Redis. | `CHECKPOINT_09_RESILIENCIA.zip`, relatório de locks/threads/jobs/linhas e snapshots. |
| **F10 — Segurança/desempenho** | SEC-001..014, versões de dependências, exportações, temporalidade, desempenho p50/p95/p99 e limite de escala nacional. | `CHECKPOINT_10_SEG_PERF.zip`, auditoria de segurança e métricas com massa declarada. |
| **F11 — Congelamento final** | Parar compilações concorrentes, fixar fontes, gerar React produção, incorporar no backend, construir JAR ÚNICO e marcar hashes; extrair fontes e ZIPs de entrega de modo limpo. | `CHECKPOINT_11_ARTEFATOS_SELOS.zip`, `SELO_FINAL_BYTES.json`. Tudo imutável após selo. |
| **F12 — Re-homologação DOS BYTES FINAIS** | Executar **novamente** todos os testes impactados pelos últimos commits, UT/IT, Chrome real para fluxos alterados, versões MySQL/Redis, segurança, rollback/upgrade/reinício, e verificar que não houve alteração de byte desde o selo. | `CHECKPOINT_12_HOMOLOGACAO_FINAL.zip`, matriz de versões de teste, JAR final, hashes e resultado formal. Se nova correção, REFAZER F11/F12. |
| **F13 — Auditoria adversarial independente** | Revisor distinto (ou passada independente declarada) examina o código, diffs, logs, artefatos e falsos positivos, repete testes críticos e confronta pendências e removidos. | `PARECER_INDEPENDENTE_FINAL.md` assinado por evidências; não autocertificar independência inexistente. |
| **F14 — Publicação na Biblioteca** | Fazer upload dos arquivos finais e dos relatórios para pasta de entrega, conferir releitura por hash, fornecer caminhos reais e instruções reproduzíveis. | `RECIBO_PUBLICACAO_E_RELEITURA.json`, `00_LEIA_PRIMEIRO.md`; não afirmar upload sem confirmação. |

### Regra de repetição após alteração tardia

Se qualquer arquivo de produção, teste, `pom.xml`, lockfile, Vite config, asset, JAR ou configuração de execução for alterado **após** um teste, identificar quais provas ficaram obsoletas e **reexecutá-las**. Se o Java/React incorporado no JAR mudar depois do E2E, o E2E anterior **não homologa o novo JAR**. Se somente texto de relatório mudar, manter identidade dos binários e documento atualizado, mas conferir publicação. Se um gate tem execução interrompida, logs incompletos, fixture inválida ou versão MySQL diferente, **não** classificá-lo como aprovado por completar apenas testes relacionados.

### Estrutura de decisão RED/GREEN para cada correção

Para cada `DEF-xxx`, manter um `CASE_<ID>/` com:

1. `00_REGRA_E_CAUSA.md`: fato observado, expectativa, origem no negócio, hipóteses testadas, causa confirmada/indeterminada.
2. `01_RED_H02/`: script reproduzível, perfil, dados, timestamps, processo/JAR/React, versão MySQL, log de teste, HAR/trace/screenshot, snapshots antes/depois; se H02 exigir adaptação de infraestrutura para 8.0.45, separar patch de suporte de teste do código produtivo original.
3. `02_CORRECAO/`: commit/diff, arquivos alterados, invariantes e riscos de regressão.
4. `03_GREEN_DERIVADA/`: mesmo oráculo, dados controlados, servidor verdadeiro, assert UI/rede/banco.
5. `04_REGRESSAO_TRANSVERSAL/`: fluxos adjacentes, autorização, concorrência e casos negativos.
6. `05_GREEN_ARTEFATO_FINAL/`: **novo** recibo depois do selo final, hash do JAR/React idêntico à entrega.
7. `06_PARECER_REVISAO/`: revisão crítica: aprovado, falhou, parcial, não executado, bloqueado; efeito sobre requisitos/ROP/módulos.

A evidência RED histórica do MySQL 8.0.46 poderá ser citada **somente como antecedente**. Nova prova RED em 8.0.45, quando viável e segura, deve ser executada em cópia de teste da fonte baseline sem adulterar os hashes produtivos. Se o erro não reproduzir por ambiente ou mudanças de tempo, declarar limite e manter investigação, sem fabricar falha.

## 10. Matriz formal de rastreabilidade e classificação dos 198 requisitos / 635 critérios

**Não basta citar “198 requisitos e 635 critérios”.** Abrir a matriz completa e construir `MATRIZ_198_635_NOVA.csv` e `.md` com **exatamente 635 IDs únicos**, preservando **198 agrupamentos**, textos integrais e vínculo com regras e ROP. Conferir contagem por parser/inventário e registrar IDs ausentes, duplicados ou alterações de texto; não inventar IDs para completar 635. Campos obrigatórios:

```text
requirement_id,criterion_id,original_text,module_id,business_rule,
java_controller,java_service,java_entity,java_repository,java_dto,
http_method_path,react_component,
unit_case,integration_case,http_real_case,chrome_case,
mysql_observed,document_authenticity_required,
status_h01,status_h02,status_candidate,
red_receipt,green_receipt,final_artifact_sha256,
assertion_predicates,evidence_paths,unverified_part,conclusion_reason
```

**Status:**

- **APROVADO:** toda a conjunção de predicados técnicos daquele critério testada nos níveis aplicáveis e evidenciada nos **bytes finais**, respeitando `MySQL 8.0.45` e documentos autênticos onde necessários.
- **FALHOU:** comportamento efetivamente observado diverge de regra; descrever menor contraexemplo reproduzível, causa e estado após falha.
- **PARCIAL:** implementação/alguns predicados observados, mas falta parte significativa; indicar exatamente qual, sem declarar sucesso global.
- **NÃO EXECUTADO:** ensaio não realizado ou preparo não chegou à condição alvo; não confundir com FALHOU.
- **BLOQUEADO POR DOCUMENTAÇÃO:** documentos externos autênticos não disponíveis; ainda pode haver PASS técnico do mecanismo, que deve ser mostrado em coluna separada.
- **BLOQUEADO POR AMBIENTE:** dependência material não operacional (por exemplo MySQL 8.0.45 não inicializável), com causas e tentativa de diagnóstico.

A H02 histórica consolidou **598 com aceite técnico delimitado, 4 sem execução integral e 33 bloqueados por documentação**, apesar da reprovação global. Esse número **não** é meta para repetir mecanicamente e não certifica a nova derivada. Novos testes podem reclassificar itens para melhor ou pior, mas cada mudança precisa de prova.

Para cada um dos **21 módulos**, gerar `MATRIZ_MODULOS_NOVA.csv` indicando páginas React, controllers, regras, ROP IDs, fluxos que passaram UI/API/DB, pendências e status de módulo: mantido funcional no alcance, parcial, falho, bloqueado. Não considerar 21 módulos como equivalentes aos 20 drivers originais ou aos 64 ROP.

Para cada um dos **64 ROP**, gerar `MATRIZ_64_ROP_NOVA.csv` com dados iniciais, ação, resultado esperado, casos RED/GREEN, HTTP, Chrome, MySQL e conclusão por predicado. O **Anexo A** abaixo fornece os 64 casos e serve como checklist de execução; sua fonte canônica deve ser relida.

## 11. Artefatos de evidência e auditoria: contrato de qualidade do registro

### 11.1. Recibo mínimo por comando ou cenário

`run_id`, `case_id`, `utc_start/end`, timezone administrativo, hostname/ambiente isolado, cgroup/CPU/memória, PIDs/portas, versões Java/Node/Chrome/Playwright/Maven/MySQL/Redis, UUID MySQL, nome/URI do schema sintético (sem senha), manifestos de input, hash JAR/React/auxiliar, comando e argumentos sanitizados, exit code, quantidades de testes executados/falhados/ignorados, resultado **semântico** de assertions, localizador de cada screenshot/trace/log/SELECT, notas de riscos e limite. Em caso de erro de infra, manter o recibo e não trocar o resultado por um run posterior sem ID diferente.

### 11.2. Rigor sobre XMLs, console e dados

Conferir timestamp/frescor de XMLs de Surefire/Failsafe e JUnit Vitest; somar contagens e confrontar o comando/console integral. `exit=0` pode ser sucesso de script que só reuniu dados; validar assertions de negócio e número de testes. Não contar `skipped`, testes que não chegaram ao assert ou setups que falharam como aprovados. Quando haja comando interrompido, publicar status PARCIAL e lista exata de testes que completaram. Registrar se foi `MockMvc`, TCP real ou Chrome; não reclassificar artificialmente.

O banco só pode ser consultado para provas via **SELECT/leitura**; Hibernate/JPA faz DDL automático, nunca scripts manuais. Para campos temporais, comparar valores completos, datas efetivas, vigência, autoria, versão, evento e SHA de anexos antes/depois; um contador de linhas sozinho não prova integridade. Em testes de locks, demonstrar que a disputa ocorreu de verdade e não apenas em sequência.

### 11.3. Sanitização e segurança dos pacotes

Não publicar senhas, tokens, cookies, segredos 2FA, `storageState`, paths privados de configuração ou documentos reais sensíveis. Traces HAR/screenshots sanitizados devem preservar a causalidade da prova e registrar qualquer omissão; não retocar imagens para simular estado. Se sanitização inviabilizar replay completo, indicar limitação e preservar outras evidências independentes. Não eliminar logs de teste que falhou por contrariar o parecer final. Listar nomes/CRC/sha dos ZIPs, verificar extração limpa e integridade de arquivos HTML/JS/CSS/Java/ZIP.

## 12. Estrutura de pasta e entrega progressiva na Biblioteca

Criar nova derivada, sem alterar ou sobrescrever H01/H02/C25 originais e sem misturar com o diretório antigo `01_VERSAO_ATUAL`:

`/Projetos/SGT/09_CORRECAO_E_HOMOLOGACAO_INTEGRAL_MYSQL8045_20261103/`

```text
09_CORRECAO_E_HOMOLOGACAO_INTEGRAL_MYSQL8045_20261103/
├── 00_BASELINE_IDENTIDADE/
│   ├── 00_LEIA_PRIMEIRO_BASELINE.md
│   ├── BASELINE_CONGELADA_H02.md
│   ├── INVENTARIO_ARTEFATOS.json
│   ├── INVENTARIO_DEPENDENCIAS.csv
│   ├── H03_DIFF_E_AVALIACAO.md
│   └── SHA256SUMS_BASELINE.txt
├── 01_REGRAS_CONTRATOS_MATRIZES/
│   ├── MATRIZ_198_635_NOVA.csv
│   ├── MATRIZ_64_ROP_NOVA.csv
│   ├── MATRIZ_21_MODULOS_NOVA.csv
│   ├── MATRIZ_ENDPOINTS_JAVA_REACT.csv
│   ├── MAPA_REMOVIDOS_PRESERVADOS.md
│   └── REGRAS_TEMPORAIS_E_AUTORIZACAO.md
├── 02_CHECKPOINTS/
│   ├── CHECKPOINT_00_BASELINE.zip
│   ├── CHECKPOINT_01_RUNTIME_MYSQL8045.zip
│   ├── ... CHECKPOINT_12_HOMOLOGACAO_FINAL.zip
│   └── MANIFESTO_CHECKPOINTS_SHA256.txt
├── 03_CODIGO_CORRIGIDO/
│   ├── backend-java/
│   ├── frontend-react/
│   ├── PATCH_H02_PARA_DERIVADA_FINAL.diff
│   ├── MATRIZ_ARQUIVOS_ALTERADOS.csv
│   └── GUIA_BUILD_SEM_DDL_MANUAL.md
├── 04_TESTES_E_EVIDENCIAS/
│   ├── INFRA_MYSQL8045_REDIS_CHROME/
│   ├── JAVA_SUREFIRE_FAILSAFE_JACOCO/
│   ├── REACT_VITEST_BUILD/
│   ├── HTTP_REAL_AUTENTICADO/
│   ├── CHROME_E2E_HAR_TRACES_SCREENSHOTS/
│   ├── TEMPORALIDADE/
│   ├── CONCORRENCIA_IDEMPOTENCIA/
│   ├── FECHAMENTO_RECUPERACAO/
│   ├── DOCUMENTOS_PDF_XLSX_SEI/
│   ├── SEGURANCA_DESEMPENHO/
│   └── REINICIO_UPGRADE_PERSISTENCIA/
├── 05_AUDITORIA_INDEPENDENTE/
│   ├── MATRIZ_RED_GREEN_FINAL.csv
│   ├── PARECER_REVISAO_CODIGO.md
│   ├── PARECER_SEGURANCA.md
│   ├── RESULTADOS_REPRODUCAO_ADVERSARIAL.md
│   ├── LIMITES_E_BLOQUEIOS.md
│   └── PARECER_INDEPENDENTE_FINAL.md
├── 06_ENTREGA_FINAL/
│   ├── SGT_JAVA_FONTES_FINAL.zip
│   ├── SGT_REACT_FONTES_FINAL.zip
│   ├── SGT_REACT_BUILD_PRODUCAO_FINAL.zip
│   ├── sgt-backend-final.jar
│   ├── MANIFESTO_SHA256_FINAL.txt
│   ├── SELO_FINAL_BYTES.json
│   ├── RELATORIO_HOMOLOGACAO_FINAL.md
│   ├── COMPROVACAO_MYSQL_8_0_45.md
│   ├── MATRIZ_CORRECOES_HOMOLOGADAS.csv
│   ├── GUIA_REPRODUCAO_E_INSTALACAO.md
│   ├── NOTAS_DE_VERSAO_E_PENDENCIAS.md
│   └── RECIBO_PUBLICACAO_E_RELEITURA.json
└── STATUS_GERAL.md
```

**Salvar de verdade na Biblioteca** se houver permissão. Checkpoints devem ser materializáveis, ZIPs integralmente verificáveis por CRC/SHA, e o salvamento progressivo deve usar caminhos canônicos, não pastas aleatórias. Se a ferramenta não permitir upload, criar pacote para download e registrar explicitamente o que **não** foi salvo; não afirmar sucesso sem recibo. Evitar múltiplas pastas “FINAL” ou versões ambíguas. Para cada pacote registrar proveniência, versão/branch/commit quando houver, hash, data, conclusão e relação com o JAR.

### `STATUS_GERAL.md`: atualizar a cada marco

Campos obrigatórios: fase atual, início e último timestamp, baseline com SHA, versão/candidata derivada, MySQL esperado/observado e UUID, processos ativos/encerrados, últimas fontes modificadas, número de arquivos alterados, status 198/635/64 ROP/21 módulos, testes por camada **executados/pass/fail/skipped**, consumo de recursos, último checkpoint e SHA, defeitos ainda abertos, impedimentos, documentos pendentes, destino de cada artefato e próxima execução necessária. Status não é parecer, é rastro operacional.

## 13. Homologação de cada modificação, freeze e encerramento

### Regra de ouro de cobertura por mudança

Construir `MATRIZ_CORRECOES_HOMOLOGADAS.csv` com **UMA LINHA POR DEFEITO E POR ALTERAÇÃO DE CÓDIGO PRODUTIVO OU CONTRATO**, campos:

```text
change_id,defect_id,file_path,before_sha256,after_sha256,
reason,requirement_ids,rop_ids,affected_modules,
red_h02_case,red_observed,green_unit_case,green_it_mysql8045,
green_http_real_case,green_chrome_case,negative_case,concurrent_case,
before_sql_snapshot,after_sql_snapshot,redis_effect,
regression_suite_result,final_jar_sha256,final_react_assets_sha256,
reviewer_result,final_status,evidence_refs
```

Exemplo de interpretação: se H03 introduzir um listener `storage` e mudar o painel de prorrogação, **ambas** as mudanças exigem teste unitário, teste em Chrome e regressões diferentes; o fato de toda a suite React passar não prova o RI03 se o teste não reproduz troca real de abas e a ausência de PUT indevido. Se corrigir `DocumentoComprobatorioServico`, reexecutar histórico, arquivo, replay, optimistic lock e MySQL; uma comparação de campo `version` isolada não basta.

### Gate final binário

**APROVAR TECNICAMENTE A NOVA DERIVADA somente quando:**

- MySQL Server real **8.0.45** comprovado para **todos os testes dependentes de banco**, sem exceções ocultas; Redis real e Chrome real nos gates pertinentes.
- Todos os defeitos confirmados na H02 e todos os defeitos novos realmente encontrados foram corrigidos e seus RED/GREEN e casos adversariais estão rastreáveis (ou um achado foi honestamente reclassificado por prova de inexistência).
- Todas as regressões Java/React/HTTP/Chrome aplicáveis passaram nos **mesmos bytes finais**; zero erro ignorado/silenciado, zero teste inventado, zero falha de integração disfarçada de timeout benigno.
- Os 21 módulos, 198 requisitos/635 critérios e 64 ROP foram auditados com status fiel; nenhum requisito essencial passa com ausência de parte de seu predicado.
- Fechamento real, segurança, histórico, emissão de arquivos, concorrência, reinício, persistência, remoções de Governança e limites D+7 demonstrados no alcance declarado.
- O JAR final realmente contém os assets do React final; SHA-256 e ZIP CRC consistentes, fonte e binário rastreáveis, HTTP real serve a UI esperada.
- Uma revisão independente/adversarial examinou e contestou a entrega; suas objeções foram resolvidas ou declaradas.

**Se restarem os 33 critérios por falta de documentação administrativa autêntica, não declarar “homologação integral de negócio”**. A conclusão aceitável, se os demais gates técnicos estiverem todos verdes, é: **“HOMOLOGAÇÃO TÉCNICA CONCLUÍDA NO ESCOPO COMPROVADO, COM 33 CRITÉRIOS DOCUMENTAIS BLOQUEADOS”** (ajustando quantidade ao que foi realmente provado). Se ainda houver defeito funcional ou falha de gate crítico, concluir **REPROVADO**; se testes não executados, indicar **PARCIAL / NÃO COMPROVADO**, nunca “100%”. Esta tarefa **não é autorização para implantação em produção**.

### Relatório final obrigatório, com ordem fixa

1. Candidata exata e verificação SHA-256; origem H02, interpretação do H03 e versões finais Java/React/JAR.
2. Prova de MySQL Server 8.0.45 real, Redis e Chrome; ambiente, processos, drivers e dependências.
3. Lista completa de defeitos antigos e novos; causa, patch, RED, GREEN, regressão e resultado final.
4. Contagens **novas** unitárias/integração Java, React, HTTP, Chrome, skipped, falhas e XMLs/recibos frescos.
5. Resultado por 21 módulos e por 64 ROP com critérios não cumpridos.
6. Reconciliação dos 198 requisitos/635 critérios e seis dependências documentais.
7. Resultados de concorrência, temporalidade, fechamento 200/200, arquivo, segurança, desempenho e reinício.
8. Revisão independente/adversarial, divergências e riscos residuais.
9. Manifestos, pacote final, paths reais da Biblioteca e recibo de releitura por hash.
10. Decisão de aceite explícita com justificativa **e lista de impedimentos sem mascaramento**.

## 14. Primeira ação EXIGIDA do próximo chat

**EXECUTAR agora, não responder apenas com roteiro:**

1. Abrir a Biblioteca em `/Projetos/SGT/`, localizar os quatro artefatos H02 pelos SHA-256, o H03 e as dependências.
2. Registrar cópias imutáveis e o `CHECKPOINT_00` antes de qualquer edição; ler `MATRIZ_DEFEITOS_RED_GREEN_FINAL.md`, `LIMITES_E_PENDENCIAS.md`, `MATRIZ_198_635_COM_PROVAS` e `C25_MATRIZ_REGRAS_TESTES_OPERACIONAIS.md`.
3. Inspecionar o `.deb` oficial do MySQL 8.0.45, suas dependências, iniciar instância **isolada** e provar `VERSION()`/JDBC e processo. Não rodar em outra versão.
4. Verificar Maven/Node/Redis/Chrome, conferir arquivos de testes que ainda citam `8.0.46` e preparar adaptação limitada.
5. Reproduzir RI03, PROR01, DOC-DTO e fechamento, separar problema de produto vs instrumentação; corrigir no código e testar.
6. Continuar pelas demais fases, salvando checkpoints, até finalizar a melhor candidata **realmente comprovável**, sem afirmar sucesso por parecer anterior.

**Não repetir perguntas cujas respostas já constam nos arquivos. Não voltar a uma versão antiga. Não trocar o MySQL. Não fazer DDL manual. Não encerrar com uma promessa ou um prompt alternativo. Corrigir, homologar tudo o que for corrigido e entregar os artefatos recuperáveis com prova.**

---

## ANEXO A — OS 64 ROTEIROS OPERACIONAIS: DADOS, AÇÃO, ORÁCULO E ENDPOINTS

**Este anexo integra a ordem de execução e NÃO é um resumo de títulos.** Os campos abaixo foram extraídos da matriz operacional C25, que é uma referência HISTÓRICA. Revalidar o endpoint exato, autorização e pré-condições contra a fonte H02/H03, executar o procedimento em **MySQL 8.0.45 real**, preencher o recibo novo e acrescentar os negativos/concorrência/tempo exigidos no caderno dos módulos. Resultados de C25/H01/H02 nunca são transcritos como GREEN novo. O anexo preserva os requisitos de ação e oráculo, **sem transportar dados de infraestrutura antiga** que citavam MySQL 8.0.46. Onde um endpoint foi retirado por decisão de Governança, provar 404 autenticado, e não reintroduzi-lo.

Para **cada ROP abaixo**, criar no mínimo um caso com preparação válida, execução, resposta/DOM, snapshot de banco ou prova de ausência de estado, verificação de autorização e resultado; quando existir UI, também navegador real. Acrescentar variantes adversariais previstas nos módulos M01..M21 e matriculá-las explicitamente na matriz de 635 critérios. Se a preparação falhar antes da operação visada, classificar esse caso como **NÃO EXECUTADO/PRÉ-CONDIÇÃO INVÁLIDA**, não como comportamento aprovado nem defeito presumido.

### ROP-001 — Cadastro da empresa contratada

- **Preparação/dados iniciais:** Gestor Local ativo, CNPJ sintético livre, razão social e e-mail; senha/CSRF válidos.
- **Operação a executar:** Cadastrar empresa pela API operacional e pelo formulário.
- **Resultado esperado (oráculo de negócio):** Persistir identidade normalizada e ativo=true; DTO contém somente id, razão, CNPJ, e-mail e ativo; não criar risco/processo.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/fornecedores; GET /api/fornecedores`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-002 — Edição, inativação e reativação cadastral da empresa

- **Preparação/dados iniciais:** Empresa criada e contrato publicado que já a fotografou.
- **Operação a executar:** Editar razão/e-mail/ativo e consultar a versão contratual anterior.
- **Resultado esperado (oráculo de negócio):** Atualizar o cadastro sem apagar a empresa nem reescrever fotografias contratuais; não regravar o monitoramento legado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `PUT /api/fornecedores/{id}; GET /api/contratos/{id}/versoes`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-003 — Associação da empresa ao contrato e bloqueio após publicação

- **Preparação/dados iniciais:** Fornecedor existente, um rascunho e um contrato publicado.
- **Operação a executar:** Associar o fornecedor aos dois contratos pela mesma API.
- **Resultado esperado (oráculo de negócio):** Rascunho aceita; publicado retorna 409 e conserva empresa, versão e eventos; divergência CNPJ/instrumento é recusada.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-004 — Rascunho, composição e publicação contratual

- **Preparação/dados iniciais:** Gestor Local e catálogos próprios; instrumento, empresa e vigência explícitos.
- **Operação a executar:** Criar/editar rascunho, compor lotação+turno+cargo, gerar prévia e publicar; tentar edição livre posterior.
- **Resultado esperado (oráculo de negócio):** Publicação exige empresa/vigência/fonte/soma coerentes; congela a fotografia e impede edição livre; descarte permitido apenas nas condições de rascunho.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/contratos; PUT /api/contratos/{id}; POST /api/contratos/{id}/previas/publicar; POST /api/contratos/{id}/publicar`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-005 — Cargo e catálogos operacionais por gestão

- **Preparação/dados iniciais:** Cargo/turno/lotação da Gestão A; catálogo alheio B e uso futuro documentado.
- **Operação a executar:** Cadastrar/editar/inativar e consultar opções na data efetiva.
- **Resultado esperado (oráculo de negócio):** Normalização e escopo preservados; inativação não apaga ID/história e não invalida programação futura sem tratamento.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/cargos; POST /api/cargos; PUT /api/cargos/{id}; DELETE /api/cargos/{id}; GET /api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-006 — Fluxo empresa → contrato → pessoa/empregado → vínculo → cargo/alocação

- **Preparação/dados iniciais:** Empresa e contrato publicados; pessoa CPF sintético, catálogos válidos, matrícula conforme regra do tipo/contrato.
- **Operação a executar:** Cadastrar vínculo com pessoa e alocação inicial; ler Pessoa, Vínculo, Alocação e projeção Empregado; lançar frequência.
- **Resultado esperado (oráculo de negócio):** Um cadastro canônico, IDs relacionados corretamente, projeção operacional gerada pelo domínio; nenhum cadastro duplicado pelo cliente.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais; GET /api/pessoas/{id}; GET /api/vinculos-contratuais/{id}; GET /api/vinculos-contratuais/{id}/alocacoes; GET /api/empregados/{id}; POST /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-007 — Cadastro inválido e integridade de pessoa/matrícula

- **Preparação/dados iniciais:** CPF/nome inválidos, matrícula obrigatória ausente ou já reservada; controle válido.
- **Operação a executar:** Tentar cadastro integrado e recontratação sem substituir identidade/opcionais.
- **Resultado esperado (oráculo de negócio):** Recusar sem Pessoa, projeção, Vínculo, Alocação ou reserva parcial; matrícula pertence ao vínculo e permanece historicamente reservada.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais; POST /api/vinculos-contratuais/{id}/alterar-matricula`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-008 — Admissão programada, limites inclusivos e recontratação

- **Preparação/dados iniciais:** Vínculo futuro; outro vínculo com último dia conhecido; mesma pessoa em duas gestões.
- **Operação a executar:** Consultar antes/no início; tentar nova admissão no último dia e no dia seguinte.
- **Resultado esperado (oráculo de negócio):** Não antecipar vínculo/projeção operacional; último dia é inclusivo; sobreposição conflita, dia seguinte pode reutilizar a mesma Pessoa.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais; GET /api/vinculos-contratuais/pessoas/{pessoaId}/posicao; POST /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-009 — Alocação, mudança de cargo/turno/lotação e consentimento de excedente

- **Preparação/dados iniciais:** Vínculo vigente, composição válida e composição lotada; alteração em data definida.
- **Operação a executar:** Consultar prévia, confirmar mudança com consentimento quando exigido e cancelar programação futura.
- **Resultado esperado (oráculo de negócio):** Fechar faixa anterior no dia anterior sem desligamento; conservar história; excedente exige aceite específico e autoria; cancelamento restaura cobertura sem lacuna.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais/{id}/previas/alocacoes; POST /api/vinculos-contratuais/{id}/alocacoes; POST /api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/cancelar-programacao`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-010 — Presença sem batidas explícitas em jornada comprovada

- **Preparação/dados iniciais:** Vínculo/alocação vigentes na data, jornada fotografada e controle noturno.
- **Operação a executar:** Registrar PRESENCA sem inventar horário mestre retroativo.
- **Resultado esperado (oráculo de negócio):** Apuração utiliza fotografia da jornada e mantém identidade e memória física; falta de fonte não vira descanso fictício.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-011 — Presença integral por batidas físicas

- **Preparação/dados iniciais:** Dia com jornada 08–12/13–17, contrato/vínculo/alocação válidos e fonte nominal sintética comprovada.
- **Operação a executar:** Enviar PRESENCA com 08:00, 12:00, 13:00, 17:00; reler e consolidar.
- **Resultado esperado (oráculo de negócio):** PRESENCA, 480 minutos físicos; vínculo/alocação/contrato corretos, relatório coerente e evento com hash.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos; GET /api/relatorios/frequencia-consolidada`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-012 — Presença parcial por batidas físicas

- **Preparação/dados iniciais:** Mesmo cenário integral, saída às 15:00; cliente ainda solicita PRESENCA.
- **Operação a executar:** Enviar 08–12/13–15 e consultar memória física/consolidado.
- **Resultado esperado (oráculo de negócio):** Backend classifica PRESENCA_PARCIAL com 360 minutos; redução noturna não transforma presença fisicamente parcial em integral.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos; GET /api/relatorios/frequencia-consolidada`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-013 — Presença futura proibida inclusive ao Gestor Local

- **Preparação/dados iniciais:** Amanhã em America/Sao_Paulo; controle de Operacional e Gestor Local com contexto válido.
- **Operação a executar:** POST direto para a data futura.
- **Resultado esperado (oráculo de negócio):** 400 por datas futuras; nenhuma Frequencia ou EventoFrequencia persistido, sem exceção gerencial.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-014 — Sete dias: imediatamente antes do limite

- **Preparação/dados iniciais:** Data D = 01/01/2026; instante 07/01/2026 23:59:59 America/Sao_Paulo; registro não retificado por gestor.
- **Operação a executar:** Avaliar política e tentar alteração operacional antes de D + 7 às 00:00.
- **Resultado esperado (oráculo de negócio):** Janela aberta; dia da frequência conta como dia 1. Autorização/contexto continuam obrigatórios.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-015 — Sete dias: instante exato do limite

- **Preparação/dados iniciais:** Data D = 01/01/2026; instante 08/01/2026 00:00 America/Sao_Paulo.
- **Operação a executar:** Avaliar política exatamente em D + 7; tentar edição/criação pelo Operacional.
- **Resultado esperado (oráculo de negócio):** Política FECHADA no instante exato; escrita operacional proibida; não aguardar cron para bloquear.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-016 — Sete dias: após o limite

- **Preparação/dados iniciais:** D passado além de D + 7; Operacional e Gestor Local da gestão responsável.
- **Operação a executar:** Tentar criação/edição operacional e correção formal gerencial.
- **Resultado esperado (oráculo de negócio):** Operacional 403; Gestor Local pode corrigir com justificativa/chave válidas, preservando história. Não há prazo máximo gerencial encontrado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-017 — Fuso do prazo e contador visual

- **Preparação/dados iniciais:** Instantes 02:59:59Z e 03:00:00Z de 08/01/2026; cliente em fusos distintos; prazoEdicao sem offset.
- **Operação a executar:** Converter instante pela política e exibir contagem na UI.
- **Resultado esperado (oráculo de negócio):** Backend usa America/Sao_Paulo com conversão do mesmo instante. Contador deve informar o mesmo limite sem conceder autorização local.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-018 — Plantão noturno cruza meia-noite com mudança no dia seguinte

- **Preparação/dados iniciais:** Plantão 22–06, alocação A na data inicial e B no dia seguinte.
- **Operação a executar:** Lançar as batidas e consultar dia inicial/dia de saída.
- **Resultado esperado (oráculo de negócio):** Frequência e janela pertencem ao início; memória noturna preservada e alocação B não recebe parcela indevida.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos; GET /api/relatorios/frequencia-consolidada`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-019 — Correção gerencial formal dentro e depois da janela

- **Preparação/dados iniciais:** Frequência original e depois alterada por Operacional; Gestor Local legítimo.
- **Operação a executar:** Corrigir com motivo, chave e contexto válidos; reler eventos/autoria.
- **Resultado esperado (oráculo de negócio):** Nova versão, RETIFICADA_GESTOR e bloqueio operacional; autor original/fotografias anteriores preservados; dentro da janela não fechar antecipadamente.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-020 — Bloqueio gerencial restrito ao registro corrigido

- **Preparação/dados iniciais:** Dois registros distintos ainda ABERTA e editáveis; gestor corrige somente A.
- **Operação a executar:** Operacional tenta modificar A e B.
- **Resultado esperado (oráculo de negócio):** A retorna 403; B permanece editável se prazo/contexto permitem.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-021 — Justificativa gerencial, chave e entrada inválida

- **Preparação/dados iniciais:** Gestor Local; motivo ausente/curto, chave inválida; controle válido.
- **Operação a executar:** Enviar retificação e fechamento com payload inválido.
- **Resultado esperado (oráculo de negócio):** Recusar sem versão/evento parcial; motivo formal e chave válida obrigatórios; atestado sem justificativa no fechamento é recusado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; POST /api/frequencias/fechamentos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-022 — Ausência/FALTA, sem lançamento e absenteísmo

- **Preparação/dados iniciais:** Pessoa elegível com carga prevista; um dia FALTA e outro sem registro.
- **Operação a executar:** Consultar diária/mensal/intervalo e absenteísmo.
- **Resultado esperado (oráculo de negócio):** FALTA e SEM_LANCAMENTO permanecem distintos; população, minutos previstos e totais concordam sem duplicação por alocação.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias; GET /api/frequencias/mensal; GET /api/frequencias/intervalo; GET /api/relatorios/absenteismo/paginado; GET /api/relatorios/absenteismo/resumo`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-023 — Atestado e ocorrência com anexo sobre presença existente

- **Preparação/dados iniciais:** Presença física de 480 min já registrada; atestado sintético 1 dia com anexo classificado.
- **Operação a executar:** Criar ocorrência, consultar/baixar anexo e cancelar ocorrência autorizada.
- **Resultado esperado (oráculo de negócio):** 201 na criação; classificar ATESTADO sem perder batidas; cancelamento restaura classificação cabível, conserva ocorrência cancelada/anexo e três eventos da frequência.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/ocorrencias; GET /api/ocorrencias/{id}; GET /api/ocorrencias/anexos/{anexoId}; DELETE /api/ocorrencias/{id}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-024 — Ocorrência inválida e período que atravessa gestões

- **Preparação/dados iniciais:** Data futura, dias inválidos, anexo/classificação incompatível, ocorrência indivisível 2 dias atravessando responsabilidade.
- **Operação a executar:** Tentar criação pelos perfis/gestões pertinentes e controle autorizado.
- **Resultado esperado (oráculo de negócio):** Recusar período/futuro/escopo inválidos; autorizar todos os dias; não produzir fato, arquivo ou histórico parcial.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/ocorrencias; GET /api/ocorrencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-025 — Vínculo explícito correto, homônimos e projeção de outra pessoa

- **Preparação/dados iniciais:** Duas Pessoas/homônimos; IDs distintos; vínculo selecionado e empregado de origem divergente.
- **Operação a executar:** Selecionar vínculo manualmente e enviar origem incompatível em controle negativo.
- **Resultado esperado (oráculo de negócio):** Não pesquisar identidade por nome/CPF textual; backend 409 se empregado diverge da projeção do vínculo; UI mantém seleção explícita e dados após recusa.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos; GET /api/vinculos-contratuais/{id}/alocacoes/posicao`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-026 — Vínculo inexistente ou ID ausente

- **Preparação/dados iniciais:** ID de vínculo não persistido; payload sem vínculo canônico; nenhuma frequência anterior.
- **Operação a executar:** POST/fetch de posição com ID inexistente/ausente e controle válido.
- **Resultado esperado (oráculo de negócio):** Payload público sem vínculo 400; vínculo não encontrado 400; não escolher outro vínculo nem criar associação automática.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-027 — Vínculo fora da data efetiva, contrato inativo ou vínculo cancelado

- **Preparação/dados iniciais:** Pessoa com vínculo programado, encerrado/cancelado ou contrato historicamente inativo; controle vigente.
- **Operação a executar:** Registrar frequência em data sem cobertura.
- **Resultado esperado (oráculo de negócio):** 409 por ausência de vínculo operacional na data, inclusive com cadastro atual ativo; não antecipar admissão.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-028 — Alocação inexistente, cancelada ou incompatível com vínculo/data

- **Preparação/dados iniciais:** Vínculo vigente sem alocação comprovada, faixa cancelada ou alocação de outro vínculo.
- **Operação a executar:** Tentar frequência e consultar opções para data histórica.
- **Resultado esperado (oráculo de negócio):** 409 sem alocação Lotação+Turno+Cargo válida; não usar projeção atual nem associar faixa alheia.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-029 — Mudança cadastral de nome sem reescrever fotografias

- **Preparação/dados iniciais:** Frequência/ato publicado antes da correção de identidade.
- **Operação a executar:** Corrigir identidade e retificar frequência; consultar cortes anteriores/posteriores.
- **Resultado esperado (oráculo de negócio):** Identidade atual separada da histórica; autor/fotografia anterior preservados; desconhecimento não preenchido com nome atual.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/pessoas/{id}/correcoes-identidade; GET /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-030 — Mudança de cargo/turno/município não altera apuração histórica

- **Preparação/dados iniciais:** Alocação fotografada com grade de 480 min e município original; mestre alterado depois.
- **Operação a executar:** Consultar dia sem lançamento, lançar retroativo e consolidar antes/depois.
- **Resultado esperado (oráculo de negócio):** Carga/feriado/rótulos da data seguem fonte comprovada; não usar configuração mestre posterior como prova retroativa.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/relatorios/frequencia-consolidada`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-031 — Desligamento retroativo preserva presença, ocorrência e documento

- **Preparação/dados iniciais:** Frequência e ocorrência com anexo nos dias dentro/depois do fim efetivo.
- **Operação a executar:** Desligar/retificar fim e recalcular relatórios.
- **Resultado esperado (oráculo de negócio):** Último dia incluído; fatos posteriores preservados para auditoria, mas deixam cálculo oficial válido; pendência/revisão explícita, sem purga.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais/{id}/desligar; POST /api/vinculos-contratuais/{id}/retificar-desligamento; GET /api/frequencias; GET /api/ocorrencias/anexos/{anexoId}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-032 — Transferência de gestão e acesso histórico corrigido

- **Preparação/dados iniciais:** Contrato transferido com vínculo/frequência antigos; origem A, destino B e cortes de conhecimento.
- **Operação a executar:** Listar frequência, filtrar/paginar e consultar evento após transferência/retificação.
- **Resultado esperado (oráculo de negócio):** Responsabilidade efetiva autoriza o recorte correto sem reescrever fotografia; origem/destino não capturam período alheio; total paginado só inclui autorizado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/contratos/{id}/transferir-gestao; GET /api/frequencias/intervalo; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-033 — Fonte histórica tardia e desconhecimento preservado

- **Preparação/dados iniciais:** Publicação/composição com lacuna documental e prova posterior autorizada.
- **Operação a executar:** Incorporar prova por prévia/permissão e consultar antes/depois do conhecimento.
- **Resultado esperado (oráculo de negócio):** Completar só campos comprovados da mesma origem; não reescrever original nem certificar cadeia incompleta; seis bloqueios documentais da matriz 198 não se tornam resolvidos por existir código.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/contratos/{contratoId}/provas-historicas/previa; POST /api/contratos/{contratoId}/provas-historicas; GET /api/contratos/{contratoId}/provas-historicas`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-034 — Fechamento automático no oitavo dia

- **Preparação/dados iniciais:** Frequências ABERTA vencidas, fontes/eventos originais intactos e lotes limitados.
- **Operação a executar:** Executar fechamento e repetir; consultar estado/eventos.
- **Resultado esperado (oráculo de negócio):** FECHADA e evento encadeado uma vez; fonte/autoria preservadas; repetição não duplica; autorização já recusa prazo vencido antes do cron.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-035 — Fechamento assíncrono preserva comando e contexto

- **Preparação/dados iniciais:** Escopo elegível; alterações explícitas ou comando coletivo com motivo/chave.
- **Operação a executar:** POST fechamento, acompanhar GET até estado terminal, ler frequências/eventos.
- **Resultado esperado (oráculo de negócio):** 202, processamento após commit; CONCLUIDO, contadores corretos, motivo/chave/autoria do gestor preservados; usa alocação histórica.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias/fechamentos; GET /api/frequencias/fechamentos/{id}; GET /api/frequencias/fechamentos/atual`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-036 — Cancelamento, reprocessamento e recuperação de fechamento

- **Preparação/dados iniciais:** Job pendente/processando/erro/concluído e heartbeat vencido.
- **Operação a executar:** Cancelar/reprocessar estados permitidos, recuperar pendências e reiniciar processo.
- **Resultado esperado (oráculo de negócio):** Estado terminal/conteúdo/contadores coerentes; concluído não reprocessa; recuperação não duplica fatos e conserva MySQL como fonte.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias/fechamentos/{id}/cancelamento; POST /api/frequencias/fechamentos/{id}/reprocessamento; GET /api/frequencias/fechamentos/{id}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-037 — Perfis e escopos de leitura/escrita operacional

- **Preparação/dados iniciais:** Seis perfis canônicos; Gestões A/B e estados diferentes; usuário inativo/sem autorização.
- **Operação a executar:** Ler contratos/pessoas/frequência e tentar mutações por cada perfil.
- **Resultado esperado (oráculo de negócio):** Gestor Local escreve no próprio escopo; Operacional apenas capacidades de frequência; leitores nacionais/estaduais/presidenciais não herdam escrita; dados minimizados; fornecedor exclusivo Local.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/fornecedores; GET /api/contratos; GET /api/pessoas; POST /api/vinculos-contratuais; POST /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-038 — Documento contratual: upload, SEI, versão e invalidação

- **Preparação/dados iniciais:** Documento SEI/anexo sintético, tipos/classificações válidos e inválidos; rascunho/publicado.
- **Operação a executar:** Criar, anexar, retificar SEI, substituir/invalidar e consultar cadeia.
- **Resultado esperado (oráculo de negócio):** Nova versão preserva anterior; valida tipo/MIME/assinatura/sigilo; publicado não é apagado; rascunho tem regras próprias.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/contratos/{contratoId}/documentos; POST /api/contratos/{contratoId}/documentos/{documentoId}/retificar-sei; POST /api/contratos/{contratoId}/documentos/{documentoId}/invalidar; GET /api/contratos/{contratoId}/documentos/{documentoId}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-039 — Download restrito, finalidade, revogação e expiração

- **Preparação/dados iniciais:** Arquivo restrito íntegro, concessão válida/revogada/expirada, perfil consultivo e controle Local.
- **Operação a executar:** Baixar antes/depois da revogação/expiração e adulteração sintética.
- **Resultado esperado (oráculo de negócio):** Revalidar autorização atual antes de entregar bytes; registrar finalidade/acesso; recusa preserva arquivo e prova anterior; checksum adulterado bloqueia.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/contratos/{contratoId}/documentos/{documentoId}/arquivo; GET /api/ocorrencias/anexos/{anexoId}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-040 — Relatório, emissão histórica e invalidação de fonte alterada

- **Preparação/dados iniciais:** Frequência elegível, emissão original e posterior retificação/alteração operacional.
- **Operação a executar:** Emitir, mudar fonte e consultar emissão/corte de conhecimento/comparação.
- **Resultado esperado (oráculo de negócio):** Snapshot/hash/autor/período originais preservados; emissão anterior superada quando fonte muda; reenvio idêntico não invalida indevidamente.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/relatorios/administrativos/emissoes; GET /api/relatorios/administrativos/emissoes/{id}; POST /api/relatorios/administrativos/emissoes/conhecimento; GET /api/relatorios/administrativos/emissoes/{id}/comparacao-vigente`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-041 — Exportação XLSX/PDF síncrona e assíncrona

- **Preparação/dados iniciais:** Relatório com dados significativos e duas gestões; exportação pronta sob concessão.
- **Operação a executar:** Exportar todos os dados filtrados; baixar pronto após alteração de autorização.
- **Resultado esperado (oráculo de negócio):** Arquivo válido e conteúdo/quantidade/escopo corretos; exportação usa recorte completo e revalida autorização antes dos bytes.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/relatorios/frequencia-consolidada/exportacao; POST /api/relatorios/frequencia-consolidada/exportacao/solicitacao; GET /api/relatorios/exportacoes/{id}; GET /api/relatorios/exportacoes/{id}/arquivo`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-042 — Paginação preserva totais, cauda e filtros

- **Preparação/dados iniciais:** Seis empresas; 31 colaboradores; mais de 5 documentos e opções/cargos em páginas posteriores.
- **Operação a executar:** Navegar primeira/intermediária/última página, filtrar e exportar.
- **Resultado esperado (oráculo de negócio):** Cinco linhas visuais sem truncar universo; totais autorizados oficiais; nenhuma duplicação/perda de IDs; exportação não limitada à página visual.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias; GET /api/relatorios/frequencia-consolidada/paginada; GET /api/relatorios/absenteismo/paginado; GET /api/fornecedores`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-043 — Idempotência de frequência com/sem chave

- **Preparação/dados iniciais:** Mesma intenção operacional/gerencial, mesma chave e depois conteúdo diferente.
- **Operação a executar:** Reenviar sequencialmente e verificar versões/eventos; alterar intenção.
- **Resultado esperado (oráculo de negócio):** Mesmo reenvio não duplica fato/versão/autor; mesma chave com intenção diferente 409 sem mutação; nova alteração legítima é versionada.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-044 — Frequência concorrente, inativação de contrato e executor revogado

- **Preparação/dados iniciais:** Duas transações/threads; barreira SQL real; contexto/autorização mudam durante espera.
- **Operação a executar:** Enviar reenvios simultâneos e disputar frequência com inativação/revogação.
- **Resultado esperado (oráculo de negócio):** Um fato idempotente; não confirmar frequência válida com contexto velho nem executor revogado após lock.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias; GET /api/frequencias/{id}/eventos; POST /api/contratos/{id}/inativar`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-045 — Rollback de lote com item inválido/futuro

- **Preparação/dados iniciais:** Lote com primeiro item válido e segundo inválido/futuro.
- **Operação a executar:** POST/lote e reler frequências/eventos/histórico após falha.
- **Resultado esperado (oráculo de negócio):** Nenhum item confirmado nem auditoria de sucesso parcial; 400 na presença futura; controle lote válido cria ambos.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/frequencias/lote; GET /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-046 — Ocorrência: concorrência idempotente e rollback de arquivo/fato

- **Preparação/dados iniciais:** Mesma ocorrência/anexo/chave em duas transações; falha induzida antes do commit.
- **Operação a executar:** Criar simultaneamente; em cenário separado reverter criação.
- **Resultado esperado (oráculo de negócio):** Uma única fonte confirmada; rollback não deixa ocorrência/contextos/arquivo/histórico assinado parcial.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/ocorrencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-047 — Documento: rollback e concorrência da cadeia

- **Preparação/dados iniciais:** Documento rascunho com arquivo e duas concessões concorrentes.
- **Operação a executar:** Descartar com rollback e depois commit; disputar concessões.
- **Resultado esperado (oráculo de negócio):** Arquivo somente descartado após commit autorizado; rollback conserva; cadeia do documento não bifurca.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `DELETE /api/contratos/{contratoId}/documentos/{documentoId}; POST /api/contratos/{contratoId}/documentos/{documentoId}/permissoes`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-048 — Vínculos concorrentes e reserva global da Pessoa

- **Preparação/dados iniciais:** Mesma Pessoa, dois contratos; retificações ou cancelamento de desligamento contra nova admissão.
- **Operação a executar:** Executar transações simultâneas e reler períodos.
- **Resultado esperado (oráculo de negócio):** Não existir sobreposição global; exclusividade preservada após commit e em novo persistence context.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/vinculos-contratuais; POST /api/vinculos-contratuais/{id}/retificar-inicio; POST /api/vinculos-contratuais/{id}/cancelar-desligamento-programado`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-049 — Fornecedor: autorização corrente após espera de lock

- **Preparação/dados iniciais:** Gestor inicialmente autorizado; lock de contrato/fornecedor/CNPJ e revogação durante espera.
- **Operação a executar:** Associar/editar/criar, revogar antes de liberar a transação bloqueadora.
- **Resultado esperado (oráculo de negócio):** Revalidar executor; recusa sem alterações indevidas, inclusive após rollback do reservador de CNPJ.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}; PUT /api/fornecedores/{id}; POST /api/fornecedores`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-050 — GOV-003: auditoria geral e publicação após commit

- **Preparação/dados iniciais:** Operação bem-sucedida e operação revertida; Administrador para consulta geral.
- **Operação a executar:** Alterar domínio preservado, verificar auditoria após commit/rollback e ler API geral.
- **Resultado esperado (oráculo de negócio):** Somente fato confirmado gera sucesso; eventos, autor, data e histórico permanecem consultáveis; remoção do módulo não elimina auditoria compartilhada.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/registros-auditoria; GET /api/frequencias/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-051 — APIs antigas realmente removidas com controle autenticado

- **Preparação/dados iniciais:** Gestor Local válido, sessão e CSRF funcionais; GET /api/fornecedores retorna 200 como controle.
- **Operação a executar:** Invocar as dez rotas de resumo, fornecedores, associação, processos, status e riscos sob o prefixo antigo.
- **Resultado esperado (oráculo de negócio):** 404 com ausência de handler; 401/403 não demonstram remoção. As rotas operacionais continuam disponíveis antes e depois.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `REMOVIDAS /api/governanca-terceiros/*; GET /api/fornecedores`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-052 — UI removida, menu e nova página de empresas

- **Preparação/dados iniciais:** Aplicação React, Gestor Local e perfis sem acesso; URLs antigas com query e subrota.
- **Operação a executar:** Abrir rotas antigas e menu; acessar cadastro de empresas, pesquisar a sexta empresa e enviar cadastro.
- **Resultado esperado (oráculo de negócio):** Não renderizar governança, risco ou processo nem chamar endpoints removidos; o cadastro de empresas usa as novas rotas, confirmação de senha, CSRF e paginação.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `REMOVIDAS UI /governanca-terceiros e subrotas; GET/POST /api/fornecedores; PUT /api/fornecedores/{id}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-053 — Classes, entidades, DTOs e jobs exclusivos de risco retirados

- **Preparação/dados iniciais:** Classpath compilado e contexto Spring/metamodelo previstos para a candidata.
- **Operação a executar:** Inspecionar classes e DTOs; no runtime autorizado, verificar beans e agendamentos.
- **Resultado esperado (oráculo de negócio):** ProcessoTerceiro, AvaliacaoRiscoFornecedor e RiscoContinuoServico ausentes; APIs não expõem escores, processos ou monitoramento de fornecedor. O núcleo operacional permanece.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/fornecedores; REMOVIDAS /api/governanca-terceiros/riscos; REMOVIDAS /api/governanca-terceiros/processos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-054 — Jobs operacionais de frequência, pendências, painel e auditoria preservados

- **Preparação/dados iniciais:** Jobs e filas/pendências sintéticas; somente o risco exclusivo de fornecedor deve ser retirado.
- **Operação a executar:** Inicializar aplicação e observar agendamentos, execução após commit e recuperação; consultar resultados.
- **Resultado esperado (oráculo de negócio):** Fechamento automático e recuperação, pendências de vigência/revisão, painel e auditoria continuam ativos. Monitoramento operacional permanece.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias/fechamentos/{id}; GET /api/painel/calculos/{id}; GET /api/registros-auditoria`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-055 — Upgrade com dados legados preservados sem purga nem DDL manual

- **Preparação/dados iniciais:** Schema sintético criado pela baseline com Hibernate/update; fornecedores, riscos, processos, históricos e fotografias sintéticos.
- **Operação a executar:** Capturar contagens e hashes, iniciar candidata no mesmo catálogo e comparar após operações cadastrais.
- **Resultado esperado (oráculo de negócio):** Nenhuma perda de linhas, tabelas ou histórico; classes exclusivas deixam de ser ativas; dados compartilhados permanecem. UPDATE cadastral não regrava monitoramento legado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET/POST /api/fornecedores; PUT /api/fornecedores/{id}`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-056 — Ausência financeira previamente decidida permanece

- **Preparação/dados iniciais:** Baseline já sem tarifas, folhas monetárias e faturas; frequência e cadastro de fornecedor operacionais existentes.
- **Operação a executar:** Verificar metamodelo e handlers; chamar rotas financeiras retiradas com sessão autorizada.
- **Resultado esperado (oráculo de negócio):** Sem restauração de órfãos financeiros; 404 nas rotas retiradas; frequência e cadastro operacional continuam funcionando.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `REMOVIDAS /api/governanca-terceiros/tabelas-tarifarias; REMOVIDAS /api/governanca-terceiros/folhas-horas; REMOVIDAS /api/governanca-terceiros/faturas; POST /api/frequencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-057 — Painel e indicadores operacionais após retificação

- **Preparação/dados iniciais:** Cálculo assíncrono no escopo A e frequência que será corrigida.
- **Operação a executar:** Calcular, consultar por outro escopo, reutilizar cache e retificar a fonte.
- **Resultado esperado (oráculo de negócio):** Resultado concluído com dados do escopo correto e recusa ao escopo alheio; não apresentar cache antigo como atual depois da retificação.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/painel/calculos; GET /api/painel/calculos/{id}; GET /api/painel/efetivo`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-058 — HTTP do JAR empacotado com bundle novo

- **Preparação/dados iniciais:** Candidata com artefatos identificados; conta restrita **nova**, sintética, provisionada em servidor **MySQL 8.0.45**, conexão JDBC observada e recibo desta nova campanha. Não reutilizar a conta/teste legado 8.0.46 da matriz C25.
- **Operação a executar:** Iniciar o JAR com perfil real; autenticar, obter CSRF, confirmar senha e invocar rotas operacionais e retiradas por TCP.
- **Resultado esperado (oráculo de negócio):** Aplicação inicializa com Hibernate/update e bundle correto, sem classes/jobs exclusivos; respostas reais atendem aos contratos operacionais.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/fornecedores; GET /api/contratos; POST /api/frequencias; REMOVIDAS /api/governanca-terceiros/*`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-059 — Navegador real: fluxo operacional completo e rotas retiradas

- **Preparação/dados iniciais:** JAR, MySQL e React integrados; Gestor Local, Operacional e perfis de controle negativo.
- **Operação a executar:** Navegar empresa→contrato→pessoa/vínculo/alocação→frequência→ocorrência→relatório/exportação; verificar menus e rotas retirados.
- **Resultado esperado (oráculo de negócio):** Concluir o fluxo com identidade canônica, autorização, erros visíveis, histórico e arquivo correto; nenhuma chamada a recurso retirado.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET/POST /api/fornecedores; POST /api/contratos; POST /api/vinculos-contratuais; POST /api/frequencias; POST /api/ocorrencias; GET /api/relatorios/frequencia-consolidada/exportacao`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-060 — Reinício conserva fatos, histórico e jobs

- **Preparação/dados iniciais:** JAR com dados operacionais e legados sintéticos, job pendente e snapshots/hashes capturados.
- **Operação a executar:** Encerrar e reiniciar a aplicação; consultar os mesmos IDs, arquivos e jobs.
- **Resultado esperado (oráculo de negócio):** Dados, documentos e cadeias persistem; pendências são recuperáveis; nenhum score, processo ou job retirado é reativado; nenhum seed destrutivo.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/frequencias; GET /api/frequencias/fechamentos/{id}; GET /api/contratos/{id}/eventos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-061 — Listagem nacional: falha histórica existente

- **Preparação/dados iniciais:** Contratos de vários testes no mesmo schema histórico; IND08 executado antes do CRUD nas duas rodadas anteriores.
- **Operação a executar:** GET /api/contratos com perfil nacional no método CRUD real.
- **Resultado esperado (oráculo de negócio):** Se todas as cadeias visíveis são válidas, consulta 200; se houver cadeia inválida, 409 legítimo. Não impor 200 contra a validação.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/contratos`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-062 — Caracterização causal com restauração da única revisão sintética

- **Preparação/dados iniciais:** Catálogo sintético sem outro escritor; contrato com cadeia válida na revisão 1; servidor **MySQL exatamente 8.0.45**. Não usar a versão histórica 8.0.46 referida na matriz antiga.
- **Operação a executar:** Confirmar 200; alterar somente a prova própria de 1 para 3; exigir 409 individual/nacional; restaurar somente essa revisão em finally e confirmar 200.
- **Resultado esperado (oráculo de negócio):** 200→409→200; restauração restringida por IDs de contrato, evento e prova e predecessor nulo; não purgar nem relaxar a validação.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/contratos; GET /api/contratos/{contratoId}/provas-historicas; GET /api/contratos/{id}/versoes`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-063 — Isolamento entre IND08 negativo real e observador/CRUD

- **Preparação/dados iniciais:** Schema autorizado e controle inicial válido; método negativo IND08 real; nenhuma purga intermediária.
- **Operação a executar:** Executar o IND08 real seguido de observador/CRUD; comparar estado antes e depois do único dado sintético alterado.
- **Resultado esperado (oráculo de negócio):** Teste negativo individual continua retornando 409 durante a corrupção; ao encerrar, deve ter restaurado seu próprio dado sem deixar invalidade para o próximo teste.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `GET /api/contratos; GET /api/contratos/{contratoId}/provas-historicas`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

### ROP-064 — Recusas, confirmação cancelada e respostas antigas não simulam sucesso

- **Preparação/dados iniciais:** Formulário preenchido; respostas 409/403 ou erro; senha cancelada; logout ou troca de data com requisição pendente.
- **Operação a executar:** Enviar ou consultar, cancelar confirmação, trocar contexto e resolver a resposta antiga.
- **Resultado esperado (oráculo de negócio):** Preservar intenção e formulário; mostrar recusa real; não apresentar lista vazia ou sucesso indevido; não enviar confirmação vencida nem repopular outra sessão.
- **Endpoints de referência da matriz C25 (confirmar na implementação atual):** `POST /api/fornecedores; POST /api/frequencias; GET /api/ocorrencias`
- **Prova nova obrigatória:** testar comportamento real, asserções do oráculo completo, autorização do ator, HTTP/persistência/arquivo quando pertinente, impacto no histórico/auditoria e caso negativo relacionado; adicionar Chrome real se for uma jornada de interface. Registrar o `ROP-ID`, a versão MySQL 8.0.45 efetiva, hash do JAR/React, o recibo e o resultado por predicado. Não herdar status da C25.

**Controle de completude:** esta lista contém exatamente ROP-001 até ROP-064 uma vez cada; preencher `MATRIZ_64_ROP_NOVA.csv` com 64 linhas de cenário e expandir os casos/subpredicados sem alterar a contagem canônica. Em especial ROP-064 continua exigindo contraprovas para erros e respostas antigas; ROP-059 exige **navegador real**, não jsdom apenas.
