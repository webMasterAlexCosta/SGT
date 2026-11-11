package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

/** RED estável: compila contra a fonte anterior; só usa JSON e entidades/getters que já existem antes64. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaConferenciaAto64RedHttpIT extends TransferenciaConferencia64HttpBase {
    @Test
    void concessaoNaoSubstituiConferenciaAusenteFalsaOuVersaoDiferente() throws Exception {
        for (String caso : java.util.List.of("ausente", "falsa", "versao-diferente")) {
            var f = preparar64(true); var req = intencao64(f, f.n().inicio().plusDays(2));
            if (!"ausente".equals(caso)) {
                req.put("conferenciaDocumentalConfirmada", !"falsa".equals(caso));
                req.put("versaoConferenciaDocumental", "versao-diferente".equals(caso) ? "v0-sintetica" : VERSAO);
            }
            var antes = identidades64(f);
            enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 400,
                    "64RED " + caso + ": capacidade genérica e ATO não são conferência específica");
            assertEquals(antes, identidades64(f), "Nova transação: nenhuma identidade de fato, transferência ou uso foi criada");
            declarar64(req);
            enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200,
                    "64 controle positivo da MESMA intenção com declaração sintética explícita");
        }
    }
    @Test
    void referenciaDoAtoNaoEhSobrescritaPelaReferenciaDaConcessao() throws Exception {
        var f = preparar64(true); var req = intencao64(f, f.n().inicio().plusDays(2)); declarar64(req);
        req.put("autorizacaoAdministrativa", "ATO-SINTETICO-64-ESPECIFICO");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200,
                "64RED referência específica do ato pode diferir de ATO-HTTP da capacidade; não comprova teor externo");
        emTransacao(() -> {
            var t = em.find(com.example.siggtrat.entidade.TransferenciaGestaoContrato.class, transferencia64(f));
            assertEquals("ATO-SINTETICO-64-ESPECIFICO", t.getAutorizacaoAdministrativa());
            var uso = em.createQuery("select u from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato'",
                    com.example.siggtrat.entidade.UsoPermissaoExcepcional.class).setParameter("c", f.n().id()).getSingleResult();
            assertEquals("ATO-HTTP", uso.getPermissao().getAutorizacaoAdministrativa()); return null;
        });
    }
    @Test
    void cancelarExigeConferenciaNovaSemHerdarDaTransferencia() throws Exception {
        var f = preparar64(true); var data = f.n().inicio().plusDays(2);
        var req = intencao64(f, data); declarar64(req);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200, "64 fixture de transferência conferida");
        var cancelar = mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "motivo", "Cancelamento sintético distinto", "referenciaSei", "SEI-SINTETICO-64-CANCELAR",
                "autorizacaoAdministrativa", "ATO-HTTP");
        String rota = base(f.n().id()) + "/transferencias-gestao/" + transferencia64(f) + "/cancelar";
        var antes = identidades64(f);
        enviar(f.c().localA, rota, cancelar, 400, "64RED cancelamento é outro ato; conferência anterior não é herdada");
        assertEquals(antes, identidades64(f));
        declarar64(cancelar);
        enviar(f.c().localA, rota, cancelar, 200, "64 controle positivo: nova declaração sintética do cancelamento");
    }
}
