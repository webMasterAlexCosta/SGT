# Laboratório autorizado — dashboard e prazos

A decisão humana autorizou criar PostgreSQL novo e Redis oficial local. A execução atual usa recursos exclusivos, sem ler dotenv operacional, sem containers e sem acessar produção. O laboratório transitório anterior em `/tmp` não é usado como prova desta versão.

## Recursos atuais

Diretório local ignorado pelo Git: `.homologacao/dashboard-proposta-20261111/`. O subdiretório `privado/` tem modo 0700; configurações, chaves e acesso sintético, 0600. Seus conteúdos nunca entram em commits/recibos públicos. `metadata.json` contém somente URLs/versões/limites, sem credenciais.

- PostgreSQL 18.6 nativo, cluster novo loopback. Bancos `sgt_dashboard_lab` para ITs e `sgt_dashboard_ui_lab` para interface; ambos de propriedade da role limitada `sgt_dashboard_app` (sem superuser, criação de role/banco, replicação ou bypass RLS). Ownership conferido por consulta. Schema de aplicação somente Hibernate/JPA update; sem DDL manual de aplicação ou purga.
- Redis oficial 8.10.3, loopback, autenticação privada, maxmemory 128 MB, sem persistência de cache. Fonte oficial `https://download.redis.io/releases/redis-8.10.3.tar.gz`, SHA256 `edde778f6bdee7581e42fc139ca9e07e2822212092f7501f99989656e3629d02`; compilação core servidor/cliente, um processo, sem instalação global ou módulos externos.
- Spring/JAR e Vite/HMR separados, proxy e autenticação reais, portas exclusivas descritas em `metadata.json`. `.env.example` públicos e parâmetros do laboratório formam a configuração privada; nenhum `.env` operacional é importado.
- Dados sintéticos: três contratos/prestadores/unidades na gestão de origem, 27 trabalhadores fictícios, presenças, faltas/atestados e pendências, registros classificados e acervo indeterminado; gestão externa separada. Atores Gestor Local, gestor externo, Presidente e Operacional sintéticos. Sua autenticação, CSRF e troca inicial usam os fluxos reais. Nenhum dado é institucionalmente autêntico.

## Recursos e execução

Serviços próprios: `sgt-dashboard-pg-proposta-20261111`, `sgt-dashboard-redis-proposta-20261111`, `sgt-dashboard-api-r4-homologacao-20261111`, `sgt-dashboard-vite-proposta-20261111`. Reutilizar; não iniciar duplicatas nem parar serviços de terceiros. A API roda uma cópia imutável do JAR identificada por hash, fora de `JAVA/target`, para que novo package não substitua o arquivo em uso. Serviços anteriores desta API foram parados, preservando seus artefatos. Limites de memória: PostgreSQL 768 MiB, Redis 256 MiB, API 2 GiB (heap 768 MiB/metaspace 256 MiB), Vite 1 GiB; suíte pesada até 2 GiB, Node heap 768 MiB, Maven 512 MiB, um worker/fork/pool 8. Fila global `~/.codex/tools/heavy-run.py`, suítes sequenciais, teto agregado autorizado 15 GB. Recibos de memória registram serviço/cgroup; medidas ausentes não são tratadas como pico zero.

Os comandos de produto são os de `.codex/validation.toml`. O helper ignorado `run-heavy.py` injeta exclusivamente parâmetros privados do laboratório; `--lab` usa a base de IT e `--lab --ui` a base de interface. `--prepare` é opt-in de fixtures JPA com guarda explícita de banco/role. Não é script de produção.

## Evidências e alcance

`smoke-api.json` reconcilia os cinco períodos reais, classifica sem inventar histórico, confere funil, escopo 403/sessão e módulos legados. `React/test/e2e/painel-desempenho-real.mjs` é o cenário UI para execução via cua_repl com Tab documentado e laboratório informado, sobre o oráculo da API real; sem mocks nem alteração de credenciais pelo navegador. Testes HTTP/JPA pertinentes ficam no Java e têm recibos próprios.

Fontes, assets/JAR, checks, pareceres Astra e homologação Sol precisam corresponder aos mesmos bytes na conclusão. Contagem de testes não equivale à homologação de todo catálogo histórico. Escopo: sistema em laboratório sintético desktop Chrome/Chromium; produção, Windows nativo, LAN externa, carga nacional, autenticidade institucional e PDF/UA integral dependem de evidência específica e não são inferidos deste laboratório.
