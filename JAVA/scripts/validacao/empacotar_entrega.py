#!/usr/bin/env python3
"""Empacota uma árvore de distribuição já validada, com manifests e ZIP completo.

Não compila, executa testes ou decide requisitos. Verifica fontes/JAR contra as
provas incluídas; gera hashes internos sem referência circular; relê o ZIP e
confere CRC/SHA de cada arquivo. O checkpoint final tem os mesmos bytes do ZIP.
"""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import shutil
import zipfile


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def ler_prova_referenciada(project, evidence, key):
    """Confere a proveniência prometida pelo índice; não decide o status da prova."""
    relative, expected_hash = evidence.get(key), evidence.get(key + '_sha256')
    assert isinstance(relative, str) and relative and isinstance(expected_hash, str) \
        and len(expected_hash) == 64, 'Índice sem par completo de arquivo/SHA: ' + key
    relative = Path(relative)
    assert not relative.is_absolute() and '..' not in relative.parts, 'Caminho de prova inválido: ' + key
    path = (project / relative).resolve()
    assert path.is_relative_to(project.resolve()), 'Prova fora da distribuição: ' + key
    assert path.is_file(), 'Prova referenciada ausente: ' + key
    assert sha(path) == expected_hash, 'Hash da prova referenciada diverge: ' + key
    return json.loads(path.read_text())


def verificar_provas_tcp(project, caminhos):
    """Exige os dois roteiros reais nas duas fases, ligados aos scripts incluídos."""
    project = project.resolve()
    assert len(caminhos) == 4, 'Esperadas quatro provas TCP.'
    arquivos = [(project / path).resolve() for path in caminhos]
    assert len(set(arquivos)) == 4, 'As quatro provas TCP devem ser arquivos distintos.'
    assert all(path.is_relative_to(project) for path in arquivos), 'Prova TCP fora da distribuição.'
    roteiros = ('validar_jar_tcp.py', 'validar_auditorias_tcp.py')
    pares_esperados = {(roteiro, fase) for roteiro in roteiros for fase in ('criar', 'reinicio')}
    hashes_roteiros = {roteiro: sha(project / 'scripts/validacao' / roteiro) for roteiro in roteiros}
    utilitario = 'validar_jar_tcp.py'
    pares = set()
    for path in arquivos:
        tcp = json.loads(path.read_text())
        par = (tcp.get('roteiro'), tcp.get('fase'))
        assert par in pares_esperados, 'Roteiro/fase TCP não reconhecido: ' + str(path)
        assert par not in pares, 'Roteiro/fase TCP repetido: ' + str(par)
        assert tcp.get('roteiro_sha256') == hashes_roteiros[par[0]], 'Hash do roteiro TCP diverge: ' + str(path)
        assert tcp.get('utilitario') == utilitario, 'Utilitário TCP não reconhecido: ' + str(path)
        assert tcp.get('utilitario_sha256') == hashes_roteiros[utilitario], 'Hash do utilitário TCP diverge: ' + str(path)
        assert tcp['concluido'] is True and tcp['sucesso'] is True and tcp['erro'] is None, path
        assert tcp['assercoes'] and all(check['passou'] is True for check in tcp['assercoes']), path
        pares.add(par)
    assert pares == pares_esperados, 'Falta roteiro/fase TCP obrigatório.'



def ler_casos_confirmados_por_xml(round_dir):
    """Confere cada invocação e seu estado no XML arquivado, sem executar testes."""
    import collections
    import re
    import xml.etree.ElementTree as ET

    cases = json.loads((round_dir / 'RESULTADOS_METODOS.json').read_text())
    result = json.loads((round_dir / 'RESUMO.json').read_text())
    assert isinstance(cases, list) and cases, 'Rodada sem invocações: ' + str(round_dir)
    groups = ('surefire-reports', 'failsafe-reports')
    referenced = {case['xml'] for case in cases}
    actual_files = {str(path.relative_to(round_dir)) for group in groups
                    for path in (round_dir / group).glob('TEST-*.xml')}
    assert referenced == actual_files, 'XMLs e catálogo de invocações divergem: ' + str(round_dir)
    # Na distribuição, XMLs podem ter somente valores efêmeros mascarados.
    # O catálogo conserva o SHA original; o manifesto vincula os bytes entregues.
    evidence_root = round_dir.parent.parent.resolve()
    sanitization_path = evidence_root / 'MANIFESTO_SANITIZACAO.json'
    sanitization = None
    if sanitization_path.exists():
        manifest = json.loads(sanitization_path.read_text())
        assert isinstance(manifest.get('arquivos'), list), 'Manifesto de sanitização sem inventário.'
        sanitization = {}
        for entry in manifest['arquivos']:
            relative = Path(entry['arquivo'])
            assert relative.parts and not relative.is_absolute() and '..' not in relative.parts, \
                'Caminho inválido no manifesto de sanitização.'
            distributed = (evidence_root / relative).resolve()
            assert distributed.is_relative_to(evidence_root), 'Sanitização fora da raiz de evidências.'
            assert distributed not in sanitization, 'Arquivo repetido no manifesto de sanitização.'
            sanitization[distributed] = entry
    observed = []
    class_summaries = []
    suite_names = {}
    counts = {'testes': 0, 'falhas': 0, 'erros': 0, 'ignorados': 0, 'aprovados': 0}
    for relative in sorted(referenced):
        path = (round_dir / relative).resolve()
        parts = Path(relative).parts
        assert len(parts) == 2 and parts[0] in groups and path.is_relative_to(round_dir.resolve()), \
            'Caminho XML fora do papel de execução: ' + relative
        actual_hash = sha(path)
        xml_hash = actual_hash
        if sanitization is not None:
            assert path in sanitization, 'XML omitido do manifesto de sanitização: ' + relative
            entry = sanitization[path]
            assert actual_hash == entry['sha256_distribuido'], 'Hash distribuído do XML diverge: ' + relative
            assert path.stat().st_size == entry['bytes'], 'Tamanho do XML sanitizado diverge: ' + relative
            xml_hash = entry['sha256_original']
            assert isinstance(xml_hash, str) and re.fullmatch(r'[0-9a-f]{64}', xml_hash), \
                'SHA original inválido no manifesto de sanitização: ' + relative
        xml = ET.parse(path).getroot()
        assert xml.tag == 'testsuite' and xml.get('name'), 'XML sem suíte identificada: ' + relative
        suite_name = xml.attrib['name']
        assert parts[1] == 'TEST-' + suite_name + '.xml', 'Nome do arquivo diverge da suíte XML: ' + relative
        suite_names[relative] = suite_name
        elements = xml.findall('testcase')
        local = {'testes': len(elements), 'falhas': 0, 'erros': 0, 'ignorados': 0}
        for case in elements:
            issue = next((e for e in case if e.tag in ('failure', 'error', 'skipped')), None)
            state = issue.tag.upper() if issue is not None else 'PASSOU'
            invocation = case.attrib['name']
            method = re.split(r'[\[(]', invocation, maxsplit=1)[0]
            observed.append((case.get('classname', xml.get('name')), method, invocation, state,
                             relative, xml_hash))
            if issue is not None:
                local[{'failure': 'falhas', 'error': 'erros', 'skipped': 'ignorados'}[issue.tag]] += 1
        for key, attr in [('testes', 'tests'), ('falhas', 'failures'), ('erros', 'errors'), ('ignorados', 'skipped')]:
            assert int(xml.get(attr, '0')) == local[key], 'Contagem XML diverge dos casos: ' + relative
            counts[key] += local[key]
        class_summaries.append({'classe': xml.get('name'), **local, 'xml_sha256': xml_hash})
    expected = [(v['classe'], v['metodo'], v['invocacao'], v['estado'], v['xml'], v['xml_sha256']) for v in cases]
    assert collections.Counter(observed) == collections.Counter(expected), \
        'Invocação, estado ou hash não corresponde ao XML: ' + str(round_dir)
    counts['aprovados'] = counts['testes'] - counts['falhas'] - counts['erros'] - counts['ignorados']
    assert result['totais'] == counts, 'Totais do resumo não correspondem aos XMLs: ' + str(round_dir)
    assert result['classes'] == len(class_summaries)
    assert sorted(result['classes_resultados'], key=lambda item: item['classe']) == \
           sorted(class_summaries, key=lambda item: item['classe']), 'Resumo por classe diverge dos XMLs.'
    assert all(case['estado'] == 'PASSOU' for case in cases), 'A rodada contém caso não aprovado.'
    # Identidade da suíte é derivada do XML conferido. O classname bruto e os
    # catálogos/hash arquivados permanecem intactos; @DisplayName é permitido.
    return [{**case, 'classe_suite_xml': suite_names[case['xml']]} for case in cases]


def verificar_papeis_rodadas(rodadas_dir, nome_integral, nome_carga):
    """Integral e carga têm identidades e provas distintas, independentes do nome."""
    rodadas_dir = rodadas_dir.resolve()
    integral = (rodadas_dir / nome_integral).resolve()
    carga = (rodadas_dir / nome_carga).resolve()
    assert nome_integral != nome_carga and integral != carga, \
        'Integral e carga devem ser rodadas distintas, inclusive após resolver o caminho.'
    assert integral.is_relative_to(rodadas_dir) and carga.is_relative_to(rodadas_dir), \
        'Rodada fora da raiz de evidências.'
    integral_cases = ler_casos_confirmados_por_xml(integral)
    load_cases = ler_casos_confirmados_por_xml(carga)
    load_class = 'com.example.siggtrat.integration.DatabaseLoadIT'
    unit = [v for v in integral_cases if Path(v['xml']).parts[0] == 'surefire-reports']
    integration = [v for v in integral_cases if Path(v['xml']).parts[0] == 'failsafe-reports'
                   and v['classe_suite_xml'] != load_class]
    load = [v for v in load_cases if Path(v['xml']).parts[0] == 'failsafe-reports'
            and v['classe_suite_xml'] == load_class
            and v['xml'] == 'failsafe-reports/TEST-' + load_class + '.xml'
            and v['metodo'] == v['invocacao'] == 'mixedReadWriteLoad']
    assert unit, 'O papel integral exige unitários comprovados por XML surefire.'
    assert integration, 'O papel integral exige ITs não-load comprovados por XML failsafe.'
    assert load, 'O papel carga exige DatabaseLoadIT#mixedReadWriteLoad aprovado em XML failsafe.'
    return {'integral': {'nome': nome_integral, 'casos': integral_cases,
                         'unitarios': len(unit), 'integracao_nao_load': len(integration)},
            'carga': {'nome': nome_carga, 'casos': load_cases, 'invocacoes_database_load': len(load)}}


def verificar_matriz_requisitos(matrix):
    """Confere o formato declarado sem traduzir preservação de escopo em aceite."""
    rows = matrix['requisitos']
    metadata = matrix['metadados']
    assert isinstance(rows, list) and len(rows) == 198, 'Esperados 198 requisitos.'
    current = 'classificacoes' in metadata or any('classificacao' in row for row in rows)
    legacy = 'contagem_requisitos' in metadata or any('classificacao_final' in row for row in rows)
    assert current != legacy, 'Formato da matriz ausente ou misto; não há fallback histórico.'
    count_key, row_key, criteria_key = (
        ('classificacoes', 'classificacao', 'criterios_recebidos') if current else
        ('contagem_requisitos', 'classificacao_final', 'criterios_decompostos_recebidos'))
    counts = metadata.get(count_key)
    allowed = {'PRESERVADO', 'BLOQUEADO'} if current else {'A', 'B', 'C', 'D', 'E'}
    assert isinstance(counts, dict) and counts and set(counts) <= allowed, 'Classificações inválidas.'
    assert all(type(value) is int and value >= 0 for value in counts.values()), 'Contagem inválida.'
    assert sum(counts.values()) == 198, 'Contagem total de requisitos divergente.'
    assert all(row.get(row_key) in counts for row in rows), 'Classificação ausente ou desconhecida.'
    assert all(sum(row[row_key] == status for row in rows) == count
               for status, count in counts.items()), 'Contagem por classificação divergente.'
    ids = [row.get('id') for row in rows]
    assert all(isinstance(value, str) and value for value in ids) and len(set(ids)) == 198, \
        'IDs de requisitos ausentes ou repetidos.'
    assert all(isinstance(row.get('criterios'), list) and row['criterios'] for row in rows), \
        'Requisito sem critérios documentados.'
    criteria = [criterion for row in rows for criterion in row['criterios']]
    criterion_ids = [criterion.get('id') for criterion in criteria]
    assert len(criteria) == metadata.get(criteria_key) == 635, 'Contagem de critérios divergente.'
    assert all(isinstance(value, str) and value for value in criterion_ids) \
        and len(set(criterion_ids)) == len(criteria), 'IDs de critérios ausentes ou repetidos.'
    assert all(criterion['id'].startswith(row['id'] + ':')
               for row in rows for criterion in row['criterios']), 'Critério pertence a outro requisito.'
    if current:
        assert metadata.get('total_requisitos_originais') == 198, 'Total original divergente.'
        validation = metadata.get('validacao_atual')
        assert isinstance(validation, dict) and validation \
            and all(type(value) is int and value >= 0 for value in validation.values()) \
            and sum(validation.values()) == 198 \
            and all(row.get('validacao_atual') in validation for row in rows) \
            and all(sum(row['validacao_atual'] == status for row in rows) == count
                    for status, count in validation.items()), 'Contagem de validação atual divergente.'
        assert type(metadata.get('homologacao_integral')) is bool, 'Homologação deve ser explícita.'
        for row in rows:
            assert type(row.get('aceite_integral_atual')) is bool \
                and all(type(criterion.get('aceite_integral_atual')) is bool
                        for criterion in row['criterios']), 'Aceite atual deve ser explícito.'
            if row['classificacao'] == 'BLOQUEADO':
                assert row.get('bloqueio_documental_recebido') and not row['aceite_integral_atual'], \
                    'Bloqueio documental ausente ou convertido em aceite.'
            assert not row['aceite_integral_atual'] or all(
                criterion['aceite_integral_atual'] for criterion in row['criterios']), \
                'Aceite do requisito sem aceite de todos os critérios.'
        assert not metadata['homologacao_integral'] or all(row['aceite_integral_atual'] for row in rows), \
            'Homologação integral sem aceite de todos os requisitos.'
    return dict(counts)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--projeto', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    parser.add_argument('--nome-zip', default='SGT_GTRAT_BACKEND_CORRIGIDO_VALIDADO.zip')
    parser.add_argument('--zip-leve', action='store_true',
                        help='Gera também fontes/testes/documentação sem JAR e sem acervos brutos de provas.')
    args = parser.parse_args()
    project = args.projeto.resolve()
    output = args.saida.resolve()
    assert not output.is_relative_to(project), 'A saída deve ficar fora da árvore empacotada.'
    output.mkdir(parents=True, exist_ok=True)
    evidence = json.loads((project / 'docs/VALIDACAO_DISTRIBUICAO.json').read_text())
    # O índice já promete este hash. Sua integridade não aprova requisitos,
    # navegador ou qualquer status cujo contrato não é interpretado aqui.
    gates = ler_prova_referenciada(project, evidence, 'gates')
    papeis_rodadas = verificar_papeis_rodadas(project / 'evidencias/rodadas',
        evidence['rodada_integral'], evidence['rodada_carga'])
    proof = project / 'evidencias/rodadas' / evidence['rodada_integral']
    expected = json.loads((proof / 'FONTES_ANTES.json').read_text())
    source_files = [project / 'pom.xml'] + [p for p in (project / 'src').rglob('*') if p.is_file()]
    current = {str(p.relative_to(project)): sha(p) for p in source_files}
    assert current == expected, 'O código não é o código executado.'
    for name in (evidence['rodada_integral'], evidence['rodada_carga']):
        round_dir = project / 'evidencias/rodadas' / name
        result = json.loads((round_dir / 'RESUMO.json').read_text())
        before = json.loads((round_dir / 'FONTES_ANTES.json').read_text())
        after = json.loads((round_dir / 'FONTES_DEPOIS.json').read_text())
        assert current == before == after, 'Rodada de outra árvore: ' + name
        assert result['processo']['exit_code'] == 0
        assert not result['processo']['fontes_alteradas_durante_rodada']
        assert not result['discrepancias']
        assert result['totais']['testes'] > 0
        assert all(result['totais'][key] == 0 for key in ('falhas', 'erros', 'ignorados'))
    jar_proof = ler_prova_referenciada(project, evidence, 'prova_classes_jar')
    jar = project / 'target' / jar_proof['jar']
    assert sha(jar) == jar_proof['jar_sha256'], 'O JAR diverge da prova do binário.'
    assert jar_proof['classes_identicas'] is True and not jar_proof['diferencas_classes']
    expected_classes = json.loads((proof / 'CLASSES_EXERCITADAS_SHA256.json').read_text())
    with zipfile.ZipFile(jar) as binary:
        jar_classes = {name.removeprefix('BOOT-INF/classes/'): hashlib.sha256(binary.read(name)).hexdigest()
                       for name in binary.namelist()
                       if name.startswith('BOOT-INF/classes/') and name.endswith('.class')}
    assert jar_classes == expected_classes, 'Classes empacotadas não são as exercitadas.'
    http = ler_prova_referenciada(project, evidence, 'prova_http_reinicio')
    assert http['aprovado'] is True and http['jar_sha256'] == sha(jar)
    assert http['mesmo_jar_apos_reinicio'] is True
    assert http['schema_e_dados_preservados_apos_reinicio'] is True
    assert http['orfaos_apos_reinicio'] == 0
    verificar_provas_tcp(project, evidence['provas_tcp'])
    assert not any(path.name == '.env' for path in project.rglob('*')), '.env não pertence à distribuição.'
    assert (project / '.env.example').is_file(), 'Exemplo público .env.example ausente.'
    matrix = json.loads((project / 'docs/MATRIZ_REQUISITOS_FINAL.json').read_text())
    counts = verificar_matriz_requisitos(matrix)
    # Pendências honestas não impedem entregar a implementação testada. Somente
    # a matriz decide os IDs comprovados; não converter B/E ou PRESERVADO em aceite.
    assert (project / 'docs/PENDENCIAS_FINAIS.md').is_file()
    assert not any(p.is_symlink() for p in project.rglob('*')), 'Symlinks não são aceitos no pacote.'

    # Caches da regeneração Python não pertencem ao projeto entregue.
    for cache in list(project.rglob('__pycache__')):
        shutil.rmtree(cache)
    # Alguns meios de transporte preservam bytes, mas retiram o modo POSIX.
    # Fixar o modo dentro deste processo garante as permissões no ZIP produzido.
    for executable in [project / 'mvnw', *(project / 'scripts').rglob('*.sh')]:
        executable.chmod((executable.stat().st_mode & 0o777) | 0o111)
    excluded = {'MANIFESTO_SHA256.json', 'SHA256_CONTEUDO.txt'}
    files = sorted(p for p in project.rglob('*') if p.is_file())
    assert not any('.git' in p.relative_to(project).parts for p in files)
    payload = [p for p in files if str(p.relative_to(project)) not in excluded]
    entries = [{'arquivo': str(p.relative_to(project)), 'bytes': p.stat().st_size, 'sha256': sha(p)}
               for p in payload]
    manifest = {'gerado_em_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
        'escopo': 'Conteúdo atual da distribuição; manifestos recebidos permanecem históricos.',
        'excluidos_deste_manifesto': sorted(excluded),
        'motivo_exclusao': 'Evitar referências circulares entre os arquivos de hashes.',
        'arquivos': entries, 'fontes_compiladas_conferidas': len(current),
        'classificacao_funcional': counts, 'jar_sha256': sha(jar),
        'matriz_requisitos_sha256': sha(project / 'docs/MATRIZ_REQUISITOS_FINAL.json'),
        'gates_proveniencia': {'arquivo': evidence['gates'], 'sha256': evidence['gates_sha256'],
                               'integridade_conferida': True, 'aprovacao_de_negocio_inferida': False},
        'limite_classificacao': 'Rótulos da matriz preservados; preservação de escopo não significa aprovação.'}
    manifest_path = project / 'MANIFESTO_SHA256.json'
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    lines = [f"{entry['sha256']}  {entry['arquivo']}" for entry in entries]
    lines.append(f'{sha(manifest_path)}  MANIFESTO_SHA256.json')
    checksums = project / 'SHA256_CONTEUDO.txt'
    checksums.write_text('\n'.join(lines) + '\n')
    distributed = sorted(p for p in project.rglob('*') if p.is_file())
    hashes = {project.name + '/' + str(p.relative_to(project)): sha(p) for p in distributed}
    archive_path = output / args.nome_zip
    assert not archive_path.exists(), 'Não sobrescrever um pacote já registrado.'
    with zipfile.ZipFile(archive_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in distributed:
            archive.write(path, project.name + '/' + str(path.relative_to(project)))
        for directory in sorted(p for p in project.rglob('*') if p.is_dir() and not any(p.iterdir())):
            archive.write(directory, project.name + '/' + str(directory.relative_to(project)) + '/')
    with zipfile.ZipFile(archive_path) as archive:
        assert archive.testzip() is None, 'CRC inválido no pacote.'
        assert {p.filename for p in archive.infolist() if not p.is_dir()} == set(hashes)
        assert len(archive.namelist()) == len(set(archive.namelist())), 'Entradas repetidas no ZIP.'
        for name, digest in hashes.items():
            with archive.open(name) as stream:
                actual = hashlib.file_digest(stream, 'sha256').hexdigest()
            assert actual == digest, name
    checkpoint = output / 'CHECKPOINT_02_IMPLEMENTACAO_FINAL.zip'
    assert not checkpoint.exists()
    shutil.copy2(archive_path, checkpoint)
    assert sha(checkpoint) == sha(archive_path)
    verification = {'instante_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
        'arquivo': archive_path.name, 'checkpoint': checkpoint.name, 'mesmos_bytes': True,
        'bytes': archive_path.stat().st_size, 'sha256': sha(archive_path),
        'arquivos': len(distributed), 'crc_verificado': True,
        'sha256_de_cada_entrada_verificado': True, 'fontes_compiladas_conferidas': len(current),
        'jar_sha256': sha(jar), 'classificacao': counts,
        'manifesto_sha256': sha(manifest_path), 'sha256_conteudo_txt': sha(checksums)}
    if args.zip_leve:
        excluded_roots = {'target', 'evidencias', 'auditoria_independente', 'analise', 'dados'}
        light_files = [path for path in distributed
                       if path.relative_to(project).parts[0] not in excluded_roots
                       and not str(path.relative_to(project)).startswith('docs/HISTORICO/')
                       and path.name not in excluded]
        light_hashes = {project.name + '/' + str(path.relative_to(project)): sha(path) for path in light_files}
        light_manifest = {'escopo': 'Fontes, testes, scripts e relatórios. JAR e evidências brutas estão no ZIP completo.',
                          'zip_completo': archive_path.name, 'sha256_zip_completo': sha(archive_path),
                          'arquivos': light_hashes}
        light_name = project.name + '/MANIFESTO_PACOTE_LEVE.json'
        light_bytes = (json.dumps(light_manifest, ensure_ascii=False, indent=2) + '\n').encode()
        light_hashes[light_name] = hashlib.sha256(light_bytes).hexdigest()
        light = output / 'SGT_GTRAT_FONTES_TESTES_DOCUMENTACAO.zip'
        assert not light.exists()
        with zipfile.ZipFile(light, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for path in light_files:
                archive.write(path, project.name + '/' + str(path.relative_to(project)))
            archive.writestr(light_name, light_bytes)
        with zipfile.ZipFile(light) as archive:
            assert archive.testzip() is None
            assert len(archive.namelist()) == len(light_hashes)
            assert set(archive.namelist()) == set(light_hashes)
            for name, digest in light_hashes.items():
                with archive.open(name) as stream:
                    assert hashlib.file_digest(stream, 'sha256').hexdigest() == digest, name
        verification['pacote_leve'] = {'arquivo': light.name, 'bytes': light.stat().st_size,
                                      'sha256': sha(light), 'arquivos': len(light_hashes),
                                      'crc_e_sha256_de_cada_entrada_verificados': True}
    (output / 'VERIFICACAO_EMPACOTAMENTO.json').write_text(json.dumps(verification, ensure_ascii=False, indent=2) + '\n')
    archives = [archive_path, checkpoint]
    if args.zip_leve:
        archives.append(light)
    (output / 'SHA256_ZIPS.txt').write_text(''.join(f'{sha(path)}  {path.name}\n' for path in archives))
    print(json.dumps(verification, ensure_ascii=False))


if __name__ == '__main__':
    main()
