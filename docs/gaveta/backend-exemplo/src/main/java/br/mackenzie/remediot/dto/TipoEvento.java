package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/** Tipos de evento publicados pelo ESP32 (ver CONTRATO-MQTT.md). */
public enum TipoEvento {
    DISPOSITIVO_INICIADO,
    ALARME_INICIADO,
    DOSE_ANTECIPADA,
    DOSE_CONFIRMADA,
    DOSE_EM_ATRASO,
    DOSE_ATRASADA,
    DOSE_PERDIDA,
    ALARME_SILENCIADO,
    ABERTURA_FORA_HORARIO,
    POSSIVEL_DOSE_DUPLA,
    ABERTURA_NAO_CLASSIFICADA,
    GAVETA_ESQUECIDA_ABERTA,
    GAVETA_FECHADA_APOS_ALERTA,
    REABASTECIMENTO,
    MODO_REABASTECER_INICIADO,
    MODO_REABASTECER_ENCERRADO,
    FALHA_RELOGIO,
    RELOGIO_RESTABELECIDO,

    /** Tipo que este backend ainda não conhece (firmware mais novo). Não quebra a leitura. */
    @JsonEnumDefaultValue
    DESCONHECIDO;

    /** Eventos que viram notificação imediata para o cuidador. */
    public boolean alertaCuidador() {
        return switch (this) {
            case DOSE_EM_ATRASO, DOSE_PERDIDA, ABERTURA_FORA_HORARIO, POSSIVEL_DOSE_DUPLA,
                 GAVETA_ESQUECIDA_ABERTA, FALHA_RELOGIO -> true;
            default -> false;
        };
    }
}
