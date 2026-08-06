package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dose", uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "scheduled_date"}))
public class Dose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DoseStatus status = DoseStatus.AGENDADA;

    private LocalDateTime registeredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Dose() {
    }

    public Dose(MedicationSchedule schedule, LocalDate scheduledDate, LocalDateTime scheduledAt) {
        this.schedule = schedule;
        this.scheduledDate = scheduledDate;
        this.scheduledAt = scheduledAt;
    }

    public Long getId() {
        return id;
    }

    public MedicationSchedule getSchedule() {
        return schedule;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
