package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TMP-012/ARQ-003: DTO do controlador, serviços, motor temporal e PostgreSQL reais.
 * Capacidade contratual discrimina cada data; alocados e lançamentos são zero.
 * Fatos 2023/2024 novos e explicitamente sintéticos, sem retcon da fixture2026.
 * MockMvc com filtros do projeto não comprova TCP/JAR, gráficos ou Chromium.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PainelBissextoDtoRealHttpIT extends AuditoriaHttpSuporte {
    private static final LocalDate ORIGEM_CATALOGOS = LocalDate.of(2023, 1, 1);
    private static final LocalDate REFERENCIA = LocalDate.of(2024, 2, 29);
    @Autowired private JdbcTemplate jdbc;
    private Path evidencias;

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void quinzenaBissextaMantemQuinzePontosAtuaisEQuatorzeComparativosComValoresReais() throws Exception {
        evidencias = diretorioPrivado();
        conferirPostgresIsolado();
        Cenario suporte = criarCenarioPersistido();
        List<Long> contratosComuns = List.of(suporte.contratoA.getId(), suporte.contratoB.getId());
        JsonNode comumAntes = fotografia(contratosComuns);
        Preparado f = preparar(suporte);
        assertThat(fotografia(contratosComuns)).as("As organizações/ator são reutilizados; fatos2026 permanecem intactos")
                .isEqualTo(comumAntes);
        JsonNode antes = fotografia(f.contratos());
        JsonNode catalogosAntes = fontesCatalogos(f);
        assertThat(antes.path("contratos")).hasSize(29);
        assertThat(antes.path("composicoes")).hasSize(29);
        assertThat(antes.path("versoesOriginais")).hasSize(29);
        assertThat(antes.path("eventos")).hasSize(29);
        assertThat(antes.path("vinculos")).isEmpty();
        assertThat(antes.path("alocacoes")).isEmpty();
        registrar("fixture-confirmada.json", mapa("sintetica", true, "gestaoId", suporte.gestaoA.getId(),
                "lotacaoId", f.lotacao(), "jornadaId", f.jornada(), "cargoId", f.cargo(),
                "contratosPorDia", f.contratosPorDia(), "esperadoIndependente", f.esperado(),
                "alocados", 0, "lancamentos", 0, "fontes", antes, "fontesCatalogos", catalogosAntes));

        JsonNode semComparacao = consultar(suporte, f, false);
        registrar("dto-sem-comparacao.json", semComparacao);
        conferirRecorte(semComparacao, f);
        conferirSerie(semComparacao.path("serieAtual"), 2024, f.esperado());
        conferirResumo(semComparacao.path("resumoAtual"), 229);
        assertThat(semComparacao.path("resumoAnoAnterior").isNull()).isTrue();
        assertThat(semComparacao.path("serieAnoAnterior")).isEmpty();

        JsonNode comparado = consultar(suporte, f, true);
        registrar("dto-comparado.json", comparado);
        conferirRecorte(comparado, f);
        conferirSerie(comparado.path("serieAtual"), 2024, f.esperado());
        conferirSerie(comparado.path("serieAnoAnterior"), 2023, f.esperado());
        conferirResumo(comparado.path("resumoAtual"), 229);
        conferirResumo(comparado.path("resumoAnoAnterior"), 128);
        assertThat(comparado.path("serieAtual")).isEqualTo(semComparacao.path("serieAtual"));
        assertThat(comparado.path("resumoAtual")).isEqualTo(semComparacao.path("resumoAtual"));
        assertThat(comparado.path("serieAtual").get(14).path("data").asText()).isEqualTo("2024-02-29");
        assertThat(comparado.path("serieAtual").get(14).path("efetivoPrevisto").asLong()).isEqualTo(229);
        assertThat(comparado.path("serieAnoAnterior").get(13).path("data").asText()).isEqualTo("2023-02-28");
        assertThat(comparado.path("serieAnoAnterior").get(13).path("efetivoPrevisto").asLong()).isEqualTo(128);
        assertThat(comparado.path("serieAnoAnterior").get(14)).as("Não inventar ponto comparativo para29/02").isNull();
        conferirDetalhamentoFinal(comparado, f);

        JsonNode controleRelido = consultar(suporte, f, false);
        assertThat(controleRelido).as("Ativar comparação não contamina uma consulta seguinte sem comparação")
                .isEqualTo(semComparacao);
        JsonNode depois = fotografia(f.contratos());
        assertThat(depois).as("Leituras do Painel não alteram contratos, publicação, versões ou composições")
                .isEqualTo(antes);
        assertThat(fotografia(contratosComuns)).isEqualTo(comumAntes);
        assertThat(fontesCatalogos(f)).isEqualTo(catalogosAntes);
        registrar("resultado.json", mapa("predicado", "TMP-012/ARQ-003",
                "camada", "MOCKMVC_FILTROS_SPRING_JPA_POSTGRES_REAL", "datasAtuais", 15, "datasComparativas", 14,
                "valoresAtuais", "215..229", "valoresComparativos", "115..128", "alocados", 0, "lancamentos", 0,
                "pontoComparativo29Inventado", false, "fontesPreservadas", depois,
                "controleSemComparacaoRelido", controleRelido));
    }

    private record Preparado(long lotacao, long jornada, long cargo, long tipo, List<Long> contratos,
                             Map<LocalDate, Long> contratosPorDia, Map<LocalDate, Integer> esperado) {}

    private Preparado preparar(Cenario suporte) {
        return emTransacao(() -> {
            GestaoLocal gestao = em.find(GestaoLocal.class, suporte.gestaoA.getId());
            Jornada jornada = new Jornada();
            jornada.setGestaoLocal(gestao); jornada.setAtivo(true); jornada.setDescricao("Turno sintético bissexto " + UUID.randomUUID());
            for (DayOfWeek dia : DayOfWeek.values()) {
                JornadaDiaSemana item = new JornadaDiaSemana(); item.setJornada(jornada); item.setDiaSemana(dia);
                item.setDiaTrabalho(dia.getValue() <= 5);
                if (dia.getValue() <= 5) {
                    item.setEntrada1("08:00"); item.setSaida1("12:00"); item.setEntrada2("13:00"); item.setSaida2("17:00");
                }
                jornada.getDias().add(item);
            }
            em.persist(jornada);
            Lotacao lotacao = new Lotacao("Lotação sintética bissexta " + UUID.randomUUID());
            lotacao.setGestaoLocal(gestao); lotacao.setAtivo(true); lotacao.setMunicipio("MUNICIPIO SINTETICO BISSEXTO");
            lotacao.getTurnos().add(jornada); em.persist(lotacao);
            Cargo cargo = new Cargo("Cargo sintético bissexto " + UUID.randomUUID(), "411005");
            cargo.setGestaoLocal(gestao); cargo.setAtivo(true); em.persist(cargo);
            TipoContratacao tipo = new TipoContratacao(); tipo.setGestaoLocal(gestao);
            tipo.setDescricao("Tipo sintético bissexto " + UUID.randomUUID()); tipo.setAtivo(true); em.persist(tipo);
            origemCatalogo(TipoCatalogoTemporal.LOTACAO, lotacao.getId(), suporte);
            origemCatalogo(TipoCatalogoTemporal.JORNADA, jornada.getId(), suporte);
            origemCatalogo(TipoCatalogoTemporal.CARGO, cargo.getId(), suporte);
            origemCatalogo(TipoCatalogoTemporal.TIPO_CONTRATACAO, tipo.getId(), suporte);

            Map<LocalDate, Integer> esperado = new TreeMap<>();
            Map<LocalDate, Long> contratos = new TreeMap<>();
            // Oracle literal da massa, não calculado a partir de qualquer DTO/repositório.
            for (int ano : List.of(2023, 2024)) {
                int ultimo = ano == 2024 ? 29 : 28;
                for (int dia = 15; dia <= ultimo; dia++) {
                    LocalDate data = LocalDate.of(ano, 2, dia);
                    int capacidade = (ano == 2024 ? 200 : 100) + dia;
                    Contrato contrato = contratoDiario(suporte, gestao, tipo, data, capacidade);
                    ComposicaoContratual composicao = new ComposicaoContratual();
                    composicao.setContrato(contrato); composicao.setLotacao(lotacao); composicao.setJornada(jornada);
                    composicao.setCargo(cargo); composicao.setEfetivoPrevisto(capacidade);
                    composicao.setDataInicioVigencia(data); composicao.setDataFimVigencia(data);
                    composicao.aplicarConfiguracaoDocumentada(lotacao.getMunicipio(), FotografiaJornada.capturar(jornada));
                    em.persist(composicao);
                    documentarPublicacao(suporte, contrato, gestao, tipo, data, capacidade);
                    esperado.put(data, capacidade); contratos.put(data, contrato.getId());
                }
            }
            em.flush(); em.clear();
            assertThat(contratos).hasSize(29);
            return new Preparado(lotacao.getId(), jornada.getId(), cargo.getId(), tipo.getId(), List.copyOf(contratos.values()),
                    Collections.unmodifiableMap(contratos), Collections.unmodifiableMap(esperado));
        });
    }

    private Contrato contratoDiario(Cenario suporte, GestaoLocal gestao, TipoContratacao tipo, LocalDate data, int capacidade) {
        Contrato contrato = new Contrato(); contrato.setGestaoLocal(gestao); contrato.setTipoContratacao(tipo);
        contrato.setTipo(tipo.getDescricao()); contrato.setNumeroContrato("BIS-" + data + "-" + UUID.randomUUID().toString().substring(0, 8));
        contrato.setAnoContrato(data.getYear()); contrato.setDescricao("Contrato diário explicitamente sintético do Painel bissexto");
        contrato.setCnpjEmpresa("11222333000181"); contrato.setRazaoSocialEmpresa("Empresa Sintética Bissexta");
        contrato.setNomeFantasiaEmpresa("Sintética Bissexta"); contrato.setDataInicio(data); contrato.setDataFim(data);
        contrato.setEfetivoPrevisto(capacidade); contrato.setEfetivoOriginalPublicado(capacidade);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); contrato.setAtivo(true);
        // Registro/conhecimento é atual. A fonte declara a vigência histórica;
        // não se simula que esta fixture foi gravada em2023 ou2024.
        contrato.setPublicadoEm(Instant.now()); contrato.setPublicadoPorId(suporte.localA.getId());
        contrato.setPublicadoPorUsername(suporte.localA.getUsername()); contrato.setReferenciaSeiPublicacao("SEI-FIXTURE-PAINEL-" + data);
        em.persist(contrato); return contrato;
    }

    private void documentarPublicacao(Cenario suporte, Contrato contrato, GestaoLocal gestao,
                                       TipoContratacao tipo, LocalDate data, int capacidade) {
        EventoCicloVidaContrato evento = new EventoCicloVidaContrato(); evento.setContrato(contrato);
        evento.setTipoEvento(TipoEventoCicloVidaContrato.PUBLICACAO); evento.setDataEfetiva(data); evento.setDataAto(data);
        evento.setChaveNegocial("ESTADO_NEGOCIAL"); evento.setValorAnterior("RASCUNHO"); evento.setValorNovo("PUBLICADO");
        evento.setMotivo("Fonte documental sintética explícita da publicação histórica para testar o Painel");
        evento.setReferenciaSei(contrato.getReferenciaSeiPublicacao());
        evento.setUsuarioResponsavelId(suporte.localA.getId()); evento.setUsuarioResponsavelUsername(suporte.localA.getUsername());
        em.persist(evento);
        VersaoContratoNegocial fonte = new VersaoContratoNegocial(); fonte.setContrato(contrato); fonte.setEventoOrigem(evento);
        fonte.setNumeroVersao(1); fonte.setDataEfetiva(data); fonte.setDataInicio(data); fonte.setDataFim(data);
        fonte.setNumeroContrato(contrato.getNumeroContrato()); fonte.setAnoContrato(data.getYear());
        fonte.setDescricao(contrato.getDescricao()); fonte.setEfetivoPrevisto(capacidade);
        fonte.setGestaoLocalId(gestao.getId()); fonte.setGestaoLocalNome(gestao.getNome());
        fonte.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        fonte.setTipoContratacaoId(tipo.getId()); fonte.setTipoContratacaoDescricao(tipo.getDescricao());
        fonte.setCnpjEmpresa(contrato.getCnpjEmpresa()); fonte.setRazaoSocialEmpresa(contrato.getRazaoSocialEmpresa());
        fonte.setNomeFantasiaEmpresa(contrato.getNomeFantasiaEmpresa());
        fonte.setMatriculaVinculoObrigatoria(false); fonte.setMatriculaTipoContratacaoObrigatoria(false);
        fonte.setMatriculaVinculoObrigatoriaEfetiva(false); em.persist(fonte);
    }

    private void origemCatalogo(TipoCatalogoTemporal tipo, long id, Cenario suporte) {
        EventoDisponibilidadeCatalogo fonte = new EventoDisponibilidadeCatalogo();
        fonte.setTipoCatalogo(tipo); fonte.setCatalogoId(id); fonte.setDataEfetiva(ORIGEM_CATALOGOS); fonte.setAtivo(true);
        fonte.setMotivo("Fonte sintética explícita de disponibilidade desde2023 para os fatos novos do Painel");
        fonte.setEvidencia("SEI-FIXTURE-PAINEL-BISSEXTO"); fonte.setAutorizacao("ATO-SINTETICO-PAINEL-BISSEXTO");
        fonte.setResponsavelId(suporte.localA.getId()); fonte.setResponsavelUsername(suporte.localA.getUsername());
        em.persist(fonte);
    }

    private JsonNode consultar(Cenario suporte, Preparado f, boolean comparar) throws Exception {
        return dados(executar(suporte.localA, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/painel/efetivo")
                .param("referencia", REFERENCIA.toString()).param("periodo", "QUINZENAL")
                .param("lotacaoId", Long.toString(f.lotacao())).param("jornadaId", Long.toString(f.jornada()))
                .param("cargoId", Long.toString(f.cargo())).param("compararAnoAnterior", Boolean.toString(comparar)),
                200, "DTO real do Painel em fevereiro bissexto; comparação=" + comparar));
    }

    private void conferirRecorte(JsonNode dto, Preparado f) {
        assertThat(dto.path("inicio").asText()).isEqualTo("2024-02-15");
        assertThat(dto.path("fim").asText()).isEqualTo("2024-02-29");
        assertThat(dto.path("periodo").asText()).isEqualTo("QUINZENAL");
        assertThat(dto.path("contratoId").isNull()).isTrue();
        assertThat(dto.path("lotacaoId").asLong()).isEqualTo(f.lotacao());
        assertThat(dto.path("jornadaId").asLong()).isEqualTo(f.jornada());
        assertThat(dto.path("cargoId").asLong()).isEqualTo(f.cargo());
    }

    private void conferirSerie(JsonNode serie, int ano, Map<LocalDate, Integer> esperado) {
        int ultimo = ano == 2024 ? 29 : 28;
        assertThat(serie).hasSize(ultimo - 14);
        Set<LocalDate> datas = new HashSet<>(); Set<Long> valores = new HashSet<>();
        for (int dia = 15; dia <= ultimo; dia++) {
            LocalDate data = LocalDate.of(ano, 2, dia); JsonNode ponto = serie.get(dia - 15);
            assertThat(ponto.path("data").asText()).isEqualTo(data.toString());
            assertThat(datas.add(LocalDate.parse(ponto.path("data").asText()))).as("Nenhuma data repetida").isTrue();
            assertThat(ponto.path("efetivoPrevisto").asLong()).isEqualTo(esperado.get(data).longValue());
            assertThat(ponto.path("deficitContratual").asLong()).isEqualTo(esperado.get(data).longValue());
            assertThat(valores.add(ponto.path("efetivoPrevisto").asLong())).as("Cada posição possui valor discriminante").isTrue();
            conferirZeros(ponto);
        }
        assertThat(datas).hasSize(ultimo - 14); assertThat(valores).hasSize(ultimo - 14);
    }

    private void conferirResumo(JsonNode resumo, long capacidadeFinal) {
        assertThat(resumo.path("efetivoPrevisto").asLong()).isEqualTo(capacidadeFinal);
        assertThat(resumo.path("deficitContratual").asLong()).isEqualTo(capacidadeFinal);
        conferirZeros(resumo); // Resumo é a última data, não média do intervalo.
    }

    private void conferirZeros(JsonNode ponto) {
        for (String campo : List.of("efetivoAlocado", "registrosLancados", "presentes", "ausentes", "outrosRegistros",
                "pendentesLancamento", "excedenteContratual", "percentualCobertura", "percentualAtendimento")) {
            assertThat(ponto.hasNonNull(campo)).as("Campo real obrigatório: " + campo).isTrue();
            assertThat(ponto.path(campo).asDouble()).as(campo).isZero();
        }
    }

    private void conferirDetalhamentoFinal(JsonNode dto, Preparado f) {
        assertThat(dto.path("detalhamentoContratos")).hasSize(1);
        assertThat(dto.path("detalhamentoContratos").get(0).path("id").asLong()).isEqualTo(f.contratosPorDia().get(REFERENCIA));
        assertThat(dto.path("detalhamentoContratos").get(0).path("efetivoPrevisto").asLong()).isEqualTo(229);
        assertThat(dto.path("quadroDimensionado")).hasSize(1);
        JsonNode linha = dto.path("quadroDimensionado").get(0);
        assertThat(linha.path("contratoId").asLong()).isEqualTo(f.contratosPorDia().get(REFERENCIA));
        assertThat(linha.path("lotacaoId").asLong()).isEqualTo(f.lotacao());
        assertThat(linha.path("jornadaId").asLong()).isEqualTo(f.jornada());
        assertThat(linha.path("cargoId").asLong()).isEqualTo(f.cargo());
        assertThat(linha.path("efetivoPrevisto").asLong()).isEqualTo(229);
    }

    private JsonNode fotografia(List<Long> contratos) {
        String marcadores = String.join(",", Collections.nCopies(contratos.size(), "?")); Object[] ids = contratos.toArray();
        var leitura = new TransactionTemplate(transactionManager); leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return leitura.execute(status -> mapper.valueToTree(mapa(
                "contratos", jdbc.queryForList("select * from contrato where id_contrato in (" + marcadores + ") order by id_contrato", ids),
                "composicoes", jdbc.queryForList("select * from composicao_contratual where id_contrato in (" + marcadores + ") order by id_composicao_contratual", ids),
                "versoesOriginais", jdbc.queryForList("select * from contrato_versao_negocial where id_contrato in (" + marcadores + ") order by id_contrato_versao", ids),
                "eventos", jdbc.queryForList("select * from contrato_evento_ciclo_vida where id_contrato in (" + marcadores + ") order by id_evento_ciclo_vida", ids),
                "vinculos", jdbc.queryForList("select * from vinculo_contratual where id_contrato in (" + marcadores + ") order by id_vinculo_contratual", ids),
                "alocacoes", jdbc.queryForList("select a.* from alocacao_vinculo a join vinculo_contratual v on v.id_vinculo_contratual=a.id_vinculo_contratual "
                        + "where v.id_contrato in (" + marcadores + ") order by a.id_alocacao_vinculo", ids))));
    }

    private JsonNode fontesCatalogos(Preparado f) {
        var fontes = jdbc.queryForList("""
            SELECT * FROM evento_disponibilidade_catalogo
             WHERE (tipo_catalogo='LOTACAO' AND catalogo_id=?)
                OR (tipo_catalogo='JORNADA' AND catalogo_id=?)
                OR (tipo_catalogo='CARGO' AND catalogo_id=?)
                OR (tipo_catalogo='TIPO_CONTRATACAO' AND catalogo_id=?) ORDER BY id
            """, f.lotacao(), f.jornada(), f.cargo(), f.tipo());
        assertThat(fontes).hasSize(4);
        for (Map<String, Object> fonte : fontes) {
            assertThat(fonte.get("data_efetiva").toString()).isEqualTo("2023-01-01");
            assertThat(fonte.get("ativo")).isEqualTo(Boolean.TRUE);
            assertThat(fonte.get("registrado_em")).isNotNull();
        }
        return mapper.valueToTree(fontes);
    }

    private void conferirPostgresIsolado() throws Exception {
        for (String variavel : List.of("INTEGRATION_POSTGRES_URL", "INTEGRATION_POSTGRES_USER", "INTEGRATION_POSTGRES_PASSWORD"))
            assertThat(System.getenv(variavel)).as("Integração exige variável explícita, sem dotenv: " + variavel).isNotBlank();
        Map<String, Object> identidade = jdbc.queryForMap("""
            SELECT current_setting('server_version_num')::integer AS versao,
              d.datdba=r.oid AS ownership_banco,
              NOT(r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls) AS role_limitada,
              NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid) AS sem_membros,
              session_user=current_user AS mesma_sessao,
              (n.nspowner=r.oid OR (pg_get_userbyid(n.nspowner)='pg_database_owner' AND d.datdba=r.oid))
                AND has_schema_privilege(current_user,'public','USAGE')
                AND has_schema_privilege(current_user,'public','CREATE') AS ownership_schema
            FROM pg_roles r JOIN pg_database d ON d.datname=current_database()
              JOIN pg_namespace n ON n.nspname='public' WHERE r.rolname=current_user
            """);
        assertThat(((Number) identidade.get("versao")).intValue()).isEqualTo(180006);
        for (String campo : List.of("ownership_banco", "role_limitada", "sem_membros", "mesma_sessao", "ownership_schema"))
            assertThat(identidade.get(campo)).as(campo).isEqualTo(Boolean.TRUE);
        registrar("postgres-sem-credenciais.json", identidade);
    }

    private Map<String, Object> mapa(Object... pares) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int n = 0; n < pares.length; n += 2) mapa.put((String) pares[n], pares[n + 1]);
        return mapa;
    }

    private static Path diretorioPrivado() throws java.io.IOException {
        Path raiz = Path.of("target/painel-bissexto-real").toAbsolutePath(); Files.createDirectories(raiz);
        return Files.createTempDirectory(raiz, "cenario-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    }

    private void registrar(String nome, Object valor) throws Exception {
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(valor) + "\n").getBytes(StandardCharsets.UTF_8);
        Path temporario = Files.createTempFile(evidencias, nome + "-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) canal.write(buffer); canal.force(true);
        }
        Files.move(temporario, evidencias.resolve(nome), StandardCopyOption.ATOMIC_MOVE);
        try (FileChannel diretorio = FileChannel.open(evidencias, StandardOpenOption.READ)) { diretorio.force(true); }
    }
}
