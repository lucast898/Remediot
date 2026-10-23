package br.edu.remediot.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttTopicsTest {

    @Test
    void reconheceOsTresTopicosDeEntrada() {
        assertEquals(new MqttTopics.Inbound("A4CF12B3C4D5", MqttTopics.Kind.EVENTS),
                MqttTopics.parseInbound("remediot/v1/A4CF12B3C4D5/eventos").orElseThrow());
        assertEquals(MqttTopics.Kind.STATUS,
                MqttTopics.parseInbound("remediot/v1/A4CF12B3C4D5/status").orElseThrow().kind());
        assertEquals(MqttTopics.Kind.ACK,
                MqttTopics.parseInbound("remediot/v1/A4CF12B3C4D5/ack").orElseThrow().kind());
    }

    @Test
    void recusaTopicoDeSaidaOuDeviceIdInvalido() {
        assertTrue(MqttTopics.parseInbound("remediot/v1/A4CF12B3C4D5/eventos/confirmacao").isEmpty());
        assertTrue(MqttTopics.parseInbound("remediot/v1/A4CF12B3C4D5/config/agenda").isEmpty());
        assertTrue(MqttTopics.parseInbound("remediot/v1/a4cf12b3c4d5/eventos").isEmpty());
        assertTrue(MqttTopics.parseInbound("remediot/v1/A4CF12/eventos").isEmpty());
        assertTrue(MqttTopics.parseInbound("remediot/v1/../eventos").isEmpty());
        assertTrue(MqttTopics.parseInbound(null).isEmpty());
    }

    @Test
    void montaTopicosDeSaida() {
        assertEquals("remediot/v1/A4CF12B3C4D5/eventos/confirmacao", MqttTopics.eventConfirmation("A4CF12B3C4D5"));
        assertEquals("remediot/v1/A4CF12B3C4D5/config/agenda", MqttTopics.agenda("A4CF12B3C4D5"));
        assertEquals("remediot/v1/A4CF12B3C4D5/comando", MqttTopics.command("A4CF12B3C4D5"));
    }
}
