package br.edu.remediot.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lembretes antes da dose (padrão 10 min, 5 min, 30 s): dispara só o mais
 * próximo do horário que ainda não foi enviado.
 */
class ReminderPolicyTest {

    private static final Duration TEN_MIN = Duration.ofMinutes(10);
    private static final Duration FIVE_MIN = Duration.ofMinutes(5);
    private static final Duration THIRTY_SEC = Duration.ofSeconds(30);

    private final ReminderPolicy policy = new ReminderPolicy(List.of(TEN_MIN, FIVE_MIN, THIRTY_SEC));
    private final LocalDateTime scheduledAt = LocalDateTime.of(2026, 9, 29, 21, 0);

    private Optional<Duration> due(LocalDateTime now, Duration... sent) {
        return policy.dueOffset(scheduledAt, now, Set.of(sent));
    }

    @Test
    void antesDaJanelaNaoDisparaNada() {
        assertTrue(due(scheduledAt.minusMinutes(11)).isEmpty());
    }

    @Test
    void faltandoDezMinutosDisparaOLembreteDeDez() {
        assertEquals(Optional.of(TEN_MIN), due(scheduledAt.minusMinutes(10)));
        assertEquals(Optional.of(TEN_MIN), due(scheduledAt.minusMinutes(7)));
    }

    @Test
    void lembreteJaEnviadoNaoRepete() {
        assertTrue(due(scheduledAt.minusMinutes(7), TEN_MIN).isEmpty());
    }

    @Test
    void faltandoCincoMinutosDisparaOLembreteDeCinco() {
        assertEquals(Optional.of(FIVE_MIN), due(scheduledAt.minusMinutes(5), TEN_MIN));
    }

    @Test
    void faltandoTrintaSegundosDisparaOUltimoLembrete() {
        assertEquals(Optional.of(THIRTY_SEC), due(scheduledAt.minusSeconds(30), TEN_MIN, FIVE_MIN));
        assertTrue(due(scheduledAt.minusSeconds(10), TEN_MIN, FIVE_MIN, THIRTY_SEC).isEmpty());
    }

    @Test
    void doseCriadaEmCimaDaHoraRecebeSoOAvisoMaisProximo() {
        // Backend fora do ar até faltarem 3 min: não deve disparar 10 e 5 atrasados
        assertEquals(Optional.of(FIVE_MIN), due(scheduledAt.minusMinutes(3)));
    }

    @Test
    void noHorarioOuDepoisNaoDisparaLembrete() {
        assertTrue(due(scheduledAt).isEmpty());
        assertTrue(due(scheduledAt.plusMinutes(1)).isEmpty());
    }

    @Test
    void maiorAntecedenciaDefineAJanelaDeBusca() {
        assertEquals(TEN_MIN, policy.maxOffset());
    }

    @Test
    void rejeitaListaVaziaOuValoresNaoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new ReminderPolicy(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReminderPolicy(List.of(Duration.ZERO)));
    }

    @Test
    void textoFaladoEmPortugues() {
        assertEquals("Atenção, Maria. Faltam 5 minutos para o remédio Losartana, 50mg.",
                ReminderPolicy.speechFor("Maria", "Losartana", "50mg", FIVE_MIN));
        assertEquals("30 segundos", ReminderPolicy.describe(THIRTY_SEC));
        assertEquals("1 minuto", ReminderPolicy.describe(Duration.ofMinutes(1)));
    }
}
