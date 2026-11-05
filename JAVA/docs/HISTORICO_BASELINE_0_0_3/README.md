# Backend SGT/GTRAT — operacional 0.0.3-operacional-20261101

Derivação do CANDIDATO_02 validado com ressalvas de 01/11/2026, para retirada
da implementação exclusiva de Governança financeira. Os originais permanecem
preservados. Tarifas, folhas de horas monetárias, faturas e comandos tarifários
não possuem mais controllers, serviços, DTOs, repositórios ou mapeamentos JPA.

Fornecedores, vínculo com contrato em rascunho, admissão/desligamento e avaliação
composta de risco permanecem. O fator financeiro da avaliação compartilhada foi
preservado com seu peso original; removê-lo mudaria a regra composta. Frequência,
relatórios operacionais com minutos/valores contratuais, prova por campo e história
continuam com seus contratos originais.

As cinco tabelas financeiras existentes não são apagadas nem alteradas por SQL.
A retirada de mapeamentos não autoriza purgar registros, documentos ou auditoria.
O guia e os relatórios desta entrega identificam a execução efetiva e as pendências;
os relatórios herdados em `docs/` são evidência histórica da baseline e não validam
a versão operacional.

Código Java, testes e documentação dos 198 requisitos originais. A classificação
efetivamente demonstrada está em `docs/MATRIZ_REQUISITOS_FINAL.md` e no JSON
correspondente. `docs/PENDENCIAS_FINAIS.md` identifica os limites de aceite; o
número de testes ou a existência de um JAR não substitui essas evidências.

## Conteúdo e rastreabilidade

- `src/main`: implementação Spring Boot, entidades JPA e recursos da aplicação.
- `src/test`: testes permanentes de unidade, HTTP, persistência e concorrência.
- `target/sig-gtrat-backend-0.0.3-operacional-20261101.jar`: saída do build desta versão.
- `docs`: norma original, matrizes, relatórios e índice do histórico.
- `evidencias`: execuções identificadas por rodada e seus manifestos de fonte.
- `auditoria_independente`: revisões, propostas de reprodução e fontes identificadas.
- `scripts/validacao`: roteiros de verificação e empacotamento, sem DDL de aplicação.
- `SHA256_CONTEUDO.txt`: hashes dos arquivos do pacote completo.

Os arquivos de uma rodada reprovada são mantidos como histórico. Consulte
`docs/RELATORIO_REGRESSAO.md` para identificar a rodada que corresponde ao JAR
distribuído. Logs e XMLs distribuídos podem ter tokens efêmeros mascarados; os
manifestos de sanitização relacionam seu hash ao hash do original preservado.

## Ambiente

A validação deste atendimento usa Linux, JDK Temurin 21 e compilação Java 17,
Maven 3.9.16, MySQL 8.4 e Redis reais. Os números completos de versão estão no
relatório de execução. A compatibilidade de bytecode Java 17 não equivale a uma
execução homologada em JDK 17. Os roteiros Windows foram inspecionados; a
homologação de execução Windows não foi realizada neste ambiente Linux.

O schema da aplicação é criado e atualizado **exclusivamente pelo Hibernate**,
com `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`. Uma
base MySQL vazia e um usuário autorizado devem existir antes de iniciar. Não há
Flyway, Liquibase ou roteiro de criação/alteração de tabelas. Não usar um banco
de produção nos testes: eles gravam e removem dados sintéticos.

Para desenvolvimento, copie `.env.example` para `.env` e preencha as credenciais
locais. O carregador da aplicação lê esse arquivo; ele não é um script shell.
Configure `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET` e `AUDIT_SECRET`. Para usar
Redis, configure também `CACHE_TYPE=redis`, `REDIS_HOST`, `REDIS_PORT` e a senha
da instância. Os testes específicos de cache precisam de Redis mesmo quando o
cache padrão da aplicação estiver desligado. E-mail permanece desabilitado no
exemplo e nos testes.

Os seeds opcionais de contas de desenvolvimento vêm desabilitados e exigem uma
senha inicial exclusiva. O seed dos gestores locais atende apenas às Gestões
iniciais de código exato `GTRAT-UF`, `GEDIS-UF` e `GETRA-UF`, dentro da respectiva
SE. Gestões adicionais continuam disponíveis ao cadastro administrativo; suas
contas são criadas pelas operações de administração de usuários. O seed não
atribui a elas matrículas sintéticas compartilhadas nem reutiliza uma conta
por prefixo do código da Gestão.

## Memória e execução serial

Em uma máquina de 32 GB, mantenha espaço para o sistema operacional e execute
uma única suíte pesada de cada vez. Os testes deste pacote limitam Maven a
512 MiB de heap e o único fork a 768 MiB; cada JVM limita o metaspace a 256 MiB.
O pool MySQL dos testes tem no máximo oito conexões. O relatório de concorrência
registra os consumos medidos, separados desses limites configurados.

| Componente | Configuração da validação |
|---|---:|
| Maven | heap máximo 512 MiB; metaspace 256 MiB |
| Único fork de testes | heap máximo 768 MiB; metaspace 256 MiB |
| JAR HTTP, executado depois da suíte | heap máximo 768 MiB; metaspace 256 MiB |
| MySQL local desta evolução | buffer pool InnoDB de 128 MiB; máximo de 20 conexões |
| Redis local | `maxmemory` de 64 MiB; consumo total é maior |
| Pool de conexões dos testes | máximo 8; mínimo 2 |

O código de produção não limita o servidor MySQL ou Redis do usuário. Configure
esses serviços de acordo com a carga e conserve os limites conservadores para
reproduzir a validação. Paginação não garante, por si só, memória constante de
um contexto JPA durante uma transação; os limites efetivamente medidos estão
documentados nos relatórios.

## Linux

Com JDK no `PATH`, MySQL e Redis locais preparados:

```bash
cp .env.example .env
# Edite .env com as credenciais locais antes de iniciar.
chmod +x mvnw scripts/validar-backend.sh scripts/validacao/executar_regressao_mysql.sh
export MAVEN_OPTS='-Xmx512m -XX:MaxMetaspaceSize=256m'
./scripts/validar-backend.sh preflight
./mvnw -B -ntp clean test
```

Para a regressão completa, escolha uma base **descartável** cujo nome comece
com `sgt_impl_` ou `sgt_audit_` (a base local `sgt_correcao_exec` também é
reconhecida pelo roteiro). Substitua as credenciais dos exemplos:

```bash
export INTEGRATION_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/sgt_impl_validacao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo'
export INTEGRATION_MYSQL_USER='usuario_local'
export INTEGRATION_MYSQL_PASSWORD='senha_local'
export DB_URL="$INTEGRATION_MYSQL_URL"
export DB_USER="$INTEGRATION_MYSQL_USER"
export DB_PASS="$INTEGRATION_MYSQL_PASSWORD"
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export REDIS_PASSWORD='senha_local_redis'
./scripts/validar-backend.sh completo
```

`completo` executa, em sequência, `-Pintegration clean verify jacoco:report` e
`-Pdb-load verify jacoco:report`. O segundo comando repete testes de unidade;
essa repetição não aumenta o número de testes distintos comprovados. A
integração exclui apenas a tag `load`, que é executada pelo segundo perfil.
Os XMLs ficam em `target/surefire-reports` e `target/failsafe-reports`. Arquive
cada rodada antes de executar outra para não confundir seus resultados.

Para construir e executar localmente após a regressão:

```bash
./mvnw -B -ntp package
java -Xmx768m -XX:MaxMetaspaceSize=256m \
  -jar target/sig-gtrat-backend-0.0.3-operacional-20261101.jar --env-file=./.env \
  --spring.jpa.hibernate.ddl-auto=update --spring.sql.init.mode=never
```

Reconstruir gera outro artefato: o novo hash não herda a validação HTTP do JAR
fornecido. Para reproduzir essa validação, use os roteiros de
`scripts/validacao` e a sequência de preparação, HTTP, encerramento, comparação
persistida e reinício descrita em `docs/RELATORIO_JAR_HTTP_REINICIO.md`.

## Windows / PowerShell

```powershell
Copy-Item .env.example .env
# Edite .env com suas credenciais locais.
$env:MAVEN_OPTS = '-Xmx512m -XX:MaxMetaspaceSize=256m'
.\scripts\validar-backend.ps1 -Modo preflight
.\mvnw.cmd -B -ntp clean test

$env:INTEGRATION_MYSQL_URL = 'jdbc:mysql://127.0.0.1:3306/sgt_impl_validacao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo'
$env:INTEGRATION_MYSQL_USER = 'usuario_local'
$env:INTEGRATION_MYSQL_PASSWORD = 'senha_local'
$env:DB_URL = $env:INTEGRATION_MYSQL_URL
$env:DB_USER = $env:INTEGRATION_MYSQL_USER
$env:DB_PASS = $env:INTEGRATION_MYSQL_PASSWORD
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '6379'
$env:REDIS_PASSWORD = 'senha_local_redis'
.\scripts\validar-backend.ps1 -Modo completo

java -Xmx768m -XX:MaxMetaspaceSize=256m `
  -jar target\sig-gtrat-backend-0.0.3-operacional-20261101.jar --env-file=.\.env `
  --spring.jpa.hibernate.ddl-auto=update --spring.sql.init.mode=never
```

O wrapper baixa a distribuição Maven declarada e confere seu SHA-256. Uma
primeira execução precisa de acesso ao repositório de dependências, ou de um
cache Maven previamente preparado. O ZIP não inclui JDK, servidor MySQL,
servidor Redis nem cache de dependências.

## Integração e limites documentais

`docs/INTEGRACAO_FRONTEND.md` explica os contratos recebidos; os contratos
aditivos desta versão estão em `docs/EVOLUCAO_GOVERNANCA/`. O frontend recebido
foi preservado na baseline e o novo React é integrado somente a esta derivação.
As fontes históricas
ausentes não são reconstruídas a partir do cadastro atual: a aplicação conserva
`null`, desconhecimento ou recusa de negócio conforme a norma. Consulte os IDs
exatos e o efeito dessa ausência em `docs/PENDENCIAS_FINAIS.md`.
