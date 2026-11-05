package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasProgramacaoCanceladaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void programacaoCanceladaNaoRecebeDesligamentoComumENovaProgramacaoContinuaPermitida() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                NovoContrato contrato = publicado(c, 2, null, false);
                MotivoDesligamento motivo = new MotivoDesligamento();
                motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Desligamento de programação " + contrato.id());
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(motivo); em.flush();
                return new Preparado(c, contrato, motivo.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate inicio = RelogioSistema.hoje().plusDays(5);
        LocalDate fim = inicio.plusDays(5);
        int seed = Math.toIntExact(725000000L + f.contrato().id());
        Map<String, Object> cadastro = mapa("pessoa", mapa("nome", "Pessoa programação cancelada " + seed, "cpf", cpf(seed)),
                "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                "dataInicio", inicio, "matricula", "PROGRAMADA-" + f.contrato().id());
        JsonNode criado = enviar(f.c().localA, V, cadastro, 201, "VIN005 programação futura registrada");
        long id = criado.path("id").asLong();
        enviar(f.c().localA, V + "/" + id + "/cancelar-programacao", acao(), 200,
                "VIN006 cancelamento preserva o fato sem convertê-lo em desligamento");
        JsonNode eventosAntes = consultar(f.c().localA, V + "/" + id + "/eventos", "Trilha preservada após cancelamento");
        assertThat(eventosAntes).hasSize(2);

        Map<String, Object> desligamento = acao();
        desligamento.put("dataFim", fim); desligamento.put("motivoDesligamentoId", f.motivoId());
        enviar(f.c().localA, V + "/" + id + "/desligar", desligamento, 409,
                "VIN006 DES001 ação comum não transforma programação cancelada em desligamento");
        assertThat(consultar(f.c().localA, V + "/" + id + "/eventos", "Rejeição não acrescenta evento de desligamento"))
                .isEqualTo(eventosAntes);
        emTransacao(() -> {
            VinculoContratual cancelado = em.find(VinculoContratual.class, id);
            assertThat(cancelado.getEstadoNegocial()).isEqualTo(EstadoNegocialVinculo.CANCELADO);
            assertThat(cancelado.getDataFim()).isNull();
            assertThat(cancelado.getMotivoDesligamento()).isNull();
            return null;
        });

        cadastro.remove("pessoa"); cadastro.put("pessoaId", criado.path("pessoaId").asLong());
        cadastro.put("matricula", "REPROGRAMADA-" + f.contrato().id());
        JsonNode novo = enviar(f.c().localA, V, cadastro, 201,
                "VIN006 a mesma Pessoa pode ter nova programação com matrícula não reservada");
        JsonNode terminado = enviar(f.c().localA, V + "/" + novo.path("id").asLong() + "/desligar", desligamento, 200,
                "DES001 vínculo registrado ainda admite desligamento futuro documentado");
        assertThat(terminado.path("dataFim").asText()).isEqualTo(fim.toString());
        assertThat(terminado.path("estadoNegocial").asText()).isEqualTo("REGISTRADO");
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Programação administrativa documentada", "evidencia", "SEI-PROGRAMACAO-CANCELADA",
                "autorizacao", "ATO-PROGRAMACAO-CANCELADA");
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoId) {}
}
