package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "opening_event")
public class OpeningEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dose_id", nullable = false)
    private Dose dose;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OpeningEventSource source;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OpeningEvent() {
    }

    public OpeningEvent(Dose dose, LocalDateTime occurredAt, OpeningEventSource source) {
        this.dose = dose;
        this.occurredAt = occurredAt;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Dose getDose() {
        return dose;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public OpeningEventSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
