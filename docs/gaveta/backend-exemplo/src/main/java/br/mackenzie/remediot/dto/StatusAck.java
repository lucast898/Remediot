package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum StatusAck {
    OK, ERRO, EXPIRADO, DUPLICADO, IGNORADO,
    /** Só para agenda */
    APLICADA, REJEITADA,
    @JsonEnumDefaultValue
    DESCONHECIDO
}
