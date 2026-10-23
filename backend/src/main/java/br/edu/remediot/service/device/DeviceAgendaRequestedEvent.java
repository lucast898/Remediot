package br.edu.remediot.service.device;

/**
 * Pede a publicação da agenda da gaveta depois do commit (pareamento,
 * desvinculação). empty = agenda vazia, para a gaveta parar de acompanhar doses.
 */
public record DeviceAgendaRequestedEvent(String deviceId, boolean empty) {
}
