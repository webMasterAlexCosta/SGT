import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const backend=process.env.SGT_BACKEND_DIR;
const lerBackend=(p)=>readFile(resolve(backend,p),'utf8');
const lerReact=(p)=>readFile(new URL(`../${p}`,import.meta.url),'utf8');

test('Etapa 10: controladores Java somam 19 endpoints e React cobre os três módulos', async()=>{
  assert.ok(backend);
  const [contrato,corr,doc,react]=await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/controlador/ContratoControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/CorrecaoIdentificadorContratoControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/DocumentoComprobatorioControlador.java'),
    lerReact('src/services/admin/ServicoExcecoesContrato.js'),
  ]);
  for(const trecho of ['/transferencias-gestao','/transferir-gestao','/corrigir-anulacao','/correcoes-identificador','/historico-identificador','/documentos','/retificar-sei','/substituir','/invalidar','/permissoes','/acessos']) assert.match(react,new RegExp(trecho.replaceAll('/','\\/')));
  const n=(x)=>(x.match(/@(Get|Post|Put|Delete)Mapping/g)||[]).length;
  assert.ok(n(contrato)>=3); assert.equal(n(corr),3); assert.equal(n(doc),13);
});

test('Etapa 10: multipart usa metadados + arquivo opcional e Java valida 413/415 por configuração/assinatura', async()=>{
  const [react,ctrl,serv]=await Promise.all([lerReact('src/services/admin/ServicoExcecoesContrato.js'),lerBackend('src/main/java/com/example/siggtrat/controlador/DocumentoComprobatorioControlador.java'),lerBackend('src/main/java/com/example/siggtrat/servico/DocumentoComprobatorioServico.java')]);
  assert.match(react,/form\.append\('metadados'/); assert.match(react,/form\.append\('arquivo'/);
  assert.match(ctrl,/@RequestPart\("metadados"\)/); assert.match(ctrl,/@RequestPart\(value = "arquivo", required = false\)/);
  assert.match(serv,/PAYLOAD_TOO_LARGE/); assert.match(serv,/UNSUPPORTED_MEDIA_TYPE/); assert.match(serv,/validarAssinatura/);
});

test('Etapa 10: PESSOAL_RESTRITO exige finalidade e Operacional é bloqueado pelo serviço', async()=>{
  const [serv,painel]=await Promise.all([lerBackend('src/main/java/com/example/siggtrat/servico/DocumentoComprobatorioServico.java'),lerReact('src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx')]);
  assert.match(serv,/PESSOAL_RESTRITO/); assert.match(serv,/exige finalidade declarada/); assert.match(serv,/Usuário Operacional não acessa documentos/);
  assert.match(painel,/eUsuarioOperacional/); assert.match(painel,/Finalidade para PESSOAL_RESTRITO/);
});

test('Etapa 10: concessão/revogação restrita é do Administrador e escrita documental do Gestor Local responsável', async()=>{
  const [ctrl,serv,painel]=await Promise.all([lerBackend('src/main/java/com/example/siggtrat/controlador/DocumentoComprobatorioControlador.java'),lerBackend('src/main/java/com/example/siggtrat/servico/DocumentoComprobatorioServico.java'),lerReact('src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx')]);
  assert.match(serv,/Somente o Gestor Local responsável pode registrar ou versionar documentos negociais/); assert.match(serv,/Concessão excepcional de acesso restrito exige administração técnica/);
  assert.match(painel,/administrador&&/); assert.match(ctrl,/\/permissoes/);
});

test('Etapa 10: transferência é atômica e exige autorização excepcional + cobertura completa', async()=>{
  const [serv,painel]=await Promise.all([lerBackend('src/main/java/com/example/siggtrat/servico/TransferenciaGestaoContratoServico.java'),lerReact('src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx')]);
  assert.match(serv,/TRANSFERENCIA_GESTAO_LOCAL/); assert.match(serv,/validarCoberturaDoContexto/); assert.match(serv,/validarCoberturaDasAlocacoesPosteriores/); assert.match(serv,/autorização informada não corresponde/i);
  assert.match(painel,/operação é atômica/i); assert.match(painel,/mapeamentos/i);
});

test('Etapa 10: correção de identificador preserva antigo pesquisável e idempotência', async()=>{
  const [ctrl,req,serv,react]=await Promise.all([lerBackend('src/main/java/com/example/siggtrat/controlador/CorrecaoIdentificadorContratoControlador.java'),lerBackend('src/main/java/com/example/siggtrat/dto/CorrecaoIdentificadorContratoRequisicao.java'),lerBackend('src/main/java/com/example/siggtrat/servico/CorrecaoIdentificadorContratoServico.java'),lerReact('src/services/admin/ServicoExcecoesContrato.js')]);
  assert.match(ctrl,/historico-identificador/); assert.match(req,/chaveIdempotencia/); assert.match(serv,/findByChaveIdempotencia/); assert.match(react,/gerarChaveIdempotencia\('corr-id'\)/);
});

test('Etapa 10: correção de anulação cria CANCELA sem reativação silenciosa', async()=>{
  const [serv,dominio,painel]=await Promise.all([lerBackend('src/main/java/com/example/siggtrat/servico/ContratoServico.java'),lerReact('src/services/admin/ServicoDominioExcecoesContrato.js'),lerReact('src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx')]);
  assert.match(serv,/relacaoPrecedencia\(\) != TipoRelacaoPrecedenciaNegocial\.CANCELA/); assert.match(serv,/contrato\.setAtivo\(false\)/); assert.match(dominio,/relacaoPrecedencia:\s*'CANCELA'/); assert.match(painel,/continuará administrativamente inativo/i);
});
