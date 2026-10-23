package br.edu.remediot.mqtt;

import br.edu.remediot.service.device.DeviceAckService;
import br.edu.remediot.service.device.DeviceEventIngestService;
import br.edu.remediot.service.device.DeviceStatusService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceMessageRouterTest {

    private final DeviceEventIngestService events = mock(DeviceEventIngestService.class);
    private final DeviceStatusService status = mock(DeviceStatusService.class);
    private final DeviceAckService acks = mock(DeviceAckService.class);
    private final DeviceMessageRouter router = new DeviceMessageRouter(events, status, acks);

    @Test
    void entregaCadaTopicoAoServicoCerto() {
        router.route("remediot/v1/A4CF12B3C4D5/eventos", "{e}");
        router.route("remediot/v1/A4CF12B3C4D5/status", "{s}");
        router.route("remediot/v1/A4CF12B3C4D5/ack", "{a}");

        verify(events).handle("A4CF12B3C4D5", "{e}");
        verify(status).handle("A4CF12B3C4D5", "{s}");
        verify(acks).handle("A4CF12B3C4D5", "{a}");
    }

    @Test
    void topicoInesperadoNaoChegaANenhumServico() {
        router.route("remediot/v1/A4CF12B3C4D5/outro", "{}");

        verifyNoInteractions(events, status, acks);
    }

    @Test
    void excecaoDoServicoNaoSaiDoRoteador() {
        doThrow(new IllegalStateException("banco fora do ar")).when(events).handle(any(), any());

        assertDoesNotThrow(() -> router.route("remediot/v1/A4CF12B3C4D5/eventos", "{}"));
    }
}
