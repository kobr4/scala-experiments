'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { ResponseTable, FormRow, FormTable, FormTextField, FormPasswordField, FormButton, FormContainer, FormOption, Panel, PanelTable, FormInputField} from './components/generics'

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

class CommonUtils {
  static isUser() {
    return document.cookie.indexOf('authtoken=') >= 0;
  }

  static deleteCookie(cname) {
    var d = new Date(); //Create an date object
    d.setTime(d.getTime() - (1000*60*60*24)); //Set the time to the past. 1000 milliseonds = 1 second
    var expires = "expires=" + d.toGMTString(); //Compose the expirartion date
    window.document.cookie = cname+"="+"; "+expires;//Set the cookie with name and the expiration date
  }
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
    }, '/trade_bot/run', [['asset', asset], ['start', moment("2017-01-01").format(moment.defaultFormatUtc)], ['end',moment().format(moment.defaultFormatUtc)], ['initial', 10000], ['fees', 0.1], ['strategy', 'safe']]) ;
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

function TradingJobsField(props) {
  return <tr key={props.id}><td>{props.exchange}</td><td>{props.key}</td><td><FormButton text='Delete' handleClick={ (event) => props.handleClick(event) }/></td></tr>;
}

class TradingForm extends React.Component {

  deleteTradingJob = (id) => { return ((event) =>{
    RestUtils.performRestPostReqWithCreds(() => this.getTradingJobs(), '/trading_job/trade_job_delete',
    [ [ 'cron', '' ], ['apiKeyId', 0], ['strategy', ''], [ 'exchange', ''], [ 'userId',  0], [ 'id', id]], (status) => {});
  })  }

  getTradingJobs = () => {
    RestUtils.performRestPostReqWithCreds((tradingJobs) => this.setState({tradingJobs:tradingJobs}), '/trading_job/trade_job_get_all',[], (status) => {});
  }

  getApiKeys = () => {
    RestUtils.performRestPostReqWithCreds((apiKeys) => {
      if (apiKeys.length > 0) this.setState({new_trading_apiKeyId : apiKeys[0].id});
      this.setState({apiKeys:apiKeys});
      this.getTradingJobs();
    }, '/trading_job/api_key_get_all',[], (status) => {});
  }

  getComp = () => {
    var fields = [];
    this.state.tradingJobs.forEach(
      (item) => {
        item.handleClick = this.deleteTradingJob(item.id);
        var apiKey = this.state.apiKeys.filter((apiKey) => apiKey.id === item.apiKeyId)[0];
        item.exchange = apiKey.exchange;
        item.key = apiKey.key;
        fields.push(TradingJobsField(item));
      }
    )
    return fields;
  }  

  addTradingJob = (event) => {
    RestUtils.performRestPostReqWithCreds(() => this.getApiKeys(), '/trading_job/trade_job_add',
    [ [ 'cron', this.state.new_trading_cron ], ['apiKeyId', this.state.new_trading_apiKeyId], [ 'strategy', this.state.new_trading_strategy],
      [ 'userId',  0], [ 'id', 0] ], (status) => {});
    event.preventDefault();
  }


  componentDidMount() {
   
    this.getApiKeys();
  }

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

  getApiKeyOptions = () => {
    return this.state.apiKeys.map( (item) => [ item.id, item.key] );
  }

  addWeight = () => {
    var weights = this.state.tradeWeight;
    weights[this.state.new_asset_weight] = this.state.new_weight;
    this.setState({tradeWeight: weights})
  }

  getTradeWeightComp = () => {
    var weightList = [];
    this.state.tradeWeight.keys.forEach((key) =>{ 
      weightList.push(<FormRow label={key}>{this.state.tradeWeight[key]}</FormRow>)
    })
    return weightList;
  }

  constructor(props) {
    super(props);

    this.state = {
      apikey :'', apisecret : '', balanceFields : null, tradingJobs : [], apiKeys: [], 
      new_trading_apiKeyId : 0, new_trading_cron : '', new_trading_strategy : '', 
      new_asset_weight : 'BTC', new_weight: 1.0, tradeWeight : {}} 
  }

  render() {
    return (
      <span>
      <Panel title='Schedule trading'>
      <ResponseTable first='Exchange' second='Key' responseFields={this.getComp() }/>
        <FormContainer handleSubmit={this.addTradingJob} submit="Add">
          <FormTable>
              <FormRow label='API Key'>
                <FormOption name='apikeyId' values={ this.getApiKeyOptions() } onChange={(event) => this.setState({new_trading_apiKeyId: event.target.value})}/> 
              </FormRow>
              <FormTable>
                { this.getTradeWeightComp() }
              </FormTable>
              <FormRow>
                <FormOption name='asset' values={[ ['BTC','BTC'], ['ETH', 'ETH'] ]} onChange={(event) => this.setState({new_asset_weight: event.target.value}) }/>
                <FormTextField value={this.state.new_weight} name='weight' handleTextChange={(event) => this.setState({new_weight: event.target.value})} />  
                <FormButton text='Add' handleClick={ (event) => { this.addWeight() } }/>
              </FormRow>                           
            </FormTable>          
        </FormContainer>
      </Panel>
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

class SignOut extends React.Component {
  signout = () => {
    CommonUtils.deleteCookie('authtoken');
    window.location.href = '/';
  }

  componentDidMount() {
    this.signout()
  }

  render() {
    return (
      <Panel title='Signing Out'>
      </Panel>
    )
  }
}

class LoginForm extends React.Component {

  handleSubmit = (event) => {
    RestUtils.performRestPostReq((token) => {
      document.cookie = 'authtoken='+token+';path=/'
      window.location.href = '/';
    }, '/auth/login',[ ['email', this.state.email], ['password', this.state.password] ],
    (status) => this.setState({error: true})
  )
    event.preventDefault();  
  }

  constructor(props) {
    super(props);

    this.state = { 
      email: '',
      password: '',
      error: false
    }
  }

  render() {
    return (
      <span>
      <Panel title='Sign In'>
      { this.state.error &&
      <div className="alert alert-danger">Failed to log-in. Check your parameters or contact support</div>
      }      
      <FormContainer handleSubmit={this.handleSubmit} submit="Login">
      <FormTable>
      <FormRow label="Email"><FormTextField value={this.state.email} name="email" handleTextChange={(event) => this.setState({email: event.target.value})} /></FormRow>
      <FormRow label="Password"><FormPasswordField value={this.state.password} name="password" handleTextChange={(event) => this.setState({password: event.target.value})} /></FormRow>
      </FormTable>
      </FormContainer>
      </Panel>
      </span>
    )
  }  
}

class SignUpForm extends React.Component {

  handleSubmit = (event) => {
    RestUtils.performRestPostReq((token) => {
      this.setState({activation: true})
    }, '/user/signup',[ ['email', this.state.email], ['password', this.state.password] ] )
    event.preventDefault();  
  }

  constructor(props) {
    super(props);

    this.state = { 
      email: '',
      password: '',
      activation: false
    }
  }

  render() {
    return (
      
      <span>
      { !this.state.activation && 
      <Panel title='Sign up form'>
      <FormContainer handleSubmit={this.handleSubmit} submit="Sign up">
      <FormTable>
      <FormRow label="Email"><FormTextField value={this.state.email} name="email" handleTextChange={(event) => this.setState({email: event.target.value})} /></FormRow>
      <FormRow label="Password"><FormPasswordField value={this.state.password} name="password" handleTextChange={(event) => this.setState({password: event.target.value})} /></FormRow>
      </FormTable>
      </FormContainer>
      </Panel>
      } 
      { this.state.activation &&
        <Panel title='Sign up form submitted'>
        An email has been sent to {this.state.email}, follow the included link to activate your account. 
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
    RestUtils.performRestReqWithPromise( '/trade_bot/run', 
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

  handleLearn = (event) => {
    RestUtils.performRestReqWithPromise( '/trade_bot/search', 
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
                <FormRow>
                  <FormButton text='Learn' handleClick={ (event) => { this.handleLearn(event) } }/>
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

class ActivationResult extends React.Component {

  performActivation = () => {
    RestUtils.performGetReqStatus((status) => { 
      if (status === 200) 
        this.setState({status:true})
      else 
        this.setState({status:false})
    },
    '/user/activation',
    [['token', (new URLSearchParams(window.location.search)).get('token') ]])
  }

  componentDidMount() {
    this.performActivation();
  }

  constructor(props) {
    super(props);
    this.state = {status: undefined}
  }

  render() {
    return (
      <span>
      <Panel title='Activation'>
        { this.state.status === true &&
          <p>Activation successful, you can now sign in to the website</p>
        } 
        { this.state.status === false &&
          <p>Activation failed, please verify your paramater or contact support</p>
        }
      </Panel>
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

function ApiKeysField(props) {
  return <tr key={props.id}><td>{props.exchange}</td><td>{props.key}</td><td><FormButton text='Delete' handleClick={ (event) => props.handleClick(event) }/></td></tr>;
}

class ApiKeysPanel extends React.Component {

  deleteApiKey = (id) => { return ((event) =>{
      RestUtils.performRestPostReqWithCreds(() => this.getApiKeys(), '/trading_job/api_key_delete',
      [ [ 'key', '' ], ['secret', ''], 
        [ 'exchange', ''],
        [ 'userId',  0],
        [ 'id', id]
      ], (status) => {});
    })  
  }  

  getComp = () => {
    var fields = [];
    this.state.apiKeys.forEach(
      (item) => {
        item.handleClick = this.deleteApiKey(item.id);
        fields.push(ApiKeysField(item))
      }
    )
    return fields;
  }

  getApiKeys = () => {
    RestUtils.performRestPostReqWithCreds((apiKeys) => this.setState({apiKeys:apiKeys}), '/trading_job/api_key_get_all',[], (status) => {});
  }

  addApiKey = (event) => {
    RestUtils.performRestPostReqWithCreds(() => this.getApiKeys(), '/trading_job/api_key_add',
    [ [ 'key', this.state.new_api_key ], ['secret', this.state.new_api_secret], 
      [ 'exchange', this.state.new_api_exchange],
      [ 'userId',  0],
      [ 'id', 0]
    ], (status) => {});
    event.preventDefault();
  }

  componentDidMount() {
    this.getApiKeys();
  }

  constructor(props) {
    super(props);

    this.state = { 
      apiKeys : [],
      new_api_exchange : 'POLONIEX',
      new_api_key : '',
      new_api_secret : ''
    }
  }

  render() {
    return (
      <span>
      <Panel title='API Keys'>
      <ResponseTable first='Exchange' second='Key' responseFields={this.getComp() }/>
      <FormContainer handleSubmit={this.addApiKey} submit="Add">  
      <FormTable>
        <FormRow label='Key'>
          <FormTextField value={this.state.new_api_key} name="key" handleTextChange={(event) => this.setState({new_api_key: event.target.value})} />
        </FormRow>
        <FormRow label='Secret'>
          <FormTextField value={this.state.new_api_secret} name="secret" handleTextChange={(event) => this.setState({new_api_secret: event.target.value})} />
        </FormRow>
        <FormRow label='Exchange'>
          <FormOption name='exchange' values={[ ['POLONIEX','Poloniex'], ['KRAKEN','Kraken'] ]} onChange={(event) => this.setState({new_api_exchange: event.target.value})}/>
        </FormRow>
      </FormTable>
      </FormContainer>      
      </Panel>
      </span>
    )
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
        <Route path='/api_keys' render={() => ( <ApiKeysPanel/>)} />
        <Route path='/inhouse_info_poloniex' render={() => ( <InHouseInfo exchange='poloniex'/>)} />
        <Route path='/inhouse_info_kraken' render={() => ( <InHouseInfo exchange='kraken'/>)} />
        <Route path='/login' render={() => <LoginForm/>}/>
        <Route path='/logout' render={() => <SignOut/>}/>
        <Route path='/signup' render={() => <SignUpForm/>}/>
        <Route path='/activation' render= {() => <ActivationResult/>}/>
        <Route path='/' render={() => ( <TradingGlobal />)} />
      </Switch>
    </BrowserRouter>,
  document.getElementById('toto')
);

ReactDOM.render(
  <ul id="side-menu" className="nav in">
  <li><a href="btc_price">BTC backtest</a></li>
  <li><a href="eth_price">ETH backtest</a></li>
  <li><a href="xmr_price">XMR backtest</a></li>
  <li><a href="crypto_price">Crypto backtest</a></li>
  <li><a href="stock_price">Stock backtest</a></li>
  { CommonUtils.isUser() &&
    <li><a href="trading">Trading</a></li>

  }
  { CommonUtils.isUser() &&
    <li><a href="api_keys">API Keys</a></li>
  }

  <li><a href="inhouse_info_poloniex">In-House @ Poloniex</a></li>
  <li><a href="inhouse_info_kraken">In-House @ Kraken</a></li>
  </ul>,
  document.getElementById('side')
)

ReactDOM.render(
  <span>
  { !CommonUtils.isUser() &&
    <span><a href="/login">Sign In</a> or <a href="/signup">Create an account</a></span>
  }
  { CommonUtils.isUser() &&
    <span><a href="/logout">Sign Out</a></span>
  }
  </span>
  ,
  document.getElementById('top-right')
)