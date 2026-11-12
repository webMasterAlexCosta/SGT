# Homologação final Sol — R4

**Decisão: APROVADO no alcance funcional do laboratório sintético desktop demonstrado, para os bytes identificados abaixo. Os achados P1 e P2 estão rechecados e encerrados nesse alcance; não identifiquei novo achado P0–P3 nas verificações realizadas.**

Este parecer substitui a decisão pendente de `PARECER_PARCIAL_SOL.md` e os pré-checks. É aceite do checkpoint do dashboard/frequência e de suas integrações pertinentes no laboratório; não declara todo o catálogo histórico ou o projeto pronto para produção. As limitações abaixo integram a decisão.

## Identidade do checkpoint

| Item | Evidência conferida |
|---|---|
| Homologador | Chat Desktop `01a1cd63-d762-7483-aaee-315114ff5781`; execução `gpt-6.1-sol/xhigh` comprovada em `turn_context` local, não por configuração salva. |
| Fontes R4 | 80 arquivos; manifesto SHA-256 `baba3f76a8d02cf1137d78739fa1befb861e4126c226ab4c5b38f3045fc2120e`, sem divergências na conferência final. |
| JAR | SHA-256 `b1b87261111a5ac182b498a2fcb93a3f4c986f22795e593a0bf66a3bd53eb814`; cópia imutável em `artefatos/`, executada pelo serviço exclusivo `sgt-dashboard-api-r4-homologacao-20261111`. |
| Integração | 866 classes de `target/classes` e 111 arquivos de `React/dist` comparados byte a byte com o JAR/static; nenhuma divergência. Os 111 arquivos também foram requisitados por HTTP e conferidos por hash. |
| Revisão independente | `DOCUMENTACAO/CODEX/DASHBOARD_20261111/PARECER_ASTRA_R4.md`, aprovado quanto às fontes; limites daquele parecer foram respeitados. |

## Rechecagem dos achados

**P1 — 500 na solicitação de revisão: encerrado.** O erro anterior está comprovado no log do JAR R2. No JAR R4, o log registra o desafio de senha 428 e depois `POST /api/frequencias/190/revisao -> 200`; a decisão também passou por 428 e depois 200. O principal executou a jornada real: Operacional solicita, KPI passa a uma revisão pendente, Gestor retifica PRESENCA→FALTA com justificativa e conclui CONFERIDA; o KPI volta a zero, mantendo um registro e sua classificação dentro do prazo. Avaliei o recibo `jornada-revisao-ui-r4.json` e as quatro capturas correspondentes. Não executei essas mutações pelo navegador.

Após a liberação explícita do ator Operacional para leituras, conferi por API própria o registro 190: FALTA, versão negocial 4, revisão CONFERIDA, validade VALIDA, auditoria RETIFICADA_GESTOR e bloqueio de edição operacional. O estado negocial ABERTA é um eixo independente e não desfaz esse bloqueio. A cadeia tem REGISTRO→REVISAO_SOLICITADA→RETIFICACAO_GESTOR→REVISAO_DECIDIDA; hashes e encadeamento íntegros. Motivos próprios e decisão são legíveis ao Operacional autorizado, que não pode decidir a solicitação. Os 24 checks dessa leitura passaram.

Snapshots e consulta SQL limitada ao registro 190 confirmam os mesmos prazos ao longo da cadeia: lançamento fecha em 18/11/2026 00h São Paulo; edição fecha em 14/11/2026 22:44:11.384196 São Paulo, exatamente 72 horas desde o primeiro lançamento em 11/11/2026 22:44:11.384196. Autoria original é distinta do Gestor retificador. Não houve renovação de prazo por revisão/retificação.

**P2 — vencimento contratual ausente: encerrado no alcance comprovado.** O DTO e a tabela agora publicam `vigenciaFim`, obtida do motor canônico. As quatro vigências retornadas pela API coincidem com as respectivas posições temporais canônicas; filtros de contrato/prestador e partição dos totais foram conferidos. A captura `contratos-vigencia.png` demonstra a coluna e as novas exportações contêm contratos, prestadores e vigências. O IT PostgreSQL de R4 exercita vigência canônica diferente da projeção cadastral, inclusão de +30 dias e exclusão de +31. A fixture UI atual tem vencimentos em 31/12/2027: confirma a ausência correta de alerta na janela atual; o disparo positivo do alerta foi exercitado no IT, sem alegar demonstração positiva desse alerta no navegador.

## Matriz proporcional e origem da evidência

| Área | Verificação e resultado |
|---|---|
| Calendários e KPIs | Execução HTTP própria nos cinco períodos; diário padrão; intervalos reais; presença + ausência = total; dentro + fora + indeterminado = total; acervo indeterminado preservado; evolução reconciliada; contexto diário de sete dias separado do KPI. |
| Cobertura, ocupação e admissão | Bases e conservação por contrato, lançados + pendentes = esperados, três metas explicitamente propostas; fotografia da referência e interpretação cadastral preservadas. O heatmap mensal nacional reconcilia 34 pendentes + 162 lançados = 196 esperados. Na gestão origem, o arquivo reconcilia 28/189 = 14,81%, por soma das bases. |
| Autorização e minimização | Presidente em agregados, detalhe analítico recusado; gestor externo sem contratos/unidades da origem; 403 preserva sessão; detalhe mínimo/página de 20; intervalo/parâmetros inválidos recusados; POST sem CSRF recusado antes do domínio. No módulo temporal de contratos, recurso alheio retorna o 404 previsto pela política, sem encerrar sessão. |
| Módulos existentes | Leituras próprias reais de gestão, efetivo, contratos, empregados e ocorrências; SPA/rotas reais, assets, licença e modelos servidos pelo JAR. Não se infere homologação de todas as mutações históricas desses módulos. |
| API e infraestrutura | 77/77 checks principais R4 aprovados. Consulta complementar de contratos verificou motor, filtros, partição e bases. PostgreSQL 18.6 e Redis 8.10.3 reais; role limitada e ownership de ambas as bases confirmados, sem privilégios administrativos; Redis autenticado/PING/versão. Nenhum valor de chave foi coletado. |
| UI real pelo principal | Nova execução `e2e-dashboard-ui.json`: gestão origem 23/47/161/188/188, cinco períodos, origem mínima, ESC/foco, heatmap, funil, sete SVG locais e exportações. Recibo e capturas reais avaliados aqui; não se declara execução própria do navegador. |
| Desktop e footer | `viewports-desktop-r4.json`: 1366×768, 1440×900 e 1920×1080 sem overflow e sete ícones carregados localmente. Capturas dashboard, contratos e footer inspecionadas; quatro KPIs e a nova vigência estão visíveis, com atribuição Devicon MIT. |
| Exportações R4 | XLSX 40.534 bytes, 118 linhas; PDF 100.642 bytes, seis páginas. Contagens, escopo, comparação, bases, propostas, vigências, alertas existentes e metodologia reconciliados. Leitura e renderização próprias: resumo/metodologia XLSX e todas as páginas PDF. |
| Checks herdados do mesmo checkpoint | Logs conferidos por hash: `r4-package.log`, 800 unitários Java; `r4-regressoes.log`, sete unitários e oito ITs PostgreSQL/HTTP; `r4-front-completo.log`, 860 testes em 109 arquivos, seis testes de preparação, typecheck e build. Todos aprovados. Essas suítes foram executadas pelo principal, sem reexecução pesada nesta tarefa. O novo unitário de revogação integra o package final. Não existe script lint. |

Os roteiros exploratórios e seus diagnósticos foram preservados. As expectativas incorretas do homologador — rota não existente, ocorrência sem data/empregado, 403 versus 404 de outro contrato de leitura, representação de fuso e confusão inicial entre estado negocial/auditoria — não foram tratadas como defeitos de produto. Os recibos finais distinguem essas correções das verificações aprovadas.

O log R4 também registra uma interrupção de cliente (`AsyncRequestNotUsableException`/`ClientAbortException`/`IOException`) durante GET analítico, com registro HTTP 200 e sem falha negocial demonstrada. Não foi confundida com o antigo NPE do comando de revisão nem usada para afirmar ausência absoluta de mensagens ERROR.

## Limitações integrantes do aceite

- **Zoom nativo não comprovado.** As tentativas Ctrl+plus/Ctrl+equal no IAB não alteraram innerWidth/DPR. As provas 1152×720 e 720×450 são equivalentes CSS de layout a 125%/200%, não testes de zoom nativo. O aceite cobre os desktops efetivamente demonstrados, conforme o alcance final explicitado pelo principal.
- Não se executou revogação concorrente física enquanto uma requisição aguardava lock. Há unitário de recarga/revogação e HTTP com autenticação/vínculos reais; não se promove isso a prova de corrida temporal completa.
- Não houve espera física de 72 horas nem travessia de meia-noite. Os limites têm regressões próprias, e os prazos capturados foram lidos no runtime real.
- O alerta positivo de vencimento tem prova no IT PostgreSQL; a base UI atual está fora da janela. Não se inventa contrato próximo de vencer nem se altera a fixture para produzir imagem.
- Excel nativo não executado; textos longos usam overflow e podem exigir seleção/navegação na planilha. PDF tabular sem tags; PDF/UA integral não está aprovado. Aviso de chunk grande e avisos técnicos dos logs não são convertidos em falhas funcionais sem evidência.
- Não há aprovação de produção, implantação, Windows nativo, LAN externa, carga nacional, autenticidade institucional ou de todo o catálogo histórico. Não se atribui um percentual global de cobertura de código ou uma certificação WCAG a esses recibos.

## Integridade da atuação e recibos

Somente este diretório de evidências foi escrito. Não alterei produto, assets, configurações operacionais, credenciais, banco ou histórico; a consulta SQL de prazos foi somente leitura. Atores 0/3 foram reservados ao principal durante sua jornada, e o ator 3 só foi autenticado após liberação explícita. Não controlei navegador, não criei agentes/tarefas e não enviei mensagens a outros chats. Não houve commit, push ou deploy por esta tarefa.

As suítes pesadas foram reaproveitadas sem repetição. O serviço da API tem limite 2 GiB, heap 768 MiB/metaspace 256 MiB; os recursos exclusivos permanecem dentro dos tetos registrados. A última franquia observada foi 20% restante, acima da pausa obrigatória em 1%; nenhum crédito de reset foi resgatado.

Recibos próprios principais: `integridade-r4.json`, `integridade-final-r4.json`, `api-real-r4.json`, `contratos-r4.json`, `diagnostico-contratos-r4.json`, `revisao-r4.json`, `checks-herdados-r4.json` e `exportacoes-r4.json`. Exportações copiadas neste diretório preservam os bytes examinados. A consolidação documental, auditoria Git/segredos e eventual commit/push continuam sob responsabilidade do principal e da autorização humana já existente; não foram executados nem certificados aqui.
