package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Cada agenda publicada para a gaveta. A versão só cresce: a gaveta recusa versão
 * menor que a que já tem (ver docs/gaveta/CONTRATO-MQTT.md, seção 7).
 */
@Entity
@Table(name = "device_agenda")
public class DeviceAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private Long version;

    /** Volta no ack como commandId. */
    @Column(nullable = false, unique = true, length = 40)
    private String configId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String json;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceAgendaStatus status = DeviceAgendaStatus.ENVIADA;

    /** Erros do ack de agenda rejeitada, como vieram (JSON). */
    @Column(columnDefinition = "TEXT")
    private String errors;

    @Column(nullable = false, updatable = false)
    private Instant sentAt = Instant.now();

    private Instant answeredAt;

    protected DeviceAgenda() {
    }

    public DeviceAgenda(Device device, Long version, String configId, String json) {
        this.device = device;
        this.version = version;
        this.configId = configId;
        this.json = json;
    }

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public Long getVersion() {
        return version;
    }

    public String getConfigId() {
        return configId;
    }

    public String getJson() {
        return json;
    }

    public DeviceAgendaStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceAgendaStatus status) {
        this.status = status;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
