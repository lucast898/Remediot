package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro de um lembrete "faltam X" já disparado para uma Dose. Persistido para
 * que o mesmo lembrete não seja repetido após reiniciar o backend.
 */
@Entity
@Table(name = "dose_reminder", uniqueConstraints = @UniqueConstraint(columnNames = {"dose_id", "offset_seconds"}))
public class DoseReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dose_id", nullable = false)
    private Dose dose;

    @Column(name = "offset_seconds", nullable = false)
    private long offsetSeconds;

    @Column(nullable = false, updatable = false)
    private Instant sentAt = Instant.now();

    protected DoseReminder() {
    }

    public DoseReminder(Dose dose, long offsetSeconds) {
        this.dose = dose;
        this.offsetSeconds = offsetSeconds;
    }

    public Long getId() {
        return id;
    }

    public Dose getDose() {
        return dose;
    }

    public long getOffsetSeconds() {
        return offsetSeconds;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
