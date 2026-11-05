package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ContratoFornecedorDTO;
import com.example.siggtrat.dto.FornecedorDTO;
import com.example.siggtrat.dto.FornecedorEntradaDTO;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.FornecedorRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Identidade cadastral compartilhada pelos contratos e seus fatos temporais. */
@Service
@Transactional
public class FornecedorServico {
    private final FornecedorRepositorio fornecedorRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final RegistroAuditoriaServico auditoria;

    public FornecedorServico(FornecedorRepositorio fornecedorRepositorio,
                             ContratoRepositorio contratoRepositorio,
                             AcessoOrganizacionalServico acessoOrganizacionalServico,
                             RegistroAuditoriaServico auditoria) {
        this.fornecedorRepositorio = fornecedorRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<FornecedorDTO> fornecedores() {
        return fornecedorRepositorio.findAllByOrderByRazaoSocialAsc().stream().map(this::fornecedorDTO).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FornecedorDTO criarFornecedor(FornecedorEntradaDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(usuario);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial(textoObrigatorio(dto == null ? null : dto.razaoSocial(), "razaoSocial"));
        fornecedor.setCnpj(cnpjObrigatorio(dto == null ? null : dto.cnpj()));
        fornecedor.setEmail(texto(dto == null ? null : dto.email()));
        Fornecedor salvo = fornecedorRepositorio.saveAndFlush(fornecedor);
        // A criação pode esperar na identidade única do CNPJ. A autoridade
        // precisa continuar válida depois dessa espera e até o commit.
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        acessoOrganizacionalServico.exigirGestaoContrato(usuario);
        auditoria.registrarAcao("FORNECEDOR_CRIADO", "Fornecedor", salvo.getId(), salvo.getRazaoSocial(),
                RegistroAuditoriaServico.detalhes("antes", null, "depois", fornecedorDTO(salvo)));
        return fornecedorDTO(salvo);
    }

    public FornecedorDTO atualizarFornecedor(Long id, FornecedorEntradaDTO dto) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(usuario);
        Fornecedor fornecedor = fornecedorRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> naoEncontrado("Fornecedor"));
        acessoOrganizacionalServico.revalidarAposEspera(usuario);
        FornecedorDTO antes = fornecedorDTO(fornecedor);
        fornecedor.setRazaoSocial(textoObrigatorio(dto == null ? null : dto.razaoSocial(), "razaoSocial"));
        fornecedor.setCnpj(cnpjObrigatorio(dto == null ? null : dto.cnpj()));
        fornecedor.setEmail(texto(dto == null ? null : dto.email()));
        if (dto != null && dto.ativo() != null) fornecedor.setAtivo(dto.ativo());
        Fornecedor salvo = fornecedorRepositorio.saveAndFlush(fornecedor);
        auditoria.registrarAcao("FORNECEDOR_ATUALIZADO", "Fornecedor", salvo.getId(), salvo.getRazaoSocial(),
                RegistroAuditoriaServico.detalhes("antes", antes, "depois", fornecedorDTO(salvo)));
        return fornecedorDTO(salvo);
    }

    public ContratoFornecedorDTO vincularFornecedor(Long contratoId, Long fornecedorId) {
        UsuarioAplicacao usuario = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(usuario);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado("Contrato"));
        acessoOrganizacionalServico.exigirAcessoContrato(usuario, contrato);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato publicado não pode ter fornecedor alterado diretamente. Utilize Aditamento contratual.");
        }
        Fornecedor fornecedor = fornecedorRepositorio.findById(fornecedorId)
                .orElseThrow(() -> naoEncontrado("Fornecedor"));
        String cnpjContrato = texto(contrato.getCnpjEmpresa());
        if (cnpjContrato != null && !cnpjContrato.isBlank()
                && !Objects.equals(cnpjContrato.replaceAll("\\D", ""), fornecedor.getCnpj())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O CNPJ informado no contrato deve corresponder ao fornecedor escolhido.");
        }
        Map<String, Object> antes = identidadeContrato(contrato);
        contrato.setFornecedor(fornecedor);
        contratoRepositorio.save(contrato);
        auditoria.registrarAcao("CONTRATO_FORNECEDOR_VINCULADO", "Contrato", contrato.getId(),
                contrato.getNumeroContrato(), RegistroAuditoriaServico.detalhes(
                        "antes", antes, "depois", identidadeContrato(contrato)));
        return new ContratoFornecedorDTO(contratoId, fornecedorDTO(contrato.getFornecedor()));
    }

    private Map<String, Object> identidadeContrato(Contrato contrato) {
        return RegistroAuditoriaServico.detalhes("fornecedorId",
                contrato.getFornecedor() == null ? null : contrato.getFornecedor().getId(),
                "cnpjEmpresa", contrato.getCnpjEmpresa(), "razaoSocialEmpresa", contrato.getRazaoSocialEmpresa(),
                "nomeFantasiaEmpresa", contrato.getNomeFantasiaEmpresa());
    }

    private FornecedorDTO fornecedorDTO(Fornecedor fornecedor) {
        return new FornecedorDTO(fornecedor.getId(), fornecedor.getRazaoSocial(), fornecedor.getCnpj(),
                fornecedor.getEmail(), fornecedor.getAtivo());
    }

    private String texto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String textoObrigatorio(String dado, String campo) {
        String valor = texto(dado);
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " é obrigatório.");
        }
        return valor;
    }

    private String cnpjObrigatorio(String dado) {
        String cnpj = textoObrigatorio(dado, "cnpj").replaceAll("\\D", "");
        if (cnpj.length() != 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ deve conter 14 dígitos.");
        }
        return cnpj;
    }

    private ResponseStatusException naoEncontrado(String nome) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, nome + " não encontrado.");
    }
}
