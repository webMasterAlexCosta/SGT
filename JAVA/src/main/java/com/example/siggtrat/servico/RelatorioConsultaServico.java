package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.RelatorioAbsenteismoDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoPaginaDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoResumoDTO;
import com.example.siggtrat.dto.RelatorioHorasDTO;
import com.example.siggtrat.dto.RelatorioTerceirizadosDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaPaginaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.CalendarioEventoRepositorio;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Relatórios operacionais subordinados ao escopo organizacional do usuário.
 * Administração técnica e Gestão Nacional consultam o escopo nacional; Gestão
 * Estadual e Gestão Local permanecem restritas às respectivas atribuições.
 */
@org.springframework.stereotype.Service
public class RelatorioConsultaServico {

    private final JdbcTemplate jdbcTemplate;
    private final ContratoRepositorio contratoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final CalendarioEventoRepositorio calendarioRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final EscopoAcessoUsuarioServico escopoAcessoUsuarioServico;
    private final RelatorioTerceirizadosServico relatorioTerceirizadosServico;
    private final RelatorioFrequenciaConsolidadoServico relatorioFrequenciaConsolidadoServico;

    public RelatorioConsultaServico(
            JdbcTemplate jdbcTemplate,
            ContratoRepositorio contratoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            CalendarioEventoRepositorio calendarioRepositorio,
            JornadaRepositorio jornadaRepositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            EscopoAcessoUsuarioServico escopoAcessoUsuarioServico,
            RelatorioTerceirizadosServico relatorioTerceirizadosServico,
            RelatorioFrequenciaConsolidadoServico relatorioFrequenciaConsolidadoServico
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.contratoRepositorio = contratoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.calendarioRepositorio = calendarioRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.escopoAcessoUsuarioServico = escopoAcessoUsuarioServico;
        this.relatorioTerceirizadosServico = relatorioTerceirizadosServico;
        this.relatorioFrequenciaConsolidadoServico = relatorioFrequenciaConsolidadoServico;
    }

    /** Listas mínimas e já recortadas pelo escopo do usuário para os filtros de relatório. */
    @Transactional(readOnly = true)
    public com.example.siggtrat.dto.RelatorioFiltrosDTO filtros() {
        return filtros(null, null);
    }

    /** Datas opcionais limitam os contratos à responsabilidade histórica do período. */
    @Transactional(readOnly = true)
    public com.example.siggtrat.dto.RelatorioFiltrosDTO filtros(LocalDate inicio, LocalDate fim) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não possui acesso aos relatórios.");
        }

        List<Lotacao> lotacoes;
        List<Jornada> jornadas;

        if (acessoOrganizacionalServico.ehAdministrador(usuario)
                || acessoOrganizacionalServico.ehGestorNacional(usuario)) {
            lotacoes = lotacaoRepositorio.findAll(org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC, "nome"));
            jornadas = jornadaRepositorio.findAll().stream()
                    .sorted(Comparator.comparing(Jornada::getDescricao,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && acessoOrganizacionalServico.superintendenciaEfetiva(usuario) != null) {
            Long seId = acessoOrganizacionalServico.superintendenciaEfetiva(usuario).getId();
            lotacoes = lotacaoRepositorio.findByGestaoLocalSuperintendenciaIdOrderByNomeAsc(seId);
            jornadas = jornadaRepositorio.findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(seId);
        } else if (usuario.getGestaoLocal() != null && acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            Long gestaoLocalId = usuario.getGestaoLocal().getId();
            lotacoes = lotacaoRepositorio.findByGestaoLocalIdOrderByNomeAsc(gestaoLocalId);
            jornadas = jornadaRepositorio.findByGestaoLocalIdOrderByDescricaoAsc(gestaoLocalId);
        } else if (acessoOrganizacionalServico.ehOperacional(usuario)) {
            Set<Long> lotacaoIds = escopoAcessoUsuarioServico.obterIdsLotacoes(usuario);
            Set<Long> jornadaIds = escopoAcessoUsuarioServico.obterIdsJornadas(usuario);
            lotacoes = lotacaoIds.isEmpty() ? List.of() : lotacaoRepositorio.findAllById(lotacaoIds).stream()
                    .filter(item -> acessoOrganizacionalServico.podeAcessarLotacao(usuario, item))
                    .sorted(Comparator.comparing(Lotacao::getNome,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
            jornadas = jornadaIds.isEmpty() ? List.of() : jornadaRepositorio.findAllById(jornadaIds).stream()
                    .filter(item -> acessoOrganizacionalServico.podeAcessarJornada(usuario, item))
                    .sorted(Comparator.comparing(Jornada::getDescricao,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
        } else {
            lotacoes = List.of();
            jornadas = List.of();
        }

        List<com.example.siggtrat.dto.ContratoOpcaoDTO> contratosDto =
                relatorioFrequenciaConsolidadoServico.contratosParaFiltros(inicio, fim);

        List<com.example.siggtrat.dto.LotacaoOpcaoDTO> lotacoesDto;
        if (lotacoes.isEmpty()) {
            lotacoesDto = List.of();
        } else {
            Map<Long, Lotacao> permitidas = lotacoes.stream()
                    .filter(item -> item != null && item.getId() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            Lotacao::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
            lotacoesDto = lotacaoRepositorio.buscarTodosComTurnosPorIds(new ArrayList<>(permitidas.keySet())).stream()
                    .filter(item -> permitidas.containsKey(item.getId()))
                    .map(lotacao -> new com.example.siggtrat.dto.LotacaoOpcaoDTO(
                            lotacao.getId(), lotacao.getNome(),
                            lotacao.getTurnos().stream()
                                    .filter(turno -> turno != null && turno.getId() != null)
                                    .filter(turno -> !acessoOrganizacionalServico.ehOperacional(usuario)
                                            || escopoAcessoUsuarioServico.podeAcessarJornadaLotacao(
                                                    usuario,
                                                    lotacao.getGestaoLocal() != null ? lotacao.getGestaoLocal().getId() : null,
                                                    lotacao.getId(), turno.getId()))
                                    .map(turno -> new com.example.siggtrat.dto.JornadaResumoDTO(
                                            turno.getId(), turno.getDescricao()))
                                    .sorted(Comparator.comparing(
                                            com.example.siggtrat.dto.JornadaResumoDTO::descricao,
                                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                                    .toList()))
                    .toList();
        }

        List<com.example.siggtrat.dto.JornadaResumoDTO> jornadasDto = jornadas.stream()
                .map(item -> new com.example.siggtrat.dto.JornadaResumoDTO(item.getId(), item.getDescricao()))
                .toList();
        return new com.example.siggtrat.dto.RelatorioFiltrosDTO(contratosDto, lotacoesDto, jornadasDto);
    }

    /**
     * Relatório histórico e autoritativo. Sem contratoIds, inclui todos os
     * contratos sob responsabilidade do usuário no período consultado.
     */
    public RelatorioFrequenciaConsolidadoDTO frequenciaConsolidada(
            LocalDate inicio,
            LocalDate fim,
            List<Long> contratoIds,
            Long lotacaoId,
            Long jornadaId) {
        return relatorioFrequenciaConsolidadoServico
                .gerar(inicio, fim, contratoIds, lotacaoId, jornadaId);
    }

    public RelatorioFrequenciaPaginaDTO frequenciaConsolidadaPaginada(
            LocalDate inicio, LocalDate fim, List<Long> contratoIds,
            Long lotacaoId, Long jornadaId, Integer pagina, Integer tamanho) {
        return relatorioFrequenciaConsolidadoServico
                .gerarPagina(inicio, fim, contratoIds, lotacaoId, jornadaId, pagina, tamanho);
    }

    /**
     * Visão consolidada atual, calculada integralmente no servidor.
     * O navegador recebe somente a página solicitada; indicadores e gráficos
     * sempre consideram todos os registros que atendem aos filtros. O tamanho
     * padrão é 100 e cada chamada é limitada a 5.000 registros para manter
     * previsibilidade de memória e tempo de resposta.
     */
    public RelatorioTerceirizadosDTO consolidadoTerceirizados(
            String busca,
            String status,
            Long contratoId,
            Boolean semContrato,
            Long lotacaoId,
            Integer page,
            Integer size
    ) {
        return relatorioTerceirizadosServico.consultar(
                busca, status, contratoId, semContrato, lotacaoId, page, size);
    }
    public List<RelatorioAbsenteismoDTO> gerarRelatorioAbsenteismo(
            int mes,
            int ano,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId
    ) {
        if (mes < 1 || mes > 12 || ano < 2000 || ano > 2200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência inválida.");
        YearMonth referencia = YearMonth.of(ano, mes);
        Collection<Long> contratos = contratoId == null ? null : List.of(contratoId);
        return relatorioFrequenciaConsolidadoServico.gerarAbsenteismo(
                referencia.atDay(1), referencia.atEndOfMonth(), contratos, lotacaoId, jornadaId);
    }

    public RelatorioAbsenteismoPaginaDTO gerarRelatorioAbsenteismoPaginado(
            int mes, int ano, Long contratoId, Long lotacaoId, Long jornadaId,
            Integer pagina, Integer tamanho) {
        YearMonth referencia = validarCompetencia(mes, ano);
        Collection<Long> contratos = contratoId == null ? null : List.of(contratoId);
        return relatorioFrequenciaConsolidadoServico.gerarAbsenteismoPagina(
                referencia.atDay(1), referencia.atEndOfMonth(), contratos, lotacaoId, jornadaId, pagina, tamanho);
    }

    public RelatorioAbsenteismoResumoDTO gerarResumoAbsenteismo(
            int mes, int ano, Long contratoId, Long lotacaoId, Long jornadaId, int limiteDisrupcao) {
        YearMonth referencia = validarCompetencia(mes, ano);
        Collection<Long> contratos = contratoId == null ? null : List.of(contratoId);
        return relatorioFrequenciaConsolidadoServico.gerarResumoAbsenteismo(
                referencia.atDay(1), referencia.atEndOfMonth(), contratos, lotacaoId, jornadaId, limiteDisrupcao);
    }

    private YearMonth validarCompetencia(int mes, int ano) {
        if (mes < 1 || mes > 12 || ano < 2000 || ano > 2200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência inválida.");
        return YearMonth.of(ano, mes);
    }
    public List<RelatorioHorasDTO> gerarRelatorioHorasTrabalhadas(
            int mes,
            int ano,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId
    ) {
        YearMonth referencia = validarCompetencia(mes, ano);
        if (contratoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato obrigatório.");
        List<RelatorioHorasDTO> resultados = new ArrayList<>();
        // A mesma população, faixas e memória oficial dos relatórios de frequência
        // impedem que a projeção atual elimine horas históricas ou misture contratos.
        relatorioFrequenciaConsolidadoServico.processarFrequenciaEmLotes(
                referencia.atDay(1), referencia.atEndOfMonth(), List.of(contratoId),
                lotacaoId, jornadaId, 500, lote -> {
                    for (RelatorioFrequenciaConsolidadoDTO.Linha linha : lote) {
                        var totais = linha.totais();
                        int minutos = totais.minutosPagaveis();
                        resultados.add(new RelatorioHorasDTO(linha.empregadoId(), linha.nome(),
                                linha.cargo(), linha.lotacao(), linha.turno(), totais.presencas(),
                                totais.presencasParciais(), String.format("%02d:%02d", minutos / 60, minutos % 60),
                                Math.round(totais.minutosNoturnosReduzidos() / 60.0 * 100.0) / 100.0));
                    }
                });
        resultados.sort(Comparator.comparing(RelatorioHorasDTO::nome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(RelatorioHorasDTO::empregadoId));
        return List.copyOf(resultados);
    }

    private ContextoRelatorio prepararContexto(
            int mes, int ano, Long contratoId, Long lotacaoId, Long jornadaId, boolean incluirSemanaAnterior
    ) {
        if (mes < 1 || mes > 12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês inválido.");
        if (ano < 2000 || ano > 2200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano inválido.");

        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirPainelGestao(usuario);
        Contrato contrato = contratoRepositorio.findById(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(usuario, contrato);

        if (lotacaoId != null) {
            Lotacao lotacao = lotacaoRepositorio.findById(lotacaoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
            if (lotacao.getGestaoLocal() == null || contrato.getGestaoLocal() == null
                    || !Objects.equals(lotacao.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())
                    || !acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada no escopo do contrato.");
            }
        }

        List<EmpregadoRelatorio> empregados = carregarEmpregados(contratoId, lotacaoId, jornadaId);
        YearMonth referencia = YearMonth.of(ano, mes);
        LocalDate inicio = referencia.atDay(1);
        LocalDate fim = referencia.atEndOfMonth();
        LocalDate inicioBusca = incluirSemanaAnterior ? inicio.minusDays(6) : inicio;
        return new ContextoRelatorio(referencia, inicio, fim, inicioBusca, empregados);
    }

    private List<EmpregadoRelatorio> carregarEmpregados(Long contratoId, Long lotacaoId, Long jornadaId) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT e.id_empregado, e.nome, e.id_gestao_local,
                       c.nome AS cargo, l.id_lotacao, l.nome AS lotacao, l.municipio,
                       j.id_jornada, j.descricao AS turno,
                       cp.id_superintendencia
                  FROM empregado e
                  JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                  JOIN status_empregado st ON e.id_status = st.id_status
             LEFT JOIN cargo c ON e.id_cargo = c.id_cargo
             LEFT JOIN lotacao l ON e.id_lotacao = l.id_lotacao
             LEFT JOIN jornada j ON e.id_jornada = j.id_jornada
             LEFT JOIN gestao_local cp ON cp.id_gestao_local = e.id_gestao_local
                 WHERE UPPER(TRIM(st.descricao)) = UPPER(?)
                   AND ec.id_contrato = ?
                """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(ConstantesDominio.STATUS_ATIVO);
        parametros.add(contratoId);
        if (lotacaoId != null) {
            sql.append(" AND e.id_lotacao = ? ");
            parametros.add(lotacaoId);
        }
        if (jornadaId != null) {
            sql.append(" AND e.id_jornada = ? ");
            parametros.add(jornadaId);
        }
        sql.append(" ORDER BY e.nome ");

        return jdbcTemplate.query(sql.toString(), (rs, linha) -> new EmpregadoRelatorio(
                rs.getLong("id_empregado"),
                rs.getString("nome"),
                valorOuTraco(rs.getString("cargo")),
                valorOuTraco(rs.getString("lotacao")),
                valorOuTraco(rs.getString("turno")),
                longOuNulo(rs, "id_gestao_local"),
                longOuNulo(rs, "id_superintendencia"),
                longOuNulo(rs, "id_lotacao"),
                rs.getString("municipio")
        ), parametros.toArray());
    }

    private Map<Long, Map<LocalDate, String>> carregarFrequencias(
            List<EmpregadoRelatorio> empregados, LocalDate inicio, LocalDate fim, boolean incluirDetalhes
    ) {
        Map<Long, Map<LocalDate, String>> frequencias = new HashMap<>();
        if (empregados.isEmpty()) return frequencias;
        List<Long> ids = empregados.stream().map(EmpregadoRelatorio::id).toList();
        String marcadores = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id_empregado, data_referencia, status FROM frequencia " +
                "WHERE validade_operacional = 'VALIDA' AND data_referencia BETWEEN ? AND ? AND id_empregado IN (" + marcadores + ")";
        List<Object> parametros = new ArrayList<>();
        parametros.add(Date.valueOf(inicio));
        parametros.add(Date.valueOf(fim));
        parametros.addAll(ids);
        for (Map<String, Object> linha : jdbcTemplate.queryForList(sql, parametros.toArray())) {
            Long empregadoId = ((Number) linha.get("id_empregado")).longValue();
            LocalDate data = ((Date) linha.get("data_referencia")).toLocalDate();
            frequencias.computeIfAbsent(empregadoId, chave -> new HashMap<>())
                    .put(data, (String) linha.get("status"));
        }
        return frequencias;
    }

    private Map<Long, AgregacaoHoras> carregarAgregacoesDeHoras(
            List<EmpregadoRelatorio> empregados, LocalDate inicio, LocalDate fim
    ) {
        Map<Long, AgregacaoHoras> agregacoes = new HashMap<>();
        List<Long> ids = empregados.stream().map(EmpregadoRelatorio::id).toList();
        String marcadores = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id_empregado, status, horas_trabalhadas, horas_noturnas FROM frequencia " +
                "WHERE validade_operacional = 'VALIDA' AND data_referencia BETWEEN ? AND ? AND id_empregado IN (" + marcadores + ")";
        List<Object> parametros = new ArrayList<>();
        parametros.add(Date.valueOf(inicio));
        parametros.add(Date.valueOf(fim));
        parametros.addAll(ids);

        for (Map<String, Object> linha : jdbcTemplate.queryForList(sql, parametros.toArray())) {
            Long empregadoId = ((Number) linha.get("id_empregado")).longValue();
            AgregacaoHoras agregacao = agregacoes.computeIfAbsent(empregadoId, chave -> new AgregacaoHoras());
            String status = normalizar((String) linha.get("status"));
            if (ConstantesDominio.FREQ_PRESENCA.equals(status)) agregacao.presencas++;
            else if (ConstantesDominio.FREQ_PRESENCA_PARCIAL.equals(status)) agregacao.presencasParciais++;

            String horasTrabalhadas = (String) linha.get("horas_trabalhadas");
            if (horasTrabalhadas != null && horasTrabalhadas.matches("\\d{1,3}:\\d{2}")) {
                String[] partes = horasTrabalhadas.split(":");
                agregacao.totalMinutosTrabalhados += Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
            }
            Number horasNoturnas = (Number) linha.get("horas_noturnas");
            if (horasNoturnas != null) agregacao.totalHorasNoturnas += horasNoturnas.doubleValue();
        }
        return agregacoes;
    }

    private boolean houveFaltaNaSemana(
            Map<LocalDate, String> frequencias,
            List<CalendarioEvento> eventos,
            EmpregadoRelatorio empregado,
            LocalDate descanso
    ) {
        for (int retrocesso = 1; retrocesso <= 6; retrocesso++) {
            LocalDate data = descanso.minusDays(retrocesso);
            if (ConstantesDominio.FREQ_FALTA.equals(normalizar(frequencias.get(data)))) {
                String evento = obterTipoEventoAplicavel(eventos, empregado, data);
                if (!ConstantesDominio.EVENTO_FERIADO.equalsIgnoreCase(evento)) return true;
            }
        }
        return false;
    }

    private String obterTipoEventoAplicavel(
            List<CalendarioEvento> eventos, EmpregadoRelatorio empregado, LocalDate data
    ) {
        String tipoEncontrado = null;
        for (CalendarioEvento evento : eventos) {
            if (!Boolean.TRUE.equals(evento.getAtivo()) || !Objects.equals(evento.getData(), data)
                    || !eventoAplicaAoEmpregado(evento, empregado)) continue;
            if (ConstantesDominio.EVENTO_FERIADO.equalsIgnoreCase(evento.getTipo())) return evento.getTipo();
            if (ConstantesDominio.EVENTO_DIA_DESCANSO.equalsIgnoreCase(evento.getTipo())) tipoEncontrado = evento.getTipo();
            else if (tipoEncontrado == null) tipoEncontrado = evento.getTipo();
        }
        return tipoEncontrado;
    }

    private boolean eventoAplicaAoEmpregado(CalendarioEvento evento, EmpregadoRelatorio empregado) {
        String abrangencia = normalizar(evento.getAbrangencia());
        if ("NACIONAL".equals(abrangencia)) return true;

        Long eventoSeId = evento.getSuperintendencia() != null ? evento.getSuperintendencia().getId() : null;
        Long eventoGestaoLocalId = evento.getGestaoLocal() != null ? evento.getGestaoLocal().getId() : null;
        if (eventoSeId != null && !Objects.equals(eventoSeId, empregado.superintendenciaId())) return false;
        if (eventoGestaoLocalId != null && !Objects.equals(eventoGestaoLocalId, empregado.gestaoLocalId())) return false;

        if ("LOTACAO".equals(abrangencia)) {
            return evento.getLotacaoId() != null && Objects.equals(evento.getLotacaoId(), empregado.lotacaoId());
        }
        if ("MUNICIPAL".equals(abrangencia)) {
            return evento.getMunicipio() != null && empregado.municipio() != null
                    && normalizar(evento.getMunicipio()).equals(normalizar(empregado.municipio()));
        }
        if ("ESTADUAL".equals(abrangencia)) {
            return eventoSeId != null && Objects.equals(eventoSeId, empregado.superintendenciaId());
        }
        // Eventos antigos com GestaoLocal definida continuam locais à GestaoLocal.
        return eventoGestaoLocalId != null && Objects.equals(eventoGestaoLocalId, empregado.gestaoLocalId());
    }

    private static String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private static Long longOuNulo(java.sql.ResultSet rs, String coluna) throws java.sql.SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    private record EmpregadoRelatorio(
            Long id, String nome, String cargo, String lotacao, String turno,
            Long gestaoLocalId, Long superintendenciaId, Long lotacaoId, String municipio
    ) {}

    private record ContextoRelatorio(
            YearMonth mesReferencia, LocalDate inicioMes, LocalDate fimMes,
            LocalDate inicioBusca, List<EmpregadoRelatorio> empregados
    ) {}

    private static final class AgregacaoHoras {
        private int presencas;
        private int presencasParciais;
        private int totalMinutosTrabalhados;
        private double totalHorasNoturnas;
    }
}
