package br.edu.remediot.service;

import br.edu.remediot.entity.DoseStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoseStatusServiceTest {

    private final DoseStatusService service = new DoseStatusService();
    private final LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 5, 8, 0);

    @Test
    void abrirNoHorarioExatoResultaEmTomada() {
        assertEquals(DoseStatus.TOMADA, service.resolveStatusForOpening(scheduledAt, scheduledAt));
    }

    @Test
    void abrirDentroDaToleranciaAposOHorarioResultaEmTomada() {
        LocalDateTime openedAt = scheduledAt.plusMinutes(30);
        assertEquals(DoseStatus.TOMADA, service.resolveStatusForOpening(scheduledAt, openedAt));
    }

    @Test
    void abrirDentroDaToleranciaAntesDoHorarioResultaEmTomada() {
        LocalDateTime openedAt = scheduledAt.minusMinutes(30);
        assertEquals(DoseStatus.TOMADA, service.resolveStatusForOpening(scheduledAt, openedAt));
    }

    @Test
    void abrirUmMinutoAposAToleranciaResultaEmAtrasada() {
        LocalDateTime openedAt = scheduledAt.plusMinutes(31);
        assertEquals(DoseStatus.ATRASADA, service.resolveStatusForOpening(scheduledAt, openedAt));
    }

    @Test
    void abrirMuitoDepoisResultaEmAtrasada() {
        LocalDateTime openedAt = scheduledAt.plusHours(5);
        assertEquals(DoseStatus.ATRASADA, service.resolveStatusForOpening(scheduledAt, openedAt));
    }

    @Test
    void resolveStatusExigeDatasNaoNulas() {
        assertThrows(IllegalArgumentException.class, () -> service.resolveStatusForOpening(null, scheduledAt));
        assertThrows(IllegalArgumentException.class, () -> service.resolveStatusForOpening(scheduledAt, null));
    }

    @Test
    void janelaDeNaoRetiradaAindaNaoPassouLogoAposOHorario() {
        LocalDateTime now = scheduledAt.plusHours(1);
        assertEquals(false, service.isMissedWindowPassed(scheduledAt, now));
    }

    @Test
    void janelaDeNaoRetiradaPassaTresHorasAposOHorario() {
        LocalDateTime now = scheduledAt.plusHours(3).plusMinutes(1);
        assertEquals(true, service.isMissedWindowPassed(scheduledAt, now));
    }

    @Test
    void janelaDeNaoRetiradaNoLimiteExatoAindaNaoPassou() {
        LocalDateTime now = scheduledAt.plusHours(3);
        assertEquals(false, service.isMissedWindowPassed(scheduledAt, now));
    }
}
