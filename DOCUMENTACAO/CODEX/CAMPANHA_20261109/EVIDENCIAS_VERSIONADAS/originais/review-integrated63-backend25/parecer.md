**APROVADO para o recorte integrado25 do JAR08. Homologação global: PENDENTE.**

Não identifiquei novos achados P0–P3 de implementação ou inconsistências invalidantes nas provas examinadas. A revisão ocorreu em **gpt-6.1-sol/xhigh**, somente leitura.

Conferi novamente os **2.541 arquivos de fonte e 28 entradas**, sem divergências. O JAR mantém SHA-256:

`f9d0a761f139094ce1db585a1cc818843482f003c247c6ba03b66ebf3af9f4c6`

Os 823 arquivos de classe correspondem aos gates Java18/24; bibliotecas e assets conferem com o vínculo frontend11.

As novas provas permitem aprovar:

- **AUD-006:005/006:** responsabilidade de todas as revisões ativas, preservação da fotografia/autoria original, tratamento pelo responsável atual e retirada do aviso após resolução. Camada: PostgreSQL real com serviços e MockMvc, conforme a [prova de responsabilidade](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/pendencia-responsabilidade-integrated08-proof.json).
- **CON-012 e RST-005:** interrupção física e recuperação automática com dois executores, prefixo preservado e 200 frequências/eventos únicos. A aprovação respeita a execução em fases e os parâmetros de teste; preparador não conta como aceite nem como JUnit completo. [Prova de recuperação](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/recovery-integrated08-proof.json).
- **RST-003/004:** reinício do laboratório e do mesmo JAR, com igualdade das 72 tabelas, 7.332 linhas, schema, índices, sequências e seis arquivos duráveis, comparados antes de novo login. As 61 verificações Chrome complementam a persistência funcional. A divergência do hash JSON foi reproduzida exclusivamente pela ordem das chaves, mantendo valores e contraprovas de corrupção. [Prova de reinício](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/full-restart-integrated08-proof.json).
- **Aditamento, pendência, confirmação e paginação:** os novos recibos sustentam seus predicados declarados, incluindo 403/409, formulário preservado, cancelamento sem mutação e universo completo de empresas/pessoas. As fases preparatórias permanecem identificadas.
- **LAN:** roteiro no outro computador confirmado pelo usuário e corroborado por origem HTTP distinta. O screenshot inicial não foi tratado como captura da jornada autenticada.

Aceito o reaproveitamento delimitado de **CON-003/005/009/010**, do parecer15, e **ROP-021/026**, de `it-frequencia-entradas37-01`. Também são reutilizáveis as provas compatíveis de PDF completo, recusas e respostas tardias. Elas conservam seus JARs e camadas originais.

Para o aceite global, permanecem estas **lacunas de demonstração**, sem defeito de código comprovado:

| Predicado | Menor ação necessária |
|---|---|
| **ROP-042 — documentos e opções/cargos tardios** | Completar a prova com mais de cinco documentos e opção/cargo em página posterior: navegação, filtro, totais e IDs sem perda/duplicação. A prova de PDF com 41 registros já sustenta seu componente de exportação; não precisa ser repetida apenas por mudança do número do JAR. |
| **E2E-10 — N30, turno noturno e autorização histórica na UI** | Vincular as provas backend já válidas e completar a observação Chrome do plantão e seu recorte histórico: data de referência, alocação/autorização e prazo N30 coerentes com o DTO. Não é necessário esperar trinta dias nem atravessar fisicamente a meia-noite para afirmar esse comportamento. |
| **E2E-24 — falha parcial sem sucesso indevido** | Completar um caso de carga/operação parcialmente malsucedida na API real, verificando mensagem, preservação do contexto/formulário e ausência de lista vazia ou sucesso indevidos. Cancelamento, recusas e respostas antigas já possuem provas aproveitáveis. |

Essas pendências correspondem ao [fechamento funcional atual](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/DOCUMENTACAO/CODEX/CAMPANHA_20261109/FECHAMENTO_FUNCIONAL_ATUAL.md:26). Depois, falta reconciliar cada predicado aplicável com sua camada, recibo e parecer. Os contadores históricos **não representam centenas de funcionalidades ausentes**, nem concedem aprovação coletiva.

Limites: análise das fontes e evidências preservadas, sem novas suítes ou execução do sistema pelo revisor. A aprovação cobre o laboratório sintético e os recortes descritos; não comprova homologação global, migração ou operação de produção.
