import React from 'react';
import RestUtils from '../restutils';
import { Panel, PanelTable } from './generics';

// Note: Hooks require React >= 16.8. This project uses React 16.5,
// so implement PolymarketTab as a class component instead of using hooks.
class PolymarketTab extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      view: 'markets', // 'markets' | 'tags' | 'tagMarkets'
      markets: [],
      tags: [],
      loading: false,
      error: null,
      selectedTag: null,
      tagMarkets: [],
      tagMarketsAll: [],
      // pagination state
      marketsLimit: 50,
      marketsOffset: 0,
      tagsLimit: 50,
      tagsOffset: 0,
      eventsLimit: 50,
      eventsOffset: 0,
      // simple hasNext hints (derived after fetch)
      marketsHasNext: false,
      tagsHasNext: false,
      eventsHasNext: false
      ,
      // default filter: show active only
      showActive: true,
      showClosed: false
    };
  }

  async componentDidMount() {
    await this.loadMarkets();
  }

  loadMarkets = async () => {
    try {
      this.setState({ loading: true, error: null, view: 'markets' });
      const { marketsLimit, marketsOffset, showActive, showClosed } = this.state;
      const params = [['limit', marketsLimit], ['offset', marketsOffset]];
      // Apply filter: if exactly one is checked, add the corresponding backend filter; otherwise no filter (show all)
      if (showActive && !showClosed) params.push(['active', true]);
      if (showClosed && !showActive) params.push(['closed', true]);
      const response = await RestUtils.performRestPriceReqWithPromise('/markets', params);
      const markets = Array.isArray(response)
        ? response
        : (response && Array.isArray(response.markets) ? response.markets : []);
      this.setState({ markets, loading: false, marketsHasNext: markets.length === marketsLimit });
    } catch (error) {
      console.error('Error fetching Polymarket markets:', error);
      this.setState({ error: 'Failed to load markets', loading: false });
    }
  }

  loadTags = async () => {
    try {
      this.setState({ loading: true, error: null, view: 'tags' });
      const { tagsLimit, tagsOffset } = this.state;
      const response = await RestUtils.performRestPriceReqWithPromise('/tags', [['limit', tagsLimit], ['offset', tagsOffset]]);
      const tags = Array.isArray(response) ? response : [];
      this.setState({ tags, loading: false, tagsHasNext: tags.length === tagsLimit });
    } catch (error) {
      console.error('Error fetching Polymarket tags:', error);
      this.setState({ error: 'Failed to load tags', loading: false });
    }
  }

  loadMarketsForTag = async (tag) => {
    try {
      this.setState({ loading: true, error: null, selectedTag: tag, view: 'tagMarkets' });
      const { eventsLimit, eventsOffset } = this.state;
      const response = await RestUtils.performRestPriceReqWithPromise('/events', [['tag_id', tag.id], ['limit', eventsLimit], ['offset', eventsOffset]]);
      const events = Array.isArray(response) ? response : [];
      // Flatten markets from events and annotate with event title
      const tagMarkets = [];
      for (let ev of events) {
        const markets = Array.isArray(ev.markets) ? ev.markets : [];
        for (let m of markets) {
          tagMarkets.push({ ...m, _eventTitle: ev.title || '' });
        }
      }
      // Apply checkbox filters now and keep the unfiltered cache
      const { showActive, showClosed } = this.state;
      const filtered = (showActive === showClosed)
        ? tagMarkets
        : tagMarkets.filter(m => (showActive && m.active) || (showClosed && m.closed));
      this.setState({ tagMarketsAll: tagMarkets, loading: false, eventsHasNext: events.length === eventsLimit }, this.applyTagFilters);
    } catch (error) {
      console.error('Error fetching markets for tag:', error);
      this.setState({ error: 'Failed to load markets for tag', loading: false });
    }
  }

  applyTagFilters = () => {
    const all = this.state.tagMarketsAll || [];
    const { showActive, showClosed } = this.state;
    const filtered = (showActive === showClosed)
      ? all
      : all.filter(m => (showActive && m.active) || (showClosed && m.closed));
    this.setState({ tagMarkets: filtered });
  }

  // Pagination handlers
  nextMarkets = () => this.setState(
    s => ({ marketsOffset: s.marketsOffset + s.marketsLimit }),
    () => this.loadMarkets()
  )
  prevMarkets = () => this.setState(
    s => ({ marketsOffset: Math.max(0, s.marketsOffset - s.marketsLimit) }),
    () => this.loadMarkets()
  )
  nextTags = () => this.setState(
    s => ({ tagsOffset: s.tagsOffset + s.tagsLimit }),
    () => this.loadTags()
  )
  prevTags = () => this.setState(
    s => ({ tagsOffset: Math.max(0, s.tagsOffset - s.tagsLimit) }),
    () => this.loadTags()
  )
  nextEvents = () => this.setState(
    s => ({ eventsOffset: s.eventsOffset + s.eventsLimit }),
    () => this.loadMarketsForTag(this.state.selectedTag)
  )
  prevEvents = () => this.setState(
    s => ({ eventsOffset: Math.max(0, s.eventsOffset - s.eventsLimit) }),
    () => this.loadMarketsForTag(this.state.selectedTag)
  )

  renderTabsHeader() {
    const { view } = this.state;
    return (
      <div style={{ marginBottom: '1rem' }}>
        <button className={'btn btn-sm ' + (view === 'markets' ? 'btn-primary' : 'btn-outline-primary')} onClick={this.loadMarkets} style={{ marginRight: '0.5rem' }}>Markets</button>
        <button className={'btn btn-sm ' + (view === 'tags' ? 'btn-primary' : 'btn-outline-primary')} onClick={this.loadTags}>Tags</button>
        <span style={{ marginLeft: '1rem' }}>
          <label style={{ marginRight: '0.75rem' }}>
            <input
              type="checkbox"
              checked={!!this.state.showActive}
              disabled={view === 'tags'}
              onChange={() => this.setState(
                s => ({ showActive: !s.showActive, // reset pagination when filter changes
                        marketsOffset: 0, eventsOffset: 0 }),
                () => {
                  if (this.state.view === 'markets') this.loadMarkets();
                  if (this.state.view === 'tagMarkets') this.applyTagFilters();
                }
              )}
            />{' '}Show active
          </label>
          <label>
            <input
              type="checkbox"
              checked={!!this.state.showClosed}
              disabled={view === 'tags'}
              onChange={() => this.setState(
                s => ({ showClosed: !s.showClosed, marketsOffset: 0, eventsOffset: 0 }),
                () => {
                  if (this.state.view === 'markets') this.loadMarkets();
                  if (this.state.view === 'tagMarkets') this.applyTagFilters();
                }
              )}
            />{' '}Show closed
          </label>
        </span>
      </div>
    );
  }

  render() {
    const { view, loading, error, markets, tags, selectedTag, tagMarkets } = this.state;
    return (
      <Panel title={view === 'tags' ? 'Polymarket Tags' : (view === 'tagMarkets' ? `Tag: ${selectedTag && selectedTag.label}` : 'Polymarket Markets')}>
        {this.renderTabsHeader()}
        {error && <div className="alert alert-danger">{error}</div>}
        {loading && <div>Loading...</div>}

        {!loading && view === 'markets' && (
          <PanelTable headers={['ID', 'Question', 'Status']}>
            <tbody>
              {markets.map((market, index) => (
                <tr key={index}>
                  <td>{market.id}</td>
                  <td>{market.question}</td>
                  <td>{market.archived ? 'Archived' : (market.closed ? 'Closed' : (market.active ? 'Active' : ''))}</td>
                </tr>
              ))}
            </tbody>
          </PanelTable>
        )}
        {!loading && view === 'markets' && (
          <div className='d-flex justify-content-between align-items-center mt-2'>
            <button className='btn btn-sm btn-secondary' disabled={this.state.marketsOffset === 0} onClick={this.prevMarkets}>Prev</button>
            <span>Page {Math.floor(this.state.marketsOffset / this.state.marketsLimit) + 1}</span>
            <button className='btn btn-sm btn-secondary' disabled={!this.state.marketsHasNext} onClick={this.nextMarkets}>Next</button>
          </div>
        )}

        {!loading && view === 'tags' && (
          <PanelTable headers={['ID', 'Label', 'Slug']}>
            <tbody>
              {tags.map((tag, index) => (
                <tr key={index}>
                  <td>{tag.id}</td>
                  <td>
                    <a href="#" onClick={(e) => { e.preventDefault(); this.loadMarketsForTag(tag); }}>{tag.label}</a>
                  </td>
                  <td>{tag.slug}</td>
                </tr>
              ))}
            </tbody>
          </PanelTable>
        )}
        {!loading && view === 'tags' && (
          <div className='d-flex justify-content-between align-items-center mt-2'>
            <button className='btn btn-sm btn-secondary' disabled={this.state.tagsOffset === 0} onClick={this.prevTags}>Prev</button>
            <span>Page {Math.floor(this.state.tagsOffset / this.state.tagsLimit) + 1}</span>
            <button className='btn btn-sm btn-secondary' disabled={!this.state.tagsHasNext} onClick={this.nextTags}>Next</button>
          </div>
        )}
        {!loading && view === 'tagMarkets' && (
          <div>
            <div style={{ marginBottom: '0.5rem' }}>
              <button className='btn btn-sm btn-secondary' onClick={() => this.setState({ view: 'tags', selectedTag: null, tagMarkets: [], tagMarketsAll: [] })}>Back to Tags</button>
              { (this.state.showActive || this.state.showClosed) && (
                <span className='ml-2 text-muted'>
                  Filter: { this.state.showActive ? 'Active ' : ''}{ this.state.showClosed ? 'Closed' : ''}
                </span>
              )}
            </div>
            <PanelTable headers={['Event', 'Market ID', 'Question', 'Status']}>
              <tbody>
                {(() => {
                  const all = this.state.tagMarketsAll || tagMarkets;
                  const showActive = !!this.state.showActive;
                  const showClosed = !!this.state.showClosed;
                  const filtered = (showActive === showClosed)
                    ? all
                    : all.filter(m => (showActive && m.active) || (showClosed && m.closed));
                  return filtered.map((market, index) => (
                  <tr key={index}>
                    <td>{market._eventTitle}</td>
                    <td>{market.id}</td>
                    <td>{market.question}</td>
                    <td>{market.archived ? 'Archived' : (market.closed ? 'Closed' : (market.active ? 'Active' : ''))}</td>
                  </tr>
                  ));
                })()}
              </tbody>
            </PanelTable>
            <div className='d-flex justify-content-between align-items-center mt-2'>
              <button className='btn btn-sm btn-secondary' disabled={this.state.eventsOffset === 0} onClick={this.prevEvents}>Prev</button>
              <span>Page {Math.floor(this.state.eventsOffset / this.state.eventsLimit) + 1}</span>
              <button className='btn btn-sm btn-secondary' disabled={!this.state.eventsHasNext} onClick={this.nextEvents}>Next</button>
            </div>
          </div>
        )}
      </Panel>
    );
  }
}

export default PolymarketTab;