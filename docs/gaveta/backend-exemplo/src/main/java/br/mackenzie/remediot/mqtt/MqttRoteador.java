package br.mackenzie.remediot.mqtt;

import br.mackenzie.remediot.servico.ComandoService;
import br.mackenzie.remediot.servico.EventoService;
import br.mackenzie.remediot.servico.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** Recebe tudo o que chega do broker e manda para o serviço certo pelo tópico. */
@Component
public class MqttRoteador {

    private static final Logger log = LoggerFactory.getLogger(MqttRoteador.class);

    private final EventoService eventos;
    private final StatusService status;
    private final ComandoService comandos;

    public MqttRoteador(EventoService eventos, StatusService status, ComandoService comandos) {
        this.eventos = eventos;
        this.status = status;
        this.comandos = comandos;
    }

    @ServiceActivator(inputChannel = "mqttEntrada")
    public void receber(Message<String> mensagem) {
        String topico = mensagem.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = mensagem.getPayload();
        try {
            switch (Topicos.canalDe(topico)) {
                case "eventos" -> eventos.processar(Topicos.deviceIdDe(topico), payload);
                case "status" -> status.processar(Topicos.deviceIdDe(topico), payload);
                case "ack" -> comandos.processarAck(payload);
                default -> log.warn("Tópico inesperado: {}", topico);
            }
        } catch (RuntimeException e) {
            // Não deixar a exceção subir: no Paho ela derruba a conexão MQTT.
            // Sem confirmação, o ESP reenvia o evento depois.
            log.error("Erro ao processar {}: {}", topico, e.getMessage(), e);
        }
    }
}
