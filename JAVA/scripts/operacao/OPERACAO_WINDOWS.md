# Operação do SGT — instalação nova e recuperação

Este procedimento prepara uma instalação **nova**, sem importar acervo. Os ensaios desta campanha usam Linux, PostgreSQL18.6 e Valkey9.0.7 isolados. Os scripts PowerShell e as ACLs precisam de execução nativa no Windows10 de destino. Não há autorização de deploy, certificado institucional, destino externo de backup ou responsável pelo aceite definidos nesta campanha.

## Pré-requisitos do destino

Registrar a edição e cobertura de segurança do Windows, fornecedor/patch suportado do Java25, PostgreSQL18.6 e clientes `psql`, `pg_dump`, `pg_restore`18.6 no PATH. O suporte auxiliar exige Python3.11+ padrão; o aplicativo usa Java. Definir um serviço Redis compatível, privado e autenticado no destino; o ensaio Valkey em Linux não certifica sua instalação Windows. Não instalar substitutos, containers, serviço, regras de firewall ou confiança em certificados automaticamente.

Usar conta de serviço dedicada, banco próprio novo e role limitada, sem superuser, CREATEDB, CREATEROLE, REPLICATION, BYPASSRLS ou memberships. Um administrador de infraestrutura cria somente role/banco e concede o ownership necessário. Todo schema de domínio vem do Hibernate/JPA. Não usar `postgres` para executar o SGT. Não apontar para banco operacional ou compartilhado para experimentar o procedimento.

Modalidade inicial: HTTPS direto no Tomcat do JAR. Configurar DNS/hostname, certificado PKCS12 e cadeia confiável para os clientes, chave privada protegida e origem HTTPS exata. `forward-headers-strategy=none` não confia em cabeçalhos de proxy; uma topologia com proxy exige configuração e ensaio próprios. HTTP de laboratório não é configuração de produção.

## Arquivos e configuração privados

Conferir o SHA256 do JAR contra o manifesto entregue e um registro confiável externo ao próprio arquivo. Preservar o pacote anterior antes de qualquer substituição. Manter configuração, PKCS12, chaves e backups fora do repositório, do webroot e de `uploads`.

Copiar `producao.properties.example` para um arquivo privado, preencher todos os campos vazios e caminhos reais. Caminhos em `.properties` devem usar `/` no Windows ou escapes próprios do formato; nunca colar senhas numa linha de comando, histórico de terminal, Git ou recibo. Gerar chaves JWT/auditoria diferentes, criptograficamente aleatórias; não reutilizar valores de testes. Chave de auditoria e arquivos históricos precisam permanecer juntos na recuperação.

Restringir a ACL do arquivo e diretório à conta responsável, SYSTEM e Administradores. Conferir com `Get-Acl`; o launcher recusa Allow para outras identidades. Não remover proteções de diretórios anteriores para fazer o check passar. Preencher `sgt.admin.username`, `sgt.admin.matricula`, `sgt.admin.email` e `sgt.admin.password` com a identidade nominal do responsável inicial; não usar as contas do seed de desenvolvimento.

`SSL_ENABLED=true`, origens HTTPS explícitas e cargas opcionais `APP_SEED_*=false`. O perfilprod exige `ddl-auto=validate`, `generate-ddl=false`, init SQL `never`, SQL e bind OFF. E-mail está desativado no exemplo: ativar exige servidor/credenciais e teste real de entrega. O exemplo não fornece credenciais utilizáveis. O arquivo de configuração é passado explicitamente por `--env-file` e importação Spring, sem depender de `.env` no diretório corrente.

## Primeira instalação, sem dados

Executar em PowerShell no diretório de operações do pacote. Não contornar a ExecutionPolicy: usar o processo de execução/assinatura aprovado no destino. Os comandos abaixo usam caminhos de exemplo, não autorizam sua execução contra recursos reais.

```powershell
$SgtJar = 'C:/SGT/app/sgt.jar'
$SgtConfig = 'C:/SGT/private/producao.properties'
$SgtHash = '<SHA256 do JAR no manifesto confiavel>'
./windows/Start-Sgt.ps1 -Jar $SgtJar -Config $SgtConfig -ExpectedSha256 $SgtHash -CheckOnly
./windows/Initialize-SgtSchema.ps1 -Jar $SgtJar -Config $SgtConfig -ExpectedSha256 $SgtHash -AuthorizeNewDatabase
./windows/Initialize-SgtAdmin.ps1 -Jar $SgtJar -Config $SgtConfig -ExpectedSha256 $SgtHash -AuthorizeInitialAdministrator
./windows/Start-Sgt.ps1 -Jar $SgtJar -Config $SgtConfig -ExpectedSha256 $SgtHash
```

`CheckOnly` confere arquivos, ACL, hash e Java; não verifica banco, TLS nem toda a configuração. O preparador de schema usa contexto mínimo JPA, sem HTTP/seeds, somente em banco próprio vazio. O comissionador usa `validate`, transação e locks: cria somente um Administrador ativo, sem Gestão/Lotação fictícias e com troca obrigatória de senha. Ele recusa banco com qualquer dado, repetição ou lock concorrente; nunca redefine uma conta existente. Depois do comissionamento, retirar a senha inicial do arquivo operacional privado, preservando o recibo sem senha. Se uma etapa falhar, preservar banco/logs e diagnosticar; não apagar tabelas para permitir repetição.

O launcher mantém o Java em primeiro plano. Encerrar pelo mecanismo de parada normal; aguardar o término, conexões e gravações antes do backup. Um supervisor/serviço Windows, sua identidade, permissões, reinício e parada precisam ser definidos e testados no destino. Estes scripts não instalam serviço nem alteram firewall.

Em outro computador físico, Chrome deve confiar na cadeia institucional, abrir o hostname HTTPS, autenticar com a senha inicial, ser obrigado a escolher senha pessoal, recarregar `/historico`, abrir `/frequencia` e sair. Conferir que não há contas de desenvolvimento, que permissões negadas não encerram sessão válida e que não se acessa API protegida antes da troca. `Test-SgtHttps.ps1 -BaseUrl 'https://sgt.exemplo.invalid'` verifica certificado/hostname normalmente, SPA, CSRF e HSTS; não substitui autenticação e jornada humana. Não enviar senha, cookies ou tokens como evidência.

## Backup frio completo

Criar uma configuração JSON privada a partir de `backup-settings.example.json`. Informar explicitamente banco, role limitada e senha; `sslmode=verify-full` e CA são obrigatórios para PostgreSQL remoto. Os caminhos `uploads`, `audit_fallback` e `recovery_material` são separados e não podem se sobrepor. Material de recuperação inclui configuração/chaves de auditoria, material criptográfico necessário e certificados; nada disso integra o pacote público. O cache não é backup nem fonte de verdade.

Parar a aplicação e confirmar ausência de conexões de outros clientes. O parâmetro de confirmação não para o serviço por si só. O destino precisa ser novo, privado e fora das árvores de origem.

```powershell
py -3 ./sgt_operacao.py verificar --config 'C:/SGT/private/backup-source.json'
py -3 ./sgt_operacao.py backup --config 'C:/SGT/private/backup-source.json' --output 'E:/SGT-backup/novo-identificador' --application-stopped
```

O suporte confere versão, role/ownership, estabilidade da fonte, todos os valores/sequências/schema e SHA256 dos arquivos. A identidade do banco usa `system_identifier` lido do PostgreSQL e OID normalizado, sem confiar em rótulo manual ou endereço/porta locais. Se a leitura dessa identidade estiver negada, interromper e tratar com a infraestrutura; o suporte não concede privilégios. Manifestos anteriores sem identidade de cluster comprovada são recusados, sem fallback. A consulta é documentada pelo [PostgreSQL](https://www.postgresql.org/docs/18/functions-info.html#FUNCTIONS-CONTROLDATA). `database.dump` é custom data-only; schema/constraints não são importados do dump. Conservar o SHA256 de `manifest.json` em registro independente e protegido. O manifesto não contém senhas, mas o pacote de backup contém material privado e deve ser protegido como tal.

Copiar o backup completo para o destino **fora do computador** que será definido pelo responsável, com acesso restrito e proteção criptográfica do meio/transporte. Recalcular todos os SHA256 e comparar o manifesto com o hash confiável após a cópia. O suporte não inventa destino, criptografia ou retenção, nem executa essa cópia. Um backup no mesmo disco protege recuperação lógica, mas não perda física do computador. Definir retenção/RPO/RTO e ensaiar restauração a partir da cópia externa antes de liberar produção.

## Restauração e retomada

Manter a aplicação parada. Criar outro banco próprio vazio com role limitada; preparar somente o schema com `Initialize-SgtSchema.ps1` e o mesmo JAR validado. **Não executar `Initialize-SgtAdmin.ps1` no destino de restauração.** Seus usuários virão dos dados restaurados. Configurar o JSON de destino com banco diferente da fonte e caminhos de arquivos ainda inexistentes, sem sobreposição. Confirmar que o schema/constraints/índices correspondem ao manifesto. Nenhum destino de arquivos nem a configuração que recebe os recibos de fase pode ficar dentro da árvore do backup, coincidir com ela ou contê-la; a recusa ocorre antes do acesso ao banco, inclusive na retomada.

```powershell
py -3 ./sgt_operacao.py restaurar --config 'C:/SGT/private/restore-target.json' --backup 'E:/SGT-backup/identificador' --manifest-sha256 '<hash confiavel>' --application-stopped
```

A operação verifica primeiro todos os hashes, recusa destino não vazio ou igual à fonte, restaura dados em uma transação com constraints ativas e compara valores/sequências. Não usa clean, DDL do dump, disable-triggers, purga ou FLUSH. Os arquivos só são publicados após a integridade do banco. Em falha, conservar os recibos de fase; não reiniciar o aplicativo sobre recuperação incompleta.

Se os dados já foram restaurados exatamente e houve falha posterior na publicação dos arquivos, a retomada explícita `--resume-files-only` reconfere manifesto/banco e completa somente arquivos; não reaplica SQL. Não usar essa opção para ocultar divergência. Alteração legítima do banco impede essa retomada.

Após sucesso, usar as chaves/certificados recuperados, um namespace de cache novo isolado e validar TLS, login, permissões, anexos e cadeia de auditoria. Reconciliar o resultado com o manifesto. Não apontar a aplicação para o destino antigo por conveniência.

## Atualização e reversão

Este pacote cobre primeira instalação, não migração de versões de acervo. Preservar JAR/configuração e backup antes de futura atualização. Reversão de binário só pode reutilizar banco após comprovar compatibilidade de schema/dados; caso contrário, ensaiar recuperação em destino novo e reconciliação. Nunca usar `ddl-auto=update` em produção para tentar corrigir drift nem sobrescrever banco com dados. `validate` não garante detectar todo drift: usar também a comparação estrutural do backup e as verificações de negócio pertinentes.

Registrar pessoa responsável, data, JAR/hash, configurações sem valores privados, testes reais Windows/Chrome, backup externo e restore aprovado. A liberação produtiva exige esse aceite; os ensaios Linux e os pareceres históricos do laboratório não o substituem.
