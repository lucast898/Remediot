package br.mackenzie.remediot;

import br.mackenzie.remediot.dto.AckComando;
import br.mackenzie.remediot.dto.DadosAberturaForaHorario;
import br.mackenzie.remediot.dto.DadosDose;
import br.mackenzie.remediot.dto.EventoDispositivo;
import br.mackenzie.remediot.dto.StatusAck;
import br.mackenzie.remediot.dto.StatusDispositivo;
import br.mackenzie.remediot.dto.TipoEvento;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os arquivos em src/test/resources/firmware/ foram produzidos pelo firmware real
 * (rodando no simulador). O mapper ESTRITO falha se o firmware mandar um campo
 * que não existe no record: é o teste de que o contrato está igual dos dois lados.
 */
class ContratoJsonTest {

    private final ObjectMapper estrito = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private JsonNode ler(String arquivo) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/firmware/" + arquivo)) {
            return estrito.readTree(in);
        }
    }

    @Test
    void todosOsTiposDeEventoDoFirmwareBatemComOsRecords() throws Exception {
        Set<TipoEvento> vistos = EnumSet.noneOf(TipoEvento.class);
        for (JsonNode no : ler("eventos.json")) {
            EventoDispositivo ev = estrito.treeToValue(no, EventoDispositivo.class);
            assertNotNull(ev.eventId());
            assertTrue(ev.eventId().startsWith(ev.deviceId() + "-"));
            assertNotEquals(TipoEvento.DESCONHECIDO, ev.tipo());
            vistos.add(ev.tipo());

            switch (ev.tipo()) {
                case ALARME_INICIADO, DOSE_ANTECIPADA, DOSE_CONFIRMADA, DOSE_PERDIDA,
                     DOSE_ATRASADA, ALARME_SILENCIADO, POSSIVEL_DOSE_DUPLA -> {
                    DadosDose d = estrito.treeToValue(ev.dados(), DadosDose.class);
                    assertNotNull(d.doseId());
                    assertNotNull(d.horarioPrevisto());
                }
                case ABERTURA_FORA_HORARIO ->
                        estrito.treeToValue(ev.dados(), DadosAberturaForaHorario.class);
                default -> { }
            }
        }
        // todos os tipos do enum (menos DESCONHECIDO) aparecem nas amostras
        assertEquals(TipoEvento.values().length - 1, vistos.size());
    }

    @Test
    void eventoSemHoraUsaOHorarioDeChegada() throws Exception {
        for (JsonNode no : ler("eventos.json")) {
            EventoDispositivo ev = estrito.treeToValue(no, EventoDispositivo.class);
            if (ev.ts() == null) {
                assertEquals("NENHUMA", ev.fonteHora());
                var chegada = java.time.Instant.parse("2026-09-24T13:00:00Z");
                assertEquals(chegada, ev.ocorridoEm(chegada));
                return;
            }
        }
        throw new AssertionError("amostra sem evento com ts null");
    }

    @Test
    void statusELwtBatemComORecord() throws Exception {
        boolean viuOffline = false;
        for (JsonNode no : ler("status.json")) {
            StatusDispositivo s = estrito.treeToValue(no, StatusDispositivo.class);
            assertNotNull(s.deviceId());
            if (!s.online()) {
                viuOffline = true;
                assertNotNull(s.motivo());
            }
        }
        assertTrue(viuOffline, "a amostra deve ter um LWT/despedida");
    }

    @Test
    void acksBatemComORecord() throws Exception {
        Set<StatusAck> vistos = EnumSet.noneOf(StatusAck.class);
        for (JsonNode no : ler("acks.json")) {
            AckComando ack = estrito.treeToValue(no, AckComando.class);
            assertNotEquals(StatusAck.DESCONHECIDO, ack.status());
            vistos.add(ack.status());
            if (ack.status() == StatusAck.REJEITADA) {
                assertTrue(ack.erros() != null && !ack.erros().isEmpty());
            }
        }
        assertTrue(vistos.containsAll(EnumSet.of(StatusAck.OK, StatusAck.ERRO, StatusAck.APLICADA,
                StatusAck.REJEITADA, StatusAck.DUPLICADO, StatusAck.EXPIRADO, StatusAck.IGNORADO)));
    }

    @Test
    void tipoNovoDoFirmwareViraDesconhecidoSemQuebrar() throws Exception {
        ObjectMapper comoNoSpring = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
        EventoDispositivo ev = comoNoSpring.readValue(
                "{\"eventId\":\"A-1\",\"deviceId\":\"A\",\"tipo\":\"TIPO_QUE_AINDA_NAO_EXISTE\",\"ts\":null,\"campoNovo\":1}",
                EventoDispositivo.class);
        assertEquals(TipoEvento.DESCONHECIDO, ev.tipo());
        assertNull(ev.ts());
    }
}
