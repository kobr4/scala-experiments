'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td>{props.value}</td></tr>;
}

function performRestReq(updateCallback) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
             var jsonResponse = JSON.parse(this.responseText)
             var fields = [];
             Object.keys(jsonResponse.result).forEach(function (key) {
               var value = JSON.stringify(jsonResponse.result[key]);
                var field = <ApiResponseField name={key} value={value} key={key} />
                fields.push(field);
             });
             updateCallback(fields);
         }
    };
    xhttp.open("GET", "http://localhost:8080/btc-api/getblockchaininfo", true);
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
    performRestReq((fields) => this.updateState(fields));
    this.interval = setInterval(() => performRestReq( (fields) => this.updateState(fields) ), 5000);
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
        <Route path='/' render={() => (
          <ApiResponse />
        )} />
        <Route component={ApiResponse}/>
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);



