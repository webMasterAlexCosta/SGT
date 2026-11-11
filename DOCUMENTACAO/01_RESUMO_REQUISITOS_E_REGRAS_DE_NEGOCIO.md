# SGT C25-H04 — levantamento atualizado de requisitos e regras de negócio

> **Estado atual: projeto não concluído e não homologado para produção.** O corpo original preserva o corte C25-H04/MySQL; a derivada local usa PostgreSQL e a toolchain registrada em [CODEX/TOOLCHAIN.md](CODEX/TOOLCHAIN.md). Contagens, commits e aceites históricos não aprovam os bytes atuais. Leia [ESTADO_ATUAL.md](ESTADO_ATUAL.md) e o [índice](README.md).

> **Decisão vigente de09/11/2026:** a janela fixa de sete dias citada no corpo histórico abaixo foi substituída por N configurável, padrão30, limite exclusivo D+N. Consulte a [regra vigente](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md). Governança de Terceiros continua excluída; demais permissões e invariantes preservadas. Nenhum resultado histórico aprova a campanha atual.

**Referência documental:** campanha C25-H04, corte C03/C04 de 04/11/2026. **Empacotado:** 05/11/2026, sem alteração do código-fonte.

## 1. O que esta documentação contém

Esta entrega não é apenas um resumo de arquitetura: inclui o levantamento vigente de **198 requisitos, 635 critérios de aceite, 21 módulos operacionais, 64 roteiros ROP e 76 gates transversais**, com matriz dos endpoints e mapeamento das áreas removidas/preservadas. Os documentos originais estão em `DOCUMENTACAO/MATRIZES_COMPLETAS/`; o catálogo consolidado dos 198 enunciados está em `DOCUMENTACAO/02_CATALOGO_DOS_198_REQUISITOS.md`.

**IMPORTANTE:** essa é a *candidata mais recente conhecida*, não uma homologação plena. O relatório de 04/11 classifica a versão como **PARCIAL / NÃO COMPROVADA, não autorizada para produção**. Os 635 critérios possuem 602 com aceite integrado parcial e 33 bloqueados por documentação administrativa autêntica. Nos 602 não documentais, há dois predicados ainda parciais. O Chrome foi bloqueado antes de executar as 24 jornadas reais planejadas (**0/24**). Testes em JVM, HTTP e jsdom não substituem homologação visual.

## 2. Versão do produto

- Backend Java **0.0.4**, frontend React **1.4.4**, commit `19824eb0aa93d6a4d826b757852fee15a68adb92`.
- Banco **MySQL Server 8.0.45 real**, Redis; Hibernate/JPA com `spring.jpa.hibernate.ddl-auto=update` para schema, **sem DDL manual, Flyway ou Liquibase**.
- Quatro artefatos produtivos lacrados e sem modificações em `PRODUTO/`. O JAR Java tem SHA-256 `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d`.

## 3. Levantamento funcional por módulo

| ID | Módulo | Escopo e regra de negócio principal |
|---|---|---|
| M01 | Empresas contratadas | Cadastro operacional de fornecedor, CNPJ e atualização; associação apenas ao contrato em rascunho; preservação do histórico. |
| M02 | Estrutura organizacional | Gestão Local e Superintendência, escopo territorial e competência na data efetiva do fato; mudanças exigem trilha formal. |
| M03 | Catálogos operacionais | Cargo, tipo, lotação, turno, jornada, situação e motivos, com validade temporal, unicidade e preservação de registros históricos. |
| M04 | Ciclo de vida dos contratos | Rascunho, publicação, vigência, encerramento/anulação/inativação via atos formais; publicação não admite edição livre. |
| M05 | Composição contratual | Lotação + Turno + Cargo, quantitativos consistentes; piso ordinário de 75% do efetivo originalmente publicado; exceção fundamentada. |
| M06 | Aditamentos | Prorrogação com nova data final distinta da data efetiva, prévia, token vigente, documentação e conflito/autoridade revalidados. |
| M07 | Remanejamentos | Redistribuição entre composições existentes, soma das variações igual a zero e sem movimentar trabalhadores automaticamente. |
| M08 | Pessoas e empregados | Pessoa persistente, empregado como projeção e vínculo temporal separado; CPF, matrícula, homônimos e fotos históricas. |
| M09 | Vínculos contratuais | Admissão, programação, desligamento, cancelamento e recontratação com histórico, datas, unicidade e reserva global da pessoa. |
| M10 | Alocações e movimentações | Faixas temporais Lotação + Turno + Cargo, sem sobreposição indevida; alterações preservam fatos anteriores. |
| M11 | Calendário e jornadas | Escalas e feriados, jornadas noturnas pertencentes ao dia de início, fuso civil e anos bissextos. |
| M12 | Frequência | Presença integral/parcial, falta e ausência não comprovada; data futura proibida; fechamento da janela no início de D+7. |
| M13 | Ajustes e fechamento | Correções gerenciais com autorização, jobs assíncronos, progresso, cancelamento, reprocessamento e recuperação sem duplicação. |
| M14 | Ocorrências e atestados | Ocorrências, justificativas, intervalos e anexos; autoridade histórica por dia do fato; transação sem arquivo órfão. |
| M15 | Pendências administrativas | Regularização com fonte, alcance restrito, histórico imutável e proibição de bloqueio/autorizações fora do escopo. |
| M16 | Histórico e provas temporais | Distinguir data do efeito e data do conhecimento; fontes recebidas tardiamente não devem retroagir como já conhecidas. |
| M17 | Documentos e SEI | Upload/download restritos, verificação de bytes e hash, versão técnica e de negócio, retificação e auditoria. |
| M18 | Painel e indicadores | Efetivo, alocação, presença, absenteísmo, filtros temporais e invalidação de dados obsoletos após retificação. |
| M19 | Relatórios e exportações | PDF/XLSX sujeitos a escopo, conteúdo, paginação, período e vínculo à resposta e filtros atuais. |
| M20 | Usuários, perfis e segurança | Autenticação, autorização no servidor, CSRF, 2FA, sessão, revogação, concorrência entre abas e idle timeout. |
| M21 | Auditoria e rastreabilidade | Autor, perfil, motivo, versão, data efetiva, registro e fonte de cada ato; recusa e rollback não simulam sucesso. |

**Classificação dos módulos:** PARCIAL na matriz C04. O quadro é um índice de leitura; cada módulo contém fluxos positivos, negativos, regras temporais, perfis, contratos HTTP, dependências e limites detalhados em `MATRIZ_21_MODULOS_NOVA.md` e `.csv`.

## 4. Regras de negócio que não podem regredir

1. **Não apagar fatos negociais publicados.** Publicações, versões, documentos e eventos podem ser retificados, cancelados ou sucedidos com prova, jamais apagados silenciosamente. Rascunho nunca publicado e sem fato negocial pode ter descarte transacional (`GOV-003`, `GOV-004`).
2. **Modelo Pessoa / Vínculo / Alocação.** Pessoa identifica o indivíduo; vínculo estabelece relação temporal com contrato; alocação especifica Lotação + Turno + Cargo em um período. A projeção atual não é fonte canônica para reescrever o histórico (`TMP-001`, `TMP-002`).
3. **Bitemporalidade.** Data efetiva do acontecimento difere da data/hora em que se tomou conhecimento. Eventos retroativos reaplicam fatos posteriores válidos; conflitos exigem tratamento explícito, não exclusão automática (`TMP-003` a `TMP-017`).
4. **Composição contratual e limite de redução.** Soma dos componentes deve equivaler ao quantitativo pactuado; redução ordinária até 75% da quantidade originalmente publicada (100→75 admitido; 100→74 recusado na via ordinária), inclusive se houve aumento intermediário. Exceção apenas com fundamento e autorização aplicável (`M05`).
5. **Frequência e prazo civil.** Presença futura é recusada inclusive para Gestor Local. Para data D, a via operacional encerra **exatamente em D+7 às 00:00 de America/Sao_Paulo** (limite exclusivo); D+8 aciona o fluxo de fechamento previsto. A jornada noturna pertence ao dia em que se iniciou, mesmo terminando no dia seguinte (`M11`, `M12`, `ROP-013` a `ROP-018`, `ROP-034`).
6. **Autorização pela situação histórica.** Contrato, Gestão, lotação, turno, vínculo e alocação devem ser resolvidos conforme a data do fato, não só pelos cadastros atuais. Mudança de competência não libera retroativamente acesso de quem não tinha atribuição (`M02`, `M14`, `REGRAS_TEMPORAIS_E_AUTORIZACAO.md`).
7. **Aditamento e remanejamento formais.** A prorrogação tem `novaDataFim` separada da data efetiva, prévia e confirmação válidas; remanejamento troca quantidades entre composições existentes com soma zero e não move empregados por efeito colateral (`M06`, `M07`).
8. **Concorrência, idempotência e atomicidade.** Reenvio, duplo clique, disputa de versão, bloqueio, revogação de sessão e lote inválido não devem duplicar fatos nem deixar mutações parciais ou arquivos órfãos (`ROP-043` a `ROP-049`, gates `CON`).
9. **Documentos e histórico.** Versão técnica de lock não é a versão jurídica do documento; bytes, hash, fonte/SEI, histórico, permissão de download e trilha de auditoria devem ser preservados e verificáveis (`M16`, `M17`).
10. **Segurança no servidor e isolamento entre sessões.** UI não substitui 401/403/409, autorização atual, CSRF, proteção de dados entre abas, expiração, 2FA, revogação e auditoria (`M20`, gates `SEC`).
11. **Escopo após a remoção de governanças.** Cadastro operacional de empresa/Fornecedor e os módulos contratuais e operacionais permanecem; governança de terceiros/risco e famílias financeiras retiradas não devem ressurgir. Mapas de remoção são verificação estática, não prova integral de 404/cache/Chrome (`MAPA_REMOVIDOS_PRESERVADOS.md`).
12. **Relatórios e indicadores.** Totais, dados históricos, filtro ativo, período e autorizações devem coincidir entre UI, API, PDF, XLSX e persistência; resposta HTTP antiga não pode sobrescrever nova seleção (`M18`, `M19`).

## 5. Pendências da homologação de 04/11

- **33 critérios documentais** ainda aguardam documentos administrativos autênticos: `CON-023` (9), `CAT-004` (4), `CAT-007` (8), `COL-010` (4), `HIS-001` (4), `HIS-006` (4).
- **0 de 24 jornadas reais no Google Chrome** executadas por bloqueio do ambiente; interações, navegação, fluxo visual, downloads e alterações assíncronas não foram homologados ali.
- `ALO-008:002` e `PER-010:004` conservam limites de demonstração de seus predicados contextuais; a recusa de um preparo não comprova o alvo.
- Dois timeouts `SQL1205/SQLState40001` do consumidor automático e limitações de provas de ciclo/escala permanecem documentados.
- FE-02 teve comprovação ampliada em HTTP/OOXML após reinício para séries variáveis, **não** em gráficos/baixadas no Chrome e PDF React da massa.
- Verificar `DOCUMENTACAO/AUDITORIA_E_PENDENCIAS/` antes de afirmar aptidão para produção.

## 6. Guia de leitura da documentação completa

1. Comece por este documento e `02_CATALOGO_DOS_198_REQUISITOS.md` (enunciado e estado de cada requisito).
2. Consulte `MATRIZ_21_MODULOS_NOVA.md` para os fluxos, permissões, negativos e contratos por módulo.
3. Consulte `MATRIZ_198_635_NOVA.md` e `.csv` para o aceite de cada critério e as provas por camada.
4. Consulte `MATRIZ_64_ROP_NOVA.md` para os roteiros operacionais completos; os gates `MATRIZ_GATES_TRANSVERSAIS_NOVA.*` para segurança, tempo, concorrência, arquitetura, recuperação e desempenho.
5. Consulte `MATRIZ_ENDPOINTS_JAVA_REACT.csv` e `MAPA_CONTROLADORES_21_MODULOS.json` para vincular regras a implementação; consulte `MAPA_REMOVIDOS_PRESERVADOS.md` para alcance da remoção.
6. Leia o `PROMPT_MESTRE_SGT_CORRECAO_E_HOMOLOGACAO_8045_20261103.md` como especificação de execução da campanha, distinguindo testes exigidos de testes efetivamente aprovados.

**Precedência:** os documentos C04 são a leitura atualizada da candidata; o mestre orienta critérios da campanha e não pode ser convertido em aprovação por si só. Os IDs iguais entre a matriz histórica dos 198 requisitos e as matrizes transversais do mestre **podem ter significados diferentes**; preserve os namespaces (ex.: `CANONICAL198::CON-012` vs `MASTER::8.6::CON-012`).


## Contrato de preservação da derivada atual — 07/11/2026

Consolidação das obrigações do AGENTS anterior, preservado no checkpoint `d8c415f4`. Esta seção não inventa novos requisitos nem certifica que todos estejam implementados.

- Preservar autorização atual, núcleo operacional, Fornecedor compartilhado, GOV-003, documentos, histórico/auditoria, perfis/escopos/senha/CSRF, locks/transações/rollback, fotografias e limites operacionais. A duração fixa anterior foi substituída pela decisão humana de09/11/2026: N dias civis configuráveis, padrão30.
- Governança de Terceiros/financeira e risco manual/contínuo removidos na baseline não devem ser reintroduzidos. Cadastro operacional de Fornecedor e funções compartilhadas não são automaticamente áreas removidas. O escopo detalhado e suas exceções estão em [MAPA_REMOVIDOS_PRESERVADOS.md](MATRIZES_COMPLETAS/MAPA_REMOVIDOS_PRESERVADOS.md), inclusive o suplemento PostgreSQL.
- A data de início da jornada D, e não a gravação/retificação, define o primeiro dia da janela N configurável/padrão30. O limite é início de D+N em America/Sao_Paulo, exclusivo; para01/01 e N=30,31/01 às00:00. Inclusão/retificação não reinicia a janela. Permissões e demais condições permanecem. Fonte vigente: [REGRA_JANELA_FREQUENCIA.md](../JAVA/docs/REGRA_JANELA_FREQUENCIA.md); [investigação anterior](../JAVA/docs/REGRA_SETE_DIAS.md) preservada como histórico.
- Preservar precisão JSON e temporalidade America/Sao_Paulo; contratos Jackson2 de serialização/JJWT/Redis, cookies/CSRF, geração de sessão/cancelamento, acessibilidade e validações decisivas do backend real.

O catálogo de 198 enunciados é uma fonte de rastreabilidade com namespaces e cortes próprios; não reativa por si só a missão antiga de congelar 198/198 nem as áreas removidas. Cada alegação de conclusão exige prova atual e escopo identificado. Nenhuma migração de acervo ou implantação é autorizada por esta consolidação.
