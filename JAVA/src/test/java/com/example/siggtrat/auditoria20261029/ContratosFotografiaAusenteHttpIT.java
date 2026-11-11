package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/** Legado sintético por mutação HQL gerada pelo Hibernate: não muda constraints nem preenche fotografias com o cadastro atual. */
class ContratosFotografiaAusenteHttpIT extends ContratosHttpBase {
    private record Recorte(long lotacao, long jornada, long cargo, String nome, String turno, String funcao) {}

    @ParameterizedTest
    @ValueSource(strings = {"BASE", "ADITAMENTO", "TRANSFERENCIA", "ADITAMENTO_ORIGEM_AUSENTE"})
    void composicaoEExcedentesDistinguemFotografiaConhecidaDeAusenciaLegada(String fonte) throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        LocalDate efeito = RelogioSistema.hoje().minusDays(5);
        long fonteId = n.composicao1(); int quantidade = 5;
        Recorte recorte = recorte(c.lotacaoA, c.jornadaA, c.cargoA);
        if (fonte.equals("ADITAMENTO") || fonte.equals("ADITAMENTO_ORIGEM_AUSENTE")) {
            var req = aditamento("ADI-FOTO-" + n.id(), efeito);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 6)));
            fonteId = registrarAditamento(c, n.id(), req).path("id").asLong(); quantidade = 6;
        } else if (fonte.equals("TRANSFERENCIA")) {
            fonteId = transferir(c, n, efeito);
            recorte = recorte(c.lotacaoB, c.jornadaB, c.cargoB);
        }
        renomear(recorte, n.id()); em.flush(); em.clear();

        // Controle positivo: nomes fotografados resistem à renomeação do cadastro.
        for (String endpoint : List.of("/composicoes", "/composicoes/excedentes")) {
            var item = consultarRecorte(c, n, endpoint, recorte, quantidade);
            assertThat(item.path("lotacaoNome").asText()).isEqualTo(recorte.nome());
            assertThat(item.path("jornadaDescricao").asText()).isEqualTo(recorte.turno());
            assertThat(item.path("cargoNome").asText()).isEqualTo(recorte.funcao());
        }

        boolean aditamento = fonte.equals("ADITAMENTO") || fonte.equals("ADITAMENTO_ORIGEM_AUSENTE");
        removerFotografiasPersistiveis(aditamento ? "ADITAMENTO" : fonte, fonteId);
        if (fonte.equals("ADITAMENTO_ORIGEM_AUSENTE")) {
            removerFotografiasPersistiveis("BASE", n.composicao1());
        }
        em.clear();
        if (aditamento) {
            // Ausência na foto do item quantitativo não apaga a foto da origem.
            // O cenário adicional torna ambas as origens desconhecidas por DML
            // sintético; releitura após clear comprova a preparação no PostgreSQL.
            var itemPersistido = em.createQuery("select i from AditamentoComposicao i where i.aditamento.id=:id",
                            AditamentoComposicao.class)
                    .setParameter("id", fonteId).getSingleResult();
            assertThat(itemPersistido.getComposicaoBase().getId()).isEqualTo(n.composicao1());
            assertThat(itemPersistido.getQuantidadeAnterior()).isEqualTo(5);
            assertThat(itemPersistido.getVariacao()).isEqualTo(1);
            assertThat(itemPersistido.getQuantidadeResultante()).isEqualTo(6);
            assertThat(ausente(itemPersistido.getLotacaoNomeFotografia())).isTrue();
            assertThat(ausente(itemPersistido.getJornadaDescricaoFotografia())).isTrue();
            assertThat(ausente(itemPersistido.getCargoNomeFotografia())).isTrue();
            var origemPersistida = em.find(ComposicaoContratual.class, n.composicao1());
            if (fonte.equals("ADITAMENTO")) {
                assertThat(origemPersistida.getLotacaoNomeFotografia()).isEqualTo(recorte.nome());
                assertThat(origemPersistida.getJornadaDescricaoFotografia()).isEqualTo(recorte.turno());
                assertThat(origemPersistida.getCargoNomeFotografia()).isEqualTo(recorte.funcao());
            } else {
                assertThat(ausente(origemPersistida.getLotacaoNomeFotografia())).isTrue();
                assertThat(ausente(origemPersistida.getJornadaDescricaoFotografia())).isTrue();
                assertThat(ausente(origemPersistida.getCargoNomeFotografia())).isTrue();
            }
        }
        var lista = consultarRecorte(c, n, "/composicoes", recorte, quantidade);
        var excedentes = consultarRecorte(c, n, "/composicoes/excedentes", recorte, quantidade);
        assertThat(excedentes.path("quantidadeVinculadaAtual").asLong()).isZero();
        assertThat(excedentes.path("quantidadeExcedente").asLong()).isZero();
        Map<String, String> fotografiaOriginal = Map.of("lotacaoNome", recorte.nome(),
                "jornadaDescricao", recorte.turno(), "cargoNome", recorte.funcao());
        assertSoftly(s -> {
            for (var item : List.of(lista, excedentes)) {
                for (String campo : List.of("lotacaoNome", "jornadaDescricao", "cargoNome")) {
                    if (fonte.equals("ADITAMENTO")) {
                        s.assertThat(item.path(campo).asText()).as(fonte + " " + campo + " da origem conhecida")
                                .isEqualTo(fotografiaOriginal.get(campo));
                    } else {
                        s.assertThat(item.path(campo).isNull()).as(fonte + " " + campo + " sem fotografia").isTrue();
                    }
                }
            }
        });
    }

    @Test
    void historicoDeRemanejamentoPreservaQuantidadesEFotosConhecidasMasNaoInventaAsAusentes() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 50, false);
        var recorte = recorte(c.lotacaoA, c.jornadaA, c.cargoA);
        var req = remanejamento(n, "REM-FOTO-" + n.id(), RelogioSistema.hoje().minusDays(5), 5);
        previaRemanejamento(c, n.id(), req);
        long remId = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201,
                "REM003 registro válido de redistribuição soma zero").path("id").asLong();
        em.flush();
        renomear(recorte, n.id()); em.flush(); em.clear();
        var conhecido = consultar(c.nacional, base(n.id()) + "/remanejamentos", "CAT004 foto original do REM").get(0);
        var primeiro = porCargo(conhecido.path("itens"), recorte.cargo());
        assertThat(primeiro.path("lotacaoNome").asText()).isEqualTo(recorte.nome());
        assertThat(primeiro.path("jornadaDescricao").asText()).isEqualTo(recorte.turno());
        assertThat(primeiro.path("cargoNome").asText()).isEqualTo(recorte.funcao());
        String cargoSegundoFotografado = conhecido.path("itens").get(1).path("cargoNome").asText();

        assertThat(em.createQuery("update RemanejamentoComposicao set lotacaoNomeFotografia='', "
                        + "jornadaDescricaoFotografia=' ', cargoNomeFotografia='' where remanejamento.id=:id and cargo.id=:cargo")
                .setParameter("id", remId).setParameter("cargo", recorte.cargo()).executeUpdate()).isEqualTo(1);
        em.clear();
        var historico = consultar(c.nacional, base(n.id()) + "/remanejamentos", "CAT004 ausência no item do REM").get(0);
        assertThat(historico.path("id").asLong()).isEqualTo(remId);
        assertThat(historico.path("somaVariacoes").asInt()).isZero();
        var item = porCargo(historico.path("itens"), recorte.cargo());
        assertThat(item.path("composicaoId").asLong()).isEqualTo(n.composicao1());
        assertThat(item.path("lotacaoId").asLong()).isEqualTo(recorte.lotacao());
        assertThat(item.path("jornadaId").asLong()).isEqualTo(recorte.jornada());
        assertThat(item.path("quantidadeAnterior").asInt()).isEqualTo(50);
        assertThat(item.path("variacao").asInt()).isEqualTo(-5);
        assertThat(item.path("quantidadeResultante").asInt()).isEqualTo(45);
        assertThat(historico.path("itens").get(1).path("quantidadeResultante").asInt()).isEqualTo(55);
        assertThat(historico.path("itens").get(1).path("cargoNome").asText()).isEqualTo(cargoSegundoFotografado);
        assertSoftly(s -> {
            for (String campo : List.of("lotacaoNome", "jornadaDescricao", "cargoNome")) {
                s.assertThat(item.path(campo).isNull()).as(campo + " do REM sem prova histórica").isTrue();
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void novoMapaConservaProvaOuAusenciaDaOrigemECapturaDestinoDoAto(boolean origemComFoto) throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var origem = recorte(c.lotacaoA, c.jornadaA, c.cargoA);
        if (!origemComFoto) removerFotografiasPersistiveis("BASE", n.composicao1());
        em.clear(); renomear(origem, n.id());
        var destinoAnterior = recorte(c.lotacaoB, c.jornadaB, c.cargoB);
        renomear(destinoAnterior, n.id()); em.flush(); em.clear();
        var destino = recorte(em.find(Lotacao.class, destinoAnterior.lotacao()),
                em.find(Jornada.class, destinoAnterior.jornada()), em.find(Cargo.class, destinoAnterior.cargo()));
        String municipio = em.find(Lotacao.class, destino.lotacao()).getMunicipio();
        String semana = FotografiaJornada.capturar(em.find(Jornada.class, destino.jornada()));
        assertThat(semana).isNotBlank();

        long id = transferir(c, n, RelogioSistema.hoje().minusDays(5));
        em.flush(); em.clear();
        var t = consultar(c.nacional, base(n.id()) + "/transferencias-gestao",
                "CON023 novo mapa conserva a prova disponível na origem").get(0);
        assertThat(t.path("id").asLong()).isEqualTo(id);
        var dto = t.path("mapeamentos").get(0);
        assertThat(dto.path("composicaoOrigemId").asLong()).isEqualTo(n.composicao1());
        assertThat(dto.path("lotacaoOrigemId").asLong()).isEqualTo(origem.lotacao());
        assertThat(dto.path("jornadaOrigemId").asLong()).isEqualTo(origem.jornada());
        assertThat(dto.path("cargoOrigemId").asLong()).isEqualTo(origem.cargo());
        assertThat(dto.path("lotacaoDestinoId").asLong()).isEqualTo(destino.lotacao());
        assertThat(dto.path("jornadaDestinoId").asLong()).isEqualTo(destino.jornada());
        assertThat(dto.path("cargoDestinoId").asLong()).isEqualTo(destino.cargo());
        assertThat(dto.path("lotacaoDestinoNome").asText()).isEqualTo(destino.nome());
        assertThat(dto.path("jornadaDestinoDescricao").asText()).isEqualTo(destino.turno());
        assertThat(dto.path("cargoDestinoNome").asText()).isEqualTo(destino.funcao());
        var persistido = em.find(MapeamentoTransferenciaGestao.class, dto.path("id").asLong());
        assertThat(persistido.getLotacaoDestinoMunicipioFotografia()).isEqualTo(municipio);
        assertThat(persistido.getJornadaDestinoSemanalFotografia()).isEqualTo(semana);
        if (origemComFoto) {
            assertThat(dto.path("lotacaoOrigemNome").asText()).isEqualTo(origem.nome());
            assertThat(dto.path("jornadaOrigemDescricao").asText()).isEqualTo(origem.turno());
            assertThat(dto.path("cargoOrigemNome").asText()).isEqualTo(origem.funcao());
            assertThat(persistido.getLotacaoOrigemNomeFotografia()).isEqualTo(origem.nome());
            assertThat(persistido.getJornadaOrigemDescricaoFotografia()).isEqualTo(origem.turno());
            assertThat(persistido.getCargoOrigemNomeFotografia()).isEqualTo(origem.funcao());
        } else {
            assertSoftly(s -> {
                for (String campo : List.of("lotacaoOrigemNome", "jornadaOrigemDescricao", "cargoOrigemNome")) {
                    s.assertThat(dto.path(campo).isNull()).as(campo + " não pode ser reconstituído pelo mestre").isTrue();
                }
                s.assertThat(ausente(persistido.getLotacaoOrigemNomeFotografia())).isTrue();
                s.assertThat(ausente(persistido.getJornadaOrigemDescricaoFotografia())).isTrue();
                s.assertThat(ausente(persistido.getCargoOrigemNomeFotografia())).isTrue();
            });
        }
    }

    private JsonNode consultarRecorte(Cenario c, NovoContrato n, String endpoint, Recorte r, int quantidade) throws Exception {
        var lista = consultar(c.nacional, base(n.id()) + endpoint + "?data=" + RelogioSistema.hoje().minusDays(1),
                "CAT004 projeção publicada por fonte histórica");
        assertThat(lista.size()).isEqualTo(1);
        var item = lista.get(0);
        assertThat(item.path(endpoint.endsWith("excedentes") ? "composicaoId" : "id").asLong()).isEqualTo(n.composicao1());
        assertThat(item.path("contratoId").asLong()).isEqualTo(n.id());
        assertThat(item.path("lotacaoId").asLong()).isEqualTo(r.lotacao());
        assertThat(item.path("jornadaId").asLong()).isEqualTo(r.jornada());
        assertThat(item.path("cargoId").asLong()).isEqualTo(r.cargo());
        assertThat(item.path("efetivoPrevisto").asInt()).isEqualTo(quantidade);
        return item;
    }

    private void removerFotografiasPersistiveis(String fonte, long id) {
        em.flush();
        String hql = switch (fonte) {
            case "BASE" -> "update ComposicaoContratual set lotacaoNomeFotografia='', jornadaDescricaoFotografia=' ', "
                    + "cargoNomeFotografia='' where id=:id";
            case "ADITAMENTO" -> "update AditamentoComposicao set lotacaoNomeFotografia='', jornadaDescricaoFotografia=' ', "
                    + "cargoNomeFotografia='' where aditamento.id=:id";
            case "TRANSFERENCIA" -> "update MapeamentoTransferenciaGestao set lotacaoDestinoNomeFotografia='', "
                    + "jornadaDestinoDescricaoFotografia=' ', cargoDestinoNomeFotografia='' where transferencia.id=:id";
            default -> throw new AssertionError(fonte);
        };
        assertThat(em.createQuery(hql).setParameter("id", id).executeUpdate()).isEqualTo(1);
    }

    private long transferir(Cenario c, NovoContrato n, LocalDate data) throws Exception {
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        Map<String, Object> req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência documentada para prova das fotografias", "referenciaSei", "SEI-FOTO-" + n.id(),
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "CON023 transferência real com destino explícito");
        em.flush();
        return em.createQuery("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult();
    }

    private Recorte recorte(Lotacao lotacao, Jornada jornada, Cargo cargo) {
        return new Recorte(lotacao.getId(), jornada.getId(), cargo.getId(), lotacao.getNome(), jornada.getDescricao(), cargo.getNome());
    }

    private void renomear(Recorte r, long identificador) {
        em.find(Lotacao.class, r.lotacao()).setNome("Lotação atual renomeada " + identificador + "-" + r.lotacao());
        em.find(Jornada.class, r.jornada()).setDescricao("Jornada atual renomeada " + identificador + "-" + r.jornada());
        em.find(Cargo.class, r.cargo()).setNome("Cargo atual renomeado " + identificador + "-" + r.cargo());
    }

    private JsonNode porCargo(JsonNode itens, long id) {
        for (JsonNode item : itens) if (item.path("cargoId").asLong() == id) return item;
        throw new AssertionError("Cargo não encontrado no histórico: " + id);
    }

    private boolean ausente(String valor) { return valor == null || valor.isBlank(); }
}
