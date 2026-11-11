package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.MotivoDesligamento;
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

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasProjecaoTransferenciaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";

    @Test
    void transferenciaNaoMoveProjecaoDeVinculoEncerradoAntesDaDataEfetiva() throws Exception {
        Preparado f = preparar();
        JsonNode criado = criar(f, f.contrato(), null, RelogioSistema.hoje().minusDays(20));
        long id = criado.path("id").asLong();
        LocalDate fim = RelogioSistema.hoje().minusDays(10);
        emTransacao(() -> {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.motivoId(), f.c().gestaoA.getId(), f.c().localA,
                    "Desligamento individual anterior à transferência da projeção", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    fim, fim, java.time.Instant.now(), "SEI-SINTETICO-C5-PES-PROJECAO-ENCERRADA");
            return null;
        });
        desligar(f, id, fim);
        JsonNode eventos = consultar(f.c().localA, V + "/" + id + "/eventos", "Trilha humana anterior à transferência");
        transferir(f, RelogioSistema.hoje().minusDays(5));

        emTransacao(() -> {
            Empregado e = empregado(criado.path("pessoaId").asLong());
            assertThat(e.getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
            assertThat(e.getLotacao().getId()).isEqualTo(f.c().lotacaoA.getId());
            assertThat(e.getJornada().getId()).isEqualTo(f.c().jornadaA.getId());
            assertThat(e.getCargo().getId()).isEqualTo(f.c().cargoA.getId());
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(ConstantesDominio.STATUS_INATIVO);
            assertThat(e.getContratos()).isEmpty();
            assertThat(em.find(VinculoContratual.class, id).getDataFim()).isEqualTo(fim);
            // A próxima gravação da projeção deve continuar cumprindo a
            // integridade Gestão + Lotação + Turno + contrato atual.
            e.setRg("CONTROLE-ESCRITA-PROJECAO"); em.flush();
            return null;
        });
        assertThat(consultar(f.c().localB, V + "/" + id + "/eventos", "Transferência não inventa novo desligamento"))
                .isEqualTo(eventos);
    }

    @Test
    void transferenciaFuturaNaoAntecipaContextoMaterializadoNemImpedeEscritaNaOrigem() throws Exception {
        Preparado f = preparar();
        JsonNode criado = criar(f, f.contrato(), null, RelogioSistema.hoje().minusDays(5));
        transferir(f, RelogioSistema.hoje().plusDays(5));
        emTransacao(() -> {
            Empregado e = empregado(criado.path("pessoaId").asLong());
            assertThat(e.getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
            assertThat(e.getLotacao().getId()).isEqualTo(f.c().lotacaoA.getId());
            assertThat(e.getJornada().getId()).isEqualTo(f.c().jornadaA.getId());
            assertThat(e.getCargo().getId()).isEqualTo(f.c().cargoA.getId());
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(ConstantesDominio.STATUS_ATIVO);
            assertThat(e.getContratos()).extracting("id").containsExactly(f.contrato().id());
            assertThat(e.getContratos().get(0).getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
            e.setRg("CONTROLE-ESCRITA-PROJECAO"); em.flush();
            return null;
        });
    }

    @Test
    void transferenciaDeVinculoProgramadoPreservaProjecaoDoOutroContratoVigente() throws Exception {
        Preparado f = preparar();
        NovoContrato corrente = emTransacao(() -> {
            try { return publicado(f.c(), 4, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        JsonNode atual = criar(f, corrente, null, RelogioSistema.hoje().minusDays(5));
        desligar(f, atual.path("id").asLong(), RelogioSistema.hoje().plusDays(4));
        criar(f, f.contrato(), atual.path("pessoaId").asLong(), RelogioSistema.hoje().plusDays(10));
        transferir(f, RelogioSistema.hoje().minusDays(2));

        emTransacao(() -> {
            Empregado e = empregado(atual.path("pessoaId").asLong());
            assertThat(e.getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
            assertThat(e.getLotacao().getId()).isEqualTo(f.c().lotacaoA.getId());
            assertThat(e.getJornada().getId()).isEqualTo(f.c().jornadaA.getId());
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(ConstantesDominio.STATUS_ATIVO);
            assertThat(e.getContratos()).extracting("id").containsExactly(corrente.id());
            e.setRg("CONTROLE-ESCRITA-PROJECAO"); em.flush();
            return null;
        });
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                NovoContrato contrato = publicado(c, 4, null, false);
                MotivoDesligamento motivo = new MotivoDesligamento();
                motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Desligamento para projeção " + contrato.id());
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(motivo); em.flush();
                var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
                origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(contrato.inicio()); origem.setAtivo(true);
                origem.setMotivo("Fonte sintética explícita da causa individual no período anterior à transferência");
                em.persist(origem); em.flush();
                return new Preparado(c, contrato, motivo.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private JsonNode criar(Preparado f, NovoContrato contrato, Long pessoaId, LocalDate inicio) throws Exception {
        Map<String, Object> req = mapa("contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", inicio);
        if (pessoaId == null) {
            int seed = Math.toIntExact(727000000L + contrato.id());
            req.put("pessoa", mapa("nome", "Pessoa projeção " + seed, "cpf", cpf(seed)));
        } else req.put("pessoaId", pessoaId);
        return enviar(f.c().localA, V, req, 201, "Preparação: cadastro integrado com projeção canônica única");
    }

    private void desligar(Preparado f, long vinculoId, LocalDate fim) throws Exception {
        enviar(f.c().localA, V + "/" + vinculoId + "/desligar", mapa("dataFim", fim,
                "motivoDesligamentoId", f.motivoId(), "justificativa", "Término documentado antes da transferência",
                "evidencia", "SEI-PROJECAO-TRANSFERENCIA", "autorizacao", "ATO-PROJECAO-TRANSFERENCIA"), 200,
                "DES002 vínculo se encerra na data humana, preservando histórico");
    }

    private void transferir(Preparado f, LocalDate data) throws Exception {
        conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(),
                "tipoContratacaoDestinoId", f.c().tipoB.getId(), "motivo", "Transferência com contexto operacional coerente",
                "referenciaSei", "SEI-PROJECAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(),
                        "lotacaoDestinoId", f.c().lotacaoB.getId(), "jornadaDestinoId", f.c().jornadaB.getId(),
                        "cargoDestinoId", f.c().cargoB.getId()))), 200,
                "CON023 transferência conserva passado e a projeção operacional válida hoje");
    }

    private Empregado empregado(long pessoaId) {
        return em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:pessoa", Empregado.class)
                .setParameter("pessoa", pessoaId).getSingleResult();
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoId) {}
}
