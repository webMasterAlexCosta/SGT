package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.regra.RegraDisponibilidadeCatalogo;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mantém a linha do tempo completa de Lotação + Turno + Cargo. O serviço
 * preserva as faixas anteriores, registra o evento correspondente e impede
 * lacunas/sobreposições dentro do Vínculo.
 */
@Service
@Transactional(readOnly = true)
public class AlocacaoVinculoServico {
    public record CapacidadeInclusao(int efetivoPrevisto, long ocupacaoAtual,
                                     long ocupacaoAposInclusao, long excedenteAposInclusao) {
        public boolean excede() { return excedenteAposInclusao > 0; }
    }
    private record FotoAlocacao(Long id, Long composicaoId, Long lotacaoId, String lotacaoNome,
                                Long jornadaId, String jornadaDescricao, Long cargoId, String cargoNome,
                                LocalDate inicio, LocalDate fim, EstadoNegocialAlocacao estado) {}

    private record CombinacaoResolvida(ComposicaoContratual composicao,
                                       Lotacao lotacao, Jornada jornada, Cargo cargo, int efetivo) {}

    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final EventoAlocacaoVinculoRepositorio eventoRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalAlocacaoServico motor;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;

    public AlocacaoVinculoServico(AlocacaoVinculoRepositorio alocacaoRepositorio,
                                  EventoAlocacaoVinculoRepositorio eventoRepositorio,
                                  VinculoContratualRepositorio vinculoRepositorio,
                                  ContratoRepositorio contratoRepositorio,
                                  ComposicaoContratualRepositorio composicaoRepositorio,
                                  ComposicaoContratualTemporalServico composicaoTemporal,
                                  AcessoOrganizacionalServico acesso,
                                  MotorTemporalAlocacaoServico motor) {
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.composicaoTemporal = composicaoTemporal;
        this.acesso = acesso;
        this.motor = motor;
    }

    @Autowired(required = false)
    public void configurarRevalidacaoFrequencia(RevalidacaoFrequenciaTemporalServico servico) {
        this.revalidacaoFrequenciaTemporalServico = servico;
    }

    @Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    public List<AlocacaoVinculoDTO> listar(Long vinculoId, YearMonth mes) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        return alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculoId).stream()
                .filter(a -> sobrepoeMes(a, mes)).map(this::paraDTO).toList();
    }

    public List<EventoAlocacaoVinculoDTO> listarEventos(Long vinculoId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        return eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId).stream().map(this::paraDTO).toList();
    }

    public PosicaoAlocacaoTemporalDTO posicao(Long vinculoId, LocalDate data, Instant conhecidoAte) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        return motor.reconstruir(vinculo, data, conhecidoAte);
    }

    /**
     * A inativação administrativa preserva as faixas. Antes da reativação, as
     * Alocações que voltariam a operar precisam continuar representadas por uma
     * composição temporal válida na data efetiva.
     */
    public void validarRetomadaOperacional(Contrato contrato, LocalDate dataEfetiva) {
        if (contrato == null || contrato.getId() == null || dataEfetiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato e data efetiva são obrigatórios para revalidar a Alocação.");
        }
        alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contrato.getId()).stream()
                .filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                .filter(a -> a.getVinculo().getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO)
                .filter(a -> !dataEfetiva.isBefore(a.getDataInicio()))
                .filter(a -> a.getDataFim() == null || !dataEfetiva.isAfter(a.getDataFim()))
                .forEach(a -> validarCombinacaoDireta(a.getVinculo(), dataEfetiva, a));
    }

    /** Uma lista filtrável de combinações para a seleção encadeada no frontend. */
    public List<OpcaoAlocacaoTemporalDTO> opcoes(Long contratoId, LocalDate data, Long lotacaoId, Long jornadaId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Contrato contrato = contratoRepositorio.findById(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(usuario, contrato);
        LocalDate referencia = data == null ? RelogioSistema.hoje() : data;
        return composicaoTemporal.projetarEntidades(contrato, referencia, Instant.now()).stream()
                .filter(p -> p.efetivoPrevisto() > 0)
                .filter(p -> lotacaoId == null || Objects.equals(lotacaoId, p.lotacao().getId()))
                .filter(p -> jornadaId == null || Objects.equals(jornadaId, p.jornada().getId()))
                .map(p -> {
                    long ocupacao = alocacaoRepositorio.countOcupacao(contratoId, p.lotacao().getId(),
                            p.jornada().getId(), p.cargo().getId(), referencia);
                    long apos = ocupacao + 1;
                    return new OpcaoAlocacaoTemporalDTO(p.composicaoBaseId(), p.lotacao().getId(), p.lotacaoNome(),
                            p.jornada().getId(), p.jornadaDescricao(), p.cargo().getId(), p.cargoNome(),
                            p.efetivoPrevisto(), ocupacao, apos, Math.max(0L, apos - p.efetivoPrevisto()),
                            apos > p.efetivoPrevisto(), true);
                }).toList();
    }

    /** Capacidade e ocupação são reconstruídas na data efetiva, não na data do comando. */
    public CapacidadeInclusao capacidadeInclusao(Contrato contrato, ComposicaoContratual composicao,
                                                  LocalDate dataEfetiva) {
        if (contrato == null || composicao == null || dataEfetiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato, composição e data efetiva são obrigatórios para avaliar capacidade.");
        }
        int efetivo = composicaoTemporal.projetarEntidades(contrato, dataEfetiva, Instant.now()).stream()
                .filter(p -> Objects.equals(p.lotacao().getId(), composicao.getLotacao().getId()))
                .filter(p -> Objects.equals(p.jornada().getId(), composicao.getJornada().getId()))
                .filter(p -> Objects.equals(p.cargo().getId(), composicao.getCargo().getId()))
                .mapToInt(p -> p.efetivoPrevisto())
                .findFirst().orElse(0);
        long ocupacao = alocacaoRepositorio.countOcupacao(contrato.getId(), composicao.getLotacao().getId(),
                composicao.getJornada().getId(), composicao.getCargo().getId(), dataEfetiva);
        long apos = ocupacao + 1;
        return new CapacidadeInclusao(efetivo, ocupacao, apos, Math.max(0L, apos - efetivo));
    }

    /** Criação integrada da primeira faixa, chamada pela transação de Pessoa + Vínculo. */
    @Transactional
    public AlocacaoVinculo criarInicial(VinculoContratual vinculo, ComposicaoContratual composicao,
                                        UsuarioAplicacao usuario) {
        if (vinculo == null || composicao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo e composição inicial são obrigatórios.");
        }
        if (Optional.ofNullable(composicao.getEfetivoPrevisto()).orElse(0) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição inicial possui efetivo previsto zero e não aceita nova Alocação.");
        }
        validarCatalogos(new CombinacaoResolvida(composicao, composicao.getLotacao(), composicao.getJornada(),
                composicao.getCargo(), composicao.getEfetivoPrevisto()), vinculo.getDataInicio());
        AlocacaoVinculo nova = novaFaixa(vinculo, composicao, composicao.getLotacao(), composicao.getJornada(),
                composicao.getCargo(), vinculo.getDataInicio(), vinculo.getDataFim(), usuario);
        nova = alocacaoRepositorio.save(nova);
        registrarEvento(vinculo, nova, TipoEventoAlocacaoVinculo.CRIACAO, TipoRelacaoEventoAlocacao.ORIGEM,
                vinculo.getDataInicio(), usuario, "Cadastro integrado de Pessoa, Vínculo e Alocação inicial", null, null,
                null, foto(nova), null);
        garantirCobertura(vinculo);
        return nova;
    }

    public PreviaImpactoAlocacaoDTO previaAlteracao(Long vinculoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        validarRequisicao(req);
        validarDataNoVinculo(vinculo, req.dataEfetiva());
        CombinacaoResolvida alvo = resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(),
                req.lotacaoId(), req.jornadaId(), req.cargoId());
        validarCatalogos(alvo, req.dataEfetiva());

        AlocacaoVinculo atual = faixaEfetiva(vinculoId, req.dataEfetiva()).orElse(null);
        List<String> incompatibilidades = new ArrayList<>();
        if (atual == null) incompatibilidades.add("Não existe alocação efetiva a retificar na data proposta.");
        if (atual != null && mesmaCombinacao(atual, alvo)) {
            incompatibilidades.add("A combinação proposta já é a alocação efetiva na data informada.");
        }
        List<String> impactos = new ArrayList<>(List.of(
                "faixa temporal de Lotação + Turno + Cargo",
                "eventos de alocação posteriores preservados",
                "frequências históricas a revalidar",
                "relatórios e indicadores temporais a reprocessar"));
        if (atual != null && atual.getDataFim() != null && req.dataEfetiva().equals(atual.getDataFim())) {
            impactos.add("mudança no último dia da faixa; desligamento na mesma data exige precedência inequívoca");
        }
        return new PreviaImpactoAlocacaoDTO(tokenPrevia(vinculo, req.dataEfetiva(), alvo), vinculoId,
                atual == null ? null : atual.getId(), req.dataEfetiva(), atual == null ? null : atual.getDataFim(),
                List.copyOf(impactos), List.copyOf(incompatibilidades));
    }

    @Transactional
    public AlocacaoVinculoDTO alterar(Long vinculoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        exigirContratoOperacional(vinculo.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(),
                req == null ? null : req.autorizacao());
        PreviaImpactoAlocacaoDTO previa = previaAlteracao(vinculoId, req);
        confirmarPrevia(req, previa);
        exigirPrecedenciaUltimoDia(vinculo, req);
        if (!previa.incompatibilidades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", previa.incompatibilidades()));
        }
        AlocacaoVinculo nova = substituir(vinculo, req.dataEfetiva(),
                resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(), req.lotacaoId(), req.jornadaId(), req.cargoId()),
                usuario, justificativaComPrecedencia(vinculo, req), req.evidencia(), req.autorizacao(), null,
                TipoEventoAlocacaoVinculo.ALTERACAO);
        garantirCobertura(vinculo);
        revalidarFrequencias(vinculo, req.dataEfetiva(), "Alteração retroativa de Alocação");
        return paraDTO(nova);
    }

    /** A mesma data substitui formalmente a faixa anterior, sem criar dois fatos efetivos no dia. */
    @Transactional
    public AlocacaoVinculoDTO retificar(Long vinculoId, Long alocacaoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        exigirContratoOperacional(vinculo.getContrato());
        AlocacaoVinculo alvo = alocacaoRepositorio.findById(alocacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocação não encontrada."));
        if (!Objects.equals(alvo.getVinculo().getId(), vinculoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alocação pertence a outro vínculo.");
        }
        if (req == null || !Objects.equals(req.dataEfetiva(), alvo.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A retificação deve usar o primeiro dia da alocação informada.");
        }
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        PreviaImpactoAlocacaoDTO previa = previaAlteracao(vinculoId, req);
        confirmarPrevia(req, previa);
        exigirPrecedenciaUltimoDia(vinculo, req);
        if (!previa.incompatibilidades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", previa.incompatibilidades()));
        }
        CombinacaoResolvida combinacao = resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(),
                req.lotacaoId(), req.jornadaId(), req.cargoId());
        if (mesmaCombinacao(alvo, combinacao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A retificação não altera a combinação existente.");
        }
        AlocacaoVinculo nova = substituir(vinculo, req.dataEfetiva(), combinacao, usuario,
                justificativaComPrecedencia(vinculo, req), req.evidencia(), req.autorizacao(), null,
                TipoEventoAlocacaoVinculo.RETIFICACAO);
        garantirCobertura(vinculo);
        revalidarFrequencias(vinculo, req.dataEfetiva(), "Retificação retroativa de Alocação");
        return paraDTO(nova);
    }

    @Transactional
    public AlocacaoVinculoDTO cancelarProgramacao(Long vinculoId, Long alocacaoId, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(),
                req == null ? null : req.autorizacao());
        AlocacaoVinculo alvo = alocacaoRepositorio.findById(alocacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocação não encontrada."));
        if (!Objects.equals(alvo.getVinculo().getId(), vinculoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alocação pertence a outro vínculo.");
        }
        if (alvo.getEstadoNegocial() != EstadoNegocialAlocacao.REGISTRADA || !RelogioSistema.hoje().isBefore(alvo.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente alocação futura ainda registrada pode ter a programação cancelada.");
        }
        FotoAlocacao anterior = foto(alvo);
        AlocacaoVinculo predecessora = anteriorAtiva(vinculoId, alvo.getDataInicio()).orElse(null);
        FotoAlocacao fotoPredecessora = predecessora == null ? null : foto(predecessora);
        alvo.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA);
        alocacaoRepositorio.save(alvo);
        if (predecessora != null) {
            predecessora.setDataFim(anterior.fim());
            alocacaoRepositorio.save(predecessora);
        }
        registrarEvento(vinculo, alvo, TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO,
                TipoRelacaoEventoAlocacao.CANCELA, alvo.getDataInicio(), usuario, req.justificativa(), req.evidencia(),
                req.autorizacao(), anterior, foto(alvo), null);
        if (predecessora != null) {
            registrarEvento(vinculo, predecessora, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                    TipoRelacaoEventoAlocacao.AJUSTA, alvo.getDataInicio(), usuario,
                    "Restauração da faixa anterior após cancelamento de programação", req.evidencia(), req.autorizacao(),
                    fotoPredecessora, foto(predecessora), null);
        }
        garantirCobertura(vinculo);
        return paraDTO(alvo);
    }

    /**
     * Ajusta a cobertura após retificação de início/fim ou desligamento. Nenhuma
     * faixa é apagada; o ajuste é auditado e valida a composição no novo limite.
     */
    @Transactional
    public void sincronizarLimitesVinculo(VinculoContratual vinculo, UsuarioAplicacao usuario, String justificativa) {
        if (vinculo == null || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return;
        LocalDate inicio = inicioCobertura(vinculo);
        LocalDate fim = fimCobertura(vinculo);
        for (AlocacaoVinculo faixa : new ArrayList<>(ativas(vinculo.getId()))) {
            if (faixa.getDataFim() != null && faixa.getDataFim().isBefore(inicio)) {
                FotoAlocacao antes = foto(faixa); faixa.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA);
                alocacaoRepositorio.save(faixa);
                registrarEvento(vinculo, faixa, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                        TipoRelacaoEventoAlocacao.AJUSTA, inicio, usuario, justificativa,
                        "Faixa anterior ao novo início do vínculo", "Fluxo de vínculo", antes, foto(faixa), null);
            }
        }
        List<AlocacaoVinculo> ativas = ativas(vinculo.getId());
        if (ativas.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Vínculo operacional não possui Alocação para sincronizar.");
        AlocacaoVinculo primeira = ativas.get(0);
        if (!primeira.getDataInicio().equals(inicio)) {
            validarCombinacaoDireta(vinculo, inicio, primeira);
            FotoAlocacao antes = foto(primeira); primeira.setDataInicio(inicio); alocacaoRepositorio.save(primeira);
            registrarEvento(vinculo, primeira, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                    TipoRelacaoEventoAlocacao.AJUSTA, inicio, usuario, justificativa, "Revalidação da linha de Alocação", "Fluxo de vínculo",
                    antes, foto(primeira), null);
        }
        if (fim != null) {
            for (AlocacaoVinculo faixa : new ArrayList<>(ativas(vinculo.getId()))) {
                if (faixa.getDataInicio().isAfter(fim)) {
                    FotoAlocacao antes = foto(faixa); faixa.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA);
                    alocacaoRepositorio.save(faixa);
                    registrarEvento(vinculo, faixa, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                            TipoRelacaoEventoAlocacao.AJUSTA, fim, usuario, justificativa,
                            "Faixa posterior ao fim do vínculo", "Fluxo de vínculo", antes, foto(faixa), null);
                }
            }
            AlocacaoVinculo ultimaVigente = faixaEfetiva(vinculo.getId(), fim).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Fim do vínculo ficou sem Alocação válida."));
            if (ultimaVigente.getDataFim() == null || ultimaVigente.getDataFim().isAfter(fim)) {
                FotoAlocacao antes = foto(ultimaVigente); ultimaVigente.setDataFim(fim); alocacaoRepositorio.save(ultimaVigente);
                registrarEvento(vinculo, ultimaVigente, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                        TipoRelacaoEventoAlocacao.AJUSTA, fim, usuario, justificativa,
                        "Ajuste ao último dia do vínculo", "Fluxo de vínculo", antes, foto(ultimaVigente), null);
            }
        } else {
            List<AlocacaoVinculo> faixasAtivas = ativas(vinculo.getId());
            AlocacaoVinculo ultima = faixasAtivas.get(faixasAtivas.size() - 1);
            if (ultima.getDataFim() != null) {
                validarCombinacaoDireta(vinculo, ultima.getDataFim().plusDays(1), ultima);
                FotoAlocacao antes = foto(ultima); ultima.setDataFim(null); alocacaoRepositorio.save(ultima);
                registrarEvento(vinculo, ultima, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                        TipoRelacaoEventoAlocacao.AJUSTA, ultima.getDataInicio(), usuario, justificativa,
                        "Reabertura temporal do vínculo", "Fluxo de vínculo", antes, foto(ultima), null);
            }
        }
        garantirCobertura(vinculo);
    }

    /** Aplica a nova responsabilidade aos vínculos existentes sem criar outro vínculo ou desligamento. */
    @Transactional
    public void aplicarTransferenciaGestao(TransferenciaGestaoContrato transferencia, UsuarioAplicacao usuario) {
        if (transferencia == null || transferencia.getContrato() == null) return;
        Map<String, MapeamentoTransferenciaGestao> mapas = transferencia.getMapeamentos().stream().collect(Collectors.toMap(
                m -> chave(m.getLotacaoOrigem().getId(), m.getJornadaOrigem().getId(), m.getCargoOrigem().getId()),
                m -> m, (a, b) -> a, LinkedHashMap::new));
        Map<Long, VinculoContratual> vinculos = new LinkedHashMap<>();
        for (AlocacaoVinculo faixa : alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(
                transferencia.getContrato().getId())) {
            VinculoContratual vinculo = faixa.getVinculo();
            vinculos.put(vinculo.getId(), vinculo);
        }
        for (VinculoContratual vinculo : vinculos.values()) {
            if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            for (AlocacaoVinculo faixa : new ArrayList<>(ativas(vinculo.getId()))) {
                if (faixa.getDataFim() != null && faixa.getDataFim().isBefore(transferencia.getDataEfetiva())) continue;
                LocalDate efetiva = faixa.getDataInicio().isAfter(transferencia.getDataEfetiva())
                        ? faixa.getDataInicio() : transferencia.getDataEfetiva();
                MapeamentoTransferenciaGestao mapa = mapas.get(chave(faixa.getLotacao().getId(), faixa.getJornada().getId(), faixa.getCargo().getId()));
                if (mapa == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Transferência sem mapeamento para uma Alocação posterior do vínculo " + vinculo.getId() + ".");
                }
                CombinacaoResolvida destino = new CombinacaoResolvida(mapa.getComposicaoOrigem(), mapa.getLotacaoDestino(),
                        mapa.getJornadaDestino(), mapa.getCargoDestino(), 1);
                substituir(vinculo, efetiva, destino, usuario, transferencia.getMotivo(), transferencia.getReferenciaSei(),
                        transferencia.getAutorizacaoAdministrativa(), transferencia, TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO);
            }
            garantirCobertura(vinculo);
            revalidarFrequencias(vinculo, transferencia.getDataEfetiva(),
                    "Transferência excepcional de Gestão alterou a Alocação histórica");
        }
    }

    private void revalidarFrequencias(VinculoContratual vinculo, LocalDate desde, String motivo) {
        if (revalidacaoFrequenciaTemporalServico != null && vinculo != null && vinculo.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorVinculo(vinculo.getId(), desde, motivo);
        }
    }

    private void exigirPrecedenciaUltimoDia(VinculoContratual vinculo, AlteracaoAlocacaoVinculoRequisicao req) {
        if (vinculo.getDataFim() != null && vinculo.getDataFim().equals(req.dataEfetiva())
                && !Boolean.TRUE.equals(req.confirmarPrecedenciaUltimoDia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Alteração de Alocação no último dia exige confirmarPrecedenciaUltimoDia=true: "
                            + "a Alocação vale no último dia e o desligamento produz efeito após esse dia.");
        }
    }

    private void exigirContratoOperacional(Contrato contrato) {
        if (contrato == null || contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || !Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo não aceita alteração operacional de Alocação.");
        }
    }

    private String justificativaComPrecedencia(VinculoContratual vinculo,
                                                AlteracaoAlocacaoVinculoRequisicao req) {
        if (vinculo.getDataFim() != null && vinculo.getDataFim().equals(req.dataEfetiva())) {
            return "[PRECEDENCIA: ALOCACAO_VALE_NO_ULTIMO_DIA; DESLIGAMENTO_APOS_O_DIA] "
                    + req.justificativa();
        }
        return req.justificativa();
    }

    private AlocacaoVinculo substituir(VinculoContratual vinculo, LocalDate dataEfetiva, CombinacaoResolvida destino,
                                       UsuarioAplicacao usuario, String justificativa, String evidencia, String autorizacao,
                                       TransferenciaGestaoContrato transferencia, TipoEventoAlocacaoVinculo tipoSolicitado) {
        validarDataNoVinculo(vinculo, dataEfetiva);
        validarCatalogos(destino, dataEfetiva);
        AlocacaoVinculo atual = faixaEfetiva(vinculo.getId(), dataEfetiva).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Não existe Alocação efetiva para a data informada."));
        if (mesmaCombinacao(atual, destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A nova Alocação é igual à faixa efetiva atual.");
        }
        FotoAlocacao antes = foto(atual);
        LocalDate fimNova = atual.getDataFim();
        TipoEventoAlocacaoVinculo tipo = tipoSolicitado == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                ? TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                : dataEfetiva.equals(atual.getDataInicio()) ? TipoEventoAlocacaoVinculo.RETIFICACAO : tipoSolicitado;
        TipoRelacaoEventoAlocacao relacao = tipo == TipoEventoAlocacaoVinculo.RETIFICACAO
                ? TipoRelacaoEventoAlocacao.RETIFICA : TipoRelacaoEventoAlocacao.SUBSTITUI;
        if (dataEfetiva.equals(atual.getDataInicio())) {
            atual.setEstadoNegocial(EstadoNegocialAlocacao.SUBSTITUIDA);
        } else {
            atual.setDataFim(dataEfetiva.minusDays(1));
        }
        alocacaoRepositorio.save(atual);
        FotoAlocacao depoisDaFaixaAnterior = foto(atual);
        AlocacaoVinculo nova = novaFaixa(vinculo, destino.composicao(), destino.lotacao(), destino.jornada(), destino.cargo(),
                dataEfetiva, fimNova, usuario);
        nova = alocacaoRepositorio.save(nova);
        registrarEvento(vinculo, nova, tipo, relacao, dataEfetiva, usuario, justificativa, evidencia, autorizacao,
                antes, foto(nova), transferencia);
        // O evento de substituição preserva o estado anterior. Este segundo fato
        // registra o resultado da faixa sucedida, permitindo reconstrução
        // bitemporal sem inferir silenciosamente o seu novo fim/estado.
        registrarEvento(vinculo, atual, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                TipoRelacaoEventoAlocacao.AJUSTA, dataEfetiva, usuario,
                "Ajuste da faixa anterior após " + tipo.name().toLowerCase(Locale.ROOT), evidencia, autorizacao,
                antes, depoisDaFaixaAnterior, transferencia);
        return nova;
    }

    private AlocacaoVinculo novaFaixa(VinculoContratual vinculo, ComposicaoContratual composicao,
                                      Lotacao lotacao, Jornada jornada, Cargo cargo, LocalDate inicio, LocalDate fim,
                                      UsuarioAplicacao usuario) {
        AlocacaoVinculo nova = new AlocacaoVinculo();
        nova.setVinculo(vinculo); nova.setComposicao(composicao); nova.setLotacao(lotacao); nova.setJornada(jornada); nova.setCargo(cargo);
        nova.setDataInicio(inicio); nova.setDataFim(fim); nova.setCriadoPorId(usuario == null ? null : usuario.getId());
        nova.setCriadoPorUsername(usuario == null ? null : usuario.getUsername()); nova.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA);
        return nova;
    }

    private Optional<AlocacaoVinculo> faixaEfetiva(Long vinculoId, LocalDate data) {
        return ativas(vinculoId).stream().filter(a -> !data.isBefore(a.getDataInicio())
                && (a.getDataFim() == null || !data.isAfter(a.getDataFim()))).findFirst();
    }

    private Optional<AlocacaoVinculo> anteriorAtiva(Long vinculoId, LocalDate inicio) {
        return ativas(vinculoId).stream().filter(a -> a.getDataInicio().isBefore(inicio)
                && (a.getDataFim() == null || a.getDataFim().isBefore(inicio))).max(Comparator.comparing(AlocacaoVinculo::getDataFim,
                        Comparator.nullsLast(LocalDate::compareTo)));
    }

    private List<AlocacaoVinculo> ativas(Long vinculoId) {
        return alocacaoRepositorio.findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(vinculoId,
                EstadoNegocialAlocacao.REGISTRADA);
    }

    private CombinacaoResolvida resolverCombinacao(VinculoContratual vinculo, LocalDate data, Long composicaoId,
                                                    Long lotacaoId, Long jornadaId, Long cargoId) {
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado = composicaoTemporal
                .projetarEntidades(vinculo.getContrato(), data, Instant.now());
        Optional<ComposicaoContratualTemporalServico.ComposicaoProjetada> encontrada = composicaoTemporal
                .localizar(estado, composicaoId, lotacaoId, jornadaId, cargoId);
        var p = encontrada.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A combinação Contrato → Lotação → Turno → Cargo não existe na composição temporal."));
        if (lotacaoId != null && !Objects.equals(lotacaoId, p.lotacao().getId())
                || jornadaId != null && !Objects.equals(jornadaId, p.jornada().getId())
                || cargoId != null && !Objects.equals(cargoId, p.cargo().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O recorte informado não corresponde à composição temporal selecionada.");
        }
        if (p.efetivoPrevisto() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição possui efetivo previsto zero e não está disponível para nova Alocação.");
        }
        ComposicaoContratual base = p.composicaoBaseId() == null ? null : composicaoRepositorio.findById(p.composicaoBaseId()).orElse(null);
        return new CombinacaoResolvida(base, p.lotacao(), p.jornada(), p.cargo(), p.efetivoPrevisto());
    }

    private void validarCombinacaoDireta(VinculoContratual vinculo, LocalDate data, AlocacaoVinculo faixa) {
        resolverCombinacao(vinculo, data, faixa.getComposicao() == null ? null : faixa.getComposicao().getId(),
                faixa.getLotacao().getId(), faixa.getJornada().getId(), faixa.getCargo().getId());
    }

    private void validarCatalogos(CombinacaoResolvida combinacao, LocalDate dataEfetiva) {
        if (combinacao.lotacao() == null || combinacao.jornada() == null || combinacao.cargo() == null
                || combinacao.lotacao().getGestaoLocal() == null || combinacao.jornada().getGestaoLocal() == null
                || combinacao.cargo().getGestaoLocal() == null
                || !Objects.equals(combinacao.lotacao().getGestaoLocal().getId(), combinacao.jornada().getGestaoLocal().getId())
                || !Objects.equals(combinacao.lotacao().getGestaoLocal().getId(), combinacao.cargo().getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo devem pertencer à mesma Gestão Local.");
        }
        boolean turnoDaLotacao = combinacao.lotacao().getTurnos().stream()
                .anyMatch(j -> Objects.equals(j.getId(), combinacao.jornada().getId()));
        if (!turnoDaLotacao) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Turno informado não está vinculado à Lotação selecionada.");
        }
        if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, combinacao.lotacao().getId(), dataEfetiva, combinacao.lotacao().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, combinacao.jornada().getId(), dataEfetiva, combinacao.jornada().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, combinacao.cargo().getId(), dataEfetiva, combinacao.cargo().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Catálogo inativo não pode ser utilizado em nova Alocação atual ou futura.");
        }
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : RegraDisponibilidadeCatalogo.permiteUso(data, atual);
    }

    private void validarDataNoVinculo(VinculoContratual vinculo, LocalDate data) {
        if (data == null || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocação exige Vínculo registrado e data efetiva.");
        }
        if (data.isBefore(inicioCobertura(vinculo)) || fimCobertura(vinculo) != null && data.isAfter(fimCobertura(vinculo))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data da Alocação deve estar contida no período operacional do Vínculo e do Contrato.");
        }
    }

    private LocalDate inicioCobertura(VinculoContratual vinculo) {
        LocalDate inicioContrato = vinculo.getContrato().getDataInicio();
        return inicioContrato != null && inicioContrato.isAfter(vinculo.getDataInicio()) ? inicioContrato : vinculo.getDataInicio();
    }

    private LocalDate fimCobertura(VinculoContratual vinculo) {
        LocalDate a = vinculo.getDataFim(); LocalDate b = vinculo.getContrato().getDataFim();
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private void garantirCobertura(VinculoContratual vinculo) {
        if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return;
        List<AlocacaoVinculo> faixas = ativas(vinculo.getId());
        if (faixas.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Todo Vínculo operacional deve possuir exatamente uma Alocação por dia.");
        LocalDate inicio = inicioCobertura(vinculo); LocalDate fim = fimCobertura(vinculo);
        if (fim != null && faixas.stream().anyMatch(f -> f.getDataInicio().isAfter(fim))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existe Alocação ativa posterior ao fim do Vínculo.");
        }
        LocalDate proximo = inicio;
        for (AlocacaoVinculo faixa : faixas) {
            if (faixa.getDataInicio() == null || faixa.getDataInicio().isAfter(proximo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Foi detectada lacuna na linha temporal de Alocação do vínculo.");
            }
            if (faixa.getDataInicio().isBefore(proximo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Foi detectada sobreposição de Alocações no mesmo dia.");
            }
            if (faixa.getDataFim() == null) {
                if (fim != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Alocação aberta excede o último dia do Vínculo.");
                return;
            }
            proximo = faixa.getDataFim().plusDays(1);
            if (fim != null && proximo.isAfter(fim)) {
                if (!proximo.equals(fim.plusDays(1))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocação excede o período do Vínculo.");
                }
                return;
            }
        }
        if (fim == null || !proximo.equals(fim.plusDays(1))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A linha temporal não cobre todos os dias do Vínculo operacional.");
        }
    }

    private void validarRequisicao(AlteracaoAlocacaoVinculoRequisicao req) {
        if (req == null || req.dataEfetiva() == null || (req.composicaoId() == null
                && (req.lotacaoId() == null || req.jornadaId() == null || req.cargoId() == null))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data efetiva e uma combinação válida de Alocação são obrigatórias.");
        }
    }

    private void confirmarPrevia(AlteracaoAlocacaoVinculoRequisicao req, PreviaImpactoAlocacaoDTO previa) {
        if (!Boolean.TRUE.equals(req.confirmarImpacto()) || req.tokenPrevia() == null || !req.tokenPrevia().equals(previa.token())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prévia de impacto da Alocação não foi confirmada ou expirou.");
        }
    }

    private String tokenPrevia(VinculoContratual vinculo, LocalDate data, CombinacaoResolvida alvo) {
        String hash = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(vinculo.getId())
                .map(EventoAlocacaoVinculo::getHashEvento).orElse("SEM_EVENTO_ALOCACAO");
        List<String> partes = new ArrayList<>(List.of(String.valueOf(vinculo.getId()), String.valueOf(vinculo.getVersion()),
                AssinaturaPreviaNegocial.valor(data), AssinaturaPreviaNegocial.valor(alvo.composicao() == null ? null : alvo.composicao().getId()),
                AssinaturaPreviaNegocial.valor(alvo.lotacao().getId()), AssinaturaPreviaNegocial.valor(alvo.jornada().getId()),
                AssinaturaPreviaNegocial.valor(alvo.cargo().getId()), hash));
        ativas(vinculo.getId()).forEach(a -> partes.add(a.getId() + ":" + a.getVersion() + ":" + a.getDataInicio() + ":" + a.getDataFim()));
        return AssinaturaPreviaNegocial.sha256(partes);
    }

    private EventoAlocacaoVinculo registrarEvento(VinculoContratual vinculo, AlocacaoVinculo nova,
                                                    TipoEventoAlocacaoVinculo tipo, TipoRelacaoEventoAlocacao relacao,
                                                    LocalDate dataEfetiva, UsuarioAplicacao usuario,
                                                    String justificativa, String evidencia, String autorizacao,
                                                    FotoAlocacao anterior, FotoAlocacao novaFoto,
                                                    TransferenciaGestaoContrato transferencia) {
        EventoAlocacaoVinculo precedente = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(vinculo.getId()).orElse(null);
        EventoAlocacaoVinculo evento = new EventoAlocacaoVinculo();
        evento.setVinculo(vinculo); evento.setAlocacaoNova(nova); evento.setTipoEvento(tipo); evento.setRelacaoPrecedencia(
                precedente == null ? TipoRelacaoEventoAlocacao.ORIGEM : relacao); evento.setEventoPrecedente(precedente);
        evento.setDataEfetiva(dataEfetiva); evento.setResponsavelId(usuario == null ? null : usuario.getId());
        evento.setResponsavelUsername(usuario == null ? null : usuario.getUsername()); evento.setJustificativa(justificativa);
        evento.setEvidencia(evidencia); evento.setAutorizacao(autorizacao); aplicarAnterior(evento, anterior); aplicarNovo(evento, novaFoto);
        if (transferencia != null) {
            evento.setTransferenciaGestaoId(transferencia.getId());
            evento.setGestaoOrigemIdFotografia(transferencia.getGestaoOrigem().getId());
            evento.setGestaoOrigemNomeFotografia(transferencia.getGestaoOrigem().getNome());
            evento.setGestaoDestinoIdFotografia(transferencia.getGestaoDestino().getId());
            evento.setGestaoDestinoNomeFotografia(transferencia.getGestaoDestino().getNome());
        }
        String hashAnterior = precedente == null ? null : precedente.getHashEvento();
        evento.setHashAnterior(hashAnterior);
        evento.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|",
                AssinaturaPreviaNegocial.valor(hashAnterior), tipo.name(), AssinaturaPreviaNegocial.valor(dataEfetiva),
                fotoTexto(anterior), fotoTexto(novaFoto), AssinaturaPreviaNegocial.valor(usuario == null ? null : usuario.getId()),
                AssinaturaPreviaNegocial.valor(justificativa), AssinaturaPreviaNegocial.valor(transferencia == null ? null : transferencia.getId()))));
        return eventoRepositorio.save(evento);
    }

    private void aplicarAnterior(EventoAlocacaoVinculo e, FotoAlocacao f) {
        if (f == null) return;
        e.setAlocacaoAnteriorId(f.id()); e.setComposicaoAnteriorId(f.composicaoId()); e.setLotacaoAnteriorId(f.lotacaoId());
        e.setLotacaoAnteriorNome(f.lotacaoNome()); e.setJornadaAnteriorId(f.jornadaId());
        e.setJornadaAnteriorDescricao(f.jornadaDescricao()); e.setCargoAnteriorId(f.cargoId());
        e.setCargoAnteriorNome(f.cargoNome()); e.setInicioAnterior(f.inicio());
        e.setFimAnterior(f.fim()); e.setEstadoAnterior(f.estado());
    }

    private void aplicarNovo(EventoAlocacaoVinculo e, FotoAlocacao f) {
        if (f == null) return;
        e.setComposicaoNovaId(f.composicaoId()); e.setLotacaoNovaId(f.lotacaoId()); e.setLotacaoNovaNome(f.lotacaoNome());
        e.setJornadaNovaId(f.jornadaId()); e.setJornadaNovaDescricao(f.jornadaDescricao()); e.setCargoNovoId(f.cargoId());
        e.setCargoNovoNome(f.cargoNome()); e.setInicioNovo(f.inicio()); e.setFimNovo(f.fim()); e.setEstadoNovo(f.estado());
    }

    private FotoAlocacao foto(AlocacaoVinculo a) {
        return a == null ? null : new FotoAlocacao(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), a.getLotacao().getNome(), a.getJornada().getId(), a.getJornada().getDescricao(),
                a.getCargo().getId(), a.getCargo().getNome(), a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial());
    }

    private String fotoTexto(FotoAlocacao f) {
        return f == null ? "" : String.join(":", AssinaturaPreviaNegocial.valor(f.id()), AssinaturaPreviaNegocial.valor(f.composicaoId()),
                AssinaturaPreviaNegocial.valor(f.lotacaoId()), AssinaturaPreviaNegocial.valor(f.jornadaId()),
                AssinaturaPreviaNegocial.valor(f.cargoId()), AssinaturaPreviaNegocial.valor(f.inicio()),
                AssinaturaPreviaNegocial.valor(f.fim()), AssinaturaPreviaNegocial.valor(f.estado()));
    }

    private boolean mesmaCombinacao(AlocacaoVinculo a, CombinacaoResolvida b) {
        return Objects.equals(a.getLotacao().getId(), b.lotacao().getId()) && Objects.equals(a.getJornada().getId(), b.jornada().getId())
                && Objects.equals(a.getCargo().getId(), b.cargo().getId());
    }

    private boolean sobrepoeMes(AlocacaoVinculo a, YearMonth mes) {
        if (mes == null) return true;
        LocalDate inicio = mes.atDay(1); LocalDate fim = mes.atEndOfMonth();
        return !a.getDataInicio().isAfter(fim) && (a.getDataFim() == null || !a.getDataFim().isBefore(inicio));
    }

    private String chave(Long lotacao, Long jornada, Long cargo) { return lotacao + ":" + jornada + ":" + cargo; }

    private VinculoContratual obterVinculo(Long id) {
        return vinculoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
    }

    private void exigirJustificativaCompleta(String justificativa, String evidencia, String autorizacao) {
        if (vazio(justificativa) || vazio(evidencia) || vazio(autorizacao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justificativa, evidência e autorização são obrigatórias.");
        }
    }

    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }

    private AlocacaoVinculoDTO paraDTO(AlocacaoVinculo a) {
        return new AlocacaoVinculoDTO(a.getId(), a.getVinculo().getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), a.getLotacao().getNome(), a.getJornada().getId(), a.getJornada().getDescricao(),
                a.getCargo().getId(), a.getCargo().getNome(), a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial(),
                a.getCriadoEm(), a.getCriadoPorId(), a.getCriadoPorUsername(), a.getVersion());
    }

    private EventoAlocacaoVinculoDTO paraDTO(EventoAlocacaoVinculo e) {
        return new EventoAlocacaoVinculoDTO(e.getId(), e.getVinculo().getId(), e.getAlocacaoNova() == null ? null : e.getAlocacaoNova().getId(),
                e.getTipoEvento(), e.getRelacaoPrecedencia(), e.getEventoPrecedente() == null ? null : e.getEventoPrecedente().getId(),
                e.getDataEfetiva(), e.getRegistradoEm(), e.getResponsavelId(), e.getResponsavelUsername(), e.getJustificativa(),
                e.getEvidencia(), e.getAutorizacao(), e.getAlocacaoAnteriorId(), e.getInicioAnterior(), e.getFimAnterior(),
                e.getEstadoAnterior(), e.getComposicaoAnteriorId(), e.getLotacaoAnteriorId(), e.getLotacaoAnteriorNome(),
                e.getJornadaAnteriorId(), e.getJornadaAnteriorDescricao(), e.getCargoAnteriorId(), e.getCargoAnteriorNome(),
                e.getInicioNovo(), e.getFimNovo(), e.getEstadoNovo(), e.getComposicaoNovaId(),
                e.getLotacaoNovaId(), e.getLotacaoNovaNome(), e.getJornadaNovaId(), e.getJornadaNovaDescricao(),
                e.getCargoNovoId(), e.getCargoNovoNome(), e.getTransferenciaGestaoId(),
                e.getGestaoOrigemIdFotografia(), e.getGestaoOrigemNomeFotografia(), e.getGestaoDestinoIdFotografia(),
                e.getGestaoDestinoNomeFotografia(), e.getHashAnterior(), e.getHashEvento());
    }
}
