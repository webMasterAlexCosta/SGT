// Caminho: java/com/example/siggtrat/repositorio/ConfiguracaoSistemaRepositorio.java
package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoSistemaRepositorio extends JpaRepository<ConfiguracaoSistema, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from ConfiguracaoSistema c where c.chave = :chave")
    java.util.Optional<ConfiguracaoSistema> buscarPorChaveParaAtualizacao(
            @org.springframework.data.repository.query.Param("chave") String chave);
}
