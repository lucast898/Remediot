package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Comando enviado à gaveta (reabastecer, localizar, "fui eu"...) e a resposta dela. */
@Entity
@Table(name = "device_command")
public class DeviceCommand {

    @Id
    @Column(name = "command_id", length = 40)
    private String commandId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 40)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceCommandStatus status = DeviceCommandStatus.PENDENTE;

    private String message;

    @Column(nullable = false)
    private Instant sentAt;

    private Instant answeredAt;

    protected DeviceCommand() {
    }

    public DeviceCommand(String commandId, Device device, String type, Instant sentAt) {
        this.commandId = commandId;
        this.device = device;
        this.type = type;
        this.sentAt = sentAt;
    }

    public String getCommandId() {
        return commandId;
    }

    public Device getDevice() {
        return device;
    }

    public String getType() {
        return type;
    }

    public DeviceCommandStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceCommandStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
