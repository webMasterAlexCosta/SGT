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

    private static final Pattern MYSQL_NOT_NULL_COLUMN = Pattern.compile("Column '([^']+)' cannot be null", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarArgumentoIlegal(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Requisição inválida"
                ));
    }


    @ExceptionHandler(ExcecaoConflitoDados.class)
    public ResponseEntity<Map<String, Object>> tratarConflitoDados(ExcecaoConflitoDados ex) {
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
        int status = ex.getStatusCode().value();
        String reason = ex.getReason();
        String message = reason != null && !reason.isBlank()
                ? reason
                : "Não foi possível concluir a operação.";

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status,
                        "error", ex.getStatusCode().toString(),
                        "message", message
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarJsonInvalido(HttpMessageNotReadableException ex) {
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
                        "message", summary,
                        "fields", fields
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarViolacaoRestricao(ConstraintViolationException ex) {
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
                        "message", "Erro de validação",
                        "fields", violations
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarIntegridade(DataIntegrityViolationException ex) {
        // Útil para capturar violações de FK/unique e devolver feedback mais claro.
        // Observação: "cannot be null" é erro de validação (400), e não conflito (409).
        String mostSpecific = Optional.ofNullable(ex.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse("");

        String integrityLower = mostSpecific.toLowerCase(java.util.Locale.ROOT);
        if (integrityLower.contains("uk_frequencia_empregado_data")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", 409,
                            "error", "Conflict",
                            "code", "FREQUENCIA_DUPLICADA",
                            "message", "Outra requisição gravou esta frequência ao mesmo tempo. Recarregue os dados e tente novamente."
                    ));
        }

        Matcher m = MYSQL_NOT_NULL_COLUMN.matcher(mostSpecific);
        if (m.find()) {
            String col = m.group(1);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "status", 400,
                            "error", "Bad Request",
                            "message", "Campo obrigatório não informado: " + col
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

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> tratarBloqueioOtimista(ObjectOptimisticLockingFailureException ex) {
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
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 401,
                        "error", "Unauthorized",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Não autenticado."
                ));
    }
}
