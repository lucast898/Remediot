package br.mackenzie.remediot.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Conexão com o EMQX Cloud via Spring Integration MQTT (Eclipse Paho por baixo).
 *
 * Por que Spring Integration e não o Paho "puro": o adaptador entra no ciclo de
 * vida do Spring (conecta ao subir, desconecta ao parar), reconecta sozinho,
 * reassina os tópicos e entrega as mensagens num canal, tratado por um método
 * comum (@ServiceActivator). Com o Paho puro teríamos de escrever à mão os
 * callbacks, as threads, a reconexão e o encerramento.
 */
@Configuration
public class MqttConfig {

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties props) {
        MqttConnectOptions opcoes = new MqttConnectOptions();
        opcoes.setServerURIs(new String[] {props.url()});   // ssl://...:8883
        opcoes.setUserName(props.usuario());
        opcoes.setPassword(props.senha().toCharArray());
        opcoes.setAutomaticReconnect(true);
        opcoes.setKeepAliveInterval(30);
        // Sessão persistente: o broker guarda as mensagens QoS 1 enquanto o backend
        // estiver fora do ar. (Os eventos do ESP são QoS 0; para eles a garantia vem
        // do reenvio até a confirmação, ver EventoService.)
        opcoes.setCleanSession(false);

        DefaultMqttPahoClientFactory fabrica = new DefaultMqttPahoClientFactory();
        fabrica.setConnectionOptions(opcoes);
        return fabrica;
    }

    // ---------------- Entrada (ESP -> backend) ----------------

    @Bean
    public MessageChannel mqttEntrada() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttAdaptadorEntrada(MqttProperties props, MqttPahoClientFactory fabrica) {
        MqttPahoMessageDrivenChannelAdapter adaptador = new MqttPahoMessageDrivenChannelAdapter(
                props.clientId() + "-entrada", fabrica,
                Topicos.TODOS_EVENTOS, Topicos.TODOS_STATUS, Topicos.TODOS_ACKS);
        adaptador.setQos(1, 1, 1);
        adaptador.setConverter(new DefaultPahoMessageConverter());   // payload como String
        adaptador.setCompletionTimeout(10_000);
        adaptador.setOutputChannel(mqttEntrada());
        return adaptador;
    }

    // ---------------- Saída (backend -> ESP) ----------------

    @Bean
    public MessageChannel mqttSaida() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttSaida")
    public MessageHandler mqttAdaptadorSaida(MqttProperties props, MqttPahoClientFactory fabrica) {
        // clientId diferente do de entrada: o broker derruba conexões com o mesmo id
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(props.clientId() + "-saida", fabrica);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(false);
        return handler;
    }
}
