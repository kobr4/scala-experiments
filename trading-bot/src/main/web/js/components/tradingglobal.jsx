import React from 'react'
import PropTypes from 'prop-types'
import { TradingGlobalAction } from '../actions'
import { connect } from 'react-redux'
import {Panel} from './generics'
import RestUtils from '../restutils'
import moment from 'moment';

const formatOrder = (order) => {
  return order.type+' at '+order.price+' on '+moment(order.date).format('LL');
}

const TradingGlobal = ({BTC, ETH, XMR}) => 
(
  <Panel title='Introduction'>
  <p>
    Simple and honest trading algorithm with a provable performance record.<br/>
    Positions are evaluated on daily basis.
  </p>
  Last take on BTC : { BTC && formatOrder(BTC) }<br/>
  Last take on ETH : { ETH && formatOrder(ETH) }<br/>
  Last take on XMR : { XMR && formatOrder(XMR) }<br/>
  </Panel>
)

TradingGlobal.propTypes = {
  BTC: PropTypes.object,
  ETH: PropTypes.object,
  XMR: PropTypes.object
}

class TradingGlobalContainer extends React.Component {

  handleTrade = (asset) => {
    RestUtils.performRestReq((tradeBotResponse) => {
      this.props.updateTrade(asset, tradeBotResponse.slice(-1)[0] )
    }, '/trade_bot/run', [['asset', asset], ['start', moment("2017-01-01").format(moment.defaultFormatUtc)], ['end',moment().format(moment.defaultFormatUtc)], ['initial', 10000], ['fees', 0.1], ['strategy', 'safe']]) ;
  }

  componentDidMount() {
    this.handleTrade('BTC');
    this.handleTrade('ETH');
    this.handleTrade('XMR');
  }

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <TradingGlobal {...this.props}/>
    )
  }
}

const mapStateToProps = state => {
  return {
      BTC: state.getTradingGlobal.BTC,
      ETH: state.getTradingGlobal.ETH,
      XMR: state.getTradingGlobal.XMR
  }
};

const handleAssetTradeChange = (assetName, assetValue) => ({
  type : TradingGlobalAction.END_FETCH_TRADE,
  name : assetName,
  value : assetValue
})

const mapDispatchToProps = dispatch => {
  return {
      updateTrade: (asset, value) => { dispatch(handleAssetTradeChange(asset, value)) }
     }
};


export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TradingGlobalContainer)