package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.PainelCalculo;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** AUD-05/PER-003/PER-006: alcance geográfico igual não concede capacidades de outro perfil. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud05PainelCapacidadesHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"detalhamento", "contratos", "empregados", "frequencia", "relatorios"})
    void calculoPersistidoComUmaCapacidadeDivergenteNaoPodeSerLido(String capacidade) throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = calculoNacional(capacidade);
        executar(c.nacional, get("/api/painel/calculos/{id}", id), 403,
                "AUD-05 a consulta por ID revalida também a capacidade " + capacidade);
        assertEquals(PainelCalculoStatus.PENDENTE, emTransacao(() -> em.find(PainelCalculo.class, id).getStatus()),
                "Negação de leitura não pode iniciar ou alterar um cálculo de outro contexto");
    }

    @Test
    void mesmosAlcanceECapacidadesPermitemCompartilhamentoDoAgregado() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = calculoNacional(null);
        var pagina = dados(executar(c.nacional, get("/api/painel/calculos/{id}", id), 200,
                "Agregado nacional continua acessível ao perfil com as capacidades correspondentes"));
        assertEquals(id, pagina.path("id").asLong());
        assertEquals("PENDENTE", pagina.path("status").asText());
        executar(c.admin, get("/api/painel/calculos/{id}", id), 200,
                "Perfis distintos com o mesmo alcance e capacidades podem compartilhar o agregado");
    }

    @Test
    void mudancaParaPresidenciaRevogaLeituraDeCalculoDetalhadoMesmoComMesmaSessao() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = calculoNacional(null);
        executar(c.nacional, get("/api/painel/calculos/{id}", id), 200, "Acesso nacional inicial");
        emTransacao(() -> {
            em.find(UsuarioAplicacao.class, c.nacional.getId()).setPerfil(PerfilAcesso.PRESIDENTE);
            return null;
        });
        executar(c.nacional, get("/api/painel/calculos/{id}", id), 403,
                "PER-003 revogação de detalhamento vale para a mesma identidade e sessão");
        executar(c.presidente, get("/api/painel/calculos/{id}", id), 403,
                "A Presidência nacional não herda o detalhamento pessoal do Gestor Nacional");
    }

    private long calculoNacional(String divergente) {
        return emTransacao(() -> {
            PainelCalculo p = new PainelCalculo();
            p.setChaveConsulta(UUID.randomUUID().toString());
            p.setDataReferencia(RelogioSistema.hoje().minusDays(1));
            p.setPeriodoDias(7);
            p.setNivelEscopo("NACIONAL");
            p.setNomeEscopo("Agregado sintético da auditoria de capacidades");
            p.setPermiteDetalhamentoOperacional(!"detalhamento".equals(divergente));
            p.setPodeVisualizarContratos(!"contratos".equals(divergente));
            p.setPodeVisualizarEmpregados(!"empregados".equals(divergente));
            p.setPodeOperarFrequencia("frequencia".equals(divergente));
            p.setPodeAcessarRelatorios(!"relatorios".equals(divergente));
            em.persist(p); em.flush();
            return p.getId();
        });
    }
}
