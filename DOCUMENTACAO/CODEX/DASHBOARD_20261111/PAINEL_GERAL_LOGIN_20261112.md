# Painel gerencial, login e identificação SGT — 12/11/2026

Pedido humano: melhorar Resumo, Contratos e Operação a partir do acabamento aceito em Desempenho; retirar Governança legada; adotar Diário como padrão; remover o modo visual do login e refatorar o TSX; identificar Alex Costa 89618815 e nomear o projeto/JAR SGT. Checkpoint anterior publicado antes das correções: `a561e5b09f44e2dc9da963afb1f8cd191ca25abe`. Correções e assets foram commitados e enviados em checkpoints sucessivos, conforme solicitado.

## Resultado

- Resumo, Contratos e Operação receberam hierarquia tipográfica, filtros e ações maiores, gráficos com espaço adequado, linhas de tabelas alternadas, hover e foco legível. Detalhamento, exportações e ferramentas existentes foram preservados. A barra do funil representa zero como 0%, com valor acessível, e o eixo percentual não corta 100%. O botão de ranking segue o estilo do painel.
- Governança foi retirada da navegação, com exclusão do checklist e código exclusivo. Integridade e atividade recente ficam em Operação; comparação e matriz de unidades, em Contratos. Não se recriou o módulo de Governança de Terceiros removido anteriormente.
- Diário inicia as três visões, o indicador de efetivo e os endpoints síncronos/assíncronos correspondentes. As janelas históricas de 7/15/30/60/90 dias mantêm sua semântica; Desempenho preserva os cinco calendários existentes. Textos distinguem a janela diária das janelas maiores.
- Os oito arquivos de fonte de Desempenho são idênticos ao checkpoint anterior. O login foi separado em apresentação e `useLogin.ts`; alternância, armazenamento, fundo animado, bolhas, propriedades e estilos exclusivos do modo visual foram retirados. Autenticação, 2FA, recuperação, cadastro e troca obrigatória de senha permanecem nos fluxos existentes. O layout permite alcançar os controles em telas de pouca altura.
- Rodapés identificam “Sistema desenvolvido por Alex Costa 89618815”. Maven usa artifactId/name/finalName SGT e gera `JAVA/target/SGT.jar`. A raiz já se chamava SGT; o módulo JAVA mantém o destino de integração solicitado. Versões, lockfile, bancos e dados operacionais não foram alterados.

## Verificação

| Etapa | Resultado |
|---|---|
| Preparação frontend | 6 testes aprovados |
| React | 879 testes / 113 arquivos; typecheck, cobertura e build aprovados após o último commit de fonte |
| Java unitário | 808 testes, zero falhas/erros/skips no último clean package |
| Integração PostgreSQL/Redis/JAR | 1.598 casos aprovados na consolidação; 378 classes, sendo 373 ordinárias e cinco adicionais; zero falhas/erros/skips pendentes |
| Python operacional / helpers | 31 + 106 testes aprovados |
| JAR final | Correspondência integral, HTTP real/reinício, relatório multipágina, cancelamento com heap 192 MiB e carga PostgreSQL aprovados |
| HTTP da interface | Cinco rotas SPA e 110 assets com hash igual ao dist; API anônima 401 e bootstrap CSRF 200 |

A integração é uma consolidação da última execução por classe/caso, com tentativas anteriores preservadas; não representa uma única execução integral sem interrupções sobre um único JAR. As correções finais de apresentação foram seguidas por nova suíte React completa, clean package e os cinco checks específicos do JAR final. Os cinco cenários de recuperação foram executados no JAR anterior `ac919e1d85cbeaf8d9543be1df78892346138c9e929b3b280734cc5e90b4c418`; suas 866 classes principais são byte a byte idênticas às do JAR final. Atomicidade, revogação, cancelamento/recuperação com dois executores e controles de 200 registros passaram. Os controles com blocos 500 e 1 concluíram em 108,5 s e 81,3 s, dentro dos 120 s originais; heaps e demais limites foram mantidos.

No navegador real do laboratório, Resumo/Contratos/Operação iniciaram em Diário e passaram em 1280/1440/1920 px sem overflow no conteúdo principal nem rótulos de eixo cortados. O zero do funil foi reproduzido antes da correção e conferido depois; vinte origens de detalhamento tiveram Enter/Escape e retorno de foco verificados. Exportações reais XLSX/PDF do JAR final concluíram; ZIP íntegro e assinatura PDF conferidos. O ciclo final de navegação não gerou erros/avisos novos de console. Login sem alternância visual e com o crédito exato conferido no JAR. Não há script de lint.

## Reexecuções e proteção dos dados

Dois defeitos de fixture foram corrigidos: alvo AOP resolvido antes de configurar o spy de aditamento e CNPJ sintético individualizado por cenário. O teste de artefato passou a respeitar `finalName` do POM. Não foram retiradas asserções de rollback, unicidade, autorização ou correspondência de bytes.

O schema público anterior esgotou a faixa de UFs sintéticas e continha chaves de campanhas anteriores. Os dados e uploads foram preservados; os lotes restantes/retestes usaram outro schema criado pelo Hibernate/JPA, na mesma base isolada e role limitada com ownership comprovado. Três classes que coordenam concorrência observando SQL sem prefixo foram repetidas com `currentSchema` e nomes SQL não qualificados, preservando tempos e assertivas. O ensaio HTTP exigia Redis exclusivo no índice 0: uma instância temporária local, com credencial sintética existente e identidade PID/run_id comprovada, preservou essa proteção e o Redis da interface. A instância temporária foi encerrada após os ensaios, com snapshot privado preservado. Sem DDL manual, purga, aumento de privilégios, mocks de desenvolvimento ou alteração de guardiões históricos.

Uma interrupção do coordenador não encerrou o quinto lote; seu BUILD SUCCESS e todos os XMLs foram recolhidos, sem repetir os casos aprovados. Logs das tentativas e resultados finais permanecem separados.

## Artefato e execução

React dist → `JAVA/src/main/resources/static` → Maven wrapper clean package → `java -jar SGT.jar` a partir de `JAVA/target`. Assets e target anteriores foram preservados antes da substituição/limpeza, incluindo backup privado `target-antes-ultima.tar.gz` no diretório local da campanha.

JAR final SHA-256: `13ec71f30cba4385cfac62cb9604355b729eb2439203ba64f5700ca8c8fb6490`. São 110 assets atuais, sem bundles anteriores extras, e 866 classes principais conferidas. Diretórios privados, dotenv e credenciais do laboratório não constam no conteúdo de aplicação do JAR. API própria em `http://127.0.0.1:58501`, com a configuração privada já autorizada do laboratório; PostgreSQL, Redis da interface e Vite preservados.

Suítes pesadas em fila única, cgroups de 2 GiB e 3 GiB quando há JVMs filhas, swap zero e reserva mínima de 8 GiB disponíveis. Limites: Maven 512 MiB, fork Java 768 MiB/metaspace 256 MiB, Node 768 MiB, um worker; limites menores dos cenários prevaleceram. Serviços somam 4,25 GiB de caps durante o Redis temporário. O limite agregado conservador, incluindo validação aguardando a fila e coordenadores, foi 9,5 GiB (aproximadamente 10,2 GB), abaixo do teto humano de 15 GB. Maior pico de um cgroup de validação: 1,8 GiB.

## Capturas, recibos e limites

Capturas antes/depois das três visões e do login, XMLs e logs: `.homologacao/painel-visoes-login-20261112/`. As fotos anteriores usam o padrão de 30 dias e as posteriores, Diário; seus totais não representam o mesmo intervalo. [Recibo sanitizado, hashes, contagens por classe e exclusões](PAINEL_GERAL_LOGIN_20261112.json). [Contrato vigente das visões e períodos](CONTRATO.md). A documentação da iteração anterior foi esclarecida: seu recorte era Desempenho.

Seis cenários históricos/preparadores opt-in permanecem discriminados no recibo, sujeitos a suas próprias precondições de artefato/hash/janela/fixture. Não foram convertidos em passes. A busca Sonar por SGT não retornou projeto; métricas e gate não foram obtidos e não houve scanner. Não houve nova revisão independente. Estas verificações não homologam produção, Windows nativo, LAN externa, autenticidade institucional, PDF/UA completo ou todo o catálogo histórico por associação de classe.
