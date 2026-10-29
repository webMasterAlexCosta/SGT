package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.HistoricoEmpregadoRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class HistoricoIntegridadeServico {

    public static final String PREFIXO = "HMAC-SHA256:";
    private static final String GENESIS = "GENESIS";
    private static final String PENDENTE = "PENDENTE:";

    private final HistoricoEmpregadoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final EntityManager entityManager;
    private final byte[] secret;

    public HistoricoIntegridadeServico(HistoricoEmpregadoRepositorio repositorio,
                                       EmpregadoRepositorio empregadoRepositorio,
                                       EntityManager entityManager,
                                       PropriedadesAuditoria properties) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.entityManager = entityManager;
        if (properties.secret() == null || properties.secret().length() < 32) {
            throw new IllegalStateException("AUDIT_SECRET deve possuir pelo menos 32 caracteres.");
        }
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public HistoricoEmpregado salvarAssinado(HistoricoEmpregado historico) {
        validar(historico);
        Long empregadoId = historico.getEmpregado().getId();
        Empregado empregadoBloqueado = entityManager.find(Empregado.class, empregadoId, LockModeType.PESSIMISTIC_WRITE);
        if (empregadoBloqueado == null) throw new IllegalArgumentException("Empregado do histórico não encontrado.");

        List<HistoricoEmpregado> existentes = repositorio.findByEmpregado_IdOrderByIdAsc(empregadoId);
        long quantidadeAnterior = empregadoBloqueado.getHistoricoQuantidadeAssinada();
        String anterior;
        if (quantidadeAnterior == 0L) {
            validarAncoraInicialVazia(empregadoBloqueado, existentes);
            anterior = GENESIS;
        } else {
            ResultadoVerificacao verificacaoAtual = verificar(empregadoBloqueado, existentes);
            if (!verificacaoAtual.verificavel() || !verificacaoAtual.integro()) {
                throw new IllegalStateException("A gravação foi bloqueada porque o histórico atual não está íntegro: "
                        + verificacaoAtual.mensagem());
            }
            anterior = empregadoBloqueado.getHistoricoUltimoHash();
        }

        historico.setEmpregado(empregadoBloqueado);
        historico.setDataEvento(historico.getDataEvento().truncatedTo(ChronoUnit.SECONDS));
        // O ID gerado também participa da assinatura. O marcador existe apenas dentro
        // da transação e nunca é confirmado caso a assinatura não seja concluída.
        historico.setHashIntegridade(PENDENTE + UUID.randomUUID());
        HistoricoEmpregado salvo = repositorio.saveAndFlush(historico);
        String hashAtual = PREFIXO + hmac(registroCanonico(salvo, anterior));
        salvo.setHashIntegridade(hashAtual);
        repositorio.saveAndFlush(salvo);

        long novaQuantidade = Math.addExact(quantidadeAnterior, 1L);
        long novoTotal = Math.addExact((long) existentes.size(), 1L);
        empregadoBloqueado.setHistoricoUltimoHash(hashAtual);
        empregadoBloqueado.setHistoricoQuantidadeAssinada(novaQuantidade);
        empregadoBloqueado.setHistoricoTotalAncorado(novoTotal);
        empregadoBloqueado.setHistoricoUltimoIdAssinado(salvo.getId());
        empregadoBloqueado.setHistoricoAncoraAssinatura(
                PREFIXO + hmac(ancoraCanonica(
                        empregadoId,
                        novaQuantidade,
                        novoTotal,
                        salvo.getId(),
                        hashAtual
                ))
        );
        return salvo;
    }

    @Transactional(readOnly = true)
    public ResultadoVerificacao verificarEmpregado(Long empregadoId) {
        Empregado empregado = empregadoRepositorio.findById(empregadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado."));
        List<HistoricoEmpregado> itens = repositorio.findByEmpregado_IdOrderByIdAsc(empregadoId);
        return verificar(empregado, itens);
    }

    private ResultadoVerificacao verificar(Empregado empregado, List<HistoricoEmpregado> itens) {
        Long empregadoId = empregado.getId();
        String anterior = GENESIS;
        int verificados = 0;
        Long ultimoIdAssinado = null;
        String ultimoHash = null;
        for (HistoricoEmpregado item : itens) {
            String armazenado = item.getHashIntegridade();
            boolean assinado = armazenado != null && armazenado.startsWith(PREFIXO);
            if (!assinado) {
                return falha(itens, verificados, 1, item.getId(),
                        "Existe registro de histórico sem assinatura de integridade.");
            }

            String esperado = PREFIXO + hmac(registroCanonico(item, anterior));
            if (!iguaisEmTempoConstante(esperado, armazenado)) {
                return falha(itens, verificados, 0, item.getId(),
                        "Divergência detectada no histórico " + item.getId() + ".");
            }
            anterior = armazenado;
            ultimoHash = armazenado;
            ultimoIdAssinado = item.getId();
            verificados++;
        }

        if (verificados == 0) {
            if (possuiAlgumaAncora(empregado)) {
                return falha(itens, 0, 0, null,
                        "Existe âncora sem registros assinados correspondentes.");
            }
            return new ResultadoVerificacao(false, false, 0, 0, 0, null,
                    "O empregado ainda não possui histórico assinado.");
        }

        long quantidadeAncorada = empregado.getHistoricoQuantidadeAssinada();
        if (quantidadeAncorada != verificados) {
            return falha(itens, verificados, 0, null,
                    "A quantidade assinada não corresponde à âncora. Pode ter ocorrido inclusão ou remoção de histórico.");
        }

        if (empregado.getHistoricoTotalAncorado() != itens.size()) {
            return falha(itens, verificados, 0, null,
                    "O total de registros não corresponde à âncora. Pode ter ocorrido inclusão ou remoção de linhas.");
        }

        if (!iguaisEmTempoConstante(ultimoHash, empregado.getHistoricoUltimoHash())) {
            return falha(itens, verificados, 0, null,
                    "O último hash não corresponde à âncora do empregado.");
        }

        if (ultimoIdAssinado == null || !ultimoIdAssinado.equals(empregado.getHistoricoUltimoIdAssinado())) {
            return falha(itens, verificados, 0, ultimoIdAssinado,
                    "O último ID assinado não corresponde à âncora do empregado.");
        }

        String assinaturaEsperada = PREFIXO + hmac(ancoraCanonica(
                empregadoId,
                quantidadeAncorada,
                empregado.getHistoricoTotalAncorado(),
                ultimoIdAssinado,
                ultimoHash
        ));
        if (!iguaisEmTempoConstante(assinaturaEsperada, empregado.getHistoricoAncoraAssinatura())) {
            return falha(itens, verificados, 0, null, "A assinatura da âncora do histórico é inválida.");
        }

        return new ResultadoVerificacao(true, true, itens.size(), verificados, 0, null,
                "Todos os registros e a âncora final possuem assinatura íntegra.");
    }

    private void validarAncoraInicialVazia(Empregado empregado, List<HistoricoEmpregado> existentes) {
        if (possuiAlgumaAncora(empregado) || !existentes.isEmpty()) {
            throw new IllegalStateException("A âncora inicial do histórico está inconsistente.");
        }
    }

    private boolean possuiAlgumaAncora(Empregado empregado) {
        return empregado.getHistoricoQuantidadeAssinada() != 0L
                || empregado.getHistoricoTotalAncorado() != 0L
                || empregado.getHistoricoUltimoHash() != null
                || empregado.getHistoricoUltimoIdAssinado() != null
                || empregado.getHistoricoAncoraAssinatura() != null;
    }

    private ResultadoVerificacao falha(List<HistoricoEmpregado> itens, int verificados, int naoAssinados,
                                       Long registro, String mensagem) {
        return new ResultadoVerificacao(true, false, itens.size(), verificados, naoAssinados, registro, mensagem);
    }

    private void validar(HistoricoEmpregado historico) {
        if (historico == null || historico.getEmpregado() == null || historico.getEmpregado().getId() == null) {
            throw new IllegalArgumentException("Empregado é obrigatório no histórico.");
        }
        if (historico.getDataEvento() == null) throw new IllegalArgumentException("Data do histórico é obrigatória.");
        if (historico.getDescricao() == null || historico.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do histórico é obrigatória.");
        }
    }

    private String registroCanonico(HistoricoEmpregado historico, String anterior) {
        Long usuarioId = historico.getUsuario() != null ? historico.getUsuario().getId() : null;
        String descricao = base64(historico.getDescricao());
        return historico.getEmpregado().getId() + "|"
                + historico.getId() + "|"
                + historico.getDataEvento().truncatedTo(ChronoUnit.SECONDS) + "|"
                + (usuarioId == null ? "0" : usuarioId) + "|"
                + descricao + "|" + anterior;
    }

    private String ancoraCanonica(Long empregadoId, long quantidadeAssinada, long totalAncorado,
                                    Long ultimoIdAssinado, String ultimoHash) {
        return empregadoId + "|" + quantidadeAssinada + "|" + totalAncorado + "|"
                + ultimoIdAssinado + "|" + ultimoHash;
    }

    private String base64(String value) {
        String seguro = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(seguro.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível assinar o histórico.", ex);
        }
    }

    private boolean iguaisEmTempoConstante(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record ResultadoVerificacao(
            boolean verificavel,
            boolean integro,
            int totalRegistros,
            int registrosVerificados,
            int registrosNaoAssinados,
            Long primeiroRegistroInvalido,
            String mensagem
    ) {}
}
