package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Serviço de auditoria para registrar ações de escrita.  As ações são enfileiradas
 * em um buffer para persistência assíncrona em lote no banco de dados, reduzindo
 * a sobrecarga de criação de threads e commits individuais.  Além disso, o
 * serviço registra as operações em INFO no log de aplicação.
 */
@Service
public class RegistroAuditoriaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return log; }


    private static final Logger log = LoggerFactory.getLogger(RegistroAuditoriaServico.class);

    // A configuração pode ampliar a proteção, mas nunca retirar as credenciais
    // conhecidas. Inclui variantes como newPassword, currentPassword e senhaAtual.
    private static final Pattern CAMPOS_CREDENCIAIS = Pattern.compile(
            "(?i).*(password|passphrase|senha|token|secret|segredo|credential|credencial|"
                    + "private.?key|api.?key|totp|otp|authorization|cookie|recovery.?code|backup.?code).*"
    );

    private final PersistenciaRegistroAuditoriaServico persistencia;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final ObjectMapper objectMapper;

    // Buffer para persistência assíncrona de logs.  Utilizar uma fila
    // desacoplada evita saturar a pool de threads e garante controle de
    // backpressure quando houver picos de eventos de auditoria.
    private final FilaRegistroAuditoria auditLogBuffer;

    private final int maxPayloadBytes;

    /**
     * Padrão para campos considerados sensíveis no corpo da requisição. Este valor é
     * configurável através da propriedade `audit.sensitive-fields-regex`. Caso não
     * seja fornecida, utiliza a expressão padrão que inclui nomes comuns de
     * credenciais e chaves (password, senha, token, etc.).
     */
    private final Pattern sensitiveFieldPattern;

    public RegistroAuditoriaServico(
            PersistenciaRegistroAuditoriaServico persistencia,
            UsuarioAplicacaoRepositorio appUserRepositorio,
            ObjectMapper objectMapper,
            FilaRegistroAuditoria auditLogBuffer,
            PropriedadesAuditoria properties
    ) {
        this.persistencia = persistencia;
        this.appUserRepositorio = appUserRepositorio;
        this.objectMapper = objectMapper;
        this.auditLogBuffer = auditLogBuffer;
        this.maxPayloadBytes = Math.max(1024, properties.payload().maxBytes());
        this.sensitiveFieldPattern = Pattern.compile(properties.sensitiveFieldsRegex());
    }

    public void registrarAcao(String action, String entityType, Long entityId, String entityLabel, Map<String, Object> details) {
        registrarAcaoInterna(action, entityType, entityId, entityLabel, details, null);
    }

    /**
     * Variante para tarefas assíncronas, nas quais o SecurityContext e o request
     * HTTP já não existem. O ator é persistido explicitamente na auditoria.
     */
    public void registrarAcaoComoUsuario(String action,
                                String entityType,
                                Long entityId,
                                String entityLabel,
                                Map<String, Object> details,
                                UsuarioAplicacao actor) {
        registrarAcaoInterna(action, entityType, entityId, entityLabel, details, actor);
    }

    private void registrarAcaoInterna(String action,
                                   String entityType,
                                   Long entityId,
                                   String entityLabel,
                                   Map<String, Object> details,
                                   UsuarioAplicacao actorOverride) {
        RegistroAuditoria al = new RegistroAuditoria();
        al.setAction(action);
        al.setEntityType(entityType);
        al.setEntityId(entityId);
        al.setEntityLabel(entityLabel);

        // Extração do Request context de forma síncrona
        HttpServletRequest req = requisicaoAtual().orElse(null);
        if (req != null) {
            Object rid = req.getAttribute(FiltroIdentificadorRequisicao.REQUEST_ID_ATTR);
            if (rid != null) al.setRequestId(rid.toString());
            else {
                String hdr = req.getHeader(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER);
                if (hdr != null && !hdr.isBlank()) al.setRequestId(hdr.trim());
            }

            al.setIp(resolverIpCliente(req));
            String ua = req.getHeader("User-Agent");
            if (ua != null) al.setUserAgent(limitarTexto(ua, 255));

            al.setHttpMethod(limitarTexto(req.getMethod(), 10));
            al.setPath(limitarTexto(req.getRequestURI(), 255));
            String qs = req.getQueryString();
            if (qs != null && !qs.isBlank()) al.setQueryString(limitarTexto(mascararQuery(qs), 4000));

            // Captura payload (body)
            capturarCorpoRequisicaoSeDisponivel(req, al);
        }

        // Auth context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = actorOverride != null
                ? actorOverride.getUsername()
                : (auth != null ? auth.getName() : null);
        al.setUsername(username);

        if (actorOverride != null) {
            al.setUserId(actorOverride.getId());
        } else if (username != null) {
            Optional<UsuarioAplicacao> u = appUserRepositorio.findByUsername(username);
            u.ifPresent(appUser -> al.setUserId(appUser.getId()));
        }

        if (details != null && !details.isEmpty()) {
            al.setDetailsJson(paraJsonSeguro(details));
        }

        // O conteúdo e o ator são capturados na thread original, mas uma ação
        // transacional só pode ser anunciada e enfileirada após o commit.
        // Rollback (inclusive de tentativa que será repetida) descarta o evento.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publicar(al); }
            });
        } else {
            publicar(al);
        }
    }

    private void publicar(RegistroAuditoria al) {
        try {
            auditLogBuffer.enfileirarOuPersistir(al);
        } catch (Exception e) {
            log.error("RegistroAuditoriaServico: CRÍTICO: Falha ao enfileirar log de auditoria. Tentativa de persistir de forma síncrona. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
            try {
                persistencia.salvar(al);
            } catch (Exception ex) {
                log.error("RegistroAuditoriaServico: CRÍTICO: Falha ao persistir log de auditoria de fallback. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(ex));
            }
        }

        // Também registra em log de aplicação (útil para grep no terminal)
        log.info("AUDIT action={} entityType={} entityId={} user={} requestId={}",
                al.getAction(), al.getEntityType(), al.getEntityId(), al.getUsername(), al.getRequestId());
    }

    private void capturarCorpoRequisicaoSeDisponivel(HttpServletRequest req, RegistroAuditoria al) {
        try {
            // Spring Security e o contêiner podem envolver o wrapper de cache.
            // O corpo consumido pelo MVC continua no wrapper interno da mesma requisição.
            ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(req, ContentCachingRequestWrapper.class);
            if (wrapper == null) return;

            String contentType = Optional.ofNullable(wrapper.getContentType()).orElse("");
            // Evita armazenar binários/multipart
            if (contentType.toLowerCase().startsWith("multipart/") || contentType.toLowerCase().startsWith("application/octet-stream")) {
                return;
            }

            byte[] body = wrapper.getContentAsByteArray();
            if (body == null || body.length == 0) return;

            Charset cs;
            try {
                cs = (wrapper.getCharacterEncoding() != null) ? Charset.forName(wrapper.getCharacterEncoding()) : StandardCharsets.UTF_8;
            } catch (Exception ignored) {
                cs = StandardCharsets.UTF_8;
            }

            String raw = new String(body, cs).trim();
            if (raw.isBlank()) return;

            // Aplica masking de campos sensíveis quando for JSON
            String masked = raw;
            if (contentType.toLowerCase().contains("json") || (raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
                masked = mascararJsonSensivel(raw);
            }

            al.setPayloadJson(masked);
            long len = wrapper.getContentLengthLong();
            if (len > 0) {
                al.setPayloadTruncated(len > maxPayloadBytes);
            } else {
                al.setPayloadTruncated(body.length >= maxPayloadBytes);
            }
        } catch (Exception e) {
            al.setPayloadJson("{\"error\":\"failed_to_capture_payload\"}");
            al.setPayloadTruncated(false);
        }
    }

    private String mascararJsonSensivel(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            mascararNo(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // Um corpo truncado pode conter justamente a senha em sua parte
            // legível. Na dúvida, não armazene bytes que não foram mascarados.
            return "{\"payloadOmitido\":\"JSON inválido ou truncado\"}";
        }
    }

    private void mascararNo(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(fn -> {
                JsonNode child = obj.get(fn);
                if (child == null) return;
                if (CAMPOS_CREDENCIAIS.matcher(fn).matches() || sensitiveFieldPattern.matcher(fn).matches()) {
                    obj.put(fn, "***");
                } else {
                    mascararNo(child);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) mascararNo(child);
        }
    }

    private String mascararQuery(String original) {
        try {
            String[] campos = original.split("&", -1);
            for (int i = 0; i < campos.length; i++) {
                int separador = campos[i].indexOf('=');
                String chave = separador < 0 ? campos[i] : campos[i].substring(0, separador);
                String nome = URLDecoder.decode(chave, StandardCharsets.UTF_8);
                if (CAMPOS_CREDENCIAIS.matcher(nome).matches() || sensitiveFieldPattern.matcher(nome).matches()) {
                    campos[i] = chave + "=***";
                }
            }
            return String.join("&", campos);
        } catch (IllegalArgumentException invalida) {
            return "[QUERY_OMITIDA_POR_CODIFICACAO_INVALIDA]";
        }
    }

    private Optional<HttpServletRequest> requisicaoAtual() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return Optional.ofNullable(sra.getRequest());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private String resolverIpCliente(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return limitarTexto(first, 45);
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return limitarTexto(realIp.trim(), 45);
        return limitarTexto(req.getRemoteAddr(), 45);
    }

    private String paraJsonSeguro(Object obj) {
        try {
            return mascararJsonSensivel(objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"failed_to_serialize_details\"}";
        }
    }

    private String limitarTexto(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    public static Map<String, Object> detalhes(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (kv == null) return m;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (k != null) m.put(String.valueOf(k), v);
        }
        return m;
    }
}
