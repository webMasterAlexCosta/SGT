package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.entidade.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

/**
 * A elegibilidade deriva das raízes e eventos canônicos, nunca do contrato,
 * status ou dimensões atuais da tabela operacional. Cada página usa sua própria
 * transação para limitar a persistência de entidades durante fechamentos grandes.
 */
@Service
public class ElegibilidadeFechamentoFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ElegibilidadeFechamentoFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final int PAGINA = 200;
    private static final int PROJECOES_POR_CONTEXTO = 25;
    @PersistenceContext private EntityManager em;
    private final MotorTemporalContratoServico contratos;
    private final MotorTemporalVinculoServico vinculos;
    private final MotorTemporalAlocacaoServico alocacoes;
    private final ResolvedorContextoTemporalFrequenciaServico resolvedor;
    private final ProjecaoOperacionalPessoaServico projecoes;
    private final BloqueioEscritaFrequenciaServico bloqueioEscrita;
    private final TransactionTemplate leitura;

    public ElegibilidadeFechamentoFrequenciaServico(MotorTemporalContratoServico contratos,
            MotorTemporalVinculoServico vinculos, MotorTemporalAlocacaoServico alocacoes,
            ResolvedorContextoTemporalFrequenciaServico resolvedor,
            ProjecaoOperacionalPessoaServico projecoes, PlatformTransactionManager transacoes,
            BloqueioEscritaFrequenciaServico bloqueioEscrita) {
        this.contratos = contratos; this.vinculos = vinculos; this.alocacoes = alocacoes;
        this.resolvedor = resolvedor; this.projecoes = projecoes;
        this.bloqueioEscrita = bloqueioEscrita;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.leitura.setReadOnly(true);
    }

    public boolean existe(LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds) {
        return contarVinculos(data, lotacao, jornada, contratosIds, false, Set.of(), true) > 0;
    }
    public long contarSemFrequencia(LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds) {
        return contarVinculos(data, lotacao, jornada, contratosIds, true, Set.of(), false);
    }
    public boolean existeImplicitoSemIntencao(LocalDate data, Long lotacao, Long jornada,
                                             List<Long> contratosIds, Set<Long> empregadosExplicitos) {
        return contarVinculos(data, lotacao, jornada, contratosIds, true, empregadosExplicitos, true) > 0;
    }

    private long contarVinculos(LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds,
                                boolean semFrequencia, Set<Long> excluidos, boolean primeiro) {
        long cursor = 0, total = 0;
        while (true) {
            long apos = cursor;
            ResumoPagina pagina = ler(() -> {
                List<Long> ids = candidatosVinculo(lotacao, jornada, contratosIds, apos, PAGINA);
                long quantidade = 0;
                for (Long id : ids) {
                    var contexto = contextoElegivel(id, data, lotacao, jornada, contratosIds);
                    if (contexto == null) continue;
                    Pessoa pessoa = contexto.pessoa();
                    if (possuiColisaoDeIdentidade(pessoa)) continue;
                    Long empregadoId = empregadoId(pessoa);
                    if (empregadoId != null && excluidos.contains(empregadoId)) continue;
                    if (semFrequencia && possuiFrequencia(pessoa, data)) continue;
                    quantidade++;
                    if (primeiro) break;
                }
                return new ResumoPagina(ids.isEmpty() ? apos : ids.get(ids.size() - 1), ids.size() < PAGINA, quantidade);
            });
            total += pagina.quantidade();
            if (primeiro && total > 0 || pagina.ultima()) return total;
            cursor = pagina.ultimoId();
        }
    }

    /** Materializa somente projeções internas; não cria fato negocial nem frequência. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public long prepararProjecoesApos(LocalDate data, Long lotacao, Long jornada,
                                      List<Long> contratosIds, long apos, int tamanho) {
        List<Long> ids = candidatosVinculo(lotacao, jornada, contratosIds, apos, Math.min(Math.max(tamanho, 1), PAGINA));
        bloqueioEscrita.bloquearVinculos(ids);
        // Os locks de todas as raízes continuam no PostgreSQL até o fim desta
        // transação. Desanexar suas entidades evita varrê-las em cada AUTO flush.
        em.flush();
        em.clear();
        int avaliados = 0;
        for (Long id : ids) {
            var contexto = contextoElegivel(id, data, lotacao, jornada, contratosIds);
            if (contexto != null && !possuiColisaoDeIdentidade(contexto.pessoa()))
                projecoes.projetar(contexto.vinculo(), contexto.alocacao());
            if (++avaliados % PROJECOES_POR_CONTEXTO == 0) {
                // Sincronizar não é commit: falha posterior ainda reverte o lote
                // inteiro, sem reduzir o chunk transacional ou liberar os locks.
                em.flush();
                em.clear();
            }
        }
        return ids.isEmpty() ? 0 : ids.get(ids.size() - 1);
    }

    public long contarExplicitos(Collection<Long> ids, LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds) {
        return ler(() -> ids.stream().filter(id -> empregadoElegivel(id, data, lotacao, jornada, contratosIds)).count());
    }

    public long contarPendentes(Long fechamentoId, LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds) {
        long cursor = 0, total = 0;
        while (true) {
            List<Long> pagina = buscarPendentesApos(fechamentoId, data, lotacao, jornada, contratosIds, cursor, PAGINA);
            if (pagina.isEmpty()) return total;
            total += pagina.size(); cursor = pagina.get(pagina.size() - 1);
        }
    }

    public List<Long> buscarPendentesApos(Long fechamentoId, LocalDate data, Long lotacao, Long jornada,
                                         List<Long> contratosIds, long apos, int tamanho) {
        int limite = Math.min(Math.max(tamanho, 1), 1000);
        List<Long> resultado = new ArrayList<>();
        long cursor = apos;
        while (resultado.size() < limite) {
            long inicio = cursor;
            int restantes = limite - resultado.size();
            PaginaEmpregados pagina = ler(() -> {
                var query = em.createQuery("""
                    select e.id from Empregado e
                    where e.id > :apos and e.pessoaCanonica is not null
                      and exists (select a.id from AlocacaoVinculo a
                        where a.vinculo.pessoa = e.pessoaCanonica and a.lotacao.id = :lotacao
                          and a.jornada.id = :jornada)
                      and (not exists (select f.id from Frequencia f where f.empregado=e and f.dataReferencia=:data)
                        or exists (select i.id from FechamentoFrequenciaItem i where i.empregado=e and i.fechamento.id=:fechamento))
                    order by e.id
                    """, Long.class).setParameter("apos", inicio).setParameter("lotacao", lotacao)
                        .setParameter("jornada", jornada).setParameter("data", data).setParameter("fechamento", fechamentoId)
                        .setMaxResults(PAGINA);
                List<Long> candidatos = query.getResultList();
                // A cauda ainda não devolvida será reavaliada pela próxima busca
                // a partir do último empregado efetivamente processado. Mantém
                // a validação completa dos vínculos de cada Pessoa selecionada.
                List<Long> elegiveis = candidatos.stream()
                        .filter(id -> empregadoElegivel(id, data, lotacao, jornada, contratosIds))
                        .limit(restantes).toList();
                return new PaginaEmpregados(candidatos.isEmpty() ? inicio : candidatos.get(candidatos.size() - 1),
                        candidatos.size() < PAGINA, elegiveis);
            });
            for (Long id : pagina.ids()) {
                resultado.add(id);
                if (resultado.size() == limite) break;
            }
            if (pagina.ultima()) break;
            cursor = pagina.ultimoId();
        }
        return List.copyOf(resultado);
    }

    private List<Long> candidatosVinculo(Long lotacao, Long jornada, List<Long> contratosIds, long apos, int limite) {
        String filtro = contratosIds.isEmpty() ? "" : " and v.contrato.id in :contratos";
        var query = em.createQuery("""
            select v.id from VinculoContratual v where v.id > :apos
              and exists (select a.id from AlocacaoVinculo a where a.vinculo=v
                and a.lotacao.id=:lotacao and a.jornada.id=:jornada)
            """ + filtro + " order by v.id", Long.class)
                .setParameter("apos", apos).setParameter("lotacao", lotacao).setParameter("jornada", jornada)
                .setMaxResults(limite);
        if (!contratosIds.isEmpty()) query.setParameter("contratos", contratosIds);
        return query.getResultList();
    }

    private ResolvedorContextoTemporalFrequenciaServico.Contexto contextoElegivel(Long id, LocalDate data,
                                    Long lotacao, Long jornada, List<Long> contratosIds) {
        VinculoContratual v = em.find(VinculoContratual.class, id);
        if (v == null || v.getContrato() == null || !contratosIds.isEmpty() && !contratosIds.contains(v.getContrato().getId())) return null;
        Instant conhecimento = Instant.now();
        var pv = vinculos.reconstruir(v, data, conhecimento);
        if (pv.situacaoTemporal() != SituacaoTemporalVinculo.VIGENTE) return null;
        var pc = contratos.reconstruirPosicao(v.getContrato(), data, conhecimento);
        if (pc.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || pc.situacaoTemporal() != SituacaoTemporalContrato.VIGENTE
                || pc.habilitacaoAdministrativa() != HabilitacaoAdministrativaContrato.ATIVO) return null;
        var pa = alocacoes.reconstruir(v, data, conhecimento);
        if (pa.situacaoTemporal() != SituacaoTemporalAlocacao.VIGENTE
                || !Objects.equals(lotacao, pa.lotacaoId()) || !Objects.equals(jornada, pa.jornadaId())) return null;
        // Valida também a responsabilidade histórica do contrato e a coerência da combinação.
        return resolvedor.resolverPorVinculo(id, data);
    }

    private boolean empregadoElegivel(Long id, LocalDate data, Long lotacao, Long jornada, List<Long> contratosIds) {
        Empregado e = em.find(Empregado.class, id);
        if (e == null) return false;
        Pessoa pessoa = e.getPessoaCanonica();
        if (pessoa == null || possuiColisaoDeIdentidade(pessoa)) return false;
        List<Long> ids = em.createQuery("select v.id from VinculoContratual v where v.pessoa.id=:p", Long.class)
                .setParameter("p", pessoa.getId()).getResultList();
        long elegiveis = ids.stream().filter(v -> contextoElegivel(v, data, lotacao, jornada, contratosIds) != null).count();
        if (elegiveis > 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Mais de um Vínculo operacional foi encontrado para a Pessoa na data do fechamento.");
        return elegiveis == 1;
    }

    /** CPF só identifica uma colisão impeditiva; nunca comprova associação canônica. */
    private boolean possuiColisaoDeIdentidade(Pessoa pessoa) {
        return !em.createQuery("""
            select e.id from Empregado e left join e.pessoaCanonica canonica
             where e.cpf=:cpf and (canonica.id is null or canonica.id<>:p)
            """, Long.class).setParameter("cpf", pessoa.getCpf()).setParameter("p", pessoa.getId())
                .setMaxResults(1).getResultList().isEmpty();
    }

    private Long empregadoId(Pessoa pessoa) {
        return em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoa.getId())
                .getResultStream().findFirst().orElse(null);
    }
    private boolean possuiFrequencia(Pessoa pessoa, LocalDate data) {
        return !em.createQuery("""
            select f.id from Frequencia f where f.dataReferencia=:data
              and (f.pessoa.id=:p or f.empregado.pessoaCanonica.id=:p)
            """, Long.class).setParameter("data", data).setParameter("p", pessoa.getId())
                .setMaxResults(1).getResultList().isEmpty();
    }
    private <T> T ler(Supplier<T> acao) { return leitura.execute(status -> acao.get()); }
    private record ResumoPagina(long ultimoId, boolean ultima, long quantidade) {}
    private record PaginaEmpregados(long ultimoId, boolean ultima, List<Long> ids) {}
}
