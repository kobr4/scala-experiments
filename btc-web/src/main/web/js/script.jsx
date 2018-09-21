'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td><pre>{props.value}</pre></td></tr>;
}

function ResponseTable(props) {
  return (
       <table className="table table-bordered table-hover">
       <tbody>
       <tr><th>Name</th><th>Value</th></tr>
       {
         props.responseFields.map(field => field)
       }
       </tbody>
       </table>
  );
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
    return <ResponseTable responseFields={this.state.responseFields}/>;
  }
}

function FormRadioField(props) {
  return <span><input type="radio" name={props.name} value={props.value} checked={props.current === props.value} onChange={props.handleRadioChange}/>{props.value}</span>;
}

function FormRadioFieldList(props) {
    var rlist = []
    props.values.map(value => {
        var radioField = <FormRadioField name={props.name} value={value} handleRadioChange={props.handleRadioChange} current={props.current} key={props.name+value}/>;
        rlist.push(radioField);
    });
    return <label>{props.name}:{rlist}</label>;
}

function FormListField(props) {
  return <label>{props.name}<input type="text" value={props.value} name={props.name} onChange={props.handleTextChange} /></label>;
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
       <form onSubmit={this.handleSubmit}>
           <FormListField value={this.state.value} name="TXID" handleTextChange={(event) => this.setState({value: event.target.value}) } />
           <FormRadioFieldList name="verbose" values={['true','false']} current={this.state.verbose} handleRadioChange={ (event) => this.setState({verbose: event.target.value}) }/>
         <input type="submit" value="Submit" />
       </form>
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
        <Route path='/' render={() => ( <ApiResponse method='getblockchaininfo'/>)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);



