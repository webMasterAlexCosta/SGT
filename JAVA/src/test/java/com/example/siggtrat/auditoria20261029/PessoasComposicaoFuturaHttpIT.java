package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A inclusão posterior respeita toda a programação já confirmada da composição. */
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class PessoasComposicaoFuturaHttpIT extends ContratosHttpBase {
    @Test
    void vinculoAbertoNaoPodeAtravessarZeramentoFuturoRegistradoAntesDoCadastro() throws Exception {
        Preparado f=preparar();
        String cpf=cpf(850000000+f.cenario().pessoaA.getId().intValue());
        enviar(f.cenario().localA,"/api/vinculos-contratuais",cadastro(f,cpf,null),409,
                "CMP006 CMP007 ALO007 cadastro não pode tornar inválida uma composição já programada para zero");
        emTransacao(()->{
            assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf",Long.class)
                    .setParameter("cpf",cpf).getSingleResult()).as("Cadastro integrado rejeitado não deixa Pessoa órfã").isZero();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id",Long.class)
                    .setParameter("id",f.contrato().id()).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:id",Long.class)
                    .setParameter("id",f.contrato().id()).getSingleResult()).isZero();
            return null;
        });
        assertThat(capacidadeNoZeramento(f)).isZero();
    }

    @Test
    void vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido() throws Exception {
        Preparado f=preparar(); LocalDate fim=f.zeramento().minusDays(1);
        String cpf=cpf(860000000+f.cenario().pessoaA.getId().intValue());
        JsonNode criado=enviar(f.cenario().localA,"/api/vinculos-contratuais",cadastro(f,cpf,fim),201,
                "CMP006 CMP007 término anterior ao zeramento mantém cobertura válida de todos os dias");
        long vinculoId=criado.path("id").asLong(),pessoaId=criado.path("pessoaId").asLong();
        assertThat(criado.path("dataFim").asText()).isEqualTo(fim.toString());
        emTransacao(()->{
            assertThat(em.find(Pessoa.class,pessoaId).getCpf()).isEqualTo(cpf);
            assertThat(em.find(VinculoContratual.class,vinculoId).getDataFim()).isEqualTo(fim);
            List<AlocacaoVinculo> faixas=em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id",AlocacaoVinculo.class)
                    .setParameter("id",vinculoId).getResultList();
            assertThat(faixas).hasSize(1);
            assertThat(faixas.get(0).getDataInicio()).isEqualTo(f.zeramento().minusDays(2));
            assertThat(faixas.get(0).getDataFim()).isEqualTo(fim);
            return null;
        });
        JsonNode posicao=consultar(f.cenario().localA,"/api/vinculos-contratuais/pessoas/"+pessoaId+"/posicao?data="+f.zeramento(),
                "VIN002 DES002 dia de capacidade zero já está fora do vínculo finito");
        assertThat(posicao.path("vinculos").get(0).path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
        assertThat(capacidadeNoZeramento(f)).isZero();
    }

    @Test
    void previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta() throws Exception {
        Preparado f=preparar(); String cpf=cpf(870000000+f.cenario().pessoaA.getId().intValue());
        Map<String,Object> inclusao=cadastro(f,cpf,null); inclusao.put("composicaoId",f.contrato().composicao2());
        JsonNode criado=enviar(f.cenario().localA,"/api/vinculos-contratuais",inclusao,201,
                "Vínculo inicial usa a composição B que continua válida após a redução");
        long vinculoId=criado.path("id").asLong();
        Map<String,Object> mudanca=mapa("dataEfetiva",f.zeramento().minusDays(1),"composicaoId",f.contrato().composicao1(),
                "justificativa","Mudança para a composição A","evidencia","SEI-MUDANCA","autorizacao","ATO-MUDANCA");
        String base="/api/vinculos-contratuais/"+vinculoId;
        JsonNode previa=enviar(f.cenario().localA,base+"/previas/alocacoes",mudanca,200,
                "CMP006 CMP007 prévia deve avaliar toda a faixa futura proposta");
        assertThat(previa.path("incompatibilidades")).as("A composição de destino será zerada durante a faixa proposta").isNotEmpty();
        mudanca.put("tokenPrevia",previa.path("token").asText()); mudanca.put("confirmarImpacto",true);
        enviar(f.cenario().localA,base+"/alocacoes",mudanca,409,"Confirmação não pode contornar incompatibilidade futura já apresentada");
        emTransacao(()->{
            List<AlocacaoVinculo> faixas=em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id",AlocacaoVinculo.class)
                    .setParameter("id",vinculoId).getResultList();
            assertThat(faixas).hasSize(1);
            assertThat(faixas.get(0).getComposicao().getId()).isEqualTo(f.contrato().composicao2());
            assertThat(faixas.get(0).getDataFim()).isEqualTo(f.contrato().fim());
            assertThat(em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:id",Long.class)
                    .setParameter("id",vinculoId).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    private Preparado preparar() {
        return emTransacao(()->{
            try {
                Cenario c=criarCenario(); NovoContrato contrato=publicado(c,100,25,false);
                LocalDate zero=RelogioSistema.hoje().plusDays(10);
                Map<String,Object> reducao=aditamento("ZERAMENTO-ANTES-DO-CADASTRO",zero);
                reducao.put("novoEfetivoTotal",75);
                reducao.put("alteracoesComposicao",List.of(mapa("composicaoId",contrato.composicao1(),"quantidadeResultante",0)));
                registrarAditamento(c,contrato.id(),reducao);
                assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id",Long.class)
                        .setParameter("id",contrato.id()).getSingleResult()).isZero();
                return new Preparado(c,contrato,zero);
            } catch (Exception e) { throw new IllegalStateException("Preparação HTTP do contrato e do zeramento futuro falhou",e); }
        });
    }
    private Map<String,Object> cadastro(Preparado f,String cpf,LocalDate fim) {
        Map<String,Object> req=mapa("pessoa",mapa("nome","Pessoa de composição com redução futura","cpf",cpf),
                "contratoId",f.contrato().id(),"composicaoId",f.contrato().composicao1(),"dataInicio",f.zeramento().minusDays(2));
        if(fim!=null)req.put("dataFim",fim);
        return req;
    }
    private int capacidadeNoZeramento(Preparado f) throws Exception {
        JsonNode composicoes=consultar(f.cenario().localA,base(f.contrato().id())+"/composicoes?data="+f.zeramento(),
                "Programação futura de capacidade continua preservada");
        for(JsonNode item:composicoes)if(item.path("id").asLong()==f.contrato().composicao1())return item.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição histórica não retornada");
    }
    private record Preparado(Cenario cenario,NovoContrato contrato,LocalDate zeramento) {}
}
