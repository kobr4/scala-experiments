import { combineReducers } from 'redux'

import getSignUpForm from './signupform'
import getTradingGlobal from './tradingglobal'
import getLoginForm from './loginform'
import getTradingForm from './tradingform'

export default combineReducers({
    getSignUpForm,
    getTradingGlobal,
    getLoginForm,
    getTradingForm
})