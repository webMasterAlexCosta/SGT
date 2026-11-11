package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Cada cenário afirma uma fronteira ainda não coberta pelos controles abrangentes do inventário. */
class ContratosCriteriosComplementaresHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;
    @Autowired DisponibilidadeCatalogoTemporalServico disponibilidade;

    @Test
    void mesmoNumeroEmAnosDiferentesIdentificaDoisContratosPersistidos() throws Exception {
        var c = criarCenario(); var inicio = RelogioSistema.hoje().plusDays(5); String numero = "ANOS-" + c.gestaoA.getId();
        var req = rascunho(c, numero, 5, inicio, inicio.plusMonths(2));
        long primeiro = enviar(c.localA, "/api/contratos", req, 201, "CON001 número/ano original").path("id").asLong();
        req.put("anoContrato", inicio.getYear() + 1);
        long segundo = enviar(c.localA, "/api/contratos", req, 201, "CON001 mesmo número em ano distinto não colide").path("id").asLong();
        em.flush(); em.clear();
        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(em.find(Contrato.class, primeiro).getAnoContrato()).isEqualTo(inicio.getYear());
        assertThat(em.find(Contrato.class, segundo).getAnoContrato()).isEqualTo(inicio.getYear() + 1);
        assertThat(em.find(Contrato.class, primeiro).getNumeroContrato()).isEqualTo(numero);
        assertThat(em.find(Contrato.class, segundo).getNumeroContrato()).isEqualTo(numero);
    }

    @ParameterizedTest
    @ValueSource(strings = {"vigenciaInvertida", "tipoAusente", "tipoLivre", "cnpjCurto"})
    void contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial(String caso) throws Exception {
        var c = criarCenario(); var inicio = RelogioSistema.hoje().plusDays(5);
        String numero = "VALIDACAO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var req = rascunho(c, numero, 5, inicio, inicio.plusMonths(2));
        switch (caso) {
            case "vigenciaInvertida" -> req.put("dataFim", inicio.minusDays(1));
            case "tipoAusente" -> req.remove("tipoContratacaoId");
            case "tipoLivre" -> { req.remove("tipoContratacaoId"); req.put("tipo", "Texto sem catálogo próprio"); }
            case "cnpjCurto" -> req.put("cnpjEmpresa", "12345");
            default -> throw new IllegalArgumentException(caso);
        }
        if (caso.equals("tipoAusente")) {
            long id = enviar(c.localA, "/api/contratos", req, 201, "CON008 rascunho pode omitir Tipo").path("id").asLong();
            enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "Composição válida isola a ausência do Tipo");
            enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-VALIDACAO"),
                    400, "CON008 publicação exige Tipo cadastrado");
            em.flush(); em.clear();
            assertThat(em.find(Contrato.class, id).getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.RASCUNHO);
            assertThat(contarEventos(id)).isZero();
        } else {
            enviar(c.localA, "/api/contratos", req, 400, "CON006/CAT001/EMP001 entrada inválida: " + caso);
            em.flush(); em.clear();
            assertThat(em.createQuery("select count(c) from Contrato c where c.numeroContrato=:numero", Long.class)
                    .setParameter("numero", numero).getSingleResult()).isZero();
        }
    }

    @ParameterizedTest
    @CsvSource({"anular,dataAto", "anular,referenciaSei", "encerrar-antecipadamente,referenciaSei"})
    void cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo(String operacao, String ausente) throws Exception {
        var c = criarCenario(); LocalDate fim = c.contratoA.getDataFim(); long eventos = contarEventos(c.contratoA.getId());
        var req = mapa("dataEfetiva", DATA, "dataAto", DATA.plusDays(2), "motivo", "Ato documental de controle", "referenciaSei", "SEI-CONTROLE");
        req.remove(ausente);
        enviar(c.localA, base(c.contratoA.getId()) + "/" + operacao, req, 400,
                "CON015/016 ausência isolada de campo obrigatório: " + ausente);
        em.flush(); em.clear();
        assertThat(em.find(Contrato.class, c.contratoA.getId()).getDataFim()).isEqualTo(fim);
        assertThat(em.find(Contrato.class, c.contratoA.getId()).getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.PUBLICADO);
        assertThat(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataFim()).isNull();
        assertThat(contarEventos(c.contratoA.getId())).isEqualTo(eventos);
    }

    @Test
    void cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, true);
        var req = mapa("dataEfetiva", RelogioSistema.hoje(), "motivo", "Cancelamento formal do instrumento futuro", "referenciaSei", "SEI-CANCELAR");
        enviar(c.localA, base(c.contratoA.getId()) + "/cancelar-futuro", req, 409, "CON021 contrato já iniciado não é futuro cancelável");
        assertThat(em.find(Contrato.class, c.contratoA.getId()).getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.PUBLICADO);
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", vinculo(c, n, n.inicio(), 841000000),
                201, "CON021 programação criada antes do cancelamento").path("id").asLong();
        confirmar(c, n.id(), "cancelar-futuro", req);
        em.flush(); em.clear();
        assertThat(em.find(Contrato.class, n.id()).getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.CANCELADO);
        assertThat(em.find(VinculoContratual.class, vinculo).getEstadoNegocial().name()).isEqualTo("CANCELADO");
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo, "Programação perdeu efeito sem perder identidade")
                .path("situacaoTemporal").asText()).isEqualTo("CANCELADO");
        enviar(c.localA, "/api/vinculos-contratuais", vinculo(c, n, n.inicio(), 842000000), 409,
                "CON021 novo vínculo não opera em contrato cancelado");
        executar(c.localA, delete(base(n.id())), 409, "CON025 contrato cancelado não é apagado");
        var evento = evento(consultar(c.localA, base(n.id()) + "/eventos", "CON025 história do cancelamento"), "CANCELAMENTO_CONTRATO_FUTURO");
        assertThat(evento.path("motivo").asText()).isEqualTo(req.get("motivo"));
        assertThat(evento.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(evento.path("dataEfetiva").asText()).isEqualTo(RelogioSistema.hoje().toString());
        assertThat(evento.path("registradoEm").asText()).isNotBlank();
        assertThat(em.find(VinculoContratual.class, vinculo).getContrato().getId()).isEqualTo(n.id());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void encerramentoSemCausaCadastradaFazRollbackEDepoisEncerraSemRealocacao() throws Exception {
        var c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTRATO-RESCISAO-SEM-CAUSA-ROLLBACK");
        LocalDate fim = emTransacao(() -> em.find(Contrato.class, c.contratoA.getId()).getDataFim());
        long antes = emTransacao(() -> contarEventos(c.contratoA.getId()));
        var req = mapa("dataEfetiva", DATA, "dataAto", DATA, "motivo", "Rescisão comprovada", "referenciaSei", "SEI-RESCISAO");
        previaCiclo(c, c.contratoA.getId(), "encerrar-antecipadamente", req);
        enviar(c.localA, base(c.contratoA.getId()) + "/encerrar-antecipadamente", req, 409,
                "CAT009 ausência de causa própria não cria motivo genérico");
        emTransacao(() -> {
            assertThat(em.find(Contrato.class, c.contratoA.getId()).getDataFim()).isEqualTo(fim);
            assertThat(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataFim()).isNull();
            assertThat(contarEventos(c.contratoA.getId())).isEqualTo(antes);
            assertThat(em.createQuery("select count(m) from MotivoDesligamento m where m.gestaoLocal.id=:g and m.categoria=:categoria", Long.class)
                    .setParameter("g", c.gestaoA.getId()).setParameter("categoria", CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO)
                    .getSingleResult()).isZero();
            var m = new MotivoDesligamento(); m.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            m.setDescricao("Encerramento antecipado documental"); m.setCategoria(CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO); em.persist(m);
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
            origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa de rescisão desde o início contratual"); em.persist(origem);
            FonteMotivoSintetica.declarar(em, gestaoTemporal, c.contratoA.getId(), m.getId(),
                    c.gestaoA.getId(), c.localA, "Encerramento antecipado documental",
                    CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO, DATA, DATA,
                    java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), "SEI-SINTETICO-RESCISAO-DEPOIS-ROLLBACK-SEM-CAUSA");
            return null;
        });
        confirmar(c, c.contratoA.getId(), "encerrar-antecipadamente", req);
        emTransacao(() -> {
            var v = em.find(VinculoContratual.class, c.vinculoA.getId());
            var a = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
            assertThat(v.getDataFim()).isEqualTo(DATA); assertThat(v.getContrato().getId()).isEqualTo(c.contratoA.getId());
            assertThat(a.getVinculo().getId()).isEqualTo(v.getId()); assertThat(a.getLotacao().getId()).isEqualTo(c.lotacaoA.getId());
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:id", Long.class)
                    .setParameter("id", c.pessoaA.getId()).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    @Test
    void pendenciaDeReplayNaoTrocaEstadoNegocialNemHabilitacao() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        var posterior = remanejamento(n, "REM-PENDENCIA", n.inicio().plusDays(5), 20); previaRemanejamento(c, n.id(), posterior);
        var rem = enviar(c.localA, base(n.id()) + "/remanejamentos", posterior, 201, "Posterior inicialmente válido");
        var anterior = aditamento("ADI-PENDENCIA", n.inicio().plusDays(2)); anterior.put("novoEfetivoTotal", 90);
        anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 15)));
        registrarAditamento(c, n.id(), anterior); em.flush(); em.clear();
        assertThat(em.createQuery("select count(p) from PendenciaAdministrativa p where p.contratoId=:c and p.origemId=:e", Long.class)
                .setParameter("c", n.id()).setParameter("e", rem.path("eventoId").asLong()).getSingleResult()).isEqualTo(1L);
        var atual = consultar(c.localA, base(n.id()), "CON022 pendência não é estado de contrato");
        assertThat(atual.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(atual.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "CON022 posição canônica mantém eixos");
        assertThat(posicao.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(posicao.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
    }

    @Test
    void cargoNormalizadoDuplicadoERecorteDeOutraGestaoSaoRecusadosSemGravarComposicao() throws Exception {
        var c = criarCenario(); String nome = "Cargo de prova " + c.gestaoA.getId();
        var salvo = enviar(c.localA, "/api/cargos", mapa("nome", "  " + nome + "  ", "ativo", true), 200, "CAT002 normalização");
        assertThat(salvo.path("nome").asText()).isEqualTo(nome);
        enviar(c.localA, "/api/cargos", mapa("nome", nome.toLowerCase(), "ativo", true), 409, "CAT002 duplicata normalizada");
        long id = rascunhoNovo(c);
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoB.getId(), 5), 400,
                "CAT002 Cargo da outra Gestão não compõe o contrato local");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(id) + "/composicoes", "Nenhuma composição fora de escopo foi criada").isEmpty()).isTrue();
        assertThat(em.createQuery("select count(c) from Cargo c where c.gestaoLocal.id=:g and lower(c.nome)=:nome", Long.class)
                .setParameter("g", c.gestaoA.getId()).setParameter("nome", nome.toLowerCase()).getSingleResult()).isEqualTo(1L);
    }

    @Test
    void reativacaoDeCargoPreservaIntervaloInativoAutoriaEPermiteComposicaoFutura() throws Exception {
        var c = criarCenario(); LocalDate hoje = RelogioSistema.hoje(); String nome = "Cargo reativado " + c.gestaoA.getId();
        long id = enviar(c.localA, "/api/cargos", mapa("nome", nome, "ativo", true), 200, "CAT006 catálogo sem dependências").path("id").asLong();
        executar(c.localA, delete("/api/cargos/" + id), 204, "CAT006 inativação efetiva inicial");
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(hoje.plusDays(2));
            var reativado = dados(executar(c.localA, put("/api/cargos/" + id).contentType(MediaType.APPLICATION_JSON)
                    .content(json(mapa("nome", nome, "ativo", true))), 200, "CAT006 reativação explícita posterior"));
            assertThat(reativado.path("id").asLong()).isEqualTo(id);
            long contrato = rascunhoNovo(c);
            enviar(c.localA, base(contrato) + "/composicoes", composicao(c, id, 5), 201, "CAT008 reativação restaura elegibilidade futura");
            enviar(c.localA, base(contrato) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-REATIVADO"),
                    200, "CAT008 composição futura com Cargo reativado pode ser publicada");
        }
        em.flush(); em.clear();
        assertThat(disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, hoje.plusDays(1), true)).isFalse();
        assertThat(disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, hoje.plusDays(2), true)).isTrue();
        var fatos = em.createQuery("select e from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:t and e.catalogoId=:id order by e.dataEfetiva,e.registradoEm,e.id", EventoDisponibilidadeCatalogo.class)
                .setParameter("t", TipoCatalogoTemporal.CARGO).setParameter("id", id).getResultList();
        assertThat(fatos).hasSize(3); var reativacao = fatos.get(2);
        assertThat(reativacao.getAtivo()).isTrue(); assertThat(reativacao.getAtivoAnterior()).isFalse();
        assertThat(reativacao.getResponsavelId()).isEqualTo(c.localA.getId());
        assertThat(reativacao.getDataEfetiva()).isEqualTo(hoje.plusDays(2)); assertThat(reativacao.getRegistradoEm()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"lotacaoId", "jornadaId", "cargoId"})
    void composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita(String ausente) throws Exception {
        var c = criarCenario(); long id = rascunhoNovo(c); var req = composicao(c, c.cargoA.getId(), 5); req.remove(ausente);
        enviar(c.localA, base(id) + "/composicoes", req, 400, "CMP001 falta identificador explícito " + ausente);
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(id) + "/composicoes", "CMP001 recusa não cria combinação automática").isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"identificacao", "dataDocumento", "dataEfetiva", "justificativa"})
    void aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento(String ausente) throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var req = aditamento("ADI-OBRIGATORIO", RelogioSistema.hoje()); req.put("novaDescricao", "Conteúdo pretendido"); req.remove(ausente);
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400, "ADI002 falta " + ausente);
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI002 nenhum instrumento sem metadados").isEmpty()).isTrue();
        assertThat(contarEventos(n.id())).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao(boolean depois) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        em.flush(); em.clear();
        java.time.Instant inicioConsultas = java.time.Instant.now();
        var composicoesAntes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + n.inicio(),
                "REM003 fotografia completa por ID antes da recusa de data");
        var paresAntes = java.util.stream.StreamSupport.stream(composicoesAntes.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(item -> item.path("id").asLong(), item -> item.path("efetivoPrevisto").asInt()));
        assertThat(paresAntes).isEqualTo(java.util.Map.of(n.composicao1(), 25, n.composicao2(), 75));
        var req = remanejamento(n, "REM-FORA", depois ? n.fim().plusDays(1) : n.inicio().minusDays(1), 5);
        enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req, 400, "CON017/REM003 data fora da vigência");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/remanejamentos", "REM003 nenhum fato inválido").isEmpty()).isTrue();
        assertThat(consultar(c.localA, base(n.id()) + "/composicoes", "REM003 somente recortes originais").size()).isEqualTo(2);
        var composicoesDepois = consultar(c.localA, base(n.id()) + "/composicoes?data=" + n.inicio(),
                "REM003 nova consulta dos mesmos IDs e conteúdo após recusa de data");
        for (var consulta : java.util.List.of(composicoesAntes, composicoesDepois)) {
            for (var composicao : consulta) {
                assertThat(composicao.path("conhecidoAte").isTextual()).isTrue();
                java.time.Instant corte = java.time.Instant.parse(composicao.path("conhecidoAte").asText());
                assertThat(corte).isBetween(inicioConsultas.minusSeconds(1), java.time.Instant.now().plusSeconds(1));
                // Somente o instante automático do GET; datas persistidas das fontes continuam comparadas.
                ((com.fasterxml.jackson.databind.node.ObjectNode) composicao).remove("conhecidoAte");
            }
        }
        assertThat(composicoesDepois).isEqualTo(composicoesAntes);
        assertThat(java.util.stream.StreamSupport.stream(composicoesDepois.spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(item -> item.path("id").asLong(), item -> item.path("efetivoPrevisto").asInt())))
                .isEqualTo(paresAntes);
        assertThat(contarEventos(n.id())).isEqualTo(1L);
    }

    @Test
    void totalDoAditamentoDeveConferirComDeltasSemMutacaoParcial() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        var req = aditamento("ADI-SOMA-DIVERGENTE", RelogioSistema.hoje()); req.put("novoEfetivoTotal", 101);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 25)));
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400, "CMP003 total101 não corresponde à soma100");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "CMP003 nenhuma alteração parcial").isEmpty()).isTrue();
        assertThat(contarEventos(n.id())).isEqualTo(1L);
        assertThat(consultar(c.localA, base(n.id()), "CMP003 total persistido permanece100").path("efetivoPrevisto").asInt()).isEqualTo(100);
    }

    @Test
    void novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false); LocalDate efeito = RelogioSistema.hoje().plusDays(5);
        var req = aditamento("ADI-DEPENDENCIA-TRATADA", efeito); req.put("novoEfetivoTotal", 75);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 0)));
        String tokenAntes = previaAditamento(c, n.id(), req).path("tokenPrevia").asText();
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", vinculo(c, n, efeito.plusDays(1), 843000000),
                201, "CMP007 dependência criada depois da prévia").path("id").asLong();
        enviar(c.localA, base(n.id()) + "/aditamentos", req, 409, "CMP007 intenção antiga não confirma depois de nova ocupação");
        var afetada = previaAditamento(c, n.id(), req);
        assertThat(afetada.path("tokenPrevia").asText()).isNotEqualTo(tokenAntes);
        assertThat(afetada.path("possuiImpactoOcupacao").asBoolean()).isTrue();
        enviar(c.localA, base(n.id()) + "/aditamentos", req, 409, "CMP006 confirmação não elimina dependência incompatível");
        enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/cancelar-programacao",
                mapa("justificativa", "Programação cancelada pelo responsável", "evidencia", "SEI-TRATAMENTO", "autorizacao", "Despacho local"),
                200, "CMP006 tratamento explícito da dependência");
        var tratada = previaAditamento(c, n.id(), req);
        assertThat(tratada.path("possuiImpactoOcupacao").asBoolean()).isFalse();
        enviar(c.localA, base(n.id()) + "/aditamentos", req, 201, "CMP006 após tratar dependência redução pode confirmar");
        em.flush(); em.clear();
        assertThat(em.find(VinculoContratual.class, vinculo).getEstadoNegocial().name()).isEqualTo("CANCELADO");
        assertThat(quantidade(consultar(c.localA, base(n.id()) + "/composicoes?data=" + efeito, "Capacidade reduzida na data"), n.composicao1())).isZero();
    }

    @Test
    void aumentoAbsorveExcedenteSemReescreverEpisodioAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 1, null, false); LocalDate data = n.inicio().plusDays(1);
        enviar(c.localA, "/api/vinculos-contratuais", vinculo(c, n, data, 844000000), 201, "CMP011 capacidade original ocupada");
        var extra = vinculo(c, n, data, 845000000); extra.put("confirmarInclusaoExcedente", true);
        extra.put("justificativaExcedente", "Necessidade documentada do episódio original"); extra.put("evidenciaExcedente", "SEI-EPISODIO"); extra.put("autorizacaoExcedente", "Despacho local");
        enviar(c.localA, "/api/vinculos-contratuais", extra, 201, "CMP011 episódio excedente documentado");
        var req = aditamento("ADI-ABSORCAO", data.plusDays(1)); req.put("novoEfetivoTotal", 2);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 2)));
        registrarAditamento(c, n.id(), req); em.flush(); em.clear();
        var atual = consultar(c.localA, base(n.id()) + "/composicoes/excedentes?data=" + data.plusDays(1), "CMP011 capacidade absorveu excedente").get(0);
        assertThat(atual.path("quantidadeExcedente").asLong()).isZero();
        var anterior = consultar(c.localA, base(n.id()) + "/composicoes/excedentes?data=" + data, "CMP011 episódio passado ainda comprovado").get(0);
        assertThat(anterior.path("efetivoPrevisto").asInt()).isEqualTo(1); assertThat(anterior.path("quantidadeVinculadaAtual").asInt()).isEqualTo(2);
        assertThat(anterior.path("quantidadeExcedente").asInt()).isEqualTo(1); assertThat(anterior.path("desdeQuandoExcedente").asText()).isEqualTo(data.toString());
        assertThat(anterior.path("justificativas").toString()).contains("Necessidade documentada do episódio original");
    }

    @Test
    void reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        for (int i = 0; i < 2; i++) {
            int total = i == 0 ? 90 : 75;
            var req = aditamento("REDUCAO-SUCESSIVA-" + total, n.inicio().plusDays(i + 1)); req.put("novoEfetivoTotal", total);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", total)));
            var previa = previaAditamento(c, n.id(), req);
            assertThat(previa.path("efetivoOriginalPublicado").asInt()).isEqualTo(100); assertThat(previa.path("pisoNormalReducao").asInt()).isEqualTo(75);
            enviar(c.localA, base(n.id()) + "/aditamentos", req, 201, "RED001 reduções100→90→75 cumulativas");
        }
        var outro = publicado(c, 100, null, false); long aumentoId = 0;
        for (int i = 0; i < 2; i++) {
            int total = i == 0 ? 140 : 120;
            var req = aditamento("QUANTIDADE-" + total, outro.inicio().plusDays(i + 1)); req.put("novoEfetivoTotal", total);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", outro.composicao1(), "quantidadeResultante", total)));
            var salvo = registrarAditamento(c, outro.id(), req); if (i == 0) aumentoId = salvo.path("id").asLong();
        }
        em.flush(); em.clear();
        var historico = consultar(c.localA, base(outro.id()) + "/aditamentos", "ADI003 valores originais não são recalculados pela redução posterior");
        assertThat(historico.size()).isEqualTo(2); assertThat(historico.get(0).path("id").asLong()).isEqualTo(aumentoId);
        var item = historico.get(0).path("alteracoesComposicao").get(0);
        assertThat(item.path("quantidadeAnterior").asInt()).isEqualTo(100);
        assertThat(item.path("variacao").asInt()).isEqualTo(40); assertThat(item.path("quantidadeResultante").asInt()).isEqualTo(140);
        assertThat(consultar(c.localA, base(n.id()), "RED001 total após reduções sucessivas").path("efetivoPrevisto").asInt()).isEqualTo(75);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aditamentos", "remanejamentos"})
    void cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior(String recurso) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, true); LocalDate data = n.inicio().plusDays(1); JsonNode ato;
        if (recurso.equals("aditamentos")) {
            var req = aditamento("ADI-CANCELAMENTO-PROVA", data); req.put("novaDescricao", "Descrição ainda futura"); ato = registrarAditamento(c, n.id(), req);
        } else {
            var req = remanejamento(n, "REM-CANCELAMENTO-PROVA", data, 5); previaRemanejamento(c, n.id(), req);
            ato = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201, "REM001 redistribuição antes do cancelamento");
            var item = ato.path("itens").get(0);
            assertThat(item.path("quantidadeAnterior").asInt()).isEqualTo(25); assertThat(item.path("variacao").asInt()).isEqualTo(-5);
            assertThat(item.path("quantidadeResultante").asInt()).isEqualTo(20);
        }
        String motivo = "Cancelamento documental individualizado do instrumento";
        enviar(c.localA, base(n.id()) + "/" + recurso + "/" + ato.path("id").asLong() + "/cancelar",
                mapa("dataAto", RelogioSistema.hoje(), "motivo", motivo, "referenciaSei", "SEI-CANCELAMENTO"), 200, "ADI006/REM007 cancelamento rastreável");
        em.flush(); em.clear();
        var cancelamento = evento(consultar(c.localA, base(n.id()) + "/eventos", "Evento do cancelamento preservado"),
                recurso.equals("aditamentos") ? "CANCELAMENTO_ADITAMENTO" : "CANCELAMENTO_REMANEJAMENTO");
        assertThat(cancelamento.path("motivo").asText()).isEqualTo(motivo); assertThat(cancelamento.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(cancelamento.path("dataEfetiva").asText()).isEqualTo(data.toString()); assertThat(cancelamento.path("dataAto").asText()).isEqualTo(RelogioSistema.hoje().toString());
        assertThat(cancelamento.path("registradoEm").asText()).isNotBlank();
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias", "Cancelamento aponta exatamente o alvo original");
        assertThat(relacoes.size()).isEqualTo(1); assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("CANCELA");
        assertThat(relacoes.get(0).path("eventoFonteId").asLong()).isEqualTo(cancelamento.path("id").asLong());
        assertThat(relacoes.get(0).path("eventoAlvoId").asLong()).isEqualTo(ato.path("eventoId").asLong());
        var comps = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data, "REM007 distribuição anterior integralmente restaurada");
        assertThat(quantidade(comps, n.composicao1())).isEqualTo(25); assertThat(quantidade(comps, n.composicao2())).isEqualTo(75);
        var lista = consultar(c.localA, base(n.id()) + "/" + recurso, "Instrumento cancelado não é apagado");
        assertThat(lista.size()).isEqualTo(1); assertThat(lista.get(0).path("id").asLong()).isEqualTo(ato.path("id").asLong());
        assertThat(lista.get(0).path("situacao").asText()).isEqualTo("CANCELADO");
    }

    private long rascunhoNovo(Cenario c) throws Exception {
        var inicio = RelogioSistema.hoje().plusDays(5);
        return enviar(c.localA, "/api/contratos", rascunho(c, "CRITERIO-" + UUID.randomUUID().toString().substring(0, 8),
                5, inicio, inicio.plusMonths(2)), 201, "Preparação isolada").path("id").asLong();
    }
    private Map<String, Object> vinculo(Cenario c, NovoContrato n, LocalDate inicio, int semente) {
        return mapa("contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", inicio,
                "pessoa", mapa("nome", "Pessoa do critério " + semente, "cpf", cpf(semente + c.pessoaA.getId().intValue())));
    }
    private void previaCiclo(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "Prévia específica do ato");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
    }
    private void confirmar(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        previaCiclo(c, id, operacao, req); enviar(c.localA, base(id) + "/" + operacao, req, 200, "Confirmação após prévia");
    }
    private long contarEventos(long id) {
        return em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult();
    }
    private JsonNode evento(JsonNode eventos, String tipo) {
        for (var e : eventos) if (e.path("tipoEvento").asText().equals(tipo)) return e;
        throw new AssertionError("Evento não encontrado: " + tipo);
    }
    private int quantidade(JsonNode composicoes, long id) {
        for (var c : composicoes) if (c.path("id").asLong() == id) return c.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição não encontrada: " + id);
    }
}
