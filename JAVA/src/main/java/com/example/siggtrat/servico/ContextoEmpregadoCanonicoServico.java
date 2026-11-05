package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Contexto corrente da API legada; as entidades operacionais nunca são regravadas pela consulta. */
@Service
@Transactional(readOnly = true)
public class ContextoEmpregadoCanonicoServico {
    public record Contexto(Pessoa pessoa, VinculoContratual vinculo, AlocacaoVinculo alocacao,
                           GestaoLocal gestao, String matricula, LocalDate data) {
        public Lotacao lotacao() { return alocacao == null ? null : alocacao.getLotacao(); }
        public Jornada jornada() { return alocacao == null ? null : alocacao.getJornada(); }
        public Cargo cargo() { return alocacao == null ? null : alocacao.getCargo(); }
        public String lotacaoNome() {
            return alocacao == null ? null : alocacao.getLotacaoNomeFotografia() != null
                    ? alocacao.getLotacaoNomeFotografia() : lotacao() == null ? null : lotacao().getNome();
        }
        public String jornadaDescricao() {
            return alocacao == null ? null : alocacao.getJornadaDescricaoFotografia() != null
                    ? alocacao.getJornadaDescricaoFotografia() : jornada() == null ? null : jornada().getDescricao();
        }
        public String cargoNome() {
            return alocacao == null ? null : alocacao.getCargoNomeFotografia() != null
                    ? alocacao.getCargoNomeFotografia() : cargo() == null ? null : cargo().getNome();
        }
        public boolean faixaVigente() {
            return vinculo.getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO && alocacao != null
                    && alocacao.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA
                    && !vinculo.getDataInicio().isAfter(data) && (vinculo.getDataFim() == null || !vinculo.getDataFim().isBefore(data))
                    && !alocacao.getDataInicio().isAfter(data) && (alocacao.getDataFim() == null || !alocacao.getDataFim().isBefore(data));
        }
    }

    public record Filtros(String busca, String status, Long lotacaoId, Long cargoId, Long jornadaId,
                           Set<Long> contratosSolicitados, boolean filtrarContratos) {}

    private final EmpregadoRepositorio empregados;
    private final VinculoContratualRepositorio vinculos;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final EventoVinculoContratualRepositorio eventos;
    private final MotorTemporalVinculoServico motorVinculo;
    private final GestaoContratoTemporalServico gestoes;

    public ContextoEmpregadoCanonicoServico(EmpregadoRepositorio empregados, VinculoContratualRepositorio vinculos,
            AlocacaoVinculoRepositorio alocacoes, EventoVinculoContratualRepositorio eventos,
            MotorTemporalVinculoServico motorVinculo, GestaoContratoTemporalServico gestoes) {
        this.empregados = empregados; this.vinculos = vinculos; this.alocacoes = alocacoes;
        this.eventos = eventos; this.motorVinculo = motorVinculo; this.gestoes = gestoes;
    }

    public Contexto resolver(Empregado empregado) {
        if (empregado == null || empregado.getPessoaCanonica() == null) return null;
        Long pessoaId = empregado.getPessoaCanonica().getId();
        if (pessoaId == null) return null;
        return resolverPessoas(Set.of(pessoaId), RelogioSistema.hoje()).get(pessoaId);
    }

    /** Carga em lote para a página: identidade, vínculos, faixas e eventos. */
    public Map<Long, Contexto> resolverEmpregados(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var identidades = empregados.buscarIdentidadesCanonicas(ids);
        if (identidades.isEmpty()) return Map.of();
        Set<Long> pessoaIds = identidades.stream().map(EmpregadoRepositorio.IdentidadeCanonica::getPessoaId).collect(Collectors.toSet());
        Map<Long, Contexto> porPessoa = resolverPessoas(pessoaIds, RelogioSistema.hoje());
        Map<Long, Contexto> resultado = new HashMap<>();
        for (var identidade : identidades) {
            Contexto contexto = porPessoa.get(identidade.getPessoaId());
            if (contexto != null) resultado.put(identidade.getEmpregadoId(), contexto);
        }
        return Map.copyOf(resultado);
    }

    private Map<Long, Contexto> resolverPessoas(Set<Long> pessoaIds, LocalDate data) {
        Map<Long, VinculoContratual> escolhidos = new LinkedHashMap<>();
        for (VinculoContratual v : vinculos.buscarPorPessoasComContexto(pessoaIds)) {
            VinculoContratual atual = escolhidos.get(v.getPessoa().getId());
            if (atual == null || prioritario(v.getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO, v.getDataInicio(), v.getId(),
                    atual.getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO, atual.getDataInicio(), atual.getId(), data))
                escolhidos.put(v.getPessoa().getId(), v);
        }
        if (escolhidos.isEmpty()) return Map.of();
        Set<Long> vinculoIds = escolhidos.values().stream().map(VinculoContratual::getId).collect(Collectors.toSet());
        Map<Long, AlocacaoVinculo> faixas = new HashMap<>();
        for (AlocacaoVinculo a : alocacoes.buscarPorVinculos(vinculoIds)) {
            AlocacaoVinculo atual = faixas.get(a.getVinculo().getId());
            if (atual == null || prioritario(a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA, a.getDataInicio(), a.getId(),
                    atual.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA, atual.getDataInicio(), atual.getId(), data))
                faixas.put(a.getVinculo().getId(), a);
        }
        Map<Long, List<EventoVinculoContratual>> historicos = eventos.findByVinculoIdInOrderByRegistradoEmAscIdAsc(vinculoIds)
                .stream().collect(Collectors.groupingBy(e -> e.getVinculo().getId()));
        Map<Long, GestaoLocal> gestaoPorContrato = new HashMap<>();
        Map<Long, Contexto> resultado = new HashMap<>();
        Instant conhecimento = Instant.now();
        for (VinculoContratual v : escolhidos.values()) {
            GestaoLocal gestao = gestaoPorContrato.computeIfAbsent(v.getContrato().getId(),
                    id -> gestoes.gestaoEm(v.getContrato(), data, conhecimento));
            resultado.put(v.getPessoa().getId(), new Contexto(v.getPessoa(), v, faixas.get(v.getId()), gestao,
                    motorVinculo.matriculaNaData(v, data, historicos.getOrDefault(v.getId(), List.of())), data));
        }
        return Map.copyOf(resultado);
    }

    /** Registrados primeiro; último contexto já iniciado; senão, primeira programação. */
    private boolean prioritario(boolean registrado, LocalDate inicio, Long id,
                                  boolean outroRegistrado, LocalDate outroInicio, Long outroId, LocalDate data) {
        if (registrado != outroRegistrado) return registrado;
        boolean iniciado = !inicio.isAfter(data), outroIniciado = !outroInicio.isAfter(data);
        if (iniciado != outroIniciado) return iniciado;
        int comparacao = inicio.compareTo(outroInicio);
        if (comparacao == 0) comparacao = id.compareTo(outroId);
        return iniciado ? comparacao > 0 : comparacao < 0;
    }

    /** A escolha do contexto participa do WHERE; paginação e contagem não são filtradas em memória. */
    public Specification<Empregado> especificacao(Filtros filtros, Set<Long> contratosPermitidos,
            boolean operacional, List<EscopoAcessoUsuarioServico.Escopo> escopos, LocalDate data) {
        String termo = filtros.busca() == null || filtros.busca().isBlank() ? null
                : filtros.busca().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (termo != null && termo.length() > 80) termo = termo.substring(0, 80);
        String busca = termo;
        return (empregado, query, cb) -> {
            if (contratosPermitidos != null && contratosPermitidos.isEmpty()
                    || filtros.filtrarContratos() && filtros.contratosSolicitados().isEmpty()) return cb.disjunction();
            Subquery<Long> sq = query.subquery(Long.class);
            Root<VinculoContratual> v = sq.from(VinculoContratual.class);
            List<Predicate> partes = new ArrayList<>();
            partes.add(cb.equal(v.get("pessoa").get("id"), empregado.get("pessoaCanonica").get("id")));
            if (contratosPermitidos != null) partes.add(v.get("contrato").get("id").in(contratosPermitidos));
            if (filtros.filtrarContratos()) partes.add(v.get("contrato").get("id").in(filtros.contratosSolicitados()));
            partes.add(semVinculoPrioritario(sq, cb, v, data));
            if (filtros.lotacaoId() != null || filtros.cargoId() != null || filtros.jornadaId() != null || operacional)
                partes.add(existeFaixaEscolhida(sq, cb, v, filtros, operacional, escopos, data));
            if (operacional) {
                partes.add(cb.equal(v.get("estadoNegocial"), EstadoNegocialVinculo.REGISTRADO));
                partes.add(cb.lessThanOrEqualTo(v.get("dataInicio"), data));
                partes.add(cb.or(cb.isNull(v.get("dataFim")), cb.greaterThanOrEqualTo(v.get("dataFim"), data)));
            }
            if (busca != null) {
                List<Predicate> nomes = new ArrayList<>();
                for (String palavra : busca.split(" ")) nomes.add(cb.like(cb.lower(v.get("pessoa").get("nome")), "%" + palavra + "%"));
                List<Predicate> termos = new ArrayList<>(List.of(cb.and(nomes.toArray(Predicate[]::new)),
                        cb.like(v.get("pessoa").get("cpf"), "%" + busca + "%"), cb.like(v.get("matricula"), "%" + busca + "%")));
                String digitos = busca.replaceAll("\\D", "");
                if (digitos.length() >= 4) termos.add(cb.like(v.get("pessoa").get("cpf"), "%" + digitos + "%"));
                partes.add(cb.or(termos.toArray(Predicate[]::new)));
            }
            sq.select(v.get("id")).where(partes.toArray(Predicate[]::new));
            Predicate resultado = cb.and(cb.isNotNull(empregado.get("pessoaCanonica")), cb.exists(sq));
            if (filtros.status() != null && !filtros.status().isBlank())
                resultado = cb.and(resultado, cb.equal(empregado.get("status").get("descricao"), filtros.status().trim()));
            return resultado;
        };
    }

    private Predicate semVinculoPrioritario(Subquery<?> query, CriteriaBuilder cb, Root<VinculoContratual> atual, LocalDate data) {
        Subquery<Long> melhor = query.subquery(Long.class);
        Root<VinculoContratual> outro = melhor.from(VinculoContratual.class);
        melhor.select(outro.get("id")).where(cb.equal(outro.get("pessoa"), atual.get("pessoa")),
                prioridade(cb, outro, atual, EstadoNegocialVinculo.REGISTRADO, data));
        return cb.not(cb.exists(melhor));
    }

    private Predicate existeFaixaEscolhida(Subquery<?> query, CriteriaBuilder cb, Root<VinculoContratual> v,
            Filtros filtros, boolean operacional, List<EscopoAcessoUsuarioServico.Escopo> escopos, LocalDate data) {
        Subquery<Long> sq = query.subquery(Long.class);
        Root<AlocacaoVinculo> a = sq.from(AlocacaoVinculo.class);
        Subquery<Long> melhor = sq.subquery(Long.class);
        Root<AlocacaoVinculo> outra = melhor.from(AlocacaoVinculo.class);
        melhor.select(outra.get("id")).where(cb.equal(outra.get("vinculo"), v),
                prioridade(cb, outra, a, EstadoNegocialAlocacao.REGISTRADA, data));
        List<Predicate> partes = new ArrayList<>(List.of(cb.equal(a.get("vinculo"), v), cb.not(cb.exists(melhor))));
        if (filtros.lotacaoId() != null) partes.add(cb.equal(a.get("lotacao").get("id"), filtros.lotacaoId()));
        if (filtros.cargoId() != null) partes.add(cb.equal(a.get("cargo").get("id"), filtros.cargoId()));
        if (filtros.jornadaId() != null) partes.add(cb.equal(a.get("jornada").get("id"), filtros.jornadaId()));
        if (operacional) {
            partes.add(cb.equal(a.get("estadoNegocial"), EstadoNegocialAlocacao.REGISTRADA));
            partes.add(cb.lessThanOrEqualTo(a.get("dataInicio"), data));
            partes.add(cb.or(cb.isNull(a.get("dataFim")), cb.greaterThanOrEqualTo(a.get("dataFim"), data)));
            List<Predicate> permitidos = new ArrayList<>();
            for (var escopo : escopos) {
                List<Predicate> recorte = new ArrayList<>();
                if (escopo.gestaoLocalId() != null) recorte.add(cb.equal(a.get("lotacao").get("gestaoLocal").get("id"), escopo.gestaoLocalId()));
                if (escopo.lotacaoId() != null) recorte.add(cb.equal(a.get("lotacao").get("id"), escopo.lotacaoId()));
                if (escopo.jornadaId() != null) recorte.add(cb.equal(a.get("jornada").get("id"), escopo.jornadaId()));
                permitidos.add(cb.and(recorte.toArray(Predicate[]::new)));
            }
            partes.add(cb.or(permitidos.toArray(Predicate[]::new)));
        }
        sq.select(a.get("id")).where(partes.toArray(Predicate[]::new));
        return cb.exists(sq);
    }

    /** Mesma ordem de prioritario(), traduzida em predicado para evitar divergência lista/detalhe. */
    private Predicate prioridade(CriteriaBuilder cb, Path<?> outro, Path<?> atual, Object estadoRegistrado, LocalDate data) {
        Predicate registrado = cb.equal(outro.get("estadoNegocial"), estadoRegistrado);
        Predicate atualRegistrado = cb.equal(atual.get("estadoNegocial"), estadoRegistrado);
        Predicate mesmaClasse = cb.or(cb.and(registrado, atualRegistrado), cb.and(cb.not(registrado), cb.not(atualRegistrado)));
        Predicate iniciado = cb.lessThanOrEqualTo(outro.<LocalDate>get("dataInicio"), data);
        Predicate atualIniciado = cb.lessThanOrEqualTo(atual.<LocalDate>get("dataInicio"), data);
        Predicate depois = cb.or(cb.greaterThan(outro.<LocalDate>get("dataInicio"), atual.<LocalDate>get("dataInicio")),
                cb.and(cb.equal(outro.get("dataInicio"), atual.get("dataInicio")), cb.greaterThan(outro.<Long>get("id"), atual.<Long>get("id"))));
        Predicate antes = cb.or(cb.lessThan(outro.<LocalDate>get("dataInicio"), atual.<LocalDate>get("dataInicio")),
                cb.and(cb.equal(outro.get("dataInicio"), atual.get("dataInicio")), cb.lessThan(outro.<Long>get("id"), atual.<Long>get("id"))));
        return cb.or(cb.and(registrado, cb.not(atualRegistrado)), cb.and(mesmaClasse, cb.or(
                cb.and(iniciado, cb.not(atualIniciado)), cb.and(iniciado, atualIniciado, depois),
                cb.and(cb.not(iniciado), cb.not(atualIniciado), antes))));
    }
}
