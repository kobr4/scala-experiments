import { combineReducers } from 'redux'

import getSignUpForm from './signupform'
import getTradingGlobal from './tradingglobal'

export default combineReducers({
    getSignUpForm,
    getTradingGlobal
})