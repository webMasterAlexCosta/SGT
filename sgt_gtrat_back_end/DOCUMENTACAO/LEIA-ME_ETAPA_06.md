# SGT-GTRAT — Entrega Java B após a Etapa 06

Data do checkpoint: **09/09/2026**.

Esta entrega mantém, de forma deliberada:

- **Java 17**;
- **Spring Boot 3.5.16**;
- banco novo e descartável por versão, sem tentativa automática de conciliar bases legadas;
- backend Java ainda em evolução até a Etapa 12;
- React fora deste checkpoint, conforme o plano aprovado.

## Resultado

A Etapa 06 implementa o modelo canônico de **Pessoa + Vínculo Contratual + Matrícula + Desligamento**, integrado ao contrato temporal das etapas anteriores. O cadastro legado `Empregado` foi mantido apenas como compatibilidade transitória para módulos que serão migrados nas Etapas 07 e 09.

Validação executada:

- 315 testes JUnit, sem falhas;
- 23 testes específicos da Etapa 06;
- verificações T01–T50 aprovadas;
- JAR executável gerado com Java 17 e Spring Boot 3.5.16.

O perfil de integração MySQL/Testcontainers não foi executado neste ambiente porque Docker não estava disponível. Essa validação é obrigatória antes de homologação.

## Documentos desta entrega

- `docs/SGT_GTRAT_ETAPA_06_FEITO_E_PROXIMA_ETAPA.md`
- `docs/SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_06.md`
- `docs/SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_06.md`
- `docs/SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_06.md`

## Reprodução rápida

```bash
./mvnw -B -ntp clean test
python3 scripts/validar-checkpoint-etapa06.py
```

Com Docker disponível:

```bash
./mvnw -B -ntp -Pintegration verify
```

Antes de iniciar a aplicação, copie `.env.example` para `.env`, preencha apenas valores locais e crie um schema MySQL vazio exclusivo desta versão. O arquivo `.env` real não integra o ZIP.
