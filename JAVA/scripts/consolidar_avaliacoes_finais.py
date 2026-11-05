#!/usr/bin/env python3
"""Prepara entradas finais rastreáveis a partir de decisões humanas explícitas.

As avaliações de trabalho e seus hashes permanecem preservados. Este script não
promove candidatas automaticamente, não decide suficiência funcional e não usa
cobertura para classificar requisitos. Remove metadados transitórios das notas de
trabalho; o gerador da matriz vincula a execução definitiva e seus XMLs.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import re
import shutil

from atualizar_rastreabilidade_198 import collect_reports, matching, references


FIELDS = ('id enunciado_original regra_funcional_completa fonte_normativa criterios_aceitacao '
          'criterios_aceitacao_adicionais entidades_relacionamentos estados pre_condicoes pos_condicoes '
          'invariantes perfis_autorizados escopo_organizacional_temporal endpoints validacao_entrada '
          'resposta_esperada efeito_persistencia falhas historico dependencias alteracoes_realizadas '
          'pendencias_remanescentes limitacoes_reais limitacoes_legado limite_documental_sem_inferencia '
          'limites_dados_legados pendencias_documentais_condicionais limites_legado limite_da_avaliacao '
          'causa_verificavel_E justificativa_classificacao_antes').split()
CRITERIA_FIELDS = ('criterio testes demonstracao assertivas_observadas persistencia_rollback '
                   'persistencia_ou_rollback limite_da_prova limite_dado_legado origem_criterio '
                   'limite extensao_criterio origem_adendo fundamento').split()


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def portable(value, project, work_notes):
    if isinstance(value, list):
        return [portable(v, project, work_notes) for v in value]
    if isinstance(value, dict):
        return {k: portable(v, project, work_notes) for k, v in value.items()}
    if isinstance(value, str):
        # As notas conservam caminhos da sessão que produziu a prova. Ao
        # regenerar em outra máquina, resolve-se o conteúdo distribuído pelo
        # marcador conhecido, sem exigir que aquele diretório absoluto exista.
        if value.startswith('/'):
            for marker, replacement in (
                    ('/baseline_original/' + project.name + '/',
                     'evidencias-implementacao/baseline-preservado/fontes-originais/'),
                    ('/work/' + project.name + '/', ''),
                    ('/evidence/', 'evidencias-implementacao/')):
                if marker in value:
                    value = replacement + value.split(marker, 1)[1]
                    break
        original = project.parent.parent / 'baseline_original' / project.name
        return value.replace(str(original) + '/',
            'evidencias-implementacao/baseline-preservado/fontes-originais/').replace(str(project) + '/', '').replace(str(work_notes) + '/',
            'evidencias-implementacao/').replace('evidence/', 'evidencias-implementacao/')
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--notas', type=Path, required=True)
    parser.add_argument('--decisoes', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--xml-baseline', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    baseline_document = json.loads(args.baseline.read_text())
    baseline = {r['id']: r for r in baseline_document['requisitos']}
    decisions_document = json.loads(args.decisoes.read_text())
    assert decisions_document['aprovacao_tecnica_consolidada'] is True
    decisions = {r['id']: r for r in decisions_document['decisoes']}
    assert set(decisions) == set(baseline) and len(decisions_document['decisoes']) == 198
    baseline_cases, _ = collect_reports(args.xml_baseline)
    assert baseline_cases, 'XMLs do baseline são obrigatórios para distinguir os testes acrescentados'
    sources = {}
    for path in sorted((project / 'src/test/java').rglob('*.java')):
        text = path.read_text()
        package = re.search(r'^package\s+([^;]+);', text, re.M)
        if package:
            sources[package.group(1) + '.' + path.stem] = path

    def test_source(ref):
        cls = ref.split('#', 1)[0].removesuffix('.java')
        found = [p for name, p in sources.items() if name == cls or name.endswith('.' + cls)]
        assert len(found) == 1, 'Referência de fonte ambígua/inexistente: ' + ref
        path = found[0]
        return {'teste': ref, 'arquivo': str(path.relative_to(project)), 'sha256_fonte': sha(path)}

    def component(value):
        if isinstance(value, str):
            if '#' in value and not value.startswith(('/', 'src/')):
                class_name, member = value.split('#', 1)
                matches = list((project / 'src/main/java').rglob(class_name.rsplit('.', 1)[-1] + '.java'))
                assert len(matches) == 1, 'Componente ambíguo/inexistente: ' + value
                value = {'caminho': str(matches[0].relative_to(project)), 'membro': member}
            else:
                value = {'caminho': value}
        path_value = value.get('caminho') or value.get('arquivo')
        assert path_value, 'Componente sem caminho'
        path = Path(path_value)
        path = path if path.is_absolute() else project / path
        assert path.is_file(), str(path)
        return {'caminho': str(path.relative_to(project)),
                **{k: copy.deepcopy(value[k]) for k in ('metodo', 'metodos', 'membro', 'regra') if k in value},
                'sha256_fonte': sha(path)}

    args.saida.mkdir(parents=True, exist_ok=True)
    seen = set()
    provenance = []
    for notes in sorted(args.notas.glob('avaliacao_*_final.json')):
        output = []
        for raw in json.loads(notes.read_text()):
            ident = raw['id']
            assert ident not in seen
            seen.add(ident)
            decision = decisions[ident]
            assert decision['classificacao'] in set('ABCDE') and decision.get('fundamento')
            assert raw['classificacao_antes'] == baseline[ident]['classificacao_antes'], ident
            row = {k: copy.deepcopy(raw[k]) for k in FIELDS if k in raw}
            row['classificacao_antes'] = baseline[ident]['classificacao_antes']
            row['classificacao_depois'] = decision['classificacao']
            row['decisao_final'] = copy.deepcopy(decision)
            row['lacunas'] = copy.deepcopy(baseline[ident].get('lacunas', []))
            if isinstance(row['lacunas'], list):
                row['lacunas'] = ['No código recebido: ' + v if isinstance(v, str) else v
                                  for v in row['lacunas']]
            row['semantica_lacunas'] = 'Lacunas do código recebido; a situação restante está em pendencias_remanescentes.'
            row['alteracoes_planejadas_baseline'] = copy.deepcopy(baseline[ident].get('alteracoes_necessarias', []))
            row['testes_planejados_baseline'] = copy.deepcopy(baseline[ident].get('testes_adicionais', []))
            row['alteracoes_necessarias'] = copy.deepcopy(decision.get('acoes_remanescentes', []))
            row['testes_adicionais'] = copy.deepcopy(decision.get('testes_ainda_necessarios', []))
            reviewed_components = decision.get('componentes_revisados', raw['componentes_responsaveis'])
            row['componentes_responsaveis'] = [component(c) for c in reviewed_components]
            refs = references(raw)
            row['testes_existentes'] = [r for r in refs if matching(r, baseline_cases)]
            row['testes_implementados'] = [r for r in refs if not matching(r, baseline_cases)]
            row['fontes_testes'] = [test_source(r) for r in refs]
            row['matriz_criterios'] = []
            reviewed_criteria = {v['indice']: v for v in decision.get('criterios_revisados', [])}
            assert all(isinstance(i, int) and 0 <= i < len(raw['matriz_criterios'])
                       for i in reviewed_criteria), ident
            for index, current in enumerate(raw['matriz_criterios']):
                criterion = {k: copy.deepcopy(current[k]) for k in CRITERIA_FIELDS if k in current}
                if index in reviewed_criteria:
                    revision = reviewed_criteria[index]
                    assert revision.get('criterio') and revision.get('fundamento'), ident
                    criterion['criterio'] = revision['criterio']
                    criterion['revisao_documental'] = copy.deepcopy(revision)
                    if revision.get('evidencias_suplementares'):
                        criterion['evidencias_suplementares'] = copy.deepcopy(revision['evidencias_suplementares'])
                    if isinstance(row.get('criterios_aceitacao'), list) and index < len(row['criterios_aceitacao']):
                        accepted = row['criterios_aceitacao'][index]
                        if isinstance(accepted, str):
                            row['criterios_aceitacao'][index] = revision['criterio']
                        elif isinstance(accepted, dict):
                            accepted['criterio'] = revision['criterio']
                criterion['componentes'] = [component(c) for c in (current.get('componentes')
                    or current.get('componentes_metodos') or reviewed_components)]
                criterion['fontes_testes'] = [test_source(r) for r in criterion['testes']]
                criterion['origem_da_avaliacao'] = ('Confronto humano da regra com os componentes e assertivas '
                    'indicados. A matriz produzida pelo gerador identifica os resultados por XML e, '
                    'quando aplicável, por prova suplementar.')
                row['matriz_criterios'].append(criterion)
            row['proveniencia_avaliacao'] = {'notas': notes.name, 'sha256_notas': sha(notes),
                'arquivo_decisoes': args.decisoes.name, 'sha256_decisoes': sha(args.decisoes),
                'baseline': args.baseline.name, 'sha256_baseline': sha(args.baseline)}
            for key, value in decision.get('campos_revisados', {}).items():
                assert key in {'limitacoes_reais', 'limites_legado', 'limite_da_avaliacao'}, key
                row[key] = copy.deepcopy(value)
            if decision['classificacao'] == 'A':
                assert not row.get('pendencias_remanescentes'), ident + ': decisão A com pendência conhecida'
            output.append(portable(row, project, args.notas.resolve()))
        (args.saida / notes.name).write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
        provenance.append({'arquivo': notes.name, 'sha256_notas': sha(notes), 'requisitos': len(output)})
    assert seen == set(decisions), 'Cobertura de decisões incompleta'
    canonical = args.notas / 'requisitos_canonicos.json'
    shutil.copy2(canonical, args.saida / canonical.name)
    (args.saida / 'PROVENIENCIA.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'requisitos': len(seen), 'arquivos_de_avaliacao': len(provenance)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
