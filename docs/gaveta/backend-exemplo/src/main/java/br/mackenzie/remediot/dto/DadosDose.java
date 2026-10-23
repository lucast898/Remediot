package br.mackenzie.remediot.dto;

/**
 * "dados" dos eventos de dose: ALARME_INICIADO, DOSE_ANTECIPADA, DOSE_CONFIRMADA,
 * DOSE_EM_ATRASO, DOSE_ATRASADA, DOSE_PERDIDA, ALARME_SILENCIADO e POSSIVEL_DOSE_DUPLA.
 * Campos que não se aplicam ao tipo vêm null.
 */
public record DadosDose(
        String doseId,
        String dataReferencia,     // "2026-09-24" (data local da dose)
        String horaPrevista,       // "08:00"
        Long horarioPrevisto,      // epoch UTC
        Integer peso,              // 1, 2 ou 3
        Integer atrasoSeg,         // CONFIRMADA e ATRASADA
        Integer antecedenciaSeg,   // ANTECIPADA
        String motivo,             // EM_ATRASO e PERDIDA: SEM_ABERTURA ou DISPOSITIVO_INDISPONIVEL
        Boolean sonoro,            // ALARME_INICIADO: a gaveta apita no horário?
        Boolean alarmeSonoro,      // EM_ATRASO: a gaveta vai apitar pelo atraso? (peso 2 e 3)
        Boolean alarmeSilenciado,  // CONFIRMADA, ATRASADA e PERDIDA
        String estadoDose,         // POSSIVEL_DOSE_DUPLA
        Integer aberturasNaJanela, // POSSIVEL_DOSE_DUPLA
        String commandId) {        // ALARME_SILENCIADO

    /**
     * Status equivalente no app (DoseStatus do backend do Lucas), ou null quando
     * o evento não muda o status.
     */
    public static String statusNoApp(TipoEvento tipo) {
        return switch (tipo) {
            case DOSE_ANTECIPADA, DOSE_CONFIRMADA -> "TOMADA";
            case DOSE_ATRASADA -> "ATRASADA";
            case DOSE_PERDIDA -> "NAO_RETIRADA";
            default -> null;
        };
    }
}
