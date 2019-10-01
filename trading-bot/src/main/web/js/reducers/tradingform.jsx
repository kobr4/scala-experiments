import { TradingFormAction } from '../actions'
import { defaultStrategy } from '../constants'

export default function(state = {scheduledTradings: [], tradingJob: '', apiKeys: [], tradeWeights: {}, newWeight: '', newAsset: 'BTC', 
    baseAsset: 'BTC', useCustom: false, newTradingStrategy: defaultStrategy, balanceFields: null, newTradingCron: '', apiKeyId: null}, action) {
    switch (action.type) {
        case TradingFormAction.ADD_TRADE_WEIGHT:
            const tradeWeights = {...state.tradeWeights}
            tradeWeights[state.newAsset] = state.newWeight
            return {
                ...state,
                tradeWeights: tradeWeights
            }
        case TradingFormAction.SET_ASSET:
            return {
                ...state,
                newAsset: action.newAsset
            }
        case TradingFormAction.SET_TRADE_WEIGHT:
            return {
                ...state,
                tradeWeights: action.tradeWeights
            }
        case TradingFormAction.ADD_TRADING_JOB:
            return {
                ...state
            }
        case TradingFormAction.SET_API_KEY_ID:
            return {
                ...state,
                apiKeyId: action.apiKeyId
            }
        case TradingFormAction.SET_BASE_ASSET:
            return {
                ...state,
                baseAsset: action.baseAsset
            }
        case TradingFormAction.SET_CUSTOM_STRATEGY:
            return {
                ...state,
                newTradingStrategy: newTradingStrategy
            }
        case TradingFormAction.SET_WEIGHT:
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
        case TradingFormAction.SET_SCHEDULED_TRADINGS:
            return {
                ...state,
                scheduledTradings: action.scheduledTradings
            }
        case TradingFormAction.SET_API_KEYS: 
            return {
                ...state,
                apiKeys: action.apiKeys,
                apiKeyId: (action.apiKeys.length > 0)? action.apiKeys[0].id:null
            }
        default:
            return state;             
    }
};