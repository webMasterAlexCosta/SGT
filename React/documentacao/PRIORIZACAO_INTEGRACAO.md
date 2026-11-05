# Priorização React → Java congelado

Esta matriz contém apenas as **lacunas observadas**, não substitui a matriz completa dos 198 requisitos/635 critérios a ser reconstruída pelo Codex.

| Requisito / módulo | Ponto React atual | Contrato Java a conferir | Trabalho no Fedora |
|---|---|---|---|
| CON-023 | `src/services` de exceções contratuais e `PainelExcecoesContrato` | `ContratoControlador`, `TransferenciaGestaoContratoRequisicao`, rotas de prévia/cancelamento | DTO, prévia com token/impacto, mapas, confirmação, cancelamento e E2E |
| CAT-004 | Contratos/documentos, fotografias históricas parciais | `ProvaHistoricaContratoControlador`, origens e fotos de composição | Provas parciais/integrais, município desconhecido vs ausente, efeitos tardios |
| CAT-007 | Serviços temporais não encontrados | `DisponibilidadeCatalogoControlador` | Consultar períodos e prévia/confirmar transições |
| COL-010 | `ServicoPessoa.js` e correções de identidade | `ProvaNomePessoaHistoricoControlador` | Provas de nome, mesmo titular/vínculo, intervalos e conhecimento |
| HIS-001 | `ServicoContrato.consultarPosicaoTemporal` | `ContratoControlador`/`PosicaoTemporalContratoDTO` | Ampliar visualização bitemporal, contrato e origens canônicas |
| HIS-006 | Documentos e publicação existentes | `ContratoControlador`, `ProvaHistoricaContratoControlador` | Preservar publicação original, diferenciar parcial/integral e carência documental |
| Demais 192 A | Telas RH, administração, gestão, relatórios | Todos os controllers e DTOs | Matriz de 198/635, regressão e autenticação/autorização |
| Infra de testes | Vários scripts npm referenciam arquivos não fornecidos | Testes Java existentes, MySQL 8.4, Redis | Recriar testes/fixtures/configs verdadeiros, executar E2E |

Não há prova de que os seis requisitos E tenham documentos autênticos novos. A decisão documental só muda após autenticação efetiva da fonte e competência de quem a emitiu.
