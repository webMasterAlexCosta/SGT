# Registro de execução — plano PLANO_EXECUCAO.md

- Origem recuperada com igualdade dos35003 arquivos comuns;95 arquivos acrescentados pelo kit, incluindo JAR e static. Java original preservado.
- Manifesto baseline separado, sem herdar aprovações anteriores.
- Instância MySQL8.4.11 na porta23306 e Redis8.4.8 na26379, somente loopback. Três catálogos vazios.
- Baseline integral01 em execução serial; nenhuma modificação produtiva realizada.
- Revisores iniciais:66 requisitos contratos,55 pessoas,77 transversais;198/635 no total.

## Baseline em execução

-493 testes unitários concluídos sem falhas/erros/ignorados.
-A primeira rodada de integração detectou erro de infraestrutura em AuditoriaFinalCorrecaoCpfControlesHttpIT: SELECT negado em performance_schema.data_locks. Concedida leitura somente ao usuário executor na instância isolada. A rodada e o erro serão preservados, e a classe será repetida. Não foi alterado schema ou dado da aplicação.
-Os três revisores entregaram rastreio estático individual dos198 requisitos/635 critérios, sem importar aprovações anteriores. Novo revisor adicional procura falhas de autorização e cache sobre a mesma fonte congelada.

## Decisões registradas

-A fonte ativa permanece sem alterações Java durante a baseline; novos REDs ficam em reviews/ até sua execução centralizada.
-A implementação será dividida por domínios com arquivos/métodos acordados. Nenhum agente executa suítes pesadas em paralelo. O parecer final usará outra fonte congelada e revisão independente das implementações.
-Prova documental complementar não autoriza trocar silenciosamente valor já comprovado. Deve usar fonte explícita, vínculo canônico, autorização específica, prévia, versão esperada, idempotência e instante de conhecimento do servidor.
-Ausência de versão original e ausência do próprio evento original são cenários distintos. Uma versão de ADI nunca será promovida a prova de publicação.
-Null opcional documentado e ausência de prova serão diferenciados por declarações explícitas de configuração comprovada; uma grade semanal desconhecida não será convertida em jornada de zero horas.

## Baseline concluída

- baseline_integral_01:1607execuções (493unitários+1114integrações),1606aprovadas,0falhas,1erro de infraestrutura,0ignorados; sources_unchanged=true.
- Commit origem3d1a877c91a40e6fd5a3a0581b0c424c3a332914; commit dos REDsb4fdde05bffc262cd5888076c8e6289d3ad8649f.
-12classes novas copiadas por nome explícito;45cenários propostos. Reexecução de AuditoriaFinalCorrecaoCpfControlesHttpIT incluída após concessão de leitura dos locks.
