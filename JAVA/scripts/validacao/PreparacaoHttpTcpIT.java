package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.TipoContratacao;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ferramenta opt-in de preparação, fora da suíte principal contada nos relatórios.
 * Reutiliza o cenário sintético JPA existente, incluindo seus demais perfis,
 * dois contratos e pessoas auxiliares; não cria Frequências nem faz DDL manual.
 * Os dados são confirmados e relidos em outra transação, sem rollback de teste.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreparacaoHttpTcpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping rotas;

    @org.springframework.beans.factory.annotation.Autowired
    MotorTemporalContratoServico motorTemporal;

    @Test
    void prepararFixtureTcpComDadosConfirmadosEIdentificadoresGerados() throws Exception {
        String url = System.getProperty("integration.mysql.url", System.getenv("INTEGRATION_MYSQL_URL"));
        assertNotNull(url, "Declare explicitamente o banco sintético de preparação");
        assertTrue(url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/(sgt_audit_[A-Za-z0-9_]+|sgt_correcao_exec|sgt8045_(baseline|candidata|upgrade|http|409_red|regressao))(\\?.*)?"),
                "A ferramenta é restrita às bases sintéticas locais explicitamente nomeadas");
        Path file = Path.of(System.getProperty("fixture.http.saida", "target/auditoria-http/fixture_tcp.json"))
                .toAbsolutePath().normalize();
        Path inventarioFile = file.resolveSibling("inventario_rotas_contexto_spring.json");
        assertFalse(Files.exists(file), "Não sobrescrever fixture de preparação anterior");
        assertFalse(Files.exists(inventarioFile), "Preserve inclusive o inventário de uma preparação interrompida");
        Cenario c = criarCenarioPersistido();
        Map<String, Object> motivos = emTransacao(() -> {
            Map<String, Object> resultado = new java.util.LinkedHashMap<>();
            for (var categoria : CategoriaTerminoVinculo.values()) {
                MotivoDesligamento motivo = new MotivoDesligamento();
                motivo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
                motivo.setDescricao("Causa sintética TCP " + categoria); motivo.setCategoria(categoria);
                em.persist(motivo); em.flush();
                EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(motivo.getId());
                origem.setDataEfetiva(java.time.LocalDate.of(2025, 1, 1)); origem.setAtivo(true);
                origem.setMotivo("Fonte sintética documental do cenário TCP desde 2025"); em.persist(origem);
                resultado.put(categoria.name(), motivo.getId());
            }
            return resultado;
        });
        Map<String, Object> provasHistoricas = prepararFontesSinteticas(c, motivos);
        Map<String, UsuarioAplicacao> atores = Map.of("local", c.localA, "outro", c.localB,
                "nacional", c.nacional, "admin", c.admin, "presidente", c.presidente,
                "operacional", c.operacionalA, "operacional_destino", c.operacionalB,
                "estadual", c.estadualA, "estadual_destino", c.estadualB);
        Map<String, Object> usuarios = new java.util.LinkedHashMap<>();
        Map<String, Object> usuariosIds = new java.util.LinkedHashMap<>();
        atores.forEach((nome, usuario) -> { usuarios.put(nome, usuario.getMatricula()); usuariosIds.put(nome, usuario.getId()); });
        Map<String, Object> fixture = mapa(
                "gestao_id", c.gestaoA.getId(), "tipo_id", c.tipoA.getId(),
                "lotacao_id", c.lotacaoA.getId(), "jornada_id", c.jornadaA.getId(), "cargo_id", c.cargoA.getId(),
                "gestao_destino_id", c.gestaoB.getId(), "tipo_destino_id", c.tipoB.getId(),
                "lotacao_destino_id", c.lotacaoB.getId(), "jornada_destino_id", c.jornadaB.getId(),
                "cargo_destino_id", c.cargoB.getId(), "superintendencia_id", c.seA.getId(),
                "superintendencia_destino_id", c.seB.getId(), "senha", SENHA,
                "usuarios", usuarios, "usuarios_ids", usuariosIds, "motivos", motivos,
                "provas_historicas", provasHistoricas);
        emTransacao(() -> {
            assertNotNull(em.find(GestaoLocal.class, c.gestaoA.getId()));
            assertNotNull(em.find(TipoContratacao.class, c.tipoA.getId()));
            assertNotNull(em.find(Lotacao.class, c.lotacaoA.getId()));
            assertNotNull(em.find(Jornada.class, c.jornadaA.getId()));
            assertNotNull(em.find(Cargo.class, c.cargoA.getId()));
            for (UsuarioAplicacao original : atores.values()) {
                UsuarioAplicacao persistido = em.find(UsuarioAplicacao.class, original.getId());
                assertNotNull(persistido);
                assertEquals(original.getMatricula(), persistido.getMatricula());
                assertEquals(original.getPerfil(), persistido.getPerfil());
                assertTrue(passwordEncoder.matches(SENHA, persistido.getPassword()));
            }
            return null;
        });
        Files.createDirectories(file.getParent());
        Files.writeString(file, json(fixture), java.nio.file.StandardOpenOption.CREATE_NEW);
        Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        var relida = mapper.readTree(Files.readString(file));
        assertEquals(17, relida.size());
        assertTrue(relida.path("provas_historicas").path("somente_dados_sinteticos").asBoolean());
        for (String key : List.of("gestao_id", "tipo_id", "lotacao_id", "jornada_id", "cargo_id")) {
            assertTrue(relida.path(key).isIntegralNumber());
            assertEquals(((Number) fixture.get(key)).longValue(), relida.path(key).longValue());
        }
        assertEquals(SENHA, relida.path("senha").asText());
        assertEquals(9, relida.path("usuarios").size());
        assertEquals(c.localA.getMatricula(), relida.path("usuarios").path("local").asText());
        assertEquals(c.localB.getMatricula(), relida.path("usuarios").path("outro").asText());
        assertEquals(c.nacional.getMatricula(), relida.path("usuarios").path("nacional").asText());
        assertEquals(c.admin.getMatricula(), relida.path("usuarios").path("admin").asText());

        // Inventário obtido do contexto Spring real. Não é estimativa por regex
        // nem reaproveitamento da matriz de outra versão do código.
        java.util.List<Map<String, Object>> inventario = new java.util.ArrayList<>();
        rotas.getHandlerMethods().forEach((mapeamento, metodo) -> {
            for (String caminho : mapeamento.getPatternValues()) {
                if (!caminho.startsWith("/api/")) continue;
                var verbos = mapeamento.getMethodsCondition().getMethods();
                java.util.List<String> nomesVerbos = verbos.isEmpty() ? java.util.List.of("ANY")
                        : verbos.stream().map(Enum::name).sorted().toList();
                for (String verbo : nomesVerbos) inventario.add(mapa(
                        "http", verbo, "rota", caminho, "controlador", metodo.getBeanType().getName(),
                        "metodo", metodo.getMethod().getName(), "retorno", metodo.getMethod().getGenericReturnType().getTypeName(),
                        "parametros", java.util.Arrays.stream(metodo.getMethod().getParameters())
                                .map(p -> mapa("nome", p.getName(), "tipo", p.getParameterizedType().getTypeName())).toList()));
            }
        });
        inventario.sort(java.util.Comparator.comparing(m -> m.get("rota") + " " + m.get("http")));
        assertFalse(inventario.isEmpty());
        Files.writeString(inventarioFile, json(inventario), java.nio.file.StandardOpenOption.CREATE_NEW);
    }

    /**
     * Documento-oráculo exclusivo deste banco descartável. As fontes são
     * capturadas da criação sintética declarada, ANTES de simular a lacuna.
     * Não alteramos qualquer acervo recebido e não consultamos o cadastro
     * atual para preencher a lacuna durante a validação TCP.
     */
    private Map<String, Object> prepararFontesSinteticas(Cenario c, Map<String, Object> motivos) {
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        return emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, n.id());
            VersaoContratoNegocial origem = motorTemporal.fotografiaOriginalConhecida(
                    contrato, java.time.Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked")
            Map<String, Object> foto = mapper.convertValue(motorTemporal.paraVersaoDTO(origem), java.util.LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            ComposicaoContratual comp = em.find(ComposicaoContratual.class, n.composicao1());
            Map<String, Object> fonte = mapa("eventoOrigemId", origem.getEventoOrigem().getId(),
                    "versaoAnteriorId", origem.getId(), "dataDocumento", RelogioSistema.hoje().minusDays(40),
                    "referenciaSei", "SEI-SINTETICO-TCP-FONTE-CONTRATO",
                    "motivo", "Fonte sintética explícita preservada antes da simulação isolada de lacuna documental",
                    "autorizacaoAdministrativa", "ATO-SINTETICO-TCP-PROVAS",
                    "versaoContratoEsperada", contrato.getVersion(), "chaveIdempotencia", java.util.UUID.randomUUID().toString(),
                    "configuracaoCompletaComprovada", true, "fotografia", foto,
                    "composicoes", List.of(mapa("composicaoId", comp.getId(),
                            "lotacaoNome", comp.getLotacaoNomeFotografia(),
                            "jornadaDescricao", comp.getJornadaDescricaoFotografia(),
                            "cargoNome", comp.getCargoNomeFotografia(),
                            "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                            "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true)));
            long versaoOrigemId = origem.getId();
            // Apenas dados criados neste método e exclusivamente neste banco
            // sintético: a origem passa a documentar uma lacuna conhecida.
            em.createQuery("update VersaoContratoNegocial v set v.descricao=null, v.tipoContratacaoDescricao=null where v.id=:id")
                    .setParameter("id", versaoOrigemId).executeUpdate();
            em.createQuery("update ComposicaoContratual c set c.jornadaSemanalFotografia=null, c.lotacaoNomeFotografia='' where c.id=:id")
                    .setParameter("id", n.composicao1()).executeUpdate();
            em.flush(); em.clear();
            assertNull(em.find(VersaoContratoNegocial.class, versaoOrigemId).getDescricao());
            assertNull(em.find(ComposicaoContratual.class, n.composicao1()).getJornadaSemanalFotografia());
            return mapa("somente_dados_sinteticos", true,
                    "descricao_fonte", "Documento-oráculo sintético criado pela ferramenta de preparação do banco TCP isolado",
                    "contrato_prova_id", n.id(), "composicao_prova_id", n.composicao1(),
                    "data_contrato", n.inicio(), "fonte_contrato", fonte,
                    "contrato_contexto_id", c.contratoA.getId(), "pessoa_id", c.pessoaA.getId(),
                    "vinculo_id", c.vinculoA.getId(), "data_contexto", DATA,
                    "data_inicio_contexto", DATA.minusDays(1), "data_fim_contexto", DATA.plusDays(1),
                    "nome_comprovado", "Nome documental sintético TCP versão um",
                    "nome_retificado", "Nome documental sintético TCP versão dois",
                    "motivo_id", motivos.get(CategoriaTerminoVinculo.OUTRO.name()),
                    "descricao_motivo_comprovada", "Causa documental sintética TCP versão um",
                    "descricao_motivo_retificada", "Causa documental sintética TCP versão dois");
        });
    }
}
