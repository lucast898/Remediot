package br.mackenzie.remediot.dto;

/** Comandos aceitos pelo ESP (tópico remediot/v1/{deviceId}/comando). */
public enum TipoComando {
    RESSINCRONIZAR_AGENDA,
    MODO_REABASTECER,     // parametros: acao = INICIAR | ENCERRAR, duracaoMin (1–60, opcional)
    RECONHECER_ALERTA,    // parametros: eventId  ("fui eu" / "ciente")
    LOCALIZAR,            // parametros: duracaoSeg (1–60, opcional)
    SILENCIAR_ALARME,
    PEDIR_STATUS,
    REINICIAR,
    RESETAR_WIFI
}
