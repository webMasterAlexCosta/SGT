package com.example.siggtrat.configuracao;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.example.siggtrat.excecao.ExcecaoSenhaAusente;
import com.example.siggtrat.excecao.ExcecaoSenhaInvalida;
import com.example.siggtrat.excecao.ExcecaoConflitoDados;
import com.example.siggtrat.excecao.ExcecaoUsuarioNaoAutenticado;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class TratadorGlobalExcecoes {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TratadorGlobalExcecoes.class);

    private static final Pattern POSTGRES_NOT_NULL_COLUMN = Pattern.compile("null value in column \"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarArgumentoIlegal(IllegalArgumentException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarArgumentoIlegal", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "code", "INVALID_ARGUMENT",
                        "message", ex instanceof com.example.siggtrat.excecao.ExcecaoValidacaoNegocio
                                ? com.example.siggtrat.diagnostico.DiagnosticoFalha.mensagemPublica(ex.getMessage(), "Verifique os campos da requisição.")
                                : "Um parâmetro da requisição é inválido. Verifique os campos e formatos informados."
                ));
    }


    @ExceptionHandler(ExcecaoConflitoDados.class)
    public ResponseEntity<Map<String, Object>> tratarConflitoDados(ExcecaoConflitoDados ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarConflitoDados", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.CONFLICT.value(),
                        "error", "Conflict",
                        "code", "DATA_CONFLICT",
                        "message", ex.getMessage() != null ? ex.getMessage() : "A operação conflita com um registro existente."
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> tratarStatusResposta(ResponseStatusException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarStatusResposta", ex);
        int status = ex.getStatusCode().value();
        String reason = ex.getReason();
        String message = status >= 500 ? "Não foi possível concluir esta operação. Informe o identificador da requisição ao suporte."
                : com.example.siggtrat.diagnostico.DiagnosticoFalha.mensagemPublica(reason, "A operação foi recusada. Verifique os dados e sua permissão.");

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status,
                        "error", ex.getStatusCode().toString(),
                        "code", "HTTP_" + status,
                        "message", message
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarJsonInvalido", ex);
        String detalhe = Optional.ofNullable(ex.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse("");

        String mensagem = detalhe.contains("Cannot map `null` into type")
                ? "Um campo interno foi enviado com valor nulo em formato incompatível. Atualize a tela e tente novamente."
                : "O corpo da requisição está inválido. Verifique datas, números e campos obrigatórios.";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "code", "INVALID_REQUEST_BODY",
                        "message", mensagem
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarValidacao", ex);
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", Optional.ofNullable(err.getDefaultMessage()).orElse("Inválido")
                ))
                .toList();

        String summary = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse("Erro de validação");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "code", "VALIDATION_ERROR",
                        "message", summary,
                        "fields", fields
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarViolacaoRestricao(ConstraintViolationException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarViolacaoRestricao", ex);
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", String.valueOf(v.getPropertyPath()),
                        "message", Optional.ofNullable(v.getMessage()).orElse("Inválido")
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "code", "VALIDATION_ERROR",
                        "message", "Erro de validação",
                        "fields", violations
                ));
    }

    private static String nomeRestricaoHibernate(Throwable causa) {
        var visitadas = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        for (int profundidade = 0; causa != null && profundidade < 16 && visitadas.add(causa);
                profundidade++, causa = causa.getCause()) {
            if (causa instanceof org.hibernate.exception.ConstraintViolationException violacao) {
                return violacao.getConstraintName();
            }
        }
        return null;
    }

    private static String estadoSql(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql) return sql.getSQLState();
            if (causa.getCause() == causa) break;
        }
        return null;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarIntegridade(DataIntegrityViolationException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarIntegridade", ex);
        // Útil para capturar violações de FK/unique e devolver feedback mais claro.
        // Observação: "cannot be null" é erro de validação (400), e não conflito (409).
        String mostSpecific = Optional.ofNullable(ex.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse("");

        String restricao = nomeRestricaoHibernate(ex);
        if ("23505".equals(estadoSql(ex)) && "uk_fornecedor_cnpj".equals(restricao)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", 409,
                            "error", "Conflict",
                            "code", "CNPJ_DUPLICADO",
                            "message", "Já existe uma empresa cadastrada com este CNPJ. Verifique o cadastro existente."
                    ));
        }
        if ("23505".equals(estadoSql(ex)) && "uk_frequencia_empregado_data".equals(restricao)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", 409,
                            "error", "Conflict",
                            "code", "FREQUENCIA_DUPLICADA",
                            "message", "Outra requisição gravou esta frequência ao mesmo tempo. Recarregue os dados e tente novamente."
                    ));
        }

        Matcher m = POSTGRES_NOT_NULL_COLUMN.matcher(mostSpecific);
        if ("23502".equals(estadoSql(ex))) {
            String mensagem = m.find() ? "Campo obrigatório não informado: " + m.group(1)
                    : "Campo obrigatório não informado.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", 400,
                            "error", "Bad Request",
                            "message", mensagem
                    ));
        }

        // Mantém 409 para cenários típicos (FK/unique etc.)
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 409,
                        "error", "Conflict",
                        "message", "Operação não permitida: existe conflito de integridade para este registro."
                ));
    }

    @ExceptionHandler({org.springframework.dao.PessimisticLockingFailureException.class,
            jakarta.persistence.LockTimeoutException.class, jakarta.persistence.PessimisticLockException.class})
    public ResponseEntity<Map<String, Object>> tratarDisputaConcorrente(Exception ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarDisputaConcorrente", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now().toString(), "status", 409, "error", "Conflict",
                "code", "CONCURRENT_OPERATION",
                "message", "Outra operação concorreu com esta alteração. Recarregue os dados e repita a intenção."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> tratarBloqueioOtimista(ObjectOptimisticLockingFailureException ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarBloqueioOtimista", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 409,
                        "error", "Conflict",
                        "message", "Registro foi alterado por outro usuário. Recarregue os dados e tente novamente."
                ));
    }

    /**
     * Trata erros quando a confirmação de senha não foi fornecida em um endpoint protegido.
     * Retorna 428 para indicar que a confirmação precisa acompanhar a requisição protegida.
     */
    @ExceptionHandler(ExcecaoSenhaAusente.class)
    public ResponseEntity<Map<String, Object>> tratarSenhaAusente(ExcecaoSenhaAusente ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarSenhaAusente", ex);
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.PRECONDITION_REQUIRED.value(),
                        "error", "Precondition Required",
                        "code", "PASSWORD_CONFIRMATION_REQUIRED",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Confirmação de senha obrigatória."
                ));
    }

    /**
     * Trata erros de autenticação quando a senha fornecida está incorreta.
     * Retorna 403 sem encerrar a sessão, pois somente a confirmação da operação falhou.
     */
    @ExceptionHandler(ExcecaoSenhaInvalida.class)
    public ResponseEntity<Map<String, Object>> tratarSenhaInvalida(ExcecaoSenhaInvalida ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarSenhaInvalida", ex);
        // A sessão continua válida. O 403 diferencia a confirmação incorreta de uma
        // expiração de sessão (401), sem mascarar o problema como Bad Request genérico.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "code", "INVALID_CONFIRMATION_PASSWORD",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Senha de confirmação inválida."
                ));
    }

    /**
     * Trata casos em que o usuário não está autenticado ao acessar um endpoint protegido.
     * Retorna um status 401 (Unauthorized) com mensagem apropriada.
     */
    @ExceptionHandler(ExcecaoUsuarioNaoAutenticado.class)
    public ResponseEntity<Map<String, Object>> tratarUsuarioNaoAutenticado(ExcecaoUsuarioNaoAutenticado ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarUsuarioNaoAutenticado", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 401,
                        "error", "Unauthorized",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Não autenticado."
                ));
    }
    @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tratarConversao(org.springframework.beans.TypeMismatchException ex) {
        // Ausência de conversor é falha da configuração do servidor, não entrada malformada.
        if (ex instanceof org.springframework.beans.ConversionNotSupportedException) return tratarNaoEsperada(ex);
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarConversao", ex);
        String nome = ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException argumento
                ? argumento.getName() : ex.getPropertyName();
        if (nome == null || !nome.matches("[A-Za-z0-9_.\\[\\]-]{1,80}")) nome = "parâmetro";
        Class<?> tipo = ex.getRequiredType();
        String esperado = "formato válido para este campo";
        if (tipo != null) {
            if (tipo.isEnum()) esperado = "uma das opções permitidas";
            else if (tipo == Boolean.class || tipo == boolean.class) esperado = "valor verdadeiro ou falso";
            else if (Number.class.isAssignableFrom(tipo) || tipo.isPrimitive() && tipo != char.class) esperado = "número válido";
            else if (java.time.temporal.Temporal.class.isAssignableFrom(tipo)) esperado = "data ou horário válido conforme o formato do campo";
        }
        String mensagem = "Parâmetro '" + nome + "' inválido: informe " + esperado + ".";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(), "status", 400, "error", "Bad Request",
                "code", "INVALID_PARAMETER", "message", mensagem,
                "fields", List.of(Map.of("field", nome, "message", esperado))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEsperada(Exception ex) {
        com.example.siggtrat.diagnostico.DiagnosticoFalha.registrar(LOG, "tratarNaoEsperada", ex);
        // Erros do protocolo MVC conservam seu status (404/405/413 etc.).
        if (ex instanceof org.springframework.web.ErrorResponse resposta) {
            int status = resposta.getStatusCode().value();
            var http = org.springframework.http.HttpStatus.resolve(status);
            String mensagem = switch (status) {
                case 400 -> "Os parâmetros da requisição estão incompletos ou em formato inválido.";
                case 404 -> "O recurso solicitado não foi encontrado.";
                case 405 -> "O método HTTP não é permitido para este recurso.";
                case 413 -> "O arquivo enviado excede o limite permitido.";
                case 415 -> "O formato enviado não é aceito por este recurso.";
                default -> "Não foi possível processar esta requisição. Informe seu identificador ao suporte.";
            };
            return ResponseEntity.status(resposta.getStatusCode()).headers(resposta.getHeaders()).body(Map.of(
                    "timestamp", Instant.now().toString(), "status", status,
                    "error", http == null ? "HTTP Error" : http.getReasonPhrase(),
                    "code", "HTTP_" + status, "message", mensagem));
        }
        if (ex instanceof org.springframework.security.core.AuthenticationException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "timestamp", Instant.now().toString(), "status", 401, "error", "Unauthorized",
                    "code", "AUTHENTICATION_REQUIRED", "message", "Autentique-se para realizar esta operação."));
        }
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "timestamp", Instant.now().toString(), "status", 403, "error", "Forbidden",
                    "code", "ACCESS_DENIED", "message", "Você não possui permissão para esta operação."));
        }
        String code = "INTERNAL_OPERATION_FAILED";
        String message = "Não foi possível concluir esta operação. Informe o identificador da requisição ao suporte.";
        if (ex instanceof org.springframework.dao.DataAccessException || ex instanceof jakarta.persistence.PersistenceException) {
            code = "DATABASE_OPERATION_FAILED";
            message = "Não foi possível acessar os dados desta operação. Tente novamente; se persistir, informe o identificador ao suporte.";
        } else if (ex instanceof java.io.IOException) {
            code = "FILE_OPERATION_FAILED";
            message = "Não foi possível acessar o arquivo desta operação. Informe o identificador da requisição ao suporte.";
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", Instant.now().toString(), "status", 500, "error", "Internal Server Error",
                "code", code, "message", message));
    }
}
