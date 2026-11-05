# Restaurar a infraestrutura externa de testes

Estes arquivos são cópias dos helpers utilizados na homologação real. Não os execute diretamente desta pasta: seus caminhos relativos pressupõem uma raiz de teste com `runtime/`, `work/java-frozen/` e `evidence/`. Não acompanham senhas, sessões, banco ou arquivos privados. A infraestrutura é Linux x86_64/glibc, separada do frontend e do Java congelado.

## Layout e fontes

| Caminho na raiz de teste | Conteúdo |
|---|---|
| `runtime/*.py`, `runtime/README.md` | Copiar os helpers desta pasta. |
| `runtime/bin/mysql-8.4.11-linux-glibc2.28-x86_64-minimal/` | Extrair o MySQL já preservado em `dependencia/Copiadas_SGE/MySQL/`. |
| `runtime/bin/jdk-21.0.12.1+1/` | Extrair o JDK já preservado em `dependencia/Copiadas_SGE/Java-e-Spring/`; o helper descobre `jdk-*/bin/java`. |
| `runtime/lib/libaio.so.1` | Copiar a biblioteca já preservada em `dependencia/Copiadas_SGE/Outras/`. |
| `runtime/bin/redis/` | Binários do complemento de dependências descrito abaixo. |
| `runtime/lib/ubuntu/` | Extrair os dois `.deb` do complemento, sem instalar globalmente. |
| `runtime/chromium/chrome-linux64/` | Extrair `chrome-linux64.zip` encontrado no pacote próprio `SGT_REACT_COMPLETO_COM_CHROME_20261101T024306Z.zip`. |
| `runtime/automation/node_modules/{playwright,playwright-core}` | Copiar os módulos do complemento, preservando os nomes. |
| `work/java-frozen/` | Extrair `SGT_GTRAT_6_E/` do ZIP Java canônico para este destino, sem alterar seu conteúdo. |
| `runtime/private/` | Criado localmente por `manage.py init`, modo restrito; nunca distribuir. |

O complemento `SGT_COMPLEMENTO_RUNTIME_REDIS_PLAYWRIGHT_20261101.zip`, preservado uma única vez em `/Projetos/SGT/dependencia/Proprias/`, contém apenas dependências adicionais que não estavam nos pacotes recebidos. Ele inclui Redis 8.2.11 com fonte correspondente, Playwright/Playwright Core 1.62.1 e os dois pacotes Ubuntu do cliente MySQL. O README e manifesto internos registram origens, versões e hashes. Não é necessário reinstalar React ou duplicar os pacotes originais.

Exemplo de preparação dos helpers:

```bash
export SGT_BASE="/caminho/SGT-teste"
export SGT_FRONTEND="/caminho/frontend"
mkdir -p "$SGT_BASE/runtime/bin" "$SGT_BASE/runtime/lib" "$SGT_BASE/work" "$SGT_BASE/evidence/infra"
cp "$SGT_FRONTEND"/test/real-runtime/*.py "$SGT_BASE/runtime/"
cp "$SGT_FRONTEND"/test/real-runtime/README.md "$SGT_BASE/runtime/README.md"
```

Extraia os componentes para os destinos da tabela. Antes de iniciar, confira o JAR com `sha256sum`; o resultado obrigatório é `08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d`. A extração do Java preserva o conteúdo canônico, sem recriar seu ZIP. O helper recusa outro SHA.

## Ordem da primeira execução

1. Execute `python3 runtime/manage.py init` somente com datadir vazio.
2. Mantenha `python3 runtime/manage.py serve` em uma sessão; inicia MySQL e Redis.
3. Mantenha `python3 runtime/manage.py serve-app --expected-jar-sha256 08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d` em outra sessão. Aguarde o marcador de prontidão.
4. Execute `python3 runtime/http-smoke.py` e depois `python3 runtime/create-fixture.py`. Eles criam contas sintéticas e registros por HTTP legítimo, preservando credenciais somente em `runtime/private/`.
5. Configure o frontend com `.env` local derivado de `.env.example`, API `/api`, proxy para `http://127.0.0.1:18080` e origem 5173 ou 4173. Compile e abra o React.
6. Exporte `SGT_TEST_RUNTIME="$SGT_BASE/runtime"`, `PLAYWRIGHT_MODULE="$SGT_BASE/runtime/automation/node_modules/playwright"` e `CHROMIUM_EXECUTABLE="$SGT_BASE/runtime/chromium/chrome-linux64/chrome"`. Se usar o build, defina `SGT_FRONTEND_URL=http://127.0.0.1:4173` e `SGT_E2E_MODE=build-producao`.

Os cenários são sequenciais e têm pré-condições de negócio. A ordem da rodada aprovada foi: publicação do contrato → pessoa/vínculo → documento → diálogos → catálogo histórico → nome histórico → contrato histórico → concessão administrativa → segurança/concorrência → transferência/retificação/cancelamento → CNPJ → relatórios → reinício → consultas/replays → paginação das versões. O cenário `admin-autorizacao.cjs` também executa o recibo separado de escopo do gestor de destino. Leia cada cenário antes de executá-lo: repetir uma criação sobre dados já utilizados não é uma restauração limpa.

Os IDs e datas são gerados e preservados pelos helpers em arquivos privados. Não copie IDs de recibos para uma instalação diferente. Todas as provas e referências são explicitamente sintéticas e não substituem documentação administrativa autêntica.

O cenário `persistencia-pos-reinicio.cjs` pressupõe os comandos de negócio já preservados em `private/e2e-business.json`. Execute o reinício documentado em `runtime/README.md` com todos os clientes HTTP pausados; somente após o recibo estrito de comparação abra o React e execute esse cenário. `test_restart_readiness.py` valida apenas o reconhecedor de log; a prova da persistência depende do MySQL real e dos snapshots.

## Limites da reprodução

A entrega preserva código, versões, configurações externas geradas, procedimentos e evidências; não distribui um datadir com credenciais. Os binários foram exercitados no Linux/glibc desta rodada. Outro sistema deve fornecer bibliotecas nativas compatíveis. O ensaio offline de instalação React e os ensaios reais estão documentados; não foi feita uma segunda instalação integral de todos os serviços em outra máquina a partir do ZIP final.
