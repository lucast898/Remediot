package br.mackenzie.remediot.servico;

import java.util.List;

public class AgendaInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> erros;

    public AgendaInvalidaException(List<String> erros) {
        super("Agenda inválida: " + erros);
        this.erros = List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}
