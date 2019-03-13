import React from 'react'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'
import {Panel, FormContainer, FormGroup, FormTextField, FormPasswordField} from './generics'
import { LoginFormAction } from '../actions'
import RestUtils from '../restutils'

/*
handleSubmit = (event) => {
    RestUtils.performRestPostReq((token) => {
      document.cookie = 'authtoken='+token+';path=/'
      window.location.href = '/';
    }, '/auth/login',[ ['email', this.state.email], ['password', this.state.password] ],
    (status) => this.setState({error: true})
  )
    event.preventDefault();  
  }
*/

const LoginForm = ({email, password, error, formSubmit, formEmailChange, formPasswordChange}) => (
    <span>
    <Panel title='Sign In'>
    { error &&
    <div className="alert alert-danger">Failed to log-in. Check your parameters or contact support</div>
    }      
    <FormContainer handleSubmit={ (event) => formSubmit(event, email, password)} submit="Login">
        <FormGroup>
            <FormTextField value={email} name="email" placeholder="Enter your email..." handleTextChange={formEmailChange} />
        </FormGroup>
        <FormGroup>
            <FormPasswordField value={password} name="password" placeholder="Enter your password..." handleTextChange={formPasswordChange} />
        </FormGroup>
    </FormContainer>
    </Panel>
    </span>     
)


LoginForm.propTypes = {
    formSubmit: PropTypes.func.isRequired,
    formEmailChange: PropTypes.func.isRequired,
    formPasswordChange: PropTypes.func.isRequired,
    error: PropTypes.bool.isRequired,
    email: PropTypes.string.isRequired,
    password: PropTypes.string.isRequired
  }


const mapStateToProps = state => {
return {
    email: state.getLoginForm.email,
    password: state.getLoginForm.password,
    error: state.getLoginForm.error
}};

const handleSubmit = (event) => ({
    type: LoginFormAction.SIGN_UP
})

const handleError = (error) => ({
    type: LoginFormAction.SET_ERROR,
    error: error
})

const handleEmailChange = (event) => ({
    type : LoginFormAction.SET_EMAIL,
    email : event.target.value
})

const handlePasswordChange = (event) => ({
    type : LoginFormAction.SET_PASSWORD,
    password : event.target.value
})

const mapDispatchToProps = dispatch => {
    return {
        formSubmit: (event, email, password) => { 
            event.preventDefault();
            RestUtils.performRestPostReq(
                (token) => {
                    document.cookie = 'authtoken='+token+';path=/'
                    window.location.href = '/';
                }, 
                '/auth/login',[ ['email', email], ['password', password] ],
                (status) => { dispatch(handleError(true)) }
            );
              
        },
        formEmailChange: (event) => { dispatch(handleEmailChange(event)) },
        formPasswordChange: (password) => {
            dispatch(handlePasswordChange(password))
        }
       }
};
  
export default connect(
    mapStateToProps,
    mapDispatchToProps
    )(LoginForm)