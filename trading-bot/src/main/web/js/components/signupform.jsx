import React from 'react'
import PropTypes from 'prop-types'
import {Panel, FormContainer, FormTable, FormRow, FormGroup, FormEmailField, FormTextField, FormPasswordField} from './generics'
import { connect } from 'react-redux'
import { SignUpAction } from '../actions'
import ReactPasswordStrength from 'react-password-strength'
import RestUtils from '../restutils'

const SignUpForm = ({errorMessage, activation, email, password, formSubmit, formEmailChange, formPasswordScoreChange }) => (
    <span>
    { !activation && 
    <Panel title='Sign up form'>
    <FormContainer handleSubmit={ (event) => formSubmit(event, email, password)} submit="Sign up">
    <FormGroup>
        <FormEmailField value={email} placeholder="Enter your email..." name="email" handleTextChange={formEmailChange} />
    </FormGroup>
    <FormGroup>
        <ReactPasswordStrength
            minLength={5}
            minScore={2}
            scoreWords={['weak', 'okay', 'good', 'strong', 'stronger']}
            changeCallback={formPasswordScoreChange}
            inputProps={{ name: "password", placeholder: "Enter your password...", autoComplete: "off" }}
        />
    </FormGroup>
    { errorMessage &&
        <FormGroup>
        <div className="alert alert-danger">
            { errorMessage }
        </div>
        </FormGroup>
    }   
    </FormContainer>
    </Panel>
    } 
    { activation &&
      <Panel title='Sign up form submitted'>
      <div className="alert alert-success">
        An email has been sent to {email}, follow the included link to activate your account. 
      </div>
      </Panel>
    }
    </span>
)

SignUpForm.propTypes = {
    formSubmit: PropTypes.func.isRequired,
    formEmailChange: PropTypes.func.isRequired,
    formPasswordChange: PropTypes.func.isRequired,
    errorMessage: PropTypes.string,
    activation: PropTypes.bool.isRequired,
    email: PropTypes.string.isRequired,
    password: PropTypes.object.isRequired
  }


const mapStateToProps = state => {
return {
    errorMessage: state.getSignUpForm.errorMessage,
    email: state.getSignUpForm.email,
    password: state.getSignUpForm.password,
    activation: state.getSignUpForm.activation
}};

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

const handleErrorMessage = (error) => ({
    type: SignUpAction.SET_ERROR,
    errorMessage: error

})

const mapDispatchToProps = dispatch => {
    return {
        formSubmit: (event, email, password) => { 
            event.preventDefault();
            RestUtils.performRestPostReq(
                (token) => { dispatch(handleSubmit(event))}, 
                '/user/signup',[ ['email', email], ['password', password.password] ],
                (status) => {  dispatch(handleErrorMessage('An error has occured during signup.\n Please check your parameters.')) });
            },
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