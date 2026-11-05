package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Casos que exigem reconstrução temporal completa, além do estado da entidade atual. */
class ContratosTemporalLacunasHttpIT extends ContratosHttpBase {
    @Test
    void remanejamentoNaoZeraComposicaoComVinculoProgramadoDepoisDoEfeito() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        vincular(c, n, RelogioSistema.hoje().plusDays(20));
        var r = remanejamento(n, "REM-ZERAR", RelogioSistema.hoje().plusDays(10), 25);
        var p = previaRemanejamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/remanejamentos", r, 409,
                "CMP-006/REM-004 programação futura deve ser tratada antes do zeramento");
        assertThat(p.path("possuiImpactoOcupacao").asBoolean()).isTrue();
    }

    @Test
    void novaFrequenciaInvalidaTokenDeAditamentoRetroativo() throws Exception {
        var c = criarCenario();
        var r = aditamento("ADI-ASSINATURA", DATA.minusDays(1)); r.put("novaDescricao", "Descrição retificada");
        previaAditamento(c, c.contratoA.getId(), r);
        frequencia(c, false, DATA, "FALTA"); em.flush();
        enviar(c.localA, base(c.contratoA.getId()) + "/aditamentos", r, 409,
                "AUD-004 nova frequência altera impacto da retificação retroativa");
    }

    @Test
    void novaFrequenciaInvalidaTokenDeRemanejamentoRetroativo() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        var v = vincular(c, n, n.inicio().plusDays(1));
        var r = remanejamento(n, "REM-ASSINATURA", n.inicio().plusDays(2), 5);
        previaRemanejamento(c, n.id(), r);
        frequencia(v, n.inicio().plusDays(3));
        enviar(c.localA, base(n.id()) + "/remanejamentos", r, 409,
                "AUD-004 nova frequência altera impacto do remanejamento retroativo");
    }

    @Test
    void chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        LocalDate hoje = RelogioSistema.hoje();
        var r = aditamento("ADI-PASSAGEM-TEMPO", hoje.plusDays(10)); r.put("novaDescricao", "Descrição programada vigente");
        r.put("novoEfetivoTotal", 110); r.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 35)));
        registrarAditamento(c, n.id(), r);
        em.flush(); em.clear();
        String fatosPersistidosAntes = fotografiaFatosContratuais(n.id());
        var antes = consultar(c.localA, base(n.id()), "CON-017 não antecipar o fato futuro");
        assertThat(antes.path("descricao").asText()).isNotEqualTo("Descrição programada vigente");
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(hoje.plusDays(11));
            var atual = consultar(c.localA, base(n.id()), "CON-005/TMP-017 GET comum deriva projeção pela data atual");
            assertThat(atual.path("descricao").asText()).isEqualTo("Descrição programada vigente");
            assertThat(atual.path("efetivoPrevisto").asInt()).isEqualTo(110);
            JsonNode naLista = null;
            for (var item : consultar(c.localA, "/api/contratos", "CON-005 lista também usa projeção vigente"))
                if (item.path("id").asLong() == n.id()) naLista = item;
            assertThat(naLista).isNotNull();
            assertThat(naLista.path("descricao").asText()).isEqualTo("Descrição programada vigente");
            em.flush(); em.clear();
            assertThat(fotografiaFatosContratuais(n.id())).as(
                    "TMP-006: GET e passagem do tempo não criam versões/eventos nem reescrevem os fatos armazenados")
                    .isEqualTo(fatosPersistidosAntes);
        }
    }

    @Test
    void publicacaoRetroativaExigePreviaMesmoComFonteEComposicaoCompletas() throws Exception {
        var c = criarCenario();
        long id = prepararRetroativo(c, "RETRO-SEM-PREVIA");
        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-RETRO", "justificativa", "Original registrado posteriormente"), 409,
                "CON-014 contrato original retroativo requer prévia de impactos");
        assertThat(consultar(c.localA, base(id), "CON-014 sem confirmação permanece rascunho").path("estadoNegocial").asText()).isEqualTo("RASCUNHO");
    }

    @Test
    void publicacaoRetroativaTemPreviaEAssinaDadosDePreparacao() throws Exception {
        var c = criarCenario(); long id = prepararRetroativo(c, "RETRO-COM-PREVIA");
        var req = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-RETRO", "justificativa", "Original comprovado");
        var p = enviar(c.localA, base(id) + "/previas/publicar", req, 200, "CON-014 prévia do original retroativo");
        assertThat(p.path("dataEfetiva").asText()).isEqualTo(RelogioSistema.hoje().minusDays(20).toString());
        req.put("tokenPrevia", p.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        var publico = enviar(c.localA, base(id) + "/publicar", req, 200, "CON-014 publicação original confirmada");
        assertThat(publico.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
    }

    @Test
    void correcaoMaterialCnpjTemFatoProprioPreservaCorteDeConhecimentoEIdempotencia() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var antes = consultar(c.localA, base(n.id()), "EMP-003 CNPJ digitado anteriormente");
        Instant corte = Instant.now();
        var req = mapa("cnpjCorrigido", "98765432000198", "dataEfetiva", n.inicio(), "dataAto", RelogioSistema.hoje(),
                "motivo", "Erro de digitação comprovado no cadastro original", "referenciaSei", "SEI-CNPJ-MATERIAL",
                "chaveIdempotencia", "CNPJ-" + n.id());
        enviar(c.nacional, base(n.id()) + "/correcoes-cnpj", req, 403, "EMP-003 somente Gestor responsável corrige CNPJ");
        enviar(c.localB, base(n.id()) + "/correcoes-cnpj", req, 404, "EMP-003 correção respeita Gestão responsável");
        var corrigida = enviar(c.localA, base(n.id()) + "/correcoes-cnpj", req, 201, "EMP-003 correção material é fato próprio");
        assertThat(corrigida.path("cnpjAnterior").asText()).isEqualTo(antes.path("cnpjEmpresa").asText());
        assertThat(corrigida.path("cnpjCorrigido").asText()).isEqualTo("98765432000198");
        assertThat(enviar(c.localA, base(n.id()) + "/correcoes-cnpj", req, 201, "AUD-005 mesma correção não duplica fatos")
                .path("id").asLong()).isEqualTo(corrigida.path("id").asLong());
        assertThat(consultar(c.localA, base(n.id()) + "/correcoes-cnpj", "EMP-003 trilha específica").size()).isEqualTo(1);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "EMP-003 erro material não vira troca real").size()).isZero();
        var atual = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio(), "EMP-003 original corrigido preserva data real");
        assertThat(atual.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo("98765432000198");
        var historico = dados(executar(c.localA, get(base(n.id()) + "/posicao-temporal").param("data", n.inicio().toString())
                .param("conhecidoAte", corte.toString()), 200, "EMP-003/HIS-006 corte anterior preserva CNPJ então conhecido"));
        assertThat(historico.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo(antes.path("cnpjEmpresa").asText());
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "EMP-003 evento de correção distinto de aditamento");
        assertThat(eventos.toString()).contains("CORRECAO_CNPJ");
    }

    private String fotografiaFatosContratuais(long contratoId) throws Exception {
        var foto = mapa();
        foto.put("contrato", em.createQuery("select c.id,c.descricao,c.efetivoPrevisto,c.dataInicio,c.dataFim,c.estadoNegocial,c.ativo,c.version from Contrato c where c.id=:id")
                .setParameter("id", contratoId).getResultList());
        foto.put("eventos", em.createQuery("select e.id,e.fatoLogicoId,e.versaoNegocial,e.tipoEvento,e.chaveNegocial,e.situacaoFato,e.dataEfetiva,e.dataAto,e.registradoEm,e.usuarioResponsavelId,e.motivo,e.referenciaSei,e.valorAnterior,e.valorNovo from EventoCicloVidaContrato e where e.contrato.id=:id order by e.id")
                .setParameter("id", contratoId).getResultList());
        foto.put("versoes", em.createQuery("select v.id,v.numeroVersao,v.dataEfetiva,v.registradoEm,v.descricao,v.efetivoPrevisto,v.dataInicio,v.dataFim,v.gestaoLocalId,v.tipoContratacaoId,v.cnpjEmpresa,v.razaoSocialEmpresa,v.habilitacaoAdministrativa from VersaoContratoNegocial v where v.contrato.id=:id order by v.id")
                .setParameter("id", contratoId).getResultList());
        return json(foto);
    }

    private long prepararRetroativo(Cenario c, String numero) throws Exception {
        var d = enviar(c.localA, "/api/contratos", rascunho(c, numero, 5, RelogioSistema.hoje().minusDays(20), RelogioSistema.hoje().plusDays(40)),
                201, "CON-014 original retroativo em preparação");
        long id = d.path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "CON-014 composição completa");
        return id;
    }

    private VinculoContratual vincular(Cenario c, NovoContrato n, LocalDate inicio) throws Exception {
        long id = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa temporal", "cpf", "52998224725"),
                "contratoId", n.id(), "dataInicio", inicio, "composicaoId", n.composicao1()), 201, "programação documental de referência").path("id").asLong();
        return em.find(VinculoContratual.class, id);
    }

    private void frequencia(VinculoContratual v, LocalDate dia) {
        var a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id", AlocacaoVinculo.class)
                .setParameter("id", v.getId()).getSingleResult();
        var e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:id", Empregado.class)
                .setParameter("id", v.getPessoa().getId()).getSingleResult();
        Frequencia f = new Frequencia(); f.setEmpregado(e); f.setPessoa(v.getPessoa()); f.setVinculoContratual(v); f.setAlocacaoVinculo(a);
        f.setContratoSnapshotId(v.getContrato().getId()); f.setDataReferencia(dia); f.setStatus("FALTA"); em.persist(f); em.flush();
    }
}
