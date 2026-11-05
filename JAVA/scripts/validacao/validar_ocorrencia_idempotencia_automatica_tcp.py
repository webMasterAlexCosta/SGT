#!/usr/bin/env python3
"""Prova suplementar AUD-005 no JAR real, com reenvio sem chave explícita.

Reutiliza a fixture/estado de validar_jar_tcp.py. Requer PyMySQL e uma base local
sintética. Toda escrita ocorre pela API; as releituras SQL usam novas conexões e
somente SELECT. Não altera schema, dados de produção ou código Java.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import traceback
import uuid

import pymysql
from validar_jar_tcp import Cliente, Prova
from configuracao_mysql import conexao_mysql, schema_sintetico


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', required=True)
    parser.add_argument('--fixture', type=Path, required=True)
    parser.add_argument('--estado', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    parser.add_argument('--database', required=True)
    args = parser.parse_args()
    config = conexao_mysql()
    if not schema_sintetico(args.database):
        parser.error('Somente os schemas sintéticos locais enumerados em configuracao_mysql.py.')
    fixture = json.loads(args.fixture.read_text())
    state = json.loads(args.estado.read_text())
    proof = Prova(args.saida, 'idempotencia-automatica', 'validar_ocorrencia_idempotencia_automatica_tcp.py')

    def read(sql, params):
        with pymysql.connect(**config, database=args.database, charset='utf8mb4', autocommit=True,
                cursorclass=pymysql.cursors.DictCursor) as connection:
            with connection.cursor() as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()

    def snapshot(employee):
        queries = {
            'ocorrencias': 'SELECT * FROM ocorrencia WHERE id_empregado=%s ORDER BY id',
            'contextos': 'SELECT c.* FROM contexto_ocorrencia c JOIN ocorrencia o ON o.id=c.ocorrencia_id WHERE o.id_empregado=%s ORDER BY c.id',
            'anexos': 'SELECT a.* FROM anexo_ocorrencia a JOIN ocorrencia o ON o.id=a.id_ocorrencia WHERE o.id_empregado=%s ORDER BY a.id',
            'historicos': 'SELECT * FROM historico_empregado WHERE id_empregado=%s ORDER BY id_historico',
            'frequencias': 'SELECT * FROM frequencia WHERE id_empregado=%s ORDER BY id',
            'eventos_frequencia': 'SELECT e.* FROM evento_frequencia e JOIN frequencia f ON f.id=e.id_frequencia WHERE f.id_empregado=%s ORDER BY e.id_evento_frequencia',
        }
        return {name: read(sql, (employee,)) for name, sql in queries.items()}

    def persist_snapshot(name, values):
        raw = json.dumps(values, ensure_ascii=False, indent=2, default=str).encode()
        (args.saida / name).write_bytes(raw)
        return hashlib.sha256(raw).hexdigest()

    try:
        employee_rows = read('SELECT id_empregado FROM empregado WHERE id_pessoa_canonica=%s', (state['pessoa_id'],))
        proof.check('Fixture possui uma projeção operacional canônica', len(employee_rows) == 1)
        employee = employee_rows[0]['id_empregado']
        local = Cliente(args.base, 'local', proof, fixture['senha'])
        other = Cliente(args.base, 'outro', proof, fixture['senha'])
        local.login(fixture['usuarios']['local'])
        other.login(fixture['usuarios']['outro'])
        day = dt.date.fromisoformat(state['data']) + dt.timedelta(days=1)
        fields = {'empregadoId': str(employee), 'motivo': 'ATESTADO', 'dataOcorrencia': str(day),
                  'dias': '1', 'observacao': 'Prova automática TCP ' + uuid.uuid4().hex,
                  'classificacoesAnexos': 'NEGOCIAL'}
        file_bytes = b'Prova sintetica de repeticao sem chave explicita.'

        def post(client, expected):
            boundary = 'SGTAutomatico' + uuid.uuid4().hex
            chunks = []
            for name, value in fields.items():
                chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
            chunks += [f'--{boundary}\r\nContent-Disposition: form-data; name="anexos"; filename="prova-auto.txt"\r\nContent-Type: text/plain\r\n\r\n'.encode(),
                       file_bytes, f'\r\n--{boundary}--\r\n'.encode()]
            return client.call('POST', '/api/ocorrencias', expected,
                raw=b''.join(chunks), content_type='multipart/form-data; boundary=' + boundary,
                cenario='Ocorrência sem chave explícita; ' + client.perfil)[0]

        before = snapshot(employee)
        first = post(local, 201)
        committed = snapshot(employee)
        second = post(local, 201)
        repeated = snapshot(employee)
        proof.check('Reenvio automático mantém o mesmo ID e payload', first == second,
                    first.get('id'), second.get('id'))
        proof.check('Novo commit criou exatamente uma ocorrência', len(committed['ocorrencias']) == len(before['ocorrencias']) + 1)
        occurrence = next(o for o in committed['ocorrencias'] if o['id'] == first['id'])
        proof.check('Chave persistida usa digest da intenção', occurrence['chave_idempotencia'] == 'auto:' + occurrence['hash_intencao'])
        proof.check('Reenvio não altera fato, contextos, anexos, história ou frequência', committed == repeated)
        proof.check('Anexo foi persistido uma única vez', len(committed['anexos']) == len(before['anexos']) + 1)
        proof.check('Fato possui histórico vinculado', any(h.get('id_ocorrencia') == first['id'] for h in committed['historicos']))
        new_file = next(a for a in committed['anexos'] if a['id_ocorrencia'] == first['id'])
        proof.check('Hash do arquivo persistido corresponde ao conteúdo', new_file['sha256'] == hashlib.sha256(file_bytes).hexdigest())
        post(other, 403)
        denied = snapshot(employee)
        proof.check('Tentativa fora do escopo não deixa gravação parcial', denied == repeated)
        _, download, _ = local.call('GET', f'/api/ocorrencias/anexos/{new_file["id"]}')
        proof.check('Download do único anexo preserva todos os bytes', download == file_bytes)
        digests = {name: persist_snapshot(name + '.json', value) for name, value in
                   [('antes', before), ('primeiro-commit', committed), ('reenvio', repeated), ('recusa', denied)]}
        (args.saida / 'contexto.json').write_text(json.dumps({'database': args.database,
            'empregado_id': employee, 'ocorrencia_id': first['id'], 'chave_explicita_enviada': False,
            'metodo_sql': 'SELECT em nova conexão após cada resposta HTTP confirmada',
            'snapshots_sha256': digests, 'sha256_script': hashlib.sha256(Path(__file__).read_bytes()).hexdigest()},
            ensure_ascii=False, indent=2) + '\n')
        local.call('POST', '/api/autenticacao/encerrar-sessao')
        other.call('POST', '/api/autenticacao/encerrar-sessao')
        proof.concluir()
        print(json.dumps({'sucesso': True, 'requisicoes': len(proof.requests), 'assercoes': len(proof.assertions)}))
    except Exception as error:
        proof.salvar(f'{type(error).__name__}: {error}')
        traceback.print_exc()
        raise


if __name__ == '__main__':
    main()
