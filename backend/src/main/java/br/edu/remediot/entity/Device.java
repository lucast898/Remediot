package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Gaveta física (ESP32). O deviceId vem do MAC (12 hexadecimais) e é o mesmo que
 * aparece nos tópicos MQTT. Fica sem paciente até o pareamento (D7).
 */
@Entity
@Table(name = "device")
public class Device {

    @Id
    @Column(name = "device_id", length = 12)
    private String deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", unique = true)
    private Patient patient;

    @Column(nullable = false)
    private boolean online;

    private Instant lastSeenAt;

    @Column(length = 40)
    private String firmwareVersion;

    /** NTP ou NENHUMA, como vem no contrato. */
    @Column(length = 20)
    private String timeSource;

    private Integer offlineQueueSize;

    private Long appliedAgendaVersion;

    @Column(columnDefinition = "TEXT")
    private String lastStatusJson;

    private Instant pairedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Device() {
    }

    public Device(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(String timeSource) {
        this.timeSource = timeSource;
    }

    public Integer getOfflineQueueSize() {
        return offlineQueueSize;
    }

    public void setOfflineQueueSize(Integer offlineQueueSize) {
        this.offlineQueueSize = offlineQueueSize;
    }

    public Long getAppliedAgendaVersion() {
        return appliedAgendaVersion;
    }

    public void setAppliedAgendaVersion(Long appliedAgendaVersion) {
        this.appliedAgendaVersion = appliedAgendaVersion;
    }

    public String getLastStatusJson() {
        return lastStatusJson;
    }

    public void setLastStatusJson(String lastStatusJson) {
        this.lastStatusJson = lastStatusJson;
    }

    public Instant getPairedAt() {
        return pairedAt;
    }

    public void setPairedAt(Instant pairedAt) {
        this.pairedAt = pairedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
