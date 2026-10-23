package br.mackenzie.remediot.dto;

/** "dados" de ABERTURA_FORA_HORARIO. proximaDose é null se não há dose nos próximos 8 dias. */
public record DadosAberturaForaHorario(ProximaDose proximaDose) {

    public record ProximaDose(String doseId, String dataReferencia, String horaPrevista, Long emSeg) {
    }
}
