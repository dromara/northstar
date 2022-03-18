/* eslint-disable */
/**
 * @fileoverview
 * @enhanceable
 * @suppress {messageConventions} JS Compiler reports an error if a variable or
 *     field starts with 'MSG_' and isn't a translatable message.
 * @public
 */
// GENERATED CODE -- DO NOT EDIT!

var jspb = require('google-protobuf')
var goog = jspb
var global = Function('return this')()

goog.exportSymbol('proto.xyz.redtorch.pb.ActionFlagEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.BarCycleEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.CombinationTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.CommonStatusEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.ConnectStatusEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.ContingentConditionEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.CurrencyEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.DirectionEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.ExchangeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.ForceCloseReasonEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.GatewayAdapterTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.GatewayTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.HedgeFlagEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.LogLevelEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.MarketDataDBTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OffsetFlagEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OptionsTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderActionStatusTyp', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderPriceTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderSourceEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderStatusEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderSubmitStatusEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.OrderTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.PositionDirectionEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.PositionTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.PriceSourceEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.ProductClassEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.StrategyEngineTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.StrikeModeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.TimeConditionEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.TradeTypeEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.TradingRightEnum', null, global)
goog.exportSymbol('proto.xyz.redtorch.pb.VolumeConditionEnum', null, global)
/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.StrategyEngineTypeEnum = {
  SET_TREADING: 0,
  SET_BACKTESTING: 1
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.GatewayTypeEnum = {
  GTE_TRADEANDMARKETDATA: 0,
  GTE_MARKETDATA: 1,
  GTE_TRADE: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.GatewayAdapterTypeEnum = {
  GAT_CTP: 0,
  GAT_IB: 1
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.CommonStatusEnum = {
  COMS_SUCCESS: 0,
  COMS_INFO: 1,
  COMS_WARN: 2,
  COMS_ERROR: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.LogLevelEnum = {
  LL_ALL: 0,
  LL_ERROR: 40000,
  LL_WARN: 30000,
  LL_INFO: 20000,
  LL_DEBUG: 10000,
  LL_TRACE: 5000
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.MarketDataDBTypeEnum = {
  MDDT_MIX: 0,
  MDDT_TD: 1,
  MDDT_HIST: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.BarCycleEnum = {
  B_5SEC: 0,
  B_1MIN: 1,
  B_3MIN: 2,
  B_5MIN: 3,
  B_15MIN: 4,
  B_1DAY: 5
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ConnectStatusEnum = {
  CS_UNKONWN: 0,
  CS_DISCONNECTED: 1,
  CS_CONNECTED: 2,
  CS_DISCONNECTING: 3,
  CS_CONNECTING: 4
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderActionStatusTyp = {
  OAS_UNKONWN: 0,
  OAS_SUBMITTED: 1,
  OAS_ACCEPTED: 2,
  OAS_REJECTED: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderStatusEnum = {
  OS_UNKNOWN: 0,
  OS_ALLTRADED: 1,
  OS_PARTTRADEDQUEUEING: 2,
  OS_PARTTRADEDNOTQUEUEING: 3,
  OS_NOTRADEQUEUEING: 4,
  OS_NOTRADENOTQUEUEING: 5,
  OS_CANCELED: 6,
  OS_NOTTOUCHED: 7,
  OS_TOUCHED: 8,
  OS_REJECTED: 9
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderSubmitStatusEnum = {
  OSS_UNKNOWN: 0,
  OSS_INSERTSUBMITTED: 1,
  OSS_CANCELSUBMITTED: 2,
  OSS_MODIFYSUBMITTED: 3,
  OSS_ACCEPTED: 4,
  OSS_INSERTREJECTED: 5,
  OSS_CANCELREJECTED: 6,
  OSS_MODIFYREJECTED: 7
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ProductClassEnum = {
  UNKNOWNPRODUCTCLASS: 0,
  EQUITY: 1,
  FUTURES: 2,
  OPTION: 3,
  INDEX: 4,
  COMBINATION: 5,
  BOND: 6,
  FOREX: 7,
  SPOT: 8,
  DEFER: 9,
  ETF: 10,
  WARRANTS: 11,
  SPREAD: 12,
  FUND: 13,
  EFP: 14,
  SPOTOPTION: 15
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.DirectionEnum = {
  D_UNKNOWN: 0,
  D_BUY: 1,
  D_SELL: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.PositionTypeEnum = {
  PT_UNKNOWN: 0,
  PT_NET: 1,
  PT_GROSS: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.PositionDirectionEnum = {
  PD_UNKNOWN: 0,
  PD_NET: 1,
  PD_LONG: 2,
  PD_SHORT: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.HedgeFlagEnum = {
  HF_UNKNOWN: 0,
  HF_SPECULATION: 1,
  HF_ARBITRAGE: 2,
  HF_HEDGE: 3,
  HF_MARKETMAKER: 4,
  HF_SPECHEDGE: 5,
  HF_HEDGESPEC: 6
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderPriceTypeEnum = {
  OPT_UNKNOWN: 0,
  OPT_ANYPRICE: 1,
  OPT_LIMITPRICE: 2,
  OPT_BESTPRICE: 3,
  OPT_LASTPRICE: 4,
  OPT_LASTPRICEPLUSONETICKS: 5,
  OPT_LASTPRICEPLUSTWOTICKS: 6,
  OPT_LASTPRICEPLUSTHREETICKS: 7,
  OPT_ASKPRICE1: 8,
  OPT_ASKPRICE1PLUSONETICKS: 9,
  OPT_ASKPRICE1PLUSTWOTICKS: 10,
  OPT_ASKPRICE1PLUSTHREETICKS: 11,
  OPT_BIDPRICE1: 12,
  OPT_BIDPRICE1PLUSONETICKS: 13,
  OPT_BIDPRICE1PLUSTWOTICKS: 14,
  OPT_BIDPRICE1PLUSTHREETICKS: 15,
  OPT_FIVELEVELPRICE: 16
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OffsetFlagEnum = {
  OF_UNKONWN: 0,
  OF_OPEN: 1,
  OF_CLOSE: 2,
  OF_FORCECLOSE: 3,
  OF_CLOSETODAY: 4,
  OF_CLOSEYESTERDAY: 5,
  OF_FORCEOFF: 6,
  OF_LOCALFORCECLOSE: 7
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ForceCloseReasonEnum = {
  FCR_UNKONWN: 0,
  FCR_NOTFORCECLOSE: 1,
  FCR_LACKDEPOSIT: 2,
  FCR_CLIENTOVERPOSITIONLIMIT: 3,
  FCR_MEMBEROVERPOSITIONLIMIT: 4,
  FCR_NOTMULTIPLE: 5,
  FCR_VIOLATION: 6,
  FCR_OTHER: 7,
  FCR_PERSONDELIVER: 8
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderTypeEnum = {
  OT_UNKONWN: 0,
  OT_NORMAL: 1,
  OT_DERIVEFROMQUOTE: 2,
  OT_DERIVEFROMCOMBINATION: 3,
  OT_COMBINATION: 4,
  OT_CONDITIONALORDER: 5,
  OT_SWAP: 6
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.TimeConditionEnum = {
  TC_UNKONWN: 0,
  TC_IOC: 1,
  TC_GFS: 2,
  TC_GFD: 3,
  TC_GTD: 4,
  TC_GTC: 5,
  TC_GFA: 6
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.VolumeConditionEnum = {
  VC_UNKONWN: 0,
  VC_AV: 1,
  VC_MV: 2,
  VC_CV: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ContingentConditionEnum = {
  CC_UNKONWN: 0,
  CC_IMMEDIATELY: 1,
  CC_TOUCH: 2,
  CC_TOUCHPROFIT: 3,
  CC_PARKEDORDER: 4,
  CC_LASTPRICEGREATERTHANSTOPPRICE: 5,
  CC_LASTPRICEGREATEREQUALSTOPPRICE: 6,
  CC_LASTPRICELESSERTHANSTOPPRICE: 7,
  CC_LASTPRICELESSEREQUALSTOPPRICE: 8,
  CC_ASKPRICEGREATERTHANSTOPPRICE: 9,
  CC_ASKPRICEGREATEREQUALSTOPPRICE: 10,
  CC_ASKPRICELESSERTHANSTOPPRICE: 11,
  CC_ASKPRICELESSEREQUALSTOPPRICE: 12,
  CC_BIDPRICEGREATERTHANSTOPPRICE: 13,
  CC_BIDPRICEGREATEREQUALSTOPPRICE: 14,
  CC_BIDPRICELESSERTHANSTOPPRICE: 15,
  CC_BIDPRICELESSEREQUALSTOPPRICE: 16,
  CC_LOCALLASTPRICELESSEREQUALSTOPPRICE: 17,
  CC_LOCALLASTPRICEGREATEREQUALSTOPPRICE: 18
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ActionFlagEnum = {
  AF_UNKONWN: 0,
  AF_DELETE: 1,
  AF_MODIFY: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.TradingRightEnum = {
  TR_UNKONWN: 0,
  TR_ALLOW: 1,
  TR_CLOSEONLY: 2,
  TR_FORBIDDEN: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OrderSourceEnum = {
  ODS_UNKONWN: 0,
  ODS_PARTICIPANT: 1,
  ODS_ADMINISTRATOR: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.TradeTypeEnum = {
  TT_UNKONWN: 0,
  TT_SPLITCOMBINATION: 1,
  TT_COMMON: 2,
  TT_OPTIONSEXECUTION: 3,
  TT_OTC: 4,
  TT_EFPDERIVED: 5,
  TT_COMBINATIONDERIVED: 6,
  TT_BLOCKTRADE: 7
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.PriceSourceEnum = {
  PSRC_UNKONWN: 0,
  PSRC_LASTPRICE: 1,
  PSRC_BUY: 2,
  PSRC_SELL: 3,
  PSRC_OTC: 4
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.CurrencyEnum = {
  UNKNOWNCURRENCY: 0,
  USD: 1,
  CNY: 2,
  CNH: 3,
  HKD: 4,
  JPY: 5,
  EUR: 6,
  GBP: 7,
  DEM: 8,
  CHF: 9,
  FRF: 10,
  CAD: 11,
  AUD: 12,
  ATS: 13,
  FIM: 14,
  BEF: 15,
  THB: 16,
  IEP: 17,
  ITL: 18,
  LUF: 19,
  NLG: 20,
  PTE: 21,
  ESP: 22,
  IDR: 23,
  MYR: 24,
  NZD: 25,
  PHP: 26,
  SUR: 27,
  SGD: 28,
  KRW: 29
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.ExchangeEnum = {
  UNKNOWNEXCHANGE: 0,
  SSE: 1,
  SZSE: 2,
  CFFEX: 3,
  SHFE: 4,
  CZCE: 5,
  DCE: 6,
  SGE: 7,
  INE: 8,
  SEHK: 9,
  HKFE: 10,
  SGX: 11,
  NYBOT: 12,
  NYMEX: 13,
  COMEX: 14,
  CME: 15,
  CFE: 16,
  GLOBEX: 17,
  ICE: 18,
  IPE: 19,
  LME: 20,
  IDEALPRO: 21,
  OANDA: 22,
  FXCM: 23,
  SMART: 24
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.OptionsTypeEnum = {
  O_UNKNOWN: 0,
  O_CALLOPTIONS: 1,
  O_PUTOPTIONS: 2
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.StrikeModeEnum = {
  STM_UNKNOWN: 0,
  STM_CONTINENTAL: 1,
  STM_AMERICAN: 2,
  STM_BERMUDA: 3
}

/**
 * @enum {number}
 */
proto.xyz.redtorch.pb.CombinationTypeEnum = {
  COMBT_UNKNOWN: 0,
  COMBT_FUTURE: 1,
  COMBT_BUL: 2,
  COMBT_BER: 3,
  COMBT_STD: 4,
  COMBT_STG: 5,
  COMBT_PRT: 6,
  COMBT_CLD: 7
}

goog.object.extend(exports, proto.xyz.redtorch.pb)
