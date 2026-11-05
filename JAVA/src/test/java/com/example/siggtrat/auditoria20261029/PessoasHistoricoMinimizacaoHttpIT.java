package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** As narrativas geradas pelo próprio cadastro também contêm dados pessoais restritos. */
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class PessoasHistoricoMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings={"GESTOR_LOCAL","GESTOR_ESTADUAL","GESTOR_NACIONAL","ADMINISTRADOR"})
    void consultasDoHistoricoMinimizamNarrativaPessoalSemAlterarRegistroAssinado(String perfil) throws Exception {
        Cenario c=criarCenarioPersistido();
        String rg="RG-PRIVADO-"+c.pessoaA.getId(),email="privado-"+c.pessoaA.getId()+"@example.test";
        ObjectNode body=(ObjectNode)dados(executar(c.localA,get("/api/pessoas/"+c.pessoaA.getId()),200,"Preparar edição opcional real"));
        body.put("rg",rg); body.put("email",email);
        executar(c.localA,put("/api/pessoas/"+c.pessoaA.getId()+"/dados-opcionais").contentType(MediaType.APPLICATION_JSON).content(json(body)),
                200,"Edição real gera histórico com valores pessoais anteriores e novos");
        String porEmpregado="/api/historicos-empregados/empregado/"+c.empregadoA.getId();
        JsonNode originais=dados(executar(c.localA,get(porEmpregado),200,"Gestor responsável consulta conteúdo original"));
        assertThat(originais.toString()).contains(rg,email);
        Map<Long,JsonNode> porId=new LinkedHashMap<>(); originais.forEach(x->porId.put(x.path("id").asLong(),x));
        assertThat(porId.size()).isGreaterThanOrEqualTo(2);
        UsuarioAplicacao leitor=switch(perfil) {
            case "GESTOR_LOCAL" -> c.localA;
            case "GESTOR_ESTADUAL" -> c.estadualA;
            case "GESTOR_NACIONAL" -> c.nacional;
            case "ADMINISTRADOR" -> c.admin;
            default -> throw new IllegalArgumentException(perfil);
        };
        boolean completo=perfil.equals("GESTOR_LOCAL");
        JsonNode pagina=dados(executar(leitor,get("/api/historicos-empregados").param("size","2000").param("sort","id,desc"),200,
                "PER006 PER011 narrativa pessoal na listagem por "+perfil));
        validarColecao(pagina.path("content"),porId,rg,email,completo);
        JsonNode colecao=dados(executar(leitor,get(porEmpregado),200,"PER006 PER011 narrativa por Empregado para "+perfil));
        validarColecao(colecao,porId,rg,email,completo);
        for(var entry:porId.entrySet()) {
            JsonNode individual=dados(executar(leitor,get("/api/historicos-empregados/"+entry.getKey()),200,
                    "PER006 PER011 narrativa individual para "+perfil));
            validarItem(individual,entry.getValue(),rg,email,completo);
        }
        executar(c.localB,get("/api/historicos-empregados/"+porId.keySet().iterator().next()),403,
                "Minimizar conteúdo não concede acesso fora do escopo histórico");
        assertThat(dados(executar(c.localB,get(porEmpregado),200,"Histórico da outra Gestão permanece fora da coleção"))).isEmpty();
        emTransacao(()->{
            for(var entry:porId.entrySet()) {
                HistoricoEmpregado original=em.find(HistoricoEmpregado.class,entry.getKey());
                assertThat(original.getDescricao()).isEqualTo(entry.getValue().path("descricao").asText());
                assertThat(original.getHashIntegridade()).isEqualTo(entry.getValue().path("hashIntegridade").asText());
            }
            return null;
        });
    }

    private void validarColecao(JsonNode itens,Map<Long,JsonNode> originais,String rg,String email,boolean completo) {
        int encontrados=0;
        for(JsonNode item:itens) {
            JsonNode original=originais.get(item.path("id").asLong());
            if(original!=null){validarItem(item,original,rg,email,completo);encontrados++;}
        }
        assertThat(encontrados).as("Metadados dos registros continuam visíveis no escopo").isEqualTo(originais.size());
    }
    private void validarItem(JsonNode item,JsonNode original,String rg,String email,boolean completo) {
        for(String campo:new String[]{"id","empregado","dataEvento","usuario","hashIntegridade","version"})
            assertThat(item.path(campo)).as("Metadado histórico preservado: %s",campo).isEqualTo(original.path(campo));
        assertThat(item.path("hashIntegridade").asText()).startsWith("HMAC-SHA256:");
        if(completo)assertThat(item.path("descricao")).isEqualTo(original.path("descricao"));
        else assertThat(item.toString()).as("Consulta de auditoria não deve revelar RG ou e-mail pela narrativa")
                .doesNotContain(rg,email);
    }
}
