# INTEGRAÇÃO FUNCIONAL FUTURA — NÃO EXECUTAR NA ETAPA ATUAL

A prioridade solicitada agora é obter **somente o pacote completo de dependências Fedora**, para anexar ao ChatGPT e armazenar na Biblioteca.

O conteúdo abaixo é o prompt anteriormente criado para a **etapa posterior** de integração do frontend com o backend congelado. Não executá-lo antes de devolver as dependências.

---

# PROMPT PARA CODEX NO FEDORA — SGT/GTRAT REACT + JAVA CONGELADO

> Cole integralmente este texto no Codex trabalhando na pasta que contém os DOIS ZIPs de entrega. A tarefa é executar implementação, não elaborar outro prompt. Não invente evidências.

## MISSÃO

Atue como implementador sênior React/JavaScript, Vite, frontend corporativo, integração Spring Boot, segurança, consultas temporais e governança documental. Conclua o React SGT/GTRAT existente para atender aos contratos REST reais e aos 198 requisitos (635 critérios aplicáveis) do **backend Java final congelado**. Não reconstrua indiscriminadamente o frontend; preserve o que funciona e corrija/complete os serviços e telas incompatíveis. Execute testes reais, auditoria e entregue ao final exatamente dois ZIPs (React e Java/documentação), cada um com seus relatórios pertinentes dentro, sem criar arquivos ZIP de saída adicionais.

## FONTES E DIRETÓRIOS

Os dois ZIPs atuais são:
- `SGT_GTRAT_REACT_FEDORA_DEPENDENCIAS.zip`: raiz `SGT_GTRAT_REACT_FEDORA/frontend` (fonte React 1.4.1D, build local, `node_modules` Linux e documentos).
- `SGT_GTRAT_JAVA_CONGELADO_DOCUMENTACAO.zip`: raiz `SGT_GTRAT_6_E` (`backend`, `bin`, documentação histórica, matrizes, auditoria, evidências e demais arquivos da entrega verificada).

Na primeira execução, extraia-os em uma nova pasta de trabalho e confirme todos os hashes. **Não copie o dist para `backend/src/main/resources/static`, não execute `sync:java` nem `build:java`**, não altere `pom.xml`, fontes, entidades, controllers, DTOs, propriedades, testes ou o JAR congelado. O SHA-256 esperado do JAR é:

`08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d`

Se divergir, interrompa o uso do backend não autenticado e registre a diferença. O JAR em `SGT_GTRAT_6_E/bin/sig-gtrat-backend-0.0.1-SNAPSHOT.jar` é o artefato imutável de referência. As senhas e arquivos `.env` privados não estão distribuídos: configure *externamente* as credenciais de teste. Jamais introduza dados reais no MySQL de ensaio.

## PREPARAÇÃO DO FEDORA — EXECUTAR

1. Confirmar Linux x86_64, Node 22.16.x (ou versão compatível com Vite 8) e npm 10.x. Se necessário, utilizar nvm. Verificar Java, MySQL 8.4 e Redis reais para os testes de integração.
2. Entrar em `SGT_GTRAT_REACT_FEDORA/frontend` e executar `bash scripts/preparar-fedora.sh`. O pacote já contém os `node_modules` Linux disponíveis, com versões fixadas no `package.json` e *build Vite aprovado nesta entrega*. O script cria um `.env` local sanitizado se necessário, roda quatro testes de preparação e build; personalize o `.env` para o domínio e a API do ambiente de teste.
3. O pacote **não** contém `package-lock.json`, porque o ambiente que o criou não acessava `registry.npmjs.org`. Com internet no Fedora, usar `npm install --no-audit --no-fund` para completar as seis dependências de desenvolvimento e gerar o `package-lock.json`; depois validar com `npm ci --no-audit --no-fund`. Não substituir versões testadas indiscriminadamente, não rodar `npm audit fix --force` automaticamente, nem confiar em códigos de saída verdes de scripts vazios. Examinar incompatibilidades reais antes de ajustar versões.
4. Verificar `npm run diagnostico:dependencias`; ele deve passar depois de completar as dependências. Restaurar ou implementar de verdade configs e testes ausentes antes de executar `npm test`. **Nenhum resultado histórico de testes React está disponível no pacote recebido.**
5. Não incorporar credenciais em `VITE_*`, assets, prints, relatórios ou bundle. `VITE_SHOW_TEST_CREDENTIALS=false`. Usar `VITE_API_URL=/api` com proxy da instância Java; alinhar `DEV_PROXY_TARGET` ao host/porta efetivos do backend. O domínio de email institucional de exemplo é *fictício* e precisa ser ajustado ao ambiente autorizado.
6. Executar React no Vite durante desenvolvimento, publicar por servidor estático ou reverse proxy em produção. Encaminhar `/api` e `/config.json` para o JAR imutável preservando CSRF, cookies, headers e políticas de segurança. Nunca modificar o Java para servir a nova interface.

## BASELINE, FONTE AUTORITATIVA E SEGURANÇA

Criar checkpoint do React recebido e manifesto SHA-256 de Java e React, registrar a árvore completa e o estado real. Ler todos os controllers, DTOs, validadores, enums, anotações de autorização e serviços Java relevantes. Extrair, a partir da fonte real, método/path, payload/response, datas, paginação, status, X-User-Password, CSRF, idempotência, versão esperada, finalidade, escopo e erros. Documentação histórica e relatórios orientam a comparação, mas não substituem a leitura do Java congelado.

Preservar arquitetura existente: `src/core/http.js`, `src/core/http/*`, módulos `src/modules/*`, serviços `src/services/*`, CSS Modules e navegação. É permitido refatorar, criar componentes e serviços React, adicionar testes e migrar seletivamente para TypeScript, sem alterar Java. Nenhum dado de negócio deve ser considerado persistido apenas por alteração de estado local. Qualquer endpoint inexistente deve gerar lacuna explícita; é proibido mockar sucesso de API.

Regras do banco: exclusivamente Hibernate/JPA define o schema com `spring.jpa.hibernate.ddl-auto=update`, `spring.sql.init.mode=never`; sem `CREATE TABLE`, `ALTER TABLE`, `schema.sql`, Flyway ou Liquibase. Testes de persistência/concorrência devem usar MySQL 8.4 real e Redis real, sem H2 ou mocks substitutivos. Usar DB e credenciais sintéticos isolados; limitar forks e memória e executar suítes pesadas sequencialmente.

## LACUNAS COMPROVADAS DO REACT RECEBIDO — CORRIGIR PRIMEIRO

1. **CON-023**: `ServicoExcecoesContrato.js`, `ServicoDominioExcecoesContrato.js` e `PainelExcecoesContrato` não cobrem integralmente a prévia e o cancelamento, nem `tipoContratacaoDestinoId`, `tokenPrevia` e `confirmarImpacto` no payload. Confrontar o DTO Java `TransferenciaGestaoContratoRequisicao` e as rotas `POST /api/contratos/{id}/transferencias-gestao/previa`, `POST /api/contratos/{id}/transferir-gestao`, `POST /api/contratos/{id}/transferencias-gestao/{transferenciaId}/cancelar`. Implementar autorização, versão/impacto, mapas, retificação, cancelamento futuro e histórico.
2. **CAT-004 e HIS-006**: faltam fluxos completos de prova histórica contratual, prévia/confirmação, origem canônica, publicação integral/parcial e preservação das fotografias. Usar controlador `ProvaHistoricaContratoControlador` e `POST/GET /api/contratos/{contratoId}/provas-historicas`, incluindo `/previa`. Nunca reconstruir publicação original com estado mestre atual nem confundir grade e município.
3. **CAT-007**: implementar serviços e interfaces para `DisponibilidadeCatalogoControlador`: `GET /api/catalogos-temporais/{tipo}/{id}/disponibilidade`, `POST /api/catalogos-temporais/{tipo}/{id}/transicoes/previa` e `POST /api/catalogos-temporais/{tipo}/{id}/transicoes`. Diferenciar atividade atual, fato retroativo, disponibilidade comprovada, corte de conhecimento, idempotência, versão e recusa atômica.
4. **COL-010**: completar provas históricas de nome para a mesma Pessoa e Vínculo, sem duplicar identidade. Usar `ProvaNomePessoaHistoricoControlador` e as rotas `/api/pessoas/{pessoaId}/provas-nome/previa`, `/api/pessoas/{pessoaId}/provas-nome`, `/api/pessoas/{pessoaId}/nome-historico`, conforme métodos existentes. Distinguir `nomeAtual`, `nomeFotografia`, correção material, mudança legal, versões e reemissões afetadas.
5. **HIS-001**: preservar o `ServicoContrato.consultarPosicaoTemporal` existente e completar telas por data efetiva/corte de conhecimento, fontes, composições, quantitativos, habilitação, publicações, ADI/REM e transferências. Não reconstruir lacunas a partir de estado atual.
6. **Requisitos remanescentes**: reconciliar **todos** os módulos RH, Pessoas/Vínculos, contratos, permissões, frequência, ocorrências, auditoria, downloads, relatórios e painel. Paginação desktop padrão de cinco itens por página, inclusive Pessoas/Vínculos (antes 20). Tratar 401/403/409/412/428 e respostas reais do Java; downloads/autorização; E2E de refazer prova municipal após frequência fechada.
7. **Testes incompletos**: `package.json` do ZIP original referenciava scripts/configurações Vitest, testes e utilitários que não acompanhavam os fontes. Recupere-os se disponíveis em outra fonte autêntica; do contrário, implemente novos testes sem fingir que os anteriores foram executados. Não relaxe asserções apenas para obter GREEN.
8. **Dependências locais**: quatro componentes foram adaptados de `react-icons` (ausente) para `FileSpreadsheet` e `FileText` em `lucide-react`, mantendo as ações Excel/PDF. Mantenha os imports corrigidos ou substitua por pacote real apenas com justificativa e testes. Pins locais: React/ReactDOM 19.2.5, Vite 8.2.2, @vitejs/plugin-react 6.1.1, Axios 1.20.0, Router 7.18.3, Lucide 1.11.0, Recharts 3.8.1. Ver inventário em `documentacao/DEPENDENCIAS_LOCAL.md`.

## CRITÉRIOS FUNCIONAIS E TESTES — SEM RESULTADOS INVENTADOS

Mapear os **198 requisitos e 635 critérios originais** (cobertura de interface, HTTP, segurança, teste e prova documental), usando matrizes e documentação incluídas no ZIP Java. Registrar exatamente quais critérios são somente backend, quais precisam de React e quais dependem de documentos autênticos. Não declarar 198/198: seis requisitos mantêm dependências **E documentais** até que fonte legítima real seja autenticada; testes sintéticos não substituem isso. Preservar os 192 anteriormente A no alcance demonstrável.

Para cada defeito: reproduzir RED permanente, capturar falha; aplicar correção React mínima e correta; GREEN; conferir dados reais; regressão focal e integral. Executar unitários, React Testing Library, Vitest e Playwright Chromium (instalar no Fedora se necessário). Não usar fixtures de sucesso como substituto da integração. Priorizar E2E:

- Sessão, CSRF, cookies, permissões e conflitos.
- Transferência prévia → mapa por origem → confirmação → alocação → comprovação posterior de município, com data efetiva e corte de conhecimento distintos.
- Município desconhecido versus ausência comprovada, mapa convergente, frequência fechada, recálculo operacional e fotografia histórica imutável.
- Nome atual/nome histórico, provas de catálogo, publicação original versus aditamento/remanejamento.
- Documento parcial/integral, idempotência, versões/tokens obsoletos, recusas por autorização e revogação com lock real.
- Cinco registros/página, download, relatórios, erros HTTP, reload do navegador e reinício do JAR.

Para validação: iniciar JAR idêntico em ambiente isolado, executar React via navegador real, consultar MySQL/Redis, parar Java, fotografar schema e dados, reiniciar exatos mesmos bytes, comparar persistência **antes de novas requisições** e repetir consultas. Os relatos históricos 1.822/1.848 testes Java são apenas referência; declare somente resultados realmente executados nesta rodada. Documentar todos os comandos, contagens e erros.

## INTERFACE CORPORATIVA DESKTOP

Preservar visual, navegação e arquitetura existente quando corretos. Desktop somente. Paginação padrão cinco itens, formulários compactos, filtros, estados de erro/carregamento, ESC para fechamento seguro de diálogo, confirmação de alterações não salvas, bloqueio de duplo envio, efeitos transacionais exibidos somente após resposta Java, acessibilidade, preferências de visualização quando aplicáveis. Em históricos, separar claramente **data efetiva**, **corte de conhecimento**, **fotografia**, **estado atual** e **fonte comprovada**. Nenhuma operação sem autorização; não ocultar falhas com UI otimista incorreta.

## AUDITORIA E ENTREGA

A revisão deve ser independente do implementador, por cópia congelada SHA-256 e confronto dos requisitos/contratos. Corrigir defeitos detectados antes de encerrar. Os relatórios exigidos devem ser incluídos **dentro dos dois ZIPs finais**, na pasta `documentacao`/`relatorios`: matrizes 198/635, endpoints/DTOs, antes-depois, RED/GREEN, testes/HTTP/E2E, segurança, regressão, auditoria, hashes, guia Fedora, limitações e pendências documentais.

Gerar **exatamente dois ZIPs finais**, um frontend React completo com fontes, testes, lockfile gerado corretamente, build, documentação e dependências compatíveis; outro com a mesma árvore Java congelada, JAR idêntico e documentação histórica/final, sem alterações de byte aos fontes ou JAR. Não redistribuir `.env` com senhas; preserve apenas exemplos sanitizados. Verificar CRC e hashes, integridade e ausência de segredos privados. Usar `node_modules` Linux desta entrega como ponto de partida, e `npm install` para completar no Fedora sem atualizar versões desnecessariamente.

**ENCERRAMENTO OBRIGATÓRIO:** implementar realmente, executar o máximo de testes viáveis, auditar e entregar os dois ZIPs com evidências. Se algum bloqueio persistir, entregar o máximo tecnicamente validado, identificar exatamente a dependência externa e não inventar comprovação.
