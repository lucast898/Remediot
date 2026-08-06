package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.Medication;
import br.edu.remediot.entity.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingAlertService implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertService.class);

    @Override
    public void handleLateOrMissedDose(Dose dose) {
        Medication medication = dose.getSchedule().getMedication();
        Patient patient = medication.getPatient();
        int weight = medication.getWeight();

        logAppWarning(patient, medication, dose);

        if (weight >= 2) {
            triggerSoundAlarm(patient, medication, dose);
            triggerVoiceAnnouncement(patient, medication, dose);
        }
        if (weight >= 3) {
            notifyCaregiverWhatsApp(patient, medication, dose);
        }
    }

    protected void logAppWarning(Patient patient, Medication medication, Dose dose) {
        log.info("[ALERTA app] paciente={} medicamento={} (peso {}) status={} horario={}",
                patient.getId(), medication.getName(), medication.getWeight(), dose.getStatus(), dose.getScheduledAt());
    }

    protected void triggerSoundAlarm(Patient patient, Medication medication, Dose dose) {
        log.info("[GANCHO buzzer] paciente={} medicamento={} — publicar MQTT '.../buzzer: ligar' quando o ESP32 existir",
                patient.getId(), medication.getName());
    }

    protected void triggerVoiceAnnouncement(Patient patient, Medication medication, Dose dose) {
        log.info("[GANCHO alexa] paciente={} medicamento={} — chamar webhook do Voice Monkey quando integrado",
                patient.getId(), medication.getName());
    }

    protected void notifyCaregiverWhatsApp(Patient patient, Medication medication, Dose dose) {
        log.info("[GANCHO whatsapp] cuidador={} paciente={} medicamento={} (peso 3, atrasado) — enviar mensagem quando integrado",
                patient.getCaregiver().getId(), patient.getId(), medication.getName());
    }
}
