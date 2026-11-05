# AGENTS.md — SGT/GTRAT Backend (desenvolvimento, pré-auditoria)

## Propósito

Concluir rigorosamente os 198 requisitos originais, eliminar código de compatibilidade legado e preparar um congelamento técnico para posterior auditoria independente. O sistema não foi a produção: rotas/implementações obsoletas podem ser eliminadas após demonstrar que a funcionalidade canônica preserva as regras originais. Guardar matrizes, histórico e fotografias de negócio; não confundir dados históricos com código legado.

## Autoridade das fontes

1. `docs/LEVANTAMENTO_REQUISITOS.md` e decisões posteriores explicitamente comprovadas.
2. Contratos de API válidos e coerentes com regras originais.
3. Código atual e testes existentes, tratados como evidências a investigar.
4. Relatórios e matrizes anteriores, tratados como referências históricas.

Em ambiguidade, consultar documentação e evidenciar a dúvida, nunca inventar fato ou regra. Status histórico 192 A / 6 E não prova a versão em edição. Nem número de testes nem JaCoCo prova um requisito integral.

## Persistência: obrigatoriedade absoluta

- MySQL real para persistência, transações, concorrência e rollback.
- Hibernate como implementação JPA e único gestor de schema; `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`.
- Entidades JPA/Hibernate com anotações de índices, constraints, colunas, chaves e relacionamentos.
- Proibidos Flyway, Liquibase, `schema.sql`, SQL manual de DDL, criação/alteração/remoção de tabelas fora do Hibernate e soluções que forcem schema via testes.
- `update` não remove automaticamente objetos físicos obsoletos: testar sobre schema MySQL isolado vazio para verificar as entidades atuais; não eliminar DDL antigo por SQL manual.

## `.env` e segurança

`.env` é PERMITIDO para desenvolvimento e testes. Manter `.env.example` seguro, e não divulgar credenciais privadas, tokens JWT reais nem senhas do MySQL em ZIPs, relatórios, logs ou commits. O `.env` recebido foi sanitizado. Validar autorização no backend, horizontal/vertical/temporal, PII, anexos e arquivos. Nenhum frontend deve ser modificado nesta missão; documentar novos contratos React.

## Refatoração e remoção do legado

Localizar rotas compatíveis, DTOs antigos, regras duplicadas, serviços alternativos, flags mortas, testes e configs obsoletos. Para cada item mapear requisito e consumidor, substituir por serviço/rota canônico testado, remover de fato, atualizar matriz de endpoints e registrar motivo/evidência em `MATRIZ_LEGADO_REMOVIDO.md`. Excluir histórico de domínio ou documentos de autoria NÃO está autorizado. Não manter compatibilidade apenas porque uma versão antiga existiu; aplicação nunca foi produção.

## Testes, documentação e controle de recursos

Primeiro baseline/hash e testes, depois reproduzir RED, corrigir, provar GREEN, regressão relacionada e final em MySQL. Ler persistência em novas transações; testar operações concorrentes em transações reais. JAR deve ser iniciado por HTTP real e reiniciado. Preservar XMLs novos vinculados ao hash da fonte, não misturar `target` antigo.

Orçamento agregado de memória considera cgroup efetivo e máquina com 32 GB; não paralelizar suítes pesadas; limitar forks, heaps e pools; persistência em lotes `flush()/clear()` com atomicidade preservada. Não usar mocks para substituir MySQL nas provas de dados.

## Congelamento e auditoria

Somente com 198/198 requisitos em A e testes correspondentes confirmados, gerar candidato final imutável com manifestos SHA-256, JAR e matrizes. Qualquer B/C/D/E mantém o trabalho em checkpoint técnico com pendências, não como backend concluído/congelado. Revisor independente de preferência somente leitura deve auditar o código do mesmo hash, cada um dos 198 requisitos e todas as rotas canônicas. Defeito encontrado reprova o candidato: corrigir em outro estado, repetir testes, recongelar com novo hash e auditar novamente. Não declarar 198/198 se a fonte ausente impedir critério original. Separar completude de código, completude de dados históricos e homologação independente.

## Início rápido

Ler `../../PROMPTS/01_PROMPT_CODEX_CONCLUIR_198_LIMPAR_LEGADO_CONGELAR_AUDITAR.md` quando disponível na extração do kit. O inventário de entrada e as provas históricas ficam em `../../HISTORICO/` (não devem sobrescrever resultados atuais). No projeto individual fora do kit, mantenha uma cópia deste AGENTS.md e consulte a documentação original dentro de `docs/`.
