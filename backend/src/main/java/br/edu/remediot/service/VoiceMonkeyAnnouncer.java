package br.edu.remediot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Faz a Alexa falar via API v3 do Voice Monkey (https://voicemonkey.io/docs/api/announcement).
 * Sem token/dispositivo configurados, só registra no log — o resto do sistema segue igual.
 */
@Service
public class VoiceMonkeyAnnouncer implements VoiceAnnouncer {

    private static final Logger log = LoggerFactory.getLogger(VoiceMonkeyAnnouncer.class);

    private final RestClient restClient;
    private final String url;
    private final String token;
    private final String deviceId;
    private final String language;
    private final String voice;

    public VoiceMonkeyAnnouncer(
            @Value("${remediot.alexa.voicemonkey.url}") String url,
            @Value("${remediot.alexa.voicemonkey.token:}") String token,
            @Value("${remediot.alexa.voicemonkey.device:}") String deviceId,
            @Value("${remediot.alexa.language:pt-BR}") String language,
            @Value("${remediot.alexa.voice:}") String voice
    ) {
        this.restClient = RestClient.create();
        this.url = url;
        this.token = token;
        this.deviceId = deviceId;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public boolean isConfigured() {
        return !token.isBlank() && !deviceId.isBlank();
    }

    @Override
    public boolean announce(String speech) {
        if (!isConfigured()) {
            log.info("[ALEXA desligada — defina VOICEMONKEY_TOKEN e VOICEMONKEY_DEVICE] \"{}\"", speech);
            return false;
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("device", deviceId);
        body.put("speech", speech);
        body.put("language", language);
        if (!voice.isBlank()) {
            body.put("voice", voice);
        }

        try {
            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[ALEXA] anunciado: \"{}\"", speech);
            return true;
        } catch (RestClientException e) {
            // Nunca derrubar o agendador por falha externa (rede, cota do plano, 429)
            log.warn("[ALEXA] falha ao anunciar \"{}\": {}", speech, e.getMessage());
            return false;
        }
    }
}
