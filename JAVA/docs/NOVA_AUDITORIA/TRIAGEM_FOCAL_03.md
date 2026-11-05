# Triagem da rodada focal 03

Fonte congelada: `58c5c89b692902ec214e99e58ef7720edc21c63e6f0b1f65d056573bcb29b54e`. Rodada: 12 unitários e 126 integrações; 131 aprovados, cinco falhas de assertiva e dois erros, nenhuma ignorada. Fontes inalteradas durante execução.

| Cenário | Causa observada | Encaminhamento |
|---|---|---|
| MotivosProvaContextoHistoricoHttpIT, transferência | Fixture enviou autorização diferente da concessão ATO-HTTP | Corrigir somente valor da autorização sintética; manter destino, intervalo e assertivas |
| ProvaHistoricaConfiguracaoAlocacaoHttpIT | Inferência genérica escolheu sobrecarga reflexiva estática, convertendo Configuracao para Class | Usar acessores tipados; manter todos os valores esperados |
| TransversaisRelatorioFonteFuturaRedIT | Consulta de absenteísmo não enviava mes/ano/contratoId exigidos pela rota | Corrigir URI mensal; preservar proibição de preencher lacuna nominal |
| SegurancaRevogacaoConcorrenteRedIT | Lock de contrato previamente carregado usa versão anterior após espera e impede revogação | Refrescar contrato sob lock de escrita antes de refrescar concessão, na mesma ordem |
| ProvaHistoricaContratoHttpIT (dois cortes K) | API substituía ausência de fotografia por DTO projetado sem origem | Respeitar base.versaoContratual nula; fotografia parcial existente continua disponível |
| NovaAuditoriaFotografiasTardiasPessoasHttpIT | Banco descartável criado com anotação antiga conservou NOT NULL, apesar da nova anotação permitir desconhecimento | Preservar evidências, recriar somente banco sintético isolado e repetir com schema exclusivamente gerado pelo Hibernate update; não executar ALTER manual |

As mudanças acima ainda dependem da próxima execução GREEN. A falha concorrente demonstra revogação não concluída; não comprova uso posterior a uma revogação já commitada. Nenhuma falha de roteiro foi contada como defeito produtivo. A recriação de homologação sintética não constitui validação de migração de uma base produtiva.
