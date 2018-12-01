import React from 'react'
import RestUtils from '../restutils'
import { TradingGlobalAction } from '../actions'

export default function(state = {}, action) {
    switch (action.type) {
        case TradingGlobalAction.END_FETCH_TRADE:
            return { 
                ...state,
                [action.name] : action.value
            }
        default:
            return state;                
    }
};