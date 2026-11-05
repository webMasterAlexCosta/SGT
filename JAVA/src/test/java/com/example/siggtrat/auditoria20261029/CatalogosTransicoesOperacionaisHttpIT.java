package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** CAT006/007/008 e AUD004: atos de disponibilidade alcançáveis por HTTP canônico. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogosTransicoesOperacionaisHttpIT extends ContratosHttpBase {
    @Test
    void fonteRetroativaDocumentadaPermiteVinculoPassadoComCargoHojeInativo() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = cargoNovo(c, false);
        LocalDate inicio = RelogioSistema.hoje().minusDays(30), fim = RelogioSistema.hoje().minusDays(10);
        Instant corteAnterior = Instant.now();
        assertThat(disponibilidade(c, id, inicio, null).path("disponibilidadeComprovada").asBoolean()).isFalse();
        Map<String, Object> req = requisicao(c, id, inicio, true);
        confirmarPrevia(c, id, req);
        JsonNode registrado = enviar(c.localA, rota(id), req, 200,
                "CAT007 ato com fonte e autorização comprova a disponibilidade na data efetiva");
        assertThat(registrado.path("ativoAtualAposRegistro").asBoolean()).isFalse();
        assertThat(disponibilidade(c, id, inicio, null).path("ativo").asBoolean()).isTrue();
        assertThat(disponibilidade(c, id, inicio.minusDays(1), null).path("ativo").isNull()).isTrue();
        assertThat(disponibilidade(c, id, inicio, corteAnterior).path("ativo").isNull()).isTrue();
        assertThat(consultar(c.localA, "/api/cargos/" + id, "CAT007 estado atual continua inativo").path("ativo").asBoolean()).isFalse();

        JsonNode contrato = enviar(c.localA, "/api/contratos", rascunho(c, "RETRO-CAT-" + id, 1, inicio, fim),
                201, "CON008 contrato integralmente passado com origem documental");
        long contratoId = contrato.path("id").asLong();
        long composicaoId = enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, id, 1), 201,
                "CAT007 catálogo hoje inativo é selecionado no período comprovadamente válido").path("id").asLong();
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-PUBLICACAO-RETRO-CAT",
                "justificativa", "Publicação de fato passado com prova integral");
        JsonNode previa = enviar(c.localA, base(contratoId) + "/previas/publicar", publicacao, 200, "AUD004 prévia da publicação retroativa");
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText()); publicacao.put("confirmarImpacto", true);
        enviar(c.localA, base(contratoId) + "/publicar", publicacao, 200, "CON008 publicação retroativa documental");
        JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa(
                        "nome", "Pessoa catálogo retroativo " + id, "cpf", cpf(650000000 + Math.toIntExact(id))),
                "contratoId", contratoId, "composicaoId", composicaoId, "dataInicio", inicio, "dataFim", fim), 201,
                "CAT007 VIN001 comando canônico conclui vínculo retroativo usando a prova do catálogo");
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculo.path("id").asLong()).getDataFim()).isEqualTo(fim);
            assertThat(em.find(Cargo.class, id).getAtivo()).isFalse(); return null;
        });
    }

    @Test
    void transicaoRetroativaPreservaReativacaoEfetivaPosteriorMesmoRegistradaAntes() throws Exception {
        Cenario c = criarCenarioPersistido(); long id = cargoNovo(c, true);
        LocalDate posterior = RelogioSistema.hoje().minusDays(3), anterior = posterior.minusDays(4);
        var reativacao = requisicao(c, id, posterior, true); confirmarPrevia(c, id, reativacao);
        enviar(c.localA, rota(id), reativacao, 200, "CAT006 fonte posterior explícita do estado ativo");
        var inativacao = requisicao(c, id, anterior, false); confirmarPrevia(c, id, inativacao);
        JsonNode registrada = enviar(c.localA, rota(id), inativacao, 200, "CAT007 registro tardio respeita a ordem efetiva dos fatos");
        assertThat(registrada.path("ativoAtualAposRegistro").asBoolean()).isTrue();
        assertThat(disponibilidade(c, id, anterior, null).path("ativo").asBoolean()).isFalse();
        assertThat(disponibilidade(c, id, posterior, null).path("ativo").asBoolean()).isTrue();
        assertThat(disponibilidade(c, id, anterior.minusDays(1), null).path("ativo").isNull()).isTrue();
        assertThat(consultar(c.localA, "/api/cargos/" + id, "CAT006 projeção atual segue a última data efetiva").path("ativo").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(TipoCatalogoTemporal.class)
    void primeiraTransicaoAtualEReativacaoConservamIdEHistoriaSemInventarOrigem(TipoCatalogoTemporal tipo) throws Exception {
        Cenario c = criarCenarioPersistido(); long id = itemSemFonte(c, tipo);
        var req = mapa("dataEfetiva", RelogioSistema.hoje(), "ativo", false, "versaoEsperada", 0,
                "motivo", "Inativação administrativa documentada", "evidencia", "SEI-ATUAL-" + tipo,
                "autorizacao", "ATO-ATUAL-" + tipo, "chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode primeira = enviar(c.localA, rota(tipo, id), req, 200, "CAT006 primeira transição atual de cada catálogo");
        assertThat(primeira.path("ativoAtualAposRegistro").asBoolean()).isFalse();
        var semFonte = consultar(c.localA, disponibilidadeRota(tipo, id, DATA, null), "CAT007 primeira transição não inventa origem antiga");
        assertThat(semFonte.path("ativo").isNull()).isTrue();
        assertThat(semFonte.path("disponibilidadeComprovada").asBoolean()).isFalse();
        req.put("ativo", true); req.put("versaoEsperada", primeira.path("versaoAtual").asLong());
        req.put("chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode segunda = enviar(c.localA, rota(tipo, id), req, 200, "CAT006 reativação explícita conserva identidade");
        assertThat(segunda.path("catalogoId").asLong()).isEqualTo(id);
        assertThat(segunda.path("ativoAtualAposRegistro").asBoolean()).isTrue();
        assertThat(contar(tipo, id)).isEqualTo(2);
    }

    @Test
    void reenvioDaMesmaIntencaoRetornaMesmoEventoSemExigirVersaoJaConsumida() throws Exception {
        Cenario c = criarCenarioPersistido(); long id = cargoNovo(c, true);
        var req = requisicao(c, id, RelogioSistema.hoje().minusDays(5), false); confirmarPrevia(c, id, req);
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        JsonNode primeira = enviar(c.localA, rota(id), req, 200, "CAT007 primeiro registro da intenção documentada");
        JsonNode segunda = enviar(c.localA, rota(id), req, 200, "AUD001 reenvio idempotente não duplica evento ou versão");
        assertThat(segunda.path("eventoId").asLong()).isEqualTo(primeira.path("eventoId").asLong());
        assertThat(segunda.path("versaoAtual").asLong()).isEqualTo(primeira.path("versaoAtual").asLong());
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes + 1);
        req.put("ativo", true);
        enviar(c.localA, rota(id), req, 409, "AUD001 mesma chave com outra intenção é conflito");
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEM_EVIDENCIA", "SEM_AUTORIZACAO", "SEM_MOTIVO", "SEM_VERSAO", "FUTURA", "SEM_TOKEN", "SEM_CONFIRMACAO", "VERSAO_OBSOLETA"})
    void requisicaoIncompletaOuNaoConfirmadaNaoProduzFatos(String defeito) throws Exception {
        Cenario c = criarCenarioPersistido(); long id = cargoNovo(c, true);
        var req = requisicao(c, id, RelogioSistema.hoje().minusDays(4), false);
        confirmarPrevia(c, id, req);
        int status = 400;
        switch (defeito) {
            case "SEM_EVIDENCIA" -> req.remove("evidencia");
            case "SEM_AUTORIZACAO" -> req.remove("autorizacao");
            case "SEM_MOTIVO" -> req.remove("motivo");
            case "SEM_VERSAO" -> req.remove("versaoEsperada");
            case "FUTURA" -> req.put("dataEfetiva", RelogioSistema.hoje().plusDays(1));
            case "SEM_TOKEN" -> { req.remove("tokenPrevia"); status = 409; }
            case "SEM_CONFIRMACAO" -> { req.put("confirmarImpacto", false); status = 409; }
            case "VERSAO_OBSOLETA" -> { req.put("versaoEsperada", 99999L); status = 409; }
        }
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        enviar(c.localA, rota(id), req, status, "CAT007/AUD004 validação atômica: " + defeito);
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes);
        assertThat(consultar(c.localA, "/api/cargos/" + id, "Sem efeito parcial no cadastro").path("ativo").asBoolean()).isTrue();
    }

    @Test
    void previaObsoletaPorNovoVinculoExigeNovaAnaliseMesmoSemMudarVersaoDoCatalogo() throws Exception {
        Cenario c = criarCenarioPersistido(); long id = c.cargoA.getId();
        var req = requisicao(c, id, DATA, true); confirmarPrevia(c, id, req);
        enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa altera impacto " + id,
                        "cpf", cpf(660000000 + Math.toIntExact(id))), "contratoId", c.contratoA.getId(),
                "composicaoId", c.composicaoA.getId(), "dataInicio", DATA.plusDays(1)), 201,
                "Novo fato dependente surgiu após a prévia de disponibilidade");
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        enviar(c.localA, rota(id), req, 409, "AUD004 assinatura inclui dependências, não apenas a flag do catálogo");
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OUTRA_GESTAO", "OPERACIONAL", "ESTADUAL", "NACIONAL", "PRESIDENTE", "ADMINISTRADOR"})
    void somenteGestorLocalDaPropriaGestaoPodeRegistrarOuPrepararTransicao(String perfil) throws Exception {
        Cenario c = criarCenarioPersistido(); long id = cargoNovo(c, true);
        UsuarioAplicacao executor = switch (perfil) {
            case "OUTRA_GESTAO" -> c.localB; case "OPERACIONAL" -> c.operacionalA;
            case "ESTADUAL" -> c.estadualA; case "NACIONAL" -> c.nacional;
            case "PRESIDENTE" -> c.presidente; default -> c.admin;
        };
        var req = requisicao(c, id, DATA, false);
        int status = perfil.equals("OUTRA_GESTAO") ? 404 : 403;
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        enviar(executor, rota(id) + "/previa", req, status, "PER escopo da prévia do catálogo");
        enviar(executor, rota(id), req, status, "PER escopo da mutação do catálogo");
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes);
    }

    @Test
    void atoRetroativoNaoPodeInvalidarEscolhaPosteriorNemApagarSuaFonte() throws Exception {
        Cenario c = criarCenarioPersistido(); long id = c.cargoA.getId();
        var reativacao = requisicao(c, id, RelogioSistema.hoje(), true);
        enviar(c.localA, rota(id), reativacao, 200, "CAT006 estado ativo atual possui ato explícito posterior");
        JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa depende da validade " + id,
                        "cpf", cpf(670000000 + Math.toIntExact(id))), "contratoId", c.contratoA.getId(),
                "composicaoId", c.composicaoA.getId(), "dataInicio", DATA.plusDays(2)), 201,
                "CAT007 escolha posterior nasceu sob disponibilidade comprovada");
        var req = requisicao(c, id, DATA, false);
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        enviar(c.localA, rota(id) + "/previa", req, 409,
                "TMP009 ato invalidaria escolha posterior e exige tratamento explícito antes de continuar");
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes);
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculo.path("id").asLong()).getDataInicio()).isEqualTo(DATA.plusDays(2));
            assertThat(em.find(Cargo.class, id).getAtivo()).isTrue(); return null;
        });
    }

    @Test
    void inativacaoAtualPelaNovaRotaRespeitaProgramacaoFutura() throws Exception {
        Cenario c = criarCenarioPersistido(); long id = c.cargoA.getId();
        var req = requisicao(c, id, RelogioSistema.hoje(), false);
        long antes = contar(TipoCatalogoTemporal.CARGO, id);
        enviar(c.localA, rota(id), req, 409, "CAT005 nova rota mantém guarda de programação futura");
        assertThat(contar(TipoCatalogoTemporal.CARGO, id)).isEqualTo(antes);
        emTransacao(() -> { assertThat(em.find(Cargo.class, id).getAtivo()).isTrue(); return null; });
    }

    private long cargoNovo(Cenario c, boolean ativo) throws Exception {
        String nome = "Cargo ato " + UUID.randomUUID();
        // CargoControlador.criar e o contrato CatalogosContratoHttpIT usam 200.
        // CAT002 exige o cadastro próprio na Gestão, sem prescrever 201.
        JsonNode criado = enviar(c.localA, "/api/cargos", mapa("nome", nome, "ativo", ativo),
                200, "CAT002 criação prospectiva canônica: contrato HTTP de Cargo");
        long id = criado.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(criado.path("nome").asText()).isEqualTo(nome);
        assertThat(criado.path("ativo").asBoolean()).isEqualTo(ativo);
        assertThat(criado.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        emTransacao(() -> {
            Cargo salvo = em.find(Cargo.class, id);
            assertThat(salvo).isNotNull();
            assertThat(salvo.getNome()).isEqualTo(nome);
            assertThat(salvo.getAtivo()).isEqualTo(ativo);
            assertThat(salvo.getGestaoLocal().getId()).isEqualTo(c.gestaoA.getId());
            return null;
        });
        return id;
    }
    private Map<String, Object> requisicao(Cenario c, long id, LocalDate data, boolean ativo) throws Exception {
        long versao = consultar(c.localA, "/api/cargos/" + id, "Versão atual do catálogo antes do ato").path("version").asLong();
        return mapa("dataEfetiva", data, "ativo", ativo, "motivo", "Regularização administrativa documental da disponibilidade",
                "evidencia", "SEI-CAT-" + id, "autorizacao", "ATO-CAT-" + id, "versaoEsperada", versao,
                "chaveIdempotencia", UUID.randomUUID().toString());
    }
    private void confirmarPrevia(Cenario c, long id, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, rota(id) + "/previa", req, 200, "AUD004 prévia de disponibilidade retroativa");
        assertThat(previa.path("tokenPrevia").asText()).isNotBlank();
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
    }
    private JsonNode disponibilidade(Cenario c, long id, LocalDate data, Instant corte) throws Exception {
        return consultar(c.localA, disponibilidadeRota(TipoCatalogoTemporal.CARGO, id, data, corte), "CAT007 leitura nos dois cortes");
    }
    private String disponibilidadeRota(TipoCatalogoTemporal tipo, long id, LocalDate data, Instant corte) {
        return "/api/catalogos-temporais/" + tipo + "/" + id + "/disponibilidade?data=" + data
                + (corte == null ? "" : "&conhecidoAte=" + corte);
    }
    private String rota(long id) { return rota(TipoCatalogoTemporal.CARGO, id); }
    private String rota(TipoCatalogoTemporal tipo, long id) { return "/api/catalogos-temporais/" + tipo + "/" + id + "/transicoes"; }
    private long contar(TipoCatalogoTemporal tipo, long id) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                .setParameter("tipo", tipo).setParameter("id", id).getSingleResult());
    }
    private long itemSemFonte(Cenario c, TipoCatalogoTemporal tipo) {
        return emTransacao(() -> {
            GestaoLocal gestao = em.getReference(GestaoLocal.class, c.gestaoA.getId());
            return switch (tipo) {
                case CARGO -> { Cargo x = new Cargo("Cargo sem origem " + UUID.randomUUID(), "1"); x.setGestaoLocal(gestao); em.persist(x); yield x.getId(); }
                case JORNADA -> { Jornada x = new Jornada(); x.setGestaoLocal(gestao); x.setDescricao("Turno sem origem " + UUID.randomUUID());
                    em.persist(x); yield x.getId(); }
                case LOTACAO -> { Lotacao x = new Lotacao("Lotação sem origem " + UUID.randomUUID()); x.setGestaoLocal(gestao);
                    x.getTurnos().add(em.getReference(Jornada.class, c.jornadaA.getId())); em.persist(x); yield x.getId(); }
                case TIPO_CONTRATACAO -> { TipoContratacao x = new TipoContratacao(); x.setGestaoLocal(gestao); x.setDescricao("Tipo sem origem " + UUID.randomUUID()); em.persist(x); yield x.getId(); }
                case MOTIVO_DESLIGAMENTO -> { MotivoDesligamento x = new MotivoDesligamento(); x.setGestaoLocal(gestao); x.setDescricao("Motivo sem origem " + UUID.randomUUID());
                    x.setCategoria(CategoriaTerminoVinculo.OUTRO); em.persist(x); yield x.getId(); }
            };
        });
    }
}
