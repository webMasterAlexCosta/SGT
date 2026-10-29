package com.example.siggtrat.servico;

import com.example.siggtrat.dto.EmpregadoReferenciaDTO;
import com.example.siggtrat.dto.TelefoneDTO;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Telefone;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.TelefoneRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TelefoneServico {
    private final TelefoneRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;

    public TelefoneServico(TelefoneRepositorio repositorio, EmpregadoRepositorio empregadoRepositorio) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
    }

    @Transactional(readOnly = true)
    public Page<TelefoneDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public TelefoneDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public TelefoneDTO criar(TelefoneDTO dto) {
        Telefone entity = new Telefone();
        aplicarDados(dto, entity, true);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public TelefoneDTO atualizar(Long id, TelefoneDTO dto) {
        Telefone entity = repositorio.findById(id).orElse(null);
        if (entity == null) return null;
        aplicarDados(dto, entity, false);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repositorio.existsById(id)) return false;
        repositorio.deleteById(id);
        return true;
    }

    private void aplicarDados(TelefoneDTO dto, Telefone entity, boolean exigirEmpregado) {
        if (dto == null || dto.numero() == null || dto.numero().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O número do telefone é obrigatório.");
        }
        entity.setNumero(dto.numero().trim());
        if (dto.empregado() != null && dto.empregado().id() != null) {
            Empregado empregado = empregadoRepositorio.findById(dto.empregado().id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado."));
            entity.setEmpregado(empregado);
        } else if (exigirEmpregado && entity.getEmpregado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O empregado do telefone é obrigatório.");
        }
    }

    private TelefoneDTO paraDTO(Telefone e) {
        return new TelefoneDTO(e.getId(), e.getNumero(),
                e.getEmpregado() == null ? null : new EmpregadoReferenciaDTO(e.getEmpregado().getId()), e.getVersion());
    }
}
