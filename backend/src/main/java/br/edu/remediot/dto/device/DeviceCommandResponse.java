package br.edu.remediot.dto.device;

import br.edu.remediot.entity.DeviceCommand;
import br.edu.remediot.entity.DeviceCommandStatus;

import java.time.Instant;

/**
 * Comando enviado à gaveta. status começa PENDENTE; o app consulta de novo até
 * virar OK, ERRO, IGNORADO, DUPLICADO, EXPIRADO ou SEM_RESPOSTA. message traz o
 * motivo da gaveta (ex.: DOSE_EM_ANDAMENTO).
 */
public record DeviceCommandResponse(
        String commandId,
        String type,
        DeviceCommandStatus status,
        String message,
        Instant sentAt,
        Instant answeredAt
) {

    public static DeviceCommandResponse from(DeviceCommand command) {
        return new DeviceCommandResponse(command.getCommandId(), command.getType(), command.getStatus(),
                command.getMessage(), command.getSentAt(), command.getAnsweredAt());
    }
}
