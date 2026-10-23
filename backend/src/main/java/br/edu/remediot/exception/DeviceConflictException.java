package br.edu.remediot.exception;

/** Operação da gaveta que não pode ser feita no estado atual (ex.: pareamento ambíguo). */
public class DeviceConflictException extends RuntimeException {

    public DeviceConflictException(String message) {
        super(message);
    }
}
