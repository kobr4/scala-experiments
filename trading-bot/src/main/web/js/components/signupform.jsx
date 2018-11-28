import React from 'react'
import PropTypes from 'prop-types'
import {Panel, FormContainer, FormTable, FormRow, FormTextField, FormPasswordField} from './generics'


export const SignUpForm = ({activation, email, password, formSubmit, formEmailChange, formPasswordChange }) => (
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

//export default SignUpFormZ