# Concorrência, transações e memória

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

A comprovação funcional usa disputas sobre recursos reais: lock de contrato, uso/publicação de catálogo, unicidade de CPF e número contratual, capacidade e piso de efetivo, idempotência e produção concorrente de pendências. Os oráculos exigem estado e trilha após commit/rollback; número de threads ou ausência de exceção não bastam.

No CPF, o observador vincula ENGINE_TRANSACTION_ID às conexões via INNODB_TRX e confirma espera no índice único de pessoa.cpf antes de liberar a primeira transação. O intervalo de 200 ms permite atualização do cache InnoDB; a conclusão depende da espera observada, não de presumir concorrência por um sleep.

## Classes com cenários de coordenação ou idempotência

A seleção abaixo facilita localizar evidências. Nem todo método de uma classe selecionada é uma corrida. A associação funcional exata está nos oráculos da matriz e nos corpos dos testes.

| Classe | Casos | Aprovados | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| AuditoriaFinalCorrecaoCpfControlesHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaFinalPendenciaFimControlesHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaInstrumentosConcorrentesHttpIT | 3 | 3 | 0 | 0 | 0 |
| CalendarioSemContratoConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT | 12 | 12 | 0 | 0 | 0 |
| ComposicaoPublicacaoConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| ConclusaoFrequenciaIdempotenciaOperacionalHttpIT | 7 | 7 | 0 | 0 | 0 |
| DocumentosCadeiaConcorrenciaNovaAuditoriaHttpIT | 1 | 1 | 0 | 0 | 0 |
| EmissaoRetificacaoConcorrenciaAuditoriaHttpIT | 2 | 2 | 0 | 0 | 0 |
| FechamentosTransacoesHttpIT | 7 | 7 | 0 | 0 | 0 |
| FrequenciaConcorrenciaContratoHttpIT | 2 | 2 | 0 | 0 | 0 |
| IdempotenciaFontesFrequenciaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasMatriculaConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PreauditoriaPainelRetificacaoConcorrenteHttpIT | 1 | 1 | 0 | 0 | 0 |
| RevisaoEmissaoIdempotenciaConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| AutenticacaoSessaoUnicaIT | 1 | 1 | 0 | 0 | 0 |
| VinculoAlocacaoConcorrenciaIT | 3 | 3 | 0 | 0 | 0 |

## Carga sintética

A carga separada exercita contadores em configuracao_sistema com conexões reais e verifica que os incrementos confirmados coincidem com o total persistido. Não representa benchmark HTTP da aplicação, latência em produção ou garantia de escala ilimitada. Configuração e métricas observadas:

```text
SIG GTRAT - TESTE DE CARGA DO BANCO
Gerado em: 2026-10-31T04:15:21.361938474Z
Banco: MySQL nativo via INTEGRATION_MYSQL_URL
Usuários virtuais: 24
Duração configurada: 15s
Chaves concorridas: 50
Percentual de escrita: 20.00%

Operações: 153613
Leituras concluídas: 122884
Escritas concluídas: 30729
Falhas: 0
Taxa de erro: 0.0000%
Throughput: 10220.96 ops/s
P50: 0ms
P95: 10ms
P99: 27ms
Incrementos persistidos: 30729
Incrementos esperados: 30729

Limites:
P95 <= 2000ms
erro <= 0.5000%
throughput >= 10.00 ops/s
```

## Orçamento e consumo medido

Uma suíte pesada de cada vez; Maven 512 MiB de heap e 256 MiB de metaspace; único fork de testes 768 MiB/256 MiB; pool JDBC de oito conexões; buffer pool MySQL 256 MiB; Redis 64 MiB. O executor possui limite cgroup de 8 GiB, menor que os 32 GB da máquina informada pelo usuário.

| Rodada | Amostras | Máx. soma RSS Java/MySQL/Redis (MiB) | Máx. cgroup com cache (MiB) | Delta memory.max | Delta OOM kill | JSONL SHA-256 |
|---|---|---|---|---|---|---|
| regressao_integral_06 | 322 | 2798.81 | 8192.0 | 6533 | 0 | b29958ff6067618ff360d08e357f91c74d95080969c2622fbb905a498f99d73d |
| carga_final_01 | 18 | 2199.94 | 7659.09 | 0 | 0 | ddb11934a429953dfac8da3267156158f15b0a96d572c65b5bbac3aebce65d16 |

Amostragem de /proc e cgroup a cada5s pelo supervisor. Maven e seu único fork são duas JVMs da mesma suíte. RSS e cgroup incluem categorias diferentes; cgroup também inclui cache de arquivos. Amostragem não garante capturar todo pico instantâneo.

O cgroup inclui cache de arquivos e outros processos do executor: sua pressão de memória não pode ser substituída pela soma RSS de Java/MySQL/Redis. Eventos memory.max registram tentativas de ultrapassar o limite do grupo mesmo quando não houve encerramento por OOM. Os valores são amostrados, não uma garantia de que nenhum pico entre amostras tenha sido maior.

Consultas paginadas/keyset limitam listas de leitura, mas não demonstram memória constante do contexto JPA para volumes arbitrários. O relatório não estende os resultados além da carga efetivamente executada.

[evidencias/MEMORIA_FINAL.json](../evidencias/MEMORIA_FINAL.json)
