package br.edu.remediot.service.device;

import br.edu.remediot.dto.device.DeviceEventType;
import tools.jackson.databind.JsonNode;

/**
 * Texto de cada evento da gaveta para alertas e histórico. A gaveta detecta a
 * abertura do compartimento, não a ingestão: nunca dizer que o remédio foi "tomado".
 */
public final class DeviceEventDescriptions {

    private DeviceEventDescriptions() {
    }

    public static String describe(String rawType, JsonNode data) {
        String time = text(data, "horaPrevista");
        String doseAt = time != null ? " das " + time : "";
        return switch (DeviceEventType.from(rawType)) {
            case DISPOSITIVO_INICIADO -> "Gaveta ligada";
            case ALARME_INICIADO -> "Chegou o horário da dose" + doseAt + " (luz piscando na gaveta)";
            case DOSE_ANTECIPADA -> "Compartimento aberto antes do horário: dose" + doseAt + " registrada";
            case DOSE_CONFIRMADA -> "Compartimento aberto no horário: dose" + doseAt + " registrada";
            case DOSE_EM_ATRASO -> "Dose" + doseAt + " em atraso: compartimento ainda não foi aberto";
            case DOSE_ATRASADA -> "Compartimento aberto com atraso: dose" + doseAt + " registrada";
            case DOSE_PERDIDA -> "DISPOSITIVO_INDISPONIVEL".equals(text(data, "motivo"))
                    ? "Dose" + doseAt + " sem registro: a gaveta estava desligada ou sem hora no horário"
                    + " (não dá para saber se o compartimento foi aberto)"
                    : "Dose" + doseAt + " não retirada: o compartimento não foi aberto em 3 horas";
            case ALARME_SILENCIADO -> "Alarme da dose" + doseAt + " silenciado pelo app";
            case ABERTURA_FORA_HORARIO -> "Gaveta aberta fora do horário de uma dose";
            case POSSIVEL_DOSE_DUPLA -> "Gaveta aberta de novo perto da dose" + doseAt
                    + ", que já estava registrada (possível dose dupla)";
            case ABERTURA_NAO_CLASSIFICADA -> "Gaveta aberta enquanto estava sem hora (abertura não classificada)";
            case GAVETA_ESQUECIDA_ABERTA -> "A gaveta ficou aberta por mais de 5 minutos";
            case GAVETA_FECHADA_APOS_ALERTA -> "A gaveta foi fechada depois do alerta";
            case REABASTECIMENTO -> "Gaveta aberta durante o modo reabastecer";
            case MODO_REABASTECER_INICIADO -> "Modo reabastecer ativado";
            case MODO_REABASTECER_ENCERRADO -> "Modo reabastecer encerrado";
            case FALHA_RELOGIO -> "A gaveta está sem hora confiável e não está acompanhando as doses";
            case RELOGIO_RESTABELECIDO -> "A gaveta voltou a ter hora e a acompanhar as doses";
            case UNKNOWN -> "Evento da gaveta: " + rawType;
        };
    }

    private static String text(JsonNode data, String field) {
        if (data == null || !data.hasNonNull(field)) {
            return null;
        }
        return data.get(field).asString();
    }
}
