# SGT-GTRAT — Próximos passos após a Etapa 08

Data-base: **09/09/2026**

## 1. Validação obrigatória antes da homologação

Executar o perfil MySQL/Testcontainers em máquina com Docker:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco descartável, vazio e exclusivo desta versão. Não importar automaticamente banco de etapas anteriores.

## 2. Etapa 09 — Frequência integrada ao modelo temporal

Objetivo: tornar Vínculo e Alocação históricos a fonte canônica de cada frequência diária.

### 2.1 Resolução temporal

Para a data negocial da frequência, resolver:

1. Contrato válido e operacional;
2. Vínculo Contratual válido;
3. Alocação `Lotação + Turno + Cargo` válida;
4. Gestão Local e escopo históricos;
5. Jornada correspondente à data de início, inclusive quando cruza meia-noite.

Não usar a alocação atual do colaborador para reescrever um registro histórico.

### 2.2 Janela operacional

Implementar os sete dias corridos em `America/Sao_Paulo`:

- a data da frequência é o dia 1;
- frequência de 01/01 permanece editável pelo Operacional até 07/01 23:59:59;
- no início de 08/01 passa automaticamente a `FECHADA`;
- o fechamento é estado negocial derivado/registrado, não mera aparência de tela.

### 2.3 Retificação pelo Gestor Local

- toda alteração do Gestor Local gera versão formal;
- exige identificação e justificativa, mesmo dentro dos sete dias;
- não possui prazo máximo rígido;
- após intervenção do Gestor, o registro não volta à edição do Operacional;
- preservar conteúdo anterior, autor original, data/hora e cadeia de versões.

### 2.4 Retroatividade e validade oficial

Quando desligamento, Vínculo, Alocação ou Contrato forem corrigidos retroativamente:

- identificar frequências abertas e fechadas afetadas;
- preservar o lançamento original;
- separar visibilidade histórica, validade operacional e estado de auditoria;
- retirar da visão oficial o que perdeu cobertura válida, sem excluir;
- impedir que outro Operacional assuma silenciosamente o registro;
- encaminhar correção fechada ao Gestor Local;
- sinalizar relatórios como sujeitos a revisão até a conclusão.

### 2.5 Autorização

A edição deve combinar:

- autorização atual do usuário;
- escopo histórico da frequência;
- janela temporal;
- estado do registro;
- existência de retificação gerencial.

### 2.6 Testes mínimos

Cobrir `FRE-001` a `FRE-019`, incluindo:

- bordas do primeiro, sétimo e oitavo dias no fuso de São Paulo;
- mudança de horário de verão tratada pelo `ZoneId`, sem offset fixo;
- jornada noturna atribuída à data de início;
- mudança de Alocação dentro do mês;
- desligamento retroativo com frequência posterior;
- Alocação retificada para outra Gestão;
- Gestor corrigindo no terceiro e após o oitavo dia;
- Operacional original, outro Operacional e usuário sem escopo;
- fechamento concorrente com edição;
- idempotência de fechamento e retificação;
- visão oficial atual e versões auditáveis.

### 2.7 Critério de saída

- `FRE-001` a `FRE-019` classificados com evidência;
- `ALO-006` concluído no consumidor de frequência;
- `VIN-014` reavaliado quanto aos impactos de frequência;
- nenhuma frequência histórica é apagada ou reassociada silenciosamente;
- suíte completa e checkpoints T01–T110 verdes;
- perfil MySQL/Testcontainers executado antes de homologação;
- ZIP Java C gerado com Etapas 07, 08 e 09 consolidadas.

## 3. Sequência posterior

| Etapa | Conteúdo principal |
|---:|---|
| 10 | Perfis, permissões granulares e minimização de dados |
| 11 | Relatórios temporais, indicadores, auditoria consolidada e Central de Pendências |
| 12 | Testes finais, carga, concorrência, idempotência e fechamento do Java |
| React | Somente após congelamento das APIs do backend final |

## 4. Regras fixas

- Spring Boot 3.5.16 e Java 17;
- banco novo por versão durante a evolução;
- fatos e documentos publicados não são apagados;
- inativação administrativa não equivale a encerramento;
- mudança de Alocação não equivale a desligamento/recontratação;
- frequência pertence à data de início da jornada;
- autorização atual não muda silenciosamente a autoria nem o escopo histórico;
- React não começa antes do backend Java consolidado.
