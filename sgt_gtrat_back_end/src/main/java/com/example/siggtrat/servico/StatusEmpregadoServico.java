package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.StatusEmpregadoDTO;
import com.example.siggtrat.entidade.StatusEmpregado;
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
public class StatusEmpregadoServico {
    private static final Set<String> STATUS_ESTRUTURAIS = Set.of(
            ConstantesDominio.STATUS_ATIVO, ConstantesDominio.STATUS_INATIVO,
            ConstantesDominio.STATUS_FERIAS, ConstantesDominio.STATUS_AFASTADO_INSS
    );

    private final StatusEmpregadoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;

    public StatusEmpregadoServico(StatusEmpregadoRepositorio repositorio, EmpregadoRepositorio empregadoRepositorio) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
    }

    @Transactional(readOnly = true)
    public Page<StatusEmpregadoDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public StatusEmpregadoDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public StatusEmpregadoDTO criar(StatusEmpregadoDTO dto) {
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
        StatusEmpregado entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status não encontrado."));
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
        StatusEmpregado entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status não encontrado."));
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
