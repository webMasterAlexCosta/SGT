package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** RPI04: materializar uma projeção não transforma prova parcial em fotografia integral. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Ind08AcervoDerivadoMascaraHttpIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;

    private record Origem(Cenario c, NovoContrato contrato, long versaoOriginal,
                          long eventoPublicacao, Map<String,Object> fotografia) { }
    private record ProvaParcial(Origem origem, JsonNode descricao, JsonNode matricula) { }

    @Test
    void fotoDeAdiHerdeMascaraPorCampoESuaInterpretacaoNaoMudeComProvaPosterior() throws Exception {
        ProvaParcial parcial = provaParcial();
        Origem o = parcial.origem();
        LocalDate dataAdi = o.contrato().inicio().plusDays(3);
        Map<String,Object> req = aditamento("IND08-ACERVO-DERIVADO-" + o.contrato().id(), dataAdi);
        req.put("novaDescricao", "Somente a descrição foi alterada pelo ADI");
        JsonNode adi = registrar(o, req);
        long eventoAdi = adi.path("eventoId").asLong();
        JsonNode fotoAntes = fotoDoEvento(o, eventoAdi, Instant.now());
        long versaoAdi = fotoAntes.path("id").asLong();
        Instant kFoto = Instant.parse(fotoAntes.path("registradoEm").asText());
        JsonNode posicaoAntes = posicao(o, dataAdi, kFoto);

        // Só agora existe fonte que declara ausência. Ela pertence à origem
        // publicada e muda a posição conhecida depois, sem reescrever o ADI.
        JsonNode ausencia = incorporar(o, fonte(o, parcial.matricula(),
                List.of("diaSemanaReduzido", "horasDiaReduzido"),
                mapa("diaSemanaReduzido", null, "horasDiaReduzido", null)));
        Instant kAusencia = Instant.parse(ausencia.path("registradoEm").asText());
        JsonNode fotoDepois = fotoDoEvento(o, eventoAdi, kAusencia);
        JsonNode posicaoAntigaDepois = posicao(o, dataAdi, kFoto);
        JsonNode posicaoNova = posicao(o, dataAdi, kAusencia);
        JsonNode provaP2Depois = consultar(o.c().localA,
                base(o.contrato().id()) + "/provas-historicas?conhecidoAte=" + kFoto,
                "Revisão parcial conserva sua própria interpretação").path(1).path("fotografia");
        long provaMatricula = parcial.matricula().path("id").asLong();

        assertAll("Acervo derivado conserva máscaras e fontes do instante de sua materialização",
                () -> estado(fotoAntes, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(fotoAntes, "horasDiaReduzido", "NAO_COMPROVADO"),
                () -> estado(posicaoAntes, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(fotoDepois, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(posicaoAntigaDepois, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(provaP2Depois, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> estado(fotoDepois, "tipoContratacaoDescricao", "NAO_COMPROVADO"),
                () -> estado(fotoDepois, "matriculaTipoContratacaoObrigatoria", "VALOR_COMPROVADO"),
                () -> assertThat(fotoDepois.path("matriculaTipoContratacaoObrigatoria").booleanValue()).isFalse(),
                () -> origem(fotoDepois, "matriculaTipoContratacaoObrigatoria", "provaId", provaMatricula),
                () -> origem(fotoDepois, "matriculaVinculoObrigatoria", "provaId", provaMatricula),
                () -> origem(fotoDepois, "matriculaVinculoObrigatoriaEfetiva", "provaId", provaMatricula),
                () -> origem(fotoDepois, "descricao", "eventoId", eventoAdi),
                () -> origem(fotoDepois, "numeroContrato", "versaoId", o.versaoOriginal()),
                () -> estado(posicaoNova, "diaSemanaReduzido", "AUSENCIA_COMPROVADA"),
                () -> origem(posicaoNova, "diaSemanaReduzido", "provaId", ausencia.path("id").asLong()),
                () -> assertThat(fotoDepois.path("id").asLong()).isEqualTo(versaoAdi),
                () -> assertThat(Instant.parse(fotoDepois.path("registradoEm").asText())).isEqualTo(kFoto),
                () -> assertThat(fotoDepois.path("descricao").asText()).isEqualTo(req.get("novaDescricao")),
                () -> assertThat(fotoAntes.path("provaCampos")).isEqualTo(fotoDepois.path("provaCampos")));
        emTransacao(() -> {
            em.clear();
            VersaoContratoNegocial persistida = em.find(VersaoContratoNegocial.class, versaoAdi);
            assertThat(persistida.getDiaSemanaReduzido()).isNull();
            assertThat(persistida.getHorasDiaReduzido()).isNull();
            assertThat(persistida.getMatriculaTipoContratacaoObrigatoria()).isFalse();
            assertThat(persistida.getRegistradoEm()).isEqualTo(kFoto);
            assertThat(persistida.getEventoOrigem().getId()).isEqualTo(eventoAdi);
            return null;
        });
    }

    @Test
    void adiQueDeclaraRemocaoComprovaAusenciaSemPromoverOutrosCamposDaFotografia() throws Exception {
        ProvaParcial parcial = provaParcial();
        Origem o = parcial.origem();
        LocalDate dataAdi = o.contrato().inicio().plusDays(4);
        Map<String,Object> req = aditamento("IND08-ACERVO-REMOCAO-" + o.contrato().id(), dataAdi);
        req.put("removerConfiguracaoDiaReduzido", true);
        JsonNode adi = registrar(o, req);
        long eventoAdi = adi.path("eventoId").asLong();
        JsonNode foto = fotoDoEvento(o, eventoAdi, Instant.now());
        Instant kFoto = Instant.parse(foto.path("registradoEm").asText());
        JsonNode posicao = posicao(o, dataAdi, kFoto);

        assertAll("Ausência exige o delta documental próprio; campos herdados conservam suas fontes",
                () -> estado(foto, "diaSemanaReduzido", "AUSENCIA_COMPROVADA"),
                () -> estado(foto, "horasDiaReduzido", "AUSENCIA_COMPROVADA"),
                () -> origem(foto, "diaSemanaReduzido", "eventoId", eventoAdi),
                () -> origem(foto, "horasDiaReduzido", "eventoId", eventoAdi),
                () -> estado(foto, "tipoContratacaoDescricao", "NAO_COMPROVADO"),
                () -> origem(foto, "descricao", "provaId", parcial.descricao().path("id").asLong()),
                () -> origem(foto, "matriculaTipoContratacaoObrigatoria", "provaId", parcial.matricula().path("id").asLong()),
                () -> assertThat(foto.path("matriculaTipoContratacaoObrigatoria").booleanValue()).isFalse(),
                () -> assertThat(foto.path("diaSemanaReduzido").isNull()).isTrue(),
                () -> assertThat(foto.path("horasDiaReduzido").isNull()).isTrue(),
                () -> assertThat(foto.path("provaCampos").path("diaSemanaReduzido"))
                        .isEqualTo(posicao.path("provaCampos").path("diaSemanaReduzido")));
    }

    private ProvaParcial provaParcial() throws Exception {
        Origem o = origem();
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.descricao=null, v.tipoContratacaoDescricao=null, "
                    + "v.matriculaVinculoObrigatoria=null, v.matriculaTipoContratacaoObrigatoria=null, "
                    + "v.matriculaVinculoObrigatoriaEfetiva=null, v.diaSemanaReduzido=null, v.horasDiaReduzido=null where v.id=:id")
                    .setParameter("id", o.versaoOriginal()).executeUpdate();
            return null;
        });
        conceder(o.c(), o.contrato().id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode descricao = incorporar(o, fonte(o, null, List.of("descricao"),
                mapa("descricao", "Descrição comprovada sem prova de dia reduzido")));
        JsonNode matricula = incorporar(o, fonte(o, descricao,
                List.of("matriculaVinculoObrigatoria", "matriculaTipoContratacaoObrigatoria", "matriculaVinculoObrigatoriaEfetiva"),
                mapa("matriculaVinculoObrigatoria", false, "matriculaTipoContratacaoObrigatoria", false,
                        "matriculaVinculoObrigatoriaEfetiva", false)));
        return new ProvaParcial(o, descricao, matricula);
    }

    private Origem origem() {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        return emTransacao(() -> {
            var v = motor.fotografiaOriginalConhecida(em.find(Contrato.class, n.id()), Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked") Map<String,Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
            for (String metadado : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(metadado);
            return new Origem(c, n, v.getId(), v.getEventoOrigem().getId(), foto);
        });
    }

    private Map<String,Object> fonte(Origem o, JsonNode anterior, List<String> campos, Map<String,Object> valores) {
        Map<String,Object> foto = new LinkedHashMap<>(o.fotografia()); foto.putAll(valores);
        return mapa("eventoOrigemId", o.eventoPublicacao(),
                "versaoAnteriorId", anterior == null ? o.versaoOriginal() : anterior.path("versaoGeradaId").asLong(),
                "provaAnteriorId", anterior == null ? null : anterior.path("id").asLong(),
                "dataDocumento", o.contrato().inicio(), "referenciaSei", "SEI-SINTETICO-ACERVO-IND08",
                "motivo", "Prova parcial sintética de campo", "autorizacaoAdministrativa", "ATO-SINTETICO-ACERVO-IND08",
                "versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, o.contrato().id()).getVersion()),
                "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", false,
                "camposComprovados", campos, "fotografia", foto, "composicoes", List.of());
    }

    private JsonNode incorporar(Origem o, Map<String,Object> req) throws Exception {
        String rota = base(o.contrato().id()) + "/provas-historicas";
        JsonNode previa = enviar(o.c().localA, rota + "/previa", req, 200, "Prévia de fonte parcial real");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(o.c().localA, rota, req, 201, "Incorporação conserva máscara documental");
    }

    private JsonNode registrar(Origem o, Map<String,Object> req) throws Exception {
        String rota = base(o.contrato().id()) + "/aditamentos";
        JsonNode previa = enviar(o.c().localA, rota + "/previa", req, 200, "Prévia do único delta pretendido");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(o.c().localA, rota, req, 201, "ADI materializa sua projeção sem ampliar prova");
    }

    private JsonNode fotoDoEvento(Origem o, long evento, Instant conhecido) throws Exception {
        JsonNode fotos = dados(executar(o.c().localA, get(base(o.contrato().id()) + "/versoes")
                .param("conhecidoAte", conhecido.toString()), 200, "Acervo imutável no corte de conhecimento"));
        for (JsonNode foto : fotos) if (foto.path("eventoOrigemId").asLong() == evento) return foto;
        throw new AssertionError("Fotografia persistida do ADI ausente no acervo");
    }

    private JsonNode posicao(Origem o, LocalDate data, Instant conhecido) throws Exception {
        return dados(executar(o.c().localA, get(base(o.contrato().id()) + "/posicao-temporal")
                .param("data", data.toString()).param("conhecidoAte", conhecido.toString()), 200,
                "Posição no D/K da materialização")).path("versaoContratual");
    }

    private void estado(JsonNode objeto, String campo, String esperado) {
        JsonNode prova = objeto.path("provaCampos").path(campo);
        assertThat(prova.path("visibilidade").asText()).isEqualTo("DISPONIVEL");
        assertThat(prova.path("estado").asText()).as("Prova individual de " + campo).isEqualTo(esperado);
        if ("NAO_COMPROVADO".equals(esperado)) assertThat(prova.path("origens")).isEmpty();
        else assertThat(prova.path("origens")).isNotEmpty();
    }

    private void origem(JsonNode objeto, String campo, String referencia, long id) {
        boolean encontrada = false;
        for (JsonNode origem : objeto.path("provaCampos").path(campo).path("origens"))
            if (origem.path(referencia).isNumber() && origem.path(referencia).asLong() == id) encontrada = true;
        assertThat(encontrada).as("Fonte herdada real de " + campo + ": " + referencia + "=" + id).isTrue();
    }
}
