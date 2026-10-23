package br.edu.remediot.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexão com o broker da gaveta (EMQX Cloud). Credenciais só por variável de
 * ambiente: use o usuário do BACKEND, nunca o dos dispositivos.
 */
@ConfigurationProperties(prefix = "remediot.mqtt")
public record MqttProperties(
        boolean enabled,
        String host,
        int port,
        String username,
        String password,
        String clientId
) {

    public String url() {
        return "ssl://" + host + ":" + port;
    }
}
