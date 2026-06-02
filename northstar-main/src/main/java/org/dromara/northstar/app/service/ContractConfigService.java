package org.dromara.northstar.app.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.BatchResult;
import org.dromara.northstar.common.model.ContractConfigDTO;
import org.dromara.northstar.common.model.ContractConfigVO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.jdbc.ContractConfigRepository;
import org.dromara.northstar.data.jdbc.entity.ContractConfigDO;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * 合约配置服务实现
 */
@Slf4j
@Service
public class ContractConfigService {

	@Autowired
    private ContractConfigRepository contractConfigRepository;

	@Autowired
	private MarketCenter mktCenter;

    public List<ContractConfigVO> findAll(String exchange, Boolean onlyCustom) {
        log.debug("查询合约配置，exchange: {}, onlyCustom: {}", exchange, onlyCustom);

        // 从数据库查询自定义配置
        List<ContractConfigDO> customEntities;
        if (StringUtils.isNotBlank(exchange)) {
            customEntities = contractConfigRepository.findByExchange(exchange);
        } else {
            customEntities = contractConfigRepository.findAll();
        }
        
        // 构建自定义配置 Map
        Map<String, ContractConfigDO> contractIdConfigMap = customEntities.stream()
                .collect(Collectors.toMap(ContractConfigDO::getContractId, item -> item, (a, b) -> a));
        
        if (Boolean.TRUE.equals(onlyCustom)) {
            // 只返回自定义配置
            return customEntities.stream()
                    .map(ContractConfigDO::convertTo)
                    .map(dto -> buildVO(mktCenter.getContract(Identifier.of(dto.getContractId())).contract(), dto))
                    .toList();
        }
        
        // 查询所有合约
        List<Contract> allContracts = mktCenter.channelContracts()
                .values().stream()
                .flatMap(List::stream)
                .filter(c -> StringUtils.isEmpty(exchange) || c.exchange().toString().equalsIgnoreCase(exchange))
                .toList();
        
        return allContracts.stream()
                .map(c -> {
                    ContractConfigDO configDo = contractIdConfigMap.get(c.contractId());
                    ContractConfigDTO dto = null;
                    if (configDo != null) {
                    		dto = configDo.convertTo();
                    }
                    return buildVO(c, dto);
                })
                .toList();
    }

    public ContractConfigVO save(ContractConfigDTO dto) {
        log.info("保存合约配置: {}", dto.getContractId());

        // 从 DTO 中提取合约信息
        String contractId = dto.getContractId();
        
        // 检查是否已存在相同合约ID的配置
        ContractConfigDO existingEntity = contractConfigRepository.findByContractId(contractId);

        // 查找合约信息
        Contract contract = mktCenter.getContract(Identifier.of(contractId)).contract();
        
        // 只有 PLAYBACK 网关的合约支持自定义配置
        if (contract.channelType() != ChannelType.PLAYBACK) {
            throw new IllegalStateException("只有PLAYBACK网关的合约支持自定义配置");
        }

        // 构建 DO
        ContractConfigDO entity = new ContractConfigDO();
        if (existingEntity != null) {
            entity.setId(existingEntity.getId());
        } else {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        
        // 设置合约基本信息
        entity.setContractId(contractId);
        entity.setExchange(contract.exchange().toString());
        
        // 设置配置值
        entity.setMarginRatio(dto.getMarginRatio());
        entity.setLeverage(dto.getLeverage());
        entity.setCommissionType(dto.getCommissionType());
        entity.setCommissionFee(dto.getCommissionFee());
        entity.setCommissionRate(dto.getCommissionRate());
        
        contractConfigRepository.save(entity);
        return buildVO(contract, dto);
    }

    public BatchResult saveBatch(List<ContractConfigDTO> dtos) {
        log.info("批量保存合约配置，数量: {}", dtos.size());

        BatchResult result = new BatchResult();

        for (ContractConfigDTO dto : dtos) {
            try {
                save(dto);
                result.incrementSuccess();
            } catch (Exception e) {
                log.error("保存合约配置失败: {}", dto.getContractId(), e);
                result.incrementFailed(dto.getContractId() + ": " + e.getMessage());
            }
        }

        return result;
    }

    @Transactional
    public void resetToDefault(String contractId) {
        log.info("重置合约配置为默认: {}", contractId);
        contractConfigRepository.deleteByContractId(contractId);
    }

    private ContractConfigVO buildVO(Contract c, ContractConfigDTO dto) {
    		String defaultCommissionType = c.contractDefinition().commissionFee() != null ? "FIXED" : "RATE";
        Double defaultMarginRatio = c.contractDefinition().marginRatio();
        if(defaultMarginRatio == null) {
        		throw new IllegalStateException(c.contractId() + "未设置保证金");
        }
        Integer defaultLeverage = (int)(1 / defaultMarginRatio);
        
        return ContractConfigVO.builder()
                .contractId(c.contractId())
                .unifiedSymbol(c.unifiedSymbol())
                .gatewayId(c.gatewayId())
                .channelType(c.channelType() != null ? c.channelType().toString() : null)
                .symbol(c.symbol())
                .exchange(c.exchange().toString())
                .productClass(c.productClass().toString())
                .name(c.name())
                .pricePrecision(c.pricePrecision())
                .quantityPrecision(c.quantityPrecision())
                // 系统默认值
                .defaultMarginRatio(defaultMarginRatio)
                .defaultLeverage(defaultLeverage)
                .defaultCommissionFee(c.contractDefinition().commissionFee())
                .defaultCommissionRate(c.contractDefinition().commissionRate())
                .defaultCommissionType(defaultCommissionType)
                .defaultTradeTimeSlots(c.contractDefinition().tradeTimeDef().timeSlots())
                .defaultWeekendClosure(!c.activeInWeekend())
                // 无自定义配置
                .customConfig(dto)
                .build();
    }

}
