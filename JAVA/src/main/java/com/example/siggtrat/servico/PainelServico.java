package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.constante.EsquemaPainel;
import com.example.siggtrat.dto.AtividadePainelDTO;
import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.dto.PainelRankingDTO;
import com.example.siggtrat.dto.PainelEfetivoDTO;
import com.example.siggtrat.dto.EstatisticasPainelDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.CargoRepositorio;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.repositorio.PainelRepositorio;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PainelServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PainelServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final EmpregadoRepositorio empregadoRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final HistoricoEmpregadoConsultaServico historicoConsulta;
    private final PainelRepositorio painelRepositorio;
    private final PainelEfetivoRepositorio painelEfetivoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final ConsultaCacheRedisServico redisQueryCacheServico;
    private final ServicoChaveCacheRedis redisCacheKeyServico;

    public PainelServico(EmpregadoRepositorio empregadoRepositorio, CargoRepositorio cargoRepositorio,
                         LotacaoRepositorio lotacaoRepositorio, HistoricoEmpregadoConsultaServico historicoConsulta,
                         PainelRepositorio painelRepositorio, PainelEfetivoRepositorio painelEfetivoRepositorio,
                         AcessoOrganizacionalServico acessoOrganizacionalServico,
                         ConsultaCacheRedisServico redisQueryCacheServico,
                         ServicoChaveCacheRedis redisCacheKeyServico) {
        this.empregadoRepositorio = empregadoRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.historicoConsulta = historicoConsulta;
        this.painelRepositorio = painelRepositorio;
        this.painelEfetivoRepositorio = painelEfetivoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.redisQueryCacheServico = redisQueryCacheServico;
        this.redisCacheKeyServico = redisCacheKeyServico;
    }

    @Transactional(readOnly = true)
    public PainelEfetivoDTO.Filtros obterFiltrosEfetivo(
            Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId) {
        return obterFiltrosEfetivo(contratoId, lotacaoId, jornadaId, cargoId, null, "DIARIO", false);
    }

    @Transactional(readOnly = true)
    public PainelEfetivoDTO.Filtros obterFiltrosEfetivo(
            Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId,
            LocalDate referencia, String periodo, boolean compararAnoAnterior) {
        PainelCalculoContexto contexto = contextoAtual();
        boolean detalhamento = contexto.permiteDetalhamentoOperacional();
        LocalDate hoje = LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        LocalDate fim = referencia == null ? hoje : referencia;
        if (fim.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência não pode estar no futuro.");
        }
        LocalDate inicio = PeriodoIndicador.resolver(periodo).inicio(fim);
        Long lotacao = detalhamento ? lotacaoId : null;
        Long jornada = detalhamento ? jornadaId : null;
        Long cargo = detalhamento ? cargoId : null;
        Long gestao = contexto.gestaoLocalId();
        Long se = contexto.superintendenciaId();
        return new PainelEfetivoDTO.Filtros(
                opcoesPorPeriodo((i, f) -> painelEfetivoRepositorio.listarContratos(
                        gestao, se, lotacao, jornada, cargo, i, f), inicio, fim, compararAnoAnterior),
                detalhamento ? opcoesPorPeriodo((i, f) -> painelEfetivoRepositorio.listarLotacoes(
                        gestao, se, contratoId, jornada, cargo, i, f), inicio, fim, compararAnoAnterior) : List.of(),
                detalhamento ? opcoesPorPeriodo((i, f) -> painelEfetivoRepositorio.listarTurnos(
                        gestao, se, contratoId, lotacao, cargo, i, f), inicio, fim, compararAnoAnterior) : List.of(),
                detalhamento ? opcoesPorPeriodo((i, f) -> painelEfetivoRepositorio.listarCargos(
                        gestao, se, contratoId, lotacao, jornada, i, f), inicio, fim, compararAnoAnterior) : List.of(),
                detalhamento);
    }

    private List<PainelEfetivoDTO.Opcao> opcoesPorPeriodo(
            java.util.function.BiFunction<LocalDate, LocalDate, List<PainelEfetivoDTO.Opcao>> consulta,
            LocalDate inicio, LocalDate fim, boolean compararAnoAnterior) {
        Map<Long, PainelEfetivoDTO.Opcao> unicas = new LinkedHashMap<>();
        if (compararAnoAnterior) {
            for (var opcao : consulta.apply(inicio.minusYears(1), fim.minusYears(1))) unicas.put(opcao.id(), opcao);
        }
        for (var opcao : consulta.apply(inicio, fim)) unicas.put(opcao.id(), opcao);
        return unicas.values().stream().sorted(Comparator.comparing(PainelEfetivoDTO.Opcao::nome,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)).thenComparing(PainelEfetivoDTO.Opcao::id))
                .toList();
    }

    @Transactional(readOnly = true)
    public PainelEfetivoDTO obterIndicadorEfetivo(
            LocalDate referenciaInformada,
            String periodoInformado,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId,
            boolean compararAnoAnterior
    ) {
        PainelCalculoContexto contexto = contextoAtual();
        LocalDate hoje = LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        LocalDate fim = referenciaInformada == null ? hoje.minusDays(1) : referenciaInformada;
        if (fim.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência não pode estar no futuro.");
        }

        PeriodoIndicador periodo = PeriodoIndicador.resolver(periodoInformado);
        LocalDate inicio = periodo.inicio(fim);

        if (!contexto.permiteDetalhamentoOperacional()) {
            lotacaoId = null;
            jornadaId = null;
            cargoId = null;
        }

        List<PainelEfetivoDTO.PontoDiario> atualDiario = completarSerieDiaria(
                inicio, fim,
                painelEfetivoRepositorio.obterSerie(
                        inicio, fim, contratoId, lotacaoId, jornadaId, cargoId,
                        contexto.gestaoLocalId(), contexto.superintendenciaId())
        );
        List<PainelEfetivoDTO.PontoDiario> atual = periodo.agruparPorMes()
                ? agruparSeriePorMes(atualDiario)
                : atualDiario;

        List<PainelEfetivoDTO.PontoDiario> anteriorDiario = List.of();
        List<PainelEfetivoDTO.PontoDiario> anterior = List.of();
        if (compararAnoAnterior) {
            LocalDate inicioAnterior = inicio.minusYears(1);
            LocalDate fimAnterior = fim.minusYears(1);
            anteriorDiario = completarSerieDiaria(
                    inicioAnterior, fimAnterior,
                    painelEfetivoRepositorio.obterSerie(
                            inicioAnterior, fimAnterior, contratoId, lotacaoId, jornadaId, cargoId,
                            contexto.gestaoLocalId(), contexto.superintendenciaId())
            );
            anterior = periodo.agruparPorMes()
                    ? agruparSeriePorMes(anteriorDiario)
                    : anteriorDiario;
        }

        List<PainelEfetivoDTO.LinhaDimensionada> quadroDimensionado = List.of();
        List<PainelEfetivoDTO.DetalheDimensao> detalhamentoLotacoes = List.of();
        List<PainelEfetivoDTO.DetalheDimensao> detalhamentoTurnos = List.of();
        List<PainelEfetivoDTO.DetalheDimensao> detalhamentoCargos = List.of();

        List<PainelEfetivoDTO.DetalheContrato> detalhamentoContratos =
                painelEfetivoRepositorio.obterDetalhamentoContratos(
                        fim,
                        contratoId,
                        contexto.permiteDetalhamentoOperacional() ? lotacaoId : null,
                        contexto.permiteDetalhamentoOperacional() ? jornadaId : null,
                        contexto.permiteDetalhamentoOperacional() ? cargoId : null,
                        contexto.gestaoLocalId(),
                        contexto.superintendenciaId());

        if (contexto.permiteDetalhamentoOperacional()) {
            quadroDimensionado = painelEfetivoRepositorio.obterQuadroDimensionado(
                    fim, contratoId, lotacaoId, jornadaId, cargoId,
                    contexto.gestaoLocalId(), contexto.superintendenciaId());
            detalhamentoLotacoes = resumirDimensao(quadroDimensionado, "LOTACAO");
            detalhamentoTurnos = resumirDimensao(quadroDimensionado, "TURNO");
            detalhamentoCargos = resumirDimensao(quadroDimensionado, "CARGO");
        }

        return new PainelEfetivoDTO(
                inicio, fim, periodo.codigo(), contratoId, lotacaoId, jornadaId, cargoId,
                resumirEfetivo(atualDiario),
                compararAnoAnterior ? resumirEfetivo(anteriorDiario) : null,
                atual, anterior,
                detalhamentoContratos,
                detalhamentoLotacoes,
                detalhamentoTurnos,
                detalhamentoCargos,
                quadroDimensionado
        );
    }

    private List<PainelEfetivoDTO.PontoDiario> completarSerieDiaria(
            LocalDate inicio,
            LocalDate fim,
            List<PainelEfetivoDTO.PontoDiario> valores
    ) {
        Map<LocalDate, PainelEfetivoDTO.PontoDiario> porData = new LinkedHashMap<>();
        if (valores != null) {
            for (PainelEfetivoDTO.PontoDiario ponto : valores) {
                if (ponto != null && ponto.data() != null) porData.put(ponto.data(), ponto);
            }
        }
        List<PainelEfetivoDTO.PontoDiario> serie = new ArrayList<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            serie.add(porData.getOrDefault(data, pontoVazio(data)));
        }
        return serie;
    }

    private PainelEfetivoDTO.PontoDiario pontoVazio(LocalDate data) {
        return new PainelEfetivoDTO.PontoDiario(
                data, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0D, 0D);
    }

    private List<PainelEfetivoDTO.PontoDiario> agruparSeriePorMes(
            List<PainelEfetivoDTO.PontoDiario> serieDiaria
    ) {
        Map<java.time.YearMonth, List<PainelEfetivoDTO.PontoDiario>> grupos = new LinkedHashMap<>();
        for (PainelEfetivoDTO.PontoDiario ponto : serieDiaria) {
            grupos.computeIfAbsent(java.time.YearMonth.from(ponto.data()), ignorado -> new ArrayList<>()).add(ponto);
        }

        List<PainelEfetivoDTO.PontoDiario> resultado = new ArrayList<>(grupos.size());
        for (Map.Entry<java.time.YearMonth, List<PainelEfetivoDTO.PontoDiario>> entrada : grupos.entrySet()) {
            List<PainelEfetivoDTO.PontoDiario> pontos = entrada.getValue();
            long dias = Math.max(1, pontos.size());
            long somaPrevisto = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::efetivoPrevisto).sum();
            long somaAlocado = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::efetivoAlocado).sum();
            long somaRegistros = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::registrosLancados).sum();
            long somaPresentes = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::presentes).sum();
            long somaAusentes = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::ausentes).sum();
            long somaOutros = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::outrosRegistros).sum();
            long somaPendentes = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::pendentesLancamento).sum();
            long somaDeficit = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::deficitContratual).sum();
            long somaExcedente = pontos.stream().mapToLong(PainelEfetivoDTO.PontoDiario::excedenteContratual).sum();

            resultado.add(new PainelEfetivoDTO.PontoDiario(
                    entrada.getKey().atDay(1),
                    Math.round(somaPrevisto / (double) dias),
                    Math.round(somaAlocado / (double) dias),
                    Math.round(somaRegistros / (double) dias),
                    Math.round(somaPresentes / (double) dias),
                    Math.round(somaAusentes / (double) dias),
                    Math.round(somaOutros / (double) dias),
                    Math.round(somaPendentes / (double) dias),
                    Math.round(somaDeficit / (double) dias),
                    Math.round(somaExcedente / (double) dias),
                    percentual(somaRegistros, somaAlocado),
                    percentual(somaPresentes, somaPrevisto)
            ));
        }
        return resultado;
    }

    /**
     * O resumo é uma fotografia da data de referência (último ponto da série),
     * não uma média do intervalo selecionado. O período existe para tendência.
     */
    private PainelEfetivoDTO.Resumo resumirEfetivo(List<PainelEfetivoDTO.PontoDiario> serie) {
        if (serie == null || serie.isEmpty()) {
            return new PainelEfetivoDTO.Resumo(0, 0, 0, 0, 0, 0, 0, 0, 0, 0D, 0D);
        }
        PainelEfetivoDTO.PontoDiario ponto = serie.get(serie.size() - 1);
        return new PainelEfetivoDTO.Resumo(
                ponto.efetivoPrevisto(),
                ponto.efetivoAlocado(),
                ponto.registrosLancados(),
                ponto.presentes(),
                ponto.ausentes(),
                ponto.outrosRegistros(),
                ponto.pendentesLancamento(),
                ponto.deficitContratual(),
                ponto.excedenteContratual(),
                ponto.percentualCobertura(),
                ponto.percentualAtendimento()
        );
    }

    private List<PainelEfetivoDTO.DetalheDimensao> resumirDimensao(
            List<PainelEfetivoDTO.LinhaDimensionada> linhas,
            String dimensao
    ) {
        if (linhas == null || linhas.isEmpty()) return List.of();

        Map<String, AcumuladorDimensao> acumulados = new LinkedHashMap<>();
        for (PainelEfetivoDTO.LinhaDimensionada linha : linhas) {
            Long id;
            String nome;
            switch (dimensao) {
                case "LOTACAO" -> { id = linha.lotacaoId(); nome = linha.lotacao(); }
                case "TURNO" -> { id = linha.jornadaId(); nome = linha.turno(); }
                case "CARGO" -> { id = linha.cargoId(); nome = linha.cargo(); }
                default -> throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Dimensão operacional inválida: " + dimensao);
            }
            String nomeSeguro = nome == null || nome.isBlank() ? "Não informado" : nome;
            String chave = String.valueOf(id) + "|" + nomeSeguro;
            AcumuladorDimensao acumulador = acumulados.computeIfAbsent(
                    chave, ignorado -> new AcumuladorDimensao(id, nomeSeguro));
            acumulador.previstos += linha.efetivoPrevisto();
            acumulador.alocados += linha.efetivoAlocado();
            acumulador.registros += linha.registrosLancados();
            acumulador.presentes += linha.presentes();
            acumulador.ausentes += linha.ausentes();
            acumulador.pendentes += linha.pendentesLancamento();
        }

        List<PainelEfetivoDTO.DetalheDimensao> resultado = new ArrayList<>(acumulados.size());
        for (AcumuladorDimensao acumulador : acumulados.values()) {
            resultado.add(new PainelEfetivoDTO.DetalheDimensao(
                    acumulador.id,
                    acumulador.nome,
                    acumulador.alocados,
                    acumulador.registros,
                    acumulador.presentes,
                    acumulador.ausentes,
                    acumulador.pendentes,
                    percentual(acumulador.registros, acumulador.alocados),
                    percentual(acumulador.presentes, acumulador.previstos),
                    acumulador.previstos, Math.max(0, acumulador.previstos - acumulador.alocados),
                    Math.max(0, acumulador.alocados - acumulador.previstos)
            ));
        }
        resultado.sort(
                Comparator.comparingLong(PainelEfetivoDTO.DetalheDimensao::efetivoAlocado).reversed()
                        .thenComparing(PainelEfetivoDTO.DetalheDimensao::nome, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    private double percentual(long numerador, long denominador) {
        if (denominador <= 0L) return 0D;
        return Math.round((numerador * 10000D) / denominador) / 100D;
    }

    private static final class AcumuladorDimensao {
        private final Long id;
        private final String nome;
        private long previstos;
        private long alocados;
        private long registros;
        private long presentes;
        private long ausentes;
        private long pendentes;

        private AcumuladorDimensao(Long id, String nome) {
            this.id = id;
            this.nome = nome;
        }
    }

    private record PeriodoIndicador(String codigo, boolean agruparPorMes) {
        private LocalDate inicio(LocalDate fim) {
            return switch (codigo) {
                case "DIARIO" -> fim;
                case "SEMANAL" -> fim.minusDays(6);
                case "QUINZENAL" -> fim.minusDays(14);
                case "MENSAL" -> fim.withDayOfMonth(1);
                case "SEMESTRAL" -> LocalDate.of(fim.getYear(), fim.getMonthValue() <= 6 ? 1 : 7, 1);
                case "ANUAL" -> LocalDate.of(fim.getYear(), 1, 1);
                default -> throw new IllegalStateException("Período não resolvido: " + codigo);
            };
        }

        private static PeriodoIndicador resolver(String valor) {
            String normalizado = valor == null ? "DIARIO" : valor.trim().toUpperCase(java.util.Locale.ROOT);
            return switch (normalizado) {
                case "DIARIO" -> new PeriodoIndicador("DIARIO", false);
                case "SEMANAL" -> new PeriodoIndicador("SEMANAL", false);
                case "QUINZENAL" -> new PeriodoIndicador("QUINZENAL", false);
                case "MENSAL" -> new PeriodoIndicador("MENSAL", false);
                case "SEMESTRAL" -> new PeriodoIndicador("SEMESTRAL", true);
                case "ANUAL" -> new PeriodoIndicador("ANUAL", true);
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Período inválido. Use DIARIO, SEMANAL, QUINZENAL, MENSAL, SEMESTRAL ou ANUAL."
                );
            };
        }
    }

    @Transactional(readOnly = true)
    public PainelGestaoDTO obterGestao(LocalDate referenciaInformada, Integer periodoDiasInformado) {
        return calcularGestaoCacheado(contextoAtual(), referenciaInformada, periodoDiasInformado);
    }

    /**
     * Caminho cacheado usado tanto pelo endpoint síncrono quanto pelo cálculo
     * assíncrono consumido pelo React. Antes, o processador assíncrono chamava
     * calcularGestao diretamente e nunca consultava o Redis.
     */
    @Transactional(readOnly = true)
    public PainelGestaoDTO calcularGestaoCacheado(
            PainelCalculoContexto contexto,
            LocalDate referenciaInformada,
            Integer periodoDiasInformado
    ) {
        LocalDate referencia = referenciaInformada == null
                ? LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)).minusDays(1)
                : referenciaInformada;
        int periodoDias = periodoDiasInformado == null ? 1 : periodoDiasInformado;
        String chave = EsquemaPainel.CACHE_PREFIX + redisCacheKeyServico.painelEscopo(
                contexto.nivel(), contexto.superintendenciaId(), contexto.gestaoLocalId(),
                contexto.permiteDetalhamentoOperacional(), contexto.podeVisualizarContratos(),
                contexto.podeVisualizarEmpregados(), contexto.podeOperarFrequencia(),
                contexto.podeAcessarRelatorios(), referencia, periodoDias);
        PainelGestaoDTO dados = redisQueryCacheServico.painelGestao(
                chave,
                () -> calcularGestao(contexto, referencia, periodoDias));
        // Cache é aceleração, nunca fonte de verdade. Um valor nulo anterior não
        // pode fazer um cálculo persistido terminar como CONCLUIDO sem dados.
        return dados != null ? dados : calcularGestao(contexto, referencia, periodoDias);
    }

    @Transactional(readOnly = true)
    public PainelCalculoContexto contextoAtual() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirPainelGestao(user);
        ContextoEscopo scope = resolverEscopo(user);
        return new PainelCalculoContexto(
                scope.nivel(),
                scope.nome(),
                scope.superintendenciaId(),
                scope.gestaoLocalId(),
                !acessoOrganizacionalServico.ehPresidente(user),
                acessoOrganizacionalServico.podeConsultarContratos(user),
                acessoOrganizacionalServico.podeConsultarEmpregados(user),
                acessoOrganizacionalServico.podeUsarFrequenciaDiaria(user),
                acessoOrganizacionalServico.podeAcessarRelatoriosGestao(user)
        );
    }

    /**
     * Calcula o painel sem depender do SecurityContext da thread. Esse formato é
     * necessário para o processamento assíncrono persistido do painel.
     */
    @Transactional(readOnly = true)
    public PainelGestaoDTO calcularGestao(
            PainelCalculoContexto contexto,
            LocalDate referenciaInformada,
            Integer periodoDiasInformado
    ) {
        if (contexto == null) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Contexto do painel não informado.");
        }

        LocalDate hoje = LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        LocalDate referencia = referenciaInformada == null ? hoje.minusDays(1) : referenciaInformada;
        if (referencia.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência não pode estar no futuro.");
        }
        if (referencia.isBefore(hoje.minusYears(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de referência deve estar dentro dos últimos dois anos.");
        }

        int periodoDias = periodoDiasInformado == null ? 1 : periodoDiasInformado;
        if (!List.of(1, 7, 15, 30, 60, 90).contains(periodoDias)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O período deve ser um dos valores permitidos: 1, 7, 15, 30, 60 ou 90 dias."
            );
        }
        LocalDate inicioPeriodo = referencia.minusDays(periodoDias - 1L);
        LocalDate fimPeriodoAnterior = inicioPeriodo.minusDays(1);
        LocalDate inicioPeriodoAnterior = fimPeriodoAnterior.minusDays(periodoDias - 1L);

        return calcularGestaoIntervalo(contexto, referencia, inicioPeriodo, inicioPeriodoAnterior, fimPeriodoAnterior,
                contexto.gestaoLocalId(), contexto.superintendenciaId(), true, painelRepositorio);
    }

    /** Apenas chamado após autorização dos filtros pela consulta analítica. Não usa cache legado. */
    @Transactional(readOnly = true)
    public PainelGestaoDTO calcularGestaoRecortada(PainelCalculoContexto contexto,
            com.example.siggtrat.repositorio.PainelAnaliticoRepositorio.Filtro filtro,
            ResolvedorPeriodoPainel.Periodo periodo, boolean comparar, PainelRepositorio repositorio) {
        return calcularGestaoIntervalo(contexto, periodo.fim(), periodo.inicio(), periodo.anteriorInicio(), periodo.anteriorFim(),
                filtro.gestao(), filtro.superintendencia(), comparar, repositorio);
    }

    private PainelGestaoDTO calcularGestaoIntervalo(PainelCalculoContexto contexto, LocalDate referencia,
            LocalDate inicioPeriodo, LocalDate inicioPeriodoAnterior, LocalDate fimPeriodoAnterior,
            Long gestaoLocalId, Long superintendenciaId, boolean comparar, PainelRepositorio painelRepositorio) {
        int periodoDias = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(inicioPeriodo, referencia) + 1);
        ContextoEscopo scope = new ContextoEscopo(
                contexto.nivel(), contexto.nome(), superintendenciaId, gestaoLocalId);
        PainelGestaoDTO.ResumoEfetivo efetivo = painelRepositorio.obterResumoEfetivo(
                referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.QuadroPessoal quadro = painelRepositorio.obterQuadroPessoal(
                referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.AnaliseCadastro analiseCadastro = painelRepositorio.obterAnaliseCadastro(
                inicioPeriodo, referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.AdmissaoMensal> admissoesMensais = completarAdmissoesMensais(
                referencia, painelRepositorio.obterAdmissoesMensais(
                        referencia, scope.gestaoLocalId(), scope.superintendenciaId()));
        PainelGestaoDTO.SaudeContratual contratos = painelRepositorio.obterSaudeContratual(
                referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.CoberturaFrequencia frequencia = painelRepositorio.obterCoberturaFrequencia(
                referencia, efetivo.empregadosAtivosCadastrados(), scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.AnalisePeriodo periodo = painelRepositorio.obterAnalisePeriodo(
                inicioPeriodo, referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.AnalisePeriodo periodoAnterior = painelRepositorio.obterAnalisePeriodo(
                inicioPeriodoAnterior, fimPeriodoAnterior, scope.gestaoLocalId(), scope.superintendenciaId());
        long ausenciasPeriodo = periodo.faltasNaoJustificadas() + periodo.atestados() + periodo.suspensoes();
        long ausenciasPeriodoAnterior = periodoAnterior.faltasNaoJustificadas()
                + periodoAnterior.atestados() + periodoAnterior.suspensoes();
        PainelGestaoDTO.ComparativoPeriodo comparativo = comparar ? new PainelGestaoDTO.ComparativoPeriodo(
                inicioPeriodoAnterior,
                fimPeriodoAnterior,
                periodoAnterior.registrosOperacionais(),
                ausenciasPeriodoAnterior,
                periodoAnterior.repousosTrabalhados(),
                periodoAnterior.taxaPresenca(),
                periodoAnterior.taxaAbsenteismo(),
                periodo.registrosOperacionais() - periodoAnterior.registrosOperacionais(),
                ausenciasPeriodo - ausenciasPeriodoAnterior,
                periodo.repousosTrabalhados() - periodoAnterior.repousosTrabalhados(),
                arredondarUmaCasa(periodo.taxaPresenca() - periodoAnterior.taxaPresenca()),
                arredondarUmaCasa(periodo.taxaAbsenteismo() - periodoAnterior.taxaAbsenteismo())
        ) : null;
        PainelGestaoDTO.PressaoOperacional pressao = painelRepositorio.obterPressaoOperacional(
                inicioPeriodo, referencia, efetivo.empregadosAtivosCadastrados(), ausenciasPeriodo,
                scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.IntegridadeDados integridade = painelRepositorio.obterIntegridadeDados(
                referencia, efetivo.ativosSemContrato(), scope.gestaoLocalId(), scope.superintendenciaId());
        PainelGestaoDTO.HorizonteContratual horizonte = painelRepositorio.obterHorizonteContratual(
                referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.DistribuicaoStatus> distribuicao = painelRepositorio.obterDistribuicaoStatus(
                inicioPeriodo, referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.SituacaoQuadro> situacaoQuadro = painelRepositorio.obterSituacaoQuadro(
                referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.PadraoSemanal> padraoSemanal = painelRepositorio.obterPadraoSemanal(
                inicioPeriodo, referencia, scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.UnidadeGestao> unidades = painelRepositorio.obterUnidadesGestao(
                referencia, scope.nivel(), scope.gestaoLocalId(), scope.superintendenciaId());
        List<PainelGestaoDTO.SerieTemporal> evolucao = completarSerie(
                referencia,
                periodoDias,
                painelRepositorio.obterEvolucao(
                        referencia, periodoDias, scope.gestaoLocalId(), scope.superintendenciaId())
        );

        return new PainelGestaoDTO(
                referencia,
                LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)),
                new PainelGestaoDTO.Escopo(
                        scope.nivel(), scope.nome(), scope.superintendenciaId(), scope.gestaoLocalId(),
                        contexto.permiteDetalhamentoOperacional()
                ),
                efetivo,
                quadro,
                analiseCadastro,
                admissoesMensais,
                contratos,
                frequencia,
                periodo,
                comparativo,
                pressao,
                integridade,
                horizonte,
                distribuicao,
                situacaoQuadro,
                padraoSemanal,
                unidades,
                evolucao,
                construirAlertas(contexto, contratos, frequencia, periodo, integridade, efetivo),
                List.of(
                        "Previsão, ocupação e saldo contratual do resumo usam Vínculo/Alocação e composição temporal na data consultada.",
                        "A análise cadastral e os campos empregadosAtivosCadastrados/ativosSemContrato descrevem o cadastro legado de empregados, independente dos vínculos canônicos.",
                        "A análise cadastral usa a data de admissão persistida no cadastro e não depende de lançamentos de frequência.",
                        "A série de admissões mostra os 12 meses encerrados na data de referência; meses sem admissão aparecem com zero real, não estimado.",
                        "Efetivo vinculado considera vínculos com alocação registrada e vigente, sem duplicidade, no escopo organizacional efetivo na data.",
                        "A cobertura diária usa a jornada do dia da semana e exclui folga remunerada aprovada e feriado/descanso aplicável ao empregado.",
                        "Jornadas parcialmente configuradas não presumem dia de trabalho quando o dia da semana está ausente; a inconsistência permanece visível na integridade.",
                        "As análises históricas de frequência usam o retrato organizacional gravado no lançamento.",
                        "Referências passadas limitam o quadro cadastral pela data de admissão, mas não reconstroem integralmente status ou jornada de pessoas sem lançamento porque o cadastro não mantém histórico estruturado desses campos.",
                        "As taxas do período usam apenas registros operacionais: presença, presença parcial, atraso, TR, falta, atestado e suspensão.",
                        "A comparação usa os intervalos anterior/atual explicitados na resposta; diferenças de taxa são pontos percentuais.",
                        "DSR perdido é apresentado separadamente e não é somado ao absenteísmo, pois representa consequência em repouso, não ausência em dia programado.",
                        "RT é trabalho em repouso/folga; sua relação com ausências é observacional e não comprova substituição individual.",
                        "A cobertura por unidade usa empregados efetivamente esperados na data, respeitando jornada, folga remunerada aprovada e calendário aplicável.",
                        "O painel não estima horas, FTE ou custo financeiro sem jornada e valor contratual formalmente consolidados.",
                        "Todos os totais são calculados no servidor e filtrados pelo escopo do usuário autenticado."
                )
        );
    }

    private List<PainelGestaoDTO.AdmissaoMensal> completarAdmissoesMensais(
            LocalDate referencia,
            List<PainelGestaoDTO.AdmissaoMensal> valores
    ) {
        Map<LocalDate, Long> porMes = new LinkedHashMap<>();
        if (valores != null) {
            for (PainelGestaoDTO.AdmissaoMensal item : valores) {
                if (item != null && item.mes() != null) {
                    porMes.put(item.mes().withDayOfMonth(1), item.quantidade());
                }
            }
        }
        LocalDate inicio = referencia.withDayOfMonth(1).minusMonths(11);
        List<PainelGestaoDTO.AdmissaoMensal> serie = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            LocalDate mes = inicio.plusMonths(i);
            serie.add(new PainelGestaoDTO.AdmissaoMensal(mes, porMes.getOrDefault(mes, 0L)));
        }
        return serie;
    }

    @Cacheable(
            cacheNames = NomesCache.PAINEL_STATS,
            key = "@redisCacheKey.usuarioAtual()",
            sync = true
    )
    @Transactional(readOnly = true)
    public EstatisticasPainelDTO obterEstatisticas() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirPainelGestao(user);
        if (acessoOrganizacionalServico.ehPresidente(user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A Presidência deve utilizar a visão consolidada por gestor estadual em /api/painel/gestao."
            );
        }
        long total;
        long ativos;
        long inativos;
        long lotacoes;
        long cargos;
        if (acessoOrganizacionalServico.ehGlobal(user)) {
            total = empregadoRepositorio.count();
            ativos = empregadoRepositorio.countByStatus_Descricao(ConstantesDominio.STATUS_ATIVO);
            inativos = Math.max(0, total - ativos);
            lotacoes = lotacaoRepositorio.count();
            cargos = cargoRepositorio.count();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(user)) {
            Long seId = exigirSuperintendencia(user);
            total = empregadoRepositorio.countByGestaoLocalSuperintendenciaId(seId);
            ativos = empregadoRepositorio.countByGestaoLocalSuperintendenciaIdAndStatus_Descricao(seId, ConstantesDominio.STATUS_ATIVO);
            inativos = Math.max(0, total - ativos);
            lotacoes = lotacaoRepositorio.countByGestaoLocalSuperintendenciaId(seId);
            cargos = empregadoRepositorio.contarCargosDistintosPorIdSuperintendencia(seId);
        } else {
            Long gestaoLocalId = exigirGestaoLocal(user);
            total = empregadoRepositorio.countByGestaoLocalId(gestaoLocalId);
            ativos = empregadoRepositorio.countByGestaoLocalIdAndStatus_Descricao(gestaoLocalId, ConstantesDominio.STATUS_ATIVO);
            inativos = Math.max(0, total - ativos);
            lotacoes = lotacaoRepositorio.countByGestaoLocalId(gestaoLocalId);
            cargos = empregadoRepositorio.contarCargosDistintosPorIdGestaoLocal(gestaoLocalId);
        }
        return new EstatisticasPainelDTO(total, ativos, inativos, cargos, lotacoes);
    }

    @Transactional(readOnly = true)
    public Page<AtividadePainelDTO> obterAtividadesRecentes(int size) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirPainelGestao(user);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "dataEvento"));
        // A Presidência recebe somente visão agregada por SE e não nomes de usuários operacionais.
        if (acessoOrganizacionalServico.ehPresidente(user)) return Page.empty(pageable);
        // A mesma autorização temporal e minimização da rota canônica deve
        // preceder também a paginação do painel. O histórico genérico não possui
        // fotografia nominal: não atribuir a um evento antigo o nome atual.
        return historicoConsulta.listarTodos(pageable).map(h -> new AtividadePainelDTO(
                h.id(), h.dataEvento(), h.descricao(),
                h.empregado() == null ? null : h.empregado().id(), null,
                h.usuario() == null ? null : h.usuario().id(),
                h.usuario() == null ? null : h.usuario().username()));
    }

    @Cacheable(
            cacheNames = NomesCache.PAINEL_RANKING,
            key = "@redisCacheKey.usuarioAtual() + ':' + @redisCacheKey.valor(#p0) + ':' "
                    + "+ @redisCacheKey.valor(#p1) + ':' + @redisCacheKey.valor(#p2)",
            sync = true
    )
    @Transactional(readOnly = true)
    public List<PainelRankingDTO> obterRankingLotacoes(LocalDate inicio, LocalDate fim, Long contratoId) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirPainelGestao(user);
        if (acessoOrganizacionalServico.ehPresidente(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Presidência possui visão consolidada por Gestão Estadual, sem detalhamento de Gestão Local, lotação ou usuário operacional.");
        }
        Long gestaoLocalId = acessoOrganizacionalServico.ehGestaoLocal(user) ? exigirGestaoLocal(user) : null;
        Long seId = acessoOrganizacionalServico.ehGestorEstadual(user) ? exigirSuperintendencia(user) : null;
        return painelRepositorio.obterRankingLotacoes(inicio, fim, contratoId, gestaoLocalId, seId);
    }

    private ContextoEscopo resolverEscopo(UsuarioAplicacao user) {
        if (acessoOrganizacionalServico.ehAdministrador(user)) {
            return new ContextoEscopo("NACIONAL", "Brasil — administração técnica", null, null);
        }
        if (acessoOrganizacionalServico.ehGestorNacional(user)) {
            return new ContextoEscopo("NACIONAL", "Brasil — Gestão Nacional", null, null);
        }
        if (acessoOrganizacionalServico.ehPresidente(user)) {
            return new ContextoEscopo("NACIONAL", "Brasil — visão por gestor estadual", null, null);
        }
        if (acessoOrganizacionalServico.ehGestorEstadual(user)) {
            SuperintendenciaEstadual se = acessoOrganizacionalServico.superintendenciaEfetiva(user);
            if (se == null || se.getId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário da superintendência sem vínculo com uma SE ativa.");
            }
            return new ContextoEscopo("SUPERINTENDENCIA", se.getNome(), se.getId(), null);
        }
        GestaoLocal gestaoLocal = acessoOrganizacionalServico.gestaoLocalEfetiva(user);
        if (gestaoLocal == null || gestaoLocal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário Gestão Local sem vínculo com uma unidade de gestão.");
        }
        Long seId = gestaoLocal.getSuperintendencia() != null ? gestaoLocal.getSuperintendencia().getId() : null;
        return new ContextoEscopo("Gestão Local", gestaoLocal.getNome(), seId, gestaoLocal.getId());
    }

    private List<PainelGestaoDTO.AlertaGestao> construirAlertas(
            PainelCalculoContexto contexto,
            PainelGestaoDTO.SaudeContratual contratos,
            PainelGestaoDTO.CoberturaFrequencia frequencia,
            PainelGestaoDTO.AnalisePeriodo periodo,
            PainelGestaoDTO.IntegridadeDados integridade,
            PainelGestaoDTO.ResumoEfetivo efetivo
    ) {
        List<PainelGestaoDTO.AlertaGestao> alertas = new ArrayList<>();
        String rotaContratos = contexto.podeVisualizarContratos() ? "/contratos" : "/painel";
        String rotaDados = contexto.podeVisualizarEmpregados() ? "/empregados" : "/painel";
        String rotaFrequencia = contexto.podeOperarFrequencia()
                ? "/frequencia"
                : "/painel";

        if (integridade.totalInconsistencias() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "INTEGRIDADE_DADOS", "CRITICO", "Inconsistências de integridade",
                    integridade.totalInconsistencias() + " vínculo(s) exigem saneamento antes de decisões contratuais.",
                    rotaDados
            ));
        }
        if (contratos.vencidosMarcadosAtivos() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "CONTRATO_VENCIDO_ATIVO", "CRITICO", "Contrato vencido ainda ativo",
                    contratos.vencidosMarcadosAtivos() + " contrato(s) estão vencidos, mas permanecem marcados como ativos.",
                    rotaContratos
            ));
        }
        if (contratos.vencendoEm30Dias() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "CONTRATO_VENCENDO", "AVISO", "Vencimentos próximos",
                    contratos.vencendoEm30Dias() + " contrato(s) vencem nos próximos 30 dias.",
                    rotaContratos
            ));
        }
        if (efetivo.ativosSemContrato() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "ATIVO_SEM_CONTRATO", "CRITICO", "Empregado ativo sem contrato vigente",
                    efetivo.ativosSemContrato() + " empregado(s) ativos não possuem vínculo com contrato vigente.",
                    rotaDados
            ));
        }
        if (frequencia.empregadosEsperados() > 0 && frequencia.percentualCobertura() < 95.0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "COBERTURA_FREQUENCIA", frequencia.percentualCobertura() < 85.0 ? "CRITICO" : "AVISO",
                    "Cobertura de frequência incompleta",
                    frequencia.pendentes() + " empregado(s) esperados na escala não possuem registro na data de referência.",
                    rotaFrequencia
            ));
        }
        if (frequencia.trabalhosForaEscalaSemRt() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "TRABALHO_FORA_ESCALA", "AVISO", "Trabalho fora da escala sem RT",
                    frequencia.trabalhosForaEscalaSemRt() + " registro(s) de trabalho ocorreram fora da escala sem classificação RT.",
                    rotaFrequencia
            ));
        }
        if (periodo.empregadosComAusenciaRecorrente() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "AUSENCIA_RECORRENTE", "AVISO", "Recorrência de ausências",
                    periodo.empregadosComAusenciaRecorrente() + " empregado(s) tiveram três ou mais ausências no período selecionado.",
                    contexto.podeAcessarRelatorios() ? "/relatorio_absenteismo" : "/painel"
            ));
        }
        if (contratos.semEfetivoPrevisto() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "CONTRATO_SEM_EFETIVO", "CRITICO", "Contrato sem efetivo previsto",
                    contratos.semEfetivoPrevisto() + " contrato(s) ativos não possuem dimensionamento de efetivo válido.",
                    rotaContratos
            ));
        }
        if (contratos.abaixoDoEfetivoPrevisto() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "ABAIXO_EFETIVO", "AVISO", "Efetivo abaixo do previsto",
                    contratos.abaixoDoEfetivoPrevisto() + " contrato(s) estão abaixo do efetivo previsto na referência.",
                    rotaContratos
            ));
        }
        if (contratos.acimaDoEfetivoPrevisto() > 0) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "ACIMA_EFETIVO", "AVISO", "Efetivo acima do previsto",
                    contratos.acimaDoEfetivoPrevisto() + " contrato(s) possuem mais empregados ativos que o efetivo previsto.",
                    rotaContratos
            ));
        }
        if (alertas.isEmpty()) {
            alertas.add(new PainelGestaoDTO.AlertaGestao(
                    "SEM_ALERTAS_CRITICOS", "INFO", "Indicadores consistentes",
                    "Não foram detectadas inconsistências críticas para a data de referência.",
                    "/painel"
            ));
        }
        return alertas;
    }

    private List<PainelGestaoDTO.SerieTemporal> completarSerie(
            LocalDate referencia,
            int periodoDias,
            List<PainelGestaoDTO.SerieTemporal> dados
    ) {
        Map<LocalDate, PainelGestaoDTO.SerieTemporal> porData = new LinkedHashMap<>();
        for (PainelGestaoDTO.SerieTemporal item : dados) porData.put(item.data(), item);
        List<PainelGestaoDTO.SerieTemporal> completa = new ArrayList<>();
        LocalDate inicio = referencia.minusDays(periodoDias - 1L);
        for (int i = 0; i < periodoDias; i++) {
            LocalDate data = inicio.plusDays(i);
            completa.add(porData.getOrDefault(data, new PainelGestaoDTO.SerieTemporal(data, 0, 0, 0, 0)));
        }
        return completa;
    }

    private Long exigirSuperintendencia(UsuarioAplicacao user) {
        if (acessoOrganizacionalServico.superintendenciaEfetiva(user) == null
                || acessoOrganizacionalServico.superintendenciaEfetiva(user).getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário da superintendência sem vínculo com uma SE ativa.");
        }
        return acessoOrganizacionalServico.superintendenciaEfetiva(user).getId();
    }

    private Long exigirGestaoLocal(UsuarioAplicacao user) {
        if (user.getGestaoLocal() == null || user.getGestaoLocal().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário Gestão Local sem vínculo com uma unidade de gestão.");
        }
        return user.getGestaoLocal().getId();
    }

    private double arredondarUmaCasa(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    public record PainelCalculoContexto(
            String nivel,
            String nome,
            Long superintendenciaId,
            Long gestaoLocalId,
            boolean permiteDetalhamentoOperacional,
            boolean podeVisualizarContratos,
            boolean podeVisualizarEmpregados,
            boolean podeOperarFrequencia,
            boolean podeAcessarRelatorios
    ) {}

    private record ContextoEscopo(String nivel, String nome, Long superintendenciaId, Long gestaoLocalId) {}
}
