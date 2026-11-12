# Dashboard de gestão — proposta aprovada para implementação

Especificação atual: decisões humanas de 11/11/2026 e mockup desktop v3 aprovado, consolidadas no [adendo do plano](PLANO_GESTAO_REFERENCIA.md#adendo-de-execução-aprovado--11112026). As adaptações preliminares que deixavam prazo/revisão indisponíveis foram substituídas. Homologação desta alteração depende de recibos e pareceres novos.

## Calendário e recortes

| Seleção | Intervalo inclusivo do KPI |
|---|---|
| DIARIO (padrão) | A própria referência |
| SEMANAL | Segunda-feira ISO até a referência |
| MENSAL | Primeiro dia do mês até a referência |
| TRIMESTRAL | Primeiro dia do trimestre civil até a referência |
| ANUAL | Primeiro dia do ano até a referência |

Referência padrão: ontem, preservando o painel existente. Sem futuro, últimos dois anos. Comparação parcial: mesmo número de dias decorridos, limitado ao fim do calendário anterior. Calendários completos comparam calendários completos, com datas explícitas; não alegam mesma duração quando diferentes.

Evolução é contexto histórico: 14 dias, 12 semanas, 12 meses, oito trimestres ou cinco anos. Cada calendário recebe corte equivalente ao ponto atual quando parcial. Zero de registros significa nenhum fato encontrado, sem comprovar atividade. Heatmap diário é contexto dos sete últimos dias; semanal usa dias ISO transcorridos, mensal blocos 1–7, 8–14 etc., trimestral/anual meses transcorridos. Não há futuro.

## Indicadores

| Indicador | Regra e base |
|---|---|
| Total de registros | Frequências operacionais VALIDA: PRESENCA, PRESENCA_PARCIAL, ATRASO, TR, FALTA, ATESTADO, SUSPENSAO; data e escopo fotografados |
| Dentro / fora do prazo | Primeiro timestamp comparado ao prazo de lançamento capturado; edição não reclassifica. Sem prazo capturado é indeterminado. Dentro + fora + indeterminado = total |
| Percentual dentro do prazo | Dentro / (dentro + fora); classificados e indeterminados identificados separadamente. Sem classificados: sem base |
| Em revisão | Uma frequência válida do período com solicitação PENDENTE do Operacional, ainda não decidida pelo Gestor Local. Pode sobrepor prazo; não é soma adicional ao total nem a revisão histórica automática |
| Cobertura | Lançados válidos entre esperados / esperados pela grade, folgas aprovadas e calendário aplicável, na referência |
| Ocupação | Vinculados com alocação vigente / previsto, conforme fonte canônica de efetivo na referência. Déficit/excesso separados |
| Qualidade cadastral | Vínculos com admissão preenchida no cadastro consultado / vínculos vigentes da referência. Não mede toda qualidade nem afirma cadastro histórico imutável |
| Pendências | Esperados sem lançamento válido; não são faltas. Taxa do intervalo usa soma pendentes / soma esperados |

Metas propostas para avaliação da chefia: cobertura 100% após o prazo, ocupação 100% do previsto e admissão 100% preenchida. Nenhuma é apresentada como meta institucional homologada. Base zero ou elegibilidade incompleta não gera percentual inventado. A grade e o município da alocação são fotografados; o motor temporal contratual delimita gestão e vigência. Consulta reconstrói a verdade conhecida no momento, incluindo alterações documentadas.

Funil: vinculados → esperados → lançamentos entre esperados; pendências separadas. Fotografia da referência independente da granularidade do período. Heatmap/tabela têm página de até 20 unidades, sem limitar KPIs e evolução globais. Prestador é a identificação cadastrada no contrato consultado, assim explicitada.

Vencimento contratual: a tabela de contratos e os relatórios exibem o término reconstruído pelo motor temporal na referência. O alerta conta somente contratos vigentes já autorizados e abrangidos pelos filtros, com término no intervalo inclusivo referência → referência + 30 dias. O limiar de 30 dias preserva a regra já existente no painel. Data ausente fica indisponível, sem estimativa.

Solicitação/decisão de revisão revalida o executor após os bloqueios negociais: perfil, raízes de escopo, vínculos de acesso e situação ativa. A recarga localizada evita o refresh em cascata de grafo inicializado que falhou no Hibernate da stack fixada, sem limpar o contexto de persistência ou descartar auditoria. O contexto histórico da frequência é autorizado novamente antes da mutação.

## Prazos e revisão

[Regra vigente de frequência](../../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md): `FREQUENCIA_JANELA_OPERACIONAL_DIAS=7` e `FREQUENCIA_EDICAO_HORAS=72`, positivos no `.env` Java. Lançamento: sete datas civis, D conta como primeiro dia; fecha no início de D+7 em São Paulo. Edição: 72 horas desde o primeiro lançamento, limite exclusivo e sem reinício por retificação. Pode ultrapassar o prazo do primeiro lançamento.

Novos registros preservam os prazos capturados; acervo sem captura não recebe classificação histórica. Estado FECHADA persistido não reabre por configuração. Futuro é vedado inclusive ao gestor. Exceção gerencial mantém escopo histórico, justificativa, idempotência, versão, autoria original e auditoria.

Operacional solicita conferência; Gestor Local confere, executa eventual retificação pelo fluxo existente e conclui ou rejeita com justificativa. Solicitação e decisão são eventos independentes, versionados e idempotentes. Revisão automática de validade histórica permanece independente.

## API e proteção

`GET /api/painel/analitico`: granularidade DIARIO, referência ontem, contrato/prestador/tipo de unidade/unidade opcionais, comparar=true, página=0. `PainelAnaliticoDTO` possui intervalos, recorte, catálogos autorizados, totais, comparação, evolução, indicadores com bases, heatmap, diagnóstico, metas propostas, alertas e metodologia.

`GET /api/painel/analitico/registros`: referência/início/fim obrigatórios, até 366 dias; contrato/prestador/tipo/unidade/métrica/página opcionais. Métricas registros, presencas, ausencias, dentro, fora, revisao, indeterminado. Retorna identificação mínima (sem nome/CPF), 20 por página. Presidente permanece em agregados.

`GET /api/frequencias/{id}/revisao`, `POST` no mesmo caminho e `POST .../decisao`: leitura, solicitação e decisão autorizadas no servidor. Mutações exigem confirmação de segurança, versão, motivo e chave de idempotência.

Todas as leituras revalidam escopo. 400 para parâmetros inválidos, 403 para filtro não autorizado, 409 para concorrência, indisponibilidade distinta de zero. Transação analítica read-only REPEATABLE_READ, timeout 90s. Sem cache analítico compartilhado; transporte e Redis dos módulos existentes preservados.

## Interface e integração

Visão Desempenho inicial, demais modos e funcionalidades preservados. Componentes compactos conforme mockup, estados de consulta/erro/base zero, cancelamento individual e geração de sessão. 403 não encerra sessão. Detalhamento preserva recorte; ESC/foco e teclado conferidos na homologação.

XLSX/PDF incluem recorte, períodos, contagens, classificação, bases, metas propostas, metodologia e página das unidades/heatmap. Footer usa SVG locais Devicon (React, TypeScript, Vite, Java, Spring, PostgreSQL e Redis), com licença MIT distribuída. Nenhum ícone depende de rede em execução.

Entrega: HMR React e API real separados; no checkpoint, React dist → static Spring → Maven/JAR → SPA/assets/rotas profundas/API/Redis, com hashes vinculados. Laboratório privado e suítes sequenciais, teto agregado 15 GB. Sem implantação produtiva. Commit/push somente após revisão Astra/xhigh e homologação Sol/xhigh aprovadas, conforme autorização humana.
