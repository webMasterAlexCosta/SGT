# Pendências finais

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

**A implementação não pode ser declarada 198/198 enquanto houver qualquer ID fora de A.**

| Classificação | Antes | Depois |
|---|---|---|
| A | 0 | 192 |
| B | 149 | 0 |
| C | 49 | 0 |
| D | 0 | 0 |
| E | 0 | 6 |

A = integral e comprovado; B = parcial ou comprovação insuficiente; C = incorreto confirmado; D = não implementado; E = bloqueado por fonte externa/ambiguidade. Ausência de teste executado não é automaticamente D.

## Dependências documentais que testes não podem suprir

### CON-023

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

**Fonte faltante:** Tipo de Contratação que pertencia à Gestão de destino e sua validade na data de transferências legadas sem vínculo de Tipo; nomes e composição/contexto de origem e destino não fotografados.

**Comportamento seguro implementado:** Transferências novas exigem concessão específica, Tipo válido no destino e mapa completo. O replay de transferência legada sem Tipo destino deixa a informação desconhecida; novo ADI explícito só estabelece o Tipo desde o alcance do novo ato. IDs e fotografias anteriores conhecidos permanecem.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Para cada transferência afetada, comprovar Gestão/contexto anterior e posterior, Tipo de destino, pertinência/validade na data, mapa dos recortes e autorização administrativa do ato; não deduzir o Tipo pelo cadastro atual nem pela Gestão de origem.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

### CAT-004

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

**Fonte faltante:** Nomes e configurações efetivos de Gestão, Lotação, Turno/Jornada, Cargo e Tipo nos períodos/fatos em que não foram fotografados. Inclui grade semanal e município quando sua fonte histórica inexiste; município opcional comprovadamente nulo é situação distinta.

**Comportamento seguro implementado:** Leitura histórica utiliza fotografias conhecidas e devolve null para rótulo ausente. Publicação captura configuração definitiva. Nova alocação e deltas quantitativos conservam a configuração/rotulagem histórica; a correção CTR-001 tem GREEN específico posterior à candidata02 e não restaura nomes ausentes.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Para cada item/período sem fotografia, obter o nome e a configuração então válidos, vinculados ao ID estável e ao fato de origem; distinguir ausência de prova de campo opcional documentadamente nulo.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

### CAT-007

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

**Fonte faltante:** Origem e intervalo de disponibilidade anteriores ao primeiro evento comprovado. A flag atual ou o valor ativoAnterior não informa, por si só, desde quando o catálogo era válido.

**Comportamento seguro implementado:** Consulta sem origem mostra disponibilidade desconhecida; uso retroativo sem prova é recusado. Há ato documentado de disponibilidade com escopo, versão, prévia, autoria e duas temporalidades. Uma prova nova certifica apenas o intervalo que documenta; não estende automaticamente a origem para datas anteriores.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Comprovar o início e o término, quando houver, do intervalo válido por fonte datada e atribuída ao catálogo/ID; registrar a origem sem extrapolar ativoAnterior nem apagar as transições recebidas.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

### COL-010

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

**Fonte faltante:** Nome efetivamente usado no período e sua associação comprovada à identidade canônica da pessoa quando inexiste fotografia nominal do fato.

**Comportamento seguro implementado:** Consulta distingue nome atual e fotografia do evento, com ID da fonte e comprovação. Correção cadastral não renomeia o passado. Ato retroativo sem fonte nominal é recusado; um fato contemporâneo pode fotografar o que é conhecido naquele momento sem preencher períodos anteriores.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Comprovar o nome no período e sua ligação à mesma pessoa canônica; distinguir correção de erro histórico de alteração nominal ocorrida depois e registrar o alcance de cada fato.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

### HIS-001

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

**Fonte faltante:** Instrumentos e fontes que comprovem a configuração contratual completa nos recortes afetados: identificação, empresa, vigência, Tipo/política de matrícula, composição/efetivo, configurações e origem dos eventos. Campos ausentes na versão e ausência de toda a versão original são ambos tratados, mas não recuperados.

**Comportamento seguro implementado:** Reconstrução usa publicação conhecida e deltas documentados nos cortes; dados do mestre e primeira fotografia futura não são usados para suprir origem perdida. Campos/habilitação sem fonte permanecem desconhecidos. Essa resposta segura não fornece a configuração completa que o documento original ausente deveria comprovar.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Comprovar todos os campos necessários à configuração dos recortes afetados e a cadeia de atos; qualquer campo indispensável ainda desconhecido mantém a limitação do requisito integral.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

### HIS-006

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

**Fonte faltante:** Instrumento/publicação original e seu contexto completo para os registros que não conservaram a fotografia original. Nova coluna ou uma fotografia de ADI posterior não comprova o conteúdo perdido da publicação.

**Comportamento seguro implementado:** Publicação nova fotografa identificação, empresa, vigência, Tipo, efetivo, composição e configurações com autoria e fonte. A origem perdida não é substituída por ADI ou cadastro atual. Configuração definitiva é revalidada antes de publicar e permanece após edições do mestre.

**Condições de fechamento:**

1. Inventariar os IDs, fatos, campos e intervalos efetivamente sem fonte; identificar o documento autêntico que cobre cada lacuna, com proveniência verificável.
2. Obter a publicação/instrumento original completo e autenticar a ligação com o contrato e composição de origem; uma fonte posterior só serve como prova do original se esse conteúdo e alcance estiverem expressamente demonstrados.
3. Incorporar a prova por procedimento autorizado, auditado e versionado, preservando o recebido, a autoria, a data efetiva e o instante real de conhecimento. Se o fluxo necessário não existe, implementar e verificar esse fluxo antes do aceite; este adendo não o presume existente.
4. Demonstrar em MySQL, por consultas antes/depois dos cortes efetivo e de conhecimento, que a prova cobre precisamente o intervalo declarado, não aparece antes de conhecida e não reescreve eventos/documentos existentes.
5. Concluir os gates da fonte final e a revisão independente dos critérios próprios. Testes sintéticos e tratamento seguro de desconhecidos não substituem os documentos faltantes.

## Outros requisitos ainda pendentes

Nenhum outro ID permaneceu B/C/D na conciliação final. Isso não remove os seis E.

## Limites de verificação e acompanhamento

- O runtime efetivamente usado foi Temurin 21.0.12.1, com compilação release 17. Execução em JVM 17 não foi realizada.
- O wrapper Linux foi executado via bash com download e checksum. O roteiro Windows/PowerShell recebeu revisão estática; não foi executado neste ambiente.
- O perfil opcional PIT de mutation testing não foi executado; a entrega não declara escore de mutação. A regressão, os oráculos de negócio e a cobertura JaCoCo têm provas separadas.
- Avisos condicionais de dependências permanecem descritos no relatório de segurança; superfície não exposta não significa biblioteca corrigida.
- Carga sintética local e amostragem de memória não garantem throughput, disponibilidade ou picos de memória em infraestrutura de produção.
- A disponibilização usa os arquivos deste workspace. A persistência adicional remota foi impedida pela revisão automática de aprovação; esse bloqueio não foi contornado.

Nenhum resultado de teste de versão anterior substitui a comprovação da versão entregue.
