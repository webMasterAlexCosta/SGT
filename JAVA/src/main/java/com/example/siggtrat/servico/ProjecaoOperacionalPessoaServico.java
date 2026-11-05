package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import com.example.siggtrat.repositorio.VinculoContratualRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Mantém a tabela operacional como projeção interna. O cliente usa Pessoa/Vínculo;
 * não precisa cadastrar um segundo colaborador para lançar frequência.
 */
@Service
public class ProjecaoOperacionalPessoaServico {
    private final EmpregadoRepositorio empregadoRepositorio;
    private final StatusEmpregadoRepositorio statusRepositorio;
    private GestaoContratoTemporalServico gestaoTemporal;
    private VinculoContratualRepositorio vinculoRepositorio;
    private AlocacaoVinculoRepositorio alocacaoRepositorio;
    private MotorTemporalVinculoServico motorVinculo;
    private MotorTemporalAlocacaoServico motorAlocacao;
    private MotorTemporalContratoServico motorContrato;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoTemporal(GestaoContratoTemporalServico servico) { this.gestaoTemporal = servico; }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarReconstrucao(VinculoContratualRepositorio vinculos, AlocacaoVinculoRepositorio alocacoes,
            MotorTemporalVinculoServico motorVinculo, MotorTemporalAlocacaoServico motorAlocacao,
            MotorTemporalContratoServico motorContrato) {
        this.vinculoRepositorio = vinculos;
        this.alocacaoRepositorio = alocacoes;
        this.motorVinculo = motorVinculo;
        this.motorAlocacao = motorAlocacao;
        this.motorContrato = motorContrato;
    }

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
        Empregado empregado = empregadoRepositorio.findByPessoaCanonicaId(pessoa.getId()).orElse(null);
        if (empregado == null) {
            Empregado porCpf = empregadoRepositorio.findByCpf(pessoa.getCpf()).orElse(null);
            if (porCpf != null && porCpf.getPessoaCanonica() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O CPF coincide com um cadastro operacional legado sem associação de identidade comprovada. "
                                + "Trate a identidade/duplicidade documentalmente antes de vinculá-lo à Pessoa.");
            }
            if (porCpf != null && porCpf.getPessoaCanonica() != null
                    && !Objects.equals(porCpf.getPessoaCanonica().getId(), pessoa.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O CPF está associado à projeção de outra Pessoa; a identidade não pode ser fundida automaticamente.");
            }
            empregado = porCpf;
        }
        if (empregado != null) {
            empregado.setPessoaCanonica(pessoa);
            empregado.setNome(pessoa.getNome());
            empregado.setCpf(pessoa.getCpf());
            LocalDate hoje = RelogioSistema.hoje();
            Instant conhecimento = Instant.now();
            // Uma nova contratação já vigente governa a projeção atual. O DELETE
            // compatível do vínculo encerrado não é uma inativação desse novo fato.
            // Programação, período passado e habilitação contratual continuam separados.
            if (vinculo.getId() != null && !vinculo.getDataInicio().isAfter(hoje)
                    && (vinculo.getDataFim() == null || !vinculo.getDataFim().isBefore(hoje))
                    && motorVinculo.reconstruir(vinculo, hoje, conhecimento).situacaoTemporal()
                        == SituacaoTemporalVinculo.VIGENTE) {
                // O chamador pode estar preparando uma frequência histórica.
                // Só uma Alocação reconstruída para hoje pode materializar hoje;
                // sem essa fonte, conserva-se a projeção existente.
                AlocacaoVinculo alocacaoAtual = alocacaoVigenteEm(vinculo, hoje, conhecimento);
                if (alocacaoAtual != null) {
                    aplicarContextoVigente(empregado, vinculo, alocacaoAtual, hoje, conhecimento);
                }
            }
            return empregadoRepositorio.save(empregado);
        }

        empregado = new Empregado();
        empregado.setPessoaCanonica(pessoa);
        empregado.setNome(pessoa.getNome());
        empregado.setCpf(pessoa.getCpf());
        empregado.setMatricula(null);
        empregado.setDataAdmissao(vinculo.getDataInicio());
        LocalDate hoje = RelogioSistema.hoje();
        Instant conhecimento = Instant.now();
        boolean vigente = vinculo.getDataInicio() != null && !vinculo.getDataInicio().isAfter(hoje)
                && (vinculo.getDataFim() == null || !vinculo.getDataFim().isBefore(hoje))
                && (motorVinculo == null || motorVinculo.reconstruir(vinculo, hoje, conhecimento).situacaoTemporal()
                    == SituacaoTemporalVinculo.VIGENTE);
        AlocacaoVinculo contexto = vigente && motorAlocacao != null && vinculo.getId() != null
                ? alocacaoVigenteEm(vinculo, hoje, conhecimento) : alocacao;
        if (contexto == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O Vínculo vigente não possui Alocação para criar sua projeção operacional.");
        // Uma programação sem vínculo vigente mantém apenas uma referência
        // inativa. Ela não materializa um contrato operacional antes da data.
        empregado.setGestaoLocal(gestaoTemporal == null ? vinculo.getContrato().getGestaoLocal()
                : gestaoTemporal.gestaoEm(vinculo.getContrato(), vigente ? hoje : contexto.getDataInicio(), conhecimento));
        empregado.setLotacao(contexto.getLotacao());
        empregado.setJornada(contexto.getJornada());
        empregado.setCargo(contexto.getCargo());
        boolean habilitado = motorContrato == null ? Boolean.TRUE.equals(vinculo.getContrato().getAtivo())
                : motorContrato.reconstruirPosicao(vinculo.getContrato(), hoje, conhecimento).habilitacaoAdministrativa()
                    == HabilitacaoAdministrativaContrato.ATIVO;
        empregado.setStatus(obterStatus(vigente && habilitado ? ConstantesDominio.STATUS_ATIVO : ConstantesDominio.STATUS_INATIVO));
        empregado.setContratos(new ArrayList<>());
        if (vigente) empregado.getContratos().add(vinculo.getContrato());
        return empregadoRepositorio.save(empregado);
    }

    /**
     * A transferência já efetiva muda também a referência contratual da projeção.
     * Reconstruir hoje evita usar uma programação ou o último vínculo cadastrado
     * como se estivesse vigente. Nenhum Vínculo, Alocação ou histórico é alterado.
     */
    @Transactional
    public void sincronizarAposTransferencia(Contrato contrato) {
        sincronizarAposAlteracaoContratual(contrato);
    }

    /** Atualiza apenas a projeção de hoje após um ato contratual já efetivo. */
    @Transactional
    public void sincronizarAposAlteracaoContratual(Contrato contrato) {
        if (contrato == null || contrato.getId() == null) return;
        List<Long> ids = empregadoRepositorio.buscarIdsProjetadosNoContrato(contrato.getId());
        if (ids.isEmpty()) return;
        LocalDate hoje = RelogioSistema.hoje();
        Instant conhecimento = Instant.now();
        for (Empregado empregado : empregadoRepositorio.buscarTodosPorIdParaAtualizacao(ids)) {
            sincronizarProjecaoAtual(empregado, hoje, conhecimento);
        }
    }

    /** Retificação de início também pode ativar uma projeção antes apenas programada. */
    @Transactional
    public void sincronizarAposAlteracaoVinculo(VinculoContratual vinculo) {
        if (vinculo == null || vinculo.getPessoa() == null) return;
        Empregado empregado = empregadoRepositorio.findByPessoaCanonicaId(vinculo.getPessoa().getId()).orElse(null);
        if (empregado == null) return;
        for (Empregado bloqueado : empregadoRepositorio.buscarTodosPorIdParaAtualizacao(List.of(empregado.getId())))
            sincronizarProjecaoAtual(bloqueado, RelogioSistema.hoje(), Instant.now());
    }

    private void sincronizarProjecaoAtual(Empregado empregado, LocalDate hoje, Instant conhecimento) {
        List<VinculoContratual> vigentes = vinculoRepositorio
                .findByPessoaIdOrderByDataInicioAscIdAsc(empregado.getPessoaCanonica().getId()).stream()
                .filter(v -> motorVinculo.reconstruir(v, hoje, conhecimento).situacaoTemporal()
                        == SituacaoTemporalVinculo.VIGENTE).toList();
        if (vigentes.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A Pessoa possui mais de um Vínculo vigente; trate a sobreposição antes de atualizar a projeção.");
        if (vigentes.isEmpty()) {
            empregado.setStatus(obterStatus(ConstantesDominio.STATUS_INATIVO));
            empregado.getContratos().clear();
        } else {
            VinculoContratual vigente = vigentes.get(0);
            AlocacaoVinculo alocacao = alocacaoVigenteEm(vigente, hoje, conhecimento);
            if (alocacao == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O Vínculo vigente não possui Alocação para manter a projeção operacional.");
            aplicarContextoVigente(empregado, vigente, alocacao, hoje, conhecimento);
        }
        empregadoRepositorio.save(empregado);
    }

    private AlocacaoVinculo alocacaoVigenteEm(VinculoContratual vinculo, LocalDate data,
                                            Instant conhecimento) {
        var posicao = motorAlocacao.reconstruir(vinculo, data, conhecimento);
        return posicao.alocacaoId() == null ? null
                : alocacaoRepositorio.findById(posicao.alocacaoId()).orElse(null);
    }

    private void aplicarContextoVigente(Empregado empregado, VinculoContratual vigente, AlocacaoVinculo alocacao,
                                        LocalDate hoje, Instant conhecimento) {
        GestaoLocal gestao = gestaoTemporal.gestaoEm(vigente.getContrato(), hoje, conhecimento);
        var posicaoContrato = motorContrato.reconstruirPosicao(vigente.getContrato(), hoje, conhecimento);
        StatusEmpregado status = obterStatus(posicaoContrato.habilitacaoAdministrativa()
                == HabilitacaoAdministrativaContrato.ATIVO
                ? ConstantesDominio.STATUS_ATIVO : ConstantesDominio.STATUS_INATIVO);
        empregado.setGestaoLocal(gestao);
        empregado.setLotacao(alocacao.getLotacao());
        empregado.setJornada(alocacao.getJornada());
        empregado.setCargo(alocacao.getCargo());
        empregado.setDataAdmissao(vigente.getDataInicio());
        empregado.setStatus(status);
        empregado.getContratos().clear();
        empregado.getContratos().add(vigente.getContrato());
    }

    private StatusEmpregado obterStatus(String descricao) {
        return statusRepositorio.findByDescricaoIgnoreCase(descricao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Status operacional " + descricao + " não foi inicializado."));
    }

    /** Mantém o identificador ativo da projeção alinhado à correção excepcional. */
    @Transactional
    public void sincronizarIdentidade(Pessoa pessoa) {
        Empregado empregado = empregadoRepositorio.findByPessoaCanonicaId(pessoa.getId()).orElse(null);
        if (empregado == null) return;
        empregadoRepositorio.findByCpf(pessoa.getCpf())
                .filter(outro -> !Objects.equals(outro.getId(), empregado.getId()))
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "O CPF corrigido já identifica outro cadastro operacional; é necessário tratar a duplicidade de identidade.");
                });
        empregado.setNome(pessoa.getNome());
        empregado.setCpf(pessoa.getCpf());
        empregadoRepositorio.saveAndFlush(empregado);
    }

    public void validarCorrespondencia(Empregado empregado, Pessoa pessoa) {
        if (empregado == null || pessoa == null || empregado.getPessoaCanonica() == null
                || !Objects.equals(empregado.getPessoaCanonica().getId(), pessoa.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Empregado operacional não corresponde à Pessoa canônica informada.");
        }
    }
}
