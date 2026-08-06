package br.edu.remediot.service;

import br.edu.remediot.dto.CreateMedicationRequest;
import br.edu.remediot.dto.MedicationResponse;
import br.edu.remediot.dto.UpdateMedicationRequest;
import br.edu.remediot.entity.Medication;
import br.edu.remediot.entity.MedicationSchedule;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.MedicationRepository;
import br.edu.remediot.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;

    public MedicationService(MedicationRepository medicationRepository, PatientRepository patientRepository) {
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
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
        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationResponse update(Long medicationId, Long patientId, UpdateMedicationRequest request) {
        Medication medication = findOwnedByPatient(medicationId, patientId);
        medication.setName(request.name());
        medication.setDose(request.dose());
        medication.setWeight(request.weight());
        medication.getSchedules().clear();
        request.times().forEach(time -> medication.getSchedules().add(new MedicationSchedule(medication, time)));
        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional
    public void delete(Long medicationId, Long patientId) {
        medicationRepository.delete(findOwnedByPatient(medicationId, patientId));
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
