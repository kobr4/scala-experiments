import React from 'react'
import PropTypes from 'prop-types'
import {Panel, FormContainer, FormTable, FormRow, FormTextField, FormPasswordField} from './generics'
import { connect } from 'react-redux'
import { SignUpAction } from '../actions'
import ReactPasswordStrength from 'react-password-strength'

const SignUpForm = ({activation, email, password, formSubmit, formEmailChange, formPasswordScoreChange }) => (
    <span>
    { !activation && 
    <Panel title='Sign up form'>
    <FormContainer handleSubmit={formSubmit} submit="Sign up">
    <FormTable>
    <FormRow label="Email"><FormTextField value={email} name="email" handleTextChange={formEmailChange} /></FormRow>
    <FormRow label="Password">
        <ReactPasswordStrength
            className="customClass"
            minLength={5}
            minScore={2}
            scoreWords={['weak', 'okay', 'good', 'strong', 'stronger']}
            changeCallback={formPasswordScoreChange}
            inputProps={{ name: "password", autoComplete: "off", className: "form-control" }}
        />
    </FormRow>
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

const handlePasswordChange = (password) => ({
    type : SignUpAction.SET_PASSWORD,
    password : password
})

const handleScoreChange = (score) => ({
    type: SignUpAction.SET_SCORE,
    score : score
})

const mapDispatchToProps = dispatch => {
    return {
        formSubmit: (event) => { 
            event.preventDefault();
            RestUtils.performRestPostReq((token) => {}, '/user/signup',[ ['email', state.email], ['password', state.password] ]);
            dispatch(handleSubmit(event)) },
        formEmailChange: (event) => { dispatch(handleEmailChange(event)) },
        formPasswordScoreChange: (score, password, isValid) => {
            dispatch(handlePasswordChange(password))
            dispatch(handleScoreChange(score)) 
        }
       }
};
  
export default connect(
    mapStateToProps,
    mapDispatchToProps
    )(SignUpForm)