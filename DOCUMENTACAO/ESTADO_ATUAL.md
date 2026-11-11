# SGT — estado atual

**Campanha atual: homologação funcional integrada do laboratório aprovada, em Chrome/Chromium desktop, pelos pareceres independentes backend26 e frontend12. Sem P0–P3 nem obrigação aplicável remanescente no recorte examinado.** [Alcance e evidências](CODEX/CAMPANHA_20261109/HOMOLOGACAO_FUNCIONAL_LABORATORIO.md). Produção e implantação não foram homologadas ou autorizadas.

## O que existe

Backend Java/Spring, frontend React/TypeScript e a derivada PostgreSQL, com Redis nos cenários previstos. A toolchain atual e os recibos técnicos estão em [CODEX/TOOLCHAIN.md](CODEX/TOOLCHAIN.md). A organização documental não alterou nem executou a aplicação.

## Como interpretar as evidências

O [resumo C25-H04](01_RESUMO_REQUISITOS_E_REGRAS_DE_NEGOCIO.md) e as [notas da candidata](AUDITORIA_E_PENDENCIAS/NOTAS_DE_VERSAO_E_PENDENCIAS.md) registram um corte anterior, MySQL, parcial/não comprovado. Os 635 critérios, 33 dependências documentais e 0/24 jornadas reais pertencem àquela campanha; não são uma contagem atual revalidada. Os recibos da migração PostgreSQL/Java/Node aprovam somente os bytes e checks identificados, sem homologação integral.

## Exigências históricas e alcance atual

- Reconciliar critérios aplicáveis e áreas removidas com a derivada e seus hashes atuais; não declarar 198/198 por herança.
- Executar e documentar as jornadas reais pertinentes, incluindo autorização/CSRF, janela configurável com padrão30 e limites exclusivos, integrações, arquivos e reinício, em recursos isolados aprovados.
- Resolver ou revalidar as pendências funcionais/documentais apontadas nos pareceres anteriores. Fixtures não comprovam autenticidade administrativa.
- Definir e comprovar destino, operação, segurança, backup/recuperação e aceite institucional de produção.

Os três primeiros itens foram reconciliados e demonstrados no alcance do laboratório da campanha atual, conforme os pareceres26/12 e o ledger820. Não se aprovam automaticamente todos os635 critérios por associação de classe, nem autenticidade administrativa de fixtures. O quarto item permanece condição de uma futura implantação em produção, fora do pedido executado. A jornada LAN completa foi confirmada pelo usuário em outro computador físico. Status e limites ficam nos recibos com fonte/diff/hash.

## Campanha atual —09/11/2026

Execução autorizada no [plano atual](CODEX/CAMPANHA_20261109/PLANO.md): Java → revisão/homologação backend → React/TSX → revisão frontend → homologação integrada. Sol/Xalto confirmado na sessão; somente Chrome/Chromium desktop; Sonar excluído por decisão humana e teto agregado15GB, com execução pesada sequencial. A [janela vigente](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui referências antigas a sete dias fixos. Recursos reais isolados dependem da conferência/autorização registrada na campanha; nenhum aceite histórico é promovido.
