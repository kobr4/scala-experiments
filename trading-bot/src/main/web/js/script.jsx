'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'
import {ApiResponseField, ResponseTable, FormRadioField, FormRadioFieldList, FormListField, FormContainer} from './components/generics'
//var LineChart = require("react-chartjs").Line;
import {Line} from 'react-chartjs'

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

function performRestPriceReq(updateCallback, path, params = []) {
  var xhttp = new XMLHttpRequest();
  xhttp.onreadystatechange = function() {
       if (this.readyState == 4 && this.status == 200) {
           var jsonResponse = JSON.parse(this.responseText)
           updateCallback(jsonResponse);
       }
  };
  var protocol = location.protocol;
  var slashes = protocol.concat("//");
  var host = slashes.concat(window.location.hostname+(window.location.port==8080?":8080":""));
  var paramsString = params.map(field => field[0]+"="+encodeURIComponent(field[1])+"&").join('')
  xhttp.open("GET", host+path+"?"+paramsString, true);
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

function HelloWorld(props) {
    return <div>Hello World !</div>;
}

class GraphResult extends React.Component {

  constructor(props) {
    super(props);

    

    var data = {
      labels: ["January", "February", "March", "April", "May", "June", "July"],
      datasets:  [ 0, 0, 0, 0, 0]
    };    

    this.state = { chartData : data }
  }

  componentDidMount() {

    performRestPriceReq((prices) => {
    var data = {
      labels: ["January", "February", "March", "April", "May", "June", "July"],
      datasets: [{
      label: "My First dataset",
      fillColor: "rgba(220,220,220,0.2)",
      strokeColor: "rgba(220,220,220,1)",
      pointColor: "rgba(220,220,220,1)",
      pointStrokeColor: "#fff",
      pointHighlightFill: "#fff",
      pointHighlightStroke: "rgba(220,220,220,1)",
      data: prices
    }]
    };
    this.setState({chartData : data});
    }, '/price_api/btc_history', [ ['start', new Date('2017-01-01T0:0:0Z').toISOString()], ['end',new Date().toISOString()]]);

  }

  render() {
    return (<Line data={this.state.chartData} width="600" height="250"/>);
   }
}


ReactDOM.render(
    <BrowserRouter>
      <Switch>
        <Route path='/btc_price' render={() => ( <GraphResult />)} />
        <Route path='/' render={() => ( <HelloWorld />)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);