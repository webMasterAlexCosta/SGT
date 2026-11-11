**PENDENTE para estender a aprovação técnica5 e integrar o dist07.** Identifiquei um achado P2 que impede fechar a reconciliação no caso com rascunho.

**[P2] O rascunho pode sobrescrever os metadados oficiais recém-carregados.**  
Em [useLogicaFrequencia.ts:375](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/React/src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts:375), a carga retorna `{ ...linhaBase, ...rascunho }`. O rascunho armazena a linha inteira, incluindo `jaSalvo`, `frequenciaId`, `vinculoContratualId` e `identidadeCanonicaDisponivel`.

Após um cancelamento parcial, o GL pode editar uma linha ainda pendente e reprocessar o job. Quando essa linha for persistida, a nova recarga obterá os IDs corretos, mas o rascunho voltará a sobrepor `jaSalvo=false` e IDs nulos. A grade continuará mostrando **Pendente/associação indisponível**, apesar da frequência oficial retornada pelo backend.

O teste novo preserva rascunho de uma linha **já pertencente ao prefixo persistido**; por isso não detecta esse caso. A correção deve preservar valores editados e identidade da intenção, separando-os dos metadados oficiais atualizados. Falta regressão com rascunho numa linha pendente que passa a ser gravada pelo job. O achado é determinístico na composição dos objetos; não executei reprodução dinâmica nesta revisão.

A chave por ID/tentativa/status e o reaproveitamento da carga abortável são adequados nos demais caminhos examinados. Não confirmei outros achados P0–P3.

Conferi RED **21/25**, GREEN **25/25** e os105 recibos completos: **819 testes/105 arquivos**, sem divergências nas fontes/testes vinculados. Os433 arquivos de React/src estão vinculados. Strict/build07, dry-run e ZIP/CRC/SHA dos100 assets conferem. A verificação final encontrou **zero divergências em2.342 fontes,143 inputs e100 assets**. Java/POM permanecem inalterados; JAR `af2f8b5a…a04da983` ainda contém dist06.

Os novos resultados de jobs permanecem delimitados: controle padrão500 aprovado em109,455s; cancelamento/reprocessamento20+180 com limites superiores de61,297s/79,225s por fase. A recuperação antiga continua **FAIL no prazo de120s**. Esses resultados não comprovam a nova transição de grade no dist07.

Revisão somente leitura, sem suítes, edições, banco ou acesso a arquivos privados/logs/JFR bruto. Mantêm-se os pareceres estáveis anteriores. Após corrigir e verificar o P2, o incremento deve retornar a este revisor. **Não há homologação global, dos20 drivers/24 critérios, reinício completo ou LAN.**
