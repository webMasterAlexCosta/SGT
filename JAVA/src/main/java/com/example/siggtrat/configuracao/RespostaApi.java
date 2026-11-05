package com.example.siggtrat.configuracao;

import java.time.Instant;

public class RespostaApi<T> {

    private String timestamp;
    private int status;
    private String error;   // null em sucesso
    private String message; // ex: "OK", "Created"


    private String requestId;

    private Object meta;

    private T data;
    private String path;

    public RespostaApi() {
    }

    public RespostaApi(int status, String message, T data, String path, String requestId, Object meta) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = null;
        this.message = message;
        this.data = data;
        this.path = path;
        this.requestId = requestId;
        this.meta = meta;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getMeta() {
        return meta;
    }

    public void setMeta(Object meta) {
        this.meta = meta;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
