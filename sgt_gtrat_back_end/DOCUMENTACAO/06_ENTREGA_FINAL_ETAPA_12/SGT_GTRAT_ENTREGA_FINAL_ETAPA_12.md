# SGT/GTRAT — Entrega final da Etapa 12

## Objetivo concluído

A Etapa 12 encerra o backend Java antes do React. O fechamento foi feito sobre o levantamento canônico de 198 requisitos, mantendo a decisão definitiva de usar Java 17 e Spring Boot 3.x.

## Plataforma congelada

| Componente | Versão/decisão |
|---|---|
| Java | 17 (compilação com `--release 17`) |
| Spring Boot | 3.5.16 |
| Maven | 3.9.16 usado na validação |
| Banco | MySQL 8.4.11 |
| Schema de teste | sempre novo; `create-drop` na integração |
| Migração de banco antigo | inexistente e fora do escopo |
| Frontend | React será iniciado após esta entrega |

## Fechamentos técnicos desta etapa

- unificação da regra de uso de Cargo, Lotação e Turno ativos/inativos em fatos retroativos e futuros;
- bloqueio de inativação de catálogo com composição, aditamento, remanejamento ou alocação futura ainda não tratada;
- reativação do mesmo cadastro, preservando identidade, versão e auditoria;
- fotografia imutável dos nomes de Cargo, Lotação e Turno em composição, aditamento, remanejamento e transferência de gestão;
- remoção dos caminhos substitutos que calculavam ocupação por relações não canônicas; efetivo e ocupação passam por Vínculo/Alocação;
- auditoria sem rotina de exclusão física de registros publicados;
- atualização do rate limiter para a API vigente do Bucket4j;
- segurança adversarial para CORS, TRACE, sessão stateless e cabeçalhos;
- persistência real de LOB de relatório como `LONGBLOB` no MySQL;
- remoção de scripts de checkpoints superados e de build com frontend inexistente.

## Limites conscientes

O agregado operacional usado pela tela/lote de frequência continua existindo como chave de entrada desse módulo. Ele não é fonte de verdade para Contrato, Vínculo, Alocação ou ocupação: essas decisões são sempre reconstruídas pelo domínio temporal `Pessoa → Vínculo → Alocação`.

Não há promessa de 100% de cobertura de linhas. A garantia desta entrega é a cobertura auditada dos 198 requisitos, combinada com testes unitários, arquitetura, integração MySQL, concorrência e carga. Os percentuais JaCoCo reais estão documentados sem maquiagem.

## Resultado

O backend está apto a servir como contrato do React. Alterações incompatíveis na API congelada devem ser versionadas ou aprovadas como mudança de contrato, nunca introduzidas silenciosamente.
