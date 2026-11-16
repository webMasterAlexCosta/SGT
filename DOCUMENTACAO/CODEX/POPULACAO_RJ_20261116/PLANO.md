# População sintética da SE/RJ

Pedido e decisões de 16/11/2026: preservar baseline/checkpoint e executar clean package antes da alteração; colocar a carga no código Java; preencher empregados, vínculos e frequência (presença, faltas e outras situações) para exercitar indicadores. O esclarecimento humano concentra a entrega nesses dados e dependências, sem exigir ocupar as 70 entidades técnicas. O usuário escolheu alternar para um banco separado sem esses dados pelo .env. Servidor local deve ficar iniciado; Windows será testado posteriormente pelo usuário.

## Desenho e escolhas de implementação

`APP_DEMO_RJ_ENABLED=false` é o padrão. `true` seleciona URL/usuário/senha exclusivos de demonstração antes da configuração do DataSource e ativa a carga. `false` mantém o DataSource normal e não carrega demonstração. Reinício obrigatório; nenhuma opção apaga dados. Ambos os bancos são preservados.

Usar banco PostgreSQL próprio, local e separado, role limitada e ownership comprovados; recusar mesmo banco, perfil produtivo, destino sem identificação explícita e dados negociais preexistentes sem marcador de demonstração. Schema somente JPA/update; sem DDL de domínio ou purga. Para Windows manter .env importado pelo mecanismo atual do projeto e execução pelo JAR, sem serviços systemd no código.

Reaproveitar InicializadorDados para SE/RJ e GTRAT/GEDIS/GETRA. Criar dados identificados DEMO-RJ: seis contratos/composições, seis lotações/jornadas e cargos, fornecedores, 54 pessoas/empregados/vínculos/alocações (60 vagas), frequências desde o início do ano anterior até a referência atual. Conservar escala semanal; criar presenças, faltas, atestados, atrasos, parcial e pendências distribuídos deterministamente. Sem frequência futura, ausência de registro não vira falta. Datas e horários da massa são explicitamente fictícios; não representam prova administrativa autêntica. Cálculos, snapshots e cadeia de frequência usam componentes existentes, com rastreabilidade sintética.

Volume bounded: dois dias por semana no acervo antigo e todos os dias úteis dos últimos 90 dias, incluindo referência diária. Tamanho em dezenas de milhares no máximo; flush por lotes; limite de memória mantido. Valores/datas iniciais fixados no marcador da carga; reinício não duplica nem sobrescreve alterações manuais. Novos dias não são acrescentados implicitamente a um corpus já concluído.

Criar acesso fictício estadual e local apenas no banco de demonstração, senha inicial externa obrigatória, sem senha padrão no código ou exemplo. Não mudar contas/dados do banco normal. Contas identificadas como demonstração e troca inicial obrigatória.

Adendo humano: administrador padrão sempre presente, independente de demonstracao. Na aplicação normal, criar `adm`/`90000001` somente se nenhum Administrador já existir; senha externa `APP_ADMIN_INITIAL_PASSWORD` obrigatória somente na criação, troca inicial obrigatória. Preservar contas existentes, senha e estado; recusar colisões, sem promover contas. Fixtures do perfil test mantêm seus usuários próprios. Não alterar o comissionador isolado anterior. Separar também o database Redis entre os modos para evitar cache compartilhado.

Alternativas: carga no banco normal foi descartada pela escolha humana de alternar bancos; popular todas as tabelas técnicas sem necessidade foi substituído pelo foco humano em indicadores. Seeds opcionais existentes são parciais e não atendem ao corpus; serão reutilizados somente para a estrutura base.

## Execução nesta sessão

Um responsável, sem agentes ou tarefa/revisão separada, sem worktree/branch nova ou atualização de dependências. Maven wrapper 3.9.16/JDK25/Boot4.1.2; fonte e assets preservados; heavy-run.py, Maven 512 MiB/fork 768 MiB/metaspace 256 MiB, pool8, cgroup2–3 GiB, agregado 15 GB e reserva 8 GiB. Franquia: pausar em 1% restante.

- [x] Baseline limpo e backup de fontes/JAR/target; tag local `codex/baseline-rj-20261116` no commit `e873b36b`; 811 unitários e clean package aprovados antes da implementação.
- [x] Escrever e observar regressões da seleção de banco, flag desligada, separação, configurações inválidas, corpus e repetição; implementar seletor e inicializador condicionado.
- [x] Configurar variável desativada no exemplo, documentar os dois bancos e como usar o JAR no Windows; não publicar configurações privadas.
- [x] Validar unitários/package e integração PostgreSQL real nova: corpus escopado em RJ, frequências/ausências não vazias, períodos, cadeia, rollback e segunda execução sem duplicação; banco normal intacto.
- [x] Preservar alvo, criar apenas banco isolado explicitamente autorizado, iniciar JAR em demonstração, conferir HTTP/login/painel, registrar hashes e resultados e salvar checkpoint final.

Critérios: false mantém URL/credenciais normais; true jamais grava no banco normal; faltas e presenças aparecem no painel da SE/RJ; nenhum registro futuro ou órfão; repetição idempotente; fonte/testes/build identificados por hash. Não afirmar execução Windows ou homologação de produção.


## Resultado — 16/11/2026

Implementados o seletor anterior ao DataSource, a classe de população e o administrador padrão independente da demonstração. A classe adicionada ao perfil normal cria o administrador apenas se nenhum Administrador já existir; o perfil test usa suas fixtures. O administrador existente nunca é reativado ou tem senha redefinida.

Passaram os 825 unitários no clean package final e os dois testes PostgreSQL com os mesmos fontes finais. O primeiro package encontrou a ausência do contrato DiagnosticoOperacao na classe nova; a correção foi revalidada na suíte completa. Os logs/estados intermediários permanecem na campanha privada.

O banco normal manteve os hashes e contagens dos seis conjuntos negociais conferidos (contratos, pessoas, empregados, vínculos, alocações e frequências); somente o administrador padrão foi acrescentado, por pedido humano. O banco demo tem 6 contratos, 54 empregados/vínculos/alocações e 9.626 frequências: 5.347 presenças, 1.069 faltas, 1.070 atestados, 1.070 atrasos e 1.070 presenças parciais. Cinco usuários: administrador e quatro perfis fictícios da SE/RJ.

Navegador real: primeiro acesso com troca obrigatória, cinco períodos com registros/ausências, três telas em Diário; reinício sem duplicação e senha alterada preservada. Admin autenticou e exigiu troca inicial, sem consumo de sua senha inicial pelo QA. A aplicação permanece na porta 58501, modo de demonstração. [Uso, .env, contas e Windows](USO.md). [Recibo e hash do JAR](VERIFICACAO.json).
