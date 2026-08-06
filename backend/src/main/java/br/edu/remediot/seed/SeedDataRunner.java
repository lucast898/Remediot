package br.edu.remediot.seed;

import br.edu.remediot.entity.Medication;
import br.edu.remediot.entity.MedicationSchedule;
import br.edu.remediot.entity.Patient;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;
import br.edu.remediot.repository.MedicationRepository;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * Popula o banco com um cuidador e alguns pacientes de demonstração na
 * primeira execução (banco vazio), para as telas não nascerem vazias na
 * apresentação. Não roda de novo se já existir algum usuário.
 */
@Component
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String DEMO_PASSWORD = "demo1234";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicationRepository medicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public SeedDataRunner(
            UserRepository userRepository,
            PatientRepository patientRepository,
            MedicationRepository medicationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${remediot.seed.enabled:true}") boolean enabled
    ) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.medicationRepository = medicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }

        User caregiver = new User("Ana Souza", "cuidadora@remediot.com.br", passwordEncoder.encode(DEMO_PASSWORD), Role.CUIDADOR);
        caregiver.setPhone("11999990000");
        caregiver = userRepository.save(caregiver);

        createPatient(caregiver, "Maria Silva", "maria.silva@remediot.com.br", 78,
                "Rua das Acácias, 120 - São Paulo, SP", -23.5505, -46.6333,
                "https://i.pravatar.cc/300?img=47",
                new SeedMedication("Losartana", "50mg", 2, "08:00", "20:00"),
                new SeedMedication("AAS", "100mg", 1, "08:00"),
                new SeedMedication("Insulina NPH", "10UI", 3, "07:00", "19:00")
        );

        createPatient(caregiver, "Antônio Ferreira", "antonio.ferreira@remediot.com.br", 82,
                "Av. Paulista, 900 - São Paulo, SP", -23.5613, -46.6565,
                "https://i.pravatar.cc/300?img=12",
                new SeedMedication("Losartana", "50mg", 2, "09:00"),
                new SeedMedication("Metformina", "850mg", 2, "08:00", "20:00"),
                new SeedMedication("Digoxina", "0,25mg", 3, "08:00")
        );

        createPatient(caregiver, "Rosa Oliveira", "rosa.oliveira@remediot.com.br", 69,
                "Rua Augusta, 500 - São Paulo, SP", -23.5537, -46.6627,
                "https://i.pravatar.cc/300?img=45",
                new SeedMedication("Levotiroxina", "50mcg", 1, "07:00"),
                new SeedMedication("Sinvastatina", "20mg", 1, "21:00")
        );

        createPatient(caregiver, "Carlos Mendes", "carlos.mendes@remediot.com.br", 74,
                "Rua Oscar Freire, 300 - São Paulo, SP", -23.5629, -46.6693,
                "https://i.pravatar.cc/300?img=13",
                new SeedMedication("Varfarina", "5mg", 3, "18:00"),
                new SeedMedication("Omeprazol", "20mg", 1, "07:00"),
                new SeedMedication("Losartana", "50mg", 2, "08:00", "20:00")
        );

        log.info("Dados de exemplo criados: cuidador {} (senha {}) com 4 pacientes na carteira",
                caregiver.getEmail(), DEMO_PASSWORD);
    }

    private void createPatient(
            User caregiver, String fullName, String email, int age, String address,
            double latitude, double longitude, String photoUrl, SeedMedication... medications
    ) {
        User patientUser = new User(fullName, email, passwordEncoder.encode(DEMO_PASSWORD), Role.PACIENTE);
        patientUser = userRepository.save(patientUser);

        Patient patient = new Patient(patientUser, caregiver, age, address, latitude, longitude);
        patient.setPhotoUrl(photoUrl);
        patient = patientRepository.save(patient);

        for (SeedMedication seedMedication : medications) {
            Medication medication = new Medication(patient, seedMedication.name(), seedMedication.dose(), seedMedication.weight());
            for (String time : seedMedication.times()) {
                medication.getSchedules().add(new MedicationSchedule(medication, LocalTime.parse(time)));
            }
            medicationRepository.save(medication);
        }
    }

    private record SeedMedication(String name, String dose, int weight, String... times) {
    }
}
