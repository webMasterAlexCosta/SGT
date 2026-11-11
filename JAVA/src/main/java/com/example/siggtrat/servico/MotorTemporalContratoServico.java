package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.EventoCicloVidaContratoRepositorio;
import com.example.siggtrat.repositorio.RelacaoPrecedenciaEventoContratoRepositorio;
import com.example.siggtrat.repositorio.VersaoContratoNegocialRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.beans.factory.ObjectProvider;
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
public class MotorTemporalContratoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(MotorTemporalContratoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final EventoCicloVidaContratoRepositorio eventoRepositorio;
    private final RelacaoPrecedenciaEventoContratoRepositorio relacaoRepositorio;
    private final VersaoContratoNegocialRepositorio versaoRepositorio;
    private final AditamentoContratualRepositorio aditamentoRepositorio;

    public MotorTemporalContratoServico(
            EventoCicloVidaContratoRepositorio eventoRepositorio,
            RelacaoPrecedenciaEventoContratoRepositorio relacaoRepositorio,
            VersaoContratoNegocialRepositorio versaoRepositorio,
            AditamentoContratualRepositorio aditamentoRepositorio
    ) {
        this.eventoRepositorio = eventoRepositorio;
        this.relacaoRepositorio = relacaoRepositorio;
        this.versaoRepositorio = versaoRepositorio;
        this.aditamentoRepositorio = aditamentoRepositorio;
    }

    private ConsultaProvaHistoricaContratoServico provasHistoricas;
    private ObjectProvider<AditamentoContratualServico> projecaoNegocial;
    @org.springframework.beans.factory.annotation.Autowired
    public void configurarProjecaoNegocial(ObjectProvider<AditamentoContratualServico> servico) {
        this.projecaoNegocial = servico;
    }
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void configurarProvasHistoricas(ConsultaProvaHistoricaContratoServico servico) {
        this.provasHistoricas=servico;
    }
    public Map<Long,VersaoContratoNegocialDTO> fotografiasConhecidasPorEvento(
            List<VersaoContratoNegocialDTO> versoes, Instant conhecido) {
        if(provasHistoricas!=null)return provasHistoricas.selecionarFotografias(versoes,conhecido);
        return versoes.stream().filter(v->v.eventoOrigemId()!=null).collect(Collectors.toMap(
                VersaoContratoNegocialDTO::eventoOrigemId, v->v,(a,b)->a));
    }
    public Map<Long,VersaoContratoNegocialDTO> fotografiasConhecidasPorEvento(
            List<VersaoContratoNegocialDTO> versoes, Instant conhecido, LeituraHistoricaContratos entradas) {
        return entradas==null||provasHistoricas==null?fotografiasConhecidasPorEvento(versoes,conhecido)
                :provasHistoricas.selecionarFotografias(versoes,conhecido,entradas);
    }
    public boolean transferenciaComProva(Long evento, Instant conhecido) {
        return campoHistoricoComprovado(evento,"tipoContratacaoId",conhecido);
    }
    public boolean transferenciaComProva(Long evento,Instant conhecido,LeituraHistoricaContratos entradas) {
        return campoHistoricoComprovado(evento,"tipoContratacaoId",conhecido,entradas);
    }
    public boolean campoHistoricoComprovado(Long evento,String campo,Instant conhecido) {
        return provasHistoricas!=null && provasHistoricas.campoComprovado(evento,campo,conhecido);
    }
    public boolean campoHistoricoComprovado(Long evento,String campo,Instant conhecido,LeituraHistoricaContratos entradas) {
        return entradas==null?campoHistoricoComprovado(evento,campo,conhecido)
                :provasHistoricas!=null&&provasHistoricas.campoComprovado(evento,campo,conhecido,entradas);
    }
    public boolean campoConhecidoNaFotografia(VersaoContratoNegocialDTO foto,String campo,boolean legado) {
        return provasHistoricas==null?legado:provasHistoricas.campoConhecidoNaFotografia(foto,campo,legado);
    }
    public boolean campoConhecidoNaFotografia(VersaoContratoNegocialDTO foto,String campo,boolean legado,
                                               LeituraHistoricaContratos entradas) {
        return entradas==null?campoConhecidoNaFotografia(foto,campo,legado):provasHistoricas==null?legado
                :provasHistoricas.campoConhecidoNaFotografia(foto,campo,legado,entradas);
    }
    public Map<String,ProvaCampoHistoricoDTO> provasDaFotografia(VersaoContratoNegocialDTO foto) {
        if(foto==null)return Map.of();
        return provasHistoricas==null?ProvasCamposHistoricos.desconhecidos(ProvasCamposHistoricos.valores(foto).keySet())
                :provasHistoricas.camposFotografia(foto);
    }
    public Map<String,ProvaCampoHistoricoDTO> provasDaFotografia(VersaoContratoNegocialDTO foto,LeituraHistoricaContratos entradas) {
        return entradas==null||provasHistoricas==null?provasDaFotografia(foto):provasHistoricas.camposFotografia(foto,entradas);
    }
    public Map<String,ProvaCampoHistoricoDTO> provasDiretasDaTransferencia(VersaoContratoNegocialDTO foto) {
        if(foto==null)return Map.of();
        return provasHistoricas==null?Map.of():provasHistoricas.camposDiretosTransferencia(foto);
    }
    public Map<String,ProvaCampoHistoricoDTO> provasDiretasDaTransferencia(VersaoContratoNegocialDTO foto,LeituraHistoricaContratos entradas) {
        return entradas==null||provasHistoricas==null?provasDiretasDaTransferencia(foto)
                :provasHistoricas.camposDiretosTransferencia(foto,entradas);
    }

    /** O chamador fornece somente os contratos de sua seleção autorizada. Nenhum corte D/K é antecipado. */
    @Transactional(readOnly=true)
    public LeituraHistoricaContratos carregarLeitura(Collection<Long> contratoIds) {
        Set<Long> ids=contratoIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if(ids.isEmpty())return new LeituraHistoricaContratos(ids,List.of(),List.of(),List.of(),List.of());
        return new LeituraHistoricaContratos(ids,
                eventoRepositorio.findByContratoIdInOrderByContratoIdAscDataEfetivaAscRegistradoEmAsc(ids),
                relacaoRepositorio.findByContratoIdInOrderByContratoIdAscRegistradoEmAsc(ids),
                versaoRepositorio.findByContratoIdInOrderByContratoIdAscNumeroVersaoAsc(ids),
                provasHistoricas==null?List.of():provasHistoricas.carregarProvas(ids));
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
        return registrarEvento(contrato, tipo, acao, usuario, valorAnterior, valorNovo, chaveNegocialCustomizada, null);
    }

    @Transactional
    public EventoCicloVidaContrato registrarEvento(Contrato contrato, TipoEventoCicloVidaContrato tipo,
            AcaoCicloVidaContratoDTO acao, UsuarioAplicacao usuario, String valorAnterior, String valorNovo,
            String chaveNegocialCustomizada, ConferenciaDocumentalTransferencia conferencia) {
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
            boolean invalida = acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.CANCELA
                    || acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || acao.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI;
            if (invalida && !podeInvalidar(tipo, precedente.getTipoEvento(), acao.relacaoPrecedencia())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A relação não pode invalidar fatos de outra operação negocial; use a correção ou cancelamento próprio do fato.");
            }
        }

        List<EventoCicloVidaContrato> conflitosAtivos = conflitosAtivos(contrato.getId(), chave, acao.dataEfetiva());
        if (tipo == TipoEventoCicloVidaContrato.CORRECAO_CNPJ) {
            Set<Long> aditamentosCnpj = aditamentoRepositorio
                    .findByContratoIdAndDataEfetiva(contrato.getId(), acao.dataEfetiva()).stream()
                    .filter(a -> a.getEvento() != null && a.getNovoCnpjEmpresa() != null)
                    .map(a -> a.getEvento().getId()).filter(Objects::nonNull).collect(Collectors.toSet());
            List<EventoCicloVidaContrato> conflitosCnpj = eventosValidosOrdenados(
                    contrato.getId(), acao.dataEfetiva(), Instant.now()).stream()
                    .filter(e -> Objects.equals(e.getDataEfetiva(), acao.dataEfetiva()))
                    .filter(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_CNPJ
                            || aditamentosCnpj.contains(e.getId())).toList();
            // CNPJ é um único campo, mesmo quando os fatos vêm de operações e chaves distintas.
            validarPrecedenciaCompleta(contrato, acao.dataEfetiva(), conflitosCnpj,
                    acao.eventoPrecedenteId(), acao.relacaoPrecedencia(), acao.ordemDocumental());
        } else if (!conflitosAtivos.isEmpty()) {
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

        if (conferencia != null) {
            if (tipo != TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL
                    && tipo != TipoEventoCicloVidaContrato.CANCELAMENTO_TRANSFERENCIA_GESTAO)
                throw new IllegalStateException("Conferência de transferência não pertence a esta operação.");
            var uso = conferencia.getUsoPermissaoExcepcional();
            if (!Objects.equals(uso.getRecursoId(), contrato.getId()) || !"Contrato".equals(uso.getTipoRecurso())
                    || !Objects.equals(uso.getUsuarioId(), evento.getUsuarioResponsavelId())
                    || uso.getTipo() != com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL)
                throw new IllegalStateException("O uso da concessão não corresponde ao evento protegido.");
            evento.registrarConferenciaDocumental(conferencia);
        }
        EventoCicloVidaContrato salvo = eventoRepositorio.save(evento);

        if (precedente != null && acao.relacaoPrecedencia() != null) {
            registrarRelacao(salvo, precedente, acao.relacaoPrecedencia(), usuario, acao.motivo());
        }
        return salvo;
    }

    private boolean podeInvalidar(TipoEventoCicloVidaContrato fonte, TipoEventoCicloVidaContrato alvo,
                                 TipoRelacaoPrecedenciaNegocial relacao) {
        if (fonte == alvo) return true;
        boolean fonteAdministrativa = fonte == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                || fonte == TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA;
        boolean alvoAdministrativo = alvo == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                || alvo == TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA;
        if (fonteAdministrativa && alvoAdministrativo) return true;
        if (relacao != TipoRelacaoPrecedenciaNegocial.CANCELA) return false;
        return switch (fonte) {
            case CORRECAO_ANULACAO -> alvo == TipoEventoCicloVidaContrato.ANULACAO;
            case CANCELAMENTO_ADITAMENTO -> alvo == TipoEventoCicloVidaContrato.ADITAMENTO;
            case CANCELAMENTO_REMANEJAMENTO -> alvo == TipoEventoCicloVidaContrato.REMANEJAMENTO;
            case CANCELAMENTO_TRANSFERENCIA_GESTAO -> alvo == TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL;
            case CANCELAMENTO_CONTRATO_FUTURO -> alvo == TipoEventoCicloVidaContrato.PUBLICACAO;
            default -> false;
        };
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
        v.setMatriculaVinculoObrigatoria(contrato.getMatriculaVinculoObrigatoria());
        v.setMatriculaTipoContratacaoObrigatoria(contrato.getTipoContratacao() != null
                && Boolean.TRUE.equals(contrato.getTipoContratacao().getMatriculaVinculoObrigatoria()));
        v.setMatriculaVinculoObrigatoriaEfetiva(contrato.exigeMatriculaNoVinculo());
        v.setDiaSemanaReduzido(contrato.getDiaSemanaReduzido());
        v.setHorasDiaReduzido(contrato.getHorasDiaReduzido());
        v.setHabilitacaoAdministrativa(contrato.getHabilitacaoAdministrativa());
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
        return registrarFotografiaProjetada(contrato, eventoOrigem, dataEfetiva, foto, null);
    }

    @Transactional
    public VersaoContratoNegocial registrarFotografiaProjetada(Contrato contrato, EventoCicloVidaContrato eventoOrigem,
            LocalDate dataEfetiva, VersaoContratoNegocialDTO foto, Instant conhecimentoDocumental) {
        if (contrato == null || contrato.getId() == null || foto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotografia projetada incompleta.");
        }
        int proxima = versaoRepositorio.findTopByContratoIdOrderByNumeroVersaoDesc(contrato.getId())
                .map(v -> v.getNumeroVersao() + 1).orElse(1);
        VersaoContratoNegocial v = new VersaoContratoNegocial();
        if (conhecimentoDocumental != null) v.definirConhecimentoDocumental(conhecimentoDocumental);
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
        v.setMatriculaVinculoObrigatoria(foto.matriculaVinculoObrigatoria());
        v.setMatriculaTipoContratacaoObrigatoria(foto.matriculaTipoContratacaoObrigatoria());
        v.setMatriculaVinculoObrigatoriaEfetiva(foto.matriculaVinculoObrigatoriaEfetiva());
        v.setDiaSemanaReduzido(foto.diaSemanaReduzido());
        v.setHorasDiaReduzido(foto.horasDiaReduzido());
        v.setHabilitacaoAdministrativa(foto.habilitacaoAdministrativa());
        return versaoRepositorio.save(v);
    }

    public List<EventoCicloVidaContrato> eventosValidosOrdenados(
            Long contratoId,
            LocalDate dataEfetivaAte,
            Instant conhecidoAte
    ) {
        return eventosValidosOrdenados(contratoId,dataEfetivaAte,conhecidoAte,null);
    }

    public List<EventoCicloVidaContrato> eventosValidosOrdenados(Long contratoId,LocalDate dataEfetivaAte,
                                                               Instant conhecidoAte,LeituraHistoricaContratos entradas) {
        LocalDate data = dataEfetivaAte == null ? RelogioSistema.hoje() : dataEfetivaAte;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;

        List<EventoCicloVidaContrato> todosConhecidos = eventosDoContrato(contratoId,entradas).stream()
                .filter(e -> e.getRegistradoEm() == null || !e.getRegistradoEm().isAfter(conhecimento))
                .toList();
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacoesConhecidas(contratoId,conhecimento,entradas);

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
        return relacoesConhecidas(contratoId,conhecidoAte,null);
    }
    public List<RelacaoPrecedenciaEventoContrato> relacoesConhecidas(Long contratoId,Instant conhecidoAte,
                                                                   LeituraHistoricaContratos entradas) {
        if(entradas!=null&&entradas.contemContrato(contratoId))return entradas.relacoes(contratoId,conhecidoAte);
        if (conhecidoAte == null) {
            return relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(contratoId);
        }
        return relacaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(
                contratoId, conhecidoAte);
    }

    /** Ordenação negocial compartilhada com prévias; os argumentos não são persistidos. */
    public List<EventoCicloVidaContrato> ordenarEventosParaSimulacao(
            List<EventoCicloVidaContrato> eventos, List<RelacaoPrecedenciaEventoContrato> relacoes) {
        return ordenarSemUsarIdOuTimestamp(eventos, relacoes);
    }

    /** O instrumento proposto deve estar ordenado em relação a cada fato que ele realmente afeta. */
    public void validarPrecedenciaCompleta(Contrato contrato, LocalDate data,
                                           Collection<EventoCicloVidaContrato> conflitos,
                                           Long precedenteId, TipoRelacaoPrecedenciaNegocial tipoRelacao,
                                           Integer ordemDocumental) {
        if (conflitos == null || conflitos.isEmpty()) return;
        if (precedenteId == null || tipoRelacao == null
                || tipoRelacao == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fatos conflitantes na mesma data exigem precedência negocial completa.");
        }
        EventoCicloVidaContrato precedente = eventoRepositorio.findById(precedenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento precedente não encontrado."));
        validarPrecedenteSemAutoReferencia(contrato, precedente);
        Instant conhecido = Instant.now();
        List<EventoCicloVidaContrato> grupo = new ArrayList<>(eventosValidosOrdenados(contrato.getId(), data, conhecido)
                .stream().filter(e -> Objects.equals(data, e.getDataEfetiva())).toList());
        EventoCicloVidaContrato proposta = new EventoCicloVidaContrato() {
            @Override public Long getId() { return Long.MIN_VALUE; }
        };
        proposta.setContrato(contrato);
        proposta.setDataEfetiva(data);
        proposta.setTipoEvento(TipoEventoCicloVidaContrato.ADITAMENTO);
        proposta.setChaveNegocial("PROPOSTA_NAO_PERSISTIDA");
        proposta.setFatoLogicoId("PROPOSTA_NAO_PERSISTIDA");
        proposta.setOrdemDocumental(ordemDocumental);
        boolean substitui = tipoRelacao == TipoRelacaoPrecedenciaNegocial.RETIFICA
                || tipoRelacao == TipoRelacaoPrecedenciaNegocial.SUBSTITUI;
        if (substitui) proposta.setFatoLogicoId(precedente.getFatoLogicoId());
        if (substitui || tipoRelacao == TipoRelacaoPrecedenciaNegocial.CANCELA)
            grupo.removeIf(e -> Objects.equals(e.getId(), precedenteId));
        grupo.add(proposta);
        List<RelacaoPrecedenciaEventoContrato> relacoes = new ArrayList<>(relacoesConhecidas(contrato.getId(), conhecido));
        if (tipoRelacao == TipoRelacaoPrecedenciaNegocial.SUCEDE) {
            RelacaoPrecedenciaEventoContrato relacao = new RelacaoPrecedenciaEventoContrato();
            relacao.setEventoFonte(proposta); relacao.setEventoAlvo(precedente);
            relacao.setTipoRelacao(tipoRelacao); relacoes.add(relacao);
        }
        // Também detecta uma sequência formal que contradiga a relação explícita.
        ordenarGrupoMesmaData(grupo, relacoes);
        Map<Long, Set<Long>> grafo = grafoPrecedencia(grupo, relacoes);
        for (EventoCicloVidaContrato conflito : conflitos) {
            if ((substitui || tipoRelacao == TipoRelacaoPrecedenciaNegocial.CANCELA)
                    && Objects.equals(conflito.getId(), precedenteId)) continue;
            if (!alcanca(grafo, conflito.getId(), proposta.getId())
                    && !alcanca(grafo, proposta.getId(), conflito.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A precedência informada não ordena todos os fatos conflitantes na mesma data. Evento sem sequência: "
                                + conflito.getId() + ".");
            }
        }
    }

    public List<VersaoContratoNegocialDTO> listarVersoes(Long contratoId) {
        return listarVersoes(contratoId,null);
    }
    public List<VersaoContratoNegocialDTO> listarVersoes(Long contratoId,LeituraHistoricaContratos entradas) {
        return (entradas!=null&&entradas.contemContrato(contratoId)?entradas.versoes(contratoId)
                :versaoRepositorio.findByContratoIdOrderByNumeroVersaoAsc(contratoId))
                .stream().map(this::paraVersaoDTO).toList();
    }
    public List<VersaoContratoLeituraDTO> listarVersoesLeitura(Long contratoId,Instant conhecidoAte) {
        Instant conhecido=conhecidoAte==null?Instant.now():conhecidoAte;
        return versaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(contratoId,conhecido)
                .stream().map(v->{var dados=paraVersaoDTO(v);return new VersaoContratoLeituraDTO(dados,provasDaFotografia(dados));}).toList();
    }

    /**
     * A publicação é a única base que pode anteceder os deltas contratuais.
     * Uma fotografia posterior não vira a origem quando a primeira se perdeu.
     * A versão 1 sem evento preserva a identificação explícita da origem no
     * acervo anterior à cadeia de eventos; versão ligada a outro ato não a prova.
     */
    public Optional<VersaoContratoNegocial> fotografiaOriginalConhecida(Contrato contrato, Instant conhecidoAte) {
        return fotografiaOriginalConhecida(contrato,conhecidoAte,null);
    }
    public Optional<VersaoContratoNegocial> fotografiaOriginalConhecida(Contrato contrato,Instant conhecidoAte,
                                                                       LeituraHistoricaContratos entradas) {
        if (contrato == null || contrato.getId() == null) return Optional.empty();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        if(provasHistoricas!=null) {
            var prova=entradas==null?provasHistoricas.original(contrato.getId(),conhecimento)
                    :provasHistoricas.original(contrato.getId(),conhecimento,entradas);
            if(prova.isPresent())return prova;
        }
        return versoesConhecidas(contrato.getId(),conhecimento,entradas).stream()
                .filter(v -> v.getRegistradoEm() != null && !v.getRegistradoEm().isAfter(conhecimento))
                .filter(v -> {
                    EventoCicloVidaContrato origem = v.getEventoOrigem();
                    if (origem == null) return Objects.equals(v.getNumeroVersao(), 1);
                    return origem.getTipoEvento() == TipoEventoCicloVidaContrato.PUBLICACAO
                            && origem.getRegistradoEm() != null
                            && !origem.getRegistradoEm().isAfter(conhecimento);
                }).findFirst();
    }

    public Optional<VersaoContratoNegocial> versaoAplicavel(
            Contrato contrato,
            LocalDate dataReferencia,
            Instant conhecidoAte
    ) {
        return versaoAplicavel(contrato,dataReferencia,conhecidoAte,null);
    }
    public Optional<VersaoContratoNegocial> versaoAplicavel(Contrato contrato,LocalDate dataReferencia,
                                                          Instant conhecidoAte,LeituraHistoricaContratos entradas) {
        if (contrato == null || contrato.getId() == null || (entradas!=null&&entradas.contemContrato(contrato.getId())
                ?entradas.versoes(contrato.getId()).isEmpty():!versaoRepositorio.existsByContratoId(contrato.getId()))) {
            return Optional.empty();
        }
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;

        List<VersaoContratoNegocial> versoes = versoesConhecidas(contrato.getId(),conhecimento,entradas);
        if (versoes.isEmpty()) return Optional.empty();

        // Versões cujo evento de origem foi invalidado por relação conhecida são desconsideradas.
        List<EventoCicloVidaContrato> todosConhecidos = eventosDoContrato(contrato.getId(),entradas).stream()
                .filter(e -> e.getRegistradoEm() == null || !e.getRegistradoEm().isAfter(conhecimento))
                .toList();
        Set<Long> ativos = resolverIdsAtivos(
                todosConhecidos,
                relacoesConhecidas(contrato.getId(), conhecimento, entradas)
        );
        versoes = versoes.stream()
                .filter(v -> provasHistoricas==null || (entradas==null?provasHistoricas.versaoSelecionada(v,conhecimento)
                        :provasHistoricas.versaoSelecionada(v,conhecimento,entradas)))
                .filter(v -> v.getEventoOrigem() == null
                        || v.getEventoOrigem().getId() == null
                        || ativos.contains(v.getEventoOrigem().getId()))
                .toList();
        if (versoes.isEmpty()) return fotografiaOriginalConhecida(contrato, conhecimento, entradas);

        List<VersaoContratoNegocial> aplicaveis = versoes.stream()
                .filter(v -> v.getDataEfetiva() != null && !v.getDataEfetiva().isAfter(data))
                .toList();
        if (!aplicaveis.isEmpty()) {
            // Número da versão indica ordem de registro, não ordem negocial.
            // Para fato retroativo, a versão aplicável é definida primeiro pela
            // data efetiva e, quando a data é a mesma, pela ordem explícita dos
            // eventos reconstruída pelo motor temporal.
            List<EventoCicloVidaContrato> ordenados = eventosValidosOrdenados(contrato.getId(), data, conhecimento, entradas);
            Map<Long, Integer> ordemEvento = new HashMap<>();
            for (int i = 0; i < ordenados.size(); i++) {
                if (ordenados.get(i).getId() != null) ordemEvento.put(ordenados.get(i).getId(), i);
            }
            return aplicaveis.stream().max(Comparator
                    .comparing(VersaoContratoNegocial::getDataEfetiva)
                    .thenComparingInt(v -> v.getEventoOrigem() != null && v.getEventoOrigem().getId() != null
                            ? ordemEvento.getOrDefault(v.getEventoOrigem().getId(), -1) : -1));
        }
        // A publicação conhecida também descreve um contrato ainda FUTURO.
        // Um aditamento futuro, porém, não certifica o contexto que o antecedeu.
        return fotografiaOriginalConhecida(contrato, conhecimento, entradas);
    }

    public PosicaoTemporalContratoDTO reconstruirPosicao(
            Contrato contrato,
            LocalDate dataReferencia,
            Instant conhecidoAte
    ) {
        return reconstruirPosicao(contrato,dataReferencia,conhecidoAte,null);
    }
    public PosicaoTemporalContratoDTO reconstruirPosicao(Contrato contrato,LocalDate dataReferencia,
                                                         Instant conhecidoAte,LeituraHistoricaContratos entradas) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoCicloVidaContrato> eventos = eventosValidosOrdenados(contrato.getId(), data, conhecimento, entradas);
        List<RelacaoPrecedenciaEventoContrato> relacoes = relacoesConhecidas(contrato.getId(), conhecimento, entradas);
        Optional<VersaoContratoNegocial> versaoOpt = versaoAplicavel(contrato, data, conhecimento, entradas);

        // A foto integral de um ato posterior pode ter sido registrada antes de
        // uma prorrogação retroativa. Só a origem conhecida inicia o replay; os
        // aditamentos seguintes contribuem exclusivamente com seus próprios deltas.
        Optional<VersaoContratoNegocial> original = fotografiaOriginalConhecida(contrato, conhecimento, entradas);
        VersaoContratoNegocial fonte = original.orElseGet(() -> versaoOpt.orElse(null));
        Map<Long, AditamentoContratualRepositorio.DeltaVigenciaConhecido> deltas = eventos.stream()
                .noneMatch(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.ADITAMENTO) ? Map.of()
                : aditamentoRepositorio.buscarDeltasVigenciaConhecidos(contrato.getId(), conhecimento).stream()
                    .collect(Collectors.toMap(AditamentoContratualRepositorio.DeltaVigenciaConhecido::getEventoId,
                            Function.identity()));
        if (original.isEmpty() && fonte != null) {
            // Uma fonte legada datada prova apenas o estado em sua própria data.
            // Sem origem não se presume se ela já incorpora deltas anteriores de
            // outros atos. Preservar a prova exige tratamento documental explícito.
            for (EventoCicloVidaContrato evento : eventos) {
                if (deltas.containsKey(evento.getId()) && !evento.getDataEfetiva().isAfter(fonte.getDataEfetiva())
                        && (fonte.getEventoOrigem() == null
                            || !Objects.equals(fonte.getEventoOrigem().getId(), evento.getId()))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A vigência não está comprovada: fotografia sem origem e aditamento anterior "
                                    + "exigem conciliação documental antes da reconstrução.");
                }
            }
        }

        boolean rascunhoNuncaPublicado = contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO
                && contrato.getPublicadoEm() == null;
        LocalDate inicio = fonte != null ? fonte.getDataInicio()
                : rascunhoNuncaPublicado ? contrato.getDataInicio() : null;
        LocalDate fimPactuado = fonte != null ? fonte.getDataFim()
                : rascunhoNuncaPublicado ? contrato.getDataFim() : null;
        LocalDate fimPorEncerramento = null;

        EstadoNegocialContrato estado = estadoInicialConhecido(contrato, conhecimento);
        HabilitacaoAdministrativaContrato habilitacao = habilitacaoAdministrativaEm(contrato, data, conhecimento, Set.of(), entradas);

        for (EventoCicloVidaContrato e : eventos) {
            switch (e.getTipoEvento()) {
                case ADITAMENTO -> {
                    var delta = deltas.get(e.getId());
                    if (delta != null) {
                        if (delta.getNovaDataInicio() != null) inicio = delta.getNovaDataInicio();
                        if (delta.getNovaDataFim() != null) fimPactuado = delta.getNovaDataFim();
                    }
                }
                case INATIVACAO_ADMINISTRATIVA -> habilitacao = HabilitacaoAdministrativaContrato.INATIVO;
                case REATIVACAO_ADMINISTRATIVA -> habilitacao = HabilitacaoAdministrativaContrato.ATIVO;
                case ENCERRAMENTO_ANTECIPADO -> fimPorEncerramento = e.getDataEfetiva();
                case CANCELAMENTO_CONTRATO_FUTURO -> estado = EstadoNegocialContrato.CANCELADO;
                case ANULACAO -> estado = EstadoNegocialContrato.ANULADO;
                case CORRECAO_ANULACAO -> {
                    // A relação CANCELA já retirou somente a anulação corrigida.
                    // Outra anulação válida continua governando o estado.
                    habilitacao = HabilitacaoAdministrativaContrato.INATIVO;
                }
                case PUBLICACAO -> {
                    if (estado != EstadoNegocialContrato.CANCELADO
                            && estado != EstadoNegocialContrato.ANULADO) {
                        estado = EstadoNegocialContrato.PUBLICADO;
                    }
                }
                default -> { /* Os demais fatos são refletidos pela versão contratual aplicável. */ }
            }
        }

        // ADI altera a vigência pactuada, mas não cancela um encerramento próprio.
        LocalDate fim = fimPorEncerramento != null ? fimPorEncerramento : fimPactuado;
        VersaoContratoNegocialDTO versao = versaoOpt.isEmpty() ? null
                : paraVersaoDTO(versaoOpt.get()).comVigencia(inicio, fim);
        if (versao != null && original.isPresent() && projecaoNegocial != null) {
            AditamentoContratualServico projetor = projecaoNegocial.getIfAvailable();
            if (projetor != null) {
                // O mesmo replay de deltas atende a API e os consumidores do motor; a foto integral de ADI não substitui a origem.
                versao = (entradas==null?projetor.projetarVersaoContrato(contrato, data, conhecimento, Set.of())
                        :projetor.projetarVersaoContrato(contrato, data, conhecimento, Set.of(), entradas)).comVigencia(inicio, fim);
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
                versao,
                eventos.stream().map(e -> paraEventoDTO(e, SituacaoFatoNegocial.VIGENTE)).toList(),
                relacoes.stream().map(this::paraRelacaoDTO).toList()
        );
    }

    /** Habilitação pela fotografia inicial e cadeia válida, sem depender de materialização por relógio. */
    public HabilitacaoAdministrativaContrato habilitacaoAdministrativaEm(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        return habilitacaoAdministrativaEm(contrato, dataReferencia, conhecidoAte, Set.of());
    }

    /** Reconstrói a base de uma retificação sem aplicar a versão explicitamente substituída. */
    public HabilitacaoAdministrativaContrato habilitacaoAdministrativaEm(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados) {
        return habilitacaoAdministrativaEm(contrato,dataReferencia,conhecidoAte,eventosIgnorados,null);
    }
    public HabilitacaoAdministrativaContrato habilitacaoAdministrativaEm(Contrato contrato,LocalDate dataReferencia,
            Instant conhecidoAte,Set<Long> eventosIgnorados,LeituraHistoricaContratos entradas) {
        if (contrato == null) return HabilitacaoAdministrativaContrato.INATIVO;
        if (contrato.getId() == null || contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO
                && contrato.getPublicadoEm() == null)
            return contrato.getHabilitacaoAdministrativa();
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        Set<Long> ignorados = eventosIgnorados == null ? Set.of() : eventosIgnorados;
        List<EventoCicloVidaContrato> conhecidos = eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecimento, entradas)
                .stream().filter(e -> !ignorados.contains(e.getId())).toList();
        HabilitacaoAdministrativaContrato inicial = fotografiaOriginalConhecida(contrato, conhecimento, entradas)
                .map(VersaoContratoNegocial::getHabilitacaoAdministrativa).orElse(null);
        // Ausência da fotografia original não comprova o estado anterior. Um ato
        // datado define somente o período a partir de seu alcance, sem retrocarga.
        HabilitacaoAdministrativaContrato resultado = inicial;
        for (EventoCicloVidaContrato e : conhecidos) {
            if (e.getDataEfetiva().isAfter(data)) continue;
            if (e.getTipoEvento() == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                    || e.getTipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_ANULACAO) {
                resultado = HabilitacaoAdministrativaContrato.INATIVO;
            } else if (e.getTipoEvento() == TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA) {
                resultado = HabilitacaoAdministrativaContrato.ATIVO;
            }
        }
        return resultado;
    }

    /** Estado efetivo na data consultada, inclusive atos de alcance futuro ainda não materializados. */
    public EstadoNegocialContrato estadoNegocialEm(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        return estadoNegocialEm(contrato,dataReferencia,conhecidoAte,null);
    }
    public EstadoNegocialContrato estadoNegocialEm(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte,
                                                  LeituraHistoricaContratos entradas) {
        if (contrato == null) return EstadoNegocialContrato.RASCUNHO;
        if (contrato.getId() == null) return contrato.getEstadoNegocial();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        EstadoNegocialContrato estado = estadoInicialConhecido(contrato, conhecimento);
        for (EventoCicloVidaContrato e : eventosValidosOrdenados(contrato.getId(), dataReferencia, conhecimento, entradas)) {
            switch (e.getTipoEvento()) {
                case ANULACAO -> estado = EstadoNegocialContrato.ANULADO;
                case CANCELAMENTO_CONTRATO_FUTURO -> estado = EstadoNegocialContrato.CANCELADO;
                case PUBLICACAO -> {
                    if (estado != EstadoNegocialContrato.ANULADO && estado != EstadoNegocialContrato.CANCELADO)
                        estado = EstadoNegocialContrato.PUBLICADO;
                }
                default -> { }
            }
        }
        return estado;
    }

    private EstadoNegocialContrato estadoInicialConhecido(Contrato contrato, Instant conhecidoAte) {
        if (contrato.getPublicadoEm() == null) {
            return EstadoNegocialContrato.RASCUNHO;
        }
        return contrato.getPublicadoEm().isAfter(conhecidoAte)
                ? EstadoNegocialContrato.RASCUNHO
                : EstadoNegocialContrato.PUBLICADO;
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
        if (inicio == null || fim == null) return null;
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
                    if (fonteId == null) continue;
                    // RETIFICA/SUBSTITUI preservam a superação da versão antiga
                    // mesmo quando a versão nova é posteriormente cancelada.
                    // A projeção deve seguir a mesma semântica de resolverIdsAtivos.
                    if (r.getTipoRelacao() == TipoRelacaoPrecedenciaNegocial.CANCELA && !ativos.contains(fonteId)) continue;
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
        Map<Long, Set<Long>> saidas = grafoPrecedencia(grupo, relacoes);
        Map<Long, Integer> entrada = new HashMap<>();
        porId.keySet().forEach(id -> entrada.put(id, 0));
        saidas.values().forEach(destinos -> destinos.forEach(id -> entrada.compute(id, (k, v) -> v + 1)));

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
        validarAmbiguidadeMesmaChave(ordenados, saidas);
        return ordenados;
    }

    private Map<Long, Set<Long>> grafoPrecedencia(List<EventoCicloVidaContrato> grupo,
                                                 List<RelacaoPrecedenciaEventoContrato> relacoes) {
        Map<Long, Set<Long>> saidas = new HashMap<>();
        Map<String, Long> versaoAtivaPorFato = new HashMap<>();
        for (EventoCicloVidaContrato evento : grupo) {
            if (evento.getId() == null) continue;
            saidas.put(evento.getId(), new LinkedHashSet<>());
            if (evento.getFatoLogicoId() != null) versaoAtivaPorFato.put(evento.getFatoLogicoId(), evento.getId());
        }
        Function<EventoCicloVidaContrato, Long> identidadeAtiva = evento -> {
            if (evento == null) return null;
            return saidas.containsKey(evento.getId()) ? evento.getId() : versaoAtivaPorFato.get(evento.getFatoLogicoId());
        };
        for (RelacaoPrecedenciaEventoContrato relacao : relacoes == null
                ? List.<RelacaoPrecedenciaEventoContrato>of() : relacoes) {
            if (relacao.getTipoRelacao() != TipoRelacaoPrecedenciaNegocial.SUCEDE) continue;
            Long fonte = identidadeAtiva.apply(relacao.getEventoFonte());
            Long alvo = identidadeAtiva.apply(relacao.getEventoAlvo());
            if (fonte != null && alvo != null && !fonte.equals(alvo)) saidas.get(alvo).add(fonte);
        }
        // Números distintos constituem sequência formal; igualdade ou ausência não desempata conflito.
        for (EventoCicloVidaContrato antes : grupo) {
            if (antes.getId() == null || antes.getOrdemDocumental() == null) continue;
            for (EventoCicloVidaContrato depois : grupo) {
                if (depois.getId() != null && depois.getOrdemDocumental() != null
                        && antes.getOrdemDocumental() < depois.getOrdemDocumental())
                    saidas.get(antes.getId()).add(depois.getId());
            }
        }
        return saidas;
    }

    private boolean alcanca(Map<Long, Set<Long>> grafo, Long origem, Long destino) {
        if (origem == null || destino == null) return false;
        Set<Long> visitados = new HashSet<>();
        Deque<Long> proximos = new ArrayDeque<>();
        proximos.add(origem);
        while (!proximos.isEmpty()) {
            Long atual = proximos.removeFirst();
            if (!visitados.add(atual)) continue;
            if (atual.equals(destino)) return true;
            proximos.addAll(grafo.getOrDefault(atual, Set.of()));
        }
        return false;
    }

    private void validarAmbiguidadeMesmaChave(
            List<EventoCicloVidaContrato> ordenados,
            Map<Long, Set<Long>> grafo
    ) {
        Map<String, List<EventoCicloVidaContrato>> porChave = ordenados.stream()
                .collect(Collectors.groupingBy(EventoCicloVidaContrato::getChaveNegocial));
        for (Map.Entry<String, List<EventoCicloVidaContrato>> entry : porChave.entrySet()) {
            if (entry.getValue().size() < 2 || !chaveSingular(entry.getKey())) continue;
            for (int i = 1; i < entry.getValue().size(); i++) {
                Long anterior = entry.getValue().get(i - 1).getId();
                Long atual = entry.getValue().get(i).getId();
                if (!alcanca(grafo, anterior, atual)) {
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
            case CORRECAO_CNPJ -> "CNPJ_EMPRESA";
            case TRANSFERENCIA_GESTAO_LOCAL -> "GESTAO_LOCAL";
            case CANCELAMENTO_TRANSFERENCIA_GESTAO -> "CANCELAMENTO_TRANSFERENCIA_GESTAO";
            case ADITAMENTO -> "ADITAMENTO";
            case CANCELAMENTO_ADITAMENTO -> "CANCELAMENTO_ADITAMENTO";
            case REMANEJAMENTO -> "REMANEJAMENTO";
            case CANCELAMENTO_REMANEJAMENTO -> "CANCELAMENTO_REMANEJAMENTO";
            case PUBLICACAO, CANCELAMENTO_CONTRATO_FUTURO, ANULACAO, CORRECAO_ANULACAO -> "ESTADO_NEGOCIAL";
        };
    }

    private boolean chaveSingular(String chave) {
        return Set.of("HABILITACAO_ADMINISTRATIVA", "VIGENCIA", "IDENTIFICACAO", "CNPJ_EMPRESA", "GESTAO_LOCAL", "ESTADO_NEGOCIAL")
                .contains(chave);
    }

    private Long id(EventoCicloVidaContrato e) { return e == null ? null : e.getId(); }

    public EventoCicloVidaContratoDTO paraEventoDTO(EventoCicloVidaContrato e) {
        return paraEventoDTO(e, e.getSituacaoFato());
    }

    private EventoCicloVidaContratoDTO paraEventoDTO(EventoCicloVidaContrato e, SituacaoFatoNegocial situacaoNoCorte) {
        return new EventoCicloVidaContratoDTO(
                e.getId(), e.getTipoEvento(), e.getFatoLogicoId(), e.getChaveNegocial(),
                e.getVersaoNegocial(), e.getOrdemDocumental(), situacaoNoCorte,
                e.getDataEfetiva(), e.getDataAto(), e.getRegistradoEm(),
                e.getUsuarioResponsavelId(), e.getUsuarioResponsavelUsername(),
                e.getMotivo(), e.getReferenciaSei(), e.getValorAnterior(), e.getValorNovo(),
                paraConferenciaDocumentalDTO(e)
        );
    }

    public static ConferenciaDocumentalAtoDTO paraConferenciaDocumentalDTO(EventoCicloVidaContrato evento) {
        if (evento == null || evento.getConferenciaDocumental() == null) return null;
        var c = evento.getConferenciaDocumental();
        var uso = c.getUsoPermissaoExcepcional();
        return new ConferenciaDocumentalAtoDTO(c.getVersaoDeclaracao(), c.getDeclaracao(), c.getVersaoIntencao(),
                c.getIntencaoCanonica(), c.getHashIntencao(), c.getAutorizacaoAdministrativaAto(),
                uso == null ? null : uso.getId(), uso == null ? null : uso.getPermissao().getId());
    }

    public RelacaoPrecedenciaEventoContratoDTO paraRelacaoDTO(RelacaoPrecedenciaEventoContrato r) {
        return new RelacaoPrecedenciaEventoContratoDTO(
                r.getId(), id(r.getEventoFonte()), id(r.getEventoAlvo()), r.getTipoRelacao(),
                r.getRegistradoEm(), r.getUsuarioResponsavelId(), r.getUsuarioResponsavelUsername(), r.getMotivo()
        );
    }

    private List<EventoCicloVidaContrato> eventosDoContrato(Long contratoId,LeituraHistoricaContratos entradas) {
        return entradas!=null&&entradas.contemContrato(contratoId)?entradas.eventos(contratoId)
                :eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId);
    }

    private List<VersaoContratoNegocial> versoesConhecidas(Long contratoId,Instant conhecido,LeituraHistoricaContratos entradas) {
        return entradas!=null&&entradas.contemContrato(contratoId)?entradas.versoesConhecidas(contratoId,conhecido)
                :versaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(contratoId,conhecido);
    }

    public VersaoContratoNegocialDTO paraVersaoDTO(VersaoContratoNegocial v) {
        return new VersaoContratoNegocialDTO(
                v.getId(), v.getNumeroVersao(), v.getEventoOrigem() != null ? v.getEventoOrigem().getId() : null,
                v.getDataEfetiva(), v.getRegistradoEm(), v.getNumeroContrato(), v.getAnoContrato(), v.getDescricao(),
                v.getTipoContratacaoId(), v.getTipoContratacaoDescricao(), v.getFornecedorId(), v.getCnpjEmpresa(),
                v.getRazaoSocialEmpresa(), v.getNomeFantasiaEmpresa(), v.getDataInicio(), v.getDataFim(),
                v.getEfetivoPrevisto(), v.getGestaoLocalId(), v.getGestaoLocalNome(),
                v.getMatriculaVinculoObrigatoria(), v.getMatriculaTipoContratacaoObrigatoria(),
                v.getMatriculaVinculoObrigatoriaEfetiva(), v.getDiaSemanaReduzido(), v.getHorasDiaReduzido(),
                v.getHabilitacaoAdministrativa()
        );
    }
}
