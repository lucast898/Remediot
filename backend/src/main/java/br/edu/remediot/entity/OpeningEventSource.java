package br.edu.remediot.entity;

/**
 * SIMULATOR = botão "Simular abertura da gaveta" (fase atual, sem hardware).
 * DEVICE = evento real do ESP32, entregue via Node-RED em POST /api/devices/events (fase futura).
 */
public enum OpeningEventSource {
    SIMULATOR,
    DEVICE
}
