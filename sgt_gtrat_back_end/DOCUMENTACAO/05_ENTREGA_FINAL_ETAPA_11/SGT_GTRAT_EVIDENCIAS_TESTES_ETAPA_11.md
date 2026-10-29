# SGT/GTRAT — Evidências de testes da Etapa 11

**Execução:** 10/09/2026

## 1. Ambiente validado

| Item | Evidência |
|---|---|
| Java | compilação Maven com `javac release 17`; runtime Java 17.0.20 |
| Spring Boot | parent `3.5.16` no `pom.xml` |
| Maven | Apache Maven 3.9.16 fornecido no pacote de dependências |
| Fontes principais | 563 compiladas |
| Fontes de teste | 120 compiladas |
| JaCoCo | relatório gerado; bundle analisado com 624 classes |

## 2. Regressão integral

Comando equivalente executado na raiz de `PROJETO_BACKEND`:

```bash
mvn -o clean test
```

Resultado:

```text
Tests run: 383, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os 375 testes herdados da Etapa 10 permaneceram verdes. Foram acrescentados 8 testes específicos da Etapa 11.

## 3. Testes novos

| Classe | Testes | Cobertura principal |
|---|---:|---|
| `PendenciaAdministrativaServicoEtapa11Test` | 3 | geração de pendência de frequência, contagem/aviso e fim natural |
| `RelatorioAdministrativoServicoEtapa11Test` | 3 | emissão original, emissão retificada e consulta conhecida até um instante |
| `InvalidacaoEmissaoRelatorioServicoEtapa11Test` | 1 | invalidação seletiva por período e escopo |
| `ExportacaoRelatorioAvisoEtapa11Test` | 1 | mensagem de revisão dentro do XLSX efetivamente gerado |
| **Total** | **8** | **Etapa 11** |

## 4. Integração MySQL preparada

`Etapa11PersistenceIT` cobre a persistência real de pendências e emissões administrativas, incluindo enumerações, relacionamentos, LOB do snapshot e consultas de repositório.

O MySQL 8.4.11 anexado foi descompactado e seu diretório de dados novo foi inicializado com sucesso. Contudo, o processo não pôde abrir socket Unix ou TCP neste executor: a chamada de sistema retorna `Operation not permitted`. Uma reprodução mínima com `socket(AF_UNIX)` confirmou que a limitação é do ambiente, não do projeto.

Consequência: a execução MySQL da Etapa 11 **não é contabilizada como aprovada** nesta entrega. Ela permanece como primeiro gate da Etapa 12 em ambiente com sockets permitidos. Os testes nativos históricos do checkpoint anterior não substituem essa nova homologação.

## 5. Build do artefato

Após a suíte, deve ser executado:

```bash
mvn -o -DskipTests package
```

O build terminou com `BUILD SUCCESS` e gerou `target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar`; a inspeção do arquivo confirmou as classes da Central, das emissões e da invalidação. O ZIP final exclui `target` para manter somente código, testes e documentação reproduzíveis.

## 6. Critério de aceite

- regressão automatizada: aprovada;
- testes novos da Etapa 11: aprovados;
- compilação Java 17: aprovada;
- persistência nativa MySQL da Etapa 11: preparada, não executada por restrição externa;
- homologação final: reservada à Etapa 12.
