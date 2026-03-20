package org.dromara.northstar.app.restful;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.app.service.MultichartsService;
import org.dromara.northstar.common.model.OverlayVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;


@RequestMapping("northstar/multicharts")
@RestController
public class MultichartsController {

	@Autowired
	private MultichartsService multichartService;
	
	private Map<String, BarCycleEnum> barCycleEnumMap = new HashMap<>(){
		private static final long serialVersionUID = 1L;

		{
			put("1m", BarCycleEnum.B_1Min);
			put("3m", BarCycleEnum.B_3Min);
			put("5m", BarCycleEnum.B_5Min);
			put("10m", BarCycleEnum.B_10Min);
			put("15m", BarCycleEnum.B_15Min);
			put("30m", BarCycleEnum.B_30Min);
			put("1h", BarCycleEnum.B_1Hr);
			put("2h", BarCycleEnum.B_2Hr);
			put("4h", BarCycleEnum.B_4Hr);
			put("1d", BarCycleEnum.B_1Day);
			put("1wk", BarCycleEnum.B_1Wk);
		}
	};
	
	@PostMapping("overlay/{contractId}")
	public void saveOverlay(@PathVariable String contractId, @RequestBody OverlayVO overlay) {
		Assert.notNull(overlay, "OverlayVO不能为空");
		Assert.hasText(contractId, "ContractID不能为空");
		multichartService.saveOverlay(contractId, overlay);
	}
	
	@GetMapping("overlay/{contractId}/{periodText}")
	public List<OverlayVO> getOverlays(@PathVariable String contractId, @PathVariable String periodText) {
		Assert.hasText(contractId, "ContractID不能为空");
		Assert.hasText(periodText, "周期不能为空");
		return multichartService.getOverlays(contractId, barCycleEnumMap.get(periodText));
	}
	
	@DeleteMapping("overlay/{overlayId}")
	public void deleteOverlay(@PathVariable String overlayId) {
		Assert.hasText(overlayId, "画线ID不能为空");
		multichartService.deleteOverlay(overlayId);
	}
	
	@DeleteMapping("overlays/{contractId}")
	public void deleteOverlays(@PathVariable String contractId) {
		Assert.hasText(contractId, "ContractID不能为空");
		multichartService.deleteOverlays(contractId);
	}
	
}
