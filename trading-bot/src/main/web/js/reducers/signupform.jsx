import React from 'react'
import RestUtils from '../restutils'
import { SignUpAction } from '../actions'

export default function(state = {email: '', password: '', activation: false}, action) {
    switch (action.type) {
        case SignUpAction.SET_EMAIL:
            console.log('action='+action.type)
            return {
                ...state,
                email: action.email       
            }
        case SignUpAction.SET_PASSWORD:
            console.log('action='+action.type)
            return {
                ...state,
                password: action.password       
            }
        case SignUpAction.SIGN_UP:
            RestUtils.performRestPostReq((token) => {}, '/user/signup',[ ['email', state.email], ['password', state.password] ])        
            return {
                ...state,
                activation: true       
            }
        default:
            return state;                
    }
};



