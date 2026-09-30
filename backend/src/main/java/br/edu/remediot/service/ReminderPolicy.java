package br.edu.remediot.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decide qual lembrete "faltam X" deve disparar agora para uma dose.
 *
 * Regra: entre os lembretes cujo momento já chegou, vale só o mais próximo do
 * horário. Assim, se o backend ficar fora do ar (ou a dose for criada em cima
 * da hora), o paciente ouve um único aviso atualizado em vez de três seguidos.
 */
public class ReminderPolicy {

    private final List<Duration> offsetsAscending;

    public ReminderPolicy(List<Duration> offsets) {
        if (offsets == null || offsets.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um lembrete");
        }
        if (offsets.stream().anyMatch(offset -> offset.isNegative() || offset.isZero())) {
            throw new IllegalArgumentException("Lembretes devem ser positivos: " + offsets);
        }
        this.offsetsAscending = offsets.stream().distinct().sorted().toList();
    }

    public Duration maxOffset() {
        return offsetsAscending.get(offsetsAscending.size() - 1);
    }

    public Optional<Duration> dueOffset(LocalDateTime scheduledAt, LocalDateTime now, Set<Duration> alreadySent) {
        if (!now.isBefore(scheduledAt)) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(now, scheduledAt);
        return offsetsAscending.stream()
                .filter(offset -> remaining.compareTo(offset) <= 0)
                .min(Comparator.naturalOrder())
                .filter(offset -> !alreadySent.contains(offset));
    }

    /** Texto em pt-BR falado pela Alexa. Evita afirmar ingestão (ver CLAUDE.md). */
    public static String speechFor(String patientFirstName, String medicationName, String medicationDose, Duration offset) {
        return "Atenção, %s. Faltam %s para o remédio %s, %s."
                .formatted(patientFirstName, describe(offset), medicationName, medicationDose);
    }

    static String describe(Duration offset) {
        long seconds = offset.toSeconds();
        if (seconds % 60 == 0) {
            long minutes = seconds / 60;
            return minutes == 1 ? "1 minuto" : minutes + " minutos";
        }
        return seconds == 1 ? "1 segundo" : seconds + " segundos";
    }
}
