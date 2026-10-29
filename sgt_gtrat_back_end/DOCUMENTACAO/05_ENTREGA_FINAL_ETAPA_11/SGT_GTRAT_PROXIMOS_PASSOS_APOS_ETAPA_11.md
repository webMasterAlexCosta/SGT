# SGT/GTRAT — Próximos passos após a Etapa 11

**Data-base:** 10/09/2026

## 1. Etapa 12 — fechamento do backend Java

Ordem recomendada dos gates:

1. executar `Etapa11PersistenceIT` e toda a integração em MySQL 8.4 com banco vazio;
2. validar carga e volume ampliados em relatórios, frequência e Central de Pendências;
3. testar concorrência e idempotência, especialmente emissão simultânea e transições de pendência;
4. executar cenários adversariais de segurança, indisponibilidade e isolamento organizacional;
5. executar a regressão integral em Java 17;
6. revisar individualmente os 44 requisitos parciais e 10 pendentes;
7. publicar a matriz final dos 198 requisitos e congelar os contratos HTTP.

## 2. Casos nativos prioritários

- unicidade concorrente de `chaveIdempotencia` por usuário;
- concorrência entre invalidação e nova emissão da mesma série;
- leitura/gravação do snapshot GZIP em LOB e conferência SHA-256;
- scheduler de fim natural com múltiplas Gestões Locais;
- paginação e filtros com volume representativo;
- transações de retificação que geram pendência e superam emissão;
- escopo estadual/local nas consultas JPA;
- restauração do conteúdo histórico após reinício do processo.

## 3. Regras que não podem regredir

- Java 17 e Spring Boot 3.5.x;
- banco novo durante o desenvolvimento, sem reconciliação automática;
- Gestor Local continua sendo o escritor negocial;
- permissões excepcionais continuam distintas de perfis;
- dados pessoais e documentos restritos permanecem minimizados/protegidos;
- verdade efetiva atual e verdade conhecida em cada emissão permanecem separadas;
- nenhuma versão publicada é apagada;
- pendência bloqueia apenas o recorte necessário.

## 4. React — somente depois do gate final

O frontend React continua deliberadamente adiado. Depois do congelamento das APIs na Etapa 12, a sequência inicial deve ser:

1. autenticação, perfil e autoridades;
2. guards de escopo e minimização;
3. Central de Pendências;
4. relatórios e comparação de versões;
5. avisos de revisão e downloads;
6. fluxos temporais restantes.

Isso evita construir o cliente sobre contratos ainda sujeitos a mudanças durante a homologação do backend.
