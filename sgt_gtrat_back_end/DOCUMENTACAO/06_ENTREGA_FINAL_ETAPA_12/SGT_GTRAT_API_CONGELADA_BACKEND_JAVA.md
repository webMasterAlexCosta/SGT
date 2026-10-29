# SGT/GTRAT — API congelada do backend Java

**Congelamento:** 10/09/2026  
**Consumidor previsto:** aplicação React  
**Base padrão:** `http://<host>:5000`  
**Prefixo funcional:** `/api`

O apêndice `SGT_GTRAT_INVENTARIO_ROTAS_API_ETAPA_12.md` lista todas as combinações verbo/rota detectadas nos controladores Java na data do congelamento.

## Regras transversais congeladas

- JSON UTF-8, salvo download/stream de arquivo;
- datas negociais em `yyyy-MM-dd`, mês em `yyyy-MM` e instantes em ISO-8601;
- autenticação por sessão/JWT emitida pelo módulo de autenticação;
- escopo e permissão são sempre decididos no backend;
- mutações sensíveis anotadas com `@ExigirSenha` exigem confirmação de senha;
- paginação Spring usa `page`, `size` e `sort` nos endpoints paginados;
- respostas de sucesso não binárias usam o envelope `status`, `message`, `data`, `path`, `requestId` e `meta`;
- erros usam, no mínimo, `timestamp`, `status`, `error` e `message`; alguns incluem `code` e `fields`;
- códigos relevantes para o React: `400`, `401`, `403`, `404`, `409`, `428` e `429`;
- CORS aceita somente as origens explícitas em `CORS_ALLOWED_ORIGINS`; TRACE é negado;
- a situação temporal vem calculada pelo backend e não deve ser inferida pelo cliente.

## Autenticação e perfil

| Verbo | Rota | Finalidade |
|---|---|---|
| POST | `/api/autenticacao/autenticar` | iniciar sessão |
| GET | `/api/autenticacao/sessao?touch=false` | consultar sessão |
| POST | `/api/autenticacao/renovar-sessao` | renovar sessão |
| POST | `/api/autenticacao/encerrar-sessao` | revogar/encerrar sessão |
| POST | `/api/autenticacao/alterar-senha-inicial` | substituir senha inicial |
| GET | `/api/autenticacao/superintendencias` | opções públicas de cadastro |
| GET | `/api/autenticacao/gestoes-locais?superintendenciaId=` | opções públicas de cadastro |
| POST | `/api/autenticacao/cadastrar` | cadastrar acesso |
| POST | `/api/autenticacao/recuperar-senha/solicitar` | solicitar recuperação |
| GET | `/api/autenticacao/recuperar-senha/validar?token=` | validar token |
| POST | `/api/autenticacao/recuperar-senha/confirmar` | confirmar recuperação |
| GET | `/api/usuarios/meu-perfil` | perfil e escopo do usuário atual |
| GET/POST/DELETE | `/api/perfil/2fa/**` | ciclo de 2FA |
| POST | `/api/perfil/alterar-senha` | alterar senha |
| POST | `/api/perfil/validar-senha` | validar confirmação |

## Contrato e composição

| Verbo | Rota | Finalidade |
|---|---|---|
| GET | `/api/contratos` | listar no escopo |
| GET | `/api/contratos/opcoes` | lista leve com ocupação canônica |
| GET | `/api/contratos/{id}` | detalhe |
| GET | `/api/contratos/{id}/posicao-temporal?data=&conhecidoAte=` | reconstrução bitemporal |
| GET | `/api/contratos/{id}/eventos` | linha de eventos |
| GET | `/api/contratos/{id}/eventos/precedencias` | relações de precedência |
| GET | `/api/contratos/{id}/versoes` | versões negociais |
| GET | `/api/contratos/{id}/transferencias-gestao` | transferências históricas |
| POST | `/api/contratos` | criar rascunho |
| PUT | `/api/contratos/{id}` | editar rascunho |
| POST | `/api/contratos/{id}/publicar` | publicar |
| POST | `/api/contratos/{id}/inativar` | inativação administrativa |
| POST | `/api/contratos/{id}/reativar` | reativação administrativa |
| POST | `/api/contratos/{id}/encerrar-antecipadamente` | encerramento antecipado |
| POST | `/api/contratos/{id}/cancelar-futuro` | cancelar contrato futuro |
| POST | `/api/contratos/{id}/anular` | anular com alcance efetivo |
| POST | `/api/contratos/{id}/transferir-gestao` | transferência excepcional |
| DELETE | `/api/contratos/{id}` | descartar somente rascunho nunca publicado |
| GET/POST/PUT/DELETE | `/api/contratos/{contratoId}/composicoes/**` | composição do rascunho |
| GET | `/api/contratos/{contratoId}/composicoes/excedentes` | ocupação/excedente atual |

## Aditamentos e remanejamentos

| Verbo | Rota |
|---|---|
| GET | `/api/contratos/{contratoId}/aditamentos` |
| POST | `/api/contratos/{contratoId}/aditamentos/previa` |
| POST | `/api/contratos/{contratoId}/aditamentos` |
| POST | `/api/contratos/{contratoId}/aditamentos/{aditamentoId}/retificar` |
| POST | `/api/contratos/{contratoId}/aditamentos/{aditamentoId}/cancelar` |
| GET | `/api/contratos/{contratoId}/remanejamentos` |
| POST | `/api/contratos/{contratoId}/remanejamentos/previa` |
| POST | `/api/contratos/{contratoId}/remanejamentos` |
| POST | `/api/contratos/{contratoId}/remanejamentos/{remanejamentoId}/retificar` |
| POST | `/api/contratos/{contratoId}/remanejamentos/{remanejamentoId}/cancelar` |

A confirmação deve reutilizar a assinatura/token da prévia. Se os dados ou o impacto mudarem, o backend responde conflito e o React deve solicitar nova prévia.

## Pessoa, Vínculo e Alocação

| Verbo | Rota | Finalidade |
|---|---|---|
| GET/POST | `/api/pessoas` | listar/criar Pessoa |
| GET | `/api/pessoas/{id}` | detalhe minimizado |
| GET | `/api/pessoas/{id}/dados-restritos` | dados sujeitos a permissão |
| PUT | `/api/pessoas/{id}/dados-opcionais` | atualizar dados opcionais |
| POST/GET | `/api/pessoas/{id}/correcoes-identidade` | corrigir/listar correções |
| GET | `/api/vinculos-contratuais?contratoId=` | listar Vínculos |
| GET | `/api/vinculos-contratuais/{id}` | detalhe do Vínculo |
| GET | `/api/vinculos-contratuais/{id}/eventos` | eventos do Vínculo |
| POST | `/api/vinculos-contratuais` | operação integrada Pessoa/Vínculo/Alocação |
| GET | `/api/vinculos-contratuais/pessoas/{pessoaId}/posicao?data=&conhecidoAte=` | posição histórica da Pessoa |
| GET | `/api/vinculos-contratuais/{id}/alocacoes?mes=` | períodos de Alocação |
| GET | `/api/vinculos-contratuais/{id}/alocacoes/posicao?data=&conhecidoAte=` | posição da Alocação |
| GET | `/api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao?data=` | seleção encadeada válida |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-inicio` | prévia de início |
| POST | `/api/vinculos-contratuais/{id}/retificar-inicio` | retificar início |
| POST | `/api/vinculos-contratuais/{id}/previas/alocacoes` | prévia de Alocação |
| POST | `/api/vinculos-contratuais/{id}/alocacoes` | nova faixa de Alocação |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/retificar` | retificar faixa |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/cancelar-programacao` | cancelar faixa futura |
| POST | `/api/vinculos-contratuais/{id}/alterar-matricula` | alterar matrícula por evento |
| POST | `/api/vinculos-contratuais/{id}/cancelar-programacao` | cancelar Vínculo futuro |
| POST | `/api/vinculos-contratuais/{id}/desligar` | desligar |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-desligamento` | prévia de retificação |
| POST | `/api/vinculos-contratuais/{id}/retificar-desligamento` | retificar desligamento |
| POST | `/api/vinculos-contratuais/{id}/cancelar-desligamento-programado` | cancelar desligamento futuro |

## Frequência

| Verbo | Rota |
|---|---|
| GET | `/api/frequencias?data=&lotacaoId=&jornadaId=&contratoId=&gestaoLocalId=&superintendenciaId=` |
| GET | `/api/frequencias/mensal?mes=&ano=` |
| GET | `/api/frequencias/intervalo?inicio=&fim=` |
| GET | `/api/frequencias/{id}/eventos` |
| POST | `/api/frequencias` |
| POST | `/api/frequencias/lote` |
| POST/GET | `/api/frequencias/fechamentos/**` |
| GET/POST/PUT/DELETE | `/api/frequencias-adicionais/**` |
| GET/POST/PUT/DELETE | `/api/frequencias-excecoes/**` |

## Documentos, pendências e relatórios

| Família | Rotas congeladas |
|---|---|
| Documentos | `/api/contratos/{contratoId}/documentos`, detalhe, eventos, arquivo, upload, retificação SEI, substituição, invalidação, permissões e acessos |
| Pendências | `GET /api/pendencias`, `GET /api/pendencias/resumo`, início de tratamento e confirmação de resolução |
| Relatórios administrativos | prévia, emissões, consulta de emissão, corte de conhecimento, comparação vigente e listagem em `/api/relatorios/administrativos` |
| Relatórios operacionais | filtros, frequência consolidada, terceirizados, absenteísmo, horas, exportações e arquivos em `/api/relatorios` |

## Demais famílias públicas do backend

| Domínio | Base |
|---|---|
| Catálogos | `/api/cargos`, `/api/lotacoes`, `/api/jornadas`, `/api/tipos-contratacao`, `/api/motivos-desligamento` |
| Organização | `/api/superintendencias`, `/api/gestoes-locais` |
| Usuários e permissões | `/api/usuarios`, `/api/permissoes-excepcionais`, `/api/registros-auditoria` |
| Painel | `/api/painel`, `/api/painel/calculos` |
| Calendário | `/api/calendario-eventos` |
| Governança de terceiros | `/api/governanca-terceiros` |
| Operação de colaboradores/frequência | `/api/empregados`, `/api/historicos-empregados`, `/api/status-empregados`, `/api/ocorrencias` |
| Cadastros administrativos restritos | `/api/agencias`, `/api/bancos`, `/api/contas`, `/api/enderecos`, `/api/telefones`, `/api/tipos-conta`, regras adicionais de Jornada |
| Diagnóstico | `/api/cache/status`, `/api/sistema/configuracoes`, `/config.json` |

Os endpoints de contas, endereços e telefones estão negados pela política HTTP atual. Eles não devem orientar novas telas React até decisão funcional explícita.

## Política de evolução

Este documento congela rota, verbo, envelope, autenticação e semântica temporal. Adicionar campo opcional ou nova rota pode ser compatível. Remover/renomear campo, mudar tipo, alterar verbo/rota, relaxar permissão ou mudar a interpretação temporal é mudança incompatível e exige versionamento ou registro formal de quebra.
