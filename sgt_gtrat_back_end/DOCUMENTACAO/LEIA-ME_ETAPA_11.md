# SGT/GTRAT — Backend após a Etapa 11

Esta versão conclui a Etapa 11 do backend: relatórios administrativos versionados, indicadores temporalmente corretos e Central de Pendências.

## Base técnica obrigatória

- Java 17;
- Spring Boot 3.5.16;
- Maven 3.9.16;
- MySQL 8.4 em banco novo;
- nenhuma migração ou reconciliação de modelo legado;
- frontend React ainda não iniciado.

## Entregas principais

- Central de Pendências com origem, motivo, responsável, estado e escopo de bloqueio;
- pendências de retificação de frequência e fim natural de vigência;
- aviso `sujeito a revisão` e contagem de pendências nas consultas e exportações;
- emissões administrativas imutáveis, idempotentes e encadeadas por versão;
- distinção entre emissão original, retificada, vigente e superada;
- snapshot JSON comprimido, hash SHA-256 e metadados de autoria/período;
- consulta do que era conhecido até um instante e comparação com a verdade vigente;
- invalidação seletiva de emissões afetadas por retificações de frequência ou vínculo;
- escopo organizacional, minimização de dados pessoais e auditoria preservados.

## Validação

```bash
mvn clean test
```

Resultado desta entrega: **383 testes, 0 falhas, 0 erros e 0 ignorados**. O teste de persistência da Etapa 11 para MySQL está em `src/test/java/com/example/siggtrat/integration/Etapa11PersistenceIT.java`; a execução nativa fica para ambiente que permita abrir sockets, conforme as evidências da entrega.

Consulte `../DOCUMENTACAO/05_ENTREGA_FINAL_ETAPA_11/` para API, matriz, evidências e próximos passos.
