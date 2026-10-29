package com.example.siggtrat.servico;

import com.example.siggtrat.dto.AgenciaDTO;
import com.example.siggtrat.entidade.Agencia;
import com.example.siggtrat.entidade.Banco;
import com.example.siggtrat.repositorio.AgenciaRepositorio;
import com.example.siggtrat.repositorio.BancoRepositorio;
import com.example.siggtrat.repositorio.ContaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Camada de servico for {@link Agencia}. Encapsulates regras de negocio and data
 * access operations, converting between {@link Agencia} entities and
 * {@link AgenciaDTO} objects. Using this servico ensures that controllers
 * remain thin and do not depend directly on JPA entities.
 */
@Service
public class AgenciaServico {

    private final AgenciaRepositorio agenciaRepositorio;
    private final BancoRepositorio bancoRepositorio;
    private final ContaRepositorio contaRepositorio;

    public AgenciaServico(
            AgenciaRepositorio agenciaRepositorio,
            BancoRepositorio bancoRepositorio,
            ContaRepositorio contaRepositorio
    ) {
        this.agenciaRepositorio = agenciaRepositorio;
        this.bancoRepositorio = bancoRepositorio;
        this.contaRepositorio = contaRepositorio;
    }

    /**
     * Returns a paginated list of agencies as DTOs.
     */
    public Page<AgenciaDTO> listarTodos(Pageable pageable) {
        return agenciaRepositorio.findAll(pageable).map(this::paraDTO);
    }

    /**
     * Retrieves an agency by its identifier and converts it to a DTO. Returns
     * {@code null} if the agency is not found.
     */
    public AgenciaDTO buscarPorId(Long id) {
        return agenciaRepositorio.findById(id).map(this::paraDTO).orElse(null);
    }

    /**
     * Creates a new agency from a DTO. If the referenced bank does not exist,
     * an {@link IllegalArgumentException} is thrown.
     */
    @Transactional
    public AgenciaDTO criar(AgenciaDTO dto) {
        if (dto == null || dto.getBancoId() == null) {
            throw new IllegalArgumentException("Banco informado não encontrado.");
        }
        Banco banco = bancoRepositorio.findById(dto.getBancoId())
                .orElseThrow(() -> new IllegalArgumentException("Banco informado não encontrado."));
        String numero = normalizarNumero(dto.getNumero());
        validarDuplicidade(banco.getId(), numero, null);

        Agencia agencia = new Agencia();
        agencia.setNumero(numero);
        agencia.setBanco(banco);
        Agencia saved = agenciaRepositorio.save(agencia);
        return paraDTO(saved);
    }

    /**
     * Updates an existing agency identified by {@code id} using the values
     * from the provided DTO. Returns {@code null} if the agency does not
     * exist. If the referenced bank does not exist, the bank relationship is
     * left unchanged.
     */
    @Transactional
    public AgenciaDTO atualizar(Long id, AgenciaDTO dto) {
        return agenciaRepositorio.findById(id).map(existing -> {
            if (dto == null) {
                throw new IllegalArgumentException("Dados da agência não informados.");
            }
            Banco banco = dto.getBancoId() == null
                    ? existing.getBanco()
                    : bancoRepositorio.findById(dto.getBancoId())
                    .orElseThrow(() -> new IllegalArgumentException("Banco informado não encontrado."));
            String numero = normalizarNumero(dto.getNumero());
            validarDuplicidade(banco.getId(), numero, id);

            existing.setNumero(numero);
            existing.setBanco(banco);
            Agencia saved = agenciaRepositorio.save(existing);
            return paraDTO(saved);
        }).orElse(null);
    }

    /**
     * Deletes an agency by its identifier.
     */
    @Transactional
    public void excluir(Long id) {
        Agencia agencia = agenciaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agência não encontrada."));
        if (contaRepositorio.existsByAgenciaId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A agência possui contas vinculadas e não pode ser excluída."
            );
        }
        agenciaRepositorio.delete(agencia);
        agenciaRepositorio.flush();
    }

    private String normalizarNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("O número da agência é obrigatório.");
        }
        return numero.trim().replaceAll("\\s+", "");
    }

    private void validarDuplicidade(Long bancoId, String numero, Long idAtual) {
        agenciaRepositorio.findByBancoIdAndNumeroIgnoreCase(bancoId, numero).ifPresent(existente -> {
            if (!java.util.Objects.equals(existente.getId(), idAtual)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe uma agência com este número no banco selecionado."
                );
            }
        });
    }

    /**
     * Helper method to convert an {@link Agencia} entity to an
     * {@link AgenciaDTO} instance. Handles null values gracefully.
     */
    private AgenciaDTO paraDTO(Agencia agencia) {
        if (agencia == null) return null;
        Long bancoId = null;
        Banco banco = agencia.getBanco();
        if (banco != null) {
            bancoId = banco.getId();
        }
        return new AgenciaDTO(agencia.getId(), agencia.getNumero(), bancoId);
    }
}