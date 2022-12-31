import baseService from './baseRequest'

export default {
    getGatewayContracts(channelType){
        return baseService.get(`/contracts?gateway=${channelType}`)
    },
    getSubscribedContracts(gatewayId){
        return baseService.get(`/contracts/subscribed?gateway=${gatewayId}`)
    }
}