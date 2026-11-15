import React from 'react';
import RestUtils from '../restutils';
import { Panel } from './generics';

// Note: Hooks require React >= 16.8. This project uses React 16.5,
// so implement PolymarketTab as a class component instead of using hooks.
class PolymarketTab extends React.Component {
  constructor(props) {
    super(props);
    this.state = { markets: [] };
  }

  async componentDidMount() {
    try {
      const response = await RestUtils.performRestPriceReqWithPromise('/markets');
      const markets = Array.isArray(response)
        ? response
        : (response && Array.isArray(response.markets) ? response.markets : []);
      this.setState({ markets });
    } catch (error) {
      console.error('Error fetching Polymarket markets:', error);
    }
  }

  render() {
    return (
      <Panel title='Polymarket Markets'>
        <ul>
          {this.state.markets.map((market, index) => (
            <li key={index}>
              {market.id} - {market.question} - {market.archived ? 'Archived' : 'Active'}
            </li>
          ))}
        </ul>
      </Panel>
    );
  }
}

export default PolymarketTab;