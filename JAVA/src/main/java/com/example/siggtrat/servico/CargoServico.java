package com.example.siggtrat.servico;

import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.CargoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class CargoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CargoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final CargoRepositorio repositorio;
    private final AcessoOrganizacionalServico acesso;
    private final ProgramacaoFuturaCatalogoServico programacaoFutura;
    private final RegistroAuditoriaServico auditoria;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeTemporal;

    public CargoServico(CargoRepositorio repositorio, AcessoOrganizacionalServico acesso,
                        ProgramacaoFuturaCatalogoServico programacaoFutura,
                        RegistroAuditoriaServico auditoria) {
        this.repositorio = repositorio;
        this.acesso = acesso;
        this.programacaoFutura = programacaoFutura;
        this.auditoria = auditoria;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeTemporal(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeTemporal = servico;
    }

    @Transactional(readOnly = true)
    public Page<CargoDTO> listarTodos(Pageable pageable) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirConsultaContrato(usuario);

        Page<Cargo> pagina;
        if (acesso.ehGlobal(usuario) || acesso.ehPresidente(usuario)) {
            pagina = repositorio.findAll(pageable);
        } else if (acesso.ehGestorEstadual(usuario) && acesso.superintendenciaEfetiva(usuario) != null) {
            pagina = repositorio.findByGestaoLocalSuperintendenciaId(
                    acesso.superintendenciaEfetiva(usuario).getId(), pageable);
        } else if (usuario.getGestaoLocal() != null) {
            pagina = repositorio.findByGestaoLocalId(usuario.getGestaoLocal().getId(), pageable);
        } else {
            pagina = Page.empty(pageable);
        }
        return pagina.map(this::paraDTO);
    }

    @Transactional(readOnly = true)
    public CargoDTO buscarPorId(Long id) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Cargo cargo = repositorio.findById(id).orElse(null);
        if (cargo == null || !podeConsultar(usuario, cargo)) return null;
        return paraDTO(cargo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CargoDTO criar(CargoDTO dto) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        GestaoLocal gestao = exigirGestorLocalAtivo(usuario);
        bloquearNamespaceAtivo(gestao);
        validar(dto, gestao, null);

        Cargo entity = new Cargo();
        entity.setGestaoLocal(gestao);
        aplicarDados(dto, entity);
        entity.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        Cargo salvo = repositorio.save(entity);
        registrarDisponibilidade(salvo, null, salvo.getAtivo(), "Criação do Cargo", usuario);
        // Sincronizar o item e seu fato antes do lock do executor original.
        repositorio.flush();
        acesso.revalidarAposEsperaSemAguardarExecutor(usuario);
        auditar("CARGO_CRIADO", salvo, null, salvo.getAtivo());
        return paraDTO(salvo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CargoDTO atualizar(Long id, CargoDTO dto) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        GestaoLocal gestao = exigirGestorLocalAtivo(usuario);
        Cargo entity = repositorio.findById(id).orElse(null);
        if (entity != null && disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(TipoCatalogoTemporal.CARGO, id);
        // Não reter a Gestão enquanto se aguarda um Cargo já usado por outro comando.
        bloquearNamespaceAtivo(gestao);
        acesso.revalidarAposEsperaSemAguardarExecutor(usuario);
        if (entity == null || entity.getGestaoLocal() == null
                || !Objects.equals(entity.getGestaoLocal().getId(), gestao.getId())) {
            return null;
        }

        validar(dto, gestao, id);
        Boolean ativoAnterior = entity.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior) && Boolean.FALSE.equals(dto.ativo())) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, entity.getId());
        }
        aplicarDados(dto, entity);
        if (dto.ativo() != null) entity.setAtivo(dto.ativo());
        Cargo salvo = repositorio.save(entity);
        if (!Objects.equals(ativoAnterior, salvo.getAtivo()))
            registrarDisponibilidade(salvo, ativoAnterior, salvo.getAtivo(), "Alteração administrativa do Cargo", usuario);
        auditar(acaoAtualizacao(ativoAnterior, salvo.getAtivo()), salvo, ativoAnterior, salvo.getAtivo());
        // O DTO deve conter o @Version consumido pela edição, não o anterior.
        repositorio.flush();
        return paraDTO(salvo);
    }

    /**
     * A operação DELETE representa inativação administrativa do cadastro.
     * O registro não é apagado fisicamente e pode ser reativado explicitamente.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public boolean excluir(Long id) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        GestaoLocal gestao = exigirGestorLocalAtivo(usuario);
        Cargo entity = repositorio.findById(id).orElse(null);
        if (entity != null && disponibilidadeTemporal != null)
            disponibilidadeTemporal.bloquearParaAlteracao(TipoCatalogoTemporal.CARGO, id);
        acesso.revalidarAposEspera(usuario);
        if (entity == null || entity.getGestaoLocal() == null
                || !Objects.equals(entity.getGestaoLocal().getId(), gestao.getId())) {
            return false;
        }
        Boolean ativoAnterior = entity.getAtivo();
        if (Boolean.TRUE.equals(ativoAnterior)) {
            programacaoFutura.exigirSemProgramacaoFutura(
                    ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, entity.getId());
        }
        entity.setAtivo(false);
        repositorio.save(entity);
        if (Boolean.TRUE.equals(ativoAnterior))
            registrarDisponibilidade(entity, ativoAnterior, false, "Inativação administrativa do Cargo", usuario);
        auditar("CARGO_INATIVADO", entity, ativoAnterior, false);
        return true;
    }

    private void validar(CargoDTO dto, GestaoLocal gestao, Long idAtual) {
        if (dto == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Dados do cargo não informados.");
        if (dto.nome() == null || dto.nome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do cargo é obrigatório.");
        }
        String nome = normalizar(dto.nome());
        repositorio.findByGestaoLocalIdAndNomeIgnoreCase(gestao.getId(), nome)
                .filter(outro -> !Objects.equals(outro.getId(), idAtual))
                .ifPresent(outro -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe um Cargo com este nome na Gestão Local."); });

    }

    private void bloquearNamespaceAtivo(GestaoLocal gestao) {
        // O escalar relê a condição após a espera, sem reutilizar a entidade gerenciada anterior.
        if (!repositorio.bloquearNamespaceDaGestaoLocal(gestao.getId()).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local ativo administra Cargos.");
        }
    }

    private GestaoLocal exigirGestorLocalAtivo(UsuarioAplicacao usuario) {
        if (!acesso.ehGestaoLocal(usuario) || usuario.getGestaoLocal() == null
                || !Boolean.TRUE.equals(usuario.getGestaoLocal().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local ativo administra Cargos.");
        }
        return usuario.getGestaoLocal();
    }

    private boolean podeConsultar(UsuarioAplicacao usuario, Cargo cargo) {
        if (acesso.ehGlobal(usuario) || acesso.ehPresidente(usuario)) return true;
        return cargo.getGestaoLocal() != null && acesso.podeAcessarGestaoLocal(usuario, cargo.getGestaoLocal());
    }

    private void aplicarDados(CargoDTO dto, Cargo entity) {
        entity.setNome(normalizar(dto.nome()));
        entity.setCbo(limpar(dto.cbo()));
        entity.setDescricao(limpar(dto.descricao()));
    }

    private static String normalizar(String valor) {
        return valor == null ? null : valor.trim().replaceAll("\\s+", " ");
    }

    private static String limpar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim().replaceAll("\\s+", " ");
        return limpo.isBlank() ? null : limpo;
    }

    private String acaoAtualizacao(Boolean antes, Boolean depois) {
        if (Boolean.TRUE.equals(antes) && Boolean.FALSE.equals(depois)) return "CARGO_INATIVADO";
        if (Boolean.FALSE.equals(antes) && Boolean.TRUE.equals(depois)) return "CARGO_REATIVADO";
        return "CARGO_ATUALIZADO";
    }

    private void auditar(String acao, Cargo cargo, Boolean ativoAnterior, Boolean ativoNovo) {
        auditoria.registrarAcao(acao, "Cargo", cargo.getId(), cargo.getNome(),
                RegistroAuditoriaServico.detalhes("gestaoLocalId",
                        cargo.getGestaoLocal() != null ? cargo.getGestaoLocal().getId() : null,
                        "ativoAnterior", ativoAnterior, "ativoNovo", ativoNovo,
                        "version", cargo.getVersion()));
    }

    private void registrarDisponibilidade(Cargo cargo, Boolean ativoAnterior, Boolean ativo, String motivo, UsuarioAplicacao usuario) {
        if (disponibilidadeTemporal != null && cargo.getId() != null)
            disponibilidadeTemporal.registrarTransicao(TipoCatalogoTemporal.CARGO, cargo.getId(),
                    ativoAnterior, Boolean.TRUE.equals(ativo), motivo, usuario);
    }

    private CargoDTO paraDTO(Cargo e) {
        GestaoLocal gestao = e.getGestaoLocal();
        SuperintendenciaEstadual se = gestao != null ? gestao.getSuperintendencia() : null;
        return new CargoDTO(
                e.getId(), e.getNome(), e.getCbo(), e.getDescricao(), e.getAtivo(),
                gestao != null ? gestao.getId() : null,
                gestao != null ? gestao.getNome() : null,
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                se != null ? se.getUf() : null,
                e.getVersion()
        );
    }
}
