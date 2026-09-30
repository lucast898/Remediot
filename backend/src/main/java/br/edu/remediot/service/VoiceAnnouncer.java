package br.edu.remediot.service;

/**
 * Anúncio por voz no dispositivo do paciente. Hoje: Alexa via Voice Monkey.
 * Isolado atrás de interface para trocar de provedor sem mexer no agendador.
 */
public interface VoiceAnnouncer {

    boolean isConfigured();

    /** @return true se o provedor confirmou o anúncio */
    boolean announce(String speech);
}
