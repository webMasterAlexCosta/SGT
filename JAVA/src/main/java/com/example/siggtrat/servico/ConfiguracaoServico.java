// Caminho: java/com/example/siggtrat/servico/ConfiguracaoServico.java
package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConfiguracaoServico {

    private final ConfiguracaoSistemaRepositorio repositorio;

    // Cache simples em memória para não fazer query a todo momento
    private Map<String, String> cache = new ConcurrentHashMap<>();
    private long ultimaAtualizacao = 0;
    private static final long CACHE_TTL_MS = 60000; // Atualiza a cada 1 minuto

    public ConfiguracaoServico(ConfiguracaoSistemaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    private void carregarCacheSeNecessario() {
        if (cache.isEmpty() || System.currentTimeMillis() - ultimaAtualizacao > CACHE_TTL_MS) {
            cache = repositorio.findAll().stream()
                    .collect(Collectors.toMap(ConfiguracaoSistema::getChave, ConfiguracaoSistema::obterValor));
            ultimaAtualizacao = System.currentTimeMillis();
        }
    }

    public String obterValor(String chave, String valorPadrao) {
        carregarCacheSeNecessario();
        return cache.getOrDefault(chave, valorPadrao);
    }

    // --- Métodos Tipados para Facilidade ---

    public int obterHorasTrabalhoPorDia() {
        return Integer.parseInt(obterValor("HORAS_DIA_TRABALHO", "8"));
    }

    public LocalTime obterHoraInicioNoturno() {
        return LocalTime.parse(obterValor("HORA_INICIO_NOTURNO", "22:00"));
    }

    public LocalTime obterHoraFimNoturno() {
        return LocalTime.parse(obterValor("HORA_FIM_NOTURNO", "05:00"));
    }

    public double obterMinutosPorHoraNoturna() {
        // 52.5 minutos representa 52 minutos e 30 segundos da CLT
        return Double.parseDouble(obterValor("MINUTOS_HORA_NOTURNA", "52.5"));
    }
}