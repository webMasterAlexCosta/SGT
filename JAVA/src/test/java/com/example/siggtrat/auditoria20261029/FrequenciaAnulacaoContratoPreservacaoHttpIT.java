package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;

import com.example.siggtrat.entidade.EventoVinculoContratual;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/** CON-018/FRE-017: prova especifica de anulacao, sem inferir o resultado da inatividade. */
class FrequenciaAnulacaoContratoPreservacaoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void anulacaoPreservaFrequenciaFechadaEProvaOriginalMasRetiraCalculoEGeraPendencia() throws Exception {
        Cenario c = criarCenario();
        // Este cenário conhece a admissão original; a fixture JPA genérica não
        // fornece fonte nominal e deve continuar servindo aos controles de ausência.
        String nomeDocumentado = c.pessoaA.getNome();
        declararFonteNominalSintetica(c, "FRE-ANULACAO-PRESERVACAO");
        emTransacao(() -> {
            em.flush(); em.clear();
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(vinculo.getDataInicio()).isEqualTo(INICIO);
            assertThat(vinculo.getDataFim()).isNull();
            assertThat(DATA).isAfter(INICIO).isBefore(com.example.siggtrat.utilitario.RelogioSistema.hoje());
            var fontes = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v "
                            + "and e.dataEfetiva<=:dia order by e.registradoEm,e.id", EventoVinculoContratual.class)
                    .setParameter("v", vinculo.getId()).setParameter("dia", DATA.minusDays(1)).getResultList();
            assertThat(fontes).hasSize(1);
            EventoVinculoContratual fonte = fontes.get(0);
            assertThat(fonte.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
            assertThat(fonte.getDataEfetiva()).isEqualTo(INICIO);
            assertThat(fonte.getRegistradoEm()).isEqualTo(INICIO.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            assertThat(fonte.getPessoaNomeFotografia()).isEqualTo(nomeDocumentado);
            assertThat(fonte.getPessoaCpfFotografia()).isEqualTo(vinculo.getPessoa().getCpf());
            assertThat(fonte.getInicioNovo()).isEqualTo(vinculo.getDataInicio());
            assertThat(fonte.getGestaoLocalIdFotografia()).isEqualTo(c.gestaoA.getId());
            assertThat(fonte.getResponsavelId()).isEqualTo(c.localA.getId());
            assertThat(fonte.getEvidencia()).isEqualTo("SEI-FIXTURE-FRE-ANULACAO-PRESERVACAO");
            assertThat(fonte.getAutorizacao()).isEqualTo("ATO-FIXTURE-FRE-ANULACAO-PRESERVACAO");
            assertThat(fonte.getHashEvento()).isNotBlank();
            return null;
        });
        MotivoDesligamento motivo = new MotivoDesligamento();
        motivo.setGestaoLocal(em.getReference(com.example.siggtrat.entidade.GestaoLocal.class, c.gestaoA.getId()));
        motivo.setDescricao("Anulacao documentada com preservacao da frequencia original");
        motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO);
        em.persist(motivo); em.flush();
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa de anulação anterior à frequência preservada");
        em.persist(origem); em.flush();
        // Disponibilidade não comprova descrição/categoria no passado.
        // A fonte de contexto precede os controles e as emissões deste cenário.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(),
                c.gestaoA.getId(), c.localA,
                "Anulacao documentada com preservacao da frequencia original",
                CategoriaTerminoVinculo.ANULACAO_CONTRATO, DATA, DATA,
                com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                "SEI-SINTETICO-FREQUENCIA-ANULACAO-PRESERVACAO");

        long id = registrar(c, DATA);
        long controleId = registrar(c, DATA.minusDays(1));
        em.flush(); em.clear();
        Frequencia original = em.find(Frequencia.class, id);
        assertEquals("FECHADA", original.getEstadoNegocial().name());
        assertEquals("VALIDA", original.getValidadeOperacional().name());
        Long autor = original.getAutorOriginalId();
        JsonNode eventosOriginais = consultar(c.localA, "/api/frequencias/" + id + "/eventos",
                "Cadeia original da frequencia antes do ato anulatorio");
        JsonNode emitida = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro(c, DATA), 201,
                "Emissao anterior a anulacao contem uma falta valida");
        assertEquals(1, emitida.path("conteudo").path("totais").path("faltas").asInt());
        assertFalse(emitida.path("metadados").path("sujeitoARevisao").asBoolean());
        long emissaoId = emitida.path("metadados").path("id").asLong();
        JsonNode controle = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro(c, DATA.minusDays(1)), 201,
                "Controle anterior ao alcance da anulacao nao deve perder validade");

        Map<String,Object> ato = mapa("dataEfetiva", DATA, "dataAto", RelogioSistema.hoje(),
                "motivo", "Ato formal anulou a cobertura desde a data da jornada", "referenciaSei", "SEI-ANULACAO-FREQUENCIA");
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/anular", ato, 200,
                "Previa real identifica o alcance contratual antes da confirmacao");
        ato.put("tokenPrevia", previa.path("tokenPrevia").asText());
        ato.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/anular", ato, 200,
                "Confirmacao real da anulacao retira cobertura sem apagar a prova da jornada");
        em.flush(); em.clear();

        Frequencia afetada = em.find(Frequencia.class, id);
        assertNotNull(afetada, "Anulacao nao exclui fisicamente a frequencia historica");
        assertEquals("FALTA", afetada.getStatus());
        assertEquals("FECHADA", afetada.getEstadoNegocial().name());
        assertEquals(autor, afetada.getAutorOriginalId());
        assertEquals(c.vinculoA.getId(), afetada.getVinculoContratual().getId());
        assertEquals("INVALIDA_COBERTURA", afetada.getValidadeOperacional().name());
        assertEquals("SUJEITA_REVISAO", afetada.getEstadoAuditoria().name());
        assertTrue(Boolean.TRUE.equals(afetada.getBloqueadaEdicaoOperacional()));
        assertNotNull(afetada.getMotivoRevisao());
        assertFalse(afetada.getMotivoRevisao().isBlank());
        assertEquals("VALIDA", em.find(Frequencia.class, controleId).getValidadeOperacional().name());

        JsonNode eventosDepois = consultar(c.localA, "/api/frequencias/" + id + "/eventos",
                "Historico da frequencia permanece acessivel apos anular sua cobertura");
        assertTrue(eventosDepois.size() > eventosOriginais.size());
        assertEquals(eventosOriginais.get(0), eventosDepois.get(0));
        assertTrue(eventosDepois.toString().contains("IMPACTO_TEMPORAL"));
        JsonNode historica = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "Emissao anterior continua sendo prova historica integra");
        assertEquals("SUPERADA", historica.path("metadados").path("estado").asText());
        assertEquals(emitida.path("conteudo"), historica.path("conteudo"));
        assertEquals(emitida.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo"));
        JsonNode atual = enviar(c.localA, "/api/relatorios/administrativos/previa", filtro(c, DATA), 200,
                "Visao operacional exclui a falta sem cobertura e conserva aviso da revisao pendente");
        assertEquals(0, atual.path("conteudo").path("registros").size());
        assertEquals(0, atual.path("conteudo").path("totais").path("faltas").asInt());
        assertEquals(0, atual.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        assertTrue(atual.path("metadados").path("sujeitoARevisao").asBoolean());
        assertEquals(1, atual.path("metadados").path("quantidadePendencias").asLong());
        JsonNode controleDepois = consultar(c.localA, "/api/relatorios/administrativos/emissoes/"
                        + controle.path("metadados").path("id").asLong(),
                "Periodo anterior ao alcance continua vigente");
        assertEquals("VIGENTE", controleDepois.path("metadados").path("estado").asText());
        assertEquals(controle.path("conteudo"), controleDepois.path("conteudo"));
    }

    private long registrar(Cenario c, LocalDate data) throws Exception {
        return enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data, "status", "FALTA", "justificativaRetificacao", "Conferencia documental da jornada",
                "chaveIdempotencia", "frequencia-anulacao-" + UUID.randomUUID()), 200,
                "Frequencia real registrada antes de conhecer a anulacao").path("id").asLong();
    }

    private Map<String,Object> filtro(Cenario c, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", data, "fim", data,
                "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", "emissao-anulacao-" + UUID.randomUUID());
    }
}
