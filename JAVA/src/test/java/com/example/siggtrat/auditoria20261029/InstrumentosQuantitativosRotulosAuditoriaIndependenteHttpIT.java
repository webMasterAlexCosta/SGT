package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.AditamentoComposicao;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Proposta independente, ainda não executada: §5.9/5.10, CAT004 e HIS004.
 * Um delta quantitativo não documenta renomeação retroativa da combinação existente.
 * Combinação realmente nova de ADI preserva a fotografia própria do instrumento.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"ADI", "REM"})
    void instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, true);
        Rotulos originais = new Rotulos(c.lotacaoA.getNome(), c.jornadaA.getDescricao(), c.cargoA.getNome());
        assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(originais);
        assertThat(projecao(c, n.id(), c.cargoA.getId(), n.inicio())).isEqualTo(originais);
        Rotulos atuais = renomearMestres(c, c.cargoA.getId(), "POSTERIOR A PUBLICACAO " + n.id());
        assertThat(atuais).isNotEqualTo(originais);

        LocalDate dataAto = n.inicio().plusDays(2);
        Map<String, Object> pedido;
        if (tipo.equals("ADI")) {
            pedido = aditamento("ADI-SOMENTE-QUANTIDADE-ROTULOS", dataAto);
            pedido.put("novoEfetivoTotal", 11);
            pedido.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(),
                    "quantidadeResultante", 6)));
        } else {
            pedido = remanejamento(n, "REM-SOMENTE-QUANTIDADE-ROTULOS", dataAto, 1);
        }
        long instrumento = registrarInstrumento(c, n.id(), tipo, pedido);
        LocalDate dataAlocacao = dataAto.plusDays(1);
        assertThat(dataAlocacao).isBefore(RelogioSistema.hoje());

        // Coleta os três consumidores antes de comparar, para observar a propagação inteira.
        ItemDocumentado item = fotografiaInstrumento(tipo, instrumento, c.cargoA.getId());
        Rotulos projetados = projecao(c, n.id(), c.cargoA.getId(), dataAlocacao);
        long vinculo = cadastrar(c, n, dataAlocacao, n.composicao1(), c.cargoA.getId());
        Rotulos posicaoAlocacao = posicaoAlocacao(c, vinculo, dataAlocacao);
        Rotulos alocacaoPersistida = fotografiaAlocacao(vinculo);
        registrarEvidencia(mapa("tipo", tipo, "contratoId", n.id(), "instrumentoId", instrumento,
                "publicacao", originais, "mestresAtuais", atuais, "itemPersistido", item,
                "projecaoHttp", projetados, "alocacaoHttp", posicaoAlocacao, "alocacaoPersistida", alocacaoPersistida));
        assertAll(
                () -> assertThat(item.composicaoBaseId()).isEqualTo(n.composicao1()),
                // O item pode documentar o contexto conhecido no registro. Esse
                // dado é evidência, não autorização para renomear a combinação histórica.
                () -> assertThat(projetados).as("Projeção temporal depois do delta quantitativo").isEqualTo(originais),
                () -> assertThat(posicaoAlocacao).as("Posição HTTP da nova alocação retroativa").isEqualTo(originais),
                () -> assertThat(alocacaoPersistida).as("Fotografia relida em nova transação").isEqualTo(originais),
                () -> assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(originais),
                () -> assertThat(projecao(c, n.id(), c.cargoA.getId(), dataAto.minusDays(1))).isEqualTo(originais),
                () -> assertThat(fotografiaInstrumento(tipo, instrumento, c.cargoA.getId())).isEqualTo(item));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADI", "REM"})
    void instrumentoQuantitativoNaoCompletaRotulosLegadosAusentesComOsMestresAtuais(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, true);
        emTransacao(() -> {
            // As colunas são NOT NULL. Vazio é a representação legada já aceita
            // pelo schema e convertida em unknown por nomeFotografado; nenhum DDL.
            assertThat(em.createQuery("update ComposicaoContratual c set c.lotacaoNomeFotografia='', "
                            + "c.jornadaDescricaoFotografia='', c.cargoNomeFotografia='' where c.id in :ids")
                    .setParameter("ids", List.of(n.composicao1(), n.composicao2())).executeUpdate()).isEqualTo(2);
            return null;
        });
        Rotulos fonteFisicaAusente = new Rotulos("", "", "");
        Rotulos desconhecidos = new Rotulos(null, null, null);
        assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(fonteFisicaAusente);
        assertThat(projecao(c, n.id(), c.cargoA.getId(), n.inicio()))
                .as("A origem canônica representa a falta de fotografia antes de qualquer instrumento")
                .isEqualTo(desconhecidos);
        Rotulos atuais = renomearMestres(c, c.cargoA.getId(), "SEM PROVA DO PERIODO " + n.id());

        LocalDate dataAto = n.inicio().plusDays(2);
        Map<String, Object> pedido;
        if (tipo.equals("ADI")) {
            pedido = aditamento("ADI-QUANTIDADE-ROTULOS-AUSENTES", dataAto);
            pedido.put("novoEfetivoTotal", 11);
            pedido.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(),
                    "quantidadeResultante", 6)));
        } else {
            pedido = remanejamento(n, "REM-QUANTIDADE-ROTULOS-AUSENTES", dataAto, 1);
        }
        long instrumento = registrarInstrumento(c, n.id(), tipo, pedido);
        LocalDate dataAlocacao = dataAto.plusDays(1);
        assertThat(dataAlocacao).isBefore(RelogioSistema.hoje());
        ItemDocumentado item = fotografiaInstrumento(tipo, instrumento, c.cargoA.getId());
        Rotulos projetados = projecao(c, n.id(), c.cargoA.getId(), dataAlocacao);
        long vinculo = cadastrar(c, n, dataAlocacao, n.composicao1(), c.cargoA.getId());
        Rotulos posicaoAlocacao = posicaoAlocacao(c, vinculo, dataAlocacao);
        Rotulos alocacaoPersistida = fotografiaAlocacao(vinculo);
        registrarEvidencia(mapa("tipo", tipo, "origemAusente", true, "contratoId", n.id(),
                "instrumentoId", instrumento, "fonteFisica", fonteFisicaAusente, "mestresAtuais", atuais,
                "itemPersistido", item, "projecaoHttp", projetados, "alocacaoHttp", posicaoAlocacao,
                "alocacaoPersistida", alocacaoPersistida));
        assertAll(
                () -> assertThat(projetados).as("Quantidade não documenta os rótulos desconhecidos da origem")
                        .isEqualTo(desconhecidos),
                () -> assertThat(posicaoAlocacao).isEqualTo(desconhecidos),
                () -> assertThat(alocacaoPersistida).isEqualTo(desconhecidos),
                () -> assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(fonteFisicaAusente),
                () -> assertThat(fotografiaPublicada(n.composicao2())).isEqualTo(fonteFisicaAusente),
                () -> assertThat(fotografiaInstrumento(tipo, instrumento, c.cargoA.getId())).isEqualTo(item));
    }

    @Test
    void combinacaoRealmenteNovaDeAditamentoPreservaSeusPropriosRotulosDocumentados() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c, false);
        Rotulos originais = new Rotulos(c.lotacaoA.getNome(), c.jornadaA.getDescricao(), c.cargoA.getNome());
        assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(originais);
        Rotulos primeiraRenomeacao = renomearMestres(c, c.cargoA.getId(), "ANTES DO NOVO ADI " + n.id());

        LocalDate dataAto = n.inicio().plusDays(2);
        String nomeNovoCargo = "CARGO NOVO DOCUMENTADO NO ADI " + n.id();
        long cargoNovo = criarCargoComOrigemExplicita(c, nomeNovoCargo, n.inicio());
        assertThat(cargoNovo).isNotEqualTo(c.cargoA.getId());
        Rotulos documentados = new Rotulos(primeiraRenomeacao.lotacao(), primeiraRenomeacao.jornada(), nomeNovoCargo);
        var pedido = aditamento("ADI-CRIA-COMBINACAO-COM-ROTULOS-PROPRIOS", dataAto);
        pedido.put("novoEfetivoTotal", 7);
        pedido.put("alteracoesComposicao", List.of(mapa("lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "cargoId", cargoNovo, "quantidadeResultante", 2)));
        long instrumento = registrarInstrumento(c, n.id(), "ADI", pedido);
        ItemDocumentado item = fotografiaInstrumento("ADI", instrumento, cargoNovo);
        assertAll(
                () -> assertThat(item.composicaoBaseId()).as("A combinação não é uma composição-base preexistente").isNull(),
                () -> assertThat(item.rotulos()).isEqualTo(documentados));

        // Uma segunda renomeação distingue a fonte própria do ADI dos mestres usados depois dele.
        Rotulos posteriores = renomearMestres(c, cargoNovo, "DEPOIS DO NOVO ADI " + n.id());
        assertThat(posteriores).isNotEqualTo(documentados);
        LocalDate dataAlocacao = dataAto.plusDays(1);
        assertThat(dataAlocacao).isBefore(RelogioSistema.hoje());
        Rotulos projetados = projecao(c, n.id(), cargoNovo, dataAlocacao);
        long vinculo = cadastrar(c, n, dataAlocacao, null, cargoNovo);
        Rotulos posicaoAlocacao = posicaoAlocacao(c, vinculo, dataAlocacao);
        Rotulos alocacaoPersistida = fotografiaAlocacao(vinculo);
        assertAll(
                () -> assertThat(projetados).isEqualTo(documentados),
                () -> assertThat(posicaoAlocacao).isEqualTo(documentados),
                () -> assertThat(alocacaoPersistida).isEqualTo(documentados),
                () -> assertThat(fotografiaInstrumento("ADI", instrumento, cargoNovo)).isEqualTo(item),
                () -> assertThat(fotografiaPublicada(n.composicao1())).isEqualTo(originais),
                () -> assertThat(projecao(c, n.id(), c.cargoA.getId(), dataAlocacao)).isEqualTo(originais));
    }

    private NovoContrato publicar(Cenario c, boolean duasComposicoes) {
        return emTransacao(() -> {
            try { return publicado(c, duasComposicoes ? 10 : 5, duasComposicoes ? 5 : null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private void registrarEvidencia(Object evidencia) throws Exception {
        var arquivo = java.nio.file.Path.of("target/auditoria-http/rotulos-instrumentos-quantitativos.jsonl");
        java.nio.file.Files.createDirectories(arquivo.getParent());
        java.nio.file.Files.writeString(arquivo, json(evidencia) + "\n", java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private Rotulos renomearMestres(Cenario c, long cargoId, String marco) throws Exception {
        Rotulos novos = new Rotulos("LOTACAO " + marco, "JORNADA " + marco, "CARGO " + marco);
        renomear(c, "lotacoes", c.lotacaoA.getId(), "nome", novos.lotacao());
        renomear(c, "jornadas", c.jornadaA.getId(), "descricao", novos.jornada());
        renomear(c, "cargos", cargoId, "nome", novos.cargo());
        return novos;
    }

    private void renomear(Cenario c, String recurso, long id, String campo, String valor) throws Exception {
        String rota = "/api/" + recurso + "/" + id;
        ObjectNode completo = (ObjectNode) consultar(c.localA, rota,
                "Preserva atributos, escopo e disponibilidade ao alterar apenas o rótulo").deepCopy();
        completo.put(campo, valor);
        executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON).content(json(completo)), 200,
                "Renomeação atual do catálogo não é retificação retroativa de composição");
        // Literais em maiúsculas preservam a comparação exata após normalização da Jornada.
        assertThat(consultar(c.localA, rota, "Releitura após commit confirma o novo rótulo normalizado")
                .path(campo).asText()).isEqualTo(valor);
    }

    private long criarCargoComOrigemExplicita(Cenario c, String nome, LocalDate inicio) throws Exception {
        long id = enviar(c.localA, "/api/cargos", mapa("nome", nome, "cbo", "1", "ativo", true,
                "gestaoLocalId", c.gestaoA.getId()), 200,
                "Cargo realmente novo para uma combinação não existente na publicação").path("id").asLong();
        assertThat(consultar(c.localA, "/api/cargos/" + id, "Nome próprio do novo Cargo foi confirmado")
                .path("nome").asText()).isEqualTo(nome);
        emTransacao(() -> {
            // Fonte sintética explícita da fixture: não infere validade passada pela flag atual.
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(id);
            origem.setDataEfetiva(inicio); origem.setAtivo(true);
            origem.setMotivo("Documento sintético comprova disponibilidade do novo Cargo desde o período do ADI");
            em.persist(origem); em.flush();
            return null;
        });
        return id;
    }

    private long registrarInstrumento(Cenario c, long contrato, String tipo, Map<String, Object> pedido) throws Exception {
        String rota = base(contrato) + (tipo.equals("ADI") ? "/aditamentos" : "/remanejamentos");
        JsonNode previa = enviar(c.localA, rota + "/previa", pedido, 200, "Prévia real do instrumento");
        pedido.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, rota, pedido, 201, "Instrumento confirmado com escopo de alteração explícito")
                .path("id").asLong();
    }

    private long cadastrar(Cenario c, NovoContrato n, LocalDate data, Long composicao, long cargo) throws Exception {
        Map<String, Object> pedido = mapa("pessoa", mapa("nome", "Pessoa da revisão de rótulos " + n.id(),
                        "cpf", cpf(Math.toIntExact(706000000L + n.id()))), "contratoId", n.id(), "dataInicio", data);
        if (composicao != null) pedido.put("composicaoId", composicao);
        else {
            pedido.put("lotacaoId", c.lotacaoA.getId()); pedido.put("jornadaId", c.jornadaA.getId()); pedido.put("cargoId", cargo);
        }
        return enviar(c.localA, "/api/vinculos-contratuais", pedido, 201,
                "Nova alocação usa os rótulos comprovados do recorte na data efetiva").path("id").asLong();
    }

    private Rotulos projecao(Cenario c, long contrato, long cargo, LocalDate data) throws Exception {
        JsonNode composicoes = consultar(c.localA, base(contrato) + "/composicoes?data=" + data,
                "Composição temporal HTTP após os instrumentos conhecidos");
        JsonNode encontrada = null;
        for (JsonNode item : composicoes) {
            if (item.path("lotacaoId").asLong() == c.lotacaoA.getId()
                    && item.path("jornadaId").asLong() == c.jornadaA.getId()
                    && item.path("cargoId").asLong() == cargo) {
                assertThat(encontrada).as("Combinação temporal única").isNull();
                encontrada = item;
            }
        }
        assertThat(encontrada).as("A combinação esperada deve existir na projeção").isNotNull();
        return rotulosHttp(encontrada);
    }

    private Rotulos posicaoAlocacao(Cenario c, long vinculo, LocalDate data) throws Exception {
        return rotulosHttp(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/alocacoes/posicao?data=" + data,
                "Posição HTTP da alocação retroativa confirmada"));
    }

    private Rotulos rotulosHttp(JsonNode item) {
        return new Rotulos(textoHistorico(item, "lotacaoNome"), textoHistorico(item, "jornadaDescricao"),
                textoHistorico(item, "cargoNome"));
    }

    private String textoHistorico(JsonNode item, String campo) {
        assertThat(item.has(campo)).as("Fotografia conhecida ou null permanece explícita: %s", campo).isTrue();
        return item.get(campo).isNull() ? null : item.get(campo).asText();
    }

    private Rotulos fotografiaPublicada(long id) {
        return emTransacao(() -> {
            ComposicaoContratual c = em.find(ComposicaoContratual.class, id);
            return new Rotulos(c.getLotacaoNomeFotografia(), c.getJornadaDescricaoFotografia(), c.getCargoNomeFotografia());
        });
    }

    private Rotulos fotografiaAlocacao(long vinculo) {
        return emTransacao(() -> {
            AlocacaoVinculo a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).getSingleResult();
            return new Rotulos(a.getLotacaoNomeFotografia(), a.getJornadaDescricaoFotografia(), a.getCargoNomeFotografia());
        });
    }

    private ItemDocumentado fotografiaInstrumento(String tipo, long instrumento, long cargo) {
        return emTransacao(() -> {
            if (tipo.equals("ADI")) {
                AditamentoComposicao i = em.createQuery("select i from AditamentoComposicao i "
                                + "where i.aditamento.id=:id and i.cargo.id=:cargo", AditamentoComposicao.class)
                        .setParameter("id", instrumento).setParameter("cargo", cargo).getSingleResult();
                return new ItemDocumentado(i.getComposicaoBase() == null ? null : i.getComposicaoBase().getId(),
                        new Rotulos(i.getLotacaoNomeFotografia(), i.getJornadaDescricaoFotografia(), i.getCargoNomeFotografia()));
            }
            RemanejamentoComposicao i = em.createQuery("select i from RemanejamentoComposicao i "
                            + "where i.remanejamento.id=:id and i.cargo.id=:cargo", RemanejamentoComposicao.class)
                    .setParameter("id", instrumento).setParameter("cargo", cargo).getSingleResult();
            return new ItemDocumentado(i.getComposicaoBase() == null ? null : i.getComposicaoBase().getId(),
                    new Rotulos(i.getLotacaoNomeFotografia(), i.getJornadaDescricaoFotografia(), i.getCargoNomeFotografia()));
        });
    }

    private record Rotulos(String lotacao, String jornada, String cargo) {}
    private record ItemDocumentado(Long composicaoBaseId, Rotulos rotulos) {}
}
