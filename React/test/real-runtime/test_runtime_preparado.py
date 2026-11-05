"""Preparation checks with controlled subprocesses; never starts a real service."""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace
import zipfile
from contextlib import redirect_stderr, redirect_stdout


class PreparedRuntimeTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for name in ['private/app', 'private/reports', 'evidence', 'bin']:
            (self.root / name).mkdir(parents=True, exist_ok=True)
        for name in ['mysql', 'mysqld', 'java']:
            (self.root / 'bin' / name).write_text('synthetic placeholder; never execute\n')
        self.client_options = self.root / 'private/mysql-app.cnf'
        self.client_options.write_text('[client]\nuser=sgt_app\npassword=synthetic-password\nhost=127.0.0.1\nport=13317\nprotocol=TCP\n')
        self.app_env = self.root / 'private/app/app.env'
        self.app_env.write_text('DB_URL=jdbc:mysql://127.0.0.1:13317/sgt_validacao?createDatabaseIfNotExist=false\nDB_USER=sgt_app\nDB_PASS=synthetic-password\nspring.jpa.hibernate.ddl-auto=update\nspring.sql.init.mode=never\n')
        self.jar = self.root / 'candidate.jar'
        with zipfile.ZipFile(self.jar, 'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nImplementation-Version: 0.0.4-operacional-20261101\n')
        self.sha = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        self.config = {
            'private_dir': str(self.root / 'private'), 'evidence_dir': str(self.root / 'evidence'),
            'schema': 'sgt_validacao',
            'mysql': {'client': str(self.root / 'bin/mysql'), 'client_options': str(self.client_options), 'mysqld': str(self.root / 'bin/mysqld')},
            'java': {'executable': str(self.root / 'bin/java'), 'jar': str(self.jar), 'sha256': self.sha,
                     'version': '0.0.4-operacional-20261101', 'app_env': str(self.app_env)},
        }
        self.config_path = self.root / 'private/runtime.json'
        self.config_path.write_text(json.dumps(self.config))
        self.config_path.chmod(0o600)
        self.environment = patch.dict(os.environ, {'SGT_RUNTIME_CONFIG': str(self.config_path)})
        self.environment.start(); self.addCleanup(self.environment.stop)
        spec = importlib.util.spec_from_file_location('prepared_runtime_under_test', Path(__file__).with_name('manage.py'))
        self.helper = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.helper)
        self.server_version = '8.0.45'
        self.binary_version = '8.0.45'
        self.run_patch = patch.object(self.helper.subprocess, 'run', side_effect=self.fake_run)
        self.run_mock = self.run_patch.start(); self.addCleanup(self.run_patch.stop)
        self.popen_patch = patch.object(self.helper.subprocess, 'Popen', side_effect=AssertionError('No service may start in preparation tests'))
        self.popen_mock = self.popen_patch.start(); self.addCleanup(self.popen_patch.stop)

    def fake_run(self, args, **kwargs):
        name = Path(args[0]).name
        if name == 'mysqld':
            return subprocess.CompletedProcess(args, 0, f'mysqld  Ver {self.binary_version} for Linux on x86_64\n', '')
        if name == 'java':
            return subprocess.CompletedProcess(args, 0, '', 'openjdk version "17.0.18" 2026-01-20\n')
        query = kwargs.get('input', '')
        output = f'{self.server_version}\t11111111-2222-3333-4444-555555555555\tsgt_validacao\t13317\tsgt_app@127.0.0.1\n' if 'VERSION()' in query else '1\n'
        return subprocess.CompletedProcess(args, 0, output, '')

    def test_rejects_wrong_server_version_before_query(self):
        self.server_version = '8.4.11'
        with self.assertRaisesRegex(RuntimeError, '8.0.45'):
            self.helper.mysql('SELECT 1;')

    def test_rejects_mysql_8046_from_select_version_before_business_query(self):
        # A versão efetiva do servidor é obrigatória mesmo sem binário local.
        self.config['mysql'].pop('mysqld')
        self.config_path.write_text(json.dumps(self.config))
        self.server_version = '8.0.46'
        with self.assertRaisesRegex(RuntimeError, '8.0.45'):
            self.helper.mysql('SELECT 1;')
        self.assertTrue(all(call.kwargs.get('input') != 'SELECT 1;' for call in self.run_mock.call_args_list))
        self.popen_mock.assert_not_called()

    def test_rejects_noncanonical_binary_suffix_before_any_server_query(self):
        self.binary_version = '8.0.45-other'
        with self.assertRaisesRegex(RuntimeError, '8.0.45'):
            self.helper.mysql('SELECT 1;')
        self.assertEqual(self.run_mock.call_count, 1)
        self.popen_mock.assert_not_called()

    def test_mysql_8045_uses_only_explicit_client_options_without_inherited_login_file(self):
        with patch.dict(os.environ, {'MYSQL_TEST_LOGIN_FILE': '/ignored/personal-login.cnf',
                                     'MYSQL_PWD': 'other-password', 'MYSQL_HOST': 'other-host'}):
            self.assertEqual(self.helper.mysql('SELECT 1;'), '1')
        args, kwargs = self.run_mock.call_args
        command = args[0]
        self.assertEqual(command[1], '--defaults-file=' + str(self.client_options))
        self.assertNotIn('--no-login-paths', command)
        self.assertEqual(kwargs['env'].get('MYSQL_TEST_LOGIN_FILE'), os.devnull)
        self.assertNotIn('MYSQL_PWD', kwargs['env'])
        self.assertNotIn('MYSQL_HOST', kwargs['env'])
        self.assertNotIn('synthetic-password', ' '.join(command))

    def test_requires_exact_server_version_even_with_a_suffix(self):
        self.server_version = '8.0.45-other'
        with self.assertRaisesRegex(RuntimeError, '8.0.45'):
            self.helper.mysql('SELECT 1;')

    def test_rejects_root_client_before_any_subprocess(self):
        self.client_options.write_text('[client]\nuser=root\npassword=synthetic-password\nhost=127.0.0.1\nport=13317\n')
        with self.assertRaisesRegex(RuntimeError, 'aplica|root'):
            self.helper.mysql('SELECT 1;')
        self.run_mock.assert_not_called()

    def test_rejects_wrong_server_binary(self):
        self.binary_version = '8.4.11'
        with self.assertRaisesRegex(RuntimeError, '8.0.45'):
            self.helper.mysql('SELECT 1;')

    def test_missing_explicit_config_does_not_use_a_default(self):
        os.environ.pop('SGT_RUNTIME_CONFIG', None)
        with self.assertRaisesRegex(RuntimeError, 'SGT_RUNTIME_CONFIG'):
            self.helper.mysql('SELECT 1;')
        self.run_mock.assert_not_called()

    def test_rejects_automatic_catalog_creation_before_java(self):
        self.app_env.write_text(self.app_env.read_text().replace('createDatabaseIfNotExist=false', 'createDatabaseIfNotExist=true'))
        with self.assertRaisesRegex(RuntimeError, 'cria|createDatabaseIfNotExist'):
            self.helper.serve_app(self.sha)
        self.popen_mock.assert_not_called()

    def test_rejects_hash_mismatch_before_java(self):
        with self.assertRaisesRegex(RuntimeError, 'SHA|hash'):
            self.helper.serve_app('0' * 64)
        self.popen_mock.assert_not_called()

    def test_rejects_url_account_overrides_before_java(self):
        original = self.app_env.read_text()
        for suffix in ['&user=root', '&password=override', '&propertiesTransform=override']:
            with self.subTest(parameter=suffix):
                self.app_env.write_text(original.replace('createDatabaseIfNotExist=false', 'createDatabaseIfNotExist=false' + suffix))
                with self.assertRaisesRegex(RuntimeError, 'conta|propriedades'):
                    self.helper.serve_app(self.sha)
        self.app_env.write_text(original.replace('jdbc:mysql://', 'jdbc:mysql://root:override@'))
        with self.assertRaisesRegex(RuntimeError, 'credenciais'):
            self.helper.serve_app(self.sha)
        self.popen_mock.assert_not_called()

    def test_rejects_datasource_override_in_private_properties(self):
        self.app_env.write_text(self.app_env.read_text() + 'SPRING_DATASOURCE_USERNAME=root\n')
        with self.assertRaisesRegex(RuntimeError, 'datasource'):
            self.helper.serve_app(self.sha)
        self.popen_mock.assert_not_called()

    def test_rejects_previous_artifact_version_even_with_its_declared_hash(self):
        self.config['java']['version'] = '0.0.1-SNAPSHOT'
        self.config_path.write_text(json.dumps(self.config))
        with self.assertRaisesRegex(RuntimeError, 'candidata'):
            self.helper.serve_app(self.sha)
        self.popen_mock.assert_not_called()

    def test_forbidden_initialization_action_never_dispatches(self):
        with patch('sys.argv', ['manage.py', 'init']):
            with self.assertRaises(SystemExit) as result, redirect_stderr(io.StringIO()):
                self.helper.main()
        self.assertEqual(result.exception.code, 2)
        self.run_mock.assert_not_called(); self.popen_mock.assert_not_called()

    def test_status_records_observed_identity_without_claiming_a_running_jar(self):
        with redirect_stdout(io.StringIO()):
            result = self.helper.status()
        self.assertEqual(result['mysql']['version'], '8.0.45')
        self.assertEqual(result['mysql']['version_source'], 'SELECT VERSION()')
        self.assertEqual(result['mysql']['server_uuid'], '11111111-2222-3333-4444-555555555555')
        self.assertEqual(result['mysql']['schema'], 'sgt_validacao')
        self.assertEqual(result['mysql']['port'], 13317)
        self.assertFalse(result['java']['running'])
        self.assertEqual(result['java']['jar_sha256'], self.sha)
        self.assertNotIn('synthetic-password', json.dumps(result))
        self.popen_mock.assert_not_called()

    def launch_with_controlled_child(self):
        """Exercise the real receipt writer, without executing Java/MySQL."""
        log = self.root / 'private/app/java.log'
        log.write_bytes(b'old readiness\n')

        def spawn(*args, **kwargs):
            # The child can write immediately: the boundary must precede Popen.
            with log.open('ab') as stream:
                stream.write(b'new launch output\n')
            return SimpleNamespace(pid=4321, poll=lambda: 0, wait=lambda **kw: 0)

        token = {'start_ticks': 12345, 'kernel_boot_id': 'synthetic-kernel-boot'}
        with patch.object(self.helper.subprocess, 'Popen', side_effect=spawn), \
             patch.object(self.helper, '_process_identity', return_value=token, create=True), \
             patch.object(self.helper.signal, 'signal'):
            with self.assertRaisesRegex(RuntimeError, 'encerrou'):
                self.helper.serve_app(self.sha)
        return json.loads((self.root / 'evidence/jar-process.json').read_text())

    def test_launch_receipt_binds_the_process_start_and_kernel_boot(self):
        receipt = self.launch_with_controlled_child()
        self.assertEqual({key: receipt.get(key) for key in ('pid', 'start_ticks', 'kernel_boot_id')},
                         {'pid': 4321, 'start_ticks': 12345, 'kernel_boot_id': 'synthetic-kernel-boot'})

    def test_launch_receipt_records_log_boundary_before_child_can_write(self):
        receipt = self.launch_with_controlled_child()
        self.assertEqual(receipt.get('log_offset'), 14)


class ProcessLaunchIdentityTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.proc = Path(self.temporary.name)
        (self.proc / '4321').mkdir()
        (self.proc / 'sys/kernel/random').mkdir(parents=True)
        self.stat = self.proc / '4321/stat'
        self.stat.write_text('4321 (java worker (SGT)) ' + ' '.join(['S'] + ['0'] * 18 + ['12345']))
        self.boot = self.proc / 'sys/kernel/random/boot_id'
        self.boot.write_text('synthetic-kernel-boot\n')
        spec = importlib.util.spec_from_file_location('launch_identity_under_test', Path(__file__).with_name('manage.py'))
        self.helper = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.helper)

    def test_reads_start_ticks_and_kernel_boot_from_process_observation(self):
        self.assertEqual(self.helper._process_identity(4321, self.proc),
                         {'start_ticks': 12345, 'kernel_boot_id': 'synthetic-kernel-boot'})

    def test_zombie_process_cannot_produce_a_launch_token(self):
        self.stat.write_text(self.stat.read_text().replace(') S ', ') Z '))
        with self.assertRaisesRegex(RuntimeError, 'identidade viva'):
            self.helper._process_identity(4321, self.proc)

    def test_missing_or_incomplete_process_observation_cannot_produce_token(self):
        self.boot.write_text('')
        with self.assertRaisesRegex(RuntimeError, 'identidade viva'):
            self.helper._process_identity(4321, self.proc)
        self.boot.write_text('synthetic-kernel-boot\n')
        self.stat.write_text('4321 (java) S')
        with self.assertRaisesRegex(RuntimeError, 'identidade viva'):
            self.helper._process_identity(4321, self.proc)
        self.stat.unlink()
        with self.assertRaisesRegex(RuntimeError, 'identidade viva'):
            self.helper._process_identity(4321, self.proc)


if __name__ == '__main__':
    unittest.main()
