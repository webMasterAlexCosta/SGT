package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.AditamentoComposicao;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.RemanejamentoComposicao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * TMP004/HIS004: configuração publicada não se transforma em configuração do mestre atual.
 * ADI quantitativo e REM alteram quantidades; combinação nova de ADI documenta sua própria configuração.
 * As leituras JPA são feitas em transações posteriores às operações HTTP já confirmadas.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InstrumentosConfiguracaoHistoricaHttpIT extends ContratosHttpBase {
    private static final String GRADE_8H = "MONDAY|true|08:00|12:00|13:00|17:00;"
            + "TUESDAY|true|08:00|12:00|13:00|17:00;"
            + "WEDNESDAY|true|08:00|12:00|13:00|17:00;"
            + "THURSDAY|true|08:00|12:00|13:00|17:00;"
            + "FRIDAY|true|08:00|12:00|13:00|17:00;"
            + "SATURDAY|false||||;SUNDAY|false||||";

    @Test
    void combinacaoNovaDeAditamentoPreservaConfiguracaoPropriaDepoisDaMudancaDoMestre() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, false);
        String municipioOriginal = c.lotacaoA.getMunicipio();
        Foto fontePublicada = fotoComposicao(n.composicao1());
        long cargoNovo = criarCargoComOrigemExplicita(c, n.inicio());
        LocalDate dataAto = n.inicio().plusDays(2);
        var ato = aditamento("ADI-CONFIGURACAO-NOVA", dataAto);
        ato.put("novoEfetivoTotal", 7);
        ato.put("alteracoesComposicao", List.of(mapa("lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "cargoId", cargoNovo, "quantidadeResultante", 2)));
        long aditamento = registrarInstrumento(c, n.id(), "ADI", ato);
        List<FotoItem> fonteInstrumento = fotosInstrumento("ADI", aditamento);
        assertThat(fonteInstrumento).hasSize(1);
        assertAll(
                () -> assertThat(fonteInstrumento.get(0).composicaoBaseId()).isNull(),
                () -> assertThat(fonteInstrumento.get(0).grade()).isEqualTo(GRADE_8H),
                () -> assertThat(fonteInstrumento.get(0).municipio()).isEqualTo(municipioOriginal),
                () -> assertThat(fonteInstrumento.get(0).anterior()).isZero(),
                () -> assertThat(fonteInstrumento.get(0).resultante()).isEqualTo(2));
        JsonNode historicoAntes = semIndicadoresDeOcupacao(consultar(c.localA, base(n.id()) + "/aditamentos",
                "Instrumento original confirmado antes da mudança administrativa"));

        alterarMestresParaSeisHorasEOutroMunicipio(c);
        LocalDate data = primeiraSegundaDepois(dataAto);
        long vinculo = cadastrar(c, n, data, null, cargoNovo);
        conferirAlocacaoEApuracaoOitoHoras(c, n, vinculo, data, municipioOriginal);

        assertAll(
                () -> assertThat(fotosInstrumento("ADI", aditamento)).isEqualTo(fonteInstrumento),
                () -> assertThat(fotoComposicao(n.composicao1())).isEqualTo(fontePublicada),
                () -> assertThat(semIndicadoresDeOcupacao(consultar(c.localA, base(n.id()) + "/aditamentos",
                        "Leitura e nova alocação não reescrevem o aditamento"))).isEqualTo(historicoAntes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADI", "REM"})
    void instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, true);
        String municipioOriginal = c.lotacaoA.getMunicipio();
        Foto fontePublicada = fotoComposicao(n.composicao1());
        assertThat(fontePublicada.grade()).isEqualTo(GRADE_8H);
        alterarMestresParaSeisHorasEOutroMunicipio(c);

        // O documento modifica só quantitativos. Selecionar a composição não declara revisão de horários.
        LocalDate dataAto = n.inicio().plusDays(2);
        Map<String, Object> ato;
        if (tipo.equals("ADI")) {
            ato = aditamento("ADI-SOMENTE-EFETIVO", dataAto);
            ato.put("novoEfetivoTotal", 11);
            ato.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(),
                    "quantidadeResultante", 6)));
        } else {
            ato = remanejamento(n, "REM-SOMENTE-EFETIVO", dataAto, 1);
        }
        long instrumento = registrarInstrumento(c, n.id(), tipo, ato);
        List<FotoItem> fonteInstrumento = fotosInstrumento(tipo, instrumento);
        assertThat(fonteInstrumento).hasSize(tipo.equals("ADI") ? 1 : 2);
        assertThat(fonteInstrumento).allSatisfy(item -> {
            assertThat(item.grade()).isEqualTo(GRADE_8H);
            assertThat(item.municipio()).isEqualTo(municipioOriginal);
        });
        String rota = base(n.id()) + (tipo.equals("ADI") ? "/aditamentos" : "/remanejamentos");
        JsonNode historicoAntes = semIndicadoresDeOcupacao(consultar(c.localA, rota, "Instrumento quantitativo confirmado"));
        LocalDate data = primeiraSegundaDepois(dataAto);
        long vinculo = cadastrar(c, n, data, n.composicao1(), c.cargoA.getId());
        conferirAlocacaoEApuracaoOitoHoras(c, n, vinculo, data, municipioOriginal);

        assertAll(
                () -> assertThat(fotosInstrumento(tipo, instrumento)).isEqualTo(fonteInstrumento),
                () -> assertThat(fotoComposicao(n.composicao1())).isEqualTo(fontePublicada),
                () -> assertThat(semIndicadoresDeOcupacao(consultar(c.localA, rota,
                        "Projeção e apuração não alteram as versões do instrumento"))).isEqualTo(historicoAntes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADI", "REM"})
    void instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, true);
        emTransacao(() -> {
            // DML de fixture simula somente as novas colunas ausentes no acervo publicado.
            // IDs, rótulos, período, disponibilidade e quantidades permanecem íntegros.
            assertThat(em.createQuery("update ComposicaoContratual c set c.jornadaSemanalFotografia=null, "
                            + "c.lotacaoMunicipioFotografia=null where c.id in :ids")
                    .setParameter("ids", List.of(n.composicao1(), n.composicao2())).executeUpdate()).isEqualTo(2);
            return null;
        });
        Foto primeiraPublicada = fotoComposicao(n.composicao1());
        Foto segundaPublicada = fotoComposicao(n.composicao2());
        assertThat(primeiraPublicada).isEqualTo(new Foto(null, null));
        assertThat(segundaPublicada).isEqualTo(new Foto(null, null));
        alterarMestresParaSeisHorasEOutroMunicipio(c);

        LocalDate dataAto = n.inicio().plusDays(2);
        Map<String, Object> ato;
        if (tipo.equals("ADI")) {
            ato = aditamento("ADI-QUANTIDADE-SEM-CONFIGURACAO", dataAto);
            ato.put("novoEfetivoTotal", 11);
            ato.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(),
                    "quantidadeResultante", 6)));
        } else {
            ato = remanejamento(n, "REM-QUANTIDADE-SEM-CONFIGURACAO", dataAto, 1);
        }
        long instrumento = registrarInstrumento(c, n.id(), tipo, ato);
        List<FotoItem> fonteInstrumento = fotosInstrumento(tipo, instrumento);
        assertThat(fonteInstrumento).hasSize(tipo.equals("ADI") ? 1 : 2);
        assertThat(fonteInstrumento).allSatisfy(item -> {
            assertThat(item.grade()).isNull();
            assertThat(item.municipio()).isNull();
        });
        String rota = base(n.id()) + (tipo.equals("ADI") ? "/aditamentos" : "/remanejamentos");
        JsonNode historicoAntes = semIndicadoresDeOcupacao(consultar(c.localA, rota,
                "Instrumento quantitativo não cria a configuração histórica que a origem não preservou"));
        LocalDate data = primeiraSegundaDepois(dataAto);
        long vinculo = cadastrar(c, n, data, n.composicao1(), c.cargoA.getId());
        conferirAusenciaSemApuracao(c, n, vinculo, data);

        assertAll(
                () -> assertThat(fotosInstrumento(tipo, instrumento)).isEqualTo(fonteInstrumento),
                () -> assertThat(fotoComposicao(n.composicao1())).isEqualTo(primeiraPublicada),
                () -> assertThat(fotoComposicao(n.composicao2())).isEqualTo(segundaPublicada),
                () -> assertThat(semIndicadoresDeOcupacao(consultar(c.localA, rota,
                        "Ausência permanece sem preencher o acervo após criação e tentativas de apuração")))
                        .isEqualTo(historicoAntes));
    }

    @Test
    void combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, false);
        Foto fontePublicada = fotoComposicao(n.composicao1());
        assertThat(fontePublicada.grade()).isEqualTo(GRADE_8H);
        assertThat(fontePublicada.municipio()).isEqualTo(c.lotacaoA.getMunicipio());
        long cargoNovo = criarCargoComOrigemExplicita(c, n.inicio());
        LocalDate dataAto = n.inicio().plusDays(2);
        var ato = aditamento("ADI-NOVO-ITEM-LEGADO", dataAto);
        ato.put("novoEfetivoTotal", 7);
        ato.put("alteracoesComposicao", List.of(mapa("lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "cargoId", cargoNovo, "quantidadeResultante", 2)));
        long aditamento = registrarInstrumento(c, n.id(), "ADI", ato);
        emTransacao(() -> {
            // Simula a lacuna somente no item que é a origem da combinação nova.
            // A outra composição mantém 8h comprovadas no MESMO catálogo, sem ser fonte deste fato.
            assertThat(em.createQuery("update AditamentoComposicao i set i.jornadaSemanalFotografia=null, "
                            + "i.lotacaoMunicipioFotografia=null where i.aditamento.id=:id")
                    .setParameter("id", aditamento).executeUpdate()).isEqualTo(1);
            return null;
        });
        List<FotoItem> fonteInstrumento = fotosInstrumento("ADI", aditamento);
        assertThat(fonteInstrumento).hasSize(1);
        assertAll(
                () -> assertThat(fonteInstrumento.get(0).composicaoBaseId()).isNull(),
                () -> assertThat(fonteInstrumento.get(0).grade()).isNull(),
                () -> assertThat(fonteInstrumento.get(0).municipio()).isNull());
        JsonNode historicoAntes = semIndicadoresDeOcupacao(consultar(c.localA, base(n.id()) + "/aditamentos",
                "Instrumento legado conserva dados conhecidos e não preenche a configuração ausente"));
        alterarMestresParaSeisHorasEOutroMunicipio(c);
        LocalDate data = primeiraSegundaDepois(dataAto);
        long vinculo = cadastrar(c, n, data, null, cargoNovo);
        conferirAusenciaSemApuracao(c, n, vinculo, data);

        assertAll(
                () -> assertThat(fotosInstrumento("ADI", aditamento)).isEqualTo(fonteInstrumento),
                () -> assertThat(fotoComposicao(n.composicao1())).isEqualTo(fontePublicada),
                () -> assertThat(semIndicadoresDeOcupacao(consultar(c.localA, base(n.id()) + "/aditamentos",
                        "Consulta e apuração não emprestam a configuração da outra composição"))).isEqualTo(historicoAntes));
    }

    @Test
    void calendarioMunicipalUsaMunicipioOriginalDaNovaAlocacaoRetroativa() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, false);
        String municipioOriginal = c.lotacaoA.getMunicipio();
        Foto fontePublicada = fotoComposicao(n.composicao1());
        LocalDate feriadoOriginal = n.inicio().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate diaNormalOriginal = feriadoOriginal.plusDays(1);
        long calendarioOriginal = criarFeriadoMunicipal(c, feriadoOriginal, municipioOriginal);
        String municipioAtual = alterarMestresParaSeisHorasEOutroMunicipio(c);
        // Este feriado pertence somente ao município atual; não deve atingir a alocação antiga.
        long calendarioAtual = criarFeriadoMunicipal(c, diaNormalOriginal, municipioAtual);
        long vinculo = cadastrar(c, n, feriadoOriginal, n.composicao1(), c.cargoA.getId());

        Foto alocacao = fotoAlocacao(vinculo);
        JsonNode antes = relatorio(c, n, feriadoOriginal, diaNormalOriginal);
        JsonNode feriado = dia(antes, feriadoOriginal);
        JsonNode normal = dia(antes, diaNormalOriginal);
        assertAll(
                () -> assertThat(alocacao.grade()).isEqualTo(GRADE_8H),
                () -> assertThat(alocacao.municipio()).isEqualTo(municipioOriginal),
                () -> assertThat(feriado.path("status").asText()).isEqualTo("FERIADO"),
                () -> assertThat(feriado.path("minutosPrevistos").asInt()).isZero(),
                () -> assertThat(normal.path("status").asText()).isEqualTo("SEM_LANCAMENTO"),
                () -> assertThat(normal.path("minutosPrevistos").asInt()).isEqualTo(480));

        registrarPresencaEConferir(c, vinculo, diaNormalOriginal, municipioOriginal);
        JsonNode depois = relatorio(c, n, feriadoOriginal, diaNormalOriginal);
        assertAll(
                () -> assertThat(dia(depois, feriadoOriginal)).isEqualTo(feriado),
                () -> assertThat(dia(depois, diaNormalOriginal).path("minutosPrevistos").asInt()).isEqualTo(480),
                () -> assertThat(dia(depois, diaNormalOriginal).path("minutosFisicos").asInt()).isEqualTo(480),
                () -> assertThat(dia(depois, diaNormalOriginal).path("status").asText()).isEqualTo("PRESENCA"),
                () -> assertThat(fotoComposicao(n.composicao1())).isEqualTo(fontePublicada),
                () -> assertThat(fotoAlocacao(vinculo)).isEqualTo(alocacao));
        emTransacao(() -> {
            assertThat(em.find(CalendarioEvento.class, calendarioOriginal).getMunicipio()).isEqualTo(municipioOriginal);
            assertThat(em.find(CalendarioEvento.class, calendarioAtual).getMunicipio()).isEqualTo(municipioAtual);
            return null;
        });
    }

    private NovoContrato publicar(Cenario c, boolean duasComposicoes) {
        return emTransacao(() -> {
            try { return publicado(c, duasComposicoes ? 10 : 5, duasComposicoes ? 5 : null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private long criarCargoComOrigemExplicita(Cenario c, LocalDate inicio) {
        return emTransacao(() -> {
            Cargo cargo = new Cargo("Cargo da combinação nova " + UUID.randomUUID(), "1");
            cargo.setGestaoLocal(em.find(com.example.siggtrat.entidade.GestaoLocal.class, c.gestaoA.getId()));
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(cargo.getId());
            origem.setDataEfetiva(inicio); origem.setAtivo(true);
            origem.setMotivo("Origem sintética explícita do Cargo no período do documento de aditamento");
            em.persist(origem); em.flush();
            return cargo.getId();
        });
    }

    private long registrarInstrumento(Cenario c, long contrato, String tipo, Map<String, Object> ato) throws Exception {
        String rota = base(contrato) + (tipo.equals("ADI") ? "/aditamentos" : "/remanejamentos");
        JsonNode previa = enviar(c.localA, rota + "/previa", ato, 200,
                "Prévia real do instrumento antes da confirmação");
        ato.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, rota, ato, 201, "Instrumento confirmado com a configuração de sua fonte")
                .path("id").asLong();
    }

    private String alterarMestresParaSeisHorasEOutroMunicipio(Cenario c) throws Exception {
        String rotaJornada = "/api/jornadas/" + c.jornadaA.getId();
        ObjectNode jornada = (ObjectNode) consultar(c.localA, rotaJornada,
                "Preserva o DTO integral da Jornada ao mudar somente a saída da tarde").deepCopy();
        for (JsonNode dia : jornada.path("dias")) {
            if (dia.path("diaTrabalho").asBoolean()) ((ObjectNode) dia).put("saida2", "15:00");
        }
        executar(c.localA, put(rotaJornada).contentType(MediaType.APPLICATION_JSON).content(json(jornada)), 200,
                "Mudança atual de 8h para 6h não retifica os instrumentos passados");
        JsonNode diasAtuais = consultar(c.localA, rotaJornada,
                "Releitura confirma sete dias e a grade atual efetivamente alterada para 6h").path("dias");
        assertThat(diasAtuais.isArray()).isTrue();
        assertThat(diasAtuais.size()).isEqualTo(7);
        EnumSet<DayOfWeek> diasEncontrados = EnumSet.noneOf(DayOfWeek.class);
        for (JsonNode dia : diasAtuais) {
            DayOfWeek semana = DayOfWeek.valueOf(dia.path("diaSemana").asText());
            assertThat(diasEncontrados.add(semana)).as("Dia da semana único na grade: %s", semana).isTrue();
            if (semana.getValue() <= 5) {
                assertAll(
                        () -> assertThat(dia.path("diaTrabalho").asBoolean()).isTrue(),
                        () -> assertThat(dia.path("entrada1").asText()).isEqualTo("08:00"),
                        () -> assertThat(dia.path("saida1").asText()).isEqualTo("12:00"),
                        () -> assertThat(dia.path("entrada2").asText()).isEqualTo("13:00"),
                        () -> assertThat(dia.path("saida2").asText()).isEqualTo("15:00"));
            } else {
                assertAll(
                        () -> assertThat(dia.path("diaTrabalho").asBoolean()).isFalse(),
                        () -> assertThat(dia.path("entrada1").isNull()).isTrue(),
                        () -> assertThat(dia.path("saida1").isNull()).isTrue(),
                        () -> assertThat(dia.path("entrada2").isNull()).isTrue(),
                        () -> assertThat(dia.path("saida2").isNull()).isTrue());
            }
        }
        assertThat(diasEncontrados).containsExactlyInAnyOrder(DayOfWeek.values());
        String rotaLotacao = "/api/lotacoes/" + c.lotacaoA.getId();
        ObjectNode lotacao = (ObjectNode) consultar(c.localA, rotaLotacao,
                "Preserva o DTO integral da Lotação ao mudar somente o município").deepCopy();
        String atual = "MUNICIPIO ATUAL CONFIGURACAO " + c.lotacaoA.getId();
        lotacao.put("municipio", atual);
        executar(c.localA, put(rotaLotacao).contentType(MediaType.APPLICATION_JSON).content(json(lotacao)), 200,
                "Município atual não substitui o município dos instrumentos passados");
        assertThat(consultar(c.localA, rotaLotacao, "Releitura confirma o município atual após o commit")
                .path("municipio").asText()).isEqualTo(atual);
        return atual;
    }

    private JsonNode semIndicadoresDeOcupacao(JsonNode instrumentos) {
        assertThat(instrumentos.isArray()).isTrue();
        JsonNode copia = instrumentos.deepCopy();
        // paraDTO calcula esses três valores a partir da ocupação atual. Somente
        // eles podem mudar com o novo vínculo; todo o restante continua comparado.
        for (JsonNode instrumento : copia) {
            for (String colecao : List.of("alteracoesComposicao", "itens")) {
                JsonNode itens = instrumento.get(colecao);
                if (itens == null) continue;
                assertThat(itens.isArray()).isTrue();
                for (JsonNode item : itens) {
                    assertThat(item.isObject()).isTrue();
                    ((ObjectNode) item).remove(List.of("colaboradoresAtuais", "excedenteResultante", "exigeTratamento"));
                }
            }
        }
        return copia;
    }

    private LocalDate primeiraSegundaDepois(LocalDate dataAto) {
        LocalDate data = dataAto.plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(data).isBefore(RelogioSistema.hoje());
        return data;
    }

    private long cadastrar(Cenario c, NovoContrato n, LocalDate data, Long composicao, long cargo) throws Exception {
        Map<String, Object> corpo = mapa("pessoa", mapa("nome", "Pessoa da configuração instrumental " + n.id(),
                        "cpf", cpf(Math.toIntExact(695000000L + n.id()))),
                "contratoId", n.id(), "dataInicio", data);
        if (composicao != null) corpo.put("composicaoId", composicao);
        else {
            corpo.put("lotacaoId", c.lotacaoA.getId()); corpo.put("jornadaId", c.jornadaA.getId()); corpo.put("cargoId", cargo);
        }
        return enviar(c.localA, "/api/vinculos-contratuais", corpo, 201,
                "Nova alocação retroativa usa a configuração documental da combinação na data")
                .path("id").asLong();
    }

    private void conferirAlocacaoEApuracaoOitoHoras(Cenario c, NovoContrato n, long vinculo, LocalDate data,
                                                   String municipioOriginal) throws Exception {
        Foto fotografia = fotoAlocacao(vinculo);
        JsonNode dia = dia(relatorio(c, n, data, data), data);
        assertAll(
                () -> assertThat(fotografia.grade()).isEqualTo(GRADE_8H),
                () -> assertThat(fotografia.municipio()).isEqualTo(municipioOriginal),
                () -> assertThat(dia.path("status").asText()).isEqualTo("SEM_LANCAMENTO"),
                () -> assertThat(dia.path("minutosPrevistos").asInt()).isEqualTo(480));
        registrarPresencaEConferir(c, vinculo, data, municipioOriginal);
        JsonNode depois = dia(relatorio(c, n, data, data), data);
        assertAll(
                () -> assertThat(depois.path("status").asText()).isEqualTo("PRESENCA"),
                () -> assertThat(depois.path("minutosPrevistos").asInt()).isEqualTo(480),
                () -> assertThat(depois.path("minutosFisicos").asInt()).isEqualTo(480),
                () -> assertThat(fotoAlocacao(vinculo)).isEqualTo(fotografia));
    }

    private void conferirAusenciaSemApuracao(Cenario c, NovoContrato n, long vinculo, LocalDate data) throws Exception {
        Foto fotografia = fotoAlocacao(vinculo);
        assertThat(fotografia).isEqualTo(new Foto(null, null));
        JsonNode relatorio = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", data.toString()).param("fim", data.toString())
                .param("contratoIds", Long.toString(n.id())), 409,
                "Sem fonte histórica da grade, o relatório bloqueia a apuração em vez de usar o mestre"));
        assertThat(relatorio.toString()).containsIgnoringCase("fotografia");
        JsonNode frequencia = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", vinculo,
                "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Presença não recupera configuração histórica ausente",
                "chaveIdempotencia", "instrumentos-sem-configuracao-" + UUID.randomUUID()), 409,
                "A frequência não pode fotografar os horários atuais como prova do período antigo");
        assertThat(frequencia.toString()).containsIgnoringCase("fotografia");
        long quantidade = emTransacao(() -> em.createQuery(
                        "select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                .setParameter("v", vinculo).getSingleResult());
        assertAll(
                () -> assertThat(quantidade).isZero(),
                () -> assertThat(fotoAlocacao(vinculo)).isEqualTo(fotografia));
    }

    private void registrarPresencaEConferir(Cenario c, long vinculo, LocalDate data, String municipio) throws Exception {
        JsonNode resposta = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", vinculo,
                "dataReferencia", data, "status", "PRESENCA",
                "justificativaRetificacao", "Presença sem alteração de configuração documental do período",
                "chaveIdempotencia", "instrumentos-configuracao-" + UUID.randomUUID()), 200,
                "A apuração da presença usa a Jornada e o município históricos");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, resposta.path("id").asLong());
            assertAll(
                    () -> assertThat(f.getMinutosFisicos()).isEqualTo(480),
                    () -> assertThat(f.getLotacaoSnapshotMunicipio()).isEqualTo(municipio),
                    () -> assertThat(f.getJornadaSnapshotEntrada1()).isEqualTo("08:00"),
                    () -> assertThat(f.getJornadaSnapshotSaida1()).isEqualTo("12:00"),
                    () -> assertThat(f.getJornadaSnapshotEntrada2()).isEqualTo("13:00"),
                    () -> assertThat(f.getJornadaSnapshotSaida2()).isEqualTo("17:00"));
            return null;
        });
    }

    private long criarFeriadoMunicipal(Cenario c, LocalDate data, String municipio) throws Exception {
        return enviar(c.localA, "/api/calendario-eventos", mapa("data", data, "tipo", "FERIADO",
                "descricao", "Feriado do município documentado", "abrangencia", "MUNICIPAL",
                "municipio", municipio, "ativo", true), 201,
                "Fonte de calendário municipal publicada no escopo da Gestão Local").path("id").asLong();
    }

    private JsonNode relatorio(Cenario c, NovoContrato n, LocalDate inicio, LocalDate fim) throws Exception {
        JsonNode resposta = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", inicio.toString()).param("fim", fim.toString())
                .param("contratoIds", Long.toString(n.id())), 200,
                "Relatório aplica a configuração comprovada e o calendário do município histórico"));
        assertThat(resposta.path("registros").size()).isEqualTo(1);
        return resposta.path("registros").get(0);
    }

    private JsonNode dia(JsonNode linha, LocalDate data) { return linha.path("diario").path(data.toString()); }

    private Foto fotoComposicao(long composicao) {
        return emTransacao(() -> {
            ComposicaoContratual c = em.find(ComposicaoContratual.class, composicao);
            return new Foto(c.getJornadaSemanalFotografia(), c.getLotacaoMunicipioFotografia());
        });
    }

    private Foto fotoAlocacao(long vinculo) {
        return emTransacao(() -> {
            AlocacaoVinculo a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).getSingleResult();
            return new Foto(a.getJornadaSemanalFotografia(), a.getLotacaoMunicipioFotografia());
        });
    }

    private List<FotoItem> fotosInstrumento(String tipo, long instrumento) {
        return emTransacao(() -> {
            if (tipo.equals("ADI")) {
                return em.createQuery("select i from AditamentoComposicao i where i.aditamento.id=:id order by i.id", AditamentoComposicao.class)
                        .setParameter("id", instrumento).getResultList().stream()
                        .map(i -> new FotoItem(i.getId(), i.getComposicaoBase() == null ? null : i.getComposicaoBase().getId(),
                                i.getCargo().getId(), i.getJornadaSemanalFotografia(), i.getLotacaoMunicipioFotografia(),
                                i.getQuantidadeAnterior(), i.getVariacao(), i.getQuantidadeResultante())).toList();
            }
            return em.createQuery("select i from RemanejamentoComposicao i where i.remanejamento.id=:id order by i.id", RemanejamentoComposicao.class)
                    .setParameter("id", instrumento).getResultList().stream()
                    .map(i -> new FotoItem(i.getId(), i.getComposicaoBase() == null ? null : i.getComposicaoBase().getId(),
                            i.getCargo().getId(), i.getJornadaSemanalFotografia(), i.getLotacaoMunicipioFotografia(),
                            i.getQuantidadeAnterior(), i.getVariacao(), i.getQuantidadeResultante())).toList();
        });
    }

    private record Foto(String grade, String municipio) {}
    private record FotoItem(Long id, Long composicaoBaseId, Long cargoId, String grade, String municipio,
                            Integer anterior, Integer variacao, Integer resultante) {}
}
