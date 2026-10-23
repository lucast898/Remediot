package br.edu.remediot.dto.device;

/**
 * Tipos de evento do CONTRATO-MQTT.md, seção 4.3. UNKNOWN cobre tipos que um
 * firmware mais novo venha a mandar: o evento é guardado e confirmado mesmo assim.
 */
public enum DeviceEventType {
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
    UNKNOWN;

    public static DeviceEventType from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /** Abertura que registra a dose (D1): o status sai do DoseStatusService. */
    public boolean isDoseOpening() {
        return this == DOSE_ANTECIPADA || this == DOSE_CONFIRMADA || this == DOSE_ATRASADA;
    }

    /** Eventos que não são de dose e viram alerta ao cuidador (D9). */
    public boolean isCaregiverAlert() {
        return this == ABERTURA_FORA_HORARIO || this == POSSIVEL_DOSE_DUPLA
                || this == GAVETA_ESQUECIDA_ABERTA || this == FALHA_RELOGIO;
    }

    /** Eventos cujo "dados" segue o formato de DoseEventData. */
    public boolean hasDoseData() {
        return isDoseOpening() || this == ALARME_INICIADO || this == DOSE_EM_ATRASO
                || this == DOSE_PERDIDA || this == ALARME_SILENCIADO || this == POSSIVEL_DOSE_DUPLA;
    }
}
