package com.example.siggtrat.servico;

import com.example.siggtrat.dto.TipoContratacaoDTO;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.TipoContratacao;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.TipoContratacaoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TipoContratacaoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(TipoContratacaoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final TipoContratacaoRepositorio repositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeTemporal;
    private RegistroAuditoriaServico auditoria;

    public TipoContratacaoServico(
            TipoContratacaoRepositorio repositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico
    ) {
        this.repositorio = repositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarGovernancaTemporal(DisponibilidadeCatalogoTemporalServico disponibilidadeTemporal,
                                              RegistroAuditoriaServico auditoria) {
        this.disponibilidadeTemporal = disponibilidadeTemporal;
        this.auditoria = auditoria;
    }

    public List<TipoContratacaoDTO> listar() {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaContrato(usuario);

        List<TipoContratacao> itens;
        if (acessoOrganizacionalServico.ehGlobal(usuario) || acessoOrganizacionalServico.ehPresidente(usuario)) {
            itens = repositorio.findAllByOrderByGestaoLocalNomeAscDescricaoAsc();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && acessoOrganizacionalServico.superintendenciaEfetiva(usuario) != null) {
            itens = repositorio.findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(usuario).getId()
            );
        } else if (usuario.getGestaoLocal() != null) {
            itens = repositorio.findByGestaoLocalIdOrderByDescricaoAsc(usuario.getGestaoLocal().getId());
        } else {
            itens = List.of();
        }
        return itens.stream().map(this::paraDTO).toList();
    }

    public TipoContratacaoDTO buscarPorId(Long id) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        TipoContratacao entidade = obter(id);
        exigirConsultaNoEscopo(usuario, entidade.getGestaoLocal());
        return paraDTO(entidade);
    }

    @Transactional
    public TipoContratacaoDTO criar(TipoContratacaoDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestorLocalNegocial(usuario);

        if (dto == null || dto.descricao() == null || dto.descricao().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a descrição do Tipo de Contratação.");
        }

        GestaoLocal gestao = usuario.getGestaoLocal();
        if (gestao == null || !Boolean.TRUE.equals(gestao.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local do usuário está inativa ou ausente.");
        }

        String descricao = normalizarDescricao(dto.descricao());
        if (repositorio.findByGestaoLocalIdAndDescricaoIgnoreCase(gestao.getId(), descricao).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um Tipo de Contratação com esta descrição na Gestão Local.");
        }

        TipoContratacao entidade = new TipoContratacao();
        entidade.setGestaoLocal(gestao);
        entidade.setDescricao(descricao);
        entidade.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        entidade.setMatriculaVinculoObrigatoria(Boolean.TRUE.equals(dto.matriculaVinculoObrigatoria()));
        entidade.setCriadoPorId(usuario.getId());
        entidade.setCriadoPorUsername(usuario.getUsername());

        TipoContratacao salva = repositorio.save(entidade);
        registrarMudanca(salva, null, salva.getAtivo(), usuario);
        // A FK da Gestão pode ter aguardado depois da autorização inicial.
        // O fato e a auditoria transacionais também são revertidos na recusa.
        repositorio.flush();
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        return paraDTO(salva);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public TipoContratacaoDTO atualizar(Long id, TipoContratacaoDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        exigirGestorLocalNegocial(usuario);

        TipoContratacao entidade = obter(id);
        if (disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.TIPO_CONTRATACAO, id);
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        if (entidade.getGestaoLocal() == null || usuario.getGestaoLocal() == null
                || !Objects.equals(entidade.getGestaoLocal().getId(), usuario.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tipo de Contratação não encontrado na sua Gestão Local.");
        }

        if (dto == null || dto.descricao() == null || dto.descricao().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a descrição do Tipo de Contratação.");
        }

        String descricao = normalizarDescricao(dto.descricao());
        repositorio.findByGestaoLocalIdAndDescricaoIgnoreCase(usuario.getGestaoLocal().getId(), descricao)
                .filter(outro -> !Objects.equals(outro.getId(), id))
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Já existe um Tipo de Contratação com esta descrição na Gestão Local.");
                });

        entidade.setDescricao(descricao);
        Boolean ativoAnterior = entidade.getAtivo();
        if (dto.ativo() != null) entidade.setAtivo(dto.ativo());
        if (dto.matriculaVinculoObrigatoria() != null) entidade.setMatriculaVinculoObrigatoria(dto.matriculaVinculoObrigatoria());
        TipoContratacao salva = repositorio.save(entidade);
        if (!Objects.equals(ativoAnterior, salva.getAtivo()))
            registrarMudanca(salva, ativoAnterior, salva.getAtivo(), usuario);
        // A confirmação da próxima prévia recebe a versão sincronizada no banco.
        repositorio.flush();
        return paraDTO(salva);
    }

    private TipoContratacao obter(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de Contratação não encontrado."));
    }

    private void exigirGestorLocalNegocial(UsuarioAplicacao usuario) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(usuario)
                || usuario.getGestaoLocal() == null
                || !Boolean.TRUE.equals(usuario.getGestaoLocal().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local ativo administra Tipos de Contratação.");
        }
    }

    private void exigirConsultaNoEscopo(UsuarioAplicacao usuario, GestaoLocal gestaoLocal) {
        if (acessoOrganizacionalServico.ehGlobal(usuario) || acessoOrganizacionalServico.ehPresidente(usuario)) {
            return;
        }
        if (!acessoOrganizacionalServico.podeAcessarGestaoLocal(usuario, gestaoLocal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tipo de Contratação não encontrado no seu escopo.");
        }
    }

    private static String normalizarDescricao(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }

    private void registrarMudanca(TipoContratacao entidade, Boolean antes, Boolean depois,
                                  UsuarioAplicacao usuario) {
        if (disponibilidadeTemporal != null)
            disponibilidadeTemporal.registrarTransicao(TipoCatalogoTemporal.TIPO_CONTRATACAO, entidade.getId(),
                    antes, Boolean.TRUE.equals(depois), "Alteração do Tipo de Contratação", usuario);
        if (auditoria != null)
            auditoria.registrarAcao("TIPO_CONTRATACAO_DISPONIBILIDADE", "TipoContratacao",
                    entidade.getId(), entidade.getDescricao(),
                    RegistroAuditoriaServico.detalhes("ativoAnterior", antes, "ativoNovo", depois));
    }

    private TipoContratacaoDTO paraDTO(TipoContratacao entidade) {
        GestaoLocal gestao = entidade.getGestaoLocal();
        SuperintendenciaEstadual se = gestao != null ? gestao.getSuperintendencia() : null;
        return new TipoContratacaoDTO(
                entidade.getId(),
                entidade.getDescricao(),
                entidade.getAtivo(),
                entidade.getMatriculaVinculoObrigatoria(),
                gestao != null ? gestao.getId() : null,
                gestao != null ? gestao.getNome() : null,
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                se != null ? se.getUf() : null,
                entidade.getCriadoEm(),
                entidade.getCriadoPorId(),
                entidade.getCriadoPorUsername(),
                entidade.getAtualizadoEm(),
                entidade.getVersion()
        );
    }
}
