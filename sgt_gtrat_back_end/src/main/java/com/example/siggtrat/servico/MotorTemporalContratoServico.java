package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoCicloVidaContratoRepositorio;
import com.example.siggtrat.repositorio.RelacaoPrecedenciaEventoContratoRepositorio;
import com.example.siggtrat.repositorio.VersaoContratoNegocialRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor temporal comum dos fatos negociais do contrato.
 *
 * Princípios:
 * - data efetiva define quando o fato produz efeito;
 * - registradoEm define quando o sistema tomou conhecimento;
 * - ID/timestamp nunca resolvem precedência negocial;
 * - conflitos reais na mesma data/chave exigem relação explícita;
 * - retificação/cancelamento/substituição preservam o fato anterior.
 */
@Service
public class MotorTemporalContratoServico {

    private final EventoCicloVidaContratoRepositorio eventoRepositorio;
    private final RelacaoPrecedenciaEventoContratoRepositorio relacaoRepositorio;
    private final VersaoContratoNegocialRepositorio versaoRepositorio;

    public MotorTemporalContratoServico(
            EventoCicloVidaContratoRepositorio eventoRepositorio,
            RelacaoPrecedenciaEventoContratoRepositorio relacaoRepositorio,
            VersaoContratoNegocialRepositorio versaoRepositorio
    ) {
        this.eventoRepositorio = eventoRepositorio;
        this.relacaoRepositorio = relacaoRepositorio;
        this.versaoRepositorio = versaoRepositorio;
    }

    @Transactional
    public EventoCicloVidaContrato registrarEvento(
            Contrato contrato,
            TipoEventoCicloVidaContrato tipo,
            AcaoCicloVidaContratoDTO acao,
            UsuarioAplicacao usuario,
            String valorAnterior,
            String valorNovo
    ) {
        return registrarEvento(contrato, tipo, acao, usuario, valorAnterior, valorNovo, null);
    }

    /**
     * Registra fato temporal permitindo chave semântica específica do domínio.
     * A chave customizada é usada por aditamentos/remanejamentos, pois dois
     * instrumentos na mesma data só conflitam quando atingem o mesmo recorte.
     */
    @Transactional
    public EventoCicloVidaContrato registrarEvento(
            Contrato contrato,
            TipoEventoCicloVidaContrato tipo,
            AcaoCicloVidaContratoDTO acao,
            UsuarioAplicacao usuario,
            String valorAnterior,
            String valorNovo,
            String chaveNegocialCustomizada
    ) {
        if (contrato == null || contrato.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato persistido é obrigatório para registrar fato temporal.");
        }
        if (tipo == null || acao == null || acao.dataEfetiva() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo e data efetiva são obrigatórios para registrar fato temporal.");
        }

        String chave = chaveNegocialCustomizada == null || chaveNegocialCustomizada.isBlank()
                ? chaveNegocialPadrao(tipo)
                : chaveNegocialCustomizada.trim().toUpperCase();
        validarParPrecedencia(acao);

        EventoCicloVidaContrato precedente = null;
        if (acao.eventoPrecedenteId() != null) {
            precedente = eventoRepositorio.findById(acao.eventoPrecedenteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento precedente informado não encontrado."));
            if (precedente.getContrato() == null || !Objects.equals(precedente.getContrato().getId(), contrato.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento precedente pertence a outro contrato.");
            }
        }

        List<EventoCicloVidaContrato> conflitosAtivos = conflitosAtivos(contrato.getId(), chave, acao.dataEfetiva());
        if (!conflitosAtivos.isEmpty()) {
            EventoCicloVidaContrato ultimo = ultimoOrdenadoDaChave(conflitosAtivos, contrato.getId());
            if (precedente == null || acao.relacaoPrecedencia() == null) {
                throw conflitoSemPrecedencia(chave, acao.dataEfetiva(), conflitosAtivos);
            }
            if (ultimo == null || !Objects.equals(ultimo.getId(), precedente.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A precedência deve referenciar o último fato válido da cadeia na mesma data/chave. Evento esperado: "
                                + (ultimo != null ? ultimo.getId() : "indefinido") + "."
                );
            }
            if (acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE
                    && chaveSingular(chave)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A chave negocial " + chave + " representa estado singular e não admite MANTEM_COEXISTE."
                );
            }
        } else if (precedente != null && acao.relacaoPrecedencia() != null) {
            // Relações podem ser declaradas mesmo entre datas diferentes (ex.: retificação retroativa).
            validarPrecedenteSemAutoReferencia(contrato, precedente);
        }

        EventoCicloVidaContrato evento = new EventoCicloVidaContrato();
        evento.setContrato(contrato);
        evento.setTipoEvento(tipo);
        evento.setChaveNegocial(chave);
        evento.setDataEfetiva(acao.dataEfetiva());
        evento.setDataAto(acao.dataAto());
        evento.setMotivo(acao.motivo());
        evento.setReferenciaSei(acao.referenciaSei());
        evento.setValorAnterior(valorAnterior);
        evento.setValorNovo(valorNovo);
        evento.setOrdemDocumental(acao.ordemDocumental());
        evento.setUsuarioResponsavelId(usuario != null ? usuario.getId() : null);
        evento.setUsuarioResponsavelUsername(usuario != null ? usuario.getUsername() : null);

        if (precedente != null && (acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                || acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI)) {
            evento.setFatoLogicoId(precedente.getFatoLogicoId());
            evento.setVersaoNegocial(Optional.ofNullable(precedente.getVersaoNegocial()).orElse(1) + 1);
        }

        EventoCicloVidaContrato salvo = eventoRepositorio.save(evento);

        if (precedente != null && acao.relacaoPrecedencia() != null) {
            registrarRelacao(salvo, precedente, acao.relacaoPrecedencia(), usuario, acao.motivo());
        }
        return salvo;
    }

    @Transactional
    public VersaoContratoNegocial registrarFotografia(
            Contrato contrato,
            EventoCicloVidaContrato eventoOrigem,
            LocalDate dataEfetiva
    ) {
        if (contrato == null || contrato.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato persistido é obrigatório para versionamento.");
        }
        int proxima = versaoRepositorio.findTopByContratoIdOrderByNumeroVersaoDesc(contrato.getId())
                .map(v -> v.getNumeroVersao() + 1)
                .orElse(1);

        VersaoContratoNegocial v = new VersaoContratoNegocial();
        v.setContrato(contrato);
        v.setEventoOrigem(eventoOrigem);
        v.setNumeroVersao(proxima);
        v.setDataEfetiva(dataEfetiva != null ? dataEfetiva : contrato.getDataInicio());
        v.setNumeroContrato(contrato.getNumeroContrato());
        v.setAnoContrato(contrato.getAnoContrato());
        v.setDescricao(contrato.getDescricao());
        v.setTipoContratacaoId(contrato.getTipoContratacao() != null ? contrato.getTipoContratacao().getId() : null);
        v.setTipoContratacaoDescricao(contrato.getTipo());
        v.setFornecedorId(contrato.getFornecedor() != null ? contrato.getFornecedor().getId() : null);
        v.setCnpjEmpresa(contrato.getCnpjEmpresa());
        v.setRazaoSocialEmpresa(contrato.getRazaoSocialEmpresa());
        v.setNomeFantasiaEmpresa(contrato.getNomeFantasiaEmpresa());
        v.setDataInicio(contrato.getDataInicio());
        v.setDataFim(contrato.getDataFim());
        v.setEfetivoPrevisto(contrato.getEfetivoPrevisto());
        v.setGestaoLocalId(contrato.getGestaoLocal() != null ? contrato.getGestaoLocal().getId() : null);
        v.setGestaoLocalNome(contrato.getGestaoLocal() != null ? contrato.getGestaoLocal().getNome() : null);
        return versaoRepositorio.save(v);
    }

    /** Persiste uma fotografia já reconstruída pelo domínio sem alterar a entidade Contrato. */
    @Transactional
    public VersaoContratoNegocial registrarFotografiaProjetada(
            Contrato contrato,
            EventoCicloVidaContrato eventoOrigem,
            LocalDate dataEfetiva,
            VersaoContratoNegocialDTO foto
    ) {
        if (contrato == null || contrato.getId() == null || foto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotografia projetada incompleta.");
        }
        int proxima = versaoRepositorio.findTopByContratoIdOrderByNumeroVersaoDesc(contrato.getId())
                .map(v -> v.getNumeroVersao() + 1).orElse(1);
        VersaoContratoNegocial v = new VersaoContratoNegocial();
        v.setContrato(contrato);
        v.setEventoOrigem(eventoOrigem);
        v.setNumeroVersao(proxima);
        v.setDataEfetiva(dataEfetiva != null ? dataEfetiva : foto.dataEfetiva());
        v.setNumeroContrato(foto.numeroContrato());
        v.setAnoContrato(foto.anoContrato());
        v.setDescricao(foto.descricao());
        v.setTipoContratacaoId(foto.tipoContratacaoId());
        v.setTipoContratacaoDescricao(foto.tipoContratacaoDescricao());
        v.setFornecedorId(foto.fornecedorId());
        v.setCnpjEmpresa(foto.cnpjEmpresa());
        v.setRazaoSocialEmpresa(foto.razaoSocialEmpresa());
        v.setNomeFantasiaEmpresa(foto.nomeFantasiaEmpresa());
        v.setDataInicio(foto.dataInicio());
        v.setDataFim(foto.dataFim());
        v.setEfetivoPrevisto(foto.efetivoPrevisto());
        v.setGestaoLocalId(foto.gestaoLocalId());
        v.setGestaoLocalNome(foto.gestaoLocalNome());
        return versaoRepositorio.save(v);
    }

    public List<EventoCicloVidaContrato> eventosValidosOrdenados(
            Long contratoId,
            LocalDate dataEfetivaAte,
            Instant conhecidoAte
    ) {
        LocalDate data = dataEfetivaAte == null ? RelogioSistema.hoje() : dataEfetivaAte;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;

        List<EventoCicloVidaContrato> todosConhecidos = eventoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId).stream()
                .filter(e -> e.getRegistradoEm() == null || !e.getRegistradoEm().isAfter(conhecimento))
                .toList();
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacaoRepositorio
                .findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(contratoId, conhecimento);

        // A validade de um fato é reconstruída com todas as relações já conhecidas
        // no instante da consulta; só depois aplicamos o corte da data efetiva.
        Set<Long> ativos = resolverIdsAtivos(todosConhecidos, relacoes);
        List<EventoCicloVidaContrato> validos = todosConhecidos.stream()
                .filter(e -> e.getDataEfetiva() != null && !e.getDataEfetiva().isAfter(data))
                .filter(e -> e.getId() != null && ativos.contains(e.getId()))
                .toList();
        return ordenarSemUsarIdOuTimestamp(validos, relacoes);
    }

    public List<RelacaoPrecedenciaEventoContrato> relacoesConhecidas(Long contratoId, Instant conhecidoAte) {
        if (conhecidoAte == null) {
            return relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(contratoId);
        }
        return relacaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(
                contratoId, conhecidoAte);
    }

    public List<VersaoContratoNegocialDTO> listarVersoes(Long contratoId) {
        return versaoRepositorio.findByContratoIdOrderByNumeroVersaoAsc(contratoId)
                .stream().map(this::paraVersaoDTO).toList();
    }

    public Optional<VersaoContratoNegocial> versaoAplicavel(
            Contrato contrato,
            LocalDate dataReferencia,
            Instant conhecidoAte
    ) {
        if (contrato == null || contrato.getId() == null || !versaoRepositorio.existsByContratoId(contrato.getId())) {
            return Optional.empty();
        }
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;

        List<VersaoContratoNegocial> versoes = versaoRepositorio
                .findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(contrato.getId(), conhecimento);
        if (versoes.isEmpty()) return Optional.empty();

        // Versões cujo evento de origem foi invalidado por relação conhecida são desconsideradas.
        List<EventoCicloVidaContrato> todosConhecidos = eventoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(e -> e.getRegistradoEm() == null || !e.getRegistradoEm().isAfter(conhecimento))
                .toList();
        Set<Long> ativos = resolverIdsAtivos(
                todosConhecidos,
                relacaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(
                        contrato.getId(), conhecimento)
        );
        versoes = versoes.stream()
                .filter(v -> v.getEventoOrigem() == null
                        || v.getEventoOrigem().getId() == null
                        || ativos.contains(v.getEventoOrigem().getId()))
                .toList();
        if (versoes.isEmpty()) return Optional.empty();

        List<VersaoContratoNegocial> aplicaveis = versoes.stream()
                .filter(v -> v.getDataEfetiva() != null && !v.getDataEfetiva().isAfter(data))
                .toList();
        if (!aplicaveis.isEmpty()) {
            // Número da versão indica ordem de registro, não ordem negocial.
            // Para fato retroativo, a versão aplicável é definida primeiro pela
            // data efetiva e, quando a data é a mesma, pela ordem explícita dos
            // eventos reconstruída pelo motor temporal.
            List<EventoCicloVidaContrato> ordenados = eventosValidosOrdenados(contrato.getId(), data, conhecimento);
            Map<Long, Integer> ordemEvento = new HashMap<>();
            for (int i = 0; i < ordenados.size(); i++) {
                if (ordenados.get(i).getId() != null) ordemEvento.put(ordenados.get(i).getId(), i);
            }
            return aplicaveis.stream().max(Comparator
                    .comparing(VersaoContratoNegocial::getDataEfetiva)
                    .thenComparingInt(v -> v.getEventoOrigem() != null && v.getEventoOrigem().getId() != null
                            ? ordemEvento.getOrDefault(v.getEventoOrigem().getId(), -1) : -1));
        }
        // Contrato já publicado mas ainda FUTURO: expõe sua primeira fotografia conhecida.
        return Optional.of(versoes.get(0));
    }

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
                case CORRECAO_ANULACAO -> estado = EstadoNegocialContrato.PUBLICADO;
                case PUBLICACAO -> {
                    if (estado != EstadoNegocialContrato.CANCELADO
                            && estado != EstadoNegocialContrato.ANULADO) {
                        estado = EstadoNegocialContrato.PUBLICADO;
                    }
                }
                default -> { /* Os demais fatos são refletidos pela versão contratual aplicável. */ }
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

    private EstadoNegocialContrato estadoInicialConhecido(Contrato contrato, Instant conhecidoAte) {
        if (contrato.getPublicadoEm() == null) {
            return EstadoNegocialContrato.RASCUNHO;
        }
        return contrato.getPublicadoEm().isAfter(conhecidoAte)
                ? EstadoNegocialContrato.RASCUNHO
                : EstadoNegocialContrato.PUBLICADO;
    }

    private LocalDate dataFimOriginalFallback(Contrato contrato) {
        List<EventoCicloVidaContrato> eventos = eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId());
        for (EventoCicloVidaContrato e : eventos) {
            if (e.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO
                    && e.getValorAnterior() != null) {
                try { return LocalDate.parse(e.getValorAnterior()); }
                catch (RuntimeException ignored) { }
            }
        }
        return contrato.getDataFim();
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

    private RelacaoPrecedenciaEventoContrato registrarRelacao(
            EventoCicloVidaContrato fonte,
            EventoCicloVidaContrato alvo,
            TipoRelacaoPrecedenciaNegocial tipo,
            UsuarioAplicacao usuario,
            String motivo
    ) {
        if (Objects.equals(fonte.getId(), alvo.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento não pode se relacionar consigo mesmo.");
        }
        RelacaoPrecedenciaEventoContrato r = new RelacaoPrecedenciaEventoContrato();
        r.setContrato(fonte.getContrato());
        r.setEventoFonte(fonte);
        r.setEventoAlvo(alvo);
        r.setTipoRelacao(tipo);
        r.setUsuarioResponsavelId(usuario != null ? usuario.getId() : null);
        r.setUsuarioResponsavelUsername(usuario != null ? usuario.getUsername() : null);
        r.setMotivo(motivo);
        RelacaoPrecedenciaEventoContrato salva = relacaoRepositorio.save(r);

        switch (tipo) {
            case RETIFICA -> alvo.setSituacaoFato(SituacaoFatoNegocial.RETIFICADO);
            case CANCELA -> alvo.setSituacaoFato(SituacaoFatoNegocial.CANCELADO);
            case SUBSTITUI -> alvo.setSituacaoFato(SituacaoFatoNegocial.SUBSTITUIDO);
            default -> { }
        }
        eventoRepositorio.save(alvo);
        recalcularSituacoesProjecaoAtual(fonte.getContrato().getId());
        return salva;
    }

    private void recalcularSituacoesProjecaoAtual(Long contratoId) {
        List<EventoCicloVidaContrato> eventos = eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId);
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(contratoId);
        Set<Long> ativos = resolverIdsAtivos(eventos, relacoes);

        for (EventoCicloVidaContrato evento : eventos) {
            SituacaoFatoNegocial nova = SituacaoFatoNegocial.VIGENTE;
            if (evento.getId() != null && !ativos.contains(evento.getId())) {
                for (int i = relacoes.size() - 1; i >= 0; i--) {
                    RelacaoPrecedenciaEventoContrato r = relacoes.get(i);
                    if (!Objects.equals(id(r.getEventoAlvo()), evento.getId())) continue;
                    Long fonteId = id(r.getEventoFonte());
                    if (fonteId == null || !ativos.contains(fonteId)) continue;
                    nova = switch (r.getTipoRelacao()) {
                        case RETIFICA -> SituacaoFatoNegocial.RETIFICADO;
                        case CANCELA -> SituacaoFatoNegocial.CANCELADO;
                        case SUBSTITUI -> SituacaoFatoNegocial.SUBSTITUIDO;
                        default -> SituacaoFatoNegocial.VIGENTE;
                    };
                    break;
                }
            }
            if (evento.getSituacaoFato() != nova) evento.setSituacaoFato(nova);
        }
        eventoRepositorio.saveAll(eventos);
    }

    private List<EventoCicloVidaContrato> conflitosAtivos(Long contratoId, String chave, LocalDate data) {
        List<EventoCicloVidaContrato> eventos = eventoRepositorio
                .findByContratoIdAndChaveNegocialAndDataEfetiva(contratoId, chave, data);
        if (eventos.isEmpty()) return List.of();
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(contratoId);
        List<EventoCicloVidaContrato> todos = eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId);
        Set<Long> ativos = resolverIdsAtivos(todos, relacoes);
        return eventos.stream().filter(e -> e.getId() != null && ativos.contains(e.getId())).toList();
    }

    /**
     * Invalida fatos pela cadeia conhecida sem apagar registros. Processar relações
     * do conhecimento mais recente para o mais antigo evita que uma correção já
     * invalidada continue invalidando seu alvo.
     */
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

    private List<EventoCicloVidaContrato> ordenarSemUsarIdOuTimestamp(
            List<EventoCicloVidaContrato> eventos,
            List<RelacaoPrecedenciaEventoContrato> relacoes
    ) {
        if (eventos.size() < 2) return eventos;
        Map<LocalDate, List<EventoCicloVidaContrato>> porData = eventos.stream()
                .collect(Collectors.groupingBy(EventoCicloVidaContrato::getDataEfetiva, TreeMap::new, Collectors.toList()));
        List<EventoCicloVidaContrato> resultado = new ArrayList<>();
        for (List<EventoCicloVidaContrato> grupo : porData.values()) {
            resultado.addAll(ordenarGrupoMesmaData(grupo, relacoes));
        }
        return resultado;
    }

    private List<EventoCicloVidaContrato> ordenarGrupoMesmaData(
            List<EventoCicloVidaContrato> grupo,
            List<RelacaoPrecedenciaEventoContrato> relacoes
    ) {
        Map<Long, EventoCicloVidaContrato> porId = grupo.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(EventoCicloVidaContrato::getId, Function.identity()));
        Map<Long, Set<Long>> saidas = new HashMap<>();
        Map<Long, Integer> entrada = new HashMap<>();
        porId.keySet().forEach(id -> { saidas.put(id, new LinkedHashSet<>()); entrada.put(id, 0); });

        for (RelacaoPrecedenciaEventoContrato r : relacoes == null ? List.<RelacaoPrecedenciaEventoContrato>of() : relacoes) {
            Long fonte = id(r.getEventoFonte());
            Long alvo = id(r.getEventoAlvo());
            if (!porId.containsKey(fonte) || !porId.containsKey(alvo)) continue;
            if (r.getTipoRelacao() == TipoRelacaoPrecedenciaNegocial.SUCEDE) {
                if (saidas.get(alvo).add(fonte)) entrada.put(fonte, entrada.get(fonte) + 1);
            }
        }

        Comparator<EventoCicloVidaContrato> desempateNaoNegocial = Comparator
                .comparing((EventoCicloVidaContrato e) -> e.getOrdemDocumental() == null ? Integer.MAX_VALUE : e.getOrdemDocumental())
                .thenComparing(EventoCicloVidaContrato::getChaveNegocial, Comparator.nullsLast(String::compareTo))
                .thenComparing(e -> e.getTipoEvento().name())
                .thenComparing(EventoCicloVidaContrato::getFatoLogicoId, Comparator.nullsLast(String::compareTo));

        PriorityQueue<EventoCicloVidaContrato> fila = new PriorityQueue<>(desempateNaoNegocial);
        porId.values().stream().filter(e -> entrada.get(e.getId()) == 0).forEach(fila::add);
        List<EventoCicloVidaContrato> ordenados = new ArrayList<>();
        while (!fila.isEmpty()) {
            EventoCicloVidaContrato atual = fila.poll();
            ordenados.add(atual);
            for (Long prox : saidas.get(atual.getId())) {
                int novo = entrada.compute(prox, (k, v) -> v - 1);
                if (novo == 0) fila.add(porId.get(prox));
            }
        }
        if (ordenados.size() != porId.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ciclo inválido detectado nas relações de precedência negocial.");
        }
        validarAmbiguidadeMesmaChave(ordenados, relacoes);
        return ordenados;
    }

    private void validarAmbiguidadeMesmaChave(
            List<EventoCicloVidaContrato> ordenados,
            List<RelacaoPrecedenciaEventoContrato> relacoes
    ) {
        Map<String, List<EventoCicloVidaContrato>> porChave = ordenados.stream()
                .collect(Collectors.groupingBy(EventoCicloVidaContrato::getChaveNegocial));
        for (Map.Entry<String, List<EventoCicloVidaContrato>> entry : porChave.entrySet()) {
            if (entry.getValue().size() < 2 || !chaveSingular(entry.getKey())) continue;
            for (int i = 1; i < entry.getValue().size(); i++) {
                Long anterior = entry.getValue().get(i - 1).getId();
                Long atual = entry.getValue().get(i).getId();
                boolean sucede = relacoes.stream().anyMatch(r ->
                        r.getTipoRelacao() == TipoRelacaoPrecedenciaNegocial.SUCEDE
                                && Objects.equals(id(r.getEventoAlvo()), anterior)
                                && Objects.equals(id(r.getEventoFonte()), atual));
                if (!sucede) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Eventos conflitantes na mesma data/chave sem cadeia SUCEDE explícita: " + entry.getKey()
                    );
                }
            }
        }
    }

    private EventoCicloVidaContrato ultimoOrdenadoDaChave(
            List<EventoCicloVidaContrato> eventos,
            Long contratoId
    ) {
        if (eventos.size() == 1) return eventos.get(0);
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(contratoId);
        List<EventoCicloVidaContrato> ordenados = ordenarGrupoMesmaData(eventos, relacoes);
        return ordenados.isEmpty() ? null : ordenados.get(ordenados.size() - 1);
    }

    private ResponseStatusException conflitoSemPrecedencia(
            String chave,
            LocalDate data,
            List<EventoCicloVidaContrato> conflitos
    ) {
        String ids = conflitos.stream().map(e -> String.valueOf(e.getId())).collect(Collectors.joining(", "));
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Existe fato conflitante para " + chave + " em " + data
                        + ". Informe eventoPrecedenteId e relacaoPrecedencia. Eventos conflitantes: " + ids
        );
    }

    private void validarParPrecedencia(AcaoCicloVidaContratoDTO acao) {
        if ((acao.eventoPrecedenteId() == null) != (acao.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos."
            );
        }
        if (acao.ordemDocumental() != null && acao.ordemDocumental() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ordemDocumental deve ser positiva.");
        }
    }

    private void validarPrecedenteSemAutoReferencia(Contrato contrato, EventoCicloVidaContrato precedente) {
        if (precedente.getContrato() == null || !Objects.equals(contrato.getId(), precedente.getContrato().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Precedência entre contratos diferentes não é permitida.");
        }
    }

    public static String chaveNegocialPadrao(TipoEventoCicloVidaContrato tipo) {
        return switch (tipo) {
            case INATIVACAO_ADMINISTRATIVA, REATIVACAO_ADMINISTRATIVA -> "HABILITACAO_ADMINISTRATIVA";
            case ENCERRAMENTO_ANTECIPADO -> "VIGENCIA";
            case CORRECAO_IDENTIFICADOR -> "IDENTIFICACAO";
            case TRANSFERENCIA_GESTAO_LOCAL -> "GESTAO_LOCAL";
            case ADITAMENTO -> "ADITAMENTO";
            case CANCELAMENTO_ADITAMENTO -> "CANCELAMENTO_ADITAMENTO";
            case REMANEJAMENTO -> "REMANEJAMENTO";
            case CANCELAMENTO_REMANEJAMENTO -> "CANCELAMENTO_REMANEJAMENTO";
            case PUBLICACAO, CANCELAMENTO_CONTRATO_FUTURO, ANULACAO, CORRECAO_ANULACAO -> "ESTADO_NEGOCIAL";
        };
    }

    private boolean chaveSingular(String chave) {
        return Set.of("HABILITACAO_ADMINISTRATIVA", "VIGENCIA", "IDENTIFICACAO", "GESTAO_LOCAL", "ESTADO_NEGOCIAL")
                .contains(chave);
    }

    private Long id(EventoCicloVidaContrato e) { return e == null ? null : e.getId(); }

    public EventoCicloVidaContratoDTO paraEventoDTO(EventoCicloVidaContrato e) {
        return new EventoCicloVidaContratoDTO(
                e.getId(), e.getTipoEvento(), e.getFatoLogicoId(), e.getChaveNegocial(),
                e.getVersaoNegocial(), e.getOrdemDocumental(), e.getSituacaoFato(),
                e.getDataEfetiva(), e.getDataAto(), e.getRegistradoEm(),
                e.getUsuarioResponsavelId(), e.getUsuarioResponsavelUsername(),
                e.getMotivo(), e.getReferenciaSei(), e.getValorAnterior(), e.getValorNovo()
        );
    }

    public RelacaoPrecedenciaEventoContratoDTO paraRelacaoDTO(RelacaoPrecedenciaEventoContrato r) {
        return new RelacaoPrecedenciaEventoContratoDTO(
                r.getId(), id(r.getEventoFonte()), id(r.getEventoAlvo()), r.getTipoRelacao(),
                r.getRegistradoEm(), r.getUsuarioResponsavelId(), r.getUsuarioResponsavelUsername(), r.getMotivo()
        );
    }

    public VersaoContratoNegocialDTO paraVersaoDTO(VersaoContratoNegocial v) {
        return new VersaoContratoNegocialDTO(
                v.getId(), v.getNumeroVersao(), v.getEventoOrigem() != null ? v.getEventoOrigem().getId() : null,
                v.getDataEfetiva(), v.getRegistradoEm(), v.getNumeroContrato(), v.getAnoContrato(), v.getDescricao(),
                v.getTipoContratacaoId(), v.getTipoContratacaoDescricao(), v.getFornecedorId(), v.getCnpjEmpresa(),
                v.getRazaoSocialEmpresa(), v.getNomeFantasiaEmpresa(), v.getDataInicio(), v.getDataFim(),
                v.getEfetivoPrevisto(), v.getGestaoLocalId(), v.getGestaoLocalNome()
        );
    }
}
