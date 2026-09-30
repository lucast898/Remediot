package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.DoseReminder;
import br.edu.remediot.entity.DoseStatus;
import br.edu.remediot.entity.Medication;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.repository.DoseReminderRepository;
import br.edu.remediot.repository.DoseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lembretes antes do horário de cada dose (padrão: 10 min, 5 min e 30 s).
 * Todo lembrete é registrado no log; a Alexa só fala para os pacientes
 * listados em remediot.alexa.patient-ids (o plano gratuito do Voice Monkey
 * tem cota mensal pequena).
 */
@Service
public class DoseReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DoseReminderScheduler.class);
    private static final Duration DOSE_GENERATION_INTERVAL = Duration.ofMinutes(1);

    private final DoseGenerationService doseGenerationService;
    private final DoseRepository doseRepository;
    private final DoseReminderRepository reminderRepository;
    private final VoiceAnnouncer voiceAnnouncer;
    private final ReminderPolicy policy;
    private final Set<Long> alexaPatientIds;

    private LocalDateTime lastDoseGeneration = LocalDateTime.MIN;

    public DoseReminderScheduler(
            DoseGenerationService doseGenerationService,
            DoseRepository doseRepository,
            DoseReminderRepository reminderRepository,
            VoiceAnnouncer voiceAnnouncer,
            @Value("${remediot.reminders.offsets:10m,5m,30s}") String offsets,
            @Value("${remediot.alexa.patient-ids:}") String alexaPatientIds
    ) {
        this.doseGenerationService = doseGenerationService;
        this.doseRepository = doseRepository;
        this.reminderRepository = reminderRepository;
        this.voiceAnnouncer = voiceAnnouncer;
        this.policy = new ReminderPolicy(parseDurations(offsets));
        this.alexaPatientIds = parseIds(alexaPatientIds);
    }

    @Scheduled(fixedDelayString = "${remediot.reminders.check-interval-ms:5000}")
    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Doses são geradas sob demanda; garante que medicamentos recém-criados entrem na verificação
        if (Duration.between(lastDoseGeneration, now).compareTo(DOSE_GENERATION_INTERVAL) >= 0) {
            doseGenerationService.ensureDosesForToday();
            lastDoseGeneration = now;
        }

        List<Dose> upcoming = doseRepository.findByStatusAndScheduledAtBetween(
                DoseStatus.AGENDADA, now, now.plus(policy.maxOffset()));

        for (Dose dose : upcoming) {
            Set<Duration> alreadySent = reminderRepository.findByDoseId(dose.getId()).stream()
                    .map(reminder -> Duration.ofSeconds(reminder.getOffsetSeconds()))
                    .collect(Collectors.toSet());

            policy.dueOffset(dose.getScheduledAt(), now, alreadySent).ifPresent(offset -> {
                reminderRepository.save(new DoseReminder(dose, offset.toSeconds()));
                remind(dose, offset);
            });
        }
    }

    private void remind(Dose dose, Duration offset) {
        Medication medication = dose.getSchedule().getMedication();
        Patient patient = medication.getPatient();
        String speech = ReminderPolicy.speechFor(
                firstName(patient.getUser().getFullName()), medication.getName(), medication.getDose(), offset);

        log.info("[LEMBRETE] paciente={} dose={} horario={} faltam={}s: {}",
                patient.getId(), dose.getId(), dose.getScheduledAt(), offset.toSeconds(), speech);

        if (alexaPatientIds.contains(patient.getId())) {
            voiceAnnouncer.announce(speech);
        }
    }

    private static String firstName(String fullName) {
        return fullName.trim().split("\\s+")[0];
    }

    private static List<Duration> parseDurations(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(DurationStyle::detectAndParse)
                .toList();
    }

    private static Set<Long> parseIds(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
