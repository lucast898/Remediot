package br.edu.remediot.dto.device;

import java.time.Instant;
import java.util.List;

/**
 * Estado da gaveta para a tela do paciente. paired=false quando o paciente não
 * tem gaveta (os demais campos vêm nulos).
 *
 * agendaStatus: SINCRONIZADA (a gaveta aplicou a última agenda), AGUARDANDO (enviada,
 * sem resposta), REJEITADA (a gaveta ou o app recusou; ver agendaErrors) ou SEM_AGENDA.
 */
public record DeviceResponse(
        boolean paired,
        String deviceId,
        Boolean online,
        String offlineReason,
        Instant lastSeenAt,
        Instant pairedAt,
        String firmwareVersion,
        String timeSource,
        Integer offlineQueueSize,
        Integer rssi,
        String drawer,
        Boolean alarmActive,
        Boolean refillMode,
        long pendingAlerts,
        String agendaStatus,
        Long agendaSentVersion,
        Long agendaAppliedVersion,
        List<String> agendaErrors
) {

    public static DeviceResponse notPaired() {
        return new DeviceResponse(false, null, null, null, null, null, null, null, null, null, null, null, null,
                0, null, null, null, List.of());
    }
}
