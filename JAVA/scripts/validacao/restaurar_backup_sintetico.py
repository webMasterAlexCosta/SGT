#!/usr/bin/env python3
"""Restaura INSERTs de um backup verificado em schema sintético vazio criado por Hibernate.

Não cria banco, tabela, coluna ou constraint. Requer PyMySQL e servidor local.
Pare a aplicação antes de executar. O SHA-256 é obrigatório; apenas INSERTs e
SETs de sessão do mysqldump são aceitos. Todas as tabelas devem estar vazias;
nenhum dado existente é apagado ou substituído pelo restaurador.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re

import pymysql
from configuracao_mysql import conexao_mysql, schema_sintetico


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', required=True)
    parser.add_argument('--backup', type=Path, required=True)
    parser.add_argument('--sha256', required=True)
    args = parser.parse_args()
    config = conexao_mysql()
    if not schema_sintetico(args.database):
        parser.error('Restauração restrita aos schemas sintéticos locais enumerados em configuracao_mysql.py.')
    contents = args.backup.read_bytes()
    actual = hashlib.sha256(contents).hexdigest()
    if actual != args.sha256:
        parser.error('SHA-256 do backup divergente; nenhuma conexão foi aberta.')
    statements = []
    for number, raw in enumerate(contents.decode('utf-8').splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith('--'):
            continue
        if not (re.fullmatch(r'/\*!\d+ SET .+ \*/;', line)
                or re.match(r'^INSERT INTO `[^`]+` \(.+\) VALUES .+;$', line)):
            parser.error(f'Comando não permitido ou formato multilinha na linha {number}. Nenhum SQL executado.')
        statements.append(line)
    connection = pymysql.connect(**config, database=args.database, charset='utf8mb4', autocommit=False)
    inserted = 0
    try:
        with connection.cursor() as cursor:
            cursor.execute('SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES '
                           'WHERE TABLE_SCHEMA=%s AND TABLE_TYPE=\'BASE TABLE\'', (args.database,))
            tables = cursor.fetchall()
            if not tables:
                raise RuntimeError('Schema inexistente/vazio: primeiro inicie o JAR original com Hibernate update.')
            occupied = {}
            for table, engine in tables:
                if engine != 'InnoDB':
                    raise RuntimeError('Restauração atômica exige todas as tabelas em InnoDB.')
                cursor.execute('SELECT COUNT(*) FROM `' + table.replace('`', '``') + '`')
                count = cursor.fetchone()[0]
                if count:
                    occupied[table] = count
            if occupied:
                raise RuntimeError('Base de destino não está vazia; nenhum dado será apagado ou substituído. '
                                   'Use outro schema sintético preservando o anterior.')
            for statement in statements:
                cursor.execute(statement)
                if statement.startswith('INSERT '):
                    inserted += cursor.rowcount
        connection.commit()
    except BaseException:
        connection.rollback()
        raise
    finally:
        connection.close()
    print(json.dumps({'database': args.database, 'backup_sha256': actual, 'linhas_inseridas': inserted,
                      'linhas_semente_substituidas': 0,
                      'ddl_executado': False, 'metodo': 'INSERTs em transação única, schema pré-criado pelo Hibernate'},
                     ensure_ascii=False))


if __name__ == '__main__':
    main()
