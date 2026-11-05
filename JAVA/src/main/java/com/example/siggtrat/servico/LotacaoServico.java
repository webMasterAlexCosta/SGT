package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.JornadaResumoDTO;
import com.example.siggtrat.dto.LotacaoDTO;
import com.example.siggtrat.dto.LotacaoOpcaoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Única responsabilidade de negócio para lotações. O controller expõe HTTP;
 * este serviço resolve escopo, valida vínculos e persiste a agregação
 * lotação/turnos.
 */
@Service
@Transactional(readOnly = true)
public class LotacaoServico {

    private final LotacaoRepositorio repositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final EscopoAcessoUsuarioServico scopeServico;
    private final ProgramacaoFuturaCatalogoServico programacaoFutura;
    private final RegistroAuditoriaServico auditoria;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeTemporal;

    public LotacaoServico(LotacaoRepositorio repositorio,
                          JornadaRepositorio jornadaRepositorio,
                          GestaoLocalRepositorio gestaoLocalRepositorio,
                          EmpregadoRepositorio empregadoRepositorio,
                          UsuarioAplicacaoRepositorio appUserRepositorio,
                          AcessoOrganizacionalServico acessoOrganizacionalServico,
                          EscopoAcessoUsuarioServico scopeServico,
                          ProgramacaoFuturaCatalogoServico programacaoFutura,
                          RegistroAuditoriaServico auditoria) {
        this.repositorio = repositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.appUserRepositorio = appUserRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.scopeServico = scopeServico;
        this.programacaoFutura = programacaoFutura;
        this.auditoria = auditoria;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeTemporal(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeTemporal = servico;
    }

    public Page<LotacaoDTO> listar(Pageable pageable) {
        List<Lotacao> lotacoes = listarAcessiveis(acessoOrganizacionalServico.usuarioAtual());
        int inicio = (int) Math.min(pageable.getOffset(), lotacoes.size());
        int fim = Math.min(inicio + pageable.getPageSize(), lotacoes.size());
        List<LotacaoDTO> conteudo = lotacoes.subList(inicio, fim).stream().map(this::paraDTO).toList();
        return new PageImpl<>(conteudo, pageable, lotacoes.size());
    }

    @Cacheable(cacheNames = NomesCache.LOTACOES_OPCOES, key = "@redisCacheKey.usuarioAtual()", sync = true)
    public List<LotacaoOpcaoDTO> listarOpcoes() {
        List<Lotacao> lotacoes = listarAcessiveis(acessoOrganizacionalServico.usuarioAtual());
        if (lotacoes.isEmpty()) return List.of();
        List<Long> ids = lotacoes.stream().map(Lotacao::getId).toList();
        return repositorio.buscarTodosComTurnosPorIds(ids).stream()
                .map(lotacao -> new LotacaoOpcaoDTO(
                        lotacao.getId(),
                        lotacao.getNome(),
                        lotacao.getTurnos().stream()
                                .filter(turno -> turno != null && turno.getId() != null)
                                .map(turno -> new JornadaResumoDTO(turno.getId(), turno.getDescricao()))
                                .toList()))
                .toList();
    }

    public Optional<LotacaoDTO> buscar(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        return repositorio.findById(id)
                .filter(lotacao -> acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao))
                .map(this::paraDTO);
    }

    @Transactional
    public LotacaoDTO criar(LotacaoDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        GestaoLocal gestaoLocal = resolverGestaoLocal(usuario, dto == null ? null : dto.gestaoLocalId());
        validarDados(dto, gestaoLocal, null);

        Lotacao lotacao = new Lotacao();
        aplicarDados(lotacao, dto, gestaoLocal);
        Lotacao salva = repositorio.save(lotacao);
        registrarDisponibilidade(salva, null, salva.getAtivo(), "Criação da Lotação", usuario);
        // Sincronizar também os turnos associados antes de reter o executor.
        repositorio.flush();
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        auditar("LOTACAO_CRIADA", salva, null, salva.getAtivo());
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public LotacaoDTO atualizar(Long id, LotacaoDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        Lotacao lotacao = buscarEntidadeAcessivel(usuario, id);
        Long gestaoLocalId = dto != null && dto.gestaoLocalId() != null
                ? dto.gestaoLocalId()
                : lotacao.getGestaoLocal() != null ? lotacao.getGestaoLocal().getId() : null;
        GestaoLocal gestaoLocal = resolverGestaoLocal(usuario, gestaoLocalId);

        boolean mudandoGestaoLocal = lotacao.getGestaoLocal() != null && gestaoLocal != null
                && !Objects.equals(lotacao.getGestaoLocal().getId(), gestaoLocal.getId());
        if (mudandoGestaoLocal && possuiVinculos(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível transferir a lotação para outra Gestão Local enquanto existirem empregados ou usuários vinculados.");
        }

        validarDados(dto, gestaoLocal, id);
        Boolean ativoAnterior = lotacao.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior) && Boolean.FALSE.equals(dto.ativo())) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.LOTACAO, lotacao.getId());
        }
        aplicarDados(lotacao, dto, gestaoLocal);
        Lotacao salva = repositorio.save(lotacao);
        if (!Objects.equals(ativoAnterior, salva.getAtivo()))
            registrarDisponibilidade(salva, ativoAnterior, salva.getAtivo(), "Alteração administrativa da Lotação", usuario);
        auditar(acaoAtualizacao(ativoAnterior, salva.getAtivo()), salva, ativoAnterior, salva.getAtivo());
        // Expor a versão já sincronizada, sem antecipar o commit da operação.
        repositorio.flush();
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void excluir(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestao(usuario);
        Lotacao lotacao = buscarEntidadeAcessivel(usuario, id);
        // A remoção operacional é inativação. O registro permanece disponível
        // para histórico e pode ser reativado explicitamente.
        Boolean ativoAnterior = lotacao.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior)) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.LOTACAO, lotacao.getId());
        }
        lotacao.setAtivo(false);
        repositorio.save(lotacao);
        if (Boolean.TRUE.equals(ativoAnterior))
            registrarDisponibilidade(lotacao, ativoAnterior, false, "Inativação administrativa da Lotação", usuario);
        auditar("LOTACAO_INATIVADA", lotacao, ativoAnterior, false);
    }

    private List<Lotacao> listarAcessiveis(UsuarioAplicacao usuario) {
        if (acessoOrganizacionalServico.ehGlobal(usuario)) {
            return repositorio.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        }
        if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && acessoOrganizacionalServico.superintendenciaEfetiva(usuario) != null) {
            return repositorio.findByGestaoLocalSuperintendenciaIdOrderByNomeAsc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(usuario).getId());
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(usuario) && usuario.getGestaoLocal() != null) {
            return repositorio.findByGestaoLocalIdOrderByNomeAsc(usuario.getGestaoLocal().getId());
        }
        if (acessoOrganizacionalServico.ehOperacional(usuario)) {
            Set<Long> ids = scopeServico.obterIdsLotacoes(usuario);
            if (ids.isEmpty()) return List.of();
            return repositorio.findAllById(ids).stream()
                    .filter(lotacao -> acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao))
                    .sorted(Comparator.comparing(Lotacao::getNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        return List.of();
    }

    private Lotacao buscarEntidadeAcessivel(UsuarioAplicacao usuario, Long id) {
        Lotacao lotacao = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada."));
        if (disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(TipoCatalogoTemporal.LOTACAO, id);
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        if (!acessoOrganizacionalServico.podeAcessarLotacao(usuario, lotacao)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lotação não encontrada.");
        }
        return lotacao;
    }

    private boolean possuiVinculos(Long id) {
        return empregadoRepositorio.existsByLotacao_Id(id)
                || appUserRepositorio.existsByLotacaoId(id)
                || appUserRepositorio.existsByVinculosGestao_Lotacao_Id(id);
    }

    private void aplicarDados(Lotacao lotacao, LotacaoDTO dto, GestaoLocal gestaoLocal) {
        lotacao.setNome(dto.nome());
        lotacao.setMunicipio(dto.municipio());
        lotacao.setGestaoLocal(gestaoLocal);
        if (dto.ativo() != null) lotacao.setAtivo(dto.ativo());
        associarTurnos(lotacao, dto.turnos(), gestaoLocal);
    }

    private void exigirGestao(UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente a Gestão Local pode gerir as lotações da sua operação.");
        }
    }

    private void validarDados(LotacaoDTO dto, GestaoLocal gestaoLocal, Long idAtual) {
        if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da lotação é obrigatório.");
        }
        validarGestaoLocalAtiva(gestaoLocal);
        String nome = dto.nome().trim().replaceAll("\\s+", " ");
        if (Set.of("#VALOR!", "#N/A", "#REF!", "#DIV/0!", "#NOME?")
                .contains(nome.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O nome da lotação contém um marcador inválido de planilha.");
        }
        repositorio.findByNomeIgnoreCaseAndGestaoLocalId(nome, gestaoLocal.getId()).ifPresent(existente -> {
            if (!Objects.equals(existente.getId(), idAtual)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe uma lotação com este nome na Gestão Local.");
            }
        });
    }

    private void validarGestaoLocalAtiva(GestaoLocal gestaoLocal) {
        if (gestaoLocal == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local inválida.");
        if (Boolean.FALSE.equals(gestaoLocal.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Gestão Local selecionada está inativa.");
        }
        if (gestaoLocal.getSuperintendencia() == null || Boolean.FALSE.equals(gestaoLocal.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O gestor estadual da Gestão Local está inativo ou não foi definido.");
        }
    }

    private GestaoLocal resolverGestaoLocal(UsuarioAplicacao usuario, Long gestaoLocalId) {
        if (acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            if (usuario.getGestaoLocal() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Usuário Gestão Local sem vínculo organizacional.");
            }
            if (gestaoLocalId != null && !Objects.equals(gestaoLocalId, usuario.getGestaoLocal().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A lotação deve pertencer à sua Gestão Local.");
            }
            return usuario.getGestaoLocal();
        }
        if (gestaoLocalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecione a Gestão Local responsável pela lotação.");
        }
        return gestaoLocalRepositorio.findById(gestaoLocalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local inválida."));
    }

    private LotacaoDTO paraDTO(Lotacao lotacao) {
        List<JornadaResumoDTO> turnos = lotacao.getTurnos() == null ? List.of() : lotacao.getTurnos().stream()
                .filter(jornada -> jornada != null && jornada.getId() != null)
                .map(jornada -> new JornadaResumoDTO(jornada.getId(), jornada.getDescricao()))
                .toList();
        GestaoLocal gestaoLocal = lotacao.getGestaoLocal();
        SuperintendenciaEstadual superintendencia = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;
        return new LotacaoDTO(lotacao.getId(), lotacao.getNome(), turnos,
                gestaoLocal != null ? gestaoLocal.getId() : null, gestaoLocal != null ? gestaoLocal.getNome() : null,
                superintendencia != null ? superintendencia.getId() : null,
                superintendencia != null ? superintendencia.getNome() : null,
                superintendencia != null ? superintendencia.getUf() : null,
                lotacao.getMunicipio(),
                lotacao.getAtivo(), lotacao.getVersion());
    }

    private void associarTurnos(Lotacao lotacao, List<JornadaResumoDTO> turnosDTO, GestaoLocal gestaoLocal) {
        LinkedHashMap<Long, Jornada> jornadas = new LinkedHashMap<>();
        if (turnosDTO != null) {
            for (JornadaResumoDTO turnoDTO : turnosDTO) {
                if (turnoDTO == null || turnoDTO.id() == null || jornadas.containsKey(turnoDTO.id())) continue;
                Jornada jornada = jornadaRepositorio.findById(turnoDTO.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Turno/Jornada não encontrado."));
                if (jornada.getGestaoLocal() == null || gestaoLocal == null
                        || !Objects.equals(jornada.getGestaoLocal().getId(), gestaoLocal.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "O turno selecionado não pertence à Gestão Local da lotação.");
                }
                if (Boolean.FALSE.equals(jornada.getAtivo())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Turno inativo não pode ser associado para uso operacional futuro.");
                }
                jornadas.put(jornada.getId(), jornada);
            }
        }
        lotacao.setTurnos(new ArrayList<>(jornadas.values()));
    }

    private String acaoAtualizacao(Boolean antes, Boolean depois) {
        if (Boolean.TRUE.equals(antes) && Boolean.FALSE.equals(depois)) return "LOTACAO_INATIVADA";
        if (Boolean.FALSE.equals(antes) && Boolean.TRUE.equals(depois)) return "LOTACAO_REATIVADA";
        return "LOTACAO_ATUALIZADA";
    }

    private void auditar(String acao, Lotacao lotacao, Boolean ativoAnterior, Boolean ativoNovo) {
        auditoria.registrarAcao(acao, "Lotacao", lotacao.getId(), lotacao.getNome(),
                RegistroAuditoriaServico.detalhes("gestaoLocalId",
                        lotacao.getGestaoLocal() != null ? lotacao.getGestaoLocal().getId() : null,
                        "ativoAnterior", ativoAnterior, "ativoNovo", ativoNovo,
                        "version", lotacao.getVersion()));
    }

    private void registrarDisponibilidade(Lotacao lotacao, Boolean ativoAnterior, Boolean ativo, String motivo, UsuarioAplicacao usuario) {
        if (disponibilidadeTemporal != null && lotacao.getId() != null)
            disponibilidadeTemporal.registrarTransicao(TipoCatalogoTemporal.LOTACAO, lotacao.getId(),
                    ativoAnterior, Boolean.TRUE.equals(ativo), motivo, usuario);
    }
}
