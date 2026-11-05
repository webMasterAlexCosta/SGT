"""Enrich an identified synthetic contract via canonical proof API, without SQL."""
from pathlib import Path
import datetime
import json
import os
import sys
import uuid


def run():
    runtime = Path(os.environ['SGT_TEST_RUNTIME']).resolve()
    sys.path.insert(0, str(runtime))
    from api_client import Client, EVENTS

    fixture = json.loads((runtime / 'private/e2e-fixture.json').read_text())
    business = json.loads((runtime / 'private/e2e-business.json').read_text())
    contract_id = fixture.get('ids', {}).get('contrato_rascunho')
    observed = fixture.get('objects', {}).get('contrato_rascunho', {})
    if (fixture.get('synthetic') is not True or type(contract_id) is not int or contract_id <= 0
            or observed.get('id') != contract_id):
        raise ValueError('A fixture deve identificar o contrato sintético observado.')
    contract_path = f'/api/contratos/{contract_id}'
    proof_path = contract_path + '/provas-historicas'
    if business.get('contratoEndpoint') != proof_path:
        raise ValueError('O recibo de incorporação deve corresponder ao mesmo contrato da fixture.')
    payload = {key: value for key, value in business['contratoReplay'].items()
               if key not in ['tokenPrevia', 'confirmarImpacto']}

    client = Client(fixture['accounts']['gestor_local'])
    client.login()
    versions = client.request('GET', contract_path + '/versoes', expected=200)
    before = len(versions)
    if before != 5:
        raise RuntimeError('Expected precisely five versions before preparing sixth boundary fixture')
    proofs = client.request('GET', proof_path, expected=200)
    prior = max((proof for proof in proofs if proof['eventoOrigemId'] == payload['eventoOrigemId']),
                key=lambda proof: proof['numeroRevisao'])
    current = client.request('GET', contract_path, expected=200)
    payload.update(
        provaAnteriorId=prior['id'], versaoAnteriorId=prior['versaoGeradaId'],
        versaoContratoEsperada=current['version'], chaveIdempotencia=str(uuid.uuid4()),
        referenciaSei='SINTETICO CORRECAO FRONTEIRA SEXTO REGISTRO SEM VALOR DOCUMENTAL',
        motivo='Revisão sintética para verificar a fronteira do sexto registro no navegador',
        autorizacaoAdministrativa='ATO SINTETICO ISOLADO SEM VALOR DOCUMENTAL REAL')
    preview = client.request('POST', proof_path + '/previa', payload, expected=200)
    receipt = client.request('POST', proof_path,
                             {**payload, 'tokenPrevia': preview['tokenPrevia'], 'confirmarImpacto': True}, expected=201)
    after = client.request('GET', contract_path + '/versoes', expected=200)
    assert len(after) == 6
    report = {
        'synthetic': True, 'contract_id': contract_id,
        'before_versions': before, 'after_versions': len(after),
        'new_proof_id': receipt['id'], 'new_version_id': receipt['versaoGeradaId'],
        'assertions_modified': False, 'application_source_modified': False, 'application_sql_used': False,
        'purpose': 'Complementar a pré-condição explícita de fronteira 5+1 no contrato observado da fixture.',
        'http': [{'method': event['method'], 'path': event['path'], 'status': event['status']} for event in EVENTS]}
    report_path = runtime.parent / 'evidence/infra/pos-auditoria-sixth-version-fixture.json'
    # Each prior checkpoint survives a new run, including session state superseded by this API login.
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ') + '-' + uuid.uuid4().hex
    if report_path.exists():
        report_path.rename(report_path.with_name(f'{report_path.stem}-before-{stamp}.json'))
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'contract_id': contract_id, 'before': before, 'after': len(after), 'status': 201}))
    state = runtime / 'private/e2e-gestor_local-browser-state.json'
    if state.exists():
        state.rename(state.with_name(f'e2e-gestor_local-browser-state-before-sixth-{stamp}.json'))


if __name__ == '__main__':
    run()
