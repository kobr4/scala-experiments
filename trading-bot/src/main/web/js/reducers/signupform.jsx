import React from 'react'
import RestUtils from '../restutils'
import { SignUpAction } from '../actions'

export default function(state = {email: '', password: '', activation: false, score: ''}, action) {
    switch (action.type) {
        case SignUpAction.SET_EMAIL:
            return {
                ...state,
                email: action.email       
            }
        case SignUpAction.SET_PASSWORD:
            return {
                ...state,
                password: action.password       
            }
        case SignUpAction.SET_SCORE:
            return {
                ...state,
                score: action.score       
            }            
        case SignUpAction.SIGN_UP:       
            return {
                ...state,
                activation: true       
            }
        default:
            return state;                
    }
};



