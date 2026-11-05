# Mapa de removidos e preservados — candidata04

**Conclusão estática:** as famílias retiradas identificadas no contrato não possuem mappings declarados no inventário atual; os tipos exclusivos de risco examinados estão ausentes das fontes e das classes da aplicação no JAR. Fornecedor operacional, contratos, frequência, auditoria e rotinas operacionais selecionadas permanecem declarados e empacotados. **Isso não prova404, autorização, cache, beans ou jornadas Chrome em execução.**

JAR `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d`; selo `ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79`; React600. Regra: mestre§5, com contexto§0; ROP-051/052/053/054/056 e E2E-21. Este corte deriva dos mapas15/17, preservados, e revalida os bytes atuais. O arquivo `MAPA_REMOVIDOS_PRESERVADOS_PROVENIENCIA.json` contém caminhos, linhas, hashes e limites completos.

## Limite de remoção e preservação

| Área | Observação estática atual | Fluxo preservado / limite |
|---|---|---|
| Governança de terceiros e risco | Sem mapping sob `/api/governanca-terceiros`; sem declarações ou classes da aplicação denominadas `ProcessoTerceiro`, `AvaliacaoRiscoFornecedor`, `RiscoContinuoServico` | Cadastro operacional de Fornecedor e infraestrutura compartilhada de auditoria continuam presentes. Não se infere ausência de todo bean por busca de nome. |
| Famílias financeiras identificadas | Sem mappings sob `/api/tabelas-tarifarias`, `/api/folhas-horas`, `/api/faturas` | Esses prefixos são vetores de remoção do contrato/testes recebidos, não endpoints ativos inventados. Remuneração/benefícios e risco operacional do painel não são automaticamente esse módulo retirado. |
| React: rotas e menu |28 declarações Route sem `/governanca-terceiros`; fontes de aplicação/menu idênticas ao mapa revisado | `/empresas-contratadas`, `/contratos`, `/frequencia` e `/logs` continuam declaradas. A rota curinga não prova404HTTP, subrotas, query ou comportamento Chrome. |
| Fornecedor operacional | Quatro mappings reais e quatro chamadas correspondentes em `ServicoEmpresaContratada.js` | Listar,cadastrar,atualizar e associar a rascunho; Gestor Local declarado na classe; confirmação de senha nas mutações. Autorização real ainda exige execução. |
| Contrato,frequência e auditoria | Controladores/serviços e classes compiladas selecionados permanecem no JAR;23 mappings de Contrato,7 de Frequência,5 de Fechamento,2 de RegistroAuditoria | Declarar e empacotar não comprova execução/commit ou operação sem regressões. |
| Agendamentos e cache |13 fontes com `@Scheduled` preservadas; incluem fechamento negocial,recuperação,sincronização de pendências,fila de auditoria,exportação e manutenção operacional |13 fontes não é contagem de jobs ativos. O prefixo antigo de governança é deliberadamente excluído pelo filtro de cache; exige ensaio novo com payload legado200/HIT e404 autenticado. |

## Endpoints operacionais de Fornecedor

| Verbo e caminho declarado | Handler | Chamada React |
|---|---|---|
| `GET /api/fornecedores` | `fornecedores` | REACT-052 |
| `POST /api/fornecedores` | `criarFornecedor` | REACT-053 |
| `PUT /api/fornecedores/{id}` | `atualizarFornecedor` | REACT-054 |
| `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}` | `vincularFornecedor` | REACT-055 |

Fonte Java: `work/backend/src/main/java/com/example/siggtrat/controlador/FornecedorControlador.java`, SHA `614dd2fb349d884538ea8046e85dacb9992c76116e222ad443b087efd3cc4b6d`. Fonte React: `work/react/src/services/admin/ServicoEmpresaContratada.js`, SHA `bb307b409bdb5de19712648264c5ec9644c5d80235c445a694494db3280307af`.

`FornecedorServico` conserva validações de cadastro,escopo,CNPJ,associação ao rascunho e revalidação após espera. A entidade mantém o campo privado de compatibilidade `estadoMonitoramentoLegado` ligado a `monitoramento_status`, sem exposição no DTO operacional e sem atualização pela aplicação. Isso não reinstala avaliação de risco nem autoriza apagar coluna/dado legado.

## Proveniência e contagens

A matriz de endpoints possui **299 linhas:260 declarações Spring +39 registros React sem vínculo demonstrado**. Não são299 endpoints Spring. Os 44 arquivos com mapping — 43 classes @RestController no pacote controlador e ConfiguracaoReact, @Controller no pacote configuracao — e as 28 rotas React foram reconferidos por hash, sem alterações frente ao inventário15. Essa contagem não substitui a classificação de 43 controladores e 40 configurações do inventário de componentes. Os603 caminhos de fontes Java são os mesmos. Dentre as872 fontes Java/JS/JSX do inventário anterior, somente `FrequenciaServico.java` tem hash atual diferente; o delta consta da proveniência. Não se reaproveita seu hash anterior. As600 fontes React conferem integralmente com o manifesto atual.

O exame do JAR é leitura de ZIP: três nomes exclusivos ausentes e14 classes operacionais selecionadas presentes em `BOOT-INF/classes`, com hash de cada membro. Não é carregamento de classe nem inventário de dependências,beans,schedulers ou metamodeloJPA em execução. As referências residuais `ServicoBanco`/`ServicoAgencia` não demonstram mapping Java ou montagem de UI; sua existência não é transformada em rota ativa.

Referências principais, preservadas por hash na proveniência: `frontend_evidence/15_ENDPOINTS_E_REGRAS/MATRIZ_ENDPOINTS_JAVA_REACT.csv`; `ENDPOINTS_SPRING_LITERAL.json`; `ROTAS_REACT_DECLARADAS.json`; `REGRAS_AUTORIZACAO_HTTP_ORDENADAS.json`; mapa corrigido§5 em `frontend_evidence/17_REVISAO_FOCAL_MAPA/MAPA_REMOVIDOS_PRESERVADOS_V2.md`; parecer focal independente `review_notes/VERIFICACAO_FOCAL_MAPA_ENDPOINTS_20261103.json`.

## Gates pendentes do ensaio atual

Permanecem pendentes os controles reais de remoção/cache/CSRF do driver nativo:404 autenticado das rotas retiradas após cache200/HIT, controle positivo de Fornecedor, mutações com senha/CSRF e perfis distintos, preservação de valor/TTL conforme protocolo, inventário de beans/jobs e observação dos fluxos operacionais. HTTP401/403 isolado não fecha remoção. O novoChrome deve observar menu,URLs antigas,subrotas e recuperação dos fluxos legítimos; ausência de arquivo ou presença de asset não substitui essas jornadas.

Os 23 casos de `remocao-governanca.test.jsx` foram conferidos PASS no XML fresco de F12_REACT_COVERAGE_01, ligado ao JAR6ab, na camada unidade/jsdom. O F12 anterior permanece histórico. Os ensaios `F12_NATIVE_FINAL04_02`/`F12_CHROME_FINAL04_02` ainda aguardam seus recibos neste corte. Nenhum PASS de HTTP/Chrome ou resultado operacional novo é atribuído aqui.

Nenhum produto,configuração,selo ou teste foi alterado; nenhum Node,JVM,HTTP,SQL ou Chrome foi iniciado nesta preparação.

## Nota de preservação documental

A versão anterior deste mapa, SHA `79348a01df08dbe7d8f54797bb4cf4832854675e47b6cd8644429f5b62f08679`, e sua proveniência foram copiadas integralmente para o histórico do corte30. Esta revisão esclarece a definição de44 arquivos de rotas e vincula os23 casos unitários à execução fresca; não muda fonte,rota,regra ou resultadoHTTP/Chrome.
