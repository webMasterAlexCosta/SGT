# Reprodução e instalação em ambiente de homologação — C25-H04-8045

## Ponto de entrada

Leia primeiro `RELATORIO_HOMOLOGACAO_FINAL.md`, `NOTAS_DE_VERSAO_E_PENDENCIAS.md` e `SELO_FINAL_BYTES.json`. Esta entrega destina-se à homologação comprovável e não autoriza implantação em produção. Os quatro artefatos canônicos estão identificados no selo; fontes e binário precisam pertencer ao mesmo conjunto. A candidata04 corresponde ao commit `19824eb0aa93d6a4d826b757852fee15a68adb92`, ao JAR SHA256 `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d` e ao selo operacional rev2 SHA256 `ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79`.

O guia de build é `03_CODIGO_CORRIGIDO/GUIA_BUILD_SEM_DDL_MANUAL.md`. A matriz de cada correção relaciona defeito, arquivo, RED, GREEN, regressão, teste final e parecer. Os recibos antigos e as tentativas interrompidas permanecem disponíveis com suas próprias identidades; não aprovam automaticamente uma nova execução.

## Ambiente requerido

Use MySQL Server real **8.0.45**, Redis real e o Google Chrome fornecido com Playwright. A campanha utilizou Java 17.0.20, Maven 3.9.16, Node 22.16.0, Connector/J 9.7.0, Redis 8.2.11, Google Chrome 153.0.8010.36 e Playwright 1.62.1. O inventário de origem contém tamanho, SHA e caminho de cada dependência. Verifique o binário de servidor, `SELECT VERSION()`, metadados JDBC, UUID, conta, catálogo e porta antes do teste e na DataSource do teste/aplicação.

Crie um ambiente sintético isolado, sem qualquer conexão com dados administrativos reais. O provisionamento de infraestrutura fornece catálogo e conta não administrativa; Hibernate/JPA administra todas as tabelas. Redis deve usar instância/DB autorizados e credencial privada. Não execute scripts SQL de negócio, não troque o servidor por outra versão e não reutilize uma conta privilegiada para ocultar uma configuração incompleta.

Na plataforma desta campanha, processos de chamadas de execução diferentes não compartilham a mesma rede. Por isso o supervisor `runtime/serve_runtime.py` mantém MySQL, Redis e todos os comandos de campanha como processos filhos da mesma sessão. Essa é uma característica do ambiente observado, não uma dependência funcional do SGT. Fora dele, mantenha conectividade equivalente e registre os processos e endereços efetivamente usados.

## Configuração privada

Configure valores autorizados em um arquivo privado com modo `0600`; use `.env.example` e os contratos de configuração fornecidos como referência. Não distribua o arquivo, não o execute como shell e não inclua credenciais em URL JDBC, linha de comando, relatórios ou histórico de terminal.

| Grupo | Entradas relevantes |
|---|---|
| Aplicação/JDBC | `DB_DRIVER`, `DB_URL`, `DB_USER`, `DB_PASS` |
| Integração | `INTEGRATION_MYSQL_MODE`, `INTEGRATION_MYSQL_URL`, `INTEGRATION_MYSQL_USER`, `INTEGRATION_MYSQL_PASSWORD` |
| Identidade auditada | `SGT_MYSQL_SERVER_UUID`, identificador exclusivo da rodada |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_DATABASE`, `REDIS_PASSWORD` |
| Segurança | `JWT_SECRET`, `AUDIT_SECRET`, origens CORS e demais campos exigidos pelo exemplo |
| Arquivos | `FILE_UPLOAD_DIR`, `RELATORIO_TEMP_DIR`, `AUDIT_FALLBACK_LOG` |
| Tempo | `TZ=America/Sao_Paulo`, `TIMEZONE_PADRAO=America/Sao_Paulo` |

Os suportes de integração recusam parâmetros ausentes/vazios e contas administrativas. Na campanha, os catálogos eram exclusivos por finalidade e os bancos Redis separados. A aplicação não foi autorizada a enviar e-mails. As opções de seed de usuários globais, gestores, unidades operacionais e jornadas operacionais foram desativadas na preparação da campanha; as identidades sintéticas foram criadas por fixtures JPA rastreadas. Isso não significa ausência de toda inicialização de referência: as fotografias registram as cinco tabelas de referência/seed admitidas antes da criação dos dados de negócio.

## Inicialização do JAR

Após conferir o SHA do JAR e preparar o arquivo privado:

```bash
java -Xmx640m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2 \
  -Duser.timezone=America/Sao_Paulo \
  -Denv-file="$SGT_PRIVATE_ENV_FILE" \
  -jar sgt-backend-final.jar \
  --spring.jpa.hibernate.ddl-auto=update \
  --spring.sql.init.mode=never \
  --logging.level.org.springframework.boot.availability=DEBUG \
  --app.homologacao.mysql8045=true
```

`SGT_PRIVATE_ENV_FILE` é um caminho privado local, não um arquivo da entrega pública. O nível DEBUG acima é o mesmo usado pelo harness para tornar observável o estado de disponibilidade. Confirme no log da JVM a linha `SGT_MYSQL8045_DATASOURCE` e o estado `ACCEPTING_TRAFFIC` antes de iniciar cenários. A linha deve declarar o MySQL 8.0.45 real e a identidade esperada do catálogo. A guarda recusa nominalmente `root` e contas internas, registra UUID sem compará-la ao ambiente e não lê GRANTS; confira separadamente os privilégios da conta dedicada. O JAR integra os assets React; confira suas respostas HTTP e hashes, além da simples disponibilidade da página inicial.

## Repetição das camadas

1. Extração limpa, conferência dos manifests e build conforme o guia próprio. Registre as versões e a identidade de todos os inputs.
2. Java normal sem filtros: `mvn -o -B -Pintegration clean verify jacoco:report`, com cache Maven local e configuração privada explícitos. Some os XMLs novos e compare os casos ao comando e ao console. Execute separadamente o perfil de carga e os suplementos opt-in, conservando seus resultados individuais.
3. React integral com cobertura pelo runner `frontend_evidence/20_REACT_COVERAGE_PREPARACAO/run_f12_react_coverage.py`, com novo `run-id`, mesmo selo/JAR e seleção completa. Não atribua a Vitest prova de navegador, banco ou transporte TCP.
4. Campanha final `native_revisions/run_native_campaign_v3.py`, vinculada ao plano e ao selo atuais: H02 cria o schema por Hibernate; fixture JPA sintética explicitamente atribuída; H02 cria fatos reais por HTTP; derivada abre os mesmos dados; snapshots comparam todos os valores, estrutura e arquivos antes de novas requisições. Depois ocorrem os casos HTTP, documentos, fornecedores/cache/CSRF, relatórios e reinício do mesmo JAR. O V3 exige catálogo sem negócio anterior e só admite como seed as cinco tabelas explicitamente verificadas; se houver estado inesperado, a campanha para em vez de reutilizá-lo silenciosamente. O adendo operacional registra os pedidos futuros e seus hashes; não altera o selo nem o controlador da F12 completa.
5. Fechamento real: use o plano próprio da rodada, com 200 colaboradores, arquivos válidos, chunk 1 e comparação chunk 500. Observe controle completo, prefixo confirmado, cancelamento, reprocessamento, revogação, queda da JVM própria, retomada automática e reinício. Setup incompleto e timeout conservam status próprio.
6. Chrome real: use o executável fornecido e Playwright, perfil novo e o JAR final vivo. Um probe de lançamento prova apenas disponibilidade do navegador. As jornadas precisam de DOM/rede/persistência e seus próprios resultados. Se o ambiente impedir a inicialização, preserve stderr/código de saída e classifique o bloqueio; não substitua Chrome por outra ferramenta.
7. Revisão adversarial: reabra os artefatos, confira os bytes e repita os oráculos críticos em execução independente da implementação. Registre qualquer divergência e as partes sem comprovação.

Cada comando recebe identificador único, timestamps UTC, fuso administrativo, input hashes, processo, porta, exit code, logs íntegros e XMLs frescos. Os comandos detalhados e parâmetros pertencem aos recibos sanitizados da rodada. A contagem de testes não substitui o atendimento dos predicados de cada requisito ou ROP.

## Persistência e recuperação

Uploads e relatórios de uma mesma rodada precisam usar os mesmos diretórios durante os reinícios. No ambiente observado, os diretórios vivos ficaram em `/tmp` para separar escrita da aplicação da sincronização da área de trabalho; após saída da JVM, foi exigida cópia privada com igualdade de tamanho/SHA. Um caminho temporário não é armazenamento permanente de produção.

O supervisor só produz um snapshot frio normal depois de MySQL e Redis encerrarem corretamente. Uma cópia de estado após interrupção é rotulada separadamente e recuperada pelo mecanismo normal dos servidores; nunca por remoção de redo/undo ou `innodb_force_recovery`. A recuperação de um servidor não comprova, sozinha, igualdade de todos os dados: essa conclusão exige os snapshots de valores correspondentes.

Conserve originais H02, checkpoint H03, todas as tentativas e resultados que falharam. Não reinicialize um catálogo para fazer desaparecer uma divergência. Ao repetir, use novo ID e informe se o catálogo era novo, recuperado ou já continha dados de uma tentativa anterior.

## Publicação e aceite

Os arquivos públicos omitem credenciais, tokens, cookies, configurações privadas e snapshots SQL completos. A sanitização registra hashes de origem e cópia e não altera assertions ou transforma falha em sucesso. Arquivos históricos podem conter resultados e versões anteriores devidamente rotulados.

O recibo de publicação identifica cada arquivo persistido e sua releitura por SHA/CRC. A decisão de aceite pertence ao relatório final: 33 critérios dependem de documentos administrativos autênticos não fornecidos; qualquer outro defeito, bloqueio de ambiente ou cenário não executado deve permanecer expresso. Uma reprodução bem-sucedida em outra máquina é uma nova evidência e precisa de seus próprios recibos.

Revisão documental: 2026-11-04T00:25:06.293160+00:00. Textos anteriores preservados em histórico; produto, testes e selo intactos.

Adendo editorial de 2026-11-04T03:15:34.204054+00:00: distingue as quatro opções de seed desativadas dos dados de referência observados; nenhuma configuração ou fonte de execução foi alterada.


## Adendo final — FE02 discriminante e localização das provas

O complemento fechado usa FE02_HTTP_VARIADO_03 e FE02_XLSX_VARIADO_CONTINUACAO03_01. O primeiro continuou um prefixo qualificado sem repetir cadastros já persistidos: 72 vínculos da tentativa original, o item 73 qualificado separadamente e 49 novos totalizam 122 vínculos do corpus. As duas identidades auxiliares não integram essa contagem. Os 29 lotes de frequência e as 29 fontes de capacidade sustentam as dez séries diárias distintas; 106 chamadas e 328 asserções incluem duas fases, antes/depois do reinício. Os 78 POSTs negociais e os dois logins são universos separados.

A segunda execução invocou a função produtiva React sobre os DTOs HTTP reais encerrados. Produziu dois XLSX, cada um com sete abas/360 células tipadas e série diária de 15×12. B–K têm 15 valores distintos, L tem 14 e o texto “—” em 29/02; 01/03 conserva a data civil anterior correspondente. A revisão independente conferiu todas as células, tipos, formatos e a igualdade após reinício. Esse harness usa Node/Vite e instrumenta contexto e captura do Blob; não executa a tela, Chrome, nova rede ou PDF React. watch:null foi solicitado, porém sua aplicação não foi comprovada. Não apresente essa prova como uma jornada de navegador.

Os manifests públicos de FE02_CONTINUACAO03_REAL e do writer variado identificam as cópias exatas e as omissões pontuais de credenciais e caminhos privados. As tentativas anteriores conservam seus próprios IDs e resultados; reabrir um estado interrompido exige qualificá-lo antes de continuar. Uma repetição deve escolher nova identidade de execução, preservar os oráculos e registrar o corpus que realmente criou ou reutilizou. Os planos já encerrados não autorizam repetir seus POSTs no mesmo catálogo.

Os caminhos internos registrados em recibos são os localizadores de captura no workspace da campanha. Para localizar um arquivo incluído na entrega, use seu SHA256 integral no manifesto do pacote e no mapeamento fonte→cópia pública; o nome editorial de uma cópia pode ser diferente sem mudar seus bytes. Os fontes completos estão nos ZIPs e na pasta 03 do pacote final. Um caminho de origem não implica que o arquivo privado correspondente foi distribuído.

Configurações privadas, bases frias, fotografias SQL integrais, sessões e credenciais foram omitidas expressamente. Suas identidades agregadas e os resultados de comparação permanecem rastreáveis, mas o pacote público não permite repetir literalmente toda comparação de linhas privadas sem novos dados e configuração autorizados. Essa limitação de replay é distinta da possibilidade de compilar os fontes e executar novas suítes no ambiente exigido. Consulte o registro de omissões e os pareceres antes de concluir que uma entrada ausente é uma evidência publicada.

Java e React não foram alterados durante esse complemento. Os quatro hashes de produto e o selo F11 rev2 continuam sendo a identidade da entrega; a comparação de valores e a exportação posterior não implicam uma nova compilação.
