
import java.time.*;
import java.util.*;
import java.util.stream.*;
import com.example.siggtrat.regra.RegraEfetivoContratual;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;

public class ProvasRevisao {
    static final LocalDate DIA = LocalDate.of(2026, 10, 10);
    static final LocalDate FIM = LocalDate.of(2026, 12, 31);
    static int falhas = 0;
    static void verificar(String id, Object esperado, Object obtido) {
        boolean ok = Objects.equals(esperado, obtido);
        if (!ok) falhas++;
        System.out.println(id + "\t" + (ok ? "PASSOU" : "FALHOU_REQUISITO")
            + "\t" + esperado + "\t" + obtido);
    }
    enum TipoRelacaoPrecedenciaNegocial { RETIFICA, SUBSTITUI, CANCELA, SUCEDE, MANTEM_COEXISTE }
    enum TipoEventoCicloVidaContrato { PUBLICACAO, CANCELAMENTO_CONTRATO_FUTURO, ANULACAO,
        INATIVACAO_ADMINISTRATIVA, REATIVACAO_ADMINISTRATIVA, ENCERRAMENTO_ANTECIPADO }
    enum EstadoNegocialContrato { RASCUNHO, PUBLICADO, CANCELADO, ANULADO }
    enum SituacaoTemporalContrato { RASCUNHO, FUTURO, VIGENTE, ENCERRADO, CANCELADO, ANULADO }
    enum HabilitacaoAdministrativaContrato { ATIVO, INATIVO }
    static class RelogioSistema { static LocalDate hoje() { return DIA; } }
    record EventoCicloVidaContrato(Long id, TipoEventoCicloVidaContrato tipo, LocalDate data) {
        EventoCicloVidaContrato(Long id) { this(id, TipoEventoCicloVidaContrato.PUBLICACAO, DIA); }
        Long getId() { return id; }
        LocalDate getDataEfetiva() { return data; }
        TipoEventoCicloVidaContrato getTipoEvento() { return tipo; }
    }
    record RelacaoPrecedenciaEventoContrato(EventoCicloVidaContrato fonte,
            EventoCicloVidaContrato alvo, TipoRelacaoPrecedenciaNegocial tipo) {
        EventoCicloVidaContrato getEventoFonte() { return fonte; }
        EventoCicloVidaContrato getEventoAlvo() { return alvo; }
        TipoRelacaoPrecedenciaNegocial getTipoRelacao() { return tipo; }
    }
    record Contrato(Long id) {
        Long getId() { return id; }
        LocalDate getDataInicio() { return DIA; }
        LocalDate getDataFim() { return FIM; }
        Instant getPublicadoEm() { return Instant.parse("2026-01-01T00:00:00Z"); }
        EstadoNegocialContrato getEstadoNegocial() { return EstadoNegocialContrato.CANCELADO; }
    }
    record VersaoContratoNegocial(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate getDataInicio() { return dataInicio; }
        LocalDate getDataFim() { return dataFim; }
    }
    record PosicaoTemporalContratoDTO(Long id, LocalDate data, Instant conhecimento,
        EstadoNegocialContrato estado, SituacaoTemporalContrato situacao,
        HabilitacaoAdministrativaContrato habilitacao, LocalDate inicio, LocalDate fim,
        Object versao, List<EventoCicloVidaContrato> eventos, List<RelacaoPrecedenciaEventoContrato> relacoes) {}
    static class CicloFixture {
        List<EventoCicloVidaContrato> eventos;
        List<EventoCicloVidaContrato> eventosValidosOrdenados(Long id, LocalDate data, Instant i) {
            return eventos.stream().filter(e -> !e.getDataEfetiva().isAfter(data))
                .sorted(Comparator.comparing(EventoCicloVidaContrato::getDataEfetiva)).toList();
        }
        List<RelacaoPrecedenciaEventoContrato> relacoesConhecidas(Long id, Instant i) { return List.of(); }
        Optional<VersaoContratoNegocial> versaoAplicavel(Contrato c, LocalDate d, Instant i) { return Optional.empty(); }
        LocalDate dataFimOriginalFallback(Contrato c) { return c.getDataFim(); }
        Object paraVersaoDTO(VersaoContratoNegocial v) { return v; }
        EventoCicloVidaContrato paraEventoDTO(EventoCicloVidaContrato e) { return e; }
        RelacaoPrecedenciaEventoContrato paraRelacaoDTO(RelacaoPrecedenciaEventoContrato r) { return r; }
        public PosicaoTemporalContratoDTO reconstruirPosicao(
            Contrato contrato,
            LocalDate dataReferencia,
            Instant conhecidoAte
    ) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoCicloVidaContrato> eventos = eventosValidosOrdenados(contrato.getId(), data, conhecimento);
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacoesConhecidas(contrato.getId(), conhecimento);
        Optional<VersaoContratoNegocial> versaoOpt = versaoAplicavel(contrato, data, conhecimento);

        LocalDate inicio = versaoOpt.map(VersaoContratoNegocial::getDataInicio).orElse(contrato.getDataInicio());
        LocalDate fim = versaoOpt.map(VersaoContratoNegocial::getDataFim).orElseGet(() -> dataFimOriginalFallback(contrato));

        EstadoNegocialContrato estado = estadoInicialConhecido(contrato, conhecimento);
        HabilitacaoAdministrativaContrato habilitacao = HabilitacaoAdministrativaContrato.ATIVO;

        for (EventoCicloVidaContrato e : eventos) {
            switch (e.getTipoEvento()) {
                case INATIVACAO_ADMINISTRATIVA -> habilitacao = HabilitacaoAdministrativaContrato.INATIVO;
                case REATIVACAO_ADMINISTRATIVA -> habilitacao = HabilitacaoAdministrativaContrato.ATIVO;
                case ENCERRAMENTO_ANTECIPADO -> fim = e.getDataEfetiva();
                case CANCELAMENTO_CONTRATO_FUTURO -> estado = EstadoNegocialContrato.CANCELADO;
                case ANULACAO -> estado = EstadoNegocialContrato.ANULADO;
                case PUBLICACAO -> {
                    if (estado != EstadoNegocialContrato.CANCELADO
                            && estado != EstadoNegocialContrato.ANULADO) {
                        estado = EstadoNegocialContrato.PUBLICADO;
                    }
                }
                default -> { /* Etapa 05 conectará os demais fatos ao snapshot contratual. */ }
            }
        }

        SituacaoTemporalContrato situacao = situacaoTemporal(estado, inicio, fim, data);
        return new PosicaoTemporalContratoDTO(
                contrato.getId(),
                data,
                conhecimento,
                estado,
                situacao,
                habilitacao,
                inicio,
                fim,
                versaoOpt.map(this::paraVersaoDTO).orElse(null),
                eventos.stream().map(this::paraEventoDTO).toList(),
                relacoes.stream().map(this::paraRelacaoDTO).toList()
        );
    }
        private SituacaoTemporalContrato situacaoTemporal(
            EstadoNegocialContrato estado,
            LocalDate inicio,
            LocalDate fim,
            LocalDate data
    ) {
        if (estado == EstadoNegocialContrato.RASCUNHO) return SituacaoTemporalContrato.RASCUNHO;
        if (estado == EstadoNegocialContrato.CANCELADO) return SituacaoTemporalContrato.CANCELADO;
        if (estado == EstadoNegocialContrato.ANULADO) return SituacaoTemporalContrato.ANULADO;
        if (inicio != null && data.isBefore(inicio)) return SituacaoTemporalContrato.FUTURO;
        if (fim != null && data.isAfter(fim)) return SituacaoTemporalContrato.ENCERRADO;
        return SituacaoTemporalContrato.VIGENTE;
    }
        private EstadoNegocialContrato estadoInicialConhecido(Contrato contrato, Instant conhecidoAte) {
        if (contrato.getPublicadoEm() == null) {
            return EstadoNegocialContrato.RASCUNHO;
        }
        return contrato.getPublicadoEm().isAfter(conhecidoAte)
                ? EstadoNegocialContrato.RASCUNHO
                : EstadoNegocialContrato.PUBLICADO;
    }
    }
    record Lotacao(Long id) { Long getId() { return id; } }
    record Jornada(Long id) { Long getId() { return id; } }
    record Cargo(Long id) { Long getId() { return id; } }
    record ComposicaoProjetada(Long composicaoBaseId, Lotacao lotacao, Jornada jornada,
            Cargo cargo, int efetivoPrevisto, LocalDate dataInicio, LocalDate dataFim) {
        static String chave(Long l, Long j, Long c) { return l + ":" + j + ":" + c; }
    }
    static class ComposicaoContratualTemporalServico {
        static class ComposicaoProjetada {
            static String chave(Long l, Long j, Long c) { return l + ":" + j + ":" + c; }
        }
    }
    record RemanejamentoComposicao(Lotacao lotacao, Jornada jornada, Cargo cargo, int resultante, int variacao) {
        RemanejamentoComposicao(Lotacao lotacao, Jornada jornada, Cargo cargo, int resultante) {
            this(lotacao, jornada, cargo, resultante, 0);
        }
        Lotacao getLotacao() { return lotacao; }
        Jornada getJornada() { return jornada; }
        Cargo getCargo() { return cargo; }
        Integer getQuantidadeResultante() { return resultante; }
        Integer getVariacao() { return variacao; }
    }
    record RemanejamentoContratual(EventoCicloVidaContrato evento, List<RemanejamentoComposicao> itens) {
        EventoCicloVidaContrato getEvento() { return evento; }
        List<RemanejamentoComposicao> getItens() { return itens; }
    }
    record AditamentoContratual(EventoCicloVidaContrato evento, List<RemanejamentoComposicao> itens) {
        EventoCicloVidaContrato getEvento() { return evento; }
        List<RemanejamentoComposicao> getAlteracoesComposicao() { return itens; }
    }
    record ItemPreparado(String chave) {}
    record AditamentoContratualRequisicao(LocalDate dataEfetiva, Long eventoPrecedenteId,
            TipoRelacaoPrecedenciaNegocial relacaoPrecedencia) {}
    record RemanejamentoContratualRequisicao(LocalDate dataEfetiva, Long eventoPrecedenteId,
            TipoRelacaoPrecedenciaNegocial relacaoPrecedencia) {}
    static class RepoA {
        List<AditamentoContratual> dados = List.of();
        int consultas;
        List<AditamentoContratual> findByContratoIdAndDataEfetiva(Long id, LocalDate d) {
            consultas++; return dados;
        }
    }
    static class RepoR {
        List<RemanejamentoContratual> dados = List.of();
        int consultas;
        List<RemanejamentoContratual> findByContratoIdAndDataEfetiva(Long id, LocalDate d) {
            consultas++; return dados;
        }
    }
    static class MotorFixture {
        List<EventoCicloVidaContrato> dados = List.of();
        List<EventoCicloVidaContrato> eventosValidosOrdenados(Long id, LocalDate d, Instant i) { return dados; }
    }
    enum HttpStatus { CONFLICT }
    static class ResponseStatusException extends RuntimeException {
        ResponseStatusException(HttpStatus s, String m) { super(m); }
    }
    static class ValidadorA {
        RepoA aditamentoRepositorio = new RepoA();
        RepoR remanejamentoRepositorio = new RepoR();
        MotorFixture motor = new MotorFixture();
        // As chaves aqui são dados da fixture, previamente calculados no serviço.
        Set<String> chavesAfetadas(AditamentoContratualRequisicao r, List<ItemPreparado> itens) {
            return itens.stream().map(i -> "COMPOSICAO:" + i.chave()).collect(Collectors.toSet());
        }
        Set<String> chavesAfetadas(AditamentoContratual a) {
            return a.getAlteracoesComposicao().stream().map(i -> "COMPOSICAO:" +
                ComposicaoProjetada.chave(i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))
                .collect(Collectors.toSet());
        }
        private void validarConflitosMesmaData(Contrato contrato, AditamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> novasChaves = chavesAfetadas(req, itens);
        List<AditamentoContratual> mesmosDia = aditamentoRepositorio.findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva());
        Set<Long> ativos = motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()).stream()
                .filter(e -> e.getDataEfetiva().equals(req.dataEfetiva()))
                .map(EventoCicloVidaContrato::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<AditamentoContratual> conflitosAditamento = mesmosDia.stream()
                .filter(a -> a.getEvento() != null && ativos.contains(a.getEvento().getId()))
                .filter(a -> !Collections.disjoint(novasChaves, chavesAfetadas(a)))
                .toList();
        List<RemanejamentoContratual> conflitosRemanejamento = remanejamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva()).stream()
                .filter(r -> r.getEvento() != null && ativos.contains(r.getEvento().getId()))
                .filter(r -> r.getItens().stream().anyMatch(i -> novasChaves.contains("COMPOSICAO:" +
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        if (conflitosAditamento.isEmpty() && conflitosRemanejamento.isEmpty()) return;
        if (req.eventoPrecedenteId() == null || req.relacaoPrecedencia() == null) {
            String idsA = conflitosAditamento.stream().map(a -> String.valueOf(a.getEvento().getId())).collect(Collectors.joining(", "));
            String idsR = conflitosRemanejamento.stream().map(r -> String.valueOf(r.getEvento().getId())).collect(Collectors.joining(", "));
            String ids = String.join(", ", java.util.stream.Stream.of(idsA, idsR).filter(x -> !x.isBlank()).toList());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existe fato contratual conflitante na mesma data. Informe precedência negocial explícita. Eventos: " + ids);
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campos conflitantes não admitem MANTEM_COEXISTE.");
        }
        boolean alvoConflitante = conflitosAditamento.stream().anyMatch(a -> Objects.equals(a.getEvento().getId(), req.eventoPrecedenteId()))
                || conflitosRemanejamento.stream().anyMatch(r -> Objects.equals(r.getEvento().getId(), req.eventoPrecedenteId()));
        if (!alvoConflitante) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O evento precedente informado não é o fato conflitante da mesma data.");
        }
    }
    }
    static class ValidadorR {
        RepoA aditamentoRepositorio = new RepoA();
        RepoR remanejamentoRepositorio = new RepoR();
        MotorFixture motor = new MotorFixture();
        private void validarConflitosMesmaData(Contrato contrato, RemanejamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> novas = itens.stream().map(ItemPreparado::chave).collect(Collectors.toSet());
        List<RemanejamentoContratual> mesmosDia = remanejamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva());
        Set<Long> ativos = motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()).stream()
                .filter(e -> e.getDataEfetiva().equals(req.dataEfetiva()))
                .map(EventoCicloVidaContrato::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<RemanejamentoContratual> conflitosRemanejamento = mesmosDia.stream()
                .filter(r -> r.getEvento() != null && ativos.contains(r.getEvento().getId()))
                .filter(r -> r.getItens().stream().anyMatch(i -> novas.contains(
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        List<AditamentoContratual> conflitosAditamento = aditamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva()).stream()
                .filter(a -> a.getEvento() != null && ativos.contains(a.getEvento().getId()))
                .filter(a -> a.getAlteracoesComposicao().stream().anyMatch(i -> novas.contains(
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        if (conflitosRemanejamento.isEmpty() && conflitosAditamento.isEmpty()) return;
        if (req.eventoPrecedenteId() == null || req.relacaoPrecedencia() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fatos conflitantes sobre a mesma composição e data exigem precedência negocial explícita.");
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fatos sobre o mesmo recorte não admitem MANTEM_COEXISTE.");
        }
        boolean alvo = conflitosRemanejamento.stream().anyMatch(r -> Objects.equals(r.getEvento().getId(), req.eventoPrecedenteId()))
                || conflitosAditamento.stream().anyMatch(a -> Objects.equals(a.getEvento().getId(), req.eventoPrecedenteId()));
        if (!alvo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O evento precedente informado não é o fato conflitante da mesma data.");
        }
    }
    }
    private Set<Long> resolverIdsAtivos(
            List<EventoCicloVidaContrato> eventos,
            List<RelacaoPrecedenciaEventoContrato> relacoes
    ) {
        Set<Long> ids = eventos.stream().map(EventoCicloVidaContrato::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> ativos = new LinkedHashSet<>(ids);
        List<RelacaoPrecedenciaEventoContrato> conhecidas = new ArrayList<>(relacoes == null ? List.of() : relacoes);

        // RETIFICA e SUBSTITUI encerram definitivamente a versão alvo na cadeia.
        // Uma versão antiga não volta a valer apenas porque a versão que a
        // sucedeu foi novamente retificada/substituída.
        for (RelacaoPrecedenciaEventoContrato r : conhecidas) {
            Long fonte = id(r.getEventoFonte());
            Long alvo = id(r.getEventoAlvo());
            if (fonte == null || alvo == null || !ids.contains(fonte) || !ids.contains(alvo)) continue;
            if (r.getTipoRelacao() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || r.getTipoRelacao() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI) {
                ativos.remove(alvo);
            }
        }

        // CANCELA é diferente de versionamento: só invalida o alvo se o próprio
        // evento cancelador continuar válido. Processar do conhecimento mais
        // recente para o mais antigo permite cancelar um cancelamento sem usar
        // ID/timestamp como precedência negocial.
        Collections.reverse(conhecidas);
        for (RelacaoPrecedenciaEventoContrato r : conhecidas) {
            if (r.getTipoRelacao() != TipoRelacaoPrecedenciaNegocial.CANCELA) continue;
            Long fonte = id(r.getEventoFonte());
            Long alvo = id(r.getEventoAlvo());
            if (fonte == null || alvo == null || !ativos.contains(fonte)) continue;
            ativos.remove(alvo);
        }
        return ativos;
    }
    private Long id(EventoCicloVidaContrato e) { return e == null ? null : e.getId(); }
    private void aplicarRemanejamento(Map<String, ComposicaoProjetada> estado, RemanejamentoContratual r) {
        int totalAntes = estado.values().stream().mapToInt(ComposicaoProjetada::efetivoPrevisto).sum();
        int somaVariacoesAplicadas = 0;
        for (RemanejamentoComposicao item : r.getItens()) {
            String chave = ComposicaoProjetada.chave(item.getLotacao().getId(), item.getJornada().getId(), item.getCargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            if (anterior == null) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: composição inexistente " + chave + ".");
            }
            int quantidadeDepois = anterior.efetivoPrevisto() + item.getVariacao();
            if (quantidadeDepois < 0) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: quantidade negativa em " + chave + ".");
            }
            somaVariacoesAplicadas += item.getVariacao();
            estado.put(chave, new ComposicaoProjetada(
                    anterior.composicaoBaseId(), anterior.lotacao(), anterior.jornada(), anterior.cargo(),
                    quantidadeDepois, anterior.dataInicio(), anterior.dataFim()));
        }
        int totalDepois = estado.values().stream().mapToInt(ComposicaoProjetada::efetivoPrevisto).sum();
        if (somaVariacoesAplicadas != 0 || totalDepois != totalAntes) {
            throw new IllegalStateException(
                    "Remanejamento incompatível com a linha temporal reconstruída: o total contratual deve ser preservado.");
        }
    }

    public static void main(String[] args) {
        ProvasRevisao p = new ProvasRevisao();
        var a = new EventoCicloVidaContrato(1L);
        var b = new EventoCicloVidaContrato(2L);
        var c = new EventoCicloVidaContrato(3L);
        var ba = new RelacaoPrecedenciaEventoContrato(b, a, TipoRelacaoPrecedenciaNegocial.RETIFICA);
        var cb = new RelacaoPrecedenciaEventoContrato(c, b, TipoRelacaoPrecedenciaNegocial.RETIFICA);
        verificar("T01_RETIFICACAO_SIMPLES", Set.of(2L), p.resolverIdsAtivos(List.of(a,b), List.of(ba)));
        verificar("T02_RETIFICACAO_DE_RETIFICACAO", Set.of(3L),
            p.resolverIdsAtivos(List.of(a,b,c), List.of(ba,cb)));
        var substitui = new RelacaoPrecedenciaEventoContrato(c, b, TipoRelacaoPrecedenciaNegocial.SUBSTITUI);
        verificar("T03_SUBSTITUICAO_DE_RETIFICACAO", Set.of(3L),
            p.resolverIdsAtivos(List.of(a,b,c), List.of(ba,substitui)));

        var l = new Lotacao(1L); var j = new Jornada(1L); var cargo1 = new Cargo(1L); var cargo2 = new Cargo(2L);
        var item1 = new RemanejamentoComposicao(l,j,cargo1,40,-10);
        var item2 = new RemanejamentoComposicao(l,j,cargo2,60,10);
        var rem = new RemanejamentoContratual(a, List.of(item1,item2));
        Map<String,ComposicaoProjetada> estado = new LinkedHashMap<>();
        estado.put("1:1:1", new ComposicaoProjetada(1L,l,j,cargo1,60,DIA.minusDays(2),FIM));
        estado.put("1:1:2", new ComposicaoProjetada(2L,l,j,cargo2,50,DIA.minusDays(2),FIM));
        // Fato posterior foi originalmente registrado sobre 50+50 e redistribuiu -10/+10.
        // O fato retroativo anterior elevou a primeira composição de 50 para 60.
        // Reaplicar remanejamento não pode reduzir o novo total 110 para 100.
        p.aplicarRemanejamento(estado, rem);
        verificar("T04_REMANEJAMENTO_APOS_RETROATIVO_TOTAL", 110,
            estado.values().stream().mapToInt(ComposicaoProjetada::efetivoPrevisto).sum());

        var contrato = new Contrato(1L);
        var itens = List.of(new ItemPreparado("1:1:1"),new ItemPreparado("1:1:2"));
        var va = new ValidadorA();
        va.remanejamentoRepositorio.dados = List.of(rem);
        va.motor.dados = List.of(a);
        boolean bloqueou = false;
        try { va.validarConflitosMesmaData(contrato, new AditamentoContratualRequisicao(DIA,null,null), itens); }
        catch (ResponseStatusException e) { bloqueou = true; }
        verificar("T05_ADITAMENTO_APOS_REMANEJAMENTO_CONFLITANTE", true, bloqueou);
        verificar("T06_ADITAMENTO_CONSULTA_OUTRO_DOMINIO", true, va.remanejamentoRepositorio.consultas > 0);

        var vr = new ValidadorR();
        vr.aditamentoRepositorio.dados = List.of(new AditamentoContratual(a,List.of(item1)));
        vr.motor.dados = List.of(a);
        bloqueou = false;
        try { vr.validarConflitosMesmaData(contrato,new RemanejamentoContratualRequisicao(DIA,null,null),itens); }
        catch (ResponseStatusException e) { bloqueou = true; }
        verificar("T07_REMANEJAMENTO_APOS_ADITAMENTO_CONFLITANTE", true, bloqueou);
        verificar("T08_REMANEJAMENTO_CONSULTA_OUTRO_DOMINIO", true, vr.aditamentoRepositorio.consultas > 0);

        verificar("T09_PISO_ORIGINAL_100",75,RegraEfetivoContratual.pisoNormal(100));
        verificar("T10_PISO_ORIGINAL_101",76,RegraEfetivoContratual.pisoNormal(101));
        verificar("T11_AUMENTO_140_REDUCAO_75_BASE_100",false,RegraEfetivoContratual.reducaoExcedeLimiteNormal(100,75));
        verificar("T12_REDUCAO_74_BASE_100",true,RegraEfetivoContratual.reducaoExcedeLimiteNormal(100,74));
        verificar("T13_REMANEJAMENTO_SOMA_ZERO",true,RegraEfetivoContratual.remanejamentoPreservaTotal(List.of(-10,10)));
        verificar("T14_REMANEJAMENTO_NAO_ZERO",false,RegraEfetivoContratual.remanejamentoPreservaTotal(List.of(-10,9)));
        verificar("T15_HASH_DETECTA_MUDANCA",false,AssinaturaPreviaNegocial.sha256(List.of("100"))
            .equals(AssinaturaPreviaNegocial.sha256(List.of("101"))));
        var ciclo = new CicloFixture();
        ciclo.eventos = List.of(
            new EventoCicloVidaContrato(4L,TipoEventoCicloVidaContrato.CANCELAMENTO_CONTRATO_FUTURO,DIA.minusDays(1)),
            new EventoCicloVidaContrato(5L,TipoEventoCicloVidaContrato.PUBLICACAO,DIA));
        verificar("T16_CONTRATO_CANCELADO_NAO_REVIVE_NO_INICIO", SituacaoTemporalContrato.CANCELADO,
            ciclo.reconstruirPosicao(contrato,DIA.plusDays(1),Instant.parse("2026-12-01T00:00:00Z")).situacao());
        if (falhas > 0) System.exit(1);
    }
}
