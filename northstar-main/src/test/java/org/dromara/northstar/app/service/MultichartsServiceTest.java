package org.dromara.northstar.app.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.dromara.northstar.common.model.OverlayVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

@ExtendWith(MockitoExtension.class)
class MultichartsServiceTest {

    @Mock
    MultichartsService service;
    
    OverlayVO overlay = OverlayVO.builder()
            .id("test-overlay-id")
            .name("Test Overlay")
            .build();
    
    @Test
    void testSaveOverlay() {
        service.saveOverlay("BTCUSDT@BINANCE@SPOT@PLAYBACK", overlay);
        Mockito.verify(service).saveOverlay("BTCUSDT@BINANCE@SPOT@PLAYBACK", overlay);
    }

    @Test
    void testDeleteOverlay() {
        service.deleteOverlay("test-overlay-id");
        Mockito.verify(service).deleteOverlay("test-overlay-id");
    }

    @Test
    void testGetOverlays() {
        Mockito.when(service.getOverlays("BTCUSDT@BINANCE@SPOT@PLAYBACK", BarCycleEnum.B_10Min))
               .thenReturn(List.of(overlay));
        List<OverlayVO> vos = service.getOverlays("BTCUSDT@BINANCE@SPOT@PLAYBACK", BarCycleEnum.B_10Min);
        assertEquals(1, vos.size());
        Mockito.verify(service).getOverlays("BTCUSDT@BINANCE@SPOT@PLAYBACK", BarCycleEnum.B_10Min);
    }

}