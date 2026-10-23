package br.mackenzie.remediot;

import br.mackenzie.remediot.dto.EventoDispositivo;
import br.mackenzie.remediot.mqtt.MqttGateway;
import br.mackenzie.remediot.servico.EventoService;
import br.mackenzie.remediot.servico.NotificacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Idempotência sem banco: um JdbcTemplate falso que imita a chave primária. */
class EventoServiceTest {

    private static final String EVENTO = """
            {"eventId":"A4CF12B3C4D5-00000042","deviceId":"A4CF12B3C4D5","tipo":"ABERTURA_FORA_HORARIO",
             "ts":1790262000,"fonteHora":"NTP","uptimeMs":5000,"boot":3,"fw":"2.1.0-mvp","reenvio":false,
             "dados":{"proximaDose":null}}""";

    private final Set<String> chaves = new HashSet<>();
    private final List<String> publicados = new ArrayList<>();
    private final List<String> alertas = new ArrayList<>();
    private EventoService servico;

    @BeforeEach
    void preparar() {
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                if (sql.contains("INSERT INTO evento") && !chaves.add((String) args[0])) {
                    throw new DuplicateKeyException("event_id duplicado");
                }
                return 1;
            }
        };
        MqttGateway mqtt = (topico, qos, retained, payload) -> publicados.add(topico + " " + payload);
        NotificacaoService notificacoes = new NotificacaoService() {
            @Override
            public void alertarCuidador(EventoDispositivo evento) {
                alertas.add(evento.eventId());
            }
        };
        servico = new EventoService(jdbc, new ObjectMapper(), mqtt, notificacoes);
    }

    @Test
    void eventoNovoEhConfirmadoENotificado() {
        servico.processar("A4CF12B3C4D5", EVENTO);
        assertEquals(List.of("remediot/v1/A4CF12B3C4D5/eventos/confirmacao {\"eventIds\":[\"A4CF12B3C4D5-00000042\"]}"),
                publicados);
        assertEquals(1, alertas.size());
    }

    @Test
    void reenvioEhConfirmadoDeNovoMasNaoNotificaDuasVezes() {
        servico.processar("A4CF12B3C4D5", EVENTO);
        servico.processar("A4CF12B3C4D5", EVENTO.replace("\"reenvio\":false", "\"reenvio\":true"));
        assertEquals(2, publicados.size());
        assertEquals(1, alertas.size());
    }

    @Test
    void deviceIdDiferenteDoTopicoEhDescartado() {
        servico.processar("OUTRO0000000", EVENTO);
        assertTrue(publicados.isEmpty());
    }

    @Test
    void jsonInvalidoNaoDerrubaNada() {
        servico.processar("A4CF12B3C4D5", "{isso nao e json");
        assertTrue(publicados.isEmpty());
    }
}
