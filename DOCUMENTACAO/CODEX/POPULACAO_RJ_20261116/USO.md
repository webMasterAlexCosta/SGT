# Dados de demonstração da SE/RJ

A carga fictícia está em `PopuladorDadosRj`, condicionada à seleção do banco de demonstração. Não é importação de registros institucionais. Inclui contratos, composições, fornecedores, jornadas/escala, cargos, 54 pessoas/empregados, vínculos e alocações; frequências com presenças, faltas, atestados, atrasos e presenças parciais. Vagas previstas e pendências são mantidas para que os indicadores não fiquem artificialmente em 100%. Históricos de frequência possuem eventos/hash gerados pelo serviço de auditoria existente.

O administrador padrão existe independentemente desse modo. Na execução normal da aplicação, se nenhum usuário de perfil ADMINISTRADOR existir, é criada a conta `adm`, matrícula `90000001`, com a senha externa `APP_ADMIN_INITIAL_PASSWORD` e troca obrigatória. Se um Administrador já existir, nenhuma conta/senha/ativação é modificada. Colisões com o nome ou a matrícula recusam a criação; não há promoção automática de usuários. O perfil `test` conserva suas fixtures controladas, sem criar essa conta implicitamente.

## Configuração

Acrescente às variáveis já existentes do `.env` (não substitua os outros valores):

```properties
APP_ADMIN_INITIAL_PASSWORD=<senha inicial externa com pelo menos 12 caracteres>
APP_DEMO_RJ_ENABLED=true
APP_DEMO_RJ_DB_URL=jdbc:postgresql://127.0.0.1:5432/sgt_demo_rj
APP_DEMO_RJ_DB_USER=sgt_demo_app
APP_DEMO_RJ_DB_PASSWORD=<senha da role limitada do banco de demonstração>
APP_DEMO_RJ_REDIS_DATABASE=15
APP_DEMO_RJ_INITIAL_PASSWORD=<senha inicial externa das contas fictícias>
```

Os campos entre `<...>` são explicações, não valores prontos. Não distribua o `.env`, senhas ou os arquivos privados do laboratório. O `.env.example` mantém os valores secretos vazios e `APP_DEMO_RJ_ENABLED=false`.

- `true`: seleciona o banco de demonstração antes de criar o DataSource. O banco precisa estar previamente provisionado, com nome terminado em `_demo_rj`, no PostgreSQL local, pertencente a uma role limitada (sem superuser/createdb/createrole). Hibernate/JPA cria as tabelas. A configuração recusa usar o mesmo banco normal, mesmo com aliases de localhost ou parâmetros diferentes.
- `false`: usa `DB_URL`, `DB_USER` e `DB_PASS` já configurados. Não cria dados de demonstração nesse banco.
- O espaço Redis selecionado deve ser diferente de `REDIS_DATABASE`; o padrão da demonstração é 15. Preserve namespaces já ocupados; a aplicação não esvazia Redis ao alternar.
- Reinicie o JAR após trocar a chave. A troca não remove, limpa nem migra nenhum dos bancos. Sessões do outro modo deixam de ser válidas no novo banco; faça login novamente.
- Perfil `prod`/`production` recusa a demonstração. O usuário normal do Windows deve configurar seu banco normal e o banco de demonstração separadamente; não há fallback automático de URL ou senha.

Na primeira carga, a referência é a data civil atual de `America/Sao_Paulo`. O corpus abrange o ano anterior e o ano atual até essa referência, com amostragem antiga e maior densidade nos últimos 90 dias. Escalas 5x2 variam entre as unidades, com unidades trabalhando aos sábados/domingos. Não são criadas frequências futuras nem faltas para representar lançamentos pendentes.

O marcador `DEMO_RJ_DADOS_V1`, gravado na mesma transação da carga, conserva a referência da primeira execução. Reiniciar não duplica o corpus, não redefine senhas nem sobrescreve suas edições. A carga não avança a referência automaticamente; para nova fotografia de demonstração, use outro banco exclusivo, preservando o anterior.

## Acessos

| Conta | Matrícula | Finalidade |
| --- | --- | --- |
| adm | 90000001 | Administrador padrão do sistema |
| demo_rj_estadual | 99000701 | Indicadores consolidados da SE/RJ |
| demo_rj_gtrat | 99000702 | Operação e indicadores da GTRAT/RJ |
| demo_rj_gedis | 99000703 | Operação e indicadores da GEDIS/RJ |
| demo_rj_getra | 99000704 | Operação e indicadores da GETRA/RJ |

As quatro contas fictícias usam inicialmente `APP_DEMO_RJ_INITIAL_PASSWORD`; a conta administrativa usa `APP_ADMIN_INITIAL_PASSWORD`. Todas exigem troca no primeiro acesso. Cada perfil mantém as permissões do sistema: para testar os indicadores da SE/RJ, use a conta estadual ou local correspondente.

## JAR e Windows

O código não depende de systemd ou de comandos Linux. Com JDK25, PostgreSQL18.6 e Redis configurados, execute no diretório `JAVA`, onde o `.env` será lido:

```powershell
.\mvnw.cmd -B -ntp -T1 clean package
java -Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m -jar .\target\SGT.jar
```

Para um `.env` em outro local, use uma variável de ambiente `ENV_FILE` com o caminho (barras `/` também funcionam no Windows) ou a opção Spring `--env-file=C:/caminho/sgt.env`. O teste Windows será feito posteriormente pelo usuário; a validação local registrada não comprova Windows nativo.


## Laboratório local desta entrega

A instância local está na porta 58501, no banco `sgt_demo_rj`. A chave atualmente está ligada no `.env` privado da campanha `.homologacao/populacao-rj-20261116/privado/.env`, importado explicitamente pelo comando do JAR. O banco normal anterior é `sgt_dashboard_ui_lab`; seus dados negociais foram preservados e o administrador padrão foi adicionado. Não copiar os endereços/segredos locais desse laboratório para outra máquina.

As credenciais fictícias atuais estão somente no arquivo local ignorado `.homologacao/populacao-rj-20261116/privado/acesso-demo.json`. A conta estadual passou pelo primeiro acesso no QA: use seu campo `senhaAtual`. O administrador e as três contas locais continuam com a senha inicial e a troca obrigatória. No Windows, forneça seus próprios valores externos no `.env`; a carga cria essas contas no banco de demonstração daquele ambiente.

A referência da massa local é 16/11/2026. O painel abre em Diário, normalmente com referência no dia anterior; altere a referência conforme o período que quiser investigar. O corpus tem amostragem do ano anterior e mais dados nos últimos 90 dias; não é um espelho completo de ponto histórico. [Resultados, hashes e limites](VERIFICACAO.json).
