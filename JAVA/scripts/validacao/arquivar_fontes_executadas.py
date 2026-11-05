#!/usr/bin/env python3
"""Arquiva exatamente os fontes identificados por um manifesto de execução.

Recusa diferenças de conteúdo, adições e remoções de src/pom.xml. Não decide
aprovação de testes nem altera o projeto compilado. O arquivo de saída precisa
ser novo; as provas anteriores nunca são sobrescritas.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--projeto', type=Path, required=True)
    parser.add_argument('--manifesto', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    project = args.projeto.resolve()
    expected = json.loads(args.manifesto.read_text())['fontes']
    paths = [project / 'pom.xml'] + [p for p in (project / 'src').rglob('*') if p.is_file()]
    observed = {str(p.relative_to(project)): sha(p) for p in sorted(paths)}
    differences = sorted(k for k in expected.keys() | observed.keys()
                         if expected.get(k) != observed.get(k))
    if differences:
        raise SystemExit('Fontes divergentes; arquivo não produzido: ' + ', '.join(differences))
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.saida, 'x', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for name in sorted(expected):
            archive.write(project / name, name)
    with zipfile.ZipFile(args.saida) as archive:
        assert archive.testzip() is None, 'CRC inválido'
        assert set(archive.namelist()) == set(expected)
        for name, digest in expected.items():
            assert hashlib.sha256(archive.read(name)).hexdigest() == digest, name
    print(json.dumps({'arquivado': True, 'arquivos': len(expected),
                      'bytes': args.saida.stat().st_size, 'sha256': sha(args.saida),
                      'sha256_manifesto': sha(args.manifesto)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
