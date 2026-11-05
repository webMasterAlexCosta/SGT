package com.example.siggtrat.configuracao;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuração para execução assíncrona de tarefas.
 *
 * <p>Define um {@link ThreadPoolTaskExecutor} com uma pool de threads
 * dimensionada para lidar com altas taxas de requisições simultâneas. O tamanho
 * da pool e da fila foram dimensionados para aproximadamente 10 mil usuários
 * concorrentes sem bloquear a CPU, mas esses valores podem ser ajustados via
 * propriedades conforme necessário.</p>
 */
@Configuration
public class ConfiguracaoAssincrona implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Número mínimo de threads mantidas para tarefas assíncronas.
        executor.setCorePoolSize(50);
        // Número máximo de threads permitidas. Ajuste conforme a carga prevista.
        executor.setMaxPoolSize(200);
        // Capacidade da fila para requisições pendentes quando todas as threads estão ocupadas.
        executor.setQueueCapacity(1000);
        // Prefixo dos nomes das threads para fácil identificação nos logs.
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }



    /**
     * Executor dedicado aos fechamentos massivos. Ele é deliberadamente pequeno:
     * o objetivo é retirar a espera do HTTP sem transformar o MySQL em uma fila de
     * centenas de transações concorrentes.
     */
    @Bean(name = "fechamentoFrequenciaExecutor")
    public Executor fechamentoFrequenciaExecutor(PropriedadesFechamentoFrequencia properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.executorCorePoolSize());
        executor.setMaxPoolSize(properties.executorMaxPoolSize());
        executor.setQueueCapacity(properties.executorQueueCapacity());
        executor.setThreadNamePrefix("freq-fechamento-");
        // A solicitação já está persistida no MySQL. Se a fila local estiver cheia,
        // o recuperador periódico reenfileira o trabalho sem bloquear a requisição HTTP.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /** Executor pequeno e dedicado à consolidação do painel. */
    @Bean(name = "painelExecutor")
    public Executor painelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("painel-calculo-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }


    /**
     * Executor dedicado aos relatórios grandes. Mantém concorrência baixa para
     * evitar que duas ou três exportações nacionais disputem toda a capacidade
     * do MySQL e da área temporária do servidor.
     */
    @Bean(name = "relatorioExportacaoExecutor")
    public Executor relatorioExportacaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("relatorio-exportacao-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}