#!/usr/bin/env python3
"""Controle permanente da representação dos valores, com SELECTs em MySQL real.

Não cria tabelas, não altera dados e não conta como teste de requisito funcional.
Use as mesmas MYSQL_HOST/USER/TCP_PORT/PWD/BIN do capturador. A saída não inclui
credenciais. O servidor deve ser local; todos os valores SQL são constantes.
"""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
from configuracao_mysql import ambiente_cliente, argumentos_cliente, conexao_mysql


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def utf8(value):
    return value.encode('utf-8').hex().upper()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capturador', type=Path, default=Path(__file__).with_name('capturar_schema_mysql.py'))
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('capturador_em_controle', args.capturador)
    capture = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(capture)
    assert capture.FORMATO_VALORES == 'HEX_CAST_BINARY_V2'
    command = argumentos_cliente(conexao_mysql())
    cases = [
        ('decimal_101', "CAST('1.01' AS DECIMAL(10,2))", utf8('1.01')),
        ('decimal_102', "CAST('1.02' AS DECIMAL(10,2))", utf8('1.02')),
        ('decimal_negativo', "CAST('-0.01' AS DECIMAL(10,2))", utf8('-0.01')),
        ('decimal_grande', "CAST('99999999999999999999.99' AS DECIMAL(22,2))", utf8('99999999999999999999.99')),
        ('decimal_pequeno', "CAST('0.000000000000000001' AS DECIMAL(20,18))", utf8('0.000000000000000001')),
        ('double_101', "CAST('1.01' AS DOUBLE)", utf8('1.01')),
        ('double_102', "CAST('1.02' AS DOUBLE)", utf8('1.02')),
        ('double_um', "CAST('1' AS DOUBLE)", utf8('1')),
        ('double_proximo', "CAST('1.0000000000000002' AS DOUBLE)", utf8('1.0000000000000002')),
        ('double_negativo', "CAST('-1.25' AS DOUBLE)", utf8('-1.25')),
        ('inteiro_minimo', "CAST('-9223372036854775808' AS SIGNED)", utf8('-9223372036854775808')),
        ('inteiro_maximo', "CAST('18446744073709551615' AS UNSIGNED)", utf8('18446744073709551615')),
        ('bit_zero', "b'0'", '00'),
        ('bit_um', "b'1'", '01'),
        ('bit_oito', "b'11111111'", 'FF'),
        ('binario_nul_ff_espaco', "UNHEX('0000FF20')", '0000FF20'),
        ('binario_nul', "UNHEX('00')", '00'),
        ('texto_utf8', "_utf8mb4'á界😀 '", utf8('á界😀 ')),
        ('texto_espacos_finais', "'a  '", utf8('a  ')),
        ('vazio', "''", ''),
        ('nulo', "NULL", None),
        ('datetime_micros_1', "CAST('2026-10-31 12:34:56.000001' AS DATETIME(6))", utf8('2026-10-31 12:34:56.000001')),
        ('datetime_micros_2', "CAST('2026-10-31 12:34:56.000002' AS DATETIME(6))", utf8('2026-10-31 12:34:56.000002')),
        ('data', "CAST('2026-10-31' AS DATE)", utf8('2026-10-31')),
        ('hora_micros', "CAST('12:34:56.000001' AS TIME(6))", utf8('12:34:56.000001')),
    ]
    selections = []
    for name, expression, expected in cases:
        selections.append("SELECT JSON_OBJECT('caso'," + capture.literal(name) +
                          ",'observado'," + capture.valor_hex('v') +
                          ") FROM (SELECT " + expression + " AS v) AS controle")
    sql = ' UNION ALL '.join(selections) + ';'
    sql += "SELECT JSON_OBJECT('mysql',VERSION(),'sql_mode',@@sql_mode,'charset',@@character_set_connection);"
    sql += "SELECT JSON_OBJECT('hex_original_101',HEX(CAST('1.01' AS DECIMAL(10,2)))," \
           "'hex_original_102',HEX(CAST('1.02' AS DECIMAL(10,2))));"
    start = dt.datetime.now(dt.timezone.utc).isoformat()
    executed = subprocess.run(command, input=sql, text=True, capture_output=True, env=ambiente_cliente())
    if executed.returncode:
        raise RuntimeError('Falha no cliente MySQL: '+executed.stderr[:1000])
    lines = [json.loads(line) for line in executed.stdout.splitlines() if line.strip()]
    assert len(lines) == len(cases)+2, 'Resposta SQL incompleta.'
    results = []
    for (name, expression, expected), actual in zip(cases, lines):
        results.append({'caso': name, 'expressao_sql': expression, 'esperado': expected,
                        'observado': actual.get('observado'),
                        'aprovado': actual.get('caso') == name and actual.get('observado') == expected})
    values = {item['caso']: item['observado'] for item in results}
    pairs = [('decimal_101', 'decimal_102'), ('double_101', 'double_102'),
             ('double_um', 'double_proximo'), ('datetime_micros_1', 'datetime_micros_2'),
             ('nulo', 'vazio'), ('vazio', 'binario_nul'), ('nulo', 'binario_nul')]
    distinct = [{'casos': [a, b], 'distintos': values[a] != values[b]} for a, b in pairs]
    old = lines[-1]
    collision = old['hex_original_101'] == old['hex_original_102']
    report = {'inicio_utc': start, 'fim_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'tipo': 'Controle SELECT-only do capturador; não é caso de requisito funcional Java.',
              'formato_valores': capture.FORMATO_VALORES, 'capturador_sha256': sha(args.capturador),
              'controle_sha256': sha(Path(__file__)), 'sql_sha256': hashlib.sha256(sql.encode()).hexdigest(),
              'servidor': lines[-2], 'casos': results, 'distincoes_obrigatorias': distinct,
              'controle_negativo_hex_original': old | {'colisao_reproduzida': collision},
              'aprovado': all(r['aprovado'] for r in results) and all(p['distintos'] for p in distinct) and collision}
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    assert not args.saida.exists(), 'Não sobrescrever controle anterior.'
    args.saida.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'aprovado': report['aprovado'], 'casos': len(results), 'distincoes': len(distinct),
                      'controle_negativo_reproduzido': collision, 'saida': str(args.saida)}, ensure_ascii=False))
    if not report['aprovado']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
