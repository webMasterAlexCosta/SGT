package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasProjecaoHabilitacaoHttpIT extends ContratosHttpBase {
    @Test
    void opcionalNaPessoaAposInativacaoEReativacaoMantemMesmaLinhaCanonica() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); return new Preparado(c, publicado(c, 4, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate hoje = RelogioSistema.hoje();
        int seed = Math.toIntExact(733000000L + f.contrato().id());
        JsonNode criado = enviar(f.c().localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa habilitação operacional " + seed, "cpf", cpf(seed)),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", hoje.minusDays(5)), 201, "Preparação: Pessoa e vínculo vigente");
        long pessoaId = criado.path("pessoaId").asLong();
        long empregadoId = emTransacao(() -> em.createQuery(
                "select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult());
        LinhaCanonica antes = linhaCanonica(pessoaId);

        ciclo(f, "inativar", hoje);
        atualizarRg(f, pessoaId, "RG-OPCIONAL-733");
        assertThat(linhaCanonica(pessoaId)).as("VIN013 ALO008 inativação e opcional não encerram nem recriam a linha")
                .isEqualTo(antes);
        conferirProjecao(empregadoId, ConstantesDominio.STATUS_INATIVO, false, "RG-OPCIONAL-733");

        LocalDate amanha = hoje.plusDays(1);
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(amanha);
            ciclo(f, "reativar", amanha);
            atualizarRg(f, pessoaId, "RG-REATIVADO-733");
            conferirProjecao(empregadoId, ConstantesDominio.STATUS_ATIVO, true, "RG-REATIVADO-733");
            long eventos = quantidadeEventosContrato(f.contrato().id());
            enviar(f.c().localA, base(f.contrato().id()) + "/reativar",
                    mapa("dataEfetiva", amanha, "motivo", "Repetição não cria nova reativação"), 409,
                    "Contrato já ativo recusa reativação repetida antes de qualquer mutação");
            assertThat(quantidadeEventosContrato(f.contrato().id())).isEqualTo(eventos);
            assertThat(linhaCanonica(pessoaId)).as("Reativação reutiliza os mesmos vínculos, faixas e eventos históricos")
                    .isEqualTo(antes);
            assertThat(consultar(f.c().localA, base(f.contrato().id()) + "/posicao-temporal?data=" + hoje,
                    "O intervalo administrativamente inativo permanece histórico")
                    .path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        }
    }

    private void atualizarRg(Preparado f, long pessoaId, String rg) throws Exception {
        ObjectNode dto = (ObjectNode) consultar(f.c().localA, "/api/pessoas/" + pessoaId,
                "A Pessoa é a fonte dos dados opcionais");
        dto.put("rg", rg);
        JsonNode salvo = dados(executar(f.c().localA, put("/api/pessoas/" + pessoaId + "/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200,
                "COL011 atualização exclusiva de RG funciona após mudança administrativa do Contrato"));
        assertThat(salvo.path("rg").asText()).isEqualTo(rg);
    }

    private void conferirProjecao(long empregadoId, String status, boolean contratoAtivo, String rg) {
        emTransacao(() -> {
            Empregado e = em.find(Empregado.class, empregadoId);
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(status);
            assertThat(e.getContratos()).hasSize(1);
            assertThat(e.getContratos().get(0).getAtivo()).isEqualTo(contratoAtivo);
            assertThat(e.getPessoaCanonica().getRg()).isEqualTo(rg);
            return null;
        });
    }

    private LinhaCanonica linhaCanonica(long pessoaId) {
        return emTransacao(() -> new LinhaCanonica(
                linhas("select v.id,v.contrato.id,v.dataInicio,v.dataFim,v.estadoNegocial from VinculoContratual v "
                        + "where v.pessoa.id=:p order by v.id", pessoaId),
                linhas("select a.id,a.vinculo.id,a.dataInicio,a.dataFim,a.estadoNegocial from AlocacaoVinculo a "
                        + "where a.vinculo.pessoa.id=:p order by a.id", pessoaId),
                linhas("select e.id,e.tipoEvento,e.hashEvento from EventoVinculoContratual e "
                        + "where e.vinculo.pessoa.id=:p order by e.id", pessoaId),
                linhas("select e.id,e.tipoEvento,e.hashEvento from EventoAlocacaoVinculo e "
                        + "where e.vinculo.pessoa.id=:p order by e.id", pessoaId)));
    }

    private List<String> linhas(String jpql, long pessoaId) {
        return em.createQuery(jpql, Object[].class).setParameter("p", pessoaId)
                .getResultList().stream().map(Arrays::toString).toList();
    }

    private long quantidadeEventosContrato(long contratoId) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult());
    }

    private void ciclo(Preparado f, String comando, LocalDate data) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "motivo", "Habilitação administrativa " + comando);
        JsonNode previa = enviar(f.c().localA, base(f.contrato().id()) + "/previas/" + comando, req, 200,
                "Prévia do evento administrativo");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(f.c().localA, base(f.contrato().id()) + "/" + comando, req, 200,
                "Habilitação preserva a linha de vínculo e alocação");
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
    private record LinhaCanonica(List<String> vinculos, List<String> alocacoes,
                                 List<String> eventosVinculo, List<String> eventosAlocacao) {}
}
