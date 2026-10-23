package br.edu.remediot.service;

import br.edu.remediot.dto.CreateMedicationRequest;
import br.edu.remediot.dto.MedicationResponse;
import br.edu.remediot.dto.UpdateMedicationRequest;
import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.Medication;
import br.edu.remediot.entity.MedicationSchedule;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.DoseReminderRepository;
import br.edu.remediot.repository.DoseRepository;
import br.edu.remediot.repository.MedicationRepository;
import br.edu.remediot.repository.OpeningEventRepository;
import br.edu.remediot.repository.PatientRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;
    private final DoseRepository doseRepository;
    private final OpeningEventRepository openingEventRepository;
    private final DoseReminderRepository doseReminderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MedicationService(
            MedicationRepository medicationRepository,
            PatientRepository patientRepository,
            DoseRepository doseRepository,
            OpeningEventRepository openingEventRepository,
            DoseReminderRepository doseReminderRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
        this.doseRepository = doseRepository;
        this.openingEventRepository = openingEventRepository;
        this.doseReminderRepository = doseReminderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> listByPatient(Long patientId) {
        return medicationRepository.findByPatientId(patientId).stream()
                .map(MedicationResponse::from)
                .toList();
    }

    @Transactional
    public MedicationResponse create(Long patientId, CreateMedicationRequest request) {
        Patient patient = patientRepository.getReferenceById(patientId);
        Medication medication = new Medication(patient, request.name(), request.dose(), request.weight());
        request.times().forEach(time -> medication.getSchedules().add(new MedicationSchedule(medication, time)));
        MedicationResponse response = MedicationResponse.from(medicationRepository.save(medication));
        eventPublisher.publishEvent(new MedicationsChangedEvent(patientId));
        return response;
    }

    /**
     * Horários mantidos preservam suas doses e histórico; só os horários removidos
     * perdem as doses associadas.
     */
    @Transactional
    public MedicationResponse update(Long medicationId, Long patientId, UpdateMedicationRequest request) {
        Medication medication = findOwnedByPatient(medicationId, patientId);
        medication.setName(request.name());
        medication.setDose(request.dose());
        medication.setWeight(request.weight());

        Set<LocalTime> requestedTimes = new LinkedHashSet<>(request.times());
        List<MedicationSchedule> removedSchedules = medication.getSchedules().stream()
                .filter(schedule -> !requestedTimes.contains(schedule.getTime()))
                .toList();
        deleteDoseHistory(removedSchedules);
        medication.getSchedules().removeAll(removedSchedules);

        Set<LocalTime> existingTimes = medication.getSchedules().stream()
                .map(MedicationSchedule::getTime)
                .collect(Collectors.toSet());
        requestedTimes.stream()
                .filter(time -> !existingTimes.contains(time))
                .forEach(time -> medication.getSchedules().add(new MedicationSchedule(medication, time)));

        MedicationResponse response = MedicationResponse.from(medicationRepository.save(medication));
        eventPublisher.publishEvent(new MedicationsChangedEvent(patientId));
        return response;
    }

    /** Apaga o medicamento junto com todo o histórico de doses dele. */
    @Transactional
    public void delete(Long medicationId, Long patientId) {
        Medication medication = findOwnedByPatient(medicationId, patientId);
        deleteDoseHistory(medication.getSchedules());
        medicationRepository.delete(medication);
        eventPublisher.publishEvent(new MedicationsChangedEvent(patientId));
    }

    private void deleteDoseHistory(Collection<MedicationSchedule> schedules) {
        if (schedules.isEmpty()) {
            return;
        }
        List<Long> scheduleIds = schedules.stream().map(MedicationSchedule::getId).toList();
        List<Dose> doses = doseRepository.findByScheduleIdIn(scheduleIds);
        if (doses.isEmpty()) {
            return;
        }
        List<Long> doseIds = doses.stream().map(Dose::getId).toList();
        openingEventRepository.deleteByDoseIdIn(doseIds);
        doseReminderRepository.deleteByDoseIdIn(doseIds);
        doseRepository.deleteAll(doses);
        // O Hibernate remove órfãos (horários) antes de outros deletes; força a ordem certa
        doseRepository.flush();
    }

    private Medication findOwnedByPatient(Long medicationId, Long patientId) {
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento não encontrado"));
        if (!medication.getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Medicamento não encontrado");
        }
        return medication;
    }
}
