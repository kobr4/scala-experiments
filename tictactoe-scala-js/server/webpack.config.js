const path = require('path');


module.exports = {
   mode: "development",
   context: path.resolve(__dirname),
   entry: './assets/scalajs-output.js',
    output: {
        path: __dirname + '/src/main/public/bundles',
        filename: '[name]-bundle.js'
    },

  resolve: {
    alias: {
      'react-native$': 'react-native-web'
    },
    extensions: ['.js']
  }


};
