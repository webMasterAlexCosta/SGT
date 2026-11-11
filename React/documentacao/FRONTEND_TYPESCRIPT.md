# React TypeScript e execução local

A implementação completa local foi autorizada após o piloto PostgreSQL e a verificação Redis. As restrições históricas da etapa de entrega de dependências não descrevem esta etapa.

Node 22.16+ ou 24 e npm 12.2.2. O Vite 8.2.4 instalado aceita Node ^20.19.0 ou >=22.12.0; o runtime local usado é Node 24.22.2. React, Router e demais versões existentes foram preservados. Somente TypeScript 5.9.3, @types/react 19.2.18 e @types/react-dom 19.2.7 foram adicionados.

O exemplo `.env.example` contém apenas configuração pública. Copie os valores públicos apropriados para seu ambiente privado de execução; nunca inclua senhas reais em VITE_*, que integra o bundle. A autenticação usa cookies e CSRF, e a revisão compartilhada de sessão invalida continuações de outra conta. A revisão no Storage/cookie público é somente sinal de invalidação, sem token nem autoridade.

`npm run dev` preserva HMR em http://127.0.0.1:5173, com proxy `/api` e `/config.json` para http://127.0.0.1:5000. A API Java executa separadamente.

`npm run typecheck` verifica os tipos; `npm run build` exige essa verificação antes do Vite. O build não comprova navegador ou integração de sessão real. As regressões Vitest devem rodar em lotes sequenciais de cinco ou dez arquivos neste ambiente compartilhado.

`npm run sync:java:check` confere o destino e o dist sem copiar. `npm run sync:java` copia explicitamente o dist compilado para `JAVA/src/main/resources/static`, sem apagar outros recursos Java. `npm run build:java` encadeia build e sincronização. Esta frente frontend não executa esses comandos de cópia: a integração final React → static → Maven/JAR cabe ao orquestrador.

Os contratos de `src/contracts/java.ts` espelham os DTOs e enums Java; a evidência desta execução mantém o mapa de fontes e seus hashes. `CivilDate` corresponde a LocalDate, `Instant` carrega offset, e LocalDateTime permanece distinto. `RequestOptions`, `HttpContext<T>` e `HttpError` descrevem o transporte normalizado: os serviços recebem dados ou contexto, nunca AxiosResponse.

O gate final usa `strict=true`, com `noImplicitAny=true` e `strictNullChecks=true`. DTOs de resposta e `Request*` são distintos: as restrições de presença do Java e seus defaults de desserialização permanecem no servidor. Formulários possuem seus próprios modelos de texto/IDs até serializar. As rejeições externas são unknown e seus campos são refinados antes da leitura; a normalização não substitui a causa original dos rethrows.

O build de laboratório recebe todas as VITE_* do exemplo por override somente no processo, com API `/api` e credenciais de teste desativadas. Nenhum .env real é reescrito. A geocodificação opcional preexistente usa Nominatim; ela é independente da API Java e não é invocada pelos testes ou pelo build.
