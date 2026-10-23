package br.edu.remediot.mqtt;

/**
 * Saída para a gaveta. Com remediot.mqtt.enabled=false (padrão em dev e nos
 * testes) a implementação só registra no log, e o app sobe sem broker.
 */
public interface DeviceMessagePublisher {

    void publish(String topic, String payload, int qos, boolean retained);
}
