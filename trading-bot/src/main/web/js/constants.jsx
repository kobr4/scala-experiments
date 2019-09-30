export const allAssets = [['BTC','BTC'],['ETH','ETH'],['XMR','XMR'],['XRP','XRP'],
    ['XLM','XLM'],['DOGE','DOGE'],['ADA','ADA'],['LTC','LTC'],['XEM','XEM'],['ZEC','ZEC'],['DASH','DASH'],['DGB','DGB'],['TETHER', 'TETHER']];
  
export const baseAssets = [['BTC','BTC'],['USD','USD'],['TETHER','TETHER']];
  
  
export const defaultStrategy = {
      strategyList : [
        {
          buyList: [ 
            {
              method : 'whenAboveMovingAverage',
              days : 20
            }
          ],
          sellList: [
            {
              method : 'whenBelowMovingAverage',
              days : 20
            }      
          ]
        }
      ]
    }