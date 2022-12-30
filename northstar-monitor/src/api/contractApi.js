import baseService from './baseRequest'

export default {
    getGatewayContracts(gatewayId){
        return baseService.get(`/contracts?gateway=${gatewayId}`)
    },
    getSubscribedContracts(gatewayId){
        return baseService.get(`/contracts/subscribed?gateway=${gatewayId}`)
    }
}