package com.example.siggtrat.servico;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.PreviaCicloVidaContratoDTO;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A confirmação é chamada sob o mesmo bloqueio do contrato que protege a mutação. */
@Service
@Transactional(readOnly = true)
public class PreviaCicloVidaContratoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PreviaCicloVidaContratoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final int LIMITE_DETALHES = 200;
    private static final Set<String> OPERACOES = Set.of("inativar", "reativar", "encerrar-antecipadamente",
            "cancelar-futuro", "anular", "corrigir-anulacao", "publicar", "transferir-gestao");
    private static final String VINCULOS = """
            from VinculoContratual v where v.contrato.id=:contrato
              and v.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
              and (v.dataFim is null or v.dataFim>=:desde)
            """;
    private static final String ALOCACOES_FUTURAS = """
            from AlocacaoVinculo a where a.vinculo.contrato.id=:contrato and a.dataInicio>:desde
              and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
            """;
    private static final String EVENTOS_FUTUROS = """
            from EventoCicloVidaContrato e where e.contrato.id=:contrato and e.dataEfetiva>:desde
              and e.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE
            """;
    private static final String FREQUENCIAS = """
            from Frequencia f left join f.vinculoContratual v
            where (f.contratoSnapshotId=:contrato or v.contrato.id=:contrato) and f.dataReferencia>=:desde
            """;
    private final EntityManager em;
    private final AssinaturaImpactoVinculoServico assinaturaImpacto;

    public PreviaCicloVidaContratoServico(EntityManager em, AssinaturaImpactoVinculoServico assinaturaImpacto) {
        this.em = em;
        this.assinaturaImpacto = assinaturaImpacto;
    }

    public PreviaCicloVidaContratoDTO gerar(Contrato contrato, String operacao, AcaoCicloVidaContratoDTO acao,
                                           UsuarioAplicacao usuario) {
        return gerar(contrato, operacao, acao, usuario, List.of());
    }

    /** A mesma assinatura de impactos pode vincular o contexto específico de uma operação. */
    public PreviaCicloVidaContratoDTO gerar(Contrato contrato, String operacao, AcaoCicloVidaContratoDTO acao,
                                           UsuarioAplicacao usuario, List<?> partesDaIntencao) {
        validarDados(operacao, acao);
        LocalDate desde = inicioDoAlcance(contrato, acao);
        long quantidadeVinculos = contar("select count(v) " + VINCULOS, contrato.getId(), desde);
        List<PreviaCicloVidaContratoDTO.VinculoAfetado> vinculos = em.createQuery("""
                select v from VinculoContratual v join fetch v.pessoa
                where v.contrato.id=:contrato
                  and v.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                  and (v.dataFim is null or v.dataFim>=:desde)
                order by v.dataInicio,v.id
                """, VinculoContratual.class).setParameter("contrato", contrato.getId()).setParameter("desde", desde)
                .setMaxResults(LIMITE_DETALHES).getResultList().stream().map(v -> new PreviaCicloVidaContratoDTO.VinculoAfetado(
                        v.getId(), v.getPessoa().getId(), v.getPessoa().getNome(), v.getMatricula(),
                        v.getDataInicio(), v.getDataFim(), v.getDataInicio().isAfter(RelogioSistema.hoje()))).toList();
        List<PreviaCicloVidaContratoDTO.ProgramacaoAfetada> programacoes = new ArrayList<>();
        String vinculosFuturos = VINCULOS + " and v.dataInicio>:desde ";
        long quantidadeProgramacoes = contar("select count(v) " + vinculosFuturos, contrato.getId(), desde)
                + contar("select count(a) " + ALOCACOES_FUTURAS, contrato.getId(), desde)
                + contar("select count(e) " + EVENTOS_FUTUROS, contrato.getId(), desde);
        for (Object[] v : em.createQuery("select v.id,v.dataInicio,v.pessoa.nome " + vinculosFuturos
                        + " order by v.dataInicio,v.id", Object[].class)
                .setParameter("contrato", contrato.getId()).setParameter("desde", desde).setMaxResults(LIMITE_DETALHES).getResultList()) {
            programacoes.add(new PreviaCicloVidaContratoDTO.ProgramacaoAfetada("VINCULO", (Long) v[0], (Long) v[0],
                    (LocalDate) v[1], (String) v[2]));
        }
        for (Object[] a : em.createQuery("select a.id,a.vinculo.id,a.dataInicio,a.lotacao.nome,a.jornada.descricao,a.cargo.nome "
                        + ALOCACOES_FUTURAS + " order by a.dataInicio,a.id", Object[].class)
                .setParameter("contrato", contrato.getId()).setParameter("desde", desde).setMaxResults(LIMITE_DETALHES).getResultList()) {
            programacoes.add(new PreviaCicloVidaContratoDTO.ProgramacaoAfetada("ALOCACAO", (Long) a[0], (Long) a[1],
                    (LocalDate) a[2], a[3] + " / " + a[4] + " / " + a[5]));
        }
        for (Object[] e : em.createQuery("select e.id,e.tipoEvento,e.dataEfetiva,e.motivo " + EVENTOS_FUTUROS
                        + " order by e.dataEfetiva,e.id", Object[].class)
                .setParameter("contrato", contrato.getId()).setParameter("desde", desde).setMaxResults(LIMITE_DETALHES).getResultList()) {
            programacoes.add(new PreviaCicloVidaContratoDTO.ProgramacaoAfetada(String.valueOf(e[1]), (Long) e[0], null,
                    (LocalDate) e[2], (String) e[3]));
        }
        programacoes.sort(Comparator.comparing(PreviaCicloVidaContratoDTO.ProgramacaoAfetada::dataEfetiva)
                .thenComparing(PreviaCicloVidaContratoDTO.ProgramacaoAfetada::tipo).thenComparing(PreviaCicloVidaContratoDTO.ProgramacaoAfetada::id));
        List<PreviaCicloVidaContratoDTO.ProgramacaoAfetada> amostraProgramacoes = programacoes.stream().limit(LIMITE_DETALHES).toList();
        LocalDate frequenciasDesde = "encerrar-antecipadamente".equals(operacao) ? desde.plusDays(1) : desde;
        long quantidadeFrequencias = contar("select count(f) " + FREQUENCIAS, contrato.getId(), frequenciasDesde);
        long quantidadeDatas = contar("select count(distinct f.dataReferencia) " + FREQUENCIAS, contrato.getId(), frequenciasDesde);
        List<LocalDate> datas = em.createQuery("select distinct f.dataReferencia " + FREQUENCIAS + " order by f.dataReferencia", LocalDate.class)
                .setParameter("contrato", contrato.getId()).setParameter("desde", frequenciasDesde).setMaxResults(LIMITE_DETALHES).getResultList();
        List<String> impactos = new ArrayList<>();
        impactos.add("transferir-gestao".equals(operacao)
                ? quantidadeVinculos + " vínculo(s) no alcance da transferência; alocações seguem o mapeamento explícito aprovado."
                : quantidadeVinculos + " vínculo(s) no alcance da operação; nenhum será realocado automaticamente.");
        if (quantidadeProgramacoes > 0) impactos.add(quantidadeProgramacoes + " programação(ões) posterior(es) devem permanecer auditáveis.");
        if (quantidadeFrequencias > 0) impactos.add(quantidadeFrequencias + " frequência(s) serão revalidadas, preservando as versões anteriores.");
        if (quantidadeVinculos > LIMITE_DETALHES || quantidadeProgramacoes > LIMITE_DETALHES || quantidadeDatas > LIMITE_DETALHES)
            impactos.add("Cada lista de detalhes exibe até " + LIMITE_DETALHES + " itens. Contagens e assinatura consideram todos os fatos no alcance.");
        if ("inativar".equals(operacao)) impactos.add("A inativação bloqueia a operação e mantém a vigência e os vínculos.");
        if ("reativar".equals(operacao)) impactos.add("A retomada exige cobertura temporal válida e não reabre vínculos encerrados.");
        if ("corrigir-anulacao".equals(operacao)) impactos.add("A correção mantém o contrato administrativamente inativo até reativação explícita.");
        return new PreviaCicloVidaContratoDTO(contrato.getId(), operacao, acao.dataEfetiva(), acao.dataAto(), acao.motivo(),
                usuario.getId(), usuario.getUsername(), "transferir-gestao".equals(operacao)
                    ? "TRANSFERENCIA_GESTAO_LOCAL" : "GESTOR_LOCAL_RESPONSAVEL", Math.toIntExact(quantidadeVinculos), vinculos,
                amostraProgramacoes, quantidadeFrequencias, datas, List.copyOf(impactos), true,
                assinar(contrato, operacao, acao, usuario, partesDaIntencao), LIMITE_DETALHES, quantidadeVinculos > vinculos.size(),
                quantidadeProgramacoes, quantidadeProgramacoes > amostraProgramacoes.size(),
                quantidadeDatas, quantidadeDatas > datas.size());
    }

    private long contar(String jpql, Long contratoId, LocalDate desde) {
        return em.createQuery(jpql, Long.class).setParameter("contrato", contratoId).setParameter("desde", desde).getSingleResult();
    }

    /** Uma retificação também modifica a faixa retirada do ato anterior. */
    private LocalDate inicioDoAlcance(Contrato contrato, AcaoCicloVidaContratoDTO acao) {
        if (acao.eventoPrecedenteId() == null) return acao.dataEfetiva();
        EventoCicloVidaContrato anterior = em.find(EventoCicloVidaContrato.class, acao.eventoPrecedenteId());
        if (anterior == null || anterior.getContrato() == null
                || !Objects.equals(anterior.getContrato().getId(), contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O evento precedente deve pertencer ao mesmo contrato da prévia.");
        }
        if (anterior.getDataEfetiva() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O alcance do evento precedente não está comprovado.");
        }
        // Sucessão e coexistência preservam o alcance anterior. Somente os
        // atos que retiram/substituem aquele fato atingem também a sua faixa.
        TipoRelacaoPrecedenciaNegocial relacao = acao.relacaoPrecedencia();
        if (relacao != TipoRelacaoPrecedenciaNegocial.RETIFICA
                && relacao != TipoRelacaoPrecedenciaNegocial.SUBSTITUI
                && relacao != TipoRelacaoPrecedenciaNegocial.CANCELA)
            return acao.dataEfetiva();
        return anterior.getDataEfetiva().isBefore(acao.dataEfetiva())
                ? anterior.getDataEfetiva() : acao.dataEfetiva();
    }

    private void validarDados(String operacao, AcaoCicloVidaContratoDTO acao) {
        if (!OPERACOES.contains(operacao)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operação contratual desconhecida.");
        if (acao == null || acao.dataEfetiva() == null || acao.motivo() == null || acao.motivo().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data efetiva e motivo são obrigatórios para a prévia.");
    }

    private String assinar(Contrato contrato, String operacao, AcaoCicloVidaContratoDTO acao,
            UsuarioAplicacao usuario, List<?> partesDaIntencao) {
        List<Object> partes = new ArrayList<>(AssinaturaPreviaNegocial.partes(operacao, usuario.getId(), acao.dataEfetiva(), acao.dataAto(),
                acao.motivo(), acao.referenciaSei(), acao.eventoPrecedenteId(), acao.relacaoPrecedencia(), acao.ordemDocumental()));
        partes.addAll(assinaturaImpacto.partesPorContrato(contrato.getId(), inicioDoAlcance(contrato, acao), null));
        if ("publicar".equals(operacao)) partes.addAll(assinaturaImpacto.partesDaConfiguracaoPublicacao(contrato.getId()));
        partes.add(partesDaIntencao == null ? null : new ArrayList<>(partesDaIntencao));
        return AssinaturaPreviaNegocial.sha256("sgt.previa.ciclo-vida.v1", partes);
    }

    public void validarConfirmacao(Contrato contrato, String operacao, AcaoCicloVidaContratoDTO acao, UsuarioAplicacao usuario) {
        validarConfirmacao(contrato, operacao, acao, usuario, List.of());
    }

    public void validarConfirmacao(Contrato contrato, String operacao, AcaoCicloVidaContratoDTO acao,
            UsuarioAplicacao usuario, List<?> partesDaIntencao) {
        if (acao == null || !Boolean.TRUE.equals(acao.confirmarImpacto()) || acao.tokenPrevia() == null || acao.tokenPrevia().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consulte a prévia de impactos e confirme expressamente a operação.");
        }
        validarDados(operacao, acao);
        if (!Objects.equals(assinar(contrato, operacao, acao, usuario, partesDaIntencao), acao.tokenPrevia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Os impactos mudaram. Consulte uma nova prévia e confirme novamente.");
        }
    }
}
