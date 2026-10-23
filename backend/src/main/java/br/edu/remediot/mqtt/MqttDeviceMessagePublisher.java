package br.edu.remediot.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

/** Publica no EMQX pelo canal de saída do Spring Integration (ver MqttConfig). */
public class MqttDeviceMessagePublisher implements DeviceMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceMessagePublisher.class);

    private final MessageChannel outbound;

    public MqttDeviceMessagePublisher(MessageChannel outbound) {
        this.outbound = outbound;
    }

    @Override
    public void publish(String topic, String payload, int qos, boolean retained) {
        outbound.send(MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .setHeader(MqttHeaders.RETAINED, retained)
                .build());
        log.debug("MQTT -> {} (qos={}, retained={}): {}", topic, qos, retained, payload);
    }
}
