package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** CON-023:001: mapa não vazio deve cobrir todas as composições; PostgreSQL e filtros reais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaMapaIntegralHttpIT extends ContratosHttpBase {
    @Autowired private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void mapaParcialNaoConsomeConcessaoNemPublicaECompletarSomenteMapaFaltantePermite(int primeiroMapeado) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, 5, true); }
            catch (Exception falha) { throw new IllegalStateException("Preparação sintética publicada", falha); }
        });
        assertNotNull(n.composicao2());
        long outroCargoDestino = emTransacao(() -> {
            Cargo cargo = new Cargo(); cargo.setGestaoLocal(c.gestaoB);
            cargo.setNome("Cargo destino sintético mapa integral " + UUID.randomUUID()); cargo.setAtivo(true);
            em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo fonte = new EventoDisponibilidadeCatalogo();
            fonte.setTipoCatalogo(TipoCatalogoTemporal.CARGO); fonte.setCatalogoId(cargo.getId());
            fonte.setDataEfetiva(n.inicio()); fonte.setAtivo(true);
            fonte.setMotivo("Fonte explicitamente sintética de disponibilidade no período da transferência");
            em.persist(fonte); return cargo.getId();
        });
        LocalDate efeito = n.inicio().plusDays(1);
        var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + efeito,
                "Preparação tem exatamente duas composições operacionais");
        assertEquals(2, composicoes.size());
        assertTrue(java.util.stream.StreamSupport.stream(composicoes.spliterator(), false)
                .allMatch(comp -> comp.path("efetivoPrevisto").asInt() == 5));
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var mapas = List.of(mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()),
                mapa("composicaoOrigemId", n.composicao2(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", outroCargoDestino));
        var pedido = mapa("conferenciaDocumentalConfirmada", true,
                "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", efeito,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência sintética com mapa integral",
                "referenciaSei", "SEI-MAPA-INTEGRAL-SINTETICO", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapas.get(primeiroMapeado)));
        Map<String, List<String>> antes = fotografia(n.id());
        assertEquals(2, antes.get("composicoes").size());
        assertTrue(antes.get("transferencias").isEmpty()); assertTrue(antes.get("usos").isEmpty());
        var recusada = enviar(c.localA, base(n.id()) + "/transferir-gestao", pedido, 409,
                "CON023 mapa não vazio mas incompleto deve recusar pela cobertura integral");
        assertTrue(recusada.path("message").asText().contains("mapeamento para todas as composições operacionais"));
        Map<String, List<String>> depoisRecusa = fotografia(n.id());
        assertEquals(antes, depoisRecusa,
                "Releitura SQL fora da transação HTTP preserva integralmente contrato, composições, eventos, versões, mapas, documentos e concessão/uso");
        List<Map<String, Object>> completo = new ArrayList<>();
        completo.add(mapas.get(primeiroMapeado)); completo.add(mapas.get(1 - primeiroMapeado));
        pedido.put("mapeamentos", completo); // Única mudança entre a recusa e o controle positivo.
        enviar(c.localA, base(n.id()) + "/transferir-gestao", pedido, 200,
                "Completar somente o mapa faltante permite a mesma intenção autorizada");
        emTransacao(() -> {
            em.clear();
            TransferenciaGestaoContrato t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:id",
                    TransferenciaGestaoContrato.class).setParameter("id", n.id()).getSingleResult();
            assertEquals(c.gestaoA.getId(), t.getGestaoOrigem().getId());
            assertEquals(c.gestaoB.getId(), t.getGestaoDestino().getId()); assertEquals(efeito, t.getDataEfetiva());
            assertEquals(2, t.getMapeamentos().size());
            assertEquals(Set.of(n.composicao1(), n.composicao2()), t.getMapeamentos().stream()
                    .map(m -> m.getComposicaoOrigem().getId()).collect(Collectors.toSet()));
            assertEquals(Set.of(c.cargoB.getId(), outroCargoDestino), t.getMapeamentos().stream()
                    .map(m -> m.getCargoDestino().getId()).collect(Collectors.toSet()));
            UsoPermissaoExcepcional uso = em.createQuery("select u from UsoPermissaoExcepcional u "
                    + "where u.recursoId=:id and u.tipoRecurso='Contrato'", UsoPermissaoExcepcional.class)
                    .setParameter("id", n.id()).getSingleResult();
            assertEquals(c.localA.getId(), uso.getUsuarioId()); assertEquals(pedido.get("motivo"), uso.getFinalidade());
            return null;
        });
        var origem = consultar(c.nacional, base(n.id()) + "/posicao-temporal?data=" + efeito.minusDays(1), "Origem anterior preservada");
        var destino = consultar(c.nacional, base(n.id()) + "/posicao-temporal?data=" + efeito, "Destino na data efetiva");
        assertEquals(c.gestaoA.getId().longValue(), origem.path("versaoContratual").path("gestaoLocalId").asLong());
        assertEquals(c.gestaoB.getId().longValue(), destino.path("versaoContratual").path("gestaoLocalId").asLong());
        gravar(mapa("criterio", "CANONICAL198::CON-023:001", "primeiroMapeado", primeiroMapeado,
                "antes", antes, "aposRecusa", depoisRecusa, "aposControle", fotografia(n.id()),
                "somenteMapaFaltanteAcrescentado", true));
    }

    private Map<String, List<String>> fotografia(long contrato) {
        // O log de auditoria da requisição pode registrar a negativa; os fatos negociais abaixo não podem mudar.
        return Map.of("contrato", linhas("contrato", "id_contrato", contrato),
                "composicoes", linhas("composicao_contratual", "id_contrato", contrato),
                "eventos", linhas("contrato_evento_ciclo_vida", "id_contrato", contrato),
                "versoes", linhas("contrato_versao_negocial", "id_contrato", contrato),
                "transferencias", linhas("transferencia_gestao_contrato", "id_contrato", contrato),
                "mapeamentos", jdbc.queryForList("select row_to_json(m)::text from transferencia_gestao_mapeamento m "
                        + "join transferencia_gestao_contrato t on t.id_transferencia_gestao=m.id_transferencia_gestao "
                        + "where t.id_contrato=? order by m.id_mapeamento_transferencia", String.class, contrato),
                "usos", jdbc.queryForList("select row_to_json(u)::text from permissao_excepcional_uso u "
                        + "where id_recurso=? and tipo_recurso='Contrato' order by id_permissao_uso", String.class, contrato),
                "concessoes", linhas("permissao_excepcional", "id_contrato", contrato),
                "documentos", linhas("documento_comprobatorio", "id_contrato", contrato));
    }

    private List<String> linhas(String tabela, String coluna, long id) {
        // Identificadores exclusivamente constantes privadas deste teste; valores sempre parametrizados.
        return jdbc.queryForList("select row_to_json(t)::text as foto from " + tabela
                + " t where " + coluna + "=? order by foto", String.class, id);
    }

    private void gravar(Object prova) throws Exception {
        Path pasta = Path.of("target/transferencia-mapa-integral");
        Files.createDirectories(pasta, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path arquivo = Files.createFile(pasta.resolve("caso-" + UUID.randomUUID() + ".json"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prova));
        try (var canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) { canal.force(true); }
        try (var canal = FileChannel.open(pasta, StandardOpenOption.READ)) { canal.force(true); }
    }
}
