package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.servico.AutorizacaoTemporalOcorrenciaServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * PER-010:004: concessão de leitura vinculada ao fato; filtros/serviços/JPA/PostgreSQL reais.
 * A ausência de origem usa somente a retirada autorizada da versão1 da fixture
 * sintética, preservando eventos e fotografias posteriores legítimos.
 * MockMvc não comprova TCP/JAR, navegador ou homologação integral.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcessaoContextualOcorrenciaHttpIT extends ContratosHttpBase {
    private static final String FINALIDADE = "Conferência contextual sintética PER010004";
    private static final String NARRATIVA = "Conteúdo pessoal sintético da ocorrência contextual";
    private static final byte[] ARQUIVO = "Prova sintética contextual não vazia.\n".getBytes(StandardCharsets.UTF_8);
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AutorizacaoTemporalOcorrenciaServico autorizacao;
    @Autowired private MotorTemporalContratoServico motorContrato;
    @Autowired private FilaRegistroAuditoria filaAuditoria;
    @Autowired private PropriedadesArmazenamentoArquivo armazenamento;
    private Path evidencias;

    @Test
    void concessaoLiberaSomenteOcorrenciaIndicadaEARevogacaoRecusaNovoUso() throws Exception {
        var c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONCESSAO-CONTEXTUAL-LEITURA");
        JsonNode alvo = criar(c, DATA, 2);
        JsonNode outra = criar(c, DATA.plusDays(5), 1);
        long id = alvo.path("id").asLong(), outraId = outra.path("id").asLong();
        long anexo = alvo.path("anexos").get(0).path("id").asLong();
        long versaoOcorrencia = versao(id);
        long quantidade = quantidadePermissoes();
        var respostaConcessao = mvc.perform(autenticar(post("/api/permissoes-excepcionais")
                .contentType(APPLICATION_JSON).content(json(pedido(c, id))), c.admin)).andReturn();
        if (respostaConcessao.getResolvedException() != null) {
            throw new AssertionError("Concessão contextual recusada HTTP " + respostaConcessao.getResponse().getStatus(),
                    respostaConcessao.getResolvedException());
        }
        assertThat(respostaConcessao.getResponse().getStatus()).isEqualTo(201);
        JsonNode concedida = dados(respostaConcessao);
        long permissaoId = concedida.path("id").asLong();
        assertThat(concedida.path("ocorrenciaId").asLong()).isEqualTo(id);
        assertThat(concedida.path("contratoId").isNull()).isTrue();
        assertThat(concedida.path("gestaoLocalId").isNull()).isTrue();
        assertThat(quantidadePermissoes()).isEqualTo(quantidade + 1);

        JsonNode integral = dados(executar(c.nacional, get("/api/ocorrencias/{id}", id)
                .param("finalidade", FINALIDADE), 200, "Leitor beneficiário acessa somente o alvo autorizado"));
        assertThat(integral.path("ocorrencia").path("observacao").asText()).isEqualTo(NARRATIVA);
        assertThat(executar(c.nacional, get("/api/ocorrencias/anexos/{id}", anexo)
                .param("finalidade", FINALIDADE), 200, "Anexo pertence ao alvo da concessão")
                .getResponse().getContentAsByteArray()).isEqualTo(ARQUIVO);
        executar(c.nacional, get("/api/ocorrencias/{id}", outraId).param("finalidade", FINALIDADE), 403,
                "Mesma Pessoa e contrato não ampliam concessão para outra ocorrência");
        executar(c.nacional, delete("/api/ocorrencias/{id}", id), 403,
                "Concessão somente leitura não permite cancelar o fato");
        assertThat(versao(id)).isEqualTo(versaoOcorrencia);
        JsonNode usos = consultar(c.admin, "/api/permissoes-excepcionais/" + permissaoId + "/usos",
                "Leitura e download têm recibos; recusa e tentativa de escrita não consomem");
        assertThat(usos).hasSize(2);
        for (JsonNode uso : usos) assertThat(uso.path("finalidade").asText()).isEqualTo(FINALIDADE);

        enviar(c.admin, "/api/permissoes-excepcionais/" + permissaoId + "/revogar",
                mapa("motivo", "Fim da necessidade específica sintética"), 200, "Revogação da concessão contextual");
        executar(c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE), 403,
                "Validade passada de concessão não autoriza novo uso após revogação");
        assertThat(consultar(c.admin, "/api/permissoes-excepcionais/" + permissaoId + "/usos",
                "Recusas posteriores não criam consumo")).isEqualTo(usos);
        assertThat(versao(id)).isEqualTo(versaoOcorrencia);
    }

    @Test
    void gestorLocalNaoPodeConcederMesmoComAutoridadeSobreTodosOsDias() throws Exception {
        var c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONCESSAO-EXIGE-ADMINISTRADOR");
        long id = criar(c, DATA, 2).path("id").asLong();
        long antes = quantidadePermissoes();
        enviar(c.localA, "/api/permissoes-excepcionais", pedido(c, id), 403,
                "Somente Administrador atual concede, sem herança de poder do Gestor");
        assertThat(quantidadePermissoes()).isEqualTo(antes);
    }

    @Test
    void umDiaSemOrigemNaoPodeSerSupridoPelaFonteLegitimaDoDiaSeguinte() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-ORIGEM-PARCIAL");
        LocalDate semOrigem = DATA, comprovado = DATA.plusDays(1);
        long alvo = criar(c, semOrigem, 2).path("id").asLong();
        JsonNode foto = foto(alvo);
        assertThat(prova(alvo)).hasSize(2);
        transferir(c, comprovado);
        // Dois atos no mesmo dia têm SUCEDE explícito. D fica antes de ambos;
        // D+1 tem sua própria habilitação ATIVO, sem um dia intermediário inválido.
        long inativacao = ciclo(c, "inativar", comprovado, null);
        ciclo(c, "reativar", comprovado, inativacao);
        // D+1 já possui a frequência fotografada antes da transferência no alvo.
        // O controle usa o próximo dia comprovado, sem tentar reassociar aquela fonte.
        LocalDate diaControle = comprovado.plusDays(1);
        long controle = criarComo(c, c.localB, diaControle, 1).path("id").asLong();
        assertThat(prova(alvo)).hasSize(2);
        JsonNode versoesPosteriores = versoes(c, false);
        assertThat(versoesPosteriores.isEmpty()).isFalse();
        JsonNode antesDaRetirada = estado(c);
        emTransacao(() -> {
            // Única retirada autorizada: origem da fixture SINTÉTICA de acervo
            // incompleto, por HQL de entidade; nenhum evento/foto é adulterado.
            assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c and v.numeroVersao=1")
                    .setParameter("c", c.contratoA.getId()).executeUpdate()).isEqualTo(1);
            return null;
        });
        assertThat(versoes(c, true)).isEqualTo(versoesPosteriores);
        JsonNode depoisDaRetirada = estado(c);
        for (String chave : List.of("ocorrencias", "contextos", "anexos", "frequencias", "eventosFrequencia", "historicos", "arquivos"))
            assertThat(depoisDaRetirada.path(chave)).as("Retirada da origem não altera o fato ou suas fotografias: " + chave)
                    .isEqualTo(antesDaRetirada.path(chave));
        emTransacao(() -> {
            em.clear();
            Contrato contrato = em.find(Contrato.class, c.contratoA.getId());
            var ausente = motorContrato.reconstruirPosicao(contrato, semOrigem, Instant.now());
            var posterior = motorContrato.reconstruirPosicao(contrato, comprovado, Instant.now());
            assertThat(motorContrato.fotografiaOriginalConhecida(contrato, Instant.now())).isEmpty();
            assertThat(motorContrato.versaoAplicavel(contrato, semOrigem, Instant.now())).isEmpty();
            assertThat(ausente.versaoContratual()).isNull();
            assertThat(ausente.vigenciaInicio()).isNull();
            assertThat(posterior.situacaoTemporal().name()).isEqualTo("VIGENTE");
            assertThat(posterior.habilitacaoAdministrativa().name()).isEqualTo("ATIVO");
            return null;
        });
        assertThat(prova(alvo)).isEmpty();
        JsonNode fonteControle = prova(controle);
        assertThat(fonteControle).hasSize(1);
        assertThat(fonteControle.get(0).path("data").asText()).isEqualTo(diaControle.toString());
        assertThat(fonteControle.get(0).path("gestaoId").asLong()).isEqualTo(c.gestaoB.getId());
        negarSemMutacao(c, c.admin, post("/api/permissoes-excepcionais")
                .contentType(APPLICATION_JSON).content(json(pedido(c, alvo))), 403, "origem-parcial");
        assertThat(foto(alvo)).isEqualTo(foto);
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, controle), 201,
                "Controle positivo: mesma origem retirada, mas dia posterior com fonte própria íntegra").path("id").asLong();
        conferirConcessao(c, permissao, controle, c.nacional.getId(), c.admin.getId());
        JsonNode integral = dados(executar(c.nacional, get("/api/ocorrencias/{id}", controle).param("finalidade", FINALIDADE),
                200, "Fonte posterior autoriza somente o controle comprovado"));
        assertThat(integral.path("ocorrencia").path("observacao").asText()).isEqualTo(NARRATIVA);
        assertThat(usos(permissao)).hasSize(1);
        assertThat(foto(alvo)).isEqualTo(foto);
        registrar(c, "origem-parcial-controle", mapa("alvo", alvo, "controle", controle,
                "dataSemOrigem", semOrigem, "dataComprovada", comprovado, "dataControle", diaControle, "provaControle", fonteControle,
                "versoesPosterioresPreservadas", versoesPosteriores, "estado", estado(c)));
    }

    @Test
    void concessaoMultidiasEntreEstadosExigeEscopoIntegralDoBeneficiario() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-ESTADOS-A-B");
        JsonNode alvo = criar(c, DATA, 2);
        long id = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        JsonNode original = foto(id);
        transferir(c, DATA.plusDays(1));
        JsonNode fonte = prova(id);
        assertThat(fonte).hasSize(2);
        assertThat(fonte.get(0).path("data").asText()).isEqualTo(DATA.toString());
        assertThat(fonte.get(1).path("data").asText()).isEqualTo(DATA.plusDays(1).toString());
        assertThat(fonte.get(0).path("gestaoId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(fonte.get(1).path("gestaoId").asLong()).isEqualTo(c.gestaoB.getId());
        assertThat(fonte.get(0).path("estadoId").asLong()).isEqualTo(c.seA.getId());
        assertThat(fonte.get(1).path("estadoId").asLong()).isEqualTo(c.seB.getId());
        assertThat(c.seA.getId()).isNotEqualTo(c.seB.getId());
        assertThat(foto(id)).isEqualTo(original);
        for (UsuarioAplicacao estadual : List.of(c.estadualA, c.estadualB)) {
            Map<String, Object> req = pedido(c, id); req.put("usuarioBeneficiarioId", estadual.getId());
            negarSemMutacao(c, c.admin, post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                    .content(json(req)), 403, "estadual-sem-todos-os-dias-" + estadual.getId());
        }
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, id), 201,
                "Nacional possui autoridade atual sobre os dois estados históricos").path("id").asLong();
        conferirConcessao(c, permissao, id, c.nacional.getId(), c.admin.getId());
        JsonNode integral = dados(executar(c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE),
                200, "Narrativa indivisível só é liberada com prova dos dois dias"));
        assertThat(integral.path("ocorrencia").path("observacao").asText()).isEqualTo(NARRATIVA);
        assertThat(usos(permissao)).hasSize(1); // Uma leitura usa uma concessão do fato inteiro, sem duplicar por Gestão.
        assertThat(executar(c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                200, "Mesmo alvo contextual abrange o anexo próprio").getResponse().getContentAsByteArray()).isEqualTo(ARQUIVO);
        JsonNode consumos = usos(permissao);
        assertThat(consumos).hasSize(2);
        assertThat(contarRecurso(consumos, "Ocorrencia", id)).isEqualTo(1);
        assertThat(contarRecurso(consumos, "AnexoOcorrencia", anexo)).isEqualTo(1);
        for (JsonNode consumo : consumos) assertThat(consumo.path("finalidade").asText()).isEqualTo(FINALIDADE);
        assertThat(foto(id)).isEqualTo(original);
        registrar(c, "dois-estados", mapa("provaDosDoisDias", fonte, "fotografiaOriginal", original,
                "concessao", permissao, "consumos", consumos, "estado", estado(c)));
    }

    @ParameterizedTest(name = "pedido contextual inválido: {0}")
    @ValueSource(strings = {"BENEFICIARIO_INATIVO", "OPERACIONAL", "GESTOR_LOCAL", "ADMINISTRADOR",
            "ESTADUAL_ALHEIO", "BENEFICIARIO_INEXISTENTE", "ALVO_INEXISTENTE", "TIPO_ESCRITA",
            "MISTURA_CONTRATO", "MISTURA_GESTAO", "VALIDADE_VENCIDA", "SEM_FINALIDADE"})
    void concessaoIncompativelNaoPersistePermissaoUsoOuAuditoriaDeSucesso(String caso) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-INVALIDO-" + caso);
        long id = criar(c, DATA, 2).path("id").asLong();
        Map<String, Object> req = pedido(c, id);
        int esperado = 400;
        switch (caso) {
            case "BENEFICIARIO_INATIVO" -> {
                executar(c.admin, put("/api/usuarios/" + c.nacional.getId()).contentType(APPLICATION_JSON)
                        .content(json(mapa("active", false))), 200, "Desativação legítima prévia do beneficiário");
                assertUsuario(c.nacional.getId(), false, PerfilAcesso.GESTOR_NACIONAL);
                esperado = 409;
            }
            case "OPERACIONAL" -> req.put("usuarioBeneficiarioId", c.operacionalA.getId());
            case "GESTOR_LOCAL" -> req.put("usuarioBeneficiarioId", c.localA.getId());
            case "ADMINISTRADOR" -> req.put("usuarioBeneficiarioId", c.admin.getId());
            case "ESTADUAL_ALHEIO" -> { req.put("usuarioBeneficiarioId", c.estadualB.getId()); esperado = 403; }
            case "BENEFICIARIO_INEXISTENTE" -> { req.put("usuarioBeneficiarioId", Long.MAX_VALUE); esperado = 404; }
            case "ALVO_INEXISTENTE" -> { req.put("ocorrenciaId", Long.MAX_VALUE); esperado = 404; }
            case "TIPO_ESCRITA" -> req.put("tipo", "TRANSFERENCIA_GESTAO_LOCAL");
            case "MISTURA_CONTRATO" -> req.put("contratoId", c.contratoA.getId());
            case "MISTURA_GESTAO" -> req.put("gestaoLocalId", c.gestaoA.getId());
            case "VALIDADE_VENCIDA" -> req.put("validoAte", Instant.now().minusSeconds(60));
            case "SEM_FINALIDADE" -> req.put("finalidade", "");
            default -> throw new IllegalArgumentException(caso);
        }
        assertThat(prova(id)).hasSize(2);
        negarSemMutacao(c, c.admin, post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                .content(json(req)), esperado, "pedido-incompativel-" + caso);
    }

    @Test
    void duplicidadeDoMesmoAlvoNaoImpedeConcessaoIndependenteParaOutraOcorrencia() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-DUPLICIDADE");
        long alvo = criar(c, DATA, 2).path("id").asLong();
        long outro = criar(c, DATA.plusDays(5), 1).path("id").asLong();
        JsonNode fontes = estado(c);
        long primeira = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, alvo), 201,
                "Primeira concessão para o alvo identificado").path("id").asLong();
        conferirConcessao(c, primeira, alvo, c.nacional.getId(), c.admin.getId());
        negarSemMutacao(c, c.admin, post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                .content(json(pedido(c, alvo))), 409, "duplicada-mesmo-alvo");
        long segunda = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, outro), 201,
                "Mesmo tipo/beneficiário com outra ocorrência constitui outro escopo").path("id").asLong();
        assertThat(segunda).isNotEqualTo(primeira);
        conferirConcessao(c, segunda, outro, c.nacional.getId(), c.admin.getId());
        assertThat(usos(primeira)).isEmpty(); assertThat(usos(segunda)).isEmpty();
        JsonNode depois = estado(c);
        assertThat(depois.path("permissoes")).hasSize(2);
        assertThat(auditorias(depois, "CONCEDER_PERMISSAO_EXCEPCIONAL")).isEqualTo(2);
        for (String chave : List.of("ocorrencias", "contextos", "anexos", "frequencias", "eventosFrequencia", "historicos", "arquivos"))
            assertThat(depois.path(chave)).isEqualTo(fontes.path(chave));
        registrar(c, "dois-alvos-independentes", mapa("antes", fontes, "depois", depois));
    }

    @ParameterizedTest(name = "finalidade divergente: {0}")
    @ValueSource(strings = {"DETALHE", "ANEXO"})
    void finalidadeDivergenteRecusaConteudoSemConsumoNemAuditoriaDeUso(String consumidor) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-FINALIDADE-" + consumidor);
        JsonNode alvo = criar(c, DATA, 2);
        long id = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, id), 201,
                "Finalidade aprovada é parte da concessão contextual").path("id").asLong();
        conferirConcessao(c, permissao, id, c.nacional.getId(), c.admin.getId());
        String rota = consumidor.equals("DETALHE") ? "/api/ocorrencias/" + id : "/api/ocorrencias/anexos/" + anexo;
        executar(c.nacional, get(rota).param("finalidade", FINALIDADE), 200,
                "Controle positivo do mesmo consumidor com finalidade aprovada");
        assertThat(usos(permissao)).hasSize(1);
        negarSemMutacao(c, c.nacional, get(rota).param("finalidade", "Finalidade diferente não concedida"),
                403, "finalidade-divergente-" + consumidor);
        assertThat(usos(permissao)).hasSize(1);
    }

    @Test
    void concessaoContextualNaoAbreFrequenciaOutraOcorrenciaOuPoderDeEscrita() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-SEM-AMPLIACAO");
        JsonNode alvo = criar(c, DATA, 2), outra = criar(c, DATA.plusDays(5), 1);
        long id = alvo.path("id").asLong(), outroId = outra.path("id").asLong();
        long outroAnexo = outra.path("anexos").get(0).path("id").asLong();
        String segredoFrequencia = "Narrativa pessoal sintética da frequência sem concessão " + UUID.randomUUID();
        long frequencia = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA.plusDays(7), "status", "PRESENCA", "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                "observacao", segredoFrequencia, "justificativaRetificacao", "Fonte sintética independente da ocorrência",
                "chaveIdempotencia", "contextual-frequencia-" + UUID.randomUUID()), 200,
                "Frequência própria com conteúdo pessoal fora do período das ocorrências").path("id").asLong();
        assertThat(consultar(c.localA, "/api/frequencias/" + frequencia + "/eventos",
                "Controle local prova conteúdo pessoal existente na fonte").toString()).contains(segredoFrequencia);
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, id), 201,
                "Única concessão do Nacional identifica a ocorrência, sem escopo genérico").path("id").asLong();
        conferirConcessao(c, permissao, id, c.nacional.getId(), c.admin.getId());
        JsonNode antes = estado(c);
        for (String rota : List.of("/api/frequencias/" + frequencia + "/eventos",
                "/api/frequencias/" + frequencia + "/evidencias-integridade",
                "/api/frequencias/intervalo?inicio=" + DATA.plusDays(7) + "&fim=" + DATA.plusDays(7)
                        + "&contratoId=" + c.contratoA.getId())) {
            JsonNode minimizada = consultar(c.nacional, rota, "Concessão contextual não desminimiza outro recurso pessoal");
            assertThat(minimizada.toString()).doesNotContain(segredoFrequencia).doesNotContain(NARRATIVA);
        }
        assertThat(estado(c)).isEqualTo(antes);
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/{id}", outroId).param("finalidade", FINALIDADE),
                403, "outra-ocorrencia");
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/anexos/{id}", outroAnexo).param("finalidade", FINALIDADE),
                403, "outro-anexo");
        negarSemMutacao(c, c.nacional, delete("/api/ocorrencias/{id}", id), 403, "cancelamento-nao-concedido");
        negarSemMutacao(c, c.nacional, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "outro-fato.txt", "text/plain", ARQUIVO))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", DATA.plusDays(10).toString()).param("dias", "1").param("observacao", NARRATIVA)
                .param("chaveIdempotencia", "contextual-escrita-negada-" + UUID.randomUUID()),
                403, "criacao-nao-concedida");
        assertThat(usos(permissao)).isEmpty();
    }

    @Test
    void revogacaoConfirmadaRecusaDetalheEArquivoPreservandoUsosAnterioresEFato() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-REVOGACAO-PERSISTIDA");
        JsonNode alvo = criar(c, DATA, 2);
        long id = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        JsonNode foto = foto(id);
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, id), 201,
                "Concessão contextual válida antes da revogação").path("id").asLong();
        executar(c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE), 200,
                "Consumo válido anterior fica auditável");
        JsonNode usosAntes = usos(permissao); assertThat(usosAntes).hasSize(1);
        enviar(c.admin, "/api/permissoes-excepcionais/" + permissao + "/revogar",
                mapa("motivo", "Necessidade específica encerrada por ato sintético"), 200, "Revogação confirmada por HTTP");
        emTransacao(() -> {
            em.clear(); PermissaoExcepcional p = em.find(PermissaoExcepcional.class, permissao);
            assertThat(p.getAtivo()).isFalse(); assertThat(p.getRevogadoEm()).isNotNull();
            assertThat(p.getRevogadoPorId()).isEqualTo(c.admin.getId()); return null;
        });
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE), 403, "revogada-detalhe");
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE), 403, "revogada-anexo");
        assertThat(usos(permissao)).isEqualTo(usosAntes); assertThat(foto(id)).isEqualTo(foto);
        assertThat(auditorias(estado(c), "REVOGAR_PERMISSAO_EXCEPCIONAL")).isEqualTo(1);
    }

    @ParameterizedTest(name = "leitura contextual institucional: {0}")
    @ValueSource(strings = {"ESTADUAL", "PRESIDENTE"})
    void perfilInstitucionalAutorizadoLeODetalheEOAnexoSemGanharEscrita(String perfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-POSITIVO-" + perfil);
        UsuarioAplicacao leitor = perfil.equals("ESTADUAL") ? c.estadualA : c.presidente;
        JsonNode alvo = criar(c, DATA, 2);
        long id = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        Map<String, Object> req = pedido(c, id); req.put("usuarioBeneficiarioId", leitor.getId());
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", req, 201,
                "Perfil institucional autorizado sobre todos os dias recebe somente leitura do fato").path("id").asLong();
        conferirConcessao(c, permissao, id, leitor.getId(), c.admin.getId());
        JsonNode integral = dados(executar(leitor, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE),
                200, "Concessão permite narrativa do alvo no escopo institucional legítimo"));
        assertThat(integral.path("ocorrencia").path("observacao").asText()).isEqualTo(NARRATIVA);
        assertThat(executar(leitor, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", FINALIDADE),
                200, "Concessão permite bytes do anexo próprio").getResponse().getContentAsByteArray()).isEqualTo(ARQUIVO);
        assertThat(usos(permissao)).hasSize(2);
        negarSemMutacao(c, leitor, delete("/api/ocorrencias/{id}", id), 403, "positivo-sem-escrita-" + perfil);
        registrar(c, "positivo-institucional-" + perfil, mapa("perfil", perfil, "alvo", id,
                "concessao", permissao, "usos", usos(permissao)));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void validadeExpiradaRecusaNovoUsoSemDependerDeRevogacaoOuScheduler() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-EXPIRACAO-REAL");
        long id = criar(c, DATA, 2).path("id").asLong();
        Map<String, Object> req = pedido(c, id);
        Instant validade = Instant.now().plusSeconds(8).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        req.put("validoAte", validade);
        long permissao = enviar(c.admin, "/api/permissoes-excepcionais", req, 201,
                "Validade curta real, sem alterar fonte persistida ou relógio da aplicação").path("id").asLong();
        executar(c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE), 200,
                "Controle positivo antes de vencer a mesma concessão");
        assertThat(usos(permissao)).hasSize(1);
        // Espera somente a validade real; não é prova de simultaneidade ou virada civil.
        long prazo = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (Instant.now().isBefore(validade) && System.nanoTime() < prazo) Thread.sleep(20);
        assertThat(Instant.now()).isAfterOrEqualTo(validade);
        assertUsuario(c.nacional.getId(), true, PerfilAcesso.GESTOR_NACIONAL);
        emTransacao(() -> {
            em.clear(); PermissaoExcepcional p = em.find(PermissaoExcepcional.class, permissao);
            assertThat(p.getAtivo()).isTrue(); assertThat(p.getRevogadoEm()).isNull();
            assertThat(p.getValidoAte()).isBeforeOrEqualTo(Instant.now()); return null;
        });
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/{id}", id).param("finalidade", FINALIDADE),
                403, "expiracao-real-sem-revogacao");
        assertThat(usos(permissao)).hasSize(1);
    }

    @Test
    void alvoContextualTemPrioridadeEFallbackGenericoContinuaLimitadoASuaConcessao() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-COEXISTENCIA-GENERICA");
        long alvo = criar(c, DATA, 2).path("id").asLong();
        long outro = criar(c, DATA.plusDays(5), 1).path("id").asLong();
        long contextual = enviar(c.admin, "/api/permissoes-excepcionais", pedido(c, alvo), 201,
                "Concessão específica anterior cobre o fato inteiro").path("id").asLong();
        long generica = enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                "contratoId", c.contratoA.getId(), "finalidade", "Conferência institucional genérica sintética",
                "justificativa", "Controle independente de compatibilidade do escopo contratual",
                "autorizacaoAdministrativa", "ATO-SINTETICO-GENERICO", "validoAte", Instant.now().plusSeconds(1800)),
                201, "Concessão genérica mais nova não transforma nem substitui a específica").path("id").asLong();
        executar(c.nacional, get("/api/ocorrencias/{id}", alvo).param("finalidade", FINALIDADE), 200,
                "Preferir o alvo contextual, mesmo com grant genérico mais novo");
        assertThat(usos(contextual)).hasSize(1); assertThat(usos(generica)).isEmpty();
        executar(c.nacional, get("/api/ocorrencias/{id}", outro).param("finalidade", "Conferência institucional genérica sintética"),
                200, "Outro alvo pode usar exclusivamente sua autorização genérica legítima");
        assertThat(usos(contextual)).hasSize(1); assertThat(usos(generica)).hasSize(1);
        enviar(c.admin, "/api/permissoes-excepcionais/" + generica + "/revogar",
                mapa("motivo", "Encerramento apenas do escopo genérico de controle"), 200, "Revogar fallback de forma independente");
        negarSemMutacao(c, c.nacional, get("/api/ocorrencias/{id}", outro).param("finalidade", FINALIDADE),
                403, "sem-fallback-generico");
        executar(c.nacional, get("/api/ocorrencias/{id}", alvo).param("finalidade", FINALIDADE), 200,
                "Concessão específica continua íntegra depois de revogar o fallback");
        assertThat(usos(contextual)).hasSize(2); assertThat(usos(generica)).hasSize(1);
        registrar(c, "coexistencia-generica", mapa("contextual", contextual, "generica", generica,
                "usosContextuais", usos(contextual), "usosGenericos", usos(generica)));
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void duasConcessoesSimultaneasDoMesmoAlvoConfirmamUmaFonteEUmaAuditoria() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-DUPLICIDADE-CONCORRENTE");
        long alvo = criar(c, DATA, 2).path("id").asLong();
        var primeiroPedido = autenticar(post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                .content(json(pedido(c, alvo))), c.admin);
        var segundoPedido = autenticar(post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                .content(json(pedido(c, alvo))), c.admin);
        JsonNode antes = estado(c);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> pid = new CompletableFuture<>();
        Ator a = new Ator("contextual-mesmo-alvo-a"), b = new Ator("contextual-mesmo-alvo-b");
        try {
            Future<?> bloqueador = reterContrato(c, pool, liberar, pid);
            long raiz = pid.get(10, TimeUnit.SECONDS);
            Future<MvcResult> primeira = pool.submit(() -> a.executar(() -> mvc.perform(primeiroPedido).andReturn()));
            Future<MvcResult> segunda = pool.submit(() -> b.executar(() -> mvc.perform(segundoPedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardarAmbos(jdbc, raiz, a,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()), b,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()), "Contrato/" + c.contratoA.getId(), primeira, segunda);
            assertThat(estado(c)).as("Ambos aguardam antes de persistir concessão").isEqualTo(antes);
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult r1 = primeira.get(15, TimeUnit.SECONDS), r2 = segunda.get(15, TimeUnit.SECONDS);
            List<Integer> status = List.of(r1.getResponse().getStatus(), r2.getResponse().getStatus()).stream().sorted().toList();
            JsonNode depois = estado(c);
            registrar(c, "duas-concessoes-disputa-fisica", mapa("raiz", raiz, "espera", espera,
                    "status", status, "antes", antes, "depois", depois));
            assertThat(status).containsExactly(201, 409);
            long permissao = dados(r1.getResponse().getStatus() == 201 ? r1 : r2).path("id").asLong();
            conferirConcessao(c, permissao, alvo, c.nacional.getId(), c.admin.getId());
            assertThat(depois.path("permissoes")).hasSize(1); assertThat(depois.path("usos")).isEmpty();
            assertThat(auditorias(depois, "CONCEDER_PERMISSAO_EXCEPCIONAL")).isEqualTo(1);
            assertThat(depois.path("ocorrencias")).isEqualTo(antes.path("ocorrencias"));
            assertThat(depois.path("contextos")).isEqualTo(antes.path("contextos"));
            assertThat(depois.path("anexos")).isEqualTo(antes.path("anexos"));
        } finally {
            liberar.countDown(); pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest(name = "revogação HTTP durante espera: {0}")
    @ValueSource(strings = {"ADMINISTRADOR", "BENEFICIARIO"})
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void concessaoRevalidaAdministradorEBeneficiarioDepoisDaEsperaFisica(String revogado) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "CONTEXTUAL-REVOGACAO-EM-ESPERA-" + revogado);
        UsuarioAplicacao ator = emTransacao(() -> { UsuarioAplicacao u = usuario(PerfilAcesso.ADMINISTRADOR, null); em.flush(); return u; });
        long alvo = criar(c, DATA, 2).path("id").asLong();
        long controle = criar(c, DATA.plusDays(5), 1).path("id").asLong();
        long permissaoControle = enviar(ator, "/api/permissoes-excepcionais", pedido(c, controle), 201,
                "Controle positivo: mesmo Administrador e beneficiário antes do interleaving").path("id").asLong();
        conferirConcessao(c, permissaoControle, controle, c.nacional.getId(), ator.getId());
        enviar(ator, "/api/permissoes-excepcionais/" + permissaoControle + "/revogar",
                mapa("motivo", "Controle concluído antes da concorrência"), 200, "Controle não ocupa o alvo da tentativa");
        var pedido = autenticar(post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                .content(json(pedido(c, alvo))), ator);
        JsonNode antes = estado(c, ator.getId());
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> pid = new CompletableFuture<>();
        Ator atorJdbc = new Ator("contextual-revalidacao-" + revogado);
        try {
            Future<?> bloqueador = reterContrato(c, pool, liberar, pid);
            long raiz = pid.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> atorJdbc.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(jdbc, raiz, atorJdbc,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()), "Contrato/" + c.contratoA.getId(), tentativa);
            long usuario = revogado.equals("ADMINISTRADOR") ? ator.getId() : c.nacional.getId();
            Map<String, Object> alteracao = revogado.equals("ADMINISTRADOR") ? mapa("perfil", "GESTOR_NACIONAL") : mapa("active", false);
            Future<?> revogacao = pool.submit(() -> {
                executar(c.admin, put("/api/usuarios/" + usuario).contentType(APPLICATION_JSON).content(json(alteracao)),
                        200, "Outro Administrador confirma revogação enquanto a concessão aguarda o Contrato");
                return null;
            });
            revogacao.get(12, TimeUnit.SECONDS);
            assertUsuario(usuario, revogado.equals("ADMINISTRADOR"), PerfilAcesso.GESTOR_NACIONAL);
            assertThat(tentativa.isDone()).as("A concessão ainda espera após a revogação commitada").isFalse();
            assertThat(estado(c, ator.getId())).isEqualTo(antes);
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(c, ator.getId());
            registrar(c, "revalidacao-" + revogado, mapa("ator", ator.getId(), "beneficiario", c.nacional.getId(),
                    "revogador", c.admin.getId(), "raiz", raiz, "espera", espera, "revogacaoCommitadaAntesDeLiberar", true,
                    "status", resposta.getResponse().getStatus(), "antes", antes, "depois", depois));
            assertAll(
                    () -> assertThat(resposta.getResponse().getStatus()).isEqualTo(revogado.equals("ADMINISTRADOR") ? 403 : 409),
                    () -> assertThat(depois).as("Nenhuma concessão, consumo ou auditoria de sucesso da tentativa revogada").isEqualTo(antes));
        } finally {
            liberar.countDown(); pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private JsonNode criar(Cenario c, LocalDate inicio, int dias) throws Exception {
        return dados(executar(c.localA, multipart("/api/ocorrencias")
                        .file(new MockMultipartFile("anexos", "prova-contextual.txt", "text/plain", ARQUIVO))
                        .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                        .param("dataOcorrencia", inicio.toString()).param("dias", Integer.toString(dias))
                        .param("observacao", NARRATIVA).param("chaveIdempotencia", "contextual-" + UUID.randomUUID()),
                201, "Fonte canônica sintética explícita, com anexo e período íntegros"));
    }

    private Map<String, Object> pedido(Cenario c, long ocorrenciaId) {
        return mapa("tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                "ocorrenciaId", ocorrenciaId, "finalidade", FINALIDADE,
                "justificativa", "Necessidade sintética limitada ao fato identificado",
                "autorizacaoAdministrativa", "ATO-SINTETICO-CONTEXTUAL", "validoAte", Instant.now().plusSeconds(1800));
    }

    private long quantidadePermissoes() {
        return emTransacao(() -> em.createQuery("select count(p) from PermissaoExcepcional p", Long.class).getSingleResult());
    }

    private long versao(long ocorrenciaId) {
        return emTransacao(() -> em.createQuery("select o.version from Ocorrencia o where o.id=:id", Long.class)
                .setParameter("id", ocorrenciaId).getSingleResult());
    }

    private JsonNode criarComo(Cenario c, UsuarioAplicacao ator, LocalDate inicio, int dias) throws Exception {
        return dados(executar(ator, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova-contextual.txt", "text/plain", ARQUIVO))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", inicio.toString()).param("dias", Integer.toString(dias))
                .param("observacao", NARRATIVA).param("classificacoesAnexos", "PESSOAL_RESTRITO")
                .param("chaveIdempotencia", "contextual-controle-" + UUID.randomUUID()), 201,
                "Controle sintético com período integral e autoridade histórica do criador"));
    }

    private void transferir(Cenario c, LocalDate dia) throws Exception {
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1",
                "dataEfetiva", dia, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência sintética contextual PER010004 A para B",
                "referenciaSei", "SEI-SINTETICO-CONTEXTUAL", "autorizacaoAdministrativa", "ATO-SINTETICO-CONTEXTUAL",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "Mudança histórica legítima entre dois estados pela API");
    }

    private long ciclo(Cenario c, String operacao, LocalDate data, Long precedente) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "motivo", "Ato sintético posterior de habilitação " + operacao);
        if (precedente != null) {
            req.put("eventoPrecedenteId", precedente); req.put("relacaoPrecedencia", "SUCEDE"); req.put("ordemDocumental", 2);
        }
        JsonNode previa = enviar(c.localB, base(c.contratoA.getId()) + "/previas/" + operacao, req, 200,
                "Prévia atual da decisão administrativa posterior");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localB, base(c.contratoA.getId()) + "/" + operacao, req, 200,
                "Novo ato legítimo; reativação na mesma data sucede explicitamente a inativação");
        return emTransacao(() -> em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c "
                        + "and e.dataEfetiva=:d and e.tipoEvento=:t order by e.id desc", Long.class)
                .setParameter("c", c.contratoA.getId()).setParameter("d", data)
                .setParameter("t", operacao.equals("inativar") ? TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                        : TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA).setMaxResults(1).getSingleResult());
    }

    private JsonNode prova(long id) {
        return emTransacao(() -> { em.clear(); return mapper.valueToTree(autorizacao.resolver(em.find(Ocorrencia.class, id))); });
    }

    private JsonNode versoes(Cenario c, boolean incluirOrigem) {
        return mapper.valueToTree(jdbc.queryForList("select * from contrato_versao_negocial where id_contrato=?"
                + (incluirOrigem ? "" : " and numero_versao<>1") + " order by id_contrato_versao", c.contratoA.getId()));
    }

    private JsonNode foto(long id) {
        var leitura = new TransactionTemplate(transactionManager); leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return leitura.execute(status -> mapper.valueToTree(mapa(
                "ocorrencia", jdbc.queryForList("select * from ocorrencia where id=?", id),
                "contextos", jdbc.queryForList("select * from contexto_ocorrencia where ocorrencia_id=? order by id", id),
                "anexos", jdbc.queryForList("select * from anexo_ocorrencia where id_ocorrencia=? order by id", id),
                "historico", jdbc.queryForList("select * from historico_empregado where id_ocorrencia=? order by id_historico", id))));
    }

    private JsonNode usos(long id) {
        descarregarAuditoria();
        return emTransacao(() -> mapper.valueToTree(jdbc.queryForList(
                "select id_permissao_uso as id,id_permissao_excepcional as permissao_id,tipo_recurso,"
                        + "id_recurso,finalidade from permissao_excepcional_uso where id_permissao_excepcional=? order by id_permissao_uso", id)));
    }

    private long contarRecurso(JsonNode usos, String tipo, long id) {
        long total = 0;
        for (JsonNode uso : usos)
            if (uso.path("tipo_recurso").asText().equals(tipo) && uso.path("id_recurso").asLong() == id) total++;
        return total;
    }

    private void conferirConcessao(Cenario c, long permissao, long ocorrencia, long beneficiario, long administrador) {
        emTransacao(() -> {
            em.clear(); PermissaoExcepcional p = em.find(PermissaoExcepcional.class, permissao);
            assertThat(p).isNotNull(); assertThat(p.getOcorrencia().getId()).isEqualTo(ocorrencia);
            assertThat(p.getContrato()).isNull(); assertThat(p.getGestaoLocal()).isNull();
            assertThat(p.getUsuarioBeneficiario().getId()).isEqualTo(beneficiario);
            assertThat(p.getConcedidoPorId()).isEqualTo(administrador);
            assertThat(p.getConcedidoEm()).isNotNull(); assertThat(p.getValidoAte()).isAfter(Instant.now());
            assertThat(p.getFinalidade()).isEqualTo(FINALIDADE); assertThat(p.getAtivo()).isTrue();
            assertThat(p.getRevogadoEm()).isNull(); return null;
        });
        descarregarAuditoria();
        assertThat(jdbc.queryForObject("select count(*) from registro_auditoria where action='CONCEDER_PERMISSAO_EXCEPCIONAL' "
                + "and entity_type='PermissaoExcepcional' and entity_id=? and user_id=?", Long.class, permissao, administrador)).isEqualTo(1L);
        assertThat(usos(permissao)).isEmpty();
    }

    private void assertUsuario(long id, boolean ativo, PerfilAcesso perfil) {
        emTransacao(() -> {
            em.clear(); UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, id);
            assertThat(atual.getActive()).isEqualTo(ativo); assertThat(atual.getPerfil()).isEqualTo(perfil); return null;
        });
    }

    private void negarSemMutacao(Cenario c, UsuarioAplicacao ator, AbstractMockHttpServletRequestBuilder<?> pedido,
                                 int status, String fase) throws Exception {
        JsonNode antes = estado(c);
        MvcResult r = mvc.perform(autenticar(pedido, ator)).andReturn();
        JsonNode depois = estado(c);
        registrar(c, fase, mapa("status", r.getResponse().getStatus(),
                "resolvedExceptionType", r.getResolvedException() == null ? null : r.getResolvedException().getClass().getName(),
                "antes", antes, "depois", depois));
        assertAll(
                () -> assertThat(r.getResponse().getStatus()).isEqualTo(status),
                () -> assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain(NARRATIVA)
                        .doesNotContain(new String(ARQUIVO, StandardCharsets.UTF_8)),
                () -> assertThat(depois).as("Recusa não deixa permissão, consumo, auditoria de sucesso ou efeito no fato: " + fase).isEqualTo(antes));
    }

    private JsonNode estado(Cenario c, Long... adicionais) throws Exception {
        descarregarAuditoria();
        Set<Long> atores = new TreeSet<>(List.of(c.admin.getId(), c.nacional.getId(), c.presidente.getId(),
                c.localA.getId(), c.localB.getId(), c.estadualA.getId(), c.estadualB.getId(),
                c.operacionalA.getId(), c.operacionalB.getId()));
        atores.addAll(Arrays.asList(adicionais));
        String marcadores = String.join(",", Collections.nCopies(atores.size(), "?"));
        Object[] parametros = atores.toArray();
        var leitura = new TransactionTemplate(transactionManager); leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Map<String, Object> estado = leitura.execute(status -> {
            long empregado = c.empregadoA.getId();
            Map<String, Object> resultado = mapa(
                    "ocorrencias", jdbc.queryForList("select * from ocorrencia where id_empregado=? order by id", empregado),
                    "contextos", jdbc.queryForList("select x.* from contexto_ocorrencia x join ocorrencia o on o.id=x.ocorrencia_id "
                            + "where o.id_empregado=? order by x.id", empregado),
                    "anexos", jdbc.queryForList("select a.* from anexo_ocorrencia a join ocorrencia o on o.id=a.id_ocorrencia "
                            + "where o.id_empregado=? order by a.id", empregado),
                    "frequencias", jdbc.queryForList("select * from frequencia where id_empregado=? order by id", empregado),
                    "eventosFrequencia", jdbc.queryForList("select e.* from evento_frequencia e join frequencia f on f.id=e.id_frequencia "
                            + "where f.id_empregado=? order by e.id_evento_frequencia", empregado),
                    "historicos", jdbc.queryForList("select * from historico_empregado where id_empregado=? order by id_historico", empregado),
                    "permissoes", jdbc.queryForList("select * from permissao_excepcional where id_usuario_beneficiario in ("
                            + marcadores + ") order by id_permissao_excepcional", parametros),
                    "usos", jdbc.queryForList("select u.* from permissao_excepcional_uso u join permissao_excepcional p "
                            + "on p.id_permissao_excepcional=u.id_permissao_excepcional where p.id_usuario_beneficiario in ("
                            + marcadores + ") order by u.id_permissao_uso", parametros),
                    "auditoriaSucesso", jdbc.queryForList("select id,action,entity_type,entity_id,user_id,details_json "
                            + "from registro_auditoria where action in ('CONCEDER_PERMISSAO_EXCEPCIONAL',"
                            + "'USAR_PERMISSAO_EXCEPCIONAL','REVOGAR_PERMISSAO_EXCEPCIONAL') and user_id in ("
                            + marcadores + ") order by id", parametros));
            Map<String, String> arquivos = new TreeMap<>();
            for (String nome : em.createQuery("select a.nomeSalvo from AnexoOcorrencia a where a.ocorrencia.empregado.id=:e",
                    String.class).setParameter("e", empregado).getResultList()) {
                Path raiz = armazenamento.uploadDir().toAbsolutePath().normalize();
                Path arquivo = raiz.resolve(nome).normalize(); assertThat(arquivo.startsWith(raiz)).isTrue();
                try {
                    assertThat(Files.isRegularFile(arquivo)).isTrue();
                    byte[] bytes = Files.readAllBytes(arquivo); assertThat(bytes).isEqualTo(ARQUIVO);
                    arquivos.put(nome, hash(bytes));
                } catch (java.io.IOException ex) { throw new IllegalStateException("Arquivo sintético não preservado", ex); }
            }
            resultado.put("arquivos", arquivos);
            return resultado;
        });
        return mapper.valueToTree(estado);
    }

    /** O scheduler herdado é mock; drena-se a fila real para contraprova persistida, sem substituir persistência. */
    private void descarregarAuditoria() {
        for (int lote = 0; lote < 50; lote++) {
            Long antes = jdbc.queryForObject("select count(*) from registro_auditoria", Long.class);
            filaAuditoria.descarregar();
            Long depois = jdbc.queryForObject("select count(*) from registro_auditoria", Long.class);
            if (Objects.equals(antes, depois)) return;
        }
        throw new AssertionError("Fila real de auditoria não estabilizou no limite da fixture");
    }

    private long auditorias(JsonNode estado, String acao) {
        long total = 0;
        for (JsonNode linha : estado.path("auditoriaSucesso")) if (linha.path("action").asText().equals(acao)) total++;
        return total;
    }

    private Future<?> reterContrato(Cenario c, ExecutorService pool, CountDownLatch liberar, CompletableFuture<Long> pid) {
        return pool.submit(() -> emTransacao(() -> {
            assertThat(em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE)).isNotNull();
            pid.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
            try {
                if (!liberar.await(40, TimeUnit.SECONDS)) throw new AssertionError("Limite da barreira do Contrato contextual");
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
            return null;
        }));
    }

    private String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private void registrar(Cenario c, String fase, Object valor) throws Exception {
        if (evidencias == null) {
            Path raiz = Path.of("target/per010004-contextual").toAbsolutePath(); Files.createDirectories(raiz);
            evidencias = Files.createTempDirectory(raiz, "contrato-" + c.contratoA.getId() + "-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                mapa("criterio", "PER-010:004", "fase", fase, "camada", "MOCKMVC_FILTROS_JPA_POSTGRES_REAL",
                        "instante", Instant.now(), "contrato", c.contratoA.getId(), "evidencia", valor)) + "\n").getBytes(StandardCharsets.UTF_8);
        Path temporario = Files.createTempFile(evidencias, fase + "-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) canal.write(buffer); canal.force(true);
        }
        Files.move(temporario, evidencias.resolve(fase + ".json"), StandardCopyOption.ATOMIC_MOVE);
        try (FileChannel diretorio = FileChannel.open(evidencias, StandardOpenOption.READ)) { diretorio.force(true); }
    }
}
