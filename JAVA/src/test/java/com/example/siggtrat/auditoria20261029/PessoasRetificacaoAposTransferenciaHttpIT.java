package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasRetificacaoAposTransferenciaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";

    @Test
    void ampliarTerminoAlemDaTransferenciaUsaMapeamentoDocumentadoSemRecontratar() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 4, null, false);
                MotivoDesligamento a = new MotivoDesligamento(), b = new MotivoDesligamento();
                a.setGestaoLocal(c.gestaoA); a.setDescricao("Término origem " + contrato.id());
                b.setGestaoLocal(c.gestaoB); b.setDescricao("Término destino " + contrato.id());
                a.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                b.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(a); em.persist(b); em.flush();
                origemMotivos(contrato, a, b);
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, contrato.id(), a.getId(), c.gestaoA.getId(), c.localA,
                        "Término individual documentado na origem antes da transferência", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-TERMINO-ORIGEM-MAPA");
                return new Preparado(c, contrato, a.getId(), b.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        int seed = Math.toIntExact(728000000L + f.contrato().id());
        JsonNode criado = enviar(f.c().localA, V, mapa("pessoa", mapa("nome", "Pessoa término atravessa transferência " + seed,
                        "cpf", cpf(seed)), "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", RelogioSistema.hoje().minusDays(20)), 201, "Preparação: vínculo iniciado na Gestão A");
        long id = criado.path("id").asLong();
        LocalDate fimOriginal = RelogioSistema.hoje().minusDays(10);
        Map<String, Object> termino = acao(); termino.put("dataFim", fimOriginal); termino.put("motivoDesligamentoId", f.motivoA());
        enviar(f.c().localA, V + "/" + id + "/desligar", termino, 200, "DES003 término original precede a transferência");
        LocalDate transferencia = RelogioSistema.hoje().minusDays(5);
        conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", transferencia,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                "motivo", "Transferência posterior ao término registrado", "referenciaSei", "SEI-EXTENSAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                        "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId()))), 200,
                "CON023 composição posterior e mapeamento A→B documentados sem reabrir o vínculo encerrado");
        assertThat(consultar(f.c().localB, V + "/" + id + "/alocacoes/posicao?data=" + transferencia,
                "Antes da correção não há cobertura posterior ao término").path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        String nomeDestinoDocumentado = f.c().lotacaoB.getNome();
        emTransacao(() -> {
            em.find(Lotacao.class, f.c().lotacaoB.getId()).setNome("Nome posterior da lotação " + f.contrato().id());
            em.find(Jornada.class, f.c().jornadaB.getId()).setDescricao("Nome posterior do turno " + f.contrato().id());
            em.find(Cargo.class, f.c().cargoB.getId()).setNome("Nome posterior do cargo " + f.contrato().id());
            return null;
        });

        LocalDate novoFim = RelogioSistema.hoje().plusDays(3);
        Map<String, Object> retificar = acao(); retificar.put("dataFim", novoFim); retificar.put("motivoDesligamentoId", f.motivoB());
        JsonNode previa = enviar(f.c().localB, V + "/" + id + "/previas/retificacao-desligamento", retificar, 200,
                "DES005 prévia revalida a extensão nas composições anteriores e posteriores à transferência");
        assertThat(previa.path("impactos").toString()).contains("mapeamento documental", transferencia.toString());
        retificar.put("confirmarImpacto", true); retificar.put("tokenPrevia", previa.path("token").asText());
        JsonNode corrigido = enviar(f.c().localB, V + "/" + id + "/retificar-desligamento", retificar, 200,
                "ALO007 DES005 extensão explícita usa o mapeamento já autorizado para manter cobertura contínua");
        assertThat(corrigido.path("id").asLong()).isEqualTo(id);
        assertThat(corrigido.path("pessoaId").asLong()).isEqualTo(criado.path("pessoaId").asLong());
        assertThat(corrigido.path("dataFim").asText()).isEqualTo(novoFim.toString());
        assertThat(consultar(f.c().localB, V + "/" + id + "/alocacoes/posicao?data=" + transferencia.minusDays(1),
                "Contexto anterior continua na Gestão A").path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        JsonNode destino = consultar(f.c().localB, V + "/" + id + "/alocacoes/posicao?data=" + transferencia,
                "A partir da transferência o contexto usa o destino documentado");
        assertThat(destino.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
        assertThat(destino.path("lotacaoNome").asText()).isEqualTo(nomeDestinoDocumentado);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getDataFim())).isEqualTo(novoFim);
        JsonNode eventos = consultar(f.c().localB, V + "/" + id + "/eventos", "Retificação preserva término original e decisão corretiva");
        assertThat(eventos.toString()).contains("DESLIGAMENTO", "RETIFICACAO_DESLIGAMENTO", fimOriginal.toString());
        JsonNode alocacoes = consultar(f.c().localB, V + "/" + id + "/alocacoes/eventos", "Sucessora possui origem na transferência documentada");
        List<JsonNode> transferidas = new java.util.ArrayList<>();
        alocacoes.forEach(e -> { if ("TRANSFERENCIA_GESTAO".equals(e.path("tipoEvento").asText())) transferidas.add(e); });
        assertThat(transferidas).hasSize(1);
        assertThat(transferidas.get(0).path("transferenciaGestaoId").asLong()).isPositive();
        assertThat(transferidas.get(0).path("evidencia").asText()).isEqualTo("SEI-RETIFICACAO-TRANSFERENCIA");
        assertThat(transferidas.get(0).path("responsavelUsername").asText()).isEqualTo(f.c().localB.getUsername());
        assertThat(transferidas.get(0).path("hashEvento").asText()).hasSize(64);
    }

    @Test
    void extensaoMapeadaRevalidaExcedenteDoDestinoAntesDeCriarSucessora() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 1, null, false);
                MotivoDesligamento a = new MotivoDesligamento(), b = new MotivoDesligamento();
                a.setGestaoLocal(c.gestaoA); a.setDescricao("Término origem excedente " + contrato.id());
                b.setGestaoLocal(c.gestaoB); b.setDescricao("Término destino excedente " + contrato.id());
                a.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                b.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(a); em.persist(b); em.flush();
                origemMotivos(contrato, a, b);
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, contrato.id(), a.getId(), c.gestaoA.getId(), c.localA,
                        "Término individual documentado na origem antes do excedente no destino", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-TERMINO-ORIGEM-EXCEDENTE");
                return new Preparado(c, contrato, a.getId(), b.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        int seed = Math.toIntExact(729000000L + f.contrato().id() * 2L);
        long id = enviar(f.c().localA, V, mapa("pessoa", mapa("nome", "Pessoa expansão com limite " + seed, "cpf", cpf(seed)),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", RelogioSistema.hoje().minusDays(20)), 201, "Preparação: vínculo anterior encerra antes de transferir").path("id").asLong();
        LocalDate fimOriginal = RelogioSistema.hoje().minusDays(10);
        Map<String, Object> fim = acao(); fim.put("dataFim", fimOriginal); fim.put("motivoDesligamentoId", f.motivoA());
        enviar(f.c().localA, V + "/" + id + "/desligar", fim, 200, "Término individual original");
        LocalDate transferencia = RelogioSistema.hoje().minusDays(5);
        conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", transferencia,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                "motivo", "Transferência com limite de uma vaga", "referenciaSei", "SEI-EXTENSAO-CAPACIDADE", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                        "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId()))), 200, "Composição transferida com uma vaga");
        enviar(f.c().localB, V, mapa("pessoa", mapa("nome", "Ocupante do destino " + seed, "cpf", cpf(seed + 1)),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(), "dataInicio", transferencia), 201,
                "Uma pessoa já ocupa a combinação mapeada no período acrescido");
        Map<String, Object> retificar = acao(); retificar.put("dataFim", RelogioSistema.hoje().plusDays(3));
        retificar.put("motivoDesligamentoId", f.motivoB());
        JsonNode previa = enviar(f.c().localB, V + "/" + id + "/previas/retificacao-desligamento", retificar, 200,
                "CMP009 prévia avalia a capacidade real da combinação de destino");
        assertThat(previa.path("impactos").toString()).contains("ocupação excedente", transferencia.toString());
        retificar.put("confirmarImpacto", true); retificar.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localB, V + "/" + id + "/retificar-desligamento", retificar, 409,
                "CMP009 transposição do mapeamento não dispensa consentimento de excedente");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getDataFim())).isEqualTo(fimOriginal);
        assertThat(consultar(f.c().localB, V + "/" + id + "/alocacoes/posicao?data=" + transferencia,
                "Negação não materializa sucessora nem cria cobertura").path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        retificar.put("confirmarInclusaoExcedente", true);
        JsonNode novaPrevia = enviar(f.c().localB, V + "/" + id + "/previas/retificacao-desligamento", retificar, 200,
                "Confirmação utiliza a prévia atualizada com consentimento específico");
        retificar.put("tokenPrevia", novaPrevia.path("token").asText());
        enviar(f.c().localB, V + "/" + id + "/retificar-desligamento", retificar, 200,
                "CMP010 excedente documentado permite a restauração da faixa mapeada");
        assertThat(consultar(f.c().localB, base(f.contrato().id()) + "/composicoes/excedentes?data=" + transferencia,
                "CMP011 consulta de destino conserva justificativa da extensão autorizada").toString())
                .contains("SEI-RETIFICACAO-TRANSFERENCIA", "ATO-RETIFICACAO-TRANSFERENCIA");
    }

    private void origemMotivos(NovoContrato contrato, MotivoDesligamento... motivos) {
        for (MotivoDesligamento motivo : motivos) {
            var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(contrato.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa individual anterior à transferência do cenário");
            em.persist(origem);
        }
        em.flush();
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Correção comprovada da data real de término", "evidencia", "SEI-RETIFICACAO-TRANSFERENCIA",
                "autorizacao", "ATO-RETIFICACAO-TRANSFERENCIA");
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoA, long motivoB) {}
}
