import React from 'react'
import { connect } from 'react-redux'
import { SignUpForm } from '../components/signupform'
import RestUtils from '../restutils'

export const getSignUpForm = (state = {email: '', password: '', activation: false}, action) => {
    switch (action.type) {
        case 'SET_EMAIL':
            return {
                ...state,
                email: action.email       
            }
        case 'SET_PASSWORD':
            return {
                ...state,
                password: action.password       
            }
        case 'SIGN_UP':
            RestUtils.performRestPostReq((token) => {}, '/user/signup',[ ['email', state.email], ['password', state.password] ])        
            return {
                ...state,
                activation: true       
            }
        default:
            return state;                
    }
};


const mapStateToProps = state => {
    return {
        email: state.email,
        password: state.password,
        activation: state.activation
    }
};

const handleSubmit = (event) => ({
    type: 'SIGN_UP'
})

const handleEmailChange = (event) => ({
    type : 'SET_EMAIL',
    email : event.target.value
})

const handlePasswordChange = (event) => ({
    type : 'SET_PASSWORD',
    password : event.target.value
})

const mapDispatchToProps = dispatch => {
    return {
        formSubmit: (event) => { 
            event.preventDefault();
            dispatch(handleSubmit(event)) },
        formEmailChange: (event) => { dispatch(handleEmailChange(event)) },
        formPasswordChange: (event) => { dispatch(handlePasswordChange(event)) }
       }
};
  
export const SignUpFormComp = connect(
    mapStateToProps,
    mapDispatchToProps
    )(SignUpForm)
