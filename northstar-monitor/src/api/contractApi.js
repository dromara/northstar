import baseService from './baseRequest'

export default {
    getGatewayContracts(channelType, query){
        return baseService.get(`/contracts?channelType=${channelType}&query=${query||''}`)
    },
    getSubscribedContracts(gatewayId, query){
        return baseService.get(`/contracts/subscribed?gatewayId=${gatewayId}&query=${query||''}`)
    }
}