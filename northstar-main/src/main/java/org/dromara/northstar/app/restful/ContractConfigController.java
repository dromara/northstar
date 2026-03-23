package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.ContractConfigService;
import org.dromara.northstar.common.model.BatchResult;
import org.dromara.northstar.common.model.ContractConfigDTO;
import org.dromara.northstar.common.model.ContractConfigVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 合约配置控制器
 * 提供合约杠杆率、手续费、交易时间的个性化配置管理
 */
@Slf4j
@RequestMapping("northstar/contract-config")
@RestController
public class ContractConfigController {

    private final ContractConfigService contractConfigService;

    public ContractConfigController(ContractConfigService contractConfigService) {
        this.contractConfigService = contractConfigService;
    }

    /**
     * 查询所有配置
     */
    @GetMapping("all")
    public List<ContractConfigVO> findAll(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) Boolean onlyCustom) {
        return contractConfigService.findAll(exchange, onlyCustom);
    }

    /**
     * 保存配置
     */
    @PostMapping("save")
    public ContractConfigVO save(@RequestBody ContractConfigDTO dto) {
        return contractConfigService.save(dto);
    }

    /**
     * 批量保存
     */
    @PostMapping("batch")
    public BatchResult saveBatch(@RequestBody List<ContractConfigDTO> dtos) {
        return contractConfigService.saveBatch(dtos);
    }

    /**
     * 重置为默认配置
     */
    @PostMapping("{contractId}/reset")
    public void resetToDefault(@PathVariable String contractId) {
        contractConfigService.resetToDefault(contractId);
    }

}
