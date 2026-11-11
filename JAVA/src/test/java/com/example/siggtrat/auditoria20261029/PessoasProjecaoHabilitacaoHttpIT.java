package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasProjecaoHabilitacaoHttpIT extends ContratosHttpBase {
    @Test
    void opcionalNaPessoaAposInativacaoEReativacaoMantemMesmaLinhaCanonica() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); return new Preparado(c, publicado(c, 4, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate hoje = RelogioSistema.hoje();
        int seed = Math.toIntExact(733000000L + f.contrato().id());
        JsonNode criado = enviar(f.c().localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa habilitação operacional " + seed, "cpf", cpf(seed)),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", hoje.minusDays(5)), 201, "Preparação: Pessoa e vínculo vigente");
        long pessoaId = criado.path("pessoaId").asLong();
        long empregadoId = emTransacao(() -> em.createQuery(
                "select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult());
        LinhaCanonica antes = linhaCanonica(pessoaId);

        ciclo(f, "inativar", hoje);
        atualizarRg(f, pessoaId, "RG-OPCIONAL-733");
        assertThat(linhaCanonica(pessoaId)).as("VIN013 ALO008 inativação e opcional não encerram nem recriam a linha")
                .isEqualTo(antes);
        conferirProjecao(empregadoId, ConstantesDominio.STATUS_INATIVO, false, "RG-OPCIONAL-733");

        LocalDate amanha = hoje.plusDays(1);
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(amanha);
            ciclo(f, "reativar", amanha);
            atualizarRg(f, pessoaId, "RG-REATIVADO-733");
            conferirProjecao(empregadoId, ConstantesDominio.STATUS_ATIVO, true, "RG-REATIVADO-733");
            long eventos = quantidadeEventosContrato(f.contrato().id());
            enviar(f.c().localA, base(f.contrato().id()) + "/reativar",
                    mapa("dataEfetiva", amanha, "motivo", "Repetição não cria nova reativação"), 409,
                    "Contrato já ativo recusa reativação repetida antes de qualquer mutação");
            assertThat(quantidadeEventosContrato(f.contrato().id())).isEqualTo(eventos);
            assertThat(linhaCanonica(pessoaId)).as("Reativação reutiliza os mesmos vínculos, faixas e eventos históricos")
                    .isEqualTo(antes);
            assertThat(consultar(f.c().localA, base(f.contrato().id()) + "/posicao-temporal?data=" + hoje,
                    "O intervalo administrativamente inativo permanece histórico")
                    .path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        }
    }

    @Test
    void retomadaPreservaEncerradoCanceladoEFaixasVencidaESubstituida() throws Exception {
        Preparado f = emTransacao(() -> {
            try { Cenario c=criarCenario(); return new Preparado(c,publicado(c,8,4,false)); }
            catch(Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate hoje=RelogioSistema.hoje(), retomada=hoje.plusDays(1), mudanca=hoje.minusDays(2);
        JsonNode elegivel=cadastrarB05(f,1,hoje.minusDays(5),null);
        JsonNode encerrado=cadastrarB05(f,2,hoje.minusDays(10),hoje.minusDays(6));
        JsonNode cancelado=cadastrarB05(f,3,hoje.plusDays(5),null);
        for(JsonNode identidade:List.of(elegivel,encerrado,cancelado))
            assertThat(identidade.path("contratoId").asLong()).isEqualTo(f.contrato().id());
        long v=elegivel.path("id").asLong(), p=elegivel.path("pessoaId").asLong();
        enviar(f.c().localA,"/api/vinculos-contratuais/"+cancelado.path("id").asLong()+"/cancelar-programacao",
                mapa("justificativa","Cancelamento sintético antes de iniciar","evidencia","SEI-B05-CANCELAR",
                        "autorizacao","ATO-B05-CANCELAR"),200,"VIN013 controle cancelado não é vínculo apto à retomada");
        JsonNode inicial=consultar(f.c().localA,"/api/vinculos-contratuais/"+v+"/alocacoes","ALO008 faixa original").get(0);
        JsonNode substituida=mudarB05(f,v,mudanca,f.contrato().composicao2(),null);
        JsonNode finalValida=mudarB05(f,v,mudanca,f.contrato().composicao1(),substituida.path("id").asLong());
        long inicialId=inicial.path("id").asLong(), substituidaId=substituida.path("id").asLong(), finalId=finalValida.path("id").asLong();
        assertThat(java.util.Set.of(inicialId,substituidaId,finalId)).hasSize(3);
        emTransacao(() -> {
            var anterior=em.find(com.example.siggtrat.entidade.AlocacaoVinculo.class,inicialId);
            var sucedida=em.find(com.example.siggtrat.entidade.AlocacaoVinculo.class,substituidaId);
            assertThat(anterior.getDataFim()).isEqualTo(mudanca.minusDays(1));
            assertThat(sucedida.getEstadoNegocial().name()).isEqualTo("SUBSTITUIDA");
            return null;
        });
        Map<Long,LinhaCanonica> antes=new java.util.LinkedHashMap<>();
        Map<Long,Long> empregados=new java.util.LinkedHashMap<>();
        for(JsonNode pessoa:List.of(elegivel,encerrado,cancelado)) {
            long pessoaId=pessoa.path("pessoaId").asLong(); antes.put(pessoaId,linhaCanonica(pessoaId));
            empregados.put(pessoaId,emTransacao(() -> em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p",Long.class)
                    .setParameter("p",pessoaId).getSingleResult()));
        }
        ciclo(f,"inativar",hoje);
        for(long empregadoId:empregados.values()) assertStatusB05(empregadoId,ConstantesDominio.STATUS_INATIVO);
        try(var relogio=Mockito.mockStatic(RelogioSistema.class,Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(retomada);
            ciclo(f,"reativar",retomada);
            assertStatusB05(empregados.get(p),ConstantesDominio.STATUS_ATIVO);
            assertStatusB05(empregados.get(encerrado.path("pessoaId").asLong()),ConstantesDominio.STATUS_INATIVO);
            assertStatusB05(empregados.get(cancelado.path("pessoaId").asLong()),ConstantesDominio.STATUS_INATIVO);
            for(var entrada:antes.entrySet()) {
                assertThat(linhaCanonica(entrada.getKey())).as("VIN013 ALO008 Pessoa "+entrada.getKey()+" mantém IDs, limites, estados e eventos")
                        .isEqualTo(entrada.getValue());
                assertThat(emTransacao(() -> em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p",Long.class)
                        .setParameter("p",entrada.getKey()).getResultList())).containsExactly(empregados.get(entrada.getKey()));
            }
            java.time.Instant antesK=java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            JsonNode posicao=consultar(f.c().localA,"/api/vinculos-contratuais/"+v+"/alocacoes/posicao?data="+retomada,
                    "ALO008 somente faixa final válida volta a operar em D");
            java.time.Instant depoisK=java.time.Instant.now();
            assertThat(posicao.path("vinculoId").asLong()).isEqualTo(v);
            assertThat(posicao.path("alocacaoId").asLong()).isEqualTo(finalId);
            assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
            assertThat(java.time.Instant.parse(posicao.path("conhecidoAte").asText())).isBetween(antesK,depoisK);
            assertThat(posicao.path("dataReferencia").asText()).isEqualTo(retomada.toString());
            assertThat(emTransacao(() -> em.find(com.example.siggtrat.entidade.VinculoContratual.class,
                    encerrado.path("id").asLong()).getDataFim())).isEqualTo(hoje.minusDays(6));
            assertThat(emTransacao(() -> em.find(com.example.siggtrat.entidade.VinculoContratual.class,
                    cancelado.path("id").asLong()).getEstadoNegocial().name())).isEqualTo("CANCELADO");
        }
    }

    private JsonNode cadastrarB05(Preparado f,int ordem,LocalDate inicio,LocalDate fim) throws Exception {
        return enviar(f.c().localA,"/api/vinculos-contratuais",mapa("pessoa",mapa("nome","Pessoa B05 "+ordem,
                "cpf",cpf(Math.toIntExact(753000000L+f.contrato().id()*10+ordem))),"contratoId",f.contrato().id(),
                "composicaoId",f.contrato().composicao1(),"dataInicio",inicio,"dataFim",fim),201,
                "VIN013 preparo explícito da Pessoa "+ordem);
    }

    private JsonNode mudarB05(Preparado f,long vinculo,LocalDate data,long composicao,Long retificar) throws Exception {
        String rota="/api/vinculos-contratuais/"+vinculo;
        var req=mapa("dataEfetiva",data,"composicaoId",composicao,"justificativa","Decisão de alocação sintética independente",
                "evidencia","SEI-B05-FAIXAS","autorizacao","ATO-B05-FAIXAS");
        JsonNode previa=enviar(f.c().localA,rota+"/previas/alocacoes",req,200,"ALO008 prévia explícita de alteração/retificação");
        assertThat(previa.path("incompatibilidades")).isEmpty();
        req.put("confirmarImpacto",true);req.put("tokenPrevia",previa.path("token").asText());
        return enviar(f.c().localA,rota+"/alocacoes"+(retificar==null?"":"/"+retificar+"/retificar"),req,200,
                "ALO008 linha de faixas produzida pela API, não por estado atual inventado");
    }

    private void assertStatusB05(long empregadoId,String esperado) {
        emTransacao(() -> { assertThat(em.find(Empregado.class,empregadoId).getStatus().getDescricao()).isEqualToIgnoringCase(esperado); return null; });
    }

    private void atualizarRg(Preparado f, long pessoaId, String rg) throws Exception {
        ObjectNode dto = (ObjectNode) consultar(f.c().localA, "/api/pessoas/" + pessoaId,
                "A Pessoa é a fonte dos dados opcionais");
        dto.put("rg", rg);
        JsonNode salvo = dados(executar(f.c().localA, put("/api/pessoas/" + pessoaId + "/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200,
                "COL011 atualização exclusiva de RG funciona após mudança administrativa do Contrato"));
        assertThat(salvo.path("rg").asText()).isEqualTo(rg);
    }

    private void conferirProjecao(long empregadoId, String status, boolean contratoAtivo, String rg) {
        emTransacao(() -> {
            Empregado e = em.find(Empregado.class, empregadoId);
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(status);
            assertThat(e.getContratos()).hasSize(1);
            assertThat(e.getContratos().get(0).getAtivo()).isEqualTo(contratoAtivo);
            assertThat(e.getPessoaCanonica().getRg()).isEqualTo(rg);
            return null;
        });
    }

    private LinhaCanonica linhaCanonica(long pessoaId) {
        return emTransacao(() -> new LinhaCanonica(
                linhas("select v.id,v.contrato.id,v.dataInicio,v.dataFim,v.estadoNegocial from VinculoContratual v "
                        + "where v.pessoa.id=:p order by v.id", pessoaId),
                linhas("select a.id,a.vinculo.id,a.dataInicio,a.dataFim,a.estadoNegocial from AlocacaoVinculo a "
                        + "where a.vinculo.pessoa.id=:p order by a.id", pessoaId),
                linhas("select e.id,e.tipoEvento,e.hashEvento from EventoVinculoContratual e "
                        + "where e.vinculo.pessoa.id=:p order by e.id", pessoaId),
                linhas("select e.id,e.tipoEvento,e.hashEvento from EventoAlocacaoVinculo e "
                        + "where e.vinculo.pessoa.id=:p order by e.id", pessoaId)));
    }

    private List<String> linhas(String jpql, long pessoaId) {
        return em.createQuery(jpql, Object[].class).setParameter("p", pessoaId)
                .getResultList().stream().map(Arrays::toString).toList();
    }

    private long quantidadeEventosContrato(long contratoId) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult());
    }

    private void ciclo(Preparado f, String comando, LocalDate data) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "motivo", "Habilitação administrativa " + comando);
        JsonNode previa = enviar(f.c().localA, base(f.contrato().id()) + "/previas/" + comando, req, 200,
                "Prévia do evento administrativo");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.c().localA, base(f.contrato().id()) + "/" + comando, req, 200,
                "Habilitação preserva a linha de vínculo e alocação");
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
    private record LinhaCanonica(List<String> vinculos, List<String> alocacoes,
                                 List<String> eventosVinculo, List<String> eventosAlocacao) {}
}
