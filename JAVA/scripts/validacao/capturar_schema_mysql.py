#!/usr/bin/env python3
"""Fotografa e compara schema/dados sintéticos com SELECTs; nunca executa DDL/DML.

Requer PyMySQL (1.1.2 usado na validação) ou cliente mysql. Configure MYSQL_HOST,
MYSQL_TCP_PORT, MYSQL_USER e MYSQL_PWD explicitamente. MYSQL_BIN pode selecionar
o executável. Somente schemas sintéticos locais enumerados em configuracao_mysql.py
e sgt_correcao_exec são aceitos. Execute sem escritores concorrentes.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from configuracao_mysql import ambiente_cliente, argumentos_cliente, conexao_mysql, schema_sintetico


FORMATO_VALORES = "HEX_CAST_BINARY_V2"

def valor_hex(expressao):
    """Representa o valor SQL sem arredondar números para o HEX inteiro.

    CAST sem comprimento preserva a representação completa produzida pelo MySQL
    e os bytes de colunas binárias. NULL continua distinto de vazio e de NUL.
    Não é uma leitura dos bytes físicos das páginas InnoDB.
    """
    return "IF(" + expressao + " IS NULL,NULL,HEX(CAST(" + expressao + " AS BINARY)))"


def literal(value):
    return "'" + value.replace("'", "''") + "'"


def identifier(value):
    return '`' + value.replace('`', '``') + '`'


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--database', required=True)
    p.add_argument('--saida', type=Path, required=True)
    p.add_argument('--comparar', type=Path)
    a = p.parse_args()
    if a.saida.exists():
        raise FileExistsError('Não sobrescrever fotografia anterior; escolha um novo arquivo de saída.')
    config = conexao_mysql()
    nome_isolado = schema_sintetico(a.database) or a.database == 'sgt_correcao_exec'
    if not nome_isolado:
        p.error('Somente base sintética explicitamente nomeada em MySQL local.')
    cmd = argumentos_cliente(config) + [a.database]

    try:
        import pymysql
        connection = pymysql.connect(**config, database=a.database, charset='utf8mb4', autocommit=True)
    except ImportError:
        connection = None

    def query(sql):
        # Nenhum SQL vem do usuário; nomes são extraídos do próprio catálogo e escapados.
        if connection is not None:
            with connection.cursor() as cursor:
                cursor.execute(sql)
                return [json.loads(row[0]) for row in cursor.fetchall()]
        run = subprocess.run(cmd+['--execute', sql], capture_output=True, text=True, env=ambiente_cliente())
        if run.returncode:
            raise RuntimeError('Cliente MySQL: '+run.stderr[:1000])
        out = run.stdout
        return [json.loads(line) for line in out.splitlines() if line.strip()]

    db = literal(a.database)
    schema = query("SELECT JSON_OBJECT('tabela',TABLE_NAME,'coluna',COLUMN_NAME,'ordem',ORDINAL_POSITION,"
                   "'tipo',COLUMN_TYPE,'nulo',IS_NULLABLE,'padrao',COLUMN_DEFAULT,'extra',EXTRA,"
                   "'charset',CHARACTER_SET_NAME,'collation',COLLATION_NAME,'expressao_gerada',GENERATION_EXPRESSION) "
                   f"FROM information_schema.COLUMNS WHERE TABLE_SCHEMA={db} ORDER BY TABLE_NAME,ORDINAL_POSITION")
    tables = query("SELECT JSON_OBJECT('tabela',TABLE_NAME,'engine',ENGINE,'collation',TABLE_COLLATION,'formato_linha',ROW_FORMAT) "
                   f"FROM information_schema.TABLES WHERE TABLE_SCHEMA={db} AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")
    indexes = query("SELECT JSON_OBJECT('tabela',TABLE_NAME,'indice',INDEX_NAME,'nao_unico',NON_UNIQUE,"
                    "'ordem',SEQ_IN_INDEX,'coluna',COLUMN_NAME,'prefixo',SUB_PART,"
                    "'tipo',INDEX_TYPE,'visivel',IS_VISIBLE,'expressao',EXPRESSION,'direcao',COLLATION) "
                    f"FROM information_schema.STATISTICS WHERE TABLE_SCHEMA={db} ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX")
    fks = query("SELECT JSON_OBJECT('tabela',k.TABLE_NAME,'fk',k.CONSTRAINT_NAME,'coluna',k.COLUMN_NAME,"
                "'referencia',k.REFERENCED_TABLE_NAME,'coluna_referencia',k.REFERENCED_COLUMN_NAME,'ordem',k.ORDINAL_POSITION,"
                "'atualizacao',r.UPDATE_RULE,'exclusao',r.DELETE_RULE,'correspondencia',r.MATCH_OPTION) "
                "FROM information_schema.KEY_COLUMN_USAGE k JOIN information_schema.REFERENTIAL_CONSTRAINTS r "
                "ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.TABLE_NAME=k.TABLE_NAME AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME "
                f"WHERE k.TABLE_SCHEMA={db} AND k.REFERENCED_TABLE_NAME IS NOT NULL "
                "ORDER BY k.TABLE_NAME,k.CONSTRAINT_NAME,k.ORDINAL_POSITION")
    checks = query("SELECT JSON_OBJECT('tabela',t.TABLE_NAME,'nome',t.CONSTRAINT_NAME,'clausula',c.CHECK_CLAUSE,'aplicada',t.ENFORCED) "
                   "FROM information_schema.TABLE_CONSTRAINTS t JOIN information_schema.CHECK_CONSTRAINTS c "
                   "ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME "
                   f"WHERE t.TABLE_SCHEMA={db} AND t.CONSTRAINT_TYPE='CHECK' ORDER BY t.TABLE_NAME,t.CONSTRAINT_NAME")
    config = query("SELECT JSON_OBJECT('mysql',VERSION(),'foreign_key_checks',@@foreign_key_checks,"
                   "'innodb_strict_mode',@@innodb_strict_mode,'sql_mode',@@sql_mode)")[0]
    rows = {}
    for t in tables:
        name = t['tabela']
        columns = [c['coluna'] for c in schema if c['tabela'] == name]
        expr = ','.join(literal(c)+','+valor_hex(identifier(c)) for c in columns)
        values = query('SELECT JSON_OBJECT('+expr+') FROM '+identifier(name))
        rows[name] = sorted(values, key=lambda x: json.dumps(x, sort_keys=True))
    orphan_counts = []
    groups = {}
    for fk in fks:
        groups.setdefault((fk['tabela'], fk['fk']), []).append(fk)
    for (table, name), parts in groups.items():
        target = parts[0]['referencia']
        join = ' AND '.join('a.'+identifier(f['coluna'])+'=b.'+identifier(f['coluna_referencia']) for f in parts)
        notnull = ' AND '.join('a.'+identifier(f['coluna'])+' IS NOT NULL' for f in parts)
        result = query("SELECT JSON_OBJECT('orfaos',COUNT(*)) FROM "+identifier(table)+' a LEFT JOIN '
                       +identifier(target)+' b ON '+join+' WHERE '+notnull+' AND b.'
                       +identifier(parts[0]['coluna_referencia'])+' IS NULL')[0]
        orphan_counts.append({'tabela': table, 'fk': name, **result})
    result = {'instante_utc':dt.datetime.now(dt.timezone.utc).isoformat(), 'database':a.database,
              'formato_valores':FORMATO_VALORES,
              'metodo':'Somente SELECTs; HEX(CAST(coluna AS BINARY)) representa valores SQL, preserva bytes de colunas binárias e diferencia NULL. Sem escritores durante a fotografia.',
              'configuracao':config, 'schema':schema, 'tabelas':tables, 'indices':indexes, 'fks':fks,
              'restricoes_check':checks, 'orfaos':orphan_counts, 'linhas':rows,
              'resumo_tabelas':{t:{'linhas':len(v),'sha256':digest(v)} for t,v in rows.items()}}
    if a.comparar:
        old = json.loads(a.comparar.read_text())
        if old.get('formato_valores') != FORMATO_VALORES:
            raise ValueError('Fotografia anterior usa representação ausente ou incompatível; recapture as duas fases.')
        old_schema = {(c['tabela'],c['coluna']):c for c in old['schema']}
        new_schema = {(c['tabela'],c['coluna']):c for c in schema}
        preserved, differences = [], []
        for table, before in old['linhas'].items():
            cols = [c['coluna'] for c in old['schema'] if c['tabela'] == table]
            after = [{c:r.get(c) for c in cols} for r in rows.get(table, [])]
            before_sorted = sorted(before, key=lambda x:json.dumps(x,sort_keys=True))
            after_sorted = sorted(after, key=lambda x:json.dumps(x,sort_keys=True))
            item = {'tabela':table,'antes':len(before),'depois':len(after),
                    'sha256_colunas_originais_antes':digest(before_sorted),
                    'sha256_colunas_originais_depois':digest(after_sorted),
                    'preservado_integralmente':before_sorted==after_sorted}
            (preserved if item['preservado_integralmente'] else differences).append(item)
        result['comparacao'] = {
            'fonte':a.comparar.name,'sha256_fonte':hashlib.sha256(a.comparar.read_bytes()).hexdigest(),
            'colunas_adicionadas':[new_schema[k] for k in sorted(new_schema.keys()-old_schema.keys())],
            'colunas_removidas':[old_schema[k] for k in sorted(old_schema.keys()-new_schema.keys())],
            'colunas_alteradas':[{'antes':old_schema[k],'depois':new_schema[k]} for k in sorted(old_schema.keys()&new_schema.keys())
                                if old_schema[k]!=new_schema[k]],
            'tabelas_preservadas':preserved,'tabelas_com_diferencas':differences}
    a.saida.parent.mkdir(parents=True, exist_ok=True)
    with a.saida.open('x', encoding='utf-8') as arquivo:
        arquivo.write(json.dumps(result,ensure_ascii=False,indent=2))
    if connection is not None:
        connection.close()
    print(json.dumps({'arquivo':str(a.saida),'tabelas':len(tables),'linhas':sum(len(v) for v in rows.values()),
                      'fks':len(groups),'restricoes_check':len(checks),'orfaos':sum(x['orfaos'] for x in orphan_counts),
                      'tabelas_com_diferencas':len(result.get('comparacao',{}).get('tabelas_com_diferencas',[]))},ensure_ascii=False))


if __name__ == '__main__':
    main()
