package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Seleção operacional da grade e de Vínculos no mesmo recorte contratual D/K. */
@Service
@Transactional(readOnly = true)
public class GradeFrequenciaCanonicaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(GradeFrequenciaCanonicaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    public record Consulta(Specification<Empregado> especificacao,
                           Specification<VinculoContratual> vinculos, Set<Long> contratos,
                           LocalDate data, Instant conhecidoAte, Long lotacaoId, Long jornadaId, Long gestaoId) {}

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
        if (data == null || lotacaoId == null || jornadaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data, Lotação e Jornada são obrigatórias para selecionar frequência.");
        }
        Instant conhecimento = Instant.now();
        Lotacao lotacao = lotacoes.findById(lotacaoId).orElse(null);
        Jornada jornada = jornadas.findById(jornadaId).orElse(null);
        if (lotacao == null || jornada == null || !acesso.podeAcessarLotacao(usuario, lotacao)
                || !acesso.podeAcessarJornada(usuario, jornada)
                || acesso.ehOperacional(usuario) && !acesso.podeLancarFrequenciaDiaria(usuario, lotacao, jornada)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Lotação/Jornada não pertence ao seu escopo de frequência.");
        }
        Long gestaoId = lotacao.getGestaoLocal() == null ? null : lotacao.getGestaoLocal().getId();
        if (gestaoId == null || jornada.getGestaoLocal() == null
                || !Objects.equals(gestaoId, jornada.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Lotação/Jornada não possui Gestão Local coerente.");
        }
        Set<Long> solicitados = contratosSolicitados == null ? Set.of() : new LinkedHashSet<>(contratosSolicitados);
        Set<Long> permitidos = new LinkedHashSet<>();
        for (Contrato contrato : alocacoes.buscarContratosDaGradeCanonica(data, lotacaoId, jornadaId)) {
            if (!solicitados.isEmpty() && !solicitados.contains(contrato.getId())) continue;
            var posicao = contratos.reconstruirPosicao(contrato, data, conhecimento);
            if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                    || posicao.situacaoTemporal() != SituacaoTemporalContrato.VIGENTE
                    || posicao.habilitacaoAdministrativa() != HabilitacaoAdministrativaContrato.ATIVO) continue;
            GestaoLocal historica = gestaoTemporal.gestaoEm(contrato, data, conhecimento);
            if (historica != null && Objects.equals(historica.getId(), gestaoId)) permitidos.add(contrato.getId());
        }
        String termo = busca == null || busca.isBlank() ? null : busca.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (termo != null && termo.length() > 80) termo = termo.substring(0, 80);
        String termoFinal = termo;
        Set<Long> ids = Set.copyOf(permitidos);
        Specification<Empregado> empregados = (empregado, query, cb) -> {
            if (ids.isEmpty()) return cb.disjunction();
            Subquery<Long> sq = query.subquery(Long.class);
            Root<AlocacaoVinculo> a = sq.from(AlocacaoVinculo.class);
            var v = a.get("vinculo");
            List<Predicate> filtros = faixaNoDia(a, cb, data, lotacaoId, jornadaId, gestaoId);
            filtros.add(cb.equal(v.get("pessoa").get("id"), empregado.get("pessoaCanonica").get("id")));
            filtros.add(v.get("contrato").get("id").in(ids));
            if (termoFinal != null) filtros.add(buscaPessoaVinculo(v, cb, termoFinal));
            sq.select(a.get("id")).where(filtros.toArray(Predicate[]::new));
            return cb.and(cb.isNotNull(empregado.get("pessoaCanonica")), cb.exists(sq));
        };
        Specification<VinculoContratual> selecaoVinculos = (v, query, cb) -> {
            if (ids.isEmpty()) return cb.disjunction();
            Subquery<Long> sq = query.subquery(Long.class);
            Root<AlocacaoVinculo> a = sq.from(AlocacaoVinculo.class);
            List<Predicate> filtros = faixaNoDia(a, cb, data, lotacaoId, jornadaId, gestaoId);
            filtros.add(cb.equal(a.get("vinculo"), v));
            sq.select(a.get("id")).where(filtros.toArray(Predicate[]::new));
            Predicate resultado = cb.and(cb.isNotNull(v.get("pessoa")), v.get("contrato").get("id").in(ids), cb.exists(sq));
            return termoFinal == null ? resultado : cb.and(resultado, buscaPessoaVinculo(v, cb, termoFinal));
        };
        return new Consulta(empregados, selecaoVinculos, ids, data, conhecimento, lotacaoId, jornadaId, gestaoId);
    }

    private List<Predicate> faixaNoDia(Path<?> a, CriteriaBuilder cb, LocalDate data,
                                       Long lotacaoId, Long jornadaId, Long gestaoId) {
        var v = a.get("vinculo");
        return new ArrayList<>(List.of(
                cb.equal(a.get("lotacao").get("id"), lotacaoId),
                cb.equal(a.get("jornada").get("id"), jornadaId),
                cb.equal(a.get("lotacao").get("gestaoLocal").get("id"), gestaoId),
                cb.equal(a.get("jornada").get("gestaoLocal").get("id"), gestaoId),
                cb.equal(a.get("cargo").get("gestaoLocal").get("id"), gestaoId),
                cb.equal(a.get("estadoNegocial"), EstadoNegocialAlocacao.REGISTRADA),
                cb.equal(v.get("estadoNegocial"), EstadoNegocialVinculo.REGISTRADO),
                cb.lessThanOrEqualTo(a.get("dataInicio"), data),
                cb.or(cb.isNull(a.get("dataFim")), cb.greaterThanOrEqualTo(a.get("dataFim"), data)),
                cb.lessThanOrEqualTo(v.get("dataInicio"), data),
                cb.or(cb.isNull(v.get("dataFim")), cb.greaterThanOrEqualTo(v.get("dataFim"), data))));
    }

    private Predicate buscaPessoaVinculo(Path<?> v, CriteriaBuilder cb, String termo) {
        List<Predicate> partesNome = new ArrayList<>();
        for (String parte : termo.split(" ")) partesNome.add(cb.like(cb.lower(v.get("pessoa").get("nome")), "%" + parte + "%"));
        List<Predicate> termos = new ArrayList<>(List.of(cb.and(partesNome.toArray(Predicate[]::new)),
                cb.like(v.get("pessoa").get("cpf"), "%" + termo + "%"), cb.like(v.get("matricula"), "%" + termo + "%")));
        String digitos = termo.replaceAll("\\D", "");
        if (digitos.length() >= 4) termos.add(cb.like(v.get("pessoa").get("cpf"), "%" + digitos + "%"));
        return cb.or(termos.toArray(Predicate[]::new));
    }

    public Page<EmpregadoFrequenciaDTO> projetar(Page<EmpregadoFrequenciaDTO> pagina, Consulta consulta) {
        if (pagina.isEmpty() || consulta.contratos().isEmpty()) return pagina;
        List<Long> empregadoIds = pagina.getContent().stream().map(EmpregadoFrequenciaDTO::id).toList();
        List<Object[]> contextos = alocacoes.buscarContextosDaGradeCanonica(empregadoIds, consulta.contratos(),
                consulta.data(), consulta.lotacaoId(), consulta.jornadaId());
        if (contextos.isEmpty()) throw contextoAlterado();
        Set<Long> vinculoIds = contextos.stream().map(linha -> ((AlocacaoVinculo) linha[1]).getVinculo().getId()).collect(Collectors.toSet());
        Map<Long, List<EventoVinculoContratual>> historicos = eventos.findByVinculoIdInOrderByRegistradoEmAscIdAsc(vinculoIds)
                .stream().filter(e -> e.getRegistradoEm() != null && !e.getRegistradoEm().isAfter(consulta.conhecidoAte()))
                .collect(Collectors.groupingBy(e -> e.getVinculo().getId()));
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
        return pagina.map(dto -> {
            EmpregadoFrequenciaDTO projetado = canonicos.get(dto.id());
            if (projetado == null) throw contextoAlterado();
            return projetado;
        });
    }

    private String rotuloFotografado(String fotografia) {
        return fotografia == null || fotografia.isBlank() ? null : fotografia;
    }
    private ResponseStatusException contextoAlterado() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "O contexto da frequência mudou durante a consulta. Refaça a seleção.");
    }
}
