package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.ConfirmacaoEventos;
import br.mackenzie.remediot.dto.EventoDispositivo;
import br.mackenzie.remediot.mqtt.MqttGateway;
import br.mackenzie.remediot.mqtt.Topicos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Eventos do dispositivo, com idempotência pelo eventId.
 *
 * Fluxo: grava -> confirma -> notifica. A confirmação só sai depois que o evento
 * está salvo; se o banco falhar, não confirmamos e o ESP reenvia. Duplicados
 * (reenvios) também são confirmados, senão o ESP reenviaria para sempre.
 */
@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MqttGateway mqtt;
    private final NotificacaoService notificacoes;

    public EventoService(JdbcTemplate jdbc, ObjectMapper mapper, MqttGateway mqtt,
                         NotificacaoService notificacoes) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.mqtt = mqtt;
        this.notificacoes = notificacoes;
    }

    public void processar(String deviceIdDoTopico, String payload) {
        EventoDispositivo evento;
        try {
            evento = mapper.readValue(payload, EventoDispositivo.class);
        } catch (JsonProcessingException e) {
            log.warn("Evento com JSON inválido descartado: {}", e.getOriginalMessage());
            return;
        }
        if (evento.eventId() == null || evento.deviceId() == null || evento.tipo() == null) {
            log.warn("Evento sem eventId/deviceId/tipo descartado: {}", payload);
            return;
        }
        // Um dispositivo só pode publicar eventos em nome dele mesmo
        if (!evento.deviceId().equals(deviceIdDoTopico)) {
            log.warn("deviceId do evento ({}) diferente do tópico ({}): descartado",
                    evento.deviceId(), deviceIdDoTopico);
            return;
        }

        boolean novo = inserir(evento, payload, Instant.now());
        confirmar(evento.deviceId(), evento.eventId());

        if (!novo) {
            log.info("Evento duplicado ignorado: {} (reenvio={})", evento.eventId(), evento.reenvio());
            return;
        }
        log.info("Evento {} {} de {}", evento.tipo(), evento.eventId(), evento.deviceId());

        if (evento.tipo().alertaCuidador()) {
            notificacoes.alertarCuidador(evento);
        }
    }

    /** true = evento novo; false = já existia (mesmo eventId). */
    private boolean inserir(EventoDispositivo e, String payload, Instant recebidoEm) {
        try {
            jdbc.update("""
                    INSERT INTO evento (event_id, device_id, tipo, ocorrido_em, hora_confiavel,
                                        recebido_em, payload)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    e.eventId(), e.deviceId(), e.tipo().name(),
                    utc(e.ocorridoEm(recebidoEm)), e.horaConfiavel(), utc(recebidoEm), payload);
            return true;
        } catch (DuplicateKeyException duplicado) {
            return false;
        }
    }

    private void confirmar(String deviceId, String eventId) {
        String json = Json.escrever(mapper, new ConfirmacaoEventos(List.of(eventId)));
        mqtt.publicar(Topicos.confirmacao(deviceId), 1, false, json);
    }

    /** "Fui eu": marca a abertura como feita pelo cuidador. */
    public boolean reclassificarComoCuidador(String deviceId, String eventId) {
        return jdbc.update("""
                UPDATE evento SET classificacao = 'CUIDADOR'
                WHERE event_id = ? AND device_id = ?
                """, eventId, deviceId) > 0;
    }

    public List<Map<String, Object>> ultimos(String deviceId, int limite) {
        return jdbc.queryForList("""
                SELECT event_id, tipo, ocorrido_em, hora_confiavel, classificacao
                FROM evento WHERE device_id = ?
                ORDER BY ocorrido_em DESC LIMIT ?
                """, deviceId, limite);
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
