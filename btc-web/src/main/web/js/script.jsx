'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td>{props.value}</td></tr>;
}

function performRestReq(updateCallback, method) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
             var jsonResponse = JSON.parse(this.responseText)
             var fields = [];
             Object.keys(jsonResponse.result).forEach(function (key) {
               var value = JSON.stringify(jsonResponse.result[key], null, 2);
                var field = <ApiResponseField name={key} value={value} key={key} />
                fields.push(field);
             });
             updateCallback(fields);
         }
    };
    var protocol = location.protocol;
    var slashes = protocol.concat("//");
    var host = slashes.concat(window.location.hostname);
    xhttp.open("GET", host+"/btc-api/"+method, true);
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
       <table>
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


ReactDOM.render(
    <BrowserRouter>
      <Switch>
        <Route path='/btc-api/api/getblockchaininfo' render={() => ( <ApiResponse method='getblockchaininfo'/>)} /> 
        <Route path='/btc-api/api/getnetworkinfo' render={() => ( <ApiResponse method='getnetworkinfo'/>)} /> 
        <Route path='/btc-api/api/getmempoolinfo' render={() => ( <ApiResponse method='getmempoolinfo'/>)} /> 
        <Route path='/btc-api/api/getpeerinfo' render={() => ( <ApiResponse method='getpeerinfo'/>)} />
        <Route path='/btc-api/api/getnettotals' render={() => ( <ApiResponse method='getnettotals'/>)} />
        <Route path='/' render={() => ( <ApiResponse method='getblockchaininfo'/>)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);



