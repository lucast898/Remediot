package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.EventoDispositivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ponto único de saída para o cuidador. No exemplo só registra no log;
 * é aqui que entram o push do app e os lembretes pela Alexa.
 * (O ESP32 não fala com a Alexa: a integração é toda no backend.)
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    public void alertarCuidador(EventoDispositivo evento) {
        String texto = switch (evento.tipo()) {
            case DOSE_EM_ATRASO -> "Passou a tolerância e a gaveta não foi aberta (dose atrasada)";
            case DOSE_PERDIDA -> "Dose não retirada (3 h sem abrir a gaveta)";
            case ABERTURA_FORA_HORARIO -> "Gaveta aberta fora do horário das doses";
            case POSSIVEL_DOSE_DUPLA -> "A gaveta foi aberta de novo depois da dose: possível dose dupla";
            case GAVETA_ESQUECIDA_ABERTA -> "Gaveta esquecida aberta";
            case FALHA_RELOGIO -> "O aparelho está sem hora e não está acompanhando as doses";
            default -> evento.tipo().name();
        };
        log.warn("[ALERTA] {} — dispositivo {} (evento {})", texto, evento.deviceId(), evento.eventId());
        // TODO: push para o app do cuidador, com botão "fui eu" quando for ABERTURA_FORA_HORARIO.
        // Nos eventos de dose, o escalonamento por peso do app (Alexa, WhatsApp) entra aqui.
    }

    public void dispositivoOffline(String deviceId, String motivo) {
        log.warn("[ALERTA] Dispositivo {} ficou offline ({})", deviceId, motivo);
        // TODO: avisar o cuidador se continuar offline por mais de X minutos
    }
}
