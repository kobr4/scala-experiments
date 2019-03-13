import { combineReducers } from 'redux'

import getSignUpForm from './signupform'
import getTradingGlobal from './tradingglobal'
import getLoginForm from './loginform'

export default combineReducers({
    getSignUpForm,
    getTradingGlobal,
    getLoginForm
})