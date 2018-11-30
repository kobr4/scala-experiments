import React from 'react'
import PropTypes from 'prop-types'
import {Panel, FormContainer, FormTable, FormRow, FormTextField, FormPasswordField} from './generics'
import { connect } from 'react-redux'
import { SignUpAction } from '../actions'

const SignUpForm = ({activation, email, password, formSubmit, formEmailChange, formPasswordChange }) => (
    <span>
    { !activation && 
    <Panel title='Sign up form'>
    <FormContainer handleSubmit={formSubmit} submit="Sign up">
    <FormTable>
    <FormRow label="Email"><FormTextField value={email} name="email" handleTextChange={formEmailChange} /></FormRow>
    <FormRow label="Password"><FormPasswordField value={password} name="password" handleTextChange={formPasswordChange} /></FormRow>
    </FormTable>
    </FormContainer>
    </Panel>
    } 
    { activation &&
      <Panel title='Sign up form submitted'>
      An email has been sent to {email}, follow the included link to activate your account. 
      </Panel>
    }
    </span>
)

SignUpForm.propTypes = {
    formSubmit: PropTypes.func.isRequired,
    formEmailChange: PropTypes.func.isRequired,
    formPasswordChange: PropTypes.func.isRequired,
    activation: PropTypes.bool.isRequired,
    email: PropTypes.string.isRequired,
    password: PropTypes.string.isRequired
  }


  const mapStateToProps = state => {
    return {
        email: state.getSignUpForm.email,
        password: state.getSignUpForm.password,
        activation: state.getSignUpForm.activation
    }
};

const handleSubmit = (event) => ({
    type: SignUpAction.SIGN_UP
})

const handleEmailChange = (event) => ({
    type : SignUpAction.SET_EMAIL,
    email : event.target.value
})

const handlePasswordChange = (event) => ({
    type : SignUpAction.SET_PASSWORD,
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
  
export default connect(
    mapStateToProps,
    mapDispatchToProps
    )(SignUpForm)