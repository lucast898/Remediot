package br.mackenzie.remediot;

import br.mackenzie.remediot.dto.DiaSemana;
import br.mackenzie.remediot.dto.DoseAgenda;
import br.mackenzie.remediot.servico.AgendaValidador;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** As mesmas regras que o firmware aplica (e que o simulador do firmware testa). */
class AgendaValidadorTest {

    private static DoseAgenda dose(String id, String hora, Integer peso) {
        return new DoseAgenda(id, hora, null, peso, null);
    }

    private static boolean temErro(List<DoseAgenda> doses, String codigo) {
        return AgendaValidador.validar(doses).stream().anyMatch(e -> e.startsWith(codigo));
    }

    @Test
    void agendaNoFormatoDoAppEhValida() {
        assertTrue(AgendaValidador.validar(List.of(
                dose("s1", "08:00", 1),
                dose("s2", "08:00", 3),
                new DoseAgenda("s3", "20:00", List.of(DiaSemana.SEG, DiaSemana.TER), 2, null))).isEmpty());
    }

    @Test
    void agendaVaziaEhValida() {
        assertTrue(AgendaValidador.validar(List.of()).isEmpty());
    }

    @Test
    void horariosLivresComoNoApp() {
        // vários no mesmo dia e muito próximos: aceitos
        assertTrue(AgendaValidador.validar(List.of(
                dose("a", "06:00", 2), dose("b", "08:00", 2), dose("c", "08:10", 2),
                dose("d", "10:00", 2), dose("e", "14:00", 2), dose("f", "22:00", 2))).isEmpty());
    }

    @Test
    void limiteDeMemoriaDoFirmware() {
        List<DoseAgenda> muitas = new ArrayList<>();
        for (int i = 0; i <= AgendaValidador.MAX_ENTRADAS; i++) {
            muitas.add(dose("x" + i, "08:00", 2));
        }
        assertTrue(temErro(muitas, "DOSES_DEMAIS"));
    }

    @Test
    void camposInvalidos() {
        assertTrue(temErro(List.of(dose("a", "24:00", 2)), "HORA_INVALIDA"));
        assertTrue(temErro(List.of(dose("a b", "08:00", 2)), "DOSE_ID_INVALIDO"));
        assertTrue(temErro(List.of(dose("a", "08:00", 2), dose("a", "20:00", 2)), "DOSE_ID_DUPLICADO"));
        assertTrue(temErro(List.of(new DoseAgenda("a", "08:00", List.of(), 2, null)), "DIAS_INVALIDOS"));
        assertTrue(temErro(List.of(dose("a", "08:00", 4)), "PESO_INVALIDO"));
    }
}
