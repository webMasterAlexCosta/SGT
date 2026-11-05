package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.DisponibilidadeCatalogoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoDisponibilidadeCatalogoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Disponibilidade comprovada por data efetiva e pelo instante de conhecimento. */
@Service
public class DisponibilidadeCatalogoTemporalServico {
    private final EventoDisponibilidadeCatalogoRepositorio repositorio;
    @PersistenceContext private EntityManager em;
    private AcessoOrganizacionalServico acesso;

    public DisponibilidadeCatalogoTemporalServico(EventoDisponibilidadeCatalogoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Autowired
    public void configurarAcesso(AcessoOrganizacionalServico acesso) { this.acesso = acesso; }

    /** Origem prospectiva de um item novo; jamais certifica um período anterior à criação. */
    @Transactional
    public void registrar(TipoCatalogoTemporal tipo, Long catalogoId, boolean ativo,
                          String motivo, UsuarioAplicacao responsavel) {
        registrarTransicao(tipo, catalogoId, null, ativo, motivo, responsavel);
    }

    /** O estado anterior de uma transição não informa desde quando ele era válido. */
    @Transactional
    public void registrarTransicao(TipoCatalogoTemporal tipo, Long catalogoId, Boolean ativoAnterior, boolean ativo,
                                   String motivo, UsuarioAplicacao responsavel) {
        bloquearCatalogo(tipo, catalogoId);
        List<EventoDisponibilidadeCatalogo> trilha = repositorio.buscarTrilhaParaAtualizacao(tipo, catalogoId);
        validarEstadoAnterior(trilha, ativoAnterior);
        salvar(tipo, catalogoId, RelogioSistema.hoje(), ativoAnterior, ativo, motivo, responsavel,
                trilha.isEmpty() && ativoAnterior == null);
    }

    /**
     * Incorpora uma origem cuja data foi fornecida expressamente pela fonte.
     * O lock é do item, que já existe mesmo quando ainda não há nenhum evento.
     * Assim duas primeiras transições não conseguem fabricar origens concorrentes.
     */
    @Transactional
    public void registrarTransicaoComOrigem(TipoCatalogoTemporal tipo, Long catalogoId,
            boolean antes, boolean depois, LocalDate inicioConhecido, String motivo, UsuarioAplicacao responsavel) {
        if (inicioConhecido == null || inicioConhecido.isAfter(RelogioSistema.hoje()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A origem exige data conhecida até hoje.");
        bloquearCatalogo(tipo, catalogoId);
        List<EventoDisponibilidadeCatalogo> trilha = repositorio.buscarTrilhaParaAtualizacao(tipo, catalogoId);
        validarEstadoAnterior(trilha, antes);
        if (trilha.isEmpty()) salvar(tipo, catalogoId, inicioConhecido, null, antes,
                "Origem documental declarada: " + motivo, responsavel, true);
        salvar(tipo, catalogoId, RelogioSistema.hoje(), antes, depois, motivo, responsavel, false);
    }

    private void validarEstadoAnterior(List<EventoDisponibilidadeCatalogo> trilha, Boolean anterior) {
        EventoDisponibilidadeCatalogo atual = trilha.stream()
                .filter(e -> !e.getDataEfetiva().isAfter(RelogioSistema.hoje())).findFirst().orElse(null);
        if (atual != null && !Objects.equals(atual.getAtivo(), anterior))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A disponibilidade do catálogo mudou. Recarregue o estado antes de registrar a transição.");
    }

    private EventoDisponibilidadeCatalogo salvar(TipoCatalogoTemporal tipo, Long id, LocalDate data,
            Boolean anterior, boolean ativo, String motivo, UsuarioAplicacao responsavel, boolean origem) {
        EventoDisponibilidadeCatalogo evento = new EventoDisponibilidadeCatalogo();
        evento.setTipoCatalogo(tipo); evento.setCatalogoId(id); evento.setDataEfetiva(data);
        evento.setAtivo(ativo); evento.setAtivoAnterior(anterior);
        evento.setChaveOrigem(origem ? tipo.name() + ":" + id : null);
        evento.setMotivo(motivo == null || motivo.isBlank() ? "Alteração administrativa do catálogo" : motivo);
        evento.setResponsavelId(responsavel == null ? null : responsavel.getId());
        evento.setResponsavelUsername(responsavel == null ? null : responsavel.getUsername());
        return repositorio.save(evento);
    }

    @Transactional(readOnly = true)
    public boolean ativoEm(TipoCatalogoTemporal tipo, Long catalogoId, LocalDate dataEfetiva,
                           Boolean estadoAtualSemHistorico) {
        if (tipo == null || catalogoId == null || dataEfetiva == null) return false;
        boolean confirmacao = transacaoDeEscrita();
        if (confirmacao) {
            // A confirmação conserva o lock até o commit do fato dependente.
            // find/flag já carregados não são uma releitura após esperar outro escritor.
            bloquearParaUso(tipo, catalogoId);
            estadoAtualSemHistorico = ativo(obterCatalogo(tipo, catalogoId, null));
        }
        // Uma escolha futura nunca pode reutilizar item hoje inativo (CAT-008).
        if (dataEfetiva.isAfter(RelogioSistema.hoje()) && Boolean.FALSE.equals(estadoAtualSemHistorico)) return false;
        var conhecido = confirmacao ? repositorio
                .findTopByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
                        tipo, catalogoId, dataEfetiva, Instant.now()) : repositorio
                .findFirstByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
                        tipo, catalogoId, dataEfetiva, Instant.now());
        if (conhecido.isPresent()) return Boolean.TRUE.equals(conhecido.get().getAtivo());
        // A projeção atual é utilizável hoje, mas não prova a disponibilidade no passado.
        if (!dataEfetiva.isBefore(RelogioSistema.hoje()) && estadoAtualSemHistorico != null)
            return estadoAtualSemHistorico;
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Disponibilidade histórica não comprovada para o catálogo " + tipo + " " + catalogoId
                        + ". É necessária prova da disponibilidade na data informada.");
    }

    /**
     * Confirmações travam a escolha antes de ler estado, escopo e associações.
     * Consultas/prévias read-only não adquirem locks; a confirmação revalida.
     * O chamador mantém sua transação de escrita aberta até concluir o fato.
     */
    @Transactional(readOnly = true)
    public void bloquearParaUso(TipoCatalogoTemporal tipo, Long id) {
        if (!transacaoDeEscrita()) return;
        Object item = obterCatalogo(tipo, id, null);
        LockModeType lock = em.getLockMode(item);
        if (lock != LockModeType.PESSIMISTIC_READ && lock != LockModeType.PESSIMISTIC_WRITE
                && lock != LockModeType.PESSIMISTIC_FORCE_INCREMENT)
            em.refresh(item, LockModeType.PESSIMISTIC_READ);
    }

    /** Deve anteceder a leitura do estado anterior e das dependências administrativas. */
    @Transactional
    public void bloquearParaAlteracao(TipoCatalogoTemporal tipo, Long id) {
        Object item = obterCatalogo(tipo, id, null);
        LockModeType lock = em.getLockMode(item);
        if (lock != LockModeType.PESSIMISTIC_WRITE && lock != LockModeType.PESSIMISTIC_FORCE_INCREMENT)
            em.refresh(item, LockModeType.PESSIMISTIC_WRITE);
    }

    private boolean transacaoDeEscrita() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeCatalogoDTO consultar(TipoCatalogoTemporal tipo, Long id, LocalDate data, Instant conhecidoAte) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirConsultaContrato(usuario);
        Object item = obterCatalogo(tipo, id, null);
        GestaoLocal gestao = gestao(item);
        boolean permitido = acesso.ehGlobal(usuario) || acesso.ehPresidente(usuario)
                || gestao != null && acesso.podeAcessarGestaoLocal(usuario, gestao);
        if (permitido && acesso.ehOperacional(usuario)) {
            if (item instanceof Lotacao lotacao) permitido = acesso.podeAcessarLotacao(usuario, lotacao);
            if (item instanceof Jornada jornada) permitido = acesso.podeAcessarJornada(usuario, jornada);
        }
        if (!permitido) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catálogo não encontrado no seu escopo.");
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        var evento = repositorio
                .findFirstByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
                        tipo, id, data, conhecimento).orElse(null);
        return new DisponibilidadeCatalogoDTO(tipo, id, data, conhecimento,
                evento == null ? null : evento.getAtivo(), evento != null, evento == null ? null : evento.getId(),
                evento == null ? null : evento.getDataEfetiva(), evento == null ? null : evento.getRegistradoEm(),
                evento == null ? null : evento.getAtivoAnterior());
    }

    private void bloquearCatalogo(TipoCatalogoTemporal tipo, Long id) { obterCatalogo(tipo, id, LockModeType.PESSIMISTIC_WRITE); }

    private Object obterCatalogo(TipoCatalogoTemporal tipo, Long id, LockModeType lock) {
        if (tipo == null || id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catálogo inválido.");
        Class<?> classe = switch (tipo) {
            case LOTACAO -> Lotacao.class;
            case JORNADA -> Jornada.class;
            case CARGO -> Cargo.class;
            case TIPO_CONTRATACAO -> TipoContratacao.class;
            case MOTIVO_DESLIGAMENTO -> MotivoDesligamento.class;
        };
        Object item = lock == null ? em.find(classe, id) : em.find(classe, id, lock);
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catálogo não encontrado.");
        return item;
    }

    private GestaoLocal gestao(Object item) {
        if (item instanceof Lotacao e) return e.getGestaoLocal();
        if (item instanceof Jornada e) return e.getGestaoLocal();
        if (item instanceof Cargo e) return e.getGestaoLocal();
        if (item instanceof TipoContratacao e) return e.getGestaoLocal();
        if (item instanceof MotivoDesligamento e) return e.getGestaoLocal();
        return null;
    }

    private Boolean ativo(Object item) {
        if (item instanceof Lotacao e) return e.getAtivo();
        if (item instanceof Jornada e) return e.getAtivo();
        if (item instanceof Cargo e) return e.getAtivo();
        if (item instanceof TipoContratacao e) return e.getAtivo();
        if (item instanceof MotivoDesligamento e) return e.getAtivo();
        return null;
    }
}
