package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;

/**
 * Ponto único de escalonamento de alertas por peso do medicamento (ver
 * CLAUDE.md). Hoje só loga; a implementação real (Alexa/WhatsApp/push) entra
 * numa fase futura sem precisar mudar quem chama este serviço.
 */
public interface AlertService {

    void handleLateOrMissedDose(Dose dose);
}
