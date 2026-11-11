# Regras temporais e de autorização — contrato e rastreabilidade

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

## Escopo desta leitura

Este documento organiza os oráculos obrigatórios do mestre e aponta as fontes da candidata 04. Ele não transforma leitura de código em execução. Os resultados pertencem às matrizes de critérios, ROP, módulos e posições transversais, vinculadas aos recibos frescos de cada camada.

O mestre tem precedência sobre referências históricas. Os IDs abaixo pertencem ao namespace `MASTER::8.x`. Por exemplo, `MASTER::8.6::CON-012` trata dois executores após reinício; `CANONICAL198::CON-012` trata prorrogação retroativa. A coincidência do nome não autoriza misturar resultados.

## Regra temporal observada no código

`PoliticaTemporalFrequenciaServico.instanteFechamento` calcula o início do dia D+7 na zona configurada do domínio. `fechada` usa a negação de `isBefore`: o instante exato já está fechado. O objeto de data/hora fornecido é convertido conservando o instante. `RelogioSistema` usa a mesma constante de zona para hoje/agora. A campanha mantém `America/Sao_Paulo` como fuso administrativo e timestamps UTC nos recibos.

A janela temporal é uma condição entre outras. Estar antes do prazo não substitui vínculo, alocação, contrato, ausência de futuro e autorização válidos. Também não transforma um teste com relógio controlado em atravessamento físico de meia-noite. Esse último cenário exige evidência própria e conserva o status não comprovado quando não executado.

Na leitura de `AutorizacaoTemporalOcorrenciaServico.validarCriacao`, todos os dias atingidos são resolvidos antes da validação de escrita. O contexto contém contrato, vínculo, alocação, Gestão, Estado, lotação e jornada. A resolução histórica procura prova vinculada ao fato e realinha o contexto temporal; sem contexto suficiente, não inventa uma associação a partir do cadastro atual. Esta é uma constatação estática localizada, não aprovação automática do requisito completo de concessão ou de todos os consumidores.

## Identidade do banco na integração

Os suportes de integração importam `GuardaMysql8045`, que obtém uma conexão da DataSource real antes de Hibernate, exige produto MySQL e ambas as versões exatamente8.0.45 e recusa nominalmente contas administrativas internas. Sua verificação de versão delega à função produtiva. O modo nativo da campanha fornece explicitamente os parâmetros; não houve execução funcional em Testcontainers, H2 ou outro servidor.

A guarda não inspeciona GRANTS e não compara UUID. A prova de conta limitada vem dos recibos de provisionamento/conferência. A campanha e o teste de identidade registram/confrontam UUID, catálogo, porta e demais metadados com alcance explicitado no relatório. Uma única consulta externa não é apresentada como a conexão interna do H02, que não possui a nova instrumentação.

## Oráculos integrais do mestre

As três seções a seguir foram transcritas do documento fornecido, sem reduzir as condições de aceite. Uma resposta HTTP negativa só sustenta a parte observada; ausência de mutação, arquivos, eventos, UI e autorização corrente precisam das respectivas provas.

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

## Contraprovas e limites de interpretação

- ALO-008:002: a tentativa suplementar original exige201 no aditamento de preparo. A recusa409 de capacidade ocupada ocorre antes do alvo de reativação e não é uma contraprova concluída desse alvo. A análise das alternativas com inativação prévia está em `review_notes/ALO008002_ALCANCABILIDADE_INATIVACAO_PREVIA_20261104.json`; não se fabricam estados que contornem a reserva temporal.
- PER-010:004: os consumidores e a abrangência multidiária têm oráculos próprios. Uma concessão genérica sem ocorrência/data não demonstra, por si, recusa contextual no ato de conceder.
- As condições de DOC-VERSION exigem primeira resposta, GET, replay, versão persistida, todas as colunas pertinentes e bytes de arquivo. Conferir um único campo isolado é insuficiente.
- Os testes de cancelamento com três vínculos por MockMvc/MySQL não demonstram sozinhos200vínculos, recuperação após morte da JVM, dois executores ou interface Chrome.
- Testes React com transporte controlado não comprovam a ausência de uma mutação real depois da troca de sessão. O gate de Chrome e o gate HTTP permanecem camadas distintas.

## Fontes conferidas

| Fonte na árvore Java final | SHA256 |
|---|---|
| `src/main/java/com/example/siggtrat/servico/PoliticaTemporalFrequenciaServico.java` | `dbe21c8020259d58e3282c2ca9f949479f6d8f5a2ba6ff453c3db9c1ef5082df` |
| `src/main/java/com/example/siggtrat/utilitario/RelogioSistema.java` | `08aaab6b34444ec178f080069af2839fdc33e9a06225381dabf44db9d0ecbcf1` |
| `src/main/java/com/example/siggtrat/servico/AutorizacaoTemporalOcorrenciaServico.java` | `2c83d3b67ea683e30df8750ddaf91d9aff265761c74bd76a6df04d4e3f4e0a93` |
| `src/test/java/com/example/siggtrat/infra/GuardaMysql8045.java` | `54b5ac66233cbc9060ea99e5ef8b51865695d95747b773f5576095ef031bf9f2` |

Mestre SHA256 `5386b2a82e5e2779993801f89deb09217f15dc9940e316a052ba6b6b8019c92f`. Commit `19824eb0aa93d6a4d826b757852fee15a68adb92`; selo `ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79`. Preparação documental UTC: 2026-11-04T00:19:44.093939+00:00. Nenhum produto, teste, configuração ou selo foi modificado.
