package br.edu.remediot.controller;

import br.edu.remediot.service.VoiceAnnouncer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Teste de conexão com a Alexa: confirma que token e dispositivo do Voice Monkey
 * configurados no backend fazem o Echo falar.
 */
@RestController
@RequestMapping("/api/alexa")
public class AlexaController {

    private final VoiceAnnouncer voiceAnnouncer;

    public AlexaController(VoiceAnnouncer voiceAnnouncer) {
        this.voiceAnnouncer = voiceAnnouncer;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        if (!voiceAnnouncer.isConfigured()) {
            return ResponseEntity.status(503).body(Map.of(
                    "announced", false,
                    "message", "Alexa não configurada: defina VOICEMONKEY_TOKEN e VOICEMONKEY_DEVICE no backend."));
        }
        boolean announced = voiceAnnouncer.announce("Olá! Aqui é o RemedIoT. A Alexa está conectada.");
        return ResponseEntity.status(announced ? 200 : 502).body(Map.of(
                "announced", announced,
                "message", announced ? "Anúncio enviado." : "O Voice Monkey recusou o anúncio. Veja o log do backend."));
    }
}
