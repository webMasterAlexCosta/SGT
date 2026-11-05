package com.example.siggtrat.auditoria20261029;

import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** Inventário do contexto real; os testes consumidores devem exercitar todas as rotas devolvidas. */
final class InventarioRotasPrivadas {
    private InventarioRotasPrivadas() {}

    static List<String> coletarEConferir(RequestMappingHandlerMapping mappings, Set<String> publicas) {
        List<String> inventario=new ArrayList<>();
        Set<String> registradas=new TreeSet<>();
        mappings.getHandlerMethods().forEach((info,handler)->{
            if(!handler.getBeanType().getPackageName().startsWith("com.example.siggtrat.controlador"))return;
            assertFalse(info.getPatternValues().isEmpty(), "Handler sem caminho inventariado: "+handler);
            assertFalse(info.getMethodsCondition().getMethods().isEmpty(),
                    "Handler sem método HTTP explícito não pode ser omitido do gate: "+handler);
            for(String path:info.getPatternValues())for(var method:info.getMethodsCondition().getMethods()) {
                String rota=method.name()+" "+path;
                registradas.add(rota);
                if(!publicas.contains(rota))inventario.add(rota);
            }
        });
        Set<String> privadasEsperadas=new TreeSet<>(registradas);
        privadasEsperadas.removeAll(publicas);
        assertEquals(privadasEsperadas,new TreeSet<>(inventario),
                "O inventário deve conter todas as combinações privadas do registro Spring");
        assertTrue(registradas.containsAll(publicas), "As exceções públicas existentes devem continuar registradas");
        assertTrue(Collections.disjoint(inventario,publicas), "Nenhuma exceção pública deve entrar no gate privado");
        for(String essencial:Set.of(
                "GET /api/contratos", "POST /api/contratos", "GET /api/contratos/{id}/eventos",
                "GET /api/fornecedores", "POST /api/fornecedores", "PUT /api/fornecedores/{id}",
                "PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}",
                "GET /api/pessoas", "GET /api/empregados", "GET /api/vinculos-contratuais",
                "POST /api/vinculos-contratuais", "GET /api/frequencias", "POST /api/frequencias",
                "POST /api/frequencias/lote", "GET /api/registros-auditoria",
                "GET /api/contratos/{contratoId}/documentos",
                "GET /api/contratos/{contratoId}/documentos/{documentoId}/arquivo",
                "GET /api/relatorios/frequencia-consolidada", "GET /api/relatorios/frequencia-consolidada/exportacao",
                "GET /api/autenticacao/sessao", "POST /api/autenticacao/renovar-sessao",
                "POST /api/autenticacao/alterar-senha-inicial")) {
            assertTrue(inventario.contains(essencial), "Endpoint operacional/autenticação privado preservado: "+essencial);
        }
        assertTrue(mappings.getHandlerMethods().keySet().stream().flatMap(info->info.getPatternValues().stream())
                .noneMatch(path->path.startsWith("/api/governanca-terceiros")),
                "O prefixo retirado não pode permanecer registrado, mesmo sem navegação");
        Collections.sort(inventario);
        return List.copyOf(inventario);
    }
}
