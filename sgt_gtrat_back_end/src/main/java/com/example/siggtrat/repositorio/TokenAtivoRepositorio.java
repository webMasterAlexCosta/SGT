package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenAtivoRepositorio extends JpaRepository<TokenAtivo, Long> {

    boolean existsByTokenHash(String tokenHash);
    Optional<TokenAtivo> findByTokenHash(String tokenHash);

    @Transactional
    void deleteByTokenHash(String tokenHash);

    default boolean existePorTokenBruto(String rawToken) {
        return rawToken != null && existsByTokenHash(UtilitarioHashSeguranca.sha256Hexadecimal(rawToken));
    }

    default Optional<TokenAtivo> buscarPorTokenBruto(String rawToken) {
        return rawToken == null ? Optional.empty() : findByTokenHash(UtilitarioHashSeguranca.sha256Hexadecimal(rawToken));
    }

    default void excluirPorTokenBruto(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) deleteByTokenHash(UtilitarioHashSeguranca.sha256Hexadecimal(rawToken));
    }

    @Transactional
    void deleteByUsername(String username);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM token_ativo WHERE expire_at < :now LIMIT :limit", nativeQuery = true)
    int excluirLoteTokensExpirados(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("UPDATE TokenAtivo t SET t.lastSeenAt = :now WHERE t.id = :id")
    void atualizarUltimoAcesso(@Param("id") Long id, @Param("now") LocalDateTime now);
}
