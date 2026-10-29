package com.example.siggtrat.seguranca;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para marcar endpoints que exigem confirmação de senha do usuário.
 *
 * Os métodos anotados serão interceptados pelo {@link InterceptadorExigirSenha}.
 * Em sessões comuns, o interceptor valida o header <code>X-User-Password</code>.
 * Quando a conta possui 2FA ativo e o segundo fator foi validado na sessão atual,
 * a autenticação forte da sessão substitui a redigitação da senha.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExigirSenha {
}