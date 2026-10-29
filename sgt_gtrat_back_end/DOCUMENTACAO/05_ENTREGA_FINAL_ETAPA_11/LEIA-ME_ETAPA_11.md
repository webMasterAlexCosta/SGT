# SGT/GTRAT — Leitura inicial da Etapa 11

**Data-base:** 10/09/2026

Esta pasta documenta o fechamento funcional da Etapa 11 do backend Java. A entrega parte integralmente do checkpoint da Etapa 10 e não introduz código de migração, reconciliação ou compatibilidade com bancos anteriores.

## Ordem sugerida

1. `SGT_GTRAT_RESULTADO_ETAPA_11.md` — escopo e decisões implementadas;
2. `SGT_GTRAT_API_RELATORIOS_PENDENCIAS_ETAPA_11.md` — contratos HTTP;
3. `SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_11.md` — requisitos e matriz global;
4. `SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_11.md` — evidências reproduzíveis;
5. `SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_11.md` — fechamento Java na Etapa 12;
6. `SGT_GTRAT_ENTREGA_FINAL_ETAPA_11.md` — síntese executiva.

## Resultado objetivo

- `REL-001` a `REL-006`: implementados;
- `FRE-011` e `FRE-012`: implementados;
- `VIN-008` e parcela restante de `VIN-014`: implementados;
- `AUD-006`: implementado;
- matriz global conservadora: **144 implementados, 44 parciais e 10 pendentes**, totalizando 198;
- suíte automatizada: **383/383 testes aprovados**.

O MySQL fornecido foi inicializado em diretório novo, porém este executor bloqueou a criação de sockets Unix e TCP. Por isso a integração nativa específica da Etapa 11 não é declarada como executada; o teste `Etapa11PersistenceIT` foi incluído para essa homologação na Etapa 12.
