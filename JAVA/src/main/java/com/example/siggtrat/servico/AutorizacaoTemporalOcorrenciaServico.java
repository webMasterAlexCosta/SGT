package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.*;
import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.documento.ClassificacaoAcessoDocumento;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Uma política para consulta, filtros, anexos e comandos; nunca autoriza pela projeção Empregado. */
@Service
public class AutorizacaoTemporalOcorrenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AutorizacaoTemporalOcorrenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    public record Contexto(LocalDate data, Long contratoId, Long vinculoId, Long alocacaoId,
                           Long gestaoId, Long estadoId, Long lotacaoId, Long jornadaId) {}

    private final ResolvedorContextoTemporalFrequenciaServico operacional;
    private final VinculoContratualRepositorio vinculos;
    private final FrequenciaRepositorio frequencias;
    private final MotorTemporalVinculoServico motorVinculo;
    private final MotorTemporalAlocacaoServico motorAlocacao;
    private final GestaoContratoTemporalServico gestoes;
    private final EscopoAcessoUsuarioServico escopos;
    private final PermissaoExcepcionalServico permissoes;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final EntityManager em;
    private final ResolvedorEscopoProvaFrequenciaServico escopoProva;

    public AutorizacaoTemporalOcorrenciaServico(ResolvedorContextoTemporalFrequenciaServico operacional,
            VinculoContratualRepositorio vinculos, FrequenciaRepositorio frequencias,
            MotorTemporalVinculoServico motorVinculo, MotorTemporalAlocacaoServico motorAlocacao,
            GestaoContratoTemporalServico gestoes, EscopoAcessoUsuarioServico escopos,
            PermissaoExcepcionalServico permissoes, MinimizadorDadosPessoaisServico minimizador, EntityManager em,
            ResolvedorEscopoProvaFrequenciaServico escopoProva) {
        this.operacional = operacional; this.vinculos = vinculos;
        this.frequencias = frequencias; this.motorVinculo = motorVinculo; this.motorAlocacao = motorAlocacao;
        this.gestoes = gestoes; this.escopos = escopos; this.permissoes = permissoes;
        this.minimizador = minimizador; this.em = em; this.escopoProva = escopoProva;
    }

    /** Todo dia atingido é validado antes de gravar o fato ou mover arquivos. */
    public List<Contexto> validarCriacao(UsuarioAplicacao usuario, Empregado empregado, LocalDate inicio, int dias) {
        List<Contexto> resultado = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate dia = inicio.plusDays(i);
            var c = operacional.resolver(empregado, dia);
            var a = c.posicaoAlocacao(); var g = c.gestaoHistorica();
            resultado.add(new Contexto(dia, c.vinculo().getContrato().getId(), c.vinculo().getId(), a.alocacaoId(),
                    g.getId(), g.getSuperintendencia().getId(), a.lotacaoId(), a.jornadaId()));
        }
        exigirEscrita(usuario, resultado);
        return List.copyOf(resultado);
    }

    public void fotografar(Ocorrencia ocorrencia, List<Contexto> contextos) {
        for (Contexto c : contextos) ocorrencia.getContextos().add(new ContextoOcorrencia(ocorrencia, c.data(),
                c.contratoId(), c.vinculoId(), c.alocacaoId(), c.gestaoId(), c.estadoId(), c.lotacaoId(), c.jornadaId()));
    }

    /**
     * O replay atual determina o contexto efetivo, inclusive após correção retroativa.
     * Quando o fato perdeu validade operacional (p.ex. vínculo cancelado), sua prova
     * continua auditável na responsabilidade efetiva comprovada. Legado usa a fotografia de frequência,
     * nunca os campos atuais de Empregado. Sem prova suficiente, a leitura é negada.
     */
    public List<Contexto> resolver(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getEmpregado() == null || ocorrencia.getDataOcorrencia() == null) return List.of();
        Empregado empregado = ocorrencia.getEmpregado();
        Pessoa pessoa = empregado.getPessoaCanonica();
        // CPF é corrigível e pode ser reutilizado por outra identidade. Sua coincidência
        // atual não comprova a autoria de um fato legado: somente associação explícita
        // ou fotografias já vinculadas ao próprio fato podem definir seu contexto.
        List<VinculoContratual> candidatos = pessoa == null ? List.of()
                : vinculos.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId());
        Map<LocalDate, ContextoOcorrencia> fotografias = new HashMap<>();
        ocorrencia.getContextos().forEach(c -> fotografias.put(c.getDataReferencia(), c));
        int dias = ocorrencia.getQuantidadeDias() == null ? 1 : ocorrencia.getQuantidadeDias();
        if (dias <= 0) return List.of();
        Instant conhecimento = Instant.now();
        List<Contexto> resultado = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate data = ocorrencia.getDataOcorrencia().plusDays(i);
            ContextoOcorrencia foto = fotografias.get(data);
            Contexto contexto;
            if (foto != null) contexto = new Contexto(data, foto.getContratoId(), foto.getVinculoId(), foto.getAlocacaoId(),
                    foto.getGestaoLocalId(), foto.getSuperintendenciaId(), foto.getLotacaoId(), foto.getJornadaId());
            else contexto = frequencias.findByEmpregadoIdAndDataReferencia(empregado.getId(), data)
                        .filter(f -> f.getContratoSnapshotId() != null && f.getGestaoLocalSnapshotId() != null
                                && f.getSuperintendenciaSnapshotId() != null && f.getLotacaoSnapshotId() != null
                                && f.getJornadaSnapshotId() != null)
                        .map(f -> new Contexto(data, f.getContratoSnapshotId(),
                                f.getVinculoContratual() == null ? null : f.getVinculoContratual().getId(),
                                f.getAlocacaoVinculo() == null ? null : f.getAlocacaoVinculo().getId(),
                                f.getGestaoLocalSnapshotId(), f.getSuperintendenciaSnapshotId(),
                                f.getLotacaoSnapshotId(), f.getJornadaSnapshotId())).orElse(null);
            if (contexto != null) {
                    // Uma recontratação posterior não pode trocar a identidade do fato antigo.
                    var comprovado = escopoProva.realinhar(new com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO(
                            contexto.data(), contexto.contratoId(), contexto.vinculoId(), contexto.alocacaoId(),
                            contexto.gestaoId(), contexto.estadoId(), contexto.lotacaoId(), contexto.jornadaId()), conhecimento);
                    contexto = comprovado == null ? null : new Contexto(comprovado.dataReferencia(), comprovado.contratoId(),
                            comprovado.vinculoId(), comprovado.alocacaoId(), comprovado.gestaoLocalId(),
                            comprovado.superintendenciaId(), comprovado.lotacaoId(), comprovado.jornadaId());
            } else contexto = resolverCanonico(candidatos, data, conhecimento);
            if (contexto == null) return List.of();
            resultado.add(contexto);
        }
        return List.copyOf(resultado);
    }

    private Contexto resolverCanonico(List<VinculoContratual> candidatos, LocalDate data, Instant conhecimento) {
        Contexto resultado = null;
        for (VinculoContratual v : candidatos) {
            if (v.getContrato() == null || motorVinculo.reconstruir(v, data, conhecimento).situacaoTemporal()
                    != SituacaoTemporalVinculo.VIGENTE) continue;
            var a = motorAlocacao.reconstruir(v, data, conhecimento);
            if (a.situacaoTemporal() != SituacaoTemporalAlocacao.VIGENTE || a.alocacaoId() == null) continue;
            GestaoLocal g = gestoes.gestaoEm(v.getContrato(), data, conhecimento);
            if (g == null || g.getSuperintendencia() == null || a.lotacaoId() == null || a.jornadaId() == null) continue;
            if (resultado != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mais de um contexto temporal foi encontrado para a ocorrência.");
            resultado = new Contexto(data, v.getContrato().getId(), v.getId(), a.alocacaoId(),
                    g.getId(), g.getSuperintendencia().getId(), a.lotacaoId(), a.jornadaId());
        }
        return resultado;
    }

    public List<Contexto> exigirLeitura(UsuarioAplicacao usuario, Ocorrencia ocorrencia) {
        List<Contexto> contextos = resolver(ocorrencia);
        if (!podeConsultar(usuario, contextos)) negar();
        return contextos;
    }

    public boolean podeConsultar(UsuarioAplicacao usuario, List<Contexto> contextos) {
        return usuario != null && Boolean.TRUE.equals(usuario.getActive()) && !contextos.isEmpty()
                && contextos.stream().allMatch(c -> podeConsultar(usuario, c));
    }

    private boolean podeConsultar(UsuarioAplicacao u, Contexto c) {
        if (u.getPerfil() == PerfilAcesso.ADMINISTRADOR || u.getPerfil() == PerfilAcesso.GESTOR_NACIONAL
                || u.getPerfil() == PerfilAcesso.PRESIDENTE) return true;
        if (u.getPerfil() == PerfilAcesso.GESTOR_ESTADUAL)
            return u.getSuperintendencia() != null && Objects.equals(u.getSuperintendencia().getId(), c.estadoId());
        return (u.getPerfil() == PerfilAcesso.GESTOR_LOCAL || u.getPerfil() == PerfilAcesso.OPERACIONAL)
                && escopos.podeGerenciar(u, c.gestaoId(), c.lotacaoId(), c.jornadaId());
    }

    /** Uma narrativa indivisível de vários dias exige autorização em todos eles. */
    public void exigirEscrita(UsuarioAplicacao usuario, List<Contexto> contextos) {
        if (usuario == null || usuario.getPerfil() != PerfilAcesso.GESTOR_LOCAL || !podeConsultar(usuario, contextos)) negar();
    }

    public boolean atendeFiltros(List<Contexto> contextos, Long lotacao, Long jornada, Long contrato) {
        if (contextos.isEmpty()) return false;
        Contexto c = contextos.get(0); // data inicial da ocorrência é o filtro público existente.
        return (lotacao == null || Objects.equals(lotacao, c.lotacaoId()))
                && (jornada == null || Objects.equals(jornada, c.jornadaId()))
                && (contrato == null || Objects.equals(contrato, c.contratoId()));
    }

    public NivelExposicaoDadosPessoais nivel(UsuarioAplicacao usuario, List<Contexto> contextos,
            String finalidade, String recurso, Long id) {
        var padrao = minimizador.nivelPadrao(usuario);
        if (padrao == NivelExposicaoDadosPessoais.COMPLETO || finalidade == null || finalidade.isBlank()) return padrao;
        autorizarConteudoPessoal(usuario, contextos, finalidade, recurso, id,
                "Ocorrencia".equals(recurso) ? id : null);
        return NivelExposicaoDadosPessoais.COMPLETO;
    }

    public void autorizarAnexo(UsuarioAplicacao usuario, AnexoOcorrencia anexo, List<Contexto> contextos, String finalidade) {
        if (anexo.getClassificacaoAcesso() == ClassificacaoAcessoDocumento.PESSOAL_RESTRITO)
            autorizarConteudoPessoal(usuario, contextos, finalidade, "AnexoOcorrencia", anexo.getId(),
                    anexo.getOcorrencia().getId());
    }

    private void autorizarConteudoPessoal(UsuarioAplicacao usuario, List<Contexto> contextos,
            String finalidade, String recurso, Long id, Long ocorrenciaId) {
        if (!podeConsultar(usuario, contextos)) negar();
        if (usuario.getPerfil() == PerfilAcesso.GESTOR_LOCAL) return;
        if (usuario.getPerfil() == PerfilAcesso.OPERACIONAL) negar();
        if (finalidade == null || finalidade.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "O acesso ao conteúdo pessoal exige finalidade e concessão específica vigente.");
        TipoPermissaoExcepcional tipo = usuario.getPerfil() == PerfilAcesso.ADMINISTRADOR
                ? TipoPermissaoExcepcional.INTERVENCAO_NEGOCIAL_TECNICA
                : TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS;
        Set<String> verificados = new HashSet<>();
        for (Contexto c : contextos) if (verificados.add(c.contratoId() + ":" + c.gestaoId())) {
            if (ocorrenciaId != null) {
                var utilizada = permissoes.exigirERegistrarUsoDaOcorrencia(usuario, tipo,
                        em.getReference(Contrato.class, c.contratoId()), em.getReference(GestaoLocal.class, c.gestaoId()),
                        finalidade, recurso, id, ocorrenciaId);
                // Todos os dias já foram autorizados acima. Uma concessão da
                // própria ocorrência cobre o fato inteiro e tem um único uso.
                if (utilizada.getOcorrencia() != null) return;
            } else permissoes.exigirERegistrarUsoNoEscopoHistorico(usuario, tipo,
                    em.getReference(Contrato.class, c.contratoId()), em.getReference(GestaoLocal.class, c.gestaoId()),
                    finalidade, recurso, id);
        }
    }

    private void negar() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão no contexto histórico comprovado da ocorrência.");
    }
}
