package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Hipótese a reproduzir pelo coordenador: CAT-004, HIS-004 e TMP-004/005.
 * A Gestão tinha nome comprovado na publicação e na criação do vínculo.
 * Uma renomeação cadastral posterior não documenta que o nome novo já valia
 * na data de um ato retroativo que altera somente matrícula.
 * Controle: um ato contemporâneo pode documentar o nome conhecido hoje.
 * Não aplica esse oráculo a correções materiais de CPF ou de número/ano.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoNomeGestaoEventoVinculoHttpIT extends ContratosHttpBase {

    @ParameterizedTest(name = "atoRetroativo={0}")
    @ValueSource(booleans = {true, false})
    void atoDeMatriculaPreservaNomeDaGestaoDoRecorteComFonteOriginal(boolean retroativo) throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        String nomeOriginal = c.gestaoA.getNome();
        LocalDate inicioVinculo = n.inicio().plusDays(1);
        String cpfLivre;
        do {
            cpfLivre = cpf(ThreadLocalRandom.current().nextInt(600_000_000, 700_000_000));
            String candidato = cpfLivre;
            if (emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", candidato).getSingleResult()) == 0L) break;
        } while (true);
        var criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", inicioVinculo,
                "matricula", "ORIGEM-GESTAO-" + n.id(),
                "pessoa", mapa("nome", "Pessoa com origem nominal documentada", "cpf", cpfLivre)),
                201, "VIN001 origem integrada anterior à renomeação cadastral da Gestão");
        long vinculoId = criado.path("id").asLong();
        String rota = "/api/vinculos-contratuais/" + vinculoId;
        var origem = consultar(c.localA, rota + "/eventos", "HIS004 fonte de identidade do vínculo");
        assertThat(origem).hasSize(1);
        assertThat(origem.get(0).path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(origem.get(0).path("gestaoLocalIdFotografia").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(origem.get(0).path("gestaoLocalNomeFotografia").asText()).isEqualTo(nomeOriginal);
        var fotoContrato = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + inicioVinculo,
                "HIS006 publicação comprova a mesma Gestão e seu nome anterior").path("versaoContratual");
        assertThat(fotoContrato.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(fotoContrato.path("gestaoLocalNome").asText()).isEqualTo(nomeOriginal);

        String nomeNovo = "Gestão renomeada posteriormente " + n.id();
        var renomeada = dados(executar(c.admin, put("/api/gestoes-locais/" + c.gestaoA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(mapa("nome", nomeNovo, "codigo", c.gestaoA.getCodigo(),
                                "ativo", true, "superintendenciaId", c.seA.getId()))),
                200, "Renomeação atual do cadastro não corrige a identidade histórica da Gestão"));
        assertThat(renomeada.path("nome").asText()).isEqualTo(nomeNovo);
        assertThat(emTransacao(() -> em.find(GestaoLocal.class, c.gestaoA.getId()).getNome())).isEqualTo(nomeNovo);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + inicioVinculo,
                "HIS004 o contrato continua mostrando o nome documentado no período")
                .path("versaoContratual").path("gestaoLocalNome").asText()).isEqualTo(nomeOriginal);

        LocalDate dataAto = retroativo ? inicioVinculo.plusDays(1) : RelogioSistema.hoje();
        enviar(c.localA, rota + "/alterar-matricula", mapa("dataEfetiva", dataAto,
                "novaMatricula", "NOVA-GESTAO-" + n.id(),
                "justificativa", "Documento comprova somente alteração de matrícula nesta data",
                "evidencia", "SEI-MATRICULA-SEM-RENOMEACAO-GESTAO",
                "autorizacao", "ATO-MATRICULA"), 200,
                "MAT001 nova versão de matrícula com identidade organizacional do recorte");
        var eventos = consultar(c.localA, rota + "/eventos", "HIS004 criação preservada e novo ato fotografado");
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0)).isEqualTo(origem.get(0));
        assertThat(eventos.get(1).path("dataEfetiva").asText()).isEqualTo(dataAto.toString());
        assertThat(eventos.get(1).path("gestaoLocalIdFotografia").asLong()).isEqualTo(c.gestaoA.getId());
        String esperado = retroativo ? nomeOriginal : nomeNovo;
        assertThat(eventos.get(1).path("gestaoLocalNomeFotografia").asText())
                .as("a matrícula não documenta uma renomeação retroativa da Gestão")
                .isEqualTo(esperado);
        emTransacao(() -> {
            var persistidos = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id",
                            EventoVinculoContratual.class).setParameter("v", vinculoId).getResultList();
            assertThat(persistidos).hasSize(2);
            assertThat(persistidos.get(0).getGestaoLocalNomeFotografia()).isEqualTo(nomeOriginal);
            assertThat(persistidos.get(1).getGestaoLocalNomeFotografia()).isEqualTo(esperado);
            assertThat(persistidos.get(1).getDataEfetiva()).isEqualTo(dataAto);
            return null;
        });
    }
}
