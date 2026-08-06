package br.edu.remediot.service;

import br.edu.remediot.entity.DoseStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regras de janela para o status da Dose. Os valores de tolerância são uma
 * decisão de produto (não uma medida clínica) — ver CLAUDE.md: o sistema mede
 * interação com o compartimento, não ingestão.
 */
@Service
public class DoseStatusService {

    public static final Duration ON_TIME_TOLERANCE = Duration.ofMinutes(30);
    public static final Duration MISSED_AFTER = Duration.ofHours(3);

    public DoseStatus resolveStatusForOpening(LocalDateTime scheduledAt, LocalDateTime openedAt) {
        if (scheduledAt == null || openedAt == null) {
            throw new IllegalArgumentException("scheduledAt e openedAt são obrigatórios");
        }
        LocalDateTime windowStart = scheduledAt.minus(ON_TIME_TOLERANCE);
        LocalDateTime windowEnd = scheduledAt.plus(ON_TIME_TOLERANCE);
        boolean withinWindow = !openedAt.isBefore(windowStart) && !openedAt.isAfter(windowEnd);
        return withinWindow ? DoseStatus.TOMADA : DoseStatus.ATRASADA;
    }

    public boolean isMissedWindowPassed(LocalDateTime scheduledAt, LocalDateTime now) {
        if (scheduledAt == null || now == null) {
            throw new IllegalArgumentException("scheduledAt e now são obrigatórios");
        }
        return now.isAfter(scheduledAt.plus(MISSED_AFTER));
    }
}
