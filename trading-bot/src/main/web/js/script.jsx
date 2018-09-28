'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { ResponseTable, FormRow, FormTable, FormTextField, FormButton, FormContainer, Panel, PanelTable} from './components/generics'

import Helper from './components/helper'
import RestUtils from './restutils'
import {Line} from 'react-chartjs'
import DatePicker from 'react-datepicker';
import moment from 'moment';

import 'react-datepicker/dist/react-datepicker.css';

const priceEndpoint = '/price_api/price_history';
const priceAtEndpoint = '/price_api/price_at';
const movingEndpoint = '/price_api/moving';



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

  return {lastPortfolioValue: lastPortfolioValue, performance: (performance > 0 ? '+':'')+performance+' %', buyAndHoldValue: buyAndHoldValue, buyAndHoldPerformance: (buyAndHoldPerformance > 0 ? '+':'')+buyAndHoldPerformance+' %'} 
}


function buildExecutionResult(tradeBotResponse, initial, asset, start, end, updateCallback) {
  var promises = [ 
    RestUtils.performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', asset], ['date', start.format(moment.defaultFormatUtc)] ]), 
    RestUtils.performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', asset], ['date', end.format(moment.defaultFormatUtc)] ])
    ];
  
  Promise.all(promises).then( (prices) => {
    let result = computeExecutionResult(tradeBotResponse, initial, prices[0], prices[1]);
    updateCallback(result);
  });
}

class GraphResult extends React.Component {

  requestBTC = () => RestUtils.performRestPriceReq((prices) => { this.setState({btcdatapoints : prices}) }, 
    this.props.endpoint, [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], ['end',this.state.end.format(moment.defaultFormatUtc)]]);
  

  requestMA30 = () => RestUtils.performRestPriceReq((prices) => { this.setState({ma30datapoints : prices})}, 
    movingEndpoint, [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], ['end',this.state.end.format(moment.defaultFormatUtc)], ['days', 30] ]);

  handleSubmit = (event) => {
    RestUtils.performRestReq((tradeBotResponse) => {
      this.setState({responseFields : Helper.buildResponseComponent(tradeBotResponse)});
      buildExecutionResult(tradeBotResponse, this.state.initial, this.props.asset, this.state.start, this.state.end, (result) =>  this.setState({executionResultFields: Helper.rowsFromObjet(result)}));
      
    
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

    setInterval(() => RestUtils.performRestPriceReq((quotes) => {
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
              <FormTable>
                <FormRow label='Start'>
                  <DatePicker title='start' selected={this.state.start} onChange={(date) => this.setState({start: date})}/>
                </FormRow>
                <FormRow label='End'>
                  <DatePicker title='end' selected={this.state.end} onChange={(date) => this.setState({end: date})}/>
                </FormRow>
                <FormRow label='Initial amount (USD)'>
                  <FormTextField value={this.state.initial} name="initial" handleTextChange={(event) => this.setState({initial: event.target.value})} />
                </FormRow>
                <FormRow>
                  <FormButton text='Update' handleClick={ (event) => this.componentDidMount() }/>
                </FormRow>
              </FormTable>    
            </FormContainer>
          </Panel>
          {
            this.state.executionResultFields.length > 0 &&
            <Panel title='TradeBot execution results'>
            <PanelTable>
              {this.state.executionResultFields}
            </PanelTable>
          </Panel>
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