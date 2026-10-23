package br.mackenzie.remediot.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloco remediot.mqtt do application.yml. */
@ConfigurationProperties(prefix = "remediot.mqtt")
public record MqttProperties(String host, int porta, String usuario, String senha, String clientId) {

    public String url() {
        // "ssl://" = TLS. O Paho valida o certificado com o truststore padrão da JVM,
        // que já traz as raízes da DigiCert usadas pelo EMQX Cloud.
        return "ssl://" + host + ":" + porta;
    }
}
