# Portagem dos suplementos históricos — fontes preparadas

**Estado: não compilado e não executado nesta entrega.** O pacote não atribui PASS aos 39 critérios históricos. Há 12 classes de IT, 34 métodos anotados e 49 invocações previstas; essas quantidades não são resultados de execução. Os 118 predicados do índice independente V2 permanecem individualizados na matriz.

## Conteúdo e decisões

| Grupo | Fonte selecionada | Decisão |
|---|---|---|
| A244 | Atomicidade/Fotografia e suporte v2; quatro outros IT da pasta original | O suporte v2 verifica a raiz real configurada de armazenamento. Os seis corpos de teste são idênticos aos originais selecionados. |
| B230 | b230_followup_cat001_v2, inclusive seu suporte | O override canônico aninhado de CAT-001 exige401/ACCOUNT_INACTIVE/LOGOUT. O RED antigo403 e ambos os hashes continuam preservados; não houve ajuste de negócio nesta portagem. |
| C | criteria_c_probe_doc005_v6 | Preserva o snapshot com Hibernate.unproxy do v5 e os controles de estado pós-commit. Ocorrências permanece byte a byte; Histórico recebe somente wrappers de leitura readOnly nas fotografias antes/depois. |
| Diagnóstico de finalidade textual | CriteriosCFinalidadeObservacaoHttpIT | Preservado fora de src/test/java e sem crédito criterial; não inventa proibição de texto diferente. |

O pacote será entregue integralmente como suplemento opt-in, sem registro automático no src/test principal. São 15 fontes históricas selecionadas: 7 permanecem byte a byte. Os três suportes recebem ajustes de identidade/isolamento necessários à campanha nativa e helpers de observação em transação somente de leitura. As três classes B230 e CriteriosCHistorico mudam apenas o wrapper das consultas para o novo helper; suas assertions e as lambdas de escrita/fixtures ficam intactas. CriteriosCDocumentos também usa o helper para reler a versão e acrescenta duas comparações exigindo a resposta canônica completa desde o primeiro POST. O guard comum é o único arquivo novo. 22 pares nome/SHA históricos, incluindo variantes superadas, estão em proveniencia_variantes; originais selecionados têm sua própria cópia e hash. PORTAGEM.diff explicita todas as alterações.

## DOC-005: fortalecimento do oráculo

A versão histórica v6 não fixa version=0 como esperado: ela registra a divergência do primeiro POST em relação ao GET, mas usa o GET pós-commit para o replay. A nova cópia exige JsonNode integral primeiroPOST=GET, nenhum campo divergente, GET.version=JPA.version e replay=GET, conservando as demais comparações de documentos, estado e arquivos antes/depois do409. Não se fixa version=1, não se remove version e não se afrouxa o expected status. A igualdade histórica primeiroPOST=replay também decorre das duas igualdades integrais agora exigidas. O RED independente do coordenador sobre os bytes H02 é externo a esta entrega; este pacote ainda não possui resultado novo.

## Isolamento e fixtures

A guarda de execução externa A244 antiga dependia de um diretório e selo H02 que não existem no projeto atual. Sua finalidade de isolamento foi substituída, por autorização do coordenador, por opt-in explícito e validação do endpoint, conta, prefixo de schema, UUID, versão real do servidor e metadata JDBC. A validação da DataSource ocorre por BeanPostProcessor antes do EntityManager e se repete antes dos casos. Reutiliza GuardaMysql8045; o gate do binário executável continua pertencendo aos recibos nativos do coordenador. Não é suficiente compilar ou simular esta guarda para homologar a campanha.

O UUID esperado vem obrigatoriamente de SGT_MYSQL_SERVER_UUID no ambiente privado selado da rodada. O suporte valida o formato completo antes de abrir conexão e o compara com SELECT @@server_uuid; não existe fallback, UUID fixo ou adoção automática do servidor observado. Após a perda do redo na interrupção das18:31:41Z, o datadir anterior foi preservado e a campanha passou a um datadir novo. Os resultados anteriores permanecem vinculados ao UUID f241c2d2-d7a5-11f1-a2d7-73b42ec6596b; não há alegação de continuidade ou persistência entre os dois datadirs. Esta alteração de suporte não reexecuta nem reclassifica os recibos existentes.

Conforme §133 e revisão independente, as fotografias B230/C e consultas diretas de controle executam REQUIRES_NEW/READ_COMMITTED com readOnly=true, limpando o contexto antes e depois. Os helpers nova/emTransacao continuam disponíveis e inalterados para o preparo e para as adulterações JPA deliberadas. A244 já tinha essa separação. Essa mudança é apenas de observação; todos os SELECTs e asserts mantêm a semântica. 

O Redis usa o índice declarado no ambiente da campanha, em vez de9/12 herdados, e os quatro campos efetivos são comparados sem registrar valores secretos. A raiz de upload C permanece target/criteria-c/uploads; A244 preserva a conferência da raiz efetivamente configurada e contida no projeto. Não houve DDL/DML manual a converter: os SQLs presentes leem identidade e snapshots; as fixtures e mutações de controle usam JPA ou comandos HTTP MockMvc. Os deletes de requests são comandos de negócio da API, não SQL manual. A fonte sintética de adulteração AUD-001 é intencional e permanece na base isolada.

## Limites de aceite

- ALO-008:002: o caso negativo conserva a preparação histórica que esperava201 no aditamento. No acervo ela parou em409 antes da reativação. A execução focal separada deve manter essa distinção e não transformar rejeição do preparo em prova da guarda de retomada. Nenhum estado de negócio inválido foi fabricado para forçar a etapa final.
- PER-010:004: os testes de fronteira temporal cobrem os predicados mapeados; a lacuna histórica de concessão/contexto multidia e matriz completa de entradas permanece explicitamente no mapa. Não se herda fechamento total.
- AUD-006:002/004: aparecem entre os39, mas não trazem suplemento histórico Java portável neste recorte. O V2 aponta ContraprovasPendenciasH04HttpIT do coordenador; seus recibos são avaliados separadamente, sem atribuição por esta portagem.
- ADI-004 usa um Aspect exclusivo do teste para falhar após escritas/flush e verificar rollback. VIN-013 controla RelogioSistema.hoje. O scheduler herdado é substituído para evitar tarefas de fundo. Esses limites permanecem no código.
- Todos estes IT usam MockMvc autenticado e serviços/JPA reais quando executados no MySQL autorizado. Não exercitam TCP HTTP nem Chrome. As49 invocações planejadas não correspondem a49 critérios nem à homologação de39 critérios.
- Snapshots integrais excluem apenas token_ativo e registro_auditoria nos suportes A/B, conforme o oráculo histórico. Alterações nesses registros técnicos não devem ser vendidas como parte do rollback negocial. O runner deve conservar a comparação integral e publicar apenas evidências sanitizadas.

## Arquivos de revisão

- MATRIZ_39_CRITERIOS_PORTAGEM.csv e MATRIZ_118_PREDICADOS_PORTAGEM.csv: método, fonte/hash, base preservada, limite e coluna vazia para o novo recibo; nenhuma aprovação herdada.
- INVENTARIO_FONTES_ADAPTADAS.json, DEPENDENCIAS_BASE_HASHES.json e METODOS_E_INVOCACOES_PLANEJADAS.json: bytes e escopo.
- COMANDOS_SUGERIDOS.md e SELETORES_FOCAIS.json: cinco seleções reproduzíveis, com ALO negativo discriminado.
- SUPLEMENTOS_8045_OVERLAY.tar.gz: somente os16 arquivos a sobrepor na cópia isolada autorizada pelo coordenador.
- MANIFESTO_STAGING.json e SHA256SUMS.txt: selo desta entrega. Nenhum arquivo do work React/backend foi alterado por esta portagem.
