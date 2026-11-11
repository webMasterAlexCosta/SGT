package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.StatusEmpregadoDTO;
import com.example.siggtrat.entidade.StatusEmpregado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class StatusEmpregadoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(StatusEmpregadoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final Set<String> STATUS_ESTRUTURAIS = Set.of(
            ConstantesDominio.STATUS_ATIVO, ConstantesDominio.STATUS_INATIVO,
            ConstantesDominio.STATUS_FERIAS, ConstantesDominio.STATUS_AFASTADO_INSS
    );

    private final StatusEmpregadoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final AcessoOrganizacionalServico acesso;

    public StatusEmpregadoServico(StatusEmpregadoRepositorio repositorio, EmpregadoRepositorio empregadoRepositorio,
                                  AcessoOrganizacionalServico acesso) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public Page<StatusEmpregadoDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public StatusEmpregadoDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public StatusEmpregadoDTO criar(StatusEmpregadoDTO dto) {
        exigirAdministradorAtual(acesso.usuarioAtual());
        String descricao = normalizar(dto == null ? null : dto.descricao());
        repositorio.findByDescricaoIgnoreCase(descricao).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um status com essa descrição.");
        });
        StatusEmpregado entity = new StatusEmpregado();
        entity.setDescricao(descricao);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public StatusEmpregadoDTO atualizar(Long id, StatusEmpregadoDTO dto) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        StatusEmpregado entity = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status não encontrado."));
        exigirAdministradorAtual(usuario);
        String atual = normalizar(entity.getDescricao());
        String nova = normalizar(dto == null ? null : dto.descricao());
        if (STATUS_ESTRUTURAIS.contains(atual) && !atual.equals(nova)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status estruturais do sistema não podem ser renomeados.");
        }
        repositorio.findByDescricaoIgnoreCase(nova).ifPresent(outro -> {
            if (!Objects.equals(outro.getId(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um status com essa descrição.");
            }
        });
        entity.setDescricao(nova);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public void excluir(Long id) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        StatusEmpregado entity = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status não encontrado."));
        exigirAdministradorAtual(usuario);
        String descricao = normalizar(entity.getDescricao());
        if (STATUS_ESTRUTURAIS.contains(descricao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status estruturais do sistema não podem ser excluídos.");
        }
        if (empregadoRepositorio.existsByStatus_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O status está vinculado a colaboradores e não pode ser excluído.");
        }
        repositorio.delete(entity);
        repositorio.flush();
    }

    private void exigirAdministradorAtual(UsuarioAplicacao usuario) {
        acesso.revalidarAposEspera(usuario);
        if (!acesso.ehAdministrador(usuario)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Somente o Administrador pode manter status de colaboradores.");
    }

    private String normalizar(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A descrição do status é obrigatória.");
        }
        String n = value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (n.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A descrição do status deve ter no máximo 30 caracteres.");
        }
        return n;
    }

    private StatusEmpregadoDTO paraDTO(StatusEmpregado e) { return new StatusEmpregadoDTO(e.getId(), e.getDescricao(), e.getVersion()); }
}
