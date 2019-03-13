import { LoginFormAction } from '../actions'

export default function(state = {email: '', password: '', error: false}, action) {
    switch (action.type) {
        case LoginFormAction.SET_EMAIL:
            return {
                ...state,
                email: action.email       
            }
        case LoginFormAction.SET_PASSWORD:
            return {
                ...state,
                password: action.password       
            }
        case LoginFormAction.SET_ERROR:
        return {
            ...state,
            error: action.error       
        }                        
        case LoginFormAction.SIGN_IN:       
            return {
                ...state   
            }
        default:
            return state;                
    }
};
