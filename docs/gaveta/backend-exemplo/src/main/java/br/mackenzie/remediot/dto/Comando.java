package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Comando para o ESP. NUNCA publicar com retained (um REINICIAR retido
 * reiniciaria o aparelho a cada reconexão).
 *
 * @param emitidoEm epoch UTC em segundos
 * @param validoAte epoch UTC; depois disso o ESP responde EXPIRADO e não executa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Comando(String commandId, TipoComando tipo, long emitidoEm, Long validoAte,
                      Map<String, Object> parametros) {
}
