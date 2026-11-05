package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** A verificação de escopo é feita em cada rota, com JWT e cadeia de filtros reais. */
class ContratosEscopoHttpIT extends ContratosHttpBase {
    @Test
    void leiturasContratuaisValidamGestaoEmCadaSubrecurso() throws Exception {
        var c = criarCenario(); String b = base(c.contratoA.getId());
        var rotas = List.of(b, b + "/eventos", b + "/eventos/precedencias", b + "/versoes", b + "/transferencias-gestao",
                b + "/posicao-temporal?data=" + DATA, b + "/composicoes?data=" + DATA,
                b + "/composicoes/excedentes?data=" + DATA, b + "/aditamentos", b + "/remanejamentos",
                b + "/correcoes-identificador", b + "/correcoes-cnpj");
        for (String rota : rotas) {
            for (var u : List.of(c.localA, c.estadualA, c.nacional, c.presidente, c.admin, c.operacionalA))
                executar(u, get(rota), 200, "leitura contratual autorizada: " + rota + " / " + u.getPerfil());
            for (var u : List.of(c.localB, c.estadualB, c.operacionalB))
                executar(u, get(rota), 404, "subrecurso fora do escopo: " + rota + " / " + u.getPerfil());
            executar(null, get(rota), 401, "autenticação obrigatória no subrecurso: " + rota);
        }
    }

    @Test
    void todasAsMutacoesContratuaisExigemGestorEContratoDaGestao() throws Exception {
        var c = criarCenario(); String b = base(c.contratoA.getId());
        var corpo = mapa("dataEfetiva", DATA, "dataAto", DATA, "motivo", "Ato documentado", "referenciaSei", "SEI-ESCOPO",
                "dataPublicacao", DATA, "justificativa", "Justificativa documentada", "identificacao", "ESCOPO",
                "dataDocumento", DATA, "novaDescricao", "Descrição pretendida", "novoNumero", "NUM-ESCOPO", "novoAno", 2026,
                "autorizacaoAdministrativa", "ATO-ESCOPO", "chaveIdempotencia", "escopo-" + c.contratoA.getId(),
                "cnpjCorrigido", "98765432000198", "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "cargoId", c.cargoA.getId(), "efetivoPrevisto", 5);
        var rotas = List.of(new String[]{"PUT", b}, new String[]{"DELETE", b}, new String[]{"POST", b + "/publicar"},
                new String[]{"POST", b + "/previas/publicar"}, new String[]{"POST", b + "/inativar"},
                new String[]{"POST", b + "/reativar"}, new String[]{"POST", b + "/encerrar-antecipadamente"},
                new String[]{"POST", b + "/cancelar-futuro"}, new String[]{"POST", b + "/anular"},
                new String[]{"POST", b + "/corrigir-anulacao"}, new String[]{"POST", b + "/transferir-gestao"},
                new String[]{"POST", b + "/previas/inativar"}, new String[]{"POST", b + "/previas/reativar"},
                new String[]{"POST", b + "/previas/encerrar-antecipadamente"}, new String[]{"POST", b + "/previas/cancelar-futuro"},
                new String[]{"POST", b + "/previas/anular"}, new String[]{"POST", b + "/previas/corrigir-anulacao"},
                new String[]{"POST", b + "/composicoes"}, new String[]{"PUT", b + "/composicoes/" + c.composicaoA.getId()},
                new String[]{"DELETE", b + "/composicoes/" + c.composicaoA.getId()},
                new String[]{"POST", b + "/aditamentos/previa"}, new String[]{"POST", b + "/aditamentos"},
                new String[]{"POST", b + "/aditamentos/1/retificar"}, new String[]{"POST", b + "/aditamentos/1/cancelar"},
                new String[]{"POST", b + "/remanejamentos/previa"}, new String[]{"POST", b + "/remanejamentos"},
                new String[]{"POST", b + "/remanejamentos/1/retificar"}, new String[]{"POST", b + "/remanejamentos/1/cancelar"},
                new String[]{"POST", b + "/correcoes-identificador"}, new String[]{"POST", b + "/correcoes-cnpj"});
        for (var rota : rotas) {
            for (var u : List.of(c.operacionalA, c.estadualA, c.nacional, c.presidente, c.admin))
                executar(u, request(HttpMethod.valueOf(rota[0]), rota[1]).contentType(MediaType.APPLICATION_JSON).content(json(corpo)),
                        403, "escrita negocial exige Gestor: " + rota[1] + " / " + u.getPerfil());
            executar(c.localB, request(HttpMethod.valueOf(rota[0]), rota[1]).contentType(MediaType.APPLICATION_JSON).content(json(corpo)),
                    404, "escrita exige contrato da Gestão: " + rota[1]);
            executar(null, request(HttpMethod.valueOf(rota[0]), rota[1])
                            .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", "csrf-contratos-anonimo"))
                            .header("X-XSRF-TOKEN", "csrf-contratos-anonimo")
                            .contentType(MediaType.APPLICATION_JSON).content(json(corpo)),
                    401, "escrita exige autenticação: " + rota[1]);
        }
    }
}
