package com.example.siggtrat.servico;

import com.example.siggtrat.dto.EnderecoDTO;
import com.example.siggtrat.entidade.Endereco;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.EnderecoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnderecoServico {
    private final EnderecoRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;

    public EnderecoServico(EnderecoRepositorio repositorio, EmpregadoRepositorio empregadoRepositorio) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
    }

    @Transactional(readOnly = true)
    public Page<EnderecoDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public EnderecoDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public EnderecoDTO criar(EnderecoDTO dto) {
        Endereco entity = new Endereco();
        aplicarDados(dto, entity);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public EnderecoDTO atualizar(Long id, EnderecoDTO dto) {
        Endereco entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado."));
        aplicarDados(dto, entity);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public void excluir(Long id) {
        Endereco entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado."));
        if (empregadoRepositorio.findByEndereco_Id(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O endereço está vinculado a um colaborador e não pode ser excluído.");
        }
        repositorio.delete(entity);
        repositorio.flush();
    }

    private void aplicarDados(EnderecoDTO dto, Endereco entity) {
        if (dto == null || dto.logradouro() == null || dto.logradouro().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O logradouro é obrigatório.");
        }
        entity.setLogradouro(dto.logradouro());
        entity.setNumero(dto.numero());
        entity.setBairro(dto.bairro());
        entity.setCidade(dto.cidade());
        entity.setUf(dto.uf());
        entity.setCep(dto.cep());
    }

    private EnderecoDTO paraDTO(Endereco e) {
        return new EnderecoDTO(e.getId(), e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCidade(), e.getUf(), e.getCep(), e.getVersion());
    }
}
