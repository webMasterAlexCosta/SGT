# SGT-GTRAT — Resultado da Etapa 10

**Data-base:** 09/09/2026  
**Objetivo:** concluir perfis, permissões e minimização de dados sem manter modelo paralelo antigo.

## 1. Modelo canônico de acesso

`UsuarioAplicacao` agora possui somente `PerfilAcesso perfil`, persistido na coluna `perfil`. O campo textual plural de autoridades foi removido do domínio, dos repositórios, dos DTOs, da autenticação e dos relatórios.

| Perfil | Escopo normal | Escrita negocial | Exposição pessoal padrão |
|---|---|---|---|
| `ADMINISTRADOR` | técnico/nacional | não | auditoria minimizada |
| `GESTOR_NACIONAL` | nacional | não | auditoria minimizada |
| `GESTOR_ESTADUAL` | estado vinculado | não | auditoria minimizada |
| `GESTOR_LOCAL` | Gestão Local vinculada | sim | completa no próprio escopo |
| `PRESIDENTE` | consulta institucional nacional | não | sem dados pessoais |
| `OPERACIONAL` | atribuição operacional | somente frequência permitida | operacional minimizada |

As authorities Spring Security são derivadas do enum (`PERFIL_ADMINISTRADOR`, `PERFIL_GESTOR_NACIONAL`, etc.); não constituem uma segunda fonte de verdade.

## 2. Separação entre administração e negócio

O Administrador permanece responsável por configuração técnica, cadastros estruturais e concessão/revogação de permissões. Ele não recebe escrita automática em contratos, empregados, vínculos, jornadas, lotações, calendários, ocorrências ou frequência negocial.

As verificações existem em duas camadas:

- matriz HTTP em `ConfiguracaoSeguranca`;
- validações de perfil e escopo nos serviços de domínio.

Assim, uma alteração de roteamento não transforma silenciosamente o Administrador, o Gestor Estadual, o Gestor Nacional ou o Presidente em responsável negocial.

## 3. Permissões excepcionais

Foi introduzido um agregado próprio, sem transformar exceções em perfis:

- `PermissaoExcepcional`: beneficiário, tipo, contrato ou Gestão Local, finalidade, justificativa, autorização administrativa, concedente, validade, revogação e versão otimista;
- `UsoPermissaoExcepcional`: trilha imutável de cada consumo;
- `PermissaoExcepcionalServico`: concessão/revogação exclusiva da administração, validação de escopo, expiração, prevenção de duplicidade e auditoria;
- endpoints REST de consulta, concessão, revogação e usos.

Tipos canônicos:

1. `CORRECAO_IDENTIFICADOR_CONTRATO`;
2. `REDUCAO_CONTRATUAL_EXCEPCIONAL`;
3. `TRANSFERENCIA_GESTAO_LOCAL`;
4. `ACESSO_DADOS_PESSOAIS_RESTRITOS`;
5. `INTERVENCAO_NEGOCIAL_TECNICA`.

Uma permissão deve estar ativa, não expirada, vinculada ao mesmo beneficiário e compatível com o contrato/Gestão Local da operação. O uso é gravado na mesma transação do ato protegido.

## 4. Correção de número e ano do contrato

Foi criado fluxo próprio para `PER-008`, sem sobrescrever silenciosamente a identidade anterior:

- somente Gestor Local responsável pelo contrato;
- contrato publicado e bloqueio pessimista de linha;
- permissão administrativa específica e vigente;
- referência documental publicada para o fato `CORRECAO_IDENTIFICADOR`;
- autorização enviada deve ser idêntica à da concessão;
- chave de idempotência global;
- evento temporal, fotografia e registro imutável de antes/depois;
- pesquisa pelo identificador antigo ou novo;
- auditoria geral e auditoria de uso da permissão.

## 5. Minimização de dados pessoais

`MinimizadorDadosPessoaisServico` centraliza a política de exposição:

- Gestor Local: dados completos dentro do próprio escopo;
- Operacional: nome e identificadores estritamente operacionais, com CPF mascarado;
- Administrador, Gestor Nacional e Gestor Estadual: visão de auditoria minimizada;
- Presidente: visão institucional sem campos pessoais.

A política foi aplicada a pessoas, empregados, vínculos, frequência e relatório de terceirizados. Endpoints genéricos de conta, endereço e telefone foram bloqueados para impedir consultas pessoais sem escopo.

Dados restritos adicionais exigem finalidade informada, permissão específica e registro de uso. Para o Administrador, o acesso técnico excepcional usa `INTERVENCAO_NEGOCIAL_TECNICA`; para os perfis de auditoria/consulta, usa `ACESSO_DADOS_PESSOAIS_RESTRITOS`.

## 6. Escopo temporal e documental

- autorização atual continua combinada com o escopo histórico do registro de frequência;
- Gestor Estadual é filtrado pelo estado atual e pelo snapshot histórico aplicável;
- Gestor Nacional possui leitura/auditoria nacional, sem escrita;
- Presidente consulta contratos e documentos negociais nacionais, mas não recebe módulos de pessoa/empregado;
- documentos restritos continuam sujeitos ao controle explícito implantado na Etapa 08.

## 7. Limpeza do modelo

Além do fechamento funcional:

- foram removidas referências de produção a `ROLE_*`, `roles`, `getRoles`, `setRoles` e consultas por roles;
- construtores alternativos do serviço de contrato foram removidos;
- tipo de contratação por texto livre deixou de ser aceito quando o catálogo canônico é obrigatório;
- fallbacks nulos do motor temporal e dos repositórios obrigatórios foram removidos;
- não foi criada rotina de migração de perfil, contrato ou dado antigo;
- a instalação validada parte de um banco novo e vazio.

## 8. Correções de infraestrutura descobertas

Durante a validação final foram corrigidos dois defeitos do pipeline anterior:

- o Failsafe passou a carregar o agente Byte Buddy, assim como o Surefire, permitindo a suíte MySQL completa;
- o import do `.env` foi ajustado para a sintaxe compatível com Spring Boot 3.5.16, e o `JWT_SECRET` de exemplo passou a satisfazer a validação mínima de 64 caracteres.

## 9. Conclusão

A Etapa 10 fecha `PER-001` a `PER-012`. O backend permanece em Java 17/Spring Boot 3.5.16, validado com MySQL 8.4.11 real. As pendências funcionais restantes pertencem às Etapas 11 e 12; o React continua posterior ao congelamento das APIs.
