package com.example.siggtrat.seguranca;

import com.example.siggtrat.excecao.ExcecaoSenhaInvalida;
import com.example.siggtrat.excecao.ExcecaoSenhaAusente;
import com.example.siggtrat.excecao.ExcecaoUsuarioNaoAutenticado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor responsável pela confirmação adicional dos métodos anotados com {@link ExigirSenha}.
 *
 * Regra de segurança:
 * - sessão comum ou sessão que entrou sem validar o 2FA: exige X-User-Password;
 * - conta com 2FA ativo E 2FA validado na sessão atual: a autenticação forte da sessão
 *   substitui a redigitação da senha para endpoints marcados com {@link ExigirSenha}.
 *
 * O atributo "twoFactorVerified" é definido pelo {@link FiltroAutenticacaoJwt} a partir
 * de um claim assinado no JWT, portanto não é controlado pelo cliente.
 */
@Component
public class InterceptadorExigirSenha implements HandlerInterceptor {

    private final UsuarioAplicacaoRepositorio userRepositorio;
    private final PasswordEncoder passwordEncoder;

    public InterceptadorExigirSenha(UsuarioAplicacaoRepositorio userRepositorio, PasswordEncoder passwordEncoder) {
        this.userRepositorio = userRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        ExigirSenha annotation = hm.getMethodAnnotation(ExigirSenha.class);
        if (annotation == null) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ExcecaoUsuarioNaoAutenticado("A sessão não está autenticada.");
        }

        UsuarioAplicacao user = null;
        boolean twoFactorVerified = Boolean.TRUE.equals(request.getAttribute("twoFactorVerified"));
        if (twoFactorVerified) {
            user = userRepositorio.findByUsername(auth.getName()).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                return true;
            }
        }

        String providedPassword = request.getHeader("X-User-Password");
        if (providedPassword == null || providedPassword.isBlank()) {
            throw new ExcecaoSenhaAusente("A senha de confirmação é obrigatória para esta operação.");
        }
        if (providedPassword.length() > 72) {
            throw new ExcecaoSenhaInvalida("A senha de confirmação informada é inválida.");
        }

        if (user == null) {
            user = userRepositorio.findByUsername(auth.getName()).orElse(null);
        }
        if (user == null) {
            throw new ExcecaoUsuarioNaoAutenticado("O usuário autenticado não foi encontrado.");
        }

        boolean senhaValida = false;
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            try {
                senhaValida = passwordEncoder.matches(providedPassword, user.getPassword());
            } catch (RuntimeException ignored) {
                senhaValida = false;
            }
        }
        if (!senhaValida) {
            throw new ExcecaoSenhaInvalida("A senha informada não confere com o usuário autenticado.");
        }
        return true;
    }
}
