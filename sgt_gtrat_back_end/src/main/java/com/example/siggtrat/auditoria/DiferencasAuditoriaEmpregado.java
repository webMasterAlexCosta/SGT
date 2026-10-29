package com.example.siggtrat.auditoria;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class DiferencasAuditoriaEmpregado {

    private DiferencasAuditoriaEmpregado() {}

    public static final class EstadoEmpregado {
        public String nome, cpf, matricula, rg, ctpsSerie, nitPisPasep, email;
        public LocalDate dataNascimento, dataAdmissao;
        public String formacaoEscolar;
        public Boolean optanteFgts, optanteVa;
        public LocalDate dataOpcaoFgts;
        public BigDecimal valorVa;
        public Integer qtdDependenteIr, qtdDependenteSalFamilia, qtdBeneficiariosPensaoAlim;
        public String outrosBeneficios, observacao;
        public Long statusId; public String statusLabel;
        public Long cargoId; public String cargoLabel;
        public Long jornadaId; public String jornadaLabel;
        public Long lotacaoId; public String lotacaoLabel;
        public Boolean ativo;
        public String endLogradouro, endNumero, endBairro, endCidade, endUf, endCep;
        public String bancoNome, agenciaNumero, contaNumero, contaTipo;
        public String telefonesStr;
        public String ocorrenciasStr;
    }

    public static EstadoEmpregado capturarEstado(Empregado e) {
        EstadoEmpregado s = new EstadoEmpregado();
        if (e == null) return s;

        s.nome = e.getNome();
        s.cpf = e.getCpf();
        s.matricula = e.getMatricula();
        s.rg = e.getRg();
        s.ctpsSerie = e.getCtpsSerie();
        s.nitPisPasep = e.getNitPisPasep();
        s.email = e.getEmail();
        s.dataNascimento = e.getDataNascimento();
        s.dataAdmissao = e.getDataAdmissao();
        s.formacaoEscolar = e.getFormacaoEscolar();
        s.observacao = e.getObservacao();

        s.optanteFgts = e.getOptanteFgts();
        s.dataOpcaoFgts = e.getDataOpcaoFgts();
        s.optanteVa = e.getOptanteVa();
        s.valorVa = e.getValorVa();
        s.outrosBeneficios = e.getOutrosBeneficios();
        s.qtdDependenteIr = e.getQtdDependenteIr();
        s.qtdDependenteSalFamilia = e.getQtdDependenteSalFamilia();
        s.qtdBeneficiariosPensaoAlim = e.getQtdBeneficiariosPensaoAlim();

        s.statusId = id(e.getStatus()); s.statusLabel = label(e.getStatus());
        s.cargoId = id(e.getCargo()); s.cargoLabel = label(e.getCargo());
        s.jornadaId = id(e.getJornada()); s.jornadaLabel = label(e.getJornada());
        s.lotacaoId = id(e.getLotacao()); s.lotacaoLabel = label(e.getLotacao());
        s.ativo = estaAtivo(e.getStatus());

        if (e.getEndereco() != null) {
            s.endLogradouro = e.getEndereco().getLogradouro();
            s.endNumero = e.getEndereco().getNumero();
            s.endBairro = e.getEndereco().getBairro();
            s.endCidade = e.getEndereco().getCidade();
            s.endUf = e.getEndereco().getUf();
            s.endCep = e.getEndereco().getCep();
        }

        if (e.getConta() != null) {
            s.contaNumero = e.getConta().getNumeroConta();
            if (e.getConta().getAgencia() != null) {
                s.agenciaNumero = e.getConta().getAgencia().getNumero();
                if (e.getConta().getAgencia().getBanco() != null) {
                    s.bancoNome = e.getConta().getAgencia().getBanco().getNome();
                }
            }
            if (e.getConta().getTipoConta() != null) {
                s.contaTipo = e.getConta().getTipoConta().getDescricao();
            }
        }

        if (e.getTelefones() != null) {
            s.telefonesStr = e.getTelefones().stream()
                    .map(t -> t.getNumero() != null ? t.getNumero().replaceAll("\\D", "") : "")
                    .filter(str -> !str.isBlank())
                    .sorted()
                    .collect(Collectors.joining(", "));
        }

        if (e.getOcorrencias() != null) {
            s.ocorrenciasStr = e.getOcorrencias().stream()
                    .map(o -> {
                        String dt = o.getDataOcorrencia() != null ? o.getDataOcorrencia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "?";
                        String motivo = o.getMotivo() != null ? o.getMotivo() : "S/M";
                        Integer dias = o.getQuantidadeDias();
                        if (dias == null) dias = o.getDiasSuspensao();
                        if (dias == null) dias = o.getDiasAfastamento();
                        return "[" + dt + "] " + motivo + (dias != null && dias > 0 ? " (" + dias + "d)" : "");
                    })
                    .sorted()
                    .collect(Collectors.joining(", "));
        }

        return s;
    }

    public static List<Map<String, Object>> calcularDiferencas(EstadoEmpregado before, Empregado after) {
        EstadoEmpregado b = (before != null) ? before : new EstadoEmpregado();
        EstadoEmpregado a = capturarEstado(after);
        List<Map<String, Object>> changes = new ArrayList<>();

        verificarAlteracao(changes, "nome", "Nome", b.nome, a.nome);
        verificarAlteracao(changes, "cpf", "CPF", b.cpf, a.cpf);
        verificarAlteracao(changes, "matricula", "Matrícula", b.matricula, a.matricula);
        verificarAlteracao(changes, "rg", "RG", b.rg, a.rg);
        verificarAlteracao(changes, "ctps", "CTPS/Série", b.ctpsSerie, a.ctpsSerie);
        verificarAlteracao(changes, "pis", "PIS/PASEP", b.nitPisPasep, a.nitPisPasep);
        verificarAlteracao(changes, "email", "E-mail", b.email, a.email);
        verificarData(changes, "dataNascimento", "Data Nascimento", b.dataNascimento, a.dataNascimento);
        verificarData(changes, "dataAdmissao", "Data Admissão", b.dataAdmissao, a.dataAdmissao);
        verificarAlteracao(changes, "escolaridade", "Escolaridade", b.formacaoEscolar, a.formacaoEscolar);

        verificarBooleano(changes, "optanteFgts", "Optante FGTS", b.optanteFgts, a.optanteFgts);
        verificarData(changes, "dataOpcaoFgts", "Data Opção FGTS", b.dataOpcaoFgts, a.dataOpcaoFgts);
        verificarBooleano(changes, "optanteVa", "Optante VA", b.optanteVa, a.optanteVa);
        verificarDecimal(changes, "valorVa", "Valor VA", b.valorVa, a.valorVa);
        verificarAlteracao(changes, "outrosBeneficios", "Outros Benefícios", b.outrosBeneficios, a.outrosBeneficios);
        verificarInteiro(changes, "dependenteIr", "Dep. IR", b.qtdDependenteIr, a.qtdDependenteIr);
        verificarInteiro(changes, "dependenteSf", "Dep. Sal. Família", b.qtdDependenteSalFamilia, a.qtdDependenteSalFamilia);
        verificarInteiro(changes, "benefPensao", "Benef. Pensão", b.qtdBeneficiariosPensaoAlim, a.qtdBeneficiariosPensaoAlim);
        verificarAlteracao(changes, "observacao", "Observação", b.observacao, a.observacao);

        verificarAlteracao(changes, "logradouro", "Logradouro", b.endLogradouro, a.endLogradouro);
        verificarAlteracao(changes, "numero", "Número (End)", b.endNumero, a.endNumero);
        verificarAlteracao(changes, "bairro", "Bairro", b.endBairro, a.endBairro);
        verificarAlteracao(changes, "cidade", "Cidade", b.endCidade, a.endCidade);
        verificarAlteracao(changes, "uf", "UF", b.endUf, a.endUf);
        verificarAlteracao(changes, "cep", "CEP", b.endCep, a.endCep);

        verificarAlteracao(changes, "banco", "Banco", b.bancoNome, a.bancoNome);
        verificarAlteracao(changes, "agencia", "Agência", b.agenciaNumero, a.agenciaNumero);
        verificarAlteracao(changes, "conta", "Conta", b.contaNumero, a.contaNumero);
        verificarAlteracao(changes, "tipoConta", "Tipo Conta", b.contaTipo, a.contaTipo);

        verificarAlteracao(changes, "telefone", "Telefones", b.telefonesStr, a.telefonesStr);
        verificarAlteracao(changes, "ocorrencias", "Ocorrências", b.ocorrenciasStr, a.ocorrenciasStr);

        verificarReferencia(changes, "status", "Status", b.statusId, b.statusLabel, a.statusId, a.statusLabel);
        verificarReferencia(changes, "cargo", "Cargo", b.cargoId, b.cargoLabel, a.cargoId, a.cargoLabel);
        verificarReferencia(changes, "lotacao", "Lotação", b.lotacaoId, b.lotacaoLabel, a.lotacaoId, a.lotacaoLabel);
        verificarReferencia(changes, "jornada", "Turno", b.jornadaId, b.jornadaLabel, a.jornadaId, a.jornadaLabel);
        verificarBooleano(changes, "ativo", "Ativo", b.ativo, a.ativo);

        return changes;
    }

    public static List<Map<String, Object>> criado(Empregado after) {
        return calcularDiferencas(new EstadoEmpregado(), after);
    }

    public static List<Map<String, Object>> excluido(Empregado before) {
        EstadoEmpregado b = capturarEstado(before);
        List<Map<String, Object>> changes = new ArrayList<>();
        changes.add(alteracaoSimples("cadastro", "Cadastro", b.nome, "REMOVIDO"));
        return changes;
    }

    private static void verificarAlteracao(List<Map<String, Object>> list, String key, String label, String v1, String v2) {
        String nv1 = normalizarTexto(v1);
        String nv2 = normalizarTexto(v2);
        if (Objects.equals(nv1, nv2)) return;
        list.add(alteracaoSimples(key, label, v1, v2));
    }

    private static String normalizarTexto(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void verificarData(List<Map<String, Object>> list, String key, String label, LocalDate v1, LocalDate v2) {
        if (!Objects.equals(v1, v2)) list.add(alteracaoSimples(key, label, formatarData(v1), formatarData(v2)));
    }
    private static void verificarInteiro(List<Map<String, Object>> list, String key, String label, Integer v1, Integer v2) {
        if (!Objects.equals(v1, v2)) list.add(alteracaoSimples(key, label, v1, v2));
    }
    private static void verificarDecimal(List<Map<String, Object>> list, String key, String label, BigDecimal v1, BigDecimal v2) {
        boolean eq = (v1 == null && v2 == null) || (v1 != null && v2 != null && v1.compareTo(v2) == 0);
        if (!eq) list.add(alteracaoSimples(key, label, v1, v2));
    }
    private static void verificarBooleano(List<Map<String, Object>> list, String key, String label, Boolean v1, Boolean v2) {
        if (!Objects.equals(v1, v2)) list.add(alteracaoSimples(key, label, booleanoComoTexto(v1), booleanoComoTexto(v2)));
    }
    private static void verificarReferencia(List<Map<String, Object>> list, String field, String label, Long id1, String l1, Long id2, String l2) {
        if (!Objects.equals(id1, id2) || !Objects.equals(l1, l2)) {
            list.add(alteracaoReferenciaMapeada(field, label, id1, l1, id2, l2));
        }
    }

    private static String formatarData(LocalDate d) { return d == null ? "" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    private static String booleanoComoTexto(Boolean b) { return b == null ? "" : (b ? "SIM" : "NÃO"); }

    private static Map<String, Object> alteracaoSimples(String key, String label, Object from, Object to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", ConstantesDominio.AUDIT_TIPO_SIMPLE);
        m.put("field", key);
        m.put("label", label);
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private static Map<String, Object> alteracaoReferenciaMapeada(String field, String label, Long id1, String l1, Long id2, String l2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", ConstantesDominio.AUDIT_TIPO_REFERENCE);
        m.put("field", field);
        m.put("label", label);
        m.put("from", referencia(id1, l1));
        m.put("to", referencia(id2, l2));
        return m;
    }

    private static Map<String, Object> referencia(Long id, String label) {
        if (id == null && (label == null || label.isBlank())) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        if (id != null) m.put("id", id);
        if (label != null) m.put("label", label);
        return m;
    }

    private static boolean estaAtivo(StatusEmpregado s) {
        return s != null && ConstantesDominio.STATUS_ATIVO.equalsIgnoreCase(s.getDescricao());
    }

    private static Long id(Object o) {
        if (o instanceof Cargo c) return c.getId();
        if (o instanceof Lotacao l) return l.getId();
        if (o instanceof Jornada j) return j.getId();
        if (o instanceof StatusEmpregado s) return s.getId();
        return null;
    }

    private static String label(Object o) {
        if (o instanceof Cargo c) return c.getNome();
        if (o instanceof Lotacao l) return l.getNome();
        if (o instanceof Jornada j) return j.getDescricao();
        if (o instanceof StatusEmpregado s) return s.getDescricao();
        return null;
    }
}