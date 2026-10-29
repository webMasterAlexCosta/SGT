# SGT/GTRAT — Leia-me da Etapa 12

Esta é a entrega de encerramento do backend Java. O projeto fica estabilizado em **Java 17**, **Spring Boot 3.5.16** e **MySQL 8.4**, com a API preparada para a futura aplicação React.

## Resultado

- 198 requisitos canônicos implementados, sem item parcial ou pendente;
- 396 testes unitários/arquiteturais aprovados;
- 22 testes de integração aprovados em MySQL 8.4.11 nativo e banco novo;
- 1 teste de carga aprovado com 450.821 operações e nenhuma falha;
- contrato da API congelado em 10/09/2026;
- exclusão física restrita a rascunho nunca publicado e dependências exclusivas de preparação;
- nenhum script de migração ou reconciliação de banco antigo.

## Ordem de leitura

1. `SGT_GTRAT_ENTREGA_FINAL_ETAPA_12.md` — escopo e decisões finais;
2. `SGT_GTRAT_MATRIZ_FINAL_198_REQUISITOS.md` — auditoria individual 198/198;
3. `SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_12.md` — testes, MySQL e carga;
4. `SGT_GTRAT_API_CONGELADA_BACKEND_JAVA.md` — contrato para o React;
5. `SGT_GTRAT_INVENTARIO_ROTAS_API_ETAPA_12.md` — inventário exaustivo de 281 verbo/rota;
6. `SGT_GTRAT_RESULTADO_ETAPA_12.md` — termo de encerramento;
7. `SGT_GTRAT_PROXIMOS_PASSOS_REACT.md` — início seguro do frontend.

## Execução local

1. Instale Java 17, Maven 3.9.x e MySQL 8.4.
2. Crie um banco vazio chamado `sgt_gtrat`.
3. Em `PROJETO_BACKEND`, copie `.env.example` para `.env` e substitua senhas e segredos.
4. Execute `mvn clean test`.
5. Execute `mvn spring-boot:run`.

O `.env` real, o banco, o diretório `target`, uploads e caches não integram o pacote.
