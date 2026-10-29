# SGT/GTRAT — Resultado da Etapa 11

**Data-base:** 10/09/2026  
**Base:** Java 17, Spring Boot 3.5.16, Maven 3.9.16 e MySQL 8.4

## 1. Objetivo concluído

A Etapa 11 fecha a camada administrativa de relatórios e pendências sem alterar a verdade negocial histórica. Relatórios atuais são calculados pela linha temporal vigente; emissões formais preservam exatamente o conteúdo conhecido no momento da geração.

## 2. Central de Pendências

A entidade `PendenciaAdministrativa` registra, de forma explícita:

- tipo, estado, motivo, operação e entidade de origem;
- data efetiva e momento em que a pendência surgiu;
- perfil responsável e atores de início/resolução;
- contrato, pessoa, vínculo, frequência, lotação, jornada e escopo organizacional afetados;
- bloqueio mínimo: frequência/período ou operação do vínculo;
- fotografia textual mínima e submetida à política de minimização.

São geradas pendências idempotentes para:

- frequência marcada como `SUJEITA_REVISAO` após impacto temporal;
- vínculo ainda aberto após o fim natural do contrato.

O tratamento é exclusivo do Gestor Local responsável. A confirmação de resolução revalida a origem: uma frequência ainda sujeita a revisão ou um vínculo ainda aberto impede o encerramento artificial da pendência.

## 3. Relatórios administrativos

Os tipos formais disponibilizados são `FREQUENCIA_CONSOLIDADA` e `ABSENTEISMO`, nos formatos declarados `VISUALIZACAO`, `PDF` e `XLSX`. Independentemente do formato solicitado, a prova histórica é o snapshot JSON canônico. Cada emissão persiste:

- período, filtros normalizados e escopo do emissor;
- data/hora, usuário e perfil responsáveis;
- versão da série, natureza `ORIGINAL` ou `RETIFICADA` e estado `VIGENTE` ou `SUPERADA`;
- quantidade e aviso de pendências existentes;
- vínculo com a emissão anterior;
- snapshot JSON canônico comprimido em GZIP, tamanho original e hash SHA-256;
- chave de idempotência e hash da intenção.

Uma retificação que afeta período e escopo marca somente as emissões alcançadas como superadas, conserva seu snapshot e registra a causa. A próxima emissão da mesma série nasce como retificada e encadeada à anterior.

## 4. Visões temporalmente corretas

- a prévia e a nova emissão usam a verdade efetiva vigente;
- a consulta `conhecidaAte` retorna a última emissão formal que o sistema conhecia até o instante solicitado;
- a comparação vigente expõe hashes e conteúdos histórico/atual sem apagar o anterior;
- frequência e absenteísmo mantêm os filtros temporais existentes;
- alterações de frequência, início/fim de vínculo e encerramento propagam invalidação para consolidações dependentes.

## 5. Aviso nas exportações

Relatórios com retificações ativas continuam consultáveis e exportáveis, mas recebem mensagem visível de `sujeito a revisão` e a quantidade correspondente. O aviso foi incorporado aos DTOs, às planilhas XLSX e aos PDFs já existentes.

## 6. Segurança e auditoria

- somente Administrador, Gestor Nacional, Gestor Estadual e Gestor Local consultam a Central e os relatórios administrativos;
- Estado e Gestão Local ficam limitados ao próprio escopo; acesso indevido responde como recurso não encontrado;
- apenas o Gestor Local responsável trata pendências;
- dados pessoais seguem a minimização definida na Etapa 10;
- emissão, superação e transições de pendência produzem registros de auditoria.

## 7. Critérios arquiteturais preservados

- nenhum papel legado foi reintroduzido;
- permissão excepcional não foi convertida em perfil;
- o histórico não é sobrescrito para fabricar o estado atual;
- não existe rotina de migração/reconciliação de banco antigo;
- o React permanece fora do pacote até o congelamento final da API na Etapa 12.
