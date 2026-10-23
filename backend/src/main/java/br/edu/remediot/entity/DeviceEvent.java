package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Evento recebido da gaveta, guardado como veio. O eventId é a chave de
 * idempotência do contrato: a gaveta reenvia até receber a confirmação.
 */
@Entity
@Table(name = "device_event")
public class DeviceEvent {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /** String, e não enum, para um tipo novo do firmware não quebrar a leitura. */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false)
    private Instant occurredAt;

    /** false quando o ts veio null e occurredAt é o horário de chegada. */
    @Column(nullable = false)
    private boolean timeReliable;

    @Column(nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeviceEventClassification classification;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    protected DeviceEvent() {
    }

    public DeviceEvent(String eventId, Device device, String type, Instant occurredAt, boolean timeReliable,
                       Instant receivedAt, String payload) {
        this.eventId = eventId;
        this.device = device;
        this.type = type;
        this.occurredAt = occurredAt;
        this.timeReliable = timeReliable;
        this.receivedAt = receivedAt;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public Device getDevice() {
        return device;
    }

    public String getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isTimeReliable() {
        return timeReliable;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public DeviceEventClassification getClassification() {
        return classification;
    }

    public void setClassification(DeviceEventClassification classification) {
        this.classification = classification;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public String getPayload() {
        return payload;
    }
}
