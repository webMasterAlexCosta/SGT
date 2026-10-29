package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Mantém a tabela operacional como projeção interna. O cliente usa Pessoa/Vínculo;
 * não precisa cadastrar um segundo colaborador para lançar frequência.
 */
@Service
public class ProjecaoOperacionalPessoaServico {
    private final EmpregadoRepositorio empregadoRepositorio;
    private final StatusEmpregadoRepositorio statusRepositorio;

    public ProjecaoOperacionalPessoaServico(EmpregadoRepositorio empregadoRepositorio,
                                             StatusEmpregadoRepositorio statusRepositorio) {
        this.empregadoRepositorio = empregadoRepositorio;
        this.statusRepositorio = statusRepositorio;
    }

    @Transactional
    public Empregado projetar(VinculoContratual vinculo, AlocacaoVinculo alocacao) {
        if (vinculo == null || vinculo.getPessoa() == null || alocacao == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pessoa, Vínculo e Alocação são obrigatórios para a projeção operacional.");
        }
        Pessoa pessoa = vinculo.getPessoa();
        Empregado empregado = empregadoRepositorio.findByPessoaCanonicaId(pessoa.getId())
                .or(() -> empregadoRepositorio.findByCpf(pessoa.getCpf()))
                .orElse(null);
        if (empregado != null) {
            empregado.setPessoaCanonica(pessoa);
            empregado.setNome(pessoa.getNome());
            empregado.setCpf(pessoa.getCpf());
            return empregadoRepositorio.save(empregado);
        }

        empregado = new Empregado();
        empregado.setPessoaCanonica(pessoa);
        empregado.setNome(pessoa.getNome());
        empregado.setCpf(pessoa.getCpf());
        empregado.setMatricula(null);
        empregado.setDataAdmissao(vinculo.getDataInicio());
        empregado.setGestaoLocal(vinculo.getContrato().getGestaoLocal());
        empregado.setLotacao(alocacao.getLotacao());
        empregado.setJornada(alocacao.getJornada());
        empregado.setCargo(alocacao.getCargo());
        empregado.setStatus(statusRepositorio.findByDescricaoIgnoreCase(ConstantesDominio.STATUS_ATIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Status operacional ATIVO não foi inicializado.")));
        empregado.setContratos(new ArrayList<>());
        empregado.getContratos().add(vinculo.getContrato());
        return empregadoRepositorio.save(empregado);
    }

    public void validarCorrespondencia(Empregado empregado, Pessoa pessoa) {
        if (empregado == null || pessoa == null || empregado.getPessoaCanonica() == null
                || !Objects.equals(empregado.getPessoaCanonica().getId(), pessoa.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Empregado operacional não corresponde à Pessoa canônica informada.");
        }
    }
}
