package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.StatusEmpregado;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AlteracoesAuditoriaEmpregado {

    private AlteracoesAuditoriaEmpregado() {}

    public record EstadoEmpregado(
            Long id,
            String nome,
            String cpf,
            Lotacao lotacao,
            Jornada jornada,
            Cargo cargo,
            StatusEmpregado status
    ) {
        public boolean estaAtivo() {
            String s = (status != null && status.getDescricao() != null) ? status.getDescricao() : "";
            return !pareceInativo(s);
        }
    }

    public static Map<String, Object> construir(EstadoEmpregado before, EstadoEmpregado after) {
        List<Map<String, Object>> changes = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        final String mode = (before == null && after != null) ? ConstantesDominio.AUDIT_CREATE
                : (before != null && after == null) ? ConstantesDominio.AUDIT_DELETE
                  : ConstantesDominio.AUDIT_UPDATE;

        if (ConstantesDominio.AUDIT_CREATE.equals(mode)) messages.add("Registro cadastrado.");
        if (ConstantesDominio.AUDIT_DELETE.equals(mode)) messages.add("Registro removido.");

        Boolean oldActive = (before != null) ? before.estaAtivo() : null;
        Boolean newActive = (after != null) ? after.estaAtivo() : null;

        if (!Objects.equals(oldActive, newActive) && newActive != null) {
            changes.add(alteracaoBooleana("ativo", "Ativo", oldActive, newActive));
            if (oldActive == null) {
                messages.add(newActive ? "Colaborador foi cadastrado como ativo." : "Colaborador foi cadastrado como inativo.");
            } else {
                messages.add(newActive ? "Colaborador foi reativado." : "Colaborador ficou inativo." );
            }
        }

        // Status (descrição)
        if (!mesmoId(before != null ? before.status() : null, after != null ? after.status() : null)) {
            changes.add(alteracaoEntidade(
                    "status",
                    "Status",
                    objetoStatus(before != null ? before.status() : null),
                    objetoStatus(after != null ? after.status() : null)
            ));

            String from = nomeSeguro(before != null ? before.status() : null);
            String to = nomeSeguro(after != null ? after.status() : null);
            messages.add(mensagemEntidade(mode, "Status", from, to, false));
        }

        // Cargo
        if (!mesmoId(before != null ? before.cargo() : null, after != null ? after.cargo() : null)) {
            changes.add(alteracaoEntidade(
                    "cargo",
                    "Cargo",
                    objetoEntidadeNomeada(before != null ? before.cargo() : null),
                    objetoEntidadeNomeada(after != null ? after.cargo() : null)
            ));
            messages.add(mensagemEntidade(mode, "Cargo", nomeSeguro(before != null ? before.cargo() : null), nomeSeguro(after != null ? after.cargo() : null), false));
        }

        // Turno/Jornada
        if (!mesmoId(before != null ? before.jornada() : null, after != null ? after.jornada() : null)) {
            changes.add(alteracaoEntidade(
                    "jornada",
                    "Turno",
                    objetoJornada(before != null ? before.jornada() : null),
                    objetoJornada(after != null ? after.jornada() : null)
            ));
            messages.add(mensagemEntidade(mode, "Turno", nomeSeguro(before != null ? before.jornada() : null), nomeSeguro(after != null ? after.jornada() : null), false));
        }

        // Lotação
        if (!mesmoId(before != null ? before.lotacao() : null, after != null ? after.lotacao() : null)) {
            changes.add(alteracaoEntidade(
                    "lotacao",
                    "Lotação",
                    objetoEntidadeNomeada(before != null ? before.lotacao() : null),
                    objetoEntidadeNomeada(after != null ? after.lotacao() : null)
            ));
            messages.add(mensagemEntidade(mode, "Lotação", nomeSeguro(before != null ? before.lotacao() : null), nomeSeguro(after != null ? after.lotacao() : null), true));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("changes", changes);
        out.put("messages", messages);
        return out;
    }

    private static String mensagemEntidade(String mode, String label, String from, String to, boolean feminine) {
        from = nuloParaVazio(from);
        to = nuloParaVazio(to);
        if (ConstantesDominio.AUDIT_CREATE.equals(mode)) {
            if (!to.isBlank()) return String.format("%s definido%s: %s", label, feminine ? "a" : "o", vazioComoTraco(to));
            return String.format("%s definido%s.", label, feminine ? "a" : "o");
        }
        if (ConstantesDominio.AUDIT_DELETE.equals(mode)) {
            if (!from.isBlank()) return String.format("%s anterior: %s", label, vazioComoTraco(from));
            return String.format("%s anterior não informado.", label);
        }
        // UPDATE
        if (!from.isBlank() || !to.isBlank()) {
            return String.format("%s alterad%s: %s → %s", label, feminine ? "a" : "o", vazioComoTraco(from), vazioComoTraco(to));
        }
        return String.format("%s alterad%s.", label, feminine ? "a" : "o");
    }

    private static Map<String, Object> alteracaoBooleana(String field, String label, Boolean from, Boolean to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("label", label);
        m.put("kind", "BOOLEAN");
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private static Map<String, Object> alteracaoEntidade(String field, String label, Map<String, Object> from, Map<String, Object> to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("label", label);
        m.put("kind", "ENTITY");
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private static boolean mesmoId(Object a, Object b) {
        Long ida = idDe(a);
        Long idb = idDe(b);
        return Objects.equals(ida, idb);
    }

    private static Long idDe(Object o) {
        if (o == null) return null;
        if (o instanceof Cargo c) return c.getId();
        if (o instanceof Lotacao l) return l.getId();
        if (o instanceof Jornada j) return j.getId();
        if (o instanceof StatusEmpregado s) return s.getId();
        return null;
    }

    private static Map<String, Object> objetoEntidadeNomeada(Object o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", idDe(o));
        m.put("name", nomeSeguro(o));
        return m;
    }

    private static Map<String, Object> objetoJornada(Jornada j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j != null ? j.getId() : null);
        m.put("name", j != null ? nuloParaVazio(j.getDescricao()) : "");
        if (j != null) {
            if (j.getDias() != null && !j.getDias().isEmpty()) {
                List<Map<String, Object>> diasList = new ArrayList<>();
                for (JornadaDiaSemana dia : j.getDias()) {
                    Map<String, Object> dm = new LinkedHashMap<>();
                    dm.put("diaSemana", dia.getDiaSemana());
                    dm.put("diaTrabalho", dia.getDiaTrabalho());
                    dm.put("entrada1", dia.getEntrada1());
                    dm.put("saida1", dia.getSaida1());
                    dm.put("entrada2", dia.getEntrada2());
                    dm.put("saida2", dia.getSaida2());
                    diasList.add(dm);
                }
                m.put("dias", diasList);
            }
        }
        return m;
    }

    private static Map<String, Object> objetoStatus(StatusEmpregado s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s != null ? s.getId() : null);
        String name = (s != null) ? nuloParaVazio(s.getDescricao()) : "";
        m.put("name", name);
        m.put("active", !pareceInativo(name));
        return m;
    }

    private static String nomeSeguro(Object o) {
        if (o == null) return "";
        if (o instanceof Cargo c) return nuloParaVazio(c.getNome());
        if (o instanceof Lotacao l) return nuloParaVazio(l.getNome());
        if (o instanceof Jornada j) return nuloParaVazio(j.getDescricao());
        if (o instanceof StatusEmpregado s) return nuloParaVazio(s.getDescricao());
        return "";
    }

    private static String vazioComoTraco(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String nuloParaVazio(String s) {
        return s == null ? "" : s;
    }

    private static boolean pareceInativo(String statusDescricao) {
        if (statusDescricao == null) return false;
        String s = statusDescricao.toLowerCase(Locale.ROOT).trim();
        return s.contains("inat") || s.contains("deslig") || s.contains("demit") || s.contains("afast") || s.contains("susp") || s.contains("bloq");
    }
}