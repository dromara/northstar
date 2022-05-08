import baseService from './baseRequest'
import {
    ContractField
  } from '@/lib/xyz/redtorch/pb/core_field_pb'
const contractsCache = {}

export default {
    async getContracts(gatewayType){
        if(contractsCache[gatewayType] && contractsCache[gatewayType].length){
            return contractsCache[gatewayType]
        }
        const resultList = await baseService.get(`/data/contracts?type=${gatewayType}`)
        const contracts = resultList.map(data => ContractField.deserializeBinary(data).toObject())
        contractsCache[gatewayType] = contracts
        return contracts
    }
}
