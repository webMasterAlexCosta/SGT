# SGT-GTRAT — Entrega da Etapa 07

Data: **09/09/2026**  
Plataforma mantida: **Spring Boot 3.5.16 + Java 17**

## Resultado

A Etapa 07 implementa a linha temporal completa de Alocação `Lotação + Turno + Cargo` dentro do Vínculo Contratual e o fluxo excepcional de transferência de Gestão Local.

Principais garantias:

- mudança interna não cria novo vínculo nem desligamento;
- períodos são inclusivos, contínuos e sem sobreposição;
- existe no máximo uma Alocação efetiva por dia;
- lançamento na mesma data retifica formalmente a faixa anterior;
- fatos retroativos preservam auditoria e eventos posteriores compatíveis;
- consulta bitemporal separa data efetiva de data de conhecimento;
- a trilha de eventos possui fotografias e cadeia SHA-256;
- inativação administrativa não encerra Alocações;
- reativação revalida a composição temporal que voltará a operar;
- transferência de Gestão exige mapeamento explícito do contexto posterior.

## Validação executada

- **327 testes aprovados**;
- **0 falhas, 0 erros e 0 ignorados**;
- **T01–T70 aprovados**;
- JAR executável gerado com `Build-Jdk-Spec: 17` e `Spring-Boot-Version: 3.5.16`.

O teste MySQL/Testcontainers não pôde ser executado neste ambiente porque o Docker não está instalado. Antes da homologação, execute em banco descartável e vazio:

```bash
./mvnw -B -ntp -Pintegration verify
```

## Documentos desta entrega

- `docs/SGT_GTRAT_ETAPA_07_FEITO_E_PROXIMA_ETAPA.md`;
- `docs/SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_07.md`;
- `docs/SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_07.md`;
- `docs/SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_07.md`;
- `docs/SGT_GTRAT_API_ALOCACAO_TEMPORAL_ETAPA_07.md`.

## Próxima etapa

Prosseguir para a **Etapa 08 — Documentos, anexos e SEI**. A frequência continuará no fluxo legado até sua migração temporal na Etapa 09. O ZIP Java C permanece programado para o fechamento conjunto das Etapas 07, 08 e 09.

