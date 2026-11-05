from pathlib import Path
import collections
import csv
import datetime
import difflib
import hashlib
import gzip
import io
import json
import re
import tarfile

STAGE = Path(__file__).resolve().parent
ROOT = STAGE.parent
PKG = Path('com/example/siggtrat/auditoria20261029')
CATALOG = ROOT / 'review_notes/CATALOGO_39_SUPLEMENTOS_V2_20261103.json'
BT = chr(96)
FENCE = BT * 3


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(name, obj):
    (STAGE / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')


def write_csv(name, rows, columns):
    with (STAGE / name).open('w', newline='') as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)


selection = json.loads((STAGE / 'SELECAO_FONTES.json').read_text())
catalog = json.loads(CATALOG.read_text())
assert sha(CATALOG) == selection['catalog_v2_sha256']
assert len(catalog['criteria']) == 39
source_dir = STAGE / 'src/test/java' / PKG
sources = {p.name: p for p in sorted(source_dir.glob('*.java'))}
assert len(sources) == 16
patch = []
file_rows = []
methods = []
assertion = re.compile(r'\bassert(?:That|Equals|True|False|ArrayEquals|NotEquals|NotNull|Null|Throws)\(')
method_regex = re.compile(r'@(Test|ParameterizedTest)\b(.*?)\bvoid\s+(\w+)\s*\(', re.S)
for item in selection['selected']:
    original = STAGE / item['original']
    current = STAGE / item['adapted']
    assert sha(original) == item['original_sha256']
    before, after = original.read_text(), current.read_text()
    patch.extend(difflib.unified_diff(before.splitlines(keepends=True), after.splitlines(keepends=True),
                                    fromfile=item['original'], tofile=item['adapted']))
    row = dict(item, adapted_sha256=sha(current), bytes_adapted=current.stat().st_size,
               byte_identical=before == after,
               lexical_assertions_before=len(assertion.findall(before)),
               lexical_assertions_after=len(assertion.findall(after)))
    file_rows.append(row)
    if current.name.endswith('IT.java'):
        for match in method_regex.finditer(after):
            kind, annotations, method = match.groups()
            parameter_count = 1
            if kind == 'ParameterizedTest':
                values = re.search(r'@(?:ValueSource\(strings\s*=\s*|CsvSource\()\{([^}]+)\}', annotations)
                assert values, (current.name, method)
                parameter_count = len(re.findall(r'"(?:[^"\\]|\\.)*"', values.group(1)))
            methods.append({'reference': current.stem + '#' + method,
                            'source': item['adapted'], 'source_sha256': sha(current),
                            'annotation': kind, 'planned_invocations_static': parameter_count,
                            'status': 'NAO_COMPILADO_NAO_EXECUTADO'})

new_source = sources['Suplementos8045Ambiente.java']
patch.extend(difflib.unified_diff([], new_source.read_text().splitlines(keepends=True),
                                fromfile='/dev/null', tofile=str(new_source.relative_to(STAGE))))
(STAGE / 'PORTAGEM.diff').write_text(''.join(patch))
assert len(methods) == 34
assert sum(m['planned_invocations_static'] for m in methods) == 49
assert sum(x['byte_identical'] for x in file_rows) == 7
assert all(x['lexical_assertions_after'] >= x['lexical_assertions_before'] for x in file_rows)
method_index = {m['reference']: m for m in methods}
write_json('INVENTARIO_FONTES_ADAPTADAS.json', {'files': file_rows,
    'new_helper': {'path': str(new_source.relative_to(STAGE)), 'sha256': sha(new_source)},
    'assertion_count_limit': 'Contagem lexical é inventário auxiliar; a prova de preservação é o diff integral e os 7 arquivos idênticos.'})
write_json('METODOS_E_INVOCACOES_PLANEJADAS.json', {'methods': methods,
    'test_classes': 12, 'test_methods': 34, 'planned_invocations_static': 49,
    'executed': 0, 'compiled': False, 'limit': 'Invocações calculadas das anotações, não contagem de XML executado.'})

criteria_rows = []
predicate_rows = []
for criterion in catalog['criteria']:
    refs = sorted({r for p in criterion['predicates'] for r in p['supplemental_methods']})
    for ref in refs:
        assert ref in method_index, ref
    criteria_rows.append({'criterion_id': criterion['criterion_id'], 'original_text': criterion['original_text'],
        'supplemental_methods': ' | '.join(refs),
        'files': ' | '.join(sorted({method_index[r]['source'] for r in refs})),
        'historical_remaining_gap': criterion.get('manual_remaining_gap_original') or '',
        'current_status': 'PREPARADO_NAO_EXECUTADO' if refs else 'SEM_SUPLEMENTO_HISTORICO_PORTAVEL_NESTE_RECORTE',
        'new_receipt': ''})
    for predicate in criterion['predicates']:
        bound = [method_index[r] for r in predicate['supplemental_methods']]
        predicate_rows.append({'criterion_id': criterion['criterion_id'],
            'predicate_index_zero_based': predicate['index_zero_based'],
            'predicate_original': predicate['predicate_original'],
            'supplemental_methods': ' | '.join(predicate['supplemental_methods']),
            'base_methods_preserved': ' | '.join(predicate['base_evidence_methods_preserved']),
            'new_h04_methods_outside_staging': ' | '.join(predicate['new_h04_methods_pending_receipt_audit']),
            'adapted_sources_and_sha256': ' | '.join(sorted({r['source'] + ' sha256=' + r['source_sha256'] for r in bound})),
            'limit': predicate['limit'], 'current_status': 'NAO_EXECUTADO_NESTA_PORTAGEM', 'new_receipt': ''})
assert len(predicate_rows) == 118
write_csv('MATRIZ_39_CRITERIOS_PORTAGEM.csv', criteria_rows, list(criteria_rows[0]))
write_csv('MATRIZ_118_PREDICADOS_PORTAGEM.csv', predicate_rows, list(predicate_rows[0]))
write_json('MATRIZ_PORTAGEM.json', {'catalog_v2_source': str(CATALOG), 'catalog_v2_sha256': sha(CATALOG),
    'criteria': criteria_rows, 'predicates': predicate_rows, 'new_executions': 0})

base_paths = [ROOT / 'work/backend/pom.xml', ROOT / 'work/backend/AGENTS.md',
    ROOT / 'work/backend/src/test/resources/auditoria-endpoints.env',
    ROOT / 'work/backend/src/main/java/com/example/siggtrat/configuracao/VerificadorAmbienteMysql8045.java']
base_paths += [ROOT / 'work/backend/src/test/java' / PKG / name for name in
               ['AuditoriaHttpSuporte.java', 'ContratosHttpBase.java', 'FonteMotivoSintetica.java']]
base_paths += [ROOT / 'work/backend/src/test/java/com/example/siggtrat/infra' / name for name in
               ['ConfiguracaoMysqlIntegracao.java', 'GuardaMysql8045.java']]
write_json('DEPENDENCIAS_BASE_HASHES.json', {'scope': 'Hash de dependências atuais lidas, sem copiar ou alterar a base.',
    'files': [{'path': str(p), 'sha256': sha(p), 'bytes': p.stat().st_size} for p in base_paths]})

temporal = 'B230TemporalOperacionalComplementosHttpIT'
negative = 'alo008ComposicaoZeradaPorAditamentoImpedeRetomadaSemGravarReativacao'
temporal_positive = [m['reference'].split('#')[1] for m in methods
                     if m['reference'].startswith(temporal + '#') and not m['reference'].endswith('#' + negative)]
selectors = [
    {'round': 'SUP_A', 'selector': ','.join(sorted(p.stem for p in sources.values() if p.name.startswith('A244') and p.name.endswith('IT.java'))), 'planned': 6},
    {'round': 'SUP_B_CATALOGOS_RELATORIOS', 'selector': 'B230CatalogosPessoasComplementosHttpIT,B230RelatoriosComplementosHttpIT', 'planned': 19},
    {'round': 'SUP_B_TEMPORAIS', 'selector': temporal + '#' + '+'.join(temporal_positive), 'planned': 6},
    {'round': 'SUP_C', 'selector': 'CriteriosCDocumentosHttpIT,CriteriosCHistoricoHttpIT,CriteriosCOcorrenciasHttpIT', 'planned': 17},
    {'round': 'SUP_ALO_PREPARO_SEPARADO', 'selector': temporal + '#' + negative, 'planned': 1},
]
write_json('SELETORES_FOCAIS.json', {'status': 'SUGESTOES_NAO_EXECUTADAS', 'rounds': selectors})
command_lines = ['# Comandos focais sugeridos — ainda não executados', '',
    'O coordenador incorporará o pacote integral como suplemento opt-in em scripts/validacao/suplementos8045, fora do src/test normal. '
    'Executará após terminar a suíte pesada em andamento e preparar uma cópia isolada do backend. '
    'Sobrepor somente os 16 arquivos de src/test/java deste pacote nessa cópia; não copiar os originais, variantes ou diagnóstico. '
    'Todas as49 invocações, inclusive o negativo ALO, devem ter resultado preservado; os seletores abaixo não são uma alegação de full verde. '
    'O POM, dependências e fontes do produto permanecem os autorizados pelo coordenador.', '',
    'Exigir antes: Java 17.0.20 da campanha, conforme release17 do POM; SGT_MVN e SGT_MAVEN_REPOSITORY apontando para Maven/cache já aprovados; '
    'SGT_SUPPLEMENTAL_PROJECT para a cópia isolada; SGT_SUPPLEMENTAL_EXECUTE=1; '
    'INTEGRATION_MYSQL_MODE=native; INTEGRATION_MYSQL_URL para 127.0.0.1:33075/sgt_audit_8045_red '
    '(ou schema já autorizado sob o mesmo prefixo); INTEGRATION_MYSQL_USER=sgt_campaign. '
    'SGT_MYSQL_SERVER_UUID deve vir explicitamente do ambiente privado selado da rodada e conter um UUID completo válido. '
    'INTEGRATION_MYSQL_PASSWORD e REDIS_PASSWORD devem ser injetados pelo mecanismo privado existente, sem printenv ou eco. '
    'REDIS_HOST, REDIS_PORT e REDIS_DATABASE são explícitos e devem coincidir com as propriedades efetivas. '
    'Não há criação de schema, conta ou concessão neste pacote.', '',
    'O guard compara SELECT @@server_uuid com SGT_MYSQL_SERVER_UUID, sem valor padrão e sem adotar o valor observado. '
    'Exige MySQL/JDBC exatamente 8.0.45, '
    'JPA update e SQL init never. A conta observada em B230 continua estritamente sgt_campaign@127.0.0.1.', '',
    'A recriação do datadir após a interrupção das18:31:41Z rompeu a continuidade física da campanha. '
    'Os recibos anteriores conservam o UUID antigo e não provam persistência/restart do servidor novo. '
    'A nova identidade deve ser confirmada por recibo nativo independente antes de executar estes suplementos; '
    'não reutilizar evidências antigas como se pertencessem ao novo datadir.', '',
    'Os comandos usam os goals de compilação e Failsafe para selecionar estes IT; não executam a suíte unitária geral, '
    'não geram o JAR final e não representam clean verify. O limite de memória deve ser composto com o processo pai '
    'pelo coordenador; abaixo há apenas o teto de 768 MiB para o fork.', '']
for item in selectors:
    command_lines += ['## ' + item['round'], '',
        str(item['planned']) + (' invocação prevista' if item['planned'] == 1 else ' invocações previstas') + ' pela leitura das anotações; confirmar pela execução e pelo XML.', '', FENCE + 'bash',
        'flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \\',
        '  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \\',
        "  '-Dit.test=" + item['selector'] + "' \\",
        "  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \\",
        '  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \\',
        '  test-compile failsafe:integration-test failsafe:verify', FENCE, '']
command_lines += ['Preservar antes da rodada seguinte: console e código de saída, comando literal sem segredos, identidade nativa, '
    'hashes dos fontes/build, XML Failsafe, requests.jsonl e recibos específicos em target/a244-criteria, '
    'target/auditoria-http/b230-complementos.jsonl e target/criteria-c. Não reutilizar diretório de saída como se fosse recibo de nova rodada. '
    'O runner do coordenador deve proteger evidências de senhas/tokens e manter todos os asserts e resultados de falha.', '',
    'SUP_ALO_PREPARO_SEPARADO mantém o esperado201 no aditamento de preparo. Se ele receber409 antes da reativação, '
    'registrar PREPARO_INTERROMPIDO, com a etapa e resposta; não converter para PASS da retomada. '
    'Nenhum caso foi desabilitado na fonte: a separação só determina o recibo focal.', '']
(STAGE / 'COMANDOS_SUGERIDOS.md').write_text('\n'.join(command_lines))

readme = ['# Portagem dos suplementos históricos — fontes preparadas', '',
    '**Estado: não compilado e não executado nesta entrega.** O pacote não atribui PASS aos 39 critérios históricos. '
    'Há 12 classes de IT, 34 métodos anotados e 49 invocações previstas; essas quantidades não são resultados de execução. '
    'Os 118 predicados do índice independente V2 permanecem individualizados na matriz.', '',
    '## Conteúdo e decisões', '',
    '| Grupo | Fonte selecionada | Decisão |', '|---|---|---|',
    '| A244 | Atomicidade/Fotografia e suporte v2; quatro outros IT da pasta original | O suporte v2 verifica a raiz real configurada de armazenamento. Os seis corpos de teste são idênticos aos originais selecionados. |',
    '| B230 | b230_followup_cat001_v2, inclusive seu suporte | O override canônico aninhado de CAT-001 exige401/ACCOUNT_INACTIVE/LOGOUT. O RED antigo403 e ambos os hashes continuam preservados; não houve ajuste de negócio nesta portagem. |',
    '| C | criteria_c_probe_doc005_v6 | Preserva o snapshot com Hibernate.unproxy do v5 e os controles de estado pós-commit. Ocorrências permanece byte a byte; Histórico recebe somente wrappers de leitura readOnly nas fotografias antes/depois. |',
    '| Diagnóstico de finalidade textual | CriteriosCFinalidadeObservacaoHttpIT | Preservado fora de src/test/java e sem crédito criterial; não inventa proibição de texto diferente. |', '',
    'O pacote será entregue integralmente como suplemento opt-in, sem registro automático no src/test principal. '
    'São 15 fontes históricas selecionadas: 7 permanecem byte a byte. Os três suportes recebem ajustes de '
    'identidade/isolamento necessários à campanha nativa e helpers de observação em transação somente de leitura. '
    'As três classes B230 e CriteriosCHistorico mudam apenas o wrapper das consultas para o novo helper; suas assertions '
    'e as lambdas de escrita/fixtures ficam intactas. CriteriosCDocumentos também usa o helper para reler a versão e acrescenta '
    'duas comparações exigindo a resposta canônica completa desde o primeiro POST. O guard comum é o único arquivo novo. '
    '22 pares nome/SHA históricos, incluindo variantes superadas, estão em proveniencia_variantes; originais selecionados '
    'têm sua própria cópia e hash. PORTAGEM.diff explicita todas as alterações.', '',
    '## DOC-005: fortalecimento do oráculo', '',
    'A versão histórica v6 não fixa version=0 como esperado: ela registra a divergência do primeiro POST em relação ao GET, '
    'mas usa o GET pós-commit para o replay. A nova cópia exige JsonNode integral primeiroPOST=GET, nenhum campo divergente, '
    'GET.version=JPA.version e replay=GET, conservando as demais comparações de documentos, estado e arquivos antes/depois do409. '
    'Não se fixa version=1, não se remove version e não se afrouxa o expected status. A igualdade histórica primeiroPOST=replay '
    'também decorre das duas igualdades integrais agora exigidas. O RED independente do coordenador sobre os bytes H02 é externo '
    'a esta entrega; este pacote ainda não possui resultado novo.', '',
    '## Isolamento e fixtures', '',
    'A guarda de execução externa A244 antiga dependia de um diretório e selo H02 que não existem no projeto atual. '
    'Sua finalidade de isolamento foi substituída, por autorização do coordenador, por opt-in explícito e validação do endpoint, '
    'conta, prefixo de schema, UUID, versão real do servidor e metadata JDBC. A validação da DataSource ocorre por BeanPostProcessor '
    'antes do EntityManager e se repete antes dos casos. Reutiliza GuardaMysql8045; o gate do binário executável continua '
    'pertencendo aos recibos nativos do coordenador. Não é suficiente compilar ou simular esta guarda para homologar a campanha.', '',
    'O UUID esperado vem obrigatoriamente de SGT_MYSQL_SERVER_UUID no ambiente privado selado da rodada. '
    'O suporte valida o formato completo antes de abrir conexão e o compara com SELECT @@server_uuid; '
    'não existe fallback, UUID fixo ou adoção automática do servidor observado. Após a perda do redo na interrupção '
    'das18:31:41Z, o datadir anterior foi preservado e a campanha passou a um datadir novo. '
    'Os resultados anteriores permanecem vinculados ao UUID f241c2d2-d7a5-11f1-a2d7-73b42ec6596b; '
    'não há alegação de continuidade ou persistência entre os dois datadirs. '
    'Esta alteração de suporte não reexecuta nem reclassifica os recibos existentes.', '',
    'Conforme §133 e revisão independente, as fotografias B230/C e consultas diretas de controle executam '
    'REQUIRES_NEW/READ_COMMITTED com readOnly=true, limpando o contexto antes e depois. Os helpers nova/emTransacao '
    'continuam disponíveis e inalterados para o preparo e para as adulterações JPA deliberadas. A244 já tinha essa separação. '
    'Essa mudança é apenas de observação; todos os SELECTs e asserts mantêm a semântica. ', '',
    'O Redis usa o índice declarado no ambiente da campanha, em vez de9/12 herdados, e os quatro campos efetivos são comparados '
    'sem registrar valores secretos. A raiz de upload C permanece target/criteria-c/uploads; A244 preserva a conferência da raiz '
    'efetivamente configurada e contida no projeto. Não houve DDL/DML manual a converter: os SQLs presentes leem identidade e '
    'snapshots; as fixtures e mutações de controle usam JPA ou comandos HTTP MockMvc. Os deletes de requests são comandos de '
    'negócio da API, não SQL manual. A fonte sintética de adulteração AUD-001 é intencional e permanece na base isolada.', '',
    '## Limites de aceite', '',
    '- ALO-008:002: o caso negativo conserva a preparação histórica que esperava201 no aditamento. No acervo ela parou em409 '
    'antes da reativação. A execução focal separada deve manter essa distinção e não transformar rejeição do preparo em prova '
    'da guarda de retomada. Nenhum estado de negócio inválido foi fabricado para forçar a etapa final.',
    '- PER-010:004: os testes de fronteira temporal cobrem os predicados mapeados; a lacuna histórica de concessão/contexto '
    'multidia e matriz completa de entradas permanece explicitamente no mapa. Não se herda fechamento total.',
    '- AUD-006:002/004: aparecem entre os39, mas não trazem suplemento histórico Java portável neste recorte. O V2 aponta '
    'ContraprovasPendenciasH04HttpIT do coordenador; seus recibos são avaliados separadamente, sem atribuição por esta portagem.',
    '- ADI-004 usa um Aspect exclusivo do teste para falhar após escritas/flush e verificar rollback. VIN-013 controla '
    'RelogioSistema.hoje. O scheduler herdado é substituído para evitar tarefas de fundo. Esses limites permanecem no código.',
    '- Todos estes IT usam MockMvc autenticado e serviços/JPA reais quando executados no MySQL autorizado. Não exercitam '
    'TCP HTTP nem Chrome. As49 invocações planejadas não correspondem a49 critérios nem à homologação de39 critérios.',
    '- Snapshots integrais excluem apenas token_ativo e registro_auditoria nos suportes A/B, conforme o oráculo histórico. '
    'Alterações nesses registros técnicos não devem ser vendidas como parte do rollback negocial. O runner deve conservar '
    'a comparação integral e publicar apenas evidências sanitizadas.', '',
    '## Arquivos de revisão', '',
    '- MATRIZ_39_CRITERIOS_PORTAGEM.csv e MATRIZ_118_PREDICADOS_PORTAGEM.csv: método, fonte/hash, base preservada, limite e '
    'coluna vazia para o novo recibo; nenhuma aprovação herdada.',
    '- INVENTARIO_FONTES_ADAPTADAS.json, DEPENDENCIAS_BASE_HASHES.json e METODOS_E_INVOCACOES_PLANEJADAS.json: bytes e escopo.',
    '- COMANDOS_SUGERIDOS.md e SELETORES_FOCAIS.json: cinco seleções reproduzíveis, com ALO negativo discriminado.',
    '- SUPLEMENTOS_8045_OVERLAY.tar.gz: somente os16 arquivos a sobrepor na cópia isolada autorizada pelo coordenador.',
    '- MANIFESTO_STAGING.json e SHA256SUMS.txt: selo desta entrega. Nenhum arquivo do work React/backend foi alterado por '
    'esta portagem.', '']
(STAGE / 'README_PORTAGEM.md').write_text('\n'.join(readme))

with (STAGE / 'SUPLEMENTOS_8045_OVERLAY.tar.gz').open('wb') as raw:
    with gzip.GzipFile(fileobj=raw, filename='', mode='wb', mtime=0) as compressed:
        with tarfile.open(fileobj=compressed, mode='w') as archive:
            for path in sorted(sources.values()):
                data = path.read_bytes()
                info = tarfile.TarInfo(str(path.relative_to(STAGE)))
                info.size = len(data)
                info.mode = 0o644
                info.mtime = 0
                archive.addfile(info, io.BytesIO(data))

excluded = {'MANIFESTO_STAGING.json', 'SHA256SUMS.txt'}
artifacts = [{'path': str(p.relative_to(STAGE)), 'sha256': sha(p), 'bytes': p.stat().st_size}
             for p in sorted(STAGE.rglob('*')) if p.is_file() and p.name not in excluded]
write_json('MANIFESTO_STAGING.json', {'generated_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'status': 'PREPARADO_NAO_COMPILADO_NAO_EXECUTADO', 'artifacts': artifacts,
    'counts': {'source_files': 16, 'historical_selected': 15, 'historical_identical': 7,
               'historical_variants_preserved': 22, 'IT_classes': 12, 'test_methods': 34,
               'planned_invocations_static': 49, 'criteria': 39, 'predicates': 118, 'executed': 0}})
listed = sorted([STAGE / a['path'] for a in artifacts] + [STAGE / 'MANIFESTO_STAGING.json'])
(STAGE / 'SHA256SUMS.txt').write_text(''.join(sha(p) + '  ' + str(p.relative_to(STAGE)) + '\n' for p in listed))
print(json.dumps({'source_files': 16, 'IT_classes': 12, 'test_methods': 34, 'planned_invocations_static': 49,
                  'criteria': 39, 'predicates': 118, 'executed': 0,
                  'manifest_sha256': sha(STAGE / 'MANIFESTO_STAGING.json'),
                  'sha256sums_sha256': sha(STAGE / 'SHA256SUMS.txt'),
                  'overlay_sha256': sha(STAGE / 'SUPLEMENTOS_8045_OVERLAY.tar.gz')}, ensure_ascii=False))
