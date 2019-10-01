//import { cpus } from "os";
import React from 'react'
import PropTypes from 'prop-types'
import {ResponseTable, Panel, FormContainer, FormTable, FormRow, FormTextField, FormOption, FormButton} from './generics'
import { TradingFormAction } from '../actions'
import { connect } from 'react-redux'
import { allAssets, baseAssets, defaultStrategy } from '../constants'
import RestUtils from '../restutils'


function defaultKrakenWeight() {
    return {
      BTC : 0.4,
      ETH : 0.2,
      XMR : 0.2,
      XRP : 0.2
    }
  }
  
  function defaultPoloniexWeight() {
    return {
      BTC : 0.3,
      ETH : 0.2,
      XMR : 0.1,
      XRP : 0.2,
      XLM : 0.1,
      DOGE : 0.1
    }
  }

const deleteTradingJob = (id, formFetchTradingJobs) => { return ((event) => {
      RestUtils.performRestPostReqCredsWithPromise('/trading_job/trade_job_delete',[ [ 'id', id]], (status) => {}).then(() => formFetchTradingJobs())
    })  
}

const fetchTradingJobs = () => {
    return RestUtils.performRestPostReqCredsWithPromise('/trading_job/trade_job_get_all',[], (status) => {});
}

const fetchApiKeys = () => {
    return RestUtils.performRestPostReqCredsWithPromise('/trading_job/api_key_get_all',[], (status) => {});
}

function TradingJobsField(props) {
  return <tr key={props.id}><td>{props.exchange}</td><td>{props.key}</td><td><FormButton text='Delete' handleClick={ (event) => props.handleClick(event) }/></td></tr>;
}

const buildScheduledTradings = (tradingJobs, apiKeys, formFetchTradingJobs) => {
    var fields = [];
    tradingJobs.forEach(
        (item) => {
          item.handleClick = deleteTradingJob(item.id, formFetchTradingJobs);
          var apiKey = apiKeys.filter((apiKey) => apiKey.id === item.apiKeyId)[0];
          item.exchange = apiKey.exchange;
          item.key = apiKey.key;
          fields.push(TradingJobsField(item));
        }
    )
    return fields;
}  

const addTradingJob = (event, newTradingCron, newApiKeyId, newTradingStrategy, tradeWeights, baseAsset) => {
    const promise = RestUtils.performRestPostReqCredsWithPromise( '/trading_job/trade_job_add',
    [ [ 'cron', newTradingCron ], ['apiKeyId', parseInt(newApiKeyId)], [ 'strategy', newTradingStrategy],
        [ 'userId',  0], [ 'id', 0], ['weights', tradeWeights], ['baseAsset', baseAsset] ], (status) => {});
    event.preventDefault();
    return promise;
}

const fetchBalance = (exchange, apiKey, apiSecret) => RestUtils.performRestReqWithPromise(
        balanceEndpoint,
        [ ['exchange', exchange], ['apikey', apiKey],['apisecret',apiSecret]]
)
    
const fetchOpenOrders = () => RestUtils.performRestPostReq(
        (balanceList)=> { this.setState({balanceFields: Helper.rowsFromObjet(balanceList)})},
        openOrdersEndpoint,
        [['apikey', this.state.apikey],['apisecret',this.state.apisecret]]
)  
    
const getApiKeyOptions = (apiKeys) => {
        return apiKeys.map( (item) => [ item.id, item.key] );
}
    
const buildTradeWeightComp = (tradeWeights) => {
        var weightList = [];
        Object.keys(tradeWeights).forEach((key) =>{ 
          weightList.push(<FormRow label={key} key={key}>{tradeWeights[key]}</FormRow>)
        })
        return weightList;
}

const handleTradeWeightChange = (tradeWeights) => ({
    type : TradingFormAction.SET_TRADE_WEIGHT,
    tradeWeights : tradeWeights
})

const handleAddTradeWeightChange = (tradeWeight) => ({
    type : TradingFormAction.ADD_TRADE_WEIGHT
})

const handleAddTradingJob = () => ({
    type : TradingFormAction.ADD_TRADING_JOB
})

const handleSetScheduledTradings = (tradingJobs) => ({
  type: TradingFormAction.SET_SCHEDULED_TRADINGS,
  scheduledTradings: tradingJobs
})

const handleSetAsset = (asset) => ({
    type: TradingFormAction.SET_ASSET,
    newAsset: asset
})

const handleSetWeight = (weight) => ({
    type: TradingFormAction.SET_WEIGHT,
    newWeight: weight
})

const handleSetBaseAsset = (asset) => ({
    type: TradingFormAction.SET_BASE_ASSET,
    baseAsset: asset
})

const handleSetApiKeyId = (apiKeyId) => ({
    type: TradingFormAction.SET_API_KEY_ID,
    apiKeyId: apiKeyId
})

const handleSetUseCustom = (use) => ({
    type: TradingFormAction.SET_USE_CUSTOM,
    useCustom: use
})

const handleSetCustomStrategy = (strategy) => ({
    type: TradingFormAction.SET_CUSTOM_STRATEGY,
    customStrategy: strategy
})

const handleSetBalanceFields = (balanceFields) => ({
  type: TradingFormAction.SET_BALANCE_FIELDS,
  balanceFields: balanceFields
})

const handleSetApiKeys = (apiKeys) => ({
  type: TradingFormAction.SET_API_KEYS,
  apiKeys: apiKeys
})


const TradingForm = ({scheduledTradings, apiKeys, baseAsset, tradeWeights, newAsset, newWeight, useCustom, apiKeyId, apiKey, apiSecret, newTradingStrategy, balanceFields, newTradingCron, formTradeWeightChange, formAddTradeWeightChange, 
    formAddTradingJob, formSetAsset, formSetWeight, formSetBaseAsset, formSetApiKeyId, formSetUseCustom, formSetUseCustomStrategy, formFetchTradingJobs, formFetchBalanceFields, formSetScheduledTrading}) => (
        <span>
        <Panel title='Scheduled Trading'>
          <ResponseTable first='Exchange' second='Key' responseFields={ buildScheduledTradings(scheduledTradings, apiKeys, formFetchTradingJobs) }/>
        </Panel>        
        <Panel title='New Scheduled Trading'>
          <table className="table table-bordered table-hover table-striped">
          <thead>
            <tr><th>Asset</th><th>Weight</th></tr>
          </thead>
          <tbody>
          { buildTradeWeightComp(tradeWeights) }
          </tbody>
          </table>
          <FormContainer handleSubmit={(event) => formAddTradingJob(event, newTradingCron, apiKeyId, newTradingStrategy, tradeWeights, baseAsset, formFetchTradingJobs)} submit="Add">
            <FormTable>
                <FormRow label='Asset to trade'>
                  <FormOption name='asset' values={ allAssets } onChange={ formSetAsset }/>
                  <FormTextField value={newWeight} name='weight' handleTextChange={ formSetWeight } />  
                  <FormButton text='Add Weight' handleClick={ formAddTradeWeightChange }/>
                </FormRow>  
                <FormRow label='Set default Asset/Weight'>
                  <FormButton text='Kraken Default' handleClick={ (event) => { formTradeWeightChange(defaultKrakenWeight()) } }/>
                  <FormButton text='Poloniex Default' handleClick={ (event) => { formTradeWeightChange(defaultPoloniexWeight()) } }/>
                </FormRow>
                <FormRow label='Base Asset'>
                  <FormOption values={ baseAssets } name='baseAsset' onChange={(event) => { formSetBaseAsset(event.target.value) }} value={baseAsset}/> 
                  <FormButton text='Kraken Default' handleClick={ (event) => { formSetBaseAsset('USD') } }/>
                  <FormButton text='Poloniex Default' handleClick={ (event) => { formSetBaseAsset('TETHER') } }/>                 
                </FormRow>
                <FormRow label='API Key'>
                  <FormOption name='apikeyId' values={ getApiKeyOptions(apiKeys) } onChange={ formSetApiKeyId }/> 
                </FormRow>      
                <FormRow label='Strategy'>
                  <FormOption name='strategy' values={[ ['safe','safe and defensive'], ['custom','custom'] ]} onChange={(event) => {
                    if (event.target.value === 'custom') { 
                      formSetUseCustom(true)
                    } else {
                      formSetUseCustom(false)
                      formSetUseCustomStrategy(defaultStrategy)
                    }
                  }}/>    
                </FormRow>
                { useCustom && 
                  <FormRow label='Custom Strategy'>
                  <textarea name="custom_strategy" onChange={(event) => {
                    try {
                        formSetUseCustomStrategy(JSON.parse(event.target.value))
                    } catch(error) {
                    }
                  }} value={JSON.stringify(newTradingStrategy, null, 2)}>
                  </textarea>
                  </FormRow>
                }
              </FormTable>          
          </FormContainer>
        </Panel>
        <Panel title='Trading operations'>
          <FormContainer handleSubmit={null} submit="Run trade-bot">  
            <FormTable>
              <FormRow label='API Key'>
                <FormTextField value={apiKey} name="apikey" handleTextChange={(event) => this.setState({apikey: event.target.value})} />
              </FormRow>
              <FormRow label='API Secret'>
                <FormTextField value={apiSecret} name="apisecret" handleTextChange={(event) => this.setState({apisecret: event.target.value})} />
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
        { balanceFields && 
          <Panel title='Request output'>
          <PanelTable>
          { balanceFields } 
          </PanelTable>
          </Panel>
        }
        </span>
      )

class TradingFormContainer extends React.Component {

  componentDidMount() {
    fetchApiKeys().then( apiKeys => {
      this.props.formSetApiKeys(apiKeys)
      this.props.formFetchTradingJobs()
    })
  }

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <TradingForm {...this.props}/>
    )
  }
}


TradingForm.propTypes = {
    scheduledTradings: PropTypes.array.isRequired
}

const mapStateToProps = state => {
    return {
        scheduledTradings: state.getTradingForm.scheduledTradings,
        apiKeys: state.getTradingForm.apiKeys,
        baseAsset: state.getTradingForm.baseAsset,
        tradeWeights: state.getTradingForm.tradeWeights,
        newAsset: state.getTradingForm.newAsset,
        newWeight: state.getTradingForm.newWeight,
        useCustom: state.getTradingForm.useCustom,
        apiKeyId: state.getTradingForm.apiKeyId,
        apiKey: state.getTradingForm.apiKey,
        apiSecret: state.getTradingForm.apiSecret,
        newTradingStrategy: state.getTradingForm.newTradingStrategy,
        balanceFields: state.getTradingForm.balanceFields,
        newTradingCron: state.getTradingForm.newTradingCron       
    }
}

const mapDispatchToProps = dispatch => {
    return {
        formTradeWeightChange: (event) => { dispatch(handleTradeWeightChange(event)) },
        formAddTradeWeightChange: (event) => { dispatch(handleAddTradeWeightChange(event)) },
        formAddTradingJob: (event, newTradingCron, newApiKeyId, newTradingStrategy, tradeWeights, baseAsset, formFetchTradingJobs) => { 
          addTradingJob(event, newTradingCron, newApiKeyId, newTradingStrategy, tradeWeights, baseAsset, formFetchTradingJobs).then(() => {
          dispatch(handleAddTradingJob())
          formFetchTradingJobs()
        })   },
        formSetAsset: (event) => { dispatch(handleSetAsset(event.target.value)) },
        formSetWeight: (event) => { dispatch(handleSetWeight(event.target.value)) },
        formSetBaseAsset: (asset) => { dispatch(handleSetBaseAsset(asset)) },
        formSetApiKeyId: (event) => { dispatch(handleSetApiKeyId(event.target.value)) },
        formSetUseCustom: (use) => { dispatch(handleSetUseCustom(use)) },
        formSetUseCustomStrategy: (strategy) => { dispatch(handleSetCustomStrategy(strategy)) },
        formFetchTradingJobs: () => fetchTradingJobs().then( (tradingJobs) => { dispatch(handleSetScheduledTradings(tradingJobs)) }),
        formFetchBalanceFields: (exchange, apiKey, apiSecret) => { fetchBalance().then(balanceList => dispatch(handleSetBalanceFields(Helper.rowsFromObjet(balanceList)))) },
        formSetScheduledTrading: (scheduledTradings) => { dispatch(handleSetScheduledTradings(scheduledTradings)) },
        formSetApiKeys: (apiKeys) => { 
          
          dispatch(handleSetApiKeys(apiKeys)) }
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(TradingFormContainer)