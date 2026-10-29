// Caminho: java/com/example/siggtrat/repositorio/ConfiguracaoSistemaRepositorio.java
package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoSistemaRepositorio extends JpaRepository<ConfiguracaoSistema, String> {
}