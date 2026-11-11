package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Critérios CAT/COL/MAT/VIN/ALO/DES/HIS exercidos pela API completa. */
class PessoasVinculosContratoHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";
    private static final String P = "/api/pessoas";

    @Test
    void cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita() throws Exception {
        Cenario c = criarCenario();
        String cpf = cpf(710000000 + c.pessoaA.getId().intValue());
        String pontuado = cpf.substring(0,3)+"."+cpf.substring(3,6)+"."+cpf.substring(6,9)+"-"+cpf.substring(9);
        Map<String,Object> pessoa = new LinkedHashMap<>(Map.of("nome", "  Pessoa  Completa  ", "cpf", pontuado,
                "rg", "RG-123", "ctpsSerie", "CTPS-9", "nitPisPasep", "PIS-456",
                "dataNascimento", "1990-03-04", "email", "cadastro@example.test"));
        pessoa.put("endereco", Map.of("logradouro", "Rua de Teste", "numero", "10", "bairro", "Centro",
                "cidade", "Cidade", "uf", "SP", "cep", "12345678"));
        Map<String,Object> req = cadastro(c, DATA, null, "M-COMPLETO"); req.put("pessoa", pessoa);
        JsonNode vinculo = dados(executar(c.localA, post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)), 201,
                "COL001-005 VIN001 cadastro integrado"));
        long pessoaId = vinculo.path("pessoaId").asLong(), vinculoId = vinculo.path("id").asLong();
        assertThat(vinculo.path("pessoaNome").asText()).isEqualTo("Pessoa Completa");
        assertThat(vinculo.path("pessoaCpf").asText()).isEqualTo(cpf);
        assertThat(vinculo.path("dataInicio").asText()).isEqualTo(DATA.toString());
        assertThat(vinculo.path("composicaoInicialId").asLong()).isEqualTo(c.composicaoA.getId());
        assertThat(vinculo.path("matricula").asText()).isEqualTo("M-COMPLETO");
        JsonNode completa = dados(executar(c.localA, get(P+"/"+pessoaId),200,"Pessoa completa no escopo local"));
        assertThat(completa.path("rg").asText()).isEqualTo("RG-123");
        assertThat(completa.path("endereco").path("logradouro").asText()).isEqualTo("RUA DE TESTE");
        assertThat(completa.has("conta")).isFalse(); assertThat(completa.has("banco")).isFalse();
        JsonNode mantida = dados(executar(c.localA, put(P+"/"+pessoaId+"/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content("{}"),200,"COL011 omissão mantém os dados"));
        assertThat(mantida.path("rg").asText()).isEqualTo("RG-123");
        assertThat(mantida.path("email").asText()).isEqualTo("cadastro@example.test");
        assertThat(mantida.path("endereco")).isEqualTo(completa.path("endereco"));
        for (String campo : List.of("rg", "ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco"))
            assertThat(mantida.path(campo)).as("COL-011:002[1] omissão preserva %s", campo).isEqualTo(completa.path(campo));
        JsonNode limpa = dados(executar(c.localA, put(P+"/"+pessoaId+"/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("limparRg",true,"limparEmail",true,"limparEndereco",true))),
                200,"COL003 COL011 limpeza expressa"));
        assertThat(limpa.path("rg").isNull()).isTrue(); assertThat(limpa.path("email").isNull()).isTrue();
        assertThat(limpa.path("endereco").isNull()).isTrue(); assertThat(limpa.path("ctpsSerie").asText()).isEqualTo("CTPS-9");
        for (String campo : List.of("ctpsSerie", "nitPisPasep", "dataNascimento"))
            assertThat(limpa.path(campo)).as("COL-011:002[1] limpar RG/email/endereço preserva %s", campo)
                    .isEqualTo(completa.path(campo));
        em.flush(); em.clear();
        JsonNode relida = dados(executar(c.localA, get(P+"/"+pessoaId), 200, "COL011 releitura pelo pessoaId explícito"));
        for (String campo : List.of("rg", "ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco"))
            assertThat(relida.path(campo)).as("COL011 persistência após limpeza: %s", campo).isEqualTo(limpa.path(campo));
        JsonNode alocacoes = dados(executar(c.localA,get(V+"/"+vinculoId+"/alocacoes"),200,"VIN001 primeira alocação persistida"));
        assertThat(alocacoes).hasSize(1); assertThat(alocacoes.get(0).path("dataInicio").asText()).isEqualTo(DATA.toString());
        assertThat(em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:p",Long.class)
                .setParameter("p",pessoaId).getSingleResult()).isEqualTo(1);
        executar(c.localB,put(P+"/"+pessoaId+"/dados-opcionais").contentType(MediaType.APPLICATION_JSON).content("{}"),404,"Opcionais negados fora da Gestão");
        executar(c.nacional,put(P+"/"+pessoaId+"/dados-opcionais").contentType(MediaType.APPLICATION_JSON).content("{}"),403,"Consulta nacional não autoriza alteração pessoal");
        executar(c.localA,put(P+"/"+pessoaId+"/dados-opcionais").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("endereco",Map.of("logradouro","")))),400,"Endereço informado exige logradouro");
    }

    @Test
    void leiturasDePessoaEVinculoRespeitamGestaoEstadoEMinimizacao() throws Exception {
        Cenario c=criarCenario();
        for (UsuarioAplicacao usuario : List.of(c.localA,c.estadualA,c.nacional,c.admin,c.operacionalA)) {
            JsonNode pessoas=dados(executar(usuario,get(P).param("contratoId",c.contratoA.getId().toString()),200,"Listar Pessoas no escopo "+usuario.getPerfil()));
            assertThat(ids(pessoas.path("content"))).contains(c.pessoaA.getId()).doesNotContain(c.pessoaB.getId());
            JsonNode p=dados(executar(usuario,get(P+"/"+c.pessoaA.getId()),200,"Detalhe Pessoa "+usuario.getPerfil()));
            assertThat(p.path("id").asLong()).isEqualTo(c.pessoaA.getId());
            if (usuario==c.operacionalA || usuario==c.nacional || usuario==c.estadualA || usuario==c.admin)
                assertThat(p.path("cpf").asText()).isNotEqualTo(c.pessoaA.getCpf());
            JsonNode pagina=dados(executar(usuario,get(V).param("contratoId",c.contratoA.getId().toString()).param("ativoEm",DATA.toString()),200,"Vínculos filtrados por contrato "+usuario.getPerfil()));
            assertThat(ids(pagina.path("content"))).contains(c.vinculoA.getId()).doesNotContain(c.vinculoB.getId());
            JsonNode v=dados(executar(usuario,get(V+"/"+c.vinculoA.getId()),200,"Detalhe Vínculo "+usuario.getPerfil()));
            assertThat(v.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId());
            for(String sufixo:List.of("/eventos","/alocacoes","/alocacoes/eventos"))
                assertThat(dados(executar(usuario,get(V+"/"+c.vinculoA.getId()+sufixo),200,"Linha histórica "+sufixo+" "+usuario.getPerfil())).isArray()).isTrue();
            JsonNode pos=dados(executar(usuario,get(V+"/pessoas/"+c.pessoaA.getId()+"/posicao").param("data",DATA.toString()),200,"Posição temporal Pessoa "+usuario.getPerfil()));
            assertThat(pos.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId()); assertThat(pos.path("vinculos")).hasSize(1);
            JsonNode alo=dados(executar(usuario,get(V+"/"+c.vinculoA.getId()+"/alocacoes/posicao").param("data",DATA.toString()),200,"Posição temporal Alocação "+usuario.getPerfil()));
            assertThat(alo.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
            JsonNode op=dados(executar(usuario,get(V+"/contratos/"+c.contratoA.getId()+"/opcoes-alocacao").param("data",DATA.toString())
                    .param("lotacaoId",c.lotacaoA.getId().toString()).param("jornadaId",c.jornadaA.getId().toString()),200,"Seleção encadeada "+usuario.getPerfil()));
            assertThat(op).hasSize(1); assertThat(op.get(0).path("cargoId").asLong()).isEqualTo(c.cargoA.getId());
        }
        for(UsuarioAplicacao fora:List.of(c.localB,c.estadualB,c.operacionalB)) {
            for(String rota:List.of(P+"/"+c.pessoaA.getId(),P+"/"+c.pessoaA.getId()+"/correcoes-identidade",V+"/"+c.vinculoA.getId(),
                    V+"/"+c.vinculoA.getId()+"/eventos",V+"/"+c.vinculoA.getId()+"/alocacoes",V+"/"+c.vinculoA.getId()+"/alocacoes/eventos"))
                executar(fora,get(rota),404,"Leitura fora do escopo "+fora.getPerfil()+" "+rota);
            executar(fora,get(V+"/pessoas/"+c.pessoaA.getId()+"/posicao").param("data",DATA.toString()),404,"Posição Pessoa fora do escopo");
            executar(fora,get(V+"/"+c.vinculoA.getId()+"/alocacoes/posicao").param("data",DATA.toString()),404,"Posição Alocação fora do escopo");
            executar(fora,get(V+"/contratos/"+c.contratoA.getId()+"/opcoes-alocacao").param("data",DATA.toString()),404,"Opções fora do escopo");
        }
        JsonNode restrita=dados(executar(c.localA,get(P+"/"+c.pessoaA.getId()+"/dados-restritos").param("finalidade","Conferência cadastral"),200,"Pessoa integral para Gestor responsável"));
        assertThat(restrita.path("cpf").asText()).isEqualTo(c.pessoaA.getCpf());
        executar(c.operacionalA,get(P+"/"+c.pessoaA.getId()+"/dados-restritos").param("finalidade","Consulta"),403,"Operacional não recebe cadastro integral");
        executar(c.localB,get(P+"/"+c.pessoaA.getId()+"/dados-restritos").param("finalidade","Consulta"),404,"Cadastro integral também é escopado");
        executar(c.localA,get(V).param("inicioDe","2026-06-11").param("inicioAte","2026-06-10"),400,"Intervalo de início invertido");
        executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/alocacoes").param("mes","2026-99"),400,"Mês inválido rejeitado");
    }

    @Test
    void correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao() throws Exception {
        Cenario c=criarCenario(); JsonNode v=cadastrar(c,DATA,null,"MAT-IDENTIDADE"); long id=v.path("pessoaId").asLong(), vid=v.path("id").asLong();
        String anterior=v.path("pessoaNome").asText();
        Map<String,Object> correcao=new LinkedHashMap<>(Map.of("tipo","NOME","novoValor","Nome legal corrigido","motivo","Mudança legal",
                "evidencia","Documento SEI-123","autorizacao","Ato 9","impactosEncontrados","Vínculo "+vid+" e projeção cadastral"));
        JsonNode nova=dados(executar(c.localA,post(P+"/"+id+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON).content(json(correcao)),200,"COL009 correção excepcional"));
        assertThat(nova.path("id").asLong()).isEqualTo(id); assertThat(nova.path("nome").asText()).isEqualTo("Nome legal corrigido");
        JsonNode trilha=dados(executar(c.localA,get(P+"/"+id+"/correcoes-identidade"),200,"Correção contém valores e autoria"));
        assertThat(trilha).hasSize(1); JsonNode e=trilha.get(0);
        assertThat(e.path("valorAnterior").asText()).isEqualTo(anterior); assertThat(e.path("valorNovo").asText()).isEqualTo("Nome legal corrigido");
        assertThat(e.path("responsavelId").asLong()).isEqualTo(c.localA.getId()); assertThat(e.path("registradoEm").asText()).isNotBlank();
        assertThat(e.path("evidencia").asText()).isEqualTo("Documento SEI-123"); assertThat(e.path("impactosEncontrados").asText()).contains(String.valueOf(vid));
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+vid+"/eventos"),200,"COL010 HIS004 fotografia preservada"));
        assertThat(eventos.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(anterior);
        correcao.put("tipo","CPF"); correcao.put("novoValor",c.pessoaB.getCpf());
        executar(c.localA,post(P+"/"+id+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON).content(json(correcao)),409,"COL007 CPF de outra Pessoa nunca funde identidade");
        executar(c.localA,post(P+"/"+id+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Correção exige evidência e autorização");
        executar(c.localB,post(P+"/"+id+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON).content(json(correcao)),404,"Correção fora da Gestão");
        executar(c.nacional,post(P+"/"+id+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON).content(json(correcao)),403,"Correção reservada ao Gestor Local");
    }

    @Test
    void vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-RECONTRATACAO-CONSECUTIVA");
        MotivoDesligamento motivo=motivo(c.gestaoA,CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Desligamento individual para recontratação consecutiva", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA, DATA, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-VINCULOS-CONSECUTIVOS");
        Map<String,Object> req=cadastro(c,DATA,null,null); req.put("pessoaId",c.pessoaB.getId());
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"VIN003 sobreposição entre Gestões");
        req.put("pessoaId",c.pessoaA.getId());
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"VIN003 sobreposição no mesmo contrato");
        desligar(c,c.vinculoA.getId(),DATA,motivo.getId());
        Map<String,Object> proximo=cadastro(c,DATA.plusDays(1),null,null); proximo.put("pessoaId",c.pessoaA.getId());
        proximo.put("contratoId",c.contratoB.getId()); proximo.put("composicaoId",c.composicaoB.getId());
        JsonNode novo=dados(executar(c.localB,post(V).contentType(MediaType.APPLICATION_JSON).content(json(proximo)),201,"VIN004 COL013 troca de contrato no dia consecutivo"));
        assertThat(novo.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId()); assertThat(novo.path("id").asLong()).isNotEqualTo(c.vinculoA.getId());
        assertThat(novo.path("matricula").isNull()).isTrue();
        assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf",Long.class).setParameter("cpf",c.pessoaA.getCpf()).getSingleResult()).isEqualTo(1);
        JsonNode pos=dados(executar(c.nacional,get(V+"/pessoas/"+c.pessoaA.getId()+"/posicao").param("data",DATA.plusDays(1).toString()),200,"HIS002 história dos dois contratos"));
        assertThat(pos.path("vinculos")).hasSize(2);
        List<List<Long>> identidadesHistoricas = new ArrayList<>();
        for (JsonNode historico : pos.path("vinculos")) {
            assertThat(historico.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId());
            identidadesHistoricas.add(List.of(historico.path("vinculoId").asLong(), historico.path("contratoId").asLong()));
        }
        assertThat(identidadesHistoricas).as("HIS002 mesma Pessoa conserva ambos os Vínculos e Contratos")
                .containsExactlyInAnyOrder(List.of(c.vinculoA.getId(), c.contratoA.getId()),
                        List.of(novo.path("id").asLong(), c.contratoB.getId()));
        Map<String,Object> duplicada=cadastro(c,DATA,null,null); duplicada.put("pessoa",Map.of("nome","Outra identidade","cpf",c.pessoaA.getCpf()));
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(duplicada)),409,"COL005 CPF global duplicado");
        Map<String,Object> invalida=cadastro(c,DATA,null,null); invalida.put("pessoa",Map.of("nome","Nome","cpf","11111111111"));
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(invalida)),400,"COL001 COL005 CPF inválido");
        invalida.put("pessoa",Map.of("cpf",cpf(720000000+c.pessoaA.getId().intValue())));
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(invalida)),400,"COL001 nome obrigatório");
    }

    @Test
    void matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento() throws Exception {
        Cenario c=criarCenario(); JsonNode novo=cadastrar(c,INICIO,null,"MAT-ORIGINAL"); long id=novo.path("id").asLong();
        Map<String,Object> req=new LinkedHashMap<>(acao()); req.put("dataEfetiva",DATA); req.put("novaMatricula","MAT-NOVA");
        JsonNode alterado=dados(executar(c.localA,post(V+"/"+id+"/alterar-matricula").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"MAT001 alteração pertence ao vínculo"));
        assertThat(alterado.path("matricula").asText()).isEqualTo("MAT-NOVA");
        JsonNode antes=dados(executar(c.localA,get(V+"/pessoas/"+novo.path("pessoaId").asLong()+"/posicao").param("data",DATA.minusDays(1).toString()),200,"Matrícula histórica anterior"));
        assertThat(antes.path("vinculos").get(0).path("matricula").asText()).isEqualTo("MAT-ORIGINAL");
        MotivoDesligamento motivo=motivo(c.gestaoA,CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Desligamento individual com reserva de matrícula", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA.plusDays(1), DATA.plusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-RESERVA-MATRICULA");
        desligar(c,id,DATA.plusDays(1),motivo.getId());
        Map<String,Object> segunda=cadastro(c,DATA.plusDays(2),null,"MAT-ORIGINAL"); segunda.put("pessoaId",novo.path("pessoaId").asLong());
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(segunda)),409,"MAT003 MAT004 nem a própria Pessoa reutiliza matrícula antiga");
        segunda.put("matricula","MAT-NOVA"); executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(segunda)),409,"Matrícula alterada também fica reservada");
        em.flush(); em.clear();
        List<List<Object>> reservas = em.createQuery("select r.contrato.id, r.vinculo.id, r.matricula from ReservaMatriculaContrato r "
                        + "where r.vinculo.id=:v order by r.matricula", Object[].class)
                .setParameter("v", id).getResultList().stream().map(Arrays::asList).toList();
        assertThat(reservas).as("MAT-001:001[1] reservas persistidas vinculadas ao contrato e vínculo explícitos")
                .containsExactlyInAnyOrder(List.<Object>of(c.contratoA.getId(), id, "MAT-ORIGINAL"),
                        List.<Object>of(c.contratoA.getId(), id, "MAT-NOVA"));
        executar(c.localB,post(V+"/"+id+"/alterar-matricula").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Matrícula fora da Gestão");
        executar(c.operacionalA,post(V+"/"+id+"/alterar-matricula").contentType(MediaType.APPLICATION_JSON).content(json(req)),403,"Operacional não altera matrícula");
        req.put("dataEfetiva",RelogioSistema.hoje().plusDays(1)); req.put("novaMatricula","MAT-FUTURA");
        executar(c.localA,post(V+"/"+id+"/alterar-matricula").contentType(MediaType.APPLICATION_JSON).content(json(req)),400,"Matrícula futura rejeitada");
    }

    @Test
    void tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia() throws Exception {
        Cenario c=criarCenario(); c.tipoA.setMatriculaVinculoObrigatoria(true);
        // A obrigação pertence à política conhecida na publicação. Alterar o
        // Tipo mestre depois de publicar outro contrato não retroage sobre ele.
        c.contratoA=contrato(c.gestaoA,c.tipoA,"MAT-OBR-"+c.gestaoA.getId());
        c.composicaoA=composicao(c.contratoA,c.lotacaoA,c.jornadaA,c.cargoA,5);
        Map<String,Object> sem=cadastro(c,RelogioSistema.hoje().plusDays(10),null,null);
        sem.put("pessoa",Map.of("nome","Pessoa sem matrícula","cpf",cpf(730000000+c.pessoaA.getId().intValue())));
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(sem)),400,"MAT005 Tipo exige matrícula");
        sem.put("pessoa",Map.of("nome","Pessoa com matrícula","cpf",cpf(735000000+c.pessoaA.getId().intValue())));
        sem.put("matricula","MAT-PROGRAMADA");
        JsonNode futuro=dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(sem)),201,"VIN005 vínculo futuro programado"));
        long id=futuro.path("id").asLong(); assertThat(futuro.path("situacaoTemporal").asText()).isEqualTo("PROGRAMADO");
        JsonNode cancelado=dados(executar(c.localA,post(V+"/"+id+"/cancelar-programacao").contentType(MediaType.APPLICATION_JSON).content(json(acao())),200,"VIN006 cancelar sem desligar"));
        assertThat(cancelado.path("estadoNegocial").asText()).isEqualTo("CANCELADO"); assertThat(cancelado.path("motivoDesligamentoId").isNull()).isTrue();
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+id+"/eventos"),200,"VIN007 origem e cancelamento encadeados"));
        assertThat(eventos).hasSize(2); assertThat(eventos.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventos.get(0).path("id").asLong());
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("CANCELAMENTO_PROGRAMACAO");
        Map<String,Object> reutilizar=cadastro(c,RelogioSistema.hoje().plusDays(10),null,"MAT-PROGRAMADA"); reutilizar.put("pessoaId",futuro.path("pessoaId").asLong());
        executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(reutilizar)),409,"MAT003 reserva sobrevive à programação cancelada");
        reutilizar.put("matricula","MAT-REPROGRAMADA");
        JsonNode reprogramado=dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(reutilizar)),201,"Programação vigente usa Pessoa anterior e matrícula nova"));
        assertThat(reprogramado.path("pessoaId").asLong()).isEqualTo(futuro.path("pessoaId").asLong());
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/cancelar-programacao").contentType(MediaType.APPLICATION_JSON).content(json(acao())),409,"Vínculo já iniciado não é programação futura");
        executar(c.localB,post(V+"/"+id+"/cancelar-programacao").contentType(MediaType.APPLICATION_JSON).content(json(acao())),404,"Cancelamento fora da Gestão");
    }

    @Test
    void retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-RETIFICACAO-INICIO");
        Map<String,Object> req=new LinkedHashMap<>(acao()); req.put("novoInicio",INICIO.plusDays(1));
        JsonNode previa=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"VIN014 prévia do início"));
        assertThat(previa.path("token").asText()).hasSize(64); assertThat(previa.path("valorAtual").asText()).isEqualTo(INICIO.toString());
        assertThat(previa.path("impactos").size()).isGreaterThanOrEqualTo(5);
        req.put("confirmarImpacto",true); req.put("tokenPrevia",previa.path("token").asText());
        List<String> estadoAntes = estadoRetificacaoB07(c.vinculoA.getId());
        JsonNode eventosAntes = dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),
                200,"VIN014 fotografia antes das recusas isoladas"));
        for (String campo : List.of("justificativa", "evidencia", "autorizacao")) {
            for (boolean omitir : List.of(true, false)) {
                Map<String,Object> incompleta = new LinkedHashMap<>(req);
                if (omitir) incompleta.remove(campo); else incompleta.put(campo, "   ");
                // Novo início e token da prévia válida continuam exatamente os mesmos.
                assertThat(incompleta.get("novoInicio")).isEqualTo(req.get("novoInicio"));
                assertThat(incompleta.get("tokenPrevia")).isEqualTo(req.get("tokenPrevia"));
                for (String outro : List.of("justificativa", "evidencia", "autorizacao"))
                    if (!outro.equals(campo)) assertThat(incompleta.get(outro)).isEqualTo(req.get(outro));
                var negada = executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/retificar-inicio")
                        .contentType(MediaType.APPLICATION_JSON).content(json(incompleta)),400,
                        "VIN014 metadado isolado " + campo + (omitir ? " omitido" : " vazio"));
                assertThat(mapper.readTree(negada.getResponse().getContentAsString()).path("message").asText())
                        .containsIgnoringCase("justificativa").containsIgnoringCase("evidência").containsIgnoringCase("autorização");
                assertThat(estadoRetificacaoB07(c.vinculoA.getId())).as("Recusa de " + campo + " não grava limites, versões ou eventos")
                        .isEqualTo(estadoAntes);
                assertThat(dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),200,
                        "VIN014 trilha preservada após recusa de " + campo))).isEqualTo(eventosAntes);
            }
        }
        JsonNode retificado=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"VIN014 retificação auditada"));
        assertThat(retificado.path("id").asLong()).isEqualTo(c.vinculoA.getId()); assertThat(retificado.path("dataInicio").asText()).isEqualTo(INICIO.plusDays(1).toString());
        JsonNode alocacoes=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/alocacoes"),200,"Cobertura acompanha início retificado"));
        assertThat(alocacoes.get(0).path("dataInicio").asText()).isEqualTo(INICIO.plusDays(1).toString());
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"Prévia consumida não reaplicada ao novo estado");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Prévia fora da Gestão");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Retificação fora da Gestão");
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-inicio").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Prévia sem novo início");
    }

    @Test
    void desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-DESLIGAMENTO-RETROATIVO");
        MotivoDesligamento motivo=motivo(c.gestaoA,CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Desligamento individual com retificação do último dia", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA, DATA.plusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-RETIFICACAO-ULTIMO-DIA");
        JsonNode fim=desligar(c,c.vinculoA.getId(),DATA,motivo.getId());
        assertThat(fim.path("dataFim").asText()).isEqualTo(DATA.toString()); assertThat(fim.path("motivoDesligamentoId").asLong()).isEqualTo(motivo.getId());
        assertThat(fim.path("desligamentoRegistradoEm").asText()).isNotBlank(); assertThat(c.contratoA.getDataFim()).isEqualTo(FIM);
        JsonNode ultimo=posicao(c,c.vinculoA.getPessoa().getId(),DATA), depois=posicao(c,c.vinculoA.getPessoa().getId(),DATA.plusDays(1));
        assertThat(ultimo.path("vinculos").get(0).path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(depois.path("vinculos").get(0).path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
        Map<String,Object> req=desligamento(DATA.plusDays(1),motivo.getId());
        JsonNode previa=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-desligamento").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"DES005 prévia do desligamento"));
        req.put("confirmarImpacto",true); req.put("tokenPrevia",previa.path("token").asText());
        JsonNode corrigido=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/retificar-desligamento").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"DES005 novo último dia auditado"));
        assertThat(corrigido.path("dataFim").asText()).isEqualTo(DATA.plusDays(1).toString());
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),200,"Original e retificação preservados"));
        assertThat(eventos).hasSize(3);
        assertThat(eventos.get(0).path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(eventos.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(c.pessoaA.getNome());
        assertThat(eventos.get(0).path("dataEfetiva").asText()).isEqualTo(INICIO.toString());
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("DESLIGAMENTO");
        assertThat(eventos.get(1).path("fimNovo").asText()).isEqualTo(DATA.toString());
        assertThat(eventos.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventos.get(0).path("id").asLong());
        assertThat(eventos.get(2).path("tipo").asText()).isEqualTo("RETIFICACAO_DESLIGAMENTO");
        assertThat(eventos.get(2).path("fimNovo").asText()).isEqualTo(DATA.plusDays(1).toString());
        assertThat(eventos.get(2).path("eventoPrecedenteId").asLong()).isEqualTo(eventos.get(1).path("id").asLong());
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/desligar").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Desligamento fora da Gestão");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-desligamento").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Prévia do desligamento fora da Gestão");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/retificar-desligamento").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Retificação do desligamento fora da Gestão");
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/desligar").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Desligamento sem data e motivo");
    }

    @Test
    void desligamentoFuturoPodeSerCanceladoSemApagarOrigem() throws Exception {
        Cenario c=criarCenario(); MotivoDesligamento motivo=motivo(c.gestaoA,CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        LocalDate fim=RelogioSistema.hoje().plusDays(10); desligar(c,c.vinculoA.getId(),fim,motivo.getId());
        JsonNode aberta=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/cancelar-desligamento-programado")
                .contentType(MediaType.APPLICATION_JSON).content(json(acao())),200,"DES006 cancelamento de desligamento futuro"));
        assertThat(aberta.path("dataFim").isNull()).isTrue(); assertThat(aberta.path("motivoDesligamentoId").isNull()).isTrue();
        JsonNode linha=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),200,"Desligamento original mantido após cancelamento"));
        assertThat(linha).hasSize(2); assertThat(linha.get(0).path("fimNovo").asText()).isEqualTo(fim.toString());
        assertThat(linha.get(1).path("tipo").asText()).isEqualTo("CANCELAMENTO_DESLIGAMENTO_PROGRAMADO");
        assertThat(c.alocacaoA.getDataFim()).isEqualTo(FIM);
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/cancelar-desligamento-programado").contentType(MediaType.APPLICATION_JSON).content(json(acao())),404,"Cancelamento de desligamento fora da Gestão");
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/cancelar-desligamento-programado").contentType(MediaType.APPLICATION_JSON).content(json(acao())),409,"Sem desligamento programado para cancelar");
    }

    @Test
    void alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior() throws Exception {
        Cenario c=criarCenario(); ComposicaoContratual b=destino(c,"B"), d=destino(c,"C");
        declararFonteNominalSintetica(c, "B04-ALOCACAO-NAO-CRIA-CONTRATACAO");
        long pessoaId = c.pessoaA.getId(), vinculoId = c.vinculoA.getId();
        long pessoasAntes = em.createQuery("select count(p) from Pessoa p", Long.class).getSingleResult();
        List<List<Object>> vinculosAntes = fotografiaVinculosB04();
        List<List<Object>> eventosAntes = fotografiaEventosVinculoB04();
        assertThat(em.createQuery("select e.id from EventoVinculoContratual e where e.vinculo.id=:v "
                        + "and e.tipoEvento=:tipo", Long.class).setParameter("v", vinculoId)
                .setParameter("tipo", com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO).getResultList())
                .as("Controle positivo: origem sintética declarada antes das mudanças internas").hasSize(1);
        JsonNode posterior=alterar(c,c.vinculoA.getId(),DATA.plusDays(10),d.getId(),false,null);
        JsonNode retro=alterar(c,c.vinculoA.getId(),DATA,b.getId(),false,null);
        assertThat(retro.path("dataFim").asText()).isEqualTo(DATA.plusDays(9).toString());
        assertThat(posterior.path("id").asLong()).isNotEqualTo(retro.path("id").asLong());
        JsonNode retificada=alterar(c,c.vinculoA.getId(),DATA,c.composicaoA.getId(),true,retro.path("id").asLong());
        assertThat(retificada.path("dataInicio").asText()).isEqualTo(DATA.toString());
        JsonNode meio=posicaoAlocacao(c,DATA), apos=posicaoAlocacao(c,DATA.plusDays(10));
        assertThat(meio.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId()); assertThat(apos.path("lotacaoId").asLong()).isEqualTo(d.getLotacao().getId());
        JsonNode faixas=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/alocacoes").param("mes","2026-06"),200,"HIS002 todas as faixas do mês"));
        assertThat(faixas.size()).isGreaterThanOrEqualTo(4);
        List<List<Object>> faixasExatas = new ArrayList<>();
        for (JsonNode faixa : faixas) faixasExatas.add(List.<Object>of(faixa.path("id").asLong(),
                faixa.path("vinculoId").asLong(), faixa.path("composicaoBaseId").asLong(),
                faixa.path("estadoNegocial").asText(), faixa.path("dataInicio").asText(), faixa.path("dataFim").asText()));
        assertThat(faixasExatas).as("HIS-002:001[1] conjunto mensal exato por IDs, estados e limites")
                .containsExactlyInAnyOrder(
                        List.<Object>of(c.alocacaoA.getId(), vinculoId, c.composicaoA.getId(), "REGISTRADA", INICIO.toString(), DATA.minusDays(1).toString()),
                        List.<Object>of(retro.path("id").asLong(), vinculoId, b.getId(), "SUBSTITUIDA", DATA.toString(), DATA.plusDays(9).toString()),
                        List.<Object>of(retificada.path("id").asLong(), vinculoId, c.composicaoA.getId(), "REGISTRADA", DATA.toString(), DATA.plusDays(9).toString()),
                        List.<Object>of(posterior.path("id").asLong(), vinculoId, d.getId(), "REGISTRADA", DATA.plusDays(10).toString(), FIM.toString()));
        long efetivas=0; for(JsonNode f:faixas) if(f.path("estadoNegocial").asText().equals("REGISTRADA")
                && !LocalDate.parse(f.path("dataInicio").asText()).isAfter(DATA)
                && (f.path("dataFim").isNull() || !LocalDate.parse(f.path("dataFim").asText()).isBefore(DATA))) efetivas++;
        assertThat(efetivas).isEqualTo(1);
        em.flush();em.clear();
        var anterior=em.find(AlocacaoVinculo.class,c.alocacaoA.getId());
        assertThat(anterior.getDataFim()).as("TMP-013:002 alocação anterior termina em D-1").isEqualTo(DATA.minusDays(1));
        var vinculoAtual=em.find(VinculoContratual.class,c.vinculoA.getId());
        assertThat(vinculoAtual.getDataInicio()).isEqualTo(INICIO);assertThat(vinculoAtual.getDataFim()).isNull();
        assertThat(vinculoAtual.getPessoa().getId()).as("ALO-002:002[0] mesma Pessoa canônica pelo ID").isEqualTo(pessoaId);
        assertThat(em.find(Pessoa.class, pessoaId).getId()).isEqualTo(pessoaId);
        assertThat(em.createQuery("select count(p) from Pessoa p", Long.class).getSingleResult())
                .as("ALO-002:002[0] alteração interna não cria outra Pessoa").isEqualTo(pessoasAntes);
        assertThat(fotografiaVinculosB04()).as("ALO-002:002[2] conjunto global de IDs e limites de vínculos permanece exato")
                .isEqualTo(vinculosAntes);
        assertThat(fotografiaEventosVinculoB04()).as("ALO-002:002[2] nenhum evento extra de contratação/desligamento")
                .isEqualTo(eventosAntes);
        JsonNode inicioReal = pesquisarVinculosB04(c, pessoaId, "inicio", INICIO);
        assertThat(ids(inicioReal.path("content"))).containsExactly(vinculoId);
        assertThat(inicioReal.path("totalElements").asLong()).isEqualTo(1);
        for (LocalDate diaMudanca : List.of(DATA, DATA.plusDays(10))) {
            for (String limite : List.of("inicio", "fim")) {
                JsonNode artificial = pesquisarVinculosB04(c, pessoaId, limite, diaMudanca);
                assertThat(artificial.path("content")).as("VIN-002:002[0]/HIS-003:002[1] %s da alocação não cria evento de vínculo em %s", limite, diaMudanca).isEmpty();
                assertThat(artificial.path("totalElements").asLong()).isZero();
            }
        }
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/alocacoes/eventos"),200,"ALO003 ALO004 trilha encadeada"));
        assertThat(eventos.size()).isGreaterThanOrEqualTo(3); assertThat(eventos.get(eventos.size()-1).path("hashEvento").asText()).hasSize(64);
        Map<String,Object> req=alocacao(DATA,b.getId());
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/previas/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Prévia de Alocação fora da Gestão");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Alteração de Alocação fora da Gestão");
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/alocacoes/"+retificada.path("id").asLong()+"/retificar").contentType(MediaType.APPLICATION_JSON).content(json(req)),404,"Retificação de Alocação fora da Gestão");
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/alocacoes").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Alocação exige data e combinação");

        // Controle positivo posterior: somente este comando encerra o vínculo.
        LocalDate fimReal = DATA.plusDays(15);
        MotivoDesligamento motivo = motivo(c.gestaoA, CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Fonte sintética do término usado como controle B04", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                fimReal, fimReal, java.time.Instant.now(), "SEI-SINTETICO-B04-TERMINO");
        JsonNode encerrado = desligar(c, vinculoId, fimReal, motivo.getId());
        LocalDate diaRegistro = java.time.Instant.parse(encerrado.path("desligamentoRegistradoEm").asText())
                .atZone(RelogioSistema.ZONA).toLocalDate();
        assertThat(diaRegistro).as("HIS-003:001[2] registro distinto do término efetivo e das mudanças")
                .isNotIn(fimReal, DATA, DATA.plusDays(10));
        em.flush(); em.clear();
        assertThat(em.find(VinculoContratual.class, vinculoId).getDataFim()).isEqualTo(fimReal);
        JsonNode porFimReal = pesquisarVinculosB04(c, pessoaId, "fim", fimReal);
        assertThat(ids(porFimReal.path("content"))).as("HIS-003:001[2] limite inclusivo exato do término efetivo").containsExactly(vinculoId);
        assertThat(porFimReal.path("totalElements").asLong()).isEqualTo(1);
        assertThat(porFimReal.path("content").get(0).path("dataFim").asText()).isEqualTo(fimReal.toString());
        for (LocalDate outroDia : List.of(DATA, DATA.plusDays(10), diaRegistro)) {
            JsonNode porOutroDia = pesquisarVinculosB04(c, pessoaId, "fim", outroDia);
            assertThat(porOutroDia.path("content")).as("HIS003 término não é mudança de alocação nem registro em %s", outroDia).isEmpty();
            assertThat(porOutroDia.path("totalElements").asLong()).isZero();
        }
        assertThat(ids(pesquisarVinculosB04(c, pessoaId, "inicio", INICIO).path("content"))).containsExactly(vinculoId);
    }

    @Test
    void alocacaoProgramadaCanceladaRestauraCoberturaEPrimeiroDiaDoFuturo() throws Exception {
        Cenario c=criarCenario(); ComposicaoContratual d=destino(c,"FUTURA"); LocalDate data=RelogioSistema.hoje().plusDays(10);
        JsonNode nova=alterar(c,c.vinculoA.getId(),data,d.getId(),false,null); long id=nova.path("id").asLong();
        JsonNode cancelada=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/alocacoes/"+id+"/cancelar-programacao")
                .contentType(MediaType.APPLICATION_JSON).content(json(acao())),200,"ALO007 programação cancelada restaura faixa anterior"));
        assertThat(cancelada.path("estadoNegocial").asText()).isEqualTo("CANCELADA");
        assertThat(posicaoAlocacao(c,data).path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
        assertThat(c.alocacaoA.getDataFim()).isEqualTo(FIM);
        executar(c.localB,post(V+"/"+c.vinculoA.getId()+"/alocacoes/"+id+"/cancelar-programacao").contentType(MediaType.APPLICATION_JSON).content(json(acao())),404,"Cancelamento de Alocação fora da Gestão");
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/alocacoes/"+c.alocacaoA.getId()+"/cancelar-programacao").contentType(MediaType.APPLICATION_JSON).content(json(acao())),409,"Alocação iniciada não é programação cancelável");
    }

    @Test
    void alocacaoNoUltimoDiaExigePrecedenciaExplicita() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-ALOCACAO-ULTIMO-DIA");
        ComposicaoContratual d=destino(c,"ULTIMO-DIA");
        MotivoDesligamento motivo=motivo(c.gestaoA,CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Desligamento individual com alocação no último dia", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA, DATA, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-ALOCACAO-ULTIMO-DIA");
        desligar(c,c.vinculoA.getId(),DATA,motivo.getId());
        Map<String,Object> req=alocacao(DATA,d.getId());
        JsonNode previa=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"ALO005 prévia reconhece último dia"));
        req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"ALO005 último dia ambíguo bloqueado");
        req.put("confirmarPrecedenciaUltimoDia",true);
        JsonNode unica=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"ALO005 último dia explicitamente atribuído"));
        assertThat(unica.path("dataInicio").asText()).isEqualTo(DATA.toString()); assertThat(unica.path("dataFim").asText()).isEqualTo(DATA.toString());
        assertThat(posicaoAlocacao(c,DATA).path("lotacaoId").asLong()).isEqualTo(d.getLotacao().getId());
    }

    @Test
    void fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-FIM-NATURAL");
        MotivoDesligamento natural=motivo(c.gestaoA,CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        c.contratoA.setDataFim(DATA); c.composicaoA.setDataFimVigencia(DATA); c.alocacaoA.setDataFim(DATA);
        // A fotografia tem updatable=false: preparar a fixture por DML JPA,
        // nunca tomar setter no cache como prova de alteração persistida.
        em.flush();
        assertThat(em.createQuery("update VersaoContratoNegocial v set v.dataFim=:fim where v.contrato.id=:id")
                .setParameter("fim",DATA).setParameter("id",c.contratoA.getId()).executeUpdate()).isEqualTo(1);
        em.clear();
        assertThat(em.createQuery("select v.dataFim from VersaoContratoNegocial v where v.contrato.id=:id",LocalDate.class)
                .setParameter("id",c.contratoA.getId()).getResultList()).containsExactly(DATA);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), natural.getId(), c.gestaoA.getId(), c.localA,
                "Fim natural documentado da vigência contratual", CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,
                DATA, DATA, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-FIM-NATURAL");
        JsonNode criacaoAntes = dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),200,
                "VIN011 evento anterior é fotografado antes do registro posterior")).get(0).deepCopy();
        java.time.Instant antesRegistro = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        assertThat(antesRegistro.atZone(java.time.ZoneOffset.UTC).toLocalDate()).isAfter(DATA);
        JsonNode encerrados=dados(executar(c.localA,post(V+"/contratos/"+c.contratoA.getId()+"/tratar-fim-natural"),200,"VIN009-011 tratamento posterior conserva fim da vigência"));
        java.time.Instant depoisRegistro = java.time.Instant.now();
        assertThat(encerrados).hasSize(1); assertThat(encerrados.get(0).path("dataFim").asText()).isEqualTo(DATA.toString());
        assertThat(encerrados.get(0).path("motivoDesligamentoId").asLong()).isEqualTo(natural.getId());
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/eventos"),200,"Causa própria do fim natural"));
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0).path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(eventos.get(0).path("dataEfetiva").asText()).isEqualTo(INICIO.toString());
        assertThat(eventos.get(0)).as("VIN011 registro posterior não reescreve criação").isEqualTo(criacaoAntes);
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("FIM_NATURAL_CONTRATO");
        assertThat(eventos.get(1).path("dataEfetiva").asText()).isEqualTo(DATA.toString());
        java.time.Instant registroEvento = java.time.Instant.parse(eventos.get(1).path("registradoEm").asText());
        assertThat(registroEvento).as("VIN011 Instant administrativo próprio, limitado pelo comando")
                .isBetween(antesRegistro, depoisRegistro);
        assertThat(registroEvento.atZone(java.time.ZoneOffset.UTC).toLocalDate()).isAfter(DATA);
        assertThat(eventos.get(1).path("responsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(eventos.get(1).path("responsavelUsername").asText()).isEqualTo(c.localA.getUsername());
        em.flush(); em.clear(); // persistência recarregada na transação ambiente; não commit independente
        VinculoContratual gravado = em.find(VinculoContratual.class,c.vinculoA.getId());
        assertThat(gravado.getPessoa().getId()).isEqualTo(c.pessoaA.getId());
        assertThat(gravado.getContrato().getId()).isEqualTo(c.contratoA.getId());
        assertThat(gravado.getDataFim()).isEqualTo(DATA);
        assertThat(gravado.getDesligamentoRegistradoEm()).isBetween(antesRegistro, depoisRegistro);
        assertThat(gravado.getDesligamentoRegistradoPorId()).isEqualTo(c.localA.getId());
        assertThat(gravado.getDesligamentoRegistradoPorUsername()).isEqualTo(c.localA.getUsername());
        EventoVinculoContratual eventoGravado = em.find(EventoVinculoContratual.class,eventos.get(1).path("id").asLong());
        assertThat(eventoGravado.getVinculo().getId()).isEqualTo(gravado.getId());
        assertThat(eventoGravado.getRegistradoEm()).isEqualTo(registroEvento);
        assertThat(eventoGravado.getResponsavelId()).isEqualTo(c.localA.getId());
        assertThat(eventoGravado.getResponsavelUsername()).isEqualTo(c.localA.getUsername());
        assertThat(eventos.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventos.get(0).path("id").asLong());
        executar(c.localB,post(V+"/contratos/"+c.contratoA.getId()+"/tratar-fim-natural"),404,"Fim natural fora da Gestão");
        executar(c.localB,post(V+"/contratos/"+c.contratoB.getId()+"/tratar-fim-natural"),409,"Contrato com vigência futura não encerra por fim natural");
    }

    @Test
    void mutacoesDePessoaVinculoEAlocacaoExigemGestorLocalAtivo() throws Exception {
        Cenario c=criarCenario(); long v=c.vinculoA.getId(), p=c.pessoaA.getId(), a=c.alocacaoA.getId();
        List<String> postagens=List.of(V,V+"/"+v+"/previas/retificacao-inicio",V+"/"+v+"/retificar-inicio",
                V+"/"+v+"/previas/alocacoes",V+"/"+v+"/alocacoes",V+"/"+v+"/alocacoes/"+a+"/retificar",
                V+"/"+v+"/alocacoes/"+a+"/cancelar-programacao",V+"/"+v+"/alterar-matricula",
                V+"/"+v+"/cancelar-programacao",V+"/"+v+"/desligar",V+"/"+v+"/previas/retificacao-desligamento",
                V+"/"+v+"/retificar-desligamento",V+"/"+v+"/cancelar-desligamento-programado",
                V+"/contratos/"+c.contratoA.getId()+"/tratar-fim-natural",P+"/"+p+"/correcoes-identidade");
        for(UsuarioAplicacao usuario:List.of(c.admin,c.nacional,c.estadualA,c.operacionalA)) {
            for(String path:postagens) executar(usuario,post(path).contentType(MediaType.APPLICATION_JSON).content("{}"),403,
                    "Mutação exige Gestor Local: "+usuario.getPerfil()+" "+path);
            executar(usuario,put(P+"/"+p+"/dados-opcionais").contentType(MediaType.APPLICATION_JSON).content("{}"),403,
                    "Opcionais exigem Gestor Local: "+usuario.getPerfil());
        }
    }

    @Test
    void retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "PESSOAS-COBERTURA-FREQUENCIA");
        Frequencia f=frequencia(c,false,INICIO,"FALTA"); long frequenciaId=f.getId();
        Map<String,Object> req=new LinkedHashMap<>(acao()); req.put("novoInicio",INICIO.plusDays(1));
        JsonNode previa=dados(executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/previas/retificacao-inicio")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"TMP009 impacto conhecido antes da confirmação"));
        req.put("confirmarImpacto",true); req.put("tokenPrevia",previa.path("token").asText());
        executar(c.localA,post(V+"/"+c.vinculoA.getId()+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,
                "TMP009 retificação propaga revisão sem apagar frequência");
        Frequencia preservada=em.find(Frequencia.class,frequenciaId);
        assertThat(preservada).isNotNull(); assertThat(preservada.getStatus()).isEqualTo("FALTA");
        assertThat(preservada.getVinculoContratual().getId()).isEqualTo(c.vinculoA.getId());
        assertThat(preservada.getAlocacaoVinculo().getId()).isEqualTo(c.alocacaoA.getId());
        assertThat(preservada.getValidadeOperacional().name()).isEqualTo("INVALIDA_COBERTURA");
        assertThat(preservada.getEstadoAuditoria().name()).isEqualTo("SUJEITA_REVISAO");
        assertThat(preservada.getBloqueadaEdicaoOperacional()).isTrue();
        assertThat(em.createQuery("select count(p) from PendenciaAdministrativa p where p.frequenciaId=:id",Long.class)
                .setParameter("id",frequenciaId).getSingleResult()).isGreaterThanOrEqualTo(1);
        assertThat(em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.id=:id",Long.class)
                .setParameter("id",frequenciaId).getSingleResult()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial() throws Exception {
        Cenario c=criarCenario(); c.composicaoA.setEfetivoPrevisto(1);
        Map<String,Object> req=cadastro(c,DATA,null,null); req.put("pessoa",Map.of("nome","Pessoa excedente 1","cpf",cpf(790000000+c.pessoaA.getId().intValue())));
        JsonNode negativa = dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"CMP008 excedente exige confirmação específica"));
        assertThat(negativa.path("message").asText()).as("CMP008 resposta informa todas as condições do consentimento")
                .contains("confirmação", "justificativa", "evidência", "autorização");
        req.put("confirmarInclusaoExcedente",true); req.put("justificativaExcedente","Necessidade formal documentada");
        req.put("evidenciaExcedente","SEI-EXCEDENTE"); req.put("autorizacaoExcedente","Ato do Gestor Local");
        JsonNode primeiro=dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),201,"CMP009 primeira inclusão excedente autorizada"));
        JsonNode eventos=dados(executar(c.localA,get(V+"/"+primeiro.path("id").asLong()+"/eventos"),200,"Excedente documentado desde o primeiro INSERT"));
        assertThat(eventos.get(0).path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(eventos.get(0).path("efetivoPrevistoExcedente").asInt()).isEqualTo(1);
        assertThat(eventos.get(0).path("ocupacaoAposInclusaoExcedente").asInt()).isEqualTo(2);
        assertThat(eventos.get(0).path("evidencia").asText()).contains("SEI-EXCEDENTE");
        assertThat(eventos.get(0).path("autorizacao").asText()).contains("Ato do Gestor Local");
        long primeiroId = primeiro.path("id").asLong();
        em.flush();
        em.clear();
        Map<String, List<List<Object>>> primeiraFonteAntes = fonteDaInclusaoExcedente(primeiroId);
        assertThat(primeiraFonteAntes.get("vinculo")).hasSize(1);
        assertThat(primeiraFonteAntes.get("alocacoes")).hasSize(1);
        assertThat(primeiraFonteAntes.get("eventosVinculo")).hasSize(1);
        assertThat(primeiraFonteAntes.get("eventosAlocacao")).hasSize(1);
        req.put("pessoa",Map.of("nome","Pessoa excedente 2","cpf",cpf(795000000+c.pessoaA.getId().intValue())));
        JsonNode segundo=dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),201,"CMP010 segunda inclusão excedente sem teto artificial"));
        assertThat(segundo.path("id").asLong()).isNotEqualTo(primeiroId);
        em.flush();
        em.clear();
        assertThat(fonteDaInclusaoExcedente(primeiroId))
                .as("CMP008 a segunda inclusão não reescreve o primeiro vínculo, alocação ou eventos")
                .isEqualTo(primeiraFonteAntes);
        VinculoContratual preservado = em.find(VinculoContratual.class, primeiroId);
        assertThat(preservado).isNotNull();
        assertThat(preservado.getPessoa().getId()).isEqualTo(primeiro.path("pessoaId").asLong());
        assertThat(preservado.getPessoa().getNome()).isEqualTo("Pessoa excedente 1");
        assertThat(preservado.getContrato().getId()).isEqualTo(c.contratoA.getId());
        assertThat(preservado.getDataInicio()).isEqualTo(DATA);
        assertThat(preservado.getDataFim()).isNull();
        assertThat(preservado.getMatricula()).isNull();
        assertThat(preservado.getEstadoNegocial().name()).isEqualTo("REGISTRADO");
        assertThat(dados(executar(c.localA, get(V+"/"+primeiroId+"/eventos"), 200,
                "CMP008 primeira autorização relida após a segunda inclusão"))).isEqualTo(eventos);
    }

    private List<List<Object>> fotografiaVinculosB04() {
        return em.createQuery("select v.id, v.pessoa.id, v.contrato.id, v.dataInicio, v.dataFim, v.estadoNegocial, "
                        + "v.matricula, v.desligamentoRegistradoEm from VinculoContratual v order by v.id", Object[].class)
                .getResultList().stream().map(Arrays::asList).toList();
    }
    private List<List<Object>> fotografiaEventosVinculoB04() {
        return em.createQuery("select e.id, e.vinculo.id, e.tipoEvento, e.dataEfetiva, e.inicioAnterior, e.inicioNovo, "
                        + "e.fimAnterior, e.fimNovo, e.hashAnterior, e.hashEvento from EventoVinculoContratual e order by e.id", Object[].class)
                .getResultList().stream().map(Arrays::asList).toList();
    }
    private JsonNode pesquisarVinculosB04(Cenario c, long pessoaId, String limite, LocalDate dia) throws Exception {
        return dados(executar(c.localA, get(V).param("pessoaId", Long.toString(pessoaId))
                .param(limite+"De", dia.toString()).param(limite+"Ate", dia.toString()), 200,
                "B04 busca inclusiva por data canônica de " + limite + ": " + dia));
    }

    private Map<String, List<List<Object>>> fonteDaInclusaoExcedente(long vinculoId) {
        // Esta classe mantém a transação ambiente herdada. flush/clear força
        // releitura JPA; não equivale a comprovar commit em outra transação.
        return Map.of(
                "vinculo", linhasDaInclusao("select v.id,v.pessoa.id,v.pessoa.nome,v.pessoa.cpf,v.contrato.id,"
                        + "v.dataInicio,v.dataFim,v.matricula,v.estadoNegocial,v.criadoEm,v.criadoPorId,"
                        + "v.criadoPorUsername,v.version from VinculoContratual v where v.id=:v", vinculoId),
                "alocacoes", linhasDaInclusao("select a.id,a.vinculo.id,c.id,a.lotacao.id,a.jornada.id,a.cargo.id,"
                        + "a.dataInicio,a.dataFim,a.estadoNegocial,a.criadoEm,a.criadoPorId,a.version,"
                        + "a.lotacaoNomeFotografia,a.jornadaDescricaoFotografia,a.cargoNomeFotografia,"
                        + "a.lotacaoMunicipioFotografia,a.jornadaSemanalFotografia,a.municipioAusenciaComprovada "
                        + "from AlocacaoVinculo a left join a.composicao c where a.vinculo.id=:v order by a.id", vinculoId),
                "eventosVinculo", linhasDaInclusao("select e.id,e.vinculo.id,e.tipoEvento,e.dataEfetiva,e.registradoEm,"
                        + "e.responsavelId,e.responsavelUsername,e.justificativa,e.evidencia,e.autorizacao,"
                        + "e.inclusaoExcedente,e.composicaoExcedenteId,e.lotacaoExcedenteId,e.jornadaExcedenteId,"
                        + "e.cargoExcedenteId,e.efetivoPrevistoExcedente,e.ocupacaoAposInclusaoExcedente,"
                        + "e.hashAnterior,e.hashEvento,e.inicioNovo,e.fimNovo,e.matriculaNova,e.pessoaCpfFotografia "
                        + "from EventoVinculoContratual e where e.vinculo.id=:v order by e.id", vinculoId),
                "eventosAlocacao", linhasDaInclusao("select e.id,e.vinculo.id,e.tipoEvento,e.dataEfetiva,e.registradoEm,"
                        + "e.responsavelId,e.responsavelUsername,e.justificativa,e.evidencia,e.autorizacao,"
                        + "e.inclusaoExcedente,e.efetivoPrevistoExcedente,e.ocupacaoAposInclusaoExcedente,"
                        + "e.composicaoNovaId,e.lotacaoNovaId,e.jornadaNovaId,e.cargoNovoId,e.inicioNovo,e.fimNovo,"
                        + "e.hashAnterior,e.hashEvento from EventoAlocacaoVinculo e "
                        + "where e.vinculo.id=:v order by e.id", vinculoId));
    }

    private List<List<Object>> linhasDaInclusao(String jpql, long vinculoId) {
        return em.createQuery(jpql, Object[].class).setParameter("v", vinculoId).getResultList().stream()
                .map(Arrays::asList).toList();
    }

    private JsonNode cadastrar(Cenario c,LocalDate inicio,LocalDate fim,String matricula) throws Exception {
        Map<String,Object> req=cadastro(c,inicio,fim,matricula);
        req.put("pessoa",Map.of("nome","Pessoa HTTP "+UUID.randomUUID(),"cpf",cpf(740000000+c.pessoaA.getId().intValue())));
        return dados(executar(c.localA,post(V).contentType(MediaType.APPLICATION_JSON).content(json(req)),201,"Cadastro integrado de vínculo"));
    }
    private List<String> estadoRetificacaoB07(long vinculoId) {
        em.flush(); em.clear();
        List<String> estado = new ArrayList<>();
        for (String jpql : List.of(
                "select v.id,v.pessoa.id,v.contrato.id,v.dataInicio,v.dataFim,v.estadoNegocial,v.version from VinculoContratual v where v.id=:v",
                "select a.id,a.vinculo.id,a.dataInicio,a.dataFim,a.estadoNegocial,a.version from AlocacaoVinculo a where a.vinculo.id=:v order by a.id",
                "select e.id,e.hashEvento from EventoVinculoContratual e where e.vinculo.id=:v order by e.id",
                "select e.id,e.hashEvento from EventoAlocacaoVinculo e where e.vinculo.id=:v order by e.id"))
            em.createQuery(jpql,Object[].class).setParameter("v",vinculoId).getResultList().stream()
                    .map(Arrays::toString).forEach(estado::add);
        return estado;
    }

    private Map<String,Object> cadastro(Cenario c,LocalDate inicio,LocalDate fim,String matricula) {
        Map<String,Object> req=new LinkedHashMap<>(); req.put("contratoId",c.contratoA.getId()); req.put("dataInicio",inicio);
        req.put("dataFim",fim); req.put("matricula",matricula); req.put("composicaoId",c.composicaoA.getId()); return req;
    }
    private Map<String,Object> acao() { return new LinkedHashMap<>(Map.of("justificativa","Conferência documental do fato real","evidencia","SEI-TESTE-42","autorizacao","Despacho 7")); }
    private Map<String,Object> desligamento(LocalDate fim,Long motivo) { Map<String,Object> r=acao(); r.put("dataFim",fim); r.put("motivoDesligamentoId",motivo); return r; }
    private JsonNode desligar(Cenario c,Long id,LocalDate fim,Long motivo) throws Exception { return dados(executar(c.localA,post(V+"/"+id+"/desligar").contentType(MediaType.APPLICATION_JSON).content(json(desligamento(fim,motivo))),200,"DES001-004 desligamento datado e auditado")); }
    private Map<String,Object> alocacao(LocalDate data,Long composicao) { Map<String,Object> r=acao(); r.put("dataEfetiva",data); r.put("composicaoId",composicao); return r; }
    private JsonNode alterar(Cenario c,Long id,LocalDate data,Long composicao,boolean retificar,Long alocacaoId) throws Exception {
        Map<String,Object> req=alocacao(data,composicao);
        JsonNode p=dados(executar(c.localA,post(V+"/"+id+"/previas/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"Prévia de mudança temporal de Alocação"));
        assertThat(p.path("incompatibilidades")).isEmpty(); req.put("tokenPrevia",p.path("token").asText()); req.put("confirmarImpacto",true);
        return dados(executar(c.localA,post(V+"/"+id+"/alocacoes"+(retificar?"/"+alocacaoId+"/retificar":""))
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"ALO001-004 mudança temporal auditada"));
    }
    private ComposicaoContratual destino(Cenario c,String sufixo) {
        Lotacao l=new Lotacao("Destino "+sufixo+" "+c.gestaoA.getId()); l.setGestaoLocal(c.gestaoA); l.setMunicipio(c.lotacaoA.getMunicipio()); l.getTurnos().add(c.jornadaA); em.persist(l);
        // Fonte sintética explícita: a composição desse cenário pressupõe a
        // disponibilidade da Lotação desde INICIO; a flag atual não a prova.
        EventoDisponibilidadeCatalogo origem=new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO);
        origem.setCatalogoId(l.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética do destino de alocação desde o início contratual: "+sufixo);
        origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername());
        em.persist(origem); em.flush();
        return composicao(c.contratoA,l,c.jornadaA,c.cargoA,5);
    }
    private MotivoDesligamento motivo(GestaoLocal g,CategoriaTerminoVinculo categoria) {
        MotivoDesligamento m=new MotivoDesligamento(); m.setGestaoLocal(g); m.setDescricao("Motivo "+categoria+" "+g.getId()); m.setCategoria(categoria); em.persist(m); em.flush();
        EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(m.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa de término durante o período do cenário");
        em.persist(origem); em.flush(); return m;
    }
    private JsonNode posicao(Cenario c,Long pessoaId,LocalDate data) throws Exception { return dados(executar(c.localA,get(V+"/pessoas/"+pessoaId+"/posicao").param("data",data.toString()),200,"Posição no dia efetivo")); }
    private JsonNode posicaoAlocacao(Cenario c,LocalDate data) throws Exception { return dados(executar(c.localA,get(V+"/"+c.vinculoA.getId()+"/alocacoes/posicao").param("data",data.toString()),200,"Alocação única no dia efetivo")); }
    private List<Long> ids(JsonNode nodes) { List<Long> ids=new ArrayList<>(); nodes.forEach(n->ids.add(n.path("id").asLong())); return ids; }
}
