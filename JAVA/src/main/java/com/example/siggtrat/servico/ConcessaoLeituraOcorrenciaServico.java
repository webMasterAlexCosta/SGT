package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dto.ConcessaoPermissaoExcepcionalRequisicao;
import com.example.siggtrat.dto.PermissaoExcepcionalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.OcorrenciaRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Coordena prova histórica e locks; o ciclo da permissão permanece no serviço compartilhado. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ConcessaoLeituraOcorrenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ConcessaoLeituraOcorrenciaServico.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final AcessoOrganizacionalServico acesso;
    private final AutorizacaoTemporalOcorrenciaServico autorizacao;
    private final PermissaoExcepcionalServico permissoes;
    private final OcorrenciaRepositorio ocorrencias;
    private final UsuarioAplicacaoRepositorio usuarios;
    private final EntityManager em;

    public ConcessaoLeituraOcorrenciaServico(AcessoOrganizacionalServico acesso,
            AutorizacaoTemporalOcorrenciaServico autorizacao, PermissaoExcepcionalServico permissoes,
            OcorrenciaRepositorio ocorrencias, UsuarioAplicacaoRepositorio usuarios, EntityManager em) {
        this.acesso = acesso; this.autorizacao = autorizacao; this.permissoes = permissoes;
        this.ocorrencias = ocorrencias; this.usuarios = usuarios; this.em = em;
    }

    public PermissaoExcepcionalDTO conceder(ConcessaoPermissaoExcepcionalRequisicao r) {
        UsuarioAplicacao administrador = acesso.usuarioAtual();
        exigirAdministrador(administrador);
        if (r == null || r.ocorrenciaId() == null || r.ocorrenciaId() <= 0
                || r.tipo() != TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS
                || r.contratoId() != null || r.gestaoLocalId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe somente a ocorrência como alvo da concessão de leitura restrita.");
        }
        Ocorrencia descoberta = obter(r.ocorrenciaId());
        Set<Long> contratos = contratos(provaCompleta(descoberta));
        Long empregadoId = descoberta.getEmpregado().getId();
        Long pessoaId = pessoaId(descoberta.getEmpregado());
        // A descoberta é somente escalar. Os loads com lock abaixo devem partir
        // de um contexto vazio, sem reutilizar entidades anteriores à espera.
        em.clear();

        // A descoberta não autoriza o comando. Mesma ordem das escritas canônicas:
        // Contratos crescentes -> Pessoa -> Empregado -> Ocorrência -> usuários.
        // Sem vínculo atual com Pessoa, a prova legada só pode vir das fontes
        // históricas reconhecidas pelo resolvedor; jamais de CPF ou Gestão atuais.
        contratos.forEach(id -> travar(Contrato.class, id, LockModeType.PESSIMISTIC_WRITE));
        if (pessoaId != null) travar(Pessoa.class, pessoaId, LockModeType.PESSIMISTIC_READ);
        Empregado empregado = travar(Empregado.class, empregadoId, LockModeType.PESSIMISTIC_READ);
        if (!Objects.equals(pessoaId, pessoaId(empregado))) conflito();
        // Locks PostgreSQL continuam até o commit; retirar o contexto anterior
        // impede que a segunda resolução reutilize fotografias lidas antes da espera.
        em.clear();
        Ocorrencia ocorrencia = travar(Ocorrencia.class, r.ocorrenciaId(), LockModeType.PESSIMISTIC_READ);
        if (!Objects.equals(empregadoId, ocorrencia.getEmpregado().getId())) conflito();
        var contextos = provaCompleta(ocorrencia);
        if (!contratos.equals(contratos(contextos))) conflito();

        UsuarioAplicacao beneficiario = usuarios.findById(r.usuarioBeneficiarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário beneficiário não encontrado."));
        em.refresh(beneficiario, LockModeType.PESSIMISTIC_READ);
        // clear também destacou o executor inicial; recarregar sem merge evita
        // copiar perfil/ativo antigos de volta ao banco antes da revalidação.
        administrador = usuarios.findById(administrador.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado."));
        acesso.revalidarAposEspera(administrador);
        exigirAdministrador(administrador);
        if (!Boolean.TRUE.equals(beneficiario.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão não pode ser concedida a uma conta inativa.");
        }
        if (beneficiario.getPerfil() == null || !Set.of(PerfilAcesso.GESTOR_ESTADUAL, PerfilAcesso.GESTOR_NACIONAL, PerfilAcesso.PRESIDENTE)
                .contains(beneficiario.getPerfil())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este acesso restrito é destinado aos perfis de auditoria ou consulta institucional.");
        }
        if (!autorizacao.podeConsultar(beneficiario, contextos)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O beneficiário não possui escopo sobre todos os dias da ocorrência.");
        }
        return permissoes.concederParaOcorrencia(r, ocorrencia);
    }

    private List<AutorizacaoTemporalOcorrenciaServico.Contexto> provaCompleta(Ocorrencia ocorrencia) {
        var contextos = autorizacao.resolver(ocorrencia);
        int dias = ocorrencia.getQuantidadeDias() == null ? 1 : ocorrencia.getQuantidadeDias();
        if (dias <= 0 || contextos.size() != dias) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A concessão exige contexto histórico comprovado de todos os dias da ocorrência.");
        }
        return contextos;
    }

    private Set<Long> contratos(List<AutorizacaoTemporalOcorrenciaServico.Contexto> contextos) {
        Set<Long> ids = new TreeSet<>();
        for (var contexto : contextos) ids.add(contexto.contratoId());
        return ids;
    }

    private Ocorrencia obter(Long id) {
        return ocorrencias.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));
    }

    private <T> T travar(Class<T> tipo, Long id, LockModeType modo) {
        T entidade = em.find(tipo, id, modo);
        if (entidade == null) conflito();
        return entidade;
    }

    private Long pessoaId(Empregado empregado) {
        return empregado.getPessoaCanonica() == null ? null : empregado.getPessoaCanonica().getId();
    }

    private void exigirAdministrador(UsuarioAplicacao usuario) {
        if (!acesso.ehAdministrador(usuario)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Somente a administração técnica concede ou revoga permissões excepcionais.");
    }

    private void conflito() {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O contexto da ocorrência mudou durante a concessão. Refaça a solicitação.");
    }
}
