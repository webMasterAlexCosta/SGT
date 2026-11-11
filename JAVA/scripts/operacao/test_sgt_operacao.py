"""Isolated negative controls; the PostgreSQL restore is exercised separately on the real lab."""
import hashlib,json,os,tempfile,unittest
from pathlib import Path
from unittest.mock import MagicMock,patch
import sgt_operacao as op

class OperacaoTest(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
  self.upload=self.root/'uploads';self.upload.mkdir();(self.upload/'doc.bin').write_bytes(b'documento-sintetico')
  self.keys=self.root/'keys';self.keys.mkdir(mode=0o700);(self.keys/'keys.properties').write_text('AUDIT_SECRET=VALOR_EXPLICITAMENTE_SINTETICO\n');(self.keys/'keys.properties').chmod(0o600)
  self.audit=self.root/'fallback.jsonl';self.audit.write_bytes(b'{"sintetico":true}\n');self.audit.chmod(0o600)
  self.cfg={'database':{'host':'127.0.0.1','port':55432,'name':'sgt_sintetico','user':'sgt_app','password':'SENHA_SINTETICA_TESTE','sslmode':'disable'},'uploads':str(self.upload),'recovery_material':str(self.keys),'audit_fallback':str(self.audit)}
  self.settings=self.root/'private.json';self.settings.write_text(json.dumps(self.cfg));self.settings.chmod(0o600)
  self.fp={'tables':{'t':{'rows':2,'sha256':'a'*64}},'sequences':{'t_seq':{'last_value':2,'is_called':True}}}
  self.schema={'columns':[{'table':'t','name':'id','type':'bigint'}],'constraints':[],'indexes':[]}
  self.identity={'database':'sgt_sintetico','database_oid':41,'server_address':'127.0.0.1','server_port':55432}

 def fake_db(self,target=False):
  d=MagicMock();d.identity.return_value={**self.identity,**({'database':'sgt_destino','database_oid':42} if target else {})}
  d.schema.return_value=self.schema;d.fingerprint.return_value=self.fp
  d.dump.side_effect=lambda path:Path(path).write_bytes(b'DUMP_SINTETICO_NAO_EXECUTAVEL')
  return d

 def make_backup(self):
  db=self.fake_db();out=self.root/'backup'
  with patch.object(op,'Database',return_value=db):result=op.backup(op.load_settings(self.settings),out,application_stopped=True)
  return out,result

 def target(self):
  cfg=op.load_settings(self.settings);cfg['database']={**cfg['database'],'name':'sgt_destino'}
  cfg['uploads']=self.root/'restored/uploads';cfg['recovery_material']=self.root/'restored/keys';cfg['audit_fallback']=self.root/'restored/fallback.jsonl'
  return cfg

 def test_requer_configuracao_privada_explicita(self):
  self.settings.chmod(0o644)
  if os.name!='nt':
   with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_recusa_role_administrativa_antes_de_conectar(self):
  for role in ['postgres','root','rdsadmin']:
   self.cfg['database']['user']=role;self.settings.write_text(json.dumps(self.cfg))
   with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_recusa_injecao_em_nome_de_banco(self):
  self.cfg['database']['name']='sgt; DROP DATABASE outro';self.settings.write_text(json.dumps(self.cfg))
  with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_endpoint_remoto_exige_tls_verificado(self):
  self.cfg['database']['host']='db.example.invalid';self.settings.write_text(json.dumps(self.cfg))
  with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_backup_exige_aplicacao_parada_sem_criar_destino(self):
  out=self.root/'backup'
  with patch.object(op,'Database') as db:
   with self.assertRaises(op.OperationError):op.backup(op.load_settings(self.settings),out)
   db.assert_not_called();self.assertFalse(out.exists())

 def test_backup_nao_sobrescreve_diretorio_existente(self):
  out=self.root/'backup';out.mkdir();sentinel=out/'preservar';sentinel.write_bytes(b'original')
  with self.assertRaises(op.OperationError):op.backup(op.load_settings(self.settings),out,application_stopped=True)
  self.assertEqual(sentinel.read_bytes(),b'original')

 def test_backup_recusa_saida_dentro_dos_uploads(self):
  with self.assertRaises(op.OperationError):op.backup(op.load_settings(self.settings),self.upload/'backup',application_stopped=True)

 def test_nao_admite_material_privado_dentro_dos_uploads(self):
  self.cfg['recovery_material']=str(self.upload/'chaves');self.settings.write_text(json.dumps(self.cfg))
  with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_nao_admite_fallback_de_auditoria_dentro_dos_uploads(self):
  self.cfg['audit_fallback']=str(self.upload/'audit.jsonl');self.settings.write_text(json.dumps(self.cfg))
  with self.assertRaises(op.OperationError):op.load_settings(self.settings)

 def test_arvore_recusa_symlink(self):
  if os.name=='nt':self.skipTest('Symlink permission differs on Windows; not a native Windows execution claim')
  (self.upload/'link').symlink_to(self.audit)
  with self.assertRaises(op.OperationError):op.tree_files(self.upload)

 def test_backup_confere_arquivos_chaves_e_fingerprint(self):
  out,result=self.make_backup();m=op.verify_bundle(out,result['manifest_sha256'])
  self.assertEqual(m['fingerprint'],self.fp);self.assertEqual(m['schema'],self.schema)
  self.assertEqual((out/'uploads/doc.bin').read_bytes(),b'documento-sintetico')
  self.assertIn('recovery/keys.properties',m['files'])
  self.assertNotIn('SENHA_SINTETICA_TESTE',json.dumps(m))

 def test_manifesto_adulterado_recusado_antes_do_banco(self):
  out,result=self.make_backup();(out/'manifest.json').write_text('{}')
  with patch.object(op,'Database') as db:
   with self.assertRaises(op.OperationError):op.restore(self.target(),out,result['manifest_sha256'],application_stopped=True)
   db.assert_not_called()

 def test_dump_adulterado_recusado_antes_do_banco(self):
  out,result=self.make_backup();(out/'database.dump').write_bytes(b'alterado')
  with patch.object(op,'Database') as db:
   with self.assertRaises(op.OperationError):op.restore(self.target(),out,result['manifest_sha256'],application_stopped=True)
   db.assert_not_called()

 def test_arquivo_extra_recusado(self):
  out,result=self.make_backup();(out/'uploads/estranho.bin').write_bytes(b'x')
  with self.assertRaises(op.OperationError):op.verify_bundle(out,result['manifest_sha256'])

 def test_mudanca_na_fonte_invalida_backup_sem_tocar_no_original(self):
  db=self.fake_db();db.fingerprint.side_effect=[self.fp,{'tables':{},'sequences':{}}];out=self.root/'changed-backup'
  before=(self.upload/'doc.bin').read_bytes()
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.backup(op.load_settings(self.settings),out,application_stopped=True)
  self.assertFalse((out/'manifest.json').exists());self.assertEqual((self.upload/'doc.bin').read_bytes(),before)

 def test_caminho_traversal_no_manifesto_recusado(self):
  out,result=self.make_backup();p=out/'manifest.json';d=json.loads(p.read_text());d['files']['uploads/../fora']={'sha256':'a'*64,'bytes':0};p.write_text(json.dumps(d))
  with self.assertRaises(op.OperationError):op.verify_bundle(out,op.file_hash(p))

 def test_restore_nao_aceita_o_banco_fonte(self):
  out,result=self.make_backup();db=self.fake_db()
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.restore(self.target(),out,result['manifest_sha256'],application_stopped=True)
   db.restore_data.assert_not_called()

 def test_restore_recusa_qualquer_linha_preexistente(self):
  out,result=self.make_backup();db=self.fake_db(target=True)
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.restore(self.target(),out,result['manifest_sha256'],application_stopped=True)
   db.restore_data.assert_not_called()

 def test_restore_recusa_schema_divergente(self):
  out,result=self.make_backup();db=self.fake_db(target=True);db.schema.return_value={'columns':[],'constraints':[],'indexes':[]}
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.restore(self.target(),out,result['manifest_sha256'],application_stopped=True)
   db.restore_data.assert_not_called()

 def test_restore_publica_os_mesmos_arquivos_apos_conferir_dados(self):
  out,result=self.make_backup();db=self.fake_db(target=True);db.fingerprint.side_effect=[{'tables':{'t':{'rows':0,'sha256':'0'*64}},'sequences':{}},self.fp]
  cfg=self.target()
  with patch.object(op,'Database',return_value=db):r=op.restore(cfg,out,result['manifest_sha256'],application_stopped=True)
  self.assertTrue(r['verified']);self.assertEqual((cfg['uploads']/'doc.bin').read_bytes(),b'documento-sintetico');db.restore_data.assert_called_once()

 def test_retomada_so_de_arquivos_nao_repete_restore_de_dados(self):
  out,result=self.make_backup();db=self.fake_db(target=True);cfg=self.target()
  with patch.object(op,'Database',return_value=db):r=op.restore(cfg,out,result['manifest_sha256'],application_stopped=True,resume_files_only=True)
  self.assertTrue(r['verified']);db.restore_data.assert_not_called()

 def test_restore_grava_estado_recuperavel_antes_de_reproduzir_dados(self):
  out,result=self.make_backup();db=self.fake_db(target=True);cfg=self.target()
  db.fingerprint.return_value={'tables':{'t':{'rows':0,'sha256':'0'*64}},'sequences':{}}
  def interrupted(_):
   states=list(self.root.glob('restore-state-*.json'))
   self.assertEqual(len(states),1)
   state=json.loads(states[0].read_text());self.assertEqual(state['phase'],'BEFORE_DATA_RESTORE')
   raise op.OperationError('interrupcao-sintetica')
  db.restore_data.side_effect=interrupted
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.restore(cfg,out,result['manifest_sha256'],application_stopped=True)

 def test_retomada_recusa_dados_divergentes_e_preserva_arquivo_existente(self):
  out,result=self.make_backup();db=self.fake_db(target=True);db.fingerprint.return_value={'tables':{},'sequences':{}}
  cfg=self.target();cfg['uploads'].mkdir(parents=True);sentinel=cfg['uploads']/'original';sentinel.write_bytes(b'nao-apagar')
  with patch.object(op,'Database',return_value=db):
   with self.assertRaises(op.OperationError):op.restore(cfg,out,result['manifest_sha256'],application_stopped=True,resume_files_only=True)
  self.assertEqual(sentinel.read_bytes(),b'nao-apagar')

if __name__=='__main__':unittest.main()
