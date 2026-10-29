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
public class PermissaoExcepcionalServico {
    private static final EnumSet<TipoPermissaoExcepcional> PERMISSOES_NEGOCIAIS_LOCAIS = EnumSet.of(
            TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
            TipoPermissaoExcepcional.REDUCAO_CONTRATUAL_EXCEPCIONAL,
            TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL);

    private final PermissaoExcepcionalRepositorio permissaoRepositorio;
    private final UsoPermissaoExcepcionalRepositorio usoRepositorio;
    private final UsuarioAplicacaoRepositorio usuarioRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final RegistroAuditoriaServico auditoria;

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

    public PermissaoExcepcionalDTO conceder(ConcessaoPermissaoExcepcionalRequisicao requisicao) {
        if (requisicao == null) throw new IllegalArgumentException("Informe a concessão.");
        UsuarioAplicacao administrador = acesso.usuarioAtual();
        exigirAdministrador(administrador);

        UsuarioAplicacao beneficiario = usuarioRepositorio.findById(requisicao.usuarioBeneficiarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário beneficiário não encontrado."));
        if (!Boolean.TRUE.equals(beneficiario.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão não pode ser concedida a uma conta inativa.");
        }
        Contrato contrato = requisicao.contratoId() == null ? null : contratoRepositorio
                .findById(requisicao.contratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        GestaoLocal gestaoLocal = requisicao.gestaoLocalId() == null ? null : gestaoLocalRepositorio
                .findById(requisicao.gestaoLocalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
        if (contrato != null && gestaoLocal != null && (contrato.getGestaoLocal() == null
                || !Objects.equals(contrato.getGestaoLocal().getId(), gestaoLocal.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O contrato não pertence à Gestão Local informada.");
        }
        validarConcessao(requisicao, beneficiario, contrato, gestaoLocal);
        GestaoLocal escopoGestaoLocal = contrato == null ? gestaoLocal : null;

        boolean duplicada = permissoesAtivas(beneficiario, requisicao.tipo()).stream()
                .anyMatch(p -> mesmoEscopo(p, contrato, escopoGestaoLocal));
        if (duplicada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe permissão vigente deste tipo para o mesmo usuário e escopo.");
        }

        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setTipo(requisicao.tipo());
        permissao.setUsuarioBeneficiario(beneficiario);
        permissao.setContrato(contrato);
        permissao.setGestaoLocal(escopoGestaoLocal);
        permissao.setFinalidade(textoObrigatorio(requisicao.finalidade(), "Informe a finalidade."));
        permissao.setJustificativa(textoObrigatorio(requisicao.justificativa(), "Informe a justificativa."));
        permissao.setAutorizacaoAdministrativa(textoObrigatorio(
                requisicao.autorizacaoAdministrativa(), "Informe a autorização administrativa."));
        permissao.setValidoAte(requisicao.validoAte());
        permissao.setConcedidoPorId(administrador.getId());
        permissao.setConcedidoPorUsername(administrador.getUsername());
        permissao = permissaoRepositorio.save(permissao);

        auditoria.registrarAcao("CONCEDER_PERMISSAO_EXCEPCIONAL", "PermissaoExcepcional",
                permissao.getId(), permissao.getTipo().name(), detalhes(permissao));
        return paraDTO(permissao);
    }

    public PermissaoExcepcionalDTO revogar(Long id, RevogacaoPermissaoExcepcionalRequisicao requisicao) {
        UsuarioAplicacao administrador = acesso.usuarioAtual();
        exigirAdministrador(administrador);
        PermissaoExcepcional permissao = obter(id);
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

    /** Exige a concessão vigente e grava o consumo dentro da mesma transação da operação protegida. */
    public PermissaoExcepcional exigirERegistrarUso(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo,
                                                      Contrato contrato, GestaoLocal gestaoLocal,
                                                      String finalidade, String tipoRecurso, Long recursoId) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        PermissaoExcepcional permissao = permissoesAtivas(usuario, tipo).stream()
                .filter(p -> aplicaAoEscopo(p, contrato, gestaoLocal))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A operação exige permissão excepcional específica, vigente e compatível com o escopo."));

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
        return permissao;
    }

    private List<PermissaoExcepcional> permissoesAtivas(UsuarioAplicacao usuario, TipoPermissaoExcepcional tipo) {
        return permissaoRepositorio.buscarAtivas(usuario.getId(), tipo, Instant.now());
    }

    private void validarConcessao(ConcessaoPermissaoExcepcionalRequisicao r, UsuarioAplicacao beneficiario,
                                  Contrato contrato, GestaoLocal gestaoLocal) {
        if (r.tipo() == null) throw new IllegalArgumentException("Informe o tipo da permissão.");
        if (r.validoAte() == null || !r.validoAte().isAfter(Instant.now())) {
            throw new IllegalArgumentException("A validade da permissão deve estar no futuro.");
        }
        PerfilAcesso perfil = beneficiario.getPerfil();
        if (PERMISSOES_NEGOCIAIS_LOCAIS.contains(r.tipo())) {
            if (perfil != PerfilAcesso.GESTOR_LOCAL || contrato == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Esta permissão exige Gestor Local beneficiário e contrato específico.");
            }
            if (beneficiario.getGestaoLocal() == null || contrato.getGestaoLocal() == null
                    || !Objects.equals(beneficiario.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())) {
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
            if (contrato == null && gestaoLocal == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O acesso a dados pessoais restritos exige contrato ou Gestão Local específica.");
            }
            if (perfil != PerfilAcesso.GESTOR_ESTADUAL && perfil != PerfilAcesso.GESTOR_NACIONAL
                    && perfil != PerfilAcesso.PRESIDENTE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este acesso restrito é destinado aos perfis de auditoria ou consulta institucional.");
            }
            GestaoLocal alvo = contrato != null ? contrato.getGestaoLocal() : gestaoLocal;
            if (perfil == PerfilAcesso.GESTOR_ESTADUAL && (alvo == null || alvo.getSuperintendencia() == null
                    || beneficiario.getSuperintendencia() == null
                    || !Objects.equals(alvo.getSuperintendencia().getId(), beneficiario.getSuperintendencia().getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O escopo da permissão não pertence à Gestão Estadual beneficiária.");
            }
        }
    }

    private boolean aplicaAoEscopo(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoLocal) {
        if (p.getContrato() != null) {
            return contrato != null && Objects.equals(p.getContrato().getId(), contrato.getId());
        }
        if (p.getGestaoLocal() != null) {
            GestaoLocal alvo = gestaoLocal != null ? gestaoLocal : contrato != null ? contrato.getGestaoLocal() : null;
            return alvo != null && Objects.equals(p.getGestaoLocal().getId(), alvo.getId());
        }
        return false;
    }

    private boolean mesmoEscopo(PermissaoExcepcional p, Contrato contrato, GestaoLocal gestaoLocal) {
        if (contrato != null || p.getContrato() != null) {
            return Objects.equals(id(p.getContrato()), id(contrato));
        }
        return Objects.equals(id(p.getGestaoLocal()), id(gestaoLocal));
    }

    private Long id(Contrato contrato) { return contrato == null ? null : contrato.getId(); }
    private Long id(GestaoLocal gestaoLocal) { return gestaoLocal == null ? null : gestaoLocal.getId(); }

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
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensagem);
        return valor.trim().replaceAll("\\s+", " ");
    }

    private Map<String, Object> detalhes(PermissaoExcepcional p) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tipo", p.getTipo());
        detalhes.put("beneficiarioId", p.getUsuarioBeneficiario().getId());
        detalhes.put("perfilBeneficiario", p.getUsuarioBeneficiario().getPerfil());
        detalhes.put("contratoId", id(p.getContrato()));
        detalhes.put("gestaoLocalId", id(p.getGestaoLocal()));
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
                p.getMotivoRevogacao(), p.getVersion());
    }

    private UsoPermissaoExcepcionalDTO paraDTO(UsoPermissaoExcepcional uso) {
        return new UsoPermissaoExcepcionalDTO(uso.getId(), uso.getPermissao().getId(), uso.getTipo(),
                uso.getUsuarioId(), uso.getUsuarioUsername(), uso.getTipoRecurso(), uso.getRecursoId(),
                uso.getFinalidade(), uso.getUsadoEm());
    }
}
