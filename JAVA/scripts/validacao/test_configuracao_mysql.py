#!/usr/bin/env python3
"""Configuração dos scripts reais, com conectores sentinela e nenhuma conexão externa."""
import contextlib
import hashlib
import importlib.util
import io
import os
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import patch


PASTA = Path(__file__).resolve().parent
sys.path.insert(0, str(PASTA))
SCRIPTS = (
    "capturar_schema_mysql",
    "verificar_captura_valores_mysql",
    "restaurar_backup_sintetico",
    "validar_ocorrencia_idempotencia_automatica_tcp",
)


class ConfiguracaoMysqlScriptsTest(unittest.TestCase):
    def test_catalogos_adicionais_autorizados_sem_liberar_prefixo_inteiro(self):
        from configuracao_mysql import schema_sintetico
        self.assertTrue(schema_sintetico('sgt8045_409_red'))
        self.assertTrue(schema_sintetico('sgt8045_regressao'))
        self.assertFalse(schema_sintetico('sgt8045_producao'))
        self.assertFalse(schema_sintetico('sgt8045_regressao_copia_nao_autorizada'))

    def test_captura_nao_sobrescreve_evidencia_anterior(self):
        with tempfile.TemporaryDirectory() as temporario:
            saida = Path(temporario) / "referencia.json"
            original = b'{"referencia": "preservada"}\n'
            saida.write_bytes(original)
            env = {"MYSQL_USER": "sgt_validacao_8045", "MYSQL_PWD": "senha-sintetica"}
            def nao_conectar(*args, **kwargs):
                raise AssertionError("Conectou antes de proteger a referência")
            pymysql = types.ModuleType("pymysql")
            pymysql.connect = nao_conectar
            with patch.dict(os.environ, env), \
                    patch.dict(sys.modules, {"pymysql": pymysql}), \
                    patch.object(sys, "argv", ["capturar_schema_mysql", "--database", "sgt8045_http", "--saida", str(saida)]), \
                    patch("subprocess.run", side_effect=nao_conectar):
                import capturar_schema_mysql
                with self.assertRaises(FileExistsError):
                    capturar_schema_mysql.main()
            self.assertEqual(saida.read_bytes(), original)

    def executar(self, script, ambiente, espera_conexao=False):
        # A fronteira da conexão é substituída por uma sentinela que falha se
        # o programa tentar conectar antes de validar sua identidade de banco.
        def nao_conectar(*args, **kwargs):
            if espera_conexao:
                if script == "verificar_captura_valores_mysql":
                    command = args[0]
                    self.assertIn("--user=sgt_validacao_8045", command)
                    self.assertIn("--host=127.0.0.1", command)
                    self.assertIn("--port=33076", command)
                    self.assertEqual(command[1], "--no-defaults")
                    self.assertEqual(kwargs["env"]["MYSQL_TEST_LOGIN_FILE"], os.devnull)
                    self.assertNotIn("senha-sintetica", " ".join(command))
                else:
                    self.assertEqual(kwargs["user"], "sgt_validacao_8045")
                    self.assertEqual(kwargs["password"], "senha-sintetica")
                    self.assertEqual(kwargs["host"], "127.0.0.1")
                    self.assertEqual(kwargs["port"], 33076)
                    self.assertEqual(kwargs["database"], "sgt8045_http")
                raise ConexaoValidada()
            raise AssertionError("Conector recebeu configuração que deveria ser recusada antes da conexão")

        pymysql = types.ModuleType("pymysql")
        pymysql.connect = nao_conectar
        pymysql.cursors = types.SimpleNamespace(DictCursor=object)
        with tempfile.TemporaryDirectory() as temporario:
            pasta = Path(temporario)
            backup = pasta / "backup.sql"
            backup.write_bytes(b"")
            fixture = pasta / "fixture.json"
            fixture.write_text("{}")
            estado = pasta / "estado.json"
            estado.write_text('{"pessoa_id": 1}')
            args = {
                "capturar_schema_mysql": ["--database", "sgt_audit_ri06", "--saida", str(pasta / "captura.json")],
                "verificar_captura_valores_mysql": ["--saida", str(pasta / "controle.json")],
                "restaurar_backup_sintetico": ["--database", "sgt_audit_ri06", "--backup", str(backup),
                                                "--sha256", hashlib.sha256(b"").hexdigest()],
                "validar_ocorrencia_idempotencia_automatica_tcp": ["--base", "http://127.0.0.1:18080",
                    "--fixture", str(fixture), "--estado", str(estado), "--saida", str(pasta / "prova"),
                    "--database", "sgt_audit_ri06"],
            }[script]
            if espera_conexao:
                args = ["sgt8045_http" if arg == "sgt_audit_ri06" else arg for arg in args]
            env = {k: v for k, v in os.environ.items() if not k.startswith("MYSQL_")}
            env.update(ambiente)
            with patch.dict(sys.modules, {"pymysql": pymysql}), patch.dict(os.environ, env, clear=True), \
                    patch.object(sys, "argv", [script] + args), \
                    patch("subprocess.run", side_effect=nao_conectar), \
                    contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                spec = importlib.util.spec_from_file_location("probe_" + script, PASTA / (script + ".py"))
                modulo = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(modulo)
                if espera_conexao:
                    with self.assertRaises(ConexaoValidada):
                        modulo.main()
                else:
                    with self.assertRaisesRegex(ValueError, "Configuração MySQL"):
                        modulo.main()


class ConexaoValidada(Exception):
    pass


def caso(script, ambiente, espera_conexao=False):
    def teste(self):
        self.executar(script, ambiente, espera_conexao)
    return teste


for _script in SCRIPTS:
    for _nome, _env in {
        "sem_usuario": {"MYSQL_PWD": "senha-sintetica"},
        "sem_senha": {"MYSQL_USER": "sgt_validacao_8045"},
        "administrador": {"MYSQL_USER": "root", "MYSQL_PWD": "senha-sintetica"},
    }.items():
        setattr(ConfiguracaoMysqlScriptsTest, "test_" + _script + "_" + _nome, caso(_script, _env))
    setattr(ConfiguracaoMysqlScriptsTest, "test_" + _script + "_identidade_explicita",
            caso(_script, {"MYSQL_USER": "sgt_validacao_8045", "MYSQL_PWD": "senha-sintetica",
                           "MYSQL_TCP_PORT": "33076"}, True))


if __name__ == "__main__":
    unittest.main()
