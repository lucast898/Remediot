package br.edu.remediot.service;

import br.edu.remediot.entity.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Regras de escalonamento por peso (CLAUDE.md):
 * peso 1 -> só aviso no app; peso 2 -> + buzzer/alexa; peso 3 -> + WhatsApp.
 */
class LoggingAlertServiceTest {

    @Test
    void pesoUmSoRegistraAvisoNoApp() {
        LoggingAlertService spyService = Mockito.spy(new LoggingAlertService());
        Dose dose = doseWithWeight(1);

        spyService.handleLateOrMissedDose(dose);

        verify(spyService).logAppWarning(any(), any(), eq(dose));
        verify(spyService, never()).triggerSoundAlarm(any(), any(), any());
        verify(spyService, never()).triggerVoiceAnnouncement(any(), any(), any());
        verify(spyService, never()).notifyCaregiverWhatsApp(any(), any(), any());
    }

    @Test
    void pesoDoisAcionaBuzzerEAlexaMasNaoWhatsapp() {
        LoggingAlertService spyService = Mockito.spy(new LoggingAlertService());
        Dose dose = doseWithWeight(2);

        spyService.handleLateOrMissedDose(dose);

        verify(spyService).triggerSoundAlarm(any(), any(), eq(dose));
        verify(spyService).triggerVoiceAnnouncement(any(), any(), eq(dose));
        verify(spyService, never()).notifyCaregiverWhatsApp(any(), any(), any());
    }

    @Test
    void pesoTresAcionaTudoIncluindoWhatsapp() {
        LoggingAlertService spyService = Mockito.spy(new LoggingAlertService());
        Dose dose = doseWithWeight(3);

        spyService.handleLateOrMissedDose(dose);

        verify(spyService).triggerSoundAlarm(any(), any(), eq(dose));
        verify(spyService).triggerVoiceAnnouncement(any(), any(), eq(dose));
        verify(spyService).notifyCaregiverWhatsApp(any(), any(), eq(dose));
    }

    private Dose doseWithWeight(int weight) {
        User caregiver = new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR);
        User patientUser = new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE);
        Patient patient = new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0);
        Medication medication = new Medication(patient, "Remédio", "1 comprimido", weight);
        MedicationSchedule schedule = new MedicationSchedule(medication, LocalTime.of(8, 0));
        return new Dose(schedule, LocalDate.now(), LocalDateTime.now());
    }
}
