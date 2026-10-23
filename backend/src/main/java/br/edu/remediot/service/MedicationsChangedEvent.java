package br.edu.remediot.service;

/**
 * Os remédios ou horários de um paciente mudaram. A agenda da gaveta é
 * republicada depois do commit (DeviceAgendaService).
 */
public record MedicationsChangedEvent(Long patientId) {
}
