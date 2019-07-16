import { SignUpAction } from '../actions'

export default function(state = {email: '', password: {}, errorMessage: '', activation: false, score: ''}, action) {
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
        case SignUpAction.SET_ERROR:
            return {
                ...state,
                errorMessage: action.errorMessage
            }
        default:
            return state;                
    }
};



