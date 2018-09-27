const path = require('path');

module.exports = {
  mode: "development",
  context: path.join(__dirname, '/src/main/web'),
  entry: './js/script.jsx',
  output: {
    path: path.join(__dirname, '/src/main/public/bundles'),
    filename: 'bundle.js'
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/
      },
      {
        test: /\.css$/,
        use: [ 'style-loader', 'css-loader' ]
      }
    ]
  }
};