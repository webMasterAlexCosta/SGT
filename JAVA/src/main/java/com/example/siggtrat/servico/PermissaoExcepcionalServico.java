package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/** Ciclo completo de concessão, validação, uso e revogação de privilégios excepcionais. */
@Service
@Transactional
public class PermissaoExcepcionalServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PermissaoExcepcionalServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final EnumSet<TipoPermissaoExcepcional> PERMISSOES_NEGOCIAIS_LOCAIS = EnumSet.of(
            TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
            TipoPermissaoExcepcional.REDUCAO_CONTRATUAL_EXCEPCIONAL,
            TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL,
            TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA);

    private final PermissaoExcepcionalRepositorio permissaoRepositorio;
    private final UsoPermissaoExcepcionalRepositorio usoRepositorio;
    private final UsuarioAplicacaoRepositorio usuarioRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final RegistroAuditoriaServico auditoria;
    private GestaoContratoTemporalServico gestaoContratoTemporal;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    public PermissaoExcepcionalServico(
            PermissaoExcepcionalRepositorio permissaoRepositorio,
            UsoPermissaoExcepcionalRepositorio usoRepositorio,
            UsuarioAplicacaoRepositorio usuarioRepositorio,
            ContratoRepositorio contratoRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            AcessoOrganizacionalServico acesso,
            RegistroAuditoriaServico auditoria) {
        this.permissaoRepositorio = permissaoRepositorio;
        this.usoRepositorio = usoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PermissaoExcepcionalDTO conceder(ConcessaoPermissaoExcepcionalRequisicao requisicao) {
        if (requisicao != null && requisicao.ocorrenciaId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A concessão para ocorrência exige validação do seu contexto histórico.");
        }
        return registrarConcessao(requisicao, null);
    }

    /** Chamado pelo coordenador contextual, na mesma transação e sob seus locks. */
    PermissaoExcepcionalDTO concederParaOcorrencia(ConcessaoPermissaoExcepcionalRequisicao requisicao,
                                                   Ocorrencia ocorrencia) {
        if (ocorrencia == null || !Objects.equals(ocorrencia.getId(), requisicao.ocorrenciaId())
                || requisicao.tipo() != TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS
                || requisicao.contratoId() != null || requisicao.gestaoLocalId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe somente a ocorrência como alvo da concessão de leitura restrita.");
        }
        return registrarConcessao(requisicao, ocorrencia);
    }

    private PermissaoExcepcionalDTO registrarConcessao(ConcessaoPermissaoExcepcionalRequisicao requisicao,
                                                       Ocorrencia ocorrencia) {
        if (requisicao == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe a concessão.");
        UsuarioAplicacao administrador = acesso.usuarioAtual();
        exigirAdministrador(administrador);

        UsuarioAplicacao beneficiario = usuarioRepositorio.findById(requisicao.usuarioBeneficiarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário beneficiário não encontrado."));
        Contrato contrato = requisicao.contratoId() == null ? null : contratoRepositorio
                .findById(requisicao.contratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        GestaoLocal gestaoLocal = requisicao.gestaoLocalId() == null ? null : gestaoLocalRepositorio
                .findById(requisicao.gestaoLocalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
        // Serializar a consulta de duplicidade e a inclusão na mesma raiz de
        // escopo. READ_COMMITTED vê a concessão que terminou durante a espera.
        // Mantemos a ordem raiz -> beneficiário -> executor até o commit.
        if (entityManager != null) {
            if (contrato != null) entityManager.refresh(contrato,
                    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            else if (gestaoLocal != null) entityManager.refresh(gestaoLocal,
                    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(beneficiario, jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        }
        acesso.revalidarAposEspera(administrador);
        exigirAdministrador(administrador);
        if (!Boolean.TRUE.equals(beneficiario.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão não pode ser concedida a uma conta inativa.");
        }
        GestaoLocal gestaoAtualContrato = gestaoAtual(contrato);
        boolean designacaoDocumental = contrato != null && gestaoAtualContrato == null
                && requisicao.tipo() == TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA
                && gestaoLocal != null;
        if (contrato != null && gestaoLocal != null && (gestaoAtualContrato == null
                || !Objects.equals(gestaoAtualContrato.getId(), gestaoLocal.getId())) && !designacaoDocumental) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O contrato não pertence à Gestão Local informada.");
        }
        validarConcessao(requisicao, beneficiario, contrato, gestaoLocal, ocorrencia);
        // Designação administrativa explícita permite conferir acervo cuja origem
        // está ausente. Ela não se transforma em fato histórico sobre a Gestão.
        GestaoLocal escopoGestaoLocal = contrato == null || designacaoDocumental ? gestaoLocal : null;

        boolean duplicada = candidatasAtivas(beneficiario, requisicao.tipo(),
                ocorrencia == null ? null : ocorrencia.getId()).stream()
                .anyMatch(p -> mesmoEscopo(p, contrato, escopoGestaoLocal, ocorrencia));
        if (duplicada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe permissão vigente deste tipo para o mesmo usuário e escopo.");
        }

        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setTipo(requisicao.tipo());
        permissao.setUsuarioBeneficiario(beneficiario);
        permissao.setContrato(contrato);
        permissao.setGestaoLocal(escopoGestaoLocal);
        permissao.setOcorrencia(ocorrencia);
        permissao.setFinalidade(textoObrigatorio(requisicao.finalidade(), "Informe a finalidade."));
        permissao.setJustificativa(textoObrigatorio(requisicao.justificativa(), "Informe a justificativa."));
        permissao.setAutorizacaoAdministrativa(textoObrigatorio(
                requisicao.autorizacaoAdministrativa(), "Informe a autorização administrativa."));
        permissao.setValidoAte(requisicao.validoAte());
        permissao.setConcedidoPorId(administrador.getId());
        permissao.setConcedidoPorUsername(administrador.getUsername());
        permissao = permissaoRepositorio.save(permissao);
        // A concessão pode aguardar FKs após a autorização inicial.
        permissaoRepositorio.flush();
        acesso.revalidarAposEspera(administrador);
        exigirAdministrador(administrador);

        auditoria.registrarAcao("CONCEDER_PERMISSAO_EXCEPCIONAL", "PermissaoExcepcional",
                permissao.getId(), permissao.getTipo().name(), detalhes(permissao));
        return paraDTO(permissao);
    }

    public PermissaoExcepcionalDTO revogar(Long id, RevogacaoPermissaoExcepcionalRequisicao requisicao) {
        UsuarioAplicacao administrador = acesso.usuarioAtual();
        exigirAdministrador(administrador);
        PermissaoExcepcional permissao = obter(id);
        if (entityManager != null) {
            // Mesma ordem dos comandos negociais: raiz contratual antes da concessão.
            // Evita inversão de locks entre revogar e consumir uma autorização.
            if (permissao.getContrato() != null) entityManager.refresh(permissao.getContrato(),
                    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(permissao, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        acesso.revalidarAposEspera(administrador);
        exigirAdministrador(administrador);
        if (!Boolean.TRUE.equals(permissao.getAtivo()) || !permissao.getValidoAte().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão já está inativa ou expirada.");
        }
        String motivo = textoObrigatorio(requisicao == null ? null : requisicao.motivo(),
                "Informe o motivo da revogação.");
        permissao.setAtivo(false);
        permissao.setRevogadoEm(Instant.now());
        permissao.setRevogadoPorId(administrador.getId());
        permissao.setRevogadoPorUsername(administrador.getUsername());
        permissao.setMotivoRevogacao(motivo);
        permissao = permissaoRepositorio.save(permissao);
        auditoria.registrarAcao("REVOGAR_PERMISSAO_EXCEPCIONAL", "PermissaoExcepcional",
                permissao.getId(), permissao.getTipo().name(), Map.of("motivo", motivo));
        return paraDTO(permissao);
    }

    @Transactional(readOnly = true)
    public List<PermissaoExcepcionalDTO> listar() {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        List<PermissaoExcepcional> permissoes = acesso.ehAdministrador(usuario)
                ? permissaoRepositorio.findAllByOrderByConcedidoEmDescIdDesc()
                : permissaoRepositorio.findByUsuarioBeneficiarioIdOrderByConcedidoEmDescIdDesc(usuario.getId());
        return permissoes.stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<UsoPermissaoExcepcionalDTO> listarUsos(Long permissaoId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        exigirAdministrador(usuario);
        obter(permissaoId);
        return usoRepositorio.findByPermissaoIdOrderByUsadoEmDescIdDesc(permissaoId)
                .stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public boolean possuiPermissaoAtiva(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
                                         Contrato contrato, GestaoLocal gestaoLocal) {
        if (usuario == null || usuario.getId() == null || tipo == null) return false;
        return permissoesAtivas(usuario, tipo).stream().anyMatch(p -> aplicaAoEscopo(p, contrato, gestaoLocal));
    }

    /** PER-010: o registro já autorizado informa sua Gestão fotografada. */
    @Transactional(readOnly = true)
    public boolean possuiPermissaoAtivaNoEscopoHistorico(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
                                                         Contrato contrato, GestaoLocal gestaoHistorica) {
        if (usuario == null || usuario.getId() == null || tipo == null) return false;
        return permissoesAtivas(usuario, tipo).stream()
                .anyMatch(p -> aplicaAoEscopoHistorico(p, contrato, gestaoHistorica));
    }

    /** Exige a concessão vigente e grava o consumo dentro da mesma transação da operação protegida. */
    public PermissaoExcepcional exigirERegistrarUso(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
                                                      Contrato contrato, GestaoLocal gestaoLocal,
                                                      String finalidade, String tipoRecurso, Long recursoId) {
        return registrarUsoComRecibo(usuario, tipo, contrato, gestaoLocal, finalidade, tipoRecurso, recursoId, false, null).permissao();
    }

    /** Audita o uso sem trocar o escopo histórico pelo destino atual do contrato. */
    public PermissaoExcepcional exigirERegistrarUsoNoEscopoHistorico(UsuarioAplicacao usuario,
                                                                    TipoPermissaoExcepcional tipo,
                                                                    Contrato contrato, GestaoLocal gestaoHistorica,
                                                                    String finalidade, String tipoRecurso, Long recursoId) {
        return registrarUsoComRecibo(usuario, tipo, contrato, gestaoHistorica, finalidade, tipoRecurso, recursoId, true, null).permissao();
    }

    public PermissaoExcepcional exigirERegistrarUsoDaOcorrencia(UsuarioAplicacao usuario,
            TipoPermissaoExcepcional tipo, Contrato contrato, GestaoLocal gestaoHistorica,
            String finalidade, String tipoRecurso, Long recursoId, Long ocorrenciaId) {
        if (ocorrenciaId == null || ocorrenciaId <= 0
                || !Set.of("Ocorrencia", "AnexoOcorrencia").contains(tipoRecurso)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso de ocorrência inválido.");
        }
        return registrarUsoComRecibo(usuario, tipo, contrato, gestaoHistorica, finalidade,
                tipoRecurso, recursoId, true, ocorrenciaId).permissao();
    }

    public record ConsumoPermissao(PermissaoExcepcional permissao, UsoPermissaoExcepcional uso) { }

    /** Retorna exatamente o uso inserido nesta transação, sem procurar o último uso por horário. */
    public ConsumoPermissao exigirERegistrarUsoComRecibo(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
            Contrato contrato, GestaoLocal gestaoLocal, String finalidade, String tipoRecurso, Long recursoId) {
        return registrarUsoComRecibo(usuario, tipo, contrato, gestaoLocal, finalidade, tipoRecurso, recursoId, false, null);
    }

    private ConsumoPermissao registrarUsoComRecibo(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
                                                     Contrato contrato, GestaoLocal gestaoLocal,
                                                     String finalidade, String tipoRecurso, Long recursoId,
                                                     boolean escopoHistorico, Long ocorrenciaId) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        acesso.revalidarAposEspera(usuario);
        PermissaoExcepcional permissao = candidatasAtivas(usuario, tipo, ocorrenciaId).stream()
                .filter(p -> aplicaAoRecurso(p, contrato, gestaoLocal, escopoHistorico, ocorrenciaId))
                .filter(p -> finalidadeCompativel(p, finalidade))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A operação exige permissão excepcional específica, vigente e compatível com o escopo."));

        if (entityManager != null) {
            // A consulta anterior é apenas a seleção. Sob lock, atualizamos o estado
            // antes do consumo; revogação concluída não pode autorizar um ato novo.
            entityManager.refresh(permissao, jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        }
        if (!Boolean.TRUE.equals(permissao.getAtivo()) || permissao.getRevogadoEm() != null
                || !permissao.getValidoAte().isAfter(Instant.now())
                || !Objects.equals(permissao.getUsuarioBeneficiario().getId(), usuario.getId())
                || !finalidadeCompativel(permissao, finalidade)
                || !aplicaAoRecurso(permissao, contrato, gestaoLocal, escopoHistorico, ocorrenciaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A concessão foi revogada, expirou ou deixou de corresponder ao escopo.");
        }

        UsoPermissaoExcepcional uso = new UsoPermissaoExcepcional();
        uso.setPermissao(permissao);
        uso.setTipo(tipo);
        uso.setUsuarioId(usuario.getId());
        uso.setUsuarioUsername(usuario.getUsername());
        uso.setTipoRecurso(textoObrigatorio(tipoRecurso, "Informe o recurso protegido."));
        uso.setRecursoId(recursoId);
        uso.setFinalidade(textoObrigatorio(finalidade, "Informe a finalidade do uso da permissão."));
        uso = usoRepositorio.save(uso);
        auditoria.registrarAcao("USAR_PERMISSAO_EXCEPCIONAL", tipoRecurso, recursoId,
                tipo.name(), Map.of("permissaoId", permissao.getId(), "usoId", uso.getId(),
                        "finalidade", uso.getFinalidade()));
        return new ConsumoPermissao(permissao, uso);
    }

    private List<PermissaoExcepcional> permissoesAtivas(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo) {
        return permissaoRepositorio.buscarAtivas(usuario.getId(), tipo, Instant.now());
    }

    private List<PermissaoExcepcional> candidatasAtivas(UsuarioAplicacao usuario,
            TipoPermissaoExcepcional tipo, Long ocorrenciaId) {
        return ocorrenciaId == null ? permissoesAtivas(usuario, tipo)
                : permissaoRepositorio.buscarAtivasParaOcorrencia(usuario.getId(), tipo, Instant.now(), ocorrenciaId);
    }

    private void validarConcessao(ConcessaoPermissaoExcepcionalRequisicao r, UsuarioAplicacao beneficiario,
                                  Contrato contrato, GestaoLocal gestaoLocal, Ocorrencia ocorrencia) {
        if (r.tipo() == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Informe o tipo da permissão.");
        if (r.validoAte() == null || !r.validoAte().isAfter(Instant.now())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("A validade da permissão deve estar no futuro.");
        }
        PerfilAcesso perfil = beneficiario.getPerfil();
        if (PERMISSOES_NEGOCIAIS_LOCAIS.contains(r.tipo())) {
            if (perfil != PerfilAcesso.GESTOR_LOCAL || contrato == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Esta permissão exige Gestor Local beneficiário e contrato específico.");
            }
            GestaoLocal gestao = gestaoAtual(contrato);
            if (gestao == null && r.tipo() == TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA) {
                gestao = gestaoLocal;
            }
            if (beneficiario.getGestaoLocal() == null || gestao == null
                    || !Objects.equals(beneficiario.getGestaoLocal().getId(), gestao.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O Gestor Local beneficiário não responde pelo contrato informado.");
            }
            return;
        }
        if (r.tipo() == TipoPermissaoExcepcional.INTERVENCAO_NEGOCIAL_TECNICA) {
            if (perfil != PerfilAcesso.ADMINISTRADOR || contrato == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Intervenção técnica exige administrador beneficiário e contrato específico.");
            }
            return;
        }
        if (r.tipo() == TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS) {
            if (contrato == null && gestaoLocal == null && ocorrencia == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O acesso a dados pessoais restritos exige contrato ou Gestão Local específica.");
            }
            if (perfil != PerfilAcesso.GESTOR_ESTADUAL && perfil != PerfilAcesso.GESTOR_NACIONAL
                    && perfil != PerfilAcesso.PRESIDENTE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este acesso restrito é destinado aos perfis de auditoria ou consulta institucional.");
            }
            GestaoLocal alvo = contrato != null ? gestaoAtual(contrato) : gestaoLocal;
            if (ocorrencia == null && perfil == PerfilAcesso.GESTOR_ESTADUAL && (alvo == null || alvo.getSuperintendencia() == null
                    || beneficiario.getSuperintendencia() == null
                    || !Objects.equals(alvo.getSuperintendencia().getId(), beneficiario.getSuperintendencia().getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O escopo da permissão não pertence à Gestão Estadual beneficiária.");
            }
        }
    }

    private boolean aplicaAoEscopo(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoLocal) {
        if (p.getOcorrencia() != null) return false;
        if (p.getContrato() != null) {
            return contrato != null && Objects.equals(p.getContrato().getId(), contrato.getId());
        }
        if (p.getGestaoLocal() != null) {
            GestaoLocal alvo = contrato != null ? gestaoAtual(contrato) : gestaoLocal;
            return alvo != null && Objects.equals(p.getGestaoLocal().getId(), alvo.getId());
        }
        return false;
    }

    private boolean aplicaAoEscopoHistorico(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoHistorica) {
        if (p.getOcorrencia() != null) return false;
        if (p.getContrato() != null) {
            return contrato != null && Objects.equals(p.getContrato().getId(), contrato.getId());
        }
        // Ausência de fotografia não autoriza inferir o escopo pelo cadastro atual.
        return p.getGestaoLocal() != null && gestaoHistorica != null
                && Objects.equals(p.getGestaoLocal().getId(), gestaoHistorica.getId());
    }

    private boolean aplicaAoRecurso(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoLocal,
                                     boolean historico, Long ocorrenciaId) {
        if (p.getOcorrencia() != null) return ocorrenciaId != null
                && Objects.equals(p.getOcorrencia().getId(), ocorrenciaId);
        return historico ? aplicaAoEscopoHistorico(p, contrato, gestaoLocal) : aplicaAoEscopo(p, contrato, gestaoLocal);
    }

    private boolean finalidadeCompativel(PermissaoExcepcional p, String finalidade) {
        return p.getOcorrencia() == null || finalidade != null
                && Objects.equals(p.getFinalidade(), finalidade.trim().replaceAll("\\s+", " "));
    }

    private boolean mesmoEscopo(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoLocal, Ocorrencia ocorrencia) {
        if (ocorrencia != null || p.getOcorrencia() != null) {
            return Objects.equals(p.getOcorrencia() == null ? null : p.getOcorrencia().getId(),
                    ocorrencia == null ? null : ocorrencia.getId());
        }
        if (contrato != null || p.getContrato() != null) {
            return Objects.equals(id(p.getContrato()), id(contrato));
        }
        return Objects.equals(id(p.getGestaoLocal()), id(gestaoLocal));
    }

    private Long id(Contrato contrato) { return contrato == null ? null : contrato.getId(); }
    private Long id(GestaoLocal gestaoLocal) { return gestaoLocal == null ? null : gestaoLocal.getId(); }

    private GestaoLocal gestaoAtual(Contrato contrato) {
        if (contrato == null) return null;
        return gestaoContratoTemporal == null ? contrato.getGestaoLocal() : gestaoContratoTemporal.gestaoAtual(contrato);
    }

    private PermissaoExcepcional obter(Long id) {
        return permissaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada."));
    }

    private void exigirAdministrador(UsuarioAplicacao usuario) {
        if (!acesso.ehAdministrador(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente a administração técnica concede ou revoga permissões excepcionais.");
        }
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio(mensagem);
        return valor.trim().replaceAll("\\s+", " ");
    }

    private Map<String, Object> detalhes(PermissaoExcepcional p) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tipo", p.getTipo());
        detalhes.put("beneficiarioId", p.getUsuarioBeneficiario().getId());
        detalhes.put("perfilBeneficiario", p.getUsuarioBeneficiario().getPerfil());
        detalhes.put("contratoId", id(p.getContrato()));
        detalhes.put("gestaoLocalId", id(p.getGestaoLocal()));
        detalhes.put("ocorrenciaId", p.getOcorrencia() == null ? null : p.getOcorrencia().getId());
        detalhes.put("validoAte", p.getValidoAte());
        detalhes.put("autorizacaoAdministrativa", p.getAutorizacaoAdministrativa());
        return detalhes;
    }

    private PermissaoExcepcionalDTO paraDTO(PermissaoExcepcional p) {
        Contrato contrato = p.getContrato();
        GestaoLocal gestao = p.getGestaoLocal();
        return new PermissaoExcepcionalDTO(
                p.getId(), p.getTipo(), p.getUsuarioBeneficiario().getId(),
                p.getUsuarioBeneficiario().getUsername(), p.getUsuarioBeneficiario().getPerfil(),
                id(contrato), contrato == null ? null : contrato.getNumeroContrato() + "/" + contrato.getAnoContrato(),
                id(gestao), gestao == null ? null : gestao.getNome(), p.getFinalidade(), p.getJustificativa(),
                p.getAutorizacaoAdministrativa(), p.getConcedidoEm(), p.getConcedidoPorId(),
                p.getConcedidoPorUsername(), p.getValidoAte(), Boolean.TRUE.equals(p.getAtivo())
                        && p.getValidoAte().isAfter(Instant.now()),
                p.getRevogadoEm(), p.getRevogadoPorId(), p.getRevogadoPorUsername(),
                p.getMotivoRevogacao(), p.getVersion(), p.getOcorrencia() == null ? null : p.getOcorrencia().getId());
    }

    private UsoPermissaoExcepcionalDTO paraDTO(UsoPermissaoExcepcional uso) {
        return new UsoPermissaoExcepcionalDTO(uso.getId(), uso.getPermissao().getId(), uso.getTipo(),
                uso.getUsuarioId(), uso.getUsuarioUsername(), uso.getTipoRecurso(), uso.getRecursoId(),
                uso.getFinalidade(), uso.getUsadoEm());
    }
}
