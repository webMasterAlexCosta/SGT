#!/usr/bin/env python3
"""Gera os relatórios da entrega a partir de matrizes e verificações preservadas.

Não decide classificações, não transforma cobertura em completude e não executa
testes. O arquivo de verificações deve ser consolidado de resultados reais.
"""
import argparse
import collections
import json
from pathlib import Path

LABELS = {'A': 'Implementado integralmente', 'B': 'Implementado parcialmente',
          'C': 'Implementado incorretamente', 'D': 'Não implementado',
          'E': 'Ambíguo ou bloqueado'}


def flatten(value):
    if value is None:
        return 'Não se aplica.'
    if isinstance(value, list):
        return '; '.join(flatten(v) for v in value) or 'Nenhuma.'
    if isinstance(value, dict):
        return '; '.join(f'{k}: {flatten(v)}' for k, v in value.items())
    return str(value).replace('\n', ' ').replace('|', ' / ')


def percent(counter):
    total = counter['covered'] + counter['missed']
    return f"{100 * counter['covered'] / total:.4f}%" if total else 'Não se aplica'


def cases(row):
    return {(c['classe'], c['caso']): c
            for ref in row['evidencia_execucao'] for c in ref['casos']}


def write(folder, name, lines):
    separated = []
    for line in lines:
        if line and separated and separated[-1]:
            same_table = line.startswith('|') and separated[-1].startswith('|')
            if not same_table:
                separated.append('')
        separated.append(line)
    (folder / name).write_text('\n'.join(separated).rstrip() + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--diretorio', type=Path, required=True)
    parser.add_argument('--verificacoes', type=Path, required=True)
    args = parser.parse_args()
    folder = args.diretorio
    final = json.loads((folder / 'MATRIZ_IMPLEMENTACAO_198_REQUISITOS_FINAL.json').read_text())
    baseline = json.loads((folder / 'MATRIZ_IMPLEMENTACAO_198_REQUISITOS_BASELINE.json').read_text())
    checks = json.loads(args.verificacoes.read_text())
    rows = final['requisitos']
    assert len(rows) == 198 and len({r['id'] for r in rows}) == 198
    assert checks['regressao']['conclusao'] == 'BUILD SUCCESS', 'Não gerar entrega aprovada sem regressão concluída'
    before = baseline['metadados']['contagem']
    after = final['metadados']['contagem']
    assert sum(before.values()) == sum(after.values()) == 198
    pending = [r for r in rows if r['classificacao_depois'] != 'A']
    disclaimer = ('Avaliação de implementação e regressão feita durante o desenvolvimento. '
                  'Não constitui homologação formal independente dos 198 requisitos ou de todas as combinações HTTP.')
    count_table = ['| Classificação | Antes | Depois |', '|---|---:|---:|'] + [
        f'| {LABELS[k]} | {before[k]} | {after[k]} |' for k in LABELS] + ['| Total | 198 | 198 |']
    test_before = checks['baseline']
    test_after = checks['regressao']
    test_table = ['| Suíte | Baseline | Entrega | Falhas / erros / ignorados na entrega |', '|---|---:|---:|---|']
    for label, key in [('Unitários', 'surefire-reports'), ('Integrações MySQL', 'failsafe-reports')]:
        b, a = test_before[key], test_after[key]
        test_table.append(f"| {label} | {b['tests']} | {a['tests']} | {a['failures']} / {a['errors']} / {a['skipped']} |")
    coverage_table = ['| Métrica JaCoCo | Baseline | Entrega | Cobertos / total na entrega |', '|---|---:|---:|---:|']
    for key, name in [('LINE', 'Linhas'), ('BRANCH', 'Ramos'), ('INSTRUCTION', 'Instruções'),
                      ('METHOD', 'Métodos'), ('CLASS', 'Classes')]:
        b, a = test_before['jacoco'][key], test_after['jacoco'][key]
        coverage_table.append(f"| {name} | {percent(b)} | {percent(a)} | {a['covered']} / {a['covered'] + a['missed']} |")
    intro = ['# Relatório final da implementação funcional SGT/GTRAT', '',
             f"**Resultado:** {after['A']} requisitos em A; {len(pending)} permanecem fora de A. "
             'A classificação decorre da análise da regra e dos critérios ligados a provas, e não da mera aprovação da suíte.', '',
             disclaimer, '', '## 1. Resultado individual consolidado', '', *count_table, '',
             'A matriz JSON contém regra original, componentes, entidades, estados, pré/pós-condições, invariantes, '
             'perfis, escopo, endpoints, validações, persistência, falhas, histórico, dependências, critérios e XMLs por requisito. '
             'As classificações iniciais e suas erratas estão preservadas; a coluna Antes apresenta a avaliação corrigida do código recebido.', '',
             '## 2. Preservação e baseline', '', *checks['preservacao_texto'], '',
             '## 3. Implementações e correções', '', *checks['mudancas_texto'], '',
             '## 4. DIF-02 e pontos da auditoria anterior', '', *checks['hipoteses_texto'], '',
             '## 5. Testes executados e limites da prova', '', *test_table, '',
             *checks['regressao_texto'], '', '## 6. Persistência, recuperação e evolução do schema', '',
             *checks['schema_texto'], '', '## 7. JAR real, HTTP/TCP e reinício', '', *checks['jar_texto'], '',
             '## 8. Cobertura', '', *coverage_table, '',
             'JaCoCo mede execução de código. Nenhum percentual da tabela é um percentual de requisitos implementados.', '',
             '## 9. Revisão independente de desenvolvimento', '', *checks['revisao_texto'], '',
             '## 10. Pendências funcionais', '']
    for r in pending:
        intro += [f"### {r['id']} — {LABELS[r['classificacao_depois']]}", '', r['enunciado_original'], '',
                  flatten(r.get('pendencias_remanescentes') or r.get('limitacoes_reais')), '']
    intro += ['## 11. Contratos com o frontend', '',
              'Os arquivos React recebidos foram preservados byte a byte. As alterações de contrato estão em '
              'INTEGRACAO_FRONTEND_CONTRATOS_ATUALIZADOS.md; elas devem ser tratadas pelo consumidor na etapa própria.', '',
              '## 12. Reprodução e recuperação', '', *checks['reproducao_texto'], '',
              '## 13. Limites explícitos', '', *checks['limites_texto'], '',
              '## 14. Índice das provas', '', *checks['fontes_texto']]
    write(folder, 'RELATORIO_IMPLEMENTACAO_FUNCIONAL_FINAL.md', intro)

    write(folder, 'RELATORIO_COBERTURA.md', ['# Cobertura JaCoCo da implementação', '', disclaimer, '',
        *coverage_table, '', '## Origem e denominadores', '', *checks['cobertura_texto'], '',
        'A cobertura combina unitários e integrações da execução final preservada. Repetições de lotes de desenvolvimento '
        'não são somadas como novos testes nem utilizadas para inflar o percentual. A matriz funcional é independente desta métrica.'])

    report = ['# Testes por requisito', '', disclaimer, '',
              'Um cenário pode provar critérios de vários requisitos; as quantidades desta tabela não são somáveis. '
              'O JSON da matriz identifica os nomes parametrizados, XMLs e hashes. Um HTTP isolado não substitui '
              'as assertivas de conteúdo, banco e histórico descritas em cada critério.', '',
              '| ID | Antes → depois | Critérios explícitos | Casos distintos vinculados | Sem prova aprovada |', '|---|---|---:|---:|---:|']
    for r in rows:
        criteria = r.get('matriz_criterios', [])
        no_proof = sum(c.get('resultado_execucao') != 'APROVADO' for c in criteria)
        report.append(f"| {r['id']} | {r['classificacao_antes']} → {r['classificacao_depois']} | {len(criteria)} | {len(cases(r))} | {no_proof} |")
    report += ['', '## Execução integral', '', *test_table, '', *checks['regressao_texto'], '',
               '## Rastreabilidade reproduzível', '',
               'scripts/atualizar_rastreabilidade_198.py lê decisões humanas e resolve Classe#metodo contra JUnit real. '
               'Recusa A sem referências aprovadas e sem mapeamento de critérios. Esse bloqueio automatizado verifica '
               'a integridade da rastreabilidade; a suficiência funcional continua sendo uma decisão técnica documentada.']
    write(folder, 'RELATORIO_TESTES_POR_REQUISITO.md', report)

    for code, filename, title in [
            ('B', 'MATRIZ_REGRAS_PARCIAIS_CORRIGIDAS.md', 'Regras parciais tratadas'),
            ('C', 'MATRIZ_REGRAS_INCORRETAS_CORRIGIDAS.md', 'Regras incorretas tratadas')]:
        selected = [r for r in rows if r['classificacao_antes'] == code]
        text = ['# ' + title, '', disclaimer, '',
                'A seleção usa a classificação do requisito no baseline revisado. Cada linha conserva o estado final real, '
                'inclusive E quando falta fonte documental. A mudança implementada pode atender mais de um requisito.', '',
                '| Requisito | Depois | Lacuna original | Implementação/correção |', '|---|---|---|---|']
        for r in selected:
            text.append(f"| {r['id']} | {r['classificacao_depois']} | {flatten(r.get('lacunas'))} | "
                        f"{flatten(r.get('alteracoes_realizadas') or r.get('alteracoes_implementadas'))} |")
        text += ['', 'Provas específicas e efeitos de persistência: matriz final, seção individual de cada ID.']
        write(folder, filename, text)

    entirely_missing = [r for r in rows if r['classificacao_antes'] == 'D']
    missing_report = ['# Regras ausentes implementadas', '', disclaimer, '',
        f"O baseline revisado contém {len(entirely_missing)} requisito(s) integralmente ausente(s), classificação D. "
        'Uma ramificação ausente dentro de um requisito parcialmente implementado recebe B, não transforma o requisito inteiro em D.', '',
        'As novas regras e operações introduzidas em requisitos B/C estão descritas abaixo e nas matrizes de correção.', '',
        *checks['regras_novas_texto']]
    write(folder, 'MATRIZ_REGRAS_AUSENTES_IMPLEMENTADAS.md', missing_report)

    pending_report = ['# Pendências funcionais remanescentes', '', disclaimer, '',
        f'{len(pending)} requisitos permanecem fora de A. Nenhum foi promovido para cumprir a meta numérica.', '']
    for r in pending:
        pending_report += [f"## {r['id']} — {LABELS[r['classificacao_depois']]}", '',
            '**Enunciado:** ' + r['enunciado_original'], '',
            '**Impedimento verificável:** ' + flatten(r.get('pendencias_remanescentes') or r.get('limitacoes_reais')), '',
            '**Comportamento entregue:** ' + flatten(r.get('alteracoes_realizadas') or r.get('alteracoes_implementadas')), '',
            '**Para desbloquear:** ' + flatten(r.get('alteracoes_necessarias')), '',
            '**Verificação após obter a fonte:** ' + flatten(r.get('testes_adicionais')), '',
            '**Critérios e testes de proteção:** ' + '; '.join(c['criterio'] for c in r.get('matriz_criterios', [])), '']
    pending_report += ['## Limites de ambiente e de verificação', '', *checks['limites_texto']]
    write(folder, 'PENDENCIAS_FUNCIONAIS_REMANESCENTES.md', pending_report)
    write(folder, 'RELATORIO_REGRESSAO.md', ['# Regressão e ciclos de desenvolvimento', '', disclaimer, '',
        *test_table, '', *checks['regressao_texto'], '', '## Falhas preservadas e tratamento', '',
        *checks['falhas_texto'], '', '## Autenticidade do candidato', '', *checks['autenticidade_texto']])
    write(folder, 'RELATORIO_CONCORRENCIA_TRANSACOES.md', ['# Concorrência, transações e rollback', '', disclaimer, '',
        *checks['concorrencia_texto'], '', '## Persistência e evolução', '', *checks['schema_texto'], '',
        '## Orçamento de recursos', '', *checks['recursos_texto']])
    print(json.dumps({'relatorios_gerados': 9, 'requisitos': len(rows), 'contagem': after,
                      'criterios': sum(len(r.get('matriz_criterios', [])) for r in rows)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
