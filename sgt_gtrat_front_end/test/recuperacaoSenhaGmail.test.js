import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), 'utf8');
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), 'utf8');

test('recuperação mantém contrato HTTP entre React e Java e confirmação envia apenas o token', async () => {
  assert.ok(backend, 'SGT_BACKEND_DIR é obrigatório para validar recuperação de senha');
  const [servicoReact, controlador, dto] = await Promise.all([
    lerReact('src/services/auth/ServicoAutenticacao.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/AutenticacaoControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/dto/ConfirmacaoRecuperacaoSenhaDTO.java'),
  ]);

  assert.match(servicoReact, /url:\s*["']\/autenticacao\/recuperar-senha\/solicitar["']/);
  assert.match(servicoReact, /url:\s*["']\/autenticacao\/recuperar-senha\/confirmar["']/);
  assert.match(servicoReact, /data:\s*\{\s*token:\s*chave\s*\}/);
  assert.doesNotMatch(servicoReact, /data:\s*\{\s*token,\s*newPassword\s*\}/);

  assert.match(controlador, /@PostMapping\(["']\/recuperar-senha\/solicitar["']\)/);
  assert.match(controlador, /@PostMapping\(["']\/recuperar-senha\/confirmar["']\)/);
  assert.match(controlador, /recuperacaoSenhaServico\.confirmar\(payload\.token\(\)\)/);
  assert.doesNotMatch(dto, /newPassword/);
});

test('integração de e-mail usa Gmail SMTP somente no backend', async () => {
  const [emailJava, properties, envJava, envReact, envExampleReact] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/EmailGmailServico.java'),
    lerBackend('src/main/resources/application.properties'),
    lerBackend('.env'),
    lerReact('.env.example'),
    lerReact('.env.example'),
  ]);

  assert.match(emailJava, /JavaMailSender/);
  assert.match(emailJava, /MimeMessageHelper/);
  assert.match(emailJava, /addInline\(CID_LOGO_CORREIOS/);
  assert.match(properties, /GMAIL_SMTP_HOST/);
  assert.match(properties, /GMAIL_SMTP_USERNAME/);
  assert.match(properties, /GMAIL_SMTP_APP_PASSWORD/);
  assert.match(properties, /mail\.smtp\.starttls\.enable/);
  assert.match(envJava, /GMAIL_SMTP_APP_PASSWORD=/);
  assert.doesNotMatch(properties, /MICROSOFT_GRAPH_CLIENT_SECRET/);
  assert.doesNotMatch(envReact, /GMAIL_SMTP_(?:USERNAME|APP_PASSWORD)/);
  assert.doesNotMatch(envExampleReact, /GMAIL_SMTP_(?:USERNAME|APP_PASSWORD)/);
});

test('clique do link gera senha temporária, força troca no login e revoga sessões antigas', async () => {
  const [servico, entidade, login, formulario] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/RecuperacaoSenhaServico.java'),
    lerBackend('src/main/java/com/example/siggtrat/entidade/RecuperacaoSenhaToken.java'),
    lerReact('src/modules/auth/pages/Login/index.jsx'),
    lerReact('src/modules/auth/pages/Login/components/FormularioRecuperacaoSenha.jsx'),
  ]);

  assert.match(entidade, /token_hash/);
  assert.doesNotMatch(entidade, /private\s+String\s+tokenBruto/);
  assert.match(servico, /sha256Hexadecimal\(tokenBruto/);
  assert.match(servico, /geradorSenhaTemporaria\.gerar\(\)/);
  assert.match(servico, /setForceChangePassword\(true\)/);
  assert.match(servico, /setUsedAt\(agora\)/);
  assert.match(servico, /tokenAtivoRepositorio\.deleteByUsername/);
  assert.match(servico, /removerSessoesUsuarioDoCache/);
  assert.match(servico, /Sua senha temporária/);
  assert.match(servico, /cid:/);

  assert.match(login, /localizacao\.pathname\s*!==\s*["']\/recuperar-senha["']/);
  assert.match(login, /ServicoAutenticacao\.validarTokenRecuperacaoSenha\(token\)/);
  assert.match(login, /ServicoAutenticacao\.confirmarRecuperacaoSenha\(tokenRecuperacao\)/);
  assert.match(login, /setRecuperacaoConfirmada\(true\)/);
  assert.doesNotMatch(login, /senhaRecuperacao/);
  assert.match(formulario, /Confirmar recuperação e enviar senha temporária/);
  assert.match(formulario, /Senha temporária enviada/);
  assert.match(formulario, /primeiro login/);
  assert.doesNotMatch(formulario, /Definir nova senha/);
});

test('solicitação continua genérica para impedir enumeração de contas', async () => {
  const [servico, formulario] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/RecuperacaoSenhaServico.java'),
    lerReact('src/modules/auth/pages/Login/components/FormularioRecuperacaoSenha.jsx'),
  ]);

  assert.match(servico, /Se o e-mail estiver vinculado a uma conta ativa/);
  assert.match(formulario, /Enviar link de recuperação/);
  assert.doesNotMatch(formulario, /Outlook/);
});
