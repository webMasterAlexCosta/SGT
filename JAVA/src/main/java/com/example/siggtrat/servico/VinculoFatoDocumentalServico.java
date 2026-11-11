package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dto.CadastroDocumentoComprobatorioRequisicao;
import com.example.siggtrat.entidade.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/** Confere a referência tipada antes de associar uma fonte a um fato já registrado. */
@Service
@Transactional(readOnly = true)
public class VinculoFatoDocumentalServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(VinculoFatoDocumentalServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final EntityManager em;

    public VinculoFatoDocumentalServico(EntityManager em) { this.em = em; }

    public void validar(Contrato contrato, CadastroDocumentoComprobatorioRequisicao req) {
        if (req.fatoId() == null) return; // Fonte preparada: será vinculada na publicação do fato.
        Long contratoDoFato = switch (req.tipoFato()) {
            case CONTRATO_ORIGINAL -> contratoEvento(req.fatoId(), TipoEventoCicloVidaContrato.PUBLICACAO);
            case ENCERRAMENTO_ANTECIPADO -> contratoEvento(req.fatoId(), TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO);
            case ANULACAO -> contratoEvento(req.fatoId(), TipoEventoCicloVidaContrato.ANULACAO);
            case ADITAMENTO -> {
                AditamentoContratual fato = em.find(AditamentoContratual.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case REMANEJAMENTO -> {
                RemanejamentoContratual fato = em.find(RemanejamentoContratual.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case TRANSFERENCIA_GESTAO -> {
                TransferenciaGestaoContrato fato = em.find(TransferenciaGestaoContrato.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case CORRECAO_IDENTIFICADOR -> {
                CorrecaoIdentificadorContrato fato = em.find(CorrecaoIdentificadorContrato.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case CORRECAO_CNPJ -> {
                CorrecaoCnpjContrato fato = em.find(CorrecaoCnpjContrato.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case VINCULO -> {
                VinculoContratual fato = em.find(VinculoContratual.class, req.fatoId());
                yield fato == null ? null : fato.getContrato().getId();
            }
            case ALOCACAO -> {
                AlocacaoVinculo fato = em.find(AlocacaoVinculo.class, req.fatoId());
                yield fato == null ? null : fato.getVinculo().getContrato().getId();
            }
            // OUTRO não possui identidade interna tipada; deve utilizar fatoChave.
            case OUTRO -> null;
        };
        if (!Objects.equals(contrato.getId(), contratoDoFato)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O fato documental informado não existe neste contrato ou não corresponde ao tipo de fato.");
        }
    }

    private Long contratoEvento(Long id, TipoEventoCicloVidaContrato tipo) {
        EventoCicloVidaContrato fato = em.find(EventoCicloVidaContrato.class, id);
        return fato == null || fato.getTipoEvento() != tipo ? null : fato.getContrato().getId();
    }
}
