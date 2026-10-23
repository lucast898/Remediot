package br.mackenzie.remediot.servico;

import br.mackenzie.remediot.dto.DoseAgenda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mesmas regras que o firmware aplica (agenda.cpp). Validar aqui dá resposta
 * imediata ao app; o ESP valida de novo e responde REJEITADA se algo passar.
 *
 * Por enquanto os horários são livres, como no app: sem limite por dia e sem
 * intervalo mínimo. Cada entrada é um remédio num horário.
 */
public final class AgendaValidador {

    /** Limite de memória do firmware (MAX_ENTRADAS_AGENDA no config.h). */
    public static final int MAX_ENTRADAS = 40;

    /**
     * Tamanho máximo do JSON da agenda. O buffer MQTT do ESP tem 8192 bytes
     * (MQTT_BUFFER_BYTES), que incluem o cabeçalho e o tópico; acima disso
     * a mensagem é descartada sem ack. 40 entradas normais dão ~4 KB.
     */
    public static final int MAX_BYTES = 7500;

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Pattern HORA = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private AgendaValidador() {
    }

    public static List<String> validar(List<DoseAgenda> doses) {
        List<String> erros = new ArrayList<>();
        if (doses == null) {
            erros.add("DOSES_AUSENTE");
            return erros;
        }
        if (doses.size() > MAX_ENTRADAS) {
            erros.add("DOSES_DEMAIS");
        }

        Set<String> ids = new HashSet<>();
        for (DoseAgenda d : doses) {
            if (d.doseId() == null || !ID.matcher(d.doseId()).matches()) {
                erros.add("DOSE_ID_INVALIDO: " + d.doseId());
                continue;
            }
            if (!ids.add(d.doseId())) {
                erros.add("DOSE_ID_DUPLICADO: " + d.doseId());
            }
            if (d.hora() == null || !HORA.matcher(d.hora()).matches()) {
                erros.add("HORA_INVALIDA: " + d.doseId());
            }
            // diasSemana é opcional (sem ele = todos os dias), mas não pode vir vazio
            if (d.diasSemana() != null
                    && (d.diasSemana().isEmpty() || d.diasSemana().stream().anyMatch(Objects::isNull))) {
                erros.add("DIAS_INVALIDOS: " + d.doseId());
            }
            if (d.peso() != null && (d.peso() < 1 || d.peso() > 3)) {
                erros.add("PESO_INVALIDO: " + d.doseId());
            }
        }
        return erros;
    }
}
