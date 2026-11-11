package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetificacaoCapacidadeContratualHttpIT extends ContratosHttpBase {
    @Test
    void retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 100, false); var efeito = RelogioSistema.hoje().plusDays(5);
        var original = aditamento("ADI-CAPACIDADE-RETIFICADA", efeito); original.put("novoEfetivoTotal", 101);
        original.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao2(), "quantidadeResultante", 1)));
        var instrumento = registrarAditamento(c, n.id(), original);
        enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "composicaoId", n.composicao2(),
                "dataInicio", efeito.plusDays(1), "pessoa", mapa("nome", "Pessoa dependente da versão anterior", "cpf", "52998224725")),
                201, "CMP-006 vínculo depende da capacidade do instrumento a retificar");
        em.flush(); em.clear();
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "CMP-007 fatos antes de retificação incompatível");
        var instrumentosAntes = consultar(c.localA, base(n.id()) + "/aditamentos", "Fotografia integral sintética do instrumento antes da recusa");
        assertThat(instrumentosAntes).hasSize(1);
        assertThat(instrumentosAntes.get(0)).isEqualTo(instrumento);
        var retificar = aditamento("ADI-CAPACIDADE-RETIFICADA", efeito); retificar.put("novaDescricao", "Somente descrição revisada");
        retificar.put("eventoPrecedenteId", instrumento.path("eventoId").asLong()); retificar.put("relacaoPrecedencia", "RETIFICA");
        previaAditamento(c, n.id(), retificar);
        enviar(c.localA, base(n.id()) + "/aditamentos/" + instrumento.path("id").asLong() + "/retificar", retificar,
                409, "CMP-006 retificação não pode retirar capacidade pelo desaparecimento de item da versão anterior");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "CMP-007 versão anterior preservada após recusa")).isEqualTo(eventosAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-007 retificação recusada não cria versão")).hasSize(1);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "Releitura do instrumento original por ID, versão, itens e situação"))
                .isEqualTo(instrumentosAntes);
    }

    @Test
    void retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo() throws Exception {
        var c = criarCenario(); var inicio = RelogioSistema.hoje().plusDays(2); var efeito = inicio.plusDays(2);
        var cargoB = new Cargo("Cargo B retificação", "2"); cargoB.setGestaoLocal(c.gestaoA); em.persist(cargoB);
        var cargoC = new Cargo("Cargo C retificação", "3"); cargoC.setGestaoLocal(c.gestaoA); em.persist(cargoC);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "REM-RETIFICA-" + c.contratoA.getId(), 100, inicio, inicio.plusMonths(3)),
                201, "REM-004 rascunho com três composições explícitas").path("id").asLong();
        long a = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 100), 201, "composição A").path("id").asLong();
        long b = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoB.getId(), 0), 201, "composição B zerada").path("id").asLong();
        long destinoC = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoC.getId(), 0), 201, "composição C zerada").path("id").asLong();
        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-REM-RETIFICA"),
                200, "REM-004 composição original publicada");
        var original = mapa("identificacao", "REM-CAPACIDADE-RETIFICADA", "dataEfetiva", efeito, "justificativa", "Ativar destino B",
                "confirmarImpacto", true, "itens", List.of(mapa("composicaoId", a, "variacao", -1), mapa("composicaoId", b, "variacao", 1)));
        previaRemanejamento(c, id, original);
        var instrumento = enviar(c.localA, base(id) + "/remanejamentos", original, 201, "REM-004 versão original cria capacidade B");
        enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", id, "composicaoId", b, "dataInicio", efeito.plusDays(1),
                "pessoa", mapa("nome", "Pessoa dependente do destino B", "cpf", "52998224725")), 201,
                "CMP-006 programação usa B publicada pelo remanejamento");
        em.flush(); em.clear();
        var eventosAntes = consultar(c.localA, base(id) + "/eventos", "CMP-007 estado antes da retificação de destino");
        var instrumentosAntes = consultar(c.localA, base(id) + "/remanejamentos", "Fotografia integral sintética do instrumento antes da recusa");
        assertThat(instrumentosAntes).hasSize(1);
        assertThat(instrumentosAntes.get(0)).isEqualTo(instrumento);
        var retificar = mapa("identificacao", "REM-CAPACIDADE-RETIFICADA", "dataEfetiva", efeito, "justificativa", "Destinar a C em vez de B",
                "confirmarImpacto", true, "eventoPrecedenteId", instrumento.path("eventoId").asLong(), "relacaoPrecedencia", "RETIFICA",
                "itens", List.of(mapa("composicaoId", a, "variacao", -1), mapa("composicaoId", destinoC, "variacao", 1)));
        previaRemanejamento(c, id, retificar);
        enviar(c.localA, base(id) + "/remanejamentos/" + instrumento.path("id").asLong() + "/retificar", retificar, 409,
                "CMP-006 retificação não pode deixar B sem capacidade quando o novo corpo só menciona A e C");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(id) + "/eventos", "CMP-007 eventos preservados após recusa")).isEqualTo(eventosAntes);
        assertThat(consultar(c.localA, base(id) + "/remanejamentos", "REM-004 recusa não cria versão parcial")).hasSize(1);
        assertThat(consultar(c.localA, base(id) + "/remanejamentos", "Releitura do instrumento original por ID, versão, itens e situação"))
                .isEqualTo(instrumentosAntes);
    }
}
