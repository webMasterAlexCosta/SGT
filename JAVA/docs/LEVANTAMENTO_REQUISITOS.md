# SGT/GTRAT — Levantamento Completo de Requisitos Funcionais

**Versão:** 8.0 — levantamento final consolidado após revisão integral de 06/09/2026
**Data:** 06/09/2026
**Finalidade:** levantamento funcional autossuficiente para validação com o cliente, manutenção evolutiva, testes e futura implementação.
**Escopo:** Contrato, composição contratual, aditamento, remanejamento, pessoa/colaborador, vínculo, alocação, desligamento, frequência, documentos, permissões, relatórios, histórico e auditoria.
**Código nesta etapa:** **não**.

**Emenda vigente de09/11/2026:** decisão humana altera a janela de frequência de sete dias fixos para N configurável, padrão30, preservando os IDs FRE e as demais permissões. Detalhamento e reconciliação histórica: [REGRA_JANELA_FREQUENCIA.md](REGRA_JANELA_FREQUENCIA.md). A versão8.0 acima identifica a origem; esta emenda não é um recibo de implementação/homologação.

> Este documento deve ser compreendido sem acesso a conversas, versões anteriores, imagens ou outros arquivos. Todas as regras já fechadas estão descritas aqui por conteúdo.

---


## Convenções de manutenção desta especificação

- requisitos utilizam identificadores canônicos permanentes por domínio;
- o identificador não muda quando a redação é refinada;
- quando uma regra evoluir, a redação vigente substitui a anterior para implementação;
- a manutenção documental deve preservar o histórico da evolução quando necessário, sem confundir regra atual com regra superada;
- somente decisões expressamente aceitas ou consequências diretamente derivadas de decisões fechadas entram como requisito vigente;
- recomendações não aprovadas não devem ser tratadas como regra funcional.


---

# 1. Objetivo do módulo

O módulo deve controlar o ciclo de vida contratual e a relação temporal entre contratos e colaboradores, preservando a verdade histórica.

O sistema deve permitir responder, para qualquer data válida:

- qual contrato existia e qual era sua configuração;
- qual era sua vigência;
- qual empresa estava vinculada ao contrato;
- qual era o efetivo total previsto;
- quais combinações de Lotação + Turno + Cargo estavam previstas;
- qual era o quantitativo previsto em cada combinação;
- quais aditamentos e remanejamentos já produziam efeito;
- em qual contrato determinada pessoa estava;
- em qual Lotação, Turno e Cargo ela estava;
- qual matrícula pertencia ao vínculo;
- se o vínculo estava vigente, programado ou encerrado;
- quando cada fato realmente produziu efeito;
- quando o sistema tomou conhecimento do fato;
- quem registrou, corrigiu, retificou ou encerrou cada informação.

A projeção atual nunca substitui a história.

---

# 2. Princípios obrigatórios

1. **Contrato é a base da estrutura operacional.**
2. **Pessoa, Vínculo Contratual e Alocação são conceitos diferentes.**
3. **Data efetiva e data/hora de registro são informações distintas.**
4. **Fato negocial publicado não é apagado fisicamente.**
5. **Retificação preserva o fato anterior e cria nova versão.**
6. **Retroatividade é permitida quando prevista pela regra, sempre com análise de impacto e auditoria.**
7. **Nenhuma operação crítica move, desliga, corrige ou realoca pessoas silenciosamente.**
8. **O passado deve ser reconstruído pela linha temporal válida, e não pelos campos atuais.**
9. **Reinicialização do sistema não cria nem altera fatos negociais.**
10. **A granularidade negocial de contrato, vínculo e alocação é diária; horário de registro serve para auditoria.**

---

# 3. Glossário funcional

| Termo | Definição |
|---|---|
| **Pessoa / Colaborador** | Identidade permanente da pessoa, independente de contrato específico. |
| **Contrato** | Instrumento que estabelece empresa, vigência, tipo de contratação, efetivo e combinações operacionais permitidas. |
| **Vínculo Contratual** | Relação temporal entre uma Pessoa e um Contrato. |
| **Alocação** | Lotação + Turno + Cargo válida durante um período do Vínculo. |
| **Composição Contratual** | Combinação explícita de Lotação + Turno + Cargo com quantitativo previsto. |
| **Efetivo previsto** | Quantitativo contratualmente previsto para o contrato e suas composições. |
| **Excedente** | Quantidade de pessoas vinculadas acima do quantitativo previsto para a composição/recorte. |
| **Aditamento** | Fato contratual que altera condições do contrato a partir de uma data efetiva. |
| **Remanejamento** | Redistribuição do quantitativo entre combinações já válidas, sem alterar o total do contrato. |
| **Retificação** | Nova versão que corrige um fato anterior sem apagá-lo. |
| **Data efetiva** | Data em que o fato realmente passa a produzir efeito negocial. |
| **Data de registro** | Data/hora em que o sistema registrou ou tomou conhecimento do fato. |
| **Programado** | Fato válido cadastrado para produzir efeito no futuro. |
| **Anulado** | Estado de contrato publicado posteriormente declarado inválido; não implica exclusão física. |
| **Inativação administrativa** | Bloqueio operacional distinto da situação temporal de vigência do contrato. |

---

# 4. Cadastros auxiliares

## 4.1. Tipo de Contratação

Tipo de Contratação é cadastro próprio, não texto livre.

Deve possuir, no mínimo:

- descrição;
- situação ativo/inativo;
- data/hora de cadastro;
- usuário responsável.

Regras:

- é administrado pelo Gestor Local;
- é exclusivo do escopo da Gestão Local;
- deve ser escolhido por seleção de cadastro;
- quando já utilizado historicamente, sua inativação não pode apagar nem alterar o histórico.

## 4.2. Lotação

Lotação é cadastro operacional utilizado nas composições e alocações.

Uma Lotação inativa permanece disponível para consulta histórica.

## 4.3. Turno

Turno é cadastro operacional utilizado nas composições e alocações.

Um Turno inativo permanece disponível para consulta histórica.

## 4.4. Cargo

Cargo é cadastro próprio administrado pela Gestão Local.

Não deve ser digitado como texto livre no contrato ou no vínculo.

Cargo já utilizado historicamente permanece consultável mesmo quando inativo.

## 4.5. Motivo de Desligamento

Motivo de Desligamento é catálogo administrado pelo Gestor Local.

O desligamento deve selecionar um motivo previamente cadastrado.

O histórico deve preservar o motivo aplicado ao vínculo.

As causas de término do vínculo devem distinguir, no mínimo:

- desligamento individual;
- fim natural da vigência do contrato;
- encerramento antecipado/rescisão;
- anulação do contrato;
- outras causas formalmente cadastradas.

Não utilizar um único motivo genérico para fatos negociais diferentes.

## 4.6. Uso temporal de cadastros inativos

Cargo, Lotação, Turno e demais catálogos podem ser usados em registro retroativo quando estavam válidos na data efetiva do fato e a operação é historicamente coerente.

Regras:

- a inativação atual não apaga o uso histórico;
- um item hoje inativo pode ser referenciado em fato retroativo se estava válido na data efetiva;
- um item atualmente inativo não pode ser usado em evento futuro;
- para uso futuro, deve ser reativado explicitamente antes da programação;
- a reativação preserva a mesma identidade histórica e deve ser auditada.

---

# 5. Requisitos detalhados por domínio

ditada.

## 5.1. Contrato — dados obrigatórios

Um contrato publicado deve possuir:

- número;
- ano;
- descrição;
- Tipo de Contratação;
- empresa terceirizada;
- CNPJ;
- razão social;
- nome fantasia;
- data inicial de vigência;
- data final de vigência;
- efetivo total previsto;
- uma ou mais composições válidas;
- situação temporal;
- habilitação administrativa;
- fonte comprobatória por anexo e/ou referência SEI.

Número + ano identificam unicamente o contrato no sistema.

Um contrato pertence a uma única Gestão Local por vez.

## 5.1.1. Correção excepcional de número/ano após publicação

Número + ano são imutáveis no fluxo comum depois da publicação.

Correção excepcional é permitida apenas para erro material comprovado.

Regras:

- a permissão não é automática para todo Gestor Local;
- exige permissão excepcional específica;
- a administração técnica pode conceder essa permissão;
- o ato negocial de correção continua sendo executado pelo Gestor Local autorizado;
- o identificador anterior permanece preservado e pesquisável;
- registrar motivo, valor anterior, valor novo, usuário, data/hora e autorização;
- Gestor Estadual, Gestor Nacional e Presidente permanecem sem escrita negocial.

## 5.1.2. Transferência excepcional de Gestão Local

Contrato publicado não muda de Gestão Local no fluxo comum.

Em caso de reorganização administrativa real, a transferência pode ocorrer de forma excepcional.

A transferência deve:

- possuir data efetiva;
- possuir motivo;
- registrar Gestão Local anterior e Gestão Local nova;
- registrar responsável pela operação;
- ser auditada;
- preservar qual Gestão Local era responsável em cada período;
- preservar integralmente os cadastros, composições e relações históricas usados antes da data efetiva;
- não copiar nem reescrever silenciosamente Tipo de Contratação, Cargo, Lotação, Turno ou composição do período anterior;
- validar o contexto operacional que ficará vigente na nova Gestão Local;
- bloquear a conclusão enquanto não existir composição válida para o período posterior;
- permitir mapeamento explícito para cadastros válidos da nova Gestão quando necessário;
- preservar o contexto anterior somente para consulta histórica.

A transferência exige autorização administrativa específica e é executada como operação excepcional pelo Gestor Local autorizado. O Gestor Estadual não recebe escrita negocial por causa dessa transferência.

A responsabilidade da Gestão Local possui história temporal.

## 5.2. Contrato FUTURO

O contrato pode ser publicado antes da data inicial da vigência.

Enquanto a data inicial não for alcançada, sua situação temporal é `FUTURO`.

Contrato FUTURO pode possuir fatos programados, desde que:

- documentalmente válidos;
- temporalmente coerentes;
- nenhum efeito operacional ocorra antes da data permitida.

### Derivação da situação temporal

A situação temporal comum é **derivada da linha temporal válida e da data corrente**, e não criada por rotina técnica:

- antes da data inicial: `FUTURO`;
- entre a data inicial e a data final, inclusive: `VIGENTE`;
- depois da data final: `ENCERRADO`.

Rotinas agendadas podem atualizar projeções, criar alertas e gerar pendências, mas não criam nem modificam fatos negociais.

`CANCELADO` e `ANULADO` dependem de eventos explícitos e não são derivados apenas da passagem do tempo.

## 5.3. RASCUNHO

Enquanto `RASCUNHO`:

- o contrato pode estar incompleto;
- não pode ser usado para criar vínculo operacional;
- pode ter número/ano corrigidos respeitando unicidade;
- pode ter composição ajustada livremente antes da publicação.

Rascunho nunca publicado e sem fato negocial pode ser descartado transacionalmente.

## 5.4. Publicação

Para publicar:

- todos os campos obrigatórios devem estar preenchidos;
- a empresa deve estar completa;
- o Tipo de Contratação deve estar selecionado;
- deve existir pelo menos uma fonte comprobatória;
- a soma das composições deve corresponder ao efetivo total vigente;
- não pode existir combinação duplicada.

A publicação deve preservar uma fotografia integral da versão original, incluindo:

- identificação;
- empresa;
- vigência;
- tipo de contratação;
- efetivo total;
- composições;
- demais valores negociais efetivamente publicados.

Depois de publicado, o contrato não volta a ser um formulário livre.

Contrato temporalmente encerrado permanece disponível para consulta histórica.

## 5.5. Empresa contratada

CNPJ, razão social e nome fantasia são obrigatórios no contrato publicado.

### Mudança real de empresa/CNPJ

Quando o contrato passa a ter outra empresa/CNPJ por fato negocial válido:

- não editar silenciosamente a empresa anterior;
- registrar mudança contratual de empresa;
- informar data efetiva;
- preservar a empresa anterior no histórico;
- aplicar a nova empresa somente a partir da data efetiva.

### Correção material de CNPJ

Se o CNPJ foi apenas digitado incorretamente no cadastro original:

- tratar como correção material excepcional;
- não interpretar como troca real de empresa;
- preservar valor anterior e valor corrigido;
- registrar justificativa, usuário e data/hora;
- preservar auditoria.

Mudança de razão social/nome fantasia:

- vale a partir da data efetiva do fato;
- não reescreve o passado;
- não altera silenciosamente outros contratos da mesma empresa.

## 5.6. Composição Contratual

A composição é a combinação explícita:

`Lotação + Turno + Cargo + Efetivo previsto`

Regras:

- não gerar produto cartesiano automaticamente;
- cada combinação deve existir expressamente;
- não permitir combinação duplicada no mesmo período;
- a soma dos quantitativos das composições deve corresponder ao efetivo total vigente do contrato;
- composição com quantitativo zero fica indisponível para nova alocação;
- composição que volta de zero para valor positivo retorna por nova versão temporal;
- reduzir ou remover composição ocupada exige análise de impacto;
- pessoas não são movidas automaticamente;
- programações futuras incompatíveis devem ser tratadas antes da conclusão da alteração.

## 5.7. Excedente

O sistema pode permitir vínculo acima do efetivo previsto.

Nesse caso:

- exibir alerta forte;
- exigir justificativa do Gestor Local;
- registrar auditoria;
- não impor teto técnico absoluto de excedente;
- manter o excedente monitorável enquanto existir.

A consulta do excedente deve permitir identificar, no mínimo:

- composição;
- efetivo previsto;
- quantidade vinculada;
- quantidade excedente;
- desde quando existe;
- justificativas relacionadas.

## 5.8. Redução contratual — regras já definidas

A redução contratual possui limite máximo global de 25% calculado sobre o **efetivo total originalmente publicado do contrato**.

Regras:

- a base de cálculo é fixa e corresponde ao efetivo total da publicação original;
- aumentos posteriores não alteram essa base;
- o efetivo total vigente nunca pode ficar abaixo de 75% do efetivo total originalmente publicado;
- o limite é cumulativo ao longo dos eventos de redução válidos;
- o cálculo considera somente os eventos válidos na linha temporal;
- redução cancelada ou retificada deixa de consumir o limite quando seu efeito deixa de ser vigente;
- depois de um aumento, é permitido reduzir todo o aumento e ainda reduzir até o piso de 75% da base original;
- a validação é sobre o total do contrato, não sobre cada composição isoladamente;
- uma composição pode ser reduzida em percentual superior a 25%, inclusive a zero, desde que o total global respeite o piso e os impactos sejam tratados;
- aumento contratual não possui limite percentual máximo;
- redução acima do limite é bloqueada no fluxo comum;
- redução excepcional acima de 25% somente é permitida quando existir fundamento jurídico/negocial documentado e autorização excepcional;
- quando o piso percentual resultar fracionário, o valor mínimo inteiro é arredondado para cima.

### Exceção documentada acima de 25%

Quando existir fundamento jurídico/negocial válido para ultrapassar o limite normal:

- exigir fonte comprobatória obrigatória por SEI e/ou anexo;
- registrar explicitamente o fundamento da exceção;
- exigir permissão técnica excepcional para o Gestor Local;
- a administração técnica pode conceder a permissão, mas não pratica o ato negocial;
- a execução continua sendo do Gestor Local autorizado;
- exigir justificativa;
- exigir prévia de impacto;
- aplicar auditoria reforçada.

A permissão técnica nunca substitui o documento/fundamento que autoriza a exceção.

Exemplo: contrato originalmente publicado com efetivo 100 possui piso 75. Se depois aumentar para 140, a base continua 100 e o contrato poderá ser reduzido até 75, respeitados os demais impactos e autorizações.

## 5.9. Aditamento

O contrato pode possuir vários aditamentos.

Um aditamento pode alterar, conforme o fato negocial real:

- efetivo total;
- distribuição do efetivo;
- vigência;
- Lotação;
- Turno;
- Cargo;
- composição contratual;
- descrição/escopo;
- Tipo de Contratação;
- empresa terceirizada;
- CNPJ/identificação da empresa quando houver fato contratual válido que altere a empresa;
- razão social/nome fantasia quando o fato contratual os alterar;
- outros campos negociais existentes no próprio cadastro do contrato, desde que o documento de aditamento determine expressamente sua alteração.

Aditamento **não altera diretamente estados do ciclo de vida**. Permanecem como eventos/operações próprios:

- publicação;
- inativação;
- reativação;
- encerramento;
- cancelamento;
- anulação.

Aditamento pode alterar a vigência e demais condições documentadas, mas não substitui essas operações específicas.

Cada aditamento deve possuir:

- identificação/número;
- data do documento;
- data da assinatura, quando existente;
- data efetiva;
- justificativa/observação;
- usuário responsável pelo registro;
- data/hora de registro;
- fonte comprobatória por anexo e/ou SEI.

Aditamento quantitativo deve registrar:

- combinação afetada;
- quantidade anterior;
- variação;
- quantidade resultante.

Aditamento:

- pode aumentar ou reduzir;
- pode ser retroativo;
- pode ser programado para o futuro;
- pode ser retificado;
- pode ser cancelado antes de produzir efeito;
- nunca apaga fisicamente a versão anterior.

## 5.10. Remanejamento

Remanejamento é fato contratual próprio.

Deve possuir:

- identificação;
- data efetiva;
- distribuição anterior;
- distribuição resultante;
- motivo/justificativa;
- usuário responsável;
- data/hora de registro;
- referência SEI quando existente;
- anexo quando existente.

Regras:

- o total do contrato é preservado;
- a soma das variações efetivamente aplicadas deve ser zero;
- somente combinações válidas já existentes podem participar;
- criar nova combinação exige Aditamento;
- remanejamento pode ser retificado;
- remanejamento programado para data futura pode ser cancelado antes de produzir efeito;
- o cancelamento preserva o remanejamento original e registra motivo, usuário, data/hora e fonte comprobatória quando aplicável;
- remanejamento cancelado não produz efeito;
- toda versão anterior permanece auditável.

## 5.11. Ordem de eventos

Quando dois eventos conflitarem sobre o mesmo dado na mesma data:

- deve existir precedência negocial explícita;
- a ordem não pode depender de ID de banco;
- a ordem não pode depender de milissegundo de gravação;
- a ordem não pode depender da sequência técnica de leitura.

Eventos da mesma data que não colidem podem coexistir.

Quando um documento antigo for registrado depois de um documento posterior já conhecido:

- inserir o fato antigo na posição temporal correta;
- reaplicar os eventos posteriores válidos;
- não substituir silenciosamente o documento posterior;
- transformar incompatibilidades em pendências de tratamento;
- não apagar fatos posteriores automaticamente.

A verdade histórica é reconstruída pela sequência negocial dos fatos, não pela ordem em que foram digitados no sistema.

### Relações explícitas de precedência

A precedência entre eventos deve ser registrada por relação negocial explícita, utilizando, conforme o caso:

- `SUCEDE`;
- `RETIFICA`;
- `CANCELA`;
- `SUBSTITUI`;
- `MANTÉM/COEXISTE`.

Quando o instrumento possuir sequência formal, registrar também número/ordem documental.

Se eventos conflitantes tiverem a mesma data efetiva e não houver precedência definida, bloquear a conclusão até que a relação negocial seja informada.

Timestamp, ID técnico e ordem de digitação nunca resolvem precedência.

## 5.12. Pessoa / Colaborador

Campos obrigatórios:

- nome;
- CPF.

Campos opcionais:

- RG;
- CTPS;
- NIT/PIS;
- data de nascimento;
- e-mail;
- endereço.

Dados bancários não fazem parte deste fluxo.

CPF:

- é único globalmente;
- deve ser validado;
- é armazenado sem pontuação;
- pode ser exibido com máscara na interface;
- não é editável no fluxo comum.

Correção excepcional de CPF por erro material comprovado:

- é executada pelo Gestor Local;
- exige validação do novo CPF;
- registra CPF anterior;
- registra CPF novo;
- registra motivo;
- registra evidência;
- registra responsável;
- registra autorização;
- registra data/hora;
- registra impactos encontrados.

Se o CPF novo já pertencer a outra pessoa:

- bloquear correção automática;
- não fundir pessoas silenciosamente.

Nome:

- não é editável no fluxo comum;
- erro material ou mudança legal pode ser corrigido excepcionalmente;
- a correção é auditada;
- não cria nova pessoa.

O cadastro atual exibe o valor atual da pessoa; o histórico preserva a fotografia correspondente ao período.

### Destino do CPF incorreto após correção excepcional

Quando um CPF incorreto é corrigido:

- o CPF antigo permanece somente na auditoria da correção;
- deixa de ser identificador ativo da pessoa;
- não fica reservado permanentemente como se pertencesse àquela pessoa;
- poderá ser utilizado futuramente por seu titular real, após validações normais.

Se o novo CPF já estiver associado a outra pessoa ativa:

- bloquear a correção automática;
- não realizar fusão automática;
- exigir tratamento excepcional de identidade/duplicidade.

## 5.13. Matrícula

Matrícula pertence ao Vínculo, não à Pessoa.

Regras:

- é opcional por padrão;
- Tipo de Contratação e/ou configuração do Contrato podem torná-la obrigatória;
- quando configurada como obrigatória, o Vínculo não pode ser publicado/ativado sem matrícula válida;
- é única dentro do contrato;
- permanece reservada historicamente depois de desligamento;
- permanece reservada depois de alteração;
- programação cancelada não libera matrícula já reservada historicamente;
- em recontratação, inclusive da mesma pessoa, um novo vínculo não reutiliza a matrícula histórica anterior.

## 5.14. Vínculo Contratual

O Vínculo representa a relação temporal Pessoa + Contrato.

A antiga “data de admissão” é substituída pela data efetiva de início do vínculo.

Regras:

- não pode existir sobreposição de vínculos da mesma pessoa;
- a verificação é global, inclusive entre contratos e Gestões diferentes;
- troca direta de contrato pode ocorrer em dias consecutivos;
- vínculo futuro pode ser cadastrado como `PROGRAMADO`;
- vínculo programado não torna a pessoa ativa antes da data;
- programação futura pode ser cancelada sem simular desligamento de vínculo que nunca ocorreu;
- cancelamento/retificação preserva toda a cadeia de versões;
- retificação do início do vínculo é permitida com justificativa, auditoria e prévia de impacto.

A retificação do início deve revalidar:

- sobreposição;
- alocação;
- matrícula;
- frequência;
- relatórios;
- eventos posteriores.

## 5.15. Seleção encadeada da alocação

Ao criar um vínculo/alocação:

1. selecionar o Contrato;
2. apresentar somente Lotações existentes em composições válidas para o recorte;
3. após a Lotação, apresentar somente Turnos válidos;
4. após o Turno, apresentar somente Cargos válidos;
5. escolher combinação realmente existente;
6. informar a data efetiva.

Combinação inexistente não pode ser utilizada.

## 5.16. Alocação

Alocação é uma faixa temporal de:

`Lotação + Turno + Cargo`

Regras:

- mudança de Lotação/Turno/Cargo cria novo período de alocação;
- não cria novo vínculo;
- não é desligamento;
- alteração retroativa é permitida;
- eventos posteriores válidos são preservados;
- incompatibilidades são apresentadas para tratamento;
- existe no máximo uma alocação efetiva por pessoa por dia;
- todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação válida.

A consulta mensal deve retornar todas as faixas ocorridas no mês, não apenas a posição final.

### Alocação durante inativação administrativa

A inativação administrativa bloqueia a operação, mas não encerra o Vínculo nem a Alocação.

Durante o intervalo de inatividade:

- a Alocação continua temporalmente registrada;
- não é criada nova Alocação apenas por causa da inativação;
- na reativação, a mesma Alocação volta a ser operacionalmente utilizável se continuar temporalmente válida, não tiver sido sucedida por outro evento de alocação e a composição continuar válida.

## 5.17. Desligamento

Desligamento pertence ao Vínculo.

A data de desligamento representa o **último dia em que o vínculo ainda existe**.

A pessoa fica inativa em relação àquele vínculo no dia seguinte.

O desligamento deve possuir:

- data efetiva;
- motivo de catálogo;
- usuário responsável;
- data/hora de registro.

Pode ser:

- retroativo;
- futuro/programado;
- retificado;
- cancelado quando programado.

A cadeia original deve ser preservada.

## 5.18. Fim natural da vigência

No fim natural da vigência:

- o contrato deixa de permitir operação a partir do dia seguinte;
- vínculos ainda abertos ficam operacionalmente inativos a partir do dia seguinte;
- o tratamento administrativo usa como data final do vínculo a data final do contrato, salvo desligamento real anterior;
- deve existir motivo próprio, como `Fim da vigência do contrato`;
- o registro administrativo pode ocorrer depois, sem alterar a data efetiva real.

Prorrogação retroativa não reabre automaticamente vínculos já encerrados; exige análise de impacto e eventual retificação explícita.

### Continuidade da vigência

Prorrogação do mesmo contrato deve manter continuidade temporal.

Não é permitido criar lacuna interna de vigência no mesmo contrato. A nova vigência deve começar no dia imediatamente seguinte ao fim anterior.

Se existir intervalo real sem cobertura contratual, tratar como novo contrato ou como exceção negocial específica formalmente autorizada.

### Alertas de proximidade do fim da vigência

O sistema deve alertar o Gestor Local antes do fim da vigência em marcos **configuráveis**, adotando 30, 15 e 7 dias como valores padrão.

O alerta não altera a regra negocial. Se nenhuma providência for tomada, o contrato deixa de permitir operação no dia seguinte ao fim da vigência e passa ao tratamento administrativo correspondente.

## 5.18.1. Operação de Encerramento do Contrato

O encerramento do contrato é uma operação negocial própria e não uma simples edição silenciosa de status.

Antes da confirmação, o sistema deve apresentar uma prévia contendo:

- data efetiva do encerramento;
- quantidade de vínculos atingidos;
- identificação/relação dos colaboradores afetados;
- programações futuras afetadas;
- impactos conhecidos;
- motivo;
- usuário responsável;
- autorização necessária;
- confirmação explícita.

O encerramento:

- preserva o histórico;
- não remaneja pessoas automaticamente para outro contrato;
- deve tratar programações futuras que se tornarem inviáveis;
- mantém programações canceladas/inviabilizadas como fatos auditáveis.


## 5.19. Inativação administrativa

A inativação administrativa é distinta da situação temporal do contrato e não altera silenciosamente sua vigência jurídica.

Pode ser:

- retroativa;
- imediata;
- futura, apenas quando houver necessidade operacional real, permanecendo como evento `PROGRAMADO` até a data efetiva.

A inativação:

- possui data efetiva;
- possui data/hora de registro;
- possui motivo;
- possui autor;
- cria intervalo de bloqueio operacional;
- bloqueia novas operações;
- bloqueia novos vínculos;
- bloqueia a operação comum de frequência durante o intervalo;
- **não encerra automaticamente os vínculos**.

Os vínculos continuam conforme sua própria linha temporal até desligamento, fim de vigência ou outro fato válido de término.

Se o contrato for reativado dentro da mesma vigência:

- vínculos ainda temporalmente válidos voltam a operar;
- não é necessário criar novo vínculo;
- o intervalo de inatividade permanece no histórico.

A reativação:

- é novo evento auditável;
- pode ser retroativa;
- exige data efetiva, justificativa, prévia de impacto e auditoria.

Inativação ou reativação retroativa que atinja frequências segue a política geral de fatos retroativos: preservar registros, identificar impacto, sinalizar períodos afetados e gerar retificação quando necessário.

A habilitação administrativa deve ser preservada como linha temporal, permitindo identificar em qualquer data se o contrato estava operacionalmente ativo, administrativamente inativo, reativado ou encerrado temporalmente.

itáveis.

## 5.20. Encerramento antecipado / rescisão

Encerramento antecipado/rescisão:

- é diferente de fim natural da vigência;
- é diferente de inativação administrativa;
- altera a data final efetiva do contrato;
- exige fonte comprobatória por anexo e/ou SEI;
- é fato auditável.

Quando ocorrer:

- vínculos ainda abertos terminam na mesma data efetiva, salvo quem já possua desligamento anterior;
- usar causa própria, como `Encerramento antecipado/rescisão do contrato`;
- não confundir com desligamento individual;
- não confundir com `Fim da vigência do contrato`;
- programações posteriores incompatíveis devem ser tratadas explicitamente;
- programações canceladas/inviabilizadas permanecem no histórico.

## 5.21. Anulação

Contrato publicado pode ser marcado como `ANULADO` quando posteriormente declarado inválido.

A anulação deve registrar separadamente:

- data do ato anulatório;
- data/hora em que foi registrada no sistema;
- data efetiva do alcance da anulação.

Se o documento declarar o contrato inválido desde a origem, o alcance começa na data inicial original do contrato. Se o ato determinar outra data, utilizar a data expressamente documentada.

A data em que o usuário digitou a anulação nunca é usada automaticamente como data de alcance negocial.

A anulação:

- não exclui fisicamente o contrato;
- preserva motivo, fonte comprobatória, autor e data/hora;
- preserva vínculos, alocações, frequências e relatórios para auditoria;
- marca os fatos relacionados como atingidos pela anulação;
- retira a validade desses fatos da visão operacional oficial;
- nunca move automaticamente colaboradores para outro contrato.

Quando for necessário corrigir o contrato de um colaborador atingido, realizar retificação explícita, preservando o fato anterior e reavaliando frequências e relatórios afetados.

Número + ano de contrato publicado e posteriormente anulado:

- permanecem reservados;
- permanecem pesquisáveis historicamente;
- não podem ser reutilizados em outro contrato.

Se a própria anulação tiver sido registrada incorretamente:

- não reativar silenciosamente o contrato;
- criar evento de retificação/cancelamento da anulação;
- preservar a anulação original;
- reavaliar explicitamente impactos em vínculos e frequências.

### Contrato FUTURO cancelado antes do início

Se um contrato foi validamente publicado como `FUTURO`, mas seus efeitos forem oficialmente cancelados antes da data inicial:

- usar estado `CANCELADO`;
- preservar contrato, motivo e documentos;
- cancelar programações futuras relacionadas de forma auditável;
- não classificar como `ANULADO`, porque o instrumento existiu validamente;
- não produzir efeitos operacionais;
- manter número + ano reservados e pesquisáveis historicamente;
- não liberar o identificador para outro contrato, pois houve publicação.

Diferenças:

- `CANCELADO`: instrumento válido cujos efeitos futuros foram cancelados antes de começar;
- `ANULADO`: fato publicado posteriormente declarado inválido;
- `ENCERRADO`: contrato válido que produziu efeitos e terminou.

## 5.22. Frequência

A frequência diária deve resolver o Vínculo e a Alocação válidos na data da jornada.

### Janela normal do Usuário Operacional

- edição por N dias civis configuráveis, padrão30, considerando o fuso `America/Sao_Paulo`;
- a própria data da frequência é o dia 1;
- edição enquanto agora for estritamente anterior ao início de D+N; inclusão/retificação não reinicia a janela;
- exemplo padrão N=30: frequência de01/01 fecha em31/01 às00:00, limite exclusivo, passando à condição negocial `FECHADA`.

O fechamento é regra negocial e não depende de lote, exportação, relatório ou processamento técnico.

Depois do fechamento:

- Operacional não faz alteração comum;
- qualquer mudança é retificação versionada.

O Gestor Local:

- não fecha antecipadamente uma frequência antes do fim da janela configurada;
- pode corrigir dentro ou fora da janela normal;
- toda alteração do Gestor gera versão/retificação formal;
- se o Gestor corrigir determinado registro durante a janela, aquele registro deixa de ser editável pelo Operacional;
- os demais registros continuam seguindo suas próprias janelas.

Alterações do Operacional dentro da janela permanecem em trilha de auditoria.

### Jornada noturna que cruza a meia-noite

A frequência pertence à **data de início da jornada**.

Exemplo: jornada de 22:00 de 10/09 até 06:00 de 11/09 pertence a 10/09 e usa Vínculo/Alocação válidos em 10/09. Mudança de alocação em 11/09 não altera a jornada iniciada no dia anterior.

### Fato retroativo que remove cobertura válida

Se qualquer fato ou retificação retroativa fizer a pessoa deixar de possuir cobertura válida de Contrato/Vínculo em determinado dia:

- preservar a frequência original;
- sinalizar que foi atingida pela correção histórica;
- registrar o motivo;
- retirar o lançamento da composição operacional oficial daquele período;
- não misturar o registro aos indicadores operacionais válidos;
- manter consulta em auditoria;
- tratar correção por retificação quando aplicável.

### Autorização temporal da frequência

A permissão para editar combina:

**autorização atual do usuário + escopo histórico do registro**.

Regras:

- usar a Alocação histórica do colaborador na data da frequência;
- verificar se o usuário possui hoje autorização sobre aquela Lotação/escopo;
- permissão antiga do usuário não concede acesso depois de removida;
- se nenhum Operacional atual possuir acesso ao escopo histórico, o tratamento passa ao Gestor Local.

### Concessão contextual de leitura de ocorrência

Decisão humana da campanha de 09/11/2026 para PER-010:004: o Administrador atual pode conceder acesso pessoal restrito para **uma ocorrência identificada**, a beneficiário ativo com perfil de consulta institucional e escopo válido sobre todos os dias. Finalidade, justificativa, autorização administrativa, validade e auditoria são obrigatórias. Antes de conceder, o backend exige prova histórica completa do período; permissão genérica ou cadastro atual não preenchem uma origem ausente.

A concessão é somente de leitura do fato e de seus anexos protegidos. Não abrange outra ocorrência, frequência ou documento contratual, nem concede cancelamento, edição ou escrita negocial. Cada uso exige novamente autorização atual, contexto histórico comprovado, finalidade compatível e concessão vigente; revogação ou perda de escopo impede novo uso. O registro da concessão e dos usos conserva autoria e auditoria. Essa modalidade não equivale às concessões genéricas por contrato/Gestão ou às permissões documentais próprias.

A decisão define a regra; implementação, contraprovas e aceite técnico estão registrados em `DOCUMENTACAO/CODEX/CAMPANHA_20261109`, sem aprovação por herança dos resultados históricos.

### Três dimensões da frequência

Separar conceitualmente:

1. **visibilidade do registro** — continua existindo em histórico/relatório;
2. **validade para cálculo operacional** — define se entra nos indicadores vigentes;
3. **estado de auditoria** — original, alterada, retificada, substituída ou sujeita a revisão.

Um registro pode permanecer visível para auditoria e, ao mesmo tempo, não compor o indicador operacional vigente.

## 5.23. Relatórios

Relatório oficial atual:

- usa a verdade válida/retificada vigente;
- deve refletir retificações concluídas;
- não apaga versões anteriores.

Quando retificação retroativa alterar resultado já emitido:

- nova emissão é identificada como retificada;
- registra data/hora de geração;
- a versão anterior continua auditável.

Uma emissão administrativa deve registrar:

- período;
- data/hora de geração;
- usuário responsável;
- versão;
- existência de pendências;
- indicação de emissão original ou retificada.

Relatório com retificação pendente:

- pode ser visualizado e exportado;
- deve estar claramente marcado como `sujeito a revisão`.

A auditoria deve permitir consultar:

- a verdade efetiva atual sobre uma data passada;
- o que o sistema conhecia até determinada data de registro.

### Consolidação após retificação

Toda retificação que alterar cálculo ou resultado consolidado deve:

- preservar a versão anterior;
- marcar a versão anterior como histórica/superada para a visão vigente;
- recalcular a verdade operacional atual;
- atualizar indicadores, caches e consolidações dependentes;
- permitir comparação antes/depois em auditoria.

## 5.24. Documentos e SEI

Contrato publicado deve possuir pelo menos uma fonte comprobatória: anexo, referência SEI ou ambos.

Aditamento exige pelo menos uma fonte comprobatória: anexo, referência SEI ou ambos.

Remanejamento pode possuir referência SEI e anexo opcional.

Regras:

- o mesmo processo SEI pode referenciar vários contratos;
- SEI não é identificador único do contrato;
- referência SEI incorreta em fato publicado é corrigida por versão auditável;
- a referência anterior permanece no histórico;
- contrato publicado apenas com SEI pode receber anexo posteriormente;
- o novo anexo não substitui silenciosamente o SEI;
- arquivo associado a fato publicado não é apagado fisicamente;
- arquivo incorreto é marcado como inválido/substituído e permanece auditável.

### Classificação de acesso dos documentos

Cada documento anexado deve possuir classificação de acesso própria.

Categorias mínimas:

- **Negocial** — documento contratual/administrativo necessário à consulta e auditoria;
- **Pessoal/Restrito** — documento com dados pessoais ou informações que não devem ser amplamente liberadas.

A permissão não depende apenas do evento ao qual o arquivo está anexado.

Perfis de consulta podem acessar documentos negociais dentro de seu escopo, mas isso não libera automaticamente dados pessoais/restritos desnecessários à auditoria.

### Acesso a documentos Pessoal/Restrito

- **Gestor Local:** acesso dentro de sua Gestão quando necessário à operação;
- **Gestor Estadual:** acesso somente quando necessário à auditoria estadual e mediante permissão específica;
- **Gestor Nacional:** acesso somente quando necessário à auditoria nacional e mediante permissão específica;
- **Presidente:** sem acesso por padrão a documentos Pessoal/Restrito; acesso normal apenas aos documentos negociais;
- **Administrador técnico:** sem acesso funcional por padrão; acesso excepcional somente para suporte autorizado e auditado;
- **Usuário Operacional:** sem acesso a documentos Pessoal/Restrito deste módulo, salvo requisito futuro específico.

Todo acesso a documento `Pessoal/Restrito` deve ser auditado.

## 5.25. Perfis e permissões

### Gestor Local

É o principal perfil de escrita negocial do módulo dentro da Gestão Local.

### Gestor Estadual

- consulta/auditoria no escopo estadual;
- não executa operação negocial comum de contrato;
- visualiza somente dados pessoais necessários à auditoria;
- pode consultar documentos negociais dentro do escopo estadual.

### Gestor Nacional

- consulta/auditoria nacional;
- não executa operação negocial comum;
- visualiza somente os dados pessoais necessários à auditoria nacional;
- dados pessoais/restritos adicionais exigem permissão específica;
- pode consultar documentos negociais dentro do escopo nacional.

### Presidente

- consulta nacional;
- visualiza somente os dados pessoais necessários à consulta institucional;
- dados pessoais/restritos adicionais não são liberados por padrão;
- pode consultar documentos negociais dentro do escopo nacional;
- não executa operação negocial comum.

### Administrador

- executa administração técnica;
- não deve ser confundido com responsável negocial;
- eventual intervenção negocial excepcional precisa ser explicitamente autorizada e fortemente auditada.

### Usuário Operacional

Atua na frequência dentro do escopo e da janela permitidos.

## 5.26. Auditoria

Toda operação crítica deve registrar, conforme aplicável:

- usuário responsável;
- data/hora da ação;
- data efetiva;
- motivo/justificativa;
- valor anterior;
- valor novo;
- versão anterior;
- versão vigente.

Retificação de retificação é permitida sem limite artificial de versões.

A cadeia deve permitir identificar:

- qual versão está vigente;
- qual versão foi retificada;
- quais versões foram superadas;
- qual era a versão conhecida em determinada data.

### Revalidação da prévia de impacto

Toda operação crítica com prévia de impacto deve revalidar a situação no momento da confirmação.

Se o impacto relevante mudar entre prévia e confirmação:

- recalcular;
- impedir confirmação com dados desatualizados;
- reapresentar impactos;
- exigir nova confirmação.

Aplica-se, no mínimo, a encerramento, anulação, redução, retificação retroativa, inativação retroativa e remoção/zeramento de composição.

### Prevenção de duplicidade acidental

Cliques repetidos, reenvio de formulário, repetição de requisição ou falha de rede não podem criar fatos negociais duplicados por acidente.

Aplica-se especialmente a aditamento, remanejamento, desligamento, retificação, encerramento, anulação e inativação/reativação.

Se a mesma operação já estiver registrada, a interface deve informar isso e evitar nova gravação equivalente.

### Central de pendências

O sistema deve possuir visão consolidada de pendências.

Cada pendência deve indicar, quando aplicável:

- contrato;
- pessoa/composição;
- motivo;
- data em que surgiu;
- operação de origem;
- perfil responsável;
- estado `ABERTA`, `EM_TRATAMENTO` ou `RESOLVIDA`;
- vínculo com o evento/histórico de origem.

Uma pendência deve bloquear apenas o escopo realmente afetado.

Exemplos:

- pendência de frequência afeta aquela frequência/período;
- programação futura órfã bloqueia a operação que a tornaria inconsistente;
- fim de vigência bloqueia a operação do contrato;
- excedente permanece visível, mas não bloqueia automaticamente todo o contrato.

## 5.27. Pesquisa e consulta

A pesquisa de colaboradores deve permitir filtros por:

- nome;
- CPF;
- contrato;
- Lotação/Turno quando pertinente;
- `Vínculos iniciados`;
- `Desligados`;
- ano;
- mês.

Terminologia obrigatória:

- usar **Vínculos iniciados** em vez de “admitidos”;
- usar **Desligados** em vez de “demitidos”.

Mês/ano da pesquisa usam a **data efetiva** do fato, não a data em que o registro foi digitado.

Exemplo:

- vínculo efetivo em 25/08;
- cadastrado no sistema em 03/09;
- deve aparecer em `Vínculos iniciados em agosto`.

## 5.28. Proteção do histórico dos cadastros

Não excluir fisicamente, quando já utilizados historicamente:

- Cargo;
- Lotação;
- Turno;
- Tipo de Contratação;
- Motivo de Desligamento;
- Contrato publicado;
- documentos de fatos publicados.

A inativação deve preservar o uso histórico.

---

# 6. Matriz canônica de requisitos aprovados

## 6.1. Governança e integridade do levantamento

| ID | Requisito fechado para implementação |
|---|---|
| **GOV-003** | Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável. |
| **GOV-004** | Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação. |

## 6.2. Temporalidade e reconstrução histórica

| ID | Requisito fechado para implementação |
|---|---|
| **TMP-001** | Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos. |
| **TMP-002** | Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo. |
| **TMP-003** | Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável. |
| **TMP-004** | O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica. |
| **TMP-005** | A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato. |
| **TMP-006** | Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida. |
| **TMP-007** | O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia. |
| **TMP-008** | Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente. |
| **TMP-009** | Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente. |
| **TMP-010** | Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada. |
| **TMP-011** | Eventos da mesma data que não colidem podem coexistir. |
| **TMP-012** | Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos. |
| **TMP-013** | Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo. |
| **TMP-014** | Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito. |
| **TMP-015** | Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência. |
| **TMP-016** | Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão. |
| **TMP-017** | `FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito. |

## 6.3. Contrato e ciclo de vida

| ID | Requisito fechado para implementação |
|---|---|
| **CON-001** | Número + ano identificam unicamente o contrato em todo o sistema. |
| **CON-002** | Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade. |
| **CON-003** | Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável. |
| **CON-004** | Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização. |
| **CON-005** | Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico. |
| **CON-006** | Data de início e fim de vigência são obrigatórias para contrato publicado. |
| **CON-007** | Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências. |
| **CON-008** | Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto. |
| **CON-009** | O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões. |
| **CON-010** | Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável. |
| **CON-011** | Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita. |
| **CON-012** | Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa. |
| **CON-013** | Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período. |
| **CON-014** | Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos. |
| **CON-015** | Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora. |
| **CON-016** | Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI. |
| **CON-017** | Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido. |
| **CON-018** | Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente. |
| **CON-019** | Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização. |
| **CON-020** | Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa. |
| **CON-021** | Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO. |
| **CON-022** | A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato. |
| **CON-023** | Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado. |
| **CON-024** | Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação. |
| **CON-025** | Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador. |

## 6.4. Empresa contratada

| ID | Requisito fechado para implementação |
|---|---|
| **EMP-001** | Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada. |
| **EMP-002** | Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos. |
| **EMP-003** | Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real. |

## 6.5. Cadastros auxiliares e catálogos

| ID | Requisito fechado para implementação |
|---|---|
| **CAT-001** | Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre. |
| **CAT-002** | Cargo é cadastro próprio administrado pela Gestão Local. |
| **CAT-003** | Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos. |
| **CAT-004** | Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual. |
| **CAT-005** | Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação. |
| **CAT-006** | Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria. |
| **CAT-007** | Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva. |
| **CAT-008** | Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita. |
| **CAT-009** | Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas. |

## 6.6. Composição contratual

| ID | Requisito fechado para implementação |
|---|---|
| **CMP-001** | Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático. |
| **CMP-002** | Não pode haver combinação duplicada no mesmo recorte temporal. |
| **CMP-003** | Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato. |
| **CMP-004** | Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente. |
| **CMP-005** | Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível. |
| **CMP-006** | Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida. |
| **CMP-007** | Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história. |
| **CMP-008** | Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo. |
| **CMP-009** | Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria. |
| **CMP-010** | Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente. |
| **CMP-011** | Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente. |

## 6.7. Redução e efetivo contratual

| ID | Requisito fechado para implementação |
|---|---|
| **RED-001** | Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas. |
| **RED-002** | Limite de 25% é verificado sobre o total do contrato, não individualmente por composição. |
| **RED-003** | Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados. |
| **RED-004** | Aumento contratual não possui limite de 25%. |
| **RED-005** | Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada. |
| **RED-006** | O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo. |
| **RED-007** | Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro. |
| **RED-008** | Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada. |

## 6.8. Aditamentos

| ID | Requisito fechado para implementação |
|---|---|
| **ADI-001** | Contrato pode possuir vários aditamentos sem apagar registros anteriores. |
| **ADI-002** | Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória. |
| **ADI-003** | Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante. |
| **ADI-004** | Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto. |
| **ADI-005** | Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica. |
| **ADI-006** | Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data. |
| **ADI-007** | Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada. |
| **ADI-008** | Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados. |
| **ADI-009** | Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação. |
| **ADI-010** | Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios. |

## 6.9. Remanejamentos

| ID | Requisito fechado para implementação |
|---|---|
| **REM-001** | Remanejamento é fato próprio, identificado, datado, justificado e auditado. |
| **REM-002** | Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero. |
| **REM-003** | Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento. |
| **REM-004** | Remanejamento pode ser retificado/versionado, preservando original e cadeia completa. |
| **REM-005** | Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade. |
| **REM-006** | Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação. |
| **REM-007** | Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito. |

## 6.10. Documentos e SEI

| ID | Requisito fechado para implementação |
|---|---|
| **DOC-001** | Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos. |
| **DOC-002** | Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI. |
| **DOC-003** | Remanejamento pode registrar SEI e mantém anexo opcional. |
| **DOC-004** | Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato. |
| **DOC-005** | Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior. |
| **DOC-006** | Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI. |
| **DOC-007** | Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável. |
| **DOC-008** | Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo. |
| **DOC-009** | Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito. |
| **DOC-010** | Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado. |

## 6.11. Pessoa / Colaborador

| ID | Requisito fechado para implementação |
|---|---|
| **COL-001** | Nome e CPF são obrigatórios. |
| **COL-002** | RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais. |
| **COL-003** | Endereço é mantido e opcional. |
| **COL-004** | Dados bancários foram removidos do cadastro deste fluxo. |
| **COL-005** | CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface. |
| **COL-006** | CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF. |
| **COL-007** | Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente. |
| **COL-009** | Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa. |
| **COL-010** | Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época. |
| **COL-011** | Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita. |
| **COL-012** | Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época. |
| **COL-013** | Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa. |

## 6.12. Matrícula

| ID | Requisito fechado para implementação |
|---|---|
| **MAT-001** | Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa. |
| **MAT-002** | Matrícula é opcional no vínculo e única dentro do contrato. |
| **MAT-003** | Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada. |
| **MAT-004** | Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula. |
| **MAT-005** | Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida. |

## 6.13. Vínculo contratual

| ID | Requisito fechado para implementação |
|---|---|
| **VIN-001** | Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional. |
| **VIN-002** | Antiga data de admissão é substituída por início efetivo do vínculo. |
| **VIN-003** | Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes. |
| **VIN-004** | Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo. |
| **VIN-005** | Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva. |
| **VIN-006** | Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido. |
| **VIN-007** | Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente. |
| **VIN-008** | Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa. |
| **VIN-009** | Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior. |
| **VIN-010** | Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local. |
| **VIN-011** | Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência. |
| **VIN-012** | Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita. |
| **VIN-013** | Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo. |
| **VIN-014** | Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores. |
| **VIN-015** | Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria. |

## 6.14. Alocação

| ID | Requisito fechado para implementação |
|---|---|
| **ALO-001** | Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo. |
| **ALO-002** | Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação. |
| **ALO-003** | Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades. |
| **ALO-004** | Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior. |
| **ALO-005** | Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar. |
| **ALO-006** | Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data. |
| **ALO-007** | Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida. |
| **ALO-008** | Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida. |

## 6.15. Desligamento

| ID | Requisito fechado para implementação |
|---|---|
| **DES-001** | Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato. |
| **DES-002** | Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte. |
| **DES-003** | Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado. |
| **DES-004** | Desligamento possui motivo de catálogo, responsável e data/hora de registro. |
| **DES-005** | Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição. |
| **DES-006** | Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia. |

## 6.16. Frequência

| ID | Requisito fechado para implementação |
|---|---|
| **FRE-001** | Frequência resolve vínculo e alocação válidos na data negocial. |
| **FRE-002** | Usuário Operacional pode editar por N dias civis configuráveis, padrão30, contando a data da frequência como dia1, em America/Sao_Paulo; inclusão/retificação não reinicia a janela. |
| **FRE-003** | Edição enquanto agora < início de D+N, limite exclusivo. Exemplo padrão N=30: frequência de01/01 bloqueada para Operacional a partir de31/01 00:00. |
| **FRE-004** | Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora. |
| **FRE-005** | Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente. |
| **FRE-006** | Gestor Local pode corrigir antes do fim da janela configurada, com identificação e justificativa. |
| **FRE-007** | Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria. |
| **FRE-008** | Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis. |
| **FRE-009** | Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada. |
| **FRE-010** | Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores. |
| **FRE-011** | Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências. |
| **FRE-012** | Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada. |
| **FRE-013** | No início de D+N, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação. O atraso do processamento técnico não amplia a janela. |
| **FRE-014** | Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional. |
| **FRE-015** | A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador. |
| **FRE-016** | Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original. |
| **FRE-017** | Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial. |
| **FRE-018** | Permissão de edição combina autorização atual do usuário com o escopo histórico do registro. |
| **FRE-019** | Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria. |

## 6.17. Relatórios

| ID | Requisito fechado para implementação |
|---|---|
| **REL-001** | Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores. |
| **REL-002** | Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração. |
| **REL-003** | Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro. |
| **REL-004** | Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão. |
| **REL-005** | Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada. |
| **REL-006** | Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica. |

## 6.18. Auditoria e versionamento

| ID | Requisito fechado para implementação |
|---|---|
| **AUD-001** | Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável. |
| **AUD-002** | Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva. |
| **AUD-003** | Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento. |
| **AUD-004** | Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação. |
| **AUD-005** | Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede. |
| **AUD-006** | Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário. |
| **AUD-007** | Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal. |

## 6.19. Perfis e permissões

| ID | Requisito fechado para implementação |
|---|---|
| **PER-001** | Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão. |
| **PER-002** | Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial. |
| **PER-003** | Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial. |
| **PER-004** | Presidente atua somente em consulta nacional neste módulo. |
| **PER-005** | Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte. |
| **PER-006** | Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria. |
| **PER-007** | Correção excepcional de CPF é autorizada pelo Gestor Local. |
| **PER-008** | Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado. |
| **PER-009** | Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos. |
| **PER-010** | Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro. |
| **PER-011** | Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica. |
| **PER-012** | Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial. |

## 6.20. Histórico e rastreabilidade

| ID | Requisito fechado para implementação |
|---|---|
| **HIS-001** | Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos. |
| **HIS-002** | Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês. |
| **HIS-003** | Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação. |
| **HIS-004** | Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo. |
| **HIS-005** | História é a fonte canônica; projeções atuais são conveniência técnica. |
| **HIS-006** | A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores. |

---

# 7. Semântica temporal consolidada

| Evento | Significado da data efetiva |
|---|---|
| Início do contrato | Primeiro dia em que o contrato vale. |
| Fim natural da vigência | Último dia coberto pela vigência. |
| Aditamento | Primeiro dia em que a nova condição contratual vale. |
| Remanejamento | Primeiro dia em que a nova distribuição vale. |
| Início do vínculo | Primeiro dia em que a pessoa pertence ao vínculo. |
| Mudança de alocação | Primeiro dia da nova Lotação + Turno + Cargo; a faixa anterior termina no dia anterior. |
| Desligamento | Último dia do vínculo; a inatividade começa no dia seguinte. |
| Inativação administrativa | Data em que o bloqueio operacional passa a valer. |
| Reativação administrativa | Data em que a retomada operacional passa a valer. |
| Retificação | A nova versão corrige a linha temporal sem apagar a anterior. |

---


# 7.1. Ciclo de vida funcional do Contrato

A situação negocial/temporal é separada da habilitação administrativa e das pendências.

| Estado | Significado |
|---|---|
| `RASCUNHO` | Ainda não publicado e indisponível para operação. |
| `FUTURO` | Publicado e válido, mas antes da data inicial. |
| `VIGENTE` | Dentro da vigência efetiva. |
| `ENCERRADO` | Contrato válido que produziu efeitos e terminou. |
| `CANCELADO` | Contrato FUTURO válido cujos efeitos foram cancelados antes de começar. |
| `ANULADO` | Contrato publicado posteriormente declarado inválido. |

A habilitação administrativa é eixo independente e pode indicar operacionalmente ativo ou administrativamente inativo durante a vigência.

Pendências são outro eixo independente e bloqueiam somente o escopo necessário.

A situação temporal comum é calculada a partir da linha temporal válida e da data corrente:

- antes do início: `FUTURO`;
- da data inicial até a data final, inclusive: `VIGENTE`;
- depois da data final: `ENCERRADO`.

Essa derivação não cria fato negocial. `CANCELADO` e `ANULADO` somente surgem por evento explícito.

# 7.2. Matriz funcional evento → efeito → impacto → permissão

| Evento | Significado da data | Futuro | Retroativo | Cancelável | Retificável | Justificativa | Executor principal | Impacta frequência |
|---|---|---:|---:|---:|---:|---:|---|---:|
| Publicação | início/condição publicada | Sim | Cadastro retroativo permitido quando documentado | Não apagar; usar cancelamento/anulação conforme caso | Correções excepcionais | Conforme operação | Gestor Local | Indiretamente |
| Cancelamento de contrato FUTURO | antes do início da vigência | Sim | Não se aplica como fato ocorrido | É o próprio evento de cancelamento | Pode ser corrigido por novo evento auditado | Sim | Gestor Local | Pode cancelar programações |
| Aditamento | primeiro dia da nova regra | Sim | Sim | Sim antes do efeito | Sim | Sim | Gestor Local | Sim |
| Remanejamento | primeiro dia da nova distribuição | Sim | Sim | Sim antes do efeito quando programado | Sim | Sim | Gestor Local | Sim |
| Alteração de alocação | primeiro dia da nova alocação | Sim | Sim | Programação futura pode ser cancelada | Sim | Quando aplicável | Gestor Local/fluxo autorizado | Sim |
| Desligamento | último dia do vínculo | Sim | Sim | Programado pode ser cancelado | Sim | Sim | Gestor Local | Sim |
| Fim natural de vigência | último dia do contrato | Não se aplica | Pode ser reconstruído por fato posterior | Não | Pode ser afetado por prorrogação/retificação | Tratamento administrativo | Gestor Local | Sim |
| Inativação administrativa | primeiro dia do bloqueio | Sim quando programada | Sim | Por reativação/evento posterior | Sim | Sim | Gestor Local | Sim |
| Reativação administrativa | primeiro dia da retomada | Sim quando programada | Sim | Por novo evento | Sim | Sim | Gestor Local | Sim |
| Correção número/ano | corrige identificação publicada | Não é fluxo comum | Sim como correção material | Não apaga anterior | Sim | Sim | Gestor Local com permissão especial | Não diretamente |
| Correção de identidade | corrige identidade | Não | Sim | Não apaga anterior | Sim | Sim | Gestor Local | Pode impactar pesquisas/vínculos |
| Retificação de frequência | data da frequência | Não se aplica | Sim | Não apaga original | Sim | Sim | Gestor Local | É a própria frequência |
| Cancelamento de programação | data da programação afetada | Sim | Sim quando se comprova que não ocorreu | Preserva cadeia | Sim | Sim | Gestor Local | Pode impactar |
| Encerramento antecipado | último dia efetivo do contrato | Sim quando documentado | Sim quando fato chega atrasado | Não apaga anterior | Sim | Sim | Gestor Local | Sim |
| Anulação | data efetiva de alcance definida pelo ato anulatório | Pode atingir contrato futuro | Sim | Pode ser retificada/cancelada por novo evento | Sim | Sim | Gestor Local autorizado | Sim |


---

# 8. Critérios gerais de aceite já definidos

A solução será considerada coerente quando conseguir responder corretamente, apenas com os dados mantidos pelo próprio sistema:

## 8.1. Contrato

Em uma data informada, apresentar:

- identificação do contrato;
- empresa;
- vigência válida;
- Tipo de Contratação;
- efetivo total;
- composições válidas;
- aditamentos efetivos;
- remanejamentos efetivos;
- situação temporal;
- habilitação administrativa.

## 8.2. Colaborador

Em uma data informada, apresentar:

- Pessoa;
- Vínculo válido;
- Contrato válido;
- matrícula do vínculo;
- situação do vínculo.

## 8.3. Alocação

Em uma data informada, apresentar exatamente a Lotação + Turno + Cargo válidos.

## 8.4. Mudanças dentro de um período

Para um mês/período, apresentar todas as mudanças de:

- vínculo;
- matrícula;
- Lotação;
- Turno;
- Cargo;
- desligamento;
- fatos contratuais que afetaram a pessoa.

## 8.5. Auditoria

Para qualquer fato relevante, responder:

- o que aconteceu;
- quando passou a valer;
- quando foi registrado;
- quem registrou;
- qual era o valor anterior;
- qual passou a ser o valor vigente;
- quais versões anteriores existem.

## 8.6. Retroatividade

Ao inserir ou retificar um fato retroativo:

- reconstruir a linha temporal;
- preservar fatos posteriores que continuam válidos;
- identificar incompatibilidades;
- não apagar nem mover pessoas silenciosamente;
- sinalizar frequências/relatórios impactados;
- preservar todas as versões necessárias à auditoria.


## 8.7. Cenários de aceite de borda

| Cenário | Resultado esperado |
|---|---|
| Contrato original 100 aumenta para 140 e depois reduz para 75. | Permitido; a base permanece 100 e o piso permanece 75. |
| Contrato originalmente com 101 empregados sofre redução máxima. | Piso inteiro mínimo é 76. |
| Redução anterior é cancelada/retificada e deixa de produzir efeito. | Deixa de consumir o limite cumulativo vigente, permanecendo auditável. |
| Existe fundamento jurídico documentado para redução superior a 25%. | Fluxo comum permanece bloqueado; exceção exige documento, permissão excepcional, Gestor Local autorizado, prévia e auditoria reforçada. |
| Documento antigo é cadastrado depois de documento posterior. | Inserir na posição temporal correta, reaplicar posteriores válidos e apontar conflitos. |
| Dois eventos conflitantes têm a mesma data e nenhuma precedência. | Bloquear conclusão até registrar relação negocial explícita. |
| Inativação retroativa atinge frequência já lançada. | Preservar registro, sinalizar impacto e retificar quando aplicável. |
| Contrato é reativado dentro da mesma vigência. | Vínculos e Alocações ainda válidos retomam operação sem criação artificial de novos registros. |
| Frequência alcança o início de D+N no fuso America/Sao_Paulo, N configurado/padrão30. | Passa automaticamente a `FECHADA`, limite exclusivo. |
| Gestor Local corrige frequência no 3º dia. | Aquele registro não volta ao controle de edição do Operacional. |
| Jornada 22:00–06:00 atravessa meia-noite. | Pertence à data de início e utiliza Vínculo/Alocação daquela data. |
| CPF incorreto é corrigido. | CPF antigo permanece na auditoria e deixa de ser identificador ativo. |
| Transferência excepcional muda contrato da Gestão A para B. | Preserva contexto anterior, exige composição válida/mapeamento posterior e autorização específica. |
| CNPJ muda por troca real de empresa. | Novo fato temporal; empresa anterior permanece histórica. |
| CNPJ era erro material. | Correção excepcional auditada; não é troca de empresa. |
| Catálogo hoje inativo estava válido na data retroativa. | Pode ser usado no fato retroativo. |
| Catálogo inativo é escolhido para evento futuro. | Bloqueado até reativação explícita. |
| Contrato é anulado com alcance desde a origem. | Fatos anteriores permanecem auditáveis, mas perdem validade operacional desde a data inicial do contrato. |
| Ato anulatório determina alcance apenas a partir de data específica. | Invalidar operacionalmente somente a partir da data efetiva documentada. |
| Contrato FUTURO é cancelado antes do início. | Fica `CANCELADO`, não produz efeito e mantém número/ano reservado. |
| Remanejamento FUTURO é cancelado antes do efeito. | Preservar original + cancelamento; nenhuma redistribuição é aplicada. |
| Matrícula configurada como obrigatória para o contrato. | Bloquear ativação/publicação do Vínculo sem matrícula válida. |
| Documento Pessoal/Restrito é acessado. | Autorizar somente conforme perfil/permissão e registrar auditoria do acesso. |
| Prévia mostra 20 impactos e passa a 23 antes da confirmação. | Recalcular e exigir nova confirmação. |
| Usuário perde hoje permissão da Lotação A. | Permissão histórica antiga não autoriza edição; usa autorização atual + escopo histórico. |
| Retificação muda consolidação já emitida. | Nova verdade é recalculada e emissão anterior permanece histórica. |
| Clique/reenvio repete aditamento. | Não criar fato negocial duplicado. |
| Data corrente alcança o início de contrato FUTURO. | Situação consultada passa a `VIGENTE` por derivação temporal, sem criar novo fato negocial. |
| Data corrente ultrapassa o fim da vigência. | Situação consultada passa a `ENCERRADO`; rotinas podem gerar pendência, mas não criar fato negocial. |

---

# 9. Uso do documento

Este é o levantamento funcional acumulativo do módulo.

Ele deve ser utilizado para:

- validação com o cliente;
- manutenção evolutiva;
- planejamento técnico;
- implementação futura;
- criação de testes;
- revisão de aderência do sistema;
- rastreabilidade de comportamento funcional.

Somente regras efetivamente decididas e aceitas são incorporadas a este levantamento. Após a revisão integral e o fechamento das decisões finais, este documento está funcionalmente apto a orientar implementação, manutenção e testes, sujeito apenas à validação formal do cliente e às decisões técnicas de arquitetura que não alterem estas regras de negócio.
