package com.elyella.backend.scheduler;

import com.elyella.backend.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tareas programadas de mantenimiento.
 * Reemplaza el CleanupJob de Quartz del legado (que era un stub sin lógica real).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final CartRepository cartRepository;

    /**
     * Elimina los carritos con más de 30 días de antigüedad.
     * Se ejecuta todos los días a las 02:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanAbandonedCarts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = cartRepository.deleteByAddedAtBefore(cutoff);
        log.info("Limpieza de carritos abandonados completada: {} registros eliminados.", deleted);
    }
}
