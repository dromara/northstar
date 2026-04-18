package org.dromara.northstar.app.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.OverlayVO;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.jdbc.OverlayRepository;
import org.dromara.northstar.data.jdbc.entity.OverlayDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.support.utils.bar.BarTimePadding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MultichartsService {
	
	@Autowired
	private OverlayRepository overlayRepository;
	
	@Autowired
	private MarketCenter mktCenter;

	public void saveOverlay(String contractId, OverlayVO overlay) {
		log.debug("保存画线：{} {}", overlay.getName(), overlay.getId());
		overlayRepository.save(OverlayDO.builder().id(overlay.getId()).contractId(contractId).overlayContent(JSON.toJSONString(overlay)).build());
	}
	
	public void deleteOverlay(String overlayId) {
		log.debug("删除画线：{}", overlayId);
		overlayRepository.deleteById(overlayId);
	}
	
	private LocalTime endTimeOfDate(Contract contract) {
		return contract.contractDefinition().tradeTimeDef().timeSlots().getLast().end();
	}
	
	public List<OverlayVO> getOverlays(String contractId, Interval interval) {
		List<OverlayDO> overlayDOList = overlayRepository.findByContractId(contractId);
		
		IContract icontract = mktCenter.getContract(Identifier.of(contractId));
		Contract contract = icontract.contract();
		LocalTime endTimeOfDate = endTimeOfDate(contract);
		boolean usingStartTimeInBarTime = contract.usingStartTimeInBarTime();
		BarTimePadding barTimePadding = new BarTimePadding(interval, contract);
		return overlayDOList.stream()
				.map(OverlayDO::getOverlayContent)
				.map(content -> JSON.parseObject(content, OverlayVO.class))
				.map(overley -> {
					contract.usingStartTimeInBarTime();
					overley.getPoints().stream().forEach(point -> {
						if(!usingStartTimeInBarTime) {
							try {								
								LocalDateTime cutoffDT = barTimePadding.cutoffTimeInMinute(point.getTimestamp());
								if(endTimeOfDate.equals(cutoffDT.toLocalTime())) {								
									if(interval == Interval.D1) {
										if(cutoffDT.getDayOfWeek().getValue() < 5 || contract.activeInWeekend()) {
											long cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT.plusDays(1));
											point.setTimestamp(cutoffTime);
										} else {
											long cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT.plusDays(8L - cutoffDT.getDayOfWeek().getValue()));
											point.setTimestamp(cutoffTime);
										}
										point.setTimestamp(CommonUtils.localDateTimeToMills(cutoffDT));
									} else if(interval == Interval.WK1) {
										long cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT.plusWeeks(1));
										point.setTimestamp(cutoffTime);
									}
								} else {
									long cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT);
									point.setTimestamp(cutoffTime);
								}
							} catch(Exception e) {
								log.warn("{}", e.getMessage());
							}
						}
					});
					return overley;
				})
				.toList();
	}
	
	@Transactional
	public void deleteOverlays(String contractId) {
		log.debug("移除[{}]的全部画线", contractId);
		overlayRepository.deleteByContractId(contractId);
	}
	
}
