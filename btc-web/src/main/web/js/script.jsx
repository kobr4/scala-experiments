'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'
import {ApiResponseField, ResponseTable, FormRadioField, FormRadioFieldList, FormListField, FormContainer} from './components/generics'


function performRestReq(updateCallback, method, params = []) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
             var jsonResponse = JSON.parse(this.responseText)
             var fields = [];
             if (typeof jsonResponse.result === 'string' || typeof jsonResponse.result === 'number') {
                var field = <ApiResponseField name='result' value={jsonResponse.result} key='result' />
                fields.push(field);
             } else {
                 Object.keys(jsonResponse.result).forEach(function (key) {
                   var value = JSON.stringify(jsonResponse.result[key], null, 2);
                    var field = <ApiResponseField name={key} value={value} key={key} />
                    fields.push(field);
                 });
             }
             updateCallback(fields);
         }
    };
    var protocol = location.protocol;
    var slashes = protocol.concat("//");
    var host = slashes.concat(window.location.hostname+(window.location.port==8080?":8080":""));
    var paramsString = params.map(field => field[0]+"="+field[1]+"&").join('')
    xhttp.open("GET", host+"/btc-api/"+method+"?"+paramsString, true);
    xhttp.setRequestHeader("Content-type", "application/json");
    xhttp.send();
}


class ApiResponse extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [] }
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  componentDidMount() {
    performRestReq((fields) => this.updateState(fields), this.props.method);
    this.interval = setInterval(() => performRestReq( (fields) => this.updateState(fields) , this.props.method), 5000);
  }

  render() {
    return <ResponseTable responseFields={this.state.responseFields}/>;
  }
}

class ApiInputResult extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [], value : '', verbose: 'true' }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(event) {
    performRestReq((fields) => this.updateState(fields), this.props.method, [['TXID', this.state.value], ['verbose', this.state.verbose]]) ;
    event.preventDefault();
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
       <span>
       <FormContainer handleSubmit={this.handleSubmit}>
           <FormListField value={this.state.value} name="TXID" handleTextChange={(event) => this.setState({value: event.target.value}) } />
           <FormRadioFieldList name="verbose" values={['true','false']} current={this.state.verbose} handleRadioChange={ (event) => this.setState({verbose: event.target.value}) }/>
       </FormContainer>
       <ResponseTable responseFields={this.state.responseFields}/>
       </span>
    );
  }
}

class ApiHelpInputResult extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [], method : '' }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(event) {
    performRestReq((fields) => this.updateState(fields), this.props.method, [['method', this.state.method]]) ;
    event.preventDefault();
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
       <span>
       <FormContainer handleSubmit={this.handleSubmit}>
           <FormListField value={this.state.method} name="method" handleTextChange={(event) => this.setState({method: event.target.value}) } />
       </FormContainer>
       <ResponseTable responseFields={this.state.responseFields}/>
       </span>
    );
  }
}

class ApiBlockHashInputResult extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [], height : '' }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(event) {
    performRestReq((fields) => this.updateState(fields), this.props.method, [['height', this.state.height]]) ;
    event.preventDefault();
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
       <span>
       <FormContainer handleSubmit={this.handleSubmit}>
           <FormListField value={this.state.height} name="height" handleTextChange={(event) => this.setState({height: event.target.value}) } />
       </FormContainer>
       <ResponseTable responseFields={this.state.responseFields}/>
       </span>
    );
  }
}

class ApiBlockInputResult extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [], blockhash : '', verbosity : '0' }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(event) {
    performRestReq((fields) => this.updateState(fields), this.props.method, [['blockhash', this.state.blockhash],['verbosity', this.state.verbosity]]) ;
    event.preventDefault();
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
       <span>
       <FormContainer handleSubmit={this.handleSubmit}>
           <FormListField value={this.state.blockhash} name="blockhash" handleTextChange={(event) => this.setState({blockhash: event.target.value}) } />
           <FormRadioFieldList name="verbosity" values={['0','1','2']} current={this.state.verbosity} handleRadioChange={ (event) => this.setState({verbosity: event.target.value}) }/>
       </FormContainer>
       <ResponseTable responseFields={this.state.responseFields}/>
       </span>
    );
  }
}

ReactDOM.render(
    <BrowserRouter>
      <Switch>
        <Route path='/btc-api/api/getblockchaininfo' render={() => ( <ApiResponse method='getblockchaininfo'/>)} /> 
        <Route path='/btc-api/api/getnetworkinfo' render={() => ( <ApiResponse method='getnetworkinfo'/>)} /> 
        <Route path='/btc-api/api/getmempoolinfo' render={() => ( <ApiResponse method='getmempoolinfo'/>)} /> 
        <Route path='/btc-api/api/getpeerinfo' render={() => ( <ApiResponse method='getpeerinfo'/>)} />
        <Route path='/btc-api/api/getnettotals' render={() => ( <ApiResponse method='getnettotals'/>)} />
        <Route path='/btc-api/api/getblockcount' render={() => ( <ApiResponse method='getblockcount'/>)} />
        <Route path='/btc-api/api/getrawmempool' render={() => ( <ApiResponse method='getrawmempool'/>)} />
        <Route path='/btc-api/api/getrawtransaction' render={() => ( <ApiInputResult method='getrawtransaction'/>)} />
        <Route path='/btc-api/api/getmemoryinfo' render={() => ( <ApiResponse method='getmemoryinfo'/>)} />
        <Route path='/btc-api/api/getdifficulty' render={() => ( <ApiResponse method='getdifficulty'/>)} />
        <Route path='/btc-api/api/getchaintips' render={() => ( <ApiResponse method='getchaintips'/>)} />
        <Route path='/btc-api/api/getblockhash' render={() => ( <ApiBlockHashInputResult method='getblockhash'/>)} />
        <Route path='/btc-api/api/getblock' render={() => ( <ApiBlockInputResult method='getblock'/>)} />
        <Route path='/btc-api/api/help' render={() => ( <ApiHelpInputResult method='help'/>)} />
        <Route path='/' render={() => ( <ApiResponse method='getblockchaininfo'/>)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);



