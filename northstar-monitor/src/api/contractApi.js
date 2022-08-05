import baseService from './baseRequest'

export default {
    getContractDefs(providerName){
        return baseService.get(`/contract/defs?provider=${providerName}`)
    },
    getContractList(providerName){
        return baseService.get(`/contract/list?provider=${providerName}`)
    },
    getContractProviders(gatewayType){
        return baseService.get(`/contract/providers?gatewayType=${gatewayType}`)
    },
    getSubscribableContractList(contractDefId){
        return baseService.get(`/contract/subable?contractDefId=${contractDefId}`)
    }
}