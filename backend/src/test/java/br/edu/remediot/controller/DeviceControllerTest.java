package br.edu.remediot.controller;

import br.edu.remediot.entity.Patient;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import br.edu.remediot.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Permissões dos endpoints da gaveta (isolamento por dono, pareamento só pelo cuidador). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;

    private User caregiver;
    private User patientUser;
    private User otherCaregiver;
    private Patient patient;

    @BeforeEach
    void setUp() {
        caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        patientUser = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        otherCaregiver = userRepository.save(new User("Outro", "outro@example.com", "hash", Role.CUIDADOR));
        patient = patientRepository.save(new Patient(patientUser, caregiver, 70, "Rua A", 0.0, 0.0));
    }

    @Test
    void cuidadorEPacienteVeemAGaveta() throws Exception {
        mvc.perform(get(url("")).with(user(new UserPrincipal(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paired").value(false));
        mvc.perform(get(url("")).with(user(new UserPrincipal(patientUser))))
                .andExpect(status().isOk());
    }

    @Test
    void cuidadorDeOutraCarteiraNaoVe() throws Exception {
        mvc.perform(get(url("")).with(user(new UserPrincipal(otherCaregiver))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pacienteNaoPareiaNemDesvincula() throws Exception {
        mvc.perform(post(url("/pair")).with(user(new UserPrincipal(patientUser)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"C4D5\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url("")).with(user(new UserPrincipal(patientUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void codigoSemGavetaDevolveMensagemClara() throws Exception {
        mvc.perform(post(url("/pair")).with(user(new UserPrincipal(caregiver)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"C4D5\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Nenhuma gaveta")));
    }

    private String url(String suffix) {
        return "/api/patients/" + patient.getId() + "/device" + suffix;
    }
}
