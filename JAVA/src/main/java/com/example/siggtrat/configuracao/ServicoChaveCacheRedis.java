package com.example.siggtrat.configuracao;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final com.example.siggtrat.servico.RevisaoFontesPainelServico revisaoFontes;

    public ServicoChaveCacheRedis(UsuarioAplicacaoRepositorio usuarioAplicacaoRepositorio,
                                  com.example.siggtrat.servico.RevisaoFontesPainelServico revisaoFontes) {
        this.usuarioAplicacaoRepositorio = usuarioAplicacaoRepositorio;
        this.revisaoFontes = revisaoFontes;
    }

    @Transactional(readOnly = true)
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
        String perfil = usuario == null ? "-" : String.valueOf(usuario.getPerfil());
        String lotacaoId = usuario != null && usuario.getLotacao() != null
                ? String.valueOf(usuario.getLotacao().getId()) : "-";
        String jornadaId = usuario != null && usuario.getJornada() != null
                ? String.valueOf(usuario.getJornada().getId()) : "-";
        // A sessão pode continuar válida depois de uma alteração cadastral de
        // escopo. A chave deve refletir as permissões persistidas nesta leitura,
        // inclusive vínculos adicionais que não alteram a versão do usuário.
        String vinculos = usuario == null ? "-" : usuario.getVinculosGestao().stream()
                .map(v -> String.join(":",
                        v.getLotacao() == null ? "-" : String.valueOf(v.getLotacao().getId()),
                        v.getJornada() == null ? "-" : String.valueOf(v.getJornada().getId()),
                        v.getLotacao() == null || v.getLotacao().getGestaoLocal() == null ? "-"
                                : String.valueOf(v.getLotacao().getGestaoLocal().getId()),
                        v.getJornada() == null || v.getJornada().getGestaoLocal() == null ? "-"
                                : String.valueOf(v.getJornada().getGestaoLocal().getId())))
                .sorted().collect(Collectors.joining(","));

        return UtilitarioHashSeguranca.sha256Hexadecimal(
                revisaoFontes.atual() + "|" + username + "|" + authorities + "|" + perfil + "|" + superintendenciaId + "|"
                        + gestaoLocalId + "|" + escopoAtivo + "|" + lotacaoId + "|" + jornadaId + "|" + vinculos);
    }

    public String valor(Object value) {
        return value == null ? "padrao" : String.valueOf(value);
    }


    @Transactional(readOnly = true)
    public String consulta(String namespace, Object... valores) {
        String parametros = Arrays.stream(valores == null ? new Object[0] : valores)
                .map(this::canonico)
                .collect(Collectors.joining("|"));
        // Autorizações e estados derivados mudam na virada do dia sem qualquer
        // mutação que dispare invalidação: transferência programada, vigência,
        // alocação e fechamento no oitavo dia. A data negocial deve integrar
        // tanto o cache HTTP quanto os caches internos, mesmo quando o período
        // histórico consultado é explícito e permanece igual.
        return usuarioAtual() + ":" + UtilitarioHashSeguranca.sha256Hexadecimal(
                canonico(namespace) + "|dia-negocial=" + RelogioSistema.hoje() + "|" + parametros);
    }


    @Transactional(readOnly = true)
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
                revisaoFontes.atual(), canonico(nivel), canonico(superintendenciaId), canonico(gestaoLocalId),
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
