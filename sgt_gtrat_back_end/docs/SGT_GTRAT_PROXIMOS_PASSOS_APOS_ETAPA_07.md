# SGT-GTRAT — Próximos passos após a Etapa 07

Data-base: **09/09/2026**

## 1. Validação obrigatória antes da homologação

Executar o perfil MySQL/Testcontainers em máquina com Docker:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco descartável, vazio e exclusivo desta versão. Não importar automaticamente banco de etapas anteriores.

## 2. Etapa 08 — Documentos, anexos e SEI

Objetivo: tornar reproduzível a fonte comprobatória de cada fato negocial sem exclusão física do histórico publicado.

Implementar:

1. agregado documental capaz de representar `ANEXO`, `SEI` ou `ANEXO_E_SEI`;
2. vínculo de uma fonte a Contrato, aditamento, remanejamento, transferência, Vínculo, Alocação e demais fatos aplicáveis;
3. obrigatoriedade de fonte para publicação de Contrato e aditamento;
4. fonte opcional, mas auditável, para remanejamento;
5. versão documental com autor, instante, motivo e hash do conteúdo;
6. invalidação e substituição sem apagar arquivo publicado;
7. retificação de referência SEI preservando o valor anterior;
8. relação de um mesmo processo SEI com vários Contratos;
9. classificação `NEGOCIAL` e `PESSOAL_RESTRITO`;
10. autorização e auditoria de todo acesso a conteúdo pessoal/restrito;
11. metadados seguros: nome original, tipo MIME, tamanho, checksum e armazenamento lógico;
12. bloqueio de extensão, MIME ou tamanho não permitido, sem confiar apenas no nome do arquivo.

### Critério de saída da Etapa 08

- nenhum documento publicado é apagado fisicamente pelo fluxo comum;
- toda substituição ou retificação preserva a cadeia anterior;
- Contrato e aditamento publicados apontam fonte válida;
- referência SEI é versionada;
- acesso a documento restrito deixa trilha consultável;
- testes cobrem autorização, integridade, versão, substituição e vínculo múltiplo de SEI;
- regressão total e novo checkpoint permanecem verdes.

## 3. Etapa 09 — Frequência temporal

Após a Etapa 08, migrar a frequência para resolver na data do lançamento:

- Contrato válido;
- Vínculo válido;
- Alocação válida;
- Gestão Local histórica;
- Jornada pertencente à data de início, inclusive quando cruza meia-noite.

Também implementar janela operacional de sete dias em `America/Sao_Paulo`, fechamento no início do oitavo dia, retificação formal do Gestor Local e repercussão controlada de fatos retroativos.

Essa etapa fecha ALO-006 no consumidor, avança VIN-014 e prepara o ZIP Java C.

## 4. Checkpoint Java C

Ao concluir as Etapas 08 e 09, gerar o ZIP Java C contendo:

- Alocação temporal;
- transferência de Gestão;
- documentos, anexos e SEI;
- frequência temporal;
- testes unitários e de integração;
- matriz atualizada de aderência.

## 5. Etapas posteriores

| Etapa | Conteúdo principal |
|---:|---|
| 10 | Perfis, permissões granulares e minimização de dados |
| 11 | Relatórios temporais, indicadores, auditoria consolidada e Central de Pendências |
| 12 | Testes finais, carga, concorrência, idempotência e fechamento do Java |
| React | Somente após congelamento das APIs do backend final |

## 6. Regras que permanecem fixas

- Spring Boot 3.5.16 e Java 17;
- banco novo por versão durante a evolução;
- fatos históricos não são apagados nem reescritos silenciosamente;
- inativação administrativa não equivale a encerramento;
- mudança de Alocação não equivale a desligamento/recontratação;
- transferência de Gestão exige mapeamento explícito;
- frequência e relatórios ainda não podem ser declarados totalmente temporais;
- o React não deve ser iniciado antes do backend Java consolidado.

