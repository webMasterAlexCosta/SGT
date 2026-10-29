package com.example.siggtrat.servico;

import com.example.siggtrat.dto.TipoContaDTO;
import com.example.siggtrat.entidade.TipoConta;
import com.example.siggtrat.repositorio.TipoContaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoContaServico {
    private final TipoContaRepositorio repositorio;
    public TipoContaServico(TipoContaRepositorio repositorio) { this.repositorio = repositorio; }

    @Transactional(readOnly = true)
    public Page<TipoContaDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public TipoContaDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public TipoContaDTO criar(TipoContaDTO dto) {
        TipoConta entity = new TipoConta();
        aplicarDados(dto, entity);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public TipoContaDTO atualizar(Long id, TipoContaDTO dto) {
        return repositorio.findById(id).map(entity -> {
            aplicarDados(dto, entity);
            return paraDTO(repositorio.save(entity));
        }).orElse(null);
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repositorio.existsById(id)) return false;
        repositorio.deleteById(id);
        return true;
    }

    private void aplicarDados(TipoContaDTO dto, TipoConta entity) {
        if (dto == null || dto.descricao() == null || dto.descricao().isBlank()) {
            throw new IllegalArgumentException("A descrição do tipo de conta é obrigatória.");
        }
        entity.setDescricao(dto.descricao().trim());
    }

    private TipoContaDTO paraDTO(TipoConta e) { return new TipoContaDTO(e.getId(), e.getDescricao(), e.getVersion()); }
}
