package br.mackenzie.remediot.servico;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Atalho para serializar sem exceção checada. */
final class Json {

    private Json() {
    }

    static String escrever(ObjectMapper mapper, Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao gerar JSON", e);
        }
    }
}
