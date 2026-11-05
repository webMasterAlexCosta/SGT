# Legado retirado e APIs canônicas

A comparação byte a byte entre Java.zip e o checkpoint identifica 35 arquivos Java de produção que já estavam retirados no checkpoint. A árvore entregue continua sem esses arquivos. Esta retirada não é contabilizada entre as novas correções desta sessão; o código e os documentos originais permanecem preservados na entrada e identificados pelo inventário.

## Correspondência operacional

| Caminho antigo | Tratamento na árvore entregue | Prova atual |
|---|---|---|
| POST/PUT/DELETE de `/api/empregados` e `/api/empregados/{id}` | Escrita canônica por Pessoa, Vínculo e Alocação; a projeção Empregado continua disponível às leituras autorizadas. | Registro real de mappings Spring em `ConclusaoApiCanonicaHttpIT`. |
| CRUD de `/api/bancos`, `/api/agencias`, `/api/contas`, `/api/tipos-conta`, `/api/telefones` | Raízes descontinuadas sem mappings Spring. Não são restauradas como escrita alternativa de pessoa. | O mesmo teste percorre todos os mappings e exige ausência dessas raízes. |
| CRUD isolado de `/api/enderecos` | Dados opcionais pelo PUT `/api/pessoas/{id}/dados-opcionais`; a entidade Endereco é conservada. | Preservação de identidade/vínculo, autorização de escopo, auditoria após commit e controle concorrente real. |

Os controles de dados opcionais se relacionam a COL-003, COL-011 e AUD-001 no teste lido. A ausência de CRUD legado não é um requisito funcional adicional. Os contratos atuais completos e seus consumidores estão descritos em [INTEGRACAO_FRONTEND.md](INTEGRACAO_FRONTEND.md) e [MATRIZ_ENDPOINTS.md](MATRIZ_ENDPOINTS.md). O frontend recebido não foi alterado.

## Execução atual

As quatro invocações de `ConclusaoApiCanonicaHttpIT` passaram na regressão integral06, com o mesmo manifesto de fontes da candidata07. O JSON desta matriz preserva nomes, estados e hashes dos XMLs. Os testes incluem releitura após commit/rollback e uma espera observada no MySQL para atualização concorrente de campos opcionais.

## Arquivos retirados antes deste atendimento

| Arquivo Java | SHA-256 no original | Estado no checkpoint / entrega |
|---|---|---|
| `src/main/java/com/example/siggtrat/auditoria/DiferencasAuditoriaEmpregado.java` | `a77b9edd49514793f5deca09e5b4d558348e06483d8075464f3982db035e2a81` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/auditoria/NarradorAlteracoesEmpregado.java` | `d8c3340e14a68677fd9973f868fa7458e69feb2329cd470897f28ff480731682` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/AgenciaControlador.java` | `5f7759b22c12943873fb2681ea9dd4b27151fbdcc1b9e0c0a518e5aa919eb475` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/BancoControlador.java` | `a5aa2a167581bd84e5976403345b2885561a86483fde8bcc1d7b0d095ba16278` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/ContaControlador.java` | `e24e130623864ae76cd1d081c047c627b8877bfd1ce14583b3a664a2e430bce3` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/EnderecoControlador.java` | `ea56d8731ec2948f2f7774cdacbc78193faad52007845c2368a8856e84be05ee` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/TelefoneControlador.java` | `9d5f6d1123b66ffb383f5ca232971e9b3508f14ff9a2d906b00526b2305d4340` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/controlador/TipoContaControlador.java` | `173584289ab7d6e4a8a4a060cd75c67564d82cbd726c07060470b9a7654951bb` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/AgenciaDTO.java` | `bc85d253671648299098a9fb57cc6a11455587e56b5be99a3134b871c7c0ee8a` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/BancoDTO.java` | `498c14590d2ab5da3e0672e05fc623fc9b88e42e63c95d7cdaab66ac0b089bae` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/ContaDTO.java` | `8e859871c22fb6d3da76865b9054ff9f4bab8b6ae0b43db0f13f8c79cde68e9e` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/EmpregadoContaDTO.java` | `bc4118366756bb8b932865b7988ac5695cf73f66c7a59c8257bf5e4415f7156c` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/EmpregadoEnderecoDTO.java` | `d22cad4a739b2e8f4586f5c93f2da482b39c46bd07f13898acabd04ca7dd48cb` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/EmpregadoTelefoneDTO.java` | `af2a5fd176939cc190b7cf637c6aa0b3f8ca54df74424a8e9a1fde58880ff70c` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/TelefoneDTO.java` | `c358e96040e15e801d13ee94e555758ae2622fc27e6ab054ac754d616c81ff10` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/dto/TipoContaDTO.java` | `8e98bd7a8c5dc9a703176befbfe701287ed45359825bc4e4a5260c0f65fc5a77` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/entidade/Agencia.java` | `ffc139be232e78ce538206478ec86ff6a6f02b051c109b78c6301f37ebd2fd93` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/entidade/Banco.java` | `9934a592feb44ede7f6e5901ced92321a9fe98c10d4e08de56acbdbd9760dfd9` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/entidade/Conta.java` | `e5ca3bf04493b6aa47b500176da1e349aea01506448123b2ae8b66a217a3a448` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/entidade/Telefone.java` | `b406314974a950cffea0c5f8a274d4632971759813e0572080cc7b694e456c4d` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/entidade/TipoConta.java` | `7c15515fd085eb5afd18c96593f24d54804437be7d1a9612e08d073e36a04ddc` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/AgenciaRepositorio.java` | `7f4c5ed58588d2bd6fd737eb42ab9ce3399b2478138d0cb17ebd35657a7e11c2` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/BancoRepositorio.java` | `6fa23482a9342181971ab74c1b8ae1feb554b7040bff912e870c082680ad4ea6` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/ContaRepositorio.java` | `d1de21daa6817b17eb1cc3bcf320516a2bfe06dfa19cbf0fc04a2d0ea3582efd` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/EnderecoRepositorio.java` | `5e1bfad8a3e44b4dc15cba46395712da13d32db119ea31db985ae9a2a4e02b28` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/TelefoneRepositorio.java` | `01dd3e00721537b5367852849ec0ca41fdec7339a173c3024d0119c9894ec208` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/repositorio/TipoContaRepositorio.java` | `e4b6f37de38d1c56156444b2858adf87d3f8a5839fdf144c712e6029b6c7c43b` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/AgenciaServico.java` | `cc7a06d521a8d4e783331f61579f503491a9e6144c3397558127a3c3bcdb12f7` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/BancoServico.java` | `257df012ea32e6b2be04c362b870951b507745db2245dc781767f95e12c77159` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/CompatibilidadeEmpregadoCanonicoServico.java` | `5e5c161b7d12edbedac3569e72fc2870a5693fe8839cae8cee3573bdc0472910` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/ContaServico.java` | `53f0447069505b25446444489157e1a0cd3e69bbaf3eda5c628efbdb1c8ebb39` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/EnderecoServico.java` | `e703b64556c7946d2b668dcd4bf77297e01b0b835a578c3d0eee4e3c387c5784` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/TelefoneServico.java` | `11857853a60aa3f7f1aa433d22acd08387e4e8e949f9168d6c74064376d37520` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/servico/TipoContaServico.java` | `103cb6a77e4c0190f137e12490d31d3f9372412e1a67e4cd644eea22291930a2` | Ausente / ausente |
| `src/main/java/com/example/siggtrat/utilitario/UtilitarioMatriculaEmpregado.java` | `eece22f8d284c769cdcf9616d1d437bcea2677347d0c4c87927d825fdc3a6d77` | Ausente / ausente |

## Limites

Não foram apagados eventos históricos, fotografias de negócio, documentos de autoria ou registros de domínio para provar a retirada de rotas. O schema atual foi gerado em catálogo vazio exclusivamente pelo Hibernate update; o inventário de classes não autoriza remover tabelas manualmente de uma base existente.

Fontes: `../analise/comparacao_base.md`, `../analise/inventario_comparacao_base.json`, `../evidencias/rodadas/regressao_integral_06/RESULTADOS_METODOS.json` e a matriz JSON que acompanha este documento.
