package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** HIS001/HIS006: ausência histórica também não autoriza fabricar o antes do novo ato. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoHabilitacaoSemOrigemHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"inativar", "reativar"})
    void primeiroAtoMantemValorAnteriorDesconhecidoNaTrilha(String operacao) throws Exception {
        var f = preparar();
        var req = mapa("dataEfetiva", f.n().inicio().plusDays(2), "dataAto", RelogioSistema.hoje(),
                "motivo", "Primeiro estado documentalmente comprovado", "referenciaSei", "SEI-PRIMEIRO-ESTADO");
        var previa = enviar(f.c().localA, base(f.n().id()) + "/previas/" + operacao, req, 200,
                "HIS006 prévia de ato que estabelece estado comprovado");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(f.c().localA, base(f.n().id()) + "/" + operacao, req, 200,
                "HIS006 evento novo preserva os limites do conhecimento anterior");
        var eventos = consultar(f.c().localA, base(f.n().id()) + "/eventos", "AUD001 antes desconhecido permanece explícito");
        var evento = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals(operacao.equals("inativar")
                        ? "INATIVACAO_ADMINISTRATIVA" : "REATIVACAO_ADMINISTRATIVA"))
                .findFirst().orElseThrow();
        assertThat(evento.path("valorAnterior").isNull()).isTrue();
        assertThat(evento.path("valorNovo").asText()).isEqualTo(operacao.equals("inativar") ? "INATIVO" : "ATIVO");
        assertThat(evento.path("usuarioResponsavelId").asLong()).isEqualTo(f.c().localA.getId());
    }

    @Test
    void habilitacaoDesconhecidaNaoLiberaNovaAlocacaoPorFlagMaterializada() throws Exception {
        var f = preparar();
        enviar(f.c().localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Nova pessoa sem prova operacional " + f.n().id(), "cpf", cpf(680000000 + Math.toIntExact(f.n().id()))),
                "contratoId", f.n().id(), "composicaoId", f.n().composicao1(), "dataInicio", f.n().inicio()),
                409, "HIS006 informação ausente não libera nova operação pelo valor materializado atual");
        assertThat(emTransacao(() -> em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                .setParameter("id", f.n().id()).getSingleResult())).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"aditamentos", "remanejamentos"})
    void documentoNovoExigeHabilitacaoComprovadaSemInferirFlagAtual(String operacao) throws Exception {
        var f = preparar();
        var req = operacao.equals("aditamentos") ? aditamento("ADI-SEM-ORIGEM", f.n().inicio().plusDays(1))
                : remanejamento(f.n(), "REM-SEM-ORIGEM", f.n().inicio().plusDays(1), 5);
        if (operacao.equals("aditamentos")) req.put("novaDescricao", "Descrição depende de operação habilitada");
        var previa = enviar(f.c().localA, base(f.n().id()) + "/" + operacao + "/previa", req, 200,
                "HIS001 consulta de impactos não publica o documento");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.c().localA, base(f.n().id()) + "/" + operacao, req, 409,
                "HIS006 documento novo não transforma flag atual em habilitação histórica comprovada");
        assertThat(consultar(f.c().localA, base(f.n().id()) + "/" + operacao,
                "GOV003 recusa não publica instrumento parcial").isEmpty()).isTrue();
    }

    private Preparado preparar() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 100, 25, true); }
            catch (Exception e) { throw new IllegalStateException(e); } });
        emTransacao(() -> {
            // DML de fixture; simula campo ausente em fotografia imutável, sem criar fato histórico.
            assertThat(em.createQuery("update VersaoContratoNegocial set habilitacaoAdministrativa=null where contrato.id=:id")
                    .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            em.find(Contrato.class, n.id()).setAtivo(true); return null;
        });
        return new Preparado(c, n);
    }
    private record Preparado(Cenario c, NovoContrato n) {}
}
