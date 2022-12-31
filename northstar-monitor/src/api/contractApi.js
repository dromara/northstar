import baseService from './baseRequest'

export default {
    getGatewayContracts(channelType, query){
        return baseService.get(`/contracts?channelType=${channelType}&query=${query||''}`)
    },
    getSubscribedContracts(gatewayId){
        return baseService.get(`/contracts/subscribed?gateway=${gatewayId}`)
    }
}