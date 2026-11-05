# E2E operacional: JAR integrado, prova zero/falso e IND-08

> Requisito operacional vigente em 2026-11-03: MySQL exatamente 8.0.45. As referências de versões anteriores preservadas em documentação histórica e seus recibos não autorizam outra versão nesta execução.

## Estado desta entrega

Os cenários foram preservados ou adaptados para React `1.4.4-operacional.20261101`
e Java `0.0.4-operacional-20261101`, mas sua execução nesta candidata permanece
**pendente de recibo**. Resultados e bloqueios de navegador das rodadas anteriores
pertencem ao acervo histórico; não são atribuídos automaticamente a esta execução.

Os roteiros usam `ambiente-real.cjs`, Playwright com Google Chrome real e frontend servido pelo
JAR em loopback, sem simular respostas ou inserir dados no DOM. Devem ser executados
em ambiente de navegador autorizado que alcance o runtime sintético.

## Preparação necessária

1. Concluir os gates Java, o build React e a integração do `dist` no novo JAR. Iniciar esse JAR com MySQL **8.0.45** e Redis sintéticos preparados, registrando as versões reais. Não executar login ou mutações concorrentes nas contas usadas pelo browser.
2. Preservar a fixture Java mais recente `target/evolucao-fixtures/ind08-historico-parcial.json` em `runtime/private/ind08-historico-parcial.json` antes de eventual `clean`. Ela contém os IDs das três provas e os quatro cortes K exatos. O teste Java criou lacunas documentais sintéticas e incorporou as provas pela API; o navegador não reproduz essa perda por SQL.
3. Manter `runtime/private/e2e-fixture.json` com `synthetic: true`, os catálogos da fixture existente e os aliases `accounts.gestor_local` e `accounts.ind08_historico`. A conta histórica deve corresponder à matrícula da fixture Java atual. A coordenação preenche sua senha privadamente; nenhum script deste diretório contém ou extrai senha de fonte Java.
4. Definir o caminho do Playwright e do Google Chrome reais já preparados. Credenciais, cookies, estado de sessão e payloads de recuperação permanecem em `runtime/private`.

As variáveis abaixo mostram o contrato de execução; os caminhos concretos do executor podem variar. O label precisa ser novo, de 1 a 40 caracteres `[A-Za-z0-9_-]`.

```bash
export SGT_TEST_RUNTIME=/caminho/sgt_evolucao/runtime
export SGT_BACKEND_URL=http://127.0.0.1:18080
export SGT_FRONTEND_URL=http://127.0.0.1:18080
export SGT_E2E_MODE=jar-integrado
export SGT_E2E_OUTPUT=/caminho/sgt_evolucao/evidencias/browser-evolucao
export SGT_EVOLUCAO_LABEL=candidato-final-01
export PLAYWRIGHT_MODULE=/caminho/playwright
export CHROMIUM_EXECUTABLE=/caminho/google-chrome/chrome
python3 test/e2e/preparar-evolucao-governanca-real.py --label "$SGT_EVOLUCAO_LABEL"
node test/e2e/evolucao-governanca-real.cjs
```

Usar Node22 e o ambiente de bibliotecas nativas já preparado para o Google Chrome. A coordenação pode também definir `SGT_E2E_MANIFEST` para registrar no recibo a referência do manifesto do frontend/JAR usado.

As duas origens são explícitas e não possuem fallback. O preparador usa `SGT_BACKEND_URL`, validada pelo cliente Python; o driver usa `SGT_FRONTEND_URL`. Neste cenário integrado, o estado privado exige que ambas identifiquem o mesmo JAR. Para a cópia de upgrade na porta `18081`, configure ambas com essa origem e use seu runtime/label isolados. Não sobrescreva `api_client.BASE` nem reutilize sessões de outra origem. Copie também `configuracao_http.py` ao preparar os helpers no runtime privado.

O setup cria contrato futuro, composição positiva e composição com zero documentado,
e publica o contrato pelas APIs existentes. Não cria tarifas nem chama endpoints financeiros.
O estado fica em `runtime/private/evolucao-governanca-real-<label>.json` e a evidência pública
contém apenas contagens/identificadores técnicos. Use um novo label em cada execução.

## Cobertura conservada

### Zero e falso

O cenário `zeroFalse` consulta o histórico do contrato e exige zero e falso comprovados
na composição, com identidade real da origem. Essas asserções operacionais foram
extraídas do antigo cenário tarifário, sem alterar seus critérios.

### IND-08

- Compara a descrição desconhecida no K anterior com o valor comprovado em K exato da primeira prova.
- Em K da matrícula, verifica falso comprovado e ausência de prova para dia reduzido.
- Em K da terceira prova, verifica ausência comprovada de dia reduzido, outro campo ainda desconhecido, origem herdada da descrição e ID nulo da projeção.
- Confere que a página envia D/K exatos para posição e composições, preservando nanossegundos/microssegundos nas strings recebidas.
- Abre o recibo da prova e verifica `RESTRITA`, estado factual nulo e origens vazias nos metadados omitidos, além da indicação de acesso restrito na interface.
- Verifica ausência dos marcadores documentais sintéticos restritos no DTO e no DOM.
- Abre a fotografia original da publicação e exige que continue desconhecida no campo incorporado por prova posterior.

## Navegação e evidência

`empresas-contratadas.cjs` verifica a navegação para o cadastro preservado, as
consultas canônicas de empresas e contratos, a paginação e a pesquisa, além da
ausência de requisições e controles da Governança de Terceiros removida. A URL
antiga deve seguir o fallback da aplicação sem montar a funcionalidade anterior.

Os nomes históricos dos arquivos abaixo foram preservados: seus cenários tratam
da prova histórica contratual, de zero/falso comprovados e do recorte D/K, que
continuam vigentes após a retirada do módulo de Governança de Terceiros.
`evolucao-governanca-real.cjs` produz recibos `-zero-falso.json` e `-ind08.json`;
o setup produz `-setup.json`. Senhas, cookies, cabeçalhos e documentos de fixture
permanecem privados. Capturas servem somente à inspeção interna.

As operações usam dados sintéticos e não comprovam fontes administrativas autênticas.
O teste histórico depende da fixture Java persistida. Concorrência, rollback e reinício
possuem verificações próprias; esses resultados não podem ser inferidos de um build.
