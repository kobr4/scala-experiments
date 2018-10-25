'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { ResponseTable, FormRow, FormTable, FormTextField, FormButton, FormContainer, FormOption, Panel, PanelTable} from './components/generics'

import Helper from './components/helper'
import RestUtils from './restutils'
import {Line} from 'react-chartjs'
import DatePicker from 'react-datepicker';
import moment from 'moment';

import 'react-datepicker/dist/react-datepicker.css';

const priceEndpoint = '/price_api/price_history';
const tickerEndpoint = '/price_api/ticker';
const priceAtEndpoint = '/price_api/price_at';
const movingEndpoint = '/price_api/moving';
const balanceEndpoint = '/trading_api/balances';
const openOrdersEndpoint = '/trading_api/open_orders';


function HelloWorld(props) {
    return <div>Hello World !</div>;
}

function GraphResultBase(props) {
  const datas = props.datas.map(data => 
    ({ 
        label: data.name,
        fillColor: 'rgba(0,0,0,0)',
        strokeColor: data.color,
        pointColor: data.color,
        data : data.datapoints
      })
    )
    const chartData = { labels : [],datasets : datas };
  return (
    <Line data={chartData} width="800" height="400"/>
  );
}


function computeExecutionResult(tradeBotResponse, initial, firstDayPrice, lastDayPrice, currency_left, currency_right, start, end) {

  var lastPortfolioValue = (tradeBotResponse.slice(-1)[0].price *  tradeBotResponse.slice(-1)[0].quantity).toFixed(2);
  
  var performance = (lastPortfolioValue * 100 / initial - 100).toFixed(2);

  var buyAndHoldValue =  ((initial / firstDayPrice) * lastDayPrice).toFixed(2);

  var buyAndHoldPerformance = (buyAndHoldValue * 100 / initial - 100).toFixed(2);

  return {
    asset : currency_right,
    start : start.format("MMM Do YY"),
    end : end.format("MMM Do YY"),
    initial : initial+' '+currency_left,
    lastPortfolioValue: lastPortfolioValue+' '+currency_left, 
    performance: (performance > 0 ? '+':'')+performance+' %', 
    buyAndHoldValue: buyAndHoldValue+' '+currency_left, 
    buyAndHoldPerformance: (buyAndHoldPerformance > 0 ? '+':'')+buyAndHoldPerformance+' %'
  } 
}


function buildExecutionResult(tradeBotResponse, initial, currency_left, currency_right, start, end, updateCallback) {
  var promises = [ 
    RestUtils.performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', currency_right], ['date', start.format(moment.defaultFormatUtc)] ]), 
    RestUtils.performRestPriceReqWithPromise(priceAtEndpoint, [ ['asset', currency_right], ['date', end.format(moment.defaultFormatUtc)] ])
    ];
  
  Promise.all(promises).then( (prices) => {
    let result = computeExecutionResult(tradeBotResponse, initial, prices[0], prices[1], currency_left, currency_right, start, end);
    updateCallback(result);
  });
}

function ExecutionResultPanel(props) {
  return (
    <tbody>
      <tr><td>Traded pair</td><td>{props.result.asset}</td></tr>
      <tr><td>Traded period</td><td>{props.result.start} to {props.result.end}</td></tr>
      <tr><td>Initial amount</td><td>{props.result.initial}</td></tr>
      <tr><td>Current portfolio valuation</td><td>{props.result.lastPortfolioValue} [ {props.result.performance} ]</td></tr>
      <tr><td>Buy and hold valuation</td><td>{props.result.buyAndHoldValue} [ {props.result.buyAndHoldPerformance} ]</td></tr>
    </tbody>
  );  
}

class TradingGlobal extends React.Component {

  handleTrade = (asset) => {
    RestUtils.performRestReq((tradeBotResponse) => {
      let storeObj = new Object();
      storeObj[asset]=tradeBotResponse.slice(-1)[0];
      this.setState(storeObj);
    }, '/trade_bot', [['asset', asset], ['start', moment("2017-01-01").format(moment.defaultFormatUtc)], ['end',moment().format(moment.defaultFormatUtc)], ['initial', 10000], ['fees', 0.1], ['strategy', 'safe']]) ;
  }


  constructor(props) {
    super(props);

    this.state = {}
  }

  componentDidMount() {
    this.handleTrade('BTC');
    this.handleTrade('ETH');
    this.handleTrade('XMR');
  }

  formatOrder = (order) => {
    return order.type+' at '+order.price+' on '+moment(order.date).format('LL');
  }

  render() {
    return (
      <Panel title='Introduction'>
      <p>
        Simple and honest trading algorithm with a provable performance record.<br/>
        Positions are evaluated on daily basis.
      </p>
      Last take on BTC : { this.state.BTC && this.formatOrder(this.state.BTC) }<br/>
      Last take on ETH : { this.state.ETH && this.formatOrder(this.state.ETH) }<br/>
      Last take on XMR : { this.state.XMR && this.formatOrder(this.state.XMR) }<br/>
      </Panel>
      )}
}

class TradingForm extends React.Component {

  requestBalance = () => RestUtils.performRestPostReq(
    (balanceList)=> { this.setState({balanceFields: Helper.rowsFromObjet(balanceList)})},
    balanceEndpoint,
    [['apikey', this.state.apikey],['apisecret',this.state.apisecret]]
  )

  requestOpenOrders = () => RestUtils.performRestPostReq(
    (balanceList)=> { this.setState({balanceFields: Helper.rowsFromObjet(balanceList)})},
    openOrdersEndpoint,
    [['apikey', this.state.apikey],['apisecret',this.state.apisecret]]
  )  

  constructor(props) {
    super(props);

    this.state = {apikey :'', apisecret : '', balanceFields : null} 
  }

  render() {
    return (
      <span>
      <Panel title='Trading operations'>
        <FormContainer handleSubmit={this.handleSubmit} submit="Run trade-bot">  
          <FormTable>
            <FormRow label='API Key'>
              <FormTextField value={this.state.apikey} name="apikey" handleTextChange={(event) => this.setState({apikey: event.target.value})} />
            </FormRow>
            <FormRow label='API Secret'>
              <FormTextField value={this.state.apisecret} name="apisecret" handleTextChange={(event) => this.setState({apisecret: event.target.value})} />
            </FormRow>                
            <FormRow>
              <FormButton text='Show balances' handleClick={ (event) => this.requestBalance() }/>
            </FormRow>
            <FormRow>
              <FormButton text='Show open orders' handleClick={ (event) => this.requestOpenOrders() }/>
            </FormRow>            
          </FormTable>    
        </FormContainer>      
      </Panel>
      { this.state.balanceFields && 
        <Panel title='Request output'>
        <PanelTable>
        {this.state.balanceFields} 
        </PanelTable>
        </Panel>
      }
      </span>
    )
  }
}


class GraphResult extends React.Component {

  requestBTC = () => RestUtils.performRestPriceReqWithPromise( this.props.endpoint, 
    [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], 
      ['end',this.state.end.format(moment.defaultFormatUtc)], ['pair', this.state.currency_left+'_'+this.state.currency_right]  ]
    ).then((prices) => { this.setState({btcdatapoints : prices}) });
  

  requestMA30 = () => RestUtils.performRestPriceReqWithPromise( movingEndpoint, 
    [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], 
      ['end',this.state.end.format(moment.defaultFormatUtc)], ['days', 20], ['pair', this.state.currency_left+'_'+this.state.currency_right]  ]
    ).then((prices) => { this.setState({ma30datapoints : prices})});

  handleSubmit = (event) => {
    RestUtils.performRestReqWithPromise( '/trade_bot', 
      [ ['asset', this.props.asset], ['start', this.state.start.format(moment.defaultFormatUtc)], 
        ['end',this.state.end.format(moment.defaultFormatUtc)], ['initial', this.state.initial], 
        ['fees', this.state.fees], ['strategy', this.state.strategy], ['pair', this.state.currency_left+'_'+this.state.currency_right] ]
    ).then((tradeBotResponse) => {
      this.setState({responseFields : Helper.buildResponseComponent(tradeBotResponse)});
      buildExecutionResult(tradeBotResponse, this.state.initial, this.state.currency_left, this.state.currency_right, this.state.start, this.state.end, 
        (result) =>  this.setState({executionResultFields: <ExecutionResultPanel result={result}/>})
      );
    }) ;
    event.preventDefault();
  }

  performTickerRequest = (exchange) => RestUtils.performRestPriceReq((quotes) => {
    let currencyFields = [];
    for(let q of quotes) {
      if (q.pair.right === this.props.asset)
        currencyFields.push(
          <tr>
            <td>{q.pair.right}</td>
            <td>{q.last+' '+q.pair.left}</td>
            <td>{q.percentChange}</td>
          </tr>
        );
    }
    this.setState({currencyFields : currencyFields});
  }, tickerEndpoint,[['exchange',exchange]]);
  
  constructor(props) {
    super(props);
    
    this.state = { 
      ma30datapoints : [], 
      btcdatapoints : [], 
      responseFields : [], 
      initial : '10000', 
      start : moment("2017-01-01"), 
      end : moment(), 
      fees: '0.1',
      strategy: 'safe',
      currencyFields : [],
      currency_left : 'USD',
      currency_right : this.props.asset,
      asset : this.props.asset,
      executionResultFields : null
    }

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentDidMount() {

    this.performTickerRequest('poloniex');
    setInterval(() => this.performTickerRequest('poloniex'),5000);
      
    this.requestBTC();
    this.requestMA30();

  }



  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  render() {
    return (
          <span>
          {
          !this.props.pairChoice &&             
          <Panel title='Real-time prices'>
            <PanelTable headers={['Currency','Price', 'Change']}>
            <tbody>
              {this.state.currencyFields}
            </tbody>
            </PanelTable>
          </Panel>
          }

          <Panel title={this.state.asset+' price history'}>
            <GraphResultBase datas={[ { name: this.props.asset, color: 'rgba(220,220,220,1)', datapoints: this.state.btcdatapoints},
              { name: 'MA30', color: 'rgba(220,0,0,1)', datapoints: this.state.ma30datapoints } ]}/>
            <FormContainer handleSubmit={this.handleSubmit} submit="Run trade-bot">  
              <FormTable>
              {
                this.props.pairChoice &&                
                <FormRow label='Pair'>
                  <FormOption name='left' values={[ ['USD','USD'], ['BTC','BTC'] ]} onChange={(event) => this.setState({currency_left: event.target.value})}/>    
                  <FormOption name='right' values={[ ['BTC','BTC'], ['ETH','ETH'], ['XMR','XMR'], ['XRP','XRP'], ['XLM','XLM'] ]} onChange={(event) => this.setState({currency_right: event.target.value})}/>    
                </FormRow>
              }          
              {
                this.props.stockChoice &&
                <FormRow label='Stock code'>
                <FormTextField value={this.state.currency_right} name="right" handleTextChange={(event) => this.setState({currency_right: event.target.value})} />
                </FormRow>
              }
                <FormRow label='Start'>
                  <DatePicker title='start' selected={this.state.start} onChange={(date) => this.setState({start: date})}/>
                </FormRow>
                <FormRow label='End'>
                  <DatePicker title='end' selected={this.state.end} onChange={(date) => this.setState({end: date})}/>
                </FormRow>
                <FormRow label={'Initial amount ('+this.state.currency_left+')'}>
                  <FormTextField value={this.state.initial} name="initial" handleTextChange={(event) => this.setState({initial: event.target.value})} />
                </FormRow>
                <FormRow label='Transaction fees'>
                  <FormTextField value={this.state.fees} name="fees" handleTextChange={(event) => this.setState({fees: event.target.value})} />
                </FormRow>
                <FormRow label='Strategy'>
                  <FormOption name='strategy' values={[ ['safe','safe and defensive'], ['custom','custom undocumented'] ]} onChange={(event) => this.setState({strategy: event.target.value})}/>    
                </FormRow>
                <FormRow>
                  <FormButton text='Update' handleClick={ (event) => { this.componentDidMount(); this.setState({asset: this.state.currency_right})} }/>
                </FormRow>
              </FormTable>    
            </FormContainer>
          </Panel>
          {
            this.state.executionResultFields &&
            <Panel title='Trade-bot execution results'>
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

class InHouseInfo extends React.Component {

  rowsFromObjets(jsArr) {
    var fields = [];  
    jsArr.forEach(function(jsObj) {
      if (jsObj.quantity > 0) {
        fields.push(<tr><td>{jsObj.asset}</td><td>{jsObj.quantity}</td></tr>);
      }
    })
    return fields;
  }

  rowsFromArray(jsArr) {
    var fields = [];  
    jsArr.forEach(function(jsObj) {
      fields.push(<tr><td>{jsObj[0]}</td><td>{jsObj[1]}</td></tr>);
      
    })
    return fields;
  }

  rowsFromTradeList(jsArr) {
    var fields = [];  
    jsArr.forEach(function(jsObj) {
      fields.push(<tr><td>{jsObj.date}</td><td>{jsObj.type}</td><td>{jsObj.asset}</td><td>{jsObj.price}</td></tr>);
      
    })
    return fields;
  }
  

  requestCurrentlyTrading = () => RestUtils.performRestPriceReq((currencyWeightList)=> 
    { this.setState({currentlyTradingFields: this.rowsFromArray(currencyWeightList)})},
    '/inhouse/currently_trading',[['exchange', this.props.exchange]]
  )

  requestBalance = () => RestUtils.performRestPriceReq((balanceList)=> 
    { 
      this.requestTradeHistory()
      this.setState({balancesFields: this.rowsFromObjets(balanceList.assetList), balance_valuation: balanceList.valuation})},
    '/inhouse/balances',[['exchange', this.props.exchange]]
  )

  requestTradeHistory = () => RestUtils.performRestPriceReq((tradeList)=> 
    { this.setState({tradeHistoryFields: this.rowsFromTradeList(tradeList)})},
    '/inhouse/trade_history',[['exchange', this.props.exchange]]
  )

  constructor(props) {
    super(props);

    this.state = { 
      currentlyTradingFields : [],
      balancesFields : [],
      tradeHistoryFields : [],
      balance_valuation : 0
    }

  }

  componentDidMount() {
    this.requestBalance();
    this.requestCurrentlyTrading();
  }

  render() {
    return (
      <span>
      <Panel title='Currently Trading'>
        <ResponseTable first='Currency' second='Weight' responseFields={this.state.currentlyTradingFields}/>
      </Panel>

      <Panel title={'Balances ['+(this.state.balance_valuation).toFixed(2)+' USD]'}>
        <ResponseTable first='Currency' second='Quantity' responseFields={this.state.balancesFields}/>
      </Panel>

      <Panel title='Trade History'>
      <table className="table table-bordered table-hover">
       <tbody>
       <tr><th>Date</th><th>Type</th><th>Currency</th><th>Price</th></tr>
       {
         this.state.tradeHistoryFields.map(field => field)
       }
       </tbody>
       </table>
      </Panel>      
      </span>
    );
  }
}



ReactDOM.render(
    <BrowserRouter>
      <Switch>
        <Route path='/btc_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='BTC backtest' asset='BTC'/>)} />
        <Route path='/eth_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='ETH backtest' asset='ETH'/>)} />
        <Route path='/xmr_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='XMR backtest' asset='XMR'/>)} />
        <Route path='/crypto_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='Crypto backtest' asset='BTC' pairChoice/>)} />
        <Route path='/stock_price' render={() => ( <GraphResult endpoint={priceEndpoint} title='Stock backtest' asset='GOOG' stockChoice/>)} />
        <Route path='/trading' render={() => ( <TradingForm/>)} />
        <Route path='/inhouse_info_poloniex' render={() => ( <InHouseInfo exchange='poloniex'/>)} />
        <Route path='/inhouse_info_kraken' render={() => ( <InHouseInfo exchange='kraken'/>)} />
        <Route path='/' render={() => ( <TradingGlobal />)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);