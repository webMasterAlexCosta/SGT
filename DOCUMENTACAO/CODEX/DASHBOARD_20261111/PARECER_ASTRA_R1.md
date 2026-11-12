# Revisão independente Astra — R1

Chat: `01a1cd45-040f-7762-9f55-281305a2db98`. Execução comprovada por `turn_context` do rollout local: `model=gpt-6-astra`, `effort=xhigh` (nenhuma credencial incluída).

**Parecer: não aprovado neste checkpoint.** Identifiquei **2 achados P1 e 4 P2**.

1. **[P1] Operacional não consegue solicitar revisão pela interface.**
   O único acesso a `RevisaoRegistro` está em [RegistrosOrigem.tsx:40](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/React/src/modules/painel/components/PainelDesempenho/RegistrosOrigem.tsx:40), dentro do dashboard. Entretanto, `/painel` e `/api/painel/**` excluem o Operacional. Como somente esse perfil pode solicitar revisão, o fluxo humano aprovado fica inacessível. A solicitação precisa estar disponível na tela operacional autorizada.

2. **[P1] Consulta de revisão expõe justificativas sem minimização.**
   [FrequenciaServico.java:308](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/FrequenciaServico.java:308) autoriza o escopo e devolve diretamente os motivos livres da solicitação e decisão. Administrador, Gestor Nacional e Gestor Estadual podem consultar frequência, mas sua leitura padrão é minimizada. O endpoint novo contorna `LeituraFrequenciaServico`, utilizado pelos eventos existentes para restringir esses textos e auditar concessões excepcionais. Aplicar a mesma política à revisão.

3. **[P2] Cobertura ignora grade complementada por prova histórica.**
   [PainelEfetivoRepositorio.java:79](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java:79) usa exclusivamente `alocacao_vinculo.jornada_semanal_fotografia`. O resolvedor canônico permite complementar essa fotografia por prova histórica, preservando a coluna original. Nesse cenário já contemplado por testes existentes, uma jornada comprovada continua aparecendo como “sem prova” no dashboard, excluindo esperados e lançamentos da cobertura. O agregado precisa considerar a configuração histórica efetiva.

4. **[P2] Admissão preenchida pode ser apresentada como ausente.**
   [PainelEfetivoRepositorio.java:108](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java:108) exige também `data_admissao <= dia`. Após uma recontratação, a projeção atual recebe a nova admissão; consultar um vínculo anterior então reduz a qualidade cadastral e produz “Admissão sem preenchimento”, embora o campo esteja preenchido. Isso diverge da métrica aprovada de completude do cadastro consultado.

5. **[P2] Exportações omitem a evolução das pendências e suas comparações.**
   [exportacao.ts:30](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/React/src/modules/painel/components/PainelDesempenho/exportacao.ts:30) exporta somente registros, presenças e ausências atuais. `evolucaoPendencias` e os valores/intervalos anteriores de cada ponto são descartados. Assim, o gestor que analisa pendências com comparação não consegue reproduzir essa análise no XLSX/PDF. Incluir essas séries, bases e indisponibilidades.

6. **[P2] E2E versionado ainda exige regras substituídas.**
   [painel-desempenho-real.mjs:41](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/React/test/e2e/painel-desempenho-real.mjs:41) exige todas as metas nulas, contrariando as três propostas de 100% aprovadas. Também espera KPIs indisponíveis e uma célula de heatmap que abre registros de falta, comportamento substituído pelo detalhamento de pendências. Esse teste abortará diante da implementação atual; precisa ser alinhado antes de sustentar a homologação.

Sobre a falha histórica: examinei `FormularioProvaHistorica` e `usePreviaHistorica`. Há uma janela potencial entre atualizar as versões e a invalidação posterior pelo efeito, além de ser possível gerar prévia enquanto as versões carregam. O log é compatível com descarte de prévia, mas **não comprovei que essa foi a causa daquela falha**. As guardas de assinatura impedem confirmar intenção obsoleta; o rerun isolado não encerra a investigação.

Os **68 hashes permaneceram íntegros**, assim como o patch `879f62da…df28`. Os logs consultados registram 799 unitários + 7 ITs Java aprovados, typecheck/build aprovado, 22 testes frontend focados aprovados e a execução ampla com 854 aprovados/1 falha. Não reexecutei suítes. Conferi também SVGs locais e correspondência das licenças.

Revisão exclusivamente estática e somente leitura, abrangendo os novos fluxos e seus consumidores pertinentes. Não validei JAR/assets integrados, navegação real, zoom, contraste renderizado, carga ou produção. Modelo/esforço efetivo não foi confirmado pela interface disponível. Nenhum arquivo foi alterado.
