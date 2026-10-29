package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContaServico {
    private final ContaRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final AgenciaRepositorio agenciaRepositorio;
    private final TipoContaRepositorio tipoContaRepositorio;

    public ContaServico(ContaRepositorio repositorio, EmpregadoRepositorio empregadoRepositorio,
                        AgenciaRepositorio agenciaRepositorio, TipoContaRepositorio tipoContaRepositorio) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.agenciaRepositorio = agenciaRepositorio;
        this.tipoContaRepositorio = tipoContaRepositorio;
    }

    @Transactional(readOnly = true)
    public Page<ContaDTO> listarTodos(Pageable pageable) { return repositorio.findAll(pageable).map(this::paraDTO); }

    @Transactional(readOnly = true)
    public ContaDTO buscarPorId(Long id) { return repositorio.findById(id).map(this::paraDTO).orElse(null); }

    @Transactional
    public ContaDTO criar(ContaDTO dto) {
        Conta entity = new Conta();
        aplicarDados(dto, entity);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public ContaDTO atualizar(Long id, ContaDTO dto) {
        Conta entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        aplicarDados(dto, entity);
        return paraDTO(repositorio.save(entity));
    }

    @Transactional
    public void excluir(Long id) {
        Conta entity = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        if (empregadoRepositorio.findByConta_Id(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A conta está vinculada a um colaborador e não pode ser excluída.");
        }
        repositorio.delete(entity);
        repositorio.flush();
    }

    private void aplicarDados(ContaDTO dto, Conta entity) {
        if (dto == null || dto.numeroConta() == null || dto.numeroConta().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O número da conta é obrigatório.");
        }
        if (dto.agencia() == null || dto.agencia().id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A agência é obrigatória.");
        }
        if (dto.tipoConta() == null || dto.tipoConta().id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo da conta é obrigatório.");
        }
        entity.setNumeroConta(dto.numeroConta());
        entity.setAgencia(agenciaRepositorio.findById(dto.agencia().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agência não encontrada.")));
        entity.setTipoConta(tipoContaRepositorio.findById(dto.tipoConta().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de conta não encontrado.")));
    }

    private ContaDTO paraDTO(Conta e) {
        Agencia a = e.getAgencia();
        Banco b = a == null ? null : a.getBanco();
        AgenciaReferenciaDTO agencia = a == null ? null : new AgenciaReferenciaDTO(
                a.getId(), a.getNumero(),
                b == null ? null : new BancoReferenciaDTO(b.getId(), b.getNome(), b.getCodigo(), b.getVersion()),
                a.getVersion()
        );
        TipoConta t = e.getTipoConta();
        TipoContaDTO tipo = t == null ? null : new TipoContaDTO(t.getId(), t.getDescricao(), t.getVersion());
        return new ContaDTO(e.getId(), e.getNumeroConta(), agencia, tipo, e.getVersion());
    }
}
