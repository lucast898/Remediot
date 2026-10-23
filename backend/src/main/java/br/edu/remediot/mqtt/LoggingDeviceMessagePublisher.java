package br.edu.remediot.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "remediot.mqtt.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingDeviceMessagePublisher implements DeviceMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDeviceMessagePublisher.class);

    @Override
    public void publish(String topic, String payload, int qos, boolean retained) {
        log.info("[MQTT desligado] publicaria em {} (qos={}, retained={}): {}", topic, qos, retained, payload);
    }
}
