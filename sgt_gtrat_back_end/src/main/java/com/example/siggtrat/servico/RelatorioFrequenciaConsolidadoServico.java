package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.dto.RelatorioAbsenteismoDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoPaginaDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoResumoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaPaginaDTO;
import com.example.siggtrat.dto.AvisoRevisaoRelatorioDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CalendarioEventoRepositorio;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import com.example.siggtrat.repositorio.HistoricoVinculoContratoRepositorio;
import com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RelatorioFrequenciaConsolidadoServico {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int LIMITE_DIAS = 366;
    private static final int TAMANHO_LOTE_FREQUENCIAS = 1000;
    private static final int TAMANHO_PAGINA_MAXIMO = 500;
    public static final int TAMANHO_LOTE_EXPORTACAO = 5000;

    private final HistoricoVinculoContratoRepositorio historicoRepositorio;
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final ConfiguracaoServico configuracaoServico;
    private final CalendarioEventoRepositorio calendarioEventoRepositorio;
    private final CalculadoraJornadaNoturnaServico calculadoraJornadaNoturna;
    private final RelatorioFrequenciaEscalaRepositorio relatorioEscalaRepositorio;
    private final EscopoAcessoUsuarioServico escopoAcessoUsuarioServico;
    private final EntityManager entityManager;
    private PendenciaAdministrativaServico pendencias;

    public RelatorioFrequenciaConsolidadoServico(
            HistoricoVinculoContratoRepositorio historicoRepositorio,
            FrequenciaRepositorio frequenciaRepositorio,
            ContratoRepositorio contratoRepositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            ConfiguracaoServico configuracaoServico,
            CalendarioEventoRepositorio calendarioEventoRepositorio,
            CalculadoraJornadaNoturnaServico calculadoraJornadaNoturna,
            RelatorioFrequenciaEscalaRepositorio relatorioEscalaRepositorio,
            EscopoAcessoUsuarioServico escopoAcessoUsuarioServico,
            EntityManager entityManager) {
        this.historicoRepositorio = historicoRepositorio;
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.configuracaoServico = configuracaoServico;
        this.calendarioEventoRepositorio = calendarioEventoRepositorio;
        this.calculadoraJornadaNoturna = calculadoraJornadaNoturna;
        this.relatorioEscalaRepositorio = relatorioEscalaRepositorio;
        this.escopoAcessoUsuarioServico = escopoAcessoUsuarioServico;
        this.entityManager = entityManager;
    }

    @Autowired(required = false)
    public void configurarAvisoPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    public RelatorioFrequenciaConsolidadoDTO gerar(LocalDate inicio, LocalDate fim,
                                                    Collection<Long> contratoIds,
                                                    Long lotacaoId, Long jornadaId) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        if (contexto.idsContratos().isEmpty()) return vazio(inicio, fim);

        List<RelatorioFrequenciaConsolidadoDTO.Linha> registros = new ArrayList<>();
        AcumuladorTotais geral = new AcumuladorTotais();
        processarFrequenciaEmLotes(contexto, TAMANHO_LOTE_EXPORTACAO, lote -> {
            registros.addAll(lote);
            lote.forEach(item -> geral.somar(item.totais()));
        });
        registros.sort(Comparator.comparing(RelatorioFrequenciaConsolidadoDTO.Linha::nome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return new RelatorioFrequenciaConsolidadoDTO(true, CalculoFrequenciaServico.CALCULO_VERSAO,
                inicio, fim, List.copyOf(contexto.idsContratos()), List.copyOf(registros), geral.paraDTO(),
                aviso(inicio, fim, contratoIds, lotacaoId, jornadaId));
    }

    public RelatorioFrequenciaPaginaDTO gerarPagina(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, Integer paginaSolicitada, Integer tamanhoSolicitado) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        int tamanho = Math.min(Math.max(tamanhoSolicitado == null ? 100 : tamanhoSolicitado, 25), TAMANHO_PAGINA_MAXIMO);
        long total = contarColaboradores(contexto);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil(total / (double) tamanho);
        int pagina = Math.max(0, paginaSolicitada == null ? 0 : paginaSolicitada);
        if (totalPaginas > 0) pagina = Math.min(pagina, totalPaginas - 1);
        else pagina = 0;

        List<Long> ids = buscarIdsColaboradores(contexto, (long) pagina * tamanho, tamanho);
        List<RelatorioFrequenciaConsolidadoDTO.Linha> registros = gerarLinhasDoLote(contexto, ids);
        return new RelatorioFrequenciaPaginaDTO(
                true,
                CalculoFrequenciaServico.CALCULO_VERSAO,
                inicio,
                fim,
                List.copyOf(contexto.idsContratos()),
                registros,
                total,
                pagina,
                tamanho,
                totalPaginas,
                pagina == 0,
                totalPaginas == 0 || pagina >= totalPaginas - 1,
                aviso(inicio, fim, contratoIds, lotacaoId, jornadaId)
        );
    }

    /**
     * Apuração enxuta de absenteísmo: usa a mesma regra oficial do consolidado,
     * mas não materializa o mapa diário de todos os colaboradores. Isso reduz
     * drasticamente o uso de memória em competências nacionais.
     */
    public List<RelatorioAbsenteismoDTO> gerarAbsenteismo(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        List<RelatorioAbsenteismoDTO> registros = new ArrayList<>();
        processarAbsenteismoEmLotes(contexto, TAMANHO_LOTE_EXPORTACAO, registros::addAll);
        registros.sort(Comparator.comparing(RelatorioAbsenteismoDTO::nome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return List.copyOf(registros);
    }

    public RelatorioAbsenteismoPaginaDTO gerarAbsenteismoPagina(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, Integer paginaSolicitada, Integer tamanhoSolicitado) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        int tamanho = Math.min(Math.max(tamanhoSolicitado == null ? 100 : tamanhoSolicitado, 25), TAMANHO_PAGINA_MAXIMO);
        long total = contarColaboradores(contexto);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil(total / (double) tamanho);
        int pagina = Math.max(0, paginaSolicitada == null ? 0 : paginaSolicitada);
        if (totalPaginas > 0) pagina = Math.min(pagina, totalPaginas - 1);
        else pagina = 0;
        List<Long> ids = buscarIdsColaboradores(contexto, (long) pagina * tamanho, tamanho);
        List<RelatorioAbsenteismoDTO> registros = gerarAbsenteismoDoLote(contexto, ids);
        return new RelatorioAbsenteismoPaginaDTO(
                registros, total, pagina, tamanho, totalPaginas,
                pagina == 0, totalPaginas == 0 || pagina >= totalPaginas - 1,
                aviso(inicio, fim, contratoIds, lotacaoId, jornadaId));
    }

    public RelatorioAbsenteismoResumoDTO gerarResumoAbsenteismo(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, int limiteDisrupcao) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        AbsenteismoResumoAcumulado resumo = new AbsenteismoResumoAcumulado(Math.max(0, limiteDisrupcao));
        processarAbsenteismoEmLotes(contexto, TAMANHO_LOTE_EXPORTACAO, lote -> lote.forEach(resumo::somar));
        return resumo.paraDTO().comRevisao(aviso(inicio, fim, contratoIds, lotacaoId, jornadaId));
    }

    /**
     * Itera o relatório completo por lotes fixos. O consumidor pode persistir em
     * XLSX/PDF temporário sem manter os 268 mil colaboradores na memória.
     */
    public long processarFrequenciaEmLotes(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, int tamanhoLote,
            java.util.function.Consumer<List<RelatorioFrequenciaConsolidadoDTO.Linha>> consumidor) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        return processarFrequenciaEmLotes(contexto, tamanhoLote, consumidor);
    }

    public long processarAbsenteismoEmLotes(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, int tamanhoLote,
            java.util.function.Consumer<List<RelatorioAbsenteismoDTO>> consumidor) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId);
        return processarAbsenteismoEmLotes(contexto, tamanhoLote, consumidor);
    }

    private long processarFrequenciaEmLotes(
            ContextoApuracao contexto, int tamanhoLote,
            java.util.function.Consumer<List<RelatorioFrequenciaConsolidadoDTO.Linha>> consumidor) {
        // A exportação em disco não precisa de COUNT(DISTINCT) antes de começar.
        // Em bases nacionais essa contagem faria uma varredura completa adicional
        // sem acrescentar informação ao arquivo final.
        int lote = Math.min(Math.max(tamanhoLote, 100), TAMANHO_LOTE_EXPORTACAO);
        long ultimoId = 0L;
        long processados = 0L;
        while (true) {
            List<Long> ids = buscarIdsColaboradoresAposId(contexto, ultimoId, lote);
            if (ids.isEmpty()) break;
            consumidor.accept(gerarLinhasDoLote(contexto, ids));
            processados += ids.size();
            ultimoId = ids.get(ids.size() - 1);
            // Impede que entidades de todos os lotes permaneçam referenciadas pelo
            // persistence context durante uma exportação nacional. O lote já foi
            // convertido em DTO e persistido no arquivo temporário antes da limpeza.
            entityManager.clear();
        }
        return processados;
    }

    private long processarAbsenteismoEmLotes(
            ContextoApuracao contexto, int tamanhoLote,
            java.util.function.Consumer<List<RelatorioAbsenteismoDTO>> consumidor) {
        // Mesmo princípio da frequência: persiste cada lote e conta os IDs
        // processados durante a própria paginação por chave.
        int lote = Math.min(Math.max(tamanhoLote, 100), TAMANHO_LOTE_EXPORTACAO);
        long ultimoId = 0L;
        long processados = 0L;
        while (true) {
            List<Long> ids = buscarIdsColaboradoresAposId(contexto, ultimoId, lote);
            if (ids.isEmpty()) break;
            consumidor.accept(gerarAbsenteismoDoLote(contexto, ids));
            processados += ids.size();
            ultimoId = ids.get(ids.size() - 1);
            entityManager.clear();
        }
        return processados;
    }

    private ContextoApuracao prepararContexto(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId) {
        validarPeriodo(inicio, fim);
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        List<Contrato> contratos = resolverContratos(usuario, contratoIds);
        Set<Long> idsContratos = contratos.stream().map(Contrato::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<LocalDate, List<CalendarioEvento>> calendario = calendarioEventoRepositorio
                .findByDataBetween(inicio, fim).stream()
                .filter(evento -> Boolean.TRUE.equals(evento.getAtivo()))
                .collect(java.util.stream.Collectors.groupingBy(CalendarioEvento::getData));

        Long gestaoLocalOperacionalId = null;
        List<RelatorioFrequenciaEscalaRepositorio.EscopoOperacional> escoposOperacionais = List.of();
        if (acessoOrganizacionalServico.ehOperacional(usuario)) {
            gestaoLocalOperacionalId = usuario.getGestaoLocal() != null ? usuario.getGestaoLocal().getId() : -1L;
            escoposOperacionais = escopoAcessoUsuarioServico.obterEscopos(usuario).stream()
                    .map(item -> new RelatorioFrequenciaEscalaRepositorio.EscopoOperacional(
                            item.lotacaoId(), item.jornadaId()))
                    .distinct()
                    .toList();
        }
        return new ContextoApuracao(usuario, inicio, fim, idsContratos, lotacaoId, jornadaId,
                Map.copyOf(calendario), gestaoLocalOperacionalId, escoposOperacionais);
    }

    private long contarColaboradores(ContextoApuracao contexto) {
        if (contexto.idsContratos().isEmpty()) return 0L;
        return relatorioEscalaRepositorio.contarColaboradores(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.gestaoLocalOperacionalId(), contexto.escoposOperacionais());
    }

    private List<Long> buscarIdsColaboradores(ContextoApuracao contexto, long offset, int limite) {
        if (contexto.idsContratos().isEmpty()) return List.of();
        return relatorioEscalaRepositorio.buscarIdsColaboradores(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.gestaoLocalOperacionalId(), contexto.escoposOperacionais(),
                offset, limite);
    }

    private List<Long> buscarIdsColaboradoresAposId(ContextoApuracao contexto, long ultimoId, int limite) {
        if (contexto.idsContratos().isEmpty()) return List.of();
        return relatorioEscalaRepositorio.buscarIdsColaboradoresAposId(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.gestaoLocalOperacionalId(), contexto.escoposOperacionais(),
                ultimoId, limite);
    }

    private List<HistoricoVinculoContrato> carregarAlocacoesDoLote(
            ContextoApuracao contexto, Collection<Long> empregadoIds) {
        if (empregadoIds == null || empregadoIds.isEmpty() || contexto.idsContratos().isEmpty()) return List.of();
        return historicoRepositorio.buscarAlocacoesPorEmpregadosEContratosNoIntervalo(
                        empregadoIds, contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                        contexto.fim().plusDays(1).atStartOfDay(ZONA)).stream()
                .filter(item -> contexto.lotacaoId() == null || Objects.equals(contexto.lotacaoId(), idLotacao(item)))
                .filter(item -> contexto.jornadaId() == null || Objects.equals(contexto.jornadaId(), idJornada(item)))
                .filter(item -> acessoOrganizacionalServico.podeAcessarAlocacaoRelatorio(
                        contexto.usuario(), item.getContrato(), item.getLotacao(), item.getJornada()))
                .toList();
    }

    private Map<Long, List<HistoricoVinculoContrato>> agruparAlocacoesPorEmpregado(
            List<HistoricoVinculoContrato> alocacoes) {
        Map<Long, List<HistoricoVinculoContrato>> alocacoesPorEmpregado = alocacoes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.getEmpregado().getId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toCollection(ArrayList::new)));
        alocacoesPorEmpregado.values().forEach(itens ->
                itens.sort(Comparator.comparing(HistoricoVinculoContrato::getDataInicio).reversed()));
        return alocacoesPorEmpregado;
    }

    private List<RelatorioFrequenciaConsolidadoDTO.Linha> gerarLinhasDoLote(
            ContextoApuracao contexto, Collection<Long> empregadoIds) {
        List<HistoricoVinculoContrato> alocacoes = carregarAlocacoesDoLote(contexto, empregadoIds);
        if (alocacoes.isEmpty()) return List.of();
        Map<Long, List<HistoricoVinculoContrato>> alocacoesPorEmpregado = agruparAlocacoesPorEmpregado(alocacoes);
        Map<ChaveDia, Frequencia> frequencias = carregarFrequenciasEmLotes(
                alocacoesPorEmpregado.keySet(), contexto.inicio(), contexto.fim());

        Map<String, LinhaAcumulada> linhas = new LinkedHashMap<>();
        for (Map.Entry<Long, List<HistoricoVinculoContrato>> entrada : alocacoesPorEmpregado.entrySet()) {
            Long empregadoId = entrada.getKey();
            List<HistoricoVinculoContrato> doEmpregado = entrada.getValue();
            for (LocalDate data = contexto.inicio(); !data.isAfter(contexto.fim()); data = data.plusDays(1)) {
                HistoricoVinculoContrato alocacao = vigenteMaisRecente(doEmpregado, data);
                if (alocacao == null) continue;
                String chave = chaveAlocacao(alocacao);
                LinhaAcumulada linha = linhas.computeIfAbsent(chave, ignorado -> new LinhaAcumulada(alocacao));
                Frequencia frequencia = frequencias.get(new ChaveDia(empregadoId, data));
                if (!frequenciaPertenceAlocacao(frequencia, alocacao)) frequencia = null;
                linha.adicionar(data, criarDia(alocacao, data, frequencia,
                        contexto.calendario().getOrDefault(data, List.of())));
            }
        }
        return linhas.values().stream()
                .map(LinhaAcumulada::paraDTO)
                .sorted(Comparator.comparing(RelatorioFrequenciaConsolidadoDTO.Linha::nome,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private List<RelatorioAbsenteismoDTO> gerarAbsenteismoDoLote(
            ContextoApuracao contexto, Collection<Long> empregadoIds) {
        List<HistoricoVinculoContrato> alocacoes = carregarAlocacoesDoLote(contexto, empregadoIds);
        if (alocacoes.isEmpty()) return List.of();
        Map<Long, List<HistoricoVinculoContrato>> alocacoesPorEmpregado = agruparAlocacoesPorEmpregado(alocacoes);
        Map<ChaveDia, Frequencia> frequencias = carregarFrequenciasEmLotes(
                alocacoesPorEmpregado.keySet(), contexto.inicio(), contexto.fim());

        Map<String, AbsenteismoAcumulado> linhas = new LinkedHashMap<>();
        for (Map.Entry<Long, List<HistoricoVinculoContrato>> entrada : alocacoesPorEmpregado.entrySet()) {
            Long empregadoId = entrada.getKey();
            List<HistoricoVinculoContrato> doEmpregado = entrada.getValue();
            for (LocalDate data = contexto.inicio(); !data.isAfter(contexto.fim()); data = data.plusDays(1)) {
                HistoricoVinculoContrato alocacao = vigenteMaisRecente(doEmpregado, data);
                if (alocacao == null) continue;
                Frequencia frequencia = frequencias.get(new ChaveDia(empregadoId, data));
                if (!frequenciaPertenceAlocacao(frequencia, alocacao)) frequencia = null;
                RelatorioFrequenciaConsolidadoDTO.Dia dia = criarDia(
                        alocacao, data, frequencia, contexto.calendario().getOrDefault(data, List.of()));
                linhas.computeIfAbsent(chaveAlocacao(alocacao), ignorado -> new AbsenteismoAcumulado(alocacao))
                        .adicionar(dia);
            }
        }
        return linhas.values().stream()
                .map(AbsenteismoAcumulado::paraDTO)
                .sorted(Comparator.comparing(RelatorioAbsenteismoDTO::nome,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private record ContextoApuracao(
            UsuarioAplicacao usuario,
            LocalDate inicio,
            LocalDate fim,
            Set<Long> idsContratos,
            Long lotacaoId,
            Long jornadaId,
            Map<LocalDate, List<CalendarioEvento>> calendario,
            Long gestaoLocalOperacionalId,
            List<RelatorioFrequenciaEscalaRepositorio.EscopoOperacional> escoposOperacionais
    ) {}

    private Map<ChaveDia, Frequencia> carregarFrequenciasEmLotes(
            Collection<Long> empregadosIds, LocalDate inicio, LocalDate fim) {
        Map<ChaveDia, Frequencia> resultado = new HashMap<>();
        if (empregadosIds == null || empregadosIds.isEmpty()) return resultado;

        List<Long> ids = new ArrayList<>(empregadosIds);
        for (int inicioLote = 0; inicioLote < ids.size(); inicioLote += TAMANHO_LOTE_FREQUENCIAS) {
            int fimLote = Math.min(inicioLote + TAMANHO_LOTE_FREQUENCIAS, ids.size());
            List<Long> lote = ids.subList(inicioLote, fimLote);
            for (Frequencia frequencia : frequenciaRepositorio
                    .findByEmpregadoIdInAndDataReferenciaBetweenAndValidadeOperacional(
                            lote, inicio, fim, ValidadeOperacionalFrequencia.VALIDA)) {
                resultado.put(new ChaveDia(
                        frequencia.getEmpregado().getId(), frequencia.getDataReferencia()), frequencia);
            }
        }
        return resultado;
    }

    private List<Contrato> resolverContratos(UsuarioAplicacao usuario, Collection<Long> contratoIds) {
        List<Contrato> candidatos;
        if (contratoIds == null || contratoIds.isEmpty()) {
            candidatos = contratoRepositorio.findByAtivoTrue().stream()
                    .filter(item -> acessoOrganizacionalServico.podeAcessarContrato(usuario, item))
                    .toList();
        } else {
            Set<Long> unicos = new LinkedHashSet<>(contratoIds);
            candidatos = contratoRepositorio.findAllById(unicos);
            if (candidatos.size() != unicos.size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais contratos não foram encontrados.");
            }
        }
        if (contratoIds != null && !contratoIds.isEmpty()) {
            for (Contrato contrato : candidatos) acessoOrganizacionalServico.exigirAcessoContrato(usuario, contrato);
        }
        return candidatos.stream().filter(item -> contratoIds != null && !contratoIds.isEmpty()
                        || Boolean.TRUE.equals(item.getAtivo()))
                .sorted(Comparator.comparing(Contrato::getNumeroContrato, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private RelatorioFrequenciaConsolidadoDTO.Dia criarDia(HistoricoVinculoContrato alocacao,
                                                            LocalDate data,
                                                            Frequencia frequencia,
                                                            List<CalendarioEvento> eventos) {
        boolean feriadoCalendario = eventos.stream().anyMatch(evento ->
                eventoDoTipoAplica(evento, ConstantesDominio.EVENTO_FERIADO, alocacao));
        boolean descansoCalendario = eventos.stream().anyMatch(evento ->
                eventoDoTipoAplica(evento, ConstantesDominio.EVENTO_DIA_DESCANSO, alocacao));
        int previstosTurno = minutosPrevistos(alocacao, data.getDayOfWeek());
        int previstos = (feriadoCalendario || descansoCalendario) ? 0 : previstosTurno;
        boolean diaTrabalho = previstos > 0;
        if (frequencia == null) {
            String status = feriadoCalendario
                    ? ConstantesDominio.EVENTO_FERIADO
                    : (descansoCalendario || previstosTurno == 0 ? "DESCANSO" : "SEM_LANCAMENTO");
            return new RelatorioFrequenciaConsolidadoDTO.Dia(data, diaTrabalho, previstos, false,
                    status, null, null, null, null,
                    0, 0, 0, 0, 0, null, false);
        }
        MemoriaCalculoDia memoria = apurarMemoriaDoDia(alocacao, data, frequencia);
        return new RelatorioFrequenciaConsolidadoDTO.Dia(data, diaTrabalho, previstos, true,
                statusParaRelatorio(frequencia.getStatus(), feriadoCalendario),
                frequencia.getEntrada1(), frequencia.getSaida1(),
                frequencia.getEntrada2(), frequencia.getSaida2(), memoria.minutosFisicos(),
                memoria.minutosNoturnosReais(), memoria.minutosNoturnosReduzidos(),
                memoria.minutosAdicionais(), memoria.minutosPagaveis(), memoria.calculoVersao(),
                Boolean.TRUE.equals(frequencia.getOcorrenciaOverrideAtivo()));
    }

    private String statusParaRelatorio(String status, boolean feriadoCalendario) {
        if (!feriadoCalendario) return status;
        String normalizado = normalizar(status);
        if (ConstantesDominio.FREQ_FOLGA.equals(normalizado)
                || ConstantesDominio.FREQ_DESCANSO.equals(normalizado)) {
            return ConstantesDominio.EVENTO_FERIADO;
        }
        return status;
    }

    /**
     * Reconstitui a memória oficial sem alterar a frequência persistida.
     * Batidas completas prevalecem; uma presença automática usa o capturarEstado do
     * turno. Assim, registros antigos também passam a respeitar 22h–05h e a
     * hora reduzida de 52min30s, sem aplicar a redução duas vezes.
     */
    private MemoriaCalculoDia apurarMemoriaDoDia(HistoricoVinculoContrato alocacao,
                                                  LocalDate data,
                                                  Frequencia frequencia) {
        String status = Objects.toString(normalizar(frequencia.getStatus()), "");
        if (!Set.of(
                ConstantesDominio.FREQ_PRESENCA,
                ConstantesDominio.FREQ_PRESENCA_PARCIAL,
                ConstantesDominio.FREQ_ATRASO,
                ConstantesDominio.FREQ_RT,
                ConstantesDominio.FREQ_TR
        ).contains(status)) {
            return MemoriaCalculoDia.vazia(frequencia.getCalculoVersao());
        }

        CalculadoraJornadaNoturnaServico.Resultado recalculado = null;
        if (possuiBatidaCompleta(frequencia)) {
            recalculado = calculadoraJornadaNoturna.calcular(
                    frequencia.getEntrada1(), frequencia.getSaida1(),
                    frequencia.getEntrada2(), frequencia.getSaida2());
        } else if (Set.of(
                ConstantesDominio.FREQ_PRESENCA,
                ConstantesDominio.FREQ_RT,
                ConstantesDominio.FREQ_TR
        ).contains(status)) {
            recalculado = calcularPresencaAutomatica(alocacao, data, frequencia);
        }

        if (recalculado != null && recalculado.minutosFisicos() > 0) {
            int adicionais = valorOuZero(frequencia.getMinutosAdicionais());
            return new MemoriaCalculoDia(
                    recalculado.minutosFisicos(),
                    recalculado.minutosNoturnosReais(),
                    recalculado.minutosNoturnosReduzidos(),
                    adicionais,
                    recalculado.minutosPagaveisBase() + adicionais,
                    CalculoFrequenciaServico.CALCULO_VERSAO
            );
        }

        int fisicos = valorOuZero(frequencia.getMinutosFisicos());
        int noturnosReais = valorOuZero(frequencia.getMinutosNoturnosReais());
        int adicionais = valorOuZero(frequencia.getMinutosAdicionais());
        int pagaveis = frequencia.getMinutosPagaveis() != null
                ? Math.max(0, frequencia.getMinutosPagaveis())
                : duracaoParaMinutos(frequencia.getHorasTrabalhadas());
        int noturnosReduzidos = noturnosReais > 0
                ? (int) Math.round(noturnosReais * 60.0
                / configuracaoServico.obterMinutosPorHoraNoturna())
                : 0;
        return new MemoriaCalculoDia(fisicos, noturnosReais, noturnosReduzidos,
                adicionais, pagaveis, frequencia.getCalculoVersao());
    }

    private boolean possuiBatidaCompleta(Frequencia frequencia) {
        return parCompleto(frequencia.getEntrada1(), frequencia.getSaida1())
                || parCompleto(frequencia.getEntrada2(), frequencia.getSaida2());
    }

    private boolean parCompleto(String entrada, String saida) {
        return entrada != null && !entrada.isBlank() && saida != null && !saida.isBlank();
    }

    private CalculadoraJornadaNoturnaServico.Resultado calcularPresencaAutomatica(
            HistoricoVinculoContrato alocacao,
            LocalDate data,
            Frequencia frequencia) {
        if (Boolean.TRUE.equals(frequencia.getJornadaSnapshotDiaTrabalho())) {
            return calculadoraJornadaNoturna.calcular(
                    frequencia.getJornadaSnapshotEntrada1(), frequencia.getJornadaSnapshotSaida1(),
                    frequencia.getJornadaSnapshotEntrada2(), frequencia.getJornadaSnapshotSaida2());
        }
        Jornada jornada = jornadaEfetiva(alocacao);
        if (jornada == null || jornada.getDias() == null) return null;
        JornadaDiaSemana dia = jornada.getDias().stream()
                .filter(item -> item.getDiaSemana() == data.getDayOfWeek())
                .filter(item -> Boolean.TRUE.equals(item.getDiaTrabalho()))
                .findFirst()
                .orElse(null);
        if (dia == null) return null;
        return calculadoraJornadaNoturna.calcular(
                dia.getEntrada1(), dia.getSaida1(), dia.getEntrada2(), dia.getSaida2());
    }

    /** Uma batida só pode ser agregada à alocação histórica na qual foi gravada. */
    private boolean frequenciaPertenceAlocacao(Frequencia frequencia,
                                                HistoricoVinculoContrato alocacao) {
        if (frequencia == null) return true;
        if (!Objects.equals(frequencia.getContratoSnapshotId(), alocacao.getContrato().getId())) return false;
        Long lotacao = idLotacao(alocacao);
        if (!Objects.equals(frequencia.getLotacaoSnapshotId(), lotacao)) return false;
        Long jornada = idJornada(alocacao);
        return Objects.equals(frequencia.getJornadaSnapshotId(), jornada);
    }

    private boolean eventoDoTipoAplica(CalendarioEvento evento, String tipo,
                                        HistoricoVinculoContrato alocacao) {
        return evento != null && Boolean.TRUE.equals(evento.getAtivo())
                && tipo.equalsIgnoreCase(evento.getTipo()) && eventoAplica(evento, alocacao);
    }

    private boolean eventoAplica(CalendarioEvento evento, HistoricoVinculoContrato alocacao) {
        String abrangencia = normalizar(evento.getAbrangencia());
        GestaoLocal gestaoLocal = alocacao.getContrato().getGestaoLocal();
        Long gestaoLocalId = gestaoLocal != null ? gestaoLocal.getId() : null;
        Long seId = gestaoLocal != null && gestaoLocal.getSuperintendencia() != null
                ? gestaoLocal.getSuperintendencia().getId() : null;
        Long eventoGestaoLocalId = evento.getGestaoLocal() != null ? evento.getGestaoLocal().getId() : null;
        Long eventoSeId = evento.getSuperintendencia() != null ? evento.getSuperintendencia().getId() : null;

        if (eventoGestaoLocalId != null && !Objects.equals(eventoGestaoLocalId, gestaoLocalId)) return false;
        if (eventoSeId != null && !Objects.equals(eventoSeId, seId)) return false;
        if ("NACIONAL".equals(abrangencia)) return true;
        if ("LOTACAO".equals(abrangencia)) {
            return evento.getLotacaoId() != null && Objects.equals(evento.getLotacaoId(), idLotacao(alocacao));
        }
        if ("MUNICIPAL".equals(abrangencia)) {
            Lotacao lotacao = lotacaoEfetiva(alocacao);
            String municipio = texto(alocacao.getLotacaoSnapshotMunicipio(),
                    lotacao != null ? lotacao.getMunicipio() : null);
            return evento.getMunicipio() != null
                    && Objects.equals(normalizar(evento.getMunicipio()), normalizar(municipio));
        }
        if ("ESTADUAL".equals(abrangencia)) {
            if (eventoSeId != null) return Objects.equals(eventoSeId, seId);
            String uf = gestaoLocal != null && gestaoLocal.getSuperintendencia() != null
                    ? gestaoLocal.getSuperintendencia().getUf() : null;
            return evento.getUf() != null && Objects.equals(normalizar(evento.getUf()), normalizar(uf));
        }
        return eventoGestaoLocalId != null && Objects.equals(eventoGestaoLocalId, gestaoLocalId);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private int minutosPrevistos(HistoricoVinculoContrato alocacao, DayOfWeek dia) {
        String mapa = alocacao.getJornadaSnapshotMinutosDia();
        if (mapa != null && !mapa.isBlank()) {
            for (String item : mapa.split(";")) {
                String[] partes = item.split(":", 2);
                if (partes.length == 2 && partes[0].equals(dia.name())) {
                    try { return Math.max(0, Integer.parseInt(partes[1])); }
                    catch (NumberFormatException ignored) { return 0; }
                }
            }
            return 0;
        }
        Jornada jornada = jornadaEfetiva(alocacao);
        if (jornada == null || jornada.getDias() == null) return 0;
        return jornada.getDias().stream().filter(item -> item.getDiaSemana() == dia)
                .filter(item -> Boolean.TRUE.equals(item.getDiaTrabalho()))
                .mapToInt(item -> (int) (intervalo(item.getEntrada1(), item.getSaida1())
                        + intervalo(item.getEntrada2(), item.getSaida2())))
                .findFirst().orElse(0);
    }

    private long intervalo(String entrada, String saida) {
        return com.example.siggtrat.utilitario.UtilitarioHorario.calcularIntervaloMinutos(
                entrada, saida, "entrada da jornada", "saída da jornada");
    }

    private HistoricoVinculoContrato vigenteMaisRecente(List<HistoricoVinculoContrato> itens, LocalDate data) {
        var inicioDia = data.atStartOfDay(ZONA);
        var fimDia = data.plusDays(1).atStartOfDay(ZONA);
        return itens.stream().filter(item -> item.getDataInicio().isBefore(fimDia))
                .filter(item -> item.getDataFim() == null || !item.getDataFim().isBefore(inicioDia))
                .findFirst().orElse(null);
    }

    private String chaveAlocacao(HistoricoVinculoContrato item) {
        return item.getEmpregado().getId() + ":" + item.getContrato().getId() + ":"
                + Objects.toString(idLotacao(item), "-") + ":" + Objects.toString(idJornada(item), "-")
                + ":" + Objects.toString(idCargo(item), "-");
    }

    private Lotacao lotacaoEfetiva(HistoricoVinculoContrato item) {
        return item.getLotacao();
    }

    private Jornada jornadaEfetiva(HistoricoVinculoContrato item) {
        return item.getJornada();
    }

    private Cargo cargoEfetivo(HistoricoVinculoContrato item) {
        return item.getCargo();
    }

    private Long idLotacao(HistoricoVinculoContrato item) {
        Lotacao lotacao = lotacaoEfetiva(item);
        return lotacao != null ? lotacao.getId() : null;
    }
    private Long idJornada(HistoricoVinculoContrato item) {
        Jornada jornada = jornadaEfetiva(item);
        return jornada != null ? jornada.getId() : null;
    }
    private Long idCargo(HistoricoVinculoContrato item) {
        Cargo cargo = cargoEfetivo(item);
        return cargo != null ? cargo.getId() : null;
    }
    private String texto(String capturarEstado, String atual) {
        return capturarEstado != null && !capturarEstado.isBlank() ? capturarEstado : atual != null ? atual : "-";
    }
    private int valorOuZero(Integer valor) { return valor == null ? 0 : Math.max(0, valor); }
    private int duracaoParaMinutos(String valor) {
        if (valor == null || !valor.matches("\\d{1,4}:\\d{2}")) return 0;
        String[] partes = valor.split(":");
        return Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
    }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > LIMITE_DIAS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O relatório aceita no máximo 366 dias.");
    }
    private AvisoRevisaoRelatorioDTO aviso(LocalDate inicio, LocalDate fim,
                                           Collection<Long> contratoIds,
                                           Long lotacaoId, Long jornadaId) {
        return pendencias == null ? AvisoRevisaoRelatorioDTO.definitivo()
                : pendencias.avisoRelatorio(inicio, fim, contratoIds, lotacaoId, jornadaId);
    }
    private RelatorioFrequenciaConsolidadoDTO vazio(LocalDate inicio, LocalDate fim) {
        return vazio(inicio, fim, Set.of());
    }
    private RelatorioFrequenciaConsolidadoDTO vazio(LocalDate inicio, LocalDate fim, Collection<Long> contratos) {
        return new RelatorioFrequenciaConsolidadoDTO(true, CalculoFrequenciaServico.CALCULO_VERSAO,
                inicio, fim, List.copyOf(contratos), List.of(), new AcumuladorTotais().paraDTO());
    }

    private record MemoriaCalculoDia(
            int minutosFisicos,
            int minutosNoturnosReais,
            int minutosNoturnosReduzidos,
            int minutosAdicionais,
            int minutosPagaveis,
            String calculoVersao
    ) {
        private static MemoriaCalculoDia vazia(String versao) {
            return new MemoriaCalculoDia(0, 0, 0, 0, 0, versao);
        }
    }

    private record ChaveDia(Long empregadoId, LocalDate data) {}

    private final class LinhaAcumulada {
        private final HistoricoVinculoContrato alocacao;
        private final Map<String, RelatorioFrequenciaConsolidadoDTO.Dia> dias = new LinkedHashMap<>();
        private final AcumuladorTotais totais = new AcumuladorTotais();
        private LinhaAcumulada(HistoricoVinculoContrato alocacao) { this.alocacao = alocacao; }
        private void adicionar(LocalDate data, RelatorioFrequenciaConsolidadoDTO.Dia dia) {
            dias.put(data.toString(), dia); totais.somar(dia);
        }
        private RelatorioFrequenciaConsolidadoDTO.Linha paraDTO() {
            Empregado e = alocacao.getEmpregado();
            Contrato c = alocacao.getContrato();
            Lotacao lotacao = lotacaoEfetiva(alocacao);
            Jornada jornada = jornadaEfetiva(alocacao);
            Cargo cargo = cargoEfetivo(alocacao);
            return new RelatorioFrequenciaConsolidadoDTO.Linha(chaveAlocacao(alocacao), e.getId(),
                    texto(alocacao.getEmpregadoSnapshotNome(), e.getNome()), c.getId(),
                    texto(alocacao.getContratoSnapshotNumero(), c.getNumeroContrato()), idLotacao(alocacao),
                    texto(alocacao.getLotacaoSnapshotNome(), lotacao != null ? lotacao.getNome() : null),
                    idJornada(alocacao), texto(alocacao.getJornadaSnapshotDescricao(),
                    jornada != null ? jornada.getDescricao() : null), idCargo(alocacao),
                    texto(alocacao.getCargoSnapshotNome(), cargo != null ? cargo.getNome() : null),
                    Map.copyOf(dias), totais.paraDTO());
        }
    }


    private final class AbsenteismoAcumulado {
        private final HistoricoVinculoContrato alocacao;
        private int faltas;
        private int atestados;
        private int dsrPerdido;
        private int episodios;
        private int diasTrabalhaveis;
        private int minutosPrevistos;
        private boolean ausenciaAnterior;

        private AbsenteismoAcumulado(HistoricoVinculoContrato alocacao) {
            this.alocacao = alocacao;
        }

        private void adicionar(RelatorioFrequenciaConsolidadoDTO.Dia dia) {
            if (dia == null) return;
            String status = normalizar(dia.status());
            if (ConstantesDominio.FREQ_FALTA.equals(status)) faltas++;
            if (ConstantesDominio.FREQ_ATESTADO.equals(status)) atestados++;
            if (ConstantesDominio.FREQ_DSR_PERDIDO.equals(status)) dsrPerdido++;
            if (dia.diaTrabalho()) {
                diasTrabalhaveis++;
                boolean ausencia = ConstantesDominio.FREQ_FALTA.equals(status)
                        || ConstantesDominio.FREQ_ATESTADO.equals(status);
                if (ausencia && !ausenciaAnterior) episodios++;
                ausenciaAnterior = ausencia;
            }
            minutosPrevistos += dia.minutosPrevistos();
        }

        private RelatorioAbsenteismoDTO paraDTO() {
            Empregado empregado = alocacao.getEmpregado();
            Lotacao lotacao = lotacaoEfetiva(alocacao);
            Jornada jornada = jornadaEfetiva(alocacao);
            Cargo cargo = cargoEfetivo(alocacao);
            int ausencias = faltas + atestados;
            double taxa = diasTrabalhaveis > 0 ? ausencias * 100.0 / diasTrabalhaveis : 0.0;
            double disrupcao = (double) episodios * episodios * ausencias;
            return new RelatorioAbsenteismoDTO(
                    empregado.getId(),
                    texto(alocacao.getEmpregadoSnapshotNome(), empregado.getNome()),
                    texto(alocacao.getCargoSnapshotNome(), cargo != null ? cargo.getNome() : null),
                    texto(alocacao.getLotacaoSnapshotNome(), lotacao != null ? lotacao.getNome() : null),
                    texto(alocacao.getJornadaSnapshotDescricao(), jornada != null ? jornada.getDescricao() : null),
                    faltas, atestados, dsrPerdido, episodios,
                    Math.round(taxa * 100.0) / 100.0, disrupcao, diasTrabalhaveis, minutosPrevistos);
        }
    }

    private static final class AbsenteismoResumoAcumulado {
        private final int limiteDisrupcao;
        private long faltas;
        private long atestados;
        private long dsrPerdido;
        private long totalColaboradores;
        private long diasTrabalhaveis;
        private long alertasDisrupcao;
        private final PriorityQueue<RelatorioAbsenteismoDTO> maioresTaxas = new PriorityQueue<>(
                Comparator.comparingDouble(RelatorioAbsenteismoDTO::taxaIndividual)
                        .thenComparing(RelatorioAbsenteismoDTO::nome,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        private AbsenteismoResumoAcumulado(int limiteDisrupcao) {
            this.limiteDisrupcao = limiteDisrupcao;
        }

        private void somar(RelatorioAbsenteismoDTO item) {
            if (item == null) return;
            faltas += item.faltas();
            atestados += item.atestados();
            dsrPerdido += item.dsrPerdido();
            diasTrabalhaveis += item.diasTrabalhaveis();
            totalColaboradores++;
            if (item.disrupcao() >= limiteDisrupcao) alertasDisrupcao++;
            if (maioresTaxas.size() < 10) {
                maioresTaxas.offer(item);
            } else if (Comparator.comparingDouble(RelatorioAbsenteismoDTO::taxaIndividual)
                    .compare(item, maioresTaxas.peek()) > 0) {
                maioresTaxas.poll();
                maioresTaxas.offer(item);
            }
        }

        private RelatorioAbsenteismoResumoDTO paraDTO() {
            double taxa = diasTrabalhaveis > 0
                    ? (faltas + atestados) * 100.0 / diasTrabalhaveis : 0.0;
            List<RelatorioAbsenteismoDTO> top = new ArrayList<>(maioresTaxas);
            top.sort(Comparator.comparingDouble(RelatorioAbsenteismoDTO::taxaIndividual).reversed()
                    .thenComparing(RelatorioAbsenteismoDTO::nome,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            return new RelatorioAbsenteismoResumoDTO(
                    faltas, atestados, dsrPerdido, totalColaboradores, diasTrabalhaveis,
                    alertasDisrupcao, Math.round(taxa * 100.0) / 100.0, List.copyOf(top));
        }
    }

    private static final class AcumuladorTotais {
        int trabalhav, lancados, presencas, parciais, faltas, atestados, descansos, semLancamento;
        int previstos, fisicos, noturnosReais, noturnosReduzidos, adicionais, pagaveis;
        void somar(RelatorioFrequenciaConsolidadoDTO.Dia d) {
            if (d.diaTrabalho()) trabalhav++;
            if (d.lancado()) lancados++;
            if ("PRESENCA".equalsIgnoreCase(d.status())) presencas++;
            else if ("PRESENCA_PARCIAL".equalsIgnoreCase(d.status())) parciais++;
            else if ("FALTA".equalsIgnoreCase(d.status())) faltas++;
            else if ("ATESTADO".equalsIgnoreCase(d.status())) atestados++;
            else if ("DESCANSO".equalsIgnoreCase(d.status()) || "FOLGA".equalsIgnoreCase(d.status())
                    || ConstantesDominio.EVENTO_FERIADO.equalsIgnoreCase(d.status())) descansos++;
            else if ("SEM_LANCAMENTO".equalsIgnoreCase(d.status())) semLancamento++;
            previstos += d.minutosPrevistos(); fisicos += d.minutosFisicos();
            noturnosReais += d.minutosNoturnosReais(); noturnosReduzidos += d.minutosNoturnosReduzidos();
            adicionais += d.minutosAdicionais(); pagaveis += d.minutosPagaveis();
        }
        void somar(RelatorioFrequenciaConsolidadoDTO.Totais t) {
            trabalhav += t.diasTrabalhaveis(); lancados += t.diasLancados(); presencas += t.presencas();
            parciais += t.presencasParciais(); faltas += t.faltas(); atestados += t.atestados();
            descansos += t.descansos(); semLancamento += t.semLancamento(); previstos += t.minutosPrevistos();
            fisicos += t.minutosFisicos(); noturnosReais += t.minutosNoturnosReais();
            noturnosReduzidos += t.minutosNoturnosReduzidos(); adicionais += t.minutosAdicionais();
            pagaveis += t.minutosPagaveis();
        }
        RelatorioFrequenciaConsolidadoDTO.Totais paraDTO() {
            return new RelatorioFrequenciaConsolidadoDTO.Totais(trabalhav, lancados, presencas, parciais,
                    faltas, atestados, descansos, semLancamento, previstos, fisicos, noturnosReais,
                    noturnosReduzidos, adicionais, pagaveis);
        }
    }
}
