package br.edu.remediot.dto.device;

import java.time.LocalDate;

/**
 * "dados" dos eventos de dose (CONTRATO-MQTT.md, seção 4.3). Cada tipo usa só
 * parte dos campos; os demais vêm nulos.
 */
public record DoseEventData(
        String doseId,
        LocalDate dataReferencia,
        String horaPrevista,
        Long horarioPrevisto,
        Integer peso,
        Long atrasoSeg,
        Long antecedenciaSeg,
        String motivo,
        Boolean alarmeSilenciado,
        Boolean alarmeSonoro,
        Boolean sonoro,
        String commandId,
        String estadoDose,
        Integer aberturasNaJanela
) {
}
