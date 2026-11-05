package com.example.siggtrat.servico;

import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import com.example.siggtrat.repositorio.MapeamentoTransferenciaGestaoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

/**
 * Impede inativação de catálogo ainda usado por programação operacional futura.
 * Na confirmação administrativa, o chamador já mantém o catálogo em bloqueio
 * exclusivo e usa READ_COMMITTED: uma publicação que venceu a espera precisa
 * aparecer nesta reconstrução. As leituras não adquirem locks de Contrato,
 * pois comandos dependentes mantêm Contrato antes do shared lock do catálogo.
 */
@Service
@Transactional(readOnly = true)
public class ProgramacaoFuturaCatalogoServico {

    public enum TipoCatalogo {
        CARGO("Cargo"), LOTACAO("Lotação"), JORNADA("Turno");

        private final String rotulo;
        TipoCatalogo(String rotulo) { this.rotulo = rotulo; }
    }

    private final ComposicaoContratualRepositorio composicoes;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final AditamentoContratualRepositorio aditamentos;
    private final RemanejamentoContratualRepositorio remanejamentos;
    private MapeamentoTransferenciaGestaoRepositorio transferencias;
    @PersistenceContext private EntityManager em;
    private ComposicaoContratualTemporalServico composicaoTemporal;
    private MotorTemporalContratoServico motorContrato;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarProjecaoTemporal(ComposicaoContratualTemporalServico composicaoTemporal,
                                           MotorTemporalContratoServico motorContrato) {
        this.composicaoTemporal = composicaoTemporal;
        this.motorContrato = motorContrato;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarTransferencias(MapeamentoTransferenciaGestaoRepositorio repositorio) {
        this.transferencias = repositorio;
    }

    public ProgramacaoFuturaCatalogoServico(
            ComposicaoContratualRepositorio composicoes,
            AlocacaoVinculoRepositorio alocacoes,
            AditamentoContratualRepositorio aditamentos,
            RemanejamentoContratualRepositorio remanejamentos) {
        this.composicoes = composicoes;
        this.alocacoes = alocacoes;
        this.aditamentos = aditamentos;
        this.remanejamentos = remanejamentos;
    }

    public void exigirSemProgramacaoFutura(TipoCatalogo tipo, Long catalogoId) {
        if (tipo == null || catalogoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catálogo inválido para inativação.");
        }
        LocalDate referencia = RelogioSistema.hoje().plusDays(1);
        if (composicaoTemporal != null && motorContrato != null && em != null) {
            if (possuiUsoProjetado(tipo, catalogoId, referencia)) conflito(tipo);
            return;
        }
        long usos = switch (tipo) {
            case CARGO -> composicoes.contarUsoProgramadoPorCargo(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorCargo(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorCargo(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorCargo(catalogoId, referencia)
                    + (transferencias == null ? 0 : transferencias.contarUsoFuturoPorCargo(catalogoId, referencia));
            case LOTACAO -> composicoes.contarUsoProgramadoPorLotacao(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorLotacao(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorLotacao(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorLotacao(catalogoId, referencia)
                    + (transferencias == null ? 0 : transferencias.contarUsoFuturoPorLotacao(catalogoId, referencia));
            case JORNADA -> composicoes.contarUsoProgramadoPorJornada(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorJornada(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorJornada(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorJornada(catalogoId, referencia)
                    + (transferencias == null ? 0 : transferencias.contarUsoFuturoPorJornada(catalogoId, referencia));
        };
        if (usos > 0) conflito(tipo);
    }

    /** Composição-base e mapas antigos só localizam candidatos; o uso é decidido pela história vigente. */
    private boolean possuiUsoProjetado(TipoCatalogo tipo, Long id, LocalDate desde) {
        return possuiUsoProjetadoEntre(tipo, id, desde, null);
    }

    public boolean possuiUsoProjetadoEntre(TipoCatalogo tipo, Long id, LocalDate desde, LocalDate ate) {
        Instant conhecimento = Instant.now();
        for (Contrato contrato : contratosRelacionados(tipo, id)) {
            SortedSet<LocalDate> datas = new TreeSet<>(composicaoTemporal.datasMudancaCapacidade(contrato, null));
            datas.add(desde);
            motorContrato.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecimento).stream()
                    .map(e -> e.getDataEfetiva()).filter(Objects::nonNull).forEach(datas::add);
            while (!datas.isEmpty()) {
                LocalDate data = datas.first(); datas.remove(data);
                if (data.isBefore(desde) || ate != null && data.isAfter(ate)) continue;
                var posicao = motorContrato.reconstruirPosicao(contrato, data, conhecimento);
                if (posicao.vigenciaInicio() != null && posicao.vigenciaInicio().isAfter(data)) datas.add(posicao.vigenciaInicio());
                if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                        || posicao.vigenciaInicio() != null && data.isBefore(posicao.vigenciaInicio())
                        || posicao.vigenciaFim() != null && data.isAfter(posicao.vigenciaFim())) continue;
                if (composicaoTemporal.projetarEntidades(contrato, data, conhecimento).stream()
                        .anyMatch(p -> p.efetivoPrevisto() > 0 && Objects.equals(id, switch (tipo) {
                            case CARGO -> p.cargo().getId();
                            case LOTACAO -> p.lotacao().getId();
                            case JORNADA -> p.jornada().getId();
                        }))) return true;
            }
        }
        return false;
    }

    public List<Contrato> contratosRelacionados(TipoCatalogo tipo, Long id) {
        String campo = switch (tipo) { case CARGO -> "cargo"; case LOTACAO -> "lotacao"; case JORNADA -> "jornada"; };
        return em.createQuery("select c from Contrato c where "
                + "exists(select x.id from ComposicaoContratual x where x.contrato=c and x." + campo + ".id=:id) or "
                + "exists(select x.id from AlocacaoVinculo x where x.vinculo.contrato=c and x." + campo + ".id=:id) or "
                + "exists(select x.id from AditamentoComposicao x where x.aditamento.contrato=c and x." + campo + ".id=:id) or "
                + "exists(select x.id from RemanejamentoComposicao x where x.remanejamento.contrato=c and x." + campo + ".id=:id) or "
                + "exists(select x.id from MapeamentoTransferenciaGestao x where x.transferencia.contrato=c and (x."
                + campo + "Origem.id=:id or x." + campo + "Destino.id=:id)) order by c.id", Contrato.class)
                .setParameter("id", id).getResultList();
    }

    private void conflito(TipoCatalogo tipo) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                tipo.rotulo + " possui programação futura. Trate ou cancele os fatos programados antes da inativação.");
    }
}
