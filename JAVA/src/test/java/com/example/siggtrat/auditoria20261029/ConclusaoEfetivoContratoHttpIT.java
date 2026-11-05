package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** CMP-003 / RED-001/006/007 / REM-002: soma matemática não pode ser truncada em 32 bits. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoEfetivoContratoHttpIT extends ContratosHttpBase {
    @Test
    void publicacaoRecusaSomaQueSoCoincideComTotalPorOverflow() throws Exception {
        var c = criarCenarioPersistido();
        var f = preparar(c, Integer.MAX_VALUE - 2, List.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), false);
        enviar(c.localA, base(f.id()) + "/publicar", publicacao(), 400,
                "CMP003 soma real 6442450941 não equivale ao total informado 2147483645");
        assertThat(emTransacao(() -> em.find(Contrato.class, f.id()).getEstadoNegocial().name())).isEqualTo("RASCUNHO");
        assertThat(contar(f.id(), "EventoCicloVidaContrato")).isZero();
        assertThat(contar(f.id(), "VersaoContratoNegocial")).isZero();
    }

    @Test
    void reducaoDeEfetivoGrandeContinuaSujeitaAoPisoOriginal() throws Exception {
        var c = criarCenarioPersistido();
        var f = preparar(c, 40_000_000, List.of(40_000_000), true);
        var req = aditamento("REDUCAO-SEM-OVERFLOW", RelogioSistema.hoje().plusDays(11));
        req.put("novoEfetivoTotal", 28_000_000);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", f.composicoes().get(0), "quantidadeResultante", 28_000_000)));
        var previa = enviar(c.localA, base(f.id()) + "/aditamentos/previa", req, 200,
                "RED006 piso fixo mantém magnitude do efetivo publicado");
        assertThat(previa.path("pisoNormalReducao").asInt()).isEqualTo(30_000_000);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localA, base(f.id()) + "/aditamentos", req, 409,
                "RED001 redução de 30% permanece bloqueada mesmo acima do limiar de overflow de int");
        assertThat(contar(f.id(), "AditamentoContratual")).isZero();
    }

    @Test
    void aditamentoRecusaTotalResultanteQueSofreriaOverflow() throws Exception {
        var c = criarCenarioPersistido();
        var f = preparar(c, 10, List.of(10, 0, 0), true);
        var req = aditamento("ADI-SOMA-NAO-REPRESENTAVEL", RelogioSistema.hoje().plusDays(11));
        req.put("novoEfetivoTotal", Integer.MAX_VALUE - 2);
        req.put("alteracoesComposicao", f.composicoes().stream()
                .map(id -> mapa("composicaoId", id, "quantidadeResultante", Integer.MAX_VALUE)).toList());
        enviar(c.localA, base(f.id()) + "/aditamentos/previa", req, 400,
                "CMP003 variações são somadas sem descartar os bits altos");
        assertThat(contar(f.id(), "AditamentoContratual")).isZero();
    }

    @Test
    void remanejamentoRecusaVariacoesCujaSomaRealNaoEhZero() throws Exception {
        var c = criarCenarioPersistido();
        var f = preparar(c, 10, List.of(10, 0, 0, 0), true);
        var req = mapa("identificacao", "REM-SOMA-4294967296", "dataEfetiva", RelogioSistema.hoje().plusDays(11),
                "justificativa", "Soma real deve ser preservada", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", f.composicoes().get(1), "variacao", Integer.MAX_VALUE),
                        mapa("composicaoId", f.composicoes().get(2), "variacao", Integer.MAX_VALUE),
                        mapa("composicaoId", f.composicoes().get(3), "variacao", 2)));
        enviar(c.localA, base(f.id()) + "/remanejamentos/previa", req, 400,
                "REM002 soma matemática de 4294967296 não é redistribuição neutra");
        assertThat(contar(f.id(), "RemanejamentoContratual")).isZero();
    }

    private Preparado preparar(Cenario c, int total, List<Integer> quantidades, boolean publicar) throws Exception {
        return emTransacao(() -> {
            try {
                var inicio = RelogioSistema.hoje().plusDays(10);
                long id = enviar(c.localA, "/api/contratos", rascunho(c, "INT-" + UUID.randomUUID().toString().substring(0, 8),
                        total, inicio, inicio.plusDays(60)), 201, "Contrato para critério quantitativo").path("id").asLong();
                List<Long> ids = new ArrayList<>();
                for (int i = 0; i < quantidades.size(); i++) {
                    long cargoId = c.cargoA.getId();
                    if (i > 0) {
                        Cargo cargo = new Cargo(); cargo.setGestaoLocal(c.gestaoA);
                        cargo.setNome("Cargo numérico " + UUID.randomUUID()); cargo.setAtivo(true); em.persist(cargo); em.flush();
                        cargoId = cargo.getId();
                    }
                    ids.add(enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoId, quantidades.get(i)), 201,
                            "Composição explícita de magnitude válida individualmente").path("id").asLong());
                }
                if (publicar) enviar(c.localA, base(id) + "/publicar", publicacao(), 200, "Publicação de base quantitativa coerente");
                return new Preparado(id, List.copyOf(ids));
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private java.util.Map<String, Object> publicacao() {
        return mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-EFETIVO-EXATO", "justificativa", "Publicação documentada");
    }

    private long contar(long id, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult());
    }

    private record Preparado(long id, List<Long> composicoes) {}
}
