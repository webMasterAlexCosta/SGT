# Auditoria independente da implementação

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

Três revisores em agentes separados examinaram fontes, requisitos originais, critérios e oráculos. As correções propostas por um revisor receberam leitura cruzada de outro que não as propôs, além da revisão e execução pelo coordenador. A divisão cobre 68 requisitos/264 critérios de contratos, 59/136 de pessoas e 71/235 transversais: total 198/635. É revisão técnica independente dentro desta execução assistida; não auditoria humana externa certificada.

IDs revisados no hash atual: 198. IDs com defeito confirmado aberto: 0. Gate técnico sem defeitos confirmados abertos: True.

Revisões estáticas de arquivos idênticos podem manter sua proveniência. Resultados de execução só são aceitos com igualdade dos arquivos de compilação e das classes exercitadas. Entre candidatas06 e07 mudou somente o roteiro HTTP; a integral06/carga01 executou exatamente o mesmo Java e os mesmos testes compilados. A nova cadeia TCP foi executada por completo. As correções produtivas receberam reproduções permanentes e nova regressão; os arquivos e as tentativas rejeitadas continuam no histórico.

| Revisor | IDs | Parecer concluído | Parecer / SHA-256 |
|---|---|---|---|
| contratos | 68 | True | [Parecer](../auditoria_independente/candidata07/contratos/VEREDITO_FINAL.json) / `79ebb2db5cd1f9fb63445e4cff3f121c9d0ae4e0fae3edf19f5d2149f1250437` |
| pessoas | 59 | True | [Parecer](../auditoria_independente/candidata07/pessoas/VEREDITO_FINAL.json) / `de8630cc7f87aabc20c08f84e5911c1a73dff7eb5656473635e4568838da6b84` |
| transversais | 71 | True | [Parecer](../auditoria_independente/candidata07/transversais/VEREDITO_FINAL.json) / `63fbde67fd032bdc43fc0810021b7926c427ef05a859386830c39407948632c0` |

## Execuções HTTP próprias

Cada revisor escreveu e executou seu próprio roteiro no JAR real após a prova principal de reinício. Os três processos Python foram serializados. Os guards verificaram identidade do processo, socket e JAR; os resultados contêm status HTTP esperados e asserções de conteúdo, escopo e preservação. A conciliação relê as provas, seus hashes e roteiros, exige todas as asserções aprovadas e verifica que as execuções cabem entre a prova principal e a parada final. As consultas não alteram fatos negociais; login e logout podem alterar dados de sessão.

| Revisor | Requisições | Asserções | PID | Prova / SHA-256 |
|---|---|---|---|---|
| contratos | 28 | 56 | 112421 | [Execução](../auditoria_independente/candidata07/contratos/PROVA_INDEPENDENTE_TCP_02.json) / `3d6ff1a9936e25ec05536a1698bccf1060b43ef8316c14916e4b5d99fc1e8e10` |
| pessoas | 32 | 119 | 112421 | [Execução](../auditoria_independente/candidata07/pessoas/tcp_independente/EXECUCAO_INDEPENDENTE_TCP.json) / `88f1261d86baaec57187b249e5092b1df99ad82be25b96a0d6732ff8b6ec0475` |
| transversais | 35 | 161 | 112421 | [Execução](../auditoria_independente/candidata07/transversais/tcp_independente/EXECUCAO_INDEPENDENTE_TCP.json) / `6f5f05d3eac7bfd1c8bef346bef373c5aa59dc67618719f5b8851c93d896763c` |

Total adicional: 95 requisições / 336 asserções. Não são contadas como testes Java ou como novos requisitos aprovados.

Os seis bloqueios documentais não são declarados resolvidos por testes sintéticos. Cada ID mantém o enunciado original, condição de fechamento e fonte faltante na matriz final e em PENDENCIAS_FINAIS.md.

## Adendo do utilitário de distribuição

A candidata funcional07 e seus pareceres foram preservados. Depois desse fechamento, o utilitário de empacotamento recebeu a correção de identidade da suíte JUnit descrita na regressão. O gate compara todos os arquivos do manifesto auditável original e permite somente essa diferença expressamente identificada. Exige o novo SHA do empacotador nos controles executados e em duas notas de revisão, sem defeitos confirmados abertos. A fonte compilada, o JAR e os roteiros HTTP permanecem idênticos. O manifesto do pacote registra os bytes efetivamente distribuídos.

[evidencias/controle_roteiros/identidade_carga_01/ADENDO_APROVADO.json](../evidencias/controle_roteiros/identidade_carga_01/ADENDO_APROVADO.json)
