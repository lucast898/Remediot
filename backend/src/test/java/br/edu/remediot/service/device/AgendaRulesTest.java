package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.AgendaMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Mesmas regras do AgendaValidador do exemplo e do firmware (CONTRATO-MQTT.md, seção 7). */
class AgendaRulesTest {

    private static final String SMALL_JSON = "{}";

    @Test
    void versaoSempreCresceEmSegundos() {
        assertEquals(1790244000L, AgendaRules.nextVersion(1790244000L, null));
        assertEquals(1790244000L, AgendaRules.nextVersion(1790244000L, 1790243000L));
        // Duas alterações no mesmo segundo: versão anterior + 1
        assertEquals(1790244001L, AgendaRules.nextVersion(1790244000L, 1790244000L));
    }

    @Test
    void agendaValidaNaoTemErros() {
        AgendaMessage agenda = new AgendaMessage(7, "cfg-2f9c1e0a", List.of(
                new AgendaMessage.Entry("s17", "08:00", 3),
                new AgendaMessage.Entry("s21", "08:00", 1)));

        assertTrue(AgendaRules.validate(agenda, SMALL_JSON).isEmpty());
    }

    @Test
    void agendaVaziaEhValida() {
        assertTrue(AgendaRules.validate(new AgendaMessage(7, "cfg-2f9c1e0a", List.of()), SMALL_JSON).isEmpty());
    }

    @Test
    void recusaMaisDeQuarentaEntradas() {
        List<AgendaMessage.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            entries.add(new AgendaMessage.Entry("s" + i, "08:00", 1));
        }

        assertEquals(List.of("DOSES_DEMAIS"),
                AgendaRules.validate(new AgendaMessage(7, "cfg-2f9c1e0a", entries), SMALL_JSON));
    }

    @Test
    void recusaEntradasInvalidas() {
        AgendaMessage agenda = new AgendaMessage(0, "", List.of(
                new AgendaMessage.Entry("s1", "8h", 1),
                new AgendaMessage.Entry("s1", "24:00", 4),
                new AgendaMessage.Entry("nome do remédio", "08:00", 2)));

        List<String> errors = AgendaRules.validate(agenda, SMALL_JSON);

        assertTrue(errors.containsAll(List.of("VERSAO_INVALIDA", "CONFIG_ID_INVALIDO", "HORA_INVALIDA",
                "DOSE_ID_DUPLICADO", "PESO_INVALIDO", "DOSE_ID_INVALIDO")), errors.toString());
    }

    @Test
    void recusaJsonAcimaDe7500Bytes() {
        AgendaMessage agenda = new AgendaMessage(7, "cfg-2f9c1e0a", List.of());

        assertEquals(List.of("AGENDA_GRANDE_DEMAIS"), AgendaRules.validate(agenda, "x".repeat(7_501)));
        assertTrue(AgendaRules.validate(agenda, "x".repeat(7_500)).isEmpty());
    }
}
