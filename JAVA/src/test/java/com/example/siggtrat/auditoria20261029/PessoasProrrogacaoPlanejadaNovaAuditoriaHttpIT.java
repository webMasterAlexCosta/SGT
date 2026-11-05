package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Complementos de continuidade planejada: executar RED antes da implementação correspondente. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior() throws Exception {
        Preparado f = preparar();
        JsonNode pessoa = cadastrar(f, 0, null, null);
        long vinculoId = pessoa.path("id").asLong();
        LocalDate mudancaProgramada = RelogioSistema.hoje().plusDays(10);
        JsonNode sucessora = mudar(f, vinculoId, mudancaProgramada, f.contrato().composicao2());
        long sucessoraId = sucessora.path("id").asLong();
        Instant conhecimentoAnterior = Instant.now();
        LocalDate novoFim = f.contrato().fim().plusDays(30);

        prorrogar(f, f.contrato(), "FUTURA-CONTINUIDADE", RelogioSistema.hoje().plusDays(5), novoFim);
        assertThat(emTransacao(() -> em.find(Contrato.class, f.contrato().id()).getDataFim())).isEqualTo(f.contrato().fim());
        JsonNode depois = posicao(f, vinculoId, f.contrato().fim().plusDays(1));
        assertThat(depois.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(depois.path("alocacaoId").asLong()).isEqualTo(sucessoraId);
        assertThat(depois.path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao2());
        assertThat(depois.path("dataFim").asText()).isEqualTo(novoFim.toString());
        JsonNode corte = consultar(f.cenario().localA, V + "/" + vinculoId + "/alocacoes/posicao?data="
                        + f.contrato().fim().plusDays(1) + "&conhecidoAte=" + conhecimentoAnterior,
                "AUD003 o conhecimento anterior ao aditamento mantém o fim antigo da alocação");
        assertThat(corte.path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");

        LocalDate posterior = f.contrato().fim().plusDays(2);
        JsonNode nova = mudar(f, vinculoId, posterior, f.contrato().composicao1());
        assertThat(nova.path("dataFim").asText()).isEqualTo(novoFim.toString());
        assertThat(posicao(f, vinculoId, posterior).path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao1());
        assertThat(posicao(f, vinculoId, mudancaProgramada).path("alocacaoId").asLong()).isEqualTo(sucessoraId);
    }

    @Test
    void cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano() throws Exception {
        Preparado f = preparar();
        LocalDate novoFim = f.contrato().fim().plusDays(30);
        prorrogar(f, f.contrato(), "FUTURA-ANTES-CADASTRO", RelogioSistema.hoje().plusDays(5), novoFim);
        JsonNode aberto = cadastrar(f, 0, null, null);
        long abertoId = aberto.path("id").asLong();
        JsonNode estendido = posicao(f, abertoId, f.contrato().fim().plusDays(1));
        assertThat(estendido.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(estendido.path("dataFim").asText()).isEqualTo(novoFim.toString());
        assertThat(aberto.path("dataFim").isNull()).isTrue();
        assertThat(emTransacao(() -> em.find(Contrato.class, f.contrato().id()).getDataFim())).isEqualTo(f.contrato().fim());

        JsonNode limitado = cadastrar(f, 1, null, f.contrato().fim());
        long limitadoId = limitado.path("id").asLong();
        assertThat(posicao(f, limitadoId, f.contrato().fim()).path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao(f, limitadoId, f.contrato().fim().plusDays(1)).path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, limitadoId).getDataFim())).isEqualTo(f.contrato().fim());
    }

    @Test
    void prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa() throws Exception {
        Preparado f = preparar();
        JsonNode encerravel = cadastrar(f, 0, null, f.contrato().fim());
        long pessoaId = encerravel.path("pessoaId").asLong();
        long anteriorId = encerravel.path("id").asLong();
        NovoContrato segundo = emTransacao(() -> {
            try { return publicado(f.cenario(), 2, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        prorrogar(f, segundo, "DESTINO-DA-RECONTRATACAO", RelogioSistema.hoje(), segundo.fim().plusDays(60));
        JsonNode recontratacao = enviar(f.cenario().localA, V,
                mapa("pessoaId", pessoaId, "contratoId", segundo.id(), "dataInicio", f.contrato().fim().plusDays(1),
                        "composicaoId", segundo.composicao1()), 201,
                "VIN004 recontratação em contrato distinto no dia seguinte ao término humano");
        long novaId = recontratacao.path("id").asLong();
        prorrogar(f, f.contrato(), "ORIGEM-NAO-REABRE", RelogioSistema.hoje().plusDays(5), f.contrato().fim().plusDays(30));

        assertThat(emTransacao(() -> em.find(VinculoContratual.class, anteriorId).getDataFim())).isEqualTo(f.contrato().fim());
        assertThat(posicao(f, anteriorId, f.contrato().fim().plusDays(1)).path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        assertThat(posicao(f, novaId, f.contrato().fim().plusDays(1)).path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(emTransacao(() -> em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                .setParameter("v", anteriorId).getSingleResult().getDataFim())).isEqualTo(f.contrato().fim());
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 4, 2, false));
            } catch (Exception e) { throw new IllegalStateException("Falha na preparação da cobertura planejada", e); }
        });
    }

    private JsonNode cadastrar(Preparado f, int ordinal, Long pessoaId, LocalDate fim) throws Exception {
        Map<String, Object> req = mapa("contratoId", f.contrato().id(), "dataInicio", RelogioSistema.hoje(),
                "composicaoId", f.contrato().composicao1());
        if (pessoaId != null) req.put("pessoaId", pessoaId);
        else req.put("pessoa", mapa("nome", "Pessoa cobertura planejada " + ordinal,
                "cpf", cpf(Math.toIntExact(920000000L + f.contrato().id() * 10L + ordinal))));
        if (fim != null) req.put("dataFim", fim);
        return enviar(f.cenario().localA, V, req, 201, "VIN001 cadastro integrado após conhecimento dos fatos contratuais");
    }

    private JsonNode mudar(Preparado f, long vinculo, LocalDate data, long composicao) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "composicaoId", composicao,
                "justificativa", "Mudança válida na cobertura planejada", "evidencia", "SEI-COBERTURA-PLANEJADA",
                "autorizacao", "ATO-COBERTURA-PLANEJADA", "confirmarImpacto", true);
        JsonNode previa = enviar(f.cenario().localA, V + "/" + vinculo + "/previas/alocacoes", req, 200,
                "ALO007 prévia usa cobertura contratual planejada conhecida");
        req.put("tokenPrevia", previa.path("token").asText());
        return enviar(f.cenario().localA, V + "/" + vinculo + "/alocacoes", req, 200,
                "ALO003 mudança preserva sucessoras e cobertura futura válida");
    }

    private void prorrogar(Preparado f, NovoContrato contrato, String identificacao, LocalDate efetiva, LocalDate fim) throws Exception {
        Map<String, Object> req = aditamento(identificacao, efetiva);
        req.put("dataDocumento", RelogioSistema.hoje()); req.put("novaDataFim", fim);
        JsonNode previa = enviar(f.cenario().localA, base(contrato.id()) + "/aditamentos/previa", req, 200,
                "TMP015 prévia da prorrogação conhecida sem antecipar projeção de hoje");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.cenario().localA, base(contrato.id()) + "/aditamentos", req, 201,
                "TMP015 prorrogação tempestiva documentada");
    }

    private JsonNode posicao(Preparado f, long vinculo, LocalDate data) throws Exception {
        return consultar(f.cenario().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + data,
                "ALO007 cobertura temporal da alocação");
    }

    private record Preparado(Cenario cenario, NovoContrato contrato) {}
}
