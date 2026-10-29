package com.example.siggtrat.auditoria;

import com.example.siggtrat.constante.ConstantesDominio;
import java.util.*;

public final class NarradorAlteracoesEmpregado {

    private NarradorAlteracoesEmpregado() {}

    public static List<String> paraFrasesAmigaveis(List<Map<String, Object>> changes) {
        if (changes == null || changes.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();

        for (Map<String, Object> c : changes) {
            if (c == null) continue;

            String rawType = comoTexto(c.get("type"));
            String type = rawType != null ? rawType.toUpperCase() : ConstantesDominio.AUDIT_TIPO_SIMPLE;
            String label = comoTexto(c.get("label"));

            if (ConstantesDominio.AUDIT_TIPO_SIMPLE.equals(type)) {
                String from = comoTexto(c.get("from"));
                String to = comoTexto(c.get("to"));

                if (estaVazio(from) && estaVazio(to)) continue;

                if (estaVazio(from)) {
                    out.add(label + " definido como '" + to + "'");
                } else if (estaVazio(to)) {
                    out.add(label + " removido (era '" + from + "')");
                } else {
                    out.add(label + " alterado de '" + from + "' para '" + to + "'");
                }
            }
            else if (ConstantesDominio.AUDIT_TIPO_REFERENCE.equals(type) || ConstantesDominio.AUDIT_TIPO_ENTITY.equals(type)) {
                String phrase = fraseAlteracaoReferencia(label, c);
                if (phrase != null) out.add(phrase);
            }
            else if (ConstantesDominio.AUDIT_TIPO_BOOLEAN.equals(type)) {
                Object valTo = c.get("to");
                Boolean bTo = extrairBooleano(valTo);

                if (bTo != null) {
                    if (ConstantesDominio.LABEL_ATIVO.equalsIgnoreCase(label)) {
                        out.add(bTo ? "Colaborador foi reativado" : "Colaborador foi desativado");
                    } else {
                        out.add(label + " alterado para " + (bTo ? "SIM" : "NÃO"));
                    }
                }
            }
        }

        return out;
    }

    private static String fraseAlteracaoReferencia(String label, Map<String, Object> change) {
        Map<String, Object> from = map(change.get("from"));
        Map<String, Object> to = map(change.get("to"));

        String fromLabel = from != null ? comoTexto(from.get("label")) : null;
        String toLabel = to != null ? comoTexto(to.get("label")) : null;

        if (fromLabel == null && from != null) fromLabel = comoTexto(from.get("name"));
        if (toLabel == null && to != null) toLabel = comoTexto(to.get("name"));

        if (!estaVazio(fromLabel) && !estaVazio(toLabel)) {
            if (fromLabel.equals(toLabel)) return null;
            return label + ": " + fromLabel + " → " + toLabel;
        }

        if (estaVazio(fromLabel) && !estaVazio(toLabel)) return label + " definido: " + toLabel;
        if (!estaVazio(fromLabel) && estaVazio(toLabel)) return label + " removido: " + fromLabel;

        return label + " alterado";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return null;
    }

    private static Boolean extrairBooleano(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Map<?,?> m) {
            Object v = m.get("value");
            if (v instanceof Boolean b) return b;
        }
        if (o instanceof String s) {
            return "true".equalsIgnoreCase(s) || "sim".equalsIgnoreCase(s);
        }
        return null;
    }

    private static String comoTexto(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean estaVazio(String s) {
        return s == null || s.isBlank();
    }
}