package br.edu.remediot.service;

import br.edu.remediot.entity.Dose;
import br.edu.remediot.entity.Patient;

/**
 * Ponto único de escalonamento de alertas por peso do medicamento (ver
 * CLAUDE.md). Hoje só loga; a implementação real (WhatsApp/push) entra numa
 * fase futura sem precisar mudar quem chama este serviço.
 */
public interface AlertService {

    void handleLateOrMissedDose(Dose dose);

    /**
     * Alerta da gaveta que não é de uma dose (fora de horário, dose dupla, gaveta
     * esquecida aberta, sem hora, offline). Vai para o cuidador (D9). alertType é
     * o tipo do evento da gaveta, ou GAVETA_OFFLINE.
     */
    void handleDeviceAlert(Patient patient, String alertType, String message);
}
