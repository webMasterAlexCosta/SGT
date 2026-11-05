package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Critérios negociais complementares: encadeamento, empresa, capacidade e publicação. */
class ContratosRegrasHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;
    @Test
    void aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var antes = aditamento("ANTES-INICIO", n.inicio().minusDays(1)); antes.put("novaDescricao", "Descrição precoce");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", antes, 400, "TMP-014 fato não antecipa vigência sem alteração válida");
        var depois = aditamento("APOS-FIM", n.fim().plusDays(1)); depois.put("novaDescricao", "Descrição posterior");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", depois, 400, "TMP-014 fato não ultrapassa vigência sem prorrogação");
        depois.put("novaDataFim", n.fim().plusDays(30));
        registrarAditamento(c, n.id(), depois);
        var p = posicao(c, n.id(), n.fim().plusDays(1));
        assertThat(p.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(p.path("vigenciaInicio").asText()).isEqualTo(n.inicio().toString());
        assertThat(p.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
    }

    @Test
    void seiCompartilhadoNaoViraIdentificadorUnicoDeContrato() throws Exception {
        var c = criarCenario(); var primeiro = publicado(c, 5, null, true); var segundo = publicado(c, 5, null, true);
        assertThat(primeiro.id()).isNotEqualTo(segundo.id());
        var fonte1 = consultar(c.localA, base(primeiro.id()) + "/documentos", "DOC-004 primeira referência SEI").get(0);
        var fonte2 = consultar(c.localA, base(segundo.id()) + "/documentos", "DOC-004 mesmo SEI em contrato distinto").get(0);
        assertThat(fonte1.path("referenciaSei").asText()).isEqualTo("SEI-ORIGINAL");
        assertThat(fonte2.path("referenciaSei").asText()).isEqualTo("SEI-ORIGINAL");
    }

    @Test
    void aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var r = aditamento("CONTEUDO-SEM-ESTADO", n.inicio().plusDays(1)); r.put("novaDescricao", "Descrição legítima");
        r.put("estadoNegocial", "ANULADO"); r.put("ativo", false);
        registrarAditamento(c, n.id(), r);
        var atual = consultar(c.localA, base(n.id()), "ADI-010 estados são controlados apenas por comandos próprios");
        assertThat(atual.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(atual.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(atual.path("descricao").asText()).isEqualTo("Descrição legítima");
    }

    @Test
    void identificadorEhGlobalEVigenciaCompletaEhObrigatoria() throws Exception {
        var c = criarCenario();
        var igual = rascunho(c, c.contratoA.getNumeroContrato(), 5, DATA, DATA.plusDays(10));
        igual.put("anoContrato", c.contratoA.getAnoContrato());
        enviar(c.localA, "/api/contratos", igual, 409, "CON-001 mesmo número/ano não pode coexistir");
        igual.put("gestaoLocalId", c.gestaoB.getId()); igual.put("tipoContratacaoId", c.tipoB.getId());
        enviar(c.localB, "/api/contratos", igual, 409, "CON-001 unicidade também entre Gestões distintas");
        for (String ausente : List.of("dataInicio", "dataFim", "cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa")) {
            var body = rascunho(c, "OBR-" + ausente, 5, RelogioSistema.hoje().plusDays(1), RelogioSistema.hoje().plusDays(30));
            body.remove(ausente);
            long id = enviar(c.localA, "/api/contratos", body, 201, "CON-008 rascunho pode omitir " + ausente).path("id").asLong();
            enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-OBR"),
                    400, "CON-006/EMP-001 publicação exige " + ausente);
        }
    }

    @Test
    void anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados() throws Exception {
        var c = criarCenario();
        documentarAdmissaoOriginal(c);
        var motivo = new MotivoDesligamento(); motivo.setGestaoLocal(c.gestaoA); motivo.setDescricao("Anulação documentada");
        motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO); motivo.setCriadoPorId(c.localA.getId());
        motivo.setCriadoPorUsername(c.localA.getUsername()); em.persist(motivo);
        // Fonte sintética explícita: a causa já existia no início do cenário.
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa de anulação desde o início contratual"); em.persist(origem);
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(),
                c.gestaoA.getId(), c.localA, "Anulação documentada", CategoriaTerminoVinculo.ANULACAO_CONTRATO,
                DATA, DATA, com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                "SEI-SINTETICO-CONTRATOS-ANULACAO-RESERVA-IDENTIFICADOR");
        var req = mapa("dataEfetiva", DATA, "dataAto", DATA.plusDays(3), "motivo", "Instrumento invalidado pelo ato formal", "referenciaSei", "SEI-ANULACAO");
        confirmarCiclo(c, c.contratoA.getId(), "anular", req);
        var evento = consultar(c.localA, base(c.contratoA.getId()) + "/eventos", "CON-024 datas e autor auditáveis").get(0);
        assertThat(evento.path("dataEfetiva").asText()).isEqualTo(DATA.toString());
        assertThat(evento.path("dataAto").asText()).isEqualTo(DATA.plusDays(3).toString());
        assertThat(evento.path("registradoEm").asText()).isNotBlank();
        assertThat(evento.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(evento.path("motivo").asText()).isEqualTo(req.get("motivo"));
        assertThat(evento.path("referenciaSei").asText()).isEqualTo("SEI-ANULACAO");
        var novo = rascunho(c, c.contratoA.getNumeroContrato(), 5, DATA, DATA.plusDays(30)); novo.put("anoContrato", c.contratoA.getAnoContrato());
        enviar(c.localA, "/api/contratos", novo, 409, "CON-019 anulação mantém número/ano reservado");
        executar(c.localA, delete(base(c.contratoA.getId())), 409, "GOV-003 anulação preserva contrato");
        assertThat(em.find(com.example.siggtrat.entidade.VinculoContratual.class, c.vinculoA.getId())).isNotNull();
        assertThat(c.vinculoA.getContrato().getId()).isEqualTo(c.contratoA.getId());
    }

    @Test
    void empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        LocalDate efeito = n.inicio().plusDays(4);
        var r = aditamento("EMPRESA-NOVA", efeito); r.put("novoCnpjEmpresa", "98765432000198");
        r.put("novaRazaoSocialEmpresa", "Empresa sucessora"); r.put("novoNomeFantasiaEmpresa", "Sucessora");
        registrarAditamento(c, n.id(), r);
        var antes = posicao(c, n.id(), efeito.minusDays(1)); var depois = posicao(c, n.id(), efeito);
        assertThat(antes.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(antes.path("versaoContratual").path("razaoSocialEmpresa").asText()).isEqualTo("Empresa Original");
        assertThat(antes.path("versaoContratual").path("nomeFantasiaEmpresa").asText()).isEqualTo("Original");
        assertThat(depois.path("versaoContratual").path("cnpjEmpresa").asText()).isEqualTo("98765432000198");
        assertThat(depois.path("versaoContratual").path("razaoSocialEmpresa").asText()).isEqualTo("Empresa sucessora");
        assertThat(depois.path("versaoContratual").path("nomeFantasiaEmpresa").asText()).isEqualTo("Sucessora");
        var outro = consultar(c.localA, base(c.contratoA.getId()), "EMP-002 todos os campos empresariais do outro contrato preservados");
        assertThat(outro.path("cnpjEmpresa").asText()).isEqualTo(c.contratoA.getCnpjEmpresa());
        assertThat(outro.path("razaoSocialEmpresa").asText()).isEqualTo(c.contratoA.getRazaoSocialEmpresa());
        assertThat(outro.path("nomeFantasiaEmpresa").asText()).isEqualTo(c.contratoA.getNomeFantasiaEmpresa());
    }

    @Test
    void mudancaSomenteDosNomesComMesmoCnpjNaoAlteraOutroContratoNemVersaoOriginal() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false); var outro = publicado(c, 5, null, false);
        // As duas leituras devem partir da persistência real. O contexto do
        // teste é compartilhado com MockMvc: uma entidade recém-criada ainda
        // retém nanos, enquanto a releitura MySQL retorna a precisão persistida.
        // Nenhum campo da fotografia é removido ou normalizado no oráculo.
        em.flush(); em.clear();
        LocalDate efeito = n.inicio().plusDays(4);
        var outroAntes = consultar(c.localA, base(outro.id()), "EMP002 contrato independente com o mesmo CNPJ");
        var versoesOutroAntes = consultar(c.localA, base(outro.id()) + "/versoes", "EMP002 fotografia independente antes do ato");
        var req = aditamento("EMPRESA-RENOMEADA-" + n.id(), efeito);
        req.put("novaRazaoSocialEmpresa", "Razão social documentada posteriormente");
        req.put("novoNomeFantasiaEmpresa", "Fantasia documentada posteriormente");
        registrarAditamento(c, n.id(), req);
        em.flush(); em.clear();
        var antes = posicao(c, n.id(), efeito.minusDays(1)).path("versaoContratual");
        var depois = posicao(c, n.id(), efeito).path("versaoContratual");
        assertThat(antes.path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(depois.path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(antes.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa Original");
        assertThat(antes.path("nomeFantasiaEmpresa").asText()).isEqualTo("Original");
        assertThat(depois.path("razaoSocialEmpresa").asText()).isEqualTo(req.get("novaRazaoSocialEmpresa"));
        assertThat(depois.path("nomeFantasiaEmpresa").asText()).isEqualTo(req.get("novoNomeFantasiaEmpresa"));
        var outroDepois = consultar(c.localA, base(outro.id()), "EMP002 mesmo CNPJ não compartilha a alteração entre contratos");
        for (String campo : List.of("cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa"))
            assertThat(outroDepois.path(campo)).isEqualTo(outroAntes.path(campo));
        assertThat(consultar(c.localA, base(outro.id()) + "/versoes", "EMP002 histórico independente byte a byte inalterado"))
                .isEqualTo(versoesOutroAntes);
        var original = consultar(c.localA, base(n.id()) + "/versoes", "EMP002 versão original preservada").get(0);
        assertThat(original.path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(original.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa Original");
        assertThat(original.path("nomeFantasiaEmpresa").asText()).isEqualTo("Original");
    }

    @Test
    void composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        LocalDate zero = n.inicio().plusDays(2); LocalDate retorno = zero.plusDays(3);
        var reduzir = aditamento("ZERAR", zero); reduzir.put("novoEfetivoTotal", 75);
        reduzir.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 0)));
        registrarAditamento(c, n.id(), reduzir);
        var incluir = vinculo(n, zero.plusDays(1), "52998224725"); incluir.put("confirmarInclusaoExcedente", true);
        incluir.put("justificativaExcedente", "Excedente não autoriza composição inexistente");
        enviar(c.localA, "/api/vinculos-contratuais", incluir, 409, "CMP-004 zero não é autorização de excedente");
        var restaurar = aditamento("RESTAURAR", retorno); restaurar.put("novoEfetivoTotal", 100);
        restaurar.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 25)));
        registrarAditamento(c, n.id(), restaurar);
        assertThat(quantidade(c, n, zero.plusDays(1))).isZero();
        assertThat(quantidade(c, n, retorno)).isEqualTo(25);
        incluir.put("dataInicio", retorno);
        enviar(c.localA, "/api/vinculos-contratuais", incluir, 201, "CMP-005 reativação por nova versão permite alocação");
    }

    @Test
    void excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve() throws Exception {
        var c = criarCenario(); var n = publicado(c, 1, null, false);
        LocalDate inicio = n.inicio().plusDays(1);
        enviar(c.localA, "/api/vinculos-contratuais", vinculo(n, inicio, "52998224725"), 201, "CMP-011 primeira ocupação");
        var extra = vinculo(n, inicio, "11144477735"); extra.put("confirmarInclusaoExcedente", true);
        extra.put("justificativaExcedente", "Necessidade operacional autorizada"); extra.put("evidenciaExcedente", "SEI-EXCEDENTE");
        extra.put("autorizacaoExcedente", "Despacho do Gestor Local");
        enviar(c.localA, "/api/vinculos-contratuais", extra, 201, "CMP-009 inclusão autorizada e auditada");
        var excedentes = consultar(c.localA, base(n.id()) + "/composicoes/excedentes?data=" + inicio, "CMP-011 monitoramento temporal");
        var ex = excedentes.get(0);
        assertThat(ex.path("composicaoId").asLong()).isEqualTo(n.composicao1());
        assertThat(ex.path("efetivoPrevisto").asInt()).isEqualTo(1);
        assertThat(ex.path("quantidadeVinculadaAtual").asLong()).isEqualTo(2);
        assertThat(ex.path("quantidadeExcedente").asLong()).isEqualTo(1);
        assertThat(ex.path("desdeQuandoExcedente").asText()).isEqualTo(inicio.toString());
        assertThat(ex.path("justificativas").toString()).contains("Necessidade operacional autorizada");
        var aumento = aditamento("ABSORVER", inicio.plusDays(1)); aumento.put("novoEfetivoTotal", 2);
        aumento.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 2)));
        registrarAditamento(c, n.id(), aumento);
        var atual = consultar(c.localA, base(n.id()) + "/composicoes/excedentes?data=" + inicio.plusDays(1), "CMP-011 aumento absorve excedente").get(0);
        assertThat(atual.path("excedente").asBoolean()).isFalse();
        assertThat(atual.path("quantidadeExcedente").asLong()).isZero();
    }

    @Test
    void reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var r = aditamento("REDUCAO-EXCECAO", n.inicio().plusDays(1)); r.put("novoEfetivoTotal", 60);
        r.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 60)));
        r.put("reducaoExcepcional", true);
        previaAditamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 400, "RED-008 não basta indicar exceção sem fundamento");
        r.put("fundamentoExcecao", "Ato jurídico específico documentado"); previaAditamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 403, "RED-008 exige concessão administrativa específica");
        conceder(c, n.id(), "REDUCAO_CONTRATUAL_EXCEPCIONAL");
        r.put("confirmarImpacto", false); previaAditamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 409, "RED-008 confirmação expressa obrigatória");
        r.put("confirmarImpacto", true); previaAditamento(c, n.id(), r);
        var salvo = enviar(c.localA, base(n.id()) + "/aditamentos", r, 201, "RED-008 exceção documentada permitida ao Gestor");
        assertThat(salvo.path("reducaoExcepcional").asBoolean()).isTrue();
        assertThat(salvo.path("fundamentoExcecao").asText()).isEqualTo(r.get("fundamentoExcecao"));
        assertThat(salvo.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
    }

    @Test
    void documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var posterior = aditamento("RECEBIDO-PRIMEIRO", n.inicio().plusDays(5)); posterior.put("novoEfetivoTotal", 110);
        posterior.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 110)));
        registrarAditamento(c, n.id(), posterior);
        var anterior = aditamento("RECEBIDO-DEPOIS", n.inicio().plusDays(2)); anterior.put("novoEfetivoTotal", 120);
        anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 120)));
        registrarAditamento(c, n.id(), anterior);
        assertThat(quantidade(c, n, n.inicio().plusDays(1))).isEqualTo(100);
        assertThat(quantidade(c, n, n.inicio().plusDays(2))).isEqualTo(120);
        assertThat(quantidade(c, n, n.inicio().plusDays(6))).isEqualTo(130);
        assertThat(posicao(c, n.id(), n.inicio().plusDays(6)).path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(130);
    }

    @Test
    void retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false);
        var rem = remanejamento(n, "POSTERIOR", n.inicio().plusDays(5), 20); previaRemanejamento(c, n.id(), rem);
        var salvo = enviar(c.localA, base(n.id()) + "/remanejamentos", rem, 201, "TMP-009 fato posterior inicialmente válido");
        var anterior = aditamento("RETRO-INCOMPATIVEL", n.inicio().plusDays(2)); anterior.put("novoEfetivoTotal", 90);
        anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 15)));
        registrarAditamento(c, n.id(), anterior);
        assertThat(consultar(c.localA, base(n.id()) + "/remanejamentos", "ADI-008 fato incompatível preservado").get(0).path("id").asLong())
                .isEqualTo(salvo.path("id").asLong());
        long pendencias = em.createQuery("select count(p) from PendenciaAdministrativa p where p.contratoId=:id and p.origemId=:evento", Long.class)
                .setParameter("id", n.id()).setParameter("evento", salvo.path("eventoId").asLong()).getSingleResult();
        assertThat(pendencias).isEqualTo(1);
        assertThat(quantidade(c, n, n.inicio().plusDays(6))).isEqualTo(15);
    }

    @Test
    void mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false); LocalDate d = n.inicio().plusDays(1);
        var a = aditamento("DESCRICAO-A", d); a.put("novaDescricao", "Descrição A");
        var original = registrarAditamento(c, n.id(), a);
        var independente = aditamento("EMPRESA-B", d); independente.put("novaRazaoSocialEmpresa", "Razão B");
        registrarAditamento(c, n.id(), independente);
        var colide = aditamento("DESCRICAO-C", d); colide.put("novaDescricao", "Descrição C"); previaAditamento(c, n.id(), colide);
        enviar(c.localA, base(n.id()) + "/aditamentos", colide, 409, "ADI-009/TMP-010 colisão exige precedência");
        colide.put("eventoPrecedenteId", original.path("eventoId").asLong()); colide.put("relacaoPrecedencia", "SUCEDE");
        registrarAditamento(c, n.id(), colide);
        var p = posicao(c, n.id(), d).path("versaoContratual");
        assertThat(p.path("descricao").asText()).isEqualTo("Descrição C");
        assertThat(p.path("razaoSocialEmpresa").asText()).isEqualTo("Razão B");
        assertThat(consultar(c.localA, base(n.id()) + "/eventos/precedencias", "TMP-016 relação explícita preservada").toString()).contains("SUCEDE");
    }

    @Test
    void eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, true);
        LocalDate d = n.inicio().plusDays(1);
        var rem = remanejamento(n, "REM-INATIVO", d.plusDays(1), 5); previaRemanejamento(c, n.id(), rem);
        enviar(c.localA, base(n.id()) + "/remanejamentos", rem, 201, "REM-006 programação antes do bloqueio");
        var a = aditamento("ADI-INATIVO", d.plusDays(2)); a.put("novaDescricao", "Vigente apesar do bloqueio operacional"); registrarAditamento(c, n.id(), a);
        confirmarCiclo(c, n.id(), "inativar", mapa("dataEfetiva", d, "motivo", "Bloqueio operacional explícito"));
        var p = posicao(c, n.id(), d.plusDays(3));
        assertThat(p.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        assertThat(p.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(p.path("versaoContratual").path("descricao").asText()).isEqualTo("Vigente apesar do bloqueio operacional");
        assertThat(quantidade(c, n, d.plusDays(3))).isEqualTo(20);
        enviar(c.localA, "/api/vinculos-contratuais", vinculo(n, d.plusDays(3), "52998224725"), 409,
                "CON-011/REM-006 história programada não libera operação inativa");
    }

    @Test
    void prorrogacaoNaoPodeCriarLacunaInternaNaVigencia() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var lacuna = aditamento("PRORROGA-GAP", n.fim().plusDays(3)); lacuna.put("novaDataInicio", n.fim().plusDays(3)); lacuna.put("novaDataFim", n.fim().plusDays(50));
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", lacuna, 400, "TMP-015 prorrogação do mesmo contrato não admite intervalo sem cobertura");
    }

    @Test
    void prorrogacaoRetroativaNaoReativaContratoInativo() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        confirmarCiclo(c, n.id(), "inativar", mapa("dataEfetiva", n.inicio().plusDays(1), "motivo", "Bloqueio administrativo"));
        var prorrogar = aditamento("PRORROGA-SEM-REATIVAR", n.inicio().plusDays(2)); prorrogar.put("novaDataFim", n.fim().plusDays(50));
        registrarAditamento(c, n.id(), prorrogar);
        var p = posicao(c, n.id(), n.fim().plusDays(1));
        assertThat(p.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(p.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
    }

    private void documentarAdmissaoOriginal(Cenario c) {
        // criarCenario monta a projeção por JPA, sem fotografar sua admissão.
        // Este cenário positivo de anulação fornece a origem nominal antes do
        // término retroativo, sem alterar as fixtures de ausência documental.
        var v = c.vinculoA;
        EventoVinculoContratual origem = new EventoVinculoContratual();
        origem.setVinculo(v); origem.setTipoEvento(TipoEventoVinculoContratual.CRIACAO);
        origem.setRelacaoPrecedencia(TipoRelacaoEventoVinculo.ORIGEM);
        origem.setDataEfetiva(INICIO); origem.setRegistradoEm(Instant.parse("2026-01-01T00:00:00Z"));
        origem.setInicioNovo(INICIO); origem.setMatriculaNova(v.getMatricula());
        origem.setPessoaNomeFotografia(v.getPessoa().getNome()); origem.setPessoaCpfFotografia(v.getPessoa().getCpf());
        origem.setContratoNumeroFotografia(v.getContrato().getNumeroContrato());
        origem.setContratoAnoFotografia(v.getContrato().getAnoContrato());
        origem.setGestaoLocalIdFotografia(c.gestaoA.getId()); origem.setGestaoLocalNomeFotografia(c.gestaoA.getNome());
        origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername());
        origem.setJustificativa("Fonte sintética explícita da admissão anterior à anulação");
        origem.setEvidencia("SEI-ORIGEM-VINCULO-ANULACAO"); origem.setAutorizacao("ATO-ORIGEM-VINCULO");
        origem.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal("FIXTURE-ANULACAO-CRIACAO|"
                + v.getId() + "|" + INICIO + "|" + v.getPessoa().getNome() + "|" + v.getPessoa().getCpf()));
        em.persist(origem); em.flush();
    }

    private void confirmarCiclo(Cenario c, long id, String op, Map<String,Object> req) throws Exception {
        var p = enviar(c.localA, base(id) + "/previas/" + op, req, 200, "prévia " + op);
        req.put("tokenPrevia", p.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/" + op, req, 200, "ação contratual " + op);
    }
    private Map<String,Object> vinculo(NovoContrato n, LocalDate inicio, String cpf) {
        return mapa("pessoa", mapa("nome", "Pessoa " + cpf, "cpf", cpf), "contratoId", n.id(), "dataInicio", inicio, "composicaoId", n.composicao1());
    }
    private JsonNode posicao(Cenario c, long id, LocalDate data) throws Exception {
        return consultar(c.localA, base(id) + "/posicao-temporal?data=" + data, "posição contratual em " + data);
    }
    private int quantidade(Cenario c, NovoContrato n, LocalDate data) throws Exception {
        for (var comp : consultar(c.localA, base(n.id()) + "/composicoes?data=" + data, "composição em " + data))
            if (comp.path("id").asLong() == n.composicao1()) return comp.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição não retornada");
    }
}
