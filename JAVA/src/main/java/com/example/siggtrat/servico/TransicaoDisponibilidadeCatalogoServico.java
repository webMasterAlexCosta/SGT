package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.repositorio.EventoDisponibilidadeCatalogoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Atos documentados de disponibilidade, inclusive conhecidos depois da data efetiva. */
@Service
@Transactional(readOnly = true)
public class TransicaoDisponibilidadeCatalogoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(TransicaoDisponibilidadeCatalogoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final Comparator<EventoDisponibilidadeCatalogo> ORDEM = Comparator
            .comparing(EventoDisponibilidadeCatalogo::getDataEfetiva)
            .thenComparing(EventoDisponibilidadeCatalogo::getRegistradoEm)
            .thenComparing(EventoDisponibilidadeCatalogo::getId);
    private final EntityManager em;
    private final EventoDisponibilidadeCatalogoRepositorio eventos;
    private final AcessoOrganizacionalServico acesso;
    private final ProgramacaoFuturaCatalogoServico programacao;
    private final AssinaturaImpactoVinculoServico assinaturaImpacto;
    private final MotorTemporalContratoServico motorContrato;
    private final RegistroAuditoriaServico auditoria;

    public TransicaoDisponibilidadeCatalogoServico(EntityManager em,
            EventoDisponibilidadeCatalogoRepositorio eventos, AcessoOrganizacionalServico acesso,
            ProgramacaoFuturaCatalogoServico programacao, AssinaturaImpactoVinculoServico assinaturaImpacto,
            MotorTemporalContratoServico motorContrato, RegistroAuditoriaServico auditoria) {
        this.em = em; this.eventos = eventos; this.acesso = acesso; this.programacao = programacao;
        this.assinaturaImpacto = assinaturaImpacto; this.motorContrato = motorContrato; this.auditoria = auditoria;
    }

    public PreviaTransicaoCatalogoDTO previa(TipoCatalogoTemporal tipo, Long id,
                                            TransicaoDisponibilidadeCatalogoRequisicao req) {
        UsuarioAplicacao usuario = exigirExecutor();
        Object item = obterNoEscopo(tipo, id, usuario);
        validar(req);
        List<EventoDisponibilidadeCatalogo> trilha = eventos
                .findByTipoCatalogoAndCatalogoIdOrderByDataEfetivaAscRegistradoEmAscIdAsc(tipo, id);
        return preparar(tipo, id, item, req, usuario, trilha, contratosRelacionados(tipo, id));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransicaoCatalogoDTO registrar(TipoCatalogoTemporal tipo, Long id,
                                          TransicaoDisponibilidadeCatalogoRequisicao req) {
        UsuarioAplicacao usuario = exigirExecutor();
        Object item = obterNoEscopo(tipo, id, usuario);
        validar(req);
        // Mesma ordem dos comandos canônicos dependentes: contrato antes de catálogo.
        // O corte é recalculado sob locks; o token nunca substitui a validação transacional.
        List<Contrato> contratos = contratosRelacionados(tipo, id);
        for (Contrato contrato : contratos) em.refresh(contrato, LockModeType.PESSIMISTIC_WRITE);
        em.refresh(item, LockModeType.PESSIMISTIC_WRITE);
        acesso.revalidarAposEspera(usuario);
        exigirExecutor(usuario);
        obterNoEscopo(tipo, id, usuario);
        String intencao = intencao(tipo, id, req, usuario);
        EventoDisponibilidadeCatalogo existente = eventos
                .findByTipoCatalogoAndCatalogoIdAndChaveIdempotencia(tipo, id, req.chaveIdempotencia().trim()).orElse(null);
        if (existente != null) {
            if (!Objects.equals(existente.getHashIntencao(), intencao))
                throw conflito("A chave de idempotência já foi utilizada para outra intenção ou responsável.");
            return recibo(existente, true);
        }
        List<EventoDisponibilidadeCatalogo> trilha = eventos.buscarTrilhaParaAtualizacao(tipo, id).stream().sorted(ORDEM).toList();
        // READ_COMMITTED inclui referências confirmadas durante a espera.
        // Não adquirir novos locks de Contrato depois do catálogo: um publicador
        // pode manter justamente esse Contrato e aguardar o shared lock do item.
        // Novas escolhas não concluem enquanto este escritor mantém o catálogo X.
        // Os contratos já relacionados continuam protegidos pelos locks anteriores.
        contratos = contratosRelacionados(tipo, id);
        PreviaTransicaoCatalogoDTO previa = preparar(tipo, id, item, req, usuario, trilha, contratos);
        if (req.dataEfetiva().isBefore(RelogioSistema.hoje()) && (!Boolean.TRUE.equals(req.confirmarImpacto())
                || !Objects.equals(req.tokenPrevia(), previa.tokenPrevia())))
            throw conflito("A prévia do ato retroativo não foi confirmada ou ficou obsoleta. Refaça a análise de impacto.");

        Boolean atualAnterior = ativo(item);
        aplicarEstado(item, previa.ativoAtualAposRegistro());
        // Uma prova histórica nova também consome versão quando não muda a flag de hoje.
        em.lock(item, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        em.flush();
        EventoDisponibilidadeCatalogo evento = new EventoDisponibilidadeCatalogo();
        evento.setTipoCatalogo(tipo); evento.setCatalogoId(id); evento.setDataEfetiva(req.dataEfetiva());
        evento.setAtivoAnterior(previa.ativoAnterior()); evento.setAtivo(req.ativo());
        evento.setMotivo(req.motivo().trim()); evento.setEvidencia(req.evidencia().trim());
        evento.setAutorizacao(req.autorizacao().trim()); evento.setResponsavelId(usuario.getId());
        evento.setResponsavelUsername(usuario.getUsername()); evento.setHashIntencao(intencao);
        evento.setChaveIdempotencia(req.chaveIdempotencia().trim());
        evento.setChaveOrigem(trilha.isEmpty() ? tipo.name() + ":" + id : null);
        evento.setAtivoAtualAposRegistro(previa.ativoAtualAposRegistro()); evento.setVersaoCatalogoApos(versao(item));
        eventos.saveAndFlush(evento);
        auditoria.registrarAcao("CATALOGO_TRANSICAO_DOCUMENTADA", tipo.name(), id,
                "Ato de disponibilidade " + evento.getId(), RegistroAuditoriaServico.detalhes(
                        "eventoId", evento.getId(), "dataEfetiva", req.dataEfetiva(),
                        "ativoAnteriorNaData", previa.ativoAnterior(), "ativoNovoNaData", req.ativo(),
                        "ativoAtualAnterior", atualAnterior, "ativoAtualAposRegistro", previa.ativoAtualAposRegistro(),
                        "motivo", evento.getMotivo(), "evidencia", evento.getEvidencia(), "autorizacao", evento.getAutorizacao(),
                        "version", versao(item)));
        return recibo(evento, false);
    }

    private PreviaTransicaoCatalogoDTO preparar(TipoCatalogoTemporal tipo, Long id, Object item,
            TransicaoDisponibilidadeCatalogoRequisicao req, UsuarioAplicacao usuario,
            List<EventoDisponibilidadeCatalogo> trilha, List<Contrato> contratos) {
        if (req.versaoEsperada() != versao(item)) throw conflito("A versão do catálogo mudou. Recarregue o cadastro e a prévia.");
        LocalDate hoje = RelogioSistema.hoje();
        EventoDisponibilidadeCatalogo fonteAnterior = trilha.stream()
                .filter(e -> !e.getDataEfetiva().isAfter(req.dataEfetiva())).max(ORDEM).orElse(null);
        Boolean anterior = fonteAnterior == null ? req.dataEfetiva().equals(hoje) ? ativo(item) : null : fonteAnterior.getAtivo();
        EventoDisponibilidadeCatalogo posterior = trilha.stream()
                .filter(e -> e.getDataEfetiva().isAfter(req.dataEfetiva()) && !e.getDataEfetiva().isAfter(hoje))
                .max(ORDEM).orElse(null);
        boolean atualApos = posterior == null ? req.ativo() : Boolean.TRUE.equals(posterior.getAtivo());
        LocalDate proximaData = trilha.stream().map(EventoDisponibilidadeCatalogo::getDataEfetiva)
                .filter(d -> d.isAfter(req.dataEfetiva())).min(LocalDate::compareTo).orElse(null);
        LocalDate fimAfetado = proximaData == null ? null : proximaData.minusDays(1);
        if (!req.ativo()) validarFatosPosteriores(tipo, id, req.dataEfetiva(), fimAfetado, atualApos, contratos);

        List<Object> partes = new ArrayList<>();
        partes.add("disponibilidade-documentada-v1"); partes.add(intencao(tipo, id, req, usuario));
        partes.add(String.valueOf(versao(item))); partes.add(ativo(item));
        partes.add(hoje.toString()); partes.add(anterior); partes.add(String.valueOf(atualApos));
        for (EventoDisponibilidadeCatalogo e : trilha.stream().sorted(ORDEM).toList())
            partes.add(AssinaturaPreviaNegocial.partes(e.getId(), e.getDataEfetiva(), e.getRegistradoEm(), e.getAtivo(), e.getAtivoAnterior()));
        for (Contrato contrato : contratos) partes.addAll(assinaturaImpacto.partesPorContrato(contrato.getId(), req.dataEfetiva(), null));
        List<String> impactos = new ArrayList<>();
        impactos.add("Disponibilidade documentada a partir de " + req.dataEfetiva()
                + (fimAfetado == null ? "." : " até " + fimAfetado + "; preservados os atos posteriores."));
        if (anterior == null) impactos.add("Estado anterior sem fonte permanece desconhecido; o ato não certifica períodos anteriores.");
        impactos.add("Estado atual após o registro: " + (atualApos ? "ativo" : "inativo") + ".");
        impactos.add("Contratos com referências revalidadas: " + contratos.size() + ".");
        return new PreviaTransicaoCatalogoDTO(tipo, id, req.dataEfetiva(), anterior, req.ativo(), atualApos,
                versao(item), AssinaturaPreviaNegocial.sha256("sgt.previa.disponibilidade.v1", partes), List.copyOf(impactos));
    }

    private void validarFatosPosteriores(TipoCatalogoTemporal tipo, Long id, LocalDate desde, LocalDate ate,
                                         boolean ativoAtualApos, List<Contrato> contratos) {
        var operacional = tipoOperacional(tipo);
        if (operacional != null && !ativoAtualApos) programacao.exigirSemProgramacaoFutura(operacional, id);
        // A inativação administrativa de hoje conserva fatos já ocorridos. O
        // bloqueio adicional é da mudança retroativa que retiraria sua validade.
        if (!desde.isBefore(RelogioSistema.hoje())) return;
        if (operacional != null) {
            LocalDate limite = ate == null || ate.isAfter(RelogioSistema.hoje()) ? RelogioSistema.hoje() : ate;
            if (!desde.isAfter(limite) && programacao.possuiUsoProjetadoEntre(operacional, id, desde, limite))
                throw conflito("O ato tornaria incompatível uma composição ou Alocação posterior já documentada. "
                        + "Trate os fatos dependentes antes de alterar a disponibilidade histórica.");
        } else if (tipo == TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO) {
            long usos = em.createQuery("select count(e) from EventoVinculoContratual e "
                            + "where e.motivoDesligamentoIdFotografia=:id and e.dataEfetiva>=:desde "
                            + "and (:ate is null or e.dataEfetiva<=:ate)", Long.class)
                    .setParameter("id", id).setParameter("desde", desde).setParameter("ate", ate).getSingleResult();
            if (usos > 0) throw conflito("O ato invalidaria causa de término já documentada. Trate o vínculo antes de alterar a disponibilidade.");
        } else {
            Instant conhecimento = Instant.now();
            for (Contrato contrato : contratos) {
                SortedSet<LocalDate> datas = new TreeSet<>(); datas.add(desde);
                if (contrato.getDataInicio() != null) datas.add(contrato.getDataInicio());
                motorContrato.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecimento).stream()
                        .map(EventoCicloVidaContrato::getDataEfetiva).filter(Objects::nonNull).forEach(datas::add);
                while (!datas.isEmpty()) {
                    LocalDate data = datas.first(); datas.remove(data);
                    if (data.isBefore(desde) || ate != null && data.isAfter(ate)) continue;
                    var posicao = motorContrato.reconstruirPosicao(contrato, data, conhecimento);
                    if (posicao.vigenciaInicio() != null && posicao.vigenciaInicio().isAfter(data)) datas.add(posicao.vigenciaInicio());
                    if (posicao.estadoNegocial() == EstadoNegocialContrato.PUBLICADO
                            && (posicao.vigenciaInicio() == null || !data.isBefore(posicao.vigenciaInicio()))
                            && (posicao.vigenciaFim() == null || !data.isAfter(posicao.vigenciaFim()))
                            && posicao.versaoContratual() != null && Objects.equals(id, posicao.versaoContratual().tipoContratacaoId()))
                        throw conflito("O ato invalidaria Tipo de Contratação de fato posterior documentado. Trate o instrumento antes de continuar.");
                }
            }
        }
    }

    private List<Contrato> contratosRelacionados(TipoCatalogoTemporal tipo, Long id) {
        var operacional = tipoOperacional(tipo);
        if (operacional != null) return programacao.contratosRelacionados(operacional, id);
        if (tipo == TipoCatalogoTemporal.TIPO_CONTRATACAO)
            return em.createQuery("select c from Contrato c where c.tipoContratacao.id=:id or "
                            + "exists(select v.id from VersaoContratoNegocial v where v.contrato=c and v.tipoContratacaoId=:id) or "
                            + "exists(select a.id from AditamentoContratual a where a.contrato=c and a.novoTipoContratacao.id=:id) or "
                            + "exists(select t.id from TransferenciaGestaoContrato t where t.contrato=c and t.tipoContratacaoDestino.id=:id) order by c.id", Contrato.class)
                    .setParameter("id", id).getResultList();
        return em.createQuery("select c from Contrato c where "
                        + "exists(select v.id from VinculoContratual v where v.contrato=c and v.motivoDesligamento.id=:id) or "
                        + "exists(select e.id from EventoVinculoContratual e where e.vinculo.contrato=c and e.motivoDesligamentoIdFotografia=:id) order by c.id", Contrato.class)
                .setParameter("id", id).getResultList();
    }

    private String intencao(TipoCatalogoTemporal tipo, Long id, TransicaoDisponibilidadeCatalogoRequisicao req, UsuarioAplicacao usuario) {
        return AssinaturaPreviaNegocial.sha256("sgt.catalogo.intencao.v1", AssinaturaPreviaNegocial.partes(tipo, id, usuario.getId(),
                req.dataEfetiva(), req.ativo(), req.motivo().trim(), req.evidencia().trim(), req.autorizacao().trim()));
    }

    private TransicaoCatalogoDTO recibo(EventoDisponibilidadeCatalogo e, boolean reenvio) {
        return new TransicaoCatalogoDTO(e.getId(), e.getTipoCatalogo(), e.getCatalogoId(), e.getDataEfetiva(),
                Boolean.TRUE.equals(e.getAtivo()), e.getRegistradoEm(), e.getAtivoAtualAposRegistro(), e.getVersaoCatalogoApos(), reenvio);
    }

    private UsuarioAplicacao exigirExecutor() {
        UsuarioAplicacao u = acesso.usuarioAtual();
        exigirExecutor(u);
        return u;
    }

    private void exigirExecutor(UsuarioAplicacao u) {
        if (!acesso.ehGestaoLocal(u) || u.getGestaoLocal() == null || !Boolean.TRUE.equals(u.getGestaoLocal().getAtivo()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o Gestor Local ativo registra atos de disponibilidade.");
    }

    private Object obterNoEscopo(TipoCatalogoTemporal tipo, Long id, UsuarioAplicacao usuario) {
        if (tipo == null || id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catálogo inválido.");
        Class<?> classe = switch (tipo) {
            case LOTACAO -> Lotacao.class; case JORNADA -> Jornada.class; case CARGO -> Cargo.class;
            case TIPO_CONTRATACAO -> TipoContratacao.class; case MOTIVO_DESLIGAMENTO -> MotivoDesligamento.class;
        };
        Object item = em.find(classe, id);
        GestaoLocal gestao = item == null ? null : gestao(item);
        if (gestao == null || !Objects.equals(gestao.getId(), usuario.getGestaoLocal().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catálogo não encontrado na sua Gestão Local.");
        return item;
    }

    private void validar(TransicaoDisponibilidadeCatalogoRequisicao req) {
        if (req == null || req.dataEfetiva() == null || req.ativo() == null || req.versaoEsperada() == null
                || req.versaoEsperada() < 0 || !textoValido(req.motivo(), 500) || !textoValido(req.evidencia(), 1000)
                || !textoValido(req.autorizacao(), 1000) || !textoValido(req.chaveIdempotencia(), 80))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data, estado, versão, motivo, evidência, autorização e chave de idempotência são obrigatórios e devem respeitar seus limites.");
        if (req.dataEfetiva().isAfter(RelogioSistema.hoje()) || req.dataEfetiva().isBefore(LocalDate.of(1000, 1, 1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data efetiva deve ser válida e não posterior a hoje; este comando não agenda transições futuras.");
    }

    private boolean textoValido(String texto, int limite) { return texto != null && !texto.isBlank() && texto.trim().length() <= limite; }
    private ResponseStatusException conflito(String mensagem) { return new ResponseStatusException(HttpStatus.CONFLICT, mensagem); }
    private ProgramacaoFuturaCatalogoServico.TipoCatalogo tipoOperacional(TipoCatalogoTemporal tipo) {
        return switch (tipo) {
            case CARGO -> ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO;
            case LOTACAO -> ProgramacaoFuturaCatalogoServico.TipoCatalogo.LOTACAO;
            case JORNADA -> ProgramacaoFuturaCatalogoServico.TipoCatalogo.JORNADA;
            default -> null;
        };
    }
    private GestaoLocal gestao(Object item) {
        if (item instanceof Cargo e) return e.getGestaoLocal(); if (item instanceof Lotacao e) return e.getGestaoLocal();
        if (item instanceof Jornada e) return e.getGestaoLocal(); if (item instanceof TipoContratacao e) return e.getGestaoLocal();
        return ((MotivoDesligamento) item).getGestaoLocal();
    }
    private Boolean ativo(Object item) {
        if (item instanceof Cargo e) return e.getAtivo(); if (item instanceof Lotacao e) return e.getAtivo();
        if (item instanceof Jornada e) return e.getAtivo(); if (item instanceof TipoContratacao e) return e.getAtivo();
        return ((MotivoDesligamento) item).getAtivo();
    }
    private long versao(Object item) {
        if (item instanceof Cargo e) return e.getVersion(); if (item instanceof Lotacao e) return e.getVersion();
        if (item instanceof Jornada e) return e.getVersion(); if (item instanceof TipoContratacao e) return e.getVersion();
        return ((MotivoDesligamento) item).getVersion();
    }
    private void aplicarEstado(Object item, boolean ativo) {
        if (item instanceof Cargo e) e.setAtivo(ativo); else if (item instanceof Lotacao e) e.setAtivo(ativo);
        else if (item instanceof Jornada e) e.setAtivo(ativo); else if (item instanceof TipoContratacao e) e.setAtivo(ativo);
        else ((MotivoDesligamento) item).setAtivo(ativo);
    }
}
