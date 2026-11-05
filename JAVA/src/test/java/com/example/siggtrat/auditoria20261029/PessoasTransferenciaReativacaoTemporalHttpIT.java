package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasTransferenciaReativacaoTemporalHttpIT extends ContratosHttpBase {
    @Test
    void transferenciaDepoisDeReativacaoProgramadaMaterializaHabilitacaoTemporalUnica() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); return new Preparado(c, publicado(c, 4, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate hoje = RelogioSistema.hoje(), amanha = hoje.plusDays(1);
        int seed = Math.toIntExact(732000000L + f.contrato().id());
        JsonNode criado = enviar(f.c().localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa reativação programada " + seed,
                        "cpf", cpf(seed)), "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", hoje.minusDays(5)), 201, "Preparação: pessoa com vínculo vigente");
        ciclo(f, "inativar", hoje);
        ciclo(f, "reativar", amanha);
        assertThat(emTransacao(() -> em.find(Contrato.class, f.contrato().id()).getAtivo())).isFalse();
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(amanha);
            assertThat(consultar(f.c().localA, base(f.contrato().id()) + "/posicao-temporal?data=" + amanha,
                    "CON010 reativação passa a valer pela data, sem scheduler").path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
            conceder(f.c(), f.contrato().id(), "TRANSFERENCIA_GESTAO_LOCAL");
            enviar(f.c().localA, base(f.contrato().id()) + "/transferir-gestao", mapa("dataEfetiva", amanha,
                    "dataAto", amanha, "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                    "motivo", "Transferência após retomada temporal", "referenciaSei", "SEI-REATIVACAO-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                    "mapeamentos", List.of(mapa("composicaoOrigemId", f.contrato().composicao1(), "lotacaoDestinoId", f.c().lotacaoB.getId(),
                            "jornadaDestinoId", f.c().jornadaB.getId(), "cargoDestinoId", f.c().cargoB.getId()))), 200,
                    "CON022 transferência usa a habilitação válida na data atual");
            emTransacao(() -> {
                Empregado empregado = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:p", Empregado.class)
                        .setParameter("p", criado.path("pessoaId").asLong()).getSingleResult();
                assertThat(empregado.getStatus().getDescricao()).as("HIS005 projeção não usa Contrato.ativo desatualizado como segunda fonte")
                        .isEqualToIgnoringCase(ConstantesDominio.STATUS_ATIVO);
                assertThat(empregado.getGestaoLocal().getId()).isEqualTo(f.c().gestaoB.getId());
                assertThat(empregado.getContratos()).hasSize(1);
                assertThat(empregado.getContratos().get(0).getAtivo()).isTrue();
                empregado.setRg("CONTROLE-ESCRITA-PROJECAO"); em.flush();
                return null;
            });
        }
    }

    private void ciclo(Preparado f, String comando, LocalDate data) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "motivo", "Habilitação temporal documentada " + comando);
        JsonNode previa = enviar(f.c().localA, base(f.contrato().id()) + "/previas/" + comando, req, 200,
                "Prévia do evento de habilitação");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.c().localA, base(f.contrato().id()) + "/" + comando, req, 200, "Evento administrativo auditado");
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
}
