# SGT-GTRAT — Entrega da Etapa 08

Data: **09/09/2026**  
Plataforma mantida: **Spring Boot 3.5.16 + Java 17**

## Resultado

A Etapa 08 implementa o repositório canônico de documentos comprobatórios para Contrato e fatos negociais, representando `ANEXO`, `SEI` ou `ANEXO_E_SEI`.

Principais garantias:

- os 10 requisitos `DOC-001` a `DOC-010` foram implementados;
- publicação de Contrato e aditamento exige anexo, referência SEI ou ambos;
- remanejamento mantém fonte opcional e auditável;
- o mesmo processo SEI pode referenciar vários Contratos;
- correção de SEI, acréscimo de anexo e troca de arquivo criam nova versão;
- versões publicadas incorretas são substituídas ou invalidadas, sem exclusão física;
- somente rascunho exclusivo e sem valor SEI externo pode ser removido;
- cada documento possui classificação `NEGOCIAL` ou `PESSOAL_RESTRITO`;
- conteúdo restrito exige finalidade e permissão conforme perfil, com auditoria de cada acesso;
- upload valida limite, extensão, MIME, assinatura binária, nome físico aleatório e SHA-256;
- eventos documentais possuem cadeia de integridade SHA-256 e idempotência.

## Validação executada

- **342 testes aprovados**;
- **0 falhas, 0 erros e 0 ignorados**;
- **15 testes específicos da Etapa 08**;
- **T01–T90 aprovados**;
- **505 fontes Java principais** e **111 arquivos Java de teste** compilados com `release 17`;
- JAR executável gerado com `Build-Jdk-Spec: 17` e `Spring-Boot-Version: 3.5.16`.

O teste MySQL/Testcontainers não pôde ser executado neste ambiente porque o Docker não está instalado. Antes da homologação, execute em banco descartável, vazio e exclusivo desta versão:

```bash
./mvnw -B -ntp -Pintegration verify
```

## Documentos desta entrega

- `docs/SGT_GTRAT_ETAPA_08_FEITO_E_PROXIMA_ETAPA.md`;
- `docs/SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_08.md`;
- `docs/SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_08.md`;
- `docs/SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_08.md`;
- `docs/SGT_GTRAT_API_DOCUMENTOS_ETAPA_08.md`.

## Próxima etapa

Prosseguir para a **Etapa 09 — Frequência integrada ao modelo temporal**. O React continua adiado até o congelamento das APIs do backend.
