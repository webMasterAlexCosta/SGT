package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Dependências escalares em páginas limitadas, resumidas sem perder a última linha. */
@Service
@Transactional(readOnly = true)
public class AssinaturaImpactoVinculoServico {
    private static final int TAMANHO_PAGINA = 200;
    private static final String VERSAO_ASSINATURA = "dependencias-v2";
    private final EntityManager em;

    public AssinaturaImpactoVinculoServico(EntityManager em) { this.em = em; }

    public List<String> partesPorVinculo(VinculoContratual vinculo, LocalDate desde, LocalDate ate) {
        return partes(vinculo.getContrato().getId(), vinculo.getId(), desde, ate);
    }

    /** Consumida também pela prévia de operações que atingem todo o contrato. */
    public List<String> partesPorContrato(Long contratoId, LocalDate desde, LocalDate ate) {
        return partes(contratoId, null, desde, ate);
    }

    /** Valores que a publicação vai congelar, distintos da fotografia ainda em preparação. */
    public List<String> partesDaConfiguracaoPublicacao(Long contratoId) {
        List<String> partes = new ArrayList<>();
        adicionar(partes, "configuracao-publicacao", em.createQuery("""
                select c.id,c.lotacao.nome,c.lotacao.municipio,c.jornada.descricao,c.cargo.nome
                from ComposicaoContratual c where c.contrato.id=:contrato and c.id>:apos order by c.id
                """, Object[].class).setParameter("contrato", contratoId));
        // A mesma Jornada pode ocorrer em várias composições. A subconsulta
        // mantém cada dia uma única vez e permite paginação pela sua identidade.
        adicionar(partes, "grade-publicacao", em.createQuery("""
                select d.id,d.jornada.id,d.diaSemana,d.diaTrabalho,d.entrada1,d.saida1,d.entrada2,d.saida2
                from JornadaDiaSemana d where d.jornada.id in
                    (select c.jornada.id from ComposicaoContratual c where c.contrato.id=:contrato)
                  and d.id>:apos order by d.id
                """, Object[].class).setParameter("contrato", contratoId));
        return List.copyOf(partes);
    }

    private List<String> partes(Long contratoId, Long vinculoId, LocalDate desde, LocalDate ate) {
        Contrato contrato = em.find(Contrato.class, contratoId);
        if (contrato == null) return List.of(VERSAO_ASSINATURA, "contrato-ausente:" + contratoId);
        LocalDate inicio = desde == null ? LocalDate.of(1000, 1, 1) : desde;
        LocalDate fim = ate == null ? LocalDate.of(9999, 12, 31) : ate;
        List<String> partes = new ArrayList<>();
        partes.add(VERSAO_ASSINATURA);
        partes.add("contrato:" + contrato.getId() + ":" + contrato.getVersion() + ":"
                + contrato.getEstadoNegocial() + ":" + contrato.getAtivo() + ":"
                + contrato.getDataInicio() + ":" + contrato.getDataFim());
        adicionar(partes, "evento-contrato", em.createQuery("""
                select e.id,e.tipoEvento,e.dataEfetiva,e.versaoNegocial,e.situacaoFato
                from EventoCicloVidaContrato e where e.contrato.id=:contrato and e.id>:apos order by e.id
                """, Object[].class).setParameter("contrato", contratoId));
        adicionar(partes, "composicao", em.createQuery("""
                select c.id,c.version,c.efetivoPrevisto,c.dataInicioVigencia,c.dataFimVigencia,
                       c.lotacao.id,c.jornada.id,c.cargo.id
                from ComposicaoContratual c where c.contrato.id=:contrato and c.id>:apos order by c.id
                """, Object[].class).setParameter("contrato", contratoId));
        String filtroVinculo = vinculoId == null ? "" : " and v.id=:vinculo";
        TypedQuery<Object[]> vinculos = em.createQuery("""
                select v.id,v.version,v.dataInicio,v.dataFim,v.estadoNegocial,v.matricula
                from VinculoContratual v where v.contrato.id=:contrato and v.id>:apos
                """ + filtroVinculo + " order by v.id", Object[].class).setParameter("contrato", contratoId);
        if (vinculoId != null) vinculos.setParameter("vinculo", vinculoId);
        adicionar(partes, "vinculo", vinculos);
        String filtroRelacionado = vinculoId == null ? "" : " and a.vinculo.id=:vinculo";
        TypedQuery<Object[]> alocacoes = em.createQuery("""
                select a.id,a.version,a.dataInicio,a.dataFim,a.estadoNegocial,a.lotacao.id,a.jornada.id,a.cargo.id
                from AlocacaoVinculo a where a.vinculo.contrato.id=:contrato and a.id>:apos
                """ + filtroRelacionado + " order by a.id", Object[].class).setParameter("contrato", contratoId);
        if (vinculoId != null) alocacoes.setParameter("vinculo", vinculoId);
        adicionar(partes, "alocacao", alocacoes);
        TypedQuery<Object[]> eventosAlocacao = em.createQuery("""
                select a.id,a.hashEvento from EventoAlocacaoVinculo a
                where a.vinculo.contrato.id=:contrato and a.id>:apos
                """ + filtroRelacionado + " order by a.id", Object[].class).setParameter("contrato", contratoId);
        if (vinculoId != null) eventosAlocacao.setParameter("vinculo", vinculoId);
        adicionar(partes, "evento-alocacao", eventosAlocacao);
        TypedQuery<Object[]> frequencias = em.createQuery("""
                select f.id,f.version,f.versaoNegocial,f.dataReferencia,f.estadoNegocial,
                       f.estadoAuditoria,f.validadeOperacional,f.bloqueadaEdicaoOperacional
                from Frequencia f left join f.vinculoContratual vf
                where (f.contratoSnapshotId=:contrato or vf.contrato.id=:contrato)
                  and f.dataReferencia between :desde and :ate
                  and f.id>:apos
                """ + (vinculoId == null ? "" : " and vf.id=:vinculo") + " order by f.id",
                Object[].class).setParameter("contrato", contratoId).setParameter("desde", inicio).setParameter("ate", fim);
        if (vinculoId != null) frequencias.setParameter("vinculo", vinculoId);
        adicionar(partes, "frequencia", frequencias);
        TypedQuery<Object[]> pendencias = em.createQuery("""
                select p.id,p.version,p.estado,p.tipo,p.escopoBloqueio,p.dataEfetiva
                from PendenciaAdministrativa p where p.contratoId=:contrato
                  and (p.dataEfetiva is null or p.dataEfetiva between :desde and :ate)
                  and p.id>:apos
                """ + (vinculoId == null ? "" : " and (p.vinculoId is null or p.vinculoId=:vinculo)") + " order by p.id",
                Object[].class).setParameter("contrato", contratoId).setParameter("desde", inicio).setParameter("ate", fim);
        if (vinculoId != null) pendencias.setParameter("vinculo", vinculoId);
        adicionar(partes, "pendencia", pendencias);
        Long gestao = contrato.getGestaoLocal() == null ? null : contrato.getGestaoLocal().getId();
        Long estado = contrato.getGestaoLocal() == null || contrato.getGestaoLocal().getSuperintendencia() == null
                ? null : contrato.getGestaoLocal().getSuperintendencia().getId();
        adicionar(partes, "emissao", em.createQuery("""
                select e.id,e.version,e.estado,e.numeroVersao,e.hashConteudo
                from EmissaoRelatorioAdministrativo e
                where e.inicio<=:ate and e.fim>=:desde
                  and (e.gestaoLocalEscopoId is null or e.gestaoLocalEscopoId=:gestao)
                  and (e.superintendenciaEscopoId is null or e.superintendenciaEscopoId=:estado)
                  and (e.contratoIdsCsv is null or e.contratoIdsCsv=''
                       or concat(',',e.contratoIdsCsv,',') like :contratos)
                  and e.id>:apos
                order by e.id
                """, Object[].class).setParameter("desde", inicio).setParameter("ate", fim)
                .setParameter("gestao", gestao).setParameter("estado", estado)
                .setParameter("contratos", "%," + contratoId + ",%"));
        return List.copyOf(partes);
    }

    /** Somente a página escalar atual fica em memória; nenhuma entidade/lock é descartado do contexto. */
    private void adicionar(List<String> destino, String tipo, TypedQuery<Object[]> consulta) {
        MessageDigest digest = novoDigest();
        long ultimoId = 0;
        long quantidade = 0;
        consulta.setMaxResults(TAMANHO_PAGINA);
        while (true) {
            List<Object[]> pagina = consulta.setParameter("apos", ultimoId).getResultList();
            if (pagina.isEmpty()) break;
            for (Object[] linha : pagina) {
                long id = ((Number) linha[0]).longValue();
                if (id <= ultimoId) throw new IllegalStateException("A assinatura exige dependências ordenadas por ID único.");
                for (Object valor : linha) adicionarCampo(digest, valor);
                digest.update((byte) 0x7f);
                ultimoId = id;
                quantidade++;
            }
            if (pagina.size() < TAMANHO_PAGINA) break;
        }
        destino.add(tipo + ":" + quantidade + ":" + HexFormat.of().formatHex(digest.digest()));
    }

    private void adicionarCampo(MessageDigest digest, Object valor) {
        if (valor == null) { digest.update((byte) 0); return; }
        digest.update((byte) 1);
        byte[] bytes = AssinaturaPreviaNegocial.valor(valor).getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private MessageDigest novoDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível.", e); }
    }
}
