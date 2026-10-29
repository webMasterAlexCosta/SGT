package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioAplicacaoRepositorio extends JpaRepository<UsuarioAplicacao, Long> {
    Optional<UsuarioAplicacao> findByUsername(String username);
    Optional<UsuarioAplicacao> findByEmail(String email);
    Optional<UsuarioAplicacao> findByEmailIgnoreCase(String email);
    Optional<UsuarioAplicacao> findByMatricula(String matricula);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByMatricula(String matricula);
    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    /**
     * Consultas de usuários já limitadas no banco pelo perfil e pelo escopo do
     * gestor autenticado. A filtragem em memória continua existindo no serviço
     * apenas como defesa adicional, nunca como única barreira de acesso.
     */
    List<UsuarioAplicacao> findAllByOrderByUsernameAsc();
    List<UsuarioAplicacao> findByPerfilOrderByUsernameAsc(PerfilAcesso perfil);
    List<UsuarioAplicacao> findByPerfilAndSuperintendenciaIdOrderByUsernameAsc(PerfilAcesso perfil, Long superintendenciaId);
    List<UsuarioAplicacao> findByPerfilAndGestaoLocalIdOrderByUsernameAsc(PerfilAcesso perfil, Long gestaoLocalId);

    @Query("""
            select distinct usuario
            from UsuarioAplicacao usuario
            left join usuario.gestaoLocal gestaoLocal
            where usuario.superintendencia.id = :superintendenciaId
               or gestaoLocal.superintendencia.id = :superintendenciaId
            """)
    List<UsuarioAplicacao> buscarUsuariosVinculadosASuperintendencia(
            @Param("superintendenciaId") Long superintendenciaId
    );

    @Query("""
            select usuario
            from UsuarioAplicacao usuario
            where usuario.gestaoLocal.id = :gestaoLocalId
            """)
    List<UsuarioAplicacao> buscarUsuariosVinculadosAoGestaoLocal(
            @Param("gestaoLocalId") Long gestaoLocalId
    );

    boolean existsBySuperintendenciaId(Long superintendenciaId);
    boolean existsByGestaoLocalId(Long gestaoLocalId);
    boolean existsByLotacaoId(Long lotacaoId);
    boolean existsByJornadaId(Long jornadaId);
    boolean existsByVinculosGestao_Lotacao_Id(Long lotacaoId);
    boolean existsByVinculosGestao_Jornada_Id(Long jornadaId);
}
