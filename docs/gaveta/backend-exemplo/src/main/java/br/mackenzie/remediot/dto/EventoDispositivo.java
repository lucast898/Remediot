package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Envelope comum a todos os eventos (tópico remediot/v1/{deviceId}/eventos).
 * O conteúdo específico de cada tipo vem em {@code dados}.
 *
 * @param eventId    "{deviceId}-{contador}", único: chave de idempotência
 * @param ts         epoch UTC em segundos; null se o ESP não tinha hora
 * @param tsEstimado true se o ts foi calculado depois, pelo uptime
 * @param reenvio    true se o ESP já tinha publicado este evento antes
 */
public record EventoDispositivo(
        String eventId,
        String deviceId,
        TipoEvento tipo,
        Long ts,
        String fonteHora,
        Boolean tsEstimado,
        Long uptimeMs,
        Long boot,
        String fw,
        Boolean reenvio,
        JsonNode dados) {

    /** Quando aconteceu. Sem hora do ESP, a melhor aproximação é a chegada. */
    public Instant ocorridoEm(Instant recebidoEm) {
        return ts != null ? Instant.ofEpochSecond(ts) : recebidoEm;
    }

    public boolean horaConfiavel() {
        return ts != null;
    }

    /** Converte {@code dados} para um record específico, ex.: {@link DadosDose}. */
    public <T> T dadosComo(ObjectMapper mapper, Class<T> tipo) {
        return mapper.convertValue(dados, tipo);
    }
}
