package br.edu.remediot.dto.device;

/**
 * Heartbeat, LWT e despedida (CONTRATO-MQTT.md, seção 6). No LWT e na despedida
 * só vêm online=false, deviceId e motivo.
 */
public record DeviceStatusMessage(
        Boolean online,
        String deviceId,
        Long ts,
        Long uptimeSeg,
        Integer rssi,
        String fw,
        String fonteHora,
        Long ultimaSyncNtp,
        String gaveta,
        Boolean alarmeAtivo,
        Integer alertasPendentes,
        Boolean modoReabastecer,
        Long agendaVersao,
        Integer filaOffline,
        Integer filaDescartados,
        Long heapLivre,
        String motivoReset,
        String motivo
) {

    public static final String REASON_CONNECTION_LOST = "CONEXAO_PERDIDA";
    public static final String REASON_RESTART = "REINICIO_SOLICITADO";
    public static final String REASON_WIFI_CHANGE = "TROCANDO_WIFI";
}
