# SGT-GTRAT — Etapa 09 concluída e decisão para a Etapa 10

**Data:** 09/09/2026  
**Checkpoint:** Frequência integrada ao modelo temporal

## 1. O que foi feito

### 1.1 Contexto canônico

Foi criado `ResolvedorContextoTemporalFrequenciaServico` para reconstruir a frequência pela data negocial. O fluxo localiza a Pessoa pelo CPF, seleciona o Vínculo válido, reconstrói Contrato e Vínculo, reconstrói a Alocação e fixa a Gestão Local histórica.

`Empregado` permanece como identificador de compatibilidade da API atual, mas seus campos atuais de Lotação/Jornada/Cargo/Contrato não são a fonte canônica da frequência.

### 1.2 Estado negocial e três dimensões

A entidade `Frequencia` passou a persistir:

- `estadoNegocial`: `ABERTA` ou `FECHADA`;
- `validadeOperacional`: `VALIDA`, `SUJEITA_REVISAO` ou `INVALIDA_COBERTURA`;
- `estadoAuditoria`: original/alterada/retificada/sujeita a revisão;
- Pessoa, Vínculo e Alocação canônicos;
- versão negocial;
- autoria original e última retificação;
- snapshots históricos de Contrato, Gestão, Lotação, Jornada e Cargo;
- `@Version` para concorrência otimista.

### 1.3 Janela temporal

`PoliticaTemporalFrequenciaServico` usa exclusivamente `America/Sao_Paulo`:

- data da frequência = dia 1;
- Operacional edita até o fim do 7º dia;
- no início do 8º dia o registro é fechado;
- a regra antiga baseada em horas desde `updatedAt` foi removida do fluxo negocial.

`FechamentoAutomaticoFrequenciaNegocialServico` persiste o fechamento e registra evento auditável.

### 1.4 Gestor Local

Qualquer escrita do Gestor Local é tratada como retificação formal:

- exige justificativa;
- exige chave de idempotência;
- usa hash da intenção para distinguir repetição exata de reutilização indevida da chave;
- cria evento `RETIFICACAO_GESTOR`;
- bloqueia nova edição pelo Operacional;
- pode atualizar o snapshot canônico para a verdade temporal corrigida.

Administrador técnico não recebe escrita negocial automaticamente.

### 1.5 Auditoria

Foi criada `EventoFrequencia` e `AuditoriaFrequenciaServico` com:

- antes/depois;
- autor;
- instante de registro;
- motivo;
- versão;
- chave de idempotência e hash da intenção;
- hash anterior e hash do evento;
- validação da cadeia SHA-256.

A API passa a expor `GET /api/frequencias/{id}/eventos`.

### 1.6 Retroatividade

`RevalidacaoFrequenciaTemporalServico` foi conectado a Vínculo, Alocação, Contrato e Aditamento.

Quando a história muda:

- o lançamento original é preservado;
- não há reassociação automática para outro Vínculo/Alocação;
- perda de cobertura gera `INVALIDA_COBERTURA`;
- divergência de contexto gera `SUJEITA_REVISAO`;
- reativação de cobertura não restaura validade silenciosamente;
- correção passa ao Gestor Local.

### 1.7 Consumidores oficiais

Painéis, efetivo e relatórios atuais foram ajustados para não considerar como verdade oficial frequência cuja `validade_operacional` não seja `VALIDA`.

## 2. Defeitos encontrados durante a integração MySQL

A validação real encontrou e permitiu corrigir:

1. query derivada inválida em `CalendarioEventoRepositorio` (`lotacaoId` não era atributo direto da entidade); foi substituída por JPQL explícito com `e.lotacao.id`;
2. scanner do smoke test procurava `*Repository.class`, enquanto o projeto usa `*Repositorio.class`;
3. testes de integridade usavam strings maiores que o contrato real de `status_empregado.descricao`;
4. gerador de argumentos de smoke test não suportava entidades JPA e records de parâmetros; passou a gerar referências fictícias adequadas.

Após as correções, a integração MySQL ficou verde.

## 3. Evidências finais

- 359 testes JUnit aprovados;
- 16 testes específicos Etapa 09 aprovados;
- 18 testes MySQL aprovados;
- T01–T110 aprovados;
- 517 fontes principais e 112 testes compilados em `release 17`;
- JAR executável gerado.

## 4. Dependências legítimas futuras

- `FRE-011`: aviso e quantidade de pendências na emissão de relatórios — Etapa 11;
- `FRE-012`: versionamento formal da emissão anterior de relatório — Etapa 11;
- `VIN-014`: frequência concluída; reflexo completo em relatórios — Etapa 11;
- Testcontainers real — antes da homologação, em máquina com Docker;
- empacotamento executado por JDK 17 real — antes da homologação.

## 5. Próxima etapa

Iniciar **Etapa 10 — Perfis, permissões granulares e minimização de dados**. Não iniciar React.
