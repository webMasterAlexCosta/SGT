package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Regressão de COL-010/012, HIS-004, TMP-004/005/008: fotografia desconhecida
 * não impede, por si, um ato documentado de matrícula da identidade canônica.
 * Adjudicação independente /root/revisao_final_pessoas: fonte C8 e SQL central
 * demonstraram foto NULL, sem uso do nome atual como fonte retroativa.
 * O oráculo antigo de 409 permanece preservado na fonte congelada C8 e no XML.
 * Esta proposta reforça a ausência nominal persistida e os cortes D/D+1/K.
 * A ausência de todas as fontes nominais não autoriza inventar nome no passado.
 * DML remove somente o evento de criação desta fixture; nenhum DDL é executado.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoHistoricaAusenciaNominalHttpIT extends ContratosHttpBase {

    @ParameterizedTest(name = "atoRetroativoSemFonte={0}")
    @ValueSource(booleans = {true, false})
    void ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea(boolean retroativo) throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        String nome = "NOME CONHECIDO AGORA " + n.id();
        String original = "SEM-FOTO-ORIG-" + n.id();
        String proposta = "SEM-FOTO-NOVA-" + n.id();
        var criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(),
                "dataInicio", n.inicio().plusDays(1), "matricula", original,
                "pessoa", mapa("nome", nome, "cpf", cpf(800000000 + (int) (n.id() % 100000000)))),
                201, "COL010 criação canônica anterior à perda simulada da fonte nominal");
        long vinculoId = criado.path("id").asLong();
        long pessoaId = criado.path("pessoaId").asLong();
        emTransacao(() -> {
            assertThat(em.createQuery("delete from EventoVinculoContratual e where e.vinculo.id=:id")
                    .setParameter("id", vinculoId).executeUpdate()).isEqualTo(1);
            return null;
        });

        LocalDate data = retroativo ? n.inicio().plusDays(2) : RelogioSistema.hoje();
        enviar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/alterar-matricula", mapa(
                "dataEfetiva", data, "novaMatricula", proposta,
                "justificativa", "Ato de matrícula não prova identidade nominal de período sem fonte",
                "evidencia", "SEI-MATRICULA-SEM-FOTO", "autorizacao", "ATO-MATRICULA-SEM-FOTO"),
                200,
                "COL010 matrícula documentada conserva nome passado desconhecido; hoje captura nome contemporâneo");

        Foto foto = emTransacao(() -> {
            em.clear();
            var persistido = em.find(VinculoContratual.class, vinculoId);
            assertThat(persistido.getMatricula()).isEqualTo(proposta);
            assertThat(persistido.getPessoa().getNome()).isEqualTo(nome);
            var eventos = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id",
                    EventoVinculoContratual.class).setParameter("id", vinculoId).getResultList();
            assertThat(eventos).hasSize(1);
            var evento = eventos.get(0);
            assertThat(evento.getPessoaNomeFotografia()).isEqualTo(retroativo ? null : nome);
            assertThat(evento.getDataEfetiva()).isEqualTo(data);
            assertThat(evento.getTipoEvento().name()).isEqualTo("ALTERACAO_MATRICULA");
            assertThat(evento.getMatriculaAnterior()).isEqualTo(original);
            assertThat(evento.getMatriculaNova()).isEqualTo(proposta);
            assertThat(evento.getResponsavelId()).isEqualTo(c.localA.getId());
            assertThat(evento.getEvidencia()).isEqualTo("SEI-MATRICULA-SEM-FOTO");
            assertThat(evento.getAutorizacao()).isEqualTo("ATO-MATRICULA-SEM-FOTO");
            assertThat(em.createQuery("select count(r) from ReservaMatriculaContrato r "
                            + "where r.vinculo.id=:id and r.matricula=:matricula", Long.class)
                    .setParameter("id", vinculoId).setParameter("matricula", proposta).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select count(p) from ProvaNomePessoaHistorico p where p.vinculo.id=:id", Long.class)
                    .setParameter("id", vinculoId).getSingleResult()).isZero();
            return fotografia(evento);
        });
        Instant antesK = foto.registradoEm().minusNanos(1000);
        Instant depoisK = foto.registradoEm().plusNanos(1000);
        emTransacao(() -> {
            em.clear();
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id "
                            + "and e.registradoEm<=:k", Long.class)
                    .setParameter("id", vinculoId).setParameter("k", antesK).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id "
                            + "and e.registradoEm<=:k", Long.class)
                    .setParameter("id", vinculoId).setParameter("k", foto.registradoEm()).getSingleResult()).isEqualTo(1L);
            return null;
        });
        long versaoAntesDaConsulta = emTransacao(() -> {
            em.clear();
            return em.find(VinculoContratual.class, vinculoId).getVersion();
        });
        var semFonteNoCorte = executar(c.localA,
                get("/api/vinculos-contratuais/pessoas/" + pessoaId + "/posicao")
                        .param("data", data.toString()).param("conhecidoAte", antesK.toString()), 409,
                "TMP004/005 antes da primeira fonte do vínculo, recusa não certifica a projeção atual como história");
        assertThat(semFonteNoCorte.getResponse().getContentAsString()).doesNotContain(nome, proposta);
        emTransacao(() -> {
            em.clear();
            var persistido = em.find(VinculoContratual.class, vinculoId);
            assertThat(persistido.getVersion()).isEqualTo(versaoAntesDaConsulta);
            assertThat(persistido.getMatricula()).isEqualTo(proposta);
            assertThat(persistido.getPessoa().getNome()).isEqualTo(nome);
            assertThat(fotografia(em.find(EventoVinculoContratual.class, foto.id()))).isEqualTo(foto);
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", vinculoId).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id "
                            + "and e.registradoEm<=:k", Long.class)
                    .setParameter("id", vinculoId).setParameter("k", antesK).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(r) from ReservaMatriculaContrato r "
                            + "where r.vinculo.id=:id and r.matricula=:matricula", Long.class)
                    .setParameter("id", vinculoId).setParameter("matricula", proposta).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select count(p) from ProvaNomePessoaHistorico p where p.vinculo.id=:id", Long.class)
                    .setParameter("id", vinculoId).getSingleResult()).isZero();
            return null;
        });
        conferirPosicao(c, pessoaId, nome, data, foto.registradoEm(), retroativo ? null : nome, foto);
        conferirPosicao(c, pessoaId, nome, data, depoisK, retroativo ? null : nome, foto);
        conferirPosicao(c, pessoaId, nome, data.minusDays(1), depoisK, null, foto);
        conferirPosicao(c, pessoaId, nome, data.plusDays(1), depoisK, null, foto);
        var eventosHttp = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/eventos",
                "Evento devolve sua fotografia própria e K exato, separados do DTO cadastral");
        assertThat(eventosHttp.size()).isEqualTo(1);
        assertThat(eventosHttp.get(0).path("id").asLong()).isEqualTo(foto.id());
        assertThat(Instant.parse(eventosHttp.get(0).path("registradoEm").asText())).isEqualTo(foto.registradoEm());
        if (retroativo) assertThat(eventosHttp.get(0).path("pessoaNomeFotografia").isNull()).isTrue();
        else assertThat(eventosHttp.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(nome);

        var passado = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + pessoaId
                        + "/posicao?data=" + n.inicio().plusDays(3),
                "COL010 nenhum dos dois caminhos fabrica fotografia para o período sem fonte");
        assertThat(passado.path("nomeAtual").asText()).isEqualTo(nome);
        assertThat(passado.path("nomeFotografia").isNull()).isTrue();
        assertThat(passado.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
        assertThat(passado.path("eventoIdentidadeFonteId").isNull()).isTrue();
        Foto relida = emTransacao(() -> {
            em.clear();
            return fotografia(em.find(EventoVinculoContratual.class, foto.id()));
        });
        assertThat(relida).as("Consultas D/K não preenchem a foto nem alteram o instante ou o hash do evento")
                .isEqualTo(foto);
        Path evidencia = Path.of("target/auditoria-http/candidato8-oraculo-nominal-"
                + (retroativo ? "retroativo" : "hoje") + ".json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, json(mapa("retroativo", retroativo, "fotoPersistida", foto,
                "fotoRelida", relida, "conhecimentoAnterior", antesK, "statusCorteAnterior", 409,
                "conhecimentoExato", foto.registradoEm(),
                "conhecimentoPosterior", depoisK, "diaSeguinte", data.plusDays(1),
                "fontesNominaisIncorporadas", 0, "autorRevisao", "/root/revisao_final_pessoas")));
    }

    private void conferirPosicao(Cenario c, long pessoaId, String atual, LocalDate data, Instant k,
                                 String esperado, Foto foto) throws Exception {
        var posicao = consultar(c.localA, "/api/vinculos-contratuais/pessoas/" + pessoaId
                        + "/posicao?data=" + data + "&conhecidoAte=" + k,
                "COL010/TMP004/005 fonte nominal pontual em D e conhecida somente a partir de K");
        assertThat(posicao.path("nomeAtual").asText()).isEqualTo(atual);
        assertThat(Instant.parse(posicao.path("conhecidoAte").asText())).isEqualTo(k);
        if (esperado == null) {
            assertThat(posicao.path("nomeFotografia").isNull()).isTrue();
            assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
            assertThat(posicao.path("eventoIdentidadeFonteId").isNull()).isTrue();
            assertThat(posicao.path("identidadeHistorica").path("nome").isNull()).isTrue();
            assertThat(posicao.path("identidadeHistorica").path("comprovado").asBoolean()).isFalse();
            assertThat(posicao.path("identidadeHistorica").path("fonteId").isNull()).isTrue();
        } else {
            assertThat(posicao.path("nomeFotografia").asText()).isEqualTo(esperado);
            assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isTrue();
            assertThat(posicao.path("eventoIdentidadeFonteId").asLong()).isEqualTo(foto.id());
            var resolvida = posicao.path("identidadeHistorica");
            assertThat(resolvida.path("nome").asText()).isEqualTo(esperado);
            assertThat(resolvida.path("comprovado").asBoolean()).isTrue();
            assertThat(resolvida.path("fonteId").asLong()).isEqualTo(foto.id());
            assertThat(Instant.parse(resolvida.path("fonteRegistradaEm").asText())).isEqualTo(foto.registradoEm());
            assertThat(resolvida.path("inicioComprovado").asText()).isEqualTo(data.toString());
            assertThat(resolvida.path("fimComprovado").asText()).isEqualTo(data.toString());
        }
    }

    private Foto fotografia(EventoVinculoContratual e) {
        return new Foto(e.getId(), e.getDataEfetiva(), e.getRegistradoEm(), e.getPessoaNomeFotografia(),
                e.getPessoaCpfFotografia(), e.getMatriculaAnterior(), e.getMatriculaNova(), e.getHashEvento());
    }
    private record Foto(long id, LocalDate dataEfetiva, Instant registradoEm, String nome, String cpf,
                        String matriculaAnterior, String matriculaNova, String hashEvento) {}
}
