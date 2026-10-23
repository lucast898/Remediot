package br.mackenzie.remediot.web;

import br.mackenzie.remediot.dto.Agenda;
import br.mackenzie.remediot.dto.DoseAgenda;
import br.mackenzie.remediot.dto.TipoComando;
import br.mackenzie.remediot.servico.AgendaInvalidaException;
import br.mackenzie.remediot.servico.AgendaService;
import br.mackenzie.remediot.servico.ComandoService;
import br.mackenzie.remediot.servico.EventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Endpoints de exemplo que o app chama (dá para testar com curl ou Postman). */
@RestController
@RequestMapping("/api/dispositivos/{deviceId}")
public class DispositivoController {

    private final AgendaService agendas;
    private final ComandoService comandos;
    private final EventoService eventos;

    public DispositivoController(AgendaService agendas, ComandoService comandos, EventoService eventos) {
        this.agendas = agendas;
        this.comandos = comandos;
        this.eventos = eventos;
    }

    /** Corpo: lista de doses. A versão e o configId são gerados aqui. */
    @PutMapping("/agenda")
    public Agenda atualizarAgenda(@PathVariable String deviceId, @RequestBody List<DoseAgenda> doses) {
        return agendas.publicar(deviceId, doses);
    }

    @PostMapping("/reabastecer")
    public Map<String, String> reabastecer(@PathVariable String deviceId,
                                           @RequestParam(required = false) Integer minutos) {
        return Map.of("commandId", comandos.iniciarReabastecimento(deviceId, minutos));
    }

    @PostMapping("/reabastecer/encerrar")
    public Map<String, String> encerrarReabastecimento(@PathVariable String deviceId) {
        return Map.of("commandId", comandos.encerrarReabastecimento(deviceId));
    }

    @PostMapping("/alertas/{eventId}/fui-eu")
    public Map<String, String> fuiEu(@PathVariable String deviceId, @PathVariable String eventId) {
        return Map.of("commandId", comandos.fuiEu(deviceId, eventId));
    }

    /** LOCALIZAR, SILENCIAR_ALARME, PEDIR_STATUS, REINICIAR, RESETAR_WIFI, RESSINCRONIZAR_AGENDA */
    @PostMapping("/comandos/{tipo}")
    public Map<String, String> comando(@PathVariable String deviceId, @PathVariable TipoComando tipo,
                                       @RequestBody(required = false) Map<String, Object> parametros) {
        return Map.of("commandId", comandos.enviar(deviceId, tipo, parametros));
    }

    @GetMapping("/eventos")
    public List<Map<String, Object>> eventos(@PathVariable String deviceId,
                                             @RequestParam(defaultValue = "50") int limite) {
        return eventos.ultimos(deviceId, Math.min(limite, 500));
    }

    @ExceptionHandler(AgendaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> agendaInvalida(AgendaInvalidaException e) {
        return ResponseEntity.badRequest().body(Map.of("erros", e.getErros()));
    }
}
