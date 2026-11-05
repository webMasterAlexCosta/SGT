package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ComposicaoContratualDTO;
import com.example.siggtrat.dto.ComposicaoContratualLeituraDTO;
import com.example.siggtrat.dto.ExcedenteComposicaoDTO;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ComposicaoContratualServico {
    private final ComposicaoContratualRepositorio repositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private ComposicaoContratualTemporalServico composicaoTemporal;
    private VinculoContratualServico vinculoContratualServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;

    public ComposicaoContratualServico(
            ComposicaoContratualRepositorio repositorio,
            ContratoRepositorio contratoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            JornadaRepositorio jornadaRepositorio,
            CargoRepositorio cargoRepositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico) {
        this.repositorio = repositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarComposicaoTemporal(ComposicaoContratualTemporalServico composicaoTemporal) {
        this.composicaoTemporal = composicaoTemporal;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarVinculosCanonicos(VinculoContratualServico vinculoContratualServico) {
        this.vinculoContratualServico = vinculoContratualServico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    public List<ComposicaoContratualDTO> listar(Long contratoId, LocalDate data) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(contratoId);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        LocalDate ref = data == null ? RelogioSistema.hoje() : data;
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO && composicaoTemporal != null) {
            return composicaoTemporal.projetarDTO(contrato, ref, java.time.Instant.now());
        }
        List<ComposicaoContratual> itens = data == null
                ? repositorio.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contratoId)
                : repositorio.findValidasEm(contratoId, data);
        return itens.stream().map(c -> paraDTO(c, ref)).toList();
    }

    public List<ComposicaoContratualLeituraDTO> listarLeitura(Long contratoId,LocalDate data,Instant conhecidoAte) {
        UsuarioAplicacao user=acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato=obterContrato(contratoId);
        acessoOrganizacionalServico.exigirAcessoContrato(user,contrato);
        LocalDate ref=data==null?RelogioSistema.hoje():data;
        Instant conhecido=conhecidoAte==null?Instant.now():conhecidoAte;
        if(contrato.getEstadoNegocial()!=EstadoNegocialContrato.RASCUNHO&&composicaoTemporal!=null)
            return composicaoTemporal.projetarLeituraDTO(contrato,ref,conhecido);
        // Preparação de rascunho continua editável; seus valores não são prova publicada.
        return listar(contratoId,data).stream().map(d->new ComposicaoContratualLeituraDTO(d,null,null,ref,conhecido,
                ProvasCamposHistoricos.desconhecidos(List.of("lotacaoId","lotacaoNome","jornadaId","jornadaDescricao",
                        "cargoId","cargoNome","efetivoPrevisto","dataInicioVigencia","dataFimVigencia",
                        "permiteNovaAlocacao","lotacaoMunicipio","jornadaSemanal")))).toList();
    }

    public List<ExcedenteComposicaoDTO> listarExcedentesAtuais(Long contratoId) {
        return listarExcedentes(contratoId, null);
    }

    public List<ExcedenteComposicaoDTO> listarExcedentes(Long contratoId, LocalDate data) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(contratoId);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        LocalDate hoje = data == null ? RelogioSistema.hoje() : data;
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO && composicaoTemporal != null) {
            return composicaoTemporal.projetarEntidades(contrato, hoje, java.time.Instant.now()).stream().map(c -> {
                long vinculados = ocupacaoEm(contratoId, c.lotacao().getId(), c.jornada().getId(), c.cargo().getId(), hoje);
                long excedente = Math.max(0L, vinculados - c.efetivoPrevisto());
                return new ExcedenteComposicaoDTO(
                        c.composicaoBaseId(), contratoId, c.lotacao().getId(), c.lotacaoNome(),
                        c.jornada().getId(), c.jornadaDescricao(), c.cargo().getId(), c.cargoNome(),
                        c.efetivoPrevisto(), vinculados, excedente,
                        desdeQuandoExcedente(contrato, c.lotacao().getId(), c.jornada().getId(), c.cargo().getId(), hoje),
                        excedente > 0, justificativas(contratoId, c.lotacao().getId(), c.jornada().getId(), c.cargo().getId(), hoje)
                );
            }).toList();
        }
        return repositorio.findValidasEm(contratoId, hoje).stream().map(c -> {
            long vinculados = ocupacaoEm(contratoId, c.getLotacao().getId(), c.getJornada().getId(), c.getCargo().getId(), hoje);
            long excedente = Math.max(0L, vinculados - Optional.ofNullable(c.getEfetivoPrevisto()).orElse(0));
            return new ExcedenteComposicaoDTO(
                    c.getId(), contratoId, c.getLotacao().getId(), c.getLotacao().getNome(),
                    c.getJornada().getId(), c.getJornada().getDescricao(), c.getCargo().getId(), c.getCargo().getNome(),
                    c.getEfetivoPrevisto(), vinculados, excedente,
                    desdeQuandoExcedente(contrato, c.getLotacao().getId(), c.getJornada().getId(), c.getCargo().getId(), hoje),
                    excedente > 0, justificativas(contratoId, c.getLotacao().getId(), c.getJornada().getId(), c.getCargo().getId(), hoje)
            );
        }).toList();
    }

    private long ocupacaoEm(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, LocalDate data) {
        return vinculoContratualServico == null ? 0L
                : vinculoContratualServico.contarOcupacao(contratoId, lotacaoId, jornadaId, cargoId, data);
    }

    private List<com.example.siggtrat.dto.JustificativaExcedenteDTO> justificativas(
            Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, LocalDate data) {
        return vinculoContratualServico == null ? List.of()
                : vinculoContratualServico.justificativasExcedente(contratoId, lotacaoId, jornadaId, cargoId, data);
    }

    private LocalDate desdeQuandoExcedente(Contrato contrato, Long lotacaoId, Long jornadaId,
                                            Long cargoId, LocalDate ate) {
        if (vinculoContratualServico == null || composicaoTemporal == null) return null;
        java.util.SortedSet<LocalDate> mudancas = new java.util.TreeSet<>();
        if (contrato.getDataInicio() != null && !contrato.getDataInicio().isAfter(ate)) mudancas.add(contrato.getDataInicio());
        mudancas.addAll(composicaoTemporal.datasMudancaCapacidade(contrato, ate));
        mudancas.addAll(vinculoContratualServico.datasMudancaOcupacao(contrato.getId(), ate));
        LocalDate desde = null;
        for (LocalDate data : mudancas) {
            int capacidade = composicaoTemporal.projetarEntidades(contrato, data, java.time.Instant.now()).stream()
                    .filter(p -> Objects.equals(lotacaoId, p.lotacao().getId()))
                    .filter(p -> Objects.equals(jornadaId, p.jornada().getId()))
                    .filter(p -> Objects.equals(cargoId, p.cargo().getId()))
                    .mapToInt(p -> p.efetivoPrevisto()).findFirst().orElse(0);
            long ocupacao = ocupacaoEm(contrato.getId(), lotacaoId, jornadaId, cargoId, data);
            if (ocupacao > capacidade) {
                if (desde == null) desde = data;
            } else {
                desde = null;
            }
        }
        return desde;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ComposicaoContratualDTO criar(Long contratoId, ComposicaoContratualDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterRascunhoAcessivel(contratoId, user);
        ComposicaoContratual entidade = new ComposicaoContratual();
        entidade.setContrato(contrato);
        aplicar(dto, entidade, contrato, null);
        return paraDTO(repositorio.save(entidade), RelogioSistema.hoje());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ComposicaoContratualDTO atualizar(Long contratoId, Long id, ComposicaoContratualDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterRascunhoAcessivel(contratoId, user);
        ComposicaoContratual entidade = repositorio.findById(id)
                .filter(c -> c.getContrato() != null && Objects.equals(c.getContrato().getId(), contratoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Composição não encontrada."));
        aplicar(dto, entidade, contrato, id);
        return paraDTO(repositorio.save(entidade), RelogioSistema.hoje());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void excluir(Long contratoId, Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        obterRascunhoAcessivel(contratoId, user);
        ComposicaoContratual entidade = repositorio.findById(id)
                .filter(c -> c.getContrato() != null && Objects.equals(c.getContrato().getId(), contratoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Composição não encontrada."));
        repositorio.delete(entidade);
    }

    private void aplicar(ComposicaoContratualDTO dto, ComposicaoContratual entidade, Contrato contrato, Long idAtual) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da composição não informados.");
        if (dto.lotacaoId() == null || dto.jornadaId() == null || dto.cargoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe Lotação, Turno e Cargo da composição.");
        }
        if (dto.efetivoPrevisto() == null || dto.efetivoPrevisto() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Efetivo previsto da composição deve ser zero ou positivo.");
        }

        Lotacao lotacao = lotacaoRepositorio.findById(dto.lotacaoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação não encontrada."));
        Jornada jornada = jornadaRepositorio.findById(dto.jornadaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno não encontrado."));
        Cargo cargo = cargoRepositorio.findById(dto.cargoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo não encontrado."));

        if (disponibilidadeCatalogo != null) {
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, lotacao.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, jornada.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, cargo.getId());
        }
        Long gestaoContratoId = contrato.getGestaoLocal() == null ? null : contrato.getGestaoLocal().getId();
        if (lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null
                || !Objects.equals(gestaoContratoId, lotacao.getGestaoLocal().getId())
                || !Objects.equals(gestaoContratoId, jornada.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação e Turno devem pertencer à mesma Gestão Local do contrato.");
        }
        boolean jornadaPermitidaNaLotacao = lotacao.getTurnos() != null && lotacao.getTurnos().stream()
                .anyMatch(j -> j != null && Objects.equals(j.getId(), jornada.getId()));
        if (!jornadaPermitidaNaLotacao) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Turno informado não está vinculado à Lotação selecionada.");
        }
        if (cargo.getGestaoLocal() == null || !Objects.equals(gestaoContratoId, cargo.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cargo deve pertencer à mesma Gestão Local do contrato.");
        }
        LocalDate inicio = dto.dataInicioVigencia() != null ? dto.dataInicioVigencia() : contrato.getDataInicio();
        if (inicio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a data inicial da composição ou a vigência inicial do contrato.");
        }
        if (!catalogoDisponivelEm(TipoCatalogoTemporal.LOTACAO, lotacao.getId(), inicio, lotacao.getAtivo())
                || !catalogoDisponivelEm(TipoCatalogoTemporal.JORNADA, jornada.getId(), inicio, jornada.getAtivo())
                || !catalogoDisponivelEm(TipoCatalogoTemporal.CARGO, cargo.getId(), inicio, cargo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo devem estar disponíveis na data inicial da composição.");
        }
        LocalDate fim = dto.dataFimVigencia() != null ? dto.dataFimVigencia() : contrato.getDataFim();
        if (fim != null && fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período da composição inválido.");
        }
        if (contrato.getDataInicio() != null && inicio.isBefore(contrato.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição não pode iniciar antes do contrato.");
        }
        if (contrato.getDataFim() != null && (fim == null || fim.isAfter(contrato.getDataFim()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição não pode terminar após o contrato.");
        }

        List<ComposicaoContratual> existentes = repositorio.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contrato.getId());
        boolean duplicada = existentes.stream().anyMatch(c -> !Objects.equals(c.getId(), idAtual)
                && Objects.equals(c.getLotacao().getId(), lotacao.getId())
                && Objects.equals(c.getJornada().getId(), jornada.getId())
                && Objects.equals(c.getCargo().getId(), cargo.getId())
                && periodosSobrepostos(inicio, fim, c.getDataInicioVigencia(), c.getDataFimVigencia()));
        if (duplicada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe esta combinação de Lotação + Turno + Cargo no mesmo período.");
        }

        entidade.setLotacao(lotacao);
        entidade.setJornada(jornada);
        entidade.setCargo(cargo);
        entidade.fotografarCatalogosEmRascunho();
        entidade.setEfetivoPrevisto(dto.efetivoPrevisto());
        entidade.setDataInicioVigencia(inicio);
        entidade.setDataFimVigencia(fim);
    }

    private boolean periodosSobrepostos(LocalDate aIni, LocalDate aFim, LocalDate bIni, LocalDate bFim) {
        LocalDate aFinal = aFim == null ? LocalDate.MAX : aFim;
        LocalDate bFinal = bFim == null ? LocalDate.MAX : bFim;
        return !aFinal.isBefore(bIni) && !bFinal.isBefore(aIni);
    }

    private Contrato obterContrato(Long id) {
        return contratoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterRascunhoAcessivel(Long id, UsuarioAplicacao user) {
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Composição de contrato publicado só pode mudar por fato negocial versionado.");
        }
        return contrato;
    }

    private boolean catalogoDisponivelEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean ativo) {
        return disponibilidadeCatalogo == null ? !Boolean.FALSE.equals(ativo)
                : disponibilidadeCatalogo.ativoEm(tipo, id, data, ativo);
    }

    private ComposicaoContratualDTO paraDTO(ComposicaoContratual c, LocalDate dataRef) {
        return new ComposicaoContratualDTO(
                c.getId(),
                c.getContrato() != null ? c.getContrato().getId() : null,
                c.getLotacao() != null ? c.getLotacao().getId() : null,
                c.getLotacao() != null ? c.getLotacao().getNome() : null,
                c.getJornada() != null ? c.getJornada().getId() : null,
                c.getJornada() != null ? c.getJornada().getDescricao() : null,
                c.getCargo() != null ? c.getCargo().getId() : null,
                c.getCargo() != null ? c.getCargo().getNome() : null,
                c.getEfetivoPrevisto(),
                c.getDataInicioVigencia(),
                c.getDataFimVigencia(),
                c.permiteNovaAlocacaoEm(dataRef),
                c.getVersion()
        );
    }

    private long ocupacaoAtual(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId) {
        return vinculoContratualServico == null
                ? 0
                : vinculoContratualServico.contarOcupacao(
                        contratoId, lotacaoId, jornadaId, cargoId, RelogioSistema.hoje());
    }
}
