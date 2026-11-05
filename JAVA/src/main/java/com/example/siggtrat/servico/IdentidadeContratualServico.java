package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.ReservaIdentificadorContrato;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CorrecaoIdentificadorContratoRepositorio;
import com.example.siggtrat.repositorio.ReservaIdentificadorContratoRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/** Unicidade global atual e histórica, na mesma transação do ato contratual. */
@Service
@Transactional(readOnly = true)
public class IdentidadeContratualServico {
    private final ContratoRepositorio contratos;
    private final CorrecaoIdentificadorContratoRepositorio correcoes;
    private final ReservaIdentificadorContratoRepositorio reservas;

    public IdentidadeContratualServico(ContratoRepositorio contratos,
            CorrecaoIdentificadorContratoRepositorio correcoes, ReservaIdentificadorContratoRepositorio reservas) {
        this.contratos = contratos;
        this.correcoes = correcoes;
        this.reservas = reservas;
    }

    public void validarDisponibilidade(String numero, Integer ano, Long titular) {
        String normalizado = ReservaIdentificadorContrato.normalizarNumero(numero);
        if (normalizado == null || ano == null) return;
        contratos.findByNumeroContratoIgnoreCaseAndAnoContrato(normalizado, ano)
                .filter(c -> !Objects.equals(c.getId(), titular))
                .ifPresent(c -> { throw conflito(); });
        reservas.findByNumeroAndAno(normalizado, ano)
                .filter(r -> !Objects.equals(r.getContrato().getId(), titular))
                .ifPresent(r -> { throw conflito(); });
        // A tabela de reservas pode ser nova sobre uma base com correções já
        // documentadas. A fonte histórica existente continua vinculante, sem
        // reconstruir datas ou versões que não tenham sido preservadas.
        if (correcoes.pesquisarHistorico(normalizado, ano).stream()
                .anyMatch(c -> !Objects.equals(c.getContrato().getId(), titular))) throw conflito();
    }

    @Transactional
    public void reservar(Contrato contrato, String numero, Integer ano) {
        String normalizado = ReservaIdentificadorContrato.normalizarNumero(numero);
        if (contrato == null || contrato.getId() == null || normalizado == null || ano == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reserva histórica exige contrato persistido e identificador completo.");
        validarDisponibilidade(normalizado, ano, contrato.getId());
        var existente = reservas.findByNumeroAndAno(normalizado, ano);
        if (existente.isPresent()) return;
        ReservaIdentificadorContrato reserva = new ReservaIdentificadorContrato();
        reserva.setContrato(contrato);
        reserva.setNumero(normalizado);
        reserva.setAno(ano);
        try {
            reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            // O índice único é a decisão final entre transações que validaram
            // simultaneamente uma identidade livre. A transação inteira reverte.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Número e ano já reservados por outro contrato.", e);
        }
    }

    private ResponseStatusException conflito() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Número e ano já pertencem à identidade atual ou histórica de outro contrato.");
    }
}
