"""Regressões de configuração dos auxiliares reais, sem JVM, serviços ou SQL real."""
import contextlib
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

PASTA = Path(__file__).resolve().parent
sys.path.insert(0, str(PASTA))
SCHEMAS = ('sgt8045_baseline', 'sgt8045_candidata', 'sgt8045_upgrade', 'sgt8045_http',
           'sgt8045_409_red', 'sgt8045_regressao')


class PreparacaoTcpTest(unittest.TestCase):
    def executar_wrapper(self, schema, aceito, host='127.0.0.1'):
        with tempfile.TemporaryDirectory() as pasta:
            sentinela = Path(pasta) / 'maven-controlado'
            recibo = Path(pasta) / 'chamada.json'
            sentinela.write_text('#!/usr/bin/env python3\nimport json,os,sys\n'
                                'open(os.environ["RECIBO_SENTINELA"],"x").write(json.dumps(sys.argv[1:]))\n')
            sentinela.chmod(0o700)
            env = {**os.environ, 'INTEGRATION_MYSQL_URL': f'jdbc:mysql://{host}:33076/{schema}?serverTimezone=America/Sao_Paulo',
                   'INTEGRATION_MYSQL_USER': 'sgt_validacao_8045', 'INTEGRATION_MYSQL_PASSWORD': 'senha-sintetica',
                   'SGT_MAVEN_EXECUTABLE': str(sentinela), 'RECIBO_SENTINELA': str(recibo)}
            result = subprocess.run(['bash', str(PASTA / 'executar_regressao_mysql.sh')], env=env,
                                    text=True, capture_output=True, timeout=10)
            self.assertEqual(result.returncode, 0 if aceito else 2, result.stderr)
            self.assertEqual(recibo.exists(), aceito)
            if aceito:
                command = json.loads(recibo.read_text())
                self.assertIn('-Pintegration', command)
                self.assertIn('-DskipTests=false', command)
                self.assertIn('-DskipITs=false', command)
                self.assertIn('verify', command)

    def _exercitar_supervisor(self, schema):
        import gerenciar_jar_tcp
        env = {'REDIS_HOST': '127.0.0.1', 'REDIS_PORT': '26376', 'REDIS_DATABASE': '8'}
        self.assertEqual(gerenciar_jar_tcp.conexao_redis_http(schema, env, 8),
                         {'redis_host': '127.0.0.1', 'redis_port': 26376, 'redis_database': 8})

    def _exercitar_snapshot(self, schema):
        import fotografar_persistencia_jar as foto
        with tempfile.TemporaryDirectory() as pasta:
            raiz = Path(pasta)
            saida = raiz / 'publico'
            saida.mkdir()
            jar = raiz / 'candidato.jar'
            jar.write_bytes(b'artefato sintetico: nao executar')
            envfile = raiz / 'infra.env'
            envfile.write_text("export DB_USER='sgt_validacao_8045'\nexport DB_PASS='senha-sintetica'\n"
                               "export REDIS_HOST='127.0.0.1'\nexport REDIS_PORT='26376'\nexport REDIS_DATABASE='8'\n")
            processo = {'jar': str(jar), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'pid': 123,
                        'database': schema, 'mysql_host': '127.0.0.1', 'mysql_port': 33076,
                        'redis_host': '127.0.0.1', 'redis_port': 26376, 'redis_database': 8,
                        'base_http': 'http://127.0.0.1:18086', 'eventos': [{'acao': 'encerrado'}]}
            (saida / 'PROCESSO.json').write_text(json.dumps(processo))
            def capturar(command, **kwargs):
                self.assertEqual(kwargs['env']['MYSQL_USER'], 'sgt_validacao_8045')
                self.assertEqual(kwargs['env']['MYSQL_TCP_PORT'], '33076')
                self.assertEqual(command[command.index('--database') + 1], schema)
                destino = Path(command[command.index('--saida') + 1])
                dados = {'database': schema, 'formato_valores': 'HEX_CAST_BINARY_V2',
                         'configuracao': {'mysql': '8.0.45', 'foreign_key_checks': 1, 'innodb_strict_mode': 1},
                         'resumo_tabelas': {}, 'orfaos': [], 'tabelas': [], 'linhas': {}, 'indices': [], 'fks': []}
                destino.write_text(json.dumps(dados))
                return subprocess.CompletedProcess(command, 0)
            args = ['fotografar', '--saida', str(saida), '--privado', str(raiz / 'privado'),
                    '--infra-env', str(envfile), '--fase', 'antes_reinicio']
            with patch.object(sys, 'argv', args), patch.object(foto, 'vivo', return_value=False), \
                    patch.object(foto.subprocess, 'run', side_effect=capturar) as captura, \
                    contextlib.redirect_stdout(io.StringIO()):
                foto.main()
            self.assertEqual(captura.call_count, 1)
            result = json.loads((saida / 'PERSISTENCIA_antes_reinicio.json').read_text())
            self.assertEqual(result['database'], schema)
            self.assertEqual(result['coordenacao']['redis_port'], 26376)

    def test_wrapper_recusa_producao(self):
        self.executar_wrapper('sgt8045_producao', False)

    def test_wrapper_recusa_destino_remoto(self):
        self.executar_wrapper('sgt8045_http', False, 'banco.example.test')

    def test_supervisor_recusa_redis_divergente_ou_incompleto(self):
        import gerenciar_jar_tcp
        for env, argumento in [
            ({'REDIS_HOST': '127.0.0.1', 'REDIS_PORT': '26376', 'REDIS_DATABASE': '8'}, 9),
            ({'REDIS_HOST': 'remoto.example', 'REDIS_PORT': '26376', 'REDIS_DATABASE': '8'}, 8),
            ({'REDIS_HOST': '127.0.0.1', 'REDIS_DATABASE': '8'}, 8),
            ({'REDIS_HOST': '127.0.0.1', 'REDIS_PORT': '26376'}, 8),
        ]:
            with self.subTest(env=env, argumento=argumento), self.assertRaises(SystemExit):
                gerenciar_jar_tcp.conexao_redis_http('sgt8045_http', env, argumento)

    def test_reinicio_recusa_troca_de_identidade_redis(self):
        from gerenciar_jar_tcp import conferir_redis_reinicio
        state = {'redis_host': '127.0.0.1', 'redis_port': 26376, 'redis_database': 8}
        conferir_redis_reinicio(state, dict(state))
        for campo, valor in [('redis_host', 'localhost'), ('redis_port', 26377), ('redis_database', 9)]:
            with self.subTest(campo=campo), self.assertRaises(SystemExit):
                conferir_redis_reinicio(state, {**state, campo: valor})

    def test_redis_nao_completa_arquivo_com_ambiente_herdado(self):
        import gerenciar_jar_tcp as supervisor
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / 'infra.env'
            arquivo.write_text("export JAVA_HOME='/java-sintetico'\n")
            with patch.dict(os.environ, {'REDIS_HOST': '127.0.0.1', 'REDIS_PORT': '26376',
                                         'REDIS_DATABASE': '8'}):
                env = supervisor.carregar_ambiente(arquivo)
            with self.assertRaises(SystemExit):
                supervisor.conexao_redis_http('sgt8045_http', env, 8)

    def test_reinicio_recusa_troca_da_porta_mysql_antes_de_iniciar(self):
        import gerenciar_jar_tcp as supervisor
        with tempfile.TemporaryDirectory() as pasta:
            raiz = Path(pasta)
            jar = raiz / 'candidato.jar'
            jar.write_bytes(b'artefato sintetico: nao executar')
            arquivo = raiz / 'infra.env'
            arquivo.write_text("export JAVA_HOME='/java-sintetico'\n"
                               "export DB_USER='sgt_validacao_8045'\nexport DB_PASS='senha-sintetica'\n"
                               "export REDIS_HOST='127.0.0.1'\nexport REDIS_PORT='26376'\nexport REDIS_DATABASE='8'\n")
            state = {'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'database': 'sgt8045_http',
                     'base_http': 'http://127.0.0.1:18086', 'eventos': [{'acao': 'encerrado'}],
                     'mysql_host': '127.0.0.1', 'mysql_port': 33076,
                     'redis_host': '127.0.0.1', 'redis_port': 26376, 'redis_database': 8}
            (raiz / 'PROCESSO.json').write_text(json.dumps(state))
            args = ['supervisor', 'iniciar', '--saida', str(raiz), '--project', str(raiz),
                    '--jar', str(jar), '--infra-env', str(arquivo), '--trava', str(raiz / 'trava'),
                    '--database', 'sgt8045_http', '--mysql-port', '33077', '--redis-database', '8']
            with patch.object(sys, 'argv', args), patch.object(supervisor, 'vivo', return_value=False), \
                    patch.object(supervisor.socket, 'socket'), \
                    patch.object(supervisor.subprocess, 'Popen', side_effect=AssertionError('Não iniciar JVM')) as inicio:
                with self.assertRaisesRegex(SystemExit, 'MySQL'):
                    supervisor.main()
            inicio.assert_not_called()
            self.assertEqual(json.loads((raiz / 'PROCESSO.json').read_text()), state)

    def test_snapshot_recusa_redis_divergente_do_arquivo(self):
        import fotografar_persistencia_jar as foto
        env = {'REDIS_HOST': '127.0.0.1', 'REDIS_PORT': '26376', 'REDIS_DATABASE': '8'}
        processo = {'database': 'sgt8045_http', 'mysql_host': '127.0.0.1', 'mysql_port': 33076,
                    'redis_host': '127.0.0.1', 'redis_port': 26376, 'redis_database': 8}
        foto.conferir_conexao(processo, env)
        for campo, valor in [('redis_host', 'localhost'), ('redis_port', 26377), ('redis_database', 9)]:
            with self.subTest(campo=campo), self.assertRaises((RuntimeError, SystemExit)):
                foto.conferir_conexao({**processo, campo: valor}, env)


class RestauracaoSemPurgaTest(unittest.TestCase):
    def test_opcao_de_remocao_nao_apaga_acervo_com_contagem_igual_a_semente(self):
        consultas = []
        class Cursor:
            rowcount = 0
            def __enter__(self): return self
            def __exit__(self, *args): pass
            def execute(self, sql, params=None):
                consultas.append(sql)
                self.sql = sql
                if sql.startswith('DELETE '): self.rowcount = 1
            def fetchall(self):
                return [(t, 'InnoDB') for t in ['configuracao_sistema', 'gestao_local',
                                                'status_empregado', 'superintendencia_estadual']]
            def fetchone(self):
                quantidades = {'configuracao_sistema': 4, 'gestao_local': 81,
                               'status_empregado': 4, 'superintendencia_estadual': 27}
                return (quantidades[self.sql.split('`')[1]],)
        class Conexao:
            def cursor(self): return Cursor()
            def commit(self): pass
            def rollback(self): pass
            def close(self): pass
        mysql = types.ModuleType('pymysql')
        mysql.connect = lambda **kwargs: Conexao()
        with tempfile.TemporaryDirectory() as pasta, patch.dict(sys.modules, {'pymysql': mysql}):
            backup = Path(pasta) / 'backup.sql'
            backup.write_bytes(b'')
            spec = importlib.util.spec_from_file_location('restaurador_sem_purga', PASTA / 'restaurar_backup_sintetico.py')
            modulo = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(modulo)
            args = ['restaurar', '--database', 'sgt8045_upgrade', '--backup', str(backup),
                    '--sha256', hashlib.sha256(b'').hexdigest(), '--remover-sementes-iniciais']
            with patch.object(sys, 'argv', args), \
                    patch.dict(os.environ, {'MYSQL_USER': 'sgt_validacao_8045', 'MYSQL_PWD': 'senha-sintetica'}), \
                    contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                try:
                    modulo.main()
                except SystemExit as result:
                    self.assertEqual(result.code, 2)
            self.assertFalse(any(sql.startswith('DELETE ') for sql in consultas), consultas)


class CompilacaoOptInTest(unittest.TestCase):
    def test_compilador_preserva_recibo_existente_sem_executar_javac(self):
        import compilar_fixture_tcp_optin as compilador
        with tempfile.TemporaryDirectory() as pasta:
            raiz = Path(pasta)
            xml = raiz / 'classe.xml'
            xml.write_text('<testsuite><properties><property name="java.class.path" '
                           'value="/dependencias-sinteticas"/></properties></testsuite>')
            env = raiz / 'infra.env'
            env.write_text("export JAVA_HOME='/java-sintetico'\n")
            recibo = raiz / 'recibo.json'
            anterior = b'{"execucao_anterior":true}\n'
            recibo.write_bytes(anterior)
            args = ['compilar', '--project', str(raiz), '--xml-classpath', str(xml),
                    '--infra-env', str(env), '--saida', str(recibo)]
            with patch.object(sys, 'argv', args), \
                    patch.object(compilador.subprocess, 'run', side_effect=AssertionError('Não compilar')) as run:
                with self.assertRaises(SystemExit):
                    compilador.main()
            run.assert_not_called()
            self.assertEqual(recibo.read_bytes(), anterior)


def caso_wrapper(schema):
    return lambda self: self.executar_wrapper(schema, True)


def caso_supervisor(schema):
    return lambda self: self._exercitar_supervisor(schema)


def caso_snapshot(schema):
    return lambda self: self._exercitar_snapshot(schema)


for _schema in SCHEMAS:
    setattr(PreparacaoTcpTest, 'test_wrapper_' + _schema, caso_wrapper(_schema))
    setattr(PreparacaoTcpTest, 'test_supervisor_' + _schema, caso_supervisor(_schema))
    setattr(PreparacaoTcpTest, 'test_snapshot_' + _schema, caso_snapshot(_schema))


if __name__ == '__main__':
    unittest.main()
