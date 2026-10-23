package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.AckComando;
import br.mackenzie.remediot.dto.Agenda;
import br.mackenzie.remediot.dto.DoseAgenda;
import br.mackenzie.remediot.mqtt.MqttGateway;
import br.mackenzie.remediot.mqtt.Topicos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Publica a agenda como mensagem RETAINED: o ESP recebe na hora se estiver
 * online, ou assim que reconectar. A versão só cresce (ver {@link Agenda}).
 */
@Service
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MqttGateway mqtt;

    public AgendaService(JdbcTemplate jdbc, ObjectMapper mapper, MqttGateway mqtt) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.mqtt = mqtt;
    }

    public Agenda publicar(String deviceId, List<DoseAgenda> doses) {
        List<String> erros = AgendaValidador.validar(doses);
        if (!erros.isEmpty()) {
            throw new AgendaInvalidaException(erros);
        }

        long versao = proximaVersao(deviceId);
        String configId = "cfg-" + UUID.randomUUID().toString().substring(0, 8);
        Agenda agenda = new Agenda(versao, configId, doses);
        String json = Json.escrever(mapper, agenda);

        // O ESP descarta em silêncio (sem ack) mensagem maior que o buffer do MQTT
        int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > AgendaValidador.MAX_BYTES) {
            throw new AgendaInvalidaException(List.of(
                    "AGENDA_GRANDE_DEMAIS: " + bytes + " bytes (máximo " + AgendaValidador.MAX_BYTES + ")"));
        }

        int alterados = jdbc.update(
                "UPDATE agenda SET versao = ?, config_id = ?, json = ?, status = 'ENVIADA' WHERE device_id = ?",
                versao, configId, json, deviceId);
        if (alterados == 0) {
            jdbc.update("INSERT INTO agenda (device_id, versao, config_id, json, status) VALUES (?, ?, ?, ?, 'ENVIADA')",
                    deviceId, versao, configId, json);
        }
        // o ack da agenda chega com commandId = configId
        jdbc.update("""
                INSERT INTO comando (command_id, device_id, tipo, status, enviado_em)
                VALUES (?, ?, 'AGENDA', 'PENDENTE', ?)
                """, configId, deviceId, OffsetDateTime.now(ZoneOffset.UTC));

        mqtt.publicar(Topicos.agenda(deviceId), 1, true, json);   // retained + QoS 1
        log.info("Agenda v{} ({}) publicada para {}", versao, configId, deviceId);
        return agenda;
    }

    public void processarAck(AckComando ack) {
        jdbc.update("UPDATE agenda SET status = ? WHERE config_id = ?", ack.status().name(), ack.commandId());
        if (ack.erros() != null && !ack.erros().isEmpty()) {
            log.warn("Agenda {} rejeitada por {}: {}", ack.commandId(), ack.deviceId(), ack.erros());
        }
    }

    /**
     * Instante atual em segundos, ou a versão gravada + 1 se o relógio estiver
     * atrás dela. Assim a versão cresce mesmo depois de apagar o banco (o H2
     * deste exemplo é em memória e volta vazio a cada reinício do backend).
     */
    private long proximaVersao(String deviceId) {
        List<Long> atual = jdbc.queryForList("SELECT versao FROM agenda WHERE device_id = ?",
                Long.class, deviceId);
        long agora = Instant.now().getEpochSecond();
        return atual.isEmpty() ? agora : Math.max(agora, atual.get(0) + 1);
    }
}
