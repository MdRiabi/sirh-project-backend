package com.sirh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pas de file de jobs dédiée (Redis pub/sub, RabbitMQ...) pour cette version
 * — décision actée en ARCHITECTURE.md §7 : volumétrie modeste (import
 * ≤1500 lignes), un pool borné @Async suffit. La table async_jobs reste
 * l'unique source de vérité de suivi (le frontend fait du polling dessus).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "pointagesImportExecutor")
    public Executor pointagesImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("import-pointages-");
        executor.initialize();
        return executor;
    }

    // Pool dédié pour NotificationEventListener (Lot 8) : plusieurs bean
    // Executor existant désormais, @Async doit être qualifié explicitement
    // partout (Spring ne résout plus un unique bean TaskExecutor par défaut).
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }

    // Génération de rapports (Lot 11, Module 7) : POST /reports/generer lance
    // toujours le calcul sur ce pool, en attend le résultat jusqu'à 5s
    // (CDF : "si le calcul prend <5s, téléchargement direct"), puis bascule en
    // suivi async_jobs si la tâche est encore en cours au-delà.
    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("report-");
        executor.initialize();
        return executor;
    }
}