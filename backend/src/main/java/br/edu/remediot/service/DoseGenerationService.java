package br.edu.remediot.service;

import br.edu.remediot.dto.DoseResponse;
import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.MedicationSchedule;
import br.edu.remediot.repository.DoseRepository;
import br.edu.remediot.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Doses são geradas sob demanda (não pré-criadas em lote): cada MedicationSchedule
 * gera uma Dose por dia, criada na primeira vez que alguém consulta aquele dia.
 */
@Service
public class DoseGenerationService {

    private final MedicationScheduleRepository scheduleRepository;
    private final DoseRepository doseRepository;

    public DoseGenerationService(MedicationScheduleRepository scheduleRepository, DoseRepository doseRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doseRepository = doseRepository;
    }

    @Transactional
    public Dose ensureDoseForDate(MedicationSchedule schedule, LocalDate date) {
        return doseRepository.findByScheduleIdAndScheduledDate(schedule.getId(), date)
                .orElseGet(() -> doseRepository.save(
                        new Dose(schedule, date, LocalDateTime.of(date, schedule.getTime()))
                ));
    }

    @Transactional
    public List<Dose> ensureDosesForPatientAndDate(Long patientId, LocalDate date) {
        List<MedicationSchedule> schedules = scheduleRepository.findByMedicationPatientId(patientId);
        return schedules.stream()
                .map(schedule -> ensureDoseForDate(schedule, date))
                .sorted(Comparator.comparing(Dose::getScheduledAt))
                .toList();
    }

    @Transactional
    public void ensureDosesForToday() {
        LocalDate today = LocalDate.now();
        scheduleRepository.findAll().forEach(schedule -> ensureDoseForDate(schedule, today));
    }

    @Transactional
    public List<DoseResponse> listDosesForDate(Long patientId, LocalDate date) {
        return ensureDosesForPatientAndDate(patientId, date).stream()
                .map(DoseResponse::from)
                .toList();
    }
}
