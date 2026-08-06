package br.edu.remediot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "medication_schedule")
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MedicationSchedule() {
    }

    public MedicationSchedule(Medication medication, LocalTime time) {
        this.medication = medication;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public Medication getMedication() {
        return medication;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
