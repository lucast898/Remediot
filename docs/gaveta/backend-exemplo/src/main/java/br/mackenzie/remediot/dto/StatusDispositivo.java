package br.mackenzie.remediot.dto;

/**
 * Heartbeat (online=true) ou despedida/LWT (online=false) no tópico
 * remediot/v1/{deviceId}/status (retained). No LWT só vêm online, deviceId e motivo.
 */
public record StatusDispositivo(
        boolean online,
        String deviceId,
        Long ts,
        Long uptimeSeg,
        Integer rssi,
        String fw,
        String fonteHora,          // NTP ou NENHUMA
        Long ultimaSyncNtp,
        String gaveta,             // ABERTA ou FECHADA
        Boolean alarmeAtivo,
        Integer alertasPendentes,
        Boolean modoReabastecer,
        Long agendaVersao,
        Integer filaOffline,
        Long filaDescartados,
        Long heapLivre,
        String motivoReset,
        String motivo) {           // só no offline: CONEXAO_PERDIDA, REINICIO_SOLICITADO ou TROCANDO_WIFI
}
