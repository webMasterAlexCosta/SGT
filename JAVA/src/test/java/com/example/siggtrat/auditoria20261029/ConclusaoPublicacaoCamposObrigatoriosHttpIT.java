package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** CON-006/008, EMP-001: uma única omissão, composição válida e leituras após a transação HTTP. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoPublicacaoCamposObrigatoriosHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "publicação exige {0} sem outro motivo de bloqueio")
    @CsvSource(delimiter = '|', value = {
            "numeroContrato|Número, ano, descrição e vigência completa",
            "anoContrato|Número, ano, descrição e vigência completa",
            "descricao|Número, ano, descrição e vigência completa",
            "dataInicio|Número, ano, descrição e vigência completa",
            "dataFim|Número, ano, descrição e vigência completa",
            "tipoContratacaoId|Tipo de Contratação",
            "cnpjEmpresa|CNPJ, razão social e nome fantasia",
            "razaoSocialEmpresa|CNPJ, razão social e nome fantasia",
            "nomeFantasiaEmpresa|CNPJ, razão social e nome fantasia",
            "referenciaSei|fonte comprobatória"
    })
    void omissaoIsoladaImpedePublicacaoECompletarSomenteOCampoPermitePublicar(
            String ausente, String mensagemEsperada) throws Exception {
        var c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(5);
        var fim = inicio.plusDays(60);
        var completo = rascunho(c, "OBR-ISOLADO-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim);
        var incompleto = new LinkedHashMap<>(completo);
        if (!ausente.equals("referenciaSei")) incompleto.remove(ausente);
        long id = enviar(c.localA, "/api/contratos", incompleto, 201,
                "CON008 rascunho admite a ausência isolada de " + ausente).path("id").asLong();

        var recorte = composicao(c, c.cargoA.getId(), 5);
        // Mesmo quando falta uma data do contrato, a preparação da composição
        // informa expressamente o período pretendido: nenhum 400 por composição ausente.
        recorte.put("dataInicioVigencia", inicio);
        recorte.put("dataFimVigencia", fim);
        long composicaoId = enviar(c.localA, base(id) + "/composicoes", recorte, 201,
                "CMP003 composição única de cinco vagas cobre integralmente o período pretendido").path("id").asLong();
        var publicar = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-OBRIGATORIO-ISOLADO",
                "justificativa", "Publicação de fonte documental conferida");
        if (ausente.equals("referenciaSei")) publicar.remove("referenciaSei");

        var erro = enviar(c.localA, base(id) + "/publicar", publicar, 400,
                "CON006/008 EMP001 publicação recusa somente a ausência de " + ausente);
        assertThat(erro.path("message").asText()).containsIgnoringCase(mensagemEsperada);
        emTransacao(() -> {
            var contrato = em.find(Contrato.class, id);
            assertThat(contrato.getEstadoNegocial().name()).isEqualTo("RASCUNHO");
            assertThat(contrato.getPublicadoEm()).isNull();
            assertThat(contrato.getPublicadoPorId()).isNull();
            assertThat(contrato.getEfetivoOriginalPublicado()).isNull();
            assertThat(contar(id, "EventoCicloVidaContrato")).isZero();
            assertThat(contar(id, "VersaoContratoNegocial")).isZero();
            assertThat(contar(id, "ComposicaoContratual")).isEqualTo(1L);
            var composicao = em.find(ComposicaoContratual.class, composicaoId);
            assertThat(composicao.getEfetivoPrevisto()).isEqualTo(5);
            assertThat(composicao.getDataInicioVigencia()).isEqualTo(inicio);
            assertThat(composicao.getDataFimVigencia()).isEqualTo(fim);
            return null;
        });

        // O controle positivo difere somente no campo omitido, comprovando que
        // a preparação satisfaz as demais condições e a recusa não é incidental.
        if (ausente.equals("referenciaSei")) publicar.put("referenciaSei", "SEI-OBRIGATORIO-ISOLADO");
        else executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON).content(json(completo)), 200,
                "CON008 completar somente o campo ausente mantém a mesma preparação");
        enviar(c.localA, base(id) + "/publicar", publicar, 200,
                "CON008 a mesma preparação torna-se publicável após completar " + ausente);
        emTransacao(() -> {
            var contrato = em.find(Contrato.class, id);
            assertThat(contrato.getEstadoNegocial().name()).isEqualTo("PUBLICADO");
            assertThat(contrato.getEfetivoOriginalPublicado()).isEqualTo(5);
            assertThat(contrato.getPublicadoPorId()).isEqualTo(c.localA.getId());
            assertThat(contrato.getPublicadoEm()).isNotNull();
            assertThat(contar(id, "EventoCicloVidaContrato")).isEqualTo(1L);
            assertThat(contar(id, "VersaoContratoNegocial")).isEqualTo(1L);
            assertThat(contar(id, "ComposicaoContratual")).isEqualTo(1L);
            return null;
        });
    }

    private long contar(long contratoId, String entidade) {
        return em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contratoId).getSingleResult();
    }
}
