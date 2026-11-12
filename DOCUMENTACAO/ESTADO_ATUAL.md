# SGT — estado atual

**Correção de paridade em andamento — 12/11/2026:** o usuário rejeitou a entrega abaixo porque Resumo, Contratos e Operação não seguiam o padrão completo de Desempenho. A implementação nova usa cinco calendários e recorte comum validado no servidor; não considerar o trabalho concluído antes de [encerrar as verificações desta correção](CODEX/DASHBOARD_20261111/CORRECAO_PARIDADE_20261112.md).

**Entrega das demais visões e login em 12/11/2026:** Resumo, Contratos e Operação refinados; Governança legada retirada; padrão Diário em todas as visões/endpoints correspondentes; login refatorado sem modo visual; crédito Alex Costa 89618815 e Maven/JAR SGT. Suíte aplicável reexecutada: 879 React, 808 unitários Java, 1.598 ITs na consolidação por caso e 137 Python, sem falhas pendentes. JAR `13ec71f30cba4385cfac62cb9604355b729eb2439203ba64f5700ca8c8fb6490` executado a partir de `JAVA/target`, porta 58501, com 110 assets conferidos por HTTP. Desempenho preserva seus oito arquivos de fonte. Ver [escopo, reexecuções, capturas e limites](CODEX/DASHBOARD_20261111/PAINEL_GERAL_LOGIN_20261112.md) e [recibo](CODEX/DASHBOARD_20261111/PAINEL_GERAL_LOGIN_20261112.json). Sem homologação de produção.

**Build e execução anteriores de 12/11/2026 — históricos:** checkpoint prévio `c9a1d0fb` publicado antes das alterações; React compilado, 111 arquivos de `dist` copiados para `static`, Maven wrapper `clean package` com 808 testes sem falhas e JAR `13dda6173b8af06d371ee1264d456c0b70dd5e5a169fdd65b88fc58324e358d8` executado a partir de `JAVA/target` no laboratório, porta 58501. Cinco rotas SPA, 111 assets e API conferidos. Ver [recibo desta execução](CODEX/DASHBOARD_20261111/BUILD_EXECUCAO_20261112.json).

**Iteração anterior de UX de Desempenho em 12/11/2026 — histórica:** filtros, quatro KPIs, evolução/comparação, metas, heatmap, funil, diagnóstico, alertas, navegação e detalhes, preservando cálculos e contratos. Substitui a iteração parcial restrita às três barras. Alteração local posterior ao commit da auditoria; JAR `bdf997e2cec5b5605a64f29b541ead4ea0c5aaa693eb1be67c757011f85cccc4` conferido no laboratório. Ver [escopo, testes, capturas e limites](CODEX/DASHBOARD_20261111/PAINEL_UX_COMPLETO_20261112.md).

**Auditoria de 12/11/2026 concluída no alcance do repositório e do laboratório isolado**, com 14 achados corrigidos. Passaram 808 testes unitários Java, 875 React e 1.597 ITs na consolidação da última execução por classe; os ITs não correspondem a uma única execução integral sobre os bytes finais. JAR `963677cb995456c40b3f61a2865d8dfe1b8be93e8f8be9b2ae11861b4a6789d4` integrado e conferido por HTTP/navegador, com ensaios de recuperação e 200 registros abaixo de 120 segundos. Ver [plano](CODEX/AUDITORIA_20261112/PLANO.md), [relatório e limites](CODEX/AUDITORIA_20261112/RELATORIO.md) e [recibos](CODEX/AUDITORIA_20261112/EVIDENCIAS.json). O checkpoint anterior foi enviado ao origin antes das alterações; as correções e a documentação foram publicadas posteriormente no commit `23dd2437db64036a0287365a183ab4e077df9054`, por pedido humano explícito. Sem homologação de produção ou revisão independente desta auditoria; resultados anteriores não aprovam seus bytes.

**Campanha de 09/11 (histórica): homologação funcional integrada do laboratório aprovada, em Chrome/Chromium desktop, pelos pareceres independentes backend26 e frontend12. Sem P0–P3 nem obrigação aplicável remanescente no recorte examinado.** [Alcance e evidências](CODEX/CAMPANHA_20261109/HOMOLOGACAO_FUNCIONAL_LABORATORIO.md). Produção e implantação não foram homologadas ou autorizadas.

## O que existe

Backend Java/Spring, frontend React/TypeScript e a derivada PostgreSQL, com Redis nos cenários previstos. A toolchain atual e os recibos técnicos estão em [CODEX/TOOLCHAIN.md](CODEX/TOOLCHAIN.md). A organização documental não alterou nem executou a aplicação.

## Como interpretar as evidências

O [resumo C25-H04](01_RESUMO_REQUISITOS_E_REGRAS_DE_NEGOCIO.md) e as [notas da candidata](AUDITORIA_E_PENDENCIAS/NOTAS_DE_VERSAO_E_PENDENCIAS.md) registram um corte anterior, MySQL, parcial/não comprovado. Os 635 critérios, 33 dependências documentais e 0/24 jornadas reais pertencem àquela campanha; não são uma contagem atual revalidada. Os recibos da migração PostgreSQL/Java/Node aprovam somente os bytes e checks identificados, sem homologação integral.

## Exigências históricas e alcance atual

- Reconciliar critérios aplicáveis e áreas removidas com a derivada e seus hashes atuais; não declarar 198/198 por herança.
- Executar e documentar as jornadas reais pertinentes, incluindo autorização/CSRF, janela configurável e limites exclusivos, integrações, arquivos e reinício, em recursos isolados aprovados. O padrão30 pertence à campanha de 09/11; a regra de 11/11 separa lançamento em sete datas civis e edição em 72 horas.
- Resolver ou revalidar as pendências funcionais/documentais apontadas nos pareceres anteriores. Fixtures não comprovam autenticidade administrativa.
- Definir e comprovar destino, operação, segurança, backup/recuperação e aceite institucional de produção.

Os três primeiros itens foram reconciliados e demonstrados no alcance do laboratório da campanha de 09/11, conforme os pareceres26/12 e o ledger820. Não se aprovam automaticamente todos os635 critérios por associação de classe, nem autenticidade administrativa de fixtures. O quarto item permanece condição de uma futura implantação em produção, fora do pedido executado. A jornada LAN completa foi confirmada pelo usuário em outro computador físico. Status e limites ficam nos recibos com fonte/diff/hash.

## Campanha de 09/11/2026 — histórica

Execução autorizada no [plano da campanha](CODEX/CAMPANHA_20261109/PLANO.md): Java → revisão/homologação backend → React/TSX → revisão frontend → homologação integrada. Sol/Xalto confirmado na sessão; somente Chrome/Chromium desktop; Sonar excluído por decisão humana e teto agregado15GB, com execução pesada sequencial. Essa campanha adotou a janela de 30 dias, posteriormente substituída pelos [prazos separados de lançamento e edição de 11/11](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md). Recursos reais isolados dependem da conferência/autorização registrada na campanha; nenhum aceite histórico é promovido.

## Preparação operacional de 11/11/2026

A extensão para instalação nova foi implementada e ensaiada no laboratório: verificador de configuração produtiva, schema novo por JPA, comissionamento nominal de um Administrador com troca obrigatória, backup/restore íntegros e scripts/documentação Windows. Veja [ensaios e limites](CODEX/PRODUCAO_20261111/ENSAIOS_LABORATORIO.json) e [operação Windows](../JAVA/scripts/operacao/OPERACAO_WINDOWS.md).

O JAR dessa extensão tem hash `6d55cba894b037b6a20cca83a6d49b6f30b0e4a1c62ba95bfb434bdfbadf6944`. Preserva823classes da homologação funcional anterior; suas novas configurações/componentes e assets têm ensaios próprios. A revisão independente Java/operacional e frontend desta extensão foi aprovada no laboratório, no commit9c8bf186, com doisP2 de restore corrigidos e rechecados. Veja [pareceres](CODEX/PRODUCAO_20261111/PARECERES.json). Windows nativo, certificado/ChromeHTTPS do destino, cópia externa de backup/restauração e responsável pelo aceite não estão comprovados. **Não liberado para produção.**

## Dashboard desktop de cinco períodos — revisão de 11/11/2026

Mockup desktop v3 implementado e aprovado no laboratório sintético desktop para o checkpoint R4. Diário padrão; backend real dos cinco períodos; classificação do primeiro lançamento, revisão solicitada pelo Operacional/decidida pelo Gestor Local, cobertura por escala, ocupação, admissão, pendências, vigência canônica e ícones no footer. Prazos propostos configuráveis: sete datas civis para lançar e 72 horas para editar desde o primeiro lançamento. Ver [contrato atual](CODEX/DASHBOARD_20261111/CONTRATO.md), [regra de frequência](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) e [parecer final Sol](CODEX/DASHBOARD_20261111/PARECER_FINAL_SOL_R4.md).

As aprovações históricas acima não homologam esta alteração. Checkpoints anteriores do dashboard que registravam indicadores indisponíveis/padrão mensal foram substituídos pelo pedido humano atual; seus recibos permanecem históricos. Esta versão tem revisão independente Astra/xhigh e homologação Sol/xhigh próprias, vinculadas ao JAR `b1b87261111a5ac182b498a2fcb93a3f4c986f22795e593a0bf66a3bd53eb814`. Passaram 800 unitários Java, 860 testes React, seis verificações de preparação, typecheck/build e oito ITs pertinentes PostgreSQL/HTTP. Ver [verificação e limites](CODEX/DASHBOARD_20261111/VERIFICACAO.json). Zoom nativo, Windows, LAN externa, produção e todo o catálogo histórico permanecem fora desse aceite. Commit/push autorizados após aprovação; teto 15 GB e pausa na franquia semanal restante <=1% preservados.
