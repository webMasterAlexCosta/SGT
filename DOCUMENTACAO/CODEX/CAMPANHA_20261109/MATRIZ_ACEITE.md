# Matriz de aceite — campanha atual

A [matriz individual JSON](MATRIZ_ACEITE.json) contém 635 critérios de 198 requisitos como índice de rastreabilidade, mais 76 posições de gates, 64 roteiros ROP, 21 módulos e 24 jornadas E2E. Esses números identificam as fontes históricas; não são contagens de aprovação atual. Nenhum PASS antigo foi promovido.

Cada critério relaciona fonte/decisão, arquivos Java localizados, referência de API, frontend e testes candidatos existentes. Localizar arquivo não comprova implementação. Os campos de verificação, revisão e aceite continuam pendentes até evidência atual específica. A implementação Java pode estar ausente, parcial ou completa: esta distinção exige avaliação do critério, não inferência pelo nome de uma classe.

| Domínio canônico | Requisitos indexados | Critérios indexados | Aceite integral atual |
|---|---:|---:|---|
| CANONICAL198::GOV | 2 | 7 | Pendente |
| CANONICAL198::TMP | 17 | 46 | Pendente |
| CANONICAL198::CON | 25 | 105 | Pendente |
| CANONICAL198::EMP | 3 | 12 | Pendente |
| CANONICAL198::CAT | 9 | 26 | Pendente |
| CANONICAL198::CMP | 11 | 41 | Pendente |
| CANONICAL198::RED | 8 | 29 | Pendente |
| CANONICAL198::ADI | 10 | 36 | Pendente |
| CANONICAL198::REM | 7 | 26 | Pendente |
| CANONICAL198::DOC | 10 | 33 | Pendente |
| CANONICAL198::COL | 12 | 28 | Pendente |
| CANONICAL198::MAT | 5 | 12 | Pendente |
| CANONICAL198::VIN | 15 | 32 | Pendente |
| CANONICAL198::ALO | 8 | 17 | Pendente |
| CANONICAL198::DES | 6 | 12 | Pendente |
| CANONICAL198::FRE | 19 | 57 | Pendente |
| CANONICAL198::REL | 6 | 21 | Pendente |
| CANONICAL198::AUD | 7 | 32 | Pendente |
| CANONICAL198::PER | 12 | 46 | Pendente |
| CANONICAL198::HIS | 6 | 17 | Pendente |

A regra temporal vigente está em [REGRA_JANELA_FREQUENCIA.md](../../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md). GOV-003/004 preservam fatos negociais e rascunhos; não são a Governança de Terceiros removida. O cadastro operacional de Fornecedor e os compartilhados continuam no escopo.

Namespaces: `CANONICAL198::<requisito>:<critério>`, `MASTER::<seção>::<gate>`, `MASTER::6::<Mxx>`, `MASTER::ANEXO_A::<ROP-xxx>` e `MASTER::8.7::<E2E-xx>`. O mestre tem oito posições PERF com aceite comum; não foram inventados oito predicados independentes.

Os 76 gates possuem obrigação atual e camadas individuais em `scope_assessment_current`: backend, frontend/integrada, proveniência histórica e condições específicas. [Alcance por etapa](MASTER_ALCANCE_BACKEND.md). A classificação não é aprovação e preserva toda parte backend exigível. A preparação negativa de retomada ALO-008:002 tem [análise delimitada dos escritores canônicos](ALO008_ALCANCABILIDADE.md), sem PASS HTTP fabricado ou alegação universal de impossibilidade.

Documentos administrativos antigos: avaliar por fato/identidade/intervalo efetivamente aplicável. Os 33 critérios documentais históricos não são bloqueio universal da base PostgreSQL nova; mecanismos técnicos, recusa segura e cortes bitemporais precisam de testes, enquanto autenticidade requer origem humana quando pertinente. Não há migração de acervo nesta missão.

Os recibos da campanha ficam separados por lote em `.homologacao/20261109-sgt-final`, sem publicar logs/ambiente/credenciais. Uma classe/metodologia citada historicamente continua apenas candidata até ser relacionada ao resultado novo e aos hashes compatíveis. O [plano](PLANO.md) registra gates, sequência e limitações.

A varredura estática inicial localizou candidatos Java, React e testes atuais para os635 critérios, sem atestar cobertura. O frontend já contém TS/TSX; o transporte tem uma instância Axios dedicada e a leitura de import.meta.env está centralizada. As duas chamadas fetch localizadas pertencem a configuração pública e integração externa de localização, exigindo verificação própria; não representam permissão para transporte de API nas telas. Nenhuma implementação principal React foi iniciada antes do gate Java.
