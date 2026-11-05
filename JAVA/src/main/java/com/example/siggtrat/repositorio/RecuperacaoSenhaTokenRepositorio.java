package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RecuperacaoSenhaToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RecuperacaoSenhaTokenRepositorio extends JpaRepository<RecuperacaoSenhaToken, Long> {

    Optional<RecuperacaoSenhaToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RecuperacaoSenhaToken t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<RecuperacaoSenhaToken> buscarParaConsumo(@Param("tokenHash") String tokenHash);

    Optional<RecuperacaoSenhaToken> findTopByUsuario_IdOrderByCreatedAtDesc(Long usuarioId);

    @Transactional
    void deleteByUsuario_Id(Long usuarioId);

    @Modifying
    @Transactional
    @Query("delete from RecuperacaoSenhaToken t where t.usuario.id = :usuarioId and t.id <> :tokenId")
    int excluirOutrosTokensDoUsuario(@Param("usuarioId") Long usuarioId, @Param("tokenId") Long tokenId);

    @Modifying
    @Transactional
    @Query("delete from RecuperacaoSenhaToken t where t.expireAt < :agora or (t.usedAt is not null and t.usedAt < :limiteUsados)")
    int limparExpiradosEConsumidos(@Param("agora") LocalDateTime agora,
                                   @Param("limiteUsados") LocalDateTime limiteUsados);
}
