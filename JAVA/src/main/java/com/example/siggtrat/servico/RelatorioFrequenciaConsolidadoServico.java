package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio.CandidatoInvalidacao;
import com.example.siggtrat.dto.IdentidadeHistoricaResolvidaDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoPaginaDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoResumoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaPaginaDTO;
import com.example.siggtrat.dto.AvisoRevisaoRelatorioDTO;
import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CalendarioEventoRepositorio;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import com.example.siggtrat.repositorio.HistoricoVinculoContratoRepositorio;
import com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio;
import com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio.EscopoConsulta;
import com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio.FaixaResponsabilidade;
import com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio.FonteAlocacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
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

    private final IdentidadeHistoricaServico identidadeHistorica;
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
    private final MotorTemporalContratoServico motorContrato;
    private final GestaoContratoTemporalServico gestaoContratoTemporal;
    private final MinimizadorDadosPessoaisServico minimizador;
    private PendenciaAdministrativaServico pendencias;
    private ResolvedorContextoTemporalFrequenciaServico resolvedorContexto;

    public RelatorioFrequenciaConsolidadoServico(
            IdentidadeHistoricaServico identidadeHistorica,
            HistoricoVinculoContratoRepositorio historicoRepositorio,
            FrequenciaRepositorio frequenciaRepositorio,
            ContratoRepositorio contratoRepositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            ConfiguracaoServico configuracaoServico,
            CalendarioEventoRepositorio calendarioEventoRepositorio,
            CalculadoraJornadaNoturnaServico calculadoraJornadaNoturna,
            RelatorioFrequenciaEscalaRepositorio relatorioEscalaRepositorio,
            EscopoAcessoUsuarioServico escopoAcessoUsuarioServico,
            EntityManager entityManager,
            MotorTemporalContratoServico motorContrato,
            GestaoContratoTemporalServico gestaoContratoTemporal,
            MinimizadorDadosPessoaisServico minimizador) {
        this.identidadeHistorica = identidadeHistorica;
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
        this.motorContrato = motorContrato;
        this.gestaoContratoTemporal = gestaoContratoTemporal;
        this.minimizador = minimizador;
    }

    @Autowired(required = false)
    public void configurarAvisoPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    @Autowired(required = false)
    public void configurarResolvedorContexto(ResolvedorContextoTemporalFrequenciaServico resolvedorContexto) {
        this.resolvedorContexto = resolvedorContexto;
    }

    /**
     * A emissão mantém as mesmas raízes travadas pelos comandos de frequência,
     * ocorrência e cobertura temporal. Descoberta não é apuração: nenhuma fonte
     * do relatório é lida antes de adquirir todos os Contratos em ordem de ID.
     * O chamador usa READ_COMMITTED, inclusive se esperou por uma retificação.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = false)
    public List<Long> bloquearFontesParaEmissao(Collection<Long> contratoIds) {
        EscopoConsulta escopo = escopoAtual(acessoOrganizacionalServico.usuarioAtual());
        Set<Long> solicitados = contratoIds == null ? Set.of() : new TreeSet<>(contratoIds);
        List<Long> candidatos = relatorioEscalaRepositorio.listarContratosCandidatos(
                solicitados, escopo.gestaoLocalId(), escopo.superintendenciaId());
        if (!solicitados.isEmpty() && !new HashSet<>(candidatos).containsAll(solicitados)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Um ou mais contratos não foram encontrados no seu escopo.");
        }
        List<Long> ordenados = candidatos.stream().distinct().sorted().toList();
        for (Long id : ordenados) {
            Contrato contrato = entityManager.find(Contrato.class, id);
            if (contrato == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Um contrato deixou de estar disponível durante a emissão. Refaça a solicitação.");
            // Um Contrato pode ter sido anexado antes da espera. Renovar também
            // seu estado evita reutilizar a fotografia do primeiro nível JPA.
            entityManager.refresh(contrato, LockModeType.PESSIMISTIC_WRITE);
        }
        List<Long> atuais = relatorioEscalaRepositorio.listarContratosCandidatos(
                solicitados, escopo.gestaoLocalId(), escopo.superintendenciaId()).stream()
                .distinct().sorted().toList();
        if (!ordenados.equals(atuais)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O conjunto de contratos foi alterado durante a emissão. Refaça a solicitação.");
        // O conteúdo usa exatamente esta lista, inclusive quando o filtro do
        // usuário significa todo o escopo; não descobre fontes sem lock depois.
        return ordenados;
    }

    public RelatorioFrequenciaConsolidadoDTO gerar(LocalDate inicio, LocalDate fim,
                                                    Collection<Long> contratoIds,
                                                    Long lotacaoId, Long jornadaId) {
        return gerarNoEscopo(inicio, fim, contratoIds, lotacaoId, jornadaId, null, null);
    }

    /** O recorte publicado restringe a autorização atual, inclusive nas faixas históricas. */
    public RelatorioFrequenciaConsolidadoDTO gerarNoEscopo(LocalDate inicio, LocalDate fim,
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            Long gestaoLocalId, Long superintendenciaId) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId,
                gestaoLocalId, superintendenciaId);
        AvisoRevisaoRelatorioDTO revisao = avisoNoEscopo(inicio, fim, contratoIds, lotacaoId, jornadaId,
                gestaoLocalId, superintendenciaId);
        if (contexto.idsContratos().isEmpty()) {
            return new RelatorioFrequenciaConsolidadoDTO(true, CalculoFrequenciaServico.CALCULO_VERSAO,
                    inicio, fim, List.of(), List.of(), new AcumuladorTotais().paraDTO(),
                    revisao);
        }

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
                revisao);
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
        return gerarAbsenteismoNoEscopo(inicio, fim, contratoIds, lotacaoId, jornadaId, null, null);
    }

    public List<RelatorioAbsenteismoDTO> gerarAbsenteismoNoEscopo(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, Long gestaoLocalId, Long superintendenciaId) {
        ContextoApuracao contexto = prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId,
                gestaoLocalId, superintendenciaId);
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
        // O cursor já percorre IDs distintos de colaboradores. As linhas do
        // mesmo ID podem representar várias alocações e não aumentam população.
        resumo.totalColaboradores = processarAbsenteismoEmLotes(contexto, TAMANHO_LOTE_EXPORTACAO,
                lote -> lote.forEach(resumo::somar));
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
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) entityManager.clear();
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
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) entityManager.clear();
        }
        return processados;
    }

    /**
     * Compara somente os dias da fonte alterada, usando a mesma seleção canônica
     * e o mesmo cálculo da emissão. Inclui SEM_LANCAMENTO e município fotografado.
     * O chamador mantém os Contratos bloqueados e não altera as frequências.
     */
    public boolean calendarioAlteraResultado(CandidatoInvalidacao emissao,
            CalendarioEvento anterior, CalendarioEvento novo) {
        Set<LocalDate> datas = new TreeSet<>();
        if (anterior != null) datas.add(anterior.getData());
        if (novo != null) datas.add(novo.getData());
        EscopoConsulta atual = escopoAtual(acessoOrganizacionalServico.usuarioAtual());
        Long gestao = intersecao(atual.gestaoLocalId(), emissao.getGestaoLocalEscopoId());
        Long se = intersecao(atual.superintendenciaId(), emissao.getSuperintendenciaEscopoId());
        Collection<Long> filtro = emissao.getContratoIdsCsv() == null || emissao.getContratoIdsCsv().isBlank()
                ? List.of() : Arrays.stream(emissao.getContratoIdsCsv().split(",")).map(String::trim).map(Long::valueOf).toList();
        List<Long> contratos = relatorioEscalaRepositorio.listarContratosCandidatos(filtro, gestao, se);
        if (contratos.isEmpty()) return false;
        Long fonteId = novo == null ? anterior.getId() : novo.getId();
        for (LocalDate data : datas) {
            if (data.isBefore(emissao.getInicio()) || data.isAfter(emissao.getFim())) continue;
            ContextoApuracao contexto = prepararContexto(data, data, contratos, emissao.getLotacaoId(),
                    emissao.getJornadaId(), emissao.getGestaoLocalEscopoId(), emissao.getSuperintendenciaEscopoId());
            List<CalendarioEvento> demais = contexto.calendario().getOrDefault(data, List.of()).stream()
                    .filter(e -> !Objects.equals(e.getId(), fonteId)).toList();
            List<CalendarioEvento> antes = calendarioComFonte(demais, anterior, data);
            List<CalendarioEvento> depois = calendarioComFonte(demais, novo, data);
            long ultimoId = 0L;
            while (true) {
                List<Long> ids = buscarIdsColaboradoresAposId(contexto, ultimoId, TAMANHO_LOTE_FREQUENCIAS);
                if (ids.isEmpty()) break;
                List<HistoricoVinculoContrato> alocacoes = carregarAlocacoesDoLote(contexto, ids);
                Map<Long, List<HistoricoVinculoContrato>> porEmpregado = agruparAlocacoesPorEmpregado(alocacoes);
                Map<ChaveDia, Frequencia> frequencias = carregarFrequenciasEmLotes(ids, data, data);
                boolean alterou = false;
                for (var entrada : porEmpregado.entrySet()) {
                    HistoricoVinculoContrato alocacao = vigenteMaisRecente(entrada.getValue(), data, contexto.escopo());
                    if (alocacao == null) continue;
                    Frequencia frequencia = frequencias.get(new ChaveDia(entrada.getKey(), data));
                    if (!frequenciaPertenceAlocacao(frequencia, alocacao)) frequencia = null;
                    var diaAntes = criarDia(alocacao, data, frequencia, antes);
                    var diaDepois = criarDia(alocacao, data, frequencia, depois);
                    if (resultadoDiarioDiferente(emissao.getTipo(), diaAntes, diaDepois)) { alterou = true; break; }
                }
                liberarFontesCalendario(alocacoes, frequencias.values());
                if (alterou) return true;
                ultimoId = ids.get(ids.size() - 1);
            }
        }
        return false;
    }

    private List<CalendarioEvento> calendarioComFonte(List<CalendarioEvento> demais,
            CalendarioEvento fonte, LocalDate data) {
        List<CalendarioEvento> eventos = new ArrayList<>(demais);
        if (fonte != null && data.equals(fonte.getData()) && Boolean.TRUE.equals(fonte.getAtivo()) && !fonte.isCancelado())
            eventos.add(fonte);
        return eventos;
    }

    private boolean resultadoDiarioDiferente(TipoRelatorioAdministrativo tipo,
            RelatorioFrequenciaConsolidadoDTO.Dia antes, RelatorioFrequenciaConsolidadoDTO.Dia depois) {
        if (tipo != TipoRelatorioAdministrativo.ABSENTEISMO) return !antes.equals(depois);
        if (antes.diaTrabalho() != depois.diaTrabalho() || antes.minutosPrevistos() != depois.minutosPrevistos()) return true;
        for (String classificacao : List.of(ConstantesDominio.FREQ_FALTA, ConstantesDominio.FREQ_ATESTADO,
                ConstantesDominio.FREQ_DSR_PERDIDO)) {
            if (classificacao.equalsIgnoreCase(antes.status()) != classificacao.equalsIgnoreCase(depois.status())) return true;
        }
        return false;
    }

    private void liberarFontesCalendario(List<HistoricoVinculoContrato> alocacoes, Collection<Frequencia> frequencias) {
        // Não limpa o persistence context do comando: Calendário e Contratos
        // bloqueados continuam gerenciados; libera somente as fontes lidas no lote.
        for (HistoricoVinculoContrato h : alocacoes) {
            if (!(h instanceof FotografiaCanonica) && entityManager.contains(h)) entityManager.detach(h);
            if (h.getEmpregado() != null && entityManager.contains(h.getEmpregado())) entityManager.detach(h.getEmpregado());
            if (h instanceof FotografiaCanonica canonica) {
                AlocacaoVinculo a = entityManager.getReference(AlocacaoVinculo.class, canonica.alocacaoId);
                VinculoContratual v = a.getVinculo();
                Pessoa p = v.getPessoa();
                if (entityManager.contains(a)) entityManager.detach(a);
                if (entityManager.contains(v)) entityManager.detach(v);
                if (entityManager.contains(p)) entityManager.detach(p);
            }
        }
        frequencias.stream().filter(entityManager::contains).forEach(entityManager::detach);
    }

    private ContextoApuracao prepararContexto(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId) {
        return prepararContexto(inicio, fim, contratoIds, lotacaoId, jornadaId, null, null);
    }

    private ContextoApuracao prepararContexto(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId, Long gestaoLocalId, Long superintendenciaId) {
        validarPeriodo(inicio, fim);
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        EscopoConsulta atual = escopoAtual(usuario);
        EscopoConsulta escopoBase = new EscopoConsulta(
                intersecao(atual.gestaoLocalId(), gestaoLocalId),
                intersecao(atual.superintendenciaId(), superintendenciaId),
                atual.operacional(), atual.pares(), List.of(), atual.conhecidoAte());
        List<Contrato> contratos = resolverContratos(contratoIds, escopoBase);
        List<FaixaResponsabilidade> faixas = reconstruirFaixas(contratos, inicio, fim, escopoBase);
        Set<Long> idsContratos = faixas.stream().map(FaixaResponsabilidade::contratoId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<LocalDate, List<CalendarioEvento>> calendario = calendarioEventoRepositorio
                .findByDataBetween(inicio, fim).stream()
                .filter(evento -> Boolean.TRUE.equals(evento.getAtivo()))
                .collect(java.util.stream.Collectors.groupingBy(CalendarioEvento::getData));

        return new ContextoApuracao(usuario, inicio, fim, idsContratos, lotacaoId, jornadaId,
                Map.copyOf(calendario), new EscopoConsulta(escopoBase.gestaoLocalId(),
                        escopoBase.superintendenciaId(), escopoBase.operacional(), escopoBase.pares(), faixas,
                        escopoBase.conhecidoAte()));
    }

    private long contarColaboradores(ContextoApuracao contexto) {
        if (contexto.idsContratos().isEmpty()) return 0L;
        return relatorioEscalaRepositorio.contarColaboradores(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.escopo());
    }

    private List<Long> buscarIdsColaboradores(ContextoApuracao contexto, long offset, int limite) {
        if (contexto.idsContratos().isEmpty()) return List.of();
        return relatorioEscalaRepositorio.buscarIdsColaboradores(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.escopo(),
                offset, limite);
    }

    private List<Long> buscarIdsColaboradoresAposId(ContextoApuracao contexto, long ultimoId, int limite) {
        if (contexto.idsContratos().isEmpty()) return List.of();
        return relatorioEscalaRepositorio.buscarIdsColaboradoresAposId(
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(),
                contexto.jornadaId(), contexto.escopo(),
                ultimoId, limite);
    }

    private List<HistoricoVinculoContrato> carregarAlocacoesDoLote(
            ContextoApuracao contexto, Collection<Long> empregadoIds) {
        if (empregadoIds == null || empregadoIds.isEmpty() || contexto.idsContratos().isEmpty()) return List.of();
        List<FonteAlocacao> fontes = relatorioEscalaRepositorio.buscarFontesDoLote(empregadoIds,
                contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                contexto.fim().plusDays(1).atStartOfDay(ZONA), contexto.lotacaoId(), contexto.jornadaId(), contexto.escopo());
        Set<Long> historicos = fontes.stream().map(FonteAlocacao::historicoId).filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<HistoricoVinculoContrato> resultado = new ArrayList<>();
        if (!historicos.isEmpty()) {
            resultado.addAll(historicoRepositorio.buscarAlocacoesPorEmpregadosEContratosNoIntervalo(
                            empregadoIds, contexto.idsContratos(), contexto.inicio().atStartOfDay(ZONA),
                            contexto.fim().plusDays(1).atStartOfDay(ZONA)).stream()
                    .filter(h -> historicos.contains(h.getId())).toList());
        }
        resultado.addAll(carregarFotografiasCanonicas(fontes, contexto.inicio(), contexto.fim(), contexto.escopo().conhecidoAte()));
        return resultado;
    }

    /** Adapta a fonte canônica ao cálculo existente, sem persistir históricos artificiais. */
    private List<HistoricoVinculoContrato> carregarFotografiasCanonicas(List<FonteAlocacao> fontes,
            LocalDate inicio, LocalDate fim, Instant conhecimento) {
        List<FonteAlocacao> canonicas = fontes.stream().filter(f -> f.alocacaoId() != null).toList();
        if (canonicas.isEmpty()) return List.of();
        Set<Long> alocacaoIds = canonicas.stream().map(FonteAlocacao::alocacaoId).collect(java.util.stream.Collectors.toSet());
        Set<Long> empregadoIds = canonicas.stream().map(FonteAlocacao::empregadoId).collect(java.util.stream.Collectors.toSet());
        Set<Long> vinculoIds = canonicas.stream().map(FonteAlocacao::vinculoId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Empregado> empregados = entityManager.createQuery(
                        "select e from Empregado e where e.id in :ids", Empregado.class)
                .setParameter("ids", empregadoIds).getResultList().stream()
                .collect(java.util.stream.Collectors.toMap(Empregado::getId, e -> e));
        Map<Long, AlocacaoVinculo> alocacoes = entityManager.createQuery("""
                select distinct a from AlocacaoVinculo a
                  join fetch a.vinculo v join fetch v.pessoa join fetch v.contrato
                  join fetch a.lotacao join fetch a.cargo join fetch a.jornada j
                  left join fetch j.dias
                 where a.id in :ids
                """, AlocacaoVinculo.class).setParameter("ids", alocacaoIds).getResultList().stream()
                .collect(java.util.stream.Collectors.toMap(AlocacaoVinculo::getId, a -> a));
        Map<Long, NomesAlocacao> nomes = new HashMap<>();
        // Cada evento contém fotografias anterior/nova. A ordem abaixo é a mesma
        // usada pelo motor para sua projeção atual, sem multiplicar a população.
        for (EventoAlocacaoVinculo e : entityManager.createQuery("""
                select e from EventoAlocacaoVinculo e left join fetch e.alocacaoNova
                 where e.vinculo.id in :ids and e.registradoEm <= :conhecimento
                 order by e.registradoEm, e.id
                """, EventoAlocacaoVinculo.class).setParameter("ids", vinculoIds)
                .setParameter("conhecimento", com.example.siggtrat.utilitario.PrecisaoTemporal
                        .limiteInclusivoMicros(conhecimento)).getResultList()) {
            if (e.getAlocacaoAnteriorId() != null && e.getEstadoAnterior() != null)
                nomes.put(e.getAlocacaoAnteriorId(), new NomesAlocacao(e.getLotacaoAnteriorNome(),
                        e.getJornadaAnteriorDescricao(), e.getCargoAnteriorNome()));
            if (e.getAlocacaoNova() != null && e.getEstadoNovo() != null)
                nomes.put(e.getAlocacaoNova().getId(), new NomesAlocacao(e.getLotacaoNovaNome(),
                        e.getJornadaNovaDescricao(), e.getCargoNovoNome()));
        }
        // Duas leituras por vínculo; cada data é resolvida em memória respeitando K.
        Map<Long, Map<LocalDate, IdentidadeHistoricaResolvidaDTO>> nomesPessoas = new HashMap<>();
        List<HistoricoVinculoContrato> resultado = new ArrayList<>();
        for (FonteAlocacao fonte : canonicas) {
            AlocacaoVinculo a = alocacoes.get(fonte.alocacaoId());
            if (a == null) throw new IllegalStateException("Alocação da população do relatório não encontrada.");
            Map<LocalDate, IdentidadeHistoricaResolvidaDTO> nomesDoPeriodo = nomesPessoas.computeIfAbsent(
                    a.getVinculo().getId(), id -> identidadeHistorica.resolverPeriodo(
                            a.getVinculo().getPessoa(), a.getVinculo(), inicio, fim, conhecimento));
            FotografiaCanonica h = new FotografiaCanonica(a.getVinculo().getPessoa().getId(),
                    fonte.vinculoId(), fonte.alocacaoId(), nomesDoPeriodo);
            h.setEmpregado(empregados.get(fonte.empregadoId())); h.setContrato(a.getVinculo().getContrato());
            h.setLotacao(a.getLotacao()); h.setJornada(a.getJornada()); h.setCargo(a.getCargo());
            h.setDataInicio(fonte.inicio().atStartOfDay(ZONA));
            h.setDataFim(fonte.fim().plusDays(1).atStartOfDay(ZONA).minusNanos(1));
            h.setGestaoLocalSnapshotId(fonte.gestaoLocalId()); h.setSuperintendenciaSnapshotId(fonte.superintendenciaId());
            h.setContratoSnapshotNumero(fonte.numeroContrato());
            NomesAlocacao foto = nomes.get(a.getId());
            if (foto != null) {
                h.setLotacaoSnapshotNome(foto.lotacao()); h.setJornadaSnapshotDescricao(foto.jornada());
                h.setCargoSnapshotNome(foto.cargo());
            }
            var configuracao = resolvedorContexto == null
                    ? new ConsultaProvaHistoricaContratoServico.Configuracao(a.getLotacaoNomeFotografia(),
                            a.getJornadaDescricaoFotografia(),a.getCargoNomeFotografia(),
                            a.getLotacaoMunicipioFotografia(),a.getJornadaSemanalFotografia())
                    : resolvedorContexto.resolverConfiguracao(a,fonte.inicio(),conhecimento);
            if (configuracao.lotacaoNome() != null) h.setLotacaoSnapshotNome(configuracao.lotacaoNome());
            if (configuracao.jornadaDescricao() != null) h.setJornadaSnapshotDescricao(configuracao.jornadaDescricao());
            if (configuracao.cargoNome() != null) h.setCargoSnapshotNome(configuracao.cargoNome());
            h.setLotacaoSnapshotMunicipio(configuracao.lotacaoMunicipio());
            h.setJornadaSnapshotMinutosDia(com.example.siggtrat.utilitario.FotografiaJornada
                    .minutosPorDia(configuracao.jornadaSemanal()));
            resultado.add(h);
        }
        return resultado;
    }

    private record NomesAlocacao(String lotacao, String jornada, String cargo) {}

    /** Objeto transitório de cálculo: nunca é salvo nem anexado ao EntityManager. */
    private static final class FotografiaCanonica extends HistoricoVinculoContrato {
        private final Long pessoaId;
        private final Long vinculoId;
        private final Long alocacaoId;
        private final Map<LocalDate, IdentidadeHistoricaResolvidaDTO> nomesDoPeriodo;
        private FotografiaCanonica(Long pessoaId, Long vinculoId, Long alocacaoId,
                                   Map<LocalDate, IdentidadeHistoricaResolvidaDTO> nomesDoPeriodo) {
            this.pessoaId = pessoaId; this.vinculoId = vinculoId; this.alocacaoId = alocacaoId;
            this.nomesDoPeriodo = nomesDoPeriodo;
        }
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
                HistoricoVinculoContrato alocacao = vigenteMaisRecente(doEmpregado, data, contexto.escopo());
                if (alocacao == null) continue;
                String chave = chaveAlocacao(alocacao);
                LinhaAcumulada linha = linhas.computeIfAbsent(chave, ignorado -> new LinhaAcumulada(
                        alocacao, minimizador.nivelPadrao(contexto.usuario())));
                Frequencia frequencia = frequencias.get(new ChaveDia(empregadoId, data));
                if (!frequenciaPertenceAlocacao(frequencia, alocacao)) frequencia = null;
                linha.adicionar(data, criarDia(alocacao, data, frequencia,
                        contexto.calendario().getOrDefault(data, List.of())), frequencia);
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
                HistoricoVinculoContrato alocacao = vigenteMaisRecente(doEmpregado, data, contexto.escopo());
                if (alocacao == null) continue;
                Frequencia frequencia = frequencias.get(new ChaveDia(empregadoId, data));
                if (!frequenciaPertenceAlocacao(frequencia, alocacao)) frequencia = null;
                RelatorioFrequenciaConsolidadoDTO.Dia dia = criarDia(
                        alocacao, data, frequencia, contexto.calendario().getOrDefault(data, List.of()));
                linhas.computeIfAbsent(chaveAlocacao(alocacao), ignorado -> new AbsenteismoAcumulado(
                                alocacao, minimizador.nivelPadrao(contexto.usuario())))
                        .adicionar(data, dia, frequencia);
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
            EscopoConsulta escopo
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

    private List<Contrato> resolverContratos(Collection<Long> contratoIds, EscopoConsulta escopo) {
        Set<Long> solicitados = contratoIds == null ? Set.of() : new LinkedHashSet<>(contratoIds);
        List<Long> permitidos = relatorioEscalaRepositorio.listarContratosCandidatos(
                solicitados, escopo.gestaoLocalId(), escopo.superintendenciaId());
        if (!solicitados.isEmpty() && !new HashSet<>(permitidos).containsAll(solicitados)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais contratos não foram encontrados no seu escopo.");
        }
        return contratoRepositorio.findAllById(permitidos).stream()
                .sorted(Comparator.comparing(Contrato::getNumeroContrato, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    /** Opções sem corte incluem o acervo histórico; datas opcionais recortam sua responsabilidade. */
    public List<ContratoOpcaoDTO> contratosParaFiltros(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim == null) fim = inicio;
        if (fim != null && inicio == null) inicio = fim;
        if (inicio != null) validarPeriodo(inicio, fim);
        EscopoConsulta escopo = escopoAtual(acessoOrganizacionalServico.usuarioAtual());
        List<Contrato> contratos = resolverContratos(null, escopo);
        LeituraHistoricaContratos entradas = inicio == null ? null : motorContrato.carregarLeitura(
                contratos.stream().map(Contrato::getId).toList());
        Set<Long> permitidos = inicio == null ? null : reconstruirFaixas(contratos, inicio, fim, escopo, entradas).stream()
                .map(FaixaResponsabilidade::contratoId).collect(java.util.stream.Collectors.toSet());
        return contratos.stream().filter(c -> permitidos == null || permitidos.contains(c.getId()))
                .map(c -> new ContratoOpcaoDTO(c.getId(), c.getNumeroContrato(), c.getDescricao(), c.getAtivo(), null))
                .toList();
    }

    private static Long intersecao(Long autorizado, Long publicado) {
        if (autorizado == null) return publicado;
        if (publicado == null || Objects.equals(autorizado, publicado)) return autorizado;
        return -1L; // Interseção vazia; jamais transforma divergência em escopo nacional.
    }

    private EscopoConsulta escopoAtual(UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.podeConsultarContratos(usuario)
                || (acessoOrganizacionalServico.ehGestaoLocal(usuario)
                    || acessoOrganizacionalServico.ehOperacional(usuario))
                    && !acessoOrganizacionalServico.podeConsultarFrequencia(usuario))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não possui acesso aos relatórios.");
        Long gestao = null, se = null;
        boolean operacional = acessoOrganizacionalServico.ehOperacional(usuario);
        if (acessoOrganizacionalServico.ehGestorEstadual(usuario)) {
            SuperintendenciaEstadual estadual = acessoOrganizacionalServico.superintendenciaEfetiva(usuario);
            se = estadual == null ? -1L : estadual.getId();
        } else if (!acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(usuario)) {
            gestao = usuario.getGestaoLocal() == null ? -1L : usuario.getGestaoLocal().getId();
        }
        List<RelatorioFrequenciaEscalaRepositorio.EscopoOperacional> pares = operacional
                ? escopoAcessoUsuarioServico.obterEscopos(usuario).stream()
                    .map(e -> new RelatorioFrequenciaEscalaRepositorio.EscopoOperacional(e.lotacaoId(), e.jornadaId()))
                    .distinct().toList() : List.of();
        return new EscopoConsulta(gestao, se, operacional, pares, List.of(), Instant.now());
    }

    private List<FaixaResponsabilidade> reconstruirFaixas(List<Contrato> contratos, LocalDate inicio,
                                                         LocalDate fim, EscopoConsulta escopo) {
        return reconstruirFaixas(contratos,inicio,fim,escopo,null);
    }
    private List<FaixaResponsabilidade> reconstruirFaixas(List<Contrato> contratos,LocalDate inicio,
            LocalDate fim,EscopoConsulta escopo,LeituraHistoricaContratos entradas) {
        Instant conhecimento = escopo.conhecidoAte();
        LocalDate hoje = RelogioSistema.hoje();
        List<FaixaResponsabilidade> resultado = new ArrayList<>();
        Map<Long, Long> estados = new HashMap<>();
        for (Contrato contrato : contratos) {
            var versoes = entradas == null ? motorContrato.listarVersoes(contrato.getId())
                    : motorContrato.listarVersoes(contrato.getId(), entradas);
            // A mesma foto inicial utilizada pelo motor distingue prova temporal
            // do fallback conservador da flag operacional materializada.
            boolean habilitacaoInicialFotografada = versoes.stream()
                    .filter(v -> v.registradoEm() == null || !v.registradoEm().isAfter(conhecimento))
                    .min(Comparator.comparingInt(v -> v.numeroVersao() == null ? Integer.MAX_VALUE : v.numeroVersao()))
                    .map(v -> v.habilitacaoAdministrativa() != null).orElse(false);
            SortedSet<LocalDate> limites = new TreeSet<>(List.of(inicio, fim.plusDays(1)));
            // Fronteira da consulta: não é evento nem data presumida de inativação.
            // Impede estender a compatibilidade histórica ao bloqueio conhecido hoje.
            limites.add(hoje);
            if (versoes.isEmpty()) {
                if (contrato.getDataInicio() != null) limites.add(contrato.getDataInicio());
                if (contrato.getDataFim() != null) limites.add(contrato.getDataFim().plusDays(1));
            }
            for (var v : versoes) {
                if (v.dataEfetiva() != null) limites.add(v.dataEfetiva());
                if (v.dataInicio() != null) limites.add(v.dataInicio());
                if (v.dataFim() != null) limites.add(v.dataFim().plusDays(1));
            }
            for (var evento : entradas == null ? motorContrato.eventosValidosOrdenados(contrato.getId(), fim, conhecimento)
                    : motorContrato.eventosValidosOrdenados(contrato.getId(), fim, conhecimento, entradas)) {
                limites.add(evento.getDataEfetiva());
                limites.add(evento.getDataEfetiva().plusDays(1));
            }
            limites.removeIf(d -> d.isBefore(inicio) || d.isAfter(fim.plusDays(1)));
            List<LocalDate> datas = new ArrayList<>(limites);
            for (int i = 0; i + 1 < datas.size(); i++) {
                LocalDate data = datas.get(i);
                var posicao = entradas == null ? motorContrato.reconstruirPosicao(contrato, data, conhecimento)
                        : motorContrato.reconstruirPosicao(contrato, data, conhecimento, entradas);
                if (versoes.isEmpty()) {
                    // O legado não dispõe de fotografia contratual. Seus limites
                    // de vigência e eventos continuam valendo; a responsabilidade
                    // organizacional vem exclusivamente do histórico da alocação.
                    if (posicao.vigenciaInicio() != null && data.isBefore(posicao.vigenciaInicio())
                            || posicao.vigenciaFim() != null && data.isAfter(posicao.vigenciaFim())
                            || inatividadeImpedeApuracao(posicao, habilitacaoInicialFotografada, hoje)
                            || posicao.estadoNegocial() == EstadoNegocialContrato.CANCELADO
                            || posicao.estadoNegocial() == EstadoNegocialContrato.ANULADO
                            || contrato.getPublicadoEm() != null && posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO)
                        continue;
                    resultado.add(new FaixaResponsabilidade(contrato.getId(), null, null,
                            data, datas.get(i + 1).minusDays(1), contrato.getNumeroContrato()));
                    continue;
                }
                if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                        || posicao.situacaoTemporal() != SituacaoTemporalContrato.VIGENTE
                        || inatividadeImpedeApuracao(posicao, habilitacaoInicialFotografada, hoje)
                        || posicao.versaoContratual() == null) continue;
                // Responsabilidade deriva da transferência efetiva, não de
                // fotografia integral de ADI que já era conhecida antes dela.
                GestaoLocal gestaoHistorica = entradas == null ? gestaoContratoTemporal.gestaoEm(contrato, data, conhecimento)
                        : gestaoContratoTemporal.gestaoEm(contrato, data, conhecimento, entradas);
                if (gestaoHistorica == null || gestaoHistorica.getId() == null) continue;
                Long gestao = gestaoHistorica.getId();
                Long se = estados.computeIfAbsent(gestao, id -> {
                    GestaoLocal g = entityManager.find(GestaoLocal.class, id);
                    return g == null || g.getSuperintendencia() == null ? -1L : g.getSuperintendencia().getId();
                });
                if (escopo.gestaoLocalId() != null && !Objects.equals(escopo.gestaoLocalId(), gestao)) continue;
                if (escopo.superintendenciaId() != null && !Objects.equals(escopo.superintendenciaId(), se)) continue;
                resultado.add(new FaixaResponsabilidade(contrato.getId(), gestao, se,
                        data, datas.get(i + 1).minusDays(1), posicao.versaoContratual().numeroContrato()));
            }
        }
        return List.copyOf(resultado);
    }

    private boolean inatividadeImpedeApuracao(PosicaoTemporalContratoDTO posicao,
            boolean habilitacaoInicialFotografada, LocalDate hoje) {
        if (posicao.habilitacaoAdministrativa() == HabilitacaoAdministrativaContrato.ATIVO) return false;
        if (!posicao.dataReferencia().isBefore(hoje) || habilitacaoInicialFotografada) return true;
        // A flag atual sem fotografia/ato não retira cobertura de fatos passados.
        // Eventos aplicados já respeitam data efetiva, conhecimento e precedência.
        return posicao.eventosAplicados().stream().anyMatch(e ->
                e.tipoEvento() == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                        || e.tipoEvento() == TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA
                        || e.tipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_ANULACAO);
    }

    private boolean diaNoEscopo(HistoricoVinculoContrato h, LocalDate data, EscopoConsulta escopo) {
        return escopo.faixas().stream().anyMatch(f -> Objects.equals(f.contratoId(), h.getContrato().getId())
                && !data.isBefore(f.inicio()) && !data.isAfter(f.fim())
                && (f.gestaoLocalId() == null || Objects.equals(f.gestaoLocalId(), h.getGestaoLocalSnapshotId()))
                && (f.superintendenciaId() == null || Objects.equals(f.superintendenciaId(), h.getSuperintendenciaSnapshotId())));
    }

    private RelatorioFrequenciaConsolidadoDTO.Dia criarDia(HistoricoVinculoContrato alocacao,
                                                            LocalDate data,
                                                            Frequencia frequencia,
                                                            List<CalendarioEvento> eventos) {
        boolean feriadoCalendario = eventos.stream().anyMatch(evento ->
                eventoDoTipoAplica(evento, ConstantesDominio.EVENTO_FERIADO, alocacao, frequencia));
        boolean descansoCalendario = eventos.stream().anyMatch(evento ->
                eventoDoTipoAplica(evento, ConstantesDominio.EVENTO_DIA_DESCANSO, alocacao, frequencia));
        int previstosTurno = frequencia != null && frequencia.getJornadaSnapshotDiaTrabalho() != null
                ? Boolean.TRUE.equals(frequencia.getJornadaSnapshotDiaTrabalho())
                    ? (int) (intervalo(frequencia.getJornadaSnapshotEntrada1(), frequencia.getJornadaSnapshotSaida1())
                        + intervalo(frequencia.getJornadaSnapshotEntrada2(), frequencia.getJornadaSnapshotSaida2())) : 0
                : minutosPrevistos(alocacao, data.getDayOfWeek());
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
     * A memória gravada é o resultado oficial daquele lançamento, inclusive
     * quando vale zero. Somente registros legados sem memória completa são
     * reconstituídos a partir das batidas ou da fotografia da jornada.
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

        if (frequencia.getMinutosFisicos() != null && frequencia.getMinutosNoturnosReais() != null
                && frequencia.getMinutosAdicionais() != null && frequencia.getMinutosPagaveis() != null) {
            int fisicos = valorOuZero(frequencia.getMinutosFisicos());
            int noturnosReais = valorOuZero(frequencia.getMinutosNoturnosReais());
            int adicionais = valorOuZero(frequencia.getMinutosAdicionais());
            int pagaveis = valorOuZero(frequencia.getMinutosPagaveis());
            // A redução já integra o total pagável persistido. Derivá-la da
            // mesma memória evita reaplicar a redução ou usar o parâmetro atual
            // para reescrever uma apuração histórica.
            int noturnosReduzidos = noturnosReais == 0 ? 0
                    : Math.max(0, pagaveis - adicionais - (fisicos - noturnosReais));
            return new MemoriaCalculoDia(fisicos, noturnosReais, noturnosReduzidos,
                    adicionais, pagaveis, frequencia.getCalculoVersao());
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
        // Uma presença canônica sem fotografia de horário usa a memória gravada;
        // a jornada de hoje não pode recalcular uma frequência histórica.
        if (frequencia.getJornadaSnapshotDiaTrabalho() != null || alocacao instanceof FotografiaCanonica) return null;
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
        if (alocacao instanceof FotografiaCanonica canonica) {
            if (frequencia.getPessoa() != null && !Objects.equals(frequencia.getPessoa().getId(), canonica.pessoaId)) return false;
            if (frequencia.getVinculoContratual() != null && !Objects.equals(frequencia.getVinculoContratual().getId(), canonica.vinculoId)) return false;
            if (frequencia.getAlocacaoVinculo() != null && !Objects.equals(frequencia.getAlocacaoVinculo().getId(), canonica.alocacaoId)) return false;
        }
        if (!Objects.equals(frequencia.getContratoSnapshotId(), alocacao.getContrato().getId())) return false;
        if (!Objects.equals(frequencia.getGestaoLocalSnapshotId(), alocacao.getGestaoLocalSnapshotId())) return false;
        if (!Objects.equals(frequencia.getSuperintendenciaSnapshotId(), alocacao.getSuperintendenciaSnapshotId())) return false;
        if (!Objects.equals(frequencia.getCargoSnapshotId(), idCargo(alocacao))) return false;
        Long lotacao = idLotacao(alocacao);
        if (!Objects.equals(frequencia.getLotacaoSnapshotId(), lotacao)) return false;
        Long jornada = idJornada(alocacao);
        return Objects.equals(frequencia.getJornadaSnapshotId(), jornada);
    }

    private boolean eventoDoTipoAplica(CalendarioEvento evento, String tipo,
                                        HistoricoVinculoContrato alocacao, Frequencia frequencia) {
        return evento != null && Boolean.TRUE.equals(evento.getAtivo())
                && tipo.equalsIgnoreCase(evento.getTipo()) && eventoAplica(evento, alocacao, frequencia);
    }

    private boolean eventoAplica(CalendarioEvento evento, HistoricoVinculoContrato alocacao, Frequencia frequencia) {
        String abrangencia = normalizar(evento.getAbrangencia());
        Long gestaoLocalId = alocacao.getGestaoLocalSnapshotId();
        Long seId = alocacao.getSuperintendenciaSnapshotId();
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
            String municipio = alocacao instanceof FotografiaCanonica
                    ? (frequencia != null && frequencia.possuiSnapshotJornada()
                        ? frequencia.getLotacaoSnapshotMunicipio() : alocacao.getLotacaoSnapshotMunicipio())
                    : texto(frequencia == null ? null : frequencia.getLotacaoSnapshotMunicipio(),
                        texto(alocacao.getLotacaoSnapshotMunicipio(), lotacao != null ? lotacao.getMunicipio() : null));
            return evento.getMunicipio() != null
                    && Objects.equals(normalizar(evento.getMunicipio()), normalizar(municipio));
        }
        if ("ESTADUAL".equals(abrangencia)) {
            if (eventoSeId != null) return Objects.equals(eventoSeId, seId);
            SuperintendenciaEstadual estadual = seId == null ? null : entityManager.find(SuperintendenciaEstadual.class, seId);
            String uf = estadual == null ? null : estadual.getUf();
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
                    catch (NumberFormatException invalido) {
                        if (alocacao instanceof FotografiaCanonica) throw fotografiaHistoricaAusente();
                        return 0;
                    }
                }
            }
            if (alocacao instanceof FotografiaCanonica) throw fotografiaHistoricaAusente();
            return 0;
        }
        if (alocacao instanceof FotografiaCanonica) throw fotografiaHistoricaAusente();
        Jornada jornada = jornadaEfetiva(alocacao);
        if (jornada == null || jornada.getDias() == null) return 0;
        return jornada.getDias().stream().filter(item -> item.getDiaSemana() == dia)
                .filter(item -> Boolean.TRUE.equals(item.getDiaTrabalho()))
                .mapToInt(item -> (int) (intervalo(item.getEntrada1(), item.getSaida1())
                        + intervalo(item.getEntrada2(), item.getSaida2())))
                .findFirst().orElse(0);
    }

    private ResponseStatusException fotografiaHistoricaAusente() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "A data consultada não possui fotografia histórica suficiente da jornada. "
                        + "Regularize a evidência documental da Alocação; o horário atual não pode substituir o passado.");
    }

    private long intervalo(String entrada, String saida) {
        return com.example.siggtrat.utilitario.UtilitarioHorario.calcularIntervaloMinutos(
                entrada, saida, "entrada da jornada", "saída da jornada");
    }

    private HistoricoVinculoContrato vigenteMaisRecente(List<HistoricoVinculoContrato> itens, LocalDate data,
                                                        EscopoConsulta escopo) {
        var inicioDia = data.atStartOfDay(ZONA);
        var fimDia = data.plusDays(1).atStartOfDay(ZONA);
        return itens.stream().filter(item -> item.getDataInicio().isBefore(fimDia))
                .filter(item -> item.getDataFim() == null || !item.getDataFim().isBefore(inicioDia))
                .filter(item -> diaNoEscopo(item, data, escopo))
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
    private AvisoRevisaoRelatorioDTO avisoNoEscopo(LocalDate inicio, LocalDate fim,
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            Long gestaoLocalId, Long superintendenciaId) {
        if (gestaoLocalId == null && superintendenciaId == null)
            return aviso(inicio, fim, contratoIds, lotacaoId, jornadaId);
        return pendencias == null ? AvisoRevisaoRelatorioDTO.definitivo()
                : pendencias.avisoRelatorioNoEscopo(inicio, fim, contratoIds, lotacaoId, jornadaId,
                        gestaoLocalId, superintendenciaId);
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

    /** Usa apenas prova nominal da linha consultada, sem completar pelo cadastro atual. */
    private String nomeComprovado(HistoricoVinculoContrato alocacao, LocalDate data, Frequencia frequencia) {
        if (alocacao instanceof FotografiaCanonica canonica) {
            IdentidadeHistoricaResolvidaDTO fonte = canonica.nomesDoPeriodo.get(data);
            // Prova tardia válida altera só a visão vigente; a foto original do fato não é escrita.
            if (fonte != null && fonte.comprovado()) return fonte.nome();
            return fotografiaComprovada(null, frequencia == null ? null : frequencia.getEmpregadoSnapshotNome());
        }
        return fotografiaComprovada(alocacao.getEmpregadoSnapshotNome(),
                frequencia == null ? null : frequencia.getEmpregadoSnapshotNome());
    }

    /** O DTO tem um nome por linha: uma prova pontual não pode nomear dias sem fonte. */
    private static final class NomeDoPeriodo {
        private String nome;
        private boolean iniciado;
        private boolean incompletoOuDivergente;
        void adicionar(String valor) {
            if (valor == null || valor.isBlank()) incompletoOuDivergente = true;
            if (!iniciado) { nome = valor; iniciado = true; }
            else if (!Objects.equals(nome, valor)) incompletoOuDivergente = true;
        }
        String valor() { return incompletoOuDivergente ? null : nome; }
    }

    private String fotografiaComprovada(String fotografia, String fotografiaDaFrequencia) {
        String valor = fotografia == null || fotografia.isBlank() ? fotografiaDaFrequencia : fotografia;
        return valor == null || valor.isBlank() ? null : valor;
    }

    private final class LinhaAcumulada {
        private final HistoricoVinculoContrato alocacao;
        private final NivelExposicaoDadosPessoais nivel;
        private final NomeDoPeriodo nomeDoPeriodo = new NomeDoPeriodo();
        private String lotacaoFotografada;
        private String jornadaFotografada;
        private String cargoFotografado;
        private final Map<String, RelatorioFrequenciaConsolidadoDTO.Dia> dias = new LinkedHashMap<>();
        private final AcumuladorTotais totais = new AcumuladorTotais();
        private LinhaAcumulada(HistoricoVinculoContrato alocacao, NivelExposicaoDadosPessoais nivel) {
            this.alocacao = alocacao; this.nivel = nivel;
            this.lotacaoFotografada = fotografiaComprovada(alocacao.getLotacaoSnapshotNome(), null);
            this.jornadaFotografada = fotografiaComprovada(alocacao.getJornadaSnapshotDescricao(), null);
            this.cargoFotografado = fotografiaComprovada(alocacao.getCargoSnapshotNome(), null);
        }
        private void adicionar(LocalDate data, RelatorioFrequenciaConsolidadoDTO.Dia dia, Frequencia frequencia) {
            nomeDoPeriodo.adicionar(nomeComprovado(alocacao, data, frequencia));
            if (frequencia != null) {
                lotacaoFotografada = fotografiaComprovada(lotacaoFotografada, frequencia.getLotacaoSnapshotNome());
                jornadaFotografada = fotografiaComprovada(jornadaFotografada, frequencia.getJornadaSnapshotDescricao());
                cargoFotografado = fotografiaComprovada(cargoFotografado, frequencia.getCargoSnapshotNome());
            }
            dias.put(data.toString(), dia); totais.somar(dia);
        }
        private RelatorioFrequenciaConsolidadoDTO.Linha paraDTO() {
            Empregado e = alocacao.getEmpregado();
            Contrato c = alocacao.getContrato();
            return new RelatorioFrequenciaConsolidadoDTO.Linha(chaveAlocacao(alocacao), e.getId(),
                    minimizador.nome(nomeDoPeriodo.valor(), nivel), c.getId(),
                    texto(alocacao.getContratoSnapshotNumero(), c.getNumeroContrato()), idLotacao(alocacao),
                    lotacaoFotografada, idJornada(alocacao), jornadaFotografada, idCargo(alocacao), cargoFotografado,
                    Map.copyOf(dias), totais.paraDTO());
        }
    }


    private final class AbsenteismoAcumulado {
        private final HistoricoVinculoContrato alocacao;
        private final NivelExposicaoDadosPessoais nivel;
        private final NomeDoPeriodo nomeDoPeriodo = new NomeDoPeriodo();
        private String lotacaoFotografada;
        private String jornadaFotografada;
        private String cargoFotografado;
        private int faltas;
        private int atestados;
        private int dsrPerdido;
        private int episodios;
        private int diasTrabalhaveis;
        private int minutosPrevistos;
        private boolean ausenciaAnterior;

        private AbsenteismoAcumulado(HistoricoVinculoContrato alocacao, NivelExposicaoDadosPessoais nivel) {
            this.alocacao = alocacao;
            this.nivel = nivel;
            this.lotacaoFotografada = fotografiaComprovada(alocacao.getLotacaoSnapshotNome(), null);
            this.jornadaFotografada = fotografiaComprovada(alocacao.getJornadaSnapshotDescricao(), null);
            this.cargoFotografado = fotografiaComprovada(alocacao.getCargoSnapshotNome(), null);
        }

        private void adicionar(LocalDate data, RelatorioFrequenciaConsolidadoDTO.Dia dia, Frequencia frequencia) {
            nomeDoPeriodo.adicionar(nomeComprovado(alocacao, data, frequencia));
            if (frequencia != null) {
                lotacaoFotografada = fotografiaComprovada(lotacaoFotografada, frequencia.getLotacaoSnapshotNome());
                jornadaFotografada = fotografiaComprovada(jornadaFotografada, frequencia.getJornadaSnapshotDescricao());
                cargoFotografado = fotografiaComprovada(cargoFotografado, frequencia.getCargoSnapshotNome());
            }
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
            int ausencias = faltas + atestados;
            double taxa = diasTrabalhaveis > 0 ? ausencias * 100.0 / diasTrabalhaveis : 0.0;
            double disrupcao = (double) episodios * episodios * ausencias;
            return new RelatorioAbsenteismoDTO(
                    empregado.getId(),
                    minimizador.nome(nomeDoPeriodo.valor(), nivel),
                    cargoFotografado, lotacaoFotografada, jornadaFotografada,
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
