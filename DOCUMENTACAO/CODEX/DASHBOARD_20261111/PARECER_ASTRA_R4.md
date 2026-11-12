# Revisão independente Astra — R4

Chat local visível: `01a1cd45-040f-7762-9f55-281305a2db98`. Execução confirmada no `turn_context`: `gpt-6-astra`, `xhigh`. Parecer emitido em 11/11/2026. Revisão somente leitura, sem suítes ou navegador pelo revisor.

**Decisão: aprovado quanto à revisão de fonte. Nenhum achado P0–P3 nos 12 caminhos alterados/adicionados nem nas interações examinadas.**

Manifesto R4: 80 arquivos, SHA-256 `baba3f76a8d02cf1137d78739fa1befb861e4126c226ab4c5b38f3045fc2120e`, sem divergências. `git diff --check` sem problemas.

- Vencimento usa a vigência do motor canônico sobre contratos filtrados/autorizados, intervalo inclusivo referência → +30 dias. DTO, tabela e exportações recebem a data. IT confere +30 e +31 dias.
- Erro 500 reproduzido no refresh Hibernate: a recarga desanexa somente usuário/vínculos, preserva `PESSIMISTIC_READ`, compara atividade, perfil, raízes e pares lotação/turno. O comando reautoriza o contexto histórico com o usuário recarregado. A edição administrativa conserva lock de escrita do usuário.
- Log R4 conferido: 7 testes unitários e 8 ITs aprovados, sem falhas, erros ou skips, incluindo solicitação com vínculos e auditoria.

Limites no instante do parecer: novo unitário de revogação ainda sem execução comprovada; revogação concorrente durante espera não demonstrada; frontend com seis testes de preparação aprovados e cobertura em andamento. JAR/assets/E2E finais ainda precisavam ser vinculados aos bytes R4. Este parecer não substitui a homologação final Sol, nem aprova produção ou toda a matriz histórica.
