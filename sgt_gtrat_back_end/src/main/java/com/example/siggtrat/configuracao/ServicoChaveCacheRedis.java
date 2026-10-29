package com.example.siggtrat.configuracao;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;


@Component("redisCacheKey")
public class ServicoChaveCacheRedis {

    private final UsuarioAplicacaoRepositorio usuarioAplicacaoRepositorio;

    public ServicoChaveCacheRedis(UsuarioAplicacaoRepositorio usuarioAplicacaoRepositorio) {
        this.usuarioAplicacaoRepositorio = usuarioAplicacaoRepositorio;
    }

    public String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonimo";
        }

        String username = authentication.getName() == null
                ? "sem-usuario"
                : authentication.getName().trim().toLowerCase(Locale.ROOT);
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));

        UsuarioAplicacao usuario = usuarioAplicacaoRepositorio.findByUsername(authentication.getName()).orElse(null);
        String superintendenciaId = usuario != null && usuario.getSuperintendencia() != null
                ? String.valueOf(usuario.getSuperintendencia().getId())
                : "-";
        String gestaoLocalId = usuario != null && usuario.getGestaoLocal() != null
                ? String.valueOf(usuario.getGestaoLocal().getId())
                : "-";
        String escopoAtivo = usuario != null
                ? String.valueOf(Boolean.TRUE.equals(usuario.getActive()))
                : "-";

        return UtilitarioHashSeguranca.sha256Hexadecimal(
                username + "|" + authorities + "|" + superintendenciaId + "|" + gestaoLocalId + "|" + escopoAtivo);
    }

    public String valor(Object value) {
        return value == null ? "padrao" : String.valueOf(value);
    }


    public String consulta(String namespace, Object... valores) {
        String parametros = Arrays.stream(valores == null ? new Object[0] : valores)
                .map(this::canonico)
                .collect(Collectors.joining("|"));
        return usuarioAtual() + ":" + UtilitarioHashSeguranca.sha256Hexadecimal(canonico(namespace) + "|" + parametros);
    }


    public String painelEscopo(
            String nivel,
            Long superintendenciaId,
            Long gestaoLocalId,
            boolean permiteDetalhamentoOperacional,
            boolean podeVisualizarContratos,
            boolean podeVisualizarEmpregados,
            boolean podeOperarFrequencia,
            boolean podeAcessarRelatorios,
            LocalDate referencia,
            Integer periodoDias
    ) {
        return UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|",
                canonico(nivel), canonico(superintendenciaId), canonico(gestaoLocalId),
                canonico(permiteDetalhamentoOperacional), canonico(podeVisualizarContratos),
                canonico(podeVisualizarEmpregados), canonico(podeOperarFrequencia),
                canonico(podeAcessarRelatorios), canonico(referencia), canonico(periodoDias)));
    }

    private String canonico(Object value) {
        if (value == null) return "-";
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::canonico)
                    .sorted()
                    .collect(Collectors.joining(",", "[", "]"));
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[] objects) {
                return Arrays.stream(objects)
                        .map(this::canonico)
                        .sorted()
                        .collect(Collectors.joining(",", "[", "]"));
            }
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * O painel usa ontem como referência quando a API não recebe uma data.
     * Materializar essa data na chave impede que a chave "padrao" atravesse
     * a virada do dia e devolva o resultado da referência anterior.
     */
    public String dataReferenciaPainel(Object value) {
        if (value != null) {
            return String.valueOf(value);
        }
        return LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO))
                .minusDays(1)
                .toString();
    }
}
