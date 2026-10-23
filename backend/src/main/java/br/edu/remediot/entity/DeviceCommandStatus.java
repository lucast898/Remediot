package br.edu.remediot.entity;

/** PENDENTE até o ack chegar; SEM_RESPOSTA quando o ack não chega a tempo. */
public enum DeviceCommandStatus {
    PENDENTE,
    OK,
    ERRO,
    IGNORADO,
    DUPLICADO,
    EXPIRADO,
    SEM_RESPOSTA
}
