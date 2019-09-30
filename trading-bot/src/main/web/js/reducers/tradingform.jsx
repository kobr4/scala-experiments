import { TradingFormAction } from '../actions'

export default function(state = {scheduledTradings: [], tradingJob: '', apiKeys: [], tradeWeights: [], newWeight: '', useCustom: false, newTradingStrategy: ''}, action) {
    switch (action.type) {
        case TradingFormAction.ADD_TRADE_WEIGHT:
            return {
                ...state
            }
        case TradingFormAction.ADD_TRADING_JOB:
            return {
                ...state
            }
        case TradingFormAction.SET_API_KEY_ID:
            return {
                ...state
            }
        case TradingFormAction.SET_BASE_ASSET:
            return {
                ...state
            }
        case TradingFormAction.SET_CUSTOM_STRATEGY:
            return {
                ...state
            }
        case TradingFormAction.SET_TRADE_WEIGHT:
            return {
                ...state,
                newWeight: action.newWeight
            }
        case TradingFormAction.SET_TRADING_JOB:
            return {
                ...state
            }
        case TradingFormAction.SET_USE_CUSTOM:
            return {
                ...state,
                useCustom: action.useCustom
            }
        default:
            return state;             
    }
};