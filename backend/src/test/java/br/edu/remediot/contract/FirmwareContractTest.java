package br.edu.remediot.contract;

import br.edu.remediot.dto.device.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de contrato: lê os JSONs reais do firmware (docs/gaveta/backend-exemplo/
 * src/test/resources/firmware) com os records em modo ESTRITO. Se o firmware
 * mandar um campo que o backend não conhece, ou um tipo de evento novo, este
 * teste quebra: hora de conversar com quem cuida do firmware e atualizar o
 * CONTRATO-MQTT.md dos dois lados.
 */
class FirmwareContractTest {

    private static final Path FIRMWARE = Path.of("..", "docs", "gaveta", "backend-exemplo",
            "src", "test", "resources", "firmware");

    private final JsonMapper strict = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void eventosDoFirmware() throws IOException {
        List<JsonNode> events = readArray("eventos.json");
        assertFalse(events.isEmpty());

        for (JsonNode node : events) {
            DeviceEventMessage event = strict.treeToValue(node, DeviceEventMessage.class);
            assertNotNull(event.eventId(), node::toString);
            assertTrue(event.eventId().startsWith(event.deviceId() + "-"), node::toString);
            DeviceEventType type = DeviceEventType.from(event.tipo());
            assertNotEquals(DeviceEventType.UNKNOWN, type, () -> "tipo novo no firmware: " + event.tipo());

            if (type.hasDoseData()) {
                DoseEventData data = strict.treeToValue(event.dados(), DoseEventData.class);
                assertTrue(data.doseId().matches("s\\d+"), () -> "doseId fora do formato s<id>: " + data.doseId());
                assertNotNull(data.dataReferencia(), node::toString);
            }
        }
    }

    @Test
    void statusDoFirmware() throws IOException {
        for (JsonNode node : readArray("status.json")) {
            DeviceStatusMessage status = strict.treeToValue(node, DeviceStatusMessage.class);
            assertNotNull(status.online(), node::toString);
            if (!status.online()) {
                assertTrue(List.of(DeviceStatusMessage.REASON_CONNECTION_LOST, DeviceStatusMessage.REASON_RESTART,
                        DeviceStatusMessage.REASON_WIFI_CHANGE).contains(status.motivo()), node::toString);
            }
        }
    }

    @Test
    void acksDoFirmware() throws IOException {
        List<String> statuses = List.of("OK", "ERRO", "IGNORADO", "DUPLICADO", "EXPIRADO", "APLICADA", "REJEITADA");
        for (JsonNode node : readArray("acks.json")) {
            DeviceAckMessage ack = strict.treeToValue(node, DeviceAckMessage.class);
            assertTrue(statuses.contains(ack.status()), node::toString);
            if (ack.erros() != null) {
                ack.erros().forEach(error -> assertNotNull(error.codigo(), node::toString));
            }
        }
    }

    @Test
    void modoEstritoRecusaCampoDesconhecido() {
        String withExtraField = "{\"online\":true,\"deviceId\":\"A4CF12B3C4D5\",\"bateria\":80}";

        assertThrows(tools.jackson.core.JacksonException.class,
                () -> strict.readValue(withExtraField, DeviceStatusMessage.class));
    }

    @Test
    void agendaQueOBackendMontaPassaNasRegrasDoFirmware() {
        AgendaMessage agenda = new AgendaMessage(1790244000L, "cfg-2f9c1e0a", List.of(
                new AgendaMessage.Entry("s17", "08:00", 3),
                new AgendaMessage.Entry("s21", "08:00", 1)));
        String json = strict.writeValueAsString(agenda);

        assertTrue(AgendaRulesAccess.validate(agenda, json).isEmpty());
        JsonNode tree = strict.readTree(json);
        assertEquals(1790244000L, tree.get("versao").asLong());
        assertFalse(tree.get("doses").get(0).has("descricao"), "a agenda não leva nome de remédio (LGPD)");
        assertFalse(tree.get("doses").get(0).has("diasSemana"), "sem diasSemana: vale todos os dias (D2)");
    }

    private List<JsonNode> readArray(String file) throws IOException {
        JsonNode root = strict.readTree(Files.readString(FIRMWARE.resolve(file)));
        assertTrue(root.isArray(), file + " deveria ser uma lista");
        List<JsonNode> items = new ArrayList<>();
        root.forEach(items::add);
        return items;
    }

    /** AgendaRules fica no pacote do serviço; o acesso é só para o teste. */
    private static final class AgendaRulesAccess {
        static List<String> validate(AgendaMessage agenda, String json) {
            return br.edu.remediot.service.device.AgendaRules.validate(agenda, json);
        }
    }
}
