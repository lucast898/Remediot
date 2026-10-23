package br.mackenzie.remediot.mqtt;

/** Nomes dos tópicos (mesmos do firmware e do CONTRATO-MQTT.md). */
public final class Topicos {

    public static final String PREFIXO = "remediot/v1/";

    // O backend assina estes três (+ = qualquer dispositivo)
    public static final String TODOS_EVENTOS = PREFIXO + "+/eventos";
    public static final String TODOS_STATUS = PREFIXO + "+/status";
    public static final String TODOS_ACKS = PREFIXO + "+/ack";

    private Topicos() {
    }

    public static String confirmacao(String deviceId) {
        return PREFIXO + deviceId + "/eventos/confirmacao";
    }

    public static String agenda(String deviceId) {
        return PREFIXO + deviceId + "/config/agenda";
    }

    public static String comando(String deviceId) {
        return PREFIXO + deviceId + "/comando";
    }

    /** "remediot/v1/A4CF12B3C4D5/eventos" -> "A4CF12B3C4D5" */
    public static String deviceIdDe(String topico) {
        String[] partes = topico.split("/");
        return partes.length >= 4 ? partes[2] : null;
    }

    /** "remediot/v1/A4CF12B3C4D5/eventos" -> "eventos" */
    public static String canalDe(String topico) {
        String[] partes = topico.split("/");
        return partes.length >= 4 ? partes[3] : "";
    }
}
