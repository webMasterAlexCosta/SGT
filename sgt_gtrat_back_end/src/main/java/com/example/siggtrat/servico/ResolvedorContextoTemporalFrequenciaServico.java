package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.PosicaoVinculoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Resolve a verdade da frequência pelo domínio Pessoa -> Vínculo -> Alocação.
 * O registro operacional de entrada não participa da decisão de
 * Contrato/Lotação/Turno/Cargo.
 */
@Service
public class ResolvedorContextoTemporalFrequenciaServico {
    public record Contexto(
            Pessoa pessoa,
            VinculoContratual vinculo,
            AlocacaoVinculo alocacao,
            PosicaoTemporalContratoDTO posicaoContrato,
            PosicaoVinculoTemporalDTO posicaoVinculo,
            PosicaoAlocacaoTemporalDTO posicaoAlocacao,
            GestaoLocal gestaoHistorica
    ) {}

    private final PessoaRepositorio pessoaRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final MotorTemporalContratoServico motorContrato;
    private final MotorTemporalVinculoServico motorVinculo;
    private final MotorTemporalAlocacaoServico motorAlocacao;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;

    public ResolvedorContextoTemporalFrequenciaServico(PessoaRepositorio pessoaRepositorio,
                                                        VinculoContratualRepositorio vinculoRepositorio,
                                                        AlocacaoVinculoRepositorio alocacaoRepositorio,
                                                        MotorTemporalContratoServico motorContrato,
                                                        MotorTemporalVinculoServico motorVinculo,
                                                        MotorTemporalAlocacaoServico motorAlocacao,
                                                        GestaoLocalRepositorio gestaoLocalRepositorio) {
        this.pessoaRepositorio = pessoaRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.motorContrato = motorContrato;
        this.motorVinculo = motorVinculo;
        this.motorAlocacao = motorAlocacao;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
    }

    public Contexto resolver(Empregado empregado, LocalDate dataReferencia) {
        if (empregado == null || dataReferencia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto temporal da frequência inválido.");
        }
        Pessoa pessoa = empregado.getPessoaCanonica();
        if (pessoa == null) {
            String cpf = normalizarCpf(empregado.getCpf());
            if (cpf == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O colaborador não possui identidade canônica nem CPF de compatibilidade.");
            }
            pessoa = pessoaRepositorio.findByCpf(cpf)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Pessoa canônica não encontrada para o colaborador."));
        }

        Instant conhecimento = Instant.now();
        List<ResultadoVinculo> vigentes = new ArrayList<>();
        for (VinculoContratual vinculo : vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId())) {
            PosicaoVinculoTemporalDTO posicao = motorVinculo.reconstruir(vinculo, dataReferencia, conhecimento);
            if (posicao.situacaoTemporal() != SituacaoTemporalVinculo.VIGENTE || vinculo.getContrato() == null) {
                continue;
            }
            PosicaoTemporalContratoDTO posicaoContrato = motorContrato.reconstruirPosicao(
                    vinculo.getContrato(), dataReferencia, conhecimento);
            if (posicaoContrato.estadoNegocial() == EstadoNegocialContrato.PUBLICADO
                    && posicaoContrato.situacaoTemporal() == SituacaoTemporalContrato.VIGENTE
                    && posicaoContrato.habilitacaoAdministrativa() == HabilitacaoAdministrativaContrato.ATIVO) {
                vigentes.add(new ResultadoVinculo(vinculo, posicao, posicaoContrato));
            }
        }
        if (vigentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não existe Vínculo Contratual operacional válido na data da jornada.");
        }
        if (vigentes.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mais de um Vínculo Contratual operacional foi encontrado na mesma data.");
        }
        return resolverEntidades(pessoa, vigentes.get(0).vinculo(), dataReferencia);
    }

    /** Contrato público canônico: o frontend informa o Vínculo, não um Empregado duplicado. */
    public Contexto resolverPorVinculo(Long vinculoId, LocalDate dataReferencia) {
        if (vinculoId == null || dataReferencia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vínculo e data de referência são obrigatórios.");
        }
        VinculoContratual vinculo = vinculoRepositorio.findById(vinculoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Vínculo Contratual não encontrado."));
        return resolverEntidades(vinculo.getPessoa(), vinculo, dataReferencia);
    }

    private Contexto resolverEntidades(Pessoa pessoa, VinculoContratual vinculo, LocalDate dataReferencia) {
        Instant conhecimento = Instant.now();
        PosicaoVinculoTemporalDTO posicaoVinculo = motorVinculo.reconstruir(vinculo, dataReferencia, conhecimento);
        PosicaoTemporalContratoDTO posicaoContrato = motorContrato.reconstruirPosicao(
                vinculo.getContrato(), dataReferencia, conhecimento);
        if (posicaoVinculo.situacaoTemporal() != SituacaoTemporalVinculo.VIGENTE
                || posicaoContrato.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || posicaoContrato.situacaoTemporal() != SituacaoTemporalContrato.VIGENTE
                || posicaoContrato.habilitacaoAdministrativa() != HabilitacaoAdministrativaContrato.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O Vínculo Contratual informado não está operacional na data da jornada.");
        }
        PosicaoAlocacaoTemporalDTO posicaoAlocacao = motorAlocacao.reconstruir(vinculo, dataReferencia, conhecimento);
        if (posicaoAlocacao.situacaoTemporal() != SituacaoTemporalAlocacao.VIGENTE || posicaoAlocacao.alocacaoId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não existe Alocação Lotação + Turno + Cargo válida na data de início da jornada.");
        }
        AlocacaoVinculo alocacao = alocacaoRepositorio.findById(posicaoAlocacao.alocacaoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "A Alocação reconstruída não está disponível para persistência da frequência."));
        if (alocacao.getVinculo() == null || !Objects.equals(alocacao.getVinculo().getId(), vinculo.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocação temporal incompatível com o Vínculo resolvido.");
        }

        Long gestaoHistoricaId = posicaoContrato.versaoContratual() == null
                ? null : posicaoContrato.versaoContratual().gestaoLocalId();
        if (gestaoHistoricaId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A posição temporal do Contrato não possui Gestão Local histórica definida.");
        }
        GestaoLocal gestaoHistorica = gestaoLocalRepositorio.findById(gestaoHistoricaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "A Gestão Local histórica do Contrato não está disponível."));
        validarEscopoHistoricoDaAlocacao(alocacao, gestaoHistoricaId);

        return new Contexto(pessoa, vinculo, alocacao, posicaoContrato, posicaoVinculo, posicaoAlocacao, gestaoHistorica);
    }

    private void validarEscopoHistoricoDaAlocacao(AlocacaoVinculo alocacao, Long gestaoHistoricaId) {
        Long gestaoLotacao = alocacao.getLotacao() == null || alocacao.getLotacao().getGestaoLocal() == null
                ? null : alocacao.getLotacao().getGestaoLocal().getId();
        Long gestaoJornada = alocacao.getJornada() == null || alocacao.getJornada().getGestaoLocal() == null
                ? null : alocacao.getJornada().getGestaoLocal().getId();
        Long gestaoCargo = alocacao.getCargo() == null || alocacao.getCargo().getGestaoLocal() == null
                ? null : alocacao.getCargo().getGestaoLocal().getId();
        if (!Objects.equals(gestaoHistoricaId, gestaoLotacao)
                || !Objects.equals(gestaoHistoricaId, gestaoJornada)
                || !Objects.equals(gestaoHistoricaId, gestaoCargo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A Alocação histórica não pertence à Gestão Local válida do Contrato na data da jornada.");
        }
    }

    private record ResultadoVinculo(VinculoContratual vinculo, PosicaoVinculoTemporalDTO posicaoVinculo,
                                    PosicaoTemporalContratoDTO posicaoContrato) {}

    private static String normalizarCpf(String cpf) {
        if (cpf == null) return null;
        String n = cpf.replaceAll("\\D", "");
        return n.length() == 11 ? n : null;
    }
}
