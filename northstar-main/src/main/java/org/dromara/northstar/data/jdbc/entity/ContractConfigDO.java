package org.dromara.northstar.data.jdbc.entity;

import org.dromara.northstar.common.model.ContractConfigDTO;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合约配置数据实体
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "CONTRACT_CONFIG", indexes = {
    @Index(name = "idx_exchange", columnList = "exchange")
})
public class ContractConfigDO {

    @Id
    private String id;
    
    /**
     * 合约ID
     */
    private String contractId;
    
    /**
     * 交易所
     */
    private String exchange;
    
    // ========== 杠杆配置 ==========
    
    /**
     * 保证金率
     */
    private Double marginRatio;
    
    /**
     * 杠杆倍数
     */
    private Integer leverage;
    
    // ========== 手续费配置 ==========
    
    /**
     * 手续费类型：FIXED-固定, RATE-比例
     */
    private String commissionType;
    
    /**
     * 固定手续费（元/手）
     */
    private Double commissionFee;
    
    /**
     * 手续费率
     */
    private Double commissionRate;
    
    
    /**
     * 从 DTO 转换为 DO
     */
    public static ContractConfigDO convertFrom(ContractConfigDTO dto) {
        ContractConfigDO entity = new ContractConfigDO();
        entity.setContractId(dto.getContractId());
        entity.setExchange(dto.getExchange());
        entity.setMarginRatio(dto.getMarginRatio());
        entity.setLeverage(dto.getLeverage());
        entity.setCommissionType(dto.getCommissionType());
        entity.setCommissionFee(dto.getCommissionFee());
        entity.setCommissionRate(dto.getCommissionRate());
        
        return entity;
    }
    
    /**
     * 转换为 DTO
     */
    public ContractConfigDTO convertTo() {
        ContractConfigDTO dto = new ContractConfigDTO();
        dto.setContractId(contractId);
        dto.setExchange(exchange);
        dto.setMarginRatio(marginRatio);
        dto.setLeverage(leverage);
        dto.setCommissionType(commissionType);
        dto.setCommissionFee(commissionFee);
        dto.setCommissionRate(commissionRate);
        
        return dto;
    }
}
