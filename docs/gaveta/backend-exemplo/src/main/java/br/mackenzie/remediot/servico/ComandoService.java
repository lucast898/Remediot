package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.AckComando;
import br.mackenzie.remediot.dto.Comando;
import br.mackenzie.remediot.dto.TipoComando;
import br.mackenzie.remediot.mqtt.MqttGateway;
import br.mackenzie.remediot.mqtt.Topicos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Comandos para o ESP e o acompanhamento dos acks. */
@Service
public class ComandoService {

    private static final Logger log = LoggerFactory.getLogger(ComandoService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MqttGateway mqtt;
    private final EventoService eventos;
    private final AgendaService agendas;
    private final long validadeSeg;
    private final long timeoutAckSeg;

    public ComandoService(JdbcTemplate jdbc, ObjectMapper mapper, MqttGateway mqtt,
                          EventoService eventos, AgendaService agendas,
                          @Value("${remediot.comando.validade-seg:120}") long validadeSeg,
                          @Value("${remediot.comando.timeout-ack-seg:30}") long timeoutAckSeg) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.mqtt = mqtt;
        this.eventos = eventos;
        this.agendas = agendas;
        this.validadeSeg = validadeSeg;
        this.timeoutAckSeg = timeoutAckSeg;
    }

    /** Envia um comando e devolve o commandId (o ack chega depois, pelo MQTT). */
    public String enviar(String deviceId, TipoComando tipo, Map<String, Object> parametros) {
        String commandId = "cmd-" + UUID.randomUUID().toString().substring(0, 8);
        long agora = Instant.now().getEpochSecond();
        Comando comando = new Comando(commandId, tipo, agora, agora + validadeSeg,
                parametros == null ? Map.of() : parametros);

        jdbc.update("""
                INSERT INTO comando (command_id, device_id, tipo, status, enviado_em)
                VALUES (?, ?, ?, 'PENDENTE', ?)
                """, commandId, deviceId, tipo.name(), OffsetDateTime.now(ZoneOffset.UTC));

        // QoS 1 e NUNCA retained
        mqtt.publicar(Topicos.comando(deviceId), 1, false, Json.escrever(mapper, comando));
        log.info("Comando {} ({}) enviado para {}", tipo, commandId, deviceId);
        return commandId;
    }

    public String iniciarReabastecimento(String deviceId, Integer minutos) {
        Map<String, Object> p = new HashMap<>();
        p.put("acao", "INICIAR");
        if (minutos != null) {
            p.put("duracaoMin", minutos);
        }
        return enviar(deviceId, TipoComando.MODO_REABASTECER, p);
    }

    public String encerrarReabastecimento(String deviceId) {
        return enviar(deviceId, TipoComando.MODO_REABASTECER, Map.of("acao", "ENCERRAR"));
    }

    /**
     * "Fui eu": o cuidador abriu a gaveta e esqueceu de ativar o modo reabastecer.
     * 1) reclassifica o evento no banco; 2) manda o ESP apagar o alerta do LED.
     * Serve também como "ciente" para os outros alertas.
     */
    public String fuiEu(String deviceId, String eventId) {
        boolean achou = eventos.reclassificarComoCuidador(deviceId, eventId);
        if (!achou) {
            log.warn("'Fui eu' para evento desconhecido {} de {}", eventId, deviceId);
        }
        return enviar(deviceId, TipoComando.RECONHECER_ALERTA, Map.of("eventId", eventId));
    }

    public void processarAck(String payload) {
        AckComando ack;
        try {
            ack = mapper.readValue(payload, AckComando.class);
        } catch (JsonProcessingException e) {
            log.warn("Ack inválido: {}", e.getOriginalMessage());
            return;
        }
        if (ack.commandId() == null) {
            log.warn("Ack sem commandId (o ESP não conseguiu ler o comando): {}", payload);
            return;
        }
        jdbc.update("""
                UPDATE comando SET status = ?, mensagem = ?, respondido_em = ?
                WHERE command_id = ?
                """, ack.status().name(), ack.mensagem(), OffsetDateTime.now(ZoneOffset.UTC), ack.commandId());

        if ("AGENDA".equals(ack.tipo())) {
            agendas.processarAck(ack);
        }
        log.info("Ack {} de {}: {} {}", ack.commandId(), ack.deviceId(), ack.status(),
                ack.mensagem() == null ? "" : ack.mensagem());
    }

    /** Comando sem resposta no prazo: o app mostra "o aparelho não respondeu". */
    @Scheduled(fixedDelay = 10_000)
    public void marcarSemResposta() {
        OffsetDateTime limite = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutAckSeg);
        int n = jdbc.update("UPDATE comando SET status = 'SEM_RESPOSTA' WHERE status = 'PENDENTE' AND enviado_em < ?",
                limite);
        if (n > 0) {
            log.warn("{} comando(s) sem resposta do dispositivo", n);
        }
    }
}
