# Conclusão dos 198 requisitos — plano de execução

**Objetivo:** corrigir defeitos reproduzidos, implementar os fluxos documentais faltantes e revalidar os 198 requisitos sem fabricar dados históricos.

**Especificação:** `../LEVANTAMENTO_REQUISITOS.md`, prompt de continuidade recuperado e instruções diretas do proprietário nesta execução.

**Arquitetura:** manter controllers/DTOs/serviços/repositórios/entidades existentes. Suplementos documentais serão fatos imutáveis, vinculados à identidade estável, com autoria, fonte, intervalo efetivo e instante real de conhecimento. Não alterar fotografias recebidas. A reconstrução consulta apenas provas conhecidas no corte pedido.

**Tecnologias:** Spring Boot 3.5.16, Hibernate/JPA, MySQL 8.4.11, Redis8.4.8; Java release17 com JDK21 disponível. Nenhum frontend alterado.

## Restrições globais

- Hibernate único gestor das tabelas, ddl-auto=update e sql.init.mode=never.
- Instância nova e isolada, três catálogos sintéticos. A configuração recebida não foi utilizada contra o banco original.
- Limite efetivo8GiB: um único fork, heap768MiB; Maven512MiB; pools8; suíte pesada serializada.
- Sem mocks para persistência/concorrência; RED antes da mudança produtiva, GREEN permanente.
- Históricos não recuperáveis continuam E; aprovação antiga não é aceita automaticamente.

## Focos da revisão

1. Nome atual indevidamente usado em novo fato retroativo e nulidade confundida com desconhecimento.
2. Cancelamento de vigência fonte de atos posteriores, mesmo sem vínculo de pessoa.
3. Precedência de alterações do mesmo campo por rotas diferentes na mesma data.
4. Incorporação documental concorrente/fora do escopo, prova posterior visível antes do conhecimento.
5. Renomeação e mudança de configuração de catálogo alterando fotografia de registro anterior.

## Blocos verificáveis

### 0. Origem e baseline

- [x] Recuperar kit, comparar todos arquivos comuns e verificar JAR esperado.
- [x] Preservar CHECKPOINT_00_ORIGEM_RECUPERADA.zip e manifestos.
- [x] Preparar MySQL/Redis reais isolados e confirmar catálogos vazios.
- [ ] Executar baseline integral, arquivar XMLs/cobertura/memória e recontar casos.

### 1. Reproduções independentes

Arquivos de teste propostos: ContratosVigenciaDependentesNovaAuditoriaHttpIT, ContratosPrecedenciaCnpjNovaAuditoriaHttpIT, NovaAuditoriaFotografiasTardiasPessoasHttpIT; mais hipóteses transversais fundamentadas.

- [ ] Revisar oráculos à luz da norma original.
- [ ] Executar RED em MySQL real; distinguir defeito da aplicação de falha da fixture.
- [ ] Registrar observado/esperado, rollback e IDs afetados.

### 2. Contratos e precedência

Arquivos: AditamentoContratualServico, ImpactoProgramacaoComposicaoServico, CorrecaoCnpjContratoServico, MotorTemporalContratoServico, testes focais.

- [ ] Corrigir somente defeitos confirmados.
- [ ] Demonstrar recusa integral, alternativa válida documentada e concorrência pertinente.
- [ ] Executar GREEN e regressão dos domínios.

### 3. Identidade e catálogos

Arquivos: VinculoContratualServico, PessoaServico e novo resolvedor de prova nominal; MotivoDesligamentoServico; consumidores Frequencia/Ocorrencia e testes focais.

- [ ] Não transformar nome/descrição atual em prova histórica retroativa.
- [ ] Implementar incorporação autorizada e imutável de fotografia nominal/documental com intervalo e conhecimento.
- [ ] Exercitar com fonte válida, sem fonte e fonte incorporada depois; preservar correções de identidade e fotografia existente.

### 4. Publicação, configuração e transferência

Novas entidades/DTOs/repositórios/serviços de prova histórica; integração aos motores de contrato e composição sem substituir fatos originais.

- [ ] Adicionar testes RED para fluxo de incorporação documental ausente.
- [ ] Validar fonte, alvo canônico, escopo, versão, idempotência, autoria e integralidade; recusar conflito com valor comprovado.
- [ ] Respeitar data efetiva e conhecidoAte, preservar fotos/bytes e replay posterior.
- [ ] Revalidar relatórios/cache e testar concorrência/rollback.

### 5. Regressão e JAR

- [ ] Regressão integral da fonte modificada e carga separada.
- [ ] JAR das classes testadas, HTTP real positivo/negativo e leitura MySQL.
- [ ] Reiniciar exatamente o mesmo JAR e comparar dados, schema, arquivos e hashes.

### 6. Revisão e entrega

- [ ] Fonte congelada por hash e revisão independente com testes próprios.
- [ ] Corrigir qualquer achado confirmado, repetir gates afetados e revisão.
- [ ] Matriz individual198/635 atualizada, endpoints, cobertura e pendências documentais objetivas.
- [ ] ZIP consolidado com originais/histórico/checkpoints/evidências e fontes finais.

## Decisões de execução

O pedido já autoriza implementação e execução integral; não se exige nova confirmação do plano. O trabalho usa extração isolada do ZIP, sem modificar repositório externo. As revisões iniciais buscam defeitos; o parecer final avaliará a fonte congelada após as correções. Qualquer ajuste de escopo/oráculo será registrado com justificativa neste diretório.
