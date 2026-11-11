package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** As requisições abrem e concluem suas próprias transações PostgreSQL. */
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class PessoasAtomicidadeHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSource;
    @Test
    void falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo() throws Exception {
        Cenario c=criarCenarioPersistido(); String cpf=cpf(760000000+c.pessoaA.getId().intValue());
        Map<String,Object> req=cadastro(c,false,cpf); req.put("matricula","X".repeat(31));
        executar(c.localA,post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON).content(json(req)),400,
                "VIN001 validação tardia de matrícula deve fazer rollback da Pessoa já inserida");
        assertThat(contar("select count(p) from Pessoa p where p.cpf=:cpf",cpf)).isZero();
        assertThat(contar("select count(e) from Empregado e where e.cpf=:cpf",cpf)).isZero();
        assertThat(emTransacao(()->em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id",Long.class)
                .setParameter("id",c.contratoA.getId()).getSingleResult())).isEqualTo(1);
        req.put("matricula","APOS-ROLLBACK");
        JsonNode criado=dados(executar(c.localA,post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON).content(json(req)),201,
                "VIN001 o CPF da tentativa revertida continua utilizável"));
        long id=criado.path("id").asLong(),pessoaId=criado.path("pessoaId").asLong();
        assertThat(contar("select count(p) from Pessoa p where p.cpf=:cpf",cpf)).isEqualTo(1);
        assertThat(emTransacao(()->em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:id",Long.class)
                .setParameter("id",id).getSingleResult())).isEqualTo(1);
        assertThat(emTransacao(()->em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:id",Long.class)
                .setParameter("id",pessoaId).getSingleResult())).isEqualTo(1);
        assertThat(emTransacao(()->em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id",Long.class)
                .setParameter("id",id).getSingleResult())).isEqualTo(1);
    }

    @Test
    void cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa() throws Exception {
        Cenario c=criarCenarioPersistido(); String cpf=cpf(770000000+c.pessoaA.getId().intValue());
        Map<String,Object> a=cadastro(c,false,cpf), b=cadastro(c,true,cpf);
        List<Integer> status=simultaneos(c,a,b);
        assertThat(status).containsExactlyInAnyOrder(201,409);
        assertThat(contar("select count(p) from Pessoa p where p.cpf=:cpf",cpf)).isEqualTo(1);
        assertThat(contar("select count(v) from VinculoContratual v where v.pessoa.cpf=:cpf",cpf)).isEqualTo(1);
        assertThat(contar("select count(a) from AlocacaoVinculo a where a.vinculo.pessoa.cpf=:cpf",cpf)).isEqualTo(1);
    }

    @Test
    void vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos() throws Exception {
        Cenario c=criarCenarioPersistido(); String cpf=cpf(780000000+c.pessoaA.getId().intValue());
        Long pessoaId=emTransacao(()->pessoa("Pessoa sem vínculo para concorrência",cpf).getId());
        Map<String,Object> a=cadastro(c,false,cpf),b=cadastro(c,true,cpf);
        a.remove("pessoa"); b.remove("pessoa"); a.put("pessoaId",pessoaId); b.put("pessoaId",pessoaId);
        assertThat(simultaneos(c,a,b)).containsExactlyInAnyOrder(201,409);
        assertThat(contar("select count(v) from VinculoContratual v where v.pessoa.cpf=:cpf",cpf)).isEqualTo(1);
        assertThat(contar("select count(a) from AlocacaoVinculo a where a.vinculo.pessoa.cpf=:cpf",cpf)).isEqualTo(1);
    }

    @Test
    @Timeout(value=45,unit=TimeUnit.SECONDS)
    void frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia() throws Exception {
        Cenario c=criarCenarioPersistido();
        Map<String,Object> req=new LinkedHashMap<>(Map.of("novoInicio",INICIO.plusDays(1),
                "justificativa","Início documentado", "evidencia","SEI-CONCORRENCIA", "autorizacao","Ato de retificação"));
        JsonNode previa=dados(executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/previas/retificacao-inicio")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),200,
                "VIN014 prévia anterior ao impacto concorrente"));
        req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        String body=json(req);
        CompletableFuture<Long> bloqueador=new CompletableFuture<>();
        CountDownLatch inserirFrequencia=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<Long> gravacao=pool.submit(()->emTransacao(()->{
                em.find(Contrato.class,c.contratoA.getId(),LockModeType.PESSIMISTIC_WRITE);
                bloqueador.complete(((Number)em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if (!inserirFrequencia.await(30,TimeUnit.SECONDS)) throw new AssertionError("Confirmação não chegou ao lock do Contrato.");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                Frequencia nova=frequencia(c,false,INICIO,"FALTA"); em.flush(); return nova.getId();
            }));
            long conexaoBloqueadora=bloqueador.get(10,TimeUnit.SECONDS);
            Future<MvcResult> confirmacao=pool.submit(()->executar(c.localA,
                    post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/retificar-inicio")
                            .contentType(MediaType.APPLICATION_JSON).content(body),409,
                    "VIN014 confirmação deve reler dependências commitadas enquanto aguardava o lock"));
            aguardarEsperaNoBloqueador(conexaoBloqueadora,confirmacao);
            inserirFrequencia.countDown();
            Long frequenciaId=gravacao.get(20,TimeUnit.SECONDS);
            confirmacao.get(20,TimeUnit.SECONDS);
            emTransacao(()->{
                assertThat(em.find(VinculoContratual.class,c.vinculoA.getId()).getDataInicio()).isEqualTo(INICIO);
                assertThat(em.find(AlocacaoVinculo.class,c.alocacaoA.getId()).getDataInicio()).isEqualTo(INICIO);
                assertThat(em.find(Frequencia.class,frequenciaId).getStatus()).isEqualTo("FALTA");
                assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id",Long.class)
                        .setParameter("id",c.vinculoA.getId()).getSingleResult()).isZero();
                return null;
            });
        } finally { inserirFrequencia.countDown(); pool.shutdownNow(); }
    }

    @Test
    @Timeout(value=45,unit=TimeUnit.SECONDS)
    void atualizacaoOpcionalCanonicaAguardaPessoaAntesDeTravarEmpregado() throws Exception {
        Cenario c=criarCenarioPersistido();
        emTransacao(()->{ em.find(Pessoa.class,c.pessoaA.getId()).setCtpsSerie("CTPS ORIGINAL"); return null; });
        var corpo=(com.fasterxml.jackson.databind.node.ObjectNode)dados(executar(c.localA,
                get("/api/pessoas/"+c.pessoaA.getId()),200,"Preparar atualização opcional concorrente"));
        corpo.put("rg","RG-CONCORRENTE"); corpo.put("email","concorrente@example.test");
        String body=json(corpo);
        CompletableFuture<Long> bloqueador=new CompletableFuture<>();
        CountDownLatch adquirirEmpregado=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<Long> escritorCanonico=pool.submit(()->emTransacao(()->{
                em.find(Pessoa.class,c.pessoaA.getId(),LockModeType.PESSIMISTIC_WRITE);
                bloqueador.complete(((Number)em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if(!adquirirEmpregado.await(30,TimeUnit.SECONDS))throw new AssertionError("PUT canônico não aguardou a Pessoa bloqueada.");
                } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                return em.find(Empregado.class,c.empregadoA.getId(),LockModeType.PESSIMISTIC_WRITE).getId();
            }));
            long conexao=bloqueador.get(10,TimeUnit.SECONDS);
            Future<MvcResult> opcional=pool.submit(()->executar(c.localA,put("/api/pessoas/"+c.pessoaA.getId()+"/dados-opcionais")
                    .contentType(MediaType.APPLICATION_JSON).content(body),200,
                    "VIN001 PUT opcional deve respeitar Pessoa antes de Empregado sem deadlock"));
            aguardarEsperaNoBloqueador(conexao,opcional);
            adquirirEmpregado.countDown();
            Throwable falhaCanonica=null,falhaOpcional=null;
            try{escritorCanonico.get(20,TimeUnit.SECONDS);}catch(ExecutionException e){falhaCanonica=e.getCause();}
            try{opcional.get(20,TimeUnit.SECONDS);}catch(ExecutionException e){falhaOpcional=e.getCause();}
            assertThat(falhaCanonica).as("Escritor na ordem Pessoa→Empregado deve concluir sem deadlock").isNull();
            assertThat(falhaOpcional).as("PUT canônico aguardando Pessoa deve concluir depois do commit canônico").isNull();
            emTransacao(()->{
                Pessoa atual=em.find(Pessoa.class,c.pessoaA.getId());
                assertThat(atual.getRg()).isEqualTo("RG-CONCORRENTE");
                assertThat(atual.getEmail()).isEqualTo("concorrente@example.test");
                assertThat(atual.getCtpsSerie()).isEqualTo("CTPS ORIGINAL");
                assertThat(em.find(Empregado.class,c.empregadoA.getId()).getPessoaCanonica().getId()).isEqualTo(atual.getId());
                return null;
            });
        } finally{adquirirEmpregado.countDown();pool.shutdownNow();}
    }

    /** O passo concorrente só é liberado após o PostgreSQL confirmar uma espera pela transação A. */
    private void aguardarEsperaNoBloqueador(long conexaoBloqueadora,Future<MvcResult> confirmacao) throws Exception {
        long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime()<limite) {
            long esperas=emTransacao(()->((Number)em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao",conexaoBloqueadora).getSingleResult()).longValue());
            if (esperas>0) return;
            if (confirmacao.isDone()) {
                MvcResult r=confirmacao.get();
                throw new AssertionError("Confirmação terminou antes de aguardar o lock: "+r.getResponse().getStatus());
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        throw new AssertionError("PostgreSQL não registrou a confirmação aguardando a transação bloqueadora "+conexaoBloqueadora);
    }

    private List<Integer> simultaneos(Cenario c,Map<String,Object> a,Map<String,Object> b) throws Exception {
        executar(c.localA,get("/api/vinculos-contratuais/"+c.vinculoA.getId()),200,"Preparar sessão A para concorrência");
        executar(c.localB,get("/api/vinculos-contratuais/"+c.vinculoB.getId()),200,"Preparar sessão B para concorrência");
        var reqA=autenticar(post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON).content(json(a)),c.localA);
        var reqB=autenticar(post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON).content(json(b)),c.localB);
        // No cadastro novo, uma Pessoa sintética não confirmada força os dois INSERTs
        // a disputar o mesmo CPF no índice único; ela será revertida antes dos cadastros.
        // No vínculo de Pessoa existente, os dois comandos disputam sua PK canônica.
        boolean cadastroNovo = !a.containsKey("pessoaId");
        Long pessoaId = cadastroNovo ? null : ((Number) a.get("pessoaId")).longValue();
        String cpf = cadastroNovo ? (String) ((Map<?,?>) a.get("pessoa")).get("cpf") : null;
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        if (cadastroNovo) assertThat(EsperaJdbcPostgres.indiceUnico(jdbc, "pessoa", "uk_pessoa_cpf", "cpf"))
                .as("COL005 controle positivo: índice único integral, válido e pronto para o CPF sintético").isTrue();
        CountDownLatch largada=new CountDownLatch(1), liberar=new CountDownLatch(1);
        CompletableFuture<Long> raizPronta=new CompletableFuture<>();
        ExecutorService pool=Executors.newFixedThreadPool(3);
        var atorA=new Ator("cadastro-A-"+cadastroNovo); var atorB=new Ator("cadastro-B-"+cadastroNovo);
        try {
            Future<?> bloqueador=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(tx->{
                if(cadastroNovo) { pessoa("Pessoa exclusivamente sintética e revertida da barreira",cpf); em.flush(); }
                else em.find(Pessoa.class,pessoaId,LockModeType.PESSIMISTIC_WRITE);
                raizPronta.complete(((Number)em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if(!liberar.await(25,TimeUnit.SECONDS)) throw new AssertionError("COL005/VIN003 fixture não liberou Pessoa");
                } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                if(cadastroNovo)tx.setRollbackOnly();
                return null;
            }));
            long raiz=raizPronta.get(10,TimeUnit.SECONDS);
            Future<MvcResult> fa=pool.submit(()->atorA.executar(()->{largada.await();return mvc.perform(reqA).andReturn();}));
            Future<MvcResult> fb=pool.submit(()->atorB.executar(()->{largada.await();return mvc.perform(reqB).andReturn();}));
            largada.countDown();
            java.util.function.Predicate<com.example.siggtrat.infra.ComandoJdbcAtor> alvo=cadastroNovo
                    ? sql->sql.insercao("pessoa","cpf",cpf)
                    : sql->sql.selecaoChave("pessoa","id_pessoa",pessoaId);
            var esperas=EsperaJdbcPostgres.aguardarAmbos(jdbc,raiz,atorA,alvo,atorB,alvo,
                    cadastroNovo?"COL005 INSERT Pessoa/CPF sintético único":"VIN003 Pessoa/"+pessoaId,fa,fb);
            assertThat(esperas).isNotEmpty(); assertThat(fa.isDone()).isFalse(); assertThat(fb.isDone()).isFalse();
            liberar.countDown(); bloqueador.get(10,TimeUnit.SECONDS);
            MvcResult ra=fa.get(30,TimeUnit.SECONDS),rb=fb.get(30,TimeUnit.SECONDS);
            List<Integer> resultado=List.of(ra.getResponse().getStatus(),rb.getResponse().getStatus());
            assertThat(resultado).as("A=%s; B=%s",ra.getResponse().getContentAsString(),rb.getResponse().getContentAsString()).allMatch(status->status==201 || status==409);
            MvcResult vencedora=ra.getResponse().getStatus()==201?ra:rb;
            JsonNode recibo=dados(vencedora);
            long vinculoConfirmado=recibo.path("id").asLong(), pessoaConfirmada=recibo.path("pessoaId").asLong();
            assertThat(vinculoConfirmado).isPositive(); assertThat(pessoaConfirmada).isPositive();
            if(!cadastroNovo)assertThat(pessoaConfirmada).isEqualTo(pessoaId);
            emTransacao(()->{
                var v=em.find(VinculoContratual.class,vinculoConfirmado);
                assertThat(v).isNotNull(); assertThat(v.getPessoa().getId()).isEqualTo(pessoaConfirmada);
                assertThat(v.getContrato().getId()).isIn(c.contratoA.getId(),c.contratoB.getId());
                assertThat(em.createQuery("select v.id from VinculoContratual v where v.pessoa.id=:id",Long.class)
                        .setParameter("id",pessoaConfirmada).getResultList()).containsExactly(vinculoConfirmado);
                assertThat(em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id",AlocacaoVinculo.class)
                        .setParameter("id",vinculoConfirmado).getResultList()).singleElement()
                        .satisfies(alocacao->assertThat(alocacao.getVinculo().getId()).isEqualTo(vinculoConfirmado));
                return null;
            });
            return resultado;
        } finally { largada.countDown(); liberar.countDown(); pool.shutdownNow(); }
    }

    private Map<String,Object> cadastro(Cenario c,boolean ladoB,String cpf) {
        return new LinkedHashMap<>(Map.of("pessoa",Map.of("nome","Pessoa de cadastro concorrente","cpf",cpf),
                "contratoId",ladoB?c.contratoB.getId():c.contratoA.getId(),"dataInicio",DATA,
                "composicaoId",ladoB?c.composicaoB.getId():c.composicaoA.getId()));
    }
    private long contar(String jpql,String cpf) { return emTransacao(()->em.createQuery(jpql,Long.class).setParameter("cpf",cpf).getSingleResult()); }
}
