#!/usr/bin/env python3
"""Aplica decisões de revisão à avaliação inicial, preservando as fontes originais.

Não usa contagem de testes para inferir implementação. Cada alteração vem de uma
errata humana com ID, motivo e evidência; o gerador da matriz continua separado.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def main():
    project = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--originais', type=Path, default=project / 'docs/implementacao-198/fontes-avaliacao-originais')
    parser.add_argument('--erratas', type=Path, default=project / 'docs/implementacao-198/erratas-baseline')
    parser.add_argument('--saida', type=Path, default=project / 'docs/implementacao-198/fontes-avaliacao')
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    changes = collections.defaultdict(list)
    for path in sorted(args.erratas.glob('errata*json')):
        raw = json.loads(path.read_text())
        entries = raw if isinstance(raw, list) else raw.get('erratas', [])
        for entry in entries:
            changes[entry['id']].append(dict(entry, arquivo_errata=path.name,
                sha256_errata=hashlib.sha256(path.read_bytes()).hexdigest()))
    counts = collections.Counter()
    seen = set()
    for path in sorted(args.originais.glob('avaliacao_*_baseline.json')):
        rows = json.loads(path.read_text())
        for row in rows:
            assert row['id'] not in seen, 'ID duplicado: ' + row['id']
            seen.add(row['id'])
            original = row['classificacao_antes']
            for entry in changes[row['id']]:
                decision = (entry.get('classificacao_baseline_revisada') or entry.get('classificacao_baseline_corrigida')
                            or entry.get('classificacao_corrigida'))
                assert decision in {'A', 'B', 'C', 'D', 'E'}, entry
                row['classificacao_antes'] = decision
                row.setdefault('erratas_baseline', []).append(entry)
                reason = entry.get('motivo') or entry.get('causa_funcional') or entry.get('causa')
                if reason and decision != 'A':
                    row.setdefault('lacunas', []).append(reason)
            if row['id'] == 'ALO-006' and row['classificacao_antes'] == 'A':
                row['lacunas_registradas_inicialmente'] = row['lacunas']
                row['lacunas'] = []
                row['testes_existentes'] += [
                    'FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte',
                    'FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal']
            row['classificacao_antes_registrada_inicialmente'] = original
            row['classificacao_depois'] = None
            counts[row['classificacao_antes']] += 1
        (args.saida / path.name).write_text(json.dumps(rows, ensure_ascii=False, indent=2))
    assert len(seen) == 198, 'Esperados exatamente 198 requisitos'
    normative = args.originais / 'requisitos_canonicos.json'
    (args.saida / normative.name).write_bytes(normative.read_bytes())
    print(json.dumps({'total': len(seen), 'classificacao_antes_revisada': dict(counts)}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
