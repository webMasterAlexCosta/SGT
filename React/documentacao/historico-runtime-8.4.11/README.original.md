# Runtime externo sintético desta correção

Siga `../../GUIA_REPRODUCAO.md` a partir deste diretório (ou `GUIA_REPRODUCAO.md` na raiz React). Copie os helpers para o layout `SGT_BASE/runtime` descrito no guia antes de executá-los. Configure segredos novos localmente; nenhum banco, cookie ou credencial acompanha a entrega.

Java/JAR são imutáveis. MySQL8.4 e Redis são reais. O schema é criado/atualizado somente pelo Hibernate, com `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`. Os helpers não aplicam DDL de tabelas nem migrações. O reinício exige `ACCEPTING_TRAFFIC` do novo PID e compara todas as tabelas antes de retomar HTTP.

O README recebido, com resultados da rodada anterior, está preservado em `../../documentacao/baseline/README_RUNTIME_RECEBIDO.md`. Esses resultados não são atribuídos a esta execução. Consulte os relatórios finais e os recibos novos distribuídos com o pacote completo.
