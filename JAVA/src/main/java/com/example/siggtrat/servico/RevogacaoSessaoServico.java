package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.transacao.ExecutorTransacaoIdempotente;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import org.springframework.stereotype.Service;

/** Revoga uma sessão sem repetir efeitos externos ou suspender a transação do chamador. */
@Service
public class RevogacaoSessaoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RevogacaoSessaoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final TokenAtivoRepositorio tokens;
    private final ExecutorTransacaoIdempotente executor;

    public RevogacaoSessaoServico(TokenAtivoRepositorio tokens, ExecutorTransacaoIdempotente executor) {
        this.tokens = tokens;
        this.executor = executor;
    }

    public void revogarToken(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) return;
        executor.executar(() -> {
            tokens.excluirPorTokenBruto(tokenBruto);
            return null;
        });
    }
}
