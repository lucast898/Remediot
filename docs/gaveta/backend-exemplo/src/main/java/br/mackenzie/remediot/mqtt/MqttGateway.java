package br.mackenzie.remediot.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/** Porta de saída: o Spring gera a implementação e manda para o canal mqttSaida. */
@MessagingGateway(defaultRequestChannel = "mqttSaida")
public interface MqttGateway {

    void publicar(@Header(MqttHeaders.TOPIC) String topico,
                  @Header(MqttHeaders.QOS) int qos,
                  @Header(MqttHeaders.RETAINED) boolean retained,
                  String payload);
}
