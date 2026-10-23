package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.StatusDispositivo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Heartbeat e LWT (o tópico de status é retained: o último valor sempre está lá). */
@Service
public class StatusService {

    private static final Logger log = LoggerFactory.getLogger(StatusService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final NotificacaoService notificacoes;

    public StatusService(JdbcTemplate jdbc, ObjectMapper mapper, NotificacaoService notificacoes) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.notificacoes = notificacoes;
    }

    public void processar(String deviceId, String payload) {
        StatusDispositivo status;
        try {
            status = mapper.readValue(payload, StatusDispositivo.class);
        } catch (JsonProcessingException e) {
            log.warn("Status inválido de {}: {}", deviceId, e.getOriginalMessage());
            return;
        }
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        int alterados = jdbc.update(
                "UPDATE dispositivo_status SET online = ?, payload = ?, atualizado_em = ? WHERE device_id = ?",
                status.online(), payload, agora, deviceId);
        if (alterados == 0) {
            jdbc.update("INSERT INTO dispositivo_status (device_id, online, payload, atualizado_em) VALUES (?, ?, ?, ?)",
                    deviceId, status.online(), payload, agora);
        }

        if (!status.online() && "CONEXAO_PERDIDA".equals(status.motivo())) {
            notificacoes.dispositivoOffline(deviceId, status.motivo());
        } else if (status.online() && nulo(status.filaOffline()) > 0) {
            log.info("{} online com {} evento(s) na fila offline", deviceId, status.filaOffline());
        }
    }

    private static Integer nulo(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
