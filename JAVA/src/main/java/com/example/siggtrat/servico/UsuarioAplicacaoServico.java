package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesUsuarioAplicacao;
import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsuarioAplicacaoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(UsuarioAplicacaoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final UsuarioAplicacaoRepositorio userRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;
    private final FiltroAutenticacaoJwt jwtAuthenticationFilter;
    private final GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;
    private final long retroactiveUnlockHours;
    private final long retroactiveUnlockDays;
    private final int passwordMinLength;

    public UsuarioAplicacaoServico(UsuarioAplicacaoRepositorio userRepositorio,
                          LotacaoRepositorio lotacaoRepositorio,
                          JornadaRepositorio jornadaRepositorio,
                          SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
                          GestaoLocalRepositorio gestaoLocalRepositorio,
                          PasswordEncoder passwordEncoder,
                          EscopoAcessoUsuarioServico userAccessScopeServico,
                          AcessoOrganizacionalServico acessoOrganizacionalServico,
                          TokenAtivoRepositorio tokenAtivoRepositorio,
                          FiltroAutenticacaoJwt jwtAuthenticationFilter,
                          GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico,
                          PropriedadesUsuarioAplicacao properties,
                          PropriedadesAutenticacao authProperties) {
        this.userRepositorio = userRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.superintendenciaRepositorio = superintendenciaRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.userAccessScopeServico = userAccessScopeServico;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.gestaoAcessoHierarquicoServico = gestaoAcessoHierarquicoServico;
        this.retroactiveUnlockHours = properties.retroactiveUnlockHours();
        this.retroactiveUnlockDays = properties.retroactiveUnlockDays();
        this.passwordMinLength = authProperties.passwordMinLength();
    }

    @Transactional(readOnly = true)
    public List<ResumoUsuarioAplicacaoDTO> listarTodos() {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        List<UsuarioAplicacao> candidatos;

        if (acessoOrganizacionalServico.ehAdministrador(gestor)) {
            candidatos = userRepositorio.findAllByOrderByUsernameAsc();
        } else if (acessoOrganizacionalServico.ehGestorNacional(gestor)) {
            candidatos = userRepositorio.findByPerfilOrderByUsernameAsc(PerfilAcesso.GESTOR_ESTADUAL);
        } else if (acessoOrganizacionalServico.ehGestorEstadual(gestor)) {
            SuperintendenciaEstadual se = acessoOrganizacionalServico.superintendenciaEfetiva(gestor);
            candidatos = se == null ? List.of()
                    : userRepositorio.findByPerfilAndSuperintendenciaIdOrderByUsernameAsc(
                            PerfilAcesso.GESTOR_LOCAL, se.getId());
        } else if (acessoOrganizacionalServico.ehGestaoLocal(gestor) && gestor.getGestaoLocal() != null) {
            candidatos = userRepositorio.findByPerfilAndGestaoLocalIdOrderByUsernameAsc(
                    PerfilAcesso.OPERACIONAL, gestor.getGestaoLocal().getId());
        } else {
            candidatos = List.of(gestor);
        }

        return candidatos.stream()
                .filter(usuario -> podeVisualizarUsuario(gestor, usuario))
                .map(MapeadorUsuarioAplicacaoDTO::deEntidade)
                .collect(Collectors.toList());
    }

    public ResumoUsuarioAplicacaoDTO criar(CriacaoUsuarioAplicacaoDTO dto) {
        if (dto == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe os dados do usuário.");
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        String username = dto.username().trim().toLowerCase(Locale.ROOT);
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        validarEmailInstitucional(email);
        String matricula = UtilitarioMatricula.normalizar(dto.matricula());
        if (matricula == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe a matrícula com 8 dígitos.");
        if (userRepositorio.findByUsername(username).isPresent()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Nome de usuário já cadastrado.");
        if (userRepositorio.existsByEmailIgnoreCase(email)) throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("E-mail já cadastrado.");
        if (userRepositorio.existsByMatricula(matricula)) {
            throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("Matrícula já cadastrada.");
        }

        PerfilAcesso perfil = Objects.requireNonNull(dto.perfil(), "Informe o perfil de acesso.");
        validarPerfilQueGestorPodeCriar(gestor, perfil);

        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setUsername(username);
        user.setEmail(email);
        user.setMatricula(matricula);
        validarNovaSenha(dto.password());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPerfil(perfil);
        user.setActive(dto.active() == null || dto.active());
        user.setForceChangePassword(true);

        aplicarEscopoOrganizacional(user, perfil, dto.superintendenciaId(), dto.gestaoLocalId());
        validarEscopoDentroDoGestor(gestor, user);

        if (perfil == PerfilAcesso.OPERACIONAL) {
            AtualizacaoUsuarioAplicacaoDTO temporario = new AtualizacaoUsuarioAplicacaoDTO(
                    null, null, null, perfil, null,
                    dto.lotacaoId(), dto.jornadaId(), dto.vinculosGestao(),
                    dto.superintendenciaId(), dto.gestaoLocalId());
            List<VinculoGestaoUsuarioAplicacao> vinculos = resolverEscoposGestao(temporario, user.getGestaoLocal());
            if (vinculos.isEmpty()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe ao menos uma lotação/turno para o usuário operacional.");
            aplicarEscoposGestao(user, vinculos);
        } else {
            aplicarEscoposGestao(user, List.of());
        }
        UsuarioAplicacao salvo = userRepositorio.save(user);
        // Sincronizar também os vínculos em cascata antes de reter o executor.
        // Se sua autoridade mudou durante uma espera, toda a criação é revertida.
        userRepositorio.flush();
        acessoOrganizacionalServico.revalidarAposEspera(gestor);
        return MapeadorUsuarioAplicacaoDTO.deEntidade(salvo);
    }

    public String concederAcessoRetroativo(Long id) {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = obterUsuarioOuFalhar(id);
        exigirPodeGerenciar(gestor, user);
        if (!acessoOrganizacionalServico.ehOperacional(user)) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O acesso retroativo é exclusivo de usuários operacionais.");
        user.setRetroactiveUnlockExpireAt(com.example.siggtrat.utilitario.RelogioSistema.agora().plusHours(retroactiveUnlockHours));
        userRepositorio.save(user);
        return "Marcador legado de acesso retroativo registrado por " + retroactiveUnlockHours
                + " horas. A janela operacional e os bloqueios de frequência não foram alterados; "
                + "este marcador não libera lançamentos.";
    }

    public void revogarAcessoRetroativo(Long id) {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = obterUsuarioOuFalhar(id);
        exigirPodeGerenciar(gestor, user);
        user.setRetroactiveUnlockExpireAt(null);
        userRepositorio.save(user);
    }

    public void atualizar(Long id, AtualizacaoUsuarioAplicacaoDTO dto) {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = userRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        acessoOrganizacionalServico.revalidarAposEspera(gestor);
        exigirPodeGerenciar(gestor, user);

        if (dto == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe os dados do usuário.");

        boolean propriaConta = Objects.equals(gestor.getId(), user.getId());
        PerfilAcesso perfilAnterior = user.getPerfil();
        Long seAnterior = user.getSuperintendencia() != null ? user.getSuperintendencia().getId() : null;
        Long gestaoLocalAnterior = user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
        Boolean ativoAnterior = user.getActive();

        if (propriaConta && Boolean.FALSE.equals(dto.active())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Não é permitido desativar a própria conta por esta tela.");
        }
        if (Boolean.FALSE.equals(ativoAnterior) && Boolean.TRUE.equals(dto.active())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Use a ação de ativação para gerar uma nova senha temporária.");
        }

        if (dto.email() != null) {
            String email = dto.email().trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe o e-mail do usuário.");
            validarEmailInstitucional(email);
            if (userRepositorio.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
                throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("E-mail já cadastrado.");
            }
            user.setEmail(email);
        }
        if (dto.active() != null) user.setActive(dto.active());
        if (dto.matricula() != null) {
            String matricula = UtilitarioMatricula.normalizar(dto.matricula());
            if (matricula == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe a matrícula com 8 dígitos.");
            if (userRepositorio.existsByMatriculaAndIdNot(matricula, user.getId())) {
                throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("Matrícula já cadastrada.");
            }
            user.setMatricula(matricula);
        }

        PerfilAcesso perfilFinal = dto.perfil() != null ? dto.perfil() : user.getPerfil();
        validarPerfilQueGestorPodeCriar(gestor, perfilFinal);
        if (propriaConta && perfilAnterior != perfilFinal) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Não é permitido alterar o próprio perfil de acesso.");
        }
        user.setPerfil(perfilFinal);

        boolean senhaRedefinida = dto.password() != null && !dto.password().isBlank();
        if (senhaRedefinida) {
            aplicarNovaSenhaAdministrativa(user, dto.password(), true);
        }

        aplicarEscopoOrganizacional(user, perfilFinal, dto.superintendenciaId(), dto.gestaoLocalId());
        validarEscopoDentroDoGestor(gestor, user);

        boolean shouldUpdateScopes = dto.vinculosGestao() != null || dto.lotacaoId() != null || dto.jornadaId() != null;
        if (perfilFinal != PerfilAcesso.OPERACIONAL) {
            aplicarEscoposGestao(user, List.of());
        } else if (shouldUpdateScopes) {
            List<VinculoGestaoUsuarioAplicacao> vinculos = resolverEscoposGestao(dto, user.getGestaoLocal());
            if (vinculos.isEmpty()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe ao menos uma lotação/turno para este usuário operacional.");
            aplicarEscoposGestao(user, vinculos);
        } else if (userAccessScopeServico.obterEscopos(user).isEmpty()) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe ao menos uma lotação/turno para este usuário operacional.");
        }
        userRepositorio.save(user);
        boolean acabouDeSerInativado = Boolean.TRUE.equals(ativoAnterior)
                && Boolean.FALSE.equals(user.getActive());
        if (acabouDeSerInativado && !propriaConta) {
            inativarUsuarioEHierarquia(user);
            return;
        }
        Long seAtual = user.getSuperintendencia() != null ? user.getSuperintendencia().getId() : null;
        Long gestaoLocalAtual = user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
        boolean segurancaAlterada = senhaRedefinida
                || !Objects.equals(ativoAnterior, user.getActive())
                || perfilAnterior != perfilFinal
                || !Objects.equals(seAnterior, seAtual)
                || !Objects.equals(gestaoLocalAnterior, gestaoLocalAtual);
        if (segurancaAlterada && !propriaConta) {
            revogarSessoes(user);
        }
    }

    public void redefinirSenha(Long id, RedefinicaoSenhaUsuarioAplicacaoDTO dto) {
        if (dto == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe os dados da redefinição de senha.");
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = obterUsuarioOuFalhar(id);
        exigirPodeGerenciar(gestor, user);
        if (Objects.equals(gestor.getId(), user.getId())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Para alterar a própria senha, utilize a opção Meu perfil.");
        }

        aplicarNovaSenhaAdministrativa(user, dto.newPassword(), true);
        userRepositorio.save(user);
        revogarSessoes(user);
    }

    public AtivacaoUsuarioAplicacaoDTO ativarUsuario(Long id) {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = obterUsuarioOuFalhar(id);
        if (Boolean.TRUE.equals(user.getActive())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O usuário já está ativo.");
        }

        // Solicitações públicas nascem com perfil OPERACIONAL e vinculadas à Gestão Local da
        // SE informada pelo empregado. A ativação desse perfil é exclusiva da
        // Gestão Local responsável; nem mesmo o administrador técnico aprova em nome
        // da unidade operacional.
        if (acessoOrganizacionalServico.ehOperacional(user)) {
            if (!acessoOrganizacionalServico.ehGestaoLocal(gestor)
                    || gestor.getGestaoLocal() == null
                    || user.getGestaoLocal() == null
                    || !Objects.equals(gestor.getGestaoLocal().getId(), user.getGestaoLocal().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Somente um Gestor Local da unidade responsável pode ativar este cadastro.");
            }
        } else {
            exigirPodeGerenciar(gestor, user);
        }
        if (Objects.equals(gestor.getId(), user.getId())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A própria conta já está autenticada e não pode ser reativada por esta tela.");
        }
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && user.getSuperintendencia() != null) {
            return gestaoAcessoHierarquicoServico
                    .ativarGestorEstadual(user.getSuperintendencia().getId());
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
            return gestaoAcessoHierarquicoServico.ativarComSenhaTemporaria(user);
        }
        return gestaoAcessoHierarquicoServico.ativarComSenhaTemporaria(user);
    }

    public MeuPerfilDTO obterMeuPerfil(String username) {
        UsuarioAplicacao user = userRepositorio.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        ResumoUsuarioAplicacaoDTO dto = MapeadorUsuarioAplicacaoDTO.deEntidade(user);
        EscopoAcessoUsuarioServico.Escopo primaryScope = userAccessScopeServico.obterEscopoPrincipal(user);

        return new MeuPerfilDTO(
                dto.id(), dto.username(), dto.email(), dto.perfil(), dto.autoridade(), dto.matricula(),
                primaryScope != null ? primaryScope.jornadaDescricao() : "N/A",
                primaryScope != null ? primaryScope.jornadaId() : null,
                dto.jornada(), dto.lotacao(),
                primaryScope != null ? primaryScope.lotacaoNome() : "N/A",
                primaryScope != null ? primaryScope.lotacaoId() : null,
                dto.vinculosGestao(), dto.superintendencia(), dto.gestaoLocal(),
                user.getForceChangePassword() != null && user.getForceChangePassword(),
                dto.retroactiveUnlocked(), user.getRetroactiveUnlockExpireAt(),
                dto.fotoUrl(), user.getCreatedAt(),
                Boolean.TRUE.equals(user.getTwoFactorEnabled()));
    }

    public void excluirUsuario(Long id) {
        UsuarioAplicacao gestor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao user = obterUsuarioOuFalhar(id);
        exigirPodeGerenciar(gestor, user);
        if (Objects.equals(gestor.getId(), user.getId())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Não é permitido desativar a própria conta.");
        }
        inativarUsuarioEHierarquia(user);
    }

    private boolean podeVisualizarUsuario(UsuarioAplicacao gestor, UsuarioAplicacao alvo) {
        if (acessoOrganizacionalServico.ehAdministrador(gestor)) return true;
        if (acessoOrganizacionalServico.ehGestorNacional(gestor)) return acessoOrganizacionalServico.ehGestorEstadual(alvo);
        if (acessoOrganizacionalServico.ehGestorEstadual(gestor)) {
            SuperintendenciaEstadual seAlvo = acessoOrganizacionalServico.superintendenciaEfetiva(alvo);
            return acessoOrganizacionalServico.ehGestaoLocal(alvo)
                    && seAlvo != null
                    && acessoOrganizacionalServico.podeAcessarSuperintendencia(gestor, seAlvo.getId());
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(gestor)) {
            return acessoOrganizacionalServico.ehOperacional(alvo)
                    && alvo.getGestaoLocal() != null
                    && acessoOrganizacionalServico.podeAcessarGestaoLocal(gestor, alvo.getGestaoLocal());
        }
        return Objects.equals(gestor.getId(), alvo.getId());
    }

    private boolean podeGerenciarUsuario(UsuarioAplicacao gestor, UsuarioAplicacao alvo) {
        if (acessoOrganizacionalServico.ehAdministrador(gestor)) return true;
        if (acessoOrganizacionalServico.ehGestorNacional(gestor)) return acessoOrganizacionalServico.ehGestorEstadual(alvo);
        if (acessoOrganizacionalServico.ehGestorEstadual(gestor)) {
            SuperintendenciaEstadual seAlvo = acessoOrganizacionalServico.superintendenciaEfetiva(alvo);
            return acessoOrganizacionalServico.ehGestaoLocal(alvo)
                    && seAlvo != null
                    && acessoOrganizacionalServico.podeAcessarSuperintendencia(gestor, seAlvo.getId());
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(gestor)) {
            return acessoOrganizacionalServico.ehOperacional(alvo)
                    && alvo.getGestaoLocal() != null
                    && acessoOrganizacionalServico.podeAcessarGestaoLocal(gestor, alvo.getGestaoLocal());
        }
        return false;
    }

    private void exigirPodeGerenciar(UsuarioAplicacao gestor, UsuarioAplicacao alvo) {
        if (!podeGerenciarUsuario(gestor, alvo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado no seu escopo de gestão.");
        }
    }

    private void inativarUsuarioEHierarquia(UsuarioAplicacao usuario) {
        if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && usuario.getSuperintendencia() != null) {
            gestaoAcessoHierarquicoServico
                    .inativarSuperintendencia(usuario.getSuperintendencia().getId());
            return;
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(usuario) && usuario.getGestaoLocal() != null) {
            gestaoAcessoHierarquicoServico.inativarUsuario(usuario);
            return;
        }
        gestaoAcessoHierarquicoServico.inativarUsuario(usuario);
    }

    private void validarPerfilQueGestorPodeCriar(UsuarioAplicacao gestor, PerfilAcesso perfil) {
        if (acessoOrganizacionalServico.ehAdministrador(gestor)) return;
        if (acessoOrganizacionalServico.ehGestorNacional(gestor)
                && perfil == PerfilAcesso.GESTOR_ESTADUAL) return;
        if (acessoOrganizacionalServico.ehGestorEstadual(gestor)
                && perfil == PerfilAcesso.GESTOR_LOCAL) return;
        if (acessoOrganizacionalServico.ehGestaoLocal(gestor) && perfil == PerfilAcesso.OPERACIONAL) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não pode atribuir o perfil solicitado.");
    }

    private void validarEscopoDentroDoGestor(UsuarioAplicacao gestor, UsuarioAplicacao alvo) {
        if (acessoOrganizacionalServico.ehAdministrador(gestor)) return;
        if (acessoOrganizacionalServico.ehGestorNacional(gestor) && !acessoOrganizacionalServico.ehGestorEstadual(alvo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O gestor nacional somente administra usuários de gestor estadual.");
        }
        if (acessoOrganizacionalServico.ehGestorNacional(gestor)) return;
        SuperintendenciaEstadual seAlvo = acessoOrganizacionalServico.superintendenciaEfetiva(alvo);
        if (seAlvo == null || !acessoOrganizacionalServico.podeAcessarSuperintendencia(gestor, seAlvo.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A organização selecionada não pertence ao seu escopo.");
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(gestor)
                && (alvo.getGestaoLocal() == null || !acessoOrganizacionalServico.podeAcessarGestaoLocal(gestor, alvo.getGestaoLocal()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A Gestão Local selecionada não pertence ao seu escopo.");
        }
    }

    private UsuarioAplicacao obterUsuarioOuFalhar(Long id) {
        // Os cinco chamadores são comandos de escrita. A espera do alvo
        // precede a verificação que protege a autoridade até o commit.
        UsuarioAplicacao executor = acessoOrganizacionalServico.usuarioAtual();
        UsuarioAplicacao alvo = userRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        acessoOrganizacionalServico.revalidarAposEspera(executor);
        return alvo;
    }

    private void aplicarEscopoOrganizacional(UsuarioAplicacao user, PerfilAcesso perfil, Long superintendenciaId, Long gestaoLocalId) {
        if (perfil == PerfilAcesso.ADMINISTRADOR || perfil == PerfilAcesso.GESTOR_NACIONAL
                || perfil == PerfilAcesso.PRESIDENTE) {
            user.setSuperintendencia(null); user.setGestaoLocal(null); return;
        }
        if (perfil == PerfilAcesso.GESTOR_ESTADUAL) {
            Long targetId = superintendenciaId != null ? superintendenciaId
                    : user.getSuperintendencia() != null ? user.getSuperintendencia().getId() : null;
            if (targetId == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Selecione a superintendência estadual deste usuário.");
            SuperintendenciaEstadual se = superintendenciaRepositorio.findById(targetId)
                    .orElseThrow(() -> new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Gestor estadual inválido."));
            user.setSuperintendencia(se); user.setGestaoLocal(null); return;
        }

        if (perfil == PerfilAcesso.GESTOR_LOCAL || perfil == PerfilAcesso.OPERACIONAL) {
            Long targetId = gestaoLocalId != null ? gestaoLocalId : user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
            if (targetId == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Selecione a Gestão Local deste usuário.");
            GestaoLocal gestaoLocal = gestaoLocalRepositorio.findById(targetId)
                    .orElseThrow(() -> new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Gestão Local inválida."));
            if (!Boolean.TRUE.equals(gestaoLocal.getAtivo())) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A Gestão Local selecionada está inativa.");
            }
            if (superintendenciaId != null && gestaoLocal.getSuperintendencia() != null
                    && !Objects.equals(superintendenciaId, gestaoLocal.getSuperintendencia().getId())) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A Gestão Local selecionada não pertence à Gestão Estadual informada.");
            }
            user.setGestaoLocal(gestaoLocal);
            user.setSuperintendencia(gestaoLocal.getSuperintendencia());
            return;
        }
        user.setSuperintendencia(null); user.setGestaoLocal(null);
    }

    private List<VinculoGestaoUsuarioAplicacao> resolverEscoposGestao(AtualizacaoUsuarioAplicacaoDTO dto, GestaoLocal gestaoLocalUsuario) {
        List<AtualizacaoUsuarioAplicacaoDTO.VinculoGestaoDTO> items = dto.vinculosGestao();
        if ((items == null || items.isEmpty()) && (dto.lotacaoId() != null || dto.jornadaId() != null)) {
            items = List.of(new AtualizacaoUsuarioAplicacaoDTO.VinculoGestaoDTO(dto.lotacaoId(), dto.jornadaId()));
        }
        if (items == null || items.isEmpty()) return List.of();
        if (gestaoLocalUsuario == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O usuário operacional precisa estar vinculado a uma Gestão Local.");

        List<VinculoGestaoUsuarioAplicacao> vinculos = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (AtualizacaoUsuarioAplicacaoDTO.VinculoGestaoDTO item : items) {
            if (item == null || item.lotacaoId() == null || item.jornadaId() == null) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Cada vínculo operacional deve informar lotação e turno.");
            }
            Lotacao lotacao = lotacaoRepositorio.findById(item.lotacaoId())
                    .orElseThrow(() -> new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Lotação inválida."));
            if (lotacao.getGestaoLocal() == null || !Objects.equals(gestaoLocalUsuario.getId(), lotacao.getGestaoLocal().getId())) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A lotação selecionada não pertence à Gestão Local do usuário.");
            }
            Jornada jornada = jornadaRepositorio.findById(item.jornadaId())
                    .orElseThrow(() -> new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Turno/Jornada inválido."));
            if (!jornadaPertenceLotacao(lotacao, jornada)) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O turno selecionado não está habilitado para a lotação informada.");
            }
            String key = item.lotacaoId() + ":" + item.jornadaId();
            if (unique.add(key)) vinculos.add(new VinculoGestaoUsuarioAplicacao(null, lotacao, jornada));
        }
        return vinculos;
    }

    private void aplicarEscoposGestao(UsuarioAplicacao user, List<VinculoGestaoUsuarioAplicacao> vinculos) {
        /*
         * Não substitua a coleção inteira por novas entidades quando o usuário já
         * possui vínculos persistidos. Com orphanRemoval, o Hibernate pode agendar
         * os INSERTs dos novos vínculos antes dos DELETEs dos antigos. Como a tabela
         * possui UNIQUE(usuario_aplicacao_id, id_lotacao, id_jornada), salvar uma edição sem
         * alterar o escopo acabava tentando inserir exatamente os mesmos vínculos e
         * gerava DataIntegrityViolationException/HTTP 409.
         *
         * Fazemos um merge por chave lotação+jornada: vínculos inalterados mantêm a
         * mesma entidade/ID, removidos são retirados da coleção e somente vínculos
         * realmente novos são adicionados. Isso também evita churn desnecessário no
         * banco e mantém o orphanRemoval funcionando normalmente.
         */
        List<VinculoGestaoUsuarioAplicacao> desejados = vinculos == null ? List.of() : vinculos;
        List<VinculoGestaoUsuarioAplicacao> atuais = user.getVinculosGestao();

        Set<String> chavesDesejadas = desejados.stream()
                .filter(Objects::nonNull)
                .map(this::chaveEscopoGestao)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        atuais.removeIf(existente -> {
            String chave = chaveEscopoGestao(existente);
            return chave == null || !chavesDesejadas.contains(chave);
        });

        Set<String> chavesAtuais = atuais.stream()
                .filter(Objects::nonNull)
                .map(this::chaveEscopoGestao)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (VinculoGestaoUsuarioAplicacao desejado : desejados) {
            if (desejado == null) continue;
            String chave = chaveEscopoGestao(desejado);
            if (chave == null || !chavesAtuais.add(chave)) continue;
            desejado.setUser(user);
            atuais.add(desejado);
        }

        if (!desejados.isEmpty()) {
            VinculoGestaoUsuarioAplicacao principal = desejados.get(0);
            user.setLotacao(principal.getLotacao());
            user.setJornada(principal.getJornada());
        } else {
            user.setLotacao(null);
            user.setJornada(null);
        }
    }

    private String chaveEscopoGestao(VinculoGestaoUsuarioAplicacao vinculo) {
        if (vinculo == null || vinculo.getLotacao() == null || vinculo.getJornada() == null
                || vinculo.getLotacao().getId() == null || vinculo.getJornada().getId() == null) {
            return null;
        }
        return vinculo.getLotacao().getId() + ":" + vinculo.getJornada().getId();
    }

    private boolean jornadaPertenceLotacao(Lotacao lotacao, Jornada jornada) {
        if (lotacao == null || lotacao.getId() == null || jornada == null || jornada.getId() == null) return false;
        Long lotId = lotacao.getId();
        if (jornada.getLotacoes() != null && jornada.getLotacoes().stream().filter(Objects::nonNull)
                .anyMatch(l -> lotId.equals(l.getId()))) return true;
        Lotacao atual = lotacaoRepositorio.findById(lotId).orElse(lotacao);
        return atual.getTurnos() != null && atual.getTurnos().stream()
                .anyMatch(t -> t != null && jornada.getId().equals(t.getId()));
    }

    private void aplicarNovaSenhaAdministrativa(UsuarioAplicacao user, String novaSenha, boolean exigirTrocaNoProximoLogin) {
        validarNovaSenha(novaSenha);
        if (senhaCorresponde(novaSenha, user.getPassword())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A nova senha deve ser diferente da senha atual do usuário.");
        }
        user.setPassword(passwordEncoder.encode(novaSenha));
        user.setForceChangePassword(exigirTrocaNoProximoLogin);
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);
    }

    private void validarNovaSenha(String novaSenha) {
        if (novaSenha == null || novaSenha.length() < passwordMinLength) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A nova senha deve ter no mínimo " + passwordMinLength + " caracteres.");
        }
        if (novaSenha.length() > 72) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A nova senha deve ter no máximo 72 caracteres.");
        }
    }

    private boolean senhaCorresponde(String senhaEmTexto, String hash) {
        if (senhaEmTexto == null || hash == null || hash.isBlank()) return false;
        try {
            return passwordEncoder.matches(senhaEmTexto, hash);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void revogarSessoes(UsuarioAplicacao user) {
        if (user == null || user.getUsername() == null) return;
        tokenAtivoRepositorio.deleteByUsername(user.getUsername());
        jwtAuthenticationFilter.removerSessoesUsuarioDoCache(user.getUsername());
    }

    private String texto(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private void validarEmailInstitucional(String email) {
        if (email == null || !email.matches("^[a-z0-9._%+\\-]+@correios\\.com\\.br$")) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe um e-mail institucional @correios.com.br.");
        }
    }
}
