#!/usr/bin/env python3
"""Consolida XMLs reais, cobertura e hashes de uma execução isolada.

Não executa testes, não decide requisitos e não transforma um log incompleto em
aprovação. Use um target novo por execução, conservando também o log integral.
"""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--projeto', type=Path, required=True)
    p.add_argument('--log', type=Path, required=True)
    p.add_argument('--manifesto-fontes', type=Path, required=True)
    p.add_argument('--saida', type=Path, required=True)
    a = p.parse_args()
    result = {'instante_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'log': a.log.name, 'sha256_log': sha(a.log), 'relatorios': [], 'jacoco': {}}
    for group in ('surefire-reports', 'failsafe-reports'):
        count = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
        for file in sorted((a.projeto / 'target' / group).glob('TEST-*.xml')):
            root = ET.parse(file).getroot()
            for key in count:
                count[key] += int(root.get(key, '0'))
            result['relatorios'].append({'arquivo': str(file.relative_to(a.projeto)),
                                          'sha256': sha(file), 'classe': root.get('name')})
        result[group] = count
    coverage = a.projeto / 'target/site/jacoco/jacoco.xml'
    if coverage.exists():
        result['jacoco'] = {c.get('type'): {k: int(c.get(k)) for k in ('covered', 'missed')}
                            for c in ET.parse(coverage).getroot().findall('counter')}
        result['sha256_jacoco_xml'] = sha(coverage)
    original = json.loads(a.manifesto_fontes.read_text())['fontes']
    current_paths = sorted([a.projeto / 'pom.xml'] + [f for f in (a.projeto / 'src').rglob('*') if f.is_file()])
    current = {str(f.relative_to(a.projeto)): sha(f) for f in current_paths}
    result['fontes_inalteradas_na_execucao'] = original == current
    result['fontes_divergentes'] = sorted(k for k in original.keys() | current.keys()
                                         if original.get(k) != current.get(k))
    log = a.log.read_text(errors='replace')
    result['conclusao'] = ('BUILD FAILURE' if 'BUILD FAILURE' in log else
                           'BUILD SUCCESS' if 'BUILD SUCCESS' in log else 'EXECUCAO_INCOMPLETA')
    result['jars'] = [{'arquivo': f.name, 'bytes': f.stat().st_size, 'sha256': sha(f)}
                      for f in sorted((a.projeto / 'target').glob('*.jar'))]
    a.saida.parent.mkdir(parents=True, exist_ok=True)
    a.saida.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ('surefire-reports', 'failsafe-reports', 'conclusao',
                                          'fontes_inalteradas_na_execucao')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
