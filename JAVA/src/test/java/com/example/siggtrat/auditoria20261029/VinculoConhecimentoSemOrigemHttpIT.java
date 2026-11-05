package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.MotivoDesligamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import com.example.siggtrat.utilitario.RelogioSistema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** TMP004/005: a projeção mutável não completa dimensões ausentes de uma origem legada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VinculoConhecimentoSemOrigemHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"INICIO", "FIM"})
    void primeiroEventoConhecidoProvaEstadoAnteriorSemHerdarLimiteModificadoDepois(String dimensao) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate primeiraData = RelogioSistema.hoje();
        exigirAusenciaDeCriacaoNominal(c);
        String raiz = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        enviar(c.localA, raiz + "/alterar-matricula", mapa("dataEfetiva", primeiraData, "novaMatricula", "PRIMEIRA-PROVA-" + c.vinculoA.getId(),
                "justificativa", "Primeiro evento preserva o estado do vínculo legado", "evidencia", "SEI-ORIGEM-CONHECIDA",
                "autorizacao", "ATO-ORIGEM-CONHECIDA"), 200, "TMP005 evento contemporâneo documenta o cadastro conhecido hoje, sem criar origem nominal antiga");
        exigirAusenciaDeCriacaoNominal(c);
        var primeiraFonte = consultar(c.localA, raiz + "/eventos", "Primeira fonte é matrícula, sem evento CRIACAO");
        assertThat(primeiraFonte).hasSize(1);
        assertThat(primeiraFonte.get(0).path("tipo").asText()).isEqualTo("ALTERACAO_MATRICULA");
        assertThat(primeiraFonte.get(0).path("dataEfetiva").asText()).isEqualTo(primeiraData.toString());
        Instant corte = Instant.now();
        if (dimensao.equals("INICIO")) {
            var req = mapa("novoInicio", primeiraData, "justificativa", "Correção recebida após a primeira prova",
                    "evidencia", "SEI-LIMITE-POSTERIOR", "autorizacao", "ATO-LIMITE-POSTERIOR");
            var previa = enviar(c.localA, raiz + "/previas/retificacao-inicio", req, 200, "VIN014 prévia do novo início");
            req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
            enviar(c.localA, raiz + "/retificar-inicio", req, 200, "VIN014 retificação posterior conhecida");
        } else {
            Long motivo = emTransacao(() -> {
                MotivoDesligamento m = new MotivoDesligamento(); m.setDescricao("Fim posterior " + c.vinculoA.getId());
                m.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                m.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId())); em.persist(m); em.flush();
                EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
                origem.setDataEfetiva(INICIO); origem.setAtivo(true); origem.setMotivo("Fonte sintética explícita da causa desde o início");
                em.persist(origem); return m.getId();
            });
            enviar(c.localA, raiz + "/desligar", mapa("dataFim", primeiraData.plusDays(5), "motivoDesligamentoId", motivo,
                    "justificativa", "Término recebido depois do corte", "evidencia", "SEI-FIM-POSTERIOR",
                    "autorizacao", "ATO-FIM-POSTERIOR"), 200, "DES003 data real e conhecimento posteriores distintos");
        }
        var posicao = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId() + "/posicao?data="
                        + primeiraData.plusDays(6) + "&conhecidoAte=" + corte,
                "TMP004 primeira prova conhecida conserva todas as dimensões anteriores, não apenas o campo alterado");
        var vinculo = posicao.path("vinculos").get(0);
        assertThat(vinculo.path("dataInicio").asText()).isEqualTo(INICIO.toString());
        assertThat(vinculo.path("dataFim").isNull()).isTrue();
        assertThat(vinculo.path("matricula").asText()).isEqualTo("PRIMEIRA-PROVA-" + c.vinculoA.getId());
        assertThat(vinculo.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        exigirAusenciaDeCriacaoNominal(c);
    }

    @Test
    void corteAnteriorAPrimeiraFonteNaoCertificaProjecaoAtualComoEstadoHistorico() throws Exception {
        Cenario c = criarCenarioPersistido();
        exigirAusenciaDeCriacaoNominal(c);
        Instant corte = Instant.now();
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/alterar-matricula", mapa(
                "dataEfetiva", RelogioSistema.hoje(), "novaMatricula", "PROVA-POSTERIOR-" + c.vinculoA.getId(),
                "justificativa", "Primeiro documento posterior ao corte", "evidencia", "SEI-SEM-ORIGEM",
                "autorizacao", "ATO-SEM-ORIGEM"), 200, "TMP005 fonte contemporânea posterior ao corte não inventa origem nominal passada");
        exigirAusenciaDeCriacaoNominal(c);
        executar(c.localA, get("/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId() + "/posicao")
                        .param("data", DATA.toString()).param("conhecidoAte", corte.toString()), 409,
                "TMP004 antes da primeira prova, a posição legada exige fonte em vez de copiar a projeção atual");
        var conhecidoAgora = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId()
                        + "/posicao?data=" + RelogioSistema.hoje(),
                "Controle positivo: depois do primeiro documento contemporâneo, a posição conhecida é reconstruível");
        assertThat(conhecidoAgora.path("vinculos")).hasSize(1);
        assertThat(conhecidoAgora.path("vinculos").get(0).path("matricula").asText())
                .isEqualTo("PROVA-POSTERIOR-" + c.vinculoA.getId());
        exigirAusenciaDeCriacaoNominal(c);
    }

    private void exigirAusenciaDeCriacaoNominal(Cenario c) {
        assertThat(emTransacao(() -> em.createQuery("select count(e) from EventoVinculoContratual e "
                        + "where e.vinculo.id=:v and e.tipoEvento=:tipo", Long.class)
                .setParameter("v", c.vinculoA.getId())
                .setParameter("tipo", com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO)
                .getSingleResult())).as("O cenário deve continuar sem origem nominal/CRIACAO").isZero();
    }
}
