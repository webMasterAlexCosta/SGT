# Segurança e autorização

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

A revisão combina leitura independente do código, testes adversariais, persistência e HTTP/TCP real. Não é certificação externa, pentest de infraestrutura produtiva ou garantia de ausência universal de vulnerabilidades.

## Controles exercitados

Autenticação com cookies e CSRF, revogação de sessão, confirmação de senha para comandos sensíveis, perfis e escopo organizacional, concessões excepcionais, minimização de dados pessoais, autorização documental e revalidação de exportações são confrontados com leituras de estado e trilha. As provas por papel, endpoint e cenário estão na matriz de critérios e nos roteiros TCP.

O banco real verifica unicidade e rollback, inclusive na disputa por CPF. A reserva histórica de número contratual impede reutilização indevida, e locks/revalidação impedem aceitar prévias sobre estado superado.

## Dependências do JAR entregue

Consulta OSV concluída: True. Bibliotecas empacotadas: 88; coordenadas públicas consultadas: 96; alertas retornados: 2; bibliotecas sem coordenada: 0.

JAR consultado SHA-256: `582dfe319873c94d58ad4b4ccea47311e98c9528ed8a24d18d01de4fad168946`. Somente nomes Maven e versões foram enviados à API pública OSV; código e credenciais não foram enviados.

| Pacote | Versão | Alertas OSV |
|---|---|---|
| org.springframework:spring-webmvc | 6.2.19 | GHSA-j9f9-w8pj-32f8, GHSA-pc63-qcmh-9cmg |

Os avisos Spring MVC condicionados ao uso de fragmentos de view via SSE e XsltView devem permanecer visíveis quando retornados pelo OSV. O backend REST não expõe essas superfícies segundo a revisão e os testes de contexto `SuperficieRenderizacaoSeguraHttpIT`. Isso limita a aplicabilidade demonstrada; não equivale a dizer que a versão da biblioteca recebeu o patch ou que o produto não tem CVEs. A atualização upstream permanece acompanhável.

Fontes oficiais: [CVE-2026-47890](https://spring.io/security/cve-2026-47890), [CVE-2026-47884](https://spring.io/security/cve-2026-47884), [API OSV](https://google.github.io/osv.dev/post-v1-querybatch/).

## Credenciais e distribuição

As credenciais descartáveis autorizadas foram usadas somente na execução. A distribuição substitui `.env` pela cópia de `.env.example`, remove fixtures privadas e fotografias SQL brutas e mascara segredos nos logs. O manifesto de sanitização informa hashes originais/distribuídos; hashes de fonte e binário não são alterados por essa etapa.

A tentativa de adicionar PROCESS foi rejeitada pela revisão automática de permissões; a concessão não foi executada. A observação foi viabilizada por consultas somente leitura com os acessos já existentes, sem repetir a mudança bloqueada.
