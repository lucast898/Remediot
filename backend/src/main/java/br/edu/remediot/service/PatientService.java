package br.edu.remediot.service;

import br.edu.remediot.dto.CreatePatientRequest;
import br.edu.remediot.dto.PatientResponse;
import br.edu.remediot.dto.UpdatePatientRequest;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;
import br.edu.remediot.exception.EmailAlreadyInUseException;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * As leituras convertem para DTO dentro do próprio método transacional:
 * com spring.jpa.open-in-view=false, associações lazy (ex.: patient.getUser())
 * não podem ser acessadas depois que a transação termina.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listByCaregiver(Long caregiverId) {
        return patientRepository.findByCaregiverId(caregiverId).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getResponse(Long patientId) {
        return PatientResponse.from(findOrThrow(patientId));
    }

    @Transactional
    public PatientResponse create(Long caregiverId, CreatePatientRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }
        User caregiver = userRepository.findById(caregiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuidador não encontrado"));

        User patientUser = new User(request.fullName(), request.email(), passwordEncoder.encode(request.password()), Role.PACIENTE);
        patientUser = userRepository.save(patientUser);

        Patient patient = new Patient(patientUser, caregiver, request.age(), request.address(), request.latitude(), request.longitude());
        patient.setPhotoUrl(request.photoUrl());
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(Long patientId, UpdatePatientRequest request) {
        Patient patient = findOrThrow(patientId);
        patient.setAge(request.age());
        patient.setAddress(request.address());
        patient.setLatitude(request.latitude());
        patient.setLongitude(request.longitude());
        patient.setPhotoUrl(request.photoUrl());
        return PatientResponse.from(patientRepository.save(patient));
    }

    private Patient findOrThrow(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
    }
}
