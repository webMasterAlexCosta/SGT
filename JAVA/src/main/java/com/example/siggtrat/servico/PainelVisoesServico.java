package com.example.siggtrat.servico;

import com.example.siggtrat.dto.PainelAnaliticoDTO.TipoUnidade;
import com.example.siggtrat.dto.PainelVisoesDTO;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio.Filtro;
import com.example.siggtrat.repositorio.PainelRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class PainelVisoesServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PainelVisoesServico.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG; }
    private final PainelAnaliticoServico analitico;
    private final PainelServico painel;
    private final PainelRepositorio repositorio;

    public PainelVisoesServico(PainelAnaliticoServico analitico, PainelServico painel, PainelRepositorio repositorio) {
        this.analitico = analitico;
        this.painel = painel;
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 90)
    public PainelVisoesDTO obter(String granularidade, LocalDate referencia, Long contratoId, String tipoUnidade,
            Long unidadeId, boolean comparar, int paginaUnidades, Long prestadorId) {
        // Esta consulta revalida todos os filtros antes de executar os indicadores complementares.
        var a = analitico.obter(granularidade, referencia, contratoId, tipoUnidade, unidadeId, comparar, paginaUnidades, prestadorId);
        var contexto = painel.contextoAtual();
        Long gestao = contexto.gestaoLocalId(), se = contexto.superintendenciaId();
        if (unidadeId != null) {
            if (a.selecao().tipoUnidade() == TipoUnidade.GESTAO_LOCAL) gestao = unidadeId;
            if (a.selecao().tipoUnidade() == TipoUnidade.SUPERINTENDENCIA) se = unidadeId;
        }
        var filtro = new Filtro(gestao, se, contratoId, a.selecao().tipoUnidade(), unidadeId, prestadorId);
        var recortado = repositorio.noRecorte(filtro, a.unidades());
        var periodo = ResolvedorPeriodoPainel.resolver(granularidade, a.referencia());
        var g = painel.calcularGestaoRecortada(contexto, filtro, periodo, comparar, recortado);
        var ranking = contexto.permiteDetalhamentoOperacional()
                ? recortado.obterRankingLotacoes(periodo.inicio(), periodo.fim(), contratoId, gestao, se) : List.<com.example.siggtrat.dto.PainelRankingDTO>of();
        var anterior = comparar ? recortado.obterResumoEfetivo(periodo.anteriorFim(), gestao, se) : null;
        return new PainelVisoesDTO(a, g, ranking, anterior);
    }
}
