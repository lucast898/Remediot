package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.AgendaMessage;
import br.edu.remediot.entity.MedicationSchedule;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Montagem e validação da agenda com as mesmas regras do firmware
 * (CONTRATO-MQTT.md, seção 7), para recusar no app em vez de a gaveta rejeitar.
 */
public final class AgendaRules {

    /** Limite de memória da gaveta (D3). Checado só aqui, não no cadastro de remédios. */
    public static final int MAX_DEVICE_SCHEDULES = 40;

    /** O buffer MQTT da gaveta tem 8 KB; acima disso ela descarta sem ack. */
    public static final int MAX_AGENDA_BYTES = 7_500;

    public static final long MAX_VERSION = 4_294_967_295L;

    private static final Pattern DOSE_ID = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");
    private static final Pattern HOUR = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private AgendaRules() {
    }

    /** Uma entrada por MedicationSchedule (D2): doseId = "s" + id, peso do remédio. */
    public static List<AgendaMessage.Entry> entriesFor(Collection<MedicationSchedule> schedules) {
        return schedules.stream()
                .sorted(Comparator.comparing(MedicationSchedule::getTime).thenComparing(MedicationSchedule::getId))
                .map(s -> new AgendaMessage.Entry(
                        doseIdOf(s), s.getTime().format(HH_MM), s.getMedication().getWeight()))
                .toList();
    }

    public static String doseIdOf(MedicationSchedule schedule) {
        return "s" + schedule.getId();
    }

    /** Sempre cresce, em segundos, mesmo se o banco for apagado (a gaveta recusa versão menor). */
    public static long nextVersion(long nowEpochSeconds, Long previousVersion) {
        long afterPrevious = previousVersion == null ? 1 : previousVersion + 1;
        return Math.max(nowEpochSeconds, afterPrevious);
    }

    /** Códigos de erro iguais aos do ack da gaveta. Lista vazia = agenda válida. */
    public static List<String> validate(AgendaMessage agenda, String json) {
        List<String> errors = new ArrayList<>();
        if (agenda.versao() <= 0 || agenda.versao() > MAX_VERSION) {
            errors.add("VERSAO_INVALIDA");
        }
        if (agenda.configId() == null || agenda.configId().isEmpty() || agenda.configId().length() > 40) {
            errors.add("CONFIG_ID_INVALIDO");
        }
        if (agenda.doses() == null) {
            errors.add("DOSES_AUSENTE");
            return errors;
        }
        if (agenda.doses().size() > MAX_DEVICE_SCHEDULES) {
            errors.add("DOSES_DEMAIS");
        }
        Set<String> seen = new HashSet<>();
        for (AgendaMessage.Entry entry : agenda.doses()) {
            if (entry.doseId() == null || !DOSE_ID.matcher(entry.doseId()).matches()) {
                errors.add("DOSE_ID_INVALIDO");
            } else if (!seen.add(entry.doseId())) {
                errors.add("DOSE_ID_DUPLICADO");
            }
            if (entry.hora() == null || !HOUR.matcher(entry.hora()).matches()) {
                errors.add("HORA_INVALIDA");
            }
            if (entry.peso() != null && (entry.peso() < 1 || entry.peso() > 3)) {
                errors.add("PESO_INVALIDO");
            }
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_AGENDA_BYTES) {
            errors.add("AGENDA_GRANDE_DEMAIS");
        }
        return errors.stream().distinct().toList();
    }
}
