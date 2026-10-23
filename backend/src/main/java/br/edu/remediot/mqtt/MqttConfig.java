package br.edu.remediot.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Conexão real com o EMQX Cloud (TLS, porta 8883). Só existe com
 * remediot.mqtt.enabled=true. O certificado do EMQX (DigiCert Global Root CA) já
 * está no truststore padrão do Java: não é preciso configurar certificado.
 */
@Configuration
@ConditionalOnProperty(name = "remediot.mqtt.enabled", havingValue = "true")
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    static final String INBOUND_CHANNEL = "mqttInbound";
    static final String OUTBOUND_CHANNEL = "mqttOutbound";

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties props) {
        if (isBlank(props.host()) || isBlank(props.username()) || isBlank(props.password())) {
            throw new IllegalStateException(
                    "remediot.mqtt.enabled=true exige MQTT_HOST, MQTT_USUARIO e MQTT_SENHA nas variáveis de ambiente");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {props.url()});
        options.setUserName(props.username());
        options.setPassword(props.password().toCharArray());
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(30);
        // Sessão persistente: o broker guarda as mensagens QoS 1 enquanto o backend
        // estiver fora. Os eventos da gaveta vêm em QoS 0; a garantia deles é o
        // reenvio até a confirmação (CONTRATO-MQTT.md, seção 3).
        options.setCleanSession(false);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        log.info("MQTT ligado: {} como {} (clientId {})", props.url(), props.username(), props.clientId());
        return factory;
    }

    // ---------------- Entrada (gaveta -> backend) ----------------

    @Bean(name = INBOUND_CHANNEL)
    public MessageChannel mqttInbound() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInboundAdapter(MqttProperties props, MqttPahoClientFactory factory) {
        // clientIds diferentes para entrada e saída: o broker derruba conexões com o mesmo id
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                props.clientId() + "-in", factory,
                MqttTopics.ALL_EVENTS, MqttTopics.ALL_STATUS, MqttTopics.ALL_ACKS);
        adapter.setQos(1, 1, 1);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setCompletionTimeout(10_000);
        adapter.setOutputChannel(mqttInbound());
        return adapter;
    }

    @ServiceActivator(inputChannel = INBOUND_CHANNEL)
    @Bean
    public MessageHandler mqttInboundHandler(DeviceMessageRouter router) {
        return (Message<?> message) -> router.route(
                (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC),
                String.valueOf(message.getPayload()));
    }

    // ---------------- Saída (backend -> gaveta) ----------------

    @Bean(name = OUTBOUND_CHANNEL)
    public MessageChannel mqttOutbound() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = OUTBOUND_CHANNEL)
    public MessageHandler mqttOutboundHandler(MqttProperties props, MqttPahoClientFactory factory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(props.clientId() + "-out", factory);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(false);
        return handler;
    }

    @Bean
    public DeviceMessagePublisher mqttDeviceMessagePublisher() {
        return new MqttDeviceMessagePublisher(mqttOutbound());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
