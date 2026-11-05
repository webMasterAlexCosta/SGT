# JAR, HTTP/TCP e reinício

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

JAR SHA-256: `582dfe319873c94d58ad4b4ccea47311e98c9528ed8a24d18d01de4fad168946`. Gate aprovado: **True**.

As classes BOOT-INF/classes do artefato foram comparadas byte a byte às classes efetivamente exercitadas na regressão final. O processo iniciou com heap limitado, MySQL/Redis reais e Hibernate update. Foram usados login, cookies, CSRF e requisições TCP reais por urllib; não MockMvc para esta prova.

| Roteiro | Fase | Requisições | Asserções | Aprovado | SHA-256 da prova |
|---|---|---|---|---|---|
| validar_jar_tcp.py | criar | 59 | 105 | True | 5abba62e63eb9360875ecb947a407803c1c32051696de625d6b572835d337db7 |
| validar_jar_tcp.py | reinicio | 30 | 62 | True | f274e6aa83a6787db1a3af84d57d731f42b16850d83799e80c8f6d58ba8d41c2 |
| validar_auditorias_tcp.py | criar | 254 | 410 | True | cf7596d75650d46b3bf214ce52c9ad43dd17788ccd86444762e999646fa2526f |
| validar_auditorias_tcp.py | reinicio | 114 | 242 | True | ad19d1eaf6a4c9306f2e2adcc98fe99d43bc218b3f135376885c04921544876b |

Total principal: 457 requisições e 819 asserções. Após essa prova, os três revisores executaram mais 95 requisições e 336 asserções próprias no mesmo segundo processo. Os hashes, o PID e a janela anterior à parada final foram conciliados; os roteiros permitiram apenas consultas negociais e autenticação. Essas contagens HTTP são separadas das invocações Java e da aprovação por requisito.

Os roteiros verificam criação/publicação, pessoa/vínculo, frequência, idempotência, rollback, PDF e XLSX reais, hash documental, emissão formal, minimização, acessos por perfis, transferências, capacidade concorrente, piso contratual, exportação revogada/restaurada e persistência. A cobertura exata é a lista de asserções dos quatro JSONs; o roteiro TCP não prova todos os cenários de ADI/REM sozinho.

Mesmo JAR após reinício: True; ordem de parada/início verificada: True; schema e todos os dados preservados: True.

Comparadas 72 tabelas / 525 linhas / 138 FKs; órfãos: 0.

[evidencias/jar/PROVA_HTTP_REINICIO.json](../evidencias/jar/PROVA_HTTP_REINICIO.json); [evidencias/jar/CLASSES_JAR_EXERCITADAS.json](../evidencias/jar/CLASSES_JAR_EXERCITADAS.json)

A preparação sintética de autenticação é separada do aceite. O JAR não foi reconstruído entre as duas inicializações. As credenciais/estados privados usados no roteiro não acompanham o pacote.

A tentativa anterior foi reprovada por uma pré-condição ausente no roteiro de anulação e está preservada em `evidencias/jar_tentativa01_reprovada`. A prova aprovada acima foi reexecutada integralmente com o roteiro corrigido, sem alteração das fontes Java ou do JAR. Frequências abertas e fechadas são distinguidas antes do ato e verificadas novamente após reinício.
