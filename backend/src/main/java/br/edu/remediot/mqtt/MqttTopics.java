package br.edu.remediot.mqtt;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tópicos do docs/gaveta/CONTRATO-MQTT.md, seção 2: remediot/v1/{deviceId}/... */
public final class MqttTopics {

    public static final String ALL_EVENTS = "remediot/v1/+/eventos";
    public static final String ALL_STATUS = "remediot/v1/+/status";
    public static final String ALL_ACKS = "remediot/v1/+/ack";

    private static final Pattern INBOUND = Pattern.compile("^remediot/v1/([0-9A-F]{12})/(eventos|status|ack)$");

    private MqttTopics() {
    }

    public enum Kind {
        EVENTS,
        STATUS,
        ACK
    }

    public record Inbound(String deviceId, Kind kind) {
    }

    /** Vazio quando o tópico não é um dos três que o backend assina ou o deviceId é inválido. */
    public static Optional<Inbound> parseInbound(String topic) {
        if (topic == null) {
            return Optional.empty();
        }
        Matcher matcher = INBOUND.matcher(topic);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Kind kind = switch (matcher.group(2)) {
            case "eventos" -> Kind.EVENTS;
            case "status" -> Kind.STATUS;
            default -> Kind.ACK;
        };
        return Optional.of(new Inbound(matcher.group(1), kind));
    }

    public static String eventConfirmation(String deviceId) {
        return "remediot/v1/" + deviceId + "/eventos/confirmacao";
    }

    public static String agenda(String deviceId) {
        return "remediot/v1/" + deviceId + "/config/agenda";
    }

    public static String command(String deviceId) {
        return "remediot/v1/" + deviceId + "/comando";
    }
}
