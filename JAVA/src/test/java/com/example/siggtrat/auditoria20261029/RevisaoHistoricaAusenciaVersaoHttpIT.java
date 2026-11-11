package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revisão independente preparatória: HIS-001/HIS-006, CAT-004 e TMP-004/005.
 * Oráculo: norma §§1, 2, 5.4, 6.20 e 8.1, não o comportamento corrente.
 * DML altera exclusivamente os acervos sintéticos destes testes. Não há DDL.
 * Execução deve ser feita pelo coordenador em PostgreSQL real, sobre hash identificado.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoHistoricaAusenciaVersaoHttpIT extends ContratosHttpBase {

    @ParameterizedTest(name = "ausenciaTotalDaLinha={0}")
    @ValueSource(booleans = {true, false})
    void ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado(boolean removerLinha) throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate data = n.inicio().plusDays(1);
        var original = consultar(c.localA, posicao(n.id(), data), "HIS006 controle com fotografia original");
        var fotoOriginal = original.path("versaoContratual");
        assertThat(fotoOriginal.isObject()).isTrue();
        assertThat(fotoOriginal.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(fotoOriginal.path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");

        emTransacao(() -> {
            if (removerLinha) {
                assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:id")
                        .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            } else {
                assertThat(em.createQuery("update VersaoContratoNegocial set numeroContrato='', anoContrato=null, "
                                + "descricao='', tipoContratacaoId=null, tipoContratacaoDescricao='', fornecedorId=null, "
                                + "cnpjEmpresa='', razaoSocialEmpresa='', nomeFantasiaEmpresa='', dataInicio=null, dataFim=null, "
                                + "gestaoLocalId=null, gestaoLocalNome='', matriculaVinculoObrigatoria=null, "
                                + "matriculaTipoContratacaoObrigatoria=null, matriculaVinculoObrigatoriaEfetiva=null, "
                                + "diaSemanaReduzido=null, horasDiaReduzido=null, habilitacaoAdministrativa=null "
                                + "where contrato.id=:id")
                        .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            }
            Contrato atual = em.find(Contrato.class, n.id());
            atual.setNumeroContrato("ATUAL-SEM-PROVA-" + n.id());
            atual.setAnoContrato(n.inicio().getYear() + 1);
            atual.setDescricao("Descrição atual não comprovada no período original");
            atual.setCnpjEmpresa("11222333000181");
            atual.setRazaoSocialEmpresa("Empresa atual não comprovada no período original");
            atual.setNomeFantasiaEmpresa("Fantasia atual não comprovada");
            atual.setDataInicio(n.inicio().plusDays(5));
            atual.setDataFim(n.fim().plusDays(40));
            atual.setMatriculaVinculoObrigatoria(true);
            atual.setDiaSemanaReduzido(DayOfWeek.FRIDAY);
            atual.setHorasDiaReduzido(3);
            atual.setAtivo(true);
            em.find(TipoContratacao.class, c.tipoA.getId()).setDescricao("Tipo atual sem prova original");
            atual.setTipo("Tipo atual sem prova original");
            em.find(GestaoLocal.class, c.gestaoA.getId()).setNome("Gestão atual sem prova original " + n.id());
            em.flush();
            return null;
        });

        var recusada = executar(c.localA, get(posicao(n.id(), data)), 404,
                "PER001 Gestão histórica desconhecida não concede leitura local pela projeção atual");
        var erroLocal = mapper.readTree(recusada.getResponse().getContentAsString());
        assertThat(erroLocal.path("message").asText()).contains("no seu escopo");
        assertThat(erroLocal.hasNonNull("data")).isFalse();
        assertThat(erroLocal.toString()).doesNotContain("ATUAL-SEM-PROVA", "Empresa atual não comprovada",
                "Descrição atual não comprovada", "Gestão atual sem prova original");

        // O perfil global legítimo permite examinar o payload histórico mesmo
        // quando falta a prova organizacional.404 local, sozinho, não comprova
        // que o reconstrutor deixou de inventar campos a partir do cadastro atual.
        var resposta = consultar(c.nacional, posicao(n.id(), data),
                "HIS001/HIS006 leitura global explicita os campos históricos desconhecidos");
        var foto = resposta.path("versaoContratual");
        if (removerLinha) {
            assertThat(foto.isNull())
                    .as("Sem qualquer fotografia conhecida, não existe versão a fabricar").isTrue();
        } else {
            assertThat(foto.isObject()).as("A fonte parcial existente continua representada").isTrue();
            for (String campo : List.of("numeroVersao", "eventoOrigemId", "dataEfetiva",
                    "registradoEm", "efetivoPrevisto"))
                assertThat(foto.path(campo)).as("A fonte preserva seu campo comprovado %s", campo)
                        .isEqualTo(fotoOriginal.path(campo));
        }
        assertThat(resposta.toString()).doesNotContain("ATUAL-SEM-PROVA", "Empresa atual não comprovada",
                "Descrição atual não comprovada", "Gestão atual sem prova original",
                "Fantasia atual não comprovada", "Tipo atual sem prova original");
        assertSoftly(s -> {
            for (String campo : List.of("numeroContrato", "anoContrato", "descricao", "cnpjEmpresa",
                    "razaoSocialEmpresa", "nomeFantasiaEmpresa", "dataInicio", "dataFim", "tipoContratacaoId",
                    "tipoContratacaoDescricao", "gestaoLocalId", "gestaoLocalNome", "matriculaVinculoObrigatoria",
                    "matriculaTipoContratacaoObrigatoria", "matriculaVinculoObrigatoriaEfetiva",
                    "diaSemanaReduzido", "horasDiaReduzido"))
                s.assertThat(desconhecido(foto.path(campo)))
                        .as("%s: sem fonte original, a projeção atual não comprova o passado", campo).isTrue();
        });
        for (String campo : List.of("vigenciaInicio", "vigenciaFim", "situacaoTemporal"))
            assertThat(desconhecido(resposta.path(campo))).as("%s não pode ser inferido sem origem", campo).isTrue();
        emTransacao(() -> {
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isEqualTo(removerLinha ? 0L : 1L);
            return null;
        });
    }

    @Test
    void ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate antesDoAto = n.inicio().plusDays(1);
        var original = consultar(c.localA, posicao(n.id(), antesDoAto), "HIS006 publicação antes do aditamento")
                .path("versaoContratual");
        LocalDate efeito = n.inicio().plusDays(10);
        var req = aditamento("REVISOR-ADI-" + n.id(), efeito);
        req.put("novaDescricao", "Descrição introduzida somente pelo aditamento posterior");
        req.put("novaMatriculaVinculoObrigatoria", true);
        req.put("novoDiaSemanaReduzido", "FRIDAY");
        req.put("novasHorasDiaReduzido", 4);
        emTransacao(() -> {
            try { registrarAditamento(c, n.id(), req); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
            return null;
        });
        emTransacao(() -> {
            assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:id and v.numeroVersao=1")
                    .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isEqualTo(1);
            return null;
        });
        var antes = consultarSemInventar(c.localA, posicao(n.id(), antesDoAto));
        if (antes != null && antes.path("versaoContratual").isObject()) {
            var foto = antes.path("versaoContratual");
            assertSoftly(s -> {
                for (String campo : List.of("descricao", "matriculaVinculoObrigatoria",
                        "matriculaVinculoObrigatoriaEfetiva", "diaSemanaReduzido", "horasDiaReduzido")) {
                    JsonNode valor = foto.path(campo);
                    s.assertThat(desconhecido(valor) || valor.equals(original.path(campo)))
                            .as("%s: foto de ADI posterior não substitui publicação ausente; recebido=%s", campo, valor).isTrue();
                }
            });
        }
        // O ato documentado continua comprovando seus próprios deltas a partir da data correta.
        var depois = consultar(c.localA, posicao(n.id(), efeito), "HIS001 aditamento documentado conserva seu alcance")
                .path("versaoContratual");
        assertThat(depois.path("descricao").asText()).isEqualTo(req.get("novaDescricao"));
        assertThat(depois.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
    }

    @Test
    void fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        Instant corte = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.SECONDS);
        emTransacao(() -> {
            // Acervo sintético: publicação conhecida, fotografia incorporada depois do corte.
            em.find(Contrato.class, n.id()).setPublicadoEm(corte.minusSeconds(60));
            assertThat(em.createQuery("update EventoCicloVidaContrato set registradoEm=:instante "
                            + "where contrato.id=:id and tipoEvento='PUBLICACAO'")
                    .setParameter("instante", corte.minusSeconds(60)).setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            assertThat(em.createQuery("update VersaoContratoNegocial set registradoEm=:instante where contrato.id=:id")
                    .setParameter("instante", corte.plusSeconds(60)).setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            return null;
        });
        var resposta = consultarSemInventar(c.localA, posicao(n.id(), n.inicio()) + "&conhecidoAte=" + corte);
        if (resposta != null) {
            JsonNode foto = resposta.path("versaoContratual");
            if (foto.isObject()) {
                JsonNode instante = foto.path("registradoEm");
                if (!desconhecido(instante)) assertThat(Instant.parse(instante.asText()))
                        .as("TMP005/REL003: fonte registrada depois do corte não era conhecida")
                        .isBeforeOrEqualTo(corte);
                assertThat(desconhecido(foto.path("descricao")))
                        .as("Sem fotografia conhecida, descrição não pode vir da versão futura").isTrue();
            }
        }
        var depois = consultar(c.localA, posicao(n.id(), n.inicio()) + "&conhecidoAte=" + corte.plusSeconds(90),
                "HIS001 controle: fonte passa a existir depois da incorporação");
        assertThat(depois.path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
    }

    @Test
    void publicacaoNovaMantemValoresERotulosMesmoComMestresAlterados() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicarPersistido(c);
        LocalDate data = n.inicio().plusDays(1);
        var foto = consultar(c.localA, posicao(n.id(), data), "HIS006 controle positivo da nova publicação")
                .path("versaoContratual");
        var composicao = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data,
                "CAT004 composição original conhecida").get(0);
        emTransacao(() -> {
            em.find(GestaoLocal.class, c.gestaoA.getId()).setNome("Gestão renomeada " + n.id());
            var tipo = em.find(TipoContratacao.class, c.tipoA.getId());
            tipo.setDescricao("Tipo renomeado " + n.id()); tipo.setMatriculaVinculoObrigatoria(true);
            em.find(Lotacao.class, c.lotacaoA.getId()).setNome("Lotação renomeada " + n.id());
            em.find(Jornada.class, c.jornadaA.getId()).setDescricao("Turno renomeado " + n.id());
            em.find(Cargo.class, c.cargoA.getId()).setNome("Cargo renomeado " + n.id());
            var atual = em.find(Contrato.class, n.id());
            atual.setDescricao("Projeção divergente " + n.id()); atual.setMatriculaVinculoObrigatoria(true);
            atual.setDiaSemanaReduzido(DayOfWeek.FRIDAY); atual.setHorasDiaReduzido(3);
            return null;
        });
        var relida = consultar(c.localA, posicao(n.id(), data), "HIS001/CAT004 leitura nova usa a fonte publicada")
                .path("versaoContratual");
        for (String campo : List.of("numeroContrato", "anoContrato", "descricao", "tipoContratacaoId",
                "tipoContratacaoDescricao", "cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa", "dataInicio",
                "dataFim", "efetivoPrevisto", "gestaoLocalId", "gestaoLocalNome", "matriculaVinculoObrigatoria",
                "matriculaTipoContratacaoObrigatoria", "matriculaVinculoObrigatoriaEfetiva", "diaSemanaReduzido",
                "horasDiaReduzido", "habilitacaoAdministrativa"))
            assertThat(relida.path(campo)).as("fotografia original de %s", campo).isEqualTo(foto.path(campo));
        var recorte = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data,
                "CAT004 nomes originais permanecem fotografados").get(0);
        for (String campo : List.of("lotacaoNome", "jornadaDescricao", "cargoNome"))
            assertThat(recorte.path(campo)).as(campo).isEqualTo(composicao.path(campo));
        emTransacao(() -> {
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    private NovoContrato publicarPersistido(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private String posicao(long id, LocalDate data) { return base(id) + "/posicao-temporal?data=" + data; }

    private boolean desconhecido(JsonNode valor) {
        return valor.isMissingNode() || valor.isNull() || valor.isTextual() && valor.asText().isBlank();
    }

    /** A norma admite indisponibilidade segura; não exige um código HTTP específico para acervo incompleto. */
    private JsonNode consultarSemInventar(UsuarioAplicacao usuario, String path) throws Exception {
        var resposta = mvc.perform(autenticar(get(path), usuario)).andReturn();
        int status = resposta.getResponse().getStatus();
        assertThat(status).as("consulta deve responder dados comprovados/desconhecidos ou conflito seguro: %s", path)
                .isIn(200, 409);
        return status == 409 ? null : dados(resposta);
    }
}
