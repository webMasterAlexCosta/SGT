# SGT/GTRAT — Próximos passos para o React

O React pode começar consumindo a API congelada da Etapa 12. O frontend não deve duplicar regras temporais, permissões ou cálculo de situação: deve apresentar a verdade devolvida pelo backend.

## Sequência recomendada

1. criar o projeto React com TypeScript, roteamento e cliente HTTP central;
2. implementar autenticação, renovação/encerramento de sessão e tratamento comum de erros;
3. carregar `GET /api/usuarios/meu-perfil` e construir as rotas por perfil/permissão;
4. criar componentes de seleção para Gestão, Contrato, Pessoa, Vínculo e Alocação;
5. entregar primeiro as consultas de Contrato e posição temporal;
6. implementar fluxos mutáveis com prévia, confirmação de senha e idempotência;
7. implementar frequência, pendências e relatórios;
8. executar testes de contrato HTTP e jornadas ponta a ponta contra banco descartável.

## Regras do cliente

- enviar e receber datas negociais em `yyyy-MM-dd`;
- enviar instantes em ISO-8601;
- tratar `400`, `401`, `403`, `404`, `409`, `428` e `429` de forma centralizada;
- não inferir `FUTURO`, `VIGENTE` ou `ENCERRADO` no navegador;
- não reusar a prévia se o backend responder conflito de assinatura/impacto;
- não ocultar avisos de excedente, pendência ou relatório sujeito a revisão;
- nunca armazenar segredo, senha de confirmação ou conteúdo restrito em logs/localStorage;
- usar a origem React na allowlist `CORS_ALLOWED_ORIGINS`.

## Primeiro marco do frontend

O primeiro marco deve autenticar, exibir o perfil atual, listar Contratos no escopo e abrir a posição temporal de um Contrato. Isso valida sessão, autorização, CORS, datas e contrato JSON antes de construir telas de escrita mais caras.
