package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.FotografiaJornada;
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
class PessoasFotografiaTransferenciaRestauradaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void sucessoraRestauradaUsaMunicipioEGradeDocumentadosNaTransferencia() throws Exception {
        exercitar(false, false, preparado -> {
            emTransacao(() -> {
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, preparado.contrato().id(), preparado.motivoA(), preparado.c().gestaoA.getId(), preparado.c().localA,
                        "Término individual documentado para restauração de município e grade", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-FOTO-MUNICIPIO-GRADE");
                return null;
            });
        });
    }

    @Test
    void mapaLegadoSemFotografiaBloqueiaReconstrucaoSemInventarGradeDoCadastroAtual() throws Exception {
        exercitar(true, false, preparado -> {
            emTransacao(() -> {
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, preparado.contrato().id(), preparado.motivoA(), preparado.c().gestaoA.getId(), preparado.c().localA,
                        "Término individual documentado antes do mapa sem grade comprovada", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-FOTO-GRADE-AUSENTE");
                return null;
            });
        });
    }

    @Test
    void restauracaoHttpMantemRotulosAusentesDoMapaSemPerderGradeDocumentada() throws Exception {
        exercitar(false, true, preparado -> {
            emTransacao(() -> {
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, preparado.contrato().id(), preparado.motivoA(), preparado.c().gestaoA.getId(), preparado.c().localA,
                        "Término individual documentado antes do mapa com rótulos ausentes", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-FOTO-ROTULOS-AUSENTES");
                return null;
            });
        });
    }

    @Test
    void persistenciaDeCopiaDocumentalComRotulosNulosNaoCapturaMestresAtuais() throws Exception {
        Preparado f = exercitar(false, false, preparado -> {
            emTransacao(() -> {
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, preparado.contrato().id(), preparado.motivoA(), preparado.c().gestaoA.getId(), preparado.c().localA,
                        "Término individual documentado antes da fotografia restaurada", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(10), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-FOTO-COPIA-DOCUMENTAL");
                return null;
            });
        });
        long alocacaoId = emTransacao(() -> {
            MapeamentoTransferenciaGestao mapa = em.createQuery("select m from MapeamentoTransferenciaGestao m "
                            + "where m.transferencia.contrato.id=:id", MapeamentoTransferenciaGestao.class)
                    .setParameter("id", f.contrato().id()).getSingleResult();
            // O banco original exige texto no destino do mapa. Para alcançar o
            // callback com null sem mudar schema ou documento persistido, a cópia
            // é destacada e declara ausência nos rótulos; grade e município ficam
            // exatamente como foram comprovados no mapa real.
            em.detach(mapa);
            mapa.setLotacaoDestinoNomeFotografia(null);
            mapa.setJornadaDestinoDescricaoFotografia(null);
            mapa.setCargoDestinoNomeFotografia(null);
            Pessoa p = pessoa("Pessoa da cópia documental " + f.contrato().id(), cpf(Math.toIntExact(733000000L + f.contrato().id())));
            VinculoContratual v = vinculo(p, em.find(Contrato.class, f.contrato().id()));
            v.setDataInicio(RelogioSistema.hoje());
            AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(v);
            a.setComposicao(em.find(ComposicaoContratual.class, f.contrato().composicao1()));
            a.setLotacao(em.find(Lotacao.class, f.c().lotacaoB.getId()));
            a.setJornada(em.find(Jornada.class, f.c().jornadaB.getId()));
            a.setCargo(em.find(Cargo.class, f.c().cargoB.getId()));
            a.setDataInicio(RelogioSistema.hoje()); a.setDataFim(f.contrato().fim());
            a.aplicarFotografiaDoMapeamento(mapa);
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(f.gradeDocumentada());
            assertThat(a.getLotacaoNomeFotografia()).isNull();
            em.persist(a); em.flush(); return a.getId();
        });
        emTransacao(() -> {
            AlocacaoVinculo a = em.find(AlocacaoVinculo.class, alocacaoId);
            assertThat(a.getLotacaoNomeFotografia()).as("PrePersist não completa null documental pelo mestre").isNull();
            assertThat(a.getJornadaDescricaoFotografia()).isNull();
            assertThat(a.getCargoNomeFotografia()).isNull();
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(f.gradeDocumentada());
            assertThat(a.getLotacaoMunicipioFotografia()).isEqualTo("MUNICIPIO DOCUMENTADO NA TRANSFERENCIA");
            assertThat(a.getLotacao().getId()).isEqualTo(f.c().lotacaoB.getId());
            assertThat(a.getJornada().getId()).isEqualTo(f.c().jornadaB.getId());
            assertThat(a.getCargo().getId()).isEqualTo(f.c().cargoB.getId());
            MapeamentoTransferenciaGestao original = em.createQuery("select m from MapeamentoTransferenciaGestao m "
                            + "where m.transferencia.contrato.id=:id", MapeamentoTransferenciaGestao.class)
                    .setParameter("id", f.contrato().id()).getSingleResult();
            assertThat(original.getLotacaoDestinoNomeFotografia()).isEqualTo(f.lotacaoDocumentada());
            assertThat(original.getJornadaDestinoDescricaoFotografia()).isEqualTo(f.jornadaDocumentada());
            assertThat(original.getCargoDestinoNomeFotografia()).isEqualTo(f.cargoDocumentado());
            return null;
        });
    }

    private Preparado exercitar(boolean legadoSemFotografia) throws Exception {
        return exercitar(legadoSemFotografia, false);
    }

    private Preparado exercitar(boolean legadoSemFotografia, boolean rotulosAusentes) throws Exception {
        return exercitar(legadoSemFotografia, rotulosAusentes, preparado -> {});
    }

    private Preparado exercitar(boolean legadoSemFotografia, boolean rotulosAusentes,
                                java.util.function.Consumer<Preparado> fonteContextualExplicita) throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 4, null, false);
                c.lotacaoB.setMunicipio("MUNICIPIO DOCUMENTADO NA TRANSFERENCIA");
                MotivoDesligamento a = new MotivoDesligamento(), b = new MotivoDesligamento();
                a.setGestaoLocal(c.gestaoA); a.setDescricao("Término foto origem " + contrato.id());
                b.setGestaoLocal(c.gestaoB); b.setDescricao("Término foto destino " + contrato.id());
                a.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                b.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(a); em.persist(b); em.flush();
                for (MotivoDesligamento motivo : List.of(a, b)) {
                    var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
                    origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
                    origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(contrato.inicio()); origem.setAtivo(true);
                    origem.setMotivo("Fonte sintética explícita da causa individual antes das faixas restauradas");
                    em.persist(origem);
                }
                em.flush();
                return new Preparado(c, contrato, a.getId(), b.getId(), FotografiaJornada.capturar(c.jornadaB),
                        c.lotacaoB.getNome(), c.jornadaB.getDescricao(), c.cargoB.getNome());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        fonteContextualExplicita.accept(f);
        String raiz = "/api/vinculos-contratuais";
        int seed = Math.toIntExact(731000000L + f.contrato().id());
        long id = enviar(f.c().localA, raiz, mapa("pessoa", mapa("nome", "Pessoa fotografia restaurada " + seed, "cpf", cpf(seed)),
                "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                "dataInicio", RelogioSistema.hoje().minusDays(20)), 201, "Preparação: vínculo anterior à transferência").path("id").asLong();
        LocalDate fimAnterior = RelogioSistema.hoje().minusDays(10);
        Map<String, Object> terminar = acao(); terminar.put("dataFim", fimAnterior); terminar.put("motivoDesligamentoId", f.motivoA());
        enviar(f.c().localA, raiz + "/" + id + "/desligar", terminar, 200, "Término original não possui sucessora no destino");
        LocalDate transferencia = RelogioSistema.hoje().minusDays(5);
        conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa("dataEfetiva", transferencia,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                "motivo", "Contexto posterior comprovado", "referenciaSei", "SEI-FOTO-CONTEXTO", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                        "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId()))), 200,
                "HIS004 mapeamento documenta o contexto posterior conhecido nessa operação");
        if (legadoSemFotografia || rotulosAusentes) {
            emTransacao(() -> {
                Long transferenciaId = em.createQuery("select t.id from TransferenciaGestaoContrato t "
                                + "where t.contrato.id=:contrato and t.dataEfetiva=:data", Long.class)
                        .setParameter("contrato", f.contrato().id()).setParameter("data", transferencia).getSingleResult();
                // DML de fixture representa uma linha anterior às colunas de
                // fotografia; não cria schema nem preenche retrospectivamente a prova.
                String campos = legadoSemFotografia ? "lotacaoDestinoMunicipioFotografia=null, jornadaDestinoSemanalFotografia=null"
                        : "lotacaoDestinoNomeFotografia='', jornadaDestinoDescricaoFotografia='', cargoDestinoNomeFotografia=''";
                int alteradas = em.createQuery("update MapeamentoTransferenciaGestao set " + campos
                                + " where transferencia.id=:id")
                        .setParameter("id", transferenciaId).executeUpdate();
                assertThat(alteradas).isEqualTo(1);
                em.clear(); return null;
            });
        }
        emTransacao(() -> {
            em.find(Lotacao.class, f.c().lotacaoB.getId()).setMunicipio("MUNICIPIO POSTERIOR DO MESTRE");
            em.find(Lotacao.class, f.c().lotacaoB.getId()).setNome("Lotação atual posterior " + f.contrato().id());
            em.find(Cargo.class, f.c().cargoB.getId()).setNome("Cargo atual posterior " + f.contrato().id());
            Jornada jornada = em.find(Jornada.class, f.c().jornadaB.getId());
            jornada.setDescricao("Turno atual posterior " + f.contrato().id());
            jornada.getDias().forEach(d -> {
                if (Boolean.TRUE.equals(d.getDiaTrabalho())) {
                    d.setEntrada1("06:00"); d.setSaida1("07:00"); d.setEntrada2(null); d.setSaida2(null);
                }
            });
            assertThat(FotografiaJornada.capturar(jornada)).isNotEqualTo(f.gradeDocumentada());
            return null;
        });
        Map<String, Object> corrigir = acao(); corrigir.put("dataFim", RelogioSistema.hoje().plusDays(3));
        corrigir.put("motivoDesligamentoId", f.motivoB());
        if (legadoSemFotografia) {
            enviar(f.c().localB, raiz + "/" + id + "/previas/retificacao-desligamento", corrigir, 409,
                    "HIS004 ausência de foto histórica exige tratamento documental, sem inferir grade atual");
            emTransacao(() -> {
                assertThat(em.find(VinculoContratual.class, id).getDataFim()).isEqualTo(fimAnterior);
                assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:id and a.dataInicio=:data", Long.class)
                        .setParameter("id", id).setParameter("data", transferencia).getSingleResult()).isZero();
                return null;
            });
            return f;
        }
        JsonNode previa = enviar(f.c().localB, raiz + "/" + id + "/previas/retificacao-desligamento", corrigir, 200,
                "DES005 restauração usa apenas contexto documentado");
        corrigir.put("confirmarImpacto", true); corrigir.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localB, raiz + "/" + id + "/retificar-desligamento", corrigir, 200,
                "Retificação materializa sucessora com a fotografia da transferência");
        emTransacao(() -> {
            AlocacaoVinculo sucessora = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id and a.dataInicio=:data", AlocacaoVinculo.class)
                    .setParameter("id", id).setParameter("data", transferencia).getSingleResult();
            assertThat(sucessora.getJornadaSemanalFotografia()).as("HIS004 FRE001 grade usada na frequência deve ser a comprovada, não a atual")
                    .isEqualTo(f.gradeDocumentada());
            assertThat(sucessora.getLotacaoMunicipioFotografia()).isEqualTo("MUNICIPIO DOCUMENTADO NA TRANSFERENCIA");
            assertThat(sucessora.getLotacao().getMunicipio()).isEqualTo("MUNICIPIO POSTERIOR DO MESTRE");
            assertThat(sucessora.getLotacao().getId()).isEqualTo(f.c().lotacaoB.getId());
            assertThat(sucessora.getJornada().getId()).isEqualTo(f.c().jornadaB.getId());
            assertThat(sucessora.getCargo().getId()).isEqualTo(f.c().cargoB.getId());
            assertThat(sucessora.getLotacaoNomeFotografia()).isEqualTo(rotulosAusentes ? null : f.lotacaoDocumentada());
            assertThat(sucessora.getJornadaDescricaoFotografia()).isEqualTo(rotulosAusentes ? null : f.jornadaDocumentada());
            assertThat(sucessora.getCargoNomeFotografia()).isEqualTo(rotulosAusentes ? null : f.cargoDocumentado());
            MapeamentoTransferenciaGestao fonte = em.createQuery("select m from MapeamentoTransferenciaGestao m "
                            + "where m.transferencia.contrato.id=:id", MapeamentoTransferenciaGestao.class)
                    .setParameter("id", f.contrato().id()).getSingleResult();
            assertThat(fonte.getLotacaoDestinoNomeFotografia()).isEqualTo(rotulosAusentes ? "" : f.lotacaoDocumentada());
            assertThat(fonte.getJornadaDestinoSemanalFotografia()).isEqualTo(f.gradeDocumentada());
            return null;
        });
        return f;
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Correção comprovada da data final", "evidencia", "SEI-FOTO-RESTAURADA",
                "autorizacao", "ATO-FOTO-RESTAURADA");
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoA, long motivoB, String gradeDocumentada,
                             String lotacaoDocumentada, String jornadaDocumentada, String cargoDocumentado) {}
}
