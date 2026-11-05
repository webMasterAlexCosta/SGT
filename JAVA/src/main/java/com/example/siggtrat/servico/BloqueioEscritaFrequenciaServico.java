package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.VinculoContratual;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Protocolo compartilhado das escritas de frequência: Contrato → Pessoa →
 * Vínculo → Empregado → Frequência. Todos os IDs de cada nível são adquiridos
 * em ordem crescente, antes de resolver a posição temporal ou alterar projeções.
 * A transação do comando chamador usa READ_COMMITTED e retém os locks até commit.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BloqueioEscritaFrequenciaServico {
    private final EntityManager em;

    public BloqueioEscritaFrequenciaServico(EntityManager em) { this.em = em; }

    public void bloquearVinculos(Collection<Long> vinculoIds) {
        Set<Long> ids = ids(vinculoIds);
        if (ids.isEmpty()) return;
        List<IdentidadeVinculo> identidades = identidades("v.id in :ids", ids);
        if (identidades.size() != ids.size()) throw entrada("Vínculo Contratual não encontrado.");
        Set<Long> pessoas = pessoas(identidades);
        bloquearContratos(identidades);
        pessoas.forEach(id -> bloquear(Pessoa.class, id));
        identidades.stream().map(IdentidadeVinculo::id).sorted().forEach(id -> bloquear(VinculoContratual.class, id));
        // A mesma Pessoa pode ainda não ter projeção. O lock da raiz impede duas
        // criações concorrentes; projeções existentes são travadas antes do uso.
        em.createQuery("""
                select distinct e.id from Empregado e, Pessoa p
                where p.id in :ids and (e.pessoaCanonica = p or (e.pessoaCanonica is null and e.cpf = p.cpf))
                order by e.id
                """, Long.class).setParameter("ids", pessoas).getResultList()
                .forEach(id -> bloquear(Empregado.class, id));
    }

    public Map<Long, Empregado> bloquearEmpregados(Collection<Long> empregadoIds) {
        Set<Long> ids = ids(empregadoIds);
        if (ids.isEmpty()) return Map.of();
        Map<Long, Long> pessoasPorEmpregado = new LinkedHashMap<>();
        List<Object[]> entradas = em.createQuery("""
                select e.id, p.id, e.cpf from Empregado e left join e.pessoaCanonica p
                where e.id in :ids order by e.id
                """, Object[].class).setParameter("ids", ids).getResultList();
        if (entradas.size() != ids.size()) throw entrada("Um ou mais colaboradores não foram encontrados.");
        for (Object[] entrada : entradas) {
            Long pessoaId = (Long) entrada[1];
            if (pessoaId == null) {
                String cpf = entrada[2] == null ? "" : entrada[2].toString().replaceAll("\\D", "");
                pessoaId = em.createQuery("select p.id from Pessoa p where p.cpf = :cpf", Long.class)
                        .setParameter("cpf", cpf).getResultStream().findFirst()
                        .orElseThrow(() -> conflito("Pessoa canônica não encontrada para o colaborador."));
            }
            pessoasPorEmpregado.put((Long) entrada[0], pessoaId);
        }
        Set<Long> pessoas = new TreeSet<>(pessoasPorEmpregado.values());
        List<IdentidadeVinculo> anteriores = identidades("v.pessoa.id in :ids", pessoas);
        bloquearContratos(anteriores);
        pessoas.forEach(id -> bloquear(Pessoa.class, id));
        // Uma recontratação pode ter terminado entre a descoberta e o lock da
        // Pessoa. Não adquirir um novo Contrato depois da Pessoa: o cliente
        // refaz o comando e todos os locks voltam a ser ordenados desde a raiz.
        List<IdentidadeVinculo> atuais = identidades("v.pessoa.id in :ids", pessoas);
        if (!anteriores.equals(atuais)) throw conflito(
                "Os Vínculos da Pessoa foram alterados durante a operação. Refaça a solicitação.");
        atuais.stream().map(IdentidadeVinculo::id).sorted().forEach(id -> bloquear(VinculoContratual.class, id));
        Map<Long, Empregado> resultado = new LinkedHashMap<>();
        for (Long id : ids) {
            Empregado empregado = bloquear(Empregado.class, id);
            Long pessoaId = pessoasPorEmpregado.get(id);
            if (empregado.getPessoaCanonica() != null
                    && !Objects.equals(pessoaId, empregado.getPessoaCanonica().getId())) {
                throw conflito("A identidade do colaborador mudou durante a operação. Refaça a solicitação.");
            }
            if (empregado.getPessoaCanonica() == null
                    && !Objects.equals(empregado.getCpf(), em.find(Pessoa.class, pessoaId).getCpf())) {
                throw conflito("O CPF de compatibilidade mudou durante a operação. Refaça a solicitação.");
            }
            resultado.put(id, empregado);
        }
        return resultado;
    }

    public Frequencia bloquearFrequencia(Long id) { return bloquear(Frequencia.class, id); }

    private void bloquearContratos(List<IdentidadeVinculo> identidades) {
        identidades.stream().map(IdentidadeVinculo::contratoId).distinct().sorted()
                .forEach(id -> bloquear(Contrato.class, id));
    }

    private List<IdentidadeVinculo> identidades(String filtro, Set<Long> ids) {
        return em.createQuery("select v.id, v.contrato.id, v.pessoa.id from VinculoContratual v where "
                        + filtro + " order by v.id", Object[].class).setParameter("ids", ids).getResultList().stream()
                .map(r -> new IdentidadeVinculo((Long) r[0], (Long) r[1], (Long) r[2])).toList();
    }

    private Set<Long> pessoas(List<IdentidadeVinculo> identidades) {
        Set<Long> pessoas = new TreeSet<>();
        identidades.forEach(i -> pessoas.add(i.pessoaId()));
        return pessoas;
    }

    private <T> T bloquear(Class<T> tipo, Long id) {
        T entidade = em.find(tipo, id);
        if (entidade == null) throw conflito("Uma entidade do contexto deixou de estar disponível. Refaça a solicitação.");
        if (em.getLockMode(entidade) != LockModeType.PESSIMISTIC_WRITE) {
            // refresh é necessário quando uma leitura anterior já anexou a
            // entidade: obter o lock sozinho não renova o estado do primeiro nível.
            em.refresh(entidade, LockModeType.PESSIMISTIC_WRITE);
        }
        return entidade;
    }

    private Set<Long> ids(Collection<Long> valores) {
        if (valores == null || valores.stream().anyMatch(id -> id == null || id <= 0)) {
            throw entrada("Identificadores do contexto de frequência inválidos.");
        }
        return new TreeSet<>(valores);
    }
    private ResponseStatusException entrada(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem); }
    private ResponseStatusException conflito(String mensagem) { return new ResponseStatusException(HttpStatus.CONFLICT, mensagem); }
    private record IdentidadeVinculo(Long id, Long contratoId, Long pessoaId) { }
}
