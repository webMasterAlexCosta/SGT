"""SGT: backup coordenado e restauração data-only, sem DDL, purga ou defaults privados.

Configuração JSON privada explícita. A aplicação deve estar parada. PostgreSQL
18.6, role própria limitada e schema public são conferidos em cada operação.
O schema de restauração deve ter sido produzido pelo PreparadorSchemaNovo/JPA.
"""
from contextlib import contextmanager
from datetime import datetime, timezone
import argparse
import hashlib
import ipaddress
import json
import os
from pathlib import Path, PurePosixPath
import queue
import re
import shutil
import subprocess
import threading
import uuid


class OperationError(RuntimeError):
    """Código seguro para o operador; nunca inclui respostas SQL ou valores privados."""


def file_hash(path):
    with Path(path).open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def _private(path):
    path = Path(path)
    if path.is_symlink() or not path.exists():
        raise OperationError('SGT_OP_PRIVATE_PATH_INVALID')
    if os.name != 'nt':
        if path.stat().st_uid != os.getuid() or path.stat().st_mode & 0o077:
            raise OperationError('SGT_OP_PRIVATE_PERMISSIONS_REQUIRED')
    else:
        # Windows chmod não verifica ACL. Ler somente SIDs/permissões, sem conteúdo.
        code = '''$ErrorActionPreference='Stop'; $me=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value;
$ok=@($me,'S-1-5-18','S-1-5-32-544'); $a=Get-Acl -LiteralPath $env:SGT_PRIVATE_PATH;
foreach($r in $a.Access){if($r.AccessControlType -eq 'Allow' -and $r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $ok){exit 3}}; exit 0'''
        result = subprocess.run(['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', code],
                                env={**os.environ, 'SGT_PRIVATE_PATH': str(path)}, capture_output=True, timeout=20)
        if result.returncode:
            raise OperationError('SGT_OP_PRIVATE_NTFS_ACL_REQUIRED')


def _new_private_dir(path):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    path.mkdir(mode=0o700)
    if os.name == 'nt':
        code = '''$ErrorActionPreference='Stop'; $me=[Security.Principal.WindowsIdentity]::GetCurrent().User;
$a=New-Object Security.AccessControl.DirectorySecurity; $a.SetAccessRuleProtection($true,$false);
$r=New-Object Security.AccessControl.FileSystemAccessRule($me,'FullControl','ContainerInherit,ObjectInherit','None','Allow');
$a.AddAccessRule($r); Set-Acl -LiteralPath $env:SGT_PRIVATE_PATH -AclObject $a'''
        r = subprocess.run(['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', code],
                           env={**os.environ, 'SGT_PRIVATE_PATH': str(path)}, capture_output=True, timeout=20)
        if r.returncode:
            raise OperationError('SGT_OP_PRIVATE_NTFS_ACL_REQUIRED')
    _private(path)


def _write_json(path, value):
    path = Path(path)
    temp = path.with_name(path.name + '.tmp-' + uuid.uuid4().hex)
    with temp.open('x', encoding='utf-8') as output:
        temp.chmod(0o600)
        json.dump(value, output, ensure_ascii=False, indent=2, sort_keys=True)
        output.write('\n'); output.flush(); os.fsync(output.fileno())
    os.replace(temp, path)


def load_settings(path):
    path = Path(path).absolute(); _private(path)
    _private(path.parent)
    try:
        cfg = json.loads(path.read_text(encoding='utf-8-sig')); db = cfg['database']
        for key in ['name', 'user']:
            if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]{0,62}', db[key]):
                raise ValueError()
        if db['user'].lower() in {'postgres', 'root', 'rdsadmin', 'azure_pg_admin', 'cloudsqladmin'}:
            raise ValueError()
        if not isinstance(db['password'], str) or not db['password'] or not 1 <= int(db['port']) <= 65535:
            raise ValueError()
        host = db['host']
        if not isinstance(host, str) or not re.fullmatch(r'[A-Za-z0-9_.:\-]+', host) or host.startswith('-'):
            raise ValueError()
        try:
            local = ipaddress.ip_address(host).is_loopback
        except ValueError:
            local = host.lower() == 'localhost'
        if db.get('sslmode') not in {'disable', 'verify-full'} or (not local and db['sslmode'] != 'verify-full'):
            raise ValueError()
        if db['sslmode'] == 'verify-full' and not Path(db.get('sslrootcert', '')).is_file():
            raise ValueError()
        for key in ['uploads', 'recovery_material', 'audit_fallback']:
            p = Path(cfg[key])
            if not p.is_absolute() or p.is_symlink():
                raise ValueError()
            cfg[key] = p
        _check_layout(cfg)
        cfg['_settings_path'] = path
        return cfg
    except (KeyError, TypeError, ValueError, OSError):
        raise OperationError('SGT_OP_CONFIG_INVALID: database/uploads/recovery_material/audit_fallback') from None


def _check_layout(cfg):
    paths = [Path(cfg[k]).resolve() for k in ['uploads', 'recovery_material', 'audit_fallback']]
    if any(a.is_relative_to(b) or b.is_relative_to(a) for i, a in enumerate(paths) for b in paths[i+1:]):
        raise OperationError('SGT_OP_PRIVATE_RECOVERY_AND_AUDIT_MUST_BE_SEPARATE_FROM_UPLOADS')


def _relative(name):
    p = PurePosixPath(name)
    if not name or '\\' in name or ':' in name or p.is_absolute() or any(v in {'.', '..'} for v in p.parts) or str(p) != name:
        raise OperationError('SGT_OP_UNSAFE_RELATIVE_PATH')
    return p


def tree_files(root):
    root = Path(root)
    if not root.is_dir() or root.is_symlink():
        raise OperationError('SGT_OP_TREE_INVALID')
    files = {}
    for p in sorted(root.rglob('*')):
        if p.is_symlink() or (not p.is_file() and not p.is_dir()):
            raise OperationError('SGT_OP_SYMLINK_OR_SPECIAL_FILE_REFUSED')
        if p.is_file():
            relative = str(p.relative_to(root).as_posix()); _relative(relative)
            files[relative] = {'sha256': file_hash(p), 'bytes': p.stat().st_size}
    return files


def _copy_tree(source, target):
    _new_private_dir(target)
    for name in tree_files(source):
        p = Path(target) / name; p.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        with (Path(source) / name).open('rb') as source_file, p.open('xb') as destination:
            p.chmod(0o600); shutil.copyfileobj(source_file, destination, 1024 * 1024)
            destination.flush(); os.fsync(destination.fileno())


class Database:
    def __init__(self, cfg):
        self.cfg = cfg; self.db = cfg['database']

    def _tool(self, name):
        suffix = '.exe' if os.name == 'nt' else ''
        return str(Path(self.cfg['pg_bin']) / (name + suffix)) if self.cfg.get('pg_bin') else name + suffix

    def _env(self, readonly=False):
        env = {k: os.environ[k] for k in ['PATH', 'HOME', 'SystemRoot', 'TEMP', 'TMP', 'LANG'] if k in os.environ}
        env.update(PGPASSWORD=self.db['password'], PGCONNECT_TIMEOUT='5', PGSSLMODE=self.db['sslmode'],
                   PGAPPNAME='sgt-explicit-operations', PGOPTIONS='-c statement_timeout=60000 -c lock_timeout=5000 -c timezone=UTC' + (' -c default_transaction_read_only=on' if readonly else ''))
        if self.db.get('sslrootcert'): env['PGSSLROOTCERT'] = str(self.db['sslrootcert'])
        return env

    def _connection(self):
        return ['--host', self.db['host'], '--port', str(self.db['port']), '--username', self.db['user'], '--dbname', self.db['name']]

    def _run(self, name, args, text=None, readonly=False):
        r = subprocess.run([self._tool(name), *args], input=text, capture_output=True, text=True,
                           env=self._env(readonly), timeout=180)
        if r.returncode:
            raise OperationError('SGT_OP_DATABASE_COMMAND_FAILED: ' + name)
        return r.stdout

    def query(self, sql):
        value = self._run('psql', ['-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1', *self._connection()], sql, readonly=True)
        try: return json.loads(value)
        except ValueError: raise OperationError('SGT_OP_DATABASE_RESULT_INVALID') from None

    def identity(self):
        d = self.query("""SELECT json_build_object('version',current_setting('server_version_num')::int,
          'database',current_database(),'database_oid',d.oid,'current_user',current_user,
          'server_address',inet_server_addr()::text,'server_port',inet_server_port(),
          'owned',d.datdba=r.oid,'limited',NOT(r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls),
          'no_memberships',NOT EXISTS(SELECT 1 FROM pg_auth_members WHERE member=r.oid),
          'all_relations_owned',NOT EXISTS(SELECT 1 FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname='public' AND t.relkind IN ('r','p','S','v','m','f') AND t.relowner<>r.oid),
          'no_other_user_schemas',NOT EXISTS(SELECT 1 FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') AND t.relkind IN ('r','p','S','v','m','f')))
          FROM pg_database d JOIN pg_roles r ON r.rolname=current_user WHERE d.datname=current_database();""")
        if d['version'] != 180006 or d['database'] != self.db['name'] or d['current_user'] != self.db['user'] or not all(d[k] for k in ['owned', 'limited', 'no_memberships', 'all_relations_owned', 'no_other_user_schemas']):
            raise OperationError('SGT_OP_DATABASE_IDENTITY_REFUSED')
        for tool in ['psql', 'pg_dump', 'pg_restore']:
            if not re.search(r'\b18\.6(?:\s|$)', self._run(tool, ['--version'])):
                raise OperationError('SGT_OP_POSTGRES_CLIENT_186_REQUIRED')
        return d

    def tables(self):
        names = self.query("SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') FROM pg_tables WHERE schemaname='public';")
        if not names or not all(re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', v) for v in names):
            raise OperationError('SGT_OP_JPA_SCHEMA_REQUIRED')
        return names

    def idle(self):
        count = self.query("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() AND backend_type='client backend';")
        if count: raise OperationError('SGT_OP_APPLICATION_CONNECTIONS_STILL_ACTIVE')

    def schema(self):
        return self.query("""SELECT json_build_object(
          'columns',(SELECT coalesce(json_agg(json_build_object('table',c.relname,'column',a.attname,'type',format_type(a.atttypid,a.atttypmod),'notnull',a.attnotnull,'identity',a.attidentity,'generated',a.attgenerated,'default',pg_get_expr(d.adbin,d.adrelid)) ORDER BY c.relname,a.attname),'[]') FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_attribute a ON a.attrelid=c.oid LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum WHERE n.nspname='public' AND c.relkind IN ('r','p') AND a.attnum>0 AND NOT a.attisdropped),
          'constraints',(SELECT coalesce(json_agg(json_build_object('table',c.relname,'name',k.conname,'definition',pg_get_constraintdef(k.oid,true),'validated',k.convalidated) ORDER BY c.relname,k.conname),'[]') FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'),
          'indexes',(SELECT coalesce(json_agg(json_build_object('table',tablename,'name',indexname,'definition',indexdef) ORDER BY tablename,indexname),'[]') FROM pg_indexes WHERE schemaname='public'));""")

    def fingerprint(self):
        tables = {}
        for name in self.tables():
            tables[name] = self.query(f"SELECT json_build_object('rows',count(*),'sha256',encode(sha256(convert_to(coalesce(string_agg(h,'' ORDER BY h),''),'UTF8')),'hex')) FROM (SELECT encode(sha256(convert_to(to_jsonb(t)::text,'UTF8')),'hex') h FROM public.\"{name}\" t) hashes;")
        sequences = {}
        for name in self.query("SELECT coalesce(json_agg(sequencename ORDER BY sequencename),'[]') FROM pg_sequences WHERE schemaname='public';"):
            if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', name): raise OperationError('SGT_OP_SEQUENCE_NAME_INVALID')
            sequences[name] = self.query(f'SELECT row_to_json(s) FROM (SELECT last_value,is_called FROM public."{name}") s;')
        return {'tables': tables, 'sequences': sequences}

    @contextmanager
    def hold_read_lock(self):
        tables = ','.join('public."' + n + '"' for n in self.tables())
        p = subprocess.Popen([self._tool('psql'), '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1', *self._connection()],
                             stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env=self._env())
        try:
            p.stdin.write("BEGIN ISOLATION LEVEL REPEATABLE READ; LOCK TABLE " + tables + " IN SHARE MODE; SELECT 'SGT_BACKUP_LOCKED';\n");p.stdin.flush()
            observed = queue.Queue();threading.Thread(target=lambda: observed.put(p.stdout.readline()), daemon=True).start()
            if observed.get(timeout=15).strip() != 'SGT_BACKUP_LOCKED': raise OperationError('SGT_OP_BACKUP_LOCK_REFUSED')
            yield
            p.stdin.write('COMMIT;\n');p.stdin.flush();p.stdin.close();p.wait(timeout=10)
            if p.returncode: raise OperationError('SGT_OP_BACKUP_LOCK_FAILED')
        except queue.Empty:
            raise OperationError('SGT_OP_BACKUP_LOCK_TIMEOUT') from None
        finally:
            if p.poll() is None: p.terminate();p.wait(timeout=10)

    def dump(self, path):
        self._run('pg_dump', [*self._connection(), '--format=custom', '--data-only', '--no-owner', '--no-privileges', '--file', str(path)])

    def restore_data(self, path):
        # No --clean, --create, --disable-triggers or schema restoration.
        self._run('pg_restore', [*self._connection(), '--single-transaction', '--data-only', '--no-owner', '--no-privileges', '--exit-on-error', str(path)])


def backup(cfg, output, *, application_stopped=False):
    if not application_stopped: raise OperationError('SGT_OP_STOP_APPLICATION_FIRST')
    _check_layout(cfg)
    output = Path(output).absolute()
    roots = [cfg['uploads'], cfg['recovery_material'], cfg['audit_fallback']]
    if output.exists() or any(output.resolve().is_relative_to(Path(p).resolve()) or Path(p).resolve().is_relative_to(output.resolve()) for p in roots):
        raise OperationError('SGT_OP_NEW_SEPARATE_BACKUP_DIRECTORY_REQUIRED')
    _private(cfg['recovery_material'])
    for name in tree_files(cfg['recovery_material']): _private(cfg['recovery_material'] / name)
    before_files = {'uploads': tree_files(cfg['uploads']), 'recovery': tree_files(cfg['recovery_material'])}
    audit = cfg['audit_fallback'];before_audit = file_hash(audit) if audit.exists() else None
    if audit.exists(): _private(audit)
    db = Database(cfg);identity = db.identity();db.idle();_new_private_dir(output)
    _write_json(output / 'operation-state.json', {'phase': 'BACKUP_STARTED', 'application_stopped_asserted': True})
    with db.hold_read_lock():
        schema = db.schema();before = db.fingerprint()
        dump = output / 'database.dump';dump.touch(mode=0o600, exist_ok=False);db.dump(dump);dump.chmod(0o600)
        _copy_tree(cfg['uploads'], output / 'uploads');_copy_tree(cfg['recovery_material'], output / 'recovery')
        if before_audit is not None:
            _new_private_dir(output / 'audit');shutil.copyfile(audit, output / 'audit/fallback.jsonl');(output / 'audit/fallback.jsonl').chmod(0o600)
        if db.fingerprint() != before or db.schema() != schema or tree_files(cfg['uploads']) != before_files['uploads'] or tree_files(cfg['recovery_material']) != before_files['recovery'] or (file_hash(audit) if audit.exists() else None) != before_audit:
            raise OperationError('SGT_OP_SOURCE_CHANGED_DURING_BACKUP')
    files = tree_files(output);files.pop('operation-state.json')
    manifest = {'format': 1, 'state': 'VERIFIED_LOCAL_BACKUP', 'created_utc': datetime.now(timezone.utc).isoformat(), 'identity': identity,
                'schema': schema, 'fingerprint': before, 'files': files, 'application_stopped': True,
                'scope': 'Data-only PostgreSQL plus durable uploads, audit fallback and explicitly supplied private recovery material; no cache or production migration.'}
    _write_json(output / 'manifest.json', manifest)
    _write_json(output / 'operation-state.json', {'phase': 'BACKUP_VERIFIED', 'manifest_sha256': file_hash(output / 'manifest.json')})
    return {'verified': True, 'manifest_sha256': file_hash(output / 'manifest.json'), 'files': len(files), 'tables': len(before['tables']), 'external_copy_verified': False}


def verify_bundle(path, expected_manifest_sha256):
    path = Path(path);_private(path)
    if not re.fullmatch(r'[a-f0-9]{64}', expected_manifest_sha256) or file_hash(path / 'manifest.json') != expected_manifest_sha256:
        raise OperationError('SGT_OP_MANIFEST_HASH_MISMATCH')
    manifest = json.loads((path / 'manifest.json').read_text(encoding='utf-8'))
    if manifest.get('format') != 1 or manifest.get('state') != 'VERIFIED_LOCAL_BACKUP' or 'database.dump' not in manifest.get('files', {}):
        raise OperationError('SGT_OP_BACKUP_FORMAT_INVALID')
    for name in manifest['files']:
        _relative(name)
        if name != 'database.dump' and not name.startswith(('uploads/', 'recovery/', 'audit/')):
            raise OperationError('SGT_OP_BACKUP_FILE_SCOPE_INVALID')
    actual = tree_files(path)
    if set(actual) - {'manifest.json', 'operation-state.json'} != set(manifest['files']):
        raise OperationError('SGT_OP_BACKUP_FILE_SET_MISMATCH')
    if any(actual.get(name) != info for name, info in manifest['files'].items()):
        raise OperationError('SGT_OP_BACKUP_CONTENT_HASH_MISMATCH')
    return manifest


def _publish_tree(source, target):
    if target.exists():
        if tree_files(target) != tree_files(source): raise OperationError('SGT_OP_TARGET_FILES_ALREADY_EXIST')
        return
    stage = target.with_name('.sgt-restore-' + uuid.uuid4().hex)
    _copy_tree(source, stage)
    if tree_files(stage) != tree_files(source): raise OperationError('SGT_OP_STAGED_FILES_MISMATCH')
    if target.exists(): raise OperationError('SGT_OP_TARGET_FILES_ALREADY_EXIST')
    os.rename(stage, target)


def _publish_file(source, target):
    if target.exists():
        if target.is_symlink() or file_hash(target) != file_hash(source): raise OperationError('SGT_OP_TARGET_FILE_ALREADY_EXISTS')
        return
    target.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    _private(target.parent)
    temp = target.with_name('.sgt-restore-' + uuid.uuid4().hex)
    with Path(source).open('rb') as src, temp.open('xb') as dest:
        temp.chmod(0o600);shutil.copyfileobj(src, dest);dest.flush();os.fsync(dest.fileno())
    os.link(temp, target);temp.unlink()


def restore(cfg, path, expected_manifest_sha256, *, application_stopped=False, resume_files_only=False):
    if not application_stopped: raise OperationError('SGT_OP_STOP_APPLICATION_FIRST')
    _check_layout(cfg)
    path = Path(path);manifest = verify_bundle(path, expected_manifest_sha256)
    db = Database(cfg);identity = db.identity()
    key = lambda d: (d['server_address'], d['server_port'], d['database_oid'])
    if key(identity) == key(manifest['identity']): raise OperationError('SGT_OP_RESTORE_SOURCE_DATABASE_REFUSED')
    db.idle()
    if db.schema() != manifest['schema']: raise OperationError('SGT_OP_JPA_SCHEMA_MISMATCH')
    before = db.fingerprint()
    # Durable, private phase marker survives a crash. Resume never repeats SQL
    # once the operator selects --resume-files-only and the data exactly match.
    state_path = cfg.get('_settings_path')
    if state_path is not None:
        state_path = Path(state_path).with_name('restore-state-' + uuid.uuid4().hex + '.json')
        _write_json(state_path, {'phase': 'BEFORE_DATA_RESTORE', 'manifest_sha256': expected_manifest_sha256,
                                'target_database': identity['database'], 'resume_files_only': resume_files_only})
    if not resume_files_only:
        if any(t['rows'] for t in before['tables'].values()): raise OperationError('SGT_OP_RESTORE_TARGET_NOT_EMPTY')
        if any(p.exists() for p in [cfg['uploads'], cfg['recovery_material'], cfg['audit_fallback']]):
            raise OperationError('SGT_OP_NEW_RESTORE_PATHS_REQUIRED')
        db.restore_data(path / 'database.dump')
        if db.fingerprint() != manifest['fingerprint']: raise OperationError('SGT_OP_RESTORED_DATABASE_MISMATCH')
    elif before != manifest['fingerprint']:
        raise OperationError('SGT_OP_RESUME_REQUIRES_EXACT_RESTORED_DATA')
    if state_path is not None:
        _write_json(state_path, {'phase': 'DATA_VERIFIED_FILES_PENDING', 'manifest_sha256': expected_manifest_sha256})
    _publish_tree(path / 'uploads', cfg['uploads']);_publish_tree(path / 'recovery', cfg['recovery_material'])
    if (path / 'audit/fallback.jsonl').exists(): _publish_file(path / 'audit/fallback.jsonl', cfg['audit_fallback'])
    if state_path is not None:
        _write_json(state_path, {'phase': 'DATA_AND_FILES_VERIFIED', 'manifest_sha256': expected_manifest_sha256})
    return {'verified': True, 'manifest_sha256': expected_manifest_sha256, 'tables': len(manifest['fingerprint']['tables']),
            'data_replayed': not resume_files_only, 'restored_files_verified': True, 'schema_restored_by_dump': False,
            'constraints_disabled': False, 'source_untouched': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['verificar', 'backup', 'restaurar'])
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--backup', type=Path)
    parser.add_argument('--manifest-sha256')
    parser.add_argument('--application-stopped', action='store_true')
    parser.add_argument('--resume-files-only', action='store_true')
    args = parser.parse_args()
    try:
        cfg = load_settings(args.config)
        if args.command == 'verificar':
            db = Database(cfg);identity = db.identity();result = {'verified': True, 'database': identity['database'], 'version': identity['version'], 'limited_role_and_ownership': True, 'tables': len(db.tables())}
        elif args.command == 'backup':
            if args.output is None: parser.error('--output é obrigatório')
            result = backup(cfg, args.output, application_stopped=args.application_stopped)
        else:
            if args.backup is None or args.manifest_sha256 is None: parser.error('--backup e --manifest-sha256 são obrigatórios')
            result = restore(cfg, args.backup, args.manifest_sha256, application_stopped=args.application_stopped, resume_files_only=args.resume_files_only)
        print(json.dumps(result, ensure_ascii=False));return 0
    except OperationError as error:
        print(str(error));return 2
    except (OSError, ValueError, subprocess.SubprocessError):
        print('SGT_OP_FAILED: operacao incompleta; preserve os arquivos e confira a configuracao privada');return 2


if __name__ == '__main__':
    raise SystemExit(main())
