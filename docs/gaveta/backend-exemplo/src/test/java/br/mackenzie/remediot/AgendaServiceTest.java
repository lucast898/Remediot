package br.mackenzie.remediot;

import br.mackenzie.remediot.dto.Agenda;
import br.mackenzie.remediot.dto.DoseAgenda;
import br.mackenzie.remediot.mqtt.MqttGateway;
import br.mackenzie.remediot.servico.AgendaInvalidaException;
import br.mackenzie.remediot.servico.AgendaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Versão da agenda e limite de tamanho, sem banco (JdbcTemplate falso). */
class AgendaServiceTest {

    private static final String DEVICE = "A4CF12B3C4D5";
    private static final List<DoseAgenda> UMA_DOSE = List.of(new DoseAgenda("s1", "08:00", null, 2, "Manhã"));

    private final List<String> publicados = new ArrayList<>();
    private Long versaoNoBanco;   // null = banco vazio (ex.: H2 depois de reiniciar o backend)
    private AgendaService servico;

    @BeforeEach
    void preparar() {
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                return 1;
            }

            @Override
            public <T> List<T> queryForList(String sql, Class<T> tipo, Object... args) {
                return versaoNoBanco == null ? List.of() : List.of(tipo.cast(versaoNoBanco));
            }
        };
        MqttGateway mqtt = (topico, qos, retained, payload) -> publicados.add(topico + " " + payload);
        servico = new AgendaService(jdbc, new ObjectMapper(), mqtt);
    }

    @Test
    void bancoVazioNaoRecomecaDoUm() {
        long antes = Instant.now().getEpochSecond();
        Agenda a = servico.publicar(DEVICE, UMA_DOSE);
        assertTrue(a.versao() >= antes, "versão vem do relógio (o ESP rejeitaria 1 depois de já ter recebido outras)");
        assertTrue(a.versao() < 4_294_967_296L, "cabe no uint32 do ESP");
        assertEquals(1, publicados.size());
    }

    @Test
    void versaoNuncaDiminui() {
        versaoNoBanco = Instant.now().getEpochSecond() + 1000;   // relógio do servidor atrasado
        Agenda a = servico.publicar(DEVICE, UMA_DOSE);
        assertEquals(versaoNoBanco + 1, a.versao());
    }

    @Test
    void agendaGrandeDemaisNemEhPublicada() {
        List<DoseAgenda> doses = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            doses.add(new DoseAgenda("schedule-" + i, "08:00", null, 2, "x".repeat(200)));
        }
        boolean recusou = false;
        try {
            servico.publicar(DEVICE, doses);
        } catch (AgendaInvalidaException e) {
            recusou = e.getErros().get(0).startsWith("AGENDA_GRANDE_DEMAIS");
        }
        assertTrue(recusou, "agenda acima de 7500 bytes -> AGENDA_GRANDE_DEMAIS");
        assertTrue(publicados.isEmpty());
    }
}
