package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Adaptador da grade legada para a identidade e o contexto canônicos da data consultada. */
@Service
@Transactional(readOnly = true)
public class GradeFrequenciaCanonicaServico {
    public record Consulta(Specification<Empregado> especificacao, Set<Long> contratos,
                           LocalDate data, Long lotacaoId, Long jornadaId) {}

    private final AlocacaoVinculoRepositorio alocacoes;
    private final EventoVinculoContratualRepositorio eventos;
    private final MotorTemporalContratoServico contratos;
    private final MotorTemporalVinculoServico vinculos;
    private final AcessoOrganizacionalServico acesso;
    private final LotacaoRepositorio lotacoes;
    private final JornadaRepositorio jornadas;
    private final GestaoContratoTemporalServico gestaoTemporal;

    public GradeFrequenciaCanonicaServico(AlocacaoVinculoRepositorio alocacoes,
            EventoVinculoContratualRepositorio eventos, MotorTemporalContratoServico contratos,
            MotorTemporalVinculoServico vinculos, AcessoOrganizacionalServico acesso,
            LotacaoRepositorio lotacoes, JornadaRepositorio jornadas, GestaoContratoTemporalServico gestaoTemporal) {
        this.alocacoes = alocacoes; this.eventos = eventos; this.contratos = contratos;
        this.vinculos = vinculos; this.acesso = acesso; this.lotacoes = lotacoes; this.jornadas = jornadas;
        this.gestaoTemporal = gestaoTemporal;
    }

    public Consulta preparar(UsuarioAplicacao usuario, LocalDate data, Long lotacaoId, Long jornadaId,
                             Collection<Long> contratosSolicitados, String busca) {
        Lotacao lotacao = lotacoes.findById(lotacaoId).orElse(null);
        Jornada jornada = jornadas.findById(jornadaId).orElse(null);
        Set<Long> permitidos = new LinkedHashSet<>();
        if (lotacao != null && jornada != null && acesso.podeAcessarLotacao(usuario, lotacao)
                && acesso.podeAcessarJornada(usuario, jornada)) {
            Instant conhecimento = Instant.now();
            for (Contrato contrato : alocacoes.buscarContratosDaGradeCanonica(data, lotacaoId, jornadaId)) {
                if (!contratosSolicitados.isEmpty() && !contratosSolicitados.contains(contrato.getId())) continue;
                var posicao = contratos.reconstruirPosicao(contrato, data, conhecimento);
                if (posicao.estadoNegocial() == EstadoNegocialContrato.PUBLICADO
                        && posicao.situacaoTemporal() == SituacaoTemporalContrato.VIGENTE
                        && posicao.habilitacaoAdministrativa() == HabilitacaoAdministrativaContrato.ATIVO) {
                    GestaoLocal gestao = gestaoTemporal.gestaoEm(contrato, data, conhecimento);
                    Long gestaoHistorica = gestao == null ? null : gestao.getId();
                    if (gestaoHistorica == null || lotacao.getGestaoLocal() != null
                            && Objects.equals(gestaoHistorica, lotacao.getGestaoLocal().getId())) permitidos.add(contrato.getId());
                }
            }
        }
        String termo = busca == null || busca.isBlank() ? null : busca.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (termo != null && termo.length() > 80) termo = termo.substring(0, 80);
        String termoFinal = termo;
        Set<Long> ids = Set.copyOf(permitidos);
        Specification<Empregado> especificacao = (empregado, query, cb) -> {
            if (ids.isEmpty()) return cb.disjunction();
            Subquery<Long> sq = query.subquery(Long.class);
            Root<AlocacaoVinculo> a = sq.from(AlocacaoVinculo.class);
            var v = a.get("vinculo");
            List<Predicate> filtros = new ArrayList<>();
            filtros.add(cb.equal(v.get("pessoa").get("id"), empregado.get("pessoaCanonica").get("id")));
            filtros.add(v.get("contrato").get("id").in(ids));
            filtros.add(cb.equal(a.get("lotacao").get("id"), lotacaoId));
            filtros.add(cb.equal(a.get("jornada").get("id"), jornadaId));
            filtros.add(cb.equal(a.get("estadoNegocial"), EstadoNegocialAlocacao.REGISTRADA));
            filtros.add(cb.equal(v.get("estadoNegocial"), EstadoNegocialVinculo.REGISTRADO));
            filtros.add(cb.lessThanOrEqualTo(a.get("dataInicio"), data));
            filtros.add(cb.or(cb.isNull(a.get("dataFim")), cb.greaterThanOrEqualTo(a.get("dataFim"), data)));
            filtros.add(cb.lessThanOrEqualTo(v.get("dataInicio"), data));
            filtros.add(cb.or(cb.isNull(v.get("dataFim")), cb.greaterThanOrEqualTo(v.get("dataFim"), data)));
            if (termoFinal != null) {
                List<Predicate> partesNome = new ArrayList<>();
                for (String parte : termoFinal.split(" ")) partesNome.add(cb.like(cb.lower(v.get("pessoa").get("nome")), "%" + parte + "%"));
                List<Predicate> termos = new ArrayList<>();
                termos.add(cb.and(partesNome.toArray(Predicate[]::new)));
                termos.add(cb.like(v.get("pessoa").get("cpf"), "%" + termoFinal + "%"));
                termos.add(cb.like(v.get("matricula"), "%" + termoFinal + "%"));
                String digitos = termoFinal.replaceAll("\\D", "");
                if (digitos.length() >= 4) termos.add(cb.like(v.get("pessoa").get("cpf"), "%" + digitos + "%"));
                filtros.add(cb.or(termos.toArray(Predicate[]::new)));
            }
            sq.select(a.get("id")).where(filtros.toArray(Predicate[]::new));
            return cb.and(cb.isNotNull(empregado.get("pessoaCanonica")), cb.exists(sq));
        };
        return new Consulta(especificacao, ids, data, lotacaoId, jornadaId);
    }

    /** Uma busca por contexto e outra por eventos para toda a página, sem leitura por Pessoa. */
    public Page<EmpregadoFrequenciaDTO> projetar(Page<EmpregadoFrequenciaDTO> pagina, Consulta consulta) {
        if (pagina.isEmpty() || consulta.contratos().isEmpty()) return pagina;
        List<Long> empregadoIds = pagina.getContent().stream().map(EmpregadoFrequenciaDTO::id).toList();
        List<Object[]> contextos = alocacoes.buscarContextosDaGradeCanonica(empregadoIds, consulta.contratos(),
                consulta.data(), consulta.lotacaoId(), consulta.jornadaId());
        if (contextos.isEmpty()) return pagina;
        Set<Long> vinculoIds = contextos.stream().map(linha -> ((AlocacaoVinculo) linha[1]).getVinculo().getId()).collect(Collectors.toSet());
        Map<Long, List<EventoVinculoContratual>> historicos = eventos.findByVinculoIdInOrderByRegistradoEmAscIdAsc(vinculoIds)
                .stream().collect(Collectors.groupingBy(e -> e.getVinculo().getId()));
        Map<Long, EmpregadoFrequenciaDTO> canonicos = new HashMap<>();
        for (Object[] linha : contextos) {
            Long empregadoId = (Long) linha[0];
            AlocacaoVinculo faixa = (AlocacaoVinculo) linha[1];
            VinculoContratual vinculo = faixa.getVinculo();
            Pessoa pessoa = vinculo.getPessoa();
            EmpregadoFrequenciaDTO dto = new EmpregadoFrequenciaDTO(empregadoId, pessoa.getNome(),
                    vinculos.matriculaNaData(vinculo, consulta.data(), historicos.getOrDefault(vinculo.getId(), List.of())), pessoa.getCpf(),
                    rotuloFotografado(faixa.getLotacaoNomeFotografia()),
                    faixa.getLotacao().getId(),
                    rotuloFotografado(faixa.getJornadaDescricaoFotografia()),
                    faixa.getJornada().getId(),
                    rotuloFotografado(faixa.getCargoNomeFotografia()));
            if (canonicos.put(empregadoId, dto) != null) throw new IllegalStateException("Mais de um contexto canônico efetivo na grade para a Pessoa.");
        }
        return pagina.map(dto -> canonicos.getOrDefault(dto.id(), dto));
    }

    private String rotuloFotografado(String fotografia) {
        return fotografia == null || fotografia.isBlank() ? null : fotografia;
    }
}
