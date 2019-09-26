import { cpus } from "os";

deleteTradingJob = (id) => { return ((event) =>{
    RestUtils.performRestPostReqWithCreds(() => this.getTradingJobs(), '/trading_job/trade_job_delete',
    [ [ 'id', id]], (status) => {});
})  }

fetchTradingJobs = () => {
    RestUtils.performRestPostReqWithCreds((tradingJobs) => this.setState({tradingJobs:tradingJobs}), '/trading_job/trade_job_get_all',[], (status) => {});
}

fetchApiKeys = () => {
    RestUtils.performRestPostReqWithCreds((apiKeys) => {
        if (apiKeys.length > 0) this.setState({new_trading_apiKeyId : apiKeys[0].id});
        this.setState({apiKeys:apiKeys});
        this.getTradingJobs();
    }, '/trading_job/api_key_get_all',[], (status) => {});
}

buildScheduledTradings = (tradingJobs, apiKeys) => {
    var fields = [];
    tradingJobs.forEach(
        (item) => {
        item.handleClick = this.deleteTradingJob(item.id);
        var apiKey = apiKeys.filter((apiKey) => apiKey.id === item.apiKeyId)[0];
        item.exchange = apiKey.exchange;
        item.key = apiKey.key;
        fields.push(TradingJobsField(item));
        }
    )
    return fields;
}  

addTradingJob = (event) => {
    RestUtils.performRestPostReqWithCreds(() => this.getApiKeys(), '/trading_job/trade_job_add',
    [ [ 'cron', this.state.new_trading_cron ], ['apiKeyId', parseInt(this.state.new_trading_apiKeyId)], [ 'strategy', this.state.new_trading_strategy],
        [ 'userId',  0], [ 'id', 0], ['weights', this.state.tradeWeight], ['baseAsset', this.state.new_trading_base_asset] ], (status) => {});
    event.preventDefault();
}

fetchBalance = (exchange, apiKey, apiSecret) => RestUtils.performRestPostReq(
        (balanceList)=> { this.setState({balanceFields: Helper.rowsFromObjet(balanceList)})},
        balanceEndpoint,
        [ ['exchange', exchange], ['apikey', apiKey],['apisecret',apiSecret]]
)
    
fetchOpenOrders = () => RestUtils.performRestPostReq(
        (balanceList)=> { this.setState({balanceFields: Helper.rowsFromObjet(balanceList)})},
        openOrdersEndpoint,
        [['apikey', this.state.apikey],['apisecret',this.state.apisecret]]
)  
    
getApiKeyOptions = (apiKeys) => {
        return apiKeys.map( (item) => [ item.id, item.key] );
}
    
addWeight = () => {
        var weights = this.state.tradeWeight;
        weights[this.state.new_asset_weight] = this.state.new_weight;
        this.setState({tradeWeight: weights})
}
    
buildTradeWeightComp = (tradeWeights) => {
        var weightList = [];
        Object.keys(tradeWeights).forEach((key) =>{ 
          weightList.push(<FormRow label={key}>{tradeWeights[key]}</FormRow>)
        })
        return weightList;
}

const handleTradeWeightChange = (tradeWeights) => ({
    type : TradingFormAction.SET_TRADE_WEIGHT,
    tradeWeights : tradeWeights
})

const handleAddTradeWeightChange = (tradeWeight) => ({
    type : TradingFormAction.ADD_TRADE_WEIGHT,
    tradeWeight : tradeWeight
})

const handleAddTradingJob = (tradingJob) => ({
    type : TradingFormAction.ADD_TRADING_JOB,
    tradingJob : tradingJob
})

const handleSetTradingJob = (tradingJob) => ({
    type: TradingFormAction.SET_TRADING_JOB,
    tradingJob: tradingJob
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


const TradingForm = (scheduledTradings, tradingJob, apiKeys, baseAsset, tradeWeights, formTradeWeightChange, formAddTradeWeightChange, 
    formAddTradingJob, formSetTradingJob, formSetUseCustom, formSetUseCustomStrategy) => (
        <span>
        <Panel title='Scheduled Trading'>
          <ResponseTable first='Exchange' second='Key' responseFields={ getScheduledTradings(scheduledTradings, apiKeys) }/>
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
          <FormContainer handleSubmit={formAddTradingJob(tradingJob)} submit="Add">
            <FormTable>
                <FormRow label='Asset to trade'>
                  <FormOption name='asset' values={ allAssets() } onChange={ formSetAsset }/>
                  <FormTextField value={this.state.new_weight} name='weight' handleTextChange={ formSetWeight } />  
                  <FormButton text='Add Weight' handleClick={ formAddTradeWeightChange }/>
                </FormRow>  
                <FormRow label='Set default Asset/Weight'>
                  <FormButton text='Kraken Default' handleClick={ (event) => { this.setState({tradeWeight: defaultKrakenWeight()}) } }/>
                  <FormButton text='Poloniex Default' handleClick={ (event) => { this.setState({tradeWeight: defaultPoloniexWeight()}) } }/>
                </FormRow>
                <FormRow label='Base Asset'>
                  <FormOption values={ baseAssets() } name='baseAsset' onChange={(event) => { formSetBaseAsset(event.target.value) }} value={baseAsset}/> 
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
                      formSetUseCustomStrategy(defaultStrategy())
                    }
                  }}/>    
                </FormRow>
                { this.state.use_custom && 
                  <FormRow label='Custom Strategy'>
                  <textarea name="custom_strategy" onChange={(event) => {
                    try {
                        formSetUseCustomStrategy(JSON.parse(event.target.value))
                    } catch(error) {
                    }
                  }} value={JSON.stringify(this.state.new_trading_strategy, null, 2)}>
                  </textarea>
                  </FormRow>
                }
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


const mapStateToProps = state => {
    return {
        scheduledTradings: state.getTradingForm.scheduledTradings,
        tradingJob: state.getTradingForm.tradingJob,
        apiKeys: state.getTradingForm.apiKeys,
        tradeWeights: state.getTradingForm.getTradeWeights        
    }
}

const mapDispatchToProps = dispatch => {
    return {
        formTradeWeightChange: (event) => { dispatch(handleTradeWeightChange(event)) },
        formAddTradeWeightChange: (event) => { dispatch(handleAddTradeWeightChange(event)) },
        formAddTradingJob: () => { dispatch(handleAddTradingJob(state.getTradingForm.tradingJob)) },
        formSetTradingJob: () => { dispatch(handleSetTradingJob(state.getTradingForm.tradingJob)) },
        formDeleteTradingJob: (id) => { 
            deleteTradingJob(id)
        },
        formSetAsset: (event) => { dispatch(handleSetAsset(event.target.value)) },
        formSetWeight: (event) => { dispatch(handleSetWeight(event.target.value)) },
        formSetBaseAsset: (asset) => { dispatch(handleSetBaseAsset(asset)) },
        formSetApiKeyId: (event) => { dispatch(handleSetApiKeyId(event.target.value)) },
        formSetUseCustom: (use) => { dispatch(handleSetUseCustom(use)) },
        formSetUseCustomStrategy: (strategy) => { dispatch(handleSetCustomStrategy(strategy)) }
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(TradingForm)