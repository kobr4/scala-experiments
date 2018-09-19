'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td><pre>{props.value}</pre></td></tr>;
}

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
    return (
       <table class="table table-bordered table-hover">
       <tbody>
       <tr><th>Name</th><th>Value</th></tr>
       {
         this.state.responseFields.map(field => field)
       }
       </tbody>
       </table>
    );
  }
}

class ApiInputResult extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [], value : '', verbose: 'true' }

    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.handleRadioChange = this.handleRadioChange.bind(this);
  }

  handleChange(event) {
    this.setState({value: event.target.value});
  }

  handleRadioChange(event) {
    this.setState({verbose: event.target.value});
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
       <div>
       <form onSubmit={this.handleSubmit}>
         <label>
           TXID:
           <input type="text" value={this.state.value} name="TXID" onChange={this.handleChange} />
         </label>
         <label>
            Decode:
            <input type="radio" name="verbose" value="true" checked={this.state.verbose === 'true'} onChange={this.handleRadioChange}/>true
            <input type="radio" name="verbose" value="false" checked={this.state.verbose === 'false'} onChange={this.handleRadioChange}/>false
          </label>
         <input type="submit" value="Submit" />
       </form>
       <table class="table table-bordered table-hover">
       <tbody>
       <tr><th>Name</th><th>Value</th></tr>
       {
         this.state.responseFields.map(field => field)
       }
       </tbody>
       </table>
       </div>
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
        <Route path='/' render={() => ( <ApiResponse method='getblockchaininfo'/>)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);



