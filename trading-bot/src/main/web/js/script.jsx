'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'
import {ApiResponseField, ApiResponseSpanField, ResponseTable, FormRadioField, FormRadioFieldList, FormTextField, FormButton, FormContainer, Panel, PanelTable} from './components/generics'
import {Line} from 'react-chartjs'
import DatePicker from 'react-datepicker';
import moment from 'moment';

import 'react-datepicker/dist/react-datepicker.css';

const priceEndpoint = '/price_api/price_history';
const priceAtEndpoint = '/price_api/price_at';
const movingEndpoint = '/price_api/moving';


function buildResponseComponent(jsonResponse) {
  var fields = [];
  if (typeof jsonResponse.result === 'string' || typeof jsonResponse.result === 'number') {
     var field = <ApiResponseField name='result' value={jsonResponse.result} key='result' />
     fields.push(field);
  } if (Array.isArray(jsonResponse)) {
   jsonResponse.forEach(function(data){
     var field = <ApiResponseField name={data.date} value= {JSON.stringify(data.order)}/>
     fields.push(field);
   })

  } else {
      Object.keys(jsonResponse.result).forEach(function (key) {
        var value = JSON.stringify(jsonResponse.result[key], null, 2);
         var field = <ApiResponseField name={key} value={value} key={key} />
         fields.push(field);
      });
  }
  return fields;
}

function rowsFromObjet(jsObj) {
  var fields = [];  
  Object.keys(jsObj).forEach(function (key) {
    fields.push(<tr><td>{key}</td><td>{jsObj[key]}</td></tr>);
  })
  return fields;
}

function performRestReq(updateCallback, path, params = []) {
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
  var paramsString = params.map(field => field[0]+"="+field[1]+"&").join('')
  xhttp.open("GET", host+path+"?"+paramsString, true);
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

function performRestPriceReqWithPromise(path, params = []) {
  return new Promise( (resolve, reject) =>  {
    performRestPriceReq( (jsonResponse) => resolve(jsonResponse), path, params);
  })
}

function HelloWorld(props) {
    return <div>Hello World !</div>;
}

function GraphResultBase(props) {
  let datas = props.datas.map(data => 
    ({ 
        label: data.name,
        fillColor: 'rgba(0,0,0,0)',
        strokeColor: data.color,
        pointColor: data.color,
        data : data.datapoints
      })
    )
    let chartData = { labels : [],datasets : datas };
  return (
    <Line data={chartData} width="800" height="400"/>
  );
}


function computeExecutionResult(tradeBotResponse, initial, firstDayPrice, lastDayPrice) {

  var lastPortfolioValue = (tradeBotResponse.slice(-1)[0].order.price *  tradeBotResponse.slice(-1)[0].order.quantity).toFixed(2);
  
  var performance = (lastPortfolioValue * 100 / initial - 100).toFixed(2);

  var buyAndHoldValue =  ((initial / firstDayPrice) * lastDayPrice).toFixed(2);

  var buyAndHoldPerformance = (buyAndHoldValue * 100 / initial - 100).toFixed(2);

  return {lastPortfolioValue: lastPortfolioValue, performance: performance+' %', buyAndHoldValue: buyAndHoldValue, buyAndHoldPerformance: buyAndHoldPerformance+' %'} 
}


function buildExecutionResult(tradeBotResponse, initial, asset, start, end, updateCallback) {
  var promises = [ 
      performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', asset], ['date', start.format(moment.defaultFormatUtc)] ]), 
      performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', asset], ['date', end.format(moment.defaultFormatUtc)] ])
    ];
  
  Promise.all(promises).then( (prices) => {
    let result = computeExecutionResult(tradeBotResponse, initial, prices[0], prices[1]);
    updateCallback(result);
  });
}


function ExecutionResult(props) {
  return (
    <Panel title='TradeBot execution results'>
    <PanelTable>
      {props.executionResultFields}
    </PanelTable>
  </Panel>
  );
}


class GraphResult extends React.Component {

  requestBTC = () => performRestPriceReq((prices) => { this.setState({btcdatapoints : prices}) }, 
    this.props.endpoint, [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], ['end',this.state.end.format(moment.defaultFormatUtc)]]);
  

  requestMA30 = () => performRestPriceReq((prices) => { this.setState({ma30datapoints : prices})}, 
    movingEndpoint, [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], ['end',this.state.end.format(moment.defaultFormatUtc)], ['days', 30] ]);

  handleSubmit = (event) => {
    performRestReq((tradeBotResponse) => {
      this.setState({responseFields : buildResponseComponent(tradeBotResponse)});
      buildExecutionResult(tradeBotResponse, this.state.initial, this.props.asset, this.state.start, this.state.end, (result) =>  this.setState({executionResultFields: rowsFromObjet(result)}));
      
    
    }, '/trade_bot', [['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], ['end',this.state.end.format(moment.defaultFormatUtc)], ['initial', this.state.initial]]) ;
    event.preventDefault();
  }

    
  constructor(props) {
    super(props);
    
    this.state = { 
      ma30datapoints : [], 
      btcdatapoints : [], 
      responseFields : [], 
      initial : '10000', 
      start : moment("2017-01-01"), 
      end : moment(), 
      currencyFields : [],
      executionResultFields : []
    }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentDidMount() {

    setInterval(() => performRestPriceReq((quotes) => {
        let currencyFields = [];
        for(let q of quotes) {
          currencyFields.push(
            <tr>
              <td>{q.pair.right}</td>
              <td>{q.last+' '+q.pair.left}</td>
              <td>{q.percentChange}</td>
            </tr>
          );
        }
        this.setState({currencyFields : currencyFields});
    }, '/ticker'),5000);
      
    this.requestBTC();
    this.requestMA30();

  }



  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
          <span>
          <Panel title='Real-time prices'>
            <PanelTable headers={['Currency','Price', 'Change']}>
              {this.state.currencyFields}
            </PanelTable>
          </Panel>

          <Panel title={this.props.asset+' price history'}>
            <GraphResultBase datas={[ { name: this.props.asset, color: 'rgba(220,220,220,1)', datapoints: this.state.btcdatapoints},
              { name: 'MA30', color: 'rgba(220,0,0,1)', datapoints: this.state.ma30datapoints } ]}/>
            <FormContainer handleSubmit={this.handleSubmit} submit="Run trade-bot">  
              <table>
              <tbody>      
              <tr><td>Start</td><td><DatePicker title='start' selected={this.state.start} onChange={(date) => this.setState({start: date})}/></td></tr>
              <tr><td>End</td><td><DatePicker title='end' selected={this.state.end} onChange={(date) => this.setState({end: date})}/></td></tr>
              <tr><td>Initial amount (USD)</td><td><FormTextField value={this.state.initial} name="initial" handleTextChange={(event) => this.setState({initial: event.target.value})} /></td></tr>
              <tr><td><FormButton text='Update' handleClick={ (event) => this.componentDidMount() }/></td></tr>
              </tbody>
              </table>
            </FormContainer>
          </Panel>
          {
            this.state.executionResultFields.length > 0 &&
            <ExecutionResult executionResultFields={this.state.executionResultFields}/>
          }

          { this.state.responseFields.length > 0 &&
            <Panel title='Trade-bot output'>
              <ResponseTable first='Date' second='Order' responseFields={this.state.responseFields}/>
            </Panel>
          }
          </span>  
      );
   }
}


ReactDOM.render(
    <BrowserRouter>
      <Switch>
        <Route path='/btc_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='BTC backtest' asset='BTC'/>)} />
        <Route path='/eth_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='ETH backtest' asset='ETH'/>)} />
        <Route path='/' render={() => ( <HelloWorld />)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);