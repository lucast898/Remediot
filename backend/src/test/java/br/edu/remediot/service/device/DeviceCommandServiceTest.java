package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceCommandMessage;
import br.edu.remediot.dto.device.DeviceCommandResponse;
import br.edu.remediot.entity.*;
import br.edu.remediot.exception.ResourceNotFoundException;
import br.edu.remediot.mqtt.DeviceMessagePublisher;
import br.edu.remediot.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Comandos e acks (CONTRATO-MQTT.md, seções 8 a 10). */
@SpringBootTest
@Transactional
class DeviceCommandServiceTest {

    private static final String DEVICE_ID = "A4CF12B3C4D5";
    private static final String COMMAND_TOPIC = "remediot/v1/A4CF12B3C4D5/comando";

    @Autowired DeviceCommandService service;
    @Autowired DeviceAckService ackService;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceCommandRepository commandRepository;
    @Autowired DeviceEventRepository eventRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired EntityManager entityManager;

    @MockitoBean DeviceMessagePublisher publisher;

    private Patient patient;
    private Device device;

    @BeforeEach
    void setUp() {
        User caregiver = userRepository.save(new User("Cuidador", "cuidador@example.com", "hash", Role.CUIDADOR));
        User user = userRepository.save(new User("Paciente", "paciente@example.com", "hash", Role.PACIENTE));
        patient = patientRepository.save(new Patient(user, caregiver, 70, "Rua A", 0.0, 0.0));
        device = new Device(DEVICE_ID);
        device.setPatient(patient);
        device.setPairedAt(Instant.now().minus(Duration.ofDays(1)));
        device = deviceRepository.save(device);
        flushAndClear();
    }

    @Test
    void reabastecerPublicaComandoNuncaRetidoEValidoPor120s() {
        DeviceCommandResponse response = service.startRefill(patient.getId(), null);

        DeviceCommandMessage message = publishedCommand();
        assertEquals(response.commandId(), message.commandId());
        assertTrue(message.commandId().matches("cmd-[0-9a-f]{8}"));
        assertEquals("MODO_REABASTECER", message.tipo());
        assertEquals("INICIAR", message.parametros().get("acao"));
        assertEquals(10, ((Number) message.parametros().get("duracaoMin")).intValue());
        assertEquals(message.emitidoEm() + 120, message.validoAte());
        assertEquals(DeviceCommandStatus.PENDENTE, response.status());
        assertEquals(DeviceCommandStatus.PENDENTE, commandRepository.findById(response.commandId()).orElseThrow().getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "OK,,OK",
            "ERRO,DOSE_EM_ANDAMENTO,ERRO",
            "IGNORADO,SEM_ALARME_ATIVO,IGNORADO",
            "DUPLICADO,,DUPLICADO",
            "EXPIRADO,,EXPIRADO",
            "OUTRO_STATUS,,ERRO"
    })
    void ackAtualizaOStatusDoComando(String ackStatus, String message, DeviceCommandStatus expected) {
        DeviceCommandResponse sent = service.startRefill(patient.getId(), 5);

        ackService.handle(DEVICE_ID, ack(sent.commandId(), "MODO_REABASTECER", ackStatus, message));
        flushAndClear();

        DeviceCommandResponse answered = service.get(patient.getId(), sent.commandId());
        assertEquals(expected, answered.status());
        assertEquals(message, answered.message());
        assertNotNull(answered.answeredAt());
    }

    @Test
    void semAckEm30sViraSemResposta() {
        commandRepository.save(new DeviceCommand("cmd-velho001", device, "LOCALIZAR", Instant.now().minusSeconds(31)));
        commandRepository.save(new DeviceCommand("cmd-novo0001", device, "LOCALIZAR", Instant.now()));
        flushAndClear();

        service.markUnanswered();
        flushAndClear();

        assertEquals(DeviceCommandStatus.SEM_RESPOSTA, commandRepository.findById("cmd-velho001").orElseThrow().getStatus());
        assertEquals(DeviceCommandStatus.PENDENTE, commandRepository.findById("cmd-novo0001").orElseThrow().getStatus());
    }

    @Test
    void ackDeOutraGavetaNaoMudaOComando() {
        DeviceCommandResponse sent = service.locate(patient.getId());

        ackService.handle("0123456789AB", ack(sent.commandId(), "LOCALIZAR", "OK", null).replace(DEVICE_ID, "0123456789AB"));
        flushAndClear();

        assertEquals(DeviceCommandStatus.PENDENTE, service.get(patient.getId(), sent.commandId()).status());
    }

    @Test
    void fuiEuReclassificaAAberturaEReconheceNaGaveta() {
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000137", deviceRepository.getReferenceById(DEVICE_ID),
                "ABERTURA_FORA_HORARIO", Instant.now(), true, Instant.now(), "{\"dados\":{}}"));
        flushAndClear();

        service.acknowledgeAlert(patient.getId(), "A4CF12B3C4D5-00000137");
        flushAndClear();

        DeviceEvent event = eventRepository.findById("A4CF12B3C4D5-00000137").orElseThrow();
        assertTrue(event.isAcknowledged());
        assertEquals(DeviceEventClassification.CAREGIVER, event.getClassification());
        DeviceCommandMessage message = publishedCommand();
        assertEquals("RECONHECER_ALERTA", message.tipo());
        assertEquals("A4CF12B3C4D5-00000137", message.parametros().get("eventId"));
    }

    @Test
    void cienteEmOutroAlertaNaoReclassifica() {
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000140", deviceRepository.getReferenceById(DEVICE_ID),
                "GAVETA_ESQUECIDA_ABERTA", Instant.now(), true, Instant.now(), "{\"dados\":{}}"));
        flushAndClear();

        service.acknowledgeAlert(patient.getId(), "A4CF12B3C4D5-00000140");
        flushAndClear();

        DeviceEvent event = eventRepository.findById("A4CF12B3C4D5-00000140").orElseThrow();
        assertTrue(event.isAcknowledged());
        assertNull(event.getClassification());
    }

    @Test
    void alertaJaResolvidoOuQueNaoEhAlertaDa404() {
        eventRepository.save(new DeviceEvent("A4CF12B3C4D5-00000150", deviceRepository.getReferenceById(DEVICE_ID),
                "DOSE_CONFIRMADA", Instant.now(), true, Instant.now(), "{\"dados\":{}}"));
        flushAndClear();

        assertThrows(ResourceNotFoundException.class,
                () -> service.acknowledgeAlert(patient.getId(), "A4CF12B3C4D5-00000150"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.acknowledgeAlert(patient.getId(), "A4CF12B3C4D5-99999999"));
        verifyNoInteractions(publisher);
    }

    @Test
    void pacienteSemGavetaNaoEnviaComando() {
        deviceRepository.findById(DEVICE_ID).orElseThrow().setPatient(null);
        flushAndClear();

        assertThrows(ResourceNotFoundException.class, () -> service.locate(patient.getId()));
        verifyNoInteractions(publisher);
    }

    private DeviceCommandMessage publishedCommand() {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(eq(COMMAND_TOPIC), json.capture(), eq(1), eq(false));
        return jsonMapper.readValue(json.getValue(), DeviceCommandMessage.class);
    }

    private static String ack(String commandId, String type, String status, String message) {
        String mensagem = message == null ? "null" : "\"" + message + "\"";
        return "{\"commandId\":\"" + commandId + "\",\"deviceId\":\"" + DEVICE_ID + "\",\"tipo\":\"" + type + "\","
                + "\"status\":\"" + status + "\",\"mensagem\":" + mensagem + ",\"ts\":1790250002}";
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
