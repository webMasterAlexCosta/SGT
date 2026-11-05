#!/usr/bin/env python3
"""Consolida avaliações humanas e liga critérios a arquivos e resultados JUnit reais.

Não infere completude de cobertura, presença de método ou quantidade de testes.
Classificações são decisões funcionais explícitas nas avaliações de entrada.
"""
import argparse
import collections
import datetime as dt
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

LABELS = {'A': 'Implementado integralmente', 'B': 'Implementado parcialmente',
          'C': 'Implementado incorretamente', 'D': 'Não implementado',
          'E': 'Ambíguo ou bloqueado'}

def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def test_name(value):
    return re.split(r'[\[(]', value, maxsplit=1)[0]

def collect_reports(report_root):
    cases, summaries = [], {}
    for group in ('surefire-reports', 'failsafe-reports'):
        count = collections.Counter()
        for p in sorted((report_root / group).glob('TEST-*.xml')):
            root = ET.parse(p).getroot()
            for key in ('tests', 'failures', 'errors', 'skipped'):
                count[key] += int(root.get(key, 0))
            digest = sha(p)
            for t in root.findall('testcase'):
                status = ('FALHOU' if t.find('failure') is not None else
                          'ERRO' if t.find('error') is not None else
                          'IGNORADO' if t.find('skipped') is not None else 'APROVADO')
                cases.append({'classe': t.get('classname', root.get('name')),
                              'metodo': test_name(t.get('name', '')),
                              'caso': t.get('name'), 'resultado': status,
                              'xml': str(p.relative_to(report_root)),
                              'sha256_xml': digest, 'segundos': t.get('time')})
        summaries[group] = dict(count)
    return cases, summaries

def references(row):
    refs = []
    for val in row.get('testes_existentes', []) + row.get('testes_implementados', []):
        if isinstance(val, str):
            refs.append(val)
        elif isinstance(val, dict):
            refs.append(val.get('classe_metodo') or val.get('teste') or '')
    for criterion in row.get('matriz_criterios', []):
        refs.extend(criterion.get('testes', []))
    return list(dict.fromkeys(v for v in refs if v))

def matching(ref, cases):
    cls, _, method = ref.partition('#')
    cls = cls.removesuffix('.java')
    return [c for c in cases if (c['classe'] == cls or c['classe'].endswith('.' + cls))
            and (not method or c['metodo'] == test_name(method))]

def plain(value):
    if value is None:
        return 'Não se aplica nesta fase.'
    if isinstance(value, list):
        return '; '.join(plain(v) for v in value) or 'Nenhuma lacuna identificada nesta avaliação.'
    if isinstance(value, dict):
        return '; '.join(f'{k}: {plain(v)}' for k, v in value.items())
    return str(value).replace('\n', ' ')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fase', choices=['baseline', 'final'], required=True)
    parser.add_argument('--relatorios', type=Path, required=True)
    parser.add_argument('--avaliacoes', type=Path)
    parser.add_argument('--saida', type=Path)
    parser.add_argument('--componentes-base', type=Path, help='Fonte congelada antes da implementação, quando fase=baseline')
    parser.add_argument('--manifesto-fontes', type=Path,
                        help='Manifesto da cópia efetivamente compilada; confere todos os fontes e o pom.xml')
    parser.add_argument('--evidencias-suplementares', type=Path, action='append', default=[])
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    components_base = args.componentes_base or project
    source_manifest = None
    if args.manifesto_fontes:
        expected = json.loads(args.manifesto_fontes.read_text())['fontes']
        paths = [components_base / 'pom.xml'] + [p for p in (components_base / 'src').rglob('*') if p.is_file()]
        actual = {str(p.relative_to(components_base)): sha(p) for p in sorted(paths)}
        differences = sorted(k for k in expected.keys() | actual.keys()
                             if expected.get(k) != actual.get(k))
        assert not differences, 'Fontes distintos dos executados: ' + ', '.join(differences)
        source_manifest = {'arquivo': str(args.manifesto_fontes), 'sha256': sha(args.manifesto_fontes),
                           'arquivos_verificados': len(actual), 'correspondencia_integral': True}
    inputs = args.avaliacoes or project / 'docs/implementacao-198/fontes-avaliacao'
    output = args.saida or project / 'docs/implementacao-198'
    output.mkdir(parents=True, exist_ok=True)
    normative = json.loads((inputs / 'requisitos_canonicos.json').read_text())
    ids = [r['id'] for r in normative]
    literal = {r['id']: r['enunciado_original'] for r in normative}
    rows = []
    for p in sorted(inputs.glob(f'avaliacao_*_{args.fase}.json')):
        rows.extend(json.loads(p.read_text()))
    assert len(rows) == 198 and len({r['id'] for r in rows}) == 198, '198 IDs únicos obrigatórios'
    assert set(ids) == {r['id'] for r in rows}, 'IDs divergem do documento original'
    rows.sort(key=lambda r: ids.index(r['id']))
    cases, report_summary = collect_reports(args.relatorios)
    supplemental_summary = []
    for folder in args.evidencias_suplementares:
        extra_cases, extra_summary = collect_reports(folder)
        for case in extra_cases:
            case['origem_suplementar'] = folder.name
        cases.extend(extra_cases)
        supplemental_summary.append({'origem': folder.name, 'execucao': extra_summary})
    assert cases, 'Sem XMLs de execução: não produzir matriz comprovada'
    counts = collections.Counter()
    missing = []
    for row in rows:
        assert row['enunciado_original'] == literal[row['id']], row['id'] + ': enunciado adulterado'
        decision = row['classificacao_antes'] if args.fase == 'baseline' else row['classificacao_depois']
        assert decision in LABELS, row['id'] + ': classificação explícita ausente'
        counts[decision] += 1
        if args.fase == 'baseline':
            row['classificacao_depois'] = None
        row['classificacao_principal'] = decision
        row['classificacao_provisoria'] = False
        row['resultado_baseline' if args.fase == 'baseline' else 'resultado_final'] = 'EXECUTADO_XML_RASTREADO'
        row['nota_metodologica'] = ('A indica ausência de lacuna funcional conhecida nos critérios confrontados e '
            'testes correspondentes aprovados. Não indica homologação independente nem cobertura exaustiva '
            'de combinações. A execução isolada nunca promove automaticamente um requisito a A.')
        evidence = []
        for ref in references(row):
            found = matching(ref, cases)
            if not found:
                missing.append({'id': row['id'], 'referencia': ref})
            evidence.append({'referencia': ref, 'casos': found,
                             'resultado': 'APROVADO' if found and all(c['resultado'] == 'APROVADO' for c in found)
                                          else 'SEM_EVIDENCIA' if not found else 'NAO_APROVADO'})
        row['evidencia_execucao'] = evidence
        by_ref = {e['referencia']: e for e in evidence}
        for criterion in row.get('matriz_criterios', []):
            criterion['evidencia_execucao'] = [by_ref[ref] for ref in criterion.get('testes', [])]
            supplemental_proofs = []
            for proof in criterion.get('evidencias_suplementares', []):
                artifact = project / proof['arquivo']
                script = project / proof['script']
                assert artifact.is_file() and script.is_file(), row['id'] + ': prova suplementar ausente'
                actual_proof = json.loads(artifact.read_text())
                approved = (actual_proof.get('sucesso') is True and actual_proof.get('assercoes')
                            and all(check.get('passou') is True for check in actual_proof['assercoes']))
                supplemental_proofs.append({**proof, 'sha256_arquivo': sha(artifact),
                    'sha256_script': sha(script), 'requisicoes': len(actual_proof.get('requisicoes', [])),
                    'assercoes': len(actual_proof.get('assercoes', [])),
                    'resultado': 'APROVADO' if approved else 'NAO_APROVADO'})
            criterion['evidencias_suplementares'] = supplemental_proofs
            criterion['resultado_execucao'] = ('APROVADO' if criterion['evidencia_execucao']
                and all(e['resultado'] == 'APROVADO' for e in criterion['evidencia_execucao'])
                and all(e['resultado'] == 'APROVADO' for e in supplemental_proofs)
                else 'SEM_PROVA_SUFICIENTE')
        if decision == 'A':
            assert evidence and all(e['resultado'] == 'APROVADO' for e in evidence), row['id'] + ': A sem provas aprovadas'
            if args.fase == 'final':
                assert row.get('matriz_criterios'), row['id'] + ': A sem mapeamento explícito dos critérios'
                assert all(c['resultado_execucao'] == 'APROVADO' for c in row['matriz_criterios']), row['id'] + ': critério A sem prova'
        source_evidence = []
        for c in row.get('componentes_responsaveis', []):
            path = c if isinstance(c, str) else c.get('caminho') or c.get('arquivo')
            if path:
                f = components_base / path
                assert f.is_file(), f'{row["id"]}: componente inexistente {path}'
                source_evidence.append({'arquivo': path, 'sha256': sha(f)})
        row['hashes_componentes'] = source_evidence
    meta = {'fase': args.fase, 'gerado_em': dt.datetime.now(dt.timezone.utc).isoformat(),
            'total_requisitos': 198, 'contagem': {k: counts[k] for k in LABELS},
            'classificacoes': LABELS, 'execucao': report_summary,
            'execucao_suplementar': supplemental_summary,
            'referencias_sem_execucao': missing, 'fonte': 'docs/LEVANTAMENTO_REQUISITOS.md',
            'diretorio_relatorios': str(args.relatorios),
            'manifesto_fontes_executadas': source_manifest,
            'sha256_fonte': sha(project / 'docs/LEVANTAMENTO_REQUISITOS.md'),
            'homologacao_independente': False}
    name = 'MATRIZ_IMPLEMENTACAO_198_REQUISITOS_' + args.fase.upper()
    (output / (name + '.json')).write_text(json.dumps({'metadados': meta, 'requisitos': rows}, ensure_ascii=False, indent=2))
    md = [f'# Matriz de implementação funcional — {args.fase.upper()}', '',
          'Avaliação de desenvolvimento confrontada com a norma original. Não constitui homologação formal independente.', '',
          '| Classificação | Quantidade |', '|---|---:|']
    md.extend(f'| {k} — {label} | {counts[k]} |' for k, label in LABELS.items())
    md += ['| Total | 198 |', '', 'Os XMLs e os hashes de cada componente estão ligados individualmente no JSON correspondente.', '',
           '| ID | Classificação | Enunciado original |', '|---|---|---|']
    md.extend(f'| {r["id"]} | {r["classificacao_principal"]} | {r["enunciado_original"].replace("|", " / ")} |' for r in rows)
    keys = [('regra_funcional_completa','Regra completa'),('criterios_aceitacao','Critérios de aceitação'),
            ('componentes_responsaveis','Componentes'),('entidades_relacionamentos','Entidades e relacionamentos'),
            ('estados','Estados'),('pre_condicoes','Pré-condições'),('pos_condicoes','Pós-condições'),
            ('invariantes','Invariantes'),('perfis_autorizados','Perfis'),('escopo_organizacional_temporal','Escopo'),
            ('endpoints','Endpoints'),('validacao_entrada','Validação'),('resposta_esperada','Resposta'),
            ('efeito_persistencia','Persistência'),('falhas','Falhas e rollback'),('historico','Histórico'),
            ('dependencias','Dependências'),('testes_existentes','Testes existentes'),
            ('testes_implementados','Testes adicionais implementados'),('lacunas','Lacunas no baseline'),
            ('matriz_criterios','Critérios e provas explícitas'),
            ('alteracoes_necessarias','Alterações necessárias'),('alteracoes_realizadas','Alterações realizadas'),
            ('testes_adicionais','Testes adicionais necessários'),('pendencias_remanescentes','Pendências remanescentes')]
    for r in rows:
        md += ['', f'## {r["id"]} — {LABELS[r["classificacao_principal"]]}', '', r['enunciado_original'], '',
               f'Antes: **{r["classificacao_antes"]}**. Depois: **{r.get("classificacao_depois") or "não apurado nesta matriz inicial"}**.', '']
        for key, label in keys:
            if key in r:
                if key == 'matriz_criterios':
                    md += ['**Critérios e provas explícitas:**', '',
                           '| Critério | Testes correspondentes | Execução |', '|---|---|---|']
                    for criterion in r[key]:
                        description = plain(criterion.get('criterio')).replace('|', ' / ')
                        tests = '; '.join(criterion.get('testes', [])).replace('|', ' / ')
                        if criterion.get('evidencias_suplementares'):
                            tests += '; prova suplementar: ' + '; '.join(
                                proof['arquivo'] for proof in criterion['evidencias_suplementares'])
                        md.append(f'| {description} | {tests} | {criterion.get("resultado_execucao", "Não apurado")} |')
                    md.append('')
                else:
                    md += [f'**{label}:** {plain(r[key])}', '']
        md += ['**Evidência de execução:** ' + '; '.join(f'{e["referencia"]}: {e["resultado"]} ({len(e["casos"])} casos)' for e in r['evidencia_execucao']), '']
    (output / (name + '.md')).write_text('\n'.join(md))
    print(json.dumps(meta, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
