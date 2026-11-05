#!/usr/bin/env python3
"""Replay real da retificação canônica sintética; não cria outra intenção válida."""
import json
import os
from pathlib import Path
import sys
from urllib.parse import urlencode

runtime = Path(os.environ['SGT_TEST_RUNTIME']).resolve()
root = runtime.parent
sys.path.insert(0, str(runtime))
from api_client import Client, EVENTS

fixture = json.loads((runtime / 'private/e2e-fixture.json').read_text())
assert fixture['synthetic'] is True
original = json.loads((root / 'evidence/browser/lancamento-vinculo-real-gl.json').read_text())
assert original['passed'] is True and original['post']['status'] == 200
payload = original['post']['payload']
expected = original['persistenciaDepois']
client = Client(fixture['accounts']['gestor_local'])
report = {'synthetic': True, 'jar_modified': False, 'application_sql_used': False, 'checks': [], 'passed': False}

def check(name, ok):
    assert ok, name
    report['checks'].append({'name': name, 'passed': True})

def read_current():
    result = client.request('GET', '/api/frequencias?' + urlencode({'data': payload['dataReferencia'], 'lotacaoId': fixture['ids']['lotacao'], 'jornadaId': fixture['ids']['jornada'], 'page': 0, 'size': 1000}), expected=200)
    rows = result if isinstance(result, list) else result['content']
    matches = [row for row in rows if row.get('vinculoContratualId') == payload['vinculoContratualId'] and row.get('dataReferencia') == payload['dataReferencia']]
    check('Leitura por Vínculo/data encontra exatamente um registro', len(matches) == 1)
    return matches[0]

try:
    client.login()
    before = read_current()
    replay = client.request('POST', '/api/frequencias', payload, expected=200)
    check('Replay da mesma intenção retorna a frequência e a versão previamente gravadas', replay['id'] == expected['id'] and replay['versaoNegocial'] == expected['versaoNegocial'] and replay['observacao'] == expected['observacao'])
    rejected = client.request('POST', '/api/frequencias', {**payload, 'observacao': 'INTENCAO SINTETICA DIFERENTE COM A MESMA CHAVE: DEVE SER RECUSADA'}, expected=409)
    check('Mesma chave com conteúdo diferente é rejeitada pelo Java', EVENTS[-1]['status'] == 409)
    after = read_current()
    fields = ['id', 'pessoaId', 'vinculoContratualId', 'alocacaoVinculoId', 'contratoId', 'dataReferencia', 'status', 'observacao', 'versaoNegocial', 'estadoAuditoria', 'bloqueadaEdicaoOperacional']
    check('Replay e conflito não alteram a identidade, versão ou conteúdo persistidos', all(before.get(k) == after.get(k) for k in fields))
    report.update({'before': {k: before.get(k) for k in fields}, 'after': {k: after.get(k) for k in fields}, 'conflict': rejected, 'passed': True})
except Exception as exc:
    report['failure'] = str(exc)
    raise
finally:
    report['http'] = EVENTS
    label = os.environ.get('SGT_HTTP_RECEIPT_NAME', 'pos-auditoria-frequencia-idempotencia-http')
    if not label.replace('-', '').replace('_', '').isalnum(): raise RuntimeError('Nome de recibo inválido')
    target = root / 'evidence/infra' / (label + '.json')
    if target.exists():
        raise RuntimeError('Recibo já existe; não sobrescrever execução anterior')
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    state = runtime / 'private/e2e-gestor_local-browser-state.json'
    if state.exists(): state.rename(state.with_name('e2e-gestor_local-before-idempotencia-correcao.json'))
    print(json.dumps({'passed': report['passed'], 'checks': len(report['checks']), 'statuses': [event['status'] for event in EVENTS]}))
