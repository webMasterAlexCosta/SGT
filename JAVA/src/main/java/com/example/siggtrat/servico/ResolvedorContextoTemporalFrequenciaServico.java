package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.alocacao.TipoEventoAlocacaoVinculo;
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
public class ResolvedorContextoTemporalFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ResolvedorContextoTemporalFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    public record Contexto(
            Pessoa pessoa,
            VinculoContratual vinculo,
            AlocacaoVinculo alocacao,
            PosicaoTemporalContratoDTO posicaoContrato,
            PosicaoVinculoTemporalDTO posicaoVinculo,
            PosicaoAlocacaoTemporalDTO posicaoAlocacao,
            GestaoLocal gestaoHistorica,
            ConsultaProvaHistoricaContratoServico.Configuracao configuracaoAlocacao
    ) {
        public Contexto(Pessoa pessoa, VinculoContratual vinculo, AlocacaoVinculo alocacao,
                        PosicaoTemporalContratoDTO posicaoContrato, PosicaoVinculoTemporalDTO posicaoVinculo,
                        PosicaoAlocacaoTemporalDTO posicaoAlocacao, GestaoLocal gestaoHistorica) {
            this(pessoa, vinculo, alocacao, posicaoContrato, posicaoVinculo, posicaoAlocacao, gestaoHistorica,
                    fotografiaDaAlocacao(alocacao));
        }
    }

    private final PessoaRepositorio pessoaRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final MotorTemporalContratoServico motorContrato;
    private final MotorTemporalVinculoServico motorVinculo;
    private final MotorTemporalAlocacaoServico motorAlocacao;
    private final GestaoContratoTemporalServico gestaoContratoTemporal;
    private ComposicaoContratualTemporalServico composicaoTemporal;
    private ConsultaProvaHistoricaContratoServico provasHistoricas;
    private TransferenciaGestaoContratoRepositorio transferenciasGestao;
    private EventoAlocacaoVinculoRepositorio eventosAlocacao;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarComposicaoTemporal(ComposicaoContratualTemporalServico servico) {
        this.composicaoTemporal = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarFontesHistoricas(ConsultaProvaHistoricaContratoServico provas,
                                            TransferenciaGestaoContratoRepositorio transferencias) {
        this.provasHistoricas = provas; this.transferenciasGestao = transferencias;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarEventosAlocacao(EventoAlocacaoVinculoRepositorio eventos) {
        this.eventosAlocacao = eventos;
    }

    public ResolvedorContextoTemporalFrequenciaServico(PessoaRepositorio pessoaRepositorio,
                                                        VinculoContratualRepositorio vinculoRepositorio,
                                                        AlocacaoVinculoRepositorio alocacaoRepositorio,
                                                        MotorTemporalContratoServico motorContrato,
                                                        MotorTemporalVinculoServico motorVinculo,
                                                        MotorTemporalAlocacaoServico motorAlocacao,
                                                        GestaoContratoTemporalServico gestaoContratoTemporal) {
        this.pessoaRepositorio = pessoaRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.motorContrato = motorContrato;
        this.motorVinculo = motorVinculo;
        this.motorAlocacao = motorAlocacao;
        this.gestaoContratoTemporal = gestaoContratoTemporal;
    }

    public Contexto resolver(Empregado empregado, LocalDate dataReferencia) {
        if (empregado == null || dataReferencia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto temporal da frequência inválido.");
        }
        Pessoa pessoa = empregado.getPessoaCanonica();
        if (pessoa == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O colaborador não possui associação canônica comprovada. Regularize a identidade antes de registrar a fonte.");
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

        // Um ADI conhecido antes da transferência pode conservar a Gestão
        // anterior na fotografia integral. Responsabilidade segue seu próprio
        // delta temporal, no mesmo corte de data e conhecimento da frequência.
        GestaoLocal gestaoHistorica = gestaoContratoTemporal.gestaoEm(
                vinculo.getContrato(), dataReferencia, conhecimento);
        if (gestaoHistorica == null || gestaoHistorica.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A posição temporal do Contrato não possui Gestão Local histórica definida.");
        }
        validarEscopoHistoricoDaAlocacao(alocacao, gestaoHistorica.getId());

        return new Contexto(pessoa, vinculo, alocacao, posicaoContrato, posicaoVinculo, posicaoAlocacao, gestaoHistorica,
                resolverConfiguracao(alocacao, dataReferencia, conhecimento));
    }

    /** Complementa somente lacunas do mesmo recorte no corte temporal, sem alterar a Alocação persistida. */
    public ConsultaProvaHistoricaContratoServico.Configuracao resolverConfiguracao(
            AlocacaoVinculo alocacao, LocalDate data, Instant conhecido) {
        var original = fotografiaDaAlocacao(alocacao);
        if (original.jornadaSemanal() != null && !original.jornadaSemanal().isBlank()
                && original.lotacaoNome() != null && !original.lotacaoNome().isBlank()
                && original.jornadaDescricao() != null && !original.jornadaDescricao().isBlank()
                && original.cargoNome() != null && !original.cargoNome().isBlank()
                && (original.municipioComprovado() || alocacao.getMunicipioAusenciaComprovada() == null)) return original;
        if (composicaoTemporal == null || provasHistoricas == null || transferenciasGestao == null) return original;
        Instant conhecimento = conhecido == null ? Instant.now() : conhecido;
        Contrato contrato = alocacao.getVinculo().getContrato();
        Long composicaoId = alocacao.getComposicao() == null ? null : alocacao.getComposicao().getId();
        var recorteVigente = composicaoTemporal.projetarEntidades(contrato, data, conhecimento).stream()
                .filter(p -> p.lotacao() != null && Objects.equals(p.lotacao().getId(), alocacao.getLotacao().getId())
                        && p.jornada() != null && Objects.equals(p.jornada().getId(), alocacao.getJornada().getId())
                        && p.cargo() != null && Objects.equals(p.cargo().getId(), alocacao.getCargo().getId())
                        // A projeção conserva o fim da fotografia original. A
                        // vigência prorrogada pertence ao ciclo temporal do
                        // mesmo contrato, já validado pelo resolvedor canônico.
                        && (p.dataInicio() == null || !data.isBefore(p.dataInicio())))
                .findFirst().orElse(null);
        if (recorteVigente == null) return original;
        var mapas = mapasAplicados(contrato, recorteVigente);
        if (mapas.size() != recorteVigente.mapeamentosOrigemIds().size()) return original;
        var mapaDeOrigem = mapaDeOrigemDaAlocacao(alocacao, mapas, data, conhecimento);
        // O ID da base é opcional no mapa por tríade e pode representar somente
        // a primeira origem de um agregado. A associação vem do replay/evento.
        if (!Objects.equals(recorteVigente.composicaoBaseId(), composicaoId)
                && !(composicaoId == null && !mapas.isEmpty()) && mapaDeOrigem == null) return original;
        Long origemFotografia = alocacao.getAditamentoComposicaoFotografia() == null ? null
                : alocacao.getAditamentoComposicaoFotografia().getId();
        if (origemFotografia != null && !Objects.equals(origemFotografia,recorteVigente.aditamentoComposicaoOrigemId()))
            return original;
        ConsultaProvaHistoricaContratoServico.Configuracao fonte;
        if (recorteVigente.aditamentoComposicaoOrigemId() != null) {
            // A identidade acompanha o replay: ADI quantitativo e REM preservam
            // a origem; uma transferência substitui a configuração pelo mapa.
            fonte = provasHistoricas.configuracaoAditamentoComprovada(
                    recorteVigente.aditamentoComposicaoOrigemId(), conhecimento).orElse(null);
        } else if (!mapas.isEmpty()) {
            var individual = mapaDeOrigem != null ? mapaDeOrigem : mapas.size() == 1 ? mapas.get(0) : null;
            if (individual != null) {
                // Identificar primeiro a origem impede tomar a prova de outro
                // mapa quando o mapa desta alocação ainda não tem documento.
                fonte = provasHistoricas.mapeamentoPossuiProva(individual, conhecimento)
                        ? provasHistoricas.configuracao(individual, conhecimento) : null;
            } else {
                // Inclusão direta no destino, ou legado sem origem individual:
                // somente propriedades comprovadas pelo agregado inteiro.
                fonte = mapas.stream().anyMatch(m -> provasHistoricas.mapeamentoPossuiProva(m, conhecimento))
                        ? new ConsultaProvaHistoricaContratoServico.Configuracao(recorteVigente.lotacaoNome(),
                            recorteVigente.jornadaDescricao(), recorteVigente.cargoNome(),
                            recorteVigente.lotacaoMunicipio(), recorteVigente.jornadaSemanal(),
                            recorteVigente.municipioComprovado()) : null;
            }
        } else {
            fonte = null;
            ComposicaoContratual composicao = alocacao.getComposicao();
            if (composicao != null
                    && Objects.equals(composicao.getLotacao().getId(), alocacao.getLotacao().getId())
                    && Objects.equals(composicao.getJornada().getId(), alocacao.getJornada().getId())
                    && Objects.equals(composicao.getCargo().getId(), alocacao.getCargo().getId())
                    && provasHistoricas.composicaoPossuiProva(composicao, conhecimento)) {
                fonte = provasHistoricas.configuracao(composicao, conhecimento);
            }
        }
        if (fonte == null) return original;
        boolean gradeAusente = original.jornadaSemanal() == null || original.jornadaSemanal().isBlank();
        boolean lacunaMunicipalDocumentada = Boolean.FALSE.equals(alocacao.getMunicipioAusenciaComprovada());
        boolean complementarMunicipio = original.lotacaoMunicipio() == null
                && !Boolean.TRUE.equals(alocacao.getMunicipioAusenciaComprovada())
                && (gradeAusente || lacunaMunicipalDocumentada) && fonte.municipioComprovado();
        return new ConsultaProvaHistoricaContratoServico.Configuracao(
                complementarRotulo(original.lotacaoNome(), fonte.lotacaoNome()),
                complementarRotulo(original.jornadaDescricao(), fonte.jornadaDescricao()),
                complementarRotulo(original.cargoNome(), fonte.cargoNome()),
                complementarMunicipio ? fonte.lotacaoMunicipio() : original.lotacaoMunicipio(),
                gradeAusente ? fonte.jornadaSemanal() : original.jornadaSemanal(),
                original.municipioComprovado() || complementarMunicipio);
    }

    private List<MapeamentoTransferenciaGestao> mapasAplicados(Contrato contrato,
            ComposicaoContratualTemporalServico.ComposicaoProjetada recorte) {
        if (recorte.mapeamentosOrigemIds().isEmpty()) return List.of();
        Map<Long, MapeamentoTransferenciaGestao> encontrados = new LinkedHashMap<>();
        for (var transferencia : transferenciasGestao.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())) {
            for (var mapa : transferencia.getMapeamentos()) {
                if (recorte.mapeamentosOrigemIds().contains(mapa.getId())
                        && Objects.equals(mapa.getLotacaoDestino().getId(), recorte.lotacao().getId())
                        && Objects.equals(mapa.getJornadaDestino().getId(), recorte.jornada().getId())
                        && Objects.equals(mapa.getCargoDestino().getId(), recorte.cargo().getId()))
                    encontrados.put(mapa.getId(), mapa);
            }
        }
        return List.copyOf(encontrados.values());
    }

    private MapeamentoTransferenciaGestao mapaDeOrigemDaAlocacao(AlocacaoVinculo alocacao,
            List<MapeamentoTransferenciaGestao> mapas, LocalDate data, Instant conhecimento) {
        if (eventosAlocacao == null || mapas.isEmpty() || alocacao.getId() == null) return null;
        Set<Long> origens = new HashSet<>();
        for (var evento : eventosAlocacao.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(
                alocacao.getVinculo().getId(), conhecimento)) {
            if (evento.getTipoEvento() != TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                    && evento.getTipoEvento() != TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA) continue;
            if (evento.getAlocacaoNova() == null || !Objects.equals(evento.getAlocacaoNova().getId(), alocacao.getId())
                    || evento.getAlocacaoAnteriorId() == null
                    || Objects.equals(evento.getAlocacaoAnteriorId(), alocacao.getId())
                    || evento.getDataEfetiva() == null || evento.getDataEfetiva().isAfter(data)
                    || !Objects.equals(evento.getLotacaoNovaId(), alocacao.getLotacao().getId())
                    || !Objects.equals(evento.getJornadaNovaId(), alocacao.getJornada().getId())
                    || !Objects.equals(evento.getCargoNovoId(), alocacao.getCargo().getId())) continue;
            // Data da retificação pode anteceder a transferência reaplicada.
            // Identidade causal usa transferência + tríade, no mesmo corte K.
            for (var mapa : mapas) {
                if (mapa.getTransferencia() != null
                        && Objects.equals(mapa.getTransferencia().getId(), evento.getTransferenciaGestaoId())
                        && Objects.equals(mapa.getLotacaoOrigem().getId(), evento.getLotacaoAnteriorId())
                        && Objects.equals(mapa.getJornadaOrigem().getId(), evento.getJornadaAnteriorId())
                        && Objects.equals(mapa.getCargoOrigem().getId(), evento.getCargoAnteriorId()))
                    origens.add(mapa.getId());
            }
        }
        return origens.size() == 1 ? mapas.stream().filter(m -> origens.contains(m.getId())).findFirst().orElse(null) : null;
    }

    private static ConsultaProvaHistoricaContratoServico.Configuracao fotografiaDaAlocacao(AlocacaoVinculo alocacao) {
        return new ConsultaProvaHistoricaContratoServico.Configuracao(alocacao.getLotacaoNomeFotografia(),
                alocacao.getJornadaDescricaoFotografia(), alocacao.getCargoNomeFotografia(),
                alocacao.getLotacaoMunicipioFotografia(), alocacao.getJornadaSemanalFotografia(),
                alocacao.getLotacaoMunicipioFotografia() != null
                        || Boolean.TRUE.equals(alocacao.getMunicipioAusenciaComprovada()));
    }

    private static String complementarRotulo(String original, String documentado) {
        return original == null || original.isBlank() ? documentado : original;
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

}
