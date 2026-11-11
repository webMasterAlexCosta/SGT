package com.example.siggtrat.diagnostico;

import com.example.siggtrat.excecao.ExcecaoValidacaoNegocio;
import org.springframework.web.server.ResponseStatusException;

/** Mensagem pública contextual; as causas técnicas permanecem no diagnóstico seguro. */
public final class MensagemFalhaAssincrona {
    private MensagemFalhaAssincrona() { }
    public static String criar(String operacao, String referencia, Throwable erro) {
        String mensagem;
        if (erro instanceof ExcecaoValidacaoNegocio) {
            mensagem = DiagnosticoFalha.mensagemPublica(erro.getMessage(), "Verifique os dados da operação.");
        } else if (erro instanceof ResponseStatusException resposta && resposta.getStatusCode().is4xxClientError()) {
            mensagem = DiagnosticoFalha.mensagemPublica(resposta.getReason(), "A operação foi recusada. Verifique dados e permissões.");
        } else if (erro instanceof org.springframework.dao.DataAccessException || erro instanceof jakarta.persistence.PersistenceException) {
            mensagem = "Não foi possível acessar os dados para " + operacao + ".";
        } else if (erro instanceof java.io.IOException) {
            mensagem = "Não foi possível acessar o arquivo para " + operacao + ".";
        } else mensagem = "Não foi possível concluir " + operacao + ".";
        return mensagem + " Referência para suporte: " + referencia + ".";
    }
}
