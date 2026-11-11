# Contratos API do candidato Java atual

O [inventário](API_ATUAL.json) contém 261 pares método/rota e 263 tipos Jackson, regenerados pelo contexto Spring do lote `it-backend-safety41-02`, Main35/compilação41. A rota nova é `POST /api/relatorios/exportacoes/{id}/cancelamento`. Metadados estão vinculados às fontes/recibo por SHA-256; não substituem prova comportamental.

JSON segue `RespostaApi<T>`; arquivos e respostas sem conteúdo conservam contratos próprios. Erros preservam status, código estável e correlação. O frontend deve conservar a intenção/dados em recusas, conflitos ou indisponibilidade, conforme o fluxo; annotations vazias não dispensam filtros e autorização dos serviços. O DTO de fornecedor possui Email/Size180, com e-mail opcional e normalização conferidos.

A concessão específica usa `ocorrenciaId` opcional/nullable, Administrador atual, beneficiário ativo, finalidade/validade/auditoria e prova histórica de todos os dias; concede somente leitura restrita. Escopo genérico permanece separado. [Decisão/contrato](CONCESSAO_LEITURA_OCORRENCIA.md) e [28 casos atuais](CONCESSAO_REAL.json).

Exportação permite cancelamento explícito pelo solicitante ainda autorizado, em fila/processamento, com estado terminal CANCELADO e retirada do arquivo tardio. Cancelar a requisição no navegador não cancela o processamento no servidor. [Contrato](CONTRATO_CANCELAMENTO_EXPORTACAO.md); integração React ainda pendente.

[Frequência](../../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md): N configurável/padrão30, prazo civil exclusivo D+N, sem reinício por retificação. N30 foi conferido com relógio semântico e materialização transacional; um JAR real com relógio físico/defaultN30 e cron de laboratório2s processou alvo e testemunha posterior, sem duplicação. Isso não prova cadência default1min ou virada física de meia-noite.

[HTTP/JAR](JAR_HTTP_REAL.json): 146 requisições/45 janelas, reinício do mesmo JARdcb. [Jobs](FECHAMENTO_REAL.json): quatro métodos atuais e recuperação em fases,14 controles/200 efeitos/tentativa2/retomada65.891ms; preparador não é aceite e fullJUnitExecuted=false. [Catálogo](JAVA_CHECKS.json) distingue bytes produtivos exatos e estágios anteriores; compatibilidade após os dois arquivos Main35 aguarda revisão independente15. [Candidato](CANDIDATO_JAVA_ATUAL.json). Chrome, React, LAN e homologação global continuam pendentes.
