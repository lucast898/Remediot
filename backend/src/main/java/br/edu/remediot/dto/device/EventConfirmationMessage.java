package br.edu.remediot.dto.device;

import java.util.List;

/** Confirmação de recebimento de eventos (CONTRATO-MQTT.md, seção 5). */
public record EventConfirmationMessage(List<String> eventIds) {
}
